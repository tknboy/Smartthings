/**
 *  Tuya Thermostat (v.0.0.1)
 *    Copyright 2021 Tae Kim
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License. You may obtain a copy of the License at:
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *    on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *    for the specific language governing permissions and limitations under the License.
 */

import groovy.json.JsonOutput

metadata {
    definition(name: "Tuya Fan Coil Thermostat", namespace: "tknboy", author: "Tae Kim", ocfDeviceType: "oic.d.thermostat", mnmn: "SmartThingsCommunity", vid: "94d9a579-a027-31d5-b534-9da3833b1ffa") {
        capability "Thermostat"
        capability "Thermostat Cooling Setpoint"
        capability "Thermostat Fan Mode"
        capability "Thermostat Heating Setpoint"
        capability "Thermostat Mode"
        capability "Thermostat Operating State"
        capability "Thermostat Setpoint"
        capability "Temperature Measurement"
        capability "Refresh"
        capability "Actuator"
        capability "Sensor"

        fingerprint profileId: "0104", inClusters: "0000 0004 0005 00EF", outClusters: "0019 000A", manufacturer: "_TZE200_aoclfnxz", model: "TS0601", deviceJoinName: "Tuya Thermostat" // Moes BHT-002-GALZBW, GBLZBW, GCLZBW
        fingerprint profileId: "0104", inClusters: "0000 0004 0005 EF00", outClusters: "0019 000A", manufacturer: "_TZE200_dzuqwsyg", model: "TS0601", deviceJoinName: "Tuya Fan coil Thermostat"
        
    }

    preferences {
        input name: "forceManual", type: "enum", title: "Force Manual Mode : If the thermostat changes to schedule mode, then it automatically reverts to manual mode", options:["0": "Default", "1": "Force Manual Mode"], defaultValue: "0"
    }

    tiles {
        multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4){
/*            tileAttribute ("device.thermostatMode", key: "PRIMARY_CONTROL") {
                attributeState "heat", label:'${name}', action:"off", icon:"st.thermostat.heat", backgroundColor:"#e86d13", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"heat", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"off", icon:"st.thermostat.heat", backgroundColor:"#e86d13", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"heat", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#ffffff", nextState:"turningOn"
            }*/
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
            	attributeState("temperature", label:'${currentValue}°', icon: "st.alarm.temperature.normal",
					backgroundColors: [
						// Celsius
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
						[value: 96, color: "#bc2323"]
					]
				)
			}
			tileAttribute(name:"operatingState", "device.thermostatOperatingState", key: "OPERATING_STATE") {
				attributeState("idle", backgroundColor: "#cccccc")
				attributeState("heating", backgroundColor: "#E86D13")
				attributeState("cooling", backgroundColor: "#00A0DC")
                attributeState("fan only", backgroundColor: "#135141")
			}
            tileAttribute(name:"thermostatMode", "device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", action: "setThermostatMode", label: "Off", icon: "st.thermostat.heating-cooling-off")
				attributeState("cool", action: "setThermostatMode", label: "Cool", icon: "st.thermostat.cool")
				attributeState("heat", action: "setThermostatMode", label: "Heat", icon: "st.thermostat.heat")
				attributeState("auto", action: "setThermostatMode", label: "Auto", icon: "st.tesla.tesla-hvac")
			}
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
            }
        }
        controlTile(name:"heatingTemp","temperatureControl", "device.heatingSetpoint", "slider", sliderType: "HEATING", range:"(5..35)", height: 2, width: 3) {
            state "default", action:"setHeatingSetpoint", backgroundColor: "#E86D13"
        }
        controlTile(name: "coolingTemp","temperatureControl", "device.coolingSetpoint", "slider", sliderType: "COOLING", range:"(5..35)", height: 2, width: 3) {
            state "default", action:"setCoolingSetpoint", backgroundColor: "#E86D13"
        }
        valueTile(name: "temprature", "curTemp_label", "", decoration: "flat", width: 3, height: 1) {
            state "default", label:'Current\nTemp'
        }
        /*valueTile("temperature", "device.temperature", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }*/
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 3, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
        
        main "thermostatMode"
        details(["thermostatMode", "temperature","operatingState", "heatingTemp","coolingTemp", "refresh"])
    }
}

private getCLUSTER_TUYA() { 0xEF00 }
private getSETDATA() { 0x00 }
private getSETTIME() { 0x24 }

