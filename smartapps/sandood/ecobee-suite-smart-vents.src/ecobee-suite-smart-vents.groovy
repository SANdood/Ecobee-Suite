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
 *	1.7.05 - More code optimizations
 *	1.7.06 - Added generic switch control (e.g., to control a fan)
 *	1.7.07 - Update vent status (refresh) before & after taking actions, display vent status in appLabel
 *	1.7.08 - Optimized checks when nothing changes; added vent open/close option for 'fan only'
 *	1.7.09 - Removed redundant log.debug text, fixed new fan only vent option
 *	1.7.10 - Added option to disable local display of log.debug() logs, tweaked myLabel handling
 *	1.7.11 - Check both hasCapability('switchLevel') & hasCommand('setLevel')
 *	1.7.12 - Fix typo in ventsOn(); set 100 instead of 99
 *	1.7.13 - Optimized checkTemperature() to avoid timeout errors on ST
 *	1.7.14 - Added maximumVentLevel and fanOnlyState; more optimizations
 */
String getVersionNum() 		{ return "1.7.14" }
String getVersionLabel() 	{ return "Ecobee Suite Smart Vents & Switches Helper, version ${getVersionNum()} on ${getHubPlatform()}" }
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
    def vc = 0			// vent counter
	
	dynamicPage(name: "mainPage", title: (HE?'<b>':'') + "${getVersionLabel()}" + (HE?'</b>':''), uninstall: true, install: true) {
    	section(title: "") {
        	String defaultLabel = "Smart Vents & Switches"
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
				if (settings.theSensors) paragraph "The current ${settings.theSensors?.size()>1?'average ':''}temperature for ${settings.theSensors?.size()==1?'this sensor':'these sensors'} is ${getAverageTemperature()}°"
            }
		}
        
        if (!settings.tempDisable && settings?.theSensors) {
       		section(title: (HE?'<b>':'') + "Smart Vents & Switches: Windows (optional)" + (HE?'</b>':'')) {
        		paragraph("Windows will temporarily deactivate Smart Vents while they are open")
            	input(name: "theWindows", type: "capability.contactSensor", title: "Which Window contact sensor(s)? (optional)", required: false, multiple: true)
				if (HE) paragraph ''
        	}
       
        	section(title: (HE?'<b>':'') + "Smart Vents & Switches: Automated Vents" + (HE?'</b>':'')) {
        		paragraph("Specified Econet, Keen and/or 'generic' dimmer/switch-controlled vents will be opened until target temperature is achieved, and then closed")
				input(name: "theEconetVents", type: "${ST?'device.econetVent':'device.EconetVent'}", title: "Control which EcoNet Vent(s)?", required: false, multiple: true, 
					  submitOnChange: true)
                vc = settings?.theEconetVents.size()
				input(name: "theKeenVents", type: "${ST?'device.keenHomeSmartVent':'device.KeenHomeSmartVent'}", title: "Control which Keen Home Smart Vent(s)?", required: false, 
					  multiple:true, submitOnChange: true)
            	vc = vc + settings?.theKeenVents.size()
                input(name: "theGenericVents", type: 'capability.switchLevel', title: "Control which Generic (dimmer) Vent(s)?", required: false, multiple: true, 
					  submitOnChange: true)
                vc = vc + settings?.theGenericVents.size()
				input(name: "theGenericSwitches", type: 'capability.switch', title: "Control which Switch(es)?", required: false, multiple: true, submitOnChange: true)
                vc = vc + settings?.theGenericVents.size()
            	if (settings.theEconetVents || settings.theKeenVents || settings.theGenericVents ) {
            		paragraph("Fully closing too many vents at once may be detrimental to your HVAC system. You may want to define a minimum closed percentage.")
            		input(name: "minimumVentLevel", type: "number", title: "Minimum vent level when closed?", required: true, defaultValue:10, description: '10', range: "0..100")
                    input(name: "maximumVentLevel", type: "number", title: "Maximum vent level when open?", required: true, defaultValue:100, description: '100', range: "0..100")
            	}
				if (HE) paragraph ''
        	}
        
			section(title: (HE?'<b>':'') + "Smart Vents & Switches: Thermostat" + (HE?'</b>':'')) {
				paragraph("Specify which thermostat to monitor for heating/cooling events")
				input(name: "theThermostat", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Select thermostat",
					  multiple: false, required: true, submitOnChange: true)
				if (HE) paragraph ''
			}
		
			section(title: (HE?'<b>':'') + "Smart Vents: Target Temperature" + (HE?'</b>':'')) {
            	if (settings.useThermostat && settings.theThermostat) {
					def cSetpoint = 	ST ? settings.theThermostat.currentValue('thermostatSetpoint') : settings.theThermostat.currentValue('thermostatSetpoint', true)
					def cHeatSetpoint = ST ? settings.theThermostat.currentValue('heatingSetpoint') : settings.theThermostat.currentValue('heatingSetpoint', true)
					def cCoolSetpoint = ST ? settings.theThermostat.currentValue('coolingSetpoint') : settings.theThermostat.currentValue('coolingSetpoint', true)
					def cMode = 		ST ? settings.theThermostat.currentValue('thermostatMode') : settings.theThermostat.currentValue('thermostatMode', true)
					paragraph("${settings.theThermostat} is in '${cMode}' mode, heatingSetpoint: ${cHeatSetpoint}°, coolingSetpoint: ${cCoolSetpoint}°, thermostatSetpoint: ${cSetpoint}°.")
                }
				input(name: "useThermostat", type: "bool", title: "Follow setpoints on thermostat${settings.theThermostat?' '+settings.theThermostat.displayName:''}?", required: true, 
					  defaultValue: true, submitOnChange: true)
				if (!settings.useThermostat) {
					input(name: "heatingSetpoint", type: "decimal", title: "Target heating setpoint?", description: 'Tap to choose...', required: true)
					input(name: "coolingSetpoint", type: "decimal", title: "Target cooling setpoint?", description: 'Tap to choose...', required: true)
				} else {
                	input(name: "heatOffset", type: "decimal", title: "Heating differential?", defaultValue: 0.0,/* description: "0.0",*/ required: true, range: "-10..10")
					input(name: "coolOffset", type: "decimal", title: "Cooling differential?", defaultValue: 0.0,/* description: "0.0",*/ required: true, range: "-10..10")
				}
				if (false) input(name: 'closedFanOnly', type: 'bool', title: "Close the vent${vc>1?'s':''} while HVAC is 'fan only'?", defaultValue: false)
                def foDefault = settings?.closedFanOnly != null ? (settings.closedFanOnly ? 'closed' : 'unchanged') : 'unchanged'
                input(name: 'fanOnlyState', type: 'enum', title: "Vent state during 'Fan Only' operation?", required: true, submitOnChange: true, default: foDefault,
                	  options: ['open', 'closed', 'unchanged'])
				if (HE) paragraph ''
			}
        } else { 
        	if (settings.theEconetVents || settings.theKeenVents || settings.theGenericVents || settings.theGenericSwitches) {
            	section( title: (HE?'<b>':'') + "Disabled Vent State" + (HE?'</b>':'')) {
            		input(name: 'disabledVents', type: 'enum', title: 'Disabled, desired vent state', options:[open: 'open/on',closed: 'closed/off',unchanged: 'unchanged'], 
						  required: true, multiple: false, defaultValue: 'closed')
					if (HE) paragraph ''
                }
      		}
        }        	
		section(title: (HE?'<b>':'') + "Temporarily Disable?" + (HE?'</b>':'')) {
        	input(name: "tempDisable", title: "Pause this Helper?", type: "bool", description: "", defaultValue: false, submitOnChange: true)
        }
		section(title: "") {
			input(name: "debugOff", title: "Disable debug logging? ", type: "bool", required: false, defaultValue: false, submitOnChange: true)
		}         
        section (getVersionLabel()) {}
    }
}

