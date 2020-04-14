
metadata {
    definition (name: "Keen Home Smart Vent", namespace: "sandood", author: "Keen Home & Barry Burke") {
        capability "Switch Level"
        capability "Switch"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Battery"
		capability "Contact Sensor"
		capability "Actuator"

		command "open"
		command "close"
		command "closeContact"
		command "openContact"

        command "getLevel"
        command "getOnOff"
        command "getPressure"
        command "getBattery"
        command "getTemperature"
        command "setZigBeeIdTile"
        command "clearObstruction"
		
		attribute "zigbeeId", "string"
		attribute "pressure", "number"
		attribute "pascals", "number"
		attribute "serial", "string"

        fingerprint endpoint: "1",
        profileId: "0104",
        inClusters: "0000,0001,0003,0004,0005,0006,0008,0020,0402,0403,0B05,FC01,FC02",
        outClusters: "0019"
    }

    // simulator metadata
    simulator {
        // status messages
        status "on": "on/off: 1"
        status "off": "on/off: 0"

        // reply messages
        reply "zcl on-off on": "on/off: 1"
        reply "zcl on-off off": "on/off: 0"
    }

    // UI tile definitions
    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: false) {
            state "on", action: "switch.off",icon: "st.vents.vent-open-text", backgroundColor: "#53a7c0"
            state "off", action: "switch.on",icon: "st.vents.vent-closed", backgroundColor: "#ffffff"
            state "obstructed", action: "clearObstruction", icon: "st.vents.vent-closed", backgroundColor: "#ff0000"
            state "clearing", action: "", icon: "st.vents.vent-closed", backgroundColor: "#ffff33"
        }
        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false) {
            state "level", action:"switch level.setLevel"
        }
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        valueTile("temperature", "device.temperature", inactiveLabel: false) {
            state "temperature", label:'${currentValue}°',
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        }
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
            state "battery", label: 'Battery ${currentValue}%', backgroundColor:"#ffffff"
        }
        valueTile("zigbeeId", "device.zigbeeId", inactiveLabel: true, decoration: "flat") {
            state "serial", label:'${currentValue}', backgroundColor:"#ffffff"
        }
        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
        }
        main "switch"
        details(["switch","refresh","temperature","levelSliderControl","battery",  "configure"])
    }
	preferences {
		input "referenceTemp", "decimal", title: "<b>Reference temperature</b>", description: "Enter current reference temperature reading", displayDuringSetup: false
		input "skipTemp", "bool", title: "<b>Send every 10th temperature event</b>", defaultValue: false
		input "debugOn", "bool", title: "<b>Enable debug logging for 1/2 hour</b>", defaultValue: true
		input "infoOn", "bool", title: "<b>Enable descriptionText logging?</b>", defaultValue: true
    }
}

def installed(){
    initialize()
}

//when device preferences are changed
def updated(){
	unschedule()
	initialize()
}

def initialize() {
	def linkText = getLinkText(device)
	if (infoOn) log.info "${linkText} descriptionText logging enabled"
	if (debugOn) log.info "${linkText} debug logging enabled for 1/2 hour"
	log.info "${linkText} updated, settings: ${settings}"
	state.tempCounter = null

	configure()
	
	// handle reference temperature / tempOffset automation
	if (settings.referenceTemp != null) {
		if (state.sensorTemp) {
			state.sensorTemp = roundIt(state.sensorTemp, 2)
			state.tempOffset = roundIt(referenceTemp - state.sensorTemp, 2)
			settings.referenceTemp = null
			device.clearSetting('referenceTemp')
			if (debugOn) log.debug "sensorTemp: ${state.sensorTemp}, referenceTemp: ${referenceTemp}, offset: ${state.tempOffset}"
			sendEvent(makeTemperatureResult(state.sensorTemp))
		} // else, preserve settings.referenceTemp, state.tempOffset will be calculate on the next temperature report
	} else if (state.tempOffset == null) {
		// Initialize the offset, converting from the old attribute-based approach if necessary
		def offset = device.currentValue('tempOffset')
		if (offset != null) {
			log.info "${linkText} One-time tempOffset conversion completed"
			sendEvent(name: 'tempOffset', value: null, descriptionText: "One-time tempOffset conversion completed")
			device.clearSetting('tempOffset')
			state.tempOffset = roundIt(offset, 2)
			if (state.sensorTemp) sendEvent(makeTemperatureResult(state.sensorTemp))
		} else {
			state.tempOffset = 0.0
		}
	}
	
	if (debugOn) {
		// turn off debug logging after 1/2 hour
		runIn(1800, debugOff, [overwrite: true])
	}
	if (settings.skipTemp) state.tempCounter = 10	// send next temperature update
}


