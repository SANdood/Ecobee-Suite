/**
 *  ecobee Suite Quiet Time
 *
 *	Copyright 2017 Barry A. Burke *
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
 *	1.6.00 - Release number synchronization
 *	1.6.01 - Fix reservation initialization error
 *	1.6.02 - REALLY fix reservation initialization error
 *	1.6.03 - Really, REALLY fix reservation initialization error
 *	1.6.10 - Converted to parent-based reservations
 *	1.6.11 - Clear reservations when disabled
 *	1.6.12 - Clear reservations on manual override
 *	1.6.13 - Removed use of *SetpointDisplay
 *	1.6.14 - Fixed typo (thanks @jml923)
 *	1.6.15 - Added scheduled Auto Off for Quiet Time
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - Use nonCached currentValue() for stat attributes on Hubitat
 *	1.7.02 - Fixing private method issue caused by grails
 */
String getVersionNum() { return "1.7.02" }
String getVersionLabel() { return "Ecobee Suite Quiet Time Helper,\nversion ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: "ecobee Suite Quiet Time",
	namespace: "sandood",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSets HVAC into user-specified 'Quiet Mode' when a specified switch (real or virtual) is enabled.",
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
    	section(title: "") {
        	String defaultLabel = "Quiet Time"
        	label(title: "Name for this ${defaultLabel} Helper", required: true, defaultValue: defaultLabel)
            if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			}
			if (isHE) {
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
        	if(settings.tempDisable) { 
				paragraph "WARNING: Temporarily Paused - re-enable below." 
			} else { 
            	input(name: "theThermostats", type: "${isST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)
            }          
		}
    
		if (!settings.tempDisable && (settings.theThermostats?.size() > 0)) {
			def hasH = hasHumidifier()
			def hasD = hasDehumidifier()
			
			section(title: 'Quiet Time Control Switch') {
				paragraph("Quiet Time is enabled by turning on (or off) a physical or virtual switch.")
				input(name: 'qtSwitch', type: 'capability.switch', required: true, title: 'Which switch controls Quiet Time?', multiple: false, submitOnChange: true)
				if (settings.qtSwitch) {
					input(name: "qtOn", type: "enum", title: "Effect Quiet Time Actions when switch '${settings.qtSwitch.displayName}' is turned", defaultValue: 'on', required: true, multiple: false, submitOnChange: true,
							options: ["on","off"])
					input(name: "qtAutoOff", type: "enum", title: "Auto-disable Quiet Time after ", descriptionText: (settings?.qtAutoOff != null)?:'(Disabled)', defaultValue: '(Disabled)', required: true, multiple: false, submitOnChange: true,
							options: ["(Disabled)", "10 Minutes", "15 Minutes", "30 Minutes", "45 Minutes", "1 Hour", "2 Hours", "3 Hours", "4 Hours", "6 Hours"])
				}
			}

			section(title: "Quiet Time Actions") {
				input(name: 'hvacOff', type: "bool", title: "Turn off HVAC?", required: true, defaultValue: false, submitOnChange: true)
				if (settings.hvacOff) {
					paragraph("HVAC Mode will be set to Off. Circulation, Humidification and/or Dehumidification may still operate while HVAC is Off.")
				}
				if (!settings.hvacOff) {
					input(name: 'hvacMode', type: 'bool', title: 'Change HVAC Mode?', required: true, defaultValue: false, submitOnChange: true)
					if (settings.hvacMode) {
						input(name: 'quietMode', title: 'Set thermostat mode to', type: 'enum', required: true, multiple: false, 
								options:getThermostatModes())
						if (settings.quietMode) paragraph("HVAC mode will be set to ${settings.quietMode} Mode.${(settings.quietMode=='off')?' Circulation, Humidification and/or Dehumidification may still operate while HVAC is Off.':''}")
					}
				}
				if (settings.hvacOff || settings.hvacMode) paragraph("HVAC mode will be returned to its original value when Quiet Time ends")

				input(name: 'fanOff', type: "bool", title: "Turn off the Fan?", required: true, defaultValue: false, submitOnChange: true)
				if (settings.fanOff) {
					paragraph('Turning off the fan will not stop automatic circulation, even if the HVAC is also off.')
					input(name: 'circOff', type: "bool", title: 'Also disable Circulation?', required: true, defaultValue: false, submitOnChange: true)
					if (settings.circOff) {
						paragraph("Circulation will also be disabled.")
					} else {
						paragraph("Circulation will not be modified.")
					}
					paragraph("At the end of Quiet Time, the Fan Mode will be restored to its prior setting${settings.circOff?', and circulation will be re-enabled':''}.")
				}
				if (settings.hvacOff || settings.hvacMode || settings.fanOff) {
					input(name: 'modeResume', type: 'bool', title: 'Also resume current program at the end of Quiet Time (recommended)?', defaultValue: true, required: true)
				}

				if (!settings.hvacOff && !settings.hvacMode) {
					input(name: 'adjustSetpoints', type: 'bool', title: 'Adjust heat/cool setpoints?', required: true, defaultValue: false, submitOnChange: true)
					if (settings.adjustSetpoints) {
						input(name: 'heatAdjust', type: 'decimal', title: 'Heating setpoint adjustment (+/- 20°)', required: true, defaultValue: 0.0, range: '-20..20')
						input(name: 'coolAdjust', type: 'decimal', title: 'Cooling setpoint adjustment (+/- 20°)', required: true, defaultValue: 0.0, range: '-20..20')
						input(name: 'setpointResume', type: 'enum', title: 'At the end of Quiet Time', description: 'Tap to choose...', multiple: false, required: true,
								options: ['Restore prior Setpoints','Resume Current Program', 'Resume Scheduled Program'], submitOnChange: true)
						if (settings.setpointResume) paragraph("At the end of Quiet Time, ${settings.setpointResume.startsWith('Resu')?'the currently scheduled program will be resumed.':'the prior setpoints will be restored.'}")
					}
				}
				if ((settings.theThermostats?.size() != 0) && atomicState.hasHumidifier) {
					input(name: 'humidOff', type: 'bool', title: 'Turn off the Humidifier?', required: true, defaultValue: false, submitOnChange: true)
					if (settings.humidOff) paragraph("At the end of Quiet Time, the humidifier(s) will be turned back on.")
				}
				if ((settings.theThermostats?.size() != 0) && atomicState.hasDehumidifier) {
					input(name: 'dehumOff', type: 'bool', title: 'Turn off the Dehumidifier?', required: true, defaultValue: false, submitOnChange: true)
					if (settings.dehumOff) paragraph("At the end of Quiet Time, the dehumidifier(s) will be turned back on.")
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
				} else {		// isHE
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
		section(title: "Temporarily Disable?") {
			input(name: "tempDisable", title: "Pause this Helper?", type: "bool", required: false, description: "", submitOnChange: true)                
		}

		section (getVersionLabel()) {}
    }
}

// Main functions
void installed() {
	LOG("installed() entered", 5)
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
	LOG("updated() entered", 5)
	unsubscribe()
	unschedule()
	initialize()
}
def initialize() {
	LOG("${getVersionLabel()}\nInitializing...", 3, null, 'info')
    log.debug "${app.name}, ${app.label}"
	updateMyLabel()
	
	if(tempDisable == true) {
    	clearReservations()
		LOG("Temporarily Paused", 2, null, "warn")
		return true
	}
    // Initialize the saved states
    def statState = [:]
    settings.theThermostats.each() { stat ->
    	def tid = getDeviceId(stat.deviceNetworkId)
		if (isST) {
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
    clearReservations()			// clean slate
	settings.theThermostats.each() { stat ->
    	def tid = getDeviceId(stat.deviceNetworkId)
        if (settings.hvacOff) {
        	statState[tid].thermostatMode = isST ? stat.currentValue('thermostatMode') : stat.currentValue('thermostatMode', true)
            makeReservation(tid, 'modeOff')							// We have to reserve this now, to stop other Helpers from turning it back on
            if (statState[tid].thermostatMode != 'off') stat.setThermostatMode('off')
            LOG("${stat.device.displayName} Mode is Off",3,null,'info')
        } else if (settings.hvacMode) {
        	statState[tid].thermostatMode = isST ? stat.currentValue('thermostatMode') : stat.currentValue('thermostatMode', true)
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
        	statState[tid].thermostatFanMode = isST ? stat.currentValue('thermostatFanMode') : stat.currentValue('thermostatMode', true)
            makeReservation(tid, 'fanOff')						// reserve the fanOff also
            stat.setThermostatFanMode('off','indefinite')
            LOG("${stat.device.displayName} Fan Mode is off",3,null,'info')
        }
        if (settings.circOff) { 
        	statState[tid].fanMinOnTime = isST ? stat.currentValue('fanMinOnTime') : stat.currentValue('fanMinOnTime', true)
            makeReservation(tid, 'circOff')							// reserve no recirculation as well (SKIP VACACIRCOFF FOR NOW!!!)
            stat.setFanMinOnTime(0)
            LOG("${stat.device.displayName} Circulation time is 0 mins/hour",3,null,'info')
        }
        if ( !settings.hvacOff && !settings.hvacMode && settings.adjustSetpoints) {
        	statState[tid].holdType = 			isST ? stat.currentValue('lastHoldType') 			: stat.currentValue('lastHoldType', true)
        	statState[tid].heatingSetpoint = 	isST ? stat.currentValue('heatingSetpointDisplay') 	: stat.currentValue('heatingSetpointDisplay', true)
            statState[tid].coolingSetpoint = 	isST ? stat.currentValue('coolingSetpointDisplay') 	: stat.currentValue('coolingSetpointDisplay', true)
            def h = statState[tid].heatingSetpoint + settings.heatAdjust
            def c = statState[tid].coolingSetpoint + settings.coolAdjust
            stat.setHeatingSetpoint(h, 'indefinite')
            stat.setCoolingSetpoint(c, 'indefinite')
            LOG("${stat.device.displayName} heatingSetpoint adjusted to ${h}, coolingSetpoint to ${c}",3,null,'info')
        }
        if (settings.humidOff && (stat.currentValue('hasHumidifier') == 'true')) { 
        	LOG("Turning off the humidifier",3,null,'info')
        	statState[tid].humidifierMode = stat.currentValue('humidifierMode')
            makeReservation(tid, 'humidOff')
            stat.setHumidifierMode('off')
            LOG("${stat.device.displayName} humidifierMode is off",3,null,'info')
        }
        if (settings.dehumOff && (stat.currentValue('hasDehumidifier') == 'true')) {
        	def dehumNow = stat.currentValue('dehumidifierMode')
            if (dehumNow == 'on') {
        		LOG("Turning off the dehumidifier",3,null,'info')
        		statState[tid].dehumidifierMode = 'on'
            	makeReservation(tid, 'dehumOff')
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
    
    if ((settings.qtAutoOff == null) || (settings.qtAutoOff != '(Disabled)')) {
    	def seconds = settings.qtAutoOff.contains('Minute')? (settings.qtAutoOff.tokenize()[0].toInteger() * 60) : (settings.quAutoOff.tokenize()[0].toInteger() * 3600)
        LOG("Quiet Time Auto Off scheduled in ${seconds} seconds.",2,null,'info')
       	runIn( seconds, turnQuietOff, [overwrite: true])
  	}        
    
    LOG('Quiet Time On is complete',2,null,'info')
}

void turnQuietOff() {
	LOG("Executing scheduled Auto Off for ${settings.qtSwitch.displayName}",2,null,'info')
    def qtOff = settings.qtOn=='on'?'off':'on'
    settings.qtSwitch."${qtOff}"
}

def quietOffHandler(evt=null) {
    if (!atomicState.isQuietTime || (atomicState.quietTime = false)) {
    	LOG("Quiet Time Off requested, but not in Quiet Time",1,null,'warn')
        return
    }
	LOG("Quiet Time Off requested",2,null,'info')
    
   	atomicState.isQuietTime = false
   	// No delayed execution - 
   	// runIn(3, 'turnOffQuietTime', [overwrite: true])
   	def statState = atomicState.statState
   	if (statState) {
   		settings.theThermostats.each() { stat ->
        	def tid = getDeviceId(stat.deviceNetworkId)
        	cancelReservation(tid, 'circOff')			// ASAP so SmartCirculation can carry on
        	if ((settings.hvacOff || settings.hvacMode) && statState[tid]?.thermostatMode) {
				def ncTm = isST ? stat.currentValue('thermostatMode') : stat.currentValue('thermostatMode', true)
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
						def ncSp = isST ? stat.currentValue('scheduledProgram') : stat.currentValue('scheduledProgram', true)
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
	atomicState.hasDehumidifier = result
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
	log.debug "cancel ${tid}, ${type}"
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
log.debug "${app.name}, ${app.label}"
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
        String msg = "${app.label} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
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
		} else {		// isHE
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

void updateMyLabel() {
	String flag = isST ? ' (paused)' : '<span '
	
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
		def newLabel = myLabel + (isHE ? '<span style="color:orange"> Paused</span>' : ' (paused)')
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
	}
}

void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${app.label} ${message}"
    if (logType == null) logType = 'debug'
    log."${logType}" message
	//parent.LOG(msg, level, null, logType, event, displayEvent)
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
