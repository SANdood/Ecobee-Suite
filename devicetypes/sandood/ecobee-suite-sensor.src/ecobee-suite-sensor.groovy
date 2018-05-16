/**
 *  Ecobee Sensor
 *
 *  Copyright 2015 Juan Risso
 *	Copyright 2017-2018 Barry A. Burke
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
 *  See Changelog for change history
 *
 *	1.0.0  - Preparation for General Release
 *	1.0.1  - Added Offline handling (power out, network down, etc.)
 *	1.0.2  - Changed handling of online/offline
 *	1.0.3  - Added Health Check support
 *	1.0.4  - Revised poll/refresh/ping commands
 *	1.0.5  - Improved Health Check reliability
 *  1.0.6  - Restored Fahrenheit 451 ("offline" display)
 *	1.0.7  - Fixed indiscriminate Polling issue
 *  1.0.8  - Setting decimals=0 now also applies to value displayed in device lists
 *	1.0.9  - Uses new Refresh icon
 *	1.2.0  - Sync revision number with new holdHours/holdAction updates
 *	1.2.1  - Reinstated default icon for default Temperature tile
 *	1.2.2  - Added new Program icons, Awake/Wakeup attributes (not currently displayed)
 *	1.3.0  - Major Release: renamed and moved to "sandood" namespace
 *	1.4.0  - Major Release: renamed devices also
 *	1.4.01 - Added VersionLabel display
 *	1.4.02 - Fixed getMyId so that add/delete works properly
 *	1.4.03 - Fixed a typo
 *	1.4.04 - Updated for delayed add/delete function
 *	1.4.05 - Fixed add/deleteSensorFromProgram
 *	1.4.06 - Removed extra 'inactiveLabel: false', changed main() definition
 *
 */

def getVersionNum() { return "1.4.06" }
private def getVersionLabel() { return "Ecobee Suite Sensor, Version ${getVersionNum()}" }
private def programIdList() { return ["home","away","sleep"] } // we only support these program IDs for addSensorToProgram()

