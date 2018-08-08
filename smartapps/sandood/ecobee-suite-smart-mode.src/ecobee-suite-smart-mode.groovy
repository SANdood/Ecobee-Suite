/**
 *  Ecobee Suite Smart Mode
 *
 *  Copyright 2018 Justin Leonard, Barry A. Burke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0  
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	1.4.0  - Initial release
 *	1.4.01 - Added unschedule() to updated()
 *	1.4.02 - Shortened LOG and NOTIFY strings when reporting on multiple thermostats
 *	1.4.03 - Fix frequency enum translations
 *	1.4.04 - Fixed notifications
 *	1.4.05 - Change the mode only ONCE when crossing a configured threshold (for coexistence with other Helpers/Instances)
 *	1.4.06 - Added more data validation around outside temp sources
 *	1.4.07 - Added inside temperature Mode change options
 *	1.4.08 - Tweaked inside temp change - don't switch to auto if mode is already the correct one (cool/heat)
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Allow Ecobee Suite Thermostats only
 *	1.5.02 - Converted all math to BigDecimal
 *	1.5.03 - Added (optional) dewpoint override for belowTemp Off Mode
 *	1.5.04 - Added modeOff reservation support to avoid conflicts with other Helper Apps
 *	1.5.05 - Added multiple SMS support (Contacts being deprecated by ST)
 *	1.6.00 - Release number synchronization
 *	1.6.01 - Fixed sendMessage()
 *	1.6.02 - Fix reservation initialization error
 *	1.6.03 - REALLY fix reservation initialization error
 *	1.6.04 - Really, REALLY fix reservation initialization error
 *	1.6.10 - Converted to parent-based reservations
 *	1.6.11 - Clear reservations when disabled
 *	1.6.12 - Logic tuning, clear reservations when externally overridden
 *	1.6.13 - Removed location.contactBook - unexpectedly deprecated by SmartThings
 *	1.6.14 - Updated to remove use of *SetpointDisplay
 */
def getVersionNum() { return "1.6.14" }
private def getVersionLabel() { return "Ecobee Suite Smart Mode Helper, version ${getVersionNum()}" }
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

