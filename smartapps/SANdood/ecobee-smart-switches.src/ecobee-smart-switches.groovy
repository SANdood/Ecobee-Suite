/**
 *  ecobee Smart Switches
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
 *	1.0.1 - Initial Release
 *	1.0.2 - Added option to limit operation to certain SmartThings Modes
 *	1.0.3 - Updated settings and TempDisable handling
 *	1.2.0 - Sync version number with new holdHours/holdAction support
 *	1.2.1 - Protect agsinst LOG type errors
 */
def getVersionNum() { return "1.2.1" }
private def getVersionLabel() { return "ecobee Smart Switches Version ${getVersionNum()}" }

definition(
	name: "ecobee Smart Switches",
	namespace: "smartthings",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "Automates SmartThings-controlled switches, dimmers and generic vents based on thermostat operating state",
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
	dynamicPage(name: "mainPage", title: "Configure Smart Switches", uninstall: true, install: true) {
    	section(title: "Name for Smart Switches Helper App") {
        	label title: "Name this Smart Switches Handler", required: true, defaultValue: "Smart Switches"      
        }
        
        section(title: "Smart Switches: Thermostat(s)") {
            if (settings.tempDisable) {
            	paragraph "WARNING: Temporarily Disabled as requested. Turn back on below to enable handler."
            } else {
				input(name: "theThermostats", type: "capability.thermostat", title: "Monitor these thermostat(s) for operating state changes", multiple: true, required: true, submitOnChange: true)
            }
		}
        
        if (!settings.tempDisable && (theThermostats?.size() > 0)) {
        	section(title: "Smart Switches: Operating State") {
        		input(name: "theOpState", type: "enum", title: "When ${theThermostats?theThermostats:'thermostat'} changes to one of these Operating States", 
                	metadata:[values:['heating','cooling','fan only','idle','pending cool','pending heat','vent economizer']],
                    required: true, multiple: true, submitOnChange: true)
        	}
       
        	section(title: "Smart Switches: Actions") {
				input(name: 'theOnSwitches', type: 'capability.switch', title: "Turn On these switches", multiple: true, required: false, submitOnChange: true)
          		input(name: 'theOnDimmers', type: 'capability.switchLevel', title: "Set level on these dimmers", multiple: true, required: false, submitOnChange: true)
          		if (settings.theOnDimmers) {
					input(name: 'onDimmerLevel', type: 'number', title: "Set dimmers to level...", range: "0..99", required: true)
				}
          		input(name: 'theOffSwitches', type: 'capability.switch', title: 'Turn Off these switches', multiple: true, required: false, submitOnChange: true)
          		input(name:	'theOffDimmers', type: 'capability.switchLevel', title: "Set level on these dimmers", multiple: true, required: false, submitOnChange: true)
          		if (settings.theOffDimmers) {
					input(name: 'offDimmerLevel', type: 'number', title: "Set dimmers to level...", range: "0..99", required: true)
				}
            
            	if (!settings.theOpState?.contains('idle')) {
            		input(name: 'reverseOnIdle', type: 'bool', title: "Reverse above actions when ${settings.theThermostats?.size()>1?'all thermostats':'thermostat'} return to 'idle'?", 
						defaultValue: false, submitOnChange: true)
            	}
        	}
        }
		section(title: "Smart Switches: Operation") {
        	mode(title: "Enable only for specific mode(s)")
        	input(name: "tempDisable", title: "Temporarily Disable this Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        section (getVersionLabel())
    }
}

// Main functions
void installed() {
	LOG("installed() entered", 3, "", 'trace')
    initialize()
}

void updated() {
	LOG("updated() entered", 3, "", 'trace')
	unsubscribe()
    initialize()
}

void uninstalled() {
}

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
    atomicState.scheduled = false
    // Now, just exit if we are disabled...
	if(tempDisable == true) {
    	LOG("Temporarily disabled as per request.", 1, null, "warn")
    	return true
    }

	subscribe(theThermostats, 'thermostatOperatingState', opStateHandler)
    return true
}

def opStateHandler(evt) {
	LOG("${evt.name}: ${evt.value}",2,null,'info')
	if (evt.value == 'idle') {
    	if (reverseOnIdle) {
        	def isReallyIdle = true
        	if (theThermostats.size() > 1) {
            	theThermostats.each { if (it.currentValue('thermostatOperatingState') != 'idle') isReallyIdle = false }
            }
            if (isReallyIdle) {	
            	if (theOnSwitches) {
                	LOG("Turning off ${theOnSwitches.displayName}",2,null,'info')
        			theOnSwitches*.off()
                }
            	if (theOnDimmers) dimmersOff(theOnDimmers)
        		if (theOffSwitches) {
                	LOG("Turning on ${theOffSwitches.displayName}",2,null,'info')
                	theOffSwitches*.on()
                }
        		if (theOffDimmers) dimmersOn(theOffDimmers)
            }
            return
        }
    }
	if (settings.theOpState.contains(evt.value)) {
        if (theOnSwitches) {
        	LOG("Turning on ${theOnSwitches.displayName}",2,null,'info')
    		theOnSwitches*.on()
        }
        if (theOnDimmers) dimmersOn(theOnDimmers)
        if (theOffSwitches) {
        	LOG("Turning off ${theOffSwitches.displayName}",2,null,'info')
        	theOffSwitches*.off()
        }
        if (theOffDimmers) dimmersOff(theOffDimmers)
    }
}

private def dimmersOff( theDimmers ) {
	boolean changed = false
	def dimLevel = offDimmerLevel?:0
    if (dimLevel == 0) {
      	if (theDimmers.currentSwitch.contains('on')) {
        	theDimmers*.off()
            LOG("Turning off ${theDimmers.displayName}",3,null,'info')
        } else {
        	LOG("${theDimmers.displayName} ${theDimmers.size()>1?'are':'is'} already off",3,null,'info')
        }
        return
    } else {
    	theDimmers.each {
        	if (it.currentSwitch == 'off') it.on()
        	if (it.currentLevel.toInteger() != dimLevel) {
        	   	changed = true
            	it.setLevel(dimLevel)	// make sure none of the vents are less than the specified minimum
            }
       	}
   	}
   
   	if (changed) {
       	LOG("Turning down ${(theDimmers.displayName).toString()} to ${dimLevel}%",3,null,'info')
  	} else {
       	LOG("${theDimmers.displayName} ${theDimmers.size()>1?'are':'is'} already at ${dimLevel}%",3,null,'info')
    }
}

private def dimmersOn( theDimmers ) {
    boolean changed = false
    if (theDimmers?.currentSwitch.contains('off')) {
    	theDimmers.on()
        changed = true
    }
    def dimLevel = onDimmerLevel?:99
    theDimmers.each {
    	if (it.currentLevel.toInteger() != dimLevel) {
    		it.setLevel(dimLevel)
        	changed = true
        }
    }
    if (changed) {
    	LOG("Set ${theDimmers.displayName} to ${dimLevel}%",3,null,'info')
    } else {
    	LOG("${theDimmers.displayName} ${theDimmers.size()>1?'are':'is'} already at ${dimLevel}%",3,null,'info')
    }
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	if (logType == null) logType = 'debug'
	log."${logType}" message
	message = "${app.label} ${message}"
	parent?.LOG(message, level, null, logType, event, displayEvent)
}
