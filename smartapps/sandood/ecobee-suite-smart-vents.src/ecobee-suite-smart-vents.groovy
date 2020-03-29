/**
 *  ecobee Suite Smart Vents
 *
 *  Copyright 2017-2020 Barry A. Burke
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
 * <snip>
 *	1.7.20 - Added support for HubConnected EcoVents and Keen Vents; optimized Keen Vent handling; new Fan Only percentage setting
 *	1.7.21 - Fix HubConnect EcoVent selector
 *	1.7.22 - Added optional Ecobee Programs exception & auto Sensor enrollments; show open percentage in label
 *	1.7.23 - Enable Smart Recovery handling when *nextClimate* is enabled (but currently running Program is not), default min/maxVentLevel to 1/98 for silent operation
 *	1.7.24 - Remove extraneous log.debug, fix app.label handling of levels, added 'percentage' as option for Paused
 *	1.7.25 - Fixed labels (again), fixed open contacts, and updated configuration layout, added intro ***
 *	1.7.26 - Integrated with Smart Room and ES Sensors
 *	1.7.27 - Added atomic updateSensorPrograms - add/remove in 1 call
 *	1.7.28 - Added reservation serializer for climate/sensor changes (program.climates)
 *	1.7.29 - Added Mode restriction
 *	1.7.30 - Fixed undefined "excluded" mode/climate vent setting
 *	1.7.31 - Added minimize UI
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - Option to close vents when contact open and fan only; fixed too many ES sensors error when 0 ES sensors, fix Pause warning
 *	1.8.03 - More busy bees
 *	1.8.04 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.05 - New SHPL, using Global Fields instead of atomicState
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.8.05" }
String getVersionLabel() 	{ return "Ecobee Suite Smart Vents & Switches Helper, version ${getVersionNum()} on ${getHubPlatform()}" }


definition(
	name: 				"ecobee Suite Smart Vents",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description:		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAutomates ${isST?'SmartThings':'Hubitat'}-controlled vents to meet a target temperature in a room.",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-vents.src/ecobee-suite-smart-vents.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-smart-vents-sa",
	singleInstance: 	false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
}

// Preferences Pages
def mainPage() {
	//boolean ST = isST
	//boolean HE = !ST
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
    def vc = 0			// vent counter
    def unit = temperatureScale
    String defaultName = "Smart Vents & Switches"
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This Helper dynamically adjusts the vent open percentage based on room temperature relative to an Ecobee Suite Thermostat's setpoints, or to pre-defined cooling & heating setpoints. "+
                          "It will also automatically register the room's Ecobee Suite Sensor(s) to the chosen ES Thermostat programs while active, and unregister while the Helper is Paused. "+
                          "There should be one instance of this Helper for each room with a ${getHubPlatform()}-controlled adjustable vent, ideally (but optionally) with an Ecobee Suite Sensor "+
                          "in the room (multiple sensors will be averaged).")
            }
        }
		section(title: sectionTitle("Naming${!settings.tempDisable?' & Sensor Selection':''}")) {
			String defaultLabel
			if (!atomicState?.appDisplayName) {
				defaultLabel = defaultName
				app.updateLabel(defaultName)
				atomicState?.appDisplayName = defaultName
			} else {
				defaultLabel = atomicState.appDisplayName
			}
			label(title: inputTitle("Name for this ${defaultName} Helper"), required: false, submitOnChange: true, defaultValue: defaultLabel, width: 6)
            if (!app.label) {
				app.updateLabel(defaultLabel)
				atomicState.appDisplayName = defaultLabel
			} else {
            	atomicState.appDisplayName = app.label
            }
			if (HE) {
				if (app.label.contains('<span ')) {
					if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains('<span ')) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
						String myLabel = app.label.substring(0, app.label.indexOf('<span '))
						atomicState.appDisplayName = myLabel
						app.updateLabel(myLabel)
					}
				} else {
                	atomicState.appDisplayName = app.label
                }
			} else {
				def opts = [' (paused', ' (open', ' (closed']
				String flag
				opts.each {
					if (!flag && app.label.contains(it)) flag = it
				}
				if (flag) {
					if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains(flag)) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
                        String myLabel = app.label.substring(0, app.label.indexOf(flag))
                        atomicState.appDisplayName = myLabel
                        app.updateLabel(myLabel)
                    }
                } else {
                	atomicState.appDisplayName = app.label
                }
            }
            if (settings?.tempDisable) { 
            	paragraph getFormat("warning","This Helper is temporarily paused...")
            } else {
            	if (maximize) paragraph("Select 1 or more temperature sensors, ideally at least 1 of which is an Ecobee Suite Sensor")
        		input(name: "theSensors", type:"capability.temperatureMeasurement", title: inputTitle("Select Temperature Sensor(s)"), required: true, multiple: true, submitOnChange: true)
				if (maximize && settings?.theSensors) paragraph "The current ${settings.theSensors?.size()>1?'average ':''}temperature for ${settings.theSensors?.size()==1?'this sensor':'these sensors'} is ${getAverageTemperature()}°"
            }
		}
        
        if (!settings.tempDisable && settings?.theSensors) {     
        	section(title: sectionTitle("Configuration")+(ST?"\nVents":'')) {
				if (HE) paragraph(smallerTitle("Vents"))
        		if (maximize) paragraph("Selected vents will be opened while the HVAC system is heating or cooling until target temperature is achieved, and then closed")
				input(name: "theEconetVents", type: "${ST?'device.econetVent':'device.EcoNetVent'}", title: inputTitle("Select EcoNet Vent(s)?"), multiple: true, submitOnChange: true, 
                	  hideWhenEmpty: true, required: (!settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
                if (settings.theEconetVents) vc = settings.theEconetVents.size()
                input(name: "theHCEcoVents", type: "${ST?'device.hubconnectEcovent':'device.HubConnectEcoVent'}", title: inputTitle("Select HubConnect EcoNet Vent(s)?"), multiple:true, 
                	  submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
            	if (settings.theHCEcoVents) vc = vc + settings.theHCEcoVents.size()
				input(name: "theKeenVents", type: "${ST?'device.keenHomeSmartVent':'device.KeenHomeSmartVent'}", title: inputTitle("Select Keen Home Smart Vent(s)?"), multiple:true, 
                	  submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
            	if (settings.theKeenVents) vc = vc + settings.theKeenVents.size()
                input(name: "theHCKeenVents", type: "${ST?'device.hubconnectKeenHomeSmartVent':'device.HubConnectKeenHomeSmartVent'}", title: inputTitle("Select HubConnect Keen Home Smart Vent(s)?"), multiple:true, 
                	  submitOnChange: true, hideWhenEmpty: true, required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theGenericVents && !settings.theGenericSwitches))
            	if (settings.theHCKeenVents) vc = vc + settings.theHCKeenVents.size()
                input(name: "theGenericVents", type: 'capability.switchLevel', title: inputTitle("Select Generic (dimmer) Vent(s)"), multiple: true, submitOnChange: true, hideWhenEmpty: true, 
                	  required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericSwitches))
                if (settings.theGenericVents) vc = vc + settings.theGenericVents.size()
				input(name: "theGenericSwitches", type: 'capability.switch', title: inputTitle("Select Switch(es)"), multiple: true, submitOnChange: true, hideWhenEmpty: true,
                	  required: (!settings.theEconetVents && !settings.theHCEcoVents && !settings.theKeenVents && !settings.theHCKeenVents && !settings.theGenericVents))
                if (settings.theGenericSwitches) vc = vc + settings.theGenericSwitches.size()
                def s = ((vc == 0) || (vc > 1)) ? 's' : ''
                if (maximize) paragraph "${vc} vent${s}/switch${s=='s'?'es':''} selected"
                
            	if (settings.theEconetVents || settings.theHCEcoVents || settings.theKeenVents || settings.theHCKeenVents || settings.theGenericVents ) {
            		if (maximize) paragraph('The default settings are optimized for silent operation of most vents (1-98). However, fully closing too many vents at once may be detrimental to your HVAC system. ' +
                    		  'You may want to increase the minimum closed percentage.')
            		input(name: "minimumVentLevel", type: "number", title: inputTitle("Minimum vent level when closed?"), required: true, defaultValue:1, description: '1', range: "0..100")
                    input(name: "maximumVentLevel", type: "number", title: inputTitle("Maximum vent level when open?"), required: true, defaultValue:98, description: '98', range: "0..100")
                    if (settings?.minimumVentLevel == null) {app.updateSetting('minimumVentLevel',1); settings?.minimumVentLevel = 1; }
                    if (settings?.maximumVentLevel == null) {app.updateSetting('maximumVentLevel',98); settings?.maximumVentLevel = 98; }
            	}
			}
            section(title: smallerTitle("Windows & Doors")) {
        		if (maximize) paragraph("Open Windows and Doors will temporarily deactivate (close) the vent${vc>1?'s':''}, except during 'Fan Only'")
            	input(name: "theWindows", type: "capability.contactSensor", title: inputTitle("Monitor these Window/Door contact sensor(s)?")+'(optional)', required: false, multiple: true)
                input(name: "closeFanOnly", type: "bool", title: "Also close during Fan Only?", required: false, defaultValue: false, submitOnChange: true)
			}
            section(title: smallerTitle("Thermostat")) {
				if (maximize) paragraph("Specify which Ecobee Suite Thermostat to monitor for operating state, mode and setpoint change events")
				input(name: "theThermostat", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select Ecobee Suite Thermostat"),
					  multiple: false, required: true, submitOnChange: true)
            	if (maximize) paragraph "If you want the vents to be adjusted at any time, based entirely on the room temperature and target setpoints (ignoring the thermostat's operating state), enable this option"
            	input(name: "adjustAlways", type: 'bool', title: inputTitle("Always adjust vents/switches?"), defaultValue: false, submitOnChange: true)
                if (settings.adjustAlways && maximize) paragraph "NOTE: this setting does not override the Modes or Programs restrictions below"
            }
			if (settings?.theThermostat) {
            section(title: smallerTitle("Modes")) {
					def modes = getThermostatModes()
					paragraph
					input(name: 'theModes', type: 'enum', title: inputTitle("Make vent adjustments only during these thermostat Modes")+' (optional)', required: false, submitOnChange: true,
						  multiple: true, options: modes, width: 6)
					if (settings?.theModes) {
						input(name: 'notModeState', type: 'enum', title: inputTitle("Vent state for excluded Modes?"), required: true, submitOnChange: true, defaultValue: 'unchanged',
							  options: ['open', 'closed', 'percentage', 'unchanged'], multiple: false, width: 3)
						if (notModeState == 'percentage') {
							input(name: 'notModeLevel', type: "number", title: inputTitle("Vent level for excluded Modes?"), required: true, defaultValue:50, description: '50', range: "0..100", width: 3)
						}
					}
				}
				section(title: smallerTitle("Programs")) {
					def programs = getThermostatPrograms()
					programs = programs + ["Vacation"]
					input(name: "theClimates", type: 'enum', title: inputTitle("Make vent adjustments only during these thermostat Programs")+" (optional)", required: false, submitOnChange: true, 
						  multiple: true, options: programs, width: 6)
					if (settings?.theClimates && (settings?.theClimates.size() != programs.size())) {
						input(name: 'notClimateState', type: 'enum', title: inputTitle("Vent state for excluded Programs?"), required: true, submitOnChange: true, defaultValue: 'unchanged',
							  options: ['open', 'closed', 'percentage', 'unchanged'], multiple: false, width: 3)
						if (notClimateState == 'percentage') {
							input(name: 'notClimateLevel', type: "number", title: inputTitle("Vent level for excluded Programs?"), required: true, defaultValue:50, description: '50', range: "0..100", width: 3)
						}
					}

					if (settings.theSensors) {
						def ecobeeSensors = []
						settings?.theSensors.each { 
							if (it.hasCommand('updateSensorPrograms')) {
								ecobeeSensors << it
							}
						}
						if (ecobeeSensors?.size() == 1) {
							if (settings.theClimates) input(name: "enrollClimates", type: 'bool', title: inputTitle("Automatically include sensor ${ecobeeSensors[0].displayName} in the above Programs?"),
															defaulValue: true, submitOnChange: true, width: 12)     
							if (settings.enrollClimates && settings.theClimates) {
								def notTheseClimates = programs - theClimates - ['Vacation']
								//def these = settings.theClimates.contains(['Vacation']) ? settings.theClimates - ['Vacation'] : settings.theClimates
								def these = settings.theClimates - ['Vacation']
								String notThese = ''
								if (notTheseClimates) notThese = " and removed from (" + notTheseClimates.toString()[1..-2].replace(',',', ') + ')'
								if (maximize) paragraph "${ecobeeSensors[0].displayName} will be added to (${these.toString()[1..-2].replace('"','').replace(',',', ')})${notThese} for ${theThermostat.displayName}. " +
																"It will also be removed from ALL Programs when this Helper is Paused."
							}
						} else {
							if (ecobeeSensors == []) paragraph("You have selected more than 1 Ecobee Suite Sensor. At this time, this Helper only supports automatically enrolling a single sensor into Ecobee Programs.")
							app.updateSetting('enrollClimates', false)
							settings.enrollClimates = false
						}
					}
				}
		
				section(title: smallerTitle("Thermostat Setpoints")) {
					def cSetpoint
					def cHeatSetpoint
					def cCoolSetpoint
					def cMode
					def cProgram
					def cLastRunMode

					input(name: "useThermostat", type: "bool", title: inputTitle("Follow the setpoints on ${settings?.theThermostat?settings.theThermostat.displayName:'the thermostat'}?"), required: true, 
						  defaultValue: true, submitOnChange: true, width: 6)
					if (settings.useThermostat == true) {
						settings.useVirtualStat = false
						app.updateSetting('useVirtualStat', false)
					}
					def heatAt = null
					def coolAt = null
					//if (!settings?.useThermostat) {
						input(name: "useVirtualStat", type: "bool", title: inputTitle("Follow the setpoints on a different thermostat?"), width: 6, required: true, 
							  defaultValue: !settings.useThermostat, submitOnChange: true)
						if (settings?.useVirtualStat == true){
							settings.usrThermostat = false
							app.updateSetting('useThermostat', false)
							input(name: "theVirtualStat", type: "capability.thermostat", title: inputTitle("Select a thermostat"),  multiple: false, required: true, submitOnChange: true)
						}
					//}
					if (settings?.useThermostat && settings?.theThermostat) {
						cSetpoint = 	ST ? settings.theThermostat.currentValue('thermostatSetpoint') 	: 	settings.theThermostat.currentValue('thermostatSetpoint', true)
						cHeatSetpoint = ST ? settings.theThermostat.currentValue('heatingSetpoint') 	: 	settings.theThermostat.currentValue('heatingSetpoint', true)
						cCoolSetpoint = ST ? settings.theThermostat.currentValue('coolingSetpoint') 	: 	settings.theThermostat.currentValue('coolingSetpoint', true)
						cMode = 		ST ? settings.theThermostat.currentValue('thermostatMode') 		: 	settings.theThermostat.currentValue('thermostatMode', true)
						cProgram = 		ST ? settings.theThermostat.currentValue('currentProgram') 		: 	settings.theThermostat.currentValue('currentProgram', true)
						cLastRunMode = 	ST ? settings.theThermostat.currentValue('lastRunningMode') 	: 	settings.theThermostat.currentValue('lastRunningMode', true)
						if (maximize) paragraph("${settings.theThermostat.displayName} is in '${cMode}' mode running the '${cProgram}' program. The heating setpoint is ${cHeatSetpoint}°${unit}, " +
								  "the cooling setpoint is ${cCoolSetpoint}°${unit}, and the last operation was '${cLastRunMode}'")
					} else if (settings?.useVirtualStat && settings?.theVirtualStat) {
						cSetpoint = 	ST ? settings.theVirtualStat.currentValue('thermostatSetpoint') : 	settings.theVirtualStat.currentValue('thermostatSetpoint', true)
						cHeatSetpoint = ST ? settings.theVirtualStat.currentValue('heatingSetpoint') 	: 	settings.theVirtualStat.currentValue('heatingSetpoint', true)
						cCoolSetpoint = ST ? settings.theVirtualStat.currentValue('coolingSetpoint') 	: 	settings.theVirtualStat.currentValue('coolingSetpoint', true)
						cMode = 		ST ? settings.theVirtualStat.currentValue('thermostatMode') 	: 	settings.theVirtualStat.currentValue('thermostatMode', true)
						cProgram = 		ST ? settings.theVirtualStat.currentValue('schedule') 			: 	settings.theVirtualStat.currentValue('schedule', true)
						//cLastRunMode = 	ST ? settings.theVirtualStat.currentValue('lastRunningMode') 	: 	settings.theVirtualStat.currentValue('lastRunningMode', true)
						if (maximize) paragraph("${settings.theVirtualStat.displayName} is in '${cMode}' mode${(cProgram && (cProgram!='null')) ? ' running the \''+cProgram+'\' program':''}. The heating setpoint is ${cHeatSetpoint}°${unit} " +
								  "and the cooling setpoint is ${cCoolSetpoint}°${unit}")
					}
					/////////////////////////////////////////////
					//
					// TODO
					// Do I need a setting for non-Program holds (e.g., Temp, Auto, etc.)
					//
					////////////////////////////////////////////
					if (!settings?.useThermostat && !settings?.useVirtualStat) {  
						paragraph "\n"+smallerTitle("Manual Setpoints")
						input(name: "heatingSetpoint", type: "decimal", title: inputTitle("Target heating setpoint?"), required: true, submitOnChange: true, width: 4)
						input(name: "coolingSetpoint", type: "decimal", title: inputTitle("Target cooling setpoint?"), required: true, submitOnChange: true, width: 4)
						if (settings.heatingSetpoint) heatAt = settings.heatingSetpoint
						if (settings.coolingSetpoint) coolAt = settings.coolingSetpoint
					} else {
						if (HE) {
							paragraph(smallerTitle("Setpoint Offsets"))
							if (maximize) paragraph("Setpoint offsets are ADDED to the current thermostat's heating/cooling setpoint. Use negative numbers to reduce the target setpoint, positive to increase it.", width: 9)
						} else paragraph(title: "Setpoint Offsets", (maximize?"Setpoint offsets are ADDED to the current thermostat's heating/cooling setpoint. Use negative numbers to reduce the target setpoint, positive to increase it.":''), width: 9)
						input(name: "heatOffset", type: "decimal", title: inputTitle("Heating Setpoint Offset?"), defaultValue: 0.0, required: true, range: "-10..10", submitOnChange: true, width: 4)
						input(name: "coolOffset", type: "decimal", title: inputTitle("Cooling Setpoint Offset?"), defaultValue: 0.0, required: true, range: "-10..10", submitOnChange: true, width: 4)
						if (!settings?.heatOffset) {settings.heatOffset = 0.0; app.updateSetting('heatOffset', 0.0); }
						if (!settings?.coolOffset) {settings.coolOffset = 0.0; app.updateSetting('coolOffset', 0.0); }
						if (cHeatSetpoint && (settings.heatOffset != null)) heatAt = cHeatSetpoint + settings.heatOffset
						if (cCoolSetpoint && (settings.coolOffset != null)) coolAt = cCoolSetpoint + settings.coolOffset
						if (heatAt && coolAt && maximize) paragraph "In the '${cProgram}' program, the vent${vc>1?'s':''} will open when the observed ambient temperature at the selected " +
													"sensor${settings.theSensors?.size()>1?'s':''} is less than ${heatAt}°${unit} or more than ${coolAt}°${unit}"
						def overCool
						def overCoolOffset
						if (settings?.theThermostat) {
							overCool = theThermostat.currentValue('dehumidifyWithAC')
							overCoolOffset = theThermostat.currentValue('dehumidifyOvercoolOffset')
							overCool = ((overCool != null) && ((overCool == true) || (overCool == 'true'))) ? true : false
							if (overCool && !overCoolOffset) overCool = false 	// if no offset, or offset is 0.0, then we're not overcooling
							if (overCool) {
								if (maximize) paragraph "${theThermostat.displayName} is configured to dehumidify using the HVAC to over-cool as much as ${overCoolOffset}°${temperatureScale} lower than the cooling setpoint"
								input(name: "overCoolToo", type: "bool", title: "Would you like to overcool this room also?", required: true, defaultValue: false, submitOnChange: true, width: 6)
							}
						}
					}
				}
			}
            section(title: smallerTitle("Fan Only State")) {
				//if (false) input(name: 'closedFanOnly', type: 'bool', title: "Close the vent${vc>1?'s':''} while HVAC is 'fan only'?", defaultValue: false)
                String foDefault = (settings?.closedFanOnly != null) ? (settings.closedFanOnly ? 'closed' : 'unchanged') : 'unchanged'
                input(name: 'fanOnlyState', type: 'enum', title: inputTitle("Vent state for 'Fan Only' operation?"), required: true, submitOnChange: true, defaultValue: foDefault,
                	  options: ['open', 'closed', 'percentage', 'unchanged'], multiple: false, width: 5)
                if (fanOnlyState == 'percentage') {
                	input(name: 'fanOnlyLevel', type: "number", title: inputTitle("Fan Only Vent level?"), required: true, defaultValue:50, description: '50', 
						  range: "0..100", submitOnChange: true, width: 4)
                }
			}
        }
        section( title: sectionTitle("Operations")) {
			input(name: "minimize", type: "bool", title: inputTitle("Minimize settings text"), defaultValue: false, submitOnChange: true, width: 3)
            input(name: "tempDisable", title: inputTitle("Pause this Helper"), type: "bool", defaultValue: false, submitOnChange: true, width: 3)
            if (settings.tempDisable) {
                if (settings.theEconetVents || settings.theHCEcoVents || settings.theKeenVents || settings.theHCKeenVents || settings.theGenericVents || settings.theGenericSwitches) {
                    input(name: 'disabledVents', type: 'enum', title: inputTitle('Paused vent state'), options:['open': 'open/on','closed': 'closed/off','percentage': 'percentage','unchanged': 'unchanged'], 
                          required: true, multiple: false, defaultValue: 'closed', submitOnChange: true, width: 3)
                    if (disabledVents == 'percentage') {
                        input(name: 'disabledLevel', type: "number", title: inputTitle("Paused Vent level?"), required: true, defaultValue:50, description: '50', range: "0..100", submitOnChange: true, width: 3)
                    } else if (HE) paragraph("", width: 3)
                }
            }
			input(name: "debugOff", title: 	inputTitle("Disable debug logging"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
            input(name: "infoOff", title: 	inputTitle("Disable info logging"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
        }         
		// Standard footer
        if (ST) {
        	section(getVersionLabel().replace('er, v',"er\nV")+"\n\nCopyright \u00a9 2017-2020 Barry A. Burke\nAll rights reserved.\n\nhttps://github.com/SANdood/Ecobee-Suite") {}
        } else {
        	section() {
        		paragraph(getFormat("line")+"<div style='color:#5BBD76;text-align:center'>${getVersionLabel()}<br><small>Copyright \u00a9 2017-2020 Barry A. Burke - All rights reserved.</small><br>"+
                		  "<a href='https://github.com/SANdood/Ecobee-Suite' target='_blank' style='color:#5BBD76'><u>Click here for the Ecobee Suite GitHub Repository</u></a></div>")
            }
		}
    }
}

// Main functions
void installed() {
	LOG("Installed with settings: ${settings}", 4, null, 'trace')
    initialize()
}
void updated() {
	LOG("Updated with settings: ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    initialize()
}
void uninstalled() {
	// generateSensorsEvents([doors:'default', windows:'default', vents:'default',SmartRoom:'default'])
    unenrollSensors()
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
    //boolean ST = isST
    //boolean HE = !ST

    // Housekeeping
    if (settings.closedFanOnly != null) {
    	if (settings.fanOnlyState == null) {
        	def vs = settings.closedFanOnly ? 'closed' : 'unchanged'
            settings.fanOnlyState = vs
            app.updateSetting('fanOnlyState', vs)
        }
        settings.closedFanOnly = null
        if (HE) app.removeSetting('closedFanOnly')
    }
    atomicState.version = getVersionLabel()
    atomicState.scheduled = false
    // Now, just exit if we are disabled...
	if (settings.tempDisable) {
        if (settings.disabledVents && (settings.disabledVents != 'unchanged')) {
			if (settings.disabledVents == 'open') {
            	setTheVents('open')
            } else if (settings.disabledVents == 'closed') {
            	setTheVents('closed')
            } else if (settings.disabledVents == 'percentage') {
            	setTheVents(settings.disabledLevel ?: 50)
            }
            LOG("Temporarily Paused, setting vents to (${atomicState.ventState} ${atomicState.ventLevel})", 3, null, 'info')
        } else {
        	LOG("Temporarily Paused, vents unchanged", 3, null, 'info')
        }
		updateMyLabel()
        if (settings.theClimates && settings.enrollClimates) {
        	log.trace "initialize(): Calling unenrollSensors()"
        	unenrollSensors()
        }
        return true
    }
	if (settings.debugOff) log.info "log.debug() logging disabled"
	
    // Ecobee sensors are special: we update the vent status there if SmartRoom is enabled on them
    if (settings.theSensors) {
        def ecobeeSensors = []
        settings.theSensors.each { 
           	if (it.hasAttribute('SmartRoom')) {
              	ecobeeSensors << it.deviceNetworkId
            }
        }
        atomicState.ecobeeSensors = ecobeeSensors
    }
    
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])

	
    subscribe(settings.theSensors, 			'temperature', 				changeHandler, [filterEvents: true])	
	subscribe(settings.theThermostat, 		'thermostatOperatingState', changeHandler, [filterEvents: true])
    //subscribe(theThermostat, 				'temperature', 				changeHandler)
	subscribe(settings.theThermostat,		'thermostatMode', 			changeHandler, [filterEvents: true])
    //subscribe(theThermostat,				'currentProgram',			changeHandler)
    subscribe(settings.theVents, 			'level', 					changeHandler, [filterEvents: true])	// In case someone changes them on us
    subscribe(settings.theVents, 			'switch', 					changeHandler, [filterEvents: true])
	if (settings.theWindows) {
    	subscribe(settings.theWindows, 		'contact',					changeHandler, [filterEvents: true])
    }
    if (settings.useThermostat) {
    	subscribe(settings.theThermostat,	'heatingSetpoint', 			changeHandler, [filterEvents: true])
        subscribe(settings.theThermostat,	'coolingSetpoint', 			changeHandler, [filterEvents: true])
    //  subscribe(theThermostat, 			'thermostatSetpoint', 		changeHandler)
    } else if (settings.useVirtualStat) {
    	subscribe(settings.theVirtualStat,	'thermostatOperatingState', changeHandler, [filterEvents: true])
		subscribe(settings.theVirtualStat,	'thermostatMode', 			changeHandler, [filterEvents: true])
    	subscribe(settings.theVirtualStat,	'heatingSetpoint', 			changeHandler, [filterEvents: true])
        subscribe(settings.theVirtualStat,	'coolingSetpoint', 			changeHandler, [filterEvents: true])
    }
    
    // These don't change much, and we need them frequently, so stash them away and track their changes
    atomicState.heatDifferential = 0.0	// NO LONGER USED theThermostat.currentValue('heatDifferential')
    atomicState.coolDifferential = 0.0	// theThermostat.currentValue('coolDifferential') 
    def dehumidifyWithAC = theThermostat.currentValue('dehumidifyWithAC')
    def overcoolOffset = theThermostat.currentValue('dehumidifyOvercoolOffset') ?: 0.0
    if ((overcoolOffset && (overcoolOffset != 0.0)) && dehumidifyWithAC && ((dehumidifyWithAC == true) || (dehumidifyWithAC == 'true'))) {
    	atomicState.dehumidifyWithAC = true
        atomicState.dehumidifyOvercoolOffset = overcoolOffset
    } else {
    	atomicState.dehumidifyWithAC = false
        atomicState.dehumidifyOvercoolOffset = 0.0
    }
        
    //subscribe(theThermostat, 'heatDifferential', atomicHandler)
    //subscribe(theThermostat, 'coolDifferential', atomicHandler)
    subscribe(theThermostat, 				'dehumidifyOvercoolOffset', atomicHandler)
    subscribe(theThermostat, 				'dehumidifyWithAC', atomicHandler)
    
	atomicState.currentStatus = [:]
	atomicState.heatOrCool = null

	if (settings.theClimates && settings.enrollClimates) {
    	log.trace "initialize(): Calling enrollSensors()"
    	enrollSensors()
    }
    
    if (atomicState.lastLevel == null) {
    	String ventState = atomicState.ventState
    	if (ventState != null) {
        	ventState = ventState.isNumber()? "${ventState}%" : (ventState == 'open'? "${settings.maximumVentLevel}%" : "${settings.minimumVentLevel}%")
            atomicState.lastLevel = ventState
        }
    }
    
    setTheVents(checkTemperature())
	updateMyLabel()
    return true
}

void atomicHandler(evt) {
	def value = evt.value
    if 		(value == 'true') 	value = true
    else if (value == 'false') 	value = false
    else if (value == 'null')	value = ""
	atomicState."${evt.name}" = value		// Clever, no?
}
// The following code handles the intricacies of changing the ecobee program.climates maps, without stepping on other Helpers that
// might also be doing the same. This is necessary because adding/removing sensors to climates requires modifying and resending the
// entire programs.* Map - we need to make sure changes are serialized and atomic...
def enrollSensors() {
	if (settings.theClimates.size() != 0) {
	    settings.theSensors.each { sensor ->
        	// N.B there can only be 1 ES Sensor
            if (sensor.hasCommand('updateSensorPrograms')) {
              	List notPrograms = getThermostatPrograms() - settings.theClimates
    			LOG(("enrolling ${sensor.displayName} in ${settings.theClimates.toString()[1..-2].replaceAll('"','').replaceAll(',',', ')}"+(notPrograms?" and unenrolling from ${notPrograms.toString()[1..-2]}":'')),3,null,'info')
                String tid = sensor.currentValue('thermostatId').toString()
                if (needClimateChange( sensor, settings.theClimates, notPrograms )) {
                	boolean anyReserved = anyReservations( tid, 'programChange' )
                    if (!anyReserved || haveReservation( tid, 'programChange' )) {
                    	// Nobody has a reservation, or the reservation is mine
                    	if (!anyReserved) makeReservation(tid, 'programChange')	
						makeClimateChange( sensor, settings.theClimates, notPrograms)
                        if (!notPrograms) notPrograms = ['(none)']
                    	LOG("Sensor ${sensor.displayName} added to ${settings.theClimates.toString()[1..-2].replaceAll('"','').replaceAll(',',', ')} and removed from ${notPrograms.toString()[1..-2]}",3,null,'debug')
                	} else {
            			// somebody else has a reservation - we have to wait
                    	atomicState.pendedUpdates = [add: settings.theClimates, remove: notPrograms]
                		subscribe(sensor, 'climatesUpdated', programWaitHandler)
                        if (!notPrograms) notPrograms = ['(none)']
                    	LOG("Delayed: Sensor ${sensor.displayName} will be added to ${settings.theClimates.toString()[1..-2].replaceAll('"','').replaceAll(',',', ')} and removed from ${notPrograms.toString()[1..-2]} when pending changes complete",2,null,'info')
                    }
            	} else {
                	// No changes required
                    cancelReservation( tid, 'programChange' )
                }
                return	// only does the first Ecobee Sensor
            }
        } 
    }
}
def unenrollSensors() {
    settings.theSensors.each { sensor ->
    	// N.B., there can only be 1 ES Sensor
    	if (sensor.hasCommand('updateSensorPrograms')) {
        	List programs = getThermostatPrograms()
            if (needClimateChange( sensor, [], programs)) {
            	LOG("unenrolling ${sensor.displayName} from all programs (${programs.toString()[1..-2]})",3,null,'info')
        		String tid = sensor.currentValue('thermostatId').toString()
            	boolean anyReserved = anyReservations( tid, 'programChange' )
            	if (!anyReserved || haveReservation( tid, 'programChange' )) {
                	// Nobody has a reservation, or the reservation is mine
                	if (!anyReserved) makeReservation( tid, 'programChange' )
                    makeClimateChange( sensor, [], programs )
       		     	LOG("Sensor ${sensor.displayName} removed from ${programs.toString()[1..-2]}",3,null,'debug')
                } else {
                    // somebody else has a reservation - we have to wait
                    atomicState.pendedUpdates = [add: [], remove: programs]
                    subscribe(sensor, 'climatesUpdated', programWaitHandler)	// wait until somebody updates the climates
                    LOG("Delayed: Sensor ${sensor.displayName} will be removed from ${programs.toString()[1..-2]} when pending changes complete", 3, null, 'info')
                }
        	} else {
            	// No active climates = nothing to do except cleanup
                LOG("no active programs",3,null,'info')
                cancelReservation(tid, 'programChange')
        	}
            return	// Only does the first Ecobee Sensor...
        }
    }
}
def programUpdateHandler(evt) {
	// Clear our reservation once we know that the Ecobee Cloud has updated our thermostat's climates
    cancelReservation(evt.device.currentValue('thermostatId') as String, 'programChange')
    unschedule(clearReservation)
    unsubscribe(evt.device)
    if (!settings?.tempDisable) subscribe(evt.device, 'temperature', changeHandler, [filterEvents: true])
    def pendingRequest = atomicState.updateSensorRequest
    if (pendingRequest != null) {
    	atomicState.updateSensorRequest = null
	   	LOG("${pendingRequest} operation completed",3,null,'info')
    }
}
def programWaitHandler(evt) {
    unsubscribe(evt.device)
    if (!settings?.tempDisable) subscribe(evt.device, 'temperature', changeHandler, [filterEvents: true])
    String tid = evt.device.currentValue('thermostatId') as String
    def count = countReservations(tid, 'programChange')
    if ((count > 0) && !haveReservation( tid, 'programChange' )) {
    	atomicState.programWaitCounter = 0
    	runIn(5, checkReservations, [overwrite: true, data: [tid:tid, type:'programChange']])
        LOG("programWaitHandler(): There are still ${count} reservations for 'programChange', waiting...", 3, null, 'debug')
    } else {
    	makeReservation(tid, 'programChange')
        LOG("programWaitHandler(): 'programChange' reservation secured, sending pended updates", 3, null, 'debug')
    	doPendedUpdates()
    }
}
void checkReservations(data) {
    def count = countReservations(data.tid, data.type)
    def counter = atomicState.programWaitCounter
	if ((count > 0) && !haveReservation(data.tid, data.type)  && (counter <= 60)) {	// Try for up to 5.0 minutes... others will clear their reservations after 2.5 minutes
    	// Need to wait longer
        runIn(5, checkReservations, [overwrite: true, data: [tid: (data.tid), type: (data.type)]])
        counter++
        atomicState.programWaitCounter = counter
        if ((counter % 12) == 0) runIn(2, doRefresh, [overwrite: true])	// force a refresh every minute if we don't get updated
        LOG("checkReservations(): There are still ${count} reservations for 'programChange', waiting...", 3, null, 'debug')
    } else {
    	makeReservation(data.tid, data.type)
    	atomicState.programWaitCounter = 0
        LOG("checkReservation()(): 'programChange' reservation secured, sending pended updates", 3, null, 'debug')
        doPendedUpdates()
    }
}
void clearReservation() {
	settings.theSensors.each { sensor ->
        // N.B., there can only be 1 ES Sensor
        if (sensor.hasCommand('updateSensorPrograms')) {
        	def tid = sensor.currentValue('thermostatId') as String
            cancelReservation(tid, 'programChange')
        }
    }
}
void doRefresh() {
	settings?.theThermostat?.doRefresh(true) // do a forced refresh
}
void doPendedUpdates() {
	LOG("doPendedUpdates() @ ${now()}: ${atomicState.pendedUpdates}",4,null,'debug')
    def updates = atomicState.pendedUpdates
    if (updates?.remove || updates?.add) {
    	// Find the sensor
        settings.theSensors.each { sensor ->
    		// N.B., there can only be 1 ES Sensor
            if (sensor.hasCommand('updateSensorPrograms')) {
            	if (needClimateChange( sensor, updates.add, updates.remove )) {
					makeClimateChange( sensor, updates.add, updates.remove )
                } else {
                    // Nothing to do - release the reservation now
                    def tid = sensor.currentValue('thermostatId') as String
                    cancelReservation(tid, 'programChange')
                }
            }
        }
        atomicState.pendedUpdates = null
    }            	
}
def makeClimateChange( sensor, adds, removes ) {
    subscribe( sensor, 'activeClimates', programUpdateHandler )
    atomicState.updateSensorRequest = adds ? 'enroll' : 'unenroll'
    sensor.updateSensorPrograms( adds, removes)
    runIn(150, clearReservation, [overwrite: true])		// failsafe/watchdog - clear the reservation if we don't hear back within 2.5 minutes
    if (!adds) {
        LOG("Sensor ${sensor.displayName} removed from ${removes.toString()[1..-2]}",3,null,'info')
    } else {
    	if (!removes) removes = ['(none)']
        LOG("Sensor ${sensor.displayName} added to ${adds.toString()[1..-2]} and removed from ${removes.toString()[1..-2]}",3,null,'info')
    }
    // programUpdateHandler will release the reservation for us
}
boolean needClimateChange(sensor, List adds, List removes) {
	if (!adds && !removes) return false
    String ac = ST ? sensor.currentValue('activeClimates') : sensor.currentValue('activeClimates', true)
    def activeClimates = ac ? ((ac == '[]') ? [] : ac[1..-2].tokenize(', ').sort(false)) : []
    log.debug "activeClimates: ${activeClimates}"
    boolean updatesToDo = false
    if (!activeClimates) {
        // Easy one: no active climates, and we have climates to add
        if (adds) {
            updatesToDo = true
        }
    } else { // we have some activeClimates - do we need to adjust them?
        if (!adds && removes) {	
            // Easy one: there are active climates, and we aren't adding any, thus we know that we are removing ALL of them
            updatesToDo = true
        } else {
            // Hardest one: we have some active climates, and we have some adds and some removes to do - figure out if we need to change anything
            activeClimates.each { climate ->
                if (!updatesToDo) {
                    if (adds) {
                        // are there any active climates that we don't want active?
                        if (!adds.contains(climate)) updatesToDo = true		// need to remove at least one
                    } else if (removes) {
                        // or any active climates that we want inactive?
                        if (removes.contains(climate)) updatesToDo = true	// need to remove at least one
                    }
                }
            }
        }
    }
	return updatesToDo
}
//@Field Random rand = new Random()
//@Field String lastEvent = "foo"
void changeHandler(evt) {
	//String thisEvent = evt.id
	LOG("changeHandler(): ${evt.displayName} ${evt.name} ${evt.value}, ${evt.id}", 4, null, 'debug')
    //def lastEvent = atomicState.lastEvent
//    if (evt.id != lastEvent) {
//    	lastEvent = evt.id
//    	int randomSeconds = rand.nextInt(40)
    	//log.debug "randomSeconds: ${randomSeconds}"
//        log.debug "new"
		updateTheVents()
		runIn( 2, checkAndSet, [overwrite: true])		// collapse multiple inter-related events into a single thread
    	//atomicState.lastEvent = thisEvent
//    } else {
//    	log.debug "dup"
//    }
}
void checkAndSet() {
	if (!atomicState.version || (atomicState.version != getVersionLabel())) {
    	LOG('Helper version changed, re-initializing...',1,null,'info')
    	updated()
    }
//    log.debug "c&s"
	setTheVents(checkTemperature())
}
String checkTemperature() {
	//boolean ST = isST
    
    // Check if we're supposed to do anything during the currently active Ecobee Program (or the upcoming Program if in Smart Recovery)
    if (settings?.theClimates) {
    	def currentProgram = ST ? theThermostat.currentValue('currentProgram') 		: theThermostat.currentValue('currentProgram', true)
        if (!settings.theClimates.contains(currentProgram)) {
            // If we are in (Smart Recovery), check to see if we're supposed to be adjusting during the upcoming program
            def nextProgram = ST ? theThermostat.currentValue('nextProgramName') 	: theThermostat.currentValue('nextProgramName', true)
            if (!nextProgram || !settings.theClimates.contains(nextProgram)) {
                LOG("${theThermostat.displayName} is currently running a Program (${currentProgram}) that we're not configured for (${settings.theClimates.toString()[1..-2].replaceAll('"','').replaceAll(',',', ')})",3,null,'info')
                if (settings.notClimateState) {
                    if (settings.notClimateState != 'percentage') {
                        return settings.notClimateState
                    } else {
                        return settings.notClimateLevel ?: 50
                    }
                } else {
                	return 'unchanged'
                }
            }
        }
    }
    
    if (settings.theModes) {
        def currentMode = 	 ST ? theThermostat.currentValue('thermostatMode') 		: theThermostat.currentValue('thermostatMode', true)
        if (!settings.theModes.contains(currentMode)) {
        	 LOG("${theThermostat.displayName} is currently running a Mode (${currentMode}) that we're not configured for (${settings.theModes.toString()[1..-2]})",3,null,'info')
            if (settings.notModeState) {
                if (settings.notModeState != 'percentage') {
                    return settings.notModeState
                } else {
                    return settings.notModeLevel ?: 50
                }
            } else {
            	return 'unchanged'
            }
        }
    }
    
    def lastHVAC = atomicState.lastHVAC
    
    def cTemp = getAverageTemperature()
    def vents = 'unchanged'			// if not heating/cooling/fan, then no change to current vents
    if (cTemp != null) {		// only if valid temperature readings (Ecosensors can return "unknown")
        // Be smarter if we are in Smart Recovery mode: follow the thermostat's temperature instead of watching the current setpoint. Otherwise the room won't get the benefits of heat/cool
        // Smart Recovery. Also, we add the heat/cool differential to try and get ahead of the Smart Recovery curve (otherwise we close too early or too often)

        String smarter = 	ST ? theThermostat.currentValue('thermostatOperatingStateDisplay')	: theThermostat.currentValue('thermostatOperatingStateDisplay', true)
        boolean beSmart = 	(smarter?.contains('mart'))	// "(Smart Recovery)"
        boolean beCool = (smarter?.contains('verc'))	// "(Overcooling)"
        String cOpState = 	ST ? theThermostat.currentValue('thermostatOperatingState') 		: theThermostat.currentValue('thermostatOperatingState', true)
        def cTemperature = 	ST ? theThermostat.currentValue('temperature')						: theThermostat.currentValue('temperature', true)
        def coolSP = 		ST ? theThermostat.currentValue('coolingSetpoint') 					: theThermostat.currentValue('coolingSetpoint', true)
        def heatSP = 		ST ? theThermostat.currentValue('heatingSetpoint') 					: theThermostat.currentValue('heatingSetpoint', true)
        String cMode = 		ST ? theThermostat.currentValue('thermostatMode') 					: theThermostat.currentValue('thermostatMode', true)
        def nextHeatSP
        def nextCoolSP
        if (settings.useThermostat) {
        	nextHeatSP = 	ST ? theThermostat.currentValue('nextHeatingSetpoint') : theThermostat.currentValue('nextHeatingSetpoint', true)
        	nextCoolSP = 	ST ? theThermostat.currentValue('nextCoolingSetpoint') : theThermostat.currentValue('nextCoolingSetpoint', true)       
        }
        def currentStatus = [s: smarter, bs: beSmart, bc: beCool, op: cOpState, md: cMode, t: cTemp, h: heatSP, c: coolSP, nh: nextHeatSP, nc: nextCoolSP] as HashMap
        if (cOpState == 'heating') { atomicState.heatOrCool = 'heat' } else if (cOpState == 'cooling') { atomicState.heatOrCool == 'cool' }
        if (atomicState.heatOrCool == null) atomicState.heatOrCool = cMode
        
        if (atomicState.currentStatus == currentStatus) { 
            LOG("Status unchanged...",3,null,'info')
            return 'unchanged'			// ignore - identical to last time
        } else {
            atomicState.currentStatus = currentStatus
            LOG("currentStatus: ${currentStatus}",3,null,'debug')
        }
        def offset 
    	if ((cOpState == 'heating') || (settings.adjustAlways && (atomicState.heatOrCool == 'heat'))) {
        	offset = settings.heatOffset ?: 0.0
    		def heatTarget 
            if (settings.useThermostat) {
            	if (beSmart) {
                	// Smart Recovery - we're heating to the "next" heatingSetpoint
                	heatTarget = nextHeatSP + offset // + atomicState.heatDifferential
                } else {
                	// Normal - just heating to the heatingSetpoint
                	heatTarget = heatSP + offset // + atomicState.heatDifferential
                }
            } else if (useVirtualStat) {
				heatTarget = ((ST ? theVirtualStat.currentValue('heatingSetpoint') : theVirtualStat.currentValue('heatingSetpoint', true)) as BigDecimal) + offset
            } else {
            	heatTarget = settings.heatingSetpoint
            }
			vents = (cTemp <= heatTarget) ? 'open' : 'closed'
        	LOG("${theThermostat.displayName} is heating, target temperature is ${heatTarget}°${beSmart?' (smart recovery)':''}, room temperature is ${cTemp}°",3,null,'info')
    	} else if ((cOpState == 'cooling') || (settings.adjustAlways && (atomicState.heatOrCool == 'cool'))) {
        	offset = settings.coolOffset ?: 0.0
            def coolTarget 
            if (settings.useThermostat) {
            	if (beSmart) {
                	// Smart Recovery - we're cooling to the "next" coolingSetpoint
                	coolTarget = nextCoolSP + offset // - atomicState.coolDifferential
                } else if (beCool) {
                	// Overcooling (to dehumidify) - we're cooling to the current coolingSetpoint - overcoolOffset
                	coolTarget = coolSP + offset - (settings.overCoolToo ? atomicState.dehumidifyOvercoolOffset : 0.0)
                } else {
                	// Normal - just cooling to the coolingSetpoint
                	coolTarget = coolSP + offset //- atomicState.coolDifferential
                }
            } else if (useVirtualStat) {
            	coolTarget = ((ST ? theVirtualStat.currentValue('coolingSetpoint') : theVirtualStat.currentValue('coolingSetpoint', true)) as BigDecimal) + offset
            } else {
            	coolTarget = settings.coolingSetpoint
            }
			vents = (cTemp >= coolTarget) ? 'open' : 'closed'
        	LOG("${theThermostat.displayName} is cooling, target temperature is ${coolTarget}°${beSmart?' (smart recovery)':''}${beCool?' (overcooling)':''}, room temperature is ${cTemp}°",3,null,'info')
		} else if (cOpState == 'idle') {
    		LOG("${theThermostat.displayName} is idle, room temperature is ${cTemp}°, vents-->unchanged",3,null,'info')
            vents = 'unchanged'	// fix it next time the fan goes on
		} else if (/*(vents == 'unchanged') && */(cOpState == 'fan only')) {
        	if (!settings.fanOnlyState) {
                if (!settings.closedFanOnly) {
                    vents = 'unchanged'
                    LOG("${theThermostat.displayName} is running 'Fan Only', room temperature is ${cTemp}°, vents-->unchanged",3,null,'info')
                } else {
                    vents = 'closed'
                    LOG("${theThermostat.displayName} is running 'Fan Only', room temperature is ${cTemp}°, vents-->closed",3,null,'info')
                }
            } else {
            	// New Fan Only selector is in use
                vents = settings.fanOnlyState
                if (!vents) vents = 'unchanged'
                if (vents == 'percentage') vents = (settings.fanOnlyLevel ?: 50) as Integer
                LOG("${theThermostat.displayName} is running 'Fan Only', room temperature is ${cTemp}°, vents-->${vents}${settings?.fanOnlyState == 'percentage'?'%':''}",3,null,'info')
            }
		}    
		if ((vents == 'open') && (settings.adjustAlways || ((cOpState != 'fan only') || settings.closeFanOnly))) { // let 'Fan Only' run even if windows are open, unless overriden in settings (1.8.02)
        	if (settings.theWindows) {
            	boolean openWindows = ST ? settings.theWindows*.currentValue('contact').contains('open') : settings.theWindows*.currentValue('contact', true).contains('open')
                if (openWindows) {
					vents = 'closed'	// but if a window is open, close the vents
        			LOG("${(theWindows.size()>1)?'A':'The'} window/contact is open",3,null,'info')
                }
            }
    	}
		LOG("Vents should be ${vents?:'unchanged'}",3,null,'info')
		//return vents
    }
    return vents
}

