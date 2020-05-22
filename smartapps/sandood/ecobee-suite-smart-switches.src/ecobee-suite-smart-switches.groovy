/**
 *  ecobee Suite Smart Switches
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
 *	1.8.06 - Updated formatting; added Do Not Disturb Modes & Time window
 *	1.8.07 - Fixed reverseActions()
 *	1.8.08 - Miscellaneous updates & fixes
 *	1.8.09 - Added status to app.label
 *	1.8.10 - Option to treat 'fan only' as 'idle'
 *	1.8.11 - Fix cl/dl/tl mishmash
 *	1.8.12 - Added fanControl support, changed name to Smart Switch/Dimmer/Fan
 *	1.8.13 - Only "deactivate" during selected days and outside of "disabled" time window
 */
import groovy.transform.Field

String getVersionNum()		{ return "1.8.13" }
String getVersionLabel() 	{ return "Ecobee Suite Smart Switch/Dimmer/Fan Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Smart Switches",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAutomates ${isST?'SmartThings':'Hubitat'}-controlled switches, dimmers, fans and generic vents based on thermostat operating state",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-switches.src/ecobee-suite-smart-switches.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-smart-switches-sa",
	singleInstance: 	false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
	String defaultName = "Smart Switch/Dimmer/Fan/Vent"
    
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
    		section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
        	
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This handy utility Helper automates changing of (real or virtual) switches, dimmers and generic vents based upon Operating State changes of one or more Ecobee Suite Thermostats. "+
                          "For example, it will turn off (or on) an attic fan while the HVAC system is cooling.")
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
                def opts = [' (paused', ' (Cool', ' (Heat', ' (Fan', ' (Idle', ' (Pend', ' (Vent']
				String flag
                opts.each {
                	if (!flag && app.label.contains(it)) flag = it
				}
				if (flag) {
					if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains(flag)) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
                        String myLabel = app.label.substring(0, app.label.indexOf(flag))
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
				input(name: "theThermostats", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select Ecobee Suite Thermostat(s)"), 
					  multiple: true, required: true, submitOnChange: true)
            }
		}
        
        if (!settings.tempDisable && (settings?.theThermostats?.size() > 0)) {
        
        	section(title: sectionTitle("Trigger")) {
        		input(name: "theOpState", type: "enum", title: inputTitle("When ${settings?.theThermostats?.size()>1?'any ':'the '} thermostat changes to one of these Operating States"), 
					  options:['heating','cooling','fan only','idle','pending cool','pending heat','vent economizer'], required: true, multiple: true, submitOnChange: true, width: 6)
				// mode(title: inputTitle("Enable only for specific mode(s)"))
                input(name: "fanOnly", type: "bool", title: inputTitle("Deactivate when 'fan only'?"), defaultValue: !settings?.theOpState?.contains('fan only'), submitOnChange: true, width: 6)
        	}
       
        	section(title: sectionTitle("Actions")) {
				paragraph(smallerTitle("Turn on..."))
				input(name: 'theOnSwitches', type: 'capability.switch', title: inputTitle("Turn on these switches"), multiple: true, required: false, submitOnChange: true)
          		input(name: 'theOnDimmers', type: 'capability.switchLevel', title: inputTitle("Set level on these dimmers"), multiple: true, required: false, submitOnChange: true)
          		if (settings.theOnDimmers) {
					input(name: 'onDimmerLevel', type: 'number', title: inputTitle("Set dimmers to level...")+' (0-100)', range: "0..100", required: true, width: 4)
				}
                input(name: 'theOnFans', type: 'capability.fanControl', title: inputTitle("Set speed on these fans"), multiple: true, required: false, submitOnChange: true, width: 4, hideWhenEmpty: true)
                if (settings.theOnFans) {
                	input(name: 'onFanSpeed', type: 'enum', title: inputTitle("Set fan speed to..."), options: ["low","medium low","medium","medium high","high","on","off","auto"], required: true, defaultValue: "low",
                    	  submitOnChange: true, width: 4)
                    if (!settings?.onFanSpeed) { app.updateSetting('onFanSpeed', 'low'); settings.onFanSpeed = 'low'; }
                }
                
				paragraph(smallerTitle("Turn off..."))
          		input(name: 'theOffSwitches', type: 'capability.switch', title: inputTitle('Turn off these switches'), multiple: true, required: false, submitOnChange: true)
          		input(name:	'theOffDimmers', type: 'capability.switchLevel', title: inputTitle("Set level on these dimmers"), multiple: true, required: false, submitOnChange: true, width: 4)
          		if (settings.theOffDimmers) {
					input(name: 'offDimmerLevel', type: 'number', title: inputTitle("Set dimmers to level...")+' (0-100)', range: "0..100", required: true, width: 4)
				}
                input(name: 'theOffFans', type: 'capability.fanControl', title: inputTitle("Set speed on these fans"), multiple: true, required: false, submitOnChange: true, width: 4, hideWhenEmpty: true)
                if (settings.theOffFans) {
                	input(name: 'offFanSpeed', type: 'enum', title: inputTitle("Set fan speed to..."), options: ["low","medium low","medium","medium high","high","on","off","auto"], required: true, defaultValue: "off",
                    	  submitOnChange: true, width: 4)
                    if (!settings?.offFanSpeed) { app.updateSetting('offFanSpeed', 'off'); settings.offFanSpeed = 'off'; }
                }
                
            	if (!settings.theOpState?.contains('idle')) {
            		input(name: 'reverseOnIdle', type: 'bool', title: inputTitle("Reverse above Actions when ${settings.theThermostats?.size()>1?'all thermostats return':'thermostat returns'} to 'idle'?"), 
						  defaultValue: false, submitOnChange: true, width: 6)
            	}
                if (settings.reverseOnIdle || settings.reverseAtFromTime) {
                    //if (HE) paragraph("", width: 6)
                    input(name: "reversePreserve", type: 'bool', title: inputTitle("Preserve previous state when reversing actions?"), defaultValue: false, submitOnChange: true, width: 6)
                }
            }
            
            section(title: sectionTitle("Conditions")) {
        		input(name: "actionDays", type: "enum", title: inputTitle("Select Days of the Week to run the above Actions"), required: true, multiple: true, submitOnChange: true, width: 4,
                	options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"])
				input(name: "fromTime", type: "time", title: inputTitle("Disable Actions daily between${HE?'<br>':''}From Time (optional)"), required: false, submitOnChange: true, width: 4)
				input(name: "toTime", type: "time", title: inputTitle("${HE?'<br>':''}...and To Time"), required: (settings.fromTime != null), submitOnChange: true, width: 4)
                def between = (((settings.fromTime != null) && (settings.toTime != null)) ? myTimeOfDayIsBetween(timeToday(settings.fromTime), timeToday(settings.toTime), new Date(), location.timeZone) : false)         				
                if (maximize) paragraph "Actions ${between?'would NOT':'would'} run right now (Hint: set From & To to the same time to block Actions at any time).${settings.reverseOnIdle?' Actions will be reversed on \'idle\' at ANY time.':''}"
                if (settings.fromTime) {
                	input(name: 'reverseAtFromTime', type: 'bool', title: inputTitle("${settings.reverseOnIdle?'Also r':'R'}everse above Actions at From time daily?"), defaultValue: true, 
                    	  submitOnChange: true, width: 4)
                    def theDays = (settings?.actionDays?.size() == 7) ? 'daily' : 'on ' + settings.actionDays.toString()[1..-2].replace('",',', ').replace('"','')
                    if (settings.actionDays && maximize) paragraph "Actions ${settings.reverseAtFromTime?'will':'will NOT'} be reversed at ${getShortTime(settings?.fromTime)} ${theDays}"
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
            section("") {
        		href(name: "hrefNotRequired", description: "Tap to donate via PayPal", required: false, style: "external", image: "https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/paypal-green.png",
                	 url: "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=MJQD5NGVHYENY&currency_code=USD&source=url", title: "Your donation is appreciated!" )
    		}
        	section(getVersionLabel().replace('er, v',"er\nV")+"\n\nCopyright \u00a9 2017-2020 Barry A. Burke\nAll rights reserved.") {}
        } else {
        	section() {
        		paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br><small>Copyright \u00a9 2017-2020 Barry A. Burke - All rights reserved.</small><br>"+
						  "<small><i>Your</i>&nbsp;</small><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=MJQD5NGVHYENY&currency_code=USD&source=url' target='_blank'>" + 
						  "<img src='https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/paypal-green.png' border='0' width='64' alt='PayPal Logo' title='Please consider donating via PayPal!'></a>" +
						  "<small><i>donation is appreciated!</i></small></div>" )
            }
		}
    }
}

