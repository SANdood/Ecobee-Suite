/**
 *  Ecobee Suite Thermal Comfort
 *
 *  Copyright 2018 Barry A. Burke
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
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - Internal optimizations, better type-ing & cosmetic cleanups
 *  1.7.02 - Adjusted for synchronized setpoints/climates udpates
 *	1.7.03 - No adjustements when thermostat is in Vacation Mode
 *	1.7.04 - Stop repeated messages...
 *	1.7.05 - Cleanup arguments passed to setProgramSetpoint()
 *	1.7.06 - Fixed SMS text entry
 *	1.7.07 - Fixing private method issue caused by grails
 *  1.7.08 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.09 - Optimized isST/isHE, formatting, added Global Pause
 *	1.7.10 - Fixed isST/isHE Optimization bugs
 *	1.7.11 - Added multi-humidistat support
 *	1.7.12 - Fixed multi-humidistat initialization error
 *	1.7.13 - Clean up app label in sendMessage()
 *	1.7.14 - Fixed Notifications setup
 *	1.7.15 - Added option to disable local display of log.debug() logs, support Notification devices on ST
 *	1.7.16 - Fixed ST Notifications section, removed HE SMS option, fixed event handlers typo, fixed humidity initialization
 *	1.7.17 - Cleaned up logging, trying to find why it is changing setpoints on wrong program
 */