definition(
	name: "ecobee Suite Smart Mode",
	namespace: "sandood",
	author: "Justin J. Leonard & Barry A. Burke",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSets Ecobee Heat/Cool/Auto mode based on (outside) temperature.",
	category: "Convenience",
	parent: "sandood:Ecobee Suite Manager",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false,
    pausable: true
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "${getVersionLabel()}", uninstall: true, install: true) {
    	section(title: "Name for Smart Mode Helper") {
        	label title: "Name this Helper", required: true, defaultValue: "Smart Mode"
        }
        section(title: "${settings.tempDisable?'':'Select Thermostat(s)'}") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled as requested. Turn back on to activate handler."}
        	else {input ("thermostats", "device.ecobeeSuiteThermostat", title: "Pick Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)}            
		}
        if (!settings.tempDisable) {
			section(title: "Select Outdoor Weather Source") {
				input(name: 'tempSource', title: 'Monitor this weather source', type: 'enum', required: true, multiple: false, description: 'Tap to choose...', 
                	options:[
                    	'ecobee':"Ecobee Thermostat's Weather", 
                    	'location':'SmartThings Weather for Location', 
                        'sensors':'SmartThings Sensors',
                        'station':'SmartThings Weather Station',
                        'wunder':'Weather Underground Station'
                    ], submitOnChange: true
                )
				if (tempSource) {
					if (tempSource == 'location') {
                    	paragraph "Using SmartThings weather for the current location (${location.name})."
                        if (!settings.latLon) input(name: "zipCode", type: 'text', title: 'Zipcode (Default is location Zip code)', defaultValue: getZIPcode(), required: true, submitOnChange: true )
                        if (location.latitude && location.longitude) input(name: "latLon", type: 'bool', title: 'Use SmartThings hub\'s GPS coordinates instead (better precision)?', submitOnChange: true)
						input(name: 'locFreq', type: 'enum', title: 'Temperature check frequency (minutes)', required: true, multiple: false, description: 'Tap to choose...', 
                        	options:['1','5','10','15','30','60','180'])
					} else if (tempSource == 'sensors') {
                    	paragraph "Using SmartThings sensors. Note: Both Temperature & Humidity sensors are required for dew point-based actions."
						input(name: 'thermometer', type: 'capability.temperatureMeasurement', title: "Which Temperature Sensor?", description: 'Tap to choose...', required: true, multiple: false)
                        input(name: 'humidistat', type: 'capability.relativeHumidityMeasurement', title: "Which Relative Humidity Sensor?", description: 'Tap to choose...', 
                        	required: (settings.dewBelowOverride), multiple: false) 
					} else if (tempSource == "ecobee") {
                    	paragraph "Using weather data from the (notoriously inaccurate) Ecobee thermostat${(settings.thermostats.size()==1)?' '+settings.thermostats.displayName:':'}"
						if (settings.thermostats.size() > 1) {
							input(name: 'tstatTemp', type: 'enum', title: "Which Ecobee thermostat?", description: 'Tap to choose...', required: true, multiple: false, submitOnChange: true,
                            		options:thermostats.displayName)
						}
					} else if (tempSource == 'station') {
                    	paragraph "Using a SmartThings-based Weather Station - please select ONE from the list of available & supported Weather Station devices below..."
                        input(name: "smartWeather", type: "device.smartWeatherStationTile", title: 'Which SmartWeather Station Tile?', description: "Tap to choose...", required: false, 
                        		multiple: false, hideWhenEmpty: true)
                        input(name: "smartWeather2", type: "device.smartWeatherStationTile2.0", title: 'Which SmartWeather Station Tile 2.0?', description: "Tap to choose...", required: false, 
                        		multiple: false, hideWhenEmpty: true)
                        input(name: "meteoWeather", type: "device.meteobridgeWeatherStation", title: 'Which Meteobridge Weather Station?', description: "Tap to choose...", required: false, 
                        		multiple: false, hideWhenEmpty: true)
                    } else if (tempSource == "wunder") {
                    	paragraph "Using a specific Weather Underground Weather Station"
						input(name: 'stationID', type: 'string', title: 'Enter WeatherUnderground Station identifier', description: "Tap to choose...", 
                        		defaultValue: "${settings.nearestPWS?getPWSID():''}", required: true)
                        input(name: 'nearestPWS', type: 'bool', title: 'Use nearest available station', options: ['true', 'false'], defaultValue: true, submitOnChange: true)
        				href(title: "Or, Search WeatherUnderground.com for your desired PWS",
        					description: 'After page loads, select "Change Station" for a list of weather stations.  ' +
        					'You will need to copy the station code into the PWS field above, in the form of "pws:STATIONID"',
             				required: false, style:'embedded',             
             				url: (location.latitude && location.longitude)? "http://www.wunderground.com/cgi-bin/findweather/hdfForecast?query=${location.latitude},${location.longitude}" :
             		 		"http://www.wunderground.com/q/${location.zipCode}")
                        input(name: 'pwsFreq', type: 'enum', title: 'Temperature check frequency (minutes)', required: true, multiple: false, description: 'Tap to choose...', 
                        	options:['1','5','10','15','30','60','180'])
					}
				}
			}
			section(title: "Outdoor Temperature Thresholds\n(Required, range: ${getThermostatRange()})") {
				// need to set min & max - get from thermostat range
       			input(name: "aboveTemp", title: "When the outdoor temperature is at or above...", type: 'decimal', description: "Enter decimal temperature (${settings.belowTemp?'optional':'required'})", 
                		range: getThermostatRange(), required: !settings.belowTemp, submitOnChange: true)
                input(name: "dewAboveTemp", title: "Or, (optionally) when the outdoor dewpoint is at or above...", type: 'decimal', description: "Enter decimal dewpoint", 
                		required: false, submitOnChange: true)
				if (settings.aboveTemp || settings.dewAboveTemp) {
					input(name: 'aboveMode', title: 'Set thermostat mode to', type: 'enum', description: 'Tap to choose...', required: true, multiple: false, options:getThermostatModes(), 
                    		submitOnChange: true)
                    if (settings.aboveMode == 'off') {
                    	paragraph "Note that Ecobee thermostats will still run fan circulation (if enabled) while the HVAC is in Off Mode"
                    }
				}
            	input(name: "belowTemp", title: 'When the outdoor temperature is at or below...', type: 'decimal', description: "Enter decimal temperature (${settings.aboveTemp?'optional':'required'})", 
                		range: getThermostatRange(), required: !settings.aboveTemp, submitOnChange: true)
				if (settings.belowTemp) {
					input(name: 'belowMode', title: 'Set thermostat mode to', type: 'enum', description: 'Tap to choose...', required: true, multiple: false, options:getThermostatModes(), 
                    		submitOnChange: true)
                    if (!(settings.aboveMode == 'off') && (settings.belowMode == 'off')) {
                    	paragraph "Note that Ecobee thermostats will still run fan circulation (if enabled) while the HVAC is in Off Mode"
                    }
				}
                if (settings.belowTemp && (settings.belowMode == 'off')) {
                	input(name: 'dewBelowOverride', type: 'bool', title: 'Dewpoint overrides below temp Off Mode?', required: true, defaultValue: false, submitOnChange: true)
                	if (settings.dewBelowOverride) {
                    	input(name: 'dewBelowTemp', type: 'decimal', title: 'Override Off Mode when dew point is at or above...', description: "Enter decimal dew point", required: true, 
                        		submitOnChange: true)       
                	}
            	}
				if ((settings.belowTemp && settings.aboveTemp) && (settings.belowTemp != settings.aboveTemp)) {
					input(name: 'betweenMode', title: "When the outdoor temperature is between ${belowTemp}° and ${aboveTemp}°, set thermostat mode to (optional)", type: 'enum', 
                    		description: 'Tap to choose...', required: false, multiple: false, options:getThermostatModes(), submitOnChange: true)
				}
            }
            section(title: "Indoor Temperature Controls (Optional)") {
            	if (getThermostatModes().contains('cool') && !settings.insideAuto) {
            		input(name: 'aboveCool', title: 'Set thermostat Mode to Cool if its indoor temperature is above its Cooling Setpoint (optional)?', type: 'bool', defaultValue: false, 
                    		submitOnChange: true)
                }
                if (getThermostatModes().contains('heat') && !settings.insideAuto) {
                	input(name: 'belowHeat', title: 'Set thermostat Mode to Heat if its indoor temperature is below its Heating Setpoint (optional)?', type: 'bool', defaultValue: false, 
                    		submitOnChange: true)
                }
                if (getThermostatModes().contains('auto') && !(settings.aboveCool || settings.belowHeat)) {
                	input(name: 'insideAuto', title: 'Set thermostat Mode to Auto if its indoor temperature is above or below its Setpoints (optional)?', type: 'bool', defaultValue: false, 
                    		submitOnChange: true)
                }
			}
			section(title: "Enable only for specific modes or programs?") {
        		paragraph("Thermostat Modes will only be changed while ${location.name} is in these SmartThings Modes.")
            	input(name: "theModes",type: "mode", title: "Only when the Location Mode is", multiple: true, required: false)
        	}            
      		section("Notifications (optional)") {
        		input(name: 'notify', type: 'bool', title: "Notify on Activations?", required: false, defaultValue: false, submitOnChange: true)
                
            	if (settings.notify) {
       				paragraph "You can enter multiple phone numbers seperated by a semi-colon (;)"
       				input "phone", "string", title: "Send SMS notifications to", description: "Phone Number", required: false, submitOnChange: true 
                    if (!settings.phone) {
                        input( name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, submitOnChange: true)
                    }
                    if (!settings.phone && !settings.sendPush) paragraph "Notifications configured, but nobody to send them to!"
                }
            	paragraph("A notification is always sent to the Hello Home log whenever Smart Mode changes a thermostat's Mode")
        	}
        }
        section(title: "Temporary Disable") {
        	input(name: "tempDisable", title: "Temporarily Disable this Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
		}
    	section (getVersionLabel()) {}
    }
}

def installed() {
	LOG("installed() entered", 3, "", 'trace')
    atomicState.aboveChanged = false
    atomicState.betweenChanged = false
    atomicState.belowChanged = false
    atomicState.dewpoint = null
    atomicState.humidity = null
	initialize()  
}

def uninstalled() {
	clearReservations()
}
def clearReservations() {
	thermostats?.each {
    	cancelReservation(getDeviceId(it.deviceNetworkId), 'modeOff' )
    }
}
def updated() {
	LOG("updated() with settings: ${settings}", 3, "", 'trace')
	unsubscribe()
    unschedule()
    atomicState.dewpoint = null
    atomicState.humidity = null
    initialize()
}

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	if(settings.tempDisable) {
    	clearReservations()
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }
    
    if (settings.aboveCool || settings.belowHeat || settings.insideAuto) {
    	subscribe(thermostats, 'temperature', insideChangeHandler)
    }
    
    if (settings.aboveTemp || settings.belowTemp) {
    	subscribe(thermostats, 'thermostatMode', thermostatModeHandler)
    }
    def tempNow
    def gu = getTemperatureScale()
	switch( settings.tempSource) {
		case 'location':
        	def WUname = (settings.latLon) ? 'getGPSTemp' : 'getZipTemp'
			if (settings.locFreq.toInteger() < 60) {
            	"runEvery${settings.locFreq}Minute${settings.locFreq!='1'?'s':''}"( "${WUname}" )
            } else {
            	def locHours = settings.locFreq.toInteger() / 60
                "runEvery${locHours}Hour${locHours!=1?'s':''}"( "${WUname}" )
            }
            def t = "${WUname}"()					// calls temperatureUpdate() & stores dewpoint
            if (t.isNumber()) tempNow = t
			break;
		
		case 'sensors':
            if (settings.dewBelowOverride) {
            	if (settings.humidistat) subscribe( settings.humidistat, 'relativeHumidity', humidityChangeHandler)
            } else {
            	log.error "Dewpoint override enabled, but no humidistat selected - initialization FAILED."
                return false
            }
            subscribe( settings.thermometer, 'temperature', tempChangeHandler)
            def latest = settings.thermometer.currentState("temperature")
			def unit = latest.unit
            def t 
            if (latest.value.isNumber()) {
            	t = roundIt(latest.numberValue, (unit=='C'?2:1))
            	if (dewBelowOverride) {
                	latest = settings.humidistat.currentState('humidity')
            		if (latest.value.isNumber()) {
                    	def h = latest.numberValue
            			atomicState.humidity = h
                        LOG("Humidity is: ${h}%",3,null,'info')
                		def d = calculateDewpoint( t, h, unit )
            			atomicState.dewpoint = d
                        LOG("Dewpoint is: ${d}°",3,null,'info')
                   	}
                }
            	tempNow = t 
                temperatureUpdate(tempNow) 
            }
			break;
            
        case 'station':
        	if (settings.smartWeather) {
            	subscribe(settings.smartWeather, 'temperature', tempChangeHandler)
                def latest = settings.smartWeather.currentState('temperature')
                def t = latest.value
                def unit = latest.unit
                if (t.isNumber()) {
                	t = latest.numberValue
                	if (dewBelowOverride) {
                		subscribe(settings.smartWeather, 'relativeHumidity', humidityChangeHandler)
                		latest = settings.smartWeather.currentState('relativeHumidity')
                		if (latest.value.isNumber()) {
                        	def h = roundIt(latest.numberValue, (unit=='C'?2:1))
                        	LOG("Humidity is: ${h}%",3,null,'info')
                			def d = calculateDewpoint( t, h, unit )
                            atomicState.dewpoint = d
                            LOG("Dewpoint is: ${d}°",3,null,'info')
                        }
                    }
                	tempNow = t 
                    temperatureUpdate(tempNow) 
                }
            } else if (settings.smartWeather2) {
            	def latest
                if (settings.dewBelowOverride) {
                	subscribe(settings.smartWeather2, 'dewpoint', dewpointChangeHandler)
                	latest = settings.smartWeather2.currentState('dewpoint')
                    if (latest.value.isNumber()) {
                    	def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
                        atomicState.dewpoint = d
                        LOG("Dewpoint is: ${d}°",3,null,'info')
                    }
                }
            	subscribe(settings.smartWeather2, 'temperature', tempChangeHandler)
                latest = settings.smartWeather2.currentState('temperature')
            	if (latest.value.isNumber()) { 
                	tempNow = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
                    temperatureUpdate(tempNow) 
                }
            } else if (settings.meteoWeather) {
            	def latest
                if (settings.dewBelowOverride) {
                	subscribe(settings.meteoWeather, 'dewpoint', dewpointChangeHandler)
                	latest = settings.meteoWeather.currentState('dewpoint')
                    if (latest.value.isNumber()) {
                    	def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
                        atomicState.dewpoint = d
                        LOG("Dewpoint is: ${d}°",3,null,'info')
                    }
                }
            	subscribe(settings.meteoWeather, 'temperature', tempChangeHandler)
                latest = settings.meteoWeather.currentState('temperature')
            	if (latest.value.isNumber()) { 
                	tempNow = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
                    temperatureUpdate(tempNow) 
                }
            }
        	break;
		
		case "ecobee":
			def theStat = []
            def latest
			theStat = settings.thermostats.size() == 1 ? settings.thermostats[0] : settings.tstatTemp
            if (dewBelowOverride) {
            	subscribe(theStat, 'weatherDewpoint', dewpointChangeHandler)
            	latest = theStat.currentState('weatherDewpoint')
            	if (latest.value.isNumber()) {
                	def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
                	atomicState.dewpoint = d
                    LOG("Dewpoint is: ${d}°",3,null,'info')
                }
            }
            subscribe(theStat, 'weatherTemperature', tempChangeHandler)
            latest = theStat.currentState('weatherTemperature')
            if (latest.value.isNumber()) {
            	tempNow = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
                temperatureUpdate(tempnow) 
            }
			break;
		
		case 'wunder':
			if (settings.pwsFreq.toInteger() < 60) {
            	"runEvery${settings.pwsFreq}Minute${settings.pwsFreq!='1'?'s':''}"( 'getPwsTemp' )
            } else {
            	def pwsHours = settings.pwsFreq.toInteger() / 60
                "runEvery${pwsHours}Hour${pwsHours!=1?'s':''}"( 'getPwsTemp' )
            }
            def t = getPwsTemp()					// calls temperatureUpdate() and updates atomicState.dewpoint
            if (t.isNumber()) tempNow = t
			break;
	}
    atomicState.locModeEnabled = theModes ? theModes.contains(location.mode) : true
    if (tempNow) {
    	atomicState.temperature = tempNow
    	LOG("Initialization complete...current temperature is ${tempNow}°",2,null,'info')
        return true
    } else {
    	LOG("Initialization error...invalid temperature: ${tempNow}° - please check settings and retry", 2, null, 'error')
        return false
    }
}

