/**
 *  ecobee Suite Routines
 *
 *  Copyright 2015 Sean Kendall Schneyer
 *	Copyright 2017-2020 Barry A. Burke
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
 *	1.7.04 - noncached currentValue() on HE
 *	1.7.05 - Belt & suspenders for thermostatHold compares
 *	1.7.06 - Cosmetic Cleanups
 *	1.7.07 - Fixed variable definition (ncCp)
 *	1.7.08 - Cleaned up messages (a little - more still to do)
 *	1.7.09 - Fixed SMS text entry
 *	1.7.10 - Fixing private method issue caused by grails, "Vacation" from currentProgramName
 *	1.7.11 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.12 - Optimized isST/isHE, formatting, Global Pause
 *	1.7.13 - Fixed holdType == ES Manager setting, & getEcobeePrograms()
 *	1.7.14 - Clean up appLabel in sendMessage(), eliminate duplicate currentProgram/currentProgramName subscriptions
 *	1.7.15 - Remove fanMode setting, fix fanMinOnTime to not break the hold
 *	1.7.16 - Added option to disable local display of log.debug() logs, support Notification devices on ST
 *	1.7.17 - Option to skip notification if Thermostat Mode is 'Off'
 *	1.7.18 - Fixed helper labelling
 *	1.7.19 - Fixed labels (again), added infoOff, cleaned up preferences setup
 *	1.7.20 - Added minimize UI
 *	1.7.21 - Layout tweaks for Hubitat
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - Updated WARNING formatting
 *	1.8.03 - More busy bees
 *	1.8.04 - Send simultaneous notification Announcements to multiple Echo Speaks devices
 *	1.8.05 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.06 - New SHPL, using Global Fields instead of atomicState
 *	1.8.07 - Fixed appDisplayName in sendMessage
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.8.07" }
String getVersionLabel() 	{ return "Ecobee Suite Mode${isST?'/Routine':''}/Switches/Program Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Routines",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description:		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nChange Ecobee Programs based on ${isST?'SmartThings Routine execution or':'Hubitat'} Mode changes, Switch(es) state change, OR change Mode/run Routine based on Ecobee Program/Vacation changes",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-routines.src/ecobee-suite-routines.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-routines-sa",
	singleInstance:		false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	//boolean ST = isST
	//boolean HE = !ST
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
    String defaultName = "Mode${ST?'/Routine':''}/Switches/Program"
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This polyfunctional Helper coordinates Ecobee Suite Thermostats' Programs with your ${getHubPlatform()} Location Mode, ${ST?'Routines, ':''} "+
                          "and/or (real or virtual) Switches. You can change your thermostat's program based on Location Mode changes and events, or your thermostat's scheduled program changes can update your "+
                          "Location Mode, turn on switches, etc.")
            }
        }
            
		section(title: sectionTitle("Naming${!settings.tempDisable?' & Thermostat Selection':''}")) {	
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
			if (HE) {
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
			} else {
            	if (app.label.contains(' (paused)')) {
                	if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains(' (paused)')) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
                        String myLabel = app.label.substring(0, app.label.indexOf(' (paused)'))
                        atomicState.appDisplayName = myLabel
                        app.updateLabel(myLabel)
                    }
                } else {
                	atomicState.appDisplayName = app.label
                }
            }
        	if(settings.tempDisable) { 
				paragraph getFormat("warning","This Helper is temporarily paused...")
			} else { 
        		input ("myThermostats", "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select Ecobee Suite Thermostat(s)"), required: true, 
					   multiple: true, submitOnChange: true)            
			}
        }
        
        if (!settings?.tempDisable && (settings?.myThermostats?.size()>0)) {
			section(title: sectionTitle("Trigger Selection")) {
        		// Settings option for using Mode or Routine
				input(name: "modeOrRoutine", title: inputTitle("Select a Trigger"), type: "enum", required: true, multiple: false, submitOnChange: true, width: 6,
					  options: (ST?["Mode":'Location Mode Change',"Routine":'Routine Execution',"Switch(es)":'Switch(es) On/Off',"Ecobee Program":'Ecobee Program Change']:
							   /*HE*/["Mode":'Location Mode Change',"Switch(es)":'Switch(es) On/Off',"Ecobee Program":'Ecobee Program Change']))
					  
				if (settings?.modeOrRoutine != null) {
					if (settings?.modeOrRoutine == "Mode") {
						input(name: "modes", type: "mode", title: inputTitle("When the Location Mode changes to: "), required: true, multiple: true, width: 5)
					} else if (ST && (settings?.modeOrRoutine == "Routine")) {
						def actions = location.helloHome?.getPhrases()*.label
						if (actions) {
							actions.sort()
							input(name: "action", type: "enum", title: inputTitle("When these Routines execute: "), options: actions, required: true, multiple: true)
						}
					} else if (settings.modeOrRoutine == "Switch(es)") {
						input(name: 'startSwitches', type: 'capability.switch', required: true, title: inputTitle('When any of these switches...'), multiple: true, submitOnChange: true)
						if (settings.startSwitches) {
							def s = (settings.startSwitches.size() > 1)
							input(name: "startOn", type: "enum", title: inputTitle("${s?'Are':'Is'} turned:"), required: true, multiple: false, options: ["on","off"], submitOnChange: true, 
								  defaultValue: 'on', width: 2)
							if (settings.startOn != null) {
								input(name: "startOff", type: 'bool', title: inputTitle("Turn the switch${s?'es':''} ${settings.startOn=='on'?'off':'on'} after running Actions?"), defaultValue: 'false', 
									  submitOnChange: true, width: 5)
								if (settings.startOff) input(name: "startOffDelay", type: 'number', title: inputTitle("Delay before turning ${settings.startOn=='on'?'off':'on'} (seconds):"), required: true, 
															 defaultValue: 0, range: "0..3600", submitOnChange: true, width: 5)
								String explain = "This Helper will run the Actions below when ${s?'any of these switches are':'the switch '+settings.startSwitches[0].displayName+' is'} turned ${settings.startOn?'On':'Off'}"
								if (settings.startOff) explain += ", and the switch${s?'es':' '+settings.startSwitches[0].displayName} will be turned ${settings.startOn=='on'?'Off':'On'} ${((settings.startOffDelay==null)||(settings.startOffDelay==0))?'when':settings.startOffDelay.toString()+' seconds after'} the Actions are completed"
								if (maximize) paragraph explain
							} else if (HE) paragraph("", width: 6)
						}
					} else if (settings.modeOrRoutine == "Ecobee Program") {
						def programs = getThermostatPrograms()
						programs = programs + ["Vacation"]
						input(name: "ctrlProgram", title: inputTitle("When the Ecobee Program changes to:"), type: "enum", options: programs, required: true, multiple: true,
							  width: 5)
						def n = settings?.myThermostats?.size()
						if (n > 1) paragraph("NOTE: It is recommended (but not required) to select only one thermostat when using Ecobee Program Change as the Trigger")
					}
				}
			}

			section(title: sectionTitle("Actions")) {
				if (settings?.modeOrRoutine != "Ecobee Program") {
					def programs = getThermostatPrograms()
					programs += ["Resume Program"]
					LOG("Found the following programs: ${programs}", 4)

					input(name: "cancelVacation", title: inputTitle("Cancel Vacation hold if active?"), type: "bool", required: true, defaultValue: false, width: 6)
					if (HE) paragraph("", width: 6)
					input(name: "whichProgram", title: inputTitle("Switch to this Ecobee Program:"), type: "enum", required: true, multiple:false, options: programs, 
						  submitOnChange: true, width: 6)
					if (HE) paragraph("", width: 6)
					/* if (settings?.whichProgram != 'Resume Program') {
						if (settings?.fanMinutes == null) { // || ((settings.fanMinutes != null) && (settings.fanMinutes != 0))) {
							// input(name: "fanMode", title: "Fan Mode (optional)", type: "enum", required: false, multiple: false, options: getThermostatFanModes(), submitOnChange: true)
						} else 
					} */
					/*if (settings.fanMode == 'Auto') {
						paragraph("Note that the fan circulation time will also be set to 0 because you selected Fan Mode 'Auto'")
					} else if (settings.fanMode == 'On') {
						paragraph("Note that the fan circulation time will be set to 0 because you selected Fan Mode 'On'")
					} else if (settings.fanMode == 'Off') {
						// this can never happen, because 'off' is not a value fanMode
						input(name: 'statOff', title: 'Do you want to turn off the HVAC entirely?', type: 'bool', defaultValue: false)
					} else if ((settings.fanMode == null) || (settings.fanMode == 'Circulate')) { */
					input(name: "fanMinutes", title: inputTitle("Set Fan Circulation Minutes per Hour (optional)"), type: "number", 
						  required: false, multiple: false, range:"0..55", submitOnChange: true, width: 6)
					if (HE) paragraph("", width: 6)
							  // defaultValue: (settings.fanMode==null?settings.fanMinutes:20))
						// defaultValue: (settings.fanMode==null?(settings.fanMinutes!=null?settings.fanMinutes:0):20))
					//}
					if (settings?.fanMinutes != null) {
						if (settings.fanMinutes == 0) {
							if (maximize) paragraph("Note that the Fan Mode will be set to 'Auto' because you specified 0 Circulation Minutes")
						} else {
							if (maximize) paragraph("Note that the Fan Mode will be set to 'Circulate' because you specified non-0 Circulation Minutes")
						}
					}
					if (settings.whichProgram != "Resume Program") {
						input(name: "holdType", title: inputTitle("Select Hold Type (optional)"), type: "enum", required: false, width: 6, 
							  multiple: false, submitOnChange: true, defaultValue: "Ecobee Manager Setting",
							  options:["Until I Change", "Until Next Program", "2 Hours", "4 Hours", "Specified Hours", "Thermostat Setting", 
													"Ecobee Manager Setting"]) //, "Parent Ecobee (Connect) Setting"])
						if (settings.holdType=="Specified Hours") {
							input(name: 'holdHours', title: inputTitle('How many hours (1-48)?'), type: 'number', range:"1..48", required: true, description: '2', defaultValue: 2, width: 4)
						} else if (settings.holdType=='Thermostat Setting') {
							if (maximize) paragraph("Thermostat Setting at the time of the hold request will be applied")
						} else if ((settings.holdType == null) || (settings.holdType == 'Ecobee Manager Setting') || (settings.holdType == 'Parent Ecobee (Connect) Setting')) {
							if (maximize) paragraph("Ecobee Manager Setting at the time of the hold request will be applied")
						}
					}
					// input(name: "useSunriseSunset", title: "Also at Sunrise or Sunset? (optional) ", type: "enum", required: false, multiple: true, description: "Tap to choose...", metadata:[values:["Sunrise", "Sunset"]], submitOnChange: true)                
				} else {
					if (ST) {
						input(name: "runModeOrRoutine", title: inputTitle("Change Mode or Execute Routine:"), type: "enum", required: true, multiple: false, defaultValue: "Mode", 
						  options: ["Mode", "Routine"], submitOnChange: true)
					} else {
						app.updateSetting('runModeOrRoutine', 'Mode')
						settings?.runModeOrRoutine = 'Mode'
					}
					if ((settings.runModeOrRoutine == null) || (settings.runModeOrRoutine == "Mode")) {
						input(name: "runMode", type: "mode", title: inputTitle("Change Location Mode to: "), required: true, multiple: false, width: 4)
					} else if (HE) {
						paragraph("", width: 6)
					} else if (ST && settings.runModeOrRoutine == "Routine") {
						// Routine based inputs
						def actions = location.helloHome?.getPhrases()*.label
						if (actions) {
							actions.sort()
							input(name:"runAction", type:"enum", title: inputTitle("Execute this Routine: "), options: actions, required: true, multiple: false)
						} // End if (actions)
					} // End if (Routine)
				} // End else Program --> Mode/Routine
				// switches

				input(name: 'doneSwitches', type: 'capability.switch', title: inputTitle("Also change these switches (optional)"), required: false, multiple: true, submitOnChange: true)
				if (settings.doneSwitches) {
					def s = (settings.doneSwitches.size() > 1)
					input(name: "doneOn", type: "enum", title: inputTitle("Turn the Switch${s?'es':''}:"), required: true, multiple: false, defaultValue: 'off', options: ["on","off"], 
						  submitOnChange: true, width: 3)
					if (HE) paragraph("", width: 9)
				}
			} // End of "Actions" section

			if (ST) {
				section("Notifications") {
					input(name: "notify", type: "bool", title: inputTitle("Notify on Actions?"), required: true, defaultValue: false, submitOnChange: true, width: 4)
					if (settings.notify) {
						input(name: "noOffNotify", type: "bool", title: inputTitle("Suppress notifications if Thermostat Mode is Off?"), required: true, 
							  defaultValue: false, submitOnChange: true)
						input(name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, 
							  required: ((settings?.phone == null) && !settings.notifiers && !settings.speak), submitOnChange: true)
						input(name: "notifiers", type: "capability.notification", title: "Select Notification Devices", hideWhenEmpty: true,
							  required: ((settings.phone == null) && !settings.speak && !settings.pushNotify), multiple: true, submitOnChange: true)
                        if (settings?.notifiers) {
                            List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                            if (echo) {
                            	input(name: "echoAnnouncements", type: "bool", title: "Use ${echo.size()>1?'simultaneous ':''}Announcements for the Echo Speaks device${echo.size()>1?'s':''}?", 
                                	  defaultValue: false, submitOnChange: true)
                            }
                        }
						input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777; +441234567890)", 
							  required: (!settings.pushNotify && !settings.notifiers && !settings.speak), submitOnChange: true)
					}
				}
				if (settings.notify) {
					section(hideWhenEmpty: (!"speechDevices" && !"musicDevices"), title: "") {
						input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", 
								  multiple: true, hideWhenEmpty: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", 
								  multiple: true, hideWhenEmpty: true, submitOnChange: true)
							if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
					}
				}
                if (maximize) {
					section() {
						paragraph ( "A notification is always sent to the Hello Home log whenever an action is taken")
					}
                }
			} else {		// HE
				section(sectionTitle("Notifications")) {
					input(name: "notify", type: "bool", title: inputTitle("Notify on Actions?"), required: true, defaultValue: false, submitOnChange: true, width: 4)
					if (settings.notify) {
						input(name: "noOffNotify", type: "bool", title: inputTitle("Suppress notifications if Thermostat Mode is Off?"), required: true, 
							  defaultValue: false, submitOnChange: true, width: 8)
                        input(name: "notifiers", type: "capability.notification", multiple: true, title: inputTitle("Select Notification Devices"), submitOnChange: true,
                              required: (!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null))))
                    	if (settings?.notifiers) {
                            List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                            if (echo) {
                            	input(name: "echoAnnouncements", type: "bool", title: "Use ${echo.size()>1?'simultaneous ':''}Announcements for the Echo Speaks device${echo.size()>1?'s':''}?", 
                                	  defaultValue: false, submitOnChange: true)
                            }
                        }
					}
				}
				if (settings.notify) {
					section(hideWhenEmpty: (!"speechDevices" && !"musicDevices"), title: "") {
						input(name: "speak", type: "bool", title: inputTitle("Speak messages?"), required: !settings?.notifiers, defaultValue: false, submitOnChange: true, width: 6)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: inputTitle("Select speech devices"), 
								  multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: inputTitle("Select music devices"), 
								  multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
							input(name: "volume", type: "number", range: "0..100", title: inputTitle("At this volume (%)"), defaultValue: 50, required: false, width: 4)
						}
					}
				}
                if (maximize) {
					section("A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken"	){}
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
        if (ST) {
        	section(getVersionLabel().replace('er, v',"er\nV")+"\n\nCopyright \u00a9 2017-2020 Barry A. Burke\nAll rights reserved.\n\nhttps://github.com/SANdood/Ecobee-Suite") {}
        } else {
        	section() {
        		paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br><small>Copyright \u00a9 2017-2020 Barry A. Burke - All rights reserved.</small><br>"+
                		  "<a href='https://github.com/SANdood/Ecobee-Suite' target='_blank' style='color:#5BBD76'><u>Click here for the Ecobee Suite GitHub Repository</u></a></div>")
            }
		}
    }
}

