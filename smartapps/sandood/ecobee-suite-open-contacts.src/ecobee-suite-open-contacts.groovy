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
 *	1.6.13- Removed location.contactBook support - deprecated by SmartThings
 *	1.6.14- Removed use of *SetpointDisplay
 */
def getVersionNum() { return "1.6.14" }
private def getVersionLabel() { return "Ecobee Suite Contacts & Switches Helper, version ${getVersionNum()}" }

definition(
	name: "ecobee Suite Open Contacts",
	namespace: "sandood",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nTurn HVAC on/off based on status of contact sensors or switches (e.g. doors, windows, or fans)",
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
    	section(title: "Name for this Contacts & Switches Helper") {
        	label title: "Name this Helper", required: true, defaultValue: "Contacts & Switches"
        }
        
        section(title: "Select Thermostats") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled per request. Turn back on below to activate handler." }
        	else { 
            	input(name: "myThermostats", type: "device.ecobeeSuiteThermostat", title: "Select Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)
                input(name: 'defaultMode', type: 'enum',  title: "Default Mode for thermostat${((settings.myThermostats==null)||(settings.myThermostats.size()>1))?'s':''}", 
                		multiple: false, required: true, metadata: [values: ['auto', 'cool', 'heat', 'off']], defaultValue: 'auto', submitOnChange: true)
            }          
		}
    
		if (!settings.tempDisable && (settings.myThermostats?.size() > 0)) {

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
				input(name: "contactSensors", title: "Contact Sensors: ", type: "capability.contactSensor", required: false, multiple: true, description: "", submitOnChange: true)
                if (settings.contactSensors) {
                	input(name: 'contactOpen', type: 'bool', title: "Run HVAC Off Actions when ${settings.contactSensors.size()>1?'any of the contacts are':'the contact is'} open?", required: true, defaultValue: true, submitOnChange: true)
                   	paragraph("HVAC Off Actions will be taken when a contact sensor is ${((settings.contactOpen==null)||settings.contactOpen)?'Open':'Closed'}.")
                }
			}
            
            section(title: "Select Switches") {
            	input(name: "theSwitches", title: "Switches: ", type: "capability.switch", required: false, multiple: true, description: "", submitOnChange: true)
                if (settings.theSwitches) {
                	input(name: 'switchOn', type: 'bool', title: "Run HVAC Off Actions when ${settings.theSwitches.size()>1?'any of the switches are':'the switch is'} turned on?", required: true, defaultValue: true, submitOnChange: true)
                    paragraph("HVAC Off Actions will be taken off when a switch is turned ${((settings.switchOn==null)||settings.switchOn)?'On':'Off'}")
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
                    	metadata: [values: ["Notify Only", "HVAC Actions Only", "Notify and HVAC Actions"]], defaultValue: "Notify Only", submitOnChange: true)
                        
					if (settings.whichAction != "HVAC Actions Only") {
                    	paragraph "You can enter multiple phone numbers seperated by a semi-colon (;)"
            			input "phone", "string", title: "Send SMS notifications to", description: "Phone Number(s)", required: false, submitOnChange: true 
                        if (!settings.phone) {
                        	input( name: 'sendPush', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, submitOnChange: true)
                        }
                        if (!settings.phone && !settings.sendPush) paragraph "WARNING: Notifications configured, but nobody to send them to!"
                	}
                    paragraph("All notifications are always sent to the Hello Home log")
            	}
            }          
		} // End if (myThermostats?.size() > 0)

		section(title: "Temporarily Disable?") {
			input(name: "tempDisable", title: "Temporarily Disable Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
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
	myThermostats?.each {
    	cancelReservation(getDeviceId(it.deviceNetworkId), 'modeOff') 
	}
}

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	if(tempDisable == true) {
    	clearReservations()
		LOG("Teporarily Disabled as per request.", 2, null, "warn")
		return true
	}
    subscribe(app, appTouch)

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
            switchOffState = theSwitches.currentValue('switch').contains('on')
        } else {
        	subscribe(theSwitches, "switch.off", sensorOpened)
            subscribe(theSwitches, "switch.on", sensorClosed)
            switchOffState = theSwitches.currentSwitch.contains('off')
        }
    }
       
    def tempState = atomicState.HVACModeState
    if (tempState == null) tempState = (contactOffState || switchOffState)?'off':'on'		// recalculate if we should be off or on
    if (tempState == 'on') {
    	// Initialize the saved state values
    	if (!settings.quietTime) {
    		def tmpThermSavedState = [:]
    		settings.myThermostats.each() { therm ->
    			def tid = getDeviceId(therm.deviceNetworkId)
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
    		}
    		atomicState.thermSavedState = tmpThermSavedState
    	}
        if (atomicState.HVACModeState != 'on') turnOnHVAC()
    } else {
    	LOG("Initialized while should be 'Off' - can't update states",2,null,'warn')
        turnOffHVAC()
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
	def tid = getDeviceId(evt.device.deviceNetworkId)
    
    // save the new value
	def tmpThermSavedState = atomicState.thermSavedState
    if (tmpThermSavedState[tid].heatAdj == evt.value) return 	// we generated this event (below)
    tmpThermSavedState[tid].heatSP = evt.value
    
    if (atomicState.HVACModeState == 'off') {
        def h = (evt.value.isNumber()) evt.numberValue + settings.heatAdjust
        tmpThermSavedState[tid].heatAdj = h
        evt.device.setHeatingSetpoint( h, 'nextTransition')
        // Notify???
    } else {
    	tmpThermSavedState[tid].heatAdj = 999.0
    }
    atomicState.thermSavedState = tmpThermSavedState
}

