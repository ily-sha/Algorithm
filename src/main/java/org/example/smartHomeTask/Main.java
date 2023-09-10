package org.example.smartHomeTask;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Time;
import java.time.Duration;
import java.util.*;


public class Main {

    private final static int MASK_CONTINUE = 0x80;
    private final static int MASK_DATA = 0x7f;
    private final static int broadcastAddress = 0x3fff;

    private static final int DEV_TYPE_SIZE = 1;
    private static final int CMD_SIZE = 1;
    private static final int CRC8_AND_LENGTH_SIZE = 2;
    private static final String NAME = "SmartHub";

    private int SERAIL = 1;
    HttpClient httpClient;

    private final Queue<byte[]> queueRequests = new LinkedList<>();


    interface PowerSwitch {

        DeviceState getDeviceState();

        void changeDeviceState(DeviceState state);

        void connectWithSwitch(Switch connectedSwitch);

    }

    enum DeviceState {
        OFF(0), ON(1);

        final int state;

        DeviceState(int i) {
            state = i;
        }
    }

    enum SensorType {
        TEMPERATURE, HUMIDITY, LIGHT, AIR_POLLUTION
    }

    static class Device {

        //lamp or socket
        private final String name;
        private final int address;
        private final DeviceType deviceType;


        public Device(String name, int address, DeviceType deviceType) {
            this.name = name;
            this.address = address;
            this.deviceType = deviceType;
        }


        @Override
        public boolean equals(Object obj) {
            if (hashCode() != obj.hashCode()) return false;
            if (obj instanceof Device) {
                return name.equals(((Device) obj).name) && address == (((Device) obj).address);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return address % 100;
        }
    }


    static class Switch extends Device {

        private DeviceState state;

        private List<PowerSwitch> connectedDevices;
        private final String[] powerSwitchNames;


        public Switch(String name, int address, String[] powerSwitchNames) {
            super(name, address, DeviceType.SWITCH);
            this.state = DeviceState.ON;
            this.powerSwitchNames = powerSwitchNames;
        }


        public void connectToDevices(Set<Device> deviceList) {
            connectedDevices = new ArrayList<>(powerSwitchNames.length);
            for (String deviceName : powerSwitchNames) {
                PowerSwitch foundedDevice = null;
                for (Device device : deviceList) {
                    if (deviceName.equals(device.name)) {
                        foundedDevice = (PowerSwitch) device;
                        break;
                    }
                }

                if (foundedDevice != null) {
                    connectedDevices.add(foundedDevice);
                    foundedDevice.connectWithSwitch(this);
                }
            }

        }

    }

    static class Lamp extends Device implements PowerSwitch {

        private DeviceState state;
        private Switch connectedSwitch;

        public Lamp(String name, int address) {
            super(name, address, DeviceType.LAMP);
            this.state = DeviceState.ON;
        }

        public void connectWithSwitch(Switch connectedSwitch) {
            this.connectedSwitch = connectedSwitch;
        }

        @Override
        public DeviceState getDeviceState() {
            return state;
        }

        @Override
        public void changeDeviceState(DeviceState state) {
            this.state = state;
        }
    }

    static class Socket extends Device implements PowerSwitch {

        private DeviceState state;
        private Switch connectedSwitch;

        public Socket(String name, int address) {
            super(name, address, DeviceType.SOCKET);
            this.state = DeviceState.ON;
        }

        public void connectWithSwitch(Switch connectedSwitch) {
            this.connectedSwitch = connectedSwitch;
        }


        @Override
        public DeviceState getDeviceState() {
            return state;
        }

        @Override
        public void changeDeviceState(DeviceState state) {
            this.state = state;
        }
    }

     class EnvSensor extends Device {

        private final SensorDevProperty sensorDevProperty;

        public EnvSensor(String name, int address, SensorDevProperty sensorDevProperty) {
            super(name, address, DeviceType.ENV_SENSOR);
            this.sensorDevProperty = sensorDevProperty;
       }

