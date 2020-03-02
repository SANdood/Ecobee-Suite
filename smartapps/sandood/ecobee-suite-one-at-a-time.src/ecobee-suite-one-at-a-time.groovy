/**
 *  ecobee Suite <Helper Template>
 *
 *  Copyright 2020 Barry A. Burke
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
 *	1.8.00 - Initial release
 */
String getVersionNum()		{ return "1.8.00" }
String getVersionLabel() 	{ return "Ecobee Suite One at a Time Helper, version ${getVersionNum()} on ${getHubPlatform()}" }
import groovy.json.*

definition(
	name: 			"ecobee Suite One at a Time Helper",
	namespace: 		"sandood",
	author: 		"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 	"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\n",
	category: 		"Convenience",
	//parent: 		"sandood:Ecobee Suite Manager",
	iconUrl:		"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:		"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:		"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:		"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-humidity.src/ecobee-suite-one-at-a-time.groovy",
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
	boolean humidifierEnabled = false
	def hasHumidifier
    def cTemp		// current indoor temperature
    String unit = temperatureScale
    String defaultName = "One at a Time"
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
        	if (ST) {
				paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
            } else {
				paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
            }
			paragraph("For homes with multiple HVAC systems (or Zoned systems), this Helper limits to only 1 system cooling or heating at a time in order to minimize concurrent power demands. " +
            		  "It will turn off all the other HVAC systems whenever one of them is not idle, on a first come, first serve basis.")
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
            	String flag
    			def opts = [' (paused', ' (active', ' (inactive', '%)']
				opts.each {
					if (!flag && app.label.contains(it)) flag = it
				}
				if (flag) {
                	if ((atomicState?.appDisplayName != null) && (flag=='%)'? !atomicState?.appDisplayName.endsWith('%)') : !atomicState?.appDisplayName.contains(flag))) {
                    	app.updateLabel(atomicState.appDisplayName)
                    } else if (flag == '%)') {
                    	int i = app.label.reverse().indexOf('(') + 2
                    	String myLabel = app.label.take(app.label.size()-i)
                    	atomicState.appDisplayName = myLabel
                    	app.updateLabel(myLabel)
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
        		input(name: "theThermostats", type:"${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select two or more Ecobee Suite Thermostats"), 
					  required: true, multiple: true, submitOnChange: true)
				if (settings?.theThermostats) {
					if (settings.theThermostats.size() == 1) paragraph getFormat("warning", "You must select at least 2 Ecobee Suite Thermostsats...")
				}
            }
		}
        
        if (!settings?.tempDisable && settings?.theThermostats && (settings.theThermostats.size() > 1)) {
        	section(title: sectionTitle("Actions")) {
				input(name: "busyStates", type: "enum", title: inputTitle("When any Thermostat's Operating State becomes one of these..."), submitOnChange: true, required: true, width: 6, multiple: true,
                	  options: ["cooling": "Cooling", "heating": "Heating", "fan only": "Fan Only"], defaultValue: "cooling")
				if (HE) paragraph("", width: 6)
            	input(name: "hvacOff", type: "bool", title: inputTitle("Turn off the idle Thermostat${(settings?.theThermostats?.size() > 2)?'s':''}?"), defaultValue: false, submitOnChange: true, width: 4)
                if (settings?.hvacOff) {
                	input(name: "fanOff", type: "bool", title: inputTitle("Turn off the Fan on the idle systems?"), defaultValue: false, submitOnChange: true, width: 6)
                	if (HE) paragraph("", width: 2)
                }
                def statModes = getThermostatModes()
                if (settings?.hvacOff) input(name: "hvacOn", type: "enum", title: inputTitle("Return HVAC to"), defaultValue: "Prior State", options: statModes, width: 4)
                if (settings?.fanOff) {
                	if (HE && !settings?.hvacOff) paragraph("", width: 4)
                    def fanModes = getThermostatFanModes()
                    input(name: "fanOn", type: "enum", title: inputTitle("Return Fan to"), defaultValue: "Prior State", options: fanModes, width: 4)
                    if (settings?.fanOn == 'Circulate') input(name: "fanOnTime", type: "number", title: inputTitle("Fan Circulation Time (minutes)"), range: 1..55, defaultValue: 20, submitOnChange: true, width: 4)
                }
            }
            section (title: sectionTitle("Conditions")) {
            	paragraph "Choose one or more conditions; the Thermostat Mode will be changed only when ALL of these are true"
            	if (!settings?.busyStates) { app.updateSetting("busyStates", "cool"); settings.busyStates = "cool"; }
                input(name: "actionDays", type: "enum", title: smallerTitle("Only on these days of the week"), multiple: true, required: false, submitOnChange: true,
                      options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"], width: 5)
            	paragraph(smallerTitle("Only during this time period:"), width: 3)
				
                input(name: "fromTime", type: "time", title: inputTitle("From:"), required: false, submitOnChange: true, width: 4)
                if (HE) paragraph("", width: 8)
        		input(name: "toTime", type: "time", title: inputTitle("To:"), required: (settings?.fromTime != null), submitOnChange: true, width: 4)
				//def between = ((settings.fromTime != null) && (settings.toTime != null)) ? myTimeOfDayIsBetween(timeToday(fromTime), timeToday(toTime), new Date(), location.timeZone) : true
				paragraph(getFormat("note","All thermostats will be returned to the 'on' Mode outside of the above conditions"))
            }
		}  
        
        section(title: sectionTitle("Operations")) {
        	input(name: "minimize", 	title: inputTitle("Minimize settings text"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
           	input(name: "tempDisable", 	title: inputTitle("Pause this Helper"), 		type: "bool", required: false, defaultValue: false,	submitOnChange: true, width: 3)                
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
	atomicState.HVACModeState = 'unknown'
	atomicState.runningThermostat = ''
	initialize()  
}
def uninstalled() {
}
def updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    initialize()
}
def initialize() {
	String version = getVersionLabel()
	LOG("${version} Initializing...", 2, null, 'info')
    atomicState.versionLabel = getVersionLabel()
	boolean ST = isST
	
	updateMyLabel()

	// Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }

/*
	int idleStats = 0
    int busyStats = 0
    
    theThermostats.each { stat ->
    	String opState = ST ? stat.currentValue('thermostatOperatingState') : stat.currentValue('thermostatOperatingState', true)
        String theMode = ST ? stat.currentValue('thermostatMode') 			: stat.currentValue('thermostatMode', true)
        if (opState && (opState != 'idle')) {
        	busyStats++
        } else {
        	idleStats++
        }
    }
    */
	
	subscribe(theThermostats, 'thermostatOperatingState', thermostatHandler)
	updateMyLabel()
    LOG("Initialization complete", 4, "", 'info')
    return true
}

def thermostatHandler(evt) {
	String dni = evt.device.deviceNetworkId as String
    String tid = getDeviceId(dni)
    
	// First make sure we are supposed to be making changes now...
    boolean notNow = false
    if (!dayCheck()) {
        if (atomicState.HVACModeState != 'idle') LOG("Not configured to run Actions today, ignoring", 2, null, 'info')
        notNow = true
    }
    def between = ((settings.fromTime != null) && (settings.toTime != null)) ? myTimeOfDayIsBetween(timeToday(settings.fromTime), timeToday(settings.toTime), new Date(), location.timeZone) : true
    if (!notNow && !between) {
        if (atomicState.HVACModeState != 'idle') LOG('Not configured to run Actions at this time, ignoring', 2, null, 'info')
        notNow = true
    }
    
    if (notNow) {
    	// Make sure that we leave all the systems turned on once we are outside the configured days & time window
        if (atomicState.HVACModeState == 'off') {
        	theThermostats.each { stat ->
            	turnOnHVAC(stat)
            }
            atomicState.HVACModeState == 'idle'	// we'll only do this once...
        }
    } else if (evt.value.startsWith('idle') && (tid == atomicState.runningThermostat)) {
    	// turn them all back on again
        atomicState.HVACModeState = 'on'
        atomicState.runningThermostat = ''
        theThermostats.each { stat ->
        	if (stat.deviceNetworkId != dni) {
            	turnOnHVAC(stat)
            }
        }
    } else if (settings?.busyStates?.contains(evt.value)) {
    	// turn off all the other thermostats
        atomicState.runningThermostat = tid
        atomicState.HVACModeState = 'off'
        theThermostats.each { stat ->
        	if (stat.deviceNetworkId != dni) {
            	turnOffHVAC(stat)
            }
        }
    }  else {
    	LOG("thermostatHandler(): ${evt.name} = ${evt.value} for ${evt.device.displayName} - nothing to do...", 3, null, 'info')
    }
}

void turnOffHVAC(therm) {
	LOG("turnOffHVAC(${therm.displayName}}) entered...", 4,null,'info')
	boolean ST = isST
    
    if (settings.hvacOff) {
    	String tid = getDeviceId(therm.deviceNetworkId)
    	def tmpThermSavedState = atomicState.thermSavedState ?: [:]
    	if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
        // turn off the HVAC & save the state
		makeReservation(tid, 'modeOff')						// make sure nobody else turns HVAC on until I'm ready
		String thermostatMode = 	ST ? therm.currentValue('thermostatMode') 		: therm.currentValue('thermostatMode', true)
        String thermostatFanMode = 	ST ? therm.currentValue('thermostatFanMode') 	: therm.currentValue('thermostatFanMode', true)
        def fanMinOnTime = 			(ST ? therm.currentValue('fanMinOnTime') 		: therm.currentValue('fanMinOnTime', true)) ?: 0
    	if (thermostatMode != 'off') {
        	fanToo = false
            tmpThermSavedState[tid].mode = thermostatMode
            tmpThermSavedState[tid].fanMode = thermostatFanMode
            tmpThermSavedState[tid].fanTime = fanMinOnTime
            tmpThermSavedState[tid].HVACModeState = 'off'
			tmpThermSavedState[tid].wasAlreadyOff = false
            //therm.setThermostatMode('off')
			log.debug "therm.setThermostatMode( 'off' )"
            if (settings.fanOff) {
            	makeReservation(tid, 'fanOff')
            	//therm.setThermostatFanMode('off')		// "Off" will also clear fanMinOnTime
				log.debug "therm.setThermostatFanMode( 'off' )"
                fanToo = true
            }
			LOG("${therm.displayName} Mode ${fanToo?'and Fan ':''}turned off (was ${thermostatMode}/${thermostatFanMode}-${fanMinOnTime})",3,null,'info')    
        } else {
        	tmpThermSavedState[tid].wasAlreadyOff = true
            tmpThermSavedState[tid].HVACModeState = 'off'
        }
    	atomicState.thermSavedState = tmpThermSavedState
		LOG("turnOffHVAC(${therm.displayName}) - thermSavedState: ${atomicState.thermSavedState}", 4, null, 'debug')
    }
}

void turnOnHVAC(therm) {
 	LOG("turnOnHVAC(${therm.displayName}) entered...", 4,null,'info')
	boolean ST = isST

	if (settings.hvacOff) {
    	// turn on the HVAC
    	String tid = getDeviceId(therm.deviceNetworkId)
    	def tmpThermSavedState = atomicState.thermSavedState ?: [:]
        if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
        String currentMode = ST ? therm.currentValue('thermostatMode') : therm.currentValue('thermostatMode', true) 		// Better be "off"
     
        String thermostatMode
        String thermostatFanMode
        def fanMinOnTime = 0

        if (settings.hvacOn == "Prior State") {
            thermostatMode = tmpThermSavedState[tid].mode
            if (settings.fanOff) {
                if (settings.fanOn) {
                    if (settings.fanOn == "Prior State") {
                        thermostatFanMode = tmpThermSavedState[tid].fanMode
                        fanMinOnTime = tmpThermSavedState[tid].fanTime
                    }else {
                        thermostatFanMode = settings.fanOn
                        if (settings.fanOn == "Circulate") {
                            fanMinOnTime = settings.fanOnTime ?: 20
                        }
                    }
                }
            }
        } else {
            thermostatMode = settings.hvacOn
            if (settings.fanOff) {
                if (settings.fanOn) {
                    if (settings.fanOn == "Prior State") {
                        thermostatFanMode = tmpThermSavedState[tid].fanMode
                        fanMinOnTime = tmpThermSavedState[tid].fanTime
                    } else {
                        thermostatFanMode = settings.fanOn
                        if (settings.fanOn == "Circulate") {
                            fanMinOnTime = settings.fanOnTime ?: 20
                        }
                    }
                }
            }
        }
            
        if (currentMode == 'off') {
           	int i = countReservations( tid, 'modeOff' ) - (haveReservation(tid, 'modeOff') ? 1 : 0)
            if (i > 0) {
                // Currently off, and somebody besides me has a reservation - just release my reservation
                cancelReservation(tid, 'modeOff')
                notReserved = false							
                LOG("Cannot change ${therm.displayName} to ${thermostatMode.capitalize()} - ${getGuestList(tid, 'modeOff').toString()[1..-2]} hold 'modeOff' reservations",1,null,'warn')
            } else {
              	// Nobody else but me has a reservation
                cancelReservation(tid, 'modeOff')
				if (tmpThermSavedState[tid].containsKey('wasAlreadyOff') && (tmpThermSavedState[tid].wasAlreadyOff == false)) {
					tmpThermSavedState[tid].HVACModeState = 'on'
					tmpThermSavedState[tid].mode = thermostatMode
                	//therm.setThermostatMode( thermostatMode )
					log.debug "therm.setThermostatMode( ${thermostatMode} )"
                    boolean andFan = false
                    if (settings.fanOff) {
						i = countReservations( tid, 'fanOff' ) - (haveReservation(tid, 'fanOff') ? 1 : 0)
						if (i > 0) {
                			// Currently off, and somebody besides me has a reservation - just release my reservation
                			cancelReservation(tid, 'fanOff')	
                			LOG("Cannot change ${therm.displayName} to ${thermostatFanMode.capitalize()} - ${getGuestList(tid, 'fanOff').toString()[1..-2]} hold 'fanOff' reservations",1,null,'warn')
            			} else {
              				// Nobody else but me has a reservation
                			cancelReservation(tid, 'fanOff')
							currentFanTime = ST ? therm.currentValue('fanMinOnTime') 		: therm.currentValue('fanMinOnTime', true)
							if (fanMinOnTime != currentFanTime) {
								andFan = true
								therm.setFanMinOnTime = fanMinOnTime
								log.debug "therm.setFanMinOnTime( ${fanMinOnTime} )"
							}
							currentFanMode = ST ? therm.currentValue('thermostatFanMode') 	: therm.currentValue('thermostatFanMode', true)
							if (thermostatFanMode != currentFanMode) {
								andFan = true
								therm.setFanMode(thermostatFanMode)
								log.debug "therm.setThermostatFanMode( ${thermostatFanMode} )"
							}
						}
					}
					LOG("${therm.displayName} ${thermostatMode.capitalize()} Mode ${andFan?'and Fan':''} restored (was ${currentMode.capitalize()})",3,null,'info')
				} else {
					LOG("${therm.displayName} was already off, not turning back on",3,null,'info')
				}
            } 
        } else if (currentMode == thermostatMode) {
            LOG("${therm.displayName} is already in ${thermostatMode.capitalize()}",3,null,'info')
            tmpThermSavedState[tid].HVACModeState = 'on'
        }
        atomicState.thermSavedState = tmpThermSavedState
    }
}


// HELPER FUNCTIONS
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
	if (!settings.actionDays) return true
    
    def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    return (actionDays.contains(day))
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(String tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id as String, type )
}
// Cancel my reservation
void cancelReservation(String tid, String type='modeOff') {
	//log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id as String, type )
}
// Do I have a reservation?
Boolean haveReservation(String tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id as String, type )
}
// Do any Apps have reservations?
Boolean anyReservations(String tid, String type='modeOff') {
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
// Temporary/Global Pause functions
void updateMyLabel() {
	boolean ST = isST
    
	String flag
	if (ST) {
    	def opts = [' (paused)', ' (active ', ' (inactive', '%)']
		opts.each {
			if (!flag && app.label.contains(it)) flag = it
		}
	} else {
		flag = '<span '
	}
    
    String myLabel = atomicState.appDisplayName	
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!flag || !myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		if (flag != '%)') {
        	myLabel = myLabel.substring(0, myLabel.indexOf(flag))
        } else {
            int i = app.label.reverse().indexOf('(') + 2		// "${app.label} (nn%)" --> "${app.label}"
            myLabel = app.label.take(app.label.size()-i)
        }
		atomicState.appDisplayName = myLabel
	}
    def active = atomicState.isOK
	if (settings.tempDisable) {
		def newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
    	def newLabel = myLabel + (ST ? " (${active?'active ':'inactive '}${atomicState.smartSetpoint}%)" : '<span style="color:green"> (' + (active?'active ':'inactive ') + atomicState.smartSetpoint + '%)</span>')
        if (app.label != newLabel) app.updateLabel(newLabel)
    }// else {
	//	if (app.label != myLabel) app.updateLabel(myLabel)
	//}
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
def getThermostatModes() {
	def statModes = []
    if (settings?.theThermostats) {
    	settings.theThermostats.each { stat ->
    		def tempModes = stat.currentValue('supportedThermostatModes')
        	def modeTemp = (tempModes) ? tempModes[1..-2].tokenize(", ") : []
            if (modeTemp != []) statModes = (statModes == []) ? modeTemp : statModes.intersect(modeTemp)
        }
    }
    if (statModes == []) statModes = ["off","heat","cool","auto","auxHeatOnly"]
    return ["Prior State"] + statModes*.capitalize().sort(false)
}
List getThermostatFanModes() {
	def fanModes = []
    if (settings?.theThermostats) {
    	settings.theThermostats.each { stat ->
        	def tempModes = stat.currentValue('supportedThermostatFanModes')
        	def fanTemp = tempModes ? tempModes[1..-2].tokenize(", ") : []
            if (fanTemp != []) fanModes = (fanModes == []) ? fanTemp : fanModes.intersect(fanTemp)
        }
    }
    if (fanModes == []) fanModes = ["off", "auto", "circulate", "on"]
    return ["Prior State"] + fanModes*.capitalize().sort(false)
}
String getDeviceId(networkId) {
    return networkId.split(/\./).last()
}

void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${atomicState.appDisplayName} ${message}"
    if (logType == null) logType = 'debug'
	if (logType == 'debug') {
    	if (!settings?.debugOff) log.debug message
    } else if (logType == 'info') {
    	if (!settings?.infoOff) log.info message
    } else log."${logType}" message
	if (false) parent.LOG(msg, level, null, logType, event, displayEvent)
}

