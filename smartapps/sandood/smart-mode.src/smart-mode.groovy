/**
 *  Ecobee Suite Smart Mode
 *
 *  Copyright 2018 Justin Leonard, Barry A. Burke
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
 *
 */
def getVersionNum() { return "1.4.0" }
private def getVersionLabel() { return "Ecobee Suite Smart Mode, version ${getVersionNum()}" }

definition(
	name: "Ecobee Suite Smart Mode",
	namespace: "sandood",
	author: "Justin J. Leonard & Barry A. Burke",
	description: "Set Ecobee Heat/Cool/Auto Mode Based on outside temperature",
	category: "Convenience",
	parent: "sandood:Ecobee Suite Manager",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Setup ${getVersionLabel()}", uninstall: true, install: true) {
    	section(title: "Name for Smart Mode Handler") {
        	label title: "Name thisHandler", required: true, defaultValue: "Smart Mode"
        }
        
        section(title: "Select Thermostat(s)") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled as requested. Turn back on to activate handler."}
        	else {input ("thermostats", "capability.Thermostat", title: "Pick Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)}            
		}

// input(name: "modeOrRoutine", title: "Use Mode Change, Routine Execution or Ecobee Program: ", type: "enum", 
// required: true, multiple: false, description: "Tap to choose...", metadata:[values:["Mode", "Routine", "Ecobee Program"]], submitOnChange: true)
        if (!settings.tempDisable) {
			section(title: "Select Thermometer") {
				input(name: 'tempSource', title: 'Monitor this temperature source', type: 'enum', required: true, multiple: false, description: 'Tap to choose...', metadata:[values:['Location', 'Temperature Device', "Thermostat's Weather", 'WU Weather Station']], submitOnChange: true)
				if (tempSource) {
					if (tempSource == 'Location') {
						input(name: 'locFreq', type: 'enum', title: 'Temperature check frequency (seconds)', required: true, description: 'Tap to choose...', metadata:[values:[5,10,15,20,30,60]]
					} else if (tempSource == 'Temperature Device') {
						input(name: 'thermometer', type: 'capability.temperatureMeasurement', required: true, multiple: false)
					} else if (tempSource == "Thermostat's Weather") {
						if (thermostats.size() > 1) {
							input(name: 'tstatTemp', type: 'enum', description: 'Tap to choose...', required: true, multiple: false, metadata:[values:thermostats])
						}
					} else if (tempSource == "WU Weather Station") {
						input(name: 'stationID', type: 'string', description: 'Enter WU station identifier', required: true)
						input(name: 'pwsFreq', type: 'enum', title: 'Temperature check frequency (seconds)', required: true, description: 'Tap to choose...', metadata:[values:[5,10,15,20,30,60]]
					}
				}
			}
		}
		section(title: 'Mode Thresholds') {
			// need to set min & max
       		input(name: "aboveTemp", title: "When the temperature is at or above", type: 'decimal', description: 'Enter decimal temperature (optional)', required: false, submitOnChange: true)
			if (aboveTemp) {
				input(name: 'aboveMode', title: 'Set thermostat mode to', type: 'enum', description: 'Tap to choose...', required: true, multiple: false, metadata:[values:getThermostatModes()])
			}
            input(name: "belowThemp", title: 'When the temperature is at or below...', type: 'number', description: 'Enter decimal temperature', required: false, submitOnChange: true)
			if (belowTemp) {
				input(name: 'belowMode', title: 'Set thermostat mode to', type: 'enum', description: 'Tap to choose...', required: true, multiple: false, metadata:[values:getThermostatModes()])
			}
			if ((belowTemp && aboveTemp) && (belowTemp != aboveTemp)) {
				input(name: 'betweenMode', title: "Between ${belowTemp} and ${aboveTemp} set thermostat mode to", type: 'enum', description: 'Tap to choose...', required: false, multiple: false, metadata:[values:getThermostatModes()])
			}
		}
        //section(title: "Notifications") {
		//	input(name:"sendPushMessage", type: "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], 
    	//			required: false)
		//		}
        //}
        section(title: "Temporary Disable") {
        	input(name: "tempDisable", title: "Temporarily Disable this Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
		}
        section (getVersionLabel()) {}
    }
}

def installed() {
	initialize()  
}

def updated() {
	unsubscribe()
    initialize()
}

def initialize() {
	if(tempDisable == true) {
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }
	switch( settings.tempSource) {
		case 'Location':
			// setup timed event
			break;
		
		case 'Temperature Device':
			subscribe( settings.thermometer, 'temperature', tempChangeHandler)
			break;
		
		case "Thermostat's Weather":
			def theStat = []
			theStat = settings.thermostats.size() == 1 ? settings.thermostats : [settings.tstatTemp]
			subscribe(theStat, "weatherTemperature", tempChangeHandler)
			break;
		
		case 'WU Weather Station':
			// check pws name is valid
			// setup timed event
			break;
	}
}

def tempChangeHandler(evt) {
	Double newTemp = evt.DoubleValue
	temperatureUpdate( newTemp )
}   
							  
def temperatureUpdate( Double temp ) {
	def desiredMode = null
	if (settings.aboveTemp && (temp >= settings.aboveTemp)) {
		desiredMode = settings.aboveMode
	} else if (settings.belowTemp && (temp <= settings.belowTemp)) {
		desiredMode = settings.belowMode
	} else if (settings.aboveTemp && settings.belowTemp && settings.betweenMode) {
		desiredMode = settings.betweenMode
	}
	if (desiredMode != null) {
		settings.thermostats.each { 
			if (it.currentValue('thermostatMode') != desiredMode) {
				it.setThermostatMode(desiredMode)
				// notify
			}
		}
	}
}

def getThermostatModes() {
	def modes = []
	thermostats.each {
		modes += it.currentValue('supportedThermostatModes') // need to parse this json
	}
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}

// Gotta fix notifications...
private send(msg) {
	if (sendPushMessage != "No") {
		log.debug("sending push message")
		sendPush(msg)
	}
	log.debug msg
}
