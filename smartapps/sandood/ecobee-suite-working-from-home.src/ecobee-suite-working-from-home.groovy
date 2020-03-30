/**
 *  Ecobee Suite Working From Home
 *
 *	Copyright 2017-2020 Barry A. Burke
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
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - nonCached currentValue() for HE
 *	1.7.02 - Optionally identify who is still home in logs and notifications
 *	1.7.03 - Miscellaneous optimizations
 *	1.7.04 - Fixed myThermostats subscription (thx @astephon88) & missing sendMessages
 *	1.7.05 - Fixed SMS text entry
 *	1.7.06 - Fixing private method issue caused by grails
 *	1.7.07 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.08 - Optimized isST/isHE, formatting
 *	1.7.09 - Clean up app label in sendMessage()
 *	1.7.10 - Added option to disable local display of log.debug() logs, support Notification devices on ST
 *	1.7.11 - Parameterized Home/Away selections
 *	1.7.12 - Cleaned up notifications, removed SMS for HE platform
 *`	1.7.13 - Added Customized Notifications
 *	1.7.14 - Fixed custom notifications, removed extraneous logging
 *	1.7.15 - Fixed helper labelling
 *	1.7.16 - Fixed labels (again), added infoOff, cleaned up preferences setup
 *	1.7.17 - Added minimize UI
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - Warning on Pause updated
 *	1.8.03 - More busy bees
 *	1.8.04 - Send simultaneous notification Announcements to multiple Echo Speaks devices
 *	1.8.05 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.06 - New SHPL, using Global Fields instead of atomicState
 *	1.8.07 - Fixed appDisplayName in sendMessage
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.8.07" }
String getVersionLabel() 	{ return "ecobee Suite Working From Home Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
    name: 				"ecobee Suite Working From Home",
    namespace: 			"sandood",
    author: 			"Barry A. Burke",
    description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nIf, after thermostat mode change to 'Away' and/or at a particular time of day, anyone is still at home, " +
    			 		"${ST?'trigger a \'Working From Home\' Routine (opt), ':''}, change the Location mode (opt), and/or reset thermostat(s) to 'Home' program (opt).",
    category: 			"Convenience",
    parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-working-from-home.src/ecobee-suite-working-from-home.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-working-home",
	singleInstance: 	false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
    page(name: "customNotifications")
}

// Preferences Pages
def mainPage() {
	//boolean ST = isST
	//boolean HE = !ST
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
    String defaultName = "Working From Home"
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This Helper is used to override automated Ecobee schedules for the 'Away' program when 1 or more people are Working From Home (i.e., still present after a scheduled change to 'Away'). The override can be "+
                          "configured to occur immediately when the program changes to 'Away', or at a specific time of day (e.g., 5 minutes after the scheduled change), and the target program is configurable.")
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
            	if (app.label.contains(' (paused)')) {
                	if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains(' (paused)')) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
                        String myLabel = app.label.substring(0, app.label.indexOf(' (paused)'))
                        atomicState.appDisplayName = myLabel
                        app.updateLabel(myLabel)
                    }
                } else {
                	atomicState.appDisplayName = app.label
                }
            }
        	if(settings.tempDisable) { 
				paragraph getFormat("warning","This Helper is temporarily paused...")
			} else {
        		input (name: "myThermostats", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select Ecobee Suite Thermostat(s)"),
                	   required: true, multiple: true, submitOnChange: true)
				if (HE) paragraph ''
			}
        }

		if (settings?.myThermostats && !settings?.tempDisable) {
            section (title: sectionTitle("Triggers")) {
                input(name: "people", type: "capability.presenceSensor", title: inputTitle("When any of these are present..."),  multiple: true, required: true, submitOnChange: true, width: 6)
				input(name: "onAway", type: "bool", title: inputTitle("When ${settings?.myThermostats?.size()>1?'any thermostats\'':'the thermostat\'s'} Program changes"), defaultValue: false, 
                	  required: (settings.timeOfDay == null), submitOnChange: true, width: 6)
				input(name: "timeOfDay", type: "time", title: inputTitle("At this time of day"),  required: !settings.onAway, submitOnChange: true, width: 6)
                if (settings.onAway) {
                	def programs = getEcobeePrograms()
                    programs = programs - ["Resume"]
                	input(name: 'awayPrograms', type: 'enum', title: inputTitle("When Program changes to any of these: "), options: programs, required: true, defaultValue: 'Away', multiple: true, 
                          submitOnChange: true, width: 6)
                }
				if (HE) paragraph ''
            }
            
			section( title: sectionTitle("Actions")) {
				def phrases
				if (ST) {
                    phrases = location.helloHome?.getPhrases()*.label
				}
				// Row 1
        		input(name: "setHome", type: "bool", title: inputTitle("Change thermostat${settings?.myThermostats?.size()>1?'\'s':'s\''} Program?"), defaulValue: true, 
                	  submitOnChange: true, width: 4)
                if (settings.setHome) {
                	def programs = getEcobeePrograms()
                    programs = programs - ((settings.awayPrograms?:[]) + ["Resume"])
                	input(name: 'homeProgram', type: 'enum', title: inputTitle("Change Program to: "), options: programs, required: true, defaultValue: 'Home', multiple: false, 
                          submitOnChange: true, width: 4)
					if (settings?.homeProgram == null) { app.updateSetting('homeProgram', 'Home'); settings?.homeProgram = 'Home'; }
					input(name: "setMode", type: "mode", title: inputTitle("Set Location Mode"), required: false, multiple: false, submitOnChange: true, width: 4)
					
					// Row 2
                    if (HE) paragraph("",width: 4)
					input(name: "holdType", title: inputTitle("Hold Type for Program")+" (optional)", type: "enum", required: false, 
						  multiple: false, submitOnChange: true, defaultValue: "Ecobee Manager Setting",
						  options:["Until I Change", "Until Next Program", "2 Hours", "4 Hours", "Custom Hours", "Thermostat Setting", 
												"Ecobee Manager Setting"], width: 4)
                    if (phrases) {	// SmartThings only
                        phrases.sort()
                        input(name: "wfhPhrase", type: "enum", title: inputTitle("Run this Routine"), required: false, options: phrases, submitOnChange: true)
                    }
					
					// Row 3
					if (settings?.holdType == 'Until Next Program') {
						//if (HE) paragraph("",width: 4)
						if (maximize) paragraph("The '${settings?.homeProgram}' hold will be a temporary hold", width: 8)
					} else if (settings?.holdType == 'Until I Change') {
						//if (HE) paragraph("",width: 4)
						if (maximize) paragraph("The '${settings?.homeProgram}' hold will be a permanent hold", width: 8)
					} else if (settings.holdType=='Thermostat Setting') {
						//if (HE) paragraph("",width: 4)
						if (maximize) paragraph("Thermostat Setting at the time of the '${settings?.homeProgram}' hold request will be applied", width: 8)
					} else if ((settings.holdType == null) || (settings.holdType == 'Ecobee Manager Setting') || (settings.holdType == 'Parent Ecobee (Connect) Setting')) {
						//if (HE) paragraph("",width: 4)
						if (maximize) paragraph("Ecobee Manager Setting at the time of the '${settings?.homeProgram}' hold request will be applied", width: 8)
					} else if (settings?.holdType == '2 Hours') {
						//if (HE) paragraph("",width: 4)
						if (maximize) aragraph("The '${settings?.homeProgram}' hold request will be for 2 hours", width: 8)
					} else if (settings?.holdType == '4 Hours') {
						//if (HE) paragraph("",width: 4)
						if (maximize) paragraph("The '${settings?.homeProgram}' hold request will be for 4 hours", width: 8)
					} else 	if ((settings.holdType=="Specified Hours") || (settings?.holdType == 'Custom Hours')) {
						if (HE) paragraph("",width: 4)
						input(name: 'holdHours', title: inputTitle('How many hours (1-48)?'), type: 'number', range:"1..48", required: true, description: '2', defaultValue: 2,
							  submitOnChange: true, width: 4)
						if (HE) paragraph("",width: 4)
						if (maximize) paragraph("The '${settings?.homeProgram}' hold request will be for ${holdHours} hours", width: 8)
					} 
				} else {
					input(name: "setMode", type: "mode", title: inputTitle("Set Location Mode")+' (optional)', required: false, multiple: false, submitOnChange: true, width: 4)
					if (phrases) {	// SmartThings only
                        phrases.sort()
                        input(name: "wfhPhrase", type: "enum", title: inputTitle("Run this Routine")+' (optional)', required: false, options: phrases, submitOnChange: true)
                    }
				}
            }
                
            section (title: sectionTitle("Conditions")) {
				input(name: 'statMode', title: inputTitle('Only when thermostat mode is'), type: 'enum', required: false, multiple: true, 
                    		options:getThermostatModes(), submitOnChange: true, width: 4)
                input(name: "days", type: "enum", title: inputTitle("Only on certain days of the week"), multiple: true, required: false, submitOnChange: true,
                    options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"], width: 4)
                input(name: "modes", type: "mode", title: inputTitle("Only when Location Mode is"), multiple: true, required: false, submitOnChange: true, width: 4)
				// paragraph HE ? "A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken\n" : "A notification is always sent to the Hello Home log whenever an action is taken\n"
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
                if (maximize) {
					section() {
						paragraph ( "A notification is always sent to the Hello Home log whenever an action is taken")
					}
                }
			} else {		// HE
				section(sectionTitle("Notifications")) {
					input(name: "notify", type: "bool", title: inputTitle("Notify on Actions?"), required: true, defaultValue: false, submitOnChange: true, width: 4)
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
					section("A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken"){}
                }
			}
            if ((settings?.notify) && (settings?.pushNotify || settings?.phone || settings?.notifiers || (settings?.speak &&(settings?.speechDevices || settings?.musicDevices)))) {
                section() {
                    href name: "customNotifications", title: inputTitle("Customize Notifications"), page: "customNotifications", 
                         description: "Customize notification messages", state: isCustomized()
                }
			}				
        }
        section(title: sectionTitle("Operations")) {
        	input(name: "minimize", 	title: inputTitle("Minimize settings text"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
           	input(name: "tempDisable", 	title: inputTitle("Pause this Helper"), 		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)                
			input(name: "debugOff",	 	title: inputTitle("Disable debug logging"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
            input(name: "infoOff", 		title: inputTitle("Disable info logging"), 		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
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
	//oolean ST = isST
	//boolean HE = !ST
	dynamicPage(name: "customNotifications", title: pageTitle("${defaultName} Custom Notifications"), uninstall: false, install: false) {
    	section(sectionTitle("Customizations") + (ST ? "\n" : '') + smallerTitle("Notification Prefix")){
			input(name: "customPrefix", type: "enum", title: inputTitle("Notification Prefix text:"), defaultValue: "(helper) at (location):", required: false, submitOnChange: true, 
				  options: ['(helper):', '(helper) at (location):', '(location):', 'none', 'custom'], multiple: false)
			if (settings?.customPrefix == null) { app.updateSetting('customPrefix', '(helper) at (location):'); settings.customPrefix = '(helper) at (location):'; }
			if (settings.customPrefix == 'custom') {
				input(name: "customPrefixText", type: "text", title: inputTitle("Custom Prefix text"), defaultValue: "", required: true, submitOnChange: true)
			}
        }
        section(smallerTitle("Explanation")) {
        	input(name: "identify", type: 'bool', title: inputTitle('Identify who is home for logs & notifications?'), required: (settings.people != null), defaultValue: false, submitOnChange: true)
            input(name: 'customBecause', type: "enum", title: inputTitle('Explanation text:'), required: true, defaultValue: 'still home', submitOnChange: true, multiple: false, 
            	  options: ['still here', 'still home', 'still present', 'home', 'at home', 'here', 'working from home', 'working from home today','present'].sort(false) + ['custom'])
        	if (settings?.customBecause == 'custom') {
            	input(name: 'customBecauseText', type: 'text', title: inputTitle("Custom Explanation text"), defaultValue: "", required: true, submitOnChange: true)
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
			String thePrefix = getMsgPrefix()
			String theTstat = getMsgTstat()
			String samples = ""
            String who = whoIsHome()
            def tc = myThermostats.size()
            boolean multiple = false
            
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
			paragraph samples
		}
	}
}

def isCustomized() {
	return (customPrefix || customTstat || (useSensorNames != null)) ? "complete" : null
}

void installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
    initialize()
}
void updated() {
    unsubscribe()
    unschedule()
    LOG("Updated with settings ${settings}", 4, null, 'trace')
    initialize()
//    checkPresence()
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, null, 'info')
	updateMyLabel()
	
    if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
    if (settings.debugOff) log.info "Debug logging disabled"
    if (settings.infoOff) log.info "Info logging disabled"
	
    if (settings.timeOfDay != null) schedule(timeToday(settings.timeOfDay, location.timeZone), "checkPresence")
    if (settings.onAway) subscribe(settings.myThermostats, "currentProgram", "checkProgram")
}

def checkPresence() {
	LOG("Check presence", 4, null, 'trace')
	//boolean ST = isST
	
    if (anyoneIsHome() && getDaysOk() && getModeOk() && getStatModeOk()) {
    	def multiple = false
		LOG("Someone is present", 2, null, 'trace')
        if (ST && wfhPhrase) {
            location.helloHome.execute(wfhPhrase)
        	LOG("Executed ${wfhPhrase}", 4, null, 'trace')
			def who = whoIsHome()
			sendMessage("I executed '${wfhPhrase}' because ${who} ${becaueText()}")
            multiple = true
        }
        if (settings.setMode) {
        	location.setMode(settings.setMode)
            sendMessage("I ${multiple?'also ':''}changed Location Mode to ${settings.setMode}")
            multiple = true
        }
        if (settings.setHome) {
			def verified = true
            String homeTarget = settings.homeProgram ?: 'Home'
        	myThermostats.each { tstat ->
				String currentProgram = ST ? tstat.currentValue('currentProgram') : tstat.currentValue('currentProgram', true)
				if (currentProgram && (currentProgram != homeTarget)) {
               		def sendHoldType = whatHoldType(tstat)
                	def sendHoldHours = null
                	if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
                    	sendHoldHours = sendHoldType
                    	sendHoldType = 'holdHours'
                	}
                    LOG("${app.label} checkPresence(): calling setThermostatProgram(${homeTarget}, ${sendHoldType}, ${sendHoldHours})",2,null,'info')
                    tstat.setThermostatProgram(homeTarget, sendHoldType, sendHoldHours)
					verified = false
				}
        	}
            def tc = myThermostats.size()
            def also = multiple ? 'also ' : ''
			def who = whoIsHome()
			if (verified) {
				sendMessage("I ${also}verified that ${getMsgTstat()} ${tc>1?'are':'is'} set to the '${settings.homeProgram}' program because ${who} ${becauseText(who)}")
			} else {
				sendMessage("I ${also}changed ${getMsgTstat()} to the '${settings.homeProgram}' program because ${who} ${becauseText(who)}")
				runIn(300, checkHome, [overwrite: true])
			}
        }
    }
}

def checkProgram(evt) {
	LOG("Check program: ${evt.device.displayName} changed to ${evt.value}", 4, null, 'trace')
    //boolean ST = isST
	
    def multiple = false
    if (settings.onAway && (settings.awayPrograms.contains(evt.value)) && anyoneIsHome() && getDaysOk() && getModeOk() && getStatModeOk()) {
    	checkHome()
		def who = whoIsHome()
        sendMessage("I reset ${getMsgTstat()} to the '${settings.homeProgram}' program because Thermostat ${evt.device.displayName} changed to '${evt.value}' and ${who} ${becauseText(who)}")
        runIn(300, checkHome, [overwrite: true])
        
    	if (ST && wfhPhrase) {
            location.helloHome.execute(wfhPhrase)
        	LOG("Executed ${wfhPhrase}", 4, null, 'trace')
			sendMessage("I also executed '${wfhPhrase}'")
        }
    	if (settings.setMode) {
        	location.setMode(settings.setMode)
            sendMessage("And I changed Location Mode to ${settings.setMode}")
            multiple = true
        } 
    }
}

def checkHome() {
	//boolean ST = isST

	if (settings.setHome) {
    	String homeTarget = settings.homeProgram ?: 'Home'
    	myThermostats.each { tstat ->
			String  currentProgram = ST ? tstat.currentValue('currentProgram') : tstat.currentValue('currentProgram', true)
        	if (currentProgram && (currentProgram != homeTarget)) { 	// Need to check if in Vacation Mode also...
                def sendHoldType = whatHoldType(tstat)
                def sendHoldHours = null
                if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
                    sendHoldHours = sendHoldType
                    sendHoldType = 'holdHours'
                }
                LOG("${app.label} checkHome(): calling setThermostatProgram(${homeTarget}, ${sendHoldType}, ${sendHoldHours})",2,null,'info')
                tstat.setThermostatProgram(homeTarget, sendHoldType, sendHoldHours)
            }
        }
    }
}

boolean anyoneIsHome() {
  def result = false

  if (settings.people.findAll { it?.currentPresence == "present" }) {
    result = true
  }
  LOG("anyoneIsHome: ${result}", 4, null, 'trace')
  return result
}

String whoIsHome() {
	if (!settings.identify) return "somebody"
	
	String names = ""
	settings.people.each {
		if (it.currentPresence == 'present') {
			names = (names == "") ? it.displayName : (names.contains(it.displayName) ? names : names + ", ${it.displayName}")
		}
	}
	if (names != "") {
		if (names.contains(', ')) {
			int comma = names.lastIndexOf(', ')
			String front = names.substring(0, comma)
			String tail = names.substring(comma+2)
			return front + ' and ' + tail
		} else {
			return names
		}
	} else {
		return "nobody"
	}
}

String becauseText(who) {
	if (settings?.customBecause == null) { app.updateSetting('customBecause', 'still home'); settings.customBecause = 'still home'; }
    String reason = settings?.customBecause == 'custom' ? settings?.customBecauseText : settings?.customBecause
	return (who.contains(' and ')?'are ':'is ') + reason
}

// get the combined set of Ecobee Programs applicable for these thermostats
def getEcobeePrograms() {
	def programs
	if (settings.myThermostats?.size() > 0) {
		settings.myThermostats.each { stat ->
			def pl = stat.currentValue('programsList')
			if (!programs) {
				if (pl) programs = new JsonSlurper().parseText(pl)
			} else {
				if (pl) programs = programs.intersect(new JsonSlurper().parseText(pl))
			}
        }
	} 
	if (!programs) programs =  ['Away', 'Home', 'Sleep']
    LOG("getEcobeePrograms: returning ${programs}", 4, null, 'info')
    return programs.sort(false)
}

// return all the modes that ALL thermostats support
def getThermostatModes() {
	def theModes = []
    
    settings.myThermostats?.each { stat ->
    	if (theModes == []) {
        	theModes = stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", ")
        } else {
        	theModes = theModes.intersect(stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", "))
        }   
    }
    return theModes.sort(false)
}

boolean getStatModeOk() {
	if (settings.statMode == null) return true
	//boolean ST = isST
	boolean result = false
	settings.myThermostats?.each { stat ->
		def statMode = ST ? stat.currentValue('thermostatMode') : stat.currentValue('thermostatMode', true)
		//log.debug "statMode: ${statMode}"
		if (settings.statMode.contains(statMode)) {
			//log.debug "statModeOk"
			result = true
		}
	}
	LOG("statModeOk: ${result}", 4, null, 'trace')
	return result
}

boolean getModeOk() {
    boolean result = (!modes || modes.contains(location.mode))
    LOG("modeOk: ${result}", 4, null, 'trace')
	return result
}

boolean getDaysOk() {
    boolean result = true
    if (settings.days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = settings.days.contains(day)
    }
	LOG("daysOk: ${result}", 4, null, 'trace')
    return result
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
		case 'Custom Hours':
            if (settings.holdHours && settings.holdHours.isNumber()) {
            	sendHoldType = settings.holdHours
            } else if (((parentHoldType == 'Specified Hours') || (parentHoldType == 'Custom Hours')) && ((parentHoldHours != null) && parentHoldHours.isNumber())) {
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

String getMsgTstat() {						
	String theTstat = ""
	if (settings?.customTstat == null) { app.updateSetting('customTstat', '(thermostat names)'); settings?.customTstat = '(thermostat names)'; }
	switch (settings.customTstat) {
		case 'custom':
			theTstat = settings.customTstatText 
			break
		case "(thermostat names)":
			def stats = settings?.theThermostats ?: myThermostats
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
					if ((!settings?.tstatSuffix || (settings.tstatSuffix != it)) && (!settings?.tstatPrefix || (settings.tstatPrefix != it))) {	// Don't strip the prefix/suffix we added above
						statStr = statStr.replace(it, '').replace(it.toLowerCase(), '')	// Strip out any unnecessary words
					}
				}
			}
			statStr = statStr.replace(':','').replace('  ', ' ').trim()		// Finally, get rid of any double spaces
			theTstat = prefix + statStr + suffix 	// (statStr + ((stats?.size() > 1) ? ' are' : ' is'))	
			break
		case 'the HVAC system':
			theTstat = 'the H V A C system'
			break
        case 'the thermostat':
        	def stats = settings?.theThermostats ?: myThermostats
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
	//boolean ST = isST
    
	String flag = ST ? ' (paused)' : '<span '
	
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (myLabel.contains(flag)) {
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	if (settings.tempDisable) {
		def newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
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
def hideOptions() {
    return (settings.days || settings.modes) ? false : true
}
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
