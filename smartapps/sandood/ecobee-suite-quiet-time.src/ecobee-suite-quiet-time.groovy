/**
 *  ecobee Suite Quiet Time
 *
 *	Copyright 2017-2020 Barry A. Burke
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
 *	1.7.01 - Use nonCached currentValue() for stat attributes on Hubitat
 *	1.7.02 - Fixing private method issue caused by grails
 *	1.7.03 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.04 - Optimized isST/isHE, added Global Pause
 *	1.7.05 - Added option to disable local display of log.debug() logs
 *	1.7.06 - Fixes for the auto-disable logic
 *	1.7.07 - More fixes for autoOff
 *	1.7.08 - Fix hasHumidifier
 *	1.7.09 - Fixed Helper labelling
 *  1.7.10 - Fixed labels (again), added infoOff, cleaned up preferences setup ***
 *	1.7.11 - Added minimize UI
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - More busy bees
 *	1.8.03 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.04 - New SHPL, using Global Fields instead of atomicState
 */
import groovy.transform.Field

String getVersionNum()		{ return "1.8.04" }
String getVersionLabel() 	{ return "Ecobee Suite Quiet Time Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Quiet Time",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSets HVAC into user-specified 'Quiet Mode' when a specified switch (real or virtual) is enabled.",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-quiet-time.src/ecobee-suite-quiet-time.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-quiet-time-sa",
	singleInstance: 	false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	//boolean ST = isST
	//boolean HE = !ST
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
    String defaultName = "Quiet Time"
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("Ideal for watching movies, this Helper implements the concept of 'Quiet Time' for your Ecobee Suite-controlled HVAC system. Activated by a (real or virtual) switch, the Helper will "+
                          "turn off the components of your system that create noise. The entire system can be turned off, as well as the fan, humidifier and dehumidifier. Alternately, setpoints can be changed "+
                          "as another means of reducing the noise. Quiet Time supports integration with other Helpers (e.g., Smart Circulation), and also supports auto-off after a configurable delay.")
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
				paragraph getFormat("warning","This Helper is temporarily paused...")  
			} else { 
            	input(name: "theThermostats", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select Ecobee Suite Thermostat(s)"), 
					  required: true, multiple: true, submitOnChange: true)
            }          
		}
    
		if (!settings.tempDisable && (settings.theThermostats?.size() > 0)) {
			def hasH = hasHumidifier()
			def hasD = hasDehumidifier()
			
			section(title: sectionTitle('Trigger')) {
				if (maximize) paragraph("Quiet Time is enabled by turning on (or off) a physical or virtual switch.")
				input(name: 'qtSwitch', type: 'capability.switch', required: true, title: inputTitle('Select a Switch to control this instance of Quiet Time'), multiple: false, submitOnChange: true, width: 4)
				if (settings.qtSwitch) {
					input(name: "qtOn", type: "enum", title: inputTitle("Turn on Quiet Time when the Switch is"), defaultValue: 'on', required: true, 
                    	  multiple: false, submitOnChange: true, options: ["on","off"], width: 6)
					input(name: "qtAutoOff", type: "enum", title: inputTitle("Auto-disable Quiet Time after"), defaultValue: '(Disabled)', required: true, multiple: false, submitOnChange: true,
						  options: ["(Disabled)", "10 Minutes", "15 Minutes", "30 Minutes", "45 Minutes", "1 Hour", "2 Hours", "3 Hours", "4 Hours", "6 Hours"], width: 6)
				}
                if (settings?.qtOn == null) {app.updateSetting('qtOn', 'on'); settings?.qtOn = 'on'; }
                if (settings?.qtAutoOff == null) {app.updateSetting('qtAutoOff', '(Disabled)'); settings?.qtAutoOff = '(Disabled)'; }
			}

			section(title: sectionTitle("Actions")) {
				if (maximize) paragraph("Quiet Time can turn off most components of your HVAC system in order to minimize background noise", width: 12)
				input(name: 'hvacOff', type: "bool", title: inputTitle("Turn off HVAC?"), required: true, defaultValue: false, submitOnChange: true, width: 4)
				if (settings.hvacOff) {
					if (maximize) paragraph("HVAC Mode will be set to Off. Circulation, Humidification and/or Dehumidification may still operate while HVAC is Off. "+
							  "\n\nHVAC Mode will be returned to its original state when Quiet Time ends.", width: 8)
				} else if (HE) paragraph("", width: (maximize?8:12))
				
				if (!settings.hvacOff) {
					input(name: 'hvacMode', type: 'bool', title: inputTitle('Change HVAC Mode?'), required: true, defaultValue: false, submitOnChange: true, , width: 4)
					if (settings.hvacMode) {
						input(name: 'quietMode', title: inputTitle('Set thermostat mode to'), type: 'enum', required: true, multiple: false, 
								options:getThermostatModes(), width: 4, submitOnChange: true)
						if (settings.quietMode) {
							if (HE) paragraph("", width: 4)
							if (HE) paragraph("", width: 4)
							if (maximize) paragraph("HVAC Mode will be set to ${settings.quietMode} Mode." + 
									  "${(settings.quietMode=='off')?' Circulation, Humidification and/or Dehumidification may still operate while HVAC is Off.':''}"+
									  "\n\nHVAC Mode will be returned to its original state when Quiet Time ends.", width: 8)
						}
					} else if (HE) paragraph("", width: 8)
				}
				//if (settings.hvacOff || settings.hvacMode) paragraph("HVAC mode will be returned to its original state when Quiet Time ends")

				input(name: 'fanOff', type: "bool", title: inputTitle("Turn off the Fan?"), required: true, defaultValue: false, submitOnChange: true, width: 4)
				if (settings.fanOff) {
					if (maximize) paragraph('Turning off the fan will not stop automatic circulation, even if the HVAC is also off.', width: 8)
					input(name: 'circOff', type: "bool", title: inputTitle('Also disable Circulation?'), required: true, defaultValue: false, submitOnChange: true, 
						  width: 4)
					if (settings.circOff) {
						if (maximize) paragraph("Circulation will also be disabled.", width: 4)
					} else {
						if (maximize) paragraph("Circulation will not be modified.", width: 4)
					}
					if (HE && maximize) paragraph("", width: 4)
					if (HE && maximize) paragraph("", width: 4)
					if (maximize) paragraph("At the end of Quiet Time, the Fan Mode will be restored to its prior setting${settings.circOff?', and circulation will be re-enabled':''}.", width: 8)
				} else if (HE && maximize) paragraph("", width: 8)
					
				if (settings.hvacOff || settings.hvacMode || settings.fanOff) {
					input(name: 'modeResume', type: 'bool', title: inputTitle('Also resume the Current Program at the end of Quiet Time (recommended)?'), defaultValue: true, required: true, width: 10)
				}

				if (!settings.hvacOff && !settings.hvacMode) {
					input(name: 'adjustSetpoints', type: 'bool', title: inputTitle('Adjust heat/cool setpoints?'), required: true, defaultValue: false, submitOnChange: true, width: 4)
					if (settings.adjustSetpoints) {
						input(name: 'heatAdjust', type: 'decimal', title: inputTitle('Heating setpoint adjustment')+' (+/- 20°)', required: true, defaultValue: 0.0, range: '-20..20', width: 4)
						input(name: 'coolAdjust', type: 'decimal', title: inputTitle('Cooling setpoint adjustment')+' (+/- 20°)', required: true, defaultValue: 0.0, range: '-20..20', width: 4)
						if (HE) paragraph("", width: 4)
						input(name: 'setpointResume', type: 'enum', title: inputTitle('At the end of Quiet Time'), /*description: 'Tap to choose...',*/ multiple: false, required: true,
								options: ['Restore prior Setpoints','Resume Current Program', 'Resume Scheduled Program'], submitOnChange: true, width: 8)
						if (settings.setpointResume) {
							if (maximize) paragraph(width: 9, "At the end of Quiet Time, ${settings.setpointResume.startsWith('Resu')?'the currently scheduled program will be resumed.':'the prior setpoints will be restored.'}")
						} else if (HE) paragraph("", width: 9)
					} else if (HE) paragraph("", width: 8)
				}
				if ((settings.theThermostats?.size() != 0) && atomicState.hasHumidifier) {
					input(name: 'humidOff', type: 'bool', title: inputTitle('Turn off the Humidifier?'), required: true, defaultValue: false, submitOnChange: true, width: 4)
					if (settings.humidOff) {
						if (maximize) paragraph("At the end of Quiet Time, the humidifier will be turned back on.", width: 8)
					} else if (HE) paragraph("", width: 8)
				}
				if ((settings.theThermostats?.size() != 0) && atomicState.hasDehumidifier) {
					input(name: 'dehumOff', type: 'bool', title: inputTitle('Turn off the Dehumidifier?'), required: true, defaultValue: false, submitOnChange: true, width: 4)
					if (settings.dehumOff) {
						if (maximize) paragraph("At the end of Quiet Time, the dehumidifier will be turned back on.", width: 8)
					} else if (HE) paragraph("", width: 8)
				}
			}
	//        section(title: 'Actions when Quiet Time Ends') {
	//        	input(name: 'loudActions', type: 'enum', title: 'When quiet time ends', description: 'Tap to choose...', required: true, defaultValue: 'Resume Current Program', 
	//            		options: ['Resume Current Program', "${adjustSetpoints?'Set Hold with Prior Setpoints':''}", 
	//        }

	/* NOTIFICATIONS NOT YET IMPLEMENTED 
			if (settings.notify) {
				if (isST) {
					section("Notifications") {
						input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777; +441234567890)", required: false, submitOnChange: true)
						input( name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, required: true, submitOnChange: true)
						input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
							if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
						if (!settings.phone && !settings.pushNotify && !settings.speak) paragraph "WARNING: Notifications configured, but nowhere to send them!"
					}
				} else {		// HE
					section("Use Notification Device(s)") {
						input(name: "notifiers", type: "capability.notification", title: "", required: ((settings.phone == null) && !settings.speak), multiple: true, 
							  description: "Select notification devices", submitOnChange: true)
						paragraph ""
					}
					section("Use SMS to Phone(s) (limit 10 messages per day)") {
						input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777, +441234567890)",
							  required: ((settings.notifiers == null) && !settings.speak), submitOnChange: true)
						paragraph ""
					}
					section("Use Speech Device(s)") {
						input(name: "speak", type: "bool", title: "Speak messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
							input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
						paragraph ""
					}
				}
			}
	*/
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
void uninstalled () {
	clearReservations()
}
void clearReservations() {
	theThermostats?.each {
    	cancelReservation( getDeviceId(it.deviceNetworkId), 'modeOff' )
        cancelReservation( getDeviceId(it.deviceNetworkId), 'fanOff' )
        cancelReservation( getDeviceId(it.deviceNetworkId), 'circOff' )
        cancelReservation( getDeviceId(it.deviceNetworkId), 'humidOff' )
        cancelReservation( getDeviceId(it.deviceNetworkId), 'dehumOff' )
	}
}
void updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
	unschedule()
	initialize()
}
def initialize() {
	LOG("${getVersionLabel()}\nInitializing...", 2, null, 'info')
    //log.info "${app.name}, ${app.label}"
	updateMyLabel()
	
	if(tempDisable == true) {
    	clearReservations()
		LOG("Temporarily Paused", 3, null, 'info')
		return true
	}
	if (settings.debugOff) log.info "log.debug() logging disabled"
	
    // Initialize the saved states
    def statState = [:]
    settings.theThermostats.each() { stat ->
    	def tid = getDeviceId(stat.deviceNetworkId)
		if (ST) {
			statState[tid] = [
								thermostatMode: 	stat.currentValue('thermostatMode'),
								thermostatFanMode: 	stat.currentValue('thermostatFanMode'),
								fanMinOnTime:		stat.currentValue('fanMinOnTime'),
								heatingSetpoint:	stat.currentValue('heatingSetpoint'),
								coolingSetpoint:	stat.currentValue('coolingSetpoint'),
								holdType:			stat.currentValue('lastHoldType'),
								hasHumidifier:		stat.currentValue('hasHumidifier'),
								humidifierMode:		stat.currentValue('humidifierMode'),
								hasDehumidifier:	stat.currentValue('hasDehumidifier'),
								dehumidifierMode:	stat.currentValue('dehumidifierMode'),
								thermostatHold:		stat.currentValue('thermostatHold'),
								currentProgramName:	stat.currentValue('currentProgramName'),
								currentProgramId:	stat.currentValue('currentProgramId'),
								currentProgram:		stat.currentValue('currentProgram'),
								scheduledProgram:	stat.currentValue('scheduledProgram'),
							 ]   
		} else {
			statState[tid] = [
								thermostatMode: 	stat.currentValue('thermostatMode', true),
								thermostatFanMode: 	stat.currentValue('thermostatFanMode', true),
								fanMinOnTime:		stat.currentValue('fanMinOnTime', true),
								heatingSetpoint:	stat.currentValue('heatingSetpoint', true),
								coolingSetpoint:	stat.currentValue('coolingSetpoint', true),
								holdType:			stat.currentValue('lastHoldType', true),
								hasHumidifier:		stat.currentValue('hasHumidifier', true),
								humidifierMode:		stat.currentValue('humidifierMode', true),
								hasDehumidifier:	stat.currentValue('hasDehumidifier', true),
								dehumidifierMode:	stat.currentValue('dehumidifierMode', true),
								thermostatHold:		stat.currentValue('thermostatHold', true),
								currentProgramName:	stat.currentValue('currentProgramName', true),
								currentProgramId:	stat.currentValue('currentProgramId', true),
								currentProgram:		stat.currentValue('currentProgram', true),
								scheduledProgram:	stat.currentValue('scheduledProgram', true),
							 ] 
		}
        atomicState.statState = statState
    }
	subscribe(qtSwitch, "switch.${settings.qtOn}", quietOnHandler)
    def qtOff = settings.qtOn=='on'?'off':'on'
    subscribe(qtSwitch, "switch.${qtOff}", quietOffHandler)
   	if (!atomicState.isQuietTime) {
    	if (qtSwitch.currentSwitch == qtOn) {
        	quietOnHandler()
        } else {
        	atomicState.isQuietTime = false
        }
    } else if (qtSwitch.currentSwitch == qtOff) {
    	quietOffHandler()
    }
    if (atomicState.isQuietTime == null) atomicState.isQuietTime = false
    
    if (settings.hvacOff || (settings.hvacMode && (settings.quietMode == 'off'))) subscribe(theThermostats, 'thermostatMode', statModeChange)
    if (settings.fanOff) {
    	subscribe(theThermostats, 'thermostatFanMode', fanModeChange)
        if (settings.circOff) subscribe(theThermostats, 'fanMinOnTime', circTimeChange)
    }
    if (settings.humidOff) subscribe(theThermostats, 'humidifierMode', humidModeChange)
    if (settings.dehumOff) subscribe(theThermostats, 'dehumidifierMode', dehumModeChange)
    
	LOG("initialize() exiting")
}

