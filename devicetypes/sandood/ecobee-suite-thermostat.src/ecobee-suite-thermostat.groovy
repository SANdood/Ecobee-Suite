/**
 *	Based on original version Copyright 2015 SmartThings
 *	Additions Copyright 2016 Sean Kendall Schneyer
 *	Additions Copyright 2017-2018 Barry A. Burke
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *	Ecobee Suite Thermostat
 *
 *	Author: SmartThings
 *	Date: 2013-06-13
 *
 *	Updates by Sean Kendall Schneyer <smartthings@linuxbox.org>
 *	Date: 2015-12-23
 *
 *	Updates by Barry A. Burke (storageanarchy@gmail.com)
 *	Date: 2017 - 2018
 *
 *	See Github Changelog for complete change history
 *
 * <snip>
 *	1.5.00 - Release number synchronization
 *	1.5.01 - Removed deprecated getLinkText()
 *	1.5.02 - Added more Weather data for Smart Mode
 *	1.5.03 - Converted all math to BigDecimal for better precision & rounding
 *	1.5.04 - Changed humidity/dehumidity setpoint attributes to "number"
 *	1.5.05 - Added reservations manager for better Helper cooperation
 *	1.6.00 - Release number synchronization
 *	1.6.02 - Fix reservations initialization error
 *	1.6.03 - Automatically re-run updated() if VersionLabel changes
 *	1.6.04 - Deprecated make/cancelReservation - moved to parent
 *	1.6.10 - Resync for Ecobee Suite Manager-based reservations
 *	1.6.11 - Deleted 'reservations' attribute - no longer used
 *	1.6.12 - Fixed type conversion error on Auto mode temp display
 *	1.6.13 - Clean up setpoint/setpointDisplay stuff (new isIOS & isAndroid)\
 *	1.6.14 - Another Android setpoint tweak
 *	1.6.15 - Missed one
 *	1.6.16 - Fixed initialization errors & priorities
 *	1.6.17 - Added a modicom of compatibility with the "new" Samsung (Connect)
 *	1.6.18 - Fixed coolingSetpoint error
 *	1.6.19 - Shortcut 'TestingForInstall' in installed()
 *	1.6.20 - Log uninstall also
 *	1.6.21 - Added setProgramSetpoints()
 *	1.6.22 - Expanded to include ALL settings as attributes
 *	1.6.23 - Changed equipment operating state to 'de/humidifying'
 *	1.6.24 - Added support for schedule/setSchedule (new Capability definition)
 *	1.7.00 - Initial Release of Universal Ecobee Suite
 *	1.7.01 - Improved thermostatHold testing
 *  1.7.03 - More thermostatHold optimizations
 *	1.7.04 - Big String fix
 *  1.7.05 - noCache most currentValue() for HE
 *	1.7.06 - More sendValue cleanup
 *	1.7.07 - Added dehumidifierLevel/setpoint, fix setFanMinOnTime()
 *	1.7.08 - Updating setpoints & climates fixed
 *	1.7.09 - Updating timeOfDay & weatherSymbol fixed
 *	1.7.10 - Cleanup state changes, fix thermostatOperatingStateDisplay
 *  1.7.11 - Don't accept null temperature, fixing private method issue caused by grails
 */
String getVersionNum() { return "1.7.11" }
String getVersionLabel() { return "Ecobee Suite Thermostat,\nversion ${getVersionNum()} on ${getPlatform()}" }
import groovy.json.*
import groovy.transform.Field

