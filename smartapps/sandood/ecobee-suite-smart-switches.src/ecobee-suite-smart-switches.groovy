/**
 *  ecobee Suite Smart Switches
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
 *	1.0.1  - Initial Release
 *	1.0.2  - Added option to limit operation to certain SmartThings Modes
 *	1.0.3  - Updated settings and TempDisable handling
 *	1.2.0  - Sync version number with new holdHours/holdAction support
 *	1.2.1  - Protect agsinst LOG type errors
 *	1.3.0  - Major Release: renamed and moved to "sandood" namespace
 *	1.4.0  - Renamed parent Ecobee Suite Manager
 *	1.4.01 - Updated description
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Allow Ecobee Suite Thermostats only
 *	1.6.00 - Release number synchronization
 *	1.6.10 - Resync for parent-based reservations
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - nonCached currentValue() for HE
 *	1.7.02 - Fixing private method issue caused by grails
 *  1.7.03 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.04 - Optimized isST/isHE, added Global Pause
 *	1.7.05 - Added option to disable local display of log.debug() logs
 *	1.7.06 - Added optional daily "Actions disabled" time window
 *	1.7.07 - Fixed between time calculation
 */
 
String getVersionNum() { return "1.7.07" }
String getVersionLabel() { return "Ecobee Suite Smart Switch/Dimmer/Vent Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: "ecobee Suite Smart Switches",
	namespace: "sandood",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAutomates ${isST?'SmartThings':'Hubitat'}-controlled switches, dimmers and generic vents based on thermostat operating state",
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
	boolean ST = isST
	boolean HE = !ST
	
	dynamicPage(name: "mainPage", title: (HE?'<b>':'') + "${getVersionLabel()}" + (HE?'</b>':''), uninstall: true, install: true) {
    	section(title: "") {
        	String defaultLabel = "Smart Switch/Dimmer/Vent"
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
            if (settings.tempDisable) {
            	paragraph "WARNING: Temporarily Paused - re-enable below."
            } else {
				input(name: "theThermostats", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: "Monitor these Ecobee thermostat(s) for operating state changes:", 
					  multiple: true, required: true, submitOnChange: true)
            }
		}
        
        if (!settings.tempDisable && (settings?.theThermostats?.size() > 0)) {
        
        	section(title: (HE?'<b>':'') + "Smart Switches: Operating State" + (HE?'</b>':'')) {
        		input(name: "theOpState", type: "enum", title: "When ${settings?.theThermostats?settings.theThermostats.toString()[1..-2]:'thermostat'} changes to one of these Operating States", 
					  options:['heating','cooling','fan only','idle','pending cool','pending heat','vent economizer'], required: true, multiple: true, submitOnChange: true)
				// mode(title: "Enable only for specific mode(s)")
        	}
       
        	section(title: (HE?'<b>':'') + "Smart Switches: Actions" + (HE?'</b>':'')) {
				input(name: 'theOnSwitches', type: 'capability.switch', title: "Turn On these switches", multiple: true, required: false, submitOnChange: true)
          		input(name: 'theOnDimmers', type: 'capability.switchLevel', title: "Set level on these dimmers", multiple: true, required: false, submitOnChange: true)
          		if (settings.theOnDimmers) {
					input(name: 'onDimmerLevel', type: 'number', title: "Set dimmers to level...", range: "0..99", required: true)
				}
          		input(name: 'theOffSwitches', type: 'capability.switch', title: 'Turn Off these switches', multiple: true, required: false, submitOnChange: true)
          		input(name:	'theOffDimmers', type: 'capability.switchLevel', title: "Set level on these dimmers", multiple: true, required: false, submitOnChange: true)
          		if (settings.theOffDimmers) {
					input(name: 'offDimmerLevel', type: 'number', title: "Set dimmers to level...", range: "0..99", required: true)
				}
            	if (!settings.theOpState?.contains('idle')) {
            		input(name: 'reverseOnIdle', type: 'bool', title: "Reverse above Actions when ${settings.theThermostats?.size()>1?'all thermostats return':'thermostat returns'} to 'idle'?", 
						  defaultValue: false, submitOnChange: true)
            	}
            }
            
            section(title: (HE?'<b>':'') + "Smart Switches: Actions Schedule" + (HE?'</b>':'')) {
        		input(name: "actionDays", type: "enum", title: "Select Days of the Week to run the above Actions", required: true, multiple: true, submitOnChange: true,
                	options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"])
     		   	input(name: "fromTime", type: "time", title: "Disable Actions daily between From Time (optional)", required: false, submitOnChange: true)
        		input(name: "toTime", type: "time", title: "...and To Time", required: (settings.fromTime != null), submitOnChange: true)
                def between = (((settings.fromTime != null) && (settings.toTime != null)) ? myTimeOfDayIsBetween(timeToday(settings.fromTime), timeToday(settings.toTime), new Date(), location.timeZone) : false)         				
                paragraph "Actions ${between?'would NOT':'would'} run right now (Hint: set From & To to the same time to block Actions at any time).${settings.reverseOnIdle?' Actions will be reversed on \'idle\' at ANY time.':''}"
                if (settings.fromTime) {
                	input(name: 'reverseAtFromTime', type: 'bool', title: "${settings.reverseOnIdle?'Also r':'R'}everse above Actions at From time daily?", defaultValue: true, submitOnChange: true)
                    def theDays = (settings?.actionDays?.size() == 7) ? 'daily' : 'on ' + settings.actionDays.toString()[1..-2].replace('",',', ').replace('"','')
                    if (settings.actionDays) paragraph "Actions ${settings.reverseAtFromTime?'will':'will NOT'} be reversed at ${getShortTime(settings?.fromTime)} ${theDays}"
                }
			}
        }
		
		section(title: (HE?'<b>':'') + "Temporarily Disable?" + (HE?'</b>':'')) {
        	input(name: "tempDisable", title: "Pause this Helper?", type: "bool", required: false, description: "", submitOnChange: true)                   
        }
		section(title: "") {
			input(name: "debugOff", title: "Disable debug logging? ", type: "bool", required: false, defaultValue: false, submitOnChange: true)
		}        
        section (getVersionLabel()) {}
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
	updateMyLabel()
	
    atomicState.scheduled = false
    // Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
    if (settings.debugOff) log.info "log.debug() logging disabled"
	
	subscribe(settings.theThermostats, 'thermostatOperatingState', opStateHandler)
    if (settings.fromTime && settings.reverseAtFromTime) {
    	// schedule the daily reversal...
        schedule(settings.fromTime, reverseActionsScheduled)
    }
    return true
}

def opStateHandler(evt) {
	LOG("${evt.name}: ${evt.value}",2,null,'info')
	boolean ST = atomicState.isST

	if (evt.value == 'idle') {
    	if (settings.reverseOnIdle) {
        	def isReallyIdle = true
        	if (settings.theThermostats.size() > 1) {
            	settings.theThermostats.each { 
					String ncTos = ST ? it.currentValue('thermostatOperatingState') : it.currentValue('thermostatOperatingState', true)
					if (ncTos != 'idle') isReallyIdle = false }
            }
            if (isReallyIdle) {	
				reverseActions()
            }
            return
        }
    }
	if (settings.theOpState.contains(evt.value)) {
    	if (!dayCheck()) {
        	LOG("Not configured to run Actions today, ignoring", 2, null, 'info')
            return
        }
    	def between = ((settings.fromTime != null) && (settings.toTime != null)) ? myTimeOfDayIsBetween(timeToday(settings.fromTime), timeToday(settings.toTime), new Date(), location.timeZone) : false
        
        if (between) {
    		LOG('Not running Actions because the current time is within the disabled time window', 2, null, 'info')
        	return
    	}
        if (settings.theOnSwitches) {
        	LOG("Turning on ${settings.theOnSwitches*.displayName.toString()[1..-2]}",2,null,'info')
    		settings.theOnSwitches*.on()
        }
        if (settings.theOnDimmers) dimmersOn(settings.theOnDimmers)
        if (settings.theOffSwitches) {
        	LOG("Turning off ${settings.theOffSwitches*.displayName.toString()[1..-2]}",2,null,'info')
        	settings.theOffSwitches*.off()
        }
        if (settings.theOffDimmers) dimmersOff(settings.theOffDimmers)
    }
}
void reverseActionsScheduled() {
	if (dayCheck) reverseActions()
}
void reverseActions() {
    if (settings.theOnSwitches) {
        LOG("Turning off ${settings.theOnSwitches*.displayName.toString()[1..-2]}",2,null,'info')
        settings.theOnSwitches*.off()
    }
    if (settings.theOnDimmers) dimmersOff(settings.theOnDimmers)
    if (settings.theOffSwitches) {
        LOG("Turning on ${settings.theOffSwitches.displayName.toString()[1..-2]}",2,null,'info')
        settings.theOffSwitches*.on()
    }
    if (settings.theOffDimmers) dimmersOn(settings.theOffDimmers)
}

void dimmersOff( theDimmers ) {
	if (!theDimmers) return
    
	boolean changed = false
	def dimLevel = settings.offDimmerLevel?:0
    if (dimLevel == 0) {
      	if (settings.theDimmers*.currentSwitch.contains('on')) {
        	theDimmers*.off()
            LOG("Turning off ${theDimmers*.displayName.toString()[1..-2]}",3,null,'info')
        } else {
        	LOG("${theDimmers*.displayName.toString()[1..-2]} ${theDimmers.size()>1?'are':'is'} already off",3,null,'info')
        }
        return
    } else {
    	theDimmers.each {
        	if (it.currentSwitch == 'off') it.on()
        	if (it.currentLevel.toInteger() != dimLevel) {
        	   	changed = true
            	it.setLevel(dimLevel)	// make sure none of the vents are less than the specified minimum
            }
       	}
   	}
   
   	if (changed) {
       	LOG("Turning down ${theDimmers*.displayName.toString()[1..-2]} to ${dimLevel}%",3,null,'info')
  	} else {
       	LOG("${theDimmers*.displayName.toString()[1..-2]} ${theDimmers.size()>1?'are':'is'} already at ${dimLevel}%",3,null,'info')
    }
}

void dimmersOn( theDimmers ) {
	if (!theDimmers) return
    
    boolean changed = false
    if (theDimmers*.currentSwitch.contains('off')) {
    	theDimmers*.on()
        changed = true
    }
    def dimLevel = settings.onDimmerLevel?:99
    theDimmers.each {
    	if (it.currentLevel.toInteger() != dimLevel) {
    		it.setLevel(dimLevel)
        	changed = true
        }
    }
    if (changed) {
    	LOG("Set ${theDimmers*.displayName.toString()[1..-2]} to ${dimLevel}%",3,null,'info')
    } else {
    	LOG("${theDimmers*.displayName.toString()[1..-2]} ${theDimmers.size()>1?'are':'is'} already at ${dimLevel}%",3,null,'info')
    }
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
// Helper Functions
void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${atomicState.appDisplayName} ${message}"
    if (logType == null) logType = 'debug'
	if ((logType != 'debug') || (!settings.debugOff)) log."${logType}" message
	parent.LOG(msg, level, null, logType, event, displayEvent)
}

// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
//
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