       public void updateDevices(int[] parameters) {
            for (int i = 0; i < parameters.length; i++) {
                int parameter = parameters[i];

                    SensorTrigger sensorTrigger = (SensorTrigger) sensorDevProperty.sensorTriggerList.get(i);
                    if ((sensorTrigger.compareOperator == 0 && sensorTrigger.thresholdValue > parameter) ||
                            (sensorTrigger.compareOperator == 1 && sensorTrigger.thresholdValue < parameter)){
                        Device device = findDeviceByName(sensorTrigger.deviceName);
                        if (device != null){
                            sendPostRequest(createCommandSETSTATUS(device.address, device.deviceType, sensorTrigger.operation));
                        }

                }
            }
       }
    }

    private Device findDeviceByName(String deviceName) {
        for (Device device: devices){
            if (device.name.equals(deviceName)) return device;
        }
        return null;
    }


    private final Set<Device> devices = new HashSet<>();

    public enum DeviceType {
        SMART_HUB(1), ENV_SENSOR(2), SWITCH(3), LAMP(4), SOCKET(5), CLOCK(6);

        final int deviceNumber;

        DeviceType(int i) {
            deviceNumber = i;
        }
    }

    public enum CommandType {
        WHOISHERE(1), IAMHERE(2), GETSTATUS(3), STATUS(4), SETSTATUS(5), TICK(6);

        final int commandNumber;

        CommandType(int i) {
            commandNumber = i;
        }
    }



    private String serverURL;
    private int hubAddress;

    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();

        main.httpClient = HttpClient.newBuilder().build();
        System.out.println(System.currentTimeMillis());
        main.serverURL = args[0];
        main.hubAddress = Integer.parseInt(args[1], 16);
        byte[] command = main.createCommandWHOISHERE();
        main.sendPostRequest(command);

//        main.decodeReceived("OAL_fwMCAQhTRU5TT1IwMQ8EDGQGT1RIRVIxD7AJBk9USEVSMgCsjQYGT1RIRVIzCAAGT1RIRVI03Q");
        while (true) {
            if (main.queueRequests.size() == 0) {
                for (Device device : main.devices) {
                    command = main.createCommandGETSTATUS(device.address, device.deviceType);
                    main.queueRequests.add(command);
                }
            }
            main.sendPostRequest(main.compactRequests(main.queueRequests));
            main.queueRequests.clear();
//            Thread.sleep(300);
        }

    }


    private byte[] compactRequests(Queue<byte[]> requests) {
        int size = 0;
        for (byte[] request : requests) size += request.length;
        byte[] result = new byte[size];
        int index = 0;
        for (byte[] request : requests) {
            for (byte b : request) {
                result[index++] = b;
            }
        }
        return result;
    }