metadata {
	definition (name: "Ecobee Suite Thermostat", namespace: "sandood", author: "Barry A. Burke (storageanarchy@gmail.com)",
					mnmn: "SmartThings", vid: "SmartThings-smartthings-Z-Wave_Thermostat") {		// add (rough) approximations for the new Samsung (Connect) app
		capability "Thermostat"
		capability "Actuator"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Motion Sensor"
		capability "Sensor"
		capability "Refresh"

		// Extended Set of Thermostat Capabilities
		// capability "Thermostat Cooling Setpoint"
		// capability "Thermostat Fan Mode"
		// capability "Thermostat Heating Setpoint"
		// capability "Thermostat Mode"
		capability "Thermostat Operating State"
		capability "Thermostat Setpoint"
		capability "Health Check"

		command "asleep", 					[]						// We cannot overload the internal Java/Groovy definition of 'sleep()'
		// command "auto", 					[]
		command "auxHeatOnly", 				[]
		command "awake", 					[]
		command "away", 					[]
		command "cancelReservation", 		['string', 'string']
		command "cancelVacation", 			[]
		// command "cool", 					[]
		command "deleteVacation", 			['string']
		command "doRefresh", 				[]						// internal use by the refresh button
		command "emergency", 				[]	
		// command "emergencyHeat", 		[]
		// command "fanAuto", 				[]
		// command "fanCirculate", 			[]
		command "fanOff", 					[]						// Missing from the Thermostat standard capability set
		// command "fanOn", 				[]
		command "forceRefresh", 			[]	
		command "generateEvent"
		// command "heat", 					[]
		command "home", 					[]	
		command "lowerSetpoint", 			[]	
		command "makeReservation", 			[]	
		command "night", 					[]						// this is probably the appropriate SmartThings device command to call, matches ST mode
		command "noOp", 					[]						// Workaround for formatting issues
		// command "off", 					[]						// redundant - should be predefined by the Thermostat capability
		command "present", 					[]	
		command "raiseSetpoint", 			[]	
		command "resumeProgram", 			['string']
		command "setCoolingSetpointDelay",	['number']
		command "setDehumidifierMode", 		['string']
		command "setDehumiditySetpoint", 	['number']
		command "setEcobeeSetting", 		['string', 'string']	// Allows changes to (most) Ecobee Settings
		command "setFanMinOnTime", 			['number']
		command "setFanMinOnTimeDelay", 	['number']
		command "setHeatingSetpointDelay",	['number']
		command	"setHumidifierMode", 		['string']
		command "setHumiditySetpoint", 		['number']
		command "setProgramSetpoints", 		['string', 'number', 'number']
		// command "setSchedule"			['JSON_OBJECT']
		// command "setThermostatFanMode"	['string']
		// command "setThermostatMode"		['string']
		command "setThermostatProgram", 	['string', 'string', 'number']
		command "setVacationFanMinOnTime",	['number']
		command "wakeup", 					[]

		attribute 'apiConnected','string'
		attribute 'autoAway', 'string'
		attribute 'autoHeatCoolFeatureEnabled', 'string'
		attribute 'autoMode', 'string'
		attribute 'auxHeatMode', 'string'
		attribute 'auxMaxOutdoorTemp', 'number'
		attribute 'auxOutdoorTempAlert', 'number'		// temp
		attribute 'auxOutdoorTempAlertNotify', 'string'
		attribute 'auxRuntimeAlert', 'number'			// time
		attribute 'auxRuntimeAlertNotify', 'string'
		attribute 'auxOutdoorTempAlertNotifyTechnician', 'string'
		attribute 'auxRuntimeAlertNotifyTechnician', 'string'
		attribute 'backlightOffDuringSleep', 'string'
		attribute 'backlightOffTime', 'number'
		attribute 'backlightOnIntensity', 'number'
		attribute 'backlightSleepIntensity', 'number'
		attribute 'brand', 'string'
		attribute 'coldTempAlert', 'number'
		attribute 'coldTempAlertEnabled', 'string'		// or boolean?
		attribute 'compressorProtectionMinTemp', 'number'
		attribute 'compressorProtectionMinTime', 'number'
		attribute 'condensationAvoid', 'string'			// boolean
		attribute 'coolAtSetpoint', 'number'
		attribute 'coolDifferential', 'number'
		attribute 'coolMaxTemp', 'number'				// Read Only
		attribute 'coolMinTemp', 'number'				// Read Only
		attribute 'coolMode', 'string'
		attribute 'coolRange', 'string'
		attribute 'coolRangeHigh', 'number'
		attribute 'coolRangeLow', 'number'
		attribute 'coolStages', 'number'				// Read Only
		attribute 'coolingLockout', 'string'
		attribute 'coolingSetpointDisplay', 'number'	// Now the same as coolingSetpoint
		attribute 'coolingSetpointMax', 'number'
		attribute 'coolingSetpointMin', 'number'
		attribute 'coolingSetpointRange', 'vector3'
		attribute 'coolingSetpointTile', 'number'		// Used to show coolAt/coolTo for MultiTile
		attribute 'coldTempAlert', 'number'
		attribute 'coldTempAlertEnabled', 'string'
		attribute 'currentProgram','string'
		attribute 'currentProgramId','string'
		attribute 'currentProgramName', 'string'
		attribute 'debugEventFromParent','string'
		attribute 'debugLevel', 'number'
		attribute 'decimalPrecision', 'number'
		attribute 'dehumidifierLevel', 'number'		// should be same as dehumiditySetpoint
		attribute 'dehumidifierMode', 'string'
		attribute 'dehumidifyOvercoolOffset', 'number'
		attribute 'dehumidifyWhenHeating', 'string'
		attribute 'dehumidifyWithAC', 'string'
        attribute 'dehumidityLevel', 'number'
		attribute 'dehumiditySetpoint', 'number'
		attribute 'disableAlertsOnIdt', 'string'
		attribute 'disableHeatPumpAlerts', 'string'
		attribute 'disablePreCooling', 'string'
		attribute 'disablePreHeating', 'string'
		attribute 'drAccept', 'string'
		attribute 'ecobeeConnected', 'string'
		attribute 'eiLocation', 'string'
		attribute 'electricityBillCycleMonths', 'string'
		attribute 'electricityBillStartMonth', 'string'
		attribute 'electricityBillingDayOfMonth', 'string'
		attribute 'enableElectricityBillAlert', 'string'
		attribute 'enableProjectedElectricityBillAlert', 'string'
		attribute 'equipmentOperatingState', 'string'
		attribute 'equipmentStatus', 'string'
		attribute 'fanControlRequired', 'string'
		attribute 'fanMinOnTime', 'number'
		attribute 'features','string'
		attribute 'followMeComfort', 'string'
		attribute 'groupName', 'string'
		attribute 'groupRef', 'string'
		attribute 'groupSetting', 'string'
		attribute 'hasBoiler', 'string'					// Read Only
		attribute 'hasDehumidifier', 'string'			// Read Only
		attribute 'hasElectric', 'string'				// Read Only
		attribute 'hasErv', 'string'					// Read Only
		attribute 'hasForcedAir', 'string'				// Read Only
		attribute 'hasHeatPump', 'string'				// Read Only
		attribute 'hasHrv', 'string'					// Read Only
		attribute 'hasHumidifier', 'string'				// Read Only
		attribute 'hasUVFilter', 'string'				// Read Only
		attribute 'heatAtSetpoint', 'number'
		attribute 'heatCoolMinDelta', 'number'
		attribute 'heatDifferential', 'number'
		attribute 'heatMaxTemp', 'number'				// Read Only
		attribute 'heatMinTemp', 'number'				// Read Only
		attribute 'heatMode', 'string'
		attribute 'heatPumpGroundWater', 'string'				// Read Only
		attribute 'heatPumpReversalOnCool', 'string'
		attribute 'heatRange', 'string'
		attribute 'heatRangeHigh', 'number'
		attribute 'heatRangeLow', 'number'
		attribute 'heatStages', 'number'				// Read Only
		attribute 'heatingSetpointDisplay', 'number'	// now the same as heatingSetpoint
		attribute 'heatingSetpointMax', 'number'
		attribute 'heatingSetpointMin', 'number'
		attribute 'heatingSetpointRange', 'vector3'
		attribute 'heatingSetpointTile', 'number'		// Used to show heatAt/heatTo for MultiTile
		attribute 'holdAction', 'string'
		attribute 'holdEndsAt', 'string'
		attribute 'holdStatus', 'string'
		attribute 'hotTempAlert', 'number'
		attribute 'hotTempAlertEnabled', 'string'
		attribute 'humidifierMode', 'string'
		attribute 'humidityAlertNotify', 'string'
		attribute 'humidityAlertNotifyTechnician', 'string'
		attribute 'humidityHighAlert', 'number'			// %
		attribute 'humidityLowAlert', 'number'
		attribute 'humiditySetpoint', 'number'
		attribute 'humiditySetpointDisplay', 'string'
		attribute 'identifier', 'string'
		attribute 'installerCodeRequired', 'string'
		attribute 'isRegistered', 'string' 
		attribute 'isRentalProperty', 'string'
		attribute 'isVentilatorTimerOn', 'string'
		attribute 'lastHoldType', 'string'
		attribute 'lastModified', 'string' 
		attribute 'lastOpState', 'string'				// keeps track if we were most recently heating or cooling
		attribute 'lastPoll', 'string'
		attribute 'lastServiceDate', 'string'
		attribute 'locale', 'string'
		attribute 'logo', 'string'
		attribute 'maxSetBack', 'number'
		attribute 'maxSetForward', 'number'
		attribute 'mobile', 'string'
		attribute 'modelNumber', 'string' 
		attribute 'monthlyElectricityBillLimit', 'string'
		attribute 'monthsBetweenService', 'string'
		attribute 'motion', 'string'
		attribute 'name', 'string'
		attribute 'programsList', 'string'				// USAGE: List programs = new JsonSlurper().parseText(stat.currentValue('programsList'))
		attribute 'quickSaveSetBack', 'number'
		attribute 'quickSaveSetForward', 'number'
		attribute 'randomStartDelayCool', 'string'
		attribute 'randomStartDelayHeat', 'string'
		attribute 'remindMeDate', 'string'
		attribute 'schedText', 'string'
		attribute 'schedule', 'string'					// same as 'currentProgram'
		attribute 'scheduledProgram','string'
		attribute 'scheduledProgramId','string'
		attribute 'scheduledProgramName', 'string'
		attribute 'serviceRemindMe', 'string'
		attribute 'serviceRemindTechnician', 'string'
		attribute 'setpointDisplay', 'string'
		attribute 'smartCirculation', 'string'			// not implemented by E3, but by this code it is
		attribute 'soundAlertVolume', 'number'
		attribute 'soundTickVolume', 'number'
		attribute 'stage1CoolingDifferentialTemp', 'number'
		attribute 'stage1CoolingDissipationTime', 'number'
		attribute 'stage1HeatingDifferentialTemp', 'number'
		attribute 'stage1HeatingDissipationTime', 'number'
		attribute 'statHoldAction', 'string'
		attribute 'supportedThermostatFanModes', 'JSON_OBJECT' // USAGE: List theFanModes = stat.currentValue('supportedThermostatFanModes')[1..-2].tokenize(', ')
		attribute 'supportedThermostatModes', 'JSON_OBJECT' // USAGE: List theModes = stat.currentValue('supportedThermostatModes')[1..-2].tokenize(', ')
		attribute 'supportedVentModes', 'JSON_OBJECT' // USAGE: List theModes = stat.currentValue('supportedVentModes')[1..-2].tokenize(', ')
		attribute 'tempAlertNotify', 'string'
		attribute 'tempAlertNotifyTechnician', 'string'
		attribute 'tempCorrection', 'number'
		attribute 'temperatureDisplay', 'string'
		attribute 'temperatureScale', 'string'
		attribute 'thermostatFanModeDisplay', 'string'
		attribute 'thermostatHold', 'string'
		attribute 'thermostatOperatingStateDisplay', 'string'
		attribute 'thermostatSetpoint','number'
		attribute 'thermostatSetpointMax', 'number'
		attribute 'thermostatSetpointMin', 'number'
		attribute 'thermostatSetpointRange', 'vector3'
		attribute 'thermostatStatus','string'
		attribute 'thermostatTime', 'string'
		attribute 'timeOfDay', 'enum', ['day', 'night']
		attribute 'userAccessCode', 'string'			// Read Only
		attribute 'userAccessSetting', 'string'			// Read Only
		attribute 'vent', 'string'						// same as 'ventMode'
		attribute 'ventMode', 'string'					// Ecobee actually calls it only 'vent'
		attribute 'ventilatorDehumidify', 'string'
		attribute 'ventilatorFreeCooling', 'string'
		attribute 'ventilatorMinOnTime', 'number'
		attribute 'ventilatorMinOnTimeAway', 'number'
		attribute 'ventilatorMinOnTimeHome', 'number'
		attribute 'ventilatorOffDateTime', 'string'		// Read Only
		attribute 'ventilatorType', 'string'			// Read Only ('none', 'ventilator', 'hrv', 'erv')
		attribute 'weatherDewpoint', 'number'
		attribute 'weatherHumidity', 'number'
		attribute 'weatherPressure', 'number'
		attribute 'weatherSymbol', 'string'
		attribute 'weatherTemperature', 'number'
		attribute 'wifiOfflineAlert', 'string'
	}

	simulator { }

	tiles(scale: 2) {
		multiAttributeTile(name:"temperatureDisplay", type:"thermostat", width:6, height:4, canChangeIcon: true) {
			tileAttribute("device.temperatureDisplay", key: "PRIMARY_CONTROL") {
				attributeState("default", label:'${currentValue}', /* unit:"dF", */ defaultState: true)
			}
			tileAttribute("device.thermostatSetpoint", key: "VALUE_CONTROL") {
				attributeState("VALUE_UP", action: "raiseSetpoint")
				attributeState("VALUE_DOWN", action: "lowerSetpoint")
//				  attributeState("default", label: '${currentValue}°', defaultState:true)
//				  attributeState("default", action: "setTemperature")
			}
			tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'${currentValue}%', unit:"Humidity", defaultState: true)
			}
			tileAttribute('device.thermostatOperatingStateDisplay', key: "OPERATING_STATE") {
				attributeState('idle',						backgroundColor:"#d28de0")		// ecobee purple/magenta
				attributeState('fan only',					backgroundColor:"#66cc00")		// ecobee green
				attributeState('heating',					backgroundColor:"#ff9c14")		// ecobee flame orange
				attributeState('cooling',					backgroundColor:"#2db9e7")		// ecobee snowflake blue
				attributeState('heating (smart recovery)',	backgroundColor:"#ff9c14")		// ecobee flame orange
				attributeState('cooling (smart recovery)',	backgroundColor:"#2db9e7")		// ecobee snowflake blue
				attributeState('cooling (overcool)',		backgroundColor:"#2db9e7")
				attributeState('offline',					backgroundColor:"#ff4d4d")
				attributeState('off',						backGroundColor:"#cccccc")		// grey - iOS only???
				attributeState('default',					backgroundColor:"#d28de0", defaultState: true)
			}
			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", label:'${name}')
				attributeState("heat", label:'${name}')
				attributeState("cool", label:'${name}')
				attributeState("auto", label:'${name}')
				attributeState('default', label:'${name}', defaultState: true)
			}
			tileAttribute("device.heatingSetpointTile", key: "HEATING_SETPOINT") {
				attributeState("default", label:'${currentValue}°', /* unit:"dF", */ defaultState: true)
			}
			tileAttribute("device.coolingSetpointTile", key: "COOLING_SETPOINT") {
				attributeState("default", label:'${currentValue}°', /* unit:"dF", */ defaultState: true)
			}
		}
		// Show status of the API Connection for the Thermostat
		standardTile("apiStatus", "device.apiConnected", width: 1, height: 1) {
			state "full", label: 'FULL', backgroundColor: "#44b621", icon: "st.contact.contact.closed"
			state "warn", label: 'WARN', backgroundColor: "#e86d13", icon: "st.contact.contact.open"
			state "lost", label: 'LOST', backgroundColor: "#bc2323", icon: "st.contact.contact.open"
		}
		valueTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: true, decoration: 'flat') {
			// Use the first version below to show Temperature in Device History - will also show Large Temperature when device is default for a room
			//		The second version will show icon in device lists
			state("default", label:'${currentValue}°', unit:"F", backgroundColors: getTempColors(), defaultState: true)
			//state("default", label:'${currentValue}°', unit:"F", backgroundColors: getTempColors(), defaultState: true, icon: 'st.Weather.weather2')
		}
		// these are here just to get the colored icons to diplay in the Recently log in the Mobile App
		valueTile("heatingSetpointDisplay", "device.heatingSetpointDisplay", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("default", label:'${currentValue}°', /* unit:"dF", */ backgroundColor:"#ff9c14", defaultState: true)
		}
		valueTile("coolingSetpointDisplay", "device.coolingSetpointDisplay", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("default", label:'${currentValue}°', /* unit:"dF", */ backgroundColor:"#2db9e7", defaultState: true)
		}
		valueTile("heatingSetpointColor", "device.heatingSetpoint", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("default", label:'${currentValue}°', /* unit:"dF", */ backgroundColor:"#ff9c14", defaultState: true)
		}
		valueTile("coolingSetpointColor", "device.coolingSetpoint", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("default", label:'${currentValue}°', /* unit:"dF", */ backgroundColor:"#2db9e7", defaultState: true)
		}
		valueTile("thermostatSetpointColor", "device.thermostatSetpoint", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("default", label:'${currentValue}°', /* unit:"dF", */	backgroundColors: getTempColors(), defaultState: true)
		}
		valueTile("weatherTempColor", "device.weatherTemperature", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("default", label:'${currentValue}°', /* unit:"dF", */	backgroundColors: getStockTempColors(), defaultState: true)		// use Fahrenheit scale so that outdoor temps register
		}
		valueTile("weatherDewpointColor", "device.weatherDewpoint", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("default", label:'${currentValue}°', /* unit:"dF", */	backgroundColors: getStockTempColors(), defaultState: true)		// use Fahrenheit scale so that outdoor temps register
		}
		valueTile("fanMinOnTimeColor", "device.fanMinOnTime", width: 2, height: 2, decoration: "flat") {
			state("default", label: '${currentValue}′', backgroundColor: "#808080", defaultState: true)
		}
		standardTile("mode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
			state "off",			label: "Mode: Off",		action:"heat",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_label.png"
			state "heat",			label: "Mode: Heat",	action:"cool",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_heat.png"
			state "cool",			label: "Mode: Cool",	action:"auto",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_cool.png"
			state "auto",			label: "Mode: Auto",	action:"off",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_auto.png"
			// Not included in the button loop, but if already in "auxHeatOnly" pressing button will go to "auto"
			state "auxHeatOnly",	label: "Mode: Aux Heat",action:"auto",			nextState: "updating",		icon: "st.thermostat.emergency-heat"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		standardTile("modeShow", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
			state "off",			label: "Off",			action:"noOp",			nextState: "off",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_label.png"
			state "heat",			label: "Heat",			action:"noOp",			nextState: "heat",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_heat.png"
			state "cool",			label: "Cool",			action:"noOp",			nextState: "cool",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_cool.png"
			state "auto",			label: "Auto",			action:"noOp",			nextState: "auto",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_auto.png"
			// Not included in the button loop, but if already in "auxHeatOnly" pressing button will go to "auto"
			state "auxHeatOnly",	label: 'Aux Heat',		action:"noOp",			nextState: "auto",			icon: "st.thermostat.emergency-heat"
			state "emergency",		label: 'Emergency',		action:"noOp",			nextState: "auto",			icon: "st.thermostat.emergency-heat"
			state "emergencyHeat",	label: 'Emergency Heat',action:"noOp",			nextState: "auto",			icon: "st.thermostat.emergency-heat"
			state "updating",		label: 'Working',															icon: "st.motion.motion.inactive"
		}
		standardTile("setModeHeat", "device.setModeHeat", width: 2, height: 2, decoration: "flat") {
			state "heat",			label: "Heat",			action:"heat",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_heat.png"
			state "heat dis",		label: "Heat",			action:"noOp",			nextState: "heat dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_heat_grey.png"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		standardTile("setModeCool", "device.setModeCool", width: 2, height: 2, decoration: "flat") {
			state "cool",			label: "Cool",			action:"cool",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_cool.png"
			state "cool dis",		label: "Cool",			action:"noOp",			nextState: "cool dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_cool_grey.png"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		standardTile("setModeAuto", "device.setModeAuto", width: 2, height: 2, decoration: "flat") {
			state "auto",			label: "Auto",			action:"auto",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_auto.png"
			state "auto dis",		label: "Auto",			action:"noOp",			nextState: "auto dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_auto_grey.png"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		standardTile("setModeOff", "device.setModeOff", width: 2, height: 2, decoration: "flat") {
			state "off",			label: "HVAC Off",		action:"off",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_label.png"
			state "off dis",		label: "HVAC Off",		action:"noOp",			nextState: "off dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_label_grey.png"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		// This one is the one that will control the icon displayed in device Messages log - but we don't actually use it
		standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
			state "on",				label:'Fan: On',		action:"fanAuto",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
			state "auto",			label:'Fan: Auto',		action:"fanOn",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
			state "off",			label:'Fan: Off',		action:"fanAuto",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
			state "circulate",		label:'Fan: Circulate',	action:"fanAuto",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1.png"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		standardTile("fanModeLabeled", "device.thermostatFanModeDisplay", width: 2, height: 2, decoration: "flat") {
			state "on",				label:'Fan: On',		action:"fanCirculate",	nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
			state "auto",			label:'Fan: Auto',		action:"fanOn",			nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
			state "off",			label:'Fan: Off',		action:"fanAuto",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
			state "circulate",		label:'Fan: Circulate', action:"fanAuto",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1.png"
			state "on dis",			label:'Fan: On',		action:"noOp",			nextState: "on dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_grey.png"
			state "auto dis",		label:'Fan: Auto',		action:"noOp",			nextState: "auto dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_grey.png"
			state "off dis",		label:'Fan: Off',		action:"fanCirculate",	nextState: "circulate dis",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
			state "circulate dis",	label:'Fan: Circulate',	action:"fanOff",		nextState: "off dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1.png"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		standardTile("fanOffButton", "device.setFanOff", width: 2, height: 2, decoration: "flat") {
			state "off",			label:"Fan Off",		action:"fanOff",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
			state "off dis",		label:"Fan Off",		action:"fanOffDisabled",nextState: "off dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_grey.png"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		standardTile("fanCirculate", "device.setFanCirculate", width: 2, height: 2, decoration: "flat") {
			state "circulate",		label:"Fan Circulate", action:"fanCirculate",	nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_big_solid_nolabel.png"
			state "updating",		label:"Working",															icon: "st.motion.motion.inactive"
		}
		standardTile("fanModeCycler", "device.thermostatFanModeDisplay", width: 2, height: 2, decoration: "flat") {
			state "auto",			action:"fanOn",			label: "Fan On",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
			state "on",				action:"fanCirculate",	label: "Fan Circulate",	nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1.png"
			state "off",			action:"fanAuto",		label: "Fan Auto",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
			state "circulate",		action:"fanAuto",		label: "Fan Auto",		nextState: "updating",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
			state "on dis",			action:"noOp",			label: "Fan Circulate", nextState: "on dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1_grey.png"
			state "auto dis",		action:"noOp",			label: "Fan On",		nextState: "auto dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_grey.png"
			state "off dis",		action:"noOp",			label: "Fan Auto",		nextState: "off dis",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_grey.png"
			state "circulate dis",	action:"noOp",			label: "Fan Auto",		nextState: "circulate dis",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_grey.png"
			state "updating",								label:"Working",									icon: "st.motion.motion.inactive"
		}
		standardTile("upButtonControl", "device.thermostatSetpoint", width: 2, height: 1, decoration: "flat") {
			state "setpoint", action:"raiseSetpoint", icon:"st.thermostat.thermostat-up", defaultState: true
		}
		valueTile("thermostatSetpoint", "device.thermostatSetpoint", width: 2, height: 2, decoration: "flat") {
			state "thermostatSetpoint", label:'${currentValue}°', backgroundColors: getTempColors(), defaultState: true
		}
		valueTile("currentStatus", "device.thermostatStatus", height: 2, width: 4, decoration: "flat") {
			state "thermostatStatus", label:'${currentValue}', backgroundColor:"#ffffff", defaultState: true
		}
		standardTile("downButtonControl", "device.thermostatSetpoint", height: 1, width: 2, decoration: "flat") {
			state "setpoint", action:"lowerSetpoint", icon:"st.thermostat.thermostat-down", defaultState: true
		}
		controlTile("circSliderControl", "device.fanMinOnTime", "slider", height: 1, width: 1, inactiveLabel: false, range: "(0..55)" ) {
			state "setCircSetpoint", action:"setFanMinOnTimeDelay",	 backgroundColor:"#aaaaaa", unit: '', defaultState: true
		}
		controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 1, inactiveLabel: false, range: getSliderRange() /* "(15..85)" */ ) {
			state "setHeatingSetpoint", action:"setHeatingSetpointDelay",  backgroundColor:"#ff9c14", /* unit:"dF", */ defaultState: true
		}
		valueTile("heatingSetpoint", "device.heatAtSetpoint", height: 1, width: 1, decoration: "flat") {
			state "heat", label:'Heat\nat\n${currentValue}°', defaultState: true//, unit:"F", backgroundColor:"#ff9c14"
		}
		controlTile("coolSliderControl", "device.coolingSetpoint", "slider", height: 1, width: 1, inactiveLabel: false, range: getSliderRange() /* "(15..85)" */ ) {
			state "setCoolingSetpoint", action:"setCoolingSetpointDelay", backgroundColor:"#2db9e7", /* unit:"dF", */ defaultState: true
		}
		valueTile("coolingSetpoint", "device.coolAtSetpoint", width: 1, height: 1, decoration: "flat") {
			state "cool", label:'Cool\nat\n${currentValue}°', defaultState: true //, unit:"F", backgroundColor:"#2db9e7"
		}
		controlTile("humiditySlider", "device.humiditySetpoint", "slider", height: 1, width: 1, inactiveLabel: false, range: "(0..100)") {
			state "setpoint", action: "setHumiditySetpoint", unit: '', defaultState: true // , backgroundColors: [[value: 10, color: "#0033cc"],[value: 60, color: "#ff66ff"]]
		}
		valueTile('humiditySetpoint', 'device.humiditySetpoint', width: 1, height: 1, decoration: 'flat') {
			state 'humidity', label: 'Humidify\nto\n${currentValue}%', defaultState: true
		}
		controlTile("dehumiditySlider", "device.dehumiditySetpoint", "slider", height: 1, width: 1, inactiveLabel: false, range: "(0..100)") {
			state "setpoint", action: "setDehumiditySetpoint", unit: "", defaultState: true
		}
		valueTile('dehumiditySetpoint', 'device.dehumiditySetpoint', width: 1, height: 1, decoration: 'flat') {
			state 'humidity', label: 'Dehum\'fy\nto\n${currentValue}%', defaultState: true
		}
		standardTile("refresh", "device.doRefresh", width: 2, height: 2, decoration: "flat") {
			state "refresh", action:"doRefresh", nextState: 'updating', /*label: "Refresh",*/ defaultState: true, icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/ecobee_refresh_green.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
		standardTile("resumeProgram", "device.resumeProgram", width: 2, height: 2, decoration: "flat") {
			state "resume",				action:"resumeProgram",		nextState: "updating",		label:'Resume',		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/action_resume_program.png"
			state "resume dis",			action: 'noOp',				nextState: 'resume dis',	label: 'Resume',	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/action_resume_program_grey.png"
			state "cancel",				action:"cancelVacation",	nextState: "updating",		label:'Cancel',		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_vacation_airplane_yellow.png"
			state "updating",																	label:"Working",	icon: "st.motion.motion.inactive"
		}
		standardTile("currentProgramIcon", "device.currentProgramName", height: 2, width: 2, decoration: "flat") {
			state "Home",				action:"noOp", nextState:'Home',				label: 'Home',				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png"
			state "Away",				action:"noOp", nextState:'Away',				label: 'Away',				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png"
			state "Sleep",				action:"noOp", nextState:'Sleep',				label: 'Sleep',				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue.png"
			state "Awake",				action:"noOp", nextState:'Awake',				label: 'Awake',				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake.png"
			state "Wakeup",				action:"noOp", nextState:'Wakeup',				label: 'Wakeup',			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake.png"
			state "Awake",				action:"noOp", nextState:'Awake',				label: 'Awake',				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake.png"
			state "Auto",				action:"noOp", nextState:'Auto',				label: 'Auto',				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
			state "Auto Away",			action:"noOp", nextState:'Auto Away',			label: 'Auto Away',			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_auto_away.png" // Fix to auto version
			state "Auto Home",			action:"noOp", nextState:'Auto Home',			label: 'Auto Home',			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_auto_home.png" // Fix to auto
			state "Hold",				action:"noOp", nextState:'Hold',				label: 'Hold',				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
			state "Hold: Fan",			action:"noOp", nextState:'Hold: Fan',			label: "Hold: Fan",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_blue.png"
			state "Hold: Fan On",		action:"noOp", nextState:'Hold: Fan on',		label: "Hold: Fan On",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_blue.png"
			state "Hold: Fan Auto",		action:"noOp", nextState:'Hold: Fan Auto',		label: "Hold: Fan Auto",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_blue.png"
			state "Hold: Circulate",	action:"noOp", nextState:'Hold: Circulate',		label: "Hold: Circulate",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1_blue..png"
			state "Hold: Home",			action:"noOp", nextState:'Hold: Home',			label: 'Hold: Home',		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue_solid.png"
			state "Hold: Away",			action:"noOp", nextState:'Hold: Away',			label: 'Hold: Away',		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue_solid.png"
			state "Hold: Sleep",		action:"noOp", nextState:'Hold: Sleep',			label: 'Hold: Sleep',		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue_solid.png"
			state "Vacation",			action:"noOp", nextState:'Vacation',			label: 'Vacation',			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_vacation_blue_solid.png"
			state "Offline",			action:"noOp", nextState:'Offline',				label: 'Offline',			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_black_dot.png"
			state "Hold: Temp",			action:'noOp', nextState: 'Hold: Temp',			label: 'Hold: Temp',		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/thermometer_hold.png"
			state "Hold: Wakeup",		action:"noOp", nextState:'Hold: Wakeup',		label: 'Hold: Wakeup',		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake_blue.png"
			state "Hold: Awake",		action:"noOp", nextState:'Hold: Awake',			label: 'Hold: Awake',		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake_blue.png"
			state "default",			action:"noOp", nextState: 'default',			label: '${currentValue}',	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png", defaultState: true
		}
		valueTile("currentProgram", "device.currentProgramName", height: 2, width: 4, decoration: "flat") {
			state "default", label:'Comfort Setting:\n${currentValue}', defaultState: true
		}
		standardTile("setHome", "device.setHome", width: 2, height: 2, decoration: "flat") {
			state "home",		action:"home",			nextState: "updating",	label:'Home',		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png"
			state "home dis",	action:"homeDisabled",	nextState: "home dis",	label:'Home',		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_grey.png"
			state "updating",													label:"Working",	icon: "st.motion.motion.inactive"
		}
		standardTile("setAway", "device.setAway", width: 2, height: 2, decoration: "flat") {
			state "away",		action:"away",			nextState: "updating",	label:'Away',		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png"
			state "away dis",	action:"awayDisabled",	nextState: "away dis",	label:'Away',		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_grey.png"
			state "updating",													label:"Working",	icon: "st.motion.motion.inactive"
		}
		standardTile("setSleep", "device.setSleep", width: 2, height: 2, decoration: "flat") {
			// state "sleep", action:"sleep", nextState: "updating", label:'Set Sleep', icon:"st.Bedroom.bedroom2"
			// can't call "sleep()" because of conflict with internal definition (it silently fails)
			state "sleep",		action:"night",			nextState: "updating",	label:'Sleep',		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue.png"
			state "sleep dis",	action:"nightDisabled", nextState: "sleep dis", label:'Sleep',		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_grey.png"
			state "updating",													label:"Working",	icon: "st.motion.motion.inactive"
		}
		standardTile("operatingState", "device.thermostatOperatingState", width: 2, height: 2, decoration: "flat") {
			state "idle",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_idle.png"
			state "fan only",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "heating",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "cooling",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
			state "offline",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
			state "default",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", label: '${currentValue}', defaultState: true
		}
		standardTile("operatingStateDisplay", "device.thermostatOperatingStateDisplay", width: 2, height: 2, decoration: "flat") {
			state "idle",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_idle.png"
			state "fan only",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "heating",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "cooling",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
			state "heating (smart recovery)",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "cooling (smart recovery)",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
			state 'cooling (overcool)',			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
			state "offline",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
			state "off",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_purple.png"
			state "default",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", label: '${currentValue}', defaultState: true
		}
		standardTile("equipmentState", "device.equipmentOperatingState", width: 2, height: 2, decoration: "flat") {
			state "idle",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_idle.png"
			state "fan only",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "emergency",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_emergency.png"
			state "auxHeatOnly",				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_emergency.png"
			state "heat pump",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "heat 1",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_1.png"
			state "heat 2",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2.png"
			state "heat 3",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3.png"
			state "heat pump 2",				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2.png"
			state "heat pump 3",				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3.png"
			state "cool 1",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_1.png"
			state "cool 2",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_2.png"
			state "heating",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//operatingstate_heat.png"
			state "cooling",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
			state "emergency hum",				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_emergency+humid.png"
			state "auxHeatOnly hum",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_emergency+humid.png"
			state "heat pump hum",				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat+humid.png"
			state "heat 1 hum",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_1+humid.png"
			state "heat 2 hum",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2+humid.png"
			state "heat 3 hum",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3+humid.png"
			state "heat pump 2 hum",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2+humid.png"
			state "heat pump 3 hum",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3+humid.png"
			state "cool 1 deh",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_1-humid.png"
			state "cool 2 deh",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_2-humid.png"
			state "heating hum",				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat+humid.png"
			state "cooling deh",				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool-humid.png"
			state "humidifier",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_humidifier_only.png"
			state "dehumidifier",				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_dehumidifier_only.png"
			state "offline",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
			state "off",						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_purple.png"
			state "default",					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", action:"noOp", label: '${currentValue}', defaultState: true
		}
		valueTile("humidity", "device.humidity", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
				[value: 10, color: "#0033cc"],
				[value: 60, color: "#ff66ff"]
			] )
		}
		valueTile("weatherHumidity", "device.weatherHumidity", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
				[value: 10, color: "#0033cc"],
				[value: 60, color: "#ff66ff"]
			] )
		}
		valueTile('humiditySetpointDisplay', 'device.humiditySetpointDisplay', decoration: 'flat', width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
				[value: 10, color: "#0033cc"],
				[value: 60, color: "#ff66ff"]
			] )
		}
		standardTile("motionState", "device.motion", width: 2, height: 2, decoration: "flat") {
			state "active",			action:"noOp",	nextState: "active",		label:"Motion",		icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_motion.png"
			state "inactive",		action: "noOp", nextState: "inactive",		label:"No Motion",	icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_nomotion.png"
			state "not supported",	action: "noOp", nextState: "not supported", label: "N/A",		icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/notsupported_x.png"
			state "unknown",		action: "noOp", nextState: "unknown",		label:"Offline",	icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_noconnection.png"
		}
		// Weather Tiles and other Forecast related tiles
		standardTile("weatherIcon", "device.weatherSymbol", width: 2, height: 2, decoration: "flat") {
			state "-2",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_updating_-2_fc.png"				// , label: 'updating...',	action: 'noOp'
			state "0",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_sunny_00_fc.png"					// , label: 'Sunny',		action: 'noOp'
			state "1",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png"			// , label: 'Msly Sun',		action: 'noOp'
			state "2",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png"			// , label: 'Ptly Cldy',	action: 'noOp'
			state "3",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png"			// , label: 'Msly Cldy',	action: 'noOp'
			state "4",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_cloudy_04_fc.png"					// , label: 'Cloudy',		action: 'noOp'
			state "5",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_drizzle_05_fc.png"
			state "6",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_rain_06_fc.png"
			state "7",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png"
			state "8",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_rain_06_fc.png"
			state "9",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png"
			state "10",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_snow_10_fc.png"
			state "11",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_flurries_11_fc.png"
			state "12",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png"
			state "13",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_snow_10_fc.png"
			state "14",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png"
			state "15",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_thunderstorms_15_fc.png"
			state "16",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_windy_16.png"
			state "17",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_tornado_17.png"
			state "18",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"
			state "19",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"						// Hazy
			state "20",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"						// Smoke
			state "21",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"						// Dust
			// Night Time Icons (Day time Value + 100)
			state "100",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_clear_night_100_fc.png"			// , label: 'Clear'
			state "101",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png"	// , label: 'Msly Sun'
			state "102",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png"	// , label: 'Ptly Cldy'
			state "103",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_mostly_cloudy_103_fc.png"	// , label: 'Msly Cldy'
			state "104",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_cloudy_04_fc.png"					// , label: 'Cloudy'
			state "105",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_drizzle_105_fc.png"			// , label: 'Drizzle'
			state "106",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png"				// , label: 'Rain'
			state "107",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png"	// , label: 'Frz Rain'
			state "108",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png"				// , label: 'Rain'
			state "109",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png"	// , label: 'Frz Rain'
			state "110",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_night_snow_110_fc.png"			// , label: 'Snow'
			state "111",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png"			// , label: 'Flurries'
			state "112",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png"	// , label: 'Frz Rain'
			state "113",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_snow_110_fc.png"				// , label: 'Snow'
			state "114",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png"	// , label: 'Frz Rain'
			state "115",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_thunderstorms_115_fc.png"	// , label: 'T-Storms'
			state "116",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_windy_16.png"						// , label: 'Windy'
			state "117",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_tornado_17.png"					// , label: 'Tornado'
			state "118",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"						// , label: 'Fog'
			state "119",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"						// , label: 'Hazy'
			state "120",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"						// , label: 'Smoke'
			state "121",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"						// , label: 'Dust'
		}
		standardTile("weatherTemperature", "device.weatherTemperature", width: 2, height: 2, decoration: "flat") {
			state "default", action: "noOpWeatherTemperature", nextState: "default", label: 'Out: ${currentValue}°', defaultState: true,
								icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/thermometer_fc.png"
		}
		valueTile("lastPoll", "device.lastPoll", height: 1, width: 5, decoration: "flat") {
			state "thermostatStatus",	label:'Last Poll: ${currentValue}', defaultState: true, backgroundColor:"#ffffff"
		}
		valueTile("holdStatus", "device.holdStatus", height: 1, width: 4, decoration: "flat") {
			state "default",			label:'${currentValue}', defaultState: true //, backgroundColor:"#000000", foregroudColor: "#ffffff"
		}
