/**
 *  AlertOnSmoke
 *
 *  Copyright 2017 Michael Tynion
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
definition(
    name: "AlertOnSmoke",
    namespace: "miketynion",
    author: "Michael Tynion",
    description: "Smoke/CO2 Detector",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select smoke detector(s)..."){
		input "smoke_detectors", "capability.smokeDetector", title: "Which one(s)...?", multiple: true
	}
    
    section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
		input "phoneNumber", "phone", title: "Enter number for SMS (optional).", required: false
	}
    
    section( "Low battery warning" ){
    	input "lowBattThreshold", "number", title: "Low Batt Threshold % (default 10%)", required: false
    }

}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
	initialize()
}

def smokeHandler(evt) {
	log.trace "$evt.value: $evt, $settings"
    
    String theMessage
    
    if (evt.value == "tested") {
    	theMessage = "${evt.displayName} was tested for smoke."
    } else if (evt.value == "clear") {
    	theMessage = "${evt.displayName} is clear for smoke."
    } else if (evt.value == "detected") {
    	theMessage = "${evt.displayName} detected smoke!"
    } else {
    	theMessage = ("Unknown event received from ${evt.name}")
    }
    
    sendMsg(theMessage)
}


def carbonMonoxideHandler(evt) {
	log.trace "$evt.value: $evt, $settings"
    
    String theMessage
    
    if (evt.value == "tested") {
    	theMessage = "${evt.displayName} was tested for carbon monoxide."
    } else if (evt.value == "clear") {
    	theMessage = "${evt.displayName} is clear of carbon monoxide."
    } else if (evt.value == "detected") {
    	theMessage = "${evt.displayName} detected carbon monoxide!"
    } else {
    	theMessage = "Unknown event received from ${evt.name}"
    }
    
    sendMsg(theMessage)
}

def batteryHandler(evt) {
	log.trace "$evt.value: $evt, $settings"
    String theMessage
    int battLevel = evt.integerValue
    
    log.debug "${evt.displayName} has battery of ${battLevel}"
    
    if (battLevel < lowBattThreshold ?: 10) {
    	theMessage = "${evt.displayName} has battery of ${battLevel}"
        sendMsg(theMessage)
    }
}

private sendMsg(theMessage) {
    log.debug "Sending message: ${theMessage}"
    if (phoneNumber) {
    	sendSms(phoneNumber, theMessage)
    }

	if (sendPushMessage == "Yes") {
    	sendPush(theMessage)
    }
}
    
    

private initialize() {
	subscribe(smoke_detectors, "smoke", smokeHandler)
    subscribe(smoke_detectors, "carbonMonoxide", carbonMonoxideHandler)
    subscribe(smoke_detectors, "battery", batteryHandler)
}
