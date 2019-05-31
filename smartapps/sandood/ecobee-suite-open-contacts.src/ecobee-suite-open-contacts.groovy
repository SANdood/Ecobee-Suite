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
 * <snip>
 *	1.4.00 - Renamed parent Ecobee Suite Manager
 *  1.4.01 - Updated description
 *	1.4.02 - Fixed contact open/closed notification
 *	1.4.03 - Added Temp setpoint option, linkage to Quiet Time
 *	1.4.04 - Changed displayed name for consistency
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Fixed HVACOff
 *	1.5.02 - Allow Ecobee Suite Thermostats only
 *	1.5.03 - Fixed qtOn capitalization error
 *	1.5.04 - Converted all math back to BigDecimal
 *	1.5.05 - Added modeOff reservations support
 *	1.5.06 - Added multiple SMS support (Contacts being deprecated by ST)
 *	1.6.00 - Release number synchronization
 *	1.6.01 - Fixed sendMessage()
 *	1.6.02 - Fix reservation initialization error
 *	1.6.03 - REALLY fix reservation initialization error
 *	1.6.04 - Really, REALLY fix reservation initialization error
 *	1.6.05 - Fixed getDeviceId()
 *	1.6.10 - Converted to parent-based reservations
 *	1.6.11 - Clear reservations when disabled
 *	1.6.12 - Cancel modeOff reservation if we get overridden
 *	1.6.13 - Removed location.contactBook support - deprecated by SmartThings
 *	1.6.14 - Removed use of *SetpointDisplay
 *	1.6.15 - Fixed(?) adjust temperature to adjust only when HVACMode is !Off
 *	1.6.16 - Fixed initialization logic WRT HVAC on/off state
 *	1.6.17 - Minor text edits
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - nonCached currentValue() for HE
 *	1.7.02 - Fixed initialization error
 *	1.7.03 - Cosmetic cleanup, and nonCached currentValue() on Hubitat
 * 	1.7.04 - Fix myThermostats (should have been theThermostats)
 *	1.7.05 - More nonCached cleanup
 *	1.7.06 - Fixed multi-contact/multi-switch initialization
 */