String getVersionNum() { return "1.7.17" }
String getVersionLabel() { return "Ecobee Suite Thermal Comfort Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

import groovy.json.*

definition(
	name: 			"ecobee Suite Thermal Comfort",
	namespace: 		"sandood",
	author: 		"Barry A. Burke and Richard Peng",
	description: 	"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSets Ecobee Temperature based on relative humidity using PMV.",
	category: 		"Convenience",
	parent: 		"sandood:Ecobee Suite Manager",
	iconUrl: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance:	false,
    pausable: 		true
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
    def unit = getTemperatureScale()
	def ST = isST
	def HE = !ST
	
    def coolPmvOptions = [
			(-1.0): 'Very cool (-1.0)',
			(-0.5): 'Cool (-0.5)',
			0.0: 'Slightly cool (0.0)',
            0.5: 'Comfortable (0.5)',
            0.64: 'Eco (0.64)',
            0.8: 'Slightly warm (0.8)',
            1.9: 'Warm (1.9)',
            'custom': 'Custom'
    ]
    def heatPmvOptions = [
			1.0: 'Very warm (1.0)',
			0.5: 'Warm (0.5)',
			0.0: 'Slightly warm (0.0)',
            (-0.5): 'Comfortable (-0.5)',
            (-0.64): 'Eco (-0.64)',
            (-1.0): 'Slightly cool (-1.0)',
            (-2.3): 'Cool (-2.3)',
            'custom': 'Custom'
    ]
    def metOptions = [
            0.7: 'Sleeping (0.7)',
            0.8: 'Reclining (0.8)',
            1.0: 'Seated, quiet (1.0)',
            1.1: 'Typing (1.1)',
            1.2: 'Standing, relaxed (1.2)',
            1.7: 'Walking about (1.7)',
            1.8: 'Cooking (1.8)',
            2.1: 'Lifting/packing (2.1)',
            2.7: 'House cleaning (2.7)',
            'custom': 'Custom'
    ]
    def cloOptions = [
            0.0: 'Naked (0.0)',
            0.6: 'Typical Summer indoor clothing (0.6)',
            1.0: 'Typical Winter indoor clothing (1.0)',
            2.9: 'Summer lightweight duvet [0.64-2.9] (2.9)',
			5.0: 'Typical Spring/Autumn indoor clothing (5.0)',
            6.8: 'Spring/Autumn weight duvet [4.5-6.8] (6.8)',
            8.7: 'Winter weight duvet [7.7-8.7] (8.7)',
            'custom': 'Custom'
    ]
	dynamicPage(name: "mainPage", title: (HE?'<b>':'') + "${getVersionLabel()}" + (HE?'</b>':''), uninstall: true, install: true) {
    	section(title: "") {
        	String defaultLabel = "Thermal Comfort"
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
        	if (settings.tempDisable == true) {
            	paragraph "WARNING: Temporarily Paused - re-enable below."
            } else {
        		input(name: 'theThermostat', type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Ecobee Thermostat", 
                	  required: true, multiple: false, submitOnChange: true)
			}
			paragraph ''
        }
        if (!settings?.tempDisable && settings?.theThermostat) {
            section(title: (HE?'<b>':'') + "Sensors" + (HE?'</b>':'')) {
				if (settings?.humidistat) {
                	input(name: 'humidistat', type: 'capability.relativeHumidityMeasurement', title: "Which Relative Humidity Sensor?", 
                	  	  required: true, multiple: false, submitOnChange: true)
                    atomicState.humidity = settings.humidistat.currentHumidity
					paragraph "The current temperature at ${theThermostat.displayName} is ${theThermostat.currentTemperature}°${unit} and the relative humidity is ${atomicState.humidity}%"
				} else {
					input(name: 'humidistats', type: 'capability.relativeHumidityMeasurement', title: "Which Relative Humidity Sensors?", 
                		  required: true, multiple: true, submitOnChange: true)
					boolean multiHumid = false
					if (settings.humidistats) {
						if (settings.humidistats.size() == 1) {
							atomicState.humidity = settings.humidistats[0].currentHumidity
						} else {
							multiHumid = true
							input(name: 'multiHumidType', type: 'enum', options: ['average', 'highest', 'lowest'], title: 'Multiple Humidity Sensors, use:',
								  required: true, multiple: false, defaultValue: 'average', submitOnChange: true)
							atomicState.humidity = getMultiHumidistats()
						}
					}
					if (atomicState.humidity != null) paragraph "The current temperature at ${theThermostat.displayName} is ${theThermostat.currentTemperature}°${unit} and the ${multiHumid?(settings.multiHumidType+' '):''}relative humidity reading is ${atomicState.humidity}%" 
				}
				paragraph ''
            }
			
			section(title: (HE?'<b>':'') + "Cool Comfort Settings" + (HE?'</b>':'')) {
       			input(name: "coolPmv", title: "PMV in cool mode${settings.coolPmv!=null&&coolConfigured()?' ('+calculateCoolSetpoint()+'°'+unit+')':''}", 
                	  type: 'enum', options: coolPmvOptions, required: !settings.heatPmv, submitOnChange: true)
                if (settings.coolPmv=='custom') {
                    input(name: "coolPmvCustom", title: "Custom cool mode PMV (decimal)", type: 'decimal', range: "-5..*", required: true, submitOnChange: true )
                }
                input(name: "coolMet", title: "Metabolic rate", type: 'enum', options: metOptions, required: (settings.coolPMV), submitOnChange: true, defaultValue: 1.1 )
                if (settings.coolMet=='custom') {
                    input(name: "coolMetCustom", title: "Custom cool mode Metabolic rate (decimal)", type: 'decimal', range: "0..*", required: true, submitOnChange: true )
                }
                input(name: "coolClo", title: "Clothing level", type: 'enum', options: cloOptions, required: (settings.coolPMV), submitOnChange: true, defaultValue: 0.6 )
                if (settings.coolClo=='custom') {
                    input(name: "coolCloCustom", title: "Custom cool mode Clothing level (decimal)", type: 'decimal', range: "0..*", required: true, submitOnChange: true )
                }
				paragraph ''
			}
			
            section(title: (HE?'<b>':'') + "Heat Comfort Settings" + (HE?'</b>':'')) {
                input(name: "heatPmv", title: "PMV in heat mode${((settings.heatPmv) && heatConfigured()) ? ' ('+calculateHeatSetpoint()+'°'+unit+')' : '(foo!)'}", 
					  type: 'enum', options: heatPmvOptions, required: !settings.coolPmv, submitOnChange: true)
                if (settings.heatPmv=='custom') {
                    input(name: "heatPmvCustom", title: "Custom heat mode PMV (decimal)", type: 'decimal', range: "*..5", required: true, submitOnChange: true )
                }
                input(name: "heatMet", title: "Metabolic rate", type: 'enum', options: metOptions, required: (settings.heatPMV), submitOnChange: true, defaultValue: 1.1 )
                if (settings.heatMet=='custom') {
                    input(name: "heatMetCustom", title: "Custom heat mode Metabolic rate (decimal)", type: 'decimal', range: "0..*", required: true, submitOnChange: true )
                }
                input(name: "heatClo", title: "Clothing level", type: 'enum', options: cloOptions, required: (settings.heatPMV), submitOnChange: true, defaultValue: 1.0 )
                if (settings.heatClo=='custom') {
                    input(name: "heatCloCustom", title: "Custom heat mode Clothing level (decimal)", type: 'decimal', range: "0..*", required: true, submitOnChange: true )
                }
				paragraph ''
            }
			
			section(title: (HE?'<b>':'') + "Enable only for specific thermostat modes and/or programs? (optional)" + (HE?'</b>':'')) {
        		// paragraph("Thermostat Modes will only be changed while ${location.name} is in these SmartThings Modes.")
                input(name: "statModes", type: "enum", title: "When ${settings.theThermostat!=null?settings.theThermostat:'the thermostat'}'s Mode is", 
                	  multiple: true, required: false, options: getThermostatModesList(), submitOnChange: true)
                input(name: "thePrograms", type: "enum", title: "When ${settings.theThermostat!=null?settings.theThermostat:'the thermostat'}'s Program is", 
                	  multiple: true, required: false, options: getProgramsList(), submitOnChange: true)
                if (settings.statModes && settings.thePrograms) {
                	input(name: "enable", type: "enum", title: "Require Both or Either condition?", options: ['and':'Both (AND)', 'or':'Either (OR)'], required: true, 
                    	  defaultValue: 'or', multiple: false)
                }
				paragraph ''
        	}
			section (title: (HE?'<b>':'') + "Notifications" + (HE?'</b>':'')) {
				input(name: 'notify', type: 'bool', title: "Notify on Setpoint Adjustments?", required: false, defaultValue: false, submitOnChange: true)
				paragraph HE ? "A 'Hello Home' notification is always sent to the Location Event log whenever setpoints are adjusted\n" 
							 : "A notification is always sent to the Hello Home log whenever setpoints are adjusted\n"
				if (settings.notify) {
					if (ST) {
                        input(name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, required: true, submitOnChange: true)
                        input(name: "notifiers", type: "capability.notification", title: "", required: ((settings.phone == null) && !settings.speak && !settings.pushNotify && !settings.notifiers),
                              multiple: true, description: "Select notification devices", submitOnChange: true)
                        input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777; +441234567890)", required: false, submitOnChange: true)
                        input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
                        if (settings.speak) {
                            input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
                            input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
                            if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
                        }
					} else {		// HE
                        input(name: "notifiers", type: "capability.notification", title: "", required: ((settings.phone == null) && !settings.speak), multiple: true, 
                              description: "Select notification devices", submitOnChange: true)
                        paragraph ''

                        input(name: "speak", type: "bool", title: "Speak messages?", required: true, defaultValue: false, submitOnChange: true)
                        if (settings.speak) {
                            input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
                            input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
                            input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
                        }
                        paragraph ''
					}
				}
			}
        }
        section(title: (HE?'<b>':'') + "Temporarily Paused" + (HE?'</b>':'')) {
        	input(name: "tempDisable", title: "Pause this Helper?", type: "bool", required: false, description: "", submitOnChange: true)
		}
		section(title: "") {
			input(name: "debugOff", title: "Disable debug logging? ", type: "bool", required: false, defaultValue: false, submitOnChange: true)
		}
    	section (getVersionLabel()) {}
    }
}