def insideChangeHandler(evt) {
    def theTemp = evt.value
    def newMode = null
    if (theTemp.isNumber()) {
    	theTemp = evt.numberValue
    	def coolSP = evt.device.currentValue('coolingSetpoint')
        if (coolSP.isNumber()) {
        	coolSP = coolSP.toBigDecimal()
        	if (theTemp > coolSP) {
            	if (settings.aboveCool) {
                	newMode = 'cool'
                } else if (settings.insideAuto && (evt.device.currentValue('thermostatMode') != 'cool')) {
                	newMode = 'auto'
                }
            }
        }
        if (newMode == null) {
       		def heatSP = evt.device.currentValue('heatingSetpoint')
            if (heatSP.isNumber()) {
            	heatSP = heatSP.toBigDecimal()
				if (theTemp < heatSP) {
                	if (settings.belowHeat) {
                    	newMode = 'heat'
                    } else if (settings.insideAuto && (evt.device.currentValue('thermostatMode') != 'heat')) {
                    	newMode = 'auto'
                    }
                }
            }
        }
        def okMode = theModes ? theModes.contains(location.mode) : true
        if (okMode) {
        	atomicState.locModeEnabled = true
            if (newMode != null) {
                def cMode = evt.device.currentValue('thermostatMode')
                if (cMode != newMode) {
                    def tid = getDeviceId(evt.device.deviceNetworkId)
                    if ((cMode == 'off') && anyReservations( tid, 'modeOff' )) {
                        // if ANYBODY (including me) has a reservation on this being off, I can't turn it back on
                        LOG("${evt.device.displayName} inside temp is ${theTemp}°, but can't change to ${newMode} since ${getGuestList(tid,'offMode').toString()[1..-2]} have offMode reservations",2,null,'warn')
                        // Here's where we could subscribe to reservations and re-evaluate. For now, just wait for another inside Temp Change to occur
                    } else {
                        // not currently off or there are no modeOff reservations, change away!
                        cancelReservation(tid, 'modeOff' )
                        evt.device.setThermostatMode(newMode)
                        LOG("${evt.device.displayName} temp is ${theTemp}°, changed thermostat to ${newMode} mode",3,null,'trace')
                        sendMessage("Thermostat ${evt.device.displayName} temperature is ${theTemp}°, so I changed it to ${newMode} mode")
                    }
                }
            }
        } else {
        	if (atomicState.locModeEnabled) {
                // Do we check for/cancel reservations?
                def tid = getDeviceId(evt.device.deviceNetworkId)
                cancelReservation(tid, 'modeOff')
                if (!anyReservations(tid, 'modeOff')) {
                    evt.device.setThermostatMode('auto')		// allow choice, keep reservation if off
                }
                atomicState.locModeEnabled = false
            }
        }
    }
}

