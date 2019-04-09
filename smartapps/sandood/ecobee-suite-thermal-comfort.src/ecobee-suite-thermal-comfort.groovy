/**
 *  Ecobee Suite Thermal Comfort
 *
 *  Copyright 2018 Justin Leonard, Barry A. Burke
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
 *	1.6.23 - Initial release
 */
def getVersionNum() { return "1.6.23" }
private def getVersionLabel() { return "Ecobee Suite Thermal Comfort Helper, version ${getVersionNum()}" }

definition(
	name: "ecobee Suite Thermal Comfort",
	namespace: "sandood",
	author: "Justin J. Leonard, Barry A. Burke, and Richard Peng",
	description: "INSTALL USING ECOBEE SUITE MANAGER ONLY!\n\nSets Ecobee Temperature based on relative humidity using PMV.",
	category: "Convenience",
	parent: "sandood:Ecobee Suite Manager",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
	singleInstance: false,
    pausable: true
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
    def unit = getTemperatureScale()
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
            3.4: 'Dancing (3.4)',
            3.8: 'Tennis (3.8)',
            6.3: 'Basketball (6.3)',
            7.8: 'Wrestling (7.8)',
            'custom': 'Custom'
    ]
    def cloOptions = [
            0.0: 'Naked (0.0)',
            0.6: 'Typical summer indoor clothing (0.6)',
            1.0: 'Typical winter indoor clothing (1.0)',
            2.9: 'Summer lightweight duvet [0.64-2.9] (2.9)',
            6.8: 'Spring/Autumn weight duvet [4.5-6.8] (6.8)',
            8.7: 'Winter weight duvet [7.7-8.7] (8.7)',
            'custom': 'Custom'
    ]
    def pmvRange = '-5..5'
	dynamicPage(name: "mainPage", title: "${getVersionLabel()}", uninstall: true, install: true) {
    	section(title: "Name for Thermal Comfort Helper") {
        	label title: "Name this Helper", required: true, defaultValue: "Thermal Comfort"
        }
        section(title: "${settings.tempDisable?'':'Select Thermostat(s)'}") {
        	if(settings.tempDisable) { paragraph "WARNING: Temporarily Disabled as requested. Turn back on to activate handler."}
        	else {input ("thermostats", "device.ecobeeSuiteThermostat", title: "Pick Ecobee Thermostat(s)", required: true, multiple: true, submitOnChange: true)}
		}
        if (!settings.tempDisable) {
            section(title: "Sensors") {
                input(name: 'thermometer', type: 'capability.temperatureMeasurement', title: "Which Temperature Sensor?", description: 'Tap to choose...', required: true, multiple: false)
                input(name: 'humidistat', type: 'capability.relativeHumidityMeasurement', title: "Which Relative Humidity Sensor?", description: 'Tap to choose...', required: true, multiple: false)
            }
			section(title: "Comfort Settings") {
       			input(name: "coolPmv", title: "PMV in cool mode${settings.coolPmv!=null&&configured()?' ('+calculateCoolSetpoint()+'°'+unit+')':''}", type: 'decimal', description: "Enter decimal PMV (${settings.heatPmv?'optional':'required'})",
                		range: pmvRange, required: !settings.heatPmv, submitOnChange: true)
                input(name: "heatPmv", title: "PMV in heat mode${settings.heatPmv!=null&&configured()?' ('+calculateHeatSetpoint()+'°'+unit+')':''}", type: 'decimal', description: "Enter decimal PMV (${settings.coolPmv?'optional':'required'})",
                        range: pmvRange, required: !settings.coolPmv, submitOnChange: true)
                input(name: "met", title: "Metabolic rate", type: 'enum', description: "Tap to choose...",
                        options: metOptions, required: true, submitOnChange: true )
                if (settings.met=='custom') {
                    input(name: "metCustom", title: "Metabolic rate (custom)", type: 'decimal', description: "Enter decimal (required)",
                            range: "0..*", required: settings.met=='custom', submitOnChange: true )
                }
                input(name: "clo", title: "Clothing level", type: 'enum', description: "Tap to choose...",
                        options: cloOptions, required: true, submitOnChange: true )
                if (settings.clo=='custom') {
                    input(name: "cloCustom", title: "Clothing level (custom)", type: 'decimal', description: "Enter decimal (required)",
                            range: "0..*", required: settings.clo=='custom', submitOnChange: true )
                }
			}
			section(title: "Enable only for specific modes or programs? (optional)") {
        		paragraph("Thermostat Modes will only be changed while ${location.name} is in these SmartThings Modes.")
            	input(name: "theModes",type: "mode", title: "Only when the Location Mode is", multiple: true, required: false)
        	}
      		section("Notifications (optional)") {
        		input(name: 'notify', type: 'bool', title: "Notify on Activations?", required: false, defaultValue: false, submitOnChange: true)

            	if (settings.notify) {
       				paragraph "You can enter multiple phone numbers seperated by a semi-colon (;)"
       				input "phone", "string", title: "Send SMS notifications to", description: "Phone Number", required: false, submitOnChange: true
                    if (!settings.phone) {
                        input( name: 'pushNotify', type: 'bool', title: "Send Push notifications to everyone?", defaultValue: false, submitOnChange: true)
                    }
                    if (!settings.phone && !settings.sendPush) paragraph "Notifications configured, but nobody to send them to!"
                }
            	paragraph("A notification is always sent to the Hello Home log whenever Smart Mode changes a thermostat's Mode")
        	}
        }
        section(title: "Temporary Disable") {
        	input(name: "tempDisable", title: "Temporarily Disable this Handler? ", type: "bool", required: false, description: "", submitOnChange: true)
		}
    	section (getVersionLabel()) {}
    }
}

