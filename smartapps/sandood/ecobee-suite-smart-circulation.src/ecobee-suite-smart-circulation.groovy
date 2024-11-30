/**
 *  ecobee Suite Smart Circulation
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
 *	1.8.02 - More busy bees
 *	1.8.03 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.04 - New SHPL, using Global Fields instead of atomicState
 *	1.8.05 - Allow individual un-pause from peers, even if was already paused
 *	1.8.06 - Enhanced isOkNow() check
 *	1.8.07 - HOTFIX: Tweaked LOGs to be less chatty
 *	1.8.08 - Updated formatting
 *	1.8.09 - Miscellaneous updates & fixes
 *	1.8.10 - Fix for multi-word Climate names
 *	1.8.11 - Fix settings descriptive text for minFanOnTime==0
 *  1.8.12 - Fix getThermostatModes()
 *	1.8.13 - Fix for Hubitat 'supportedThermostatModes', etc.
 *	1.9.00 - Removed all ST code
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.9.00" }
String getVersionLabel() 	{ return "Ecobee Suite Smart Circulation Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Smart Circulation",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAdjust fan circulation time based on temperature delta between 2 or more rooms.",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-circulation.src/ecobee-suite-smart-circulation.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-smart-circ-sa",
	singleInstance: 	false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
	String defaultName = "Smart Circulation"
    
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                paragraph("This goal-seeking Helper tries to converge the ambient temperature of two or more rooms by dynamically adjusting the minimum fan circulation time (minutes per hour).")
            }
        }
		section(title: sectionTitle("Naming${!settings.tempDisable?' & Thermostat and Sensors Selection':''}")) {	
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
            	input(name: "theThermostat", type: 'device.EcobeeSuiteThermostat', title: inputTitle("Select Ecobee Suite Thermostat(s)"), 
					  required: true, multiple: false, submitOnChange: true)
            	if (settings?.theThermostat) {
                	Integer currentOnTime = (theThermostat.currentValue('fanMinOnTime', true))
                	if (maximize) paragraph("The current circulation time for ${theThermostat.displayName} is ${currentOnTime} min/hr")
                }
            }    
			if (!settings.tempDisable && settings.theThermostat) {
            	input(name: "theSensors", title: inputTitle("Select Indoor Temperature Sensor(s)"), type: "capability.temperatureMeasurement", required: true, multiple: true, 
					  submitOnChange: true)
                if (settings?.theSensors) {
                	if (settings.theSensors.size() > 1) {
					    def temps = []
    					def total = 0.0G
    					int i=0
    					settings.theSensors.each {
    						def temp = it.currentValue("temperature", true)
							//def temp = it.currentValue("temperature")
    						if (temp && (temp > 0)) {
								temp = temp as BigDecimal
        						temps += [temp]	// we want to deal with valid inside temperatures only
            					total += temp
            					i++
                            }
                        }
                        def avg = 0.0G
                        if (i > 1) {
                            avg = roundIt((total / i), 2) 
                            if (maximize) paragraph("The current temperature readings for these sensors: ${temps}, average is ${String.format("%.2f",avg)}°${temperatureScale}")
                        } else {
                            paragraph("WARNING: Only 1 of these sensors is reporting valid temperature readings - Smart Circulation requires at least 2 working sensors...")
                        }
                    } else {
                		paragraph("ERROR: Smart Circulation requires at least 2 sensors...")
                    }
                }
			}
		}
        
        if (!settings.tempDisable && settings.theThermostat) {        
       		section(title: sectionTitle("Configuration")) {
            	input(name: "deltaTemp", type: "enum", title: inputTitle("Select Temperature Delta"), required: true, defaultValue: "2.0", multiple:false, 
                	  options:["1.0", "1.5", "2.0", "2.5", "3.0", "4.0", "5.0", "7.5", "10.0"], submitOnChange: true, width: 6)
				if (maximize) paragraph "Circulation time (min/hr) will be increased/decreased when the difference between the maximum and the minimum temperature reading of the above sensors is more/less than the Temperature Delta."
				if (!maximize) paragraph("", width: 6)
            	input(name: "minFanOnTime", type: "number", title: inputTitle("Minimum fan on time")+" (min/hr - 0-${settings.maxFanOnTime!=null?settings.maxFanOnTime:55})", 
                	  required: true, defaultValue: 5, /*description: "5",*/ range: "0..${settings.maxFanOnTime!=null?settings.maxFanOnTime:55}", submitOnChange: true, width: 3)
                // if (settings.minFanOnTime == null) { app.updateSetting('minFanOnTime', 5); settings.minFanOnTime = 5; }
            	input(name: "maxFanOnTime", type: "number", title: inputTitle("Maximum fan on time")+" (min/hr - ${settings.minFanOnTime!=null?settings.minFanOnTime:5}-55)", 
                	  required: true, defaultValue: 55, /* description: "55",*/ range: "${settings.minFanOnTime!=null?settings.minFanOnTime:5}..55", submitOnChange: true, width: 3)
				input(name: "fanAdjustMinutes", type: "number", title: inputTitle("Adjustment frequency")+" (minutes - 5-60)", required: true, defaultValue: 10, 
                	  /*description: "10",*/ range: "5..60", width: 3, submitOnChange: true)
            	input(name: "fanOnTimeDelta", type: "number", title: inputTitle("Adjustment Increments")+" (minutes - 1-20)", required: true, defaultValue: 5, /* description: "5",*/ 
                	  range: "1..20", width: 3, submitOnChange: true)            	
				if (maximize) paragraph "Circulation time includes the Heating, Cooling and Fan Only run time. Adjustments will be made every ${settings?.fanAdjustMinutes?:10} minutes, " +
						  "and the circulation time will change in ${settings?.fanOnTimeDelta?:5} minute increments within the range of ${settings?.minFanOnTime!=null?settings.minFanOnTime:5} to " +
						  "${settings?.maxFanOnTime?:10} minutes"
        	}
            
            section(title: sectionTitle("Conditions")) {
            	if (maximize) paragraph("To adjust Circulation based on inside/outside temperature difference, first select an outside temperature source\n" +
                		  "(the indoor temperature will be the average of the sensors selected above)")
                input(name: "outdoorSensor", title: inputTitle("Select an Outdoor Temperature Sensor")+" (blank to disable)", type: "capability.temperatureMeasurement", required: false, 
                	  multiple: false, submitOnChange: true)
                if (settings.outdoorSensor) {
                    input(name: "adjRange", type: "enum", title: inputTitle("Adjust Circulation when the indoor/outdoor Temperature Difference is:"), multiple: false, required: true, 
							options: ["More than 10 degrees warmer", "5 to 10 degrees warmer", "0 to 4.9 degrees warmer", "-4.9 to -0.1 degrees cooler",
                            			"-10 to -5 degrees cooler", "More than 10 degrees cooler"], submitOnChange: true, width: 8)
                }

            	if (maximize) paragraph("To adjust Circulation based on relative humidity, first select a humidity sensor")
                input(name: "theHumidistat", type: "capability.relativeHumidityMeasurement", title: inputTitle("Select a Humidity Sensor")+" (blank to disable)", multiple: false, 
                	  required: false, submitOnChange: true)
                if (settings.theHumidistat) {
                	input(name: "highHumidity", type: "number", title: inputTitle("Adjust Circulation only when the Relative Humidity is higher than:"), 
                    	  range: "0..100", required: true, width: 10)
                }
				
        		if (maximize) paragraph("To adjust Circulation time during Vacation holds, enable this setting (otherwise Circulation time will be what is configured for the Vacation)")
            	input(name: "vacationOverride", type: "bool", title: inputTitle("Adjust Circulation during Vacation holds?"), 
					  defaultValue: (settings?.thePrograms && settings.thePrograms.contains('Vacation')), submitOnChange: true)
				if (settings?.vacationOverride && settings?.thePrograms && !settings?.thePrograms.contains('Vacation')) {
					def newPrograms = settings.thePrograms + ['Vacation']
					app.updateSetting('thePrograms', newPrograms)
					settings.thePrograms = newPrograms
				}
        	}
       
			section(title: smallerTitle("Modes & Programs")) {
				def multiple = false
				input(name: "theModes", type: "mode", title: inputTitle("Adjust when ${location.name}'s Location Mode is"), multiple: true, required: false, submitOnChange: true, width: 4)
                input(name: "statModes", type: "enum", title: inputTitle("Adjust when the ${settings.theThermostat!=null?settings.theThermostat:'thermostat'}'s Mode is"), 
                	  multiple: true, required: false, submitOnChange: true, options: getThermostatModes(), width: 4)
				def programOptions = getThermostatPrograms() + ['Vacation']
            	input(name: "thePrograms", type: "enum", title: inputTitle("Adjust when the ${settings.theThermostat!=null?settings.theThermostat:'thermostat'}'s Program is"), 
                	  multiple: true, required: false, submitOnChange: true, options: programOptions, width: 4)
				if (settings?.thePrograms?.contains('Vacation')) {
					if (!settings?.vacationOverride) {
						app.updateSetting('vacationOverride', true)
						settings.vacationOverride = true
					}
				}
				boolean any = (settings?.theModes || settings?.statModes || settings?.thePrograms)
				log.debug "${any} and ${settings.thePrograms}"
				if ((settings.theModes && settings.statModes) || (settings.statModes && settings.thePrograms) || (settings.thePrograms && settings.theModes)) {
					multiple = true
					input(name: 'needAll', type: 'bool', title: inputTitle('Require ALL above conditions to be met?'), required: true, defaultValue: false, submitOnChange: true, width: 6)
				}
				if (any) {
					if (!multiple) {
						if (maximize) paragraph("Smart Circulation will only make adjustments when the above condition is met")
					} else {
						if (maximize) paragraph("Smart Circulation will ${settings.needAll?'only ':''}make adjustments when ${settings.needAll?'ALL':'ANY'} of the above conditions are met")	 
					}
				} else {
					if (maximize) paragraph "Smart Circulation will make adjustments independent of Modes or Programs"
				}
        	}
            
            section(title: sectionTitle("'Quiet Time' Integration")) {
            	if (maximize) paragraph("You can configure Smart Circulation to integrate with one or more instances of the Ecobee Suite Quiet Time Helper. This helper will stop adjusting Circulation " +
                		  "when one or more Quiet Time switch(es) are enabled.")
            	input(name: "quietSwitches", type: "capability.switch", title: inputTitle("Select Quiet Time Control Switch(es)"), multiple: true, required: false, submitOnChange: true)
                if (settings.quietSwitches) {
                	paragraph("All selected Quiet Time switches must use the same state to turn on Quiet Time.")
                	input(name: "qtOn", type: "enum", title: inputTitle("Disable adjustments when any of these Quiet Time Control Switches is:"), defaultValue: 'on', required: true, 
                    	  multiple: false, options: ["on","off"], width: 8)
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
		section() {
			paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br><small>Copyright \u00a9 2017-2020 Barry A. Burke - All rights reserved.</small><br>"+
					  "<small><i>Your</i>&nbsp;</small><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=MJQD5NGVHYENY&currency_code=USD&source=url' target='_blank'>" + 
					  "<img src='https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/paypal-green.png' border='0' width='64' alt='PayPal Logo' title='Please consider donating via PayPal!'></a>" +
					  "<small><i>donation is appreciated!</i></small></div>" )
		}
    }
}