metadata {
	definition (name: "Ecobee Suite Sensor", namespace: "sandood", author: "Barry A. Burke (storageanarchy@gmail.com)") {
		capability "Sensor"
		capability "Temperature Measurement"
		capability "Motion Sensor"
		capability "Refresh"
		// capability "Polling"
        capability "Health Check"
		
		attribute "decimalPrecision", "number"
		attribute "temperatureDisplay", "string"
        attribute "Home", "string"
        attribute "Away", "string"
        attribute "Sleep", "string"
        attribute "Awake", "string"
        attribute "Wakeup", "string"       
        attribute "thermostatId", "string"
        attribute "doors", "string"
        attribute "windows", "string"
        attribute "vents", "string"
        attribute "SmartRoom", "string"
        attribute "currentProgramName", "string"
        attribute "humidity", "string"
        
        command "noOp"					// these are really for internal use only
        command "enableSmartRoom"
        command "disableSmartRoom"
        command "doRefresh"
        
        command "addSensorToHome"
        command "addSensorToAway"
        command "addSensorToSleep"
        command "deleteSensorFromHome"
        command "deleteSensorFromAway"
        command "deleteSensorFromSleep"
        command "removeSensorFromHome"
        command "removeSensorFromAway"
        command "removeSensorFromSleep"
	}

	simulator {
		// TODO: define status and reply messages here
	}
    
/*	COLOR REFERENCE

		backgroundColor:"#d28de0"		// ecobee purple/magenta
        backgroundColor:"#66cc00"		// ecobee green
		backgroundColor:"#2db9e7"		// ecobee snowflake blue
		backgroundColor:"#ff9c14"		// ecobee flame orange
        backgroundColor:"#00A0D3"		// SmartThings new "good" blue (replaced green)
*/    

	tiles(scale: 2) {
		multiAttributeTile(name:"temperatureDisplay", type: "generic", width: 6, height: 4){
			tileAttribute ("device.temperatureDisplay", key: "PRIMARY_CONTROL") {
				attributeState("temperature", label:'\n${currentValue}',
					backgroundColors: getTempColors(), defaultState: true)
			}
			tileAttribute ("device.motion", key: "SECONDARY_CONTROL") {
                attributeState "active", action:"noOp", nextState: "active", label:"Motion", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_motion.png"
				attributeState "inactive", action: "noOp", nextState: "inactive", label:"No Motion", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_nomotion.png"
            	attributeState "unknown", action: "noOp", label:"Off\nline", nextState: "unknown", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_noconnection.png"
             	attributeState "offline", action: "noOp", label:"Off\nline", nextState: "offline", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_noconnection.png"
 				attributeState "not supported", action: "noOp", nextState: "not supported", label: "N/A", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/notsupported_x.png"
            }
		}

        valueTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: true, decoration: 'flat') {
        	// Use the first version below to show Temperature in Device History - will also show Large Temperature when device is default for a room
            // 		The second version will show icon in device lists
			//state("default", label:'${currentValue}°', unit:"dF", backgroundColors: getTempColors(), defaultState: true)
            state("default", label:'${currentValue}°', unit:"dF", backgroundColors: getTempColors(), defaultState: true, icon:'st.Weather.weather2')
		}
        
        standardTile("motion", "device.motion", width: 2, height: 2, decoration: "flat") {
			state "active", action:"noOp", nextState: "active", label:"Motion", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_motion.png"
			state "inactive", action: "noOp", nextState: "inactive", label:"No Motion", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_nomotion.png"
            state "unknown", action: "noOp", label:"Offline", nextState: "unknown", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_noconnection.png"
            state "not supported", action: "noOp", nextState: "not supported", label: "N/A", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/notsupported_x.png"
		}

        standardTile("refresh", "device.doRefresh", width: 1, height: 1, decoration: "flat") {
            state "refresh", action:"doRefresh", nextState: 'updating', label: "Refresh", defaultState: true, icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/ecobee_refresh_green.png"
            state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}

		standardTile("Home", "device.Home", width: 1, height: 1, decoration: "flat") {
			state 'on', action:"deleteSensorFromHome", nextState: 'updating', label:'on', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue_solid.png"
			state 'off', action: "addSensorToHome", nextState: 'updating', label:'off', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png"
            state 'updating', label:"Working...", icon: "st.motion.motion.inactive"
		}
        
        standardTile("Away", "device.Away", width: 1, height: 1, decoration: "flat") {
			state 'on', action:"deleteSensorFromAway", nextState: 'updating', label:'on', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue_solid.png"
			state 'off', action: "addSensorToAway", nextState: 'updating', label:'off', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png"
            state 'updating', label:"Working...", icon: "st.motion.motion.inactive"
		}

        standardTile("Sleep", "device.Sleep", width: 1, height: 1, decoration: "flat") {
            state 'on', action:"deleteSensorFromSleep", nextState: 'updating', label:'on', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue_solid.png"
			state 'off', action: "addSensorToSleep", nextState: 'updating', label:'off', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue.png"
            state 'updating', label:"Working...", icon: "st.motion.motion.inactive"
		}
        
        standardTile('vents', 'device.vents', width: 1, height: 1, decoration: 'flat') {
        	state 'default', label: '', action: 'noOp', nextState: 'default', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", backgroundColor:"#ffffff"
            state 'notused', label: 'vents', action: 'noOp', nextState: 'notused', icon: "st.vents.vent", backgroundColor:"#ffffff"
            state 'open', label: 'open', action: 'noOp', nextState: 'open', icon: "st.vents.vent-open", backgroundColor:"#ff9c14"
            state 'closed', label: 'closed', action: 'noOp', nextState: 'closed', icon: "st.vents.vent", backgroundColor:"#d28de0"
        }

        standardTile('doors', 'device.doors', width: 1, height: 1, decoration: 'flat') {
        	state 'default', label: '', action: 'noOp', nextState: 'default', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", backgroundColor:"#ffffff"
            state 'open', label: 'open', action: 'noOp', nextState: 'open', backgroundColor:"#00A0D3", icon: "st.contact.contact.open"
            state 'closed', label: 'closed', action: 'noOp', nextState: 'closed', backgroundColor:"#d28de0", icon: "st.contact.contact.closed"
        }
        
        standardTile('windows', 'device.windows', width: 1, height: 1, decoration: 'flat') {
        	state 'default', label: '', action: 'noOp', nextState: 'default', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", backgroundColor:"#ffffff"
            state 'notused', label: 'windows', action: 'noOp', nextState: 'notused', icon: "st.Home.home9", backgroundColor:"#ffffff"
            state 'open', label: 'open', action: 'noOp', nextState: 'open', icon: "st.Home.home9", backgroundColor:"#d28de0"
            state 'closed', label: 'closed', action: 'noOp', nextState: 'closed', icon: "st.Home.home9", backgroundColor:"#00A0D3"
        }
        
         standardTile('SmartRoom', 'device.SmartRoom', width: 1, height: 1, decoration: 'flat') {
        	state 'default', label: '', action: 'noOp', nextState: 'default', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", backgroundColor:"#ffffff"
            state 'active', label: 'active', action: 'disableSmartRoom', nextState: "disable", icon: "st.Home.home1", backgroundColor:"#00A0D3"
            state 'inactive', label: 'inactive', action: 'enableSmartRoom', nextState: "enable", icon: "st.Home.home2", backgroundColor:"#d28de0"
            state 'disabled', label: 'disabled', action: 'enableSmartRoom', nextState: "enable", icon: "st.Home.home2", backgroundColor:"#ff9c14"	// turned off in Smart Room settings
            state 'enable', label:"Working...", icon: "st.motion.motion.inactive", backgroundColor:"#ffffff"
            state 'disable', label:"Working...", icon: "st.motion.motion.inactive", backgroundColor:"#ffffff"
        }
        
        standardTile('blank', 'device.blank', width: 1, height: 1, decoration: 'flat') {
        	state 'default', label: '', action: 'noOp', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png"
        }
        
        standardTile("currentProgramIcon", "device.currentProgramName", height: 2, width: 2, decoration: "flat") {
			state "Home", 				action:"noOp", 	nextState:'Home', 				label: 'Home', 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png"
			state "Away", 				action:"noOp", 	nextState:'Away', 				label: 'Away', 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png"
            state "Sleep", 				action:"noOp", 	nextState:'Sleep', 				label: 'Sleep', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue.png"
            state "Awake", 				action:"noOp", 	nextState:'Awake', 				label: 'Awake', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake.png"
            state "Wakeup", 			action:"noOp", 	nextState:'Wakeup', 			label: 'Wakeup', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake.png"
			state "Auto", 				action:"noOp", 	nextState:'Auto', 				label: 'Auto', 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
			state "Auto Away", 			action:"noOp", 	nextState:'Auto Away', 			label: 'Auto Away', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png" // Fix to auto version
            state "Auto Home", 			action:"noOp", 	nextState:'Auto Home', 			label: 'Auto Home', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png" // Fix to auto
            state "Hold", 				action:"noOp", 	nextState:"Hold", 				label: "Hold Activated", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
            state "Hold: Fan", 			action:"noOp", 	nextState:"Hold: Fan", 			label: "Hold: Fan", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
            state "Hold: Fan On", 		action:"noOp", 	nextState:'Hold: Fan on', 		label: "Hold: Fan On", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_blue.png"
            state "Hold: Fan Auto",		action:"noOp", 	nextState:'Hold: Fan Auto',		label: "Hold: Fan Auto", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_blue.png"
            state "Hold: Circulate",	action:"noOp", 	nextState:'Hold: Circulate',	label: "Hold: Circulate",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1_blue..png"
			state "Hold: Home", 		action:"noOp", 	nextState:'Hold: Home', 		label: 'Hold: Home', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue_solid.png"
            state "Hold: Away", 		action:"noOp", 	nextState:'Hold: Away', 		label: 'Hold: Away', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue_solid.png"
            state "Hold: Sleep", 		action:"noOp", 	nextState:'Hold: Sleep', 		label: 'Hold: Sleep',		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue_solid.png"
      		state "Vacation", 			action:"noOp",	nextState:'Vacation', 			label: 'Vacation', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_vacation_blue_solid.png"
			state "Offline", 			action:"noOp",	nextState:'Offline', 			label: 'Offline', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_black_dot.png"
            state "Hold: Temp", 		action:'noOp',	nextState: 'Hold: Temp', 		label: 'Hold: Temp', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/thermometer_hold.png"
			state "default", 			action:"noOp", 	nextState:'default', 			label:'${currentValue}', 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
		}

		main (['temperature']) //, "temperatureDisplay",])
		details(   ['temperatureDisplay',
        			'currentProgramIcon', 	'doors', 'windows', 'vents', 'SmartRoom',
                    						'Home',  'Away',  'Sleep', 'refresh'])
	}
    preferences {
       	input "dummy", "text", title: "${getVersionLabel()}", description: "."
	}
}

