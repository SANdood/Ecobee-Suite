/**
 *	Ecobee Suite Smart Mode
 *
 *	Copyright 2018-2020 Justin Leonard, Barry A. Burke
 * 
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0	
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 * <snip>
 *	1.7.12 - Extended external temperature range, add Program change support
 *	1.7.13 - Changed displayName to Smart Mode, Programs & Setpoints
 *	1.7.14 - Rerun temperature checks when location.mode becomes valid again
 *	1.7.15 - Display current Mode & Program in appLabel
 *	1.7.16 - Clean up app label in sendMessage()
 *	1.7.17 - Fixed appLabel on ST
 *	1.7.18 - Added option to disable local display of log.debug() logs, support Notification devices on ST
 *	1.7.19 - Fixed appLabel yet again
 *	1.7.20 - Cleaned up Notifications settings, removed SMS for HE
 *	1.7.21 - Added customized notifications, fixed missing notification on mode change
 *	1.7.22 - Fixed typo in insideChangeHandler()
 *	1.7.23 - Fixed setting.* typos
 *	1.7.24 - Added more supported weather stations for both ST and HE
 *	1.7.25 - Fixed Helper labelling
 *	1.7.26 - Fixed labels (again), added infoOff, cleaned up preferences setup, added intro ***
 *	1.7.27 - Added reservation serializer for setpoint changes (program.climates)
 *	1.7.28 - Added minimize UI
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - Fixed Mode selection error & updated WARNING formatting
 *	1.8.03 - More busy bees
 *	1.8.04 - Fix update on Location Mode changes
 *	1.8.05 - Send simultaneous notification Announcements to multiple Echo Speaks devices
 *	1.8.06 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.07 - New SHPL, using Global Fields instead of atomicState
 *	1.8.08 - Fixed pageLabel issue of CustomNotifications page
 *	1.8.09 - Fixed appDisplayName in sendMessage
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.8.09" }
String getVersionLabel()	{ return "Ecobee Suite Smart Mode, Programs & Setpoints Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name:				"ecobee Suite Smart Mode",
	namespace:			"sandood",
	author:				"Justin J. Leonard & Barry A. Burke",
	description:		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSet Ecobee Mode, Program and/or Program Setpoints based on temperature & dewpoint",
	category:			"Convenience",
	parent:				"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
	iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
	importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-mode.src/ecobee-suite-smart-mode.groovy",
	documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#smart-mode-sa",
	singleInstance:		false,
	pausable:			true
)

preferences {
	page(name: "mainPage")
	page(name: "customNotifications")
}

def mainPage() {
	//boolean ST = isST
	//boolean HE = !ST
	boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
	String defaultName = "Smart Mode, Programs & Setpoints"
	def statModes
		
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
		if (maximize) {
			section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
				if (ST) {
					paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
				} else {
					paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
				}
				paragraph("This Helper will automatically change Ecobee Suite Thermostats' Mode, Program and/or Program Setpoints based on the outdoor temperature and/or dewpoint, and (optionally) "+
						  "when the indoor temperature falls outside of the setpoints. It can be configured to use many different outdoor weather stations and sources, including Ecobee thermostats.")
			}
		}
		section(title: sectionTitle("Naming${!settings.tempDisable?' & Thermostat Selection':''}")) {	
			String defaultLabel
			if (!atomicState?.appDisplayName) {
				defaultLabel = defaultName
				app.updateLabel(defaultName)
				atomicState?.appDisplayName = defaultName
			} else {
				defaultLabel = atomicState.appDisplayName
			}
			label(title: inputTitle("Name for this ${defaultName} Helper"), required: false, submitOnChange: true, defaultValue: defaultLabel, width: 6)
			if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			} else {
				atomicState.appDisplayName = app.label
			}
			if (HE) {
				if (app.label.contains('<span ')) {
					if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains('<span ')) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
						String myLabel = app.label.substring(0, app.label.indexOf('<span '))
						atomicState.appDisplayName = myLabel
						app.updateLabel(myLabel)
					}
				} else {
					atomicState.appDisplayName = app.label
				}
			} else {
				def opts = [' (paused)', ' (Cool', ' (Heat', ' (Aux', ' (Off', ' (Auto', ' (Emer']
				String flag
				String asFlag
				opts.each {
					if (!flag && app.label.contains(it)) flag = it
					if (!asFlag && atomicState?.appDisplayName?.contains(it)) asFlag = it
				}
				if (flag) {
					if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains(asFlag)) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
						String myLabel = app.label.substring(0, app.label.indexOf(flag))
						atomicState.appDisplayName = myLabel
						app.updateLabel(myLabel)
					}
				} else {
					atomicState.appDisplayName = app.label
				}
			}
			if (settings?.tempDisable) { 
				paragraph getFormat("warning","This Helper is temporarily paused...")
			} else {
				input ("thermostats", "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select Ecobee Suite Thermostat(s)"), required: true, 
					   multiple: true, submitOnChange: true)
			}
		}
		
		if (!settings?.tempDisable && (settings?.thermostats?.size()>0)) {
			statModes = getThermostatModes()
			//log.debug "${statModes}"
			section(title: sectionTitle("Configuration")+(ST?"\n":'')+smallerTitle("Outdoor Weather Source")) {
				if (maximize) paragraph("Smart Mode requires access to external weather information (temperature and relative humidity). If you don't have a physical weather station of your own, "+
						  "there are user-contributed weather drivers for many weather providers. If your provider isn't listed, choose '${ST?'SmartThings ':'Hubitat '} Sensors' and select your "+
						  "external weather sensors manually")
				input(name: 'tempSource', title: inputTitle('Select a Weather Source'), type: 'enum', required: true, multiple: false,	
					  options: (ST?[
						  'ecobee':"Ecobee Thermostat's Weather", 
						  'location':"SmartThings/TWC Weather for ${location.name}", 
						  'sensors':'SmartThings Sensors',
						  'station':'SmartThings-based Weather Station DTH',
						  'wunder':'Weather Underground Station (obsolete)'
						]:[
						  'ecobee':"Ecobee Thermostat's Weather",  
						  'sensors':'Hubitat Sensors',
						  'station':'Hubitat-based Weather Station Device',
						  ]), submitOnChange: true
				)
				if (settings.tempSource) {
					if (settings.tempSource == 'location') {
						if (maximize) paragraph "Using The Weather Company weather for the current location (${location.name})."
						if (!settings.latLon) input(name: "zipCode", type: 'text', title: inputTitle('Zipcode (Default is location Zip code)'), defaultValue: getZIPcode(), required: true,
													submitOnChange: true, width: 6)
						if (location.latitude && location.longitude) input(name: "latLon", type: 'bool', submitOnChange: true, width: 6, 
																		   title: inputTitle("Use ${ST?'SmartThings':'Hubitat'} hub's GPS coordinates?")+" (better precision)")
						input(name: 'locFreq', type: 'enum', title: inputTitle('Temperature check frequency (minutes)'), required: true, multiple: false, 
							options:['1','5','10','15','30','60','180'], width: 6)
					} else if (settings.tempSource == 'sensors') {
						if (maximize) paragraph "Using ${ST?'SmartThings':'Hubitat'} sensors. Note: Both Temperature & Humidity sensors are required for dew point-based actions."
						input(name: 'thermometer', type: 'capability.temperatureMeasurement', title: inputTitle("Which Temperature Sensor?"), required: true, multiple: false, width: 4)
						input(name: 'humidistat', type: 'capability.relativeHumidityMeasurement', title: inputTitle("Which Relative Humidity Sensor?"),	 
							required: (settings.dewBelowOverride), multiple: false, width: 4)
						if (HE) paragraph("", width: 4)
					} else if (settings.tempSource == "ecobee") {
						if (maximize) paragraph "Using weather data from the (notoriously inaccurate) Ecobee thermostat${(settings.thermostats.size()==1)?' '+settings.thermostats.displayName:':'}"
						if (settings.thermostats.size() > 1) {
							input(name: 'tstatTemp', type: 'enum', title: inputTitle("Which Ecobee Thermostat?"), required: true, multiple: false, submitOnChange: true,
									options:thermostats.displayName, width: 6)
						}
					} else if (settings.tempSource == 'station') {
						paragraph((maximize ? "Using a ${ST?'SmartThings':'Hubitat'}-based Weather Station - p":"P") + "lease select ${ST?'ONE ':'<b>ONE</b> '}from the list of the supported Weather Station devices below...")
						if (ST) {
							// Smart Weather Station Tile
							// NOTES: supplies temperature & relative humidity only
							input(name: "smartWeather", type: "device.smartWeatherStationTile", title: inputTitle('Which SmartWeather Station Tile?'), required: false,
									multiple: false, hideWhenEmpty: true, width: 4)
							
							// Smart Weather Station Tile 2.0
							// NOTES: supplies temperature & dewpoint
							input(name: "smartWeather2", type: "device.smartWeatherStationTile2.0", title: inputTitle('Which SmartWeather Station Tile 2.0?'), required: false, 
									multiple: false, hideWhenEmpty: true, width: 4)
							
							// Netatmo Outdoor Module
							// NOTES: provides temperature & relative humidity only
							input(name: "netatmoOutdoorModule", type: "device.NetatmoOutdoorModule", title: inputTitle("Which Netatmo Outdoor Module?"), required: false, 
								  multiple: false, hideWhenEmpty: true, width: 4)
						} else {
							// Ambient Weather Device (https://community.hubitat.com/t/ambient-weather-device/3660)
							// NOTES: provides temperature & dewpoint
							input(name: "ambientWeatherDevice", type: "device.AmbientWeatherDevice", title: inputTitle("Which Ambient Weather Device?"), required: false, 
								  multiple: false, hideWhenEmpty: true, width: 4)
							
							// DarkSky.net Weather Driver (https://community.hubitat.com/t/release-darksky-net-weather-driver-no-pws-required/22699)
							// NOTES: provides temperature & relative humidity, but dewpoint is optional, so we'll calculate it ourselves
							input(name: "darkSkyNetWeatherDriver", type: "device.DarkSky.netWeatherDriver", title: inputTitle("Which DarkSky.net Weather Driver?"), 
								  required: false, multiple: false, hideWhenEmpty: true, width: 4)
							
							// Netatmo Outdoor Module (multiple sources - one is here: https://github.com/fuzzysb/Hubitat)
							// NOTES: provides temperature & relative humidity only
							input(name: "netatmoOutdoorModule", type: "device.NetatmoOutdoorModule", title: inputTitle("Which Netatmo Outdoor Module?"), required: false, 
								  multiple: false, hideWhenEmpty: true, width: 4)
						}
						input(name: "meteoWeather", type: "${ST?'device.meteobridgeWeatherStation':'device.MeteobridgeWeatherStation'}", title: inputTitle('Which Meteobridge Weather Station?'), required: false, 
								multiple: false, hideWhenEmpty: true, width: 4)		 
					} else if (settings.tempSource == "wunder") {
						if (maximize) paragraph "Using a specific Weather Underground Weather Station"
						input(name: 'stationID', type: 'string', title: inputTitle('Enter WeatherUnderground Station identifier'), defaultValue: "${settings.nearestPWS?getPWSID():''}", required: true)
						input(name: 'nearestPWS', type: 'bool', title: inputTitle('Use nearest available station'), options: ['true', 'false'], defaultValue: true, submitOnChange: true)
						href(title: inputTitle("Or, Search WeatherUnderground.com for your desired PWS"),
							description: 'After page loads, select "Change Station" for a list of weather stations.	 ' +
							'You will need to copy the station code into the PWS field above, in the form of "pws:STATIONID"',
							required: false, style:'embedded',			   
							url: (location.latitude && location.longitude)? "http://www.wunderground.com/cgi-bin/findweather/hdfForecast?query=${location.latitude},${location.longitude}" :
							"http://www.wunderground.com/q/${location.zipCode}")
						input(name: 'pwsFreq', type: 'enum', title: inputTitle('Temperature check frequency (minutes)'), required: true, multiple: false, options:['1','5','10','15','30','60','180'])
					}
				}
			}
			section(title: sectionTitle("Actions")+(ST?"\n":'')+smallerTitle("Outdoor Temperature 'Above'")) {
				// need to set min & max - get from thermostat range
				input(name: "aboveTemp", title: inputTitle("When the outdoor temperature is at or above..."), type: 'decimal', description: "Enter decimal temperature (${settings.belowTemp?'optional':'required'})", 
						range: getThermostatRange(), required: !settings.belowTemp, submitOnChange: true, width: 6)
				input(name: "dewAboveTemp", title: inputTitle("Or, when the outdoor dewpoint is at or above..."), type: 'decimal', description: "Enter decimal dewpoint (optional)", 
						required: false, submitOnChange: true, width: 6)
				if (settings.aboveTemp || settings.dewAboveTemp) {
					input(name: 'aboveMode', title: inputTitle('Set thermostat Mode to'), type: 'enum', required: (!settings.aboveSetpoints && !settings.aboveSchedule), multiple: false, 
						  options:getThermostatModes(), submitOnChange: true, width: 6)
					if (settings.aboveMode == 'off') {
						if (maximize) paragraph "Note that Ecobee thermostats will still run fan circulation (if enabled) while the HVAC is in Off Mode"
					}
					input(name: 'aboveSchedule', title: inputTitle('Set thermostat Program to'), type: 'enum', required: (!settings.aboveSetpoints && ! settings.aboveMode), multiple: false, 
						  submitOnChange: true, options:getThermostatPrograms(), width: 6)
					input(name: 'aboveSetpoints', title: inputTitle('Change Program Setpoints'), type: 'bool', required: (!settings.aboveMode && !settings.aboveSchedule), 
						  defaultValue: false, submitOnChange: true, width: 6)
					if (HE) paragraph("", width: 6)
					if (settings.aboveSetpoints) {
						if (!settings.aboveProgram && (!settings.aboveHeatTemp || !settings.aboveCoolTemp)) paragraph "You must select the program to modify and at least one setpoint to change"
						input(name: 'aboveProgram', title: inputTitle('Change Setpoints for Program'), type: 'enum', required: true, submitOnChange: true, multiple: false, 
								options:getThermostatPrograms(), width: 4)
						input(name: 'aboveHeatTemp', title: inputTitle("Desired heating setpoint (${getHeatRange()})"), type: 'decimal', description: 'Default = no change...', required: (!settings.aboveCoolTemp), 
								range: getHeatRange(), submitOnChange: true, width: 4)
						input(name: 'aboveCoolTemp', title: inputTitle("Desired cooling setpoint (${getCoolRange()})"), type: 'decimal', description: 'Default = no change...', required: (!settings.aboveHeatTemp), 
								range: getCoolRange(), submitOnChange: true, width: 4)
					}
				}
			}
			section(title: smallerTitle("Outdoor Temperature 'Below'")) {
				input(name: "belowTemp", title: inputTitle('When the outdoor temperature is at or below...'), type: 'decimal', description: "Enter decimal temperature (${settings.aboveTemp?'optional':'required'})", 
						range: getThermostatRange(), required: !settings.aboveTemp, submitOnChange: true, width: 6)
				if (HE) paragraph("", width: 6)
				if (settings.belowTemp) {
					input(name: 'belowMode', title: inputTitle('Set thermostat Mode to'), type: 'enum', required: (!settings.belowSetpoints && !settings.belowSchedule), multiple: false, 
							options:statModes, submitOnChange: true, width: 6)
					if (settings.belowMode == 'off') {
						if (maximize) paragraph "Note that Ecobee thermostats will still run fan circulation (if enabled) while the HVAC is in Off Mode"
					}
					input(name: 'belowSchedule', title: inputTitle('Set thermostat Program to'), type: 'enum', required: (!settings.belowSetpoints && !settings.belowMode), multiple: false,
						  submitOnChange: true, options:getThermostatPrograms(), width: 6)
					input(name: 'belowSetpoints', title: inputTitle('Change Program Setpoints'), type: 'bool', required: (!settings.belowMode && !settings.belowSchedule), defaultValue: false,
						  submitOnChange: true, width: 6)
					if (HE) paragraph("", width: 6)
					if (settings.belowSetpoints) {
						if (!settings.belowProgram && (!settings.belowHeatTemp || !settings.belowCoolTemp)) paragraph "You must select the program to modify and at least one setpoint to change"
						input(name: 'belowProgram', title: inputTitle('Change Setpoints for Program'), type: 'enum', required: true, submitOnChange: true, multiple: false, 
								options:getThermostatPrograms(), width: 4)
						input(name: 'belowHeatTemp', title: inputTitle("Desired heating setpoint (${getHeatRange()})"), type: 'decimal', description: 'Default = no change...', required: (!settings.belowCoolTemp), 
								range: getHeatRange(), submitOnChange: true, width: 4)
						input(name: 'belowCoolTemp', title: inputTitle("Desired cooling setpoint (${getCoolRange()})"), type: 'decimal', description: 'Default = no change...', required: (!settings.belowHeatTemp), 
								range: getCoolRange(), submitOnChange: true, width: 4)
					}
				}
				if (settings.belowTemp && (settings.belowMode == 'off')) {
					input(name: 'dewBelowOverride', type: 'bool', title: inputTitle('Dewpoint overrides below temp Off Mode?'), required: true, defaultValue: false, submitOnChange: true, width: 4)
					if (settings.dewBelowOverride) {
						input(name: 'dewBelowTemp', type: 'decimal', title: inputTitle('Override Off Mode when dew point is at or above...'), description: "Enter decimal dew point", required: true, 
								submitOnChange: true, width: 4)		  
					}
				}
			}
			if ((settings.belowTemp && settings.aboveTemp) && (settings.belowTemp != settings.aboveTemp)) {
				//section(title: sectionTitle("Actions: Outdoor Temperature 'Between' Actions")) {}
				section(title: smallerTitle("Outdoor Temperature 'Between'")) {
					input(name: 'betweenMode', title: inputTitle("Set thermostat Mode to")+" (optional)", type: 'enum', 
							required: false, multiple: false, options:statModes, submitOnChange: true, width: 6)
					input(name: 'betweenSchedule', title: inputTitle('Set thermostat Program to')+' (optional)', type: 'enum', required: false, multiple: false, options:getThermostatPrograms(), width: 6)
					input(name: 'betweenSetpoints', title: inputTitle('Change Program Setpoints'), type: 'bool', required: false, defaultValue: false, submitOnChange: true, width: 6) 
					if (HE) paragraph("", width: 6)
					if (settings.betweenSetpoints) {
						if (!settings.betweenProgram && (!settings.betweenHeatTemp || !settings.betweenCoolTemp)) paragraph "You must select the program to modify and at least one setpoint to change"
						input(name: 'betweenProgram', title: inputTitle('Change Setpoints for Program'), type: 'enum', required: true, submitOnChange: true, multiple: false, 
								options:getThermostatPrograms(), width: 4)
						input(name: 'betweenHeatTemp', title: inputTitle('Desired heating setpoint'), type: 'decimal', description: 'Default = no change...', required: (!settings.betweenCoolTemp), 
								range: getHeatRange(), submitOnChange: true, width: 4)
						input(name: 'betweenCoolTemp', title: inputTitle('Desired cooling setpoint'), type: 'decimal', description: 'Default = no change...', required: (!settings.betweenHeatTemp), 
								range: getCoolRange(), submitOnChange: true, width: 4)
					}
				}
			}
			if (settings.aboveSchedule || settings.belowSchedule || settings.betweenSchedule) {
				section(title: smallerTitle("Hold Type")) {
					input(name: "holdType", title: inputTitle("Hold Type for Program changes (optional)"), type: "enum", required: false, 
						  multiple: false, submitOnChange: true, defaultValue: "Ecobee Manager Setting",
						  options:["Until I Change", "Until Next Program", "2 Hours", "4 Hours", "Specified Hours", "Thermostat Setting", 
												"Ecobee Manager Setting"], width: 6) //, "Parent Ecobee (Connect) Setting"])
					if (settings.holdType=="Specified Hours") {
						input(name: 'holdHours', title: inputTitle('How many hours (1-48)?'), type: 'number', range:"1..48", required: true, description: '2', defaultValue: 2, width: 4)
					} else if (settings.holdType=='Thermostat Setting') {
						if (maximize) paragraph("Thermostat Setting at the time of hold request will be applied.")
					} else if ((settings.holdType == null) || (settings.holdType == 'Ecobee Manager Setting') || (settings.holdType == 'Parent Ecobee (Connect) Setting')) {
						if (maximize) paragraph("Ecobee Manager Setting at the time of hold request will be applied")
					}
				}
			}
			section(title:smallerTitle('Indoor Temperature')) {
				if (statModes.contains('cool')) { // && !settings.insideAuto) {
					input(name: 'aboveCool', title: inputTitle('Set thermostat Mode to Cool if its Indoor Temperature is above its Cooling Setpoint')+' (optional)', type: 'bool', defaultValue: false, 
						  submitOnChange: true)
					if (settings.aboveCool) {
						if (maximize) paragraph "Mode will be set to 'cool' when the indoor temperature reaches the Cooling Setpoint PLUS the Cooling Setpoint Differential MINUS ${((getTemperatureScale()=='F')?'0.1':'0.055')}°" +
							  " - This is just before the thermostat will demand Cool from the HVAC"
					}
				}
				if (statModes.contains('heat')) { // && !settings.insideAuto) {
					input(name: 'belowHeat', title: inputTitle('Set thermostat Mode to Heat if its Indoor Temperature is below its Heating Setpoint')+' (optional)', type: 'bool', defaultValue: false, 
						  submitOnChange: true)
					if (settings.aboveCool) {
						if (maximize) paragraph "Mode will be set to 'heat' when the indoor temperature reaches the Heating Setpoint MINUS the Heating Setpoint Differential PLUS ${((getTemperatureScale()=='F')?'0.1':'0.055')}°" +
							  " - This is just before the thermostat will demand Heat from the HVAC"
					}
				}
				if (statModes.contains('auto')) { // && !(settings.aboveCool || settings.belowHeat)) {
					input(name: 'insideAuto', title: inputTitle('Set thermostat Mode to Auto if its Indoor Temperature is between its Heating & Cooling Setpoints')+' (optional)', type: 'bool', defaultValue: false, 
						  submitOnChange: true)
					if (settings.insideAuto) {
						if (maximize) paragraph "Mode will be set to 'auto' when the indoor temperature falls outside the Cooling and Heating Setpoints (adjusted by the appropriate differentials) - This is just before the" +
								  " thermostat will demand Heat or Cool from the HVAC"
					}
				}
				if ((settings.aboveCool || settings.belowHeat || settings.insideAuto) && (settings.aboveMode?.contains('off') || settings.belowMode?.contains('off') || settings.betweenMode?.contains('off'))) {
					input(name: 'insideOverridesOff', title: inputTitle("Allow the above indoor temperature/setpoint operations to change the Mode when the HVAC is 'off'?"), type: 'bool', defaultValue: false, 
						  submitOnChange: true)
				}
			}
			section(title: sectionTitle("Conditions")) {
				input(name: "theModes",type: "mode", title: inputTitle("Perform Actions only when the Location Mode is"), multiple: true, required: false, width: 6)
				if (settings.theModes) {
					input(name: 'doneMode', title: inputTitle("When the Location Mode is no longer valid, reset thermostat mode to")+" (optional)", type: 'enum', 
							required: false, multiple: false, options:statModes, submitOnChange: true)
				}
				//input(name: 'notify', type: 'bool', title: inputTitle("Notify on Activations?"), required: false, defaultValue: false, submitOnChange: true)
				//paragraph HE ? "A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken\n" : "A notification is always sent to the Hello Home log whenever an action is taken\n"
			}			 
			if (ST) {
				section("Notifications") {
					input(name: "notify", type: "bool", title: inputTitle("Notify on Actions?"), required: true, defaultValue: false, submitOnChange: true, width: 3)
					if (settings.notify) {
						input(name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, 
							  required: ((settings?.phone == null) && !settings.notifiers && !settings.speak), submitOnChange: true)
						input(name: "notifiers", type: "capability.notification", title: "Select Notification Devices", hideWhenEmpty: true,
							  required: ((settings.phone == null) && !settings.speak && !settings.pushNotify), multiple: true, submitOnChange: true)
                        if (settings?.notifiers) {
                            List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                            if (echo) {
                            	input(name: "echoAnnouncements", type: "bool", title: "Use ${echo.size()>1?'simultaneous ':''}Announcements for the Echo Speaks device${echo.size()>1?'s':''}?", 
                                	  defaultValue: false, submitOnChange: true)
                            }
                        }
						input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777; +441234567890)", 
							  required: (!settings.pushNotify && !settings.notifiers && !settings.speak), submitOnChange: true)
					}
				}
				if (settings.notify) {
					section(hideWhenEmpty: (!"speechDevices" && !"musicDevices"), title: "") {
						input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", 
								  multiple: true, hideWhenEmpty: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", 
								  multiple: true, hideWhenEmpty: true, submitOnChange: true)
							if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
					}
				}
				if (maximize)  {
					section() {
						paragraph ( "A notification is always sent to the Hello Home log whenever an action is taken")
					}
				}
			} else {		// HE
				section(sectionTitle("Notifications")) {
					input(name: "notify", type: "bool", title: inputTitle("Notify on Actions?"), required: true, defaultValue: false, submitOnChange: true, width: 3)
					if (settings.notify) {
						input(name: "notifiers", type: "capability.notification", multiple: true, title: inputTitle("Select Notification Devices"), submitOnChange: true,
							  required: (!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null))))
                        if (settings?.notifiers) {
                            List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                            if (echo) {
                            	input(name: "echoAnnouncements", type: "bool", title: "Use ${echo.size()>1?'simultaneous ':''}Announcements for the Echo Speaks device${echo.size()>1?'s':''}?", 
                                	  defaultValue: false, submitOnChange: true)
                            }
                        }
					}
				}
				if (settings.notify) {
					section(hideWhenEmpty: (!"speechDevices" && !"musicDevices"), title: "") {
						input(name: "speak", type: "bool", title: inputTitle("Speak messages?"), required: !settings?.notifiers, defaultValue: false, submitOnChange: true, width: 6)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: inputTitle("Select speech devices"), 
								  multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: inputTitle("Select music devices"), 
								  multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
							input(name: "volume", type: "number", range: "0..100", title: inputTitle("At this volume (%)"), defaultValue: 50, required: false, width: 4)
						}
					}
				}
				if (maximize) {
					section(){
						paragraph "A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken"		
					}
				}
			}
			if (true) { //((settings?.notify) && (settings?.pushNotify || settings?.phone || settings?.notifiers || (settings?.speak && (settings?.speechDevices || settings?.musicDevices)))) {
				section() {
					href name: "customNotifications", title: inputTitle("Customize Notifications"), page: "customNotifications", 
								 description: "Customize notification messages", state: isCustomized()
				}
			}
		}
		section(title: sectionTitle("Operations")) {
			input(name: "minimize",		title: inputTitle("Minimize settings text"),	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
			input(name: "tempDisable",	title: inputTitle("Pause this Helper"),			type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)				   
			input(name: "debugOff",		title: inputTitle("Disable debug logging"),		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
			input(name: "infoOff",		title: inputTitle("Disable info logging"),		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
		}		
		// Standard footer
		if (ST) {
			section(getVersionLabel().replace('er, v',"er\nV")+"\n\nCopyright \u00a9 2017-2020 Barry A. Burke\nAll rights reserved.\n\nhttps://github.com/SANdood/Ecobee-Suite") {}
		} else {
			section() {
				paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br><small>Copyright \u00a9 2017-2020 Barry A. Burke - All rights reserved.</small><br>"+
						  "<a href='https://github.com/SANdood/Ecobee-Suite' target='_blank' style='color:#5BBD76'><u>Click here for the Ecobee Suite GitHub Repository</u></a></div>")
			}
		}
	}
}


def customNotifications(){
	String pageLabel = getVersionLabel()
	pageLabel = pageLabel.take(pageLabel.indexOf(','))
	dynamicPage(name: "customNotifications", title: pageTitle("${pageLabel}\nCustom Notifications"), uninstall: false, install: false) {
		section(sectionTitle("Customizations")) {}
		section(smallerTitle("Notification Prefix")) {
			input(name: "customPrefix", type: "enum", title: inputTitle("Notification Prefix text:"), defaultValue: "(helper) at (location):", required: false, submitOnChange: true, 
				  options: ['(helper):', '(helper) at (location):', '(location):', 'none', 'custom'], multiple: false)
			if (settings?.customPrefix == null) { app.updateSetting('customPrefix', '(helper) at (location):'); settings.customPrefix = '(helper) at (location):'; }
			if (settings.customPrefix == 'custom') {
				input(name: "customPrefixText", type: "text", title: inputTitle("Custom Prefix text"), defaultValue: "", required: true, submitOnChange: true)
			}
		}
		section(smallerTitle("Thermostat")) {
			input(name: "customTstat", type: "enum", title: inputTitle("Refer to the HVAC system as"), defaultValue: "(thermostat names)", options:
				  ['the thermostat', 'the HVAC system', '(thermostat names)', 'custom'], submitOnChange: true, multiple: false)
			if (settings?.customTstat == 'custom') {
				input(name: "customTstatText", type: "text", title: inputTitle("Custom HVAC system text"), defaultValue: "", required: true, submitOnChange: true)
			} 
			if (settings?.customTstat == null) { app.updateSetting('customTstat', '(thermostat names)'); settings.customTstat = '(thermostat names)'; }
			if (settings?.customTstat == '(thermostat names)') {
				input(name: "tstatCleaners", type: 'enum', title: inputTitle("Strip these words from the Thermostat display names"), multiple: true, required: false,
					  submitOnChange: true, options: ['EcobeeTherm', 'EcoTherm', 'Thermostat', 'Ecobee'].sort(false))
				input(name: "tstatPrefix", type: 'enum', title: inputTitle("Add this prefix to the Thermostat display names"), multiple: false, required: false,
					  submitOnChange: true, options: ['the', 'Ecobee', 'thermostat', 'Ecobee thermostat', 'the Ecobee', 'the Ecobee thermostat', 'the thermostat'].sort(false)) 
				input(name: "tstatSuffix", type: 'enum', title: inputTitle("Add this suffix to the Thermostat display names"), multiple: false, required: false,
					  submitOnChange: true, options: ['Ecobee', 'HVAC', 'HVAC system', 'thermostat'])
			}
		}
		section(title: sampleTitle("Sample Notification Messages"), hideable: true, hidden: false) {
			String unit = getTemperatureScale()
			String thePrefix = getMsgPrefix()
			String theTstats = getMsgTstat()
			String theTstat = getMsgTstat([thermostats[0]])
			String theLowTemp = unit == 'F' ? 62 : 18
			String theHighTemp = unit == 'F' ? 80 : 28
			String samples = ""
			def tc = thermostats.size()
			boolean multiple = false
			
			if (settings?.aboveCool || settings?.belowHeat) {
				String tt = theTstat.endsWith('s') ? theTstat + "'" : theTstat + "'s"
				samples = getMsgPrefix() + "${tt} inside temperature is ${theLowTemp}°, so I changed it to 'Heat' mode\n"
			}
			samples = samples + "The outside temperature is ${theHighTemp}°${unit}, so I changed ${getMsgTstat()} to 'Cool' mode\n"
						
	  /*	  
			if (ST && settings.wfhPhrase) {
				samples = samples + thePrefix + "I executed '${settings.wfhPhrase}' because ${who} ${becauseText(who)}\n"
				multiple = true
			}
			if (settings.setMode) {
				samples = samples + thePrefix + "I ${multiple?'also ':''}changed Location Mode to ${settings.setMode}\n"
				multiple = true
			}
			if (settings.onAway) {
				samples = samples + thePrefix + "${thePrefix}: I ${multiple?'also ':''}reset ${theTstat} to the '${settings.homeProgram}' program because Thermostat ${myThermostats[0].displayName} "
				"changed to '${settings.awayPrograms[0]}' and ${who} ${becauseText(who)}\n"
				multiple = true
			}
			if (settings.setHome) {
				samples = samples + thePrefix + "I ${multiple?'also ':''}changed ${theTstat} to the '${settings.homeProgram}' program because ${who} ${becauseText(who)}"
			}
			*/
			paragraph samples
		}
	}
}

