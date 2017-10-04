/**
 *  Copyright 2015 SmartThings
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
 */
metadata {
	definition (name: "Lightify Surface", namespace: "smartthings", author: "SmartThings") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Configuration"
		capability "Sensor"
		capability "Refresh"

		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0B04,FC0F", outClusters: "0019"
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
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
			state "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
			state "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
			state "level", label:'${currentValue} %', unit:"%", backgroundColor:"#ffffff"
		}
		main "switch"
		details(["switch", "refresh", "level", "levelSliderControl"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.info description
	def msg = zigbee.parse(description)
if (description?.startsWith("catchall:")) {
	log.trace msg
	if(description?.endsWith("0100") ||description?.endsWith("1001"))
{
	def result = createEvent(name: "switch", value: "on")
log.debug "Parse returned ${result?.descriptionText}"
return result
}
    
if(description?.endsWith("0000") || description?.endsWith("1000"))
{
	def result = createEvent(name: "switch", value: "off")
    log.debug "Parse returned ${result?.descriptionText}"
    return result
}
	}
	else {
		def name = description?.startsWith("on/off: ") ? "switch" : null
		def value = name == "switch" ? (description?.endsWith(" 1") ? "on" : "off") : null
		def result = createEvent(name: name, value: value)
		log.debug "Parse returned ${result?.descriptionText}"
		return result
	}

   if (description?.startsWith("read attr")) {
   	
Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
		}
		
log.debug "Desc Map: $descMap"
 
switch (descMap.cluster) {

	case "0008":
    
		log.debug description[-2..-1]
		def i = Math.round(convertHexToInt(descMap.value) / 256 * 100 )
		sendEvent( name: "level", value: i )
        sendEvent( name: "switch.setLevel", value: i) //added to help subscribers
        break
        
}            
        
}

}

// Commands to device
def on() {
	log.debug "on()"
	sendEvent(name: "switch", value: "on")
	"st cmd 0x${device.deviceNetworkId} 3 6 1 {}"
}

def off() {
	log.debug "off()"
	sendEvent(name: "switch", value: "off")
	"st cmd 0x${device.deviceNetworkId} 3 6 0 {}"
}
def setLevel(value) {
	log.trace "setLevel($value)"
	def cmds = []

	if (value == 0) {
		sendEvent(name: "switch", value: "off")
		cmds << "st cmd 0x${device.deviceNetworkId} 3 6 0 {}"
	}
	else if (device.latestValue("switch") == "off") {
sendEvent(name: "switch", value: "on")
cmds << "st cmd 0x${device.deviceNetworkId} 3 6 1 {}"

	}

	sendEvent(name: "level", value: value)
def level = hexString(Math.round(value * 255/100))
cmds << "st cmd 0x${device.deviceNetworkId} 3 8 4 {${level} 1500}"

	//log.debug cmds
	cmds
}

def refresh() {
	[
		"st wattr 0x${device.deviceNetworkId} 3 6 0", "delay 200",
		"st wattr 0x${device.deviceNetworkId} 3 8 0"
	]
}

def configure() {

	log.debug "binding to switch and level control cluster"
	[
		
//Switch Reporting
"zcl global send-me-a-report 6 0 0x10 0 3600 {01}", "delay 500",
"send 0x${device.deviceNetworkId} 3 1", "delay 1000",

//Level Control Reporting
"zcl global send-me-a-report 8 0 0x20 5 3600 {0010}", "delay 200",
"send 0x${device.deviceNetworkId} 3 1", "delay 1500",

"zdo bind 0x${device.deviceNetworkId} 3 1 6 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 3 1 8 {${device.zigbeeId}} {}"
	]


	//set transition time to 2 seconds. Not currently working.
	// "st wattr 0x${device.deviceNetworkId} 1 8 0x10 0x21 {1400}"
}



private hex(value, width=2) {
	def s = new BigInteger(Math.round(value).toString()).toString(16)
	while (s.size() < width) {
		s = "0" + s
	}
	s
}

private getEndpointId() {
	new BigInteger(device.endpointId, 16).toString()
}