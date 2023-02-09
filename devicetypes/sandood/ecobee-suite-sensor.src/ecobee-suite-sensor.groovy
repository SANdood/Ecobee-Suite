/**
 *  Ecobee Sensor
 *
 *  Copyright 2015 Juan Risso
 *	Copyright 2017-2020 Barry A. Burke
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
 * <snip>
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - nonCached currentValue() on HE
 *	1.7.02 - Fixing private method issue caused by grails
 *	1.7.03 - Register new health check; auto reload new versions, avoid Health Check for test device install
 *	1.7.04 - Added importUrl for HE IDE
 *	1.7.05 - Optimized isST
 *	1.7.06 - Fixed importUrl for HE
 *	1.7.07 - Added ability to add/delete sensor from ANY Named program/schedule/climate
 *	1.7.08 - Optimized generateEvents() tally
 *	1.7.09 - Ornamented command arguments for HE
 *	1.7.10 - Additional Hubitat optimizations
 *	1.7.11 - New updateSensorPrograms - add/remove multiple programs atomically
 *	1.7.12 - Added (hidden) ***Updated timestamps (so Apps can detect changes)
 *	1.8.00 - Release number synchronization
 *	1.8.01 - General Release
 *	1.8.02 - Better Health Check integration (ST)
 *	1.8.03 - Added debugLevel attribute
 *	1.8.04 - New SHPL, using Global Fields instead of State
 *	1.8.05 - Fixed icon for Samsung Smart Things mobile app (the new one)
 *	1.8.06 - Clean up null vs. 'null' attributes - HE doesn't overwrite existing attrs with null
 *	1.8.07 - HOTFIX: DeviceWatch fix for hubless ST locations 
 *	1.8.08 - Display temperature as default for the new Samsung app
 *	1.9.00 - Removed all ST code
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum() 		{ return "1.9.00" }
String getVersionLabel() 	{ return "Ecobee Suite Sensor, version ${getVersionNum()} on ${getPlatform()}" }
def programIdList() 		{ return ["home","away","sleep"] } // we only support these program IDs for addSensorToProgram() - better to use the Name


metadata {
	definition (
        name:         	"Ecobee Suite Sensor", 
        namespace:    	"sandood", 
        author:       	"Barry A. Burke (storageanarchy@gmail.com)",
        //mnmn:         "SmartThings",          		// for the new Samsung (Connect) app
        //vid:          "generic-motion",        		// for the new Samsung (Connect) app
        //ocfDeviceType:"x.com.st.d.sensor.motion",  	// "x.com.st.d.sensor.multifunction", //["oic.r.humidity", "x.com.st.d.sensor.moisture", "x.com.st.d.sensor.temperature", "x.com.st.d.sensor.motion"],
		mnmn: 			"SmartThingsCommunity", 				// for the new Samsung (Connect) app 
		vid: 			"4a31c6b1-57cf-3591-bc17-dfb07e1d9641", // for the new Samsung (Connect) app 
		ocfDeviceType: 	"x.com.st.d.sensor.temperature", 
		cstHandler: 	true,
        importUrl:    	"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/devicetypes/sandood/ecobee-suite-sensor.src/ecobee-suite-sensor.groovy"
    ) 
    {	
    	capability "Health Check"
		capability "Temperature Measurement"
        capability "Relative Humidity Measurement"		// Thermostat-as-Sensor only (otherwise we never actually sendEvent 'humidity')
		capability "Motion Sensor"
		capability "Sensor"
		capability "Refresh"
        
		
		attribute "Awake", 'STRING'
		attribute "Away", 'STRING'
        attribute "Fan Only", 'STRING'
		attribute "Home", 'STRING'
		attribute "Sleep", 'STRING'
		attribute "SmartRoom", 'STRING'
		attribute "Wakeup", 'STRING'
        
        attribute "activeClimates", 'JSON_OBJECT'
        attribute "climatesList", "JSON_OBJECT"
        attribute "currentProgram", "STRING"
		attribute "currentProgramName", 'STRING'
		attribute "decimalPrecision", 'NUMBER'
        attribute "debugLevel", "NUMBER"
		attribute "doors", 'STRING'
		attribute "humidity", 'NUMBER'
		attribute "occupancy", 'ENUM', ['occupied', 'vacant']
        attribute "programsList", 'STRING'
		attribute "temperatureDisplay", 'STRING'
		attribute "thermostatId", 'STRING'
		attribute "vents", 'STRING'
		attribute "windows", 'STRING'
        
		attribute "programUpdated", 'NUMBER'
        attribute "curClimRefUpdated", 'NUMBER'
        attribute "climatesUpdated", 'NUMBER'
        attribute "scheduleUpdated", 'NUMBER'
        attribute "eventsUpdated", 'NUMBER'
		
		command "addSensorToProgram", 		[[name:'Program Name*', type:'STRING', description:'Add sensor to this Program Name']]
		command "deleteSensorFromProgram", 	[[name:'Program Name*', type:'STRING', description:'Delete sensor from this Program Name']]
        
	// These commands are all really internal-use only
		command "addSensorToAway", 				[]
		command "addSensorToHome", 				[]
		command "addSensorToSleep", 			[]
		command "deleteSensorFromAway", 		[]
		command "deleteSensorFromHome", 		[]
		command "deleteSensorFromSleep", 		[]
		command "disableSmartRoom", 			[]
		command "doRefresh", 					[]
		command "enableSmartRoom", 				[]
		command "noOp", 						[]
		command "removeSensorFromAway", 		[]
		command "removeSensorFromHome", 		[]
       	command "removeSensorFromProgram",	[[name:'Program Name*', type:'STRING', description:'Remove sensor from this Program Name']]
		command "removeSensorFromSleep", 		[]
        command "addSensorToPrograms"
        command "deleteSensorFromPrograms"
        command "removeSensorFromPrograms"
        command "updateSensorPrograms"				// arguments: (activeList, inactiveList)
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
	LOG( "Pinged - executing parent.pollChildren(${tstatId})", 2, null, 'info')
    sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
	sendEvent(name: "healthStatus", value: "online")
	parent.pollChildren(tstatId,true)		// we have to poll our Thermostat to get updated
}

void installed() {
	LOG("${device.label} being installed",2,null,'info')
    if (device.label?.contains('TestingForInstall')) return	// we're just going to be deleted in a second...
	updated()
}

void uninstalled() {
	LOG("${device.label} being uninstalled",2,null,'info')
}

void updated() {
	state?.hubPlatform = null
	getHubPlatform()
	LOG("${getVersionLabel()} updated",1,null,'info')
	state.version = getVersionLabel()
    updateDataValue("myVersion", getVersionLabel())
	
	if (!device.displayName.contains('TestingForInstall')) {
		// Try not to get hung up in the Health Check so that ES Manager can delete the temporary device
		sendEvent(name: 'checkInterval', value: 3900, displayed: false) //, isStateChange: true)  // 65 minutes (we get forcePolled every 60 minutes
	}
}

void noOp() {}

def generateEvent(Map results) {
	if (debugLevel(3)) LOG("generateEvent(): parsing data ${results}",1,null,'trace')
    String myVersion = getDataValue("myVersion")
    //if (!state.version || (state.version != getVersionLabel())) updated()
	if (!myVersion || (myVersion != getVersionLabel())) updated()
	
	def startMS = now()
	
	Integer objectsUpdated = 0
	String tempScale = getTemperatureScale()
    def precision = device.currentValue('decimalPrecision')
    if (!precision) precision = (tempScale == 'C') ? 1 : 0
    String currentProgramName = device.currentValue('currentProgramName', true)
    def isConnected = (currentProgramName != 'Offline')

	if(results) {
		String tempDisplay = ''
		results.each { name, value ->
			// objectsUpdated++
			def linkText = getLinkText(device)
			def isChange = false
			def isDisplayed = true
			def event = [:]  // [name: name, linkText: linkText, handlerName: name]
           
			String sendValue = value as String
			switch(name) {
				case 'temperature':
					if ((sendValue == null) || (sendValue == 'unknown') || !isConnected) {
						// We are OFFLINE
						LOG("Warning: Remote Sensor (${device.displayName}:${name}) is OFFLINE. Please check the batteries or move closer to the thermostat.", 
							2, null, 'warn')
						sendEvent( name: 'temperatureDisplay', linkText: linkText, value: '451°', handlerName: "temperatureDisplay", 
								  descriptionText: 'Sensor is offline', /* isStateChange: true, */ displayed: true)
						objectsUpdated++
						// don't actually chhange the temperature - leave the old value
						// event = [name: name, linkText: linkText, descriptionText: "Sensor is Offline", handlerName: name, value: sendValue, isStateChange: true, displayed: true]
					} else {
						// must be online  
						// isChange = isStateChange(device, name, sendValue)
						isChange = true // always send the temperature, else HealthCheck will think we are OFFLINE

						// Generate the display value that will preserve decimal positions ending in 0
						if (isChange) {
							def dValue = value.toBigDecimal()
							if (precision == 0) {
								tempDisplay = roundIt(dValue, 0).toString() + '°'
								sendValue = roundIt(dValue, 0).toInteger()								// Remove decimals in device lists also
							} else {
								tempDisplay = String.format( "%.${precision.toInteger()}f", roundIt(dValue, precision.toInteger())) + '°'
							}
							//sendEvent(name: 'temperatureDisplay', linkText: linkText, value: "${tempDisplay}", handlerName: 'temperatureDisplay', 
							//		  descriptionText: "Display temperature is ${tempDisplay}", isStateChange: true, displayed: false)
							event = [name: name, linkText: linkText, descriptionText: "Temperature is ${tempDisplay}", unit: tempScale, handlerName: name, 
									 value: sendValue, isStateChange: true, displayed: true]
							objectsUpdated += 2
						}
					}
					break;
				case 'motion':     
					if ( (sendValue == 'unknown') || !isConnected) {
						// We are OFFLINE
						LOG( "Warning: Remote Sensor (${device.displayName}:${name}) is OFFLINE. Please check the batteries or move closer to the thermostat.", 2, null, 'warn')
						sendValue = 'unknown'
					}

					isChange = isStateChange(device, name, sendValue.toString())
					if (isChange) {
						event = [name: name, linkText: linkText, descriptionText: "Motion is ${sendValue}", handlerName: name, value: sendValue, isStateChange: true, displayed: true]
						objectsUpdated++
					}
					String occupancy = ((value == 'active') ? 'occupied' : 'vacant')
					isChange = isChange ?: isStateChange(device, 'occupancy', occupancy)
					if (isChange) {
						sendEvent (name: 'occupancy', linkText: linkText, value: occupancy, descriptionText: "Occupancy is ${occupancy}", isStateChange: true, displayed: true)
						objectsUpdated++
					}
				    break;
			case 'currentProgramName':
					isChange = isStateChange(device, name, sendValue)
					if (isChange) {
						isConnected = (sendValue != 'Offline')		// update if it changes
						objectsUpdated++
						event = [name: name, linkText: linkText, value: sendValue, descriptionText: 'Program is '+sendValue.replaceAll(':',''), isStateChange: true, displayed: true]
					}
					break;
            case 'checkInterval':
                    event = [name: name, value: sendValue, /*isStateChange: true,*/ displayed: false]
                    break;
            case 'debugEventFromParent':
                    event = [name: name, value: sendValue, descriptionText: "DEBUG: "+sendValue, isStateChange: true, displayed: false]
                    updateDataValue( name, sendValue )
                    break;
            case 'debugLevel':
                    //String sendText = (sendValue != 'null') ? sendValue : ''
                    updateDataValue('debugLevel', sendValue)
                    event = [name: name, value: sendValue, descriptionText: "debugLevel is ${sendValue}", displayed: false]
                    break;
			case 'Home':
			case 'Away':
			case 'Sleep':
			case 'vents':
			case 'doors':
			case 'windows':
			case 'SmartRoom':
			case 'decimalPrecision':
			case 'programsList':
            case 'climatesList':
			case 'thermostatId':
            case 'activeClimates':
                   // isChange = isStateChange(device, name, sendValue)
                   // if (isChange) {
                        event = [name: name, linkText: linkText, handlerName: name, value: sendValue, /* isStateChange: true, */ displayed: false]
                        objectsUpdated++
                   // }
                    break;
			default:
                    // Must be a non-standard program name, or one of the XXXupdated timestamps
                   // isChange = isStateChange(device, name, sendValue)
                   // if (isChange) {
                        event = [name: name, linkText: linkText, handlerName: name, value: sendValue, /*isStateChange: true,*/ displayed: false]
                        if (!name.endsWith("Updated")) {
                            // Save non-standard Programs in a state variable and a dataValue
                            state."${name}" = sendValue
                            updateDataValue( name, sendValue )
                        } // else {
                            //if (getDataValue(name) != null) updateDataValue(name, "")
                            //if (getDataValue('thermostatUpdated') != "") updateDataValue('thermostatUpdated', "")
                        //}
                        objectsUpdated++
                   // }
                    break;
            }			
			if (event != [:]) sendEvent(event)
		}
		if (tempDisplay) {
			sendEvent( name: "temperatureDisplay", linkText: linkText, value: "${tempDisplay}", handlerName: "temperatureDisplay", descriptionText: "Display temperature is ${tempDisplay}",/* isStateChange: true,*/ displayed: false)
		}
	}
	def elapsed = now() - startMS
    LOG("Updated ${objectsUpdated} object${objectsUpdated!=1?'s':''} (${elapsed}ms)",2,this,'info')
}