def dehumModeChange(evt) {
	// only gets called if we are turning off the dehumidifier
    if (settings.humidOff && atomicState.isQuietTime && (evt.value != 'off')) {
    	def tid = getDeviceId(evt.device.deviceNetworkId)
    	if (evt.value != atomicState.statState[tid].dehumidifierMode) {
    		def statState = atomicState.statState
    		statState[tid].dehumidifierMode = evt.value	// update the saved time
    		atomicState.statState = statState
    	}
		   // For now, just cancel the reservation - don't take as a Quiet Time override
        cancelReservation( tid, 'dehumOff' )
    }
}

def humidModeChange(evt) {
	// only gets called if we are turning off the humidifier
    if (settings.humidOff && atomicState.isQuietTime && (evt.value != 'off')) {
    	def tid = getDeviceId(evt.device.deviceNetworkId)
    	if (evt.value != atomicState.statState[tid].humidifierMode) {
    		def statState = atomicState.statState
    		statState[tid].humidifierMode = evt.value	// update the saved time
    		atomicState.statState = statState
    	}
        // For now, just cancel the reservation - don't take as a Quiet Time override
       	cancelReservation( tid, 'humidOff' )
    }
}

def circTimeChange(evt) {
	// only gets called if we are turning off the thermostat's circulation time
    if (settings.circOff && atomicState.isQuietTime && (evt.value != 0)) {
    	def tid = getDeviceId(evt.device.deviceNetworkId)
    	if (evt.value != atomicState.statState[tid].fanMinOnTime) {
    		def statState = atomicState.statState
    		statState[tid].fanMinOnTime = evt.value	// update the saved time
    		atomicState.statState = statState
    	}
        // NOTIFY here?
        LOG("${evt.device.displayName} Fan minimum circulation time changed to ${evt.value}, exiting Quiet Time",2,null,'info')
        quietOffHandler(null)					// effect the override
    }
}

