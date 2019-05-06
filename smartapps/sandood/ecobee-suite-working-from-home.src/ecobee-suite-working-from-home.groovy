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
 * <snip>
 * 	1.7.00 - Initial Release
 */
def getVersionNum() { return "1.7.00d" }
private def getVersionLabel() { return "ecobee Suite Working From Home Helper,\nversion ${getVersionNum()} on ${getHubPlatform()}" }

definition(
    name: "ecobee Suite Working From Home",
    namespace: "sandood",
    author: "Barry A. Burke",
    description: "If, after thermostat mode change to 'Away' and/or at a particular time of day, anyone is still at home, " +
    			 "${isST?'trigger a \'Working From Home\' Routine (opt), ':''}, change the Location mode (opt), and/or reset thermostat(s) to 'Home' program (opt).",
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

// Preferences Pages
def mainPage() {
	dynamicPage(name: "mainPage", title: "${getVersionLabel()}", uninstall: true, install: true) {
		section(title: "Name for this Working Fom Home Helper") {
        	label title: "Name this Helper", required: true, defaultValue: "Working From Home"
        }
        
        section(title: "Select Thermostats") {
        	if(settings.tempDisable == true) {
            	paragraph "WARNING: Temporarily Disabled as requested. Turn back on below to activate handler."
            } else {
        		input (name: "myThermostats", type: "${isST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Pick Ecobee Thermostat(s)", description: "Tap to choose...", 
                	   required: true, multiple: true, submitOnChange: true)            
			}
        }

		if ((settings?.myThermostats?.size() > 0) && (settings.tempDisable == false)) {
            section (title: "Select Presence Sensors") {
                input(name: "people", type: "capability.presenceSensor", title: "When any of these are present...", description: "Tap to choose...", multiple: true, required: true, submitOnChange: true)
                input(name: "onAway", type: "bool", title: "When thermostat(s) change to 'Away'", defaultValue: false, required: (settings.timeOfDay == null), submitOnChange: true)
                input(name: "timeOfDay", type: "time", title: "At this time of day", description: "Tap to choose...", required: (settings.onAway == null), submitOnChange: true)
            }
            
			section( title: "Perform these actions") {
                if (isST) {
                    def phrases = location.helloHome?.getPhrases()*.label
                    if (phrases) {
                        phrases.sort()
                        input(name: "wfhPhrase", type: "enum", title: "Run this Routine", description: "Tap to choose...", required: false, options: phrases, submitOnChange: true)
                    }
            	}
                input(name: "setMode", type: "mode", title: "Set location mode", description: "Tap to choose...", required: false, multiple: false, submitOnChange: true)
        		input(name: "setHome", type: "bool", title: "Set thermostat${myThermostats.size()>1?'s':''} to the 'Home' schedule/program?", defaulValue: true, submitOnChange: true)
            }
                

            section (title: "Advanced options", hidden: hideOptions(), hideable: true) {
                input(name: "days", type: "enum", title: "Only on certain days of the week", description: "Tap to choose...", multiple: true, required: false, submitOnChange: true,
                    options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"])
                input(name: "modes", type: "mode", title: "Only when Location Mode is", description: "Tap to choose...", multiple: true, required: false, submitOnChange: true)
                if (isST) input(name: "sendPushMessage", type: "enum", title: "Send a push notification?", options:["Yes","No"], required: false, submitOnChange: true)
                input(name: "phone", type: "phone", title: "Send a Text Message?", required: false, description: "Tap to choose...", submitOnChange: true)
            }
        }
        section(title: "Temporarily Disable?") {
           	input(name: "tempDisable", title: "Temporarily disable this Helper? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
		section (getVersionLabel()) {}
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    LOG("Updated with settings ${settings}", 2, null, 'info')
    initialize()
//    checkPresence()
}

def initialize() {
	LOG("${getVersionLabel()}\nInitializing...", 2, null, 'info')
    if(tempDisable == true) {
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }
    if (settings.timeOfDay != null) schedule(timeToday(settings.timeOfDay, location.timeZone), "checkPresence")
    if (settings.onAway) subscribe(settings.theThermostats, "currentProgram", "checkProgram")
}

def checkPresence() {
	LOG("Check presence", 4, null, 'trace')
    if (anyoneIsHome() && getDaysOk() && getModeOk()) {
    	def multiple = false
		LOG("Someone is present", 4, null, 'trace')
        if (isST && wfhPhrase) {
            location.helloHome.execute(wfhPhrase)
        	LOG("Executed ${wfhPhrase}", 4, null, 'trace')
			send("${app.label} at ${location.name} executed '${wfhPhrase}' because someone is still home.")
            multiple = true
        }
        if (settings.setMode) {
        	location.setMode(settings.setMode)
            send("${app.label} ${multiple?'also ':''}changed Location Mode to ${settings.setMode} because someone is still home.")
            multiple = true
        }
        if (settings.setHome) {
        	myThermostats.each { tstat ->
        		if (tstat.currentValue('currentProgram') != "Home") tstat.home()
        	}
            def tc = myThermostats.size()
            def also = multiple ? 'also ' : ''
            send("${app.label} at ${location.name} ${also}verified that thermostat${tc>1?'s':''} ${myThermostats.toString()[1..-2]} ${tc>1?'are':'is'} set to the 'Home' program because someone is still home.")
            runIn(300, checkHome, [overwrite: true])
        }
    }
}

def checkProgram(evt) {
	LOG("Check program: ${evt.device.displayName} changed to ${evt.value}", 4, null, 'trace')
    
    def multiple = false
    if (settings.onAway && (evt.value == 'Away') && anyoneIsHome() && daysOk && modeOk) {
    	evt.device.home()
        send("Thermostat ${evt.device.displayName} changed to 'Away', so ${app.label} at ${location.name} reset thermostat${tc>1?'s':''} ${myThermostats.toString()[1..-2]} to the 'Home' program because someone is still home.")
        runIn(300, checkHome, [overwrite: true])
        
    	if (isST && wfhPhrase) {
            location.helloHome.execute(wfhPhrase)
        	LOG("Executed ${wfhPhrase}", 4, null, 'trace')
			send("${app.label} also executed '${wfhPhrase}' because someone is still home.")
        }
    	if (settings.setMode) {
        	location.setMode(settings.setMode)
            send("${app.label} also changed Location Mode to ${settings.setMode} because someone is still home.")
            multiple = true
        } 
    }
}

def checkHome() {
	def allSet = true
	if (settings.setHome) {
    	myThermostats.each { tstat ->
        	if (tstat.currentValue('currentProgram') != "Home") { 	// Need to check if in Vacation Mode also...
            	allSet = false
            	tstat.home()
                LOG("${app.label} at ${location.name} failed twice to set 'Home' program on ${tstat.displayName}",2,null,'warn')
            }
        }
    }
}

private anyoneIsHome() {
  def result = false

  if (settings.people.findAll { it?.currentPresence == "present" }) {
    result = true
  }

  LOG("anyoneIsHome: ${result}", 4, null, 'trace')

  result
}

private send(msg) {
    if (isST && sendPushMessage != "No") {
        if (isST) sendPush(msg)
    }

    if (phone) {
        sendSms(phone, msg)
    }

    LOG(msg, 2, null, 'info')
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    result
}

private getDaysOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    result
}

private hideOptions() {
    (days || modes)? false: true
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	def messageLbl = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	if (parent) parent.LOG(messageLbl, level, null, logType, event, displayEvent)
    log."${logType}" message
}

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
