package arduino_simulator;
import arduino_simulator.sensors.LightSensor;
import arduino_simulator.sensors.PressureSensor;
import arduino_simulator.sensors.Sensor;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;

public class ArduinoSimulatorTest {

    public static void main(String[] args) {
        testRaw();
    }

    public static void read(InputStream inputStream, byte[] byteArray){
        try {
            int ret = inputStream.read(byteArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testWithConnection(){
        LightSensor lightSensor = new LightSensor();
        PressureSensor pressureSensor = new PressureSensor();
        ArrayList<Sensor> availableSensors = new ArrayList();
        availableSensors.add(lightSensor);
        availableSensors.add(pressureSensor);

        ArduinoSimulator arduinoSimulator = new ArduinoSimulator(availableSensors, 9600);

        arduinoSimulator.serialBegin(9600);

        System.out.println("Connecting to arduino bluetooth");
        InputStream inputStream = arduinoSimulator.connectViaBluetooth();
        System.out.println("Connected");


        arduinoSimulator.start();


        byte[] buffer = new byte[4];
        read( inputStream, buffer );
        System.out.println("read data is " + (new BigInteger(buffer)).intValue());

        arduinoSimulator.disconnect();
    }

    private static void testRaw(){
        LightSensor lightSensor = new LightSensor();
        PressureSensor pressureSensor = new PressureSensor();
        ArrayList<Sensor> availableSensors = new ArrayList();
        availableSensors.add(lightSensor);
        availableSensors.add(pressureSensor);

        ArduinoSimulator arduinoSimulator = new ArduinoSimulator(availableSensors, 9600);
        arduinoSimulator.serialBegin(9600);

        arduinoSimulator.start();

        //read last data 10 times
        for( int i = 0; i<10; ++i ){

            try {
                Thread.sleep(500);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            byte[] data = new byte[0];
            while( data.length == 0 )
                data = arduinoSimulator.getLastReading();//last reading has reading of both light sensor and pressure sensor

            for( int i2 = 0; i2<availableSensors.size(); ++i2){
                Sensor sensor = availableSensors.get(i2);

                byte[] lastRaedingBytes = new byte[4];
                for( int k = 0; k<4; ++k ){
                    lastRaedingBytes[k] = data[i2*4 + k];
                }
                int lastReading = new BigInteger(lastRaedingBytes).intValue();

                System.out.println( sensor.getSensorType().toString() + ": " + lastReading);

            }
            System.out.println();
        }


        arduinoSimulator.disconnect();
    }
}
