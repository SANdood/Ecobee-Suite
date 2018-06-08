/**
 *  ecobee Suite Smart Zones
 *
 *  Copyright 2017 Barry A. Burke
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
 *	1.0.0  - Final prep for General Release
 *	1.0.1  - Edits to LOG and setup for consistency
 *	1.0.2  - Updated settings and TempDisable handling
 *  1.2.0  - Sync version number with new holdHours/holdAction support
 *	1.2.1  - Protect against LOG type errors
 *	1.3.0  - Major Release: renamed and moved to "sandood" namespace
 *	1.4.0  - Rename parent Ecobee Suite Manager
 *	1.4.01 - Updated description
 *	1.4.02 - Added heat/cool "leeching", improved fan only sharing
 *	1.5.00 - Release number synchronization
 */
def getVersionNum() { return "1.5.00" }
private def getVersionLabel() { return "Ecobee Suite Smart Zones Helper, version ${getVersionNum()}" }

definition(
	name: "ecobee Suite Smart Zones",
	namespace: "sandood",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSynchronizes ecobee recirculation fan between two zones",
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
    	section(title: "Name for Smart Zones Helper") {
        	label title: "Name this Helper", required: true, defaultValue: "Smart Zones"
        }
        
        section(title: "Select Master Thermostat") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled as requested. Turn back on to activate handler."}
        	else {input ("masterThermostat", "capability.Thermostat", title: "Pick Master Ecobee Thermostat", required: true, multiple: false, submitOnChange: true)}            
		}
        
        if (!settings.tempDisable) {
        	if (masterThermostat) {
        		section(title: "Select Slave Thermostats") {
        			// Settings option for using Mode or Routine
            		input(name: "slaveThermostats", title: "Pick Slave Ecobee Thermostat(s)", type: "capability.Thermostat", required: true, multiple: true, submitOnChange: true)
				}
                if (slaveThermostats) {
            		section(title: "Slave Thermostat Actions") {
            			input(name: 'shareHeat', title: "Share ${masterThermostat.displayName} heating?", type: "bool", required: true, defaultValue: false, submitOnChange: true)
                		input(name: 'shareCool', title: "Share ${masterThermostat.displayName} cooling?", type: "bool", required: true, defaultValue: false, submitOnChange: true)
                		if (!settings.shareHeat && !settings.shareCool && !settings.shareFan) {
                			input(name: 'shareFan',  title: "Share ${masterThermostat.displayName} fan only?", type: "bool", required: true, defaultValue: true, submitOnChange: true)
                		} else {
                			input(name: 'shareFan',  title: "Share ${masterThermostat.displayName} fan only?", type: "bool", required: true, /*defaultValue: true,*/ submitOnChange: true)
                		}
                    }
            	}
            }
		}
        
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

def updated() {
	LOG("updated() entered", 5)
	unsubscribe()
    initialize()
}

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	
	// Get slaves into a known state
	slaveThermostats.each { stat ->
    	if (stat.currentValue('thermostatHold') == 'hold') {
        	if (state."${stat.displayName}-currProg" == null) {
           		state."${stat.displayName}-currProg" = stat.currentValue('currentProgram')
            }
            if (state."${stat.displayName}-holdType" == null) {
        		state."${stat.displayName}-holdType" = stat.currentValue('lastHoldType')
            }
        } else {
      		state."${stat.displayName}-currProg" = null
        	state."${stat.displayName}-holdType" = null
        }
	}
	
	// Now, just exit if we are disabled...
	if(tempDisable == true) {
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }
	
	// check the master, but give it a few seconds first
	runIn (5, "theAdjuster", [overwrite: true])
	
	// and finally, subscribe to thermostatOperatingState changes
	subscribe( masterThermostat, 'thermostatOperatingState', masterFanStateHandler )
    subscribe( slaveThermostats, 'thermostatOperatingState', changeHandler )
    subscribe( slaveThermostats, 'temperature', changeHandler )
    subscribe( slaveThermostats, 'heatingSetpointDisplay', changeHandler )
    subscribe( slaveThermostats, 'heatingSetpoint', changeHandler )
    subscribe( slaveThermostats, 'coolingSetpointDisplay', changeHandler )
    subscribe( slaveThermostats, 'coolingSetpoint', changeHandler )
    subscribe( slaveThermostats, 'temperature', changeHandler )
    // subscribe( slaveThermostats, 'currentProgram', changeHandler )
    
    LOG("initialize() complete", 3, null, 'trace')
}

