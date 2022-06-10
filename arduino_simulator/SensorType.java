package arduino_simulator;

public enum SensorType {
    //positive responses
    ACCELEROMETER((byte)0b0001_0000),
    LIGHT_SENSOR((byte)0b0010_0000),
    PRESSURE_SENSOR((byte)0b0011_0000),

    PROXIMITY_SENSOR((byte)10),

    TEMPERATURE_SENSOR((byte)11),

    RELATIVE_HUMIDITY_SENSOR((byte)12),

    ORIENTATION_SENSOR((byte)13),

    GYROSCOPE_TYPE((byte)14),

    PRESSURE_SENSOR_DIGITAL((byte)0b0011_0001),
    SENSOR_CUSTOM_ANALOG((byte)0b1111_1111),
    SENSOR_CUSTOM_DIGITAL((byte)0b1111_1110);
    private byte mByte;
    SensorType(byte mByte) {
        this.mByte = mByte;
    }

    public byte getValue() {
        return mByte;
    }

    public static Boolean isSensorDataDigital(SensorType sensorType){
        switch (sensorType){
            case LIGHT_SENSOR:
            case PRESSURE_SENSOR:
            case SENSOR_CUSTOM_ANALOG:
            case ACCELEROMETER: return false;
            case SENSOR_CUSTOM_DIGITAL:
            case PRESSURE_SENSOR_DIGITAL: return true;
        }

        return null;
    }

    public int dataByteSize(){
        return dataByteSize(this);
    }

    public static Boolean isSensorDataAnalog(SensorType sensorType){
        Boolean isDigital = isSensorDataDigital(sensorType);
        return (isDigital == null) ? null : !isDigital;
    }

    public static SensorType getSensorTypeFromByte(byte requestAsByte){
        if( requestAsByte == SensorType.LIGHT_SENSOR.mByte )
            return SensorType.LIGHT_SENSOR;
        if( requestAsByte == SensorType.PRESSURE_SENSOR.mByte )
            return SensorType.PRESSURE_SENSOR;
        if( requestAsByte == SensorType.PRESSURE_SENSOR_DIGITAL.mByte )
            return SensorType.PRESSURE_SENSOR_DIGITAL;
        if( requestAsByte == SensorType.ACCELEROMETER.mByte )
            return SensorType.ACCELEROMETER;

        return null;
    }

    public static int dataByteSize(SensorType sensorType){
        if( isSensorDataDigital(sensorType) )
            return 1;//digital data returns 0/1, 1 byte is enough for it (actually 1 bit but output stream can't take less than a byte
        else
            return 4;//analog data returns an integer 0-1023
    }
}