def isCustomized() {
	return (customPrefix || customTstat || (useSensorNames != null)) ? "complete" : null
}

void installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
	atomicState.aboveChanged = false
	atomicState.betweenChanged = false
	atomicState.belowChanged = false
	atomicState.dewpoint = null
	atomicState.humidity = null
	initialize()  
}
void uninstalled() {
	clearReservations()
}
void clearReservations() {
	thermostats?.each {
		cancelReservation(getDeviceId(it.deviceNetworkId), 'modeOff' )
	}
}
void updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
	unschedule()
	atomicState.aboveChanged = false
	atomicState.betweenChanged = false
	atomicState.belowChanged = false
	atomicState.dewpoint = null
	atomicState.humidity = null
	initialize()
}
boolean initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	updateMyLabel()
	runEvery15Minutes(updateMyLabel)
	
	if (settings.tempDisable) {
		clearReservations()
		LOG("Temporarily Paused", 3, null, 'info')
		return true
	}
	if (settings.debugOff) log.info "log.debug() logging disabled"
	
	if (settings.aboveCool || settings.belowHeat || settings.insideAuto) {
		subscribe(thermostats, 'temperature', insideChangeHandler)
	} else {
		atomicState.insideOverride = [:]
	}
	
	if (settings.aboveTemp || settings.belowTemp) {
		subscribe(thermostats, 'thermostatMode', thermostatModeHandler)
	}
	def tempNow
	def gu = getTemperatureScale()
	//boolean ST = atomicState.isST
	switch( settings.tempSource) {
		case 'location':			
			// SmartThings Only
			def WUname = (settings.latLon) ? 'getGPSTemp' : 'getZipTemp'
			if (settings.locFreq.toInteger() < 60) {
				"runEvery${settings.locFreq}Minute${settings.locFreq!='1'?'s':''}"( "${WUname}" )
			} else {
				def locHours = settings.locFreq.toInteger() / 60
				"runEvery${locHours}Hour${locHours!=1?'s':''}"( "${WUname}" )
			}
			def t = "${WUname}"()					// calls temperatureUpdate() & stores dewpoint
			if (t != null) tempNow = t as BigDecimal
			break;
		
		case 'sensors':
			if (settings.dewBelowOverride || (settings.dewAboveTemp != null)) {
				if (settings.humidistat) { 
					subscribe( settings.humidistat, 'relativeHumidity', humidityChangeHandler)
				} else {
					log.error "Dewpoint override(s) enabled, but no humidistat selected - initialization FAILED."
					return false
				}
			}
			subscribe( settings.thermometer, 'temperature', tempChangeHandler)
			def latest = ST ? settings.thermometer.currentState("temperature") : settings.thermometer.currentState("temperature", true)
			def unit = latest.unit
			def t 
			if (latest.numberValue != null) {
				t = roundIt(latest.numberValue, (unit=='C'?2:1))
				if (dewBelowOverride) {
					latest = ST ? settings.humidistat.currentState('humidity') : settings.humidistat.currentState('humidity', true)
					if (latest.value != null) {
						def h = latest.numberValue
						atomicState.humidity = h
						LOG("Outside Humidity is: ${h}%",3,null,'info')
						def d = calculateDewpoint( t, h, unit )
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
					}
				}
				tempNow = t 
				temperatureUpdate(tempNow) 
			}
			break;
			
		case 'station':
			if (settings.smartWeather) {
				// ST only, temperature & humidity, no dewpoint
				subscribe(settings.smartWeather, 'temperature', tempChangeHandler)
				def latest = ST ? settings.smartWeather.currentState('temperature') : settings.smartWeather.currentState('temperature', true)
				def t = latest.numberValue
				def unit = latest.unit
				if (t != null) {
					t = latest.numberValue
					if (settings.dewBelowOverride) {
						subscribe(settings.smartWeather, 'relativeHumidity', humidityChangeHandler)
						latest = ST ? settings.smartWeather.currentState('relativeHumidity') : settings.smartWeather.currentState('relativeHumidity', true)
						if (latest?.numberValue != null) {
							def h = roundIt(latest.numberValue, (unit=='C'?2:1))
							LOG("Outside Humidity is: ${h}%",3,null,'info')
							def d = calculateDewpoint( t, h, unit )
							atomicState.dewpoint = d
							LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
						}
					}
					tempNow = t 
					temperatureUpdate(tempNow) 
				}
			} else if (settings.smartWeather2) {
				// ST only, has temperature & dewpoint
				def latest
				if (settings.dewBelowOverride) {
					subscribe(settings.smartWeather2, 'dewpoint', dewpointChangeHandler)
					latest = settings.smartWeather2.currentState('dewpoint')
					if (latest?.numberValue != null) {
						def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
					}
				}
				subscribe(settings.smartWeather2, 'temperature', tempChangeHandler)
				latest = settings.smartWeather2.currentState('temperature')
				if (latest?.numberValue != null) { 
					tempNow = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
					temperatureUpdate(tempNow) 
				}
			} else if (settings.ambientWeatherDevice) {
				// HE only, has temperature & dewpoint
				def latest
				if (settings.dewBelowOverride) {
					subscribe(settings.ambientWeatherDevice, 'dewpoint', dewpointChangeHandler)
					latest = ST ? settings.ambientWeatherDevice.currentState('dewpoint') : settings.ambientWeatherDevice.currentState('dewpoint', true)
					if (latest?.numberValue != null) {
						def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
					}
				}
				subscribe(settings.ambientWeatherDevice, 'temperature', tempChangeHandler)
				latest = ST ? settings.ambientWeatherDevice.currentState('temperature') : settings.ambientWeatherDevice.currentState('temperature', true)
				if (latest?.numberValue != null) { 
					tempNow = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
					temperatureUpdate(tempNow) 
				}
			} else if (settings.darkSkyNetWeatherDriver) {
				// HE only, optional dewpoint so we calculate ourselves
				subscribe(settings.darkSkyNetWeatherDriver, 'temperature', tempChangeHandler)
				def latest = settings.darkSkyNetWeatherDriver.currentState('temperature', true)
				def t = latest.numberValue
				def unit = latest.unit
				if (t != null) {
					t = latest.numberValue
					if (settings.dewBelowOverride) {
						subscribe(settings.darkSkyNetWeatherDriver, 'relativeHumidity', humidityChangeHandler)
						latest = settings.darkSkyNetWeatherDriver.currentState('relativeHumidity', true)
						if (latest?.numberValue != null) {
							def h = roundIt(latest.numberValue, (unit=='C'?2:1))
							LOG("Outside Humidity is: ${h}%",3,null,'info')
							def d = calculateDewpoint( t, h, unit )
							atomicState.dewpoint = d
							LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
						}
					}
					tempNow = t 
					temperatureUpdate(tempNow) 
				}
			} else if (settings.netatmoOutdoorModule) {
				// ST & HE No dewpoint - calculate ourselves
				subscribe(settings.netatmoOutdoorModule, 'temperature', tempChangeHandler)
				def latest = ST ? settings.netatmoOutdoorModule.currentState('temperature') : settings.netatmoOutdoorModule.currentState('temperature', true)
				def t = latest.numberValue
				def unit = latest.unit
				if (t != null) {
					t = latest.numberValue
					if (settings.dewBelowOverride) {
						subscribe(settings.netatmoOutdoorModule, 'relativeHumidity', humidityChangeHandler)
						latest = ST ? settings.netatmoOutdoorModule.currentState('relativeHumidity') : settings.netatmoOutdoorModule.currentState('relativeHumidity', true)
						if (latest?.numberValue != null) {
							def h = roundIt(latest.numberValue, (unit=='C'?2:1))
							LOG("Outside Humidity is: ${h}%",3,null,'info')
							def d = calculateDewpoint( t, h, unit )
							atomicState.dewpoint = d
							LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
						}
					}
					tempNow = t 
					temperatureUpdate(tempNow) 
				}
			} else if (settings.meteoWeather) {
				// HE & ST, has temperature & dewpoint
				def latest
				if (settings.dewBelowOverride) {
					subscribe(settings.meteoWeather, 'dewpoint', dewpointChangeHandler)
					latest = ST ? settings.meteoWeather.currentState('dewpoint') : settings.meteoWeather.currentState('dewpoint', true)
					if (latest?.numberValue != null) {
						def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
					}
				}
				subscribe(settings.meteoWeather, 'temperature', tempChangeHandler)
				latest = ST ? settings.meteoWeather.currentState('temperature') : settings.meteoWeather.currentState('temperature', true)
				if (latest?.numberValue != null) { 
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
				latest = ST ? theStat.currentState('weatherDewpoint') : theStat.currentState('weatherDewpoint', true)
				if (latest?.numberValue != null) {
					def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
					atomicState.dewpoint = d
					LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
				}
			}
			subscribe(theStat, 'weatherTemperature', tempChangeHandler)
			latest = ST ? theStat.currentState('weatherTemperature') : theStat.currentState('weatherTemperature', true)
			if (latest?.numberValue != null) {
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
			if (t != null) tempNow = t as BigDecimal
			break;
	}
	atomicState.locModeEnabled = settings.theModes ? settings.theModes : true
	if (settings.theModes) {
		subscribe(location, locationModeChangeHandler)
		atomicState.locModeEnabled = settings.theModes.contains(location.mode)
	} else { atomicState.locModeEnabled = true }
	
	if (tempNow) {
		atomicState.temperature = tempNow
		LOG("Initialization complete...current Outside Temperature is ${tempNow}°${gu} - checking...",2,null,'info')
		atomicTempUpdater()
		return true
	} else {
		LOG("Initialization error...invalid temperature: ${tempNow}°${gu} - please check settings and retry", 2, null, 'error')
		return false
	}
}

def locationModeChangeHandler(evt) {
	// not using Location Mode filter
	updateMyLabel()
	if (!settings.theModes) {
		if (atomicState.temperature) atomicTempUpdater()
		return	
	} else if (settings.theModes.contains(evt.value)) {
		if (atomicState.temperature) atomicTempUpdater()
	} else {
		atomicState.locModeEnabled = false
	}
}

def insideChangeHandler(evt) {
	def theTemp = evt.numberValue
	def unit = getTemperatureScale()
	def diffAdjust = ((unit == 'F') ? -0.1 : -0.055) as BigDecimal
	if (theTemp == null) {
		LOG("Ignoring invalid temperature: ${theTemp}°${unit}", 2, null, 'warn')
		return
	}
	theTemp = roundIt(theTemp, (unit=='C'?2:1))
	LOG("${evt.device.displayName} Temperature is: ${theTemp}°${unit}",3,null,'info')
	
	def newMode = null
	def insideOverride = atomicState.insideOverride ?: [:]
	boolean coolOverride = false
	boolean heatOverride = false
	//boolean ST = atomicState.isST
	String tid = getDeviceId(evt.device.deviceNetworkId)
	
	if (theTemp != null) {
		def coolSP = (ST ? evt.device.currentValue('coolingSetpoint') : evt.device.currentValue('coolingSetpoint', true)) as BigDecimal
		if (coolSP != null) {
			coolSP += diffAdjust
			def coolDiff = (ST ? evt.device.currentValue('coolDifferential') : evt.device.currentValue('coolDifferential', true)) as BigDecimal
			if (theTemp >= (coolSP + coolDiff)) {
				String cMode = ST ? evt.device.currentValue('thermostatMode') : evt.device.currentValue('thermostatMode', true)
				if (settings.aboveCool) {
					newMode = 'cool'
					coolOverride = true
				} else if (settings.insideAuto && (cMode != 'cool')) {
					newMode = 'auto'
					// coolOverride = true
				}
			}
		}
		if (newMode == null) {
			def heatSP = (ST ? evt.device.currentValue('heatingSetpoint') : evt.device.currentValue('heatingSetpoint', true)) as BigDecimal
			if (heatSP != null) {
				def heatDiff = (ST ? evt.device.currentValue('heatDifferential') : evt.device.currentValue('heatDifferential', true)) as BigDecimal
				if (theTemp <= (heatSP - heatDiff)) {
					heatSP += diffAdjust
					String cMode = ST ? evt.device.currentValue('thermostatMode') : evt.device.currentValue('thermostatMode', true)
					if (settings.belowHeat) {
						newMode = 'heat'
						heatOverride = true
					} else if (settings.insideAuto && (cMode != 'heat')) {
						newMode = 'auto'
						// heatOverride = true
					}
				}
			}
		}
		
		def okMode = settings.theModes ? settings.theModes.contains(location.mode) : true
		if (okMode) {
			atomicState.locModeEnabled = true
			if (newMode != null) {
				String cMode = ST ? evt.device.currentValue('thermostatMode') : evt.device.currentValue('thermostatMode', true)
				log.debug "newMode: ${newMode}, cMode: ${cMode}"
				if (cMode != newMode) {
					boolean override = ((cMode != 'off') || (settings.insideOverridesOff && (!anyReservations(tid, 'modeOff') || ((countReservations(tid, 'modeOff') == 1) && haveReservation(tid, 'modeOff')))))
					if (!override) {
						// if Anybode else (but not me) has a reservation on this being off, I can't turn it back on
						insideOverride[tid] = false
						LOG("${evt.device.displayName} inside temp is ${theTemp}°${evt.unit}, but can't change to ${newMode} since ${getGuestList(tid,'offMode').toString()[1..-2]} have offMode reservations",2,null,'warn')
						// Here's where we could subscribe to reservations and re-evaluate. For now, just wait for another inside Temp Change to occur
					} else {
						// not currently off or there are no modeOff reservations (other than my own), change away!
						cancelReservation(tid, 'modeOff' )
						insideOverride[tid] = (coolOverride || heatOverride)
						evt.device.setThermostatMode(newMode)
						LOG("${evt.device.displayName} temperature (inside) is ${theTemp}°${evt.unit}, changed thermostat to ${newMode} mode",3,null,'trace')
						String theTstat = getMsgTstat([evt.device])
						String tt = theTstat.endsWith('s') ? theTstat + "'" : theTstat + "'s"
						sendMessage("${tt} inside temperature is ${theTemp}°, so I changed it to '${newMode}' mode")
					}
				}
			} else {
				insideOverride[tid] = false
			}
		} else {
			if (atomicState.locModeEnabled) {
				// Mode no longer valid, but it once was, so release any reservations and leave the HVAC where it is...
				LOG("Location Mode (${location.mode}) is no longer valid, releasing reservations${settings.doneMode?', and resetting Thermostat Mode to '+settings.doneMode.toString().capitalize():''}",2,null,'info')
				cancelReservation(tid, 'modeOff')
				if (!anyReservations(tid, 'modeOff')) {
					if (settings.doneMode) thermostats*."${settings.doneMode}"()
				}
				atomicState.locModeEnabled = false
			}
		}
	}
	atomicState.insideOverride = insideOverride
	updateMyLabel()
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
	updateMyLabel()
}

def tempChangeHandler(evt) {
	//boolean ST = atomicState.isST
	
	if (evt.numberValue != null) {
		def t = roundIt(evt.numberValue, (evt.unit=='C'?2:1))
		atomicState.temperature = t
		if (settings.dewBelowOverride || settings.dewAboveTemp) {
			// We have to update the dewpoint every time the temperature (or humidity) changes
			if (atomicState.humidity != null) {
				// Somebody is updating atomicState.humidity, so we need to calculate the dewpoint
				// (Sources that provide dewpoint directly will not update atomicState.humidity)
				if (settings.tempSource == 'sensors') {	   
					def latest = ST ? settings.humidistat.currentState('humidity') : settings.humidistat.currentState('humidity', true)
					if (latest.numberValue != null) {
						def h = latest.numberValue
						atomicState.humidity = h
						LOG("Outside Humidity is: ${h}%",3,null,'info')
						def d = calculateDewpoint( t, h, evt.unit )
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${evt.unit}",3,null,'info')
						runIn(2, atomicTempUpdater, [overwrite: true] )		// humidity might be updated also
						return
					}
				} else if (settings.tempSource == 'station') {
					def latest
					if (settings.smartWeather) {						// ST only
						latest = settings.smartWeather.currentState('relativeHumidity')
					} else if (settings.darkSkyNetWeatherDriver) {		// HE only
						latest = settings.darkSkyNetWeatherDriver.currentState('relativeHumidity', true)
					} else if (settings.netatmoOutdoorModule) {			// both ST & HE
						latest = ST ? settings.netatmoOutdoorModule.currentState('relativeHumidity') : settings.netatmoOutdoorModule.currentState('relativeHumidity', true)
					}
					if (latest.numberValue != null) {
						h = latest.numberValue
						LOG("Outside Humidity is: ${h}%",3,null,'info')
						def d = calculateDewpoint( t, h, unit )
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${evt.unit}",3,null,'info')
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
	if (evt.numberValue != null) {
		def d = roundIt(evt.numberValue, (evt.unit=='C'?2:1))
		atomicState.dewpoint = d
		LOG("Outside Dewpoint is: ${d}°${evt.unit}",3,null,'info')
		runIn(2, atomicTempUpdater, [overwrite: true])		// wait for temp to be updated also
	}
}

def humidityChangeHandler(evt) {
	if (evt.numberValue != null) {
		t = atomicState.temperature
		u = getTemperatureScale()
		atomicState.humidity = evt.numberValue
		LOG("Outside Humidity is: ${evt.numberValue}%",3,null,'info')
		def d = calculateDewpoint(t, evt.numberValue, u)
		atomicState.dewpoint = d
		LOG("Outside Dewpoint is: ${d}°${getTemperatureScale()}",3,null,'info')
		runIn(2, atomicTempUpdater, [overwrite: true])
	}
}

def atomicTempUpdater() {
	temperatureUpdate( atomicState.temperature )
}

def temperatureUpdate( temp ) {
	if (temp != null) temperatureUpdate(temp as BigDecimal)
}
def temperatureUpdate( BigDecimal temp ) {
	def unit = getTemperatureScale()
	if (temp == null) {
		LOG("Ignoring invalid temperature: ${temp}°${unit}", 2, null, 'warn')
		return false
	}
	//boolean ST = atomicState.isST
	
	temp = roundIt(temp, (unit=='C'?2:1))
	atomicState.temperature = temp
	LOG("Outside Temperature is: ${temp}°${unit}",3,null,'info')
	
	def okMode = settings.theModes ? settings.theModes.contains(location.mode) : true
	if (okMode) {
		atomicState.locModeEnabled = true
	} else {
		if (atomicState.locModeEnabled) {
			LOG("Location Mode (${location.mode}) is no longer valid, releasing reservations${settings.doneMode?', and resetting Thermostat Mode to '+settings.doneMode.toString().capitalize():''}",2,null,'info')
			// release all the reservations 
			settings.thermostats.each { 
				def tid = getDeviceId(it.deviceNetworkId)
				cancelReservation(tid, 'modeOff')
				if (!anyReservations(tid, 'modeOff')) {
					if (settings.doneMode) thermostats*."${settings.doneMode}"() 
				}
			}
			atomicState.locModeEnabled = false
		}
		return
	}
	
	def desiredMode = null
	def desiredProgram = null
	if ((settings.aboveTemp && (temp >= settings.aboveTemp)) || (settings.dewAboveTemp && (atomicState.dewpoint >= settings.dewAboveTemp))) {
		if (!atomicState.aboveChanged) {
			desiredMode =		settings.aboveMode
			desiredProgram =	settings.aboveSchedule
			if (settings.aboveSetpoints) {
				changeSetpoints(settings.aboveProgram, settings.aboveHeatTemp, settings.aboveCoolTemp)
			}
			atomicState.aboveChanged = true
			atomicState.betweenChanged = false
			atomicState.belowChanged = false
		}
	} else if (settings.belowTemp && (temp <= settings.belowTemp)) {
		if (!atomicState.belowChanged) {
			// We haven't already changed to belowMode/belowSchedule
			if (settings.belowMode && (settings.belowMode != 'off')) {
				// not turning off
				desiredMode = settings.belowMode
				desiredProgram = settings.belowSchedule
				if (settings.belowSetpoints) {
					changeSetpoints(settings.belowProgram, settings.belowHeatTemp, settings.belowCoolTemp)
				}
			} else if ((settings.belowMode && (settings.belowMode == 'off')) && (!settings.dewBelowOverride || (settings.dewBelowTemp > atomicState.dewpoint))) {
				// belowMode is 'off', but we don't need to do dewpointOverride just adjust the Program and/or Setpoints
				desiredMode = null
				desiredProgram = settings.belowSchedule
				if (settings.belowSetpoints) {
					changeSetpoints(settings.belowProgram, settings.belowHeatTemp, settings.belowCoolTemp)
				}
			} else if (!settings.belowMode) {
				// belowMode changes aren't configured, just do schedule and setpoints
				desiredMode = null
				desiredProgram = settings.belowSchedule
				if (settings.belowSetpoints) {
					changeSetpoints(settings.belowProgram, settings.belowHeatTemp, settings.belowCoolTemp)
				}
			} else {
				// assert ((settings.belowMode == 'off') && (settings.dewBelowOverride && (settings.dewBelowTemp < atomicState.dewpoint))) // don't turn off
				desiredMode = null
				desiredProgram = settings.belowSchedule
				if (settings.belowSetpoints) {
					changeSetpoints(settings.belowProgram, settings.belowHeatTemp, settings.belowCoolTemp)
				}
				
			}
			atomicState.aboveChanged = false
			atomicState.betweenChanged = false
			atomicState.belowChanged = true
		} else {
			// We have prior changed to the belowMode/belowSchedule - now we have to check if dewpoint is still below the limit
			if ((settings.belowMode == 'off') && settings.dewBelowOverride && (settings.dewBelowTemp <= atomicState.dewpoint)) {
				// Uh-oh, the dewpoint has risen into the bad land, and we are supposedly off at the moment
				if (settings.betweenMode || settings.betweenSetpoints || settings.betweenSchedule) {
					// We have a between mode - let's change back to that
					desiredMode =		settings.betweenMode
					desiredProgram =	settings.betweenSchedule
					if (settings.betweenSetpoints) {
						changeSetpoints(settings.betweenProgram, settings.betweenHeatTemp, settings.betweenCoolTemp)
					}
					atomicState.aboveChanged = false
					atomicState.betweenChanged = true
					atomicState.belowChanged = true		// so we don't change it again
				} else if (settings.aboveMode || settings.aboveSetpoints) {
					// OK, no between mode. But we have an above mode - switch to that
					desiredMode =		settings.aboveMode
					desiredProgram =	settings.aboveProgram
					if (settings.aboveSetpoints) {
						changeSetpoints(settings.aboveProgram, settings.aboveHeatTemp, settings.aboveCoolTemp)
					}
					atomicState.aboveChanged = true
					atomicState.betweeChanged = false
					atomicState.belowChanged = true		// so we don't change it again...
				}
			} else if (!settings.belowMode || (!settings.dewBelowTemp || (settings.dewBelowTemp > atomicState.dewpoint))) {
				// No belowMode, or not doing dewpointOverride - return to belowMode settings
				desiredMode = settings.belowMode
				desiredProgram = settings.belowSchedule
				if (settings.belowSetpoints) {
					changeSetpoints(settings.belowProgram, settings.belowHeatTemp, settings.belowCoolTemp)
				}
				atomicState.aboveChanged = false
				atomicState.betweenChanged = false
				atomicState.belowChanged = true
			}
		}
	} else if ((settings.aboveTemp || (settings.dewAboveTemp && (atomicState.dewpoint < settings.dewAboveTemp))) && settings.belowTemp && settings.betweenMode) {
		if (!atomicState.betweenChanged) {
			desiredMode =		settings.betweenMode
			desiredProgram =	settings.betweenSchedule
			if (settings.betweenSetpoints) {
				changeSetpoints(settings.betweenProgram, settings.betweenHeatTemp, settings.betweenCoolTemp)
			}
			atomicState.aboveChanged = false
			atomicState.betweenChanged = true
			atomicState.belowChanged = false
		}
	}
	
	if ((desiredMode != null) || (desiredProgram != null)) {
		String changeNames = ""
		def changeNamesList = []
		String sameNames = ""
		def insideOverride = atomicState.insideOverride
		settings.thermostats.each { 
			String cMode = ST ? it.currentValue('thermostatMode') : it.currentValue('thermostatMode', true)
			String cProgram = ST ? it.currentValue('currentProgramName') : it.currentValue('currentProgramName', true)
			String tid = getDeviceId(it.deviceNetworkId)
			if (!insideOverride || !insideOverride.containsKey(tid) || !insideOverride[tid]) {
				if ((cMode && (cMode != desiredMode)) || (cProgram && (cProgram != desiredProgram))) {
					if ((desiredMode == 'off') && (cMode != 'off')) {
						makeReservation(tid, 'modeOff')
						it.setThermostatMode( 'off' )
					} else {
						// Desired mode IS NOT 'off'
						if (cMode == 'off') {
							cancelReservation(tid,'modeOff')
							if (countReservations(tid, 'modeOff') == 0) {
								// nobody else has a reservation on modeOff
								if (desiredMode && (cMode != desiredMode)) it.setThermostatMode(desiredMode)
								if (desiredProgram && (cProgram != desiredProgram)) {
									def sendHoldType = whatHoldType(stat)
									def sendHoldHours = null
									if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
										sendHoldHours = sendHoldType
										sendHoldType = 'holdHours'
									}
									LOG("sendHoldType: ${sendHoldType}, sendHoldHours: ${sendHoldHours}",3,null,'info')
									it.setThermostatProgram(desiredProgram, sendHoldType, sendHoldHours)
								}
								changeNames += changeNames ? ", ${it.displayName}" : it.displayName
								changeNamesList << it
							} else {
								// somebody else still has a 'modeOff' reservation so we can't turn it on
								def reservedBy = getGuestList(tid,'modeOff')
								LOG("Reservations: ${reservedBy}", 3, null, 'debug')
								if (reservedBy == []) reservedBy = ['somebody']
								def msg = "The outside temperature is ${temp}°${unit}, but I can't change ${getMsgTstat([it])} to '${desiredMode}' Mode because ${reservedBy.toString()[1..-2]} hold 'modeOff' reservations"
								LOG(msg ,2,null,'warn')
								sendMessage(msg)
								// here's where we COULD subscribe to the reservations to see when we can turn it back on. For now, let's just let whomever is last deal with it
							}
						} else {
							// Not off currently, so we can change freely
							boolean changed = false
							cancelReservation(tid, 'modeOff')	// just in case
							if (desiredMode && (cMode != desiredMode)) { it.setThermostatMode(desiredMode); changed = true }
							if (desiredProgram && (cProgram != desiredProgram)) {
								def sendHoldType = whatHoldType(stat)
								def sendHoldHours = null
								if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
									sendHoldHours = sendHoldType
									sendHoldType = 'holdHours'
								}
								LOG("sendHoldType: ${sendHoldType}, sendHoldHours: ${sendHoldHours}",3,null,'info')
								it.setThermostatProgram(desiredProgram, sendHoldType, sendHoldHours)
								changed = true
							}
							if (changed) {
								changeNames += changeNames ? ", ${it.displayName}" : it.displayName
								changeNamesList << it
							}
						}
					}
				} else {
					// already running the mode we want
					(desiredMode == 'off') ? makeReservation(tid, 'modeOff') : cancelReservation(tid, 'modeOff')
					sameNames += sameNames ? ", ${it.displayName}" : it.displayName
				}
			} else {
				LOG("Inside Temperature has overridden calculated Thermostat Mode, will not change ${it.displayName} to ${desiredMode} mode", 2, null, 'info')
			}
		}
		def multi=0
		if (changeNames) {
			LOG("Temp is ${temp}°${unit}, changed ${changeNames} to ${desiredMode} mode",3,null,'trace')
			sendMessage("The outside temperature is ${temp}°${unit}, so I changed ${getMsgTstat(changeNamesList)} to '${desiredMode}' mode")
		}
		if (sameNames) LOG("Temp is ${temp}°${unit}, ${sameNames} already in ${desiredMode} mode",3,null,'info')
	}
	updateMyLabel()
}

void changeSetpoints( program, heatTemp, coolTemp ) {
	def unit = getTemperatureScale()
	settings.thermostats.each { stat ->
		LOG("Setting ${stat.displayName} '${program}' heatingSetpoint to ${heatTemp}°${unit}, coolingSetpoint to ${coolTemp}°${unit}",2,null,'info')
		String tid = stat.identifier.toString()
		boolean anyReserved = anyReservations( tid, 'programChange' )		// need to make sure nobody changes the program Map out from beneath us
		if (!anyReserved || haveReservation( tid, 'programChange' )) {
			// Nobody has a reservation, or the reservation is mine
			if (!anyReserved) makeReservation(tid, 'programChange')
			if (needSetpointChange(stat, program, heatTemp, coolTemp)) makeSetpointChange(stat, program, heatTemp, coolTemp)
		} else {
			// somebody else has a reservation - we have to wait
			def pendedUpdates = atomicState.pendedUpdates as Map
			pendedUpdates[tid] = [program: program, heat: heatTemp, cool: coolTemp]
			atomicStates.pendedUpdates = pendedUpdates
			subscribe(stat, 'climatesUpdated', programWaitHandler)
			//LOG("Delayed: Sensor ${sensor.displayName} will be added to ${settings.theClimates.toString()[1..-2]} and removed from ${notPrograms.toString()[1..-2]} when pending changes complete",2,null,'info')
		}
	}
}

def getZipTemp() {
	return getTwcTemp('zip')
}

def getGPSTemp() {
	return getTwcTemp('gps')
}

def getPwsTemp() {
	return getWUTemp('pws')
}

// SmartThings-only
def getTwcTemp(type) {
	def isMetric = (getTemperatureScale() == "C")
	def source = (type == 'zip') ? settings.zipCode : ((type == 'gps')?"${location.latitude},${location.longitude}":null)
	
	def twcConditions = [:]
	try {
		twcConditions = getTwcConditions(source)
	} catch (e) {
		LOG("Error getting TWC Conditions: ${e}",1,null,'error')
		return null
	}
	if (twcConditions) {
		LOG("Parsing TWC data",3,null,'info')
		def tempNow
		def dewpointNow = -999.0
		tempNow = twcConditions.temperature
		dewpointNow = twcConditions.temperatureDewPoint

		if (tempNow != null) {
			if (dewpointNow != -999.0) {
				atomicState.dewpoint = dewpointNow
			} else {
				def hum = twcConditions.relativeHumidity
				if ((hum != null) && hum.contains('%')) hum = (hum-'%') as Integer		// strip off the trailing '%' sign
				if (hum.toString().isNumber()) {
					dewpointNow = calculateDewpoint( tempNow, hum, (isMetric?'C':'F'))
				}
				atomicState.dewpoint = dewpointNow
			}
			LOG("Outside Dewpoint is: ${dewpointNow}°${isMetric?'C':'F'}",2,null,'info')
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
// SmartThings only - deprecated
def getWUTemp(type) {
	def isMetric = (getTemperatureScale() == "C")
	def tempNow
	def dewpointNow
	def source = (type == 'zip') ? settings.zipCode : ((type == 'gps')?"${location.latitude},${location.longitude}":settings.stationID)
	Map wdata = getWeatherFeature('conditions', source)
	LOG("Requesting WU data for source: ${source}",3,null,'info')
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
		LOG("Parsing WU data for station: ${wdata.current_observation.station_id}",3,null,'info')
		if (!isMetric) {
			if (wdata.current_observation.temp_f?.isNumber()) tempNow = wdata.current_observation.temp_f.toBigDecimal()
			if (wdata.current_observation.dewpoint_f?.isNumber()) dewpointNow = wdata.current_observation.dewpoint_f.toBigDecimal()
		} else {
			if (wdata.current_observation.temp_c?.isNumber()) tempNow = wdata.current_observation.temp_c.toBigDecimal()
			if (wdata.current_observation.dewpoint_c?.isNumber()) dewpointNow = wdata.current_observation.dewpoint_c.toBigDecimal()
		}
		if (tempNow?.isNumber()) {
			if (dewpointNow != -999.0) {
				atomicState.dewpoint = dewpointNow
			} else {
				def hum = wdata.current_observation.relative_humidity
				if (hum && hum.contains('%')) hum = (hum-'%').toInteger()		// strip off the trailing '%' sign
				if (hum.isNumber()) {
					dewpointNow = calculateDewpoint( tempNow, hum, (isMetric?'C':'F'))
				}
				atomicState.dewpoint = dewpointNow
			}
			LOG("Outside Dewpoint is: ${dewpointNow}°${isMetric?'C':'F'}",2,null,'info')
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
def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def roundIt( BigDecimal value, decimals=0) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}

// return all the modes that ALL thermostats support
def getThermostatModes() {
	def statModes = []
	settings.thermostats?.each { stat ->
		def tm = stat.currentValue('supportedThermostatModes')
		if (statModes == []) {	
			if (tm && (tm != '[]')) statModes = tm[1..-2].tokenize(", ")
		} else {
			def nm = (tm && (tm != '[]')) ? statModes = tm[1..-2].tokenize(", ") : []
			if (nm) statModes = statModes.intersect(nm)
		}	
	}
	return statModes.sort(false)
}

// get the combined set of Ecobee Programs applicable for these thermostats
def getThermostatPrograms() {
	def programs = []
	if (thermostats?.size() > 0) {
		thermostats.each { stat ->
			def progs = []
			def cl = stat.currentValue('climatesList')
			if (cl && (cl != '[]')) {
				progs = cl[1..-2].tokenize(', ')
			} else {
				def pl = stat.currentValue('programsList')
				tp = pl ? new JsonSlurper().parseText(pl) : []
				if (tp) progs = tp
			}
			if (!programs) {
				if (progs) programs = progs
			} else {
				if (progs) programs = programs.intersect(progs)
			}
		}
	} 
	if (!programs) programs =  ['Away', 'Home', 'Sleep']
	LOG("getThermostatPrograms: returning ${programs}", 4, null, 'info')
	return programs.sort(false)
}

// return the external temperature range
def getThermostatRange() {
	def low
	def high
	if (getTemperatureScale() == "C") {
		low = -20.0
		high = 65.0
	} else {
		low = -5.0
		high = 150.0
	}
	return "${low}..${high}"
}

// returns the holdType keyword, OR the number of hours to hold
// precedence: 1. this SmartApp's preferences, 2. Parent settings.holdType, 3. indefinite (must specify to use the thermostat setting)
String whatHoldType(statDevice) {
	def theHoldType = settings.holdType
	def sendHoldType = null
	def parentHoldType = getParentSetting('holdType')
	if ((settings.holdType == null) || (settings.holdType == "Ecobee Manager Setting") || (settings.holdType == 'Parent Ecobee (Connect) Setting')) {
		if ((parentHoldType == null) || (parentHoldType == '')) {	// default for Ecobee (Connect) is permanent hold (legacy)
			LOG('Using holdType indefinite',2,null,'info')
			return 'indefinite'
		} else if (parentHoldType != 'Thermostat Setting') {
			theHoldType = parentHoldType
		}
	}
	
	def parentHoldHours = getParentSetting('holdHours')
	switch (theHoldType) {
		case 'Until I Change':
			sendHoldType = 'indefinite'
			break;	 
		case 'Until Next Program':
			sendHoldType = 'nextTransition'
			break;				 
		case '2 Hours':
			sendHoldType = 2
			break;
		case '4 Hours':
			sendHoldType = 4
		case 'Specified Hours':
			if (settings.holdHours && settings.holdHours.isNumber()) {
				sendHoldType = settings.holdHours
			} else if ((parentHoldType == 'Specified Hours') && (parentHoldHours != null)) {
				sendHoldType = parentHoldHours
			} else if ( parentHoldType == '2 Hours') {
				sendHoldType = 2
			} else if ( parentHoldType == '4 Hours') {
				sendHoldType = 4			
			} else {
				sendHoldType = 2
			}
			break;
		case 'Thermostat Setting':
			String statHoldType = ST ? statDevice.currentValue('statHoldAction') : statDevice.currentValue('statHoldAction', true)
			switch(statHoldType) {
				case 'useEndTime4hour':
					sendHoldType = 4
					break;
				case 'useEndTime2hour':
					sendHoldType = 2
					break;
				case 'nextPeriod':
				case 'nextTransition':
					sendHoldType = 'nextTransition'
					break;
				case 'indefinite':
				case 'askMe':
				case null :
				case '':
				default :
					sendHoldType = 'indefinite'
					break;
		   }
	}
	if (sendHoldType) {
		LOG("Using holdType ${sendHoldType.isNumber()?'holdHours ('+sendHoldType.toString()+')':sendHoldType}",2,null,'info')
		return sendHoldType
	} else {
		LOG("Couldn't determine holdType, returning indefinite",1,null,'error')
		return 'indefinite'
	}
}


def getHeatRange() {
	def low
	def high
	settings.thermostats.each { stat ->
		def lo
		def hi
		def setp = stat.currentValue('heatRangeLow')
		lo = lo ? ((setp < lo) ? setp : lo) : setp
		setp = stat.currentValue('heatRangeHigh')
		hi = hi ? ((setp > hi) ? setp : hi) : setp
		// if there are multiple stats, we need to find the range that ALL stats can support
		low = low ? ((lo > low) ? lo : low) : lo
		high = high ? ((hi < high) ? hi : high) : hi
	}
	return "${roundIt(low-0.5,0)}..${roundIt(high-0.5,0)}"
}

def getCoolRange() {
	def low
	def high
	settings.thermostats.each { stat ->
		def lo
		def hi
		def setp = stat.currentValue('coolRangeLow')
		lo = lo ? ((setp < lo) ? setp : lo) : setp
		setp = stat.currentValue('coolRangeHigh')
		hi = hi ? ((setp > hi) ? setp : hi) : setp
		// if there are multiple stats, we need to find the range that ALL stats can support
		low = low ? ((lo > low) ? lo : low) : lo
		high = high ? ((hi < high) ? hi : high) : hi
	}
	return "${roundIt(low-0.5,0)}..${roundIt(high-0.5,0)}"
}

String getZIPcode() {
	return location.zipCode ?: ""
}

String getPWSID() {
	String PWSID = location.zipCode
	LOG("Location ZIP Code ${PWSID}", 3, null, 'debug')
	// find the nearest PWS to the hub's geo location
	String geoLocation = location.zipCode
	// use coordinates, if available
	if (location.latitude && location.longitude) geoLocation = "${location.latitude},${location.longitude}"
	LOG("Geolocation: ${geoLocation}", 3, null, 'debug')
	Map wdata = getWeatherFeature('geolookup', geoLocation)
	if (wdata && wdata.response && !wdata.response.containsKey('error')) {	// if we get good data
		if (wdata.response.features.containsKey('geolookup') && (wdata.response.features.geolookup.toInteger() == 1) && wdata.location) {
			//log.debug "wdata ${wdata.location.nearby_weather_stations.pws}"
			LOG("wdata ${wdata.location.nearby_weather_stations}", 3, null, 'debug')
			if (wdata.location.nearby_weather_stations?.pws?.station[0]?.id) PWSID = 'pws:' + wdata.location.nearby_weather_stations.pws.station[0].id
			else if (wdata.location.nearby_weather_stations?.airport?.station[0]?.icao) PWSID = wdata.location.nearby_weather_stations.airport.station[0].icao
		}
		else LOG("bad response", 3, null, 'debug')
	}
	else LOG("null or error", 3, null, 'debug')

	LOG("Nearest PWS ${PWSID}", 3, null, 'info')
	return PWSID
}

def getDeviceId(networkId) {
	return networkId.split(/\./).last() as String
}

def programUpdateHandler(evt) {
	// Clear our reservation once we know that the Ecobee Cloud has updated our thermostat's setpoints (climates)
	cancelReservation(evt.device.currentValue('identifier') as String, 'programChange')
	unsubscribe(evt.device)
	if (!settings?.tempDisable) {
		subscribe(evt.device, 'temperature', insideChangeHandler)
		subscribe(evt.device, 'thermostatMode', thermostatModeHandler)
	}
	LOG("programUpdateHandler(): Setpoint change operation completed",4,null,'debug')
}
def programWaitHandler(evt) {
	//LOG("climatesWaitHandler(): $evt.name = $evt.value",4,null,'debug')
	unsubscribe(evt.device)
	if (!settings?.tempDisable) {
		subscribe(evt.device, 'temperature', insideChangeHandler)
		subscribe(evt.device, 'thermostatMode', thermostatModeHandler)
	}
	String tid = evt.device.currentValue('identifier') as String
	def count = countReservations(tid, 'programChange')
	if ((count > 0) && !haveReservation( tid, 'programChange' )) {
		def waitCounter = atomicState.programWaitCounter ?: [:]
		waitCounter[tid] = 0
		atomicState.programWaitCounter = waitCounter
		runIn(5, checkReservations, [overwrite: false, data: [tid:tid, type:'programChange']])
		LOG("programWaitHandler(): There are still ${count} reservations for 'programChange', waiting...", 3, null, 'debug')
	} else {
		makeReservation(tid, 'programChange')
		LOG("programWaitHandler(): 'programChange' reservation secured, sending pended updates", 3, null, 'debug')
		doPendedUpdates(tid)
	}
}
void checkReservations(data) {
	//log.debug "checkReservations(${data}) @ ${now()}"
	String tid = data.tid.toString()
	def count = countReservations(tid, data.type)
	def waitCounter = atomicState.programWaitCounter ?: [:]
	def counter = ((waitCounter != null) && (waitCounter[tid] != null)) ? waitCounter[tid] : 0
	if ((count > 0) && !haveReservation(tid, data.type) && (counter <= 60)) {
		// Need to wait longer
		runIn(5, checkReservations, [overwrite: false, data: [tid: tid, type: data.type]])
		counter++
		waitCounter[tid] = counter
		atomicState.programWaitCounter = waitCounter
		if ((counter % 12) == 0) runIn(2, doRefresh, [overwrite: false, data: [tid: tid]]) // force a refresh every minute
		LOG("checkReservations(): There are still ${count} reservations for 'programChange', waiting...", 3, null, 'debug')
	} else {
		makeReservation(tid, data.type)
		waitCounter[tid] = 0
		atomicState.programWaitCounter = waitCounter
		LOG("checkReservation(): 'programChange' reservation secured for ${tid}, sending pended updates", 3, null, 'debug')
		doPendedUpdates(tid)
	}
}
void doPendedUpdates(tid) {
	//LOG("doPendedUpdates() @ ${now()}: ${atomicState.pendedUpdates}",4,null,'debug')
	def updates = atomicState.pendedUpdates
	if (updates && updates[tid]) {
		// Find the theremostat
		settings.thermostats.each { stat ->
			statTid = stat.currentValue('identifier').toString()
			if (statTid == tid) {
				if (needSetpointChange( stat, updates[tid].program, updates[tid].heat, updates[tid].cool )) {
					makeSetpointChange( stat, updates[tid].program, updates[tid].heat, updates[tid].cool )
					updates = atomicState.pendedUpdates // in case we have multiple in parallell
				} else {
					// Nothing to do - release the reservation now
					cancelReservation(tid, 'programChange')
				}
				updates[tid] = [:]
				atomicState.pendedUpdates = updates
				return
			}
		}
	}				
}
def doRefresh( data ) {
	settings.thermostats.each { stat ->
		if (stat.currentValue('identifier').toString() == tid) {
			stat.doRefresh(true)
			return
		}
	}
}
def clearReservation( data ) {
	cancelReservation(data.tid, 'programChange')
}
def makeSetpointChange( stat, program, heat, cool ) {
	subscribe( stat, 'climatesUpdated', programUpdateHandler )
	stat.setProgramSetpoints( program, heat, cool )
	String tid = stat.currentValue('identifier').toString()
	runIn(150, clearReservation, [overwrite: false, data: [ tid: tid ]])
	// programUpdateHandler will release the reservation for us
}
boolean needSetpointChange(stat, program, heat, cool) {				
	// def tid = stat.currentValue('identifier') as String
	// need to figure out how to tell if we actually need to make changes
	return true
}
// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(String tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id as String, type )
}
// Cancel my reservation
void cancelReservation(String tid, String type='modeOff') {
	//log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id as String, type )
}
// Do I have a reservation?
boolean haveReservation(String tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id as String, type )
}
// Do any Apps have reservations?
boolean anyReservations(String tid, String type='modeOff') {
	return parent.anyReservations( tid, type )
}
// How many apps have reservations?
Integer countReservations(String tid, String type='modeOff') {
	return parent.countReservations( tid, type )
}
// Get the list of app IDs that have reservations
List getReservations(String tid, String type='modeOff') {
	return parent.getReservations( tid, type )
}
// Get the list of app Names that have reservations
List getGuestList(String tid, String type='modeOff') {
	return parent.getGuestList( tid, type )
}

// Helper Functions
void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
    switch (logType) {
    	case 'error':
        	log.error message
            break;
        case 'warn':
        	log.warn message
            break;
        case 'trace':
        	log.trace message
            break;
        case 'info':
        	if (!settings?.infoOff) log.info message
            break;
        case 'debug':
        default:
        	if (!settings?.debugOff) log.debug message
        	break;
    }
}

String textListToString(list) {
	def c = list?.size()
	String s = list.toString()[1..-2]
	if (c == 1) return s.trim()						// statName
	if (c == 2) return s.replace(', ',' and ').trim()	// statName1 and statName2
	int i = s.lastIndexOf(', ')+2
	return (s.take(i) + 'and ' + s.drop(i)).trim()		// statName1, statName2, (...) and statNameN
}
String getMsgPrefix() {
	String thePrefix = ""
	if (settings?.customPrefix == null) { app.updateSetting('customPrefix', '(helper) at (location):'); settings.customPrefix = '(helper) at (location):'; }
	switch (settings?.customPrefix) {
		case '(helper):':
			thePrefix = atomicState.appDisplayName + ': '
			break
		case '(helper) at (location):':
			thePrefix = atomicState.appDisplayName + " at ${location.name}: "
			break
		case '(location):':
			thePrefix = location.name + ': '
			break
		case 'custom':
			thePrefix = settings?.customPrefixText?.trim() + ' '
			break
		case 'none':
			break
	}
	return thePrefix
}

String getMsgTstat(statList = []) {						
	String theTstat = ""
	if (settings?.customTstat == null) { app.updateSetting('customTstat', '(thermostat names)'); settings?.customTstat = '(thermostat names)'; }
	switch (settings.customTstat) {
		case 'custom':
			theTstat = settings.customTstatText 
			break
		case "(thermostat names)":
			def stats = statList ?: settings?.thermostats
			def nameList = []
			String prefix = ""
			String suffix = ""
			if (settings?.tstatSuffix || settings?.tstatPrefix) {
				def tc = stats.size()
				if (tc == 1) {
					def name = stats[0].displayName
					if (settings.tstatPrefix) name = settings.tstatPrefix + ' ' + name
					if (settings.tstatSuffix) name = name + ' ' + settings.tstatSuffix
					nameList << name
				} else {
					nameList = stats*.displayName
					if (settings.tstatPrefix) prefix = settings.tstatPrefix == 'the' ? 'the ' : settings.tstatPrefix + 's '
					if (settings.tstatSuffix) suffix = settings.tstatSuffix == 'HVAC' ? ' HVAC' : ' ' + settings.tstatSuffix + 's'
				}
			} else {
				nameList = stats*.displayName
			}
			String statStr = textListToString(nameList)
			if (tstatCleaners != []) {
				tstatCleaners.each{
					if ((!settings?.tstatSuffix || (settings.tstatSuffix != it)) && (!settings?.tstatPrefix || (settings.tstatPrefix != it))) { // Don't strip the prefix/suffix we added above
						statStr = statStr.replace(it, '').replace(it.toLowerCase(), '') // Strip out any unnecessary words
					}
				}
			}
			statStr = statStr.replace(':','').replace('	 ', ' ').trim()		// Finally, get rid of any double spaces
			theTstat = prefix + statStr + suffix	// (statStr + ((stats?.size() > 1) ? ' are' : ' is'))	
			break
		case 'the HVAC system':
			theTstat = 'the H V A C system'
			break
		case 'the thermostat':
			def stats = statList ?: settings?.thermostats
			def tc = stats.size()
			theTstat = 'the thermostat' + ((tc > 1) ? 's' : '')
			break
	}
	return theTstat
}

void sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "info")
    if (settings.notify) {
    	String msgPrefix = getMsgPrefix()
        String msg = msgPrefix + notificationMessage
        boolean addFrom = (msgPrefix && !msgPrefix.startsWith("From "))
        //String msg = "${atomicState.appDisplayName} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (ST) {
			if (settings.notifiers) {
            	if (settings.echoAnnouncements) {
                    List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                    List notEcho = echo ? settings.notifiers - echo : settings.notifiers

                    // If we have multiple Echo Speak device targets, get them all to speak at once
                    List echoDeviceObjs = []
                    if (echo) {
                        if(echo?.size() > 1) {
                            echo?.each { 
                                String deviceType = it.currentValue('deviceType') as String
                                String serialNumber = it.deviceNetworkId.toString().split(/\|/).last() as String
                                echoDeviceObjs?.push([deviceTypeId: deviceType, deviceSerialNumber: serialNumber]) 
                            }
                        }
                        //Announcement Command Logic
                        if((echo.size() > 1) && echoDeviceObjs && echoDeviceObjs?.size()) {
                            //NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
                            def devJson = new groovy.json.JsonOutput().toJson(echoDeviceObjs)
                            echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:atomicState.appDisplayName), echoDeviceObjs)	// , changeVol, restoreVol) }
                        } else if (echo.size() == 1) {
                            echo.playAnnouncement(msg, (msgPrefix?:atomicState.appDisplayName))
                        } else {
                        	notEcho*.deviceNotification(msg)
                        }
                    } else {
                        settings.notifiers*.deviceNotification(msg)
                    }
                } else {
                	settings.notifiers*.deviceNotification(msg)
                }
            }
			if (settings.phone) { // check that the user did select a phone number
				if ( settings.phone.indexOf(";") > 0){
					def phones = settings.phone.split(";")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i].trim(), msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone.trim(), msg)						// Only to SMS contact
				}
			} 
			if (settings.pushNotify) {
				LOG("Sending Push to everyone", 3, null, 'warn')
				sendPushMessage(msg)								// Push to everyone
			}
			if (settings.speak) {
				if (settings.speechDevices != null) {
					settings.speechDevices.each {
						it.speak( (addFrom?"From ":"") + msg )
					}
				}
				if (settings.musicDevices != null) {
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText( (addFrom?"From ":"") + msg )
					}
				}
			}
		} else {		// HE
			if (settings.notifiers) {
            	if (settings.echoAnnouncements) {
                    List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                    List notEcho = echo ? settings.notifiers - echo : settings.notifiers

                    // If we have multiple Echo Speak device targets, get them all to speak at once
                    List echoDeviceObjs = []
                    if (echo) {
                        if(echo?.size() > 1) {
                            echo?.each { 
                                String deviceType = it.currentValue('deviceType') as String
                                String serialNumber = it.deviceNetworkId.toString().split(/\|/).last() as String
                                echoDeviceObjs?.push([deviceTypeId: deviceType, deviceSerialNumber: serialNumber]) 
                            }
                        }
                        //Announcement Command Logic
                        if((echo.size() > 1) && echoDeviceObjs && echoDeviceObjs?.size()) {
                            //NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
                            def devJson = new groovy.json.JsonOutput().toJson(echoDeviceObjs)
                            echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:atomicState.appDisplayName), echoDeviceObjs)	// , changeVol, restoreVol) }
                        } else if (echo.size() == 1) {
                            echo.playAnnouncement(msg, (msgPrefix?:atomicState.appDisplayName))
                        } else {
                        	notEcho*.deviceNotification(msg)
                        }
                    } else {
                        settings.notifiers*.deviceNotification(msg)
                    }
                } else {
                	settings.notifiers*.deviceNotification(msg)
                }                
            }
			if (settings.speak) {
				if (settings.speechDevices != null) {
					settings.speechDevices.each {
						it.speak((addFrom?"From ":"") + msg )
					}
				}
				if (settings.musicDevices != null) {
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText((addFrom?"From ":"") + msg )
					}
				}
			}
			
		}
    }
	// Always send to Hello Home / Location Event log
	if (ST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
}

