/**
 *  ecobee Smart Room
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
 *	0.1.0 -	Initial Release
 *	0.1.1 -	Implementation Complete
 *	0.1.2 -	Added NOTIFYcations, trimmed settings texts
 *	0.1.3 -	Added ability to manually enable/disable Smart Room by clicking on the tile
 *	1.0.0 - Final prep for General Release
 *	1.0.1 - Edit LOG and setup for consistency
 *	1.0.2 - Fixed initialization
 *	1.0.3 - Updated settings and TempDisable handling
 *	1.2.0 - Sync version number with new holdHours/holdAction support
 *	1.2.1 - Protect against LOG type errors
 */
def getVersionNum() { return "1.2.1" }
private def getVersionLabel() { return "ecobee Smart Room Version ${getVersionNum()}" }
import groovy.json.JsonSlurper

definition(
	name: "ecobee Smart Room",
	namespace: "smartthings",
	author: "Barry A. Burke (storageanarchy at gmail dot com)",
	description: "Automates a Smart Room with sensors (ecobee sensor, door, windows, occupancy), adding/removing the room from selected climates and (optionally) opening/closing SmartThings-controlled vents.",
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
	dynamicPage(name: "mainPage", title: "Configure Smart Room", uninstall: true, install: true) {
    	section(title: "Name for Smart Room Handler") {
        	label title: "Name this Smart Room Handler", required: true, defaultValue: "Smart Room"      
        }
        
        section(title: "Smart Room Ecobee Sensor(s)") {
        	if (settings.tempDisable) {
            	paragraph "WARNING: Temporarily Disabled as requested. Turn back on to Enable handler."
            } else {
            	paragraph("A Smart Room is defined by one or more Ecobee Sensors")
            	// IMPORTANT NOTE: theSensors is NOT a list of sensors, but instead is the list of selected [DNI:names] of the sensors that our parent is supporting
            	// Unfortunately, type: "devices.ecobeeSensor" returns a null list, even though the Device Name is indeed "Ecobee Sensor" as required by the documentation
        		input(name: "theSensors", type:"enum", title: "Use which Ecobee Sensor(s)", options: getEcobeeSensorsList(), required: true, multiple: true, submitOnChange: true)
				input(name: "activeProgs", type:"enum", title: "Include this Smart Room in these programs when Active?", options: getProgramsList(), required: true, multiple: true)
				input(name: "inactiveProgs", type:"enum", title: "Include in these programs while Inactive? (optional)", options: getProgramsList(), required: false, multiple: true)
            }
		}
		
        if (!settings.tempDisable) {
        	section(title: "Smart Room Doors") {
            	paragraph("Smart Room Activation is controlled by how long doors are left open or closed")
            	input(name: "theDoors", title: "Select Door contact sensor(s)", type: "capability.contactSensor", required: true, multiple: true, submitOnChange: true)
            	if (settings.theDoors) {
            		// paragraph("How long should a door be open before the Smart Room is Activated?")
            		input(name: "doorOpenMinutes", title: "Open door Activation delay (minutes)?", type: "number", required: true, defaultValue: 5, description: '5', range: "5..60")
            		// paragraph("How long should the door${theDoors.size()>1?'s':''} be closed before the room is Inactive (occupied rooms will not be deactivated)?")
            		input(name: "doorClosedHours", title: "Closed door Deactivation delay (hours)?", type: "number", required: true, defaultValue: 12, description: '12', range: "1..24")
            	}
			}
        
       		section(title: "Smart Room Windows (optional)") {
        		paragraph("Windows will temporarily deactivate a Smart Room while they are open")
            	input(name: "theWindows", type: "capability.contactSensor", title: "Which Window contact sensor(s)? (optional)", required: false, multiple: true)
        	}
        
        	section(title:"Smart Room Motion Sensors (optional)") {
        		paragraph("Occupancy within the room will stop the Smart Room from being deactivated while the door${theDoors?.size()>1?'s are':' is'} closed")
            	input(name:"moreMotionSensors", type: "capability.motionSensor", title: "Select additional motion sensors?", required: false, multiple: true, submitOnChange: false)
        	}
       
        	section(title: "Smart Room Vents (optional)") {
        		paragraph("You can have specified Econet or Keen vents opened while a Smart Room is Active, and closed when Inactive")
            	input(name: "theEconetVents", type: "device.econetVent", title: "Control which EcoNet Vent(s)?", required: false, multiple: true, submitOnChange: true)
            	input(name: "theKeenVents", type: "device.keenHomeSmartVent", title: "Control which Keen Home Smart Vent(s)?", required: false, multiple:true, submitOnChange: true)
            	if (settings.theEconetVents || settings.theKeenVents) {
            		paragraph("Fully closing too many vents at once may be detrimental to your HVAC system. You may want to define a minimum closed percentage")
            		input(name: "minimumVentLevel", type: "number", title: "Minimum vent level when closed?", required: true, defaultValue:0, description: '0', range: "0..100")
            	}
        	}
        
      		section("Smart Room Notifications (optional)") {
        		input(name: "notify", type: "boolean", title: "Notify on Activations?", required: false, defaultValue: false, submitOnChange: true)
            	if (settings.notify) {
        			input(name: "recipients", type: "contact", title: "Send notifications to", required: notify) {
                		input(name: "pushNotify", type: "boolean", title: "Send push notifications?", required: true, defaultValue: false)
                	}
            	}
            	paragraph("(A notification is always sent to the Hello Home log whenever a Smart Room is activated or de-activated)")
        	}
        }
        	
		section(title: "Temporarily Disable?") {
        	input(name: "tempDisable", title: "Temporarily Disable this Handler? ", type: "bool", required: false, description: "", submitOnChange: true)                
        }
        
        section (getVersionLabel())
    }
}

