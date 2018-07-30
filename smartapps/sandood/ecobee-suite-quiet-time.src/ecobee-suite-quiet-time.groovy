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
 *	1.5.00a- Initial Release
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Allow Ecobee Suite Thermostats only
 *	1.5.02 - Fixed statState error
 *	1.5.03 - Converted all setpoint math to BigDecimal
 *	1.5.04 - Add modeOff reservation support
 *	1.5.05 - Removed Notifications settings - not yet implemented
 *	1.6.00 - Release number synchronization
 *	1.6.01 - Fix reservation initialization error
 *	1.6.02 - REALLY fix reservation initialization error
 *	1.6.03 - Really, REALLY fix reservation initialization error
 *	1.6.10 - Converted to parent-based reservations
 */
def getVersionNum() { return "1.6.10" }
private def getVersionLabel() { return "Ecobee Suite Quiet Time Helper, version ${getVersionNum()}" }

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
    	section(title: "Name for this Quiet Time Helper") {
        	label title: "Name this Helper", required: true, defaultValue: "Quiet Time"
        }
        
        section(title: "Select Thermostats") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled per request. Turn back on below to activate handler." }
        	else { 
            	input(name: "theThermostats", type: "device.ecobeeSuiteThermostat", title: "Select Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)
            }          
		}
    
		if (!settings.tempDisable && (settings.theThermostats?.size() > 0)) {

		section(title: 'Quiet Time Control Switch') {
        	paragraph("Quiet Time is enabled by turning on (or off) a physical or virtual switch.")
            input(name: 'qtSwitch', type: 'capability.switch', required: true, title: 'Which switch controls Quiet Time?', multiple: false, submitOnChange: true)
            if (settings.qtSwitch) {
                input(name: "qtOn", type: "enum", title: "Effect Quiet Time Actions when ${settings.qtSwitch.displayName} is turned:", defaultValue: 'on', required: true, multiple: false, options: ["on","off"])
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
            		input(name: 'quietMode', title: 'Set thermostat mode to', type: 'enum', description: 'Tap to choose...', required: true, multiple: false, 
                			options:getThermostatModes())
                	if (settings.quietMode) paragraph("HVAC mode will be set to ${settings.quietMode} Mode.${(settings.quietMode=='off')?' Circulation, Humidification and/or Dehumidification may still operate while HVAC is Off.':''}")
                }
            }
            if (settings.hvacOff || hvacMode) paragraph("HVAC mode will be returned to its original value when Quiet Time ends")

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
                if (adjustSetpoints) {
                   	input(name: 'heatAdjust', type: 'decimal', title: 'Heating setpoint adjustment (+/- 20°)', required: true, defaultValue: 0.0, range: '-20..20')
                    input(name: 'coolAdjust', type: 'decimal', title: 'Cooling setpoint adjustment (+/- 20°)', required: true, defaultValue: 0.0, range: '-20..20')
                	input(name: 'setpointResume', type: 'enum', title: 'At the end of Quiet Time', description: 'Tap to choose...', multiple: false, required: true,
                			options: ['Restore prior Setpoints','Resume Current Program', 'Resume Scheduled Program'], submitOnChange: true)
                    if (setpointResume) paragraph("At the end of Quiet Time, ${settings.setpointResume.startsWith('Resu')?'the currently scheduled program will be resumed.':'the prior setpoints will be restored.'}")
                }
            }
            if (hasHumidifier()) {
				input(name: 'humidOff', type: 'bool', title: 'Turn off the Humidifier?', required: true, defaultValue: false, submitOnChange: true)
                if (settings.humidOff) paragraph("At the end of Quiet Time, the humidifier(s) will be turned back on.")
            }
            if (hasDehumidifier()) {
				input(name: 'dehumOff', type: 'bool', title: 'Turn off the Dehumidifier?', required: true, defaultValue: false, submitOnChange: true)
                if (settings.dehumOff) paragraph("At the end of Quiet Time, the dehumidifier(s) will be turned back on.")
            }
        }
//        section(title: 'Actions when Quiet Time Ends') {
//        	input(name: 'loudActions', type: 'enum', title: 'When quiet time ends', description: 'Tap to choose...', required: true, defaultValue: 'Resume Current Program', 
//            		options: ['Resume Current Program', "${adjustSetpoints?'Set Hold with Prior Setpoints':''}", 
//        }
        