void updateMyLabel() {
	//boolean ST = atomicState.isST
	
	def opts = [' (paused)', '(Cool', ' (Heat', ' (Aux', ' (Off', ' (Auto', ' (Emer']
	String flag
	if (ST) {
		opts.each {
			if (!flag && app.label.contains(it)) flag = it
		}
	} else {
		flag = '<span '
	}
	
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	if (settings.tempDisable) {
		String	newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		String newLabel = myLabel
		if (settings.thermostats?.size()) {
			String modeProgStr = ' (' + thermostats[0].currentValue('thermostatMode').capitalize() + ' - ' + thermostats[0].currentValue('currentProgramName') + ')'
			newLabel = newLabel + (!ST ? '<span style="color:green">' + modeProgStr + '</span>' : modeProgStr)
		}
		// log.debug "newLabel: ${newLabel}"
		if (app.label != newLabel) app.updateLabel(newLabel)
	}
}
def pauseOn() {
	// Pause this Helper
	atomicState.wasAlreadyPaused = (settings.tempDisable && !atomicState.globalPause)
	if (!settings.tempDisable) {
		LOG("performing Global Pause",2,null,'info')
		app.updateSetting("tempDisable", true)
		atomicState.globalPause = true
		runIn(2, updated, [overwrite: true])
	} else {
		LOG("was already paused, ignoring Global Pause",3,null,'info')
	}
}
def pauseOff() {
	// Un-pause this Helper
	if (settings.tempDisable) {
		def wasAlreadyPaused = atomicState.wasAlreadyPaused
		if (!wasAlreadyPaused) { // && settings.tempDisable) {
			LOG("performing Global Unpause",2,null,'info')
			app.updateSetting("tempDisable", false)
			runIn(2, updated, [overwrite: true])
		} else {
			LOG("was paused before Global Pause, ignoring Global Unpause",3,null,'info')
		}
	} else {
		LOG("was already unpaused, skipping Global Unpause",3,null,'info')
		atomicState.wasAlreadyPaused = false
	}
	atomicState.globalPause = false
}