    private void sendPostRequest(byte[] requestBytes) {
        String requestString = Base64.getUrlEncoder().encodeToString(requestBytes).replace("=", "");
//        System.out.println("Sended " + requestString);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverURL))
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(requestString))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204){
                System.out.println(response.body());
            }
            System.out.println(response.statusCode() + " " + System.currentTimeMillis());
            switch (response.statusCode()) {
                case 200 -> {
                    decodeReceived(response.body());
                }
                case 204 -> {
                    System.exit(0);
                }
                default -> {
                    System.exit(99);
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void decodeReceived(String code) {
        byte[] arr = Base64.getUrlDecoder().decode(code);
//        System.out.println("Received " + code);
        int index = 0;
        try {
            do {
                int packetLen = arr[index++];
                Payload receivedPayload = Payload.restore(arr, index);
                index += packetLen;
                answerToReceived(receivedPayload);
            } while (++index != arr.length);
        } catch (Exception ignored){
        }

    }


    private void answerToReceived(Payload payload) {
//        System.out.println("From " + payload.deviceType + " Command " + payload.commandType);
        switch (payload.commandType) {
            case WHOISHERE -> {
                handleWHOISHERECommand(payload);
            }
            case IAMHERE -> {
                handleIAMHERECommand(payload);
            }
            case STATUS -> {
                switch (payload.deviceType) {
                    case LAMP, SOCKET -> {
                        for (Device device : devices) {
                            if (device.address == payload.src) {
                                ((PowerSwitch) device).changeDeviceState(payload.commandBody.deviceState);
                                break;
                            }
                        }
                    }
                    case SWITCH -> {
                        handleSwitchStatusCommand(payload);
                    }
                    case ENV_SENSOR -> {
                        handleSensorStatusCommand(payload);
                    }
                }
            }
            case TICK -> {
            }
        }
    }

    private void handleSensorStatusCommand(Payload payload) {
        EnvSensor sensor = (EnvSensor) findDeviceByAddress(payload.src);
        if (sensor != null){
            sensor.updateDevices(payload.commandBody.sensorParameters);
        }
    }

    private void handleSwitchStatusCommand(Payload payload) {
        Switch switchDevice = (Switch) findDeviceByAddress(payload.src);
        if (switchDevice != null) {
            if (switchDevice.connectedDevices == null) {
                switchDevice.connectToDevices(devices);
            }
            DeviceState receivedDeviceState = payload.commandBody.deviceState;
            for (PowerSwitch device : switchDevice.connectedDevices) {
                if (device.getDeviceState() != receivedDeviceState) {
                    queueRequests.add(createCommandSETSTATUS(((Device) device).address, ((Device) device).deviceType, receivedDeviceState));
                }
            }
            switchDevice.state = receivedDeviceState;
        }

    }

    private void handleIAMHERECommand(Payload payload) {
        switch (payload.deviceType) {
            case SWITCH -> {
                Device device = new Switch(payload.commandBody.name, payload.src, payload.commandBody.switchConnected);
                devices.add(device);
            }
            case SOCKET -> {
                Device device = new Socket(payload.commandBody.name, payload.src);
                devices.add(device);
            }
            case LAMP -> {
                Device device = new Lamp(payload.commandBody.name, payload.src);
                devices.add(device);
            }
            case ENV_SENSOR -> {
                Device device = new EnvSensor(payload.commandBody.name, payload.src, payload.commandBody.sensorDevProperty);
                devices.add(device);
            }
        }
    }


    private void handleWHOISHERECommand(Payload payload) {
        Device device = switch (payload.deviceType) {
            case SWITCH ->  new Switch(payload.commandBody.name, payload.src, payload.commandBody.switchConnected);
            case LAMP -> new Lamp(payload.commandBody.name, payload.src);
            case SOCKET -> new Socket(payload.commandBody.name, payload.src);
            case ENV_SENSOR -> new EnvSensor(payload.commandBody.name, payload.src, payload.commandBody.sensorDevProperty);
            default -> throw new RuntimeException("Not such devices as " + payload.deviceType);
        };
        devices.add(device);
        queueRequests.add(createCommandIAMHERE());
    }

    private Device findDeviceByAddress(int src) {
        for (Device device : devices) {
            if (device.address == src) {
                return device;
            }
        }
        return null;
    }


    private static int convertFromULEB128(byte[] arr) {
        int result = arr[arr.length - 1];
        for (int i = arr.length - 2; i > -1; i--) {
            result <<= 7;
            result |= (arr[i] & MASK_DATA);
        }
        return result;
    }


    private static byte[] convertToULEB128(long value) {
        ArrayList<Byte> bytes = new ArrayList<>();
        do {
            byte b = (byte) (value & MASK_DATA);
            value >>= 7;
            if (value != 0) {
                b = (byte) (b | MASK_CONTINUE);
            }
            bytes.add(b);
        } while (value != 0);

        byte[] ret = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            ret[i] = bytes.get(i);
        }
        return ret;
    }


    public byte[] createCommandWHOISHERE() {
        CommandBody body = new CommandBody(NAME);
        Payload payload = new Payload(hubAddress, broadcastAddress, SERAIL++, DeviceType.SMART_HUB,
                CommandType.WHOISHERE, body);
        return new Packet(payload).getPacket();
    }

    public byte[] createCommandGETSTATUS(int destination, DeviceType receiver) {
        Payload payload = new Payload(hubAddress, destination, SERAIL++, receiver,
                CommandType.GETSTATUS, new CommandBody());
        return new Packet(payload).getPacket();
    }

    public byte[] createCommandSETSTATUS(int destination, DeviceType receiver, DeviceState state) {
        Payload payload = new Payload(hubAddress, destination, SERAIL++, receiver,
                CommandType.SETSTATUS, new CommandBody(state));
        return new Packet(payload).getPacket();
    }

    public byte[] createCommandIAMHERE() {
        Payload payload = new Payload(hubAddress, broadcastAddress, SERAIL++, DeviceType.SMART_HUB,
                CommandType.IAMHERE, new CommandBody(NAME));
        return new Packet(payload).getPacket();
    }


    static class Packet {

        private byte[] packet;
        private final Payload payload;

        public Packet(Payload payload) {
            this.payload = payload;
        }


        private byte getCrc8() {
            byte crc = 0;
            byte generator = 0x1D;
            for (byte b : payload.getPayload()) {
                crc ^= b;
                for (int i = 0; i < 8; i++) {
                    if ((crc & 0x80) != 0) {
                        crc = (byte) ((crc << 1) ^ generator);
                    } else {
                        crc <<= 1;
                    }
                }
            }
            return crc;
        }

        public byte[] getPacket() {
            if (packet != null) return packet;
            packet = new byte[CRC8_AND_LENGTH_SIZE + payload.getPayload().length];
            packet[0] = (byte) payload.getPayload().length;
            int i = 1;
            for (byte b : payload.getPayload()) {
                packet[i++] = b;
            }
            packet[i] = getCrc8();
            return packet;
        }

        @Override
        public String toString() {
            return payload.toString();
        }
    }

    static class Payload {
        private byte[] payload;
        private final int src, dst, serial;
        private final CommandBody commandBody;
        private final CommandType commandType;
        private final DeviceType deviceType;


        public Payload(int src, int dst, int serial,
                       DeviceType deviceType, CommandType commandType, CommandBody commandBody) {
            this.src = src;
            this.dst = dst;
            this.serial = serial;
            this.commandType = commandType;
            this.commandBody = commandBody;
            this.deviceType = deviceType;

        }

        public static Payload restore(byte[] payload, int startArr) {
            int i = startArr;
            //size = 4 т.к. сначала вставляем 8 байт, а потом можем максимум 3 раза << 7
            byte[] src_arr = new byte[4];
            int arr_index = 0;
            do {
                src_arr[arr_index++] = payload[i];
            }
            while (payload[i++] < 0);

            byte[] dst_arr = new byte[4];
            arr_index = 0;
            do {
                dst_arr[arr_index++] = payload[i];
            }
            while (payload[i++] < 0);

            arr_index = 0;
            byte[] serial_arr = new byte[4];
            do {
                serial_arr[arr_index++] = payload[i];
            }
            while (payload[i++] < 0);

            DeviceType deviceType = decodeDeviceType(payload[i++]);
            CommandType commandType = decodeCommandType(payload[i++]);
            CommandBody commandBody = decodeCommandBody(i++, deviceType, commandType, payload);

            return new Payload(convertFromULEB128(src_arr), convertFromULEB128(dst_arr), convertFromULEB128(serial_arr),
                    deviceType, commandType, commandBody == null ? new CommandBody() : commandBody);
        }

        private static CommandBody decodeCommandBody(int i, DeviceType deviceType, CommandType commandType, byte[] payload) {
            CommandBody commandBody = null;
            switch (commandType) {
                case IAMHERE, WHOISHERE -> {
                    switch (deviceType) {
                        case SWITCH -> {
                            commandBody = CommandBody.restoreSwitchIAMHERE(payload, i);
                        }
                        case ENV_SENSOR -> {
                            commandBody = CommandBody.restoreSensorProperty(payload, i);
                        }
                        default -> {
                            commandBody = new CommandBody(CommandBody.restoreString(payload, i));
                        }
                    }

                }
                case TICK -> {
                    List<Byte> time_arr = new ArrayList<Byte>();
                    do {
                        time_arr.add(payload[i]);
                    }
                    while (payload[i++] < 0);
                    byte[] time2 = new byte[time_arr.size()];
                    for (int j = 0; j < time_arr.size(); j++) {
                        time2[j] = time_arr.get(j);
                    }
                    commandBody = new CommandBody(time2);
                }
                case STATUS -> {
                    switch (deviceType) {
                        case LAMP, SOCKET, SWITCH -> {
                            return CommandBody.restoreDeviseState(payload, i);
                        }
                        case ENV_SENSOR -> {
                            return CommandBody.restoreSensorStatus(payload, i);
                        }
                    }
                }
            }
            return commandBody;
        }

        @Override
        public String toString() {
            return "Payload{" +
                    "payload=" + Arrays.toString(payload) +
                    ", src=" + src +
                    ", dst=" + dst +
                    ", serial=" + serial +
                    ", commandType=" + commandType +
                    ", deviceType=" + deviceType +
                    ", commandBody=" + commandBody +
                    '}';
        }

        private static CommandType decodeCommandType(int commandTypeNumber) {
            CommandType commandType = null;
            for (CommandType comType : CommandType.values()) {
                if (comType.commandNumber == commandTypeNumber) {
                    commandType = comType;
                    break;
                }
            }
            return commandType;
        }

        private static DeviceType decodeDeviceType(int deviceTypeNumber) {
            DeviceType deviceType = null;
            for (DeviceType devType : DeviceType.values()) {
                if (devType.deviceNumber == deviceTypeNumber) {
                    deviceType = devType;
                    break;
                }
            }
            return deviceType;
        }

        public byte[] getPayload() {
            if (payload != null) return payload;
            byte[] src_arr = convertToULEB128(src);
            byte[] dst_arr = convertToULEB128(dst);
            byte[] serial_arr = convertToULEB128(serial);

            payload = new byte[src_arr.length + dst_arr.length + DEV_TYPE_SIZE + serial_arr.length +
                    CMD_SIZE + commandBody.getCommandBody().length];
            int i = 0;
            for (byte b : src_arr) {
                payload[i++] = b;
            }
            for (byte b : dst_arr) {
                payload[i++] = b;
            }
            for (byte b : serial_arr) {
                payload[i++] = b;
            }
            payload[i++] = (byte) deviceType.deviceNumber;
            payload[i++] = (byte) commandType.commandNumber;

            for (byte b : commandBody.getCommandBody()) {
                payload[i++] = b;
            }
            return payload;
        }


    }

    static class CommandBody {
        private int[] sensorParameters;
        private SensorDevProperty sensorDevProperty;
        private byte[] commandBody;
        private String name;

        private DeviceState deviceState;

        private String[] switchConnected;

        public CommandBody(byte[] timestamp) {
            commandBody = timestamp;
        }

        public CommandBody(boolean turnOff) {
            commandBody = new byte[]{turnOff ? (byte) 0 : 1};
        }

        public CommandBody() {
            commandBody = new byte[0];
        }

        public CommandBody(String name, String[] switchConnected) {
            this.name = name;
            this.switchConnected = switchConnected;
        }

        public CommandBody(int[] sensorParameters) {
            this.sensorParameters = sensorParameters;
        }

        public CommandBody(DeviceState deviceState) {
            this.deviceState = deviceState;
        }

        public CommandBody(String name) {
            this.name = name;
        }

        public CommandBody(String name, SensorDevProperty sensorDevProperty) {
            this.name = name;
            this.sensorDevProperty = sensorDevProperty;
        }

        public static CommandBody restoreSwitchIAMHERE(byte[] payload, int i) {
            String name = restoreString(payload, i);
            i += payload[i++];
            int countString = payload[++i];
            String[] switchConnected = new String[countString];
            for (int j = 0; j < countString; j++) {
                switchConnected[j] = restoreString(payload, ++i);
                i += payload[i++];
            }
            return new CommandBody(name, switchConnected);
        }

        public static CommandBody restoreDeviseState(byte[] payload, int i) {
            return new CommandBody(payload[i] == 1 ? DeviceState.ON : DeviceState.OFF);
        }

        public static String restoreString(byte[] payload, int i) {
            int len = payload[i++];
            byte[] string_arr = new byte[len];
            for (int j = 0; j < len; j++) {
                string_arr[j] = payload[i++];
            }
            return new String(string_arr);
        }

        public static CommandBody restoreSensorStatus(byte[] payload, int i) {
            int valuesLen = payload[i++];
            int[] values = new int[valuesLen];
            for (int j = 0; j < valuesLen; j++){

                byte[] parameter = new byte[4];
                int arr_index = 0;
                do {
                    parameter[arr_index++] = payload[i];
                }
                while (payload[i++] < 0);
                values[j] = convertFromULEB128(parameter);
            }
            return new CommandBody(values);
        }



        public static CommandBody restoreSensorProperty(byte[] payload, int i) {
            String sensorName = restoreString(payload, i);
            i += sensorName.length() + 1;
            int sensors = payload[i++];
            int triggersSize = payload[i++];
            int mask = 1;
            EnumSet<SensorType> sensor = EnumSet.noneOf(SensorType.class);
            for (SensorType sensorType : SensorType.values()) {
                if ((sensors & mask) == mask) {
                    sensor.add(sensorType);
                }
                mask <<= 1;
            }
            List<SensorTrigger> sensorTriggers = new ArrayList<>();
            for (int j = 0; j < triggersSize; j++) {

                int operation = payload[i++];
                DeviceState deviceState = (operation & 1) == 1 ? DeviceState.ON : DeviceState.OFF;
                int compareOperation = (operation & 2) == 2 ? 1 : 0;

                SensorType sensorType = switch (operation >> 2){
                    case 0 -> SensorType.TEMPERATURE;
                    case 1 -> SensorType.HUMIDITY;
                    case 2 -> SensorType.LIGHT;
                    case 3 -> SensorType.AIR_POLLUTION;
                    default -> throw new RuntimeException("Unknown sensor type");
                };
                byte[] thresholdValue = new byte[4];
                int arr_index = 0;
                do {
                    thresholdValue[arr_index++] = payload[i];
                }
                while (payload[i++] < 0);

                String name = restoreString(payload, i);
                i += name.length() + 1;
                sensorTriggers.add(new SensorTrigger(deviceState, compareOperation, sensorType,
                        convertFromULEB128(thresholdValue), name));
            }
            sensorTriggers.sort(new Comparator<SensorTrigger>() {
                @Override
                public int compare(SensorTrigger o1, SensorTrigger o2) {
                    return o1.sensorType.compareTo(o2.sensorType);
                }
            });

            return new CommandBody(sensorName, new SensorDevProperty(sensor, sensorTriggers));
        }


        public byte[] getCommandBody() {
            if (commandBody != null) return commandBody;
            if (deviceState != null) return new byte[]{(byte) deviceState.state};
            if (name != null) {
                commandBody = new byte[1 + name.length()];
                int i = 0;
                commandBody[i++] = (byte) name.length();
                for (byte b : name.getBytes()) {
                    commandBody[i++] = b;
                }
                return commandBody;
            }
            return new byte[0];
        }

        @Override
        public String toString() {
            return "CommandBody{" +
                    "commandBody=" + Arrays.toString(commandBody) +
                    ", name='" + name + '\'' +
                    ", deviceState=" + deviceState +
                    ", switchConnected=" + Arrays.toString(switchConnected) +
                    '}';
        }
    }

    static class SensorTrigger {
        private final DeviceState operation;
        private final int compareOperator;
        private final String deviceName;
        private final SensorType sensorType;
        private final int thresholdValue;

        public SensorTrigger(DeviceState deviceState, int compareOperator, SensorType sensorType, int thresholdValue, String deviceName) {
            this.operation = deviceState;
            this.compareOperator = compareOperator;
            this.sensorType = sensorType;
            this.thresholdValue = thresholdValue;
            this.deviceName = deviceName;
        }

        @Override
        public String toString() {
            return "SensorTriggers{" +
                    "operation=" + operation +
                    ", compareOperator=" + compareOperator +
                    ", deviceName='" + deviceName + '\'' +
                    ", sensorType=" + sensorType +
                    ", thresholdValue=" + thresholdValue +
                    '}';
        }
    }

    static class SensorDevProperty {
        private final EnumSet<SensorType> sensors;
        private final List<SensorTrigger> sensorTriggerList;


        public SensorDevProperty(EnumSet<SensorType> sensors, List<SensorTrigger> sensorTriggerList) {
            this.sensors = sensors;
            this.sensorTriggerList = sensorTriggerList;
        }

        @Override
        public String toString() {
            return "SensorDevProperty{" +
                    "sensors=" + sensors +
                    ", sensorTriggersList=" + sensorTriggerList +
                    '}';
        }
    }



}