def installed() {
	LOG("installed() entered", 3, "", 'trace')
    atomicState.humidity = null
	initialize()
}

def uninstalled() {
	clearReservations()
}
def clearReservations() {
	thermostats?.each {
    	cancelReservation(getDeviceId(it.deviceNetworkId), 'modeOff' )
    }
}
def updated() {
	LOG("updated() with settings: ${settings}", 3, "", 'trace')
	unsubscribe()
    unschedule()
    atomicState.humidity = null
    initialize()
}

def initialize() {
	LOG("${getVersionLabel()} Initializing...", 2, "", 'info')
	if(settings.tempDisable) {
    	clearReservations()
    	LOG("Temporarily Disabled as per request.", 2, null, "warn")
    	return true
    }

    subscribe( settings.humidistat, 'humidity', humidityChangeHandler)
    subscribe(thermostats, 'currentProgram', programHandler)

    def gu = getTemperatureScale()
    def latest = settings.thermometer.currentState("temperature")
    def unit = latest.unit
    def t, pmv
    if (latest.value.isNumber()) {
        t = roundIt(latest.numberValue, (unit=='C'?2:1))
        latest = settings.humidistat.currentState('humidity')
        if (latest.value.isNumber()) {
            def h = latest.numberValue
            atomicState.humidity = h
            pmv = calculatePmv(t, gu, h)
        }

        runIn(2, atomicHumidityUpdater, [overwrite: true])
    }
    if (pmv) {
    	LOG("Initialization complete...current temperature is ${t}°${gu} (PMV: ${pmv})",2,null,'info')
        return true
    } else {
    	LOG("Initialization error...invalid temperature: ${t}°${gu} (PMV: ${pmv}) - please check settings and retry", 2, null, 'error')
        return false
    }
}

def configured() {
    return atomicState.humidity != null &&
            (settings.met != null && ( settings.met == 'custom' ? settings.metCustom != null : true)) &&
            (settings.clo != null && ( settings.clo == 'custom' ? settings.cloCustom != null : true)) &&
            settings.thermostats != null
}

def programHandler(evt) {
    LOG("Program is: ${evt.value}",3,null,'info')
    runIn(2, atomicHumidityUpdater, [overwrite: true])
}

def humidityChangeHandler(evt) {
	if (evt.value.isNumber()) {
        atomicState.humidity = evt.numberValue
        runIn(2, atomicHumidityUpdater, [overwrite: true])
    }
}

def atomicHumidityUpdater() {
	humidityUpdate( atomicState.humidity )
}

def humidityUpdate( humidity ) {
	if (humidity?.isNumber()) humidityUpdate(humidity as Integer)
}
def humidityUpdate( Integer humidity ) {
    if (!humidity) {
    	LOG("Ignoring invalid humidity: ${humidity}%", 2, null, 'warn')
        return false
    }

    atomicState.humidity = humidity
    LOG("Humidity is: ${humidity}%",3,null,'info')

    def okMode = theModes ? theModes.contains(location.mode) : true
    if (!okMode) {
        LOG("Mode is ignored: ${location.mode}",3,null,'info')
        return
    }

    def heatSetpoint, coolSetpoint
    if (settings.heatPmv) {
        heatSetpoint = calculateHeatSetpoint()
    }
    if (settings.coolPmv) {
        coolSetpoint = calculateCoolSetpoint()
    }
    changeSetpoints(heatSetpoint, coolSetpoint)
}

private def changeSetpoints( heatTemp, coolTemp ) {
	def unit = getTemperatureScale()
	settings.thermostats.each { stat ->
        def program = stat.currentCurrentProgram
    	LOG("Setting ${stat.displayName} '${program}' heatingSetpoint to ${heatTemp}°${unit}, coolingSetpoint to ${coolTemp}°${unit}",2,null,'info')
    	stat.setProgramSetpoints( program, heatTemp, coolTemp )
    }
}

private roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP)
}
private roundIt( BigDecimal value, decimals=0) {
    return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP)
}

