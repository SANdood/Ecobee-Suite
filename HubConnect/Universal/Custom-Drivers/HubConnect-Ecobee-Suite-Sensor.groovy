/*
 *	Copyright 2019 Steve White
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
import groovy.json.*
	
metadata 
{
	definition(name: "HubConnect Ecobee Suite Sensor", namespace: "shackrat", author: "Barry A. Burke", 
			   importUrl: "https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/HubConnect/Universal/Custom-Drivers/HubConnect-Ecobee-Suite-Sensor.groovy")
	{
		capability "Sensor"
		capability "Temperature Measurement"
		capability "Motion Sensor"
		capability "Actuator"
		capability "Refresh"
		
		attribute "Awake", 					"string"
		attribute "Away", 					"string"
        attribute "Fan Only", 				"string"
		attribute "Home", 					"string"
		attribute "Sleep", 					"string"
		attribute "SmartRoom", 				"string"
		attribute "Wakeup", 				"string"
        
        attribute "activeClimates", 		'string'
        attribute "climatesList", 			"string"
		attribute "currentProgramName", 	"string"
		attribute "decimalPrecision", 		"number"
		attribute "doors", 					"string"
		attribute "humidity", 				"string"
        attribute "programsList", 			"string"
		attribute "temperatureDisplay", 	"string"
		attribute "thermostatId", 			"string"
		attribute "vents", 					"string"
		attribute "windows", 				"string"
        
		attribute "programUpdated", 		'number'
        attribute "curClimRefUpdated", 		'number'
        attribute "climatesUpdated", 		'number'
        attribute "scheduleUpdated", 		'number'
        attribute "eventsUpdated", 			'number'   
		
		command "addSensorToAway", 			[]
		command "addSensorToHome", 			[]
		command "addSensorToProgram", 		[[name:'Program Name*', type:'STRING', description:'Add sensor to this Program Name']]
		command "addSensorToSleep", 		[]
		command "deleteSensorFromAway", 	[]
		command "deleteSensorFromHome", 	[]
		command "deleteSensorFromProgram", 	[[name:'Program Name*', type:'STRING', description:'Delete sensor from this Program Name']]
		command "deleteSensorFromSleep", 	[]
		command "disableSmartRoom", 		[]
		command "enableSmartRoom", 			[]
		command "removeSensorFromAway", 	[]
		command "removeSensorFromHome", 	[]
		command "removeSensorFromProgram", 	[[name:'Program Name*', type:'STRING', description:'Remove sensor from this Program Name']]
		command "removeSensorFromSleep", 	[]
		
		command "addSensorToPrograms",		[]	// programs list
        command "deleteSensorFromPrograms"	[]	// programs list
        command "removeSensorFromPrograms"	[]
        command "updateSensorPrograms"		[]	// arguments: (activeList, inactiveList)
		
		command "sync",						[]
	}
}
/*
	Standard Device Driver Commands
*/
def installed()
{
	initialize()
}
def updated()
{
	initialize()
}
def initialize()
{
	refresh()
}
def parse(String description)
{
	log.trace "Msg: Description is $description"
}
def refresh()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "refresh")
}
/*
	Ecobee Suite Sensor Commands
*/
def addSensorToAway()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "addSensorToAway")
}
def addSensorToHome()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "addSensorToHome")
}
def addSensorToProgram(program)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "addSensorToProgram", [program])
}
def addSensorToSleep()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "addSensorToSleep")
}
def deleteSensorFromAway()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "deleteSensorFromAway")
}
def deleteSensorFromHome()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "deleteSensorFromHome")
}
def deleteSensorFromProgram(program)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "deleteSensorFromProgram", [program])
}
def deleteSensorFromSleep()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "deleteSensorFromSleep")
}
def disableSmartRoom()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "disableSmartRoom")
}
def enableSmartRoom()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "enableSmartRoom")
}
def removeSensorFromAway()
{
	deleteSensorFromAway()
}
def removeSensorFromHome()
{
	deleteSensorFromHome()
}
def removeSensorFromProgram(program)
{
	deleteSensorFromProgram(program)
}
def removeSensorFromSleep()
{
	deleteSensorFromSleep()
}
def addSensorToPrograms(programsList)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "addSensorToPrograms", [programList])
}
def deleteSensorFromPrograms(programsList)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "deleteSensorFromPrograms", [programList])
}
def removeSensorFromPrograms(programsList)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "removeSensorFromPrograms", [programList])
}
def updateSensorPrograms(activeList, inactiveList)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "updateSensorPrograms", [activeList, inactiveList])
}
/*
	sync
    
	Synchronizes the device details with the parent.
*/
def sync()
{
	// The server will respond with updated status and details
	parent.syncDevice(device.deviceNetworkId, "ESSensor")
	sendEvent([name: "version", value: "v${driverVersion.major}.${driverVersion.minor}.${driverVersion.build}"])
}
def getDriverVersion() {[platform: "Hubitat", major: 1, minor: 8, build: "01"]} 
