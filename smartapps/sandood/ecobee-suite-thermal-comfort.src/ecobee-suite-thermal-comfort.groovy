/**
 *  Ecobee Suite Thermal Comfort
 *
 *  Copyright 2018-2020 Barry A. Burke
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
 *	1.7.10 - Fixed isST/isHE Optimization bugs
 *	1.7.11 - Added multi-humidistat support
 *	1.7.12 - Fixed multi-humidistat initialization error
 *	1.7.13 - Clean up app label in sendMessage()
 *	1.7.14 - Fixed Notifications setup
 *	1.7.15 - Added option to disable local display of log.debug() logs, support Notification devices on ST
 *	1.7.16 - Fixed ST Notifications section, removed HE SMS option, fixed event handlers typo, fixed humidity initialization
 *	1.7.17 - Double-verify program before changing; don't send repetitive (fixed) setpoint changes
 *	1.7.18 - Added notification customization and cleaned up notifier choices
 *	1.7.19 - Don't enforce heal/cooling setpoint rules if thermostat not configured for Auto Mode
 *	1.7.20 - Fixed helper labelling
 *	1.7.21 - Fixed labels (again), added infoOff, cleaned up preferences setup
 *	1.7.22 - Added reservation serializer for setpoint changes (program.climates)
 *	1.7.23 - Added minimize UI
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - More busy bees
 *	1.8.03 - Send simultaneous notification Announcements to multiple Echo Speaks devices
 *	1.8.04 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.05 - New SHPL, using Global Fields instead of atomicState
 *	1.8.06 - Fixed appDisplayName in sendMessage
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.8.06" }
String getVersionLabel() 	{ return "Ecobee Suite Thermal Comfort Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Thermal Comfort",
	namespace: 			"sandood",
	author: 			"Barry A. Burke and Richard Peng",
	description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSets Ecobee Temperature Setpoints based on relative humidity using PMV.",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-thermal-comfort.src/ecobee-suite-thermal-comfort.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-thermal-comfort",
	singleInstance:		false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
    page(name: "customNotifications")
}

def mainPage() {
    def unit = getTemperatureScale()
	//boolean ST = isST
	//boolean HE = !ST
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
    
    def coolPmvOptions = [
			(-1.0): 'Very cool (-1.0)',
			(-0.5): 'Cool (-0.5)',
			0.0: 'Slightly cool (0.0)',
            0.5: 'Comfortable (0.5)',
            0.64: 'Eco (0.64)',
            0.8: 'Slightly warm (0.8)',
            1.9: 'Warm (1.9)',
            'custom': 'Custom'
    ]
    def heatPmvOptions = [
			1.0: 'Very warm (1.0)',
			0.5: 'Warm (0.5)',
			0.0: 'Slightly warm (0.0)',
            (-0.5): 'Comfortable (-0.5)',
            (-0.64): 'Eco (-0.64)',
            (-1.0): 'Slightly cool (-1.0)',
            (-2.3): 'Cool (-2.3)',
            'custom': 'Custom'
    ]
    def metOptions = [
            0.7: 'Sleeping (0.7)',
            0.8: 'Reclining (0.8)',
            1.0: 'Seated, quiet (1.0)',
            1.1: 'Typing (1.1)',
            1.2: 'Standing, relaxed (1.2)',
            1.7: 'Walking about (1.7)',
            1.8: 'Cooking (1.8)',
            2.1: 'Lifting/packing (2.1)',
            2.7: 'House cleaning (2.7)',
            'custom': 'Custom'
    ]
    def cloOptions = [
            0.0: 'Naked (0.0)',
            0.6: 'Typical Summer indoor clothing (0.6)',
            1.0: 'Typical Winter indoor clothing (1.0)',
            2.9: 'Summer lightweight duvet [0.64-2.9] (2.9)',
			5.0: 'Typical Spring/Autumn indoor clothing (5.0)',
            6.8: 'Spring/Autumn weight duvet [4.5-6.8] (6.8)',
            8.7: 'Winter weight duvet [7.7-8.7] (8.7)',
            'custom': 'Custom'
    ]
    String defaultName = "Thermal Comfort"
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV").capitalize()), uninstall: true, install: true) {
    	if (maximize) {
    		section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This Helper dynamically adjusts the heating and/or cooling setpoints of Ecobee Suite Thermostats based on the concept of Thermal Comfort (see https://en.wikipedia.org/wiki/Thermal_comfort). "+
                          "Using the Predicted Mean Vote (PMV) algorithm, Thermal Comfort setpoints will changed based entirely on the observed relative humidity and your selected activity and clothing levels. You may want to create different "+
                          "instances for different seasons and/or activities.\n\n"+
                          "The Helper's dynamic configuration calculator enables you to see the resultant setpoint based upon your PMV inputs in real time.")
            }
        }
		section(title: sectionTitle("Naming${!settings.tempDisable?' & Thermostat Selection':''}")) {
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
            	if (app.label.contains(' (paused)')) {
                	if ((atomicState?.appDisplayName != null) && !atomicState?.appDisplayName.contains(' (paused)')) {
						app.updateLabel(atomicState.appDisplayName)
					} else {
                        String myLabel = app.label.substring(0, app.label.indexOf(' (paused)'))
                        atomicState.appDisplayName = myLabel
                        app.updateLabel(myLabel)
                    }
                } else {
                	atomicState.appDisplayName = app.label
                }
            }
        	if(settings.tempDisable) { 
				paragraph getFormat("warning","This Helper is temporarily paused...") 
			} else {
        		input(name: 'theThermostat', type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select an Ecobee Suite Thermostat"), 
                	  required: true, multiple: false, submitOnChange: true)
			}
			paragraph ''
        }
        if (!settings?.tempDisable && settings?.theThermostat) {
        	section(title: sectionTitle("Configuration") + (ST ? "\n" : '') + smallerTitle("Sensors")) {
				if (settings?.humidistat) {
                	input(name: 'humidistat', type: 'capability.relativeHumidityMeasurement', title: "Select a Humidity Sensor", 
                	  	  required: true, multiple: false, submitOnChange: true, width: 4)
                    atomicState.humidity = settings.humidistat.currentHumidity
					paragraph "The current temperature at ${theThermostat.displayName} is ${theThermostat.currentTemperature}°${unit} and the relative humidity is ${atomicState.humidity}%"
				} else {
					input(name: 'humidistats', type: 'capability.relativeHumidityMeasurement', title: "Select Humidity Sensors", 
                		  required: true, multiple: true, submitOnChange: true, width: 4)
					boolean multiHumid = false
					if (settings.humidistats) {
						if (settings.humidistats.size() == 1) {
							atomicState.humidity = settings.humidistats[0].currentHumidity
						} else {
							multiHumid = true
							input(name: 'multiHumidType', type: 'enum', options: ['average', 'highest', 'lowest'], title: 'Multiple Humidity Sensors, use:',
								  required: true, multiple: false, defaultValue: 'average', submitOnChange: true, width: 4)
							atomicState.humidity = getMultiHumidistats()
						}
					}
					if (atomicState.humidity != null) paragraph "The current temperature at ${theThermostat.displayName} is ${theThermostat.currentTemperature}°${unit} and the ${multiHumid?(settings.multiHumidType+' '):''}relative humidity reading is ${atomicState.humidity}%" 
				}
            }
			
			section(title: smallerTitle("Cool Comfort Settings")) {
				if (HE) {
					// First row
					input(name: "coolPmv", title: inputTitle("PMV${((settings?.coolPmv!=null) && coolConfigured()) ? ' ('+calculateCoolSetpoint()+'°'+unit+')':' ()'}"), 
						  type: 'enum', options: coolPmvOptions, required: !settings.heatPmv, submitOnChange: true, width: 4)
					input(name: "coolMet", title: inputTitle("Metabolic Rate"), type: 'enum', options: metOptions, required: (settings.coolPMV), submitOnChange: true, defaultValue: 1.1, width: 4 )
					input(name: "coolClo", title: inputTitle("Clothing Level"), type: 'enum', options: cloOptions, required: (settings.coolPMV), submitOnChange: true, defaultValue: 0.6, width: 4 )

					// Second row
					if (settings.coolPmv=='custom') {
						input(name: "coolPmvCustom", title: inputTitle("Custom PMV")+"<br>(decimal between -5.0 and 5.0)", type: 'decimal', range: "-5..5", required: true, submitOnChange: true, width: 4 )
					} else if ((settings?.coolMet == 'custom') || (settings?.coolClo == 'custom')) paragraph("",width:4)
					if (settings.coolMet=='custom') {
						input(name: "coolMetCustom", title: inputTitle("Custom Metabolic Rate")+"<br>(decimal 0-10.0)", type: 'decimal', range: "0..10", required: true, submitOnChange: true, width: 4 )
					} else if ((settings?.coolPmv == 'custom') || (settings?.coolClo == 'custom')) paragraph("",width:4)
					if (settings.coolClo=='custom') {
						input(name: "coolCloCustom", title: inputTitle("Custom Clothing Level")+"<br>(decimal 0-10.0)", type: 'decimal', range: "0..10", required: true, submitOnChange: true, width: 4 )
					} else if ((settings?.coolPmv == 'custom') || (settings?.coolMet == 'custom')) paragraph("",width:4)
					
					// Third row
					if ((settings?.coolPmv == 'custom') && (settings?.coolPmvCustom != null) && ((settings.coolPmvCustom < -5.0) || ((settings.coolPmvCustom > 5.0)))){
						paragraph("Value must be -5.0 to 5.0",width:4) 
					} else if (HE) paragraph("",width:4)
					if ((settings?.coolMet == 'custom') && (settings?.coolMetCustom != null) && ((settings.coolMetCustom < 0.0) || ((settings.coolMetCustom > 10.0)))){
						paragraph("Value must be 0.0 to 10.0",width:4) 
					} else if (HE) paragraph("",width:4)
					if ((settings?.coolClo == 'custom') && (settings?.coolCloCustom != null) && ((settings.coolCloCustom < 0.0) || ((settings.coolCloCustom > 10.0)))){
						paragraph("Value must be 0.0 to 10.0",width:4) 
					} else if (HE) paragraph("",width:4)
				} else {
					input(name: "coolPmv", title: "Cooling PMV${((settings?.coolPmv!=null) && coolConfigured()) ? ' ('+calculateCoolSetpoint()+'°'+unit+')':' ()'}", 
						  type: 'enum', options: coolPmvOptions, required: !settings.heatPmv, submitOnChange: true)
					if (settings.coolPmv=='custom') {
						input(name: "coolPmvCustom", title: inputTitle("Custom Cooling PMV (decimal -5.0 to +5.0)"), type: 'decimal', range: "-5..5", required: true, submitOnChange: true )
					}
					input(name: "coolMet", title: inputTitle("Cooling Metabolic Rate"), type: 'enum', options: metOptions, required: (settings.coolPMV), submitOnChange: true, defaultValue: 1.1 )
					if (settings.coolMet=='custom') {
						input(name: "coolMetCustom", title: inputTitle("Custom Cooling Metabolic Rate (decimal 0 to 10.0)"), type: 'decimal', range: "0..10", required: true, submitOnChange: true )
					}
					input(name: "coolClo", title: inputTitle("Cooling Clothing Level"), type: 'enum', options: cloOptions, required: (settings.coolPMV), submitOnChange: true, defaultValue: 0.6 )
					if (settings.coolClo=='custom') {
						input(name: "coolCloCustom", title: inputTitle("Custom Cooling Clothing Level (decimal 0 to 10.0)"), type: 'decimal', range: "0..10", required: true, submitOnChange: true )
					}
				}
			}
			
            section(title: smallerTitle("Heat Comfort Settings")) {
				if (HE) {
					// First row
					input(name: "heatPmv", title: inputTitle("PMV${((settings?.heatPmv!=null) && heatConfigured()) ? ' ('+calculateHeatSetpoint()+'°'+unit+')':' ()'}"), 
						  type: 'enum', options: heatPmvOptions, required: !settings.coolPmv, submitOnChange: true, width: 4)
					input(name: "heatMet", title: inputTitle("Metabolic Rate"), type: 'enum', options: metOptions, required: (settings.heatPMV), submitOnChange: true, defaultValue: 1.1, width: 4 )
					input(name: "heatClo", title: inputTitle("Clothing Level"), type: 'enum', options: cloOptions, required: (settings.heatPMV), submitOnChange: true, defaultValue: 0.6, width: 4 )

					// Second row
					if (settings.heatPmv=='custom') {
						input(name: "heatPmvCustom", title: inputTitle("Custom PMV")+"<br>(decimal between -5.0 and 5.0)", type: 'decimal', range: "-5..5", required: true, submitOnChange: true, width: 4 )
					} else if ((settings?.heatMet == 'custom') || (settings?.heatClo == 'custom')) paragraph("",width:4)
					if (settings.heatMet=='custom') {
						input(name: "heatMetCustom", title: inputTitle("Custom Metabolic Rate")+"<br>(decimal 0-10.0)", type: 'decimal', range: "0..10", required: true, submitOnChange: true, width: 4 )
					} else if ((settings?.heatPmv == 'custom') || (settings?.heatClo == 'custom')) paragraph("",width:4)
					if (settings.heatClo=='custom') {
						input(name: "heatCloCustom", title: inputTitle("Custom Clothing Level")+"<br>(decimal 0-10.0)", type: 'decimal', range: "0..10", required: true, submitOnChange: true, width: 4 )
					} else if ((settings?.heatPmv == 'custom') || (settings?.heatMet == 'custom')) paragraph("",width:4)
					
					// Third row
					if ((settings?.heatPmv == 'custom') && (settings?.heatPmvCustom != null) && ((settings.heatPmvCustom < -5.0) || ((settings.heatPmvCustom > 5.0)))){
						paragraph("Value must be -5.0 to 5.0",width:4) 
					} else if (HE) paragraph("",width:4)
					if ((settings?.heatMet == 'custom') && (settings?.heatMetCustom != null) && ((settings.heatMetCustom < 0.0) || ((settings.heatMetCustom > 10.0)))){
						paragraph("Value must be 0.0 to 10.0",width:4) 
					} else if (HE) paragraph("",width:4)
					if ((settings?.heatClo == 'custom') && (settings?.heatCloCustom != null) && ((settings.heatCloCustom < 0.0) || ((settings.heatCloCustom > 10.0)))){
						paragraph("Value must be 0.0 to 10.0",width:4) 
					} else if (HE) paragraph("",width:4)					
				} else {
					input(name: "heatPmv", title: inputTitle("Heating PMV${((settings.heatPmv!=null) && heatConfigured()) ? ' ('+calculateHeatSetpoint()+'°'+unit+')' : '()'}"), 
						  type: 'enum', options: heatPmvOptions, required: !settings.coolPmv, submitOnChange: true)
					if (settings.heatPmv=='custom') {
						input(name: "heatPmvCustom", title: inputTitle("Custom Heating PMV (decimal -5.0 to +5.0)"), type: 'decimal', range: "-5..5", required: true, submitOnChange: true )
					}
					input(name: "heatMet", title: inputTitle("Metabolic Rate"), type: 'enum', options: metOptions, required: (settings.heatPMV), submitOnChange: true, defaultValue: 1.1 )
					if (settings.heatMet=='custom') {
						input(name: "heatMetCustom", title: inputTitle("Custom Heating Metabolic Rate (decimal 0 to 10.0)"), type: 'decimal', range: "0..10", required: true, submitOnChange: true )
					}
					input(name: "heatClo", title: inputTitle("Clothing level"), type: 'enum', options: cloOptions, required: (settings.heatPMV), submitOnChange: true, defaultValue: 1.0 )
					if (settings.heatClo=='custom') {
						input(name: "heatCloCustom", title: inputTitle("Custom Heating Clothing Level (decimal 0 to 10.0)"), type: 'decimal', range: "0..10", required: true, submitOnChange: true )
					}
				}
            }
			
			section(title: sectionTitle("Conditions")+"${ST?': ':''}"+smallerTitle("Modes & Programs")) {
			//section(title: smallerTitle("Modes & Programs")) {
				boolean multiple = false
				if (maximize) paragraph "By default, Thermal Comfort adjusts the setpoint any time that the indoor temperature or humidity changes"
				input(name: "theModes", type: "mode", title: inputTitle("Adjust when ${location.name}'s Location Mode is"), multiple: true, required: false, submitOnChange: true, width: 4)
                input(name: "statModes", type: "enum", title: inputTitle("Adjust when the ${settings?.theThermostat?:'thermostat'}'s Operating Mode is"), 
					  multiple: true, required: false, submitOnChange: true, options: getThermostatModes(), width: 4)
				def programOptions = getThermostatPrograms() + ['Vacation']
            	input(name: "thePrograms", type: "enum", title: inputTitle("Adjust when the ${settings.theThermostat?:'thermostat'}'s Program is"), 
					  multiple: true, required: false, submitOnChange: true, options: programOptions, width: 4)
				boolean any = (settings?.theModes || settings?.statModes || settings?.thePrograms)
				if ((settings.theModes && settings.statModes) || (settings.statModes && settings.thePrograms) || (settings.thePrograms && settings.theModes)) {
					multiple = true
					input(name: 'needAll', type: 'bool', title: inputTitle('Require ALL conditions to be met?'), required: true, defaultValue: false, submitOnChange: true)
				}
				if (any) {
					if (!multiple) {
						if (maximize) paragraph("Thermal Comfort will only adjust the setpoint when the above condition is met")
					} else {
						if (maximize) paragraph("Thermal Comfort will ${settings.needAll?'only ':''}adjust the setpoint when ${settings.needAll?'ALL':'ANY'} of the above conditions are met")	 
					}
				} else {
					if (maximize) paragraph "Thermal Comfort will adjust the setpoint whenever the indoor temperature or humidity changes"
				}
            }
			
			if (ST) {
				section("Notifications") {
					input(name: "notify", type: "bool", title: inputTitle("Notify on setpoint adjustments?"), required: true, defaultValue: false, submitOnChange: true, width: 3)
					if (settings.notify) {
						input(name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, 
							  required: ((settings?.phone == null) && !settings.notifiers && !settings.speak), submitOnChange: true)
						input(name: "notifiers", type: "capability.notification", title: "Select Notification Devices", hideWhenEmpty: true,
							  required: ((settings.phone == null) && !settings.speak && !settings.pushNotify), multiple: true, submitOnChange: true)
                        if (settings?.notifiers) {
                            List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                            if (echo) {
                            	input(name: "echoAnnouncements", type: "bool", title: "Use ${echo.size()>1?'simultaneous ':''}Announcements for the Echo Speaks device${echo.size()>1?'s':''}?", 
                                	  defaultValue: false, submitOnChange: true)
                            }
                        }
						input(name: "phone", type: "text", title: "SMS these numbers (e.g., +15556667777; +441234567890)", 
							  required: (!settings.pushNotify && !settings.notifiers && !settings.speak), submitOnChange: true)
					}
				}
				if (settings.notify) {
					section(hideWhenEmpty: (!"speechDevices" && !"musicDevices"), title: "") {
						input(name: "speak", type: "bool", title: "Speak the messages?", required: true, defaultValue: false, submitOnChange: true)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: "On these speech devices", 
								  multiple: true, hideWhenEmpty: true, submitOnChange: true)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: "On these music devices", 
								  multiple: true, hideWhenEmpty: true, submitOnChange: true)
							if (settings.musicDevices != null) input(name: "volume", type: "number", range: "0..100", title: "At this volume (%)", defaultValue: 50, required: true)
						}
					}
				}
                if (maximize) {
					section() {
						paragraph ( "A notification is always sent to the Hello Home log whenever an action is taken")
					}
                }
			} else {		// HE
				section(sectionTitle("Notifications")) {
					input(name: "notify", type: "bool", title: inputTitle("Notify on setpoint adjustments?"), required: true, defaultValue: false, submitOnChange: true, width: 8)
					if (settings.notify) {
						input(name: "notifiers", type: "capability.notification", multiple: true, title: inputTitle("Select Notification Devices"), submitOnChange: true,
							  required: (!settings.speak || ((settings.musicDevices == null) && (settings.speechDevices == null))))
                        if (settings?.notifiers) {
                            List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                            if (echo) {
                            	input(name: "echoAnnouncements", type: "bool", title: "Use ${echo.size()>1?'simultaneous ':''}Announcements for the Echo Speaks device${echo.size()>1?'s':''}?", 
                                	  defaultValue: false, submitOnChange: true)
                            }
                        }
					}
				}
				if (settings.notify) {
					section(hideWhenEmpty: (!"speechDevices" && !"musicDevices"), title: "") {
						input(name: "speak", type: "bool", title: inputTitle("Speak messages?"), required: !settings?.notifiers, defaultValue: false, submitOnChange: true, width: 6)
						if (settings.speak) {
							input(name: "speechDevices", type: "capability.speechSynthesis", required: (settings.musicDevices == null), title: inputTitle("Select speech devices"), 
								  multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
							input(name: "musicDevices", type: "capability.musicPlayer", required: (settings.speechDevices == null), title: inputTitle("Select music devices"), 
								  multiple: true, submitOnChange: true, hideWhenEmpty: true, width: 4)
							input(name: "volume", type: "number", range: "0..100", title: inputTitle("At this volume (%)"), defaultValue: 50, required: false, width: 4)
						}
					}
				}
                if (maximize) {
					section("A 'HelloHome' notification is always sent to the Location Event log whenever an action is taken"	){}
                }
			}
            if (settings?.notify && (settings?.pushNotify || settings?.phone || settings?.notifiers || (settings?.speak &&(settings?.speechDevices || settings?.musicDevices)))) {
				section() {
					href name: "customNotifications", title: inputTitle("Customize Notifications"), page: "customNotifications", 
						 description: "Customize notification messages", state: isCustomized()
				}
            }
        }
        section(title: sectionTitle("Operations")) {
        	input(name: "minimize", 	title: inputTitle("Minimize settings text"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
           	input(name: "tempDisable", 	title: inputTitle("Pause this Helper"), 		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)                
			input(name: "debugOff",	 	title: inputTitle("Disable debug logging"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
            input(name: "infoOff", 		title: inputTitle("Disable info logging"), 		type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
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
def customNotifications(){
	//boolean ST = isST
	//boolean HE = !ST
	pageLabel = pageLabel.take(pageLabel.indexOf(','))
	dynamicPage(name: "customNotifications", title: pageTitle("${pageLabel}\nCustom Notifications"), uninstall: false, install: false) {
		section(sectionTitle("Customizations")) {}
        section(smallerTitle("Notification Prefix")) {
			input(name: "customPrefix", type: "enum", title: inputTitle("Prefix text:"), defaultValue: "(helper) at (location):", required: false, submitOnChange: true, 
				  options: ['(helper):', '(helper) at (location):', '(location):', 'none', 'custom'], multiple: false, width: 4)
			if (settings?.customPrefix == null) { app.updateSetting('customPrefix', '(helper) at (location):'); settings.customPrefix = '(helper) at (location):'; }
			if (settings.customPrefix == 'custom') {
				input(name: "customPrefixText", type: "text", title: inputTitle("Custom Prefix text"), defaultValue: "", required: true, submitOnChange: true, width: 4)
			}
            if (HE) paragraph("", width:8)
    	}
        section(smallerTitle("Thermostat")) {
			input(name: "customTstat", type: "enum", title: inputTitle("Refer to the HVAC system as"), defaultValue: "(thermostat name)", options:
				  ['the HVAC system', '(thermostat name)', 'custom'], submitOnChange: true, multiple: false, width: 4)
			if (settings?.customTstat == 'custom') {
				input(name: "customTstatText", type: "text", title: inputTitle("Custom HVAC system text"), defaultValue: "", required: true, submitOnChange: true, width: 4)
			} 
			if (settings?.customTstat == null) { app.updateSetting('customTstat', '(thermostat name)'); settings.customTstat = '(thermostat name)'; }
			if (settings?.customTstat == '(thermostat name)') {
            	if (HE) paragraph("",width:8)
				input(name: "tstatCleaners", type: 'enum', title: inputTitle("Strip these words from the Thermostat's display name (${theThermostat.displayName})"), multiple: true, required: false,
					  submitOnChange: true, options: ['EcobeeTherm', 'EcoTherm', 'Thermostat', 'Ecobee'], width: 4)
				input(name: "tstatPrefix", type: 'enum', title: inputTitle("Add this prefix to the Thermostat's display name"), multiple: false, required: false,
					  submitOnChange: true, options: ['the', 'Ecobee', 'Thermostat', 'Ecobee Thermostat', 'the Ecobee', 'the Ecobee Thermostat', 'the Thermostat'], width: 4) 
			}
        }
        section(title: sampleTitle("Sample Notification Message"), hideable: true, hidden: false) {
			String thePrefix = getMsgPrefix()
			String theTstat = getMsgTstat()
            String unit = getTemperatureScale()
            String heatTemp = "${(settings.heatPmv!=null&&heatConfigured())?calculateHeatSetpoint():(unit=='F'?'68':'21')}"
            String coolTemp = "${(settings.coolPmv!=null&&coolConfigured())?calculateCoolSetpoint():(unit=='F'?'75':'23')}"
			paragraph "${getMsgPrefix()}I changed ${theTstat} setpoints for the ${thePrograms[0]} program to Heat: ${heatTemp}°${unit} and Cool: " +
					  "${coolTemp}°${unit} because I wanted you to see the results!"
		}
	}
}
def isCustomized() {
	return (customPrefix || customTstat) ? "complete" : null
}
void installed() {
	LOG("Installed with settings: ${settings}", 4, null, 'trace')
    atomicState.humidity = null
    atomicState.because = " because ${app.label} was initialized"
	initialize()
}
void uninstalled() {
}
void updated() {
	atomicState.version = getVersionLabel()
	LOG("Updated with settings: ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    atomicState.humidity = null
    atomicState.because = " because ${app.label} was reinitialized"
    initialize()
}
def getThermostatPrograms() {
    def cl = settings?.theThermostat?.currentValue('climatesList')
    return (cl ? ((cl == '[]') ? ['Away', 'Home', 'Sleep'] : cl[1..-2].tokenize(', ').sort(false)) : ['Away', 'Home', 'Sleep'])
}
def getThermostatModes() {
    def statModes = ["heat","cool","auto","auxHeatOnly"]
    if (settings.theThermostat) {
        def tm = theThermostat.currentValue('supportedThermostatModes')
        if (tm && (tm != '[]')) statModes = tm[1..-2].tokenize(", ")
    }
    return statModes.sort(false)
}
def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
    getHubPlatform()
	updateMyLabel()
	
	if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
    if (settings.debugOff) log.info "log.debug() logging disabled"

    subscribe(settings.humidistat, 'humidity', humidityChangeHandler)
    subscribe(settings.theThermostat, 'currentProgram', programChangeHandler)
    // if (thePrograms) subscribe(settings.theThermostat, "currentProgram", modeOrProgramHandler)
    if (statModes) subscribe(settings.theThermostat, "thermostatMode", modeChangeHandler)

    //def h = ST ? settings.humidistat.currentValue('humidity') : settings.humidistat.currentValue('humidity', true)
    def h = getMultiHumidistats()
    atomicState.humidity = h
	
    atomicState.lastHeatRequest = -999
    atomicState.lastCoolRequest = 999
    atomicState.fixed = null
    atomicState.lastFixed = null
    atomicState.lastProgram = ""
    atomicState.lastHeatSetpoint = -999
    atomicState.lastCoolSetpoint = 999
    
    runIn(2, atomicHumidityUpdater, [overwrite: true])
	
    if ((h != null) && (h >= 0) && (h <= 100)) {
    	LOG("Initialization complete...current humidity is ${h}%",2,null,'info')
    } else {
    	LOG("Initialization error...invalid humidity: (${h}) - please check settings and retry", 2, null, 'error')
    }
	return
}

boolean configured() {
    return ((atomicState.humidity != null)) // && (atomicState.temperature != null))
}

boolean coolConfigured() {
    return (configured() &&
            (settings.coolPmv != null && ( settings.coolPmv == 'custom' ? settings.coolPmvCustom != null : true)) &&
            (settings.coolMet != null && ( settings.coolMet == 'custom' ? settings.coolMetCustom != null : true)) &&
            (settings.coolClo != null && ( settings.coolClo == 'custom' ? settings.coolCloCustom != null : true)))
}

boolean heatConfigured() {
    return (configured() &&
            (settings.heatPmv != null && ( settings.heatPmv == 'custom' ? settings.heatPmvCustom != null : true)) &&
            (settings.heatMet != null && ( settings.heatMet == 'custom' ? settings.heatMetCustom != null : true)) &&
            (settings.heatClo != null && ( settings.heatClo == 'custom' ? settings.heatCloCustom != null : true)))
}

def programChangeHandler(evt) {
    LOG("Thermostat Program is: ${evt.value}",3,null,'info')
    
    // Don't schedule the update if the new thermostat program isn't one that we're supposed to adjust!
    if (!settings.thePrograms || settings.thePrograms?.contains(evt.value)) {
        atomicState.because = " because the thermostat's program changed to ${evt.value}"
    	runIn(10, atomicHumidityUpdater, [overwrite: true])				// Wait a bit longer for all the setpoints to update after the program change
    }
}

def modeChangeHandler(evt) {
    LOG("Thermostat Mode is: ${evt.value}",3,null,'info')
    
    // Don't schedule the update if the new thermostat mode isn't one that we're supposed to adjust!
    if (!settings.statModes || settings.statModes?.contains(evt.value)) {
		atomicState.because = " because the thermostat's mode changed to ${evt.value}"
    	runIn(5, atomicHumidityUpdater, [overwrite: true])				// Mode changes don't directly impact setpoints, but give it time just in case
    }
}

def humidityChangeHandler(evt) {
	if (evt.numberValue != null) {
        // atomicState.humidity = evt.numberValue
		atomicState.humidity = getMultiHumidistats()
		atomicState.because = " because the ${settings.multiHumidistats?(settings.multiHumidistats + ' '):''}humidity changed to ${atomicState.humidity}%"
        runIn(2, atomicHumidityUpdater, [overwrite: true])			// Humidity changes are independent of thermostat settings, no need to wait long
    }
}

void atomicHumidityUpdater() {
	humidityUpdate( atomicState.humidity )
}

void humidityUpdate( humidity ) {
	if (humidity?.toString().isNumber()) humidityUpdate(humidity as Integer)
}

void humidityUpdate( Integer humidity ) {
	if (atomicState.version != getVersionLabel()) {
		LOG("Code version updated, re-initializing...",1,null,'warn')
		updated()
		return			// just ignore the original call, because updated/initalize will call us again
	}
    if (humidity == null) {
    	LOG("ignoring invalid humidity: ${humidity}%", 2, null, 'warn')
        return
    }
	//boolean ST = isST
    
    atomicState.humidity = humidity
    LOG("Humidity is: ${humidity}%",3,null,'info')
	String statHold = ST ? settings.theThermostat.currentValue('thermostatHold') : settings.theThermostat.currentValue('thermostatHold', true)
	if (statHold == 'vacation') {
		LOG("${settings.theThermostat.displayName} is in Vacation Mode, not adjusting setpoints", 3, null, 'warn')
		return
	}
	
    String currentProgram 	= ST ? settings.theThermostat.currentValue('currentProgram') : settings.theThermostat.currentValue('currentProgram', true)
    String currentMode 		= ST ? settings.theThermostat.currentValue('thermostatMode') : settings.theThermostat.currentValue('thermostatMode', true)

	String andOr = (settings.enable != null) ? settings.enable : 'or'
	if ((andOr == 'and') && (!settings.thePrograms || !settings.statModes)) andOr = 'or'		// if they only provided one of them, ignore 'and'
    
    boolean isOK = (!settings.thePrograms && !settings.statModes) ? true : false // isOK if both weren't specified
	if (!isOK) {
		if (settings.thePrograms  && currentProgram) {
			isOK = settings.thePrograms.contains(currentProgram)
		}
		if (isOK && (andOr == 'and')) {
			if (settings.statModes && currentMode) {
				isOK = settings.statModes.contains(currentMode)
			} else {
				isOK = false
			}
		} else if (!isOK && (andOr == 'or') && settings.statModes && currentMode) {
			isOK = settings.statModes.contains(currentMode)
		}
	}
    if (!isOK) {
		LOG("${settings.theThermostat.displayName}'s current Mode (${currentMode}/${settings.statModes}) ${andOr} Program (${currentProgram}/${settings.thePrograms}) don't match settings, not adjusting setpoints", 3, null, "info")
        return
    }

    def heatSetpoint = roundIt(((ST ? settings.theThermostat.currentValue('heatingSetpoint') : settings.theThermostat.currentValue('heatingSetpoint', true)) as BigDecimal), 2)
    def coolSetpoint = roundIt(((ST ? settings.theThermostat.currentValue('coolingSetpoint') : settings.theThermostat.currentValue('coolingSetpoint', true)) as BigDecimal), 2)
	def curHeat = heatSetpoint
	def curCool = coolSetpoint
    if (settings.heatPmv != null) {
        heatSetpoint = roundIt((calculateHeatSetpoint() as BigDecimal), 2)
    }
    if (settings.coolPmv != null) {
        coolSetpoint = roundIt((calculateCoolSetpoint() as BigDecimal), 2)
    }
    if (atomicState.lastFixed == null) {
        if ((heatSetpoint != curHeat) || (coolSetpoint != curCool)) {
            LOG("Before changeSetpoints(${currentProgram}) - Current setpoints (H/C): ${curHeat}/${curCool}, calculated setpoints: ${heatSetpoint}/${coolSetpoint}", 2, null, 'info')
            changeSetpoints(currentProgram, heatSetpoint, coolSetpoint)
        } else {
            // Could be outside of the allowed range, or just too small of a difference...
            LOG("The calculated Thermal Comfort setpoints (${heatSetpoint}/${coolSetpoint}) are the same as the current setpoints, not adjusting", 3, null, 'info')
        }
    } else {
    	// Last request was adjusted - let's see if it was for the same setpoints we just calculated
        if ((atomicState.lastProgram == program) && (atomicState.lastHeatRequest == heatSetpoint) && (atomicState.lastCoolRequest == coolSetpoint)) {
        	// Was the same request - let's just double check if it was for the same actual setpoints
        	if ((curHeat != atomicState.lastHeatSetpoint) || (curCool != atomicState.lastCoolSetpoint)) {
            	LOG("Before changeSetpoints(${currentProgram}) - Current (fixed) setpoints (H/C): ${curHeat}/${curCool}, calculated (unfixed) setpoints: ${heatSetpoint}/${coolSetpoint}", 2, null, 'info')
            	changeSetpoints(currentProgram, heatSetpoint, coolSetpoint)
            } else {
            	LOG("Skipping redundant changeSetpoints() call",3, null, 'info')
            }
        }
    }
}

void changeSetpoints( program, heatTemp, coolTemp ) {
	def unit = getTemperatureScale()
	//boolean ST = isST
    
	String currentProgram 	= ST ? settings.theThermostat.currentValue('currentProgram') : settings.theThermostat.currentValue('currentProgram', true)
    if (program != currentProgram) {
    	LOG("Request to change program ${program}, bit that is not the current program (${currentProgram}) - ignoring request",2,null,'warn')
        return
    }
    def hasAutoMode = ST ? settings.theThermostat.currentValue('hasAuto') : settings.theThermostat.currentValue('hasAuto', true) 
	def delta = !hasAutoMode ? 0.0 : (ST ? settings.theThermostat.currentValue('heatCoolMinDelta') as BigDecimal : settings.theThermostat.currentValue('heatCoolMinDelta', true) as BigDecimal)
	def fixed = null
    atomicState.lastHeatRequest = heatTemp
    atomicState.lastCoolRequest = coolTemp
	def ht = heatTemp.toBigDecimal()
	def ct = coolTemp.toBigDecimal()
	LOG("${ht} - ${ct}", 3, null, 'debug')
    // Don't adjust setpoints if we don't have Auto mode...just treat as 2 independent setpoints
    // (And let Ecobee figure it out)
    if (hasAutoMode) {
        if ((ct - ht) < delta) {
            fixed = null
            // Uh-oh, the temps are too close together!
            if (settings.heatPmv == null) {
                // We are only adjusting cool, so move heat out of the way
                ht = ct - delta
                fixed = 'heat'
            } else if (settings.coolPmv == null) {
                // We are only adjusting heat, so move cool out of the way
                ct = ht + delta
                fixed = 'cool'
            }
            if (!fixed) {
                if ((settings.statModes != null) && (settings.statModes.size() == 1)) {
                    if (settings.statModes.contains('cool')) {
                        // we are ONLY adjusting PMV in cool mode, move the heat setpoint
                        ht = ct - delta
                        fixed = 'heat'
                    } else if (settings.statModes.contains('heat')) {
                        // we are ONLY adjusting PMV in heat mode, move the cool setpoint
                        ct = ht + delta
                        fixed = 'cool'
                    }
                }
            }
            if (!fixed) {
                // Hmmm...looks like we're adjusting both, and so we don't know which to fix
                String lastMode = ST ? settings.theThermostat.currentValue('lastOpState') : settings.theThermostat.currentValue('lastOpState', true)	// what did the thermostat most recently do?
                if (lastMode) {
                    if (lastMode.contains('cool')) {
                        ht = ct - delta							// move the other one
                        fixed = 'heat'
                    } else if (lastMode.contains('heat')) {
                        ct = ht + delta							
                        fixed = 'cool'
                    }
                }
            }
            if (!fixed) {
                // Nothing left to try - we're screwed!!!!
                LOG("Unable to adjust PMV, calculated setpoints are too close together (less than ${delta}°${unit}", 1, null, 'warn')
                return
            } else {
                coolTemp = ct
                heatTemp = ht
            }
        }
	} else {
    	coolTemp = ct
        heatTemp = ht
    }
    String currentMode 		= ST ? settings.theThermostat.currentValue('thermostatMode') : settings.theThermostat.currentValue('thermostatMode', true)    
    def currentHeatSetpoint = roundIt(((ST ? settings.theThermostat.currentValue('heatingSetpoint') : settings.theThermostat.currentValue('heatingSetpoint', true)) as BigDecimal), 2)
    def currentCoolSetpoint = roundIt(((ST ? settings.theThermostat.currentValue('coolingSetpoint') : settings.theThermostat.currentValue('coolingSetpoint', true)) as BigDecimal), 2)
    
    if ((currentHeatSetpoint != heatTemp) || (currentCoolSetpoint != coolTemp)) {
        LOG("Changing setpoints for (${program}): ${heatTemp} / ${coolTemp} ${fixed?'('+fixed.toString()+'ingSetpoint was delta adjusted)':''}",2,null,debug)
        //theThermostat.setProgramSetpoints( program, heatTemp.toString(), coolTemp.toString())
    	String tid = theThermostat.currentValue("identifier").toString()
        boolean anyReserved = anyReservations( tid, 'programChange' )		// need to make sure nobody changes the program Map out from beneath us
        if (!anyReserved || haveReservation( tid, 'programChange' )) {
        	// Nobody has a reservation, or the reservation is mine
        	if (!anyReserved) makeReservation(tid, 'programChange')
			makeSetpointChange(theThermostat, program, heatTemp, coolTemp)
        } else {
        	// somebody else has a reservation - we have to wait
            atomicState.pendedUpdates = [program: program, heat: heatTemp, cool: coolTemp]
            subscribe(stat, 'climatesUpdated', programWaitHandler)
            //LOG("Delayed: Sensor ${sensor.displayName} will be added to ${settings.theClimates.toString()[1..-2]} and removed from ${notPrograms.toString()[1..-2]} when pending changes complete",2,null,'info')
        }

        atomicState.lastHeatSetpoint = heatTemp
        atomicState.lastCoolSetpoint = coolTemp
        atomicState.lastProgram = program
        atomicState.lastMode = currentMode
        atomicState.lastFixed = fixed
    } else {
       	LOG("The ${fixed?'delta adjusted':'requested'} Thermal Comfort setpoints are the same as the current setpoints, ignoring request...",2,null,'info')
        return
    }
    
	// Only send the notification if we are changing the CURRENT program - program could have changed under us...
	if (currentProgram == program) {
		String because = atomicState.because
        if (heatTemp.toString().endsWith('.00') || heatTemp.toString().endsWith('.0')) heatTemp = heatTemp as Integer
        if (coolTemp.toString().endsWith('.00') || coolTemp.toString().endsWith('.0')) coolTemp = coolTemp as Integer
		def msg = "I changed ${getMsgTstat()} setpoints for the ${program} program to Heat: ${heatTemp}°${unit} ${(fixed=='heat')?'(adjusted) ':''}and Cool: " +
			"${coolTemp}°${unit} ${(fixed=='cool')?'(adjusted) ':''}${because}"
		if (msg != atomicState.lastMsg) sendMessage( msg )	// don't send the same message over and over again (shouldn't be happening anyway)
		atomicState.lastMsg = msg
	}
	atomicState.because = ''
}

def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP)
}
def roundIt( BigDecimal value, decimals=0) {
    return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP)
}

def calculatePmv(temp, units, rh, met, clo) {
    // returns pmv
    // temp, air temperature
    // units, air temperature unit
    // rh, relative humidity (%) Used only this way to input humidity level
    // met, metabolic rate (met)
    // clo, clothing (clo)

    def vel = 0.0

    def ta = ((units == 'C') ? temp : (temp-32)/1.8) as BigDecimal

    def pa, icl, m, fcl, hcf, taa, tcla, p1, p2, p3, p4,
            p5, xn, xf, eps, hcn, hc, tcl, hl1, hl2, hl3, hl4, hl5, hl6,
            ts, pmv, n

    pa = rh * 10 * Math.exp(16.6536 - 4030.183 / (ta + 235))

    icl = 0.155 * clo //thermal insulation of the clothing in M2K/W
    m = met * 58.15 //metabolic rate in W/M2
    if (icl <= 0.078) fcl = 1 + (1.29 * icl)
    else fcl = 1.05 + (0.645 * icl)

    //heat transf. coeff. by forced convection
    hcf = 12.1 * Math.sqrt(vel)
    taa = ta + 273
    tcla = taa + (35.5 - ta) / (3.5 * icl + 0.1)

    p1 = icl * fcl
    p2 = p1 * 3.96
    p3 = p1 * 100
    p4 = p1 * taa
    p5 = 308.7 - 0.028 * m + p2 * Math.pow(taa / 100, 4)
    xn = tcla / 100
    xf = tcla / 50
    eps = 0.00015

    n = 0
    while (Math.abs(xn - xf) > eps) {
        xf = (xf + xn) / 2
        hcn = 2.38 * Math.pow(Math.abs(100.0 * xf - taa), 0.25)
        if (hcf > hcn) hc = hcf
        else hc = hcn
        xn = (p5 + p4 * hc - p2 * Math.pow(xf, 4)) / (100 + p3 * hc)
        ++n
        if (n > 150) {
            return 1
        }
    }

    tcl = 100 * xn - 273

    // heat loss diff. through skin
    hl1 = 3.05 * 0.001 * (5733 - (6.99 * m) - pa)
    // heat loss by sweating
    if (m > 58.15) hl2 = 0.42 * (m - 58.15)
    else hl2 = 0
    // latent respiration heat loss
    hl3 = 1.7 * 0.00001 * m * (5867 - pa)
    // dry respiration heat loss
    hl4 = 0.0014 * m * (34 - ta)
    // heat loss by radiation
    hl5 = 3.96 * fcl * (Math.pow(xn, 4) - Math.pow(taa / 100, 4))
    // heat loss by convection
    hl6 = fcl * hc * (tcl - ta)

    ts = 0.303 * Math.exp(-0.036 * m) + 0.028
    pmv = ts * (m - hl1 - hl2 - hl3 - hl4 - hl5 - hl6)

    return roundIt(pmv, 2)
}

def calculateHeatSetpoint() {
    def targetPmv = settings.heatPmv=='custom' ? settings.heatPmvCustom : settings.heatPmv as BigDecimal
    def met = 		settings.heatMet=='custom' ? settings.heatMetCustom : settings.heatMet as BigDecimal
    def clo = 		settings.heatClo=='custom' ? settings.heatCloCustom : settings.heatClo as BigDecimal

    def units = getTemperatureScale()
	def step = (units == 'C') ? 0.5 : 1.0
    def range = getHeatRange()
    def min = range[0] as BigDecimal
    def max = range[1] as BigDecimal
    def preferred = max
    def goodSP = preferred as BigDecimal
    def pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    def goodPMV = pmv 
    while (preferred >= min && pmv >= targetPmv) {
    	goodSP = preferred as BigDecimal
        preferred = preferred - step
        goodPMV = pmv
        pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    }
    //preferred = preferred + step
	//if (preferred > max) preferred = max
    //pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    LOG("Heating preferred set point: ${goodSP}°${units} (PMV: ${goodPMV})",3,null,'info')
    return goodSP
}

def calculateCoolSetpoint() {
    def targetPmv = settings.coolPmv=='custom' ? settings.coolPmvCustom : settings.coolPmv as BigDecimal
    def met = 		settings.coolMet=='custom' ? settings.coolMetCustom : settings.coolMet as BigDecimal
    def clo = 		settings.coolClo=='custom' ? settings.coolCloCustom : settings.coolClo as BigDecimal

    def units = getTemperatureScale()
	def step = (units == 'C') ? 0.5 : 1.0
    def range = getCoolRange()
    def min = range[0] as BigDecimal
    def max = range[1] as BigDecimal
    def preferred = min
    def goodSP = preferred as BigDecimal
    def pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    def goodPMV = pmv
    while (preferred <= max && pmv <= targetPmv) {
    	goodSP = preferred as BigDecimal
        preferred = preferred + step
        goodPMV = pmv
        pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    }
    // preferred = preferred - step
	// if (preferred < min) preferred = min
    // pmv = calculatePmv(preferred, units, atomicState.humidity, met, clo)
    LOG("Cooling preferred set point: ${goodSP}°${units} (PMV: ${goodPMV})",3,null,'info')
    return goodSP
}

def getHeatRange() {
	//boolean ST = isST
    def low  = ST ? settings.theThermostat.currentValue('heatRangeLow')  : settings.theThermostat.currentValue('heatRangeLow', true)
    def high = ST ? settings.theThermostat.currentValue('heatRangeHigh') : settings.theThermostat.currentValue('heatRangeHigh', true)
    return [roundIt(low-0.5,0),roundIt(high-0.5,0)]
}

def getCoolRange() {
	//boolean ST = isST
    def low  = ST ? settings.theThermostat.currentValue('coolRangeLow')  : settings.theThermostat.currentValue('coolRangeLow', true)
    def high = ST ? settings.theThermostat.currentValue('coolRangeHigh') : settings.theThermostat.currentValue('coolRangeHigh', true)
    return [roundIt(low-0.5,0),roundIt(high-0.5,0)]
}

def getMultiHumidistats() {
	def humidity = atomicState.humidity
	if (!settings.humidistats) {
    	humidity =  ST ? settings.humidistat.currentHumidity : settings.humidistat.currentValue('humidity', true)
    } else if (settings.humidistats.size() == 1) {
        humidity = ST ? settings.humidistats[0].currentHumidity : settings.humidistats[0].currentValue('humidity', true)
	} else {
		def tempList = atomicState.isST ? settings.humidistats*.currentHumidity : settings.humidistats*.currentValue('humidity', true)
		switch(settings.multiHumidType) {
			case 'average':
				humidity = roundIt( (tempList.sum() / tempList.size()), 0)
				break;
			case 'lowest':
				humidity = tempList.min()
				break;
			case 'highest':
				humidity = tempList.max()
				break;
        }
	}
    return humidity
}
/*					
def getMultiThermometers() {
	if (!settings.thermometers) 			return settings.theThermostat.currentTemperature
	if (settings.thermometers.size() == 1) 	return settings.thermostats[0].currentTemperature
	
	def tempList = settings.thermometers.currentTemperature
	def result
	switch(settings.multiTempType) {
		case 'average':
			return roundIt( (tempList.sum() / tempList.size()), (getTemperatureScale()=='C'?2:1))
			break;
		case 'lowest':
			return tempList.min()
			break;
		case 'highest':
			return tempList.max()
			break;
	}
}
*/

