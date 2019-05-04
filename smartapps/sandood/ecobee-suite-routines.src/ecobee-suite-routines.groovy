/**
 *  ecobee Suite Routines
 *
 *  Copyright 2015 Sean Kendall Schneyer
 *	Copyright 2017 Barry A. Burke
 *
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
 * 	1.4.0 - Renamed parent to Ecobee Suite Manager
 * 	1.4.01 - Updated description
 * 	1.4.02 - Fixed fanMode == Circulate handling
 * 	1.4.03 - Renamed display for consistency
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Allow Ecobee Suite Thermostats only
 *	1.5.02 - Added support for notifications via SMS or Push
 *	1.6.00 - Release number synchronization
 *	1.6.01 - Fixed sendMessage()
 *	1.6.10 - Resync for parent-based reservations
 *	1.6.11 - Removed location.contactBook support - deprecated by SmartThings
 *	1.6.12 - Fixed location Mode changing action
 *	1.6.13 - Use 'fanAuto' if fanMinutes is explicitly set to 0
 *	1.7.00 - Universal support for both SmartThings and Hubitat
 */
def getVersionNum() { return "1.7.00d" }
private def getVersionLabel() { return "Ecobee Suite Mode${isST?'/Routine':''}/Program Helper,\nversion ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: "ecobee Suite Routines",
	namespace: "sandood",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nChange Ecobee Programs based on ${isST?'SmartThings Routine execution or':'Hubitat'} Mode changes, OR change Mode/run Routine based on Ecobee Program/Vacation changes",
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
		section(title: "Name for Mode${isST?'/Routine':''}/Program Helper") {						// Hubitat doesn't have "Routines" yet
        	label title: "Name this Helper", required: true, defaultValue: "Mode${isST?'/Routine':''}/Program"
        }
        
        section(title: "Select Thermostats") {
        	if(settings.tempDisable == true) {
            	paragraph "WARNING: Temporarily Disabled as requested. Turn back on below to activate handler."
            } else {
        		input ("myThermostats", "${isST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Pick Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)            
			}
        }
        
        if (!settings.tempDisable) {
			section(title: "Select ST Mode ${isST?'or Routine, ':''}or Ecobee Program") {
        		// Settings option for using Mode or Routine
				input(name: "modeOrRoutine", title: "Use Mode Change${isST?', Routine Execution':''} or Ecobee Program: ", type: "enum", required: true, multiple: false, description: "Tap to choose...", 
					  options:(isST?["Mode", "Routine", "Ecobee Program"]:["Mode","Ecobee Program"]), submitOnChange: true)
			}
        
	        if (settings?.myThermostats?.size() > 0) {
            	if(settings?.modeOrRoutine == "Mode") {
	    	    	// Start defining which Modes(s) to allow the SmartApp to execute in
                    // TODO: Need to run in all modes now and then filter on which modes were selected!!!
    	            //mode(title: "When Hello Mode(s) changes to: ", required: true)
                    section(title: "Modes") {
                    	input(name: "modes", type: "mode", title: "When Hello Mode(s) change to: ", description: "Tap to choose Modes...", required: true, multiple: true)
					}
                } else if (isST && (settings?.modeOrRoutine == "Routine")) {
                	// Routine based inputs
                    def actions = location.helloHome?.getPhrases()*.label
					if (actions) {
            			// sort them alphabetically
            			actions.sort()
						LOG("Actions found: ${actions}", 4)
						// use the actions as the options for an enum input
                        section(title: "Routines") {
							input(name:"action", type:"enum", title: "When these Routines execute: ", description: "Tap to choose Routines...", options: actions, required: true, multiple: true)
                        }
					} // End if (actions)
                } else if (settings.modeOrRoutine == "Ecobee Program") {
                	def programs = getEcobeePrograms()
                    programs = programs + ["Vacation"]
                    LOG("Found the following programs: ${programs}", 4) 
                    section(title: "Programs") {
                    	def n = myThermostats?.size()
                    	if (n > 1) paragraph("NOTE: It is recommended (but not required) to select only one thermostat when using Ecobee Programs to control SmartThings Modes or Routines")
                    	input(name: "ctrlProgram", title: "When Ecobee${n>1?'s':''} change${n>1?'':'s'} to Program: ", type: "enum", description: "Tap to choose Programs...", options: programs, required: true, multiple: true)
                    }
                }

				section(title: "Actions") {
                	if (settings?.modeOrRoutine != "Ecobee Program") {
                		def programs = getEcobeePrograms()
                   		programs += ["Resume Program"]
                		LOG("Found the following programs: ${programs}", 4)
                    
                    	input(name: "cancelVacation", title: "Cancel Vacation hold if active?", type: "bool", required: true, defaultValue: false)
	               		input(name: "whichProgram", title: "Switch to this Ecobee Program:", type: "enum", required: true, multiple:false, description: "Tap to choose...", 
                        	  options: programs, submitOnChange: true)
    	       	    	if (settings?.whichProgram != 'Resume Program') {
                        	if ((settings?.fanMinutes == null) || ((settings.fanMinutes != null) && (settings.fanMinutes != 0))) {
                        		input(name: "fanMode", title: "Select a Fan Mode (optional)", type: "enum", required: false, multiple: false, description: "Tap to choose...", 
                            			options: getThermostatFanModes(), submitOnChange: true, /* defaultValue: (settings?.fanMinutes == null)?:((settings?.fanMinutes == 0)?'Auto':'Circulate') */)
                        	} else if ((settings?.fanMinutes != null) && (settings.fanMinutes == 0)) {
                            	paragraph("Note than the Fan Mode will be set to 'Auto' because you specified 0 Fan Minimum Minutes")
                            }
                        }
                        if (settings.fanMode == 'Auto') {
                        	paragraph("Note that the fan circulation time will also be set to 0 because you selected Fan Mode 'Auto'")
                        } else if (settings.fanMode == 'Off') {
                        	// this can never happen, because 'off' is not a value fanMod
                        	input(name: 'statOff', title: 'Do you want to turn off the HVAC entirely?', type: 'bool', defaultValue: false)
                        } else if ((settings.fanMode == null) || (settings.fanMode == 'Circulate')) {
                        	input(name: "fanMinutes", title: "Specify Fan Minimum Minutes per Hour (optional)", type: "number", 
								  required: false, multiple: false, description: "Tap to choose...", range:"0..55", submitOnChange: true, 
								  defaultValue: (settings.fanMode==null?settings.fanMinutes:20))
							// defaultValue: (settings.fanMode==null?(settings.fanMinutes!=null?settings.fanMinutes:0):20))
                            
                        }
        	       		if (settings.whichProgram != "Resume Program") {
                        	input(name: "holdType", title: "Select the Hold Type (optional)", type: "enum", required: false, 
								  multiple: false, description: "Tap to choose...", submitOnChange: true, defaultValue: "Parent Ecobee (Connect) Setting",
								  options:["Until I Change", "Until Next Program", "2 Hours", "4 Hours", "Specified Hours", "Thermostat Setting", 
                               							"Parent Ecobee (Connect) Setting"])
                        	if (settings.holdType=="Specified Hours") {
            					input(name: 'holdHours', title:'How many hours (1-48)?', type: 'number', range:"1..48", required: true, description: '2', defaultValue: 2)
            				} else if (settings.holdType=='Thermostat Setting') {
            					paragraph("Thermostat Setting at the time of hold request will be applied.")
            				} else if ((settings.holdType == null) || (settings.holdType == 'Parent Ecobee (Connect) Setting')) {
                            	paragraph("Ecobee (Connect) Setting at the time of hold request will be applied.")
                            }
                        }
            	   		// input(name: "useSunriseSunset", title: "Also at Sunrise or Sunset? (optional) ", type: "enum", required: false, multiple: true, description: "Tap to choose...", metadata:[values:["Sunrise", "Sunset"]], submitOnChange: true)                
                	} else {
						input(name: "runModeOrRoutine", title: "Change Mode${isST?' or Execute Routine':'to'}:", type: "enum", required: true, multiple: false, defaultValue: "Mode", description: "Tap to choose...", 
							  options:(isST?["Mode", "Routine"]:["Mode"]), submitOnChange: true)
                        if (settings.runModeOrRoutine == "Mode") {
    	                	input(name: "runMode", type: "mode", title: "Change Hello Mode to: ", required: true, multiple: false)
                		} else if (settings.runModeOrRoutine == "Routine") {
                			// Routine based inputs
                    		def actions = location.helloHome?.getPhrases()*.label
							if (actions) {
            					// sort them alphabetically
            					actions.sort()
								LOG("Actions found: ${actions}", 4)
								// use the actions as the options for an enum input
								input(name:"runAction", type:"enum", title: "Execute this Routine: ", options: actions, required: true, multiple: false)
							} // End if (actions)
                        } // End if (Routine)
                    } // End else Program --> Mode/Routine
                } // End of "Actions" section
                section("Notifications (optional)") {
                    input(name: "notify", type: "boolean", title: "Notify on Actions?", required: true, defaultValue: false, submitOnChange: true)
                    if (settings.notify) {
                        paragraph "You can enter multiple phone numbers seperated by a semi-colon (;)"
                        input "phone", "string", title: "Send SMS notifications to", description: "Phone Number(s)", required: false, submitOnChange: true 
                        if (!settings.phone) {
                            input( name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", /* defaultValue: false, */ required: true, submitOnChange: true)
                        }
                        if (!settings.phone && !settings.pushNotify) paragraph "WARNING: Notifications configured, but nobody to send them to!"
                    }
                    if (isST) paragraph("A notification is always sent to the Hello Home log whenever an action is taken")
        		}// End of Notifications
            } // End if myThermostats size
        }   
        section(title: "Temporarily Disable?") {
           	input(name: "tempDisable", title: "Temporarily Disable Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        
		section (getVersionLabel()) {}
    }
}

// Main functions
def installed() {
	LOG("installed() entered", 2)
	initialize()  
}

def updated() {
	LOG("updated() entered", 2)
	unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
	LOG("${getVersionLabel()}\nInitializing...", 2, "", 'info')
    if(tempDisable == true) {
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }

	if (settings.modeOrRoutine == "Routine") {
    	subscribe(location, "routineExecuted", changeProgramHandler)
    } else if (settings.modeOrRoutine == "Mode") {
    	subscribe(location, "mode", changeProgramHandler)
    } else { // has to be "Ecobee Program"
    	subscribe(myThermostats, "currentProgram", changeSTHandler)
    }
    
//    if(useSunriseSunset?.size() > 0) {
//		// Setup subscriptions for sunrise and/or sunset as well
//        if( useSunriseSunset.contains("Sunrise") ) subscribe(location, "sunrise", changeProgramHandler)
//        if( useSunriseSunset.contains("Sunset") ) subscribe(location, "sunset", changeProgramHandler)
//    }
    
	// Normalize settings data
    normalizeSettings()
    LOG("initialize() exiting")
}

private def normalizeSettings() {
	if (settings.modeOrRoutine == "Ecobee Program") return		// no normalization required
    
	// whichProgram
	state.programParam = null
    state.doResumeProgram = false
    state.doCancelVacation = settings.cancelVacation?true:false
	if (whichProgram != null && whichProgram != "") {
    	if (whichProgram == "Resume Program") {
        	state.doResumeProgram = true
        } else if (whichProgram == 'Cancel Vacation') {
        	state.doCancelVacation = true
            // state.programParam = null
        } else {        	
    		state.programParam = whichProgram
    	}
	}
    
    // fanMode
    state.fanCommand = null
    state.fanMinutes = null
    switch (fanMode) {
        case 'On': 
        	state.fanCommand = 'fanOn'
            if (settings.fanMinutes != null) {
    			state.fanMinutes = settings.fanMinutes.toInteger()
   			}
            break;
        case 'Auto':
        	state.fanCommand = 'fanAuto'
            state.fanMinutes = 0
            break;
        case 'Off': 
            state.fanCommand = 'fanOff'		// to turn off the fan, we need: tstatMode==Off, fanMode==Auto, fanMinOnTime==0
            state.fanMinutes = 0
            break;
    	case 'Circulate':
            state.fanCommand = 'fanCirculate'
            if (settings.fanMinutes == null) {
                state.fanMinutes = 20
            } else if (settings.fanMinutes != null) {
                state.fanMinutes = settings.fanMinutes.toInteger()
            } 
            if (state.fanMinutes == 0) state.fanCommand = 'fanAuto'	// override Circulate with 0 minutes of minFanOnTime
            break;
        default : 
        	state.fanCommand = null		// default
            if (settings.fanMinutes != null) {
    			state.fanMinutes = settings.fanMinutes.toInteger()
                if (settings.fanMode == null) state.fanCommand = (state.fanMinutes == 0) ? 'fanAuto' : 'fanCirculate'	// for backwards compatibility
   			}
   }
    
    // holdType is now calculated at the time of the hold request
    state.holdTypeParam = null
//    if (settings.holdType != null && settings.holdType != "") {
//    	if (holdType == "Until I Change") {
//        	state.holdTypeParam = "indefinite"
//        } else if (holdType == "Until Next Program") {
//        	state.holdTypeParam = "nextTransition"
//        } else {
//        	state.holdTypeParam = null
//        }
//    }
    
	if (settings.modeOrRoutine == "Routine") {
    	state.expectedEvent = settings.action
    } else {
    	state.expectedEvent = settings.modes
    }
	LOG("state.expectedEvent set to ${state.expectedEvent}", 4)
}

def changeSTHandler(evt) {
	LOG("changeSTHandler() entered with evt: ${evt.name}: ${evt.value}", 5)
    if (settings.ctrlProgram.contains(evt.value)) {
    	if (!state.ecobeeThatChanged) {
        	state.ecobeeThatChanged = evt.displayName
            state.ecobeeNewProgram = evt.value
        }
    	if (settings.runModeOrRoutine == "Mode") {
        	LOG("Changing Mode to ${settings.runMode} because ${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}",2,null,'info')
        	if (settings.myThermostats.size() == 1) { 
            	changeMode() 
            } else { 
            	// trick to avoid multiple calls if more than 1 thermostat - they could change simultaneously or within the next pollCycle
                // to avoid this delayed response, assign to only one thermostat
                parent.poll()
            	runIn(5, changeMode, [overwrite: true]) 
            }	
        } else {
        	LOG("Executing Routine ${settings.runAction} because ${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}",2,null,'info')
        	if (settings.myThermostats.size() == 1) { 
            	runRoutine() 
            } else {
            	parent.poll()
            	runIn(15, runRoutine, [overwrite: true]) 
            }
        }
    }
}

def changeMode() {
	if (settings.runMode != location.mode) { 	// only if we aren't already in the specified Mode
        if (state.ecobeeThatChanged) {
        	if (location.modes?.find {it.name == settings.runMode}) {
                sendMessage("Changing Mode to ${settings.runMode} because ${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}")
    			location.setMode(settings.runMode)
            } else {
            	sendMessage("${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}, but requested Mode change (${settings.runMode}) is no longer supported by this location")
            }
        }
    } else {
    	sendMessage("${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}, and your location is already in the requested ${settings.runMode} mode")
    }
    state.ecobeeThatChanged = null
}

def runRoutine() {
	if (state.ecobeeThatChanged) sendMessage("Executing Routine ${settings.runAction} because ${state.ecobeeThatChanged} changed to ${state.ecobeeNewProgram}")
    state.ecobeeThatChanged = null
	location.helloHome?.execute(settings.runAction)
}

def changeProgramHandler(evt) {
	LOG("changeProgramHander() entered with evt: ${evt.name}: ${evt.value}", 5)
	
    def gotEvent = (settings.modeOrRoutine == "Routine") ? evt.displayName?.toLowerCase() : evt.value?.toLowerCase()
    LOG("Event name received (in lowercase): ${gotEvent} and current expected: ${state.expectedEvent}", 5)

    if ( !state.expectedEvent*.toLowerCase().contains(gotEvent) ) {
    	LOG("Received an mode/routine that we aren't watching. Nothing to do.", 4)
        return true
    }
    
    settings.myThermostats.each { stat ->
    	LOG("In each loop: Working on stat: ${stat}", 4, null, 'trace')
        def thermostatHold = stat.currentValue('thermostatHold')
        boolean vacationHold = (thermostatHold == 'vacation')
        // Can't change the program while in Vacation mode
        if (vacationHold) {
        	if (state.doCancelVacation) {
            	stat.cancelVacation()
                sendMessage("As requested, I cancelled the active Vacation Hold on ${stat}.")
                thermostatHold = 'hold'		// Fake it, so that resumeProgram executes below
                vacationHold = false
                state.refresh()				// force a poll for changes from the Ecobee cloud
            } else if (state.doResumeProgram) {
            	LOG("Can't Resume Program while in Vacation mode (${stat})",2,null,'warn')
           		sendMessage("I was asked to Resume Program on ${stat}, but it is currently in 'Vacation' Hold so I ignored the request.")
            } else {
        		LOG("Can't change Program while in Vacation mode (${stat})",2,null,'warn')
           		sendMessage("I was asked to change ${stat} to ${state.programParam}, but it is currently in 'Vacation' Hold so I ignored the request.")
            }
        }
        
        if (!vacationHold) {
        	// If we get here, we aren't in a Vacation Hold
        	if (state.doResumeProgram) {
        		LOG("Resuming Program for ${stat}", 4, null, 'trace')
            	if (thermostatHold == 'hold') {
            		def scheduledProgram = stat.currentValue("scheduledProgram")
        			stat.resumeProgram(true) 												// resumeAll to get back to the scheduled program
                	if (state.fanMinutes != null) stat.setFanMinOnTime(state.fanMinutes)		// and reset the fanMinOnTime as requested
					sendMessage("And I resumed the scheduled ${scheduledProgram} program on ${stat}.")
            	} else {
            		// Resume Program requested, but no hold is currently active
                	sendMessage("I was asked to Resume Program on ${stat}, but there is no Hold currently active.")
            	}
        	} else {
            	// set the requested program
        		if (state.programParam != null) {
        			LOG("Setting Thermostat Program to programParam: ${state.programParam} and holdType: ${state.holdTypeParam}", 4, null, 'trace')      
        			boolean done = false
        			// def currentProgram = stat.currentValue('currentProgram')
        			def currentProgramName = stat.currentValue('currentProgramName')	// cancelProgram() will reset the currentProgramName to the scheduledProgramName
        			if ((thermostatHold == '') && (currentProgramName == state.programParam)) {
                    	// not in a hold, currentProgram is the desiredProgram
                		def fanSet = false
                		if (state.fanMinutes != null) {
                    		stat.setFanMinOnTime(state.fanMinutes)			// set fanMinOnTime
                        	fanSet = true
                    	}
               			if (state.fanCommand != null) {
                    		stat."${state.fanCommand}"()					// set fan on/auto
                        	fanSet = true
                    	}
                        if (settings.statOff) {
                        	// Don't grab a reservation here, since we won't be around later to release it
                        	stat.off()
                            sendMessage("And I verified that ${stat.displayName} is already in the ${state.programParam} program, so I turned off the HVAC as requested.")
                        } else {
                			sendMessage("And I verified that ${stat.displayName} is already in the ${state.programParam} program${fanSet?' with the requested fan settings.':'.'}")
                        }
                		done = true
                    } else if ((thermostatHold == 'hold') || currentProgramName.startsWith('Hold')) { // (In case the Vacation hasn't cleared yet)
                    	// In a hold
            			if (stat.currentValue('scheduledProgram') == state.programParam) {
                        	// the scheduledProgram is the desiredProgram
                			stat.resumeProgram(true)	// resumeAll to get back to the originally scheduled program
                			def fanSet = false
                			if (state.fanMinutes != null) {
                    			stat.setFanMinOnTime(state.fanMinutes)		// set fanMinOnTime
                        		fanSet = true
                    		}
               				if (state.fanCommand != null) {
                    			stat."${state.fanCommand}"()				// set fan on/auto
                        		fanSet = true
                    		}
                            if (whatHoldType(stat) == 'nextTransition') {
                            	if (settings.statOff) {
                                	// Don't make a reservation, since we won't be around later to release it
                            		stat.off()
                                    sendMessage("And I resumed the scheduled ${state.programParam} program on ${stat.displayName}, then I turned off the HVAC as requested.")
                                } else {
                					sendMessage("And I resumed the scheduled ${state.programParam} program on ${stat.displayName}${fanSet?' with the requested fan settings.':'.'}")
                                }
                				done = true
                            }
            			} else if (stat.currentValue('currentProgram') == state.programParam) {
                        	// we are in a hold already, and the program is the one we want...
                        	// Assume (for now) that the fan settings are also what we want (because another instance set them when they set the Hold)
                            sendMessage("${stat.displayName} is already in the specified Hold: ${state.programParam}.")
                            done = true
                        } else { 
                        	// the scheduledProgram is NOT the desiredProgram, so we need to resumeAll, then set the desired program as a Hold: Program
                			stat.resumeProgram(true)
                    		done = false
                		}
            		}
            		if (!done) {
           				// Looks like we are setting a Hold: 
                		def fanSet = false
                		if (state.fanMinutes != null) {
                   			stat.setFanMinOnTime(state.fanMinutes)		// set fanMinOnTime before setting the Hold:, becuase you can't change it after the Hold:
                    		fanSet = true
                		}
                        def sendHoldType = whatHoldType(stat)
    					def sendHoldHours = null
    					if (sendHoldType != null) {
    						sendHoldHours = sendHoldType
    						sendHoldType = 'holdHours'
						}
            			stat.setThermostatProgram(state.programParam, sendHoldType, sendHoldHours)
                		if (state.fanCommand != null) {
                   			stat."${state.fanCommand}"()				// set fan on/auto AFTER changing the program, because we are overriding the program's setting
                    		fanSet = true
                		}
                        String timeStr = ''
                        switch (sendHoldType) {
                        	case 'indefinitely':
                            	timeStr = ' indefinitely'
                                break;
                            case 'nextTransition':
                            	timeStr = ' until next scheduled program change'
                                break
                            case 'holdHours':
                            	timeStr = " for ${sendHoldHours} hours"
                                break;
                        }
                        if (settings.statOff) {
                        	stat.off()
                            sendMessage("And I set ${stat.displayName} to Hold: ${state.programParam}${timeStr}, then I turned off the HVAC as requested.")
                        } else {
							sendMessage("And I set ${stat.displayName} to Hold: ${state.programParam}${timeStr}${fanSet?' with the requested fan settings.':'.'}")
                        }
               		}
            	} // else { assert state.programParam == null; must have been 'Resume Program' or an old 'Cancel Vacation'  }
            }
		}
    }
    return true
}

// returns the holdType keyword, OR the number of hours to hold
// precedence: 1. this SmartApp's preferences, 2. Parent settings.holdType, 3. indefinite (must specify to use the thermostat setting)
def whatHoldType(statDevice) {
    def theHoldType = settings.holdType
    def sendHoldType = null
	def ht = getParentSetting('holdType')
	def hh = getParentSetting('holdHours')
    if ((settings.holdType == null) || (settings.holdType == "Parent Ecobee (Connect) Setting")) {
        if (ht == null) {	// default for Ecobee (Connect) is permanent hold (legacy)
        	LOG('Using holdType indefinite',2,null,'info')
        	return 'indefinite'
        } else if (ht != 'Thermostat Setting') {
        	theHoldType = ht
        }
    }
    
    switch (theHoldType) {
      	case 'Until I Change':
            sendHoldType = 'indefinite'
            break;   
        case 'Until Next Program':
           	sendHoldType = 'nextTransition'
            break;               
        case '2 Hours':
        	sendHoldType = 2
            break;
        case '4 Hours':
        	sendHoldType = 4
        case 'Specified Hours':
            if (settings.holdHours != null) {
            	sendHoldType = settings.holdHours as Integer
            } else if ((ht == 'Specified Hours') && (hh != null)) {
            	sendHoldType = hh as Integer
            } else if ( ht == '2 Hours') {
            	sendHoldType = 2
            } else if ( ht == '4 Hours') {
            	sendHoldType = 4            
            } else {
            	sendHoldType = 2
            }
            break;
        case 'Thermostat Setting':
       		def statHoldType = statDevice.currentValue('statHoldAction')
            switch(statHoldType) {
            	case 'useEndTime4hour':
                	sendHoldType = 4
                    break;
                case 'useEndTime2hour':
                	sendHoldType = 2
                    break;
                case 'nextPeriod':
                case 'nextTransition':
                	sendHoldType = 'nextTransition'
                    break;
                case 'indefinite':
                case 'askMe':
                case null :
                default :
                	sendHoldType = 'indefinite'
                    break;
           }
    }
    if (sendHoldType != null) {
    	LOG("Using holdType ${sendHoldType.toString().isNumber()?'holdHours ('+sendHoldType.toString()+')':sendHoldType}",2,null,'info')
        return sendHoldType
    } else {
    	LOG("Couldn't determine holdType, returning indefinite",1,null,'error')
        return 'indefinite'
    }
}

// Helper Functions
// get the combined set of Ecobee Programs applicable for these thermostats
private def getEcobeePrograms() {
	def programs

	if (myThermostats?.size() > 0) {
		myThermostats.each { stat ->
        	def DNI = stat.device.deviceNetworkId
            LOG("Getting list of programs for stat (${stat}) with DNI (${DNI})", 4)
        	if (!programs) {
            	LOG("No programs yet, adding to the list", 5)
                programs = parent.getAvailablePrograms(stat)
            } else {
            	LOG("Already have some programs, need to create the set of overlapping", 5)
                programs = programs.intersect(parent.getAvailablePrograms(stat))
            }
        }
	} 
    LOG("getEcobeePrograms: returning ${programs}", 4)
    if (programs) { return programs.sort(false) } else { return ['Away', 'Home', 'Sleep'] }
}

// return all the modes that ALL thermostats support
def getThermostatModes() {
	def theModes = []
    
    settings.myThermostats.each { stat ->
    	if (theModes == []) {
        	theModes = stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", ")
        } else {
        	theModes = theModes.intersect(stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", "))
        }   
    }
    return theModes.sort(false)
}

private def sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "trace")

    if (settings.notify) {
        String msg = "${app.label} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
        if (phone) { // check that the user did select a phone number
            if ( phone.indexOf(";") > 0){
                def phones = phone.split(";")
                for ( def i = 0; i < phones.size(); i++) {
                    LOG("Sending SMS ${i+1} to ${phones[i]}",2,null,'info')
                    sendSmsMessage(phones[i], msg)				// Only to SMS contact
                }
            } else {
                LOG("Sending SMS to ${phone}",2,null,'info')
                sendSmsMessage(phone, msg)						// Only to SMS contact
            }
        } else if (settings.pushNotify) {
            LOG("Sending Push to everyone",2,null,'warn')
            sendPushMessage(msg)								// Push to everyone
        }
    }
    if (isST) sendNotificationEvent( notificationMessage )		// Always send to hello home (not available on Hubitat yet)
}


// return all the fan modes that ALL thermostats support
def getThermostatFanModes() {
	def theFanModes = []
    
    settings.myThermostats.each { stat ->
    	if (theFanModes == []) {
        	theFanModes = stat.currentValue('supportedThermostatFanModes')[1..-2].tokenize(", ")
        } else {
        	theFanModes = theFanModes.intersect(stat.currentValue('supportedThermostatFanModes')[1..-2].tokenize(", "))
        }   
    }
    theFanModes = (theFanModes - ['off']) + ['default']		// off isn't fully implemented yet
    return theFanModes*.capitalize()
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	def messageLbl = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent.LOG(messageLbl, level, null, logType, event, displayEvent)
    log."${logType}" message
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
