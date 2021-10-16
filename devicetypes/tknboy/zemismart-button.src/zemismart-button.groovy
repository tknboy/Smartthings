
/**
*  Zemismart Button V0.8 edited by Tae Kim
*
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

import groovy.json.JsonOutput
import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType

metadata 
{
    definition (name: "Zemismart Button", namespace: "tknboy", author: "Tae Kim", ocfDeviceType: "x.com.st.d.remotecontroller", mcdSync: true)
    {
        capability "Actuator"
        capability "Battery"
        capability "Button"
        capability "Holdable Button"
        capability "Refresh"
        capability "Sensor"
        capability "Health Check"
        capability "Configuration"

        fingerprint inClusters: "0000, 0001, 0006", outClusters: "0019, 000A", manufacturer: "_TZ3400_keyjqthh", model: "TS0041", deviceJoinName: "Zemismart Button", mnmn: "SmartThings", vid: "generic-2-button"
        fingerprint inClusters: "0000, 0001, 0006", outClusters: "0019", manufacturer: "_TYZB02_keyjhapk", model: "TS0042", deviceJoinName: "Zemismart Button", mnmn: "SmartThings", vid: "generic-2-button"
        fingerprint inClusters: "0000, 0001, 0006", outClusters: "0019", manufacturer: "_TZ3400_keyjhapk", model: "TS0042", deviceJoinName: "Zemismart Button", mnmn: "SmartThings", vid: "generic-2-button"
        fingerprint inClusters: "0000, 0001, 0006", outClusters: "0019, 000A", manufacturer: "_TZ3400_key8kk7r", model: "TS0043", deviceJoinName: "Zemismart Button", mnmn: "SmartThings", vid: "generic-4-button"
        fingerprint inClusters: "0000, 0001, 0006", outClusters: "0019", manufacturer: "_TYZB02_key8kk7r", model: "TS0043", deviceJoinName: "Zemismart Button", mnmn: "SmartThings", vid: "generic-4-button"
        fingerprint inClusters: "0000, 000A, 0001 0006", outClusters: "0019", manufacturer: "_TZ3000_bi6lpsew", model: "TS0043", deviceJoinName: "Zemismart Button", mnmn: "SmartThings", vid: "generic-4-button"
        fingerprint inClusters: "0000, 000A, 0001 0006", outClusters: "0019", manufacturer: "_TZ3000_vp6clf9d", model: "TS0044", deviceJoinName: "Zemismart Button", mnmn: "SmartThings", vid: "generic-4-button"
        fingerprint inClusters: "0000, 0001, 0003, 0004, 0006, 1000", outClusters: "0019, 000A, 0003, 0004, 0005, 0006, 0008, 1000", manufacturer: "_TZ3000_xabckq1v", model: "TS004F", deviceJoinName: "Tuya Button", mnmn: "SmartThings", vid: "generic-4-button"
        fingerprint inClusters: "0000, 0003, 0001", outClusters: "0006, 0003", manufacturer: "eWeLink", model: "WB01", deviceJoinName: "Sonoff Button", mnmn: "SmartThings", vid: "generic-1-button"
    }

    tiles(scale: 2)
    {  
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) 
        {
            state "battery", label: '${currentValue}% battery', unit: ""
        }
        multiAttributeTile(name: "button", type: "generic", width: 2, height: 2) 
        {
            tileAttribute("device.button", key: "PRIMARY_CONTROL") 
            {
                attributeState "pushed", label: "Pressed", icon:"st.Weather.weather14", backgroundColor:"#53a7c0"
                attributeState "double", label: "Pressed Twice", icon:"st.Weather.weather11", backgroundColor:"#53a7c0"
                attributeState "held", label: "Held", icon:"st.Weather.weather13", backgroundColor:"#53a7c0"
            }
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) 
        {
            state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        //main(["battery"])
        //details(["battery","button", "refresh"])
    }
}

private getAttrid_Battery() { 0x0020 } //
private getCLUSTER_GROUPS() { 0x0004 }
private getCLUSTER_SCENES() { 0x0005 }
private getCLUSTER_WINDOW_COVERING() { 0x0102 }

private Integer getNumberOfButtons() {
    switch (device.getDataValue("model")) {
        case "TS0041":
        case "WB01":
            return 1
        break
        case "TS0042":
            return 2
        break
        case "TS0043":
            return 3
        break
        case "TS0044":
        case "TS004F":
            return 4
        break
    }
}
private Map getBatteryEvent(value) 
{
    def result = [:]
    //result.value = value
    log.debug "Battery: ${value}"
    //Always value 0
    if (value <= 0) result.value = 100
    else result.value = value
    result.name = 'battery'
    result.descriptionText = "${device.displayName} battery was ${result.value}%"
    return result
}

private channelNumber(String dni) 
{
    dni.split(":")[-1] as Integer
}

def parse(String description) 
{
    log.debug "description is $description"
    def event = zigbee.getEvent(description)
    //def descMap1 = zigbee.parseDescriptionAsMap(description)


	//log.debug "zigbee.parse: ${zigbee.parse(description)}\n\n"
    //log.debug "\n\n\n descMap: $descMap1 \n\n\n"

    if (event) //non-standard 
    {
        sendEvent(event)
        //log.debug "sendEvent $event"
    }
    else 
    {
        if ((description?.startsWith("catchall:")) || (description?.startsWith("read attr -"))) 
        {
            def descMap = zigbee.parseDescriptionAsMap(description)            
            //log.debug "${descMap}"
            if (descMap.clusterInt == 0x0001 && descMap.attrInt == getAttrid_Battery()) 
            {
            	event = getBatteryResult(zigbee.convertHexToInt(descMap.value))
                //log.debug("Battery Result EVENT: ${event})")
            }
            else if (descMap.clusterInt == 0x0006 || descMap.clusterInt == 0x0008) 
            {
            	event = parseNonIasButtonMessage(descMap)
            }
            /*if (descMap.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.attrInt == getAttrid_Battery()) 
            {
            	event = getBatteryEvent(zigbee.convertHexToInt(descMap.value))
            */
        }
        def result = []
        if (event) 
        {
            //log.debug "Creating event: ${event}"
            result = createEvent(event)
        } 
        else if (isBindingTableMessage(description))         
        {
            Integer groupAddr = getGroupAddrFromBindingTable(description)
            if (groupAddr != null) 
            {
                List cmds = addHubToGroup(groupAddr)
                result = cmds?.collect 
                { 
                    new physicalgraph.device.HubAction(it) 
                }
            } 
            else 
            {
                groupAddr = 0x0000
                List cmds = addHubToGroup(groupAddr) +
                zigbee.command(CLUSTER_GROUPS, 0x00, "${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr, 4))} 00")
                result = cmds?.collect 
                { 
                    new physicalgraph.device.HubAction(it) 
                }
            }
        }
        //log.debug "table message? ${desscription}"
        result
    }
    
}