def changeHandler(evt) {
	LOG("${evt.displayName} ${evt.name} ${evt.value}",4,null,'trace')
	runIn(2, 'theAdjuster', [overwrite: true])		// collapse multiple simultaneous events
}
def masterFanStateHandler(evt=null) {
	LOG("${evt.displayName} ${evt.name} ${evt.value}",4,null,'trace')
	runIn(2, 'theAdjuster', [overwrite: true])		// (backwards compatibility
}

def theAdjuster() {
	def masterOpState = masterThermostat.currentValue('thermostatOperatingState')
	LOG("theAdjuster() - master thermostatOperatingState = ${masterOpState}", 3, null, 'info')
	
	switch (masterOpState) {
		case 'fan only':
			// master is fan-only, turn on fan for any slaves not already running their fan
            if ((settings.shareFan == null) || settings.shareFan) {
				slaveThermostats.each { stat ->
                	def statOpState = stat.currentValue('thermostatOperatingState')
					if (statOpState == 'idle') {
						setFanOn(stat) 	// stat.setThermostatFanMode('on', 'nextTransition')
					}
                }
			} else if (settings.shareHeat || settings.shareCool) {
            	slaveThermostats.each { stat ->
            		setFanAuto(stat)
                }
            }
			break;
            
		case 'idle':
        	slaveThermostats.each { stat ->
            	if (stat.currentValue('currentProgramName') == 'Hold: Fan On') {
                    setFanAuto(stat)
                }
            }
            break;
            
		case 'heating':
        	if (settings.shareHeat) {
            	slaveThermostats.each { stat ->
                	def statOpState = stat.currentValue('thermostatOperatingState')
                	if (statOpState == 'heating') {
                    	setFanAuto(stat) // stat.resumeProgram(true)		// should get us back to desired fan mode
                    } else if (statOpState == 'fanOnly') {
                    	// See if we are holding the fan but don't need the heat any more
                        if (stat.currentValue('currentProgramName') == 'Hold: Fan On') {
                      		def heatTo = stat.currentValue('heatingSetpointDisplay')
                        	if (heatTo.isNumber()) {
                        		def temp = stat.currentValue('temperature')
                            	if (temp.isNumber()) {
                            		def heatAt = stat.currentValue('heatingSetpoint')
                                	if (heatAt.isNumber()) {
                            			if ((temp.toDouble() >= heatTo.toDouble()) || (temp.toDouble() < heatAt.toDouble())) { 
                                        	// This Zone has reached its target, stop stealing heat
                                			setFanAuto(stat) // stat.setThermostatFanMode('on', 'nextTransition')		// turn the fan on to leech some heat
                                        }
                                    }
                                }
                            }
                        }
                    } else if (statOpState == 'idle') {
                    	def heatTo = stat.currentValue('heatingSetpointDisplay')
                        if (heatTo.isNumber()) {
                        	def temp = stat.currentValue('temperature')
                            if (temp.isNumber()) {
                            	def heatAt = stat.currentValue('heatingSetpoint')
                                if (heatAt.isNumber()) {
                            		if ((temp.toDouble() < heatTo.toDouble()) && (temp.toDouble() > heatAt.toDouble())) { 
                                		setFanOn(stat) // stat.setThermostatFanMode('on', 'nextTransition')		// turn the fan on to leech some heat		
                                    }
                                }
                            }
                        }
                    } else { // Must be 'cooling'
                        if (stat.currentValue('currentProgramName') == 'Hold: Fan On') setFanAuto(stat)	// Just make sure fan is in Auto mode
                    }
                }
            } else {
            	// not sharing (leeching) heat
                if ((settings.shareFan == null) || settings.shareFan) {
                	// but we are sharing fan - turn off fan to avoid overheating this zone
                	slaveThermostats.each { stat ->
                		if (stat.currentValue('currentProgramName') == 'Hold: Fan On') {
                        	setFanAuto(stat)
                        }
                    }
                }
            }
            break;
            
		case 'cooling':
			if (settings.shareCool) {
				slaveThermostats.each { stat->
                    def statOpState = stat.currentValue('thermostatOperatingState')
					if (statOpState == 'cooling') {
                    	// We are cooling too - double-check that fan is in Auto mode
                        setFanAuto(stat)				
                    } else if (statOpState == 'fanOnly') {
                    	// Check if we are holding the fan but don't need the cool any more
                        if (stat.currentValue('currentProgramName') == 'Hold: Fan On') {
                      		def coolTo = stat.currentValue('coolingSetpointDisplay')
                        	if (coolTo.isNumber()) {
                        		def temp = stat.currentValue('temperature')
                            	if (temp.isNumber()) {
                            		def coolAt = stat.currentValue('coolingSetpoint')
                                	if (coolAt.isNumber()) {
                            			if ((temp.toDouble() <= coolTo.toDouble()) || (temp.toDouble() > coolAt.toDouble())) { 
                                        	// This Zone has reached its target, stop stealing cool
                                			setFanAuto(stat) // stat.setThermostatFanMode('on', 'nextTransition')		// turn the fan on to leech some heat
                                        }
                                    }
                                }
                            }
                        }
                    } else if (statOpState == 'idle') {
                    	// Check if we need the cool
                    	def coolTo = stat.currentValue('coolingSetpointDisplay')
                        if (coolTo.isNumber()) {
                        	def temp = stat.currentValue('temperature')
                            if (temp.isNumber()) {
                            	def coolAt = stat.currentValue('coolingSetpoint')
                                if (coolAt.isNumber()) {
                            		if ((temp.toDouble() > coolSp.toDouble()) && (temp.toDouble() < coolAt.toDouble())) {
                                	   	setFanOn(stat)
                                    }
                                }
                    		}
                        }
                    } else { // Must be 'heating'
                        if (stat.currentValue('currentProgramName') == 'Hold: Fan On') setFanAuto(stat)	// Just make sure fan is in Auto mode
                    }
                }
            } else {
            	// not sharing (leeching) cool
                if ((settings.shareFan == null) || settings.shareFan) {
                	// but we are sharing fan - turn off fan to avoid overcooling this zone
                	slaveThermostats.each { stat ->
                		if (stat.currentValue('currentProgramName') == 'Hold: Fan On') {	// set auto for now, so we don't break fanMinOnTime/Circulation
                        	setFanAuto(stat)
                        }
                    }
                }
            }
            break;

		default:
        	// we ignore the other possible OperatingStates (e.g., 'pendhing heat', etc.)
            slaveThermostats.each { stat ->
            	if (stat.currentValue('currentProgramName') == 'Hold: Fan On') setFanAuto(stat)
            }
			break;
	}
}