void refresh(force=false) {
    def tstatId = device.currentValue('thermostatId')
	LOG( "Refreshed - executing parent.pollChildren(${tstatId}) ${force?'(forced)':''}", 2, this, 'info')
	parent.pollChildren(tstatId,force)		// we have to poll our Thermostat to get updated
}

def doRefresh() {
    refresh(state.lastDoRefresh?((now()-state.lastDoRefresh)<6000):false)
    sendEvent(name: 'doRefresh', value: 'refresh', isStateChange: true, displayed: false)
    state.lastDoRefresh = now()	// reset the timer after the UI has been updated
}

void poll() {
	def tstatId = device.currentValue('thermostatId')
	LOG( "Polled - executing parent.pollChildren(${tstatId})", 2, this, 'info')
	parent.pollChildren(tstatId,false)		// we have to poll our Thermostat to get updated
}

// Health Check will ping us based on the frequency we configure in Ecobee (Connect) (derived from poll & watchdog frequency)
void ping() {
	def tstatId = device.currentValue('thermostatId')
	LOG( "Pinged - executing parent.pollChildren(${tstatId})", 2, this, 'info')
	parent.pollChildren(tstatId,true)		// we have to poll our Thermostat to get updated
}

void installed() {
	updated()
}

void updated() {
	sendEvent(name: 'checkInterval', value: 3900, displayed: false, isStateChange: true)  // 65 minutes (we get forcePolled every 60 minutes
}