def fanModeChange(evt) {
	// only gets called if we are turning off the thermostat's fan
    if (settings.fanOff && atomicState.isQuietTime && (evt.value != 'off')) {
        // somebody has overridden us...
        def tid = getDeviceId(evt.device.deviceNetworkId)
        if (evt.value != atomicState.statState[tid].thermostatFanMode) {
            def statState = atomicState.statState
            statState[tid].thermostatFanMode = evt.value	// update the saved mode
            atomicState.statState = statState
        }
        // NOTIFY here?
        LOG("${evt.device.displayName} Fan Mode changed to ${evt.value}, exiting Quiet Time",2,null,'info')
        quietOffHandler(null)					// effect the override       
    }
}

def statModeChange(evt) {
	// only gets called if we are turning off the HVAC
    if ((settings.hvacOff || (settings.hvacMode && (settings.quietMode == 'off'))) && atomicState.isQuietTime && (evt.value != 'off')) {
        // somebody has overridden us...
        def tid = getDeviceId(evt.device.deviceNetworkId)
        if (evt.value != atomicState.statState[tid].thermostatMode) {
            def statState = atomicState.statState
            statState[tid].thermostatMode = evt.value	// update the saved mode
            atomicState.statState = statState
        }
        // NOTIFY here?
        LOG("${evt.device.displayName} Mode changed to ${evt.value}, exiting Quiet Time",2,null,'info')
        quietOffHandler(null)					// effect the override
    }
}