def setFanAuto(stat) {
	def oldProg = state."${stat.displayName}-currProg"
    if (oldProg) {
    	if (stat.currentValue('currentProgram') != oldProg) stat.setThermostatProgram(oldProg, state."${stat.displayName}-holdType")
        state."${stat.displayName}-currProg" = null
        state."${stat.displayName}-holdType" = null
    }
	def fanMOT = stat.currentValue('fanMinOnTime')
    if (fanMOT.isNumber() && (fanMOT != 0)) {
    	if (stat.currentValue('thermostatFanModeDisplay') != 'circulate') {
        	stat.setThermostatFanMode('circulate')
            LOG("${stat.displayName} fanMode = circulate", 3)
        }
    } else {
    	if (stat.currentValue('thermostatFanMode') != 'auto') {
        	stat.setThermostatFanMode('auto')
            LOG("${stat.displayName} fanMode = auto", 3)
        }
    }
}

def setFanOn(stat) {
	if ((stat.currentValue('currentProgramName') != 'Hold: Fan On') || (stat.currentValue('thermostatFanMode') != 'on')) {
    	if (stat.currentValue('thermostatHold') == 'hold') {
        	state."${stat.displayName}-holdType" = stat.currentValue('lastHoldType')
        	state."${stat.displayName}-currProg" = stat.currentValue('currentProgram')
        }
    	stat.setThermostatFanMode('on', 'nextTransition')
        LOG("${stat.displayName} fanMode = on", 3)
    }
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}