void noOp() {}

def generateEvent(Map results) {
	LOG("generateEvent(): parsing data ${results}",3,null,'trace')
	def tempScale = getTemperatureScale()
    def precision = device.currentValue('decimalPrecision')
    if (!precision) precision = (tempScale == 'C') ? 1 : 0
    def isConnected = (device.currentValue('currentProgramName') != 'Offline')

	if(results) {
		String tempDisplay = ''
		results.each { name, value ->			
			def linkText = getLinkText(device)
			def isChange = false
			def isDisplayed = true
			def event = [:]  // [name: name, linkText: linkText, handlerName: name]
           
			String sendValue = value as String
			if (name=='temperature')  {
                if ((sendValue == 'unknown') || !isConnected) {
                	// We are OFFLINE
                    // LOG( "Warning: Remote Sensor (${name}) is OFFLINE. Please check the batteries or move closer to the thermostat.", 2, null, 'warn')
                    sendEvent( name: 'temperatureDisplay', linkText: linkText, value: '451°', handlerName: "temperatureDisplay", descriptionText: 'Fahrenheit 451', /* isStateChange: true, */ displayed: false)
					event = [name: name, linkText: linkText, descriptionText: "Sensor is Offline", handlerName: name, value: sendValue, isStateChange: true, displayed: true]
                } else {
                	// must be online  
					// isChange = isStateChange(device, name, sendValue)
                    isChange = true // always send the temperature, else HealthCheck will think we are OFFLINE
                    
                    // Generate the display value that will preserve decimal positions ending in 0
                    if (isChange) {
                    	if (precision == 0) {
                    		tempDisplay = value.toDouble().round(0).toInteger().toString() + '°'
                            sendValue = value.toDouble().round(0).toInteger()								// Remove decimals in device lists also
                    	} else {
							tempDisplay = String.format( "%.${precision.toInteger()}f", value.toDouble().round(precision.toInteger())) + '°'
                    	}
                        sendEvent( name: 'temperatureDisplay', linkText: linkText, value: "${tempDisplay}", handlerName: 'temperatureDisplay', descriptionText: "Display temperature is ${tempDisplay}", isStateChange: true, displayed: false)
						event = [name: name, linkText: linkText, descriptionText: "Temperature is ${tempDisplay}", handlerName: name, value: sendValue, isStateChange: true, displayed: true]
                    }
                }
			} else if (name=='motion') {        
            	if ( (sendValue == 'unknown') || !isConnected) {
                	// We are OFFLINE
                    LOG( "Warning: This Sensor is OFFLINE.", 2, null, 'warn')
                    sendValue = 'unknown'
                }
                
				isChange = isStateChange(device, name, sendValue.toString())
				if (isChange) event = [name: name, linkText: linkText, descriptionText: "Motion is ${sendValue}", handlerName: name, value: sendValue, isStateChange: true, displayed: true]
			} else if (name=='currentProgramName') {
            	isChange = isStateChange(device, name, sendValue)
                if (isChange) {
                	isConnected = (sendValue != 'Offline')		// update if it changes
					event = [name: name, linkText: linkText, value: sendValue, descriptionText: 'Program is '+sendValue.replaceAll(':',''), isStateChange: true, displayed: true]
                }
            } else if (name=='checkInterval') {
            	event = [name: name, value: sendValue, /*isStateChange: true,*/ displayed: false]
            } else { // must be one of Home, Away, Sleep, vents, doors, windows, SmartRoom, decimalPrecision or thermostatId
				isChange = isStateChange(device, name, sendValue)
				if (isChange) event = [name: name, linkText: linkText, handlerName: name, value: sendValue, isStateChange: true, displayed: false]
            }
			if (event != [:]) sendEvent(event)
		}
		//if (tempDisplay) {
		//	sendEvent( name: "temperatureDisplay", linkText: linkText, value: "${tempDisplay}", handlerName: "temperatureDisplay", descriptionText: "Display temperature is ${tempDisplay}", isStateChange: true, displayed: false)
		//}
	}
}