def thermostatModeHandler(evt) {
	// if the mode changes but we didn't do it, reset the atomicState modes as appropriate
    if ((settings.aboveTemp || settings.dewAboveTemp) && (evt.value == settings.aboveMode) && !atomicState.aboveChanged) {
    	atomicState.belowChanged = false
        atomicState.betweenChanged = false
    }
    else if (settings.belowTemp && (evt.value == settings.belowMode) && !atomicState.belowChanged) {
    	atomicState.aboveChanged = false
        atomicState.betweenChanged = false
    }
    else if (settings.aboveTemp && settings.belowTemp && settings.betweenMode && (evt.value == settings.betweenMode) && !atomicState.betweenChanged) {
    	atomicState.aboveChanged = false
        atomicState.belowChanged = false
    }
    if (evt.value != 'off') cancelReservation( getDeviceId(evt.device.deviceNetworkId), 'modeOff' ) // we're not off anymore, give up the reservation
}

def tempChangeHandler(evt) {
    if (evt.value?.isNumber()) {
    	def t = roundIt(evt.numberValue, (evt.unit=='C'?2:1))
    	atomicState.temperature = t
        if (settings.dewBelowOverride || settings.dewAboveTemp) {
        	// We have to update the dewpoint every time the temperature (or humidity) changes
        	if (atomicState.humidity != null) {
            	// Somebody is updating atomicState.humidity, so we need to calculate the dewpoint
                // (Sources that provide dewpoint directly will not update atomicState.humidity)
            	if (settings.tempSource == 'sensors') {    
            		def latest = settings.humidistat.currentState('humidity')
            		if (latest.value.isNumber()) {
                    	def h = latest.numberValue
            			atomicState.humidity = h
                        LOG("Humidity is: ${h}%",3,null,'info')
                		def d = calculateDewpoint( t, h, evt.unit )
            			atomicState.dewpoint = d
                        LOG("Dewpoint is: ${d}°",3,null,'info')
                        runIn(2, atomicTempUpdater, [overwrite: true] )		// humidity might be updated also
                        return
                   	}
                } else if ((settings.tempSource == 'station') && settings.smartWeather) {
                	def latest = settings.smartWeather.currentState('relativeHumidity')
                    if (latest.value.isNumber()) {
                    	h = latest.numberValue
                        LOG("Humidity is: ${h}%",3,null,'info')
                		def d = calculateDewpoint( t, h, unit )
                        atomicState.dewpoint = d
                        LOG("Dewpoint is: ${d}°",3,null,'info')
                        runIn(2, atomicTempUpdater, [overwrite: true] )		// humidity might be updated also
                        return
                    }
                }
            } else {
            	runIn(2, atomicTempUpdater, [overwrite: true] )				// wait for dewpoint to be updated also
                return
            }
        }
        // Aren't doing dewpoint stuff, so we can just update the temp directly
    	temperatureUpdate( t )
    }
}   