//generate custom mobile activity feeds event
def generateActivityFeedsEvent(notificationMessage) {
	sendEvent(name: "notificationMessage", value: "$device.displayName $notificationMessage", descriptionText: "$device.displayName $notificationMessage", displayed: true)
}

void addSensorToHome() 					{ updateSensorPrograms(	['Home'],	[] 		) }
void addSensorToAway() 					{ updateSensorPrograms(	['Away'],	[] 		) }
void addSensorToSleep()					{ updateSensorPrograms(	['Sleep'],	[] 		) }
void addSensorToProgram(program) 		{ updateSensorPrograms( [program],	[] 		) }
void addSensorToPrograms(programs)		{ updateSensorPrograms( programs,	[]		) }
void deleteSensorFromHome() 			{ updateSensorPrograms(	[],			['Home']) }
void deleteSensorFromAway() 			{ updateSensorPrograms(	[], 		['Away']) }
void deleteSensorFromSleep() 			{ updateSensorPrograms(	[],			['Sleep'])}
void removeSensorFromAway() 			{ updateSensorPrograms(	[], 		['Away']) }
void removeSensorFromSleep() 			{ updateSensorPrograms(	[], 		['Sleep'])}
void deleteSensorFromProgram(program)	{ updateSensorPrograms(	[], 		[program])}
void deleteSensorFromPrograms(programs) { updateSensorPrograms( [],			programs )}
void removeSensorFromProgram(program)	{ updateSensorPrograms(	[], 		[program])}
void removeSensorFromPrograms(programs) { updateSensorPrograms( [],			programs )}

