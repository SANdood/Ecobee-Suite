/**
 *  ecobee Suite Smart Room
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
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - Fix sort issue & noCache currentValue() for HE
 *	1.7.02 - Fixed SMS text entry
 *	1.7.03 - Fixing private method issue caused by grails
 *	1.7.04 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.05 - Optimized isST/isHE, added Global Pause
 *	1.7.06 - Added option to disable local display of log.debug() logs, support Notification devices on ST
 *	1.7.07 - Fixed Helper labelling
 */
String getVersionNum() { return "1.7.07" }
String getVersionLabel() { return "Ecobee Suite Smart Room Helper, version ${getVersionNum()} on ${getHubPlatform()}" }
import groovy.json.*

definition(
	name: 			"ecobee Suite Smart Room",
	namespace: 		"sandood",
	author: 		"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 	"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAutomates a Smart Room with sensors, adding/removing the room from selected climates and (optionally) controlling  vents.",
	category: 		"Convenience",
	parent: 		"sandood:Ecobee Suite Manager",
	iconUrl: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: 		"https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
    importUrl:		"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-room.src/ecobee-suite-smart-room.groovy",
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
        	String defaultName = "Smart Room"
			String defaultLabel = atomicState?.appDisplayName ?: defaultName
			String oldName = app.label
			input "thisName", "text", title: "Name for this ${defaultName} Helper", submitOnChange: true, defaultValue: defaultLabel
			if ((!oldName && settings.thisName) || (oldName && settings.thisName && (settings.thisName != oldName))) {
				app.updateLabel(thisName)
				atomicState.appDisplayName = thisName
			} else if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			}
			updateMyLabel()
			if (HE) {
				if (app.label.contains('<span ')) {
					if (atomicState?.appDisplayName != null) {
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
                	if (atomicState?.appDisplayName != null) {
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
            updateMyLabel()
        	if (settings.tempDisable) {
            	paragraph "WARNING: Temporarily Paused - re-enable below."
            } else {
            	paragraph("\nA Smart Room is defined by one or more Ecobee Sensors")
            	// IMPORTANT NOTE: theSensors is NOT a list of sensors, but instead is the list of selected [DNI:names] of the sensors that our parent is supporting
            	// Unfortunately, type: "devices.ecobeeSensor" returns a null list, even though the Device Name is indeed "Ecobee Sensor" as required by the documentation
        		input(name: "theSensors", type:"enum", title: "Use which Ecobee Sensor(s)", options: getEcobeeSensorsList(), required: true, multiple: true, submitOnChange: true)
				input(name: "activeProgs", type:"enum", title: "Include this Smart Room in these programs when Active?", options: getProgramsList(), required: true, multiple: true)
				input(name: "inactiveProgs", type:"enum", title: "Include in these programs while Inactive? (optional)", options: getProgramsList(), required: false, multiple: true)
            }
		}
		
        if (!settings?.tempDisable && (settings?.theSensors?.size() > 0)) {
        	section(title: (HE?'<b>':'') + "Smart Room Doors" + (HE?'</b>':'')) {
            	paragraph("Smart Room Activation is controlled by how long doors are left open or closed")
            	input(name: "theDoors", title: "Select Door contact sensor(s)", type: "capability.contactSensor", required: true, multiple: true, submitOnChange: true)
            	if (settings.theDoors) {
            		// paragraph("How long should a door be open before the Smart Room is Activated?")
            		input(name: "doorOpenMinutes", title: "Open door Activation delay (minutes)?", type: "number", required: true, defaultValue: 5, description: '5', range: "5..60")
            		// paragraph("How long should the door${theDoors.size()>1?'s':''} be closed before the room is Inactive (occupied rooms will not be deactivated)?")
            		input(name: "doorClosedHours", title: "Closed door Deactivation delay (hours)?", type: "number", required: true, defaultValue: 12, description: '12', range: "1..24")
            	}
			}
        
       		section(title: (HE?'<b>':'') + "Smart Room Windows (optional)" + (HE?'</b>':'')) {
        		paragraph("Windows will temporarily deactivate a Smart Room while they are open")
            	input(name: "theWindows", type: "capability.contactSensor", title: "Which Window contact sensor(s)? (optional)", required: false, multiple: true)
        	}
        
        	section(title: (HE?'<b>':'') + "Smart Room Motion Sensors (optional)" + (HE?'</b>':'')) {
        		paragraph("Occupancy within the room will stop the Smart Room from being deactivated while the door${theDoors?.size()>1?'s are':' is'} closed")
            	input(name:"moreMotionSensors", type: "capability.motionSensor", title: "Select additional motion sensors?", required: false, multiple: true, submitOnChange: false)
        	}
       
        	section(title: (HE?'<b>':'') + "Smart Room Vents (optional)" + (HE?'</b>':'')) {
        		paragraph("You can have specified Econet, Keen or Generic(dimmer) vents opened while a Smart Room is Active, and closed when Inactive")
				input(name: "theEconetVents", type: "${ST?'device.econetVent':'device.EconetVent'}", title: "Control which EcoNet Vent(s)?", required: false, multiple: true, submitOnChange: true)
				input(name: "theKeenVents", type: "${ST?'device.keenHomeSmartVent':'deviceKeenHomeSmartVent'}", title: "Control which Keen Home Smart Vent(s)?", required: false, multiple:true, submitOnChange: true)
                input(name: "theGenericVents", type: 'capability.switchLevel', title: "Control which Generic (dimmer) Vent(s)?", required: false, multiple: true, submitOnChange: true)
            	if (settings.theEconetVents || settings.theKeenVents || settings.theGenericVents) {
            		paragraph("Fully closing too many vents at once may be detrimental to your HVAC system. You may want to define a minimum closed percentage")
            		input(name: "minimumVentLevel", type: "number", title: "Minimum vent level when closed?", required: true, defaultValue:0, description: '0', range: "0..100")
            	}
        	}
        
      		section((HE?'<b>':'') + "Smart Room Notifications (optional)" + (HE?'</b>':'')) {
        		input(name: "notify", type: "bool", title: "Notify on Activations?", required: true, defaultValue: false, submitOnChange: true)
				paragraph HE ? "A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken\n" : "A notification is always sent to the Hello Home log whenever an action is taken\n"
			}
			
            if (settings.notify) {
				if (ST) {
					section("Notifications") {
						input(name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, required: true, submitOnChange: true)
						input(name: "notifiers", type: "capability.notification", title: "", required: ((settings.phone == null) && !settings.speak && !settings.pushNotify),
							  multiple: true, description: "Select notification devices", submitOnChange: true)
						input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777; +441234567890)", required: false, submitOnChange: true)
						input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
							if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
						if (!settings.phone && !settings.pushNotify && !settings.speak && !settings.notifiers) paragraph "WARNING: Notifications configured, but nowhere to send them!\n"
					}
				} else {		// HE
					section((HE?'<b>':'') + "Use Notification Device(s)" + (HE?'</b>':'')) {
						input(name: "notifiers", type: "capability.notification", title: "", required: ((settings.phone == null) && !settings.speak), multiple: true, 
							  description: "Select notification devices", submitOnChange: true)
						paragraph ""
					}
					section((HE?'<b>':'') + "Use SMS to Phone(s) (limit 10 messages per day)" + (HE?'</b>':'')) {
						input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777, +441234567890)",
							  required: ((settings.notifiers == null) && !settings.speak), submitOnChange: true)
						paragraph ""
					}
					section((HE?'<b>':'') + "Use Speech Device(s)" + (HE?'</b>':'')) {
						input(name: "speak", type: "bool", title: "Speak messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", multiple: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", multiple: true, submitOnChange: true)
							input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
						paragraph ""
					}
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
	generateSensorsEvents([doors:'default', windows:'default', vents:'default',SmartRoom:'default'])
}
def getProgramsList() { return /* theThermostat ? new JsonSlurper().parseText(theThermostat.currentValue('programsList')) : */ ["Away","Home","Sleep"] }
def getEcobeeSensorsList() { return parent.getEcobeeSensors().sort { it.value } }    

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	updateMyLabel()
    
    atomicState.isSmartRoomActive = false
    atomicState.isWaitingForWindows = false
    atomicState.isRoomOccupied = false
    
    // Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	generateSensorsEvents([SmartRoom:'disabled',vents:'default',windows:'default',doors:'default'])
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
	if (settings.debugOff) log.info "log.debug() logging disabled"
	
    def sensorData = [:]
    def startTime = now()
    
    // Sensors define a Smart Room
    def theSensorDevices = []
    theSensors.each {										// this is a list of the sensor's DNIs
        theSensorDevices << [parent.getChildDevice(it)]		// and this is the list of the devices themselves
    }    
    subscribe(theSensorDevices, "SmartRoom", smartRoomHandler)
    subscribe(theSensorDevices, "motion", motionHandler)
    if (moreMotionSensors) subscribe(moreMotionSensors, "motion", motionHandler)
    
    // Doors control a Smart Room, so we check here during initialization if we should be active or inactive
    String doorStatus = 'default'
    if (theDoors) {
    	subscribe(theDoors, "contact.open", doorOpenHandler)
    	subscribe(theDoors, "contact.closed", doorClosedHandler)
        checkTheDoors()
        doorStatus = theDoors.currentContact.contains('open') ? 'open' : 'closed'
        // if (doorStatus == 'open') atomicState.isSmartRoomActive = true
	} 
	sensorData << [ doors:doorStatus ]
    
    // Windows should only make a SmartRoom inactive if it is active, so nothing to do here during initialization
    // checkTheDoors() will also check the windows 
    String windowStatus = 'notused'
    if (theWindows) {
    	subscribe(theWindows, "contact", windowHandler)
		windowStatus = theWindows.currentContact.contains('open') ? 'open' : 'closed'
        //if (windowStatus == 'open') atomicState.isSmartRoomActive = false
	}
	sensorData << [windows:windowStatus]
	
    def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : []) + (theGenericVents ? theGenericVents : [])    
    def ventStatus = 'notused'
	if (theVents != []) {
    	// if any vent is on and open == 100, then open
		if (theVents.currentSwitch.contains('on')) {
            def ventsOn = false
            // we have to check the level, can't just go by on/off here
            theVents.each {
            	if ((it.currentSwitch == 'on') && (it.currentLevel >= 99)) {		// some vents only go to 99, not 100
                	ventsOn = true
                } else {
					ventOff(it)				// handles minimumVentLevel for us
                }
            }
			ventStatus = (ventsOn) ? 'open' : 'closed'
        } else {
        	theVents.each {
            	ventOff(it)			
            }
            ventStatus = 'closed'
        }
	}
	sensorData << [vents:ventStatus]
    
	// update the device handler status display
    String smartStatus = atomicState.isSmartRoomActive ? 'active' : 'inactive'
    sensorData << [SmartRoom:smartStatus]
	generateSensorsEvents(sensorData)
}

// handles SmartRoom button press on the Sensor (enabled after Smart Room is configured)
def smartRoomHandler(evt) {
	if (evt.value == 'enable') {
    	if (!theWindows ||theWindows.currentContact.contains('open')) {
    		activateRoom()
            if (theWindows) atomicState.isWaitingForWindows = false
        } else {
        	if (theWindows) atomicState.isWaitingForWindows = true
        }
        // disable the automation for now...
        if (theDoors.currentContact.contains('open')) {
        	unschedule(openCheck)
        } else {
        	unschedule(closedCheck)
        }
    } else if (evt.value == 'disable') {
    	deactivateRoom()
        // re-enable the automation, with reset timers
        if (theDoors.currentContact.contains('open')) {
        	runIn((doorOpenMinutes*60), openCheck, [overwrite:true])
        } else {
        	// runIn((doorClosedHours*3600), closedCheck, [overwrite:true])
        }
    }
}

def doorOpenHandler(evt) {
	LOG("A door opened: ${evt.device.displayName} is ${evt.value}", 3, null, 'trace')
    
    Integer i = 0
    theDoors.each { if (it.currentContact == 'open') i++ }
    if (i == 1) {								// if we're the first/only door open
    	generateSensorsEvents([doors:'open'])
    	unschedule(closedCheck)
    	if (!atomicState.isSmartRoomActive) {
        	runIn((doorOpenMinutes.toInteger() * 60), openCheck, [overwrite:true])	// schedule the door check
        } else {
        	unschedule(openCheck)		// must have been activated by motion while the door was closed
        }
    }
}

def openCheck() { checkTheDoors() }

def doorClosedHandler(evt) {
	LOG("A door closed: ${evt.device.displayName} is ${evt.value}", 3, null, 'trace')
    if (!theDoors.currentContact.contains('open')) {								// if we are the last door closed...
    	generateSensorsEvents([doors:'closed'])
        unschedule(openCheck)
        // don't need to schedule the disable check if we are already (still) disabled (door was opened only briefly).
		if (atomicState.isSmartRoomActive) {
        	runIn((doorClosedHours.toInteger() * 3600), closedCheck, [overwrite:true])	// schedule the door check
        } 
    }
}

def closedCheck() { checkTheDoors() }

def windowHandler(evt) {
	if (evt.value == 'open') {
    	LOG("Window ${evt.device.displayName} was just opened", 3, null, 'trace')
    	if (atomicState.isSmartRoomActive) {
			deactivateRoom()
            atomicState.isWaitingForWindows = true
            generateSensorsEvents([windows:'open'])
        }
    } else { // closed
    	LOG("Window ${evt.device.displayName} was just closed", 3, null, 'trace')
    	if (atomicState.isWaitingForWindows) {
        	if (!theWindows || theWindows.currentContact.contains('open')) {
            	// looks like all the windows are closed
            	activateRoom()
                atomicState.isWaitingForWindows = false
                generateSensorsEvents([windows:'closed'])
            }
		}
    }
}

void ventOff( theVent ) {
    if (minimumVentLevel.toInteger() == 0) {
      	if (theVent?.currentSwitch == 'on') theVent.off()
    } else {
    	if (theVent?.currentLevel.toInteger() != minimumVentLevel.toInteger()) theVent.setLevel(minimumVentLevel.toInteger())	// make sure none of the vents are less than the specified minimum
    }
}

void checkTheDoors() {
	LOG("Checking the doors", 3, null, 'trace')
	Long startTime = now()
	// check if the door has been closed long enough to turn
    // we use State because we will need to know when the door opened or closed
    def currentDoorStates = theDoors.latestState('contact')

    if (currentDoorStates.value.contains('open')) {
    	LOG("A door is open", 3, null, 'trace')
        // one or more doors are open, so we need to figure out if it has been long enough to turn this room on
        Long minOpenTime = startTime - (60000 * settings.doorOpenMinutes.toLong())
        def openRecently = true
        currentDoorStates.each {
            if ((it.value == 'open') && (it.date.getTime() < minOpenTime)) openRecently = false
        }
        // log.debug "${atomicState.isSmartRoomActive} ${openRecently} ${atomicState.isWatingForWindows}"
        if (atomicState.isSmartRoomActive || !openRecently || atomicState.isWaitingForWindows) {
        	// at least 1 of the doors has been open long enough to enable this room, or we were a Smart Room already, OR we want to be a Smart Room, but someone opened a window
            if (!theWindows || !theWindows.currentContact.contains('open')) {
                // no windows or no windows are open
                //log.debug "checkTheDoors - activateRoom"
                activateRoom()
                if (theWindows) atomicState.isWaitingForWindows = false
			} else {
               	// A window is open, so we can't be a Smart Room right now
                //log.debug "checkTheDoors - deactivateRoom"
                deactivateRoom()
                if (theWindows) atomicState.isWaitingForWindows = true
            }
        }
    } else /* if (currentDoorStates.value.contains('closed')) */ { 
    	LOG("All doors are closed", 3, null, 'trace')
    	// all doors are closed - long enough to disable the room?
        Long minClosedTime = startTime - (3600000 * settings.doorClosedHours.toLong())
        def closedRecently = false
        Long closedShortest = 0
        currentDoorStates.each {
            Long timeItClosed = it.date.getTime() 
            // log.debug "timeItClosed ${timeItClosed}"
            if (timeItClosed > minClosedTime) closedRecently = true
            if (timeItClosed > closedShortest) closedShortest = timeItClosed
        }
		if (!closedRecently) {
            // all the doors have been closed at least doorClosedHours
            deactivateRoom()	// Matters not if the windows are open
            if (theWindows) atomicState.isWaitingForWindows = false
        } else {
            // One or more doors has been open for less than the required time
            Integer checkDoorSeconds = ( doorClosedHours.toInteger() * 3600) - ((startTime - closedShortest) / 1000)
            if (checkDoorSeconds > 0) runIn(checkDoorSeconds, checkTheDoors, [overwrite:true])
            // leave isSmartRoomActive and isWaitingForWindows alone
        }
	} // else LOG("No door states ${currentDoorStates.device.displayName}, ${currentDoorStates.name}, ${currentDoorStates.value}",1,null, 'error')
}
	
void activateRoom() {
	LOG("Activating the Smart Room", 3, null, 'info')
    def sensorData = [:]
    atomicState.isSmartRoomActive = true
	boolean ST = atomicState.isST
    
    // turn on vents
    String status = 'notused'
    def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : []) + (theGenericVents ? theGenericVents : [])
    if (theVents != []) {
    	theVents.each {
        	if (it.currentSwitch != 'on') it.on()
        	if (it.currentLevel < 99) it.setLevel(100)
        }
        status = 'open'
    }
    sensorData << [vents:status]
	
    // register this room's sensor(s) with the thermostat for the appropriate program(s)
    def anyInactive = false
    theSensors.each {
    	def device = parent.getChildDevice(it)
        def smartRoomStatus = ST ? device.currentValue('SmartRoom') : device.currentValue('SmartRoom', true)
        if (smartRoomStatus == 'inactive') anyInactive = true
        if (smartRoomStatus == 'enable') anyInactive = true		// we are turning on
    	if (activeProgs?.contains("Home")) {
        	device.addSensorToHome()
        } else {
        	device.deleteSensorFromHome()
        }
    	if (activeProgs?.contains("Away")) {
        	device.addSensorToAway()
        } else {
        	device.deleteSensorFromAway()
        }
    	if (activeProgs?.contains("Sleep")) {
        	device.addSensorToSleep()
        } else {
        	device.deleteSensorFromSleep()
        }
	}
    // all set, mark the sensors' SmartRoom as active
    if (anyInactive) sensorData << [SmartRoom:'active']
    generateSensorsEvents( sensorData )
    
    if (anyInactive) sendMessage("I just activated ${app.label} (Smart Room)")
    LOG("Activated",3,null,'info')
}

