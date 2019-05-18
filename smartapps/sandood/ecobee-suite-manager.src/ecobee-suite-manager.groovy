/**
 *  Based on original code Copyright 2015 SmartThings
 *	Additional changes Copyright 2016 Sean Kendall Schneyer
 *  Additional changes Copyright 2017, 2018, 2019 Barry A. Burke
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
 *	Ecobee Suite Service Manager
 *
 *	Original Author: scott
 *	Date: 2013
 *
 *	Updates by Barry A. Burke (storageanarchy@gmail.com) 2016, 2017, 2018 & 2019
 *
 *  See Github Changelog for complete change history
 * 	<snip>
 *	1.5.00- Release number synchronization
 *	1.5.01- pollEcobeeAPI() now uses asynchttp
 *	1.5.02-	Cleanup of obsolete code
 *	1.5.03- Converted all temperature calculations to BigDecimal for better precision/rounding
 *	1.5.04- Added outdoor Dewpoint, Humidity & Barometric Pressure for Smart Mode's use
 *	1.5.05- Added support for multiple SMS numbers (Contacts being deprecated by ST)
 *	1.6.00- Release number synchronization
 *	1.6.10- Re-implemented reservations 
 *	1.6.11- Removed location.contactBook support - deprecated by SmartThings
 *	1.6.12- Cleaned up null values during climate changes
 *	1.6.13- Cleaned up setpoint/setpointDisplay stuff (added iOS/Android preference setting)
 *	1.6.14- Fixed Sensor temp 'unknown' --> null
 *	1.6.15- Cleaned up initialization process
 *	1.6.16- Fixed case of Home/Away when Auto Home/Away
 *	1.6.17- Fixed sensor off-line error handling
 *	1.6.18- Ensure test/dummy children are always deleted
 *	1.6.19-	Add hubId to device creation; cleanup removeChildDevice 
 *	1.6.20- Fixed unintended recursion
 *  1.6.21- Improve error logging
 *	1.6.22- SmartThings started throwing errors on some Canadian zipcodes - we now catch these and use geographic coordinates instead...
 *	1.6.23-	Added setProgramSetpoint, fixed typo & null-checking that was breaking some error handling
 *	1.6.24- Added more null-handling in http error handlers
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *  1.7.01 - changed setProgram() Hold: warning to info, fixed another resp.data==null error
 */
def getVersionNum() { return "1.7.01" }
private def getVersionLabel() { return "Ecobee Suite Manager,\nversion ${getVersionNum()} on ${getHubPlatform()}" }
private def getMyNamespace() { return "sandood" }

import groovy.json.*
import groovy.transform.Field

private def getHelperSmartApps() {
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
            title: "New Smart Mode & Setpoints Helper..."],
        [name: "ecobeeRoomChild", appName: "ecobee Suite Smart Room",
        	namespace: myNamespace, multiple: true,
            title: "New Smart Room Helper..."],
        [name: "ecobeeSwitchesChild", appName: "ecobee Suite Smart Switches",
        	namespace: myNamespace, multiple: true,
            title: "New Smart Switch/Dimmer/Vent Helper..."],
        [name: "ecobeeVentsChild", appName: "ecobee Suite Smart Vents",
        	namespace: myNamespace, multiple: true,
            title: "New Smart Vents Helper..."],
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
	name: "Ecobee Suite Manager",
	namespace: myNamespace,
	author: "Barry A. Burke (storageanarchy@gmail.com)",
	description: "Connect your Ecobee thermostats and sensors to SmartThings, along with a Suite of Helper SmartApps.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: true,
	oauth: true,
    pausable: false
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
    // page(name: "addWatchdogDevicesPage")
    page(name: "helperSmartAppsPage")    
    // Part of debug Dashboard
    page(name: "debugDashboardPage")
    page(name: "pollChildrenPage")
    page(name: "updatedPage")
    page(name: "refreshAuthTokenPage")    
}

mappings {
	path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}	// ST only
	// path("/oauth/stateredirect") {action: [GET: "callback"]}	// HE only
	path("/callback") {action: [GET: "callback"]}
	path("/oauth/callback") {action: [GET: "callback"]}
	
}

