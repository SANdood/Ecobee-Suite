/**
 *  ecobee Suite Smart Humidity
 *
 *  Copyright 2020 Barry A. Burke
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
 *	1.7.00 - Initial development start
 *	1.7.01 - Prep for testing
 *	1.7.02 - Lots of changes
 *				- "Days to shed excess humidity"
 *				- "Thermal Efficiency factor" instead of "Window Efficiency"
 *				- TE factor 0-10 (4.667% per step), instead of 1-7 (7% per step)
 *				- Begin transition to ST testing
 *				- Minimized text where possible
 *				- Added 'Vacation' as a program selection
 *				- Reformatted options table (now HE-only)
 *	1.7.03 - Completed ST port
 *	1.7.04 - Fixed custom labelling (again)
 *	1.7.05 - Optimizations, added 'Custom' thermal efficiency option
 *	1.7.06 - Added dewpoint-based adjustments for internal temp (default values are for 70.0°F)
 *	1.7.07 - Added minimize UI
 *	1.8.00 - Version synchronization, updated settings look & feel
 *	1.8.01 - General Release
 *	1.8.02 - More busy bees
 *	1.8.03 - No longer LOGs to parent (too much overhead for too little value)
 *	1.8.04 - New SHPL, using Global Fields instead of atomicState
 */
import groovy.json.*
import groovy.transform.Field

String getVersionNum()		{ return "1.8.04" }
String getVersionLabel() 	{ return "Ecobee Suite Smart Humidity Helper, version ${getVersionNum()} on ${getHubPlatform()}" }