// tuya DP type
private getDP_TYPE_BOOL() { "01" }
private getDP_TYPE_VALUE() { "02" }
private getDP_TYPE_ENUM() { "04" }


// Parse incoming device messages to generate events
def parse(String description) {
    if (description?.startsWith('catchall:') || description?.startsWith('read attr -')) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        //log.warn("\n\n\n$descMap\n\n\n")
        if (descMap?.clusterInt==CLUSTER_TUYA && descMap?.command == "24") {
            log.debug "time synchronization request from device"
            def offset = 0
            try {
                offset = location.getTimeZone().getOffset(new Date().getTime())
                log.debug "timezone offset of current location is " + offset
            } catch(e) {
                log.error "cannot resolve current location. please set location in smartthings location setting. setting timezone offset to zero"
            }
            def cmds = zigbee.command(CLUSTER_TUYA, SETTIME, "0008" +zigbee.convertToHexString((int)(now()/1000),8) +  zigbee.convertToHexString((int)((now()+offset)/1000), 8))
            log.debug "sending time data :" + cmds
            cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
            state.old_dp = ""
            state.old_fncmd = ""
        } else if (descMap?.clusterInt==CLUSTER_TUYA && descMap?.command == "0B") {
            log.debug "device has received data from clustercmd "+descMap?.data
            state.old_dp = ""
            state.old_fncmd = ""
        } else if ((descMap?.clusterInt==CLUSTER_TUYA) && (descMap?.command == "01" || descMap?.command == "02")) {
            def dp = zigbee.convertHexToInt(descMap?.data[2])
            def fncmd = zigbee.convertHexToInt(descMap?.data[6..-1].join(''))
            if (dp == state.old_dp && fncmd == state.old_fncmd) {
                //log.debug "(duplicate) dp=${dp}  fncmd=${fncmd}"
                return
            }
            log.debug "dp=${dp} fncmd=${fncmd}"
            state.old_dp = dp
            state.old_fncmd = fncmd

            switch (dp) {
                case 01: // 0x01: Heat / Off
                	// power on/off
                    log.debug "POWER ON/OFF DESCRIPTION:\n${description}\n${descMap}\n\n"
                    def mode = (fncmd == 0) ? "off" : "heat"
                    log.debug "mode: ${mode}, ${fncmd}, fncmd == 0 off, 1 = on"
                    sendEvent(name: "thermostatMode", value: mode, displayed: true)
                    if (mode == state.mode) {
                        state.mode = ""
                    }
                    break
                case 02: // Thermostat mode
                	// Thermostat mode cooling = fncmd 0
                    // heating = fncmd 1
                    // ventilation = fncmd 2
                	log.debug "Fan mode? Dp 0x2, fncmd: ${fncmd}"
                    state.thermostatMode = fncmd
                    //sendEvent(name: "thermostatMode", value: mode, displayed: true)
                    
                break
                case 16: // 0x10: Target Temperature
                    def setpointValue = fncmd
                    log.debug "target temp: ${setpointValue}"
                    sendEvent(name: "heatingSetpoint", value: setpointValue as int, unit: "C", displayed: true)
                    sendEvent(name: "coolingSetpoint", value: setpointValue as int, unit: "C", displayed: false)
                    if (setpointValue == state.setpoint)  {
                        state.setpoint = 0
                    }
                    break
                case 24: // 0x18 : Current Temperature
                    def currentTemperatureValue = fncmd/10
                    log.debug "current temp: ${currentTemperatureValue}"
                    sendEvent(name: "temperature", value: currentTemperatureValue, unit: "C", displayed: true)
                    break
                case 03: // 0x03 : Scheduled/Manual Mode
                    if (fncmd == 0) {
                        log.debug "scheduled mode"
                        if (forceManual == "1") {
                            setManualMode()
                        }
                    } else {
                        log.debug "manual mode"
                    }
                    break
                case 0x16: // unknown fncmd: setting temp
                	//first detected when changing mode to heating
                	log.debug "dp 0x16, fncmd: ${fncmd}"
                    
                break
                
                case 28: // fan speed
                	log.debug "dp 0x18, fncmd: ${fncmd}"
                    //fncmd 3 == auto,2 == high, 1 == mid, 0 == low
                break
                case 36: // 0x24 : operating state
                	log.debug "operating state dp:24, fncmd: ${fncmd}"
                    sendEvent(name: "thermostatOperatingState", value: (fncmd ? "idle" : "heating"), displayed: true)
                    break
            }
        } else {
            //log.debug "not parsed : "+descMap
        }
    }
}

