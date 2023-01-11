/**
 *  Ecobee Switch
 *
 *  Copyright 2015 Juan Risso
 *	Copyright 2017-2020 Barry A. Burke
 *  Copyright 2020 Dominick Meglio
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
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum() 		{ return "1.8.07" }
String getVersionLabel() 	{ return "Ecobee Suite Switch, version ${getVersionNum()} on ${getPlatform()}" }

metadata {
	definition (
        name:         	"Ecobee Suite Switch", 
        namespace:    	"sandood", 
        author:       	"Barry A. Burke (storageanarchy@gmail.com)",
        importUrl:    	"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/devicetypes/sandood/ecobee-suite-switch.src/ecobee-suite-switch.groovy"
    ) {
    
	capability "Switch"
	capability "Health Check"
	capability "Actuator"
	capability "Refresh"
	}
    preferences {
       	input "dummy", "text", title: "${getVersionLabel()}", description: "."
	}
}

void on() {
	parent.controlSwitch(this, true)
}

void off() {
	parent.controlSwitch(this, false)
}

void refresh(force=false) {
	LOG( "Refreshed - executing parent.pollChildren() ${force?'(forced)':''}", 2, this, 'info')
	parent.pollChildren("",force)		// we have to poll our Switch to get updated
}

// Health Check will ping us based on the frequency we configure in Ecobee (Connect) (derived from poll & watchdog frequency)
void ping() {
	LOG( "Pinged - executing parent.pollChildren()", 2, null, 'info')
    sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
	sendEvent(name: "healthStatus", value: "online")
	parent.pollChildren("",true)		// we have to poll our Switch to get updated
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
	
/*	if (!device.displayName.contains('TestingForInstall')) {
		// Try not to get hung up in the Health Check so that ES Manager can delete the temporary device
		sendEvent(name: 'checkInterval', value: 3900, displayed: false) //, isStateChange: true)  // 65 minutes (we get forcePolled every 60 minutes
		if (ST) {
			//sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "cloud", scheme:"untracked"]), displayed: false)
			updateDataValue("EnrolledUTDH", "true")
            sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
			sendEvent(name: "healthStatus", value: "online")
            if (location.hubs[0] && location.hubs[0].id) {
            	sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "cloud", scheme:"untracked", hubHardwareId: location.hubs[0].id.toString()]), displayed: false)
            } else {
            	sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "cloud", scheme:"untracked"]), displayed: false)
            }
		}
	}*/
}

void noOp() {}

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

//generate custom mobile activity feeds event
def generateActivityFeedsEvent(notificationMessage) {
	sendEvent(name: "notificationMessage", value: "$device.displayName $notificationMessage", descriptionText: "$device.displayName $notificationMessage", displayed: true)
}

def generateEvent(Map results) {
	if (debugLevel(3)) LOG("generateEvent(): parsing data ${results}",1,null,'trace')
    String myVersion = getDataValue("myVersion")
    //if (!state.version || (state.version != getVersionLabel())) updated()
	if (!myVersion || (myVersion != getVersionLabel())) updated()
	
	def startMS = now()
	
	Integer objectsUpdated = 0


	if(results) {
		results.each { name, value ->
			// objectsUpdated++
			def linkText = getLinkText(device)
			def isChange = false
			def isDisplayed = true
			def event = [:]  // [name: name, linkText: linkText, handlerName: name]
           
			String sendValue = value as String
			switch(name) {


            case 'debugEventFromParent':
                    event = [name: name, value: sendValue, descriptionText: "DEBUG: "+sendValue, isStateChange: true, displayed: false]
                    updateDataValue( name, sendValue )
                    break;
            case 'debugLevel':
                    //String sendText = (sendValue != 'null') ? sendValue : ''
                    updateDataValue('debugLevel', sendValue)
                    event = [name: name, value: sendValue, descriptionText: "debugLevel is ${sendValue}", displayed: false]
                    break;
            }			
			if (event != [:]) sendEvent(event)
		}
	}
	def elapsed = now() - startMS
    LOG("Updated ${objectsUpdated} object${objectsUpdated!=1?'s':''} (${elapsed}ms)",2,this,'info')
}
	

// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
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