definition(
	name: 				"ecobee Suite Smart Humidity",
	namespace: 			"sandood",
	author: 			"Barry A. Burke (storageanarchy at gmail dot com)",
	description: 		"INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nAdjusts the Humidity Setpoint based on outdoor forecasted low temperatures and thermal efficiency.",
	category: 			"Convenience",
	parent: 			"sandood:Ecobee Suite Manager",
	iconUrl:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-1x.jpg",
	iconX2Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-2x.jpg",
    iconX3Url:			"https://raw.githubusercontent.com/SANdood/Icons/master/Ecobee/ecobee-logo-3x.jpg",
    importUrl:			"https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-humidity.src/ecobee-suite-smart-humidity.groovy",
    documentationLink:	"https://github.com/SANdood/Ecobee-Suite/blob/master/README.md#features-smart-humid-sa",
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
	boolean humidifierEnabled = false
	def hasHumidifier
    def cTemp		// current indoor temperature
    boolean maximize = (settings?.minimize) == null ? true : !settings.minimize
    String unit = temperatureScale
    String defaultName = "Smart Humidity"
	
	dynamicPage(name: "mainPage", title: pageTitle(getVersionLabel().replace('per, v',"per\nV")), uninstall: true, install: true) {
    	if (maximize) {
            section(title: inputTitle("Helper Description & Release Notes"), hideable: true, hidden: (atomicState.appDisplayName != null)) {
                if (ST) {
                    paragraph(image: theBeeUrl, title: app.name.capitalize(), "")
                } else {
                    paragraph(theBeeLogo+"<h4><b>  ${app.name.capitalize()}</b></h4>")
                }
                paragraph("This Helper dynamically and continuously adjusts the relative humidity setpoint of your HVAC system's humidifier based on the recommended 'ideal humidity' using the ambient "+
                          "indoor temperature, the forecasted outdoor low temperatures and the humidity retention efficiency of your structure. Optionally, Smart Humidity will also raise the humidity setpoint as the inside "+
                          "temperature drops (e.g., overnight) in order to attain the desired humidity comfort when the heating setpoints increase again (e.g., in the morning).\n\n"+
                          "Importantly, Smart Humidity will reduce the indoor humidity setpoint as outdoor temperatures drop below freezing, seeking to avoid condensation on windows or (most importantly) "+
                          "inside your roofing and/or external walls, where it could support the growth of potentially toxic mold.\n\n"+
                          "For a safe and healthy home, it is important to adjust the parameters for your structure to target the highest humidity level that doesn't condense inside the structure when temperatures drop. For"+
                          "multi-zoned systems, keeping the humidity lower in the upper-level zones may help avoid hidden condensation (such as, inside unheated attics and crawl spaces).")
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
            	String flag
    			def opts = [' (paused', ' (active', ' (inactive', '%)']
				opts.each {
					if (!flag && app.label.contains(it)) flag = it
				}
				if (flag) {
                	if ((atomicState?.appDisplayName != null) && (flag=='%)'? !atomicState?.appDisplayName.endsWith('%)') : !atomicState?.appDisplayName.contains(flag))) {
                    	app.updateLabel(atomicState.appDisplayName)
                    } else if (flag == '%)') {
                    	int i = app.label.reverse().indexOf('(') + 2
                    	String myLabel = app.label.take(app.label.size()-i)
                    	atomicState.appDisplayName = myLabel
                    	app.updateLabel(myLabel)
                    } else {
                    	String myLabel = app.label.substring(0, app.label.indexOf(flag))
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
        		input(name: "theThermostat", type:"${ST?'device.ecobeeSuiteThermostat':'device.EcobeeSuiteThermostat'}", title: inputTitle("Select an Ecobee Suite Thermostat"), 
					  required: true, multiple: false, submitOnChange: true)
				if (settings?.theThermostat) {
                	def rh = ST ? settings.theThermostat.currentValue('humidity') : settings.theThermostat.currentValue('humidity', true)
                    cTemp = ST ? settings.theThermostat.currentValue('temperature') : settings.theThermostat.currentValue('temperature', true)
                    def dp = calculateDewpoint(cTemp, rh, unit)
					//paragraph "The current Relative Humidity for thermostat ${settings.theThermostat.displayName} is ${settings.theThermostat.currentValue('humidity')}%"
					String paraString = "The relative humidity on ${settings.theThermostat.displayName} is ${rh}%, the indoor temperature is "+
                    					"${cTemp}°${unit}, and the dewpoint is ~${dp}°${unit}"
					hasHumidifier = theThermostat.currentValue('hasHumidifier')
					if (!hasHumidifier) {
						paraString += ", but the thermostat is not currently configured to control a humidifier. Please reconfigure the thermostat, or choose another thermostat.\n"
						paragraph paraString
						app.updateSetting('theThermostat', null)
						settings.theThermostat = null
					} else {
						String humidifierMode = ST ? settings.theThermostat.currentValue('humidifierMode') : settings.theThermostat.currentValue('humidifierMode', true)
						if (humidifierMode == 'auto') {
							paraString += ", but the humidifier is in Frost Control mode with a humidity setpoint of ${theThermostat.currentValue('humiditySetpoint')}%.\n\n" +
											"Smart Humidity cannot adjust the humidity setpoint while the humidifier is in Frost Control mode."
							paragraph paraString
							humidifierEnabled = false
						} else if ((humidifierMode == 'manual') || (humidifierMode == 'on')) {
                        	def rhSp = ST ? theThermostat.currentValue('humiditySetpoint') : theThermostat.currentValue('humiditySetpoint', true)
							paraString += ". The humidifier is in Manual/On mode with a humidity setpoint of ${rhSp}%."
							paragraph paraString
							humidifierEnabled = true
						} else {
							paraString += ", but the humidifier is currently Off."
							paragraph paraString
							humidifierEnabled = false
						}
						if (!humidifierEnabled) paragraph "To continue, please manually set the humidifier to Manual/On mode to enable Smart Humidity."	
					}
				}
            }
		}
        
        if (!settings?.tempDisable && settings?.theThermostat && hasHumidifier && humidifierEnabled) {
        	section(title: sectionTitle("Configuration")) {
            	def forecastRaw = ST ? settings?.theThermostat?.currentValue('weatherTempLowForecast') : settings?.theThermostat?.currentValue('weatherTempLowForecast', true)
                def forecastLows = forecastRaw.split(',')	// [0]: todayLow, [1]: tomorrowLow, ... - provided in °F - convert when displaying
				Calendar myDate = Calendar.getInstance(); // set this up however you need it.
				int dow = myDate.get (Calendar.DAY_OF_WEEK);
				def dayNames = ['',"Sun:\t","Mon: ","Tue:\t","Wed:","Thu:\t","Fri:\t","Sat:\t"]
				def days = [dow, ((dow%7)+1), (((dow+1)%7)+1), (((dow+2)%7)+1), (((dow+3)%7)+1)]
				dayNames[days[0]] = 'Today:'
				dayNames[days[1]] = 'Tmrw:'
				def dayOptions = ['0':'1 Day', '1':'2 Days', '2':"3 Days", '3':"4 Days", '4':"5 Days"]
				if (maximize) paragraph "In the winter, it is generally recommended that you maintain interior humidity levels between 15 and 50% for your health, safety and comfort"
				input(name: "minHumidity", type: 'number', title: inputTitle('Desired minimum humidity (10-55%)'), defaultValue: 15, range: '10..55', submitOnChange: true, required: true, width: 6)
				input(name: "maxHumidity", type: 'number', title: inputTitle('Desired maximum humidity (10-55%)'), defaultValue: 50, range: '10..55', submitOnChange: true, required: true, width: 6)
				def minHum = (settings?.minHumidity?:15) as Integer
				def maxHum = (settings?.maxHumidity?:50) as Integer
				if (maximize) paragraph "Smart Humidity uses the forecasted outdoor low temperatures from your thermostat and the current indoor temperature to dynamically adjust the humidity setpoint "+
                		  "between ${minHum}% and ${maxHum}%.\n\n" +
						  "The objective is to keep the humidity as high as possible without causing condensation or frost on interior surfaces when outdoor temperatures drop.\n\n" +
						  "You can tune the algorithm using the following parameters:"
				input(name: 'dayString', type: 'enum', multiple: false, defaultValue: 2, required: true, options: dayOptions, 
                	  title: inputTitle("How quickly does this structure shed excess humidity?")+' (1-5 days - longer if tightly sealed/unventilated)', submitOnChange: true, width: 4)
				int dayNumber = settings?.dayString ? settings.dayString.toInteger() : 2

				def strategyOptions = ['Actual':"Actual Low Temp ${dayNumber==0?'today':'on day '+(dayNumber+1).toString()}", 
                      					'Average':"Average Low Temp for ${dayNumber==0?'today':'the next '+(dayNumber+1).toString()+' days'}", 
                      					 'Lowest':"Lowest Low Temp ${dayNumber==0?'today':'of the next '+(dayNumber+1).toString()+' days'}"]
				input(name: 'smartStrategy', type: 'enum', multiple: false, required: true, title: inputTitle("Select a Smart Humidity strategy") + 
					  " (recommended strategy: 'Lowest Low Temp')", defaultValue: 'Lowest', submitOnChange: true, width: 4,
					  options: strategyOptions)
					  
				def strategy = settings?.smartStrategy
				if (!strategy) {
					strategy = 'Lowest'
					app.updateSetting('smartStrategy', 'Lowest')
					settings.smartStrategy = 'Lowest'
				}

				input(name: 'thermalFactor', type: 'enum', title: inputTitle("Thermal Efficiency Factor")+' (0-10: the highest factor that keeps inside walls, windows & attic frost-free)', 
					  options: ['-22.167':'0','-17.5':'1','-12.833':'2','-8.667':'3','-3.5':'4','0.0':'None','1.667':'5','5.833':'6','10.5':'7','15.167':'8','19.833':'9','24.5':'10','custom':'Custom'], 
					  required: true, defaultValue: '0.0', submitOnChange: true, width: 4)
				def tFactor
				if (!settings?.thermalFactor) {
					app.updateSetting('thermalFactor','0.0')
					settings.thermalFactor = '0.0'
					tFactor = 0.0
				} else if (thermalFactor != 'custom') {
					tFactor = settings?.thermalFactor.toBigDecimal()
				} else {
                	if (HE) paragraph("", width: 8)
                	input(name: 'thermalCustom', type: 'decimal', title: inputTitle("Custom Smart Humidity setpoint adjustment")+" (-25.0 to 25.0)", defaultValue: 0.0, required: true, 
                    	  submitOnChange: true, range: '-25..25', width: 4)
                    if ((settings?.thermalCustom == null) || !settings?.thermalCustom?.toString()?.isNumber()) { app.updateSetting('thermalCustom', 0.0); settings?.thermalCustom = 0.0; }
                    tFactor = settings?.thermalCustom?.toBigDecimal()
                }
				String paraString = ""
				if (tFactor != 0.0) paraString += maximize ? "The selected Thermal Efficiency factor adjusts Smart Humidity by ${thermalFactor!='custom'?'approximately ':''}${tFactor>0.0?'+':''}${roundIt(tFactor,1)}%\n\n" : ""
				
                List actual = getHumidityTempArray('Actual')
                List average= getHumidityTempArray('Average')
                List lowest = getHumidityTempArray('Lowest')
                int i = 0
                List values = []
                if (strategy == 'Actual') {
                    values = actual
                } else if (strategy == 'Average') {
                    values = average
                } else if (strategy == 'Lowest') {
                    values = lowest
                }
                List humSetpoint = []
                while ((i<5) && values[i]) {
                    humSetpoint[i] = calcHumSetpoint( values[i] )
                    i++;
                }

                paraString +=  maximize ? "Using these settings, the Smart Humidity setpoint at the current indoor temperature (${cTemp}°${unit}) is ${calcHumSetpoint(values[0])}%" : ""
                paragraph paraString

                paraString = maximize ? "The Smart Humidity setpoints for the next ${humSetpoint.size()} days using the current indoor temperature and the forecasted outdoor low temperatures from "+
                			 "the ${theThermostat.displayName} thermostat, applying the Actual, Average, and Lowest strategies are:\n\n" : ""
                String ts = "\u2009"	// thin space
                if (ST) {
                    paraString += "   Forecast\t  Actual\tAverage\tLowest\tSP\n" 
                    for (int l=0; l<5; l++) {
                        // Note that up to now, we have been working in °F, as supplied in weatherTempLowForecast...convert for the display via mCTIN (myConvertTemperatureIfNeeded)
                        String Fo = String.format('%4.1f',mCTIN(forecastLows[l],'F',1)) + '/' + calcHumSetpoint(forecastLows[l]).toString()
                        
                        paraString = paraString + "${l==0?ts:''}${l+1}: ${l==0?'':''}${Fo}" +
                                                    "\t ${actual[l]?String.format('%4.1f',mCTIN(actual[l],'F',1)):'----'}${actual[l]?'/':'/'}${actual[l]?calcHumSetpoint(actual[l])+'':' --'}" +
                                                      " \t${average[l]?String.format('%4.1f',mCTIN(average[l],'F',1)):'----'}${average[l]?'/':'/'}${average[l]?calcHumSetpoint(average[l])+'':' --'}" +
                                                        " \t${lowest[l]?String.format('%4.1f',mCTIN(lowest[l],'F',1)):'----'}${lowest[l]?'/':'/'}${lowest[l]?calcHumSetpoint(lowest[l])+'':' --'}" +
                                                          " \t${humSetpoint[l]?humSetpoint[l]+'%':'----'}\n"
                    }
                } else {
                    paraString += "<b>Days\tForecast \tActual\t\tAverage   \tLowest   \t\tHumidity Setpoint</b>\n" 
                    for (int l=0; l<5; l++) {
                        // Note that up to now, we have been working in °F, as supplied in weatherTempLowForecast...convert for the display vi mCTIN (myConvertTemperatureIfNeeded)
                        //String.format('%02.1f',mCTIN(actual[l],'F',1)
                        paraString = paraString + "${l+1}${l==0?ts:''}\t\t${String.format('%04.1f',mCTIN(forecastLows[l],'F',1))}°/${calcHumSetpoint(forecastLows[l])}%" +
                                                    "\t${actual[l]?String.format('%04.1f',mCTIN(actual[l],'F',1)):' ---- '}${actual[l]?'°/':' / '}${actual[l]?calcHumSetpoint(actual[l])+'%':' ----'}" +
                                                      "\t${average[l]?String.format('%04.1f',mCTIN(average[l],'F',1)):' ---- '}${average[l]?'°/':' / '}${average[l]?calcHumSetpoint(average[l])+'%':' ----'}" +
                                                        "\t${lowest[l]?String.format('%04.1f',mCTIN(lowest[l],'F',1)):' ---- '}${lowest[l]?'°/':' / '}${lowest[l]?calcHumSetpoint(lowest[l])+'%':' ----'}" +
                                                          "\t${dayNames[days[l]]} \t${humSetpoint[l]?humSetpoint[l]+'%':' ----'}\n"
                    }
                }
                paragraph paraString
			}
			section(title: sectionTitle("Conditions")+"${ST?': ':''}"+smallerTitle("Modes & Programs")) {
			//section(title: smallerTitle("Modes & Programs")) {
				boolean multiple = false
				if (maximize) paragraph "By default, Smart Humidity adjusts the setpoint any time that the tuning parameters, internal temperature, and/or or the low temperature forecasts change"
				input(name: "theModes", type: "mode", title: inputTitle("Adjust when ${location.name}'s Location Mode is"), multiple: true, required: false, submitOnChange: true, width: 4)
                input(name: "statModes", type: "enum", title: inputTitle("Adjust when the ${settings?.theThermostat?:'thermostat'}'s Operating Mode is"), 
					  multiple: true, required: false, submitOnChange: true, options: getThermostatModesList(), width: 4)
				def programOptions = getProgramsList() + ['Vacation']
            	input(name: "thePrograms", type: "enum", title: inputTitle("Adjust when the ${settings.theThermostat?:'thermostat'}'s Program is"), 
					  multiple: true, required: false, submitOnChange: true, options: programOptions, width: 4)
				boolean any = (settings?.theModes || settings?.statModes || settings?.thePrograms)
				if ((settings.theModes && settings.statModes) || (settings.statModes && settings.thePrograms) || (settings.thePrograms && settings.theModes)) {
					multiple = true
					input(name: 'needAll', type: 'bool', title: inputTitle('Require ALL conditions to be met?'), required: true, defaultValue: false, submitOnChange: true)
				}
				if (any) {
					if (!multiple) {
						if (maximize) paragraph("Smart Humidity will only adjust the setpoint when the above condition is met")
					} else {
						if (maximize) paragraph("Smart Humidity will ${settings.needAll?'only ':''}adjust the setpoint when ${settings.needAll?'ALL':'ANY'} of the above conditions are met")	 
					}
				} else {
					if (maximize) paragraph "Smart Humidity will adjust the setpoint whenever the indoor temperature or outdoor low temperature forecast changes"
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

def getHumidityTemp() {
	return getHumidityTempArray(settings.smartStrategy)[0]
}
def getHumidityTempArray(strategy){
	//boolean ST = isST

	def values = []
	if (strategy) {
		def forecastRaw = ST ? settings?.theThermostat?.currentValue('weatherTempLowForecast') : settings?.theThermostat?.currentValue('weatherTempLowForecast', true)
    	def forecastLows = forecastRaw ? forecastRaw.split(',')	: [] // [0]: todayLow, [1]: tomorrowLow, ... - provided in °F - convert when displaying
		if (forecastLows.size() >= 5) {
			int dayNumber = settings?.dayString ? settings.dayString.toInteger() : 2

			switch (strategy) {
				case 'Actual':
					// Actual Low Temp of selected day
					int i = 0
					int j = i + dayNumber
					while ((i<5) && (j<5)){
						values[i] = forecastLows[j] as BigDecimal
						i++; j++;
					}
					break;

				case 'Average': 
					// Average of day range
					if (dayNumber == 0) {
						int i = 0
						int j = i + dayNumber
						while ((i<5) && (j<5)){
							values[i] = forecastLows[j] as BigDecimal
							i++; j++;
						}
					} else {
						int i = 0
						int c = 0
						while (i<5) {
							int j = i + dayNumber
							if (j < 5) {
								def total = 0.0
								for (int k=i; ((k<(j+1)) && (k<5)); k++) {
									total += forecastLows[k] as BigDecimal
									c++;
								}
								values[i] = roundIt((total / c),1)
							}
							i++; c = 0;
						}
					}
					break;

				case 'Lowest': 
					// Lowest Low Temp of day range
					if (dayNumber == 0) {
						int i = 0
						int j = i + dayNumber
						while ((i<5) && (j<5)){
							values[i] = forecastLows[j] as BigDecimal
							i++; j++;
						}
					} else {
						int i = 0
						while (i<5) {
							int j = i + dayNumber
							if (j < 5) {
								values[i] = forecastLows[i] as BigDecimal
								for (int k=i; ((k<(j+1)) && (k<5)); k++) {
									def fcl = forecastLows[k] as BigDecimal
									values[i] = (values[i] < fcl) ? values[i] : fcl
								}
							}
							i++;
						}
					}
					break;
			}
		}
	}
	return values
}

def calcHumSetpoint( temp ) {
	if (!temp) return null
    // First calculate target RH% at 70°F
    def tFactor = ( (settings?.thermalFactor == null) ? 0.0 : ((settings?.thermalFactor == 'custom') ? ( ((settings?.thermalCustom == null) || !settings?.thermalCustom?.toString()?.isNumber()) ? 0.0 : settings.thermalCustom) : settings.thermalFactor) ) as BigDecimal
	def minHum = (settings?.minHumidity?:15) as Integer
	def maxHum = (settings?.maxHumidity?:50) as Integer 
	int setpoint = roundIt(((50.0-((50.0-temp.toBigDecimal())/2.0))+tFactor), 0)
    // Second, calculate the effective dewpoint at 70°F
    def dp = calculateDewpoint( 70.0, setpoint, 'F') // dewpoint is returned in temperatureScale units
    // Third, adjust setpoint for the current temperature. Cooler air with same water content (dewpoint) will have a higher RH%.
    def cTemp = ST ? theThermostat.currentValue('temperature') : theThermostat.currentValue('temperature', true)
    if (temperatureScale == 'C') {
        setpoint = calculateRelHumidity( cTemp, dp, 'C')
    } else {
    	setpoint = (cTemp == 70.0) ? setpoint : calculateRelHumidity( cTemp, dp, 'F')
    }
	return (setpoint < minHum) ? minHum : ((setpoint > maxHum) ? maxHum : setpoint)
}

// Main functions
def installed() {
	LOG("Installed with settings ${settings}", 4, null, 'trace')
	initialize()  
}
def uninstalled() {
}
def updated() {
	LOG("Updated with settings ${settings}", 4, null, 'trace')
	unsubscribe()
    unschedule()
    initialize()
}
def initialize() {
	String version = getVersionLabel()
	LOG("${version} Initializing...", 2, null, 'info')
    atomicState.versionLabel = getVersionLabel()
	//boolean ST = isST
	
	updateMyLabel()

	// Now, just exit if we are disabled...
	if (settings.tempDisable) {
    	LOG("Temporarily Paused", 3, null, 'info')
    	return true
    }
	
	// Allow adjustments if Location Mode or Thermostat Program or Thermostat Mode is currently as configured
    // Also allow if none are configured
    boolean isOK = isOkNow()
    atomicState.isOK = isOK
	if (!isOK) {
		LOG("Not in specified Modes ${settings.needAll?'and':'or'} Programs, not updating Smart Humidity Setpoint now", 2, null, 'trace')
	} else {
		LOG("Initializing Smart Humidity Setpoint on ${theThermostat.displayName}, weatherTempLowForecast: ${theThermostat.currentValue('weatherTempLowForecast')} (°F)",2,null,'trace')
		def smartSetpoint =	calcHumSetpoint(getHumidityTemp())
		setSmartSetpoint( smartSetpoint )
		// LOG("Updated Smart Humidity Setpoint to ${smartSetpoint}%",2,null,'info')
	}
	
	subscribe(theThermostat, "weatherTempLowForecast", 	forecastChangeHandler)
	subscribe(theThermostat, "humidifierMode", 			forecastChangeHandler)
	subscribe(theThermostat, "humidity",				forecastChangeHandler)
    subscribe(theThermostat, "temperature",				forecastChangeHandler)	// this probably changes too frequently
	if (settings?.theModes)		subscribe(location, 	 'mode', 			forecastChangeHandler)
	if (settings?.thePrograms)	subscribe(theThermostat, 'currentProgram', 	forecastChangeHandler)
	if (settings?.statModes)	subscribe(theThermostat, 'thermostatMode', 	forecastChangeHandler)

	updateMyLabel()
    LOG("Initialization complete", 4, "", 'info')
    return true
}

boolean isOkNow() {
	//boolean ST = isST
	boolean isOK = true
	if (settings?.theModes || settings?.thePrograms  || settings?.statModes) {
		String currentProgram = settings?.thePrograms ? (ST ? settings?.theThermostat.latestValue('currentProgram') : settings?.theThermostat.latestValue('currentProgram', true)) : ""
		String thermostatMode = settings?.statModes	  ? (ST	? settings?.theThermostat.latestValue('thermostatMode') : settings?.theThermostat.latestValue('thermostatMode', true)) : ""
		if (settings.needAll) {
			isOK = 				settings?.theModes ?	settings.theModes.contains(location.mode) 		: true
			if (isOK) isOK = 	settings?.thePrograms ?	settings.thePrograms.contains(currentProgram) 	: true
			if (isOK) isOK = 	settings?.statModes ?	settings.statModes.contains(thermostatMode)		: true
		} else {
			isOK = 				(settings?.theModes	 	&& settings.theModes.contains(location.mode))
			if (!isOK) isOK = 	(settings?.thePrograms 	&& settings.thePrograms.contains(currentProgram))
			if (!isOK) isOK = 	(settings?.statModes 	&& settings.statModes.contains(thermostatMode))
		}
	}
    if (atomicState.isOK != isOK) atomicState.isOK = isOK
	return isOK
}

def setSmartSetpoint( setpoint ) {
	//boolean ST = isST
	boolean humidifierEnabled = true
	
	String humidifierMode = settings.theThermostat.currentValue('humidifierMode')
	if (!humidifierMode || (humidifierMode == 'off')) {
		LOG("Thermostat ${settings.theThermostat.displayName} reports that the humidifier is currently Off - cannot update Smart Humidity setpoint",1,null,'warn')
		humidifierEnabled = false
	} else if (humidifierMode == 'auto') {
		LOG("Thermostat ${settings.theThermostat.displayName}'s Humidifier is operating in 'Auto' (Frost Control) mode - cannot update Smart Humidity setpoint",1,null,'warn')
		humidifierEnabled = false
	}
	if (humidifierEnabled) {
		def currentSetpoint = ST ? settings.theThermostat.currentValue('humiditySetpoint') : settings.theThermostat.currentValue('humiditySetpoint', true)
		if (!atomicState.smartSetpoint || (atomicState.smartSetpoint != setpoint)) atomicState.smartSetpoint = setpoint
		if (currentSetpoint != setpoint) {
			settings.theThermostat.setHumiditySetpoint( setpoint )
            updateMyLabel()
			if (!infoOff) LOG("Updated Smart Humidity setpoint for ${settings.theThermostat.displayName} to ${setpoint}%",2,null,'info')
		}
	}
}

def forecastChangeHandler(evt=null) {
	// Just exit if we are disabled...
	if(settings.tempDisable == true) {
    	LOG("${atomicState.appDisplayName} is temporarily disabled, event $event.name ignored", 1, null, "warn")
    	return true
    }
    // Just in case we need to re-initialize anything
    def version = getVersionLabel()
    if (atomicState.versionLabel != version) {
    	LOG("Code updated: ${version}",1,null,'warn')
        atomicState.versionLabel = version
        runIn(2, updated, [overwrite: true])
        return
    }
    if (evt && (evt.name == 'humidity') && !infoOff) {
    	LOG("Humidity changed to ${evt.value}%",3,null,'info')
        return		// No need to recalculate, as current humidity isn't part of the algorithm, but it is useful to see in the logs
    }
    boolean isOK = isOkNow()
    if (isOK) {
    	switch(evt?.name) {
        	case 'weatherTempLowForecast':
    			if (!infoOff) LOG("Low Temperature Forecast changed to $evt.value (°F), recalculating setpoint",3,null,'info')
                break;
   			case 'temperature':
                if (evt.numberValue != null) {
                    // Skip small temperature changes
                    def d = (evt.unit == 'F') ? 0 : 1
                    def cTemp = roundIt(evt.numberValue, d)
                    if ((atomicState.lastTemp != null) && (atomicState.lastTemp == cTemp)) return
                    if (!infoOff) LOG("Temperature changed to ${cTemp}°$evt.unit, recalculating setpoint",3,null,'info')
                    atomicState.lastTemp = cTemp
                }
                break;
            default:
            	if (!debugOff) LOG("$evt.name: $evt.value ($evt.unit), recalculating setpoint",3,null,'debug')
        }
    	runIn(3, delayedUpdate, [overwrite: true])	// collapse multiple concurrent events
    } else {
    	if (!debugOff) LOG("Skipping event $evt.name: $evt.value ${evt.unit?'('+evt.unit+') ':''}because of Conditions",3,null,'debug')
    }
}

def delayedUpdate() {
	def smartSetpoint =	calcHumSetpoint(getHumidityTemp())
    //log.debug "smartSetpoint is: ${smartSetpoint}%"
	setSmartSetpoint( smartSetpoint )
}

// HELPER FUNCTIONS
// Temporary/Global Pause functions
void updateMyLabel() {
	//boolean ST = isST
    
	String flag
	if (ST) {
    	def opts = [' (paused', ' (active ', ' (inactive', '%)']
		opts.each {
			if (!flag && app.label.contains(it)) flag = it
		}
	} else {
		flag = '<span '
	}
    
    String myLabel = atomicState.appDisplayName	
	if ((myLabel == null) || !app.label.startsWith(myLabel)) {
		myLabel = app.label
		if (!flag || !myLabel.contains(flag)) atomicState.appDisplayName = myLabel
	} 
	if (flag && myLabel.contains(flag)) {
		if (flag != '%)') {
        	myLabel = myLabel.substring(0, myLabel.indexOf(flag))
        } else {
            int i = app.label.reverse().indexOf('(') + 2		// "${app.label} (nn%)" --> "${app.label}"
            myLabel = app.label.take(app.label.size()-i)
        }
		atomicState.appDisplayName = myLabel
	}
    def active = atomicState.isOK
	if (settings.tempDisable) {
		def newLabel = myLabel + ( ST ? ' (paused)' : '<span style="color:red"> (paused)</span>' )
		if (app.label != newLabel) app.updateLabel(newLabel)
	} else {
    	def newLabel = myLabel + (ST ? " (${active?'active ':'inactive '}${atomicState.smartSetpoint}%)" : '<span style="color:green"> (' + (active?'active ':'inactive ') + atomicState.smartSetpoint + '%)</span>')
        if (app.label != newLabel) app.updateLabel(newLabel)
    }// else {
	//	if (app.label != myLabel) app.updateLabel(myLabel)
	//}
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
// Thermostat Programs & Modes
def getProgramsList() {
	def programs = ["Away","Home","Sleep"]
	if (theThermostat) {
    	def pl = theThermostat.currentValue('programsList')
        if (pl) programs = new JsonSlurper().parseText(pl)
    }
    return programs.sort(false)
}
def getThermostatModesList() {
	def statModes = ["off","heat","cool","auto","auxHeatOnly"]
    if (settings.theThermostat) {
    	def tempModes = theThermostat.currentValue('supportedThermostatModes')
        if (tempModes) statModes = tempModes[1..-2].tokenize(", ")
    }
    return statModes.sort(false)
}
def mCTIN(scaledSensorValue, cmdScale, precision) {
	myConvertTemperatureIfNeeded(scaledSensorValue, cmdScale, precision)
}
def myConvertTemperatureIfNeeded(scaledSensorValue, cmdScale, precision) {
	if (scaledSensorValue == null) {
		LOG("Illegal sensorValue (null)", 1, null, "error")
		return null
	}
    if ( (cmdScale != 'F') && (cmdScale != 'C') ) {
    	if (cmdScale == 'dF') { 
        	cmdScale = 'F' 		// Normalize
        } else if (cmdScale == 'dC') { 
        	cmdScale = 'C'		// Normalize
        } else {
			LOG("Invalid temp scale used: ${cmdScale}", 1, null, "error")
			return roundIt(scaledSensorValue, precision)
        }
	}
	if (cmdScale == temperatureScale) {
        return roundIt(scaledSensorValue, precision)
	} else if (cmdScale == 'F') {				
        return roundIt(fToC(scaledSensorValue), precision)
	} else {
        return roundIt(cToF(scaledSensorValue), precision)
	}
}
// Calculate a close approximation of Dewpoint based on Temperature & Relative Humidity
//    TD: =243.04*(LN(RH/100)+((17.625*T)/(243.04+T)))/(17.625-LN(RH/100)-((17.625*T)/(243.04+T)))
def calculateDewpoint( temp, rh, units) {
	def t = ((units == 'C') ? temp : fToC(temp)) as BigDecimal
	def dpC = 243.04*(Math.log(rh/100.0)+((17.625*t)/(243.04+t)))/(17.625-Math.log(rh/100.0)-((17.625*t)/(243.04+t)))
    return (units == 'C') ? roundIt(dpC, 2) : roundIt(cToF(dpC), 1)
}
// Calculate a close approximation of Relative Humidity based on Temperature and Dewpoint
//    RH: =100*(EXP((17.625*TD)/(243.04+TD))/EXP((17.625*T)/(243.04+T)))
def calculateRelHumidity( temp, dp, units) {
	def t  = ((units == 'C') ? temp : fToC(temp)) as BigDecimal
    def td = ((units == 'C') ? dp	: fToC(dp))   as BigDecimal
    def rh = 100*((Math.exp((17.625*td)/(243.04+td)))/(Math.exp((17.625*t)/(243.04+t))))
    return roundIt(rh, 0)
}
def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_DOWN) 
}
def roundIt( BigDecimal value, decimals=0) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_DOWN) 
}
def cToF(temp) {
	return (temp != null) ? ((temp * 1.8) + 32) : null
}
def fToC(temp) {	
	return (temp != null) ? ((temp - 32) / 1.8) : null
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