def quietOnHandler(evt=null) {
	LOG("Quiet Time On requested",2,null,'info')
	// atomicState.isQuietTime = true		// don't turn this on until we are all configured
    // Allow time for other Helper apps using the same Quiet Time switches to save their states
    runIn(3, 'turnOnQuietTime', [overwrite: true])
}

void turnOnQuietTime() {
	LOG("Turning On Quiet Time",2,null,'info')
	def statState = atomicState.statState
	//boolean ST = isST
	
    clearReservations()			// clean slate
	settings.theThermostats.each() { stat ->
    	def tid = getDeviceId(stat.deviceNetworkId)
        if (settings.hvacOff) {
        	makeReservation(tid, 'modeOff')							// We have to reserve this now, to stop other Helpers from turning it back on
        	statState[tid].thermostatMode = ST ? stat.currentValue('thermostatMode') : stat.currentValue('thermostatMode', true)
            if (statState[tid].thermostatMode != 'off') stat.setThermostatMode('off')
            LOG("${stat.device.displayName} Mode is Off",3,null,'info')
        } else if (settings.hvacMode) {
        	statState[tid].thermostatMode = ST ? stat.currentValue('thermostatMode') : stat.currentValue('thermostatMode', true)
            if (settings.quietMode == 'off') {
            	makeReservation(tid, 'modeOff')
                if (statState[tid].thermostatMode != 'off') stat.setThermostatMode('off')
                LOG("${stat.device.displayName} Mode is Off",3,null,'info')
            } else if ((statState[tid].thermostatMode != 'off')  || !anyReservations(tid, 'modeOff')) {
            	stat.setThermostatMode(settings.quietMode)
            	LOG("${stat.device.displayName} Mode is ${settings.quietMode}",3,null,'info')
            } else {
               	LOG("Cannot change ${stat.device.displayName} to ${settings.quietMode} Mode - ${getGuestList(tid, 'modeOff')} hold 'modeOff' reservations",1,null,'warn')
            }
        }
        if (settings.fanOff) { 
        	makeReservation(tid, 'fanOff')						// reserve the fanOff also
        	statState[tid].thermostatFanMode = ST ? stat.currentValue('thermostatFanMode') : stat.currentValue('thermostatMode', true)
            stat.setThermostatFanMode('off','indefinite')
            LOG("${stat.device.displayName} Fan Mode is off",3,null,'info')
        }
        if (settings.circOff) { 
        	makeReservation(tid, 'circOff')							// reserve no recirculation as well (SKIP VACACIRCOFF FOR NOW!!!)
        	statState[tid].fanMinOnTime = ST ? stat.currentValue('fanMinOnTime') : stat.currentValue('fanMinOnTime', true)
            stat.setFanMinOnTime(0)
            LOG("${stat.device.displayName} Circulation time is 0 mins/hour",3,null,'info')
        }
        if ( !settings.hvacOff && !settings.hvacMode && settings.adjustSetpoints) {
        	statState[tid].holdType = 			ST ? stat.currentValue('lastHoldType') 				: stat.currentValue('lastHoldType', true)
        	statState[tid].heatingSetpoint = 	ST ? stat.currentValue('heatingSetpointDisplay') 	: stat.currentValue('heatingSetpointDisplay', true)
            statState[tid].coolingSetpoint = 	ST ? stat.currentValue('coolingSetpointDisplay') 	: stat.currentValue('coolingSetpointDisplay', true)
            def h = statState[tid].heatingSetpoint + settings.heatAdjust
            def c = statState[tid].coolingSetpoint + settings.coolAdjust
            stat.setHeatingSetpoint(h, 'indefinite')
            stat.setCoolingSetpoint(c, 'indefinite')
            LOG("${stat.device.displayName} heatingSetpoint adjusted to ${h}, coolingSetpoint to ${c}",3,null,'info')
        }
        if (settings.humidOff && (stat.currentValue('hasHumidifier') == 'true')) { 
        	makeReservation(tid, 'humidOff')
        	LOG("Turning off the humidifier",3,null,'info')
        	statState[tid].humidifierMode = stat.currentValue('humidifierMode')
            stat.setHumidifierMode('off')
            LOG("${stat.device.displayName} humidifierMode is off",3,null,'info')
        }
        if (settings.dehumOff && (stat.currentValue('hasDehumidifier') == 'true')) {
        	def dehumNow = stat.currentValue('dehumidifierMode')
            if (dehumNow == 'on') {
            	makeReservation(tid, 'dehumOff')
                LOG("Turning off the dehumidifier",3,null,'info')
        		statState[tid].dehumidifierMode = 'on'
            	stat.setDehumidifierMode('off')
            	LOG("${stat.device.displayName} dehumidifierMode is off",3,null,'info')
            } else {
            	LOG("Dehumidifier is already off",2,null,'warn')
                cancelReservation(tid, 'dehumOff')
                //log.debug anyReservations(tid, 'dehumOff')
                if (!anyReservations(tid, 'dehumOff')) {
                	makeReservation(tid, 'dehumOff')
                    statState[tid].dehumidifierMode = 'on' // we're going to try to turn it back on
                    LOG("Will turn it back on when Quiet Time ends",2,null,'warn')
                } else {
                	statState[tid].dehumidifierMode = 'off' // leave it alone
                    LOG("Will NOT turn it back on when Quiet Time ends",2,null,'warn')
                }
            }
        }
    }
    atomicState.statState = statState
    atomicState.isQuietTime = true
    
    if ((settings.qtAutoOff != null) && (settings.qtAutoOff != '(Disabled)')) {
    	def seconds = settings.qtAutoOff.contains('Minute')? (settings.qtAutoOff.tokenize()[0].toInteger() * 60) : (settings.quAutoOff.tokenize()[0].toInteger() * 3600)
        LOG("Quiet Time Auto Off scheduled in ${seconds} seconds.",2,null,'info')
       	runIn( seconds, turnQuietOff, [overwrite: true])
  	}        
    
    LOG('Quiet Time On is complete',2,null,'info')
}