// Main functions
void installed() {
	LOG("Installed with settings: ${settings}", 4, null, 'trace')
    initialize()
}
void updated() {
	LOG("Updated with settings: ${settings}", 4, null, 'trace')
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
	if (settings.tempDisable) {
        if (disabledVents && (disabledVents != 'unchanged')) {
			(disabledVents.startsWith('open')) ? setTheVents('open') : setTheVents('closed') 
            LOG("Temporarily Paused, setting vents to ${disabledVents}.", 3, null, 'info')
        } else {
        	LOG("Temporarily Paused, vents unchanged", 3, null, 'info')
        }
		updateMyLabel()
        return true
    }
	if (settings.debugOff) log.info "log.debug() logging disabled"
	
	def theVents = (settings.theEconetVents ?: []) + (settings.theKeenVents ?: []) + (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])

    subscribe(theSensors, 		'temperature', 				changeHandler)	
	subscribe(theThermostat, 	'thermostatOperatingState', changeHandler)
    subscribe(theThermostat, 	'temperature', 				changeHandler)
	subscribe(theThermostat,	'thermostatMode', 			changeHandler)
    subscribe(theVents, 		'level', 					changeHandler)
	if (theWindows) subscribe(theWindows, "contact",		changeHandler)
    if (useThermostat) {
    	subscribe(theThermostat, 'heatingSetpoint', 		changeHandler)
        subscribe(theThermostat, 'coolingSetpoint', 		changeHandler)
    }
	atomicState.currentStatus = [:]
	setTheVents(checkTemperature())
	updateMyLabel()
    return true
}

