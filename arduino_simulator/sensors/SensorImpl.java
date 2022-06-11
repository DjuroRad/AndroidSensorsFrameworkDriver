package arduino_simulator.sensors;

import arduino_simulator.SensorType;

import static arduino_simulator.SensorType.isSensorDataDigital;

/**
 * If you want to define a new sensor for testing as driver developer extend this class
 * when implementing it, pass appropriate sensor type to constructor. If sensor type not in the list of available sensor types, use custom sensor.
 * */
public abstract class SensorImpl implements Sensor {

    protected SensorType sensorType;

    SensorImpl(SensorType sensorType){
        this.sensorType = sensorType;
    }

    @Override
    public byte[] readDataValue() {
        if( isDataDigital() ) {
            int digitalResult = readDataValueDigital();

            return Sensor.intToBytes(digitalResult);
        }
        else {
            Byte[] analogData = readDataValueAnalog();

            return  Sensor.primitiveByteArrayFromByteArrayObject(analogData);
        }
    }

    /**
     * used for error indication, specific sensor needs to override this method in order for it to work
     * */
    @Override
    public int readDataValueDigital() {
        return -1;
    }


    /**
     * used for error indication, specific sensor needs to override this method in order for it to work
     * */
    @Override
    public Byte[] readDataValueAnalog() {
        return null;
    }

    /**
     * returns sensor type
     * */
    @Override
    public SensorType getSensorType() {
        return this.sensorType;
    }

    /**
     * readability purposes, checks if data of the sensor is digital
     * */
    public boolean isDataDigital(){
        return (isSensorDataDigital(sensorType) == Boolean.TRUE);
    }
}