// Main functions
def installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
	initialize()  
}
def updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    initialize()
}
def initialize() {
	atomicState.versionLabel = getVersionLabel()
	LOG("${atomicState.versionLabel} - Initializing...", 2, "", 'info')
	updateMyLabel()
	
    if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
	if (settings.debugOff) log.info "log.debug() logging disabled"
	
	if (settings.modeOrRoutine == "Routine") {
    	subscribe(location, "routineExecuted", changeProgramHandler)
    } else if (settings.modeOrRoutine == "Mode") {
    	subscribe(location, "mode", changeProgramHandler)
	} else if (settings.modeOrRoutine == "Switch(es)") {
		subscribe(startSwitches, "switch.${startOn}", changeSwitchHandler)
    } else { // has to be "Ecobee Program"
    	subscribe(myThermostats, "currentProgram", changeSTHandler)
		// subscribe(myThermostats, "currentProgramName", changeSTHandler)
    }
    
//    if(useSunriseSunset?.size() > 0) {
//		// Setup subscriptions for sunrise and/or sunset as well
//        if( useSunriseSunset.contains("Sunrise") ) subscribe(location, "sunrise", changeProgramHandler)
//        if( useSunriseSunset.contains("Sunset") ) subscribe(location, "sunset", changeProgramHandler)
//    }
    
	// Normalize settings data
    normalizeSettings()
    LOG("...initialization complete")
}