/**** PARSE METHODS ****/
def parse(String description) {
	if (debugOn) log.debug "parse(${description})"

    Map map = [:]
    if (description?.startsWith('catchall:')) {
        map = parseCatchAllMessage(description)
    }
    else if (description?.startsWith('read attr -')) {
        map = parseReportAttributeMessage(description)
    }
    else if (description?.startsWith('temperature: ') || description?.startsWith('humidity: ')) {
        map = parseCustomMessage(description)
    }
    else if (description?.startsWith('on/off: ')) {
        map = parseOnOffMessage(description)
    }

    if (debugOn) log.debug "Parse returned $map"
    return map ? createEvent(map) : [:]
}

private Map parseCatchAllMessage(String description) {
	if (debugOn) log.debug "parseCatchAllMessage(${description})"

    def cluster = zigbee.parse(description)
    if (debugOn) log.debug "cluster: ${cluster}"
    if (shouldProcessMessage(cluster)) {
        if (debugOn) log.debug "processing message"
        switch(cluster.clusterId) {
            case 0x0001:
                return makeBatteryResult(cluster.data.last())
                break

            case 0x0402:
                // temp is last 2 data values. reverse to swap endian
                String temp = cluster.data[-2..-1].reverse().collect { cluster.hex1(it) }.join()
                def value = convertTemperatureHex(temp)
                if (value > 50.0) return makeTemperatureResult(value)
                break

            case 0x0006:
                return makeOnOffResult(cluster.data[-1])
                break
        }
    }

    return [:]
}

private boolean shouldProcessMessage(cluster) {
    // 0x0B is default response indicating message got through
    // 0x07 is bind message
    if (cluster.profileId != 0x0104 ||
        cluster.command == 0x0B ||
        cluster.command == 0x07 ||
        (cluster.data.size() > 0 && cluster.data.first() == 0x3e)) {
        return false
    }

    return true
}

private Map parseReportAttributeMessage(String description) {
	if (debugOn) log.debug "parseReportAttributeMessage(${description})"

    Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
        def nameAndValue = param.split(":")
        map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
    if (debugOn) log.debug "Desc Map: $descMap"

    if (descMap.cluster == "0006" && descMap.attrId == "0000") {
        return makeOnOffResult(Integer.parseInt(descMap.value));
    }
    else if (descMap.cluster == "0008" && descMap.attrId == "0000") {
        return makeLevelResult(descMap.value)
    }
    else if (descMap.cluster == "0402" && descMap.attrId == "0000") {
        def value = convertTemperatureHex(descMap.value)
        if (value > 50.0)return makeTemperatureResult(value)
    }
    else if (descMap.cluster == "0001" && descMap.attrId == "0021") {
        return makeBatteryResult(Integer.parseInt(descMap.value, 16))
    }
    else if (descMap.cluster == "0403" && descMap.attrId == "0020") {
        return makePressureResult(Integer.parseInt(descMap.value, 16))
    }
    else if (descMap.cluster == "0000" && descMap.attrId == "0006") {
        return makeSerialResult(new String(descMap.value.decodeHex()))
    }

    // shouldn't get here
    return [:]
}

