package arduino_simulator.sensors;

import arduino_simulator.SensorType;

public class PressureSensor extends SensorImpl{

    public PressureSensor() {
        super(SensorType.PRESSURE_SENSOR_DIGITAL);
    }

    /**
     * will return null if data exception thrown while converting the int to the byte array
     * */
    @Override
    public int readDataValueDigital() {
        return Sensor.getRandomDigitalData();
    }
}