//		  standardTile("ecoLogo", "device.logo", width: 1, height: 1) {
//			state "default", defaultState: true,  icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/header_ecobeeicon_blk.png"
//		}
//		  standardTile("oneBuffer", "device.logo", width: 1, height: 1, decoration: "flat") {
//			state "default", defaultState: true
//		  }
//		  standardTile('twoByTwo', 'device.logo', width: 2, height: 2, decoration: 'flat') {
//			state "default", defaultState: true
//		  }
//		  standardTile('oneByThree', 'device.logo', width: 1, height: 3, decoration: 'flat') {
//			state "default", defaultState: true
//		  }
		valueTile("fanMinOnTime", "device.fanMinOnTime", width: 1, height: 1, decoration: "flat") {
			state "fanMinOnTime",	label: 'Circulate\n${currentValue}\nmin/hr', defaultState: true
		}
		valueTile("tstatDate", "device.tstatDate", width: 1, height: 1, decoration: "flat") {
			state "default",		label: '${currentValue}', defaultState: true
		}
		valueTile("tstatTime", "device.tstatTime", width: 1, height: 1, decoration: "flat") {
			state "default",		label: '${currentValue}', defaultState: true
		}
		standardTile("commandDivider", "device.logo", width: 4, height: 1, decoration: "flat") {
			state "default",		icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/command_divider.png", defaultState: true
		}
		standardTile("circulating", "device.thermostatFanModeDisplay", width:1, height:1, decoration: "flat") {
			state "default",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on-1_grey.png", defaultState: true
			state "circulate",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on-1.png"
		}
		standardTile("cooling", "device.thermostatMode", width:1, height:1, decoration: "flat") {
			state "default",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_grey.png", defaultState: true
			state "cool",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
			state "auto",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
		}
		standardTile("heating", "device.thermostatMode", width:1, height:1, decoration: "flat") {
			state "default",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_grey.png", defaultState: true
			state "heat",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "auxHeatOnly",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "emergency",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "emergencyHeat",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "auto",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
		}
		standardTile("humidityLogo", "device.humidifierMode", width: 1, height: 1, decoration: "flat") {
			state "off",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_humidifier_only-grey.png"
			state "auto",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_humidifier_only.png"
			state "on",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_humidifier_only-solid.png"
		}
		standardTile("dehumidityLogo", "device.dehumidifierMode", width: 1, height: 1, decoration: "flat") {
			state "off",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_dehumidifier_only-grey.png"
			state "on",		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_dehumidifier_only-solid.png"
		}
		// Display certain internal settings to help users understand why setpoints look odd (+/- differentials)
		valueTile("heatDifferential", "device.heatDifferential", width: 1, height: 1, decoration: "flat") {
			state "default", defaultValue: true, label: 'Heat\nDiff\'l:\n-${currentValue}°'
		}
		valueTile("coolDifferential", "device.coolDifferential", width: 1, height: 1, decoration: "flat") {
			state "default", defaultValue: true, label: 'Cool\nDiff\'l:\n+${currentValue}°'
		}
		valueTile("heatCoolMinDelta", "device.heatCoolMinDelta", width: 1, height: 1, decoration: "flat") {
			state "default", defaultValue: true, label: 'HeatCool\nMinDelta:\n${currentValue}°'
		}
		main(['temperatureDisplay']) // Display current temperature in the things & room lists
		details([
			"temperatureDisplay",
			"equipmentState", "weatherIcon",  "refresh",
			"currentProgramIcon", "weatherTemperature", "motionState",
			'humidity', "holdStatus", 'humiditySetpointDisplay', // "fanMinOnTime",
			"tstatDate", "commandDivider", "tstatTime",
			"mode", "fanModeLabeled",  "resumeProgram",
			//'heating',"heatSliderControl", "heatingSetpoint", "humidityLogo", "dehumidityLogo",
			//'cooling',"coolSliderControl", "coolingSetpoint", "humiditySlider", "dehumiditySlider",
			//'circulating', 'circSliderControl', 'fanMinOnTime',
			'heating','cooling','circulating','humidityLogo','dehumidityLogo','heatDifferential',
			'heatSliderControl','coolSliderControl','circSliderControl','humiditySlider','dehumiditySlider','coolDifferential',
			'heatingSetpoint','coolingSetpoint','fanMinOnTime','humiditySetpoint','dehumiditySetpoint','heatCoolMinDelta',
			"fanModeCycler", "fanOffButton", "setModeOff",
			"setModeHeat", "setModeCool", "setModeAuto",
			"setHome", "setAway", "setSleep",
			"apiStatus", "lastPoll"
			])
	}

	preferences {
        input "myHoldType", "enum", title: "${getVersionLabel()}\n\nHold Type", description: 
			"When changing temperature or program, use Permanent, Temporary, Hourly, Parent setting or Thermostat setting hold type?\n\nDefault: Ecobee (Connect) setting",
            required: false, options:["Permanent", "Temporary", "Hourly", "Parent", "Thermostat"]
        input "myHoldHours", "enum", title: "Hourly Hold Time", description: "If Hourly hold, how many hours do you want to hold?\n\nDefault: Ecobee (Connect) setting", required: false, options:[2,4,6,8,12,16,24]
        input "smartAuto", "bool", title: "Smart Auto Temp Adjust", description: "This flag allows the temperature to be adjusted manually when the thermostat " +
                "is in Auto mode. An attempt to determine if the heat or cool setting should be changed is made automatically.", required: false
	}
}

// parse events into attributes
void parse(String description) {
	LOG( "parse() --> Parsing '${description}'" )
	// Not needed for cloud connected devices
}

void refresh(force=false) {
	// No longer require forcePoll on every refresh - just get whatever has changed
	LOG("refresh() - calling pollChildren ${force?'(forced)':''}, deviceId = ${getDeviceId()}",2,null,'info')
	parent.pollChildren(getDeviceId(), force) // tell parent to just poll me silently -- can't pass child/this for some reason
}
void doRefresh() {
	// Pressing refresh within 6 seconds of the prior refresh completing will force a complete poll of the Ecobee cloud - otherwise changes only
	refresh(state.lastDoRefresh?((now()-state.lastDoRefresh)<6000):false)
	sendEvent(name: 'doRefresh', value: 'refresh', isStateChange: true, displayed: false)
	runIn(2, 'resetRefreshButton', [overwrite: true])
	resetUISetpoints()
	state.lastDoRefresh = now()	// reset the timer after the UI has been updated
}
void resetRefreshButton() {
	sendEvent(name: 'doRefresh', value: 'refresh', isStateChange: true, displayed: false)
}
void forceRefresh() {
	refresh(true)
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
	getHubPlatform()
	LOG("${getVersionLabel()} updated",1,null,'trace')
	sendEvent(name: 'checkInterval', value: 3900, displayed: false, isStateChange: true)  // 65 minutes (we get forcePolled every 60 minutes
	resetUISetpoints()
	// testChildren()
	if (device.currentValue('reservations')) sendEvent(name: 'reservations', value: null, displayed: false)
	state.version = getVersionLabel()
	runIn(2, 'forceRefresh', [overwrite: true])
}

void poll() {
	LOG("Executing 'poll' using parent SmartApp", 2, null, 'info')
	parent.pollChildren(getDeviceId(),false) // tell parent to just poll me silently -- can't pass child/this for some reason
}

// Health Check will ping us based on the frequency we configure in Ecobee (Connect) (derived from poll & watchdog frequency)
void ping() {
	if (isST ) {
		LOG("Health Check ping - apiConnected: ${device.currentValue('apiConnected')}, ecobeeConnected: ${device.currentValue('ecobeeConnected')}, checkInterval: ${device.currentValue('checkInterval')} seconds",1,null,'warn')
	} else {
		LOG("Health Check ping - apiConnected: ${device.currentValue('apiConnected', true)}, ecobeeConnected: ${device.currentValue('ecobeeConnected', true)}, checkInterval: ${device.currentValue('checkInterval', true)} seconds",1,null,'warn')
	}
	parent.pollChildren(getDeviceId(),true)		// forcePoll just me
}

// def getManufacturerName() { log.trace "getManufactururerName()"; return null /*"Ecobee"*/ }
// def getModelName() { log.trace "getModelName()"; return null }

