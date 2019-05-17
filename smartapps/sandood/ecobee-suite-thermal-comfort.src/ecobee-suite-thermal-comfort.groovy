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
 */
def getVersionNum() { return "1.7.00" }
private def getVersionLabel() { return "Ecobee Suite Thermal Comfort Helper,\nversion ${getVersionNum()} on ${getPlatform()}" }

import groovy.json.*

definition(
	name: "ecobee Suite Thermal Comfort",
	namespace: "sandood",
	author: "Barry A. Burke and Richard Peng",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSets Ecobee Temperature based on relative humidity using PMV.",
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

def mainPage() {
    def unit = getTemperatureScale()
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
	dynamicPage(name: "mainPage", title: "${getVersionLabel()}", uninstall: true, install: true) {
    	section(title: "") {
        	String defaultLabel = "Thermal Comfort"
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
        	if (settings.tempDisable == true) {
            	paragraph "WARNING: Temporarily Paused - re-enable below."
            } else {
        		input ("theThermostat", "${isST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Ecobee Thermostat", 
                	   required: true, multiple: false, submitOnChange: true)
				paragraph ''
				input(name: 'notify', type: 'bool', title: "Notify on Setpoint Adjustments?", required: false, defaultValue: false, submitOnChange: true)
				paragraph isHE ? "A 'HelloHome' notification is always sent to the Location Event log whenever setpoints are adjusted\n" : "A notification is always sent to the Hello Home log whenever setpoints are adjusted\n"
			}
        }
        if (!settings?.tempDisable && settings?.theThermostat) {
            section(title: "Sensors") {
                input(name: 'humidistat', type: 'capability.relativeHumidityMeasurement', title: "Which Relative Humidity Sensor?", 
                	  required: true, multiple: false, submitOnChange: true)
                if (settings?.humidistat) {
                    atomicState.humidity = settings.humidistat.currentHumidity
					paragraph "The current temperature is ${theThermostat.currentTemperature}°${unit} and the relative humidity is ${atomicState.humidity}%"
                }
				paragraph ''
            }
			
			section(title: "Cool Comfort Settings") {
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
			
            section(title: "Heat Comfort Settings") {
                input(name: "heatPmv", title: "PMV in heat mode${settings.heatPmv!=null&&heatConfigured()?' ('+calculateHeatSetpoint()+'°'+unit+')':''}", 
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
			
			section(title: "Enable only for specific thermostat modes and/or programs? (optional)") {
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

			if (settings.notify) {
				if (isST) {
					section("Notifications") {
						input(name: "phone", type: "string", title: "Phone number(s) for SMS, example +15556667777 (separate multiple with ; )", description: "Phone Number(s)", required: false, submitOnChange: true)
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
						paragraph ''
					}
					section("Use SMS to Phone(s) (limit 10 messages per day)") {
						input(name: "phone", type: "string", title: "Phone number(s) for SMS, example +15556667777 (separate multiple with , )", 
							  required: ((settings.notifiers == null) && !settings.speak), submitOnChange: true)
						paragraph ''
					}
					section("Use Speech Device(s)") {
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
        section(title: "Temporarily Disable") {
        	input(name: "tempDisable", title: "Pause this Helper?", type: "bool", required: false, description: "", submitOnChange: true)
		}
    	section (getVersionLabel()) {}
    }
}

void installed() {
	LOG("installed() entered", 3, "", 'trace')
    atomicState.humidity = null
	initialize()
}

void uninstalled() {
}

void updated() {
	LOG("updated() with settings: ${settings}", 3, "", 'trace')
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
	LOG("${getVersionLabel()}\nInitializing...", 2, "", 'info')
	updateMyLabel()
	
	if(settings.tempDisable) {
    	LOG("Temporarily Paused", 2, null, "warn")
    	return true
    }

    subscribe(settings.humidistat, 'humidity', humidityChangeHandler)
    subscribe(settings.theThermostat, 'currentProgram', modeOrProgramHandler)
    // if (thePrograms) subscribe(settings.theThermostat, "currentProgram", modeOrProgramHandler)
    if (statModes) subscribe(settings.theThermostat, "thermostatMode", modeOrProgramHandler)

    def h = settings.humidistat.currentHumidity
    atomicState.humidity = h

    runIn(2, atomicHumidityUpdater, [overwrite: true])
    if ((h != null) && (h >= 0) && (h <= 100)) {
    	LOG("Initialization complete...current humidity is ${h}%",2,null,'info')
        return true
    } else {
    	LOG("Initialization error...invalid humidity: (${h}) - please check settings and retry", 2, null, 'error')
        return false
    }
}

def configured() {
    return ((atomicState.humidity != null) && (settings.theThermostat != null))
}

def coolConfigured() {
    return (configured() &&
            (settings.coolPmv != null && ( settings.coolPmv == 'custom' ? settings.coolPmvCustom != null : true)) &&
            (settings.coolMet != null && ( settings.coolMet == 'custom' ? settings.coolMetCustom != null : true)) &&
            (settings.coolClo != null && ( settings.coolClo == 'custom' ? settings.coolCloCustom != null : true)))
}

def heatConfigured() {
    return (configured() &&
            (settings.heatPmv != null && ( settings.heatPmv == 'custom' ? settings.heatPmvCustom != null : true)) &&
            (settings.heatMet != null && ( settings.heatMet == 'custom' ? settings.heatMetCustom != null : true)) &&
            (settings.heatClo != null && ( settings.heatClo == 'custom' ? settings.heatCloCustom != null : true)))
}

def modeOrProgramHandler(evt) {
    LOG("Program is: ${evt.value}",3,null,'info')
    runIn(2, atomicHumidityUpdater, [overwrite: true])
}

def humidityChangeHandler(evt) {
	if (evt.numberValue != null) {
        atomicState.humidity = evt.numberValue
        runIn(2, atomicHumidityUpdater, [overwrite: true])
    }
}

def atomicHumidityUpdater() {
	humidityUpdate( atomicState.humidity )
}

def humidityUpdate( humidity ) {
	if (humidity?.toString().isNumber()) humidityUpdate(humidity as Integer)
    return
}

def humidityUpdate( Integer humidity ) {
    if (humidity == null) {
    	log("ignoring invalid humidity: ${humidity}%", 2, null, 'warn')
        return
    }

    atomicState.humidity = humidity
    LOG("Humidity is: ${humidity}%",3,null,'info')

    def currentProgram = settings.theThermostat.currentValue('currentProgram')
    def currentMode = settings.theThermostat.currentValue('thermostatMode')
	def andOr = (settings.enable != null) ? settings.enable : 'or'
	if ((settings.thePrograms == null) || (settings.statModes == null)) andOr = 'or'		// if they only provided one of them, ignore 'and'
    boolean isOK = ((settings.thePrograms == null) && (settings.statModes == null)) ? true: false // isOK if both are null
	if (!isOK) {
		if ((settings.thePrograms != null) && (currentProgram != null)) {
			isOK = settings.thePrograms.contains(currentProgram)
		}
		if (isOK && (andOr == 'and')) {
			if ((settings.statModes != null) && (currentMode != null)) {
				isOK = settings.statModes.contains(currentMode)
			} else {
				isOK = false
			}
		} else if (!isOK && (andOr == 'or') && (settings.statModes != null) && (currentMode != null)) {
			isOK = settings.statModes.contains(currentMode)
		}
	}
    if (!isOK) {
		LOG("${settings.theThermostat.displayName}'s current Mode (${currentMode}/${settings.statModes}) ${andOr} Program (${currentProgram}/${settings.thePrograms}) don't match settings, not adjusting setpoints", 3, null, "info")
        return
    }

    def heatSetpoint = settings.theThermostat.currentValue('heatingSetpoint')
    def coolSetpoint = settings.theThermostat.currentValue('coolingSetpoint')
	def curHeat = heatSetpoint
	def curCool = coolSetpoint
    if (settings.heatPmv != null) {
        heatSetpoint = calculateHeatSetpoint()
    }
    if (settings.coolPmv != null) {
        coolSetpoint = calculateCoolSetpoint()
    }
	if ((heatSetpoint != curHeat) || (coolSetpoint != curCool)) {
    	changeSetpoints(currentProgram, heatSetpoint, coolSetpoint)
	} else {
		// Could be outside of the allowed range, or just too small of a difference...
		LOG("The calculated Thermal Comfort setpoint(s) are the same as the current setpoints (${heatSetpoint}/${coolSetpoint})", 3, null, 'info')
	}
}

private def changeSetpoints( program, heatTemp, coolTemp ) {
	def unit = getTemperatureScale()
	def delta = settings.theThermostat.currentValue('heatCoolMinDelta') as BigDecimal
	def fixed
	def ht = heatTemp.toBigDecimal()
	def ct = coolTemp.toBigDecimal()
	log.debug "${ht} - ${ct}"
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
			def lastMode = settings.theThermostat.currentValue('lastOpState')	// what did the thermostat most recently do?
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
	def msg = "Setting ${settings.theThermostat.displayName}'s heatingSetpoint to ${heatTemp}°${unit} ${(fixed=='heat')?'(adjusted) ':''}and coolingSetpoint to " +
			  "${coolTemp}°${unit} ${(fixed=='cool')?'(adjusted) ':''}for the ${program} program, because the relative humidity is now ${atomicState.humidity}%"
	sendMessage( msg )
    theThermostat.setProgramSetpoints( program, heatTemp, coolTemp )
}

private roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP)
}
private roundIt( BigDecimal value, decimals=0) {
    return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP)
}

private def calculatePmv(temp, units, rh, met, clo) {
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

private def calculateHeatSetpoint() {
    def targetPmv = settings.heatPmv=='custom' ? settings.heatPmvCustom : settings.heatPmv as BigDecimal
    def met = settings.heatMet=='custom' ? settings.heatMetCustom : settings.heatMet as BigDecimal
    def clo = settings.heatClo=='custom' ? settings.heatCloCustom : settings.heatClo as BigDecimal

    def units = getTemperatureScale()
	def step = (units == 'C') ? 0.5 : 1.0
    def range = getHeatRange()
    def min = range[0]
    def max = range[1]
    def preferred = max
    def goodSP = preferred
    def pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    def goodPMV = pmv
    while (preferred >= min && pmv >= targetPmv) {
    	goodSP = preferred
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

private def calculateCoolSetpoint() {
    def targetPmv = settings.coolPmv=='custom' ? settings.coolPmvCustom : settings.coolPmv as BigDecimal
    def met = settings.coolMet=='custom' ? settings.coolMetCustom : settings.coolMet as BigDecimal
    def clo = settings.coolClo=='custom' ? settings.coolCloCustom : settings.coolClo as BigDecimal

    def units = getTemperatureScale()
	def step = (units == 'C') ? 0.5 : 1.0
    def range = getCoolRange()
    def min = range[0]
    def max = range[1]
    def preferred = min
    def goodSP = preferred
    def pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    def goodPMV = pmv
    while (preferred <= max && pmv <= targetPmv) {
    	goodSP = preferred
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
    def low = settings.theThermostat.currentValue('heatRangeLow')
    def high = settings.theThermostat.currentValue('heatRangeHigh')
    return [roundIt(low-0.5,0),roundIt(high-0.5,0)]
}

def getCoolRange() {
    def low = settings.theThermostat.currentValue('coolRangeLow')
    def high = settings.theThermostat.currentValue('coolRangeHigh')
    return [roundIt(low-0.5,0),roundIt(high-0.5,0)]
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	def msg = app.label + ': ' + message
	if (logType == null) logType = 'debug'
	parent.LOG(msg, level, null, logType, event, displayEvent)
    log."${logType}" message
}

private def sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${settings.notify}): ${notificationMessage}", 2, null, "trace")
    if (settings.notify) {
        String msg = "${app.label} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (isST) {
			if (settings.phone) { // check that the user did select a phone number
				if ( settings.phone.indexOf(";") > 0){
					def phones = settings.phone.split(";")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i], msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone, msg)						// Only to SMS contact
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
						sendSmsMessage(phones[i], msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone, msg)						// Only to SMS contact
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
	if (isST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
}

private def updateMyLabel() {
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
private String  getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
private Boolean getIsST()     { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
private Boolean getIsHE()     { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
//
// The following 3 calls are ONLY for use within the Device Handler or Application runtime
//  - they will throw an error at compile time if used within metadata, usually complaining that "state" is not defined
//  - getHubPlatform() ***MUST*** be called from the installed() method, then use "state.hubPlatform" elsewhere
//  - "if (state.isST)" is more efficient than "if (isSTHub)"
//
private String getHubPlatform() {
	def pf = getPlatform()
    atomicState?.hubPlatform = pf			// if (atomicState.hubPlatform == 'Hubitat') ... 
											// or if (state.hubPlatform == 'SmartThings')...
    atomicState?.isST = pf.startsWith('S')	// if (atomicState.isST) ...
    atomicState?.isHE = pf.startsWith('H')	// if (atomicState.isHE) ...
    return pf
}
private Boolean getIsSTHub() { return atomicState.isST }					// if (isSTHub) ...
private Boolean getIsHEHub() { return atomicState.isHE }					// if (isHEHub) ...

private def getParentSetting(String settingName) {
	// def ST = (atomicState?.isST != null) ? atomicState?.isST : isST
	//log.debug "isST: ${isST}, isHE: ${isHE}"
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"	
}
//
// **************************************************************************************************************************