void deactivateRoom() {
	LOG("Deactivating Smart Room", 3, null, 'info')
    def sensorData = [:]
    atomicState.isSmartRoomActive = false
    
    //turn off the vents
    String status = 'notused'
    def theVents = (theEconetVents ? theEconetVents : []) + (theKeenVents ? theKeenVents : []) + (theGenericVents ? theGenericVents : [])
    if (theVents != []) {
    	theVents.each {
        	ventOff(it)
        }
        status = 'closed'
    } 
    sensorData << [vents:status]

    //un-register this room's sensor(s) from the thermostat for the appropriate program(s)
    //and register this room's sensor(s) with the thermostat for the appropriate program(s)
    boolean anyActive = false
	boolean ST = atomicState.isST
    theSensors.each {
    	def device = parent.getChildDevice(it)
        def smartRoomStatus = ST ? device.currentValue('SmartRoom') : device.currentValue('SmartRoom', true) 
        if (smartRoomStatus == 'active') anyActive = true 
        if (smartRoomStatus == 'disable') anyActive = true
    	if (inactiveProgs?.contains("Home")) {
            device.addSensorToHome()
        } else {
            device.deleteSensorFromHome()
        }
    	if (inactiveProgs?.contains("Away")) {
        	device.addSensorToAway()
        } else {
        	device.deleteSensorFromAway()
        }
    	if (inactiveProgs?.contains("Sleep")) {
        	device.addSensorToSleep()
        } else {
        	device.deleteSensorFromSleep()
        }    
    }
    if (anyActive) sensorData << [SmartRoom:'inactive']
    generateSensorsEvents(sensorData)
    atomicState.isRoomOccupied = false	// this gets turned on the first time motion is detected after the doors are closed
    
    if (anyActive) sendMessage("I just deactivated ${app.label} (Smart Room)")
    LOG("Deactivated",3,null,'info',false,false)
}