void installed() {
	LOG("Installed with settings: ${settings}", 4, null, 'trace')
    atomicState.humidity = null
	initialize()
}
void uninstalled() {
}
void updated() {
	atomicState.version = getVersionLabel()
	LOG("Updated with settings: ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    atomicState.humidity = null
    initialize()
}
def getProgramsList() {
	def programs = ["Away","Home","Sleep"]
	if (theThermostat) {
    	def pl = theThermostat.currentValue('programsList')
        if (pl) programs = new JsonSlurper().parseText(pl)
    }
    return programs.sort(false)
}
def getThermostatModesList() {
    def statModes = ["heat","cool","auto","auxHeatOnly"]
    if (settings.theThermostat) {
        def tempModes = theThermostat.currentValue('supportedThermostatModes')
        if (tempModes) statModes = tempModes[1..-2].tokenize(", ")
    }
    return statModes.sort(false)
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
    getHubPlatform()
	updateMyLabel()
	
	if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
    if (settings.debugOff) log.info "log.debug() logging disabled"

    subscribe(settings.humidistat, 'humidity', humidityChangeHandler)
    subscribe(settings.theThermostat, 'currentProgram', programChangeHandler)
    // if (thePrograms) subscribe(settings.theThermostat, "currentProgram", modeOrProgramHandler)
    if (statModes) subscribe(settings.theThermostat, "thermostatMode", modeChangeHandler)

    //def h = atomicState.isST ? settings.humidistat.currentValue('humidity') : settings.humidistat.currentValue('humidity', true)
    def h = getMultiHumidistats()
    atomicState.humidity = h
	atomicState.because = " because ${app.label} was (re)initialized"
    runIn(2, atomicHumidityUpdater, [overwrite: true])
	
    if ((h != null) && (h >= 0) && (h <= 100)) {
    	LOG("Initialization complete...current humidity is ${h}%",2,null,'info')
    } else {
    	LOG("Initialization error...invalid humidity: (${h}) - please check settings and retry", 2, null, 'error')
    }
	return
}

boolean configured() {
    return ((atomicState.humidity != null)) // && (atomicState.temperature != null))
}

boolean coolConfigured() {
    return (configured() &&
            (settings.coolPmv != null && ( settings.coolPmv == 'custom' ? settings.coolPmvCustom != null : true)) &&
            (settings.coolMet != null && ( settings.coolMet == 'custom' ? settings.coolMetCustom != null : true)) &&
            (settings.coolClo != null && ( settings.coolClo == 'custom' ? settings.coolCloCustom != null : true)))
}

boolean heatConfigured() {
    return (configured() &&
            (settings.heatPmv != null && ( settings.heatPmv == 'custom' ? settings.heatPmvCustom != null : true)) &&
            (settings.heatMet != null && ( settings.heatMet == 'custom' ? settings.heatMetCustom != null : true)) &&
            (settings.heatClo != null && ( settings.heatClo == 'custom' ? settings.heatCloCustom != null : true)))
}

def programChangeHandler(evt) {
    LOG("Thermostat Program is: ${evt.value}",3,null,'info')
    
    // Don't schedule the update if the new thermostat program isn't one that we're supposed to adjust!
    if (!settings.thePrograms || settings.thePrograms?.contains(evt.value)) {
        atomicState.because = " because the thermostat's Program changed to ${evt.value}"
    	runIn(10, atomicHumidityUpdater, [overwrite: true])				// Wait a bit longer for all the setpoints to update after the program change
    }
}

def modeChangeHandler(evt) {
    LOG("Thermostat Mode is: ${evt.value}",3,null,'info')
    
    // Don't schedule the update if the new thermostat mode isn't one that we're supposed to adjust!
    if (!settings.statModes || settings.statModes?.contains(evt.value)) {
		atomicState.because = " because the thermostat's Mode changed to ${evt.value}"
    	runIn(5, atomicHumidityUpdater, [overwrite: true])				// Mode changes don't directly impact setpoints, but give it time just in case
    }
}

def humidityChangeHandler(evt) {
	if (evt.numberValue != null) {
        // atomicState.humidity = evt.numberValue
		atomicState.humidity = getMultiHumidistats()
		atomicState.because = " because the ${settings.multiHumidistats?(settings.multiHumidistats + ' '):''}humidity changed to ${atomicState.humidity}%"
        runIn(2, atomicHumidityUpdater, [overwrite: true])			// Humidity changes are independent of thermostat settings, no need to wait long
    }
}

void atomicHumidityUpdater() {
	humidityUpdate( atomicState.humidity )
}

void humidityUpdate( humidity ) {
	if (humidity?.toString().isNumber()) humidityUpdate(humidity as Integer)
}

void humidityUpdate( Integer humidity ) {
	if (atomicState.version != getVersionLabel()) {
		LOG("Code version updated, re-initializing...",1,null,'warn')
		updated()
		return			// just ignore the original call, because updated/initalize will call us again
	}
    if (humidity == null) {
    	LOG("ignoring invalid humidity: ${humidity}%", 2, null, 'warn')
        return
    }
	def ST = atomicState.isST
    atomicState.humidity = humidity
    LOG("Humidity is: ${humidity}%",3,null,'info')
	String statHold = ST ? settings.theThermostat.currentValue('thermostatHold') : settings.theThermostat.currentValue('thermostatHold', true)
	if (statHold == 'vacation') {
		LOG("${settings.theThermostat.displayName} is in Vacation Mode, not adjusting setpoints", 3, null, 'warn')
		return
	}
	
    String currentProgram 	= ST ? settings.theThermostat.currentValue('currentProgram') : settings.theThermostat.currentValue('currentProgram', true)
    String currentMode 		= ST ? settings.theThermostat.currentValue('thermostatMode') : settings.theThermostat.currentValue('thermostatMode', true)

	String andOr = (settings.enable != null) ? settings.enable : 'or'
	if ((andOr == 'and') && (!settings.thePrograms || !settings.statModes)) andOr = 'or'		// if they only provided one of them, ignore 'and'
    
    boolean isOK = (!settings.thePrograms && !settings.statModes) ? true : false // isOK if both weren't specified
	if (!isOK) {
		if (settings.thePrograms  && currentProgram) {
			isOK = settings.thePrograms.contains(currentProgram)
		}
		if (isOK && (andOr == 'and')) {
			if (settings.statModes && currentMode) {
				isOK = settings.statModes.contains(currentMode)
			} else {
				isOK = false
			}
		} else if (!isOK && (andOr == 'or') && settings.statModes && currentMode) {
			isOK = settings.statModes.contains(currentMode)
		}
	}
    if (!isOK) {
		LOG("${settings.theThermostat.displayName}'s current Mode (${currentMode}/${settings.statModes}) ${andOr} Program (${currentProgram}/${settings.thePrograms}) don't match settings, not adjusting setpoints", 3, null, "info")
        return
    }

    def heatSetpoint = roundIt(((ST ? settings.theThermostat.currentValue('heatingSetpoint') : settings.theThermostat.currentValue('heatingSetpoint', true)) as BigDecimal), 2)
    def coolSetpoint = roundIt(((ST ? settings.theThermostat.currentValue('coolingSetpoint') : settings.theThermostat.currentValue('coolingSetpoint', true)) as BigDecimal), 2)
	def curHeat = heatSetpoint
	def curCool = coolSetpoint
    if (settings.heatPmv != null) {
        heatSetpoint = roundIt((calculateHeatSetpoint() as BigDecimal), 2)
    }
    if (settings.coolPmv != null) {
        coolSetpoint = roundIt((calculateCoolSetpoint() as BigDecimal), 2)
    }
	if ((heatSetpoint != curHeat) || (coolSetpoint != curCool)) {
		LOG("Before changeSetpoints(${currentProgram}) - Current setpoints (H/C): ${curHeat}/${curCool}, calculated setpoints: ${heatSetpoint}/${coolSetpoint}", 2, null, 'info')
    	changeSetpoints(currentProgram, heatSetpoint, coolSetpoint)
	} else {
		// Could be outside of the allowed range, or just too small of a difference...
		LOG("The calculated Thermal Comfort setpoints (${heatSetpoint}/${coolSetpoint}) are the same as the current setpoints (${curHeat}/${curCool})", 3, null, 'info')
	}
}

void changeSetpoints( program, heatTemp, coolTemp ) {
	def unit = getTemperatureScale()
	boolean ST = atomicState.isST
	
	def delta = ST ? settings.theThermostat.currentValue('heatCoolMinDelta') as BigDecimal : settings.theThermostat.currentValue('heatCoolMinDelta', true) as BigDecimal
	def fixed
	def ht = heatTemp.toBigDecimal()
	def ct = coolTemp.toBigDecimal()
	LOG("${ht} - ${ct}", 3, null, 'debug')
	if ((ct - ht) < delta) {
		fixed = null
		// Uh-oh, the temps are too close together!
		if (settings.heatPmv == null) {
			// We are only adjusting cool, so move heat out of the way
			ht = ct - delta
			fixed = 'heat'
		} else if (settings.coolPmv == null) {
			// We are only adjusting heat, so move cool out of the way
			ct = ht + delta
			fixed = 'cool'
		}
		if (!fixed) {
			if ((settings.statModes != null) && (settings.statModes.size() == 1)) {
				if (settings.statModes.contains('cool')) {
					// we are ONLY adjusting PMV in cool mode, move the heat setpoint
					ht = ct - delta
					fixed = 'heat'
				} else if (settings.statModes.contains('heat')) {
					// we are ONLY adjusting PMV in heat mode, move the cool setpoint
					ct = ht + delta
					fixed = 'cool'
				}
			}
		}
		if (!fixed) {
			// Hmmm...looks like we're adjusting both, and so we don't know which to fix
			def lastMode = ST ? settings.theThermostat.currentValue('lastOpState') : settings.theThermostat.currentValue('lastOpState', true)	// what did the thermostat most recently do?
			if (lastMode != null) {
				if (lastMode.contains('cool')) {
					ht = ct - delta							// move the other one
					fixed = 'heat'
				} else if (lastMode.contains('heat')) {
					ct = ht + delta							
					fixed = 'cool'
				}
			}
		}
		if (!fixed) {
			// Nothing left to try - we're screwed!!!!
			LOG("Unable to adjust PMV, calculated setpoints are too close together (less than ${delta}°${unit}", 1, null, 'warn')
			return
		} else {
			coolTemp = ct
			heatTemp = ht
		}
	}
	def currentProgram 	= ST ? settings.theThermostat.currentValue('currentProgram') : settings.theThermostat.currentValue('currentProgram', true)
    LOG("Changing setpoints for (${program}): ${heatTemp} / ${coolTemp} (currentProgram is ${currentProgram}",2,null,debug)
	theThermostat.setProgramSetpoints( program, heatTemp.toString(), coolTemp.toString() )
	
	// Only send the notification if we are changing the CURRENT program - program could have changed under us...
	if (currentProgram == program) {
		String because = atomicState.because
		String s = settings.theThermostat.displayName.endsWith('s') ? "'" : "'s"
		def msg = "I changed ${settings.theThermostat.displayName}${s} setpoints to Heat: ${heatTemp}°${unit} ${(fixed=='heat')?'(adjusted) ':''}and Cool: " +
			"${coolTemp}°${unit} ${(fixed=='cool')?'(adjusted) ':''}for the ${program} program${because}"
		if (msg != atomicState.lastMsg) sendMessage( msg )	// don't send the same message over and over again (shouldn't be happening anyway)
		atomicState.lastMsg = msg
	}
	atomicState.because = ''
}

def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP)
}
def roundIt( BigDecimal value, decimals=0) {
    return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP)
}