// Begin Preference Pages
def mainPage() {	
	def deviceHandlersInstalled 
    def readyToInstall 
    
    // Request the Ask Alexa Message Queue list as early as possible (isn't a problem if Ask Alexa isn't installed)
	if (isST) {
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
    
	dynamicPage(name: "mainPage", title: "Welcome to ${getVersionLabel()}", install: readyToInstall, uninstall: false, submitOnChange: true) {
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
            	paragraph "Please click 'Done' to save your credentials. Then re-open the SmartApp to continue the setup."
            }
        }

		// Need to save the initial login to setup the device without timeouts
		if(atomicState.authToken != null && atomicState.initialized) {
        	if (settings.thermostats?.size() > 0 && atomicState.initialized) {
            	section("Helper SmartApps") {
                	href ("helperSmartAppsPage", title: "Helper SmartApps", description: "Tap to manage Helper SmartApps")
                }            
            }
			section("Ecobee Devices") {
				def howManyThermsSel = settings.thermostats?.size() ?: 0
                def howManyTherms = atomicState.numAvailTherms ?: "?"
                def howManySensors = atomicState.numAvailSensors ?: "?"
                
                // Thermostats
				atomicState.settingsCurrentTherms = settings.thermostats ?: []
                href ("thermsPage", title: "Thermostats", description: "Tap to select Thermostats [${howManyThermsSel}/${howManyTherms}]")                
                
                // Sensors
            	if (settings.thermostats?.size() > 0) {
					atomicState.settingsCurrentSensors = settings.ecobeesensors ?: []
                	def howManySensorsSel = settings.ecobeesensors?.size() ?: 0
                    if (howManySensorsSel > howManySensors) { howManySensorsSel = howManySensors } // This is due to the fact that you can remove alread selected hiden items
            		href ("sensorsPage", title: "Sensors", description: "Tap to select Sensors [${howManySensorsSel}/${howManySensors}]")
	            }
    	    }        
	        section("Preferences") {
    	    	href ("preferencesPage", title: "Ecobee Suite Preferences", description: "Tap to review global settings")
				if (isST) {
					href ("askAlexaPage", title: "Ask Alexa Preferences", description: "Tap to review Ask Alexa settings")
				} // askAlexa not (yet) supported on Hubitat
        	}
            
//            if( useWatchdogDevices == true ) {
//            	section("Extra Poll and Watchdog Devices") {
//                	href ("addWatchdogDevicesPage", title: "Watchdog Devices", description: "Tap to select Poll and Watchdog Devices")
//                }
//            }
           
    	} // End if(atomicState.authToken)
        
        // Setup our API Tokens       
		section("Ecobee Authentication") {
			href ("authPage", title: "Ecobee API Authorization", description: "${ecoAuthDesc}Tap for ecobee Credentials")
		}      
		if ( debugLevel(5) ) {
			section ("Debug Dashboard") {
				href ("debugDashboardPage", description: "Tap to enter the Debug Dashboard", title: "Debug Dashboard")
			}
		}
		section("Remove Ecobee Suite") {
			href ("removePage", description: "Tap to remove ${app.name}", title: "Remove this instance")
		}            
		
		section ("Name this instance of Ecobee Suite Manager") {
			label name: "name", title: "Assign a name", required: false, defaultValue: app.name, submitOnChange: true
			if (isHE) {
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

	// atomicState.accessToken = createAccessToken()
	log.debug "accessToken: ${atomicState.accessToken}, ${state.accessToken}"
	
	if(!atomicState.accessToken) { //this is an access token for Ecobee to make a callback into Ecobee Suite Manager (this code)
		try {
			atomicState.accessToken = createAccessToken()
       	} catch(Exception e) {
    		LOG("authPage() --> OAuth Exception: ${e}", 1, null, "error")
            LOG("authPage() --> Probable Cause: OAuth not enabled in ${isHE?'Hubitat':'SmartThings'} IDE for the 'Ecobee Suite Manager' ${isHE?'App':'SmartApp'}", 1, null, 'warn')
        	if (!atomicState.accessToken) {
            	LOG("authPage() --> No OAuth Access token", 3, null, 'error')
        		return dynamicPage(name: "authPage", title: "OAuth Initialization Failure", nextPage: "", uninstall: true) {
            		section() {
                		paragraph "Error initializing Ecobee Authentication: could not get the OAuth access token.\n\nPlease verify that OAuth has been enabled in " +
							"the ${isHE?'Hubitat':'SmartThings'} IDE for the 'Ecobee Suite Manager' ${isHE?'App':'SmartApp'}, and then try again.\n\nIf this error persists, view Live Logging in the IDE for " +
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
		description = "You are connected. Tap Done/Next above."
		uninstallAllowed = true
		oauthTokenProvided = true
        apiRestored()
	} else {
		description = "Tap to enter ecobee Credentials"
	}
	// HE OAuth process is slightly different than SmartThings OAuth process
	def redirectUrl = isST ? buildRedirectUrl : oauthInitUrl()

	// get rid of next button until the user is actually auth'd
	if (!oauthTokenProvided) {
		LOG("authPage() --> Valid ${isST?'ST':'HE'} OAuth Access token (${atomicState.accessToken}), need Ecobee OAuth token", 3, null, 'trace')
		LOG("authPage() --> RedirectUrl = ${redirectUrl}", 3, null, 'info')
		return dynamicPage(name: "authPage", title: "ecobee Setup", nextPage: "", uninstall: uninstallAllowed) {
			section() {
				paragraph "Tap below to log in to the ecobee service and authorize ${isST?'SmartThings':'Hubitat'} access. Be sure to press the 'Allow' button on the 2nd page."
				href url: redirectUrl, style: "${isST?'embedded':'external'}", required: true, title: "ecobee Account Authorization", description: description
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
    LOG("thermsPage() -> thermostat list: ${stats}")
    LOG("thermsPage() starting settings: ${settings}")
    LOG("thermsPage() params passed? ${params}", 4, null, "trace")

    dynamicPage(name: "thermsPage", title: "Thermostat Selection", params: params, nextPage: "", content: "thermsPage", uninstall: false) {    
    	section("Tap below to see the list of Ecobee thermostats available in your ecobee account and select the ones you want to connect to SmartThings.") {
			LOG("thermsPage(): atomicState.settingsCurrentTherms=${atomicState.settingsCurrentTherms}   thermostats=${settings.thermostats}", 4, null, "trace")
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

	def options = getEcobeeSensors() ?: []
	def numFound = options.size() ?: 0
    
    LOG("options = getEcobeeSensors == ${options}")

    dynamicPage(name: "sensorsPage", title: "Sensor Selection", nextPage: "") {
		if (numFound > 0)  {
			section("Tap below to see the list of ecobee sensors available for the selected thermostat(s) and choose the ones you want to connect to SmartThings."){
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
	dynamicPage(name: "askAlexaPage", title: "Ask Alexa Integration", nextPage: "") {
	
		if (isHE) {
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
    dynamicPage(name: "preferencesPage", title: "Update Ecobee Suite Manager Preferences", nextPage: "") {
		if (isST) {
			section("Notifications") {
				paragraph "Notifications are only sent when the Ecobee API connection is lost and unrecoverable, at most once per hour."
				input(name: "phone", type: "string", title: "Phone number(s) for SMS, example +15556667777 (separate multiple with ; )", required: false, submitOnChange: true)
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
			section("Use Notification Device(s)") {
				paragraph "Notifications are only sent when the Ecobee API connection is lost and unrecoverable, at most 1 per hour."
				input(name: "notifiers", type: "capability.notification", title: "", required: ((settings.phone == null) && !settings.speak), multiple: true, 
					  description: "Select notification devices", submitOnChange: true)
				paragraph ""
			}
			section("Use SMS to Phone(s) (limit 10 messages per day)") {
				input(name: "phone", type: "string", title: "Phone number(s) for SMS, example +15556667777 (separate multiple with , )", 
					  required: ((settings.notifiers == null) && !settings.speak), submitOnChange: true)
				paragraph ""
			}
			section("Use Speech Device(s)") {
				input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
				if (settings.speak) {
					input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
					input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
					if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
				}
				paragraph "A 'HelloHome' notification is always sent to the Location Event log\n"
			}
		}
		
		if (isST) {
			section("Select your mobile device type to enable device-specific optimizations") {
        		input(name: 'mobile', type: 'enum', options: ["Android", "iOS"], title: 'Tap to select your mobile device type', defaultValue: 'iOS', submitOnChange: true, required: true, multiple: false)
			}
        }
		
      	section("How long do you want Hold events to last?") {
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
    
    dynamicPage(name: "debugDashboardPage", title: "") {
    	section (getVersionLabel())
		section("Commands") {
        	href(name: "pollChildrenPage", title: "", required: false, page: "pollChildrenPage", description: "Tap to execute: pollChildren()")
            href(name: "refreshAuthTokenPage", title: "", required: false, page: "refreshAuthTokenPage", description: "Tap to execute: refreshAuthToken()")
            href(name: "updatedPage", title: "", required: false, page: "updatedPage", description: "Tap to execute: updated()")
        }    	
        
    	section("Settings Information") {
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
        section("Dump of Debug Variables") {
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
    	section("Commands") {
        	href(name: "pollChildrenPage", title: "", required: false, page: "pollChildrenPage", description: "Tap to execute command: pollChildren()")
            href ("removePage", description: "Tap to remove Ecobee Suite Manager ", title: "")
        }
    }    
}

// pages that are part of Debug Dashboard
def pollChildrenPage() {
	LOG("=====> pollChildrenPage() entered.", 1)
    atomicState.forcePoll = true // Reset to force the poll to happen
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

	LOG("SmartApps available are ${getHelperSmartApps()}", 5, null, "info")
	
 	dynamicPage(name: "helperSmartAppsPage", title: "Configured Helper SmartApps", install: true, uninstall: false, submitOnChange: true) { 
    	section("Avalable Helper SmartApps") {
			getHelperSmartApps().each { oneApp ->
				LOG("Processing the app: ${oneApp}", 4, null, "trace")            
            	def allowMultiple = isST ? oneApp.multiple.value : oneApp.multiple
	           	app(name:"${isST?oneApp.name.value:oneApp.name}", appName:"${isST?oneApp.appName.value:oneApp.appName}", namespace:"${isST?oneApp.namespace.value:oneApp.namespace}", 
					title:"${isST?oneApp.title.value:oneApp.title}", multiple: allowMultiple)
			}
		}
	}
}
// End Preference Pages

// Preference Pages Helpers
private def Boolean testForDeviceHandlers() {
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
    def success
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

def delayedRemoveChildren() {
	def myChildren = getAllChildDevices()
	if (myChildren.size() > 0) removeChildDevices( myChildren, true )
}

private removeChildDevices(devices, dummyOnly = false) {
	if (devices != []) {
    	LOG("Removing ${dummyOnly?'test':''} child devices",3,null,'trace')
	} else {
    	return		// nothing to remove
    }
    def devName
    try {
    	devices?.each {
        	devName = it.displayName
            LOG("Child: ${it.deviceNetworkId} - ${devName}",2,null,'trace')
    		if (!dummyOnly || it?.deviceNetworkId?.startsWith('dummy')) deleteChildDevice(it.deviceNetworkId)
    	}
    } catch (Exception e) {
    	LOG("Error ${e} removing device ${devName}",1,null,'warn')
    }
}

// End Preference Pages Helpers

// Ask Alexa Helpers
private String getMQListNames() {
	if (!listOfMQs || (listOfMQs?.size() == 0)) return ''
	def temp = []
   	listOfMQs?.each { 
    	temp << atomicState.askAlexaMQ?.getAt(it).value
    }
    return ((temp.size() > 1) ? ('(' + temp.join(", ") + ')') : (temp.toString()))?.replaceAll("\\[",'').replaceAll("\\]",'')
}

def askAlexaMQHandler(evt) {
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
                	def deleteType = (evt.jsonData && evt.jsonData?.deleteType) ? evt.jsonData.deleteType : ''  // deleteType: "delete", "delete all" or "expire"
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

def deleteAskAlexaAlert( String type, String msgID ) {
	LOG("Deleting ${type} Ask Alexa message ID ${msgID}",2,null,'info')
    sendLocationEvent( name: 'AskAlexaMsgQueueDelete', value: type, unit: msgID, isStateChange: true, data: [ queues: listOfMQs ])
}

def acknowledgeEcobeeAlert( String deviceId, String ackRef ) {
    // 					   {"functions":[{"type":"resumeProgram"}],"selection":{"selectionType":"thermostats","selectionMatch":"YYY"}}
    // def jsonRequestBody = '{"functions":[{"type":"acknowledge"}],"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '","ackRef":"' + ackRef + '","ackType":"accept"}}'
    
    // 					   {"functions":[{"type":"resumeProgram","params":{"resumeAll":"'+allStr+'"}}],"selection":{"selectionType":"thermostats","selectionMatch":"YYY"}}
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
	
	def chg = false
	if (isST && atomicState?.initialized && (atomicState?.apiKey == null)) chg = true; atomicState.initialized = false;  // Force changing over the the new ST key
	def oauthParams = [
		response_type: 	"code",
		client_id: 		ecobeeApiKey,					// actually, the Ecobee Suite app's client ID
		scope: 			"smartRead,smartWrite,ems", 
		redirect_uri: 	callbackUrl,
		state: 			atomicState.oauthInitState
	]
	if (isST && chg) atomicState.initialized = true
		
	LOG("oauthInitUrl - ${isST?'Before redirect:':''} location: ${apiEndpoint}/authorize?${toQueryString(oauthParams)}", 2, null, 'debug')
	if (isST) {
		redirect(location: "${apiEndpoint}/authorize?${toQueryString(oauthParams)}")
	} else {
		return "${apiEndpoint}/authorize?${toQueryString(oauthParams)}"
	}
}
private parseAuthResponse(resp) {
	log.debug "response data: ${resp.data}"
	log.debug "response.headers: "
	resp.headers.each {
	   log.debug "${it.name} : ${it.value}"
	}
	log.debug "response contentType: ${resp.contentType}"

    log.debug("response status: "+resp.status)
	log.debug "response params: "
	resp.params.each {
		log.debug "${it.names}"
	}
}
// OAuth Callback URL and helpers
void callback() {
	LOG("callback()>> params: $params, params.code ${params.code}, params.state ${params.state}, atomicState.oauthInitState ${atomicState.oauthInitState}", 2, null, 'debug')
	def code = params.code
	def oauthState = params.state

	//verify oauthState == atomicState.oauthInitState, so the callback corresponds to the authentication request
	if (oauthState == atomicState.oauthInitState){
    	LOG("callback() --> States matched!", 1)
		def tokenParams = [
			"grant_type": "authorization_code",
			code      : code,
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
    <p>Your ecobee Account is now connected to SmartThings!</p>
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

	def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Ecobee & SmartThings connection</title>
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
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
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
        
        //  the Thermostat Data. Will reuse for the Sensor List intialization
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
		// 		 This is needed to work around the dynamic enum "bug" which prevents proper deletion
        LOG("remoteSensors all before each loop: ${atomicState.remoteSensors}", 5, null, "trace")
		atomicState.remoteSensors[tid].each {
        	if (debugLevelFour) LOG("Looping through each remoteSensor. Current remoteSensor: ${it}", 5, null, "trace")
			if (it?.type == "ecobee3_remote_sensor") {
            	if (debugLevelFour) LOG("Adding an ecobee3_remote_sensor: ${it}", 4, null, "trace")
				def value = "${it?.name} (${it?.code})"
				def key = "ecobee_suite-sensor-${it?.id}-${it?.code}"
				sensorMap["${key}"] = value
			} else if ( (it?.type == "thermostat") && (showThermsAsSensor == true) ) {            	
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
     
def getThermostatDisplayName(stat) {
	if(stat?.name)
		return stat.name.toString()
	return (getThermostatTypeName(stat) + " (${stat.identifier})").toString()
}

def getThermostatTypeName(stat) {
	return stat.modelNumber == "siSmart" ? "Smart Si" : "Smart"
}

void installed() {
	LOG("Installed with settings: ${settings}",2,null,'warn')	
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
    LOG("Updated with settings: ${settings}",2,null,'warn')	
    
    if (!atomicState.atomicMigrate) {
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
	        	LOG("Traversing atomicState: ${it} name: ${it.key}  value: ${it.value}")
    	    }
		} catch (Exception e) {
        	LOG("Unable to traverse atomicState", 2, null, "warn")
        }
        atomicState.atomicMigrate = true
    }
    initialize()
}

private def updateMyLabel() {
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
	def newLabel
	switch (atomicState.connected?.toString()) {
		case 'full':
			newLabel = myLabel + "<span style=\"color:green\"> Online</span>"
			break;
		case 'warn':
			newLabel = myLabel + "<span style=\"color:orange\"> Warning</span>"
			break;
		case 'lost':
			newLabel = myLabel + "<span style=\"color:red\"> Offline</span>"
			break;
		default:
			newLabel = myLabel
			break;
	}
	if (newLabel && (app.label != newLabel)) app.updateLabel(newLabel)
}

def initialize() {	
    LOG("=====> initialize()", 4)    
    
	LOG("Running on ${getHubPlatform()}.", 1, null, 'info')
	
	atomicState.timers = false
	
    atomicState.connected = "full"
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
    if (isST) subscribe(location, "AskAlexaMQ", askAlexaMQHandler) // HE version doesn't support Ask Alexa yet
	// if (!askAlexa) atomicState.askAlexaAlerts = null
    
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
    atomicState.getWeather = true
    atomicState.runtimeUpdated = true
    atomicState.thermostatUpdated = true
    atomicState.sendJsonRetry = false
    atomicState.forcePoll = true				// make sure we get ALL the data after initialization
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
    def isOk = false
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
    atomicState.reAttemptInterval = 15 	// In seconds
	
    if (atomicState.initialized) {		
    	// refresh Thermostats and Sensor full lists
    	getEcobeeThermostats()
    	getEcobeeSensors()
    } 
    
    // Children
    def aOK = true
	if (settings.thermostats?.size() > 0) { aOK = aOK && createChildrenThermostats() }
	if (settings.ecobeesensors?.size() > 0) { aOK = aOK && createChildrenSensors() }    
    deleteUnusedChildren()
   
   	// Clear out all atomicState object collections (in case a thermostat was deleted or replaced, so we don't slog around useless old data)
    atomicState.alerts = [:]
    atomicState.askAlexaAlerts = [:]
    atomicState.askAlexaAppAlerts = [:]
    atomicState.changeCloud = [:]
    atomicState.changeConfig = [:]
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
    atomicState.statLocation = [:]
    atomicState.weather = [:]
    
	// Initial poll()
    if (settings.thermostats?.size() > 0) { pollInit() }
    
    // Add subscriptions as little "daemons" that will check on our health    
    subscribe(location, scheduleWatchdog)
    if (isST) subscribe(location, "routineExecuted", scheduleWatchdog)		// HE doesn't support Routines
    subscribe(location, "sunset", sunsetEvent)
    subscribe(location, "sunrise", sunriseEvent)
    subscribe(location, "position", scheduleWatchdog)
       
    // Schedule the various handlers
    LOG("Spawning scheduled events from initialize()", 5, null, "trace")
    if (settings.thermostats?.size() > 0) { 
    	LOG("Spawning the poll scheduled event. (thermostats?.size() - ${settings.thermostats?.size()})", 4)
    	spawnDaemon("poll", false) 
	} 
    spawnDaemon("watchdog", false)
    
    //send activity feeds to tell that device is connected
    def notificationMessage = aOK ? "is connected to SmartThings" : "had an error during setup of devices"
    sendActivityFeeds(notificationMessage)
    atomicState.timeSendPush = null
    if (!atomicState.initialized) {
    	atomicState.initialized = true
        // These two below are for debugging and statistics purposes
        atomicState.initializedEpic = nowTime
        atomicState.initializedDate = nowDate
	}
	runIn(60, forceNextPoll, [overwrite: true])		// get ALL the data once things settle down
    LOG("${getVersionLabel()} - initialization complete",1,null,'debug')
    atomicState.versionLabel = getVersionLabel()
    return aOK
}

private def createChildrenThermostats() {
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
		return d
	}
    LOG("Created/Updated ${devices.size()} thermostats", 4, null, 'trace')    
    return true
}

private def createChildrenSensors() {
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
		return d
	}
	LOG("Created/Updated ${sensors.size()} sensors.", 4, null, 'trace')
    return true
}

// somebody pushed my button - do a force poll
def appHandler(evt) {
	if (evt.value == 'touch') {
    	LOG('appHandler(touch) event, forced poll',2,null,'info')
		atomicState.forcePoll = true
        atomicState.getWeather = true	// update the weather also
        pollEcobeeAPI('')
    	//pollChildren(null, true)		// double force-poll
        //atomicState.runningCallQueue = false
        //atomicState.callQueue = [:]
        //resumeProgram(null, '311019854581', true)
    }
}

// For thermostat reservations handling
def isChildApp(String childId) {
	def isChild = false
    def children = getChildApps()
    children.each {
    	if (isChild || (it.id == childId)) {
        	isChild = true
        }
    }
    return isChild
}
String getChildAppName(String childId) {
	String childName = null
    def children = getChildApps()
    children.each {
    	if (!childName && (it.id == childId)) {
        	childName = it.label
        }
    }
    return childName
}

// NOTE: For this to work correctly getEcobeeThermostats() and getEcobeeSensors() should be called prior
private def deleteUnusedChildren() {
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
    } else if( (sunriseAndSunset !=  [:]) && (location != null) ) {
        atomicState.sunriseTime = sunriseAndSunset.sunrise.format("HHmm").toInteger()
    } else {
    	atomicState.sunriseTime = evt.value.toInteger()
    }
	atomicState.getWeather = true	// force updating of the weather icon in the thermostat
    atomicState.forcePoll = true
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
    } else if( (sunriseAndSunset !=  [:]) && (location != null) ) {
        atomicState.sunsetTime = sunriseAndSunset.sunset.format("HHmm").toInteger()
    } else {
        atomicState.sunsetTime = evt.value.toInteger()
    }
//    if(atomicState.timeZone) {
//    	atomicState.sunsetTime = new Date().format("HHmm", atomicState.timeZone).toInteger()
//	} else {
//    	atomicState.sunsetTime = new Date().format("HHmm").toInteger()
//    }
	atomicState.getWeather = true	// force updating of the weather icon in the thermostat
    atomicState.forcePoll = true	// force a complete poll...
    scheduleWatchdog(evt, true)
}

// Event on a monitored device
def userDefinedEvent(evt) {
    if ( ((now() - atomicState.lastUserDefinedEvent) / 60000.0) < 0.5 ) { 
    	if (debugLevel(4)) LOG("userDefinedEvent() - time since last event is less than 30 seconds, ignoring.", 4, null, 'trace')
    	return 
 	}
    
    if (debugLevel(4)) LOG("userDefinedEvent() - with evt (Device:${evt?.displayName} ${evt?.name}:${evt?.value})", 4, null, "info")
    
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

def scheduleWatchdog(evt=null, local=false) {
	def results = true  
    if (debugLevel(4)) {
    	def evtStr = evt ? "${evt.name}:${evt.value}" : 'null'
    	if (debugLevel(4)) LOG("scheduleWatchdog() called with evt (${evtStr}) & local (${local})", 4, null, "trace")
    }
    
    // Only update the Scheduled timestamp if it is not a local action or from a subscription
    if ( (evt == null) && (local==false) ) {
    	atomicState.lastScheduledWatchdog = now()
        atomicState.lastScheduledWatchdogDate = getTimestamp()
        atomicState.getWeather = true								// next pollEcobeeApi for runtime changes should also get the weather object
        
        // do a forced update once an hour, just because (e.g., forces Hold Status to update completion date string)
        def counter = atomicState.hourlyForcedUpdate
        counter = (!counter) ? 1 : counter + 1
        if (counter == 6) {
        	counter = 0
            atomicState.forcePoll = true
			// log.debug "Skipping hourly forcePoll"
        }
        atomicState.hourlyForcedUpdate = counter
	}
    
    // Check to see if we have called too soon
    def timeSinceLastWatchdog = (atomicState.lastWatchdog == null) ? 0 :(now() - atomicState.lastWatchdog) / 60000
    if ( timeSinceLastWatchdog < 1 ) {
    	if (debugLevel(4)) LOG("It has only been ${timeSinceLastWatchdog} since last scheduleWatchdog was called. Please come back later.", 4, null, "trace")
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

    LOG("scheduleWatchdog() --> pollAlive==${pollAlive}  watchdogAlive==${watchdogAlive}", 4, null, "warn")
    
    // Reschedule polling if it has been a while since the previous poll    
    if (!pollAlive) { spawnDaemon("poll") }
    if (!watchdogAlive) { spawnDaemon("watchdog") }
    
    return true
}

// Watchdog Checker
private def Boolean isDaemonAlive(daemon="all") {
	// Daemon options: "poll", "auth", "watchdog", "all"    
    def daemonList = ["poll", "auth", "watchdog", "all"]
    String preText = getDebugLevel() <= 2 ? '' : 'isDeamonAlive() - '
	Integer pollingInterval = getPollingInterval()

	daemon = daemon.toLowerCase()
    def result = true    
    
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
    if (debugLevel(4)) LOG("isDaemonAlive() - result is ${result}", 4, null, "trace")
    if (!result) LOG("${preText}(${daemon}) has died", 1, null, 'warn')
    return result
}

private def Boolean spawnDaemon(daemon="all", unsched=true) {
	// log.debug "spawnDaemon(${daemon}, ${unsched})"
	// Daemon options: "poll", "auth", "watchdog", "all"    
    def daemonList = ["poll", "auth", "watchdog", "all"]
	Random rand = new Random()
	
	Integer pollingInterval = getPollingInterval()
    
    daemon = daemon.toLowerCase()
    def result = true
    
    if (daemon == "poll" || daemon == "all") {
    	if (debugLevel(4)) LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'poll'", 4, null, "trace")
        // Reschedule the daemon
        try {
            if ( unsched ) { unschedule("pollScheduled") }
            if ( isHE || canSchedule() ) { 
            	LOG("Polling Interval == ${pollingInterval}", 4)
            	if ((pollingInterval < 5) && (pollingInterval > 1)) {	// choices were 1,2,3,5,10,15,30
                	LOG("Using schedule instead of runEvery with pollingInterval: ${pollingInterval}", 4, null, 'trace')
					int randomSeconds = rand.nextInt(59)
					schedule("${randomSeconds} 0/${pollingInterval} * * * ?", "pollScheduled")                    
                } else {
                	LOG("Using runEvery to setup polling with pollingInterval: ${pollingInterval}", 4, null, 'trace')
        			"runEvery${pollingInterval}Minute${pollingInterval!=1?'s':''}"("pollScheduled")
                }
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
    	if (debugLevel(4)) LOG("spawnDaemon() - Performing seance for daemon (${daemon}) in 'watchdog'", 4, null, "trace")
        // Reschedule the daemon
        try {
            if ( unsched ) { unschedule("scheduleWatchdog") }
            if ( isHE || canSchedule() ) { 
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
        atomicState.getWeather = true	// next pollEcobeeApi for runtime changes should also get the weather object
    }
    
    if (!daemonList.contains(daemon) ) {
    	// Unkown option passed in, gotta punt
        LOG("isDaemonAlive() - Unknown daemon: ${daemon} received. Do not know how to check this daemon.", 1, null, "error")
        result = false
    }
    return result
}

def updateLastPoll(Boolean isScheduled=false) {
	if (isScheduled) {
    	atomicState.lastScheduledPoll = now()
        atomicState.lastScheduledPollDate =  getTimestamp()
    } else {
    	atomicState.lastPoll = now()
        atomicState.lastPollDate = getTimestamp()
    }
}

def poll() {		
    if (debugLevel(4)) LOG("poll() - Running at ${getTimestamp()} (epic: ${now()})", 4, null, "trace")   
	pollChildren() // Poll ALL the children at the same time for efficiency    
}

// Called by scheduled() event handler
def pollScheduled() {
	// log.debug "pollScheduled()"
	updateLastPoll(true)
	if (debugLevel(4)) LOG("pollScheduled() - Running at ${atomicState.lastScheduledPollDate} (epic: ${atomicState.lastScheduledPoll})", 4, null, "trace")    
    return poll()
}

// Called during initialization to get the inital poll
def pollInit() {
	if (debugLevel(4)) LOG("pollInit()", 4)
	runIn(2, pollChildren, [overwrite: true]) 		// Hit the ecobee API for update on all thermostats, in 2 seconds
}

def forceNextPoll() {
	atomicState.forcePoll = true
}

def pollChildren(deviceId=null,force=false) {

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
    if (deviceId != null) {
    	atomicState.forcePoll = force 	//true
        forcePoll = force 				//true
    } else {
    	forcePoll = atomicState.forcePoll
    }
	if (debugLevelFour) LOG("=====> pollChildren(${deviceId}) - atomicState.forcePoll(${forcePoll}) atomicState.lastPoll(${atomicState.lastPoll}) now(${now()}) atomicState.lastPollDate(${atomicState.lastPollDate})", 1, child, "trace")
    
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

def generateAlertsAndEvents() {
	generateTheEvents()
	if (askAlexa) {
		def startMS = now()
    	sendAlertEvents()
    	def allDone = now()
    	LOG("Alerts sent (${allDone - startMS} / ${allDone - atomicState.pollEcobeeAPIStart}ms)",2,null,'trace')
	}
}

def generateTheEvents() {
	boolean debugLevelFour = debugLevel(4)
	def startMS = now()
    def stats = atomicState.thermostats
    def sensors = atomicState.remoteSensorsData
    //log.debug stats
    stats?.each { DNI ->
    	getChildDevice(DNI.key)?.generateEvent(DNI.value.data)
    }
    sensors?.each { DNI ->
       	if (DNI.value?.data) getChildDevice(DNI.key)?.generateEvent(DNI.value.data)
    }
    def allDone = now()
    /*if (debugLevelFour) */ LOG("Updates sent (${allDone-startMS} / ${allDone-atomicState.pollEcobeeAPIStart}ms)",2,null,'trace')
}

// enables child SmartApps to send events to child Smart Devices using only the DNI
def generateChildEvent( childDNI, dataMap) {
	getChildDevice(childDNI)?.generateEvent(dataMap)
}

// NOTE: This only updates the apiConnected state now - this used to resend all the data, but that's pretty much a waste of CPU cycles now.
// 		 If the UI needs updating, the refresh now does a forcePoll on the entire device.
private def generateEventLocalParams() {
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
//       apparently updated in real time (or at least very close to real time)
private boolean checkThermostatSummary(thermostatIdsString) {
	def startMS 
	if (atomicState.timers) startMS = now()
	String preText = getDebugLevel() <= 2 ? '' : 'checkThermostatSummary() - '
    def debugLevelFour = debugLevel(4)
    if (debugLevelFour) LOG("checkThermostatSummary() - ${thermostatIdsString}",4,null,'trace')
    
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + thermostatIdsString + '","includeEquipmentStatus":"true"}}'
	def pollParams = [
			uri: apiEndpoint,
			path: "/1/thermostatSummary",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
			query: [format: 'json', body: jsonRequestBody]
	]
	
    def result = false
	def statusCode=999
	String tstatsStr = ''     
	
	try {
		httpGet(pollParams) { resp ->
			if(resp?.status == 200) {
				if (debugLevelFour) LOG("checkThermostatSummary() - poll results returned resp.data ${resp.data}", 4, null, 'trace')
				// This is crazy, I know, but ST & HE return different junk on a failed call, thus the exhaustive verification to avoid throwing errors
				if ((resp.data != null) && (resp.data instanceof Map) && resp.data.containsKey('status')  && (resp.data.status != null) 
					&& (resp.data.status instanceof Map) && resp.data.status.containsKey('code') && (resp.data.status.code != null)) {
					statusCode = resp.data.status.code
				} else {
					LOG("checkThermostatSummary() - malformed resp.data", 2, null, 'warn')
				}
				if (statusCode == 0) { 
                    def revisions = resp.data?.revisionList
                    // log.debug "${atomicState.thermostatUpdated}, ${atomicState.runtimeUpdated}, ${atomicState.alertsUpdated}"
					def thermostatUpdated = atomicState.thermostatUpdated	// false
                    def alertsUpdated = atomicState.alertsUpdated			// false
					def runtimeUpdated = atomicState.runtimeUpdated			// false
                    def getAllStats = (runtimeUpdated || thermostatUpdated || alertsUpdated )
                    tstatsStr = ""
                    def latestRevisions = [:]
						
                    // each revision is a separate thermostat
                    revisions.each { rev ->
						def tempList = rev.split(':') as List
                        def tstat = tempList[0]
                        def latestDetails = tempList.reverse().take(4).reverse() as List	// isolate the 4 timestamps

                        def lastRevisions = atomicState.lastRevisions
                        if (lastRevisions && !(lastRevisions instanceof Map)) lastRevisions = [:]			// hack to switch from List to Map for revision cache
                        def lastDetails = ( lastRevisions && lastRevisions.containsKey(tstat)) ? lastRevisions[tstat] : []			// lastDetails should be the prior 4 timestamps
                            
                        // check if the current tstat is updated
                        def tru = false
                        def tau = false
                        def ttu = false
						if (lastDetails) {			// if we have prior revision details
							if (lastDetails[2] != latestDetails[2]) tru = true 	// runtime
                            //if (settings.askAlexa && (lastDetails[1] != latestDetails[1])) tau = true	// alerts
							if (lastDetails[1] != latestDetails[1]) tau = true	// alerts
							// log.debug "alertsChanged? ${tau}"
							if (lastDetails[0] != latestDetails[0]) ttu = true	// thermostat 
                        } else {					// no priors, assume all have been updated
                            tru = true 
                            if (settings.askAlexa) tau = true
                            ttu = true
                        }

	                    // Check if the equipmentStatus changed but runtimeUpdated didn't
                    	if (!tru) {
                    		def equipStatus = (resp.data?.containsKey('statusList')) ? resp.data.statusList : null
                        	if (equipStatus != null) {
                        		def lastEquipStatus = atomicState.equipmentStatus
                                if (debugLevelFour) LOG("equipStatus: ${equipStatus}, lastEquipStatus: ${lastEquipStatus}, tid: ${tstat}", 4, null, 'trace')
                                equipStatus.each { status ->
                                	if (!tru) {
                                    	def tList = status.split(':') as List
                                    	if (tList[0] == tstat) {
                                        	if (tList[1] == null) tList[1] = ''
                                        	if (debugLevelFour) LOG("${tstat}: New: '${tList[1]}', Old: '${lastEquipStatus[tstat]}'", 4, null, 'trace')
                                    		if (tList[1] != lastEquipStatus[tstat]) tru = true
                                    	}
                                    }
                            	}
                        	}
                        	if (tru) LOG("Equipment Status changed, runtime did not", 2, null, 'info')
                    	}

                        // update global flags (we update the superset of changes for all requested tstats)
                        if (tru || tau || ttu) {
                            runtimeUpdated = (runtimeUpdated || tru) 		// || atomicState.runtimeUpdated)
                            alertsUpdated = (alertsUpdated || tau) 			// || atomicState.alertsUpdated)
                            thermostatUpdated = (thermostatUpdated || ttu) 	// || atomicState.thermostatUpdated)
                            result = true
                            if (!getAllStats) tstatsStr = (tstatsStr=="") ? tstat : (tstatsStr.contains(tstat)) ? tstatsStr : tstatsStr + ",${tstat}"
                        }
                        if (getAllStats) tstatsStr = (tstatsStr=="") ? tstat : (tstatsStr.contains(tstat)) ? tstatsStr : tstatsStr + ",${tstat}"
                        latestRevisions[tstat] = latestDetails			// and, log the new timestamps
					}
                    
					atomicState.latestRevisions = latestRevisions		// let pollEcobeeAPI update last with latest after it finishes the poll
                    atomicState.thermostatUpdated = thermostatUpdated	// Revised: settings, program, event, device
                    //atomicState.alertsUpdated = askAlexa ? alertsUpdated : false // Revised: alerts (no need to get them if we're not doing AskAlexa)
					atomicState.alertsUpdated = alertsUpdated 			// Revised: alerts (no need to get them if we're not doing AskAlexa)
					atomicState.runtimeUpdated = runtimeUpdated			// Revised: runtime, equip status, remote sensors, weather?
                    atomicState.changedThermostatIds = tstatsStr    	// only these thermostats need to be requested in pollEcobeeAPI
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
            if (debugLevelFour) LOG( "Refreshing your auth_token!", 4, null, 'trace')
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
        runIn(atomicState.reAttemptInterval.toInteger(), "pollChildren", [overwrite: true])
       	result = false
    //
    // These appear to be transient errors, treat them all as if a Timeout... 
    } catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException | 
    			javax.net.ssl.SSLPeerUnverifiedException | javax.net.ssl.SSLHandshakeException | 
    			java.net.SocketTimeoutException | java.net.NoRouteToHostException e) {
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
        if (inTimeoutRetry < 3) runIn(atomicState.reAttemptInterval.toInteger(), "pollChildren", [overwrite: true])
        atomicState.inTimeoutRetry = inTimeoutRetry + 1
        result = false
        // throw e
    } catch (Exception e) {
		LOG("checkThermostatSummary() - General Exception: ${e}, data: ${resp?.data}", 1, null, "error")
        result = false
        throw e
    }

    if (debugLevelFour) LOG("<===== Leaving checkThermostatSummary() result: ${result}, tstats: ${tstatsStr}", 4, null, "info")
	if (atomicState.timers) log.debug "TIMER: checkThermostatSummary done (${now()-startMS}ms)"
	return result
}

private def pollEcobeeAPI(thermostatIdsString = '') {
	atomicState.pollEcobeeAPIStart = now()
	boolean debugLevelFour = debugLevel(4)
    boolean debugLevelThree = debugLevel(3)
	if (debugLevelFour) LOG("=====> pollEcobeeAPI() entered - thermostatIdsString = ${thermostatIdsString}", 4, null, "info")

	boolean forcePoll = atomicState.forcePoll		// lightweight way to use atomicStates that we don't want to change under us
    boolean thermostatUpdated = atomicState.thermostatUpdated
    boolean alertsUpdated = atomicState.alertsUpdated
    boolean runtimeUpdated = atomicState.runtimeUpdated
    boolean getWeather = atomicState.getWeather
    String preText = debugLevel(2) ? '' : 'pollEcobeeAPI() - '
	def somethingChanged = (thermostatIdsString != null)
    String allMyChildren = getChildThermostatDeviceIdsString()
    String checkTherms = thermostatIdsString?:allMyChildren

	// forcePoll = true
	if (thermostatIdsString == '') {
		somethingChanged = checkThermostatSummary(thermostatIdsString)
		if (somethingChanged) {
        	thermostatUpdated = atomicState.thermostatUpdated				// update these again after checkThermostatSummary
        	alertsUpdated = atomicState.alertsUpdated
    		runtimeUpdated = atomicState.runtimeUpdated
        }
		// log.info "pollEcobeeAPI() - somethingChanged ${somethingChanged}"
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
    // This probably can't happen anymore...shouldn't event be here if nothing has changed and not a forced poll...
    if (!somethingChanged && !forcePoll) {  
    	if (debugLevelFour) LOG("<===== Leaving pollEcobeeAPI() - nothing changed, skipping heavy poll...", 4 )
       	return true		// act like we completed the heavy poll without error
	} else {
       	// if getting the weather, get for all thermostats at the same time. Else, just get the thermostats that changed
       	checkTherms = (runtimeUpdated && getWeather) ? allMyChildren : checkTherms // (forcePoll ? checkTherms : atomicState.changedThermostatIds)
    }  
    
    // Let's only check those thermostats that actually changed...unless this is a forcePoll - or if we are getting the weather 
    // (we need to get weather for all therms at the same time, because we only update every 15 minutes and use the cached version
    // the rest of the time)
    
	// get equipmentStatus every time
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + checkTherms + '","includeEquipmentStatus":"true"'
	
    String gw = '( equipmentStatus'
	if (forcePoll || thermostatUpdated) {
		jsonRequestBody += ',"includeSettings":"true","includeProgram":"true","includeEvents":"true"'
		gw += ' settings program events'
	}
    if (forcePoll) {
    	// oemCfg needed only for Carrier Cor, to determine if useSchedule is enabled - but if it isn't, we aren't of much use anyway
        // so for now, don't bother getting oemCfg data  (BAB - May 24, 2017)
    	jsonRequestBody += /*',"includeOemCfg":"true"*/ ',"includeLocation":"true"'
        gw += /*' oemCfg */ ' location'
    }
    if (forcePoll || alertsUpdated) {
    	jsonRequestBody += ',"includeAlerts":"true"'
        gw += ' alerts'
    }
	if (forcePoll || runtimeUpdated) {
		jsonRequestBody += ',"includeRuntime":"true"'
        gw += ' runtime'
        
        // only get extendedRuntime if we have a humidifier/dehumidifier (depending upon HVAC mode at the moment)
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
		gw += ' )'
        def tids = []
        tids = checkTherms.split(",")
        String names = ""
        tids.each { names = (names=="") ? getThermostatName(it) : names+", "+getThermostatName(it) }
		LOG("${preText}Requesting ${gw} for thermostat${checkTherms.contains(',')?'s':''} ${names} (${checkTherms})", 2, null, 'info')
	}
	jsonRequestBody += '}}'   
	if (debugLevelFour) LOG("pollEcobeeAPI() - jsonRequestBody is: ${jsonRequestBody}", 4, null, 'trace')
 
    // def result = false
	
	def pollParams = [
			uri: apiEndpoint,
			path: "/1/thermostat",
			headers: ["Content-Type": "${isST?'application':'text'}/json", "Authorization": "Bearer ${atomicState.authToken}"],
		query: [format: 'json', body: jsonRequestBody, 'contenttype': "${isST?'application':'text'}/json" ]
	]
	def pollState = [
			debugLevelFour: 	debugLevelFour,
        	debugLevelThree: 	debugLevelThree,
        	forcePoll: 			forcePoll,
        	thermostatUpdated: 	thermostatUpdated,
        	alertsUpdated: 		alertsUpdated,
        	runtimeUpdated: 	runtimeUpdated,
        	getWeather: 		getWeather,
        	somethingChanged: 	somethingChanged,
			thermostatIdsString:thermostatIdsString,
			jsonRequestBody:	jsonRequestBody,
        	checkTherms:		checkTherms,
	]
    
	if (atomicState.timers) log.debug "TIMER: asyncPollPrep done (${now() - atomicState.pollEcobeeAPIStart}ms)"
    atomicState.asyncPollStart = now()
	if (isST) {
		include 'asynchttp_v1'
    	asynchttp_v1.get( pollEcobeeAPICallback, pollParams, pollState )
	} else {
		asynchttpGet( pollEcobeeAPICallback, pollParams, pollState )
	}
    atomicState.waitingForCallback = true
    return true
}

def pollEcobeeAPICallback( resp, pollState ) {
	def startMS = now()
    atomicState.waitingForCallback = false
    if (atomicState.timers) log.debug "Poll callback (${startMS - atomicState.asyncPollStart}ms)"
	boolean debugLevelFour = 	pollState.debugLevelFour
    boolean debugLevelThree = 	pollState.debugLevelThree
	boolean forcePoll = 		pollState.forcePoll
    boolean thermostatUpdated = pollState.thermostatUpdated
    boolean alertsUpdated = 	pollState.alertsUpdated
    boolean runtimeUpdated = 	pollState.runtimeUpdated
    boolean getWeather = 		pollState.getWeather
    String preText = getDebugLevel() <= 2 ? '' : 'pollEcobeeAPICallback() - '
	def somethingChanged = 		pollState.somethingChanged
	def thermostatIdsString = 	pollState.thermostatIdsString
	def jsonRequestBody = 		pollState.jsonRequestBody
    def checkTherms =			pollState.checkTherms
	if (debugLevelFour) LOG("=====> pollEcobeeAPICallback() entered - thermostatIdsString = ${thermostatIdsString}", 4, null, "info")
	
	def result = true
	def tidList = []
    LOG("pollEcobeeAPICallback() status code ${resp?.status}",4,null,'trace')
    
	if (resp && /* !resp.hasError() && */ (resp.status?.toInteger() == 200)) {
		try {
			if (resp.json) {
				atomicState.thermostatData = resp.json		// this only stores the most recent collection of changed data
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
            	runIn(atomicState.reAttemptInterval.toInteger(), "pollChildren", [overwrite: true]) 
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
		// def tempOemCfg = [:]
		def tempAlerts = [:]

		// collect the returned data into temporary individual caches (because we can't update individual Map items in an atomicState Map)	
        // if (resp && resp.containsKey(data) && resp.data.containsKey(thermostatList)) { // oddly, this occaisionally returns no thermostatList :(
        if (result && resp.json.thermostatList) { // oddly, this sometimes returns no thermostatList :(
        	resp.json.thermostatList.each { stat ->
                String tid = stat.identifier.toString()
				tidList += [tid]
				String tstatName = getThermostatName(tid)
				if (debugLevelThree) LOG("Parsing data for thermostat ${tstatName} (${tid})", 3, null, 'info')
				tempEquipStat[tid] = stat.equipmentStatus 			// always store ("" is a valid return value)                    
                    	
				if (forcePoll || thermostatUpdated) {
					if (stat.settings) 	tempSettings[tid] = stat.settings
					if (stat.program) 	tempProgram[tid] = 	stat.program
					if (stat.events) 	tempEvents[tid] = 	stat.events
					if (stat.location) 	tempLocation[tid] = stat.location
					// if (stat.oemCfg) tempOemCfg[tid] = 	stat.oemCfg
				} 
				
				if (forcePoll || runtimeUpdated) {
					if (stat.runtime) 			tempRuntime[tid] = 			stat.runtime
					if (stat.extendedRuntime) 	tempExtendedRuntime[tid] = 	stat.extendedRuntime
					if (stat.remoteSensors) 	tempSensors[tid] = 			stat.remoteSensors
					// let's don't copy all that weather info...we only need some 3 bits of that info
					if (stat.weather) {
						def shortWeather = [timestamp: 		stat.weather.timestamp,
											temperature: 	stat.weather.forecasts[0]?.temperature.toString(),
                                            humidity: 		stat.weather.forecasts[0]?.relativeHumidity,
                                            dewpoint:		stat.weather.forecasts[0]?.dewpoint,
                                            pressure:		stat.weather.forecasts[0]?.pressure,
											weatherSymbol: 	stat.weather.forecasts[0]?.weatherSymbol.toString()]
						tempWeather[tid] = shortWeather
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
		if (result && (tempEquipStat != [:])) {
			if (atomicState.equipmentStatus) tempEquipStat = atomicState.equipmentStatus + tempEquipStat
			atomicState.equipmentStatus = tempEquipStat
		}
        
        // ** Thermostat Status (Program, Events, Location) **
		if (result && (forcePoll || thermostatUpdated)) {
			if (tempProgram != [:]) {
				if (atomicState.program) tempProgram = atomicState.program + tempProgram 
				atomicState.program = tempProgram
			}
			if (tempEvents != [:]) {
				if (atomicState.events) tempEvents = atomicState.events + tempEvents
				atomicState.events = tempEvents
			}
			if (tempLocation != [:]) {
				if (atomicState.statLocation) tempLocation = atomicState.statLocation + tempLocation
				atomicState.statLocation = tempLocation
			}
			// if (tempOemCfg != [:]) {
			// 	if (atomicState.oemCfg) tempOemCfg = atomicState.oemCfg + tempOemCfg
			// 	atomicState.oemCfg = tempOemCfg
			// }
			boolean needExtRT = false
			if (tempSettings != [:]) {
				if (atomicState.settings) tempSettings = atomicState.settings + tempSettings 
				atomicState.settings = tempSettings
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
					}
				} 
			}
			atomicState.needExtendedRuntime = needExtRT
		}
		
        // ** runtime Status (runtime, extended runtime, sensors, weather) **
		if (result && (forcePoll || runtimeUpdated)) {
			if (tempRuntime != [:]) {
				if (atomicState.runtime) tempRuntime = atomicState.runtime + tempRuntime
				atomicState.runtime = tempRuntime
			}
			if (tempExtendedRuntime != [:]) {
				if (atomicState.extendedRuntime) tempExtendedRuntime = atomicState.extendedRuntime + tempExtendedRuntime
				atomicState.extendedRuntime = tempExtendedRuntime
			}
			if (tempSensors != [:]) {
				if (atomicState.remoteSensors) tempSensors = atomicState.remoteSensors + tempSensors 
				atomicState.remoteSensors = tempSensors
			}
			if (tempWeather != [:]) {
				if (atomicState.weather) tempWeather = atomicState.weather + tempWeather 
				atomicState.weather = tempWeather
			}
		}
		
        // ** Alerts Status **
		if (result && (forcePoll || alertsUpdated)) {
			if (tempAlerts != [:]) {
				if (atomicState.alerts) tempAlerts = atomicState.alerts + tempAlerts
				atomicState.alerts = tempAlerts
			}
		}
		
		// OK, Everything is safely stored, let's get to parsing the objects and finding the actual changed data
		if (result) {
			updateLastPoll()
			if (debugLevelThree) LOG('Parsing complete', 3, null, 'info')
			
			// Update the data and send it down to the devices as events
			if (settings.thermostats) updateThermostatData()  	// update thermostats first (some sensor data is dependent upon thermostat changes)
			if (settings.ecobeesensors) updateSensorData()		// Only update configured sensors (and then only if they have changes)
			// if (forcePoll || alertsUpdated) /*updateAlertsData() */ runIn(20, "updateAlertsData", [overwrite: true])	// Update alerts asynchronously
			
			// pollChildren() will actually send all the events we just created, once we return
			// just a bit more housekeeping...
			result = true
			atomicState.lastRevisions = atomicState.latestRevisions		// copy the Map
			if (forcePoll) atomicState.forcePoll = false				// it's ok to clear the flag now
			if (runtimeUpdated) atomicState.runtimeUpdated = false
			// if (thermostatUpdated) { atomicState.thermostatUpdated = false }
			def needPrograms = atomicState.needPrograms
			atomicState.thermostatUpdated = thermostatUpdated ? false : needPrograms
			if (!thermostatUpdated && needPrograms) LOG("Need settings, programs & events next time", 2, null, 'info')
			if (runtimeUpdated && getWeather) atomicState.getWeather = false
			if (alertsUpdated) atomicState.alertsUpdated = false
			
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
				if (debugLevelFour) LOG("pollEcobeeAPICallback() - Updated thermostat${(numStats>1)?'s':''} ${names} (${tidList}) ${atomicState.thermostats}", 4, null, 'info')
			}
		}
        atomicState.inTimeoutRetry = 0	// Not in Timeout Recovery any longer
	} else if (resp && resp.hasError()) {
    	result = false
		if (debugLevelFour) LOG("${preText}Poll returned http status ${resp.status}, ${resp.errorMessage}", 4, null, "error")
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
            LOG("pollEcobeeAPICallback() Polling error: ${iStatus}, ${resp.errorMessage} - Will Retry", 1, null, 'warn') 	// Just log it, and hope for better next time...
        	if (apiConnected != 'warn') {
        		atomicState.connected = 'warn'
				updateMyLabel()
        		atomicState.lastPoll = now()
        		atomicState.lastPollDate = getTimestamp()
				generateEventLocalParams()
        	}
        	def inTimeoutRetry = atomicState.inTimeoutRetry
        	if (inTimeoutRetry == null) inTimeoutRetry = 0
        	if (inTimeoutRetry < 3) runIn(atomicState.reAttemptInterval.toInteger(), "pollChildren", [overwrite: true])
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
		if (alertsUpdated) {
			if (prepTime > 11000) { runIn(2, 'generateAlertsAndEvents', [overwrite: true]) } else { generateAlertsAndEvents() }
		} else {
			if (prepTime > 11000) { runIn(2, 'generateTheEvents', [overwrite: true]) } else { generateTheEvents() }
		}
	} else {
		LOG('pollEcobeeAPICallback() Error',1,null,'warn')
	}
    if (debugLevelFour) LOG("<===== Leaving pollEcobeeAPICallback() results: ${result}", 4, null, 'trace')
	return result
}

def sendAlertEvents() {
	if (atomicState.alerts == null) return				// silently return until we get the alerts environment initialized
    boolean debugLevelFour = debugLevel(4)
	// log.debug "askAlexa: ${askAlexa}"
    if (!settings.askAlexa) {
    	if (debugLevelFour) LOG("Ask Alexa support not configured, nothing to do, returning",4, null, 'trace')
    	return							// Nothing to do (at least for now)
    }
	if (debugLevelFour) LOG("Entered sendAlertEvents() ${atomicState.alerts}", 4, null, 'trace')
   
    def askAlexaAlerts = atomicState.askAlexaAlerts			// list of alerts we have sent to Ask Alexa
    if (!askAlexaAlerts) askAlexaAlerts = [:]				// make it a map if it doesn't exist yet
	def alerts = atomicState.alerts							// the latest alerts from Ecobee (all thermostats, by tid)
    def totalAlexaAlerts = 0		// including any duplicates we send
    def totalNewAlerts = 0
    def removedAlerts = 0
    
    // Initialize some Ask Alexa items
    String queues = getMQListNames()
    def exHours = expire ? expire as int : 0
    def exSec=exHours * 3600
    
    // Walk through the list of thermostats
    atomicState.thermostatData.thermostatList.each { stat ->
    	String tid = stat.identifier
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
                    def tstatTimeMS = new Date().parse('yyyy-MM-dd HH:mm:ss',stat.thermostatTime).getTime()
                    if ((tstatTimeMS - expireTimeMS) < 0) {	
                    	// hasn't expired yet, so send it/refresh it to Ask Alexa
                		String dateTimeStr = fixDateTimeString( alert.date, alert.time, stat.thermostatTime) // gets us "today" or "yesterday"
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
//    if ((askAlexaAlerts != [:]) || (newAlexaAlerts != [:])) askAlexaAlerts = askAlexaAlerts + newAlexaAlerts
    atomicState.askAlexaAlerts = askAlexaAlerts
}

private String alertTranslation( alertType, notificationType ){
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

def updateSensorData() {
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("Entered updateSensorData() ${atomicState.remoteSensors}", 4)
 	def sensorCollector = [:]
    def sNames = []
    Integer precision = getTempDecimals()
    def forcePoll = atomicState.forcePoll
    def thermostatUpdated = atomicState.thermostatUpdated
    String preText = getDebugLevel() <= 2 ? '' : 'updateSensorData() - '
    
    atomicState.thermostatData.thermostatList.each { singleStat ->
    	def tid = singleStat.identifier
        
        def isConnected = atomicState.runtime[tid]?.connected
    
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
            	              
            if (sensorDNI && settings.ecobeesensors?.contains(sensorDNI)) {		// only process the selected/configured sensors
				def temperature
				def occupancy
                Integer humidity = null
                            
				it.capability.each { cap ->
					if (cap.type == 'temperature') {
                   		if (debugLevelFour) LOG("updateSensorData() - Sensor ${it.name} temp is ${cap.value}", 4)
                       	if ( cap.value.isNumber() ) { // Handles the case when the sensor is offline, which would return "unknown"
							temperature = roundIt((cap.value.toBigDecimal() / 10.0), precision)
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
                    
                if (forcePoll) sensorData << [ thermostatId: tid /*, vents: 'default'*/ ] 		// belt, meet suspenders
                    
                // temperature usually changes every time, goes in *List[0]
                def currentTemp = ((temperature == 'unknown') ? 'unknown' : myConvertTemperatureIfNeeded(temperature, 'F', precision+1))
				if (forcePoll || (lastList[0] != currentTemp)) { sensorData << [ temperature: currentTemp ] }
                def sensorList = [currentTemp]
                    
                // occupancy generally only changes every 30 minutes, in *List[1]
               	if (forcePoll || ((occupancy != "") && (lastList[1] != occupancy))) { sensorData << [ motion: occupancy ] }
                sensorList += [occupancy]
                    
                // precision can change at any time, *List[2]
                if (forcePoll || (lastList[2] != precision)) { sensorData << [ decimalPrecision: precision ] }
                sensorList += [precision]
                    
                // currentProgramName doesn't change all that often, but it does change
                def currentPrograms = atomicState.currentProgramName
                def currentProgramName = (currentPrograms && currentPrograms.containsKey(tid)) ? currentPrograms[tid] : 'default' // not set yet, we will have to get it later. 
                if (forcePoll || (lastList[3] != currentProgramName)) { sensorData << [ currentProgramName: currentProgramName ] }
                sensorList += [currentProgramName]
                
                // not every sensor has humidity
               	if ((humidity != null) && (lastList[4] != humidity)) { sensorData << [ humidity: humidity ] }
                sensorList += [humidity]
                
               	// collect the climates that this sensor is included in
                def sensorClimates = [:]
                def climatesList = []
               	if (forcePoll || thermostatUpdated) {	// these will only change when the program object changes
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
                	if (debugLevelFour) LOG("updateSensorData() - sensor ${it.name} is used in ${sensorClimates}", 4, null, 'trace')
                } 
                
                // check to see if the climates have changed. Notice that there can be 0 climates if the thermostat data wasn't updated this time;
                // note also that we will send the climate data every time the thermostat IS updated, even if they didn't change, becuase we don't 
                // store them in the atomicState unless the thermostat object changes...
                if (forcePoll || (lastList.size() < (listSize+climatesList.size())) || (lastList.drop(listSize) != climatesList)) {	
                    sensorData << sensorClimates
                    sensorList += climatesList
                }
                
                // For Health Check, update the sensor's checkInterval  any time we get forcePolled - which should happen a MINIMUM of once per hour
                // This because sensors don't necessarily update frequently, so this simple event should be enought to let the ST Health Checker know
                // we are still alive!
                if (forcePoll) { 
            		sensorData << [checkInterval: 3900] // 5 minutes longer than an hour
                }
                
                if (forcePoll || (sensorData != [:])) {
					sensorCollector[sensorDNI] = [name:it.name,data:sensorData]
                    atomicState."${sensorDNI}" = sensorList
                    if (debugLevelFour) LOG("updateSensorData() - sensorCollector being updated with sensorData: ${sensorData}", 4, null, 'trace')
                    sNames += it.name
                }
            } // End sensor is valid and selected in settings
		} // End [tid] sensors loop
	} // End thermostats loop
    
	atomicState.remoteSensorsData = sensorCollector
    if (preText=='') {
    	def sz = sNames.size()
		if (sz > 1) sNames.sort()
        LOG("Updates for ${sz} sensor${sz!=1?'s':''}${sNames?' ('+sNames.toString()[1..-2]+')':''}",2, null, 'info')
    } else {
		if (debugLevelFour) LOG("updateSensorData() - Updated these remoteSensors: ${sensorCollector}", 4, null, 'trace') 
    }
}

def updateThermostatData() {
	// atomicState.timeOfDay = getTimeOfDay() // shouldn't need to do this - sunrise/sunset events are maintaining it
	def runtimeUpdated = atomicState.runtimeUpdated
    def thermostatUpdated = atomicState.thermostatUpdated
	boolean usingMetric = wantMetric() // cache the value to save the function calls
	def forcePoll = atomicState.forcePoll
    if (forcePoll) {
        if (atomicState.timeZone == null) getTimeZone() // both will update atomicState with valid timezone/zipcode if available
    	if (atomicState.zipCode == null) getZipCode()
    }
    Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
    Integer userPrecision = getTempDecimals()						// user's requested display precision
    String preText = getDebugLevel() <= 2 ? '' : 'updateThermostatData() - '
    def tstatNames = []
    def askAlexaAppAlerts = atomicState.askAlexaAppAlerts
    if (askAlexaAppAlerts == null) askAlexaAppAlerts = [:]
    // def needExtendedRuntime = false
    boolean debugLevelFour = debugLevel(4)
    boolean debugLevelThree = debugLevel(3)

	atomicState.thermostats = atomicState.thermostatData.thermostatList.inject([:]) { collector, stat ->
		// we use atomicState.thermostatData because it holds the latest Ecobee API response, from which we can determine which stats actually
        // had updated data. Thus the following work is done ONLY for tstats that have updated data
		String tid = stat.identifier
        // if (atomicState.askAlexaAppAlerts?.containsKey(tid) && (atomicState.askAlexaAppAlerts[tid] == true)) atomicState.askAlexaAppAlerts[tid] = []
        String DNI = (isHE?'ecobee_suite-thermostat-':'') + ([ app.id, stat.identifier ].join('.'))
        String tstatName = getThermostatName(tid)
        
		
		if (debugLevelThree) LOG("updateThermostatData() - Updating event data for thermostat ${tstatName} (${tid})", 3, null, 'info')

	// grab a local copy from the atomic storage all at once (avoid repetive reads from backing store)
    	def es = atomicState.equipmentStatus
		def equipStatus = (es && es[tid]) ? es[tid] : ''
        if (equipStatus == '') equipStatus = 'idle'
        def changeEquip = atomicState.changeEquip ? atomicState.changeEquip : [:]
        def lastEquipStatus = (changeEquip && changeEquip[tid]) ? changeEquip[tid] : 'unknown'
        
        def statSettings = atomicState.settings ? atomicState.settings[tid] : [:]
        def program = atomicState.program ? atomicState.program[tid] : [:]
        def events = atomicState.events ? atomicState.events[tid] : [:]
        def runtime = atomicState.runtime ? atomicState.runtime[tid] : [:]
        def statLocation = atomicState.statLocation ? atomicState.statLocation[tid] : [:]
        // def oemCfg = atomicState.oemCfg ? atomicState.oemCfg[tid] : [:]
        def extendedRuntime = atomicState.extendedRuntime ? atomicState.extendedRuntime[tid] : [:]
		// not worth it - weather is only accessed twice, and it now 'shortWeather', so we will access it directly from the atomicState cache
        // def weather = atomicState.weather ? atomicState.weather[tid] : [:]    
        
    // Handle things that only change when runtime object is updated)
        def occupancy = 'not supported'
        def tempTemperature
        def tempHeatingSetpoint = 0.0
        def tempCoolingSetpoint = 999.0
        def tempWeatherTemperature
        def tempWeatherDewpoint
        def tempWeatherHumidity
        def tempWeatherPressure
        
        if (forcePoll || runtimeUpdated) {
			// Occupancy (motion)
        	// TODO: Put a wrapper here based on the thermostat brand
        	def thermSensor = ''
            if (atomicState.remoteSensors) { thermSensor = atomicState.remoteSensors[tid].find { it.type == 'thermostat' } }
            
        	def hasInternalSensors
        	if(!thermSensor) {
				if (debugLevelFour) LOG('updateThermostatData() - Thermostat ${tstatName} does not have a built in remote sensor', 4, null, 'warn')
				hasInternalSensors = false
        	} else {
        		hasInternalSensors = true
				if (debugLevelFour) LOG("updateThermostatData() - thermSensor == ${thermSensor}", 4, null, 'trace' )
        
				def occupancyCap = thermSensor?.capability.find { it.type == 'occupancy' }
                if (occupancyCap) {
					if (debugLevelFour) LOG("updateThermostatData() - occupancyCap = ${occupancyCap} value = ${occupancyCap.value}", 4, null, 'info')
        
					// Check to see if there is even a value, not all types have a sensor
                    occupancy = occupancyCap.value ? occupancyCap.value : 'not supported'
                }
        	}
            // 'not supported' is reported as 'inactive'
			if (hasInternalSensors) { occupancy = (occupancy == 'true') ? 'active' : ((occupancy == 'unknown') ? 'unknown' : 'inactive') }            
            
			// Temperatures
            // NOTE: The thermostat always present Fahrenheit temps with 1 digit of decimal precision. We want to maintain that precision for inside temperature so that apps like vents and Smart Circulation
            // 		 can operate efficiently. So, while the user can specify a display precision of 0, 1 or 2 decimal digits, we ALWAYS keep and send max decimal digits and let the device handler adjust for display
            //		 For Fahrenheit, we keep the 1 decimal digit the API provides, for Celsius we allow for 2 decimal digits as a result of the mathematical calculation       
			tempTemperature = myConvertTemperatureIfNeeded( (runtime.actualTemperature.toBigDecimal() / 10.0), 'F', apiPrecision)
        	tempHeatingSetpoint = myConvertTemperatureIfNeeded( (runtime.desiredHeat / 10.0), 'F', apiPrecision)
        	tempCoolingSetpoint = myConvertTemperatureIfNeeded( (runtime.desiredCool / 10.0), 'F', apiPrecision)
        	//if (atomicState.weather && atomicState.weather?.containsKey(tid) && atomicState.weather[tid].containsKey('temperature')) {
            // log.debug atomicState.weather[tid]
            def weather = atomicState.weather
            // log.debug weather
            if (weather[tid]?.temperature?.isNumber()) {
            	tempWeatherTemperature = myConvertTemperatureIfNeeded( ((weather[tid].temperature.toBigDecimal() / 10.0)), "F", apiPrecision)
        	} else {tempWeatherTemperature = 451.0} // will happen only once, when weather object changes to shortWeather
            if (weather[tid]?.dewpoint != null) tempWeatherDewpoint = myConvertTemperatureIfNeeded( ((weather[tid].dewpoint / 10.0)), "F", apiPrecision)
            if (weather[tid]?.humidity != null) tempWeatherHumidity = weather[tid].humidity
            if (weather[tid]?.pressure != null) tempWeatherPressure = usingMetric ? weather[tid].pressure : roundIt((weather[tid].pressure * 0.02953),2) //milliBars to inHg
        }
     
     // EQUIPMENT SPECIFICS
		def auxHeatMode =  (statSettings?.hasHeatPump) && (statSettings?.hasForcedAir || statSettings?.hasElectric || statSettings?.hasBoiler) // 'auxHeat1' == 'emergency' if using a heatPump
        
	// handle[tid] things that only change when the thermostat object is updated
		def heatHigh
		def heatLow
		def coolHigh
		def coolLow
		def heatRange
		def coolRange
		def tempHeatDiff = 0.0
        def tempCoolDiff = 0.0
        def tempHeatCoolMinDelta = 1.0

		if (forcePoll || thermostatUpdated) {
            tempHeatDiff = usingMetric ? roundIt((statSettings.stage1HeatingDifferentialTemp / 1.8), 0) / 10.0 : statSettings.stage1HeatingDifferentialTemp / 10.0
            tempCoolDiff = usingMetric ? roundIt((statSettings.stage1CoolingDifferentialTemp / 1.8), 0) / 10.0 : statSettings.stage1CoolingDifferentialTemp / 10.0
            tempHeatCoolMinDelta = usingMetric ? roundIt((statSettings.heatCoolMinDelta / 1.8), 0) / 10.0 : statSettings.heatCoolMinDelta / 10.0
            
			// RANGES
			// UI works better with the same ranges for both heat and cool...
			// but the device handler isn't using these values for the UI right now (can't dynamically define the range)			
			heatHigh = myConvertTemperatureIfNeeded((statSettings.heatRangeHigh / 10.0), 'F', 1)
			heatLow =  myConvertTemperatureIfNeeded((statSettings.heatRangeLow  / 10.0), 'F', 1)
			coolHigh = myConvertTemperatureIfNeeded((statSettings.coolRangeHigh / 10.0), 'F', 1)
			coolLow =  myConvertTemperatureIfNeeded((statSettings.coolRangeLow  / 10.0), 'F', 1)
			
			// calculate these anyway (for now) - it's easier to read the range while debugging
			heatRange = (heatLow && heatHigh) ? "(${roundIt(heatLow,0)}..${roundIt(heatHigh,0)})" : (usingMetric ? '(5..35)' : '(45..95)')
			coolRange = (coolLow && coolHigh) ? "(${roundIt(coolLow,0)}..${roundIt(coolHigh,0)})" : (usingMetric ? '(5..35)' : '(45..95)')
		}
 
	// handle things that depend on both thermostat and runtime objects
		// EVENTS
		// Determine if an Event is running, find the first running event (only changes when thermostat object is updated)
    	def runningEvent = [:]
        String currentClimateName = ''
		String currentClimateId = ''
        String currentClimate = ''
        def currentFanMode = ''
        def currentVentMode = statSettings.vent
        def ventMinOnTime = statSettings.ventilatorMinOnTime
        def climatesList = []
        def statMode = statSettings.hvacMode
        def fanMinOnTime = statSettings.fanMinOnTime
		
		// what program is supposed to be running now?
        String scheduledClimateId = 'unknown'
		String scheduledClimateName = 'Unknown'
        def schedClimateRef = null
        if (program != [:]) {
        	scheduledClimateId = program.currentClimateRef 
        	schedClimateRef = program.climates.find { it.climateRef == scheduledClimateId }
            scheduledClimateName = schedClimateRef.name
            program.climates?.each { climatesList += '"'+it.name+'"' } // read with: programsList = new JsonSlurper().parseText(theThermostat.currentValue('programsList'))
        }
		if (debugLevelFour) LOG("${tstatName} -> scheduledClimateId: ${scheduledClimateId}, scheduledClimateName: ${scheduledClimateName}, climatesList: ${climatesList.toString()}", 4, null, 'info')
        
		// check which program is actually running now
        def vacationTemplate = false
		if (events?.size()) {
        	events.each {
            	if (it.running == true) {
                	runningEvent = it
                } else if ( it.type == 'template' ) {	// templates never run...
                	vacationTemplate = true
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
       	tempRunningEvent[tid] = (runningEvent != [:]) ? runningEvent : [:]
		if (tempRunningEvent[tid] != [:]) {
        	if (atomicState.runningEvent) tempRunningEvent = atomicState.runningEvent + tempRunningEvent
        	atomicState.runningEvent = tempRunningEvent
		}
		def thermostatHold = ''
        String holdEndsAt = ''
        
        def isConnected = runtime?.connected
        if (!isConnected) {
        	LOG("${tstatName} is not connected!",1,null,'warn')
        	// Ecobee Cloud lost connection with the thermostat - device, wifi, power or network outage
            currentClimateName = 'Offline'
            occupancy = 'unknown'
            
            // Oddly, the runtime.lastModified is returned in UTC, so we have to convert it to the time zone of the thermostat
            // (Note that each thermostat could actually be in a different time zone)
            def myTimeZone = statLocation?.timeZone ? TimeZone.getTimeZone(statLocation.timeZone) : (location.timeZone?: TimeZone.getTimeZone('UTC'))
            def disconnectedMS = new Date().parse('yyyy-MM-dd HH:mm:ss',runtime?.disconnectDateTime).getTime()
            String disconnectedAt = new Date().parse('yyyy-MM-dd HH:mm:ss',runtime?.disconnectDateTime).format('yyyy-MM-dd HH:mm:ss', myTimeZone)
            // In this case, holdEndsAt is actually the date & time of the last valid update from the thermostat...
            holdEndsAt = fixDateTimeString( disconnectedAt.take(10), disconnectedAt.drop(11), stat.thermostatTime )
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
        
        if (runningEvent && isConnected) {
        	if (debugLevelFour) LOG("Found a running Event: ${runningEvent}", 4, null, 'trace') 
            holdEndsAt = fixDateTimeString( runningEvent.endDate, runningEvent.endTime, stat.thermostatTime)
			thermostatHold = runningEvent.type
            String tempClimateRef = runningEvent.holdClimateRef ? runningEvent.holdClimateRef : ''
            switch (runningEvent.type) {
            	case 'hold':
            		if (tempClimateRef != '') {
						currentClimate = (program.climates.find { it.climateRef == tempClimateRef }).name
               			currentClimateName = 'Hold: ' + currentClimate
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
               		currentClimateName = 'Vacation'
                	fanMinOnTime = runningEvent.fanMinOnTime
                    break;
                case 'quickSave':
               		currentClimateName = 'Quick Save'
                    break;
                case 'autoAway':
             		currentClimateName = 'Auto Away'
                    currentClimate = 'Away'
                	break;
                case 'autoHome':
               		currentClimateName = 'Auto Home'
                    currentClimate = 'Home'
                    break;
            	default:                
               		currentClimateName = runningEvent.type
                    break;
            }
			currentClimateId = tempClimateRef
		} else if (isConnected) {
			if (scheduledClimateId) {
        		currentClimateId = scheduledClimateId
        		currentClimateName = scheduledClimateName
				currentClimate = scheduledClimateName
			} else {
        		LOG(preText+'Program or running Event missing', 1, null, 'warn')
            	currentClimateName = ''
        		currentClimateId = '' 
                currentClimate = ''
        	}
		}
        if (debugLevelFour) LOG("updateThermostatData(${tstatName} ${tid}) - currentClimate: ${currentClimate}, currentClimateName: ${currentClimateName}, currentClimateId: ${currentClimateId}", 4, null, 'trace')

        // Note that fanMode == 'auto' might be changedby the Thermostat DTH to 'off' or 'circulate' dependent on  HVACmode and fanMinRunTime
        if (runningEvent) {
        	currentFanMode = runningEvent.fan
            currentVentMode = runningEvent.vent
            ventMinOnTime = runningEvent.ventilatorMinOnTime
        } else {
        	currentFanMode = runtime.desiredFanMode
		}
		
		// HUMIDITY
        def humiditySetpoint = runtime.desiredHumidity
        def humidifierMode = statSettings?.humidifierMode
        def dehumiditySetpoint = runtime.desiredDehumidity
        def dehumidifierMode = statSettings?.dehumidifierMode
        
        def dehumidifyOvercoolOffset = statSettings?.dehumidifyOvercoolOffset
        def hasHumidifier = statSettings?.hasHumidifier
        def hasDehumidifier = statSettings?.hasDehumidifier || (statSettings?.dehumidifyWithAC && (statSettings?.dehumidifyOvercoolOffset != 0)) // fortunately, we can hide the details from the device handler
        if (hasHumidifier && (extendedRuntime && extendedRuntime.desiredHumidity && extendedRuntime.desiredHumidity[2])) {
        	humiditySetpoint = extendedRuntime.desiredHumidity[2]		// if supplied, extendedRuntime gives the actual target (Frost Control)
        }
        if (hasDehumidifier && (extendedRuntime && extendedRuntime.desiredDehumidity && extendedRuntime.desiredDehumidity[2])) {
        	dehumiditySetpoint = extendedRuntime.desiredDehumidity[2]	
		}
        def humiditySetpointDisplay = 0
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
		def heatStages = statSettings.heatStages
		def coolStages = statSettings.coolStages 
		String equipOpStat = 'idle'	// assume we're idle - this gets fixed below if different
        String thermOpStat = 'idle'
        Boolean isHeating = false
        Boolean isCooling = false
        Boolean smartRecovery = false
        Boolean overCool = false
        Boolean dehumidifying = false

        // Let's deduce if we are in Smart Recovery mode
        if (equipStatus.contains('ea')) {
        	isHeating = true
            if (!forcePoll && !runtimeUpdated) {
            	// these weren't calculated above, so we do them here
            	tempTemperature = myConvertTemperatureIfNeeded( (runtime.actualTemperature / 10.0), "F", apiPrecision)
        		tempHeatingSetpoint = myConvertTemperatureIfNeeded( (runtime.desiredHeat / 10.0), 'F', apiPrecision)
            }
            if ((statSettings?.disablePreHeating == false) && (tempTemperature > (tempHeatingSetpoint /* + 0.1 */))) {
            	smartRecovery = true
            	equipStatus = equipStatus + ',smartRecovery'
                LOG("${tstatName} is in Smart Recovery (${tid}), temp: ${tempTemperature}, setpoint: ${tempHeatingSetpoint}",3,null,'info')
            }
        } else if (equipStatus.contains('oo')) {
        	isCooling = true
            if (!forcePoll && !runtimeUpdated) {
            	// these weren't calculated above, so we do them here
            	tempTemperature = myConvertTemperatureIfNeeded( (runtime.actualTemperature / 10.0), "F", apiPrecision)
                tempCoolingSetpoint = myConvertTemperatureIfNeeded( (runtime.desiredCool / 10.0), 'F', apiPrecision)
            }
            // Check if humidity > humidity setPoint, and tempTemperature > (coolingSetpoint - 0.1)
            if (hasDehumidifier) {
            	if (runtime.actualHumidity > dehumiditySetpoint) {
                	if ((tempTemperature < tempCoolingSetpoint) && (tempTemperature >= (tempCoolingSetpoint - (statSettings?.dehumidifyOvercoolOffset?.toBigDecimal() / 10.0)))) {
                    	overCool = true
                        LOG("${tstatName} is Over Cooling (${tid}), temp: ${tempTemperature}, setpoint: ${tempCoolingSetpoint}",3,null,'info')
                    } else {
                    	dehumidifying = true
                    }
                }         	
            }
			if (!overCool && ((statSettings?.disablePreCooling == false) && (tempTemperature < (tempCoolingSetpoint /* - 0.1 */)))) {
            	smartRecovery = true
            	equipStatus = equipStatus + ',smartRecovery'
                LOG("${tstatName} is in Smart Recovery (${tid}), temp: ${tempTemperature}, setpoint: ${tempCoolingSetpoint}",3,null,'info')
            }
        }
        
        if (forcePoll || (equipStatus != lastEquipStatus)) {
			if (equipStatus == 'fan') {
				equipOpStat = 'fan only'
            	thermOpStat = equipOpStat
            } else if (equipStatus == 'off') {
            	thermOpStat = 'idle'
                equipOpStat = 'off'
			} else if (isHeating) {					// heating
            	thermOpStat = 'heating'
                equipOpStat = 'heating'
            	if (smartRecovery) thermOpStat = 'heating (smart recovery)'
				// Figure out which stage we are running now
                if 		(equipStatus.contains('t1')) 	{ equipOpStat = ((auxHeatMode) ? 'emergency' : ((heatStages > 1) ? 'heat 1' : 'heating')) }
				else if (equipStatus.contains('t2')) 	{ equipOpStat = 'heat 2' }
				else if (equipStatus.contains('t3')) 	{ equipOpStat = 'heat 3' }
				else if (equipStatus.contains('p2')) 	{ equipOpStat = 'heat pump 2' }
				else if (equipStatus.contains('p3')) 	{ equipOpStat = 'heat pump 3' }
				else if (equipStatus.contains('mp')) 	{ equipOpStat = 'heat pump' }
                // We now have icons that depict when we are humidifying or dehumidifying...
				if (equipStatus.contains('hu')) 		{ equipOpStat += ' hum' }	// humidifying if heat
                
			} else if (isCooling) {				// cooling
        		thermOpStat = 'cooling'
                equipOpStat = 'cooling'
                if (smartRecovery) thermOpStat = 'cooling (smart recovery)'
                else if (overCool) thermOpStat = 'cooling (overcool)'
				if 		(equipStatus.contains('l1')) { equipOpStat = (coolStages == 1) ? 'cooling' : 'cool 1' }
				else if (equipStatus.contains('l2')) { equipOpStat = 'cool 2' }
                
				if (equipStatus.contains('de') || overCool || dehumidifying) { equipOpStat += ' deh' }	// dehumidifying if cool
                
			} else if (equipStatus.contains('de')) { // These can also run independent of heat/cool
        		equipOpStat = 'dehumidifier' 
              
        	} else if (equipStatus.contains('hu')) { 
        		equipOpStat = 'humidifier' 
        	} // also: economizer, ventilator, compHotWater, auxHotWater
		}
        
		// Update the API link state and the lastPoll data. If we aren't running at a high debugLevel >= 4, then supply simple
		// poll status instead of the date/time (this simplifies the UI presentation, and reduces the chatter in the devices'
		// message log
        def apiConnection = apiConnected()
        def lastPoll = (debugLevelFour) ? "${apiConnection} @ ${atomicState.lastPollDate}" : (apiConnection=='full') ? 'Succeeded' : (apiConnection=='warn') ? 'Timed Out' : 'Failed'
        
        def statusMsg
        if (isConnected) {
			statusMsg = (holdEndsAt == '') ? '' : ((thermostatHold=='hold') ? 'Hold' : (thermostatHold=='vacation') ? 'Vacation' : 'Event')+" ends ${holdEndsAt}"
		} else {
        	statusMsg = "Thermostat Offline?\nLast updated ${holdEndsAt}"
            equipOpStat = 'offline'					// override if Ecobee Cloud has lost connection with the thermostat
            thermOpStat = 'offline'
        }

        // Okey dokey - time to queue all this data into the atomicState.thermostats queue
        def changeCloud =  atomicState.changeCloud  ? atomicState.changeCloud  : [:]
		def changeConfig = atomicState.changeConfig ? atomicState.changeConfig : [:]
        def changeNever =  atomicState.changeNever  ? atomicState.changeNever  : [:]
        def changeRarely = atomicState.changeRarely ? atomicState.changeRarely : [:]
        def changeOften =  atomicState.changeOften  ? atomicState.changeOften  : [:]
        def changeAlerts = atomicState.changeAlerts ? atomicState.changeAlerts : [:]
        def changeAttrs =  atomicState.changeAttrs  ? atomicState.changeAttrs  : [:]
		def changeDevice=  atomicState.changeDevice ? atomicState.changeDevice : [:]
        def changeTemps =  atomicState.changeTemps  ? atomicState.changeTemps  : [:]
        
        //changeEquip was initialized earlier
         
        def data = [forced: forcePoll,]				// Tell the DTH to force-update all attributes, states and tiles
 
        // Equipment operating status - need to send first so that temperatureDisplay is properly updated after API connection loss/recovery
        if (forcePoll || (lastEquipStatus != equipStatus) || (atomicState.wasConnected != isConnected)) { 
        	atomicState.wasConnected = isConnected
            data += [
        		equipmentStatus: 		  equipStatus,
            	thermostatOperatingState: thermOpStat,
            	equipmentOperatingState:  equipOpStat,
        	]
            changeEquip[tid] = equipStatus
            atomicState.changeEquip = changeEquip
        }       

        // API link to Ecobee's Cloud status - doesn't change unless things get broken
        Integer pollingInterval = getPollingInterval() // if this changes, we recalculate checkInterval for Health Check
        def cloudList = [lastPoll,apiConnection,pollingInterval,isConnected]
        if (forcePoll || (changeCloud == [:]) || !changeCloud.containsKey(tid) || (changeCloud[tid] != cloudList)) { 
        	def checkInterval = (pollingInterval <= 5) ? (16*60) : (((pollingInterval+1)*2)*60) 
            if (checkInterval > 3600) checkInterval = 3900 	// 5 minutes longer than an hour
            data += [
        		lastPoll: lastPoll,
            	apiConnected: apiConnection,		// link from ST to Ecobee
                ecobeeConnected: isConnected,		// link from Ecobee to Thermostat
                checkInterval: checkInterval,
        	]
            changeCloud[tid] = cloudList
            atomicState.changeCloud = changeCloud
        }
        
        // Alerts & Read Only
        if (alertsUpdated || forcePoll) {
            if (!changeAlerts?.containsKey(tid)) changeAlerts[tid] = [:]
            def alertValues = []
            int i = 0
            alertNamesList.each { alert ->
            	def alertVal = statSettings."${alert}"
                alertValues <<  alertVal 
                if (forcePoll || (changeAlerts[tid]?.getAt(i) != alertVal)) {
                    data += [ "${alert}": alertVal, ]
                }
                i++
            }
            changeAlerts[tid] = alertValues
            atomicState.changeAlerts = changeAlerts
        }
        
        // Configuration Settings
        if (thermostatUpdated || forcePoll) {
            if (!changeAttrs?.containsKey(tid)) changeAttrs[tid] = [:]
            def attrValues = []
            int i = 0
            settingsNamesList.each { attr ->
            	def attrVal = statSettings."${attr}"
				if (attrVal == '') attrVal = 'null'
                attrValues <<  attrVal 
                if (forcePoll || (changeAttrs[tid]?.getAt(i) != attrVal)) {
                    data += [ "${attr}": attrVal, ]
                }
                i++
            }
            changeAttrs[tid] = attrValues
            atomicState.changeAttrs = changeAttrs
        }
		
		// Thermostat Device Data
		if (thermostatUpdated || forcePoll) {
			if (!changeDevice?.containsKey(tid)) changeDevice[tid] = [:]
            def deviceValues = []
            int i = 0
            EcobeeDeviceInfo.each { attr ->
            	def deviceVal = stat."${attr}"
				if (deviceVal == '') deviceVal = 'null'
                deviceValues <<  deviceVal 
                if (forcePoll || (changeDevice[tid]?.getAt(i) != deviceVal)) {
                    data += [ "${attr}": deviceVal, ]
                }
                i++
            }
            changeDevice[tid] = deviceValues
            atomicState.changeDevice = changeDevice
		}
        
        // Temperatures - need to convert from internal F*10 to standard Thermostat units
        if (thermostatUpdated || forcePoll) {
    		if (!changeTemps?.containsKey(tid)) changeTemps[tid] = [:]
            def tempValues = []
            int i = 0
            tempSettingsList.each { temp ->
            	def tempVal = String.format("%.${apiPrecision}f", roundIt((usingMetric ? roundIt((statSettings."${temp}" / 1.8), 0) / 10.0 : statSettings."${temp}" / 10.0),apiPrecision))
                tempValues <<  tempVal 
                if (forcePoll || (changeTemps[tid]?.getAt(i) != tempVal)) {
                    data += [ "${temp}": tempVal, ]
                }
                i++
            }
            changeTemps[tid] = tempValues
            atomicState.changeTemps = changeTemps
    	}
        
        // SmartApp configuration settings that almost never change (Listed in order of frequency that they should change normally)
        Integer dbgLevel = getDebugLevel()
        String tmpScale = getTemperatureScale()
        def timeOfDay = atomicState.timeZone ? getTimeOfDay() : getTimeOfDay(tid)
        // log.debug "timeOfDay: ${timeOfDay}"
		def configList = [timeOfDay,userPrecision,dbgLevel,tmpScale,settings.mobile] 
        if (forcePoll || (changeConfig == [:]) || !changeConfig.containsKey(tid) || (changeConfig[tid] != configList)) { 
            data += [
        		timeOfDay: timeOfDay,
           		decimalPrecision: userPrecision,
				temperatureScale: tmpScale,			
				debugLevel: dbgLevel,
				mobile: settings.mobile,
        	]
            changeConfig[tid] = configList
            atomicState.changeConfig = changeConfig
        }
        
		// Thermostat configuration settinngs
        if ((isConnected && (forcePoll || thermostatUpdated)) || !isConnected) {	// new settings, programs or events
        	def autoMode = statSettings?.autoHeatCoolFeatureEnabled
            def statHoldAction = statSettings?.holdAction			// thermsotat's preference setting for holdAction:
            														// useEndTime4hour, useEndTime2hour, nextPeriod, indefinite, askMe
        	
            // Thermostat configuration stuff that almost never changes - if any one changes, send them all
        	def neverList = [statMode,autoMode,statHoldAction,coolStages,heatStages,heatRange,coolRange,climatesList,
        						auxHeatMode,hasHumidifier,hasDehumidifier,tempHeatDiff,tempCoolDiff,tempHeatCoolMinDelta] 
 			if (forcePoll || (changeNever == [:]) || !changeNever.containsKey(tid) || (changeNever[tid] != neverList)) {  
            	data += [
					coolMode: (coolStages > 0),
            		coolStages: coolStages,
					heatMode: (heatStages > 0),
            		heatStages: heatStages,
					autoMode: autoMode,
                	thermostatMode: statMode,
                    statHoldAction: statHoldAction,
            		heatRangeHigh: heatHigh,
            		heatRangeLow: heatLow,
            		coolRangeHigh: coolHigh,
            		coolRangeLow: coolLow,
					heatRange: heatRange,
					coolRange: coolRange,
                    hasHumidifier: hasHumidifier, 
                    hasDehumidifier: hasDehumidifier,
                	programsList: climatesList,                
                	heatDifferential: String.format("%.${apiPrecision}f", roundIt(tempHeatDiff, apiPrecision)),
                	coolDifferential: String.format("%.${apiPrecision}f", roundIt(tempCoolDiff, apiPrecision)),
                    heatCoolMinDelta: String.format("%.${apiPrecision}f", roundIt(tempHeatCoolMinDelta, apiPrecision)),
            	]
            	changeNever[tid] = neverList
            	atomicState.changeNever = changeNever
        	}
            
            // Thermostat operational things that rarely change, (a few times a day at most)
            //
            // First, we need to clean up Fan Holds
            if ((thermostatHold != '') && (currentClimateName == 'Hold: Fan')) {
            	if (currentFanMode == 'on') { currentClimateName = 'Hold: Fan On' }
                else if (currentFanMode == 'auto') {
                	if (statMode == 'off') {
                    	if (fanMinOnTime != 0) {currentClimateName = "Hold: Circulate"}
                    } else {
                    	currentClimateName = (fanMinOnTime == 0) ? 'Hold: Auto' : 'Hold: Circulate'
                    }
                }
            }
         	def rarelyList = [fanMinOnTime,thermostatHold,holdEndsAt,statusMsg,humiditySetpoint,humidifierMode,dehumiditySetpoint,
            					currentClimate,currentClimateName,currentClimateId,scheduledClimateName,scheduledClimateId,currentFanMode,currentVentMode,auxHeatMode]
		    if (forcePoll || (changeRarely == [:]) || !changeRarely.containsKey(tid) || (changeRarely[tid] != rarelyList)) { 
            	data += [
          			thermostatHold: thermostatHold,
                	holdEndsAt: holdEndsAt,
               		holdStatus: statusMsg,
                ]
                // currentProgramName: currentClimateName,
                // don't send null values
                if (currentClimateName != '') 	data += [ currentProgramName: currentClimateName, ]
                if (currentClimateId != '') 	data += [ currentProgramId: currentClimateId, ]
				if (currentClimate != '') 		data += [ currentProgram: currentClimate, ]
                
                data += [
					scheduledProgramName: scheduledClimateName,
					scheduledProgramId: scheduledClimateId,
					scheduledProgram: scheduledClimateName,
                    thermostatFanMode: currentFanMode,
                    vent: currentVentMode,
                    ventilatorMinOnTime: ventMinOnTime,
                	fanMinOnTime: fanMinOnTime,                                        
					auxHeatMode: auxHeatMode,				// Moved these down here, since they really didn't belong on the neverList
                    humiditySetpoint: humiditySetpoint,		// ditto
                    humidifierMode: humidifierMode,			// ditto
                    dehumidifierMode: dehumidifierMode,		// ditto
                    dehumiditySetpoint: dehumiditySetpoint,	// ditto
                    dehumidifierLevel: dehumiditySetpoint,	// department of redundancy dept.
          		]
            	changeRarely[tid] = rarelyList
            	atomicState.changeRarely = changeRarely
                
                // Save the Program when it changes so that we can get to it easily for the child sensors
        		def tempProgram = [:]
                tempProgram[tid] = currentClimateName
        		if (atomicState.currentProgramName) tempProgram = atomicState.currentProgramName + tempProgram
        		atomicState.currentProgramName = tempProgram
        	}     
    	}
        
        // MOVED TO THE BOTTOM because these values are dependent upon changes to states above when they get to the thermostat DTH
        // Runtime stuff that changes most frequently - we test them 1 at a time, and send only the ones that change
        // Send these first, as they generally are the reason anything else changes (so the thermostat's notification log makes sense)
		if (forcePoll || runtimeUpdated || (tempHeatingSetpoint != 0.0) || (tempCoolingSetpoint != 999.0)) {
        	String wSymbol = atomicState.weather[tid]?.weatherSymbol?.toString()
            def oftenList = [tempTemperature,occupancy,runtime.actualHumidity,tempHeatingSetpoint,tempCoolingSetpoint,wSymbol,tempWeatherTemperature,tempWeatherHumidity,tempWeatherDewpoint,
            					tempWeatherPressure,humiditySetpointDisplay,userPrecision]
            def lastOList = []
            lastOList = changeOften[tid]
            if (forcePoll || !lastOList || (lastOList.size() < 12)) lastOList = [999,'x',-1,-1,-1,-999,-999,-1,-1,-1,-1,-1]
            if (lastOList[11] != userPrecision) data+= [decimalPrecision: userPrecision,]															// send first so that DTH uses the latest setting
            if (lastOList[0] != tempTemperature) data += [temperature: String.format("%.${apiPrecision}f", roundIt(tempTemperature, apiPrecision)),]
            if (lastOList[1] != occupancy) data += [motion: occupancy,]
            if (lastOList[2] != runtime.actualHumidity) data += [humidity: runtime.actualHumidity,]
            if (lastOList[10] != humiditySetpointDisplay) data += [humiditySetpointDisplay: humiditySetpointDisplay,]
            // send these next two also when the userPrecision changes
            def needPrograms = false
            if ((tempHeatingSetpoint != 0.0) && ((lastOList[3] != tempHeatingSetpoint) || (lastOList[11] != userPrecision))) { needPrograms = true; data += [heatingSetpoint: String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),] }
            if ((tempCoolingSetpoint != 999.0) && ((lastOList[4] != tempCoolingSetpoint) || (lastOList[11] != userPrecision))) { needPrograms = true; data += [coolingSetpoint: String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),] }
            if (wSymbol && (lastOList[5] != wSymbol)) data += [weatherSymbol: wSymbol]
            if (tempWeatherTemperature && ((lastOList[6] != tempWeatherTemperature) || (lastOList[11] != userPrecision))) data += [weatherTemperature: String.format("%0${userPrecision+2}.${userPrecision}f", roundIt(tempWeatherTemperature, userPrecision)),]
           	if (tempWeatherHumidity && (lastOList[7] != tempWeatherHumidity)) data += [weatherHumidity: tempWeatherHumidity,]
            if (tempWeatherDewpoint && ((lastOList[8] != tempWeatherDewpoint) || (lastOList[11] != userPrecision))) data += [weatherDewpoint: String.format("%0${userPrecision+2}.${userPrecision}f",roundIt(tempWeatherDewpoint,userPrecision)),]
            if (tempWeatherPressure && (lastOList[9] != tempWeatherPressure)) data += [weatherPressure: tempWeatherPressure,]
            changeOften[tid] = oftenList
            atomicState.changeOften = changeOften
            // If the setpoints change, then double-check to see if the currently running Program has changed.
            // We do this by ensuring that the rest of the thermostat datasets (settings, program, events) are 
            // collected on the next poll, if they weren't collected in this poll.
            atomicState.needPrograms = (needPrograms && (!thermostatUpdated && !forcePoll))
		}
        
        if (debugLevelFour) LOG("updateThermostatData() - Event data updated for thermostat ${tstatName} (${tid}) = ${data}", 4, null, 'trace')

		// it is possible that thermostatSummary indicated things have changed that we don't care about...
		if (data != [:]) {
        	data += [ thermostatTime:stat.thermostatTime, ]
            if (forcePoll) data += [forced: false,]			// end of forced update
        	tstatNames += [tstatName]
			collector[DNI] = [thermostatId:tid, data:data]
        }
		return collector
	}
    Integer nSize = tstatNames.size()
	if (nSize > 1) tstatNames.sort()
    LOG("${preText}Updates for ${nSize} thermostat${nSize!=1?'s':''} ${nSize!=0?'('+tstatNames.toString()[1..-2]+')':''}", 2, null, 'info')
}

def getChildThermostatDeviceIdsString(singleStat = null) {
	def debugLevelFour = debugLevel(4)
	if(!singleStat) {
    	if (debugLevelFour) LOG('getChildThermostatDeviceIdsString() - !singleStat returning the list for all thermostats', 4, null, 'info')
		return settings.thermostats.collect { it.split(/\./).last() }.join(',')
	} else {
    	// Only return the single thermostat
        if (debugLevelFour) LOG('Only have a single stat.', 4, null, 'debug')
        def ecobeeDevId = singleStat.device.deviceNetworkId.split(/\./).last()
        if (debugLevelFour) LOG("Received a single thermostat, returning the Ecobee Device ID as a String: ${ecobeeDevId}", 4, null, 'info')
        return ecobeeDevId as String  	
    }
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

private refreshAuthToken(child=null) {
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
		LOG('Performing a refreshAuthToken()', 4, null, 'trace') // 4
        
        def refreshParams = [
//                method: 'POST',
                uri   : apiEndpoint,
                path  : '/token',
                query : [grant_type: 'refresh_token', refresh_token: "${atomicState.refreshToken}", client_id: ecobeeApiKey],
        ]

        LOG("refreshParams = ${refreshParams}", 4, null, 'trace') // 4

		def jsonMap
        try {            
            httpPost(refreshParams) { resp ->
				LOG("Inside httpPost resp handling.", 4, child, "trace")
                if(resp.status == 200) {
                    LOG('refreshAuthToken() - 200 Response received - Extracting info.', 4, child, 'trace' ) // 4
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
                        LOG("jsonMap: ${jsonMap}", 4, child) // 4
						
                        atomicState.refreshToken = jsonMap.refresh_token
                        
                        // TODO - Platform BUG: This was not updating the state values for some reason if we use resp.data directly??? 
                        // 		  Workaround using jsonMap for authToken                       
                        LOG("atomicState.authToken before: ${atomicState.authToken}", 4, child, "trace") // 4
                        def oldAuthToken = atomicState.authToken
                        atomicState.authToken = jsonMap?.access_token  
						LOG("atomicState.authToken after: ${atomicState.authToken}", 4, child, "trace") // 4
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
                            LOG("Token refreshed. Rescheduling aborted action: ${action}", 4, child, 'trace')  // 4
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
        	def result = false
        	//LOG("refreshAuthToken() - HttpResponseException occurred. Exception info: ${e} StatusCode: ${e.statusCode}  response? data: ${e.getResponse()?.getData()}", 1, null, "error")
            LOG("refreshAuthToken() - HttpResponseException occurred. Exception info: ${e} StatusCode: ${e.statusCode}", 1, child, "error")
            if (e.statusCode != 401) {
            	runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true])
            } else if (e.statusCode == 401) {            
				atomicState.reAttempt = atomicState.reAttempt + 1
		        if (atomicState.reAttempt > 3) {                       	
    		    	apiLost("Too many retries (${atomicState.reAttempt - 1}) for token refresh.")        	    
            	    result = false
		        } else {
    		    	LOG("Setting up runIn for refreshAuthToken", 4, child, 'trace') // 4
        			if ( isHE || canSchedule() ) {            			
                        runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true]) 
					} else { 
    	        		LOG("Unable to schedule refreshAuthToken, running directly", 4, child, 'trace')			 // 4			
	        	    	result = refreshAuthToken(child) 
    	        	}
        		}
            }
            generateEventLocalParams() // Update the connected state at the thermostat devices
            return result
		} catch (java.util.concurrent.TimeoutException e) {
			LOG("refreshAuthToken(), TimeoutException: ${e}.", 1, child, "warn")
			// Likely bad luck and network overload, move on and let it try again
            if ( isHE || canSchedule() ) { runIn(atomicState.reAttemptInterval, "refreshAuthToken", [overwrite: true]) } else { refreshAuthToken(child) }            
            return false
        } catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException | javax.net.ssl.SSLPeerUnverifiedException | javax.net.ssl.SSLHandshakeException | java.net.SocketTimeoutException e) {
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
        	if (inTimeoutRetry < 20) runIn(atomicState.reAttemptInterval.toInteger(), "refreshAuthToken", [overwrite: true])
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

def queueFailedCall(String routine, String DNI, numArgs, arg1=null, arg2=null, arg3=null, arg4=null, arg5=null, arg6=null, arg7=null) {
	//log.debug "queueCall routine: ${routine}, DNI: ${DNI}, ${arg1}, ${arg2}, ${arg3}, ${arg4}, ${arg5}, ${arg6}, ${arg7}"  // ${routine}" // , args: ${theArgs}"
	if ((atomicState.connected == 'full') && atomicState.runningCallQueue) return // don't queue when we are clearing the queue
    runIn(2, 'queueCall', [overwrite: false, data: [routine: routine, DNI: DNI, args: [arg1, arg2, arg3, arg4, arg5], done: false, numArgs: numArgs]])
    if (atomicState.callsQueued == null) { atomicState.callsQueued = 0; atomicState.callsRun = 0; }
    atomicState.callsQueued = atomicState.callsQueued + 1
}

def queueCall(data) {
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

def runCallQueue() {
	def dbgLvl = 1
	LOG("runCallQueue() connected: ${atomicState.connected}, callQueue: ${atomicState.callQueue}, runningCallQueue: ${atomicState.runningCallQueue}", dbgLvl, null, 'trace')
	if (atomicState.connected?.toString() != 'full') return
    if ((atomicState.callQueue == null) || (atomicState.callQueue?.size() == 0)) {atomicState.runningCallQueue = false; return;}
    if (atomicState.runningCallQueue) return
    atomicState.runningCallQueue = true
    
    //while (atomicState.connected == 'full') {
    def failed = false
    def result = true
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

def resumeProgram(child, String deviceId, resumeAll=true) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to resumeProgram(${child}, ${deviceId}, ${resumeAll})", 2, child, 'warn')
        queueFailedCall('resumeProgram', child.device.deviceNetworkId, 2, deviceId, resumeAll)
        return false
    }

	def result = true
    boolean debugLevelFour = debugLevel(4)
    boolean debugLevelThree = debugLevel(3)
    if (debugLevelThree) LOG("Entered resumeProgram for deviceId: ${deviceId} with child: ${child.device?.displayName}", 3, child, 'trace')
    
	String allStr = resumeAll ? 'true' : 'false'
    def jsonRequestBody = '{"functions":[{"type":"resumeProgram","params":{"resumeAll":"' + allStr + '"}}],"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"}}'
	if (debugLevelFour) LOG("jsonRequestBody = ${jsonRequestBody}", 4, child)
    
	result = sendJson(child, jsonRequestBody)
    if (debugLevelThree) LOG("resumeProgram(${resumeAll}) returned ${result}", 3, child,result?'info':'warn')
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
    		def updates = [	'heatingSetpoint':String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
        					'coolingSetpoint':String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
                            'currentProgramName': climateName,
                            'currentProgram': climateName,
                       		'currentProgramId':climateId ]
        	LOG("resumeProgram() ${updates}",2,null,'info')
        	child.generateEvent(updates)			// force-update the calling device attributes that it can't see
        }
        // atomicState.forcePoll = true 		// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    } else {
    	if (atomicState.connected?.toString() != 'full') {
			LOG("API connection lost, queueing failed call to resumeProgram(${child}, ${deviceId}, ${resumeAll})", 2, child, 'warn')
        	queueFailedCall('resumeProgram', child.device.deviceNetworkId, 2, deviceId, resumeAll)
    	}
    }
    return result
}

def setHVACMode(child, deviceId, mode) {
	LOG("setHVACMode(${mode})", 4, child)
    def result = setMode(child, mode, deviceId)
    LOG("setHVACMode(${mode}) returned ${result}", 3, child,result?'info':'warn')    
	// if (result) atomicState.forcePoll = true 		// force next poll to get updated runtime data
    // if (!result) queue failed request
    return result
}

def setMode(child, mode, deviceId) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
        queueFailedCall('setMode', child.device.deviceNetworkId, 2, mode, deviceId)
        return false
    }
    
	LOG("setMode(${mode}) for ${deviceId}", 5, child, 'trace')
    // queueFailedCall('setMode', child, mode, deviceId)
        
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"hvacMode":"'+"${mode}"+'"}}}'  
	LOG("Mode Request Body = ${jsonRequestBody}", 4, child, 'trace')
    
	def result = sendJson(child, jsonRequestBody)
    LOG("setMode to ${mode} with result ${result}", 4, child, 'trace')
	if (result) {
    	LOG("setMode(${mode}) - Succeeded", 1, child, 'info')
    	// atomicState.forcePoll = true 		// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    } else {
        if (atomicState.connected?.toString() != 'full') {
            LOG("API is connection lost, queueing failed call to setMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
            queueFailedCall('setMode', child.device.deviceNetworkId, 2, mode, deviceId)
        } else {
        	LOG("setMode(${mode}) - Failed", 1, child, 'warn')
        }
    }
	return result
}

def setHumidifierMode(child, mode, deviceId) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHumidifierMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
        queueFailedCall('setHumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
        return false
    }
    
	LOG ("setHumidifierMode(${mode}) for ${deviceId}", 5, child, 'trace')
    
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"humidifierMode":"'+"${mode}"+'"}}}'  
    def result = sendJson(child, jsonRequestBody)
    LOG("setHumidifierMode to ${mode} with result ${result}", 4, child, 'trace')
	if (result) {
    	LOG("setHumidifierMode(${mode}) - Succeeded", 2, child, 'info')
        // atomicState.forcePoll = true 		// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    } else {
        if (atomicState.connected?.toString() != 'full') {
            LOG("API connection lost, queueing failed call to setHumidifierMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
            queueFailedCall('setHumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
            return false
        } else {
        	LOG("setHumidifierMode(${mode}) - Failed", 1, child, 'warn')
        }
    }
	return result
}

def setHumiditySetpoint(child, value, deviceId) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHumiditySetpoint(${child}, ${value}, ${deviceId}", 2, child, 'warn')
        queueFailedCall('setHumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
        return false
    }
    
	LOG ("setHumiditySetpoint${value}) for ${deviceId}", 5, child, 'trace')
                        
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"humidity":"'+"${value}"+'"}}}'  
	LOG("setHumiditySetpoint Request Body = ${jsonRequestBody}", 4, child, 'trace')
    def result = sendJson(child, jsonRequestBody)
    LOG("setHumiditySetpoint to ${value} with result ${result}", 4, child, 'trace')
	if (result) {
    	LOG("setHumiditySetpoint(${value}) - Succeeded", 2, child, 'info')
        // atomicState.forcePoll = true 		// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    } else {
        if (atomicState.connected?.toString() != 'full') {
            LOG("API connection lost, queueing failed call to setHumiditySetpoint(${child}, ${value}, ${deviceId}", 2, child, 'warn')
            queueFailedCall('setHumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
        } else {
        	LOG("setHumiditySetpoint(${value}) - Failed", 1, child, 'warn')
        }
    }
	return result	
}

def setDehumidifierMode(child, mode, deviceId) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setDehumidifierMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
        queueFailedCall('setDehumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
        return false
    }
    
	LOG ("setDehumidifierMode(${mode}) for ${deviceId}", 5, child, 'trace')
                        
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"dehumidifierMode":"'+"${mode}"+'"}}}'  
	LOG("dehumidifierMode Request Body = ${jsonRequestBody}", 4, child, 'trace')
    def result = sendJson(child, jsonRequestBody)
    LOG("setDehumidifierMode to ${mode} with result ${result}", 4, child, 'trace')
	if (result) {
    	LOG("setDehumidifierMode(${mode}) - Succeeded", 2, child, 'info')
        // atomicState.forcePoll = true 		// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    } else {
        if (atomicState.connected?.toString() != 'full') {
            LOG("API connection lost, queueing failed call to setDehumidifierMode(${child}, ${mode}, ${deviceId}", 2, child, 'warn')
            queueFailedCall('setDehumidifierMode', child.device.deviceNetworkId, 2, mode, deviceId)
        } else {
        	LOG("setDehumidifierMode(${mode}) - Failed", 1, child, 'warn')
        }
    }
	return result
}

def setDehumiditySetpoint(child, value, deviceId) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setDehumiditySetpoint(${child}, ${value}, ${deviceId}", 2, child, 'warn')
        queueFailedCall('setDehumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
        return false
    }
    
	LOG ("setDehumiditySetpoint${value}) for ${deviceId}", 5, child, 'trace')
                        
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"dehumidifierLevel":"'+"${value}"+'"}}}'  
	LOG("setDehumiditySetpoint Request Body = ${jsonRequestBody}", 4, child, 'trace')
    def result = sendJson(child, jsonRequestBody)
    LOG("setDehumiditySetpoint to ${value} with result ${result}", 4, child, 'trace')
	if (result) {
    	LOG("setDehumiditySetpoint (${value}) - Succeeded", 2, child, 'info')
        // atomicState.forcePoll = true 		// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    } else {
        if (atomicState.connected?.toString() != 'full') {
            LOG("API connection lost, queueing failed call to setDehumiditySetpoint(${child}, ${value}, ${deviceId}", 2, child, 'warn')
            queueFailedCall('setDehumiditySetpoint', child.device.deviceNetworkId, 2, value, deviceId)
        } else {
    		LOG("setDehumiditySetpoint (${mode}) - Failed", 1, child, 'warn')
        }
    }
	return result	
}

def setFanMinOnTime(child, deviceId, howLong) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setFanMinOnTime(${child}, ${deviceId}, ${howLong}", 2, child, 'warn')
        queueFailedCall('setFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
        return false
    }
    
	String statName = getThermostatName(deviceId)
	LOG("setFanMinOnTime(${howLong}) for thermostat ${statName}", 4, child, 'trace')
    
    if ((howLong < 0) || (howLong > 55)) {
    	LOG("setFanMinOnTime() for thermostat ${statName} - Invalid Argument ${howLong}",2,child,'warn')
        return false
    }
    
    def thermostatSettings = ',"thermostat":{"settings":{"fanMinOnTime":'+howLong+'}}'
    def thermostatFunctions = ''
    def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
    def result = sendJson(child, jsonRequestBody)
    LOG("setFanMinOnTime(${howLong}) for thermostat ${statName} returned ${result}", result?2:4, child,result?'info':'warn')    
	if (result) {
		// atomicState.forcePoll = true 		// force next poll to get updated runtime data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
	} else {
    	if (atomicState.connected?.toString() != 'full') {
            LOG("API connection lost, queueing failed call to setFanMinOnTime(${child}, ${deviceId}, ${howLong}", 2, child, 'warn')
            queueFailedCall('setFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
        }
    }
    return result
}

def setVacationFanMinOnTime(child, deviceId, howLong) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setVacationFanMinOnTime(${child}, ${deviceId}, ${howLong}", 2, child, 'warn')
        queueFailedCall('setVacationFanMinOnTime', child.device.deviceNetworkId, 2, deviceId, howLong)
        return false
    }
    
	String statName = getThermostatName(deviceId)
	LOG("setVacationFanMinOnTime(${howLong}) for thermostat ${statName}", 4, child)   
    if (!howLong.isNumber() || (howLong.toInteger() < 0) || howLong.toInteger() > 55) {		// Documentation says 60 is the max, but the Ecobee3 thermostat maxes out at 55 (makes 60 = 0)
    	LOG("setVacationFanMinOnTime() for thermostat ${statName} - Invalid Argument ${howLong}",2,child,'warn')
        return false
    }
    
    def evt = atomicState.runningEvent[deviceId]
    def hasEvent = true
    if (!evt) {
    	hasEvent = false						// no running event defined
    } else {
    	if (evt.running != true) hasEvent = false		// shouldn't have saved it if it wasn't running
    	if (hasEvent && (evt.type != "vacation")) hasEvent = false	// we only override vacation fanMinOnTime setting
    }
  	if (!hasEvent) {
    	LOG("setVacationFanMinOnTime() for thermostat ${statName} - Vacation not active", 2, child, 'warn')
        return false
    }
    if (evt.fanMinOnTime.toInteger() == howLong.toInteger()) return true	// didn't need to do anything!
    
    if (deleteVacation(child, deviceId, evt.name)) { // apparently on order to change something in a vacation, we have to delete it and then re-create it..
  
        def thermostatSettings = ''
        def thermostatFunctions = '{"type":"createVacation","params":{"name":"' + evt.name + '","coolHoldTemp":"' + evt.coolHoldTemp + '","heatHoldTemp":"' + evt.heatHoldTemp + 
                                    '","startDate":"' + evt.startDate + '","startTime":"' + evt.startTime + '","endDate":"' + evt.endDate + '","endTime":"' + evt.endTime + 
                                    '","fan":"' + evt.fan + '","fanMinOnTime":"' + "${howLong}" + '"}}'
        def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'

        LOG("setVacationFanMinOnTime() for thermostat ${statName} - before sendJson() jsonRequestBody: ${jsonRequestBody}", 4, child, "info")

        def result = sendJson(child, jsonRequestBody)
        LOG("setVacationFanMinOnTime(${howLong}) for thermostat ${statName} returned ${result}", 4, child, result?'info':'warn') 
		if (result) {
			// atomicState.forcePoll = true 		// force next poll to get updated data
			runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
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

private def createVacationTemplate(child, deviceId) {
	String vacationName = 'tempVac810n'
    String statName = getThermostatName(deviceId)
    
    // delete any old temporary vacation that we created
    deleteVacation(child, deviceId, vacationName)
    
    // Create the temporary vacation
    def thermostatSettings = ''
    def thermostatFunctions = 	'{"type":"createVacation","params":{"name":"' + vacationName + 
    							'","coolHoldTemp":"850","heatHoldTemp":"550","startDate":"2034-01-01","startTime":"08:30:00","endDate":"2034-01-01","endTime":"09:30:00","fan":"auto","fanMinOnTime":"5"}}'
    def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
    LOG("before sendJson() jsonRequestBody: ${jsonRequestBody}", 4, child, 'trace')
    def result = sendJson(child, jsonRequestBody)
    LOG("createVacationTemplate(${vacationName}) for thermostat ${statName} returned ${result}", 4, child, result?'info':'warn')
    // if (!result) queue failed request
    
    // Now, delete the temporary vacation
    result = deleteVacation(child, deviceId, vacationName)
    LOG("deleteVacation(${vacationName}) for thermostat ${statName} returned ${result}", 4, child, 'trace')
    return true
}

def deleteVacation(child, deviceId, vacationName=null ) {
	def vacaName = vacationName
    String statName = getThermostatName(deviceId)
	if (!vacaName) {		// no vacationName specified, let's find out if one is currently running
        def evt = atomicState.runningEvent[deviceId]
    	if (!evt ||  (evt.running != true) || (evt.type != "vacation") || !evt.name) {
    		LOG("deleteVacation() for thermostat ${statName} - Vacation not active", 4, child, 'warn')
        	return true	// Asked us to delete the current vacation, and there isn't one - I'd still call that a success!
        }
        vacaName = evt.name as String		// default names are Very Big Numbers
    }

    def thermostatSettings = ''
    def thermostatFunctions = '{"type":"deleteVacation","params":{"name":"' + vacaName + '"}}'
    def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":['+thermostatFunctions+']'+thermostatSettings+'}'
	
    def result = sendJson(child, jsonRequestBody)
    LOG("deleteVacation() for thermostat ${statName} returned ${result}", 4, child,result?'info':'warn')
    
    if (vacationName == null) {
    	resumeProgram(child, deviceId, true)		// force back to previously scheduled program
        pollChildren(deviceId,true) 				// and finally, update the state of everything (hopefully)
    }
	// if (result) atomicState.forcePoll = true 		// force next poll to get updated data
	runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    return result
}

// Should only be called by child devices, and they MUST provide sendHoldType and sendHoldHours as of version 1.2.0
def setHold(child, heating, cooling, deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setHold(${child}, ${heating}, ${cooling}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
        queueFailedCall('setHold', child.device.deviceNetworkId, 5, heating, cooling, deviceId, sendHoldType, sendHoldHours)
        return false
    }
    
    def currentThermostatHold = child.device.currentValue('thermostatHold')
    String statName = getThermostatName(deviceId)
    if (currentThermostatHold == 'vacation') {
    	LOG("setHold(): Can't set a new hold for thermostat ${statName} while in a vacation hold",2,null,'warn')
    	// can't change fan mode while in vacation hold, so silently fail
        return false
    }  else if (currentThermostatHold != '') {
    	// must resume program first
        resumeProgram(child, deviceId, true)
    }
	def isMetric = (getTemperatureScale() == "C")
	def h = roundIt((isMetric ? (cToF(heating) * 10.0) : (heating * 10.0)), 0)		// better precision using BigDecimal round-half-up
	def c = roundIt((isMetric ? (cToF(cooling) * 10.0) : (cooling * 10.0)), 0)
    
	LOG("setHold() for thermostat ${statName} - h: ${heating}(${h}), c: ${cooling}(${c}), ${sendHoldType}, ${sendHoldHours}", 2, child, 'trace')
    
    def theHoldType = sendHoldType // ? sendHoldType : whatHoldType()
	if (theHoldType == 'nextTransition') {
    	// Check if setpoints are the same as currentClimateRef, if so, don't set a new hold
        // ResumeProgram above already sent the setpoint display values for the currentClimate to the DTH
        def currHeatAt = roundIt((isMetric ? (cToF(child.device.currentValue('heatingSetpoint').toBigDecimal()) * 10.0) : (child.device.currentValue('heatingSetpoint').toBigDecimal() * 10.0)), 0)		// better precision using BigDecimal round-half-up
		def currCoolAt = roundIt((isMetric ? (cToF(child.device.currentValue('coolingSetpoint').toBigDecimal()) * 10.0) : (child.device.currentValue('coolingSetpoint').toBigDecimal() * 10.0)), 0)
        LOG("setHold() - currHeat: ${currHeatAt}, currCool: ${currCoolAt}",2, child, 'trace')
        // if ((c==currCoolAt) && (h==currHeatAt)) {
        if ((cooling==currCoolAt) && (heating==currHeatAt)) {
        	LOG("setHold() for thermostat ${statName} requesting current setpoints; ignoring", 3, child, 'info')
        	return true 
        }
    } else if (theHoldType == 'holdHours') {
    	theHoldType = 'holdHours","holdHours":"' + sendHoldHours.toString()
    } 
    
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"functions":[{"type":"setHold","params":{"coolHoldTemp":"' + c + '","heatHoldTemp":"' + h + 
    						'","holdType":"' + theHoldType + '"}}]}'
   	LOG("setHold() for thermostat ${child.device.displayName} - about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child)
    
	def result = sendJson(child, jsonRequestBody)
    LOG("setHold() for thermostat ${statName} returned ${result}", 4, child,result?'info':'error')
    if (result) { 
    	// send the new heat/cool setpoints and ProgramId to the DTH - it will update the rest of the related displayed values itself
    	Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
    	Integer userPrecision = getTempDecimals()						// user's requested display precision
   		def tempHeatAt = h //.toBigDecimal()
        def tempCoolAt = c //.toBigDecimal()
        def tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt / 10.0), 'F', apiPrecision)
       	def tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt / 10.0), 'F', apiPrecision)
    	def updates = ['heatingSetpoint':String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
        			   'coolingSetpoint':String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
                       'currentProgramName': 'Hold: Temp'
                      ]
        LOG("setHold() for thermostat ${statName} - ${updates}",3,null,'info')
        child.generateEvent(updates)			// force-update the calling device attributes that it can't see
        // atomicState.forcePoll = true 			// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
	} else {
        if (atomicState.connected?.toString() != 'full') {
            LOG("API connection lost, queueing failed call to setHold(${child}, ${heating}, ${cooling}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
            queueFailedCall('setHold', child.device.deviceNetworkId, 5, heating, cooling, deviceId, sendHoldType, sendHoldHours)
        }
    }
    return result
}

// Should only be called by child devices, and they MUST provide sendHoldType and sendHoldHours as of version 1.2.0
def setFanMode(child, fanMode, fanMinOnTime, deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setFanMode(${child}, ${fanMode}, ${fanMinOnTime}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
        queueFailedCall('setFanMode', child.device.deviceNetworkId, 5, fanMode, fanMinOnTime, deviceId, sendHoldType, sendHoldHours)
        return false
    }
    
	String statName = getThermostatName(deviceId)
	LOG("setFanMode(${fanMode}) for for thermostat ${statName}", 5, null, 'trace') 
    def isMetric = (getTemperatureScale() == "C")
    
    def currentThermostatHold = child.device.currentValue('thermostatHold') 
    if (currentThermostatHold == 'vacation') {
    	LOG("setFanMode() for thermostat ${statName}: Can't change Fan Mode while in a vacation hold",2,null,'warn')
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
   	def h = roundIt((isMetric ? (cToF(child.device.currentValue('heatingSetpoint').toBigDecimal()) * 10.0) : (child.device.currentValue('heatingSetpoint').toBigDecimal() * 10.0)), 0)		// better precision using BigDecimal round-half-up
	def c = roundIt((isMetric ? (cToF(child.device.currentValue('coolingSetpoint').toBigDecimal()) * 10.0) : (child.device.currentValue('coolingSetpoint').toBigDecimal() * 10.0)), 0)
    
    def theHoldType = sendHoldType // ? sendHoldType : whatHoldType(child)
    if (theHoldType == 'holdHours') {
    	theHoldType = 'holdHours","holdHours":"' + sendHoldHours.toString()
    }

	def thermostatSettings = ''
    def thermostatFunctions = ''
    
    // CIRCULATE: same as AUTO, but with a non-zero fanMinOnTime
    if (fanMode == "circulate") {
    	LOG("setFanMode() for thermostat ${statName} - fanMode == 'circulate'", 5, child, "trace") 
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
    LOG("setFanMode() for thermostat ${statName} - about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child, 'trace')
    
	def result = sendJson(child, jsonRequestBody)
    LOG("setFanMode(${fanMode}) for thermostat ${statName} returned ${result}", 4, child, result?'info':'warn')
    // if (result) atomicState.forcePoll = true 		// force next poll to get updated data
	runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    if (!result) {
        if (atomicState.connected?.toString() != 'full') {
            LOG("API connection lost, queueing failed call to setFanMode(${child}, ${fanMode}, ${fanMinOnTime}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
            queueFailedCall('setFanMode', child.device.deviceNetworkId, 5, fanMode, fanMinOnTime, deviceId, sendHoldType, sendHoldHours)
        }
    }
    return result    
}

def setProgram(child, program, String deviceId, sendHoldType='indefinite', sendHoldHours=2) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setProgram(${child}, ${program}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
        queueFailedCall('setProgram', child.device.deviceNetworkId, 4, program, deviceId, sendHoldType, sendHoldHours)
        return false
    }

	String statName = getThermostatName(deviceId)
	// NOTE: Will use only the first program if there are two with the same exact name
	LOG("setProgram(${program}) for for thermostat ${statName} - holdType: ${sendHoldType}, holdHours: ${sendHoldHours}", 2, child, 'info')     
    
    def currentThermostatHold = child.device.currentValue('thermostatHold') 
    if (currentThermostatHold == 'vacation') {									// shouldn't happen, child device should have already verified this
    	LOG("setProgram() for thermostat ${statName}: Can't change Program while in a vacation hold",2,null,'warn')
        return false
    } else if (currentThermostatHold != '') {									// shouldn't need this either, child device should have done this before calling us
		LOG("setProgram( ${program} ) for thermostat ${statName}: Resuming from current hold first",2,null,'info')
        resumeProgram(child, deviceId, true)
    }
   	
    // We'll take the risk and use the latest received climates data (there is a small chance it could have changed recently but not yet been picked up)
    def climates = atomicState.program[deviceId].climates
    def climate = climates?.find { it.name.toString() == program.toString() }  // vacation holds can have a number as their name
    LOG("climates - {$climates}", 5, child)
    LOG("climate - {$climate}", 5, child)
    def climateRef = climate?.climateRef.toString()
    
	LOG("setProgram() for thermostat ${statName} - climateRef = {$climateRef}", 4, child)
	
    if (climate == null) { return false }
    
    def theHoldType = sendHoldType //? sendHoldType : whatHoldType(child)
    if (theHoldType == 'holdHours') {
    	theHoldType = 'holdHours","holdHours":"' + sendHoldHours.toString()
    }
	def jsonRequestBody = '{"functions":[{"type":"setHold","params":{"holdClimateRef":"'+climateRef+'","holdType":"'+theHoldType+'"}}],"selection":{"selectionType":"thermostats","selectionMatch":"'+deviceId+'"}}'

    LOG("setProgram() for thermostat ${child.device.displayName}: about to sendJson with jsonRequestBody (${jsonRequestBody}", 4, child)    
	def result = sendJson(child, jsonRequestBody)	
    LOG("setProgram(${climateRef}) for thermostat ${statName} returned ${result}", 4, child, 'info')
    
    if (result) { 
    	// send the new heat/cool setpoints and ProgramId to the DTH - it will update the rest of the related displayed values itself
    	Integer apiPrecision = usingMetric ? 2 : 1					// highest precision available from the API
    	Integer userPrecision = getTempDecimals()						// user's requested display precision
   		def tempHeatAt = climate.heatTemp
        def tempCoolAt = climate.coolTemp
        def tempHeatingSetpoint = myConvertTemperatureIfNeeded( (tempHeatAt / 10.0), 'F', apiPrecision)
       	def tempCoolingSetpoint = myConvertTemperatureIfNeeded( (tempCoolAt / 10.0), 'F', apiPrecision)
    	def updates = ['heatingSetpoint':String.format("%.${userPrecision}f", roundIt(tempHeatingSetpoint, userPrecision)),
        			   'coolingSetpoint':String.format("%.${userPrecision}f", roundIt(tempCoolingSetpoint, userPrecision)),
                       'currentProgram': program,
                       'currentProgramId':climateRef]
        LOG("setProgram() for thermostat ${statName} ${updates}",3,null,'info')
        child.generateEvent(updates)			// force-update the calling device attributes that it can't see
        // atomicState.forcePoll = true 		// force next poll to get updated data
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
	} else {
        if (atomicState.connected?.toString() != 'full') {
            LOG("API is not fully connected, queueing call to setProgram(${child}, ${program}, ${deviceId}, ${sendHoldType} ${sendHoldHours}", 2, child, 'warn')
            queueFailedCall('setProgram', child.device.deviceNetworkId, 4, program, deviceId, sendHoldType, sendHoldHours)
        } else {
    		LOG("setProgram(${program}) for for thermostat ${statName} FAILED", 1, child, 'warn') 
        }
    }
    return result
}

////////////////////////////////////////////////////////////////
def addSensorToProgram(child, deviceId, sensorId, programId) {
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
					LOG("addSensorToProgram() - ${sensName} is already in the ${programId.capitalize()} program on thermostat ${statName}",4,child,'info')
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
def deleteSensorFromProgram(child, deviceId, sensorId, programId) {
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
                    runIn(2, 'updateClimates', [data: [deviceId: deviceId, child: child]])
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

def setProgramSetpoints(child, String deviceId, String programName, String heatingSetpoint, String coolingSetpoint) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setProgramSetpoints(${child}, ${deviceId}, ${heatingSetpoint} ${coolingSetpoint}", 2, child, 'warn')
        queueFailedCall('setProgramSetpoint', child.device.deviceNetworkId, 4, deviceId, programName, heatingSetpoint, coolingSetpoint)
        return false
    }
    
	String statName = getThermostatName( deviceId )
	LOG("setProgramSetpoints(${deviceId} (${statName}): ${programName}, heatSP: ${heatingSetpoint}, coolSP: ${coolingSetpoint})", 2, child, 'trace')
    
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
	def ht = ((heatingSetpoint != null) && heatingSetpoint.isBigDecimal() ) ? (roundIt((isMetric ? (cToF(heatingSetpoint.toBigDecimal()) * 10.0) : (heatingSetpoint.toBigDecimal() * 10.0)), 0)) : null		// better precision using BigDecimal round-half-up
	def ct = ((coolingSetpoint != null) && coolingSetpoint.isBigDecimal() ) ? (roundIt((isMetric ? (cToF(coolingSetpoint.toBigDecimal()) * 10.0) : (coolingSetpoint.toBigDecimal() * 10.0)), 0)) : null
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
            	LOG("setProgramSetpoints(): ${programName} setpoints changed successfully", 2, child, 'info')
            	return true
            } else {
            	LOG("setProgramSetpoints(): ${programName} setpoints change failed", 1, child, 'warn')
                // queue failed request
                return false
            }
        }
        c++
    }
    // didn't find the specified climate
    LOG("setProgramSetpoints(): ${programName} not found on thermostat ${statName}", 1, child, 'warn')
    return false
}

def updateClimates(data) {
	updateClimatesDirect(data.child, data.deviceId)
}

def updateClimatesDirect(child, deviceId) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to updateClimateChangeDirect(${child}, ${deviceId})", 2, child, 'warn')
        queueFailedCall('updateClimatesDirect', child.device.deviceNetworkId, 1, deviceId)
        return false
    }
    
    def statName = getThermostatName( deviceId )
	LOG("Updating Program settings for ${statName}", 4, child, 'info')
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
    	LOG("Updating Program settings for ${statName} returned ${result}", 4, child, result?'info':'warn')
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
	runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
    return result    
}

def setEcobeeSetting(child, String deviceId, String name, String value) {
	if (child == null) {
       	child = getChildDevice(getThermostatDNI(deviceId))
    }
	if (atomicState.connected?.toString() != 'full') {
		LOG("API is not fully connected, queueing call to setEcobeeSetting(${child}, ${deviceId}, ${name} ${value}", 2, child, 'warn')
        queueFailedCall('setEcobeeSetting', child.device.deviceNetworkId, 3, deviceId, name, value)
        return false
    }
	String statName = getThermostatName( deviceId )
    name = name.trim()				// be a little lenient

	def dItem = EcobeeDirectSettings.find{ it.name == name }
	if (dItem != null) {
		// LOG("setEcobeeSetting() - Invalid command, use '${dItem.command}' to change '${name}'", 1, child, 'error')
		LOG("setEcobeeSetting( ${name}, ${value} ) - calling ${dItem.command}( ${value} )", 2, child, 'info')
		return "${dItem.command}"(child, value, deviceId)
		// return false
	}
	def sendValue = null
	def found = false
	if (EcobeeTempSettings.contains(name)) {
		// Is a temperature setting - need to convert to F*10 for Ecobee
		found = true
		def isMetric = (getTemperatureScale() == "C")
		sendValue = ((value != null) && value.isBigDecimal()) ? (roundIt((isMetric ? (cToF(value as BigDecimal) * 10.0) : ((value as BigDecimal) * 10.0)), 0)) : null
	} else if (EcobeeSettings.contains(name)) {
		found = true
		sendValue = value.trim()	// leniency is kindness
	} else if (EcobeeROSettings.contains(name)) {
		LOG("setEcobeeSetting(name: '${name}', value: '${value}') - Setting is Read Only", 1, child, 'error')
		return false
	}
	if (sendValue == null) {
		if (!found) {
			LOG("setEcobeeSetting(name: '${name}', value: '${value}') - Invalid name", 1, child, 'error')
		} else {
			LOG("setEcobeeSetting(name: '${name}', value: '${value}') - Invalid value", 2, child, 'error')
		}
		return false
	}                 
	def jsonRequestBody = '{"selection":{"selectionType":"thermostats","selectionMatch":"' + deviceId + '"},"thermostat":{"settings":{"'+name+'":"'+"${sendValue}"+'"}}}'  
	LOG("setEcobeeSetting() - Request Body: ${jsonRequestBody}", 4, child, 'trace')
    def result = sendJson(child, jsonRequestBody)
	LOG("setEcobeeSetting ${name} to ${sendValue} with result ${result}", 4, child, 'trace')
	if (result) {
		if (value == sendValue) {
			LOG("Ecobee Setting '${name}' on thermostat ${statName} was successfully changed to '${value}'", 2, child, 'info')
		} else {
			LOG("Ecobee Setting '${name}' on thermostat ${statName} was successfully changed to '${value}' ('${sendValue}')", 2, child, 'info')
		}
		runIn(5, pollChildren, [overwrite: true]) 	// Pick up the changes
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
private def sendJson(child=null, String jsonBody) {
	def debugLevelFour = false // debugLevel(4)
    if (debugLevelFour) LOG("sendJson() - ${jsonBody}",1,child,'debug')
	def returnStatus
    def result = false
    
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
                	atomicState.sendJsonRetry = true 		// retry only once
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
    } catch (org.apache.http.conn.ConnectTimeoutException | org.apache.http.conn.HttpHostConnectException | javax.net.ssl.SSLPeerUnverifiedException | javax.net.ssl.SSLHandshakeException | java.net.SocketTimeoutException e) {
    	LOG("sendJson() - ${e}.",1,null,'warn')  // Just log it, and hope for better next time...
        if (apiConnected != 'warn') {
        	atomicState.connected = 'warn'
			updateMyLabel()
        	atomicState.lastPoll = now()
        	atomicState.lastPollDate = getTimestamp()
			generateEventLocalParams()
        }
        // If no cached calls, retry 8 times
        // if cached calls already, or if retries failed, then queue the call
        //    Cache Map by thermostat, order, (child & jsonBody)
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

private def sendJsonRetry() {
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

private def getChildThermostatName() { return "Ecobee Suite Thermostat" }
private def getChildSensorName()     { return "Ecobee Suite Sensor" }
private def getServerUrl()           { return (isST) ? "https://graph.api.smartthings.com" : getFullApiServerUrl()}	// hubitat: /oauth/authorize}
private def getShardUrl()            { return (isST) ? getApiServerUrl() : getFullApiServerUrl()+"?access_token=${atomicState.accessToken}" }
private def getCallbackUrl()         { return (isST) ? "${serverUrl}/oauth/callback" : "https://cloud.hubitat.com/oauth/stateredirect" } // : */ "${serverUrl}/callback" } // &" + URLEncoder.encode("access_token", "UTF-8") + "=" + URLEncoder.encode(atomicState.accessToken, "UTF-8") } // #access_token=${atomicState.accessToken}" }
private def getBuildRedirectUrl()    { return (isST) ? "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${shardUrl}" : 
									  				   "${serverUrl}/oauth/stateredirect?access_token=${atomicState.accessToken}" }
private def getStateUrl() 			 { return "${getHubUID()}/apps/${app?.id}/callback?access_token=${atomicState?.accessToken}" }
private def getApiEndpoint()         { return "https://api.ecobee.com" }
private def getInfo()				 { return 'info' }
private def getWarn()				 { return 'warn' }
private def getTrace()				 { return 'trace' }
private def getDebug()				 { return 'debug' }
private def getError()				 { return 'error' }

// This is the API Key from the Ecobee developer page. Can be provided by the app provider or use the appSettings
private def getEcobeeApiKey() { 
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
			atomicState.apiKey = 	"EnJClRbJeT7DqPnlc29goR1hQvnV33tE"	// NEW Ecobee key for Ecobee Suite 1.7.** on SmartThings
			return					"EnJClRbJeT7DqPnlc29goR1hQvnV33tE"
		}
	} else {
		return appSettings.clientId 
    }
}

private String getThermostatName(String tid) {
    // Get the name for this thermostat
    String DNI = (isHE?'ecobee_suite-thermostat-':'') + ([ app.id, tid ].join('.'))
    def thermostatsWithNames = atomicState.thermostatsWithNames
    String tstatName = (thermostatsWithNames?.containsKey(DNI)) ? thermostatsWithNames[DNI] : ''
    if (tstatName == '') {
        tstatName = getChildDevice(DNI)?.displayName		// better than displaying 'null' as the name
    }
    return tstatName
}

private String getThermostatDNI(String tid) {
	return (isHE?'ecobee_suite-thermostat-':'') + ([app.id, tid].join('.'))
}

private def LOG(message, level=3, child=null, String logType='debug', event=false, displayEvent=true) {
	def dbgLevel = debugLevel(level)
    if (!dbgLevel) return 		// let's not waste CPU cycles if we don't have to...
    
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

private def debugEvent(message, displayEvent = false) {
	def results = [
		name: 'appdebug',
		descriptionText: message,
		displayed: displayEvent
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent (results)
}

private def debugEventFromParent(child, message) {
	 def data = [
            	debugEventFromParent: message
            ]         
	if (child) { child.generateEvent(data) }
}

// TODO: Create a more generic push capability to send notifications
// send both push notification and mobile activity feeds
private def sendPushAndFeeds(notificationMessage) {
	LOG("Notification Message: ${notificationMessage}", 2, null, "trace")
	LOG("Notification Time: ${atomicState.timeSendPush}", 2, null, "info")
    
    // notification is sent to remind user no more than once per hour
    Boolean sendNotification = (atomicState.timeSendPush && ((now() - atomicState.timeSendPush) < 3600000)) ? false : true
    if (sendNotification) {
    	String msg = "Your ${location.name} Ecobee${settings.thermostats.size()>1?'s':''} " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (isST) {
			if (phone) { // check that the user did select a phone number
				if ( phone.indexOf(";") > 0){
					def phones = settings.phone.split(";")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i], msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${phone}", 3, null, 'info')
					sendSmsMessage(phone, msg)						// Only to SMS contact
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
						sendSmsMessage(phones[i], msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${phone}", 3, null, 'info')
					sendSmsMessage(phone, msg)						// Only to SMS contact
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

private def sendActivityFeeds(notificationMessage) {
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
	if ( (cmdScale != "C") && (cmdScale != "F") && (cmdScale != "dC") && (cmdScale != "dF") ) {
    	// We do not have a valid Scale input, throw a debug error into the logs and just return the passed in value
        LOG("Invalid temp scale used: ${cmdScale}", 2, null, "error")
        return scaledSensorValue
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
private roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
private roundIt( BigDecimal value, decimals=0) {
    return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def wantMetric() {
	return (getTemperatureScale() == "C")
}

private cToF(temp) {
	if (debugLevel(5)) LOG("cToF entered with ${temp}", 5, null, "info")
	if (temp) { return (temp * 1.8 + 32) } else { return null } 
    // return celsiusToFahrenheit(temp)
}
private fToC(temp) {	
	if (debugLevel(5)) LOG("fToC entered with ${temp}", 5, null, "info")
	if (temp) { return (temp - 32) / 1.8 } else { return null } 
    // return fahrenheitToCelsius(temp)
}

// Establish the minimum amount of time to wait to do another poll
private def getMinMinBtwPolls() {
    // TODO: Make this configurable in the SmartApp
	return 1
}

def toJson(Map m) {
    return groovy.json.JsonOutput.toJson(m)
}

// need these next 5 get routines because settings variable defaults aren't set unless the "Preferences" page is actually opened/rendered
def Integer getPollingInterval() {
    return (settings?.pollingInterval?.isNumber() ? settings.pollingInterval as Integer : 5)
}

private Integer getTempDecimals() {
	return ( settings?.tempDecimals?.isNumber() ? settings.tempDecimals as Integer : (wantMetric() ? 1 : 0))
}

private Integer getDebugLevel() {
	return ( settings?.debugLevel?.isNumber() ? settings.debugLevel as Integer : 3)
}

private String getHoldType() {
	return settings.holdType ?: 'Until I Change'
}

private Integer getHoldHours() {
	if (settings.holdType) {
    	if (settings.holdType == 'Specified Hours') return ((settings.holdHours != null) && settings.holdHours?.isNumber() ? settings.holdHours as Integer : 2)
        else if (settings.holdType == '2 Hours') return 2
        else if (settings.holdType == '4 Hours') return 4
    	else return 2
    }
}

private def String getTimestamp() {
	// There seems to be some possibility that the timeZone will not be returned and will cause a NULL Pointer Exception
	def timeZone = location?.timeZone ?: ""
    // LOG("timeZone found as ${timeZone}", 5)
    if(timeZone == "") {
    	return new Date().format("yyyy-MM-dd HH:mm:ss z")
    } else {
		return new Date().format("yyyy-MM-dd HH:mm:ss z", timeZone)
	}
}

private def getTimeOfDay(tid = null) {
	def nowTime 
    if(atomicState.timeZone) {
    	nowTime = new Date().format("HHmm", TimeZone.getTimeZone(atomicState.timeZone)).toInteger()
    } else if (tid && atomicState.statLocation && atomicState.statLocation[tid]) {
    	nowTime = new Date().format("HHmm", TimeZone.getTimeZone(atomicState.statLocation[tid].timeZone)).toInteger()
    } else {
    	nowTime = new Date().format("HHmm").toInteger()
    }
    if (debugLevel(4)) LOG("getTimeOfDay() - nowTime = ${nowTime}", 4, null, "trace")
    if ( (nowTime < atomicState.sunriseTime) || (nowTime > atomicState.sunsetTime) ) {
    	return "night"
    } else {
    	return "day"
    }
}

// we'd prefer to use the timeZone that the thermostat says it is in, so long as ALL the thermostats agree
// if thermostats don't have a timeZone, use the SmartThings location.timeZone
// if ST location doesn't have a time zone, we're just going to have to use ST's "local time"
private def getTimeZone() {
	if ((atomicState.timeZone == null) || atomicState.forcePoll) {
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

private String getZipCode() {
	// default to the SmartThings location's timeZone (if there is one)
	String myZipCode = location?.zipCode
    if (myZipCode == null) {
    	LOG("*** INITIALIZATION ERROR *** PLEASE SET POSTAL CODE FOR LOCATION '${location.name}'",1,null,'warn')
        atomicState.zipCode == null
    }
	if ((atomicState.zipCode == null) || atomicState.forcePoll) {
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
private String apiConnected() {
	// values can be "full", "warn", "lost"
	if (atomicState.connected == null) { atomicState.connected = "warn"; updateMyLabel(); }
	return atomicState.connected?.toString() ?: "lost"
}

private def apiRestored() {
	atomicState.connected = "full"
	updateMyLabel()
	unschedule("notifyApiLost")
    atomicState.reAttemptPoll = 0
    runIn(10, runCallQueue)
}

private def getDebugDump() {
	 def debugParams = [when:"${getTimestamp()}", whenEpic:"${now()}", 
				lastPollDate:"${atomicState.lastPollDate}", lastScheduledPollDate:"${atomicState.lastScheduledPollDate}", 
				lastScheduledWatchdogDate:"${atomicState.lastScheduledWatchdogDate}",
				lastTokenRefreshDate:"${atomicState.lastTokenRefreshDate}", 
                initializedEpic:"${atomicState.initializedEpic}", initializedDate:"${atomicState.initializedDate}",
                lastLOGerror:"${atomicState.lastLOGerror}", authTokenExpires:"${atomicState.authTokenExpires}"
			]    
	return debugParams
}

private def apiLost(where = "[where not specified]") {
    LOG("apiLost() - ${where}: Lost connection with APIs. unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with Ecobee by running the SmartApp and logging in again", 1, null, "error")
    atomicState.apiLostDump = getDebugDump()
    if (apiConnected() == "lost") {
    	LOG("apiLost() - already in lost atomicState. Nothing else to do. (where= ${where})", 5, null, "trace")
        return
    }
   
    // provide cleanup steps when API Connection is lost
	def notificationMessage = "${settings.thermostats.size()>1?'are':'is'} disconnected from SmartThings/Ecobee, because the access credential changed or was lost. Please go to the Ecobee Suite Manager SmartApp and re-enter your account login credentials."
    atomicState.connected = "lost"
	updateMyLabel()
    atomicState.authToken = null
    
    sendPushAndFeeds(notificationMessage)
	generateEventLocalParams()

    LOG("Unscheduling Polling and refreshAuthToken. User MUST reintialize the connection with Ecobee by running the SmartApp and logging in again", 0, null, "error")
    
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

def notifyApiLost() {
	def notificationMessage = "${settings.thermostats.size()>1?'are':'is'} disconnected from SmartThings/Ecobee. Please go to the Ecobee Suite Manager and re-enter your Ecobee account login credentials."
    if ( atomicState.connected == "lost" ) {
    	generateEventLocalParams()
		sendPushAndFeeds(notificationMessage)
        LOG("notifyApiLost() - API Connection Previously Lost. User MUST reintialize the connection with Ecobee by running the SmartApp and logging in again", 0, null, "error")
	} else {
    	// Must have restored connection
        unschedule("notifyApiLost")
    }    
}

private String childType(child) {
	// Determine child type (Thermostat or Remote Sensor)
    if ( child.hasCapability("Thermostat") ) { return getChildThermostatName() }
    if ( child.name.contains( getChildSensorName() ) ) { return getChildSensorName() }
    return "Unknown"
}

private def getFanMinOnTime(child) {
	if (debugLevel(4)) LOG("getFanMinOnTime() - Looking up current fanMinOnTime for ${child.device?.displayName}", 4, child)
    String devId = getChildThermostatDeviceIdsString(child)
    LOG("getFanMinOnTime() Looking for ecobee thermostat ${devId}", 5, child, "trace")
    
    def fanMinOnTime = atomicState.settings[devId]?.fanMinOnTime
    if (debugLevel(4)) LOG("getFanMinOnTime() fanMinOnTime is ${fanMinOnTime} for therm ${devId}", 4, child)
	return fanMinOnTime
}

private String getHVACMode(child) {
	if (debugLevel(4)) LOG("Looking up current hvacMode for ${child.device?.displayName}", 4, child)
    String devId = getChildThermostatDeviceIdsString(child)
    // LOG("getHVACMode() Looking for ecobee thermostat ${devId}", 5, child, "trace")
    
    def hvacMode = atomicState.settings[devId].hvacMode		// FIXME
	if (debugLevel(4)) LOG("getHVACMode() hvacMode is ${hvacMode} for therm ${devId}", 4, child)
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

private def debugLevel(level=3) {
    Integer dbgLevel = getDebugLevel()
    return (dbgLevel == 0) ? false : (dbgLevel >= level?.toInteger())
}

private String fixDateTimeString( String dateStr, String timeStr, String thermostatTime) {
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
    } else if (dateStr == '2035-01-01' ) { 		// to Infinity
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

private def getDeviceId(String networkId) {
	// def deviceId = networkId.split(/\./).last()	
    // LOG("getDeviceId() returning ${deviceId}", 4, null, 'trace')
    // return deviceId
    return networkId.split(/\./).last()
}

// Reservation Management Functions
// Make a reservation for me
void makeReservation( String statId, String childId, String type='modeOff' ) {
    def childName = getChildAppName( childId )
	if (!childName) {
    	LOG("Illegal reservation attempt using childId: ${childId} - caller is not my child.",1,null,'warn')
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
        LOG("'${type}' reservation created for ${childName}",2,null,'info')
    }
}
// Cancel my reservation
void cancelReservation( String statId, String childId, String type='modeOff') {
    def childName = getChildAppName( childId )
    if (!childName) childName = childId
    def reservations = atomicState.reservations
    if (reservations?."${statId}"?."${type}"?.contains(childId)) {
    	reservations."${statId}"."${type}" = reservations."${statId}"."${type}" - [childId]
        atomicState.reservations = reservations
        LOG("'${type}' reservation cancelled for ${childName}",2,null,'info')
    }
}
// Do I have a reservation?
Boolean haveReservation( String statId, String childId, String type='modeOff') {
    def reservations = atomicState.reservations
	return (reservations?."${statId}"?."${type}"?.contains(childId))
}
// Do any Apps have reservations?
Boolean anyReservations( String statId, String type='modeOff') {
	def reservations = atomicState.reservations
	return (reservations?."${statId}"?.containsKey(type)) ? (reservations."${statId}"."${type}".size() != 0) : false
}
// How many apps have reservations?
Integer countReservations(String statId, String type='modeOff') {
	def reservations = atomicState.reservations
	return (reservations?."${statId}"?.containsKey(type)) ? reservations."${statId}"."${type}".size() : 0
}
// Get the list of app IDs that have reservations
List getReservations(String statId, String type='modeOff') {
	def reservations = atomicState.reservations
    return (reservations?."${statId}"?.containsKey(type)) ? reservations."${statId}"."${type}" : []
}
// Get the list of app Names that have reservations
List getGuestList(String statId, String type='modeOff') {
	def guestList = []
	def reservations = atomicState.reservations
    if (reservations?."${statId}"?.containsKey(type)) {
        reservations."${statId}"."${type}".each {
        	guestList << getChildAppName( it )
        }
    }
    return guestList
}

def runEvery2Minutes(handler) {
	Random rand = new Random()
    //log.debug "Random2: ${rand}"
    int randomSeconds = rand.nextInt(59)
	schedule("${randomSeconds} 0/2 * * * ?", handler)
}

def runEvery3Minutes(handler) {
	Random rand = new Random()
    //log.debug "Random3: ${rand}"
    int randomSeconds = rand.nextInt(59)
	schedule("${randomSeconds} 0/3 * * * ?", handler)
}

// Alert settings
@Field final List alertNamesList = 		['auxOutdoorTempAlertNotify','auxRuntimeAlert','auxRuntimeAlertNotify','coldTempAlertEnabled','disableAlertsOnIdt','disableHeatPumpAlerts','hotTempAlertEnabled','humidityAlertNotify',
										 'humidityHighAlert','humidityLowAlert','randomStartDelayCool','randomStartDelayHeat','tempAlertNotify','ventilatorOffDateTime','wifiOfflineAlert'
										]
// Settings (attributes)
@Field final List settingsNamesList = 	['autoAway','auxOutdoorTempAlertNotifyTechnician','auxRuntimeAlertNotifyTechnician','backlightOffDuringSleep','backlightOffTime','backlightOnIntensity','backlightSleepIntensity',
										 'compressorProtectionMinTime','compressorProtectionMinTime','condensationAvoid','coolingLockout','dehumidifyWhenHeating','dehumidifyWithAC','disablePreCooling','disablePreHeating','drAccept',
										 'eiLocation','electricityBillCycleMonths','electricityBillStartMonth','electricityBillingDayOfMonth','enableElectricityBillAlert','enableProjectedElectricityBillAlert','fanControlRequired',
										 'followMeComfort','groupName','groupRef','groupSetting','hasBoiler','hasElectric','hasErv','hasForcedAir','hasHeatPump','hasHrv','hasUVFilter','heatPumpGroundWater','heatPumpReversalOnCool',
										 'holdAction','humidityAlertNotify','humidityAlertNotifyTechnician','humidityHighAlert','humidityLowAlert','installerCodeRequired','isRentalProperty','isVentilatorTimerOn','lastServiceDate',
										 'locale','monthlyElectricityBillLimit','monthsBetweenService','remindMeDate','serviceRemindMe','serviceRemindTechnician','smartCirculation','soundAlertVolume','soundTickVolume',
										 'stage1CoolingDissipationTime','stage1HeatingDissipationTime','tempAlertNotifyTechnician','userAccessCode','userAccessSetting','ventilatorDehumidify','ventilatorFreeCooling',
										 'ventilatorMinOnTimeAway','ventilatorMinOnTimeHome','ventilatorOffDateTime','ventilatorType'
										]
// Temperature Settings
@Field final List tempSettingsList = 	['auxMaxOutdoorTemp','auxOutdoorTempAlert','coldTempAlert','compressorProtectionMinTemp','compressorProtectionMinTemp','coolMaxTemp','coolMinTemp',
										 'dehumidifyOvercoolOffset','heatMaxTemp','heatMinTemp','hotTempAlert','maxSetBack','maxSetForward','quickSaveSetBack','quickSaveSetForward','stage1CoolingDifferentialTemp',
										 'stage1HeatingDifferentialTemp','tempCorrection'
										]
// Settings that require Temperature Conversion (callers use native C/F temps)
@Field final List EcobeeTempSettings = 	['auxMaxOutdoorTemp','auxOutdoorTempAlert','compressorProtectionMinTemp','coldTempAlert','coolRangeHigh','coolRangeLow','dehumidifyOvercoolOffset',
										 'heatCoolMinDelta','heatRangeHigh','heatRangeLow','hotTempAlert','maxSetBack','maxSetForward','quickSaveSetBack','quickSaveSetForward','stage1CoolingDifferentialTemp',
										 'stage1HeatingDifferentialTemp','tempCorrection'
										]
// Settings that are passed directly as Strings (including numbers and logicals)
@Field final List EcobeeSettings = 		['autoAway','auxOutdoorTempAlertNotifyTechnician','auxRuntimeAlertNotifyTechnician','backlightOffDuringSleep','backlightOffTime','backlightOnIntensity','backlightSleepIntensity',
										 'coldTempAlertEnabled','compressorProtectionMinTime','compressorProtectionMinTime','condensationAvoid','coolingLockout','dehumidifyWhenHeating','dehumidifyWithAC',
										 'disablePreCooling','disablePreHeating','drAccept','eiLocation','electricityBillCycleMonths','electricityBillStartMonth','electricityBillingDayOfMonth','enableElectricityBillAlert',
										 'enableProjectedElectricityBillAlert','fanControlRequired','followMeComfort','groupName','groupRef','groupSetting','heatPumpReversalOnCool','holdAction','hotTempAlertEnabled','humidityAlertNotify',
										 'humidityAlertNotifyTechnician','humidityHighAlert','humidityLowAlert','installerCodeRequired','isRentalProperty','isVentilatorTimerOn','lastServiceDate','locale',
										 'monthlyElectricityBillLimit','monthsBetweenService','remindMeDate','serviceRemindMe','serviceRemindTechnician','smartCirculation','soundAlertVolume','soundTickVolume',
										 'stage1CoolingDissipationTime','stage1HeatingDissipationTime','tempAlertNotifyTechnician','ventilatorDehumidify','ventilatorFreeCooling','ventilatorMinOnTimeAway','ventilatorMinOnTimeHome'
										]
// Settings that are Read Only and cannot be changed
@Field final List EcobeeROSettings =	['coolMaxTemp','coolMinTemp','coolStages','hasBoiler','hasDehumidifier','hasElectric','hasErv','hasForcedAir','hasHeatPump','hasHrv','hasHumidifier','hasUVFilter','heatMaxTemp','heatMinTemp',
										 'heatPumpGroundWater','heatStages','userAccessCode','userAccessSetting'
										]
// Settings that must be changed only by specific commands
@Field final List EcobeeDirectSettings= [
											[name: 'fanMinOnTime',			command: 'setFanMinOnTime'],
											[name: 'dehumidifierMode',		command: 'setDehumidifierMode'],
											[name: 'dehumidifierLevel', 	command: 'setDehumiditySetpoint'],
											[name: 'dehumiditySetpoint',	command: 'setDehumiditySetpoint'],
											[name: 'humidity',				command: 'setHumiditySetpoint'],
											[name: 'humidifierMode',		command: 'setHumidifierMode'],
											[name: 'humiditySetpoint',		command: 'setHumiditySetpoint'],
//											[name: 'schedule',				command: 'setSchedule']
										]
@Field final List EcobeeDeviceInfo =    [ 'brand','features','identifier','isRegistered','lastModified','modelNumber','name','thermostatRev']


// **************************************************************************************************************************
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
//
// The following 3 calls are safe to use anywhere within a Device Handler or Application
//  - these can be called (e.g., if (getPlatform() == 'SmartThings'), or referenced (i.e., if (platform == 'Hubitat') )
//  - performance of the non-native platform is horrendous, so it is best to use these only in the metadata{} section of a
//    Device Handler or Application
//
//	1.0.0	Initial Release
//	1.0.1	Use atomicState so that it is universal
//
private String  getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
private Boolean getIsST()     { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
private Boolean getIsHE()     { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
//
// The following 3 calls are ONLY for use within the Device Handler or Application runtime
//  - they will throw an error at compile time if used within metadata, usually complaining that "state" is not defined
//  - getHubPlatform() ***MUST*** be called from the installed() method, then use "state.hubPlatform" elsewhere
//  - "if (state.isST)" is more efficient than "if (isSTHub)"
//
private String getHubPlatform() {
	def pf = getPlatform()
    atomicState?.hubPlatform = pf			// if (atomicState.hubPlatform == 'Hubitat') ... 
											// or if (state.hubPlatform == 'SmartThings')...
    atomicState?.isST = pf.startsWith('S')	// if (atomicState.isST) ...
    atomicState?.isHE = pf.startsWith('H')	// if (atomicState.isHE) ...
    return pf
}
private Boolean getIsSTHub() { return atomicState.isST }					// if (isSTHub) ...
private Boolean getIsHEHub() { return atomicState.isHE }					// if (isHEHub) ...

private def getParentSetting(String settingName) {
	// def ST = (atomicState?.isST != null) ? atomicState?.isST : isST
	//log.debug "isST: ${isST}, isHE: ${isHE}"
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"	
}
//
// **************************************************************************************************************************
