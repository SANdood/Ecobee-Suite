/**
 *	Based on original code Copyright 2015 SmartThings
 *	Additional changes Copyright 2016 Sean Kendall Schneyer
 *	Additional changes Copyright 2017, 2018, 2019 Barry A. Burke
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
 *	Ecobee Suite Service Manager
 *
 *	Original Author: scott
 *	Date: 2013
 *
 *	Updates by Barry A. Burke (storageanarchy@gmail.com) 2016, 2017, 2018 & 2019
 *
 *	See Github Changelog for complete change history
 *	<snip>
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - changed setProgram() Hold: warning to info, fixed another resp.data==null error
 *	1.7.02 - Forcepoll 3 minutes after install/update
 *	1.7.03 - More thermostatHold optimizations
 *	1.7.04 - String fixes
 *	1.7.05 - nonCached currentValue() for HE
 *	1.7.06 - Fixed deleteSensorFromProgram() error
 *	1.7.07 - More generateEvent() & setEcobeeSetting() optimizations; wifi alert
 *	1.7.08 - Clean up conditional updates
 *	1.7.09 - Fixed climates & setpoints update consistency
 *	1.7.10 - Optimized update processing
 *	1.7.11 - Fix thermostatOperatingStateDisplay
 *	1.7.12 - Fixed SMS text entry
 *	1.7.13 - Don't require Notification/SMS
 *	1.7.14 - Fixing private method issue caused by grails, display proper platform name everywhere, don't send null temperature
 *	1.7.15 - Fixed a debugLevelFour error
 *	1.7.16 - Better logging for Reservations
 *	1.7.17 - Better logging for setXXXX() functions (ID the thermostat)
 *	1.7.18 - Added current/scheduledProgramOwner and ProgramType attributes, fixed some typos
 *	1.7.19 - Run update() if the (HE only?) hub reboots, Fixed reservations, Read Only attributes cleanup
 *	1.7.20 - Optimized isST/isHE, fixed getChildName(), added Global Pause
 *	1.7.21 - Fix Global Pause on ST
 *	1.7.22 - Fixed thermOpStat 'idle' transition, added 'ventilator', 'economizer', 'compHotWater' & 'auxHotWater' equipOpStats
 *	1.7.23 - More code optimizations
 *	1.7.24 - Handle DNS name resolution timeouts, changed displayName for Smart Vents & Switches, added external Global Pause switch
 *	1.7.25 - Changed displayName for Smart Mode,Programs & Setpoints Helper
 *	1.7.26 - Fixed pauseSwitch initialization error
 *	1.7.27 - Enabled "Demand Response" program
 */
String getVersionNum()		{ return "1.7.27" }
String getVersionLabel()	{ return "Ecobee Suite Manager, version ${getVersionNum()} on ${getHubPlatform()}" }
String getMyNamespace()		{ return "sandood" }
import groovy.json.*
import groovy.transform.Field

def getHelperSmartApps() {
	String mrpTitle = isST ? 'New Mode/Routine/Switches/Program Helper...' : 'New Mode/Switches/Program Helper...'
	return [ 
		[name: "ecobeeContactsChild", appName: "ecobee Suite Open Contacts",  
			namespace: myNamespace, multiple: true, 
			title: "New Contacts & Switches Helper..."],
		[name: "ecobeeRoutinesChild", appName: "ecobee Suite Routines",	 
			namespace: myNamespace, multiple: true, 
			title: mrpTitle],
		[name: "ecobeeQuietTimeChild", appName: "ecobee Suite Quiet Time",
			namespace: myNamespace, multiple: true,
			title: "New Quiet Time Helper..."],
		[name: "ecobeeCirculationChild", appName: "ecobee Suite Smart Circulation",
			 namespace: myNamespace, multiple: true,
			 title: "New Smart Circulation Helper..."],
		[name: "ecobeeModeChild", appName: "ecobee Suite Smart Mode",
			namespace: myNamespace, multiple: true,
			title: "New Smart Mode, Programs & Setpoints Helper..."],
		[name: "ecobeeRoomChild", appName: "ecobee Suite Smart Room",
			namespace: myNamespace, multiple: true,
			title: "New Smart Room Helper..."],
		[name: "ecobeeSwitchesChild", appName: "ecobee Suite Smart Switches",
			namespace: myNamespace, multiple: true,
			title: "New Smart Switch/Dimmer/Vent Helper..."],
		[name: "ecobeeVentsChild", appName: "ecobee Suite Smart Vents",
			namespace: myNamespace, multiple: true,
			title: "New Smart Vents & Switches Helper..."],
		[name: "ecobeeZonesChild", appName: "ecobee Suite Smart Zones",
			namespace: myNamespace, multiple: true,
			title: "New Smart Zones Helper..."],
		[name: "ecobeeThermalComfort", appName: "ecobee Suite Thermal Comfort",
			namespace: myNamespace, multiple: true,
			title: "New Thermal Comfort Helper..."],
		[name: "ecobeeWorkHomeChild", appName: "ecobee Suite Working From Home",
			namespace: myNamespace, multiple: true,
			title: "New Working From Home Helper..."]
	]
}
 
definition(
	name:			"Ecobee Suite Manager",
	namespace:		myNamespace,
	author:			"Barry A. Burke (storageanarchy@gmail.com)",
	description:	"Connect your Ecobee thermostats and sensors to ${isST?'SmartThings':'Hubitat'}, along with a Suite of Helper ${isST?'Smart':''}Apps.",
	category:		"My Apps",
	iconUrl:		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url:		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: true,
	oauth:			true,
	pausable:		false
) {
	appSetting "clientId"
}
preferences {
	page(name: "mainPage")
	page(name: "removePage")
	page(name: "authPage")
	page(name: "thermsPage")
	page(name: "sensorsPage")
	page(name: "preferencesPage")
	page(name: "askAlexaPage")
	page(name: "helperSmartAppsPage")	 
	// Parts of debug Dashboard
	page(name: "debugDashboardPage")
	page(name: "pollChildrenPage")
	page(name: "updatedPage")
	page(name: "refreshAuthTokenPage")	  
}

mappings {
	path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
	path("/callback") {action: [GET: "callback"]}
	path("/oauth/callback") {action: [GET: "callback"]}
}

// Begin Preference Pages
def mainPage() {	
	def deviceHandlersInstalled 
	def readyToInstall
	boolean ST = isST
	boolean HE = !ST
	atomicState.appsArePaused = settings.pauseHelpers?:false
	
	// Request the Ask Alexa Message Queue list as early as possible (isn't a problem if Ask Alexa isn't installed)
	if (ST) {
		subscribe(location, "AskAlexaMQ", askAlexaMQHandler)
		sendLocationEvent(name: "AskAlexaMQRefresh", value: "refresh", isStateChange: true)
	}
		
	// Only create the dummy devices if we aren't initialized yet
	if (!atomicState.initialized) {
		deviceHandlersInstalled = testForDeviceHandlers()
		readyToInstall = deviceHandlersInstalled
	} else {
		removeChildDevices( getAllChildDevices(), true )	// remove any lingering temp devices
	}
	if (atomicState.initialized) { readyToInstall = true }
	
	dynamicPage(name: "mainPage", title: (HE?'<b>':'') + "Welcome to ${getVersionLabel()}" + (HE?'</b>':''), install: readyToInstall, uninstall: false, submitOnChange: true) {
		def ecoAuthDesc = (atomicState.authToken != null) ? "[Connected]\n" :"[Not Connected]\n"		
		
		// If not device Handlers we cannot proceed
		if(!atomicState.initialized && !deviceHandlersInstalled) {
			section() {
				paragraph "ERROR!\n\nYou MUST add the ${getChildThermostatName()} and ${getChildSensorName()} Device Handlers to the IDE BEFORE running the setup."				
			}		
		} else {
			readyToInstall = true
		}
		
		if(atomicState.initialized && !atomicState.authToken) {
			section() {
				paragraph "WARNING!\n\nYou are no longer connected to the ecobee API. Please re-Authorize below."				
			}
		}		

		if(atomicState.authToken != null && atomicState.initialized != true) {
			section() {
				paragraph "Please ${HE?'click':'tap'} 'Done' to save your credentials. Then re-open the Ecobee Suite Manager to continue the setup."
			}
		}

		// Need to save the initial login to setup the device without timeouts
		if(atomicState.authToken != null && atomicState.initialized) {
			if (settings.thermostats?.size() > 0 && atomicState.initialized) {
				section((HE?'<b>':'') + "Helper ${ST?'Smart':''}Apps" + (HE?'</b>':'')) {
					href ("helperSmartAppsPage", title: "Helper ${ST?'Smart':''}Apps", description: "${HE?'Click':'Tap'} to manage Helper ${ST?'Smart':''}Apps")
				}			 
			}
			section((HE?'<b>':'') + "Ecobee Devices" + (HE?'</b>':'')) {
				def howManyThermsSel = settings.thermostats?.size() ?: 0
				def howManyTherms = atomicState.numAvailTherms ?: "?"
				def howManySensors = atomicState.numAvailSensors ?: "?"
				
				// Thermostats
				atomicState.settingsCurrentTherms = settings.thermostats ?: []
				href ("thermsPage", title: "Thermostats", description: "${HE?'Click':'Tap'} to select Thermostats [${howManyThermsSel}/${howManyTherms}]")				  
				
				// Sensors
				if (settings.thermostats?.size() > 0) {
					atomicState.settingsCurrentSensors = settings.ecobeesensors ?: []
					def howManySensorsSel = settings.ecobeesensors?.size() ?: 0
					if (howManySensorsSel > howManySensors) { howManySensorsSel = howManySensors } // This is due to the fact that you can remove alread selected hiden items
					href ("sensorsPage", title: "Sensors", description: "${HE?'Click':'Tap'} to select Sensors [${howManySensorsSel}/${howManySensors}]")
				}
			}		 
			section((HE?'<b>':'') + "Preferences" + (HE?'</b>':'')) {
				href ("preferencesPage", title: "Ecobee Suite Preferences", description: "${HE?'Click':'Tap'} to review global settings")
				if (ST) {
					href ("askAlexaPage", title: "Ask Alexa Preferences", description: "Tap to review Ask Alexa settings")
				} // askAlexa not (yet) supported on Hubitat
			}
		   
		} // End if(atomicState.authToken)
		
		// Setup our API Tokens		  
		section((HE?'<b>':'') + "Ecobee Authentication" + (HE?'</b>':'')) {
			href ("authPage", title: "Ecobee API Authorization", description: "${ecoAuthDesc}${HE?'Click':'Tap'} for ecobee Credentials")
		}	   
		if ( debugLevel(5) ) {
			section ("Debug Dashboard") {
				href ("debugDashboardPage", description: "${HE?'Click':'Tap'} to enter the Debug Dashboard", title: "Debug Dashboard")
			}
		}
		section((HE?'<b>':'') + "Remove Ecobee Suite" + (HE?'</b>':'')) {
			href ("removePage", description: "${HE?'Click':'Tap'} to remove ${app.name}", title: "Remove this instance")
		}			 
		
		section ((HE?'<b>':'') + "Name this instance of Ecobee Suite Manager" + (HE?'</b>':'')) {
			label name: "name", title: "Assign a name", required: false, defaultValue: app.name, submitOnChange: true
			if (HE) {
				if (!app.label) {
					app.updateLabel(app.name)
					atomicState.appDisplayName = app.name
				} else if (app.label.contains('<span')) {
					if (atomicState?.appDisplayName != null) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
						def myLabel = app.label.substring(0, app.label.indexOf('<span'))
						atomicState.appDisplayName = myLabel
						app.updateLabel(atomicState.appDisplayName)
					}
				}
			}
		}
	 
		section (getVersionLabel()) {}
	}
}

def removePage() {
	dynamicPage(name: "removePage", title: "Remove Ecobee Suite Manager and All Devices", install: false, uninstall: true) {
		section ("WARNING!\n\nRemoving Ecobee Suite Manager also removes all Devices\n") {
		}
	}
}

// Setup OAuth between SmartThings and Ecobee clouds
def authPage() {
	LOG("authPage() --> Begin", 3, null, 'trace')
	boolean ST = isST
	boolean HE = !ST

	// atomicState.accessToken = createAccessToken()
	log.debug "accessToken: ${atomicState.accessToken}, ${state.accessToken}"
	
	if(!atomicState.accessToken) { //this is an access token for Ecobee to make a callback into Ecobee Suite Manager (this code)
		try {
			atomicState.accessToken = createAccessToken()
		} catch(Exception e) {
			LOG("authPage() --> OAuth Exception: ${e}", 1, null, "error")
			LOG("authPage() --> Probable Cause: OAuth not enabled in ${HE?'Hubitat':'SmartThings'} IDE for the 'Ecobee Suite Manager' ${HE?'App':'SmartApp'}", 1, null, 'warn')
			if (!atomicState.accessToken) {
				LOG("authPage() --> No OAuth Access token", 3, null, 'error')
				return dynamicPage(name: "authPage", title: "OAuth Initialization Failure", nextPage: "", uninstall: true) {
					section() {
						paragraph "Error initializing Ecobee Authentication: could not get the OAuth access token.\n\nPlease verify that OAuth has been enabled in " +
							"the ${HE?'Hubitat':'SmartThings'} IDE for the 'Ecobee Suite Manager' ${HE?'App':'SmartApp'}, and then try again.\n\nIf this error persists, view Live Logging in the IDE for " +
							"additional error information."
					}
				}
			}
		}
	}

	def description = ''
	def uninstallAllowed = false
	def oauthTokenProvided = false

	if (atomicState.authToken) {
		description = "You are connected. ${ST?'Tap Done/Next above':'Click Next/Done below'}."
		uninstallAllowed = true
		oauthTokenProvided = true
		apiRestored()
	} else {
		description = "${HE?'Click':'Tap'} to enter ecobee Credentials"
	}
	// HE OAuth process is slightly different than SmartThings OAuth process
	def redirectUrl = ST ? buildRedirectUrl : oauthInitUrl()

	// get rid of next button until the user is actually auth'd
	if (!oauthTokenProvided) {
		LOG("authPage() --> Valid ${ST?'ST':'HE'} OAuth Access token (${atomicState.accessToken}), need Ecobee OAuth token", 3, null, 'trace')
		LOG("authPage() --> RedirectUrl = ${redirectUrl}", 3, null, 'info')
		return dynamicPage(name: "authPage", title: "ecobee Setup", nextPage: "", uninstall: uninstallAllowed) {
			section() {
				paragraph "${HE?'Click':'Tap'} below to log in to the Ecobee service and authorize Ecobee Suite for ${ST?'SmartThings':'Hubitat'} access. Be sure to ${HE?'Click':'Tap'} the 'Allow' button on the 2nd page."
				href url: redirectUrl, style: "${ST?'embedded':'external'}", required: true, title: "ecobee Account Authorization", description: description
			}
		}
	} else {		
		LOG("authPage() --> Valid OAuth token (${atomicState.authToken})", 3, null, 'trace')
		return dynamicPage(name: "authPage", title: "ecobee Setup", nextPage: "mainPage", uninstall: uninstallAllowed) {
			section() {
				paragraph "Return to main menu."
				href url:redirectUrl, style: "embedded", state: "complete", title: "ecobee Account Authorization", description: description
			}
		}			
	} 
}

// Select which Thermostats are to be used
def thermsPage(params) {
	LOG("=====> thermsPage() entered", 5)		 
	def stats = getEcobeeThermostats()
	boolean ST = isST
	boolean HE = !isST
	
	LOG("thermsPage() -> thermostat list: ${stats}")
	LOG("thermsPage() starting settings: ${settings}")
	LOG("thermsPage() params passed? ${params}", 4, null, "trace")

	dynamicPage(name: "thermsPage", title: (HE?'<b>':'') + "Thermostat Selection" + (HE?'</b>':''), params: params, nextPage: "", content: "thermsPage", uninstall: false) {	
		section("${HE?'Click':'Tap'} below to see the list of Ecobee thermostats available in your ecobee account and select the ones you want to connect to ${ST?'SmartThings':'Hubitat'}.") {
			LOG("thermsPage(): atomicState.settingsCurrentTherms=${atomicState.settingsCurrentTherms}	thermostats=${settings.thermostats}", 4, null, "trace")
			if (atomicState.settingsCurrentTherms != settings.thermostats) {
				LOG("atomicState.settingsCurrentTherms != thermostats determined!!!", 4, null, "trace")		
			} else { LOG("atomicState.settingsCurrentTherms == thermostats: No changes detected!", 4, null, "trace") }
			input(name: "thermostats", title:"Select Thermostats", type: "enum", required:false, multiple:true, description: "Tap to choose", params: params, options: stats, submitOnChange: true)		   
		}
		section("NOTE:\n\nThe temperature units (F or C) is determined by your Location settings automatically. Please update your Hub settings " + 
			"(under My Locations) to change the units used.\n\nThe current value is ${getTemperatureScale()}.") {
		}
	}	   
}

def sensorsPage() {
	// Only show sensors that are part of the chosen thermostat(s)
	// Refactor to show the sensors under their corresponding Thermostats. Use Thermostat name as section header?
	LOG("=====> sensorsPage() entered. settings: ${settings}", 5)
	atomicState.sensorsPageVisited = true
	boolean ST = isST
	boolean HE = !ST

	def options = getEcobeeSensors() ?: []
	def numFound = options.size() ?: 0
	
	LOG("options = getEcobeeSensors == ${options}")

	dynamicPage(name: "sensorsPage", title: (HE?'<b>':'') + "Sensor Selection" + (HE?'</b>':''), nextPage: "") {
		if (numFound > 0)  {
			section("${HE?'Click':'Tap'} below to see the list of ecobee sensors available for the selected thermostat(s) and choose the ones you want to connect to ${ST?'SmartThings':'Hubitat'}."){
				LOG("sensorsPage(): atomicState.settingsCurrentSensors=${atomicState.settingsCurrentSensors}   ecobeesensors=${settings.ecobeesensors}", 4, null, "trace")
				if (atomicState.settingsCurrentSensors != settings.ecobeesensors) {
					LOG("atomicState.settingsCurrentSensors != ecobeesensors determined!!!", 4, null, "trace")					
				} else { LOG("atomicState.settingsCurrentSensors == ecobeesensors: No changes detected!", 4, null, "trace") }
				input(name: "ecobeesensors", title:"Select Ecobee Sensors (${numFound} found)", type: "enum", required:false, description: "Tap to choose", multiple:true, options: options)
			}
			if (showThermsAsSensor) { 
				section("NOTE: Thermostats are included as an available sensor to allow for actual temperature values to be used.") { }
			}
		} else {
			 // No sensors associated with this set of Thermostats was found
		   LOG("sensorsPage(): No sensors found.", 4)
		   section("No associated sensors were found. Click Done above ${settings.thermostats?'.':' and select one or more Thermostats.'}") { }
		}		 
	}
}

def askAlexaPage() {
	boolean HE = isHE
	
	dynamicPage(name: "askAlexaPage", title: (HE?'<b>':'') + 'Ask Alexa Integration' + (HE?'</b>':''), nextPage: "") {
	
		if (HE) {
			section('Ask Alexa integration is not supported on Hubitat (yet)') {
				paragraph(image: 'https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/ask-alexa.src/AskAlexa@2x.png', 
					title: 'Ask Alexa Integration', '')
			}
		} else if (atomicState.askAlexaMQ == null) {
			section('Ask Alexa either isn\'t installed, or no Message Queues are defined (sending to the deprecated Primary Message Queue is not supported).\n\n' +
					'Please verify your Ask Alexa settings and then return here to complete the integration.') {
				paragraph(image: 'https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/ask-alexa.src/AskAlexa@2x.png', 
					title: 'Ask Alexa Integration', '')
			}
		} else {
			section("${app.label} can send Ecobee Alerts and Reminders to one or more Ask Alexa, where Alexa can deliver them as messages and/or notificaitons.") {
				paragraph(image: 'https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/ask-alexa.src/AskAlexa@2x.png', 
					title: 'Ask Alexa Integration', '')
				input(name: 'askAlexa', type: 'bool', title: 'Send Ecobee Alerts & Reminders to Ask Alexa?', required: false, submitOnChange: true, defaultValue: false)
			}
			if (askAlexa) {
				section("${app.label} can send Ecobee Alerts and Reminders to one or more Ask Alexa Message Queues.") {
					input(name: 'listOfMQs', type: 'enum', title: 'Send to these Ask Alexa Message Queues', description: 'Tap to select', options: atomicState.askAlexaMQ, submitOnChange: true, 
						multiple: true, required: true)
				}
				section("${app.label} can automatically acknowledge Ecobee Alerts and Reminders when they are deleted from the Ask Alexa Message Queue${(listOfMQs&&(listOfMQs.size()>1))?'s':''}?") {
					input(name: 'ackOnDelete', type: 'bool', title: 'Acknowledge on delete?', required: false, submitOnChange: false, defaultValue: false)
				}
				section('Ask Alexa can automatically expire Ecobee Alerts and Reminders if they are not acknowledged within a specified time limit.') {
					input(name: 'expire', type: 'number', title: 'Automatically expire Alerts & Reminders after how many hours (optional)?', description: 'Tap to set', submitOnChange: true, required: false, range: "0..*", defaultValue: '')
				}
				if (expire) {
					section("Do you want to automatically acknowledge Ecobee Alerts and Reminders when they are expired from the Ask Alexa Message Queue${(listOfMQs&&(listOfMQs.size()>1))?'s':''}?") {
						input(name: 'ackOnExpire', type: 'bool', title: 'Acknowledge on expiry?', required: false, defaultValue: false)
					}
				} 
			}
		}
	}
}

def preferencesPage() {
	LOG("=====> preferencesPage() entered. settings: ${settings}", 5)
	boolean ST = isST
	boolean HE = !ST
	
	dynamicPage(name: "preferencesPage", title: (HE?'<b>':'') + 'Update Ecobee Suite Manager Preferences' + (HE?'</b>':''), nextPage: "") {
		if (ST) {
			section("Notifications") {
				paragraph "Notifications are only sent when the Ecobee API connection is lost and unrecoverable, at most once per hour."
				input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777; +441234567890)", required: false, submitOnChange: true)
				input( name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, required: true, submitOnChange: true)
				input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
				if (settings.speak) {
					input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
					input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
					if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
				}
				if (!settings.phone && !settings.pushNotify && !settings.speak) paragraph "WARNING: Notifications configured, but nowhere to send them!"
				paragraph("A notification is always sent to the Hello Home log whenever an action is taken")
			}
		} else {		// isHE
			section("<b>Use Notification Device(s)</b>") {
				paragraph "Notifications are only sent when the Ecobee API connection is lost and unrecoverable, at most 1 per hour."
				input(name: "notifiers", type: "capability.notification", title: "", required: false, multiple: true, 
					  description: "Select notification devices", submitOnChange: true)
				paragraph ""
			}
			section("<b>Use SMS to Phone(s) (limit 10 messages per day)</b>") {
				input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777, +441234567890)", required: false, submitOnChange: true)
				paragraph ""
			}
			section("<b>Use Speech Device(s)</b>") {
				input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
				if (settings.speak) {
					input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
					input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
					if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
				}
				paragraph "A 'HelloHome' notification is always sent to the Location Event log\n"
			}
		}
		
		if (ST) {
			section("Select your mobile device type to enable device-specific optimizations") {
				input(name: 'mobile', type: 'enum', options: ["Android", "iOS"], title: 'Tap to select your mobile device type', defaultValue: 'iOS', submitOnChange: true, required: true, multiple: false)
			}
		}
		
		section((HE?'<b>':'') + "How long do you want Hold events to last?" + (HE?'</b>':'')) {
			input(name: "holdType", title:"Select Hold Type", type: "enum", required:false, multiple:false, defaultValue: "Until I Change", submitOnChange: true, description: "Until I Change", 
				options: ["Until I Change", "Until Next Program", "2 Hours", "4 Hours", "Specified Hours", "Thermostat Setting"])
			if (settings.holdType=="Specified Hours") {
				input(name: 'holdHours', title:'How many hours (1-48)?', type: 'number', range:"1..48", required: true, description: '2', defaultValue: 2)
			} else if (settings.holdType=='Thermostat Setting') {
				paragraph("Thermostat setting at time of hold request will be used.")
			}
		}	
		section("The 'Smart Auto Temperature Adjust' feature determines if you want to allow the thermostat setpoint to be changed using the arrow buttons in the Tile when the thermostat is in 'auto' mode.") {
			input(name: "smartAuto", title:"Use Smart Auto Temperature Adjust?", type: "bool", required:false, defaultValue: false, description: "")
		}	 
		section("How frequently do you want to poll the Ecobee cloud for changes?") {	
			input(name: "pollingInterval", title:"Polling Interval (in Minutes)", type: "enum", required:false, multiple:false, defaultValue:5, description: "5", options:["1", "2", "3", "5", "10", "15", "30"])
		}
		section("Showing a Thermostat as a separate Sensor is useful if you need to access the actual temperature in the room where the Thermostat is located and not just the (average) temperature displayed on the Thermostat") {
			input(name: "showThermsAsSensor", title:"Include Thermostats as a separate Ecobee Sensor?", type: "bool", required:false, defaultValue: false, description: "")
		}
		section("Set the pause between pressing the setpoint arrows and initiating the API calls. The pause needs to be long enough to allow you to click the arrow again for changing by more than one degree.") {
			input(name: "arrowPause", title:"Delay timer value after pressing setpoint arrows", type: "enum", required:false, multiple:false, description: "4", defaultValue:5, options:["1", "2", "3", "4", "5"])
		}
		section("Select the desired number of decimal places to display for all temperatures (recommended 1 for C, 0 for F).") {
			String digits = wantMetric() ? "1" : "0"
			input(name: "tempDecimals", title:"Decimal places to display", type: "enum", required:false, multiple:false, defaultValue:digits, description: digits, options:["0", "1", "2"], submitOnChange: true)
		}
		section("Select the debug logging level (higher levels send more information to IDE Live Logging). A setting of 2 is recommended for normal operations") {
			input(name: "debugLevel", title:"Debug Logging Level", type: "enum", required:false, multiple:false, defaultValue:2, description: "2", options:["5", "4", "3", "2", "1", "0"])			  
		}
	}
}

def debugDashboardPage() {
	LOG("=====> debugDashboardPage() entered.", 5)	  
	boolean HE = isHE
	
	dynamicPage(name: "debugDashboardPage", title: "") {
		section((HE?'<b>':'') + getVersionLabel() + (HE?'</b>':''))
		section((HE?'<b>':'') + "Commands" + (HE?'</b>':'')) {
			href(name: "pollChildrenPage", title: "", required: false, page: "pollChildrenPage", description: "Tap to execute: pollChildren()")
			href(name: "refreshAuthTokenPage", title: "", required: false, page: "refreshAuthTokenPage", description: "Tap to execute: refreshAuthToken()")
			href(name: "updatedPage", title: "", required: false, page: "updatedPage", description: "Tap to execute: updated()")
		}		
		
		section((HE?'<b>':'') + "Settings Information" + (HE?'</b>':'')) {
			paragraph "debugLevel: ${getDebugLevel()}"
			paragraph "holdType: ${getHoldType()}"
			if (settings.holdType && settings.holdType.contains('Hour')) paragraph "holdHours: ${getHoldHours()}"
			paragraph "pollingInterval: ${getPollingInterval()}"
			paragraph "showThermsAsSensor: ${showThermsAsSensor} (default=false if null)"
			paragraph "smartAuto: ${smartAuto} (default=false if null)"	  
			paragraph "Selected Thermostats: ${settings.thermostats}"
			paragraph "Selected Sensors: ${settings.ecobeesensors}"
			paragraph "Decimal Precision: ${getTempDecimals()}"
			if (askAlexa) paragraph 'Ask Alexa support is enabled'
		}
		section((HE?'<b>':'') + "Dump of Debug Variables" + (HE?'</b>':'')) {
			def debugParamList = getDebugDump()
			LOG("debugParamList: ${debugParamList}", 4, null, "debug")
			//if ( debugParamList?.size() > 0 ) {
			if ( debugParamList != null ) {
				debugParamList.each { key, value ->	 
					LOG("Adding paragraph: key:${key}  value:${value}", 5, null, "trace")
					paragraph "${key}: ${value}"
				}
			}
		}
		section((HE?'<b>':'') + "Commands" + (HE?'</b>':'')) {
			href(name: "pollChildrenPage", title: "", required: false, page: "pollChildrenPage", description: "Tap to execute command: pollChildren()")
			href ("removePage", description: "Tap to remove Ecobee Suite Manager ", title: "")
		}
	}	 
}

// pages that are part of Debug Dashboard
def pollChildrenPage() {
	LOG("=====> pollChildrenPage() entered.", 1)
	def updatesLog = atomicState.updatesLog
	updatesLog.forcePoll = true // Reset to force the poll to happen
	atomicState.updatesLog = updatesLog
	pollChildren()
	
	dynamicPage(name: "pollChildrenPage", title: "") {
		section() {
			paragraph "pollChildren() was called"
		}
	}	 
}

// pages that are part of Debug Dashboard
def updatedPage() {
	LOG("=====> updatedPage() entered.", 5)
	updated()
	
	dynamicPage(name: "updatedPage", title: "") {
		section() {
			paragraph "updated() was called"
		}
	}	 
}

def refreshAuthTokenPage() {
	LOG("=====> refreshAuthTokenPage() entered.", 5)
	refreshAuthToken()
	
	dynamicPage(name: "refreshAuthTokenPage", title: "") {
		section() {
			paragraph "refreshAuthTokenPage() was called"
		}
	}	 
}

def helperSmartAppsPage() {
	LOG("helperSmartAppsPage() entered", 5)
	boolean ST = isST
	boolean HE = !ST

	LOG("The available Helper ${ST?'Smart':''}Apps are ${getHelperSmartApps()}", 5, null, "info")
	
	dynamicPage(name: "helperSmartAppsPage", title: (HE?'<b>':'') + "Ecobee Suite Helper ${ST?'Smart':''}Apps" + (HE?'</b>':''), install: true, uninstall: false, submitOnChange: true) { 
		section((HE?'<b>':'') + "Global Pause: ${HE?'<span style=color:red>':''}${settings.pauseHelpers?'ON':'OFF'}${HE?'</span>':''}" + (HE?'</b>':'')) {
			// paragraph "Global Pause will pause all Helpers that are not already paused; un-pausing will restore only the Helpers that weren't already paused. "
			input(name: "pauseHelpers", type: "bool", title: "Global Pause all Helpers?", defaultValue: ((atomicState.appsArePaused == null) ? false : atomicState.appsArePaused), submitOnChange: true)
			input(name: 'pauseSwitch', type: 'capability.switch', title: 'Synchronize Global Pause with this Switch (optional)', multiple: false, required: false, submitOnChange: true) 
			if (settings?.pauseHelpers != atomicState.appsArePaused) {
				if (settings?.pauseHelpers) {
					childAppsPauser( true )
					if (settings?.pauseSwitch) settings.pauseSwitch.on()
					atomicState.appsArePaused = true
				} else {
					childAppsPauser( false )
					if (settings?.pauseSwitch) settings.pauseSwitch.off()
					atomicState.appsArePaused = false
				}
			}
			if (HE)	 paragraph "NOTE: the '(paused)' status for the installed Helpers displayed below may not change until you refresh this page."
		}
		// if (ST) section("Installed Helper SmartApps") {}
		section((HE?'<b>':'') + "Avalable Helper ${ST?'Smart':''}Apps" + (HE?'</b>':'')) {
			getHelperSmartApps().each { oneApp ->
				LOG("Processing the app: ${oneApp}", 4, null, "trace")			  
				def allowMultiple = ST ? oneApp.multiple.value : oneApp.multiple
				app(name:"${ST?oneApp.name.value:oneApp.name}", appName:"${ST?oneApp.appName.value:oneApp.appName}", namespace:"${ST?oneApp.namespace.value:oneApp.namespace}", 
					title:"${ST?oneApp.title.value:oneApp.title}", multiple: allowMultiple)
			}
		}
	}
}
// End Preference Pages