def coolSPHandler( evt ) {
	def tid = getDeviceId(evt.device.deviceNetworkId)
    
    // save the new value
	def tmpThermSavedState = atomicState.thermSavedState
    if (tmpThermSavedState[tid].coolAdj == evt.value) return
    tmpThermSavedState[tid].coolSP = evt.value
    
    if (atomicState.HVACModeState == 'off') {
    	// adjust and change the actual heating setpoints
        def c = (evt.value.isNumber()) evt.numberValue + settings.coolAdjust
        tmpThermSaveState[tid].coolAdj = c
        evt.device.setCoolingSetpoint( c, 'nextTransition')
        // Notify?
    } else {
    	tmpThermSavedState = 999.0
    }
    atomicState.thermSavedState = tmpThermSavedState
}

// "sensorOpened" called when state change should turn HVAC off - routine name preserved for backwards compatibility with prior implementations
def sensorOpened(evt=null) {
	LOG("sensorOpened() entered with event ${evt?.device} ${evt?.name}: ${evt?.value}", 3,null,'trace')
	
    def HVACModeState = atomicState.HVACModeState
    if(HVACModeState == 'off' || atomicState.openedState == 'off_pending') {
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
    settings.myThermostats.each() { therm ->
    	def tid = getDeviceId(therm.deviceNetworkId)
        if( doHVAC ) {
        	if (settings.quietTime) {
            	// Turn on quiet time
                qtSwitch."${settings.qtOn}"()
                LOG("${therm.displayName} Quiet Time enabled (${qtSwitch.displayName} turned ${settings.qtOn})",2,null,'info')
            } else if ((settings.hvacOff == null) || settings.hvacOff) {
            	// turn off the HVAC
                makeReservation(tid, 'modeOff')						// make sure nobody else turns HVAC on until I'm ready
    			if (therm.currentValue('thermostatMode') != 'off') {
                	tmpThermSavedState[tid].mode = therm.currentValue('thermostatMode')
            		therm.setThermostatMode('off')
                	tstatNames << therm.device.displayName		// only report the ones that aren't off already
                	LOG("${therm.device.displayName} turned off (was ${tmpThermSavedState[tid].mode})",2,null,'info')    
            	}
            } else if (settings.adjustSetpoints) {
            	// Adjust the setpoints
                def h = therm.currentValue('heatingSetpoint') 
                def c = therm.currentValue('coolingSetpoint')
                // save the current values for when we turn back on
                tmpThermSavedState[tid].heatSP = h
                tmpThermSavedState[tid].coolSP = c
                h = h + settings.heatAdjust
                c = c + settings.coolAdjust
                tmpThermSavedState[tid].heatAdj = h
                tmpThermSavedState[tid].coolAdj = c
                
                tmpThermSavedState[tid].holdType = therm.currentValue('lastHoldType')
                tmpThermSavedState[tid].thermostatHold = therm.currentValue('thermostatHold')
                tmpThermSavedState[tid].currentProgramName = therm.currentValue('currentProgramName')	// Hold: Home
                tmpThermSavedState[tid].currentProgramId = therm.currentValue('currentProgramId')		// home
                tmpThermSavedState[tid].currentProgram = therm.currentValue('currentProgram')			// Home
                tmpThermSavedState[tid].scheduledProgram = therm.currentValue('scheduledProgram')
                
                therm.setHeatingSetpoint(h, 'nextTransition')
                therm.setCoolingSetpoint(c, 'nextTransition')
                LOG("${therm.device.displayName} heatingSetpoint adjusted to ${h}, coolingSetpoint to ${c}",2,null,'info')
            }
        } else {
        	if (tmpThermSavedState[tid].mode != 'off') {
                tstatNames << [therm.device.displayName]		// only report the ones that aren't off
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
    				sendMessage("${sensorNames.toString()[1..-2]} left ${contactOpen?'open':'closed'} for ${settings.offDelay} minutes, ${doHVAC?'running HVAC Off actions for':'you should turn off'} ${tstatNames.toString()[1..-2]}.")
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
    				sendMessage("${switchNames.toString()[1..-2]} left ${switchOn?'on':'off'} for ${settings.offDelay} minutes, ${doHVAC?'running HVAC Off actions for':'you should turn on'} ${tstatNames.toString()[1..-2]}.")
            	} else {
            		sendMessage("${switchNames.toString()[1..-2]} turned ${switchOn?'on':'off'}, ${doHVAC?'running HVAC Off actions for':'you should turn on'} ${tstatNames.toString()[1..-2]}.")
            	}
          		notified = true
        	}
        	if (notified) LOG('Notifications sent',2,null,'info')
    	}
    } else {
    	if (action.contains('Notify')) {
        	sendMessage("${settings.myThermostats} already off.")
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
        
        settings.myThermostats.each { therm ->
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
                    def oldMode = therm.currentValue('thermostatMode')
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
                			tstatNames << [therm.device.displayName]		// only report the ones that aren't off already
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
                        if (tmpThermSavedState[tid].scheduledProgram == therm.currentValue('scheduledProgram')) {
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
    			sendMessage("All Doors and Windows ${contactOpen?'closed':'opened'} for ${settings.onDelay} minutes, " +
                					"${doHVAC?(notReserved?'running HVAC On actions for':'but reservations prevent running HVAC On actions for'):'you could turn on'} ${tstatNames.toString()[1..-2]}.")
            } else {
            	sendMessage("All Doors and Windows are ${contactOpen?'closed':'open'}, " +
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
void makeReservation(tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id, type )
}
// Cancel my reservation
void cancelReservation(tid, String type='modeOff') {
	log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id, type )
}
// Do I have a reservation?
Boolean haveReservation(tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id, type )
}
// Do any Apps have reservations?
Boolean anyReservations(tid, String type='modeOff') {
	return parent.anyReservations( tid, type )
}
// How many apps have reservations?
Integer countReservations(tid, String type='modeOff') {
	return parent.countReservations( tid, type )
}
// Get the list of app IDs that have reservations
List getReservations(tid, String type='modeOff') {
	return parent.getReservations( tid, type )
}
// Get the list of app Names that have reservations
List getGuestList(tid, String type='modeOff') {
	return parent.getGuestList( tid, type )
}

private def sendMessage(notificationMessage) {
	LOG("Notification Message: ${notificationMessage}", 2, null, "trace")
    String msg = "${app.label} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
    if (phone) { // check that the user did select a phone number
        if ( phone.indexOf(";") > 0){
            def phones = phone.split(";")
            for ( def i = 0; i < phones.size(); i++) {
                LOG("Sending SMS ${i+1} to ${phones[i]}",2,null,'info')
                sendSmsMessage(phones[i], msg)
            }
        } else {
            LOG("Sending SMS to ${phone}",2,null,'info')
            sendSmsMessage(phone, msg)
        }
    } else if (settings.sendPush) {
        LOG("Sending Push to everyone",2,null,'warn')
        sendPushMessage(msg)
    }
    sendNotificationEvent( notificationMessage )								// Always send to hello home
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${app.label} ${message}"
    if (logType == null) logType = 'debug'
    log."${logType}" message
	parent.LOG(msg, level, null, logType, event, displayEvent)
}