private Map parseCustomMessage(String description) {
	if (debugOn) log.debug "parseCustomMessage(${description})"
    Map resultMap = [:]
    if (description?.startsWith('temperature: ')) {
        // if (debugOn) log.debug "${description}"
        // def value = zigbee.parseHATemperatureValue(description, "temperature: ", getTemperatureScale())
        // if (debugOn) log.debug "split: " + description.split(": ")
        def value = Double.parseDouble(description.split(": ")[1])
        // if (debugOn) log.debug "${value}"
        if (value > 50.0) resultMap = makeTemperatureResult(convertTemperature(value))
    }
    return resultMap
}

private Map parseOnOffMessage(String description) {
	if (debugOn) log.debug "parseOnOffMessage(${description})"
    Map resultMap = [:]
    if (description?.startsWith('on/off: ')) {
        def value = Integer.parseInt(description - "on/off: ")
        resultMap = makeOnOffResult(value)
    }
    return resultMap
}

private Map makeOnOffResult(rawValue) {
    if (debugOn) log.debug "makeOnOffResult(${rawValue})"
    def linkText = getLinkText(device)
    def value = rawValue == 1 ? "on" : "off"
    Map result = [
        name: "switch",
        value: value,
        descriptionText: "${linkText} is ${value}"
    ]
	if (infoOn) log.info result.descriptionText
	return result
}

private Map makeLevelResult(rawValue) {
    def linkText = getLinkText(device)
    def value = Integer.parseInt(rawValue, 16)
    def rangeMax = 254

    // catch obstruction level
    if (value == 255) {
        log.warn "${linkText} is obstructed. Please power cycle."
        // Just return here. Once the vent is power cycled
        // it will go back to the previous level before obstruction.
        // Therefore, no need to update level on the display.
        return [
            name: "switch",
            value: "obstructed",
            descriptionText: "${linkText} is obstructed. Please power cycle."
        ]
    }

    value = roundIt(Math.floor(value / rangeMax * 100), 0)

    Map result = [
        name: "level",
        value: value,
        descriptionText: "${linkText} level is ${value}%",
		unit: '%'
    ]
	if (infoOn) log.info result.descriptionText
	return result
}

private Map makePressureResult(rawValue) {
	if (debugOn) log.debug "makePressureResult(${rawValue})"
    def linkText = getLinkText(device)

    def pascals = rawValue / 10
	def inHg = roundIt( (pascals / 3386.3886666667), 2)
    Map result = [
        name: 'pascals',
		descriptionText: "${linkText} pascals is ${pascals} Pa",
        value: pascals,
		unit:  'Pa'
    ]
	sendEvent(result)
	result = [
		name: 	'pressure',
		descriptionText: "${linkText} pressure is ${inHg} inHg",
		value: 	inHg,
		unit:	'inHg'
	]
	if (infoOn) log.info result.descriptionText
    return result
}

private Map makeBatteryResult(rawValue) {
    if (debugOn) log.debug 'makeBatteryResult'
    def linkText = getLinkText(device)

    // if (debugOn) log.debug
    Map result = [
        name: 	'battery',
        value: 	rawValue,
		unit:	'%',
        descriptionText: "${linkText} battery is at ${rawValue}%"
    ]
	if (infoOn) log.info result.descriptionText
	return result
}