void normalizeSettings() {
	
	// whichProgram
	atomicState.programParam = null
    atomicState.doResumeProgram = false
    atomicState.doCancelVacation = settings.cancelVacation?true:false
	if (whichProgram != null && whichProgram != "") {
    	if (whichProgram == "Resume Program") {
        	atomicState.doResumeProgram = true
        } else if (whichProgram == 'Cancel Vacation') {
        	atomicState.doCancelVacation = true
            // atomicState.programParam = null
        } else {        	
    		atomicState.programParam = whichProgram
    	}
	}
    
    // fanMode
    atomicState.fanCommand = null
    atomicState.fanMinutes = null
    /* switch (fanMode) {
        case 'On': 
        	atomicState.fanCommand = 'fanOn'
            //if (settings.fanMinutes != null) {
    		//	atomicState.fanMinutes = settings.fanMinutes.toInteger()
   			//}
			atomicState.fanMinutes = 0
            break;
        case 'Auto':
        	atomicState.fanCommand = 'fanAuto'
            atomicState.fanMinutes = 0
            break;
        case 'Off': 
            atomicState.fanCommand = 'fanOff'		// to turn off the fan, we need: tstatMode==Off, fanMode==Auto, fanMinOnTime==0
            atomicState.fanMinutes = 0
            break;
    	case 'Circulate':
            atomicState.fanCommand = 'fanCirculate'
            if (settings.fanMinutes == null) {
                atomicState.fanMinutes = 20
            } else if (settings.fanMinutes != null) {
                atomicState.fanMinutes = settings.fanMinutes.toInteger()
            } 
            if (atomicState.fanMinutes == 0) atomicState.fanCommand = 'fanAuto'	// override Circulate with 0 minutes of minFanOnTime
            break;
        default : 
        	atomicState.fanCommand = null		// default
		*/
	if (settings.fanMinutes != null) {
		atomicState.fanMinutes = settings.fanMinutes.toInteger()
		// if (settings.fanMode == null) */ atomicState.fanCommand = (atomicState.fanMinutes == 0) ? 'fanAuto' : 'fanCirculate'	// for backwards compatibility
	}
//   }
    
    // holdType is now calculated at the time of the hold request
    atomicState.holdTypeParam = null
//    if (settings.holdType != null && settings.holdType != "") {
//    	if (holdType == "Until I Change") {
//        	atomicState.holdTypeParam = "indefinite"
//        } else if (holdType == "Until Next Program") {
//        	atomicState.holdTypeParam = "nextTransition"
//        } else {
//        	atomicState.holdTypeParam = null
//        }
//    }
    
	if (settings.modeOrRoutine == "Routine") {
    	atomicState.expectedEvent = settings.action
	} else if (settings.modeOrRoutine == 'Mode') {
    	atomicState.expectedEvent = settings.modes
    }
	LOG("atomicState.expectedEvent set to ${atomicState.expectedEvent}", 4)
}

