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
String getVersionNum() 		{ return "1.8.00b" }
String getVersionLabel() 	{ return "HubConnect Ecobee Suite Thermostat, version ${getVersionNum()}" }
metadata 
{
	definition(name: "HubConnect Ecobee Suite Thermostat", namespace: "shackrat", author: "Barry Burke", 
			   importUrl: "https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/HubConnect/Universal/Custom-Drivers/HubConnect-Ecobee-Suite-Thermostat.groovy")
	{
		capability "Sensor"
		capability "Thermostat"
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Motion Sensor"
		capability "Actuator"
		capability "Refresh"

		attribute 'apiConnected','STRING'
		attribute 'autoAway', 'STRING'
		attribute 'autoHeatCoolFeatureEnabled', 'STRING'
		attribute 'autoMode', 'STRING'
		attribute 'auxHeatMode', 'STRING'
		attribute 'auxMaxOutdoorTemp', 'NUMBER'
		attribute 'auxOutdoorTempAlert', 'NUMBER'		// temp
		attribute 'auxOutdoorTempAlertNotify', 'STRING'
		attribute 'auxRuntimeAlert', 'NUMBER'			// time
		attribute 'auxRuntimeAlertNotify', 'STRING'
		attribute 'auxOutdoorTempAlertNotifyTechnician', 'STRING'
		attribute 'auxRuntimeAlertNotifyTechnician', 'STRING'
		attribute 'backlightOffDuringSleep', 'STRING'
		attribute 'backlightOffTime', 'NUMBER'
		attribute 'backlightOnIntensity', 'NUMBER'
		attribute 'backlightSleepIntensity', 'NUMBER'
		attribute 'brand', 'STRING'
		attribute 'climatesList', 'STRING'
		attribute 'coldTempAlert', 'NUMBER'
		attribute 'coldTempAlertEnabled', 'STRING'		// or boolean?
		attribute 'compressorProtectionMinTemp', 'NUMBER'
		attribute 'compressorProtectionMinTime', 'NUMBER'
		attribute 'condensationAvoid', 'STRING'			// boolean
		attribute 'coolAtSetpoint', 'NUMBER'
		attribute 'coolDifferential', 'NUMBER'
		attribute 'coolMaxTemp', 'NUMBER'				// Read Only
		attribute 'coolMinTemp', 'NUMBER'				// Read Only
		attribute 'coolMode', 'STRING'
		attribute 'coolRange', 'STRING'
		attribute 'coolRangeHigh', 'NUMBER'
		attribute 'coolRangeLow', 'NUMBER'
		attribute 'coolStages', 'NUMBER'				// Read Only
		attribute 'coolingLockout', 'STRING'
		attribute 'coolingSetpointDisplay', 'NUMBER'	// Now the same as coolingSetpoint
		attribute 'coolingSetpointMax', 'NUMBER'
		attribute 'coolingSetpointMin', 'NUMBER'
		attribute 'coolingSetpointRange', 'vector3'
		attribute 'coolingSetpointTile', 'NUMBER'		// Used to show coolAt/coolTo for MultiTile
		attribute 'coldTempAlert', 'NUMBER'
		attribute 'coldTempAlertEnabled', 'STRING'
		attribute 'currentProgram','STRING'				// Read only
		attribute 'currentProgramId','STRING'			// Read only
		attribute 'currentProgramName', 'STRING'		// Read only
        attribute 'currentProgramOwner', 'STRING'		// Read only
        attribute 'currentProgramType', 'STRING'		// Read only
		attribute 'debugEventFromParent','STRING'		// Read only
		attribute 'debugLevel', 'NUMBER'				// Read only - changed in preferences
		attribute 'decimalPrecision', 'NUMBER'			// Read only - changed in preferences
		attribute 'dehumidifierLevel', 'NUMBER'			// should be same as dehumiditySetpoint
		attribute 'dehumidifierMode', 'STRING'
		attribute 'dehumidifyOvercoolOffset', 'NUMBER'
		attribute 'dehumidifyWhenHeating', 'STRING'
		attribute 'dehumidifyWithAC', 'STRING'
        attribute 'dehumidityLevel', 'NUMBER'
		attribute 'dehumiditySetpoint', 'NUMBER'
		attribute 'disableAlertsOnIdt', 'STRING'
		attribute 'disableHeatPumpAlerts', 'STRING'
		attribute 'disablePreCooling', 'STRING'
		attribute 'disablePreHeating', 'STRING'
		attribute 'drAccept', 'STRING'
		attribute 'ecobeeConnected', 'STRING'
		attribute 'eiLocation', 'STRING'
		attribute 'electricityBillCycleMonths', 'STRING'
		attribute 'electricityBillStartMonth', 'STRING'
		attribute 'electricityBillingDayOfMonth', 'STRING'
		attribute 'enableElectricityBillAlert', 'STRING'
		attribute 'enableProjectedElectricityBillAlert', 'STRING'
		attribute 'equipmentOperatingState', 'STRING'
		attribute 'equipmentStatus', 'STRING'
		attribute 'fanControlRequired', 'STRING'
		attribute 'fanMinOnTime', 'NUMBER'
		attribute 'features','STRING'
		attribute 'followMeComfort', 'STRING'
		attribute 'groupName', 'STRING'
		attribute 'groupRef', 'STRING'
		attribute 'groupSetting', 'STRING'
		attribute 'hasBoiler', 'STRING'					// Read Only
		attribute 'hasDehumidifier', 'STRING'			// Read Only
		attribute 'hasElectric', 'STRING'				// Read Only
		attribute 'hasErv', 'STRING'					// Read Only
		attribute 'hasForcedAir', 'STRING'				// Read Only
		attribute 'hasHeatPump', 'STRING'				// Read Only
		attribute 'hasHrv', 'STRING'					// Read Only
		attribute 'hasHumidifier', 'STRING'				// Read Only
		attribute 'hasUVFilter', 'STRING'				// Read Only
		attribute 'heatAtSetpoint', 'NUMBER'
		attribute 'heatCoolMinDelta', 'NUMBER'
		attribute 'heatDifferential', 'NUMBER'
		attribute 'heatMaxTemp', 'NUMBER'				// Read Only
		attribute 'heatMinTemp', 'NUMBER'				// Read Only
		attribute 'heatMode', 'STRING'
		attribute 'heatPumpGroundWater', 'STRING'		// Read Only
		attribute 'heatPumpReversalOnCool', 'STRING'
		attribute 'heatRange', 'STRING'
		attribute 'heatRangeHigh', 'NUMBER'
		attribute 'heatRangeLow', 'NUMBER'
		attribute 'heatStages', 'NUMBER'				// Read Only
		attribute 'heatingSetpointDisplay', 'NUMBER'	// now the same as heatingSetpoint
		attribute 'heatingSetpointMax', 'NUMBER'
		attribute 'heatingSetpointMin', 'NUMBER'
		attribute 'heatingSetpointRange', 'vector3'
		attribute 'heatingSetpointTile', 'NUMBER'		// Used to show heatAt/heatTo for MultiTile
		attribute 'holdAction', 'STRING'
		attribute 'holdEndsAt', 'STRING'
		attribute 'holdStatus', 'STRING'
		attribute 'hotTempAlert', 'NUMBER'
		attribute 'hotTempAlertEnabled', 'STRING'
		attribute 'humidifierMode', 'STRING'
		attribute 'humidityAlertNotify', 'STRING'
		attribute 'humidityAlertNotifyTechnician', 'STRING'
		attribute 'humidityHighAlert', 'NUMBER'			// %
		attribute 'humidityLowAlert', 'NUMBER'
		attribute 'humiditySetpoint', 'NUMBER'
		attribute 'humiditySetpointDisplay', 'STRING'
		attribute 'identifier', 'STRING'
		attribute 'installerCodeRequired', 'STRING'
		attribute 'isRegistered', 'STRING' 
		attribute 'isRentalProperty', 'STRING'
		attribute 'isVentilatorTimerOn', 'STRING'
		attribute 'lastRunningMode', 'STRING'
		attribute 'lastHoldType', 'STRING'
		attribute 'lastModified', 'STRING' 
		attribute 'lastOpState', 'STRING'				// keeps track if we were most recently heating or cooling
		attribute 'lastPoll', 'STRING'
		attribute 'lastServiceDate', 'STRING'
		attribute 'locale', 'STRING'
		attribute 'logo', 'STRING'
		attribute 'maxSetBack', 'NUMBER'
		attribute 'maxSetForward', 'NUMBER'
        attribute 'microphoneEnabled', 'STRING'			// // From Audio object (boolean)
		attribute 'mobile', 'STRING'
		attribute 'modelNumber', 'STRING' 
		attribute 'monthlyElectricityBillLimit', 'STRING'
		attribute 'monthsBetweenService', 'STRING'
		attribute 'motion', 'STRING'
		attribute 'name', 'STRING'
        attribute 'playbackVolume', 'NUMBER'			// // From Audio object
		attribute 'programsList', 'STRING'				// USAGE: List programs = new JsonSlurper().parseText(stat.currentValue('programsList'))
		attribute 'quickSaveSetBack', 'NUMBER'
		attribute 'quickSaveSetForward', 'NUMBER'
		attribute 'randomStartDelayCool', 'STRING'
		attribute 'randomStartDelayHeat', 'STRING'
		attribute 'remindMeDate', 'STRING'
		attribute 'schedText', 'STRING'
		//attribute 'schedule', 'STRING'					// same as 'currentProgram'
		attribute 'scheduledProgram','STRING'
		attribute 'scheduledProgramId','STRING'
		attribute 'scheduledProgramName', 'STRING'
        attribute 'scheduledProgramOwner', 'STRING'
        attribute 'scheduledProgramType', 'STRING'
		attribute 'serviceRemindMe', 'STRING'
		attribute 'serviceRemindTechnician', 'STRING'
		attribute 'setpointDisplay', 'STRING'
		attribute 'smartCirculation', 'STRING'			// not implemented by E3, but by this code it is
		attribute 'soundAlertVolume', 'NUMBER'			// From Audio object
		attribute 'soundTickVolume', 'NUMBER'			// From Audio object
		attribute 'stage1CoolingDifferentialTemp', 'NUMBER'
		attribute 'stage1CoolingDissipationTime', 'NUMBER'
		attribute 'stage1HeatingDifferentialTemp', 'NUMBER'
		attribute 'stage1HeatingDissipationTime', 'NUMBER'
		attribute 'statHoldAction', 'STRING'
		attribute 'supportedThermostatFanModes', 'JSON_OBJECT' // USAGE: List theFanModes = stat.currentValue('supportedThermostatFanModes')[1..-2].tokenize(', ')
		attribute 'supportedThermostatModes', 'JSON_OBJECT' // USAGE: List theModes = stat.currentValue('supportedThermostatModes')[1..-2].tokenize(', ')
		attribute 'supportedVentModes', 'JSON_OBJECT' // USAGE: List theModes = stat.currentValue('supportedVentModes')[1..-2].tokenize(', ')
		attribute 'tempAlertNotify', 'STRING'
		attribute 'tempAlertNotifyTechnician', 'STRING'
		attribute 'tempCorrection', 'NUMBER'
		attribute 'temperatureDisplay', 'STRING'
		attribute 'temperatureScale', 'STRING'
		attribute 'thermostatFanModeDisplay', 'STRING'
		attribute 'thermostatHold', 'STRING'
		attribute 'thermostatOperatingStateDisplay', 'STRING'
		attribute 'thermostatSetpoint','NUMBER'
		attribute 'thermostatSetpointMax', 'NUMBER'
		attribute 'thermostatSetpointMin', 'NUMBER'
		attribute 'thermostatSetpointRange', 'vector3'
		attribute 'thermostatStatus','STRING'
		attribute 'thermostatTime', 'STRING'
		attribute 'timeOfDay', 'enum', ['day', 'night']
		attribute 'userAccessCode', 'STRING'			// Read Only
		attribute 'userAccessSetting', 'STRING'			// Read Only
		attribute 'vent', 'STRING'						// same as 'ventMode'
		attribute 'ventMode', 'STRING'					// Ecobee actually calls it only 'vent'
		attribute 'ventilatorDehumidify', 'STRING'
		attribute 'ventilatorFreeCooling', 'STRING'
		attribute 'ventilatorMinOnTime', 'NUMBER'
		attribute 'ventilatorMinOnTimeAway', 'NUMBER'
		attribute 'ventilatorMinOnTimeHome', 'NUMBER'
		attribute 'ventilatorOffDateTime', 'STRING'		// Read Only
		attribute 'ventilatorType', 'STRING'			// Read Only ('none', 'ventilator', 'hrv', 'erv')
        attribute 'voiceEngines', 'JSON_OBJECT'			// From Audio object
		attribute 'weatherDewpoint', 'NUMBER'
		attribute 'weatherHumidity', 'NUMBER'
		attribute 'weatherPressure', 'NUMBER'
		attribute 'weatherSymbol', 'STRING'
		attribute 'weatherTemperature', 'NUMBER'
		attribute 'wifiOfflineAlert', 'STRING'
		
        attribute "runtimeUpdated", 'NUMBER'
        attribute "alertsUpdated", 'NUMBER'
        attribute "settingsUpdated", 'NUMBER'
        attribute "programUpdated", 'NUMBER'
        attribute "climatesUpdated", 'NUMBER'
        attribute "scheduleUpdated", 'NUMBER'
        attribute "curClimRefUpdated", 'NUMBER'
        attribute "eventsUpdated", 'NUMBER'
        attribute "audioUpdated", 'NUMBER'
        attribute "extendRTUpdated", 'NUMBER'
        attribute "weatherUpdated", 'NUMBER'
        attribute "sensorsUpdated", 'NUMBER'
        attribute "equipUpdated", 'NUMBER'		
		
		attribute "version", "string"    
		
		command "asleep", 					[]						// We cannot overload the internal Java/Groovy definition of 'sleep()'
		command "auxHeatOnly", 				[]
		command "awake", 					[]
		command "away", 					[]
		command "fanOff", 					[]						// Missing from the Thermostat standard capability set
		command "forceRefresh",				[]
		command "home", 					[]		
        command	"microphoneOff",			[]
        command "microphoneOn",				[]
		command "night", 					[]						// this is probably the appropriate SmartThings device command to call, matches ST mode
		command "present", 					[]	
		command "resumeProgram", 			[]
		command "setDehumidifierMode", 		[[name:'Dehumidifier Mode*', type:'ENUM', description:'Select a (valid) Mode',
											  constraints: ['auto','off','on']]]
        command "setDehumiditySetpoint", 	[[name:'Dehumidity Setpoint*', type:'NUMBER', description:'Dehumidifier RH% setpoint (0-100)']]
		command "setEcobeeSetting", 		[[name:'Ecobee Setting Name*', type:'STRING', description:'Name of setting to change'],
											 [name:'Ecobee Setting Value*', type:'STRING', description:'New value for this setting']]
		command "setFanMinOnTime", 			[[name:'Fan Minimum On Time*', type:'NUMBER', description:'Minimum fan minutes/hour (0-55)']]
		command	"setHumidifierMode", 		[[name:'Humidifier Mode*', type:'ENUM', description:'Select a (valid) Mode',
											  constraints: ['auto','off','on']]]
		command "setHumiditySetpoint", 		[[name:'Humidity Setpoint*', type:'NUMBER', description:'Humidifier RH% setpoint (0-100)']]
		command "setProgramSetpoints", 		[[name:'Program Name*', type:'STRING', description:'Program to change'],
											 [name:'Heating Setpoint*', type:'NUMBER', description:'Heating setpoint temperature'],
											 [name:'Cooling Setpoint*', type:'NUMBER', description:'Cooling setpoint temperature']]
		command "setThermostatProgram", 	[[name:'Program Name*', type:'STRING', description:'Desired Program'],
											 [name:'Hold Type*', type:'ENUM', description:'Delect and option',
											  constraints: ['indefinite', 'nextTransition', 'holdHours']],
											 [name:'Hold Hours', type:'NUMBER', description:'Hours to Hold (if Hold Type == Hold Hours]']]
		command "wakeup", 					[]
		command "sync",						[]
	}
	preferences {
		input(name: "dummy", type: "text", title: "${getVersionLabel()}", description: " ", required: false)
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
	log.debug "initializing"
	unschedule()
	runEvery1Minute("refreshLRM")
	//forceRefresh()
	refreshLRM()
}
def parse(String description)
{
	log.trace "Msg: Description is $description"
}
def refreshLRM()
{
	// Update lastRunningMode from the lastRunningMode attribute (for Google Home)
	String lrmD = getDataValue("lastRunningMode")
	String lrmA = device.currentValue("lastRunningMode")
	if (lrmA && (lrmA != lrmD)) updateDataValue("lastRunningMode", lrmA)
}
def refresh()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "refresh")
}
/*
	Ecobee Suite Thermostat Commands
*/
def asleep()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "asleep")
}
def auto()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "auto")
}
def auxHeatOnly()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "auxHeatOnly")
}
def awake()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "awake")
}
def away()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "away")
}
def cool()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "cool")
}
def emergencyHeat()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "emergencyHeat")
}
def fanAuto()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "fanAuto")
}
def fanCirculate()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "fanCirculate")
}
def fanOff()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "fanOff")
}
def fanOn()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "fanOn")
}
def forceRefresh()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "forceRefresh")
}
def heat()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "heat")
}
def home()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "home")
}
def microphoneOff()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "microphoneOff")
}
def microphoneOn()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "microphoneOn")
}
def night()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "night")
}
def off()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "off")
}
def present()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "present")
}
def resumeProgram()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "resumeProgram")
}
def setCoolingSetpoint(temperature)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setCoolingSetpoint", [temperature])
}
def setDehumidifierMode(mode)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setDehumidifierMode", [mode])
}
def setDehumiditySetpoint(setpoint)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setDehumiditySetpoint", [setpoint])
}
def setEcobeeSetting(attribute, value)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setEcobeeSetting", [attribute, value])
}
def setFanMinOnTime(minutes)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setFanMinOnTime", [minutes])
}
def setHeatingSetpoint(temperature)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setHeatingSetpoint", [temperature])
}
def setHumidifierMode(mode)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setHumidifierMode", [mode])
}
def setHumiditySetpoint(setpoint)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setHumiditySetpoint", [setpoint])
}
def setProgramSetpoints(program, heat, cool)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setProgramSetpoints", [program, heat, cool])
}
def setSchedule(schedule)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setSchedule", [schedule.toString()])
}
def setThermostatFanMode(fanmode)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setThermostatFanMode", [fanmode])
}
def setThermostatMode(thermostatmode)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setThermostatMode", [thermostatmode])
}
def setThermostatProgram(program, holdType, holdHours)
{
	parent.sendDeviceEvent(device.deviceNetworkId, "setThermostatProgram", [program, holdType, holdHours])
}
def wakeup()
{
	parent.sendDeviceEvent(device.deviceNetworkId, "wakeup")
}
/*
	sync
    
	Synchronizes the device details with the parent.
*/
def sync()
{
	// The server will respond with updated status and details
	parent.syncDevice(device.deviceNetworkId, "ESThermostat")
	sendEvent([name: "version", value: "v${driverVersion.major}.${driverVersion.minor}.${driverVersion.build}"])
}
def getDriverVersion() {[platform: "Hubitat", major: 1, minor: 8, build: "01"]}
