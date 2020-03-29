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
 *	Updates by Barry A. Burke (storageanarchy@gmail.com) 2016 - 2020
 *
 *	See Github Changelog for complete change history
 *	<snip>
 *	1.7.26 - Fixed pauseSwitch initialization error
 *	1.7.27 - Enabled "Demand Response" program
 *	1.7.28 - Fixed weatherIcon change at sunrise/sunset & demandResponse currentProgramName/Id
 *	1.7.29 - Fixed null owner & type references for Auto Home/Away programs
 *	1.7.30 - Fixed null values during Hub reboot recovery
 *	1.7.31 - Optimized LOG() handling, fanMode/fanMinOnTime tweaks
 *	1.7.32 - Send programsList to sensors, to support add/deleteSensorFromProgram
 *	1.7.33 - Added notification device support on SmartThings
 *	1.7.34 - Added Audio object settings (for Ecobee 4)
 *	1.7.35 - Cleaned up initialize redundancy
 *	1.7.36 - Fixed child.label/child.name
 *	1.7.37 - If Auto mode is disabled, don't enforce heatCoolMinDelta or heatingSetpoint can't be higher than coolingSetpoint in setPrgramSetpoints()
 *	1.7.38 - Added queued requests to climateChange actions (setProgramSetpoints, addSensorToProgram, deleteSensorFromProgram)
 *	1.7.39 - Fixed type conversion error in setProgramSetpoints with null setpoint
 *	1.7.40 - Miscellaneous Map/HashMap optimizations
 *	1.7.41 - Revamped Smart Recovery, so that the Smart Vents Helper knows what the Recovery target is
 *	1.7.42 - Added weatherTempLowForecast, for the new Smart Humidity Helper
 *	1.7.43 - Added nextProgramName (for Smart Vents), fixed Fan Holds
 *	1.7.44 - Optimized myConvertTemperatureIfNeeded()
 *	1.7.45 - Fixed labels (again), cleaned up settings pages, cleaned up child logging
 *	1.7.46 - Release memory when finished, extended http request timeouts to 30 seconds
 *	1.7.47 - Added infrastructure for one child app to pause/unpause another
 *	1.7.48 - Broke program Map into currentClimateRef, climates and schedule Maps
 *	1.7.49 - Fixed runCallQueue ('setProgramSetpoint-s-' typo)
 *	1.7.50 - Added schedule conversions to/from new timeSchedule Map
 *	1.7.51 - Added ***Updated timestamps; track updates by Tid; 
 *	1.7.52 - Added managementSet support for EMS thermostats
 *	1.8.00 - Version synchronization, new Smart Humidity Helper, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - Fixed smartAuto settings bug
 *	1.8.03 - Updated WARNING & NOTE: formatting
 *	1.8.04 - Fix holdEndDate during transitions; supportedThermostatModes initialization loophole
 *	1.8.05 - Fix attribute initialization logic
 *	1.8.06 - Fix thermostatAsSensor selection page
 *	1.8.07 - Improve http error handling
 *	1.8.08 - More busy bees
 *	1.8.09 - Reworked stat.generateEvent()
 *	1.8.10 - Optimized & hardened initialize()
 *	1.8.11 - More optimizations
 *	1.1.12 - Even more optimizations
 *	1.8.13 - Send simultaneous notification Announcements to multiple Echo Speaks devices
 *	1.8.14 - Notification devices are no longer required (but still highly recommended)
 *	1.8.15 - New SHPL, using Global Fields instead of atomicState
 *	1.8.16 - Tightened up httpGet/httpPost response handling
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.8.16" }
String getVersionLabel()	{ return "Ecobee Suite Manager, version ${getVersionNum()} on ${getHubPlatform()}" }
String getMyNamespace()		{ return "sandood" }

@Field final boolean TIMERS = false

def getHelperSmartApps() {
	String mrpTitle = ST ? 'New Mode/Routine/Switches/Program Helper...' : 'New Mode/Switches/Program Helper...'
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
		[name: "ecobeeHumidChild", appName: "ecobee Suite Smart Humidity",
			namespace: myNamespace, multiple: true,
			title: "New Smart Humidity Helper..."],
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
	name:				"Ecobee Suite Manager",
	namespace:			myNamespace,
	author:				"Barry A. Burke (storageanarchy@gmail.com)",
	description:		"Connect your Ecobee thermostats and sensors to ${isST?'SmartThings':'Hubitat'}, along with a Suite of Helper ${isST?'Smart':''}Apps.",
	category:			"My Apps",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-manager.src/ecobee-suite-manager.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md",
	singleInstance: 	true,
	oauth:				true,
	pausable:			false
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
	String version = getVersionLabel()
	atomicState.inSetup = true
	def deviceHandlersInstalled 
	def readyToInstall
	//boolean ST = isST	// these are now "global" variables
	//boolean HE = !ST
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
	if (atomicState.initialized) { readyToInstall = true; }
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('er, v',"er\nV")), install: readyToInstall, uninstall: false, submitOnChange: true) {
		def ecoAuthDesc = (atomicState.authToken != null) ? "[Connected]\n" :"[Not Connected]\n"		
		
		// If no device Handlers we cannot proceed
		if(!atomicState.initialized && !deviceHandlersInstalled) {
			section() {
				paragraph "ERROR!\n\nYou MUST add the ${getChildThermostatName()} and ${getChildSensorName()} Device Handlers to the IDE BEFORE running setup."				
			}		
		} else {
			readyToInstall = true
		}
		
		if(atomicState.initialized && !atomicState.authToken) {
			section() {
				paragraph(getFormat("warning", "You are no longer connected to the ecobee API. Please re-Authorize below."))
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
				section(sectionTitle("Helpers")) {
					href ("helperSmartAppsPage", title: inputTitle("Helper ${ST?'SmartApps':'Applications'}"), description: "${HE?'Click':'Tap'} to manage Helper ${ST?'SmartApps':'Applications'}")
				}			 
			}
			section(sectionTitle("Ecobee Devices")) {
				def howManyThermsSel = settings.thermostats?.size() ?: 0
				def howManyTherms = atomicState.numAvailTherms ?: "?"
				def howManySensors = atomicState.numAvailSensors ?: "?"
				
				// Thermostats
				atomicState.settingsCurrentTherms = settings.thermostats ?: []
				href ("thermsPage", title: inputTitle("Thermostats"), description: "${HE?'Click':'Tap'} to select Ecobee Thermostats [${howManyThermsSel}/${howManyTherms}]")				  
				
				// Sensors
				if (settings.thermostats?.size() > 0) {
					atomicState.settingsCurrentSensors = settings.ecobeesensors ?: []
					def howManySensorsSel = settings?.ecobeesensors?.size() ?: 0
					if ((howManySensors != "?") && (howManySensorsSel > howManySensors)) { howManySensorsSel = howManySensors } // This is due to the fact that you can remove already selected hidden items
					href ("sensorsPage", title: inputTitle("Sensors"), description: "${HE?'Click':'Tap'} to select Ecobee Sensors [${howManySensorsSel}/${howManySensors}]")
				}
			}		 
			section(sectionTitle("Preferences")) {
				href ("preferencesPage", title: inputTitle("Ecobee Suite Preferences"), description: "${HE?'Click':'Tap'} to manage global Preferences")
				if (ST) {
					href ("askAlexaPage", title: "Ask Alexa Preferences", description: "Tap to review Ask Alexa settings")
				} // askAlexa not (yet) supported on Hubitat
			}
		   
		} // End if(atomicState.authToken)
		
		// Setup our API Tokens		  
		section(sectionTitle("Authentication")) {
			href ("authPage", title: inputTitle("Ecobee API Authorization"), description: "${ecoAuthDesc}${HE?'Click':'Tap'} for Ecobee Authentication")
		}	   
		if ( debugLevel(5) ) {
			section (sectionTitle("Debug Dashboard")) {
				href ("debugDashboardPage", description: "${HE?'Click':'Tap'} to enter the Debug Dashboard", title: inputTitle("Debug Dashboard"))
			}
		}
		section(sectionTitle( "Removal")) {
			href ("removePage", description: "${HE?'Click':'Tap'} to remove ${cleanAppName(app.label?:app.name)}", title: inputTitle("Remove Ecobee Suite Manager"))
		}			 
		
		section (sectionTitle("Naming")) {
        	String defaultName = "Ecobee Suite Manager"
			String defaultLabel
			if (!atomicState?.appDisplayName) {
				defaultLabel = defaultName
				app.updateLabel(defaultName)
				atomicState?.appDisplayName = defaultName
			} else {
				defaultLabel = atomicState.appDisplayName
			}
			label(name: "name", title: inputTitle("Assign a name"), required: false, defaultValue: defaultLabel, submitOnChange: true, width: 6)
            if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			} else {
            	atomicState.appDisplayName = app.label
            }
			if (HE) {
				if (app.label.contains('<span')) {
                    if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains('<span ')) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
						String myLabel = app.label.substring(0, app.label.indexOf('<span'))
						atomicState.appDisplayName = myLabel
						app.updateLabel(atomicState.appDisplayName)
					}
				}
			}
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

def removePage() {
	dynamicPage(name: "removePage", title: pageTitle("Ecobee Suite Manager\nRemove Ecobee Suite Manager and its Children"), install: false, uninstall: true) {
		section () {
			paragraph(getFormat("warning", "Removing Ecobee Suite Manager also removes all Helpers and Devices!"))
		}
	}
}

// Setup OAuth between SmartThings and Ecobee clouds
def authPage() {
	LOG("authPage() --> Begin", 3, null, 'trace')
	//boolean ST = isST
	//boolean HE = !ST

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
				return dynamicPage(name: "authPage", title: pageTitle("Ecobee Suite Manager\nOAuth Initialization Failure"), nextPage: "", uninstall: true) {
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
		return dynamicPage(name: "authPage", title: pageTitle("Ecobee Suite Manager\nEcobee API Authentication"), nextPage: "", uninstall: uninstallAllowed) {
            section(sectionTitle(" ")) {
            	paragraph("Most Ecobee owners have 'Residential' accounts. Select 'Commercial' if you have an EMS type thermostat.", width: 9)
            	input(name: 'accountType', type: 'enum', required: true, title: inputTitle("Select Ecobee account type"), defaultValue: 'registered', submitOnChange: true, 
                  options: ['managementSet':'Commercial', 'registered':'Residential'], width: 4)
                if (settings?.accountType == null) { app.updateSetting('accountType', 'registered'); settings.accountType = 'registered'; }
                if (settings?.accountType == 'managementSet') {
            		input(name: 'managementSet', type: "text", title: inputTitle("Enter path of the desired Management Set"), defaultValue: '/', required: true, submitOnChange: true, width: 4)
                	if (settings?.managementSet == null) { app.updateSetting('managementSet', '/'); settings.managementSet = '/' }
            	}
                
				paragraph "${HE?'Click':'Tap'} below to log in to the Ecobee service and authorize Ecobee Suite for ${ST?'SmartThings':'Hubitat'} access. Be sure to ${HE?'Click':'Tap'} the 'Allow' button on the 2nd page."
				href url: redirectUrl, style: "${ST?'embedded':'external'}", required: true, title: inputTitle("Ecobee Account Authorization"), description: description
			}
		}
	} else {		
		LOG("authPage() --> Valid OAuth token (${atomicState.authToken})", 3, null, 'trace')
		return dynamicPage(name: "authPage", title: pageTitle("Ecobee Suite Manager\nEcobee API Authentication"), nextPage: "mainPage", uninstall: uninstallAllowed) {
			section(sectionTitle(" ")) {
				paragraph "Return to the main menu"
				href url:redirectUrl, style: "embedded", state: "complete", title: inputTitle("Ecobee Account Authorization"), description: description
			}
		}			
	} 
}

// Select which Thermostats are to be used
def thermsPage(params) {
	LOG("=====> thermsPage() entered", 5)		 
	def stats = getEcobeeThermostats()
	//boolean ST = isST
	//boolean HE = !isST
	
	LOG("thermsPage() -> thermostat list: ${stats}")
	LOG("thermsPage() starting settings: ${settings}")
	LOG("thermsPage() params passed? ${params}", 4, null, "trace")

	dynamicPage(name: "thermsPage", title: pageTitle("Ecobee Suite Manager\nThermostats"), params: params, nextPage: "", content: "thermsPage", uninstall: false) {	
		section(title: sectionTitle("Thermostat Selection")) {
        	paragraph("Most Ecobee owners have 'Residential' accounts. Select 'Commercial' if you have an EMS type thermostat.", width: 9)
            	input(name: 'accountType', type: 'enum', required: true, title: inputTitle("Select Ecobee account type"), defaultValue: 'registered', submitOnChange: true, 
                  options: ['managementSet':'Commercial', 'registered':'Residential'], width: 4)
                if (settings?.accountType == null) { app.updateSetting('accountType', 'registered'); settings.accountType = 'registered'; }
                if (settings?.accountType == 'managementSet') {
            		input(name: 'managementSet', type: "text", title: inputTitle("Enter path of the desired Management Set"), defaultValue: '/', required: true, submitOnChange: true, width: 4)
                	if (settings?.managementSet == null) { app.updateSetting('managementSet', '/'); settings.managementSet = '/' }
            	}
            
        	paragraph("${HE?'Click':'Tap'} below to see the list of Ecobee thermostats available in your Ecobee account and select the ones you want to connect to ${ST?'SmartThings':'Hubitat'}.")
			LOG("thermsPage(): atomicState.settingsCurrentTherms=${atomicState.settingsCurrentTherms}	thermostats=${settings.thermostats}", 1, null, "trace")
			if (atomicState.settingsCurrentTherms != settings.thermostats) {
				LOG("atomicState.settingsCurrentTherms != thermostats: changes detected!", 1, null, "warn")		
			} else { 
            	LOG("atomicState.settingsCurrentTherms == thermostats: No changes detected!", 1, null, "trace") 
            }
			input(name: "thermostats", title:inputTitle("Select Thermostats"), type: "enum", required:false, multiple:true, description: "Tap to choose", params: params, 
            	  options: stats, submitOnChange: true, width: 6)		   
		}
		section(sectionTitle("Temperature Scale")) {
        	paragraph(getFormat("note","The temperature scale (Fahrenheit or Celsius) is determined by your ${getHubPlatform()} Location settings automatically. Please update your Hub settings " + 
			"(under ${ST?'My Locations':'Settings/Location and Modes'}) to change the units used.\n\nThe current scale is °${temperatureScale}."))
		}
	}	   
}

def sensorsPage() {
	// Only show sensors that are part of the chosen thermostat(s)
	// Refactor to show the sensors under their corresponding Thermostats. Use Thermostat name as section header?
	LOG("=====> sensorsPage() entered. settings: ${settings}", 5)
	atomicState.sensorsPageVisited = true
	//boolean ST = isST
	//boolean HE = !ST

	def options = getEcobeeSensors() ?: []
	def numFound = options.size() ?: 0
	
	LOG("options = getEcobeeSensors == ${options}")

	dynamicPage(name: "sensorsPage", title: pageTitle("Ecobee Suite Manager\nSensors"), nextPage: "") {
		if (numFound > 0)  {
			section(title: sectionTitle('Sensor Selection')) {
            	paragraph("${HE?'Click':'Tap'} below to see the list of ecobee sensors available for the selected thermostat(s) and choose the ones you want to connect to ${ST?'SmartThings':'Hubitat'}.")
				LOG("sensorsPage(): atomicState.settingsCurrentSensors=${atomicState.settingsCurrentSensors} / ecobeesensors=${settings.ecobeesensors}", 1, null, "trace")
				if (atomicState.settingsCurrentSensors != settings.ecobeesensors) {
					LOG("atomicState.settingsCurrentSensors != ecobeesensors: changes detected!", 1, null, "warn")					
				} else { 
                	LOG("atomicState.settingsCurrentSensors == ecobeesensors: No changes detected!", 1, null, "trace")
                }
				input(name: "ecobeesensors", title:inputTitle("Select Ecobee Sensors (${numFound} found)"), type: "enum", required:false, description: "${HE?'Click':'Tap'} to choose", multiple:true, 
					  options: options, width: 8, height: 1)
			}
			if (settings?.showThermsAsSensor) { 
				section() {
					paragraph(getFormat("note", "Thermostats are included as an available sensor selection to allow for actual temperature & motion states on the thermostat to be utilized within ${HE?'Hubitat':'SmartThings'}"))
				}
			}
		} else {
			// No sensors associated with this set of Thermostats was found
		    LOG("sensorsPage(): No sensors found.", 4)
			section() { 
				paragraph("No associated sensors were found. ${HE?'Click':'Tap'} Done${settings.thermostats?'':' and select one or more Thermostats'}")
			}
		}		 
	}
}