// Helper Functions
String textListToString(list) {
	def c = list?.size()
	String s = list.toString()[1..-2]
	if (c == 1) return s.trim()						// statName
	if (c == 2) return s.replace(', ',' and ').trim()	// statName1 and statName2
	int i = s.lastIndexOf(', ')+2
	return (s.take(i) + 'and ' + s.drop(i)).trim()		// statName1, statName2, (...) and statNameN
}
String getMsgPrefix() {
	String thePrefix = ""
	if (settings?.customPrefix == null) { app.updateSetting('customPrefix', '(helper) at (location):'); settings.customPrefix = '(helper) at (location):'; }
	switch (settings?.customPrefix) {
		case '(helper):':
			thePrefix = atomicState.appDisplayName + ': '
			break
		case '(helper) at (location):':
			thePrefix = atomicState.appDisplayName + " at ${location.name}: "
			break
		case '(location):':
			thePrefix = location.name + ': '
			break
		case 'custom':
			thePrefix = settings?.customPrefixText?.trim() + ' '
			break
		case 'none':
			break
	}
	return thePrefix
}

String getMsgTstat() {						
	String theTstat = ""
	if (settings?.customTstat == null) { app.updateSetting('customTstat', '(thermostat name)'); settings?.customTstat = '(thermostat name)'; }
	switch (settings?.customTstat) {
		case 'custom':
			theTstat = settings?.customTstatText 
			break
		case "(thermostat name)":
			String name = settings?.theThermostat.displayName
			if (settings?.tstatPrefix) name = settings.tstatPrefix + ' ' + name
            name = name + (name.endsWith('s') ? "'" : "'s")
			if (name != "") {
				tstatCleaners.each{
					if ((!settings?.tstatPrefix || (settings.tstatPrefix != it))) {	// Don't strip the prefix/suffix we added above
						name = name.replace(it, '').replace(it.toLowerCase(), '')	// Strip out any unnecessary words
					}
				}
			}
			theTstat = name.replace(':','').replace('  ', ' ').trim()		// Finally, get rid of any double spaces
			break
		case 'the HVAC system':
			theTstat = "the H V A C system's"
			break
	}
	return theTstat
}

void sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "info")
    if (settings.notify) {
    	String msgPrefix = getMsgPrefix()
        String msg = msgPrefix + notificationMessage
        boolean addFrom = (msgPrefix && !msgPrefix.startsWith("From "))
        //String msg = "${atomicState.appDisplayName} at ${location.name}: " + notificationMessage		// for those that have multiple locations, tell them where we are
		if (ST) {
			if (settings.notifiers) {
            	if (settings.echoAnnouncements) {
                    List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                    List notEcho = echo ? settings.notifiers - echo : settings.notifiers

                    // If we have multiple Echo Speak device targets, get them all to speak at once
                    List echoDeviceObjs = []
                    if (echo) {
                        if(echo?.size() > 1) {
                            echo?.each { 
                                String deviceType = it.currentValue('deviceType') as String
                                String serialNumber = it.deviceNetworkId.toString().split(/\|/).last() as String
                                echoDeviceObjs?.push([deviceTypeId: deviceType, deviceSerialNumber: serialNumber]) 
                            }
                        }
                        //Announcement Command Logic
                        if((echo.size() > 1) && echoDeviceObjs && echoDeviceObjs?.size()) {
                            //NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
                            def devJson = new groovy.json.JsonOutput().toJson(echoDeviceObjs)
                            echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:atomicState.appDisplayName), echoDeviceObjs)	// , changeVol, restoreVol) }
                        } else if (echo.size() == 1) {
                            echo.playAnnouncement(msg, (msgPrefix?:atomicState.appDisplayName))
                        } else {
                        	notEcho*.deviceNotification(msg)
                        }
                    } else {
                        settings.notifiers*.deviceNotification(msg)
                    }
                } else {
                	settings.notifiers*.deviceNotification(msg)
                }                
            }
			if (settings.phone) { // check that the user did select a phone number
				if ( settings.phone.indexOf(";") > 0){
					def phones = settings.phone.split(";")
					for ( def i = 0; i < phones.size(); i++) {
						LOG("Sending SMS ${i+1} to ${phones[i]}", 3, null, 'info')
						sendSmsMessage(phones[i].trim(), msg)				// Only to SMS contact
					}
				} else {
					LOG("Sending SMS to ${settings.phone}", 3, null, 'info')
					sendSmsMessage(settings.phone.trim(), msg)						// Only to SMS contact
				}
			} 
			if (settings.pushNotify) {
				LOG("Sending Push to everyone", 3, null, 'warn')
				sendPushMessage(msg)								// Push to everyone
			}
			if (settings.speak) {
				if (settings.speechDevices != null) {
					settings.speechDevices.each {
						it.speak( (addFrom?"From ":"") + msg )
					}
				}
				if (settings.musicDevices != null) {
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText( (addFrom?"From ":"") + msg )
					}
				}
			}
		} else {		// HE
			if (settings.notifiers) {
            	if (settings.echoAnnouncements) {
                    List echo = settings.notifiers.findAll { (it.deviceNetworkId.contains('|echoSpeaks|') && it.hasCommand('sendAnnouncementToDevices')) }
                    List notEcho = echo ? settings.notifiers - echo : settings.notifiers

                    // If we have multiple Echo Speak device targets, get them all to speak at once
                    List echoDeviceObjs = []
                    if (echo) {
                        if(echo?.size() > 1) {
                            echo?.each { 
                                String deviceType = it.currentValue('deviceType') as String
                                String serialNumber = it.deviceNetworkId.toString().split(/\|/).last() as String
                                echoDeviceObjs?.push([deviceTypeId: deviceType, deviceSerialNumber: serialNumber]) 
                            }
                        }
                        //Announcement Command Logic
                        if((echo.size() > 1) && echoDeviceObjs && echoDeviceObjs?.size()) {
                            //NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
                            def devJson = new groovy.json.JsonOutput().toJson(echoDeviceObjs)
                            echo[0].sendAnnouncementToDevices(msg, (msgPrefix?:atomicState.appDisplayName), echoDeviceObjs)	// , changeVol, restoreVol) }
                        } else if (echo.size() == 1) {
                            echo.playAnnouncement(msg, (msgPrefix?:atomicState.appDisplayName))
                        } else {
                        	notEcho*.deviceNotification(msg)
                        }
                    } else {
                        settings.notifiers*.deviceNotification(msg)
                    }
                } else {
                	settings.notifiers*.deviceNotification(msg)
                }                
            }
			if (settings.speak) {
				if (settings.speechDevices != null) {
					settings.speechDevices.each {
						it.speak((addFrom?"From ":"") + msg )
					}
				}
				if (settings.musicDevices != null) {
					settings.musicDevices.each {
						it.setLevel( settings.volume )
						it.playText((addFrom?"From ":"") + msg )
					}
				}
			}
			
		}
    }
	// Always send to Hello Home / Location Event log
	if (ST) { 
		sendNotificationEvent( notificationMessage )					
	} else {
		sendLocationEvent(name: "HelloHome", descriptionText: notificationMessage, value: app.label, type: 'APP_NOTIFICATION')
	}
}

