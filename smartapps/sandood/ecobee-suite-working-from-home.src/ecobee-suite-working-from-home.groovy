/**
 *  Ecobee Suite Working From Home
 *
 *	Copyright 2017 Barry A. Burke
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
 */
import groovy.json.*
String getVersionNum() { return "1.7.15" }
String getVersionLabel() { return "ecobee Suite Working From Home Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
    name: 			"ecobee Suite Working From Home",
    namespace: 		"sandood",
    author: 		"Barry A. Burke",
    description: 	"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nIf, after thermostat mode change to 'Away' and/or at a particular time of day, anyone is still at home, " +
    			 	"${isST?'trigger a \'Working From Home\' Routine (opt), ':''}, change the Location mode (opt), and/or reset thermostat(s) to 'Home' program (opt).",
    category: 		"Convenience",
    parent: 		"sandood:Ecobee Suite Manager",
	iconUrl: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
    importUrl:		"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-working-from-home.src/ecobee-suite-working-from-home.groovy",
	singleInstance: false,
    pausable: 		true
)

preferences {
	page(name: "mainPage")
    page(name: "customNotifications")
}

// Preferences Pages
def mainPage() {
	boolean ST = isST
	boolean HE = !ST
	
	dynamicPage(name: "mainPage", title: (HE?'<b>':'') + "${getVersionLabel()}" + (HE?'</b>':''), uninstall: true, install: true) {
		section("") {
        	String defaultName = "Working From Home"
			String defaultLabel = atomicState?.appDisplayName ?: defaultName
			String oldName = app.label
			input "thisName", "text", title: "Name for this ${defaultName} Helper", submitOnChange: true, defaultValue: defaultLabel
			if ((!oldName && settings.thisName) || (oldName && settings.thisName && (settings.thisName != oldName))) {
				app.updateLabel(thisName)
				atomicState.appDisplayName = thisName
			} else if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			}
			updateMyLabel()
			if (HE) {
				if (app.label.contains('<span ')) {
					if (atomicState?.appDisplayName != null) {
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
                	if (atomicState?.appDisplayName != null) {
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
            updateMyLabel()
        	if(settings.tempDisable == true) {
            	paragraph "WARNING: Temporarily Paused - re-enable below."
            } else {
        		input (name: "myThermostats", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Ecobee Thermostat(s)",
                	   required: true, multiple: true, submitOnChange: true)
				if (HE) paragraph ''
			}
        }

		if (settings?.myThermostats && !settings?.tempDisable) {
            section (title: (HE?'<b>':'') + "Conditions" + (HE?'</b>':'')) {
                input(name: "people", type: "capability.presenceSensor", title: "When any of these are present...",  multiple: true, required: true, submitOnChange: true)
				input(name: "timeOfDay", type: "time", title: "At this time of day",  required: !settings.onAway, submitOnChange: true)
				input(name: "onAway", type: "bool", title: "When ${settings?.myThermostats?.size()>1?'any thermostats\'':'the thermostat\'s'} Program changes", defaultValue: false, 
                	  required: (settings.timeOfDay == null), submitOnChange: true)
                if (settings.onAway) {
                	def programs = getEcobeePrograms()
                    programs = programs - ["Resume"]
                	input(name: 'awayPrograms', type: 'enum', title: "When Program changes to any of these: ", options: programs, required: true, defaultValue: 'Away', multiple: true, 
                          submitOnChange: true)
                }
				if (HE) paragraph ''
            }
            
			section( title: (HE?'<b>':'') + "Actions" + (HE?'</b>':'')) {
                if (ST) {
                    def phrases = location.helloHome?.getPhrases()*.label
                    if (phrases) {
                        phrases.sort()
                        input(name: "wfhPhrase", type: "enum", title: "Run this Routine", required: false, options: phrases, submitOnChange: true)
                    }
            	}
                input(name: "setMode", type: "mode", title: "Set Location Mode", required: false, multiple: false, submitOnChange: true)
        		input(name: "setHome", type: "bool", title: "Change thermostat${settings?.myThermostats?.size()>1?'\'s':'s\''} Program?", defaulValue: true, 
                	  submitOnChange: true)
                if (settings.setHome) {
                	def programs = getEcobeePrograms()
                    programs = programs - ((settings.awayPrograms?:[]) + ["Resume"])
                	input(name: 'homeProgram', type: 'enum', title: "Change Program to: ", options: programs, required: true, defaultValue: 'Home', multiple: false, 
                          submitOnChange: true)
                }
				if (HE) paragraph ''
            }
                
            section (title: (HE?'<b>':'') + "Advanced Options" + (HE?'</b>':'')) {
				input(name: 'statMode', title: 'Only when thermostat mode is', type: 'enum', required: false, multiple: true, 
                    		options:getThermostatModes(), submitOnChange: true)
                input(name: "days", type: "enum", title: "Only on certain days of the week", multiple: true, required: false, submitOnChange: true,
                    options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"])
                input(name: "modes", type: "mode", title: "Only when Location Mode is", multiple: true, required: false, submitOnChange: true)
				input(name: "notify", type: "bool", title: "Notify on Actions?", required: true, defaultValue: false, submitOnChange: true)
				// paragraph HE ? "A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken\n" : "A notification is always sent to the Hello Home log whenever an action is taken\n"
            }
			
			if (settings.notify) {
				if (ST) {
                    section("Notifications") {
                        paragraph "A notification will also be sent to the Hello Home log"
                        input(name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, required: true, submitOnChange: true)
                        input(name: "notifiers", type: "capability.notification", title: "Send Notifications to these devices", multiple: true, description: "Select notification devices", 
                              required: (!settings.pushNotify && (settings.phone == null) && (!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null)))),
                              submitOnChange: true, hideWhenEmpty: true)
                        input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777; +441234567890)", 
                              required: (!settings.pushNotify && (settings.notifiers == null) && (!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null)))),
                              submitOnChange: true)
                        input(name: "speak", type: "bool", title: "Spoken Notifications?", required: true, defaultValue: false, submitOnChange: true, hideWhenEmpty: (!"speechDevices" && !"musicDevices"))
                        if (settings.speak) {
                            input(name: "speechDevices", type: "capability.speechSynthesis", title: "Using these speech devices", multiple: true, 
                                  required: (!settings.pushNotify && (settings.notifiers == null) && (settings.phone == null) && (settings.musicDevices == null)), 
                                  submitOnChange: true, hideWhenEmpty: true)
                            input(name: "musicDevices", type: "capability.musicPlayer", title: "Using these music devices", multiple: true, 
                                  required: (!settings.pushNotify && (settings.notifiers == null) && (settings.phone == null) && (settings.speechDevices == null)), 
                                  submitOnChange: true, hideWhenEmpty: true)
                            if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: false)
                        }
                        if (!settings.phone && !settings.pushNotify && !settings.speechDevices && !settings.musicDevices && !settings.notifiers) paragraph "WARNING: Notifications configured, but nowhere to send them!\n"
                    }
                } else {		// HE
                    section("<b>Use Notification Device(s)</b>") {
                        input(name: "notifiers", type: "capability.notification", title: "Send Notifications to these devices", multiple: true, submitOnChange: true,
                              required: (!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null))))
                        paragraph ""
                    }
                    section(hideWhenEmpty: (!"speechDevices" && !"musicDevices"), "<b>Use Speech Device(s)</b>") {
                        input(name: "speak", type: "bool", title: "Speak messages?", required: !settings?.notifiers, defaultValue: false, submitOnChange: true)
                        if (settings.speak) {
                            input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "Using these speech devices", 
                                  multiple: true, submitOnChange: true)
                            input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "Using these music devices", 
                                  multiple: true, submitOnChange: true)
                            input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
                        }
                    }
                    section(){
                        paragraph "A 'HelloHome' notification will also be sent to the Location Event log"
                    }
                }
                if ((settings?.notify) && (settings?.pushNotify || settings?.phone || settings?.notifiers || (settings?.speak &&(settings?.speechDevices || settings?.musicDevices)))) {
					section() {
						href name: "customNotifications", title: (HE?'<b>':'') + "Customize Notifications" + (HE?'</b>':''), page: "customNotifications", 
							 description: "Customize notification messages", state: isCustomized()
					}
                }
			}				
        }
        section(title: (HE?'<b>':'') + "Temporary Pause" + (HE?'</b>':'')) {
           	input(name: "tempDisable", title: "Pause this Helper?", type: "bool", required: false, description: "", submitOnChange: true)                
        }
		section(title: "") {
			input(name: "debugOff", title: "Disable debug logging? ", type: "bool", required: false, defaultValue: false, submitOnChange: true)
		}
		section (getVersionLabel()) {}
    }
}

