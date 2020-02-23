/*
 *	Copyright 2019 Steve White & Barry Burke
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License. You may obtain a copy
 *	of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *	License for the specific language governing permissions and limitations
 *	under the License.
 *
 *
 */
private getFahrenheit() { true }		// Set to false for Celsius color scale
private getCelsius() { !fahrenheit }

metadata 
{
	definition(name: "HubConnect Keen Home Smart Vent", namespace: "shackrat", author: "Steve White & Barry Burke",
			   importUrl: "https://raw.githubusercontent.com/SANdood/HubConnect-Custom-Drivers/master/HubConnect%20Keen%20Home%20Smart%20Vent")
	{
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Battery"
		capability "Refresh"
		capability "Contact Sensor"
		capability "Temperature Measurement"
		capability "Sensor"
		capability "Configuration"

		attribute "zigbeeId", "string"
		attribute "pressure", "number"
		attribute "pascals", "number"
		attribute "serial", "string"
		attribute "version", "string"

		command "sync"
		command "open"
		command "close"
		command "closeContact"
		command "openContact"
		
		command "getLevel"
        command "getOnOff"
        command "getPressure"
        command "getBattery"
        command "getTemperature"
        command "setZigBeeIdTile"
        command "clearObstruction"
	}
	
    tiles(scale: 2)
	{
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true)
		{
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL")
			{
				attributeState "on", label: '', action:"switch.off", icon:"st.vents.vent-open-text", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label: '', action:"switch.on", icon:"st.vents.vent-closed", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'', action:"switch.off", icon:"st.vents.vent-open-text", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'', action:"switch.on", icon:"sst.vents.vent-closed", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL")
			{
				attributeState "level", action:"switch level.setLevel"
			}
            tileAttribute ("device.contact", key: "SECONDARY_CONTROL")
            {
            	attributeState "open", label: 'ES Smart Vent: open', icon: "st.contact.contact.open", backgroundColor: "#e86d13"
				attributeState "closed", label: 'ES Smart Vent: closed', icon: "st.contact.contact.closed", backgroundColor: "#00A0DC"
                attributeState "default", label: '', icon: 'st.unknown.unknown.unknown', defaultState: true
            }
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1)
		{
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}
        valueTile("temperature", "device.temperature", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',	backgroundColors: (temperatureColors)
        }
        valueTile("pressure", "device.pressure", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}\ninHg'
        }
		standardTile("sync", "sync", inactiveLabel: false, decoration: "flat", width: 1, height: 1)
		{
			state "default", label: 'Sync', action: "sync", icon: "st.Bath.bath19"
		}
		valueTile("version", "version", inactiveLabel: false, decoration: "flat", width: 1, height: 1)
		{
			state "default", label: '${currentValue}'
		}

		main(["switch"])
        details(["switch", "temperature", "pressure", "battery",  "refresh", "sync","version"])
    }
	/*tiles {
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
		standardTile("sync", "sync", inactiveLabel: false, decoration: "flat", width: 3, height: 2)
		{
			state "default", label: 'Sync', action: "sync", icon: "st.Bath.bath19"
		}

		main(["switch"])
		details(["switch","battery","refresh","levelSliderControl","contact","sync"])
	}
*/}

/*
	installed
    
	Doesn't do much other than call initialize().
*/
def installed()
{
	initialize()
}


/*
	updated
    
	Doesn't do much other than call initialize().
*/
def updated()
{
	initialize()
}


/*
	initialize
    
	Doesn't do much other than call refresh().
*/
def initialize()
{
	refresh()
}


/*
	parse
    
	In a virtual world this should never be called.
*/
def parse(String description)
{
	log.trace "Msg: Description is $description"
}


/*
	on
    
	Turns the device on.
*/
def on()
{
	// The server will update on/off status
	parent.sendDeviceEvent(device.deviceNetworkId, "on")
}


/*
	off
    
	Turns the device off.
*/
def off()
{
	// The server will update on/off status
	parent.sendDeviceEvent(device.deviceNetworkId, "off")
}


/*
	setLevel
    
	Sets the level to <level> over duration <duration>.
*/
def setLevel(value)
{
	// The server will update status
	parent.sendDeviceEvent(device.deviceNetworkId, "setLevel", [value])
}
def setLevel(value, duration)
{
	setLevel(value)
}

/*
	refresh
    
	Refreshes the device by requesting an update from the client hub.
*/
def refresh()
{
	// The server will update status
	parent.sendDeviceEvent(device.deviceNetworkId, "refresh")
}

/*
	open
*/
def open() {
	on()
}

/*
	close
*/
def close() {
	off()
}

/* Extended commands for Keen Vent
*/
def closeContact() {
	parent.sendDeviceEvent(device.deviceNetworkId, "closeContact")
}
def openContact() {
	parent.sendDeviceEvent(device.deviceNetworkId, "openContact")
}
def getLevel() {
	parent.sendDeviceEvent(device.deviceNetworkId, "getLevel")
}
def getPressure() {
	parent.sendDeviceEvent(device.deviceNetworkId, "getPressure")
}
def getBattery() {
	parent.sendDeviceEvent(device.deviceNetworkId, "getBattery")
}
def getTemperature() {
	parent.sendDeviceEvent(device.deviceNetworkId, "getTemperature")
}
def setZigBeeIdTile() {
	parent.sendDeviceEvent(device.deviceNetworkId, "getZigBeeIdTile")
}
def clearObstruction() {
	parent.sendDeviceEvent(device.deviceNetworkId, "clearObstruction")
}
def configure() {
	parent.sendDeviceEvent(device.deviceNetworkId, "configure")
}
/*
	sync
    
	Synchronizes the device details with the parent.
*/
def sync()
{
	// The server will respond with updated status and details
	parent.syncDevice(device.deviceNetworkId, "KeenVent")
	sendEvent([name: "version", value: "v${driverVersion.major}.${driverVersion.minor}.${driverVersion.build}"])
}
def getTemperatureColors() {
    ( (fahrenheit) ? ([
        [value: 31, color: "#153591"],
        [value: 44, color: "#1e9cbb"],
        [value: 59, color: "#90d2a7"],
        [value: 74, color: "#44b621"],
        [value: 84, color: "#f1d801"],
        [value: 95, color: "#d04e00"],
        [value: 96, color: "#bc2323"]
    ]) : ([
        [value:  0, color: "#153591"],
        [value:  7, color: "#1e9cbb"],
        [value: 15, color: "#90d2a7"],
        [value: 23, color: "#44b621"],
        [value: 28, color: "#f1d801"],
        [value: 35, color: "#d04e00"],
        [value: 37, color: "#bc2323"]
    ]) )
}
def getDriverVersion() {[platform: "Universal-BAB", major: 1, minor: 8, build: '01']} 
