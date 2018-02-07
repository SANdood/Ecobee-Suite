/**
 *  ecobee Suite Open Contacts
 *
 *  Copyright 2016 Sean Kendall Schneyer
 *	Copyright 2017 Barry A. Burke *
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
 *	1.0.0	-	Preparation for General Release
 *	1.0.1	-	Tweaked LOG and setup for consistency across all the Helper SmartApps
 *	1.0.2	-	Fixed 'clear()' and a typo or two
 *  1.0.3	-	Optimized prior fix
 *	1.0.4	- 	Added support for switches, configurable open/closed/on/off
 *	1.0.5	-	Updated settings & disabled handling
 *	1.0.6	-	Fixed errors in setup for contacts and switches
 *	1.0.7	-	Near total logic rewrite for clarity and optimization PLEASE OPEN/SAVE ALL EXISTING APP INSTANCES!!
 *  1.0.8   -   Correct typo preventing turning off HVAC in some situations
 *	1.2.0 	- 	Sync version number with new holdHours/holdAction support
 *	1.2.1	-	Changed order of LOGging
 *	1.2.2	- 	Include device names in notifications
 *	1.2.3	-	Protect against LOG type errors
 *	1.2.4	-	Fix typo in turnOffHVAC
 *	1.3.0   -   Move to sandood namespace
 *
 */
 
def getVersionNum() { return "1.3.0" }
private def getVersionLabel() { return "ecobee Suite Open Contacts, version ${getVersionNum()}" }