//generate custom mobile activity feeds event
def generateActivityFeedsEvent(notificationMessage) {
	sendEvent(name: "notificationMessage", value: "$device.displayName $notificationMessage", descriptionText: "$device.displayName $notificationMessage", displayed: true)
}

void addSensorToHome() { addSensorToProgram('home') }
void addSensorToAway() { addSensorToProgram('away') }
void addSensorToSleep() { addSensorToProgram('sleep') }

def addSensorToProgram(programId) {
	LOG("addSensorToProgram(${programId}) - entry",3,this,'trace')
	def result = false
	if (programIdList().contains(programId.toLowerCase())) {
    	if (device.currentValue(programId.capitalize()) != 'on') {
    		result = parent.addSensorToProgram(this, device.currentValue('thermostatId'), getSensorId(), programId.toLowerCase())
			if (result) {
    			sendEvent(name: "${programId.capitalize()}", value: 'on', descriptionText: "Sensor added to ${programId.capitalize()} program", isStateChange: true, displayed: true)
                runIn(5, refresh, [overwrite: true])
            } else {
            	sendEvent(name: "${programId.capitalize()}", value: 'off', isStateChange: true, displayed: false)
            }
       	} else {
       		result = true
    	}
    } else {
    	LOG("addSensorToProgram(${programId}) - Bad argument, must be one of ${programIdList}",1,null,'error')
        result = false
    }
    
    LOG("addSensorToProgram(${programId}) - ${result?'Succeeded':'Failed'}",2,this,'info')
    return result
}

void deleteSensorFromHome() { deleteSensorFromProgram('home') }
void deleteSensorFromAway() { deleteSensorFromProgram('away') }
void deleteSensorFromSleep() { deleteSensorFromProgram('sleep') }
void removeSensorFromHome() { deleteSensorFromProgram('home') }
void removeSensorFromAway() { deleteSensorFromProgram('away') }
void removeSensorFromSleep() { deleteSensorFromProgram('sleep') }

