/**
 *  ecobee Smart Vents
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
 *	1.0.2 - Misc optimizations and logging changes
 *	1.0.3 - Correct preferences page naming
 *	1.0.4 - Updated settings and TempDisable handling
 *	1.0.5 - Added "Smarter" temp following logic - works better with Smart Recovery
 *	1.0.6 - Fixed setup error
 *	1.0.7 - Added 'smarter' handling of SMart Recovery when following thermostat setpoints
 *	1.0.8 - Fixed setpoint settings, added current status displays in setup
 *	1.0.9 - Close the vents if idle while in cool Mode
 *  1.2.0 - Sync version number with new holdHours/holdAction support
 *	1.2.1 - Protect against LOG type errors
 */
def getVersionNum() { return "1.2.1" }
private def getVersionLabel() { return "ecobee Smart Vents Version ${getVersionNum()}" }
import groovy.json.JsonSlurper

definition(
	name: "ecobee Smart Vents",
	namespace: "smartthings",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "Automates SmartThings-controlled vents to meet a target temperature in a room",
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
	dynamicPage(name: "mainPage", title: "Configure Smart Vents", uninstall: true, install: true) {
    	section(title: "Name for Smart Vents Helper") {
        	label title: "Name this Smart Vents Helper", required: true, defaultValue: "Smart Vents"      
        }
        
        section(title: "Smart Vents: Temperature Sensor(s)") {
        	if (settings.tempDisable) {
            	paragraph "WARNING: Temporarily Disabled as requested. Turn on below to re-enable this Helper."
            } else {
            	paragraph("Select temperature sensors for this handler. If you select multiple sensors, the temperature will be averaged across all of them.${settings.theSensors?'\n\nCurrent temperature is '+getCurrentTemperature().toString()+'°.':''}")
        		input(name: "theSensors", type:"capability.temperatureMeasurement", title: "Use which temperature Sensor(s)", required: true, multiple: true, submitOnChange: true)
            }
		}
        
        if (!settings.tempDisable) {
       		section(title: "Smart Vents: Windows (optional)") {
        		paragraph("Windows will temporarily deactivate Smart Vents while they are open")
            	input(name: "theWindows", type: "capability.contactSensor", title: "Which Window contact sensor(s)? (optional)", required: false, multiple: true)
        	}
       
        	section(title: "Smart Vents: Automated Vents") {
        		paragraph("Specified Econet or Keen vents will be opened until target temperature is achieved, and then closed")
            	input(name: "theEconetVents", type: "device.econetVent", title: "Control which EcoNet Vent(s)?", required: false, multiple: true, submitOnChange: true)
            	input(name: "theKeenVents", type: "device.keenHomeSmartVent", title: "Control which Keen Home Smart Vent(s)?", required: false, multiple:true, submitOnChange: true)
            	if (settings.theEconetVents || settings.theKeenVents) {
            		paragraph("Fully closing too many vents at once may be detrimental to your HVAC system. You may want to define a minimum closed percentage.")
            		input(name: "minimumVentLevel", type: "number", title: "Minimum vent level when closed?", required: true, defaultValue:10, description: '10', range: "0..100")
            	}
        	}
        
			section(title: "Smart Vents: Thermostat") {
				paragraph("Specify which thermostat to monitor for heating/cooling events")
				input(name: "theThermostat", type: "capability.thermostat", title: "Select thermostat", multiple: false, required: true, submitOnChange: true)
			}
		
			section(title: "Smart Vents: Target Temperature") {
            	if (settings.useThermostat && settings.theThermostat) {
                	paragraph("Current setpoint of ${settings.theThermostat} is ${settings.theThermostat.currentValue('thermostatSetpoint')}°.")
                }
				input(name: "useThermostat", type: "bool", title: "Follow setpoints on thermostat${settings.theThermostat?' '+settings.theThermostat.displayName:''}?", required: true, defaultValue: true, submitOnChange: true)
				if (!settings.useThermostat) {
					input(name: "heatingSetpoint", type: "number", title: "Target heating setpoint?", required: true)
					input(name: "coolingSetpoint", type: "number", title: "Target cooling setpoint?", required: true)
				}
			}
        }
        	
		section(title: "Temporarily Disable?") {
        	input(name: "tempDisable", title: "Temporarily Disable this Helper? ", type: "bool", description: "", defaultValue: false, submitOnChange: true)                
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
    unschedule()
    initialize()
}

void uninstalled() {
	// generateSensorsEvents([doors:'default', windows:'default', vents:'default',SmartRoom:'default'])
}

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
    atomicState.scheduled = false
    // Now, just exit if we are disabled...
	if(tempDisable == true) {
    	LOG("temporarily disabled as per request.", 1, null, "warn")
    	return true
    }

    subscribe(theSensors, 'temperature', changeHandler)	
	subscribe(theThermostat, 'thermostatOperatingState', changeHandler)
    subscribe(theThermostat, 'temperature', changeHandler)
    subscribe(theVents, 'level', changeHandler)
	if (theWindows) subscribe(theWindows, "contact", changeHandler)
    if (useThermostat) {
    	subscribe(theThermostat, 'heatingSetpoint', changeHandler)
        subscribe(theThermostat, 'coolingSetpoint', changeHandler)
    }   
	setTheVents(checkTemperature())
    return true
}

def changeHandler(evt) {
	runIn( 2, checkAndSet, [overwrite: true])
}

def checkAndSet() {
	setTheVents(checkTemperature())
}

private String checkTemperature() {
	// Be smarter if we are in Smart Recovery mode: follow the thermostat's temperature instead of watching the current setpoint. Otherwise the room won't get the benefits of heat/cool
    // Smart Recovery. Also, we add the heat/cool differential to try and get ahead of the Smart Recovery curve (otherwise we close too early or too often)
    // 
   	def smarter = theThermostat.currentValue('thermostatOperatingStateDisplay')?.contains('smart')
    
	def cOpState = theThermostat.currentValue('thermostatOperatingState')
    LOG("Current Operating State ${cOpState}",3,null,'info')
	def cTemp = getCurrentTemperature()
	def vents = ''			// if not heating/cooling/fan, then no change to current vents
    if (cOpState == 'heating') {
    	def heatTarget = useThermostat? (smarter? theThermostat.currentTemperature : theThermostat.currentValue('heatingSetpoint')) : settings.heatingSetpoint
        if (smarter && useThermostat) cTemp = cTemp - theThermostat.currentValue('heatDifferential')
		vents = (heatTarget <= cTemp) ? 'closed' : 'open'
        LOG("${theThermostat.displayName} is heating, target temperature is ${heatTarget}°, ${smarter?'adjusted ':''}room temperature is ${cTemp}°",3,null,'info')
    } else if (cOpState == 'cooling') {
    	def coolTarget = useThermostat? (smarter? theThermostat.currentTemperature : theThermostat.currentValue('coolingSetpoint')) : settings.coolingSetpoint
        if (smarter && useThermostat) cTemp = cTemp + theThermostat.currentValue('coolDifferential')
		vents = (coolTarget >= cTemp) ? 'closed' : 'open'
        LOG("${theThermostat.displayName} is cooling, target temperature is ${coolTarget}°, ${smarter?'adjusted ':''}room temperature is ${cTemp}°",3,null,'info')
	} else if (cOpState == 'idle') {
    	LOG("${theThermostat.displayName} is idle, room temperature is ${cTemp}°",3,null,'info')
        def currentMode = theThermostat.currentValue('thermostatMode')
        if (currentMode == 'cool') {
        	def coolTarget = useThermostat ? theThermostat.currentValue('coolingSetpoint') : settings.coolingSetpoint
            vents = (coolTarget >= cTemp) ? 'closed' : 'open'
        } 
    } else if (vents == '' && (cOpState == 'fan only')) {
    	vents = 'open'		// if fan only, open the vents
        LOG("${theThermostat.displayName} is running fan only, room temperature is ${cTemp}°",3,null,'info')
    }
    
	if (theWindows && theWindows.currentContact.contains('open')) {
		vents = 'closed'	// but if a window is open, close the vents
        LOG("${(theWindows.size()>1)?'A':'The'} window/contact is open",3,null,'info')
    }
	LOG("Vents should be ${vents!=''?vents:'unchanged'}",3,null,'info')
	return vents
}

def getCurrentTemperature() {
	Double tTemp = 0.0
	settings.theSensors.each {
		tTemp += it.currentTemperature
	}
	if (settings.theSensors.size() > 1) tTemp = tTemp / settings.theSensors.size() // average all the sensors, if more than 1
	tTemp = tTemp.round(1)
    return tTemp
}

private def setTheVents(ventState) {
	if (ventState == 'open') {
        allVentsOpen()
    } else if (ventState == 'closed') {
        allVentsClosed()
	}
}

private def updateTheVents() {
	def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : [])
    theVents.each {
		if (it.hasCapability('Refresh')) {
    		it.refresh()
    	} else if (it.hasCapability('Polling')) {
    		it.poll()
    	} else if (it.hasCapability('Health Check')) {
    		it.ping()
        }
    }
}

