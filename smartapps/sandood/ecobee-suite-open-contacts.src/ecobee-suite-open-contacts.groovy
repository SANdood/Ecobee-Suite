/**
 *  ecobee Suite Open Contacts
 *
 *  Copyright 2016 Sean Kendall Schneyer
 *	Copyright 2017-2020 Barry A. Burke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *
 *  ecobee Suite Open Contacts
 *      http://www.apache.org/licenses/LICENSE-2.0  
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * <snip>
 *	1.7.25 - Don't notify if contacts are closed while "off_pending" delay
 *	1.7.26 - Fixed 'off_pending' (again)
 *	1.7.27 - Changed minimum LOG level to 3
 *	1.7.28 - Fixed unintended overwrite of thermostat's mode in statModeChange()
 *	1.7.29 - Clean up appLabel in sendMessage()
 *	1.7.30 - Corrected (user reported) tmpThermSaveState typo
 *	1.7.31 - Added noDebug option
 *	1.7.32 - Tweaked noDebug, add Notifications device support for ST
 *	1.7.33 - Cleaned up Notification messages for single/multi thermostats
 *	1.7.34 - Fixed Notifications section for ST, removed SMS for HE
 *	1.7.35 - Fixed initialization error when HVAC should be off
 *	1.7.36 - Added options to customize the Notifications
 *	1.7.37 - Bypass HE cache for currentContact/currentSwitch
 *	1.7.38 - Tweaks to notification customization
 *	1.7.39 - Fixed typo that prevented announcements being sent or spoken in some cases
 *	1.7.40 - Fixed another issue preventing announcements
 *	1.7.41 - Fixed Helper labelling
 *	1.7.42 - Fixed labels (again), added debugOff/infoOff, cleaned up preferences setup, added Intro ***
 *	1.7.43 - Fixed numOpen() fatal error
 *	1.7.44 - Added minimize UI
 *	1.7.45 - Made logging less chatty with debugOff && infoOff
 *	1.7.46 - Layout tweaks for Hubitat
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - Miscellaneous optimizations
 *	1.8.03 - Updated Paused warning
 *	1.8.04 - Fixed some logging messages, cleaned up thermostatMode changes
 *	1.8.05 - More busy bees...
 *	1.8.06 - Ignore multiple "open" events if no intermediate "close" events
 *	1.8.07 - Cleaned up open/close notification messages, HVAC state now in app.label
 *	1.8.08 - New SHPL, using Global Fields instead of atomicState
 *	1.8.09 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.10 - Send simultaneous notification Announcements to multiple Echo Speaks devices
 *	1.8.11 - Fixed appDisplayName in sendMessage
 */
import groovy.transform.Field

String getVersionNum()		{ return "1.8.11" }
String getVersionLabel() 	{ return "Ecobee Suite Contacts & Switches Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Open Contacts",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nTurn HVAC on/off based on status of contact sensors or switches (e.g. doors, windows, or fans)",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-open-contacts.src/ecobee-suite-open-contacts.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-opencontact-sa",
	singleInstance: 	false,
    pausable: 			true
)

preferences {
	page(name: "mainPage")
	page(name: "customNotifications")
}