private Map makeTemperatureResult(value) {
	if (debugOn) log.debug "makeTemperatureResult(${value})"
	
	if (settings.skipTemp && state.tempCounter) {
		state.tempCounter = state.tempCounter + 1
		if (state.tempCounter < 8) { if (debugOn) log.debug "Temp Skipped"; return [:]; }	// only send every 10th temperature update
		state.tempCounter = 1
	}
    def linkText = getLinkText(device)

	if ((state.sensorTemp == null) || (state.sensorTemp != value)) state.sensorTemp = value
	
	if (settings.referenceTemp != null) {
		state.tempOffset = roundIt((referenceTemp - value), 2)
		settings.referenceTemp = null
		device.clearSetting('referenceTemp')
		if (debugOn) log.debug "sensorTemp: ${value}, referenceTemp: ${referenceTemp}, offset: ${state.tempOffset}"
	}
	
	def offset = state.tempOffset
	if (offset == null) {
		def temp = device.currentValue('tempOffset')	// convert the old attribute to the new state variable
		offset = (temp != null) ? temp : 0.0
		state.tempOffset = offset
	}
    if (offset != 0.0) {
    	def v = value
    	value = roundIt((v + offset), 2)
    }

//	def nowDate = now()
//    def lastDate = state.lastTempDate ?: 0
    def currentValue = device.currentValue('temperature')
	if ((currentValue != value)) { // || ((nowDate - lastDate) > 10000)) {
		// eliminate duplicate updates within 10 seconds of each other
//        state.lastTempDate = nowDate
    	Map result = [
        	name: 'temperature',
        	value: value,
        	unit: temperatureScale,
        	descriptionText: "${linkText} temperature is ${value}°${temperatureScale}"
    	]
		if (infoOn) log.info result.descriptionText
		return result
	} else return [:]
}

/**** HELPER METHODS ****/
private def convertTemperatureHex(value) {
    if (debugOn) log.debug "convertTemperatureHex(${value})"
	boolean hack = false
    def celsius = Integer.parseInt(value, 16).shortValue() / 1000
	if (celsius < 0.0) { celsius = celsius * -1; hack = true; }	// hack to work around flaky temp reports
	if (debugOn) log.debug "celsius: ${celsius} (${hack})"
    return convertTemperature(celsius)
}

private def convertTemperature(celsius) {
	if (debugOn) log.debug "convertTemperature(${celsius})"

    if(getTemperatureScale() == "C"){
        return roundIt( celsius, 2 )
    } else {
        def fahrenheit = roundIt( celsiusToFahrenheit(celsius), 2 )
        // if (debugOn) log.debug "converted to F: ${fahrenheit}"
        return fahrenheit
    }
}

private def makeSerialResult(serial) {
    if (debugOn) log.debug "makeSerialResult: " + serial

    def linkText = getLinkText(device)
    //sendEvent([
    //    name: "serial",
    //    value: serial,
    //    descriptionText: "${linkText} has serial ${serial}" ])
    result = [
        name: "serial",
        value: serial as String,
        descriptionText: "${linkText} has serial ${serial}" ]
	sendEvent( result )
	if (infoOn) log.info result.descriptionText
	return result
}

// takes a level from 0 to 100 and translates it to a ZigBee move to level with on/off command
private def makeLevelCommand(level) {
    def rangeMax = 254
    def scaledLevel = roundIt((level * rangeMax / 100), 0)
    if (debugOn) log.debug "scaled level for ${level}%: ${scaledLevel}"

    // convert to hex string and pad to two digits
    def hexLevel = new BigInteger(scaledLevel.toString()).toString(16).padLeft(2, '0')

    ["st cmd 0x${device.deviceNetworkId} 1 8 4 {${hexLevel} 0000}"]
}

/**** COMMAND METHODS ****/
def on() {
    def linkText = getLinkText(device)
    if (debugOn) log.debug "open ${linkText}"

    // only change the state if the vent is not obstructed
    if (device.currentValue("switch") == "obstructed") {
        log.error("cannot open because ${linkText} is obstructed")
        return
    }

    sendEvent(makeOnOffResult(1))
	openContact()
	if (device.currentValue('level', true) == 0) { 
		sendEvent(name: 'level', value: '100',descriptionText: "${linkText} level is 100%", unit: '%')
		return makeLevelCommand(100) + ["st cmd 0x${device.deviceNetworkId} 1 6 1 {}"]
	} else {
		return ["st cmd 0x${device.deviceNetworkId} 1 6 1 {}"]
	}
}