def programUpdateHandler(evt) {
	// Clear our reservation once we know that the Ecobee Cloud has updated our thermostat's setpoints (climates)
    cancelReservation(evt.device.currentValue('identifier') as String, 'programChange')
    unschedule(clearReservation)
    unsubscribe(evt.device)
    if (!settings?.tempDisable) {
    	subscribe(evt.device, 'temperature', insideChangeHandler)
        subscribe(evt.device, 'thermostatMode', thermostatModeHandler)
    }
	LOG("programUpdateHandler(): Setpoint change operation completed",4,null,'debug')
}
def programWaitHandler(evt) {
	//LOG("climatesWaitHandler(): $evt.name = $evt.value",4,null,'debug')
    unsubscribe(evt.device)
    if (!settings?.tempDisable) {
    	subscribe(evt.device, 'temperature', insideChangeHandler)
        subscribe(evt.device, 'thermostatMode', thermostatModeHandler)
    }
    String tid = evt.device.currentValue('identifier') as String
    def count = countReservations(tid, 'programChange')
    if ((count > 0) && !haveReservation( tid, 'programChange' )) {
    	atomicState.programWaitCounter = 0
    	runIn(5, checkReservations, [overwrite: true, data: [tid:tid, type:'programChange']])
        LOG("programWaitHandler(): There are still ${count} reservations for 'programChange', waiting...", 3, null, 'debug')
    } else {
    	makeReservation(tid, 'programChange')
        LOG("programWaitHandler(): 'programChange' reservation secured, sending pended updates", 3, null, 'debug')
    	doPendedUpdates(tid)
    }
}
void checkReservations(data) {
    def count = countReservations(data.tid, data.type)
    def counter = atomicState.programWaitCounter
	if ((count > 0) && !haveReservation(data.tid, data.type)  && (counter <= 60)) {	// Try for five minutes...
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
void doRefresh() {
	settings?.theThermostat?.doRefresh(true) // do a forced refresh
}
void clearReservation() {
    String tid = settings?.theThermostat?.currentValue("identifier").toString()
    cancelReservation(tid, 'programChange')
}
void doPendedUpdates(tid) {
	LOG("doPendedUpdates() @ ${now()}: ${atomicState.pendedUpdates}",4,null,'debug')
    def updates = atomicState.pendedUpdates
    
    if (needSetpointChange( stat, updates.program, updates.heat, updates.cool )) {
		makeSetpointChange( stat, updates.program, updates.heat, updates.cool )
    } else {
        // Nothing to do - release the reservation now
        cancelReservation(tid, 'programChange')
    }
    atomicState.pendedUpdates = [:] 	
}
def makeSetpointChange( stat, program, heat, cool ) {
    subscribe( stat, 'climatesUpdated', programUpdateHandler )
    stat.setProgramSetpoints( program, heat, cool )
    runIn(150, clearReservation, [overwrite: true])
    // programUpdateHandler will release the reservation for us
}
boolean needSetpointChange(stat, program, heat, cool) {             
    // def tid = stat.currentValue('identifier') as String
    // need to figure out how to tell if we actually need to make changes
    def cProgram = ST ? stat.currentValue('currentProgram') : stat.currentProgram('currentProgram', true)
    return (cProgram == program) // just make sure we are changing the current program
}
// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(String tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id as String, type )
}
// Cancel my reservation
void cancelReservation(String tid, String type='modeOff') {
	//log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id as String, type )
}
// Do I have a reservation?
boolean haveReservation(String tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id as String, type )
}
// Do any Apps have reservations?
boolean anyReservations(String tid, String type='modeOff') {
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
    
	String flag = ST ? ' (paused)' : '<span '
	
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (myLabel.contains(flag)) {
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
	if (settings.tempDisable) {
		def newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
		if (app.label != myLabel) app.updateLabel(myLabel)
	}
}
def pauseOn() {
	// Pause this Helper
	atomicState.wasAlreadyPaused = (settings.tempDisable && !atomicState.globalPause)
	if (!settings.tempDisable) {
		LOG("performing Global Pause",2,null,'info')
		app.updateSetting("tempDisable", true)
		atomicState.globalPause = true
		runIn(2, updated, [overwrite: true])
	} else {
		LOG("was already paused, ignoring Global Pause",3,null,'info')
	}
}
def pauseOff() {
	// Un-pause this Helper
	if (settings.tempDisable) {
		def wasAlreadyPaused = atomicState.wasAlreadyPaused
		if (!wasAlreadyPaused) { // && settings.tempDisable) {
			LOG("performing Global Unpause",2,null,'info')
			app.updateSetting("tempDisable", false)
			runIn(2, updated, [overwrite: true])
		} else {
			LOG("was paused before Global Pause, ignoring Global Unpause",3,null,'info')
		}
	} else {
		LOG("was already unpaused, skipping Global Unpause",3,null,'info')
		atomicState.wasAlreadyPaused = false
	}
	atomicState.globalPause = false
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
