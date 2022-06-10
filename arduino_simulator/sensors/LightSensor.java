package arduino_simulator.sensors;

import java.math.BigInteger;

import static arduino_simulator.SensorType.LIGHT_SENSOR;

public class LightSensor extends SensorImpl{

    public LightSensor(){
        super(LIGHT_SENSOR);
    }

    /**
     * will return null if data exception thrown while converting the int to the byte array
     * */
    @Override
    public Byte[] readDataValueAnalog() {

        //light sensor has analog data in it, analog in arduino 0-5V ~ 0-1023 representation
        int sensorData = Sensor.getRandomAnalogData();
        Byte[] sensorDataByteArray = Sensor.intToBytesObject(sensorData);
        return sensorDataByteArray;
    }

    /**
     * this way I can get ready data from the sensor itself though this is not the case in real life.
     * Driver framework should perform this conversion if they want to have it
     * */
    public Byte[] readDataValueAsLux(){
        Byte[] dataInBytes = readDataValueAnalog();
        byte[] primitiveArray = new byte[dataInBytes.length];

        for( int i = 0; i<dataInBytes.length; ++i)
            primitiveArray[i] = dataInBytes[i];

        int numericalValue = new BigInteger(primitiveArray).intValue();
        double luxValue = ((numericalValue*5.0)/1024.0);

        return Sensor.doubleToBytesObject(luxValue);
    }

}