void generateEvent(Map results) {
	if (!state.version || (state.version != getVersionLabel())) updated()
	def startMS = now()
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("generateEvent(): parsing data ${results}", 4)
	//LOG("Debug level of parent: ${getParentSetting('debugLevel')}", 4, null, "debug")
	def linkText = device.displayName
	def tu = getTemperatureScale()
	def isMetric = (tu == 'C')
	boolean forceChange = false
	def ps, isIOS, isAndroid
	if (isST) {
		ps = device.currentValue('mobile') ?: getParentSetting('mobile')
		isIOS = ((ps == null) || (ps == 'iOS'))
		isAndroid = !isIOS
	}

	def updateTempRanges = false
	def precision = device.currentValue('decimalPrecision')
	if (precision == null) precision = isMetric ? 1 : 0
	Integer objectsUpdated = 0
	def supportedThermostatModes = []
	if (state.supportedThermostatModes == null) {
		// Initialize for those that are updating the DTH with this version
		supportedThermostatModes = ['off']
		if (isST) {
			if (device.currentValue('heatMode') == 'true') supportedThermostatModes << 'heat'
			if (device.currentValue('coolMode') == 'true') supportedThermostatModes << 'cool'
			if (device.currentValue('autoMode') == 'true') supportedThermostatModes << 'auto'
			if (device.currentValue('auxHeatMode') == 'true') supportedThermostatModes += ['auxHeatOnly', 'emergency heat']
		} else {
			if (device.currentValue('heatMode', true) == 'true') supportedThermostatModes << 'heat'
			if (device.currentValue('coolMode', true) == 'true') supportedThermostatModes << 'cool'
			if (device.currentValue('autoMode', true) == 'true') supportedThermostatModes << 'auto'
			if (device.currentValue('auxHeatMode', true) == 'true') supportedThermostatModes += ['auxHeatOnly', 'emergency heat']
		}
		sendEvent(name: "supportedThermostatModes", value: supportedThermostatModes, displayed: false, isStateChange: true)
		sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false, isStateChange: true)
		state.supportedThermostatModes = supportedThermostatModes.sort(false)
	} else {
		supportedThermostatModes = state.supportedThermostatModes.sort(false)
	}
	if (device.currentValue('autoHeatCoolFeatureEnabled') == null) sendEvent(name: 'autoHeatCoolFeatureEnabled', value: device.currentValue('autoMode'), isStateChange: true, displayed: false)
	def vType = device.currentValue('ventilatorType')
	if ((vType != null) && (vType != 'none')) {
		if ((state.supportedVentModes == null) || (state.supportedVentModes.size() == 1)){
			state.supportedVentModes = ventModes()
			sendEvent(name: 'supportedVentModes', value: ventModes(), displayed: false, isStateChange: true)
		}
	} else {
		if (state.supportedVentModes == null) {
			state.supportedVentModes = ['off']
			sendEvent(name: 'supportedVentModes', value: ['off'], displayed: false, isStateChange: true)
		}
	}

	def tMode = (isST ? device.currentValue('thermostatMode') : device.currentValue('thermostatMode', true))
	if(results) {
		results.each { name, value ->
			objectsUpdated++
			if (debugLevelFour) LOG("generateEvent() - In each loop: object #${objectsUpdated} name: ${name} value: ${value}", 4)

			String tempDisplay = ""
			def eventFront = [name: name, linkText: linkText, handlerName: name]
			def event = [:]
			String sendValue = value.toString()
			boolean isChange = isStateChange(device, name, sendValue)
			// def tMode = (name == 'thermostatMode') ? sendValue : (isST ? device.currentValue('thermostatMode') : device.currentValue('thermostatMode', true))

			switch (name) {
				case 'forced':
					forceChange = (sendValue == 'true')
					break;

				case 'heatingSetpoint':
					if (isChange || forceChange) {
						//log.debug "heatingSetpoint Change (${isChange}/${forceChange}): ${sendValue}"
						//LOG("heatingSetpoint: ${sendValue}",3,null,'info')
						// We have to send for backwards compatibility
						sendEvent(name: 'heatingSetpointDisplay', value: sendValue, unit: tu, /* isStateChange: isChange,*/ displayed: false) // for the slider
						objectsUpdated++
						if (tMode == 'heat') {
							sendEvent(name: 'thermostatSetpoint', value: sendValue, unit: tu, descriptionText: "Thermostat setpoint is ${sendValue}°${tu}", displayed: false)
							objectsUpdated++
						} else if (tMode == 'auto') {
                        	def ncCsp = isST ? device.currentValue('coolingSetpoint') : device.currentValue('coolingSetpoint', true)
							def avg = roundIt(((value.toBigDecimal() + ncCsp.toBigDecimal()) / 2.0), precision.toInteger())
							sendEvent(name: 'thermostatSetpoint', value: avg.toString(), unit: tu, descriptionText: "Thermostat setpoint average is ${avg}°${tu}", displayed: true)
							objectsUpdated++
						}
						def heatDiff = isST ? device.currentValue('heatDifferential') : device.currentValue('heatDifferential', true)
						if (heatDiff == null) heatDiff = 0.0
						String heatAt = roundIt((value.toBigDecimal() - heatDiff.toBigDecimal()), precision.toInteger()).toString()
						sendEvent(name: 'heatAtSetpoint', value: heatAt, unit: tu, displayed: false)
						objectsUpdated++
                        String ncTos = isST ? device.currentValue('thermostatOperatingState') : device.currentValue('thermostatOperatingState', true)
						String tileValue = (!ncTos?.startsWith('he'))? (isIOS?heatAt:sendValue) : sendValue
						sendEvent(name: 'heatingSetpointTile', value: tileValue, unit: tu, displayed: false)
						objectsUpdated++
						//} else {
						//	// Androids screw up the heatAt/heatTo display, so just send null and it should display "Idle" or "Cooling"
						//	  String tileValue = (!device.currentValue('thermostatOperatingState').startsWith('he')) ? null : sendValue
						//	  sendEvent(name: 'heatingsSetpointTile', value: null, displayed: false)
						//}
						event = eventFront + [value: sendValue, unit: tu, descriptionText: "Heating setpoint is ${sendValue}°${tu}"]
					}
					break;

				case 'heatingSetpointDisplay':
					event = eventFront + [value: sendValue, unit: tu, /* descriptionText: "Heating setpoint is ${sendValue}°", */ displayed: false]
					break;

				case 'coolingSetpoint':
					if (isChange || forceChange) {
						//log.debug "coolingSetpoint Change (${isChange}/${forceChange}): ${sendValue}"
						//LOG("coolingSetpoint: ${sendValue}",3,null,'info')
						sendEvent(name: 'coolingSetpointDisplay', value: sendValue, unit: tu,  displayed: false) // for the slider
						objectsUpdated++
						if (tMode == 'cool') {
							sendEvent(name: 'thermostatSetpoint', value: sendValue, unit: tu, descriptionText: "Thermostat setpoint is ${sendValue}°${tu}", displayed: false)
							objectsUpdated++
						} else if (tMode == 'auto') {
                        	def ncHsp = isST ? device.currentValue('heatingSetpoint') : device.currentValue('heatingSetpoint', true)
							def avg = roundIt(((value.toBigDecimal() + ncHsp.toBigDecimal()).toBigDecimal() / 2.0).toBigDecimal(), precision.toInteger())
							sendEvent(name: 'thermostatSetpoint', value: avg.toString(), unit: tu, descriptionText: "Thermostat setpoint average is ${avg}°${tu}", displayed: true)
							objectsUpdated++
						}
						def coolDiff = isST ? device.currentValue('coolDifferential') : device.currentValue('coolDifferential', true)
						if (coolDiff == null) coolDiff = 0.0
						String coolAt = roundIt((value.toBigDecimal() + coolDiff.toBigDecimal()), precision.toInteger()).toString()
						sendEvent(name: 'coolAtSetpoint', value: coolAt, unit: tu,displayed: false)
						objectsUpdated++
                        String ncTos = isST ? device.currentValue('thermostatOperatingState') : device.currentValue('thermostatOperatingState', true)
						String tileValue = (!ncTos?.startsWith('co')) ? (isIOS?coolAt:sendValue) : sendValue
						sendEvent(name: 'coolingSetpointTile', value: tileValue, unit: tu, displayed: false)
						objectsUpdated++
						event = eventFront + [value: sendValue, unit: tu, descriptionText: "Cooling setpoint is ${sendValue}°${tu}", displayed: true]
					}
					break;

				case 'coolingSetpointDisplay':
					event = eventFront + [value: sendValue, unit: tu, /* descriptionText: "Cooling setpoint is ${sendValue}°", */ displayed: false]
					break;

				case 'thermostatSetpoint':
					if (isChange || forceChange) {
						LOG("thermostatSetpoint: ${sendValue}",2,null,'info')
						def displayValue = isMetric ? sendValue : roundIt(value, 0) // Truncate the decimal point
						event = eventFront + [value: displayValue.toString(), unit: tu, descriptionText: "Thermostat setpoint is ${sendValue}°${tu}", displayed: true]
					}
					break;

				case 'weatherTemperature':
					if (isChange) event = eventFront + [value: sendValue, unit: tu, descriptionText: getTemperatureDescriptionText(name, sendValue, linkText), isStateChange: true, displayed: true]
					break;

				case 'weatherHumidity':
					if (isChange) event = eventFront + [value: sendValue, unit: '%', descriptionText: "Outdoor humidity is ${sendValue}%", isStateChange: true, displayed: true]
					break;

				case 'weatherDewpoint':
					//log.debug "weatherDewpoint ${sendValue}"
					if (isChange) event = eventFront + [value: sendValue, unit: tu, descriptionText: getTemperatureDescriptionText(name, sendValue, linkText), isStateChange: true, displayed: true]
					break;

			   case 'weatherPressure':
					def pScale = isMetric ? 'mbar' : 'inHg'
					if (isChange) event = eventFront + [value: sendValue, unit: pScale, descriptionText: "Barometric Pressure is ${sendValue}${pScale}", isStateChange: true, displayed: true]
					break;

				case 'temperature':
					String ncTos = isST ? device.currentValue('thermostatOperatingState') : device.currentValue('thermostatOperatingState', true)
					if (ncTos == 'offline') {
						tempDisplay = '451°'		// As in Fahrenheit 451
					} else {
						if (sendValue) {							// don't send null temperature values
							def dValue = sendValue?.toBigDecimal()
							// Generate the display value that will preserve decimal positions ending in 0
							if (precision == 0) {
								tempDisplay = roundIt(dValue, 0).toString() + '°'
								sendValue = roundIt(dValue, 0).toInteger()
							} else {
								tempDisplay = String.format( "%.${precision.toInteger()}f", roundIt(dValue, precision.toInteger())) + '°'
							}
							//redisplay = (device.currentValue('temperatureDisplay') == '451°'
							if (isChange || forceChange) event = eventFront + [value: sendValue, unit: tu, descriptionText: getTemperatureDescriptionText(name, sendValue, linkText), displayed: true]
						}
					}
					break;

				case 'thermostatOperatingState':
					// A little tricky, but this is how we display Smart Recovery within the stock Thermostat multiAttributeTile
					// thermostatOperatingStateDisplay has the same values as thermostatOperatingState, plus "heating (smart recovery)"
					// and "cooling (smart recovery)". We separately update the standard thermostatOperatingState attribute.
                    //log.debug "thermostatOperatingState: ${sendValue}"
					boolean displayDesc
					String descText
					String realValue
					if (sendValue.contains('(')) {
						displayDesc = true				// only show this update ThermOpStateDisplay if we are in Smart Recovery
						if (sendValue.contains('mart')) descText = 'in Smart Recovery'	// equipmentOperatingState will show what is actually running
						else if (sendValue.contains('ver')) descText = 'Overcooling to dehumidify'
						realValue = sendValue.take(7)	// this gets us to back to just heating/cooling
					} else {
						displayDesc = false				// hide this message - is redundant with EquipOpState
						descText = sendValue.capitalize()
						realValue = sendValue
					}
					if (isIOS) {
                    	if ((sendValue == 'idle') && (tMode== 'off')) sendValue = 'off'
						//if ((sendValue == 'idle') && (device.currentValue('thermostatMode') == 'off')) sendValue = 'off'
						//isChange = isStateChange(device, 'thermostatOperatingStateDisplay', sendValue)
					}
					if (forceChange || isStateChange(device, 'thermostatOperatingStateDisplay', sendValue)) {
                    	//log.debug "updating thermostatOperatingStateDisplay with ${sendValue}"
						sendEvent(name: "thermostatOperatingStateDisplay", value: sendValue, descriptionText: "Thermostat Operating State is ${descText}", linkText: linkText,
										handlerName: "${name}Display", isStateChange: true, displayed: displayDesc)
						objectsUpdated++
					}
					if (sendValue == 'offline') {
						sendEvent(name: "temperatureDisplay", value: "451°", isStateChange: true, displayed: false, descriptionText:
										"Fahrenheit 451")
						objectsUpdated++
					}

					// now update thermostatOperatingState - is limited by API to idle, fan only, heating, cooling, pending heat, pending cool, ventilator only
					if (forceChange || isStateChange(device, name, realValue)) {
						// First, check if we need to change the Heating at/Heating to temperature values
						if (realValue.startsWith('heat')) {
							// heatingSetpoint should display actual setpoint for "Heating to..."
							String heatSetp = isST ? device.currentValue('heatingSetpoint').toString() : device.currentValue('heatingSetpoint', true).toString()
							sendEvent(name: 'heatingSetpointTile', value: heatSetp, unit: tu, descriptionText: "Heating to ${heatSetp}°${tu}", displayed: false) // let sendEvent figure out if this is a change
						} else if (isIOS) {
							String heatAtSetp = isST ? device.currentValue('heatAtSetpoint').toString() : device.currentValue('heatAtSetpoint', true).toString()
							sendEvent(name: 'heatingSetpointTile', value: heatAtSetp, unit: tu, descriptionText: "Heating at ${heatSetp}°${tu}", displayed: false)
						} else { // isAndroid
							String heatSetp = isST ? device.currentValue('heatingSetpoint').toString() : device.currentValue('heatingSetpoint', true).toString()
							sendEvent(name: 'heatingSetpointTile', value: heatSetp, unit: tu, descriptionText: "Heating at ${heatSetp}°${tu}", displayed: false)
						}
						if (realValue.startsWith('cool')) {
							// coolingSetpoint should display actual setpoint for "Cooling to..."
							String coolSetp = isST ? device.currentValue('coolingSetpoint').toString() : device.currentValue('coolingSetpoint', true).toString()
							sendEvent(name: 'coolingSetpointTile', value: coolSetp, unit: tu, descriptionText: "Cooling to ${coolSetp}°${tu}", displayed: false)
						} else if (isIOS) {
							String coolSetp = isST ? device.currentValue('coolAtSetpoint').toString() : device.currentValue('coolAtSetpoint', true).toString()
							sendEvent(name: 'coolingSetpointTile', value: coolSetp, unit: tu, descriptionText: "Cooling at ${coolSetp}°${tu}", displayed: false)
						} else { // isAndroid
							String coolSetp = isST ? device.currentValue('coolingSetpoint').toString() : device.currentValue('coolingSetpoint', true).toString()
							sendEvent(name: 'coolingSetpointTile', value: coolSetp, unit: tu, descriptionText: "Cooling at ${coolSetp}°${tu}", displayed: false)
						}
						event = eventFront + [value: realValue, descriptionText: "Thermostat Operating State is ${realValue}", displayed: false]

						// Keep track of whether we were last heating or cooling
						def lastOpState = isST ? device.currentValue('thermostatOperatingState'): device.currentValue('thermostatOperatingState', true)
						if (lastOpState && (lastOpState.contains('ea') || lastOpState.contains('oo'))) sendEvent(name: 'lastOpState', value: lastOpState, displayed: false)
					}
					break;

				case 'equipmentOperatingState':
					if ((sendValue != 'off') && (tMode == 'off')) {
						sendValue = 'off'
						isChange = isStateChange(device, name, sendValue)
					}
					if (isChange || forceChange) {
						if (sendValue == 'off') {
							// force thermostat to appear idle when it is off
							def lastOpState = isST ? device.currentValue('thermostatOperatingState') : device.currentValue('thermostatOperatingState', true)
							if (lastOpState.contains('ea') || lastOpState.contains('oo')) sendEvent(name: 'lastOpState', value: lastOpState, displayed: false)
							sendEvent(name: 'thermostatOperatingState', value: 'idle', descriptionText: 'Thermostat Operating State is idle', displayed: true)
							objectsUpdated++
							if (isIOS) {
								// Android doesn't handle 'off' - update only the display
								sendEvent(name: 'thermostatOperatingStateDisplay', value: 'off', descriptionText: 'Thermostat Operating State is off', displayed: false)
								objectsUpdated++
							}
						}
						String descText = sendValue.endsWith('deh') ? sendValue.replace('deh', '& dehumidifying') : (sendValue.endsWith('hum') ? sendValue.replace('hum', '& humidifying') : sendValue)
						if (!descText.startsWith('heat pump')) {
							descText = descText.replace('heat ','heating ')
							descText = descText.replace('cool ','cooling ')
						}
						descText = descText.replace('1', '(stage 1)').replace('2', '(stage 2)').replace('3', '(stage 3)').replace('fier', 'fying')
						event = eventFront + [value: sendValue, descriptionText: "Equipment is ${descText}", isStateChange: isChange, displayed: true]
					}
					break;

				case 'equipmentStatus':
					if (isChange || forceChange) {
						String descText = (sendValue == 'idle') ? 'Equipment is idle' : ((sendValue == 'offline') ? 'Equipment is offline' : "Equipment is running " + sendValue)
						event = eventFront +  [value: sendValue, descriptionText: descText, displayed: false]
					}
					break;

				case 'lastPoll':
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "Poll: " + sendValue, isStateChange: true, displayed: debugLevel(4)]
					break;

				case 'humidity':
					if (isChange || forceChange) {
						def humSetpoint = isST ? device.currentValue('humiditySetpointDisplay') : device.currentValue('humiditySetpointDisplay', true)
						// if (humSetpoint == null) humSetpoint = 0
						String setpointText = ((humSetpoint == null) || (humSetpoint == 0)) ? '' : " (setpoint: ${humSetpoint}%)"
						event = eventFront + [value: sendValue, unit: '%', descriptionText: "Humidity is ${sendValue}%${setpointText}", displayed: true]
					}
					break;

				case 'humiditySetpoint':
					if (isChange) {
						//log.debug "humiditySetpoint: ${sendValue}"
						event = eventFront + [value: sendValue, unit: '%', descriptionText: "Humidifier setpoint is ${sendValue}%", isStateChange: true, displayed: true]
					}
					break;

				case 'dehumiditySetpoint':
					if (isChange) {
						sendEvent(name: 'dehumidifierLevel', value: sendValue, unit: '%', descriptionText: "Dehumidifier level is ${sendValue}%", isStateChange: true, displayed: false)
						sendEvent(name: 'dehumidityLevel', value: sendValue, unit: '%', descriptionText: "Dehumidity level is ${sendValue}%", isStateChange: true, displayed: false)
						event = eventFront + [value: sendValue, unit: '%', descriptionText: "Dehumidifier setpoint is ${sendValue}%", isStateChange: true, displayed: true]
					}
					break;

				case 'humiditySetpointDisplay':
					// LOG("humiditySetpointDisplay: ${sendValue}",3,null,'info')
					if ((isChange || forceChange) && (sendValue != '0')) {
						def dispValue = sendValue.replaceAll('-', "-\n")
						event = eventFront + [value: dispValue, unit: '%', descriptionText: "Humidity setpoint display is ${sendValue}%", isStateChange: true, displayed: true]
						def hum = isST ?  device.currentValue('humidity') : device.currentValue('humidity', true) 
						if (hum == null) hum = 0
						sendEvent( name: 'humidity', value: hum.toString(), unit: '%', linkText: linkText, handlerName: 'humidity', descriptionText: "Humidity is ${hum}% (setpoint: ${sendValue}%)", displayed: true )
						objectsUpdated++
					}
					break;

				case 'currentProgramName':
					// LOG("currentProgramName: ${sendValue}", 3, null, 'info')
					// always update the button states, even if the value didn't change
					String progText = ''
					String ncCpn = isST ? device.currentValue('currentProgramName') : device.currentValue('currentProgramName', true)
					if (sendValue == 'Vacation') {
						if (ncCpn == 'Offline') disableVacationButtons() // not offline any more
						progText = 'Climate is Vacation'
						sendEvent(name: 'resumeProgram', value: 'cancel', displayed: false, isStateChange: true)	// change the button to Cancel Vacation
					} else if (sendValue == 'Offline') {
						progText = 'Thermostat is Offline'
						if (ncCpn != 'Offline') disableAllButtons() // just went offline
					} else {
						progText = 'Climate is '+sendValue.trim().replaceAll(":","")
						String buttonValue = (sendValue.startsWith('Hold') || sendValue.startsWith('Auto ')) ? 'resume' : 'resume dis'
						sendEvent(name: 'resumeProgram', value: buttonValue, displayed: false, isStateChange: true)	// change the button to Resume Program
						def previousProgramName = ncCpn
						if (previousProgramName) {
							// update the button states
							if (previousProgramName == 'Offline') enableAllButtons()			// not offline any more
							if (previousProgramName.contains('acation')) updateModeButtons()	// turn the mode buttons back on if we just exited a Vacation Hold
						}
					}
					if (isChange || forceChange) {
						event = eventFront + [value: sendValue, descriptionText: progText, displayed: true]
						if (sendValue.endsWith('Temp')) enableAllProgramButtons()
					}
					sendEvent(name: 'schedule', value: sendValue + (isST?device.currentValue('schedText'):device.currentValue('schedText',true)), displayed: false)			// For Hubitat, we show	 schedule: "Hold: Home until today at 6:30pm"
					break;

				case 'currentProgram':
					// LOG("currentProgram: ${sendValue}", 3, null, 'info')
					// always update the button states, even if the value didn't change
					if (isST) {
						switch (sendValue) {
							case 'Home':
								disableHomeButton()
								break;
							case 'Away':
								disableAwayButton()
								break;
							case 'Sleep':
								disableSleepButton()
								break;
							default:
								if ((device.currentValue('currentProgramName') != 'Vacation') && (device.currentValue('thermostatHold') != 'vacation')) {
									enableAllProgramButtons()
								} else {
									disableVacationButtons()
								}
								break;
						}
					}
					if (isChange) {
						//log.debug "currentProgram Change (${isChange}/${forceChange}): ${sendValue}"
						event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
						// sendEvent(name: 'schedule', value: sendValue, isStateChange: true, displayed: false, descriptionText: "Current Schedule is ${sendValue}") // compatibility with new Capability definition
					}
					break;

				case 'apiConnected':
					// only display in the devices' log if we are in debug level 4 or 5
					if (isChange || forceChange) event = eventFront + [value: sendValue, descriptionText: "API Connection is ${value}", isStateChange: true, displayed: debugLevelFour]
					break;

				case 'weatherSymbol':
					Integer symbolNum = value as Integer
					String timeOfDay = isST ? device.currentValue('timeOfDay') : device.currentValue('timeOfDay', true)
					if ((timeOfDay == 'night') && (symbolNum < 100)) {
						symbolNum = symbolNum + 100
					} else if ((timeOfDay == 'day') && (symbolNum >= 100)) {
						symbolNum = symbolNum - 100
					}
					isChange = isStateChange(device, 'weatherSymbol', symbolNum.toString())
					if (isChange) event = [name: 'weatherSymbol', linkText: linkText, handlerName: 'weatherSymbol', value: "${symbolNum}", descriptionText: "Weather Symbol is ${symbolNum}", isStateChange: true, displayed: true]
					break;
				
				case 'timeOfDay':
					// Check to see if it is night time, if so change to a night symbol
					if (isChange) {
						def weatherSymbol = isST ? device.currentValue('weatherSymbol') : device.currentValue('weatherSymbol', true)
						Integer symbolNum = weatherSymbol as Integer
						if ((sendVal == 'night') && (symbolNum < 100)) {
							symbolNum = symbolNum + 100
						} else if ((sendVal == 'day') && (symbolNum >= 100)) {
							symbolNum = symbolNum - 100
						}
						isChange = isStateChange(device, 'weatherSymbol', symbolNum.toString())
						if (isChange) sendEvent(name: 'weatherSymbol', linkText: linkText, handlerName: 'weatherSymbol', value: "${symbolNum}", descriptionText: "Weather Symbol is ${symbolNum}", isStateChange: true, displayed: true)
						event = eventFront + [value: sendValue, descriptionText: '', isStateChange: true, displayed: false]
					}
					break;

				case 'thermostatHold':
					if (isChange) {
						if (sendValue == 'vacation') {
							disableVacationButtons()
						} else if (((sendValue == 'null') || (sendValue == '')) && (device.currentValue('thermostatHold') == 'vacation')) {
							enableVacationButtons()
						}
						String ncCp = isST ? device.currentValue('currentProgram') : device.currentValue('currentProgram', true)
						String ncSp = isST ? device.currentValue('scheduledProgram') : device.currentValue('scheduledProgram', true)
						String descText = ((sendValue == 'null') || (sendValue == '')) ? 'Hold finished' : (sendValue == 'hold') ? "Hold ${ncCp} (${ncSp})" : "Hold for ${sendValue}"
						event = eventFront + [value: sendValue, descriptionText: descText, isStateChange: true, displayed: true]
						// log.debug "thermostatHold event: ${event}"
					}
					break;

				case 'holdStatus':
					if (isChange || forceChange) {
                    	String sendText = sendValue != 'null' ? sendValue : ''
                        if (isStateChange(device, name, sendText)) event = eventFront + [value: sendText, descriptionText: sendText, displayed: (sendText != '')]
                    }
					break;

				case 'motion':
					if (isChange) {
						def cMotion = isST ? device.currentValue('motion') : device.currentValue('motion', true)
						// Once "in/active", prevent inadvertent "not supported" -
						if ((cMotion == 'null') || (cMotion == null) || !sendValue.startsWith('not') || !cMotion.contains('act')) {
							event = eventFront + [value: sendValue, descriptionText: "Motion is "+sendValue, isStateChange: true, displayed: true]
						}
					}
					break;

				case 'fanMinOnTime':
					if (isChange || forceChange) {
						String circulateText = (value == 0) ? 'Fan Circulation is disabled' : "Fan Circulation is ${sendValue} minutes per hour"
						event = eventFront + [value: sendValue, descriptionText: circulateText, /* isStateChange: isChange, */ displayed: true]
						String fanMode = isST ? device.currentValue('thermostatFanMode') : device.currentValue('thermostatFanMode', true)
						if (fanMode != 'on') {
							// String ncTm = isST ? device.currentValue('thermostatMode') : device.currentValue('thermostatMode', true)
							if (tMode == 'off') {
								fanMode = (value == 0) ? 'off' : 'circulate'
							} else {
								fanMode = (value == 0) ? 'auto' : 'circulate'
							}
							if (event != [:]) { sendEvent(event); event = [:] }		// update fanMinOnTime attribute of the device
							//event = [:]				// so the thermostatFanMode logic can adjust the buttons properly
							sendEvent(name: 'thermostatFanMode', value: fanMode, displayed: false /*, isStateChange: true */)
						}
					}
					break;

				case 'thermostatMode':
                    //log.debug "thermostatMode: ${sendValue}, isChange: ${isChange}, tMode: ${tMode}"
                    tMode = sendValue 
					if (isChange || forceChange) {
                    	//log.debug "sendEvent thermostatMode"
						event = eventFront + [value: sendValue, descriptionText: "Thermostat Mode is ${sendValue}", data:[supportedThermostatModes: supportedThermostatModes], displayed: true]
					}
					switch (sendValue) {
						case 'off':
							if (isChange) {
								sendEvent(name: 'thermostatOperatingState', value: 'idle', displayed: true, descriptionText: 'Thermostat is idle')
								if (isIOS) {
									// Android doesn't handle 'off' for operatingState
									sendEvent(name: 'thermostatOperatingStateDisplay', value: 'off', displayed: false, isStateChange: true)
								}
							}
							def currentFanMode = isST ? device.currentValue('thermostatFanMode') : device.currentValue('thermostatFanMode', true)
							if ((currentFanMode != 'off') && (currentFanMode != 'on')) { // auto or circulate
								def ncFmot = isST ? device.currentValue('fanMinOnTime') : device.currentValue('fanMinOnTime', true)
								if (ncFmot == 0) {
									sendEvent(name: 'thermostatFanModeDisplay', value: 'off dis', displayed: false, isStateChange: true)
									disableFanOffButton()
								} else {
									sendEvent(name: 'thermostatFanModeDisplay', value: 'circulate dis', displayed: false, isStateChange: true)
									enableFanOffButton()
								}
							} else {
								disableFanOffButton()
							}
							disableModeOffButton()
							objectsUpdated += 2
							break;

						case 'auto':
							String avg
							if (isST) {
								avg = roundIt(((device.currentValue('heatingSetpoint')?.toBigDecimal() + device.currentValue('coolingSetpoint')?.toBigDecimal()) / 2.0), precision.toInteger()).toString()
							} else {
								avg = roundIt(((device.currentValue('heatingSetpoint', true)?.toBigDecimal() + device.currentValue('coolingSetpoint', true)?.toBigDecimal()) / 2.0), precision.toInteger()).toString()
							}
							sendEvent(name: 'thermostatSetpoint', value: avg, unit: tu, descriptionText: "Thermostat setpoint is ${avg}°${tu}", displayed: true)
							disableModeAutoButton()
							disableFanOffButton()
							objectsUpdated++
							break;

						case 'heat':
							String statSetpoint = isST ? device.currentValue('heatingSetpoint').toString() : device.currentValue('heatingSetpoint', true).toString()
							sendEvent(name: 'thermostatSetpoint', value: statSetpoint, unit: tu, descriptionText: "Thermostat setpoint is ${statSetpoint}°${tu}", displayed: true)
							disableModeHeatButton()
							disableFanOffButton()
							objectsUpdated++
							break;

						case 'cool':
							String statSetpoint = isST ? device.currentValue('coolingSetpoint').toString() : device.currentValue('coolingSetpoint', true).toString()
							sendEvent(name: 'thermostatSetpoint', value: statSetpoint, unit: tu, descriptionText: "Thermostat setpoint is ${statSetpoint}°${tu}", displayed: true)
							disableModeCoolButton()
							disableFanOffButton()
							objectsUpdated++
							break;

						case 'auxHeatOnly':
						case 'emergencyHeat':
                        	String statSetpoint = isST ? device.currentValue('heatingSetpoint').toString() : device.currentValue('heatingSetpoint', true).toString()
							sendEvent(name: 'thermostatSetpoint', value: statSetpoint, unit: tu, descriptionText: "Thermostat setpoint is ${statSetpoint}°${tu}", displayed: true)
							enableAllModeButtons()
							disableFanOffButton()
							objectsUpdated++
							break;
					}
					break;

				case 'thermostatFanMode':
					if (isChange || forceChange){
						event = eventFront + [value: sendValue, descriptionText: "Fan Mode is ${sendValue}", data:[supportedThermostatFanModes: fanModes()], displayed: true]
						sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false)
					}
					String ncTh = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true)
					if (ncTh != 'vacation') {
						switch(sendValue) {
							case 'off':
								// Assume (for now) that thermostatMode is also 'off' - this should be enforced by setThermostatFanMode() (who is also only one	 who will send us 'off')
								sendEvent(name: 'thermostatFanModeDisplay', value: "off dis", isStateChange: true, displayed: false) // have to force it to update for some reason
								disableFanOffButton()
								break;

							case 'auto':
							case 'circulate':
								// String ncTm = isST ? device.currentValue('thermostatMode') : device.currentValue('thermostatMode', true)
								if (tMode == 'off') {
									def ncFmot = isST ? device.currentValue('fanMinOnTime') : device.currentValue('fanMinOnTime', true)
									if (ncFmot != 0) {
										sendValue = 'circulate dis'
										enableFanOffButton()
									} else {
										sendValue = 'off dis'		// display 'off' when Fan Mode == 'auto' && Thermostat Mode == 'off'
										disableFanOffButton()
									}
								} else {
									disableFanOffButton()		// can't turn off the fan if the Thermostat isn't off
									def ncFmot = isST ? device.currentValue('fanMinOnTime') : device.currentValue('fanMinOnTime', true)
									if (ncFmot != 0) { sendValue = 'circulate' } else { sendValue = 'auto' }
								}
								sendEvent(name: 'thermostatFanModeDisplay', value: sendValue, isStateChange: true, displayed: false)	// have to force it to update for some reason
								break;

							case 'on':
								sendEvent(name: 'thermostatFanModeDisplay', value: 'on', isStateChange: true, displayed: false)			// have to force it to update for some reason
								break;
						}
					} else {
						sendEvent(name: 'thermostatFanModeDisplay', value: sendValue+" dis", isStateChange: true, displayed: false)	// have to force it to update for some reason
						disableFanOffButton()
					}
					break;

				case 'debugEventFromParent':
					event = eventFront + [value: sendValue, descriptionText: "-> ${sendValue}", isStateChange: true, displayed: true]
					if (sendValue.contains('rror')) {log.error "${parent.name}: " + sendValue} else {log.debug "${parent.name}: " + sendValue}
					break;

				case 'thermostatTime':
					// 2017-03-22 15:06:14
					if (isChange) {
						event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
						String tstatDate = new Date().parse('yyyy-MM-dd',sendValue).format('M-d\nyyyy')
						String tstatTime = new Date().parse('HH:mm',sendValue.drop(11)).format('h:mm\na').toLowerCase()
						if (isStateChange(device, 'tstatDate', tstatDate)) {
							sendEvent(name: 'tstatDate', value: tstatDate, isStateChange: true, displayed: false)
							objectsUpdated++
						}
                        if (isStateChange(device, 'tstatTime', tstatTime)) {
							sendEvent(name: 'tstatTime', value: tstatTime, isStateChange: true, displayed: false)
                        }
					}
					break;

				// Update the new (Optional) SetpointMin/Max attributes
				case 'heatRangeLow':
					def ncHspmn = isST ? device.currentValue('heatingSetpointMin') : device.currentValue('heatingSetpointMin', true)
					if (isChange || forceChange || (ncHspmn != value)) {
						sendEvent(name: 'heatingSetpointMin', value: value, unit: tu, displayed: false)
						objectsUpdated++
						if ((tMode == 'heat') || (tMode == 'auto')) {
							sendEvent(name: 'thermostatSetpointMin', value: value, unit: tu, displayed: false)
							def ncTspmx = isST ? device.currentValue('thermostatSetpointMax') : device.currentValue('thermostatSetpointMax', true)
							def range = [value, ncTspmx]
							sendEvent(name: 'thermostatSetpointRange', value: range, unit: tu, displayed: false)
							def ncHspmx = isST ? device.currentValue('heatingSetpointMax') : device.currentValue('heatingSetpointMax', true)
							range = [value, ncHspmx]
							sendEvent(name: 'heatingSetpointRange', value: range, unit: tu, displayed: false)
							objectsUpdated += 3
						}
						event = eventFront + [value: value, unit: tu, displayed: false]
					}
					break;

				case 'heatRangeHigh':
					def ncHspmx = isST ? device.currentValue('heatingSetpointMax') : device.currentValue('heatingSetpointMax', true)
					if (isChange || forceChange || (ncHspmx != value)) {
						sendEvent(name: 'heatingSetpointMax', value: value, unit: tu, displayed: false)
						objectsUpdated++
						if (tMode == 'heat') {
							sendEvent(name: 'thermostatSetpointMax', value: value, unit: tu, displayed: false)
							def ncTspmn = isST ? device.currentValue('thermostatSetpointMin') : device.currentValue('thermostatSetpointMin', true)
							def range = [ncTspmn, value]
							sendEvent(name: 'thermostatSetpointRange', value: range, unit: tu, displayed: false)
							def ncHspmn = isST ? device.currentValue('heatingSetpointMin') : device.currentValue('heatingSetpointMin', true)
							range = [ncHspmn, value]
							sendEvent(name: 'heatingSetpointRange', value: range, unit: tu, displayed: false)
							objectsUpdated += 3
						}
						event = eventFront + [value: value, unit: tu, displayed: false]
					}
					break;

				case 'coolRangeLow':
					def ncCspmn = isST ? device.currentValue('coolingSetpointMin') : device.currentValue('coolingSetpointMin', true)
					if (isChange || forceChange || (ncCspmn!= value)) {
						sendEvent(name: 'coolingSetpointMin', value: value, unit: tu, displayed: false)
						objectsUpdated++
						if (tMode == 'cool') {
							sendEvent(name: 'thermostatSetpointMin', value: value, unit: tu, displayed: false)
							def ncTspmx = isST ? device.currentValue('thermostatSetpointMax') : device.currentValue('thermostatSetpointMax', true)
							def range = [value, ncTspmx]
							sendEvent(name: 'thermostatSetpointRange', value: range, unit: tu, displayed: false)
							def ncCspmx = isST ? device.currentValue('coolingSetpointMax') : device.currentValue('coolingSetpointMax', true)
							range = [value, ncCspmx]
							sendEvent(name: 'coolingSetpointRange', value: range, unit: tu, displayed: false)
							objectsUpdated += 3
						}
						event = eventFront + [value: value, unit: tu, displayed: false]
					}
					break;

				case 'coolRangeHigh':
					def ncCspmx = isST ? device.currentValue('coolingSetpointMax') : device.currentValue('coolingSetpointMax', true)
					if (isChange || forceChange || (ncCspmx != value)) {
						sendEvent(name: 'coolingSetpointMax', value: value, unit: tu, displayed: false)
						objectsUpdated++
						if ((tMode == 'cool') || (tMode == 'auto')) {
							sendEvent(name: 'thermostatSetpointMax', value: value, unit: tu, displayed: false)
							def ncTspmn = isST ? device.currentValue('thermostatSetpointMin') : device.currentValue('thermostatSetpointMin', true)
							def range = [ncTspmn, value]
							sendEvent(name: 'thermostatSetpointRange', value: range, unit: tu, displayed: false)
							def ncCspmn = isST ? device.currentValue('coolingSetpointMin') : device.currentValue('coolingSetpointMin', true)
							range = [ncCspmn, value]
							sendEvent(name: 'coolingSetpointRange', value: range, unit: tu, displayed: false)
							objectsUpdated += 3
						}
						event = eventFront + [value: value, unit: tu, displayed: false]
					}
					break;

				case 'heatRange':
					if (isChange || forceChange) {
						def newValue = sendValue.toString().replaceAll("[\\(\\)]","")
						def idx = newValue.indexOf('..')
						def low = newValue.take(idx)
						def high = newValue.drop(idx+2)
						def range = [low,high]
						sendEvent(name: 'heatingSetpointRange', value: range, unit: tu, displayed: false)
						event = eventFront + [value: sendValue, unit: tu, displayed: false]
						objectsUpdated++
					}
					break;

				case 'coolRange':
					if (isChange || forceChange) {
						def newValue = sendValue.toString().replaceAll("[\\(\\)]","")
						def idx = newValue.indexOf('..')
						def low = newValue.take(idx)
						def high = newValue.drop(idx+2)
						def range = [low,high]
						sendEvent(name: 'coolingSetpointRange', value: range, unit: tu, displayed: false)
						event = eventFront + [value: sendValue, unit: tu, displayed: false]
						objectsUpdated++
					}
					break;

				case 'ecobeeConnected':
					if (isChange) {
						if (sendValue == 'false') { disableAllProgramButtons() } else { updateProgramButtons() }
						event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
					}
					break;

				case 'holdEndsAt':
					String schedText = ''
					// log.debug "holdEndsAt: ${sendValue}"
                    if ((sendValue != 'null') && (sendValue.startsWith('a l' /*ong time from now*/))) {
                        // Record the lastHoldType for permanent holds effected through the Thermostat itself, the WebApp, or the Ecobee Mobile app
                        sendEvent(name: 'lastHoldType', value: 'indefinite', displayed: false)
                        schedText = ' forever'
                        objectsUpdated++
                    }
                    String ncCpn = isST ? device.currentValue('currentProgramName') : device.currentValue('currentProgramName', true)
                    if (ncCpn != 'Offline') {
                        if ((schedText == '') && (sendValue != 'null')) {
                            def when = sendValue - 'today at '
                            schedText = ' until ' + when
                        }
                    }
                    sendEvent(name: 'schedText', value: schedText, displayed: false)
                    if (ncCpn != '') {
                        sendEvent(name: 'schedule', value: ncCpn + schedText, displayed: false)
                        objectsUpdated++
                    }
                    if (isChange || forceChange) {
                    	String sendText = sendValue != 'null' ? sendValue : ''
                        if (isStateChange(device, name, sendText)) event = eventFront + [value: sendText, descriptionText: sendText, displayed: false]
                    }
					break;

				case 'hasDehumidifier':
					if (sendValue == 'false') {
						sendEvent(name: 'dehumidifierMode', value: 'off', displayed: false)
					}
					if (isChange) event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
					break;

				case 'hasHumidifier':
					if (sendValue == 'false') {
						sendEvent(name: 'humidifierMode', value: 'off', displayed: false)
					}
					if (isChange) event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
					break;
                
                // The following are all string values that can be 'null' - send "" instead
                case 'eiLocation':
                case 'features':
                case 'groupName':
				case 'groupRef':
                case 'groupSetting':
                case 'name':
				case 'userAccessCode':
                case 'thermostatStatus':
                case 'ventilatorOffDateTime': 
                	// if (isChange) {
                    	def sendText = (sendValue != 'null') ? sendValue : ''
                        if (isStateChange(device, name, sendText)) event = eventFront + [value: sendText, descriptionText: sendText, isStateChange: true, displayed: (sendText != '')]
                    //}
                    break;
                
				// The following are all temperature values, send with the appropriate temperature unit (tu)
				case 'heatDifferential':
				case 'coolDifferential':
				case 'heatCoolMinDelta':
				case 'stage1CoolingDifferentialTemp':
				case 'stage1HeatingDifferentialTemp':
				case 'dehumidifyOvercoolOffset':
				case 'compressorProtectionMinTemp':
				case 'auxMaxOutdoorTemp':
				case 'maxSetBack':
				case 'maxSetForward':
				case 'quickSaveSetBack':
				case 'quickSaveSetForward':
				case 'coolMaxTemp':
				case 'coolMinTemp':
				case 'heatMaxTemp':
				case 'heatMinTemp':
				case 'tempCorrection':
					if (isChange) event = eventFront +	[value: sendValue, unit: tu, isStateChange: true, displayed: false]
					break;

				case 'vent':
					if (isChange) {
						sendEvent(name: 'ventMode', value: sendValue, isStateChange: true, displayed: false)
						event = eventFront +  [value: sendValue, descriptionText: "Vent is ${sendValue}", isStateChange: true, displayed: true]
						// log.debug "vent: ${sendValue}"
					}
					break;

				// These are ones we don't need to display or provide descriptionText for (mostly internal or debug use)
				//
				case 'autoMode':
					// we piggyback the long name on the short name 'autoMode'
					if (isChange) sendEvent(name: 'autoHeatCoolFeatureEnabled', value: sendValue, isStateChange: true, displayed: false)
				case 'heatMode':
				case 'coolMode':
				case 'auxHeatMode':
					def modeValue = (name == "auxHeatMode") ? "auxHeatOnly" : name - "Mode"
					if (sendValue == 'true') {
						if (!supportedThermostatModes.contains(modeValue)) supportedThermostatModes += modeValue
					} else {
						if (supportedThermostatModes.contains(modeValue)) supportedThermostatModes -= modeValue
					}
				// Removed all the miscellaneous settings - they will thus appear in the device event list via 'default:' (May 10, 2019 - BAB)
				// This *should* improve the efficiency of the 'switch', at least a little
				case 'debugLevel':
				case 'decimalPrecision':
				case 'currentProgramId':
				case 'scheduledProgramName':
				case 'scheduledProgramId':
				case 'scheduledProgram':
				case 'programsList':
				case 'temperatureScale':
				case 'checkInterval':
				case 'statHoldAction':
				case 'lastHoldType':
                case 'thermostatOperatingStateDisplay':
					if (isChange) {
                    	def sendText = (sendValue != 'null') ? sendValue : ''
                        if (isStateChange(device, name, sendText)) event = eventFront + [value: sendText, descriptionText: sendText, isStateChange: true, displayed: false]
                    }
					break;

				// everything else gets displayed once with generic text
				default:
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "${name} is ${sendValue}", isStateChange: true, displayed: true]
					break;
			}
            //log.debug "Before event check: ${event}"
			if (event != [:]) {
				if (debugLevelFour) LOG("generateEvent() - Out of switch{}, calling sendevent(${event})", 1)
				sendEvent(event)
			}
			if (tempDisplay != "") {
				sendEvent(name: 'temperatureDisplay', value: tempDisplay, unit: tu, linkText: linkText, descriptionText:"Temperature Display is ${tempDisplay}", displayed: false )
				if (debugLevelFour) LOG("generateEvent() - Temperature updated, calling sendevent(${event})", 4)
				objectsUpdated++
			}
		}
		if (state.supportedThermostatModes != supportedThermostatModes) {
			state.supportedThermostatModes = supportedThermostatModes
			sendEvent(name: "supportedThermostatModes", value: supportedThermostatModes, displayed: false, isStateChange: true)
			sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false, isStateChange: true)
		}
		//generateSetpointEvent()
		//generateStatusEvent()
	}
	def elapsed = now() - startMS
	LOG("Updated ${objectsUpdated} object${objectsUpdated!=1?'s':''} (${elapsed}ms)", 2, this, 'info')
	// if (elapsed > 2500) log.debug results
}

void disableVacationButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	// Can't change these things during Vacation hold, turn their buttons grey
	LOG("Vacation active, disabling buttons",2,null,'info')
	sendEvent(name: 'setModeOff', value: 'off dis', displayed: false)
	sendEvent(name: 'setModeAuto', value: 'auto dis', displayed: false)
	sendEvent(name: 'setModeHeat', value: 'heat dis', displayed: false)
	sendEvent(name: 'setModeCool', value: 'cool dis', displayed: false)
	sendEvent(name: 'thermostatFanModeDisplay', value: "${device.currentValue('thermostatFanMode')} dis", displayed: false)
	sendEvent(name: 'heatLogo', value: 'disabled', displayed: false)
	sendEvent(name: 'coolLogo', value: 'disabled', displayed: false)
	sendEvent(name: 'setFanOff', value: 'off dis', displayed: false)
	sendEvent(name: 'setHome', value: 'home dis', displayed: false)
	sendEvent(name: 'setAway', value: 'away dis', displayed: false)
	sendEvent(name: 'setSleep', value: 'sleep dis', displayed: false)
}
void enableVacationButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	// Re-enable the buttons
	LOG("Vacation finished, enabling buttons",2,null,'info')
	sendEvent(name: 'thermostatFanModeDisplay', value: device.currentValue('thermostatFanMode'), displayed: false)
	sendEvent(name: 'heatLogo', value: 'enabled', displayed: false)
	sendEvent(name: 'coolLogo', value: 'enabled', displayed: false)
	sendEvent(name: 'setFanOff', value: 'off', displayed: false)
	updateProgramButtons()
	updateModeButtons()
}
void disableModeOffButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setModeOff', value: 'off dis', displayed: false)
	sendEvent(name: 'setModeAuto', value: 'auto', displayed: false)
	sendEvent(name: 'setModeHeat', value: 'heat', displayed: false)
	sendEvent(name: 'setModeCool', value: 'cool', displayed: false)
}
void disableModeAutoButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setModeOff', value: 'off'+(statModes().contains('off')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeAuto', value: 'auto dis', displayed: false)
	sendEvent(name: 'setModeHeat', value: 'heat'+(statModes().contains('heat')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeCool', value: 'cool'+(statModes().contains('cool')?'':' dis'), displayed: false)
}
void disableModeHeatButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setModeOff', value: 'off'+(statModes().contains('off')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeAuto', value: 'auto'+(statModes().contains('auto')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeHeat', value: 'heat dis', displayed: false)
	sendEvent(name: 'setModeCool', value: 'cool'+(statModes().contains('cool')?'':' dis'), displayed: false)
}
void disableModeCoolButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setModeOff', value: 'off'+(statModes().contains('off')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeAuto', value: 'auto'+(statModes().contains('auto')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeHeat', value: 'heat'+(statModes().contains('heat')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeCool', value: 'cool dis', displayed: false)
}
void enableAllModeButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setModeOff', value: 'off'+(statModes().contains('off')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeAuto', value: 'auto'+(statModes().contains('auto')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeHeat', value: 'heat'+(statModes().contains('heat')?'':' dis'), displayed: false)
	sendEvent(name: 'setModeCool', value: 'cool'+(statModes().contains('cool')?'':' dis'), displayed: false)
}
void disableAllButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setModeOff', value: 'off dis', displayed: false)
	sendEvent(name: 'setModeAuto', value: 'auto dis', displayed: false)
	sendEvent(name: 'setModeHeat', value: 'heat dis', displayed: false)
	sendEvent(name: 'setModeCool', value: 'cool dis', displayed: false)
	sendEvent(name: 'thermostatFanModeDisplay', value: "${device.currentValue('thermostatFanMode')} dis", displayed: false)
	sendEvent(name: 'heatLogo', value: 'disabled', displayed: false)
	sendEvent(name: 'coolLogo', value: 'disabled', displayed: false)
	sendEvent(name: 'setFanOff', value: 'off dis', displayed: false)
	sendEvent(name: 'setHome', value: 'home dis', displayed: false)
	sendEvent(name: 'setAway', value: 'away dis', displayed: false)
	sendEvent(name: 'setSleep', value: 'sleep dis', displayed: false)
}
void enableAllButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'thermostatFanModeDisplay', value: device.currentValue('thermostatFanMode'), displayed: false)
	sendEvent(name: 'heatLogo', value: 'enabled', displayed: false)
	sendEvent(name: 'coolLogo', value: 'enabled', displayed: false)
	sendEvent(name: 'setFanOff', value: 'off', displayed: false)
	updateModeButtons()
	updateProgramButtons()
}
void disableFanOffButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setFanOff', value: 'off dis', displayed: false)
}
void enableFanOffButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setFanOff', value: 'off', displayed: false)
}
void disableHomeButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setHome', value: 'home dis', displayed: false)
	sendEvent(name: 'setAway', value: 'away', displayed: false)
	sendEvent(name: 'setSleep', value: 'sleep', displayed: false)
}
void disableAwayButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setHome', value: 'home', displayed: false)
	sendEvent(name: 'setAway', value: 'away dis', displayed: false)
	sendEvent(name: 'setSleep', value: 'sleep', displayed: false)
}
void disableSleepButton() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setHome', value: 'home', displayed: false)
	sendEvent(name: 'setAway', value: 'away', displayed: false)
	sendEvent(name: 'setSleep', value: 'sleep dis', displayed: false)
}
void enableAllProgramButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setHome', value: 'home', displayed: false)
	sendEvent(name: 'setAway', value: 'away', displayed: false)
	sendEvent(name: 'setSleep', value: 'sleep', displayed: false)
}
void disableAllProgramButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	sendEvent(name: 'setHome', value: 'home dis', displayed: false)
	sendEvent(name: 'setAway', value: 'away dis', displayed: false)
	sendEvent(name: 'setSleep', value: 'sleep dis', displayed: false)
}
void updateProgramButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	def currentProgram = device.currentValue('currentProgram')
	// if (currentProgram=='Home') {sendEvent(name:'setHome', value:'home dis', displayed:false)} else {sendEvent(name:'setHome', value:'home', displayed:false)}
	sendEvent(name:'setHome',value:'home'+(currentProgram=='Home'?' dis':''), displayed:false)
	sendEvent(name:'setAway',value:'away'+(currentProgram=='Away'?' dis':''), displayed:false)
	sendEvent(name:'setSleep',value:'sleep'+(currentProgram=='Sleep'?' dis':''), displayed:false)
}
void updateModeButtons() {
	if (isHE) return	// No need on Hubitat, at least not until we get a mobile UI
    
	def currentMode = device.currentValue('thermmoostatMode')
	// if (currentMode=='off') {sendEvent(name:'setModeOff', value:'off dis', displayed:false)} else {sendEvent(name:'setModeOff', value:'off', displayed:false)}
	sendEvent(name:'setModeOff', value:('off'+((currentMode=='off') || !statModes().contains('off'))?' dis':''), displayed:false)
	sendEvent(name:'setModeAuto', value:('auto'+((currentMode=='auto') || !statModes().contains('auto'))?' dis':''), displayed:false)
	sendEvent(name:'setModeHeat', value:('heat'+((currentMode=='heat') || !statModes().contains('heat'))?' dis':''), displayed:false)
	sendEvent(name:'setModeCool', value:('cool'+((currentMode=='cool') || !statModes().contains('cool'))?' dis':''), displayed:false)
}

//return descriptionText to be shown on mobile activity feed
String getTemperatureDescriptionText(name, value, linkText) {
	switch (name) {
		case 'temperature':
			return "Temperature is ${value}°${getTemperatureScale()}"
			break;
		case 'heatingSetpoint':
			return "Heating setpoint is ${value}°${getTemperatureScale()}"
			break;
		case 'coolingSetpoint':
			return "Cooling setpoint is ${value}°${getTemperatureScale()}"
			break;
		case 'thermostatSetpoint':
			return "Thermostat setpoint is ${value}°${getTemperatureScale()}"
			break;
		case 'weatherTemperature':
			return "Outdoor temperature is ${value}°${getTemperatureScale()}"
			break;
		case 'weatherDewpoint':
			return "Outdoor dew point is ${value}°${getTemperatureScale()}"
	}
	return ""
}