void updateSensorPrograms(List activeList, List inactiveList) {
	LOG("updateSensorPrograms(${activeList}, ${inactiveList}) - entry",3,this,'trace')

	// Let our parent do all the work...
	def result = parent.updateSensorPrograms(this, device.currentValue('thermostatId').toString(), getSensorId(), activeList, inactiveList)
    if (result) {
    	def programsList = device.currentValue('programsList', true)
        activeList.each { program ->
        	sendEvent(name: "${program}", value: 'on', descriptionText: "Sensor added to the ${program.capitalize()} program", isStateChange: true, displayed: true)
    		if (!programIdList().contains(program.toLowerCase())) state?."$program" = 'on'
        }
        inactiveList.each { program ->
        	sendEvent(name: "${program}", value: 'off', descriptionText: "Sensor removed from the ${program.capitalize()} program", isStateChange: true, displayed: true)
    		if (!programIdList().contains(program.toLowerCase())) state?."$program" = 'off'
        }
        // wait for Ecobee Cloud to update the activeClimates list
    }    
    LOG("updateSensorPrograms(Add: ${activeList?.toString()[1..-2].replace(',"',', ').replace('"','').replace('][','(none)')}, Remove: ${inactiveList?.toString()[1..-2].replace(',"',', ').replace('"','').replace('][','(none)')}) - ${result?'Succeeded':'Failed'}",2,this,'info')
}