def motionHandler(evt) {
	LOG("Motion: ${evt.device.displayName} detected ${evt.value}", 3, null, 'trace')
    
    if (evt.value == 'active') {
    	if (theDoors.currentContact.contains('open')) {	
        	// one or more doors are still open - do nothing (let the doorOpen logic determine when to activate the room)
        } else {
        	// all the doors are closed
            if (atomicState.isSmartRoomActive) {
            	// already Active
                // stop the doorsClosedHours checks (if any)...don't need to start those until door closes again (occupant leaves)
                if (!atomicState.isRoomOccupied) {
                	unschedule(checkTheDoors) // don't deactivate while the room is populated
                    atomicState.isRoomOccupied = true
                }
            } else {
            	// There's somebody in here and the door is closed, but Smart Room is not Active - activate?
                // For now, let's ignore it - must be a phantom motion report
                // TODO:
                // we could check if it is within the doorOpenMinutes time - that is, someone just opened the door, came in, closed the door...if a new 
                // motion even happens within the doorOpenMinutes then we could activate the room...
                if (false) {
                	if (!theWindows || theWindows.currentContact.contains('open')) {
                		// no windows or no windows are open
                		activateRoom()
                		if (theWindows) atomicState.isWaitingForWindows = false
                    	atomicState.isRoomOccupied = true
					} else {
               			// A window is open, so we can't be a Smart Room right now
                        // log.debug "motionHandler - deactivateRoom"
                		deactivateRoom()
                		if (theWindows) atomicState.isWaitingForWindows = true
                    	atomicState.isRoomOccupied = true
            		}
                }
            }
        }
	} else {
    	// motion just went inactive
        // we could check if the doors have been open since we first saw motion while the doors were shut, and if not we could assume our occupant is "sleeping".
        // for now, don't do anything. 
	} 
}

