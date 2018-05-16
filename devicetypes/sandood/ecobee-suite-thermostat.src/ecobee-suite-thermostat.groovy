/**
 *  Based on original version Copyright 2015 SmartThings
 *  Additions Copyright 2016 Sean Kendall Schneyer
 *  Additions Copyright 2017-2018 Barry A. Burke
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
 *	Ecobee Suite Thermostat
 *
 *	Author: SmartThings
 *	Date: 2013-06-13
 *
 * 	Updates by Sean Kendall Schneyer <smartthings@linuxbox.org>
 * 	Date: 2015-12-23
 *
 *	Updates by Barry A. Burke (storageanarchy@gmail.com)
 *	Date: 2017 - 2018
 *
 *  See Github Changelog for complete change history 
 *
 *	1.2.0  - Release of holdHours and thermostat holdAction support
 *	1.2.1  - Ensure Mode buttons are enabled properly (esp. after vacation hold ends)
 *	1.2.2  - Handle "Auto Away" same as "Hold: Away" (& Auto Home)
 *	1.2.3  - Added overcool to operating state display, optimized generateEvent() handling
 *	1.2.4  - Fix error in currentProgramName update
 *	1.2.5  - Reinstated default icon for the default Temperature tile
 *	1.2.6  - Fixed display of Mode & fanMode icons when selected; keep unsupported Modes disabled
 *	1.2.7  - Added Awake, Auto Home and Auto Away program icons, changed Vacation airplane to solid blue (for consistency)
 *	1.2.8  - Fixed changing setpoint, display of multi-stage heat/cool equipmentOperatingState
 *	1.2.9  - Added Wakeup as synonym for Awake
 *	1.2.10 - Repaired changing setpoints while in a Hold: or Auto Program
 *	1.2.11 - Fixed slider control to show "°" instead of "C"
 *	1.2.12 - Work around Google Home erroneously sending setpoint requests in C when stat is in F mode
 *	1.2.13 - Added commands for Wakeup & Awake climates (for Smart/SI thermostats)
 *	1.2.14 - Workaround for program settings
 *	1.2.15 - Fixed typo that caused program changes to fail when logging level > 3
 *	1.2.16 - Fixed another typo in setThermostatProgram
 *	1.2.17 - Fixed CtoF/FtoC conversions in setHeat/CoolingSetpoint()
 *	1.2.18 - Fixed typos in prior fix, added heatCoolMinDelta handling
 *	1.2.19 - Hard-coded thermostat commands entry points
 *	1.2.20 - Eliminate extraneous temp display between up/down arrows of multiAttributeTile
 *  1.2.21a- Fix non-temporary program changes
 *	1.3.0  - Major release: rename and move to "sandood" namespace
 *	1.4.0  - Major release: Renamed device files
 *	1.4.01 - Don't display apiConnected status changes in device log unless in Debug Level 4 or 5
 *	1.4.02 - Fixed display of fanMinOnTime in device log
 *	1.4.03 - Fixed race condition when ST Routine sets both heatingSetpoint() and coolingSetpoint() in rapid succession
 *	1.4.04 - Optimized setProgram to avoid "Hold: Away", resumeProgram, "Hold: Away" redundancies
 *	1.4.05 - Revision number correction
 *	1.4.07 - Remove erroneous "Hold: program" 
 *	1.4.08 - Cleanup around currentProgram & lastOpState
 *	1.4.09 - Fixed replaceAll calls ("", not '')
 *	1.4.10 - Fixed setpoint up/down arrows
 *	1.4.11 - Fixed "Fan Only" green
 *	1.4.12 - Better error messages around missing programsList
 *	1.4.13 - Initial support for setting humidifier/dehumidifier mode
 *	1.4.14 - Removed extra "inactiveLabel" and changed main() to main([])
 */

def getVersionNum() { return "1.4.13" }
private def getVersionLabel() { return "Ecobee Suite Thermostat, version ${getVersionNum()}" }
import groovy.json.JsonSlurper
 
metadata {
	definition (name: "Ecobee Suite Thermostat", namespace: "sandood", author: "Barry A. Burke (storageanarchy@gmail.com)") {
		capability "Actuator"
		capability "Thermostat"
        capability "Sensor"
		capability "Refresh"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		// capability "Presence Sensor"
        capability "Motion Sensor"
        
        // Extended Set of Thermostat Capabilities
        capability "Thermostat Cooling Setpoint"
		capability "Thermostat Fan Mode"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Mode"
		capability "Thermostat Operating State"
		capability "Thermostat Setpoint"   
        capability "Health Check"

		command "setTemperature"
        command "auxHeatOnly"

		command "generateEvent"
		command "raiseSetpoint"
		command "lowerSetpoint"
		command "resumeProgram"
        command "setHeatingSetpointDelay"
        command "setCoolingSetpointDelay"
		command "switchMode"
        
        command "setThermostatProgram"
        command "setThermostatMode"
        command "setThermostatFanMode"
        command "setFanMinOnTime"
        command "setFanMinOnTimeDelay"
        command	"setHumidifierMode"
        command "setDehumidifierMode"
        command "setVacationFanMinOnTime"
        command "deleteVacation"
        command "cancelVacation"
        command "home"
        command "present"

// Unfortunately we cannot overload the internal Java/Groovy definition of 'sleep()', and calling this will silently fail (actually, it does a
// "sleep(0)")
//		command "sleep"
        command "asleep"
        command "night"				// this is probably the appropriate SmartThings device command to call, matches ST mode
        command "away"
        command "wakeup"
        command "awake"
        
        command "off"				// redundant - should be predefined by the Thermostat capability
        command "auto"
        command "cool"
        command "heat"
        command "emergencyHeat"
        command "emergency"
        command "auxHeatOnly"
        
        command "fanOff"  			// Missing from the Thermostat standard capability set
        command "fanAuto"
        command "fanCirculate"
        
        command "noOp" 				// Workaround for formatting issues 
        command "setStateVariable"
        command "doRefresh"			// internal use by the refresh button

		// Capability "Thermostat"
        attribute "temperatureScale", "string"
		attribute "thermostatSetpoint","number"
		attribute "thermostatStatus","string"
        attribute "apiConnected","string"
        attribute "ecobeeConnected", "string"
        
		attribute "currentProgramName", "string"
        attribute "currentProgramId","string"
		attribute "currentProgram","string"
		attribute "scheduledProgramName", "string"
        attribute "scheduledProgramId","string"
		attribute "scheduledProgram","string"
        attribute "weatherSymbol", "string"        
        attribute "debugEventFromParent","string"
        attribute "logo", "string"
        attribute "timeOfDay", "enum", ["day", "night"]
        attribute "lastPoll", "string"
        attribute "supportedThermostatModes", "JSON_OBJECT" // USAGE: List theModes = stat.currentValue('supportedThermostatModes')[1..-2].tokenize(", ")
        attribute "supportedThermostatFanModes", "JSON_OBJECT" // USAGE: List theFanModes = stat.currentValue('supportedThermostatFanModes')[1..-2].tokenize(", ")
		attribute "equipmentStatus", "string"
        attribute "humiditySetpoint", "string"
        attribute "dehumiditySetpoint", "string"
        attribute "weatherTemperature", "number"
		attribute "decimalPrecision", "number"
		attribute "temperatureDisplay", "string"
		attribute "equipmentOperatingState", "string"
        attribute "coolMode", "string"
		attribute "heatMode", "string"
        attribute "autoMode", "string"
		attribute "heatStages", "number"
		attribute "coolStages", "number"
		attribute "hasHeatPump", "string"
        attribute "hasForcedAir", "string"
        attribute "hasElectric", "string"
        attribute "hasBoiler", "string"
        attribute "hasHumidifier", "string"
        attribute "hasDehumidifier", "string"
        // attribute "dehumidifyOvercoolOffset", "number"
        attribute "humidifierMode", "string"
        attribute "dehumidifierMode", "string"
		attribute "auxHeatMode", "string"
        attribute "motion", "string"
		attribute "heatRangeHigh", "number"
		attribute "heatRangeLow", "number"
		attribute "coolRangeHigh", "number"
		attribute "coolRangeLow", "number"
        attribute "heatingSetpointRange", "vector3"
        attribute "heatingSetpointMin", "number"
        attribute "heatingSetpointMax", "number"
        attribute "coolingSetpointRange", "vector3"
        attribute "coolingSetpointMin", "number"
        attribute "coolingSetpointMax", "number"
        attribute "thermostatSetpointRange", "vector3"
        attribute "thermostatSetpointMin", "number"
        attribute "thermostatSetpointMax", "number"
        attribute "heatAtSetpoint", "number"
        attribute "coolAtSetpoint", "number"
        attribute "heatingSetpointDisplay", "number"	// for the Sliders (they don't math.round for their integer displays
        attribute "coolingSetpointDisplay", "number"
        // attribute "newHeatSetpoint", "number"
        // attribute "newCoolSetpoint", "number"
		attribute "heatRange", "string"
		attribute "coolRange", "string"
		attribute "thermostatHold", "string"
		attribute "holdStatus", "string"
        attribute "heatDifferential", "number"
        attribute "coolDifferential", "number"
        attribute "heatCoolMinDelta", "number"
        attribute "fanMinOnTime", "number"
        attribute "programsList", "string"				// USAGE: List programs = new JsonSlurper().parseText(stat.currentValue('programsList'))
        attribute "thermostatOperatingStateDisplay", "string"
        attribute "thermostatFanModeDisplay", "string"
        attribute "thermostatTime", "string"
        attribute "statHoldAction", "string"
        attribute "setpointDisplay", "string"
        attribute "lastHoldType", "string"
        attribute "lastOpState", "string"				// keeps track if we were most recently heating or cooling
		
		// attribute "debugLevel", "number"
		
        attribute "smart1", "string"
        attribute "smart2", "string"
        attribute "smart3", "string"
        attribute "smart4", "string"
        attribute "smart5", "string"
        attribute "smart6", "string"
        attribute "smart7", "string"
        attribute "smart8", "string"
        attribute "smart9", "string"
        attribute "smart10", "string"
	}

	simulator { }

    tiles(scale: 2) {      
              
		multiAttributeTile(name:"temperatureDisplay", type:"thermostat", width:6, height:4, canChangeIcon: true) {
			tileAttribute("device.temperatureDisplay", key: "PRIMARY_CONTROL") {
				attributeState("default", label:'${currentValue}', unit:"dF", defaultState: true)
			}
			tileAttribute("device.thermostatSetpoint", key: "VALUE_CONTROL") {
            	attributeState("VALUE_UP", action: "raiseSetpoint")
        		attributeState("VALUE_DOWN", action: "lowerSetpoint")
//                attributeState("default", label: '${currentValue}°', defaultState:true)
//                attributeState("default", action: "setTemperature")
			}
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'${currentValue}%', unit:"Humidity", defaultState: true)
			}
			tileAttribute('device.thermostatOperatingStateDisplay', key: "OPERATING_STATE") {
				attributeState('idle', 						backgroundColor:"#d28de0")		// ecobee purple/magenta
                attributeState('fan only', 					backgroundColor:"#66cc00")		// ecobee green
				attributeState('heating', 					backgroundColor:"#ff9c14")		// ecobee flame orange
				attributeState('cooling', 					backgroundColor:"#2db9e7")		// ecobee snowflake blue
                attributeState('heating (smart recovery)', 	backgroundColor:"#ff9c14")		// ecobee flame orange
                attributeState('cooling (smart recovery)', 	backgroundColor:"#2db9e7")		// ecobee snowflake blue
                attributeState('cooling (overcool)', 		backgroundColor:"#2db9e7") 
                attributeState('offline', 					backgroundColor:"#ff4d4d")
                attributeState('off', 						backGroundColor:"#cccccc")		// grey
                attributeState('default', 		 			backgroundColor:"#d28de0", defaultState: true) 
			}
			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", label:'${name}')
				attributeState("heat", label:'${name}')
				attributeState("cool", label:'${name}')
                attributeState("auto", label:'${name}')
                attributeState('default', label:'${name}', defaultState: true)
			}
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
            	attributeState("default", label:'${currentValue}°', unit:"dF", defaultState: true)
            }
			tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
				attributeState("default", label:'${currentValue}°', unit:"dF", defaultState: true)
			}
		} // End multiAttributeTile
        
        // Workaround until they fix the Thermostat multiAttributeTile. Only use this one OR the above one, not both