// ***************************************************************************
// Thermostat setpoint commands
// API calls and UI handling
// ***************************************************************************
void setHeatingSetpointDelay(setpoint) {
	LOG("Slider requested heat setpoint: ${setpoint}",4,null,'trace')
	def runWhen = (getParentSetting('arrowPause')?: 4) as Integer
	runIn( runWhen, 'sHS', [data: [setpoint:setpoint.toBigDecimal()]] )
}
void sHS(data) {
	LOG("Setting heating setpoint to: ${data.setpoint}",4,null,'trace')
	setHeatingSetpoint(data.setpoint.toBigDecimal())
}
void setHeatingSetpoint(setpoint, String sendHold=null) {
	def thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("setHeatingSetpoint(${setpoint}) called but thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
    if (!setpoint || (setpoint == '') || (setpoint == 'null')) {
    	LOG("setHeatingSetpoint(${setpoint}) - Invalid setpoint",2,null,'warn')
        return
    }

	def isMetric = wantMetric()
	def temperatureScale = isMetric ? 'C' : 'F'
	LOG("setHeatingSetpoint() request with setpoint value = ${setpoint}°${temperatureScale}", 2, null, 'info')

	if (!isMetric && (setpoint < 35.0)) {
		setpoint = roundIt(cToF(setpoint), 1)	// Hello, Google hack - seems to request C when stat is in Auto mode
		LOG ("setHeatingSetpoint() converted apparent C setpoint value to ${setpoint}°F", 2, null, 'info')
	}

	def heatingSetpoint = isMetric ? roundC(setpoint) : roundIt(setpoint, 1)

	//enforce limits of heatingSetpoint range
	LOG("setHeatingSetpoint() before adjustment: ${heatingSetpoint}°${temperatureScale}", 4, null, 'trace')
	def low  = (isST ? device.currentValue('heatRangeLow')  : device.currentValue('heatRangeLow', true)).toBigDecimal()		// already converted to C by Parent
	def high = (isST ? device.currentValue('heatRangeHigh') : device.currentValue('heatRangeHigh', true)).toBigDecimal()
	LOG("setHeatingSetpoint() low: ${low}°, high: ${high}°",4,null,'trace')
	// Silently adjust the temps to fit within the specified ranges
	if (heatingSetpoint < low ) { heatingSetpoint = isMetric ? roundC(low) : low }
	else if (heatingSetpoint > high) { heatingSetpoint = isMetric ? roundC(high) : high }

	LOG("setHeatingSetpoint() requesting heatingSetpoint: ${heatingSetpoint}°${temperatureScale}", 2, null, 'info')
	state.newHeatingSetpoint = heatingSetpoint
	state.useThisHold = sendHold
	runIn(2, "updateThermostatSetpoints", [overwrite: true])
}

//void setCoolingSetpoint(setpoint) {
//	LOG("setCoolingSetpoint() request with setpoint value = ${setpoint} (before toDouble)", 4)
//	setCoolingSetpoint(setpoint.toDouble())
//}
void setCoolingSetpointDelay(setpoint) {
	LOG("Slider requested cool setpoint: ${setpoint}",4,null,'trace')
	def runWhen = (getParentSetting('arrowPause') ?: 4 ) as Integer
	runIn( runWhen, 'sCS', [data: [setpoint:setpoint.toBigDecimal()]] )
}
void sCS(data) {
	LOG("Setting cooling setpoint to: ${data.setpoint}",4,null,'trace')
	setCoolingSetpoint(data.setpoint.toBigDecimal())
}
void setCoolingSetpoint(setpoint, String sendHold='') {
	def thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("setCoolingSetpoint(${setpoint}) called but thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
    if (!setpoint || (setpoint == '') || (setpoint == 'null')) {
    	LOG("setCoolingSetpoint(${setpoint}) - Invalid setpoint",2,null,'warn')
        return
    }
	def isMetric = wantMetric()
	def temperatureScale = isMetric ? 'C' : 'F'
	LOG("setCoolingSetpoint() request with setpoint value = ${setpoint}°${temperatureScale}", 2, null, 'info')

	if (!isMetric && (setpoint < 35.0)) {
		setpoint = roundIt(cToF(setpoint), 1)	// Hello, Google hack - seems to request C when stat is in Auto mode
		LOG ("setCoolingSetpoint() converted apparent C setpoint value to ${setpoint}°F", 2, null, 'info')
	}

	def coolingSetpoint = isMetric ? roundC(setpoint) : roundIt(setpoint, 1)

	//enforce limits of heatingSetpoint vs coolingSetpoint
	LOG("setCoolingSetpoint() before adjustment: coolingSetpoint = ${coolingSetpoint}°${temperatureScale}", 4, null, 'trace')
	def low  = (isST ? device.currentValue('coolRangeLow')  : device.currentValue('coolRangeLow', true)).toBigDecimal()
	def high = (isST ? device.currentValue('coolRangeHigh') : device.currentValue('coolRangeHigh', true)).toBigDecimal()
	LOG("setCoolingSetpoint() low: ${low}°, high: ${high}°",4,null,'trace')
	// Silently adjust the temps to fit within the specified ranges
	if (coolingSetpoint < low ) { coolingSetpoint = isMetric ? roundC(low) : low }
	else if (coolingSetpoint > high) { coolingSetpoint = isMetric ? roundC(high) : high }

	LOG("setCoolingSetpoint() requesting coolingSetpoint: ${coolingSetpoint}°${temperatureScale}", 2, null, 'info')
	state.newCoolingSetpoint = coolingSetpoint.toBigDecimal()
	state.useThisHold = sendHold
	runIn(2, "updateThermostatSetpoints", [overwrite: true])
}

void updateThermostatSetpoints() {
	def isMetric = wantMetric()
	def deviceId = getDeviceId()

	def heatingSetpoint = state.newHeatingSetpoint ? state.newHeatingSetpoint : (isST ? device.currentValue('heatingSetpoint') : device.currentValue('heatingSetpoint', true)).toBigDecimal()
	def coolingSetpoint = state.newCoolingSetpoint ? state.newCoolingSetpoint : (isST ? device.currentValue('coolingSetpoint') : device.currentValue('coolingSetpoint', true)).toBigDecimal()
	LOG("updateThermostatSetpoints(): heatingSetpoint ${heatingSetpoint}°, coolingSetpoint ${coolingSetpoint}°", 2, null, 'info')

	def sendHoldType = state.useThisHold
	if (sendHoldType) { state.useThisHold = null } else { sendHoldType = whatHoldType() }
	def sendHoldHours = null
	if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
		sendHoldHours = sendHoldType
		sendHoldType = 'holdHours'
	}
	LOG("updateThermostatSetpoints(): sendHoldType == ${sendHoldType} ${sendHoldHours}", 4, null, 'trace')

	// make sure we maintain the proper heatCoolMinDelta
	def delta = (isST ? device.currentValue('heatCoolMinDelta') : device.currentValue('heatCoolMinDelta', true))?.toBigDecimal()
	if (!delta) delta = isMetric ? 0.5 : 1.0

	if ((heatingSetpoint + delta) > coolingSetpoint) {
		// we have to adjust heat/cool
		if (state.newHeatingSetpoint) {
			if (!state.newCoolingSetpoint) {
				// got request to change heat only, push cool up
				coolingSetpoint = heatingSetpoint + delta
			} else {
				// got request to change both, let's see what the thermostat is doing
				def mode = isST ? device.currentValue('thermostatMode') : device.currentValue('thermostatMode', true)
				if (mode == 'heat') {
					// we are in Heat Mode, so raise the cooling setpoint
					coolingSetpoint = heatingSetpoint + delta
				} else if (mode == 'cool') {
					// we are in Cool Mode, so lower the heating setpoint
					heatingSetpoint = coolingSetpoint - delta
				} else if (mode == 'auto') {
					// Auto Mode - let's see if the heat or cooling is on
					def opState = isST ? device.currentValue('thermostatOperatingState') : device.currentValue('thermostatOperatingState', true)
					if (opState.contains('ea')) {
						// currently heating, raise the cooling setpoint
						coolingSetpoint = heatingSetpoint + delta
					} else if (opState.contains('oo')) {
						// currently cooling, lower the heating setpoint
						heatingSetpoint = coolingSetpoint - delta
					} else {
						// Auto & Idle, check what our last operating state was and adjust the opposite
                        def ncLos = isST ? device.currentValue('lastOpState') : device.currentValue('lastOpState', true)
						if (ncLos?.contains('ea')) {
							// We were last heating, raise the cooling setpoint
							coolingSetpoint = heatingSetpoint + delta
						} else {
							// Must have been cooling, lower the heating setpoint
							heatingSetpoint = coolingSetpoint - delta
						}
					}
				}
			}
		} else if (state.newCoolingSetpoint) {
			// heat not requested, so just lower the heating setpoint
			heatingSetpoint = coolingSetpoint - delta
		}
		LOG("updateThermostatSetpoints() adjusted setpoints, heat ${heatingSetpoint}°, cool ${coolingSetpoint}°", 2, null, 'info')
	}

	if (parent.setHold(this, heatingSetpoint,  coolingSetpoint, deviceId, sendHoldType, sendHoldHours)) {
		def updates = [	'coolingSetpoint': roundIt(coolingSetpoint, 1),	// was Display
						'heatingSetpoint': roundIt(heatingSetpoint, 1),	// was Display
						'lastHoldType': sendHoldType ]
		generateEvent(updates)
		def thermostatSetpoint = isST ? device.currentValue('thermostatSetpoint') : device.currentValue('thermostatSetpoint', true)
		LOG("Done updateThermostatSetpoints() coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}, thermostatSetpoint: ${thermostatSetpoint}",4,null,'trace')
		// generateStatusEvent()
	} else {
		LOG("Error updateThermostatSetpoints()", 2, null, "error") //This error is handled by the connect app (huh???)
	}
	state.newHeatingSetpoint = null
	state.newCoolingSetpoint = null
	runIn(5, 'refresh', [overwrite: true])
}

// raiseSetpoint: called by tile when user hit raise temperature button on UI
void raiseSetpoint() {
	LOG('raiseSetpoint()',4,null,'trace')
	// Cannot change setpoint while in Vacation mode
	def thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("Cannot change the set point while thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}

	String mode = isST ? device.currentValue("thermostatMode") : device.currentValue("thermostatMode", true)
	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("raiseSetpoint(): this mode: (${mode}) does not support raiseSetpoint${mode=='auto'?' - try enabling Smart Auto in Ecobee Suite Manager preferences':''}",1,null,'warn')
		return
	}

	def isMetric = wantMetric()
	def changingHeat = true			// is a change already in process?
	def changingCool = true
	def heatingSetpoint = state.newHeatSetpoint // device.currentValue('newHeatSetpoint')?.toDouble()
	if (!heatingSetpoint || (heatingSetpoint == 0.0)) {
		heatingSetpoint = (isST ? device.currentValue("heatingSetpoint") : device.currentValue("heatingSetpoint", true))?.toBigDecimal()
		changingHeat = false
	}
	def coolingSetpoint = state.newCoolSetpoint /// device.currentValue('newCoolSetpoint')?.toDouble()
	if (!coolingSetpoint || (coolingSetpoint == 0.0)) {
		coolingSetpoint = (isST ? device.currentValue("coolingSetpoint") : device.currentValue("coolingSetpoint", true))?.toBigDecimal()
		changingCool = false
	}
	def thermostatSetpoint = (isST ? device.currentValue("thermostatSetpoint") : device.currentValue("thermostatSetpoint", true))?.toBigDecimal()
	def operatingState = isST ? device.currentValue("thermostatOperatingState") : device.currentValue("thermostatOperatingState", true)
	LOG("raiseSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}", 2,null,'trace')

	//def precision = device.currentValue('decimalPrecision')
	//if (precision == null) precision = isMetric ? 1 : 0
	def targetValue
	def smartAuto = usingSmartAuto()
	def runWhen = (getParentSetting('arrowPause') ?: 4 ) as Integer
	if ((mode == 'auto') && smartAuto) {
		raiseSmartSetpoint(heatingSetpoint, coolingSetpoint)
		return
	}
    def ncLos = isST ? device.currentValue('lastOpState') : device.currentValue('lastOpState', true)
	if (changingHeat || (mode == 'heat') || ((mode == 'auto') && !smartAuto && (operatingState.contains('ea') || ncLos?.contains('ea')))) {
		targetValue = roundIt((heatingSetpoint + (isMetric ? 0.5 : 1.0)),1)
		LOG("raiseSetpoint() Heating to ${targetValue}", 4)
		state.newHeatSetpoint = targetValue		// sendEvent(name: 'newHeatSetpoint', value: targetValue, display: false, isStateChange: true)
		def updates = ['thermostatSetpoint': targetValue]
		generateEvent(updates)
		runIn( runWhen, 'changeHeatSetpoint', [data: [value:targetValue]] )
	} else if (changingCool || (mode == 'cool') || ((mode == 'auto') && !smartAudio && (operatingState.contains('oo') || ncLos?.contains('oo')))) {
		targetValue = roundIt((coolingSetpoint + (isMetric ? 0.5 : 1.0)), 1)
		LOG("raiseSetpoint() Cooling to ${targetValue}", 4)
		state.newCoolSetpoint = targetValue		// sendEvent(name: 'newCoolSetpoint', value: targetValue, display: false, isStateChange: true)
		def updates = ['thermostatSetpoint': targetValue]
		generateEvent(updates)
		runIn( runWhen, 'changeCoolSetpoint', [data: [value:targetValue]] )
	}
}

// lowerSetpoint: called by tile when user hit lower temperature button on UI
void lowerSetpoint() {
	LOG('lowerSetpoint()',4,null,'trace')
	// Cannot change setpoint while in Vacation mode
	def thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("Cannot change the set point while thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
	
	String mode = isST ? device.currentValue("thermostatMode") : device.currentValue("thermostatMode", true)
	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("lowerSetpoint(): this mode: $mode does not support lowerSetpoint${mode=='auto'?' - try enabling Smart Auto in Ecobee Suite Manager preferences':''}", 1, null, "warn")
		return
	}

	def isMetric = wantMetric()
	def changingHeat = true			// is a change already in process?
	def changingCool = true
	def heatingSetpoint = state.newHeatSetpoint		// device.currentValue('newHeatSetpoint')?.toDouble()
	if (!heatingSetpoint || (heatingSetpoint == 0.0)) {
		heatingSetpoint = isST ? device.currentValue("heatingSetpoint")?.toBigDecimal() : device.currentValue("heatingSetpoint", true)?.toBigDecimal()
		changingHeat = false
	}
	def coolingSetpoint = state.newCoolSetpoint		// device.currentValue('newCoolSetpoint')?.toDouble()
	if (!coolingSetpoint || (coolingSetpoint == 0.0)) {
		coolingSetpoint = isST ? device.currentValue("coolingSetpoint")?.toBigDecimal() : device.currentValue("coolingSetpoint", true)?.toBigDecimal()
		changingCool = false
	}
	def thermostatSetpoint = isST ? device.currentValue("thermostatSetpoint").toBigDecimal() : device.currentValue("thermostatSetpoint", true).toBigDecimal()
	def operatingState = isST ? device.currentValue("thermostatOperatingState") : device.currentValue("thermostatOperatingState", true)
	LOG("lowerSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}", 2,null,'trace')

	//def precision = device.currentValue('decimalPrecision')
	//if (precision == null) precision = isMetric ? 1 : 0
	def targetValue
	def smartAuto = usingSmartAuto()
	def runWhen = (getParentSetting('arrowPause') ?: 4) as Integer

	if ((mode == 'auto') && smartAuto) {
		lowerSmartSetpoint(heatingSetpoint, coolingSetpoint)
		return
	}
	def ncLos = isST ? device.currentValue('lastOpState') : device.currentValue('lastOpState', true)
	if (changingHeat || (mode == 'heat') || ((mode == 'auto') && !smartAuto && (operatingState.contains('ea') || ncLos?.contains('ea')))) {
		targetValue = roundIt((heatingSetpoint - (isMetric ? 0.5 : 1.0)), 1)
		LOG("lowerSetpoint() Heating to ${targetValue}", 4)
		state.newHeatSetpoint = targetValue			// sendEvent(name: 'newHeatSetpoint', value: targetValue, display: false, isStateChange: true)
		def updates = ['thermostatSetpoint': targetValue]
		generateEvent(updates)
		runIn( runWhen, 'changeHeatSetpoint', [data: [value:targetValue]] )
	}
	if (changingCool || (mode == 'cool') || ((mode == 'auto') && !smartAuto && (operatingState.contains('oo') || ncLos?.contains('oo')))) {
		targetValue = roundIt((coolingSetpoint - (isMetric ? 0.5 : 1.0)), 1)
		LOG("lowerSetpoint() Cooling to ${targetValue}", 4)
		state.newCoolSetpoint = targetValue			// sendEvent(name: 'newCoolSetpoint', value: targetValue, display: false, isStateChange: true)
		def updates = ['thermostatSetpoint': targetValue]
		generateEvent(updates)
		runIn( runWhen, 'changeCoolSetpoint', [data: [value:targetValue]] )
	}
}

// changeHeatSetpoint: effect setHeatingSetpoint for UI
void changeHeatSetpoint(newSetpoint) {
	def updates = ['heatingSetpoint': newSetpoint.value.toBigDecimal(), 'currentProgramName':'Hold: Temp']
	generateEvent(updates)
	LOG("changeHeatSetpoint(${newSetpoint.value}°)",2,null,'trace')
	setHeatingSetpoint( newSetpoint.value.toBigDecimal() )
	state.newHeatSetpoint = null	// sendEvent(name: 'newHeatSetpoint', value: 0.0, displayed: false, isStateChange: true)
}

// changeCoolSetpoint: effect setCoolingSetpoint for UI
void changeCoolSetpoint(newSetpoint) {
	def updates = ['coolingSetpoint': newSetpoint.value.toBigDecimal(), 'currentProgramName':'Hold: Temp']
	generateEvent(updates)
	LOG("changeCoolSetpoint(${newSetpoint.value}°)",2,null,'trace')
	setCoolingSetpoint( newSetpoint.value.toBigDecimal() )
	state.newCoolSetpoint = null	// sendEvent(name: 'newCoolSetpoint', value: 0.0, displayed: false, isStateChange: true)
}

// raiseSmartSetpoint: figure out which setpoint to raise
void raiseSmartSetpoint(heatingSetpoint, coolingSetpoint) {
	def isMetric = wantMetric()
	def currentTemp = isST ? device.currentValue('temperature').toBigDecimal() : device.currentValue('temperature', true).toBigDecimal()
	//def precision = device.currentValue('decimalPrecision')
	//if (precision == null) precision = isMetric ? 1 : 0
	def adjust = (isMetric ? 0.5 : 1.0)
	def newHeat = roundIt((heatingSetpoint + adjust), 1)
	def newCool = roundIt((coolingSetpoint + adjust), 1)

	if (newHeat > currentTemp) {
		// turn the heat up
		LOG("raiseSmartSetpoint() Heating to ${newHeat}", 4)
		state.newHeatSetpoint = newHeat			// sendEvent(name: 'newHeatSetpoint', value: newHeat, display: false, isStateChange: true)
		def updates = ['thermostatSetpoint': newHeat]
		generateEvent(updates)
		runIn( runWhen, 'changeHeatSetpoint', [data: [value:newHeat], overwrite: true] )
	} else {
		// turn the cool up
		LOG("raiseSmartSetpoint() Cooling to ${newCool}", 4)
		state.newCoolSetpoint = newCool			// sendEvent(name: 'newCoolSetpoint', value: newCool, display: false, isStateChange: true)
		def updates = ['thermostatSetpoint': newCool]
		generateEvent(updates)
		runIn( runWhen, 'changeCoolSetpoint', [data: [value:newCool], overwrite: true] )
	}
}

// lowerSmartSetpoint: figure out which setpoint to lower
void lowerSmartSetpoint(heatingSetpoint, coolingSetpoint) {
	def isMetric = wantMetric()
	def currentTemp = isST ? device.currentValue('temperature').toBigDecimal() : device.currentValue('temperature', true).toBigDecimal()
	//def precision = device.currentValue('decimalPrecision')
	//if (precision == null) precision = isMetric ? 1 : 0
	def adjust = (isMetric ? 0.5 : 1.0)
	def newHeat = roundIt((heatingSetpoint - adjust), 1)
	def newCool = roundIt((coolingSetpoint - adjust), 1)

	if (newCool < currentTemp) {
		// turn the cool down
		LOG("lowerSmartSetpoint() Cooling to ${newCool}", 4)
		state.newCoolSetpoint = newCool			// sendEvent(name: 'newCoolSetpoint', value: newCool, display: false, isStateChange: true)
		def updates = ['thermostatSetpoint': newCool]
		generateEvent(updates)
		runIn( runWhen, 'changeCoolSetpoint', [data: [value:newCool], overwrite: true] )
	} else {
		// turn the heat down
		LOG("lowerSmartSetpoint() Heating to ${newHeat}", 4)
		state.newHeatSetpoint = newHeat			// sendEvent(name: 'newHeatSetpoint', value: newHeat, display: false, isStateChange: true)
		def updates = ['thermostatSetpoint': newHeat]
		generateEvent(updates)
		runIn( runWhen, 'changeHeatSetpoint', [data: [value:newHeat], overwrite: true] )
	}
}

void resetUISetpoints() {
	// sendEvent(name: 'newHeatSetpoint', value: 0.0, displayed: false)
	// sendEvent(name: 'newCoolSetpoint', value: 0.0, displayed: false)
	state.newHeatSetpoint = null		// NOTE: different names than used by setHeatingSetpoint/setCoolingSetpoint is INTENTIONAL
	state.newCoolSetpoint = null		// in case some API calls to change temps while user is manually changing them also
}

// alterSetpoint: change setpoint (obsolete: no longer used as of v1.2.21)
void alterSetpoint(temp) {
	LOG('alterSetpoint called in error',1,null,'error')
}

//* 'Fix' buttons that really aren't buttons (ie. stop them from changing if pressed)
void noOpWeatherTemperature() { sendEvent(name:'weatherTemperature',value:device.currentValue('weatherTemperature'),displayed:false,isStateChange:true) }
void noOpCurrentProgramName() { sendEvent(name:'currentProgramName',value:device.currentValue('currentProgramName'),displayed:false,isStateChange:true) }

void generateQuickEvent(String name, String value, Integer pollIn=0) {
	sendEvent(name: name, value: value, displayed: false)
	if (pollIn > 0) { runIn(pollIn, refresh, [overwrite: true]) }
}

// ***************************************************************************
// Thermostat Mode commands
// Note that the Thermostat Mode CAN be changed while in Vacation mode
// ***************************************************************************
void setThermostatMode(String value) {
	//	Uses Ecobee Modes: "auxHeatOnly" "heat" "cool" "off" "auto"

	if (value.startsWith('emergency')) { value = 'auxHeatOnly' }
	LOG("setThermostatMode(${value})", 5)

	def validModes = statModes()		// device.currentValue('supportedThermostatModes')
	if (!validModes.contains(value)) {
		LOG("Requested Thermostat Mode (${value}) is not supported by ${device.displayName}", 2, null, 'warn')
		return
	}

	boolean changed = true
    def tMode = isST ? device.currentValue('thermostatMode') : device.currentValue('thermostatMode', true) 
	if (tMode != value) {
		if (parent.setMode(this, value, getDeviceId())) {
			// generateQuickEvent('thermostatMode', value, 5)
			def updates = [thermostatMode:value]
			if (value == 'off') {
				updates += [equipmentOperatingState:'off', thermostatOperatingState:'idle']
                def tFMode = isST ? device.currentValue('thermostatFanMode') : device.currentValue('thermostatFanMode', true)
				if ( tFMode == 'auto') {
                	def ncFMOT = isST ? device.currentValue('fanMinOnTime') : device.currentValue('fanMinOnTime', true)
					if ( ncFMOT == 0) {
						updates += [thermostatFanMode:'off',thermostatFanModeDisplay:'off']		// generateEvent will make this "fan dis"
					} else {
						updates += [thermostatFanMode:'circulate',thermostatFanModeDisplay:'circulate']
					}
				}
			} else {
				updates += [equipmentOperatingState:'idle',thermostatOperationsState:'idle',thermostatOperatingStateDisplay:'idle']
			}
			// log.debug "updates: ${updates}"
			generateEvent(updates)	// force everything to update
		} else {
			LOG("Failed to change Thermostat Mode to ${value}, Mode is ${device.currentValue('thermostatMode')}", 1, null, 'error')
			changed = false
		}
	}
	if (changed) LOG("Thermostat Mode changed to ${value}",2,null,'info')
}
def statModes() {
	return state.supportedThermostatModes
}
void off() {
	LOG('off()', 4, null, 'trace')
	setThermostatMode('off')
}
void heat() {
	LOG('heat()', 4, null, 'trace')
	setThermostatMode('heat')
}
void auxHeatOnly() {
	LOG("auxHeatOnly()", 4, null, 'trace')
	setThermostatMode("auxHeatOnly")
}
void emergency() {
	LOG("emergency()", 4, null, 'trace')
	setThermostatMode("auxHeatOnly")
}
// This is the proper definition for the capability
void emergencyHeat() {
	LOG("emergencyHeat()", 4, null, 'trace')
	setThermostatMode("auxHeatOnly")
}
void cool() {
	LOG('cool()', 4, null, 'trace')
	setThermostatMode('cool')
}
void auto() {
	LOG('auto()', 4, null, 'trace')
	setThermostatMode('auto')
}

// ***************************************************************************
// Thermostat Program (aka Climates) Commands
// Program/Climates CANNOT be changed while in Vacation mode
// ***************************************************************************
void setThermostatProgram(String program, String holdType=null, Integer holdHours=2) {
	// N.B. if holdType is provided, it must be one of the valid parameters for the Ecobee setHold call (indefinite, nextTransition, holdHours). dateTime not currently supported
	refresh()
	
	String currentThermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true)
	if (currentThermostatHold == 'vacation') {
		LOG("setThermostatProgram(${program}): program change requested but ${device.displayName} is in Vacation mode, ignoring request",2,null,'warn')
		return
	}

	def programsList = []
	def programs = device.currentValue('programsList')
	if (!programs) {
		LOG("Supported programs list not initialized, possible installation error", 1, this, 'warn')
		programsList = ["Away","Home","Sleep","Resume"]		// Just use the default list
	} else {
		programsList = new JsonSlurper().parseText(programs) + ['Resume']
	}

	if ((program == null) || (program == "") || (!programsList.contains(program))) {
		LOG("setThermostatProgram( ${program} ): Missing or Invalid argument - must be one of (${programsList.toString()[1..-2]})", 2, this, 'warn')
		return
	}

	def deviceId = getDeviceId()
	LOG("setThermostatProgram(${program}, ${holdType}, ${holdHours})", 4, this,'trace')

	if (program == 'Resume') {
		LOG("setThermostatProgram() Resuming scheduled program", 2, this, 'info')
		resumeProgram( true )
		return
	}

	def sendHoldType = null
	def sendHoldHours = null
	if (holdType && (holdType != "")) {
		sendHoldType = holdType
		if (holdType == 'holdHours') sendHoldHours = holdHours
	} else {
		sendHoldType =	whatHoldType()
		if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
			sendHoldHours = sendHoldType
			sendHoldType = 'holdHours'
		}
	}
	// refresh()		// need to know if scheduled program changed recently

	String currentProgram 
	String currentProgramName
	String scheduledProgram 
    if (isST) {
    	currentProgram =	device.currentValue('currentProgram')
    	currentProgramName =device.currentValue('currentProgramName')
    	scheduledProgram = 	device.currentValue('scheduledProgram')
    } else {
    	currentProgram =	device.currentValue('currentProgram', true)
    	currentProgramName =device.currentValue('currentProgramName', true)
    	scheduledProgram =  device.currentValue('scheduledProgram', true)
    } 

	if (currentProgram == program) {
		if ((currentProgram == currentProgramName) && (sendHoldType == 'nextTransition')) {
			// Already running desired program, not in a hold, and Program will change at next transition as requested
			LOG("Thermostat Program is ${program} (already)", 2, this, 'info')
			return
		} else if ((currentProgramName == "Hold: ${currentProgram}") || (currentProgramName == "Auto ${currentProgram}")) {
			// We are already in a Hold for the requested program
			if ((scheduledProgram == program) && (sendHoldType == 'nextTransition')) {
				// The scheduled program is the same as the requested one, and caller wants next transition, so we can just resume that program
				// This really should not occur, but we essentially just want to pop the stack of Holds if it does
				LOG("Thermostat Program is ${program} (resumed)", 2, this, 'info')
				resumeProgramInternal(true)
				// runIn(5, refresh, [overwrite: false])
				generateProgramEvent(program,'')		// this may be redundant - resumeProgramInternal does this also
				return
			} else {
				// Otherwise (already in a hold for the desired program), do nothing if we can verify that the holdType is the same
				def lastHoldType = isST ? device.currentValue('lastHoldType') : device.currentValue('lastHoldType', true)
				if (lastHoldType && (sendHoldType == lastHoldType) && (sendHoldType != 'holdHours')) {	// we have to reset the timer on an Hourly hold
					LOG("Thermostat Program is already Hold: ${program} (${sendHoldType})", 2, this, 'info')
					return
				}
			}
		}
	}

	// if the requested program is the same as the one that is supposed to be running, then just resumeProgram
	def needRefresh = true
	if (scheduledProgram == program) {
		if (resumeProgramInternal(true)) {							// resumeAll so that we get back to scheduled program
			LOG("Thermostat Program is ${program} (resumed)", 2, this, 'info')
			if (sendHoldType == 'nextTransition') {					// didn't want a permanent hold, so we are all done (don't need to set a new hold)
				// runIn(3, refresh, [overwrite: false])
				generateProgramEvent(program,'')
				def updates = [ 'lastHoldType': sendHoldType ]
				generateEvent(updates)
				return
			}
		}
	} else {
		resumeProgramInternal(true)							// resumeAll before we change the program
		needRefresh = false
	}

	if ( parent.setProgram(this, program, deviceId, sendHoldType, sendHoldHours) ) {
		if (needRefresh) runIn(5, refresh, [overwrite: true])

		LOG("Thermostat Program is Hold: ${program}",2,this,'info')
		generateProgramEvent(program, program)				// ('Hold: '+program)
		def updates = [ 'lastHoldType': sendHoldType ]
		generateEvent(updates)
	} else {
		LOG("Error setting Program to ${program}", 2, this, "warn")
		// def priorProgram = device.currentState("currentProgramId")?.value
		// generateProgramEvent(priorProgram, program) // reset the tile back
	}
}