private def calculatePmv(temp, units, rh) {
    // returns pmv
    // temp, air temperature
    // units, air temperature unit
    // rh, relative humidity (%) Used only this way to input humidity level
    // met, metabolic rate (met)
    // clo, clothing (clo)

    def vel = 0.0
    def met = settings.met=='custom' ? settings.metCustom : settings.met as BigDecimal
    def clo = settings.clo=='custom' ? settings.cloCustom : settings.clo as BigDecimal

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

private def calculateHeatSetpoint() {
    def units = getTemperatureScale()
    def range = getHeatRange()
    def min = range[0]
    def max = range[1]
    def preferred = max
    def pmv = calculatePmv(preferred, units, atomicState.humidity)
    while (preferred >= min && pmv >= settings.heatPmv) {
        preferred = preferred - 1
        pmv = calculatePmv(preferred, units, atomicState.humidity)
    }
    preferred = preferred + 1
    pmv = calculatePmv(preferred, units, atomicState.humidity)
    LOG("Heating preferred set point: ${preferred}°${units} (PMV: ${pmv})",3,null,'info')
    return preferred
}

private def calculateCoolSetpoint() {
    def units = getTemperatureScale()
    def range = getCoolRange()
    def min = range[0]
    def max = range[1]
    def preferred = min
    def pmv = calculatePmv(preferred, units, atomicState.humidity)
    while (preferred <= max && pmv <= settings.coolPmv) {
        preferred = preferred + 1
        pmv = calculatePmv(preferred, units, atomicState.humidity)
    }
    preferred = preferred - 1
    pmv = calculatePmv(preferred, units, atomicState.humidity)
    LOG("Cooling preferred set point: ${preferred}°${units} (PMV: ${pmv})",3,null,'info')
    return preferred
}

def getHeatRange() {
	def low
    def high
    settings.thermostats.each { stat ->
    	def lo
        def hi
        def setp = stat.currentValue('heatRangeLow')
        lo = lo ? ((setp < lo) ? setp : lo) : setp
        setp = stat.currentValue('heatRangeHigh')
        hi = hi ? ((setp > hi) ? setp : hi) : setp
        // if there are multiple stats, we need to find the range that ALL stats can support
        low = low ? ((lo > low) ? lo : low) : lo
        high = high ? ((hi < high) ? hi : high) : hi
    }
    return [roundIt(low-0.5,0),roundIt(high-0.5,0)]
}

def getCoolRange() {
	def low
    def high
    settings.thermostats.each { stat ->
    	def lo
        def hi
        def setp = stat.currentValue('coolRangeLow')
        lo = lo ? ((setp < lo) ? setp : lo) : setp
        setp = stat.currentValue('coolRangeHigh')
        hi = hi ? ((setp > hi) ? setp : hi) : setp
        // if there are multiple stats, we need to find the range that ALL stats can support
        low = low ? ((lo > low) ? lo : low) : lo
        high = high ? ((hi < high) ? hi : high) : hi
    }
    return [roundIt(low-0.5,0),roundIt(high-0.5,0)]
}

private def getDeviceId(networkId) {
	// def deviceId = networkId.split(/\./).last()
    // LOG("getDeviceId() returning ${deviceId}", 4, null, 'trace')
    // return deviceId
    return networkId.split(/\./).last()
}

// Reservation Management Functions - Now implemented in Ecobee Suite Manager
void makeReservation(tid, String type='modeOff' ) {
	parent.makeReservation( tid, app.id, type )
}
// Cancel my reservation
void cancelReservation(tid, String type='modeOff') {
	log.debug "cancel ${tid}, ${type}"
	parent.cancelReservation( tid, app.id, type )
}
// Do I have a reservation?
Boolean haveReservation(tid, String type='modeOff') {
	return parent.haveReservation( tid, app.id, type )
}
// Do any Apps have reservations?
Boolean anyReservations(tid, String type='modeOff') {
	return parent.anyReservations( tid, type )
}
// How many apps have reservations?
Integer countReservations(tid, String type='modeOff') {
	return parent.countReservations( tid, type )
}
// Get the list of app IDs that have reservations
List getReservations(tid, String type='modeOff') {
	return parent.getReservations( tid, type )
}
// Get the list of app Names that have reservations
List getGuestList(tid, String type='modeOff') {
	return parent.getGuestList( tid, type )
}

// Helper Functions
private def LOG(message, level=3, child=null, logType="debug", event=true, displayEvent=true) {
	def msg = app.label + ': ' + message
	if (logType == null) logType = 'debug'
	parent.LOG(msg, level, null, logType, event, displayEvent)
    log."${logType}" message
}

private def sendMessage(notificationMessage) {
	LOG("Notification Message (notify=${notify}): ${notificationMessage}", 5, null, "trace")

    if (settings.notify) {
        String msg = "${location.name} ${app.label}: " + notificationMessage		// for those that have multiple locations, tell them where we are
        if (phone) { // check that the user did select a phone number
            if ( phone.indexOf(";") > 0){
                def phones = phone.split(";")
                for ( def i = 0; i < phones.size(); i++) {
                    LOG("Sending SMS ${i+1} to ${phones[i]}",2,null,'info')
                    sendSmsMessage(phones[i], msg)									// Only to SMS contact
                }
            } else {
                LOG("Sending SMS to ${phone}",2,null,'info')
                sendSmsMessage(phone, msg)											// Only to SMS contact
            }
        } else if (settings.pushNotify) {
            LOG("Sending Push to everyone",2,null,'warn')
            sendPushMessage(msg)													// Push to everyone
        }
    }
    sendNotificationEvent( ${app.label}+ ': ' + notificationMessage )								// Always send to hello home
}