/* NOTIFICATIONS NOT YET IMPLEMENTED 
		section(title: "Notification Preferences") {
        	input(name: "whichAction", title: "Select which notification actions to take [Default=Notify Only]", type: "enum", required: true, 
               	metadata: [values: ["Notify Only", "Quiet Time Actions Only", "Notify and Quiet Time Actions"]], defaultValue: "Notify Only", submitOnChange: true)
			if (settings.whichAction != "Quiet Time Actions Only") {
				input(name: 'recipients', title: 'Send notifications to', description: 'Contacts', type: 'contact', required: false, multiple: true) {
            				paragraph "You can enter multiple phone numbers seperated by a semi-colon (;)"
            				input "phone", "string", title: "Send SMS notifications to", description: "Phone Number(s)", required: false 
                }
                if ((!location.contactBookEnabled || !settings.recipients) && !settings.phone) {
                    input( name: 'sendPush', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false)
                }
                if ((!location.contactBookEnabled || !settings.recipients) && !settings.phone && !settings.sendPush) paragraph "Notifications configured, but nobody to send them to!"
            }               
       }
*/
		section(title: "Temporarily Disable?") {
			input(name: "tempDisable", title: "Temporarily Disable Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        
        section (getVersionLabel()) {}
        }
    }
}

// Main functions
def installed() {
	LOG("installed() entered", 5)
	initialize()  
}
def uninstalled () {
	theThermostats.each {
    	cancelReservation( getDeviceId(it.deviceNetworkId), 'modeOff' )
        cancelReservation( getDeviceId(it.deviceNetworkId), 'fanOff' )
        cancelReservation( getDeviceId(it.deviceNetworkId), 'circOff' )
        cancelReservation( getDeviceId(it.deviceNetworkId), 'humidOff' )
        cancelReservation( getDeviceId(it.deviceNetworkId), 'dehumOff' )
	}   
}
def updated() {
	LOG("updated() entered", 5)
	unsubscribe()
	unschedule()
	initialize()
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 3, null, 'info')
    log.debug "${app.name}, ${app.label}"
	if(tempDisable == true) {
		LOG("Temporarily Disabled as per request.", 2, null, "warn")
		return true
	}
    // Initialize the saved states
    def statState = [:]
    settings.theThermostats.each() { stat ->
    	def tid = getDeviceId(stat.deviceNetworkId)
    	statState[tid] = [
        					thermostatMode: 	stat.currentValue('thermostatMode'),
                            thermostatFanMode: 	stat.currentValue('thermostatFanMode'),
                            fanMinOnTime:		stat.currentValue('fanMinOnTime'),
                            heatingSetpoint:	stat.currentValue('heatingSetpointDisplay'),
                            coolingSetpoint:	stat.currentValue('coolingSetpointDisplay'),
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
        atomicState.statState = statState
    }
	subscribe(qtSwitch, "switch.${qtOn}", quietOnHandler)
    def qtOff = qtOn=='on'?'off':'on'
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
    
	LOG("initialize() exiting")
}

def quietOnHandler(evt=null) {
	LOG("Quiet Time On requested",2,null,'info')
	atomicState.isQuietTime = true
    // Allow time for other Helper apps using the same Quiet Time switches to save their states
    runIn(3, 'turnOnQuietTime', [overwrite: true])
}