// Preferences Pages
def mainPage() {
	//boolean ST = isST
	//boolean HE = !ST
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
    String defaultName = "Contacts & Switches"
    
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This Helper automatically turns the HVAC system Off when doors/windows are left open for too long, and/or when (real or virtual) switch(es) are turned on or off. "+
                          "The HVAC system is restored to its prior operation once all the doors/windows are closed and switch(es) are reversed, with a configurable delay "+
                          "before turning the HVAC Off and back On.\n\n"+
                          "If desired, custom notifications can be made via Notification Devices, Echo Speaks and/or other Speech & Music devices to alert (or remind) occupants of the open doors/windows." )
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
            	def opts = [' (paused', ' (HVAC']
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
        	if (settings.tempDisable) { 
				paragraph getFormat("warning","This Helper is temporarily paused...")
			} else { 
				if (settings.theThermostats || !settings.myThermostats) {
					input(name: "theThermostats", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select Ecobee Suite Thermostat(s)"), 
                    		required: true, multiple: true, submitOnChange: true)
				} else {
            		input(name: "myThermostats", type: "${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select Ecobee Suite Thermostat(s)"), 
                    		required: true, multiple: true, submitOnChange: true)
				}
                if (HE) paragraph ''
            }          
		}
    
		if (!settings.tempDisable && ((settings.theThermostats?.size() > 0) || (settings.myThermostats?.size() > 0))) {
			section(title: sectionTitle("Triggers")+(ST?"\n":'')+smallerTitle("Contact Sensors")) {
				input(name: "contactSensors", title: inputTitle("Select Contact Sensors: "), type: "capability.contactSensor", required: false, multiple: true,  submitOnChange: true, width: 6)
                if (settings.contactSensors) {
                	input(name: 'contactOpen', type: 'bool', title: inputTitle("Trigger Actions when ${settings.contactSensors.size()>1?'any of the contacts are':'the contact is'} open?"), 
                    	  required: true, defaultValue: true, submitOnChange: true, width: 8)
                   	if (maximize) paragraph("Actions will run when a contact sensor is ${((settings.contactOpen==null)||settings.contactOpen)?'Open':'Closed'}.")
                }
			}
            section(title: smallerTitle("Switches")) {
            	input(name: "theSwitches", title: inputTitle("Select Switches: "), type: "capability.switch", required: false, multiple: true,  submitOnChange: true, width: 6)
                if (settings.theSwitches) {
                	input(name: 'switchOn', type: 'bool', title: inputTitle("Trigger Actions when ${settings.theSwitches.size()>1?'any of the switches are':'the switch is'} turned on?"), 
                    	  required: true, defaultValue: true, submitOnChange: true, width: 8)
                    if (maximize) paragraph("Actions will run when a switch is turned ${((settings.switchOn==null)||settings.switchOn)?'On':'Off'}")
                }
        	}
			section(title: sectionTitle("Actions")) {
								
                input(name: "whichAction", title: inputTitle("Select which Actions to run")+" (Default=Notify Only)", type: "enum", required: true, 
                      options: ["Notify Only", "HVAC Actions Only", "Notify and HVAC Actions"], defaultValue: "Notify Only", submitOnChange: true, width: 6)
                if (settings?.whichAction == null) { app.updateSetting('whichAction', 'Notify Only'); settings?.whichAction = 'Notify Only'; }
				if (HE) paragraph("",width: 6)
				
				if (!settings.hvacOff && !settings.adjustSetpoints) {
            		if (maximize) paragraph('If you are using  the Quiet Time Helper, you can centralize off/idle Actions by turning on Quiet Time from this Helper instead of running HVAC Actions directly. '+
							  'The Quiet Time Helper also offers additional control options (e.g.; fan/circulation off, dehumidifier off, etc.).')
                	input(name: 'quietTime', type: 'bool', title: inputTitle('Enable Quiet Time?'), required: true, defaultValue: false, submitOnChange: true, width: 4)
                	if (settings.quietTime) {
                		input(name: 'qtSwitch', type: 'capability.switch', required: true, title: inputTitle('Which switch controls Quiet Time?'), multiple: false, submitOnChange: true)
                    	if (settings.qtSwitch) {
                        	input(name: "qtOn", type: "enum", title: inputTitle("Enable Quiet Time when switch ${settings.qtSwitch.displayName} is:"), required: true, multiple: false, 
                        	  	  options: ["on","off"], submitOnChange: true, width: 3)
							if (HE) paragraph("", width: 9)
                        	if (settings.qtOn != null) paragraph("Switch ${settings.qtSwitch.displayName} will be turned ${settings.qtOn?'On':'Off'} when HVAC Off Actions are taken.")
                    	}
					}
                } 
				if (!settings.quietTime && !settings.adjustSetpoints) {
                	input(name: 'hvacOff', type: "bool", title: inputTitle("Turn off HVAC?"), required: true, defaultValue: false, submitOnChange: true, width: 4)
					//if (HE) paragraph("", width: 6)
                	if ((settings?.hvacOff != null) && settings.hvacOff) {
                    	if (maximize) paragraph("HVAC Mode will be set to Off. Circulation, Humidification and/or Dehumidification may still operate while HVAC is Off. " +
								  "Use the Quiet Time Helper for additional control options.\n\n"+
                                  'Note that no Actions will be run if the HVAC Mode was already Off when the first contact sensor or switch would have turned it ' +
                		  		  'off; the HVAC will remain Off when all the contacts & switches are reset.')
                    }
				}
				if (!settings.quietTime && !settings.hvacOff) {
                    input(name: 'adjustSetpoints', type: 'bool', title: inputTitle('Adjust heat/cool setpoints?'), required: true, defaultValue: false, submitOnChange: true, width: 4)
                    if (adjustSetpoints) {
						//paragraph ("", width: 6)
                        input(name: 'heatAdjust', type: 'decimal', title: inputTitle('Heating setpoint adjustment')+' (+/-20°) ', required: true, defaultValue: 0.0, range: '-20..20', width: 4)
                        input(name: 'coolAdjust', type: 'decimal', title: inputTitle('Cooling setpoint adjustment')+' (+/-20°) ', required: true, defaultValue: 0.0, range: '-20..20', width: 4)
                    }
           		}
                if ((settings?.contactSensors != null) || (settings?.theSwitches != null) || settings?.hvacOff) {
					input(name: "offDelay", title: inputTitle("Select the Delay Time before turning ${settings?.quietTime?'on Quiet Time':'off the HVAC'} or Sending Notifications")+" (minutes)", type: "enum", required: true, 
                    	options: ['0', '1', '2', '3', '4', '5', '10', '15', '30'], defaultValue: '5', width: 6)
					input(name: "onDelay", title: inputTitle("Select the Delay Time before turning ${settings?.quietTime?'off Quiet Time':'the HVAC back on'} or Sending Notifications")+" (minutes)", type: "enum", required: true, 
                    	options: ['0', '1', '2', '3', '4', '5', '10', '15', '30'], defaultValue: '0', width: 6)
	        	}
            }
            if (ST) {
                section("Notifications") {
                    if (settings?.whichAction != "HVAC Actions Only") {
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
                if (settings?.whichAction != "HVAC Actions Only") {
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
                    	paragraph ( "A notification is always sent to the Hello Home log whenever an action is triggered")
                	}
                }
            } else {		// HE
                section(sectionTitle("Notifications")) {
                    if (settings?.whichAction != "HVAC Actions Only") {
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
                if (settings?.whichAction != "HVAC Actions Only") {
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
                if (maximize)  {
                	section("A 'HelloHome' notification is always sent to the Location Event log whenever an action is triggered") {}
                }
            }
            if (settings?.whichAction?.startsWith('Notify') && (settings?.pushNotify || settings?.phone || settings?.notifiers || (settings?.speak &&(settings?.speechDevices || settings?.musicDevices)))) {
                section() {
                    href name: "customNotifications", title: inputTitle("Customize Notifications"), page: "customNotifications", 
                         description: "Customize notification messages", state: isCustomized()
                }
            }        
		} // End if (theThermostats?.size() > 0)
        
        section(title: sectionTitle("Operations")) {
        	input(name: "minimize", 	title: inputTitle("Minimize settings text"), 	type: "bool", required: false, defaultValue: false, submitOnChange: true, width: 3)
           	input(name: "tempDisable", 	title: inputTitle("Pause this Helper"), 		type: "bool", required: false, defaultValue: false,	submitOnChange: true, width: 3)                
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
def customNotifications() {
	//boolean ST = isST
	//boolean HE = !ST
	String pageLabel = getVersionLabel()
	pageLabel = pageLabel.take(pageLabel.indexOf(','))
	dynamicPage(name: "customNotifications", title: pageTitle("${pageLabel}\nCustom Notifications"), uninstall: false, install: false) {
        section(title: sectionTitle("Customizations") + (ST?"\n":'') + (smallerTitle("Notification Prefix"))) {
            input(name: "customPrefix", type: "enum", title: inputTitle("Prefix text:"), defaultValue: "(helper) at (location):", required: false, submitOnChange: true, 
                  options: ['(helper):', '(helper) at (location):', '(location):', 'none', 'custom'], multiple: false, width: 4)
            if (settings?.customPrefix == null) { app.updateSetting('customPrefix', '(helper) at (location):'); settings.customPrefix = '(helper) at (location):'; }
            if (settings.customPrefix == 'custom') {
                input(name: "customPrefixText", type: "text", title: inputTitle("Custom Prefix text"), defaultValue: "", required: true, submitOnChange: true, width: 4)
            } 
        }
        section(smallerTitle("Contact Sensors")) {
			if (settings?.contactSensors) {
				input(name: "customContact", type: "enum", title: inputTitle("Generally refer to Contact Sensors as"), defaultValue: 'contact sensors', submitOnChange: true,
					  options: ['contact sensors', 'doors', 'windows', 'doors and/or windows', 'custom'], multiple: false, width: 4)
				if (settings?.customContact == null) { app.updateSetting('customContact', 'contact sensors'); settings.customContact = 'contact sensors'; }
				if (settings?.customContact == 'custom') {
					input(name: "customContactText", type: "text", title: inputTitle("Custom Contact Sensors text"), defaultValue: "", required: true, submitOnChange: true, width: 4)
				}
			}
        }
        section(smallerTitle("Switches")) {
			if (settings?.theSwitches) {
				input(name: "customSwitch", type: "enum", title: inputTitle("Generally refer to Switches as"), defaultValue: 'switches', submitOnChange: true,
					  options: ['switches', 'toggles', 'custom'], multiple: false, width: 4)
				if (settings?.customSwitch == null) { app.updateSetting('customSwitch', 'switches'); settings.customSwitch = 'switches'; }
				if (settings.customSwitch == 'custom') {
					input(name: "customSwitchText", type: "text", title: inputTitle("Custom Switches text"), defaultValue: "", required: true, submitOnChange: true, width: 4)
				}
			}
        }
        section(smallerTitle("Thermostats")) {
			input(name: "customTstat", type: "enum", title: inputTitle("Refer to the HVAC system as"), defaultValue: "(thermostat names) is/are", options:
				  ['the HVAC system', '(thermostat names) is/are', 'custom'], submitOnChange: true, multiple: false, width: 4)
			if (settings?.customTstat == 'custom') {
				input(name: "customTstatText", type: "text", title: inputTitle("Custom HVAC system text"), defaultValue: "", required: true, submitOnChange: true, width: 4)
			} 
			if (settings?.customTstat == null) { app.updateSetting('customTstat', '(thermostat names) is/are'); settings.customTstat = '(thermostat names) is/are'; }
			if (settings?.customTstat == '(thermostat names) is/are') {
				input(name: "tstatCleaners", type: 'enum', title: inputTitle("Strip these words from the Thermostat display names"), multiple: true, required: false,
					  submitOnChange: true, options: ['EcobeeTherm', 'EcoTherm', 'Thermostat', 'Ecobee'], width: 4)
				input(name: "tstatPrefix", type: 'enum', title: inputTitle("Add this prefix to the Thermostat display names"), multiple: false, required: false,
					  submitOnChange: true, options: ['the', 'Ecobee', 'Thermostat', 'Ecobee Thermostat', 'the Ecobee', 'the Ecobee Thermostat', 'theThermostat'], width: 4) 
				input(name: "tstatSuffix", type: 'enum', title: inputTitle("Add this suffix to the Thermostat display names"), multiple: false, required: false,
					  submitOnChange: true, options: ['Ecobee', 'HVAC', 'HVAC system', 'Thermostat', 'heating', 'cooling', 'A/C'], width: 4)
			}
        }
        section(smallerTitle("Sensors")) {
			input(name: "useSensorNames", type: "bool", title: inputTitle("Use device display names when possible?"), defaultValue: true, submitOnChange: true, width: 4)
			if (settings?.useSensorNames) {
            	input(name: "addThePrefix", type: "bool", title: inputTitle("Add 'the' as prefix to device display names?"), defaultValue: false, submitOnChange: true, width: 4)
				if (settings?.contactSensors) input(name: "contactCleaners", type: "enum", title: inputTitle("Strip these words from Contact display names"), multiple: true, required: false, 
													submitOnChange: true, options: ['Contact', 'Monitor', 'Multi-Sensor', 'Multisensor', 'Multi', 'Sensor', 'Door', 'Window'], width: 4)
				if (settings?.theSwitches) input(name: "switchCleaners", type: "enum", title: inputTitle("Strip these words from Switch display names"), multiple: true, required: false, 
												 submitOnChange: true, options: ['Switch', 'Toggle', 'Power', 'Meter'], width: 4)
			}
			if ((settings?.onDelay != '0') || (settings?.offDelay != '0')) {
				input(name: "includeDelays", type: "bool", title: inputTitle("Include open/close delay times in notifications?"), defaultValue: true, submitOnChange: true, width: 4)
			}
        }
        section(title: sampleTitle("Sample Notification Messages"), hideable: true, hidden: false) {
            String thePrefix = getMsgPrefix()
            String theContact = getMsgContact()
            String theSwitch = getMsgSwitch()
            String theTstat = getMsgTstat()
            String samples = ""

            if (settings?.contactSensors) {
                if (!useSensorNames) {
                    samples = thePrefix + "one or more " + theContact + " have been ${((settings.contactOpen==null)||settings?.contactOpen)?'open':'closed'} " +
                        (((settings?.offDelay != '0') && settings?.includeDelays) ? "for ${offDelay} minute${settings?.offDelay?.toInteger()>1?'s':''}" : 'too long') + ", ${theTstat}now off\n"
                } else {
                    def nameList = []
                    if (settings?.addThePrefix) {
                        settings?.contactSensors.each {
                            nameList << ('the ' + it.displayName)
                        }
                    } else {
                        nameList = settings?.contactSensors*.displayName
                    }
                    String sensorStr = textListToString(nameList)
                    if (settings?.contactCleaners != []) {
                        settings?.contactCleaners.each{
                            sensorStr = sensorStr.replace(it, '').replace(it.toLowerCase(), '').trim()	// Strip out any unnecessary words
                        }
                    }
                    sensorStr = sensorStr.replace(':','').replace('  ', ' ').replace(' ,', ',').trim()           
                    samples = thePrefix + sensorStr + " ${settings?.contactSensors.size()>1?'have':'has'} been ${((settings.contactOpen==null)||settings?.contactOpen)?'open':'closed'} " +
                                (((settings?.offDelay != '0') && settings?.includeDelays) ? "for ${offDelay} minute${settings?.offDelay?.toInteger()>1?'s':''}" : 'too long') + 
                                ", ${theTstat} now off\n"
                }
                if ((settings?.onDelay == '0') || !settings?.includeDelays) {
                    samples = samples.capitalize() + "${thePrefix}all ${theContact} are ${((settings.contactOpen==null)||settings?.contactOpen)?'closed':'open'}, ${theTstat} now on\n".capitalize()
                } else {
                    samples = samples.capitalize() + "${thePrefix}all ${theContact} have been ${((settings.contactOpen==null)||settings?.contactOpen)?'closed':'open'} for ".capitalize() +
                        "${settings?.onDelay} minute${settings?.onDelay?.toInteger()>1?'s':''}, ${theTstat} now on\n"
                }
            }
            if (settings?.theSwitches) {
                String switchSample = ""
                if (!useSensorNames) {
                    switchSample = thePrefix + "one or more " + theSwitch + " have been ${((settings.switchOn==null)||settings?.switchOn)?'on':'off'} " +
                        (((settings?.offDelay != '0') && settings?.includeDelays) ? "for ${offDelay} minute${settings?.offDelay?.toInteger()>1?'s':''}" : 'too long') + ", ${theTstat} now off\n"
                } else {
                    def nameList = []
                    if (settings?.addThePrefix) {
                        settings?.theSwitches.each {
                            nameList << ('the ' + it.displayName)
                        }
                    } else {
                        nameList = settings?.theSwitches*.displayName
                    }
                    String switchStr = textListToString(nameList)
                    if (settings?.switchCleaners != []) {
                        settings?.switchCleaners.each {
                            switchStr = switchStr.replace(it, '').replace(it.toLowerCase(), '').trim()	// Strip out any unnecessary words
                        }
                    }
                    switchStr = switchStr.replace(':', '').replace('  ', ' ').replace(' ,', ',').trim()
                    switchSample = thePrefix + switchStr + " ${settings?.theSwitches.size()>1?'have':'has'} been ${((settings.switchOn==null)||settings?.switchOn)?'on':'off'} " +
                                (((settings?.offDelay != '0') && settings?.includeDelays) ? "for ${offDelay} minute${settings?.offDelay?.toInteger()>1?'s':''}" : 'too long') + 
                                ", ${theTstat} now off\n"
                }
                if ((settings?.onDelay == '0') || !settings?.includeDelays) {
                    samples = samples + switchSample.capitalize() + "${thePrefix}all ${theSwitch} are ${((settings.contactOpen==null)||settings?.switchOn)?'off':'on'}, ${theTstat} now on\n".capitalize()
                } else {
                    samples = samples + switchSample.capitalize() + "${thePrefix}all ${theSwitch} have been ${((settings.contactOpen==null)||settings?.switchOn)?'off':'on'} for ".capitalize() +
                        "${settings?.onDelay} minute${settings?.onDelay?.toInteger()>1?'s':''}, ${theTstat} now on\n"
                }
            }
            paragraph samples
        }
    }
}
def isCustomized() {
	return (customPrefix || customContact || customSwitch || customTstat || (includeDelays != null) || (useSensorNames != null)) ? "complete" : null
}

// Main functions
void installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
	initialize()  
}
void uninstalled () {
   clearReservations()
}
void updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
	unschedule()
	initialize()
    // tester()
}

//
// TODO - if stat goes offline, then comes back online, then re-initialize states...
//
def initialize() {
	LOG("${getVersionLabel()} - Initializing...", 2, "", 'info')
	updateMyLabel()
    //boolean ST = isSTHub
    //boolean HE = !ST
    log.trace "Initializing with settings: ${settings}"
	
	if(settings.tempDisable == true) {
    	clearReservations()
		LOG("Temporarily Paused", 4, null, 'info')
		return true
	}
	if (settings.debugOff) log.trace "log.debug() logging is disabled"
    if (settings.infoOff)  log.trace "log.info()  logging is disabled"
    // subscribe(app, appTouch)
	
	boolean contactOffState = false
	if (contactSensors) {
    	def openSensors = 0
        def closedSensors = 0
        contactSensors.each {
        	def currentContact = ST ? it?.currentValue('contact') : it?.currentValue('contact', true)	// bypass the cache on Hubitat
            if (currentContact == 'open') { openSensors++; } else { closedSensors++; }
        }
    	if (contactOpen) {
        	subscribe(contactSensors, "contact.open", sensorOpened)
            subscribe(contactSensors, "contact.closed", sensorClosed)
        	contactOffState = (openSensors != 0) 		// true if any of the sensors are currently open
       	} else {
        	subscribe(contactSensors, "contact.closed", sensorOpened)
			subscribe(contactSensors, "contact.open", sensorClosed)
			contactOffState = (closedSensors != 0)
        }
    }
    LOG("contactOffState = ${contactOffState}",4,null,'info')
    
    boolean switchOffState = false
    if (theSwitches) {
    	def onSwitches = 0
        def offSwitches = 0
        theSwitches.each {
        def currentSwitch = ST ? it.currentValue('switch') : it.currentValue('switch', true)
        	if (currentSwitch == 'on') { onSwitches++ } else { offSwitches++ }
        }
    	if (switchOn) {
        	subscribe(theSwitches, "switch.on", sensorOpened)
            subscribe(theSwitches, "switch.off", sensorClosed)
            switchOffState = (onSwitches != 0)
        } else {
        	subscribe(theSwitches, "switch.off", sensorOpened)
            subscribe(theSwitches, "switch.on", sensorClosed)
			switchOffState = (offSwitches != 0)
        }
    }
    LOG("switchOffState = ${switchOffState}",4,null,'info')
    
    //def tempState = atomicState.HVACModeState
    //if (tempState == null) tempState = (contactOffState || switchOffState)?'off':'on'		// recalculate if we should be off or on
    def tempState = (contactOffState || switchOffState) ? 'off' : 'on'		// recalculate if we should be off or on
	def theStats = settings.theThermostats ? settings.theThermostats : settings.myThermostats
	def tmpThermSavedState = atomicState.thermSavedState ?: [:]
    if (tempState == 'on') {
		if (atomicState.HVACModeState != 'on') turnOnHVAC(true)
    	// Initialize the saved state values
    	if (!settings.quietTime) {  		
    		theStats.each() { therm ->
    			def tid = getDeviceId(therm.deviceNetworkId)
				if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
				if (ST) {
					tmpThermSavedState[tid] = [	mode: therm.currentValue('thermostatMode'), HVACModeState: 'on', ]
					if (settings.adjustSetpoints) {
						tmpThermSavedState[tid] += [
														heatSP: therm.currentValue('heatingSetpoint'), 
														coolSP: therm.currentValue('coolingSetpoint'),
														heatAdj: 999.0,
														coolAdj: 999.0,
														holdType: therm.currentValue('lastHoldType'),
														thermostatHold: therm.currentValue('thermostatHold'),
														currentProgramName: therm.currentValue('currentProgramName'),	// Hold: Home
														currentProgramId: therm.currentValue('currentProgramId'),		// home
														currentProgram: therm.currentValue('currentProgram'),			// Home
												  ]
					}
				} else {
					// We have to ensure we get the latest values on HE - it caches stuff when it probably shouldn't
					tmpThermSavedState[tid] = [	mode: therm.currentValue('thermostatMode', true), HVACModeState: 'on' ]
					if (settings.adjustSetpoints) {
						tmpThermSavedState[tid] += [
														heatSP: therm.currentValue('heatingSetpoint', true), 
														coolSP: therm.currentValue('coolingSetpoint', true),
														heatAdj: 999.0,
														coolAdj: 999.0,
														holdType: therm.currentValue('lastHoldType', true),
														thermostatHold: therm.currentValue('thermostatHold', true),
														currentProgramName: therm.currentValue('currentProgramName', true),	// Hold: Home
														currentProgramId: therm.currentValue('currentProgramId', true),		// home
														currentProgram: therm.currentValue('currentProgram', true),			// Home
												  ]
					}
				}
    		}
    	}
        
    } else {
    	LOG("Initialized while should be 'Off' - can't update states",2,null,'warn')
        theStats.each() { therm ->
    		def tid = getDeviceId(therm.deviceNetworkId)
            if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
            tmpThermSavedState[tid].mode = (ST ? therm.currentValue('thermostatMode') : therm.currentValue('thermostatMode', true))
            tmpThermSavedState[tid].HVACModeState = 'off'
        }
        if (atomicState.HVACModeState != 'off') turnOffHVAC(true)
    }
    
	if (!settings.quietTime) {
    	//if ((settings.hvacOff == null) || settings.hvacOff) 
		subscribe(theStats, 'thermostatMode', statModeChange)
        //else 
		if (settings.adjustSetpoints) {
    		subscribe(theStats, 'heatingSetpoint', heatSPHandler)
            subscribe(theStats, 'coolingSetpoint', coolSPHandler)
        }
    }
    atomicState.thermSavedState = tmpThermSavedState
	LOG("initialize() - thermSavedState: ${atomicState.thermSavedState}",4,null,'debug')
	LOG("Initialization complete! ",4,null,'trace')
    updateMyLabel()
}

def statModeChange(evt) {
	// only gets called if we are turning off the HVAC (not for quietTime or setpointAdjust operations)
    def tid = getDeviceId(evt.device.deviceNetworkId)
    def tmpThermSavedState = atomicState.thermSavedState ?: [:]
    if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
    
	if (evt.value == 'off') {
		if (atomicState.HVACModeState != 'off') {	// If this is our generated mode change to 'off', then the HVACModeState should already be 'off'
			atomicState.HVACModeState = 'off'
			tmpThermSavedState[tid].mode = 'off'	// update the saved mode
		}
        tmpThermSavedState[tid].HVACModeState = 'off'
		tmpThermSavedState[tid].wasAlreadyOff = false
    } else {
    	// somebody has overridden us...or they just changed the stat mode between heat/cool/auto
        if (haveReservation( tid, 'modeOff')) cancelReservation( tid, 'modeOff' )		// might as well give up our reservation
    	if (atomicState.HVACModeState != 'on') atomicState.HVACModeState = 'on'
        tmpThermSavedState[tid].HVACModeState = 'on'
		tmpThermSavedState[tid].wasAlreadyOff = false
		tmpThermSavedState[tid].mode = evt.value	// update the saved mode
    }
    atomicState.thermSavedState = tmpThermSavedState
	LOG("statModeChange() - thermSavedState: ${atomicState.thermSavedState}",4,null,'debug')
    updateMyLabel()
}
    
def heatSPHandler( evt ) {
	// called when the heatingSetpoint value changes, but only if we are monitoring/making setpoint changes
	// (ie., this won't get called if we are using Quiet Time or just HVAC Off)
    if (evt.numberValue != null) {
		def tid = getDeviceId(evt.device.deviceNetworkId)
    
    	// save the new value
		def tmpThermSavedState = atomicState.thermSavedState ?: [:]
        if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
    	if ((tmpThermSavedState[tid].containsKey(heatAdj)) && (tmpThermSavedState[tid].heatAdj == evt.numberValue)) return 	// we generated this event (below)
    	tmpThermSavedState[tid].heatSP = evt.numberValue
    
    	// if (!atomicState.HVACModeState.contains('off')) {			// Only adjust setpoints when the HVAC is not off
        if (tmpThermSavedState[tid].HVACModeState != 'off') {			// Only adjust setpoints when the HVAC is not off
        	def h = evt.numberValue + settings.heatAdjust
        	tmpThermSavedState[tid].heatAdj = h
        	evt.device.setHeatingSetpoint( h, 'nextTransition')
        	// Notify???
    	} else {
    		tmpThermSavedState[tid].heatAdj = 999.0
    	}
    	atomicState.thermSavedState = tmpThermSavedState
    }
	LOG("heatSPHandler() - thermSavedState: ${atomicState.thermSavedState}",4,null,'debug')
}

def coolSPHandler( evt ) {
	if (evt.numberValue != null) {
        def tid = getDeviceId(evt.device.deviceNetworkId)

        // save the new value
        def tmpThermSavedState = atomicState.thermSavedState ?: [:]
        if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
        if ((tmpThermSavedState[tid].containsKey('coolAdj')) && (tmpThermSavedState[tid].coolAdj == evt.numberValue)) return
        tmpThermSavedState[tid].coolSP = evt.numberValue

        //if (!atomicState.HVACModeState.contains('off')) {
        if (tmpThermSavedState[tid].HVACModeState != 'off') {
            // adjust and change the actual heating setpoints
            def c = evt.numberValue + settings.coolAdjust
            tmpThermSavedState[tid].coolAdj = c
            evt.device.setCoolingSetpoint( c, 'nextTransition')
            // Notify?
        } else {
            tmpThermSavedState = 999.0
        }
        atomicState.thermSavedState = tmpThermSavedState
    }
	LOG("coolSPHandler() - thermSavedState: ${atomicState.thermSavedState}",4,null,'debug')
}

// "sensorOpened" called when state change should turn HVAC off - routine name preserved for backwards compatibility with prior implementations
void sensorOpened(evt=null) {
	LOG("sensorOpened() - ${evt?.device} ${evt?.name} ${evt?.value}", 4, null, 'info')
    def openCount = numOpen()
    def aSOC = atomicState.openCount
    if (aSOC && (aSOC == openCount)) {
    	// Weird - something just opened, but the count of open things is the same as before...
        // We must have missed something getting closed somehow. Let's just ignore this for now
        LOG("sensorOpened() - unexpected duplicate open event, or we missed a close event...ignoring", 3, null, 'warn')
        return
    }
    atomicState.openCount = openCount
    
    def HVACModeState = atomicState.HVACModeState
    if (HVACModeState == 'off_pending') {
    	LOG("sensorOpened() - HVAC off is already pending - ignoring", 3, null, 'info')
    	return		// already in process of turning off
    }

	//boolean ST = isSTHub
	def tmpThermSavedState = atomicState.thermSavedState ?: [:]
	
	def theStats = settings.theThermostats ?: settings.myThermostats
    if (HVACModeState == 'off') { // || (HVACModeState == 'off_pending')) {
    	// HVAC is already off
		def tstatModes = ST ? theStats*.currentValue('thermostatMode') : theStats*.currentValue('thermostatMode', true)
		if (tstatModes.contains('off')) { // at least 1 thermostat is actually off	
			if (atomicState.openCount == 1) {
            	// Save the current HVAC state on the FIRST contact to open ONLY
				atomicState.wasAlreadyOff = true
				theStats.each { therm ->
					def tid = getDeviceId(therm.deviceNetworkId)
					if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
					def statMode = ST ? therm.currentValue('thermostatMode') : therm.currentValue('thermostatMode', true)
					tmpThermSavedState[tid].wasAlreadyOff = (statMode == 'off') 
				}
			}
			atomicState.thermSavedState = tmpThermSavedState
			LOG("sensorOpened(already off) - thermSavedState: ${atomicState.thermSavedState}",4,null,'debug')
            updateMyLabel()
        	return
		} else {
			// No stats are actually off, clean up the mess
			if (HVACModeState != 'on_pending') HVACModeState = 'on'
			atomicState.HVACModeState = HVACModeState
			atomicState.wasAlreadyOff = false
			theStats.each { therm ->
				def tid = getDeviceId(therm.deviceNetworkId)
				if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
				tmpThermSavedState[tid].wasAlreadyOff = false
			}
			atomicState.thermSavedState = tmpThermSavedState
		}
		LOG("sensorOpened() - thermSavedState: ${atomicState.thermSavedState}",4,null,'debug')
    }
    Integer delay = (settings.onDelay ?: 0) as Integer
    if (HVACModeState == 'on_pending') {
		// HVAC is already/still off
        // if (delay > 0) 
		unschedule(turnOnHVAC)
		atomicState.HVACModeState = 'off'
		turnOffHVAC(true)			// Make sure they are really off
        updateMyLabel()
        return
    }

	// HVAC is on, turn it off
   	atomicState.HVACModeState = 'off_pending'
	delay = (settings.offDelay ?: 5) as Integer
	if (delay > 0) { 
		runIn(delay*60, 'turnOffHVAC', [overwrite: true])
		LOG("${theStats.toString()[1..-2]} will be turned off in ${delay} minutes", 4, null, 'info')
	} else { turnOffHVAC(false) }  
    updateMyLabel()
}

//void openedScheduledActions() {		// preserved only for backwards compatibility
//	LOG("openedScheduledActions entered", 5)
//    turnOffHVAC(false)
//}

// "sensorClosed" called when state change should turn HVAC On (routine name preserved for backwards compatibility with prior implementations)
void sensorClosed(evt=null) {
	// Turn HVAC Off action has occurred
    LOG("sensorClosed() - ${evt?.device} ${evt?.name} ${evt?.value}", 4, null,'info')
    atomicState.openCount = numOpen()
    
	def HVACModeState = atomicState.HVACModeState
    //boolean ST = isSTHub
    def theStats = settings.theThermostats ?: settings.myThermostats    
    if (allClosed() == true) {
    	if (HVACModeState == 'on_pending') return
        
		if (atomicState.wasAlreadyOff == true) {
			def tmpThermSavedState = atomicState.thermSavedState ?: [:]
			int i = 0, j = 0
			theStats.each { therm ->
				j++
				def tid = getDeviceId(therm.deviceNetworkId)
				if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
				def statMode = ST ? therm.currentValue('thermostatMode') : therm.currentValue('thermostatMode', true)
				if (tmpThermSavedState[tid].containsKey('wasAlreadyOff') && (tmpThermSavedState[tid].wasAlreadyOff == true)) i++
			}
			if (i == j) {
				atomicState.wasAlreadyOff = false
				LOG("All sensors & switches are reset, but HVAC was already off when the first ${settings.contactSensors?'contact':''} " +
					"${(settings.contactSensors && settings.theSwitches)?'or ':''}${settings.theSwitches?'switch':''} was " +
					"${(settings.contactSensors && settings.contactOpen)?'opened':''}${(settings.contactSensors && !settings.contactOpen)?'closed':''}" +
					"${(settings.contactSensors && settings.theSwitches)?'/':''}" +
					"${(settings.theSwitches && settings.switchOn)?'turned on':''}${(settings.theSwitches && !settings.switchOn)?'turned off':''}, no action taken.",
					3, null, 'info')
				return
			}
		}
		if (HVACModeState == 'on') {
			LOG("All sensors & switches are reset, and HVAC is already on", 3, null, 'info')
			turnOnHVAC(true)	// Just in case
            updateMyLabel()
			return	// already on, nothing more to do (catches 'on' and 'on_pending')
		}
        Integer delay = (settings.offDelay?: 5) as Integer
		if (HVACModeState == 'off_pending' ) {
			unschedule(turnOffHVAC)
			atomicState.HVACModeState = 'on'
			LOG("All sensors & switches are reset, off_pending was cancelled", 3, null, 'info')
            // still on
			turnOnHVAC(true)	// Just in case
            updateMyLabel()
            return
        }
	    
        LOG("All Contact Sensors & Switches are reset, initiating actions.", 3,null,'info')		
        
        atomicState.HVACModeState = 'on_pending'
		// unschedule(openedScheduledActions)
		unschedule(turnOffHVAC)
	    delay = (settings.onDelay?: 0) as Integer
    	//LOG("The on delay is ${delay}",5,null,'info')
		if (delay > 0) { 
			runIn(delay*60, 'turnOnHVAC', [overwrite: true])
            updateMyLabel()
			LOG("${theStats.toString()[1..-2]} will be turned on in ${delay} minutes", 3, null, 'info')
		} else { turnOnHVAC(false) }
	} else {
    	LOG("No action to perform yet...", 5,null,'info')
    }
    updateMyLabel()
}

// void closedScheduledActions() {
//	LOG("closedScheduledActions entered", 5)
//	turnOnHVAC(false)
//}

void turnOffHVAC(quietly = false) {
	// Save current states
    LOG("turnoffHVAC(quietly=${quietly}) entered...", 4,null,'info')
	//boolean ST = isSTHub
	
    if (allClosed() && (atomicState.HVACModeState == 'off_pending')) {
    	// Nothing is open, maybe got closed while we were waiting, abort the "off"
        LOG("turnOffHVAC() called, but everything is closed/off, ignoring request & leaving HVAC on",3,null,'info')
        atomicState.HVACModeState = 'on'
        updateMyLabel()
        return
    }
    
    atomicState.HVACModeState = 'off'
    def action = settings.whichAction ?:'Notify Only'
    def tmpThermSavedState = atomicState.thermSavedState// ?: [:]
    if (!tmpThermSavedState) tmpThermSavedState = [:]
    List tstatNames = []
    boolean doHVAC = action.contains('HVAC')
    def theStats = settings.theThermostats ?: settings.myThermostats
	// log.debug "theStats: ${theStats}"
	theStats.each() { therm ->
    //log.debug "working on ${therm.displayName}  @ ${now()}"
    	def tid = getDeviceId(therm.deviceNetworkId)
        if (!tmpThermSavedState?.containsKey(tid) || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
        if( doHVAC ) {
        	if (settings.quietTime) {
            	// Turn on quiet time
                qtSwitch."${settings.qtOn}"()
                LOG("${therm.displayName} Quiet Time enabled (${qtSwitch.displayName} turned ${settings.qtOn})",3,null,'info')
            } else if ((settings.hvacOff == null) || settings.hvacOff) {
            	// turn off the HVAC
                //log.debug "making reservation ${now()}"
                makeReservation(tid, 'modeOff')						// make sure nobody else turns HVAC on until I'm ready
                //log.debug "got reservation ${now()}"
				def thermostatMode = ST ? therm.currentValue('thermostatMode') : therm.currentValue('thermostatMode', true)
    			if (thermostatMode != 'off') {
                	tmpThermSavedState[tid].mode = thermostatMode	// therm.currentValue('thermostatMode')
                    tmpThermSavedState[tid].HVACModeState = 'off'
					tmpThermSavedState[tid].wasAlreadyOff = false
                    //log.debug "turning stat off @ ${now()}"
            		therm.setThermostatMode('off')
                	if (!tstatNames?.contains(therm.displayName)) tstatNames << therm.displayName		// only report the ones that aren't off already
                    //log.debug "tstatNames ${tstatNames} @ ${now()}"
                	LOG("${therm.displayName} turned off (was ${tmpThermSavedState[tid].mode})",3,null,'info')    
            	}
            } else if (settings.adjustSetpoints) {
            	// Adjust the setpoints
                def h = ST ? therm.currentValue('heatingSetpoint') : therm.currentValue('heatingSetpoint', true)
                def c = ST ? therm.currentValue('coolingSetpoint') : therm.currentValue('coolingSetpoint', true)
                // save the current values for when we turn back on
                tmpThermSavedState[tid].heatSP = h
                tmpThermSavedState[tid].coolSP = c
                h = h + settings.heatAdjust
                c = c + settings.coolAdjust
                tmpThermSavedState[tid].heatAdj = h
                tmpThermSavedState[tid].coolAdj = c
                
				if (ST) {
					tmpThermSavedState[tid].holdType = therm.currentValue('lastHoldType')
					tmpThermSavedState[tid].thermostatHold = therm.currentValue('thermostatHold')
					tmpThermSavedState[tid].currentProgramName = therm.currentValue('currentProgramName')	// Hold: Home, Vacation
					tmpThermSavedState[tid].currentProgramId = therm.currentValue('currentProgramId')		// home
					tmpThermSavedState[tid].currentProgram = therm.currentValue('currentProgram')			// Home
					tmpThermSavedState[tid].scheduledProgram = therm.currentValue('scheduledProgram')
				} else {
					tmpThermSavedState[tid].holdType = therm.currentValue('lastHoldType', true)
					tmpThermSavedState[tid].thermostatHold = therm.currentValue('thermostatHold', true)
					tmpThermSavedState[tid].currentProgramName = therm.currentValue('currentProgramName', true)	// Hold: Home, Vacation
					tmpThermSavedState[tid].currentProgramId = therm.currentValue('currentProgramId', true)		// home
					tmpThermSavedState[tid].currentProgram = therm.currentValue('currentProgram', true)			// Home
					tmpThermSavedState[tid].scheduledProgram = therm.currentValue('scheduledProgram', true)
				}
                therm.setHeatingSetpoint(h, 'nextTransition')
                therm.setCoolingSetpoint(c, 'nextTransition')
                LOG("${therm.displayName} heatingSetpoint adjusted to ${h}, coolingSetpoint to ${c}",3,null,'info')
            }
        } else {
        	if (tmpThermSavedState[tid].mode != 'off') {
                tstatNames << therm.displayName		// only report the ones that aren't off
        		LOG("Saved ${therm.displayName}'s current mode (${tmpThermSavedState[tid].mode})",3,null,'info')
            }
        }
    }
    atomicState.thermSavedState = tmpThermSavedState
	LOG("turnOffHVAC() - thermSavedState: ${atomicState.thermSavedState}", 4, null, 'debug')
    updateMyLabel()
    
	if (tstatNames.size() > 0) {
    	if (action.contains('Notify')  && !quietly) {
        //log.debug "setting up notifications @ ${now()}"
    		boolean notified = false
			def tstatModes = ST ? theStats*.currentValue('thermostatMode') : theStats*.currentValue(thermostatMode, true)
			boolean isOn = tstatModes.contains('auto') || tstatModes.contains('heat') || tstatModes.contains('cool')
        	Integer delay = (settings.offDelay != null ? settings.offDelay : 5) as Integer
			String theStatsStr = getMsgTstat()
			String justTheStats = theStatsStr.endsWith(' is') ? theStatsStr[0..-3] : (theStatsStr.endsWith(' are') ? theStatsStr[0..-4] : theStatsStr)

			String thePrefix = getMsgPrefix()
            String theContact = getMsgContact()
            String theSwitch = getMsgSwitch()
            String theTstat = getMsgTstat()
			String message = ""
			if (settings.contactSensors) {
            //log.debug "doing contact sensors @ ${now()}"
            	def sensorList = []
            	settings.contactSensors.each { 
                	def currentContact = ST ? it.currentValue('contact') : it.currentValue('contact', true)
            		if (currentContact == (settings.contactOpen?'open':'closed')) sensorList << it
            	}
            	if (!settings.useSensorNames) {
                    message = thePrefix + "one or more " + theContact + " have been ${((settings.contactOpen==null)||settings.contactOpen)?'open':'closed'} ".capitalize() +
                        (((settings?.offDelay != '0') && settings?.includeDelays) ? "for ${settings.offDelay} minute${settings.offDelay.toInteger()>1?'s':''}" : 'too long') + ', ' +
                        (doHVAC ? (theTstat + ' now off') : ('you should turn off ' + justTheStats))
                } else {
        			def nameList = []
                    if (settings?.addThePrefix) {
                        sensorList.each {
                            nameList << ('the ' + it.displayName)
                        }
                    } else {
                        nameList = sensorList*.displayName
                    }
                    String sensorStr = textListToString(nameList)
                    if (settings?.contactCleaners != []) {
                        settings?.contactCleaners.each{
                            sensorStr = sensorStr.replace(it, '').replace(it.toLowerCase(), '').trim()	// Strip out any unnecessary words
                        }
                    }
                    sensorStr = sensorStr.replace(':','').replace('  ', ' ').replace(' ,', ',').trim()           
                    message = thePrefix + sensorStr + " ${numOpen()>1 ? 'have' : 'has'} been ${((settings.contactOpen==null)||settings.contactOpen) ? 'open' : 'closed'} " +
                                (((settings.offDelay != '0') && settings?.includeDelays) ? "for ${settings.offDelay} minute${settings.offDelay?.toInteger()>1?'s':''}" : 'too long') + ', ' +
                                (doHVAC ? (theTstat + ' now off') : ('you should turn off ' + justTheStats))
                }
                //log.debug "sending message @ ${now()}"
				sendMessage(message.capitalize())
            	notified = true		// only send 1 notification
        	}
        	if (!notified && settings.theSwitches) {
        		def switchList = []
            	theSwitches.each {
                	def currentSwitch = ST ? it.currentValue('switch') : it.currentValue('switch', true)
            		if (currentSwitch == (switchOn?'on':'off')) switchList << it
            	}
				if (!settings?.useSensorNames) {
                	message = thePrefix + "one or more " + theSwitch + " have been ${((settings.switchOn==null)||settings?.switchOn)?'on':'off'} " +
                        (((settings?.offDelay != '0') && settings?.includeDelays) ? "for ${offDelay} minute${settings?.offDelay?.toInteger()>1?'s':''}" : 'too long') + ', ' +
                        (doHVAC ? (theTstat + ' now off') : ('you should turn off ' + justTheStats))
				} else {
                	def nameList = []
                	if (settings?.addThePrefix) {
						switchList.each {
							nameList << ('the ' + it.displayName)
                        }
                    } else {
                        nameList = switchList*.displayName
                    }
					String switchStr = textListToString(nameList)
					if (switchCleaners != []) {
						switchCleaners.each{
							switchStr = switchStr.replace(it, '').replace(it.toLowerCase(), '').trim()	// Strip out any unnecessary words
						}
					}
					switchStr = switchStr.replace(':', '').replace('  ', ' ').replace(' ,', ',').trim()
					// message = message + "${switchStr} ${(switchNames.size()>1)?'has':'have'} been "
                    message = thePrefix + switchStr + " ${numOpen()>1?'have':'has'} been ${((settings.switchOn==null)||settings?.switchOn)?'on':'off'} " +
                                (((settings?.offDelay != '0') && settings?.includeDelays) ? "for ${offDelay} minute${settings?.offDelay?.toInteger()>1?'s':''}" : 'too long') + ', ' +
                                (doHVAC ? (theTstat + ' now turned off') : ('you should turn off ' + justTheStats))
				}
				sendMessage(message.capitalize())
          		notified = true
        	}
        	if (notified) LOG('Notifications sent',3,null,'info')
    	}
    } else {
    	if (action.contains('Notify') && !quietly) {
        	sendMessage("${theStats} already off")
            LOG('All thermostats are already off',3,null,'info')
        }
    }
}

void turnOnHVAC(quietly = false) {
	// Restore previous state
    LOG("turnOnHVAC(quietly=${quietly}) entered...", 4,null,'debug')
	//boolean ST = isSTHub
    
	if (!allClosed() && (atomicState.HVACModeState == 'on_pending')) {
    	LOG("turnOnHVAC() called, but somethings is still open/on, ignoring request & leaving HVAC off",3,null,'info')
    	atomicState.HVACModeState = 'off'
    	return
    }
    
    atomicState.HVACModeState = 'on'
    String action = settings.whichAction ?: 'Notify Only'
    boolean doHVAC = action.contains('HVAC')
	LOG("turnOnHVAC() - action: ${action}, doHVAC: ${doHVAC}", 3, null, 'debug')
    boolean notReserved = true
	def theStats = settings.theThermostats ? settings.theThermostats : settings.myThermostats
	def tstatNames = []
	// log.debug "turnOnHVAC() - theStats: ${theStats}"
	def tmpThermSavedState = atomicState.thermSavedState ?: [:]
	LOG("turnOnHVAC() - thermSavedState = ${atomicState.thermSavedState}", 3, null, 'debug')
    if (doHVAC) {
        theStats.each { therm ->
			LOG("Working on thermostat: ${therm}", 3, null, 'info')
            // if (!tstatNames.contains(therm.displayName)) tstatNames << therm.displayName
            def tid = getDeviceId(therm.deviceNetworkId)
            if (!tmpThermSavedState || !tmpThermSavedState[tid]) tmpThermSavedState[tid] = [:]
            
            String priorMode = settings.defaultMode  
           	if (settings.quietTime) {
                // Turn on quiet time
                def onOff = settings.qtOn=='on' ? 'off' : 'on'
                qtSwitch."$onOff"()
                LOG("${therm.displayName} Quiet Time disabled (${qtSwitch.displayName} turned ${onOff})",3,null,'info')
            } else if ((settings.hvacOff == null) || settings.hvacOff) {
                // turn on the HVAC
                def oldMode = ST ? therm.currentValue('thermostatMode') : therm.currentValue('thermostatMode', true) 
                def newMode = tmpThermSavedState[tid]?.mode ?: 'auto'
                LOG("Current HVAC mode: ${oldMode}, desired HVAC mode: ${newMode}", 3, null, 'info')
                if (newMode != oldMode) {
                    def i = countReservations( tid, 'modeOff' ) - (haveReservation(tid, 'modeOff') ? 1 : 0)
                    // log.debug "count=${countReservations(tid,'modeOff')}, have=${haveReservation(tid,'modeOff')}, i=${i}"
                    if ((oldMode == 'off') && (i > 0)) {
                        // Currently off, and somebody besides me has a reservation - just release my reservation
                        cancelReservation(tid, 'modeOff')
                        notReserved = false							
                        LOG("Cannot change ${therm.displayName} to ${newMode.capitalize()} - ${getGuestList(tid, 'modeOff').toString()[1..-2]} hold 'modeOff' reservations",1,null,'warn')
                    } else {
                        // Not off, or nobody else but me has a reservation
                        cancelReservation(tid, 'modeOff')
                        if (tmpThermSavedState[tid].containsKey('wasAlreadyOff') && (tmpThermSavedState[tid].wasAlreadyOff == false)) {
                            tmpThermSavedState[tid].HVACModeState = 'on'
                            tmpThermSavedState[tid].mode = newMode
                            therm.setThermostatMode( newMode )                            
                            if (!tstatNames.contains(therm.displayName)) tstatNames << therm.displayName		// only report the ones that aren't off already
                            LOG("${therm.displayName} ${newMode.capitalize()} Mode restored (was ${oldMode.capitalize()})",3,null,'info')
                        } else {
                            LOG("${therm.displayName} was already off, not turning back on",3,null,'info')
                        }
                    } 
                } else {
                    LOG("${therm.displayName} is already in ${newMode.capitalize()}",3,null,'info')
                    tmpThermSavedState[tid].HVACModeState = 'on'
                }
            } else if (settings.adjustSetpoints) {
                // Restore the prior values
                tmpThermSavedState[tid].HVACModeState = 'on'
                def holdType = tmpThermSavedState[tid].holdType
                if (holdType == '') holdType = 'nextTransition'
                if (tmpThermSavedState[tid].currentProgram == tmpThermSavedState.scheduledProgram) {
                    // we were running the scheuled program when we turned off - just return to the currently scheduled program
                    therm.resumeProgram()
                    LOG("${therm.displayName} resumed current program",3,null,'info')
                } else if (tmpThermSavedState[tid].currentProgramName == ('Hold: ' + tmpThermSavedState[tid].currentProgram)) {
                    // we were in a hold of a named program - reinstate the hold IF the saved scheduledProgram == the current scheduledProgram
                    def scheduledProgram = ST ? therm.currentValue('scheduledProgram') : therm.currentValue('scheduledProgram', true)
                    if (tmpThermSavedState[tid].scheduledProgram == scheduledProgram) {
                        therm.setThermostatProgram(tmpThermSavedState[tid].currentProgram, holdType)
                        LOG("${therm.displayName} returned to ${tmpThermSavedState[tid]} program (${holdType})",3,null,'info')
                    }
                } else {
                    // we were in some other sort of hold - so set a new hold to the original values
                    def h = tmpThermSavedState[tid].heatSP
                    def c = tmpThermSavedState[tid].coolSP
                    tmpThermSavedState[tid].heatAdj = 999.0
                    tmpThermSavedState[tid].coolAdj = 999.0
                    therm.setHeatingSetpoint(h, holdType) // should probably be the current holdType
                    therm.setCoolingSetpoint(c, holdType)
                    LOG("${therm.displayName} heatingSetpoint returned to ${h}, coolingSetpoint to ${c} (${holdType})",3,null,'info')
                }
            }

		} 
	}
    updateMyLabel()
    atomicState.thermSavedState = tmpThermSavedState
	LOG("turnOnHVAC() - thermSavedState: ${atomicState.thermSavedState}",4,null,'debug')
    
    if ( action.contains('Notify') && !quietly ) {
		if (!doHVAC && (tstatNames == [])) tstatNames = theStats*.displayName
    	boolean notified = false
        Integer delay = (settings.onDelay != null ? settings.onDelay : 0) as Integer
		def tstatModes = ST ? theStats*.currentValue('thermostatMode') : theStats*.currentValue('thermostatMode', true)
		def isOff = tstatModes?.contains('off')
		String theStatsStr = getMsgTstat()
		String justTheStats = theStatsStr.endsWith(' is') ? theStatsStr[0..-3] : (theStatsStr.endsWith(' are') ? theStatsStr[0..-4] : theStatsStr)
        String message = ""
    	if (settings.contactSensors) {
            message = getMsgPrefix() + "${contactSensors.size()>1?'all of':''} the ${getMsgContact()} have been "
            if (delay == 0) {
                message = message + "${contactOpen?'closed':'opened'}, "
            } else if ((delay != 0) && settings?.includeDelays) {
                message = message + "${contactOpen?'closed':'open'} for ${delay} minute${delay>1?'s':''}, "
            } else {
                message = message + "${contactOpen?'closed':'opened'}, "
            }
            notified = true
        }
        if (theSwitches) {
        	if (notified) message = message + ' and '
            message = message + "${theSwitches.size()>1?'all of':''} the ${getMsgSwitch()} have been "
            if (delay == 0) {
				message = message + "turned ${switchOn?'off':'on'}, "
			} else if ((delay != 0) && settings?.includeDelays) {
				message = message + "${switchOn?'off':'on'} for ${delay} minute${delay>1?'s':''}, "
			} else {
				message = message + "turned ${switchOn?'off':'on'}, "
			}
        }
		message = message + (doHVAC ? (notReserved ? (theStatsStr + ' now back on') : ('but reservations block turning on ' + justTheStats)) : ('you could turn on ' + justTheStats))
		sendMessage(message.replace(':', '').replace('  ', ' ').replace(' ,', ',').trim().capitalize())
        LOG('Notifications sent',3,null,'info')
    }    
}

boolean allClosed() {
	//boolean ST = isSTHub
	// Check if all Sensors are in "HVAC ON" state   
    def response = true
    String txt = ''
    if (contactSensors) {
    	if (contactOpen) {
        	txt = 'closed'
            def currentContacts = ST ? contactSensors*.currentContact : contactSensors*.currentValue('contact', true)
        	if (currentContacts.contains('open')) response = false
        } else {
        	txt = 'open'
        	def currentContacts = ST ? contactSensors*.currentContact : contactSensors*.currentValue('contact', true)
        	if (currentContacts.contains('closed')) response = false
        }
        if (response) LOG("All contact sensors are ${txt}",3,null,'info')
    }
    txt = ''
    if (response && theSwitches) {
    	if (switchOn) {
        	txt = 'off'
        	def currentSwitches = ST ? theSwitches*.currentSwitch : theSwitches*.currentValue('switch', true)
        	if (currentSwitches.contains('on')) response = false
        } else {
        	txt = 'on'
        	def currentSwitches = ST ? theSwitches*.currentSwitch : theSwitches*.currentValue('switch', true)
        	if (currentSwitches.contains('off')) response = false
        }
        if (response) LOG("All switches are ${txt}",3,null,'info')
    }
    LOG("allClosed(): ${response}",4,null,'info')
    return response
}

def numOpen() {
	//boolean ST = isSTHub
	def response = 0
	if (settings.contactSensors) {
		if (settings.contactOpen) {
			def currentContacts = ST ? contactSensors*.currentContact : contactSensors*.currentValue('contact', true)
        	response = currentContacts.count { it == 'open' }
		} else {
			def currentContacts = ST ? contactSensors*.currentContact : contactSensors*.currentValue('contact', true)
        	response = currentContacts.count { it == 'closed' }
		}
	}
	if (settings.theSwitches) {
		if ( settings.switchOn ) {
        	def currentSwitches = ST ? theSwitches*.currentSwitch : theSwitches*.currentValue('switch', true)
			response += currentSwitches.count { it == 'on' }
		} else {
			def currentSwitches = ST ? theSwitches*.currentSwitch : theSwitches*.currentValue('switch', true)
			response += currentSwitches.count { it == 'off' }
		}
	}
    LOG("numOpen(): ${response}",3,null,'info')
	return response
}

// Helper functions
String getDeviceId(networkId) {
    return networkId.split(/\./).last()
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
String getMsgContact() {
	String theContact = ""
	if (settings?.contactSensors) {
    	boolean s = true // settings.contactSensors.size() > 1
		if (settings?.customContact == null) {
			app.updateSetting('customContact', 'contact sensors')
			settings?.customContact = 'contact sensors'
			theContact = s ? 'contact sensors' : 'contact sensor'
		} else {
			theContact = (settings?.customContact == 'custom') ? settings?.customContactText : settings?.customContact
            if (!s) {
            	if (theContact == 'doors') theContact = 'door'
                else if (theContact == 'windows') theContact = 'window'
                else if (theContact == 'doors and/or windows') theContact = 'door or window'
            }
		}
	}
	return theContact
}
String getMsgSwitch() {                       
	String theSwitch = ""
	if (settings?.theSwitches) {
		if (settings?.customSwitch == null) {
			app.updateSetting('customSwitch', 'switches')
			settings?.customSwitches = 'switches'
			theSwitch = 'switches'
		} else {
			theSwitch = (settings?.customSwitch == 'custom') ? settings?.customSwitchText : settings?.customSwitch
		}
	}
	return theSwitch
}
String getMsgTstat() {						
	String theTstat = ""
	if (settings?.customTstat == null) { app.updateSetting('customTstat', '(thermostat names) is/are'); settings?.customTstat = '(thermostat names) is/are'; }
	switch (settings.customTstat) {
		case 'custom':
			theTstat = settings.customTstatText 
			break
		case "(thermostat names) is/are":
			def stats = settings?.theThermostats ?: myThermostats
			def nameList = []
			if (settings?.tstatSuffix || settings?.tstatPrefix) {
				stats.each {
					def name = it.displayName
					if (settings.tstatPrefix) name = settings.tstatPrefix + ' ' + name
					if (settings.tstatSuffix) name = name + ' ' + settings.tstatSuffix
					nameList << name
				}
			} else {
				nameList = stats*.displayName
			}
			String statStr = textListToString(nameList)
			if (tstatCleaners != []) {
				tstatCleaners.each{
					if ((!settings?.tstatSuffix || (settings.tstatSuffix != it)) && (!settings?.tstatPrefix || (settings.tstatPrefix != it))) {	// Don't strip the prefix/suffix we added above
						statStr = statStr.replace(it, '').replace(it.toLowerCase(), '')	// Strip out any unnecessary words
					}
				}
			}
			statStr = statStr.replace(':','').replace('  ', ' ').trim()		// Finally, get rid of any double spaces
			theTstat = (statStr + ((stats?.size() > 1) ? ' are' : ' is'))	
			break
		case 'the HVAC system':
			theTstat = 'the H V A C system is'
			break
	}
	return theTstat
}

void sendMessage(notificationMessage) {
    String action = settings.whichAction ?:'Notify Only'
    boolean notify = action.contains("Notify")
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 2, null, "info")
    if (notify) {
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
    //log.debug "message sent @ ${now()}"
}
void updateMyLabel() {
	String flag
    if (ST) {
    	def opts = [ ' (paused', ' (HVAC: ']
        opts.each {
        	if (!flag && app.label.contains(it)) flag = it
        }
    } else flag = '<span '
	
	String myLabel = atomicState.appDisplayName
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!flag || !myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		myLabel = myLabel.substring(0, myLabel.indexOf(flag))
		atomicState.appDisplayName = myLabel
	}
    String newLabel
	if (settings.tempDisable) {
		newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
    	String myStatus = atomicState.HVACModeState
        if (!myStatus) myStatus = 'initializing...'
        if (ST) {
        	newLabel = myLabel + " (HVAC: ${myStatus})"
        } else {
        	String color = myStatus.contains('off') ? 'red' : 'green'
        	newLabel = myLabel + "<span style=\"color:${color}\"> (HVAC: ${myStatus})</span>"
        }
		if (app.label != newLabel) app.updateLabel(newLabel)
	}
}
def pauseOn() {
	// Pause this Helper
	atomicState.wasAlreadyPaused = (settings.tempDisable && !atomicState.globalPause)
	if (!settings.tempDisable) {
		LOG("performing Global Pause",3,null,'trace')
		app.updateSetting("tempDisable", true)
		atomicState.globalPause = true
		runIn(2, updated, [overwrite: true])
	} else {
		LOG("was already paused, ignoring Global Pause",3,null,'trace')
	}
}
def pauseOff() {
	// Un-pause this Helper
	if (settings.tempDisable) {
		def wasAlreadyPaused = atomicState.wasAlreadyPaused
		if (!wasAlreadyPaused) { // && settings.tempDisable) {
			LOG("performing Global Unpause",3,null,'trace')
			app.updateSetting("tempDisable", false)
			runIn(2, updated, [overwrite: true])
		} else {
			LOG("was paused before Global Pause, ignoring Global Unpause",3,null,'trace')
		}
	} else {
		LOG("was already unpaused, skipping Global Unpause",3,null,'trace')
		atomicState.wasAlreadyPaused = false
	}
	atomicState.globalPause = false
}
void clearReservations() {
	if (settings.theThermostats) {
		theThermostats?.each {
			cancelReservation(getDeviceId(it.deviceNetworkId), 'modeOff')
		}
	}
	if (settings.myThermostats) {
		myThermostats?.each {
    		cancelReservation(getDeviceId(it.deviceNetworkId), 'modeOff')
		}
	}
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