// Main functions
void installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
    initialize()
}
void updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    initialize()
}
void uninstalled() {
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
    if (!atomicState.thermostatOpState) atomicState.thermostatOpState = theThermostats[0].currentValue('thermostatOperatingState')
	if (!atomicState.currentAction) atomicState.currentAction = ' - waiting...'
	updateMyLabel()
	
    atomicState.scheduled = false
    // Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
    if (settings.debugOff) log.info "log.debug() logging disabled"
	
    //if (settings.preserve) atomicState.priorState = 
	subscribe(settings.theThermostats, 'thermostatOperatingState', opStateHandler)
    if (settings.fromTime && settings.reverseAtFromTime) {
    	// schedule the daily reversal...
        schedule(settings.fromTime, reverseActionsScheduled)
    }
    return true
}

def opStateHandler(evt) {
	LOG("${evt.name}: ${evt.value}",2,null,'info')
	//boolean ST = atomicState.isST

	String oldOpState = atomicState.thermostatOpState
	String newOpState = evt.value
	atomicState.thermostatOpState = evt.value
	if (!settings.theOpState.contains(oldOpState) && !settings.theOpState.contains(newOpState)) {
		atomicState.currentAction = ' - unchanged'
		updateMyLabel()
		return
	}
    atomicState.currentAction = ' - unchanged'
    
    // Note that this ensures that things are both "activated" AND "deactivated" only on approriate days and during the "enabled" time window
    // Thus if the fan is turned on during the "disabled" time window, it will stay on
    if (!dayCheck()) {
        LOG("Not configured to run Actions today, ignoring", 2, null, 'info')
        updateMyLabel()
        return
    }
    def between = ((settings.fromTime != null) && (settings.toTime != null)) ? myTimeOfDayIsBetween(timeToday(settings.fromTime), timeToday(settings.toTime), new Date(), location.timeZone) : false

    if (between) {
        LOG('Not running Actions because the current time is within the disabled time window', 2, null, 'info')
        updateMyLabel()
        return
    }
    
	if ((evt.value == 'idle') || (settings.fanOnly && (evt.value == 'fan only'))) {
    	if (settings.reverseOnIdle) {
        	def isReallyIdle = true
        	if (settings.theThermostats.size() > 1) {
            	settings.theThermostats.each { 
					String ncTos = ST ? it.currentValue('thermostatOperatingState') : it.currentValue('thermostatOperatingState', true)
					if ((ncTos != 'idle') && (!settings.fanOnly || (ncTos != 'fan only')))  isReallyIdle = false 
				}
            }
            if (isReallyIdle) {	
				reverseActions()
            }
            updateMyLabel()
            return
        }
    } else if (settings.theOpState.contains(evt.value)) {
        HashMap priorState = atomicState.priorState as HashMap
        if (!priorState) priorState = [:] as HashMap
        
        // Turn on the "on" switches
        if (settings.theOnSwitches) {
			settings.theOnSwitches.each { theSwitch ->
            	String cs = theSwitch.currentSwitch
            	if (settings.reversePreserve) {    	
            		String dni = theSwitch.device.deviceNetworkId as String
                    if (!priorState[dni]) priorState[dni] = []
                    priorState[dni] << [action: 'on', type: 'switch', value: cs]
                }
                if (cs != 'on') { 
                   	LOG("Turning on ${theSwitch.displayName}",2,null,'info')
                   	theSwitch.on() 
                } else {
                  	LOG("${theSwitch.displayName} was already on",2,null,'info')
                }
            }
            atomicState.currentAction = ' - activated'
        }
        
        // Turn on the "on" dimmers
        if (settings.theOnDimmers) {
            settings.theOnDimmers.each { dimmer ->
            	def cl = dimmer.currentLevel
                String cs = dimmer.currentSwitch
            	if (settings.reversePreserve) {
                	String dni = dimmer.device.deviceNetworkId
                    if (!priorState[dni]) priorState[dni] = []
                    priorState[dni] << [action: 'on', type: 'dimmer', value: cl]
                    priorState[dni] << [action: 'on', type: 'switch', value: cs]
                }
                def tl = settings.onDimmerLevel?:99
                if (cl != tl) {
                	dimmer.setLevel(tl)
                    LOG("Setting ${dimmer.displayName} to ${tl}%",2,null,'info')
                } else {
                	LOG("${dimmer.displayName} was already at ${tl}%",2,null,'info')
                }
                if (cs != 'on') {
                	LOG("Turning on ${dimmer.displayName}",2,null,'info')
                	dimmer.on()
                }
            }
            atomicState.currentAction = ' - activated'
        }
        
        // Turn on the "on" fans
        if (settings.theOnFans) {
            settings.theOnFans.each { fan ->
            	String fs = fan.currentSpeed
                String cs = fan.getSupportedCommands().contains("on") ? fan.currentSwitch : ""
            	if (settings.reversePreserve) {
                	String dni = fan.device.deviceNetworkId
                    if (!priorState[dni]) priorState[dni] = []
                    priorState[dni] << [action: 'on', type: 'speed', value: fs]
                    priorState[dni] << [action: 'on', type: 'switch', value: cs]
                }
                String ts = settings.onFanSpeed?:"low"
                if (fs != ts) {
                	fan.setSpeed(ts)
                    LOG("Setting ${fan.displayName} to ${ts}",2,null,'info')
                } else {
                	LOG("${fan.displayName} was already set to ${ts}",2,null,'info')
                }
                if (cs && (cs != 'on')) {
                	LOG("Turning on ${fan.displayName}",2,null,'info')
                	fan.on()
                }
            }
            atomicState.currentAction = ' - activated'
        }
        
        // Turn off the "off" switches
        if (settings.theOffSwitches) {
			settings.theOffSwitches.each { theSwitch ->
            	String cs = theSwitch.currentSwitch
            	if (settings.reversePreserve) {    	
            		String dni = theSwitch.device.deviceNetworkId as String
                    if (!priorState[dni]) priorState[dni] = []
                    priorState[dni] << [action: 'off', type: 'switch', value: cs]
                }
                if (cs != 'off') { 
                   	LOG("Turning off ${theSwitch.displayName}",2,null,'info')
                   	theSwitch.on() 
                } else {
                  	LOG("${theSwitch.displayName} was already off",2,null,'info')
                }
            }
            atomicState.currentAction = ' - activated'
        }
        
        // Turn off the "off" dimmers
        if (settings.theOffDimmers) {
            settings.theOffDimmers.each { dimmer ->
            	def cl = dimmer.currentLevel
                String cs = dimmer.currentSwitch
            	if (settings.reversePreserve) {
                	String dni = dimmer.device.deviceNetworkId
                    if (!priorState[dni]) priorState[dni] = []
                    priorState[dni] << [action: 'off', type: 'dimmer', value: cl]
                    priorState[dni] << [action: 'off', type: 'switch', value: cs]
                }
                def tl = settings.offDimmerLevel?:0
                if (tl != 0) {
                	// we're just turning down the dimmer
                	if (cl != tl) {
                		dimmer.setLevel(tl)
                    	LOG("Setting ${dimmer.displayName} to ${tl}%",2,null,'info')
                	} else {
                		LOG("${dimmer.displayName} was already at ${tl}%",2,null,'info')
                	}
                    if (dimmer.currentSwitch != 'on') {
                		LOG("Turning on ${dimmer.displayName}",2,null,'info')
                		dimmer.on()
                	}
                } else if (cl != 0) {
                	if (dimmer.currentSwitch != 'off') {
                		LOG("Turning off ${dimmer.displayName}",2,null,'info')
                        dimmer.setLevel(0)
                		dimmer.off()
                    }
                }
            }
            atomicState.currentAction = ' - activated'
        }
        
        // Turn off the "off" fans
        if (settings.theOffFans) {
            settings.theOffFans.each { fan ->
            	String fs = fan.currentSpeed
                String cs = fan.getSupportedCommands().contains("off") ? fan.currentSwitch : ""
            	if (settings.reversePreserve) {
                	String dni = fan.device.deviceNetworkId
                    if (!priorState[dni]) priorState[dni] = []
                    priorState[dni] << [action: 'off', type: 'speed', value: fs]
                    priorState[dni] << [action: 'off', type: 'switch', value: cs]
                }
                String ts = settings.offFanSpeed?:"off"
                if (fs != ts) {
                	fan.setSpeed(ts)
                    LOG("Setting ${fan.displayName} to ${ts}",2,null,'info')
                } else {
                	LOG("${fan.displayName} was already set to ${ts}",2,null,'info')
                }
                if (cs && (cs != 'off')) {
                	LOG("Turning off ${fan.displayName}",2,null,'info')
                	fan.off()
                }
            }
            atomicState.currentAction = ' - activated'
        }
        atomicState.priorState = priorState
    }
    updateMyLabel()
}
void reverseActionsScheduled() {
	if (dayCheck) reverseActions()
}
void reverseActions() {
	Map priorState = settings.reversePreserve ? atomicState.priorState : [:]
	LOG("reverseActions() - ${priorState}", 3, null, debug)
    
    // Turn on the "off" fans
    if (settings.theOffFans) {
    	settings.theOffFans.each { fan ->
        	String cs = fan.getSupportedCommands().contains("on") ? fan.currentSwitch : ""
            String fs = fan.currentSpeed
        	if (settings.reversePreserve) {
            	String sw
                String sp
            	String dni = fan.device.deviceNetworkId
                if (priorState && priorState[dni]) priorState[dni].each { saved ->
                	if (saved.action == 'off') {
                		if (saved.type == 'switch') {
                           	sw = saved.value
                        } else if (saved.type == 'speed') {
                           	sp = saved.value
                        }
                    }
                }
                LOG("Returning ${fan.displayName} to prior state (${sw} @ ${sp})",2,null,'info')
                if (!fs || (sp != fs)) fan.setSpeed(sp)
				if (cs && (cs != sw)) fan."${sw}"()
            } else {
            	def sp = settings.onFanSpeed?:"low"
                if (!fs || (fs != sp)) {
                	fan.setSpeed(sp)
                    LOG("Setting ${fan.displayName} to ${sp}",2,null,'info')
                } else {
                	LOG("${fan.displayName} was already set to ${sp}",2,null,'info')
                }
                if (cs && (cs != 'on')) {
                	LOG("Turning on ${fan.displayName}",2,null,'info')
                	fan.on()
                }
            }
        }
        atomicState.currentAction = ' - deactivated'
    }
    
    // Turn on the "off" dimmers
	if (settings.theOffDimmers) {
    	settings.theOffDimmers.each { dimmer ->
        	String cs = dimmer.currentSwitch
            def cl = dimmer.currentLevel
        	if (settings.reversePreserve) {
            	String sw
                def lv
            	String dni = dimmer.device.deviceNetworkId
                if (priorState && priorState[dni]) priorState[dni].each { saved ->
                	if (saved.action == 'off') {
                		if (saved.type == 'switch') {
                           	sw = saved.value
                        } else if (saved.type == 'dimmer') {
                           	lv = saved.value
                        }
                    }
                }
                LOG("Returning ${dimmer.displayName} to prior state (${sw} @ ${lv}%)",2,null,'info')
                if (lv && (lv != cl)) dimmer.setLevel(lv)
                if (cs && (cs != sw)) dimmer."${sw}"()
            } else {
            	def tl = settings.onDimmerLevel?:99
                if (cl) {
                    if (cl != tl) {
                        dimmer.setLevel(tl)
                        LOG("Setting ${dimmer.displayName} to ${tl}%",2,null,'info')
                    } else {
                        LOG("${dimmer.displayName} was already at ${tl}%",2,null,'info')
                    }
                }
                if (cs && (cs != 'on')) {
                	LOG("Turning on ${dimmer.displayName}",2,null,'info')
                	dimmer.on()
                }
            }
        }
        atomicState.currentAction = ' - deactivated'
    }
    
    // turn on the "off" switches
    if (settings.theOffSwitches) {
        settings.theOffSwitches.each { theSwitch ->
            String cs = theSwitch.currentSwitch
            if (settings.reversePreserve) {    	
                String dni = theSwitch.device.deviceNetworkId as String
                if (priorState && priorState[dni]) priorState[dni].each { saved ->
            		if ((saved.action == 'off') && (saved.type == 'switch')) {
                    	String sw = saved.value
                        if (cs != sw) {
                        	theSwitch."${sw}()"
                            LOG("Returning ${theSwitch.displayName} to prior state (${sw})",2,null,'info')
                        } else {
                        	LOG("${theSwitch.displayName} is already ${sw}",2,null,'info')
                    	}
                    }
                }
            } else {
            	if (cs != 'off') { 
                	LOG("Turning on ${theSwitch.displayName}",2,null,'info')
                	theSwitch.on() 
            	} else {
                	LOG("${theSwitch.displayName} was already on",2,null,'info')
                }
            }
        }
        atomicState.currentAction = ' - deactivated'
    }
        
    // turn off the "on" fans
    if (settings.theOnFans) {
    	settings.theOnFans.each { fan ->
        	String cs = fan.getSupportedCommands().contains("off") ? fan.currentSwitch : ""
            String fs = fan.currentSpeed
        	if (settings.reversePreserve) {
            	String dni = fan.device.deviceNetworkId
				String sw
                String sp
                if (priorState && priorState[dni]) priorState[dni].each { saved ->
                	if (saved.action == 'on') {
                		if (saved.type == 'switch') {
                           	sw = saved.value
                        } else if (saved.type == 'speed') {
                           	sp = saved.value
                        }
                    }
                }
                LOG("Returning ${fan.displayName} to prior state (${sw} @ ${sp})",2,null,'info')
                if (fs && (fs != sp)) fan.setSpeed(sp)
                if (cs && (cs != sw)) fan."${sw}"()
            } else {
            	String sp = settings.offFanSpeed?:"off"
                if (fs) {
                	if (fs != sp) {
                		fan.setSpeed(sp)
                    	LOG("Setting ${fan.displayName} to ${sp}",2,null,'info')
                	} else {
                		LOG("${fan.displayName} was already set to ${sp}",2,null,'info')
                	}
                }
                if (cs) {
                	if (cs != 'off') {
                		LOG("Turning off ${fan.displayName}",2,null,'info')
                		fan.off()
                	} else {
                    	LOG("${fan.displayName} was already off",2,null,'info')
                	}
                }
            }
        }
        atomicState.currentAction = ' - deactivated'
    }
    
    // turn off the "on" dimmers
    if (settings.theOnDimmers) {
    	settings.theOnDimmers.each { dimmer ->
        	String cs = dimmer.currentSwitch
            def cl = dimmer.currentLevel
        	if (settings.reversePreserve) {
            	String dni = dimmer.device.deviceNetworkId
				String sw
                def lv
                if (priorState && priorState[dni]) priorState[dni].each { saved ->
                	if (saved.action == 'on') {
                		if (saved.type == 'switch') {
                           	sw = saved.value
                        } else if (saved.type == 'dimmer') {
                           	lv = saved.value
                        }
                    }
                }
                LOG("Returning ${dimmer.displayName} to prior state (${sw} @ ${lv}%)",2,null,'info')
                if (lv && (lv != cl)) dimmer.setLevel(lv)
                if (cs && (cs != sw)) dimmer."${sw}"()
            } else {
            	def tl = settings.offDimmerLevel?:0
                if (cl != null) {
                    if (cl != tl) {
                        dimmer.setLevel(tl)
                        LOG("Setting ${dimmer.displayName} to ${tl}%",2,null,'info')
                    } else {
                        LOG("${dimmer.displayName} was already at ${tl}%",2,null,'info')
                    }
                }
                if (cs) {
                    if (cs != 'off') {
                        LOG("Turning off ${dimmer.displayName}",2,null,'info')
                        dimmer.off()
                    } else {
                        LOG("${dimmer.displayName} was already off",2,null,'info')
                    }
                }
            }
        }
        atomicState.currentAction = ' - deactivated'
    }
    
    // turn off the "on" switches
    if (settings.theOnSwitches) {
        settings.theOnSwitches.each { theSwitch ->
            String cs = theSwitch.currentSwitch
            if (settings.reversePreserve) {    	
                String dni = theSwitch.device.deviceNetworkId as String
				if (priorState && priorState[dni]) priorState[dni].each { saved ->
					if ((saved.action == 'on') && (saved.type == 'switch')) {
						String sw = saved.value
						if (cs != sw) {
							theSwitch."${sw}"()
							LOG("Returning ${theSwitch.displayName} to prior state (${sw})",2,null,'info')
						} else {
							LOG("${theSwitch.displayName} is already ${sw}",2,null,'info')
						}
					}					 
                }
            } else {
                if (cs != 'off') { 
                    LOG("Turning off ${theSwitch.displayName}",2,null,'info')
                    theSwitch.off() 
                } else {
                    LOG("${theSwitch.displayName} was already off",2,null,'info')
                }
            }
        }
        atomicState.currentAction = ' - deactivated'
    }
	atomicState.priorState = [:]
    // updateMyLabel() - caller will do the update
}