void enableSmartRoom() {
	sendEvent(name: "SmartRoom", value: "enable", isSateChange: true, displayed: false)		// the Smart Room SmartApp should be watching for this
}

void disableSmartRoom() {
	sendEvent(name: "SmartRoom", value: "disable", isSateChange: true, displayed: false)		// the Smart Room SmartApp should be watching for this
}

String getSensorId() {
	def myId = []
    myId = device.deviceNetworkId.split('-') as List
    return (myId[2])
}
def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def roundIt( BigDecimal value, decimals=0 ) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
int getDebugLevel() {
	 return (getDataValue('debugLevel') ?: (device.currentValue('debugLevel') ?: (getParentSetting('debugLevel') ?: 3))) as int
}
boolean debugLevel(level=3) {
	return ( getDebugLevel() >= (level as int) )
}

void LOG(message, int level=3, child=null, logType="debug", event=false, displayEvent=false) {
	def prefix = ""
	Integer dbgLvl = (getParentSetting('debugLevel') ?: level) as Integer
	if ( dbgLvl == 5 ) { prefix = "LOG: " }
	if ( dbgLvl >= level ) { 
    	log."${logType}" "${prefix}${message}"
        if (event) { debugEvent(message, displayEvent) }        
	}    
}

void debugEvent(message, displayEvent = false) {
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
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
String getPlatform() { return ((hubitat?.device?.HubAction == null) ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST() {
    return false   
}
boolean getIsHE() {
    return true
}

String getHubPlatform() {
	return "Hubitat"
}
boolean getIsSTHub() { return false }					// if (isSTHub) ...
boolean getIsHEHub() { return true }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	return ST ? parent?.settings?."${settingName}" : parent?."${settingName}"
}
@Field String  hubPlatform 	= "Hubitat"
@Field boolean ST 			= false
@Field boolean HE 			= true
@Field String  debug		= 'debug'
@Field String  error		= 'error'
@Field String  info			= 'info'
@Field String  trace		= 'trace'
@Field String  warn			= 'warn'