definition(
	name: "ecobee Suite Open Contacts",
	namespace: "sandood",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "Turn HVAC on/off based on status of contact sensors or switches",
	category: "Convenience",
	parent: "sandood:Ecobee Suite (Connect)",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	dynamicPage(name: "mainPage", title: "Setup ${getVersionLabel()}", uninstall: true, install: true) {
    	section(title: "Name for this Contact/Switch Handler") {
        	label title: "Name this Handler", required: true, defaultValue: "Contact/Switch Handler"
        }
        
        section(title: "Select Thermostats") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled per request. Turn back on below to activate handler." }
        	else { 
            	input("myThermostats", "capability.Thermostat", title: "Pick Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)
                input(name: 'defaultMode', type: 'enum',  title: "Default Mode for thermostat${((settings.myThermostats==null)||(settings.myThermostats.size()>1))?'s':''}", 
                		multiple: false, required: true, metadata: [values: ['auto', 'cool', 'heat', 'off']], defaultValue: 'auto', submitOnChange: true)
            }          
		}
	
		if (!settings.tempDisable && (settings.myThermostats?.size() > 0)) {

			section(title: "Select Contact Sensors") {
				input(name: "contactSensors", title: "Contact Sensors: ", type: "capability.contactSensor", required: false, multiple: true, description: "", submitOnChange: true)
                if (settings.contactSensors) {
                	input(name: 'contactOpen', type: 'bool', title: 'Turn off HVAC when contact(s) are open?', required: true, defaultValue: true, submitOnChange: true)
                   	paragraph("HVAC will be turned off when above contact sensors are ${((settings.contactOpen==null)||settings.contactOpen)?'Open':'Closed'}")
                }
			}
            
            section(title: "Select Switches") {
            	input(name: "theSwitches", title: "Switches: ", type: "capability.switch", required: false, multiple: true, description: "", submitOnChange: true)
                if (settings.theSwitches) {
                	input(name: 'switchOn', type: 'bool', title: 'Turn off HVAC when switch(es) are turned on?', required: true, defaultValue: true, submitOnChange: true)
                    paragraph("HVAC will be turned off when above switches are turned ${((settings.switchOn==null)||settings.switchOn)?'On':'Off'}")
                }
        	}
            
            if ((settings.contactSensors != null) || (settings.theSwitches != null)) {
				section(title: "Timers") {
					input(name: "offDelay", title: "Delay time (in minutes) before turning off HVAC or Sending Notification [Default=5]", type: "enum", required: true, 
                    	metadata: [values: [0, 1, 2, 3, 4, 5, 10, 15, 30]], defaultValue: 5)
					input(name: "onDelay", title: "Delay time (in minutes) before turning HVAC back on  or Sending Notification [Default=0]", type: "enum", required: true, 
                    	metadata: [values: [0, 1, 2, 3, 4, 5, 10, 15, 30]], defaultValue: "0")
	        	}
            
            	section(title: "Action Preferences") {
            		input(name: "whichAction", title: "Select which actions to take [Default=Notify Only]", type: "enum", required: true, 
                    	metadata: [values: ["Notify Only", "HVAC Only", "Notify and HVAC"]], defaultValue: "Notify Only", submitOnChange: true)
					if (settings.whichAction != "HVAC Only") {
						input("recipients", "contact", title: "Send notifications to") {
							input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        				}                
                	}
            	}
            }          
		} // End if (myThermostats?.size() > 0)

		section(title: "Temporarily Disable?") {
			input(name: "tempDisable", title: "Temporarily Disable Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        
        section (getVersionLabel())
    }
}

// Main functions
def installed() {
	LOG("installed() entered", 5)
	initialize()  
}

def updated() {
	LOG("updated() entered", 5)
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	LOG("initialize() entered")
	if(tempDisable == true) {
		LOG("Teporarily Disabled as per request.", 2, null, "warn")
		return true
	}

	boolean contactOffState = false
	if (contactSensors) {
    	if (contactOpen) {
    		subscribe(contactSensors, "contact.open", sensorOpened)
			subscribe(contactSensors, "contact.closed", sensorClosed)
            contactOffState = contactSensors.currentContact.contains('open')
       	} else {
        	subscribe(contactSensors, "contact.closed", sensorOpened)
			subscribe(contactSensors, "contact.open", sensorClosed)
            contactOffState = contactSensors.currentContact.contains('closed')
        }
    }
    
    boolean switchOffState = false
    if (theSwitches) {
    	if (switchOn) {
        	subscribe(theSwitches, "switch.on", sensorOpened)
            subscribe(theSwitches, "switch.off", sensorClosed)
            switchOffState = theSwitches.currentSwitch.contains('on')
        } else {
        	subscribe(theSwitches, "switch.off", sensorOpened)
            subscribe(theSwitches, "switch.on", sensorClosed)
            switchOffState = theSwitches.currentSwitch.contains('off')
        }
    }
       
    atomicState.HVACModeState = (contactOffState || switchOffState)?'off':'on'
    
    // Initialize the saved Modes
    def tmpThermSavedState = [:]
    settings.myThermostats.each() { therm ->
    	def tid = therm.id
    	tmpThermSavedState[tid] = therm.currentThermostatMode
    }
    atomicState.thermSavedState = tmpThermSavedState
    
    // TODO: Subscribe to the thermostat states to be notified when the HVAC is turned on or off outside of the SmartApp?
	subscribe(theThermostats, 'thermostatMode', statModeChange)
    
	LOG("initialize() exiting")
}

def statModeChange(evt) {
	if (evt.value == 'off') {
    	if (atomicState.HVACModeState != 'off') atomicState.HVACModeState = 'off'
    } else {
    	if (atomicState.HVACModeState != 'on') atomicState.HVACModeState = 'on'
    }
    def tmpThermSavedState = atomicState.thermSavedState
    tmpThermSavedState[evt.device.id] = evt.value	// update the saved mode
    atomicState.thermSavedState = tmpThermSavedState
}
    
// "sensorOpened" called when state change should turn HVAC off - routine name preserved for backwards compatibility with prior implementations
def sensorOpened(evt=null) {
	LOG("sensorOpened() entered with event ${evt?.device} ${evt?.name}: ${evt?.value}", 3,null,'trace')
	
    def HVACModeState = atomicState.HVACModeState
    if(HVACModeState == 'off' || state.openedState == 'off_pending') {
    	// HVAC is already off
        return
    }
    int delay = (settings.onDelay?:0).toInteger()
    if (HVACModeState == 'resume_pending') {
        atomicState.openedState = 'off'
        if (delay > 0) unschedule('turnOnHVAC')
        // HVAC is already/still off
        return
    }

	// HVAC is on, turn it off
   	atomicState.HVACModeState = 'off_pending'
	delay = (settings.offDelay?:5).toInteger()
	if (delay > 0) { runIn(delay*60, 'turnOffHVAC') } else { turnOffHVAC() }  
}

def openedScheduledActions() {		// preserved only for backwards compatibility
	LOG("openedScheduledActions entered", 5)
    turnOffHVAC()
}

// "sensorClosed" called when state change should turn HVAC On (routine name preserved for backwards compatibility with prior implementations)
def sensorClosed(evt=null) {
	// Turn HVAC Off action has occured
    LOG("sensorClosed() entered with event ${evt?.device} ${evt?.name}: ${evt?.value}", 3,null,'trace')
	def HVACModeState = atomicState.HVACModeState
    
    if ( allClosed() == true) {
    	if (HVACModeState.contains('on')) return	// already on, nothing more to do (catches 'on' and 'on_pending')
        int delay = (settings.offDelay?:5).toInteger()
        if (HVACModeState == 'off_pending' ) {
        	atomicState.HVACModeState = 'on'
			if (delay != 0) unschedule('turnOffHVAC')
            // still on
            return
        }
	    
        LOG("All Contact Sensors & Switches are reset, initiating actions.", 5,null,'trace')		
        
        atomicState.HVACModeState = 'on_pending'
		unschedule(openedScheduledActions)
	    delay = (settings.onDelay?:0).toInteger()
    	LOG("The on delay is ${delay}",5,null,'info')
		if (delay > 0) { runIn(delay*60, 'turnOnHVAC') } else { turnOnHVAC() }
	} else {
    	LOG("No action to perform yet...", 5,null,'trace')
    }
}

def closedScheduledActions() {
	LOG("closedScheduledActions entered", 5)
	turnOnHVAC()
}

def turnOffHVAC() {
	// Save current states
    LOG("turnoffHVAC() entered...", 5,null,'trace')
    atomicState.HVACModeState = 'off'
    def action = settings.whichAction?:'Notify Only'
    def tmpThermSavedState = [:]
    def tstatNames = []
    def doHVAC = action.contains('HVAC')
    settings.myThermostats.each() { therm ->
    	def tid = therm.id
    	tmpThermSavedState[tid] = therm.currentThermostatMode
        // LOG("Updated state: ${tmpThermSavedState[therm.id]", 5)
        if( doHVAC ) {
    		if (tmpThermSavedState[tid] != 'off') {
            	therm.setThermostatMode('off')
                tstatNames << [therm.device.displayName]		// only report the ones that aren't off already
                LOG("${therm.device.displayName} turned off (was ${tmpThermSavedState[tid]})",2,null,'info')
            }
        } else {
        	if (tmpThermSavedState[tid] != 'off') {
                tstatNames << [therm.device.displayName]		// only report the ones that aren't off
        		LOG("Saved ${therm.device.displayName}'s current mode (${tmpThermSavedState[tid]})",2,null,'info')
            }
        }
    }
    atomicState.thermSavedState = tmpThermSavedState
	if (tstatNames.size() > 0) {
    	if (action.contains('Notify')) {
    		boolean notified = false
        	def delay = (settings.offDelay?:5).toInteger()
    		if (contactSensors) {
        		def sensorNames = []
            	contactSensors.each { 
            		if (it.currentContact == (contactOpen?true:false)) sensorNames << [it.device.displayName]
            	}
        		if (delay != 0) {
    				sendNotification("${app.label}: ${sensorNames} left ${contactOpen?'open':'closed'} for ${settings.offDelay} minutes, ${doHVAC?'turning':'you should turn'} ${tstatNames} off.")
            	} else {
            		sendNotification("${app.label}: ${sensorNames} ${contactOpen?'opened':'closed'}, ${doHVAC?'turning':'you should turn'} ${tstatNames} off.")
            	}
            	notified = true		// only send 1 notification
        	}
        	if (!notified && theSwitches) {
        		def switchNames = []
            	theSwitches.each {
            		if (it.currentSwitch() == (switchOn?'on':'off')) switchNames << [it.device.displayName]
            	}
        		if (delay != 0) {
    				sendNotification("${app.label}: ${switchNames} left ${switchOn?'on':'off'} for ${settings.offDelay} minutes, ${doHVAC?'turning':'you should turn'} ${tstatNames} off.")
            	} else {
            		sendNotification("${app.label}: ${switchNames} turned ${switchOn?'on':'off'}, ${doHVAC?'turning':'you should turn'} ${tstatNames} off.")
            	}
          		notified = true
        	}
        	if (notified) LOG('Notifications sent',2,null,'info')
    	}
    } else {
    	if (action.contains('Notify')) {
        	sendNotification("${app.label}: ${settings.myThermostats} already off.")
            LOG('All thermostats are already off',2,null,'info')
        }
    }
}

def turnOnHVAC() {
	// Restore previous state
	LOG("turnonHVAC() entered", 5,null,'trace')
    atomicState.HVACModeState = 'on'
    def action = settings.whichAction?:'Notify Only'
    def tstatNames = []
    def doHVAC = action.contains('HVAC')
    // def didHVAC = false
    if (doHVAC) {
	   	// Restore to previous state 
        // LOG("Restoring to previous state", 5) 
        
        settings.myThermostats.each { therm ->
			// LOG("Working on thermostat: ${therm}", 5)
            tstatNames << [therm.device.displayName]
            def tid = therm.id
            String priorMode = settings.defaultMode
            if (atomicState.thermSavedState?.containsKey(tid)) {
            	priorMode = atomicState.thermSavedState[tid]
            }
            
			LOG("Setting ${therm} mode to ${priorMode}",2,null,'info')
			if ( therm.currentValue('thermostatMode') != priorMode) {
            	therm.setThermostatMode(priorMode)
                // didHVAC = true
            }
		} 
	}
    
    if( action.contains('Notify') ) {
    	boolean notified = false
        def delay = (settings.onDelay?:5).toInteger()
    	if (contactSensors) {
        	if (delay != 0) {
    			sendNotification("${app.label}: All Doors and Windows ${contactOpen?'closed':'opened'} for ${settings.onDelay} minutes, ${doHVAC?'turning':'you could turn'} ${tstatNames} on.")
            } else {
            	sendNotification("${app.label}: All Doors and Windows are ${contactOpen?'closed':'open'}, ${doHVAC?'turning':'you could turn'} ${tstatNames} on.")
            }
            notified = true		// only send 1 notification
        }
        if (!notified && theSwitches) {
        	if (delay != 0) {
    			sendNotification("${app.label}: ${(theSwitches.size()>1)?'All switches':'Switch'} left ${switchOn?'off':'on'} for ${settings.onDelay} minutes, ${doHVAC?'turning':'you could turn'} ${tstatNames} on.")
            } else {
            	sendNotification("${app.label}: ${(theSwitches.size()>1)?'All switches':'Switch'} turned ${switchOn?'off':'on'}, ${doHVAC?'turning':'you could turn'} ${tstatNames} on.")
            }
            notified = true
        }
        if (notified) LOG('Notifications sent',2,null,'info')
    }    
}

private Boolean allClosed() {
	// Check if all Sensors are in "HVAC ON" state   
    def response = true
    String txt = ''
    if (contactSensors) {
    	if (contactOpen) {
        	txt = 'closed'
        	if (contactSensors.currentContact.contains('open')) response = false
        } else {
        	txt = 'open'
        	if (contactSensors.currentContact.contains('closed')) response = false
        }
        if (response) LOG("All contact sensors are ${txt}",2,null,'info')
    }
    txt = ''
    if (response && theSwitches) {
    	if (switchOn) {
        	txt = 'off'
        	if (theSwitches.currentSwitch.contains('on')) response = false
        } else {
        	txt = 'on'
        	if (theSwitches.currentSwitch.contains('off')) response = false
        }
        if (response) LOG("All switches are ${txt}",2,null,'info')
    }
    LOG("Returning ${response}",2,null,'info')
    return response
}


// Helper Functions
private def sendNotification(message) {	
    if (location.contactBookEnabled && recipients) {
        LOG("Contact Book enabled!", 5)
        sendNotificationToContacts(message, recipients)
    } else {
        LOG("Contact Book not enabled", 5)
        if (phone) {
            sendSms(phone, message)
        }
    }
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${app.label} ${message}"
    if (logType == null) logType = 'debug'
    log."${logType}" message
	parent.LOG(msg, level, null, logType, event, displayEvent)
}