def dewpointChangeHandler(evt) {
	if (evt.value.isNumber()) {
    	def d = roundIt(evt.numberValue, (evt.unit=='C'?2:1))
    	atomicState.dewpoint = d
        LOG("Dewpoint is: ${d}°",3,null,'info')
        runIn(2, atomicTempUpdater, [overwrite: true]) 		// wait for temp to be updated also
    }
}

def humidityChangeHandler(evt) {
	if (evt.value.isNumber()) {
    	t = atomicState.temperature
        u = getTemperatureScale()
        atomicState.humidity = evt.numberValue
        LOG("Humidity is: ${evt.numberValue}%",3,null,'info')
    	def d = calculateDewpoint(t, evt.numberValue, u)
        atomicState.dewpoint = d
        LOG("Dewpoint is: ${d}°",3,null,'info')
        runIn(2, atomicTempUpdater, [overwrite: true])
    }
}

def atomicTempUpdater() {
	temperatureUpdate( atomicState.temperature )
}

def temperatureUpdate( temp ) {
	if (temp?.isNumber()) temperatureUpdate(temp as BigDecimal)
}
def temperatureUpdate( BigDecimal temp ) {
    if (!temp) {
    	LOG("Ignoring invalid temperature: ${temp}°", 2, null, 'warn')
        return false
    }
    temp = roundIt(temp, (getTemperatureScale()=='C'?2:1))
    atomicState.temperature = temp
    LOG("Temperature is: ${temp}°",3,null,'info')
    
    def okMode = theModes ? theModes.contains(location.mode) : true
    if (okMode) {
    	atomicState.locModeEnabled = true
    } else {
    	if (atomicState.locModeEnabled) {
        	// release all the reservations and reset the mode
        	settings.thermostats.each { 
            	def tid = getDeviceId(it.deviceNetworkId)
            	// Do we check for/cancel reservations?
            	cancelReservation(tid, 'modeOff')
            	if (!anyReservations(tid, 'modeOff')) {
                	evt.device.setThermostatMode('auto')		// allow choice, keep reservation if off
            	}
            }
            atomicState.locModeEnabled = false
        }
        //LOG something
        return
    }
    
    def desiredMode = null
	if ( (settings.aboveTemp && (temp >= settings.aboveTemp)) || (settings.dewAboveTemp && (atomicState.dewpoint >= settings.dewAboveTemp))) {
    	if (!atomicState.aboveChanged) {
			desiredMode = settings.aboveMode
            atomicState.aboveChanged = true
            atomicState.betweenChanged = false
            atomicState.belowChanged = false
        }
	} else if (settings.belowTemp && (temp <= settings.belowTemp)) {
    	if (!atomicState.belowChanged) {
        	// We haven't already changed to belowMode
        	if ((settings.belowMode != 'off') || !settings.dewBelowOverride || (settings.dewBelowTemp > atomicState.dewpoint)) {
            	// not turning HVAC off or aren't overriding off at this time
                desiredMode = settings.belowMode
                // TBD: Should we save the prior mode so we have something to return to???
                atomicState.aboveChanged = false
                atomicState.betweenChanged = false
                atomicState.belowChanged = true
            } else {
            	// not supposed to change the mode right now
                // belowMode is 'off', and dewBelowOverride is true, and current dewpoint is >= dewBelowTemp setting.
                // reset everything
                desiredMode = null
				atomicState.aboveChanged = false
                atomicState.betweenChanged = false
                atomicState.belowChanged = false
            }
        } else {
        	// We have prior changed to the belowMode - now we have to check if dewpoint is still below the limit
            if ((settings.belowMode == 'off') && settings.dewBelowOverride && (settings.dewBelowTemp <= atomicState.dewpoint)) {
            	// Uh-oh, the dewpoint has risen into the bad land
            	if (settings.betweenMode) {
                	// We have a between mode - let's change back to that
                	desiredMode = settings.betweenMode
            		atomicState.aboveChanged = false
            		atomicState.betweenChanged = true
            		atomicState.belowChanged = false
                } else if (settings.aboveMode) {
                	// OK, now between mode. But we have an above mode - switch to that
                	desiredMode = settings.aboveMode
                    atomicState.aboveChanged = true
                    atomicState.betweeChanged = false
                    atomicState.belowChanged = false
                }
            } else {
            	// No reason to change anything - 
            }
        }
	} else if ((settings.aboveTemp || (settings.dewAboveTemp && (atomicState.dewpoint < settings.dewAboveTemp))) && settings.belowTemp && settings.betweenMode) {
    	if (!atomicState.betweenChanged) {
			desiredMode = settings.betweenMode
            atomicState.aboveChanged = false
            atomicState.betweenChanged = true
            atomicState.belowChanged = false
        }
	}
	if (desiredMode != null) {
    	String changeNames = ""
        String sameNames = ""
		settings.thermostats.each { 
        	def cMode = it.currentValue('thermostatMode')
            def tid = getDeviceId(it.deviceNetworkId)
			if ( cMode != desiredMode) {
            	if (desiredMode == 'off') {
                	makeReservation(tid, 'modeOff')
                    it.setThermostatMode( 'off' )
                } else {
                	// Desired mode IS NOT 'off'
                	if (cMode == 'off') {
                    	cancelReservation(tid,'modeOff')
                    	if (countReservations(tid, 'modeOff') == 0) {
                    		// nobody else has a reservation on modeOff
							it.setThermostatMode(desiredMode)
                			changeNames += changeNames ? ", ${it.displayName}" : it.displayName
						} else {
                    		// somebody else still has a 'modeOff' reservation so we can't turn it on
                            def msg = "The temperature is ${temp}°, but I can't change ${it.displayName} to ${desiredMode} Mode because ${getGuestList(tid,'modeOff').toString()[1..-2]} hold 'modeOff' reservations"
                            LOG(msg ,2,null,'warn')
                            sendMessage(msg)
                            // here's where we COULD subscribe to the reservations to see when we can turn it back on. For now, let's just let whomever is last deal with it
                    	}
                    } else {
                    	// Not off currently, so we can change freely
                        cancelReservation(tid, 'modeOff')	// just in case
                    	it.setThermostatMode(desiredMode)
                    }
                }
            } else {
            	// already running the mode we want
            	(desireMode == 'off') ? makeReservation(tid, 'modeOff') : cancelReservation(tid, 'modeOff')
	            sameNames += sameNames ? ", ${it.displayName}" : it.displayName
            }
		}
        def multi=0
        if (changeNames) {
        	LOG("Temp is ${temp}°, changed ${changeNames} to ${desiredMode} mode",3,null,'trace')
        	sendMessage("The temperature is ${temp}°, so I changed thermostat${changeNames.size() > 1?'s':''} ${changeNames} to ${desiredMode} mode")
        }
        if (sameNames) LOG("Temp is ${temp}°, ${sameNames} already in ${desiredMode} mode",3,null,'info')
	}
}

