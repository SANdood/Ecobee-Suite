/**
 *  Econet EV100 Vent
 *
 *  Copyright 2014 SmartThings
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
 * SANdood' fixes:
 * - longer delays for complex commands so that status is returned in a timely manner
 * - fixed lastbatt (2 trailing t's) (no longer requests battery state every refresh/poll
 * - added log.debug/info support
 *
 * 2019/12/04
 * - Added "contact sensor" support for use with HomeKit (as blinds) and Ecobee Suite Smart Vents
 *
 */

metadata {
	// Automatically generated. Make future change here.
	definition (name: "EcoNet Vent", namespace: "sandood", author: "SmartThings & Barry Burke") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Battery"
		capability "Refresh"
		capability "Sensor"
        capability "Polling"
        capability "Configuration"
//		capability "Health Check"
		capability "Contact Sensor"

		command "open"
		command "close"
		command "closeContact"
		command "openContact"

		fingerprint deviceId: "0x1100", inClusters: "0x26,0x72,0x86,0x77,0x80,0x20"
		fingerprint mfr:"0157", prod:"0100", model:"0100", deviceJoinName: "EcoNet Controls Z-Wave Vent"
	}

	simulator {
		status "on":  "command: 2603, payload: FF"
		status "off": "command: 2603, payload: 00"
		status "09%": "command: 2603, payload: 09"
		status "10%": "command: 2603, payload: 0A"
		status "33%": "command: 2603, payload: 21"
		status "66%": "command: 2603, payload: 42"
		status "99%": "command: 2603, payload: 63"

		// reply messages
		reply "2001FF,delay 100,2602": "command: 2603, payload: FF"
		reply "200100,delay 100,2602": "command: 2603, payload: 00"
		reply "200119,delay 100,2602": "command: 2603, payload: 19"
		reply "200132,delay 100,2602": "command: 2603, payload: 32"
		reply "20014B,delay 100,2602": "command: 2603, payload: 4B"
		reply "200163,delay 100,2602": "command: 2603, payload: 63"
	}
	preferences {
		input name: "debugOutput", type: "bool", title: "Enable debug logging?", defaultValue: true
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
	
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", action:"switch.off", icon:"st.vents.vent-open-text", backgroundColor:"#00a0dc"
			state "off", action:"switch.on", icon:"st.vents.vent-closed", backgroundColor:"#ffffff"
		}
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false, range:"(0..100)") {
			state "level", action:"switch level.setLevel"
		}
		valueTile("contact", "device.contact", inactiveLabel: false, decoration: "flat") {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#e86d13"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#00A0DC"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["switch"])
		details(["switch","battery","refresh","levelSliderControl"])
	}
}

def parse(String description) {
	def result = []
	logDebug "parse description: ${description}"
	def cmd = description ? zwave.parse(description, [0x20: 1, 0x26: 3, 0x70: 1, 0x32:3]) : null
	if (cmd) {
		result = zwaveEvent(cmd)
    logDebug("'$description' parsed to $result")
	} else {
		logDebug("Couldn't zwave.parse '$description'")
	}
    return result
}

def installed() {
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	// sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	updated()
}

//send the command to stop polling
def updated() {
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	log.warn "debug logging is: ${debugOutput == true}"
  log.warn "description logging is: ${txtEnable == true}"
  if (debugOutput) runIn(1800,logsOff)
	if (state.lastbat) state.lastbat = null	// old state variable (misspelled) 
	state.lastbatt = null
	refresh()
	return "poll stop"
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd) + [ response("poll stop") ]  // we get a BasicReport when the hub starts polling
}

//parse manufacture name and store it
def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	if (state.manufacturer != cmd.manufacturerName) {
		updateDataValue("manufacturer", cmd.manufacturerName)
	}
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}

def dimmerEvents(hubitat.zwave.Command cmd) {
	def text = "${device.displayName} is ${cmd.value ? "open" : "closed"}"
	logInfo(text)
	def switchEvent = createEvent(name: "switch", value: (cmd.value ? "on" : "off"), descriptionText: text)
	def levelEvent = createEvent(name:"level", value: cmd.value == 99 ? 100 : cmd.value , unit:"%")
	//def contact = (cmd.value == 0) ? 'closed' : ((cmd.value >= 99) ? 'open' : '')
	//if (!contact) return [switchEvent, levelEvent]
	//else {
	//	def contactEvent = createEvent(name:'contact', value: contact)
	//	return [switchEvent, levelEvent, contactEvent]
	//}
	[switchEvent, levelEvent]
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		logInfo(map.descriptionText)
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	logInfo("${device.displayName} battery is ${map.value}%")
	state.lastbatt = new Date().time
	createEvent(map)
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	def linkText = device.label ?: device.name
	[linkText: linkText, descriptionText: "$linkText: $cmd", displayed: false]
}

def on() {
	openContact()
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format()
	], 4400)
}

def open() {
	on()
}

def off() {
	closeContact()
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format()
	], 4400)
}

def close() {
	off()
}

def closeContact() {
	sendEvent(name: "contact", value: "closed", displayed: false)
}

def openContact() {
	sendEvent(name: "contact", value: "open", displayed: false)	
}

def setLevel(value) {
	if (value > 0) {
        sendEvent(name: "switch", value: "on", descriptionText: "${linkText} is on by setting a level")
		logInfo "${linkText} is on by setting a level"
    }
    else {
        sendEvent(name: "switch", value: "off", descriptionText: "${linkText} is off by setting level to 0")
		logInfo "${linkText} is off by setting level to 0"
		closeContact()
    }
	delayBetween([
		zwave.basicV1.basicSet(value: value).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format()
	], 6600)
}

def setLevel(value, duration) {
	setLevel(value)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	refresh()
}

def refresh() {
	if (secondsPast(state.lastbatt, 23*60*60)) {
		//poll for battery once a day
		logDebug "battery poll (refresh)"
		delayBetween([
			zwave.switchMultilevelV1.switchMultilevelGet().format(),
			zwave.batteryV1.batteryGet().format()
		], 2200)
	} else {
		zwave.switchMultilevelV1.switchMultilevelGet().format()
	}
}

//poll for battery once a day
def poll() {
	if (secondsPast(state.lastbatt, 23*60*60)) {
		logDebug "battery poll (poll)"
		return zwave.batteryV1.batteryGet().format()
	} else {
		return zwave.switchMultilevelV1.switchMultilevelGet().format()
	}
}
def configure() {
	[
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
    ]
    refresh()
}

//check last message so battery poll doesn't happen all the time
private Boolean secondsPast(timestamp, seconds) {
	if (!(timestamp instanceof Number)) {
		if (timestamp instanceof Date) {
			timestamp = timestamp.time
		} else if ((timestamp instanceof String) && timestamp.isNumber()) {
			timestamp = timestamp.toLong()
		} else {
			return true
		}
	}
	return (new Date().time - timestamp) > (seconds * 1000)
}

//Warn message for unkown events
def createEvents(hubitat.zwave.Command cmd) {
	log.warn "UNEXPECTED COMMAND: $cmd"
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])
}

private logDebug(msg) {
	if (settings?.debugOutput || settings?.debugOutput == null) log.debug "$msg"
}

private logInfo(msg) {
	if (settings?.txtEnable || settings?.txtEnable == null) {
		if (state.msg != msg) { 
			// Don't print redundant messages
			log.info "$msg"
			state.msg = msg
		}
	}
}