void turnQuietOff() {
	LOG("Executing scheduled Auto Off for ${settings.qtSwitch.displayName}",2,null,'info')
    def qtOff = settings.qtOn=='on'?'off':'on'
    settings.qtSwitch."${qtOff}()"
}

def quietOffHandler(evt=null) {
    if (!atomicState.isQuietTime || (atomicState.quietTime = false)) {
    	LOG("Quiet Time Off requested, but not in Quiet Time",1,null,'warn')
        return
    }
	LOG("Quiet Time Off requested",2,null,'info')
	//boolean ST = isST
    
    if ((settings.qtAutoOff != null) && (settings.qtAutoOff != '(Disabled)')) { unschedule(turnQuietOff) }
   	atomicState.isQuietTime = false
   	// No delayed execution - 
   	// runIn(3, 'turnOffQuietTime', [overwrite: true])
   	def statState = atomicState.statState
   	if (statState) {
   		settings.theThermostats.each() { stat ->
        	def tid = getDeviceId(stat.deviceNetworkId)
        	cancelReservation(tid, 'circOff')			// ASAP so SmartCirculation can carry on
        	if ((settings.hvacOff || settings.hvacMode) && statState[tid]?.thermostatMode) {
				def ncTm = ST ? stat.currentValue('thermostatMode') : stat.currentValue('thermostatMode', true)
            	if (statState[tid]?.thermostatMode != 'off' && (ncTm == 'off')) {
                	if (settings.hvacOff || (settings.hvacMode && (settings.quietMode == 'off'))) {	
                    	// we wanted it off
                    	def i = countReservations(tid, 'modeOff') - (haveReservation(tid, 'modeOff')? 1 : 0)
                		if (i <= 0) {
                    		// no other reservations, we can turn it on
                            cancelReservation(tid, 'modeOff')
        					stat.setThermostatMode(statState[tid].thermostatMode)
                			LOG("${stat.device.displayName} Mode is ${statState[tid].thermostatMode}",3,null,'info')
                		} else {
                        	cancelReservation(tid, 'modeOff')			// just cancel our reservation for now
                    		LOG("${stat.device.displayName} has other 'modeOff' reservations",1,null,'info')
                    	}
                    } else {
                    	// We didn't turn it off
                        def i = countReservations(tid, 'modeOff') - (haveReservation(tid, 'modeOff')? 1 : 0)
                        if (i <= 0) {
                        	// seems nobody else has reserved it being off
                            cancelReservation(tid, 'modeOff')			// just in case, cancel our reservation
                            stat.setThermostatMode(statState[tid].thermostatMode)
                			LOG("${stat.device.displayName} Mode is ${statState[tid].thermostatMode}",3,null,'info')
                        } else {
                        	// Somebody else wants it off right now
                            cancelReservation(tid, 'modeOff')			// just cancel our reservation for now
                    		LOG("${stat.device.displayName} has other 'modeOff' reservations (${getGuestList(tid, 'modeOff')}",1,null,'info')
                        }
                    }
                } else {
                	//Odd, quiet time ends and NOW we turn off the thermostat???
                    makeReservation(tid, 'modeOff')
                    stat.setThermostatMode( 'off' )
                }
            }
        	if (settings.fanOff && statState[tid]?.thermostatFanMode) { 
            	cancelReservation(tid, 'fanOff')
            	stat.setThermostatFanMode(statState[tid].thermostatFanMode)
                LOG("${stat.device.displayName} Fan Mode is ${statState[tid].thermostatFanMode}",3,null,'info')
            }
        	if (settings.circOff && statState[tid]?.fanMinOnTime) { 
            	// cancelReservation(tid, 'circOff')
            	stat.setFanMinOnTime(statState[tid].fanMinOnTime)
                LOG("${stat.device.displayName} Circulation time is ${statState[tid].fanMinOnTime} mins/hour",3,null,'info')
            }
            if ((settings.hvacOff || settings.hvacMode || settings.fanOff) && settings.modeResume) {
            	stat.resumeProgram()
                LOG("${stat.device.displayName} resuming currently scheduled program",3,null,'info')
            } else {
        		if (settings.adjustSetpoints) {
                	if (settings.setpointResume == 'Resume Scheduled Program') {
                    	stat.resumeProgram()
                        LOG("${stat.device.displayName} resuming currently scheduled program",3,null,'info')
                    } else if (settings.setpointResume == 'Resume Current Program') {
                    	// If the scheduled program is still the same as when quiet time started, and there was a hold active at the start of Quiet Time
                        // then resume the program that was current at that time
						def ncSp = ST ? stat.currentValue('scheduledProgram') : stat.currentValue('scheduledProgram', true)
                        if ((ncSp == statState[tid].scheduledProgram) && (statState[tid].currentProgram != statState[tid].scheduledProgram)) {
                        	stat.setProgram(statState[tid].currentProgram)
                            LOG("${stat.device.displayName} resumed prior Hold: ${statState[tid].currentProgram}",3,null,'info')
                        } else {
                        	// No choice but to resume current program
                            stat.resumeProgram()
                            LOG("${stat.device.displayName} resuming currently scheduled program",3,null,'info')
                        }
                    } else {
                		if (statState[tid]?.heatingSetpoint) stat.setHeatingSetpoint(statState[tid].heatingSetpoint, statState[tid].lastHoldType)
                    	if (statState[tid]?.coolingSetpoint) stat.setCoolingSetpoint(statState[tid].coolingSetpoint, statState[tid].lastHoldType)
                        LOG("${stat.device.displayName} heatingSetpoint adjusted to ${statState[tid].heatingSetpoint}, coolingSetpoint to ${statState[tid].coolingSetpoint}",3,null,'info')
                    }
                }
            }
        	if (settings.humidOff && (stat.currentValue('hasHumidifier') == 'true') && statState[tid]?.humidifierMode) {
            	cancelReservation(tid, 'humidOff')
          		stat.setHumidifierMode(statState[tid].humidifierMode)
                LOG("${stat.device.displayName} humidifierMode is ${statState[tid].humidifierMode}",3,null,'info')
            }
        	if (settings.dehumOff && (stat.currentValue('hasDehumidifier') == 'true') && statState[tid]?.dehumidifierMode) {
            	LOG("Turning ${statState[tid]?.dehumidifierMode} the Dehumidifier",3,null,'info')
            	cancelReservation(tid, 'dehumOff')
                if (!anyReservations(tid, 'dehumOff') && (statState[tid].dehumidifierMode == 'on')) {
            		stat.setDehumidifierMode(statState[tid].dehumidifierMode)
                	LOG("${stat.device.displayName} dehumidifierMode is ${statState[tid].dehumidifierMode}",3,null,'info')
                } else {
                	LOG("Cannot turn on the dehumidifier, ${getGuestList(tid,'dehumOff').toString()[1..-2]} still hold 'dehumOff' reservations.",2,null,'warn')
                }
            }
        }
    }
    LOG('Quiet Time Off is complete',2,null,'info')
}