def changeSwitchHandler(evt) {
	LOG("changeSwitchHandler() entered with evt: ${evt.device.displayName} ${evt.name} turned ${evt.value}", 4, null, 'trace')

	atomicState.theSwitch = evt.device.displayName
	
	if (settings.modeOrRoutine && (settings.modeOrRoutine != 'Ecobee Program')) {
		changeProgramHandler( evt )
	} else {
		if ((settings.runModeOrRoutine == null) || (settings.runModeOrRoutine == "Mode")) {
			LOG("Changing Mode to ${settings.runMode} because ${evt.device.displayName} was turned ${settings.startOn}",2,null,'info')
			changeMode(true) 
		} else {
			LOG("Executing Routine ${settings.runAction} because ${evt.device.displayName} was turned ${settings.startOn}",2,null,'info')
			runRoutine(true) 
		}
		if (settings.doneSwitches) changeSwitches()
	}
	
	if (settings.startOff) {
		if ((settings.startOffDelay == null) || (settings.startOffDelay == 0)) {
			turnOffStartSwitches()
		} else {
			runIn( settings.startOffDelay, turnOffStartSwitches, [overwrite: true] )
		}
	}
}

void turnOffStartSwitches() {
	if (settings.startOff) {
		settings.startSwitches.each { aSwitch ->
			if (aSwitch.displayName == atomicState.theSwitch) {
				if (settings.startOn == 'on') {
					aSwitch.off()
				} else {
					aSwitch.on()
				}
			}
			// it."${settings.startOn=='on'?'off()':'on()'}"
		}
		LOG("And I turned ${settings.startOn=='on'?'off':'on'} ${atomicState.theSwitch}", 2, null, 'info')
	}
	atomicState.theSwitch = null
}

