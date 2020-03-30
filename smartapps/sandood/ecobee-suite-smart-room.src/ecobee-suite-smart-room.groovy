/**
 *  ecobee Suite Smart Room
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
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - Fix sort issue & noCache currentValue() for HE
 *	1.7.02 - Fixed SMS text entry
 *	1.7.03 - Fixing private method issue caused by grails
 *	1.7.04 - On HE, changed (paused) banner to match Hubitat Simple Lighting's (pause)
 *	1.7.05 - Optimized isST/isHE, added Global Pause
 *	1.7.06 - Added option to disable local display of log.debug() logs, support Notification devices on ST
 *	1.7.07 - Fixed Helper labelling
 *	1.7.08 - Fixed labels (again), added infoOff, cleaned up preferences setup
 *	1.7.09 - Integrated with Smart Vents Helper
 *	1.7.10 - Uses new updateSensorPrograms() (if not using Smart Vents Helper).
 *	1.7.11 - Added reservation serializer for climate/sensor changes (program.climates)
 *	1.7.12 - Added minimize UI
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - More busy bees
 *	1.8.03 - Send simultaneous notification Announcements to multiple Echo Speaks devices
 *	1.8.04 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.05 - New SHPL, using Global Fields instead of atomicState
 *	1.8.06 - Fixed appDisplayName in sendMessage
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.8.06" }
String getVersionLabel() { return "Ecobee Suite Smart Room Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Smart Room",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAutomates a Smart Room with sensors, adding/removing the room from selected climates and (optionally) controlling  vents.",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-room.src/ecobee-suite-smart-room.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-smart-room-sa",
	singleInstance: 	false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	//boolean ST = isST
	//boolean HE = !ST
    def vc = 0			// vent counter
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
    String defaultName = "Smart Room"
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
		if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This Helper creates 'Smart Rooms' that are automatically activated based on motion detection and door state. If a door is left open for a specified time, the Smart Room will become active. It will remain active "+
                          "as long as the door is open. When the door is closed, Smart Room will become inactive after a specified timeout, unless motion is detected within the room while the door is closed.\n\n "+
                          "When a Smart Room is activated, it will optionally enable (unpause) the associated Smart Vent Helper(s) for the room, or it can open the selected vents directly. If vents are operated directly, the room's Ecobee "+
                          "Suite Sensor(s) (required) will be registered for the selected thermostat programs, otherwise that will be left to Smart Vent Helper(s) to handle.\n\n"+
                          "When the room becomes inactive, the Smart Vents Helper(s) are paused, or the vents will be closed and unregistered from their parent thermostat's programs.")
            }
        }
		section(title: sectionTitle("Naming${!settings.tempDisable?' & Room Definition':''}")) {	
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
				def opts = [' (paused', ' (active', ' (inactive', ' (disabled', ' (default']
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
            	if (maximize) paragraph("A Smart Room is defined by one or more Ecobee Sensors (required)")
        		//input(name: "theSensors", type:"enum", title: inputTitle("Select Ecobee Suite Sensor(s)"), options: getEcobeeSensorsList(), required: true, multiple: true, submitOnChange: true)
                input(name: "theSensorDevices", type:(ST?"device.ecobeeSuiteSensor":"device.EcobeeSuiteSensor"), title: inputTitle("Select Ecobee Suite Sensor(s)"), required: true, multiple: true, submitOnChange: true)
				input(name: "activeProgs", type:"enum", title: inputTitle("Select Active programs for this Smart Room"), options: getProgramsList(), required: true, multiple: true, width: 6)
                if (settings?.theSensorDevices?.size() > 1) {
                	paragraph (warningText + 
							   (maximize?'Only 1 Ecobee Suite Sensor can be registered for Active/Inactive programs, and you have selected multiple. ':'') +
							   "Only ${isHE?'<b><i>':''}${settings?.theSensorDevices[0].displayName}${isHE?'</i>,</b>':''} will be included in the "+
                    		  "selected program(s).")
                }
				input(name: "inactiveProgs", type:"enum", title: inputTitle("Select Inactive programs for this Smart Room")+" (optional)", options: getProgramsList(), required: false, multiple: true, width: 6)
            }
		}
		
        if (!settings?.tempDisable && (settings?.theSensorDevices?.size() > 0)) {
        	section(title: sectionTitle("Configuration")) {
				if (maximize) paragraph("Smart Room Activation is controlled by how long doors are left open or closed")
				input(name: "theDoors", title: inputTitle("Select Door contact sensor(s)"), type: "capability.contactSensor", required: true, multiple: true, submitOnChange: true)
				if (settings.theDoors) {
					// paragraph("How long should a door be open before the Smart Room is Activated?")
					input(name: "doorOpenMinutes", title: inputTitle("Activation (open door) Delay")+" (minutes)", type: "number", required: true, defaultValue: 5, description: '5', range: "5..60", width: 4)
					if (doorOpenMinutes == null) { app.updateSetting('doorOpenMinutes',5); settings?.doorOpenMinutes = 5; }
					// paragraph("How long should the door${theDoors.size()>1?'s':''} be closed before the room is Inactive (occupied rooms will not be deactivated)?")
					input(name: "doorClosedHours", title: inputTitle("Deactivation (closed door) Delay")+" (hours)", type: "number", required: true, defaultValue: 12, description: '12', range: "1..24", width: 4)
					if (doorClosedHours == null) { app.updateSetting('doorClosedHours',12); settings?.doorClosedHours = 12; }
				}
				if (maximize) paragraph("Windows will temporarily deactivate a Smart Room while they are open")
				input(name: "theWindows", type: "capability.contactSensor", title: inputTitle("Select Window contact sensor(s)? (optional)"), required: false, multiple: true)

				if (maximize) paragraph("Occupancy within the room will stop the Smart Room from being deactivated while the door${theDoors?.size()>1?'s are':' is'} closed")
				input(name:"moreMotionSensors", type: "capability.motionSensor", title: inputTitle("Select additional motion sensors?"), required: false, multiple: true, submitOnChange: false)

				def allVentApps = parent.getMyChildren("ecobee Suite Smart Vents")
                if (allVentApps) {
                	if (maximize) paragraph("If you are using the Smart Vents Helper(s) for this room, this Helper can unpause them when the Smart Room is Active, and pause them when Inactive")
                	input(name: "manageSmartVents", type: "bool", title: inputTitle("Manage Smart Vents Helper(s) for this Smart Room?"), defaultValue: false, submitOnChange: true, width: 6)
                	if (settings?.manageSmartVents) {
                    	input(name: "theVentApps", type: "enum", title: inputTitle("Select Smart Vents Helper(s) to manage"), required: true, multiple: true, submitOnChange: true, options: allVentApps, width: 6)
                    /*  def appNames = []
                        if (settings?.theVentApps) {
                        	settings.theVentApps.each { appId ->
                        		allVentApps.each{ key, value ->
                                	String selection = (key == appId) ? value : ''
                        			if (selection != '') {
                                		appNames = appNames + [selection]
                                    }
                                }
                            }
                        } */
					}
                }
				if (!allVentApps || !settings?.manageSmartVents) {
                    if (maximize) paragraph("This Helper can open selected Econet, Keen or Generic(dimmer) vents and/or switches while a Smart Room is Active, and close them when Inactive")
                    input(name: "theEconetVents", type: "${ST?'device.econetVent':'device.EcoNetVent'}", title: inputTitle("Select EcoNet Vent(s)?"), multiple: true, submitOnChange: true, 
                          hideWhenEmpty: true, required: (!settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
                    if (settings.theEconetVents) vc = settings.theEconetVents.size()
                    input(name: "theHCEcoVents", type: "${ST?'device.hubconnectEcovent':'device.HubConnectEcoVent'}", title: inputTitle("Select HubConnect EcoNet Vent(s)?"), multiple:true, 
                          submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
                    if (settings.theHCEcoVents) vc = vc + settings.theHCEcoVents.size()
                    input(name: "theKeenVents", type: "${ST?'device.keenHomeSmartVent':'device.KeenHomeSmartVent'}", title: inputTitle("Select Keen Home Smart Vent(s)?"), multiple:true, 
                          submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
                    if (settings.theKeenVents) vc = vc + settings.theKeenVents.size()
                    input(name: "theHCKeenVents", type: "${ST?'device.hubconnectKeenHomeSmartVent':'device.HubConnectKeenHomeSmartVent'}", title: inputTitle("Select HubConnect Keen Home Smart Vent(s)?"), multiple:true, 
                          submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
                    if (settings.theHCKeenVents) vc = vc + settings.theHCKeenVents.size()
                    input(name: "theGenericVents", type: 'capability.switchLevel', title: inputTitle("Select Generic (dimmer) Vent(s)"), multiple: true, submitOnChange: true, hideWhenEmpty: true, 
                          required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericSwitches))
                    if (settings.theGenericVents) vc = vc + settings.theGenericVents.size()
                    input(name: "theGenericSwitches", type: 'capability.switch', title: inputTitle("Select Switch(es)"), multiple: true, submitOnChange: true, hideWhenEmpty: true,
                          required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents))
                    if (settings.theGenericSwitches) vc = vc + settings.theGenericSwitches.size()
                    def s = ((vc == 0) || (vc > 1)) ? 's' : ''
                    paragraph "${vc} vent${s}/switch${s=='s'?'es':''} selected"
                    if (vc != 0) {
                        if (maximize) paragraph("Fully closing too many vents at once may be detrimental to your HVAC system. You may want to define a minimum closed percentage")
                        input(name: "minimumVentLevel", type: "number", title: inputTitle("Minimum vent level when closed?"), required: true, defaultValue:0, description: '0', range: "0..100", width: 6)
                    }
                }
        	}
        
			if (ST) {
				section("Notifications") {
					input(name: "notify", type: "bool", title: inputTitle("Notify on Actions?"), required: true, defaultValue: false, submitOnChange: true, width: 3)
					if (settings.notify) {
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
					input(name: "notify", type: "bool", title: inputTitle("Notify on Actions?"), required: true, defaultValue: false, submitOnChange: true, width: 3)
					if (settings.notify) {
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
                if (maximize)  {
					section() {
						paragraph "A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken"		
					}
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

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	updateMyLabel()
    
    atomicState.isSmartRoomActive = false
    atomicState.isWaitingForWindows = false
    atomicState.isRoomOccupied = false
    
    // Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	generateSensorsEvents([SmartRoom:'disabled',vents:'default',windows:'default',doors:'default'])
        atomicState.SmartRoom = 'disabled'
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
	if (settings.debugOff) log.info "log.debug() logging disabled"
	
    def sensorData = [:]
    def startTime = now()
    
    // Sensors define a Smart Room
    if (!settings?.theSensorDevices && settings?.theSensors) {
    	def sensorsList = []
    	settings.theSensors.each {									// theSensors was the list of the sensor's DNIs
            sensorsList << [parent.getChildDevice(it)]		// and theSensorDevices is the list of the devices themselves
        }
        if (sensorsList) {
        	app.updateSetting('theSensorDevices', sensorsList)
            settings.theSensorDevices = sensorsList
        }
        app.updateSetting('theSensors', [])
        settings.theSensors = []
    }
    if (settings?.theSensorDevices) {
    	subscribe(settings?.theSensorDevices, "SmartRoom", smartRoomHandler)
    	subscribe(settings?.theSensorDevices, "motion", motionHandler)
    }
    if (moreMotionSensors) subscribe(moreMotionSensors, "motion", motionHandler)
    
    // Doors control a Smart Room, so we check here during initialization if we should be active or inactive
    String doorStatus = 'default'
    if (theDoors) {
    	subscribe(theDoors, "contact.open", doorOpenHandler)
    	subscribe(theDoors, "contact.closed", doorClosedHandler)
        checkTheDoors()
        doorStatus = theDoors.currentContact.contains('open') ? 'open' : 'closed'
        if (doorStatus == 'open') atomicState.isSmartRoomActive = true
	} 
	sensorData << [doors: doorStatus]
    
    // Windows should only make a SmartRoom inactive if it is active, so nothing to do here during initialization
    // checkTheDoors() will also check the windows 
    String windowStatus = 'notused'
    if (theWindows) {
    	subscribe(theWindows, "contact", windowHandler)
		windowStatus = theWindows.currentContact.contains('open') ? 'open' : 'closed'
        if (windowStatus == 'open') {
        	// atomicState.isSmartRoomActive = false
            atomicState.waitingForWindows = true
        }
	}
	sensorData << [windows: windowStatus]
	log.debug "isSRActive: ${atomicState.isSmartRoomActive}"
    if (settings?.manageSmartVents && settings?.theVentApps) {
    	settings.theVentApps.each { appId ->
        log.debug "calling parent.pauseChildApp(${appId}, ${(atomicState.isSmartRoomActive != true)})"
        	parent.pauseChildApp( appId, (atomicState.isSmartRoomActive != true))	// unpause if active, pause if not
        }
    } else {
        def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
                        (settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                         (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])   
        def ventStatus = 'notused'
        if (theVents != []) {
            // if any vent is on and open == 100, then open
            if (theVents.currentSwitch.contains('on')) {
                def ventsOn = false
                // we have to check the level, can't just go by on/off here
                theVents.each {
                    if (it.hasAttribute('contact')) {
                        // It's one of Barry's vent drivers!
                        if (it.currentContact == 'open') {
                            ventsOn = true
                        } else {
                            ventOff(it)
                        }
                    } else {
                        if ((it.currentSwitch == 'on') && (it.currentLevel >= 98)) {		// some vents only go to 99, not 100
                            ventsOn = true
                        } else {
                            ventOff(it)				// handles minimumVentLevel for us
                        }
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
    }
	// update the device handler status display
    String smartStatus = atomicState.isSmartRoomActive ? 'active' : 'inactive'
    sensorData << [SmartRoom:smartStatus]
    atomicState.SmartRoom = smartStatus
	generateSensorsEvents(sensorData)
    updateMyLabel()
}

// handles SmartRoom button press on the Sensor (enabled after Smart Room is configured)
def smartRoomHandler(evt) {
	if (evt.value == 'enable') {
    	if (!theWindows || !theWindows.currentContact.contains('open')) {
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
        if (theVent?.hasCommand('setLevel')) theVent.setLevel(0)
    } else {
    	if (theVent?.currentLevel.toInteger() != minimumVentLevel.toInteger()) {
        	theVent.setLevel(minimumVentLevel.toInteger())	// make sure none of the vents are less than the specified minimum
        }
    }
    // Display the contact as "closed", even if we are partially open (so that HomeKit shows open/closed Blinds)
    if (theVent.hasAttribute('contact') && theVent.hasCommand('closeContact') && (theVent.currentContact != 'closed')) theVent.closeContact()
}

void checkTheDoors() {
	LOG("Checking the doors", 3, null, 'trace')
	Long startTime = now()
	// check if the door has been closed long enough to turn
    // we use State because we will need to know when the door opened or closed
    def currentDoorStates = theDoors*.latestState('contact')

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
	//boolean ST = isST
    boolean anyInactive = true
    
    // turn on vents
    if (settings?.manageSmartVents && settings?.theVentApps) {
    	sensorData << [SmartRoom:'active']		// Turn on the Smart Room first, so Smart Vents knows to update vent status
        atomicState.SmartRoom = 'active'
        generateSensorsEvents( sensorData )
        
    	// Let the Smart Vents Helper handle the vents
    	settings.theVentApps.each { appId ->
        	String appName = parent.getChildAppName(appId)
        	LOG("Requesting un-pause for ${appName?:appId}",3,null,'trace')
        	parent.pauseChildApp(appId, false)
        }
    } else {
    	String status = 'notused'
        def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
                        (settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                         (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
        if (theVents != []) {
            theVents.each { theVent ->
                if (theVent.hasAttribute('contact') && (theVent.currentContact == 'closed')) {
                    if (theVent.currentLevel < 98) theVent.setLevel(98)
                    //if (theVent.currentSwitch != 'on') theVent.on()
                    if (theVent.hasCommand('openContact') && (theVent.currentContact != 'open')) theVent.openContact()
                } else if (theVent.hasCommand('setLevel')) {
                    if (theVent.currentLevel < 98) theVent.setLevel(98)
                } 
                if (theVent.currentSwitch != 'on') theVent.on()
            }
            status = 'open'
        }
        sensorData << [vents:status]
	
        // register this room's sensor(s) with the thermostat for the appropriate program(s)
        anyInactive = false
        def sensorsRegistered = 0
		def sensor = theSensorDevices[0]
        def smartRoomStatus = ST ? sensor.currentValue('SmartRoom') : sensor.currentValue('SmartRoom', true)
        if (smartRoomStatus == 'inactive') anyInactive = true
        if (smartRoomStatus == 'enable') anyInactive = true		// we are turning on
        List notPrograms = getProgramsList() - settings.activeProgs
        if (needClimateChange(sensor, settings.activeProgs, notPrograms)) {
            String tid = sensor.currentValue('thermostatId').toString()
            boolean anyReserved = anyReservations(tid, 'programChange')
            if (!anyReserved || haveReservation(tid, 'programChange')) {
                // Nobody has a reservation, or the reservation is mine
                if (!anyReserved) makeReservation(tid, 'programChange')
                makeClimateChange(sensor, settings.activeProgs, notPrograms)
                if (!notPrograms) notPrograms = ['(none)']
                LOG("Sensor ${sensor.displayName} added to ${settings.activeProgs.toString()[1..-2]} and removed from ${notPrograms.toString()[1..-2]}",2,null,'info')
            } else {
                // somebody else has a reservation - we have to wait
                atomicState.pendedUpdates = [add: settings.activeProgs, remove: notPrograms]
                subscribe(sensor, 'climatesUpdated', programWaitHandler)
                if (!notPrograms) notPrograms = ['(none)']
                LOG("Delayed: Sensor ${sensor.displayName} will be added to ${settings.activeProgs.toString()[1..-2]} and removed from ${notPrograms.toString()[1..-2]} when pending changes complete",2,null,'info')
            }
        } else {
            // No changes required
            cancelReservation(tid, 'programChange')
        }
        // all set, mark the sensors' SmartRoom as active
        if (anyInactive) { sensorData << [SmartRoom:'active']; atomicState.SmartRoom = 'active'; }
        generateSensorsEvents( sensorData )
    }
    if (anyInactive) sendMessage("I just activated ${app.label}")
    
    LOG("Activated",3,null,'info')
}

void deactivateRoom() {
	LOG("Deactivating Smart Room", 3, null, 'info')
    def sensorData = [:]
    boolean anyActive = true
    atomicState.isSmartRoomActive = false
    
    //turn off the vents
    if (settings?.manageSmartVents && settings?.theVentApps) {
    	// Let the Smart Vents Helper handle everything
    	settings.theVentApps.each { appId ->
        	String appName = parent.getChildAppName(appId)
        	LOG("Requesting pause for ${appName?:appId}",3,null,'trace')
        	parent.pauseChildApp( appId, true)	// unpause if active, pause if not
        }
        sensorData << [SmartRoom:'inactive']	// Turn off Smart Room last, so Smart Vents knows to reset the vent state
        atomicState.SmartRoom = 'inactive'
    } else {
        String status = 'notused'
        def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
                        (settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                         (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
        if (theVents != []) {
            theVents.each { theVent ->
                ventOff(theVent)
            }
            status = 'closed'
        } 
        sensorData << [vents:status]

        //un-register this room's sensor(s) from the thermostat for the appropriate program(s)
        //and register this room's sensor(s) with the thermostat for the appropriate program(s)
        anyActive = false
        //boolean ST = isST
        def sensor = theSensorDevices[0]
        def smartRoomStatus = ST ? sensor.currentValue('SmartRoom') : sensor.currentValue('SmartRoom', true) 
        if (smartRoomStatus == 'active') anyActive = true 
        if (smartRoomStatus == 'disable') anyActive = true
        List notPrograms = getProgramsList() - settings.inactiveProgs
        if (needClimateChange(sensor, settings.inactiveProgs, notPrograms )) {
            String tid = sensor.currentValue('thermostatId').toString()
            boolean anyReserved = anyReservations( tid, 'programChange' )
            if (!anyReserved || haveReservation( tid, 'programChange' )) {
                // Nobody has a reservation, or the reservation is mine
                if (!anyReserved) makeReservation(tid, 'programChange')
                makeClimateChange(sensor, settings.inactiveProgs, notPrograms)
                if (!notPrograms) notPrograms = ['(none)']
                LOG("Sensor ${sensor.displayName} added to ${settings.inactiveProgs.toString()[1..-2]} and removed from ${notPrograms.toString()[1..-2]}",2,null,'info')
            } else {
                // somebody else has a reservation - we have to wait
                atomicState.pendedUpdates = [add: settings.inactiveProgs, remove: notPrograms]
                subscribe(sensor, 'climatesUpdated', programWaitHandler)
                if (!notPrograms) notPrograms = ['(none)']
                LOG("Delayed: Sensor ${sensor.displayName} will be added to ${settings.inactiveProgs.toString()[1..-2]} and removed from ${notPrograms.toString()[1..-2]} when pending changes complete",2,null,'info')
            }
        } else {
            // No changes required
            cancelReservation( tid, 'programChange' )
        }
        if (anyActive) { sensorData << [SmartRoom:'inactive']; atomicState.SmartRoom = 'inactive'; }
    }
    generateSensorsEvents(sensorData)
    atomicState.isRoomOccupied = false	// this gets turned on the first time motion is detected after the doors are closed
    if (anyActive) sendMessage("I just deactivated ${app.label} (Smart Room)")
    LOG("Deactivated",3,null,'info',false,false)
}
def programUpdateHandler(evt) {
	// Clear our reservation once we know that the Ecobee Cloud has updated our thermostat's climates
    cancelReservation(evt.device.currentValue('thermostatId') as String, 'programChange')
    unschedule(clearReservation)
    unsubscribe(evt.device)
    if (!settings?.tempDisable) subscribe(evt.device, 'temperature', changeHandler)
    def pendingRequest = atomicState.updateSensorRequest
    if (pendingRequest != null) {
    	atomicState.updateSensorRequest = null
	   	LOG("programUpdateHandler(): ${pendingRequest} operation completed",4,null,'debug')
    }
}
def programWaitHandler(evt) {
    unsubscribe(evt.device)
    if (!settings?.tempDisable) subscribe(evt.device, 'temperature', changeHandler)
    String tid = evt.device.currentValue('thermostatId') as String
    def count = countReservations(tid, 'programChange')
    if ((count > 0) && !haveReservation( tid, 'programChange' )) {
    	atomicState.programWaitCounter = 0
    	runIn(5, checkReservations, [overwrite: true, data: [tid:tid, type:'programChange']])
        LOG("programWaitHandler(): There are still ${count} reservations for 'programChange', waiting...", 3, null, 'debug')
    } else {
    	makeReservation(tid, 'programChange')
        LOG("programWaitHandler(): 'programChange' reservation secured, sending pended updates", 3, null, 'debug')
    	doPendedUpdates()
    }
}
void checkReservations(data) {
    def count = countReservations(data.tid, data.type)
    def counter = atomicState.programWaitCounter
	if ((count > 0) && !haveReservation(data.tid, data.type)  && (counter <= 60)) {	// Try for five minutes...
    	// Need to wait longer
        runIn(5, checkReservations, [overwrite: true, data: [tid: (data.tid), type: (data.type)]])
        counter++
        atomicState.programWaitCounter = counter
        if ((counter % 12) == 0) runIn(2, doRefresh, [overwrite: true])	// force a refresh every minute if we don't get updated
        LOG("checkReservations(): There are still ${count} reservations for 'programChange', waiting...", 3, null, 'debug')
    } else {
    	makeReservation(data.tid, data.type)
        atomicState.programWaitCounter = 0
        LOG("checkReservation()(): 'programChange' reservation secured, sending pended updates", 3, null, 'debug')
        doPendedUpdates()
    }
}
void clearReservation() {
	settings.theSensors.each { sensor ->
        // N.B., there can only be 1 ES Sensor
        if (sensor.hasCommand('updateSensorPrograms')) {
        	def tid = sensor.currentValue('thermostatId') as String
            cancelReservation(tid, 'programChange')
        }
    }
}
void doRefresh() {
	settings?.ttheSensorDevices[0]?.doRefresh(true) // do a forced refresh
}
void doPendedUpdates() {
	LOG("doPendedUpdates(): ${atomicState.pendedUpdates}",4,null,'debug')
    def updates = atomicState.pendedUpdates
    if (updates?.remove || updates?.add) {
    	// Find the sensor
        settings.theSensors.each { sensor ->
    		// N.B., there can only be 1 ES Sensor
            if (sensor.hasCommand('updateSensorPrograms')) {
            	if (needClimateChange( sensor, updates.add, updates.remove )) {
					makeClimateChange( sensor, updates.add, updates.remove )
                } else {
                    // Nothing to do - release the reservation now
                    cancelReservation(tid, 'programChange')
                }
            }
        }
        atomicState.pendedUpdates = null
    }            	
}
def makeClimateChange( sensor, adds, removes ) {
    subscribe( sensor, 'activeClimates', programUpdateHandler )
    atomicState.updateSensorRequest = adds ? 'enroll' : 'unenroll'
    sensor.updateSensorPrograms( adds, removes)
    runIn(150, clearReservation, [overwrite: true])
    if (!adds) {
        LOG("Sensor ${sensor.displayName} removed from ${removes.toString()[1..-2]}",3,null,'info')
    } else {
    	if (!removes) removes = ['(none)']
        LOG("Sensor ${sensor.displayName} added to ${adds.toString()[1..-2]} and removed from ${removes.toString()[1..-2]}",3,null,'info')
    }
    // programUpdateHandler will release the reservation for us
}
boolean needClimateChange(sensor, List adds, List removes) {
	if (!adds && !removes) return false
    String ac = ST ? sensor.currentValue('activeClimates') : sensor.currentValue('activeClimates', true)
    def activeClimates = ac ? ((ac == '[]') ? [] : ac[1..-2].tokenize(', ').sort(false)) : []
    log.debug "activeClimates: ${activeClimates}"
    boolean updatesToDo = false
    if (!activeClimates) {
        // Easy one: no active climates, and we have climates to add
        if (adds) {
            updatesToDo = true
        }
    } else { // we have some activeClimates - do we need to adjust them?
        if (!adds && removes) {	
            // Easy one: there are active climates, and we aren't adding any, thus we know that we are removing ALL of them
            updatesToDo = true
        } else {
            // Hardest one: we have some active climates, and we have some adds and some removes to do - figure out if we need to change anything
            activeClimates.each { climate ->
                if (!updatesToDo) {
                    if (adds) {
                        // are there any active climates that we don't want active?
                        if (!adds.contains(climate)) updatesToDo = true		// need to remove at least one
                    } else if (removes) {
                        // or any active climates that we want inactive?
                        if (removes.contains(climate)) updatesToDo = true	// need to remove at least one
                    }
                }
            }
        }
    }
	return updatesToDo
}

def climateUpdateHandler(evt) {
	log.debug "climateUpdateHandler(): $evt.name = $evt.value @ ${now()}"
    log.debug "evt.device: $evt.device, $evt.device.displayName, ${evt.device.currentValue("thermostatId")}"
    unsubscribe("climateUpdated")
    cancelReservation(evt.device.currentValue("thermostatId"), "climateChange")
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
                // motion event happens within the doorOpenMinutes then we could activate the room...
                if (true) {
                	if (!theWindows || !theWindows.currentContact.contains('open')) {
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
	LOG("generating ${dataMap} events for ${theSensorDevices}",3,null,'trace')
	theSensorDevices.each {
    	parent.generateChildEvent( it.deviceNetworkId, dataMap)
    }
}

void sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "trace")
   // boolean ST = isST
    if (settings.notify) {
        String msg = "${atomicState.appDisplayName} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (ST) {
			if (settings.notifiers) {
            	if (settings.echoAnnouncements) {
                    List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                    List notEcho = echo ? settings.notifiers - echo : settings.notifiers

                    // If we have multiple Echo Speak device targets, get them all to speak at once
                    List echoDeviceObjs = []
                    if (echo) {
                        if (echo?.size() > 1) {
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
	// Always send to Hello Home / Location Event log
	if (ST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", description: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
}

def getSensorPrograms(sensor) {
    def cl = sensor.currentValue('climatesList')
    return (cl ? ((cl == '[]') ? ['Away', 'Home', 'Sleep'] : cl[1..-2].tokenize(', ').sort(false)) : ['Away', 'Home', 'Sleep'])
}
def getProgramsList() { 
	//boolean ST = isST
	def programs = []
	if (settings.theSensorDevices) {
    	settings.theSensorDevices.each { sensor ->
            getSensorPrograms(sensor).each { prog ->
            	if (!programs || !programs.contains(prog)) programs << prog
            }
        }
    }
	return programs.sort(false)
}
def getEcobeeSensorsList() { return parent.getEcobeeSensors().sort { it.value } }    

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
void updateMyLabel() {
	boolean ST = isST
	
	String flag
	if (ST) {
    	def opts = [' (paused', ' (active', ' (inactive', ' (disabled', ' (default']
		opts.each {
			if (!flag && app.label.contains(it)) flag = it
		}
	} else {
		flag = '<span '
	}
	
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
    String smartRoom = atomicState.SmartRoom

    String newLabel
	if (settings.tempDisable) {
    	smartRoom = ' (paused)'
		newLabel = myLabel + ( ST ? smartRoom : '<span style="color:red">' + smartRoom + '</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (SmartRoom != 'default') {
		newLabel = myLabel + ( ST ? ' ('+smartRoom+')' : '<span style="color:green"> (' + smartRoom + ')</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
    	// display nothing if SmartRoom is 'default'
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