// SmartThings internal function format (Strings instead of Dates)
private myTimeOfDayIsBetween(String fromTime, String toTime, Date checkDate, String timeZone) {
log.debug "fromTime: ${fromTime}"
	return myTimeOfDayIsBetween(timeToday(fromTime), timeToday(toTime), checkDate, timeZone)
}

private myTimeOfDayIsBetween(Date fromDate, Date toDate, Date checkDate, timeZone)     {
	if (toDate == fromDate) {
		return false	// blocks the whole day
	} else if (toDate < fromDate) {
		if (checkDate.before(fromDate)) {
			fromDate = fromDate - 1
		} else {
			toDate = toDate + 1
		}
	}
    return (!checkDate.before(fromDate) && !checkDate.after(toDate))
}
private toDateTime( str ) {
	return timeToday( str )
}
def getShortTime( date ) {
	Date dt = timeToday( date )
	def df = new java.text.SimpleDateFormat("h:mm a")
    df.setTimeZone(location.timeZone)
    return df.format( dt )
}
private boolean dayCheck() {
    def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    return (actionDays.contains(day))
}
void updateMyLabel() {
    def opts = [' (paused', ' (Cool', ' (Heat', ' (Fan', ' (Idle', ' (Pend', ' (Vent']
	String flag
	if (ST) {
		opts.each {
			if (!flag && app.label.contains(it)) flag = it
		}
	} else {
		flag = '<span '
	}
    String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label ?: app.name
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	String newLabel
	if (settings.tempDisable) {
		newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		newLabel = myLabel
		if (settings.theThermostats?.size()) {
        	String thermostatOpState = ' (' + capitalizeAll(atomicState.thermostatOpState)
			String currentAction = atomicState.currentAction + ')'
            String color = (atomicState.thermostatOpState == 'idle') ? 'green' : 'blue'
			newLabel = newLabel + (HE ? '<span style="color:' + color + '">' + thermostatOpState + currentAction + '</span>' : thermostatOpState + currentAction)
		}
		if (app.label != newLabel) app.updateLabel(newLabel)
	}
}
String capitalizeAll(String str) {
    if (str == null || str.isEmpty()) {
        return str;
    }
    str.split(" ").collect{it.capitalize()}.join(" ")
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
// Helper Functions
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
String smallerTitle (String txt)	{ return txt ? (HE ? '<h3 style="color:#5BBD76"><b><u>'+txt+'</u></b></h3>'				: txt) : '' } // <hr style="background-color:#5BBD76;height:1px;width:52%;border:0;align:top">
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
@Field String  debug		= 'debug'
@Field String  error		= 'error'
@Field String  info			= 'info'
@Field String  trace		= 'trace'
@Field String  warn			= 'warn'