def setThermostatMode(mode){
    log.debug "powerOnOff(${mode})"
    state.mode = mode
    //runIn(4, modeReceiveCheck, [overwrite:true])
    sendTuyaCommand("01", DP_TYPE_BOOL, (mode!="off")?"01":"00")
    
    if (mode != "off") {
    	def modeNum = "01"
        if (mode == "cool") {
        	modeNum = "00"
        }
    	setOperatingMode(modeNum)
    }
    //log.debug "st cmd 0x${device.deviceNetworkId} 1 ${CLUSTER_TUYA} 1 59450101000101"
    //"st cmd 0x${device.deviceNetworkId} 1 ${CLUSTER_TUYA} 1 59520101000100"
}

def setOperatingMode(mode) {
    log.debug "setOperatingMode(${mode})"
	state.operatingMode = mode
	sendTuyaCommand("02", DP_TYPE_ENUM, mode)
}

def setHeatingSetpoint(temperature){
    log.debug "setHeatingSetpoint(${temperature})"
    def settemp = temperature as int 
    settemp += (settemp != temperature && temperature > device.currentValue("heatingSetpoint")) ? 1 : 0
    log.debug "change setpoint to ${settemp}"
    state.setpoint = settemp
    runIn(4, setpointReceiveCheck, [overwrite:true])
    sendTuyaCommand("10", DP_TYPE_VALUE, zigbee.convertToHexString(settemp as int, 8))
}

def setCoolingSetpoint(temperature){
    setHeatingSetpoint(temperature)
}

def heat(){
    setThermostatMode("heat")
}

def off(){
    setThermostatMode("off")
}

def on() {
    heat()
}

def setManualMode() {
    log.debug "setManualMode()"
    def cmds = sendTuyaCommand("02", DP_TYPE_ENUM, "00") + sendTuyaCommand("03", DP_TYPE_ENUM, "01")
    cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }     
}

def installed() {
    log.info "installed()"
    sendEvent(name: "supportedThermostatModes", value: JsonOutput.toJson(["heat","cool", "fan only", "off"]), displayed: false)
    sendEvent(name: "thermostatMode", value: "off", displayed: false)
    sendEvent(name: "thermostatOperatingState", value: "idle", displayed: false)
    sendEvent(name: "heatingSetpoint", value: 0, unit: "C", displayed: false)
    sendEvent(name: "coolingSetpoint", value: 0, unit: "C", displayed: false)
    sendEvent(name: "temperature", value: 0, unit: "C", displayed: false)
    state.mode = ""
    state.setpoint = 0
    unschedule()
    runEvery1Minute(receiveCheck)
}

def updated() {
    log.info "updated()"
}

def modeReceiveCheck() {
    log.debug "modeReceiveCheck()"
    if (state.mode != "") {
        log.debug " resending mode command :"+state.mode
        def cmds = setThermostatMode(state.mode)
        cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
    }
}

def setpointReceiveCheck() {
    log.debug "setpointReceiveCheck() ${state}"
    if (state.setpoint != 0 ) {
        log.debug " resending setpoint command :"+state.setpoint
        def cmds = setHeatingSetpoint(state.setpoint)
        cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
    }
}
def refresh() {
    log.debug "refresh()"
    state.mode=2
    //sendEvent(name: "supportedThermostatModes", value: JsonOutput.toJson(["heat","cool", "fan only", "off"]), displayed: false)
}
def receiveCheck() {
    modeReceiveCheck()
    setpointReceiveCheck()
}

private sendTuyaCommand(dp, dp_type, fncmd) {
    log.debug zigbee.command(CLUSTER_TUYA, SETDATA, PACKET_ID + dp + dp_type + zigbee.convertToHexString(fncmd.length()/2, 4) + fncmd )
    zigbee.command(CLUSTER_TUYA, SETDATA, PACKET_ID + dp + dp_type + zigbee.convertToHexString(fncmd.length()/2, 4) + fncmd )
}

private getPACKET_ID() {
    state.packetID = ((state.packetID ?: 0) + 1 ) % 65536
    return zigbee.convertToHexString(state.packetID, 4)
}