private def getZipTemp() {
	return getWUTemp('zip')
}

private def getGPSTemp() {
	return getWUTemp('gps')
}

private def getPwsTemp() {
	return getWUTemp('pws')
}

private def getWUTemp(type) {
	def isMetric = (getTemperatureScale() == "C")
    def tempNow
    def dewpointNow
    def source = (type == 'zip') ? settings.zipCode : ((type == 'gps')?"${location.latitude},${location.longitude}":settings.stationID)
	Map wdata = getWeatherFeature('conditions', source)
    if (wdata && wdata.response) {
    	//LOG("conditions: ${wdata.response}",4,null,'trace')
		if (wdata.response.containsKey('error')) {
        	if (wdata.response.error.type != 'invalidfeature') {
    			LOG("Please check ${type=='zip'?'ZIPcode':((type=='gps')?'Location Lat/Lon':'WU Station')} setting, error:\n${wdata.response.error.type}: ${wdata.response.error.description}" ,1,null,'error')
        		return null
            } 
            else {
            	LOG("Error requesting weather:\n${wdata.response.error}",2,null,'warn')
            	return null
            }
		}
    }
    else {
    	LOG("Please check ZIPcode, Lat/Lon, or PWS setting, weather returned: null",2,null,'warn')
    	return null
    }
    if (wdata.current_observation) { 
    	if (!isMetric) {
			if (wdata.current_observation.temp_f?.isNumber()) tempNow = wdata.current_observation.temp_f.toBigDecimal()
            if (wdata.current_observation.dewpoint_f?.isNumber()) dewpointNow = wdata.current_observation.dewpoint_f.toBigDecimal()
        } else {
        	if (wdata.current_observation.temp_c?.isNumber()) tempNow = wdata.current_observation.temp_c.toBigDecimal()
            if (wdata.current_observation.dewpoint_c?.isNumber()) dewpointNow = wdata.current_observation.dewpoint_c.toBigDecimal()
        }
        if (tempNow?.isNumber()) {
        	LOG("Dewpoint is: ${dewpointNow}°",2,null,'info')
        	atomicState.dewpoint = dewpointNow
        	temperatureUpdate(tempNow)
            return tempNow
        } else {
        	LOG("Invalid temp returned ${newTemp}, ignoring...",2,null,'warn')
            return null
        }
    }
    LOG("Current conditions unavailable",1,null,'error')
    return null
}

