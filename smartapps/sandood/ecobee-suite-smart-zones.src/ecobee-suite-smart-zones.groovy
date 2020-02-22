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
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - nonCached currentValue() on HE
 *	1.7.02 - Fixing private method issue caused by grails
 *	1.7.03 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.04 - Optimized isST/isHE, formatting, added Global Pause
 *	1.7.05 - Added option to disable local display of log.debug() logs
 *	1.7.06 - Fixed helper labelling
 *	1.7.07 - Fixed labels (again), added infoOff, cleaned up preferences setup
 *	1.7.08 - Added minimze UI
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 */
String getVersionNum()		{ return "1.8.01" }
String getVersionLabel() { return "Ecobee Suite Smart Zones Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

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
	boolean ST = isST
	boolean HE = !ST
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
	String defaultName = "Smart Zones"
    
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This Helper will synchronize Ecobee Suite thermostats' Operating State across a multi-zone HVAC system to run the fan simultaneously in all zones, minimizing the fan's total operating time.")
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
			if (HE) {
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
			} else {
            	if (app.label.contains(' (paused)')) {
                	if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains(' (paused)')) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
                        String myLabel = app.label.substring(0, app.label.indexOf(' (paused)'))
                        atomicState.appDisplayName = myLabel
                        app.updateLabel(myLabel)
                    }
                } else {
                	atomicState.appDisplayName = app.label
                }
            }
        	if(settings.tempDisable) { 
				paragraph warningText + "Temporarily Paused; Resume below" 
			} else {
				input ("masterThermostat", "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Select Master Ecobee Suite Thermostat", required: true, multiple: false, submitOnChange: true)
			}            
		}
        
        if (!settings?.tempDisable && settings?.masterThermostat) {
        	section(title: sectionTitle('Configuration')+(ST?"\n":'')+smallerTitle("Select Slave Thermostats")) {
				// Settings option for using Mode or Routine
				input(name: "slaveThermostats", title: inputTitle("Select Slave Ecobee Suite Thermostat(s)"), type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", required: true, multiple: true, submitOnChange: true)
			}
			if (slaveThermostats) {
				section(title: sectionTitle("Actions")) {
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
		}
        
        section(title: sectionTitle("Operations")) {
        	input(name: "minimize", 	title: inputTitle("Minimize settings text"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
           	input(name: "tempDisable", 	title: inputTitle("Pause this Helper"), 		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)                
			input(name: "debugOff",	 	title: inputTitle("Disable debug logging"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
            input(name: "infoOff", 		title: inputTitle("Disable info logging"), 		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
		}       
		// Standard footer
        if (ST) {
        	section(getVersionLabel().replace('er, v',"er\nV")+"\n\nCopyright \u00a9 2017-2020 Barry A. Burke\nAll rights reserved.\n\nhttps://github.com/SANdood/Ecobee-Suite") {}
        } else {
        	section() {
        		paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br><small>Copyright \u00a9 2017-2020 Barry A. Burke - All rights reserved.</small><br>"+
                		  "<a href='https://github.com/SANdood/Ecobee-Suite' target='_blank' style='color:#5BBD76'><u>Click here for the Ecobee Suite GitHub Repository</u></a></div>")
            }
		}
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
    initialize()
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	updateMyLabel()
	boolean ST = atomicState.isST
	
	// Get slaves into a known state
	slaveThermostats.each { stat ->
		String ncTh= ST ? stat.currentValue('thermostatHold') : stat.currentValue('thermostatHold', true) 
    	if (ncTh == 'hold') {
        	if (state."${stat.displayName}-currProg" == null) {
				state."${stat.displayName}-currProg" = ST ? stat.currentValue('currentProgram') : stat.currentValue('currentProgram', true)
            }
            if (state."${stat.displayName}-holdType" == null) {
        		state."${stat.displayName}-holdType" = ST ? stat.currentValue('lastHoldType') : stat.currentValue('lastHoldType', true)
            }
        } else {
      		state."${stat.displayName}-currProg" = null
        	state."${stat.displayName}-holdType" = null
        }
	}
	
	// Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
    if (settings.debugOff) log.info "log.debug() logging disabled"
	
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

void theAdjuster() {
	boolean ST = atomicState.isST
	def masterOpState = ST ? masterThermostat.currentValue('thermostatOperatingState') : masterThermostat.currentValue('thermostatOperatingState', true)
	LOG("theAdjuster() - master thermostatOperatingState = ${masterOpState}", 3, null, 'info')
	
	switch (masterOpState) {
		case 'fan only':
			// master is fan-only, turn on fan for any slaves not already running their fan
            if ((settings.shareFan == null) || settings.shareFan) {
				slaveThermostats.each { stat ->
                	def statOpState = ST ? stat.currentValue('thermostatOperatingState') : stat.currentValue('thermostatOperatingState', true)
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
				ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
            	if (ncCpn == 'Hold: Fan On') {
                    setFanAuto(stat)
                }
            }
            break;
            
		case 'heating':
        	if (settings.shareHeat) {
            	slaveThermostats.each { stat ->
                	def statOpState = ST ? stat.currentValue('thermostatOperatingState') : stat.currentValue('thermostatOperatingState', true)
                	if (statOpState == 'heating') {
                    	setFanAuto(stat) // stat.resumeProgram(true)		// should get us back to desired fan mode
                    } else if (statOpState == 'fanOnly') {
                    	// See if we are holding the fan but don't need the heat any more
						String ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
                        if (ncCpn == 'Hold: Fan On') {
                      		def heatTo = ST ? stat.currentValue('heatingSetpoint') : stat.currentValue('heatingSetpoint', true)
                        	if (heatTo != null) {
                        		def temp = ST ? stat.currentValue('temperature') : stat.currentValue('temperature', true)
                            	if (temp != null) {
                            		def heatAt = ST ? stat.currentValue('heatAtSetpoint') : stat.currentValue('heatAtSetpoint', true)
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
                    	def heatTo = ST ? stat.currentValue('heatingSetpoint') : stat.currentValue('heatingSetpoint', true)
                        if (heatTo != null) {
                        	def temp = ST ? stat.currentValue('temperature') : stat.currentValue('temperature', true)
                            if (temp != null) {
                            	def heatAt = ST ? stat.currentValue('heatAtSetpoint') : stat.currentValue('heatAtSetpoint', true)
                                if (heatAt != null) {
                            		if ((temp < heatTo) && (temp > heatAt)) { 
                                		setFanOn(stat) // stat.setThermostatFanMode('on', 'nextTransition')		// turn the fan on to leech some heat		
                                    }
                                }
                            }
                        }
                    } else { // Must be 'cooling'
						String ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
                        if (ncCpn == 'Hold: Fan On') setFanAuto(stat)	// Just make sure fan is in Auto mode
                    }
                }
            } else {
            	// not sharing (leeching) heat
                if ((settings.shareFan == null) || settings.shareFan) {
                	// but we are sharing fan - turn off fan to avoid overheating this zone
                	slaveThermostats.each { stat ->
						String ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
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
                    def statOpState = ST ? stat.currentValue('thermostatOperatingState') : stat.currentValue('thermostatOperatingState', true)
					if (statOpState == 'cooling') {
                    	// We are cooling too - double-check that fan is in Auto mode
                        setFanAuto(stat)				
                    } else if (statOpState == 'fanOnly') {
                    	// Check if we are holding the fan but don't need the cool any more
						String ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
                        if (ncCpn == 'Hold: Fan On') {
                      		def coolTo = ST ? stat.currentValue('coolingSetpoint') : stat.currentValue('coolingSetpoint', true)
                        	if (coolTo != null) {
                        		def temp = ST ? stat.currentValue('temperature') : stat.currentValue('temperature', true)
                            	if (temp != null) {
                            		def coolAt = ST ? stat.currentValue('coolAtSetpoint') : stat.currentValue('coolAtSetpoint', true)
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
                    	def coolTo = ST ? stat.currentValue('coolingSetpoint') : stat.currentValue('coolingSetpoint', true)
                        if (coolTo != null) {
                        	def temp = ST ? stat.currentValue('temperature') : stat.currentValue('temperature', true)
                            if (temp != null) {
                            	def coolAt = ST ? stat.currentValue('coolAtSetpoint') : stat.currentValue('coolAtSetpoint', true)
                                if (coolAt != null) {
                            		if ((temp > coolSp) && (temp < coolAt)) {
                                	   	setFanOn(stat)
                                    }
                                }
                    		}
                        }
                    } else { // Must be 'heating'
						String ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
                        if (ncCpn == 'Hold: Fan On') setFanAuto(stat)	// Just make sure fan is in Auto mode
                    }
                }
            } else {
            	// not sharing (leeching) cool
                if ((settings.shareFan == null) || settings.shareFan) {
                	// but we are sharing fan - turn off fan to avoid overcooling this zone
                	slaveThermostats.each { stat ->
						String ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
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
				String ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
            	if (ncCpn == 'Hold: Fan On') setFanAuto(stat)
            }
			break;
	}
}

void setFanAuto(stat) {
	boolean ST = atomicState.isST
	
	def oldProg = state."${stat.displayName}-currProg"
    if (oldProg) {
		String ncCp = ST ? stat.currentValue('currentProgram') : stat.currentValue('currentProgram', true)
    	if (ncCp != oldProg) stat.setThermostatProgram(oldProg, state."${stat.displayName}-holdType")
        state."${stat.displayName}-currProg" = null
        state."${stat.displayName}-holdType" = null
    }
	def fanMOT = ST ? stat.currentValue('fanMinOnTime') : stat.currentValue('fanMinOnTime', true)
    if ((fanMOT != null) && (fanMOT != 0)) {
		String ncTfmd = ST ? stat.currentValue('thermostatFanModeDisplay') : stat.currentValue('thermostatFanModeDisplay', true)
    	if (ncTfmd != 'circulate') {
        	stat.setThermostatFanMode('circulate')
            LOG("${stat.displayName} fanMode = circulate", 3)
        }
    } else {
		String ncTfm = ST ? stat.currentValue('thermostatFanMode') : stat.currentValue('thermostatFanMode', true)
    	if (ncTfm != 'auto') {
        	stat.setThermostatFanMode('auto')
            LOG("${stat.displayName} fanMode = auto", 3)
        }
    }
}

void setFanOn(stat) {
	boolean ST = atomicState.isST
	
	String ncCpn = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)
	String ncTfm = ST ? stat.currentValue('thermostatFanMode') : stat.currentValue('thermostatFanMode', true)
	if ((ncCpn != 'Hold: Fan On') || (ncTfm != 'on')) {
		String ncTh = ST ? stat.currentValue('thermostatHold') : stat.currentValue('thermostatHold', true)
    	if (ncTh == 'hold') {
        	state."${stat.displayName}-holdType" = ST ? stat.currentValue('lastHoldType') : stat.currentValue('lastHoldType', true)
        	state."${stat.displayName}-currProg" = ST ? stat.currentValue('currentProgram') : stat.currentValue('currentProgram', true)
        }
    	stat.setThermostatFanMode('on', 'nextTransition')
        LOG("${stat.displayName} fanMode = on", 3)
    }
}

// Helper Functions

void updateMyLabel() {
	boolean ST = atomicState.isST
    
	String flag = ST ? ' (paused)' : '<span '
	
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
		def newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
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
void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${atomicState.appDisplayName} ${message}"
    if (logType == null) logType = 'debug'
    if (logType == 'debug') {
    	if (!settings?.debugOff) log.debug message
    } else if (logType == 'info') {
    	if (!settings?.infoOff) log.info message
    } else log."${logType}" message
	parent.LOG(msg, level, null, logType, event, displayEvent)
}

String getTheBee	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=78 height=78 align=right></img>'}
String getTheBeeLogo()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg width=30 height=30 align=left></img>'}
String getTheBeeUrl ()				{ return "https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg" }
String getTheBlank	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/blank.png width=400 height=35 align=right hspace=0 style="box-shadow: 3px 0px 3px 0px #ffffff;padding:0px;margin:0px"></img>'}
String pageTitle 	(String txt) 	{ return isHE ? getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') : txt }
String pageTitleOld	(String txt)	{ return isHE ? getFormat('header-ecobee','<h2>'+txt+'</h2>') 	: txt }
String sectionTitle	(String txt) 	{ return isHE ? getFormat('header-nobee','<h3><b>'+txt+'</b></h3>')	: txt }
String smallerTitle	(String txt) 	{ return txt ? (isHE ? '<h3><b>'+txt+'</b></h3>' 				: txt) : '' }
String sampleTitle	(String txt) 	{ return isHE ? '<b><i>'+txt+'<i></b>'			 				: txt }
String inputTitle	(String txt) 	{ return isHE ? '<b>'+txt+'</b>'								: txt }
String getWarningText()				{ return isHE ? "<div style='color:red'><b>WARNING: </b></div>"	: "WARNING: " }
String getFormat(type, myText=""){
	if(type == "header-ecobee") return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theBee}${myText}</div>"
	if(type == "header-nobee") 	return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
    if(type == "line") 			return "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>"
	if(type == "title")			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
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