// Main functions
void installed() {
	LOG("installed() entered", 2, "", 'trace')
    initialize()
}

void updated() {
	LOG("updated() entered", 2, "", 'trace')
	unsubscribe()
    unschedule()
    initialize()
}

void uninstalled() {
	generateSensorsEvents([doors:'default', windows:'default', vents:'default',SmartRoom:'default'])
}

def getProgramsList() { return theThermostat ? new JsonSlurper().parseText(theThermostat.currentValue('programsList')) : ["Away","Home","Sleep"] }

def getEcobeeSensorsList() { return parent.getEcobeeSensors().sort { it.value } }    

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 3, "", 'info')
    
    atomicState.isSmartRoomActive = false
    atomicState.isWaitingForWindows = false
    atomicState.isRoomOccupied = false
    
    // Now, just exit if we are disabled...
	if(tempDisable == true) {
    	generateSensorsEvents([SmartRoom:'disabled',vents:'default',windows:'default',doors:'default'])
    	LOG("temporarily disabled as per request.", 2, null, "warn")
    	return true
    }
    
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
        doorStatus = theDoors.latestState('contact').contains('open') ? 'open' : 'closed'
	} 
	sensorData << [ doors:doorStatus ]
    
    // Windows should only make a SmartRoom inactive if it is active, so nothing to do here during initialization
    // checkTheDoors() will also check the windows 
    String windowStatus = 'notused'
    if (theWindows) {
    	subscribe(theWindows, "contact", windowHandler)
		windowStatus = theWindows.currentContact.contains('open') ? 'open' : 'closed'
	}
	sensorData << [windows:windowStatus]
	
    def theVents = (theEconetVents ? theEconetVents : []) 
    theVents += (theKeenVents ? theKeenVents : [])
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
	log.debug 'door opened'
    
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

def openCheck() { checkTheDoors }

def doorClosedHandler(evt) {
	log.debug "door closed"
    if (!theDoors.currentContact.contains('open')) {								// if we are the last door closed...
    	generateSensorsEvents([doors:'closed'])
        unschedule(openCheck)
        // don't need to schedule the disable check if we are already (still) disabled (door was opened only briefly).
		if (atomicState.isSmartRoomActive) {
        	runIn((doorClosedHours.toInteger() * 3600), closedCheck, [overwrite:true])	// schedule the door check
        } 
    }
}

def closedCheck() { checkTheDoors }

