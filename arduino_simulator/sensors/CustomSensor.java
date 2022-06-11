package arduino_simulator.sensors;


import arduino_simulator.SensorType;

import java.nio.ByteBuffer;
import java.util.Random;

public class CustomSensor extends SensorImpl{

    public CustomSensor() {
        super(SensorType.SENSOR_CUSTOM_ANALOG);
    }


    @Override
    public Byte[] readDataValueAnalog() {
        Random r = new Random();
        double randomValue = 300 + (333 - 300) * r.nextDouble();

        byte[] arrTemp = ByteBuffer.allocate(8).putDouble(randomValue).array();
        Byte[] arr = new Byte[8];
        for( int i = 0; i<8; ++i ){
            arr[i] = arrTemp[i];
        }

        return arr;
    }
}
