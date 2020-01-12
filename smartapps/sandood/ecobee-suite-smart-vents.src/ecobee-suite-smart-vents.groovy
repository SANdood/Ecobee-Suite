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
 *	1.7.15 - More bugs squashed, settings page cleaned up
 *	1.7.16 - Fixed vents not changing 
 *	1.7.17 - Fixed vents not changing when both minLevel & maxLevel are set
 *	1.7.18 - Added conditional support for "contact sensor" capability, so vents show logical state in HomeKit (as blinds)
 *	1.7.19 - Fixed helper labelling
 *	1.7.20 - Added support for HubConnected EcoVents and Keen Vents; optimized Keen Vent handling; new Fan Only percentage setting
 *	1.7.21 - Fix HubConnect EcoVent selector
 *	1.7.22 - Added optional Ecobee Programs selections & auto Sensor enrollments; show open percentage in label
 *	1.7.23 - Enable Smart Recovery handling when *nextClimate* is enabled (but currently running Program is not), default min/maxVentLevel to 1/98 for silent operation
 */
String getVersionNum() 		{ return "1.7.23" }
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
    importUrl:		"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-vents.src/ecobee-suite-smart-vents.groovy",
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
    def unit = getTemperatureScale()
	
	dynamicPage(name: "mainPage", title: (HE?'<b>':'') + "${getVersionLabel()}" + (HE?'</b>':''), uninstall: true, install: true) {
    	section(title: "") {
        	String defaultName = "Smart Vents & Switches"
			String defaultLabel = atomicState?.appDisplayName ?: defaultName
			String oldName = app.label
            log.debug "old ${oldName}"
			input "thisName", "text", title: "Name for this ${defaultName} Helper", submitOnChange: true, defaultValue: defaultLabel
			if ((!oldName && settings.thisName) || (oldName && settings.thisName && (settings.thisName != oldName))) {
            log.debug "update ${settings.thisName} ${app.label}"
				app.updateLabel(settings.thisName)
				atomicState.appDisplayName = settings.thisName
			} else if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			}
			updateMyLabel()
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
				def opts = [' (paused)', ' (open)', ' (closed)']
				String flag
				opts.each {
					if (!flag && app.label.contains(it)) flag = it
				}
				if (flag) {
					if (atomicState?.appDisplayName != null) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
                        app.label.substring(0, app.label.indexOf(flag))
                        atomicState.appDisplayName = myLabel
                        app.updateLabel(myLabel)
                    }
                } else {
                	atomicState.appDisplayName = app.label
                }
            }
			updateMyLabel()
        }
        
        if (settings.tempDisable) {
           	section(title: (HE?'<b>':'') + "WARNING: Temporarily Paused - re-enable below." + (HE?'</b>':'')) {}
        } else {
        	section(title: (HE?'<b>':'') + "Temperature Sensors" + (HE?'</b>':'')) {
        		paragraph("Select temperature sensors for this Helper. If you select multiple sensors, the temperature will be averaged across all of them.") 
        		input(name: "theSensors", type:"capability.temperatureMeasurement", title: "Use which Temperature Sensor(s)", required: true, multiple: true, submitOnChange: true)
				if (settings.theSensors) paragraph "The current ${settings.theSensors?.size()>1?'average ':''}temperature for ${settings.theSensors?.size()==1?'this sensor':'these sensors'} is ${getAverageTemperature()}°"
            }
		}
        
        if (!settings.tempDisable && settings?.theSensors) {      
        	section(title: (HE?'<b>':'') + "Automated Vents" + (HE?'</b>':'')) {
        		paragraph("Selected vents will be opened while the HVAC system is heating or cooling until target temperature is achieved, and then closed")
				input(name: "theEconetVents", type: "${ST?'device.econetVent':'device.EcoNetVent'}", title: "Control which EcoNet Vent(s)?", multiple: true, submitOnChange: true, 
                	  hideWhenEmpty: true, required: (!settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
                if (settings.theEconetVents) vc = settings.theEconetVents.size()
                input(name: "theHCEcoVents", type: "${ST?'device.hubconnectEcovent':'device.HubConnectEcoVent'}", title: "Control which HubConnect EcoNet Vent(s)?", multiple:true, 
                	  submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
            	if (settings.theHCEcoVents) vc = vc + settings.theHCEcoVents.size()
				input(name: "theKeenVents", type: "${ST?'device.keenHomeSmartVent':'device.KeenHomeSmartVent'}", title: "Control which Keen Home Smart Vent(s)?", multiple:true, 
                	  submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
            	if (settings.theKeenVents) vc = vc + settings.theKeenVents.size()
                input(name: "theHCKeenVents", type: "${ST?'device.hubconnectKeenHomeSmartVent':'device.HubConnectKeenHomeSmartVent'}", title: "Control which HubConnect Keen Home Smart Vent(s)?", multiple:true, 
                	  submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
            	if (settings.theHCKeenVents) vc = vc + settings.theHCKeenVents.size()
                input(name: "theGenericVents", type: 'capability.switchLevel', title: "Control which Generic (dimmer) Vent(s)?", multiple: true, submitOnChange: true, hideWhenEmpty: true, 
                	  required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericSwitches))
                if (settings.theGenericVents) vc = vc + settings.theGenericVents.size()
				input(name: "theGenericSwitches", type: 'capability.switch', title: "Control which Switch(es)?", multiple: true, submitOnChange: true, hideWhenEmpty: true,
                	  required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents))
                if (settings.theGenericSwitches) vc = vc + settings.theGenericSwitches.size()
                def s = ((vc == 0) || (vc > 1)) ? 's' : ''
                paragraph "${vc} vent${s}/switch${s=='s'?'es':''} selected"
                
            	if (settings.theEconetVents || settings.theHCEcoVents || settings.theKeenVents || settings.theHCKeenVents || settings.theGenericVents ) {
            		paragraph('The default settings are optimized for silent operation of most vents. However, fully closing too many vents at once may be detrimental to your HVAC system. ' +
                    		  'You may want to increase the minimum closed percentage.')
            		input(name: "minimumVentLevel", type: "number", title: "Minimum vent level when closed?", required: true, defaultValue:1, description: '1', range: "0..100")
                    input(name: "maximumVentLevel", type: "number", title: "Maximum vent level when open?", required: true, defaultValue:98, description: '98', range: "0..100")
            	}
				if (HE) paragraph ''
        	}
            
            section(title: (HE?'<b>':'') + "Windows & Doors" + (HE?'</b>':'')) {
        		paragraph("Open Windows and Doors will temporarily deactivate (close) the vent${vc>1?'s':''}, except during 'Fan Only'")
            	input(name: "theWindows", type: "capability.contactSensor", title: "Monitor these Window/Door contact sensor(s)? (optional)", required: false, multiple: true)
				if (HE) paragraph ''
        	}
        
			section(title: (HE?'<b>':'') + "Ecobee Suite Thermostat Integration" + (HE?'</b>':'')) {
				paragraph("Specify which thermostat to monitor for operating state, mode and setpoint change events")
				input(name: "theThermostat", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Select thermostat",
					  multiple: false, required: true, submitOnChange: true)
            	paragraph "If you want the vents to be adjusted at any time, based entirely on the room temperature and target setpoints (ignoring the thermostat's operating state), enable this option"
            	input(name: "adjustAlways", type: 'bool', title: "Always adjust vents/switches?", defaultValue: false, submitOnChange: true)
                if (settings.adjustAlways) paragraph "Note that this Helper will still use the thermostat to determine whether to use heating or cooling setpoint targets"
                
                def programs = getEcobeePrograms()
                programs = programs + ["Vacation"]
                input(name: "theClimates", type: 'enum', title: "Make vent adjustments only during these Ecobee Programs (optional)", required: false, submitOnChange: true, multiple: true, options: programs)
                if (settings?.theClimates && (settings?.theClimates.size() != programs.size())) {
                	input(name: 'notClimateState', type: 'enum', title: "Vent state for excluded Ecobee Programs?", required: true, submitOnChange: true, defaultValue: 'unchanged',
                		  options: ['open', 'closed', 'percentage', 'unchanged'], multiple: false)
                	if (notClimateState == 'percentage') {
                		input(name: 'notClimateLevel', type: "number", title: "Vent level?", required: true, defaultValue:50, description: '50', range: "0..100")
                    }
                }
                
                if (settings.theSensors) {
                	def ecobeeSensors = []
                    settings.theSensors.each { 
                    	if (it.hasCommand('addSensorToProgram')) {
                        	ecobeeSensors << it
                        }
                    }
                	if (ecobeeSensors.size()) {
                		if (settings.theClimates) input(name: "enrollClimates", type: 'bool', title: "Automatically include Ecobee Sensor${(ecobeeSensors.size()>1)?'s':''} in above Programs?",
                        								defaulValue: true, submitOnChange: true)     
                        if (settings.enrollClimates) {
                        	atomicState.notTheseClimates = programs - theClimates - ['Vacation']
                            //def these = settings.theClimates.contains(['Vacation']) ? settings.theClimates - ['Vacation'] : settings.theClimates
                            def these = settings.theClimates - ['Vacation']
                            String notThese = ''
                            if (atomicState.notTheseClimates) notThese = " and removed from (" + atomicState.notTheseClimates.toString()[1..-2].replace(',',', ') + ')'
                        	paragraph "${ecobeeSensors.toString()[1..-2]} will be added to (${these.toString()[1..-2].replace('"','').replace(',',', ')})${notThese} for ${theThermostat.displayName}. " +
                        									"${ecobeeSensors.size()>1?'They':'It'} will also be removed from ALL Programs on ${theThermostat.displayName} when this Helper is Paused."
                    	}
                    }
                }
				if (HE) paragraph ''
			}
		
			section(title: (HE?'<b>':'') + "Target Temperature" + (HE?'</b>':'')) {
            	def cSetpoint
				def cHeatSetpoint
				def cCoolSetpoint
				def cMode
                def cProgram
            	if (settings.useThermostat && settings.theThermostat) {
					cSetpoint = 	ST ? settings.theThermostat.currentValue('thermostatSetpoint') 	: 	settings.theThermostat.currentValue('thermostatSetpoint', true)
					cHeatSetpoint = ST ? settings.theThermostat.currentValue('heatingSetpoint') 	: 	settings.theThermostat.currentValue('heatingSetpoint', true)
					cCoolSetpoint = ST ? settings.theThermostat.currentValue('coolingSetpoint') 	: 	settings.theThermostat.currentValue('coolingSetpoint', true)
					cMode = 		ST ? settings.theThermostat.currentValue('thermostatMode') 		: 	settings.theThermostat.currentValue('thermostatMode', true)
                    cProgram = 		ST ? settings.theThermostat.currentValue('currentProgramName') 	: 	settings.theThermostat.currentValue('currentProgramName', true)
					paragraph("${settings.theThermostat} is in '${cMode}' mode running the '${cProgram}' program. The heating setpoint is ${cHeatSetpoint}°${unit}, " +
                    		  "the cooling setpoint is ${cCoolSetpoint}°${unit}, and the thermostat setpoint is ${cSetpoint}°${unit}")
                }
				input(name: "useThermostat", type: "bool", title: "Follow the setpoints on ${settings.theThermostat?' '+settings.theThermostat.displayName:'the thermostat'}?", required: true, 
					  defaultValue: true, submitOnChange: true)
                def heatAt = null
                def coolAt = null
				if (!settings.useThermostat) {
					input(name: "heatingSetpoint", type: "decimal", title: "Target heating setpoint?", required: true, submitOnChange: true)
					input(name: "coolingSetpoint", type: "decimal", title: "Target cooling setpoint?", required: true, submitOnChange: true)
                    if (settings.heatingSetpoint) heatAt = settings.heatingSetpoint
                    if (settings.coolingSetpoint) coolAt = settings.coolingSetpoint
				} else {
                	paragraph "Setpoint offsets are ADDED to the current thermostat's heating/cooling setpoint. Use negative numbers to reduce the target setpoint, positive to increase it."
                	input(name: "heatOffset", type: "decimal", title: "Heating Setpoint Offset?", defaultValue: 0.0, required: true, range: "-10..10", submitOnChange: true)
					input(name: "coolOffset", type: "decimal", title: "Cooling Setpoint Offset?", defaultValue: 0.0, required: true, range: "-10..10", submitOnChange: true)
                    if (!settings.heatOffset) {settings.heatOffset = 0.0; app.updateSetting('heatOffset', 0.0); }
                    if (!settings.coolOffset) {settings.coolOffset = 0.0; app.updateSetting('coolOffset', 0.0); }
                    if (cHeatSetpoint && (settings.heatOffset != null)) heatAt = cHeatSetpoint + settings.heatOffset
                    if (cCoolSetpoint && (settings.coolOffset != null)) coolAt = cCoolSetpoint + settings.coolOffset
				}
                if (heatAt && coolAt) paragraph "In the '${cProgram}' program, the vent${vc>1?'s':''} will open when the observed temperature at the selected " +
                								"sensor${settings.theSensors?.size()>1?'s':''} is less than ${heatAt}°${unit} or more than ${coolAt}°${unit}"
                
    		}
            section(title: (HE?'<b>':'') + "Fan Only State" + (HE?'</b>':'')) {
				//if (false) input(name: 'closedFanOnly', type: 'bool', title: "Close the vent${vc>1?'s':''} while HVAC is 'fan only'?", defaultValue: false)
                String foDefault = (settings?.closedFanOnly != null) ? (settings.closedFanOnly ? 'closed' : 'unchanged') : 'unchanged'
                input(name: 'fanOnlyState', type: 'enum', title: "Vent state for 'Fan Only' operation?", required: true, submitOnChange: true, defaultValue: foDefault,
                	  options: ['open', 'closed', 'percentage', 'unchanged'], multiple: false)
                if (fanOnlyState == 'percentage') {
                	input(name: 'fanOnlyLevel', type: "number", title: "Vent level?", required: true, defaultValue:50, description: '50', range: "0..100")
                }
				if (HE) paragraph ''
			}
        } else { 
        	if (settings.theEconetVents || settings.theHCEcoVents || settings.theKeenVents || settings.theHCKeenVents || settings.theGenericVents || settings.theGenericSwitches) {
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
    unenrollSensors()
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
    boolean ST = atomicState.isST
    boolean HE = !ST

    // Housekeeping
    if (settings.closedFanOnly != null) {
    	if (settings.fanOnlyState == null) {
        	def vs = settings.closedFanOnly ? 'closed' : 'unchanged'
            settings.fanOnlyState = vs
            app.updateSetting('fanOnlyState', vs)
        }
        settings.closedFanOnly = null
        if (HE) app.removeSetting('closedFanOnly')
    }
    atomicState.version = getVersionLabel()
    atomicState.scheduled = false
    // Now, just exit if we are disabled...
	if (settings.tempDisable) {
        if (settings.disabledVents && (settings.disabledVents != 'unchanged')) {
			(settings.disabledVents.startsWith('open')) ? setTheVents('open') : setTheVents('closed') 
            LOG("Temporarily Paused, setting vents to ${settings.disabledVents}.", 3, null, 'info')
        } else {
        	LOG("Temporarily Paused, vents unchanged", 3, null, 'info')
        }
		updateMyLabel()
        if (settings.theClimates && settings.enrollClimates) unenrollSensors()
        return true
    }
	if (settings.debugOff) log.info "log.debug() logging disabled"
	
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])

    subscribe(theSensors, 		'temperature', 				changeHandler)	
	subscribe(theThermostat, 	'thermostatOperatingState', changeHandler)
    //subscribe(theThermostat, 	'temperature', 				changeHandler)
	subscribe(theThermostat,	'thermostatMode', 			changeHandler)
    //subscribe(theThermostat,	'currentProgram',			changeHandler)
    //subscribe(theVents, 		'level', 					changeHandler)
    //subscribe(theVents, 		'switch', 					changeHandler)
	if (theWindows) subscribe(theWindows, "contact",		changeHandler)
    if (useThermostat) {
    	subscribe(theThermostat, 'heatingSetpoint', 		changeHandler)
        subscribe(theThermostat, 'coolingSetpoint', 		changeHandler)
    //  subscribe(theThermostat, 'thermostatSetpoint', 		changeHandler)
    }
    
    // These don't change much, and we need them frequently, so stash them away and track their changes
    atomicState.heatDifferential = theThermostat.currentValue('heatDifferential')
    atomicState.coolDifferential = theThermostat.currentValue('coolDifferential') 
    atomicState.dehumidifyOvercoolOffset = theThermostat.currentValue('dehumidifyOvercoolOffset')
    subscribe(theThermostat, 'heatDifferential', atomicHandler)
    subscribe(theThermostat, 'coolDifferential', atomicHandler)
    subscribe(theThermostat, 'dehumidifyOvercoolOffset', atomicHandler)
    
	atomicState.currentStatus = [:]
	atomicState.heatOrCool = null

	if (settings.theClimates && settings.enrollClimates) enrollSensors()
    
    if (atomicState.lastLevel == null) {
    	String ventState = atomicState.ventState
    	if (ventState != null) {
        	ventState = ventState.isNumber()? "${ventState}%" : (ventState == 'open'? "${settings.maximumVentLevel}%" : "${settings.minimumVentLevel}%")
            atomicState.lastLevel = ventState
        }
    }
    
    setTheVents(checkTemperature())
	updateMyLabel()
    return true
}

void atomicHandler(evt) {
	atomicState."${evt.name}" = evt.value		// Clever, no?
}

def enrollSensors() {
	def programs = getEcobeePrograms() // + ["Vacation"]	// Can't enroll in Vacation
    settings.theSensors.each { sensor ->
    	if (sensor.hasCommand('addSensorToProgram')) {
    		programs.each { climate ->
    			if (settings.theClimates.contains(climate)) {
					sensor.addSensorToProgram(climate)
                    LOG("added to ${climate}",3,null,'info')
                } else {
                	sensor.deleteSensorFromProgram(climate)
                    LOG("removed from ${climate}",3,null,'info')
                }
            }
        } 
    }
}

def unenrollSensors() {
	def programs = getEcobeePrograms() // + ["Vacation"]	// Can't enroll in Vacation
    settings.theSensors.each { sensor ->
    	if (sensor.hasCommand('deleteSensorFromProgram')) {
    		programs.each { climate ->
               	sensor.deleteSensorFromProgram(climate)
                LOG("removed from ${climate}",3,null,'info')
            }
        } 
    }
}

void changeHandler(evt) {
	//log.debug "changeHandler(): ${evt.displayName} ${evt.name} ${evt.value}"
	updateTheVents()
	runIn( 2, checkAndSet, [overwrite: true])		// collapse multiple interelated events into a single thread
}

void checkAndSet() {
	if (!atomicState.version || (atomicState.version != getVersionLabel())) {
    	LOG('Helper version changed, re-initializing...',1,null,'info')
    	updated()
    }
	setTheVents(checkTemperature())
}

String checkTemperature() {
	boolean ST = atomicState.isST
    
    // Check if we're supposed to do anything during the currently active Ecobee Program (or the upcoming Program if in Smart Recovery)
    def currentProgram = ST ? theThermostat.currentValue('currentProgram') 		: theThermostat.currentValue('currentProgram', true)
    if (settings.theClimates && !settings.theClimates.contains(currentProgram)) {
    	// If we are in (Smart Recovery), check to see if we're supposed to be adjusting during the upcoming program
    	def nextProgram = ST ? theThermostat.currentValue('nextProgramName') 	: theThermostat.currentValue('nextProgramName', true)
        if (!nextProgram || !settings.theClimates.contains(nextProgram)) {
            LOG("${theThermostat.displayName} is currently running a Program (${currentProgram}) that we're not configured for (${settings.theClimates.toString()[1..-2]})",3,null,'info')
            if (settings.notClimateState) {
                if (settings.notClimateState != 'percentage') {
                    return settings.notClimateState
                } else {
                    return settings.notClimateLevel ?: 50
                }
            }
        }
    }
    
    def lastHVAC = atomicState.lastHVAC
    
    def cTemp = getAverageTemperature()
    def vents = 'unchanged'			// if not heating/cooling/fan, then no change to current vents
    if (cTemp != null) {		// only if valid temperature readings (Ecosensors can return "unknown")
        // Be smarter if we are in Smart Recovery mode: follow the thermostat's temperature instead of watching the current setpoint. Otherwise the room won't get the benefits of heat/cool
        // Smart Recovery. Also, we add the heat/cool differential to try and get ahead of the Smart Recovery curve (otherwise we close too early or too often)

        String smarter = 	ST ? theThermostat.currentValue('thermostatOperatingStateDisplay')	: theThermostat.currentValue('thermostatOperatingStateDisplay', true)
        boolean beSmart = 	(smarter?.contains('mart'))	// "(Smart Recovery)"
        boolean beCool = (smarter?.contains('verc'))	// "(Overcooling)"
        String cOpState = 	ST ? theThermostat.currentValue('thermostatOperatingState') 		: theThermostat.currentValue('thermostatOperatingState', true)
        def cTemperature = 	ST ? theThermostat.currentValue('temperature')						: theThermostat.currentValue('temperature', true)
        def coolSP = 		ST ? theThermostat.currentValue('coolingSetpoint') 					: theThermostat.currentValue('coolingSetpoint', true)
        def heatSP = 		ST ? theThermostat.currentValue('heatingSetpoint') 					: theThermostat.currentValue('heatingSetpoint', true)
        String cMode = 		ST ? theThermostat.currentValue('thermostatMode') 					: theThermostat.currentValue('thermostatMode', true)
        def nextHeatSP
        def nextCoolSP
        if (settings.useThermostat) {
        	nextHeatSP = 	ST ? theThermostat.currentValue('nextHeatingSetpoint') : theThermostat.currentValue('nextHeatingSetpoint', true)
        	nextCoolSP = 	ST ? theThermostat.currentValue('nextCoolingSetpoint') : theThermostat.currentValue('nextCoolingSetpoint', true)       
        }
        def currentStatus = [s: smarter, bs: beSmart, bc: beCool, op: cOpState, md: cMode, t: cTemp, h: heatSP, c: coolSP, nh: nextHeatSP, nc: nextCoolSP] as HashMap
        if (cOpState == 'heating') { atomicState.heatOrCool = 'heat' } else if (cOpState == 'cooling') { atomicState.heatOrCool == 'cool' }
        if (atomicState.heatOrCool == null) atomicState.heatOrCool = cMode
        
        if (atomicState.currentStatus == currentStatus) { 
            LOG("Status unchanged...",3,null,'info')
            return 'unchanged'			// ignore - identical to last time
        } else {
            atomicState.currentStatus = currentStatus
            LOG("currentStatus: ${currentStatus}",3,null,'info')
        }
        def offset 
    	if ((cOpState == 'heating') || (settings.adjustAlways && (atomicState.heatOrCool == 'heat'))) {
        	offset = settings.heatOffset ?: 0.0
    		def heatTarget 
            if (useThermostat) {
            	if (beSmart) {
                	heatTarget = nextHeatSP + offset // + atomicState.heatDifferential
                } else {
                	heatTarget = heatSP + offset // + atomicState.heatDifferential
                }
            } else {
            	heatTarget = settings.heatingSetpoint
            }
			vents = (cTemp <= heatTarget) ? 'open' : 'closed'
        	LOG("${theThermostat.displayName} is heating, target temperature is ${heatTarget}°${beSmart?' (smart recovery)':''}, room temperature is ${cTemp}°",3,null,'info')
    	} else if ((cOpState == 'cooling') || (settings.adjustAlways && (atomicState.heatOrCool == 'cool'))) {
        	offset = settings.coolOffset ?: 0.0
            def coolTarget 
            if (useThermostat) {
            	if (beSmart) {
                	coolTarget = nextCoolSP + offset // - atomicState.coolDifferential
                } else if (beCool) {
                	coolTarget = nextCoolSP + offset - atomicState.coolDifferential //- atomicState.dehumidifyOvercoolOffset
                } else {
                	coolTarget = coolSP + offset //- atomicState.coolDifferential
                }
            } else {
            	coolTarget = settings.coolingSetpoint
            }
			vents = (cTemp >= coolTarget) ? 'open' : 'closed'
        	LOG("${theThermostat.displayName} is cooling, target temperature is ${coolTarget}°${beSmart?' (smart recovery)':''}${beCool?' (overcooling)':''}, room temperature is ${cTemp}°",3,null,'info')
		} else if (cOpState == 'idle') {
    		LOG("${theThermostat.displayName} is idle, room temperature is ${cTemp}°, vents-->unchanged",3,null,'info')
            vents = 'unchanged'	// fix it next time the fan goes on
		} else if (/*(vents == 'unchanged') && */(cOpState == 'fan only')) {
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
                if (!vents) vents = 'unchanged'
                if (vents == 'percentage') vents = (settings.fanOnlyLevel ?: 50) as Integer
                LOG("${theThermostat.displayName} is running 'Fan Only', room temperature is ${cTemp}°, vents-->${vents}${settings?.fanOnlyState == 'percentage'?'%':''}",3,null,'info')
            }
		}    
		if ((vents == 'open') && (cOpState == 'fan only')) { // let 'Fan Only' run even if windows are open || (vents.isNumber())) {
        	if (settings.theWindows) {
            	boolean openWindows = ST ? settings.theWindows*.currentValue('contact').contains('open') : settings.theWindows*.currentValue('contact', true).contains('open')
                if (openWindows) {
					vents = 'closed'	// but if a window is open, close the vents
        			LOG("${(theWindows.size()>1)?'A':'The'} window/contact is open",3,null,'info')
                }
            }
    	}
		LOG("Vents should be ${vents?:'unchanged'}",3,null,'info')
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
	if (!ventState) ventState = 'unchanged'
	if (ventState == 'open') {
        allVentsOpen()
    } else if (ventState == 'closed') {
        allVentsClosed()
	} else if (ventState.isNumber()) {
    	allVentsLevel(ventState as Integer)
    } else if (ventState == 'unchanged') {
    	boolean ST = atomicState.isST	
    	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    					(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                    	 (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
        def ventSwitch = ST ? theVents[0].currentValue('switch') : theVents[0].currentValue('switch', true) // assumption: all vents are in the same state
        if (ventSwitch == 'on') {
            ventState = 'open'
        	def hasLevel = theVents[0].hasAttribute('level')
            if (hasLevel) {
        		def currentLevel = (ST ? theVents[0].currentValue('level') : theVents[0].currentValue('level', true))?.toInteger()
        		if (currentLevel <= minimumVentLevel.toInteger()) {
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
	if (ventState && (atomicState.ventState != ventState)) {
    	atomicState.ventState = ventState
        runIn(2, updateTheVents, [overwrite: true])
		updateMyLabel()
    }
}

void updateTheVents() {
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				// (settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + // Don't ping the Keen Vents...
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
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
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    //LOG("Opening the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents.each { ventOn(it) }
    if (theVents*.hasCommand('setLevel')) {
    	atomicState.lastLevel = "${settings.maximumVentLevel ?: 98}%"
    } else {
    	atomicState.lastLevel = 'on'
    }
}

void allVentsClosed() {
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    //LOG("Closing the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents.each { ventOff(it) }
    if (theVents*.hasCommand('setLevel')) {
    	atomicState.lastLevel = "${settings.minimumVentLevel ?: 1}%"
    } else {
    	atomicState.lastLevel = 'off'
    }
}

void allVentsLevel(level) {
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    //LOG("Opening the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents.each { ventLevel(it, level) }
    atomicState.lastLevel = "${level}%"
}

void ventOff( theVent ) {
	boolean ST = atomicState.isST	
    
	def hasSetLevel = (theVent.hasCapability('switchLevel') || theVent.hasCommand('setLevel'))
    def minVentLevel = (settings.minimumVentLevel ?: 1) as Integer
    if (minVentLevel == 0) {
    	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
      	if (currentSwitch == 'on') {
			if (hasSetLevel) {
        		theVent.off()
               	if (!settings?.theKeenVents?.contains(theVent) && !settings?.theHCKeenVents?.contains(theVent)) {
                    // Don't refresh the Keen Vents - they are quite responsive
                    if (theVent.hasCommand('refresh')) theVent.refresh()
                }
                def currentLevel = ST ? theVent.currentValue('level') : theVent.currentValue('level', true)
                // Some vents will leave the level set even when switch is off
				if (currentLevel?.toInteger() != 0) theVent.setLevel(0) // Belt & suspenders - make sure the level is reset to 0
            	LOG("Closing ${theVent.displayName}",3,null,'info')
			} else {
				theVent.off()
				LOG("Turning off ${theVent.displayName}",3,null,'info')
			}
            //if (theVent.hasCommand('refresh')) theVent.refresh()
        } else {
        	LOG("${theVent.displayName} is already closed/off",3,null,'info')
        }
    } else {
		if (hasSetLevel) {
        	def currentLevel = ST ? theVent.currentValue('level') : theVent.currentValue('level', true)
			if (currentLevel?.toInteger() != minVentLevel) {
        		theVent.setLevel(minVentLevel)	// make sure none of the vents are less than the specified minimum
            	LOG("Closing ${theVent.displayName} to ${minVentLevel}%",3,null,'info')
               // if (theVent.hasCommand('refresh')) theVent.refresh()
        	} else {
        		LOG("${theVent.displayName} is already closed to ${minVentLevel}%",3,null,'info')
        	}
		} else {
        	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
			if (currentSwitch == 'on') {
				theVent.off()
				LOG("Turning off ${theVent.displayName}",3,null,'info')
                //if (theVent.hasCommand('refresh')) theVent.refresh()
			} else {
				LOG("${theVent.displayName} is already off",3,null,'info')
			}
		}
    }
    // Display the contact as "closed", even if we are partially open (so that HomeKit shows open/closed Blinds)
    if (theVent.hasAttribute('contact') && theVent.hasCommand('closeContact') && (theVent.currentValue('contact') != 'closed')) theVent.closeContact()
}

void ventOn( theVent ) {
	boolean ST = atomicState.isST
    boolean changed = false
    def hasSetLevel = (theVent.hasCapability('switchLevel') || theVent.hasCommand('setLevel'))
    def maxVentLevel = (settings.maximumVentLevel ?: 98) as Integer
    def minVentLevel = (settings.minimumVentLevel ?: 1) as Integer
	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
    def currentLevel = (hasSetLevel ? ( ST ? theVent.currentValue('level') : theVent.currentValue('level', true) ) : ((currentSwitch == 'on') ? 100 : 0)) as Integer
    if (maxVentLevel >= 99) {
      	if ((currentSwitch == 'off') || (currentLevel < maxVentLevel)) {
            if (hasSetLevel) {
                if (currentLevel.toInteger() != maxVentLevel) { theVent.setLevel(maxVentLevel) } //some vents don't handle '100'
                if (!settings?.theKeenVents?.contains(theVent) && !settings?.theHCKeenVents?.contains(theVent)) {
                    // Don't refresh the Keen Vents - they are quite responsive
                    if (theVent.hasCommand('refresh')) theVent.refresh()
                }
                currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
                if (currentSwitch != 'on') theVent.on()						// setLevel will turn on() for some devices, but not all
                changed = true
            } else {
                theVent.on()
                changed = true
            }
        	if (changed) {
            	//if (theVent.hasCommand('refresh')) theVent.refresh()
            	LOG("${hasSetLevel?'Opening':'Turning on'} ${theVent.displayName}",3,null,'info')
        	} else {
            	LOG("${theVent.displayName} is already ${hasSetLevel?'open':'on'}",3,null,'info')
        	}
        }
    } else {
    	// New feature: use configured maximum level
        if (hasSetLevel) {
        	if (currentLevel != maxVentLevel) {
        		theVent.setLevel(maxVentLevel)	// make sure none of the vents are less than the specified minimum
            	LOG("Opening ${theVent.displayName} to ${maxVentLevel}%",3,null,'info')
                //if (theVent.hasCommand('refresh')) theVent.refresh()
        	} else {
        		LOG("${theVent.displayName} is already open to ${maxVentLevel}%",3,null,'info')
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
    // Display the contact as "open", even if we are only partially open (so that HomeKit shows open/closed Blinds)
    if (theVent.hasAttribute('contact') && theVent.hasCommand('openContact') && (theVent.currentValue('contact') != 'open')) theVent.openContact()
}

void ventLevel( theVent, level=50 ) {
	boolean ST = atomicState.isST
    if (level == 0) {
    	ventOff(theVent)
        return
    }
    boolean changed = false
    def hasSetLevel = (theVent.hasCapability('switchLevel') || theVent.hasCommand('setLevel'))
    def maxVentLevel = (settings.maximumVentLevel ?: 98) as Integer
    def minVentLevel = (settings.minimumVentLevel ?: 1) as Integer
    if (level > maxVentLevel) level = maxVentLevel
    if (level < minVentLevel) level = minVentLevel
    def currentLevel = (hasSetLevel ? ( ST ? theVent.currentValue('level') : theVent.currentValue('level', true) ) : ((currentSwitch == 'on') ? 100 : 0)) as Integer
	if (hasSetLevel) {
    	if (currentLevel != level) {
        	theVent.setLevel(level)
            changed = true
        }
    	if (!settings?.theKeenVents?.contains(theVent) && !settings?.theHCKeenVents?.contains(theVent)) {
    		// Don't refresh the Keen Vents - they are quite responsive
    		if (theVent.hasCommand('refresh')) theVent.refresh()
    	}
    }
    def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
    if (currentSwitch != 'on') {
    	theVent.on()						// setLevel will turn on() for some devices, but not all
    	changed = true
    }
    if (hasSetLevel) {
    	if (changed) {
            LOG("Opening ${theVent.displayName} to ${level}%",3,null,'info')
        } else {
        	LOG("${theVent.displayName} is already open to ${level}%",3,null,'info')
        }
    } else {
        if (changed) {
            LOG("Turning on ${theVent.displayName}",3,null,'info')
        } else {
            LOG("${theVent.displayName} is already on",3,null,'info')
        }
    }
    // Display the vent's contact as "open", even if we are only partially open (so that HomeKit shows open/closed Blinds)
    if (theVent.hasAttribute('contact') && theVent.hasCommand('openContact') && (theVent.currentValue('contact') != 'open')) theVent.openContact()
}
// Helper Functions
def getEcobeePrograms() {
	def programs
	def pl = settings?.theThermostat?.currentValue('programsList')
	if (pl) programs = new JsonSlurper().parseText(pl)
	if (!programs) programs =  ['Away', 'Home', 'Sleep']
    LOG("getEcobeePrograms: returning ${programs}", 4, null, 'info')
    return programs.sort(false)
}

void updateMyLabel() {
	boolean ST = atomicState.isST
	def opts = [' (paused', ' (open', ' (closed']
	String flag
	if (ST) {
		opts.each {
			if (!flag && app.label.contains(it)) flag = it
		}
	} else {
		flag = '<span '
	}
	
	// Display vent status 
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
    log.debug app.label
		myLabel = app.label
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		// strip off any connection status tag
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
    String cLevel = atomicState.lastLevel
	if (settings.tempDisable) {
    	cLevel = ' (paused ' + cLevel + ')'
		def newLabel = myLabel + ( ST ? cLevel : '<span style="color:red">' + cLevel + '</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (atomicState.ventState == 'open') {
    	cLevel = ' (open ' + cLevel + ')'
		def newLabel = myLabel + ( ST ? cLevel : '<span style="color:green">' + cLevel + '</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (atomicState.ventState == 'closed') { 
    	cLevel = ' (closed ' + cLevel + ')'
		def newLabel = myLabel + ( ST ? cLevel : '<span style="color:green">' + cLevel + '</span>' )
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