def allVentsOpen() {
	def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : [])
    //LOG("Opening the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents?.each { ventOn(it) }
}

def allVentsClosed() {
	def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : [])
    //LOG("Closing the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents?.each { ventOff(it) } 
}

private def ventOff( theVent ) {
    if (minimumVentLevel.toInteger() == 0) {
      	if (theVent?.currentSwitch == 'on') {
        	theVent.setLevel(0)
        	theVent.off()
            LOG("Closing ${theVent.displayName}",3,null,'info')
        } else {
        	LOG("${theVent.displayName} is already closed",3,null,'info')
        }
    } else {
    	if (theVent?.currentLevel.toInteger() != minimumVentLevel.toInteger()) {
        	theVent.setLevel(minimumVentLevel.toInteger())	// make sure none of the vents are less than the specified minimum
            LOG("Closing ${theVent.displayName} to ${minimumVentLevel}%",3,null,'info')
        } else {
        	LOG("${theVent.displayName} is already closed",3,null,'info')
        }
    }
}

private def ventOn( theVent ) {
    boolean changed = false
    if (theVent?.currentSwitch == 'off') {
    	theVent.on()
        changed = true
    }
    if (theVent?.currentLevel.toInteger() < 99) {
    	theVent.setLevel(99)
        changed = true
    }
    if (changed) {
    	LOG("Opening ${theVent.displayName}",3,null,'info')
    } else {
    	LOG("${theVent.displayName} is already open",3,null,'info')
    }
}

// Ask our parents for help sending the events to our peer sensor devices
private def generateSensorsEvents( Map dataMap ) {
	LOG("generating ${dataMap} events for ${theSensors}",3,null,'info')
	theSensors.each { DNI ->
        parent.getChildDevice(DNI)?.generateEvent(dataMap)
    }
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	if (logType == null) logType = 'debug'
	log."${logType}" message
	message = "${app.label} ${message}"
	parent?.LOG(message, level, null, logType, event, displayEvent)
}