void changeHandler(evt) {
	updateTheVents()
	runIn( 3, checkAndSet, [overwrite: true])		// collapse multiple interelated events into a single thread
}

void checkAndSet() {
	setTheVents(checkTemperature())
}

String checkTemperature() {
	boolean ST = atomicState.isST
    
    def cTemp = getAverageTemperature()
    if (cTemp != null) {		// only if valid temperature readings (Ecosensors can return "unknown")
        // Be smarter if we are in Smart Recovery mode: follow the thermostat's temperature instead of watching the current setpoint. Otherwise the room won't get the benefits of heat/cool
        // Smart Recovery. Also, we add the heat/cool differential to try and get ahead of the Smart Recovery curve (otherwise we close too early or too often)

        String smarter = 	ST ? theThermostat.currentValue('thermostatOperatingStateDisplay')	: theThermostat.currentValue('thermostatOperatingStateDisplay', true)
        boolean beSmart = 	(smarter?.contains('mart'))	// "(Smart Recovery)"
        String cOpState = 	ST ? theThermostat.currentValue('thermostatOperatingState') 		: theThermostat.currentValue('thermostatOperatingState', true)
        def cTemperature = 	ST ? theThermostat.currentValue('temperature')						: theThermostat.currentValue('temperature', true)
        def coolSP = 		ST ? theThermostat.currentValue('coolingSetpoint') 					: theThermostat.currentValue('coolingSetpoint', true)
        def heatSP = 		ST ? theThermostat.currentValue('heatingSetpoint') 					: theThermostat.currentValue('heatingSetpoint', true)
        String cMode = 		ST ? theThermostat.currentValue('thermostatMode') 					: theThermostat.currentValue('thermostatMode', true)
        // def cTemp = getAverageTemperature()
        def currentStatus = [smarter: smarter, beSmart: beSmart, opState: cOpState, mode: cMode, temperature: cTemp, coolSP: coolSP, heatSP: heatSP]
        if (atomicState.currentStatus == currentStatus) { 
            LOG("Status unchanged...",3,null,'info')
            return 			// ignore - identical to last time
        } else {
            atomicState.currentStatus = currentStatus
            LOG("currentStatus: ${currentStatus}",3,null,'info')
        }
        def offset 
        String vents = 'unchanged'			// if not heating/cooling/fan, then no change to current vents
    	if ((cOpState == 'heating') || (cMode == 'heat')) {
        	offset = settings.heatOffset ?: 0.0
    		def heatTarget = useThermostat ? ((beSmart && (cTemperature != null)) ? cTemperature + offset : heatSP + offset) : settings.heatingSetpoint
        	if (beSmart && useThermostat) {
            	cTemp = cTemp - (ST ? theThermostat.currentValue('heatDifferential') : theThermostat.currentValue('heatDifferential', true))
            }
			vents = (heatTarget <= cTemp) ? 'closed' : 'open'
        	LOG("${theThermostat.displayName} is heating, target temperature is ${heatTarget}°, ${beSmart?'adjusted ':''}room temperature is ${cTemp}°",3,null,'info')
    	} else if ((cOpState == 'cooling') || (cMode == 'cool')) {
        	offset = settings.coolOffset ?: 0.0
    		def coolTarget = useThermostat ? ((beSmart && (cTemperature != null)) ? cTemperature + offset : coolSP + offset) : settings.coolingSetpoint
        	if (beSmart && useThermostat) {
            	cTemp = cTemp + (ST ? theThermostat.currentValue('coolDifferential') : theThermostat.currentValue('coolDifferential', true))
            }
			vents = (coolTarget >= cTemp) ? 'closed' : 'open'
        	LOG("${theThermostat.displayName} is cooling, target temperature is ${coolTarget}°, ${beSmart?'adjusted ':''}room temperature is ${cTemp}°",3,null,'info')
		} else if (cOpState == 'idle') {
    		LOG("${theThermostat.displayName} is idle, room temperature is ${cTemp}°",3,null,'info')
        	def currentMode = ST ? theThermostat.currentValue('thermostatMode') : theThermostat.currentValue('thermostatMode', true)
        	if (currentMode == 'cool') {
        		def coolTarget = useThermostat ? coolSP : settings.coolingSetpoint
            	vents = (coolTarget > cTemp) ? 'closed' : 'open'
        	} else if (currentMode == 'heat') {
            	def heatTarget = useThermostat ? heatSP : settings.heatingSetpoint
                vents = (heatTarget < cTemp) ? 'closed' : 'open'
            }
		} else if (vents == '' && (cOpState == 'fan only')) {
        	if (!settings.fanOnlyState) {
                if (!settings.closedFanOnly) {
                    vents = 'unchanged'
                    LOG("${theThermostat.displayName} is running 'Fan Only', room temperature is ${cTemp}°, vents-->unchanged",3,null,'info')
                } else {
                    vents = 'closed'
                    LOG("${theThermostat.displayName} is running 'Fan Only', room temperature is ${cTemp}°, vents-->closed",3,null,'info')
                }
            } else {
            	// New Fan Only selector is in use
                vents = settings.fanOnlyState
                LOG("${theThermostat.displayName} is running 'Fan Only', room temperature is ${cTemp}°, vents-->${settings?.fanOnlyState}",3,null,'info')
            }
		}    
		if (vents == 'open') {
        	if (settings.theWindows) {
            	boolean openWindows = ST ? settings.theWindows*.currentValue('contact').contains('open') : settings.theWindows*.currentValue('contact', true).contains('open')
                if (openWindows) {
					vents = 'closed'	// but if a window is open, close the vents
        			LOG("${(theWindows.size()>1)?'A':'The'} window/contact is open",3,null,'info')
                }
            }
    	}
		LOG("Vents should be ${vents!=''?vents:'unchanged'}",3,null,'info')
		//return vents
    }
    return vents
}