def changeSTHandler(evt) {
	LOG("changeSTHandler() - entered with evt: ${evt.name}: ${evt.value}", 4, null, 'trace')
	if (evt.name.endsWith('e')) {
		LOG('Cleaning up duplicate subscriptions to both currentOProgram & currentProgramName',1,null,'trace')
		unsubscribe(myThermostats)
		subscribe(myThermostats, 'currentProgram', changeSTHandler)
		return	// ignore this 'duplicate' event
	}
	
    if (settings.ctrlProgram.contains(evt.value)) {
    	if (!atomicState.ecobeeThatChanged) {
        	atomicState.ecobeeThatChanged = evt.displayName
            atomicState.ecobeeNewProgram = evt.value
        }
    	if (settings.runModeOrRoutine == "Mode") {
        	LOG("Changing Mode to ${settings.runMode} because ${atomicState.ecobeeThatChanged} changed to ${atomicState.ecobeeNewProgram}",2,null,'info')
        	if (settings.myThermostats.size() == 1) { 
            	changeMode() 
            } else { 
            	// trick to avoid multiple calls if more than 1 thermostat - they could change simultaneously or within the next pollCycle
                // to avoid this delayed response, assign to only one thermostat
                parent.poll()
            	runIn(5, changeMode, [overwrite: true]) 
            }	
        } else {
        	LOG("Executing Routine ${settings.runAction} because ${atomicState.ecobeeThatChanged} changed to ${atomicState.ecobeeNewProgram}",2,null,'info')
        	if (settings.myThermostats.size() == 1) { 
            	runRoutine() 
            } else {
            	parent.poll()
            	runIn(10, runRoutine, [overwrite: true]) 
            }
        }
		if (settings.doneSwitches) runin(15, changeSwitches, [overwrite: true])
    }
}

void changeMode(aSwitch = null) {
	if (aSwitch != null) {
		if (settings.runMode != location.mode) { 	// only if we aren't already in the specified Mode
			if (location.modes?.find {it.name == settings.runMode}) {
				sendMessage("Changing Mode to ${settings.runMode} because ${aSwitch} was turned ${settings.startOn}")
				location.setMode(settings.runMode)
			} else {
				sendMessage("${aSwitch} was turned ${settings.startOn}, but the requested Mode change (${settings.runMode}) is no longer supported by this location")
			}
		} else {
			sendMessage("${aSwitch} was turned ${settings.startOn}, and your location is already in the requested ${settings.runMode} mode")
		}
	} else {
		if (settings.runMode != location.mode) { 	// only if we aren't already in the specified Mode
			if (atomicState.ecobeeThatChanged) {
				if (location.modes?.find {it.name == settings.runMode}) {
					sendMessage("Changing Mode to ${settings.runMode} because ${atomicState.ecobeeThatChanged} changed to ${atomicState.ecobeeNewProgram}")
					location.setMode(settings.runMode)
				} else {
					sendMessage("${atomicState.ecobeeThatChanged} changed to ${atomicState.ecobeeNewProgram}, but the requested Mode change (${settings.runMode}) is no longer supported by this location")
				}
			}
		} else {
			sendMessage("${atomicState.ecobeeThatChanged} changed to ${atomicState.ecobeeNewProgram}, and your location is already in the requested ${settings.runMode} mode")
		}
		atomicState.ecobeeThatChanged = null
	}
}

void runRoutine(aSwitch = null) {
	if (aSwitch != null) {
		sendMessage("Executing Routine ${settings.runAction} because ${aSwitch} was turned ${settings.startOn}")
	} else {
		if (atomicState.ecobeeThatChanged) {
			sendMessage("Executing Routine ${settings.runAction} because ${atomicState.ecobeeThatChanged} changed to ${atomicState.ecobeeNewProgram}")
    		atomicState.ecobeeThatChanged = null
		}
	}
	location.helloHome?.execute(settings.runAction)
}

void changeSwitches() {
	if (settings.doneSwitches) {
		settings.doneSwitches.each() {
			it."${doneOn}()"
		}
		def s = settings.doneSwitches.size() > 1
		sendMessage("Plus, I made sure that the ${s?'switches':'switch'} ${settings.doneSwitches.toString()[1..-2]} ${s?'are all':'is'} ${settings.doneOn}")
	}
}

