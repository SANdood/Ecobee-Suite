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
def getVersionNum() { return "1.7.00rc2" }
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
		section("") {
        	label title: "Name for this Working Fom Home Helper", required: true, defaultValue: "Working From Home", submitOnChange: true
			if (isHE) {
				if (!app.label) {
					app.updateLabel("Working From Home")
					atomicState.appDisplayName = "Working From Home"
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
        	if(settings.tempDisable == true) {
            	paragraph "WARNING: Temporarily Paused - re-enable below."
            } else {
        		input (name: "myThermostats", type: "${isST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Ecobee Thermostat(s)",
                	   required: true, multiple: true, submitOnChange: true)
				paragraph ''
			}
        }

		if (settings?.myThermostats && !settings?.tempDisable) {
            section (title: "Conditions") {
                input(name: "people", type: "capability.presenceSensor", title: "When any of these are present...",  multiple: true, required: true, submitOnChange: true)
				input(name: "timeOfDay", type: "time", title: "At this time of day",  required: (settings.onAway == null), submitOnChange: true)
				paragraph ''
				input(name: "onAway", type: "bool", title: "When thermostat${settings?.myThermostats?.size()>1?'s change':'changes'} to 'Away'", defaultValue: false, required: (settings.timeOfDay == null), submitOnChange: true)
				paragraph ''
            }
            
			section( title: "Actions") {
                if (isST) {
                    def phrases = location.helloHome?.getPhrases()*.label
                    if (phrases) {
                        phrases.sort()
                        input(name: "wfhPhrase", type: "enum", title: "Run this Routine", required: false, options: phrases, submitOnChange: true)
                    }
            	}
                input(name: "setMode", type: "mode", title: "Set Location Mode", required: false, multiple: false, submitOnChange: true)
        		input(name: "setHome", type: "bool", title: "Set thermostat${settings?.myThermostats?.size()>1?'s':''} to the 'Home' program?", defaulValue: true, submitOnChange: true)
				paragraph ''
            }
                
            section (title: "Advanced Options") {
				input(name: 'statMode', title: 'Only when thermostat mode is', type: 'enum', required: false, multiple: true, 
                    		options:getThermostatModes(), submitOnChange: true)
                input(name: "days", type: "enum", title: "Only on certain days of the week", multiple: true, required: false, submitOnChange: true,
                    options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"])
                input(name: "modes", type: "mode", title: "Only when Location Mode is", multiple: true, required: false, submitOnChange: true)
				input(name: "notify", type: "bool", title: "Notify on Actions?", required: true, defaultValue: false, submitOnChange: true)
				paragraph isHE ? "A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken\n" : "A notification is always sent to the Hello Home log whenever an action is taken\n"
            }
			
			if (settings.notify) {
				if (isST) {
					section("Notifications") {
						input(name: "phone", type: "string", title: "Phone number(s) for SMS, example +15556667777 (separate multiple with ; )", required: false, submitOnChange: true)
						input( name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, required: true, submitOnChange: true)
						input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
							if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
						if (!settings.phone && !settings.pushNotify && !settings.speak) paragraph "WARNING: Notifications configured, but nowhere to send them!"
					}
				} else {		// isHE
					section("Use Notification Device(s)") {
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
						input(name: "speak", type: "bool", title: "Speak messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
							input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
						paragraph ""
					}
				}
			}				
        }
        section(title: "Temporarily Disable?") {
           	input(name: "tempDisable", title: "Pause this Helper?", type: "bool", required: false, description: "", submitOnChange: true)                
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
	updateMyLabel()
	
    if(tempDisable == true) {
    	LOG("Temporarily Paused", 2, null, "warn")
    	return true
    }
    if (settings.timeOfDay != null) schedule(timeToday(settings.timeOfDay, location.timeZone), "checkPresence")
    if (settings.onAway) subscribe(settings.theThermostats, "currentProgram", "checkProgram")
}

def checkPresence() {
	LOG("Check presence", 4, null, 'trace')
    if (anyoneIsHome() && getDaysOk() && getModeOk() && getStatModeOk()) {
    	def multiple = false
		LOG("Someone is present", 4, null, 'trace')
        if (isST && wfhPhrase) {
            location.helloHome.execute(wfhPhrase)
        	LOG("Executed ${wfhPhrase}", 4, null, 'trace')
			sendMessage("I executed '${wfhPhrase}' because someone is still home.")
            multiple = true
        }
        if (settings.setMode) {
        	location.setMode(settings.setMode)
            sendMessage("I ${multiple?'also ':''}changed Location Mode to ${settings.setMode}.")
            multiple = true
        }
        if (settings.setHome) {
        	myThermostats.each { tstat ->
        		if (tstat.currentValue('currentProgram') != "Home") tstat.home()
        	}
            def tc = myThermostats.size()
            def also = multiple ? 'also ' : ''
            sendMessage("I ${also}verified that thermostat${tc>1?'s':''} ${myThermostats.toString()[1..-2]} ${tc>1?'are':'is'} set to the 'Home' program because someone is still home.")
            runIn(300, checkHome, [overwrite: true])
        }
    }
}

def checkProgram(evt) {
	LOG("Check program: ${evt.device.displayName} changed to ${evt.value}", 4, null, 'trace')
    
    def multiple = false
    if (settings.onAway && (evt.value == 'Away') && anyoneIsHome() && daysOk && modeOk && statModeOk) {
    	evt.device.home()
        sendMessage("I reset thermostat${tc>1?'s':''} ${myThermostats.toString()[1..-2]} to the 'Home' program because Thermostat ${evt.device.displayName} changed to 'Away' and someone is still home.")
        runIn(300, checkHome, [overwrite: true])
        
    	if (isST && wfhPhrase) {
            location.helloHome.execute(wfhPhrase)
        	LOG("Executed ${wfhPhrase}", 4, null, 'trace')
			send("I also executed '${wfhPhrase}'.")
        }
    	if (settings.setMode) {
        	location.setMode(settings.setMode)
            send("And I changed Location Mode to ${settings.setMode}.")
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

private getStatModeOk() {
	if (settings.statMode == null) return true
	settings.myThermostats?.each { stat ->
		if (settings.statMode.contains(stat.currentValue('thermostatMode'))) {
			return true
		}
	}
	return false
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


private def sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "trace")
    if (settings.notify) {
        String msg = "${app.label} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (isST) {
			if (settings.phone) { // check that the user did select a phone number
				if ( settings.phone.indexOf(";") > 0){
					def phones = settings.phone.split(";")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i], msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone, msg)						// Only to SMS contact
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
			if (settings.phone != null) {
				if ( settings.phone.indexOf(",") > 0){
					def phones = phone.split(",")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i], msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone, msg)						// Only to SMS contact
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
			
		}
    }
	// Always send to Hello Home / Location Event log
	if (isST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
}

private def updateMyLabel() {
	if (isST) return	// ST doesn't support the colored label approach

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
	if (settings.tempDisable) {
		def newLabel = myLabel + "<span style=\"color:orange\"> Paused</span>"
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
	}
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