// Preference Pages Helpers
boolean testForDeviceHandlers() {
	if (atomicState.runTestOnce != null) { 
		List myChildren = getAllChildDevices()
		if (atomicState.runTestOnce == false) {
			removeChildDevices( myChildren, true )	// Delete any leftover dummy (test) children
			atomicState.runTestOnce = null
			return false
		} else {
			return true
		}
	}
	
	def DNIAdder = now().toString()
	def d1
	def d2
	boolean success
	List myChildren = getAllChildDevices()
	if (myChildren.size() > 0) removeChildDevices( myChildren, true )	// Delete my test children
	log.debug "testing for device handlers"
	try {		
		d1 = addChildDevice(myNamespace, getChildThermostatName(), "dummyThermDNI-${DNIAdder}", location.hubs[0]?.id, ["label":"Ecobee Suite Thermostat:TestingForInstall", completedSetup:true])
		d2 = addChildDevice(myNamespace, getChildSensorName(), "dummySensorDNI-${DNIAdder}", location.hubs[0]?.id, ["label":"Ecobee Suite Sensor:TestingForInstall", completedSetup:true])
		if ((d1 != null) && (d2 != null)) success = true
	} catch (Exception e) {
		log.debug "Exception ${e}"
		if ("${e}".startsWith("${isST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) { 
			LOG("You MUST add the ${getChildThermostatName()} and ${getChildSensorName()} Device Handlers to the IDE BEFORE running the setup.", 1, null, "error")
			success = false
		}
	}
	log.debug "device handlers = ${success}"
	try {
		if (d1) deleteChildDevice("dummyThermDNI-${DNIAdder}") 
		if (d2) deleteChildDevice("dummySensorDNI-${DNIAdder}")
	} catch (Exception e) {
		LOG("Error ${e} deleting test devices (${d1}, ${d2})",1,null,'warn')
	}
	
	runIn(5, delayedRemoveChildren, [overwrite: true])
	atomicState.runTestOnce = success
	
	return success
}

void delayedRemoveChildren() {
	def myChildren = getAllChildDevices()
	if (myChildren.size() > 0) removeChildDevices( myChildren, true )
}

void removeChildDevices(devices, dummyOnly = false) {
	if (devices != []) {
		LOG("Removing ${dummyOnly?'test':''} child devices",3,null,'trace')
	} else {
		return		// nothing to remove
	}
	def devName
	try {
		devices?.each {
			devName = it.displayName
			LOG("Child: ${it.deviceNetworkId} - ${devName}",4,null,'trace')
			if (!dummyOnly || it?.deviceNetworkId?.startsWith('dummy')) deleteChildDevice(it.deviceNetworkId)
		}
	} catch (Exception e) {
		LOG("Error ${e} removing device ${devName}",1,null,'warn')
	}
}

// End Preference Pages Helpers

// Ask Alexa Helpers
String getMQListNames() {
	if (!listOfMQs || (listOfMQs?.size() == 0)) return ''
	def temp = []
	listOfMQs?.each { 
		temp << atomicState.askAlexaMQ?.getAt(it).value
	}
	return ((temp.size() > 1) ? ('(' + temp.join(", ") + ')') : (temp.toString()))?.replaceAll("\\[",'').replaceAll("\\]",'')
}

void askAlexaMQHandler(evt) {
	LOG("askAlexaMQHandler ${evt?.name} ${evt?.value}",4,null,'trace')
	
	// Ignore null events
	if (!evt || !settings.askAlexa) return
	
	// Always collect the List of Message Queues when refreshed, even if not currently integrating with Ask Alexa
	if (evt.value == 'refresh') {
		atomicState.askAlexaMQ = evt.jsonData && evt.jsonData?.queues ? evt.jsonData.queues : []
		return
	}
 
	if (!askAlexa) return								// Not integrated with Ask Alexa
	if (!ackOnExpire && !ackOnDelete) return	// Doesn't want these automatically acknowledged
	
	// Handle our messages that were deleted or expired (evt.value == 'Ecobee Status.msgID')
	if (evt.value.startsWith('Ecobee Status.')) {
		def askAlexaAlerts = atomicState.askAlexaAlerts				// Get the lists of alerts we have sent to Ask Alexa
		def askAlexaAppAlerts = atomicState.askAlexaAppAlerts
		if (!askAlexaAlerts && !askAlexaAppAlerts) return			// Looks like we haven't any outstanding alerts at the moment
		
		def messageID = evt.value.drop(14)
		if (askAlexaAlerts) {
			askAlexaAlerts.each { tid ->
				if (askAlexaAlerts[tid]?.contains(messageID)) {
					def deleteType = (evt.jsonData && evt.jsonData?.deleteType) ? evt.jsonData.deleteType : ''	// deleteType: "delete", "delete all" or "expire"
					if (ackOnExpire && (deleteType == 'expire')) {
						// Acknowledge this expired message
						//log.debug "askAlexaMQHandler(): request to acknowledge expired ${messageID}"
						acknowledgeEcobeeAlert( tid.toString(), messageID )
					} else if (ackOnDelete && (deleteType.startsWith('delete'))) {
						// Acknowledge this deleted message
						//log.debug "askAlexaMQHandler(): request to acknowledge deleted ${messageID}"
						acknowledgeEcobeeAlert( tid.toString(), messageID )
					}
					askAlexaAlerts[tid].removeAll{ it == messageID }
					if (!askAlexaAlerts[tid]) askAlexaAlerts[tid] = []
					atomicState.askAlexaAlerts = askAlexaAlerts
				}
			}
		}
		// AppAlerts are generated locally, so no need to acknowledge back to Ecobee
		if (askAlexaAppAlerts) {
			askAlexaAppAlerts.each { tid ->
				if (askAlexaAppAlerts[tid]?.contains(messageID)) {
					askAlexaAppAlerts[tid].removeAll{ it == messageID }
					if (!askAlexaAppAlerts[tid]) askAlexaAppAlerts[tid] = []
					atomicState.askAlexaAppAlerts = askAlexaAppAlerts
				}
			}
		}
	}
}

void deleteAskAlexaAlert( String type, String msgID ) {
	LOG("Deleting ${type} Ask Alexa message ID ${msgID}",2,null,'info')
	sendLocationEvent( name: 'AskAlexaMsgQueueDelete', value: type, unit: msgID, isStateChange: true, data: [ queues: listOfMQs ])
}

void acknowledgeEcobeeAlert( String deviceId, String ackRef ) {
	//					   {"functions":[{"type":"resumeProgram"}],"selection":{"selectionType":"thermostats","selectionMatch":"YYY"}}
	// def jsonRequestBody = '{"functions":[{"type":"acknowledge"}],"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '","ackRef":"' + ackRef + '","ackType":"accept"}}'
	
	//					   {"functions":[{"type":"resumeProgram","params":{"resumeAll":"'+allStr+'"}}],"selection":{"selectionType":"thermostats","selectionMatch":"YYY"}}
	def jsonRequestBody = '{"functions":[{"type":"acknowledge","params":{"thermostatIdentifier":"' + deviceId + '","ackRef":"' + ackRef + '","ackType":"accept"}}],"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"}}'
	LOG("acknowledgeEcobeeAlert(${deviceId},${ackRef}): jsonRequestBody = ${jsonRequestBody}", 4, child)
	// need to get child from the deviceId
	result = sendJson(null, jsonRequestBody)
	if (result) LOG("Acknowledged Ecobee Alert ${ackRef} for thermostat ${deviceId})",2,null,'info')
}
// End of Ask Alexa Helpers

// OAuth Init URL
def oauthInitUrl() {
	LOG("oauthInitUrl with callback: ${callbackUrl}", 2)
	atomicState.oauthInitState = isST ? UUID.randomUUID().toString() : stateUrl // HE does redirect a little differently
	//log.debug "oauthInitState: ${atomicState.oauthInitState}"
	
	boolean chg = false
	if (isST && atomicState?.initialized && (atomicState?.apiKey == null)) chg = true; atomicState.initialized = false;	 // Force changing over the the new ST key
	def oauthParams = [
		response_type:	"code",
		client_id:		ecobeeApiKey,					// actually, the Ecobee Suite app's client ID
		scope:			"smartRead,smartWrite,ems", 
		redirect_uri:	callbackUrl,
		state:			atomicState.oauthInitState
	]
	if (isST && chg) atomicState.initialized = true
		
	LOG("oauthInitUrl - ${isST?'Before redirect:':''} location: ${apiEndpoint}/authorize?${toQueryString(oauthParams)}", 2, null, 'debug')
	if (isST) {
		redirect(location: "${apiEndpoint}/authorize?${toQueryString(oauthParams)}")
	} else {
		return "${apiEndpoint}/authorize?${toQueryString(oauthParams)}"
	}
}
void parseAuthResponse(resp) {
	log.debug "response data: ${resp.data}"
	log.debug "response.headers: "
	resp.headers.each { log.debug "${it.name} : ${it.value}"}
	log.debug "response contentType: ${resp.contentType}"
	log.debug("response status: "+resp.status)
	log.debug "response params: "
	resp.params.each {log.debug "${it.names}"}
}
// OAuth Callback URL and helpers
def callback() {
	LOG("callback()>> params: $params, params.code ${params.code}, params.state ${params.state}, atomicState.oauthInitState ${atomicState.oauthInitState}", 2, null, 'debug')
	def code = params.code
	def oauthState = params.state

	//verify oauthState == atomicState.oauthInitState, so the callback corresponds to the authentication request
	if (oauthState == atomicState.oauthInitState){
		LOG("callback() --> States matched!", 1)
		def tokenParams = [
			"grant_type": "authorization_code",
			code	  : code,
			client_id : ecobeeApiKey,
			state	  : oauthState,
			redirect_uri: callbackUrl
		]

		def tokenUrl = "${apiEndpoint}/token?${toQueryString(tokenParams)}"
		LOG("callback()-->tokenURL: ${tokenUrl}", 2)
		httpPost(uri: tokenUrl) { resp ->
			atomicState.refreshToken = resp.data.refresh_token
			atomicState.authToken = resp.data.access_token

			LOG("Expires in ${resp?.data?.expires_in} seconds")
			atomicState.authTokenExpires = now() + (resp.data.expires_in * 1000)
			LOG("swapped token: $resp.data; atomicState.refreshToken: ${atomicState.refreshToken}; atomicState.authToken: ${atomicState.authToken}", 2)
		}

		if (atomicState.authToken) { success() } else { fail() }

	} else {
		LOG("callback() failed oauthState != atomicState.oauthInitState", 1, null, "warn")
	}
}
/*
	def requestBody = '{"selection":{"selectionType":"registered","selectionMatch":"","includeRuntime":true,"includeSensors":true,"includeLocation":true,"includeProgram":true}}'
	def deviceListParams = [
			uri: apiEndpoint,
			path: "/1/thermostat",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.authToken}"],
			query: [format: 'json', body: requestBody]
	]
*/
def success() {
	def message = """
	<p>Your ecobee Account is now connected to ${isST?'SmartThings':'Hubitat'}!</p>
	<p>${isHE?'Close this window and c':'C'}lick 'Done' to finish setup.</p>
	"""
	connectionStatus(message)
}

def fail() {
	def message = """
		<p>The connection could not be established!</p>
		<p>${isHE?'Close this window and c':'C'}lick 'Done' to return to the menu.</p>
	"""
	connectionStatus(message)
}
def connectionStatus(message, redirectUrl = null) {
	def redirectHtml = ""
	if (redirectUrl) {
		redirectHtml = """
			<meta http-equiv="refresh" content="3; url=${redirectUrl}" />
		"""
	}
	String hubIcon = isST ? 'https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png' : 'https://raw.githubusercontent.com/SANdood/Icons/master/Hubitat/HubitatLogo.png'

	def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Ecobee & ${isST?'SmartThings':'Hubitat'} connection</title>
<style type="text/css">
		@font-face {
				font-family: 'Swiss 721 W01 Thin';
				src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
				src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
						 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
						 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
						 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
				font-weight: normal;
				font-style: normal;
		}
		@font-face {
				font-family: 'Swiss 721 W01 Light';
				src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
				src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
						 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
						 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
						 url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
				font-weight: normal;
				font-style: normal;
		}
		.container {
				width: 90%;
				padding: 4%;
				/*background: #eee;*/
				text-align: center;
		}
		img {
				vertical-align: middle;
		}
		p {
				font-size: 2.2em;
				font-family: 'Swiss 721 W01 Thin';
				text-align: center;
				color: #666666;
				padding: 0 40px;
				margin-bottom: 0;
		}
		span {
				font-family: 'Swiss 721 W01 Light';
		}
</style>
</head>
<body>
		<div class="container">
				<img src="https://s3.amazonaws.com/smartapp-icons/Partner/ecobee%402x.png" alt="ecobee icon" />
				<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
				<img src="${hubIcon}" alt="${isST?'SmartThings':'Hubitat'} logo" />
				${message}
		</div>
</body>
</html>
"""
	render contentType: 'text/html', data: html
}
// End OAuth Callback URL and helpers

// Get the list of Ecobee Thermostats for use in the settings pages
def getEcobeeThermostats() {	
	LOG("====> getEcobeeThermostats() entered", 2,null,'trace')
	
	def requestBody = '{"selection":{"selectionType":"registered","selectionMatch":"","includeRuntime":true,"includeSensors":true,"includeLocation":true,"includeProgram":true}}'
	def deviceListParams = [
			uri: apiEndpoint,
			path: "/1/thermostat",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"], // was state.authToken
			query: [format: 'json', body: requestBody]
	]

	def stats = [:]
	def statLocation = [:]
	try {
		httpGet(deviceListParams) { resp ->
		LOG("getEcobeeThermostats() - httpGet() response: ${resp.data}", 4)
		
		//	the Thermostat Data. Will reuse for the Sensor List intialization
		atomicState.thermostatData = resp.data			
		
			if (resp.status == 200) {
				LOG("getEcobeeThermostats() - httpGet() in 200 Response", 3)
				atomicState.numAvailTherms = resp.data.thermostatList?.size() ?: 0
				
				resp.data.thermostatList.each { stat ->
					def dni = (isHE?'ecobee_suite-thermostat-':'') + ([app.id, stat.identifier].join('.'))	// HE App.ID is just too short :)
					stats[dni] = getThermostatDisplayName(stat)
					statLocation[stat.identifier] = stat.location
				}
			} else {				
				LOG("getEcobeeThermostats() - httpGet() in else: http status: ${resp.status}", 1)
				//refresh the auth token
				if (resp.status == 500 && resp.data?.status?.code == 14) {
					LOG("getEcobeeThermostats() - Storing the failed action to try later", 1)
					atomicState.action = "getEcobeeThermostats"
					LOG("getEcobeeThermostats() - Refreshing your auth_token!", 1)
					refreshAuthToken()
				} else {
					LOG("getEcobeeThermostats() - Other error. Status: ${resp.status}  Response data: ${resp.data} ", 1)
				}
			}
		}
	} catch(Exception e) {
		LOG("___exception getEcobeeThermostats(): ${e}", 1, null, "error")
		refreshAuthToken()
	}
	atomicState.thermostatsWithNames = stats
	atomicState.statLocation = statLocation
	LOG("getEcobeeThermostats() - thermostatsWithNames: ${stats}, locations: ${statLocation}", 4, null, 'trace')
	return stats.sort { it.value }
}

// Get the list of Ecobee Sensors for use in the settings pages (Only include the sensors that are tied to a thermostat that was selected)
// NOTE: getEcobeeThermostats() should be called prior to getEcobeeSensors to refresh the full data of all thermostats
Map getEcobeeSensors() {	
	LOG("====> getEcobeeSensors() entered. thermostats: ${settings.thermostats}", 5)
	def debugLevelFour = debugLevel(4)
	def sensorMap = [:]
	def foundThermo = null
	// TODO: Is this needed?
	atomicState.remoteSensors = [:]	   

	// Now that we routinely only collect the data that has changed in atomicState.thermostatData, we need to ALWAYS refresh that data
	// here so that we are sure we have everything we need here.
	getEcobeeThermostats()
	
	atomicState.thermostatData.thermostatList.each { singleStat ->
		def tid = singleStat.identifier
		if (debugLevelFour) LOG("thermostat loop: singleStat.identifier == ${tid} -- singleStat.remoteSensors == ${singleStat.remoteSensors} ", 4)	 
		def tempSensors = atomicState.remoteSensors
		if (!settings.thermostats.findAll{ it.contains(tid) } ) {
			// We can skip this thermostat as it was not selected by the user
			if (debugLevelFour) LOG("getEcobeeSensors() --> Skipping this thermostat: ${tid}", 4)
		} else {
			if (debugLevelFour) LOG("getEcobeeSensors() --> Entering the else... we found a match. singleStat == ${singleStat.name}", 4)
						
			// atomicState.remoteSensors[tid] = atomicState.remoteSensors[tid] ? (atomicState.remoteSensors[tid] + singleStat.remoteSensors) : singleStat.remoteSensors
			tempSensors[tid] = tempSensors[tid] ? tempSensors[tid] + singleStat.remoteSensors : singleStat.remoteSensors
			if (debugLevelFour) LOG("After atomicState.remoteSensors setup...", 5)			
						
			if (debugLevelFour) {
				LOG("getEcobeeSensors() - singleStat.remoteSensors: ${singleStat.remoteSensors}", 4)
				LOG("getEcobeeSensors() - atomicState.remoteSensors: ${atomicState.remoteSensors}", 4)
			}
		}
		atomicState.remoteSensors = tempSensors
		
		// WORKAROUND: Iterate over remoteSensors list and add in the thermostat DNI
		//		 This is needed to work around the dynamic enum "bug" which prevents proper deletion
		LOG("remoteSensors all before each loop: ${atomicState.remoteSensors}", 5, null, "trace")
		atomicState.remoteSensors[tid].each {
			if (debugLevelFour) LOG("Looping through each remoteSensor. Current remoteSensor: ${it}", 5, null, "trace")
			if (it?.type == "ecobee3_remote_sensor") {
				if (debugLevelFour) LOG("Adding an ecobee3_remote_sensor: ${it}", 4, null, "trace")
				def value = "${it?.name} (${it?.code})"
				def key = "ecobee_suite-sensor-${it?.id}-${it?.code}"
				sensorMap["${key}"] = value
			} else if ( (it?.type == "thermostat") && (settings.showThermsAsSensor == true) ) {				
				if (debugLevelFour) LOG("Adding a Thermostat as a Sensor: ${it}", 4, null, "trace")
				def value = "${it?.name}"
				def key = "ecobee_suite-sensor_tstat-${it?.id}-${it?.name}"
				if (debugLevelFour) LOG("Adding a Thermostat as a Sensor: ${it}, key: ${key}  value: ${value}", 4, null, "trace")
				sensorMap["${key}"] = value + " (Thermostat)"
			} else if ( it?.type == "control_sensor" && it?.capability[0]?.type == "temperature") {
				// We can add this one as it supports temperature
				if (debugLevelFour) LOG("Adding a control_sensor: ${it}", 4, null, "trace")
				def value = "${it?.name}"
				// def key = "ecobee_suite-control_sensor-${it?.id}"			// old DNI format
				def key = "ecobee_suite-control_sensor-${tid}.${it?.id}"		// New format include Thermostat ID
				sensorMap["${key}"] = value
			} else if (it?.type == "monitor_sensor" && it?.capability[0]?.type == "temperature" && it?.name != "") {
				if (debugLevelFour) LOG("Adding a monitor_sensor: ${it}", 4, null, "trace")
				def value = "${it?.name}"
				def key = "ecobee_suite-monitor_sensor-${tid}.${it?.id}"
				sensorMap["${key}"] = value
			} else {
				if (debugLevelFour) LOG("Did NOT add: ${it}. showThermsAsSensor=${showThermsAsSensor}", 4, null, "trace")
			}
		}
	} // end thermostats.each loop
	
	if (debugLevelFour) LOG("getEcobeeSensors() - remote sensor list: ${sensorMap}", 4)
	sensorMap = sensorMap.sort { it.value }
	atomicState.eligibleSensors = sensorMap
	atomicState.numAvailSensors = sensorMap.size() ?: 0
	return sensorMap
}
	 
String getThermostatDisplayName(stat) {
	if(stat?.name)
		return stat.name.toString()
	return (getThermostatTypeName(stat) + " (${stat.identifier})").toString()
}

String getThermostatTypeName(stat) {
	return stat.modelNumber == "siSmart" ? "Smart Si" : "Smart"
}

void installed() {
	LOG("Installed with settings: ${settings}",2,null,'trace')	
	initialize()
}

void uninstalled() {
	LOG("Uninstalling...",2,null,'warn')
	unschedule()
	// unsubscribe()
	removeChildDevices( getAllChildDevices(), false )	// delete all my children!
	// Child apps are supposedly automatically deleted.
}

void updated() {	
	LOG("Updated with settings: ${settings}",2,null,'trace')	
	
	if (!atomicState?.atomicMigrate) {
		LOG("updated() - Migrating state to atomicState...", 2, null, "warn")		 
		try {
			state.collect {
				LOG("traversing state: ${it} name: ${it.key}  value: ${it.value}")
				atomicState."${it.key}" = it.value
			}			
			atomicState.atomicMigrate = true
		} catch (Exception e) {
			LOG("updated() - Migration of state t- atomicState failed with exception (${e})", 1, null, "error")			   
		}
		try {
			LOG("atomicState after migration", 4)
			atomicState.collect {
				LOG("Traversing atomicState: ${it} name: ${it.key}	value: ${it.value}")
			}
		} catch (Exception e) {
			LOG("Unable to traverse atomicState", 2, null, "warn")
		}
		atomicState.atomicMigrate = true
	}
	initialize()
}



def rebooted(evt) {
	LOG("Hub rebooted, re-initializing", 1, null, 'debug')
	initialize()
}

def initialize() {	
	LOG("=====> initialize()", 4)	 
	
	LOG("Running on ${getHubPlatform()}.", 1, null, 'info')
	boolean ST = atomicState.isST
	
	atomicState.timers = false
	
	atomicState.connected = "full"
	atomicState.wifiAlert = false
	updateMyLabel()
	atomicState.reAttempt = 0
	atomicState.reAttemptPoll = 0
			
	try {
		unsubscribe()
		unschedule() // reset all the schedules
	} catch (Exception e) {
		LOG("updated() - Exception encountered trying to unschedule(). Exception: ${e}", 1, null, "error")
	}	 
	
	subscribe(app, appHandler)
	if (ST) subscribe(location, "AskAlexaMQ", askAlexaMQHandler) // HE version doesn't support Ask Alexa yet
	// if (!askAlexa) atomicState.askAlexaAlerts = null
	if (settings.pauseSwitch) {
		subscribe(settings.pauseSwitch, 'switch', pauseSwitchHandler)
	}
	
	def nowTime = now()
	def nowDate = getTimestamp()
	
	// Initialize several variables	   
	atomicState.lastScheduledPoll = nowTime
	atomicState.lastScheduledPollDate = nowDate
	atomicState.lastScheduledWatchdog = nowTime
	atomicState.lastScheduledWatchdogDate = nowDate
	atomicState.lastPoll = nowTime
	atomicState.lastPollDate = nowDate	  
	atomicState.lastWatchdog = nowTime
	atomicState.lastWatchdogDate = nowDate
	atomicState.lastUserDefinedEvent = now()
	atomicState.lastUserDefinedEventDate = getTimestamp()  
	atomicState.lastRevisions = [:]
	atomicState.latestRevisions = [:]
	atomicState.skipCount = 0
	atomicState.getWeather = null
	atomicState.runtimeUpdated = null
	atomicState.thermostatUpdated = null
	atomicState.sendJsonRetry = false
	atomicState.forcePoll = null				// make sure we get ALL the data after initialization
    atomicState.vacationTemplate = null
	def updatesLog = [thermostatUpdated:true, runtimeUpdated:true, forcePoll:true, getWeather:true, alertsUpdated:true, settingsUpdated:false, programUpdated:false, locationUpdated:false, 
    				  eventsUpdated:false, sensorsUpdated:false, extendRTUpdated:false]
	atomicState.updatesLog = updatesLog
	atomicState.hourlyForcedUpdate = 0
	atomicState.needExtendedRuntime = true		// we'll stop getting it once we decide we don't need it
	if (!atomicState.reservations) atomicState.reservations = [:]

	getTimeZone()		// these will set/refresh atomicState.timeZone
	getZipCode()		// and atomicState.zipCode (because atomicState.forcePoll is true)
	
   
	// get sunrise/sunset for the location of the thermostats (getZipCode() prefers thermostat.location.postalCode)
	// def sunriseAndSunset = (atomicState.zipCode != null) ? getSunriseAndSunset(zipCode: atomicState.zipCode) : getSunRiseAndSunset()
	def sunriseAndSunset = null
	if (atomicState.zipCode) {
		LOG("Trying to get sunrise/set using postal code '${atomicState.zipCode}'",1,null,'info')
		try {
			sunriseAndSunset = getSunriseAndSunset(zipCode: atomicState.zipCode)
		} catch (Exception e) {
			LOG("Failed to get sunrise/set using postal code: Exception: ${e}",1,null,'error')
			sunriseAndSunset = null
		}
	} 
	if (!sunriseAndSunset && location.longitude && location.latitude) {
		LOG("Trying to get sunrise/set using geographic coordinates for '${location.name}'",1,null,'info')
		sunriseAndSunset = getSunriseAndSunset()
	} else {
		if (!sunriseAndSunset) LOG("*** INITIALIZATION ERROR *** PLEASE SET LATITUDE/LONGITUDE FOR LOCATION '${location.name}' IN YOUR MOBILE APP",1,null,'error')
	}
	boolean isOk = false
	if (!sunriseAndSunset || (!(sunriseAndSunset.sunrise instanceof Date) || !(sunriseAndSunset.sunset instanceof Date))) {
		LOG("Can't get valid sunrise/set times, using generic defaults (05:00-->18:00)",1,null,'warn')
	} else {
		isOk = true
		LOG("Got valid sunrise/set data: ${sunriseAndSunset}",1,null,'info')
	}
	if(isOk && atomicState.timeZone) {
		// using the thermostat's time zone
		atomicState.sunriseTime = sunriseAndSunset.sunrise.format("HHmm", TimeZone.getTimeZone(atomicState.timeZone)).toInteger()
		atomicState.sunsetTime = sunriseAndSunset.sunset.format("HHmm", TimeZone.getTimeZone(atomicState.timeZone)).toInteger()
	} else if( isOk ) {
		atomicState.sunriseTime = sunriseAndSunset.sunrise.format("HHmm").toInteger()
		atomicState.sunsetTime = sunriseAndSunset.sunset.format("HHmm").toInteger()
	} else {
		atomicState.sunriseTime = "0500".toInteger()
		atomicState.sunsetTime = "1800".toInteger()
	}

	// Must do this AFTER setting up sunrise/sunset
	atomicState.timeOfDay = getTimeOfDay()
		
	// Setup initial polling and determine polling intervals
	atomicState.pollingInterval = getPollingInterval()
	atomicState.watchdogInterval = 10	// In minutes
	atomicState.reAttemptInterval = 15	// In seconds
	
	if (atomicState.initialized) {		
		// refresh Thermostats and Sensor full lists
		getEcobeeThermostats()
		getEcobeeSensors()
	} 
	
	// Children
	boolean aOK = true
	if (settings.thermostats?.size() > 0) { aOK = aOK && createChildrenThermostats() }
	if (settings.ecobeesensors?.size() > 0) { aOK = aOK && createChildrenSensors() }	
	deleteUnusedChildren()
   
	// Clear out all atomicState object collections (in case a thermostat was deleted or replaced, so we don't slog around useless old data)
	// also has the effect of forcing ALL the data bits to be sent down to the thermostats/sensors again
	atomicState.alerts = [:]
	atomicState.askAlexaAlerts = [:]
	atomicState.askAlexaAppAlerts = [:]
	atomicState.changeAlerts = [:]
	atomicState.changeAttrs = [:]
	atomicState.changeCloud = [:]
	atomicState.changeConfig = [:]
	atomicState.changeDevice = [:]
	atomicState.changeEquip = [:]
	atomicState.changeNever = [:]
	atomicState.changeOften = [:]
	atomicState.changeRarely = [:]
	atomicState.currentProgramName = [:]
	atomicState.equipmentStatus = [:]
	atomicState.events = [:]
	atomicState.extendedRuntime = [:]
	atomicState.oemCfg = [:]
	atomicState.program = [:]
	atomicState.runningEvent = [:]
	atomicState.runtime = [:]
	atomicState.settings = [:]
	atomicState.statInfo = [:]
	atomicState.statLocation = [:]
	atomicState.statTime = [:]
	atomicState.weather = [:]
	
	// Initial poll()
	if (settings.thermostats?.size() > 0) { pollInit() }
	
	// Add subscriptions as little "daemons" that will check on our health	  
	subscribe(location, scheduleWatchdog)
	if (ST) subscribe(location, "routineExecuted", scheduleWatchdog)		// HE doesn't support Routines
	subscribe(location, "sunset", sunsetEvent)
	subscribe(location, "sunrise", sunriseEvent)
	subscribe(location, "position", scheduleWatchdog)
	subscribe(location, "systemStart", rebooted)								// re-initialize if the hub reboots (HE only?)
	   
	// Schedule the various handlers
	LOG("Spawning scheduled events from initialize()", 5, null, "trace")
	if (settings.thermostats?.size() > 0) { 
		LOG("Spawning the poll scheduled event. (thermostats?.size() - ${settings.thermostats?.size()})", 4)
		spawnDaemon("poll", false) 
	} 
	spawnDaemon("watchdog", false)
	
	//send activity feeds to tell that device is connected
	def notificationMessage = aOK ? "is connected to ${ST?'SmartThings':'Hubitat'}" : "had an error during setup of devices"
	sendActivityFeeds(notificationMessage)
	atomicState.timeSendPush = null
	if (!atomicState.initialized) {
		atomicState.initialized = true
		// These two below are for debugging and statistics purposes
		atomicState.initializedEpic = nowTime
		atomicState.initializedDate = nowDate
	}
	runIn(180, forceNextPoll, [overwrite: true])		// get ALL the data once things settle down
	LOG("${getVersionLabel()} - initialization complete",1,null,'debug')
	atomicState.versionLabel = getVersionLabel()
	return aOK
}
def pauseSwitchHandler(evt) {
	if (evt.value == 'on') {
		childAppsPauser( true )
		atomicState.appsArePaused = true
		app.updateSetting("pauseHelpers", true)
	} else {
		childAppsPauser( false )
		atomicState.appsArePaused = false
		app.updateSetting("pauseHelpers", false)
	}
}

def createChildrenThermostats() {
	LOG("createChildrenThermostats() entered: thermostats=${settings.thermostats}", 5, null, 'trace')
	// Create the child Thermostat Devices
	def devices = settings.thermostats.collect { dni ->
		def d = getChildDevice(dni)
		if(!d) {			
			try {
				d = addChildDevice(myNamespace, getChildThermostatName(), dni, location.hubs[0]?.id, ["label":"EcobeeTherm: ${atomicState.thermostatsWithNames[dni]}", completedSetup:true])			
			} catch (Exception e) {
				if ("${e}".startsWith("${isST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) {
					LOG("You MUST add the ${getChildThermostatName()} Device Handler to the IDE BEFORE running the setup.", 1, null, "error")
					return false
				}
			}
			LOG("created ${d.displayName} with id ${dni}", 4, null, 'trace')
		} else {
			LOG("found ${d.displayName} with id ${dni} already exists", 4, null, 'trace')			 
		}
		// return d
	}
	LOG("Created/Updated ${devices.size()} thermostats", 4, null, 'trace')	  
	return true
}

def createChildrenSensors() {
	LOG("createChildrenSensors() entered: ecobeesensors=${settings.ecobeesensors}", 5, null, 'trace')
	// Create the child Ecobee Sensor Devices
	def sensors = settings.ecobeesensors.collect { dni ->
		def d = getChildDevice(dni)
		if(!d) {			
			try {
				d = addChildDevice(myNamespace, getChildSensorName(), dni, location.hubs[0]?.id, ["label":"EcobeeSensor: ${atomicState.eligibleSensors[dni]}", completedSetup:true])
			} catch (Exception e) { //(physicalgraph.app.exception.UnknownDeviceTypeException e) {
				if ("${e}".startsWith("${isST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) {
					LOG("You MUST add the ${getChildSensorName()} Device ${isST?'Handler':'Driver'} to the ${getHubPlatform} IDE BEFORE running the setup.", 1, null, "error")
					return false
				}
			}
			LOG("created ${d.displayName} with id $dni", 4, null, 'trace')
		} else {
			LOG("found ${d.displayName} with id $dni already exists", 4, null, 'trace')
		}
		// return d
	}
	LOG("Created/Updated ${sensors.size()} sensors.", 4, null, 'trace')
	return true
}

// somebody pushed my button - do a force poll
def appHandler(evt) {
	if (evt.value == 'touch') {
		LOG('appHandler(touch) event, forced poll',2,null,'info')
		def updatesLog = atomicState.updatesLog
		updatesLog.forcePoll = true
		updatesLog.getWeather = true	// update the weather also
		atomicState.updatesLog = updatesLog
		pollEcobeeAPI('')
		//pollChildren(null, true)		// double force-poll
		//atomicState.runningCallQueue = false
		//atomicState.callQueue = [:]
		//resumeProgram(null, '311019854581', true)
	}
}

// For thermostat reservations handling
def isChildApp(String childId) {
	//boolean isChild = false
	//def children = getChildApps()
	//children.each {
	//	if (isChild || (it.id == childId)) {
	//		isChild = true
	//	  }
	//}
	def child = getChildApps().find { it.id.toString() == childId }
	return (child != null)
}
String getChildAppName(String childId) {
	// String childName
	// def children = getChildApps()
	def child = getChildApps().find { it.id.toString() == childId }
	//children.each {
	//	if (!childName && (it.id.toString() == childId)) {
	//		childName = it.label
	//	  }
	//}
	return child ? child.label : ''
}
void childAppsPauser(pause = true) {
	getChildApps().each { child ->
		if (debugLevel(4)) LOG("Pauser(${pause}), child: ${child.label} (${child.id})",4,null,'trace')
		if (pause) { child.pauseOn() } else { child.pauseOff() }
	}
}

// NOTE: For this to work correctly getEcobeeThermostats() and getEcobeeSensors() should be called prior
void deleteUnusedChildren() {
	LOG("deleteUnusedChildren() entered", 5, null, 'trace')
	
	// Always make sure that the dummy devices were deleted
	removeChildDevices(getAllChildDevices(), true)		// Delete dummy devices
	
	if (settings.thermostats?.size() == 0) {
		// No thermostats, need to delete all children
		LOG("Deleting All My Children!", 2, null, "warn")
		removeChildDevices(getAllChildDevices(), false)			
	} else {
		// Only delete those that are no longer in the list
		// This should be a combination of any removed thermostats and any removed sensors
		def allMyChildren = getAllChildDevices()
		LOG("These are currently all of my children: ${allMyChildren}", 4, null, "debug")
		
		// Update list of "eligibleSensors"		  
		def childrenToKeep = (settings.thermostats ?: []) + (atomicState.eligibleSensors?.keySet() ?: [])
		LOG("These are the children to keep around: ${childrenToKeep}", 4, null, "trace")
		
		def childrenToDelete = allMyChildren.findAll { !childrenToKeep.contains(it.deviceNetworkId) }		 
		if (childrenToDelete.size() > 0) {
			LOG("Ready to delete these devices: ${childrenToDelete}", 2, null, "warn")
			childrenToDelete?.each { deleteChildDevice(it.deviceNetworkId) } //inherits from SmartApp (data-management)	   
		}
	}	 
}

def sunriseEvent(evt) {
	LOG("sunriseEvent() - with evt (${evt?.name}:${evt?.value})", 4, null, "info")
	atomicState.timeOfDay = "day"
	atomicState.lastSunriseEvent = now()
	atomicState.lastSunriseEventDate = getTimestamp()
	
	// def sunriseAndSunset = atomicState.zipCode ? getSunriseAndSunset(zipCode: atomicState.zipCode) : getSunRiseAndSunset()
	def sunriseAndSunset = getSunriseAndSunset(zipCode: atomicState.zipCode)
	if (!(sunriseAndSunset.sunrise instanceof Date)) {
		// the zip code is invalid or didn't return the data as expected
		LOG("sunriseAndSunset not set as expected, using default hub location")
		sunriseAndSunset = getSunriseAndSunset()
	}
	
	if(atomicState.timeZone) {
		atomicState.sunriseTime = sunriseAndSunset.sunrise.format("HHmm", TimeZone.getTimeZone(atomicState.timeZone)).toInteger()
	} else if( (sunriseAndSunset !=	 [:]) && (location != null) ) {
		atomicState.sunriseTime = sunriseAndSunset.sunrise.format("HHmm").toInteger()
	} else {
		atomicState.sunriseTime = evt.value.toInteger()
	}
	def updatesLog = atomicState.updatesLog
	updatesLog.forcePoll = true
	updatesLog.getWeather = true	// update the weather also
	atomicState.updatesLog = updatesLog
	scheduleWatchdog(evt, true)	   
}

def sunsetEvent(evt) {
	LOG("sunsetEvent() - with evt (${evt?.name}:${evt?.value})", 4, null, "info")
	atomicState.timeOfDay = "night"
	atomicState.lastSunsetEvent = now()
	atomicState.lastSunsetEventDate = getTimestamp()
	
	// get sunrise/sunset for the location of the thermostats (prefers thermostat.location.postalCode)
	// def sunriseAndSunset = atomicState.zipCode ? getSunriseAndSunset(zipCode: atomicState.zipCode) : getSunRiseAndSunset()
	def sunriseAndSunset = getSunriseAndSunset(zipCode: atomicState.zipCode)
	if (!(sunriseAndSunset.sunset instanceof Date)) {
		// the zip code is invalid or didn't return the data as expected
		LOG("sunriseAndSunset not set as expected, using default hub location")
		sunriseAndSunset = getSunriseAndSunset()
	}
	if(atomicState.timeZone) {
		atomicState.sunsetTime = sunriseAndSunset.sunset.format("HHmm", TimeZone.getTimeZone(atomicState.timeZone)).toInteger()
	} else if( (sunriseAndSunset !=	 [:]) && (location != null) ) {
		atomicState.sunsetTime = sunriseAndSunset.sunset.format("HHmm").toInteger()
	} else {
		atomicState.sunsetTime = evt.value.toInteger()
	}
//	  if(atomicState.timeZone) {
//		atomicState.sunsetTime = new Date().format("HHmm", atomicState.timeZone).toInteger()
//	} else {
//		atomicState.sunsetTime = new Date().format("HHmm").toInteger()
//	  }
	def updatesLog = atomicState.updatesLog
	updatesLog.forcePoll = true
	updatesLog.getWeather = true	// update the weather also
	atomicState.updatesLog = updatesLog
	scheduleWatchdog(evt, true)
}

// Event on a monitored device
def userDefinedEvent(evt) {
	if ( ((now() - atomicState.lastUserDefinedEvent) / 60000.0) < 0.5 ) { 
		if (debugLevel(4)) LOG("userDefinedEvent() - time since last event is less than 30 seconds, ignoring.", 1, null, 'trace')
		return 
	}
	
	if (debugLevel(4)) LOG("userDefinedEvent() - with evt (Device:${evt?.displayName} ${evt?.name}:${evt?.value})", 1, null, "info")
	
	poll()
	atomicState.lastUserDefinedEvent = now()
	atomicState.lastUserDefinedEventDate = getTimestamp()
	atomicState.lastUserDefinedEventInfo = "Event Info: (Device:${evt?.displayName} ${evt?.name}:${evt?.value})"
	
	def lastUserWatchdog = atomicState.lastUserWatchdogEvent
	if (lastUserWatchdog) {
		if ( ((now() - lastUserWatchdog) / 60000) < 3 ) {
			// Don't bother running the watchdog on EVERY userDefinedEvent - that's really overkill.
			if (debugLevel(4)) LOG('userDefinedEvent() - polled, but time since last watchdog is less than 3 minutes, exiting without performing additional actions', 4)
			return
		}
	}
	atomicState.lastUserWatchdogEvent = now()
	scheduleWatchdog(evt, true)
}

boolean scheduleWatchdog(evt=null, local=false) {
	boolean results = true	
	if (debugLevel(4)) {
		def evtStr = evt ? "${evt.name}:${evt.value}" : 'null'
		if (debugLevel(4)) LOG("scheduleWatchdog() called with evt (${evtStr}) & local (${local})", 1, null, "trace")
	}
	
	// Only update the Scheduled timestamp if it is not a local action or from a subscription
	if ( (evt == null) && (local==false) ) {
		atomicState.lastScheduledWatchdog = now()
		atomicState.lastScheduledWatchdogDate = getTimestamp()
		def updatesLog = atomicState.updatesLog
		updatesLog.getWeather = true								// next pollEcobeeApi for runtime changes should also get the weather object
		atomicState.updatesLog = updatesLog
		
		// do a forced update once an hour, just because (e.g., forces Hold Status to update completion date string)
		def counter = atomicState.hourlyForcedUpdate
		counter = (!counter) ? 1 : counter + 1
		if (counter == 6) {
			counter = 0
			updatesLog.forcePoll = true
			// atomicState.forcePoll = true
			atomicState.needExtendedRuntime = false // Force a recheck
			// log.debug "Skipping hourly forcePoll"
		}
		atomicState.hourlyForcedUpdate = counter
		atomicState.updatesLog = updatesLog
	}
	
	// Check to see if we have called too soon
	def timeSinceLastWatchdog = (atomicState.lastWatchdog == null) ? 0 :(now() - atomicState.lastWatchdog) / 60000
	if ( timeSinceLastWatchdog < 1 ) {
		if (debugLevel(4)) LOG("It has only been ${timeSinceLastWatchdog} since last scheduleWatchdog was called. Please come back later.", 1, null, "trace")
		return true
	}
   
	atomicState.lastWatchdog = now()
	atomicState.lastWatchdogDate = getTimestamp()
	def pollAlive = isDaemonAlive("poll")
	def watchdogAlive = isDaemonAlive("watchdog")
	
	if (debugLevel(4)) LOG("After watchdog tagging",4,null,'trace')
	if (apiConnected() == 'lost') {
		// Possibly a false alarm? Check if we can update the token with one last fleeting try...
		if( refreshAuthToken() ) { 
			// We are back in business!
			LOG("scheduleWatchdog() - Was able to recover the lost connection. Please ignore any notifications received.", 1, null, "warn")
		} else {		
			LOG("scheduleWatchdog() - Unable to schedule handlers do to loss of API Connection. Please ensure you are authorized.", 1, null, "error")
			return false
		}
	}

	LOG("scheduleWatchdog() --> pollAlive==${pollAlive}	 watchdogAlive==${watchdogAlive}", 4, null, "warn")
	
	// Reschedule polling if it has been a while since the previous poll	
	if (!pollAlive) { spawnDaemon("poll") }
	if (!watchdogAlive) { spawnDaemon("watchdog") }
	
	return true
}

// Watchdog Checker
boolean isDaemonAlive(daemon="all") {
	// Daemon options: "poll", "auth", "watchdog", "all"	
	def daemonList = ["poll", "auth", "watchdog", "all"]
	String preText = getDebugLevel() <= 2 ? '' : 'isDeamonAlive() - '
	Integer pollingInterval = getPollingInterval()

	daemon = daemon.toLowerCase()
	boolean result = true	 
	
	if (debugLevel(5)) LOG("isDaemonAlive() - now() == ${now()} for daemon (${daemon})", 5, null, "trace")
	
	// No longer running an auth Daemon, because we need the scheduler slot (max 4 scheduled things, poll + watchdog use 2)	
	// def timeBeforeExpiry = atomicState.authTokenExpires ? ((atomicState.authTokenExpires - now()) / 60000) : 0
	// LOG("isDaemonAlive() - Time left (timeBeforeExpiry) until expiry (in min): ${timeBeforeExpiry}", 4, null, "info")
	
	if (daemon == "poll" || daemon == "all") {
		def lastScheduledPoll = atomicState.lastScheduledPoll
		def timeSinceLastScheduledPoll = ((lastScheduledPoll == null) || (lastScheduledPoll == 0)) ? 1000 : ((now() - lastScheduledPoll) / 60000)
		if (debugLevel(4)) {
			LOG("isDaemonAlive() - Time since last poll? ${timeSinceLastScheduledPoll} -- lastScheduledPoll == ${lastScheduledPoll}", 4, null, "info")
			LOG("isDaemonAlive() - Checking daemon (${daemon}) in 'poll'", 4, null, "trace")
		}
		def maxInterval = pollingInterval + 2
		if ( timeSinceLastScheduledPoll >= maxInterval ) { result = false }
	}	
	
	if (daemon == "watchdog" || daemon == "all") {
		def lastScheduledWatchdog = atomicState.lastScheduledWatchdog
		def timeSinceLastScheduledWatchdog = ((lastScheduledWatchdog == null) || (lastScheduledWatchdog == 0)) ? 1000 : ((now() - lastScheduledWatchdog) / 60000)
		if (debugLevel(4)) {
			LOG("isDaemonAlive() - Time since watchdog activation? ${timeSinceLastScheduledWatchdog} -- lastScheduledWatchdog == ${lastScheduledWatchdog}", 4, null, "info")
			LOG("isDaemonAlive() - Checking daemon (${daemon}) in 'watchdog'", 4, null, "trace")
		}
		def maxInterval = atomicState.watchdogInterval + 2
		if (debugLevel(4)) LOG("isDaemonAlive(watchdog) - timeSinceLastScheduledWatchdog=(${timeSinceLastScheduledWatchdog})  Timestamps: (${atomicState.lastScheduledWatchdogDate}) (epic: ${lastScheduledWatchdog}) now-(${now()})", 4, null, "trace")
		if ( timeSinceLastScheduledWatchdog >= maxInterval ) { result = false }
	}
	
	if (!daemonList.contains(daemon) ) {
		// Unkown option passed in, gotta punt
		LOG("isDaemonAlive() - Unknown daemon: ${daemon} received. Do not know how to check this daemon.", 1, null, "error")
		result = false
	}
	if (debugLevel(4)) LOG("isDaemonAlive() - result is ${result}", 1, null, "trace")
	if (!result) LOG("${preText}(${daemon}) has died", 1, null, 'warn')
	return result
}

boolean spawnDaemon(daemon="all", unsched=true) {
	// log.debug "spawnDaemon(${daemon}, ${unsched})"
	// Daemon options: "poll", "auth", "watchdog", "all"	
	def daemonList = ["poll", "auth", "watchdog", "all"]
	Random rand = new Random()
	
	Integer pollingInterval = getPollingInterval()
	
	daemon = daemon.toLowerCase()
	boolean result = true
	
	if (daemon == "poll" || daemon == "all") {
		if (debugLevel(4)) LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'poll'", 1, null, "trace")
		// Reschedule the daemon
		try {
			if ( unsched ) { unschedule("pollScheduled") }
			if ( atomicState.isHE || canSchedule() ) { 
				LOG("Polling Interval == ${pollingInterval}", 4)
				//if ((pollingInterval < 5) && (pollingInterval > 3)) {	// choices were 1,2,3,5,10,15,30
				//	LOG("Using schedule instead of runEvery with pollingInterval: ${pollingInterval}", 4, null, 'trace')
				//	int randomSeconds = rand.nextInt(59)
				//	schedule("${randomSeconds} 0/${pollingInterval} * * * ?", "pollScheduled")					  
				//} else {
					LOG("Using runEvery to setup polling with pollingInterval: ${pollingInterval}", 4, null, 'trace')
					"runEvery${pollingInterval}Minute${pollingInterval!=1?'s':''}"("pollScheduled")
				//}
				// Only poll now if we were recovering - if not asked to unschedule, then whoever called us will handle the first poll (as in initialize())
				if (unsched) result = pollScheduled() && result
			} else {
				LOG("canSchedule() is NOT allowed or result already false! Unable to schedule poll daemon!", 1, null, "error")
				result = false
			}
		} catch (Exception e) {
			LOG("spawnDaemon() - Exception when performing spawn for ${daemon}. Exception: ${e}", 1, null, "error")
			result = false
		}		
	}
	
	if (daemon == "watchdog" || daemon == "all") {
		if (debugLevel(4)) LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'watchdog'", 1, null, "trace")
		// Reschedule the daemon
		try {
			if ( unsched ) { unschedule("scheduleWatchdog") }
			if ( atomicState.isHE || canSchedule() ) { 
				"runEvery${atomicState.watchdogInterval}Minutes"("scheduleWatchdog")
				result = result && true
			} else {
				LOG("canSchedule() is NOT allowed or result already false! Unable to schedule daemon!", 1, null, "error")
				result = false
			}
		} catch (Exception e) {
			LOG("spawnDaemon() - Exception when performing spawn for ${daemon}. Exception: ${e}", 1, null, "error")
			result = false
		}
		atomicState.lastScheduledWatchdog = now()
		atomicState.lastScheduledWatchdogDate = getTimestamp()
		def updatesLog = atomicState.updatesLog
		updatesLog.getWeather = true	// next pollEcobeeApi for runtime changes should also get the weather object
		atomicState.updatesLog = updatesLog
	}
	
	if (!daemonList.contains(daemon) ) {
		// Unkown option passed in, gotta punt
		LOG("isDaemonAlive() - Unknown daemon: ${daemon} received. Do not know how to check this daemon.", 1, null, "error")
		result = false
	}
	return result
}

void updateLastPoll(Boolean isScheduled=false) {
	if (isScheduled) {
		atomicState.lastScheduledPoll = now()
		atomicState.lastScheduledPollDate =	 getTimestamp()
	} else {
		atomicState.lastPoll = now()
		atomicState.lastPollDate = getTimestamp()
	}
}

def poll() {		
	if (debugLevel(4)) LOG("poll() - Running at ${getTimestamp()} (epic: ${now()})", 1, null, "trace")	 
	pollChildren() // Poll ALL the children at the same time for efficiency	   
}

// Called by scheduled() event handler
def pollScheduled() {
	// log.debug "pollScheduled()"
	updateLastPoll(true)
	if (debugLevel(4)) LOG("pollScheduled() - Running at ${atomicState.lastScheduledPollDate} (epic: ${atomicState.lastScheduledPoll})", 1, null, "trace")	  
	return poll()
}

// Called during initialization to get the inital poll
def pollInit() {
	if (debugLevel(4)) LOG("pollInit()", 4)
	def updatesLog = atomicState.updatesLog
	updatesLog.forcePoll = true
	atomicState.updatesLog = updatesLog
	runIn(2, pollChildren, [overwrite: true])		// Hit the ecobee API for update on all thermostats, in 2 seconds
}

void forceNextPoll() {
	def updatesLog = atomicState.updatesLog
	updatesLog.forcePoll = true
	atomicState.updatesLog = updatesLog
}

void pollChildren(deviceId=null,force=false) {

	// Just in case we need to re-initialize anything
	def version = getVersionLabel()
	if (atomicState.versionLabel != version) {
		LOG("Code updated: ${version}",1,null,'debug')
		atomicState.versionLabel = version
		runIn(2, updated, [overwrite: true])
		return
	}
	
	boolean debugLevelFour = debugLevel(4)
	//debugLevelFour = true
	if (debugLevelFour) LOG("pollChildren() - deviceId ${deviceId}", 1, child, 'trace')
	def forcePoll
	def updatesLog = atomicState.updatesLog
	if (deviceId != null) {
		updatesLog.forcePoll = force	//true
		forcePoll = force				//true
		atomicState.updatesLog = updatesLog
	} else {
		forcePoll = updatesLog.forcePoll
	}
	if (debugLevelFour) LOG("=====> pollChildren(${deviceId}) - forcePoll(${forcePoll}) atomicState.lastPoll(${atomicState.lastPoll}) now(${now()}) atomicState.lastPollDate(${atomicState.lastPollDate})", 1, child, "trace")
	
	if(apiConnected() == "lost") {
		// Possibly a false alarm? Check if we can update the token with one last fleeting try...
		if (debugLevel(3)) LOG("apiConnected() == lost, try to do a recovery, else we are done...", 3, null, "debug")
		if( refreshAuthToken() ) { 
			// We are back in business!
			LOG("pollChildren() - Was able to recover the lost connection. Please ignore any notifications received.", 1, null, "warn")
		} else {		
			LOG("pollChildren() - Unable to schedule handlers do to loss of API Connection. Please ensure you are authorized.", 1, null, "error")
			return
		}
	}

	// Run a watchdog checker here
	scheduleWatchdog(null, true)	
	
	if (settings.thermostats?.size() < 1) {
		LOG("pollChildren() - Nothing to poll as there are no thermostats currently selected", 1, null, "warn")
		return
	}	 
	
	// Check if anything has changed in the thermostatSummary (really don't need to call EcobeeAPI if it hasn't).
	String thermostatsToPoll = (deviceId != null) ? deviceId : getChildThermostatDeviceIdsString()
	
	boolean somethingChanged = forcePoll ?: checkThermostatSummary(thermostatsToPoll)
	// log.info "pollChildren() somethingChanged ${somethingChanged}"
	if (!forcePoll) thermostatsToPoll = atomicState.changedThermostatIds
	
	String preText = debugLevel(2) ? '' : 'pollChildren() - ' 
	if (forcePoll || somethingChanged) {
		//alertsUpdated = forcePoll || atomicState.alertsUpdated
		def tids = []
		tids = thermostatsToPoll.split(",")
		String names = ""
		tids.each { names = names == "" ? getThermostatName(it) : names+", "+getThermostatName(it) }
		LOG("${preText}Polling thermostat${thermostatsToPoll.contains(',')?'s':''} ${names} (${thermostatsToPoll})${forcePoll?' (forced)':''}", 2, null, 'info')
		pollEcobeeAPI(thermostatsToPoll)		// This will queue the async request, and values will be generated and sent from pollEcobeeAPICallback
	} else {	 
		LOG(preText+'No updates...', 2, null, 'trace')
	}
}

void generateAlertsAndEvents() {
	generateTheEvents()
	if (askAlexa) {
		def startMS = now()
		sendAlertEvents()
		def allDone = now()
		LOG("Alerts sent (${allDone - startMS} / ${allDone - atomicState.pollEcobeeAPIStart}ms)",2,null,'trace')
	}
}

void generateTheEvents() {
	boolean debugLevelFour = debugLevel(4)
	def startMS = now()
	def stats = atomicState.thermostats
	def sensors = atomicState.remoteSensorsData
	//log.debug stats
	stats?.each { DNI ->
		if (DNI.value?.data) getChildDevice(DNI.key)?.generateEvent(DNI.value.data)
	}
	sensors?.each { DNI ->
		if (DNI.value?.data) getChildDevice(DNI.key)?.generateEvent(DNI.value.data)
	}
	def allDone = now()
	/*if (debugLevelFour) */ LOG("Updates sent (${allDone-startMS} / ${allDone-atomicState.pollEcobeeAPIStart}ms)",2,null,'trace')
}

// enables child SmartApps to send events to child Smart Devices using only the DNI
void generateChildEvent( childDNI, dataMap) {
	getChildDevice(childDNI)?.generateEvent(dataMap)
}

// NOTE: This only updates the apiConnected state now - this used to resend all the data, but that's pretty much a waste of CPU cycles now.
//		 If the UI needs updating, the refresh now does a forcePoll on the entire device.
void generateEventLocalParams() {
	// Iterate over all the children
	
	def apiConnection = apiConnected()
	def lastPoll = (debugLevel(4)) ? "${apiConnection} @ ${atomicState.lastPollDate}" : (apiConnection=='full') ? 'Succeeded' : (apiConnection=='warn') ? 'Timed Out' : 'Failed'
	def data = [
		apiConnected: apiConnection,
		lastPoll: lastPoll
	]
	def LOGtype = (apiConnection == 'lost') ? 'error' : ((apiConnection == 'warn') ? 'warn' : 'info')
	if (debugLevel(2)) LOG("Updating API status with ${data}", 2, null, LOGtype)
	settings.thermostats?.each {
		getChildDevice(it)?.generateEvent(data)
	 }
}

// Checks if anything has changed since the last time this routine was called, using lightweight thermostatSummary poll
// NOTE: Despite the documentation, Runtime Revision CAN change more frequently than every 3 minutes - equipmentStatus is 
//		 apparently updated in real time (or at least very close to real time)
boolean checkThermostatSummary(String thermostatIdsString) {
	def startMS 
	if (atomicState.timers) startMS = now()

	def updatesLog = atomicState.updatesLog
	boolean thermostatUpdated = updatesLog.thermostatUpdated	// false
	boolean alertsUpdated = updatesLog.alertsUpdated			// false
	boolean runtimeUpdated = updatesLog.runtimeUpdated			// false
	boolean getAllStats = (runtimeUpdated || thermostatUpdated || alertsUpdated )
	
	String preText = getDebugLevel() <= 2 ? '' : 'checkThermostatSummary() - '
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("checkThermostatSummary() - ${thermostatIdsString}",1,null,'trace')
	if (thermostatIdsString == '') thermostatIdsString = getChildThermostatDeviceIdsString()
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + thermostatIdsString + '","includeEquipmentStatus":"true"}}'
	def pollParams = [
			uri: apiEndpoint,
			path: "/1/thermostatSummary",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
			query: [format: 'json', body: jsonRequestBody]
	]
	
	boolean result = false
	def statusCode=999
	String tstatsStr = ''	 
	if (debugLevelFour) LOG("checkThermostatSummary() pollParams: ${pollParams}",1,null,'trace')
	try {
		httpGet(pollParams) { resp ->
			if(resp?.status == 200) {
				if (debugLevelFour) LOG("checkThermostatSummary() - poll results returned resp.data ${resp.data}", 1, null, 'trace')
				// This is crazy, I know, but ST & HE return different junk on a failed call, thus the exhaustive verification to avoid throwing errors
				if ((resp.data != null) && (resp.data instanceof Map) && resp.data.containsKey('status')  && (resp.data.status != null) 
					&& (resp.data.status instanceof Map) && resp.data.status.containsKey('code') && (resp.data.status.code != null)) {
					statusCode = resp.data.status.code
				} else {
					LOG("checkThermostatSummary() - malformed resp.data", 2, null, 'warn')
				}
				if (statusCode == 0) { 
					def revisions = resp.data?.revisionList
					
					tstatsStr = ""
					def latestRevisions = [:]
					def lastRevisions = atomicState.lastRevisions
					if (lastRevisions && !(lastRevisions instanceof Map)) lastRevisions = [:]			// hack to switch from List to Map for revision cache
						
					// each revision is a separate thermostat
					revisions.each { rev ->
						def tempList = rev.split(':') as List
						def tstat = tempList[0]
						def latestDetails = tempList.reverse().take(4).reverse() as List	// isolate the 4 timestamps
						def lastDetails = ( lastRevisions && lastRevisions.containsKey(tstat)) ? lastRevisions[tstat] : []			// lastDetails should be the prior 4 timestamps
							
						// check if the current tstat is updated
						boolean tru = false
						boolean tau = false
						boolean ttu = false
						if (lastDetails) {			// if we have prior revision details
							if (lastDetails[2] != latestDetails[2]) tru = true	// runtime
							//if (settings.askAlexa && (lastDetails[1] != latestDetails[1])) tau = true	// alerts
							if (lastDetails[1] != latestDetails[1]) tau = true	// alerts
							// log.debug "alertsChanged? ${tau}"
							if (lastDetails[0] != latestDetails[0]) ttu = true	// thermostat 
						} else {					// no priors, assume all have been updated
							tru = true 
							if (settings.askAlexa) tau = true
							ttu = true
						}
						//log.debug "${tstat}: thermostat: ${ttu}, runtime: ${tru}, alerts: ${tau}"
/*
						// Check if the equipmentStatus changed but runtimeUpdated didn't
						if (!tru) {
							def equipStatus = (resp.data?.containsKey('statusList')) ? resp.data.statusList : null
							if (equipStatus != null) {
								def lastEquipStatus = atomicState.equipmentStatus
								LOG("equipStatus: ${equipStatus}, lastEquipStatus: ${lastEquipStatus}, tid: ${tstat}", 2, null, 'trace')
								equipStatus.each { status ->
									if (!tru) {
										def tList = status.split(':') as List
										if (tList[0] == tstat) {
											if (tList[1] == null) tList[1] = ''
											LOG("${tstat}: New: '${tList[1]}', Old: '${lastEquipStatus[tstat]}'", 2, null, 'trace')
											if (tList[1] != lastEquipStatus[tstat]) tru = true
										}
									}
								}
							}
							if (tru) LOG("Equipment Status changed, runtime did not (${equipStatus})", 2, null, 'info')
						}
*/
						// update global flags (we update the superset of changes for all requested tstats)
						if (tru || tau || ttu) {
							runtimeUpdated = (runtimeUpdated || tru)		// || atomicState.runtimeUpdated)
							alertsUpdated = (alertsUpdated || tau)			// || atomicState.alertsUpdated)
							thermostatUpdated = (thermostatUpdated || ttu)	// || atomicState.thermostatUpdated)
							result = true
							if (!getAllStats) tstatsStr = (tstatsStr=="") ? tstat : (tstatsStr.contains(tstat)) ? tstatsStr : tstatsStr + ",${tstat}"
						}
						if (getAllStats) tstatsStr = (tstatsStr=="") ? tstat : (tstatsStr.contains(tstat)) ? tstatsStr : tstatsStr + ",${tstat}"
						latestRevisions[tstat] = latestDetails			// and, log the new timestamps
					}
					//log.debug "${tstatsStr}: thermostat: ${thermostatUpdated}, runtime: ${runtimeUpdated}, alerts: ${alertsUpdated}"
					
					atomicState.latestRevisions = latestRevisions		// let pollEcobeeAPI update last with latest after it finishes the poll
					updatesLog.thermostatUpdated = thermostatUpdated	// Revised: settings, program, event, device
					//atomicState.alertsUpdated = askAlexa ? alertsUpdated : false // Revised: alerts (no need to get them if we're not doing AskAlexa)
					updatesLog.alertsUpdated = alertsUpdated			// Revised: alerts (no need to get them if we're not doing AskAlexa)
					updatesLog.runtimeUpdated = runtimeUpdated			// Revised: runtime, equip status, remote sensors, weather?
					atomicState.changedThermostatIds = tstatsStr		// only these thermostats need to be requested in pollEcobeeAPI
					// Tell the children that we are once again connected to the Ecobee API Cloud
					if (apiConnected() != "full") {
						apiRestored()
						generateEventLocalParams() // Update the connection status
					}
				} else if ( [14, 1, 2, 3].contains(statusCode)) {
					LOG("checkThermostatSummary() - status code: ${statusCode}, message: ${resp.data.status.message}", 2, null, 'warn')
					runIn(2, 'refreshAuthToken', [overwrite: true])
				} else {
					// if we get here, we had http status== 200, but API status != 0
					if (statusCode != 999) LOG("checkThermostatSummary() - Ecobee API status ${statusCode}, message: ${resp.data?.status?.message}", 1, null, 'warn')
					runIn(2, 'refreshAuthToken', [overwrite: true])
				}
			} else {
				LOG("${preText}ThermostatSummary poll got http status ${resp.status}", 1, null, "error")
			}
		}
		atomicState.inTimeoutRetry = 0
	} catch (groovyx.net.http.HttpResponseException e) {   
		result = false // this thread failed to get the summary
		if ((e.statusCode == 500) && (e?.response?.data?.status?.code == 14) /*&& ((atomicState.authTokenExpires - now()) <= 0) */){
			LOG('checkThermostatSummary() - HttpResponseException occurred: Auth_token has expired', 3, null, "info")
			atomicState.action = "pollChildren"
			if (debugLevelFour) LOG( "Refreshing your auth_token!", 1, null, 'trace')
			atomicState.inTimeoutRetry = 0
			if ( refreshAuthToken() ) {
				// Note that refreshAuthToken will reschedule pollChildren if it succeeds in refreshing the token...
				LOG('Checking: Auth_token refreshed', 2, null, 'info')
			} else {
				LOG('Checking: Auth_token refresh failed', 1, null, 'error')
			}
		} else {
			LOG("checkThermostatSummary() - HttpResponseException: ${e} StatusCode: ${e?.statusCode} response.data.status.code: ${e?.response?.data?.status?.code}", 1, null, "error")
		}
	} catch (java.util.concurrent.TimeoutException e) {
		LOG("checkThermostatSummary() - Concurrent Execution Timeout: ${e}.", 1, null, "warn")
		// Do not add an else statement to run immediately as this could cause an long looping cycle
		runIn(atomicState.reAttemptInterval, "pollChildren", [overwrite: true])
		result = false
	//
	// These appear to be transient errors, treat them all as if a Timeout... 
	} catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException | 
			javax.net.ssl.SSLPeerUnverifiedException | javax.net.ssl.SSLHandshakeException | 
			java.net.SocketTimeoutException | java.net.NoRouteToHostException | java.net.UnknownHostException |
			java.net.UnknownHostException e) {
		LOG("checkThermostatSummary() - ${e}.",1,null,'warn')  // Just log it, and hope for better next time...
		if (apiConnected != 'warn') {
			atomicState.connected = 'warn'
			updateMyLabel()
			atomicState.lastPoll = now()
			atomicState.lastPollDate = getTimestamp()
			generateEventLocalParams()
		}
		def inTimeoutRetry = atomicState.inTimeoutRetry
		if (inTimeoutRetry == null) inTimeoutRetry = 0
		if (inTimeoutRetry < 3) runIn(atomicState.reAttemptInterval, "pollChildren", [overwrite: true])
		atomicState.inTimeoutRetry = inTimeoutRetry + 1
		result = false
		// throw e
	} catch (Exception e) {
		LOG("checkThermostatSummary() - General Exception: ${e}, data: ${resp?.data}", 1, null, "error")
		result = false
		throw e
	}
	
	atomicState.updatesLog = updatesLog
	if (debugLevelFour) LOG("<===== Leaving checkThermostatSummary() result: ${result}, tstats: ${tstatsStr}", 1, null, "info")
	//if (atomicState.timers) log.debug "TIMER: checkThermostatSummary done (${now()-startMS}ms)"
	return result
}

boolean pollEcobeeAPI(thermostatIdsString = '') {
	boolean timers = atomicState.timers
	def pollEcobeeAPIStart = now()
	atomicState.pollEcobeeAPIStart = pollEcobeeAPIStart
	
	boolean debugLevelFour = debugLevel(4)
	boolean debugLevelThree = debugLevel(3)
	if (debugLevelFour) LOG("=====> pollEcobeeAPI() entered - thermostatIdsString = ${thermostatIdsString}", 1, null, "info")

	def updatesLog = atomicState.updatesLog
	boolean forcePoll =			updatesLog.forcePoll		// lightweight way to use atomicStates that we don't want to change under us
	boolean thermostatUpdated = updatesLog.thermostatUpdated
	boolean alertsUpdated =		updatesLog.alertsUpdated
	boolean runtimeUpdated =	updatesLog.runtimeUpdated
	boolean getWeather =		updatesLog.getWeather
	
	String preText = debugLevel(2) ? '' : 'pollEcobeeAPI() - '
	boolean somethingChanged = (thermostatIdsString != '')
	String allMyChildren = getChildThermostatDeviceIdsString()
	String checkTherms = thermostatIdsString?:allMyChildren

	// forcePoll = true
	if (thermostatIdsString == '') {
		somethingChanged = checkThermostatSummary(thermostatIdsString)
		if (somethingChanged) {
			updatesLog = atomicState.updatesLog
			thermostatUpdated = updatesLog.thermostatUpdated				// update these again after checkThermostatSummary
			alertsUpdated = updatesLog.alertsUpdated
			runtimeUpdated = updatesLog.runtimeUpdated
		}
		if (timers) log.debug "TIMER: checkThermostatSummary('') complete, changes=${somethingChanged} @ ${now() - pollEcobeeAPIStart}ms"
	}
	if (forcePoll) {
		// if it's a forcePoll, and thermostatIdString specified, we check only the specified thermostat, else we check them all
		if (thermostatIdsString) {
			checkTherms = thermostatIdsString
		} else {
			// checkTherms = allMyChildren
			atomicState.hourlyForcedUpdate = 0		// reset the hourly check counter if we are forcePolling ALL the thermostats
		} 
		somethingChanged = true
	} /* else if (atomicState.lastRevisions != atomicState.latestRevisions) {
		// we already know there are changes
		somethingChanged = true	
	} else {
		// log.debug "Shouldn't be checkingThermostatSummary() again!"
		somethingChanged = checkThermostatSummary(thermostatIdsString)
		// log.debug "somethingChanged ${somethingChanged}"
		if (somethingChanged) {
			thermostatUpdated = atomicState.thermostatUpdated				// update these again after checkThermostatSummary
			alertsUpdated = atomicState.alertsUpdated
			runtimeUpdated = atomicState.runtimeUpdated
		}
	} */
	
	// if nothing has changed, and this isn't a forced poll, just return (keep count of polls we have skipped)
	// This probably can't happen anymore...shouldn't even be here if nothing has changed and not a forced poll...
	if (!somethingChanged && !forcePoll) {	
		if (debugLevelFour) LOG("<===== Leaving pollEcobeeAPI() - nothing changed, skipping heavy poll...", 4 )
		return true		// act like we completed the heavy poll without error
	} else {
		// if getting the weather, get for all thermostats at the same time. Else, just get the thermostats that changed
		checkTherms = (getWeather) ? allMyChildren : checkTherms // (forcePoll ? checkTherms : atomicState.changedThermostatIds)
	}  
	
	// Let's only check those thermostats that actually changed...unless this is a forcePoll - or if we are getting the weather 
	// (we need to get weather for all therms at the same time, because we only update every 15 minutes and use the cached version
	// the rest of the time)
	
	// get equipmentStatus every time
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + checkTherms + '","includeEquipmentStatus":"true"'
	String gw = '( equipmentStatus'
	if (thermostatUpdated || forcePoll) {
		jsonRequestBody += ',"includeSettings":"true","includeProgram":"true","includeEvents":"true"'
		gw += ' settings program events'
	}
	if (forcePoll) {
		// oemCfg needed only for Carrier Cor, to determine if useSchedule is enabled - but if it isn't, we aren't of much use anyway
		// so for now, don't bother getting oemCfg data	 (BAB - May 24, 2017)
		jsonRequestBody += /*',"includeOemCfg":"true"*/ ',"includeLocation":"true"'
		gw += /*' oemCfg */ ' location'
	}
	if (alertsUpdated || forcePoll) {
		jsonRequestBody += ',"includeAlerts":"true"'
		gw += ' alerts'
	}
	if (runtimeUpdated || forcePoll) {
		jsonRequestBody += ',"includeRuntime":"true"'
		gw += ' runtime'
		
		// Always get extended runtime (we only save the 2 datapoints we need anymore)
		if (atomicState.needExtendedRuntime) {
			jsonRequestBody += ',"includeExtendedRuntime":"true"'
			gw += ' extendedRuntime'
		}

		// only get sensorData if we have any sensors configured
		if (settings.ecobeesensors?.size() > 0) {
			jsonRequestBody += ',"includeSensors":"true"'
			gw += ' sensors'
		}
		if (getWeather) {
			jsonRequestBody += ',"includeWeather":"true"'		// time to get the weather report (only changes every 15 minutes or so - watchdog sets this when it runs)
			gw += ' weather'
		}
	}
	gw += ' )'
	def tids = []
	tids = checkTherms.split(",")
	String names = ""
	tids.each { names = (names=="") ? getThermostatName(it) : names+", "+getThermostatName(it) }
	LOG("${preText}Requesting ${gw} for thermostat${checkTherms.contains(',')?'s':''} ${names} (${checkTherms})", 2, null, 'info')
	jsonRequestBody += '}}'	  
	if (debugLevelFour) LOG("pollEcobeeAPI() - jsonRequestBody is: ${jsonRequestBody}", 1, null, 'trace')
 
	// def result = false
	boolean ST = atomicState.isST
	
	def pollParams = [
			uri: apiEndpoint,
			path: "/1/thermostat",
			headers: ["Content-Type": "${ST?'application':'text'}/json", "Authorization": "Bearer ${atomicState.authToken}"],
		query: [format: 'json', body: jsonRequestBody, 'contenttype': "${ST?'application':'text'}/json" ]
	]
	def pollState = [
   //		debugLevelFour:		debugLevelFour,
   //		debugLevelThree:	debugLevelThree,
   //		forcePoll:			forcePoll,
   //		thermostatUpdated:	thermostatUpdated,
   //		alertsUpdated:		alertsUpdated,
   //		runtimeUpdated:		runtimeUpdated,
   //		getWeather:			getWeather,
   //		somethingChanged:	somethingChanged,
			thermostatIdsString:thermostatIdsString,
   //		jsonRequestBody:	jsonRequestBody,
			checkTherms:		checkTherms,
	]
	if (debugLevelFour) pollState += [thermostatIdsString:thermostatIdsString]
	
	if (timers) {
		def tNow = now()
		log.debug "TIMER: asyncPollPrep done (${tNow - atomicState.pollEcobeeAPIStart}ms)"
		atomicState.asyncPollStart = tNow
	}
	if (ST) {
		include 'asynchttp_v1'
		asynchttp_v1.get( pollEcobeeAPICallback, pollParams, pollState )
	} else {
		asynchttpGet( pollEcobeeAPICallback, pollParams, pollState )
	}
	atomicState.waitingForCallback = true
	return true
}

boolean pollEcobeeAPICallback( resp, pollState ) {
	def startMS = now()
	atomicState.waitingForCallback = false
	boolean timers = atomicState.timers
	def pollEcobeeAPIStart
	if (timers) {
		pollEcobeeAPIStart = atomicState.pollEcobeeAPIStart
		log.debug "TIMER: asyncPoll took ${startMS - atomicState.asyncPollStart}ms"
		log.debug "TIMER: Poll callback total time ${startMS - pollEcobeeAPIStart}ms"
	}
	boolean debugLevelFour = debugLevel(4)
	boolean debugLevelThree = debugLevel(3)
	
	def updatesLog = atomicState.updatesLog
	boolean forcePoll =			updatesLog.forcePoll
	boolean thermostatUpdated = updatesLog.thermostatUpdated
	boolean alertsUpdated =		updatesLog.alertsUpdated
	boolean runtimeUpdated =	updatesLog.runtimeUpdated
	boolean getWeather =		updatesLog.getWeather
	
	String preText = getDebugLevel() <= 2 ? '' : 'pollEcobeeAPICallback() - '
	// def somethingChanged =		pollState.somethingChanged
	String thermostatIdsString = debugLevelFour ? pollState.thermostatIdsString : ''
	// def jsonRequestBody =		pollState.jsonRequestBody
	def checkTherms =			pollState.checkTherms
	if (debugLevelFour) LOG("=====> pollEcobeeAPICallback() entered - thermostatIdsString = ${thermostatIdsString}", 1, null, "info")
	
	boolean result = true
	def tidList = []
	LOG("pollEcobeeAPICallback() status code ${resp?.status}",4,null,'trace')
	
	if (resp && /* !resp.hasError() && */ (resp.status?.toInteger() == 200)) {
		try {
			if (resp.json) {
				//*** ONLY SAVE THE tids!!!***
				// log.debug "Skipping store"
				// atomicState.thermostatData = resp.json		// this only stores the most recent collection of changed data
															// this may not be the entire set of data for all of our thermostats, so the rest 
															// of this code will calculate updates from the individual data objects (which 
															// always cache the latest values recieved)
			} else {
				// FAIL - no data
				LOG('pollEcobeeAPICallback() - poll - no JSON: ${resp.data}', 1, null, 'error')
				result = false
			}
		} catch (Exception e) {
			LOG("pollEcobeeAPICallback() - General Exception: ${e}", 1, null, "error")
			atomicState.reAttemptPoll = atomicState.reAttemptPoll + 1
			if (atomicState.reAttemptPoll > 3) {		
				apiLost("${preText}Too many retries (${atomicState.reAttemptPoll - 1}) for polling.")
			} else {
				LOG(preText+'Setting up retryPolling',1,null,'debug')
				runIn(atomicState.reAttemptInterval, "pollChildren", [overwrite: true]) 
			}
			throw e
			return false
		} 
		
		if (atomicState.reAttemptPoll > 0) apiRestored()
			
		// Update the atomicState caches for each received data object(s)
		def tempSettings = [:]
		def tempProgram = [:]
		def tempEvents = [:]
		def tempRuntime = [:]
		def tempExtendedRuntime = [:]
		def tempWeather = [:]
		def tempSensors = [:]
		def tempEquipStat = [:]
		def tempLocation = [:]
		def tempStatTime = atomicState.statTime
		def tempStatInfo = [:]
		def tempAlerts = [:]
		boolean programUpdated = false
		boolean settingsUpdated = false
		boolean eventsUpdated = false
		boolean locationUpdated = false
		boolean extendRTUpdated = false
		boolean sensorsUpdated = false
		boolean weatherUpdated = false
		boolean statInfoUpdated = false
		boolean equipUpdated = false
		boolean rtReallyUpdated = false
		
		// def tempOemCfg = [:]
		if (timers) log.debug "TIMER: pollEcobeeAPICallback() initialized @ (${now() - pollEcobeeAPIStart}ms)"

		// collect the returned data into temporary individual caches (because we can't update individual Map items in an atomicState Map)	
		// if (resp && resp.containsKey(data) && resp.data.containsKey(thermostatList)) { // oddly, this occaisionally returns no thermostatList :(
		if (result && resp.json.thermostatList) { // oddly, this sometimes returns no thermostatList :(
			resp.json.thermostatList.each { stat ->
				String tid = stat.identifier.toString()
				tidList += [tid]
				String tstatName = getThermostatName(tid)
				if (debugLevelThree) LOG("Parsing data for thermostat ${tstatName} (${tid})", 3, null, 'info')
				if (timers) log.debug "TIMER: Parsing data for ${tstatName} @ (${now() - pollEcobeeAPIStart}ms)"
				
				tempEquipStat[tid] = stat.equipmentStatus			// always store ("" is a valid return value)
				tempStatTime[tid] = stat.thermostatTime
						
				if (thermostatUpdated || forcePoll ) {
					if (stat.settings)	tempSettings[tid]	= stat.settings
					if (stat.program)	tempProgram[tid]	= stat.program
					if (stat.events)	tempEvents[tid]		= stat.events
					if (stat.location)	tempLocation[tid]	= stat.location
					// if (stat.oemCfg) tempOemCfg[tid] =	stat.oemCfg
					tempStatInfo[tid] = [	brand:			stat.brand,
											features:		stat.features,
											identifier:		stat.identifier as String,
											isRegistered:	stat.isRegistered,
											modelNumber:	stat.modelNumber,
											name:			stat.name]
				}	
				if (runtimeUpdated || forcePoll) {
					if (stat.runtime) {
						// Collect only the data values we use - no need to deal with the date/time/revisions (that always change)
						tempRuntime[tid] = [actualHumidity:		stat.runtime.actualHumidity,
											actualTemperature:	stat.runtime.actualTemperature,
											connected:			stat.runtime.connected,
											desiredCool:		stat.runtime.desiredCool,
											//desiredCoolRange:	stat.runtime.desiredCoolRange,
											desiredDehumidity:	stat.runtime.desiredDehumidity,
											desiredFanMode:		stat.runtime.desiredFanMode,
											desiredHeat:		stat.runtime.desiredHeat,
											//desiredHeatRange:	stat.runtime.desiredHeatRange,
											desiredHumidity:	stat.runtime.desiredHumidity,
											disconnectDateTime:	stat.runtime.disconnectDateTime,
											//rawTemperature:	stat.runtime.rawTemperature,
											//showIconMode:		stat.runtime.showIconMode
										   ]
					}
					if (stat.extendedRuntime) {
						// We only need 2 values from extendedRuntime (at the moment)
						tempExtendedRuntime[tid] = [desiredHumidity:	stat.extendedRuntime.desiredHumidity[2], 
													desiredDehumidity:	stat.extendedRuntime.desiredDehumidity[2]]
					}
					if (stat.remoteSensors)		tempSensors[tid] =			stat.remoteSensors
				}
				// let's don't copy all that weather info...we only need some 3 bits of that info
				if (getWeather || forcePoll) {
					if (stat.weather) {
						tempWeather[tid] = [timestamp:		stat.weather.timestamp,
											temperature:	stat.weather.forecasts[0]?.temperature.toString(),
											humidity:		stat.weather.forecasts[0]?.relativeHumidity,
											dewpoint:		stat.weather.forecasts[0]?.dewpoint,
											pressure:		stat.weather.forecasts[0]?.pressure,
											weatherSymbol:	stat.weather.forecasts[0]?.weatherSymbol.toString()]
					}
				}
				if (forcePoll || alertsUpdated) {
					tempAlerts[tid] = stat.alerts
				}
			}
		} else {
			LOG('pollEcobeeAPICallback() - poll: ${jsonRequestBody}; no thermostatList: ${resp.json}', 1, null, 'error')
			result = false
		}
				
		// OK, we have all the data received stored in their appropriate temporary cache, let's update the atomicState caches
		// ** Equipment Status **
		if (timers) log.debug "TIMER: Loading complete @ (${now() - pollEcobeeAPIStart}ms)"
		def tempAtomic = [:]
		if (result && (tempEquipStat != [:])) {
			tempAtomic = atomicState.equipmentStatus
			if (tempAtomic) {
				tidList.each { eqpTid ->
					if (tempAtomic[eqpTid] != tempEquipStat[eqpTid]) {
						equipUpdated = true
						tempAtomic[eqpTid] = tempEquipStat[eqpTid]
					}
				}
				if (equipUpdated) {
					atomicState.equipmentStatus = tempAtomic
				}
			} else {
				equipUpdated = true
				atomicState.equipmentStatus = tempEquipStat
			}
		}
		if (timers) log.debug "TIMER: equipmentStatus complete @ (${now() - pollEcobeeAPIStart}ms)"
		
		// ** Thermostat Status (Program, Events, Location) **
		// OK, Only copy the thermostat stuff (program, events, settings or location) that has changed
		if (result && (thermostatUpdated || forcePoll)) {
			if (tempProgram != [:]) {
				tempAtomic = atomicState.program
				if (tempAtomic) {
					tidList.each { prgTid ->
						if (!tempAtomic[prgTid] || (tempAtomic[prgTid] != tempProgram[prgTid])) {
							programUpdated = true
							tempAtomic[prgTid] = tempProgram[prgTid]
						}
					}
					if (programUpdated) {
						atomicState.program = tempAtomic
					} 
				} else {
					programUpdated = true
					atomicState.program = tempProgram
				}
			}
			if (tempStatInfo != [:]) {
				tempAtomic = atomicState.statInfo
				if (tempAtomic) {
					tidList.each { sfoTid ->
						if (!tempAtomic[sfoTid] || (tempAtomic[sfoTid] != tempStatInfo[sfoTid])) {
							statInfoUpdated = true
							tempAtomic[sfoTid] = tempStatInfo[sfoTid]
						}
					}
					if (statInfoUpdated) {
						atomicState.statInfo = tempAtomic
					}
				} else {
					statInfoUpdated = true
					atomicState.statInfo = tempStatInfo
				}
			}
			if (tempEvents != [:]) {
				tempAtomic = atomicState.events
				if (tempAtomic) {
					tidList.each { evtTid ->
						if (!tempAtomic[evtTid] || (tempAtomic[evtTid] != tempEvents[evtTid])) {
							eventsUpdated = true
							tempAtomic[evtTid] = tempEvents[evtTid]
						}
					}
					if (eventsUpdated) {
						atomicState.events = tempAtomic
					}
				} else {
					eventsUpdated = true
					atomicState.events = tempEvents
				}
			}
			if (tempLocation != [:]) {
				tempAtomic = atomicState.statLocation
				if (tempAtomic) {
					tidList.each { locTid ->
						if (!tempAtomic[locTid] || (tempAtomic[locTid] != tempLocation[locTid])) {
							locationUpdated = true
							tempAtomic[locTid] = tempLocation[locTid]
						}
					}
					if (locationUpdated) {
						atomicState.statLocation = tempAtomic
					}
				} else {
					locationUpdated = true
					atomicState.statLocation = tempLocation
				}
			}
			// if (tempOemCfg != [:]) {
			//	if (atomicState.oemCfg) tempOemCfg = atomicState.oemCfg + tempOemCfg
			//	atomicState.oemCfg = tempOemCfg
			// }
			boolean needExtRT = atomicState.needExtendedRuntime
			if (tempSettings != [:]) {
				tempAtomic = atomicState.settings
				if (tempAtomic) {
					tidList.each { setTid ->
						if (!tempAtomic[setTid] || (tempAtomic[setTid] != tempSettings[setTid])) {
							settingsUpdated = true
							tempAtomic[setTid] = tempSettings[setTid]
						}
					}
					if (settingsUpdated) {
						atomicState.settings = tempAtomic
					}
				} else {
					settingsUpdated = true
					atomicState.settings = tempSettings
				}
				// While we are here, let's find out if we really need the extendedRuntime data...
				// We do it here because: a) it only changes when settings changes, and b) we already have the settings Map in hand
				// (thus it saves the cost of copying atomicState.settings later
				tempSettings.each { 
					if (!needExtRT && checkTherms.contains(it.key)) {
						switch (it.value.hvacMode) {
							case 'heat':
							case 'auxHeatOnly':
								if (it.value.hasHumidifier && (it.value.humidifierMode != 'off')) needExtRT = true
								break;
							case 'cool':
								if ((it.value.dehumidifierMode == 'on') && (it.value.hasDehumidifier || 
																			(it.value.dehumidifyWithAC && 
																			 (it.value.dehumidifyOvercoolOffset != 0)))) needExtRT = true 
								break;
							case 'auto':
								if ((it.value.hasHumidifier && (it.value.humidifierMode != 'off')) || 
									(it.value.dehumidifierMode == 'on') && (it.value.hasDehumidifier || 
																			(it.value.dehumidifyWithAC && 
																			 (it.value.dehumidifyOvercoolOffset != 0)))) needExtRT = true
								break;
						}
						if (needExtRT) atomicState.needExtendedRuntime = true
					}
				}
			}
			if (timers) log.debug "TIMER: thermostatUpdated complete @ (${now() - pollEcobeeAPIStart}ms)"
		}
		
		// ** runtime Status (runtime, extended runtime, sensors, weather) **
		if (result && (runtimeUpdated || forcePoll)) {
			if (tempRuntime != [:]) {
				atomicState.holdMe = tempRuntime			// I don't know why, but the two won't 
				tempRuntime = atomicState.holdMe			// compare properly if I don't do thi first
				tempAtomic = atomicState.runtime
				if (tempAtomic) {
					tidList.each { runTid ->
						if (!tempAtomic[runTid] || (tempAtomic[runTid] != tempRuntime[runTid])) {
							rtReallyUpdated = true
							tempAtomic[runTid] = tempRuntime[runTid]
						}
					}
					if (rtReallyUpdated) {
						atomicState.runtime = tempAtomic
					}
				} else {
					rtReallyUpdated = true
					atomicState.runtime = tempRuntime
				}
			}
			if (tempExtendedRuntime != [:]) {
				tempAtomic = atomicState.extendedRuntime
				if (tempAtomic) {
					tidList.each { ertTid ->
						if (!tempAtomic[ertTid] || (tempAtomic[ertTid] != tempExtendedRuntime[ertTid])) {
							extendRTUpdated = true
							tempAtomic[ertTid] = tempExtendedRuntime[ertTid]
						}
					}
					if (extendRTUpdated) {
						atomicState.extendedRuntime = tempAtomic
					} 
				} else {
					extendRTUpdated = true
					atomicState.extendedRuntime = tempExtendedRuntime
				}
			}
			if (tempSensors != [:]) {
				tempAtomic = atomicState.remoteSensors
				if (tempAtomic) {
					tidList.each { snrTid ->
						if (!tempAtomic[snrTid] || (tempAtomic[snrTid] != tempSensors[snrTid])) {
							sensorsUpdated = true
							tempAtomic[snrTid] = tempSensors[snrTid]
						}
					}
					if (sensorsUpdated) {
						atomicState.remoteSensors = tempAtomic
					} 
				} else {
					sensorsUpdated = true
					atomicState.remoteSensors = tempSensors
				}
			}
			if (timers) log.debug "TIMER: runtimeUpdated complete @ (${now() - pollEcobeeAPIStart}ms)"
		}
		if (result && (getWeather || forcePoll)) {
			if (tempWeather != [:]) {
				tempAtomic = atomicState.weather
				if (tempAtomic) {
					tidList.each { wetTid ->
						if (!tempAtomic[wetTid] || (tempAtomic[wetTid] != tempWeather[wetTid])) {
							weatherUpdated = true
							tempAtomic[wetTid] = tempWeather[wetTid]
						}
					}
					if (weatherUpdated) {
						atomicState.weather = tempAtomic
					} 
				} else {
					weatherUpdated = true
					atomicState.weather = tempWeather
				}
			}
			if (timers) log.debug "TIMER: Weather complete @ (${now() - pollEcobeeAPIStart}ms)"
		}
		// ** Alerts Status **
		if (result && (alertsUpdated || forcePoll)) {
			if (tempAlerts != [:]) {
				alertsUpdated = false
				tempAtomic = atomicState.alerts
				if (tempAtomic) {
					tidList.each { lrtTid ->
						if (!tempAtomic[lrtTid] || (tempAtomic[lrtTid] != tempAlerts[lrtTid])) {
							alertsUpdated = true
							tempAtomic[lrtTid] = tempAlerts[lrtTid]
						}
					}
					if (alertsUpdated) {
						atomicState.alerts = tempAtomic
					}
				} else {
					alertsUpdated = true
					atomicState.alerts = tempAlerts
				}
			}
			if (timers) log.debug "TIMER: Alerts complete @ (${now() - pollEcobeeAPIStart}ms)"
		}
		
		// OK, Everything is safely stored, let's get to parsing the objects and finding the actual changed data
		if (result) {
			updateLastPoll()
			if (debugLevelThree) LOG('Parsing complete', 3, null, 'info')
			if (timers) log.debug "TIMER: Parsing complete @ (${now() - pollEcobeeAPIStart}ms)"
			
			updatesLog = [thermostatUpdated:thermostatUpdated, runtimeUpdated:runtimeUpdated, alertsUpdated:alertsUpdated, settingsUpdated:settingsUpdated, 
						  programUpdated:programUpdated, statInfoUpdated:statInfoUpdated, forcePoll:forcePoll, eventsUpdated:eventsUpdated, 
						  locationUpdated:locationUpdated, extendRTUpdated:extendRTUpdated, sensorsUpdated:sensorsUpdated, weatherUpdated:weatherUpdated, 
						  getWeather:getWeather, changedTids:tidList, equipUpdated:equipUpdated, rtReallyUpdated:rtReallyUpdated]
			atomicState.updatesLog = updatesLog
			atomicState.statTime = tempStatTime
						
			// Update the data and send it down to the devices as events
			if (debugLevelFour) LOG("T: ${thermostatUpdated} (s: ${settingsUpdated}, p: ${programUpdated}, e: ${eventsUpdated}), l: ${locationUpdated}, si: ${statInfoUpdated}, " +
					  "R: ${runtimeUpdated} (rr: ${rtReallyUpdated}, e: ${extendRTUpdated}, s: ${sensorsUpdated}), w: ${weatherUpdated}, A: ${alertsUpdated}, E: ${equipUpdated}", 1, null, 'trace')
			
			if (settings.thermostats) {
				if (thermostatUpdated || rtReallyUpdated || extendRTUpdated || settingsUpdated || weatherUpdated || equipUpdated) {
					updateThermostatData()		// update thermostats first (some sensor data is dependent upon thermostat changes)
				} else {
					LOG("No thermostat updates...", 2, null, 'info')
				}
			}
			if (settings.ecobeesensors) {						// Only update configured sensors (and then only if they have changes)
				if (sensorsUpdated) { 
					updateSensorData() 
				} else {
					LOG("No sensor updates...", 2, null, 'info')
				} 
			}
			// if (forcePoll || alertsUpdated) /*updateAlertsData() */ runIn(20, "updateAlertsData", [overwrite: true])	// Update alerts asynchronously
			
			// pollChildren() will actually send all the events we just created, once we return
			// just a bit more housekeeping...
			result = true
			atomicState.lastRevisions = atomicState.latestRevisions		// copy the Map
			updatesLog.forcePoll = false
			updatesLog.runtimeUpdated = false
			// if (thermostatUpdated) { atomicState.thermostatUpdated = false }
			def needPrograms = atomicState.needPrograms
			updatesLog.thermostatUpdated = thermostatUpdated ? false : needPrograms
			if (!thermostatUpdated && needPrograms) LOG("Need settings, programs & events next time", 3, null, 'info')
			updatesLog.getWeather = false
			updatesLog.alertsUpdated = false
			atomicState.updatesLog = updatesLog
			
			// Tell the children that we are once again connected to the Ecobee API Cloud
			if (apiConnected() != "full") {
				apiRestored()
				generateEventLocalParams() // Update the connection status
			}
			
			String names = resp.json.thermostatList?.name.toString().replaceAll("\\[","").replaceAll("\\]","")
			String tids = ""
			def numStats = 0
			tidList.each { numStats++; tids = (tids=="") ? it : tids+","+it }
			if (preText == '') {	// avoid calling debugLevel() again) {
				LOG("Polling thermostat${(numStats>1)?'s':''} ${names} (${tids}) completed", 2, null, 'info')
			} else {
				if (debugLevelFour) LOG("pollEcobeeAPICallback() - Updated thermostat${(numStats>1)?'s':''} ${names} (${tidList}) ${atomicState.thermostats}", 1, null, 'info')
			}
		}
		atomicState.inTimeoutRetry = 0	// Not in Timeout Recovery any longer
	} else if (resp && resp.hasError()) {
		result = false
		if (debugLevelFour) LOG("${preText}Poll returned http status ${resp.status}, ${resp.errorMessage}", 1, null, "error")
		def iStatus = resp.status ? resp.status?.toInteger() : null
		// Handle recoverable / retryable errors
		if (iStatus && (iStatus == 500)) {
			if (debugLevelFour) LOG("${preText}Poll returned (recoverable??) http status ${resp.status}, data ${resp.errorData}, message ${resp.errorMessage}", 2, null, "warn")
			// Ecobee server error
			def statusCode
			if (resp.errorData) {
				def errorJson = new JsonSlurper().parseText(resp.errorData)
				statusCode = errorJson?.status?.code
			}
			if (statusCode?.toInteger() == 14) {
				// Auth_token expired
				if (debugLevelThree) LOG("Polling: Auth_token expired", 3, null, "warn")
				atomicState.action = "pollChildren"
				if ( refreshAuthToken() ) { 
					// Note that refreshAuthToken will reschedule pollChildren if it succeeds in refreshing the token...
					LOG( 'Polling: Auth_token refreshed', 2, null, 'info')
				} else {
					LOG( 'Polling: Auth_token refresh failed', 1, null, 'error')
				}
				atomicState.inTimeoutRetry = 0
				return result
			} else { 
				// All other Ecobee Server (500) errors here
				LOG("Ecobee Server error ${resp.status} - ${resp.errorMessage}: ${resp.errordata}", 1, null, 'error')
				// Don't retry for now...may change in the future
				return result
			}
		} else if (((iStatus == null) && (resp.errorMessage?.contains('connection timed out'))) || 
					((iStatus > 400) && (iStatus < 405)) || (iStatus == 408) || ((iStatus > 500) && (iStatus < 505))) {
			// Retry on transient, recoverable error codes (timeouts, server temporarily unavailable, etc.) see https://en.wikipedia.org/wiki/List_of_HTTP_status_codes 
			LOG("pollEcobeeAPICallback() Polling error: ${iStatus}, ${resp.errorMessage} - Will Retry", 1, null, 'warn')	// Just log it, and hope for better next time...
			if (apiConnected != 'warn') {
				atomicState.connected = 'warn'
				updateMyLabel()
				atomicState.lastPoll = now()
				atomicState.lastPollDate = getTimestamp()
				generateEventLocalParams()
			}
			def inTimeoutRetry = atomicState.inTimeoutRetry
			if (inTimeoutRetry == null) inTimeoutRetry = 0
			if (inTimeoutRetry < 3) runIn(atomicState.reAttemptInterval, "pollChildren", [overwrite: true])
			atomicState.inTimeoutRetry = inTimeoutRetry + 1
			return result
		} else /* can we handle any other errors??? */ {
			LOG("Polling Error ${iStatus}: ${resp.errorMessage}, ${resp.errorJson?.status?.message}, ${resp.errorJson?.status?.code}", 1, null, "error")
		}
		// For now, the code here just logs the error and assumes that checkThermostatSummary() will recover from the unhandled errors on the next call.
		return result
	}
	
	// Now we have to actually send the updates
	if (result) {
		def polledMS = now()
		def prepTime = (polledMS-startMS)			// if prep takes too long, we will do the updates asynchronously too
		if (debugLevelThree) LOG("Prep complete (${prepTime}ms)",3,null,'trace')
		if (timers) log.debug "TIMER: poolEcobeeAPICallback complete @ ${polledMS - pollEcobeeAPIStart}ms"
		if (alertsUpdated) {
			if (prepTime > 11000) { runIn(2, 'generateAlertsAndEvents', [overwrite: true]) } else { generateAlertsAndEvents() }
		} else {
			if (prepTime > 11000) { runIn(2, 'generateTheEvents', [overwrite: true]) } else { generateTheEvents() }
		}
	} else {
		LOG('pollEcobeeAPICallback() Error',1,null,'warn')
	}
	if (debugLevelFour) LOG("<===== Leaving pollEcobeeAPICallback() results: ${result}", 1, null, 'trace')
	return result
}

void sendAlertEvents() {
	if (atomicState.alerts == null) return				// silently return until we get the alerts environment initialized
	boolean debugLevelFour = debugLevel(4)
	// log.debug "askAlexa: ${askAlexa}"
	if (!settings.askAlexa) {
		if (debugLevelFour) LOG("Ask Alexa support not configured, nothing to do, returning",4, null, 'trace')
		return							// Nothing to do (at least for now)
	}
	if (debugLevelFour) LOG("Entered sendAlertEvents() ${atomicState.alerts}", 1, null, 'trace')
   
	def askAlexaAlerts = atomicState.askAlexaAlerts			// list of alerts we have sent to Ask Alexa
	if (!askAlexaAlerts) askAlexaAlerts = [:]				// make it a map if it doesn't exist yet
	def alerts = atomicState.alerts							// the latest alerts from Ecobee (all thermostats, by tid)
	def totalAlexaAlerts = 0		// including any duplicates we send
	def totalNewAlerts = 0
	def removedAlerts = 0
	def updatesLog = atomicState.updatesLog
	def changedTids = updatesLog.changedTids
	def statTime = atomicState.statTime
	
	// Initialize some Ask Alexa items
	String queues = getMQListNames()
	def exHours = expire ? expire as int : 0
	def exSec=exHours * 3600
	
	// Walk through the list of thermostats
	changedTids.each { tid ->
	//atomicState.thermostatData.thermostatList.each { stat ->
		// String tid = stat.identifier
		String tstatName = getThermostatName(tid)
		
		if (alerts.containsKey(tid) && (alerts[tid] != [])) {	// we have alerts to process!
			// Avoid saying "Your Downstairs Thermostat thermostat" or even "Your Downstairs Ecobee Thermostat"
			String append = ' thermostat'
			String tName = tstatName.toLowerCase().trim()
			if (tName.endsWith('thermostat') || tName.endsWith('ecobee')) append = ''
			tstatName = tstatName + append
			// Process the alerts for this thermostat
			def newAlerts = []
			def newAlexaAlerts = [:]
			def allAlerts = []
			alerts[tid]?.each { alert ->
				if (alert.acknowledgeRef.toString().contains(tid)) {		// only process alerts that match this thermostat ID
					// reformat the alert type (header)
					String alertTypeMsg = alertTranslation( alert.alertType, alert.notificationType )

					// remove "Contact your service contractor..."
					def idx = alert.text.indexOf('Contact your s')
					
					// Build the components of the translated message 
					String messageID = alert.acknowledgeRef.toString()				// Use acknowledgeRef as the messageID - it is supposed to be unique
					
					def expireTimeMS = (exSec==0) ? 0 : new Date().parse('yyyy-MM-dd HH:mm:ss',"${alert.date} ${alert.time}").getTime() + (exSec * 1000)
					def tstatTimeMS = new Date().parse('yyyy-MM-dd HH:mm:ss',statTime[tid]).getTime()
					if ((tstatTimeMS - expireTimeMS) < 0) {	
						// hasn't expired yet, so send it/refresh it to Ask Alexa
						String dateTimeStr = fixDateTimeString( alert.date, alert.time, statTime[tid]) // gets us "today" or "yesterday"
						String translatedMessage = "Your ${tstatName} reported ${alertTypeMsg} ${dateTimeStr}: ${idx>0?alert.text.take(idx).trim():alert.text}"
						// Now send the message to Ask Alexa
						LOG("Sending '${messageID}' to Ask Alexa ${queues?.size()>0?queues:'Primary Message'} queue${listOfMQs?.size()>1?'s':''}: ${translatedMessage}",3,null,'info')
						sendLocationEvent(name: 'AskAlexaMsgQueue', value: 'Ecobee Status', unit: messageID, isStateChange: true, 
							descriptionText: translatedMessage, data: [ queues: listOfMQs, overwrite: true, expires: exSec, suppressTimeDate: true, trackDelete: true ])
						totalAlexaAlerts++			// total that we sent to Ask Alexa for all thermostats
						allAlerts << messageID		// all the alerts that we found for THIS thermostat
						// Create the list of new alerts we sent to Ask Alexa
						if (!askAlexaAlerts || !askAlexaAlerts.containsKey(tid) || !askAlexaAlerts[tid]?.contains(messageID)) {
							if (!newAlerts || !newAlerts.contains(messageID)) {
								newAlerts << messageID
							}
						}
					} else {
						// This messageID has expired, don't send it, delete it from Ask Alexa, remove it from outstanding Ask Alexa Alerts
						deleteAskAlexaAlert( 'Ecobee Status', messageID )
						if (askAlexaAlerts && askAlexaAlerts.containsKey(tid) && (askAlexaAlerts[tid] != [])) {
							if (askAlexaAlerts[tid]?.contains(messageID)) {
								askAlexaAlerts[tid].removeAll{ it == messageID }
								if (!askAlexaAlerts[tid]) askAlexaAlerts[tid] = []
								atomicState.askAlexaAlerts = askAlexaAlerts
							}
						}
					}
				}
			} // end of all the alerts for this thermostat
			// Collect all the new alerts that we found for this thermostat
			if (newAlerts != []) {
				totalNewAlerts += newAlerts.size()
				newAlexaAlerts[tid] = newAlerts
				askAlexaAlerts = askAlexaAlerts ? (askAlexaAlerts + newAlexaAlerts) : newAlexaAlerts
			}

			// Now, check if any of the saved alerts have been acknowledged (are no longer in thermostat.alerts)
			if (askAlexaAlerts && askAlexaAlerts.containsKey(tid) && (askAlexaAlerts[tid] != [])) {
				def closedAlerts = askAlexaAlerts[tid]
				if (allAlerts) {
					askAlexaAlerts[tid] = askAlexaAlerts[tid] + allAlerts
					closedAlerts = askAlexaAlerts[tid] - askAlexaAlerts[tid].intersect(allAlerts)
				}
				// def closedAlerts = allAlerts ? (askAlexaAlerts[tid] + allAlerts) - askAlexaAlerts[tid].intersect(allAlerts) : askAlexaAlerts[tid]
				// log.debug "Closed Alerts: ${closedAlerts}"
				if (closedAlerts) {
					closedAlerts.each { msgID ->
						deleteAskAlexaAlert( 'Ecobee Status', msgID.toString() )
						askAlexaAlerts[tid].removeAll{ it == msgID }
					}
					// Remove the deleted alerts from the list of outstanding alerts
					// askAlexaAlerts[tid] = askAlexaAlerts[tid] - closedAlerts
					removedAlerts += closedAlerts.size()
				}
			}
		} else {
			// There are no alerts for this tid... check if we need to delete any from Ask Alexa's queues
			if (debugLevelFour) LOG('No active Ecobee alerts',4,null,'trace')
			if (askAlexaAlerts && askAlexaAlerts.containsKey(tid) && (askAlexaAlerts[tid] != [])) {
				// we have some previously queued messages to delete
				LOG("Ecobee Alerts cleared for ${tstatName}, deleting outstanding notices from Ask Alexa",2,null,'info')
				askAlexaAlerts[tid].each { msgID ->
					deleteAskAlexaAlert ('Ecobee Status', msgID.toString())
				}
				removedAlerts += askAlexaAlerts[tid].size()
				askAlexaAlerts[tid] = []	// clear out the list
			}
		}
	} // end of { stat ->
	if (totalAlexaAlerts > 0) {
		def updated = totalAlexaAlerts - totalNewAlerts
		LOG("Sent ${totalNewAlerts} new ${updated?'and '+updated.toString()+' updated ':''}Alert${(totalNewAlerts+updated)>1?'s':''} to Ask Alexa",2,null,'info')
	}
	if (removedAlerts > 0) {
		LOG("Removed ${removedAlerts} Alert${removedAlerts>1?'s':''} from Ask Alexa",2,null,'info')
	}
//	  if ((askAlexaAlerts != [:]) || (newAlexaAlerts != [:])) askAlexaAlerts = askAlexaAlerts + newAlexaAlerts
	atomicState.askAlexaAlerts = askAlexaAlerts
}

String alertTranslation( alertType, notificationType ){
	switch (alertType) {
		case 'alert':
			switch(notificationType) {
				case 'hvac':
					return 'an H V A C Maintenance Reminder'
					break;
				case 'furnaceFilter':
					return 'a Furnace Air Filter Reminder'
					break;
				case 'humidifierFilter':
					return 'a Humidifier Filter Reminder'
					break;
				case 'dehumidifierFilter':
					return 'a Dehumidifier Filter Reminder'
					break;
				case 'ventilator':
					return 'a Ventilator Reminder'
					break;
				case 'ac':
					return 'an Air Conditioner Alert'
					break;
				case 'airFilter':
					return 'an Air Filter Reminder'
					break;
				case 'airCleaner':
					return 'an Air Cleaner Reminder'
					break;
				case 'uvLamp':
					return 'an Ultraviolet Lamp Reminder'
					break;
				case 'temp':
					return 'a Temperature Alert'
					break;
				case 'lowTemp':
					return 'a Low Temperature Alert'
					break;
				case 'highTemp':
					return 'a High temperature Alert'
					break;
				case 'lowHumidity':
					return 'a Low Humidity Alert'
					break;
				case 'highHumidity':
					return 'a High Humidity Alert'
					break;
				case 'auxHeat':
					return 'an Auxiliary Heat Run Time Alert'
					break;
				case 'auxOutdoor':
					return 'an Auxiliary Outdoor Temperature Alert'
					break;
				case 'sensor':
					return 'a Sensor Alert'
					break;
				case 'alert':
				default:
					return 'an Alert'
					break;
			}
			break;
		case 'demandResponse':
			return 'a demand response message'
			break;
		case 'emergency':
			return 'an emergency notice'
			break;
		case 'message':
			return 'a message for you'
			break;
		case 'pricing':
			return 'a pricing notice'
			break;
		default:
			return 'an alert'
			break;
	}
	return 'An Alert'
}

void updateSensorData() {
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("Entered updateSensorData() ${atomicState.remoteSensors}", 4)
	def sensorCollector = [:]
	def sNames = []
	def totalUpdates = 0
	Integer userPrecision = getTempDecimals()
	Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
	def updatesLog = atomicState.updatesLog
	boolean forcePoll = updatesLog.forcePoll
	boolean thermostatUpdated = updatesLog.thermostatUpdated
	def changedTids = updatesLog.changedTids
	String preText = getDebugLevel() <= 2 ? '' : 'updateSensorData() - '
	
	changedTids.each { tid ->
	//atomicState.thermostatData.thermostatList.each { singleStat ->
		// def tid = singleStat.identifier
		
		boolean isConnected = atomicState.runtime[tid]?.connected
	
		atomicState.remoteSensors[tid].each {
			String sensorDNI = ''
			switch (it?.type) {
				case 'ecobee3_remote_sensor':
					// Ecobee3 remote temp/occupancy sensor
					sensorDNI = "ecobee_suite-sensor-${it?.id}-${it?.code}"
					break;
				case 'thermostat':
					// The thermostat itself
					sensorDNI = "ecobee_suite-sensor_tstat-${it?.id}-${it?.name}"
					break;
				case 'control_sensor':
					// SmartSI style control sensor (temp or humidity)
					sensorDNI = "ecobee_suite-control_sensor-${tid}.${it?.id}"		// New DNI format includes Thermostat ID
					if (!settings.ecobeesensors?.contains(sensorDNI)) sensorDNI = "ecobee_suite-monitor_sensor-${it?.id}" // If not selected, try the old DNI format
					break;
				case 'monitor_sensor':
					// Smart style remote sensor (temp or humidity)
					sensorDNI = "ecobee_suite-monitor_sensor-${tid}.${it?.id}"
					break;
				default:
					LOG("Unknown sensor type reported: ${it}",2,null,'warn')
					break;
			} 
			if (debugLevelFour) LOG("updateSensorData() - Sensor ${it?.name}, sensorDNI == ${sensorDNI}", 4)
							  
			if ((sensorDNI != '') && settings.ecobeesensors?.contains(sensorDNI)) {		// only process the selected/configured sensors
				def temperature
				def occupancy
				Integer humidity = null
							
				it.capability.each { cap ->
					if (cap.type == 'temperature') {
						if (debugLevelFour) LOG("updateSensorData() - Sensor ${it.name} temp is ${cap.value}", 4)
						if ( cap.value.isNumber() ) { // Handles the case when the sensor is offline, which would return "unknown"
							temperature = roundIt((cap.value.toBigDecimal() / 10.0), apiPrecision)
							//temperature = roundIt((temperature / 10.0), precision)  //.round(precision) // wantMetric() ? (temperature / 10).toDouble().round(1) : (temperature / 10).toDouble().round(1)
						} else if (cap.value == 'unknown') {
							// TODO: Do something here to mark the sensor as offline?
							if (isConnected) {
								// No need to log this unless thermostat is actually connected to the Ecobee Cloud
								LOG("${preText}Sensor ${it.name} temperature is 'unknown'. Perhaps it is unreachable?", 1, null, 'warn')
							}
							// Need to mark as offline somehow
							temperature = 'unknown'	  
						} else {
							LOG("${preText}Sensor ${it.name} returned ${cap.value}.", 1, null, "error")
						}
					} else if (cap.type == 'occupancy') {
						if(cap.value == 'true') {
							occupancy = 'active'
						} else if (cap.value == 'unknown') {
							// Need to mark as offline somehow
							LOG("${preText}Sensor ${it.name} occupancy is 'unknown'", 2, null, 'warn')
							occupancy = 'unknown'
						} else {
							occupancy = 'inactive'
						}							 
					} else if (cap.type == 'humidity') {		// only thermostats have humidity
						if (cap.value?.isNumber()) humidity = cap.value.toInteger()
					}
				}
															
				def sensorData = [:]
				def lastList = atomicState."${sensorDNI}" as List
				def listSize = 5
				if ( !lastList || (lastList.size() < listSize)) {
					lastList = [999,'x',-1,'default','null']	// initilize the list 
					sensorData = [ thermostatId: tid ]		// this will never change, but we need to send it at least once
				}
					
				if (forcePoll) sensorData << [ thermostatId: tid /*, vents: 'default'*/ ]		// belt, meet suspenders
					
				// temperature usually changes every time, goes in *List[0]
				// Send temp with API precision	 (1 decimal F, 2 decimal C) - Sensor device will adjust for display
				def currentTemp = ((temperature == 'unknown') ? 'unknown' : myConvertTemperatureIfNeeded(temperature, 'F', apiPrecision))
				if (forcePoll || (lastList[0] != currentTemp)) { sensorData << [ temperature: currentTemp ] }
				def sensorList = [currentTemp]
					
				// occupancy generally only changes every 30 minutes, in *List[1]
				if ((occupancy != "") && (lastList[1] != occupancy)) { sensorData << [ motion: occupancy ] }
				sensorList += [occupancy]
					
				// precision can change at any time, *List[2]
				if (lastList[2] != userPrecision) { sensorData << [ decimalPrecision: userPrecision ] }
				sensorList += [userPrecision]
					
				// currentProgramName doesn't change all that often, but it does change
				def currentPrograms = atomicState.currentProgramName
				def currentProgramName = (currentPrograms && currentPrograms.containsKey(tid)) ? currentPrograms[tid] : 'default' // not set yet, we will have to get it later. 
				if (lastList[3] != currentProgramName) { sensorData << [ currentProgramName: currentProgramName ] }
				sensorList += [currentProgramName]
				
				// not every sensor has humidity
				if ((humidity != null) && (lastList[4] != humidity)) { sensorData << [ humidity: humidity ] }
				sensorList += [humidity]
				
				// collect the climates that this sensor is included in
				def sensorClimates = [:]
				def climatesList = []
				if (thermostatUpdated || forcePoll) {	// these will only change when the program object changes
					def statProgram = atomicState.program[tid]
					if (statProgram?.climates) {
						statProgram.climates.each { climate ->
							def sids = climate.sensors?.collect { sid -> sid.name}
							if (sids.contains(it.name)) {
								sensorClimates << ["${climate.name}":'on']
								climatesList += ['on']
							} else {
								sensorClimates << ["${climate.name}":'off']
								climatesList += ['off']
							}
						}
					}
					if (debugLevelFour) LOG("updateSensorData() - sensor ${it.name} is used in ${sensorClimates}", 1, null, 'trace')
				} 
				
				// check to see if the climates have changed. Notice that there can be 0 climates if the thermostat data wasn't updated this time;
				// note also that we will send the climate data every time the thermostat IS updated, even if they didn't change, becuase we don't 
				// store them in the atomicState unless the thermostat object changes...
				if ((lastList.size() < (listSize+climatesList.size())) || (lastList.drop(listSize) != climatesList)) {	
					sensorData << sensorClimates
					sensorList += climatesList
				}
				
				// For Health Check, update the sensor's checkInterval	any time we get forcePolled - which should happen a MINIMUM of once per hour
				// This because sensors don't necessarily update frequently, so this simple event should be enought to let the ST Health Checker know
				// we are still alive!
				if (forcePoll) { 
					sensorData << [checkInterval: 3900] // 5 minutes longer than an hour
				}
				
				if (sensorData != [:]) {
					def sls = sensorData.size()
					totalUpdates = totalUpdates + sls
					// log.debug "${it.name}: ${sensorData}"
					sensorCollector[sensorDNI] = [name:it.name,data:sensorData]
					atomicState."${sensorDNI}" = sensorList
					if (debugLevelFour) LOG("updateSensorData() - sensorCollector being updated with sensorData: ${sensorData}", 1, null, 'trace')
					sNames += it.name
				}
			} // End sensor is valid and selected in settings
		} // End [tid] sensors loop
	} // End thermostats loop
	
	atomicState.remoteSensorsData = sensorCollector
	if (preText=='') {
		def sz = sNames.size()
		if (sz > 1) sNames.sort()
		if (debugLevelFour) LOG("Sensor data: ${sensorCollector}", 1, null, trace)
		LOG("${totalUpdates} update${totalUpdates!=1?'s':''} for ${sz} sensor${sz!=1?'s':''}${sNames?' ('+sNames.toString()[1..-2]+')':''}",2, null, 'info')
	} else {
		if (debugLevelFour) LOG("updateSensorData() - Updated these remoteSensors: ${sensorCollector}", 1, null, 'trace') 
	}
	if (atomicState.timers) log.debug "TIMER: Sensors update completed @ ${now() - atomicState.pollEcobeeAPIStart}ms"
}

void updateThermostatData() {
	boolean timers = atomicState.timers
	def pollEcobeeAPIStart
	if (timers) {
		pollEcobeeAPIStart = atomicState.pollEcobeeAPIStart
		log.debug "TIMER: Entered updateThermostatData() @ ${now() - pollEcobeeAPIStart}ms"
	}
	boolean debugLevelFour = debugLevel(4)
	boolean debugLevelThree = debugLevel(3)
	def updatesLog = atomicState.updatesLog
	if (debugLevelFour) LOG("updateThermostatData() - updatesLog: ${updatesLog}", 1, null, 'trace')
	boolean thermostatUpdated = updatesLog.thermostatUpdated
	boolean runtimeUpdated = updatesLog.runtimeUpdated
	boolean rtReallyUpdated = updatesLog.rtReallyUpdated
	boolean alertsUpdated = updatesLog.alertsUpdated
	boolean settingsUpdated = updatesLog.settingsUpdated
	// boolean alertsUpdated = updatesLog.alertsUpdated
	boolean programUpdated = updatesLog.programUpdated
	boolean eventsUpdated = updatesLog.eventsUpdated
	//boolean locationUpdated = atomicState.locationUpdated	// this never gets sent anyway
	boolean extendRTUpdated = updatesLog.extendRTUpdated
	boolean weatherUpdated = updatesLog.weatherUpdated
	boolean sensorsUpdated = updatesLog.sensorsUpdated
	boolean equipUpdated = updatesLog.equipUpdated
	boolean forcePoll = updatesLog.forcePoll
	def changedTids = updatesLog.changedTids
	
	boolean usingMetric = wantMetric() // cache the value to save the function calls
	if (forcePoll) {
		if (atomicState.timeZone == null) getTimeZone() // both will update atomicState with valid timezone/zipcode if available
		if (atomicState.zipCode == null) getZipCode()
	}
	Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
	Integer userPrecision = getTempDecimals()						// user's requested display precision
	String preText = getDebugLevel() <= 2 ? '' : 'updateThermostatData() - '
	def tstatNames = []
	def askAlexaAppAlerts = atomicState.askAlexaAppAlerts?: [:]
	// if (askAlexaAppAlerts == null) askAlexaAppAlerts = [:]

	int totalUpdates = 0
	def statTime = atomicState.statTime
	if (timers) log.debug "TIMER: Initialized updateThermostatData() @ ${now() - pollEcobeeAPIStart}ms"
	
	def statData = changedTids.inject([:]) { collector, tid ->
	// atomicState.thermostats = atomicState.thermostatData.thermostatList.inject([:]) { collector, stat ->
		// we use atomicState.thermostatData because it holds the latest Ecobee API response, from which we can determine which stats actually
		// had updated data. Thus the following work is done ONLY for tstats that have updated data
		// String tid = stat.identifier
		// if (atomicState.askAlexaAppAlerts?.containsKey(tid) && (atomicState.askAlexaAppAlerts[tid] == true)) atomicState.askAlexaAppAlerts[tid] = []
		String DNI = (atomicState.isHE?'ecobee_suite-thermostat-':'') + ([ app.id, tid ].join('.'))
		String tstatName = getThermostatName(tid)
		
		if (debugLevelThree) LOG("updateThermostatData() - Updating event data for thermostat ${tstatName} (${tid})", 3, null, 'info')
		if (timers) log.debug "TIMER: Updating event data for ${tstatName} ${tid} @ ${now() - pollEcobeeAPIStart}ms"

	// grab a local copy from the atomic storage all at once (avoid repetive reads from backing store)
		def statSettings = atomicState.settings ? atomicState.settings[tid] : [:]
		def program = atomicState.program ? atomicState.program[tid] : [:]
		def events = atomicState.events ? atomicState.events[tid] : [:]
		def runtime = atomicState.runtime ? atomicState.runtime[tid] : [:]
		def statLocation = atomicState.statLocation ? atomicState.statLocation[tid] : [:]
		// def oemCfg = atomicState.oemCfg ? atomicState.oemCfg[tid] : [:]
		def extendedRuntime = atomicState.extendedRuntime ? atomicState.extendedRuntime[tid] : [:]
		
	// Handle things that only change when runtime object is updated)
		def occupancy = 'not supported'
		def tempTemperature
		def tempHeatingSetpoint
		def tempCoolingSetpoint
		def tempWeatherTemperature
		def tempWeatherDewpoint
		def tempWeatherHumidity
		def tempWeatherPressure

		if ( rtReallyUpdated || sensorsUpdated || forcePoll) {
			// Occupancy (motion)
			// TODO: Put a wrapper here based on the thermostat brand
			def thermSensor = ''
			if (atomicState.remoteSensors) { thermSensor = atomicState.remoteSensors[tid].find { it.type == 'thermostat' } }
			
			boolean hasInternalSensors
			if(!thermSensor) {
				if (debugLevelFour) LOG('updateThermostatData() - Thermostat ${tstatName} does not have a built in remote sensor', 1, null, 'warn')
				hasInternalSensors = false
			} else {
				hasInternalSensors = true
				if (debugLevelFour) LOG("updateThermostatData() - thermSensor == ${thermSensor}", 1, null, 'trace' )
		
				def occupancyCap = thermSensor?.capability.find { it.type == 'occupancy' }
				if (occupancyCap) {
					if (debugLevelFour) LOG("updateThermostatData() - occupancyCap = ${occupancyCap} value = ${occupancyCap.value}", 1, null, 'info')
		
					// Check to see if there is even a value, not all types have a sensor
					occupancy = occupancyCap.value ? occupancyCap.value : 'not supported'
				}
			}
			// 'not supported' is reported as 'inactive'
			if (hasInternalSensors) { occupancy = (occupancy == 'true') ? 'active' : ((occupancy == 'unknown') ? 'unknown' : 'inactive') }			  
			
			// Temperatures
			// NOTE: The thermostat always present Fahrenheit temps with 1 digit of decimal precision. We want to maintain that precision for inside temperature so that apps like vents and Smart Circulation
			//		 can operate efficiently. So, while the user can specify a display precision of 0, 1 or 2 decimal digits, we ALWAYS keep and send max decimal digits and let the device handler adjust for display
			//		 For Fahrenheit, we keep the 1 decimal digit the API provides, for Celsius we allow for 2 decimal digits as a result of the mathematical calculation	   
			if (runtime?.actualTemperature != null) tempTemperature = myConvertTemperatureIfNeeded((runtime.actualTemperature/ 10.0), 'F', apiPrecision)
			if (runtime?.desiredHeat != null)	tempHeatingSetpoint = myConvertTemperatureIfNeeded((runtime.desiredHeat / 10.0), 'F', apiPrecision)
			if (runtime?.desiredCool != null)	tempCoolingSetpoint = myConvertTemperatureIfNeeded((runtime.desiredCool / 10.0), 'F', apiPrecision)
			if (timers) log.debug "TIMER: Finished updating Sensors for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}
		
		if (weatherUpdated) {
			//if (atomicState.weather && atomicState.weather?.containsKey(tid) && atomicState.weather[tid].containsKey('temperature')) {
			// log.debug atomicState.weather[tid]
			def weather = atomicState.weather ? atomicState.weather[tid] : [:]	
			// log.debug weather
			if (weather?.temperature != null) {
				tempWeatherTemperature = myConvertTemperatureIfNeeded((weather.temperature.toBigDecimal() / 10.0), "F", apiPrecision)
			} else {tempWeatherTemperature = 451.0} // will happen only once, when weather object changes to shortWeather
			if (weather?.dewpoint != null) tempWeatherDewpoint = myConvertTemperatureIfNeeded((weather.dewpoint.toBigDecimal() / 10.0), "F", apiPrecision)
			if (weather?.humidity != null) tempWeatherHumidity = weather.humidity
			if (weather?.pressure != null) tempWeatherPressure = usingMetric ? weather.pressure : roundIt((weather.pressure.toBigDecimal() * 0.02953),2) //milliBars to inHg
			if (timers) log.debug "TIMER: Finished updating Weather for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}
		
	// handle[tid] things that only change when the thermostat object is updated (actually, just settingsUpdated)
		def auxHeatMode
		def heatHigh
		def heatLow
		def coolHigh
		def coolLow
		def heatRange
		def coolRange
		def tempHeatDiff = 0.0
		def tempCoolDiff = 0.0
		def tempHeatCoolMinDelta = 1.0

		if (settingsUpdated || forcePoll) {
			auxHeatMode =  (statSettings?.hasHeatPump) && (statSettings?.hasForcedAir || statSettings?.hasElectric || statSettings?.hasBoiler) // 'auxHeat1' == 'emergency' if using a heatPump
			tempHeatDiff = myConvertTemperatureIfNeeded(statSettings.stage1HeatingDifferentialTemp / 10.0, "F", apiPrecision)
			tempCoolDiff = myConvertTemperatureIfNeeded(statSettings.stage1CoolingDifferentialTemp / 10.0, "F", apiPrecision) 
			tempHeatCoolMinDelta = myConvertTemperatureIfNeeded(statSettings.heatCoolMinDelta / 10.0 , 'F', apiPrecision)
			
			// RANGES
			// UI works better with the same ranges for both heat and cool...
			// but the device handler isn't using these values for the UI right now (can't dynamically define the range)			
			heatHigh = myConvertTemperatureIfNeeded((statSettings.heatRangeHigh / 10.0), 'F', apiPrecision)
			heatLow =  myConvertTemperatureIfNeeded((statSettings.heatRangeLow	/ 10.0), 'F', apiPrecision)
			coolHigh = myConvertTemperatureIfNeeded((statSettings.coolRangeHigh / 10.0), 'F', apiPrecision)
			coolLow =  myConvertTemperatureIfNeeded((statSettings.coolRangeLow	/ 10.0), 'F', apiPrecision)
			// calculate these anyway (for now) - it's easier to read the range while debugging
			heatRange = (heatLow && heatHigh) ? "(${roundIt(heatLow,0)}..${roundIt(heatHigh,0)})" : (usingMetric ? '(5..35)' : '(45..95)')
			coolRange = (coolLow && coolHigh) ? "(${roundIt(coolLow,0)}..${roundIt(coolHigh,0)})" : (usingMetric ? '(5..35)' : '(45..95)')
			if (timers) log.debug "TIMER: Finished updating temperature Limits for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}
 
	// handle things that depend on both thermostat and runtime objects
		// EVENTS
		// Determine if an Event is running, find the first running event (only changes when thermostat object is updated)
		def runningEvent = [:]
		String currentClimateName = 'null'
		String currentClimateId = 'null'
		String currentClimate = 'null'
		String currentClimateOwner = 'null'
		String currentClimateType = 'null'
		String currentFanMode = 'null'
		def currentVentMode = statSettings.vent
		def ventMinOnTime = statSettings.ventilatorMinOnTime
		def climatesList = []
		def statMode = statSettings.hvacMode
		def fanMinOnTime = statSettings.fanMinOnTime
		
		// what program is supposed to be running now?
		String scheduledClimateId = 'unknown'
		String scheduledClimateName = 'Unknown'
		def schedClimateRef = null
		String scheduledClimateOwner = 'null'
		String scheduledClimateType = 'null'
		
		if (program != [:]) {
			scheduledClimateId = program.currentClimateRef 
			schedClimateRef = program.climates.find { it.climateRef == scheduledClimateId }
			scheduledClimateName = schedClimateRef.name
			scheduledClimateOwner = schedClimateRef.owner
			scheduledClimateType = schedClimateRef.type
			program.climates?.each { climatesList += '"'+it.name+'"' } // read with: programsList = new JsonSlurper().parseText(theThermostat.currentValue('programsList'))
			climatesList.sort()
		}
		if (debugLevelFour) LOG("${tstatName} -> scheduledClimateId: ${scheduledClimateId}, scheduledClimateName: ${scheduledClimateName}, scheduledClimateOwner: ${scheduledClimateOwner}, " +
					  "scheduledClimateType: ${scheduledClimateType}, climatesList: ${climatesList.toString()}", 1, null, 'info')
		
		// check which program is actually running now
		// log.debug "events: ${events}"
		def vacationTemplate = atomicState.vacationTemplate
		if (events?.size()) {
			events.each {
				if (it.running == true) {
					if (runningEvent == [:]) runningEvent = it						// We want the FIRST one...
				} else if ( it.type == 'template' ) {	// templates never run...
					vacationTemplate = true
                    atomicState.vacationTemplate = true
				}
			}
		}

		// hack to fix ecobee bug where template is not created until first Vacation is created, which screws up resumeProgram() (last hold event is not deleted)
		if (!vacationTemplate) {
			LOG("No vacation template exists for thermostat ${tstatName} (${tid}), creating one...", 2, null, 'warn')
			def r = createVacationTemplate(getChildDevice(DNI), tid.toString())		
			if (debugLevelThree) LOG("createVacationTemplate() returned ${r}", 3, null, 'trace')
		}
		
		// store the currently running event (in case we need to modify or delete it later, as in Vacation handling)
		def tempRunningEvent = [:]
		tempRunningEvent[tid] = runningEvent
		// log.debug "runningEvent: ${runningEvent}"

		if (!atomicState.runningEvent || (!atomicState.runningEvent[tid] && (runningEvent != [:])) || (atomicState.runningEvent[tid] != tempRunningEvent[tid])) {
			// Update atomicState only if the data changed (avoid hundreds of unnecessary writes to atomicStatae when events are running)
			tempRunningEvent = atomicState.runningEvent + tempRunningEvent
			atomicState.runningEvent = tempRunningEvent
		}

		String thermostatHold = 'null'
		String holdEndsAt = 'null'
		if (timers) log.debug "TIMER: Finished updating currentClimate for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		def isConnected = runtime?.connected
		if (!isConnected) {
			LOG("${tstatName} is not connected!",1,null,'warn')
			// Ecobee Cloud lost connection with the thermostat - device, wifi, power or network outage
			currentClimateName = 'Offline'
			occupancy = 'unknown'
			
			// Oddly, the runtime.lastModified is returned in UTC, so we have to convert it to the time zone of the thermostat
			// (Note that each thermostat could actually be in a different time zone)
			def myTimeZone = statLocation?.timeZone ? TimeZone.getTimeZone(statLocation.timeZone) : (location.timeZone?: TimeZone.getTimeZone('UTC'))
			def disconnectedMS = new Date().parse('yyyy-MM-dd HH:mm:ss',runtime?.disconnectDateTime)?.getTime()
			String disconnectedAt = new Date().parse('yyyy-MM-dd HH:mm:ss',runtime?.disconnectDateTime)?.format('yyyy-MM-dd HH:mm:ss', myTimeZone)
			// In this case, holdEndsAt is actually the date & time of the last valid update from the thermostat...
			holdEndsAt = fixDateTimeString( disconnectedAt.take(10), disconnectedAt.drop(11), statTime[tid] )
			if (forcePoll && settings.askAlexa) {
				// Let's send an Ask Alexa Alert for this as well - (Only on hourly/forcepolls, though, to minimize update overhead)
				String messageID = tid.toString()+'*disconnected'
				def exHours = expire ? expire as int : 0
				def exSec=exHours * 3600
				// If expiration is set, then stop sending alerts to Ask Alexa after disconnectTime + exHours
				def expireTimeMS = (exSec==0) ? 0 : disconnectedMS + (exSec * 1000)
				def currentTimeMS = now()
				if ((currentTimeMS - expireTimeMS) < 0) {	
					// Not time to expire the message, so send it	
					// Avoid saying "Your Downstairs Thermostat thermostat" or even "Your Downstairs Ecobee Thermostat"
					String append = ' thermostat'
					String tName = tstatName.toLowerCase().trim()
					if (tName.endsWith('thermostat') || tName.endsWith('ecobee')) append = ''
					tName = tstatName + append
					
					String translatedMessage = "Your ${tName} reported a Connectivity Alert: Ecobee Cloud connection lost ${holdEndsAt}. Please check HVAC power, wifi, and network connection."
					if (debugLevelFour) LOG("Sending ${translatedMessage} to Ask Alexa",4,null,'trace')
					sendLocationEvent(name: "AskAlexaMsgQueue", value: "Ecobee Status", unit: messageID, isStateChange: true, 
						descriptionText: translatedMessage, data: [ queues: listOfMQs, overwrite: true, expires: exSec, suppressTimeDate: true, trackDelete: true ])
					def newAlexaAppAlerts = [:]
					newAlexaAppAlerts[tid] = [messageID]
					if (askAlexaAppAlerts != [:]) newAlexaAppAlerts = askAlexaAppAlerts + newAlexaAppAlerts
					atomicState.askAlexaAppAlerts = newAlexaAppAlerts
					askAlexaAppAlerts = newAlexaAppAlerts
				} else {
					// It is past the expiry time, delete the message from the Ask Alexa queue(s)
					if (askAlexaAppAlerts && askAlexaAppAlerts[tid] && (askAlexaAppAlerts[tid] != [])) { 
						if (askAlexaAppAlerts[tid]?.toString().contains(messageID)) {
							deleteAskAlexaAlert('Ecobee Status', messageID)
							askAlexaAppAlerts[tid].removeAll{ it == messageID }
							if (!askAlexaAppAlerts[tid]) askAlexaAppAlerts[tid] = []
							atomicState.askAlexaAppAlerts = askAlexaAppAlerts
						}
					}
				}
			}
		} else if (settings.askAlexa) {
			// We are connected, and Ask Alexa is enabled - check if we need to delete a prior Ask Alexa Alert
			if (askAlexaAppAlerts && askAlexaAppAlerts[tid] && (askAlexaAppAlerts[tid] != [])) { 
				String msgID = tid.toString()+'*disconnected'
				if (askAlexaAppAlerts[tid]?.toString().contains(msgID)) {
					deleteAskAlexaAlert('Ecobee Status', msgID)
					askAlexaAppAlerts[tid].removeAll{ it == msgID }
					if (!askAlexaAppAlerts[tid]) askAlexaAppAlerts[tid] = []
					atomicState.askAlexaAppAlerts = askAlexaAppAlerts
				}
			}
		}
		if (timers) log.debug "TIMER: Finished updating Alerts for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		
		if (runningEvent && isConnected) {
			if (debugLevelFour) LOG("Found a running Event: ${runningEvent}", 1, null, 'trace') 
			holdEndsAt = fixDateTimeString( runningEvent.endDate, runningEvent.endTime, statTime[tid])
			thermostatHold = runningEvent.type
			String tempClimateRef = runningEvent.holdClimateRef ? runningEvent.holdClimateRef : ''
			def currentClimateRef = (tempClimateRef != '') ? program.climates.find { it.climateRef == tempClimateRef } : 'null'
			// log.debug "runningEvent.type: ${runningEvent.type}"
			switch (runningEvent.type) {
				case 'hold':
					if (tempClimateRef != '') {
						currentClimate = currentClimateRef.name
						currentClimateName = 'Hold: ' + currentClimate
						currentClimateOwner = currentClimateRef?.owner
						currentClimateType = currentClimateRef?.type
					} else if ((runningEvent.name=='auto')||(runningEvent.name=='hold')) {		// Handle the "auto" climates (includes fan on override, and maybe the Smart Recovery?)
						if ((runningEvent.isTemperatureAbsolute == false) && (runningEvent.isTemperatureRelative == false)) {
							currentClimateName = 'Hold: Fan'
						} else {
							currentClimateName = 'Hold: Temp'
						}
						currentClimate = runningEvent.name.capitalize()
					}
					break;
				case 'vacation':
					currentClimate = 'Vacation'
					currentClimateName = 'Vacation'
					currentClimateType = 'calendarEvent'	   // At least that is what it SHOULD be
					currentClimateOwner = 'user'				//
					fanMinOnTime = runningEvent.fanMinOnTime
					break;
				case 'quickSave':
					currentClimateName = 'Quick Save'
					currentClimateOwner = currentClimateRef?.owner
					currentClimateType = currentClimateRef?.type
					break;
				case 'autoAway':
					currentClimateName = 'Auto Away'
					currentClimate = 'Away'
					currentClimateOwner = currentClimateRef?.owner
					currentClimateType = currentClimateRef?.type
					break;
				case 'autoHome':
					currentClimateName = 'Auto Home'
					currentClimate = 'Home'
					currentClimateOwner = currentClimateRef?.owner
					currentClimateType = currentClimateRef?.type
					break;
				case 'demandResponse':
				if ((runningEvent.isTemperatureAbsolute == false) && (runningEvent.isTemperatureRelative == false))
					// Oddly, the *RelativeTemp values are always POSITIVE to reduce the load, and NEGATIVE to increase it (ie., pre-cool/pre-heat)
					// Per: https://getsatisfaction.com/api/topics/dr-event-with-negative-relative-heat-temp-increases-the-setpoint-instead-of-decreasing-it 
					currentClimateName = ((runningEvent.coolRelativeTemp < 0) || (runningEvent.heatRelativeTemp < 0)) ? 'Hold: Eco Prep' : 'Hold: Eco'
					currentClimateOwner = 'demandResponse'
					currentClimate = ((runningEvent.isOptional != null) && ((runningEvent.isOptional == true) || (runningEvent.isOptional == 'true'))) ? 'Eco' : 'Eco!'		// Tag mandatory DR events
					currentClimateId = runningEvent.name as String
					currentClimateType = 'program'
					break;
				default:				
					currentClimateName = (runningEvent.type != null) ? runningEvent.type : 'null'
					break;
			}
			if (!currentClimateId) currentClimateId = tempClimateRef ?: 'null'
		} else if (isConnected) {
			if (scheduledClimateId) {
				currentClimateId = scheduledClimateId
				currentClimateName = scheduledClimateName
				currentClimate = scheduledClimateName
				currentClimateOwner = scheduledClimateOwner
				currentClimateType = scheduledClimateType
			} else {
				LOG(preText+'Program or running Event missing', 1, null, 'warn')
				// These were already initialized above
				//currentClimateName = 'null'
				//currentClimateId = 'null' 
				//currentClimate = 'null'
			}
		}
		if (debugLevelFour) LOG("updateThermostatData(${tstatName} ${tid}) - currentClimate: ${currentClimate}, currentClimateName: ${currentClimateName}, currentClimateId: ${currentClimateId}, " +
								"currentClimateOwner: ${currentClimateOwner}, currentClimateType: ${currentClimateType}", 1, null, 'trace')

		// Note that fanMode == 'auto' might be changedby the Thermostat DTH to 'off' or 'circulate' dependent on  HVACmode and fanMinRunTime
		if (runningEvent) {
			currentFanMode = runningEvent.fan
			currentVentMode = runningEvent.vent
			ventMinOnTime = runningEvent.ventilatorMinOnTime
		} else {
			currentFanMode = runtime.desiredFanMode
		}
		if (timers) log.debug "TIMER: Finished updating Hold & climates for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		
		
		// if (equipUpdated || rtReallyUpdated || settingsUpdated || extendRTUpdated ) {
		// HUMIDITY
		def humiditySetpoint = runtime.desiredHumidity
		def humidifierMode = statSettings?.humidifierMode
		def dehumiditySetpoint = runtime.desiredDehumidity
		def dehumidifierMode = statSettings?.dehumidifierMode
		
		def dehumidifyOvercoolOffset = statSettings?.dehumidifyOvercoolOffset
		def hasHumidifier = statSettings?.hasHumidifier
		def hasDehumidifier = statSettings?.hasDehumidifier || (statSettings?.dehumidifyWithAC && (statSettings?.dehumidifyOvercoolOffset != 0)) // fortunately, we can hide the details from the device handler
		if (hasHumidifier && (extendedRuntime && extendedRuntime.desiredHumidity)) { // && extendedRuntime.desiredHumidity[2])) {
			humiditySetpoint = extendedRuntime.desiredHumidity		// [2]		// if supplied, extendedRuntime gives the actual target (Frost Control)
		}
			if (hasDehumidifier && (extendedRuntime && extendedRuntime.desiredDehumidity)) { // && extendedRuntime.desiredDehumidity[2])) {
			dehumiditySetpoint = extendedRuntime.desiredDehumidity	// [2]	
		}
		def humiditySetpointDisplay = humiditySetpoint
		switch (statMode) {
			case 'heat':
				if (hasHumidifier) humiditySetpointDisplay = humiditySetpoint
				break;
			case 'cool':
				if (hasDehumidifier) humiditySetpointDisplay = dehumiditySetpoint
				break;
			case 'auto':
				if (hasHumidifier && hasDehumidifier) { humiditySetpointDisplay = "${humiditySetpoint}-${dehumiditySetpoint}" }
				else if (hasHumidifier) { humiditySetpointDisplay = humiditySetpoint }
				else if (hasDehumidifier) { humiditySetpointDisplay = dehumiditySetpoint }
				break;
		}
		
		// EQUIPMENT STATUS
		def es = atomicState.equipmentStatus
		String equipStatus = (es && es[tid]) ? es[tid] : ''
		if (equipStatus == '') equipStatus = 'idle'
 
		def heatStages = statSettings.heatStages
		def coolStages = statSettings.coolStages 
		String equipOpStat // = 'idle'	// assume we're idle - this gets fixed below if different
		String thermOpStat // = 'idle'
		Boolean isHeating = false
		Boolean isCooling = false
		Boolean smartRecovery = false
		Boolean overCool = false
		Boolean dehumidifying = false
		Boolean ecoPreCool = false

		// Let's deduce if we are in Smart Recovery mode
		if (equipStatus.contains('ea')) {
			isHeating = true
			if (!forcePoll && !rtReallyUpdated) {
				// these weren't calculated above, so we do them here
				if (runtime?.actualTemperature != null) tempTemperature = myConvertTemperatureIfNeeded( (runtime.actualTemperature / 10.0), "F", apiPrecision)
				if (runtime?.desiredHeat != null) tempHeatingSetpoint = myConvertTemperatureIfNeeded( (runtime.desiredHeat / 10.0), 'F', apiPrecision)
			}
			if ((tempTemperature != null) && (tempHeatingSetpoint != null) && (statSettings?.disablePreHeating == false) && (tempTemperature > (tempHeatingSetpoint /* + 0.1 */))) {
				smartRecovery = true
				equipUpdated = true
				equipStatus = equipStatus + ',smartRecovery'
				LOG("${tstatName} is in Smart Recovery (${tid}), temp: ${tempTemperature}, setpoint: ${tempHeatingSetpoint}",3,null,'info')
			}
		} else if (equipStatus.contains('oo')) {
			isCooling = true
			if (!forcePoll && !rtReallyUpdated) {
				// these weren't calculated above, so we do them here
				if (runtime?.actualTemperature != null) tempTemperature = myConvertTemperatureIfNeeded( (runtime.actualTemperature / 10.0), "F", apiPrecision)
				if (runtime?.desiredCool != null) tempCoolingSetpoint = myConvertTemperatureIfNeeded( (runtime.desiredCool / 10.0), 'F', apiPrecision)
			}
			// Check if humidity > humidity setPoint, and tempTemperature > (coolingSetpoint - 0.1)
			if (hasDehumidifier) {
				if (runtime?.actualHumidity > dehumiditySetpoint) {
					if ((tempTemperature != null) && (tempCoolingSetpoint != null) && (tempTemperature < tempCoolingSetpoint) && (tempTemperature >= (tempCoolingSetpoint - (statSettings?.dehumidifyOvercoolOffset?.toBigDecimal() / 10.0)))) {
						overCool = true
						equipUpdated = true
						LOG("${tstatName} is Over Cooling (${tid}), temp: ${tempTemperature}, setpoint: ${tempCoolingSetpoint}",3,null,'info')
					} else {
						dehumidifying = true
					}
				}			
			}
			if (!overCool && (tempTemperature != null) && (tempCoolingSetpoint != null) && ((statSettings?.disablePreCooling == false) && (tempTemperature < (tempCoolingSetpoint /* - 0.1 */)))) {
				smartRecovery = true
				equipUpdated = true
				equipStatus = equipStatus + ',smartRecovery'
				LOG("${tstatName} is in Smart Recovery (${tid}), temp: ${tempTemperature}, setpoint: ${tempCoolingSetpoint}",3,null,'info')
			}
		}
		if (equipUpdated || forcePoll) {
			equipOpStat = 'idle'
			thermOpStat = 'idle'
			if (equipStatus == 'fan') {
				equipOpStat = 'fan only'
				thermOpStat = 'fan only'
			} else if (equipStatus == 'off') {
				thermOpStat = 'idle'
				equipOpStat = 'off'
			} else if (isHeating) {					// heating
				thermOpStat = smartRecovery ? 'heating (smart recovery)' : 'heating'
				equipOpStat = 'heating'
				//if (smartRecovery) thermOpStat = 'heating (smart recovery)'
				// Figure out which stage we are running now
				if		(equipStatus.contains('t1'))	{ equipOpStat = ((auxHeatMode) ? 'emergency' : ((heatStages > 1) ? 'heat 1' : 'heating')) }
				else if (equipStatus.contains('mpHo'))	{ equipOpStat = 'compHotWater'; thermOpStat = 'heating (hot water)'; }
				else if (equipStatus.contains('uxHo'))	{ equipOpStat = 'auxHotWater'; thermOpStat = 'heating (hot water)'; }
				else if (equipStatus.contains('t2'))	{ equipOpStat = 'heat 2' }
				else if (equipStatus.contains('t3'))	{ equipOpStat = 'heat 3' }
				else if (equipStatus.contains('p2'))	{ equipOpStat = 'heat pump 2' }
				else if (equipStatus.contains('p3'))	{ equipOpStat = 'heat pump 3' }
				else if (equipStatus.contains('mp'))	{ equipOpStat = 'heat pump' }
				// We now have icons that depict when we are humidifying or dehumidifying...
				if (equipStatus.contains('hu'))			{ equipOpStat += ' hum' }	// humidifying if heat
				// need to check if dehumidifying with heat (recent new option)
				
			} else if (isCooling) {				// cooling
				thermOpStat = smartRecovery ? 'cooling (smart recovery)' : (overCool ? 'cooling (overcool)' : 'cooling')
				equipOpStat = 'cooling'
				//if (smartRecovery) thermOpStat = 'cooling (smart recovery)'
				//else if (overCool) thermOpStat = 'cooling (overcool)'
				if		(equipStatus.contains('l1')) { equipOpStat = (coolStages == 1) ? 'cooling' : 'cool 1' }
				else if (equipStatus.contains('l2')) { equipOpStat = 'cool 2' }
				
				if (equipStatus.contains('de') || overCool || dehumidifying) { equipOpStat += ' deh' }	// dehumidifying if cool
				// need to check if vent or eco are on with cooling
				
			} else if (equipStatus.contains('de')) { // These can also run independent of heat/cool
				equipOpStat = 'dehumidifier' 
				thermOpStat = equipStats.contains('fan') ? 'fan only (dehumidifier)' : 'idle (dehumidifier)'
				// SHOULD thermOpStat BE FAN ONLY????
			} else if (equipStatus.contains('hu')) { 
				equipOpStat = 'humidifier' 
				thermOpStat = equipStatus.contains('fan') ? 'fan only (humidifier)' : 'idle (humidifier)'
			} else if (equipStatus.contains('econ')) {
				equipOpStat = 'economizer'
				thermOpStat = equipStatus.contains('fan') ? 'fan only (economizer)' : 'idle (economizer)'
			} else if (equipStatus.contains('vent')) {
				equipOpStat = 'ventilator'
				thermOpStat = equipStatus.contains('fan') ? 'fan only (ventilator)' : 'idle (ventilator)'
			} else if (equipStatus.contains('Hot')) {
				if (equipStatus.contains('comp')) {
					equipOpStat = 'compHotWater'
					thermOpStat = equipStatus.contains('fan') ? 'fan only (hot water)' : 'idle (hot water)'
				} else if (equipStatus.contains('aux')) {
					equipOpStat = 'auxHotWater'
					thermOpStat = equipStatus.contains('fan') ? 'fan only (hot water)' : 'idle (hot water)'
				}
			}
		}
		
		// Update the API link state and the lastPoll data. If we aren't running at a high debugLevel >= 4, then supply simple
		// poll status instead of the date/time (this simplifies the UI presentation, and reduces the chatter in the devices'
		// message log
		def apiConnection = apiConnected()
		def lastPoll = (debugLevelFour) ? "${apiConnection} @ ${atomicState.lastPollDate}" : (apiConnection=='full') ? 'Succeeded' : (apiConnection=='warn') ? 'Timed Out' : 'Failed'
		
		def statusMsg = 'null'
		if (isConnected) {
			// statusMsg = ((holdEndsAt == '') || (holdEndsAt == 'null')) ? 'null' : ((thermostatHold=='hold') ? 'Hold' : ((thermostatHold=='vacation') ? 'Vacation' : (thermostatHold=='demandResponse'?'Event')+" ends ${holdEndsAt}"
			if ((holdEndsAt == '') || (holdEndsAt == 'null')) {
				statusMsg = 'null'
			} else {
				def statusStart
				switch (thermostatHold) {
					case 'hold':
						statusStart = 'Hold'
						break;
					case 'vacation':
						statusStart = 'Vacation'
						break;
					case 'demandResponse':
						statusStart = "Energy Savings Event\n"
						break;
					default:
						statusStart = 'Event'
				}
				statusMsg = statusStart + " ends ${holdEndsAt}"
			}
		} else {
			statusMsg = "Thermostat Offline?\nLast updated ${holdEndsAt}"
			equipOpStat = 'offline'					// override if Ecobee Cloud has lost connection with the thermostat
			thermOpStat = 'offline'
		}
		if (timers) log.debug "TIMER: Finished updating Equipment & Connection status for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		
		def data = [:]
		if (forcePoll) data += [forced: true]				// Tell the DTH to force-update all attributes, states and tiles
		def dataStart = data
 
		// As of 1.7.10, forcePoll only forces updates to: heating/coolingSetpoint, currentClimate*, temperature, and humidity.
		// Everything else is scanned and only sent if it changed. This was a catch-all for when Updated flags weren't being
		// checked properly, but as of 1.7.10 I'm pretty sure we got everything so there really shouldn't be a need to EVER to a force-poll
		// NOTE: to force a total update, simply set all the atomicState.xxxChanged Maps to [:]
		
		// Equipment operating status - need to send first so that temperatureDisplay is properly updated after API connection loss/recovery
		if (thermostatUpdated || equipUpdated || forcePoll || (atomicState.wasConnected != isConnected)) { 
			def changeEquip = atomicState.changeEquip ? atomicState.changeEquip : [:]
			atomicState.wasConnected = isConnected
			boolean eqpChanged = false
			if ((changeEquip == [:]) || !changeEquip.containsKey(tid) || (changeEquip[tid] == null)) changeEquip[tid] = ['null','null','null','null']
			if (equipStatus && (changeEquip[tid][0] != equipStatus)) { data += [equipmentStatus: equipStatus];			changeEquip[tid][0] = equipStatus; eqpChanged = true; }
			if (thermOpStat && (changeEquip[tid][1] != thermOpStat)) { data += [thermostatOperatingState: thermOpStat];	changeEquip[tid][1] = thermOpStat; eqpChanged = true; }
			if (equipOpStat && (changeEquip[tid][2] != equipOpStat)) { data += [equipmentOperatingState: equipOpStat];	changeEquip[tid][2] = equipOpStat; eqpChanged = true; }
			// changeEquip[tid][3] = statMode
			if (eqpChanged) {
				atomicState.changeEquip = changeEquip
			}
		}		

		// API link to Ecobee's Cloud status - doesn't change unless things get broken
		boolean cldChanged = false
		Integer pollingInterval = getPollingInterval() // if this changes, we recalculate checkInterval for Health Check
		def checkInterval = 3900
		if (pollingInterval != atomicState.lastPI) {
			atomicState.lastPI = pollingInterval
			checkInterval = (pollingInterval <= 5) ? (16*60) : (((pollingInterval+1)*2)*60)
			if (checkInterval > 3600) checkInterval = 3900	// 5 minutes longer than an hour
		}
		def changeCloud =  atomicState.changeCloud	? atomicState.changeCloud  : [:]
		if ((changeCloud == [:]) || !changeCloud.containsKey(tid) || (changeCloud[tid] == null)) changeCloud[tid] = ['null','null','null','null']
		if (changeCloud[tid][0] != lastPoll)		{ data += [lastPoll: lastPoll];				changeCloud[tid][0] = lastPoll; cldChanged = true; }
		if (changeCloud[tid][1] != apiConnection)	{ data += [apiConnected: apiConnection];	changeCloud[tid][1] = apiConnection; cldChanged = true; }
		if (changeCloud[tid][2] != isConnected)		{ data += [ecobeeConnected: isConnected];	changeCloud[tid][2] = isConnected; cldChanged = true; }
		if (changeCloud[tid][3] != checkInterval)	{ data += [checkInterval: checkInterval];	changeCloud[tid][3] = checkInterval; cldChanged = true; }
		if (cldChanged) {
			// log.trace "cldChanged: true"
			atomicState.changeCloud = changeCloud
		}
		if (timers) log.debug "TIMER: Finished queueing Equip & Cloud for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		
		// Alerts & Read Only
		if (alertsUpdated || settingsUpdated || forcePoll) {
			def changeAlerts = atomicState.changeAlerts ? atomicState.changeAlerts : [:]
			if (!changeAlerts?.containsKey(tid)) changeAlerts[tid] = [:]
			def alertValues = []
			boolean alrtChanged = false
			int i = 0
			alertNamesList.each { alert ->
				def alertVal = statSettings."${alert}"
				if (alertVal == '') alertVal = 'null'
				alertValues <<	alertVal 
				if (changeAlerts[tid]?.getAt(i) != alertVal) {
					data += [ "${alert}": alertVal, ]
					alrtChanged = true
					if (atomicState.isHE && (alert == 'wifiOfflineAlert')) {
						atomicState.wifiAlert = ((alertVal == true) || (alertValue == 'true'))
						updateMyLabel()
					}
				}
				i++
			}
			if (alrtChanged) {
				// log.trace "alrtChanged: true"
				changeAlerts[tid] = alertValues
				atomicState.changeAlerts = changeAlerts
			}
		}
		if (timers) log.debug "TIMER: Finished queueing Alerts & Read Only for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		
		// Configuration Settings
		if (settingsUpdated || forcePoll) {
			def changeAttrs =  atomicState.changeAttrs ? atomicState.changeAttrs  : [:]
			if (!changeAttrs?.containsKey(tid)) changeAttrs[tid] = [:]
			def attrValues = []
			boolean attrsChanged = false
			int i = 0
			settingsNamesList.each { attr ->
				def attrVal = statSettings."${attr}"
				if (attrVal == '') attrVal = 'null'
				attrValues <<  attrVal 
				if (changeAttrs[tid]?.getAt(i) != attrVal) {
					data += [ "${attr}": attrVal, ]
					attrsChanged = true
				}
				i++
			}
			if (attrsChanged) {
				// log.trace "attrsChanged: true"
				changeAttrs[tid] = attrValues
				atomicState.changeAttrs = changeAttrs
			}
			if (timers) log.debug "TIMER: Finished queueing Attrs for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}
		
		// Thermostat Device Data
		if (statInfoUpdated || forcePoll) {
			def statInfo = atomicState.statInfo
			def changeDevice=  atomicState.changeDevice ? atomicState.changeDevice : [:]
			if (!changeDevice?.containsKey(tid)) changeDevice[tid] = [:]
			def deviceValues = [:]
			boolean devsChanged = false
			int i = 0
			statInfo[tid].each { k, v ->
				def deviceVal = v			// statInfo[tid]."${attr}"
				if (deviceVal == '') deviceVal = 'null'
				deviceValues += ["${k}": deviceVal]
				if (changeDevice[tid]?."${k}" != deviceVal) {
					data += [ "${k}": deviceVal, ]
					devsChanged = true
				}
				i++
			}
			if (devsChanged) {
				// log.trace "devsChanged: true"
				changeDevice[tid] = deviceValues
				atomicState.changeDevice = changeDevice
			}
			if (timers) log.debug "TIMER: Finished queueing Device data for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}
		
		// Temperatures - need to convert from internal F*10 to standard Thermostat units
		if (settingsUpdated || forcePoll) {
			def changeTemps =  atomicState.changeTemps	? atomicState.changeTemps  : [:]
			if (!changeTemps?.containsKey(tid)) changeTemps[tid] = [:]
			def tempValues = []
			boolean tempsChanged = false
			int i = 0
			tempSettingsList.each { temp ->
				// def tempVal = String.format("%.${apiPrecision}f", roundIt((usingMetric ? roundIt((statSettings."${temp}" / 1.8), 0) / 10.0 : statSettings."${temp}" / 10.0),apiPrecision))
				// def tempVal = roundIt((usingMetric ? roundIt((statSettings."${temp}" / 1.8), 0) / 10.0 : statSettings."${temp}" / 10.0),apiPrecision)
				def tempVal = myConvertTemperatureIfNeeded( statSettings."${temp}" / 10.0, 'F', apiPrecision)
				tempValues <<  tempVal 
				if (changeTemps[tid]?.getAt(i) != tempVal) {
					data += [ "${temp}": tempVal, ]
					tempsChanged = true
				}
				i++
			}
			if (tempsChanged) {
				// log.trace "tempsChanged: true"
				changeTemps[tid] = tempValues
				atomicState.changeTemps = changeTemps
			}
			if (timers) log.debug "TIMER: Finished queueing Temp Settings for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}
		
		// SmartApp configuration settings that almost never change (Listed in order of frequency that they should change normally)
		Integer dbgLevel = getDebugLevel()
		String tmpScale = getTemperatureScale()
		boolean cfgsChanged = false
		String timeOfDay = atomicState.timeZone ? getTimeOfDay() : getTimeOfDay(tid)
		boolean userPChanged = false
		def changeConfig = atomicState.changeConfig ? atomicState.changeConfig : [:]
		if ((changeConfig == [:]) || !changeConfig.containsKey(tid) || (changeConfig[tid] == null)) changeConfig[tid] = ['null','null','null','null','null']
		
		if (changeConfig[tid][0] != timeOfDay)		{ data += [timeOfDay: timeOfDay];				changeConfig[tid][0] = timeOfDay; cfgsChanged = true; }
		if (changeConfig[tid][1] != userPrecision)	{ data += [decimalPrecision: userPrecision];	changeConfig[tid][1] = userPrecision; cfgsChanged = true; userPChanged = true; }
		if (changeConfig[tid][2] != dbgLevel)		{ data += [debugLevel: dbgLevel];				changeConfig[tid][2] = dbgLevel; cfgsChanged = true; }
		if (changeConfig[tid][3] != tmpScale)		{ data += [temperatureScale: tmpScale];			changeConfig[tid][3] = tmpScale; cfgsChanged = true; }
		if (changeConfig[tid][4] != settings.mobile){ data += [mobile: settings.mobile];			changeConfig[tid][4] = settings.mobile; cfgsChanged = true; }
		if (cfgsChanged) {
			//log.trace "cfgsChanged: true"
			atomicState.changeConfig = changeConfig
		}
		if (timers) log.debug "TIMER: Finished queueing Config data for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		
		// Thermostat configuration settings
		// thermostatUpdated || runtimeUpdated || forcePoll || settingsUpdated || eventsUpdated || programUpdated
		// if ((isConnected && (thermostatUpdated || runtimeUpdated || forcePoll || settingsUpdated || programUpdated || eventsUpdated || extendRTUpdated)) || !isConnected) {	// new settings, programs or events
		boolean nvrChanged = false
		// Thermostat configuration stuff that almost never changes - if any one changes, send them all
		def changeNever =  atomicState.changeNever	? atomicState.changeNever  : [:]
		if (settingsUpdated || programUpdated || forcePoll || (changeNever == [:]) || !changeNever.containsKey(tid)) {														 // || (changeNever[tid] != neverList)) {
			if (changeNever[tid] == null) changeNever[tid] = ['null','null','null','null','null','null','null','null','null','null','null','null','null','null']
			if (settingsUpdated || forcePoll) {
				def autoMode = statSettings?.autoHeatCoolFeatureEnabled
				def statHoldAction = statSettings?.holdAction			// thermsotat's preference setting for holdAction:
																		// useEndTime4hour, useEndTime2hour, nextPeriod, indefinite, askMe
				// if (changeNever[tid][0] != statMode)			{ data += [thermostatMode: statMode];			changeNever[tid][0] = statMode; nvrChanged = true; }
				if (changeNever[tid][1] != autoMode)		{ data += [autoMode: autoMode];					changeNever[tid][1] = autoMode; nvrChanged = true; }
				if (changeNever[tid][2] != statHoldAction)	{ data += [statHoldAction: statHoldAction];		changeNever[tid][2] = statHoldAction; nvrChanged = true; }
				if (changeNever[tid][3] != coolStages)		{ data += [coolStages: coolStages, 
																		coolMode: (coolStages > 0)];		changeNever[tid][3] = coolStages; nvrChanged = true; }
				if (changeNever[tid][4] != heatStages)		{ data += [heatStages: heatStages,
																		heatMode: (heatStages > 0)];		changeNever[tid][4] = heatStages; nvrChanged = true; }
				if (changeNever[tid][5] != heatRange)		{ data += [heatRange: heatRange,
																		heatRangeHigh: heatHigh,
																		heatRangeLow: heatLow];				changeNever[tid][5] = heatRange; nvrChanged = true; }
				if (changeNever[tid][6] != coolRange)		{ data += [coolRange: coolRange,
																		coolRangeHigh: coolHigh,
																		coolRangeLow: coolLow];				changeNever[tid][6] = coolRange; nvrChanged = true; }
				if (changeNever[tid][11] != tempHeatDiff)	{ data += [heatDifferential: tempHeatDiff];
																	   // roundIt(tempHeatDiff, apiPrecision)];
																	   // String.format("%.${apiPrecision}f", roundIt(tempHeatDiff, apiPrecision))];	
																											changeNever[tid][11] = tempHeatDiff; nvrChanged = true; }
				if (changeNever[tid][12] != tempCoolDiff)	{ data += [coolDifferential: tempCoolDiff];
																	   // roundIt(tempCoolDiff, apiPrecision)];
																	   // String.format("%.${apiPrecision}f", roundIt(tempCoolDiff, apiPrecision))]; 
																											changeNever[tid][12] = tempCoolDiff; nvrChanged = true; }
				if (changeNever[tid][13] != tempHeatCoolMinDelta){ data += [heatCoolMinDelta: tempHeatCoolMinDelta];
																			// roundIt(tempHeatCoolMinDelta, apiPrecision)];
																			// String.format("%.${apiPrecision}f", roundIt(tempHeatCoolMinDelta, apiPrecision))]; 
																											changeNever[tid][13] = tempHeatCoolMinDelta; nvrChanged = true; }
				if (changeNever[tid][8] != auxHeatMode)		{ data += [auxHeatMode: auxHeatMode];			changeNever[tid][8] = auxHeatMode; nvrChanged = true; }
				if (changeNever[tid][9] != hasHumidifier)	{ data += [hasHumidifier: hasHumidifier];		changeNever[tid][9] = hasHumidifier; nvrChanged = true; }
				if (changeNever[tid][10] != hasDehumidifier){ data += [hasDehumidifier: hasDehumidifier];	changeNever[tid][10] = hasDehumidifier; nvrChanged = true;}
			}
			if (forcePoll || programUpdated) {
				if (climatesList && (changeNever[tid][7] != climatesList))	{ data += [programsList: climatesList];			changeNever[tid][7] = climatesList; nvrChanged = true; }
			}
			if (nvrChanged) {
				// log.trace "nvrChanged: true"
				atomicState.changeNever = changeNever
			}
			if (timers) log.debug "TIMER: Finished queueing changeNever for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}

		// Thermostat operational things that rarely change, (a few times a day at most)
		//
		// First, we need to clean up Fan Holds
		if ((thermostatHold != 'null') && (thermostatHold != '') && (currentClimateName == 'Hold: Fan')) {
			if (currentFanMode == 'on') { currentClimateName = 'Hold: Fan On' }
			else if (currentFanMode == 'auto') {
				if (statMode == 'off') {
					if (fanMinOnTime != 0) {currentClimateName = "Hold: Circulate"}
				} else {
					currentClimateName = (fanMinOnTime == 0) ? 'Hold: Auto' : 'Hold: Circulate'
				}
			}
		}
		boolean rareChanged = false
		boolean needPrograms = false
		def changeRarely = atomicState.changeRarely ? atomicState.changeRarely : [:]
		if (thermostatUpdated || runtimeUpdated ||	equipUpdated || forcePoll || settingsUpdated || eventsUpdated || programUpdated || rtReallyUpdated || extendRTUpdated || (changeRarely == [:]) || !changeRarely.containsKey(tid)) { // || (changeRarely[tid] != rarelyList)) {	
			if (changeRarely[tid] == null) changeRarely[tid] = ['null','null','null','null','null','null','null','null','null','null','null','null','null','null','null','null','null','null','null','null','null']
			// The order of these is IMPORTANT - Do setpoints and all equipment changes before notifying of the hold and program change...
			if ((tempHeatingSetpoint != null) && (forcePoll || (changeRarely[tid][19] != tempHeatingSetpoint) || userPChanged))	{ 
				needPrograms = true
				changeRarely[tid][19] = tempHeatingSetpoint
				data += [heatingSetpoint: roundIt(tempHeatingSetpoint, userPrecision)] // String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision))]
				rareChanged = true
			}
			if ((tempCoolingSetpoint != null) && (forcePoll || (changeRarely[tid][20] != tempCoolingSetpoint) || userPChanged))	{ 
				needPrograms = true
				changeRarely[tid][20] = tempCoolingSetpoint
				data += [coolingSetpoint: roundIt(tempCoolingSetpoint, userPrecision)] //String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision))] 
				rareChanged = true
			}
			if (changeRarely[tid][4]  != statMode)				{ data += [thermostatMode: statMode];						changeRarely[tid][4]  = statMode;				rareChanged = true; }
			if (changeRarely[tid][12] != currentFanMode)		{ data += [thermostatFanMode: currentFanMode];				changeRarely[tid][12] = currentFanMode;			rareChanged = true; }
			if (changeRarely[tid][13] != currentVentMode)		{ data += [vent: currentVentMode];							changeRarely[tid][13] = currentVentMode;		rareChanged = true; }
			if (changeRarely[tid][14] != ventMinOnTime)			{ data += [ventilatorMinOnTime: ventMinOnTime];				changeRarely[tid][14] = ventMinOnTime;			rareChanged = true; }
			if (changeRarely[tid][0]  != fanMinOnTime)			{ data += [fanMinOnTime: fanMinOnTime];						changeRarely[tid][0]  = fanMinOnTime;			rareChanged = true; }
			if (changeRarely[tid][5]  != humidifierMode)		{ data += [humidifierMode: humidifierMode];					changeRarely[tid][5]  = humidifierMode;			rareChanged = true; }
			if (changeRarely[tid][6]  != dehumidifierMode)		{ data += [dehumidifierMode: dehumidifierMode];				changeRarely[tid][6]  = dehumidifierMode;		rareChanged = true; }
			if (changeRarely[tid][1]  != thermostatHold)		{ data += [thermostatHold: thermostatHold];					changeRarely[tid][1]  = thermostatHold;			rareChanged = true; }
			if (changeRarely[tid][2]  != holdEndsAt)			{ data += [holdEndsAt: holdEndsAt];							changeRarely[tid][2]  = holdEndsAt;				rareChanged = true; }
			if (changeRarely[tid][3]  != statusMsg)				{ data += [holdStatus: statusMsg];							changeRarely[tid][3]  = statusMsg;				rareChanged = true; }
			if (changeRarely[tid][7]  != currentClimate)		{ data += [currentProgram: currentClimate];					changeRarely[tid][7]  = currentClimate;			rareChanged = true; }
			if (changeRarely[tid][8]  != currentClimateName)	{ data += [currentProgramName: currentClimateName];			changeRarely[tid][8]  = currentClimateName;		rareChanged = true; }
			if (changeRarely[tid][9]  != currentClimateId)		{ data += [currentProgramId: currentClimateId];				changeRarely[tid][9]  = currentClimateId;		rareChanged = true; }
			if (changeRarely[tid][10] != scheduledClimateName)	{ data += [scheduledProgramName: scheduledClimateName,
																		   scheduledProgram: scheduledClimateName];			changeRarely[tid][10] = scheduledClimateName;	rareChanged = true; }
			if (changeRarely[tid][11] != scheduledClimateId)	{ data += [scheduledProgramId: scheduledClimateId];			changeRarely[tid][11] = scheduledClimateId;		rareChanged = true; }
			if (changeRarely[tid][15] != currentClimateOwner)	{ data += [currentProgramOwner: currentClimateOwner];		changeRarely[tid][15] = currentClimateOwner;	rareChanged = true; }
			if (changeRarely[tid][16] != currentClimateType)	{ data += [currentProgramType: currentClimateType];			changeRarely[tid][16] = currentClimateType;		rareChanged = true; }
			if (changeRarely[tid][17] != scheduledClimateOwner) { data += [scheduledProgramOwner: scheduledClimateOwner];	changeRarely[tid][17] = scheduledClimateOwner;	rareChanged = true; }
			if (changeRarely[tid][18] != scheduledClimateType)	{ data += [scheduledProgramType: scheduledClimateType];		changeRarely[tid][18] = scheduledClimateType;	rareChanged = true; }

			if (rareChanged) {
				atomicState.changeRarely = changeRarely
			}
			
			// If the setpoints change, then double-check to see if the currently running Program has changed.
			// We do this by ensuring that the rest of the thermostat datasets (settings, program, events) are 
			// collected on the next poll, if they weren't collected in this poll.
			atomicState.needPrograms = (needPrograms && (!thermostatUpdated && !forcePoll))

			// Save the Program when it changes so that we can get to it easily for the child sensors
			def tempProgram = [:]
			tempProgram[tid] = currentClimateName
			if (atomicState.currentProgramName) tempProgram = atomicState.currentProgramName + tempProgram
			atomicState.currentProgramName = tempProgram
			if (timers) log.debug "TIMER: Finished queueing changeRarely for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}	  

		
		// MOVED TO THE BOTTOM because these values are dependent upon changes to states above when they get to the thermostat DTH
		// Runtime stuff that changes most frequently - we test them 1 at a time, and send only the ones that change
		// Send these first, as they generally are the reason anything else changes (so the thermostat's notification log makes sense)
		def lastOList = []
		def changeOften =  atomicState.changeOften ? atomicState.changeOften : [:]
		lastOList = changeOften[tid]
		if ( !lastOList || (lastOList.size() < 14)) lastOList = [999,'null',-1,-1,-1,-999,-999,-1,-1,-1,-1,-1,-1,-1]
		if (sensorsUpdated && (lastOList[1] != occupancy)) data += [motion: occupancy]
		if (rtReallyUpdated || forcePoll || weatherUpdated || extendRTUpdated) { // || (tempHeatingSetpoint != 0.0) || (tempCoolingSetpoint != 999.0)) {
			String wSymbol = atomicState.weather[tid]?.weatherSymbol?.toString()
			def oftenList = [tempTemperature,occupancy,runtime?.actualHumidity,null,null,wSymbol,tempWeatherTemperature,tempWeatherHumidity,tempWeatherDewpoint,
								tempWeatherPressure,humiditySetpoint,dehumiditySetpoint,humiditySetpointDisplay,userPrecision]
			
			if ((tempTemperature != null) && ((lastOList[0] != tempTemperature) || forcePoll)) data += [temperature: tempTemperature] // roundIt(tempTemperature, apiPrecision)] // String.format("%.${apiPrecision}f", roundIt(tempTemperature, apiPrecision))]
			if (forcePoll || (lastOList[2] != runtime.actualHumidity)) data += [humidity: runtime.actualHumidity]
			if (humiditySetpointDisplay && (lastOList[12] != humiditySetpointDisplay)) data += [humiditySetpointDisplay: humiditySetpointDisplay]
			if (lastOList[10] != humiditySetpoint) data += [humiditySetpoint: humiditySetpoint]
			if (lastOList[11] != dehumiditySetpoint) data += [dehumiditySetpoint: dehumiditySetpoint]		// dehumidityLevel: dehumiditySetpoint, dehumidifierLevel: dehumiditySetpoint]
			if (weatherUpdated) {
				if (wSymbol && (lastOList[5] != wSymbol)) data += [weatherSymbol: wSymbol]
				if ((tempWeatherTemperature != null) && ((lastOList[6] != tempWeatherTemperature) || userPChanged)) data += [weatherTemperature: roundIt(tempWeatherTemperature, userPrecision)] //String.format("%0${userPrecision+2}.${userPrecision}f", roundIt(tempWeatherTemperature, userPrecision))]
				if ((tempWeatherHumidity != null) && (lastOList[7] != tempWeatherHumidity)) data += [weatherHumidity: tempWeatherHumidity]
				if ((tempWeatherDewpoint != null) && ((lastOList[8] != tempWeatherDewpoint) || userPChanged)) data += [weatherDewpoint: roundIt(tempWeatherDewpoint, userPrecision)] // String.format("%0${userPrecision+2}.${userPrecision}f",roundIt(tempWeatherDewpoint,userPrecision))]
				if ((tempWeatherPressure != null) && (lastOList[9] != tempWeatherPressure)) data += [weatherPressure: tempWeatherPressure]
			}
			if (changeOften[tid] != oftenList) {
				changeOften[tid] = oftenList
				atomicState.changeOften = changeOften
			}
			if (timers) log.debug "TIMER: Finished queueing Runtime data for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
		}
		
		if (debugLevelFour) LOG("updateThermostatData() - Event data updated for thermostat ${tstatName} (${tid}) = ${data}", 1, null, 'trace')

		// it is possible that thermostatSummary indicated things have changed that we don't care about...
		// so don't send ANYTHING if we only have the initial forcePoll status for this tid
		if (data != dataStart) {
			data += [ thermostatTime:statTime[tid], ]
			if (forcePoll) data += [forced: false]			// end of forced update
			tstatNames += [tstatName]
			if (debugLevelFour) LOG("${tstatName} data: ${data}",1,null,'info')
			int ds = data.size()
			totalUpdates += forcePoll ? (ds -2) : ds
			//log.debug "For ${tid}/${tstatName}: data.size() = ${ds}, ${ds<=30?data:''}"
			collector[DNI] = [thermostatId:tid, data:data]
		}
		return collector
	}
	atomicState.thermostats = statData
	Integer nSize = tstatNames.size()
	if (nSize > 1) tstatNames.sort()
	//if (totalUpdates == 0) {
	//	if (runtimeUpdated) log.debug "Runtime:
	//}
	LOG("${preText}${totalUpdates} update${totalUpdates!=1?'s':''} for ${nSize} thermostat${nSize!=1?'s':''} ${nSize!=0?'('+tstatNames.toString()[1..-2]+')':''}", 2, null, 'info')
	if (timers) log.debug "TIMER: Thermostats update completed @ ${now() - atomicState.pollEcobeeAPIStart}ms"
}

String getChildThermostatDeviceIdsString(singleStat = null) {
	def debugLevelFour = debugLevel(4)
	if(!singleStat) {
		if (debugLevelFour) LOG('getChildThermostatDeviceIdsString() - !singleStat returning the list for all thermostats', 1, null, 'info')
		return settings.thermostats.collect { it.split(/\./).last() }.join(',')
	} else {
		// Only return the single thermostat
		if (debugLevelFour) LOG('Only have a single stat.', 1, null, 'debug')
		def ecobeeDevId = singleStat.device.deviceNetworkId.split(/\./).last()
		if (debugLevelFour) LOG("Received a single thermostat, returning the Ecobee Device ID as a String: ${ecobeeDevId}", 1, null, 'info')
		return ecobeeDevId as String	
	}
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

boolean refreshAuthToken(child=null) {
	def debugLevelFour = debugLevel(4)
	if (debugLevel(5)) LOG('Entered refreshAuthToken()', 5, null, 'trace')	

	def timeBeforeExpiry = atomicState.authTokenExpires ? atomicState.authTokenExpires - now() : 0
	// check to see if token was recently refreshed (eliminate multiple concurrent threads)
	if (timeBeforeExpiry > 2000) {
		LOG("refreshAuthToken() - skipping, token expires in ${timeBeforeExpiry/1000} seconds",2,null,'info')
		// Double check that the daemons are still running
		if (!isDaemonAlive("poll")) { LOG("refreshAuthToken - rescheduling poll daemon",1,null,'warn'); spawnDaemon("poll") }
		if (!isDaemonAlive("watchdog")) { LOG("refreshAuthToken - rescheduling watchdog daemon",1,null,'warn'); spawnDaemon("watchdog") }
		return true
	}
	
	atomicState.lastTokenRefresh = now()
	atomicState.lastTokenRefreshDate = getTimestamp()	 
	
	if (!atomicState.refreshToken) {		
		LOG('refreshAuthToken() - There is no refreshToken stored! Unable to refresh OAuth token', 1, child, "error")
		apiLost("refreshAuthToken() - No refreshToken")
		return false
	} else {
		if (debugLevelFour) LOG('Performing a refreshAuthToken()', 1, null, 'trace') // 4
		
		def refreshParams = [
//				  method: 'POST',
				uri	  : apiEndpoint,
				path  : '/token',
				query : [grant_type: 'refresh_token', refresh_token: "${atomicState.refreshToken}", client_id: ecobeeApiKey],
		]

		if (debugLevelFour) LOG("refreshParams = ${refreshParams}", 1, null, 'trace') // 4

		def jsonMap
		try {			 
			httpPost(refreshParams) { resp ->
				if (debugLevelFour) LOG("Inside httpPost resp handling.", 1, child, "trace")
				if(resp.status == 200) {
					if (debugLevelFour) LOG('refreshAuthToken() - 200 Response received - Extracting info.', 1, child, 'trace' ) // 4
					atomicState.reAttempt = 0 
					// Tell the children that we are once again connected to the Ecobee API Cloud
					if (apiConnected() != "full") {
						apiRestored()
						generateEventLocalParams() // Update the connection status
					}
					
					jsonMap = resp.data // Needed to work around strange bug that wasn't updating state when accessing resp.data directly
					if (!jsonMap) LOG("resp.data = ${resp.data}", 2, child, 'warn')
					//LOG("resp.json: ${resp.json}",2,child,'warn')			// we should get both

					if(jsonMap) {
						if (debugLevelFour) LOG("jsonMap: ${jsonMap}", 1, child) // 4
						
						atomicState.refreshToken = jsonMap.refresh_token
						
						// TODO - Platform BUG: This was not updating the state values for some reason if we use resp.data directly??? 
						//		  Workaround using jsonMap for authToken					   
						if (debugLevelFour) LOG("atomicState.authToken before: ${atomicState.authToken}", 1, child, "trace") // 4
						def oldAuthToken = atomicState.authToken
						atomicState.authToken = jsonMap?.access_token  
						if (debugLevelFour) LOG("atomicState.authToken after: ${atomicState.authToken}", 1, child, "trace") // 4
						if (oldAuthToken == atomicState.authToken) { 
							LOG('WARN: atomicState.authToken did NOT update properly! This is likely a transient problem', 1, child, 'warn')
						}
						
						// Save the expiry time for debugging purposes
						atomicState.authTokenExpires = (resp?.data?.expires_in * 1000) + now()
						LOG("refreshAuthToken() - Success! Token expires in ${String.format("%.2f",resp?.data?.expires_in/60)} minutes", 3, child, 'info') // 3
						if (debugLevel(4)) {  // 4
							LOG("Updated state.authTokenExpires = ${atomicState.authTokenExpires}", 2, child, 'trace')
							LOG("Refresh Token = state =${atomicState.refreshToken} == in: ${resp?.data?.refresh_token}", 2, child, 'trace')
							LOG("OAUTH Token = state ${atomicState.authToken} == in: ${resp?.data?.access_token}", 2, child, 'trace')
						}
						
						def action = atomicState.action
						// Reset saved action
						atomicState.action = ''
						if (action) { // && atomicState.action != "") {
							if (debugLevelFour) LOG("Token refreshed. Rescheduling aborted action: ${action}", 1, child, 'trace')  // 4
							runIn( 2, "${action}", [overwrite: true]) // this will collapse multiple threads back into just one
						} else {
							// Assume we had to re-authorize during a pollEcobeeAPI session
							runIn( 2, "pollChildren", [overwrite: true])
						}
					} else {
						LOG("No jsonMap??? ${jsonMap}", 2, child, 'trace')
					}	
					// scheduleWatchdog(null, false) 
					// Reschedule polling if it has been a while since the previous poll	
					if (!isDaemonAlive("poll")) { LOG("refreshAuthToken - rescheduling poll daemon",1,null,'warn'); spawnDaemon("poll") }
					if (!isDaemonAlive("watchdog")) { LOG("refreshAuthToken - rescheduling watchdog daemon",1,null,'warn'); spawnDaemon("watchdog") }
					atomicState.inTimeoutRetry = 0
					return true
				} else {
					LOG("refreshAuthToken() - Failed ${resp.status} : ${resp.status.code}!", 1, child, 'error')
					return false
				}
			}
		} catch (groovyx.net.http.HttpResponseException e) {
			boolean result = false
			//LOG("refreshAuthToken() - HttpResponseException occurred. Exception info: ${e} StatusCode: ${e.statusCode}  response? data: ${e.getResponse()?.getData()}", 1, null, "error")
			LOG("refreshAuthToken() - HttpResponseException occurred. Exception info: ${e} StatusCode: ${e.statusCode}", 1, child, "error")
			if ((e.statusCode != 401) && (e.statusCode != 400)) {
				runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true])
			} else if ((e.statusCode == 401) || (e.statusCode == 400)) {			
				atomicState.reAttempt = atomicState.reAttempt + 1
				if (atomicState.reAttempt > 5) {						
					apiLost("Too many retries (${atomicState.reAttempt - 1}) for token refresh.")				
					result = false
				} else {
					if (debugLevelFour) LOG("Setting up runIn for refreshAuthToken", 1, child, 'trace') // 4
					if ( atomicState.isHE || canSchedule() ) {						
						runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true]) 
					} else { 
						if (debugLevelFour) LOG("Unable to schedule refreshAuthToken, running directly", 1, child, 'trace')			 // 4			
						result = refreshAuthToken(child) 
					}
				}
			}
			generateEventLocalParams() // Update the connected state at the thermostat devices
			return result
		} catch (java.util.concurrent.TimeoutException e) {
			LOG("refreshAuthToken(), TimeoutException: ${e}.", 1, child, "warn")
			// Likely bad luck and network overload, move on and let it try again
			if ( atomicState.isHE || canSchedule() ) { runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true]) } else { refreshAuthToken(child) }			 
			return false
		} catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException | javax.net.ssl.SSLPeerUnverifiedException |
				 javax.net.ssl.SSLHandshakeException | java.net.SocketTimeoutException | java.net.UnknownHostException | java.net.UnknownHostException e) {
			LOG("refreshAuthToken() - ${e}.",1,child,'warn')  // Just log it, and hope for better next time...
			if (apiConnected != 'warn') {
				atomicState.connected = 'warn'
				updateMyLabel()
				atomicState.lastPoll = now()
				atomicState.lastPollDate = getTimestamp()
				generateEventLocalParams()
			}
			def inTimeoutRetry = atomicState.inTimeoutRetry
			if (inTimeoutRetry == null) inTimeoutRetry = 0
			// try to re-Authorize for 5 minutes
			if (inTimeoutRetry < 20) runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true])
			atomicState.inTimeoutRetry = inTimeoutRetry + 1
			//LOG("refreshAuthToken(), ConnectTimeoutException: ${e}.",1,null,'warn')
			//if(canSchedule()) { runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true]) } else { refreshAuthToken() }
			return false
		} catch (Exception e) {
			LOG("refreshAuthToken() - Exception: ${e}.", 1, child, "error")			   
			throw e
			return false
		}
	}
	return true
}

void queueFailedCall(String routine, String DNI, numArgs, arg1=null, arg2=null, arg3=null, arg4=null, arg5=null, arg6=null, arg7=null) {
	//log.debug "queueCall routine: ${routine}, DNI: ${DNI}, ${arg1}, ${arg2}, ${arg3}, ${arg4}, ${arg5}, ${arg6}, ${arg7}"	 // ${routine}" // , args: ${theArgs}"
	if ((atomicState.connected == 'full') && atomicState.runningCallQueue) return // don't queue when we are clearing the queue
	runIn(2, 'queueCall', [overwrite: false, data: [routine: routine, DNI: DNI, args: [arg1, arg2, arg3, arg4, arg5], done: false, numArgs: numArgs]])
	if (atomicState.callsQueued == null) { atomicState.callsQueued = 0; atomicState.callsRun = 0; }
	atomicState.callsQueued = atomicState.callsQueued + 1
}

void queueCall(data) {
	def dbgLvl = 1
	LOG("queueCall() data: ${data}", dbgLvl, null, 'trace')
	
	def failedCallQueue = null
	def queueSize = 0
	if (!atomicState.queueingFailedCalls) {
		atomicState.queueingFailedCalls = true
	} else {
		failedCallQueue = atomicState.callQueue
		queueSize = failedCallQueue ? failedCallQueue.size() : 0
	}
	LOG("queueSize: ${queueSize}, failedCallQueue: ${failedCallQueue}", dbgfLvl, null, 'trace')
	if (queueSize == 0) {
		failedCallQueue = ["${queueSize}": data]
	} else {
		failedCallQueue = failedCallQueue + ["${queueSize}": data]
	}
	
	log.debug "failedCallQueue: ${failedCallQueue}"
	//atomicState.callQueue = toJson(failedCallQueue)
	atomicState.callQueue = failedCallQueue
	
	// runIn( 5, runCallQueue )
}

void runCallQueue() {
	def dbgLvl = 1
	LOG("runCallQueue() connected: ${atomicState.connected}, callQueue: ${atomicState.callQueue}, runningCallQueue: ${atomicState.runningCallQueue}", dbgLvl, null, 'trace')
	if (atomicState.connected?.toString() != 'full') return
	if ((atomicState.callQueue == null) || (atomicState.callQueue?.size() == 0)) {atomicState.runningCallQueue = false; return;}
	if (atomicState.runningCallQueue) return
	atomicState.runningCallQueue = true
	
	//while (atomicState.connected == 'full') {
	boolean failed = false
	boolean result = true
	def queue = atomicState.callQueue
	int i
	for (i=0; i<queue.size(); i++) {
		//log.debug queue."${i}"
		def cmd = queue."${i}"
		log.debug "${i}: ${cmd}"
		if (cmd?.done?.toString() == 'false') {		
			if (atomicState.connected == 'full') {
				// execute the command
				//def command = "${cmd.routine}(getChildDevice(${cmd.DNI})"
				cmd.args?.each {
					if (it == [:]) it = null
				}
				//command += ')'
				//log.debug "${i}: ${command}"
				if (true) { // (cmd.routine == 'resumeProgram') {
					switch(cmd.numArgs) {
						case 0:
							result = this."${cmd.routine}"(getChildDevice(cmd.DNI))
							break;
						case 1:
							result = this."${cmd.routine}"(getChildDevice(cmd.DNI), cmd.args[0])
							break;
						case 2:
							result = this."${cmd.routine}"(getChildDevice(cmd.DNI), cmd.args[0], cmd.args[1] )
							break;
						case 3:
							result = this."${cmd.routine}"(getChildDevice(cmd.DNI), cmd.args[0], cmd.args[1], cmd.args[2])
							break;
						case 4:
							result = this."${cmd.routine}"(getChildDevice(cmd.DNI), cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3])
							break;
						case 5:
							result = this."${cmd.routine}"(getChildDevice(cmd.DNI), cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4])
							break;
						//case 6:
						//	result = this."${cmd.routine}"(getChildDevice(cmd.DNI), cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4], cmd.args[5])
						//	break;
						//case 7:
						//	result = this."${cmd.routine}"(getChildDevice(cmd.DNI), cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4], cmd.args[5], cmd.args[6])
						//	break;
					}
					LOG("RESULT: ${result}", dbgLvl, null, 'trace')
				}
				if (result) {
					queue = atomicState.callQueue
					cmd = queue."${i}"
					cmd.done = true
					queue."${i}" = cmd
					atomicState.callQueue = queue
					atomicState.callsRun = atomicState.callsRun + 1
				} else {
					i = queue.size()
					failed = true
				}
			}
		}
	}
	if (!failed) { atomicState.callQueue = [:]; LOG("callQueue completed and cleared", 2, null, 'info'); }
	atomicState.runningCallQueue = false
	atomicState.queueingFailedCalls = false
}

boolean resumeProgram(child, String deviceId, resumeAll=true) {
	if (child == null) {
		child = getChildDevice(getThermostatDNI(deviceId))
	}
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to resumeProgram(${child}, ${deviceId}, ${resumeAll})", 2, child, 'warn')
		queueFailedCall('resumeProgram', child.device.deviceNetworkId, 2, deviceId, resumeAll)
		return false
	}

	boolean result = true
	boolean debugLevelFour = debugLevel(4)
	boolean debugLevelThree = debugLevel(3)
	String statName = getThermostatName(deviceId)
	if (debugLevelThree) LOG("Entered resumeProgram for thermostat ${statName} (${deviceId}) with child: ${child.device?.displayName}", 3, child, 'trace')
	
	String allStr = resumeAll ? 'true' : 'false'
	def jsonRequestBody = '{"functions":[{"type":"resumeProgram","params":{"resumeAll":"' + allStr + '"}}],"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"}}'
	if (debugLevelFour) LOG("jsonRequestBody = ${jsonRequestBody}", 1, child)
	
	result = sendJson(child, jsonRequestBody)
	if (debugLevelThree) LOG("resumeProgram(${resumeAll}) for ${child} (${deviceId}) returned ${result}", 3, child,result?'info':'warn')
	if (result) {
		def program = atomicState.program[deviceId]
		if ( program ) {
			def climateId = program?.currentClimateRef 
			def climate = program.climates?.find { it.climateRef == climateId }
			def climateName = climate?.name

			// send the new heat/cool setpoints and ProgramId to the DTH - it will update the rest of the related displayed values itself
			Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
			Integer userPrecision = getTempDecimals()						// user's requested display precision
			def tempHeatAt = climate.heatTemp
			def tempCoolAt = climate.coolTemp
			def tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt / 10.0), 'F', apiPrecision)
			def tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt / 10.0), 'F', apiPrecision)
			def updates = [	heatingSetpoint:	tempHeatingSetpoint, // String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
							coolingSetpoint:	tempCoolingSetpoint, // String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
							currentProgramName: climateName,
							currentProgram:		climateName,
							currentProgramId:	climateId ]
			LOG("resumeProgram(${statName}) ${updates}",2,null,'info')
			child.generateEvent(updates)			// force-update the calling device attributes that it can't see
		}
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to resumeProgram(${child}, ${deviceId}, ${resumeAll})", 2, child, 'warn')
			queueFailedCall('resumeProgram', child.device.deviceNetworkId, 2, deviceId, resumeAll)
		}
	}
	return result
}

boolean setHVACMode(child, deviceId, mode) {
	LOG("setHVACMode(${mode})", 4, child)
	def result = setMode(child, mode, deviceId)
	LOG("setHVACMode(${mode}) returned ${result}", 3, child,result?'info':'warn')	 
	// if (result) atomicState.forcePoll = true			// force next poll to get updated runtime data
	// if (!result) queue failed request
	return result
}

boolean setMode(child, mode, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setMode', child.device.deviceNetworkId, 2, mode, deviceId)
		return false
	}
	def debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("setMode(${mode}) for ${child} (${deviceId})", 1, child, 'trace')
	// queueFailedCall('setMode', child, mode, deviceId)
		
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"hvacMode":"'+"${mode}"+'"}}}'  
	if (debugLevelFour) LOG("Mode Request Body = ${jsonRequestBody}", 1, child, 'trace')
	
	def result = sendJson(child, jsonRequestBody)
	if (debugLevelFour) LOG("setMode to ${mode} for ${child} (${deviceId}) with result ${result}", 1, child, 'trace')
	if (result) {
		LOG("setMode(${mode}) for ${child} (${deviceId}) - Succeeded", 1, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API is connection lost, queueing failed call to setMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setMode', child.device.deviceNetworkId, 2, mode, deviceId)
		} else {
			LOG("setMode(${mode}) for ${child} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result
}

boolean setHumidifierMode(child, mode, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHumidifierMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setHumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
		return false
	}
	def debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG ("setHumidifierMode(${mode}) for ${child} (${deviceId})", 1, child, 'trace')
	
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"humidifierMode":"'+"${mode}"+'"}}}'	
	def result = sendJson(child, jsonRequestBody)
	if (debugLevelFour) LOG("setHumidifierMode to ${mode} for ${child} (${deviceId}) with result ${result}", 1, child, 'trace')
	if (result) {
		LOG("setHumidifierMode(${mode}) for ${child} (${deviceId}) - Succeeded", 2, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setHumidifierMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setHumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
			return false
		} else {
			LOG("setHumidifierMode(${mode}) for ${child} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result
}

boolean setHumiditySetpoint(child, value, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHumiditySetpoint(${child}, ${value}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setHumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
		return false
	}
	
	LOG ("setHumiditySetpoint${value}) for ${deviceId}", 5, child, 'trace')
						
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"humidity":"'+"${value}"+'"}}}'  
	LOG("setHumiditySetpoint Request Body = ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("setHumiditySetpoint to ${value} for ${child} (${deviceId}) with result ${result}", 4, child, 'trace')
	if (result) {
		LOG("setHumiditySetpoint(${value}) for ${child} (${deviceId}) - Succeeded", 2, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setHumiditySetpoint(${child}, ${value}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setHumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
		} else {
			LOG("setHumiditySetpoint(${value}) for ${child} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result	
}

boolean setDehumidifierMode(child, mode, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setDehumidifierMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setDehumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
		return false
	}
	
	LOG ("setDehumidifierMode(${mode}) for ${child} (${deviceId})", 5, child, 'trace')
						
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"dehumidifierMode":"'+"${mode}"+'"}}}'  
	LOG("dehumidifierMode Request Body = ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("setDehumidifierMode to ${mode} for ${child} (${deviceId}) with result ${result}", 4, child, 'trace')
	if (result) {
		LOG("setDehumidifierMode(${mode}) for ${child} (${deviceId}) - Succeeded", 2, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setDehumidifierMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setDehumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
		} else {
			LOG("setDehumidifierMode(${mode}) for ${child} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result
}

boolean setDehumiditySetpoint(child, value, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setDehumiditySetpoint(${child}, ${value}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setDehumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
		return false
	}
	
	LOG ("setDehumiditySetpoint${value}) for ${child} (${deviceId})", 5, child, 'trace')
						
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"dehumidifierLevel":"'+"${value}"+'"}}}'	
	LOG("setDehumiditySetpoint Request Body = ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("setDehumiditySetpoint to ${value} with result ${result}", 4, child, 'trace')
	if (result) {
		LOG("setDehumiditySetpoint (${value}) for ${child} (${deviceId}) - Succeeded", 2, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setDehumiditySetpoint(${child}, ${value}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setDehumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
		} else {
			LOG("setDehumiditySetpoint (${mode}) for ${child} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result	
}

boolean setFanMinOnTime(child, deviceId, howLong) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setFanMinOnTime(${child}, ${deviceId}, ${howLong}", 2, child, 'warn')
		queueFailedCall('setFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
		return false
	}
	
	LOG("setFanMinOnTime(${howLong}) for ${child} (${deviceId})", 4, child, 'trace')
	
	if ((howLong < 0) || (howLong > 55)) {
		LOG("setFanMinOnTime() for ${child} (${deviceId}) - Invalid Argument ${howLong}",2,child,'warn')
		return false
	}
	
	def thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":'+howLong+'}}'
	def thermostatFunctions = ''
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
	def result = sendJson(child, jsonRequestBody)
	LOG("setFanMinOnTime(${howLong}) for ${child} (${deviceId}) returned ${result}", result?2:4, child, result?'info':'warn')	 
	if (result) {
		// atomicState.forcePoll = true			// force next poll to get updated runtime data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setFanMinOnTime(${child}, ${deviceId}, ${howLong}", 2, child, 'warn')
			queueFailedCall('setFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
		}
	}
	return result
}

boolean setVacationFanMinOnTime(child, deviceId, howLong) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setVacationFanMinOnTime(${child}, ${deviceId}, ${howLong}", 2, child, 'warn')
		queueFailedCall('setVacationFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
		return false
	}
	
	LOG("setVacationFanMinOnTime(${howLong}) for ${child} (${deviceId})", 4, child)	  
	if (!howLong.isNumber() || (howLong.toInteger() < 0) || howLong.toInteger() > 55) {		// Documentation says 60 is the max, but the Ecobee3 thermostat maxes out at 55 (makes 60 = 0)
		LOG("setVacationFanMinOnTime() for ${child} (${deviceId}) - Invalid Argument ${howLong}",2,child,'warn')
		return false
	}
	
	def evt = atomicState.runningEvent[deviceId]
	boolean hasEvent = true
	if (!evt) {
		hasEvent = false						// no running event defined
	} else {
		if (evt.running != true) hasEvent = false		// shouldn't have saved it if it wasn't running
		if (hasEvent && (evt.type != "vacation")) hasEvent = false	// we only override vacation fanMinOnTime setting
	}
	if (!hasEvent) {
		LOG("setVacationFanMinOnTime() for ${child} (${deviceId}) - Vacation not active", 2, child, 'warn')
		return false
	}
	if (evt.fanMinOnTime.toInteger() == howLong.toInteger()) return true	// didn't need to do anything!
	
	if (deleteVacation(child, deviceId, evt.name)) { // apparently on order to change something in a vacation, we have to delete it and then re-create it..
  
		def thermostatSettings = ''
		def thermostatFunctions = '{"type":"createVacation","params":{"name":"' + evt.name + '","coolHoldTemp":"' + evt.coolHoldTemp + '","heatHoldTemp":"' + evt.heatHoldTemp + 
									'","startDate":"' + evt.startDate + '","startTime":"' + evt.startTime + '","endDate":"' + evt.endDate + '","endTime":"' + evt.endTime + 
									'","fan":"' + evt.fan + '","fanMinOnTime":"' + "${howLong}" + '"}}'
		def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'

		LOG("setVacationFanMinOnTime() for ${child} (${deviceId}) - before sendJson() jsonRequestBody: ${jsonRequestBody}", 4, child, "info")

		def result = sendJson(child, jsonRequestBody)
		LOG("setVacationFanMinOnTime(${howLong}) for ${child} (${deviceId}) returned ${result}", 4, child, result?'info':'warn') 
		if (result) {
			// atomicState.forcePoll = true			// force next poll to get updated data
			runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
		} else {
			if (atomicState.connected?.toString() != 'full') {
				LOG("API connection lost, queueing Failed call to setVacationFanMinOnTime(${child}, ${deviceId}, ${howLong}", 2, child, 'warn')
				queueFailedCall('setVacationFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
			}		 
		}
		return result
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setVacationFanMinOnTime(${child}, ${deviceId}, ${howLong}", 2, child, 'warn')
			queueFailedCall('setVacationFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
		}
		return false
	}
	return false
}

boolean createVacationTemplate(child, deviceId) {
	String vacationName = 'tempVac810n'
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	// delete any old temporary vacation that we created
	deleteVacation(child, deviceId, vacationName)
	
	// Create the temporary vacation
	def thermostatSettings = ''
	def thermostatFunctions =	'{"type":"createVacation","params":{"name":"' + vacationName + 
								'","coolHoldTemp":"850","heatHoldTemp":"550","startDate":"2034-01-01","startTime":"08:30:00","endDate":"2034-01-01","endTime":"09:30:00","fan":"auto","fanMinOnTime":"5"}}'
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
	LOG("before sendJson() jsonRequestBody: ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("createVacationTemplate(${vacationName}) for ${child} (${deviceId}) returned ${result}", 4, child, result?'info':'warn')
	// if (!result) queue failed request
	
	// Now, delete the temporary vacation
	result = deleteVacation(child, deviceId, vacationName)
	LOG("deleteVacation(${vacationName}) for ${child} (${deviceId}) returned ${result}", 4, child, 'trace')
	return true
}

boolean deleteVacation(child, deviceId, vacationName=null ) {
	def vacaName = vacationName
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (!vacaName) {		// no vacationName specified, let's find out if one is currently running
		def evt = atomicState.runningEvent[deviceId]
		if (!evt ||	 (evt.running != true) || (evt.type != "vacation") || !evt.name) {
			LOG("deleteVacation() for ${child} (${deviceId}) - Vacation not active", 1, child, 'warn')
			return true	// Asked us to delete the current vacation, and there isn't one - I'd still call that a success!
		}
		vacaName = evt.name as String		// default names are Very Big Numbers
	}

	def thermostatSettings = ''
	def thermostatFunctions = '{"type":"deleteVacation","params":{"name":"' + vacaName + '"}}'
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
	boolean result = sendJson(child, jsonRequestBody)
	LOG("deleteVacation() for ${child} (${deviceId}) returned ${result}", 2, child,result?'info':'warn')
	
	if (vacationName == null) {
		resumeProgram(child, deviceId, true)		// force back to previously scheduled program
		pollChildren(deviceId,true)					// and finally, update the state of everything (hopefully)
	}
	// if (result) atomicState.forcePoll = true			// force next poll to get updated data
	runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	return result
}

// The calling child device should have verified that the current Event is demandResponse, and that it isn't mandatory
boolean cancelDemandResponse(child, String deviceId) {
	if (child == null) {
		child = getChildDevice(getThermostatDNI(deviceId))
	}
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to cancelDemandResponse(${child}, ${deviceId})", 2, child, 'warn')
		queueFailedCall('cancelDemandResponse', child.device.deviceNetworkId, 1, deviceId)
		return false
	}
	boolean debugLevelFour = debugLevel(4)
	boolean debugLevelThree = debugLevelFour ?: debugLevel(3)
	String statName = getThermostatName(deviceId)
	if (debugLevelThree) LOG("Entered cancelDemandResponse for thermostat ${statName} (${deviceId}) with child: ${child.device?.displayName}", 1, child, 'trace')

	// this is probably the ONLY time we just want to pop the stacked Hold events...
	def jsonRequestBody = '{"functions":[{"type":"resumeProgram","params":{"resumeAll":"false"}}],"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"}}'
	if (debugLevelFour) LOG("jsonRequestBody = ${jsonRequestBody}", 1, child, 'debug')
	
	boolean result = sendJson(child, jsonRequestBody)
	if (debugLevelThree) LOG("cancelDemandResponse() for ${child.device?.displayName} (${deviceId}) returned ${result}", 1, child, result?'info':'warn')
	return result
}

// Should only be called by child devices, and they MUST provide sendHoldType and sendHoldHours as of version 1.2.0
boolean setHold(child, heating, cooling, deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHold(${child}, ${heating}, ${cooling}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
		queueFailedCall('setHold', child.device.deviceNetworkId, 5, heating, cooling, deviceId, sendHoldType, sendHoldHours)
		return false
	}
	boolean ST = atomicState.isST
	
	def currentThermostatHold = ST ? child.device.currentValue('thermostatHold') : child.device.currentValue('thermostatHold', true)
	if (currentThermostatHold == 'vacation') {
		LOG("setHold(): Can't set a new hold for ${child} (${deviceId}) while in a vacation hold",2,null,'warn')
		// can't change fan mode while in vacation hold, so silently fail
		return false
	}  else if (currentThermostatHold != '') {
		// must resume program first
		resumeProgram(child, deviceId, true)
	}
	def isMetric = (getTemperatureScale() == "C")
	def h = roundIt((isMetric ? (cToF(heating) * 10.0) : (heating * 10.0)), 0)		// better precision using BigDecimal round-half-up
	def c = roundIt((isMetric ? (cToF(cooling) * 10.0) : (cooling * 10.0)), 0)
	
	LOG("setHold() for ${child} (${deviceId}) - h: ${heating}(${h}), c: ${cooling}(${c}), ${sendHoldType}, ${sendHoldHours}", 2, child, 'trace')
	
	def theHoldType = sendHoldType // ? sendHoldType : whatHoldType()
	if (theHoldType == 'nextTransition') {
		// Check if setpoints are the same as currentClimateRef, if so, don't set a new hold
		// ResumeProgram above already sent the setpoint display values for the currentClimate to the DTH
		def ncHsp = (ST ? child.device.currentValue('heatingSetpoint') : child.device.currentValue('heatingSetpoint', true)) as BigDecimal
		def ncCsp = (ST ? child.device.currentValue('coolingSetpoint') : child.device.currentValue('coolingSetpoint', true)) as BigDecimal
		def currHeatAt = roundIt((isMetric ? (cToF(ncHsp) * 10.0) : (ncHsp * 10.0)), 0)		// better precision using BigDecimal round-half-up
		def currCoolAt = roundIt((isMetric ? (cToF(ncCsp) * 10.0) : (ncCsp * 10.0)), 0)
		LOG("setHold() - currHeat: ${currHeatAt}, currCool: ${currCoolAt}",2, child, 'trace')
		// if ((c==currCoolAt) && (h==currHeatAt)) {
		if ((cooling==currCoolAt) && (heating==currHeatAt)) {
			LOG("setHold() for ${child} (${deviceId}) requesting current setpoints; ignoring", 3, child, 'info')
			return true 
		}
	} else if (theHoldType == 'holdHours') {
		theHoldType = 'holdHours","holdHours":"' + sendHoldHours.toString()
	} 
	
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":[{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + 
							'","holdType":"' + theHoldType + '"}}]}'
	LOG("setHold() for thermostat ${child.device.displayName} - about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child)
	
	def result = sendJson(child, jsonRequestBody)
	LOG("setHold() for ${child} (${deviceId}) returned ${result}", 4, child,result?'info':'error')
	if (result) { 
		// send the new heat/cool setpoints and ProgramId to the DTH - it will update the rest of the related displayed values itself
		Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
		Integer userPrecision = getTempDecimals()						// user's requested display precision
		def tempHeatAt = h //.toBigDecimal()
		def tempCoolAt = c //.toBigDecimal()
		def tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt / 10.0), 'F', apiPrecision)
		def tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt / 10.0), 'F', apiPrecision)
		def updates = [heatingSetpoint: tempHeatingSetpoint,	// String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
					   coolingSetpoint: tempCoolingSetpoint,		// String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
					   currentProgramName: 'Hold: Temp'
					  ]
		LOG("setHold() for ${child} (${deviceId}) - ${updates}",3,null,'info')
		child.generateEvent(updates)			// force-update the calling device attributes that it can't see
		// atomicState.forcePoll = true				// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setHold(${child}, ${heating}, ${cooling}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
			queueFailedCall('setHold', child.device.deviceNetworkId, 5, heating, cooling, deviceId, sendHoldType, sendHoldHours)
		}
	}
	return result
}

// Should only be called by child devices, and they MUST provide sendHoldType and sendHoldHours as of version 1.2.0
boolean setFanMode(child, fanMode, fanMinOnTime, deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setFanMode(${child}, ${fanMode}, ${fanMinOnTime}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
		queueFailedCall('setFanMode', child.device.deviceNetworkId, 5, fanMode, fanMinOnTime, deviceId, sendHoldType, sendHoldHours)
		return false
	}
	
	LOG("setFanMode(${fanMode}) for ${child} (${deviceId})", 5, null, 'trace') 
	boolean isMetric = (getTemperatureScale() == "C")
	boolean ST = atomicState.isST
	
	def currentThermostatHold = ST ? child.device.currentValue('thermostatHold') : child.device.currentValue('thermostatHold', true)
	if (currentThermostatHold == 'vacation') {
		LOG("setFanMode() for ${child} (${deviceId}): Can't change Fan Mode while in a vacation hold",2,null,'warn')
		return false
	} else if (currentThermostatHold != '') {
		// must resume program first
		resumeProgram(child, deviceId, true)
	}

	// Per this thread: http://developer.ecobee.com/api/topics/qureies-related-to-setfan
	// def extraParams = [isTemperatureRelative: "false", isTemperatureAbsolute: "false"]
	// And then these values are ignored when setting only the fan
	// use the device's values, not the ones from our last API refresh
	// BUT- IF changing Fan Mode while in a Hold, maybe we should be overloading the hold instead of cancelling it?
	def ncHsp = (ST ? child.device.currentValue('heatingSetpoint') : child.device.currentValue('heatingSetpoint', true)) as BigDecimal
	def ncCsp = (ST ? child.device.currentValue('coolingSetpoint') : child.device.currentValue('coolingSetpoint', true)) as BigDecimal
	def h = roundIt((isMetric ? (cToF(ncHsp) * 10.0) : (ncHsp * 10.0)), 0)		// better precision using BigDecimal round-half-up
	def c = roundIt((isMetric ? (cToF(ncCsp) * 10.0) : (ncCsp * 10.0)), 0)
	
	def theHoldType = sendHoldType // ? sendHoldType : whatHoldType(child)
	if (theHoldType == 'holdHours') {
		theHoldType = 'holdHours","holdHours":"' + sendHoldHours.toString()
	}

	def thermostatSettings = ''
	def thermostatFunctions = ''
	
	// CIRCULATE: same as AUTO, but with a non-zero fanMinOnTime
	if (fanMode == "circulate") {
		LOG("setFanMode() for ${child} (${deviceId})- fanMode == 'circulate'", 5, child, "trace") 
		fanMode = "auto"
		
		if (fanMinOnTime == null) fanMinOnTime = 20	// default for 'circulate' if not specified
		thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":"' + "${fanMinOnTime}" + '"}}'
		thermostatFunctions = '{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + '","holdType":"' + theHoldType + 
								'","fan":"'+fanMode+'","isTemperatureAbsolute":false,"isTemperatureRelative":false}}'
								
	// OFF: Sets fanMinOnTime to 0; if thermostatMode == off, this will stop the fan altogether (so long as not in a hold)
	} else if (fanMode == "off") {
		// How to turn off the fan http://developer.ecobee.com/api/topics/how-to-turn-fan-off
		// HVACmode=='off', fanMode=='auto' and fanMinOnTime==0
		// NOTE: Once you turn it off it does not automatically come back on if you select resume program
		fanMode = "auto"   
		// thermostatSettings = ',"thermostat":{"settings":{"hvacMode":"off","fanMinOnTime":"0"}}'		// now requires thermsotatMode to already be off, instead of overwriting it here
		thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":"0"}}'
		thermostatFunctions = ''
	// AUTO or ON
	} else {
		if (fanMinOnTime == null) fanMinOnTime = 0 // this maybe should be the fanTime of the current/scheduledProgram
		thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":"' + "${fanMinOnTime}" + /*'","hvacMode":"' + priorHVACMode + */ '"}}'
		thermostatFunctions = '{"type":"setHold","params":{"coolHoldTemp":"'+c+'","heatHoldTemp":"'+h+'","holdType":"'+theHoldType+
									'","fan":"'+fanMode+'","isTemperatureAbsolute":false,"isTemperatureRelative":false}}'
	}	 
	
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"'+deviceId+'"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	LOG("setFanMode() for ${child} (${deviceId}) - about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child, 'trace')
	
	boolean result = sendJson(child, jsonRequestBody)
	LOG("setFanMode(${fanMode}) for ${child} (${deviceId}) returned ${result}", 4, child, result?'info':'warn')
	// if (result) atomicState.forcePoll = true			// force next poll to get updated data
	runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	if (!result) {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setFanMode(${child}, ${fanMode}, ${fanMinOnTime}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
			queueFailedCall('setFanMode', child.device.deviceNetworkId, 5, fanMode, fanMinOnTime, deviceId, sendHoldType, sendHoldHours)
		}
	}
	return result	 
}

boolean setProgram(child, program, String deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setProgram(${child}, ${program}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
		queueFailedCall('setProgram', child.device.deviceNetworkId, 4, program, deviceId, sendHoldType, sendHoldHours)
		return false
	}

	// NOTE: Will use only the first program if there are two with the same exact name
	LOG("setProgram(${program}) for ${child} (${deviceId}) - holdType: ${sendHoldType}, holdHours: ${sendHoldHours}", 2, child, 'info')		
	
	String currentThermostatHold = atomicState.isST ? child.device.currentValue('thermostatHold') : child.device.currentValue('thermostatHold', true)
	if (currentThermostatHold == 'vacation') {									// shouldn't happen, child device should have already verified this
		LOG("setProgram() for ${child} (${deviceId}): Can't change Program while in a vacation hold",2,null,'warn')
		return false
	} else if ((currentThermostatHold != null) && (currentThermostatHold != 'null') && (currentThermostatHold != '')) {									// shouldn't need this either, child device should have done this before calling us
		LOG("setProgram( ${program} ) for ${child} (${deviceId}): Resuming from current hold first",2,null,'info')
		resumeProgram(child, deviceId, true)
	}
	
	// We'll take the risk and use the latest received climates data (there is a small chance it could have changed recently but not yet been picked up)
	def climates = atomicState.program[deviceId].climates
	def climate = climates?.find { it.name.toString() == program.toString() }  // vacation holds can have a number as their name
	LOG("climates - {$climates}", 5, child)
	LOG("climate - {$climate}", 5, child)
	def climateRef = climate?.climateRef.toString()
	
	LOG("setProgram() for ${child} (${deviceId}) - climateRef = {$climateRef}", 4, child)
	
	if (climate == null) { return false }
	
	def theHoldType = sendHoldType //? sendHoldType : whatHoldType(child)
	if (theHoldType == 'holdHours') {
		theHoldType = 'holdHours","holdHours":"' + sendHoldHours.toString()
	}
	def jsonRequestBody = '{"functions":[{"type":"setHold","params":{"holdClimateRef":"'+climateRef+'","holdType":"'+theHoldType+'"}}],"selection":{"selectionType":"thermostats","selectionMatch":"'+deviceId+'"}}'

	LOG("setProgram() for thermostat ${child.device.displayName}: about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child)	
	boolean result = sendJson(child, jsonRequestBody)	
	LOG("setProgram(${climateRef}) for ${child} (${deviceId}) returned ${result}", 4, child, 'info')
	
	if (result) { 
		// send the new heat/cool setpoints and ProgramId to the DTH - it will update the rest of the related displayed values itself
		Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
		Integer userPrecision = getTempDecimals()						// user's requested display precision
		def tempHeatAt = climate.heatTemp
		def tempCoolAt = climate.coolTemp
		def tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt / 10.0), 'F', apiPrecision)
		def tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt / 10.0), 'F', apiPrecision)
		def updates = [heatingSetpoint:	tempHeatingSetpoint,	// String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
					   coolingSetpoint: tempCoolingSetpoint,	// String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
					   currentProgram:	program,
					   currentProgramId:climateRef]
		LOG("setProgram() for ${child} (${deviceId}): ${updates}",3,null,'info')
		child.generateEvent(updates)			// force-update the calling device attributes that it can't see
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API is not fully connected, queueing call to setProgram(${child}, ${program}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
			queueFailedCall('setProgram', child.device.deviceNetworkId, 4, program, deviceId, sendHoldType, sendHoldHours)
		} else {
			LOG("setProgram(${program}) for ${child} (${deviceId}) - Failed", 1, child, 'warn') 
		}
	}
	return result
}

////////////////////////////////////////////////////////////////
boolean addSensorToProgram(child, deviceId, sensorId, programId) {
	String statName = getThermostatName( deviceId )
	String sensName = child.device?.displayName
	LOG("addSensorToProgram(${sensName},${statName},${sensorId},${programId})",4,child,'trace')
	
	// we basically have to edit the program object in place, and then return the entire thing back to the Ecobee API
	def program
	def climateChange = atomicState.climateChange
	if (climateChange && climateChange[deviceId]) {
		program = climateChange[deviceId]			// already have some edits underway
	} else {
		if (!climateChange) climateChange = [:]
		program = atomicState.program[deviceId]		// starting new edits
	}
	if (!program) {
		return false
	}
	
	String tempSensor = "${sensorId}:1"
	int c = 0
	int s = 0
	while ( program.climates[c] ) {	
		if (program.climates[c].climateRef == programId) {
			s = 0
			while (program.climates[c].sensors[s]) {
				if (program.climates[c].sensors[s].id == tempSensor ) {
					LOG("addSensorToProgram() - ${sensName} is already in the ${programId.capitalize()} program on ${statName} (${deviceId})",4,child,'info')
					return true	// mission accomplished - sensor is already in sensors list 
				}
				s++
			}
			def remoteSensors = atomicState.remoteSensors[deviceId]
			String sensorName = remoteSensors.find{it.id == sensorId}?.name
			program.climates[c].sensors << [id: tempSensor, name: sensorName] // add this sensor to the sensors list
			climateChange[deviceId] = program
			atomicState.climateChange = climateChange
			runIn(2, 'updateClimates', [data: [deviceId: deviceId]])
			LOG("addSensorToProgram() - ${sensName} addition to ${programId.capitalize()} program on thermostat ${statName} queued successfully",4,child,'info')
			return true
		}
		c++
	}
	LOG("addSensorToProgram() - couldn't find ${programId.capitalize()} program for thermostat ${statName}",2,child,'warn')
	return false
}
/////////////////////////////////////////////////////////////////////
boolean deleteSensorFromProgram(child, deviceId, sensorId, programId) {
	String statName = getThermostatName( deviceId )
	String sensName = child.device?.displayName
	LOG("deleteSensorFromProgram(${sensName},${statName},${sensorId},${programId})",2,child,'trace')
	
	def program
	def climateChange = atomicState.climateChange
	if (climateChange && climateChange[deviceId]) {
		program = climateChange[deviceId]			// already have some edits underway
	} else {
		if (!climateChange) climateChange = [:]
		program = atomicState.program[deviceId]		// starting new edits
	}
	if (!program) {
		return false
	}
	
	String tempSensor = "${sensorId}:1"
	int c = 0
	int s = 0
	while ( program.climates[c] ) {	
		if (program.climates[c].climateRef == programId) { // found the program we want to delete from
			def currentSensors = program.climates[c].sensors
			s = 0
			while (program.climates[c].sensors[s]) {
				if (program.climates[c].sensors[s].id == tempSensor ) {
					// found it, now we need to delete it - subtract it from the list of sensors					
					program.climates[c].sensors = program.climates[c].sensors - program.climates[c].sensors[s]	 
					climateChange[deviceId] = program
					atomicState.climateChange = climateChange		// save for later 
					runIn(2, 'updateClimates', [data: [deviceId: deviceId]])
					LOG("deleteSensorFromProgram() - ${sensName} deletion from ${programId.capitalize()} program on thermostat ${statName} queued successfully", 4, child, 'info')
					return true
				}
				s++
			}
		}
		c++
	}
	// didn't find the specified climate
	LOG("deleteSensorFromProgram() - Sensor ${sensName} not found in the ${programId.capitalize()} program on thermostat ${statName}", 2, child, 'warn') // didn't find sensor in the specified climate: Success!
	return true
}

boolean setProgramSetpoints(child, String deviceId, String programName, String heatingSetpoint, String coolingSetpoint) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setProgramSetpoints(${child}, ${deviceId}, ${heatingSetpoint} ${coolingSetpoint}", 2, child, 'warn')
		queueFailedCall('setProgramSetpoint', child.device.deviceNetworkId, 4, deviceId, programName, heatingSetpoint, coolingSetpoint)
		return false
	}
	
	LOG("setProgramSetpoints() for ${child} (${deviceId}: ${programName}, heatSP: ${heatingSetpoint}, coolSP: ${coolingSetpoint})", 2, child, 'trace')
	
	def program
	def climateChange = atomicState.climateChange
	if (climateChange && climateChange[deviceId]) {
		program = climateChange[deviceId]			// already have some edits underway
	} else {
		if (!climateChange) climateChange = [:]
		program = atomicState.program[deviceId]		// starting new edits
	}
	if (!program) {
		return false
	}
	
	// convert C temps to F
	def isMetric = (getTemperatureScale() == "C")
	// log.debug "hs: ${heatingSetpoint}, null? ${(heatingSetpoint != null)}, bigD? ${heatingSetpoint.isBigDecimal()}, *10: ${(heatingSetpoint * 10.0)}" // , ri: ${roundIt((heatingSetpoint * 10.0), 0)}"
	def ht = ((heatingSetpoint != '') && heatingSetpoint.isBigDecimal() ) ? (roundIt((isMetric ? (cToF(heatingSetpoint.toBigDecimal()) * 10.0) : (heatingSetpoint.toBigDecimal() * 10.0)), 0)) : null		// better precision using BigDecimal round-half-up
	def ct = ((coolingSetpoint != '') && coolingSetpoint.isBigDecimal() ) ? (roundIt((isMetric ? (cToF(coolingSetpoint.toBigDecimal()) * 10.0) : (coolingSetpoint.toBigDecimal() * 10.0)), 0)) : null
	// log.debug "ht: ${ht}, ct: ${ct}"
	
	// enforce the minimum delta
	def delta = atomicState.settings ? atomicState.settings[deviceId]?.heatCoolMinDelta : null
	if ((delta != null) && (ht != null) && (ct != null) && ((ct - ht) < delta)) {
		LOG("setProgramSetpoints() - Error: heating/cooling setpoints must be at least ${delta/10}°F apart.",1,child,'error')
		return false
	}
		
	int c = 0
	while ( program.climates[c] ) {	
		if (program.climates[c].name == programName) { // found the program we want to change
			def heatTemp = program.climates[c]?.heatTemp
			def coolTemp = program.climates[c]?.coolTemp
			def adjusted = ''
			// If we have both ht & ct, we already know the delta is good
			// but if we only have 1, we need to determine the valid value for the other
			if (!ct) {
				ct = (!coolTemp || ((coolTemp - ht) < delta)) ? (ht + delta) : coolTemp
				if(!coolingSetpoint || (ct != cooltemp)) adjusted = 'cool'
			} else if (!ht) {
				ht = (!heatTemp || ((ct - heatTemp) < delta)) ? (ct - delta) : heatTemp
				if (!heatingSetpoint || (ht != heatTemp)) adjusted = 'heat'
			}
			LOG("setProgramSetpoints() - heatingSetpoint: ${ht/10}°F ${adjusted=='heat'?'(adjusted)':''}, coolingSetpoint: ${ct/10}°F ${adjusted=='cool'?'(adjusted)':''}${adjusted?', minDelta: '+(delta/10).toString()+'°F':''}",2,child,'info')
			program.climates[c].heatTemp = ht
			program.climates[c].coolTemp = ct
			climateChange[deviceId] = program
			atomicState.climateChange = climateChange		// save for later 
			if (updateClimatesDirect( child, deviceId )) {
				LOG("setProgramSetpoints() for ${child} (${deviceId}): ${programName} setpoints change - Succeeded", 2, child, 'info')
				return true
			} else {
				LOG("setProgramSetpoints() for ${child} (${deviceId}): ${programName} setpoints change - Failed", 1, child, 'warn')
				// queue failed request
				return false
			}
		}
		c++
	}
	// didn't find the specified climate
	LOG("setProgramSetpoints(): ${programName} not found for ${child} (${deviceId})", 1, child, 'warn')
	return false
}

void updateClimates(data) {
	updateClimatesDirect(data.child, data.deviceId)
}

boolean updateClimatesDirect(child, deviceId) {
	if (child == null) {
		child = getChildDevice(getThermostatDNI(deviceId))
	}
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to updateClimateChangeDirect(${child}, ${deviceId})", 2, child, 'warn')
		queueFailedCall('updateClimatesDirect', child.device.deviceNetworkId, 1, deviceId)
		return false
	}
	
	LOG("Updating Program settings for ${child} (${deviceId})", 4, child, 'info')
	def climateChange = atomicState.climateChange
	def program
	if (climateChange && climateChange[deviceId]) program = climateChange[deviceId]
	def result
	if (program) {
		def programJson = JsonOutput.toJson(program)
		def thermostatSettings = ',"thermostat":{"program":' + programJson +'}'
		def thermostatFunctions = ''
		def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
		result = sendJson(child, jsonRequestBody)
		LOG("Updating Program settings for ${child} (${deviceId}) returned ${result}", 4, child, result?'info':'warn')
		if (result) {
			climateChange[deviceId] = null
			atomicState.climateChange = climateChange
		} else {
			if (atomicState.connected?.toString() != 'full') {
				LOG("API connection lost, queueing failed call to updateClimateChangeDirect(${child}, ${deviceId})", 2, child, 'warn')
				queueFailedCall('updateClimatesDirect', child.device.deviceNetworkId, 1, deviceId)
			}
		}
	}
	// atomicState.forcePoll = true
	runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	return result	 
}

boolean setEcobeeSetting(child, String deviceId, String name, String value) {
	if (child == null) {
		child = getChildDevice(getThermostatDNI(deviceId))
	}
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setEcobeeSetting(${child}, ${deviceId}, ${name} ${value}", 2, child, 'warn')
		queueFailedCall('setEcobeeSetting', child.device.deviceNetworkId, 3, deviceId, name, value)
		return false
	}
	name = name.trim()				// be a little lenient

	def dItem = EcobeeDirectSettings.find{ it.name == name }
	if (dItem != null) {
		// LOG("setEcobeeSetting() - Invalid command, use '${dItem.command}' to change '${name}'", 1, child, 'error')
		LOG("setEcobeeSetting( ${name}, ${value} ) - calling ${dItem.command}( ${value} )", 2, child, 'info')
		return "${dItem.command}"(child, value, deviceId)
		// return false
	}
	def sendValue = null
	boolean found = false
	if (EcobeeTempSettings.contains(name)) {
		// Is a temperature setting - need to convert to F*10 for Ecobee
		found = true
		def isMetric = (getTemperatureScale() == "C")
		sendValue = ((value != null) && value.isBigDecimal()) ? (roundIt((isMetric ? (cToF(value as BigDecimal) * 10.0) : ((value as BigDecimal) * 10.0)), 0)) : null
	} else if (EcobeeSettings.contains(name)) {
		found = true
		sendValue = value.trim()	// leniency is kindness
	} else if (EcobeeROSettings.contains(name)) {
		LOG("setEcobeeSetting(name: '${name}', value: '${value}') for ${child} (${deviceId}) - Setting is Read Only", 1, child, 'error')
		return false
	}
	if (sendValue == null) {
		if (!found) {
			LOG("setEcobeeSetting(name: '${name}', value: '${value}') for ${child} (${deviceId}) - Invalid name", 1, child, 'error')
		} else {
			LOG("setEcobeeSetting(name: '${name}', value: '${value}') for ${child} (${deviceId}) - Invalid value", 2, child, 'error')
		}
		return false
	}				  
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"'+name+'":"'+"${sendValue}"+'"}}}'  
	LOG("setEcobeeSetting() - Request Body: ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("setEcobeeSetting ${name} to ${sendValue} with result ${result}", 4, child, 'trace')
	if (result) {
		if (value == sendValue) {
			LOG("Ecobee Setting '${name}' for ${child} (${deviceId}) was successfully changed to '${value}'", 2, child, 'info')
		} else {
			LOG("Ecobee Setting '${name}' for ${child} (${deviceId}) was successfully changed to '${value}' ('${sendValue}')", 2, child, 'info')
		}
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
		return true
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setEcobeeSetting(${child}, ${deviceId})", 1, child, 'warn')
			queueFailedCall('setEcobeeSetting', child.device.deviceNetworkId, 3, deviceId, name, value)
		}
		return false
	}
	return false
}

// API Helper Functions
boolean sendJson(child=null, String jsonBody) {
	def debugLevelFour = false // debugLevel(4)
	if (debugLevelFour) LOG("sendJson() - ${jsonBody}",1,child,'debug')
	def returnStatus
	boolean result = false
	
	def cmdParams = [
			uri: apiEndpoint,
			path: "/1/thermostat",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
			body: jsonBody
	]
	
	// Just in case something goes wrong...
	atomicState.savedActionJsonBody = jsonBody
	atomicState.savedActionChild = child?.deviceNetworkId
	atomicState.action = "sendJsonRetry"
			
	try{
		httpPost(cmdParams) { resp ->
			returnStatus = resp.data?.status?.code

			if (debugLevelFour) LOG("sendJson() resp.status ${resp.status}, resp.data: ${resp.data}, returnStatus: ${returnStatus}", 1, child)
				
			// TODO: Perhaps add at least two tries incase the first one fails?
			if(resp.status == 200) {				
				if (debugLevelFour) LOG("Updated ${resp.data}", 1, child, 'trace')
				returnStatus = resp.data?.status?.code
				if (resp.data?.status?.code == 0) {
					if (debugLevelFour) LOG("Successful call to ecobee API.", 1, child, 'trace')
					result = true
					// Tell the children that we are once again connected to the Ecobee API Cloud
					if (apiConnected() != "full") {
						apiRestored()
						generateEventLocalParams() // Update the connection status
					}
				} else {
					LOG("Error return code = ${resp.data?.status?.code}", 1, child, "error")
				}
				// Reset saved state
				atomicState.savedActionJsonBody = null
				atomicState.savedActionChild = null
			} else {
				// Should never get here as a non-200 response is supposed to trigger an Exception
				LOG("Sent Json & got http status ${resp.status} - ${resp.status.code}", 2, child, "warn")
			} // resp.status if/else
		} // HttpPost
	} catch (groovyx.net.http.HttpResponseException e) {
		result = false // this thread failed...hopefully we can succeed after we refresh the auth_token
		LOG("sendJson() ${e.statusCode} ${e.response?.data?.status?.code}",1,null,"trace")
		if ((e.statusCode == 500) && (e.response?.data?.status?.code == 14)) {
			LOG("sendJson() - HttpResponseException occurred: Auth_token has expired", 1, null, "info")
			// atomicState.savedActionJsonBody = jsonBody
			// atomicState.savedActionChild = child.deviceNetworkId
			// atomicState.action = "sendJsonRetry"
			atomicState.action = ""					// we don't want refreshAuthToken to sendJsonRetry - we will retry ourselves instead
			LOG( "Refreshing your auth_token!", 4)
			if ( refreshAuthToken() ) { 
				LOG( "Sending: Auth_token refreshed", 2, null, 'info')
				if (!atomicState.sendJsonRetry) {
					atomicState.sendJsonRetry = true		// retry only once
					LOG( "sendJson() - Retrying once...", 2, null, 'info')
					result = sendJson( child, jsonBody )	// recursively re-attempt now that the token was refreshed
					LOG( "sendJson() - Retry ${result ? 'succeeded!' : 'failed.'}", 2, null, "${result ? 'info' : 'warn'}")
					atomicState.sendJsonRetry = false
				}
			} else {
				LOG( "Sending: Auth_token refresh failed", 1, null, 'error') 
			}
		} else {
			LOG("sendJson() - HttpResponseException occurred. Exception info: ${e} StatusCode: ${e.statusCode} || ${e.response?.data?.status?.code}", 1, null, "error")
		}
	// These appear to be transient errors, treat them all as if a Timeout...
	} catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException | javax.net.ssl.SSLPeerUnverifiedException | 
			 javax.net.ssl.SSLHandshakeException | java.net.SocketTimeoutException | java.net.UnknownHostException | java.net.UnknownHostException e) {
		LOG("sendJson() - ${e}.",1,null,'warn')	 // Just log it, and hope for better next time...
		if (apiConnected != 'warn') {
			atomicState.connected = 'warn'
			updateMyLabel()
			atomicState.lastPoll = now()
			atomicState.lastPollDate = getTimestamp()
			generateEventLocalParams()
		}
		// If no cached calls, retry 8 times
		// if cached calls already, or if retries failed, then queue the call
		//	  Cache Map by thermostat, order, (child & jsonBody)
		def inTimeoutRetry = atomicState.inTimeoutRetry
		if (inTimeoutRetry == null) inTimeoutRetry = 0
		if (inTimeoutRetry < 8) {
			// retry quickly...
			runIn(2, "sendJsonRetry", [overwrite: true])
		}
		atomicState.inTimeoutRetry = inTimeoutRetry + 1
		result = false
	} catch(Exception e) {
		// Might need to further break down 
		LOG("sendJson() - Exception: ${e}", 1, child, "error")
		result = false
		throw e
	}
	return result
}

boolean sendJsonRetry() {
	LOG("sendJsonRetry() called", 4)
	def child = null
	if (atomicState.savedActionChild) {
		child = getChildDevice(atomicState.savedActionChild)
	}
	
	if (child == null) {
		LOG("sendJsonRetry() - no savedActionChild!", 2, child, "warn")
		// return false
	}
	if (atomicState.savedActionJsonBody == null) {
		LOG("sendJsonRetry() - no saved JSON Body to send!", 2, child, "warn")
		return false
	}	   
	return sendJson(child, atomicState.savedActionJsonBody)
}

String getChildThermostatName() { return "Ecobee Suite Thermostat" }
String getChildSensorName()		{ return "Ecobee Suite Sensor" }
String getServerUrl()			{ return (isST) ? "https://graph.api.smartthings.com" : getFullApiServerUrl()}	// hubitat: /oauth/authorize}
String getShardUrl()			{ return (isST) ? getApiServerUrl() : getFullApiServerUrl()+"?access_token=${atomicState.accessToken}" }
String getCallbackUrl()			{ return (isST) ? "${serverUrl}/oauth/callback" : "https://cloud.hubitat.com/oauth/stateredirect" } // : */ "${serverUrl}/callback" } // &" + URLEncoder.encode("access_token", "UTF-8") + "=" + URLEncoder.encode(atomicState.accessToken, "UTF-8") } // #access_token=${atomicState.accessToken}" }
String getBuildRedirectUrl()	{ return (isST) ? "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${shardUrl}" : 
													   "${serverUrl}/oauth/stateredirect?access_token=${atomicState.accessToken}" }
String getStateUrl()			 { return "${getHubUID()}/apps/${app?.id}/callback?access_token=${atomicState?.accessToken}" }
String getApiEndpoint()			{ return "https://api.ecobee.com" }
String getInfo()				 { return 'info' }
String getWarn()				 { return 'warn' }
String getTrace()				 { return 'trace' }
String getDebug()				 { return 'debug' }
String getError()				 { return 'error' }

// This is the API Key from the Ecobee developer page. Can be provided by the app provider or use the appSettings
String getEcobeeApiKey() { 
	if (isHE) {
		if (atomicState.apiKey == null) atomicState.apiKey = "NOpc6i5ooiLLi1VPtVlJ0uv9Nh5cCfcc"		// Ecobee key for Ecobee Suite 1.7.** on Hubitat
		return "NOpc6i5ooiLLi1VPtVlJ0uv9Nh5cCfcc"
	} else if (!appSettings.clientId) {
		if (atomicState.initialized) {
			if (atomicState.apiKey == null) {
				return "obvlTjUuuR2zKpHR6nZMxHWugoi5eVtS"				// Original Ecobee key for Ecobee Connect && Ecobee Suite v1.0.0 -->1.6.**
			} else {
				return atomicState.apiKey
			}
		} else {
			atomicState.apiKey =	"EnJClRbJeT7DqPnlc29goR1hQvnV33tE"	// NEW Ecobee key for Ecobee Suite 1.7.** on SmartThings
			return					"EnJClRbJeT7DqPnlc29goR1hQvnV33tE"
		}
	} else {
		return appSettings.clientId 
	}
}

String getThermostatName(String tid) {
	// Get the name for this thermostat
	String DNI = (atomicState.isHE?'ecobee_suite-thermostat-':'') + ([ app.id, tid ].join('.'))
	def thermostatsWithNames = atomicState.thermostatsWithNames
	String tstatName = (thermostatsWithNames?.containsKey(DNI)) ? thermostatsWithNames[DNI] : ''
	if (tstatName == '') {
		tstatName = getChildDevice(DNI)?.displayName		// better than displaying 'null' as the name
	}
	return tstatName
}

String getThermostatDNI(String tid) {
	return (atomicState.isHE?'ecobee_suite-thermostat-':'') + ([app.id, tid].join('.'))
}

void LOG(message, level=3, child=null, String logType='debug', event=false, displayEvent=true) {
	def dbgLevel = debugLevel(level)
	if (!dbgLevel) return		// let's not waste CPU cycles if we don't have to...
	
	if (logType == null) logType = 'debug'
	def prefix = ""
	def logTypes = ['error', 'debug', 'info', 'trace', 'warn']
	
	if(!logTypes.contains(logType)) {
		log.error "LOG() - Received logType (${logType}) which is not in the list of allowed types ${logTypes}, message: ${message}, level: ${level}"
		if (event && child) { debugEventFromParent(child, "LOG() - Invalid logType ${logType}") }
		logType = 'debug'
	}
	
	if ( logType == 'error' ) { 
		atomicState.lastLOGerror = "${message} @ ${getTimestamp()}"
		atomicState.LastLOGerrorDate = getTimestamp()		 
	}
	// if ( debugLevel(0) ) { return }
	if ( debugLevel(5) ) { prefix = 'LOG: ' }
	//if ( dbgLevel ) { 
		log."${logType}" "${prefix}${message}"		  
		if (event) { debugEvent(message, displayEvent) }
		if (child) { debugEventFromParent(child, message) }
	//}	   
}

void debugEvent(message, displayEvent = false) {
	def results = [
		name: 'appdebug',
		descriptionText: message,
		displayed: displayEvent
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent (results)
}

void debugEventFromParent(child, message) {
	 def data = [
				debugEventFromParent: message
			]		  
	if (child) { child.generateEvent(data) }
}

// TODO: Create a more generic push capability to send notifications
// send both push notification and mobile activity feeds
void sendPushAndFeeds(notificationMessage) {
	LOG("Notification Message: ${notificationMessage}", 2, null, "trace")
	LOG("Notification Time: ${atomicState.timeSendPush}", 2, null, "info")
	
	// notification is sent to remind user no more than once per hour
	boolean sendNotification = (atomicState.timeSendPush && ((now() - atomicState.timeSendPush) < 3600000)) ? false : true
	if (sendNotification) {
		String msg = "Your ${location.name} Ecobee${settings.thermostats.size()>1?'s':''} " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (atomicState.isST) {
			if (phone) { // check that the user did select a phone number
				if ( phone.indexOf(";") > 0){
					def phones = settings.phone.split(";")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i].trim(), msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${phone}", 3, null, 'info')
					sendSmsMessage(phone.trim(), msg)						// Only to SMS contact
				}
			} 
			if (settings.pushNotify) {
				LOG("Sending Push to everyone", 3, null, 'warn')
				sendPushMessage(msg)								// Push to everyone
			}
			if (settings.speak) {
				if (settings.speechDevices != null) {
					settings.speechDevices.each {
						it.speak( "From " + msg )
					}
				}
				if (settings.musicDevices != null) {
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText( "From " + msg )
					}
				}
			}
			sendNotificationEvent( notificationMessage )			// Always send to hello home
		} else {		// isHE
			if (settings.notifiers != null) {
				settings.notifiers.each {							// Use notification devices on Hubitat
					it.deviceNotification(msg)
				}
			}
			if (settings.phone != null) {
				if ( phone.indexOf(",") > 0){
					def phones = phone.split(",")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i].trim(), msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${phone}", 3, null, 'info')
					sendSmsMessage(phone.trim(), msg)						// Only to SMS contact
				}
			}
			if (settings.speak) {
				if (settings.speechDevices != null) {
					settings.speechDevices.each {
						it.speak( "From " + msg )
					}
				}
				if (settings.musicDevices != null) {
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText( "From " + msg )
					}
				}
			}
			sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
		}		
		atomicState.timeSendPush = now()
	}
}

void sendActivityFeeds(notificationMessage) {
	def devices = getChildDevices()
	devices.each { child ->
		child.generateActivityFeedsEvent(notificationMessage) //parse received message from parent
	}
}

// Helper Functions
// Creating my own as it seems that the built-in version only works for a device, NOT a SmartApp
def myConvertTemperatureIfNeeded(scaledSensorValue, cmdScale, precision) {
	if (scaledSensorValue == null) {
		LOG("Illegal sensorValue (null)", 2, null, "error")
		return null
	}
	if ( (cmdScale != "F") && (cmdScale != "C") && (cmdScale != "dC") && (cmdScale != "dF") ) {
		// We do not have a valid Scale input, throw a debug error into the logs and just return the passed in value
		LOG("Invalid temp scale used: ${cmdScale}", 2, null, "error")
		return roundIt(scaledSensorValue, precision)
	}

	def returnSensorValue 
	
	// Normalize the input
	if (cmdScale == "dF") { cmdScale = "F" }
	if (cmdScale == "dC") { cmdScale = "C" }

	LOG("About to convert/scale temp: ${scaledSensorValue}", 5, null, "trace", false)
	if (cmdScale == getTemperatureScale() ) {
		// The platform scale is the same as the current value scale
		returnSensorValue = roundIt(scaledSensorValue, precision)
	} else if (cmdScale == "F") {				
		returnSensorValue = roundIt(fToC(scaledSensorValue), precision)
	} else {
		returnSensorValue = roundIt(cToF(scaledSensorValue), precision)
	}
	LOG("returnSensorValue == ${returnSensorValue}", 5, null, "trace", false)
	return returnSensorValue
}
def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def roundIt( BigDecimal value, decimals=0) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
boolean wantMetric() {
	return (getTemperatureScale() == "C")
}
def cToF(temp) {
	if (debugLevel(5)) LOG("cToF entered with ${temp}", 5, null, "info")
	if (temp) { return (temp * 1.8 + 32) } else { return null } 
	// return celsiusToFahrenheit(temp)
}
def fToC(temp) {	
	if (debugLevel(5)) LOG("fToC entered with ${temp}", 5, null, "info")
	if (temp) { return (temp - 32) / 1.8 } else { return null } 
	// return fahrenheitToCelsius(temp)
}

// Establish the minimum amount of time to wait to do another poll
def getMinMinBtwPolls() {
	// TODO: Make this configurable in the SmartApp
	return 1
}

def toJson(Map m) {
	return groovy.json.JsonOutput.toJson(m)
}

// need these next 5 get routines because settings variable defaults aren't set unless the "Preferences" page is actually opened/rendered
Integer getPollingInterval() {
	return (settings?.pollingInterval?.isNumber() ? settings.pollingInterval as Integer : 5)
}

Integer getTempDecimals() {
	return ( settings?.tempDecimals?.isNumber() ? settings.tempDecimals as Integer : (wantMetric() ? 1 : 0))
}

Integer getDebugLevel() {
	return ( settings?.debugLevel?.isNumber() ? settings.debugLevel as Integer : 3)
}

String getHoldType() {
	return settings.holdType ?: 'Until I Change'
}

Integer getHoldHours() {
	if (settings.holdType) {
		if (settings.holdType == 'Specified Hours') return ((settings.holdHours != null) && settings.holdHours?.isNumber() ? settings.holdHours as Integer : 2)
		else if (settings.holdType == '2 Hours') return 2
		else if (settings.holdType == '4 Hours') return 4
		else return 2
	}
}

String getTimestamp() {
	// There seems to be some possibility that the timeZone will not be returned and will cause a NULL Pointer Exception
	def timeZone = location?.timeZone ? location.timeZone : ""
	// LOG("timeZone found as ${timeZone}", 5)
	if (timeZone == "") {
		return new Date().format("yyyy-MM-dd HH:mm:ss z")
	} else {
		return new Date().format("yyyy-MM-dd HH:mm:ss z", timeZone)
	}
}

String getTimeOfDay(tid = null) {
	def nowTime 
	if(atomicState.timeZone) {
		nowTime = new Date().format("HHmm", TimeZone.getTimeZone(atomicState.timeZone)).toInteger()
	} else if (tid && atomicState.statLocation && atomicState.statLocation[tid]) {
		nowTime = new Date().format("HHmm", TimeZone.getTimeZone(atomicState.statLocation[tid].timeZone)).toInteger()
	} else {
		nowTime = new Date().format("HHmm").toInteger()
	}
	if (debugLevel(4)) LOG("getTimeOfDay() - nowTime = ${nowTime}", 1, null, "trace")
	if ( (nowTime < atomicState.sunriseTime) || (nowTime > atomicState.sunsetTime) ) {
		return "night"
	} else {
		return "day"
	}
}

// we'd prefer to use the timeZone that the thermostat says it is in, so long as ALL the thermostats agree
// if thermostats don't have a timeZone, use the SmartThings location.timeZone
// if ST location doesn't have a time zone, we're just going to have to use ST's "local time"
String getTimeZone() {
	def updatesLog = atomicState.updatesLog
	if ((atomicState.timeZone == null) || updatesLog.forcePoll) {
		// default to the SmartThings location's timeZone (if there is one)
		def myTimeZone = location?.timeZone ? location.timeZone.ID : null
		def timeZones = []
		settings.thermostats?.each {
			def tid = it.split(/\./).last()
			def statTimeZone = (atomicState.statLocation && atomicState.statLocation[tid]) ? atomicState.statLocation[tid].timeZone : null
			if (statTimeZone != null) {
				if (debugLevel(4)) LOG("thermostat ${tid}'s timeZone ID: ${statTimeZone}",4,null,'trace')
				// let's see how many timeZones we are using across all the thermostats
				if (!timeZones || (!timeZones.contains(statTimeZone))) timeZones += [statTimeZone]
				// if we have the Thermostat Location, use the timeZone from the thermostat
				if (myTimeZone != statTimeZone) myTimeZone = statTimeZone
			}
		}
		if (timeZones.size() != 1) {
			// we have thermostats in more than one time zone - going to have to use location data every time
			myTimeZone = null
		}
		if (myTimeZone != null) {
			atomicState.timeZone = myTimeZone		// can't save the timeZone object, so we store the ID/Name
			return myTimeZone
		}
	}
	return atomicState.timeZone
}

String getZipCode() {
	// default to the SmartThings location's timeZone (if there is one)
	String myZipCode = location?.zipCode
	if (myZipCode == null) {
		LOG("*** INITIALIZATION ERROR *** PLEASE SET POSTAL CODE FOR LOCATION '${location.name}'",1,null,'warn')
		atomicState.zipCode == null
	}
	def updatesLog = atomicState.updatesLog
	if ((atomicState.zipCode == null) || updatesLog.forcePoll) {
		def zipCodes = []
		settings.thermostats?.each{
			String tid = it.split(/\./).last()
			String statZipCode = (atomicState.statLocation && atomicState.statLocation[tid]) ? atomicState.statLocation[tid].postalCode : ''
			if ((statZipCode != '') && ((statZipCode.isNumber() && (statZipCode.toInteger() != 0)) || !statZipCode.isNumber())) { // allow non-numeric zip codes from Ecobee API
				// let's see how many postalCodes we are using across all the thermostats
				if (!zipCodes || (!zipCodes.contains(statZipCode))) zipCodes += [statZipCode]
				// if we have the Thermostat Location, use the postalCode from the thermostat
				if (myZipCode != statZipCode) myZipCode = statZipCode
			}
		}
		if (zipCodes.size() > 1) {
			// we have thermostats in more than one postalCode - going to have to use location data every time
			myZipCode = null
		} 
	}
	atomicState.zipCode = myZipCode
	LOG("getZipCode() returning ${myZipCode}",3,null,'info')
	return myZipCode
}

// Are we connected with the Ecobee service?
String apiConnected() {
	// values can be "full", "warn", "lost"
	if (atomicState.connected == null) { atomicState.connected = "warn"; updateMyLabel(); }
	return atomicState.connected?.toString() ?: "lost"
}

void apiRestored() {
	atomicState.connected = "full"
	updateMyLabel()
	unschedule("notifyApiLost")
	atomicState.reAttemptPoll = 0
	runIn(10, runCallQueue)
}

def getDebugDump() {
	 def debugParams = [when:"${getTimestamp()}", whenEpic:"${now()}", 
				lastPollDate:"${atomicState.lastPollDate}", lastScheduledPollDate:"${atomicState.lastScheduledPollDate}", 
				lastScheduledWatchdogDate:"${atomicState.lastScheduledWatchdogDate}",
				lastTokenRefreshDate:"${atomicState.lastTokenRefreshDate}", 
				initializedEpic:"${atomicState.initializedEpic}", initializedDate:"${atomicState.initializedDate}",
				lastLOGerror:"${atomicState.lastLOGerror}", authTokenExpires:"${atomicState.authTokenExpires}"
			]	 
	return debugParams
}

void apiLost(where = "[where not specified]") {
	boolean ST = atomicState.isST
	
	LOG("apiLost() - ${where}: Lost connection with APIs. unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with Ecobee by running Ecobee Suite Manager ${ST?'Smart':''}App and logging in again", 1, null, "error")
	atomicState.apiLostDump = getDebugDump()
	if (apiConnected() == "lost") {
		LOG("apiLost() - already in lost atomicState. Nothing else to do. (where= ${where})", 5, null, "trace")
		return
	}
   
	// provide cleanup steps when API Connection is lost
	def notificationMessage = "${settings.thermostats.size()>1?'are':'is'} disconnected from ${ST?'SmartThings':'Hubitat'}/Ecobee, because the access credential changed or was lost. Please go to the Ecobee Suite Manager ${ST?'Smart':''}App and re-enter your account login credentials."
	atomicState.connected = "lost"
	updateMyLabel()
	atomicState.authToken = null
	
	sendPushAndFeeds(notificationMessage)
	generateEventLocalParams()

	LOG("Unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with Ecobee by running the Ecobee Suite Manager ${ST?'Smart':''}App and logging in again", 0, null, "error")
	
	// Notify each child that we lost so it gets logged
	if ( debugLevel(3) ) {
		def d = getChildDevices()
		d?.each { oneChild ->
			LOG("apiLost() - notifying each child: ${oneChild.device.displayName} of loss", 1, child, "error")
		}
	}
	unschedule("pollScheduled")
	unschedule("scheduleWatchdog")
	runEvery3Hours("notifyApiLost")
}

void notifyApiLost() {
	boolean ST = atomicState.isST
	
	def notificationMessage = "${settings.thermostats.size()>1?'are':'is'} disconnected from ${ST?'SmartThings':'Hubitat'}/Ecobee. Please go to the Ecobee Suite Manager and re-enter your Ecobee account login credentials."
	if ( atomicState.connected == "lost" ) {
		generateEventLocalParams()
		sendPushAndFeeds(notificationMessage)
		LOG("notifyApiLost() - API Connection Previously Lost. User MUST reintialize the connection with Ecobee by running the Ecobee Suite Manager ${ST?'Smart':''}App and logging in again", 0, null, "error")
	} else {
		// Must have restored connection
		unschedule("notifyApiLost")
	}	 
}

String childType(child) {
	// Determine child type (Thermostat or Remote Sensor)
	if ( child.hasCapability("Thermostat") ) { return getChildThermostatName() }
	if ( child.name.contains( getChildSensorName() ) ) { return getChildSensorName() }
	return "Unknown"
}

def getFanMinOnTime(child) {
	if (debugLevel(4)) LOG("getFanMinOnTime() - Looking up current fanMinOnTime for ${child.device?.displayName}", 1, child)
	String devId = getChildThermostatDeviceIdsString(child)
	LOG("getFanMinOnTime() Looking for ecobee thermostat ${devId}", 5, child, "trace")
	
	def fanMinOnTime = atomicState.settings[devId]?.fanMinOnTime
	if (debugLevel(4)) LOG("getFanMinOnTime() fanMinOnTime is ${fanMinOnTime} for therm ${devId}", 1, child)
	return fanMinOnTime
}

String getHVACMode(child) {
	if (debugLevel(4)) LOG("Looking up current hvacMode for ${child.device?.displayName}", 1, child)
	String devId = getChildThermostatDeviceIdsString(child)
	// LOG("getHVACMode() Looking for ecobee thermostat ${devId}", 5, child, "trace")
	
	def hvacMode = atomicState.settings[devId].hvacMode		// FIXME
	if (debugLevel(4)) LOG("getHVACMode() hvacMode is ${hvacMode} for therm ${devId}", 1, child)
	return hvacMode
}

def getAvailablePrograms(thermostat) {
	// TODO: Finish implementing this!
	if (debugLevel(4)) LOG("Looking up the available Programs for this thermostat (${thermostat})", 4)
	String devId = getChildThermostatDeviceIdsString(thermostat)
	// LOG("getAvailablePrograms() Looking for ecobee thermostat ${devId}", 5, thermostat, "trace")

	def climates = atomicState.program[devId]?.climates
	
	return climates?.collect { it.name }
}

boolean debugLevel(level=3) {
	Integer dbgLevel = getDebugLevel()
	return (dbgLevel == 0) ? false : (dbgLevel >= level?.toInteger())
}

String fixDateTimeString( String dateStr, String timeStr, String thermostatTime) {
	// date is in ecobee format: YYYY-MM-DD or null (meaning "today")
	// time is in ecobee format: HH:MM:SS
	def today = new Date().parse('yyyy-MM-dd', thermostatTime)
	def target = new Date().parse('yyyy-MM-dd', dateStr)
	
	String resultStr = ''
	String myDate = ''
	String myTime = ''
	boolean showTime = true
	
	if (target == today) {
		myDate = 'today'	
	} else if (target == today-1) {
		myDate = 'yesterday'
	} else if (target == today+1) {
		myDate = 'tomorrow'
	} else if (dateStr == '2035-01-01' ) {		// to Infinity
		myDate = 'a long time from now'
		showTime = false
	} else {
		myDate = 'on '+target.format('MM-dd')
	}	 
	if (showTime) {
		myTime = new Date().parse('HH:mm:ss',timeStr).format('h:mma').toLowerCase()
	}
	if (myDate || myTime) {
		resultStr = myTime ? "${myDate} at ${myTime}" : "${myDate}"
	}
	return resultStr
}

String getDeviceId(String networkId) {
	// def deviceId = networkId.split(/\./).last()	
	// LOG("getDeviceId() returning ${deviceId}", 4, null, 'trace')
	// return deviceId
	return networkId.split(/\./).last()
}

// Reservation Management Functions
// Make a reservation for me
void makeReservation( String statId, String childId, String type='modeOff' ) {
	String childName = getChildAppName( childId )
	if (!childName) {
		LOG("Illegal reservation attempt using childId: ${childId} - caller is not my child.",1,null,'warn')
		return
	}
	def reservations = atomicState.reservations
	if (!reservations) reservations = [:]
	if (!reservations?.containsKey(statId)) {
		reservations."${statId}" = [:]
	}
	if (!reservations."${statId}"?.containsKey(type)) {				// allow for ANY type of reservations
		reservations."${statId}"."${type}" = []
	}
	if (!reservations."${statId}"."${type}"?.contains(childId)) {
		reservations."${statId}"."${type}" << childId
		atomicState.reservations = reservations
		LOG("'${type}' reservation created for ${childName} (${childId})",2,null,'info')
	} else {
		LOG("'${type}' reservation already exists for ${childName} (${childId})",2,null,'info')
	}
}
// Cancel my reservation
void cancelReservation( String statId, String childId, String type='modeOff') {
	String childName = getChildAppName( childId )
	if (!childName) childName = childId
	def reservations = atomicState.reservations
	if (reservations?."${statId}"?."${type}"?.contains(childId)) {
		reservations."${statId}"."${type}" = reservations."${statId}"."${type}" - [childId]
		atomicState.reservations = reservations
		LOG("'${type}' reservation cancelled for ${childName} (${childId})",2,null,'info')
	} else {
		LOG("'${type}' reservation doesn't exist for ${childName} (${childId})",2,null,'info')
	}
}
// Do I have a reservation?
boolean haveReservation( String statId, String childId, String type='modeOff') {
	String childName = getChildAppName( childId )
	def reservations = atomicState.reservations
	boolean result = (reservations?."${statId}"?."${type}"?.contains(childId))
	LOG("${childName} (${childId}) does ${result?'':'not '} hold a '${type}' reservation",2,null,'info')
	return result
}
// Do any Apps have reservations?
boolean anyReservations( String statId, String type='modeOff') {
	def reservations = atomicState.reservations
	boolean result = ((reservations?."${statId}"?.containsKey(type)) && (reservations."${statId}"."${type}" != [])) ? (reservations."${statId}"."${type}".size() != 0) : false
	LOG("${result?'Somebody':'Nobody'} holds '${type}' reservations",2,null,'info')
	return result
}
// How many apps have reservations?
Integer countReservations(String statId, String type='modeOff') {
	def reservations = atomicState.reservations
	Integer result = (((reservations?."${statId}"?.containsKey(type)) && (reservations."${statId}"."${type}" != [])) ? reservations."${statId}"."${type}".size() : 0)
	LOG("There ${result>1?'are':'is'} ${result} '${type}' reservation${result>1?'s':''}",2,null,'info')
	return result
}
// Get the list of app IDs that have reservations
List getReservations(String statId, String type='modeOff') {
	def reservations = atomicState.reservations
	def result = ((reservations?."${statId}"?.containsKey(type)) && (reservations."${statId}"."${type}" != [])) ? reservations."${statId}"."${type}" : []
	LOG("appIds holding '${type}' reservations: ${result}",2,null,'info')
	return result
}
// Get the list of app Names that have reservations
List getGuestList(String statId, String type='modeOff') {
	def result = []
	def reservations = atomicState.reservations
	if ((reservations?."${statId}"?.containsKey(type)) && (reservations."${statId}"."${type}" != [])) {
		reservations."${statId}"."${type}".each {
			result << getChildAppName( it )
		}
	}
	LOG("Apps holding '${type}' reservations: ${result.toString()[1..-2]}",2,null,'info')
	return result
}

void runEvery2Minutes(handler) {
	Random rand = new Random()
	//log.debug "Random2: ${rand}"
	int randomSeconds = rand.nextInt(59)
	schedule("${randomSeconds} 0/2 * * * ?", handler)
}

void runEvery3Minutes(handler) {
	Random rand = new Random()
	//log.debug "Random3: ${rand}"
	int randomSeconds = rand.nextInt(59)
	schedule("${randomSeconds} 0/3 * * * ?", handler)
}

// Alert settings
@Field final List alertNamesList =		['auxOutdoorTempAlertNotify','auxOutdoorTempAlertNotifyTechnician','auxRuntimeAlertNotifyTechnician','auxRuntimeAlert','auxRuntimeAlertNotify',
										 'coldTempAlertEnabled','disableAlertsOnIdt','disableHeatPumpAlerts','hotTempAlertEnabled','humidityAlertNotify',
										 'humidityHighAlert','humidityLowAlert','randomStartDelayCool','randomStartDelayHeat','tempAlertNotify','ventilatorOffDateTime','wifiOfflineAlert'
										]
// Settings (attributes)
@Field final List settingsNamesList =	['autoAway','backlightOffDuringSleep','backlightOffTime','backlightOnIntensity','backlightSleepIntensity','coldTempAlertEnabled','compressorProtectionMinTime','condensationAvoid',
										 'coolingLockout','dehumidifyWhenHeating','dehumidifyWithAC','disablePreCooling','disablePreHeating','drAccept','eiLocation','electricityBillCycleMonths','electricityBillStartMonth',
										 'electricityBillingDayOfMonth','enableElectricityBillAlert','enableProjectedElectricityBillAlert','fanControlRequired','followMeComfort','groupName','groupRef','groupSetting','hasBoiler',
										 /*hasDehumidifier,*/'hasElectric','hasErv','hasForcedAir','hasHeatPump','hasHrv','hasUVFilter','heatPumpGroundWater','heatPumpReversalOnCool','holdAction','humidityAlertNotify',
										 'humidityAlertNotifyTechnician','humidityHighAlert','humidityLowAlert','installerCodeRequired','isRentalProperty','isVentilatorTimerOn','lastServiceDate','locale','monthlyElectricityBillLimit',
										 'monthsBetweenService','remindMeDate','serviceRemindMe','serviceRemindTechnician','smartCirculation','soundAlertVolume','soundTickVolume','stage1CoolingDissipationTime',
										 'stage1HeatingDissipationTime','tempAlertNotifyTechnician','userAccessCode','userAccessSetting','ventilatorDehumidify','ventilatorFreeCooling','ventilatorMinOnTimeAway',
										 'ventilatorMinOnTimeHome','ventilatorOffDateTime','ventilatorType'
										]
// Temperature Settings
@Field final List tempSettingsList =	['auxMaxOutdoorTemp','auxOutdoorTempAlert','coldTempAlert','compressorProtectionMinTemp','compressorProtectionMinTemp','coolMaxTemp','coolMinTemp',
										 'dehumidifyOvercoolOffset','heatMaxTemp','heatMinTemp','hotTempAlert','maxSetBack','maxSetForward','quickSaveSetBack','quickSaveSetForward','stage1CoolingDifferentialTemp',
										 'stage1HeatingDifferentialTemp','tempCorrection'
										]
// Settings that require Temperature Conversion (callers use native C/F temps)
@Field final List EcobeeTempSettings =	['auxMaxOutdoorTemp','auxOutdoorTempAlert','compressorProtectionMinTemp','coldTempAlert','coolRangeHigh','coolRangeLow','dehumidifyOvercoolOffset',
										 'heatCoolMinDelta','heatRangeHigh','heatRangeLow','hotTempAlert','maxSetBack','maxSetForward','quickSaveSetBack','quickSaveSetForward','stage1CoolingDifferentialTemp',
										 'stage1HeatingDifferentialTemp','tempCorrection'
										]
// Settings that are passed directly as Strings (including numbers and logicals)
@Field final List EcobeeSettings =		['autoAway','auxOutdoorTempAlertNotify','auxOutdoorTempAlertNotifyTechnician','auxRuntimeAlert','auxRuntimeAlertNotifyTechnician','auxRuntimeAlertNotify','backlightOffDuringSleep',
										 'backlightOffTime','backlightOnIntensity','backlightSleepIntensity','coldTempAlertEnabled','compressorProtectionMinTime','condensationAvoid','coolingLockout','dehumidifyWhenHeating',
										 'dehumidifyWithAC','disableAlertsOnIdt','disableHeatPumpAlerts','disablePreCooling','disablePreHeating','drAccept','eiLocation','electricityBillCycleMonths','electricityBillStartMonth',
										 'electricityBillingDayOfMonth','enableElectricityBillAlert','enableProjectedElectricityBillAlert','fanControlRequired','followMeComfort','groupName','groupRef','groupSetting',
										 'heatPumpReversalOnCool','holdAction','hotTempAlertEnabled','humidityAlertNotify','humidityAlertNotifyTechnician','humidityHighAlert','humidityLowAlert','installerCodeRequired',
										 'isRentalProperty','isVentilatorTimerOn','lastServiceDate','locale','monthlyElectricityBillLimit','monthsBetweenService','remindMeDate','serviceRemindMe','serviceRemindTechnician',
										 'smartCirculation','soundAlertVolume','soundTickVolume','stage1CoolingDissipationTime','stage1HeatingDissipationTime','tempAlertNotifyTechnician','ventilatorDehumidify','ventilatorFreeCooling',
										 'ventilatorMinOnTimeAway','ventilatorMinOnTimeHome','ventilatorOffDateTime', 'ventilatorType'
										]
// Settings that are Read Only and cannot be changed directly
@Field final List EcobeeROSettings =	['coolMaxTemp','coolMinTemp','coolStages','hasBoiler','hasDehumidifier','hasElectric','hasErv','hasForcedAir','hasHeatPump','hasHrv','hasHumidifier','hasUVFilter','heatMaxTemp','heatMinTemp',
										 'heatPumpGroundWater','heatStages','userAccessCode','userAccessSetting','temperature','humidity','currentProgram','currentProgramName','currentProgramId','currentProgramOwner',
										 'currentProgramType','scheduledProgram','scheduledProgramName','scheduledProgramId','scheduledProgramOwner','scheduledProgramType','debugLevel','decimalPrecision',
										]
// Settings that must be changed only by specific commands
@Field final List EcobeeDirectSettings= [
											[name: 'fanMinOnTime',			command: 'setFanMinOnTime'],
											[name: 'dehumidifierMode',		command: 'setDehumidifierMode'],
											[name: 'dehumidifierLevel',		command: 'setDehumiditySetpoint'],
											[name: 'dehumidityLevel',		command: 'setDehumiditySetpoint'],
											[name: 'dehumiditySetpoint',	command: 'setDehumiditySetpoint'],
											[name: 'humidifierMode',		command: 'setHumidifierMode'],
											[name: 'humiditySetpoint',		command: 'setHumiditySetpoint'],
										]
@Field final List EcobeeDeviceInfo =	[ 'brand','features','identifier','isRegistered','modelNumber','name'] // 'lastModified'

@Field final List RuntimeValueNames =	['actualHumidity','actualTemperature','connected','desiredCool','desiredDehumidity','desiredFanMode','desiredHeat','desiredHumidity','disconnectDateTime']
									  // ,'desiredHeatRange','desiredCoolRange','rawTemperature','showIconMode'

void updateMyLabel() {
	if (isST) return								// ST doesn't support the colored label approach
	
	// Display Ecobee connection status as part of the label...
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!myLabel.contains('<span')) atomicState.appDisplayName = myLabel
	} 
	if (myLabel.contains('<span')) {
		// strip off any connection status tag
		myLabel = myLabel.substring(0, myLabel.indexOf('<span'))
		atomicState.appDisplayName = myLabel
	}
	String newLabel
	String key = atomicState.wifiAlert ? 'wifi' : atomicState.connected?.toString()
	switch (key) {
		case 'full':
			newLabel = myLabel + "<span style=\"color:green\"> Online</span>"
			break;
		case 'warn':
			newLabel = myLabel + "<span style=\"color:orange\"> Warning</span>"
			break;
		case 'lost':
			newLabel = myLabel + "<span style=\"color:red\"> Offline</span>"
			break;
		case 'wifi':
			newLabel = myLabel +  "<span style=\"color:orange\"> Check Wifi</span>"
			break;
		default:
			newLabel = myLabel
			break;
	}
	if (newLabel && (app.label != newLabel)) app.updateLabel(newLabel)
}
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
String	getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST()	  { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
boolean getIsHE()	  { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...

String getHubPlatform() {
	def pf = getPlatform()
	atomicState?.hubPlatform = pf			// if (atomicState.hubPlatform == 'Hubitat') ... 
											// or if (state.hubPlatform == 'SmartThings')...
	atomicState?.isST = pf.startsWith('S')	// if (atomicState.isST) ...
	atomicState?.isHE = pf.startsWith('H')	// if (atomicState.isHE) ...
	return pf
}
boolean getIsSTHub() { return atomicState.isST }					// if (isSTHub) ...
boolean getIsHEHub() { return atomicState.isHE }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"	
}