def calculatePmv(temp, units, rh, met, clo) {
    // returns pmv
    // temp, air temperature
    // units, air temperature unit
    // rh, relative humidity (%) Used only this way to input humidity level
    // met, metabolic rate (met)
    // clo, clothing (clo)

    def vel = 0.0

    def ta = ((units == 'C') ? temp : (temp-32)/1.8) as BigDecimal

    def pa, icl, m, fcl, hcf, taa, tcla, p1, p2, p3, p4,
            p5, xn, xf, eps, hcn, hc, tcl, hl1, hl2, hl3, hl4, hl5, hl6,
            ts, pmv, n

    pa = rh * 10 * Math.exp(16.6536 - 4030.183 / (ta + 235))

    icl = 0.155 * clo //thermal insulation of the clothing in M2K/W
    m = met * 58.15 //metabolic rate in W/M2
    if (icl <= 0.078) fcl = 1 + (1.29 * icl)
    else fcl = 1.05 + (0.645 * icl)

    //heat transf. coeff. by forced convection
    hcf = 12.1 * Math.sqrt(vel)
    taa = ta + 273
    tcla = taa + (35.5 - ta) / (3.5 * icl + 0.1)

    p1 = icl * fcl
    p2 = p1 * 3.96
    p3 = p1 * 100
    p4 = p1 * taa
    p5 = 308.7 - 0.028 * m + p2 * Math.pow(taa / 100, 4)
    xn = tcla / 100
    xf = tcla / 50
    eps = 0.00015

    n = 0
    while (Math.abs(xn - xf) > eps) {
        xf = (xf + xn) / 2
        hcn = 2.38 * Math.pow(Math.abs(100.0 * xf - taa), 0.25)
        if (hcf > hcn) hc = hcf
        else hc = hcn
        xn = (p5 + p4 * hc - p2 * Math.pow(xf, 4)) / (100 + p3 * hc)
        ++n
        if (n > 150) {
            return 1
        }
    }

    tcl = 100 * xn - 273

    // heat loss diff. through skin
    hl1 = 3.05 * 0.001 * (5733 - (6.99 * m) - pa)
    // heat loss by sweating
    if (m > 58.15) hl2 = 0.42 * (m - 58.15)
    else hl2 = 0
    // latent respiration heat loss
    hl3 = 1.7 * 0.00001 * m * (5867 - pa)
    // dry respiration heat loss
    hl4 = 0.0014 * m * (34 - ta)
    // heat loss by radiation
    hl5 = 3.96 * fcl * (Math.pow(xn, 4) - Math.pow(taa / 100, 4))
    // heat loss by convection
    hl6 = fcl * hc * (tcl - ta)

    ts = 0.303 * Math.exp(-0.036 * m) + 0.028
    pmv = ts * (m - hl1 - hl2 - hl3 - hl4 - hl5 - hl6)

    return roundIt(pmv, 2)
}