def changeProgramHandler(evt) {
	LOG("changeProgramHandler() entered with evt: ${evt.name}: ${evt.value}", 4, null, 'trace')
	//boolean ST = atomicState.isST
	//boolean HE = !ST
	
	if (settings.modeOrRoutine != "Switch(es)") {
		// If we aren't using switches, validate that we got the intended event
		def gotEvent = HE ? evt.value : ((settings.modeOrRoutine == "Routine") ? evt.displayName : evt.value)
		LOG("Event name received: ${gotEvent} and current expected: ${atomicState.expectedEvent}", 5)

		if ( !(atomicState.expectedEvent?.contains(gotEvent)) ) {
			LOG("Received an mode/routine (${gotEvent}) that we aren't watching. Nothing to do.", 4)
			return true
		}
	}
    settings.myThermostats.each { stat ->
    	LOG("In each loop: Working on stat: ${stat}", 4, null, 'trace')
        String thermostatHold = ST ? stat.currentValue('thermostatHold') : stat.currentValue('thermostatHold', true)
		LOG("thermostatHold: ${thermostatHold}", 3, null, 'debug')
        boolean vacationHold = (thermostatHold == 'vacation')
        // Can't change the program while in Vacation mode
        if (vacationHold) {
        	if (atomicState.doCancelVacation) {
            	stat.cancelVacation()
                sendMessage("As requested, I cancelled the active Vacation Hold on ${stat}")
                thermostatHold = 'hold'		// Fake it, so that resumeProgram executes below
                vacationHold = false
                atomicState.refresh()				// force a poll for changes from the Ecobee cloud
            } else if (atomicState.doResumeProgram) {
            	LOG("Can't Resume Program while in Vacation mode (${stat})",2,null,'warn')
           		sendMessage("I was asked to Resume Program on ${stat}, but it is currently in 'Vacation' Hold so I ignored the request")
            } else {
        		LOG("Can't change Program while in Vacation mode (${stat})",2,null,'warn')
           		sendMessage("I was asked to change ${stat} to ${atomicState.programParam}, but it is currently in 'Vacation' Hold so I ignored the request")
            }
        }
        
        if (!vacationHold) {
        	// If we get here, we aren't in a Vacation Hold (any more)
        	if (atomicState.doResumeProgram) {
        		LOG("Resuming Program for ${stat}", 4, null, 'trace')
            	if (thermostatHold == 'hold') {
            		String scheduledProgram = ST ? stat.currentValue("scheduledProgram") : stat.currentValue("scheduledProgram", true)
        			stat.resumeProgram(true) 												// resumeAll to get back to the scheduled program
                	if (atomicState.fanMinutes != null) stat.setFanMinOnTime(atomicState.fanMinutes)		// and reset the fanMinOnTime as requested
					sendMessage("I resumed the scheduled ${scheduledProgram} program on ${stat}")
            	} else {
            		// Resume Program requested, but no hold is currently active
                	sendMessage("I was asked to Resume Program on ${stat}, but there is no Hold currently active")
            	}
        	} else {
            	// set the requested program
        		if (atomicState.programParam != null) {
        			LOG("Setting Thermostat Program to programParam: ${atomicState.programParam} and holdType: ${settings.holdType}", 4, null, 'trace')      
        			boolean done = false
        			// def currentProgram = stat.currentValue('currentProgram')
        			String currentProgramName = ST ? stat.currentValue('currentProgramName') : stat.currentValue('currentProgramName', true)	// cancelProgram() will reset the currentProgramName to the scheduledProgramName
        			if (((thermostatHold == null) || (thermostatHold == '') || (thermostatHold == 'null')) && (currentProgramName == atomicState.programParam)) {
                    	// not in a hold, currentProgram is the desiredProgram
                		boolean fanSet = false
                		if (atomicState.fanMinutes != null) {
                    		stat.setFanMinOnTime(atomicState.fanMinutes)		// set fanMinOnTime (setFanMinOnTime will handle auto/circulate)
                        	fanSet = true
                    	} else if (atomicState.fanCommand != null) {
                    		stat."${atomicState.fanCommand}"()					// set fan on or off
                        	fanSet = true
                    	}
                        if (settings.statOff) {
                        	// Don't grab a reservation here, since we won't be around later to release it
                        	stat.off()
                            sendMessage("I verified that ${stat.displayName} is already in the ${atomicState.programParam} program, so I turned off the HVAC as requested")
                        } else {
                			sendMessage("I verified that ${stat.displayName} is already in the ${atomicState.programParam} program${fanSet?' with the requested fan settings':''}")
                        }
                		done = true
                    } else if ((thermostatHold == 'hold') || currentProgramName.startsWith('Hold')) { // (In case the Vacation hasn't cleared yet)
                    	// In a hold
						String scheduledProgram = ST ? stat.currentValue('scheduledProgram') : stat.currentValue('scheduledProgram', true)
            			if ( scheduledProgram == atomicState.programParam) {
                        	// the scheduledProgram is the desiredProgram
                			stat.resumeProgram(true)	// resumeAll to get back to the originally scheduled program
                			def fanSet = false
                			if (atomicState.fanMinutes != null) {
                    			stat.setFanMinOnTime(atomicState.fanMinutes)	// set fanMinOnTime (setFanMinOnTime will handle auto/circulate)
                        		fanSet = true
                    		} else if (atomicState.fanCommand != null) {
                    			stat."${atomicState.fanCommand}"()				// set fan on/off
                        		fanSet = true
                    		}
                            if (whatHoldType(stat) == 'nextTransition') {
                            	if (settings.statOff) {
                                	// Don't make a reservation, since we won't be around later to release it
                            		stat.off()
                                    sendMessage("I resumed the scheduled ${atomicState.programParam} program on ${stat.displayName}, then I turned off the HVAC as requested")
                                } else {
                					sendMessage("I resumed the scheduled ${atomicState.programParam} program on ${stat.displayName}${fanSet?' with the requested fan settings':''}")
                                }
                				done = true
                            }
						} else {
							String currentProgram = ST ? stat.currentValue('currentProgram') : stat.currentValue('currentProgram', true)
							if (currentProgram == atomicState.programParam) {
								// we are in a hold already, and the program is the one we want...
								// Assume (for now) that the fan settings are also what we want (because another instance set them when they set the Hold)
								sendMessage("${stat.displayName} is already in the specified Hold: ${atomicState.programParam}")
								done = true
							} else { 
								// the scheduledProgram is NOT the desiredProgram, so we need to resumeAll, then set the desired program as a Hold: Program
								stat.resumeProgram(true)
								done = false
							}
						}
            		}
            		if (!done) {
           				// Looks like we are setting a Hold: 
                		def fanSet = false

                        def sendHoldType = whatHoldType(stat)
    					def sendHoldHours = null
    					if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
    						sendHoldHours = sendHoldType
    						sendHoldType = 'holdHours'
						}
                        LOG("sendHoldType: ${sendHoldType}, sendHoldHours: ${sendHoldHours}", 3, null, 'debug')
            			stat.setThermostatProgram(atomicState.programParam, sendHoldType, sendHoldHours)
						if (atomicState.fanMinutes != null) {
                   			stat.setFanMinOnTime(atomicState.fanMinutes)		// set fanMinOnTime
                    		fanSet = true
                		} else if (atomicState.fanCommand != null) {
                   			stat."${atomicState.fanCommand}"()				// set fan on/auto AFTER changing the program, because we are overriding the program's setting
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
                            sendMessage("I set ${stat.displayName} to Hold: ${atomicState.programParam}${timeStr}, then I turned off the HVAC as requested")
                        } else {
							sendMessage("I set ${stat.displayName} to Hold: ${atomicState.programParam}${timeStr}${fanSet?' with the requested fan settings':''}")
                        }
               		}
            	} // else { assert atomicState.programParam == null; must have been 'Resume Program' or an old 'Cancel Vacation'  }
            }
		}
    }
	if (settings.doneSwitches) {
		changeSwitches()
	}
    return true
}

