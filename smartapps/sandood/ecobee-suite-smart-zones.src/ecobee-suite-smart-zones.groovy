/**
 *  ecobee Suite Smart Zones
 *
 *  Copyright 2017-2020 Barry A. Burke
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
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - Merged in the 'One at a Time' logic to make this a multi-modal Smart Zones Helper
 *	1.8.03 - More busy bees
 *	1.8.04 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.05 - New SHPL, using Global Fields instead of atomicState
 *	1.8.06 - Allow individual un-pause from peers, even if was already paused
 *	1.8.07 - Better handling of null thermostatHold
 *	1.8.08 - Updated formatting; added Do Not Disturb Modes & Time window
 *	1.8.09 - Miscellaneous updates & fixes
 *  1.8.10 - Fix getThermostatModes() & getThermostatFanModes()
 *	1.8.11 - Fix for Hubitat 'supportedThermostatModes', etc.
 *	1.9.00 - Removed all ST code
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.9.00" }
String getVersionLabel() 	{ return "Ecobee Suite Smart Zones Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Smart Zones",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSynchronizes ecobee recirculation fan between two zones",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-zones.src/ecobee-suite-smart-zones.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-smart-zone-sa",
	singleInstance:		false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
	String defaultName = "Smart Zones"
    
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                paragraph("This multi-modal Helper will either synchronize Ecobee Suite thermostats' Operating State across a multi-zone HVAC system (in Cooperative Mode), or it will turn off all " +
                		  "idle thermostats whenever one starts heat/cooling/fan only (Isolated Mode)")
            }
        }
        
		section(title: sectionTitle("Naming${!settings.tempDisable?' & Thermostat Selection':''}")) {
			String defaultLabel
			if (!atomicState?.appDisplayName) {
				defaultLabel = defaultName
				app.updateLabel(defaultName)
				atomicState?.appDisplayName = defaultName
			} else {
				defaultLabel = atomicState.appDisplayName
			}
			label(title: inputTitle("Name for this ${defaultName} Helper"), required: false, submitOnChange: true, defaultValue: defaultLabel, width: 6)
            if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			} else {
            	atomicState.appDisplayName = app.label
            }
			if (app.label.contains('<span ')) {
				if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains('<span ')) {
					app.updateLabel(atomicState.appDisplayName)
				} else {
					String myLabel = app.label.substring(0, app.label.indexOf('<span '))
					atomicState.appDisplayName = myLabel
					app.updateLabel(myLabel)
				}
			} else {
				atomicState.appDisplayName = app.label
			}
        	if(settings.tempDisable) { 
				paragraph getFormat("warning","This Helper is temporarily paused...")
			} else {
            	paragraph("In the (traditional) Cooperative operating mode, this helper share the master thermostat's cool/heat/fan operating state across all the slave thermostats. " +
                		  "In the (new) 'Isolated' operating mode, this helper will turn off all of the other thermostats when any one begins heating/cooling/fan only.")
            	if (settings?.helperMode == null) { app.updateSetting('helperMode', 'cooperate'); settings?.helperMode = 'cooperate'; }
				
            	String defaultMode = (settings?.masterThermostat || (settings?.helperMode && (settings.helperMode == 'cooperate'))) ? "cooperate" : ""
            	input(name: "helperMode", type: "enum", title: inputTitle("Select desired operating mode for this Helper"), required: true, multiple: false, submitOnChange: true, defaultValue: defaultMode, 
                	  options: ["cooperate": "Cooperative", "isolate": "Isolated"])
                
            	if ((settings?.masterThermostat && !settings?.helperMode) || (settings?.helperMode && (settings.helperMode == 'cooperate'))) {
					input(name: "masterThermostat", type: 'device.EcobeeSuiteThermostat', title: inputTitle("Select Master Ecobee Suite Thermostat"), required: true, multiple: false, submitOnChange: true)
                } else {
                	input(name: "theThermostats", type: 'device.EcobeeSuiteThermostat', title: inputTitle("Select two or more Ecobee Suite Thermostats"), 
					  	  required: true, multiple: true, submitOnChange: true)
					if (settings?.theThermostats) {
						if (settings.theThermostats.size() == 1) paragraph getFormat("warning", "You must select at least 2 Ecobee Suite Thermostsats...")
					}
                }
			}            
		}
        // COOPERATIVE MODE SETTINGS
        if (!settings?.tempDisable && (settings?.helperMode == 'cooperate') && settings?.masterThermostat) {
        	section(title: sectionTitle('Cooperative Mode: Slaves')) {
				// Settings option for using Mode or Routine
				input(name: "slaveThermostats", title: inputTitle("Select Slave Ecobee Suite Thermostat(s)"), type: 'device.EcobeeSuiteThermostat', required: true, multiple: true, submitOnChange: true)
			}
			if (slaveThermostats) {
				section(title: sectionTitle("Cooperative Mode: Actions")) {
					input(name: 'shareHeat', title: inputTitle("Share ${masterThermostat.displayName} heating?"), type: "bool", required: true, defaultValue: false, submitOnChange: true, width: 4)
					input(name: 'shareCool', title: inputTitle("Share ${masterThermostat.displayName} cooling?"), type: "bool", required: true, defaultValue: false, submitOnChange: true, width: 4)
					if (!settings.shareHeat && !settings.shareCool && !settings.shareFan) {
						input(name: 'shareFan',  title: inputTitle("Share ${masterThermostat.displayName} fan only?"), type: "bool", required: true, defaultValue: true, submitOnChange: true, width: 4)
                        if (settings.shareFan == null) { app.updateSetting('shareFan', true); settings.shareFan = true; }
					} else {
						input(name: 'shareFan',  title: inputTitle("Share ${masterThermostat.displayName} fan only?"), type: "bool", required: true, /*defaultValue: true,*/ submitOnChange: true, width: 4)
					}
				}
			}
		} else if (!settings.tempDisable && (settings.helperMode == 'isolate') && settings?.theThermostats && (settings.theThermostats.size() > 1)) {
        // ISOLATED MODE SETTINGS
			section(title: sectionTitle("Isolated Mode: Actions")) {
				input(name: "busyStates", type: "enum", title: inputTitle("When any Thermostat's Operating State becomes one of these..."), submitOnChange: true, required: true, width: 6, multiple: true,
                	  options: ["cooling": "Cooling", "heating": "Heating", "fan only": "Fan Only"], defaultValue: "cooling")
				paragraph("", width: 6)
            	input(name: "hvacOff", type: "bool", title: inputTitle("Turn off the idle Thermostat${(settings?.theThermostats?.size() > 2)?'s':''}?"), defaultValue: false, submitOnChange: true, width: 4)
                if (settings?.hvacOff) {
                	input(name: "fanOff", type: "bool", title: inputTitle("Turn off the Fan on the idle systems?"), defaultValue: false, submitOnChange: true, width: 6)
                	paragraph("", width: 2)
                }
                def statModes = getThermostatModes()
                if (settings?.hvacOff) input(name: "hvacOn", type: "enum", title: inputTitle("Return HVAC to"), defaultValue: "Prior State", options: statModes, width: 4)
                if (settings?.fanOff) {
                	if (!settings?.hvacOff) paragraph("", width: 4)
                    def fanModes = getThermostatFanModes()
                    input(name: "fanOn", type: "enum", title: inputTitle("Return Fan to"), defaultValue: "Prior State", options: fanModes, width: 4)
                    if (settings?.fanOn == 'Circulate') input(name: "fanOnTime", type: "number", title: inputTitle("Fan Circulation Time (minutes)"), range: 1..55, defaultValue: 20, submitOnChange: true, width: 4)
                }
            }
            section (title: sectionTitle("Isolated Mode: Conditions")) {
            	paragraph "Choose one or more conditions; thermostat modes will be changed only when ALL of these are true"
            	if (!settings?.busyStates) { app.updateSetting("busyStates", "cool"); settings.busyStates = "cool"; }
                input(name: "actionDays", type: "enum", title: smallerTitle("Only on these days of the week"), multiple: true, required: false, submitOnChange: true,
                      options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"], width: 5)
            	paragraph(smallerTitle("Only during this time period:"), width: 3)
				
                input(name: "fromTime", type: "time", title: inputTitle("From:"), required: false, submitOnChange: true, width: 2)
                //paragraph("", width: 8)
        		input(name: "toTime", type: "time", title: inputTitle("To:"), required: (settings?.fromTime != null), submitOnChange: true, width: 2)
				//def between = ((settings.fromTime != null) && (settings.toTime != null)) ? myTimeOfDayIsBetween(timeToday(fromTime), timeToday(toTime), new Date(), location.timeZone) : true
				paragraph(getFormat("note","All thermostats will be returned to the 'on' Mode outside of the above conditions"))
            }
        }
        
        section(title: sectionTitle("Operations")) {
        	input(name: "minimize", 	title: inputTitle("Minimize settings text"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
           	input(name: "tempDisable", 	title: inputTitle("Pause this Helper"), 		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)                
			input(name: "debugOff",	 	title: inputTitle("Disable debug logging"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
            input(name: "infoOff", 		title: inputTitle("Disable info logging"), 		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
		}       
		// Standard footer
       	section() {
       		paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br><small>Copyright \u00a9 2017-2020 Barry A. Burke - All rights reserved.</small><br>"+
					  "<small><i>Your</i>&nbsp;</small><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=MJQD5NGVHYENY&currency_code=USD&source=url' target='_blank'>" + 
					  "<img src='https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/paypal-green.png' border='0' width='64' alt='PayPal Logo' title='Please consider donating via PayPal!'></a>" +
					  "<small><i>donation is appreciated!</i></small></div>" )
		}
    }
}

// Main functions
void installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
    atomicState.HVACModeState = 'unknown'
	atomicState.runningThermostat = ''
	initialize()  
}
void updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    initialize()
}
def initialize() {
	LOG("${getVersionLabel()} Initializing (${settings?.helperMode})...", 2, "", 'info')
    atomicState.versionLabel = getVersionLabel()
	updateMyLabel()
	boolean isolate = (settings?.helperMode == 'isolate')
    boolean cooperate = (settings?.helperMode == 'isolate')
    
    if (cooperate) {
        // Get slaves into a known state
        slaveThermostats.each { stat ->
            String ncTh= stat.currentValue('thermostatHold', true) 
            if (ncTh == 'hold') {
                if (state."${stat.displayName}-currProg" == null) {
                    state."${stat.displayName}-currProg" = stat.currentValue('currentProgram', true)
                    if (state."${stat.displayName}-currProg" == 'null') state."${stat.displayName}-currProg" = null		// it really IS null!
                }
                if (state."${stat.displayName}-holdType" == null) {
                    state."${stat.displayName}-holdType" = stat.currentValue('lastHoldType', true)
                }
            } else {
                state."${stat.displayName}-currProg" = null
                state."${stat.displayName}-holdType" = null
            }
        }
	}
	
	// Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
    if (settings.debugOff) log.info "log.debug() logging disabled"
	
    if (cooperate) {
        // check the master, but give it a few seconds first
        runIn (5, "theAdjuster", [overwrite: true])

        // and finally, subscribe to thermostatOperatingState changes
        subscribe( masterThermostat, 'thermostatOperatingState', masterFanStateHandler )
        subscribe( slaveThermostats, 'thermostatOperatingState', changeHandler )
        subscribe( slaveThermostats, 'temperature', changeHandler )
        subscribe( slaveThermostats, 'heatingSetpoint', changeHandler )
        subscribe( slaveThermostats, 'coolingSetpoint', changeHandler )
        subscribe( slaveThermostats, 'temperature', changeHandler )
        // subscribe( slaveThermostats, 'currentProgram', changeHandler )
    } else if (isolate) {
    	subscribe(theThermostats, 'thermostatOperatingState', thermostatHandler)
		updateMyLabel()
    }

    LOG("initialize() complete", 3, null, 'trace')
}

// COOPERATIVE MODE METHODS
def changeHandler(evt) {
	LOG("${evt.displayName} ${evt.name} ${evt.value}",4,null,'trace')
	runIn(2, 'theAdjuster', [overwrite: true])		// collapse multiple simultaneous events
}
def masterFanStateHandler(evt=null) {
	LOG("${evt.displayName} ${evt.name} ${evt.value}",4,null,'trace')
	runIn(2, 'theAdjuster', [overwrite: true])		// (backwards compatibility
}

void theAdjuster() {
	def masterOpState = masterThermostat.currentValue('thermostatOperatingState', true)
	LOG("theAdjuster() - master thermostatOperatingState = ${masterOpState}", 3, null, 'info')
	
	switch (masterOpState) {
		case 'fan only':
			// master is fan-only, turn on fan for any slaves not already running their fan
            if ((settings.shareFan == null) || settings.shareFan) {
				slaveThermostats.each { stat ->
                	def statOpState = stat.currentValue('thermostatOperatingState', true)
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
				ncCpn = stat.currentValue('currentProgramName', true)
            	if (ncCpn == 'Hold: Fan On') {
                    setFanAuto(stat)
                }
            }
            break;
            
		case 'heating':
        	if (settings.shareHeat) {
            	slaveThermostats.each { stat ->
                	def statOpState = stat.currentValue('thermostatOperatingState', true)
                	if (statOpState == 'heating') {
                    	setFanAuto(stat) // stat.resumeProgram(true)		// should get us back to desired fan mode
                    } else if (statOpState == 'fanOnly') {
                    	// See if we are holding the fan but don't need the heat any more
						String ncCpn = stat.currentValue('currentProgramName', true)
                        if (ncCpn == 'Hold: Fan On') {
                      		def heatTo = stat.currentValue('heatingSetpoint', true)
                        	if (heatTo != null) {
                        		def temp = stat.currentValue('temperature', true)
                            	if (temp != null) {
                            		def heatAt = stat.currentValue('heatAtSetpoint', true)
                                	if (heatAt != null) {
                            			if ((temp >= heatTo) || (temp < heatAt)) { 
                                        	// This Zone has reached its target, stop stealing heat
                                			setFanAuto(stat) // stat.setThermostatFanMode('on', 'nextTransition')		// turn the fan on to leech some heat
                                        }
                                    }
                                }
                            }
                        }
                    } else if (statOpState == 'idle') {
                    	def heatTo = stat.currentValue('heatingSetpoint', true)
                        if (heatTo != null) {
                        	def temp = stat.currentValue('temperature', true)
                            if (temp != null) {
                            	def heatAt = stat.currentValue('heatAtSetpoint', true)
                                if (heatAt != null) {
                            		if ((temp < heatTo) && (temp > heatAt)) { 
                                		setFanOn(stat) // stat.setThermostatFanMode('on', 'nextTransition')		// turn the fan on to leech some heat		
                                    }
                                }
                            }
                        }
                    } else { // Must be 'cooling'
						String ncCpn = stat.currentValue('currentProgramName', true)
                        if (ncCpn == 'Hold: Fan On') setFanAuto(stat)	// Just make sure fan is in Auto mode
                    }
                }
            } else {
            	// not sharing (leeching) heat
                if ((settings.shareFan == null) || settings.shareFan) {
                	// but we are sharing fan - turn off fan to avoid overheating this zone
                	slaveThermostats.each { stat ->
						String ncCpn = stat.currentValue('currentProgramName', true)
                		if (ncCpn == 'Hold: Fan On') {
                        	setFanAuto(stat)
                        }
                    }
                }
            }
            break;
            
		case 'cooling':
			if (settings.shareCool) {
				slaveThermostats.each { stat->
                    def statOpState = stat.currentValue('thermostatOperatingState', true)
					if (statOpState == 'cooling') {
                    	// We are cooling too - double-check that fan is in Auto mode
                        setFanAuto(stat)				
                    } else if (statOpState == 'fanOnly') {
                    	// Check if we are holding the fan but don't need the cool any more
						String ncCpn = stat.currentValue('currentProgramName', true)
                        if (ncCpn == 'Hold: Fan On') {
                      		def coolTo = stat.currentValue('coolingSetpoint', true)
                        	if (coolTo != null) {
                        		def temp = stat.currentValue('temperature', true)
                            	if (temp != null) {
                            		def coolAt = stat.currentValue('coolAtSetpoint', true)
                                	if (coolAt != null) {
                            			if ((temp <= coolTo) || (temp > coolAt)) { 
                                        	// This Zone has reached its target, stop stealing cool
                                			setFanAuto(stat) // stat.setThermostatFanMode('on', 'nextTransition')		// turn the fan on to leech some heat
                                        }
                                    }
                                }
                            }
                        }
                    } else if (statOpState == 'idle') {
                    	// Check if we need the cool
                    	def coolTo = stat.currentValue('coolingSetpoint', true)
                        if (coolTo != null) {
                        	def temp = stat.currentValue('temperature', true)
                            if (temp != null) {
                            	def coolAt = stat.currentValue('coolAtSetpoint', true)
                                if (coolAt != null) {
                            		if ((temp > coolSp) && (temp < coolAt)) {
                                	   	setFanOn(stat)
                                    }
                                }
                    		}
                        }
                    } else { // Must be 'heating'
						String ncCpn = stat.currentValue('currentProgramName', true)
                        if (ncCpn == 'Hold: Fan On') setFanAuto(stat)	// Just make sure fan is in Auto mode
                    }
                }
            } else {
            	// not sharing (leeching) cool
                if ((settings.shareFan == null) || settings.shareFan) {
                	// but we are sharing fan - turn off fan to avoid overcooling this zone
                	slaveThermostats.each { stat ->
						String ncCpn = stat.currentValue('currentProgramName', true)
                		if (ncCpn == 'Hold: Fan On') {	// set auto for now, so we don't break fanMinOnTime/Circulation
                        	setFanAuto(stat)
                        }
                    }
                }
            }
            break;

		default:
        	// we ignore the other possible OperatingStates (e.g., 'pendhing heat', etc.)
            slaveThermostats.each { stat ->
				String ncCpn = stat.currentValue('currentProgramName', true)
            	if (ncCpn == 'Hold: Fan On') setFanAuto(stat)
            }
			break;
	}
}

void setFanAuto(stat) {
	
	def oldProg = state."${stat.displayName}-currProg"
    if (oldProg) {
		String ncCp = stat.currentValue('currentProgram', true)
    	if (ncCp != oldProg) stat.setThermostatProgram(oldProg, state."${stat.displayName}-holdType")
        state."${stat.displayName}-currProg" = null
        state."${stat.displayName}-holdType" = null
    }
	def fanMOT = stat.currentValue('fanMinOnTime', true)
    if ((fanMOT != null) && (fanMOT != 0)) {
		String ncTfmd = stat.currentValue('thermostatFanModeDisplay', true)
    	if (ncTfmd != 'circulate') {
        	stat.setThermostatFanMode('circulate')
            LOG("${stat.displayName} fanMode = circulate", 3)
        }
    } else {
		String ncTfm = stat.currentValue('thermostatFanMode', true)
    	if (ncTfm != 'auto') {
        	stat.setThermostatFanMode('auto')
            LOG("${stat.displayName} fanMode = auto", 3)
        }
    }
}

void setFanOn(stat) {
	
	String ncCpn = stat.currentValue('currentProgramName', true)
	String ncTfm = stat.currentValue('thermostatFanMode', true)
	if ((ncCpn != 'Hold: Fan On') || (ncTfm != 'on')) {
		String ncTh = stat.currentValue('thermostatHold', true)
    	if (ncTh == 'hold') {
        	state."${stat.displayName}-holdType" = stat.currentValue('lastHoldType', true)
        	state."${stat.displayName}-currProg" = stat.currentValue('currentProgram', true)
            if (state."${stat.displayName}-currProg" == 'null') state."${stat.displayName}-currProg" = null
        }
    	stat.setThermostatFanMode('on', 'nextTransition')
        LOG("${stat.displayName} fanMode = on", 3)
    }
}

// ISOLATED MODE METHODS
def thermostatHandler(evt) {
	String dni = evt.device.deviceNetworkId as String
    String tid = getDeviceId(dni)
    
	// First make sure we are supposed to be making changes now...
    boolean notNow = false
    if (!dayCheck()) {
        if (atomicState.HVACModeState != 'idle') LOG("Not configured to run Actions today, ignoring", 2, null, 'info')
        notNow = true
    }
    def between = ((settings.fromTime != null) && (settings.toTime != null)) ? myTimeOfDayIsBetween(timeToday(settings.fromTime), timeToday(settings.toTime), new Date(), location.timeZone) : true
    if (!notNow && !between) {
        if (atomicState.HVACModeState != 'idle') LOG('Not configured to run Actions at this time, ignoring', 2, null, 'info')
        notNow = true
    }
    
    if (notNow) {
    	// Make sure that we leave all the systems turned on once we are outside the configured days & time window
        if (atomicState.HVACModeState == 'off') {
        	theThermostats.each { stat ->
            	turnOnHVAC(stat)
            }
            atomicState.HVACModeState == 'idle'	// we'll only do this once...
        }
    } else if (evt.value.startsWith('idle') && (tid == atomicState.runningThermostat)) {
    	// turn them all back on again
        atomicState.HVACModeState = 'on'
        atomicState.runningThermostat = ''
        theThermostats.each { stat ->
        	if (stat.deviceNetworkId != dni) {
            	turnOnHVAC(stat)
            }
        }
    } else if (settings?.busyStates?.contains(evt.value)) {
    	// turn off all the other thermostats
        atomicState.runningThermostat = tid
        atomicState.HVACModeState = 'off'
        theThermostats.each { stat ->
        	if (stat.deviceNetworkId != dni) {
            	turnOffHVAC(stat)
            }
        }
    }  else {
    	LOG("thermostatHandler(): ${evt.name} = ${evt.value} for ${evt.device.displayName} - nothing to do...", 3, null, 'info')
    }
}

void turnOffHVAC(therm) {
	LOG("turnOffHVAC(${therm.displayName}}) entered...", 4,null,'info')
    
    if (settings.hvacOff) {
    	String tid = getDeviceId(therm.deviceNetworkId)
    	def tmpThermSavedState = atomicState.thermSavedState ?: [:]
    	if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
        // turn off the HVAC & save the state
		makeReservation(tid, 'modeOff')						// make sure nobody else turns HVAC on until I'm ready
		String thermostatMode = 	therm.currentValue('thermostatMode', true)
        String thermostatFanMode = 	therm.currentValue('thermostatFanMode', true)
        def fanMinOnTime = 			(therm.currentValue('fanMinOnTime', true)) ?: 0
    	if (thermostatMode != 'off') {
        	fanToo = false
            tmpThermSavedState[tid].mode = thermostatMode
            tmpThermSavedState[tid].fanMode = thermostatFanMode
            tmpThermSavedState[tid].fanTime = fanMinOnTime
            tmpThermSavedState[tid].HVACModeState = 'off'
			tmpThermSavedState[tid].wasAlreadyOff = false
            //therm.setThermostatMode('off')
			log.debug "therm.setThermostatMode( 'off' )"
            if (settings.fanOff) {
            	makeReservation(tid, 'fanOff')
            	//therm.setThermostatFanMode('off')		// "Off" will also clear fanMinOnTime
				log.debug "therm.setThermostatFanMode( 'off' )"
                fanToo = true
            }
			LOG("${therm.displayName} Mode ${fanToo?'and Fan ':''}turned off (was ${thermostatMode}/${thermostatFanMode}-${fanMinOnTime})",3,null,'info')    
        } else {
        	tmpThermSavedState[tid].wasAlreadyOff = true
            tmpThermSavedState[tid].HVACModeState = 'off'
        }
    	atomicState.thermSavedState = tmpThermSavedState
		LOG("turnOffHVAC(${therm.displayName}) - thermSavedState: ${atomicState.thermSavedState}", 4, null, 'debug')
    }
}

void turnOnHVAC(therm) {
 	LOG("turnOnHVAC(${therm.displayName}) entered...", 4,null,'info')

	if (settings.hvacOff) {
    	// turn on the HVAC
    	String tid = getDeviceId(therm.deviceNetworkId)
    	def tmpThermSavedState = atomicState.thermSavedState ?: [:]
        if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
        String currentMode = therm.currentValue('thermostatMode', true) 		// Better be "off"
     
        String thermostatMode
        String thermostatFanMode
        def fanMinOnTime = 0

        if (settings.hvacOn == "Prior State") {
            thermostatMode = tmpThermSavedState[tid].mode
            if (settings.fanOff) {
                if (settings.fanOn) {
                    if (settings.fanOn == "Prior State") {
                        thermostatFanMode = tmpThermSavedState[tid].fanMode
                        fanMinOnTime = tmpThermSavedState[tid].fanTime
                    }else {
                        thermostatFanMode = settings.fanOn
                        if (settings.fanOn == "Circulate") {
                            fanMinOnTime = settings.fanOnTime ?: 20
                        }
                    }
                }
            }
        } else {
            thermostatMode = settings.hvacOn
            if (settings.fanOff) {
                if (settings.fanOn) {
                    if (settings.fanOn == "Prior State") {
                        thermostatFanMode = tmpThermSavedState[tid].fanMode
                        fanMinOnTime = tmpThermSavedState[tid].fanTime
                    } else {
                        thermostatFanMode = settings.fanOn
                        if (settings.fanOn == "Circulate") {
                            fanMinOnTime = settings.fanOnTime ?: 20
                        }
                    }
                }
            }
        }
            
        if (currentMode == 'off') {
           	int i = countReservations( tid, 'modeOff' ) - (haveReservation(tid, 'modeOff') ? 1 : 0)
            if (i > 0) {
                // Currently off, and somebody besides me has a reservation - just release my reservation
                cancelReservation(tid, 'modeOff')
                notReserved = false							
                LOG("Cannot change ${therm.displayName} to ${thermostatMode.capitalize()} - ${getGuestList(tid, 'modeOff').toString()[1..-2]} hold 'modeOff' reservations",1,null,'warn')
            } else {
              	// Nobody else but me has a reservation
                cancelReservation(tid, 'modeOff')
				if (tmpThermSavedState[tid].containsKey('wasAlreadyOff') && (tmpThermSavedState[tid].wasAlreadyOff == false)) {
					tmpThermSavedState[tid].HVACModeState = 'on'
					tmpThermSavedState[tid].mode = thermostatMode
                	//therm.setThermostatMode( thermostatMode )
					log.debug "therm.setThermostatMode( ${thermostatMode} )"
                    boolean andFan = false
                    if (settings.fanOff) {
						i = countReservations( tid, 'fanOff' ) - (haveReservation(tid, 'fanOff') ? 1 : 0)
						if (i > 0) {
                			// Currently off, and somebody besides me has a reservation - just release my reservation
                			cancelReservation(tid, 'fanOff')	
                			LOG("Cannot change ${therm.displayName} to ${thermostatFanMode.capitalize()} - ${getGuestList(tid, 'fanOff').toString()[1..-2]} hold 'fanOff' reservations",1,null,'warn')
            			} else {
              				// Nobody else but me has a reservation
                			cancelReservation(tid, 'fanOff')
							currentFanTime = therm.currentValue('fanMinOnTime', true)
							if (fanMinOnTime != currentFanTime) {
								andFan = true
								therm.setFanMinOnTime = fanMinOnTime
								log.debug "therm.setFanMinOnTime( ${fanMinOnTime} )"
							}
							currentFanMode = therm.currentValue('thermostatFanMode', true)
							if (thermostatFanMode != currentFanMode) {
								andFan = true
								therm.setFanMode(thermostatFanMode)
								log.debug "therm.setThermostatFanMode( ${thermostatFanMode} )"
							}
						}
					}
					LOG("${therm.displayName} ${thermostatMode.capitalize()} Mode ${andFan?'and Fan':''} restored (was ${currentMode.capitalize()})",3,null,'info')
				} else {
					LOG("${therm.displayName} was already off, not turning back on",3,null,'info')
				}
            } 
        } else if (currentMode == thermostatMode) {
            LOG("${therm.displayName} is already in ${thermostatMode.capitalize()}",3,null,'info')
            tmpThermSavedState[tid].HVACModeState = 'on'
        }
        atomicState.thermSavedState = tmpThermSavedState
    }
}

// Helper Functions
// SmartThings internal function format (Strings instead of Dates)
private myTimeOfDayIsBetween(String fromTime, String toTime, Date checkDate, String timeZone) {
log.debug "fromTime: ${fromTime}"
	return myTimeOfDayIsBetween(timeToday(fromTime), timeToday(toTime), checkDate, timeZone)
}

private myTimeOfDayIsBetween(Date fromDate, Date toDate, Date checkDate, timeZone)     {
	if (toDate == fromDate) {
		return false	// blocks the whole day
	} else if (toDate < fromDate) {
		if (checkDate.before(fromDate)) {
			fromDate = fromDate - 1
		} else {
			toDate = toDate + 1
		}
	}
    return (!checkDate.before(fromDate) && !checkDate.after(toDate))
}
private toDateTime( str ) {
	return timeToday( str )
}
def getShortTime( date ) {
	Date dt = timeToday( date )
	def df = new java.text.SimpleDateFormat("h:mm a")
    df.setTimeZone(location.timeZone)
    return df.format( dt )
}
private boolean dayCheck() {
	if (!settings.actionDays) return true
    
    def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    return (actionDays.contains(day))
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(String tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id as String, type )
}
// Cancel my reservation
void cancelReservation(String tid, String type='modeOff') {
	//log.debug "cancel ${tid}, ${type}"
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

void updateMyLabel() {
    
	String flag = '<span '
	
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (myLabel.contains(flag)) {
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	if (settings.tempDisable) {
		def newLabel = myLabel + '<span style="color:red"> (paused)</span>'
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
	}
}
def pauseOn(global = false) {
	// Pause this Helper
	atomicState.wasAlreadyPaused = settings.tempDisable //!atomicState.globalPause)
	if (!settings.tempDisable) {
		LOG("pauseOn(${global}) - performing ${global?'Global':'Helper'} Pause",2,null,'info')
		app.updateSetting("tempDisable", true)
        settings.tempDisable = true
		atomicState.globalPause = global
		runIn(2, updated, [overwrite: true])
        // updateMyLabel()
	} else {
		LOG("pauseOn(${global}) - was already paused...",3,null,'info')
	}
}
def pauseOff(global = false) {
	// Un-pause this Helper
	if (settings.tempDisable) {
		// Allow peer Apps to individually re-enable anytime
        // NB: they won't be able to unpause us if we are in a global pause (they will also be paused)
        if (!global || !atomicState.wasAlreadyPaused) { 													// 
			LOG("pauseOff(${global}) - performing ${global?'Global':'Helper'} Unpause",2,null,'info')
			app.updateSetting("tempDisable", false)
            settings.tempDisable = false
            atomicState.wasAlreadyPaused = false
			runIn(2, updated, [overwrite: true])
		} else {
			LOG("pauseOff(${global}) - was already paused before Global Pause, ignoring...",3,null,'info')
		}
	} else {
		LOG("pauseOff(${global}) - not currently paused...",3,null,'info')
		atomicState.wasAlreadyPaused = false
	}
	atomicState.globalPause = global
}
// Thermostat Programs & Modes
List getThermostatModes() {
	def statModes = []
	def tm = []
    if (settings.theThermostats) {
        settings.theThermostats?.each { stat ->
            tm = new JsonSlurper().parseText(stat.currentValue('supportedThermostatModes', true))
			if (statModes == []) {	
				if (tm) statModes = tm
			} else {
				if (tm) statModes = statModes.intersect(tm)
			}
		}
    }
    if (statModes == []) statModes = ["off","heat","cool","auto","auxHeatOnly"]
    return ["Prior State"] + statModes*.capitalize().sort(false)
}
List getThermostatFanModes() {
	def fanModes = []
	def tfm = []
    if (settings?.theThermostats) {
    	settings.theThermostats.each { stat ->
			tfm = new JsonSlurper().parseText(stat.currentValue('supportedThermostatFanModes', true))
            if (tfm) fanModes = (fanModes == []) ? tfm : fanModes.intersect(tfm)
        }
    }
    if (fanModes == []) fanModes = ["off", "auto", "circulate", "on"]
    return ["Prior State"] + fanModes*.capitalize().sort(false)
}

String getDeviceId(networkId) {
    return networkId.split(/\./).last()
}
void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
    switch (logType) {
    	case 'error':
        	log.error message
            break;
        case 'warn':
        	log.warn message
            break;
        case 'trace':
        	log.trace message
            break;
        case 'info':
        	if (!settings?.infoOff) log.info message
            break;
        case 'debug':
        default:
        	if (!settings?.debugOff) log.debug message
        	break;
    }
}

String getTheBee	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=78 height=78 align=right></img>'}
String getTheBeeLogo()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg width=30 height=30 align=left></img>'}
String getTheSectionBeeLogo()		{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=25 height=25 align=left></img>'}
String getTheBeeUrl ()				{ return "https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg" }
String getTheBlank	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/blank.png width=400 height=35 align=right hspace=0 style="box-shadow: 3px 0px 3px 0px #ffffff;padding:0px;margin:0px"></img>'}
String pageTitle 	(String txt) 	{ return getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') }
String pageTitleOld	(String txt)	{ return getFormat('header-ecobee','<h2>'+txt+'</h2>') }
String sectionTitle	(String txt) 	{ return getTheSectionBeeLogo() + getFormat('header-nobee','<h3><b>&nbsp;&nbsp;'+txt+'</b></h3>') }
String smallerTitle (String txt)	{ return txt ? ('<h3 style="color:#5BBD76"><b><u>'+txt+'</u></b></h3>') : '' } // <hr style="background-color:#5BBD76;height:1px;width:52%;border:0;align:top">
String sampleTitle	(String txt) 	{ return '<b><i>'+txt+'<i></b>' }
String inputTitle	(String txt) 	{ return '<b>'+txt+'</b>' }
String getWarningText()				{ return "<span style='color:red'><b>WARNING: </b></span>" }
String getFormat(type, myText=""){
	switch(type) {
		case "header-ecobee":
			return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theBee}${myText}</div>"
			break;
		case "header-nobee":
			return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
			break;
    	case "line":
			return "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>"
			break;
		case "title":
			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
			break;
		case "warning":
			return "<span style='color:red'><b>WARNING: </b><i></span>${myText}</i>"
			break;
		case "note":
			return "<b>NOTE: </b>${myText}"
			break;
		default:
			return myText
			break;
	}
}

// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019-2020, Barry A. Burke (storageanarchy@gmail.com)
String getPlatform() { return 'Hubitat' }	// if (platform == 'SmartThings') ...
boolean getIsST() { return false }
boolean getIsHE() { return true }

String getHubPlatform() { 
	return 'Hubitat'
}
boolean getIsSTHub() { return false }					// if (isSTHub) ...
boolean getIsHEHub() { return true }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return parent?."${settingName}"
}
@Field String  hubPlatform 	= 'Hubitat'
@Field boolean ST 			= false
@Field boolean HE 			= true
@Field String  debug		= 'debug'
@Field String  error		= 'error'
@Field String  info			= 'info'
@Field String  trace		= 'trace'
@Field String  warn			= 'warn'
