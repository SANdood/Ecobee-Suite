/**
 *  ecobee Smart Circulation
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
 *	0.1.1 -	Initial Release
 *	0.1.2 -	Beta Release
 *	0.1.3 -	Added configurable support for overriding fanMinOnTime during Vacation holds
 *	0.1.4 -	Added ability to specify both modes and programsList for when a handler should/can run
 *	0.1.5 -	Android fix (bad range in min/maxFanOnTime settings)
 *	0.1.6 - Minor logic tweaking - is now Mode *OR* Program
 *	1.0.0 - Final prep for General Release
 *	1.0.1 - Tweaked LOG and setup for consistency
 *	1.0.2 - Better null variable handling
 *	1.0.3a - Updated settings and Disabled handling (fixed file)
 *	1.0.4a - Enabled min/max to be 0 w/related optimizations
 *	1.0.5 - Fixed currentProgram issues
 *	1.0.6 - Fixed tempDisable loophole
 *	1.0.6a-	Minor updates
 *  1.0.7 - More minor updates
 *	1.0.8 - Added execution filter on Thermostat Mode
 *	1.2.0 - Sync version number with new holdHours/holdAction support
 *	1.2.1 - Development Iterations
 *  1.2.2 - Can now set adjustments based on temperature difference between inside (average) and outside
 *	1.2.3 - Protect against LOG type errors
 *	1.2.4 - Allow changes while thermostatMode is 'off' also
 *
 */
def getVersionNum() { return "1.2.4" }
private def getVersionLabel() { return "ecobee Smart Circulation Version ${getVersionNum()}" }
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