String getTheBee	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=78 height=78 align=right></img>'}
String getTheBeeLogo()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg width=30 height=30 align=left></img>'}
String getTheSectionBeeLogo()		{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=25 height=25 align=left></img>'}
String getTheBeeUrl ()				{ return "https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg" }
String getTheBlank	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/blank.png width=400 height=35 align=right hspace=0 style="box-shadow: 3px 0px 3px 0px #ffffff;padding:0px;margin:0px"></img>'}
String pageTitle	(String txt)	{ return HE ? getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') : txt }
String pageTitleOld (String txt)	{ return HE ? getFormat('header-ecobee','<h2>'+txt+'</h2>')	: txt }
String sectionTitle (String txt)	{ return HE ? getTheSectionBeeLogo() + getFormat('header-nobee','<h3><b>&nbsp;&nbsp;'+txt+'</b></h3>')	: txt }
String smallerTitle (String txt)	{ return txt ? (HE ? '<h3><b>'+txt+'</b></h3>'				: txt) : '' }
String sampleTitle	(String txt)	{ return HE ? '<b><i>'+txt+'<i></b>'						: txt }
String inputTitle	(String txt)	{ return HE ? '<b>'+txt+'</b>'								: txt }
String getWarningText()				{ return HE ? "<span style='color:red'><b>WARNING: </b></span>"	: "WARNING: " }
String getFormat(type, myText=""){
	switch(type) {
		case "header-ecobee":
			return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theBee}${myText}</div>"
			break;
		case "header-nobee":
			return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
			break;
		case "line":
			return HE ? "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>" : "-----------------------------------------------"
			break;
		case "title":
			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
			break;
		case "warning":
			return HE ? "<span style='color:red'><b>WARNING: </b><i></span>${myText}</i>" : "WARNING: ${myText}"
			break;
		case "note":
			return HE ? "<b>NOTE: </b>${myText}" : "NOTE:<br>${myText}"
			break;
		default:
			return myText
			break;
	}
}
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019-2020, Barry A. Burke (storageanarchy@gmail.com)
String getPlatform() { return ((hubitat?.device?.HubAction == null) ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST() {
	if (ST == null) {
    	// ST = physicalgraph?.device?.HubAction ? true : false // this no longer compiles on Hubitat for some reason
        if (HE == null) HE = getIsHE()
        ST = !HE
    }
    return ST    
}
boolean getIsHE() {
	if (HE == null) {
    	HE = hubitat?.device?.HubAction ? true : false
        if (ST == null) ST = !HE
    }
    return HE
}

String getHubPlatform() {
    hubPlatform = getIsST() ? "SmartThings" : "Hubitat"
	return hubPlatform
}
boolean getIsSTHub() { return isST }					// if (isSTHub) ...
boolean getIsHEHub() { return isHE }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return ST ? parent?.settings?."${settingName}" : parent?."${settingName}"
}
@Field String  hubPlatform 	= getHubPlatform()
@Field boolean ST 			= getIsST()
@Field boolean HE 			= getIsHE()