boolean hasHumidifier() {
	def result = false
	settings.theThermostats.each {
    	if (!result) {
			def hh = it.currentValue('hasHumidifier')
			if ((hh != null) && ((hh == true) || (hh == 'true'))) result = true
        }
	}
	atomicState.hasHumidifier = result
    return result
}	

boolean hasDehumidifier() {
	def result = false
	settings.theThermostats.each {
    	if (!result) {
			def hd = it.currentValue('hasDehumidifier')
			if ((hd != null) && ((hd == true) || (hd == 'true'))) result = true
        }
	}
	atomicState.hasDehumidifier = result
    return result
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(String tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id as String, type )
}
// Cancel my reservation
void cancelReservation(String tid, String type='modeOff') {
	// log.debug "cancel ${tid}, ${type}"
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

// Helper Functions
String getDeviceId(networkId) {
	// def deviceId = networkId.split(/\./).last()	
    // LOG("getDeviceId() returning ${deviceId}", 4, null, 'trace')
    // return deviceId
    return networkId.split(/\./).last()
}

// return all the modes that ALL thermostats support
def getThermostatModes() {
	//log.debug "${app.name}, ${app.label}"
	def theModes = []
    
    settings.theThermostats.each { stat ->
    	if (theModes == []) {
        	theModes = stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", ")
        } else {
        	theModes = theModes.intersect(stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", "))
        }   
    }
    theModes = theModes - ['off']
    return theModes.sort(false)
}