// SmartThings somewhere decided that "schedule" would be used to define "programs" ("climates" on Ecobee). But they punted on the argument for the call,
// using the ill-defined "scheduleJson" as a JSON_OBJECT. For this implementation, we'll allow quite a bit of latitude for what this is, including just a
// simple string.
void setSchedule(scheduleJson) {
	LOG("setSchedule( ${scheduleJson} )", 4, this, 'trace')
	if ((scheduleJson != null) && (scheduleJson != '[null]') && (scheduleJson != 'null') && (scheduleJson != '')) {
		if (scheduleJson instanceof CharSequence) {
			// Handle Strings made to look like JSON (e.g. commands from Hubitat Hubconnect device driver)
			while (scheduleJson.startsWith('[') || scheduleJson.startsWith('{')) { scheduleJson = scheduleJson[1..-2] }	// Recursively trim brackets/braces
			if (scheduleJson.contains(':')) {
				// Looks like a Map masquerading as a String
				def scheduleList = scheduleJson.contains(',') ? scheduleJson.split(',') : [scheduleJson]
				scheduleJson = [:]
				scheduleList.each {
					def m = it.split(':')
					scheduleJson += [ (m[0].trim()) : m[1].trim() ]
				}
			} else if (scheduleJson.contains(',')) {
				// Looks like a List masquerading as a String
				if (scheduleJson.contains('name')) {
					// It's actually a lazy Map (name, Home, holdType, nextTransition) masquerading as a String
					def scheduleList = scheduleJson.split(',')
					def s = scheduleList.size()
					int i
					scheduleJson = [:]
					for (i=0; i+1 <= s; i=i+2) {
						scheduleJson += [ (scheduleList[i].trim()) : scheduleList[i+1].trim() ]
					}
				} else {
					// OK, it's really just a List (Home, nextTransition) masquerading as a String
					scheduleJson = (scheduleJson.split(',') + [null, null]) as List			// Pad with extra arguments
				}
			} // else it's a one-argument String - leave it alone if not a Map or a List
		}
		LOG("scheduleJson  -  isMap: ${scheduleJson instanceof Map}, isList: ${scheduleJson instanceof List}, isCharSequence: ${scheduleJson instanceof CharSequence}", 4, this, 'debug')
		LOG("setSchedule( ${scheduleJson} )", 2, this, 'info')
		if (scheduleJson instanceof Map) {
			// [name: "scheduleName", holdType: ["nextTransition", "indefinite", "holdHours"], holdHours: number]
			if (scheduleJson.holdHours && scheduleJson.holdType && scheduleJson.name) {
				setThermostatProgram( scheduleJson.name?.trim().capitalize(), scheduleJson.holdType?.trim(), scheduleJson.holdHours?.trim())
			} else if (scheduleJson.holdType && scheduleJson.name) {
				setThermostatProgram( scheduleJson.name?.trim().capitalize(), scheduleJson.holdType?.trim())
			} else if (scheduleJson.name) {
				setThermostatProgram( scheduleJson.name?.trim().capitalize())
			}
		} else if (scheduleJson instanceof List) {
			// [scheduleName, holdType, holdHours]
			setThermostatProgram( scheduleJson[0].trim().capitalize(), scheduleJson[1]?.trim(), scheduleJson[2]?.trim() )
		} else if (scheduleJson instanceof CharSequence) {
			// Simple scheduleName
			setThermostatProgram( scheduleJson.trim().capitalize() )
		} else {
			LOG("setSchedule( ${scheduleJson} ): Invalid argument Class - must be String, Map or List", 2, this, 'warn')
		}
	} else {
		def programsList = []
		def programs = device.currentValue('programsList')
		if (!programs) {
			LOG("Supported programs list not initialized, possible installation error", 1, this, 'warn')
			programsList = ['Away','Home','Sleep','Resume']		// Just use the default list
		} else {
			programsList = new JsonSlurper().parseText(programs) + ['Resume']
		}
		LOG("setSchedule( null ): Missing or Invalid argument - must be one of (${programsList.toString()[1..-2]})", 1, this, 'warn')
	}
}
void present(){
	// Change the Comfort Setting to Home (Nest compatibility)
	LOG('present()', 5, null, 'trace')
	setThermostatProgram('Home')
}
void homeDisabled() {}
void home() {
	// Change the Comfort Setting to Home
	LOG('home()', 5, null, 'trace')
	setThermostatProgram('Home')
}
void awayDisabled() {}
void away() {
	// Change the Comfort Setting to Away
	LOG('away()', 5, null, 'trace')
	setThermostatProgram('Away')
}
// Unfortunately, we can't overload the internal Java/Groovy/system definition of 'sleep()'
/*
void sleep() {
	// Change the Comfort Setting to Sleep
	LOG("sleep()", 5)
	setThermostatProgram("Sleep")
	return
}
*/
void asleep() {
	// Change the Comfort Setting to Sleep
	LOG('asleep()', 5, null, 'trace')
	setThermostatProgram('Sleep')
}
void nightDisabled() {}
void night() {
	// Change the Comfort Setting to Sleep
	LOG('night()', 5, null, 'trace')
	setThermostatProgram('Sleep')
}
void wakeup() { setThermostatProgram('Wakeup') }	// Not all thermostats have these two - setTP will validate
void awake() { setThermostatProgram('Awake') }
void resumeProgram(resumeAll=true) {
	resumeProgramInternal(resumeAll)
}
def resumeProgramInternal(resumeAll=true) {
	def result = true
	String thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true)
	if ((thermostatHold == null) || (thermostatHold == '') || (thermostatHold == 'null')) {
		LOG('resumeProgram() - No current hold', 2, null, 'info')
		if (isST) sendEvent(name: 'resumeProgram', value: 'resume dis', descriptionText: 'resumeProgram is done', displayed: false, isStateChange: true)
		return result
	} else if (thermostatHold =='vacation') { // this shouldn't happen anymore - button changes to Cancel when in Vacation mode
		LOG('resumeProgram() - Cannot resume from Vacation hold', 2, null, 'error')
		if (isST) sendEvent(name: 'resumeProgram', value: 'cancel', descriptionText: 'resumeProgram is done', displayed: false, isStateChange: true)
		return false
	} else {
		LOG("resumeProgram() - Hold type is ${thermostatHold}", 4)
	}

	sendEvent(name: 'thermostatStatus', value: 'Resuming scheduled Program...', displayed: false, isStateChange: true)

	def deviceId = getDeviceId()
	sendEvent(name: 'thermostatStatus', value: 'Resuming Scheduled Program', displayed: false, isStateChange: true)
	if (parent.resumeProgram(this, deviceId, resumeAll)) {
		def updates = [ 'holdStatus': 'null', 'thermostatHold' : 'null', 'holdEndsAt' : 'null' ]
		generateEvent(updates)
		if (isST) sendEvent(name: "resumeProgram", value: "resume dis", descriptionText: "resumeProgram is done", displayed: false, isStateChange: true)
		sendEvent(name: 'thermostatStatus', value: 'Resume Program succeeded', displayed: false, isStateChange: true)
		LOG("resumeProgram(${resumeAll}) - succeeded", 2,null,'info')
		runIn(5, refresh, [overwrite:true])
	} else {
		sendEvent(name: "thermostatStatus", value: "Resume Program failed...", displayed: false, isStateChange: true)
		LOG("resumeProgram() - failed (parent.resumeProgram(this, ${deviceId}, ${resumeAll}))", 1, null, "error")
		result = false
	}
	// sendEvent(name: 'thermostatStatus', value: '', displayed: false, isStateChange: true)
	return result
}
void generateProgramEvent(String program, String failedProgram='') {
	LOG("Generate generateProgramEvent Event: program ${program}", 4)

	sendEvent(name: "thermostatStatus", value: "Setpoint updating...", displayed: false, isStateChange: true)

	String prog = program.capitalize()
	String progHold = (failedProgram == '') ? prog : "Hold: "+prog
    // Quickly update the key hold status variables, even before we hear back from the thermostat/cloud
	// We have to wait for wait until Ecobee sends us the correct currentProgramId
	def updates = ['currentProgramName':progHold, 'currentProgram':prog]
    if (failedProgram == '') {
    	updates += ['thermostatHold':'null', 'holdEndsAt':'null', 'holdStatus':'null']
    } else {
    	updates += ['thermostatHold':'hold']
    }
	generateEvent(updates)
	if (isST) updateProgramButtons()
}

// ***************************************************************************
// Thermostat Fan Mode Commands
// Fan Mode CANNOT be changed while in Vacation mode
// ***************************************************************************
void setThermostatFanMode(String value, holdType=null, holdHours=2) {
	// N.B. if holdType is provided, it must be one of the valid parameters for the Ecobee setHold call (indefinite, nextTransition, holdHours). dateTime not currently supported
	LOG("setThermostatFanMode(${value})", 4)
	// "auto" "on" "circulate" "off"

	// Cannot change fan settings while in Vacation mode
	def thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true) 
	if (thermostatHold == 'vacation') {
		LOG("setThermostatFanMode(${value}, ${holdType}) fan mode change requested but thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
	String setValue = value
	// This is to work around a bug in some SmartApps that are using fanOn and fanAuto as inputs here, which is wrong
	if (value == "fanOn" || value == "on" ) { value = 'on'; setValue = "on" }
	else if (value == "fanAuto" || value == "auto" ) { value = 'auto'; setValue = "auto" }
	else if (value == "fanCirculate" || value == "circulate")  { value = 'circulate'; setValue == "circulate" }	// Ecobees don't have a 'circulate' mode, per se, uses fanMinOnTime
	else if (value == "fanOff" || value == "off") { value = 'off'; setValue = "auto" }
	else {
		LOG("setThermostatFanMode() - Unrecognized Fan Mode: ${value}. Setting to 'auto'", 1, null, "error")
		setValue = "auto"
	}

	String currentProgramName 
	String currentThermostatHold
    if (isST) {
    	currentProgramName = device.currentValue('currentProgramName')
    	currrentThermostatHold = device.currentValue('thermostatHold')
    } else {
    	currentProgramName = device.currentValue('currentProgramName', true)
    	currrentThermostatHold = device.currentValue('thermostatHold', true)
    }
    
	if (((currentThermostatHold != '') && (currentThermostatHold != null) && (currentThermostatHold != 'null')) || currentProgramName.startsWith('Hold') || currentProgramName.startsWith('Auto ')) {
		resumeProgramInternal(true)
		// refresh()
	}

	String currentFanMode = isST ? device.currentValue('thermostatFanMode') : device.currentValue('thermostatFanMode', true)	// value AFTER we reset the Hold (should really read the scheduledProgram's fan values)
	def updates = []
	def results
	def nullMinOnTime = null

	def sendHoldType = null
	def sendHoldHours = null
	if (holdType) {
		sendHoldType = holdType
		if (holdType == 'holdHours') sendHoldHours = holdHours
	} else {
		sendHoldType =	whatHoldType()
		if ((sendHoldType != null) && sendHoldType.toString().isNumber()) {
			sendHoldHours = sendHoldType
			sendHoldType = 'holdHours'
		}
	}

	switch (value) {
		case 'on':
			results = false
			if (currentFanMode != 'on') results = parent.setFanMode(this, 'on', nullMinOnTime, getDeviceId(), sendHoldType, sendHoldHours)
			if (results) {
				// pre-load the values that will (eventually) be sent back from the thermostat
				updates = [thermostatFanMode:'on',thermostatOperatingState:'fan only',equipmentOperatingState:'fan only' /*,currentProgramName:'Hold: Fan'*/]
				if ((currentFanMode != 'on') && (currentFanMode != 'off')) {	// i.e. if 'auto' or 'circulate', then turning on will cause a Hold: Fan event
					updates += [currentProgramName:'Hold: Fan']
				} else {
					updates += isST ? [currentProgramName:device.currentValue('scheduledProgramName')] : [currentProgramName:device.currentValue('scheduledProgramName', true)]	// In case currentProgram wasn't updated yet by the above resumeProgram()
				}
				generateEvent(updates)
			}
			break;

		case 'auto':
			// Note: we now set fanMinOntime to 0 when changing to Auto. "Circulate" will set it to non-zero number
			if (currentFanMode != 'auto') {
				results = parent.setFanMode(this, 'auto', 0, getDeviceId(), sendHoldType, sendHoldHours)
			} else {
				results = parent.setFanMinOnTime(this, deviceId, 0)
			}
			if (results) {
				updates = [fanMinOnTime:0,thermostatFanMode:'auto']
				log.warn "${updates}"
				generateEvent(updates)
			} else {
				updates = [thermostatFanMode:"${currentFanMode}"]
				log.warn "${updates}"
				generateEvent(updates)
			}
			break;

		case 'off':
			// To turn the fan OFF, HVACMode must be 'off', and Fan Mode needs to be 'auto' mode with fanMinOnTime = 0
			def thermostatMode = isST ? device.currentValue('thermostatMode') : device.currentValue('thermostatMode', true) 

			if (thermostatMode != 'off') {
				LOG("Request to change Fan Mode to 'off' while Thermostat Mode isn't 'off' (${thermostatMode}), Fan Mode set to 'auto' instead",2,null,'warn')
				updates = isST ? [thermostatFanMode:device.currentValue('thermostatFanMode')] : [thermostatFanMode:device.currentValue('thermostatFanMode', true)]
				generateEvent(updates)
				return
			}
			if (currentFanMode != 'off') {
				// this also sets fanMinOnTime to zero
				if (parent.setFanMode(this, 'off', 0, getDeviceId(), sendHoldType, sendHoldHours)) {
					updates = [thermostatFanMode:'off',fanMinOnTime:0]
					generateEvent(updates)
				}
			}
			break;

		case 'circulate':
			// For Ecobee thernmostats, 'circulate' will be active any time fanMinOnTime != 0 and Fan Mode == 'auto'
			// For this implementation, we will use 'circulate' whenever fanMinOnTime != 0, and 'off' when fanMinOnTime == 0 && thermostatMode == 'off'
			def fanMinOnTime = isST ? device.currentValue('fanMinOnTime') : device.currentValue('fanMinOnTime', true)
			def fanTime = fanMinOnTime
			if (!fanMinOnTime || (fanMinOnTime.toInteger() == 0)) {
				// 20 minutes/hour is roughly the default for HoneyWell thermostats (35%), upon which ST has modeled their thermostat implementation
				fanTime = 20
			}
			if ((currentFanMode == 'auto') || (currentFanMode == 'circulate')) {
				if (fanTime != fanMinOnTime) {
					results = parent.setFanMinOnTime(this, deviceId, fanTime)
				} else results = true
			} else {
				results = parent.setFanMode(this, setValue, fanTime, getDeviceId(), sendHoldType, sendHoldHours)
			}
			if (results) {
				updates = [fanMinOnTime:fanTime,thermostatFanMode:'circulate',thermostatFanModeDisplay:'circulate']
				generateEvent(updates)
			} else {
				updates = [thermostatFanMode:currentFanMode,thermostatFanModeDisplay:currentFanMode]
				generateEvent(updates)
			}
			break;
	}
}

def ventModes() {
	return ["auto", "minontime", "off", "on"]
}
def fanModes() {
	return ["auto", "circulate", "off", "on"]
}
void fanOn() {
	LOG('fanOn()', 5, null, 'trace')
	setThermostatFanMode('on')
}
void fanAuto() {
	LOG('fanAuto()', 5, null, 'trace')
	setThermostatFanMode('auto')
}
void fanCirculate() {
	LOG('fanCirculate()', 5, null, 'trace')
	setThermostatFanMode('circulate')
}
void fanOffDisabled() {}
void fanOff() {
	LOG('fanOff()', 5, null, 'trace')
    ncTfm = isST ? device.currentValue('thermostatFanMode') : device.currentValue('thermostatFanMode', true)
	if (ncTfm != 'off') {
		setThermostatFanMode('off')
		generateQuickEvent('setFanOff', 'off')		// reset the button icon state
		disableModeOffButton()
	} else {
		LOG("Thermostat Fan Mode is 'off' (already)",2,null,'info')
		generateQuickEvent('setFanOff', 'off')		// reset the button icon state
	}
}

void setFanMinOnTimeDelay(minutes) {
	LOG("Slider requested Minutes: ${minutes}",4,null,'trace')
	def runWhen = (getParentSetting('arrowPause') ?: 4) as Integer
	runIn( runWhen, 'sFMOT', [data: [mins:minutes]] )
}
void sFMOT(data) {
	LOG("Setting fan minutes to: ${data.mins}",4,null,'trace')
	setFanMinOnTime(data.mins)
}
void setFanMinOnTime(minutes=20) {
	def thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true)
	if ( thernostatHold == 'vacation') {
		LOG("setFanMinOnTime() requested but thermostat is in Vacation mode, ignoring request",2,null,'warn')
		return
	}
	LOG("setFanMinOnTime(${minutes})", 4, null, "trace")
	Integer howLong = 20	// default to 10 minutes, if no value supplied
	if (minutes.toString().isNumber()) howLong = minutes as Integer
	def fanMinOnTime = isST ? device.currentValue('fanMinOnTime') : device.currentValue('fanMinOnTime', true)
	LOG("Current fanMinOnTime: ${fanMinOnTime}, requested ${minutes}/${howLong}",3,null,'info')
	if (fanMinOnTime && (fanMinOnTime.toInteger() == howLong)) return // allready there - all done!

	def deviceId = getDeviceId()
	if ((howLong >=0) && (howLong <=  55)) {
		if (parent.setFanMinOnTime(this, deviceId, howLong)) {
			def updates = [fanMinOnTime:howLong]
			def currentFanMode = isST ? device.currentValue('thermostatFanMode') : device.currentValue('thermostatFanMode', true)
			if ((howLong == 0) && ((currentFanMode == 'circulate') || currentFanMode == 'auto')) {
				updates = updates + [thermostatFanMode:'auto',thermostatFanModeDisplay:'auto']
			} else if ((howLong > 0) && (currentFanMode != 'circulate')) {
				updates = updates + [thermostatFanMode:'circulate',thermostatFanModeDisplay:'circulate']
			}
			generateEvent(updates)
		}
	} else {
		LOG("setFanMinOnTime(${minutes}) - invalid argument",1,null, 'error')
	}
}