def getAverageTemperature() {
	boolean ST = atomicState.isST
    
	def tTemp = 0.0G
    Integer i = 0
	settings.theSensors.each {
		def t = ST ? it.currentValue('temperature') : it.currentValue('temperature', true)
		if (t != null) {
        	tTemp += t as BigDecimal
            i++
        }
	}
	if (i > 1) tTemp = tTemp / i // average all the sensors, if more than 1
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
	} else if (ventState == 'unchanged') {
    	boolean ST = atomicState.isST	
    	def theVents = (settings.theEconetVents ?: []) + (settings.theKeenVents ?: []) + (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
        def ventSwitch = ST ? theVents[0].currentValue('switch') : theVents[0].currentValue('switch', true) // assumption: all vents are in the same state
        if (ventSwitch == 'on') {
            ventState = 'open'
        	def hasLevel = theVents[0].hasAttribute('level')
            if (hasLevel) {
        		def currentLevel = (ST ? theVents[0].currentValue('level') : theVents[0].currentValue('level', true))?.toInteger()
        		if (currentLevel == minimumVentLevel.toInteger()) {
                	// while physically 'open', we are set to the minimum vent level, so we are logically 'closed'
	            	ventState = 'closed'
                }
           	}
        } else {
        	// assert ventSwitch == 'off'
        	ventState = 'closed'
        }
        LOG("setTheVents('unchanged'), prior ventState: ${atomicState.ventState}, new ventState: ${ventState}",3,null,'trace')
    }
	if (ventState) atomicState.ventState = ventState
	updateMyLabel()
	runIn(2, updateTheVents, [overwrite: true])
}