def customNotifications(){
	boolean ST = isST
	boolean HE = !ST
	dynamicPage(name: "customNotifications", title: (HE?'<b>':'') + "${getVersionLabel()}" + (HE?'</b>':''), uninstall: false, install: false) {
		section((HE?'<b>':'') + "Custom Explanation" + (HE?'</b>':'')) {
        	input(name: "identify", type: 'bool', title: 'Identify who is home for logs & notifications?', required: (settings.people != null), defaultValue: false, submitOnChange: true)
            input(name: 'customBecause', type: "enum", title: 'Explanation text:', required: true, defaultValue: 'still home', submitOnChange: true, multiple: false, 
            	  options: ['still here', 'still home', 'still present', 'home', 'at home', 'here', 'working from home', 'working from home today','present'].sort(false) + ['custom'])
        	if (settings?.customBecause == 'custom') {
            	input(name: 'customBecauseText', type: 'text', title: "Custom Explanation text", defaultValue: "", required: true, submitOnChange: true)
            }
        }
        section((HE?'<b>':'') + "Custom Notification Prefix" + (HE?'</b>':'')){
			input(name: "customPrefix", type: "enum", title: "Notification Prefix text:", defaultValue: "(helper) at (location):", required: false, submitOnChange: true, 
				  options: ['(helper):', '(helper) at (location):', '(location):', 'none', 'custom'], multiple: false)
			if (settings?.customPrefix == null) { app.updateSetting('customPrefix', '(helper) at (location):'); settings.customPrefix = '(helper) at (location):'; }
			if (settings.customPrefix == 'custom') {
				input(name: "customPrefixText", type: "text", title: "Custom Prefix text", defaultValue: "", required: true, submitOnChange: true)
			}
        }
        section((HE?'<b>':'') + "Custom Thermostat Identification" + (HE?'</b>':'')) {
			input(name: "customTstat", type: "enum", title: "Refer to the HVAC system as", defaultValue: "(thermostat names)", options:
				  ['the thermostat', 'the HVAC system', '(thermostat names)', 'custom'], submitOnChange: true, multiple: false)
			if (settings?.customTstat == 'custom') {
				input(name: "customTstatText", type: "text", title: "Custom HVAC system text", defaultValue: "", required: true, submitOnChange: true)
			} 
			if (settings?.customTstat == null) { app.updateSetting('customTstat', '(thermostat names)'); settings.customTstat = '(thermostat names)'; }
			if (settings?.customTstat == '(thermostat names)') {
				input(name: "tstatCleaners", type: 'enum', title: "Strip these words from the Thermostat display names", multiple: true, required: false,
					  submitOnChange: true, options: ['EcobeeTherm', 'EcoTherm', 'Thermostat', 'Ecobee'].sort(false))
				input(name: "tstatPrefix", type: 'enum', title: "Add this prefix to the Thermostat display names", multiple: false, required: false,
					  submitOnChange: true, options: ['the', 'Ecobee', 'thermostat', 'Ecobee thermostat', 'the Ecobee', 'the Ecobee thermostat', 'the thermostat'].sort(false)) 
				input(name: "tstatSuffix", type: 'enum', title: "Add this suffix to the Thermostat display names", multiple: false, required: false,
					  submitOnChange: true, options: ['Ecobee', 'HVAC', 'HVAC system', 'thermostat'])
			}
        }
		section("${HE?'<b><i>':''}Sample notification message(s):${HE?'</i></b>':''}") {
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
    if (settings.debugOff) log.info "log.debug() logging disabled"
	
    if (settings.timeOfDay != null) schedule(timeToday(settings.timeOfDay, location.timeZone), "checkPresence")
    if (settings.onAway) subscribe(settings.myThermostats, "currentProgram", "checkProgram")
}

def checkPresence() {
	LOG("Check presence", 4, null, 'trace')
	boolean ST = atomicState.isST
	
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
            def homeTarget = settings.homeProgram ?: 'Home'
        	myThermostats.each { tstat ->
				def currentProgram = ST ? tstat.currentValue('currentProgram') : tstat.currentValue('currentProgram', true)
				if (currentProgram != homeTarget) {
					// tstat.home()
                    tstat.setThermostatProgram(homeTarget)
					verified = false
				}
        	}
            def tc = myThermostats.size()
            def also = multiple ? 'also ' : ''
			def who = whoIsHome()
			if (verified) {
				sendMessage("I ${also} verified that ${getMsgTstat()} ${tc>1?'are':'is'} set to the '${settings.homeProgram}' program because ${who} ${becauseText(who)}")
			} else {
				sendMessage("I ${also} changed ${getMsgTstat()} to the '${settings.homeProgram}' program because ${who} ${becauseText(who)}")
				runIn(300, checkHome, [overwrite: true])
			}
        }
    }
}