def calculateHeatSetpoint() {
log.debug "in calculateHeatSetpoint"
    def targetPmv = settings.heatPmv=='custom' ? settings.heatPmvCustom : settings.heatPmv as BigDecimal
    def met = 		settings.heatMet=='custom' ? settings.heatMetCustom : settings.heatMet as BigDecimal
    def clo = 		settings.heatClo=='custom' ? settings.heatCloCustom : settings.heatClo as BigDecimal

    def units = getTemperatureScale()
	def step = (units == 'C') ? 0.5 : 1.0
    def range = getHeatRange()
    def min = range[0] as BigDecimal
    def max = range[1] as BigDecimal
    def preferred = max
    def goodSP = preferred as BigDecimal
    def pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    def goodPMV = pmv 
    while (preferred >= min && pmv >= targetPmv) {
    	goodSP = preferred as BigDecimal
        preferred = preferred - step
        goodPMV = pmv
        pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    }
    //preferred = preferred + step
	//if (preferred > max) preferred = max
    //pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    LOG("Heating preferred set point: ${goodSP}°${units} (PMV: ${goodPMV})",3,null,'info')
    return goodSP
}

def calculateCoolSetpoint() {
    def targetPmv = settings.coolPmv=='custom' ? settings.coolPmvCustom : settings.coolPmv as BigDecimal
    def met = 		settings.coolMet=='custom' ? settings.coolMetCustom : settings.coolMet as BigDecimal
    def clo = 		settings.coolClo=='custom' ? settings.coolCloCustom : settings.coolClo as BigDecimal

    def units = getTemperatureScale()
	def step = (units == 'C') ? 0.5 : 1.0
    def range = getCoolRange()
    def min = range[0] as BigDecimal
    def max = range[1] as BigDecimal
    def preferred = min
    def goodSP = preferred as BigDecimal
    def pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    def goodPMV = pmv
    while (preferred <= max && pmv <= targetPmv) {
    	goodSP = preferred as BigDecimal
        preferred = preferred + step
        goodPMV = pmv
        pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    }
    // preferred = preferred - step
	// if (preferred < min) preferred = min
    // pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    LOG("Cooling preferred set point: ${goodSP}°${units} (PMV: ${goodPMV})",3,null,'info')
    return goodSP
}