// Main functions
def installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
	initialize()  
}
def uninstalled() {
	clearReservations()
}
def updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    initialize()
}
def initialize() {
	String version = getVersionLabel()
	LOG("${version} Initializing...", 2, "", 'info')
    atomicState.versionLabel = getVersionLabel()
    atomicState.hubPlatform = getHubPlatform
    
    def tid = getDeviceId(theThermostat.deviceNetworkId)
    atomicState.theTid = tid
	atomicState.amIRunning = null // Now using runIn to collapse multiple calls into single calcTemps()
    def mode = location.mode
	updateMyLabel()
    
	// Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	clearReservations()
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
    
    // Initialize as if we haven't checked in more than fanAdjustMinutes
    atomicState.lastAdjustmentTime = now() // - (60001 * fanAdjustMinutes.toLong()).toLong() // make sure we run on next deltaHandler event    
    subscribe(theThermostat, "thermostatOperatingState", modeOrProgramHandler)		// so we can see when the fan runs
    if (thePrograms) subscribe(theThermostat, "currentProgram", modeOrProgramHandler)
    // subscribe(theThermostat, "thermostatHold", modeOrProgramHandler)
    // subscribe(location, "routineExecuted", modeOrProgramHandler)    
    if (theModes) subscribe(location, "mode", modeOrProgramHandler)
    if (statModes) subscribe(theThermostat, "thermostatMode", modeOrProgramHandler)
    
    if (settings.quietSwitches) {
    	subscribe(quietSwitches, "switch.${qtOn}", quietOnHandler)
        def qtOff = settings.qtOn == 'on' ? 'off' : 'on'
        subscribe(quietSwitches, "switch.${off}", quietOffHandler)
        atomicState.quietNow = (settings.quietSwitches.currentSwitch.contains(settings.qtOn)) ? true : false
    } else {
    	atomicState.quietNow = false
    }
    
    subscribe(theSensors, "temperature", deltaHandler)
    if (outdoorSensor) {
    	if (outdoorSensor.id == theThermostat.id) {
        	LOG("Using Ecobee-supplied external weatherTemperature from ${theThermostat.displayName}.",1,null,'info')
        	subscribe(theThermostat, "weatherTemperature", deltaHandler)
        } else {
        	LOG("Using external temperature from ${outdoorSensor.displayName}.",1,null,'info')
        	subscribe(outdoorSensor, "temperature", deltaHandler)
        }
    }

	Integer fanOnTime = theThermostat.currentValue('fanMinOnTime', true) as Integer
    Integer currentOnTime = fanOnTime ?: 0
	String currentProgramName = theThermostat.currentValue("currentProgramName", true)
    boolean vacationHold = (currentProgramName == "Vacation")
	if (settings.vacationOverride && settings.thePrograms) {
		if (!settings.thePrograms.contains('Vacation')) {
			def newPrograms = settings.thePrograms + 'Vacation'
			app.updateSetting('thePrograms', newPrograms)
			settings.thePrograms = newPrograms
		}
	}
    
	// log.debug "settings ${theModes}, location ${location.mode}, programs ${thePrograms} & ${programsList}, thermostat ${theThermostat.currentValue('currentProgram')}, currentOnTime ${currentOnTime}, quietSwitch ${quietSwitches.displayName}, quietState ${quietState}"
   
	// Allow adjustments if Location Mode or Thermostat Program or Thermostat Mode is currently as configured
    // Also allow if none are configured
    boolean isOK = true
    if (theModes || thePrograms  || statModes) {
		String currentProgram = theThermostat.currentValue('currentProgram', true)
        if (!currentProgram) currentProgram = 'null'
		String thermostatMode = theThermostat.currentValue('thermostatMode', true)
		if (settings.needAll) {
			isOK = 				settings.theModes ?		settings.theModes.contains(location.mode) 		: true
			if (isOK) isOK = 	settings.thePrograms ?	settings.thePrograms.contains(currentProgram) 	: true
			if (isOK) isOK = 	settings.statModes ?	settings.statModes.contains(thermostatMode)		: true
		} else {
			isOK = 				(theModes && theModes.contains(location.mode))
			if (!isOK) isOK = 	(thePrograms && thePrograms.contains(currentProgram))
			if (!isOK) isOK = 	(statModes && statModes.contains(thermostatMode))
		}
		if (!isOK) LOG("Not in specified Modes ${settings.needAll?'and':'or'} Programs, not adjusting", 3, null, "info")
    }
    
    // Check the humidity?
    if (isOK && settings.theHumidistat) {
		def ncCh = settings.theHumidistat.currentValue('humidity', true)
    	if (ncCh.toInteger() <= settings.highHumidity) {
        	isOK == false
            LOG("Relative Humidity at ${settings.theHumidistat.displayName} is only ${ncCh}% (${settings.highHumidity}% set), not adjusting", 3, null, "info")
		} else {
			LOG("Relative Humidity at ${settings.theHumidistat.displayName} is ${ncCh}% (${settings.highHumidity}% set), adjusting", 3, null, "info")
		}
    }
    
    // Quiet Time?
    if (isOK ){
    	isOK = settings.quietSwitches ? (atomicState.quietNow != true) : true
        atomicState.circMinutes = 'quiet time'
        if (!isOK) LOG("Quiet time active, not adjusting", 3, null, "info")
    }
    atomicState.isOK = isOK
    
    
    if (isOK) {	
		if (currentOnTime < settings.minFanOnTime) {
    		if (vacationHold && settings.vacationOverride) {
            	cancelReservation( tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(settings.minFanOnTime)
            	currentOnTime = settings.minFanOnTime
                atomicState.circMinutes = currentOnTime
                atomicState.lastAdjustmentTime = now() 
        	} else if (!vacationHold) {
            	cancelReservation( tid, 'circOff')
    			theThermostat.setFanMinOnTime(settings.minFanOnTime)
            	currentOnTime = settings.minFanOnTime
                atomicState.circMinutes = currentOnTime
                atomicState.lastAdjustmentTime = now() 
        	}
    	} else if (currentOnTime > settings.maxFanOnTime) {
    		if (vacationHold && settings.vacationOverride) {
            	cancelReservation( tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(settings.maxFanOnTime)
        		currentOnTime = settings.maxFanOnTime
                atomicState.circMinutes = currentOnTime
                atomicState.lastAdjustmentTime = now() 
        	} else if (!vacationHold) {
            	cancelReservation( tid, 'circOff')
    			theThermostat.setFanMinOnTime(settings.maxFanOnTime)
        		currentOnTime = settings.maxFanOnTime
                atomicState.circMinutes = currentOnTime
                atomicState.lastAdjustmentTime = now() 
        	}
    	} else {
        	atomicState.fanSinceLastAdjustment = true
            atomicState.lastAdjustmentTime = 0
			deltaHandler()
            currentOnTime = -1
            atomicState.circMinutes = currentOnTime
        }
    } else if (atomicState.quietNow) {
    	if (currentOnTime != 0) {
    		if (vacationHold && settings.vacationOverride) {
            	makeReservation(tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(0)
                atomicState.circMinutes = 0
        	} else if (!vacationHold) {
                makeReservation(tid, 'circOff')
                theThermostat.setFanMinOnTime(0)
                atomicState.circMinutes = 0
        	}
        }
    }
    if (currentOnTime > -1) {
    	def vaca = vacationHold ? " is in Vacation mode, " : " "    
    	LOG("thermostat ${theThermostat}${vaca}circulation time is now ${currentOnTime} min/hr",2,"",'info')
    }
    updateMyLabel()
    LOG("Initialization complete", 4, "", 'trace')
    return true
}

def quietOnHandler(evt) {
	LOG("Quiet Time switch ${evt.device.displayName} turned ${evt.value}", 3, null, 'info')
	if (!atomicState.quietNow) {
    	atomicState.quietNow = true
		Integer fanOnTime = theThermostat.currentValue('fanMinOnTime', true) as Integer
        Integer currentOnTime = fanOnTime?: 0	
        atomicState.quietOnTime = currentOnTime
        atomicState.circMinutes = 'quiet time'
        clearReservations()
        LOG("Quiet Time enabled, ${app.name} will stop updating circulation time", 3, null, 'info')
        // NOTE: Quiet time will actually pull the circOff reservation and set circulation time to 0
    } else {
    	LOG('Quiet Time already enabled', 3, null, 'info')
    }
}

def quietOffHandler(evt) {
	LOG("Quiet Time switch ${evt.device.displayName} turned ${evt.value}", 3, null, 'info')
    // NOTE: Quiet time will release its circOff reservation and set circulation time to whatever it was
    if (atomicState.quietNow) {
    	if (!settings.quietSwitches.currentSwitch.contains(settings.qtOn)) {
	    	// All the switches are "off"
            atomicState.quietNow = false
            LOG("Quiet Time disabled, ${app.name} will resume circulation time updates", 3, null, 'info')
            modeOrProgramHandler(null)
        } else {
        	def qtOff = settings.qtOn == 'on' ? 'off' : 'on'
        	LOG("All Quiet Time switches are not ${qtOff}, Quiet Time continues", 3, null, 'info')
        }
    } else {
    	LOG("Weird, ${app.name} is not in Quiet Time", 1, null, 'warn')
    }
}

def modeOrProgramHandler(evt=null) {
	// Just exit if we are disabled...
	if(settings.tempDisable == true) {
    	LOG("${app.name} temporarily disabled as per request.", 2, null, "warn")
        clearReservations()
    	return true
    }
    // Just in case we need to re-initialize anything
    def version = getVersionLabel()
    if (atomicState.versionLabel != version) {
    	LOG("Code updated: ${version}",1,null,'debug')
        atomicState.versionLabel = version
        runIn(2, updated, [overwrite: true])
        return
    }
	
	// Allow adjustments if Location Mode and/or Thermostat Program and/or Thermostat Mode is currently as configured
    // Also allow if none are configured
    boolean isOK = true
    if (theModes || thePrograms  || statModes) {
		String currentProgram = theThermostat.currentValue('currentProgram', true)
		String thermostatMode = theThermostat.currentValue('thermostatMode', true)
		if (settings.needAll) {
			isOK = 				settings.theModes ?		settings.theModes.contains(location.mode) 		: true
			if (isOK) isOK = 	settings.thePrograms ?	settings.thePrograms.contains(currentProgram) 	: true
			if (isOK) isOK = 	settings.statModes ?	settings.statModes.contains(thermostatMode)		: true
		} else {
			isOK = 				(theModes && theModes.contains(location.mode))
			if (!isOK) isOK = 	(thePrograms && thePrograms.contains(currentProgram))
			if (!isOK) isOK = 	(statModes && statModes.contains(thermostatMode))
		}
		if (!isOK) {
        	LOG("Not in specified Modes ${settings.needAll?'and':'or'} Programs, not adjusting", 3, null, "info")
            clearReservations()
        }
    }
    
    // Check the humidity?
    if (isOK && settings.theHumidistat) {
		def currentHumidity = settings.theHumidistat.currentValue('humidity', true)
    	if ((currentHumidity as Integer) <= settings.highHumidity) {
        	isOK == false
            LOG("Relative Humidity at ${settings.theHumidistat.displayName} is only ${ncCh}% (${settings.highHumidity}% set), not adjusting", 3, null, "info")
		} else {
			LOG("Relative Humidity at ${settings.theHumidistat.displayName} is ${ncCh}% (${settings.highHumidity}% set), adjusting enabled", 3, null, "info")
		}
    }
    
    // Quiet Time?
    if (isOK ){
    	isOK = settings.quietSwitches ? (atomicState.quietNow != true) : true
        if (!isOK) LOG("Quiet time active, not adjusting", 3, null, "info")
    }
    atomicState.isOK = isOK
    
    if (evt && (evt.name == "thermostatOperatingState") && !atomicState.fanSinceLastAdjustment) {
    	if ((evt.value != 'idle') && (!evt.value.contains('ending'))) atomicState.fanSinceLastAdjustment = true // [fan only, heating, cooling] but not [idle, pending heat, pending cool]
    }
	deltaHandler(evt)
}

def deltaHandler(evt=null) {
	// Just in case we need to re-initialize anything
    def version = getVersionLabel()
    if (atomicState.versionLabel != version) {
    	LOG("Code updated: ${version}",1,null,'debug')
        atomicState.versionLabel = version
        runIn(2, updated, [overwrite: true])
        return
    }
	// Just exit if we are disabled...
	if(settings.tempDisable == true) {
    	LOG("deltaHandler() - Temporarily disabled", 2, null, "warn")
        clearReservations()
    	return true
    }

    // Make sure it is OK for us to be changing circulation times
	if (!atomicState.isOK) return

	String currentProgramName = theThermostat.currentValue('currentProgramName', true)
    boolean vacationHold = (currentProgramName && (currentProgramName == 'Vacation'))
	if (vacationHold && !settings.vacationOverride) {
    	LOG("deltaHandler() - ${theThermostat} is in Vacation mode, but not configured to override Vacation fanMinOnTime, returning", 3, "", 'warn')
        return
    }
    def tid = getDeviceId(theThermostat.deviceNetworkId)
	
	Integer fanMinOnTime = theThermostat.currentValue('fanMinOnTime', true) as Integer
	
    if (!vacationHold) {
        if ((fanMinOnTime == 0) && anyReservations(tid, 'circOff')) {
            // Looks like somebody else has turned off circulation
            if (!haveReservation(tid, 'circOff')) {		// is it me?
                // Not me, so we can't be changing circulation
                LOG("Can't get reservation to 'circOff', exiting",1,null,'warn')
				return
            }
        }
    } else {
    	if ((fanMinOnTime == 0) && anyReservations(tid, 'vacaCircOff')) {
            // Looks like somebody else has turned off circulation
            if (!haveReservation(tid, 'vacaCircOff')) {		// is it me?
                // Not me, so we can't be changing circulation
                LOG("Can't get reservation to 'vacaCircOff', exiting",1,null,'warn')
				return
            }
        }
    }

	if (evt) {
    	if (evt.name == 'currentProgram') {
        	LOG("Thermostat Program changed to an enabled Program (${evt.value})",3,null,'info')
        } else if (evt.name == 'mode') {
        	LOG("Location Mode changed to an enabled Mode (${evt.value})",3,null,'info')
        } else {
        	LOG("${evt.device} ${evt.name} ${evt.value}",3,null,'debug')
        }
        if (settings.minFanOnTime.toInteger() == settings.maxFanOnTime.toInteger()) {
        	if (fanMinOnTime == settings.minFanOnTime.toInteger()) {
    			LOG('Configured min==max==fanMinOnTime, nothing to do, skipping...',2,null,'info')
        		return // nothing to do
            } else {
                LOG("Configured min==max, setting fanMinOnTime(${settings.minFanOnTime})",2,null,'info')
                if (vacationHold && settings.vacationOverride) {
                	cancelReservation( tid, 'vacaCircOff')
        			theThermostat.setVacationFanMinOnTime(settings.minFanOnTime.toInteger())
        		} else if (!vacationHold) {
                	cancelReservation( tid, 'circOff')
    				theThermostat.setFanMinOnTime(settings.minFanOnTime.toInteger())
        		}
                atomicState.circMinutes = settings.minFanOnTime.toInteger()
                updateMyLabel()
                return
            }
    	} else if (fanMinOnTime > settings.maxFanOnTime.toInteger()) {
        	LOG("Current > max, setting fanMinOnTime(${settings.maxFanOnTime})",2,null,'info')
        	if (vacationHold && settings.vacationOverride) {
                cancelReservation( tid, 'vacaCircOff')
                theThermostat.setVacationFanMinOnTime(settings.maxFanOnTime.toInteger())
            } else if (!vacationHold) {
                cancelReservation( tid, 'circOff')
                theThermostat.setFanMinOnTime(settings.maxFanOnTime.toInteger())
            }
            atomicState.circMinutes = settings.maxFanOnTime.toInteger()
            updateMyLabel()
        } else if (fanMinOnTime < settings.minFanOnTime.toInteger()) {
        	LOG("Current < min, setting fanMinOnTime(${settings.minFanOnTime})",2,null,'info')
        	if (vacationHold && settings.vacationOverride) {
                cancelReservation( tid, 'vacaCircOff')
                theThermostat.setVacationFanMinOnTime(settings.minFanOnTime.toInteger())
            } else if (!vacationHold) {
                cancelReservation( tid, 'circOff')
                theThermostat.setFanMinOnTime(settings.minFanOnTime.toInteger())
            }
            atomicState.circMinutes = settings.minFanOnTime.toInteger()
            updateMyLabel()
        }
    } else {
    	LOG("deltaHandler() - called directly", 4, null, 'debug')
    }
    LOG("deltaHandler() - scheduling calcTemps() in 5 seconds", 3, null, 'debug')
	runIn(5,'calcTemps',[overwrite: true])
}

def calcTemps() {
	LOG('Calculating temperatures...', 3, null, 'info')
	
    // Makes no sense to change fanMinOnTime while heating or cooling is running - take action ONLY on events while idle or fan is running
	def statState = theThermostat.currentValue("thermostatOperatingState", true)
    if (statState && (statState.contains('ea') || statState.contains('oo'))) {
    	LOG("${theThermostat} is ${statState}, no adjustments made", 4, "", 'info' )
        return
    }
    
  	// Check if it has been long enough since the last change to make another...
    if (atomicState.lastAdjustmentTime) {
        def minutesLeft = fanAdjustMinutes - ((now() - atomicState.lastAdjustmentTime) / 60000).toInteger()
        if (minutesLeft >0) {
            LOG("Not time to adjust yet - ${minutesLeft} minutes left",4,'','info')
            return
		}
	}

    // parse temps - ecobee sensors can return "unknown", others may return
    def temps = []
    def total = 0.0G
    def i=0
    theSensors.each {
		def temp = it.currentValue("temperature", true)
    	if (temp && (temp > 0)) {
			temp = temp as BigDecimal
        	temps += [temp]	// we want to deal with valid inside temperatures only
            total += temp
            i++
        }
    }
    def avg = 0.0G
    if (i > 1) {
    	avg = roundIt((total / i), 2) 
	    LOG("Current temperature readings: ${temps}, average is ${String.format("%.2f",avg)}°", 2, "", 'info')
    } else {
    	LOG("Only recieved ${temps.size()} valid temperature readings, skipping...",1,"",'warn')
        return 
    }
    // Skip if the in/out delta doesn't match our settings
    if (outdoorSensor) {
    	def outTemp = null
        if (outdoorSensor.id == theThermostat.id) {
			outTemp = roundIt(theThermostat.currentValue("weatherTemperature", true), 2)
            LOG("Using ${theThermostat.displayName}'s weatherTemperature (${outTemp}°)",4,null,"info")
        } else {
        	outTemp = roundIt(outdoorSensor.currentValue("temperature", true), 2)
            LOG("Using ${outdoorSensor.displayName}'s temperature (${outTemp}°)",4,null,"info")
        }
        def inoutDelta = null
        if (outTemp != null) {
        	inoutDelta = roundIt((outTemp - avg), 2)
        }
        if (inoutDelta == null) {
        	LOG("Invalid outdoor temperature, skipping...",1,"","warn")
        	return
        }
        LOG("Outside temperature is currently ${outTemp}°, inside temperature average is ${avg}°",4,null,'info')
        // "More than 10 degrees warmer", "5 to 10 degrees warmer", "0 to 4.9 degrees warmer", "-4.9 to -0.1 degrees cooler", -10 to -5 degrees cooler", "More than 10 degrees cooler"
    	def inRange = false
        if (adjRange.endsWith('mer')) {
        	if (adjRange.startsWith('M')) {
            	if (inoutDelta > 10.0) { inRange = true }
            } else if (adjRange.startsWith('5')) {
            	if ((inoutDelta <= 10.0) && (inoutDelta >= 5.0)) { inRange = true }
            } else { // 0-4.9
            	if ((inoutDelta < 5.0) && (inoutDelta >= 0.0)) { inRange = true }
            }
        } else {
        	if (adjRange.startsWith('M')) {
            	if (inoutDelta < -10.0) { inRange = true }
            } else if (adjRange.startsWith('-1')) {
            	if ((inoutDelta <= -5.0) && (inoutDelta >= -10.0)) { inRange = true }
            } else { // -4.9 -0.1
            	if ((inoutDelta > -5.0) && (inoutDelta < 0.0)) { inRange = true }
            }
        }
        if (!inRange) {
        	LOG("In/Out temperature delta (${inoutDelta}°) not in range (${adjRange}), skipping...", 4, null, "info")
            return
        } else {
        	LOG("In/Out temperature delta (${inoutDelta}°) is in range (${adjRange}), adjusting...", 4, null, "info")
        }
    }
    def min = 	roundIt(temps.min(), 2)
	def max = 	roundIt(temps.max(), 2)
	def delta = roundIt((max - min), 2)
    
    Integer currentOnTime
    if (atomicState.quietOnTime) {
    	// pick up where we left off at the start of Quiet Time
    	currentOnTime = roundIt(atomicState.quietOnTime, 0) as Integer
        atomicState.quietOnTime = null
    } else {
		Integer fanMinOnTime = theThermostat.currentValue('fanMinOnTime', true) as Integer
    	currentOnTime = fanMinOnTime ?: 0	// Ecobee Suite Manager will populate this with Vacation.fanMinOnTime if necessary
	}
    Integer newOnTime = currentOnTime
	String tid = getDeviceId(theThermostat.deviceNetworkId)
    
	if (delta >= (settings.deltaTemp as BigDecimal)) {			// need to increase recirculation (fanMinOnTime)
    	LOG("Can we increase fanMinOnTime, it is currently ${currentOnTime}/${settings.maxFanOnTime} (delta = ${delta})", 4, null, 'info')
		if (currentOnTime < settings.maxFanOnTime) {
			newOnTime = currentOnTime + settings.fanOnTimeDelta
			if (newOnTime > settings.maxFanOnTime) newOnTime = settings.maxFanOnTime
			if (currentOnTime != newOnTime) {
				LOG("Temperature delta is ${delta}°/${settings.deltaTemp}°, increasing circulation time for ${theThermostat} to ${newOnTime} min/hr",3,"",'info')
				if (vacationHold) {
					cancelReservation( tid, 'vacaCircOff')
					theThermostat.setVacationFanMinOnTime(newOnTime)
				} else {
					cancelReservation( tid, 'circOff')
					theThermostat.setFanMinOnTime(newOnTime)
				}
				atomicState.fanSinceLastAdjustment = false
				atomicState.lastAdjustmentTime = now()
                atomicState.circMinutes = newOnTime
                updateMyLabel()
                LOG("Done!",3,null,'info')
				return
			} else {
				LOG("Looks like we're maxed out - cur: ${currentOnTime}, new: ${newOnTime}, max: ${settings.maxFanOnTime}",3,null,'info')
				return
			}
		} else {
			LOG("Current (${currentOnTime}) >= max (${settings.maxFanOnTime}), no adjustment made",3,null,'info')
			return
		}
	} else {
    	LOG("Can we decrease fanMinOnTime, it is currently ${currentOnTime} (delta = ${delta})", 4, null, 'info')
		if (currentOnTime > settings.minFanOnTime) {
			def target = settings.deltaTemp as BigDecimal
			if (delta <= target) {			// start adjusting back downwards once we get within 1F or .5556C of the target delta
				newOnTime = currentOnTime - settings.fanOnTimeDelta
				if (newOnTime < settings.minFanOnTime) newOnTime = settings.minFanOnTime
				if (currentOnTime != newOnTime) {
					LOG("Temperature delta is ${delta}°/${target}°, decreasing circulation time for ${theThermostat} to ${newOnTime} min/hr",3,null,'info')
					if (vacationHold) {
						LOG("Calling setVacationFanMinOnTime(${newOnTime})",3,null,'info')
						cancelReservation( tid, 'vacaCircOff')
						theThermostat.setVacationFanMinOnTime(newOnTime)
					} else {
						LOG("Calling setFanMinOnTime(${newOnTime})",3,null,'info')
						cancelReservation( tid, 'circOff')
						theThermostat.setFanMinOnTime(newOnTime)
					}
					atomicState.fanSinceLastAdjustment = false
					atomicState.lastAdjustmentTime = now()
                    atomicState.circMinutes = newOnTime
                    updateMyLabel()
                    LOG("Done!",3,null,'info')
					return
				} else {
					LOG("Looks like we are as close as we can be - curr: ${currentOnTime}, new: ${newOnTime}, min: ${settings.minFanOnTime}",3,null,'info')
					return
				}
			} else {
				LOG("Looks like we just need to wait - delta: ${delta}, targetDelta: ${target}, curr: ${currentOnTime}",3,null,'info')
				return
			}
		} else {
			LOG("Current (${currentOnTime}) <= min (${settings.minFanOnTime}), no adjustment made",3,null,'info')
			return
		}
	}
}
// HELPER FUNCTIONS
// Temporary/Global Pause functions
void updateMyLabel() {
	
	String flag = '<span '
	
	// Display vent status 
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
    	//log.debug app.label
		myLabel = app.label
		if (!flag || !myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		// strip off any connection status tag
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
    def minutes = (atomicState.circMinutes != -1) ? atomicState.circMinutes : theThermostat.currentValue('fanMinOnTime', true)
    String newLabel
	if (settings.tempDisable) {
		newLabel = myLabel + '<span style="color:red"> (paused)</span>'
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (atomicState.circMinutes == 'quiet time') {
		newLabel = myLabel + '<span style="color:green"> (quiet time)</span>'
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (minutes > -1) { 
    	minutes = ' (min/hr: ' + minutes + ')'
		newLabel = myLabel + '<span style="color:green">' + minutes + '</span>' 
		if (app.label != newLabel) app.updateLabel(newLabel)
	}
    else {
		if (app.label != myLabel) app.updateLabel(myLabel)
	}
    //log.debug "newLabel: " + newLabel + ", myLabel: " + myLabel + ", app.label: " + app.label
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
List getThermostatPrograms() {
	def programs = ["Away","Home","Sleep"]
	if (settings?.theThermostat) {
    	String cl = settings.theThermostat.currentValue('climatesList')
    	if (cl && (cl != '[]')) {
        	programs = cl[1..-2].split(', ')
        } else {
    		String pl = settings?.theThermostat?.currentValue('programsList')
        	def progs = pl ? new JsonSlurper().parseText(pl) : []
            if (progs) programs = progs
        }
    }
    return programs.sort(false)
}
List getThermostatModes() {
	List statModes = ["off","heat","cool","auto","auxHeatOnly"]
	List tm = []
    if (settings.theThermostat) tm = new JsonSlurper().parseText(theThermostat.currentValue('supportedThermostatModes', true))
	if (tm != []) statModes = tm
    return statModes.sort(false)
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
boolean haveReservation(String tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id as String, type )
}
// Do any Apps have reservations?
boolean anyReservations(String tid, String type='modeOff') {
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
def clearReservations() {
	cancelReservation( getDeviceId(theThermostat?.deviceNetworkId), 'circOff' )
    cancelReservation( getDeviceId(theThermostat?.deviceNetworkId), 'vacaCircOff' )
}
// Miscellaneous Helpers
String getDeviceId(networkId) {
    return networkId.split(/\./).last()
}
def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def roundIt( BigDecimal value, decimals=0 ) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
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
