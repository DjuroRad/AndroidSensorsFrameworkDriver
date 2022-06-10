package arduino_simulator.sensors;

import arduino_simulator.ArduinoSimulator;
import arduino_simulator.SensorType;

import java.io.*;

public interface Sensor{

    /**
     * Will be different for each sensor, for light sensor it will be Int for example
     * For some digital data Boolean (digital data) will be cast to object.
     * Allows more flexibility for the simulator like this.
     *
     * @return int or Byte[] depending on sensor's data type. Caller has to cast Object to int or byte[] afterwards if they want to use it. -1 indicates analog data, null indicates digital data if there is an invalid attempt of casting.
     * */
    public byte[] readDataValue();
    /**
     * -1 indicates error, meaning some sensor that is not digital will return -1 for example
     *  digital value is of a single byte 0 - false, otherwise - true
     *  -1 indicates error and returns int since byte representation of -1 is different for int since it has 4 bytes
     * @return -1 if not digital or some error occurred
     * */
    public int readDataValueDigital();
    /**
     * @return null if error occurred / not analog data
     * */
    public Byte[] readDataValueAnalog();

    /**
     * used to specify type of data registered sensor uses
     * */
    public boolean isDataDigital();

    /**
     * returns type of the sensor
     * */
    public SensorType getSensorType();

    /**
     * some static declarations relevant to Sensors in general
     * */

    //TYPE CONVERSIONS
    public static byte[] intToBytes(int number) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);


        try {
            dataOutputStream.writeInt(number);
            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        byte[] int_bytes = byteArrayOutputStream.toByteArray();

        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return int_bytes;
    }

    public static Byte[] intToBytesObject(int number) {

        byte[] int_bytes = intToBytes(number);

        return byteArrayObjectFromPrimitiveByteArray(int_bytes);
    }

    public static byte[] doubleToBytes ( double i ) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try{
            dos.writeDouble(i);
            dos.flush();
            bos.close();
            dos.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public static Byte[] doubleToBytesObject(double number) {

        byte[] double_bytes = doubleToBytes(number);

        return byteArrayObjectFromPrimitiveByteArray(double_bytes);
    }

    public static Byte[] byteArrayObjectFromPrimitiveByteArray(byte[] byteArray){
        Byte[] byteArrayObject = new Byte[byteArray.length];
        for( int i = 0; i < byteArray.length; ++i )
            byteArrayObject[i] = byteArray[i];

        return byteArrayObject;
    }

    public static byte[] primitiveByteArrayFromByteArrayObject(Byte[] byteArray){
        byte[] byteArrayPrimitive = new byte[byteArray.length];
        for( int i = 0; i < byteArray.length; ++i )
            byteArrayPrimitive[i] = byteArray[i];

        return byteArrayPrimitive;
    }

    //SENSOR DATA TYPE RETURN VALUES
    public static int getRandomAnalogData() {
        int valueRead = (int) ((Math.random() * (ArduinoSimulator.ANALOG_UPPER_LIMIT - ArduinoSimulator.ANALOG_LOWER_LIMIT)) + ArduinoSimulator.ANALOG_LOWER_LIMIT);
        return valueRead;
    }

    public static byte getRandomDigitalData() {
        long randomValue = Math.round( Math.random() );
        return (byte)randomValue;
    }
}