/*        multiAttributeTile(name:"summary", type: "lighting", width: 6, height: 4) {
        	tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", label:'${currentValue}°', unit:"dF",
				backgroundColors: getTempColors(), defaultState: true)
			}
			tileAttribute("device.nothing", key: "VALUE_CONTROL") {
            	attributeState("VALUE_UP", action: "raiseSetpoint")
        		attributeState("VALUE_DOWN", action: "lowerSetpoint")
//                attributeState("default", action: "setTemperature")
			}
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'${currentValue}%', unit:"%", defaultState: true)
			}
			tileAttribute("device.thermostatOperatingStateDisplay", key: "OPERATING_STATE") {
				attributeState("idle", backgroundColor:"#d28de0")			// ecobee purple/magenta
                attributeState("fan only", backgroundColor:"#66cc00")		// ecobee green
				attributeState("heating", backgroundColor:"#ff9c14")		// ecobee snowflake blue
				attributeState("cooling", backgroundColor:"#2db9e7")		// ecobee flame orange
                attributeState('heating (smart recovery)', backgroundColor:"#ff9c14")		// ecobee flame orange
                attributeState('cooling (smart recovery)', backgroundColor:"#2db9e7")		// ecobee snowflake blue
                attributeState('cooling (overcool)', backgroundColor:"#2db9e7") 
                attributeState('offline', backgroundColor:"#ff4d4d")		// red
                attributeState('off', backGroundColor:"#cccccc")			// grey
                attributeState('default', label: 'idle', backgroundColor:"#d28de0", defaultState: true)
			}
			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", label:'${name}')
				attributeState("heat", label:'${name}')
				attributeState("cool", label:'${name}')
                attributeState("auto", label:'${name}')
			}
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
            	attributeState("default", label:'${currentValue}°', unit:"dF", defaultState: true)
            }
			tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
				attributeState("default", label:'${currentValue}°', unit:"dF", defaultState: true)
			}
        }
*/

        // Show status of the API Connection for the Thermostat
		standardTile("apiStatus", "device.apiConnected", width: 1, height: 1) {
        	state "full", label: "FULL", backgroundColor: "#44b621", icon: "st.contact.contact.closed"
            state "warn", label: "WARN", backgroundColor: "#e86d13", icon: "st.contact.contact.open"
            state "lost", label: "LOST", backgroundColor: "#bc2323", icon: "st.contact.contact.open"
		}

		valueTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: true, decoration: 'flat') {
        	// Use the first version below to show Temperature in Device History - will also show Large Temperature when device is default for a room
            // 		The second version will show icon in device lists
			//state("default", label:'${currentValue}°', unit:"F", backgroundColors: getTempColors(), defaultState: true)
            state("default", label:'${currentValue}°', unit:"F", backgroundColors: getTempColors(), defaultState: true, icon: 'st.Weather.weather2')
		}
        
        // these are here just to get the colored icons to diplay in the Recently log in the Mobile App
        valueTile("heatingSetpointColor", "device.heatingSetpointDisplay", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("heatingSetpoint", label:'${currentValue}°', unit:"F", backgroundColor:"#ff9c14", defaultState: true)
		}
        valueTile("coolingSetpointColor", "device.coolingSetpointDisplay", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("coolingSetpoint", label:'${currentValue}°', unit:"F", backgroundColor:"#2db9e7", defaultState: true)
		}
        valueTile("thermostatSetpointColor", "device.thermostatSetpoint", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("thermostatSetpoint", label:'${currentValue}°', unit:"F",	backgroundColors: getTempColors(), defaultState: true)
		}
        valueTile("weatherTempColor", "device.weatherTemperature", width: 2, height: 2, canChangeIcon: false, decoration: "flat") {
			state("weatherTemperature", label:'${currentValue}°', unit:"F",	backgroundColors: getStockTempColors(), defaultState: true)		// use Fahrenheit scale so that outdoor temps register
		}
        valueTile("fanMinOnTimeColor", "device.fanMinOnTime", width: 2, height: 2, decoration: "flat") {
        	state("fanMinOnTime", label: '${currentValue}′', backgroundColor: "#808080", defaultState: true)
        }
		
		standardTile("mode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
			state "off", action:"heat", label: "Mode: Off", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_label.png"
			state "heat", action:"cool",  label: "Mode: Heat", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_heat.png"
			state "cool", action:"auto",  label: "Mode: Cool", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_cool.png"
			state "auto", action:"off",  label: "Mode: Auto", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_auto.png"
            // Not included in the button loop, but if already in "auxHeatOnly" pressing button will go to "auto"
			state "auxHeatOnly", action:"auto", label: "Mode: Aux Heat", icon: "st.thermostat.emergency-heat"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
        
        standardTile("modeShow", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
			state "off", 			action:"noOp", 	label: "Off",	nextState: "off",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_label.png"
			state "heat", 			action:"noOp",  label: "Heat", 	nextState: "heat", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_heat.png"
			state "cool", 			action:"noOp",  label: "Cool", 	nextState: "cool", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_cool.png"
			state "auto", 			action:"noOp",  label: "Auto", 	nextState: "auto", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_auto.png"
            // Not included in the button loop, but if already in "auxHeatOnly" pressing button will go to "auto"
			state "auxHeatOnly", 	action:"noOp",	label: 'Aux Heat',  				icon: "st.thermostat.emergency-heat"
            state "emergency",		action:"noOp",	label: 'Emergency', 				icon: "st.thermostat.emergency-heat"
            state "emergencyHeat",	action:"noOp",	label: 'Emergency Heat', 			icon: "st.thermostat.emergency-heat"
			state "updating", 						label: 'Working', 					icon: "st.motion.motion.inactive"
		}
        
		standardTile("setModeHeat", "device.setModeHeat", width: 2, height: 2, decoration: "flat") {			
			state "heat", action:"heat",  label: "Heat", nextState: "updating", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_heat.png"
            state "heat dis", action:"noOp",  label: "Heat", nextState: "heat dis", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_heat_grey.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
		standardTile("setModeCool", "device.setModeCool", width: 2, height: 2, decoration: "flat") {			
			state "cool", action:"cool",  label: "Cool", nextState: "updating", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_cool.png"
            state "cool dis", action:"noOp",  label: "Cool", nextState: "cool dis", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_cool_grey.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}        
		standardTile("setModeAuto", "device.setModeAuto", width: 2, height: 2, decoration: "flat") {			
			state "auto", action:"auto",  label: "Auto", nextState: "updating", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_auto.png"
			state "auto dis", action:"noOp",  label: "Auto", nextState: "auto dis", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_auto_grey.png"
            state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
		standardTile("setModeOff", "device.setModeOff", width: 2, height: 2, decoration: "flat") {			
			state "off", action:"off", label: "HVAC Off", nextState: "updating", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_label.png"
            state "off dis", action:"noOp", label: "HVAC Off", nextState: "off dis", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_label_grey.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
        
        // This one is the one that will control the icon displayed in device Messages log - but we don't actually use it
        standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
			state "on", 		label:'Fan: On', 		action:"fanAuto", 	nextState: "updating",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
            state "auto", 		label:'Fan: Auto', 		action:"fanOn", 	nextState: "updating", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
            state "off", 		label:'Fan: Off', 		action:"fanAuto", 	nextState: "updating", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
			state "circulate",	label:'Fan: Circulate',	action:"fanAuto",	nextState: "updating", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1.png"
            state "updating", 	label:"Working", 													icon: "st.motion.motion.inactive"
		}

		standardTile("fanModeLabeled", "device.thermostatFanModeDisplay", width: 2, height: 2, decoration: "flat") {
			state "on", 			label:'Fan: On', 		action:"fanCirculate", 	nextState: "updating", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
            state "auto", 			label:'Fan: Auto', 		action:"fanOn", 		nextState: "updating", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
            state "off", 			label:'Fan: Off', 		action:"fanAuto", 		nextState: "updating", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
			state "circulate", 		label:'Fan: Circulate', action:"fanAuto", 		nextState: "updating", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1.png"
            state "on dis", 		label:'Fan: On', 		action:"noOp", 			nextState: "on dis", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_grey.png"
            state "auto dis", 		label:'Fan: Auto', 		action:"noOp", 			nextState: "auto dis", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_grey.png"
            state "off dis", 		label:'Fan: Off', 		action:"fanCirculate",	nextState: "circulate dis",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
			state "circulate dis", 	label:'Fan: Circulate',	action:"fanOff", 		nextState: "off dis", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1.png"
            state "updating", 		label:"Working", 															icon: "st.motion.motion.inactive"
		}
        
        standardTile("fanOffButton", "device.setFanOff", width: 2, height: 2, decoration: "flat") {
			state "off", label:"Fan Off", action:"fanOff", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
            state "off dis", label:"Fan Off", action:"fanOffDisabled", nextState: "off dis", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_grey.png"
            state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}

		standardTile("fanCirculate", "device.setFanCirculate", width: 2, height: 2, decoration: "flat") {
			state "circulate", label:"Fan Circulate", action:"fanCirculate", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_big_solid_nolabel.png"
            state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
        
		standardTile("fanModeCycler", "device.thermostatFanModeDisplay", width: 2, height: 2, decoration: "flat") {
			state "auto", 			action:"fanOn", 		label: "Fan On", 		nextState: "updating", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
            state "on", 			action:"fanCirculate",	label: "Fan Circulate",	nextState: "updating", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1.png"
            state "off", 			action:"fanAuto", 		label: "Fan Auto", 		nextState: "updating", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
			state "circulate", 		action:"fanAuto", 		label: "Fan Auto", 		nextState: "updating", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"           
            state "on dis", 		action:"noOp", 			label: "Fan Circulate", nextState: "on dis", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1_grey.png"
            state "auto dis", 		action:"noOp", 			label: "Fan On", 		nextState: "auto dis", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_grey.png"
            state "off dis", 		action:"noOp", 			label: "Fan Auto", 		nextState: "off dis", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_grey.png"
			state "circulate dis",	action:"noOp", 			label: "Fan Auto", 		nextState: "circulate dis",	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_grey.png"            
            state "updating", 								label:"Working", 									icon: "st.motion.motion.inactive"
		}
//        standardTile("fanModeAutoSlider", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
//        	state "on", action:"fanAuto", nextState: "auto", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/fanmode_auto_slider_off.png"
//            state "auto", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/fanmode_auto_slider_on.png"
//        }
//		standardTile("fanModeOnSlider", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
//        	state "auto", action:"fanOn", nextState: "auto", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/fanmode_on_slider_off.png"
//            state "on", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/fanmode_on_slider_on.png"
//        }
        
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
		controlTile("circSliderControl", "device.fanMinOnTime", "slider", height: 1, width: 4, inactiveLabel: false, range: "(0..55)" ) {
			state "setCircSetpoint", action:"setFanMinOnTimeDelay",  backgroundColor:"#aaaaaa", unit: '′', defaultState: true
		}
		controlTile("heatSliderControl", "device.heatingSetpointDisplay", "slider", height: 1, width: 4, inactiveLabel: false, range: getSliderRange() /* "(15..85)" */ ) {
			state "setHeatingSetpoint", action:"setHeatingSetpointDelay",  backgroundColor:"#ff9c14", unit: '°', defaultState: true
		}
		valueTile("heatingSetpoint", "device.heatingSetpoint", height: 1, width: 1, decoration: "flat") {
			state "heat", label:'${currentValue}°', defaultState: true//, unit:"F", backgroundColor:"#ff9c14"
		}
		controlTile("coolSliderControl", "device.coolingSetpointDisplay", "slider", height: 1, width: 4, inactiveLabel: false, range: getSliderRange() /* "(15..85)" */ ) {
			state "setCoolingSetpoint", action:"setCoolingSetpointDelay", backgroundColor:"#2db9e7", unit: '°', defaultState: true
		}
		valueTile("coolingSetpoint", "device.coolingSetpoint", width: 1, height: 1, decoration: "flat") {
			state "cool", label:'${currentValue}°', defaultState: true //, unit:"F", backgroundColor:"#2db9e7"
		}
		standardTile("refresh", "device.doRefresh", width: 2, height: 2,decoration: "flat") {
            state "refresh", action:"doRefresh", nextState: 'updating', label: "Refresh", defaultState: true, icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/ecobee_refresh_green.png"
            state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
        
        standardTile("resumeProgram", "device.resumeProgram", width: 2, height: 2, decoration: "flat") {
			state "resume", action:"resumeProgram", nextState: "updating", label:'Resume', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/action_resume_program.png"
            state "resume dis", action: 'noOp', nextState: 'resume dis', label: 'Resume', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/action_resume_program_grey.png"
            state "cancel", action:"cancelVacation", nextState: "updating", label:'Cancel', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_vacation_airplane_yellow.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
        
        standardTile("currentProgramIcon", "device.currentProgramName", height: 2, width: 2, decoration: "flat") {
			state "Home", 				action:"noOp", nextState:'Home', 				label: 'Home', 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png"
			state "Away", 				action:"noOp", nextState:'Away', 				label: 'Away', 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png"
            state "Sleep", 				action:"noOp", nextState:'Sleep', 				label: 'Sleep', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue.png"
            state "Awake", 				action:"noOp", nextState:'Awake', 				label: 'Awake', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake.png"
            state "Wakeup", 			action:"noOp", nextState:'Wakeup', 				label: 'Wakeup', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake.png"
            state "Awake", 				action:"noOp", nextState:'Awake', 				label: 'Awake', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake.png"
			state "Auto", 				action:"noOp", nextState:'Auto', 				label: 'Auto', 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
            state "Auto Away", 			action:"noOp", nextState:'Auto Away', 			label: 'Auto Away', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_auto_away.png" // Fix to auto version
            state "Auto Home", 			action:"noOp", nextState:'Auto Home', 			label: 'Auto Home', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_auto_home.png" // Fix to auto
            state "Hold", 				action:"noOp", nextState:'Hold', 				label: 'Hold', 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
            state "Hold: Fan", 			action:"noOp", nextState:'Hold: Fan', 			label: "Hold: Fan", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_blue.png"
            state "Hold: Fan On", 		action:"noOp", nextState:'Hold: Fan on', 		label: "Hold: Fan On", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid_blue.png"
            state "Hold: Fan Auto", 	action:"noOp", nextState:'Hold: Fan Auto', 		label: "Hold: Fan Auto", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_blue.png"
            state "Hold: Circulate", 	action:"noOp", nextState:'Hold: Circulate', 	label: "Hold: Circulate", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on-1_blue..png"
            state "Hold: Home", 		action:"noOp", nextState:'Hold: Home', 			label: 'Hold: Home', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue_solid.png"
            state "Hold: Away", 		action:"noOp", nextState:'Hold: Away', 			label: 'Hold: Away',  		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue_solid.png"
            state "Hold: Sleep", 		action:"noOp", nextState:'Hold: Sleep', 		label: 'Hold: Sleep',  		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue_solid.png"
      		state "Vacation", 			action:"noOp", nextState:'Vacation', 			label: 'Vacation', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_vacation_blue_solid.png"
      		state "Offline", 			action:"noOp", nextState:'Offline', 			label: 'Offline', 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_black_dot.png"
            state "Hold: Temp", 		action:'noOp', nextState: 'Hold: Temp', 		label: 'Hold: Temp', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/thermometer_hold.png"
            state "Hold: Wakeup", 		action:"noOp", nextState:'Hold: Wakeup', 		label: 'Hold: Wakeup', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake_blue.png"
            state "Hold: Awake", 		action:"noOp", nextState:'Hold: Awake', 		label: 'Hold: Awake', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_awake_blue.png"
            state "default", 			action:"noOp", nextState: 'default', 			label: '${currentValue}', 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png", defaultState: true
		}        
        
        valueTile("currentProgram", "device.currentProgramName", height: 2, width: 4, decoration: "flat") {
			state "default", label:'Comfort Setting:\n${currentValue}', defaultState: true 
		}
        
		standardTile("setHome", "device.setHome", width: 2, height: 2, decoration: "flat") {
			state "home", action:"home", nextState: "updating", label:'Home', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png"
            state "home dis", action:"homeDisabled", nextState: "home dis", label:'Home', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_grey.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
        
        standardTile("setAway", "device.setAway", width: 2, height: 2, decoration: "flat") {
			state "away", action:"away", nextState: "updating", label:'Away', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png"
            state "away dis", action:"awayDisabled", nextState: "away dis", label:'Away', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_grey.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}

        standardTile("setSleep", "device.setSleep", width: 2, height: 2, decoration: "flat") {
			// state "sleep", action:"sleep", nextState: "updating", label:'Set Sleep', icon:"st.Bedroom.bedroom2"
			// can't call "sleep()" because of conflict with internal definition (it silently fails)
            state "sleep", action:"night", nextState: "updating", label:'Sleep', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue.png"
            state "sleep dis", action:"nightDisabled", nextState: "sleep dis", label:'Sleep', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_grey.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}

        standardTile("operatingState", "device.thermostatOperatingState", width: 2, height: 2, decoration: "flat") {
            state "idle", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_idle_purple.png"
            state "fan only", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "heating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "cooling", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
            state "offline", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
            state "default", label: '${currentValue}', defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png"
		}
        
       	standardTile("operatingStateDisplay", "device.thermostatOperatingStateDisplay", width: 2, height: 2, decoration: "flat") {
            state "idle", 						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_idle_purple.png"
            state "fan only", 					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "heating", 					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "cooling", 					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
            state "heating (smart recovery)", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
			state "cooling (smart recovery)", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
            state 'cooling (overcool)', 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
            state "offline", 					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
            state "off", 						icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_purple.png"
            state "default", 					icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", label: '${currentValue}', defaultState: true
		}
			
		standardTile("equipmentState", "device.equipmentOperatingState", width: 2, height: 2, decoration: "flat") {
			state "idle", 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_idle_purple.png"
            state "fan only", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "emergency", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_emergency.png"
            state "heat pump", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
            state "heat 1", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_1.png"
			state "heat 2", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2.png"
			state "heat 3", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3.png"
			state "heat pump 2", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2.png"
			state "heat pump 3", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3.png"
			state "cool 1", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_1.png"
			state "cool 2", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_2.png"
			state "heating", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//operatingstate_heat.png"
			state "cooling", 			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
			state "emergency hum", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_emergency+humid.png"
            state "heat pump hum", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat+humid.png"
            state "heat 1 hum", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_1+humid.png"
			state "heat 2 hum", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2+humid.png"
			state "heat 3 hum", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3+humid.png"
			state "heat pump 2 hum", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2+humid.png"
			state "heat pump 3 hum", 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3+humid.png"
			state "cool 1 deh", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_1-humid.png"
			state "cool 2 deh",		 	icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_2-humid.png"
			state "heating hum", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat+humid.png"
			state "cooling deh", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool-humid.png"
            state "humidifier", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_humidifier_only.png"
            state "dehumidifier", 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_dehumidifier_only.png"
            state "offline",	 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
            state "off", 				icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_off_purple.png"
            state "default",	 		icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png", action:"noOp", label: '${currentValue}', defaultState: true
		}

        valueTile("humidity", "device.humidity", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#0033cc"],
                [value: 60, color: "#ff66ff"]
            ] )
		}
		valueTile('humiditySetpoint', 'device.humiditySetpoint', decoration: 'flat', width: 1, height: 1) {
        	state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#0033cc"],
                [value: 60, color: "#ff66ff"]
            ] )
        }
        
        standardTile("motionState", "device.motion", width: 2, height: 2, decoration: "flat") {
			state "active", action:"noOp", nextState: "active", label:"Motion", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_motion.png"
			state "inactive", action: "noOp", nextState: "inactive", label:"No Motion", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_nomotion.png"
            state "not supported", action: "noOp", nextState: "not supported", label: "N/A", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/notsupported_x.png"
			state "unknown", action: "noOp", label:"Offline", nextState: "unknown", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_noconnection.png"
		}

        // Weather Tiles and other Forecast related tiles
		standardTile("weatherIcon", "device.weatherSymbol", width: 2, height: 2, decoration: "flat") {
			state "-2",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_updating_-2_fc.png" // , label: 'updating...',	action: 'noOp'
			state "0",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_sunny_00_fc.png" // , label: 'Sunny', action: 'noOp'			
			state "1",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png" // , label: 'Msly Sun',	action: 'noOp'
			state "2",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png" // , label: 'Ptly Cldy',	action: 'noOp'
			state "3",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png" // , label: 'Msly Cldy',	action: 'noOp'
			state "4",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_cloudy_04_fc.png" // , label: 'Cloudy',	action: 'noOp'
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
			state "19",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Hazy
			state "20",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Smoke
			state "21",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Dust
            
            // Night Time Icons (Day time Value + 100)
			state "100",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_clear_night_100_fc.png" // , label: 'Clear'
			state "101",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png" // , label: 'Msly Sun'
			state "102",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png" // , label: 'Ptly Cldy'
			state "103",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_mostly_cloudy_103_fc.png" // , label: 'Msly Cldy'
			state "104",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_cloudy_04_fc.png" // , label: 'Cloudy'
			state "105",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_drizzle_105_fc.png" // , label: 'Drizzle'
			state "106",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png" // , label: 'Rain'
			state "107",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png" // , label: 'Frz Rain'
			state "108",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png" // , label: 'Rain'
			state "109",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png" // , label: 'Frz Rain'
			state "110",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_night_snow_110_fc.png" // , label: 'Snow'
			state "111",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png" // , label: 'Flurries'
			state "112",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png" // , label: 'Frz Rain'
			state "113",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_snow_110_fc.png" // , label: 'Snow'
			state "114",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png" // , label: 'Frz Rain'
			state "115",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_thunderstorms_115_fc.png" // , label: 'T-Storms'
			state "116",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_windy_16.png" // , label: 'Windy'
			state "117",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_tornado_17.png" // , label: 'Tornado'
			state "118",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // , label: 'Fog'
			state "119",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // , label: 'Hazy'
			state "120",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // , label: 'Smoke'
			state "121",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // , label: 'Dust'
		}
        standardTile("weatherTemperature", "device.weatherTemperature", width: 2, height: 2, decoration: "flat") {
			state "default", action: "noOpWeatherTemperature", nextState: "default", label: 'Out: ${currentValue}°', defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/thermometer_fc.png"
		}       
        valueTile("lastPoll", "device.lastPoll", height: 1, width: 5, decoration: "flat") {
			state "thermostatStatus", label:'Last Poll: ${currentValue}', defaultState: true, backgroundColor:"#ffffff"
		}
		valueTile("holdStatus", "device.holdStatus", height: 1, width: 4, decoration: "flat") {
			state "default", label:'${currentValue}', defaultState: true //, backgroundColor:"#000000", foregroudColor: "#ffffff"
		}		
        standardTile("ecoLogo", "device.logo", width: 1, height: 1) {
			state "default", defaultState: true,  icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/header_ecobeeicon_blk.png"			
		}
        standardTile("oneBuffer", "device.logo", width: 1, height: 1, decoration: "flat") {
        	state "default", defaultState: true
        }
        valueTile("fanMinOnTime", "device.fanMinOnTime", width: 1, height: 1, decoration: "flat") {
        	state "fanMinOnTime", label: '${currentValue}m/hr', defaultState: true
        }
        valueTile("tstatDate", "device.tstatDate", width: 1, height: 1, decoration: "flat") {
        	state "default", /*"default",  action: "noOp", nextState: "default", */ label: '${currentValue}', defaultState: true
        }
        valueTile("tstatTime", "device.tstatTime", width: 1, height: 1, decoration: "flat") {
        	state "default", /*"default",  action: "noOp", nextState: "default", */ label: '${currentValue}', defaultState: true
        }
        standardTile("commandDivider", "device.logo", width: 4, height: 1, decoration: "flat") {
        	state "default", defaultState: true, icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/command_divider.png"			
        } 
        standardTile("circulating", "device.thermostatFanModeDisplay", width:1, height:1, decoration: "flat") {
        	state "default", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on-1_grey.png"
        	state "circulating", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid-1_grey.png"
        }
        standardTile("cooling", "device.coolLogo", width:1, height:1, decoration: "flat") {
        	state "disabled",  icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_grey.png"
        	state "default", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool.png"
        }
        standardTile("heating", "device.heatLogo", width:1, height:1, decoration: "flat") {
        	state "disabled", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_grey.png"
        	state "default", defaultState: true, icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat.png"
        }
    
		main(['temperature']) // Display current temperature in the things & room lists
		details([
        	// Use this if you are on a fully operational device OS (such as iOS or Android)
        	"temperatureDisplay",
            // Use the lines below if you can't (or don't want to) use the multiAttributeTile version
            // To use, uncomment these lines below, and comment out the line above
            // "temperature", "humidity",  "upButtonControl", "thermostatSetpoint", 
            // "currentStatus", "downButtonControl",
            
        	"equipmentState", "weatherIcon",  "refresh",  
            "currentProgramIcon", "weatherTemperature", "motionState", 
            'humidity', "holdStatus", 'humiditySetpoint', // "fanMinOnTime",
            "tstatDate", "commandDivider", "tstatTime", 
            "mode", "fanModeLabeled",  "resumeProgram", 
        	'heating',"heatSliderControl", "heatingSetpoint", 
            'cooling',"coolSliderControl", "coolingSetpoint",
            'circulating', 'circSliderControl', 'fanMinOnTime',
            "fanModeCycler", "fanOffButton", "setModeOff",
            "setModeHeat", "setModeCool", "setModeAuto",
            "setHome", "setAway", "setSleep",
            "apiStatus", "lastPoll"
            ])            
	}

	preferences {
    	//section ("Title") {
			input "myHoldType", "enum", title: "${getVersionLabel()}\n\nHold Type", description: "When changing temperature or program, use Permanent, Temporary, Hourly, Parent setting or Thermostat setting hold type?\n\nDefault: Ecobee (Connect) setting", 
            	required: false, options:["Permanent", "Temporary", "Hourly", "Parent", "Thermostat"]
           	input "myHoldHours", "enum", title: "Hourly Hold Time", description: "If Hourly hold, how many hours do you want to hold?\n\nDefault: Ecobee (Connect) setting", required: false, options:[2,4,6,8,12,16,24]
            input "smartAuto", "bool", title: "Smart Auto Temp Adjust", description: "This flag allows the temperature to be adjusted manually when the thermostat " +
					"is in Auto mode. An attempt to determine if the heat or cool setting should be changed is made automatically.", required: false
       //}
	}
}

// parse events into attributes
def parse(String description) {
	LOG( "parse() --> Parsing '${description}'" )
	// Not needed for cloud connected devices
}

def refresh(force=false) {
	// No longer require forcePoll on every refresh - just get whatever has changed
	LOG("refresh() - calling pollChildren ${force?'(forced)':''}",2,null,'info')
	parent.pollChildren(getDeviceId(),force) // tell parent to just poll me silently -- can't pass child/this for some reason
}
def doRefresh() {
	// Pressing refresh within 6 seconds of the prior refresh completing will force a complete poll of the Ecobee cloud - otherwise changes only
    refresh(state.lastDoRefresh?((now()-state.lastDoRefresh)<6000):false)
    sendEvent(name: 'doRefresh', value: 'refresh', isStateChange: true, displayed: false)
    runIn(2, 'resetRefreshButton', [overwrite: true])
    resetUISetpoints()
    state.lastDoRefresh = now()	// reset the timer after the UI has been updated
}
def resetRefreshButton() {
	sendEvent(name: 'doRefresh', value: 'refresh', isStateChange: true, displayed: false)
}
def forceRefresh() {
	refresh(true)
}

def installed() {
	updated()
}

def updated() {
	LOG("${getVersionLabel()} updated",1,null,'trace')
    sendEvent(name: 'checkInterval', value: 3900, displayed: false, isStateChange: true)  // 65 minutes (we get forcePolled every 60 minutes
    resetUISetpoints()
    runIn(2, 'forceRefresh', [overwrite: true])
}

void poll() {
	LOG("Executing 'poll' using parent SmartApp", 2, null, 'info')
    parent.pollChildren(getDeviceId(),false) // tell parent to just poll me silently -- can't pass child/this for some reason
}

// Health Check will ping us based on the frequency we configure in Ecobee (Connect) (derived from poll & watchdog frequency)
def ping() {
	LOG("Health Check ping - apiConnected: ${device.currentValue('apiConnected')}, ecobeeConnected: ${device.currentValue('ecobeeConnected')}, checkInterval: ${device.currentValue('checkInterval')} seconds",1,null,'warn')
   	parent.pollChildren(getDeviceId(),true) 	// forcePoll just me
}

def generateEvent(Map results) {
	def startMS = now()
	boolean debugLevelFour = debugLevel(4)
	if (debugLevelFour) LOG("generateEvent(): parsing data ${results}", 4)
    //LOG("Debug level of parent: ${parent.settings.debugLevel}", 4, null, "debug")
	def linkText = getLinkText(device)
    def isMetric = wantMetric()
    def forceChange = false

	def updateTempRanges = false
    def precision = device.currentValue('decimalPrecision')
    if (precision == null) precision = isMetric ? 1 : 0
    Integer objectsUpdated = 0
    def supportedThermostatModes = []
    if (state.supportedThermostatModes == null) {
    	// Initialize for those that are updating the DTH with this version
    	supportedThermostatModes = ['off']
    	if (device.currentValue('heatMode') == 'true') supportedThermostatModes << 'heat'
        if (device.currentValue('coolMode') == 'true') supportedThermostatModes << 'cool'
        if (device.currentValue('autoMode') == 'true') supportedThermostatModes << 'auto'
        if (device.currentValue('auxHeatMode') == 'true') supportedThermostatModes += ['auxHeatOnly', 'emergency heat']
       	sendEvent(name: "supportedThermostatModes", value: supportedThermostatModes, displayed: false)
       	sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false)
        state.supportedThermostatModes = supportedThermostatModes
    } else {
    	supportedThermostatModes = state.supportedThermostatModes
    }
	
	if(results) {
		results.each { name, value ->
			objectsUpdated++
            if (debugLevelFour) LOG("generateEvent() - In each loop: object #${objectsUpdated} name: ${name} value: ${value}", 4)
            
            String tempDisplay = ""
			def eventFront = [name: name, linkText: linkText, handlerName: name]
			def event = [:]
            def sendValue = value.toString() // was String
			def isChange = /*forceChange ||*/ (name == 'temperature') || isStateChange(device, name, sendValue)
            def tMode = (name == 'thermostatMode') ? sendValue : device.currentValue('thermostatMode')
            
			switch (name) {	
            	case 'forced':
                	forceChange = sendValue as Boolean
                    break;
                    
				case 'heatingSetpoint':
                	if (isChange || forceChange) {
                    	LOG("heatingSetpoint: ${sendValue}",2,null,'info')
                    	sendEvent(name: 'heatingSetpointDisplay', value: sendValue, descriptionText: "Heating setpoint is ${sendValue}°", /* isStateChange: isChange,*/ displayed: true) // for the slider
                        objectsUpdated++        
                        if (tMode == 'heat') {
                            // def displayValue = isMetric ? sendValue : value.toDouble().round(0).toInteger().toString() 	// Truncate the decimal point
                            sendEvent(name: 'thermostatSetpoint', value: /*displayValue*/ sendValue, descriptionText: "Thermostat setpoint is ${sendValue}°", displayed: false /*, isStateChange: isChange */)
                        	objectsUpdated++
                        } else if (tMode == 'auto') {
                        	Double avg = ((value.toDouble() + device.currentValue('coolingSetpointDisplay').toDouble()).toDouble() / 2.0).toDouble().round(precision.toInteger())
                            sendEvent(name: 'thermostatSetpoint', value: avg.toString(), descriptionText: "Thermostat setpoint average is ${avg}°", displayed: true /*, isStateChange: isChange */)
							objectsUpdated++
                        }
                        def heatDiff = device.currentValue('heatDifferential')
                        if (heatDiff == null) heatDiff = 0.0
                        Double heatAt = ((value.toDouble() - heatDiff.toDouble()).toDouble().round(precision.toInteger()))
                        sendEvent(name: 'heatAtSetpoint', value: heatAt, /* isStateChange: isChange, */ displayed: false)
                        objectsUpdated++
                        if (!device.currentValue('thermostatOperatingState').startsWith('heat')) sendValue = heatAt.toString()
                        event = eventFront + [value: sendValue, /*descriptionText: "Heating setpoint is ${sendValue}°", isStateChange: isChange,*/ displayed: false]
                    }
                    break;
                
                case 'heatingSetpointDisplay':
                	event = eventFront + [value: sendValue, descriptionText: "Heating setpoint is ${sendValue}°", displayed: true]
                    break;
                        
				case 'coolingSetpoint':
                	if (isChange || forceChange) {
                		LOG("coolingSetpoint: ${sendValue}",2,null,'info')                    
                		sendEvent(name: 'coolingSetpointDisplay', value: sendValue, descriptionText: "Cooling setpoint is ${sendValue}°", /* isStateChange: true, */ displayed: true) // for the slider
                        objectsUpdated++                       
                        if (tMode == 'cool') {
                        	//def displayValue = isMetric ? sendValue : ((value.toInteger() == value.toFloat()) ?	value.toInteger().toString() : sendValue)	// Truncate the decimal point
                    		sendEvent(name: 'thermostatSetpoint', value: /* displayValue */ sendValue, descriptionText: "Thermostat setpoint is ${sendValue}°", displayed: false, /* isStateChange: true */)
                        	objectsUpdated++
                        } else if (tMode == 'auto') {
                        	Double avg = ((value.toDouble() + device.currentValue('heatingSetpointDisplay').toDouble()).toDouble() / 2.0).toDouble().round(precision.toInteger())
                            sendEvent(name: 'thermostatSetpoint', value: avg.toString(), descriptionText: "Thermostat setpoint average is ${avg}°", displayed: true, /* isStateChange: true */)
							objectsUpdated++
                        }
                        def coolDiff = device.currentValue('coolDifferential')
                        if (coolDiff == null) coolDiff = 0.0
                        Double coolAt = ((value.toDouble() + coolDiff.toDouble()).toDouble().round(precision.toInteger()))
                        sendEvent(name: 'coolAtSetpoint', value: coolAt, /* isStateChange: true, */ displayed: false)
                        objectsUpdated++
                        if (!device.currentValue('thermostatOperatingState').startsWith('cool')) sendValue = coolAt.toString()
                        event = eventFront + [value: sendValue, /*descriptionText: "Cooling setpoint is ${sendValue}°",  isStateChange: true, */ displayed: false]
                    }
                    break;
                    
                case 'coolingSetpointDisplay': 
                	event = eventFront + [value: sendValue, descriptionText: "Cooling setpoint is ${sendValue}°", displayed: true]
                    break;
                    
                case 'thermostatSetpoint':
                	if (isChange || forceChange) {
                    	LOG("thermostatSetpoint: ${sendValue}",2,null,'info')
                    	def displayValue = isMetric ? sendValue : ((value.toInteger() == value.toFloat()) ?	value.toInteger().toString() : sendValue)	// Truncate the decimal point
                    	if (isChange) event = eventFront + [value: displayValue,  descriptionText: "Thermostat setpoint is ${sendValue}°", isStateChange: true, displayed: true]
                    }
                    break;
                    
				case 'weatherTemperature':
                	if (isChange) event = eventFront + [value: sendValue,  descriptionText: getTemperatureDescriptionText(name, sendValue, linkText), isStateChange: true, displayed: true]
                    break;
                    
                case 'temperature':
                	//def redisplay = false
                    if (device.currentValue('thermostatOperatingState') == 'offline') {
                        tempDisplay = '451°'		// As in Fahrenheit 451
                    } else {
                        Double dValue = value.toDouble()
						// Generate the display value that will preserve decimal positions ending in 0
                    	if (precision == 0) {
                    		tempDisplay = dValue.round(0).toInteger().toString() + '°'
                            sendValue = dValue.round(0).toInteger()
                    	} else {
							tempDisplay = String.format( "%.${precision.toInteger()}f", dValue.round(precision.toInteger())) + '°'
                        }
                        //redisplay = (device.currentValue('temperatureDisplay') == '451°'
                    }
                    if (isChange) event = eventFront + [value: sendValue,  descriptionText: getTemperatureDescriptionText(name, sendValue, linkText), isStateChange: true, displayed: true]
					break;
				
				case 'thermostatOperatingState':
                	// A little tricky, but this is how we display Smart Recovery within the stock Thermostat multiAttributeTile
                    // thermostatOperatingStateDisplay has the same values as thermostatOperatingState, plus "heating (smart recovery)" 
                    // and "cooling (smart recovery)". We separately update the standard thermostatOperatingState attribute.
                    if ((sendValue == 'idle') && (device.currentValue('thermostatMode') == 'off')) sendValue = 'off'
                    isChange = isStateChange(device, 'thermostatOperatingStateDisplay', sendValue) 
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
                   	if (isStateChange(device, 'thermostatOperatingStateDisplay', sendValue)) {
                    	sendEvent(name: "thermostatOperatingStateDisplay", value: sendValue, descriptionText: "Thermostat is ${descText}", linkText: linkText, 
                    					handlerName: "${name}Display", isStateChange: true, displayed: displayDesc)
                        objectsUpdated++
                    }
                    if (sendValue == 'offline') {
                    	sendEvent(name: "temperatureDisplay", value: "451°", isStateChange: true, displayed: false, descriptionText:
                        				"Fahrenheit 451")
                        objectsUpdated++
                    }
                    
                    // now update thermostatOperatingState - is limited by API to idle, fan only, heating, cooling, pending heat, pending cool, ventilator only
					if (isStateChange(device, name, realValue)) {
                    	// First, check if we need to change the Heating at/Heating to temperature values
                    	if (realValue.startsWith('heat')) {
                        	// heatingSetpoint should display actual setpoint for "Heating to..."
                            def heatSetp = device.currentValue('heatingSetpointDisplay')
                        	sendEvent(name: 'heatingSetpoint', value: heatSetp, descriptionText: "Heating to ${heatSetp}°", displayed: false) // let sendEvent figure out if this is a change
                        } else {
                        	def heatSetp = device.currentValue('heatAtSetpoint')
                        	sendEvent(name: 'heatingSetpoint', value: heatSetp, descriptionText: "Heating at ${heatSetp}°", displayed: false)
                        }
                        if (realValue.startsWith('cool')) {
                        	// coolingSetpoint should display actual setpoint for "Cooling to..."
                            def coolSetp = device.currentValue('coolingSetpointDisplay')
                        	sendEvent(name: 'coolingSetpoint', value: coolSetp, descriptionText: "Cooling to ${coolSetp}°", displayed: false)
                        } else {
                        	def coolSetp = device.currentValue('coolAtSetpoint')
                        	sendEvent(name: 'coolingSetpoint', value: coolSetp, descriptionText: "Cooling at ${coolSetp}°", displayed: false)
                        }
                    	event = eventFront + [value: realValue, descriptionText: "Thermostat is ${realValue}", isStateChange: true, displayed: false]
                        
                        // Keep track of whether we were last heating or cooling
                        def lastOpState = device.currentValue('thermostatOperatingState')
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
                            def lastOpState = device.currentValue('thermostatOperatingState')
                            if (lastOpState.contains('ea') || lastOpState.contains('oo')) sendEvent(name: 'lastOpState', value: lastOpState, displayed: false)
                       		sendEvent(name: 'thermostatOperatingState', value: 'idle', descriptionText: 'Thermostat is off', displayed: true /*, isStateChange: true */)
                        	sendEvent(name: 'thermostatOperatingStateDisplay', value: 'off', descriptionText: 'Thermostat is off', displayed: false, /* isStateChange: true */)
                        	objectsUpdated += 2
                        }
                        String descText = sendValue.endsWith('deh') ? sendValue.replace('deh', '& dehumidifying') : (sendValue.endsWith('hum') ? sendValue.replace('hum', '& humidifying') : sendValue)
                        if (!descText.startsWith('heat pump')) {
                        	descText = descText.replace('heat ','heating ')
                        	descText = descText.replace('cool ','cooling ')
                        }
                        descText = descText.replace('1', 'stage 1').replace('2', 'stage 2').replace('3', 'stage 3')
                     	event = eventFront + [value: sendValue, descriptionText: "Equipment is ${descText}", isStateChange: isChange, displayed: true]
                    }
					break;
				
				case 'equipmentStatus':
					if (isChange) {
                    	String descText = (value == 'idle') ? 'Equipment is idle' : ((value == 'offline') ? 'Equipment is offline' : "Equipment is running ${value}")
                    	event = eventFront +  [value: "${value}", descriptionText: descText, isStateChange: true, displayed: false]
                    }
					break;
				
           		case 'lastPoll':
					if (isChange) event = eventFront + [value: "${value}", descriptionText: "Poll: ${value}", isStateChange: true, displayed: debugLevel(4)]
					break;
				
				case 'humidity':
                	if (isChange) {
                		def humSetpoint = device.currentValue('humiditySetpoint') 
                    	// if (humSetpoint == null) humSetpoint = 0
                    	String setpointText = ((humSetpoint == null) || (humSetpoint == 0)) ? '' : " (setpoint: ${humSetpoint}%)"
                        event = eventFront + [value: sendValue, descriptionText: "Humidity is ${sendValue}%${setpointText}", isStateChange: true, displayed: true]
                    }
            		break;
				
				case 'humiditySetpoint':
					if (isChange && (sendValue != '0')) {
                    	def dispValue = sendValue.replaceAll('-', "-\n")
                    	event = eventFront + [value: dispValue, descriptionText: "Humidity setpoint is ${sendValue}%", isStateChange: true, displayed: false]
                        def hum = device.currentValue('humidity')
                        if (hum == null) hum = 0
		            	sendEvent( name: 'humidity', value: hum, linkText: linkText, handlerName: 'humidity', descriptionText: "Humidity is ${hum}% (setpoint: ${sendValue}%)", isStateChange: false, displayed: true )
                        objectsUpdated++
                    }
                    break;
				
				case 'currentProgramName':
                    LOG("currentProgramName: ${sendValue}", 2, null, 'info')
                	// always update the button states, even if the value didn't change
                	String progText = ''
                    if (sendValue == 'Vacation') {
                    	if (device.currentValue('currentProgramName') == 'Offline') disableVacationButtons() // not offline any more
                    	progText = 'Climate is Vacation'
                        sendEvent(name: 'resumeProgram', value: 'cancel', displayed: false, isStateChange: true)	// change the button to Cancel Vacation
                    } else if (sendValue == 'Offline') {
                    	progText = 'Thermostat is Offline'
                        if (device.currentValue('currentProgramName') != 'Offline') disableAllButtons() // just went offline
                    } else {
                    	progText = 'Climate is '+sendValue.trim().replaceAll(":","")
                        def buttonValue = (sendValue.startsWith('Hold') || sendValue.startsWith('Auto ')) ? 'resume' : 'resume dis'
                        sendEvent(name: 'resumeProgram', value: buttonValue, displayed: false, isStateChange: true)	// change the button to Resume Program
                    	def previousProgramName = device.currentValue('currentProgramName')
                        if (previousProgramName) {
                        	// update the button states
                    		if (previousProgramName == 'Offline') enableAllButtons() 			// not offline any more
                            if (previousProgramName.contains('acation')) updateModeButtons() 	// turn the mode buttons back on if we just exited a Vacation Hold
                        }	
                    }
					if (isChange) {
                    	event = eventFront + [value: sendValue, descriptionText: progText, isStateChange: true, displayed: true]
                        if (sendValue.endsWith('Temp')) enableAllProgramButtons()
                    }
					break;
                    
                case 'currentProgram':
                	LOG("currentProgram: ${sendValue}", 2, null, 'info')
                	// always update the button states, even if the value didn't change
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
                    if (isChange) event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
                	break;
				
				case 'apiConnected':
                	// only display in the devices' log if we are in debug level 4 or 5
                	if (isChange) event = eventFront + [value: sendValue, descriptionText: "API Connection is ${value}", isStateChange: true, displayed: debugLevelFour]
					break;
				
				case 'weatherSymbol':
                case 'timeOfDay':
					// Check to see if it is night time, if so change to a night symbol
                    Integer symbolNum = (name == 'weatherSymbol') ? value.toInteger() : device.currentValue('weatherSymbol').toInteger()
                    String timeOfDay
                    if (name == 'timeOfDay') {
                    	timeOfDay = value
                        objectsUpdated++
                    } else {
                    	timeOfDay = device.currentValue('timeOfDay')
                    }
                    if ((timeOfDay == 'night') && (symbolNum < 100)) { 
                    	symbolNum = symbolNum + 100
                    } else if ((timeOfDay == 'day') && (symbolNum >= 100)) {
                    	symbolNum = symbolNum - 100
					}
					isChange = isStateChange(device, 'weatherSymbol', symbolNum.toString())
					if (isChange) event = [name: 'weatherSymbol', linkText: linkText, handlerName: 'weatherSymbol', value: "${symbolNum}", descriptionText: "Weather Symbol is ${symbolNum}", isStateChange: true, displayed: true]
					sendEvent( name: 'timeOfDay', value: timeOfDay, displayed: false, isStateChange: true )
                    break;
				
				case 'thermostatHold':
					if (isChange) {
                    	if (sendValue == 'vacation') { 
                        	disableVacationButtons()
                        } else if ((sendValue == '') && (device.currentValue('thermostatHold') == 'vacation')) {
                        	enableVacationButtons()
                        }
                            
                    	String descText = (sendValue == '') ? 'Hold finished' : (value == 'hold') ? "Hold ${device.currentValue('currentProgram')} (${device.currentValue('scheduledProgram')})" : "Hold for ${sendValue}"
                    	event = eventFront + [value: sendValue, descriptionText: descText, isStateChange: true, displayed: true]
                    }
					break;
				
				case 'holdStatus': 
					if (isChange) event = eventFront + [value: sendValue, descriptionText: sendValue, isStateChange: true, displayed: (value != '')]
					break;
                    
              	case 'motion': 
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "Motion is ${sendValue}", isStateChange: true, displayed: true]
					break;
				
				case 'fanMinOnTime':
					if (isChange || forceChange) {
                    	def circulateText = (value == 0) ? 'Fan Circulation is disabled' : "Fan Circulation is ${sendValue} minutes per hour"
                    	event = eventFront + [value: sendValue, descriptionText: circulateText, /* isStateChange: isChange, */ displayed: true]
                        def fanMode = device.currentValue('thermostatFanMode')
                        if (fanMode != 'on') {
                       		if (device.currentValue('thermostatMode') == 'off') {
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
					if (isChange) {			
                    	event = eventFront + [value: sendValue, descriptionText: "Thermostat Mode is ${sendValue}", data:[supportedThermostatModes: supportedThermostatModes], isStateChange: true, displayed: true]
					}
                    switch (sendValue) {
                       	case 'off':
                            if (isChange) {
                           		sendEvent(name: 'thermostatOperatingState', value: 'idle', displayed: true, isStateChange: true, descriptionText: 'Thermostat is idle')
                            	sendEvent(name: 'thermostatOperatingStateDisplay', value: 'off', displayed: false, isStateChange: true)
                            }
                            def currentFanMode = device.currentValue('thermostatFanMode')
                            if ((currentFanMode != 'off') && (currentFanMode != 'on')) { // auto or circulate
                                if (device.currentValue('fanMinOnTime') == 0) {
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
                            def avg = ((device.currentValue('heatingSetpointDisplay').toDouble() + device.currentValue('coolingSetpointDisplay').toDouble()) / 2.0).toDouble().round(precision.toInteger()).toString()
                            if (isStateChange(device, 'thermostatSetpoint', avg)) sendEvent(name: 'thermostatSetpoint', value: avg, descriptionText: "Thermostat setpoint is ${avg}°", displayed: true, isStateChange: true)
                            disableModeAutoButton()
                            disableFanOffButton()
                            objectsUpdated++
                            break;
                                
                        case 'heat':
                        	def statSetpoint = device.currentValue('heatingSetpointDisplay').toString()
                            if (isStateChange(device, 'thermostatSetpoint', statSetpoint)) sendEvent(name: 'thermostatSetpoint', value: statSetpoint, descriptionText: "Thermostat setpoint is ${statSetpoint}°", displayed: true, isStateChange: true)
                            disableModeHeatButton()
                            disableFanOffButton()
                            objectsUpdated++
                            break;
                                
                        case 'cool':
							def statSetpoint = device.currentValue('coolingSetpointDisplay').toString()
                            if (isStateChange(device, 'thermostatSetpoint', statSetpoint)) sendEvent(name: 'thermostatSetpoint', value: statSetpoint, descriptionText: "Thermostat setpoint is ${statSetpoint}°", displayed: true, isStateChange: true)
                            disableModeCoolButton()
                            disableFanOffButton()
                            objectsUpdated++
                            break;
                                
                        case 'auxHeatOnly':
                            enableAllModeButtons()
                            disableFanOffButton()
                            objectsUpdated++
                            break;
                    }
		            break;
				
        		case 'thermostatFanMode':
                    if (isChange){
                    	event = eventFront + [value: sendValue, descriptionText: "Fan Mode is ${sendValue}", data:[supportedThermostatFanModes: fanModes()], isStateChange: true, displayed: true]
                        sendEvent(name: "supportedThermostatFanModes", value: fanModes(), displayed: false)
					}
                    if (device.currentValue('thermostatHold') != 'vacation') {
                    	switch(sendValue) {
                            case 'off':
                        		// Assume (for now) that thermostatMode is also 'off' - this should be enforced by setThermostatFanMode() (who is also only one  who will send us 'off')
                        		sendEvent(name: 'thermostatFanModeDisplay', value: "off dis", isStateChange: true, displayed: false) // have to force it to update for some reason
                        		disableFanOffButton()
                                break;
                                
                        	case 'auto':
                            case 'circulate':
                        		if (device.currentValue('thermostatMode') == 'off') {
                            		if (device.currentValue('fanMinOnTime') != 0) {	
                                		sendValue = 'circulate dis'
                                		enableFanOffButton() 
                                	} else {
                                		sendValue = 'off dis'		// display 'off' when Fan Mode == 'auto' && Thermostat Mode == 'off'
                                		disableFanOffButton() 
                                	}
                            	} else {
                            		disableFanOffButton()		// can't turn off the fan if the Thermostat isn't off
                            		if (device.currentValue('fanMinOnTime') != 0) { sendValue = 'circulate' } else { sendValue = 'auto' }
                            	}
                        		sendEvent(name: 'thermostatFanModeDisplay', value: sendValue, isStateChange: true, displayed: false)	// have to force it to update for some reason
                                break;
                                
                        	case 'on':
                        		sendEvent(name: 'thermostatFanModeDisplay', value: 'on', isStateChange: true, displayed: false)			// have to force it to update for some reason
                                break;
                        }
                    } else {
                        sendEvent(name: 'thermostatFanModeDisplay', value: "${sendValue} dis", isStateChange: true, displayed: false)	// have to force it to update for some reason
                        disableFanOffButton()
                    }
            		break;
				
				case 'debugEventFromParent':
					event = eventFront + [value: sendValue, descriptionText: "-> ${sendValue}", isStateChange: true, displayed: true]
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
                    	sendEvent(name: 'tstatTime', value: tstatTime, isStateChange: true, displayed: false) // assume time always is a change...
                    }
                    break;
                    
                // Update the new (Optional) SetpointMin/Max attributes
                case 'heatRangeLow':
                	if (isChange || (device.currentValue('heatingSetpointMin') != value)) {
                    	sendEvent(name: 'heatingSetpointMin', value: value, isStateChange: true, displayed: false)
                        objectsUpdated++
                        if ((tMode == 'heat') || (tMode == 'auto')) {
                        	sendEvent(name: 'thermostatSetpointMin', value: value, isStateChange: true, displayed: false)
                           	def range = [value, device.currentValue('thermostatSetpointMax')]
                            sendEvent(name: 'thermostatSetpointRange', value: range, isStateChange: true, displayed: false)
                            range = [value, device.currentValue('heatingSetpointMax')]
                            sendEvent(name: 'heatingSetpointRange', value: range, isStateChange: true, displayed: false)
                            objectsUpdated += 3
                        }
                        event = eventFront + [value: value, isStateChange: true, displayed: false]
                    }
                    break;
                    
				case 'heatRangeHigh':
                  	if (isChange || (device.currentValue('heatingSetpointMax') != value)) {
                    	sendEvent(name: 'heatingSetpointMax', value: value, isStateChange: true, displayed: false)
                        objectsUpdated++
                        if (tMode == 'heat') {
                        	sendEvent(name: 'thermostatSetpointMax', value: value, isStateChange: true, displayed: false)
                            def range = [device.currentValue('thermostatSetpointMin'), value]
                            sendEvent(name: 'thermostatSetpointRange', value: range, isStateChange: true, displayed: false)
                            range = [device.currentValue('heatingSetpointMin'), value]
                            sendEvent(name: 'heatingSetpointRange', value: range, isStateChange: true, displayed: false)
                            objectsUpdated += 3
                        }
                        event = eventFront + [value: value, isStateChange: true,  displayed: false]
                    }
                    break;
                    
				case 'coolRangeLow':
                	if (isChange || (device.currentValue('coolingSetpointMin') != value)) {
                    	sendEvent(name: 'coolingSetpointMin', value: value, isStateChange: true, displayed: false)
                        objectsUpdated++
                        if (tMode == 'cool') {
                        	sendEvent(name: 'thermostatSetpointMin', value: value, isStateChange: true, displayed: false)
                            def range = [value, device.currentValue('thermostatSetpointMax')]
                            sendEvent(name: 'thermostatSetpointRange', value: range, isStateChange: true, displayed: false)
                            range = [value, device.currentValue('coolingSetpointMax')]
                            sendEvent(name: 'coolingSetpointRange', value: range, isStateChange: true, displayed: false)
                            objectsUpdated += 3
                        }
                        event = eventFront + [value: value, isStateChange: true,  displayed: false]
                    }
                    break;
                    
				case 'coolRangeHigh':
                	if (isChange|| (device.currentValue('coolingSetpointMax') != value)) {
                    	sendEvent(name: 'coolingSetpointMax', value: value, isStateChange: true, displayed: false)
                        objectsUpdated++
                        if ((tMode == 'cool') || (tMode == 'auto')) {
                        	sendEvent(name: 'thermostatSetpointMax', value: value, isStateChange: true, displayed: false)
                            def range = [device.currentValue('thermostatSetpointMin'), value]
                            sendEvent(name: 'thermostatSetpointRange', value: range, isStateChange: true, displayed: false)
                            range = [device.currentValue('coolingSetpointMin'), value]
                            sendEvent(name: 'coolingSetpointRange', value: range, isStateChange: true, displayed: false)
                            objectsUpdated += 3
                        }
                        event = eventFront + [value: value, isStateChange: true, displayed: false]
                    }
                    break;
                    	
                case 'heatRange':
                	if (isChange) {
                        def newValue = sendValue.toString().replaceAll("[\\(\\)]","")
                        def idx = newValue.indexOf('..')
                		def low = newValue.take(idx).toFloat()
                    	def high = newValue.drop(idx+2).toFloat()
                        def range = [low,high]
                        sendEvent(name: 'heatingSetpointRange', value: range, isStateChange: true, displayed: false)
                        event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
                        objectsUpdated++
                    }
                    break;
                    
                case 'coolRange':
                	if (isChange) {
                        def newValue = sendValue.toString().replaceAll("[\\(\\)]","")
                        def idx = newValue.indexOf('..')
                		def low = newValue.take(idx).toFloat()
                    	def high = newValue.drop(idx+2).toFloat()
                        def range = [low,high]
                        sendEvent(name: 'coolingSetpointRange', value: range, isStateChange: true, displayed: false)
                        event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
                        objectsUpdated++
                    }
                    break;
                    
                case 'ecobeeConnected':
                	if (isChange) {
                    	if (value == false) { disableAllProgramButtons() } else { updateProgramButtons() }
                        event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
                    }
                    break;
                    
				case 'holdEndsAt':
                	if (isChange) {
                    	if (sendValue.startsWith('Hold ends a l' /*ong time from now*/)){
                        	// Record the lastHoldType for permanent holds effected through the Thermostat itself, the WebApp, or the Ecobee Mobile app
                        	sendEvent(name: 'lastHoldType', value: 'indefinite', isStateChange: true, displayed: false)
                            objectsUpdated++
                        }
                        event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
                    }
                
                // New attribute: supportedThermostatModes
               	case 'heatMode':
				case 'coolMode':
				case 'autoMode':
				case 'auxHeatMode':
                	def modeValue = (name == "auxHeatMode") ? "auxHeatOnly" : name - "Mode"
                	if (sendValue == 'true') {
                    	if (!supportedThermostatModes.contains(modeValue)) supportedThermostatModes += modeValue
                    } else {
                    	if (supportedThermostatModes.contains(modeValue)) supportedThermostatModes -= modeValue
                    }
               	case 'hasHeatPump':
				case 'hasForcedAir':
				case 'hasElectric':
				case 'hasBoiler':
                case 'hasHumidifier':
                //case 'humidifierMode':
                //case 'dehumidifierMode':
                //case 'dehumiditySetpoint':
                case 'hasDehumidifier':                  
				// These are ones we don't need to display or provide descriptionText for (mostly internal or debug use)
				case 'debugLevel':
				case 'decimalPrecision':
				case 'currentProgramId':
				case 'scheduledProgramName':
				case 'scheduledProgramId':
				case 'scheduledProgram':
				case 'heatStages':
				case 'coolStages':
				case 'heatDifferential':
				case 'coolDifferential':
                case 'heatCoolMinDelta':
                case 'programsList':
                case 'temperatureScale':
                case 'checkInterval':
                case 'statHoldAction':
                case 'lastHoldType':
					if (isChange) event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
					break;
				
				// everything else just gets displayed with generic text
				default:
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "${name} is ${sendValue}", isStateChange: true, displayed: true]			
					break;
			}
			if (event != [:]) {
				if (debugLevelFour) LOG("generateEvent() - Out of switch{}, calling sendevent(${event})", 4)
				sendEvent(event)
			}
            if (tempDisplay != "") {
        		sendEvent(name: 'temperatureDisplay', value: tempDisplay, linkText: linkText, descriptionText:"Temperature Display is ${tempDisplay}", displayed: false )
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
    LOG("Updated ${objectsUpdated} object${objectsUpdated!=1?'s':''} (${elapsed}ms)",2,this,'info')
    // if (elapsed > 2500) log.debug results
}

private def disableVacationButtons() {
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
private def enableVacationButtons() {
	// Re-enable the buttons
    LOG("Vacation finished, enabling buttons",2,null,'info')
    sendEvent(name: 'thermostatFanModeDisplay', value: device.currentValue('thermostatFanMode'), displayed: false)
    sendEvent(name: 'heatLogo', value: 'enabled', displayed: false)
    sendEvent(name: 'coolLogo', value: 'enabled', displayed: false)
    sendEvent(name: 'setFanOff', value: 'off', displayed: false)
	updateProgramButtons()
    updateModeButtons()
}
private def disableModeOffButton() {
	sendEvent(name: 'setModeOff', value: 'off dis', displayed: false)
    sendEvent(name: 'setModeAuto', value: 'auto', displayed: false)
    sendEvent(name: 'setModeHeat', value: 'heat', displayed: false)
    sendEvent(name: 'setModeCool', value: 'cool', displayed: false)
}
private def disableModeAutoButton() {
	sendEvent(name: 'setModeOff', value: 'off'+(modes().contains('off')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeAuto', value: 'auto dis', displayed: false)
    sendEvent(name: 'setModeHeat', value: 'heat'+(modes().contains('heat')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeCool', value: 'cool'+(modes().contains('cool')?'':' dis'), displayed: false)
}
private def disableModeHeatButton() {
	sendEvent(name: 'setModeOff', value: 'off'+(modes().contains('off')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeAuto', value: 'auto'+(modes().contains('auto')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeHeat', value: 'heat dis', displayed: false)
    sendEvent(name: 'setModeCool', value: 'cool'+(modes().contains('cool')?'':' dis'), displayed: false)
}
private def disableModeCoolButton() {
	sendEvent(name: 'setModeOff', value: 'off'+(modes().contains('off')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeAuto', value: 'auto'+(modes().contains('auto')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeHeat', value: 'heat'+(modes().contains('heat')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeCool', value: 'cool dis', displayed: false)
}
private def enableAllModeButtons() {
	sendEvent(name: 'setModeOff', value: 'off'+(modes().contains('off')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeAuto', value: 'auto'+(modes().contains('auto')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeHeat', value: 'heat'+(modes().contains('heat')?'':' dis'), displayed: false)
    sendEvent(name: 'setModeCool', value: 'cool'+(modes().contains('cool')?'':' dis'), displayed: false)
}
private def disableAllButtons() {
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
private def enableAllButtons() {
    sendEvent(name: 'thermostatFanModeDisplay', value: device.currentValue('thermostatFanMode'), displayed: false)
    sendEvent(name: 'heatLogo', value: 'enabled', displayed: false)
    sendEvent(name: 'coolLogo', value: 'enabled', displayed: false)
    sendEvent(name: 'setFanOff', value: 'off', displayed: false)
	updateModeButtons()
    updateProgramButtons()
}
private def disableFanOffButton() {
	sendEvent(name: 'setFanOff', value: 'off dis', displayed: false)
}
private def enableFanOffButton() {
	sendEvent(name: 'setFanOff', value: 'off', displayed: false)
}
private def disableHomeButton() {
	sendEvent(name: 'setHome', value: 'home dis', displayed: false)
    sendEvent(name: 'setAway', value: 'away', displayed: false)
    sendEvent(name: 'setSleep', value: 'sleep', displayed: false)
}
private def disableAwayButton() {
	sendEvent(name: 'setHome', value: 'home', displayed: false)
    sendEvent(name: 'setAway', value: 'away dis', displayed: false)
    sendEvent(name: 'setSleep', value: 'sleep', displayed: false)
}
private def disableSleepButton() {
	sendEvent(name: 'setHome', value: 'home', displayed: false)
    sendEvent(name: 'setAway', value: 'away', displayed: false)
    sendEvent(name: 'setSleep', value: 'sleep dis', displayed: false)
}
private def enableAllProgramButtons() {
	sendEvent(name: 'setHome', value: 'home', displayed: false)
    sendEvent(name: 'setAway', value: 'away', displayed: false)
    sendEvent(name: 'setSleep', value: 'sleep', displayed: false)
}
private def disableAllProgramButtons() {
	sendEvent(name: 'setHome', value: 'home dis', displayed: false)
    sendEvent(name: 'setAway', value: 'away dis', displayed: false)
    sendEvent(name: 'setSleep', value: 'sleep dis', displayed: false)
}
private def updateProgramButtons() {
	def currentProgram = device.currentValue('currentProgram')
	// if (currentProgram=='Home') {sendEvent(name:'setHome', value:'home dis', displayed:false)} else {sendEvent(name:'setHome', value:'home', displayed:false)}
    sendEvent(name:'setHome',value:'home'+(currentProgram=='Home'?' dis':''), displayed:false)
    sendEvent(name:'setAway',value:'away'+(currentProgram=='Away'?' dis':''), displayed:false)
    sendEvent(name:'setSleep',value:'sleep'+(currentProgram=='Sleep'?' dis':''), displayed:false)
}
private def updateModeButtons() {
	def currentMode = device.currentValue('thermmoostatMode')
    // if (currentMode=='off') {sendEvent(name:'setModeOff', value:'off dis', displayed:false)} else {sendEvent(name:'setModeOff', value:'off', displayed:false)}
    sendEvent(name:'setModeOff', value:('off'+((currentMode=='off') || !modes().contains('off'))?' dis':''), displayed:false)
    sendEvent(name:'setModeAuto', value:('auto'+((currentMode=='auto') || !modes().contains('auto'))?' dis':''), displayed:false)
    sendEvent(name:'setModeHeat', value:('heat'+((currentMode=='heat') || !modes().contains('heat'))?' dis':''), displayed:false)
    sendEvent(name:'setModeCool', value:('cool'+((currentMode=='cool') || !modes().contains('cool'))?' dis':''), displayed:false)
}

//return descriptionText to be shown on mobile activity feed
private getTemperatureDescriptionText(name, value, linkText) {
	switch (name) {
		case 'temperature':
			return "Temperature is ${value}°"
            break;
		case 'heatingSetpoint':
			return "Heating setpoint is ${value}°"
            break;
        case 'coolingSetpoint':
			return "Cooling setpoint is ${value}°"
            break;
        case 'thermostatSetpoint':
        	return "Thermostat setpoint is ${value}°"
            break;
        case 'weatherTemperature':
        	return "Outside temperature is ${value}°"
            break;
	}
}

// ***************************************************************************
// Thermostat setpoint commands
// API calls and UI handling
// ***************************************************************************

def setTemperature(setpoint) { 	// Obsolete as of v1.2.21
	LOG('setTemperature called in error',1,null,'error')
    return
}
void setHeatingSetpoint(setpoint) {
	LOG("setHeatingSetpoint() request with setpoint value = ${setpoint} before toDouble()", 4)
	setHeatingSetpoint(setpoint.toDouble())
}
void setHeatingSetpointDelay(setpoint) {
	LOG("Slider requested heat setpoint: ${setpoint}",4,null,'trace')
    def runWhen = parent.settings?.arrowPause.toInteger() ?: 4
    runIn( runWhen, 'sHS', [data: [setpoint:setpoint.toDouble()]] )
}
void sHS(data) {
	LOG("Setting heating setpoint to: ${data.setpoint}",4,null,'trace')
    setHeatingSetpoint(data.setpoint)
}
void setHeatingSetpoint(Double setpoint) {
    if (device.currentValue('thermostatHold') == 'vacation') {
    	LOG("setHeatingSetpoint(${setpoint}) called but thermostat is in Vacation mode, ignoring request",2,null,'warn')
        return
    }

	def isMetric = wantMetric()
   	def temperatureScale = isMetric ? 'C' : 'F'
	LOG("setHeatingSetpoint() request with setpoint value = ${setpoint}°${temperatureScale}", 2, null, 'info')
    
    if (!isMetric && (setpoint < 35.0)) {
    	setpoint = cToF(setpoint).toDouble().round(1)	// Hello, Google hack - seems to request C when stat is in Auto mode
        LOG ("setHeatingSetpoint() converted apparent C setpoint value to ${setpoint}°F", 2, null, 'info')
    }

	Double heatingSetpoint = isMetric ? roundC(setpoint).toDouble() : setpoint.toDouble().round(1)

	//enforce limits of heatingSetpoint range
	LOG("setHeatingSetpoint() before adjustment: ${heatingSetpoint}°${temperatureScale}", 4, null, 'trace')
	Double low = device.currentValue('heatRangeLow').toDouble()		// already converted to C by Parent
	Double high = device.currentValue('heatRangeHigh').toDouble()
    LOG("setHeatingSetpoint() low: ${low}°, high: ${high}°",4,null,'trace')
    // Silently adjust the temps to fit within the specified ranges
	if (heatingSetpoint < low ) { heatingSetpoint = isMetric ? roundC(low) : low }
	else if (heatingSetpoint > high) { heatingSetpoint = isMetric ? roundC(high) : high }
    
	LOG("setHeatingSetpoint() requesting heatingSetpoint: ${heatingSetpoint}°${temperatureScale}", 2, null, 'info')
    state.newHeatingSetpoint = heatingSetpoint
    runIn(2, "updateThermostatSetpoints", [overwrite: true])
}

void setCoolingSetpoint(setpoint) {
	LOG("setCoolingSetpoint() request with setpoint value = ${setpoint} (before toDouble)", 4)
	setCoolingSetpoint(setpoint.toDouble())
}
void setCoolingSetpointDelay(setpoint) {
	LOG("Slider requested cool setpoint: ${setpoint}",4,null,'trace')
    def runWhen = parent.settings?.arrowPause.toInteger() ?: 4
    runIn( runWhen, 'sCS', [data: [setpoint:setpoint]] )
}
void sCS(data) {
	LOG("Setting cooling setpoint to: ${data.setpoint}",4,null,'trace')
    setCoolingSetpoint(data.setpoint.toDouble())
}
void setCoolingSetpoint(Double setpoint) {
    if (device.currentValue('thermostatHold') == 'vacation') {
    	LOG("setCoolingSetpoint(${setpoint}) called but thermostat is in Vacation mode, ignoring request",2,null,'warn')
        return
    }
	def isMetric = wantMetric()
   	def temperatureScale = isMetric ? 'C' : 'F'
	LOG("setCoolingSetpoint() request with setpoint value = ${setpoint}°${temperatureScale}", 2, null, 'info')
    
    if (!isMetric && (setpoint < 35.0)) {
    	setpoint = cToF(setpoint).toDouble().round(1)	// Hello, Google hack - seems to request C when stat is in Auto mode
        LOG ("setCoolingSetpoint() converted apparent C setpoint value to ${setpoint}°F", 2, null, 'info')
    }

	Double coolingSetpoint = isMetric ? roundC(setpoint).toDouble() : setpoint.toDouble().round(1)

	//enforce limits of heatingSetpoint vs coolingSetpoint
	LOG("setCoolingSetpoint() before adjustment: coolingSetpoint = ${coolingSetpoint}°${temperatureScale}", 4, null, 'trace')
	Double low = device.currentValue('coolRangeLow').toDouble()
	Double high = device.currentValue('coolRangeHigh').toDouble()
    LOG("setCoolingSetpoint() low: ${low}°, high: ${high}°",4,null,'trace')
    // Silently adjust the temps to fit within the specified ranges
	if (coolingSetpoint < low ) { coolingSetpoint = isMetric ? roundC(low) : low }
	else if (coolingSetpoint > high) { coolingSetpoint = isMetric ? roundC(high) : high }

	LOG("setCoolingSetpoint() requesting coolingSetpoint: ${coolingSetpoint}°${temperatureScale}", 2, null, 'info') 
    state.newCoolingSetpoint = coolingSetpoint.toDouble()
    runIn(2, "updateThermostatSetpoints", [overwrite: true])
}

def updateThermostatSetpoints() {
	def isMetric = wantMetric()
	def deviceId = getDeviceId()
    
	Double heatingSetpoint = state.newHeatingSetpoint ? state.newHeatingSetpoint : device.currentValue('heatingSetpointDisplay').toDouble()
	Double coolingSetpoint = state.newCoolingSetpoint ? state.newCoolingSetpoint : device.currentValue('coolingSetpointDisplay').toDouble()
    LOG("updateThermostatSetpoints(): heatingSetpoint ${heatingSetpoint}°, coolingSetpoint ${coolingSetpoint}°", 2, null, 'info')
    
    def sendHoldType = whatHoldType()
    def sendHoldHours = null
    if (sendHoldType.isNumber()) {
    	sendHoldHours = sendHoldType
    	sendHoldType = 'holdHours'
	}
    LOG("updateThermostatSetpoints(): sendHoldType == ${sendHoldType} ${sendHoldHours}", 4, null, 'trace')
    
    // make sure we maintain the proper heatCoolMinDelta
    Double delta = device.currentValue('heatCoolMinDelta')?.toDouble()
    if (!delta) delta = isMetric ? 0.5 : 1.0
    
    if ((heatingSetpoint + delta) > coolingSetpoint) {
    	// we have to adjust heat/cool
    	if (state.newHeatingSetpoint) {
    		if (!state.newCoolingSetpoint) {
        		// got request to change heat only, push cool up
                coolingSetpoint = heatingSetpoint + delta
            } else {
            	// got request to change both, let's see what the thermostat is doing
                def mode = device.currentValue('thermostatMode')
                if (mode == 'heat') {
                	// we are in Heat Mode, so raise the cooling setpoint
                    coolingSetpoint = heatingSetpoint + delta
                } else if (mode == 'cool') {
                	// we are in Cool Mode, so lower the heating setpoint
                    heatingSetpoint = coolingSetpoint - delta
                } else if (mode == 'auto') {
                	// Auto Mode - let's see if the heat or cooling is on
                    def opState = device.currentValue('thermostatOperatingState')
                    if (opState.contains('ea')) {
                    	// currently heating, raise the cooling setpoint
                        coolingSetpoint = heatingSetpoint + delta
                    } else if (opState.contains('oo')) {
                    	// currently cooling, lower the heating setpoint
                        heatingSetpoint = coolingSetpoint - delta
                    } else {
                    	// Auto & Idle, check what our last operating state was and adjust the opposite
                        if (device.currentValue('lastOpState')?.contains('ea')) {
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
    	def updates = [	'coolingSetpointDisplay': coolingSetpoint.round(1),
        				'heatingSetpointDisplay': heatingSetpoint.round(1),
                     	'lastHoldType': sendHoldType ]
    	generateEvent(updates)
		LOG("Done updateThermostatSetpoints() coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}, thermostatSetpoint: ${device.currentValue('thermostatSetpoint')}",4,null,'trace') 
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
log.debug 'raiseSetpoint'
	// Cannot change setpoint while in Vacation mode
    if (device.currentValue('thermostatHold') == 'vacation') {
    	LOG("Cannot change the set point while thermostat is in Vacation mode, ignoring request",2,null,'warn')
        return
    }
    
	def mode = device.currentValue("thermostatMode")
	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("raiseSetpoint(): this mode: (${mode}) does not allow raiseSetpoint",2,null,'warn')
        return
	}
    
	def isMetric = wantMetric()
    def changingHeat = true			// is a change already in process?
    def changingCool = true
   	Double heatingSetpoint = state.newHeatSetpoint // device.currentValue('newHeatSetpoint')?.toDouble()
    if (!heatingSetpoint || (heatingSetpoint == 0.0)) {
    	heatingSetpoint = device.currentValue("heatingSetpointDisplay").toDouble()
        changingHeat = false
    }
	Double coolingSetpoint = state.newCoolSetpoint /// device.currentValue('newCoolSetpoint')?.toDouble()
    if (!coolingSetpoint || (coolingSetpoint == 0.0)) {
    	coolingSetpoint = device.currentValue("coolingSetpointDisplay")?.toDouble()
        changingCool = false
    }
    Double thermostatSetpoint = device.currentValue("thermostatSetpoint")?.toDouble()
    def operatingState = device.currentValue("thermostatOperatingState")
	LOG("raiseSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}", 2,null,'trace')
    
	//def precision = device.currentValue('decimalPrecision')
    //if (precision == null) precision = isMetric ? 1 : 0
	Double targetValue
    def smartAuto = usingSmartAuto()
    def runWhen = parent.settings?.arrowPause.toInteger() ?: 4
    if ((mode == 'auto') && smartAuto) {
        raiseSmartSetpoint(heatingSetpoint.toDouble(), coolingSetpoint.toDouble())
        return
    }
	if (changingHeat || (mode == 'heat') || ((mode == 'auto') && !smartAuto && (operatingState.contains('ea') || device.currentValue('lastOpState')?.contains('ea')))) {
    	targetValue = (heatingSetpoint + (isMetric ? 0.5 : 1.0)).toDouble().round(1)
        LOG("raiseSetpoint() Heating to ${targetValue}", 4)
        state.newHeatSetpoint = targetValue 	// sendEvent(name: 'newHeatSetpoint', value: targetValue, display: false, isStateChange: true)
        def updates = ['thermostatSetpoint': targetValue]
        generateEvent(updates)
        runIn( runWhen, 'changeHeatSetpoint', [data: [value:targetValue]] )
        return
    } else if (changingCool || (mode == 'cool') || ((mode == 'auto') && !smartAudio && (operatingState.contains('oo') || device.currentValue('lastOpState')?.contains('oo')))) {
       	targetValue = (coolingSetpoint + (isMetric ? 0.5 : 1.0)).toDouble().round(1)
        LOG("raiseSetpoint() Cooling to ${targetValue}", 4)
        state.newCoolSetpoint = targetValue		// sendEvent(name: 'newCoolSetpoint', value: targetValue, display: false, isStateChange: true)
        def updates = ['thermostatSetpoint': targetValue]
        generateEvent(updates)
        runIn( runWhen, 'changeCoolSetpoint', [data: [value:targetValue]] )
        return
    }
    // oops! shouldn't be here!
}

// lowerSetpoint: called by tile when user hit lower temperature button on UI
void lowerSetpoint() {
log.debug 'lowerSetpoint'
	// Cannot change setpoint while in Vacation mode
    if (device.currentValue('thermostatHold') == 'vacation') {
    	LOG("Cannot change the set point while thermostat is in Vacation mode, ignoring request",2,null,'warn')
        return
    }

	def mode = device.currentValue("thermostatMode")
	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("lowerSetpoint(): this mode: $mode does not allow lowerSetpoint", 2, null, "warn")
        return
    }
    
    def isMetric = wantMetric()
    def changingHeat = true			// is a change already in process?
    def changingCool = true
   	Double heatingSetpoint = state.newHeatSetpoint		// device.currentValue('newHeatSetpoint')?.toDouble()
    if (!heatingSetpoint || (heatingSetpoint.toDouble() == 0.0)) {
    	heatingSetpoint = device.currentValue("heatingSetpointDisplay").toDouble()
        changingHeat = false
    }
	Double coolingSetpoint = state.newCoolSetpoint		// device.currentValue('newCoolSetpoint')?.toDouble()
    if (!coolingSetpoint || (coolingSetpoint.toDouble() == 0.0)) {
    	coolingSetpoint = device.currentValue("coolingSetpointDisplay").toDouble()
        changingCool = false
    }
	Double thermostatSetpoint = device.currentValue("thermostatSetpoint")
    def operatingState = device.currentValue("thermostatOperatingState")
	LOG("lowerSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}", 2,null,'trace')

	//def precision = device.currentValue('decimalPrecision')
    //if (precision == null) precision = isMetric ? 1 : 0
	Double targetValue
    def smartAuto = usingSmartAuto()
    def runWhen = parent.settings?.arrowPause.toInteger() ?: 4
    
    if ((mode == 'auto') && smartAuto) {
        lowerSmartSetpoint(heatingSetpoint.toDouble(), coolingSetpoint.toDouble())
        return
    }
	if (changingHeat || (mode == 'heat') || ((mode == 'auto') && !smartAuto && (operatingState.contains('ea') || device.currentValue('lastOpState')?.contains('ea')))) {
    	targetValue = (heatingSetpoint.toDouble() - (isMetric ? 0.5 : 1.0)).toDouble().round(1)
        LOG("lowerSetpoint() Heating to ${targetValue}", 4)
        state.newHeatSetpoint = targetValue			// sendEvent(name: 'newHeatSetpoint', value: targetValue, display: false, isStateChange: true)
        def updates = ['thermostatSetpoint': targetValue]
        generateEvent(updates)
        runIn( runWhen, 'changeHeatSetpoint', [data: [value:targetValue]] )
        return
    } 
    if (changingCool || (mode == 'cool') || ((mode == 'auto') && !smartAuto && (operatingState.contains('oo') || device.currentValue('lastOpState')?.contains('oo')))) {
        targetValue = (coolingSetpoint.toDouble() - (isMetric ? 0.5 : 1.0)).toDouble().toDouble().round(1)
        LOG("lowerSetpoint() Cooling to ${targetValue}", 4)
        state.newCoolSetpoint = targetValue			// sendEvent(name: 'newCoolSetpoint', value: targetValue, display: false, isStateChange: true)
        def updates = ['thermostatSetpoint': targetValue]
        generateEvent(updates)
        runIn( runWhen, 'changeCoolSetpoint', [data: [value:targetValue]] )
        return
    }
}

// changeHeatSetpoint: effect setHeatingSetpoint for UI
void changeHeatSetpoint(newSetpoint) {
    def updates = ['heatingSetpoint': newSetpoint.value.toDouble(), 'currentProgramName':'Hold: Temp']
    generateEvent(updates)
	LOG("changeHeatSetpoint(${newSetpoint.value}°)",2,null,'info')
	setHeatingSetpoint( newSetpoint.value.toDouble() )
    state.newHeatSetpoint = null	// sendEvent(name: 'newHeatSetpoint', value: 0.0, displayed: false, isStateChange: true)
}

// changeCoolSetpoint: effect setCoolingSetpoint for UI
void changeCoolSetpoint(newSetpoint) {
    def updates = ['coolingSetpoint': newSetpoint.value.toDouble(), 'currentProgramName':'Hold: Temp']
    generateEvent(updates)
	LOG("changeCoolSetpoint(${newSetpoint.value}°)",2,null,'info')
	setCoolingSetpoint( newSetpoint.value.toDouble() )
    state.newCoolSetpoint = null	// sendEvent(name: 'newCoolSetpoint', value: 0.0, displayed: false, isStateChange: true)
}

// raiseSmartSetpoint: figure out which setpoint to raise
void raiseSmartSetpoint(Double heatingSetpoint, Double coolingSetpoint) {
	def isMetric = wantMetric()
	def currentTemp = device.currentValue('temperature').toDouble()
    //def precision = device.currentValue('decimalPrecision')
    //if (precision == null) precision = isMetric ? 1 : 0
    Double adjust = (isMetric ? 0.5 : 1.0)
    Double newHeat = (heatingSetpoint + adjust).toDouble().round(1)
    Double newCool = (coolingSetpoint + adjust).toDouble().round(1)
    
    if (newHeat > currentTemp) {
    	// turn the heat up
        LOG("raiseSmartSetpoint() Heating to ${newHeat}", 4)
        state.newHeatSetpoint = newHeat			// sendEvent(name: 'newHeatSetpoint', value: newHeat, display: false, isStateChange: true)
        def updates = ['thermostatSetpoint': newHeat]
        generateEvent(updates)
        runIn( runWhen, 'changeHeatSetpoint', [data: [value:newHeat], overwrite: true] )
        return
    } else {
    	// turn the cool up
        LOG("raiseSmartSetpoint() Cooling to ${newCool}", 4)
        state.newCoolSetpoint = newCool			// sendEvent(name: 'newCoolSetpoint', value: newCool, display: false, isStateChange: true)
        def updates = ['thermostatSetpoint': newCool]
        generateEvent(updates)
        runIn( runWhen, 'changeCoolSetpoint', [data: [value:newCool], overwrite: true] )
        return
    }
}

// lowerSmartSetpoint: figure out which setpoint to lower
void lowerSmartSetpoint(Double heatingSetpoint, Double coolingSetpoint) {
	def isMetric = wantMetric()
	def currentTemp = device.currentValue('temperature').toDouble()
    //def precision = device.currentValue('decimalPrecision')
    //if (precision == null) precision = isMetric ? 1 : 0
    Double adjust = (isMetric ? 0.5 : 1.0)
    Double newHeat = (heatingSetpoint - adjust).toDouble().round(1)
    Double newCool = (coolingSetpoint - adjust).toDouble().round(1)
    
    if (newCool < currentTemp) {
    	// turn the cool down
        LOG("lowerSmartSetpoint() Cooling to ${newCool}", 4)
        state.newCoolSetpoint = newCool			// sendEvent(name: 'newCoolSetpoint', value: newCool, display: false, isStateChange: true)
        def updates = ['thermostatSetpoint': newCool]
        generateEvent(updates)
        runIn( runWhen, 'changeCoolSetpoint', [data: [value:newCool], overwrite: true] )
        return
    } else {
    	// turn the heat down
        LOG("lowerSmartSetpoint() Heating to ${newHeat}", 4)
        state.newHeatSetpoint = newHeat			// sendEvent(name: 'newHeatSetpoint', value: newHeat, display: false, isStateChange: true)
        def updates = ['thermostatSetpoint': newHeat]
        generateEvent(updates)
        runIn( runWhen, 'changeHeatSetpoint', [data: [value:newHeat], overwrite: true] )
        return
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
	return
}

//* 'Fix' buttons that really aren't buttons (ie. stop them from changing if pressed)
void noOpWeatherTemperature() { sendEvent(name:'weatherTemperature',value:device.currentValue('weatherTemperature'),displayed:false,isStateChange:true) } 
void noOpCurrentProgramName() { sendEvent(name:'currentProgramName',value:device.currentValue('currentProgramName'),displayed:false,isStateChange:true) }

def generateQuickEvent(String name, String value, Integer pollIn=0) {
	sendEvent(name: name, value: value, displayed: false)
    if (pollIn > 0) { runIn(pollIn, refresh, [overwrite: true]) }
}

// ***************************************************************************
// Thermostat Mode commands
// Note that the Thermostat Mode CAN be changed while in Vacation mode
// ***************************************************************************
void setThermostatMode(String value) {
	// 	Uses Ecobee Modes: "auxHeatOnly" "heat" "cool" "off" "auto"  
    
    if (value.startsWith('emergency')) { value = 'auxHeatOnly' }    
	LOG("setThermostatMode(${value})", 5)
    
    def validModes = modes() 		// device.currentValue('supportedThermostatModes')
	if (!validModes.contains(value)) {
    	LOG("Requested Thermostat Mode (${value}) is not supported by ${device.displayName}", 2, null, 'warn')
        return
    }
    
	boolean changed = true
	if (device.currentValue('thermostatMode') != value) {
		if (parent.setMode(this, value, getDeviceId())) {
    		// generateQuickEvent('thermostatMode', value, 5)
            def updates = [thermostatMode:value]
            if (value=='off') { 
            	updates += [equipmentOperatingState:'off']
                if (device.currentValue('thermostatFanMode') == 'auto') {
                	if (device.currentValue('fanMinOnTime') == 0) {
                    	updates += [thermostatFanMode:'off',thermostatFanModeDisplay:'off']		// generateEvent will make this "fan dis"
                    } else {
                    	updates += [thermostatFanMode:'circulate',thermostatFanModeDisplay:'circulate']
                    }
                }                	
            } else { 
            	updates += [equipmentOperatingState:'idle'] 
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
def modes() {
	return state.supportedThermostatModes
}
void off() {
	LOG('off()', 5, null, 'trace')
	setThermostatMode('off')
}
void heat() {
	LOG('heat()', 5, null, 'trace')
    setThermostatMode('heat')
}
void auxHeatOnly() {
	LOG("auxHeatOnly()", 5)
    setThermostatMode("auxHeatOnly")
}
void emergency() {
	LOG("emergency()", 5)
    setThermostatMode("auxHeatOnly")
}
// This is the proper definition for the capability
void emergencyHeat() {
	LOG("emergencyHeat()", 5)
    setThermostatMode("auxHeatOnly")
}
void cool() {
	LOG('cool()', 5, null, 'trace')
    setThermostatMode('cool')
}
void auto() {
	LOG('auto()', 5, null, 'trace')
    setThermostatMode('auto')
}

// ***************************************************************************
// Thermostat Program (aka Climates) Commands
// Program/Climates CANNOT be changed while in Vacation mode
// ***************************************************************************
void setThermostatProgram(String program, holdType=null, holdHours=2) {
	// N.B. if holdType is provided, it must be one of the valid parameters for the Ecobee setHold call (indefinite, nextTransition, holdHours). dateTime not currently supported
	def currentThermostatHold = device.currentValue('thermostatHold')
    if (currentThermostatHold == 'vacation') {
    	LOG("setThermostatProgram(${program}): program change requested but ${device.displayName} is in Vacation mode, ignoring request",2,null,'warn')
        return
    }
    
    def programsList = []
    def programs = device.currentValue('programsList')
    if (!programs) {
    	LOG("Supported programs list not initialized, possible installation error", 1, this, 'error')
        programs = ["Away","Home","Sleep"]		// Just use the default list
    }
    
    programsList = new JsonSlurper().parseText(programs)
    if (!programsList.contains(program)) {
    	LOG("setThermostatProgram(${program}) not found in (${programsList})", 2, this, 'warn')
       	return
    }
   	
	def deviceId = getDeviceId()    
	LOG("setThermostatProgram(${program}, ${holdType}, ${holdHours})", 4,null,'trace')
    
    def sendHoldType = null
    def sendHoldHours = null
    if (holdType && (holdType != "")) {
    	sendHoldType = holdType
        if (holdType == 'holdHours') sendHoldHours = holdHours
    } else { 
		sendHoldType =  whatHoldType()
    	if (sendHoldType.isNumber()) {
    		sendHoldHours = sendHoldType
    		sendHoldType = 'holdHours'
		}
    }
    refresh()		// need to know if scheduled program changed recently
    
    
    def currentProgram = device.currentValue('currentProgram')
    def currentProgramName = device.currentValue('currentProgramName')
    def scheduledProgram = device.currentValue('scheduledProgram')
    
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
                runIn(5, refresh, [overwrite: false])
                generateProgramEvent(program,'')		// this may be redundant - resumeProgramInternal does this also
                return
            } else {
            	// Otherwise (already in a hold for the desired program), do nothing if we can verify that the holdType is the same
                def lastHoldType = device.currentValue('lastHoldType')
                if (lastHoldType && (sendHoldType == lastHoldType) && (sendHoldType != 'holdHours')) {	// we have to reset the timer on an Hourly hold
                    LOG("Thermostat Program is already Hold: ${program} (${sendHoldType})", 2, this, 'info')
                    return
                }
            }
        }
    }
	
    // if the requested program is the same as the one that is supposed to be running, then just resumeProgram
	if (scheduledProgram == program) {
		if (resumeProgramInternal(true)) {							// resumeAll so that we get back to scheduled program
        	LOG("Thermostat Program is ${program} (resumed)", 2, this, 'info')
			if (sendHoldType == 'nextTransition') {					// didn't want a permanent hold, so we are all done (don't need to set a new hold)
            runIn(3, refresh, [overwrite: false])
           		generateProgramEvent(program,'')
                def updates = [ 'lastHoldType': sendHoldType ]
        		generateEvent(updates)
           		return
        	}
        }
	} else {
       	resumeProgramInternal(true)							// resumeAll before we change the program
    }
  
    if ( parent.setProgram(this, program, deviceId, sendHoldType, sendHoldHours) ) {
     	runIn(3, refresh, [overwrite: true])

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
	String currentProgramName = device.currentValue('currentProgramName')
	resumeProgramInternal(resumeAll)
    if (currentProgramName.startsWith('Hold') || currentProgramName.startsWith('Auto ')) {
    	//def updates = ['thermostatOperatingState':'idle','equipmentOperatingState':'idle']
        //log.debug updates
        //generateEvent(updates)
        runIn(5, refresh, [overwrite:false])
    }
}
private def resumeProgramInternal(resumeAll=true) {
	def result = true
	String thermostatHold = device.currentValue('thermostatHold')
	if (thermostatHold == '') {
		LOG('resumeProgram() - No current hold', 2, null, 'info')
        sendEvent(name: 'resumeProgram', value: 'resume dis', descriptionText: 'resumeProgram is done', displayed: false, isStateChange: true)
		return result
	} else if (thermostatHold =='vacation') { // this shouldn't happen anymore - button changes to Cancel when in Vacation mode
		LOG('resumeProgram() - Cannot resume from Vacation hold', 2, null, 'error')
        sendEvent(name: 'resumeProgram', value: 'cancel', descriptionText: 'resumeProgram is done', displayed: false, isStateChange: true)
		return false
	} else {
		LOG("resumeProgram() - Hold type is ${thermostatHold}", 4)
	}
	
	sendEvent(name: 'thermostatStatus', value: 'Resuming scheduled Program...', displayed: false, isStateChange: true)
    
	def deviceId = getDeviceId()
    sendEvent(name: 'thermostatStatus', value: 'Resuming Scheduled Program', displayed: false, isStateChange: true)
	if (parent.resumeProgram(this, deviceId, resumeAll)) {	
        // if (resumeAll) generateProgramEvent(device.currentValue('scheduledProgramName'))
        sendEvent(name: "resumeProgram", value: "resume dis", descriptionText: "resumeProgram is done", displayed: false, isStateChange: true)
        sendEvent(name: "holdStatus", value: '', descriptionText: 'Hold finished', displayed: true, isStateChange: true)
        sendEvent(name: 'thermostatHold', value: '', displayed: false, isStateChange: true)
        sendEvent(name: 'thermostatStatus', value: 'Resume Program succeeded', displayed: false, isStateChange: true)
        LOG("resumeProgram(${resumeAll}) - succeeded", 2,null,'info')
        runIn(3, refresh, [overwrite:true])
	} else {
		sendEvent(name: "thermostatStatus", value: "Resume Program failed...", displayed: false, isStateChange: true)
		LOG("resumeProgram() - failed (parent.resumeProgram(this, ${deviceId}, ${resumeAll}))", 1, null, "error")
        result = false
	}
    // sendEvent(name: 'thermostatStatus', value: '', displayed: false, isStateChange: true)
    return result
}
def generateProgramEvent(String program, String failedProgram='') {
	LOG("Generate generateProgramEvent Event: program ${program}", 4)
    
	sendEvent(name: "thermostatStatus", value: "Setpoint updating...", displayed: false, isStateChange: true)

	String prog = program.capitalize()
    String progHold = (failedProgram == '') ? prog : "Hold: "+prog
    // actually have to look up the programId...or wait until Ecobee (Connect) sends us the correct one
    def updates = ['currentProgramName':progHold,/*'currentProgramId':program.toLowerCase(),*/'currentProgram':prog]
    generateEvent(updates)
    updateProgramButtons()
}

// ***************************************************************************
// Thermostat Fan Mode Commands
// Fan Mode CANNOT be changed while in Vacation mode
// ***************************************************************************
def setThermostatFanMode(String value, holdType=null, holdHours=2) {
	// N.B. if holdType is provided, it must be one of the valid parameters for the Ecobee setHold call (indefinite, nextTransition, holdHours). dateTime not currently supported
	LOG("setThermostatFanMode(${value})", 4)
	// "auto" "on" "circulate" "off"
    
    // Cannot change fan settings while in Vacation mode
    if (device.currentValue('thermostatHold') == 'vacation') {
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
    
    def currentProgramName = device.currentValue('currentProgramName')
    if ((device.currentValue('thermostatHold') != '') || currentProgramName.startsWith('Hold') || currentProgramName.startsWith('Auto ')) {
    	resumeProgramInternal(true)
    	refresh()
    }
    
    def currentFanMode = device.currentValue('thermostatFanMode') 		// value AFTER we reset the Hold (should really read the scheduledProgram's fan values)
    def updates = []
    def results
    def nullMinOnTime = null
    
    def sendHoldType = null
    def sendHoldHours = null
    if (holdType) {
    	sendHoldType = holdType
        if (holdType == 'holdHours') sendHoldHours = holdHours
    } else { 
		sendHoldType =  whatHoldType()
    	if (sendHoldType.isNumber()) {
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
        		updates = [thermostatFanMode:'on',thermostatOperatingState:'fan only',equipmentOperatingState:'fan only',currentProgramName:'Hold: Fan']
        		if ((currentFanMode != 'on') && (currentFanMode != 'off')) {	// i.e. if 'auto' or 'circulate', then turning on will cause a Hold: Fan event	
        			updates += [currentProgramName:'Hold: Fan']
            	} else {
            		updates += [currentProgramName:device.currentValue('scheduledProgramName')]	// In case currentProgram wasn't updated yet by the above resumeProgram()
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
            def thermostatMode = device.currentValue('thermostatMode')
            if (thermostatMode != 'off') {
            	LOG("Request to change Fan Mode to 'off' while Thermostat Mode isn't 'off' (${thermostatMode}), Fan Mode set to 'auto' instead",2,null,'warn')
                updates = [thermostatFanMode:device.currentValue('thermostatFanMode')]
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
        	def fanMinOnTime = device.currentValue('fanMinOnTime')
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
def fanModes() {
	["on", "auto", "circulate", "off"]
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
    if (device.currentValue('thermostatFanMode') != 'off') {
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
    def runWhen = parent.settings?.arrowPause.toInteger() ?: 4
    runIn( runWhen, 'sFMOT', [data: [mins:minutes]] )
}
void sFMOT(data) {
	LOG("Setting fan minutes to: ${data.mins}",4,null,'trace')
    setFanMinOnTime(data.mins)
}
void setFanMinOnTime(minutes=20) {
    if (device.currentValue('thermostatHold') == 'vacation') {
    	LOG("setFanMinOnTime() requested but thermostat is in Vacation mode, ignoring request",2,null,'warn')
        return
    }
	LOG("setFanMinOnTime(${minutes})", 4, null, "trace")
  	Integer howLong = 20	// default to 10 minutes, if no value supplied
	if (minutes.isNumber()) howLong = minutes.toInteger()
    def fanMinOnTime = device.currentValue('fanMinOnTime')
    LOG("Current fanMinOnTime: ${fanMinOnTime}, requested ${minutes}/${howLong}",3,null,'info')
    if (fanMinOnTime && (fanMinOnTime.toInteger() == howLong)) return // allready there - all done!
    
    def deviceId = getDeviceId()
    if ((howLong >=0) && (howLong <=  55)) {
		if (parent.setFanMinOnTime(this, deviceId, howLong)) {
        	def updates = [fanMinOnTime:howLong]
            def currentFanMode = device.currentValue('thermostatFanMode')
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

void setHumidifierMode(String value) {
    // verify thermostat hasHumidifier first
    def hasHumidifier = device.currentValue('hasHumidifier')
    if (!hasHumidifier || (hasHumidifier == 'false')) {
    	LOG("${device.displayName} is not controlling a humidifier", 1, null, 'warn')
        return
    }
	if (!value  || ((value != 'auto')  && (value != 'off') && (value != 'manual') && (value != 'on'))) {
    	LOG("setHumidifierMode(${value}) - unsupported humidifier mode requested")
        return
    }
    if (value == 'on') value = 'manual'
    if (device.currentValue('humidifierMode') == value) {
    	LOG("${device.displayName}'s humidifier is already set to ${value}", 2, null, 'info')
        return
    }
	def result = parent.setHumidifierMode(this, value, getDeviceId())
    if (result) {
    	LOG("${device.displayName}'s humidifier is now set to ${value}", 2, null, 'info')
        // generate events to update the device's display here (if any)
    } else {
    	LOG("Changing ${device.displayName}'s humidifier to ${value} failed, humidifier is still ${device.currentValue('humidifierMode')}", 1, null, 'warn')
    }
}

void setDehumidifierMode(String value) {
    // verify that the stat either hasDehumidifer or dehumidifyOvercoolOffset > 0 
    def hasDehumidifier = device.currentValue('hasDehumidifier')
    if (!hasDehumidifier || (hasDehumidifier == 'false')) {
    	LOG("${deviceId.displayName} is not controlling a dehumidifier or overcooling", 1, child, 'warn')
        return
    }
    
    if (!value || (value != 'off') && (value != 'on')) {
        LOG("setHumidifierMode(${value}) - unsupported dehumidifier mode requested")
        return
    }
    if (device.currentValue('dehumidifierMode') == value) {
    	LOG("${device.displayName}'s dehumidifier is already set to ${value}", 2, null, 'info')
    }
	def result = parent.setDehumidifierMode(this, value, getDeviceId())
    if (result) {
    	LOG("${device.displayName}'s dehumidifier is now set to ${value}", 2, null, 'info')
        // generate events to update the device's display here (if any)
    } else {
    	LOG("Changing ${device.displayName}'s dehumidifier to ${value} failed, dehumidifier is still ${device.currentValue('humidifierMode')}", 1, null, 'warn')
    }
}

// Vacation commands
void setVacationFanMinOnTime(minutes=0) {
    if (device.currentValue('thermostatHold') != 'vacation') {
    	LOG("setVacationFanMinOnTime() requested but thermostat is not in Vacation mode, ignoring request",2,null,'warn')
        return
    }
	LOG("setVacationFanMinOnTime(${minutes})", 5, null, "trace")
    Integer howLong = 0	// default to 0 minutes for vacations, if no value supplied
	if (minutes.isNumber()) howLong = minutes.toInteger()    
    def fanMinOnTime = device.currentValue('fanMinOnTime')
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
    if (device.currentValue('thermostatHold') != 'vacation') {
    	LOG("cancelVacation() - current hold is ${device.currentValue('thermostatHold')}",2,null,'trace')
        // Maybe we should do a resumeProgram first? But that could kill a Hold: if this is called while there is no vacation
    	return
    }
    def deviceId = getDeviceId()
    if (parent.deleteVacation(this, deviceId, null)) {
    	// call resumePogram
    	generateQuickEvent('resumeProgram', 'resume dis', 5)
    	generateQuickEvent('currentProgramName', device.currentValue('scheduledProgramName'))
        updateModeButtons()
    }
}

// No longer used as of v1.2.21
def generateSetpointEvent() {
	LOG('generateSetpointEvent called in error',1,null,'error')
	return
}

// This just updates the generic multiAttributeTile - text should match the Thermostat mAT
def generateStatusEvent() {
	LOG('generateStatusEvent called in error',1,null,'warn')
	return // no longer needed, alternate multiAttributeTile no longer necessary
/*    
	def mode = device.currentValue('thermostatMode')
	def heatingSetpoint = device.currentValue('heatingSetpoint')
	def coolingSetpoint = device.currentValue('coolingSetpoint')
	def temperature = device.currentValue('temperature')
    def operatingState = device.currentValue('thermostatOperatingState')

	def statusText	
    if (debugLevel(4)) {
		LOG("Generate Status Event for Mode = ${mode}", 4)
		LOG("Temperature = ${temperature}", 4)
		LOG("Heating setpoint = ${heatingSetpoint}", 4)
		LOG("Cooling setpoint = ${coolingSetpoint}", 4)
		LOG("HVAC Mode = ${mode}", 4)	
    	LOG("Operating State = ${operatingState}", 4)
    }

	if (mode == "heat") {
//		if (temperature >= heatingSetpoint) {
		if (operatingState == 'fan only') {
        	statusText = 'Fan Only'
        } else if (operatingState.startsWith('heat')) {
			statusText = 'Heating '
            if (operatingState.contains('sma')) {
            	statusText += '(Smart Recovery)'
            } else {
            	statusText += "to ${heatingSetpoint}°"
            }
		} else {
        	// asert operatingState == 'idle'
			statusText = "Heating at ${heatingSetpoint}°"
		}
	} else if (mode == "cool") {
//		if (temperature <= coolingSetpoint) {
		if (operatingState == 'fan only') {
        	statusText = 'Fan Only'
		} else if (operatingState.startsWith('cool')) {
        	statusText = 'Cooling '
            if (operatingState.contains('sma')) {
            	statusText += '(Smart Recovery)'
            } else if (operatingState.contains('ove')) {
            	statusText += '(Over Cooling)'
            } else {
            	statusText += "to ${coolingSetpoint}°"
            }
		} else {
			statusText = "Cooling at ${coolingSetpoint}°"
		}
	} else if (mode == 'auto') {
		if (operatingState == 'fan only') {
        	statusText = 'Fan Only'
    	} else if (operatingState.startsWith('heating')) {
        	statusText = 'Heating '
            if (operatingState.contains('sma')) {
            	statusText += '(Smart Recovery/Auto)'
            } else {
            	statusText += "to ${heatingSetpoint}° (Auto)"
            }
        } else if (operatingState.startsWith('cooling')) {
        	statusText = 'Cooling '
            if (operatingState.contains('sma')) { 
            	statusText += '(Smart Recovery/Auto)'
        	} else {
            	statusText += "to ${coolingSetpoint}° (Auto)"
            }
        } else {
			statusText = "Idle (Auto ${heatingSetpoint}°-${coolingSetpoint}°)"
        }
	} else if (mode == "off") {
		statusText = "Off"
	} else if (mode == "emergencyHeat" || mode == "emergency heat" || mode == "emergency") {
    	if (operatingState != "heating") {
			statusText = "Emergency Heat at ${heatingSetpoint}°"
		} else {
			statusText = "Emergency Heating to ${heatingSetpoint}°"
		}
	} else {
		statusText = "${mode}?"
	}
	LOG("Generate Status Event = ${statusText}", 4)
	sendEvent(name:"thermostatStatus", value:statusText, description:statusText, displayed: false, isStateChange: true)
    */
}

// generate custom mobile activity feeds event
// (Need to clean this up to remove as many characters as possible, else it isn't readable in the Mobile App
def generateActivityFeedsEvent(notificationMessage) {
	sendEvent(name: "notificationMessage", value: "${device.displayName} ${notificationMessage}", descriptionText: "${device.displayName} ${notificationMessage}", displayed: true, isStateChange: true)
}

def noOp(arg=null) {
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

private def cToF(temp) {
    return celsiusToFahrenheit(temp)
}
private def fToC(temp) {
    return fahrenheitToCelsius(temp)
}

private Double roundC(value) {
    return (((value.toDouble() * 2.0).toDouble().round(0)) / 2.0).toDouble().round(1)
}

//private def getImageURLRoot() {
//	return "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/dark/"
//}

private def getDeviceId() {
	def deviceId = device.deviceNetworkId.split(/\./).last()	
    LOG("getDeviceId() returning ${deviceId}", 4)
    return deviceId
}

private def usingSmartAuto() {
	LOG("Entered usingSmartAuto() ", 5)
	if (settings.smartAuto) { return settings.smartAuto }
    if (parent.settings.smartAuto) { return parent.settings.smartAuto }
    return false
}

// returns the holdType keyword, OR the number of hours to hold
// precedence: 1. device preferences, 2. parent settings.holdType, 3. indefinite (must specify to use the thermostat setting)
def whatHoldType() {
    def theHoldType = myHoldType
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
            if (myHoldHours && myHoldHours.isNumber()) {
            	sendHoldType = myHoldHours
            } else if ((parent.settings.holdType == 'Specified Hours') && parent.settings.holdHours && parent.settings.holdHours.isNumber()) {
            	sendHoldType = parent.settings.holdHours
            } else if ( parent.settings.holdType == '2 Hours') {
            	sendHoldType = 2
            } else if ( parent.settings.holdType == '4 Hours') {
            	sendHoldType = 4            
            } else {
            	sendHoldType = 2
            }
            break;
        case null :		// null means use parent value
            theHoldType = 'Parent'
        case 'Parent':
            parentHoldType = parent.settings.holdType
            if (parentHoldType && (parentHoldType == 'Thermostat Setting')) theHoldType = 'Thermostat'
        case 'Thermostat':
			if (theHoldType == 'Thermostat') {
       			def holdType = device.currentValue('statHoldAction')
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
                        if (parent.settings.holdHours && parent.settings.holdHours.isNumber()) {sendHoldType = parent.settings.holdHours} else {sendHoldType = 2}
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

private Integer howManyHours() {
	Integer sendHoldHours = 2
	if (holdHours && holdHours.isNumber()) {
    	sendHoldHours = holdHours.toInteger()
        LOG("Using ${device.displayName} holdHours: ${sendHoldHours}",2,this,'info')
    } else if (parent.settings.holdHours && (parent.settings.holdHours.isNumber())) {
    	sendHoldHours = parent.settings.holdHours.toInteger()
        LOG("Using ${parent.displayName} holdHours: ${sendHoldHours}",2,this,'info')
    }
    return sendHoldHours
}

private debugLevel(level=3) {
	Integer dbg = device.currentValue('debugLevel')?.toInteger()
	Integer debugLvlNum = dbg ? dbg : (parent.settings.debugLevel ? parent.settings.debugLevel.toInteger() : 3)
    return ( debugLvlNum >= level?.toInteger() )
}

private def LOG(message, level=3, child=null, logType="debug", event=false, displayEvent=false) {
	def prefix = debugLevel(5) ? 'LOG: ' : ''
    if (logType == null) logType = 'debug'
    
	if (debugLevel(level)) { 
    	log."${logType}" "${prefix}${message}"
        if (event) { debugEvent(message, displayEvent) }        
	}  
}

private def debugEvent(message, displayEvent = false) {
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
}