def turnOnQuietTime() {
	LOG("Turning On Quiet Time",2,null,'info')
	def statState = atomicState.statState
	settings.theThermostats.each() { stat ->
    	def tid = getDeviceId(stat.deviceNetworkId)
        if (settings.hvacOff) {
        	statState[tid].thermostatMode = stat.currentValue('thermostatMode')
            makeReservation(tid, 'modeOff')							// We have to reserve this now, to stop other Helpers from turning it back on
            if (statState[tid].thermostatMode != 'off') stat.setThermostatMode('off')
            LOG("${stat.device.displayName} Mode is Off",3,null,'info')
        } else if (settings.hvacMode) {
        	statState[tid].thermostatMode = stat.currentValue('thermostatMode')
            if (settings.quietMode == 'off') {
            	makeReservation(tid, 'modeOff')
                if (statState[tid].thermostatMode != 'off') stat.setThermostatMode('off')
                LOG("${stat.device.displayName} Mode is Off",3,null,'info')
            } else {
            	if ((statState[tid].thermostatMode != 'off')  || !anyReservations(tid, 'modeOff')) {
                	cancelReservation(tid,'modeOff')				// just in case
            		stat.setThermostatMode(settings.quietMode)
            		LOG("${stat.device.displayName} Mode is ${settings.quietMode}",3,null,'info')
                } else {
                	LOG("Cannt change ${stat.device.displayName} to ${settings.quietMode} Mode - ${getGuestList(tid, 'modeOff')} hold 'modeOff' reservations",1,null,'warn')
                }
            }
        }
        if (settings.fanOff) { 
        	statState[tid].thermostatFanMode = stat.currentValue('thermostatFanMode')
            makeReservation(tid, 'fanOff')						// reserve the fanOff also
            stat.setThermostatFanMode('off','indefinite')
            LOG("${stat.device.displayName} Fan Mode is off",3,null,'info')
        }
        if (settings.circOff) { 
        	statState[tid].fanMinOnTime = stat.currentValue('fanMinOnTime')
            makeReservation(tid, 'circOff')							// reserve no recirculation as well (SKIP VACACIRCOFF FOR NOW!!!)
            stat.setFanMinOnTime(0)
            LOG("${stat.device.displayName} Circulation time is 0 mins/hour",3,null,'info')
        }
        if ( !settings.hvacOff && !settings.hvacMode && settings.adjustSetpoints) {
        	statState[tid].holdType = stat.currentValue('lastHoldType')
        	statState[tid].heatingSetpoint = stat.currentValue('heatingSetpointDisplay')
            statState[tid].coolingSetpoint = stat.currentValue('coolingSetpointDisplay')
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
    LOG('Quiet Time On is complete',2,null,'info')
}

def quietOffHandler(evt=null) {
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
            	if (statState[tid]?.thermostatMode != 'off' && (stat.currentValue('thermostatMode') == 'off')) {
                	if (settings.hvacOff || (settings.hvacMode && (settings.quietMode == 'off'))) {	
                    	// we wanted it off
                    	def i = countReservations(tid, 'modeOff') - (haveReservation(tid, 'modeOff')? 1 : 0)
                		if (i <- 0) {
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
                        if ((stat.currentValue('scheduledProgram') == statState[tid].scheduledProgram) && (statState[tid].currentProgram != statState[tid].scheduledProgram)) {
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

def hasHumidifier() {
	return (theThermostats.currentValue('hasHumidifier').contains('true'))
}	

def hasDehumidifier() {
	return (theThermostats.currentValue('hasDehumidifier').contains('true'))
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id, type )
}
// Cancel my reservation
void cancelReservation(tid, String type='modeOff') {
	log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id, type )
}
// Do I have a reservation?
Boolean haveReservation(tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id, type )
}
// Do any Apps have reservations?
Boolean anyReservations(tid, String type='modeOff') {
	return parent.anyReservations( tid, type )
}
// How many apps have reservations?
Integer countReservations(tid, String type='modeOff') {
	return parent.countReservations( tid, type )
}
// Get the list of app IDs that have reservations
List getReservations(tid, String type='modeOff') {
	return parent.getReservations( tid, type )
}
// Get the list of app Names that have reservations
List getGuestList(tid, String type='modeOff') {
	return parent.getGuestList( tid, type )
}

// Helper Functions
private def getDeviceId(networkId) {
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
private def sendMessage(notificationMessage) {
	LOG("Notification Message: ${notificationMessage}", 2, null, "trace")

    String msg = "${app.Label} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
    if (location.contactBookEnabled && settings.recipients) {
        sendNotificationToContacts(msg, settings.recipients, [event: true]) 
    } else if (phone) { // check that the user did select a phone number
        if ( phone.indexOf(";") > 0){
            def phones = phone.split(";")
            for ( def i = 0; i < phones.size(); i++) {
                LOG("Sending SMS ${i+1} to ${phones[i]}",2,null,'info')
                sendSms(phones[i], msg)
            }
        } else {
            LOG("Sending SMS to ${phone}",2,null,'info')
            sendSms(phone, msg)
        }
    } else if (settings.sendPush) {
        LOG("Sending Push to everyone",2,null,'warn')
        sendPush(msg)
    }
}
*/

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${app.label} ${message}"
    if (logType == null) logType = 'debug'
    log."${logType}" message
	//parent.LOG(msg, level, null, logType, event, displayEvent)
}
