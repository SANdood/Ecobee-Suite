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
metadata 
{
	definition(name: "HubConnect EcoVent", namespace: "shackrat", author: "Steve White & Barry Burke",
			   importUrl: "https://raw.githubusercontent.com/SANdood/HubConnect-Custom-Drivers/master/HubConnect%20EcoVent")
	{
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Battery"
		capability "Refresh"
		capability "Contact Sensor"
		capability "Sensor"

		attribute "version", "string"

		command "sync"
		command "open"
		command "close"
		command "closeContact"
		command "openContact"
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
		standardTile("sync", "sync", inactiveLabel: false, decoration: "flat", width: 3, height: 2)
		{
			state "default", label: 'Sync', action: "sync", icon: "st.Bath.bath19"
		}

		main(["switch"])
		details(["switch","battery","refresh","levelSliderControl","contact","sync"])
	}
}

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
def closeContact() {
	parent.sendDeviceEvent(device.deviceNetworkId, "closeContact")
}
def openContact() {
	parent.sendDeviceEvent(device.deviceNetworkId, "openContact")
}
/*
	sync
    
	Synchronizes the device details with the parent.
*/
def sync()
{
	// The server will respond with updated status and details
	parent.syncDevice(device.deviceNetworkId, "EcoVent")
	sendEvent([name: "version", value: "v${driverVersion.major}.${driverVersion.minor}.${driverVersion.build}"])
}
def getDriverVersion() {[platform: "Universal-BAB", major: 1, minor: 8, build: "01"]}