def open() { on() }

def off() {
    def linkText = getLinkText(device)
    if (debugOn) log.debug "close ${linkText}"

    // only change the state if the vent is not obstructed
    if (device.currentValue("switch") == "obstructed") {
        log.error("cannot close because ${linkText} is obstructed")
        return
    }

    sendEvent(makeOnOffResult(0))
	sendEvent(name: "level", value: "0", displayed: false)
	closeContact()
	//setLevel(0)
    ["st cmd 0x${device.deviceNetworkId} 1 6 0 {}"]
	
}

def close() { off() }

def closeContact() {
	sendEvent(name: "contact", value: "closed", displayed: false)
}

def openContact() {
	sendEvent(name: "contact", value: "open", displayed: false)	
}

def clearObstruction() {
    def linkText = getLinkText(device)
    if (debugOn) log.debug "attempting to clear ${linkText} obstruction"

	def result = [
        name: "switch",
        value: "clearing",
        descriptionText: "${linkText} is clearing obstruction"
    ]
	if (infoOn) log.info result.descriptionText
	sendEvent(result)

    // send a move command to ensure level attribute gets reset for old, buggy firmware
    // then send a reset to factory defaults
    // finally re-configure to ensure reports and binding is still properly set after the rtfd
    [
        makeLevelCommand(device.currentValue("level")), "delay 500",
        "st cmd 0x${device.deviceNetworkId} 1 0 0 {}", "delay 5000"
    ] + configure()
}

def openFully() {
	openContact()
	setLevel(100)
}

def setLevel(value) {
    if (debugOn) log.debug "setting level: ${value}"
    def linkText = getLinkText(device)

    // only change the level if the vent is not obstructed
    def currentState = device.currentValue("switch")

    if (currentState == "obstructed") {
        log.error("cannot set level because ${linkText} is obstructed")
        return
    }

	sendEvent(name: "level", value: value, descriptionText: "${linkText} level is ${value}%", unit: '%')
	
    if (value > 0) {
        sendEvent(name: "switch", value: "on", descriptionText: "${linkText} is on by setting a level")
		if (infoOn) log.info "${linkText} is on by setting a level"
    }
    else {
        sendEvent(name: "switch", value: "off", descriptionText: "${linkText} is off by setting level to 0")
		if (infoOn) log.info "${linkText} is off by setting level to 0"
		closeContact()
    }

    makeLevelCommand(value)
}
def setLevel(value,duration) {
	setLevel(value)	// Duration not supported
}

def getOnOff() {
    if (debugOn) log.debug "getOnOff()"

    // disallow on/off updates while vent is obstructed
    if (device.currentValue("switch") == "obstructed") {
        log.error("cannot update open/close status because ${getLinkText(device)} is obstructed")
        return []
    }

    ["st rattr 0x${device.deviceNetworkId} 1 0x0006 0"]
}

def getPressure() {
    if (debugOn) log.debug "getPressure()"

    // using a Keen Home specific attribute in the pressure measurement cluster
    [
        "zcl mfg-code 0x115B", "delay 200",
        "zcl global read 0x0403 0x20", "delay 200",
        "send 0x${device.deviceNetworkId} 1 1", "delay 200"
    ]
}

def getLevel() {
    if (debugOn) log.debug "getLevel()"

    // disallow level updates while vent is obstructed
    if (device.currentValue("switch") == "obstructed") {
        log.error("cannot update level status because ${getLinkText(device)} is obstructed")
        return []
    }

    ["st rattr 0x${device.deviceNetworkId} 1 0x0008 0x0000"]
}

def getTemperature() {
    if (debugOn) log.debug "getTemperature()"

    ["st rattr 0x${device.deviceNetworkId} 1 0x0402 0"]
}

def getBattery() {
    if (debugOn) log.debug "getBattery()"

    ["st rattr 0x${device.deviceNetworkId} 1 0x0001 0x0021"]
}