// Humidifier/Dehumidifier Commands
void setHumidifierMode(String value) {
	// verify thermostat hasHumidifier first
	def hasHumidifier = device.currentValue('hasHumidifier')
	if (!hasHumidifier || (hasHumidifier == 'false')) {
		LOG("${device.displayName} is not controlling a humidifier", 1, null, 'warn')
		return
	}
	if (!value	|| ((value != 'auto')  && (value != 'off') && (value != 'manual') && (value != 'on'))) {
		LOG("setHumidifierMode(${value}) - unsupported humidifier mode requested")
		return
	}
	if (value == 'on') value = 'manual'
	String humidifierMode = isST ? device.currentValue('humidifierMode') : device.currentValue('humidifierMode', true)
	if (humidifierMode == value) {
		LOG("${device.displayName}'s humidifier is already set to ${value}", 2, null, 'info')
		return
	}
	def result = parent.setHumidifierMode(this, value, getDeviceId())
	if (result) {
		def updates = [humidifierMode: value]
		generateEvent(updates)
		LOG("${device.displayName}'s humidifier is now set to ${value}", 2, null, 'info')
		// generate events to update the device's display here (if any)
	} else {
		humidifierMode = isST ? device.currentValue('humidifierMode') : device.currentValue('humidifierMode', true)
		LOG("Changing ${device.displayName}'s humidifier to ${value} failed, humidifier is still ${humidifierMode}", 1, null, 'warn')
	}
}

void setHumiditySetpoint(setpoint) {
	LOG("Humidity setpoint change to ${setpoint} requested", 2, null, 'trace')
	def dehumSP = isST ? device.currentValue('dehumiditySetpoint') : device.currentValue('dehumiditySetpoint', true)
	if ((dehumSP != null) && dehumSP.toString().isNumber() && (dehumSP < setpoint)) LOG('Request to set Humidify Setpoint higher than Dehumidify Setpoint',1,null,'warn')
	def runWhen = (getParentSetting('arrowPause') ?: 4) as Integer
	runIn( runWhen, 'sHumSP', [data: [setpoint:setpoint]] )
}
void sHumSP(data) {
	LOG("Setting humidity setpoint to: ${data.setpoint}",4,null,'trace')
	setHumiditySetpointDelay(data.setpoint)
}
void setHumiditySetpointDelay(setpoint) {
	LOG("setHumiditySetpointDelay ${setpoint}",4,null,'trace')
	// verify that the stat hasDehumidifer
	def hasHumidifier = device.currentValue('hasHumidifier')
	if (!hasHumidifier || (hasDehumidifier == 'false')) {
		LOG("${device.displayName} is not controlling a humidifier", 1, null, 'warn')
		return
	}
	// log.debug "${device.currentValue('humiditySetpoint')}"
	def currentSetpoint = isST ? device.currentValue('humiditySetpoint') : device.currentValue('humiditySetpoint', true)
	def humidifierMode  = isST ? device.currentValue('humidifierMode')   : device.currentValue('humidifierMode', true)
	if (humidifierMide == 'auto') {
		LOG("${device.displayName} is in Auto mode - cannot override Humidity setpoint", 1, null, 'warn')
		def updates = [forced: true, humiditySetpoint: currentSetpoint]
		generateEvent(updates)
		return
	}
	if (currentSetpoint == setpoint) {
		LOG("${device.displayName}'s humidifier setpoint is already set to ${setpoint}", 2, null, 'info')
		return
	}
	def result = parent.setHumiditySetpoint(this, setpoint, getDeviceId())
	if (result) {
		LOG("${device.displayName}'s humidifier setpoint is now set to ${setpoint}", 2, null, 'info')
		def updates = [humiditySetpoint: setpoint]
		generateEvent(updates)
	} else {
		def updates = [humiditySetpoint: currentSetpoint]
		generateEvent(updates)
		LOG("Changing ${device.displayName}'s humidifier setpoint to ${setpoint} failed, setpoint is still ${currentSetpoint}", 1, null, 'warn')
	}
}

void setDehumidifierMode(String value) {
	// verify that the stat hasDehumidifer
	def hasDehumidifier = device.currentValue('hasDehumidifier')
	if (!hasDehumidifier || (hasDehumidifier == 'false')) {
		LOG("${deviceId.displayName} is not controlling a dehumidifier or overcooling", 1, child, 'warn')
		return
	}

	if (!value || (value != 'off') && (value != 'on')) {
		LOG("setHumidifierMode(${value}) - unsupported dehumidifier mode requested")
		return
	}
	def dehumidifierMode = isST ? device.currentValue('dehumidifierMode') : device.currentValue('dehumidifierMode', true)
	if (dehumidifierMode == value) {
		LOG("${device.displayName}'s dehumidifier is already set to ${value}", 2, null, 'info')
	}
	def result = parent.setDehumidifierMode(this, value, getDeviceId())
	if (result) {
		def updates = [dehumidifierMode: value]
		generateEvent(updates)
		LOG("${device.displayName}'s dehumidifier is now set to ${value}", 2, null, 'info')
		// generate events to update the device's display here (if any)
	} else {
		LOG("Changing ${device.displayName}'s dehumidifier to ${value} failed, dehumidifier is still ${dehumidifierMode}", 1, null, 'warn')
	}
}
void setDehumiditySetpoint(setpoint) {
	LOG("Dehumidity setpoint change to ${setpoint} requested", 2, null, 'trace')
	def humiditySetpoint = isST ? device.currentValue('humiditySetpoint') : device.currentValue('humiditySetpoint', true)
	if ((humiditySetpoint != null) && humiditySetpoint.toString().isNumber() && (humiditySetpoint > setpoint)) LOG('Request to set Dehumidify Setpoint lower than Humidify Setpoint',1,null,'warn')
	def runWhen = (getParentSetting('arrowPause') ?: 4) as Integer
	runIn( runWhen, 'sDehumSP', [data: [setpoint:setpoint]] )
}
void sDehumSP(data) {
	LOG("Setting dehumidity setpoint to: ${data.setpoint}",4,null,'trace')
	setDehumiditySetpointDelay(data.setpoint)
}
void setDehumiditySetpointDelay(setpoint) {
	LOG("setDehumiditySetpointDelay ${setpoint}",4,null,'trace')
	// verify that the stat hasDehumidifer
	def hasDehumidifier = device.currentValue('hasDehumidifier')
	if (!hasDehumidifier || (hasDehumidifier == 'false')) {
		LOG("${deviceId.displayName} is not controlling a dehumidifier nor is it overcooling", 1, child, 'warn')
		return
	}
	def currentSetpoint = isST ? device.currentValue('dehumiditySetpoint') : device.currentValue('dehumiditySetpoint', true)
	if (currentSetpoint == setpoint) {
		LOG("${device.displayName}'s dehumidifier setpoint is already set to ${setpoint}", 2, null, 'info')
		return
	}
	def result = parent.setDehumiditySetpoint(this, setpoint, getDeviceId())
	if (result) {
		def updates = [dehumiditySetpoint: setpoint]
		generateEvent(updates)
		LOG("${device.displayName}'s dehumidifier setpoint is now set to ${setpoint}", 2, null, 'info')
		// generate events to update the device's display here (if any)
	} else {
		LOG("Changing ${device.displayName}'s dehumidifier setpoint to ${setpoint} failed, setpoint is still ${currentSetpoint}", 1, null, 'warn')
	}
}

// Vacation commands
void setVacationFanMinOnTime(Integer minutes=0) {
	def thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true)
	if ( thernostatHold == 'vacation') {
		LOG("setVacationFanMinOnTime() requested but thermostat is not in Vacation mode, ignoring request",2,null,'warn')
		return
	}
	LOG("setVacationFanMinOnTime(${minutes})", 5, null, "trace")
	Integer howLong = 0	// default to 0 minutes for vacations, if no value supplied
	// if (minutes.isNumber()) howLong = minutes.toInteger()
	howLong = minutes as Integer
	def fanMinOnTime = isST ? device.currentValue('fanMinOnTime') : device.currentValue('fanMinOnTime', true)
	LOG("Current fanMinOnTime: ${fanMinOnTime}, requested ${minutes}/${howLong}",3,null,'info')
	if (fanMinOnTime && (fanMinOnTime.toInteger() == howLong)) return // allready there - all done!
	def deviceId = getDeviceId()
	if ((howLong >=0) && (howLong <=  55)) {
		if (parent.setVacationFanMinOnTime(this, deviceId, howLong)) {
			def updates = ['fanMinOnTime':howLong]
			generateEvent(updates)
		}
	} else {
		LOG("setVacationFanMinOnTime(${minutes}) - invalid argument",1,null, "error")
	}
}

// deleteVacation can delete any named vacation, or the currently running one (vacationName==null)
void deleteVacation(vacationName = null) {
	LOG("deleteVacation(${vacationName})", 5, null, "trace")
	if (vacationName == null) {
		cancelVacation()
		return
	}
	def deviceId = getDeviceId()
	parent.deleteVacation(this, deviceId, vacationName)
}

// cancelVacation will only cancel the currently running vacation
void cancelVacation() {
	LOG("cancelVacation()", 2, null, 'info')
	def thermostatHold = isST ? device.currentValue('thermostatHold') : device.currentValue('thermostatHold', true)
	if ( thernostatHold != 'vacation') {
		LOG("cancelVacation() - current hold is ${thermostatHold}",2,null,'trace')
		// Maybe we should do a resumeProgram first? But that could kill a Hold: if this is called while there is no vacation
		return
	}
	def deviceId = getDeviceId()
	if (parent.deleteVacation(this, deviceId, null)) {
		// call resumePogram
		generateQuickEvent('resumeProgram', 'resume dis', 5)
		generateQuickEvent('currentProgramName', (isST ? device.currentValue('scheduledProgramName') : device.currentValue('scheduledProgramName', true)))
		updateModeButtons()
	}
}

// Climate change commands
void setProgramSetpoints(programName, heatingSetpoint, coolingSetpoint) {
	def scale = getTemperatureScale()
	LOG("setProgramSetpoints( ${programName}, heatSP: ${heatingSetpoint}°${scale}, coolSP: ${coolingSetpoint}°${scale} )",2,null,'info')
	def deviceId = getDeviceId()
	if (parent.setProgramSetpoints( this, deviceId as String, programName as String, heatingSetpoint as String, coolingSetpoint as String)) {
    	String ncCp = isST ? device.currentValue('currentProgram') : device.currentValue('currentProgram', true)
		if ( ncCp == programName) { 
			def updates = []
			if (coolingSetpoint) updates << ['coolingSetpoint': coolingSetpoint]
			if (heatinfSetpoint) updates << ['heatingSetpoint': heatingSetpoint]
			if (updates != []) generateEvent(updates)
		}
		LOG("setProgramSetpoints() - completed",3,null,'trace')
	} else LOG("setProgramSetpoints() - failed",1,null,'warn')
}

void setEcobeeSetting( String name, value ) {
	def result
	def dItem = EcobeeDirectSettings.find{ it.name == name }
	if (dItem != null) {
		LOG("setEcobeeSetting( ${name}, ${value} ) - calling ${dItem.command}( ${value} )", 2, child, 'info')
		result = "${dItem.command}"(value)
	} else {
		String deviceId = getDeviceId()
		result = parent.setEcobeeSetting( this, deviceId, name, value as String)
	}
	if (result == true){
		LOG("setEcobeeSetting( ${name}, ${value} ) completed.", 2, null, 'info')
	} else {
		LOG("setEcobeeSetting( ${name}, ${value} ) FAILED.", 1, null, 'warn')
	}
}

// No longer used as of v1.2.21
void generateSetpointEvent() {
	LOG('generateSetpointEvent called in error',1,null,'error')
}

// This just updates the generic multiAttributeTile - text should match the Thermostat mAT
void generateStatusEvent() {
	LOG('generateStatusEvent called in error',1,null,'warn')
	// no longer needed, alternate multiAttributeTile no longer necessary
}

// generate custom mobile activity feeds event
// (Need to clean this up to remove as many characters as possible, else it isn't readable in the Mobile App
void generateActivityFeedsEvent(notificationMessage) {
	sendEvent(name: "notificationMessage", value: "${device.displayName} ${notificationMessage}", descriptionText: "${device.displayName} ${notificationMessage}", displayed: true, isStateChange: true)
}

void noOp(arg=null) {
	// Doesn't do anything. Here due to a formatting issue on the Tiles!
}

def getSliderRange() {
	// should be returning the attributes heatRange and coolRange (once they are populated), but you can't get access to those while the forms are created (even after running for days).
	// return wantMetric() ? "(5..35)" : "(45..95)"
	return "(5..90)"
}

// Built in functions from SmartThings
// getTemperatureScale()
// fahrenheitToCelsius()
// celsiusToFahrenheit()

def wantMetric() {
	return (getTemperatureScale() == "C")
}

def cToF(temp) {
	return celsiusToFahrenheit(temp)
}
def fToC(temp) {
	return fahrenheitToCelsius(temp)
}

def roundC(value) {
	// return (((value.toDouble() * 2.0).toDouble().round(0)) / 2.0).toDouble().round(1)
	return roundIt((roundIt((value * 2.0), 0) / 2.0), 1)
}

def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP)
}
def roundIt( BigDecimal value, decimals=0) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP)
}

// def getImageURLRoot() {
//	return "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/dark/"
//}

String getDeviceId() {
	def deviceId = device.deviceNetworkId.split(/\./).last()
	LOG("getDeviceId() returning ${deviceId}", 4)
	return deviceId
}

boolean usingSmartAuto() {
	LOG("Entered usingSmartAuto() ", 5)
	if (settings.smartAuto) { return settings.smartAuto }
	if (getParentSetting('smartAuto')) return true	// { return parent.settings.smartAuto }
	return false
}

// returns the holdType keyword, OR the number of hours to hold
// precedence: 1. device preferences, 2. parent settings.holdType, 3. indefinite (must specify to use the thermostat setting)
def whatHoldType() {
	def theHoldType = settings.myHoldType
	def sendHoldType = null
	def parentHoldType
	switch (myHoldType) {
		case 'Temporary':
			sendHoldType = 'nextTransition'
			break;
		case 'Permanent':
			sendHoldType = 'indefinite'
			break;
		case 'Hourly':
			parentHoldType = getParentSetting('holdType')
			if (settings.myHoldHours) {
				sendHoldType = myHoldHours as Integer
			} else if ((parentHoldType != null) && (parentHoldType == 'Specified Hours') && (getParentSetting('holdHours') != null)) {
				sendHoldType = getParentSettings('holdHours')
			} else if ( parentHoldType == '2 Hours') {
				sendHoldType = 2
			} else if ( parentHoldType == '4 Hours') {
				sendHoldType = 4
			} else {
				sendHoldType = 2
			}
			break;
		case null :		// null means use parent value
			theHoldType = 'Parent'
		case 'Parent':
			parentHoldType = getParentSetting('holdType')
			if ((parentHoldType != null) && (parentHoldType == 'Thermostat Setting')) theHoldType = 'Thermostat'
		case 'Thermostat':
			if (theHoldType == 'Thermostat') {
				def holdType = isST ? device.currentValue('statHoldAction') : device.currentValue('statHoldAction', true)
				switch(holdType) {
					case 'useEndTime4hour':
						sendHoldType = 4
						break;
					case 'useEndTime2hour':
						sendHoldType = 2
						break;
					case 'nextPeriod':
					case 'nextTransition':
						sendHoldType = 'nextTransition'
						break;
					case 'indefinite':
					case 'askMe':
					case null :
					default :
						sendHoldType = 'indefinite'
						break;
				}
			} else {		// assert (theHoldType == Parent)
				switch (parentHoldType) {
					case 'Until I Change':
						sendHoldType = 'indefinite'
						break;
					case 'Until Next Program':
						sendHoldType = 'nextTransition'
						break;
					case '2 Hours':
						sendHoldType = 2
						break;
					case '4 Hours':
						sendHoldType = 4
						break;
					case 'Specified Hours':
						sendHoldType = getParentSetting('holdHours')?: 2
						break;
					case null :		// if not set in Parent, should probably use Thermostat setting, but precendence was set that null = indefinite
					default :
						sendHoldType = 'indefinite'
						break;
				}
			}
	}
	if (sendHoldType) {
		LOG("Using holdType ${sendHoldType.isNumber()?'holdHours ('+sendHoldType.toString()+')':sendHoldType}",2,null,'info')
		return sendHoldType
	} else {
		LOG("Couldn't determine holdType, returning indefinite",1,null,'error')
		return 'indefinite'
	}
}

void makeReservation(String childId, String type='modeOff') {
	LOG("The thermostat-based Reservation System has been deprecated - please recode for Ecobee Suite Manager-based implementation.",1,null,'error')
	if (device.currentValue('reservations')) sendEvent(name: 'reservations', value: null, displayed: false)
}

void cancelReservation( String childId, String type='modeOff') {
	LOG("The thermostat-based Reservation System has been deprecated - please recode for Ecobee Suite Manager-based implementation.",1,null,'error')
	if (device.currentValue('reservations')) sendEvent(name: 'reservations', value: null, displayed: false)
}

Integer howManyHours() {
	Integer sendHoldHours = 2
	if ((holdHours != null) && holdHours.toString().isNumber()) {
		sendHoldHours = holdHours.toInteger()
		LOG("Using ${device.displayName} holdHours: ${sendHoldHours}",2,this,'info')
	} else {
		def hh = getParentSetting('holdHours')
		if ((hh != null) && hh.toString().isNumber()) {
			sendHoldHours = hh as Integer
			LOG("Using ${parent.displayName} holdHours: ${sendHoldHours}",2,this,'info')
		}
	}
	return sendHoldHours
}

boolean debugLevel(level=3) {
	Integer dbg = device.currentValue('debugLevel')?.toInteger()
	Integer debugLvlNum = (dbg ?: (getParentSetting('debugLevel') ?: level)) as Integer
	return ( debugLvlNum >= level?.toInteger() )
}

void LOG(message, level=3, child=null, logType="debug", event=false, displayEvent=false) {
	def prefix = debugLevel(5) ? 'LOG: ' : ''
	if (logType == null) logType = 'debug'

	if (debugLevel(level)) {
		log."${logType}" "${prefix}${message}"
		if (event) { debugEvent(message, displayEvent) }
	}
}

void debugEvent(message, displayEvent = false) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent,
		isStateChange: true
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent (results)
}

def getTempColors() {
	def colorMap

	colorMap = [
/*		// Celsius Color Range
		[value: 0, color: "#1e9cbb"],
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
	return colorMap
}

def getStockTempColors() {
	def colorMap

	colorMap = [
		[value: 31, color: "#153591"],
		[value: 44, color: "#1e9cbb"],
		[value: 59, color: "#90d2a7"],
		[value: 74, color: "#44b621"],
		[value: 84, color: "#f1d801"],
		[value: 95, color: "#d04e00"],
		[value: 96, color: "#bc2323"]
	]
    return colorMap
}
// Ecobee Settings that must be changed only by specific commands
@Field final List EcobeeDirectSettings= [
											[name: 'fanMinOnTime',			command: 'setFanMinOnTime'],
											[name: 'dehumidifierMode',		command: 'setDehumidifierMode'],
                                            [name: 'dehumidiferLevel',		command: 'setDehumiditySetpoint'],
											[name: 'dehumidityLevel', 		command: 'setDehumiditySetpoint'],
											[name: 'dehumiditySetpoint',	command: 'setDehumiditySetpoint'],
											[name: 'humidifierMode',		command: 'setHumidifierMode'],
											[name: 'humiditySetpoint',		command: 'setHumiditySetpoint'],
											[name: 'schedule',				command: 'setSchedule']
										]

// **************************************************************************************************************************
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
//
// The following 3 calls are safe to use anywhere within a Device Handler or Application
//	- these can be called (e.g., if (getPlatform() == 'SmartThings'), or referenced (i.e., if (platform == 'Hubitat') )
//	- performance of the non-native platform is horrendous, so it is best to use these only in the metadata{} section of a
//	  Device Handler or Application
//
//	1.0.0	Initial Release
//	1.0.1	Use state so that it is universal
//
String	getPlatform() { return (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
boolean getIsST()	  { return (state?.isST != null) ? state.isST : (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
boolean getIsHE()	  { return (state?.isHE != null) ? state.isHE : (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
//
// The following 3 calls are ONLY for use within the Device Handler or Application runtime
//	- they will throw an error at compile time if used within metadata, usually complaining that "state" is not defined
//	- getHubPlatform() ***MUST*** be called from the installed() method, then use "state.hubPlatform" elsewhere
//	- "if (state.isST)" is more efficient than "if (isSTHub)"
//
String getHubPlatform() {
	def pf = getPlatform()
	state?.hubPlatform = pf			// if (state.hubPlatform == 'Hubitat') ...
											// or if (state.hubPlatform == 'SmartThings')...
	state?.isST = pf.startsWith('S')	// if (state.isST) ...
	state?.isHE = pf.startsWith('H')	// if (state.isHE) ...
	return pf
}
boolean getIsSTHub() { return state.isST }					// if (isSTHub) ...
boolean getIsHEHub() { return state.isHE }					// if (isHEHub) ...

def getParentSetting(String settingName) {
	// def ST = (state?.isST != null) ? state?.isST : isST
	return isST ? parent?.settings?."${settingName}" : parent?."${settingName}"
}
//
// **************************************************************************************************************************
