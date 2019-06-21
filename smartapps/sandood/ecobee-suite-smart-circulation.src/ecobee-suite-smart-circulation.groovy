/**
 *  ecobee Suite Smart Circulation
 *
 *  Copyright 2017 Barry A. Burke
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
 *	1.4.0  - Renamed parent Ecobee Suite Manager
 *	1.4.01 - Tweaked supportedThermostatModes handling
 *	1.4.02 - Added install warning to description
 *	1.4.03 - Optimizations for multiple simultaneous updates
 *	1.4.04 - Minor tweaks
 *	1.4.05 - Added Quiet Time Helper integration
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Allow Ecobee Suite Thermostats only
 *	1.5.02 - Converted all math to BigDecimal
 *	1.5.03 - Miscellaneous bug fixes
 *	1.5.04 - Added 'circOff' and 'vacaCircOff' reservation handling (applies to quiet time only)
 *	1.6.00 - Release number synchronization
 *	1.6.01 - Fix reservation initialization error
 *	1.6.02 - REALLY fix reservations initialization error
 *	1.6.03 - Really, REALLY fix reservations initialization error
 *	1.6.10 - Converted to parent-based reservations
 *	1.6.11 - Clear reservations when disabled
 *	1.6.12 - Minor optimizations
 *	1.6.13 - Added humidity restrictor
 *	1.6.14 - Fixed resetting fanMinOnTime when minFanOnTime==maxFanOnTime
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *  1.7.01 - nonCached currentValue() for HE
 *  1.7.02 - more nonCached cases for HE
 *	1.7.03 - Fixing private method issue caused by grails
 *	1.7.04 - Fix error message when temps don't converge
 *	1.7.05 - Fix adjustments down (was getting stuck unless delta < 1.0), fix broken mode handler, reservations work, fix 'Vacation'
 *  1.7.06 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.07 - Added option to require ALL or ANY of the Modes/Programs restrictions
 *	1.7.08 - Fixed typos and formatting
 *	1.7.09 - Optimized isST/isHE, added Global Pause, misc optimizations
 *	1.7.10 - More optimizations, auto-update new versions, fixed another typo
 *	1.7.11 - LOG when calcTemps() is Done!
 */
String getVersionNum() { return "1.7.11" }
String getVersionLabel() { return "Ecobee Suite Smart Circulation Helper, version ${getVersionNum()} on ${getHubPlatform()}" }
import groovy.json.*