private Map getBatteryResult(rawValue)
{
    log.debug "getBatteryResult"
    //log.debug 'Battery'
    //def linkText = getLinkText(device)

    def result = [:]

    def volts = rawValue / 10
    if(!(rawValue == 0 || rawValue == 255)) 
    {
        def minVolts = 2.1
        def maxVolts = 3.0
        def pct = (volts - minVolts) / (maxVolts - minVolts)
        def roundedPct = Math.round(pct * 100)
        if(roundedPct <=0)
        roundedPct = 1
        result.value = Math.min(100, roundedPct)
        result.descriptionText = "battery: ${result.value}%"
        result.name = "battery"
    } 
    return result
}
def getBatteryPercentageResult(rawValue)
{
    log.debug "attrInt == 0x0021 : getBatteryPercentageResult"
    log.debug "Battery Percentage rawValue = ${rawValue} -> ${rawValue /2}%"
    def result = [:]

    if(0<= rawValue && rawValue <=200)
    {
        result.name = 'battery'
        result.translatble = true
        result.value = Math.round(rawValue/2)
        result.descriptionText = "${device.displayName} battery was ${result.value}%"
    }
    return result
}

private Map parseNonIasButtonMessage(Map descMap)
{
    def buttonState
    def buttonNumber = 0
    Map result = [:]

    switch (device.getDataValue("model")) {
        case "TS004F":
            if (descMap.clusterInt == 0x0006) 
            {
                switch(descMap.commandInt) 
                {
                    case "1":
                    buttonNumber = 1
                    break
                    case "0":
                    buttonNumber = 3
                    break
                }
                buttonState = "pushed"
            } else if (descMap.clusterInt == 0x0008) {
                switch(descMap.data[0]) 
                {
                    case "00":
                    buttonNumber = 2
                    break
                    case "01":
                    buttonNumber = 4
                    break
                }
                buttonState = "pushed"

                if(descMap.data.size > 0) {
                    if(descMap.data.size == 2) { buttonState = "held" }
                    //log.debug "data size $descMap.data.size"
                }
            }
        break
        case "WB01":
            if (descMap.clusterInt == 0x0006) {
                buttonNumber = 1
                switch (descMap.commandInt) {
                    case 0:
                        buttonState = "held"
                    break
                    case 1:
                        buttonState = "double"
                    break
                    default:
                        buttonState = "pushed"
                    break
                }
            }
        break
    }
    if (buttonNumber !=0) 
    {
        def descriptionText = "button $buttonNumber was $buttonState"
        result = [name: "button", value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true]
        sendButtonEvent(buttonNumber, buttonState)
    //return createEvent(name: "button$buttonNumber", value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
    }
    return result
}