def checkProgram(evt) {
	LOG("Check program: ${evt.device.displayName} changed to ${evt.value}", 4, null, 'trace')
    boolean ST = atomicState.isST
	
    def multiple = false
    if (settings.onAway && (settings.awayPrograms.contains(evt.value)) && anyoneIsHome() && getDaysOk() && getModeOk() && getStatModeOk()) {
    	evt.device.home()
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
	boolean ST = atomicState.isST
	boolean allSet = true
	if (settings.setHome) {
    	def homeTarget = settings.homeProgram ?: 'Home'
    	myThermostats.each { tstat ->
			def currentProgram = ST ? tstat.currentValue('currentProgram') : tstat.currentValue('currentProgram', true)
        	if (currentProgram != homeTarget) { 	// Need to check if in Vacation Mode also...
            	allSet = false
            	// tstat.home()
                tstat.setThermostatProgram(homeTarget)
                LOG("${app.label} at ${location.name} failed twice to set '${homeTarget}' program on ${tstat.displayName}",2,null,'warn')
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
	boolean ST = atomicState.isST
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
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "trace")
    if (settings.notify) {
    	String msg = getMsgPrefix() + notificationMessage
        //String msg = "${atomicState.appDisplayName} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (atomicState.isST) {
			if (settings.notifiers != null) {
				settings.notifiers.each {							// Use notification devices on Hubitat
					it.deviceNotification(msg)
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
		} else {		// isHE
			if (settings.notifiers != null) {
				settings.notifiers.each {							// Use notification devices on Hubitat
					it.deviceNotification(msg)
				}
			}
		/*	if (settings.phone != null) {
				if ( settings.phone.indexOf(",") > 0){
					def phones = phone.split(",")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i].trim(), msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone.trim(), msg)						// Only to SMS contact
				}
			} */
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
			
		}
    }
	// Always send to Hello Home / Location Event log
	if (atomicState.isST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
}

void updateMyLabel() {
	boolean ST = atomicState.isST
    
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
	String msg = "${atomicState.appDisplayName} ${message}"
    if (logType == null) logType = 'debug'
    log."${logType}" message
	parent.LOG(msg, level, null, logType, event, displayEvent)
}
// SmartThings/Hubitat Portability Library (SHPL)
String  getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST()     { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
boolean getIsHE()     { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
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
