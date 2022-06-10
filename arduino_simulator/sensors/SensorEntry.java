package arduino_simulator.sensors;

import arduino_simulator.SensorType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * class contains basic sensor's requirements for functioning, its limitations and similary, basically it is kind of metadata for sensor
 * part of this data is shared on the initial handshake performed when we connect to the device driver
 * */
public class SensorEntry {

    public static final int DEFAULT_SAMPLE_RATE = 500;//default sample rate indicates that each sample reading is to be performed on half a second
    public static final int DEFAULT_MAX_SAMPLE_RATE = 10000;
    public static final int DEFAULT_MIN_SAMPLE_RATE = 25;

    private int sampleRate = DEFAULT_SAMPLE_RATE;
    private int MAX_SAMPLE_RATE = DEFAULT_MAX_SAMPLE_RATE;
    private int MIN_SAMPLE_RATE = DEFAULT_MIN_SAMPLE_RATE;

    private SensorType sensorType = null;
    private int sensorID = -1;
    private int dataSampleByteLength = -1;

    private boolean isConnected = false;
    public final static int SENSOR_ENTRY_BYTE_LENGTH = 9; // 1 + 4 + 4  -  type + id + sample_byte_length

    private static final String TAG = "SensorEntry";
    private static final Logger logger = Logger.getLogger( TAG );

    public SensorEntry(SensorType sensorType, int sensorID, int dataSampleByteLength) {
        this.sensorType = sensorType;
        this.sensorID = sensorID;
        this.dataSampleByteLength = dataSampleByteLength;
    }

    /***/
    public SensorEntry(SensorType sensorType, int sensorID, int dataSampleByteLength, int maxSampleRate, int minSampleRate) {
        this.sensorType = sensorType;
        this.sensorID = sensorID;
        this.dataSampleByteLength = dataSampleByteLength;
        this.MAX_SAMPLE_RATE = maxSampleRate;
        this.MIN_SAMPLE_RATE = minSampleRate;
        this.sampleRate = maxSampleRate / minSampleRate;//overrides default
    }

    public void sendSensorEntry(OutputStream outputStream){
        //5 bytes are sent for sensor entry on handshake, 1 byte is for the sensor type, the other 4 bytes are for the id
        byte[] sensorEntryAsBytesArray = new byte[SENSOR_ENTRY_BYTE_LENGTH];

        byte sensorTypeByte = sensorType.getValue();
        byte[] sensorIdBytes = ByteBuffer.allocate(4).putInt(sensorID).array();
        byte[] dataSampleLengthBytes = ByteBuffer.allocate(4).putInt(dataSampleByteLength).array();

        sensorEntryAsBytesArray[0] = sensorTypeByte;
        for( int i = 1; i<5; ++i )
            sensorEntryAsBytesArray[i] = sensorIdBytes[i-1];
        for( int i = 5; i<SENSOR_ENTRY_BYTE_LENGTH; ++i )
            sensorEntryAsBytesArray[i] = dataSampleLengthBytes[i-5];

        try {
            outputStream.write(sensorEntryAsBytesArray);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    //getters
    public int getSampleRate() { return sampleRate; }
    public SensorType getSensorType() {
        return sensorType;
    }
    public int getSensorID() {
        return sensorID;
    }
    public int getDataSampleByteLength() {
        return dataSampleByteLength;
    }
    public int getMAX_SAMPLE_RATE() { return MAX_SAMPLE_RATE; }
    public int getMIN_SAMPLE_RATE() { return MIN_SAMPLE_RATE; }
    //setters
    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }
    public void setSensorID(int sensorID) {
        this.sensorID = sensorID;
    }
    public void setDataSampleByteLength(int dataSampleByteLength) {
        this.dataSampleByteLength = dataSampleByteLength;
    }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate;}
    public void setMAX_SAMPLE_RATE(int MAX_SAMPLE_RATE) { this.MAX_SAMPLE_RATE = MAX_SAMPLE_RATE; }
    public void setMIN_SAMPLE_RATE(int MIN_SAMPLE_RATE) { this.MIN_SAMPLE_RATE = MIN_SAMPLE_RATE; }
    public boolean isConnected() {
        return isConnected;
    }
    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}