private sendButtonEvent(buttonNumber, buttonState) 
{
    def child = childDevices?.find { channelNumber(it.deviceNetworkId) == buttonNumber }
    if (child)
    {
        def descriptionText = "$child.displayName was $buttonState" // TODO: Verify if this is needed, and if capability template already has it handled
        //log.debug "child $child"
        //log.debug "description $descriptionText"
        child?.sendEvent([name: "button", value: buttonState, data: [buttonNumber: 1], descriptionText: descriptionText, isStateChange: true])
    } 
    else 
    {
        //log.debug "Child device $buttonNumber not found!"
    }
}
def ping() {
refresh()
}
def refresh() 
{
    log.debug("SSSSSSSSSSSSSSSSSSSSS")
    log.debug "Refreshing Battery"
    updated()
    zigbee.onOffRefresh() + zigbee.onOffConfig()
    def tmp = zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, getAttrid_Battery()) + zigbee.enrollResponse()
    log.debug "${tmp}"
    tmp
}

def configure() 
{
    log.debug "Configuring Reporting, IAS CIE, and Bindings."
    return zigbee.onOffConfig() +
    zigbee.levelConfig() +
    zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20, DataType.UINT8, 30, 21600, 0x01) +
    zigbee.enrollResponse() +
    zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x20)
}

private getButtonName(buttonNum) 
{
    return "${device.displayName} " + buttonNum
}

private void createChildButtonDevices(numberOfButtons) 
{
    state.oldLabel = device.label
    //log.debug "Creating $numberOfButtons"
    log.debug "Creating $numberOfButtons children"
	
    for (i in 1..numberOfButtons) 
    {
        log.debug "Creating child $i"
        def child = addChildDevice("smartthings", "Child Button", "${device.deviceNetworkId}:${i}", device.hubId,[completedSetup: true, label: getButtonName(i),
            isComponent: true, componentName: "button$i", componentLabel: "buttton ${i}"])
        
        def buttonValue = ["pushed","double","held"]
        if(device.getDataValue("model") == "TS004F") {
            if(i % 2 == 0) {
                buttonValue = ["pushed","held"]
            } else {
                buttonValue = ["pushed"]
            }
        }
        //log.debug "${buttonValue}"
        child.sendEvent(name: "supportedButtonValues",value: buttonValue.encodeAsJSON(), displayed: false)
        child.sendEvent(name: "numberOfButtons", value: 1, displayed: false)
        //child.sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], displayed: false)
    }
}

def installed() 
{
    log.debug "installed() called"
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
    
    if (device.getDataValue("model") == 'TS004F') {
        // reference: https://github.com/Koenkk/zigbee2mqtt/discussions/7158
        log.debug("Sending request to initialize TS004F in Scene Switch mode")
        log.debug "${zigbee.writeAttribute(0x0006, 0x8004, 0x30, 0x00)}"
        zigbee.writeAttribute(0x0006, 0x8004, 0x30, 0x00)
        //state.lastButtonNumber = 0
    }
    def numberOfButtons = getNumberOfButtons()
    createChildButtonDevices(numberOfButtons) //Todo
    //sendEvent(name: "numberOfButtons", value: numberOfButtons , displayed: false)
    sendEvent(name: "supportedButtonValues", value: ["pushed","held","double"].encodeAsJSON(), displayed: false)
    sendEvent(name: "numberOfButtons", value: numberOfButtons , displayed: false)
    //sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], displayed: false)

    // Initialize default states
    /*numberOfButtons.times 
    {
        sendEvent(name: "button", value: "pushed", data: [buttonNumber: it+1], displayed: false)
    }*/
    // These devices don't report regularly so they should only go OFFLINE when Hub is OFFLINE
    sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "zigbee", scheme:"untracked"]), displayed: false)
}

def updated() 
{
    log.debug "updated() called"
    log.debug "childDevices $childDevices"
    if (childDevices && device.label != state.oldLabel) 
    {
        childDevices.each 
        {
            def newLabel = getButtonName(channelNumber(it.deviceNetworkId))
            it.setLabel(newLabel)
        }
        state.oldLabel = device.label
    }
}

private Integer getGroupAddrFromBindingTable(description) 
{
    log.info "Parsing binding table - '$description'"
    def btr = zigbee.parseBindingTableResponse(description)
    def groupEntry = btr?.table_entries?.find { it.dstAddrMode == 1 }
    if (groupEntry != null) 
    {
        log.info "Found group binding in the binding table: ${groupEntry}"
        return Integer.parseInt(groupEntry.dstAddr, 16)
    } 
    else 
    {
        log.info "The binding table does not contain a group binding"
        return null
    }
}

private List addHubToGroup(Integer groupAddr) 
{
    return ["st cmd 0x0000 0x01 ${CLUSTER_GROUPS} 0x00 {${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr,4))} 00}","delay 200"]
}

private List readDeviceBindingTable() 
{
    return ["zdo mgmt-bind 0x${device.deviceNetworkId} 0","delay 200"]
}