def getHeatRange() {
	boolean ST = atomicState.isST
    def low  = ST ? settings.theThermostat.currentValue('heatRangeLow')  : settings.theThermostat.currentValue('heatRangeLow', true)
    def high = ST ? settings.theThermostat.currentValue('heatRangeHigh') : settings.theThermostat.currentValue('heatRangeHigh', true)
    return [roundIt(low-0.5,0),roundIt(high-0.5,0)]
}

def getCoolRange() {
	boolean ST = atomicState.isST
    def low  = ST ? settings.theThermostat.currentValue('coolRangeLow')  : settings.theThermostat.currentValue('coolRangeLow', true)
    def high = ST ? settings.theThermostat.currentValue('coolRangeHigh') : settings.theThermostat.currentValue('coolRangeHigh', true)
    return [roundIt(low-0.5,0),roundIt(high-0.5,0)]
}

def getMultiHumidistats() {
	def humidity = atomicState.humidity
	if (!settings.humidistats) {
    	humidity =  atomicState.isST ? settings.humidistat.currentHumidity : settings.humidistat.currentValue('humidity', true)
    } else if (settings.humidistats.size() == 1) {
        humidity = atomicState.isST ? settings.humidistats[0].currentHumidity : settings.humidistats[0].currentValue('humidity', true)
	} else {
		def tempList = atomicState.isST ? settings.humidistats*.currentHumidity : settings.humidistats*.currentValue('humidity', true)
		switch(settings.multiHumidType) {
			case 'average':
				humidity = roundIt( (tempList.sum() / tempList.size()), 0)
				break;
			case 'lowest':
				humidity = tempList.min()
				break;
			case 'highest':
				humidity = tempList.max()
				break;
        }
	}
    return humidity
}
/*					
def getMultiThermometers() {
	if (!settings.thermometers) 			return settings.theThermostat.currentTemperature
	if (settings.thermometers.size() == 1) 	return settings.thermostats[0].currentTemperature
	
	def tempList = settings.thermometers.currentTemperature
	def result
	switch(settings.multiTempType) {
		case 'average':
			return roundIt( (tempList.sum() / tempList.size()), (getTemperatureScale()=='C'?2:1))
			break;
		case 'lowest':
			return tempList.min()
			break;
		case 'highest':
			return tempList.max()
			break;
	}
}
*/

// Helper Functions
void sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${settings.notify}): ${notificationMessage}", 2, null, "trace")
    if (settings.notify) {
        String msg = "${atomicState.appDisplayName} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (atomicState.isST) {
			if (settings.notifiers != null) {
				settings.notifiers.each {								// Use notification devices
					it.deviceNotification(msg)
				}
			}
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
				msg = msg.replaceAll("${unit}",'').replaceAll('°',' degrees') 
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
				msg = msg.replaceAll("${unit}",'').replaceAll('°',' degrees') 
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
	if (atomicState.isST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
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
