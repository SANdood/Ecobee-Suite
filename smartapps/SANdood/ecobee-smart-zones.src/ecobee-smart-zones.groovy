/**
 *  ecobee smartZones
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
 *	1.0.0 - Final prep for General Release
 *	1.0.1 - Edits to LOG and setup for consistency
 *	1.0.2 - Updated settings and TempDisable handling
 *  1.2.0 - Sync version number with new holdHours/holdAction support
 *	1.2.1 - Protect against LOG type errors
 *
 */
def getVersionNum() { return "1.2.1" }
private def getVersionLabel() { return "ecobee smartZones Version ${getVersionNum()}" }

definition(
	name: "ecobee Smart Zones",
	namespace: "smartthings",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "Synchronizes ecobee recirculation fan between two zones",
	category: "Convenience",
	parent: "smartthings:Ecobee (Connect)",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	dynamicPage(name: "mainPage", title: "Setup Routines", uninstall: true, install: true) {
    	section(title: "Name for Smart Zone Handler") {
        	label title: "Name thisHandler", required: true, defaultValue: "Smart Zone"
        
        }
        
        section(title: "Select Master Thermostat") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled as requested. Turn back on to activate handler."}
        	else {input ("masterThermostat", "capability.Thermostat", title: "Pick Master Ecobee Thermostat", required: true, multiple: false, submitOnChange: true)}            
		}
        
        if (!settings.tempDisable) {
        	section(title: "Select Slave Thermostats") {
        		// Settings option for using Mode or Routine
            	input(name: "slaveThermostats", title: "Pick Slave Ecobee Thermostat(s)", type: "capability.Thermostat", required: true, multiple: true, submitOnChange: true)
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
	LOG("initialize() entered")
	
	// Get slaves into a known state
	slaveThermostats.each { stat ->
		if (state."${stat.displayName}-fanOn") {
			if (stat.currentValue("currentProgramName") == "Hold: Fan") {
				stat.resumeProgram(false)	// just pop this hold off the stack, which will return the fan to prior mode (auto/circulate)
			} // else { another Program overrode our Fan hold, let them clean up the hold stack}
			state."${stat.displayName}-fanOn" = false
		}
	}
	
	// Now, just exit if we are disabled...
	if(tempDisable == true) {
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }
	
	// check the master, but give it a few seconds first
	runIn (5, "masterFanStateHandler", [overwrite: true])
	
	// and finally, subscribe to thermostatOperatingState changes
	subscribe( masterThermostat, "thermostatOperatingState", masterFanStateHandler)
    LOG("initialize() complete", 3)
}

def masterFanStateHandler(evt=null) {
	LOG("masterFanStateHander() entered with evt: ${evt}", 5)
	
	def masterOperatingState = evt ? evt.value : masterThermostat.currentThermostatOperatingState
	LOG("masterFanStateHandler() master thermostatOperatingState = ${masterOperatingState}", 3)
	
	switch (masterOperatingState) {
		case 'fan only':
			// master just changed to fan-only, turn on fan for any slaves not already running their fan
			slaveThermostats.each { stat ->
				if (stat.currentThermostatOperatingState == 'idle') {
					stat.setThermostatFanMode('on', 'nextTransition')
					state."${stat.displayName}-fanOn" = true
					LOG("masterFanStateHandler() turned ${stat.displayName} fan ON", 3)
				} 
			}
			break;
		case 'idle':
		case 'heating':
		case 'cooling':
			// master just started heating/cooling, turn off slave fans (if we turned them on)
			slaveThermostats.each { stat->
				if (state."${stat.displayName}-fanOn") {
					if (stat.currentValue("currentProgramName") == "Hold: Fan") {
						stat.resumeProgram(false)	// just pop this hold off the stack, which will return the fan to prior mode (auto/circulate)
					} // else { another Program overrode our Fan hold, let them clean up the hold stack}
					state."${stat.displayName}-fanOn" = false
                    LOG("masterFanStateHandler() returned ${stat.displayName} to prior fan mode", 3)
				}
			}
			break;
		default:
        	// we ignore the other possible OperatingStates (e.g., 'pendhing heat', etc.)
			break;
	}
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}
