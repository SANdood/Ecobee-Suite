/**
 *  ecobee Suite Smart Vents
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
 * <snip>
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Allow Ecobee Suite Thermostats only
 *	1.5.02 - Converted all math to BigDecimal
 *	1.6.00 - Release number synchronization
 *	1.6.10 - Resync for parent-based reservatlues (nuions
 *	1.6.11 - Fix offline sensor temperature vall instead of Unknown)
 *	1.6.12 - Added "Generic" (dimmer/switchLevel) Vents
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - nonCached currentValue() for HE
 *	1.7.02 - Fixing private method issue caused by grails
 *  1.7.03 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.04 - Optimized isST/isHE, formatting, added Global Pause
 */
String getVersionNum() { return "1.7.04" }
String getVersionLabel() { return "Ecobee Suite Smart Vents Helper, version ${getVersionNum()} on ${getHubPlatform()}" }
import groovy.json.JsonSlurper

definition(
	name: 			"ecobee Suite Smart Vents",
	namespace: 		"sandood",
	author: 		"Barry A. Burke (storageanarchy at gmail dot com)",
	description:	"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAutomates ${isST?'SmartThings':'Hubitat'}-controlled vents to meet a target temperature in a room.",
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
	boolean ST = isST
	boolean HE = !ST
	
	dynamicPage(name: "mainPage", title: (HE?'<b>':'') + "${getVersionLabel()}" + (HE?'</b>':''), uninstall: true, install: true) {
    	section(title: "") {
        	String defaultLabel = "Smart Vents"
        	label(title: "Name for this ${defaultLabel} Helper", required: true, defaultValue: defaultLabel)
            if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			}
			if (HE) {
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
        	if (settings.tempDisable) {
            	paragraph "WARNING: Temporarily Paused - re-enable below."
            } else {
            	paragraph("Select temperature sensors for this Helper. If you select multiple sensors, the temperature will be averaged across all of them.") 
        		input(name: "theSensors", type:"capability.temperatureMeasurement", title: "Use which Temperature Sensor(s)", required: true, multiple: true, submitOnChange: true)
				if (settings.theSensors) paragraph "The current ${settings.theSensors?.size()>1?'average ':''}temperature for ${settings.theSensors?.size()==1?'this sensor':'these sensors'} is ${getCurrentTemperature()}°"
            }
		}
        
        if (!settings.tempDisable && settings?.theSensors) {
       		section(title: (HE?'<b>':'') + "Smart Vents: Windows (optional)" + (HE?'</b>':'')) {
        		paragraph("Windows will temporarily deactivate Smart Vents while they are open")
            	input(name: "theWindows", type: "capability.contactSensor", title: "Which Window contact sensor(s)? (optional)", description: 'Tap to choose...', required: false, multiple: true)
        	}
       
        	section(title: (HE?'<b>':'') + "Smart Vents: Automated Vents" + (HE?'</b>':'')) {
        		paragraph("Specified Econet, Keen and/or 'generic' vents will be opened until target temperature is achieved, and then closed")
				input(name: "theEconetVents", type: "${ST?'device.econetVent':'device.EconetVent'}", title: "Control which EcoNet Vent(s)?", description: 'Tap to choose...', required: false, multiple: true, submitOnChange: true)
					  input(name: "theKeenVents", type: "${ST?'device.keenHomeSmartVent':'device.KeenHomeSmartVent'}", title: "Control which Keen Home Smart Vent(s)?", description: 'Tap to choose...', required: false, multiple:true, submitOnChange: true)
                input(name: "theGenericVents", type: 'capability.switchLevel', title: "Control which Generic (dimmer) Vent(s)?", description: 'Tap to choose...', required: false, multiple: true, submitOnChange: true)
            	if (settings.theEconetVents || settings.theKeenVents || settings.theGenericVents) {
            		paragraph("Fully closing too many vents at once may be detrimental to your HVAC system. You may want to define a minimum closed percentage.")
            		input(name: "minimumVentLevel", type: "number", title: "Minimum vent level when closed?", required: true, defaultValue:10, description: '10', range: "0..100")
            	}
        	}
        
			section(title: (HE?'<b>':'') + "Smart Vents: Thermostat" + (HE?'</b>':'')) {
				paragraph("Specify which thermostat to monitor for heating/cooling events")
				input(name: "theThermostat", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Select thermostat", description: 'Tap to choose...', multiple: false, required: true, submitOnChange: true)
			}
		
			section(title: (HE?'<b>':'') + "Smart Vents: Target Temperature" + (HE?'</b>':'')) {
            	if (settings.useThermostat && settings.theThermostat) {
					def ncTsp = ST ? settings.theThermostat.currentValue('thermostatSetpoint') : settings.theThermostat.currentValue('thermostatSetpoint', true)
                	paragraph("Current setpoint of ${settings.theThermostat} is ${ncTsp}°.")
                }
				input(name: "useThermostat", type: "bool", title: "Follow setpoints on thermostat${settings.theThermostat?' '+settings.theThermostat.displayName:''}?", required: true, defaultValue: true, submitOnChange: true)
				if (!settings.useThermostat) {
					input(name: "heatingSetpoint", type: "decimal", title: "Target heating setpoint?", description: 'Tap to choose...', required: true)
					input(name: "coolingSetpoint", type: "decimal", title: "Target cooling setpoint?", description: 'Tap to choose...', required: true)
				} else {
                	input(name: "heatOffset", type: "decimal", title: "Heating differential?", defaultValue: 0.0, description: "0.0", required: true, range: "-10..10")
					input(name: "coolOffset", type: "decimal", title: "Cooling differential?", defaultValue: 0.0, description: "0.0", required: true, range: "-10..10")

				}
			}
        } else { 
        	if (settings.theEconetVents || settings.theKeenVents || settings.theGenericVents) {
            	section( title: (HE?'<b>':'') + "Disabled Vent State" + (HE?'</b>':'')) {
            		input(name: 'disabledVents', type: 'enum', title: 'Disabled, desired vent state', description: 'Tap to choose...', options:['open','closed','unchanged'], required: true, multiple: false, defaultValue: 'closed')
                }
      		}
        }        	
		section(title: (HE?'<b>':'') + "Temporarily Disable?" + (HE?'</b>':'')) {
        	input(name: "tempDisable", title: "Pause this Helper?", type: "bool", description: "", defaultValue: false, submitOnChange: true)                
        }
        
        section (getVersionLabel()) {}
    }
}

// Main functions
void installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
    initialize()
}
void updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    initialize()
}
void uninstalled() {
	// generateSensorsEvents([doors:'default', windows:'default', vents:'default',SmartRoom:'default'])
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	updateMyLabel()
	
    atomicState.scheduled = false
    // Now, just exit if we are disabled...
	if (settings.tempDisable) {
        if (disabledVents && (disabledVents != 'unchanged')) {
        	setTheVents(disabledVents)
            LOG("Temporarily Paused, setting vents to ${disabledVents}.", 3, null, 'info')
        } else {
        	LOG("Temporarily Paused, vents unchanged", 3, null, 'info')
        }
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

void changeHandler(evt) {
	runIn( 2, checkAndSet, [overwrite: true])
}

void checkAndSet() {
	setTheVents(checkTemperature())
}

String checkTemperature() {
	// Be smarter if we are in Smart Recovery mode: follow the thermostat's temperature instead of watching the current setpoint. Otherwise the room won't get the benefits of heat/cool
    // Smart Recovery. Also, we add the heat/cool differential to try and get ahead of the Smart Recovery curve (otherwise we close too early or too often)
    // 
	boolean ST = atomicState.isST
	
   	def smarter = (ST ? settings.theThermostat.currentValue('thermostatOperatingStateDisplay') : settings.theThermostat.currentValue('thermostatOperatingStateDisplay', true))?.contains('smart')
    
	def cOpState = ST ? theThermostat.currentValue('thermostatOperatingState') : theThermostat.currentValue('thermostatOperatingState', true)
    LOG("Current Operating State ${cOpState}",3,null,'info')
	def cTemp = getCurrentTemperature()
    def offset 
	def vents = ''			// if not heating/cooling/fan, then no change to current vents
    if (cTemp != null) {	// only if valid temperature readings (Ecosensors can return "unknown")
    	if (cOpState == 'heating') {
        	offset = settings.heatOffset ? settings.heatOffset : 0.0
    		def heatTarget = useThermostat ? ((smarter && (theThermostat.currentTemperature != null))? theThermostat.currentTemperature + offset 
            																							: theThermostat.currentValue('heatingSetpoint') + offset) : settings.heatingSetpoint
        	if (smarter && useThermostat) cTemp = cTemp - theThermostat.currentValue('heatDifferential')
			vents = (heatTarget <= cTemp) ? 'closed' : 'open'
        	LOG("${theThermostat.displayName} is heating, target temperature is ${heatTarget}°, ${smarter?'adjusted ':''}room temperature is ${cTemp}°",3,null,'info')
    	} else if (cOpState == 'cooling') {
        	offset = settings.coolOffset ? settings.coolOffset : 0.0
    		def coolTarget = useThermostat? ((smarter && (theThermostat.currentTemperature != null))? theThermostat.currentTemperature + offset : theThermostat.currentValue('coolingSetpoint') + offset) : settings.coolingSetpoint
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
		//return vents
    }
    return vents
}

def getCurrentTemperature() {
	def tTemp = 0.0
    Integer i = 0
	settings.theSensors.each {
		if (it.currentTemperature != null) {
        	tTemp += it.currentTemperature
            i++
        }
	}
	if (i > 1) tTemp = tTemp / i.toBigDecimal() // average all the sensors, if more than 1
    if (i > 0) {
		return roundIt(tTemp, 1)
    } else {
    	LOG("No valid temperature readings from ${settings.theSensors}",1,null,'warn')
    	return null
    }
}

void setTheVents(ventState) {
	if (ventState == 'open') {
        allVentsOpen()
    } else if (ventState == 'closed') {
        allVentsClosed()
	}
}

void updateTheVents() {
	def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : []) + (theGenericVents ? theGenericVents: [])
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

void allVentsOpen() {
	def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : []) + (theGenericVents ? theGenericVents: [])
    //LOG("Opening the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents?.each { ventOn(it) }
}

void allVentsClosed() {
	def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : []) + (theGenericVents ? theGenericVents: [])
    //LOG("Closing the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents?.each { ventOff(it) } 
}

void ventOff( theVent ) {
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

void ventOn( theVent ) {
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

void updateMyLabel() {
	boolean ST = atomicState.isST
	String flag = ST ? ' (paused)' : '<span '
	
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
		def newLabel = myLabel + (!ST ? '<span style="color:red"> (paused)</span>' : ' (paused)')
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
	}
}

// Ask our parents for help sending the events to our peer sensor devices
void generateSensorsEvents( Map dataMap ) {
	LOG("generating ${dataMap} events for ${theSensors}",3,null,'info')
	theSensors.each { DNI ->
        parent.getChildDevice(DNI)?.generateEvent(dataMap)
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
// Helper Functions
def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def roundIt( BigDecimal value, decimals=0 ) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${atomicState.appDisplayName} ${message}"
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
String  getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST()     { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
boolean getIsHE()     { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
//
// The following 3 calls are ONLY for use within the Device Handler or Application runtime
//  - they will throw an error at compile time if used within metadata, usually complaining that "state" is not defined
//  - getHubPlatform() ***MUST*** be called from the installed() method, then use "state.hubPlatform" elsewhere
//  - "if (state.isST)" is more efficient than "if (isSTHub)"
//
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
	// def ST = (atomicState?.isST != null) ? atomicState?.isST : isST
	//log.debug "isST: ${isST}, isHE: ${isHE}"
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"	
}
//
// **************************************************************************************************************************