// returns the holdType keyword, OR the number of hours to hold
// precedence: 1. this SmartApp's preferences, 2. Parent settings.holdType, 3. indefinite (must specify to use the thermostat setting)
String whatHoldType(statDevice) {
    def theHoldType = settings.holdType
    def sendHoldType = null
    def parentHoldType = getParentSetting('holdType')
    if ((settings.holdType == null) || (settings.holdType == "Ecobee Manager Setting") || (settings.holdType == 'Parent Ecobee (Connect) Setting')) {
        if ((parentHoldType == null) || (parentHoldType == '')) {	// default for Ecobee (Connect) is permanent hold (legacy)
        	LOG('Using holdType indefinite',2,null,'info')
        	return 'indefinite'
        } else if (parentHoldType != 'Thermostat Setting') {
        	theHoldType = parentHoldType
        }
    }
    
    def parentHoldHours = getParentSetting('holdHours')
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
            if (settings.holdHours && settings.holdHours.isNumber()) {
            	sendHoldType = settings.holdHours
            } else if ((parentHoldType == 'Specified Hours') && (parentHoldHours != null)) {
            	sendHoldType = parentHoldHours
            } else if ( parentHoldType == '2 Hours') {
            	sendHoldType = 2
            } else if ( parentHoldType == '4 Hours') {
            	sendHoldType = 4            
            } else {
            	sendHoldType = 2
            }
            break;
        case 'Thermostat Setting':
       		String statHoldType = ST ? statDevice.currentValue('statHoldAction') : statDevice.currentValue('statHoldAction', true)
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
				case '':
                default :
                	sendHoldType = 'indefinite'
                    break;
           }
    }
    if (sendHoldType) {
    	LOG("Using holdType ${sendHoldType.isNumber()?'holdHours ('+sendHoldType.toString()+')':sendHoldType}",2,null,'info')
        return sendHoldType
    } else {
    	LOG("Couldn't determine holdType, returning indefinite",1,null,'error')
        return 'indefinite'
    }
}

