/**
 *	Ecobee Suite Smart Mode
 *
 *	Copyright 2018-2021 Justin Leonard, Barry A. Burke
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
 *	1.8.10 - Fixed mixed Notification devices in sendMessage
 *	1.8.11 - Added Ambient Weather Station for both ST & HE
 *	1.8.12 - Reset states on location mode change (recalculate everything)
 *	1.8.13 - Refactored sendMessage / sendNotifications
 *	1.8.14 - Allow individual un-pause from peers, even if was already paused
 *	1.8.15 - HOTFIX: allow Ambient Weather Station on Hubitat
 *	1.8.16 - Updated formatting; added Do Not Disturb Modes & Time window
 *	1.8.17 - HOTFIX: location.mode subscription; sendHoldHours in setThermostatProgram()
 *	1.8.18 - HOTFIX: updated sendNotifications() for latest Echo Speaks Device version 3.6.2.0
 *	1.8.19 - Miscellaneous updates & fixes
 *	1.8.20 - Fix label display for " (Cool"
 *	1.8.21 - Fix for multi-word Climate names
 *	1.8.22 - Fix getThermostatPrograms()
 *	1.8.23 - Fix getThermostatModes()
 *	1.8.24 - Fix sendMessage() for new Samsung SmartThings app
 * 	1.8.25 - Fixed setpoint adjustments for "middle" tange
 *	1.8.26 - Fix whatHoldType for 'holdHours'
 *	1.8.27 - Fix for Hubitat 'supportedThermostatModes', etc.
 *	1.9.00 - Removed all ST code
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.9.00" }
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
	boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
	String defaultName = "Smart Mode, Programs & Setpoints"
	def statModes
		
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
		if (maximize) {
			section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
				paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
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

			if (settings?.tempDisable) { 
				paragraph getFormat("warning","This Helper is temporarily paused...")
			} else {
				input ("thermostats", 'device.EcobeeSuiteThermostat', title: inputTitle("Select Ecobee Suite Thermostat(s)"), required: true, 
					   multiple: true, submitOnChange: true)
			}
		}
		if (!settings?.tempDisable && (settings?.thermostats?.size()>0)) {
			statModes = getThermostatModes()
			//log.debug "${statModes}"
			section(title: sectionTitle("Configuration") + smallerTitle("Outdoor Weather Source")) {
				if (maximize) paragraph("Smart Mode requires access to external weather information (temperature and relative humidity). If you don't have a physical weather station of your own, "+
						  "there are user-contributed weather drivers for many weather providers. If your provider isn't listed, choose 'Hubitat Sensors' and select your "+
						  "external weather sensors manually")
				input(name: 'tempSource', title: inputTitle('Select a Weather Source'), type: 'enum', required: true, multiple: false, width: 6,	
				  	options: 
					  	[
						  'ecobee':"Ecobee Thermostat's Weather",  
						  'sensors':'Hubitat Sensors',
						  'station':'Hubitat-based Weather Station Device',
						], submitOnChange: true
				)
				if (settings.tempSource) {
					if (settings.tempSource == 'location') {
						if (maximize) paragraph "Using The Weather Company weather for the current location (${location.name})."
						if (!settings.latLon) input(name: "zipCode", type: 'text', title: inputTitle('Zipcode (Default is location Zip code)'), defaultValue: getZIPcode(), required: true,
													submitOnChange: true, width: 6)
						if (location.latitude && location.longitude) input(name: "latLon", type: 'bool', submitOnChange: true, width: 6, 
																		   title: inputTitle("Use Hubitat hub's GPS coordinates?")+" (better precision)")
						input(name: 'locFreq', type: 'enum', title: inputTitle('Temperature check frequency (minutes)'), required: true, multiple: false, 
							options:['1','5','10','15','30','60','180'], width: 6)
					} else if (settings.tempSource == 'sensors') {
						if (maximize) paragraph "Using Hubitat sensors. Note: Both Temperature & Humidity sensors are required for dew point-based actions."
						input(name: 'thermometer', type: 'capability.temperatureMeasurement', title: inputTitle("Which Temperature Sensor?"), required: true, multiple: false, width: 4)
						input(name: 'humidistat', type: 'capability.relativeHumidityMeasurement', title: inputTitle("Which Relative Humidity Sensor?"),	 
							required: (settings.dewBelowOverride), multiple: false, width: 4)
						paragraph("", width: 4)
					} else if (settings.tempSource == "ecobee") {
						if (maximize) paragraph "Using weather data from the (notoriously inaccurate) Ecobee thermostat${(settings.thermostats.size()==1)?' '+settings.thermostats.displayName:':'}"
						if (settings.thermostats.size() > 1) {
							input(name: 'tstatTemp', type: 'enum', title: inputTitle("Which Ecobee Thermostat?"), required: true, multiple: false, submitOnChange: true,
									options:thermostats.displayName, width: 6)
						}
					} else if (settings.tempSource == 'station') {
						paragraph((maximize ? "Using a Hubitat-based Weather Station - p":"P") + "lease select <b>ONE</b> from the list of the supported Weather Station devices below...")
                        
                        // Ambient Weather Station (https://github.com/KurtSanders/STAmbientWeather)
                        // NOTES: available for both HE & ST, provides temperature & dewpoint
                        input(name: "ambientWeatherStation", type: 'device.AmbientWeatherStation', title: inputTitle("Which Ambient Weather Station?"), required: false, 
                              multiple: false, hideWhenEmpty: true, width: 4)    

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
						input(name: "meteoWeather", type: 'device.MeteobridgeWeatherStation', title: inputTitle('Which Meteobridge Weather Station?'), required: false, 
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
			section(title: sectionTitle("Actions") + smallerTitle("Outdoor Temperature 'Above'")) {
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
					paragraph("", width: 6)
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
				paragraph("", width: 6)
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
					paragraph("", width: 6)
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
					paragraph("", width: 6)
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
			List echo = []
			section(sectionTitle("Notifications")) {
				input(name: "notify", type: "bool", title: inputTitle("Notify on Actions?"), required: true, defaultValue: false, submitOnChange: true, width: 3)
			}
			if (settings.notify) {
				section(smallerTitle("Notification Devices")) {
					input(name: "notifiers", type: "capability.notification", multiple: true, title: inputTitle("Select Notification devices"), submitOnChange: true,
						  required: (!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null))))
					if (settings?.notifiers) {
						echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
						if (echo) {
							input(name: "echoAnnouncements", type: "bool", title: inputTitle("Use ${echo.size()>1?'simultaneous ':''}Announcements for the selected Echo Speaks device${echo.size()>1?'s':''}?"), 
								  defaultValue: false, submitOnChange: true)
						}
					}
				}
			}
			if (settings.notify) {
				section(hideWhenEmpty: (!"speechDevices" && !"musicDevices"), title: smallerTitle("Speech Devices")) {
					input(name: "speak", type: "bool", title: inputTitle("Speak messages?"), required: !settings?.notifiers, defaultValue: false, submitOnChange: true, width: 6)
					if (settings.speak) {
						input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: inputTitle("Select Speech devices"), 
							  multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
						input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: inputTitle("Select Music devices"), 
							  multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
						input(name: "volume", type: "number", range: "0..100", title: inputTitle("At this volume (%)"), defaultValue: 50, required: false, width: 4)
					}
				}
			}
			if (settings.notify && (echo || settings.speak)) {
				section(smallerTitle("Do Not Disturb")) {
					/* if (settings.parentDND == null) { app.updateSetting('parentDND', false); settings.parentDND = false; }
					input(name: "parentDND", type: "bool", title: inputTitle("Load ES Manager's Do Not Disturb settings?"), submitOnChange: true, width: 6, defaultValue: false)
					if (settings.parentDND) {
						List parentModes = getParentSetting("speakModes")
						app.updateSetting("speakModes", parentModes); settings.speakModes = parentModes;
						def parentTimeStart = getParentSetting("speakTimeStart")
						app.updateSetting("speakTimeStart", parentTimeStart); settings.speakTimeStart = parentTimeStart;
						def parentTimeEnd = getParentSetting("speakTimeEnd")
						app.updateSetting("speakTimeEnd", parentTimeEnd); settings.speakTimeEnd = parentTimeEnd;
						paragraph(width: 6, "ES Manager's Modes: ${parentModes}, Start: ${parentTimeStart}, End: ${parentTimeEnd}")
						app.updateSetting('parentDND', false); settings.parentDND = false;
					} else if (HE) paragraph("", width: 6)
					*/
					input(name: "speakModes", type: "mode", title: inputTitle('Only speak notifications during these Location Modes:'), required: false, multiple: true, submitOnChange: true, width: 6)
					input(name: "speakTimeStart", type: "time", title: inputTitle('Only speak notifications<br>between...'), required: (settings.speakTimeEnd != null), submitOnChange: true, width: 3)
					input(name: "speakTimeEnd", type: "time", title: inputTitle("<br>...and"), required: (settings.speakTimeStart != null), submitOnChange: true, width: 3)
					String nowOK = (settings.speakModes || ((settings.speakTimeStart != null) && (settings.speakTimeEnd != null))) ? 
									(" - with the current settings, notifications WOULD ${notifyNowOK()?'':'NOT '}be spoken now") : ''
					if (maximize) paragraph(getFormat('note', "If both Modes and Times are set, both must be true" + nowOK))
				}
			}
			if (maximize) {
				section(){
					paragraph "A <i>'HelloHome'</i> notification is always sent to the Location Event log whenever an action is taken"		
				}
			}
			if ((settings?.notify) && (settings?.pushNotify || settings?.phone || settings?.notifiers || (settings?.speak && (settings?.speechDevices || settings?.musicDevices)))) {
				section(smallerTitle("Customization")) {
					href(name: "customNotifications", title: inputTitle("Customize Notifications"), page: "customNotifications", 
								 description: "Customize the notification messages", state: isCustomized())
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
		section() {
			paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br><small>Copyright \u00a9 2017-2020 Barry A. Burke - All rights reserved.</small><br>"+
					  "<small><i>Your</i>&nbsp;</small><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=MJQD5NGVHYENY&currency_code=USD&source=url' target='_blank'>" + 
					  "<img src='https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/paypal-green.png' border='0' width='64' alt='PayPal Logo' title='Please consider donating via PayPal!'></a>" +
					  "<small><i>donation is appreciated!</i></small></div>" )
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
				samples = (getMsgPrefix() + "${tt} inside temperature is ${theLowTemp}°, so I changed it to 'Heat' mode").replaceAll(':','').replaceAll('  ',' ').replaceAll('  ',' ').trim().capitalize()
			}
			samples = samples + "\n" + ("The outside temperature is ${theHighTemp}°${unit}, so I changed ${getMsgTstat()} to 'Cool' mode").replaceAll(':','').replaceAll('  ',' ').replaceAll('  ',' ').trim().capitalize()
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
			def latest = settings.thermometer.currentState("temperature", true)
			def unit = latest.unit
			def t 
			if (latest.numberValue != null) {
				t = roundIt(latest.numberValue, (unit=='C'?2:1))
				if (dewBelowOverride) {
					latest = settings.humidistat.currentState('humidity', true)
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
			if (settings.ambientWeatherDevice) {
				// HE only, has temperature & dewpoint
				def latest
				if (settings.dewBelowOverride) {
					subscribe(settings.ambientWeatherDevice, 'dewpoint', dewpointChangeHandler)
					latest = settings.ambientWeatherDevice.currentState('dewpoint', true)
					if (latest?.numberValue != null) {
						def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
					}
				}
				subscribe(settings.ambientWeatherDevice, 'temperature', tempChangeHandler)
				latest = settings.ambientWeatherDevice.currentState('temperature', true)
				if (latest?.numberValue != null) { 
					tempNow = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
					temperatureUpdate(tempNow) 
				}
			} else if (settings.ambientWeatherStation) {
				// HE & ST, has temperature & dewpoint
				def latest
				if (settings.dewBelowOverride) {
					subscribe(settings.ambientWeatherStation, 'dewpoint', dewpointChangeHandler)
					latest = settings.ambientWeatherStation.currentState('dewpoint', true)
					if (latest?.numberValue != null) {
						def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
					}
				}
				subscribe(settings.ambientWeatherStation, 'temperature', tempChangeHandler)
				latest = settings.ambientWeatherStation.currentState('temperature', true)
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
				def latest = settings.netatmoOutdoorModule.currentState('temperature', true)
				def t = latest.numberValue
				def unit = latest.unit
				if (t != null) {
					t = latest.numberValue
					if (settings.dewBelowOverride) {
						subscribe(settings.netatmoOutdoorModule, 'relativeHumidity', humidityChangeHandler)
						latest = settings.netatmoOutdoorModule.currentState('relativeHumidity', true)
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
					latest = settings.meteoWeather.currentState('dewpoint', true)
					if (latest?.numberValue != null) {
						def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
						atomicState.dewpoint = d
						LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
					}
				}
				subscribe(settings.meteoWeather, 'temperature', tempChangeHandler)
				latest = settings.meteoWeather.currentState('temperature', true)
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
				latest = theStat.currentState('weatherDewpoint', true)
				if (latest?.numberValue != null) {
					def d = roundIt(latest.numberValue, (latest.unit=='C'?2:1))
					atomicState.dewpoint = d
					LOG("Outside Dewpoint is: ${d}°${gu}",3,null,'info')
				}
			}
			subscribe(theStat, 'weatherTemperature', tempChangeHandler)
			latest = theStat.currentState('weatherTemperature', true)
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
	//atomicState.locModeEnabled = settings.theModes ? settings.theModes.contains(location.mode) : true
	if (settings.theModes) {
		subscribe(location, "mode", locationModeChangeHandler)
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
	updateMyLabel()
	if (!settings.theModes || settings.theModes.contains(evt.value)) {
    	LOG("Location mode changed to ${evt.value} (enabled), updating...",3,null,'trace')
        atomicState.locModeEnabled = true
        atomicState.aboveChanged = false
		atomicState.betweenChanged = false
		atomicState.belowChanged = false
		if (atomicState.temperature) runIn(2, atomicTempUpdater, [overwrite: true])
	} else {
		atomicState.locModeEnabled = false
        LOG("Location mode changed to ${evt.value} (disabled), ignoring...",3,null,'trace')
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
	String tid = getDeviceId(evt.device.deviceNetworkId)
	
	if (theTemp != null) {
		def coolSP = evt.device.currentValue('coolingSetpoint', true) as BigDecimal
		if (coolSP != null) {
			coolSP += diffAdjust
			def coolDiff = evt.device.currentValue('coolDifferential', true) as BigDecimal
			if (theTemp >= (coolSP + coolDiff)) {
				String cMode = evt.device.currentValue('thermostatMode', true)
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
			def heatSP = evt.device.currentValue('heatingSetpoint', true) as BigDecimal
			if (heatSP != null) {
				def heatDiff = evt.device.currentValue('heatDifferential', true) as BigDecimal
				if (theTemp <= (heatSP - heatDiff)) {
					heatSP += diffAdjust
					String cMode = evt.device.currentValue('thermostatMode', true)
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
				String cMode = evt.device.currentValue('thermostatMode', true)
				//log.debug "newMode: ${newMode}, cMode: ${cMode}"
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
	if (evt.value != 'off') {
		cancelReservation( getDeviceId(evt.device.deviceNetworkId), 'modeOff' ) // we're not off anymore, give up the reservation
		runIn(5, atomicTempUpdater, [overwrite: true])		// someone else turned the HVAC back on, let's make make sure it is set to the correct mode	
		LOG("Thermostat ${evt.device.displayName}'s Mode was changed to ${evt.value} - revalidating in 5 seconds...",2,null,'info')	
	}
	updateMyLabel()
}
def tempChangeHandler(evt) {
	if (evt.numberValue != null) {
		def t = roundIt(evt.numberValue, (evt.unit=='C'?2:1))
		atomicState.temperature = t
		if (settings.dewBelowOverride || settings.dewAboveTemp) {
			// We have to update the dewpoint every time the temperature (or humidity) changes
			if (atomicState.humidity != null) {
				// Somebody is updating atomicState.humidity, so we need to calculate the dewpoint
				// (Sources that provide dewpoint directly will not update atomicState.humidity)
				if (settings.tempSource == 'sensors') {	   
					def latest = settings.humidistat.currentState('humidity', true)
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
					if (settings.darkSkyNetWeatherDriver) {		// HE only
						latest = settings.darkSkyNetWeatherDriver.currentState('relativeHumidity', true)
					} else if (settings.netatmoOutdoorModule) {			// both ST & HE
						latest = settings.netatmoOutdoorModule.currentState('relativeHumidity', true)
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
	//log.trace "start\nokMode: ${okMode}\naboveChanged: ${atomicState.aboveChanged}\nbetweenChanged: ${atomicState.betweenChanged}\nbelowChanged: ${atomicState.belowChanged = false}"
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
        //log.trace "above\nokMode: ${okMode}\naboveChanged: ${atomicState.aboveChanged}\nbetweenChanged: ${atomicState.betweenChanged}\nbelowChanged: ${atomicState.belowChanged}"
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
            //log.trace "below1\nokMode: ${okMode}\naboveChanged: ${atomicState.aboveChanged}\nbetweenChanged: ${atomicState.betweenChanged}\nbelowChanged: ${atomicState.belowChanged}"
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
                //log.trace "below2\nokMode: ${okMode}\naboveChanged: ${atomicState.aboveChanged}\nbetweenChanged: ${atomicState.betweenChanged}\nbelowChanged: ${atomicState.belowChanged}"
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
            //log.trace "below\nokMode: ${okMode}\naboveChanged: ${atomicState.aboveChanged}\nbetweenChanged: ${atomicState.betweenChanged}\nbelowChanged: ${atomicState.belowChanged}"
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
        //log.trace "between\nokMode: ${okMode}\naboveChanged: ${atomicState.aboveChanged}\nbetweenChanged: ${atomicState.betweenChanged}\nbelowChanged: ${atomicState.belowChanged}"
	}
	//log.debug "desiredMode: ${desiredMode}, desiredProgram: ${desiredProgram}"
	if ((desiredMode != null) || (desiredProgram != null)) {
		String changeNames = ""
		def changeNamesList = []
		String sameNames = ""
		def insideOverride = atomicState.insideOverride
		settings.thermostats.each { 
			String cMode = it.currentValue('thermostatMode', true)
			String cProgram = it.currentValue('currentProgramName', true)
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
								if (desiredMode && (cMode != desiredMode)) {
                                	log.debug "${tid} - currentMode: ${cMode}, desiredMode: ${desiredMode}, changing mode"
                                	it.setThermostatMode(desiredMode)
                                }
								if (desiredProgram && (cProgram != desiredProgram)) {
									String sendHoldType = whatHoldType(stat)
									Integer sendHoldHours = null
									if ((sendHoldType != null) && sendHoldType.isInteger()) {
										sendHoldHours = sendHoldType.toInteger()
										sendHoldType = 'holdHours'
									}
									LOG("desiredProgram: ${desiredProgram}, sendHoldType: ${sendHoldType}, sendHoldHours: ${sendHoldHours}",3,null,'info')
									it.setThermostatProgram(desiredProgram, sendHoldType, sendHoldHours)
                                    atomicState.modeOffNotified = false
								}
								changeNames += changeNames ? ", ${it.displayName}" : it.displayName
								changeNamesList << it
							} else {
								// somebody else still has a 'modeOff' reservation so we can't turn it on
								def reservedBy = getGuestList(tid,'modeOff')
								LOG("Reservations: ${reservedBy}", 3, null, 'debug')
								if (reservedBy == []) reservedBy = ['somebody']
								def msg = "The outside temperature is ${temp}°${unit}, but I can't change ${getMsgTstat([it])} to '${desiredMode}' Mode because ${reservedBy.toString()[1..-2]} hold 'modeOff' reservations"
								LOG(msg,2,null,'warn')
                                if (!atomicState.modeOffNotified) sendMessage(msg)
                                atomicState.modeOffNotified = true
								// here's where we COULD subscribe to the reservations to see when we can turn it back on. For now, let's just let whomever is last deal with it
							}
						} else {
							// Not off currently, so we can change freely
							boolean changed = false
							cancelReservation(tid, 'modeOff')	// just in case
                            if (desiredMode && (cMode != desiredMode)) {
                                log.debug "${tid} - currentMode: ${cMode}, desiredMode: ${desiredMode}, changing mode"
                                it.setThermostatMode(desiredMode)
                                changed = true 
                            }
							if (desiredProgram && (cProgram != desiredProgram)) {
								String sendHoldType = whatHoldType(stat)
								Integer sendHoldHours = null
								if ((sendHoldType != null) && sendHoldType.isInteger()) {
									sendHoldHours = sendHoldType.toInteger()
									sendHoldType = 'holdHours'
								}
								LOG("sendHoldType: ${sendHoldType}, sendHoldHours: ${sendHoldHours}",3,null,'info')
								it.setThermostatProgram(desiredProgram, sendHoldType, sendHoldHours)
                                atomicState.modeOffNotified = false
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
			// def pendedUpdates = atomicState.pendedUpdates as Map
            Map pendedUpdates = [:].withDefault {[]}
            if (atomicState.pendedUpdates) pendedUpdates = atomicState.pendedUpdates as Map
			pendedUpdates[tid] = [program: program, heat: heatTemp, cool: coolTemp]
			atomicState.pendedUpdates = pendedUpdates
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
List getThermostatModes() {
	List statModes = []
	settings.thermostats?.each { stat ->
		def tm = stat.currentValue('supportedThermostatModes', true)
		if (statModes == []) {	
			if (tm && (tm != '[]')) statModes = new JsonSlurper().parseText(tm)    // tm[1..-2].tokenize(", ")
		} else {
			def nm = (tm && (tm != '[]')) ? new JsonSlurper().parseText(tm) /* tm[1..-2].tokenize(", ") */ : []
			if (nm) statModes = statModes.intersect(nm)
		}
	}
	return statModes.sort(false)
}

// get the combined set of Ecobee Programs applicable for these thermostats
List getThermostatPrograms() {
	List programs = []
	if (settings.thermostats?.size() > 0) {
		settings.thermostats.each { stat ->
        	List progs = []
        	String cl = stat.currentValue('climatesList', true)
    		if (cl && (cl != '[]')) {
        		progs = new JsonSlurper().parseText(cl)     // cl[1..-2].split(', ')
        	} else {
    			String pl = stat.currentValue('programsList', true)
        		progs = pl ? new JsonSlurper().parseText(pl) : []
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
            if ( /*settings.holdHours && */ settings.holdHours?.toString().isInteger()) {
            	sendHoldType = settings.holdHours
            } else if ((parentHoldType == 'Specified Hours') && (parentHoldHours?.toString().isInteger())) {
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
			String statHoldType = statDevice.currentValue('statHoldAction', true)
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
		return sendHoldType as String
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
		def setp = stat.currentValue('heatRangeLow', true)
		lo = lo ? ((setp < lo) ? setp : lo) : setp
		setp = stat.currentValue('heatRangeHigh', true)
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
		def setp = stat.currentValue('coolRangeLow', true)
		lo = lo ? ((setp < lo) ? setp : lo) : setp
		setp = stat.currentValue('coolRangeHigh', true)
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
boolean notifyNowOK() {
	// If both provided, both must be true; else only the provided one needs to be true
	boolean modeOK = settings.speakModes ? (settings.speakModes && settings.speakModes.contains(location.mode)) : true
	boolean timeOK = settings.speakTimeStart? myTimeOfDayIsBetween(timeToday(settings.speakTimeStart), timeToday(settings.speakTimeEnd), new Date(), location.timeZone) : true
	return (modeOK && timeOK)
}
private myTimeOfDayIsBetween(String fromTime, String toTime, Date checkDate, String timeZone) {
	return myTimeOfDayIsBetween(timeToday(fromTime), timeToday(toTime), checkDate, timeZone)
}
private myTimeOfDayIsBetween(Date fromDate, Date toDate, Date checkDate, timeZone)     {
	if (toDate == fromDate) {
		return false	// blocks the whole day
	} else if (toDate < fromDate) {
		if (checkDate.before(fromDate)) {
			fromDate = fromDate - 1
		} else {
			toDate = toDate + 1
		}
	}
    return (!checkDate.before(fromDate) && !checkDate.after(toDate))
}
void sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "info")
    if (settings.notify) {
    	String msgPrefix = getMsgPrefix()
        String msg = (msgPrefix + notificationMessage.replaceAll(':','')).replaceAll('  ',' ').replaceAll('  ',' ').trim().capitalize()
        boolean addFrom = (msgPrefix && !msgPrefix.startsWith("From "))

		if (settings.notifiers) {
			sendNotifications(msgPrefix, msg)               
		}
		if (settings.speak && notifyNowOK()) {
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
    // Always send to Hello Home / Location Event log
	sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
}
// Handles sending to Notification devices, with special handling for Echo Speaks devices (if settings.echoAnnouncements is true)
boolean sendNotifications( String msgPrefix, String msg ) {
	if (!settings.notifiers) {
		LOG("sendNotifications(): no notifiers!",2,null,'warn')
		return false
	}
    
    List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
    List notEcho = echo ? settings.notifiers - echo : settings.notifiers        
    List echoDeviceObjs = []
    if (settings.echoAnnouncements) {
        if (echo?.size()) {
        	// Get all the Echo Speaks devices to speak at once
            echo.each { 
                String deviceType = it.currentValue('deviceType') as String
                // deviceSerial is an attribute as of Echo Speaks device version 3.6.2.0
                String deviceSerial = (it.currentValue('deviceSerial') ?: it.deviceNetworkId.toString().split(/\|/).last()) as String
                echoDeviceObjs.push([deviceTypeId: deviceType, deviceSerialNumber: deviceSerial]) 
            }
			if (echoDeviceObjs?.size() && notifyNowOK()) {
				//NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
				def devJson = new groovy.json.JsonOutput().toJson(echoDeviceObjs)
				echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:atomicState.appDisplayName), echoDeviceObjs)	// , changeVol, restoreVol) }
			}
			// The rest get a standard deviceNotification
			if (notEcho.size()) notEcho*.deviceNotification(msg)
		} else {
			// No Echo Speaks devices
			settings.notifiers*.deviceNotification(msg)
		}
	} else {
		// Echo Announcements not enabled - just do deviceNotifications, but only if Do Not Disturb is not on
        if (echo?.size() && notifyNowOK()) echo*.deviceNotification(msg)
		if (notEcho.size()) notEcho*.deviceNotification(msg)
	}
	return true
}
void updateMyLabel() {
	def opts = [' (paused)', ' (Cool', ' (Heat', ' (Aux', ' (Off', ' (Auto', ' (Emer']
	String flag = '<span '
	
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
		String	newLabel = myLabel + '<span style="color:red"> (paused)</span>' 
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		String newLabel = myLabel
		if (settings.thermostats?.size()) {
        	String thermostatMode = thermostats[0].currentValue('thermostatMode', true).capitalize()
			String modeProgStr = ' (' + thermostatMode + ' - ' + thermostats[0].currentValue('currentProgramName', true) + ')'
            String color = (thermostatMode == 'Off') ? 'red' : 'green'
			newLabel = newLabel + '<span style="color:' + color + '">' + modeProgStr + '</span>'
		}
		// log.debug "newLabel: ${newLabel}"
		if (app.label != newLabel) app.updateLabel(newLabel)
	}
}
def pauseOn(global = false) {
	// Pause this Helper
	atomicState.wasAlreadyPaused = settings.tempDisable //!atomicState.globalPause)
	if (!settings.tempDisable) {
		LOG("pauseOn(${global}) - performing ${global?'Global':'Helper'} Pause",2,null,'info')
		app.updateSetting("tempDisable", true)
        settings.tempDisable = true
		atomicState.globalPause = global
		runIn(2, updated, [overwrite: true])
        // updateMyLabel()
	} else {
		LOG("pauseOn(${global}) - was already paused...",3,null,'info')
	}
}
def pauseOff(global = false) {
	// Un-pause this Helper
	if (settings.tempDisable) {
		// Allow peer Apps to individually re-enable anytime
        // NB: they won't be able to unpause us if we are in a global pause (they will also be paused)
        if (!global || !atomicState.wasAlreadyPaused) { 													// 
			LOG("pauseOff(${global}) - performing ${global?'Global':'Helper'} Unpause",2,null,'info')
			app.updateSetting("tempDisable", false)
            settings.tempDisable = false
            atomicState.wasAlreadyPaused = false
			runIn(2, updated, [overwrite: true])
		} else {
			LOG("pauseOff(${global}) - was already paused before Global Pause, ignoring...",3,null,'info')
		}
	} else {
		LOG("pauseOff(${global}) - not currently paused...",3,null,'info')
		atomicState.wasAlreadyPaused = false
	}
	atomicState.globalPause = global
}
String getTheBee	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=78 height=78 align=right></img>'}
String getTheBeeLogo()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg width=30 height=30 align=left></img>'}
String getTheSectionBeeLogo()		{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=25 height=25 align=left></img>'}
String getTheBeeUrl ()				{ return "https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg" }
String getTheBlank	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/blank.png width=400 height=35 align=right hspace=0 style="box-shadow: 3px 0px 3px 0px #ffffff;padding:0px;margin:0px"></img>'}
String pageTitle 	(String txt) 	{ return getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') }
String pageTitleOld	(String txt)	{ return getFormat('header-ecobee','<h2>'+txt+'</h2>') }
String sectionTitle	(String txt) 	{ return getTheSectionBeeLogo() + getFormat('header-nobee','<h3><b>&nbsp;&nbsp;'+txt+'</b></h3>') }
String smallerTitle (String txt)	{ return txt ? ('<h3 style="color:#5BBD76"><b><u>'+txt+'</u></b></h3>') : '' } // <hr style="background-color:#5BBD76;height:1px;width:52%;border:0;align:top">
String sampleTitle	(String txt) 	{ return '<b><i>'+txt+'<i></b>' }
String inputTitle	(String txt) 	{ return '<b>'+txt+'</b>' }
String getWarningText()				{ return "<span style='color:red'><b>WARNING: </b></span>" }
String getFormat(type, myText=""){
	switch(type) {
		case "header-ecobee":
			return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theBee}${myText}</div>"
			break;
		case "header-nobee":
			return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
			break;
    	case "line":
			return "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>"
			break;
		case "title":
			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
			break;
		case "warning":
			return "<span style='color:red'><b>WARNING: </b><i></span>${myText}</i>"
			break;
		case "note":
			return "<b>NOTE: </b>${myText}"
			break;
		default:
			return myText
			break;
	}
}

// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019-2020, Barry A. Burke (storageanarchy@gmail.com)
String getPlatform() { return 'Hubitat' }	// if (platform == 'SmartThings') ...
boolean getIsST() { return false }
boolean getIsHE() { return true }

String getHubPlatform() { 
	return 'Hubitat'
}
boolean getIsSTHub() { return false }					// if (isSTHub) ...
boolean getIsHEHub() { return true }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return parent?."${settingName}"
}
@Field String  hubPlatform 	= 'Hubitat'
@Field boolean ST 			= false
@Field boolean HE 			= true
@Field String  debug		= 'debug'
@Field String  error		= 'error'
@Field String  info			= 'info'
@Field String  trace		= 'trace'
@Field String  warn			= 'warn'