// Calculate a close approximation of Dewpoint based on Temp, Relative Humidity (need Units - algorithm only works for C values)
def calculateDewpoint( temp, rh, units) {
	def t = ((units == 'C') ? temp : (temp-32)/1.8) as BigDecimal
	def dpC = 243.04*(Math.log(rh/100.0)+((17.625*t)/(243.04+t)))/(17.625-Math.log(rh/100.0)-((17.625*t)/(243.04+t)))
    return (units == 'C') ? roundIt(dpC, 2) : roundIt(((dpC*1.8)+32), 1)
}
private roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
private roundIt( BigDecimal value, decimals=0) {
    return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}

// return all the modes that ALL thermostats support
def getThermostatModes() {
	def theModes = []
    
    settings.thermostats.each { stat ->
    	if (theModes == []) {
        	theModes = stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", ")
        } else {
        	theModes = theModes.intersect(stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", "))
        }   
    }
    return theModes.sort(false)
}

// return the largest range that ALL thermostats will allow
def getThermostatRange() {
	def low = 0.0
	def high = 99.0
	settings.thermostats.each {
		def latest = it.currentState('heatingSetpointMin')
        def l = (latest.value.isNumber()) ? latest.numberValue : low
        latest = it.currentState('coolingSetpointMax')
		def h = (latest.value.isNumber()) ? latest.numberValue : high
		if (l > low) low = l
		if (h < high) high = h
	}
	return "${low}..${high}"
}