def askAlexaPage() {
	//boolean HE = isHE
	
	dynamicPage(name: "askAlexaPage", title: pageTitle("Ecobee Suite Manager\nAsk Alexa Integration"), nextPage: "") {
	
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
			section("${cleanAppName(app.label?:app.name)} can send Ecobee Alerts and Reminders to one or more Ask Alexa, where Alexa can deliver them as messages and/or notificaitons.") {
				paragraph(image: 'https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/ask-alexa.src/AskAlexa@2x.png', 
					title: 'Ask Alexa Integration', '')
				input(name: 'askAlexa', type: 'bool', title: 'Send Ecobee Alerts & Reminders to Ask Alexa?', required: false, submitOnChange: true, defaultValue: false)
			}
			if (askAlexa) {
				section("${cleanAppName(app.label?:app.name)} can send Ecobee Alerts and Reminders to one or more Ask Alexa Message Queues.") {
					input(name: 'listOfMQs', type: 'enum', title: 'Send to these Ask Alexa Message Queues', description: 'Tap to select', options: atomicState.askAlexaMQ, submitOnChange: true, 
						multiple: true, required: true)
				}
				section("${cleanAppName(app.label?:app.name)} can automatically acknowledge Ecobee Alerts and Reminders when they are deleted from the Ask Alexa Message Queue${(listOfMQs&&(listOfMQs.size()>1))?'s':''}?") {
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
	//boolean ST = isST
	//boolean HE = !ST
	
	dynamicPage(name: "preferencesPage", title: pageTitle("Ecobee Suite Manager\nPreferences"), nextPage: "") {
		if (ST) {
        	section(title: "Notifications") {
				paragraph "Notifications are only sent when the Ecobee API connection is lost and unrecoverable, at most once per hour."
                input(name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, 
                      required: false /*((settings?.phone == null) && !settings.notifiers && !settings.speak)*/, submitOnChange: true)
                input(name: "notifiers", type: "capability.notification", title: "Select Notification Devices", hideWhenEmpty: true,
                      required: false /*((settings.phone == null) && !settings.speak && !settings.pushNotify)*/, multiple: true, submitOnChange: true)
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
			section() {
				paragraph ( "A notification is always sent to the Hello Home log")
			}
		} else {		// HE
			section(title: smallerTitle("Notifications")) {
				paragraph("Notifications are only sent when the Ecobee API connection is lost and unrecoverable, at most 1 per hour.", width: 8)
				input(name: "notifiers", type: "capability.notification", multiple: true, title: inputTitle("Select Notification Devices"), submitOnChange: true, width: 6,
					  required: false /*(!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null)))*/)
                if (settings?.notifiers) {
                    List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                    if (echo) {
                        input(name: "echoAnnouncements", type: "bool", title: "Use ${echo.size()>1?'simultaneous ':''}Announcements for the Echo Speaks device${echo.size()>1?'s':''}?", 
                              defaultValue: false, submitOnChange: true)
                    }
                }
			}
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
            section() {
            	paragraph "A 'HelloHome' notification is always sent to the Location Event log"
            }
		}
		
		if (ST) {
			section("Mobile Device") {
				input(name: 'mobile', type: 'enum', options: ["Android", "iOS"], title: 'Select your primary mobile device type', defaultValue: 'iOS', submitOnChange: true, required: true, multiple: false)
			}
		}
		
		section(title: smallerTitle("Hold Actions")) {
			input(name: "holdType", title:inputTitle("Select default Hold Type"), type: "enum", required:false, multiple:false, defaultValue: "Until I Change", width: 4, 
				  submitOnChange: true, description: "Until I Change", options: ["Until I Change", "Until Next Program", "2 Hours", "4 Hours", "Specified Hours", "Thermostat Setting"])
            if (settings.holdType == null) { app.updateSetting('holdType', 'Until I Change'); settings.holdType = 'Until I Change'; }
			if (settings.holdType=="Specified Hours") {
				input(name: 'holdHours', title:inputTitle('How many hours')+" (1-48)", type: 'number', range:"1..48", required: true, description: '2', defaultValue: 2, width: 4)
                if (settings.holdHours == null) { app.updateSetting('holdHours', 2); settings.holdHours = 2; }
			} else if (settings.holdType=='Thermostat Setting') {
				paragraph("Thermostat setting at time of hold request will be used.")
			}
		}	
		section(title: smallerTitle("Smart Auto")) {
        	paragraph("The 'Smart Auto Temperature Adjust' feature determines if you want to allow the thermostat setpoint to be changed using the arrow buttons in the Tile when the thermostat is in 'auto' mode.", width: 8)
			if (HE) paragraph("", width: 4)
			input(name: "smartAuto", title:inputTitle("Use Smart Auto Temperature Adjust?"), type: "bool", required:false, defaultValue: false, description: "", width: 4)
            if (settings?.smartAuto == null) { app.updateSetting('smartAuto', false); settings?.smartAuto = false; }
		}	 
		section(title: smallerTitle("Polling Interval")) {
        	paragraph("How frequently do you want to poll the Ecobee cloud for changes? For maximum responsiveness to commands, it is recommended to set this to 1 minute.", width: 8)
			if (HE) paragraph("", width: 4)
			input(name: "pollingInterval", title:inputTitle("Select Polling Interval")+" (minutes)", type: "enum", required:false, multiple:false, defaultValue:3, description: "3", width: 4,
            	  options:["1", "2", "3", "5", "10", "15", "30"])
            if (settings?.pollingInterval == null) { app.updateSetting('pollingInterval', "3"); settings?.pollingInterval = "3"; }
		}
		section(title: smallerTitle("Thermostat as Sensor")) {
        	paragraph("Showing Thermostats as separate Sensors is useful if you need to access the actual temperature in the room where the Thermostat is located and not just the (average) "+
            		  "temperature displayed on the Thermostat.", width: 8)
			if (HE) paragraph("", width: 4)
			input(name: "showThermsAsSensor", title:inputTitle("Include Thermostats as a separate Ecobee Sensor?"), type: "bool", required:false, defaultValue: false, description: "", width: 6)
            if (settings?.showThermsAsSensor == null) { app.updateSetting('showThermsAsSensor', false); settings?.showThermsAsSensor = false; }
		}
		section(title: smallerTitle("Button Delay")) {
        	paragraph("Set the pause between pressing the setpoint arrows and initiating the API calls. The pause needs to be long enough to allow you to click the arrow again for changing by "+
            		  "more than one degree.", width: 8)
			if (HE) paragraph("", width: 4)
			input(name: "arrowPause", title:inputTitle("Select Button Delay")+" (seconds)", type: "enum", required:false, multiple:false, description: "4", defaultValue:4, 
				  options:["1", "2", "3", "4", "5"], width: 4)
            if (settings?.arrowPause == null) { app.updateSetting('arrowPause', 4); settings?.arrowPause = 4; }
		}
		section(title: smallerTitle("Decimal Precision")) {
        	paragraph("Select the desired number of decimal places to display for all temperatures (default 1 for C, 0 for F).", width: 8)
			if (HE) paragraph("", width: 4)
			String digits = wantMetric() ? "1" : "0"
			input(name: "tempDecimals", title:inputTitle("Select Decimal display precision"), type: "enum", required:false, multiple:false, defaultValue:digits, description: digits, 
				  options:["0", "1", "2"], submitOnChange: true, width: 4)
            if (settings?.tempDecimals == null) { app.updateSetting('tempDecimals', digits); settings?.tempDecimals = digits; }
		}
		section(title: smallerTitle("Debug Log Level")) {
        	paragraph("Select the debug logging level. Higher levels send more information to IDE Live Logging. A setting of 2 is recommended for normal operations.", width: 8)
			if (HE) paragraph("", width: 4)
			input(name: "debugLevel", title:inputTitle("Select Debug Log Level"), type: "enum", required:false, multiple:false, defaultValue:2, description: "2", 
				  options:["5", "4", "3", "2", "1", "0"], width: 4)
            if (settings?.debugLevel == null) { app.updateSetting('debugLevel', 2); settings?.debugLevel = 2; }
		}
	}
}

def debugDashboardPage() {
	LOG("=====> debugDashboardPage() entered.", 5)	  
	//boolean HE = isHE
	
	dynamicPage(name: "debugDashboardPage", title: "") {
		section(getVersionLabel())
		section(sectionTitle("Commands")) {
			href(name: "pollChildrenPage", title: "", required: false, page: "pollChildrenPage", description: "Tap to execute: pollChildren()")
			href(name: "refreshAuthTokenPage", title: "", required: false, page: "refreshAuthTokenPage", description: "Tap to execute: refreshAuthToken()")
			href(name: "updatedPage", title: "", required: false, page: "updatedPage", description: "Tap to execute: updated()")
		}		
		
		section(sectionTitle("Settings Information")) {
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
		section(sectionTitle("Dump of Debug Variables")) {
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
		section(sectionTitle("Commands")) {
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
	//boolean ST = isST
	//boolean HE = !ST

	LOG("The available Helper ${ST?'Smart':''}Apps are ${getHelperSmartApps()}", 5, null, "info")
	
	dynamicPage(name: "helperSmartAppsPage", title: pageTitle("Ecobee Suite Manager\nHelper ${ST?'SmartApps':'Applications'}"), nextPage: "", install: false, uninstall: false, submitOnChange: true) { 
		section(sectionTitle("Global Pause: ${HE?'<span style=color:red;font-weight: bold>':''}${settings.pauseHelpers?'ON':'OFF'}${HE?'</span>':''}")) {
			// paragraph "Global Pause will pause all Helpers that are not already paused; un-pausing will restore only the Helpers that weren't already paused. "
			input(name: "pauseHelpers", type: "bool", title: inputTitle("Global Pause all Helpers?"), width: 6, 
            	  defaultValue: ((atomicState.appsArePaused == null) ? false : atomicState.appsArePaused), submitOnChange: true)
			input(name: 'pauseSwitch', type: 'capability.switch', title: inputTitle("Synchronize Global Pause with this Switch")+" (optional)", 
            	  multiple: false, required: false, submitOnChange: true) 
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
			if (HE)	 paragraph(getFormat("note", "The '(paused)' status for the installed Helpers displayed below may not change until you refresh this page."))
		}
		// if (ST) section("Installed Helper SmartApps") {}
		section(sectionTitle("Avalable Helper ${ST?'SmartApps':'Applications'}")) {
			getHelperSmartApps().each { oneApp ->
				LOG("Processing the app: ${oneApp}", 4, null, "trace")			  
				def allowMultiple = ST ? oneApp.multiple.value : oneApp.multiple
				app(name:"${ST?oneApp.name.value:oneApp.name}", appName:"${ST?oneApp.appName.value:oneApp.appName}", namespace:"${ST?oneApp.namespace.value:oneApp.namespace}", 
					title: inputTitle("${ST?oneApp.title.value:oneApp.title}"), multiple:allowMultiple)
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
		if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) { 
			LOG("You MUST add the ${getChildThermostatName()} and ${getChildSensorName()} Device Handlers to the IDE BEFORE running the setup.", 1, null, "error")
			success = false
		}
	}
	log.debug "device handlers = ${success}"
    boolean deletedChildren = true
	try {
		if (d1) deleteChildDevice("dummyThermDNI-${DNIAdder}") 
		if (d2) deleteChildDevice("dummySensorDNI-${DNIAdder}")
	} catch (Exception e) {
		LOG("Error ${e} deleting test devices (${d1}, ${d2})",1,null,'warn')
        deletedChildren = false
	}
	
	if (!deletedChildren) runIn(5, delayedRemoveChildren, [overwrite: true])
	atomicState.runTestOnce = success
	return success
}

void delayedRemoveChildren() {
	def myChildren = getAllChildDevices()
	if (myChildren.size() > 0) removeChildDevices( myChildren, true )
}

void removeChildDevices(devices, dummyOnly = false) {
	if (devices != []) {
		LOG("Removing ${dummyOnly?'test':'unused'} child devices",3,null,'trace')
	} else {
		return		// nothing to remove
	}
	def devName
	try {
		devices?.each {
			devName = it.displayName
			if (!dummyOnly || it?.deviceNetworkId?.startsWith('dummy')) {
            	LOG("Removing unused child: ${it.deviceNetworkId} - ${devName}",1,null,'warn')
            	deleteChildDevice(it.deviceNetworkId)
            } else {
            	LOG("Keeping child: ${it.deviceNetworkId} - ${devName}",3,null,'info')
            }
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
	if (debugLevel(4)) LOG("acknowledgeEcobeeAlert(${deviceId},${ackRef}): jsonRequestBody = ${jsonRequestBody}", 4, null, 'trace')
	// need to get child from the deviceId
	result = sendJson(null, jsonRequestBody)
	if (result) LOG("Acknowledged Ecobee Alert ${ackRef} for thermostat ${deviceId})",2,null,'info')
}
// End of Ask Alexa Helpers

// OAuth Init URL
def oauthInitUrl() {
	LOG("oauthInitUrl with callback: ${callbackUrl}", 2)
	atomicState.oauthInitState = ST ? UUID.randomUUID().toString() : stateUrl // HE does redirect a little differently
	//log.debug "oauthInitState: ${atomicState.oauthInitState}"
	
	boolean chg = false
	if (ST && atomicState?.initialized && (atomicState?.apiKey == null)) chg = true; atomicState.initialized = false;	 // Force changing over the the new ST key
	def oauthParams = [
		response_type:	"code",
		client_id:		ecobeeApiKey,					// actually, the Ecobee Suite app's client ID
		scope:			"smartRead,smartWrite,ems", 
		redirect_uri:	callbackUrl,
		state:			atomicState.oauthInitState
	]
	if (ST && chg) atomicState.initialized = true
		
	LOG("oauthInitUrl - ${ST?'Before redirect:':''} location: ${apiEndpoint}/authorize?${toQueryString(oauthParams)}", 2, null, 'debug')
	if (ST) {
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
			redirect_uri: callbackUrl,
            timeout: 30
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
def success() {
	def message = """
	<p>Your ecobee Account is now connected to ${ST?'SmartThings':'Hubitat'}!</p>
	<p>${HE?'Close this window and c':'C'}lick 'Done' to finish setup.</p>
	"""
	connectionStatus(message)
}

def fail() {
	def message = """
		<p>The connection could not be established!</p>
		<p>${HE?'Close this window and c':'C'}lick 'Done' to return to the menu.</p>
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
	String hubIcon = ST ? 'https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png' : 'https://raw.githubusercontent.com/SANdood/Icons/master/Hubitat/HubitatLogo.png'

	def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Ecobee & ${ST?'SmartThings':'Hubitat'} connection</title>
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
				<img src="${hubIcon}" alt="${ST?'SmartThings':'Hubitat'} logo" />
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
	
    if (!settings?.accountType) { app.updateSetting('accountType', 'registered'); settings.accountType = 'registered'; }
    String requestBody
    if (settings.accountType == 'registered') {
        requestBody = '{"selection":{"selectionType":"registered","selectionMatch":"","includeRuntime":true,"includeSensors":true,"includeLocation":true,"includeProgram":true}}'
    } else if (settings.accountType == 'commercial') {
        requestBody = '{"selection":{"selectionType":"managementSet","selectionMatch":"' + settings.managementSet.trim() + '","includeRuntime":true,"includeSensors":true,"includeLocation":true,"includeProgram":true}}'
    } else {
    	LOG("getEcobeeThermostats(): Cannot determine Ecobee account type - please set this on the Preferences / Thermostats settings page", 1, null, 'error')
        return [:]
    }
    def deviceListParams = [
        uri: apiEndpoint,
        path: "/1/thermostat",
        headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
        query: [format: 'json', body: requestBody],
        timeout: 30
    ]
    if (deviceListParams != [:]) {
        def stats = [:]
        def statLocation = [:]
        try {
            httpGet(deviceListParams) { resp ->
            	LOG("getEcobeeThermostats() - httpGet() response: ${resp.data}", 4, null, 'trace')
            	//	the Thermostat Data. Will reuse for the Sensor List intialization
            	atomicState.thermostatData = resp.data			

                if (resp && resp.isSuccess() && resp.status && (resp.status == 200)) {
                    LOG("getEcobeeThermostats() - httpGet() in 200 Response", 3, null, 'trace')
                    atomicState.numAvailTherms = resp.data.thermostatList?.size() ?: 0

                    resp.data.thermostatList.each { stat ->
                        def dni = (HE?'ecobee_suite-thermostat-':'') + ([app.id, stat.identifier].join('.'))	// HE App.ID is just too short :)
                        stats[dni] = getThermostatDisplayName(stat)
                        statLocation[stat.identifier] = stat.location
                    }
                } else {				
                    LOG("getEcobeeThermostats() - httpGet() in else: http status: ${resp.status}", 1, null, 'trace')
                    //refresh the auth token
                    if (resp.status == 500 && resp.data?.status?.code == 14) {
                        LOG("getEcobeeThermostats() - Storing the failed action to try later", 1, null, 'trace')
                        atomicState.action = "getEcobeeThermostats"
                        LOG("getEcobeeThermostats() - Refreshing your auth_token!", 1, null, 'trace')
                        refreshAuthToken()
                    } else {
                        LOG("getEcobeeThermostats() - Other error. Status: ${resp.status}  Response data: ${resp.data} ", 1, null, 'error')
                    }
                    return [:]
                }
            }
            // will get (groovyx.net.http.HttpResponseException e) if no "registered" thermostats, perhaps is "managementSet"
        } catch(Exception e) {
            LOG("___exception getEcobeeThermostats(): ${e}", 1, null, "error")
            atomicState.action = "getEcobeeThermostats"
            refreshAuthToken()
            return [:]
        }
        atomicState.thermostatsWithNames = stats
        atomicState.statLocation = statLocation
        LOG("getEcobeeThermostats() - thermostatsWithNames: ${stats}, locations: ${statLocation}", 4, null, 'trace')
        return stats.sort { it.value }
    }
}

// Get the list of Ecobee Sensors for use in the settings pages (Only include the sensors that are tied to a thermostat that was selected)
// NOTE: getEcobeeThermostats() should be called prior to getEcobeeSensors to refresh the full data of all thermostats
Map getEcobeeSensors() {	
	LOG("====> getEcobeeSensors() entered. thermostats: ${settings.thermostats}", 2,null,'trace')
	boolean debugLevelFour = debugLevel(4)
	def sensorMap = [:]
	def foundThermo = null
	// TODO: Is this needed?
	atomicState.remoteSensors = [:]	   

	// Now that we routinely only collect the data that has changed in atomicState.thermostatData, we need to ALWAYS refresh that data
	// here so that we are sure we have everything we need here.
	def stats = getEcobeeThermostats()
	
    if (stats != [:]) {
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
	} else {
    	LOG("getEcobeeSensors() - Missing or invalid thermostatList - no sensors",1,null,'error')
        return [:]
    }
	if (debugLevelFour) LOG("getEcobeeSensors() - remote sensor list: ${sensorMap}", 4)
	sensorMap = sensorMap.sort { it.value }
	atomicState.eligibleSensors = sensorMap
	atomicState.numAvailSensors = sensorMap.size() ?: 0
    //atomicState.thermostatData = [:]		// release the memory
	return sensorMap
}
	 
String getThermostatDisplayName(stat) {
	if (stat?.name)	return stat.name.toString()
	return getThermostatModelName(stat) + " (${stat?.identifier})"
}

String getThermostatModelName(stat) {
	switch(stat?.modelNumber) { // == "siSmart" ? "Smart Si" : "Smart"
    	case 'idtSmart' 	: return 'Smart'
		case 'idtEms'		: return 'Smart EMS'
		case 'siSmart'		: return 'Smart Si'
		case 'siEms'		: return 'Smart Si EMS'
		case 'athenaSmart'	: return 'ecobee3'
		case 'athenaEms'	: return 'ecobee3 EMS'
		case 'corSmart'		: return 'Carrier/Bryant Cor Smart'
		case 'nikeSmart'	: return 'ecobee3 lite'
		case 'nikeEms'		: return 'ecobee3 lite EMS'
        case 'apolloSmart'	: return 'ecobee4'
        case 'apolloEms'	: return 'ecobee4 EMS'
        default				: return 'unknown'
    }   
}

void installed() {
	LOG("Installed (${atomicState.inSetup}) with settings: ${settings}",1,null,'trace')	
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
	LOG("Updated (${atomicState.inSetup}) with settings: ${settings}",1,null,'trace')	
	
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
	LOG("Hub rebooted (${atomicState.inSetup}), re-initializing", 1, null, 'trace')
	initialize()
}

@Field final int watchdogInterval = 10	// In minutes
@Field final int reAttemptInterval = 15	// In seconds

def initialize() {	
	LOG("${getVersionLabel()} Initializing...", 1, null, 'trace')
    if (atomicState.inSetup) atomicState.inSetup = false

    if (atomicState.inPollChildren) {
    	// let the current poll cycle complete first, so we don't yank the rug out from under its feet
        def seconds = ST ? 21 : 26
        if (atomicState.skipTime) {
        	seconds = seconds - (((now() - atomicState.skipTime)/1000).toInteger())	// mow much longer do we need to wait
        }
    	if (seconds > 1) {
        	LOG("intialize() - Waiting ${seconds} seconds for current poll cycle to complete...",1,null,'warn')
        	runIn(seconds, initialize, [overwrite: true])
        	return false
        }
	} else {
		atomicState.inPollChildren = true
    	atomicState.skipTime = null
	}
	
    try {
		unsubscribe()
        unschedule(pollScheduled)
        unschedule(scheduleWatchdog)
        unschedule(forceNextPoll)
		unschedule() // reset all the schedules
	} catch (Exception e) {
		LOG("initialize() - Exception encountered trying to unschedule(). Exception: ${e}", 1, null, "error")
	}
    
	atomicState.timers = null
	
	atomicState.connected = "full"
	atomicState.wifiAlert = false
	updateMyLabel()
	atomicState.reAttempt = 0
	atomicState.reAttemptPoll = 0
    
	subscribe(app, appHandler)
	if (ST) subscribe(location, "AskAlexaMQ", askAlexaMQHandler) // HE version doesn't support Ask Alexa yet
	// if (!askAlexa) atomicState.askAlexaAlerts = null
	if (settings.pauseSwitch) {
		subscribe(settings.pauseSwitch, 'switch', pauseSwitchHandler)
	}
	
	def nowTime = now()
	def nowDate = getTimestamp()
	
	// Initialize several state variables
    atomicState.climateChangeQueue = 0
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
	def updatesLog = [thermostatUpdated:true, runtimeUpdated:true, forcePoll:true, getWeather:true, alertsUpdated:true, extendRTUpdated:true ]
	atomicState.updatesLog = updatesLog
	atomicState.hourlyForcedUpdate = 0
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
	atomicState.timeOfDay = getTimeOfDay()		// "day" or "night"
		
	// Setup initial polling and determine polling intervals
	atomicState.pollingInterval = getPollingInterval()
    // The next two are now Global @Fields
	//atomicState.watchdogInterval = 10	// In minutes
	//atomicState.reAttemptInterval = 15	// In seconds
	
	if (atomicState.initialized) {		
		// refresh Thermostats and Sensor full lists
		// getEcobeeThermostats()
		// getEcobeeSensors() will call getEcobeeThermostats() for us
		getEcobeeSensors()
	} 
	
	// Create our Children, if necessary. This should only be needed during initial setup and when therms or sensors are added or removed.
	boolean aOK = true
	if (settings.thermostats?.size() > 0) 				{ aOK = aOK && createChildrenThermostats() }
	if (aOK && (settings.ecobeesensors?.size() > 0)) 	{ aOK = aOK && createChildrenSensors() }
    // Don't delete if either create operation fails
	if (aOK) deleteUnusedChildren()
   
	// Clear out all atomicState object collections (in case a thermostat was deleted or replaced, so we don't slog around useless old data)
	// also has the effect of forcing ALL the data bits to be sent down to the thermostats/sensors again
	atomicState.alerts = [:]
	atomicState.askAlexaAlerts = [:]
	atomicState.askAlexaAppAlerts = [:]
    atomicState.audio = [:]
	atomicState.changeAlerts = [:]
	atomicState.changeAttrs = [:]
    atomicState.changeAudio = [:]
	atomicState.changeCloud = [:]
	atomicState.changeConfig = [:]
	atomicState.changeDevice = [:]
	atomicState.changeEquip = [:]
	atomicState.changeNever = [:]
	atomicState.changeOften = [:]
	atomicState.changeRarely = [:]
    atomicState.changeTemps = [:]
    
	atomicState.currentProgramName = [:]
	atomicState.equipmentStatus = [:]
	atomicState.events = [:]
	atomicState.extendedRuntime = [:]
    atomicState.myStatsClimates = [:]
	atomicState.oemCfg = [:]
	atomicState.program = [:]
    atomicState.climates = [:]
    atomicState.schedule = [:]
	atomicState.timeSchedule = [:]
    atomicState.currentClimateRef = [:]
	atomicState.runningEvent = [:]
	atomicState.runtime = [:]
	atomicState.settings = [:]
	atomicState.statInfo = [:]
	atomicState.statLocation = [:]
	atomicState.statTime = [:]
	atomicState.weather = [:]
	atomicState.versionLabel = getVersionLabel()
	
	// Add subscriptions as little "daemons" that will check on our health	  
	subscribe(location, scheduleWatchdog)
	if (ST) subscribe(location, "routineExecuted", scheduleWatchdog)		// HE doesn't support Routines
	subscribe(location, "sunset", sunsetEvent)
	subscribe(location, "sunrise", sunriseEvent)
	//subscribe(location, "position", scheduleWatchdog)
	subscribe(location, "systemStart", rebooted)								// re-initialize if the hub reboots (HE only?)
	
	// Initial poll()
	if (settings.thermostats?.size() > 0) { 
		pollInit() 
	} else {
		// pollInit does this for us
		atomicState.inPollChildren = false
	}

	// Schedule the various handlers
	LOG("Spawning scheduled events from initialize()", 5, null, "trace")
	if (settings.thermostats?.size() > 0) { 
		LOG("Spawning the poll scheduled event. (thermostats.size(): ${settings.thermostats?.size()})", 1, null, 'trace')
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
	runIn(180, forceNextPoll, [overwrite: true])		// get ALL the data (again) once things settle down
	LOG("${getVersionLabel()} - initialization complete",1,null,'debug')
	// atomicState.versionLabel = getVersionLabel()
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
				if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) {
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
				if ("${e}".startsWith("${ST?'physicalgraph':'com.hubitat'}.app.exception.UnknownDeviceTypeException")) {
					LOG("You MUST add the ${getChildSensorName()} Device ${ST?'Handler':'Driver'} to the ${getHubPlatform} IDE BEFORE running the setup.", 1, null, "error")
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
    	//atomicState.runningCallQueue = false
    	//runCallQueue()
        //getMyChildren()
		/*LOG('appHandler(touch) event, forced poll',2,null,'info')
		def updatesLog = atomicState.updatesLog
		updatesLog.forcePoll = true
		updatesLog.getWeather = true	// update the weather also
		atomicState.updatesLog = updatesLog
		pollEcobeeAPI('')
		*/ //pollChildren(null, true)		// double force-poll
		//atomicState.runningCallQueue = false
		//atomicState.callQueue = [:]
		//resumeProgram(null, '311019854581', true)
        //updated()
        //String foo = getHubPlatform()
        log.debug "platform: ${hubPlatform}, ST: ${ST}, HE: ${HE}"
        //log.debug "dbgLevel: ${dbgLevel}, dLF: ${debugLevelFour}, dLT: ${debugLevelThree}"
	}
}

// For thermostat reservations handling
def isChildApp(String childId) {
	def child = getChildApps().find { it.id.toString() == childId }
	return (child != null)
}
String getChildAppName(String childId) {
	def child = getChildApps().find { it.id.toString() == childId }
	return child ? (cleanAppName(child.label?:child.name)) : ''
}
def childAppsPauser(pause = true) {
	getChildApps().each { child ->
		if (debugLevel(4)) LOG("Pauser(${pause}), child: ${cleanAppName(child.label?:child.name)} (${child.id})",4,null,'trace')
		if (pause) { child.pauseOn() } else { child.pauseOff() }
	}
}
String cleanAppName(String name) {
	if (name != "") {
        String cleanName
        if (atomicState.ST) {
            def flags = [' (paused', ' (open', ' (closed', ' (active', ' (inactive', ' (Cool', ' (Heat', ' (Aux', ' (Off', ' (Auto', ' (Emer']
            flags.each { flag ->
                if (!cleanName && name.contains(flag)) cleanName = name.substring(0,name.indexOf(flag))
            }
            return cleanName ?: name
        } else {
            return name.contains(' <span') ? name.substring(0,name.indexOf(' <span')) : name
        }
    }
}
def getMyChildren(String name="") {
// Returns a map of [child.dni:child.displayName] for all children, or only those of a certain app.name    
	def apps = [:]
    def installedApps = [:]
	if (name == "") {
    	apps = getChildApps()
    } else {
		if (ST) {
    		apps = findAllChildAppsByName(name)
		} else {
			apps = getChildApps().findAll { (it.name == name) }
		}
    }
    apps.each {
       	if (it.installationState == 'COMPLETE') {
			installedApps << [(it.id.toString()): cleanAppName(it.label?:it.name)]
    	}
    }
	return installedApps
}
def pauseChildApp(String appId, pause) {
	if (appId && (pause != null)) {
    	def child
    	getChildApps().each {
        	if (!child && (it.id.toString() == appId)) child = it
        }
        if (child) {
        	if (pause) { 
            	if (debugLevel(2)) LOG("pauseChildApp(): Pausing ${cleanAppName(child.label?:child.name)}",1,null,'trace')
            	child.pauseOn() 
            } else { 
            	if (debugLevel(2)) LOG("pauseChildApp(): Resuming ${cleanAppName(child.label?:child.name)}",1,null,'trace')
            	child.pauseOff() 
            }
        }
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
		
		// Don't delete any devices that are configured in settings (thermostats or ecobeesensors)	  
		def childrenToKeep = (settings?.thermostats ?: []) + (settings?.ecobeesensors ?: []) // (atomicState.eligibleSensors?.keySet() ?: [])
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
	def updatesLog = atomicState.updatesLog
	updatesLog.forcePoll = true
	updatesLog.getWeather = true	// update the weather also
	atomicState.updatesLog = updatesLog
	scheduleWatchdog(evt, true)
}

boolean scheduleWatchdog(evt=null, local=false) {
	boolean results = true	
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) {
		def evtStr = evt ? "${evt.name}:${evt.value}" : 'null'
		/* if (debugLevel(4)) */ LOG("scheduleWatchdog() called with evt (${evtStr}) & local (${local})", 1, null, "trace")
	}
	
	// Only update the Scheduled timestamp if it is not a local action or from a subscription
	if ( (evt == null) && (local==false) ) {
		atomicState.lastScheduledWatchdog = now()
		atomicState.lastScheduledWatchdogDate = getTimestamp()
		def updatesLog = atomicState.updatesLog
		updatesLog.getWeather = true								// next pollEcobeeApi for runtime changes should also get the weather object
		//atomicState.updatesLog = updatesLog
		
		// do a forced update once an hour, just because (e.g., forces Hold Status to update completion date string)
		def counter = atomicState.hourlyForcedUpdate
		counter = (!counter) ? 1 : counter + 1
		if (counter == 6) {
			counter = 0
			updatesLog.forcePoll = true
			//if (atomicState.inPollChildren) atomicState.inPollChildren = false	// reset, just in case
			// atomicState.forcePoll = true
			//atomicState.needExtendedRuntime = false // Force a recheck
			// log.debug "Skipping hourly forcePoll"
		}
		atomicState.hourlyForcedUpdate = counter
		atomicState.updatesLog = updatesLog
	}
	
	// Check to see if we have called too soon
	def timeSinceLastWatchdog = (atomicState.lastWatchdog == null) ? 0 :(now() - atomicState.lastWatchdog) / 60000
	if ( timeSinceLastWatchdog < 1 ) {
		if (debugLevelFour) LOG("It has only been ${timeSinceLastWatchdog} since last scheduleWatchdog was called. Please come back later.", 1, null, "trace")
		return true
	}
   
	atomicState.lastWatchdog = now()
	atomicState.lastWatchdogDate = getTimestamp()
	//if (atomicState.inPollChildren) atomicState.inPollChildren = false
	def pollAlive = isDaemonAlive("poll")
	def watchdogAlive = isDaemonAlive("watchdog")
	
	if (debugLevelFour) LOG("After watchdog tagging",4,null,'trace')
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
	if (!pollAlive) spawnDaemon("poll")
	if (!watchdogAlive) spawnDaemon("watchdog")
	
	return true
}

// Watchdog Checker
boolean isDaemonAlive(daemon="all") {
	 boolean debugLevelFour = debugLevel(4)
	// Daemon options: "poll", "auth", "watchdog", "all"	
	def daemonList = ["poll", "auth", "watchdog", "all"]
	//String preText = getDebugLevel() <= 2 ? '' : 'isDeamonAlive() - '
	Integer pollingInterval = getPollingInterval()

	//daemon = daemon.toLowerCase()
	boolean result = true	 
	
	if (debugLevelFour) LOG("isDaemonAlive() - now() == ${now()} for daemon (${daemon})", 1, null, "trace")
	
	// No longer running an auth Daemon, because we need the scheduler slot (max 4 scheduled things, poll + watchdog use 2)	
	// def timeBeforeExpiry = atomicState.authTokenExpires ? ((atomicState.authTokenExpires - now()) / 60000) : 0
	// LOG("isDaemonAlive() - Time left (timeBeforeExpiry) until expiry (in min): ${timeBeforeExpiry}", 4, null, "info")
	
	if (daemon == "poll" || daemon == "all") {
		def lastScheduledPoll = atomicState.lastScheduledPoll
		def timeSinceLastScheduledPoll = ((lastScheduledPoll == null) || (lastScheduledPoll == 0)) ? 1000 : ((now() - lastScheduledPoll) / 60000)
		if (debugLevelFour) {
			LOG("isDaemonAlive() - Time since last poll? ${timeSinceLastScheduledPoll} -- lastScheduledPoll == ${lastScheduledPoll}", 4, null, "info")
			LOG("isDaemonAlive() - Checking daemon (${daemon}) in 'poll'", 4, null, "trace")
		}
		def maxInterval = pollingInterval + 2
		if ( timeSinceLastScheduledPoll >= maxInterval ) result = false
	}	
	
	if (daemon == "watchdog" || daemon == "all") {
		def lastScheduledWatchdog = atomicState.lastScheduledWatchdog
		def timeSinceLastScheduledWatchdog = ((lastScheduledWatchdog == null) || (lastScheduledWatchdog == 0)) ? 1000 : ((now() - lastScheduledWatchdog) / 60000)
		if (debugLevelFour) {
			LOG("isDaemonAlive() - Time since watchdog activation? ${timeSinceLastScheduledWatchdog} -- lastScheduledWatchdog == ${lastScheduledWatchdog}", 4, null, "info")
			LOG("isDaemonAlive() - Checking daemon (${daemon}) in 'watchdog'", 4, null, "trace")
		}
		def maxInterval = watchdogInterval + 2
		if (debugLevelFour) LOG("isDaemonAlive(watchdog) - timeSinceLastScheduledWatchdog=(${timeSinceLastScheduledWatchdog})  Timestamps: (${atomicState.lastScheduledWatchdogDate}) (epic: ${lastScheduledWatchdog}) now-(${now()})", 4, null, "trace")
		if ( timeSinceLastScheduledWatchdog >= maxInterval ) result = false
	}
	
	if (!daemonList.contains(daemon) ) {
		// Unkown option passed in, gotta punt
		LOG("isDaemonAlive() - Unknown daemon: ${daemon} received. Do not know how to check this daemon.", 1, null, "error")
		result = false
	}
	if (debugLevelFour) LOG("isDaemonAlive() - result is ${result}", 1, null, "trace")
	if (!result) LOG("isDaemonAlive() - (${daemon}) has died", 1, null, 'warn')
	return result
}

boolean spawnDaemon(daemon="all", unsched=true) {
	// log.debug "spawnDaemon(${daemon}, ${unsched})"
	// Daemon options: "poll", "auth", "watchdog", "all"	
	def daemonList = ["poll", "auth", "watchdog", "all"]
	boolean debugLevelFour = debugLevel(4)
	Random rand = new Random()
	
	Integer pollingInterval = getPollingInterval()
	
	daemon = daemon.toLowerCase()
	boolean result = true
	
	if (daemon == "poll" || daemon == "all") {
		if (debugLevelFour) LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'poll'", 1, null, "trace")
		// Reschedule the daemon
		try {
			if ( unsched ) { unschedule("pollScheduled") }
			if ( HE || canSchedule() ) { 
				//LOG("Using runEvery to setup polling with pollingInterval: ${pollingInterval}", 1, null, 'trace')
				"runEvery${pollingInterval}Minute${pollingInterval!=1?'s':''}"("pollScheduled")
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
		if (debugLevelFour) LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'watchdog'", 1, null, "trace")
		// Reschedule the daemon
		try {
			if ( unsched ) { unschedule("scheduleWatchdog") }
			if ( HE || canSchedule() ) { 
				"runEvery${watchdogInterval}Minutes"("scheduleWatchdog")
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
	updateLastPoll(true)
	if (debugLevel(4)) LOG("pollScheduled() - Running at ${atomicState.lastScheduledPollDate} (epic: ${atomicState.lastScheduledPoll})", 1, null, "trace")	  
	pollChildren()
}

// Called during initialization to get the inital poll
def pollInit() {
	if (debugLevel(4)) LOG("pollInit()", 4)
	if (atomicState.inPollChildren) atomicState.inPollChildren = false
    if (atomicState.skipTime) atomicState.skipTime = null
	forceNextPoll()
	runIn(2, pollChildren, [overwrite: true])		// Hit the ecobee API for update on all thermostats, in 2 seconds
}

void forceNextPoll() {
	def updatesLog = atomicState.updatesLog
	if (!updatesLog?.forcePoll) {
		updatesLog.forcePoll = true
		atomicState.updatesLog = updatesLog
	}
}

// Clear the stored change logs for one specific tid - this will force everything to be send down to the device again
void clearChangeLogs(tid) {
	LOG("Clearing change logs for thermostat ${tid}",2,null,'info')
	def temp = [:]   
    temp = atomicState.changeAlerts
    if (temp) { temp[tid] = []; atomicState.changeAlerts = temp; temp = [:]; }
	temp = atomicState.changeAttrs
    if (temp) { temp[tid] = []; atomicState.changeAttrs = temp; temp = [:]; }
    temp = atomicState.changeAudio
    if (temp) { temp[tid] = []; atomicState.changeAudio = temp; temp = [:]; }
	temp = atomicState.changeCloud
    if (temp) { temp[tid] = []; atomicState.changeCloud = temp; temp = [:]; }
	temp = atomicState.changeConfig
    if (temp) { temp[tid] = []; atomicState.changeConfig = temp; temp = [:]; }
	temp = atomicState.changeDevice
    if (temp) { temp[tid] = []; atomicState.changeDevice = temp; temp = [:]; }
	temp = atomicState.changeEquip
    if (temp) { temp[tid] = []; atomicState.changeEquip = temp; temp = [:]; }
	temp = atomicState.changeNever
    if (temp) { temp[tid] = []; atomicState.changeNever = temp; temp = [:]; }
	temp = atomicState.changeOften
    if (temp) { temp[tid] = []; atomicState.changeOften = temp; temp = [:]; }
	temp = atomicState.changeRarely
    if (temp) { temp[tid] = []; atomicState.changeRarely = temp; temp = [:]; }
	temp = atomicState.changeTemps
    if (temp) { temp[tid] = []; atomicState.changeTemps = temp; temp = [:]; }
}

void pollChildren(String deviceId="",force=false) { 
	// Prevent multiple concurrent poll cycles
	if (atomicState.inPollChildren) {
		def skipTime = atomicState.skipTime ?: now()
		// Give the already running poll 20/25 seconds to complete
		if ((now() - skipTime) < (ST ? 20000 : 25000)) {
			// Already/still polling, capture the arguments and skip this poll request
			if (atomicState.skipTime != skipTime) atomicState.skipTime = skipTime
            if (force) {
            	forceNextPoll()
                if (deviceId) {
                	List forceDevices = atomicState.forceDevices
                	if (forceDevices) {
                    	if (!forceDevices.contains(deviceId)) atomicState.forceDevices = forceDevices + [deviceId]
                    } else {
                    	atomicState.forceDevices = [deviceId]
                    }
                } else atomicState.forceDevices = []
            }
			return
		} else {
			atomicState.skipTime = null
		}
	} else {
		atomicState.inPollChildren = true
        if (atomicState.skipTime) atomicState.skipTime = null
	}

	// Just in case we need to re-initialize anything
	def version = getVersionLabel()
	if (atomicState.versionLabel != version) {
		LOG("Code updated: ${version} - re-initializing",1,null,'trace')
		atomicState.versionLabel = version
		atomicState.inPollChildren = false
        // runIn(2, updated, [overwrite: true])
        updated()
		return
	}

    // Start the new poll cycle
    atomicState.pollEcobeeAPIStart = now()
	LOG("Checking for updates...",1,null,'trace')
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("pollChildren(${deviceId}, ${force})", 1, null, 'trace')
	boolean forcePoll
	String thermostatsToPoll 
	
	def updatesLog = atomicState.updatesLog
    if (force || updatesLog.forcePoll) {
        updatesLog.forcePoll = true	
        forcePoll = true			
        atomicState.updatesLog = updatesLog
        if (deviceId) {
            thermostatsToPoll = deviceId
            clearChangeLogs(deviceId)	// A *real* forcePoll - clearing the change logs forces everything to be resent
        }
        List forceDevices = atomicState.forceDevices
        if (forceDevices) {
            forceDevices.each { String did ->
                if (!deviceId || (deviceId != did)) {
                    clearChangeLogs(did)
                    thermostatsToPoll = thermostatsToPoll ? (thermostatsToPoll + ',' + did) : did
                }
            }
            atomicState.forceDevices = []
        }
        if (!thermostatsToPoll) thermostatsToPoll =  getChildThermostatDeviceIdsString()
	} else {
		forcePoll = updatesLog.forcePoll
		thermostatsToPoll = getChildThermostatDeviceIdsString()
	}
	if (debugLevelFour) LOG("=====> pollChildren(${deviceId}) - forcePoll(${forcePoll}) atomicState.lastPoll(${atomicState.lastPoll}) now(${now()}) atomicState.lastPollDate(${atomicState.lastPollDate})", 1, null, "trace")
	
	if(apiConnected() == "lost") {
		// Possibly a false alarm? Check if we can update the token with one last fleeting try...
		if (debugLevel(3)) LOG("apiConnected() == lost, try to do a recovery, else we are done...", 3, null, "debug")
		if( refreshAuthToken() ) { 
			// We are back in business!
			LOG("pollChildren() - Was able to recover the lost API connection. Please ignore any notifications received.", 1, null, "warn")
		} else {		
			LOG("pollChildren() - Unable to poll do to loss of API Connection. Please ensure you are authorized.", 1, null, "error")
			atomicState.inPollChildren = false
			return
		}
	}

	// Run a watchdog checker here
	scheduleWatchdog(null, true)	
	
	if (settings.thermostats?.size() < 1) {
		LOG("pollChildren() - Nothing to poll as there are no thermostats currently selected", 1, null, "warn")
		atomicState.inPollChildren = false
		return
	}	 
	
	// Check if anything has changed in the thermostatSummary (really don't need to call EcobeeAPI if it hasn't).
	boolean somethingChanged = forcePoll ?: checkThermostatSummary(thermostatsToPoll)
	if (!forcePoll) thermostatsToPoll = atomicState.changedThermostatIds
	
	if (somethingChanged) {
		//List tids = []
		//tids = thermostatsToPoll.split(",")
		//String names = ""
		//tids.each { names = names == "" ? getThermostatName(it) : names+", "+getThermostatName(it) }
		//LOG("Polling thermostat${thermostatsToPoll.contains(',')?'s':''} ${names} (${thermostatsToPoll})${forcePoll?' (forced)':''}", 2, null, 'info')
        LOG("Polling ${thermostatsToPoll} ${forcePoll?'(forced)':''}",2,null,'info')
		pollEcobeeAPI(thermostatsToPoll)		// This will queue the async request, and values will be generated and sent from pollEcobeeAPICallback
	} else {	 
		LOG('No updates...', 2, null, 'trace')
        atomicState.inPollChildren = false
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
	def startMS = now()
	def stats = atomicState.thermostats
	def sensors = atomicState.remoteSensorsData
	stats?.each { DNI ->
		if (DNI.value?.data) getChildDevice(DNI.key).generateEvent(DNI.value.data as List)
	}
	sensors?.each { DNI ->
		if (DNI.value?.data) getChildDevice(DNI.key).generateEvent(DNI.value.data)
	}
	def allDone = now()
    def deviceUpdates = allDone - startMS
	def howLongItTook = allDone - atomicState.pollEcobeeAPIStart // - deviceUpdates
	LOG("Updates sent (${deviceUpdates} / ${howLongItTook}ms)",2,null,"${howLongItTook > 25000 ? 'warn': 'trace'}")
}

// enables child SmartApps to send events to child Smart Devices using only the DNI
void generateChildEvent( childDNI, dataMap) {
	getChildDevice(childDNI)?.generateEvent([dataMap])	// put the Map inside a List
}

// NOTE: This only updates the apiConnected state now - this used to resend all the data, but that's pretty much a waste of CPU cycles now.
//		 If the UI needs updating, the refresh now does a forcePoll on the entire device.
void generateEventLocalParams() {
	// Iterate over all the children
	
	def apiConnection = apiConnected()
	String lastPoll = (debugLevel(4)) ? "${apiConnection} @ ${atomicState.lastPollDate}" : (apiConnection=='full') ? 'Succeeded' : (apiConnection=='warn') ? 'Timed Out' : 'Failed'
	def data = [ [apiConnected: apiConnection],
				 [lastPoll: lastPoll]
			   ]
	String LOGtype = (apiConnection == 'lost') ? 'error' : ((apiConnection == 'warn') ? 'warn' : 'info')
	if (debugLevel(2)) LOG("Updating API status with ${data}${LOGtype=='warn'?' - will retry':''}", 2, null, LOGtype)
	settings.thermostats?.each {
		getChildDevice(it)?.generateEvent(data)
	 }
}

// Checks if anything has changed since the last time this routine was called, using lightweight thermostatSummary poll
// NOTE: Despite the documentation, Runtime Revision CAN change more frequently than every 3 minutes - equipmentStatus is 
//		 apparently updated in real time (or at least very close to real time)
boolean checkThermostatSummary(String thermostatIdsString) {
	//def startMS 
	//if (TIMERS) startMS = now()
	def updatesLog 				= atomicState.updatesLog
	boolean thermostatUpdated 	= updatesLog.thermostatUpdated
	boolean alertsUpdated 		= updatesLog.alertsUpdated
	boolean runtimeUpdated 		= updatesLog.runtimeUpdated
	boolean getAllStats 		= (runtimeUpdated || thermostatUpdated || alertsUpdated )
	
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("checkThermostatSummary() - ${thermostatIdsString}",1,null,'trace')
	if (thermostatIdsString == '') thermostatIdsString = getChildThermostatDeviceIdsString()
	String jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + thermostatIdsString + '","includeEquipmentStatus":"true"}}'
	def pollParams = [
			uri: apiEndpoint,
			path: "/1/thermostatSummary",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
			query: [format: 'json', body: jsonRequestBody],
            timeout: 30
	]
	
	boolean result = false
	def statusCode = 999
	String tstatsStr = ''	 
	if (debugLevelFour) LOG("checkThermostatSummary() - pollParams: ${pollParams}",1,null,'trace')
	try {
		httpGet(pollParams) { resp ->
        	if (!resp || !resp.isSuccess()) {
            	LOG("checkThermostatSummary() - invalid response - skipping...", 1, null, 'warn')
            }
            else if(resp.status && (resp.status == 200)) {
				if (debugLevelFour) LOG("checkThermostatSummary() - poll results returned resp.data ${resp.data}", 1, null, 'trace')
				// This is crazy, I know, but ST & HE return different junk on a failed call, thus the exhaustive verification to avoid throwing errors
				if (resp.data?.status?.containsKey('code')) { //  && (resp.data.status.code != null)) {
				//if ((resp.data != null) && (resp.data instanceof Map) && resp.data.containsKey('status')  && (resp.data.status != null) 
				//	&& (resp.data.status instanceof Map) && resp.data.status.containsKey('code') && (resp.data.status.code != null)) {
					statusCode = resp.data.status.code
				} else {
					LOG("checkThermostatSummary() - malformed response - skipping...", 1, null, 'warn')
				}
				if ((statusCode != null) && (statusCode == 0)) { 
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
				} else if ((statusCode != null) && [14, 1, 2, 3].contains(statusCode)) {
					LOG("checkThermostatSummary() - status code: ${statusCode}, message: ${resp.data.status.message}", 2, null, 'warn')
					atomicState.inPollChildren = false
					if (atomicState.skipTime) atomicState.skipTime = null
					//runIn(2, refreshAuthToken, [overwrite: true])
					if ( refreshAuthToken() ) {
						// Note that refreshAuthToken will reschedule pollChildren if it succeeds in refreshing the token...
						LOG('Checking: Auth_token refreshed', 2, null, 'info')
					} else {
						LOG('Checking: Auth_token refresh failed', 1, null, 'warn')
					}
				} else {
					// if we get here, we had http status== 200, but API status != 0
					atomicState.inPollChildren = false
					if (statusCode != 999) LOG("checkThermostatSummary() - status code ${statusCode}, message: ${resp.data.status.message}", 1, null, 'warn')
					//runIn(2, refreshAuthToken, [overwrite: true])
				}
			} else {
				LOG("checkThermostatSummary() - poll success: true, status ${resp?.status}", 1, null, "warn")
			}
			//if (resp) resp = null
		}
		if (atomicState.inTimeoutRetry) atomicState.inTimeoutRetry = 0
	} catch (groovyx.net.http.HttpResponseException e) {   
		result = false // this thread failed to get the summary
		def iStatus = e?.statusCode ? e.statusCode.toInteger() : null
		if (iStatus && (iStatus == 500) && (e?.response?.data?.status?.code == 14)) {
			LOG('checkThermostatSummary() - HttpResponseException occurred: Auth_token has expired', 3, null, "info")
			atomicState.action = "pollChildren"
			if (debugLevelFour) LOG( "Refreshing your auth_token!", 1, null, 'trace')
			if (atomicState.inTimeoutRetry) atomicState.inTimeoutRetry = 0
			atomicState.inPollChildren = false
			if ( refreshAuthToken() ) {
				// Note that refreshAuthToken will reschedule pollChildren if it succeeds in refreshing the token...
				LOG('Checking: Auth_token refreshed', 2, null, 'info')
			} else {
				LOG('Checking: Auth_token refresh failed', 1, null, 'warn')
			}
		} else if (((iStatus == null) && (e?.message?.contains('timed out') || e?.message?.contains('timeout'))) || 
				   (iStatus && (((iStatus > 400) && (iStatus < 405)) || (iStatus == 408) || (iStatus > 500)))) { //((iStatus > 500) && (iStatus < 505))) {
			// Retry on transient, recoverable error codes (timeouts, server temporarily unavailable, etc.) see https://en.wikipedia.org/wiki/List_of_HTTP_status_codes 
			if (iStatus && (iStatus != 525)) {
            	LOG("checkThermostatSummary() - httpGet error: ${iStatus}, ${e?.message}, ${e.response?.data} - will retry", 1, null, 'warn')	// Just log it, and hope for better next time...
            } else {
            	LOG("checkThermostatSummary() - httpGet timeout - will retry", 1, null, 'warn')
            }
			//log.debug "checkThermostatSummary() - <b>ERROR</b> e: ${e}, \nmessage: ${e.message}, \nstatusCode: ${e.statusCode}, \nresponse.data: ${e.response?.data}"
			if (apiConnected() != 'warn') {
				atomicState.connected = 'warn'
				updateMyLabel()
				atomicState.lastPoll = now()
				atomicState.lastPollDate = getTimestamp()
				generateEventLocalParams()
			}
			def inTimeoutRetry = atomicState.inTimeoutRetry
			if (inTimeoutRetry == null) inTimeoutRetry = 0
			atomicState.inPollChildren = false
			if (inTimeoutRetry < 3) runIn(watchdogInterval, pollChildren, [overwrite: true])
			atomicState.inTimeoutRetry = inTimeoutRetry + 1
			//return result
		} else /* can we handle any other errors??? */ {
			//log.debug "checkThermostatSummary() - ERROR e: ${e}, \nmessage: ${e.message}, \nstatusCode: ${e.statusCode}, \nresponse.data: ${e.response?.data}"
			LOG("checkThermostatSummary() - httpGet error: ${iStatus}, ${e?.message}, ${e.response?.data} - won't retry", 1, null, "error")
		}
        if (resp) resp = null
	} catch (java.util.concurrent.TimeoutException e) {
		LOG("checkThermostatSummary() - Concurrent Execution Timeout: ${e} - will retry", 1, null, "warn")
		// Do not add an else statement to run immediately as this could cause an long looping cycle
		runIn(watchdogInterval, pollChildren, [overwrite: true])
        if (resp) resp = null
		result = false
	// These appear to be transient errors, treat them all as if a Timeout... 
	} catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException | 
			javax.net.ssl.SSLPeerUnverifiedException | javax.net.ssl.SSLHandshakeException | 
			java.net.SocketTimeoutException | java.net.NoRouteToHostException | java.net.UnknownHostException |
			groovyx.net.http.ResponseParseException | java.lang.reflect.UndeclaredThrowableException e) {
		LOG("checkThermostatSummary() - ${e} - will retry",1,null,'warn')  // Just log it, and hope for better next time...
		if (apiConnected() != 'warn') {
			atomicState.connected = 'warn'
			updateMyLabel()
			atomicState.lastPoll = now()
			atomicState.lastPollDate = getTimestamp()
			generateEventLocalParams()
		}
		def inTimeoutRetry = atomicState.inTimeoutRetry
		if (inTimeoutRetry == null) inTimeoutRetry = 0
		atomicState.inPollChildren = false
		if (inTimeoutRetry < 3) runIn(watchdogInterval, pollChildren, [overwrite: true])
		atomicState.inTimeoutRetry = inTimeoutRetry + 1
        if (resp) resp = null
		result = false
		// throw e
	} catch (Exception e) {
		LOG("checkThermostatSummary() - General Exception: ${e}, success: ${resp?.isSuccess()}, status: ${resp?.status}, data: ${resp?.data} - skipping", 1, null, "warn")
		result = false
        if (resp) resp = null
		//throw e
	}
	
	atomicState.updatesLog = updatesLog
	if (debugLevelFour) LOG("<===== Leaving checkThermostatSummary() result: ${result}, tstats: ${tstatsStr}", 1, null, "info")
	//if (TIMERS) log.debug "TIMER: checkThermostatSummary done (${now()-startMS}ms)"
    if (resp) resp = null
	return result
}

boolean pollEcobeeAPI(thermostatIdsString = '') {
	// boolean timers = atomicState.timers
	//def pollEcobeeAPIStart = now()
	//atomicState.pollEcobeeAPIStart = now() //pollEcobeeAPIStart
	
	boolean debugLevelFour = debugLevel(4)
	boolean debugLevelThree = debugLevel(3)
	if (debugLevelFour) LOG("=====> pollEcobeeAPI() entered - thermostatIdsString = ${thermostatIdsString}", 1, null, "info")

	def updatesLog 				= atomicState.updatesLog
	boolean forcePoll			= updatesLog.forcePoll		// lightweight way to use atomicStates that we don't want to change under us
	boolean thermostatUpdated	= updatesLog.thermostatUpdated
	boolean alertsUpdated		= updatesLog.alertsUpdated
	boolean runtimeUpdated 		= updatesLog.runtimeUpdated
	boolean getWeather			= updatesLog.getWeather
	
	//String preText = debugLevel(2) ? '' : 'pollEcobeeAPI() - '
	boolean somethingChanged 	= (thermostatIdsString != '')
	String allMyChildren 		= getChildThermostatDeviceIdsString()
	String checkTherms 			= thermostatIdsString ?: allMyChildren

	// forcePoll = true
	if (thermostatIdsString == '') {
		somethingChanged 		= checkThermostatSummary(thermostatIdsString)
		if (somethingChanged) {
			updatesLog 			= atomicState.updatesLog
			thermostatUpdated	= updatesLog.thermostatUpdated				// update these again after checkThermostatSummary
			alertsUpdated		= updatesLog.alertsUpdated
			runtimeUpdated		= updatesLog.runtimeUpdated
		}
		//if (TIMERS) log.debug "TIMER: checkThermostatSummary('') complete, changes=${somethingChanged} @ ${now() - pollEcobeeAPIStart}ms"
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
	}
	
	// if nothing has changed, and this isn't a forced poll, just return (keep count of polls we have skipped)
	// This probably can't happen anymore...shouldn't even be here if nothing has changed and not a forced poll...
	if (!somethingChanged && !forcePoll) {	
		if (debugLevelFour) LOG("<===== Leaving pollEcobeeAPI() - nothing changed, skipping heavy poll...", 4 )
        atomicState.inPollChildren = false
		return true		// act like we completed the heavy poll without error
	} else {
		// if getting the weather, get for all thermostats at the same time. Else, just get the thermostats that changed
		checkTherms = (getWeather) ? allMyChildren : checkTherms // (forcePoll ? checkTherms : atomicState.changedThermostatIds)
	}  
	
	// Let's only check those thermostats that actually changed...unless this is a forcePoll - or if we are getting the weather 
	// (we need to get weather for all therms at the same time, because we only update every 15 minutes and use the cached version
	// the rest of the time)
	
	// get equipmentStatus every time
	String jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + checkTherms + '","includeEquipmentStatus":"true"'
	String gw = '( equipmentStatus'
	if (thermostatUpdated || forcePoll) {
		jsonRequestBody += ',"includeSettings":"true","includeProgram":"true","includeEvents":"true","includeAudio":"true"'
		gw += ' settings program events audio'
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
		jsonRequestBody += ',"includeRuntime":"true","includeExtendedRuntime":"true"'	// "includeEnergy":"true"'
		gw += ' runtime extendedRuntime' // energy'
        //stat[xxxxxxxxx].energy: [tou:[featureState:off, savings:basic], energyFeatureState:off, feelsLikeMode:temp, comfortPreferences:] // fLM can be 'humidex'
		
		// Always get extended runtime (we only save the 2 datapoints we need anymore)
		//if (atomicState.needExtendedRuntime) {
		//	jsonRequestBody += ',"includeExtendedRuntime":"true"'
		//	gw += ' extendedRuntime'
		//}

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
	//def tids = []
	//tids = checkTherms.split(",")
	//String names = ""
	//tids.each { names = (names=="") ? getThermostatName(it) : names+", "+getThermostatName(it) }
	//LOG("Requesting ${gw} for thermostat${checkTherms.contains(',')?'s':''} ${names} (${checkTherms})", 2, null, 'info')
    LOG("Requesting ${gw} for thermostats ${checkTherms}",2,null,'info')
	jsonRequestBody += '}}'	  
	if (debugLevelFour) LOG("pollEcobeeAPI() - jsonRequestBody is: ${jsonRequestBody}", 1, null, 'trace')
 
	//boolean ST = atomicState.isST
	
	def pollParams = [
		uri: apiEndpoint,
		path: "/1/thermostat",
		headers: ["Content-Type": "${ST?'application':'text'}/json", "Authorization": "Bearer ${atomicState.authToken}"],
		query: [format: 'json', body: jsonRequestBody, 'contenttype': "${ST?'application':'text'}/json" ],
        timeout: 30
	]
	def pollState = [
		thermostatIdsString: thermostatIdsString,
		checkTherms:		 checkTherms,
	]
	if (debugLevelFour) pollState += [thermostatIdsString: thermostatIdsString]
	
	//if (TIMERS) { def tNow = now(); log.debug "TIMER: asyncPollPrep done (${tNow - atomicState.pollEcobeeAPIStart}ms)"; atomicState.asyncPollStart = tNow; }
	boolean result = true
	try {
		if (ST) {
			include 'asynchttp_v1'
			asynchttp_v1.get( pollEcobeeAPICallback, pollParams, pollState )
		} else {
			asynchttpGet( pollEcobeeAPICallback, pollParams, pollState )
		}
		//atomicState.waitingForCallback = true
		// return true
	} catch (Exception e) {
		LOG("pollEcobeeAPI() - General Exception: ${e}", 1, null, "error")
        atomicState.inPollChildren = false
		result = false
	}
	return result
}

boolean pollEcobeeAPICallback( resp, pollState ) {
	def startMS = now()
	//atomicState.waitingForCallback = false
	//boolean timers = atomicState.timers
	//def pollEcobeeAPIStart
	//if (TIMERS) { pollEcobeeAPIStart = atomicState.pollEcobeeAPIStart; log.debug "TIMER: asyncPoll took ${startMS - atomicState.asyncPollStart}ms"; log.debug "TIMER: Poll callback total time ${startMS - pollEcobeeAPIStart}ms"; }
	boolean debugLevelFour = debugLevel(4)
	boolean debugLevelThree = debugLevelFour ?: debugLevel(3)
	//boolean ST = atomicState.isST
    
	def updatesLog 				= atomicState.updatesLog
	boolean forcePoll 			= updatesLog.forcePoll
	boolean thermostatUpdated	= updatesLog.thermostatUpdated
	boolean alertsUpdated 		= updatesLog.alertsUpdated
	boolean runtimeUpdated		= updatesLog.runtimeUpdated
	boolean getWeather			= updatesLog.getWeather
	
	//String preText = getDebugLevel() <= 2 ? '' : 'pollEcobeeAPICallback() - '
	String thermostatIdsString	= debugLevelFour ? pollState.thermostatIdsString : ''
	def checkTherms 			= pollState.checkTherms
	if (debugLevelFour) LOG("=====> pollEcobeeAPICallback() entered - thermostatIdsString = ${thermostatIdsString}", 1, null, "info")
	
	boolean result = true
	def tidList = []
	if (debugLevelFour) LOG("pollEcobeeAPICallback() - status code ${resp?.status}",4,null,'trace')
	
	// Update the atomicState caches for each received data object(s)
	// NB. Hubitat defaults Maps to be LazyMaps (hash not built until first key is referenced). Since we know we will be using these
	// as key/value pairs, it's faster to build the hashes as we add things. 
	def tempSettings 		= [:] as HashMap
	def tempProgram 		= [:] as HashMap
	def tempEvents 			= [:] as HashMap
	def tempRuntime 		= [:] as HashMap
	def tempExtendedRuntime = [:] as HashMap
	def tempWeather 		= [:] as HashMap
	def tempSensors 		= [:] as HashMap
	def tempEquipStat 		= [:] as HashMap
	def tempLocation 		= [:] as HashMap
	def tempAudio 			= [:] as HashMap
	def tempStatInfo 		= [:] as HashMap
	def tempAlerts 			= [:] as HashMap
	def tempSchedule		= [:]
	def tempClimates		= [:] as HashMap
	def tempCurrentClimateRef= [:]
	def statUpdates 		= [:].withDefault {[]}

	def tempStatTime 		= atomicState.statTime
	def latestRevisions 	= atomicState.latestRevisions

	boolean programUpdated 	= false
	boolean settingsUpdated = false
	boolean eventsUpdated 	= false
	boolean locationUpdated = false
	boolean audioUpdated 	= false
	boolean extendRTUpdated = false
	boolean sensorsUpdated 	= false
	boolean weatherUpdated 	= false
	boolean statInfoUpdated = false
	boolean equipUpdated 	= false
	boolean rtReallyUpdated = false
	boolean scheduleUpdated = false		// means both atomicState.schedule AND atomicState.timeSchedule were updated
	boolean climatesUpdated = false
	boolean curClimRefUpdated = false
	
	if (resp && resp.status && (resp.status == 200)) {
		try {
			if (!resp.json) {
				// FAIL - no data
				LOG("pollEcobeeAPICallback() - poll - no JSON: ${resp.data}", 1, null, 'error')
				result = false
				//return false
			}
		} catch (Exception e) {
			LOG("pollEcobeeAPICallback() - General Exception: ${e}", 1, null, "error")
			atomicState.reAttemptPoll = atomicState.reAttemptPoll + 1
			if (atomicState.reAttemptPoll > 3) {		
				apiLost("pollEcobeeAPICallback() - Too many retries (${atomicState.reAttemptPoll - 1}) for polling.")
			} else {
				LOG('pollEcobeeAPICallback() - Setting up retryPolling',1,null,'debug')
				runIn(watchdogInterval, pollChildren, [overwrite: true]) 
			}
			//throw e
			//return false
			result = false
		} 
		
		if (result && (atomicState.reAttemptPoll > 0)) apiRestored()
			

		
		// def tempOemCfg = [:] as HashMap
		//if (TIMERS) log.debug "TIMER: pollEcobeeAPICallback() initialized @ (${now() - pollEcobeeAPIStart}ms)"

		// collect the returned data into temporary individual caches (because we can't update individual Map items in an atomicState Map)	
		// if (resp && resp.containsKey(data) && resp.data.containsKey(thermostatList)) { // oddly, this occaisionally returns no thermostatList :(
		if (result && resp?.json?.thermostatList) { // oddly, this sometimes returns no thermostatList :(
			resp.json.thermostatList.each { stat ->
				String tid = stat.identifier.toString()
				tidList += [tid]
                statUpdates[tid] = [] //.add('foo') // as List	// initialize the list of things that changed...
                
				//String tstatName = getThermostatName(tid)
				if (debugLevelThree) LOG("pollEcobeeAPICallback() - Parsing data for thermostat ${getThermostatName(tid)} (${tid})", 3, null, 'info')
				//if (TIMERS) log.debug "TIMER: Parsing data for ${tstatName} @ (${now() - pollEcobeeAPIStart}ms)"
				
				tempEquipStat[tid] = 	stat.equipmentStatus			// always store ("" is a valid return value)
				tempStatTime[tid] = 	stat.thermostatTime
						
				if (thermostatUpdated || forcePoll ) {
					if (stat.settings)	tempSettings[tid]	= stat.settings as HashMap
                    if (stat.program) {
                    // Break program down into smaller chunks, since most of it never changes
                    	tempClimates[tid] 				 	= stat.program.climates // as HashMap
                        tempSchedule[tid]					= stat.program.schedule
                        tempCurrentClimateRef[tid]			= stat.program.currentClimateRef
                   	}
                    //log.debug "stat.events: ${stat.events}
                    
					if (stat.events)	tempEvents[tid]		= stat.events // It's a list of Maps
					if (stat.location)	tempLocation[tid]	= stat.location as HashMap
                    if (stat.audio)		tempAudio[tid]		= stat.audio as HashMap
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
											desiredDehumidity:	stat.runtime.desiredDehumidity,
											desiredFanMode:		stat.runtime.desiredFanMode,
											desiredHeat:		stat.runtime.desiredHeat,
											desiredHumidity:	stat.runtime.desiredHumidity,
											disconnectDateTime:	stat.runtime.disconnectDateTime,
										   ]
					}
					if (stat.extendedRuntime) {
						// We only need 2 values from extendedRuntime (at the moment)
						// log.debug "extRT: ${stat.extendedRuntime}"
						tempExtendedRuntime[tid] = [desiredHumidity:	stat.extendedRuntime.desiredHumidity[2], 
													desiredDehumidity:	stat.extendedRuntime.desiredDehumidity[2]]
					}
					if (stat.remoteSensors)		tempSensors[tid] =		stat.remoteSensors
					//if (stat.energy) log.debug "stat[${tid}].energy: ${stat.energy}"
				}
				atomicState.latestRevisions = latestRevisions
                
				// let's don't copy all that weather info...we only need some 3 bits of that info
				if (getWeather || forcePoll) {
					if (stat.weather) {
                    	// Find the lowest temperature forecasted for today, today+1 (2 days), today+2 (3 days), ... today+4 (5 days) - (used by Smart Humidity helper)
                        def low24 = stat.weather.forecasts[0].temperature	// Temperature right now
                        for (int j=5; j<9; j++) {
                        	if (stat.weather.forecasts[j].temperature < low24) low24 = stat.weather.forecasts[j].temperature 	// Temperature forecast for priorTempTime+6 hours (iterated)
                        }
                        low24 = low24 / 10.0
                        def low1Day = stat.weather.forecasts[1].tempLow / 10.0
                        def low2Day = stat.weather.forecasts[2].tempLow / 10.0
                        def low3Day = stat.weather.forecasts[3].tempLow / 10.0
                        def low4Day = stat.weather.forecasts[4].tempLow / 10.0

						tempWeather[tid] = [timestamp:		stat.weather.timestamp,
											temperature:	stat.weather.forecasts[0]?.temperature.toString(),
											humidity:		stat.weather.forecasts[0]?.relativeHumidity,
                                            tempLowForecast:"${low24},${low1Day},${low2Day},${low3Day},${low4Day}",		// Send as °F - let the Helper do the conversion if needed
											dewpoint:		stat.weather.forecasts[0]?.dewpoint,
											pressure:		stat.weather.forecasts[0]?.pressure,
											weatherSymbol:	stat.weather.forecasts[0]?.weatherSymbol.toString()
                                          ]
					}
				}
				if (forcePoll || alertsUpdated) {
					tempAlerts[tid] = stat.alerts
				}
			} // stat ->
		} else {
			LOG("pollEcobeeAPICallback() - poll: no thermostatList: ${resp.json}", 1, null, 'error')
			result = false
		}
	} else if (resp && resp.hasError()) {
		result = false
		if (debugLevelFour) LOG("pollEcobeeAPICallback() - Poll http status ${resp.status}, ${resp.errorMessage}", 1, null, "error")
		def iStatus = resp.status ? resp.status?.toInteger() : null
		// Handle recoverable / retryable errors
		if (iStatus && (iStatus == 500)) {
			if (debugLevelFour) LOG("pollEcobeeAPICallback() - Poll exception: 500, ${resp.errorMessage}, ${resp.errorData}", 2, null, "warn")
			// Ecobee server error
			def statusCode
			if (resp.errorData) {
				def errorJson = new JsonSlurper().parseText(resp.errorData)
				statusCode = errorJson?.status?.code
			}
			if (statusCode?.toInteger() == 14) {
				// Auth_token expired
				if (debugLevelThree) LOG("Polling: Auth_token expired", 3, null, "trace")
				atomicState.action = "pollChildren"
				atomicState.inPollChildren = false
				if ( refreshAuthToken() ) { 
					// Note that refreshAuthToken will reschedule pollChildren if it succeeds in refreshing the token...
					LOG( 'Polling: Auth_token refreshed', 2, null, 'info')
				} else {
					LOG( 'Polling: Auth_token refresh failed', 1, null, 'warn')
				}
				if (atomicState.inTimeoutRetry) atomicState.inTimeoutRetry = 0
				//return result
			} else { 
				// All other Ecobee Server (500) errors here
				LOG("pollEcobeeAPICallback() - Poll exception: 500, statusCode: ${statusCode} (${resp.errorMessage}: ${resp.errorData}) - won't retry", 1, null, 'error')
				// Don't retry for now...may change in the future
				//return result
			}
		} else if (((iStatus == null) && (resp.errorMessage?.contains('connection timed out') || resp.errorMessage?.contains('Read timeout'))) || 
				   (iStatus && ((iStatus == 200) || ((iStatus > 400) && (iStatus < 405)) || (iStatus == 408) || (iStatus > 500)))) { //((iStatus > 500) && (iStatus < 505))) {
			// Retry on transient, recoverable error codes (timeouts, server temporarily unavailable, parsing errors, etc.) see https://en.wikipedia.org/wiki/List_of_HTTP_status_codes 
			if (iStatus && (iStatus != 525)) {
            	LOG("pollEcobeeAPICallback() - Poll exception: ${iStatus}, ${resp.errorMessage}, ${resp.errorData}, - will retry", 1, null, 'warn')	// Just log it, and hope for better next time...
            } else {
            	LOG("pollEcobeeAPICallback() - Poll timeout - will retry", 1, null, 'warn')
            }
			if (apiConnected() != 'warn') {
				atomicState.connected = 'warn'
				updateMyLabel()
				atomicState.lastPoll = now()
				atomicState.lastPollDate = getTimestamp()
				generateEventLocalParams()
			}
			def inTimeoutRetry = atomicState.inTimeoutRetry
			if (inTimeoutRetry == null) inTimeoutRetry = 0
			if (inTimeoutRetry < 3) runIn(watchdogInterval, pollChildren, [overwrite: true])
			atomicState.inTimeoutRetry = inTimeoutRetry + 1
			//return result
		} else /* can we handle any other errors??? */ {
			LOG("pollEcobeeAPICallback() - Poll exception: ${iStatus}, ${resp.errorMessage}, ${resp.errorJson?.status?.message}, ${resp.errorJson?.status?.code} - won't retry", 1, null, "error")
		}
		// For now, the code here just logs the error and assumes that checkThermostatSummary() will recover from the unhandled errors on the next call.
		//return result
	} else {
		result = false
		if (!resp) {
			LOG("pollEcobeeAPICallback() - no response data - skipping...",1,null,'error')
		} else if (resp.status?.toInteger() != 200) {
			LOG("pollEcobeeAPICallback() - unexpected response status: ${resp.status}",1,null,'error')
		} else {
			LOG("pollEcobeeAPICallback() - UNKNOWN EXCEPTION!!!, ${resp}",1,null,'error')	// Can't get here!!!
		}
	}

	if (result) {
        //if (TIMERS) log.debug "TIMER: Loading complete @ (${now() - pollEcobeeAPIStart}ms)"
		// OK, we have all the data received stored in their appropriate temporary caches, let's update the atomicState stores
		// ** Equipment Status **
		def tempAtomic = null
		if (tempEquipStat) {
			tempAtomic = atomicState.equipmentStatus
			if (tempAtomic) {
				tidList.each { tid ->
					if (tempAtomic[tid] != tempEquipStat[tid]) {
						equipUpdated = true
                        statUpdates[tid].add('equip')
						tempAtomic[tid] = tempEquipStat[tid]
					}
				}
				if (equipUpdated) {
					atomicState.equipmentStatus = tempAtomic
				}
                tempAtomic = null // release the memory
			} else {
				equipUpdated = true
                tempEquipStat.each { key, value ->
                	statUpdates[key].add('equip')
                }
				atomicState.equipmentStatus = tempEquipStat
			}
            tempEquipStat = null
		}
		//if (TIMERS) log.debug "TIMER: equipmentStatus complete @ (${now() - pollEcobeeAPIStart}ms)"
		
		// ** Thermostat Status (Program, Events, Location, Audio) **
		// OK, Only copy the thermostat stuff (program, events, settings, audio or location) that has changed
		if ((thermostatUpdated || forcePoll)) {
        	// ** Program **
        	// We now store program as 3 parts: currentClimateRef, schedule, climates
			if (tempCurrentClimateRef) {
				tempAtomic = atomicState.currentClimateRef
                if (tempAtomic) {
                	tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempCurrentClimateRef[tid])) {
                        	curClimRefUpdated = true
                            statUpdates[tid].addAll(['program','curClimRef'])
                            tempAtomic[tid] = tempCurrentClimateRef[tid]
                        }
                    }
                    if (curClimRefUpdated) {
						atomicState.currentClimateRef = tempAtomic
					}
                    tempAtomic = null	// release the memory
				} else {
					curClimRefUpdated = true
                    tempCurrentClimateRef.each { key, value ->
                    	statUpdates[key].addAll(['program','curClimRef'])
                    }
					atomicState.currentClimateRef = tempCurrentClimateRef
				}
                tempCurrentClimateRef = null
            }
            if (tempSchedule) {
            	tempAtomic = atomicState.schedule
                if (tempAtomic) {
                	tidList.each { tid ->
                    	if (!tempAtomic[tid] || (tempAtomic[tid] != tempSchedule[tid])) {
                        	scheduleUpdated = true
                            statUpdates[tid].addAll(['program','schedule'])
                            tempAtomic[tid] = tempSchedule[tid]
                        }
                    }
					if (scheduleUpdated) {
						atomicState.schedule = tempAtomic
                        tempSchedule = tempAtomic			// We'll need this to build the timeSchedule update below
					}
                    tempAtomic = null
				} else {
					scheduleUpdated = true
                    tempSchedule.each { key, value ->
                    	statUpdates[key].addAll(['program','schedule'])
                    }
					atomicState.schedule = tempSchedule
				}
            }
            if (tempClimates) {
            	tempAtomic = atomicState.climates as HashMap
                if (tempAtomic) {
                	tidList.each { tid ->
                    	if (!tempAtomic[tid] || (tempAtomic[tid] != tempClimates[tid])) {
                        	climatesUpdated = true
                            statUpdates[tid].addAll(['program','climates'])
                            tempAtomic[tid] = tempClimates[tid]
                        }
                    }
                    if (climatesUpdated) {
                    	atomicState.climates = tempAtomic
                        tempClimates = tempAtomic	// We also need this for updating timeSchedule below
                    }
                    tempAtomic = null
                } else {
                	climatesUpdated = true
                    tempClimates.each { key, value ->
                    	statUpdates[key].addAll(['program','climates'])
                    }
                    atomicState.climates = tempClimates
                }
            } //else {
            	// if (atomicState.programUpdatedByAPI) {}
            //}
            // Now we build the (more efficient) timeSchedule map
            if (scheduleUpdated || !atomicState.timeSchedule) {
            	tempAtomic = atomicState.timeSchedule 
               	tidList.each { tid ->
                	def timeSchedule = convertScheduleToTimeSchedule(tempSchedule[tid], tempClimates[tid]) as LinkedHashMap 
					if (!tempAtomic) {
						tempAtomic = ["${tid}":timeSchedule]
					} else if (!tempAtomic[tid] || (tempAtomic[tid] != timeSchedule)) {
                        tempAtomic[tid] = timeSchedule
                    }
                }
                atomicState.timeSchedule = tempAtomic
                tempAtomic = null	// And NOW we can release the memory
                tempSchedule = null
                tempClimates = null
			}
            programUpdated = (curClimRefUpdated || climatesUpdated || scheduleUpdated)		// just in case
            
			if (tempStatInfo) {
				tempAtomic = atomicState.statInfo
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempStatInfo[tid])) {
							statInfoUpdated = true
                            statUpdated[tid].add('statInfo')
							tempAtomic[tid] = tempStatInfo[tid]
						}
					}
					if (statInfoUpdated) {
						atomicState.statInfo = tempAtomic
					}
                    tempAtomic = null	// release the memory
				} else {
					statInfoUpdated = true
                    tempStatInfo.each { key, value ->
                    	statUpdates[key].add('statInfo')
                    }
					atomicState.statInfo = tempStatInfo
				}
                tempStatInfo = null
			}
			if (tempEvents) {
				tempAtomic = atomicState.events
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempEvents[tid])) {
							eventsUpdated = true
                            statUpdates[tid].add('events')
							tempAtomic[tid] = tempEvents[tid]
						}
					}
					if (eventsUpdated) {
						atomicState.events = tempAtomic
					}
                    tempAtomic = null	// release the memory
				} else {
					eventsUpdated = true
                    tempEvents.each { key, value ->
                    	statUpdates[key].add('events')
                    }
					atomicState.events = tempEvents
				}
                tempEvents = null
			}
			if (tempLocation) {
				tempAtomic = atomicState.statLocation
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempLocation[tid])) {
							locationUpdated = true
                            statUpdates[tid].add('location')
							tempAtomic[tid] = tempLocation[tid]
						}
					}
					if (locationUpdated) {
						atomicState.statLocation = tempAtomic
					}
                    tempAtomic = null	// release the memory
				} else {
					locationUpdated = true
                    tempLocation.each { key, value ->
                    	statUpdates[key].add('location')
                    }
					atomicState.statLocation = tempLocation
				}
                tempLocation = null
			}
            if (tempAudio) {
				tempAtomic = atomicState.audio ?: null
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempAudio[tid])) {
							audioUpdated = true
                            statUpdates[tid].add('audio')
							tempAtomic[tid] = tempAudio[tid]
						}
					}
					if (audioUpdated) {
						atomicState.audio = tempAtomic
					}
                    tempAtomic = null	// release the memory
				} else {
					audioUpdated = true
                    tempAudio.each { key, value ->
                    	statUpdates[key].add('audio')
                    }
					atomicState.audio = tempAudio
				}
                tempAudio = null
			}
			//boolean needExtRT = true // atomicState.needExtendedRuntime
			if (tempSettings) {
				tempAtomic = atomicState.settings
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempSettings[tid])) {
							settingsUpdated = true
                            statUpdates[tid].add('settings')
							tempAtomic[tid] = tempSettings[tid]
						}
					}
					if (settingsUpdated) {
						atomicState.settings = tempAtomic
					}
                    tempAtomic = null	// release the memory
				} else {
					settingsUpdated = true
                    tempSettings.each { key, value ->
                    	statUpdates[key].add('settings')
                    }
					atomicState.settings = tempSettings
				}
                tempSettings = null
			}
			//if (TIMERS) log.debug "TIMER: thermostatUpdated complete @ (${now() - pollEcobeeAPIStart}ms)"
		} // end of things that change when "thermostatUpdated"
		
		// ** runtime Status (runtime, extended runtime, sensors, weather) **
		if ((runtimeUpdated || forcePoll)) {
			if (tempRuntime) {
				tempAtomic = atomicState.runtime
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempRuntime[tid])) {
							rtReallyUpdated = true
                            statUpdates[tid].add('rtReally')
							tempAtomic[tid] = tempRuntime[tid]
						}
					}
					if (rtReallyUpdated) {
						atomicState.runtime = tempAtomic
					}
                    tempAtomic = null	// release the memory
				} else {
					rtReallyUpdated = true
                    tempRuntime.each { key, value ->
                    	statUpdates[key].add('rtReally')
                    }
					atomicState.runtime = tempRuntime
				}
                tempRuntime = null
			}
			if (tempExtendedRuntime) {
				tempAtomic = atomicState.extendedRuntime
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempExtendedRuntime[tid])) {
							extendRTUpdated = true
                            statUpdates[tid].add('extendRT')
							tempAtomic[tid] = tempExtendedRuntime[tid]
						}
					}
					if (extendRTUpdated) {
						atomicState.extendedRuntime = tempAtomic
					}
                    tempAtomic = null	// release the memory 
				} else {
					extendRTUpdated = true
                    tempExtendedRuntime.each { key, value ->
                    	statUpdates[key].add('extendRT')
                    }
					atomicState.extendedRuntime = tempExtendedRuntime
				}
                tempExtendedRuntime = null
			}
			if (tempSensors) {
				tempAtomic = atomicState.remoteSensors
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempSensors[tid])) {
							sensorsUpdated = true
                            statUpdates[tid].add('sensors')
							tempAtomic[tid] = tempSensors[tid]
						}
					}
					if (sensorsUpdated) {
						atomicState.remoteSensors = tempAtomic
					}
                    tempAtomic = null	// release the memory 
				} else {
					sensorsUpdated = true
                    tempSensors.each { key, value ->
                    	statUpdates[key].add('sensors')
                    }
					atomicState.remoteSensors = tempSensors
				}
                tempSensors = null
			}
			//if (TIMERS) log.debug "TIMER: runtimeUpdated complete @ (${now() - pollEcobeeAPIStart}ms)"
		} // end of runtimeUpdated items
        
        // ** Weather **
		if ((getWeather || forcePoll)) {
			if (tempWeather) {
				tempAtomic = atomicState.weather
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempWeather[tid])) {
							weatherUpdated = true
                            statUpdates[tid].add('weather')
							tempAtomic[tid] = tempWeather[tid]
						}
					}
					if (weatherUpdated) {
						atomicState.weather = tempAtomic
					}
                    tempAtomic = null	// release the memory 
				} else {
					weatherUpdated = true
                    tempWeather.each { key, value ->
                    	statUpdates[key].add('weather')
                    }
					atomicState.weather = tempWeather
				}
                tempWeather = null
			}
			//if (TIMERS) log.debug "TIMER: Weather complete @ (${now() - pollEcobeeAPIStart}ms)"
		}
        
		// ** Alerts Status **
		if ((alertsUpdated || forcePoll)) {
			if (tempAlerts) {
				alertsUpdated = false
				tempAtomic = atomicState.alerts
				if (tempAtomic) {
					tidList.each { tid ->
						if (!tempAtomic[tid] || (tempAtomic[tid] != tempAlerts[tid])) {
							alertsUpdated = true
                            statUpdates[tid].add('alerts')
							tempAtomic[tid] = tempAlerts[tid]
						}
					}
					if (alertsUpdated) {
						atomicState.alerts = tempAtomic
					}
                    tempAtomic = null	// release the memory //// leave the last one for the system to clean up
				} else {
					alertsUpdated = true
                    tempAlerts.each { key, value ->
                    	statUpdates[key].add('alerts')
                    }
					atomicState.alerts = tempAlerts
				}
                tempAlerts = null
			}
			//if (TIMERS) log.debug "TIMER: Alerts complete @ (${now() - pollEcobeeAPIStart}ms)"
		}

		// OK, Everything is safely stored, let's get to parsing the objects and finding the actual changed data
		//if (result) {
		updateLastPoll()
		if (debugLevelThree) LOG('pollEcobeeAPICallback() - Parsing complete', 3, null, 'info')
		//if (TIMERS) log.debug "TIMER: Parsing complete @ (${now() - pollEcobeeAPIStart}ms)
		updatesLog = [thermostatUpdated:thermostatUpdated, runtimeUpdated:runtimeUpdated, alertsUpdated:alertsUpdated, forcePoll:forcePoll, extendRTUpdated:extendRTUpdated, 
					  equipUpdated:equipUpdated, getWeather:getWeather, weatherUpdated:weatherUpdated, changedTids:tidList]
		atomicState.updatesLog = updatesLog
		atomicState.statTime = tempStatTime
		atomicState.statUpdates = statUpdates
		statUpdates = null

		// Update the data and send it down to the devices as events
		if (debugLevelFour) LOG("pollEcobeeAPICallback() - T: ${thermostatUpdated}, R: ${runtimeUpdated} A: ${alertsUpdated}, E: ${equipUpdated}, e: ${extendRTUpdated}", 1, null, 'trace')

		if (settings.thermostats) {
			if (forcePoll || thermostatUpdated || rtReallyUpdated || extendRTUpdated || settingsUpdated || weatherUpdated || equipUpdated || audioUpdated) {
				updateThermostatData()		// update thermostats first (some sensor data is dependent upon thermostat changes)
			} else {
				LOG("No thermostat updates...", 2, null, 'info')
			}
		}
		if (settings.ecobeesensors) {						// Only update configured sensors (and then only if they have changes)
			if (forcePoll || sensorsUpdated || thermostatUpdated) { // possibly can get away with climatesUpdated, 
				updateSensorData() 
			} else {
				LOG("No sensor updates...", 2, null, 'info')
			} 
		}

		// pollChildren() will actually send all the events we just created, once we return
		// just a bit more housekeeping...
		// result = true
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

		String names = resp.json.thermostatList?.name.toString()[1..-2]			// .replaceAll("\\[","").replaceAll("\\]","")
		String tids = ""
		int numStats = 0
		tidList.each { numStats++; tids = (tids=="") ? it : tids+","+it }

		//LOG("Polling thermostat${(numStats>1)?'s':''} ${names} (${tids}) completed", 2, null, 'info')
        LOG("Polling ${tids} completed", 2, null, 'info')
		if (debugLevelFour) LOG("pollEcobeeAPICallback() - Updated thermostat${(numStats>1)?'s':''} ${names} (${tidList}) ${atomicState.thermostats}", 1, null, 'info')
		if (atomicState.inTimeoutRetry) atomicState.inTimeoutRetry = 0	// Not in Timeout Recovery any longer
	
	// Now we have to actually send the updates
	//if (result) {
		def polledMS = now()
		def prepTime = (polledMS-startMS)			// if prep takes too long, we will do the updates asynchronously too
		if (debugLevelThree) LOG("Prep complete (${prepTime}ms)",3,null,'trace')
		//if (TIMERS) log.debug "TIMER: pollEcobeeAPICallback complete @ ${polledMS - pollEcobeeAPIStart}ms"
        
        // Work-around SmartThings CPU time limit of 20 seconds...
		if (alertsUpdated) {
			if (ST && (prepTime > 11000)) { runIn(2, generateAlertsAndEvents, [overwrite: true]) } else { generateAlertsAndEvents() }
		} else {
			if (ST && (prepTime > 11000)) { runIn(2, generateTheEvents, [overwrite: true]) } else { generateTheEvents() }
		}
	} 	
	
	if (debugLevelFour) LOG("<===== Leaving pollEcobeeAPICallback() results: ${result}", 1, null, 'trace')
    atomicState.inPollChildren = false
	return result
}

def convertScheduleToTimeSchedule(oldSchedule, climates) {
	def newSchedule = [:] as LinkedHashMap 
	//def dow = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']
    int td = 0
	while (td < 7) {		// Step through the days
        newSchedule[(DOW[td])] = [:] as LinkedHashMap 
        int ti = 0
        String programId = "xYzZy-f00b@r" // Nobody will have this as a program id
    	while (ti < 48) {	// Step through the half-hours
        // ['Mon':{0:[id:"sleep",name:"Sleep"],330:[id:"smart1",name:"Awake"],510:[id:"away",name:"Away"],1080:[id:"home",name:"Home"],1410:[id:"sleep",name:"Sleep"]},'Tue'...
			def hourSched = [:] // as TreeMap
        	if (oldSchedule[td][ti] != programId) {
            	programId = oldSchedule[td][ti]
                def program = climates.find { it.climateRef == programId }
                int mins = ((ti/2)*60).toInteger()
                String hr = String.format("%02d",(mins/60).toInteger())
				String mi = String.format("%02d",(mins%60).toInteger())
                hourSched = [id:programId,name:(program.name),time:(hr+':'+mi)]
        		newSchedule[(DOW[td])][mins] = hourSched
            }
        	ti++
        }
        td++
    }
//    if (false) {
//        def rebuildSchedule = convertTimeScheduleToSchedule(newSchedule)
//        if (oldSchedule != rebuildSchedule) {
//            log.error "Schedule mismatch!!!"
//        } else {
//            log.info "Schedule Conversion Success!!!"
//        }
//    }
    return newSchedule
}

// Returns current timeSchedule Map for specified thermostat ID
def getTimeSchedule(tid) {
	def timeSchedule = atomicState.timeSchedule[tid]
	return timeSchedule
}

def getSchedule(tid) {
	def schedule = atomicState.schedule[tid]
	return schedule
}
// Converts a time-based schedule back into the Ecobee program.schedule[][] format
// Only saves the hour:00 and hour:30 time steps, but uses the closest/highest key 
// that is less than or equal to the standard half-hour timeslots
def convertTimeScheduleToSchedule(timeSchedule) {
	def newSchedule = []
	//def dow = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']
    def programId
	for (int td=0; td<7; td++) {
		newSchedule[td] = []
        TreeMap dayTimes = timeSchedule."${DOW[td]}"	// TreeMap assure that the keys (minutes) are sorted
        //log.debug "dayTimes: ${dayTimes}"
		for (int ti=0; ti<48; ti++) {
			int mins = (ti*30)
            dayTimes.each { key, climate ->
            	if (key <= mins) programId = climate.id            	
            }
            //programId = dayTimes[time]?.id ?: programId
			newSchedule[td][ti] = programId
		}
	}
    return newSchedule
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
    def statUpdates = atomicState.statUpdates
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
		
		if (statUpdates[tid]?.contains('alerts') && alerts.containsKey(tid) && (alerts[tid] != [])) {	// we have alerts to process!
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
    //def timeNow = now()
	if (debugLevelFour) LOG("Entered updateSensorData() ${atomicState.remoteSensors}", 4)
	def sensorCollector = [:] as HashMap
	def sNames = []
	def totalUpdates = 0
	Integer userPrecision = getTempDecimals()
	Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
	def updatesLog = atomicState.updatesLog
	//boolean ST = atomicState.isST
	boolean forcePoll = updatesLog.forcePoll
	def changedTids = updatesLog.changedTids
    def statUpdates = atomicState.statUpdates
    
	String preText = getDebugLevel() <= 2 ? '' : 'updateSensorData() - '
	
	if (statUpdates) changedTids.each { tid ->
    	if (statUpdates[tid] || forcePoll) {
            boolean sensorsUpdated
            boolean programUpdated
            boolean eventsUpdated
            boolean curClimRefUpdated
            boolean climatesUpdated
            boolean scheduleUpdated
			if (statUpdates[tid]) {
				sensorsUpdated		= statUpdates[tid]?.contains('sensors')
            	programUpdated		= statUpdates[tid]?.contains('program')
            	eventsUpdated		= statUpdates[tid]?.contains('events')
            	curClimRefUpdated	= statUpdates[tid]?.contains('curClimRef')
            	climatesUpdated		= statUpdates[tid]?.contains('climates')
            	scheduleUpdated		= statUpdates[tid]?.contains('schedule')
			} else {
				sensorsUpdated 		= false
            	programUpdated 		= false
            	eventsUpdated 		= false
            	curClimRefUpdated 	= false
            	climatesUpdated 	= false
            	scheduleUpdated 	= false
			}
            boolean isConnected = 		atomicState.runtime[tid]?.connected

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
                    def sensorData = [:] // as HashMap
                    def lastList = atomicState."${sensorDNI}" as List
                    int listSize = 7
                    if ( !lastList || (lastList.size() < listSize)) {
                        lastList = [999,'x',-1,'default','default','x',-1]	// initilize the list 
                        sensorData = [ thermostatId: tid ]		// this will never change, but we need to send it at least once
                    }
                    def sensorList = []
                    if (sensorsUpdated || programUpdated || forcePoll) {
                        def temperature
                        String occupancy
                        Integer humidity = null

                        it.capability.each { cap ->
                            if (cap.type == 'temperature') {
                                if (debugLevelFour) LOG("updateSensorData() - Sensor ${it.name} temp is ${cap.value}", 4)
                                if ( cap.value?.isInteger() ) { // Handles the case when the sensor is offline, which would return "unknown"
                                    temperature = roundIt((cap.value.toInteger() / 10.0), apiPrecision)
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
                                if (cap.value?.isInteger()) humidity = cap.value.toInteger()
                            }
                        }										
                        if (forcePoll) sensorData << [ thermostatId: tid /*, vents: 'default'*/ ]		// belt, meet suspenders

                        // temperature usually changes every time, goes in *List[0]
                        // Send temp with API precision	 (1 decimal F, 2 decimal C) - Sensor device will adjust for display
                        def currentTemp = ((temperature == 'unknown') ? 'unknown' : myConvertTemperatureIfNeeded(temperature, 'F', apiPrecision))
                        if (/*forcePoll || */ (lastList[0] != currentTemp)) { sensorData << [ temperature: currentTemp ] }
                        sensorList[0] = currentTemp

                        // occupancy generally only changes every 30 minutes, in *List[1]
                        if ((occupancy != "") && (lastList[1] != occupancy)) { sensorData << [ motion: occupancy ] }
                        sensorList[1] = occupancy

                        // precision can change at any time, *List[2]
                        if (lastList[2] != userPrecision) { sensorData << [ decimalPrecision: userPrecision ] }
                        sensorList[2] = userPrecision

                        // currentProgramName doesn't change all that often, but it does change
                        def currentPrograms = atomicState.currentProgramName
                        def currentProgramName = (currentPrograms && currentPrograms.containsKey(tid)) ? currentPrograms[tid] : 'default' // not set yet, we will have to get it later. 
                        if (lastList[3] != currentProgramName) { sensorData << [ currentProgramName: currentProgramName ] }
                        sensorList[3] = currentProgramName
                        currentPrograms = atomicState.currentProgram
                        def currentProgram = (currentPrograms && currentPrograms.containsKey(tid)) ? currentPrograms[tid] : 'default'
                        if (lastList[4] != currentProgram) { sensorData << [currentProgram: currentProgram] }
                        sensorList[4] = currentProgram

                        // not every sensor has humidity
                        if ((humidity != null) && (lastList[5] != humidity)) { sensorData << [ humidity: humidity ] }
                        sensorList[5] = humidity
                        
                        // get the debugLevel down to the sensor
                        int dbgLevel = getDebugLevel()
                        if (forcePoll || (lastList[6] != dbgLevel)) { sensorData << [debugLevel: dbgLevel] }
                        sensorList[6] = dbgLevel
                        
                    } else {
                        // sensorUpdated is False: no Sensor data, fix up lastList/sensorList
                        sensorList = lastList.take(listSize)
                    }

                    // collect the climates that this sensor is included in
                    def sensorClimates = [:]
                    def climatesList = []
                    def activeClimates = []
                    if (climatesUpdated) {
                        def climates = atomicState.climates[tid]
                        climates.each { climate ->
                            def sids = climate.sensors?.collect { sid -> sid.name }
                            if (sids.contains(it.name)) {
                                sensorClimates << ["${climate.name}":'on']
                                climatesList << ['on']
                                activeClimates << climate.name
                            } else {
                                sensorClimates << ["${climate.name}":'off']
                                climatesList << ['off']
                            }
                        }
                        //log.debug "climatesList: ${climatesList}"
                        sensorClimates << [activeClimates: activeClimates?.sort(false)]
                        if (debugLevelFour) LOG("updateSensorData() - sensor ${it.name} is used in ${activeClimates}", 1, null, 'trace')
                    } 

                    // check to see if the climates have changed. Notice that there can be 0 climates if the thermostat data wasn't updated this time;
                    // note also that we will send the climate data every time the thermostat IS updated, even if they didn't change, because we don't 
                    // store them in the atomicState unless the thermostat object changes...
                    if (climatesList && ((lastList.size() < (listSize+climatesList.size())) || (lastList.drop(listSize) != climatesList))) {	
                        sensorData << sensorClimates
                        sensorList << climatesList
                    }
                    // Give each sensor the complete list of valid Climates (as climatesList & programsList) from its thermostat, to support add/deleteSensorToProgram()
                    def statClimates	
                    def statPrograms
                    def changeNever = atomicState.changeNever
                    if (changeNever && changeNever.containsKey(tid) && changeNever[tid]) {
                        statPrograms = changeNever[tid][7]
                        statClimates = changeNever[tid][14]
                    } else {
                        statPrograms = '["Away", "Home", "Sleep"]'
                        statClimates = ['Away', 'Home', 'Sleep']
                    }
                    def myStatsClimates = (atomicState.myStatsClimates ?: [:])  as HashMap
                    if (myStatsClimates && myStatsClimates[sensorDNI]) {
                        if (statClimates != myStatsClimates[sensorDNI]) {
                            sensorData << [ climatesList: statClimates, programsList: statPrograms ]
                            myStatsClimates[sensorDNI] = statClimates
                            atomicState.myStatsClimates = myStatsClimates
                        }
                    } else {
                        // This initializes the climatesList/programsList data the first time through
                        sensorData << [ climatesList: statClimates, programsList: statPrograms ]
                        myStatsClimates[sensorDNI] = statClimates
                        atomicState.myStatsClimates = myStatsClimates
                    }

                    // For Health Check, update the sensor's checkInterval	any time we get forcePolled - which should happen a MINIMUM of once per hour
                    // This because sensors don't necessarily update frequently, so this simple event should be enough to let the ST Health Checker know
                    // we are still alive!
                    if (ST && forcePoll) { 
                        sensorData << [checkInterval: 3900] // 5 minutes longer than an hour
                    }

                    // Let the sensor know what's going on... (This could be the only reason we were called upon)
                    String timeNow = atomicState.statTime[tid]
                    //if (thermostatUpdated)	sensorData << [thermostatUpdated: 	timeNow]
                    if (programUpdated)		sensorData << [programUpdated: 		timeNow]
                    if (curClimRefUpdated) 	sensorData << [curClimRefUpdated: 	timeNow]	// Want them all to have "Updated" in the name
                    if (climatesUpdated) 	sensorData << [climatesUpdated: 	timeNow]
                    if (scheduleUpdated)	sensorData << [scheduleUpdated: 	timeNow]
                    if (eventsUpdated) 		sensorData << [eventsUpdated: 		timeNow]

                    if (sensorData) {
                        //int sls = sensorData.size()
                        totalUpdates = totalUpdates + sensorData.size() // sls
                        //log.debug "${it.name}: ${sensorData}"
                        sensorCollector[sensorDNI] = [name: it.name, data: sensorData]
                        atomicState."${sensorDNI}" = sensorList
                        if (debugLevelFour) LOG("updateSensorData() - sensorCollector being updated with sensorData: ${sensorData}", 1, null, 'trace')
                        sNames += it.name
                    }
                } // End sensor is valid and selected in settings
            } // End [tid] sensors loop
        } // End updates for this tid condition
	} // End thermostats loop
	
	atomicState.remoteSensorsData = sensorCollector
	if (preText=='') {
		int sz = sNames.size()
		//if (sz > 1) sNames.sort()
		if (debugLevelFour) LOG("Sensor data: ${sensorCollector}", 1, null, trace)
		//LOG("${totalUpdates} update${totalUpdates!=1?'s':''} for ${sz} sensor${sz!=1?'s':''}${sNames?' ('+sNames.toString()[1..-2]+')':''}",2, null, 'info')
        LOG("${totalUpdates} updates for ${sz} sensors",2, null, 'info')
	} else {
		if (debugLevelFour) LOG("updateSensorData() - Updated these remoteSensors: ${sensorCollector}", 1, null, 'trace') 
	}
	//if (TIMERS) log.debug "TIMER: Sensors update completed @ ${now() - atomicState.pollEcobeeAPIStart}ms"
}

void updateThermostatData() {
	//def pollEcobeeAPIStart
	//if (TIMERS) { pollEcobeeAPIStart = atomicState.pollEcobeeAPIStart; log.debug "TIMER: Entered updateThermostatData() @ ${now() - pollEcobeeAPIStart}ms"; }
	boolean debugLevelFour = debugLevel(4)
	boolean debugLevelThree = debugLevel(3)
	def updatesLog = atomicState.updatesLog
	if (debugLevelFour) LOG("updateThermostatData() - updatesLog: ${updatesLog}", 1, null, 'trace')
	boolean forcePoll = updatesLog.forcePoll
	def changedTids = updatesLog.changedTids
    def statUpdates = atomicState.statUpdates
	boolean usingMetric = wantMetric() // cache the value to save the function calls
	if (forcePoll) {
		if (atomicState.timeZone == null) getTimeZone() // both will update atomicState with valid timezone/zipcode if available
		if (atomicState.zipCode == null) getZipCode()
	}
	int apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
	int userPrecision = getTempDecimals()					// user's requested display precision
	def tstatNames = []
	def askAlexaAppAlerts 
    if (settings.askAlexa) askAlexaAppAlerts = (atomicState.askAlexaAppAlerts?: [:]) as HashMap

	int totalUpdates = 0
	def statTime = atomicState.statTime
	//if (TIMERS) log.debug "TIMER: Initialized updateThermostatData() @ ${now() - pollEcobeeAPIStart}ms"
	
	def statData = changedTids.inject([:]) { collector, tid ->
        String DNI = (HE?'ecobee_suite-thermostat-':'') + ([ app.id, tid ].join('.'))
        String tstatName = getThermostatName(tid)
        
        if (statUpdates[tid] || forcePoll) { 
        	if (debugLevelThree) LOG("Updating event data for thermostat ${tstatName} (${tid}): "+forcePoll?'forcePoll':"${statUpdates[tid].toString()[1..-2].replaceAll('rtReally','runtime')}", 1, null, 'info')
            //if (TIMERS) log.debug "TIMER: Updating event data for ${tstatName} ${tid} @ ${now() - pollEcobeeAPIStart}ms"

        // grab a local copy from the atomic storage all at once (avoid repetive reads from backing store)
            def statSettings =		(atomicState.settings ? atomicState.settings[tid] : [:]) // as HashMap
            def climates = 			(atomicState.climates ? atomicState.climates[tid] : [])
            def schedule = 			(atomicState.schedule ? atomicState.schedule[tid] : [])
            String currentClimateRef=(atomicState.currentClimateRef ? atomicState.currentClimateRef[tid] : "")
            def events = 			(atomicState.events ? atomicState.events[tid] : [])
            def runtime = 			(atomicState.runtime ? atomicState.runtime[tid] : [:]) // as HashMap
            def statLocation = 		(atomicState.statLocation ? atomicState.statLocation[tid] : [:])
            // def oemCfg = atomicState.oemCfg ? atomicState.oemCfg[tid] : [:]) as HashMap
            def extendedRuntime = (atomicState.extendedRuntime ? atomicState.extendedRuntime[tid] : [:]) // as HashMap

        // Now using per-tid list of things that changed
            boolean rtReallyUpdated = 	statUpdates[tid].contains('rtReally')
            boolean runtimeUpdated = 	rtReallyUpdated
            boolean alertsUpdated = 	statUpdates[tid].contains('alerts')
            boolean settingsUpdated = 	statUpdates[tid].contains('settings')
            boolean programUpdated = 	statUpdates[tid].contains('program')
            boolean climatesUpdated = 	statUpdates[tid].contains('climates')
            boolean scheduleUpdated = 	statUpdates[tid].contains('schedule')
            boolean curClimRefUpdated =	statUpdates[tid].contains('curClimRef')
            boolean eventsUpdated = 	statUpdates[tid].contains('events')
            boolean locationUpdated = 	statUpdates[tid].contains('location')
            boolean audioUpdated = 		statUpdates[tid].contains('audio')
            boolean extendRTUpdated = 	statUpdates[tid].contains('extendRT')
            boolean weatherUpdated = 	statUpdates[tid].contains('weather')
            boolean sensorsUpdated = 	statUpdates[tid].contains('sensors')
            boolean equipUpdated = 		statUpdates[tid].contains('equip')
            boolean statInfoUpdated = 	statUpdates[tid].contains('statInfo')

        // Handle things that only change when runtime object is updated)
            def occupancy = 'not supported'
            def tempWeatherTemperature
            def tempWeatherDewpoint
            def tempWeatherHumidity
            def tempWeatherPressure
            def tempWeatherLowForecast

            if ( runtimeUpdated || sensorsUpdated || forcePoll) {
                // Occupancy (motion)
                // TODO: Put a wrapper here based on the thermostat brand
                def thermSensor = ''
                if (atomicState.remoteSensors) { thermSensor = atomicState.remoteSensors[tid].find { it.type == 'thermostat' } }

                boolean hasInternalSensors
                if(!thermSensor) {
                    //if (debugLevelFour) LOG("updateThermostatData() - Thermostat ${tstatName} does not have a built in remote sensor", 1, null, 'warn')
                    hasInternalSensors = false
                } else {
                    hasInternalSensors = true
                    //if (debugLevelFour) LOG("updateThermostatData() - thermSensor == ${thermSensor}", 1, null, 'trace' )

                    def occupancyCap = thermSensor?.capability.find { it.type == 'occupancy' }
                    if (occupancyCap) {
                        //if (debugLevelFour) LOG("updateThermostatData() - occupancyCap = ${occupancyCap} value = ${occupancyCap.value}", 1, null, 'info')

                        // Check to see if there is even a value, not all types have a sensor
                        occupancy = occupancyCap.value ? occupancyCap.value : 'not supported'
                    }
                }
                // 'not supported' is reported as 'inactive'
                if (hasInternalSensors) { occupancy = (occupancy == 'true') ? 'active' : ((occupancy == 'unknown') ? 'unknown' : 'inactive') }			  
                //if (TIMERS) log.debug "TIMER: Finished updating Sensors for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            }

			def tempTemperature
            def tempHeatingSetpoint
            def tempCoolingSetpoint
			if (runtime) {
            	// Temperatures
                // NOTE: The thermostat always present Fahrenheit temps with 1 digit of decimal precision. We want to maintain that precision for inside temperature so that apps like vents and Smart Circulation
                //		 can operate efficiently. So, while the user can specify a display precision of 0, 1 or 2 decimal digits, we ALWAYS keep and send max decimal digits and let the device handler adjust for display
                //		 For Fahrenheit, we keep the 1 decimal digit the API provides, for Celsius we allow for 2 decimal digits as a result of the mathematical calculation	   
                tempTemperature = runtime.actualTemperature
                tempHeatingSetpoint = runtime.desiredHeat
                tempCoolingSetpoint = runtime.desiredCool
            }

            if (weatherUpdated || forcePoll) {
                def weather = (atomicState.weather ? atomicState.weather[tid] : [:]	) as HashMap
                if (weather) {
                    if (weather.temperature != null) {
                        tempWeatherTemperature = weather.temperature
                    } else {
                    	tempWeatherTemperature = 4510 // will happen only once, when weather object changes to shortWeather
                    }
                    if (weather.dewpoint != null) 		tempWeatherDewpoint 	= weather.dewpoint	// myConvertTemperatureIfNeeded((weather.dewpoint.toBigDecimal() / 10.0), "F", apiPrecision)
                    if (weather.humidity != null) 		tempWeatherHumidity 	= weather.humidity
                    if (weather.pressure != null) 		tempWeatherPressure 	= weather.pressure  // usingMetric ? weather.pressure : roundIt((weather.pressure * 0.02953),2) //milliBars to inHg
                    if (weather.tempLowForecast != null)tempWeatherLowForecast 	= weather.tempLowForecast	// Send in native 'F' so Helper doesn't have to do conversions
                }
                //if (TIMERS) log.debug "TIMER: Finished updating Weather for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            }

		// SETTINGS
		// handle[tid] things that only change when the thermostat object is updated (actually, just settingsUpdated)
            def auxHeatMode
            String currentVentMode
            def ventMinOnTime
            String statMode
            def fanMinOnTime
            String humidifierMode
            String dehumidifierMode
            def dehumidifyOvercoolOffset
            def hasHumidifier
            def hasDehumidifier
            def heatStages
            def coolStages
            def disablePreHeating
            
            if (statSettings) {
                auxHeatMode = (statSettings.hasHeatPump && (statSettings.hasForcedAir || statSettings.hasElectric || statSettings.hasBoiler))
                currentVentMode = statSettings.vent
                ventMinOnTime = statSettings.ventilatorMinOnTime
                statMode = statSettings.hvacMode
            	fanMinOnTime = statSettings.fanMinOnTime
                humidifierMode = statSettings.humidifierMode
            	dehumidifierMode = statSettings.dehumidifierMode
            	dehumidifyOvercoolOffset = statSettings.dehumidifyOvercoolOffset
            	hasHumidifier = statSettings.hasHumidifier
            	hasDehumidifier = statSettings.hasDehumidifier || (statSettings.dehumidifyWithAC && (dehumidifyOvercoolOffset != 0)) // fortunately, we can hide the details from the device handler
                heatStages = statSettings.heatStages
            	coolStages = statSettings.coolStages
                disablePreHeating = statSettings.disablePreHeating
			}
            
        // EVENTS
        // Determine if an Event is running, find the first running event
            def runningEvent = [:]
            String currentClimateName = 'null'
            String currentClimateId = 'null'
            String currentClimate = 'null'
            String currentClimateOwner = 'null'
            String currentClimateType = 'null'
            String currentFanMode = 'null'

            def climatesList = []
            def programsList = []


            // what program is supposed to be running now?
            String scheduledClimateId = 'unknown'
            String scheduledClimateName = 'Unknown'
            def schedClimateRef = null
            String scheduledClimateOwner = 'null'
            String scheduledClimateType = 'null'

            if ((curClimRefUpdated || climatesUpdated || eventsUpdated || forcePoll) && (currentClimateRef != "") && climates) {
                scheduledClimateId = currentClimateRef 
                schedClimateRef = climates.find { it.climateRef == scheduledClimateId }
                scheduledClimateName = schedClimateRef.name
                scheduledClimateOwner = schedClimateRef.owner
                scheduledClimateType = schedClimateRef.type
                climates.each { programsList += '"'+it.name+'"'; climatesList << it.name; } // read with: programsList = new JsonSlurper().parseText(theThermostat.currentValue('programsList'))
                climatesList.sort()
                programsList.sort()
            }

            if (debugLevelFour) LOG("${tstatName} -> scheduledClimateId: ${scheduledClimateId}, scheduledClimateName: ${scheduledClimateName}, scheduledClimateOwner: ${scheduledClimateOwner}, " +
                          "scheduledClimateType: ${scheduledClimateType}, climatesList: ${climatesList.toString()}", 1, null, 'info')

            if (eventsUpdated || climatesUpdated || curClimRefUpdated || forcePoll) {
                boolean vacationTemplate = false
                if (events?.size()) {
                    events.each { it ->
                        if (it.running == true) {
                            if (!runningEvent) runningEvent = it // We want the FIRST one...
                        } else if ( it.type == 'template' ) {	// templates never run...
                            vacationTemplate = true
                        }
                    }
                }
                // hack to fix ecobee bug where template is not created until first Vacation is created, which screws up resumeProgram() (last hold event is not deleted)
                if (!vacationTemplate) {
                    //LOG("No vacation template exists for thermostat ${tstatName} (${tid}), creating one...", 2, null, 'warn')
                    def r = createVacationTemplate(getChildDevice(DNI), tid.toString())		
                    //if (debugLevelThree) LOG("createVacationTemplate() returned ${r}", 3, null, 'trace')
                }

                // store the currently running event (in case we need to modify or delete it later, as in Vacation handling)
                def tempRunningEvent = [:]
                tempRunningEvent[tid] = runningEvent

                if (!atomicState.runningEvent || (!atomicState.runningEvent[tid] && runningEvent) || (atomicState.runningEvent[tid] != runningEvent)) {
                    // Update atomicState only if the data changed (avoid hundreds of unnecessary writes to atomicState when events are running)
                    tempRunningEvent = atomicState.runningEvent + tempRunningEvent
                    atomicState.runningEvent = tempRunningEvent
                }
            }
            if (!runningEvent) {
            	runningEvent = atomicState.runningEvent[tid]
                if (!runningEvent || (runningEvent.running != true)) runningEvent = [:]
            }
            
            String thermostatHold = 'null'
            String holdEndsAt = 'null'
            String holdEndDate = 'null'
            //if (TIMERS) log.debug "TIMER: Finished updating currentClimate for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            def isConnected = runtime?.connected

            if (!isConnected) {
                LOG("${tstatName} is not connected!",1,null,'warn')
                // Ecobee Cloud lost connection with the thermostat - device, wifi, power or network outage
                currentClimateName = 'Offline'
                occupancy = 'unknown'

                // Oddly, the runtime.lastModified is returned in UTC, so we have to convert it to the time zone of the thermostat
                // (Note that each thermostat could actually be in a different time zone)
                def myTimeZone = statLocation?.timeZone ? TimeZone.getTimeZone(statLocation.timeZone) : (location.timeZone?: TimeZone.getTimeZone('UTC'))
                def disconnectedMS
                String disconnectedAt
                if (runtime && runtime?.disconnectedDateTime) {
                    disconnectedMS = new Date().parse('yyyy-MM-dd HH:mm:ss',runtime?.disconnectDateTime)?.getTime()
                    disconnectedAt = new Date().parse('yyyy-MM-dd HH:mm:ss',runtime?.disconnectDateTime)?.format('yyyy-MM-dd HH:mm:ss', myTimeZone)
                } else {
                    disconnectedMS = now()
                    disconnectedAt = new Date(disconnectedMS).format('yyy-MM-dd HH:mm:ss', myTimeZone)
                }
                // In this case, holdEndsAt is actually the date & time of the last valid update from the thermostat...
                holdEndsAt = fixDateTimeString( disconnectedAt.take(10), disconnectedAt.drop(11), statTime[tid] )
                if (forcePoll && settings.askAlexa) {
                    // Let's send an Ask Alexa Alert for this as well - (Only on hourly/forcePolls, though, to minimize update overhead)
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
            //if (TIMERS) log.debug "TIMER: Finished updating Alerts for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"

            if (runningEvent && isConnected) {
                // debugLevelFour = true
                if (debugLevelFour) LOG("Found a running Event: ${runningEvent}", 1, null, 'trace')
                holdEndDate = runningEvent.endDate + ' ' + runningEvent.endTime
                holdEndsAt = fixDateTimeString( runningEvent.endDate, runningEvent.endTime, statTime[tid])
                thermostatHold = runningEvent.type
                String tempClimateRef = runningEvent.holdClimateRef ?: ''
                def holdClimateRef = ((tempClimateRef != '') ? climates.find { it.climateRef == tempClimateRef } : [:]) as HashMap
                //log.debug "runningEvent.type: ${runningEvent.type}"
                switch (runningEvent.type) {
                    case 'hold':
                        if (tempClimateRef != '') {
                            currentClimate = holdClimateRef.name
                            currentClimateName = 'Hold: ' + currentClimate
                            currentClimateOwner = holdClimateRef?.owner
                            currentClimateType = holdClimateRef?.type
                        } else if ((runningEvent.name=='auto')||(runningEvent.name=='hold')) {		// Handle the "auto" climates (includes fan on overrides)
                            currentClimateType = 'hold'
                            currentClimate = 'Hold'
                            if ((runningEvent.isTemperatureAbsolute == false) && (runningEvent.isTemperatureRelative == false)) {
                                if (runningEvent.fan == 'on') {
                                    currentClimateName = 'Hold: Fan On'
                                    thermostatHold = 'fan on'
                                } else if (runningEvent.fan == 'auto') {
                                    if (runningEvent.fanMinOnTime == 0) { 
                                        currentClimateName = 'Hold: Fan Auto'
                                        thermostatHold = 'fan auto'
                                    } else {
                                        currentClimateName = 'Hold: Circulate'
                                        thermostatHold = 'circulate'
                                    }
                                }
                            } else {
                                currentClimateName = 'Hold: Temp'
                            }
                            //currentClimate = runningEvent.name.capitalize()
                        } else {
                            currentClimateName = 'Hold'
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
                        currentClimateOwner = holdClimateRef?.owner
                        currentClimateType = holdClimateRef?.type
                        break;
                    case 'autoAway':
                        currentClimateName = 'Auto Away'
                        currentClimate = 'Away'
                        currentClimateOwner = holdClimateRef?.owner
                        currentClimateType = holdClimateRef?.type
                        break;
                    case 'autoHome':
                        currentClimateName = 'Auto Home'
                        currentClimate = 'Home'
                        currentClimateOwner = holdClimateRef?.owner
                        currentClimateType = holdClimateRef?.type
                        break;
                    case 'demandResponse':
                        // if ((runningEvent.isTemperatureAbsolute == false) && (runningEvent.isTemperatureRelative == false))
                        // Oddly, the *RelativeTemp values are always POSITIVE to reduce the load, and NEGATIVE to increase it (ie., pre-cool/pre-heat)
                        // Per: https://getsatisfaction.com/api/topics/dr-event-with-negative-relative-heat-temp-increases-the-setpoint-instead-of-decreasing-it 
                        currentClimateName = ((runningEvent.coolRelativeTemp < 0) || (runningEvent.heatRelativeTemp < 0)) ? 'Hold: Eco Prep' : 'Hold: Eco'
                        currentClimateOwner = 'demandResponse'
                        currentClimate = ((runningEvent.isOptional != null) && ((runningEvent.isOptional == true) || (runningEvent.isOptional == 'true'))) ? 'Eco' : 'Eco!'		// Tag mandatory DR events
                        currentClimateId = runningEvent.name as String
                        currentClimateType = 'program'
                        break;
                    default:		
                        LOG("Unexpected runningEvent.type: (${runningEvent.type}) - please notify Barry",1,null,'warn')
                        currentClimateName = (runningEvent.type != null) ? runningEvent.type : 'null'
                        break;
                }
                if (currentClimateId == 'null') currentClimateId = tempClimateRef ?: 'null'
            } else if (isConnected && (programUpdated || eventsUpdated || forcePoll)) {
                if (scheduledClimateId) {
                    currentClimateId 		= scheduledClimateId
                    currentClimateName 		= scheduledClimateName
                    currentClimate			= scheduledClimateName
                    currentClimateOwner 	= scheduledClimateOwner
                    currentClimateType 		= scheduledClimateType
                } else {
                    //LOG('Program or running Event missing', 1, null, 'warn')
                    // These were already initialized above
                    //currentClimateName = 'null'
                    //currentClimateId = 'null' 
                    //currentClimate = 'null'
                }
            }
            if (debugLevelFour) LOG("updateThermostatData(${tstatName} ${tid}) - currentClimate: ${currentClimate}, currentClimateName: ${currentClimateName}, currentClimateId: ${currentClimateId}, " +
                                    "currentClimateOwner: ${currentClimateOwner}, currentClimateType: ${currentClimateType}, thermostatHold: ${thermostatHold}", 1, null, 'trace')

            // Note that fanMode == 'auto' might be changed by the Thermostat DTH to 'off' or 'circulate' dependent on  HVACmode and fanMinRunTime
            if (runningEvent) {
                currentFanMode = runningEvent.fan
                currentVentMode = runningEvent.vent
                ventMinOnTime = runningEvent.ventilatorMinOnTime
            } else {
                currentFanMode = runtime.desiredFanMode
            }
            //if (TIMERS) log.debug "TIMER: Finished updating Hold & climates for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"

        // HUMIDITY
            def humiditySetpoint = runtime.desiredHumidity
            def dehumiditySetpoint = runtime.desiredDehumidity
            if (hasHumidifier && (humidifierMode == 'auto') && (extendedRuntime && extendedRuntime.desiredHumidity)) { 
                humiditySetpoint = extendedRuntime.desiredHumidity		
            }
            if (hasDehumidifier && (extendedRuntime?.desiredDehumidity != 0)) { 
                dehumiditySetpoint = extendedRuntime.desiredDehumidity
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
            String equipOpStat // = 'idle'	// assume we're idle - this gets fixed below if different
            String thermOpStat // = 'idle'
            boolean isHeating 		= false
            boolean isCooling 		= false
            boolean smartRecovery 	= false
            boolean overCool 		= false
            boolean dehumidifying 	= false
            // boolean ecoPreCool		= false

            // Let's deduce if we are in Smart Recovery mode
            if (equipStatus.contains('ea')) {
                isHeating = true
                if ((tempTemperature != null) && (tempHeatingSetpoint != null) && (disablePreHeating == false) && (tempTemperature > tempHeatingSetpoint)) {
                    smartRecovery = true
                    equipUpdated = true
                    equipStatus = equipStatus + ',smartRecovery'
                    //if (debugLevelThree) LOG("${tstatName} is in Smart Recovery (${tid}), temp: ${tempTemperature}, setpoint: ${tempHeatingSetpoint}",3,null,'info')
                }
            } else if (equipStatus.contains('oo')) {
                isCooling = true
                // Check if humidity > humidity setPoint, and tempTemperature > (coolingSetpoint - 0.1)
                if (hasDehumidifier) {
                    if (runtime?.actualHumidity > dehumiditySetpoint) {
                        if ((tempTemperature != null) && (tempCoolingSetpoint != null) && (tempTemperature < tempCoolingSetpoint) && 
                        		(tempTemperature >= (tempCoolingSetpoint - dehumidifyOvercoolOffset))) {
                            overCool = true
                            equipUpdated = true
                            //if (debugLevelThree) LOG("${tstatName} is Over Cooling (${tid}), temp: ${tempTemperature}, setpoint: ${tempCoolingSetpoint}",3,null,'info')
                        } else {
                            dehumidifying = true
                        }
                    }			
                }
                if (!overCool && (tempTemperature != null) && (tempCoolingSetpoint != null) && ((statSettings?.disablePreCooling == false) && (tempTemperature < tempCoolingSetpoint))) {
                    smartRecovery = true
                    equipUpdated = true
                    equipStatus = equipStatus + ',smartRecovery'
                    //if (debugLevelThree) LOG("${tstatName} is in Smart Recovery (${tid}), temp: ${tempTemperature}, setpoint: ${tempCoolingSetpoint}",3,null,'info')
                }
            }

            // These are only set when running Smart Recovery
            String nextHeatingSetpoint = 'null'
            String nextCoolingSetpoint = 'null'
            String nextProgramName = 'null'

            if (smartRecovery) {
                // When in Smart Recovery, set the nextHeating/CoolingSetpoints so that Smart Vents knows where we are trying to get to
                Calendar calendar = Calendar.getInstance()
                calendar.setTime(new Date().parse('yyyy-MM-dd HH:mm:ss',statTime[tid]))
                int td = (calendar.get(Calendar.DAY_OF_WEEK) > 1) ? (calendar.get(Calendar.DAY_OF_WEEK) - 2) : 6	// Change from (Sunday = 0) to (Monday == 0, Sunday == 6)
                int ti = (calendar.get(Calendar.HOUR_OF_DAY) * 2) + ((calendar.get(Calendar.MINUTE) < 30) ? 0 : 1)	// Index into Schedule[day] array of program Ids
                def schedClimateId = schedule[td][ti] // currently scheduled program
                //log.debug "[${tid}] schedClimate = ${schedClimateId}, td: ${td}, ti: ${ti}"
                ti++
                def nextClimateId = ''
                while (!nextClimateId) {
                    if (ti == 48) { ti = 0; td = (td < 6) ? td + 1 : 0; }
                    if (schedClimateId != schedule[td][ti]) {
                        nextClimateId = schedule[td][ti]
                    } else ti++ 
                }
                def nextClimate = climates?.find { it.climateRef == nextClimateId }
                if (nextClimate) {
                    nextCoolingSetpoint = nextClimate.coolTemp.toString()	// myConvertTemperatureIfNeeded( nextClimate.coolTemp / 10.0, 'F', apiPrecision ) 
                    nextHeatingSetpoint = nextClimate.heatTemp.toString()	// myConvertTemperatureIfNeeded( nextClimate.heatTemp / 10.0, 'F', apiPrecision )
                    nextProgramName = nextClimate.name
                }
            }
            //log.debug "[${tid}] nextClimate: ${nextClimate}, nextHeat: ${nextHeatingSetpoint}, nextCool: ${nextCoolingSetpoint}"

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
                    equipOpStat = whichStage(equipStatus, auxHeatMode, heatStages)	// reducing the size of this method
                    if (equipOpStat == 'compHotWater') {
                    	thermOpStat = 'heating (hot water)'
                    } else if (equipOpStat == 'auxHotWater') {
                    	thermOpStat = 'heating (hot water)'
                    }
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
                    // SHOULD thermOpStat BE FAN ONLY? --> yes, this is fixed in ES Thermostat
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
                    holdEndDate = 'null'
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
            //if (TIMERS) log.debug "TIMER: Finished updating Equip & Cloud for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"

			// ****************************************************************************************************************************
            // BUILD THE EVENT LIST FOR THE THERMOSTAT
            // ****************************************************************************************************************************
            def data = [] // Needs to be an **ordered** list - this is quicker than making a BIG map
            def dataStart = [] // Ditto
            if (forcePoll) { data << [forced: true]; dataStart << [forced: true]; }				// Tell the DTH to force-update all attributes, states and tiles
            // As of 1.7.10, forcePoll only forces updates to: heating/coolingSetpoint, currentClimate*, temperature, and humidity.
            // Everything else is scanned and only sent if it changed. This was a catch-all for when Updated flags weren't being
            // checked properly, but as of 1.7.10 I'm pretty sure we got everything so there really shouldn't be a need to EVER to a force-poll
            // NOTE: to force a total update, simply set all the atomicState.xxxChanged Maps to [:], as is done in initialize()
            
            // As of 1.8.15, a call to pollChildren(deviceId, force=true) will clear all the change logs, which will in turn force resending EVERYTHING down to the devices

            // Equipment operating status - need to send first so that temperatureDisplay is properly updated after API connection loss/recovery
            // And we send the nextSetpoints before the status, so that SmartRecovery has them initialized (for targeting in Smart Vents)
            def changeEquip = atomicState.changeEquip
            if (equipUpdated || smartRecovery || curClimRefUpdated || eventsUpdated || forcePoll || climatesUpdated || (atomicState.wasConnected != isConnected) || !changeEquip?.containsKey(tid) || !changeEquip[tid]) { 
                // [idle, idle, idle, null, null, null, null]
                // 0: equipStatus, 1:thermOpState, 2:equipOpState, 3: , 4: nextHeatSP, 5: nextCoolSP, 6: nextProgName
                atomicState.wasConnected = isConnected
                boolean eqpChanged = false
                if (!changeEquip?.containsKey(tid) || !changeEquip[tid]) {
                	changeEquip[tid] = ['x', 'x', 'x', 'x', 'x', 'x', 'x']
                }
                // These next two are either 'null' or the raw setpoint (e.g., 740 - 74.0)
                if (changeEquip[tid][4] != nextHeatingSetpoint) { 
                    data << [nextHeatingSetpoint: nextHeatingSetpoint.isInteger() ? myConvertTemperatureIfNeeded( nextHeatingSetpoint.toInteger() / 10.0, 'F', apiPrecision ) : nextHeatingSetpoint]
                    changeEquip[tid][4] = nextHeatingSetpoint
                    eqpChanged = true
                }
                if (changeEquip[tid][5] != nextCoolingSetpoint) { 
                    data << [nextCoolingSetpoint: nextCoolingSetpoint.isInteger() ? myConvertTemperatureIfNeeded( nextCoolingSetpoint.toInteger() / 10.0, 'F', apiPrecision ) : nextCoolingSetpoint]
                    changeEquip[tid][5] = nextCoolingSetpoint
                    eqpChanged = true
                }
                if (changeEquip[tid][6] != nextProgramName)				 { data << [nextProgramName: nextProgramName];			changeEquip[tid][6] = nextProgramName;		eqpChanged = true; }
                if (equipStatus && (changeEquip[tid][0] != equipStatus)) { data << [equipmentStatus: equipStatus];				changeEquip[tid][0] = equipStatus; 			eqpChanged = true; }
                if (thermOpStat && (changeEquip[tid][1] != thermOpStat)) { data << [thermostatOperatingState: thermOpStat];		changeEquip[tid][1] = thermOpStat; 			eqpChanged = true; }
                if (equipOpStat && (changeEquip[tid][2] != equipOpStat)) { data << [equipmentOperatingState: equipOpStat];		changeEquip[tid][2] = equipOpStat; 			eqpChanged = true; }
                if (eqpChanged) {
                    atomicState.changeEquip = changeEquip
                }
            }		
			
            // API link to Ecobee's Cloud status - doesn't change unless things get broken
            boolean cldChanged = false
            def checkInterval = 3600
            if (ST) {
                Integer pollingInterval = getPollingInterval() // if this changes, we recalculate checkInterval for Health Check
                if (pollingInterval != atomicState.lastPI) {
                    atomicState.lastPI = pollingInterval
                    checkInterval = (pollingInterval <= 5) ? (16*60) : (((pollingInterval+1)*2)*60)
                    if (checkInterval > 3600) checkInterval = 3900	// 5 minutes longer than an hour
                }
            }
            def changeCloud = atomicState.changeCloud
            if (!changeCloud?.containsKey(tid) || !changeCloud[tid]) { 
            	changeCloud[tid] = [lastPoll, apiConnection,isConnected,checkInterval]
                cldChanged = true
            }
            if (cldChanged) {
            	data << [lastPoll: lastPoll]
            	data << [apiConnected: apiConnection]
                data << [ecobeeConnected: isConnected]
          		if (ST) data << [checkInterval: checkInterval]
            } else {
                if (changeCloud[tid][0] != lastPoll)				{ data << [lastPoll: lastPoll];				changeCloud[tid][0] = lastPoll; 		cldChanged = true; }
                if (changeCloud[tid][1] != apiConnection)			{ data << [apiConnected: apiConnection];	changeCloud[tid][1] = apiConnection; 	cldChanged = true; }
                if (changeCloud[tid][2] != isConnected)				{ data << [ecobeeConnected: isConnected];	changeCloud[tid][2] = isConnected; 		cldChanged = true; }
                if (ST && (changeCloud[tid][3] != checkInterval))	{ data << [checkInterval: checkInterval];	changeCloud[tid][3] = checkInterval; 	cldChanged = true; }
            }
            if (cldChanged) {
                atomicState.changeCloud = changeCloud
            }
            ////if (TIMERS) log.debug "TIMER: Finished queueing Equip & Cloud for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"

            // Alerts & Read Only
            if (alertsUpdated || settingsUpdated || forcePoll) {
                def changeAlerts = atomicState.changeAlerts
                if (!changeAlerts?.containsKey(tid)) changeAlerts[tid] = []
                def alertValues = []
                boolean alrtChanged = false
                alertNamesList.eachWithIndex { alert, i ->
                    def alertVal = statSettings."${alert}"
                    if (alertVal == '') alertVal = 'null'
                    alertValues[i] = alertVal 
                    if (changeAlerts[tid]?.getAt(i) != alertVal) {
                        data << [ "${alert}": alertVal ]
                        alrtChanged = true
                        if (HE && (alert == 'wifiOfflineAlert')) {
                            atomicState.wifiAlert = ((alertVal == true) || (alertValue == 'true'))
                            updateMyLabel()
                        }
                    }
                }
                if (alrtChanged) {
                    changeAlerts[tid] = alertValues
                    atomicState.changeAlerts = changeAlerts
                }
            }
            ////if (TIMERS) log.debug "TIMER: Finished queueing Alerts & Read Only for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"

            // Configuration Settings
            if (settingsUpdated || forcePoll) {
                def changeAttrs = atomicState.changeAttrs
				if (!changeAttrs.containsKey(tid)) changeAttrs[tid] = []
                def attrValues = []
                boolean attrsChanged = false
                settingsNamesList.eachWithIndex { attr, i ->
                    def attrVal = statSettings."${attr}"
                    if (attrVal == '') attrVal = 'null'
                    attrValues[i] = attrVal 
                    if (changeAttrs[tid]?.getAt(i) != attrVal) {
                        data << [ "${attr}": attrVal ]
                        attrsChanged = true
                    }
                }
                //log.debug "attrValues changed: ${attrsChanged}, [${tid}]: ${attrValues}"
                if (attrsChanged) {
                    changeAttrs[tid] = attrValues
                    atomicState.changeAttrs = changeAttrs
                }
                //if (TIMERS) log.debug "TIMER: Finished queueing Attrs for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            }

            // Audio Settings
            if (audioUpdated || forcePoll) {
                def audio = atomicState.audio 
                audio =  (audio && (audio.containsKey(tid))) ? audio[tid] : [:]
                def changeAudio = atomicState.changeAudio // ?: [:]) as HashMap
                if (!changeAudio?.containsKey(tid)) changeAudio[tid] = []
                def audioValues = []
                boolean audioChanged = false
                audioNamesList.eachWithIndex { aud, i ->
                    def audioVal = audio?."${aud}"
                    if (audioVal == '') audioVal = 'null'
                    audioValues[i] = audioVal 
                    if (changeAudio[tid]?.getAt(i) != audioVal) {
                        data << [ "${aud}": audioVal ]
                        audioChanged = true
                    }
                }
                if (audioChanged) {
                    changeAudio[tid] = audioValues
                    atomicState.changeAudio = changeAudio
                }
                //if (TIMERS) log.debug "TIMER: Finished queueing Audio for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            }

            // Thermostat Device Data
            if (statInfoUpdated || forcePoll) {
                def statInfo = atomicState.statInfo
                def changeDevice = atomicState.changeDevice
                if (!changeDevice?.containsKey(tid)) changeDevice[tid] = [:]
                def deviceValues = [:]
                boolean devsChanged = false
                statInfo[tid].eachWithIndex { k, v, i ->
                    def deviceVal = v	
                    if (deviceVal == '') deviceVal = 'null'
                    deviceValues << ["${k}": deviceVal]
                    if (changeDevice[tid]?."${k}" != deviceVal) {
                        data << [ "${k}": deviceVal ]
                        if (!devsChanged) devsChanged = true
                    }
                }
                if (devsChanged) {
                    changeDevice[tid] = deviceValues
                    atomicState.changeDevice = changeDevice
                }
                //if (TIMERS) log.debug "TIMER: Finished queueing Device data for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            }

            // Temperatures - need to convert from internal F*10 to standard Thermostat units
            if (settingsUpdated || forcePoll) {
                def changeTemps =  atomicState.changeTemps
                if (!changeTemps?.containsKey(tid)) changeTemps[tid] = []
                boolean tempsChanged = false
                def tempValues = []
                tempSettingsList.eachWithIndex { temp, i ->
                    // def tempVal = String.format("%.${apiPrecision}f", roundIt((usingMetric ? roundIt((statSettings."${temp}" / 1.8), 0) / 10.0 : statSettings."${temp}" / 10.0),apiPrecision))
                    // def tempVal = roundIt((usingMetric ? roundIt((statSettings."${temp}" / 1.8), 0) / 10.0 : statSettings."${temp}" / 10.0),apiPrecision)
                    def theTemp = statSettings."${temp}"
                    def tempVal = theTemp ? myConvertTemperatureIfNeeded(theTemp.toInteger() / 10.0, 'F', apiPrecision) : 'null'
                    tempValues[i] = tempVal 
                    if (changeTemps[tid]?.getAt(i) != tempVal) {
                        data << [ "${temp}": tempVal ]
                        tempsChanged = true
                    }
                }
                if (tempsChanged) {
                    changeTemps[tid] = tempValues
                    atomicState.changeTemps = changeTemps
                }
                //if (TIMERS) log.debug "TIMER: Finished queueing Temp Settings for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            }

            // SmartApp configuration settings that almost never change (Listed in order of frequency that they should change normally)
            int dbgLevel = getDebugLevel()
            String tmpScale = temperatureScale
            boolean cfgsChanged = false
            String timeOfDay = atomicState.timeZone ? getTimeOfDay() : getTimeOfDay(tid)
            boolean userPChanged = false
            def changeConfig = atomicState.changeConfig
            if (!changeConfig?.containsKey(tid) || !changeConfig[tid]) { 
            	changeConfig[tid] = [timeOfDay,userPrecision,dbgLevel,tmpScale,settings.mobile]
                cfgsChanged = true 
            }
            if (cfgsChanged) {
            	data << [timeOfDay: timeOfDay]
                data << [decimalPrecision: userPrecision]
                userPChanged = true
                data << [debugLevel: dbgLevel]
                data << [temperatureScale: tmpScale]
                data << [mobile: settings.mobile]
            } else {
                if (changeConfig[tid][0] != timeOfDay)		{ data << [timeOfDay: timeOfDay];				changeConfig[tid][0] = timeOfDay; 			cfgsChanged = true; }
                if (changeConfig[tid][1] != userPrecision)	{ data << [decimalPrecision: userPrecision];	changeConfig[tid][1] = userPrecision; 		cfgsChanged = true; userPChanged = true; }
                if (changeConfig[tid][2] != dbgLevel)		{ data << [debugLevel: dbgLevel];				changeConfig[tid][2] = dbgLevel; 			cfgsChanged = true; }
                if (changeConfig[tid][3] != tmpScale)		{ data << [temperatureScale: tmpScale];			changeConfig[tid][3] = tmpScale; 			cfgsChanged = true; }
                if (changeConfig[tid][4] != settings.mobile){ data << [mobile: settings.mobile];			changeConfig[tid][4] = settings.mobile; 	cfgsChanged = true; }
            }
            if (cfgsChanged) {
                atomicState.changeConfig = changeConfig
            }
            //if (TIMERS) log.debug "TIMER: Finished queueing Config data for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"

            // Thermostat configuration settings
            boolean nvrChanged = false
            // Thermostat configuration stuff that almost never changes - if any one changes, send them all
            def changeNever = atomicState.changeNever
            if (settingsUpdated || climatesUpdated || forcePoll || !changeNever?.containsKey(tid) || !changeNever[tid]) {				// || (changeNever[tid] != neverList)) {
            	boolean needAll = false
                if (!changeNever?.containsKey(tid) || !changeNever[tid]) {
                	changeNever[tid] = ['x','x','x','x','x','x','x','x','x','x','x','x','x','x','x']
                    needAll = true
                }
                if (statSettings && (settingsUpdated || forcePoll || needAll)) {
                    def autoMode = statSettings.autoHeatCoolFeatureEnabled
                    def statHoldAction = statSettings.holdAction			// thermsotat's preference setting for holdAction:
                                                                            // useEndTime4hour, useEndTime2hour, nextPeriod, indefinite, askMe
                    if (changeNever[tid][1] != autoMode)					{ data << [autoMode: autoMode];					changeNever[tid][1] = autoMode; 				nvrChanged = true; }
                    if (changeNever[tid][2] != statHoldAction)				{ data << [statHoldAction: statHoldAction];		changeNever[tid][2] = statHoldAction; 			nvrChanged = true; }
                    if (changeNever[tid][3] != coolStages)					{ data << [coolStages: coolStages, 
                                                                            		   coolMode: (coolStages > 0)];			changeNever[tid][3] = coolStages; 				nvrChanged = true; }
                    if (changeNever[tid][4] != heatStages)					{ data << [heatStages: heatStages,
                                                                            		   heatMode: (heatStages > 0)];			changeNever[tid][4] = heatStages; 				nvrChanged = true; }
                    String heatHigh = statSettings.heatRangeHigh.toString()
                	String heatLow = statSettings.heatRangeLow.toString()
                    if (heatHigh && heatLow) {
                    	String hc = heatHigh + '-' + heatLow
                    	if (changeNever[tid][5] != hc) {
                        	changeNever[tid][5] = hc
                    		def hh = myConvertTemperatureIfNeeded((heatHigh.toInteger()	/ 10.0), 'F', apiPrecision)
                        	def hl = myConvertTemperatureIfNeeded((heatLow.toInteger()  / 10.0), 'F', apiPrecision)
                			String hr = "(${roundIt(hl,0)}..${roundIt(hh,0)})"
                            data << [heatRange: hr, heatRangeHigh: hh, heatRangeLow: hl]
                        }
                    } else {
                    	def hh, hl
                        String hr
                    	if (usingMetric) {
                        	if (changeNever[tid][5] != '5-35') {changeNever[tid][5] = '5-35'; hh = 35; hl = 5; hr = '(5..35)';}
                        } else {
                        	if (changeNever[tid][5] != '45-95') {changeNever[tid][5] = '45-95'; hh = 95; hl = 45; hr = '(45..95)';}
                        }
                        data << [heatRange: hr, heatRangeHigh: hh, heatRangeLow: hl]
                        nvrChanged = true
                    }
                    String coolHigh = statSettings.coolRangeHigh.toString()
                	String coolLow = statSettings.coolRangeLow.toString()
                    if (coolHigh && coolLow) {
                    	String cc = coolHigh + '-' + coolLow
                    	if (changeNever[tid][5] != cc) {
                        	changeNever[tid][5] = cc
                    		def ch = myConvertTemperatureIfNeeded((coolHigh.toInteger()	/ 10.0), 'F', apiPrecision)
                        	def cl = myConvertTemperatureIfNeeded((coolLow.toInteger()  / 10.0), 'F', apiPrecision)
                			String cr = "(${roundIt(cl,0)}..${roundIt(ch,0)})"
                            data << [coolRange: cr, coolRangeHigh: ch, coolRangeLow: cl]
                        }
                    } else {
                    	def ch, cl
                        String cr
                    	if (usingMetric) {
                        	if (changeNever[tid][5] != '5-35') {changeNever[tid][5] = '5-35'; ch = 35; cl = 5; cr = '(5..35)';}
                        } else {
                        	if (changeNever[tid][5] != '45-95') {changeNever[tid][5] = '45-95'; ch = 95; cl = 45; cr = '(45..95)';}
                        }
                        data << [coolRange: cr, coolRangeHigh: ch, coolRangeLow: cl]
                        nvrChanged = true
                    }
                    String tempHeatDiff = statSettings.stage1HeatingDifferentialTemp.toString()
                	String tempCoolDiff = statSettings.stage1CoolingDifferentialTemp.toString()
                	String tempHeatCoolMinDelta = statSettings.heatCoolMinDelta.toString()
                    if (tempHeatDiff && (changeNever[tid][11] != tempHeatDiff))	{ 
                    	data << [heatDifferential: myConvertTemperatureIfNeeded(tempHeatDiff.toInteger() / 10.0, "F", apiPrecision)]
                        changeNever[tid][11] = tempHeatDiff
                        nvrChanged = true
                    }
                    if (tempCoolDiff && (changeNever[tid][12] != tempCoolDiff)) { 
                    	data << [coolDifferential: myConvertTemperatureIfNeeded(tempCoolDiff.toInteger() / 10.0, "F", apiPrecision)]
                        changeNever[tid][12] = tempCoolDiff
                        nvrChanged = true
                    }
                    if (tempHeatCoolMinDelta && (changeNever[tid][13] != tempHeatCoolMinDelta)) { 
                    	data << [heatCoolMinDelta: myConvertTemperatureIfNeeded(tempHeatCoolMinDelta.toInteger() / 10.0 , 'F', apiPrecision)]
                        changeNever[tid][13] = tempHeatCoolMinDelta
                        nvrChanged = true
                    }
                    if (changeNever[tid][8] != auxHeatMode)					{ data << [auxHeatMode: auxHeatMode];			changeNever[tid][8] = auxHeatMode; 				nvrChanged = true; }
                    if (changeNever[tid][9] != hasHumidifier)				{ data << [hasHumidifier: hasHumidifier];		changeNever[tid][9] = hasHumidifier; 			nvrChanged = true; }
                    if (changeNever[tid][10] != hasDehumidifier)			{ data << [hasDehumidifier: hasDehumidifier];	changeNever[tid][10] = hasDehumidifier; 		nvrChanged = true;}
                }
                if (forcePoll || climatesUpdated || needAll) {
                    // REMINDER: updateSensorData is using atomicState.changeNever[tid][7] to send the programsList to the Sensor devices
                    // programsList is '["Away","Home","Sleep"]'
                    if (programsList && (changeNever[tid][7] != programsList))	{ data << [programsList: programsList]; changeNever[tid][7] = programsList; 	nvrChanged = true; }
                    // climatesList is ['Away','Home','Sleep']
                    if (climatesList && (changeNever[tid][14] != climatesList))	{ data << [climatesList: climatesList]; changeNever[tid][14] = climatesList; 	nvrChanged = true; }
                }
                if (nvrChanged) {
                    atomicState.changeNever = changeNever
                }
                //if (TIMERS) log.debug "TIMER: Finished queueing changeNever for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
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
            def changeRarely = atomicState.changeRarely	// ?: [:]) as HashMap
            //if (thermostatUpdated || runtimeUpdated ||	equipUpdated || forcePoll || /*settingsUpdated || eventsUpdated || programUpdated || runtimeUpdated || extendRTUpdated ||*/ (changeRarely == [:]) || !changeRarely.containsKey(tid)) { // || (changeRarely[tid] != rarelyList)) {	
            if (programUpdated || runtimeUpdated || eventsUpdated || equipUpdated || settingsUpdated || forcePoll || !changeRarely?.containsKey(tid) || !changeRarely[tid]) { // || (changeRarely[tid] != rarelyList)) {	
            	boolean needAll = false
                if (!changeRarely?.containsKey(tid) || !changeRarely[tid]) {
                	changeRarely[tid] = ['x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x']
                    needAll = true
                }
                // The order of these is IMPORTANT - Do setpoints and all equipment changes before notifying of the hold and program change...
                if (tempHeatingSetpoint && (forcePoll || (changeRarely[tid][19] != tempHeatingSetpoint) || userPChanged))	{ 
                    needPrograms = true
                    changeRarely[tid][19] = tempHeatingSetpoint
                    data << [heatingSetpoint: myConvertTemperatureIfNeeded((tempHeatingSetpoint.toInteger() / 10.0), "F", userPrecision)] // roundIt(tempHeatingSetpoint, userPrecision)] // String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision))]
                    rareChanged = true
                }
                if (tempCoolingSetpoint && (forcePoll || (changeRarely[tid][20] != tempCoolingSetpoint) || userPChanged))	{ 
                    needPrograms = true
                    changeRarely[tid][20] = tempCoolingSetpoint
                    data << [coolingSetpoint: myConvertTemperatureIfNeeded((tempCoolingSetpoint.toInteger() / 10.0), "F", userPrecision)] // roundIt(tempCoolingSetpoint, userPrecision)] //String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision))] 
                    rareChanged = true
                }
                if (changeRarely[tid][4]  != statMode)					{ data << [thermostatMode: statMode];						changeRarely[tid][4]  = statMode;				if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][12] != currentFanMode)			{ data << [thermostatFanMode: currentFanMode];				changeRarely[tid][12] = currentFanMode;			if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][13] != currentVentMode)			{ data << [vent: currentVentMode];							changeRarely[tid][13] = currentVentMode;		if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][14] != ventMinOnTime)				{ data << [ventilatorMinOnTime: ventMinOnTime];				changeRarely[tid][14] = ventMinOnTime;			if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][0]  != fanMinOnTime)				{ data << [fanMinOnTime: fanMinOnTime];						changeRarely[tid][0]  = fanMinOnTime;			if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][5]  != humidifierMode)			{ data << [humidifierMode: humidifierMode];					changeRarely[tid][5]  = humidifierMode;			if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][6]  != dehumidifierMode)			{ data << [dehumidifierMode: dehumidifierMode];				changeRarely[tid][6]  = dehumidifierMode;		if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][1]  != thermostatHold)			{ data << [thermostatHold: thermostatHold];					changeRarely[tid][1]  = thermostatHold;			if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][2]  != holdEndsAt)				{ data << [holdEndsAt: holdEndsAt];							changeRarely[tid][2]  = holdEndsAt;				if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][3]  != statusMsg)					{ data << [holdStatus: statusMsg];							changeRarely[tid][3]  = statusMsg;				if (!rareChanged) rareChanged = true; }
                if (changeRarely[tid][21] != holdEndDate)				{ data << [holdEndDate: holdEndDate];						changeRarely[tid][21] = holdEndDate;			if (!rareChanged) rarechanged = true; }

                //log.debug "currentClimateName: ${currentClimateName}"
            	if (curClimRefUpdated || climatesUpdated || eventsUpdated || forcePoll || needAll) {
                    if (changeRarely[tid][7]  != currentClimate)		{ data << [currentProgram: currentClimate];					changeRarely[tid][7]  = currentClimate;			if (!rareChanged) rareChanged = true; }
                    if (changeRarely[tid][8]  != currentClimateName)	{ data << [currentProgramName: currentClimateName];			changeRarely[tid][8]  = currentClimateName;		if (!rareChanged) rareChanged = true; }
                    if (changeRarely[tid][9]  != currentClimateId)		{ data << [currentProgramId: currentClimateId];				changeRarely[tid][9]  = currentClimateId;		if (!rareChanged) rareChanged = true; }
                    if (changeRarely[tid][15] != currentClimateOwner)	{ data << [currentProgramOwner: currentClimateOwner];		changeRarely[tid][15] = currentClimateOwner;	if (!rareChanged) rareChanged = true; }
                    if (changeRarely[tid][16] != currentClimateType)	{ data << [currentProgramType: currentClimateType];			changeRarely[tid][16] = currentClimateType;		if (!rareChanged) rareChanged = true; }
                }
                
                if (curClimRefUpdated || climatesUpdated || forcePoll || needAll) {
                    if (changeRarely[tid][10] != scheduledClimateName)	{ data << [scheduledProgramName: scheduledClimateName,
                                                                                   scheduledProgram: scheduledClimateName];			changeRarely[tid][10] = scheduledClimateName;	if (!rareChanged) rareChanged = true; }
                    if (changeRarely[tid][11] != scheduledClimateId)	{ data << [scheduledProgramId: scheduledClimateId];			changeRarely[tid][11] = scheduledClimateId;		if (!rareChanged) rareChanged = true; }
                    if (changeRarely[tid][17] != scheduledClimateOwner) { data << [scheduledProgramOwner: scheduledClimateOwner];	changeRarely[tid][17] = scheduledClimateOwner;	if (!rareChanged) rareChanged = true; }
                    if (changeRarely[tid][18] != scheduledClimateType)	{ data << [scheduledProgramType: scheduledClimateType];		changeRarely[tid][18] = scheduledClimateType;	if (!rareChanged) rareChanged = true; }
                }
				//log.debug "changeRarely[${tid}] after: ${changeRarely[tid]}"
                if (rareChanged) {
                    atomicState.changeRarely = changeRarely
                }

                // If the setpoints change, then double-check to see if the currently running Program has changed.
                // We do this by ensuring that the rest of the thermostat datasets (settings, program, events) are 
                // collected on the next poll, if they weren't collected in this poll.
                atomicState.needPrograms = (needPrograms && (!thermostatUpdated && !forcePoll))

                if (programUpdated || eventsUpdated || forcePoll || needAll) {
                    // Save the Program when it changes so that we can get to it easily for the child sensors
                    def tempProgramName = atomicState.currentProgramName as Map
                    tempProgramName[tid] = currentClimateName
                    atomicState.currentProgramName = tempProgramName
                    def tempProgram = atomicState.currentProgram as Map
                    tempProgram[tid] = currentClimate
                    atomicState.currentProgram = tempProgram
                    /*
                    def tempProgram = [:]
                    tempProgram[tid] = currentClimateName
                    if (atomicState.currentProgramName) tempProgram = atomicState.currentProgramName + tempProgram
                    atomicState.currentProgramName = tempProgram
                    tempProgram = [:]
                    tempProgram[tid] = currentClimate
                    if (atomicState.currentProgram) tempProgram = atomicState.currentProgram + tempProgram
                    atomicState.currentProgram = tempProgram
                    */
                }
                //if (TIMERS) log.debug "TIMER: Finished queueing changeRarely for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            }	  

            // Runtime stuff that changes most frequently - we test them 1 at a time, and send only the ones that change
            def changeOften = atomicState.changeOften
            if (runtimeUpdated || forcePoll || weatherUpdated || extendRTUpdated || sensorsUpdated || !changeOften?.containsKey(tid) || !changeOften[tid]) {
            	if (!changeOften?.containsKey(tid) || !changeOften[tid]) {
                	changeOften[tid] = [999,'x',-1,-1,-1,-999,-999,-1,-1,-1,-1,-1,-1,-1,'x']
                }
				if (sensorsUpdated && 						( changeOften[tid][1]  != occupancy)) 										data << [motion: occupancy]
                if ((tempTemperature != null) && 			((changeOften[tid][0]  != tempTemperature) || forcePoll))					data << [temperature: myConvertTemperatureIfNeeded(
                																																						(tempTemperature.toInteger() / 10.0), 'F', userPrecision)] 
                def actualHumidity = runtime?.actualHumidity
                if ((actualHumidity != null) && 			((changeOften[tid][2]  != actualHumidity) || forcePoll))					data << [humidity: runtime.actualHumidity]
                if (humiditySetpointDisplay && 				( changeOften[tid][12] != humiditySetpointDisplay)) 						data << [humiditySetpointDisplay: humiditySetpointDisplay]
                if ((humiditySetpoint != null) &&			( changeOften[tid][10] != humiditySetpoint)) 								data << [humiditySetpoint: humiditySetpoint]
                if ((dehumiditySetpoint != null) && 		( changeOften[tid][11] != dehumiditySetpoint)) 								data << [dehumiditySetpoint: dehumiditySetpoint]
                String wSymbol = atomicState.weather[tid]?.weatherSymbol?.toString()
                if (weatherUpdated || forcePoll) {
                    if (wSymbol && 							((changeOften[tid][5]  != wSymbol) || (timeOfDay != changeOften[tid][4]))) 	data << [timeOfDay: timeOfDay, weatherSymbol: wSymbol]
                    if ((tempWeatherTemperature != null) && ((changeOften[tid][6]  != tempWeatherTemperature) || userPChanged)) 		data << [weatherTemperature: myConvertTemperatureIfNeeded(
                    																																					(tempWeatherTemperature.toInteger() / 10.0), "F", userPrecision)]
                    if (tempWeatherHumidity && 				( changeOften[tid][7]  != tempWeatherHumidity)) 							data << [weatherHumidity: tempWeatherHumidity]
                    if ((tempWeatherDewpoint != null) && 	((changeOften[tid][8]  != tempWeatherDewpoint) || userPChanged)) 			data << [weatherDewpoint: myConvertTemperatureIfNeeded(
                    																																					(tempWeatherDewpoint.toInteger() / 10.0), "F", userPrecision)]
                    if (tempWeatherPressure &&				( changeOften[tid][9]  != tempWeatherPressure)) 							data << [weatherPressure: (usingMetric ? tempWeatherPressure : 
                    																																							 roundIt((tempWeatherPressure * 0.02953),2))] //milliBars to inHg
                    if (tempWeatherLowForecast && 			( changeOften[tid][14] != tempWeatherLowForecast)) 							data << [weatherTempLowForecast: tempWeatherLowForecast]
                }
                changeOften[tid] = [tempTemperature,occupancy,runtime?.actualHumidity,null,timeOfDay,wSymbol,tempWeatherTemperature,tempWeatherHumidity,tempWeatherDewpoint,
                                    tempWeatherPressure,humiditySetpoint,dehumiditySetpoint,humiditySetpointDisplay,userPrecision,tempWeatherLowForecast]
                atomicState.changeOften = changeOften
                //if (TIMERS) log.debug "TIMER: Finished queueing Runtime data for ${tstatName} @ ${now() - atomicState.pollEcobeeAPIStart}ms"
            }

			// Some of these can change without changing what is shown on the thermostat device, but updates like (schedule & climateChange) may have
            // Helper dependencies, so we send the timestamps even if nothing else changed.
            if (runtimeUpdated) 	data << [runtimeUpdated: 	statTime[tid]]
            if (alertsUpdated) 		data << [alertsUpdated: 	statTime[tid]]
            if (settingsUpdated) 	data << [settingsUpdated: 	statTime[tid]]
            if (programUpdated) 	data << [programUpdated: 	statTime[tid]]
            if (climatesUpdated) 	data << [climatesUpdated: 	statTime[tid]]
            if (scheduleUpdated) 	data << [scheduleUpdated: 	statTime[tid]]
            if (curClimRefUpdated)	data << [curClimRefUpdated: statTime[tid]]
            if (eventsUpdated) 		data << [eventsUpdated: 	statTime[tid]]
            if (locationUpdated)	data << [locationUpdated:	statTime[tid]]
            if (audioUpdated) 		data << [audioUpdated: 		statTime[tid]]
            if (extendRTUpdated) 	data << [extendRTUpdated:	statTime[tid]]
            if (weatherUpdated) 	data << [weatherUpdated: 	statTime[tid]]
            if (sensorsUpdated) 	data << [sensorsUpdated: 	statTime[tid]]
            if (equipUpdated) 		data << [equipUpdated: 		statTime[tid]]
            if (statInfoUpdated)	data << [statInfoUpdated:	statTime[tid]]
            
            if (debugLevelFour) LOG("updateThermostatData() - Event data updated for thermostat ${tstatName} (${tid}) = ${data}", 1, null, 'trace')
            
            // it is possible that thermostatSummary indicated things have changed that we don't care about...
            // so don't send ANYTHING if we only have the initial forcePoll status for this tid
            if (data != dataStart) {
                data << [ thermostatTime:statTime[tid] ]
                if (forcePoll) data << [forced: false]			// end of forced update
                tstatNames += [tstatName]
                if (debugLevelFour) LOG("${tstatName} data: ${data}",1,null,'info')
                int ds = data.size()
                totalUpdates += forcePoll ? (ds -1) : ds
                collector[DNI] = [thermostatId:tid, data:data]
                //data = null
            }
            return collector
        } else {
            //log.debug "NO UPDATES (${tstatName})!!!"
            if (debugLevelThree) LOG("updateThermostatData() - No updates to event data for thermostat ${tstatName} (${tid})", 3, null, 'info')
            collector[DNI] = [thermostatId:tid, data:[]]
            return collector
        }
    }
	atomicState.thermostats = statData
	Integer nSize = tstatNames.size()
	//if (nSize > 1) tstatNames.sort()
	//LOG("${preText}${totalUpdates} update${totalUpdates!=1?'s':''} for ${nSize} thermostat${nSize!=1?'s':''} ${nSize!=0?'('+tstatNames.toString()[1..-2]+')':''}", 2, null, 'info')
    LOG("${totalUpdates} updates for ${nSize} thermostats",2,null,'info')
	//if (TIMERS) log.debug "TIMER: Thermostats update completed @ ${now() - atomicState.pollEcobeeAPIStart}ms"
}

String whichStage(equipStatus, auxHeatMode, heatStages) { 
    if		(equipStatus.contains('t1'))	{ return ((auxHeatMode) ? 'emergency' : ((heatStages > 1) ? 'heat 1' : 'heating')) }
    else if (equipStatus.contains('mpHo'))	{ return 'compHotWater' }
    else if (equipStatus.contains('uxHo'))	{ return 'auxHotWater' }
    else if (equipStatus.contains('t2'))	{ return 'heat 2' }
    else if (equipStatus.contains('t3'))	{ return 'heat 3' }
    else if (equipStatus.contains('p2'))	{ return 'heat pump 2' }
    else if (equipStatus.contains('p3'))	{ return 'heat pump 3' }
    else if (equipStatus.contains('mp'))	{ return 'heat pump' }
}

String getChildThermostatDeviceIdsString(singleStat = null) {
	boolean debugLevelFour = debugLevel(4)
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
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG('Entered refreshAuthToken()', 1, null, 'trace')	

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
//			method: 'POST',
			uri: 		apiEndpoint,
			path: 		'/token',
			query:		[grant_type: 'refresh_token', refresh_token: "${atomicState.refreshToken}", client_id: ecobeeApiKey],
            timeout: 	30
		]

		if (debugLevelFour) LOG("refreshParams = ${refreshParams}", 1, null, 'trace') // 4

		def jsonMap
		try {			 
			httpPost(refreshParams) { resp ->
				if (debugLevelFour) LOG("Inside httpPost resp handling.", 1, child, "trace")
				if(resp && resp.isSuccess() && resp.status && (resp.status == 200)) {
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
						if (debugLevelFour) LOG("jsonMap: ${jsonMap}", 1, child, 'trace') // 4
						
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
						if (debugLevel(3)) LOG("refreshAuthToken() - Success! Token expires in ${String.format("%.2f",resp?.data?.expires_in/60)} minutes", 3, child, 'info') // 3
						if (debugLevelFour) {  // 4
							LOG("Updated state.authTokenExpires = ${atomicState.authTokenExpires}", 2, child, 'trace')
							LOG("Refresh Token = state =${atomicState.refreshToken} == in: ${resp?.data?.refresh_token}", 2, child, 'trace')
							LOG("OAUTH Token = state ${atomicState.authToken} == in: ${resp?.data?.access_token}", 2, child, 'trace')
						}
						
						String action = atomicState.action
						// Reset saved action
						atomicState.action = ''
						if (action) { // && atomicState.action != "") {
							if (debugLevelFour) LOG("Token refreshed. Rescheduling aborted action: ${action}", 1, child, 'trace')  // 4
							//runIn( 2, "${action}", [overwrite: true]) // this will collapse multiple threads back into just one
                            "${action}"()
						} else {
							// Assume we had to re-authorize during a pollEcobeeAPI session
							atomicState.inPollChildren = false
							runIn( 2, pollChildren, [overwrite: true])
						}
					} else {
						LOG("No jsonMap??? ${jsonMap}", 2, child, 'trace')
					}	
					// scheduleWatchdog(null, false) 
					// Reschedule polling if it has been a while since the previous poll	
					if (!isDaemonAlive("poll")) { LOG("refreshAuthToken - rescheduling poll daemon",1,null,'warn'); spawnDaemon("poll") }
					if (!isDaemonAlive("watchdog")) { LOG("refreshAuthToken - rescheduling watchdog daemon",1,null,'warn'); spawnDaemon("watchdog") }
					if (atomicState.inTimeoutRetry) atomicState.inTimeoutRetry = 0
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
				runIn(watchdogInterval, refreshAuthToken, [overwrite: true])
			} else if ((e.statusCode == 401) || (e.statusCode == 400)) {			
				atomicState.reAttempt = atomicState.reAttempt + 1
				if (atomicState.reAttempt > 5) {						
					apiLost("Too many retries (${atomicState.reAttempt - 1}) for token refresh.")				
					result = false
				} else {
					if (debugLevelFour) LOG("Setting up runIn for refreshAuthToken", 1, child, 'trace') // 4
					if ( HE || canSchedule() ) {						
						runIn(watchdogInterval, refreshAuthToken, [overwrite: true]) 
					} else { 
						if (debugLevelFour) LOG("Unable to schedule refreshAuthToken, running directly", 1, child, 'trace')			 // 4			
						result = refreshAuthToken(child) 
					}
				}
			}
			generateEventLocalParams() // Update the connected state at the thermostat devices
            if (resp) resp = [:]
			return result
		} catch (java.util.concurrent.TimeoutException e) {
			LOG("refreshAuthToken(), TimeoutException: ${e}.", 1, child, "warn")
			// Likely bad luck and network overload, move on and let it try again
			if ( HE || canSchedule() ) { runIn(watchdogInterval, refreshAuthToken, [overwrite: true]) } else { refreshAuthToken(child) }		
            if (resp) resp = [:]
			return false
		} catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException | 
				 javax.net.ssl.SSLPeerUnverifiedException | javax.net.ssl.SSLHandshakeException | 
				 java.net.SocketTimeoutException | java.net.NoRouteToHostException | java.net.UnknownHostException |
				 groovyx.net.http.ResponseParseException | java.lang.reflect.UndeclaredThrowableException e) {
			
			LOG("refreshAuthToken() - ${e} - will retry",1,child,'warn')  // Just log it, and hope for better next time...
			if (apiConnected() != 'warn') {
				atomicState.connected = 'warn'
				updateMyLabel()
				atomicState.lastPoll = now()
				atomicState.lastPollDate = getTimestamp()
				generateEventLocalParams()
			}
			def inTimeoutRetry = atomicState.inTimeoutRetry
			if (inTimeoutRetry == null) inTimeoutRetry = 0
			// try to re-Authorize for 5 minutes
			if (inTimeoutRetry < 20) runIn(watchdogInterval, refreshAuthToken, [overwrite: true])
			atomicState.inTimeoutRetry = inTimeoutRetry + 1
			//LOG("refreshAuthToken(), ConnectTimeoutException: ${e}.",1,null,'warn')
			//if(canSchedule()) { runIn(watchdogInterval, "refreshAuthToken", [overwrite: true]) } else { refreshAuthToken() }
            if (resp) resp = [:]
			return false
		} catch (Exception e) {
			LOG("refreshAuthToken() - Exception: ${e}.", 1, child, "error")	
            if (resp) resp = [:]
			throw e
			return false
		}
	}
	return true
}

void queueFailedCall(String routine, String DNI, numArgs, arg1=null, arg2=null, arg3=null, arg4=null, arg5=null, arg6=null, arg7=null) {
	//log.debug "queueCall routine: ${routine}, DNI: ${DNI}, ${arg1}, ${arg2}, ${arg3}, ${arg4}, ${arg5}, ${arg6}, ${arg7}"	 // ${routine}" // , args: ${theArgs}"
	if ((atomicState.connected == 'full') && atomicState.runningCallQueue) return // don't queue when we are clearing the queue
	runIn(2, queueCall, [overwrite: false, data: [routine: routine, DNI: DNI, args: [arg1, arg2, arg3, arg4, arg5], done: false, numArgs: numArgs]])
	if (atomicState.callsQueued == null) { atomicState.callsQueued = 0; atomicState.callsRun = 0; }
	atomicState.callsQueued = atomicState.callsQueued + 1
}

void queueCall(data) {
	def dbgLvl = 4
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
	
	LOG("failedCallQueue: ${failedCallQueue}", 3, null, 'trace')
	//atomicState.callQueue = toJson(failedCallQueue)
	atomicState.callQueue = failedCallQueue
	
	// runIn( 5, runCallQueue )
}

void runCallQueue() {
	def dbgLvl = 4
	if (debugLevel(dbgLvl)) LOG("runCallQueue() connected: ${atomicState.connected}, callQueue: ${atomicState.callQueue}, runningCallQueue: ${atomicState.runningCallQueue}", dbgLvl, null, 'trace')
	if (atomicState.connected?.toString() != 'full') return
    
    def queue = atomicState.callQueue
	if (!callQueue || (callQueue?.size() == 0)) {
    	atomicState.runningCallQueue = false
        LOG("Call Queue is empty",3,null,'info')
        return
    }
    
	if (atomicState.runningCallQueue) {
    	def timeLeft = now() - atomicState.runningCallQueueStarted
    	if (timeLeft < (callQueue.size() * 15000)) {	// 15 seconds per queued call
    		LOG("callQueue is already running - ${timeLeft/1000} seconds until restart",3,null,'warn')
    		return
        }
    }
	atomicState.runningCallQueue = true
    atomicState.runningCallQueueStarted = now()
    LOG("callQueue starting...",2,null,'trace')
	
	//while (atomicState.connected == 'full') {
	boolean failed = false
	boolean result = true
	
	int i
	for (i=0; i<queue.size(); i++) {
		//log.debug queue."${i}"
		def cmd = queue."${i}"
		LOG("${i}: ${cmd}", dbgLvl, null, 'debug')
		if (cmd?.done?.toString() == 'false') {		
			if (atomicState.connected == 'full') {
				// execute the command
                if (cmd.routine == 'setProgramSetpoint') cmd.routine = 'setProgramSetpoints'	// fix for a past mistake **TEMPORARY**
				def command = "${cmd.routine}(getChildDevice(${cmd.DNI})"
				cmd.args?.each {
					if (it == [:]) it = null
				}
				command += ')'
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
                    
				if (result) {
					queue = atomicState.callQueue
					cmd = queue."${i}"
					cmd.done = true
					queue."${i}" = cmd
					atomicState.callQueue = queue
					atomicState.callsRun = atomicState.callsRun + 1
				} else {
                LOG("callQueue failed on entry ${i} - aborting",1,null,'error')
					i = queue.size()
					failed = true
				}
			}
		}
	}
	if (!failed) { atomicState.callQueue = [:]; LOG("callQueue completed and cleared", 2, null, 'trace'); }
	atomicState.runningCallQueue = false
	atomicState.queueingFailedCalls = false
}

boolean resumeProgram(child, String deviceId, resumeAll=true) {
	if (child == null) {
		child = getChildDevice(getThermostatDNI(deviceId))
	}
    // log.debug "resumeProgram: ${child.device.displayName}"
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to resumeProgram(${child.device.displayName}, ${deviceId}, ${resumeAll})", 2, child, 'warn')
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
	if (debugLevelFour) LOG("jsonRequestBody = ${jsonRequestBody}", 1, child, 'trace')
	
	result = sendJson(child, jsonRequestBody)
	LOG("resumeProgram(${resumeAll}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child,result?'info':'warn')
	if (result) {
		//def program = atomicState.program[deviceId]
        def climates = atomicState.climates[deviceId]
		//if ( program ) {
        def climateId = atomicState.currentClimateRef[deviceId]
        def climate = climates?.find { it.climateRef == climateId }
		def climateName = climate?.name

        // send the new heat/cool setpoints and ProgramId to the DTH - it will update the rest of the related displayed values itself
        Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
        Integer userPrecision = getTempDecimals()						// user's requested display precision
        def tempHeatAt = climate.heatTemp
        def tempCoolAt = climate.coolTemp
        def tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt.toInteger() / 10.0), 'F', userPrecision)
        def tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt.toInteger() / 10.0), 'F', userPrecision)
        def updates = [	[heatingSetpoint:	tempHeatingSetpoint], // String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
                        [coolingSetpoint:	tempCoolingSetpoint], // String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
                        [currentProgramName:climateName],
                        [currentProgram:	climateName],
                        [currentProgramId:	climateId],
                      ]
        LOG("resumeProgram(${statName}) ${updates}",2,null,'info')
        child.generateEvent(updates)			// force-update the calling device attributes that it can't see

		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to resumeProgram(${child.device.displayName}, ${deviceId}, ${resumeAll})", 2, child, 'warn')
			queueFailedCall('resumeProgram', child.device.deviceNetworkId, 2, deviceId, resumeAll)
		}
	}
	return result
}

boolean setHVACMode(child, deviceId, mode) {
	LOG("setHVACMode(${mode})", 4, child, 'trace')
	def result = setMode(child, mode, deviceId)
	LOG("setHVACMode(${mode}) for ${child.device.displayName} returned ${result}", 3, child, result?'info':'warn')	 
	// if (result) atomicState.forcePoll = true			// force next poll to get updated runtime data
	// if (!result) queue failed request
	return result
}

boolean setMode(child, mode, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setMode(${child.device.displayName}, ${mode}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setMode', child.device.deviceNetworkId, 2, mode, deviceId)
		return false
	}
    boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("setMode(${mode}) for ${child.device.displayName} (${deviceId})", 1, child, 'trace')
	// queueFailedCall('setMode', child, mode, deviceId)
		
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"hvacMode":"'+"${mode}"+'"}}}'  
	if (debugLevelFour) LOG("Mode Request Body = ${jsonRequestBody}", 1, child, 'trace')
	
	def result = sendJson(child, jsonRequestBody)
	LOG("setMode(${mode}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child,  result?'info':'warn')
	if (result) {
		// LOG("setMode(${mode}) for ${child.device.displayName} (${deviceId}) - Succeeded", 1, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API is connection lost, queueing failed call to setMode(${child.device.displayName}, ${mode}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setMode', child.device.deviceNetworkId, 2, mode, deviceId)
		} else {
			LOG("setMode(${mode}) for ${child.device.displayName} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result
}

boolean setHumidifierMode(child, mode, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour = debugLevel(4)
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHumidifierMode(${child.device.displayName}, ${mode}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setHumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
		return false
	}
    
	if (debugLevelFour) LOG ("setHumidifierMode(${mode}) for ${child.device.displayName} (${deviceId})", 1, child, 'trace')
	
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"humidifierMode":"'+"${mode}"+'"}}}'	
	def result = sendJson(child, jsonRequestBody)
	LOG("setHumidifierMode(${mode}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child,  result?'info':'warn')
	if (result) {
		// LOG("setHumidifierMode(${mode}) for ${child.device.displayName} (${deviceId}) - Succeeded", 2, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setHumidifierMode(${child.device.displayName}, ${mode}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setHumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
			return false
		} else {
			LOG("setHumidifierMode(${mode}) for ${child.device.displayName} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result
}

boolean setHumiditySetpoint(child, value, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour = debugLevel(4)
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHumiditySetpoint(${child.device.displayName}, ${value}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setHumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
		return false
	}

	if (debugLevelFour) LOG ("setHumiditySetpoint${value}) for ${deviceId}", 4, child, 'trace')
						
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"humidity":"'+"${value}"+'"}}}'  
	if (debugLevelFour) LOG("setHumiditySetpoint Request Body = ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("setHumiditySetpoint(${value}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')
	if (result) {
		//LOG("setHumiditySetpoint(${value}) for ${child.device.displayName} (${deviceId}) - Succeeded", 2, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setHumiditySetpoint(${child.device.displayName}, ${value}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setHumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
		} else {
			LOG("setHumiditySetpoint(${value}) for ${child.device.displayName} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result	
}

boolean setDehumidifierMode(child, mode, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour = debugLevel(4)
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setDehumidifierMode(${child.device.displayName}, ${mode}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setDehumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
		return false
	}
	
	LOG ("setDehumidifierMode(${mode}) for ${child.device.displayName} (${deviceId})", 5, child, 'trace')
						
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"dehumidifierMode":"'+"${mode}"+'"}}}'  

	if (debugLevelFour) LOG("dehumidifierMode Request Body = ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("setDehumidifierMode(${mode}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child,  result?'info':'warn')
	if (result) {
		//LOG("setDehumidifierMode(${mode}) for ${child.device.displayName} (${deviceId}) - Succeeded", 2, child, 'info')
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setDehumidifierMode(${child.device.displayName}, ${mode}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setDehumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
		} else {
			LOG("setDehumidifierMode(${mode}) for ${child.device.displayName} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result
}

boolean setDehumiditySetpoint(child, value, deviceId) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour = debugLevel(4)
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setDehumiditySetpoint(${child.device.displayName}, ${value}, ${deviceId}", 2, child, 'warn')
		queueFailedCall('setDehumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
		return false
	}

	if (debugLevelFour) LOG ("setDehumiditySetpoint${value}) for ${child.device.displayName} (${deviceId})", 4, child, 'trace')
						
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"dehumidifierLevel":"'+"${value}"+'"}}}'	
	if (debugLevelFour) LOG("setDehumiditySetpoint Request Body = ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
    LOG("setDehumiditySetpoint(${value}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')	
	if (result) {
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setDehumiditySetpoint(${child.device.displayName}, ${value}, ${deviceId}", 2, child, 'warn')
			queueFailedCall('setDehumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
		} else {
			LOG("setDehumiditySetpoint (${mode}) for ${child.device.displayName} (${deviceId}) - Failed", 1, child, 'warn')
		}
	}
	return result	
}

boolean setFanMinOnTime(child, deviceId, howLong) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour = debugLevel(4)
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setFanMinOnTime(${child.device.displayName}, ${deviceId}, ${howLong}", 2, child, 'warn')
		queueFailedCall('setFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
		return false
	}

	if (debugLevelFour) LOG("setFanMinOnTime(${howLong}) for ${child.device.displayName} (${deviceId})", 4, child, 'trace')
	
	if ((howLong < 0) || (howLong > 55)) {
		LOG("setFanMinOnTime() for ${child.device.displayName} (${deviceId}) - Invalid Argument ${howLong}",2,child,'warn')
		return false
	}
	
	def thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":'+howLong+'}}'
	def thermostatFunctions = ''
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
	def result = sendJson(child, jsonRequestBody)
	LOG("setFanMinOnTime(${howLong}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')	 
	if (result) {
		// atomicState.forcePoll = true			// force next poll to get updated runtime data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setFanMinOnTime(${child.device.displayName}, ${deviceId}, ${howLong}", 2, child, 'warn')
			queueFailedCall('setFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
		}
	}
	return result
}

boolean setVacationFanMinOnTime(child, deviceId, howLong) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour = debugLevel(4)
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setVacationFanMinOnTime(${child.device.displayName}, ${deviceId}, ${howLong}", 2, child, 'warn')
		queueFailedCall('setVacationFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
		return false
	}

	if (debugLevelFour) LOG("setVacationFanMinOnTime(${howLong}) for ${child.device.displayName} (${deviceId})", 4, child)
	if (!howLong.isInteger() || (howLong < 0) || (howLong > 55)) {		// Documentation says 60 is the max, but the Ecobee3 thermostat maxes out at 55 (makes 60 = 0)
		LOG("setVacationFanMinOnTime() for ${child.device.displayName} (${deviceId}) - Invalid Argument ${howLong}",2,child,'warn')
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
		LOG("setVacationFanMinOnTime() for ${child.device.displayName} (${deviceId}) - Vacation not active", 2, child, 'warn')
		return false
	}
	if (evt.fanMinOnTime.toInteger() == howLong.toInteger()) return true	// didn't need to do anything!
	
	if (deleteVacation(child, deviceId, evt.name)) { // apparently on order to change something in a vacation, we have to delete it and then re-create it..
  
		def thermostatSettings = ''
		def thermostatFunctions = '{"type":"createVacation","params":{"name":"' + evt.name + '","coolHoldTemp":"' + evt.coolHoldTemp + '","heatHoldTemp":"' + evt.heatHoldTemp + 
									'","startDate":"' + evt.startDate + '","startTime":"' + evt.startTime + '","endDate":"' + evt.endDate + '","endTime":"' + evt.endTime + 
									'","fan":"' + evt.fan + '","fanMinOnTime":"' + "${howLong}" + '"}}'
		def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'

		if (debugLevelFour) LOG("setVacationFanMinOnTime() for ${child.device.displayName} (${deviceId}) - before sendJson() jsonRequestBody: ${jsonRequestBody}", 4, child, "info")

		def result = sendJson(child, jsonRequestBody)
		LOG("setVacationFanMinOnTime(${howLong}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn') 
		if (result) {
			// atomicState.forcePoll = true			// force next poll to get updated data
			runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
		} else {
			if (atomicState.connected?.toString() != 'full') {
				LOG("API connection lost, queueing Failed call to setVacationFanMinOnTime(${child.device.displayName}, ${deviceId}, ${howLong}", 2, child, 'warn')
				queueFailedCall('setVacationFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
			}		 
		}
		return result
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setVacationFanMinOnTime(${child.device.displayName}, ${deviceId}, ${howLong}", 2, child, 'warn')
			queueFailedCall('setVacationFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
		}
		return false
	}
	return false
}

boolean createVacationTemplate(child, deviceId) {
	String vacationName = 'tempVac810n'
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour = debugLevel(4)
	
	// delete any old temporary vacation that we created
	deleteVacation(child, deviceId, vacationName)
	
	// Create the temporary vacation
	def thermostatSettings = ''
	def thermostatFunctions =	'{"type":"createVacation","params":{"name":"' + vacationName + 
								'","coolHoldTemp":"850","heatHoldTemp":"550","startDate":"2034-01-01","startTime":"08:30:00","endDate":"2034-01-01","endTime":"09:30:00","fan":"auto","fanMinOnTime":"5"}}'
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'

	if (debugLevelFour) LOG("before sendJson() jsonRequestBody: ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("createVacationTemplate(${vacationName}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')
	// if (!result) queue failed request
	
	// Now, delete the temporary vacation
	result = deleteVacation(child, deviceId, vacationName)
	// LOG("deleteVacation(${vacationName}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')
	return true
}

boolean deleteVacation(child, deviceId, vacationName=null ) {
	def vacaName = vacationName
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (!vacaName) {		// no vacationName specified, let's find out if one is currently running
		def evt = atomicState.runningEvent[deviceId]
		if (!evt ||	 (evt.running != true) || (evt.type != "vacation") || !evt.name) {
			LOG("deleteVacation() for ${child.device.displayName} (${deviceId}) - Vacation not active", 1, child, 'warn')
			return true	// Asked us to delete the current vacation, and there isn't one - I'd still call that a success!
		}
		vacaName = evt.name as String		// default names are Very Big Numbers
	}

	def thermostatSettings = ''
	def thermostatFunctions = '{"type":"deleteVacation","params":{"name":"' + vacaName + '"}}'
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
	boolean result = sendJson(child, jsonRequestBody)
	LOG("deleteVacation() for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child,result?'info':'warn')
	
	if (vacationName == null) {
		resumeProgram(child, deviceId, true)		// force back to previously scheduled program
		//pollChildren(deviceId,true)					// and finally, update the state of everything (hopefully)
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
		LOG("API is not fully connected, queueing call to cancelDemandResponse(${child.device.displayName}, ${deviceId})", 2, child, 'warn')
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
	LOG("cancelDemandResponse() for ${child.device?.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')
	return result
}

// Should only be called by child devices, and they MUST provide sendHoldType and sendHoldHours as of version 1.2.0
boolean setHold(child, heating, cooling, deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour = debugLevel(4)
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHold(${child.device.displayName}, ${heating}, ${cooling}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
		queueFailedCall('setHold', child.device.deviceNetworkId, 5, heating, cooling, deviceId, sendHoldType, sendHoldHours)
		return false
	}
	//boolean ST = atomicState.isST
	
	def currentThermostatHold = ST ? child.device.currentValue('thermostatHold') : child.device.currentValue('thermostatHold', true)
	if (currentThermostatHold == 'vacation') {
		LOG("setHold(): Can't set a new hold for ${child.device.displayName} (${deviceId}) while in a vacation hold",2,null,'warn')
		// can't change fan mode while in vacation hold, so silently fail
		return false
	}  else if (currentThermostatHold != '') {
		// must resume program first
		resumeProgram(child, deviceId, true)
	}
	def isMetric = (temperatureScale == "C")
	def h = roundIt((isMetric ? (cToF(heating) * 10.0) : (heating * 10.0)), 0)		// better precision using BigDecimal round-half-up
	def c = roundIt((isMetric ? (cToF(cooling) * 10.0) : (cooling * 10.0)), 0)
	
	LOG("setHold() for ${child.device.displayName} (${deviceId}) - h: ${heating}(${h}), c: ${cooling}(${c}), ${sendHoldType}, ${sendHoldHours}", 2, child, 'trace')
	
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
			LOG("setHold() for ${child.device.displayName} (${deviceId}) requested current setpoints; ignoring", 3, child, 'info')
			return true 
		}
	} else if (theHoldType == 'holdHours') {
		theHoldType = 'holdHours","holdHours":"' + sendHoldHours.toString()
	} 
	
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":[{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + 
							'","holdType":"' + theHoldType + '"}}]}'

	if (debugLevelFour) LOG("setHold() for thermostat ${child.device.displayName} - about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child)
	
	def result = sendJson(child, jsonRequestBody)
	LOG("setHold() for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child,result?'info':'warn')
	if (result) { 
		// send the new heat/cool setpoints and ProgramId to the DTH - it will update the rest of the related displayed values itself
		Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
		Integer userPrecision = getTempDecimals()						// user's requested display precision
		def tempHeatAt = h //.toBigDecimal()
		def tempCoolAt = c //.toBigDecimal()
		def tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt.toInteger() / 10.0), 'F', userPrecision)
		def tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt.toInteger() / 10.0), 'F', userPrecision)
		def updates = [ [heatingSetpoint: 	tempHeatingSetpoint],	// String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
					    [coolingSetpoint: 	tempCoolingSetpoint],	// String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
					    [currentProgramName: 'Hold: Temp']
					  ]
		if (debugLevelFour) LOG("setHold() for ${child.device.displayName} (${deviceId}) - ${updates}",4,null,'trace')
		child.generateEvent(updates)			// force-update the calling device attributes that it can't see
		// atomicState.forcePoll = true				// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setHold(${child.device.displayName}, ${heating}, ${cooling}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
			queueFailedCall('setHold', child.device.deviceNetworkId, 5, heating, cooling, deviceId, sendHoldType, sendHoldHours)
		}
	}
	return result
}

// Should only be called by child devices, and they MUST provide sendHoldType and sendHoldHours as of version 1.2.0
boolean setFanMode(child, fanMode, fanMinOnTime, deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    boolean debugLevelFour= debugLevel(4)
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setFanMode(${child.device.displayName}, ${fanMode}, ${fanMinOnTime}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
		queueFailedCall('setFanMode', child.device.deviceNetworkId, 5, fanMode, fanMinOnTime, deviceId, sendHoldType, sendHoldHours)
		return false
	}

	if (debugLevelFour) LOG("setFanMode(${fanMode}) for ${child.device.displayName} (${deviceId})", 4, null, 'trace') 
	boolean isMetric = (temperatureScale == "C")
	//boolean ST = atomicState.isST
	
	def currentThermostatHold = ST ? child.device.currentValue('thermostatHold') : child.device.currentValue('thermostatHold', true)
	if (currentThermostatHold == 'vacation') {
		LOG("setFanMode() for ${child.device.displayName} (${deviceId}): Can't change Fan Mode while in a vacation hold",2,null,'warn')
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
		//LOG("setFanMode(circulate) for ${child.device.displayName} (${deviceId})", 4, child, "trace") 
		fanMode = "auto"
		
		if (fanMinOnTime == null) fanMinOnTime = 20	// default for 'circulate' if not specified
		thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":"' + "${fanMinOnTime}" + '"}}'
		thermostatFunctions = '{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + '","holdType":"' + theHoldType + 
								'","fan":"'+fanMode+'","isTemperatureAbsolute":false,"isTemperatureRelative":false}}'
								
	// OFF: Sets fanMinOnTime to 0; if thermostatMode == off, this will stop the fan altogether (so long as not in a hold)
	} else if (fanMode == "off") {
    	//LOG("setFanMode(off) for ${child.device.displayName} (${deviceId})", 4, child, "trace") 
		// How to turn off the fan http://developer.ecobee.com/api/topics/how-to-turn-fan-off
		// HVACmode=='off', fanMode=='auto' and fanMinOnTime==0
		// NOTE: Once you turn it off it does not automatically come back on if you select resume program
		fanMode = "auto"   
		// thermostatSettings = ',"thermostat":{"settings":{"hvacMode":"off","fanMinOnTime":"0"}}'		// now requires thermsotatMode to already be off, instead of overwriting it here
		thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":"0"}}'
		thermostatFunctions = '{"type":"setHold","params":{"coolHoldTemp":"'+c+'","heatHoldTemp":"'+h+'","holdType":"'+theHoldType+
									'","fan":"'+fanMode+'","isTemperatureAbsolute":false,"isTemperatureRelative":false}}'
	// AUTO or ON
	} else {
		if (fanMinOnTime == null) fanMinOnTime = 0 // this maybe should be the fanTime of the current/scheduledProgram
		thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":"' + "${fanMinOnTime}" + /*'","hvacMode":"' + priorHVACMode + */ '"}}'
		thermostatFunctions = '{"type":"setHold","params":{"coolHoldTemp":"'+c+'","heatHoldTemp":"'+h+'","holdType":"'+theHoldType+
									'","fan":"'+fanMode+'","isTemperatureAbsolute":false,"isTemperatureRelative":false}}'
	}	 
	
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"'+deviceId+'"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	if (debugLevel(4)) LOG("setFanMode() for ${child.device.displayName} (${deviceId}) - about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child, 'trace')
	
	boolean result = sendJson(child, jsonRequestBody)
	LOG("setFanMode(${fanMode}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')
	// if (result) atomicState.forcePoll = true			// force next poll to get updated data
	runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	if (!result) {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setFanMode(${child.device.displayName}, ${fanMode}, ${fanMinOnTime}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
			queueFailedCall('setFanMode', child.device.deviceNetworkId, 5, fanMode, fanMinOnTime, deviceId, sendHoldType, sendHoldHours)
		}
	}
	return result	 
}

boolean setProgram(child, program, String deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
	
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setProgram(${child.device.displayName}, ${program}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
		queueFailedCall('setProgram', child.device.deviceNetworkId, 4, program, deviceId, sendHoldType, sendHoldHours)
		return false
	}

	// NOTE: Will use only the first program if there are two with the same exact name
	LOG("setProgram(${program}) for ${child.device.displayName} (${deviceId}) - holdType: ${sendHoldType}, holdHours: ${sendHoldHours}", 2, child, 'info')		
	
	String currentThermostatHold = ST ? child.device.currentValue('thermostatHold') : child.device.currentValue('thermostatHold', true)
	if (currentThermostatHold == 'vacation') {									// shouldn't happen, child device should have already verified this
		LOG("setProgram(${program}) for ${child.device.displayName} (${deviceId}): Can't change Program while in a vacation hold",2,child,'warn')
		return false
	} else if ((currentThermostatHold != null) && (currentThermostatHold != 'null') && (currentThermostatHold != '')) {									// shouldn't need this either, child device should have done this before calling us
		LOG("setProgram( ${program} ) for ${child.device.displayName} (${deviceId}): Resuming from current hold first",2,child,'info')
		resumeProgram(child, deviceId, true)
	}
	
	// We'll take the risk and use the latest received climates data (there is a small chance it could have changed recently but not yet been picked up)
	def climates = atomicState.climates[deviceId]	// .program[deviceId].climates
	def climate = climates?.find { it.name.toString() == program.toString() }  // vacation holds can have a number as their name
	LOG("climates - {$climates}", 5, child)
	LOG("climate - {$climate}", 5, child)
	def climateRef = climate?.climateRef.toString()
	
	LOG("setProgram() for ${child.device.displayName} (${deviceId}) - climateRef = {$climateRef}", 4, child, 'trace')
	
	if (climate == null) { return false }
	
	def theHoldType = sendHoldType //? sendHoldType : whatHoldType(child)
	if (theHoldType == 'holdHours') {
		theHoldType = 'holdHours","holdHours":"' + sendHoldHours.toString()
	}
	def jsonRequestBody = '{"functions":[{"type":"setHold","params":{"holdClimateRef":"'+climateRef+'","holdType":"'+theHoldType+'"}}],"selection":{"selectionType":"thermostats","selectionMatch":"'+deviceId+'"}}'

	if (debugLevel(4)) LOG("setProgram() for thermostat ${child.device.displayName}: about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child, 'trace')	
	boolean result = sendJson(child, jsonRequestBody)	
	LOG("setProgram(${climateRef}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')
	
	if (result) { 
		// send the new heat/cool setpoints and ProgramId to the DTH - it will update the rest of the related displayed values itself
		Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
		Integer userPrecision = getTempDecimals()						// user's requested display precision
		def tempHeatAt = climate.heatTemp
		def tempCoolAt = climate.coolTemp
		def tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt.toInteger() / 10.0), 'F', userPrecision)
		def tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt.toInteger() / 10.0), 'F', userPrecision)
		def updates = [ [heatingSetpoint:	tempHeatingSetpoint],	// String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
					    [coolingSetpoint: 	tempCoolingSetpoint],	// String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
					    [currentProgram:	program],
					    [currentProgramId:	climateRef]
					  ]
		LOG("setProgram() for ${child.device.displayName} (${deviceId}): ${updates}",3,child,'info')
		child.generateEvent(updates)			// force-update the calling device attributes that it can't see
		// atomicState.forcePoll = true			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API is not fully connected, queueing call to setProgram(${child.device.displayName}, ${program}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
			queueFailedCall('setProgram', child.device.deviceNetworkId, 4, program, deviceId, sendHoldType, sendHoldHours)
		} else {
			LOG("setProgram(${program}) for ${child.device.displayName} (${deviceId}) - Failed", 1, child, 'warn') 
		}
	}
	return result
}
/////////////////////////////////////////////////////////////////////////////////////
//
// TODO: create new addSensorToPrograms(child, deviceId, sensorId, programIds[])
//       create new deleteSensorFromPrograms(child, deviceId, sensorId, programIds[])
//		 make ALL add/delete*Program* synchronous - do entire request in one shot...
//       make setProgramSetpoints synchronous
//		 change Smart Vents Helper to use new *Programs calls
//		 verify other Helpers will work with newly-synchronous calls
//
/////////////////////////////////////////////////////////////////////////////////////
boolean addSensorToProgram(child, deviceId, sensorId, programId) {
	return addSensorToPrograms(child, deviceId, sensorId, [programId])
}

boolean addSensorToPrograms(child, deviceId, sensorId, List programsList) {
	return updateSensorPrograms(child, deviceId, sensorId, programsList, []) 
}

boolean deleteSensorFromProgram(child, deviceId, sensorId, programId) {
	return deleteSensorFromPrograms(child, deviceId, sensorId, [programId])
}

boolean deleteSensorFromPrograms(child, deviceId, sensorId, programsList) {
	return updateSensorPrograms(child, deviceId, sensorId, [], programsList)
}

boolean updateSensorPrograms(child, deviceId, sensorId, List activeList, List inactiveList) {
	String statName = getThermostatName( deviceId )
	String sensName = child.device?.displayName
    
	if (debugLevel(4)) LOG("updateSensorPrograms( ${sensName}, ${statName}, ${sensorId}, ${activeList}, ${inactiveList} )",1,child,'trace')
    
	def activatedProgs = 0
    def totalActiveProgs = activeList ? activeList.size() : 0
	def inactivatedProgs = 0
    def totalInactiveProgs = inactiveList ? inactiveList.size() : 0
    if ((totalActiveProgs == 0) && (totalInactiveProgs == 0)) {
    	LOG("updateSensorPrograms(): No active or inactive programs specified - Succeeded",1,child,'info')
        return true	// we did EXACTLY what we were asked!
    }
    
    // Since we now update the components of the program Map independently, we need to rebuild it for changing climate/schedule data
    def program = [currentClimateRef:atomicState.currentClimateRef[deviceId],
    			   climates:atomicState.climates[deviceId],
        		   schedule:atomicState.schedule[deviceId]] as HashMap
	if (!program) {
    	LOG("updateSensorPrograms(): FATAL ERROR, could not recreate programs Map - Failed", 1, null, 'error')
		return false
	}
    //log.debug "program.currentClimateRef: ${program.currentClimateRef}"
	boolean climatesUpdated = false
    
    boolean programFound
    boolean sensorFound
    def remoteSensors = atomicState.remoteSensors[deviceId]
	String tempSensor = "${sensorId}:1"
    //def ESModifiedClimates = []
    
    if (totalActiveProgs != 0) activeList.each { progId ->
    	//log.debug "adding ${progId}"
    	def programId
        int c = 0
        int s = 0
		programFound = false
        while (!programFound && program.climates[c] ) {	
            // Allow add by program Name (e.g. ["Home", "Awake"]) or id (e.g. ["home", "smart1"])
            programId = (program.climates[c].name == progId) ? program.climates[c].climateRef : progId
            def programName = program.climates[c].name
            if (program.climates[c].climateRef == programId) {
            	//log.debug "program found ${programId}"
            	programFound = true
                sensorFound = false
                s = 0
                while (!sensorFound && program.climates[c].sensors[s]) {
                    if (program.climates[c].sensors[s].id == tempSensor ) {
                    	sensorFound = true
                        LOG("updateSensorPrograms() - ${sensName} is already in the ${programName} program on ${statName} (${deviceId})",3,child, 'info')
                    } else s++
                }
				if (!sensorFound) {
                	String sensorName = remoteSensors.find{it.id == sensorId}?.name // always use the LOCAL name
                    sensorFound = true
                    climatesUpdated = true
                    // We intentionally don't include the name
                	//program.climates[c].sensors << [id: tempSensor /*, name: sensorName*/ ] // add this sensor to the sensors list
                    program.climates[c].sensors << [id: tempSensor, name: sensorName ] // add this sensor to the sensors list
                    //ESModifiedClimates << c		// Keep track of which climates we modify
                    LOG("updateSensorPrograms() - Adding ${sensName} to the ${programName} program on ${statName} (${deviceId})",3,child, 'info')
                }
                if (sensorFound) activatedProgs++
            }
            c++
        }
        if (!programFound) LOG("updateSensorPrograms() for ${statName} (${deviceId}): could not find '${progId}' to add ${sensName}",1,child,'warn')
    }
    if (activatedProgs != totalActiveProgs) {
    	LOG("updateSensorPrograms() for ${statName} (${deviceId}): add ${sensName} to ${activeList.toString()[1..-2]} - could not find all requested programs",1,child,'warn')
        //return false
    }
    //log.debug "adds completed"
    // OK, now we do the deletes
    if (totalInactiveProgs != 0) inactiveList.each { progId ->
    //log.debug "removing ${progId}"
    	def programId
        int c = 0
        int s = 0
        programFound = false
        while (!programFound && program.climates[c] ) {	
            // Allow add by program Name (e.g. ["Home", "Awake"]) or id (e.g. ["home", "smart1"])
            programId = (program.climates[c].name == progId) ? program.climates[c].climateRef : progId
            if (program.climates[c].climateRef == programId) {
            	programFound = true
                inactivatedProgs++
                sensorFound = false
                s = 0
                while (!sensorFound && program.climates[c].sensors[s]) {
                    if (program.climates[c].sensors[s].id == tempSensor ) {
                    	sensorFound = true
                    } else s++
                }
                if (sensorFound) {
                	// found it, now we need to delete it - subtract it from the list of sensors					
                    program.climates[c].sensors = program.climates[c].sensors - program.climates[c].sensors[s]	 
                    climatesUpdated = true
                    //if (!ESModifiedClimates || !ESModifiedClimates.contains(c)) ESModifiedClimates << c			// Keep track of the climates that we change
                    LOG("updateSensorPrograms() - Removing sensor ${sensName} from the ${programId} program on thermostat ${statName}", 3, child, 'info')
                } else {
                	LOG("updateSensorPrograms() - Sensor ${sensName} is already not in the ${programId} program on thermostat ${statName}", 3, child, 'info')
                }
            }
            c++
        }
        if (!programFound) LOG("updateSensorPrograms() for ${statName} (${deviceId}): could not find '${progId}' to remove ${sensName}", 1, child, 'warn')
    }
    if (inactivatedProgs != totalInactiveProgs) {
    	LOG("updateSensorPrograms() for ${statName} (${deviceId}): delete ${sensName} from ${inactiveList.toString()[1..-2]} - could not find all requested programs",1,child,'warn')
    }
    
    // OK, the sensor has been added and deleted to the requested programs in our staging Map, now we need to send the program Map to the Ecobee cloud
    //log.debug "climatesUpdated: ${climatesUpdated}"
    def result = climatesUpdated ? updateProgramDirect( child, deviceId, program ) : true

	//if (result) {
    //	log.debug "Modified Climates: ${ESModifiedClimates}"
    //}
	// let's tell them what happened
    String action = ((totalActiveProgs>0) && (totalInactiveProgs>0)) ? "update" : ((totalActiveProgs>0) ? "add" : "delete")
    String msg = "updateSensorPrograms() for ${statName} (${deviceId}): "
	if (totalActiveProgs > 0) {
       	msg = msg + "add ${sensName} to ${activeList.toString()[1..-2]}" + ((totalInactiveProgs > 0) ? ' and ' : '')
    }
    if (totalInactiveProgs > 0) {
       	msg = msg + "remove ${sensName} from ${inactiveList.toString()[1..-2]}"
    }
    if (result) {
        LOG(msg + " - Succeeded",2,child,'info')
        return true
    } else {
        LOG(msg + " - Failed",2,child,'warn')
        return false
    }
}

boolean setProgramSetpoints(child, String deviceId, String programName, String heatingSetpoint, String coolingSetpoint) {
	// boolean debugLevelFour = debugLevel(4)
    
	if (child == null) child = getChildDevice(getThermostatDNI(deviceId))
    String statName = child.device.displayName
    //log.debug "setProgramSetpoints() for ${statName}"
    
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setProgramSetpoints(${child.device.displayName}, ${deviceId}, ${heatingSetpoint} ${coolingSetpoint}", 2, child, 'warn')
		queueFailedCall('setProgramSetpoints', child.device.deviceNetworkId, 4, deviceId, programName, heatingSetpoint, coolingSetpoint)
		return false
	}
	
	LOG("setProgramSetpoints() for ${statName} (${deviceId}): ${programName}, heatSP: ${heatingSetpoint}, coolSP: ${coolingSetpoint})", 2, child, 'info')
	
    def program = [currentClimateRef:atomicState.currentClimateRef[deviceId],
    			   climates:atomicState.climates[deviceId],
        		   schedule:atomicState.schedule[deviceId]] as HashMap
	if (!program) {
		return false
	}
	
	// convert C temps to F
	def isMetric = (temperatureScale == "C")
	def ht = (heatingSetpoint?.isBigDecimal() ? (roundIt((isMetric ? (cToF(heatingSetpoint) * 10.0) : (heatingSetpoint * 10.0)), 0)) : null )		// better precision using BigDecimal round-half-up
	def ct = (coolingSetpoint?.isBigDecimal() ? (roundIt((isMetric ? (cToF(coolingSetpoint) * 10.0) : (coolingSetpoint * 10.0)), 0)) : null )
	
	// IFF autoHeatCoolFeatureEnabled, then enforce the minimum delta
    def hasAutoMode = atomicState.settings ? atomicState.settings[deviceId].autoHeatCoolFeatureEnabled : false
	def delta = !hasAutoMode ? 0.0 : (atomicState.settings ? atomicState.settings[deviceId]?.heatCoolMinDelta : null)
	if (hasAutoMode && (delta != null) && (ht != null) && (ct != null) && (ht <= ct) && ((ct - ht) < delta)) {
		LOG("setProgramSetpoints() - Error: Auto Mode is enabled on ${statName}, heating/cooling setpoints must be at least ${delta/10}°F apart.",1,child,'error')
		return false
	}
		
	int c = 0
	while ( program.climates[c] ) {	
		if ((program.climates[c].name == programName) || (program.climates[c].climateRef == programName)) { 	// Allow search by programName or programIc
        	// found the program we want to change
			def heatTemp = program.climates[c]?.heatTemp
			def coolTemp = program.climates[c]?.coolTemp
			def adjusted = ''
			// If we have both ht & ct and we support Auto Mode, we already know the delta is good
			// but if we only have 1, we need to determine the valid value for the other
            // if no Auto mode, we just copy the current value over, otherwise we adjust it
			if (!ct) {
				ct = (!hasAutoMode && coolTemp) ? coolTemp : ((!coolTemp || ((coolTemp - ht) < delta)) ? (ht + delta) : coolTemp)
				if (hasAutoMode && (!coolingSetpoint || (ct != cooltemp))) adjusted = 'cool'
			} else if (!ht) {
				ht = (!hasAutoMode && heatTemp) ? heatTemp : ((!heatTemp || ((ct - heatTemp) < delta)) ? (ct - delta) : heatTemp)
				if (hasAutoMode && (!heatingSetpoint || (ht != heatTemp))) adjusted = 'heat'
			}
			LOG("setProgramSetpoints(${programName}) - heatingSetpoint: ${ht/10}°F ${adjusted=='heat'?'(adjusted)':''}, coolingSetpoint: ${ct/10}°F ${adjusted=='cool'?'(adjusted)':''}${adjusted?', minDelta: '+(delta/10).toString()+'°F':''}",2,child,'info')
			program.climates[c].heatTemp = ht
			program.climates[c].coolTemp = ct

			if (updateProgramDirect( child, deviceId, program )) {
				LOG("setProgramSetpoints() for ${statName} (${deviceId}): ${programName} setpoints change - Succeeded", 2, child, 'info')
				return true
			} else {
				LOG("setProgramSetpoints() for ${statName} (${deviceId}): ${programName} setpoints change - Failed", 1, child, 'warn')
				// updateProgramDirect() will queue failed requests
				return false
			}
		}
		c++
	}
	// didn't find the specified climate
	LOG("setProgramSetpoints(): ${programName} not found for ${statName} (${deviceId})", 1, child, 'warn')
	return false
}

void updateClimates(data) {
	LOG("Running delayed Climates update",2,null,'info')
    if (!atomicState.climateChangeQueue) atomicState.climateChangeQueue = 0
    if (atomicState.climateChangeQueue > 0) atomicState.climateChangeQueue = atomicState.climateChangeQueue - 1
	updateClimatesDirect(data.child, data.deviceId)
}

// "program" must be in the format of the Ecobee program structure
boolean updateProgramDirect(child, deviceId, program) {
	if (child == null) {
		child = getChildDevice(getThermostatDNI(deviceId))
	}
    // String statName = getChildDevice(getThermostatDNI(deviceId)).displayName
    String statName = child.device.displayName
    
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to updateProgramDirect(${child.device.displayName}, ${deviceId})", 2, child, 'warn')
		queueFailedCall('updateProgramDirect', child.device.deviceNetworkId, 2, deviceId, program)
		return false
	}
    boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("Updating Program settings for ${statName} (${deviceId})", 1, child, 'info')
	boolean result = false
    if (debugLevelFour) LOG("Program settings request: ${program}", 1, child, 'info')
	if (program) {
		def programJson = JsonOutput.toJson(program)
		def thermostatSettings = ',"thermostat":{"program":' + programJson +'}'
		def thermostatFunctions = ''
		def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
		result = sendJson(child, jsonRequestBody)
		LOG("updateProgramDirect(): Updating Program settings for ${statName} (${deviceId}) returned ${result}", 2, child, result?'info':'warn')
		if (result) {
        	atomicState.programUpdatedByAPI = true	// force next poll to assert that the program map was updated
        } else {
			if (atomicState.connected?.toString() != 'full') {
				LOG("API connection lost, queueing failed call to updateProgramDirect(${child.device.displayName}, ${deviceId})", 2, child, 'warn')
				queueFailedCall('updateProgramDirect', child.device.deviceNetworkId, 2, deviceId, program)
			}
		}
	}
	// atomicState.forcePoll = true
    if (result) runIn(5, pollChildren, [overwrite: true])
	return result	 
}

boolean setEcobeeSetting(child, String deviceId, String name, String value) {
	if (child == null) {
		child = getChildDevice(getThermostatDNI(deviceId))
	}
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setEcobeeSetting(${child.device.displayName}, ${deviceId}, ${name} ${value}", 2, child, 'warn')
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
    boolean audioSetting = false
	if (EcobeeTempSettings.contains(name)) {
		// Is a temperature setting - need to convert to F*10 for Ecobee
		found = true
		def isMetric = (temperatureScale == "C")
		sendValue = ((value != null) && value.isBigDecimal()) ? (roundIt((isMetric ? (cToF(value as BigDecimal) * 10.0) : ((value as BigDecimal) * 10.0)), 0)) : null
	} else if (EcobeeSettings.contains(name)) {
		found = true
		sendValue = value.trim()	// leniency is kindness
	} else if (EcobeeROSettings.contains(name)) {
		LOG("setEcobeeSetting(name: '${name}', value: '${value}') for ${child.device.displayName} (${deviceId}) - Setting is Read Only", 1, child, 'error')
		return false
	} else if (EcobeeAudioSettings.contains(name)) {
    	found = true
        audioSetting = true
        sendValue = value.trim()
    }
	if (sendValue == null) {
		if (!found) {
			LOG("setEcobeeSetting(name: '${name}', value: '${value}') for ${child.device.displayName} (${deviceId}) - Invalid name", 1, child, 'error')
		} else {
			LOG("setEcobeeSetting(name: '${name}', value: '${value}') for ${child.device.displayName} (${deviceId}) - Invalid value", 2, child, 'error')
		}
		return false
	}
    //def jsonRequestBody
    //if (!audioSetting) {
	//	jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"'+name+'":"'+"${sendValue}"+'"}}}'
    //} else {
    //	jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"audio":{"'+name+'":"'+"${sendValue}"+'"}}}'
    //}
    def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"'+deviceId+'"},"thermostat":{"'+(audioSetting?'audio':'settings')+
    					  '":{"'+name+'":"'+"${sendValue}"+'"}}}'
	LOG("setEcobeeSetting() - Request Body: ${jsonRequestBody}", 4, child, 'trace')
	def result = sendJson(child, jsonRequestBody)
	LOG("setEcobeeSetting(name: '${name}', value: '${value}' ${value!=sendValue?'('+sendValue.toString()+')':''}) for ${child.device.displayName} (${deviceId}) returned ${result}", 2, child, 'trace')
	if (result) {
		//if (value == sendValue) {
		//	LOG("Ecobee Setting '${name}' for ${child.device.displayName} (${deviceId}) was successfully changed to '${value}'", 2, child, 'info')
		//} else {
		//	LOG("Ecobee Setting '${name}' for ${child.device.displayName} (${deviceId}) was successfully changed to '${value}' ('${sendValue}')", 2, child, 'info')
		//}
		runIn(5, pollChildren, [overwrite: true])	// Pick up the changes
		return true
	} else {
		if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to setEcobeeSetting(${child.device.displayName}, ${deviceId})", 1, child, 'warn')
			queueFailedCall('setEcobeeSetting', child.device.deviceNetworkId, 3, deviceId, name, value)
		}
		return false
	}
	return false
}

// API Helper Functions
boolean sendJson(child=null, String jsonBody) {
	def debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("sendJson() - ${jsonBody}",1,child,'debug')
	def returnStatus
	boolean result = false
	
	def cmdParams = [
		uri: apiEndpoint,
		path: "/1/thermostat",
		headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
		body: jsonBody,
        timeout: 30
	]
	
	// Just in case something goes wrong...
	atomicState.savedActionJsonBody = jsonBody
	atomicState.savedActionChild = child?.deviceNetworkId
	atomicState.action = "sendJsonRetry"
			
	try{
		httpPost(cmdParams) { resp ->
        	if (!resp || !resp.isSuccess()) {
 	        	if (!atomicState.sendJsonRetry) {
                    atomicState.sendJsonRetry = true		// retry only once
                    LOG( "sendJson() - invalid response - retrying once...", 2, null, 'info')
                    result = sendJson( child, jsonBody )	// recursively re-attempt now that the token was refreshed
                    LOG( "sendJson() - Retry ${result ? 'succeeded!' : 'failed.'}", 2, null, "${result ? 'info' : 'warn'}")
                    atomicState.sendJsonRetry = false
                    if (result) {
                        if (apiConnected() != "full") {
                            apiRestored()
                            generateEventLocalParams() // Update the connection status
                        }
                    }
                } else {
                	LOG("sendJson() - invalid response (${resp})", 1, null, 'error')
                }
            } else if (resp.status && (resp.status == 200)) {
            	returnStatus = resp.data?.status?.code
				if (debugLevelFour) LOG("sendJson() resp.status ${resp.status}, resp.data: ${resp.data}, returnStatus: ${returnStatus}", 1, child, 'trace')
				//if (debugLevelFour) LOG("Updated ${resp.data}", 1, child, 'trace')
				//returnStatus = resp.data?.status?.code
				if (returnStatus == 0) {
					if (debugLevelFour) LOG("Successful call to ecobee API.", 1, child, 'trace')
					result = true
					// Tell the children that we are once again connected to the Ecobee API Cloud
					if (apiConnected() != "full") {
						apiRestored()
						generateEventLocalParams() // Update the connection status
					}
				} else {
					LOG("sendJson() - API status = ${returnStatus}", 1, child, "error")
				}
				// Reset saved state
				atomicState.savedActionJsonBody = null
				atomicState.savedActionChild = null
			} else {
				// Should never get here as a non-200 response is supposed to trigger an Exception
				LOG("sendJson() - http status ${resp.status} - ${resp.status.code}", 2, child, "warn")
			} // resp.status if/else
		} // HttpPost
        // if (resp) resp = [:]
	} catch (groovyx.net.http.HttpResponseException e) {
		result = false // this thread failed...hopefully we can succeed after we refresh the auth_token
        def iStatus = e?.statusCode ? e.statusCode.toInteger() : null
		if (debugLevelFour) LOG("sendJson() ${iStatus} ${e.response?.data?.status?.code}",1,null,"trace")
		if (iStatus && (iStatus == 500) && (e.response?.data?.status?.code == 14)) {
			LOG("sendJson() - HttpResponseException occurred: Auth_token has expired", 3, null, "trace")
			// atomicState.savedActionJsonBody = jsonBody
			// atomicState.savedActionChild = child.deviceNetworkId
			// atomicState.action = "sendJsonRetry"
			atomicState.action = ""					// we don't want refreshAuthToken to sendJsonRetry - we will retry ourselves instead
			if (debugLevelFour) LOG( "Refreshing your auth_token!", 4)
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
				LOG( "Sending: Auth_token refresh failed", 1, null, 'warn') 
			}
        } else if (((iStatus == null) && (e?.message?.contains('timed out') || e?.message?.contains('timeout'))) || 
				   (iStatus && (((iStatus > 400) && (iStatus < 405)) || (iStatus == 408) || (iStatus > 500)))) { //((iStatus > 500) && (iStatus < 505))) {
			// Retry on transient, recoverable error codes (timeouts, server temporarily unavailable, etc.) see https://en.wikipedia.org/wiki/List_of_HTTP_status_codes 
            if (iStatus && (iStatus != 525)) {
            	LOG("sendJson() - httpPost error: ${iStatus}, ${e?.message}, ${e.response?.data} - will retry", 1, null, 'warn')	// Just log it, and hope for better next time...
            } else {
            	LOG("sendJson() - httpPost timeout - will retry", 1, null, 'warn')
            }
			//log.debug "sendJson() - <b>ERROR</b> e: ${e}, \nmessage: ${e.message}, \nstatusCode: ${e.statusCode}, \nresponse.data: ${e.response?.data}"
			if (apiConnected() != 'warn') {
				atomicState.connected = 'warn'
				updateMyLabel()
			}
            if (!atomicState.sendJsonRetry) {
				atomicState.sendJsonRetry = true		// retry only once
				LOG( "sendJson() - Retrying once...", 2, null, 'info')
				result = sendJson( child, jsonBody )	// recursively re-attempt now that the token was refreshed
				LOG( "sendJson() - Retry ${result ? 'succeeded!' : 'failed.'}", 2, null, "${result ? 'info' : 'warn'}")
				atomicState.sendJsonRetry = false
                if (result) {
                	if (apiConnected() != "full") {
						apiRestored()
						generateEventLocalParams() // Update the connection status
					}
                }
            }
		} else /* can we handle any other errors??? */ {
			// log.debug "sendJson() - <b>ERROR</b> e: ${e}, \nmessage: ${e.message}, \nstatusCode: ${e.statusCode}, \nresponse.data: ${e.response?.data}"
			LOG("sendJson() - httpPost error: ${iStatus}, ${e?.message}, ${e?.response?.data} - won't retry", 1, null, "error")
		}
        if (resp) resp = [:]
	// These appear to be transient errors, treat them all as if a Timeout...
	} catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException |
			 javax.net.ssl.SSLPeerUnverifiedException | javax.net.ssl.SSLHandshakeException | 
			 java.net.SocketTimeoutException | java.net.NoRouteToHostException | java.net.UnknownHostException |
			 groovyx.net.http.ResponseParseException | java.lang.reflect.UndeclaredThrowableException e) {
		LOG("sendJson() - ${e} - will retry",1,null,'warn')	 // Just log it, and hope for better next time...
		if (apiConnected() != 'warn') {
			atomicState.connected = 'warn'
			updateMyLabel()
			generateEventLocalParams()
		}
		// If no cached calls, retry 8 times
		// if cached calls already, or if retries failed, then queue the call
		//	  Cache Map by thermostat, order, (child & jsonBody)
		def inTimeoutRetry = atomicState.inTimeoutRetry
		if (inTimeoutRetry == null) inTimeoutRetry = 0
		if (inTimeoutRetry < 8) {
			// retry quickly...
			runIn(2, sendJsonRetry, [overwrite: true])
		}
		atomicState.inTimeoutRetry = inTimeoutRetry + 1
		result = false
        if (resp) resp = [:]
	} catch(Exception e) {
		// Might need to further break down 
		LOG("sendJson() - Exception: ${e} - won't retry", 1, child, "error")
		result = false
        if (resp) resp = [:]
		//throw e
	}
    if (resp) resp = [:]
	return result
}

boolean sendJsonRetry() {
	LOG("sendJsonRetry() called", 4)
	String child = atomicState.savedActionChild ? getChildDevice(atomicState.savedActionChild) as String : ""
	
	if (!child) {
		LOG("sendJsonRetry() - no savedActionChild!", 2, child, "warn")
		return false
	}
	if (!atomicState.savedActionJsonBody) {
		LOG("sendJsonRetry() - no saved JSON Body to send!", 2, child, "warn")
		return false
	}	   
	return sendJson(child, atomicState.savedActionJsonBody)
}

String getChildThermostatName() { return "Ecobee Suite Thermostat" }
String getChildSensorName()		{ return "Ecobee Suite Sensor" }
String getServerUrl()			{ return (ST) ? "https://graph.api.smartthings.com" : getFullApiServerUrl()}	// hubitat: /oauth/authorize}
String getShardUrl()			{ return (ST) ? getApiServerUrl() : getFullApiServerUrl()+"?access_token=${atomicState.accessToken}" }
String getCallbackUrl()			{ return (ST) ? "${serverUrl}/oauth/callback" : "https://cloud.hubitat.com/oauth/stateredirect" } // : */ "${serverUrl}/callback" } // &" + URLEncoder.encode("access_token", "UTF-8") + "=" + URLEncoder.encode(atomicState.accessToken, "UTF-8") } // #access_token=${atomicState.accessToken}" }
String getBuildRedirectUrl()	{ return (ST) ? "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${shardUrl}" : 
													   "${serverUrl}/oauth/stateredirect?access_token=${atomicState.accessToken}" }
String getStateUrl()			 { return "${getHubUID()}/apps/${app?.id}/callback?access_token=${atomicState?.accessToken}" }
String getApiEndpoint()			 { return "https://api.ecobee.com" }
String getInfo()				 { return 'info' }
String getWarn()				 { return 'warn' }
String getTrace()				 { return 'trace' }
String getDebug()				 { return 'debug' }
String getError()				 { return 'error' }

// This is the API Key from the Ecobee developer page. Can be provided by the app provider or use the appSettings
String getEcobeeApiKey() { 
	if (HE) {
		if (atomicState.apiKey == null) atomicState.apiKey = "NOpc6i5ooiLLi1VPtVlJ0uv9Nh5cCfcc"		// Ecobee key for Ecobee Suite 1.7.** on Hubitat
		return "NOpc6i5ooiLLi1VPtVlJ0uv9Nh5cCfcc"
	} else if (!appSettings.clientId) {
		if (atomicState.initialized) { 
        	return atomicState.apiKey ?: "obvlTjUuuR2zKpHR6nZMxHWugoi5eVtS"	// Original Ecobee key for Ecobee Connect && Ecobee Suite v1.0.0 -->1.6.**
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
	return (HE?'ecobee_suite-thermostat-':'') + ([app.id, tid].join('.'))
}

void LOG(message, level=3, child=null, String logType='debug', event=false, displayEvent=true) {
   	int dbgLevel = getDebugLevel()
	if (level > dbgLevel) return		// let's not waste CPU cycles if we don't have to...
	
	if (logType == null) logType = 'debug'
	String prefix = ""
	// is now a Field def logTypes = ['error', 'debug', 'info', 'trace', 'warn']
	
	if(!LogTypes.contains(logType)) {
		log.error "LOG() - Received logType (${logType}) which is not in the list of allowed types ${LogTypes}, message: ${message}, level: ${level}"
		if (event && child) { debugEventFromParent(child, "LOG() - Invalid logType ${logType} (warn)") }
		logType = 'debug'
	}
	
	if ( logType == 'error' ) { 
		atomicState.lastLOGerror = "${message} @ ${getTimestamp()}"
		atomicState.LastLOGerrorDate = getTimestamp()		 
	}
	if ( dbgLevel == 5 ) { prefix = 'LOG: ' }
	log."${logType}" "${prefix}${message}"		  
	if (event) { debugEvent(message, displayEvent) }
	if (child) { debugEventFromParent(child, message+" (${logType})") }  
}

int getDebugLevel() {
	return (settings?.debugLevel ?: 3) as int
}

boolean debugLevel(int level=3) {
    return (getDebugLevel() >= level) 
}

void debugEvent(message, displayEvent = false) {
	def results = [
		name: 'appdebug',
		descriptionText: message,
		displayed: displayEvent
	]
	if ( debugLevel(4) ) { LOG("Generating AppDebug Event: ${results}", 3, null, 'debug') }
	sendEvent (results)
}

void debugEventFromParent(child, message) {
	if (child) { child.generateEvent([[debugEventFromParent: message]]) }
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
                            echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:(app.displayName?:(app.label?:app.name))), echoDeviceObjs)	// , changeVol, restoreVol) }
                        } else if (echo.size() == 1) {
                            echo.playAnnouncement(msg, (msgPrefix?:(app.displayName?:(app.label?:app.name))))
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
				sendPushMessage(msg)										// Push to everyone
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
                            echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:(app.displayName?:(app.label?:app.name))), echoDeviceObjs)	// , changeVol, restoreVol) }
                        } else if (echo.size() == 1) {
                            echo.playAnnouncement(msg, (msgPrefix?:(app.displayName?:(app.label?:app.name))))
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
			sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: cleanAppName(app.label?:app.name), type: 'APP_NOTIFICATION')
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
def myConvertTemperatureIfNeeded(scaledSensorValue, String cmdScale, precision) {
	if (scaledSensorValue == null) {
		LOG("Illegal sensorValue (null)", 2, null, "error")
		return null
	}
    if ( (cmdScale != 'F') && (cmdScale != 'C') ) {
    	if (cmdScale == 'dF') { 
        	cmdScale = 'F' 		// Normalize
        } else if (cmdScale == 'dC') { 
        	cmdScale = 'C'		// Normalize
        } else {
			LOG("Invalid temp scale used: ${cmdScale}", 2, null, "error")
			return roundIt(scaledSensorValue, precision)
        }
	}
	if (cmdScale == temperatureScale) {
		// The platform scale is the same as the current value scale
        return roundIt(scaledSensorValue, precision)
	} else if (cmdScale == 'F') {				
        return roundIt(fToC(scaledSensorValue), precision)
	} else {
        return roundIt(cToF(scaledSensorValue), precision)
	}
}
def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def roundIt( BigDecimal value, decimals=0) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
boolean wantMetric() {
	return (temperatureScale == 'C')
}
def cToF(temp) {
	return (temp != null) ? ((temp * 1.8) + 32) : null
}
def fToC(temp) {	
	return (temp != null) ? ((temp - 32) / 1.8) : null
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
	return settings.pollingInterval.toInteger() // (settings?.pollingInterval?.isInteger() ? settings.pollingInterval.toInteger() : 5)
}

Integer getTempDecimals() {
	return settings.tempDecimals.toInteger() // ( settings?.tempDecimals?.isInteger() ? settings.tempDecimals.toInteger() : (wantMetric() ? 1 : 0))
}

String getHoldType() {
	return settings.holdType ?: 'Until I Change'
}

Integer getHoldHours() {
	if (settings.holdType) {
		if (settings.holdType == 'Specified Hours') return ((settings.holdHours != null) && settings.holdHours?.isInteger() ? settings.holdHours.toInteger() : 2)
		else if (settings.holdType == '2 Hours') return 2
		else if (settings.holdType == '4 Hours') return 4
		else return 2
	}
}

String getTimestamp() {
	// There seems to be some possibility that the timeZone will not be returned and will cause a NULL Pointer Exception
	//def timeZone = location?.timeZone ?: ""
	//if (timeZone == "") {
    if (!location?.timeZone) {
		return new Date().format("yyyy-MM-dd HH:mm:ss z")
	} else {
		return new Date().format("yyyy-MM-dd HH:mm:ss z", location.timeZone)
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
	//if (debugLevel(4)) LOG("getTimeOfDay() - nowTime = ${nowTime}", 1, null, "trace")
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
        boolean debugLevelFour = debugLevel(4)
		settings.thermostats?.each {
			def tid = it.split(/\./).last()
			def statTimeZone = (atomicState.statLocation && atomicState.statLocation[tid]) ? atomicState.statLocation[tid].timeZone : null
			if (statTimeZone != null) {
				if (debugLevelFour) LOG("thermostat ${tid}'s timeZone ID: ${statTimeZone}",4,null,'trace')
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
			if (!statZipCode?.isInteger() || (statZipCode != 0)) { // allow non-numeric zip codes from Ecobee API
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
	if (atomicState.connected == null) { atomicState.connected = "warn"; updateMyLabel(); return "warn"; } else { return atomicState.connected; }
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
	//boolean ST = atomicState.isST
	
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
	//boolean ST = atomicState.isST
	
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
/*
String childType(child) {
	// Determine child type (Thermostat or Remote Sensor)
	if ( child.hasCapability("Thermostat") ) { return getChildThermostatName() }
	if ( child.name.contains( getChildSensorName() ) ) { return getChildSensorName() }
	return "Unknown"
}
*/
def getFanMinOnTime(child) {
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("getFanMinOnTime() - Looking up current fanMinOnTime for ${child.device?.displayName}", 1, child, 'trace')
	String devId = getChildThermostatDeviceIdsString(child)
	def fanMinOnTime = atomicState.settings[devId]?.fanMinOnTime
	if (debugLevelFour) LOG("getFanMinOnTime() fanMinOnTime is ${fanMinOnTime} for therm ${devId}", 1, child, 'trace')
	return fanMinOnTime
}

String getHVACMode(child) {
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("Looking up current hvacMode for ${child.device?.displayName}", 1, child, 'trace')
	String devId = getChildThermostatDeviceIdsString(child)	
	def hvacMode = atomicState.settings[devId].hvacMode
	if (debugLevelFour) LOG("getHVACMode() hvacMode is ${hvacMode} for therm ${devId}", 1, child, 'trace')
	return hvacMode
}

def getAvailablePrograms(thermostat) {
	if (debugLevel(4)) LOG("Looking up the available Programs for this thermostat (${thermostat})", 4, 'trace')
	String devId = getChildThermostatDeviceIdsString(thermostat)
	//def climates = atomicState.program[devId]?.climates
    def climates = atomicState.climates[devId]
	return climates?.collect { it.name }
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
//log.debug "makeReservation( ${statId}, ${childId}, ${type} )"
	String childName = getChildAppName( childId )
	if (!childName) {
		LOG("Illegal reservation attempt using childId: ${childId} - caller is not my child.",1,null,'warn')
		return
	}
	def reservations = atomicState.reservations
	if (!reservations) reservations = [:] as HashMap
	if (!reservations.containsKey(statId)) {
		reservations."${statId}" = [:] as HashMap
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
	def reservations = atomicState.reservations as HashMap
	if (reservations?."${statId}"?."${type}"?.contains(childId)) {
		reservations."${statId}"."${type}" = reservations."${statId}"."${type}" - [childId]
		atomicState.reservations = reservations
		LOG("'${type}' reservation cancelled for ${childName} (${childId})",3,null,'info')
	} else {
		LOG("'${type}' reservation doesn't exist for ${childName} (${childId})",3,null,'info')
	}
}
// Do I have a reservation?
boolean haveReservation( String statId, String childId, String type='modeOff') {
	String childName = getChildAppName( childId )
	def reservations = atomicState.reservations as HashMap
	boolean result = (reservations?."${statId}"?."${type}"?.contains(childId))
	LOG("${childName} (${childId}) does ${result?'':'not '} hold a '${type}' reservation",3,null,'info')
	return result
}
// Do any Apps have reservations?
boolean anyReservations( String statId, String type='modeOff') {
	def reservations = atomicState.reservations as HashMap
	boolean result = ((reservations?."${statId}"?.containsKey(type)) && (reservations."${statId}"."${type}" != [])) ? (reservations."${statId}"."${type}".size() != 0) : false
	LOG("${result?'Somebody':'Nobody'} holds '${type}' reservations",3,null,'info')
	return result
}
// How many apps have reservations?
Integer countReservations(String statId, String type='modeOff') {
	def reservations = atomicState.reservations as HashMap
	Integer result = (((reservations?."${statId}"?.containsKey(type)) && (reservations."${statId}"."${type}" != [])) ? reservations."${statId}"."${type}".size() : 0)
	LOG("There ${result>1?'are':'is'} ${result} '${type}' reservation${result>1?'s':''}",3,null,'info')
	return result
}
// Get the list of app IDs that have reservations
List getReservations(String statId, String type='modeOff') {
	def reservations = atomicState.reservations as HashMap
	def result = ((reservations?."${statId}"?.containsKey(type)) && (reservations."${statId}"."${type}" != [])) ? reservations."${statId}"."${type}" : []
	LOG("appIds holding '${type}' reservations: ${result}",3,null,'info')
	return result
}
// Get the list of app Names that have reservations
List getGuestList(String statId, String type='modeOff') {
	def result = []
	def reservations = atomicState.reservations as HashMap
	if ((reservations?."${statId}"?.containsKey(type)) && (reservations."${statId}"."${type}" != [])) {
		reservations."${statId}"."${type}".each {
			result << getChildAppName( it )
		}
	}
	LOG("Apps holding '${type}' reservations: ${result.toString()[1..-2]}",3,null,'info')
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
// Audio settings
@Field final List audioNamesList = 		['microphoneEnabled','playbackVolume','soundAlertVolume','soundTickVolume','voiceEngines'
										]
// Settings (attributes)
@Field final List settingsNamesList =	['autoAway','backlightOffDuringSleep','backlightOffTime','backlightOnIntensity','backlightSleepIntensity','coldTempAlertEnabled','compressorProtectionMinTime','condensationAvoid',
										 'coolingLockout','dehumidifyWhenHeating','dehumidifyWithAC','disablePreCooling','disablePreHeating','drAccept','eiLocation','electricityBillCycleMonths','electricityBillStartMonth',
										 'electricityBillingDayOfMonth','enableElectricityBillAlert','enableProjectedElectricityBillAlert','fanControlRequired','followMeComfort','groupName','groupRef','groupSetting','hasBoiler',
										 /*hasDehumidifier,*/'hasElectric','hasErv','hasForcedAir','hasHeatPump','hasHrv','hasUVFilter','heatPumpGroundWater','heatPumpReversalOnCool','holdAction','humidityAlertNotify',
										 'humidityAlertNotifyTechnician','humidityHighAlert','humidityLowAlert','installerCodeRequired','isRentalProperty','isVentilatorTimerOn','lastServiceDate','locale','monthlyElectricityBillLimit',
										 'monthsBetweenService','remindMeDate','serviceRemindMe','serviceRemindTechnician','smartCirculation',/*'soundAlertVolume','soundTickVolume',*/'stage1CoolingDissipationTime',
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
										 'smartCirculation'/*,'soundAlertVolume','soundTickVolume'*/,'stage1CoolingDissipationTime','stage1HeatingDissipationTime','tempAlertNotifyTechnician','ventilatorDehumidify','ventilatorFreeCooling',
										 'ventilatorMinOnTimeAway','ventilatorMinOnTimeHome','ventilatorOffDateTime', 'ventilatorType'
										]
@Field final List EcobeeeAudioSettings =['microphoneEnabled','playbackVolume','soundAlertVolume','soundTickVolume'
										]
// Settings that are Read Only and cannot be changed directly
@Field final List EcobeeROSettings =	['coolMaxTemp','coolMinTemp','coolStages','hasBoiler','hasDehumidifier','hasElectric','hasErv','hasForcedAir','hasHeatPump','hasHrv','hasHumidifier','hasUVFilter','heatMaxTemp','heatMinTemp',
										 'heatPumpGroundWater','heatStages','userAccessCode','userAccessSetting','temperature','humidity','currentProgram','currentProgramName','currentProgramId','currentProgramOwner',
										 'currentProgramType','scheduledProgram','scheduledProgramName','scheduledProgramId','scheduledProgramOwner','scheduledProgramType','debugLevel','decimalPrecision','voiceEngines'
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

@Field final List LogTypes = 			['error', 'debug', 'info', 'trace', 'warn']

@Field final List DOW = 				['Mon','Tue','Wed','Thu','Fri','Sat','Sun']	// Ecobee's order for program.schedule[day][climateIds...

void updateMyLabel() {
	if (ST) return								// ST doesn't support the colored label approach
	
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

String getTheBee	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=78 height=78 align=right></img>'}
String getTheBeeLogo()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg width=30 height=30 align=left></img>'}
String getTheSectionBeeLogo()		{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=25 height=25 align=left></img>'}
String getTheBeeUrl ()				{ return "https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg" }
String getTheBlank	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/blank.png width=400 height=35 align=right hspace=0 style="box-shadow: 3px 0px 3px 0px #ffffff;padding:0px;margin:0px"></img>'}
String pageTitle 	(String txt) 	{ return HE ? getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') : txt }
String pageTitleOld	(String txt)	{ return HE ? getFormat('header-ecobee','<h2>'+txt+'</h2>') 	: txt }
String sectionTitle	(String txt) 	{ return HE ? getTheSectionBeeLogo() + getFormat('header-nobee','<h3><b>&nbsp;&nbsp;'+txt+'</b></h3>')	: txt }
String smallerTitle	(String txt) 	{ return txt ? (HE ? '<h3><b>'+txt+'</b></h3>' 				: txt) : '' }
String sampleTitle	(String txt) 	{ return HE ? '<b><i>'+txt+'<i></b>'			 				: txt }
String inputTitle	(String txt) 	{ return HE ? '<b>'+txt+'</b>'								: txt }
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
			return HE ? "<b>NOTE: </b>${myText}" : "NOTE: ${myText}"
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
