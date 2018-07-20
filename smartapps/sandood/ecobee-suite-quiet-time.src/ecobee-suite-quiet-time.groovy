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
 *
 */
 
def getVersionNum() { return "1.5.02" }
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
                	if (settings.quietMode) paragraph("At the end of Quiet Time, the Thermostat Mode will be reset to its prior value.")
                }
            }
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
        
        section(title: "Notification Preferences") {
        	input(name: "whichAction", title: "Select which notification actions to take [Default=Notify Only]", type: "enum", required: true, 
               	metadata: [values: ["Notify Only", "Quiet Time Actions Only", "Notify and Quiet Time Actions"]], defaultValue: "Notify Only", submitOnChange: true)
			if (settings.whichAction != "Quiet Time Actions Only") {
				input("recipients", "contact", title: "Send notifications to") {
					input("phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false)
                }
        	}                
       }

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
	atomicState.isQuietTime = true
    // Allow time for other Helper apps using the same Quiet Time switches to save their states
    runIn(3, 'turnOnQuietTime', [overwrite: true])
}

def turnOnQuietTime() {
	def statState = atomicState.statState
	settings.theThermostat.each() { stat ->
    	def tid = getDeviceId(stat.deviceNetworkId)
        if (settings.hvacOff) {
        	statState[tid].thermostatMode = stat.currentValue('thermostatMode')
            stat.setThermostatMode('off')
            LOG("${stat.device.displayName} Mode is off",3,null,'info')
        } else if (settings.hvacMode) {
        	statState[tid].thermostatMode = stat.currentValue('thermostatMode')
            stat.setThermostatMode(settings.quietMode)
            LOG("${stat.device.displayName} Mode is ${settings.quietMode}",3,null,'info')
        }
        if (settings.fanOff) { 
        	statState[tid].thermostatFanMode = stat.currentValue('thermostatFanMode')
            stat.setThermostatFanMode('off','indefinite')
            LOG("${stat.device.displayName} Fan Mode is off",3,null,'info')
        }
        if (settings.circOff) { 
        	statState[tid].fanMinOnTime = stat.currentValue('fanMinOnTime')
            stat.setFanMinOnTime(0)
            LOG("${stat.device.displayName} Circulation time is 0 mins/hour",3,null,'info')
        }
        if ( !settings.hvacOff && !settings.hvacMode && settings.adjustSetpoints) {
        	statState[tid].holdType = stat.currentValue('lastHoldType')
        	statState[tid].heatingSetpoint = stat.currentValue('heatingSetpointDisplay')
            statState[tid].coolingSetpoint = stat.currentValue('coolingSetpointDisplay')
            Double h = statState[tid].heatingSetpoint.toDouble() + settings.heatAdjust.toDouble()
            Double c = statState[tid].coolingSetpoint.toDouble() + settings.coolAdjust.toDouble()
            stat.setHeatingSetpoint(h, 'indefinite')
            stat.setCoolingSetpoint(c, 'indefinite')
            LOG("${stat.device.displayName} heatingSetpoint adjusted to ${h}, coolingSetpoint to ${c}",3,null,'info')
        }
        if (settings.humidOff && (stat.currentValue('hasHumidifier') == 'true')) { 
        	statState[tid].humidifierMode = stat.currentValue('humidifierMode')
            stat.setHumidifierMode('off')
            LOG("${stat.device.displayName} humidifierMode is off",3,null,'info')
        }
        if (settings.dehumidOff && (stat.currentValue('hasDehumidifier') == 'true')) { 
        	statState[tid].dehumidifierMode = stat.currentValue('dehumidifierMode')
            stat.setDehumidifierMode('off')
            LOG("${stat.device.displayName} dehumidifierMode is off",3,null,'info')
        }
    }
    atomicState.statState = statState
}

def quietOffHandler(evt=null) {
   	atomicState.isQuietTime = false
   	// No delayed execution - 
   	// runIn(3, 'turnOffQuietTime', [overwrite: true])
   	def statState = atomicState.statState
   	if (stateState) {
   		settings.theThermostat.each() { stat ->
    		def tid = getDeviceId(stat.deviceNetworkId)
        	if ((settings.hvacOff || settingsHvacMode) && statState[tid]?.thermostatMode) { 
        		stat.setThermostatMode(statState[tid].thermostatMode)
                LOG("${stat.device.displayName} Mode is ${statState[tid].thermostatMode}",3,null,'info')
            }
        	if (settings.fanOff && statState[tid]?.thermostatFanMode) { 
            	stat.setThermostatFanMode(statState[tid].thermostatFanMode)
                LOG("${stat.device.displayName} Fan Mode is ${statState[tid].thermostatFanMode}",3,null,'info')
            }
        	if (settings.circOff && statState[tid]?.fanMinOnTime) { 
            	stat.setFanMinOnTime(statState[tid].fanMinOnTime)
                LOG("${stat.device.displayName} Circulation time is ${statState[tid].fanMinOnTime} mins/hour",3,null,'info')
            }
            if (settings.hvacOff || settings.hvacMode || settings.fanOff) {
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
          		stat.setHumidifierMode(statState[tid].humidifierMode)
                LOG("${stat.device.displayName} humidifierMode is ${statState[tid].humidifierMode}",3,null,'info')
            }
        	if (settings.dehumidOff && (stat.currentValue('hasDehumidifier') == 'true') && statState[tid]?.dehumidifierMode) { 
            	stat.setDehumidifierMode(statState[tid].dehumidifierMode)
                LOG("${stat.device.displayName} dehumidifierMode is ${statState[tid].dehumidifierMode}",3,null,'info')
            }
        }
    }
}

def hasHumidifier() {
	return (theThermostats.currentValue('hasHumidifier').contains('true'))
}	

def hasDehumidifier() {
	return (theThermostats.currentValue('hasDehumidifier').contains('true'))
}

// Helper Functions
private def getDeviceId(networkId) {
	def deviceId = networkId.split(/\./).last()	
    LOG("getDeviceId() returning ${deviceId}", 4, null, 'trace')
    return deviceId
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

private def sendNotification(message) {	
    if (location.contactBookEnabled && recipients) {
        LOG("Contact Book enabled!", 4, null, 'info')
        sendNotificationToContacts(message, recipients)
    } else {
        LOG("Contact Book not enabled", 4, null, 'info')
        if (phone) {
            sendSms(phone, message)
        }
    }
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${app.label} ${message}"
    if (logType == null) logType = 'debug'
    log."${logType}" message
	//parent.LOG(msg, level, null, logType, event, displayEvent)
}
