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
 */
def getVersionNum() { return "1.5.00" }
private def getVersionLabel() { return "Ecobee Suite Smart Mode Helper, version ${getVersionNum()}" }
import groovy.json.JsonOutput

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
        	else {input ("thermostats", "capability.Thermostat", title: "Pick Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)}            
		}
        if (!settings.tempDisable) {
			section(title: "Select Outdoor Temperature Source") {
				input(name: 'tempSource', title: 'Monitor this temperature source', type: 'enum', required: true, multiple: false, description: 'Tap to choose...', options:['Weather for Location', 'SmartThings Temperature Sensor', "Thermostat's Weather Temperature", 'WeatherUnderground Station'], submitOnChange: true)
				if (tempSource) {
					if (tempSource == 'Weather for Location') {
                        input(name: "zipCode", type: 'text', title: 'Zipcode (Default is location Zip code)', defaultValue: getZIPcode(), required: true, submitOnChange: true )
						input(name: 'locFreq', type: 'enum', title: 'Temperature check frequency (minutes)', required: true, multiple: false, description: 'Tap to choose...', options:['1','5','10','15','30','60','180'])
					} else if (tempSource == 'SmartThings Temperature Sensor') {
						input(name: 'thermometer', type: 'capability.temperatureMeasurement', title: "Which Temperature Sensor?", description: 'Tap to choose...', required: true, multiple: false)
					} else if (tempSource == "Thermostat's Weather Temperature") {
						if (thermostats.size() > 1) {
							input(name: 'tstatTemp', type: 'enum', title: "Which Thermostat?", description: 'Tap to choose...', required: true, multiple: false, options:thermostats.displayName)
						}
					} else if (tempSource == "WeatherUnderground Station") {
						input(name: 'stationID', type: 'string', title: 'Enter WeatherUnderground PWS identifier', description: "Tap to choose...", defaultValue: getPWSID(), required: true)
						
                        input(name: 'nearestPWS', type: 'bool', title: 'Use nearest PWS', options: ['true', 'false'], defaultValue: true, submitOnChange: true)
        				href(title: "Or, Search WeatherUnderground.com for your desired PWS ${nearestPWS}",
        					description: 'After page loads, select "Change Station" for a list of weather stations.  ' +
        					'You will need to copy the station code into the PWS field above',
             				required: false, style:'embedded',             
             				url: (location.latitude && location.longitude)? "http://www.wunderground.com/cgi-bin/findweather/hdfForecast?query=${location.latitude},${location.longitude}" :
             		 		"http://www.wunderground.com/q/${location.zipCode}")
                        input(name: 'pwsFreq', type: 'enum', title: 'Temperature check frequency (minutes)', required: true, multiple: false, description: 'Tap to choose...', options:['1','5','10','15','30','60','180'])
					}
				}
			}
			section(title: "Outdoors Temperature Thresholds (Required, range: ${getThermostatRange()})") {
				// need to set min & max - get from thermostat range
       			input(name: "aboveTemp", title: "When the outdoor temperature is at or above...", type: 'decimal', description: "Enter decimal temperature (${settings.belowTemp?'optional':'required'})", range: getThermostatRange(), required: !settings.belowTemp, submitOnChange: true)
				if (settings.aboveTemp) {
					input(name: 'aboveMode', title: 'Set thermostat mode to', type: 'enum', description: 'Tap to choose...', required: true, multiple: false, options:getThermostatModes(), submitOnChange: true)
				}
            	input(name: "belowTemp", title: 'When the outdoor temperature is at or below...', type: 'number', description: "Enter decimal temperature (${settings.aboveTemp?'optional':'required'})", range: getThermostatRange(), required: !settings.aboveTemp, submitOnChange: true)
				if (settings.belowTemp) {
					input(name: 'belowMode', title: 'Set thermostat mode to', type: 'enum', description: 'Tap to choose...', required: true, multiple: false, options:getThermostatModes())
				}
				if ((settings.belowTemp && settings.aboveTemp) && (settings.belowTemp != settings.aboveTemp)) {
					input(name: 'betweenMode', title: "When the outdoor temperature is between ${belowTemp}° and ${aboveTemp}°, set thermostat mode to (optional)", type: 'enum', description: 'Tap to choose...', required: false, multiple: false, options:getThermostatModes())
				}
            }
            section(title: "Indoors Temperature Controls (Optional)") {
            	if (getThermostatModes().contains('cool') && !settings.insideAuto) {
            		input(name: 'aboveCool', title: 'Set thermostat Mode to Cool if its temperature is above its Cooling Setpoint (optional)?', type: 'bool', defaultValue: false, submitOnChange: true)
                }
                if (getThermostatModes().contains('heat') && !settings.insideAuto) {
                	input(name: 'belowHeat', title: 'Set thermostat Mode to Heat if its temperature is below its Heating Setpoint (optional)?', type: 'bool', defaultValue: false, submitOnChange: true)
                }
                if (getThermostatModes().contains('auto') && !(settings.aboveCool || settings.belowHeat)) {
                	input(name: 'insideAuto', title: 'Set thermostat Mode to Auto if its temperature is above or below its Setpoints (optional)?', type: 'bool', defaultValue: false, submitOnChange: true)
                }
			}
            
      		section("Notifications (optional)") {
        		input(name: 'notify', type: 'bool', title: "Notify on Activations?", required: false, defaultValue: false, submitOnChange: true)
            	if (settings.notify) {
        			input(name: "recipients", type: "contact", title: "Send notifications to", required: notify) {
               			input(name: "pushNotify", type: "boolean", title: "Send push notifications?", required: true, defaultValue: false)
               		}
            	}
            	paragraph("(A notification is always sent to the Hello Home Notifications log whenever Ecobee Suite Smart Mode changes a thermostat's Mode)")
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
	initialize()  
}

def updated() {
	LOG("updated() entered", 3, "", 'trace')
	unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	if(settings.tempDisable) {
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }
    
    if (settings.aboveCool || settings.belowHeat || settings.insideAuto) {
    	subscribe(thermostats, 'temperature', insideChangeHandler)
    }
    Double tempNow = -99.0
	switch( settings.tempSource) {
		case 'Weather for Location':
			if (settings.locFreq.toInteger() < 60) {
            	"runEvery${settings.locFreq}Minute${settings.locFreq!='1'?'s':''}"( 'getZipTemp' )
            } else {
            	def locHours = settings.locFreq.toInteger() / 60
                "runEvery${locHours}Hour${locHours!=1?'s':''}"( 'getZipTemp' )
            }
            def t = getZipTemp()					// calls temperatureUpdate()
            if (t.isNumber()) tempNow = t.toDouble()
			break;
		
		case 'SmartThings Temperature Sensor':
			subscribe( settings.thermometer, 'temperature', tempChangeHandler)
            def t = settings.thermometer.currentValue('temperature')
            if (t.isNumber()) { tempNow = t.toDouble(); temperatureUpdate(tempNow) }
			break;
		
		case "Thermostat's Weather Temperature":
			def theStat = []
			theStat = settings.thermostats.size() == 1 ? settings.thermostats : [settings.tstatTemp]
			subscribe(theStat, 'weatherTemperature', tempChangeHandler)
            def t = theStat.currentValue('weatherTemperature')
            if (t.isNumber()) { tempNow = t.toDouble(); temperatureUpdate(tempnow) }
			break;
		
		case 'WeatherUnderground Station':
			if (settings.pwsFreq.toInteger() < 60) {
            	"runEvery${settings.pwsFreq}Minute${settings.pwsFreq!='1'?'s':''}"( 'getPwsTemp' )
            } else {
            	def pwsHours = settings.pwsFreq.toInteger() / 60
                "runEvery${pwsHours}Hour${pwsHours!=1?'s':''}"( 'getPwsTemp' )
            }
            def t = getPwsTemp()					// calls temperatureUpdate*()
            if (t.isNumber()) tempNow = t.toDouble()
			break;
	}
    if (tempNow && (tempNow > -90)) {
    	LOG("Initialization complete...current temperature is ${tempNow}°",2,null,'info')
        return true
    } else {
    	LOG("Initialization error...invalid temperature: ${tempNow}° - please check settings and retry", 2, null, 'error')
        return false
    }
}

def insideChangeHandler(evt) {
	def theTemp = evt.doubleValue
    def newMode = null
    if (theTemp.isNumber()) {
    	def coolSP = evt.device.currentValue('coolingSetpointDisplay')
        if (coolSP.isNumber()) {
        	if (theTemp > coolSP) {
            	if (settings.aboveCool) {
                	newMode = 'cool'
                } else if (settings.insideAuto && (evt.device.currentValue('thermostatMode') != 'cool')) {
                	newMode = 'auto'
                }
            }
        }
        if (newMode == null) {
       		def heatSP = evt.device.currentValue('heatingSetpointDisplay')
            if (heatSP.isNumber()) {
				if (theTemp < heatSP) {
                	if (settings.belowHeat) {
                    	newMode = 'heat'
                    } else if (settings.insideAuto && (evt.device.currentValue('thermostatMode') != 'heat')) {
                    	newMode = 'auto'
                    }
                }
            }
        }
        if (newMode != null) {
        	if (evt.device.currentValue('thermostatMode') != newMode) {
            	evt.device.setThermostatMode(newMode)
                LOG("${evt.device.displayName} temp is ${theTemp}°, changed thermostat to ${newMode} mode",3,null,'trace')
        		NOTIFY("${app.label}: Thermostat ${evt.device.displayName} temperature is ${theTemp}°, so I changed it to ${newMode} mode")
            }
        }
    }
}

def tempChangeHandler(evt) {
	Double newTemp 
    try { 
    	newTemp = evt.doubleValue 
		temperatureUpdate( newTemp )
    } catch (e) {
    	LOG("Invalid temp: ${e}",2,null,'warn')
    }
}   
							  
def temperatureUpdate( Double temp ) {
    if (!temp || !temp.isNumber()) {
    	LOG("Ignoring invalid temperature: ${temp}°", 2, null, 'warn')
        return false
    }
    
    def desiredMode = null
	if (settings.aboveTemp && (temp >= settings.aboveTemp)) {
    	if (!atomicState.aboveChanged) {
			desiredMode = settings.aboveMode
            atomicState.aboveChanged = true
            atomicState.betweenChanged = false
            atomicState.belowChanged = false
        }
	} else if (settings.belowTemp && (temp <= settings.belowTemp)) {
    	if (!atomicState.belowChanged) {
			desiredMode = settings.belowMode
            atomicState.aboveChanged = false
            atomicState.betweenChanged = false
            atomicState.belowChanged = true
        }
	} else if (settings.aboveTemp && settings.belowTemp && settings.betweenMode) {
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
			if (it.currentValue('thermostatMode') != desiredMode) {
				it.setThermostatMode(desiredMode)
                changeNames += changeNames ? ", ${it.displayName}" : it.displayName
			} else {
                sameNames += sameNames ? ", ${it.displayName}" : it.displayName
            }
		}
        def multi=0
        if (changeNames) {
        	LOG("Temp is ${temp}°, changed ${changeNames} to ${desiredMode} mode",3,null,'trace')
        	NOTIFY("${app.label}: The temperature is ${temp}°, so I changed thermostat${changeNames.size() > 1?'s':''} ${changeNames} to ${desiredMode} mode")
        }
        if (sameNames) LOG("Temp is ${temp}°, ${sameNames} already in ${desiredMode} mode",3,null,'info')
	}
}

private def getZipTemp() {
	return getWUTemp('zip')
}

private def getPwsTemp() {
	return getWUTemp('pws')
}

private def getWUTemp(type) {
	def isMetric = (getTemperatureScale() == "C")
    Double tempNow = 999.99
    def source = (type == 'zip') ? settings.zipCode : "pws:"+settings.stationID
	Map wdata = getWeatherFeature('conditions', source)
    if (wdata && wdata.response) {
    	//LOG("conditions: ${wdata.response}",4,null,'trace')
		if (wdata.response.containsKey('error')) {
        	if (wdata.response.error.type != 'invalidfeature') {
    			LOG("Please check ${type=='zip'?'Zipcode':'WU Station'} setting, error:\n${wdata.response.error.type}: ${wdata.response.error.description}" ,1,null,'error')
        		return null
            } 
            else {
            	LOG("Error requesting weather:\n${wdata.response.error}",2,null,'warn')
            	return null
            }
		}
    }
    else {
    	LOG("Please check Zipcode/PWS setting, weather returned: null",2,null,'warn')
    	return null
    }
    if (wdata.current_observation) { 
    	if (!isMetric) {
			if (wdata.current_observation.temp_f.isNumber()) tempNow = wdata.current_observation.temp_f.toDouble()
        } else {
        	if (wdata.current_observation.temp_c.isNumber()) tempNow = wdata.current_observation.temp_c.toDouble()
        }
        if (tempNow != 999.99) {
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
	Double low = 0.0
	Double high = 99.0
	settings.thermostats.each {
		def l = it.currentValue('heatingSetpointMin') as Double
		def h = it.currentValue('coolingSetpointMax') as Double
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
    Map wdata = getWeatherFeature('geolookup', geoLocation)
    if (wdata && wdata.response && !wdata.response.containsKey('error')) {	// if we get good data
    	if (wdata.response.features.containsKey('geolookup') && (wdata.response.features.geolookup.toInteger() == 1) && wdata.location) {
        	log.debug "wdata ${wdata.location.nearby_weather_stations.pws}"
    		PWSID = wdata.location.nearby_weather_stations.pws.station[0].id
    	}
    	else log.debug "bad response"
    }
    else log.debug "null or error"

	log.debug "Nearest PWS ${PWSID}"
	return PWSID
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}

private def NOTIFY(message) {
	if (settings.notify) {
    	if (location.contactBookEnabled && settings.recipients) {
        	sendNotificationToContacts(message, settings.recipients)// notify contacts & hello home
    	} else if (settings.pushNotify) {
    		sendNotification(message)								// push and hello home
    	} else {
        	sendNotificationEvent(message)							// just hello home
    	}
    }
}