// Helper Functions
// get the combined set of Ecobee Programs applicable for these thermostats
def getThermostatPrograms() {
	def programs
	if (myThermostats?.size() > 0) {
		myThermostats.each { stat ->
        	def progs = []
        	String cl = stat.currentValue('climatesList')
    			if (cl && (cl != '[]')) {
        		progs = cl[1..-2].tokenize(', ')
        	} else {
    			String pl = settings?.theThermostat?.currentValue('programsList')
        		progs = pl ? new JsonSlurper().parseText(pl) : []
        	}
			if (!programs) {
				if (progs) programs = progs
			} else {
				if (progs) programs = programs.intersect(progs)
			}
        }
	} 
	if (!programs) programs =  ['Away', 'Home', 'Sleep']
    LOG("getThermostatPrograms: returning ${programs}", 4, null, 'info')
    return programs.sort(false)
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

void sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "trace")
    
    	// Always send to Hello Home / Location Event log
	if (ST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
    
    if (!settings.notify) return
    
    if (settings.noOffNotify && myThermostats*.currentValue('thermostatMode').contains('off')) {
    	LOG("Skipping notification because all thermostats are Off and Off Notifications are disabled",2, null, "info")
        return
    }
    
    String msg = "${atomicState.appDisplayName} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
    boolean addFrom = true
    if (ST) {
        if (settings.notifiers) {
            if (settings.echoAnnouncements) {
                List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                List notEcho = echo ? settings.notifiers - echo : settings.notifiers

                // If we have multiple Echo Speak device targets, get them all to speak at once
                List echoDeviceObjs = []
                if (echo) {
                    if(echo?.size() > 1) {
                        echo?.each { 
                            String deviceType = it.currentValue('deviceType') as String
                            String serialNumber = it.deviceNetworkId.toString().split(/\|/).last() as String
                            echoDeviceObjs?.push([deviceTypeId: deviceType, deviceSerialNumber: serialNumber]) 
                        }
                    }
                    //Announcement Command Logic
                    if((echo.size() > 1) && echoDeviceObjs && echoDeviceObjs?.size()) {
                        //NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
                        def devJson = new groovy.json.JsonOutput().toJson(echoDeviceObjs)
                        echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:atomicState.appDisplayName), echoDeviceObjs)	// , changeVol, restoreVol) }
                    } else if (echo.size() == 1) {
                        echo.playAnnouncement(msg, (msgPrefix?:atomicState.appDisplayName))
                    } else {
                        notEcho*.deviceNotification(msg)
                    }
                } else {
                    settings.notifiers*.deviceNotification(msg)
                }
            } else {
                settings.notifiers*.deviceNotification(msg)
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
                sendSmsMessage(settings.phone.trim(), msg)				// Only to SMS contact
            }
        } 
        if (settings.pushNotify) {
            LOG("Sending Push to everyone", 3, null, 'warn')
            sendPushMessage(msg)										// Push to everyone
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
    } else {		// HE
        if (settings.notifiers) {
            if (settings.echoAnnouncements) {
                List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                List notEcho = echo ? settings.notifiers - echo : settings.notifiers

                // If we have multiple Echo Speak device targets, get them all to speak at once
                List echoDeviceObjs = []
                if (echo) {
                    if(echo?.size() > 1) {
                        echo?.each { 
                            String deviceType = it.currentValue('deviceType') as String
                            String serialNumber = it.deviceNetworkId.toString().split(/\|/).last() as String
                            echoDeviceObjs?.push([deviceTypeId: deviceType, deviceSerialNumber: serialNumber]) 
                        }
                    }
                    //Announcement Command Logic
                    if((echo.size() > 1) && echoDeviceObjs && echoDeviceObjs?.size()) {
                        //NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
                        def devJson = new groovy.json.JsonOutput().toJson(echoDeviceObjs)
                        echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:atomicState.appDisplayName), echoDeviceObjs)	// , changeVol, restoreVol) }
                    } else if (echo.size() == 1) {
                        echo.playAnnouncement(msg, (msgPrefix?:atomicState.appDisplayName))
                    } else {
                        notEcho*.deviceNotification(msg)
                    }
                } else {
                    settings.notifiers*.deviceNotification(msg)
                }
            } else {
                settings.notifiers*.deviceNotification(msg)
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

void updateMyLabel() {
	//boolean ST = atomicState.isST
    
	String flag = ST ? ' (paused)' : '<span '
	
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (myLabel.contains(flag)) {
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	if (settings.tempDisable) {
		def newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
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
    return theFanModes*.capitalize().sort(false)
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
String pageTitle 	(String txt) 	{ return HE ? getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') : txt }
String pageTitleOld	(String txt)	{ return HE ? getFormat('header-ecobee','<h2>'+txt+'</h2>') 	: txt }
String sectionTitle	(String txt) 	{ return HE ? getTheSectionBeeLogo() + getFormat('header-nobee','<h3><b>&nbsp;&nbsp;'+txt+'</b></h3>')	: txt }
String smallerTitle	(String txt) 	{ return txt ? (HE ? '<h3><b>'+txt+'</b></h3>' 				: txt) : '' }
String sampleTitle	(String txt) 	{ return HE ? '<b><i>'+txt+'<i></b>'			 				: txt }
String inputTitle	(String txt) 	{ return HE ? '<b>'+txt+'</b>'								: txt }
String getWarningText()				{ return HE ? "<span style='color:red'><b>WARNING: </b></span>"	: "WARNING: " }
String getFormat(type, myText=""){
	switch(type) {
		case "header-ecobee":
			return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theBee}${myText}</div>"
			break;
		case "header-nobee":
			return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
			break;
    	case "line":
			return HE ? "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>" : "-----------------------------------------------"
			break;
		case "title":
			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
			break;
		case "warning":
			return HE ? "<span style='color:red'><b>WARNING: </b><i></span>${myText}</i>" : "WARNING: ${myText}"
			break;
		case "note":
			return HE ? "<b>NOTE: </b>${myText}" : "NOTE:<br>${myText}"
			break;
		default:
			return myText
			break;
	}
}
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019-2020, Barry A. Burke (storageanarchy@gmail.com)
String getPlatform() { return ((hubitat?.device?.HubAction == null) ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST() {
	if (ST == null) {
    	// ST = physicalgraph?.device?.HubAction ? true : false // this no longer compiles on Hubitat for some reason
        if (HE == null) HE = getIsHE()
        ST = !HE
    }
    return ST    
}
boolean getIsHE() {
	if (HE == null) {
    	HE = hubitat?.device?.HubAction ? true : false
        if (ST == null) ST = !HE
    }
    return HE
}

String getHubPlatform() {
    hubPlatform = getIsST() ? "SmartThings" : "Hubitat"
	return hubPlatform
}
boolean getIsSTHub() { return isST }					// if (isSTHub) ...
boolean getIsHEHub() { return isHE }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return ST ? parent?.settings?."${settingName}" : parent?."${settingName}"
}
@Field String  hubPlatform 	= getHubPlatform()
@Field boolean ST 			= getIsST()
@Field boolean HE 			= getIsHE()