def getVersionNum() { return "1.7.06" }
private def getVersionLabel() { return "Ecobee Suite Contacts & Switches Helper,\nversion ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 			"ecobee Suite Open Contacts",
	namespace: 		"sandood",
	author: 		"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 	"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nTurn HVAC on/off based on status of contact sensors or switches (e.g. doors, windows, or fans)",
	category: 		"Convenience",
	parent: 		"sandood:Ecobee Suite Manager",
	iconUrl: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false,
    pausable: 		true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	dynamicPage(name: "mainPage", title: "${getVersionLabel()}", uninstall: true, install: true) {
    	section(title: "") {
        	String defaultLabel = "Contact & Switches"
        	label(title: "Name for this ${defaultLabel} Helper", required: true, defaultValue: defaultLabel)
            if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			}
			if (isHE) {
				if (app.label.contains('<span ')) {
					if (atomicState?.appDisplayName != null) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
						String myLabel = app.label.substring(0, app.label.indexOf('<span '))
						atomicState.appDisplayName = myLabel
						app.updateLabel(myLabel)
					}
				}
			} else {
            	if (app.label.contains(' (paused)')) {
                	String myLabel = app.label.substring(0, app.label.indexOf(' (paused)'))
                    atomicState.appDisplayName = myLabel
                    app.updateLabel(myLabel)
                } else {
                	atomicState.appDisplayName = app.label
                }
            }
        	if(settings.tempDisable) { 
				paragraph "WARNING: Temporarily Paused - re-enable below." 
			} else { 
            	input(name: "theThermostats", type: "${isST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)
            }          
		}
    
		if (!settings.tempDisable && (settings.theThermostats?.size() > 0)) {

			section(title: "Select HVAC Off Actions") {
            	paragraph('If you are using  the Quiet Time Helper, you can centralize off/idle actions by turning on Quiet Time from this Helper instead of taking HVAC actions directly. The Quiet Time Helper also offers additional control options.')
                input(name: 'quietTime', type: 'bool', title: 'Enable Quiet Time?', required: true, defaultValue: false, submitOnChange: true)
                if (settings.quietTime) {
                	input(name: 'qtSwitch', type: 'capability.switch', required: true, title: 'Which switch controls Quiet Time?', multiple: false, submitOnChange: true)
                    if (settings.qtSwitch) {
                        input(name: "qtOn", type: "enum", title: "Enable Quiet Time when switch ${settings.qtSwitch.displayName} is:", required: true, multiple: false, options: ["on","off"], submitOnChange: true)
                        if (settings.qtOn != null) paragraph("Switch ${settings.qtSwitch.displayName} will be turned ${settings.qtOn?'On':'Off'} when HVAC Off Actions are taken.")
                    }
                } else {
                	input(name: 'hvacOff', type: "bool", title: "Turn off HVAC?", required: true, defaultValue: true, submitOnChange: true)
                	if ((settings.hvacOff == null) || settings.hvacOff) {
                    	paragraph("HVAC Mode will be set to Off. Circulation, Humidification and/or Dehumidification may still operate while HVAC is Off. Use the Quiet Time Helper for additional control options.")
                    } else {
                    	input(name: 'adjustSetpoints', type: 'bool', title: 'Adjust heat/cool setpoints?', required: true, defaultValue: false, submitOnChange: true)
                        if (adjustSetpoints) {
                        	input(name: 'heatAdjust', type: 'decimal', title: 'Heating setpoint adjustment (+/- 20°)', required: true, defaultValue: 0.0, range: '-20..20')
                            input(name: 'coolAdjust', type: 'decimal', title: 'Cooling setpoint adjustment (+/- 20°)', required: true, defaultValue: 0.0, range: '-20..20')
                        }
                    }
           		}
            }
            
			section(title: "Select Contact Sensors") {
				input(name: "contactSensors", title: "Contact Sensors: ", type: "capability.contactSensor", required: false, multiple: true,  submitOnChange: true)
                if (settings.contactSensors) {
                	input(name: 'contactOpen', type: 'bool', title: "Run HVAC Off Actions when ${settings.contactSensors.size()>1?'any of the contacts are':'the contact is'} open?", required: true, defaultValue: true, submitOnChange: true)
                   	paragraph("HVAC Off Actions will be taken when a contact sensor is ${((settings.contactOpen==null)||settings.contactOpen)?'Open':'Closed'}.")
                }
			}
            
            section(title: "Select Switches") {
            	input(name: "theSwitches", title: "Switches: ", type: "capability.switch", required: false, multiple: true,  submitOnChange: true)
                if (settings.theSwitches) {
                	input(name: 'switchOn', type: 'bool', title: "Run HVAC Off Actions when ${settings.theSwitches.size()>1?'any of the switches are':'the switch is'} turned on?", required: true, defaultValue: true, submitOnChange: true)
                    paragraph("HVAC Off Actions will be taken off when a switch is turned ${((settings.switchOn==null)||settings.switchOn)?'On':'Off'}")
                }
        	}
            
            if ((settings.contactSensors != null) || (settings.theSwitches != null)) {
				section(title: "Timers") {
					input(name: "offDelay", title: "Delay time (in minutes) before turning off HVAC or Sending Notification [Default=5]", type: "enum", required: true, 
                    	options: [0, 1, 2, 3, 4, 5, 10, 15, 30], defaultValue: 5)
					input(name: "onDelay", title: "Delay time (in minutes) before turning HVAC back on  or Sending Notification [Default=0]", type: "enum", required: true, 
                    	options: [0, 1, 2, 3, 4, 5, 10, 15, 30], defaultValue: "0")
	        	}
            
            	section(title: "Action Preferences") {
            		input(name: "whichAction", title: "Select which actions to take [Default=Notify Only]", type: "enum", required: true, 
                    	options: ["Notify Only", "HVAC Actions Only", "Notify and HVAC Actions"], defaultValue: "Notify Only", submitOnChange: true)
				}
                        
				if (settings.whichAction != "HVAC Actions Only") {
					if (isST) {
						section("Notifications") {
							paragraph "A notification will also be sent to the Hello Home log\n"
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
							paragraph "A 'HelloHome' notification will also be sent to the Location Event log\n"
						}
					}
            	}
            }          
		} // End if (theThermostats?.size() > 0)

		section(title: "Temporary Pause") {
			input(name: "tempDisable", title: "Pause this Helper? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        
        section (getVersionLabel()) {}
    }
}

// Main functions
def installed() {
	LOG("installed() entered", 5)
	initialize()  
}
def uninstalled () {
   clearReservations()
}
def updated() {
	LOG("updated() entered", 5)
	unsubscribe()
	unschedule()
	initialize()
    // tester()
}

def clearReservations() {
	theThermostats?.each {
    	cancelReservation(getDeviceId(it.deviceNetworkId), 'modeOff') 
	}
}
//
// TODO - if stat goes offline, then comes back online, then re-initialize states...
//
def initialize() {
	LOG("${getVersionLabel()}\nInitializing...", 2, "", 'info')
	updateMyLabel()
	
	if(tempDisable == true) {
    	clearReservations()
		LOG("Temporarily Paused.", 2, null, "warn")
		return true
	}
    // subscribe(app, appTouch)

	boolean contactOffState = false
	if (contactSensors) {
    	def openSensors = 0
        def closedSensors = 0
        contactSensors.each {
            if (it?.currentContact == 'open') { openSensors++; } else { closedSensors++; }
        }
    	if (contactOpen) {
        	subscribe(contactSensors, "contact.open", sensorOpened)
            subscribe(contactSensors, "contact.closed", sensorClosed)
        	contactOffState = (openSensors != 0) 		// true if any of the sensors are currently open
       	} else {
        	subscribe(contactSensors, "contact.closed", sensorOpened)
			subscribe(contactSensors, "contact.open", sensorClosed)
			contactOffState = (closedSensors != 0)
        }
    }
    LOG("contactOffState = ${contactOffState}",2,null,'trace')
    
    boolean switchOffState = false
    if (theSwitches) {
    	def onSwitches = 0
        def offSwitches = 0
        theSwitches.each {
        	if (it.currentSwitch == 'on') { onSwitches++ } else { offSwitches++ }
        }
    	if (switchOn) {
        	subscribe(theSwitches, "switch.on", sensorOpened)
            subscribe(theSwitches, "switch.off", sensorClosed)
            switchOffState = (onSwitches != 0)
        } else {
        	subscribe(theSwitches, "switch.off", sensorOpened)
            subscribe(theSwitches, "switch.on", sensorClosed)
			switchOffState = (offSwitches != 0)
        }
    }
    LOG("switchOffState = ${switchOffState}",2,null,'trace')
    
    //def tempState = atomicState.HVACModeState
    //if (tempState == null) tempState = (contactOffState || switchOffState)?'off':'on'		// recalculate if we should be off or on
    def tempState = (contactOffState || switchOffState)?'off':'on'		// recalculate if we should be off or on
    if (tempState == 'on') {
    	// Initialize the saved state values
    	if (!settings.quietTime) {
    		def tmpThermSavedState = [:]
    		settings.theThermostats.each() { therm ->
    			def tid = getDeviceId(therm.deviceNetworkId)
				if (isST) {
					tmpThermSavedState[tid] = [	mode: therm.currentValue('thermostatMode'), ]
					if (settings.adjustSetpoints) {
						tmpThermSavedState[tid] += [
														heatSP: therm.currentValue('heatingSetpoint'), 
														coolSP: therm.currentValue('coolingSetpoint'),
														heatAdj: 999.0,
														coolAdj: 999.0,
														holdType: therm.currentValue('lastHoldType'),
														thermostatHold: therm.currentValue('thermostatHold'),
														currentProgramName: therm.currentValue('currentProgramName'),	// Hold: Home
														currentProgramId: therm.currentValue('currentProgramId'),		// home
														currentProgram: therm.currentValue('currentProgram'),			// Home
												  ]
					}
				} else {
					// We have to ensure we get the latest values on HE - it caches stuff when it probably shouldn't
					tmpThermSavedState[tid] = [	mode: therm.currentValue('thermostatMode', true), ]
					if (settings.adjustSetpoints) {
						tmpThermSavedState[tid] += [
														heatSP: therm.currentValue('heatingSetpoint', true), 
														coolSP: therm.currentValue('coolingSetpoint', true),
														heatAdj: 999.0,
														coolAdj: 999.0,
														holdType: therm.currentValue('lastHoldType', true),
														thermostatHold: therm.currentValue('thermostatHold', true),
														currentProgramName: therm.currentValue('currentProgramName', true),	// Hold: Home
														currentProgramId: therm.currentValue('currentProgramId', true),		// home
														currentProgram: therm.currentValue('currentProgram', true),			// Home
												  ]
					}
				}
    		}
    		atomicState.thermSavedState = tmpThermSavedState
    	}
        if (atomicState.HVACModeState != 'on') turnOnHVAC()
    } else {
    	LOG("Initialized while should be 'Off' - can't update states",2,null,'warn')
        if (atomicState.HVACModeState != 'off') turnOffHVAC()
    }
    
    // TODO: Subscribe to the thermostat states to be notified when the HVAC is turned on or off outside of the SmartApp?
	if (!settings.quietTime) {
    	if ((settings.hvacOff == null) || settings.hvacOff) subscribe(theThermostats, 'thermostatMode', statModeChange)
        else if (settings.adjustSetpoints) {
    		subscribe(theThermostats, 'heatingSetpoint', heatSPHandler)
            subscribe(theThermostats, 'coolingSetpoint', coolSPHandler)
        }
    }
    
	LOG("initialize() exiting",2,null,'trace')
}

def statModeChange(evt) {
	// only gets called if we are turning off the HVAC (not for quietTime or setpointAdjust operations)
    def tid = getDeviceId(evt.device.deviceNetworkId)
	if (evt.value == 'off') {
    	if (atomicState.HVACModeState != 'off') atomicState.HVACModeState = 'off'
    } else {
    	// somebody has overridden us..
        cancelReservation( tid, 'modeOff' )		// might as well give up our reservation
    	if (atomicState.HVACModeState != 'on') atomicState.HVACModeState = 'on'
    }
    def tmpThermSavedState = atomicState.thermSavedState
    tmpThermSavedState[tid].mode = evt.value	// update the saved mode
    atomicState.thermSavedState = tmpThermSavedState
}
    
def heatSPHandler( evt ) {
	// called when the heatingSetpoint value changes, but only if we are monitoring/making setpoint changes
	// (ie., this won't get called if we are using Quiet Time or just HVAC Off)
    if (evt.numberValue != null) {
		def tid = getDeviceId(evt.device.deviceNetworkId)
    
    	// save the new value
		def tmpThermSavedState = atomicState.thermSavedState
    	if (tmpThermSavedState[tid].heatAdj == evt.numberValue) return 	// we generated this event (below)
    	tmpThermSavedState[tid].heatSP = evt.numberValue
    
    	if (!atomicState.HVACModeState.contains('off')) {			// Only adjust setpoints when the HVAC is not off
        	def h = evt.numberValue + settings.heatAdjust
        	tmpThermSavedState[tid].heatAdj = h
        	evt.device.setHeatingSetpoint( h, 'nextTransition')
        	// Notify???
    	} else {
    		tmpThermSavedState[tid].heatAdj = 999.0
    	}
    	atomicState.thermSavedState = tmpThermSavedState
    }
}

def coolSPHandler( evt ) {
	if (evt.numberValue != null) {
        def tid = getDeviceId(evt.device.deviceNetworkId)

        // save the new value
        def tmpThermSavedState = atomicState.thermSavedState
        if (tmpThermSavedState[tid].coolAdj == evt.numberValue) return
        tmpThermSavedState[tid].coolSP = evt.numberValue

        if (!atomicState.HVACModeState.contains('off')) {
            // adjust and change the actual heating setpoints
            def c = evt.numberValue + settings.coolAdjust
            tmpThermSaveState[tid].coolAdj = c
            evt.device.setCoolingSetpoint( c, 'nextTransition')
            // Notify?
        } else {
            tmpThermSavedState = 999.0
        }
        atomicState.thermSavedState = tmpThermSavedState
    }
}

// "sensorOpened" called when state change should turn HVAC off - routine name preserved for backwards compatibility with prior implementations
def sensorOpened(evt=null) {
	LOG("sensorOpened() entered with event ${evt?.device} ${evt?.name}: ${evt?.value}", 3,null,'trace')
	
    def HVACModeState = atomicState.HVACModeState
    if(HVACModeState == 'off' || atomicState.openedState == 'off_pending') {
    	// HVAC is already off
        return
    }
    int delay = (settings.onDelay?:0) as Integer
    if (HVACModeState == 'resume_pending') {
        atomicState.openedState = 'off'
        if (delay > 0) unschedule('turnOnHVAC')
        // HVAC is already/still off
        return
    }

	// HVAC is on, turn it off
   	atomicState.HVACModeState = 'off_pending'
	delay = (settings.offDelay?:5).toInteger()
	if (delay > 0) { runIn(delay*60, 'turnOffHVAC', [overwrite: true]) } else { turnOffHVAC() }  
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
		if (delay > 0) { runIn(delay*60, 'turnOnHVAC', [overwrite: true]) } else { turnOnHVAC() }
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
    def tmpThermSavedState = atomicState.thermSavedState
    def tstatNames = []
    def doHVAC = action.contains('HVAC')
    settings.theThermostats.each() { therm ->
    	def tid = getDeviceId(therm.deviceNetworkId)
        if( doHVAC ) {
        	if (settings.quietTime) {
            	// Turn on quiet time
                qtSwitch."${settings.qtOn}"()
                LOG("${therm.displayName} Quiet Time enabled (${qtSwitch.displayName} turned ${settings.qtOn})",2,null,'info')
            } else if ((settings.hvacOff == null) || settings.hvacOff) {
            	// turn off the HVAC
                makeReservation(tid, 'modeOff')						// make sure nobody else turns HVAC on until I'm ready
				def ncTm = isST ? therm.currentValue('thermostatMode') : therm.currentValue('thermostatMode', true)
    			if (ncTm != 'off') {
                	tmpThermSavedState[tid].mode = ncTm				// therm.currentValue('thermostatMode')
            		therm.setThermostatMode('off')
                	tstatNames << therm.device.displayName		// only report the ones that aren't off already
                	LOG("${therm.device.displayName} turned off (was ${tmpThermSavedState[tid].mode})",2,null,'info')    
            	}
            } else if (settings.adjustSetpoints) {
            	// Adjust the setpoints
                def h = isST ? therm.currentValue('heatingSetpoint') : therm.currentValue('heatingSetpoint', true)
                def c = isST ? therm.currentValue('coolingSetpoint') : therm.currentValue('coolingSetpoint', true)
                // save the current values for when we turn back on
                tmpThermSavedState[tid].heatSP = h
                tmpThermSavedState[tid].coolSP = c
                h = h + settings.heatAdjust
                c = c + settings.coolAdjust
                tmpThermSavedState[tid].heatAdj = h
                tmpThermSavedState[tid].coolAdj = c
                
				if (isST) {
					tmpThermSavedState[tid].holdType = therm.currentValue('lastHoldType')
					tmpThermSavedState[tid].thermostatHold = therm.currentValue('thermostatHold')
					tmpThermSavedState[tid].currentProgramName = therm.currentValue('currentProgramName')	// Hold: Home
					tmpThermSavedState[tid].currentProgramId = therm.currentValue('currentProgramId')		// home
					tmpThermSavedState[tid].currentProgram = therm.currentValue('currentProgram')			// Home
					tmpThermSavedState[tid].scheduledProgram = therm.currentValue('scheduledProgram')
				} else {
					tmpThermSavedState[tid].holdType = therm.currentValue('lastHoldType', true)
					tmpThermSavedState[tid].thermostatHold = therm.currentValue('thermostatHold', true)
					tmpThermSavedState[tid].currentProgramName = therm.currentValue('currentProgramName', true)	// Hold: Home
					tmpThermSavedState[tid].currentProgramId = therm.currentValue('currentProgramId', true)		// home
					tmpThermSavedState[tid].currentProgram = therm.currentValue('currentProgram', true)			// Home
					tmpThermSavedState[tid].scheduledProgram = therm.currentValue('scheduledProgram', true)
				}
                therm.setHeatingSetpoint(h, 'nextTransition')
                therm.setCoolingSetpoint(c, 'nextTransition')
                LOG("${therm.device.displayName} heatingSetpoint adjusted to ${h}, coolingSetpoint to ${c}",2,null,'info')
            }
        } else {
        	if (tmpThermSavedState[tid].mode != 'off') {
                tstatNames << therm.device.displayName		// only report the ones that aren't off
        		LOG("Saved ${therm.device.displayName}'s current mode (${tmpThermSavedState[tid].mode})",2,null,'info')
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
            		if (it.currentContact == (settings.contactOpen?'open':'closed')) sensorNames << it.device.displayName
            	}
        		if (delay != 0) {
    				sendMessage("${sensorNames.toString()[1..-2]} ${(sensorNames.size()>1)?'has':'have'} been ${contactOpen?'open':'closed'} for ${settings.offDelay} minutes, ${doHVAC?'running HVAC Off actions for':'you should turn off'} ${tstatNames.toString()[1..-2]}.")
            	} else {
            		sendMessage("${sensorNames.toString()[1..-2]} ${contactOpen?'opened':'closed'}, ${doHVAC?'running HVAC Off actions for':'you should turn off'} ${tstatNames.toString()[1..-2]}.")
            	}
            	notified = true		// only send 1 notification
        	}
        	if (!notified && theSwitches) {
        		def switchNames = []
            	theSwitches.each {
            		if (it.currentSwitch == (switchOn?'on':'off')) switchNames << it.device.displayName
            	}
        		if (delay != 0) {
    				sendMessage("${switchNames.toString()[1..-2]} ${(sensorNames.size()>1)?'has':'have'} been ${switchOn?'on':'off'} for ${settings.offDelay} minutes, ${doHVAC?'running HVAC Off actions for':'you should turn on'} ${tstatNames.toString()[1..-2]}.")
            	} else {
            		sendMessage("${switchNames.toString()[1..-2]} turned ${switchOn?'on':'off'}, ${doHVAC?'running HVAC Off actions for':'you should turn on'} ${tstatNames.toString()[1..-2]}.")
            	}
          		notified = true
        	}
        	if (notified) LOG('Notifications sent',2,null,'info')
    	}
    } else {
    	if (action.contains('Notify')) {
        	sendMessage("${settings.theThermostats} already off.")
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
    def notReserved = true

    if (doHVAC) {
	   	// Restore to previous state 
        // LOG("Restoring to previous state", 5) 
        
        settings.theThermostats.each { therm ->
			// LOG("Working on thermostat: ${therm}", 5)
            tstatNames << therm.displayName
            def tid = getDeviceId(therm.deviceNetworkId)
            String priorMode = settings.defaultMode
            def tmpThermSavedState = atomicState.thermSavedState
            if (tmpThermSavedState?.containsKey(tid)) {
            	if (settings.quietTime) {
            		// Turn on quiet time
            		def onOff = settings.qtOn=='on' ? 'off' : 'on'
                	qtSwitch."$onOff"()
                    LOG("${therm.displayName} Quiet Time disabled (${qtSwitch.displayName} turned ${onOff})",2,null,'info')
            	} else if ((settings.hvacOff == null) || settings.hvacOff) {
            		// turn on the HVAC
                    def oldMode = isST ? therm.currentValue('thermostatMode') :  therm.currentValue('thermostatMode', true) 
                    def newMode = (tmpThermSavedState[tid].mode == '') ? 'auto' : tmpThermSavedState[tid].mode
                    if (newMode != oldMode) {
                    	def i = countReservations( tid, 'modeOff' ) - (haveReservation(tid, 'modeOff') ? 1 : 0)
                        // log.debug "count=${countReservations(tid,'modeOff')}, have=${haveReservation(tid,'modeOff')}, i=${i}"
                    	if ((oldMode == 'off') && (i > 0)) {
                        	// Currently off, and somebody besides me has a reservation - just release my reservation
                            cancelReservation(tid, 'modeOff')
                            notReserved = false							
                            LOG("Cannot change ${therm.device.displayName} to ${newMode.capitalize()} - ${getGuestList(tid, 'modeOff').toString()[1..-2]} hold 'modeOff' reservations",1,null,'warn')
                        } else {
                        	// Not off, or nobody else but me has a reservation
                            cancelReservation(tid, 'modeOff')
                        	therm.setThermostatMode( newMode )                            
                			tstatNames << therm.device.displayName		// only report the ones that aren't off already
                			LOG("${therm.device.displayName} ${newMode.capitalize()} Mode restored (was ${oldMode.capitalize()})",2,null,'info')
                        } 
                    } else {
                    	LOG("${therm.device.displayName} is already in ${newMode.capitalize()}",2,null,'info')
                    }
            	} else if (settings.adjustSetpoints) {
                	// Restore the prior values
                    def holdType = tmpThermSavedState[tid].holdType
                    if (holdType == '') holdType = 'nextTransition'
                    if (tmpThermSavedState[tid].currentProgram == tmpThermSavedState.scheduledProgram) {
                       	// we were running the scheuled program when we turned off - just return to the currently scheduled program
                        therm.resumeProgram()
                        LOG("${therm.device.displayName} resumed current program",2,null,'info')
                    } else if (tmpThermSavedState[tid].currentProgramName == ('Hold: ' + tmpThermSavedState[tid].currentProgram)) {
                    	// we were in a hold of a named program - reinstate the hold IF the saved scheduledProgram == the current scheduledProgram
						def ncSp = isST ? therm.currentValue('scheduledProgram') : therm.currentValue('scheduledProgram', true)
                        if (tmpThermSavedState[tid].scheduledProgram == ncSp) {
                        	therm.setThermostatProgram(tmpThermSavedState[tid].currentProgram, holdType)
                        	LOG("${therm.device.displayName} returned to ${tmpThermSavedState[tid]} program (${holdType})",2,null,'info')
                        }
                    } else {
                    	// we were in some other sort of hold - so set a new hold to the original values
                		def h = tmpThermSavedState[tid].heatSP
                		def c = tmpThermSavedState[tid].coolSP
                		tmpThermSavedState[tid].heatAdj = 999.0
                		tmpThermSavedState[tid].coolAdj = 999.0
                		therm.setHeatingSetpoint(h, holdType) // should probably be the current holdType
                		therm.setCoolingSetpoint(c, holdType)
                    	LOG("${therm.device.displayName} heatingSetpoint returned to ${h}, coolingSetpoint to ${c} (${holdType})",2,null,'info')
                    }
            	}
            }
		} 
	}
    
    if( action.contains('Notify') ) {
    	boolean notified = false
        def delay = (settings.onDelay?:5).toInteger()
    	if (contactSensors) {
        	if (delay != 0) {
    			sendMessage("All contact sensors have been ${contactOpen?'closed':'opened'} for ${settings.onDelay} minutes, " +
                					"${doHVAC?(notReserved?'running HVAC On actions for':'but reservations prevent running HVAC On actions for'):'you could turn on'} ${tstatNames.toString()[1..-2]}.")
            } else {
            	sendMessage("All contact sensors are ${contactOpen?'closed':'open'}, " +
                					"${doHVAC?(notReserved?'running HVAC On actions for':'but reservations prevent running HVAC On actions for'):'you could turn On'} ${tstatNames.toString()[1..-2]}.")
            }
            notified = true		// only send 1 notification
        }
        if (!notified && theSwitches) {
        	if (delay != 0) {
    			sendMessage("${theSwitches.toString()[1..-2]}${(theSwitches.size()>1)?' all':''} left ${switchOn?'off':'on'} for ${settings.onDelay} minutes, " +
                					"${doHVAC?(notReserved?'running HVAC On actions for':'but reservations prevent running HVAC On actions for'):'you could turn On'} ${tstatNames.toString()[1..-2]}.")
            } else {
            log.debug "foo ${tstatNames}"
            	sendMessage("${theSwitches.toString()[1..-2]}${(theSwitches.size()>1)?' all':''} turned ${switchOn?'off':'on'}, " +
                					"${doHVAC?(notReserved?'running HVAC On actions for':'but reservations prevent running HVAC On actions for'):'you could turn On'} ${tstatNames.toString()[1..-2]}.")
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

private def getDeviceId(networkId) {
	// def deviceId = networkId.split(/\./).last()	
    // LOG("getDeviceId() returning ${deviceId}", 4, null, 'trace')
    // return deviceId
    return networkId.split(/\./).last()
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(String tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id as String, type )
}
// Cancel my reservation
void cancelReservation(String tid, String type='modeOff') {
	log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id as String, type )
}
// Do I have a reservation?
Boolean haveReservation(String tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id as String, type )
}
// Do any Apps have reservations?
Boolean anyReservations(String tid, String type='modeOff') {
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

private def sendMessage(notificationMessage) {
	LOG("Notification Message: ${notificationMessage}", 2, null, "trace")
    String msg = "${app.label} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
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
}

private def updateMyLabel() {
	String flag = isST ? ' (paused)' : '<span '
	
	// Display Ecobee connection status as part of the label...
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (myLabel.contains(flag)) {
		// strip off any connection status tag
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	if (settings.tempDisable) {
		def newLabel = myLabel + (isHE ? '<span style="color:orange"> Paused</span>' : ' (paused)')
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
	}
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${app.label} ${message}"
    if (logType == null) logType = 'debug'
    log."${logType}" message
	parent.LOG(msg, level, null, logType, event, displayEvent)
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