definition(
	name: 			"ecobee Suite Smart Circulation",
	namespace: 		"sandood",
	author: 		"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 	"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAdjust fan circulation time based on temperature delta between 2 or more rooms.",
	category: 		"Convenience",
	parent: 		"sandood:Ecobee Suite Manager",
	iconUrl: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false,
    pausable: 		true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	boolean ST = isST
	boolean HE = !ST
	
	dynamicPage(name: "mainPage", title: (HE?'<b>':'') + "${getVersionLabel()}" + (HE?'</b>':''), uninstall: true, install: true) {
    	section(title: "") {
			String defaultLabel = "Smart Circulation"
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
        	if(settings?.tempDisable) { paragraph "WARNING: Temporarily Paused - re-enable below." }
            else {
        		input(name: "theThermostat", type:"${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Ecobee Thermostat", required: true, multiple: false, 
                submitOnChange: true)
            }
		}
        
        if (!settings.tempDisable && settings.theThermostat) {
        	section(title: (HE?'<b>':'') + "Select Indoor Temperature Sensors" + (HE?'</b>':'')) {
            	input(name: "theSensors", title: "Use which indoor temperature sensor(s)", type: "capability.temperatureMeasurement", required: true, multiple: true, submitOnChange: true)
				if (HE) paragraph ''
			}
        
       		section(title: (HE?'<b>':'') + "Fan On Time Automation Configuration" + (HE?'</b>':'')) {
        		paragraph("Increase Circulation time (min/hr) when the difference between the maximum and the minimum temperature reading of the above sensors is more than this.")
            	input(name: "deltaTemp", type: "enum", title: "Select temperature delta", required: true, defaultValue: "2.0", multiple:false, options:["1.0", "1.5", "2.0", "2.5", "3.0", "4.0", "5.0", "7.5", "10.0"])
            	paragraph("Minimum Circulation time (min/hr). Includes heating, cooling and fan only minutes.")
            	input(name: "minFanOnTime", type: "number", title: "Set minimum fan on min/hr (0-${settings.maxFanOnTime!=null?settings.maxFanOnTime:55})", required: true, defaultValue: "5", description: "5", 
					  range: "0..${settings.maxFanOnTime!=null?settings.maxFanOnTime:55}", submitOnChange: true)
            	paragraph("Maximum Circulation time (min/hr).")
            	input(name: "maxFanOnTime", type: "number", title: "Set maximum fan on min/hr (${settings.minFanOnTime!=null?settings.minFanOnTime:5}-55)", required: true, defaultValue: "55", description: "55", 
					  range: "${settings.minFanOnTime!=null?settings.minFanOnTime:5}..55", submitOnChange: true)
            	paragraph("Adjust Circulation time (min/hr) by this many minutes each adjustment.")
            	input(name: "fanOnTimeDelta", type: "number", title: "Minutes per adjustment (1-20)", required: true, defaultValue: "5", description: "5", range: "1..20")
            	paragraph("Minimum number of minutes between adjustments.")
            	input(name: "fanAdjustMinutes", type: "number", title: "Time adjustment frequency in minutes (5-60)", required: true, defaultValue: "10", description: "15", range: "5..60")
				if (HE) paragraph ''
        	}
            
            section(title: (HE?'<b>':'') + "Indoors/Outdoors Temperature Delta" + (HE?'</b>':'')) {
            	paragraph("To apply above adjustments based on inside/outside temperature difference, first select an outside temperature source (indoor temperature will be the average of the sensors selected above).")
                input(name: "outdoorSensor", title: "Use which outdoor temperature sensor", type: "capability.temperatureMeasurement", required: false, multiple: false, submitOnChange: true)
                if (settings.outdoorSensor) {
                	paragraph("Select the indoor/outdoor delta temperature range for which you want to apply the above automated adjustments.")
                    input(name: "adjRange", type: "enum", title: "Adjust fan on times only when outside delta is in this range", multiple: false, required: true, 
							options: ["More than 10 degrees warmer", "5 to 10 degrees warmer", "0 to 4.9 degrees warmer", "-4.9 to -0.1 degrees cooler",
                            			"-10 to -5 degrees cooler", "More than 10 degrees cooler"], submitOnChange: true)
                }
				if (HE) paragraph ''
            }
       
        	section(title: (HE?'<b>':'') + "Vacation Hold Override" + (HE?'</b>':'')) {
        		paragraph("The thermostat's Circulation setting is overridden when a Vacation is in effect. If you would like to automate the Circulation time during a Vacation hold, enable this setting.")
            	input(name: "vacationOverride", type: "bool", title: "Override fan during Vacation hold?", defaulValue: false)
				if (HE) paragraph ''
        	}
       
			section(title: (HE?'<b>':'') + "Enable only for specific modes or programs?" + (HE?'</b>':'')) {
				def multiple = false
            	input(name: "theModes", type: "mode", title: "Only when the Location Mode is", multiple: true, required: false, submitOnChange: true)
                input(name: "statModes", type: "enum", title: "Only when the ${settings.theThermostat!=null?settings.theThermostat:'thermostat'}'s Mode is", multiple: true, required: false, submitOnChange: true, options: getThermostatModesList())
            	input(name: "thePrograms", type: "enum", title: "Only when the ${settings.theThermostat!=null?settings.theThermostat:'thermostat'}'s Program is", multiple: true, required: false, subbmitOnChange: true, options: getProgramsList())
				if ((settings.theModes && settings.statModes) || (settings.statModes && settings.thePrograms) || (settings.thePrograms && settings.theModes)) {
					multiple = true
					input(name: 'needAll', type: 'bool', title: 'Require ALL conditions to be met?', required: true, defaultValue: false, submitOnChange: true)
				}
				if (!multiple) {
					paragraph("Circulation time (min/hr) will only be adjusted when the above condition is met.")
				} else {
					paragraph("Circulation time (min/hr) will ${settings.needAll?'only ':''}be adjusted when ${settings.needAll?'ALL':'ANY'} of the above conditions are met.")	 
				}
				if (HE) paragraph ''
        	}
            
            section(title: (HE?'<b>':'') + "Enable only when relative humidity is high?" + (HE?'</b>':'')) {
            	paragraph("Circulation time (min/hr) is adjusted only when the relative humidity is higher than a specified value")
                input(name: "theHumidistat", type: "capability.relativeHumidityMeasurement", title: "Use this humidity sensor (blank to disable)", multiple: false, required: false, submitOnChange: true)
                if (settings.theHumidistat) {
                	input( name: "highHumidity", type: "number", title: "Adjust circulation only when ${settings.theHumidistat.displayName}'s Relative Humidity is higher than:", range: "0..100", required: true)
                }
				if (HE) paragraph ''
            }
            
            section(title: (HE?'<b>':'') + "'Quiet Time' Integration" + (HE?'</b>':'')) {
            	paragraph("You can configure this Helper to integrate with one or more instances of the Ecobee Suite Quiet Time Helper: This helper will stop updating circulation when one or more Quiet Time switch(es) are enabled.")
            	input(name: "quietSwitches", type: "capability.switch", title: "Select Quiet Time control switch(es)", multiple: true, required: false, submitOnChange: true)
                if (settings.quietSwitches) {
                	paragraph("All selected Quiet Time switches must use the same state to turn on Quiet Time.")
                	input(name: "qtOn", type: "enum", title: "Disable circulation when any of these Quiet Switches is:", defaultValue: 'on', required: true, multiple: false, options: ["on","off"])
                }
				if (HE) paragraph ''
            }
		}
        
		section(title: (HE?'<b>':'') + "Temporarily Disable?" + (HE?'</b>':'')) {
        	input(name: "tempDisable", title: "Pause this Helper?", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        
		section (getVersionLabel()) {}
    }
}

// Main functions
def installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
    
    // initialize the min/max trackers...plan to use these to optimize the decrease cycles
    // Disabled 06/2/2019 BAB - too much overhead for something we aren't using.
    //atomicState.maxMax = 0.0
    //atomicState.minMin = 100.0
    //atomicState.maxDelta = 0.0
    //atomicState.minDelta = 100.0    
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
	LOG("${version} Initializing...", 3, "", 'info')
    atomicState.versionLabel = getVersionLabel()
	boolean ST = atomicState.isST
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

	Integer fanOnTime = (ST ? theThermostat.currentValue('fanMinOnTime') : theThermostat.currentValue('fanMinOnTime', true)) as Integer
    Integer currentOnTime = fanOnTime ?: 0
	String currentProgramName = ST ? theThermostat.currentValue("currentProgramName") : theThermostat.currentValue("currentProgramName", true)
    boolean vacationHold = (currentProgramName == "Vacation")
    
	// log.debug "settings ${theModes}, location ${location.mode}, programs ${thePrograms} & ${programsList}, thermostat ${theThermostat.currentValue('currentProgram')}, currentOnTime ${currentOnTime}, quietSwitch ${quietSwitches.displayName}, quietState ${quietState}"
   
	// Allow adjustments if Location Mode or Thermostat Program or Thermostat Mode is currently as configured
    // Also allow if none are configured
    boolean isOK = true
    if (theModes || thePrograms  || statModes) {
		String currentProgram = ST ? theThermostat.currentValue('currentProgram') : theThermostat.currentValue('currentProgram', true)
		String thermostatMode = ST ? theThermostat.currentValue('thermostatMode') : theThermostat.currentValue('thermostatMode', true)
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
		def ncCh = ST ? settings.theHumidistat.currentValue('humidity') : settings.theHumidistat.currentValue('humidity', true)
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
        if (!isOK) LOG("Quiet time active, not adjusting", 3, null, "info")
    }
    atomicState.isOK = isOK
    
    def tid = getDeviceId(theThermostat.deviceNetworkId)
    if (isOK) {	
		if (currentOnTime < settings.minFanOnTime) {
    		if (vacationHold && settings.vacationOverride) {
            	cancelReservation( tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(settings.minFanOnTime)
            	currentOnTime = settings.minFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	} else if (!vacationHold) {
            	cancelReservation( tid, 'circOff')
    			theThermostat.setFanMinOnTime(settings.minFanOnTime)
            	currentOnTime = settings.minFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	}
    	} else if (currentOnTime > settings.maxFanOnTime) {
    		if (vacationHold && settings.vacationOverride) {
            	cancelReservation( tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(settings.maxFanOnTime)
        		currentOnTime = settings.maxFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	} else if (!vacationHold) {
            	cancelReservation( tid, 'circOff')
    			theThermostat.setFanMinOnTime(settings.maxFanOnTime)
        		currentOnTime = settings.maxFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	}
    	} else {
        	atomicState.fanSinceLastAdjustment = true
			deltaHandler()
            currentOnTime = -1
        }
    } else if (atomicState.quietNow) {
    	if (currentOnTime != 0) {
    		if (vacationHold && settings.vacationOverride) {
            	makeReservation(tid, 'vacaCircOff')
        		theThermostat.setVacationFanMinOnTime(0)
        	} else if (!vacationHold) {
                makeReservation(tid, 'circOff')
                theThermostat.setFanMinOnTime(0)
        	}
        }
    }
    if (currentOnTime > -1) {
    	def vaca = vacationHold ? " is in Vacation mode, " : " "    
    	LOG("thermostat ${theThermostat}${vaca}circulation time is now ${currentOnTime} min/hr",2,"",'info')
    }
    LOG("Initialization complete", 4, "", 'trace')
    return true
}

def quietOnHandler(evt) {
	LOG("Quiet Time switch ${evt.device.displayName} turned ${evt.value}", 3, null, 'info')
	if (!atomicState.quietNow) {
    	atomicState.quietNow = true
		Integer fanOnTime = (atomicState.isST ? theThermostat.currentValue('fanMinOnTime') : theThermostat.currentValue('fanMinOnTime', true)) as Integer
        Integer currentOnTime = fanOnTime?: 0	
        atomicState.quietOnTime = currentOnTime
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
    
    boolean ST = atomicState.isST
	
	// Allow adjustments if Location Mode and/or Thermostat Program and/or Thermostat Mode is currently as configured
    // Also allow if none are configured
    boolean isOK = true
    if (theModes || thePrograms  || statModes) {
		String currentProgram = ST ? theThermostat.currentValue('currentProgram') : theThermostat.currentValue('currentProgram', true)
		String thermostatMode = ST ? theThermostat.currentValue('thermostatMode') : theThermostat.currentValue('thermostatMode', true)
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
		def currentHumidity = ST ? settings.theHumidistat.currentValue('humidity') : settings.theHumidistat.currentValue('humidity', true)
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
    	LOG("deltaHandler() - Temporarily disabled as per request.", 2, null, "warn")
    	return true
    }

    // Make sure it is OK for us to be changing circulation times
	if (!atomicState.isOK) return
    // boolean ST = atomicState.isST
	
	// String currentProgramName = ST ? theThermostat.currentValue('currentProgramName') : theThermostat.currentValue('currentProgramName', true)
	String currentProgramName = theThermostat.currentValue('currentProgramName')
    boolean vacationHold = (currentProgramName && (currentProgramName == 'Vacation'))
	if (vacationHold && !settings.vacationOverride) {
    	LOG("deltaHandler() - ${theThermostat} is in Vacation mode, but not configured to override Vacation fanMinOnTime, returning", 3, "", 'warn')
        return
    }
    def tid = getDeviceId(theThermostat.deviceNetworkId)
	
	// Integer fanMinOnTime = (ST ? theThermostat.currentValue('fanMinOnTime') : theThermostat.currentValue('fanMinOnTime', true)) as Integer
	Integer fanMinOnTime = theThermostat.currentValue('fanMinOnTime') as Integer
	
    if (!vacationHold) {
        if ((fanMinOnTime == 0) && anyReservations(tid, 'circOff')) {
            // Looks like somebody else has turned off circulation
            if (!haveReservation(tid, 'circOff')) {		// is it me?
                // Not me, so we can't be changing circulation
				return
            }
        }
    } else {
    	if ((fanMinOnTime == 0) && anyReservations(tid, 'vacaCircOff')) {
            // Looks like somebody else has turned off circulation
            if (!haveReservation(tid, 'vacaCircOff')) {		// is it me?
                // Not me, so we can't be changing circulation
				return
            }
        }
    }

	if (evt) {
    	if (evt.name == 'currentProgram') {
        	LOG("deltaHandler() - thermostat Program changed to my Program (${evt.value})",3,null,'info')
        } else if (evt.name == 'mode') {
        	LOG("deltaHandler() - location Mode changed to my Mode (${evt.value})",3,null,'info')
        } else {
        	LOG("deltaHandler() - called with ${evt.device} ${evt.name} ${evt.value}",3,null,'trace')
        }
        if (settings.minFanOnTime == settings.maxFanOnTime) {
        	if (fanMinOnTime == settings.minFanOnTime.toInteger()) {
    			LOG('deltaHandler() - configured min==max==fanMinOnTime, nothing to do, skipping...',2,null,'info')
        		return // nothing to do
            } else {
                LOG("deltaHandler() - configured min==max, setting fanMinOnTime(${settings.minFanOnTime})",2,null,'info')
                if (vacationHold && settings.vacationOverride) {
                	cancelReservation( tid, 'vacaCircOff')
        			theThermostat.setVacationFanMinOnTime(settings.minFanOnTime)
        		} else if (!vacationHold) {
                	cancelReservation( tid, 'circOff')
    				theThermostat.setFanMinOnTime(settings.minFanOnTime)
        		}
                return
            }
    	}
    } else {
    	LOG("deltaHandler() - called directly", 4, null, 'trace')
    }
    LOG("deltaHandler() - scheduling calcTemps() in 5 seconds", 3, null, 'info')
	runIn(5,'calcTemps',[overwrite: true])
}

def calcTemps() {
	LOG('Calculating temperatures...', 3, null, 'info')
    boolean ST = atomicState.isST
	
    // Makes no sense to change fanMinOnTime while heating or cooling is running - take action ONLY on events while idle or fan is running
    // def statState = ST ? theThermostat.currentValue("thermostatOperatingState") : theThermostat.currentValue("thermostatOperatingState", true)
	def statState = theThermostat.currentValue("thermostatOperatingState")
    if (statState && (statState.contains('ea') || statState.contains('oo'))) {
    	LOG("calcTemps() - ${theThermostat} is ${statState}, no adjustments made", 4, "", 'info' )
        return
    }
    
  	// Check if it has been long enough since the last change to make another...
    if (atomicState.lastAdjustmentTime) {
        def minutesLeft = fanAdjustMinutes - ((now() - atomicState.lastAdjustmentTime) / 60000).toInteger()
        if (minutesLeft >0) {
            LOG("calcTemps() - Not time to adjust yet - ${minutesLeft} minutes left",4,'','info')
            return
		}
	}
    // atomicState.lastCheckTime = now() 
    // parse temps - ecobee sensors can return "unknown", others may return
    def temps = []
    def total = 0.0G
    def i=0
    theSensors.each {
    	// def temp = ST ? it.currentValue("temperature") : it.currentValue("temperature", true)
		def temp = it.currentValue("temperature")
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
	    LOG("calcTemps() - Current temperature readings: ${temps}, average is ${String.format("%.2f",avg)}°", 4, "", 'info')
    } else {
    	LOG("calcTemps() - Only recieved ${temps.size()} valid temperature readings, skipping...",3,"",'warn')
        return 
    }
    // Skip if the in/out delta doesn't match our settings
    if (outdoorSensor) {
    	def outTemp = null
        if (outdoorSensor.id == theThermostat.id) {
        	// outTemp = ST ? theThermostat.currentValue("weatherTemperature") : theThermostat.currentValue("weatherTemperature", true)
			outTemp = roundIt(theThermostat.currentValue("weatherTemperature"), 2)
            LOG("calcTemps() - Using ${theThermostat.displayName}'s weatherTemperature (${outTemp}°)",4,null,"info")
        } else {
        	outTemp = roundIt(outdoorSensor.currentValue("temperature"), 2)
            LOG("calcTemps() - Using ${outdoorSensor.displayName}'s temperature (${outTemp}°)",4,null,"info")
        }
        def inoutDelta = null
        if (outTemp != null) {
        	inoutDelta = roundIt((outTemp - avg), 2)
        }
        if (inoutDelta == null) {
        	LOG("calcTemps() - Invalid outdoor temperature, skipping...",1,"","warn")
        	return
        }
        LOG("calcTemps() - Outside temperature is currently ${outTemp}°, inside temperature average is ${avg}°",4,null,'info')
        // More than 10 degrees warmer", "5 to 10 degrees warmer", "0 to 4.9 degrees warmer", "-4.9 to -0.1 degrees cooler", -10 to -5 degrees cooler", "More than 10 degrees cooler"
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
        	LOG("calcTemps() - In/Out temperature delta (${inoutDelta}°) not in range (${adjRange}), skipping...", 4, null, "info")
            return
        } else {
        	LOG("calcTemps() - In/Out temperature delta (${inoutDelta}°) is in range (${adjRange}), adjusting...", 4, null, "info")
        }
    }
    def min = 	roundIt(temps.min(), 2)
	def max = 	roundIt(temps.max(), 2)
	def delta = roundIt((max - min), 2)
    
    // Disabled 06/21/2019 BAB - too much overhead for something that is never used.
    // atomicState.maxMax = 	atomicState.maxMax   > max   ? roundIt(atomicState.maxMax, 2)   : max 
    // atomicState.minMin = 	atomicState.minMin 	 < min   ? roundIt(atomicState.minMin, 2)   : min
    // atomicState.maxDelta = 	atomicState.maxDelta > delta ? roundIt(atomicState.maxDelta, 2) : delta 
    // atomicState.minDelta = 	atomicState.minDelta < delta ? roundIt(atomicState.minDelta, 2) : delta
    Integer currentOnTime
    if (atomicState.quietOnTime) {
    	// pick up where we left off at the start of Quiet Time
    	currentOnTime = roundIt(atomicState.quietOnTime, 0) as Integer
        atomicState.quietOnTime = null
    } else {
		// Integer fanMinOnTime = (ST ? theThermostat.currentValue('fanMinOnTime') : theThermostat.currentValue('fanMinOnTime', true)) as Integer
		Integer fanMinOnTime = theThermostat.currentValue('fanMinOnTime') as Integer
    	currentOnTime = fanMinOnTime ?: 0	// Ecobee Suite Manager will populate this with Vacation.fanMinOnTime if necessary
	}
    Integer newOnTime = currentOnTime
	String tid = getDeviceId(theThermostat.deviceNetworkId)
    
	if (delta >= (settings.deltaTemp as BigDecimal)) {			// need to increase recirculation (fanMinOnTime)
    	LOG("calcTemps() - Can we increase fanMinOnTime, it is currently ${currentOnTime}/${settings.maxFanOnTime} (delta = ${delta})", 4, null, 'info')
		if (currentOnTime < settings.maxFanOnTime) {
			newOnTime = currentOnTime + settings.fanOnTimeDelta
			if (newOnTime > settings.maxFanOnTime) newOnTime = settings.maxFanOnTime
			if (currentOnTime != newOnTime) {
				LOG("calcTemps() - Temperature delta is ${delta}°/${settings.deltaTemp}°, increasing circulation time for ${theThermostat} to ${newOnTime} min/hr",3,"",'info')
				if (vacationHold) {
					cancelReservation( tid, 'vacaCircOff')
					theThermostat.setVacationFanMinOnTime(newOnTime)
				} else {
					cancelReservation( tid, 'circOff')
					theThermostat.setFanMinOnTime(newOnTime)
				}
				atomicState.fanSinceLastAdjustment = false
				atomicState.lastAdjustmentTime = now()
                LOG("calcTemps() - Done!",3,null,'info')
				return
			} else {
				LOG("calcTemps() - Looks like we're maxed out - cur: ${currentOnTime}, new: ${newOnTime}, max: ${settings.maxFanOnTime}",3,null,'trace')
				return
			}
		} else {
			LOG("calcTemps() - Curr (${currentOnTime}) >= max (${settings.maxFanOnTime}), no adjustment made",3,null,'trace')
			return
		}
	} else {
    	LOG("calcTemps() - Can we decrease fanMinOnTime, it is currently ${currentOnTime} (delta = ${delta})", 4, null, 'info')
		if (currentOnTime > settings.minFanOnTime) {
			def target = settings.deltaTemp as BigDecimal
			if (delta <= target) {			// start adjusting back downwards once we get within 1F or .5556C of the target delta
				newOnTime = currentOnTime - settings.fanOnTimeDelta
				if (newOnTime < settings.minFanOnTime) newOnTime = settings.minFanOnTime
				if (currentOnTime != newOnTime) {
					LOG("calcTemps() - Temperature delta is ${delta}°/${target}°, decreasing circulation time for ${theThermostat} to ${newOnTime} min/hr",3,null,'info')
					if (vacationHold) {
						LOG("calcTemps() - Calling setVacationFanMinOnTime(${newOnTime})",3,null,'info')
						cancelReservation( tid, 'vacaCircOff')
						theThermostat.setVacationFanMinOnTime(newOnTime)
					} else {
						LOG("calcTemps() - Calling setFanMinOnTime(${newOnTime})",3,null,'info')
						cancelReservation( tid, 'circOff')
						theThermostat.setFanMinOnTime(newOnTime)
					}
					atomicState.fanSinceLastAdjustment = false
					atomicState.lastAdjustmentTime = now()
                    LOG("calcTemps() - Done!",3,null,'info')
					return
				} else {
					LOG("calcTemps() - Looks like we are as close as we can be - curr: ${currentOnTime}, new: ${newOnTime}, min: ${settings.minFanOnTime}",3,null,'trace')
					return
				}
			} else {
				LOG("calcTemps() - Looks like we just need to wait - delta: ${delta}, targetDelta: ${target}, curr: ${currentOnTime}",3,null,'trace')
				return
			}
		} else {
			LOG("calcTemps() - Curr (${currentOnTime}) <= min (${settings.minFanOnTime}), no adjustment made",3,null,'trace')
			return
		}
	}
}
// HELPER FUNCTIONS
// Temporary/Global Pause functions
void updateMyLabel() {
	// Add (paused) to the label when paused
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
// Thermostat Programs & Modes
def getProgramsList() {
	def programs = ["Away","Home","Sleep"]
	if (theThermostat) {
    	def pl = theThermostat.currentValue('programsList')
        if (pl) programs = new JsonSlurper().parseText(pl)
    }
    return programs.sort(false)
}
def getThermostatModesList() {
	def statModes = ["off","heat","cool","auto","auxHeatOnly"]
    if (settings.theThermostat) {
    	def tempModes = theThermostat.currentValue('supportedThermostatModes')
        if (tempModes) statModes = tempModes[1..-2].tokenize(", ")
    }
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
	String msg = "${atomicState.appDisplayName} ${message}"
    if (logType == null) logType = 'debug'
    log."${logType}" message
	parent.LOG(msg, level, null, logType, event, displayEvent)
}
// SmartThings/Hubitat Portability Library (SHPL)
// The following 3 calls are available EVERYWHERE, but they incure a high overhead, so best used only in the Metadata definitions
String  getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST()     { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }
boolean getIsHE()     { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }
// The following 3 calls are ONLY for use within the Application runtime  - they will throw an error at compile time if used within metadata
String getHubPlatform() {
	// This MUST be called at least once in the application runtime space
	def pf = getPlatform()
    atomicState?.hubPlatform = pf			// if (atomicState.hubPlatform == 'Hubitat') ... 
											// or if (state.hubPlatform == 'SmartThings')...
    atomicState?.isST = pf.startsWith('S')	// if (atomicState.isST) ...
    atomicState?.isHE = pf.startsWith('H')	// if (atomicState.isHE) ...
    return pf
}
// THese work, but using the atomicState.is** directly is more efficient
boolean getIsSTHub() { return atomicState.isST as boolean}					// if (isSTHub) ...
boolean getIsHEHub() { return atomicState.isHE as boolean}					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"	
}