// Ask our parents for help sending the events to our peer sensor devices
void generateSensorsEvents( Map dataMap ) {
	LOG("generating ${dataMap} events for ${theSensors}",3,null,'trace')
	theSensors.each { DNI ->
        parent.getChildDevice(DNI)?.generateEvent(dataMap)
    }
}

void sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "trace")
    if (settings.notify) {
        String msg = "${app.label} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (atomicState.isST) {
			if (settings.notifiers != null) {
				settings.notifiers.each {									// Use notification devices 
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
		sendLocationEvent(name: "HelloHome", description: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
}

void updateMyLabel() {
	boolean ST = atomicState.isST
    
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
void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	String msg = "${atomicState.appDisplayName} ${message}"
    if (logType == null) logType = 'debug'
	if ((logType != 'debug') || (!settings.debugOff)) log."${logType}" message
	parent.LOG(msg, level, null, logType, event, displayEvent)
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
String  getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST()     { return (atomicState?.isST != null) ? atomicState.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
boolean getIsHE()     { return (atomicState?.isHE != null) ? atomicState.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
//
// The following 3 calls are ONLY for use within the Device Handler or Application runtime
//  - they will throw an error at compile time if used within metadata, usually complaining that "state" is not defined
//  - getHubPlatform() ***MUST*** be called from the installed() method, then use "state.hubPlatform" elsewhere
//  - "if (state.isST)" is more efficient than "if (isSTHub)"
//
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
	// def ST = (atomicState?.isST != null) ? atomicState?.isST : isST
	//log.debug "isST: ${isST}, isHE: ${isHE}"
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"	
}
//
// **************************************************************************************************************************