private String getZIPcode() {
	if (location.zipCode) return location.zipCode
    else return ""
}

private String getPWSID() {
	String PWSID = location.zipCode
	log.debug "Location ZIP Code ${PWSID}"
	// find the nearest PWS to the hub's geo location
	String geoLocation = location.zipCode
	// use coordinates, if available
	if (location.latitude && location.longitude) geoLocation = "${location.latitude},${location.longitude}"
    log.debug "Geolocation: ${geoLocation}"
    Map wdata = getWeatherFeature('geolookup', geoLocation)
    if (wdata && wdata.response && !wdata.response.containsKey('error')) {	// if we get good data
    	if (wdata.response.features.containsKey('geolookup') && (wdata.response.features.geolookup.toInteger() == 1) && wdata.location) {
        	//log.debug "wdata ${wdata.location.nearby_weather_stations.pws}"
            log.debug "wdata ${wdata.location.nearby_weather_stations}"
    		if (wdata.location.nearby_weather_stations?.pws?.station[0]?.id) PWSID = 'pws:' + wdata.location.nearby_weather_stations.pws.station[0].id
            else if (wdata.location.nearby_weather_stations?.airport?.station[0]?.icao) PWSID = wdata.location.nearby_weather_stations.airport.station[0].icao
    	}
    	else log.debug "bad response"
    }
    else log.debug "null or error"

	log.debug "Nearest PWS ${PWSID}"
	return PWSID
}

private def getDeviceId(networkId) {
	// def deviceId = networkId.split(/\./).last()	
    // LOG("getDeviceId() returning ${deviceId}", 4, null, 'trace')
    // return deviceId
    return networkId.split(/\./).last()
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id, type )
}
// Cancel my reservation
void cancelReservation(tid, String type='modeOff') {
	log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id, type )
}
// Do I have a reservation?
Boolean haveReservation(tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id, type )
}
// Do any Apps have reservations?
Boolean anyReservations(tid, String type='modeOff') {
	return parent.anyReservations( tid, type )
}
// How many apps have reservations?
Integer countReservations(tid, String type='modeOff') {
	return parent.countReservations( tid, type )
}
// Get the list of app IDs that have reservations
List getReservations(tid, String type='modeOff') {
	return parent.getReservations( tid, type )
}
// Get the list of app Names that have reservations
List getGuestList(tid, String type='modeOff') {
	return parent.getGuestList( tid, type )
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}

private def sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 5, null, "trace")
    
    if (settings.notify) {
        String msg = "${location.name} ${app.label}: " + notificationMessage		// for those that have multiple locations, tell them where we are
        if (phone) { // check that the user did select a phone number
            if ( phone.indexOf(";") > 0){
                def phones = phone.split(";")
                for ( def i = 0; i < phones.size(); i++) {
                    LOG("Sending SMS ${i+1} to ${phones[i]}",2,null,'info')
                    sendSmsMessage(phones[i], msg)									// Only to SMS contact
                }
            } else {
                LOG("Sending SMS to ${phone}",2,null,'info')
                sendSmsMessage(phone, msg)											// Only to SMS contact
            }
        } else if (settings.pushNotify) {
            LOG("Sending Push to everyone",2,null,'warn')
            sendPushMessage(msg)													// Push to everyone
        }
    }
    sendNotificationEvent( ${app.label}+ ': ' + notificationMessage )								// Always send to hello home
}