def windowHandler(evt) {
	if (evt.value == 'open') {
    	log.debug 'window open'
    	if (atomicState.isSmartRoomActive) {
			deactivateRoom()
            atomicState.isWaitingForWindows = true
            generateSensorsEvents([windows:'open'])
        }
    } else { // closed
    	log.debug 'window closed'
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

def ventOff( theVent ) {
    if (minimumVentLevel.toInteger() == 0) {
      	if (theVent?.currentSwitch == 'on') theVent.off()
    } else {
    	if (theVent?.currentLevel.toInteger() != minimumVentLevel.toInteger()) theVent.setLevel(minimumVentLevel.toInteger())	// make sure none of the vents are less than the specified minimum
    }
}

def checkTheDoors() {
	Long startTime = now()
	// check if the door has been closed long enough to turn
    // we use State because we will need to know when the door opened or closed
    def currentDoorStates = theDoors.latestState('contact')
    
    if (currentDoorStates.contains('open')) {
        // one or more doors are open, so we need to figure out if it has been long enough to turn this room on
        Long minOpenTime = startTime - (60000 * settings.doorOpenMinutes.toLong())
        def openRecently = true
        currentDoorStates.each {
            if ((it.value == 'open') && (it.date.getTime() < minOpenTime)) openRecently = false
        }
        if (atomicState.isSmartRoomActive || !openRecently || atomicState.isWaitingForWindows) {
        	// at least 1 of the doors has been open long enough to enable this room, or we were a Smart Room already, OR we want to be a Smart Room, but someone opened a window
            if (!theWindows || theWindows.currentContact.contains('open')) {
                // no windows or no windows are open
                activateRoom()
                if (theWindows) atomicState.isWaitingForWindows = false
			} else {
               	// A window is open, so we can't be a Smart Room right now
                deactivateRoom()
                if (theWindows) atomicState.isWaitingForWindows = true
            }
        }
    } else { 
    	// all doors are closed - long enough to disable the room?
        Long minClosedTime = startTime - (3600000 * settings.doorClosedHours.toLong())
        def closedRecently = false
        Long closedShortest = 0
        currentDoorStates.each {
            Long timeItClosed = it.date.getTime() 
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
	}
}
	
def activateRoom() {
	log.debug "activateRoom()"
    def sensorData = [:]
    atomicState.isSmartRoomActive = true
    
    // turn on vents
    String status = 'notused'
    def theVents = (theEconetVents ? theEconetVents : []) 
    theVents += (theKeenVents ? theKeenVents : [])
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
        def smartRoomStatus = device.currentValue('SmartRoom')
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
    
    if (anyInactive) NOTIFY("I activated the ${app.label}")
    LOG("activated",2,null,'info')
}

def deactivateRoom() {
	log.debug "deactivateRoom()"
    def sensorData = [:]
    atomicState.isSmartRoomActive = false
    
    //turn off the vents
    String status = 'notused'
    def theVents = (theEconetVents ? theEconetVents : [])
    theVents += (theKeenVents ? theKeenVents : [])
    if (theVents != []) {
    	theVents.each {
        	ventOff(it)
        }
        status = 'closed'
    } 
    sensorData << [vents:status]

    //un-register this room's sensor(s) from the thermostat for the appropriate program(s)
    //and register this room's sensor(s) with the thermostat for the appropriate program(s)
    def anyActive = false
    theSensors.each {
    	def device = parent.getChildDevice(it)
        def smartRoomStatus = device.currentValue('SmartRoom')
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
    
    if (anyActive) NOTIFY("I just deactivated the ${app.label}")
    LOG("deactivated",2,null,'info',false,false)
}

def motionHandler(evt) {
	log.debug "motionHandler() ${evt.name} is ${evt.value}"
    
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
private def generateSensorsEvents( Map dataMap ) {
	LOG("generating ${dataMap} events for ${theSensors}",3,null,'info')
	theSensors.each { DNI ->
        parent.getChildDevice(DNI)?.generateEvent(dataMap)
    }
}

// Helper Functions
private def NOTIFY(message) {
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(message, recipients)		// notify contacts & hello home
    } else if (pushNotify) {
    	sendNotification(message)							// push and hello home
    } else {
        sendNotificationEvent(message)						// just hello home
    }
}

private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	message = "${app.label} ${message}"
	if (logType == null) logType = 'debug'
	parent?.LOG(message, level, null, logType, event, displayEvent)
    log."${logType}" message
}