String getTheBee	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=78 height=78 align=right></img>'}
String getTheBeeLogo()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg width=30 height=30 align=left></img>'}
String getTheBeeUrl ()				{ return "https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg" }
String getTheBlank	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/blank.png width=400 height=35 align=right hspace=0 style="box-shadow: 3px 0px 3px 0px #ffffff;padding:0px;margin:0px"></img>'}
String pageTitle 	(String txt) 	{ return isHE ? getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') : txt }
String pageTitleOld	(String txt)	{ return isHE ? getFormat('header-ecobee','<h2>'+txt+'</h2>') 	: txt }
String sectionTitle	(String txt) 	{ return isHE ? getFormat('header-nobee','<h3><b>'+txt+'</b></h3>')	: txt }
String smallerTitle	(String txt) 	{ return txt ? (isHE ? '<h3><b>'+txt+'</b></h3>' 				: txt) : '' }
String sampleTitle	(String txt) 	{ return isHE ? '<b><i>'+txt+'<i></b>'			 				: txt }
String inputTitle	(String txt) 	{ return isHE ? '<b>'+txt+'</b>'								: txt }
String getWarningText()				{ return isHE ? "<span style='color:red'><b>WARNING: </b></span>"	: "WARNING: " }
String getFormat(type, myText=""){
	switch(type) {
		case "header-ecobee":
			return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theBee}${myText}</div>"
			break;
		case "header-nobee":
			return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
			break;
    	case "line":
			return isHE ? "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>" : "-----------------------------------------------"
			break;
		case "title":
			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
			break;
		case "warning":
			return isHE ? "<span style='color:red'><b>WARNING: </b><i></span>${myText}</i>" : "WARNING: ${myText}"
			break;
		case "note":
			return isHE ? "<b>NOTE: </b>${myText}" : "NOTE:<br>${myText}"
			break;
		default:
			return myText
			break;
	}
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