def setZigBeeIdTile() {
    if (debugOn) log.debug "setZigBeeIdTile() - ${device.zigbeeId}"

    def linkText = getLinkText(device)

    sendEvent([
        name: "zigbeeId",
        value: device.zigbeeId,
        descriptionText: "${linkText} has zigbeeId ${device.zigbeeId}" ])
	return [
        name: "zigbeeId",
        value: device.zigbeeId,
        descriptionText: "${linkText} has zigbeeId ${device.zigbeeId}" ]
}

def refresh() {
	getOnOff() +
    getLevel() +
    getTemperature() +
    getPressure() +
    getBattery()
}

def roundIt( value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
def roundIt( BigDecimal value, decimals=0 ) {
	return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}

def debugOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("debugOn",[value:"false",type:"bool"])
}
def logsOff() { debugOff() }

private logDebug(msg) {
	if (settings?.debugOn || settings?.debugOn == null) log.debug "$msg"
}

private logInfo(msg) {
	if (settings?.infoOn || settings?.infoOn == null) {
		if (state.msg != msg) { 
			// Don't print redundant messages
			log.info "$msg"
			state.msg = msg
		}
	}
}

def configure() {
    if (debugOn) log.debug "CONFIGURE"

    // get ZigBee ID by hidden tile because that's the only way we can do it
    setZigBeeIdTile()

    def configCmds = [
        // bind reporting clusters to hub
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0006 {${device.zigbeeId}} {}", "delay 500",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0008 {${device.zigbeeId}} {}", "delay 500",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0402 {${device.zigbeeId}} {}", "delay 500",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0403 {${device.zigbeeId}} {}", "delay 500",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0001 {${device.zigbeeId}} {}", "delay 1500",

//    def configCmds = [
        // configure report commands
        // [cluster] [attr] [type] [min-interval] [max-interval] [min-change]

        // mike 2015/06/22: preconfigured; see tech spec
        // vent on/off state - type: boolean, change: 1
        // "zcl global send-me-a-report 6 0 0x10 5 60 {01}", "delay 200",
        // "send 0x${device.deviceNetworkId} 1 1", "delay 1500",

        // mike 2015/06/22: preconfigured; see tech spec
        // vent level - type: int8u, change: 1
        // "zcl global send-me-a-report 8 0 0x20 5 60 {01}", "delay 200",
        // "send 0x${device.deviceNetworkId} 1 1", "delay 1500",

        // temperature - type: int16s, change: 0xA = 10 = 0.1C, 0x32=50=0.5C
        //"zcl global send-me-a-report 0x0402 0 0x29 300 600 {1900}", "delay 200",
        //"send 0x${device.deviceNetworkId} 1 1", "delay 1500",
		"zcl global send-me-a-report 0x0402 0 0x29 60 60 {0A00}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 1", "delay 1500",
		
        // Send update when pressure changes by 50 Pa instead of 0.1Pa, every 1-5 minutes
        // pressure - type: int32u, change: 1 = 0.1Pa, 500=50 Pa
        "zcl mfg-code 0x115B", "delay 200",
        "zcl global send-me-a-report 0x0403 0x20 0x22 60 60 {010000}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 1", "delay 1500",

        // mike 2015/06/2: preconfigured; see tech spec
        // battery - type: int8u, change: 1
        // "zcl global send-me-a-report 1 0x21 0x20 60 3600 {01}", "delay 200",
        // "send 0x${device.deviceNetworkId} 1 1", "delay 1500",

        // binding commands
//        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0006 {${device.zigbeeId}} {}", "delay 500",
//        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0008 {${device.zigbeeId}} {}", "delay 500",
//        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0402 {${device.zigbeeId}} {}", "delay 500",
//        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0403 {${device.zigbeeId}} {}", "delay 500",
//        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0001 {${device.zigbeeId}} {}", "delay 500"
    ]

    return configCmds + refresh()
}