def getAverageTemperature() {
	//boolean ST = isST
    
	def tTemp = 0.0G
    Integer i = 0
	settings.theSensors.each {
		def t = ST ? it.currentValue('temperature') : it.currentValue('temperature', true)
		if (t != null) {
        	tTemp += t as BigDecimal
            i++
        }
	}
	if (i > 1) tTemp = tTemp / i // average all the sensors, if more than 1
    if (i > 0) {
		return roundIt(tTemp, 1)
    } else {
    	LOG("No valid temperature readings from ${settings.theSensors}",1,null,'warn')
    	return null
    }
}

void setTheVents(ventState) {
	def newVentState
	if (!ventState) ventState = 'unchanged'
	if (ventState == 'open') {
        allVentsOpen()
        newVentState = 'open'
    } else if (ventState == 'closed') {
        allVentsClosed()
        newVentState = 'closed'
	} else if (ventState.toString().isNumber()) {
    	allVentsLevel(ventState as Integer)
        newVentState = (ventState.toInteger() <= settings.minimumVentLevel.toInteger()) ? 'closed' : 'open'
    } else if (ventState == 'unchanged') {
    	//boolean ST = isST	
    	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    					(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                    	 (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
        def ventSwitch = ST ? theVents[0].currentValue('switch') : theVents[0].currentValue('switch', true) // assumption: all vents are in the same state
        if (ventSwitch == 'on') {
            newVentState = 'open'
        	def hasLevel = theVents[0].hasAttribute('level')
            if (hasLevel) {
        		def currentLevel = (ST ? theVents[0].currentValue('level') : theVents[0].currentValue('level', true))?.toInteger()
        		if (currentLevel <= minimumVentLevel.toInteger()) {
                	// while physically 'open', we are set to the minimum vent level, so we are logically 'closed'
	            	newVentState = 'closed'
                }
           	}
        } else {
        	// assert ventSwitch == 'off'
        	newVentState = 'closed'
        }
        LOG("setTheVents('unchanged'), prior ventState: ${atomicState.ventState}, new ventState: ${newVentState}",3,null,'debug')
    }
	if (newVentState && (atomicState.ventState != newVentState)) {
    	atomicState.ventState = newVentState
        runIn(2, updateTheVents, [overwrite: true])
        // Update VentState on the ES Sensors, if SmartRoom is active
        List ecobeeSensors = atomicState.ecobeeSensors
    	if (ecobeeSensors != []) {
    		ecobeeSensors.each { dni ->
        		def sensor = parent.getChildDevice(dni)
                if (sensor) {
            		def smartRoom = ST ? sensor.currentValue('SmartRoom') : sensor.currentValue('SmartRoom', true)
            		if (smartRoom && smartRoom.contains('active')) {	// 'active' or 'inactive'
                    	// update vents state unless SmartRoom is 'disabled' or 'default'
            			parent.generateChildEvent(dni, [vents: newVentState])
                    }
            	}
        	}
    	}
		updateMyLabel()
    }
}

void updateTheVents() {
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				// (settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + // Don't ping the Keen Vents...
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    theVents.each {
		if (it.hasCommand('refresh')) {
    		it.refresh()
    	} else if (it.hasCommand('poll')) {
    		it.poll()
    	} else if (it.hasCommand('ping')) {
    		it.ping()
        }
    }
}

void allVentsOpen() {
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    //LOG("Opening the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents.each { ventOn(it) }
    if (theVents*.hasCommand('setLevel')) {
    	atomicState.lastLevel = "${settings.maximumVentLevel ?: 98}%"
    } else {
    	atomicState.lastLevel = 'on'
    }

}

void allVentsClosed() {
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    //LOG("Closing the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents.each { ventOff(it) }
    if (theVents*.hasCommand('setLevel')) {
    	atomicState.lastLevel = "${settings.minimumVentLevel ?: 1}%"
    } else {
    	atomicState.lastLevel = 'off'
    }
}

void allVentsLevel(level) {
	def theVents = (settings.theEconetVents ?: []) + (settings.theHCEcoVents ?: []) + 
    				(settings.theKeenVents ?: []) + (settings.theHCKeenVents ?: []) + 
                     (settings.theGenericVents ?: []) + (settings.theGenericSwitches ?: [])
    //LOG("Opening the vent${theVents.size()>1?'s':''}",3,null,'info')
	theVents.each { theVent ->
    	ventLevel(theVent, level)
        if (level == 0) {
        	ventOff(theVent)
        } else if (level >= 99) {
        	ventOn(theVent)
        }
    }
    atomicState.lastLevel = "${level}%"
}

void ventOff( theVent ) {
	//boolean ST = isST	
    
	def hasSetLevel = (theVent.hasCapability('switchLevel') || theVent.hasCommand('setLevel'))
    def minVentLevel = (settings.minimumVentLevel ?: 1) as Integer
    if (minVentLevel == 0) {
    	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
      	if (currentSwitch == 'on') {
			if (hasSetLevel) {
        		theVent.off()
               	if (!settings?.theKeenVents?.contains(theVent) && !settings?.theHCKeenVents?.contains(theVent)) {
                    // Don't refresh the Keen Vents - they are quite responsive
                    if (theVent.hasCommand('refresh')) theVent.refresh()
                }
                def currentLevel = ST ? theVent.currentValue('level') : theVent.currentValue('level', true)
                // Some vents will leave the level set even when switch is off
				if (currentLevel?.toInteger() != 0) theVent.setLevel(0) // Belt & suspenders - make sure the level is reset to 0
            	LOG("Closing ${theVent.displayName}",3,null,'info')
			} else {
				theVent.off()
				LOG("Turning off ${theVent.displayName}",3,null,'info')
			}
            //if (theVent.hasCommand('refresh')) theVent.refresh()
        } else {
        	LOG("${theVent.displayName} is already closed/off",3,null,'info')
        }
    } else {
		if (hasSetLevel) {
        	def currentLevel = ST ? theVent.currentValue('level') : theVent.currentValue('level', true)
			if (currentLevel?.toInteger() != minVentLevel) {
        		theVent.setLevel(minVentLevel)	// make sure none of the vents are less than the specified minimum
            	LOG("Closing ${theVent.displayName} to ${minVentLevel}%",3,null,'info')
               // if (theVent.hasCommand('refresh')) theVent.refresh()
        	} else {
        		LOG("${theVent.displayName} is already closed to ${minVentLevel}%",3,null,'info')
        	}
		} else {
        	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
			if (currentSwitch == 'on') {
				theVent.off()
				LOG("Turning off ${theVent.displayName}",3,null,'info')
                //if (theVent.hasCommand('refresh')) theVent.refresh()
			} else {
				LOG("${theVent.displayName} is already off",3,null,'info')
			}
		}
    }
    // Display the contact as "closed", even if we are partially open (so that HomeKit shows open/closed Blinds)
    if (theVent.hasAttribute('contact') && theVent.hasCommand('closeContact') && (theVent.currentValue('contact') != 'closed')) theVent.closeContact()
}

void ventOn( theVent ) {
	//boolean ST = isST
    boolean changed = false
    
    def hasSetLevel = (theVent.hasCapability('switchLevel') || theVent.hasCommand('setLevel'))
    def maxVentLevel = (settings.maximumVentLevel ?: 98) as Integer
    def minVentLevel = (settings.minimumVentLevel ?: 1) as Integer
	def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
    def currentLevel = (hasSetLevel ? ( ST ? theVent.currentValue('level') : theVent.currentValue('level', true) ) : ((currentSwitch == 'on') ? 100 : 0)) as Integer
    if (maxVentLevel >= 99) {
      	if ((currentSwitch == 'off') || (currentLevel < maxVentLevel)) {
            if (hasSetLevel) {
                if (currentLevel.toInteger() != maxVentLevel) { theVent.setLevel(maxVentLevel) } //some vents don't handle '100'
                if (!settings?.theKeenVents?.contains(theVent) && !settings?.theHCKeenVents?.contains(theVent)) {
                    // Don't refresh the Keen Vents - they are quite responsive
                    if (theVent.hasCommand('refresh')) theVent.refresh()
                }
                currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
                if (currentSwitch != 'on') theVent.on()						// setLevel will turn on() for some devices, but not all
                changed = true
            } else {
                theVent.on()
                changed = true
            }
        	if (changed) {
            	//if (theVent.hasCommand('refresh')) theVent.refresh()
            	LOG("${hasSetLevel?'Opening':'Turning on'} ${theVent.displayName}",3,null,'info')
        	} else {
            	LOG("${theVent.displayName} is already ${hasSetLevel?'open':'on'}",3,null,'info')
        	}
        }
    } else {
    	// New feature: use configured maximum level
        if (hasSetLevel) {
        	if (currentLevel != maxVentLevel) {
        		theVent.setLevel(maxVentLevel)	// make sure none of the vents are less than the specified minimum
            	LOG("Opening ${theVent.displayName} to ${maxVentLevel}%",3,null,'info')
                //if (theVent.hasCommand('refresh')) theVent.refresh()
        	} else {
        		LOG("${theVent.displayName} is already open to ${maxVentLevel}%",3,null,'info')
        	}
		} else {
			if (currentSwitch == 'off') {
				theVent.on()
				LOG("Turning on ${theVent.displayName}",3,null,'info')
			} else {
				LOG("${theVent.displayName} is already on",3,null,'info')
			}
        }
    }
    // Display the contact as "open", even if we are only partially open (so that HomeKit shows open/closed Blinds)
    if (theVent.hasAttribute('contact') && theVent.hasCommand('openContact') && (theVent.currentValue('contact') != 'open')) theVent.openContact()
}

void ventLevel( theVent, level=50 ) {
	//boolean ST = isST
    if (level == 0) {
    	ventOff(theVent)
        return
    }
    boolean changed = false
    def hasSetLevel = (theVent.hasCapability('switchLevel') || theVent.hasCommand('setLevel'))
    def maxVentLevel = (settings.maximumVentLevel ?: 98) as Integer
    def minVentLevel = (settings.minimumVentLevel ?: 1) as Integer
    if (level > maxVentLevel) level = maxVentLevel
    if (level < minVentLevel) level = minVentLevel
    def currentLevel = (hasSetLevel ? ( ST ? theVent.currentValue('level') : theVent.currentValue('level', true) ) : ((currentSwitch == 'on') ? 100 : 0)) as Integer
	if (hasSetLevel) {
    	if (currentLevel != level) {
        	theVent.setLevel(level)
            changed = true
        }
    	if (!settings?.theKeenVents?.contains(theVent) && !settings?.theHCKeenVents?.contains(theVent)) {
    		// Don't refresh the Keen Vents - they are quite responsive
    		if (theVent.hasCommand('refresh')) theVent.refresh()
    	}
    }
    def currentSwitch = ST ? theVent.currentValue('switch') : theVent.currentValue('switch', true)
    if (currentSwitch != 'on') {
    	theVent.on()						// setLevel will turn on() for some devices, but not all
    	changed = true
    }
    if (hasSetLevel) {
    	if (changed) {
            LOG("Opening ${theVent.displayName} to ${level}%",3,null,'info')
        } else {
        	LOG("${theVent.displayName} is already open to ${level}%",3,null,'info')
        }
    } else {
        if (changed) {
            LOG("Turning on ${theVent.displayName}",3,null,'info')
        } else {
            LOG("${theVent.displayName} is already on",3,null,'info')
        }
    }
    // Display the vent's contact as "open", even if we are only partially open (so that HomeKit shows open/closed Blinds)
    if (theVent.hasAttribute('contact') && theVent.hasCommand('openContact') && (theVent.currentValue('contact') != 'open')) theVent.openContact()
}
// Helper Functions
// Ask our parents for help sending the events to our peer sensor devices
void generateSensorEvents( Map dataMap ) {
	LOG("generating ${dataMap} events for ${theSensors}",3,null,'debug')
	theSensors.each { DNI ->
        parent.getChildDevice(DNI)?.generateEvent(dataMap)
    }
}
def getThermostatPrograms() {
    def cl = settings?.theThermostat?.currentValue('climatesList')
    return (cl ? ((cl == '[]') ? ['Away', 'Home', 'Sleep'] : cl.toString()[1..-2].tokenize(', ').sort(false)) : ['Away', 'Home', 'Sleep'])
}

def getThermostatModes() {
    String tm = settings?.theThermostat?.currentValue('supportedThermostatModes')
	log.debug "thermostatModes: ${tm}"
    return (tm ? ((tm == '[]') ? [] : tm[1..-2].tokenize(', ').sort(false)) : [])
    //return theModes.sort(false)
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(String tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id as String, type )
}
// Cancel my reservation
void cancelReservation(String tid, String type='modeOff') {
	// log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id as String, type )
}
// Do I have a reservation?
Boolean haveReservation(String tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id as String, type )
}
// Do any Apps have reservations?
Boolean anyReservations(String tid, String type='modeOff') {
	return parent.anyReservations( tid, type )
}
// How many apps have reservations?
Integer countReservations(String tid, String type='modeOff') {
	return parent.countReservations( tid, type )
}
// Get the list of app IDs that have reservations
List getReservations(String tid, String type='modeOff') {
	return parent.getReservations( tid, type )
}
// Get the list of app Names that have reservations
List getGuestList(String tid, String type='modeOff') {
	return parent.getGuestList( tid, type )
}

void updateMyLabel() {
	//boolean ST = isST
	
	String flag
	if (ST) {
    	def opts = [' (paused', ' (open', ' (closed']
		opts.each {
			if (!flag && app.label.contains(it)) flag = it
		}
	} else {
		flag = '<span '
	}
	
	// Display vent status 
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
    	//log.debug app.label
		myLabel = app.label
		if (!flag || !myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		// strip off any connection status tag
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
    String cLevel = atomicState.lastLevel
    // log.debug "cLevel: ${cLevel}"
    String newLabel
	if (settings.tempDisable) {
    	cLevel = ' (paused ' + cLevel + ')'
		newLabel = myLabel + ( ST ? cLevel : '<span style="color:red">' + cLevel + '</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (atomicState.ventState == 'open') {
    	cLevel = ' (open ' + cLevel + ')'
		newLabel = myLabel + ( ST ? cLevel : '<span style="color:green">' + cLevel + '</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else if (atomicState.ventState == 'closed') { 
    	cLevel = ' (closed ' + cLevel + ')'
		newLabel = myLabel + ( ST ? cLevel : '<span style="color:green">' + cLevel + '</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
	}
    //log.debug "newLabel: " + newLabel + ", myLabel: " + myLabel + ", app.label: " + app.label
}

// Ask our parents for help sending the events to our peer sensor devices
void generateSensorsEvents( Map dataMap ) {
	LOG("generating ${dataMap} events for ${theSensors}",3,null,'info')
	theSensors.each { DNI ->
        parent.getChildDevice(DNI)?.generateEvent(dataMap)
    }
}
def pauseOn() {
	// Pause this Helper
    log.debug "pauseOn()"
	atomicState.wasAlreadyPaused = (settings.tempDisable && !atomicState.globalPause)
	if (!settings.tempDisable) {
		LOG("performing Global Pause",2,null,'info')
		app.updateSetting("tempDisable", true)
        settings.tempDisable = true
		atomicState.globalPause = true
		runIn(2, updated, [overwrite: true])
        updateMyLabel()
	} else {
		LOG("was already paused, ignoring Global Pause",3,null,'info')
	}
}
def pauseOff() {
	// Un-pause this Helper
    log.debug "pauseOff()"
    log.debug "paused? ${settings.tempDisable}"
	if (settings.tempDisable) {
    log.debug "already? ${atomicState.wasAlreadyPaused}"
		def wasAlreadyPaused = atomicState.wasAlreadyPaused
		if (!wasAlreadyPaused) { // && settings.tempDisable) {
			LOG("performing Global Unpause",2,null,'info')
			app.updateSetting("tempDisable", false)
            settings.tempDisable = false
			runIn(2, updated, [overwrite: true])
            updateMyLabel()
		} else {
			LOG("was paused before Global Pause, ignoring Global Unpause",3,null,'info')
		}
	} else {
		LOG("was already unpaused, skipping Global Unpause",3,null,'info')
		atomicState.wasAlreadyPaused = false
	}
	atomicState.globalPause = false
}
def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def roundIt( BigDecimal value, decimals=0 ) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
void LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
    switch (logType) {
    	case 'error':
        	log.error message
            break;
        case 'warn':
        	log.warn message
            break;
        case 'trace':
        	log.trace message
            break;
        case 'info':
        	if (!settings?.infoOff) log.info message
            break;
        case 'debug':
        default:
        	if (!settings?.debugOff) log.debug message
        	break;
    }
}

String getTheBee	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=78 height=78 align=right></img>'}
String getTheBeeLogo()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg width=30 height=30 align=left></img>'}
String getTheSectionBeeLogo()		{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-300x300.png width=25 height=25 align=left></img>'}
String getTheBeeUrl ()				{ return "https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg" }
String getTheBlank	()				{ return '<img src=https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/blank.png width=400 height=35 align=right hspace=0 style="box-shadow: 3px 0px 3px 0px #ffffff;padding:0px;margin:0px"></img>'}
String pageTitle 	(String txt) 	{ return HE ? getFormat('header-ecobee','<h2>'+(txt.contains("\n") ? '<b>'+txt.replace("\n","</b>\n") : txt )+'</h2>') : txt }
String pageTitleOld	(String txt)	{ return HE ? getFormat('header-ecobee','<h2>'+txt+'</h2>') 	: txt }
String sectionTitle	(String txt) 	{ return HE ? getTheSectionBeeLogo() + getFormat('header-nobee','<h3><b>&nbsp;&nbsp;'+txt+'</b></h3>')	: txt }
String smallerTitle	(String txt) 	{ return txt ? (HE ? '<h3><b>'+txt+'</b></h3>' 				: txt) : '' }
String sampleTitle	(String txt) 	{ return HE ? '<b><i>'+txt+'<i></b>'			 				: txt }
String inputTitle	(String txt) 	{ return HE ? '<b>'+txt+'</b>'								: txt }
String getWarningText()				{ return HE ? "<span style='color:red'><b>WARNING: </b></span>"	: "WARNING: " }
String getFormat(type, myText=""){
	switch(type) {
		case "header-ecobee":
			return "<div style='color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${theBee}${myText}</div>"
			break;
		case "header-nobee":
			return "<div style='width:50%;min-width:400px;color:#FFFFFF;background-color:#5BBD76;padding-left:0.5em;padding-right:0.5em;box-shadow: 0px 3px 3px 0px #b3b3b3'>${myText}</div>"
			break;
    	case "line":
			return HE ? "<hr style='background-color:#5BBD76; height: 1px; border: 0;'></hr>" : "-----------------------------------------------"
			break;
		case "title":
			return "<h2 style='color:#5BBD76;font-weight: bold'>${myText}</h2>"
			break;
		case "warning":
			return HE ? "<span style='color:red'><b>WARNING: </b><i></span>${myText}</i>" : "WARNING: ${myText}"
			break;
		case "note":
			return HE ? "<b>NOTE: </b>${myText}" : "NOTE:<br>${myText}"
			break;
		default:
			return myText
			break;
	}
}
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019-2020, Barry A. Burke (storageanarchy@gmail.com)
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