definition(
	name: "ecobee Smart Circulation",
	namespace: "smartthings",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "If a larger than configured temperature delta is found between 2 or more sensors, the minimum Fan On minutes per hour (m/hr) will be automatically adjusted.",
	category: "Convenience",
	parent: "smartthings:Ecobee (Connect)",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	dynamicPage(name: "mainPage", title: "Configure Smart Circulation", uninstall: true, install: true) {
    	section(title: "Name for Smart Circulation Handler") {
        	label title: "Name this Smart Circulation Handler", required: true, defaultValue: "Smart Circulation"  
        }
        
        section(title: "Select Thermostat") {
        	if(settings.tempDisable) {paragraph "WARNING: Temporarily Disabled as requested. Turn back on below to activate handler."}
            else {
        		input(name: "theThermostat", type:"capability.Thermostat", title: "Use which Ecobee Thermostat", required: true, multiple: false, 
                submitOnChange: true)
            }
		}
        
        if (!settings.tempDisable) {
        	section(title: "Select Indoor Temperature Sensors") {
            	input(name: "theSensors", title: "Use which indoor temperature sensor(s)", type: "capability.temperatureMeasurement", required: true, multiple: true, submitOnChange: true)
			}
        
       		section(title: "Fan On Time Automation Configuration") {
        		paragraph("Increase Circulation time (min/hr) when the difference between the maximum and the minimum temperature reading of the above sensors is more than this.")
            	input(name: "deltaTemp", type: "enum", title: "Select temperature delta", required: true, defaultValue: "2.0", multiple:false, options:["1.0", "1.5", "2.0", "2.5", "3.0", "4.0", "5.0", "7.5", "10.0"])
            	paragraph("Minimum Circulation time (min/hr). Includes heating, cooling and fan only minutes.")
            	input(name: "minFanOnTime", type: "number", title: "Set minimum fan on min/hr (0-${settings.maxFanOnTime!=null?settings.maxFanOnTime:55})", required: true, defaultValue: "5", description: "5", range: "0..${settings.maxFanOnTime!=null?settings.maxFanOnTime:55}", submitOnChange: true)
            	paragraph("Maximum Circulation time (min/hr).")
            	input(name: "maxFanOnTime", type: "number", title: "Set maximum fan on min/hr (${settings.minFanOnTime!=null?settings.minFanOnTime:5}-55)", required: true, defaultValue: "55", description: "55", range: "${settings.minFanOnTime!=null?settings.minFanOnTime:5}..55", submitOnChange: true)
            	paragraph("Adjust Circulation time (min/hr) by this many minutes each adjustment.")
            	input(name: "fanOnTimeDelta", type: "number", title: "Minutes per adjustment (1-20)", required: true, defaultValue: "5", description: "5", range: "1..20")
            	paragraph("Minimum number of minutes between adjustments.")
            	input(name: "fanAdjustMinutes", type: "number", title: "Time adjustment frequency in minutes (5-60)", required: true, defaultValue: "10", description: "15", range: "5..60")
        	}
            
            section(title: "Indoors/Outdoors Temperature Delta") {
            	paragraph("To apply above adjustments based on inside/outside temperature difference, first select an outside temperature source (indoor temperature will be the average of the sensors selected above).")
                input(name: "outdoorSensor", title: "Use which outdoor temperature sensor", type: "capability.temperatureMeasurement", required: false, multiple: false, submitOnChange: true)
                if (outdoorSensor) {
                	paragraph("Select the indoor/outdoor delta temperature range for which you want to apply the above automated adjustments.")
                    input(name: "adjRange", type: "enum", title: "Adjust fan on times only when outside delta is in this range", multiple: false, required: true, 
							options: ["More than 10 degrees warmer", "5 to 10 degrees warmer", "0 to 4.9 degrees warmer", "-4.9 to -0.1 degrees cooler",
                            			"-10 to -5 degrees cooler", "More than 10 degrees cooler"], submitOnChange: true)
                }
            }
       
        	section(title: "Vacation Hold Override") {
        		paragraph("The thermostat's Circulation setting is overridden when a Vacation is in effect. If you would like to automate the Circulation time during a Vacation hold, enable this setting.")
            	input(name: "vacationOverride", type: "bool", title: "Override fan during Vacation hold?", defaulValue: false)
        	}
       
			section(title: "Enable only for specific modes or programs?") {
        		paragraph("Circulation time (min/hr) is only adjusted while in these modes *OR* programs. The time will remain at the last setting while in other modes. If you want different circulation times for other modes or programs, create multiple Smart Circulation handlers.")
            	input(name: "theModes",type: "mode", title: "Only when the Location Mode is", multiple: true, required: false)
                input(name:"statModes",type: "enum", title: "Only when the ${settings.theThermostat!=null?settings.theThermostat:'thermostat'}'s Mode is", multiple: true, required: false, options: getThermostatModesList())
            	input(name: "thePrograms", type: "enum", title: "Only when the ${settings.theThermostat!=null?settings.theThermostat:'thermostat'}'s Program is", multiple: true, required: false, options: getProgramsList())
        	}
		}
        
		section(title: "Temporarily Disable?") {
        	input(name: "tempDisable", title: "Temporarily Disable Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        
        section (getVersionLabel())
    }
}

// Main functions
def installed() {
	LOG("installed() entered", 4, "", 'trace')
    
    // initialize the min/max trackers...plan to use these to optimize the decrease cycles
    atomicState.maxMax = 0.0
    atomicState.minMin = 100.0
    atomicState.maxDelta = 0.0
    atomicState.minDelta = 100.0    
	initialize()  
}

def updated() {
	LOG("updated() entered", 4, "", 'trace')
	unsubscribe()
    unschedule()
    initialize()
}

def getProgramsList() {
    return theThermostat ? new JsonSlurper().parseText(theThermostat.currentValue('programsList')) : ["Away","Home","Sleep"]
}

def getThermostatModesList() {
	def statModes = ["off","heat","cool","auto","auxHeatOnly"]
    if (settings.theThermostat) {
    	def tempModes = theThermostat.currentValue('supportedThermostatModes')
        if (tempModes) statModes = tempModes.substring(1,tempModes.length()-1).split(', ').collect{it} 
    }
    return statModes
}

def initialize() {
	LOG("${getVersionLabel()}\nInitializing...", 3, "", 'info')
	atomicState.amIRunning = false				// reset in case we get stuck (doesn't matter a lot if we run more than 1 instance, just wastes resources)
    def mode = location.mode
    
	// Now, just exit if we are disabled...
	if(tempDisable == true) {
    	LOG("temporarily disabled as per request.", 2, null, "warn")
    	return true
    }
    
    // Initialize as if we haven't checked in more than fanAdjustMinutes
    atomicState.lastAdjustmentTime = now() // - (60001 * fanAdjustMinutes.toLong()).toLong() // make sure we run on next deltaHandler event    

    subscribe(theThermostat, "thermostatOperatingState", modeOrProgramHandler)		// so we can see when the fan runs
    if (thePrograms) subscribe(theThermostat, "currentProgram", modeOrProgramHandler)
    // subscribe(theThermostat, "thermostatHold", modeOrProgramHandler)
    subscribe(location, "routineExecuted", modeOrProgramHandler)    
    if (theModes) subscribe(location, "mode", modeOrProgramHandler)
    if (statModes) subscribe(theThermostat, "thermostatMode", modeOrProgramHandler)
    
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

    Integer currentOnTime = theThermostat.currentValue('fanMinOnTime').isNumber() ? theThermostat.currentValue('fanMinOnTime').toInteger() : 0	
    boolean vacationHold = (theThermostat.currentValue("currentProgram") == "Vacation")
    
	log.debug "settings ${theModes}, location ${location.mode}, programs ${thePrograms} & ${programsList}, thermostat ${theThermostat.currentValue('currentProgram')}, currentOnTime ${currentOnTime}"
   
	// Allow adjustments if Location Mode or Thermostat Program or Thermostat Mode is currently as configured
    // Also allow if none are configured
    boolean isOK = true
    if (theModes || thePrograms  || statModes) {
    	isOK = (theModes && theModes.contains(location.mode)) ? true : 
        			((thePrograms && thePrograms.contains(theThermostat.currentValue('currentProgram'))) ? true : 
                    	((statModes && statModes.contains(theThermostat.currentValue('thermostatMode'))) ? true : false))
    }
    atomicState.isOK = isOK
    
    if (isOK) {	
		if (currentOnTime < settings.minFanOnTime) {
    		if (vacationHold && vacationOverride) {
        		theThermostat.setVacationFanMinOnTime(settings.minFanOnTime)
            	currentOnTime = settings.minFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	} else if (!vacationHold) {
    			theThermostat.setFanMinOnTime(settings.minFanOnTime)
            	currentOnTime = settings.minFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	}
    	} else if (currentOnTime > settings.maxFanOnTime) {
    		if (vacationHold && vacationOverride) {
        		theThermostat.setVacationFanMinOnTime(settings.maxFanOnTime)
        		currentOnTime = settings.maxFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	} else if (!vacationHold) {
    			theThermostat.setFanMinOnTime(settings.maxFanOnTime)
        		currentOnTime = settings.maxFanOnTime
                atomicState.lastAdjustmentTime = now() 
        	}
    	} else {
        	atomicState.fanSinceLastAdjustment = true
			deltaHandler()
            currentOnTime = -1
        }
    }
    if (currentOnTime > -1) {
    	def vaca = vacationHold ? " is in Vacation mode, " : " "    
    	LOG("thermostat ${theThermostat}${vaca}circulation time is now ${currentOnTime} min/hr",2,"",'info')
    }

    LOG("Initialization complete", 4, "", 'trace')
}

def modeOrProgramHandler(evt=null) {
	// Just exit if we are disabled...
	if(settings.tempDisable == true) {
    	LOG("temporarily disabled as per request.", 2, null, "warn")
    	return true
    }
    
	// Allow adjustments if location.mode OR thermostat.currentProgram match configuration settings
    def isOK = true
    if (theModes || thePrograms  || statModes) {
    	isOK = (theModes && theModes.contains(location.mode)) ? true : 
        			((thePrograms && thePrograms.contains(theThermostat.currentValue('currentProgram'))) ? true : 
                    	((statModes && statModes.contains(theThermostat.currentValue('thermostatMode'))) ? true : false))
    }
    atomicState.isOK = isOK
    
    if (evt && (evt.name == "thermostatOperatingState") && !atomicState.fanSinceLastAdjustment) {
    	if ((evt.value != 'idle') && (!evt.value.contains('ending'))) atomicState.fanSinceLastAdjustment = true // [fan only, heating, cooling] but not [idle, pending heat, pending cool]
    }
	deltaHandler(evt)
}

def deltaHandler(evt=null) {
	// Just exit if we are disabled...
	if(settings.tempDisable == true) {
    	LOG("temporarily disabled as per request.", 2, null, "warn")
        atomicState.amIRunning = false
    	return true
    }
    
	def isOK = atomicState.isOK
    if ((isOK!=null) && (isOK==false)) {
    	if (atomicState.amIRunning) atomicState.amIRunning = false
        return
    }
    
    def currentProgram = theThermostat.currentValue('currentProgram')
    
	def vacationHold = ( currentProgram == 'Vacation')
	if (!vacationOverride && vacationHold) {
    	LOG("${theThermostat} is in Vacation mode, but not configured to override Vacation fanMinOnTime, returning", 3, "", 'warn')
        atomicState.amIRunning = false
        return
    }
    
	if (evt) {
    	if (evt.name == 'currentProgram') {
        	LOG("Thermostat Program changed to my Program (${evt.value})",3,null,'info')
        } else if (evt.name == 'mode') {
        	LOG("Location Mode changed to my Mode (${evt.value})",3,null,'info')
        } else {
        	LOG("Called with ${evt.device} ${evt.name} ${evt.value}",3,null,'trace')
        }
        if (settings.minFanOnTime == settings.maxFanOnTime) {
        	if (theThermostat.currentValue('fanMinOnTime').toInteger() == settings.minFanOnTime.toInteger()) {
    			LOG('Configured min==max==fanMinOnTime, nothing to do, skipping...',2,null,'info')
                atomicState.amIRunning = false
        		return // nothing to do
            } else {
                LOG("Configured min==max, setting fanMinOnTime(${settings.minFanOnTime})",2,null,'info')
                theThermostat.setFanMinOnTime(settings.minFanOnTime)
                atomicState.amIRunning = false
                return
            }
    	}
        // LOG("Entered with event ${evt.name}: ${evt.value}", 4, "", 'trace')
    } else {
    	LOG("Called directly", 4, "", 'trace')
    }

	// reset the amIRunning sequencer if it gets hung for more than an hour
    def howLong = atomicState.lastCheckTime ? (now() - atomicState.lastCheckTime) : 333333
	if (howLong > 300000) atomicState.amIRunning = false
    if (atomicState.amIRunning) {
    	LOG("An instance of ${app.name} is already running (${howLong} ms) , skipping...",3,null,'trace')
        return
    } else {
    	atomicState.amIRunning = true
    }
    atomicState.lastCheckTime = now()
    
    // parse temps - ecobee sensors can return "unknown", others may return
    def temps = []
    Double total = 0.0
    def i=0
    theSensors.each {
    	def temp = it.currentValue("temperature")
    	if (temp.isNumber() && (temp > 0)) {
        	temps += [temp]	// we want to deal with valid inside temperatures only
            total = total + temp.toDouble()
            i = i + 1
        }
    }
    Double avg = 0.0
    if (i > 1) {
    	avg = (total / i.toDouble()).round(2)
	    LOG("Current temperature readings: ${temps}, average is ${String.format("%.2f",avg)}°", 4, "", 'trace')
    } else {
    	LOG("Only recieved ${temps.size()} valid temperature readings, skipping...",3,"",'warn')
    	atomicState.amIRunning = false
        return 
    }
    
    // Skip if the in/out delta doesn't match our settings
    if (outdoorSensor) {
    	def outTemp = null
        if (outdoorSensor.id == theThermostat.id) {
        	outTemp = theThermostat.currentValue("weatherTemperature")
            LOG("Using ${theThermostat.displayName}'s weatherTemperature (${outTemp}°)",4,null,"info")
        } else {
        	outTemp = outdoorSensor.currentValue("temperature")
            LOG("Using ${outdoorSensor.displayName}'s temperature (${outTemp}°)",4,null,"info")
        }
        Double inoutDelta = null
        if (outTemp.isNumber()) {
        	inoutDelta = (outTemp.toDouble() - avg).round(2)
        }
        if (inoutDelta == null) {
        	LOG("Invalid outdoor temperature, skipping...",1,"","warn")
            atomicState.amIRunning = false
        	return
        }
        LOG("Outside temperature is currently ${outTemp}°, inside temperature average is ${String.format("%.2f",avg)}°",4,null,'trace')
        // More than 10 degrees warmer", "5 to 10 degrees warmer", "0 to 4.9 degrees warmer", "-4.9 to -0.1 degrees cooler", -10 to -5 degrees cooler", "More than 10 degrees cooler"
    	def inRange = false
        if (adjRange.endsWith('warmer')) {
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
        	LOG("In/Out temperature delta (${inoutDelta}°) not in range (${adjRange}), skipping...",4,"","trace")
            atomicState.amIRunning = false
            return
        } else {
        	LOG("In/Out temperature delta (${inoutDelta}°) is in range (${adjRange}), adjusting...",4,"","trace")
        }
    }
    
    Double min = temps.min().toDouble().round(2)
	Double max = temps.max().toDouble().round(2)
	Double delta = (max - min).round(2)
    
    atomicState.maxMax = atomicState.maxMax.toDouble() > max ? atomicState.maxMax: max 
    atomicState.minMin = atomicState.minMin.toDouble() < min ? atomicState.minMin: min
    atomicState.maxDelta = atomicState.maxDelta.toDouble() > delta ? atomicState.maxDelta: delta 
    atomicState.minDelta = atomicState.minDelta.toDouble() < delta ? atomicState.minDelta: delta
    
    // Makes no sense to change fanMinOnTime while heating or cooling is running - take action ONLY on events while idle or fan is running
    def statState = theThermostat.currentValue("thermostatOperatingState")
    if ((statState != 'idle') && (statState != 'fan only') && (statState != 'off')) {
    	LOG("${theThermostat} is ${statState}, no adjustments made", 4, "", 'trace' )
        atomicState.amIRunning = false
        return
    }

    if (atomicState.lastAdjustmentTime) {
        def timeNow = now()
        def minutesLeft = fanAdjustMinutes - ((timeNow - atomicState.lastAdjustmentTime) / 60000).toInteger()
        if (minutesLeft >0) {
            LOG("Not time to adjust yet - ${minutesLeft} minutes left",4,'','trace')
            atomicState.amIRunning = false
            return
		}
	}
    
    Integer currentOnTime = theThermostat.currentValue('fanMinOnTime') ? theThermostat.currentValue('fanMinOnTime').toInteger() : 0	// Ecobee (Connect) will populate this with Vacation.fanMinOnTime if necessary
	Integer newOnTime = currentOnTime
	
	if (delta >= deltaTemp.toDouble()) {			// need to increase recirculation (fanMinOnTime)
		newOnTime = currentOnTime + fanOnTimeDelta
		if (newOnTime > settings.maxFanOnTime) {
			newOnTime = settings.maxFanOnTime
		}
		if (currentOnTime != newOnTime) {
			LOG("Temperature delta is ${String.format("%.2f",delta)}°/${String.format("%.2f",deltaTemp.toDouble())}°, increasing circulation time for ${theThermostat} to ${newOnTime} min/hr",3,"",'info')
			if (vacationHold) {
            	theThermostat.setVacationFanMinOnTime(newOnTime)
            } else {
            	LOG("deltaHandler: calling setFanMinOnTime(${newOnTime})",3,null,'info')
            	theThermostat.setFanMinOnTime(newOnTime)
            }
            atomicState.fanSinceLastAdjustment = false
			atomicState.lastAdjustmentTime = now()
            atomicState.amIRunning = false
            return
		}
	} else {
        // Double target = (getTemperatureScale() == "C") ? 0.55 : 1.0
        //atomicState.target = target
        // if (target > deltaTemp.toDouble()) target = (deltaTemp.toDouble() * 0.66667).round(2)	// arbitrary - we have to be less than deltaTemp
    	// if (delta <= target) {			// start adjusting back downwards once we get within 1F or .5556C
			newOnTime = currentOnTime - fanOnTimeDelta
			if (newOnTime < settings.minFanOnTime) {
				newOnTime = settings.minFanOnTime
			}
            if (currentOnTime != newOnTime) {
           		LOG("Temperature delta is ${String.format("%.2f",delta)}°/${String.format("%.2f",deltaTemp.toDouble())}°, decreasing circulation time for ${theThermostat} to ${newOnTime} min/hr",3,"",'info')
				if (vacationHold) {
                	theThermostat.setVacationFanMinOnTime(newOnTime)
                } else {
                	LOG("deltaHandler: calling setFanMinOnTime(${newOnTime})",3,null,'info')
                	theThermostat.setFanMinOnTime(newOnTime)
                }
                atomicState.fanSinceLastAdjustment = false
				atomicState.lastAdjustmentTime = now()
                atomicState.amIRunning = false
                return
            }
		// }
	}
	LOG("No adjustment made",4,"",'trace')
    atomicState.amIRunning = false
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}