void updateTheVents() {
	def theVents = (settings.theEconetVents ?: []) + (settings.theKeenVents ?: []) + (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    theVents.each {
		if (it.hasCommand('refresh')) {
    		it.refresh()
    	} else if (it.hasCommand('poll')) {
    		it.poll()
    	} else if (it.hasCommand('ping')) {
    		it.ping()
        }
    }
}

void allVentsOpen() {
	def theVents = (settings.theEconetVents ?: []) + (settings.theKeenVents ?: []) + (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    //LOG("Opening the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents.each { ventOn(it) }
}

void allVentsClosed() {
	def theVents = (settings.theEconetVents ?: []) + (settings.theKeenVents ?: []) + (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    //LOG("Closing the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents.each { ventOff(it) } 
}

void ventOff( theVent ) {
	boolean ST = atomicState.isST	
    
	def hasSetLevel = (theVent.hasCapability('switchLevel') || theVent.hasCommand('setLevel'))
    if (minimumVentLevel.toInteger() == 0) {
    	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
      	if (currentSwitch == 'on') {
			if (hasSetLevel) {
        		theVent.off()
                if (theVent.hasCommand('refresh')) theVent.refresh()
                def currentLevel = ST ? theVent.currentValue('level') : theVent.currentValue('level', true)
				if (currentLevel?.toInteger() != 0) theVent.setLevel(0) // Belt & suspenders - make sure the level is reset to 0
            	LOG("Closing ${theVent.displayName}",3,null,'info')
			} else {
				theVent.off()
				LOG("Turning off ${theVent.displayName}",3,null,'info')
			}
            if (theVent.hasCommand('refresh')) theVent.refresh()
        } else {
        	LOG("${theVent.displayName} is already closed/off",3,null,'info')
        }
    } else {
		if (hasSetLevel) {
        	def currentLevel = ST ? theVent.currentValue('level') : theVent.currentValue('level', true)
			if (currentLevel?.toInteger() != minimumVentLevel.toInteger()) {
        		theVent.setLevel(minimumVentLevel.toInteger())	// make sure none of the vents are less than the specified minimum
            	LOG("Closing ${theVent.displayName} to ${minimumVentLevel}%",3,null,'info')
                if (theVent.hasCommand('refresh')) theVent.refresh()
        	} else {
        		LOG("${theVent.displayName} is already closed to ${minimumVentLevel}%",3,null,'info')
        	}
		} else {
        	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
			if (currentSwitch == 'on') {
				theVent.off()
				LOG("Turning off ${theVent.displayName}",3,null,'info')
                if (theVent.hasCommand('refresh')) theVent.refresh()
			} else {
				LOG("${theVent.displayName} is already off",3,null,'info')
			}
		}
    }
}

void ventOn( theVent ) {
	boolean ST = atomicState.isST
    boolean changed = false
    def hasSetLevel = (theVent.hasCapability('switchLevel') || theVent.hasCommand('setLevel'))
    def maximumVentLevel = (settings.maximumVentLevel ?: 100) as Integer
	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
    def currentLevel = hasSetLevel ? ( ST ? theVent.currentValue('level') : theVent.currentValue('level', true) ) : ((currentSwitch == 'on') ? 100 : 0)
    if (maximumVentLevel >= 99) {
      	if (currentSwitch == 'off') {
            if (hasSetLevel) {
                if (currentLevel.toInteger() < 99) { theVent.setLevel(100) } //some vents don't handle '100'
                if (theVent.hasCommand('refresh')) theVent.refresh()
                currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
                if (currentSwitch != 'on') theVent.on()						// setLevel will turn on() for some devices, but not all
                changed = true
            } else {
                theVent.on()
                changed = true
            }
        	if (changed) {
            	if (theVent.hasCommand('refresh')) theVent.refresh()
            	LOG("${hasSetLevel?'Opening':'Turning on'} ${theVent.displayName}",3,null,'info')
        	} else {
            	LOG("${theVent.displayName} is already ${hasSetLevel?'open':'on'}",3,null,'info')
        	}
        }
    } else {
    	// New feature: use configured maximum level
        if (hasSetLevel) {
        	if (currentLevel.toInteger() != maximumVentLevel) {
        		theVent.setLevel(maximumVentLevel)	// make sure none of the vents are less than the specified minimum
            	LOG("Opening ${theVent.displayName} to ${maximumVentLevel}%",3,null,'info')
                if (theVent.hasCommand('refresh')) theVent.refresh()
        	} else {
        		LOG("${theVent.displayName} is already open to ${maximumVentLevel}%",3,null,'info')
        	}
		} else {
			if (currentSwitch == 'off') {
				theVent.on()
				LOG("Turning on ${theVent.displayName}",3,null,'info')
			} else {
				LOG("${theVent.displayName} is already on",3,null,'info')
			}
        }
    }
}
// Helper Functions
void updateMyLabel() {
	boolean ST = atomicState.isST
	def opts = [' (pa', ' (op', ' (cl']
	String flag
	if (ST) {
		opts.each {
			if (!flag && app.label.contains(it)) flag = it
		}
	} else {
		flag = '<span '
	}
	
	// Display Ecobee connection status as part of the label...
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		// strip off any connection status tag
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	if (settings.tempDisable) {
		def newLabel = myLabel + (!ST ? '<span style="color:red"> (paused)</span>' : ' (paused)')
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (atomicState.ventState == 'open') {
		def newLabel = myLabel + (!ST ? '<span style="color:green"> (open)</span>' : ' (open)')
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (atomicState.ventState == 'closed') { 
		def newLabel = myLabel + (!ST ? '<span style="color:green"> (closed)</span>' : ' (closed)')
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
// SmartThings/Hubitat Portability Library (SHPL)
String  getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST()     { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
boolean getIsHE()     { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
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
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"	
}