def deleteSensorFromProgram(programId) {
	LOG("deleteSensorFromProgram(${programId}) - entry",3,this,'trace')
    def result = false
	if (programIdList().contains(programId.toLowerCase())) {
    	if (device.currentValue(programId.capitalize()) != 'off') {
    		result = parent.deleteSensorFromProgram(this, device.currentValue('thermostatId'), getSensorId(), programId.toLowerCase())
			if (result) {	
    			sendEvent(name: "${programId.capitalize()}", value: 'off', desciptionText: "Sensor removed from ${programId.capitalize()} program", isStateChange: true, displayed: true)
            } else {
            	sendEvent(name: "${programId.capitalize()}", value: 'on', isStateChange: true, displayed: false)
            }
        	runIn(5, refresh, [overwrite: true]) 
       	} else {
        	result = true	// not in this Program anyway
        }
    } else {
    	LOG("deleteSensorFromProgram(${programId}) - Bad argument, must be one of ${programIdList}",1,null,'error')
        result = false
    }
    
   	LOG("deleteSensorFromProgram(${programId}) - ${result?'Succeeded':'Failed'}",2,this,'info')
    return result
}

void enableSmartRoom() {
	sendEvent(name: "SmartRoom", value: "enable", isSateChange: true, displayed: false)		// the Smart Room SmartApp should be watching for this
}

void disableSmartRoom() {
	sendEvent(name: "SmartRoom", value: "disable", isSateChange: true, displayed: false)		// the Smart Room SmartApp should be watching for this
}

private String getSensorId() {
	def myId = []
    myId = device.deviceNetworkId.split('-') as List
    return (myId[2])
}

private debugLevel(level=3) {
	Integer debugLvlNum = parent.settings.debugLevel?.toInteger() ?: 3
    return ( debugLvlNum >= level?.toInteger() )
}

private def LOG(message, level=3, child=null, logType="debug", event=false, displayEvent=false) {
	def prefix = ""
	if ( parent.settings.debugLevel?.toInteger() == 5 ) { prefix = "LOG: " }
	if ( debugLevel(level) ) { 
    	log."${logType}" "${prefix}${message}"
        if (event) { debugEvent(message, displayEvent) }        
	}    
}

private def debugEvent(message, displayEvent = false) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent (results)
}
	
def getTempColors() {
	def colorMap

	colorMap = [
		// Celsius Color Range
/*		[value: 0, color: "#1e9cbb"],
		[value: 15, color: "#1e9cbb"],
		[value: 19, color: "#1e9cbb"],

		[value: 21, color: "#44b621"],
		[value: 22, color: "#44b621"],
		[value: 24, color: "#44b621"],

		[value: 21, color: "#d04e00"],
		[value: 35, color: "#d04e00"],
		[value: 37, color: "#d04e00"],
		// Fahrenheit Color Range
		[value: 40, color: "#1e9cbb"],
		[value: 59, color: "#1e9cbb"],
		[value: 67, color: "#1e9cbb"],

		[value: 69, color: "#44b621"],
		[value: 72, color: "#44b621"],
		[value: 74, color: "#44b621"],

		[value: 76, color: "#d04e00"],
		[value: 95, color: "#d04e00"],
		[value: 99, color: "#d04e00"],
 */
 							[value: 0, color: "#153591"],
							[value: 7, color: "#1e9cbb"],
							[value: 15, color: "#90d2a7"],
							[value: 23, color: "#44b621"],
							[value: 28, color: "#f1d801"],
							[value: 35, color: "#d04e00"],
							[value: 37, color: "#bc2323"],
							// Fahrenheit
							[value: 40, color: "#153591"],
							[value: 44, color: "#1e9cbb"],
							[value: 59, color: "#90d2a7"],
							[value: 74, color: "#44b621"],
							[value: 84, color: "#f1d801"],
							[value: 95, color: "#d04e00"],
							[value: 96, color: "#bc2323"],

        [value: 451, color: "#ff4d4d"] // Nod to the book and temp that paper burns. Used to catch when the device is offline
	]
}

def getStockTempColors() {
	def colorMap
    
    colorMap = [
    	[value: 32, color: "#153591"],
        [value: 44, color: "#1e9cbb"],
        [value: 59, color: "#90d2a7"],
        [value: 74, color: "#44b621"],
        [value: 84, color: "#f1d801"],
        [value: 92, color: "#d04e00"],
        [value: 98, color: "#bc2323"]
    ]       
}