/* NOTIFICATIONS NOT YET IMPLEMENTED
void sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "trace")
    if (settings.notify) {
        String msg = "${atomicState.appDisplayName} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (isST) {
			if (settings.phone) { // check that the user did select a phone number
				if ( settings.phone.indexOf(";") > 0){
					def phones = settings.phone.split(";")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i].trim(), msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone.trim(), msg)						// Only to SMS contact
				}
			} 
			if (settings.pushNotify) {
				LOG("Sending Push to everyone", 3, null, 'warn')
				sendPushMessage(msg)								// Push to everyone
			}
			if (settings.speak) {
				if (settings.speechDevices != null) {
					settings.speechDevices.each {
						it.speak( "From " + msg )
					}
				}
				if (settings.musicDevices != null) {
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText( "From " + msg )
					}
				}
			}
		} else {		// HE
			if (settings.notifiers != null) {
				settings.notifiers.each {							// Use notification devices on Hubitat
					it.deviceNotification(msg)
				}
			}
			if (settings.phone != null) {
				if ( settings.phone.indexOf(",") > 0){
					def phones = phone.split(",")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i].trim(), msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone.trim(), msg)						// Only to SMS contact
				}
			}
			if (settings.speak) {
				if (settings.speechDevices != null) {
					settings.speechDevices.each {
						it.speak( "From " + msg )
					}
				}
				if (settings.musicDevices != null) {
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText( "From " + msg )
					}
				}
			}
			
		}
    }
	// Always send to Hello Home / Location Event log
	if (isST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
}
*/
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
void updateMyLabel() {
	//boolean ST = isST
    
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
String pageTitle 	(String txt) 	{ return HE ? getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') : txt }
String pageTitleOld	(String txt)	{ return HE ? getFormat('header-ecobee','<h2>'+txt+'</h2>') 	: txt }
String sectionTitle	(String txt) 	{ return HE ? getTheSectionBeeLogo() + getFormat('header-nobee','<h3><b>&nbsp;&nbsp;'+txt+'</b></h3>')	: txt }
String smallerTitle	(String txt) 	{ return txt ? (HE ? '<h3><b>'+txt+'</b></h3>' 				: txt) : '' }
String sampleTitle	(String txt) 	{ return HE ? '<b><i>'+txt+'<i></b>'			 				: txt }
String inputTitle	(String txt) 	{ return HE ? '<b>'+txt+'</b>'								: txt }
String getWarningText()				{ return HE ? "<span style='color:red'><b>WARNING: </b></span>"	: "WARNING: " }
String getFormat(type, myText=""){
	switch(type) {
		case "header-ecobee":
			return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theBee}${myText}</div>"
			break;
		case "header-nobee":
			return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
			break;
    	case "line":
			return HE ? "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>" : "-----------------------------------------------"
			break;
		case "title":
			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
			break;
		case "warning":
			return HE ? "<span style='color:red'><b>WARNING: </b><i></span>${myText}</i>" : "WARNING: ${myText}"
			break;
		case "note":
			return HE ? "<b>NOTE: </b>${myText}" : "NOTE:<br>${myText}"
			break;
		default:
			return myText
			break;
	}
}

// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019-2020, Barry A. Burke (storageanarchy@gmail.com)
String getPlatform() { return ((hubitat?.device?.HubAction == null) ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST() {
	if (ST == null) {
    	// ST = physicalgraph?.device?.HubAction ? true : false // this no longer compiles on Hubitat for some reason
        if (HE == null) HE = getIsHE()
        ST = !HE
    }
    return ST    
}
boolean getIsHE() {
	if (HE == null) {
    	HE = hubitat?.device?.HubAction ? true : false
        if (ST == null) ST = !HE
    }
    return HE
}

String getHubPlatform() {
    hubPlatform = getIsST() ? "SmartThings" : "Hubitat"
	return hubPlatform
}
boolean getIsSTHub() { return isST }					// if (isSTHub) ...
boolean getIsHEHub() { return isHE }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return ST ? parent?.settings?."${settingName}" : parent?."${settingName}"
}
@Field String  hubPlatform 	= getHubPlatform()
@Field boolean ST 			= getIsST()
@Field boolean HE 			= getIsHE()
