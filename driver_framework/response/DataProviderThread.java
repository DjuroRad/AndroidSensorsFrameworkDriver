package driver_framework.response;

import arduino_simulator.sensors.SensorEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataProviderThread extends Thread{

    private volatile boolean writingData = true;
    private OutputStream clientOutputStream = null;
    private InputStream sensorInputStream = null;
    List<SensorEntry> availableSensors;//needed for sending data

    byte[][] prevRawData;//track previous raw data samples
    private HashMap<Integer, SampleRateTracker> availableSensorsSampleRate;
    private int n_bytes_total;

    private boolean readingStopped = false;

    public void setGeneralSampleRate(int generalSampleRate) {
        this.generalSampleRate = generalSampleRate;
    }

    private int generalSampleRate = 0;//taking sample on each generalSampleRate, expressed in milliseconds

    public DataProviderThread(OutputStream clientOutputStream, InputStream sensorInputStream, List<SensorEntry> availableSensors, int generalSampleRate) {
        this.sensorInputStream = sensorInputStream;
        this.clientOutputStream = clientOutputStream;
        this.availableSensors = availableSensors;
        this.generalSampleRate = generalSampleRate;

        prevRawData = new byte[availableSensors.size()][0];
        this.availableSensorsSampleRate = new HashMap<>();

        n_bytes_total = 0;
        for( SensorEntry sensor: availableSensors ) {
            n_bytes_total += sensor.getDataSampleByteLength();
            availableSensorsSampleRate.put(sensor.getSensorID(), new SampleRateTracker(sensor.getSampleRate()));
        }
    }

    /**
     * sets number of bytes that will be written to client / read from sensor
     * */
    public void setAvailableSensors(List<SensorEntry> availableSensors){ this.availableSensors = availableSensors; }

    @Override
    public void run() {
        super.run();

        while( writingData ){

            byte[] all_sensors_raw_data = new byte[n_bytes_total];//get all data from the sensors
            try {
                if (sensorInputStream.read(all_sensors_raw_data) == -1) throw new IOException("While reading data from external sensor error occurred");//get sensor's sample data
            } catch (IOException e) {
                e.printStackTrace();
            }

            int offset_all_raw_data = 0;
            int i = 0;
            for( SensorEntry sensor : availableSensors ){
                if( sensor.isConnected() && availableSensorsSampleRate.get( sensor.getSensorID() ).isAwake() ) {//write data if sensor is CONNECTED and AWAKE!
                    writeSensorData(sensor, i, all_sensors_raw_data, offset_all_raw_data);
                }
                ++i;
                offset_all_raw_data += sensor.getDataSampleByteLength();//always move the offset!
            }

            int sleepTime = lowestSleepTimeLeft();
            updateSleepTime(sleepTime);

            try {
                sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if( readingStopped ) {//reading stopped, wait for notify to continue
                synchronized (this){
                    try {
                        wait();
                        readingStopped = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 1. get external sensor's data sample
     * 2. write response type ---> READING_SENSOR_DATA
     * 3. write sensor id
     * -> if data not changed and precision is set to be imprecise -> don't write it, just return
     * 4. write raw data sample
     * 5. if formatted, write formatted string's length
     * 6. if formatted, write formatted string
     * */

    private void writeSensorData(SensorEntry sensor, int sensor_i, byte[] all_sensors_raw_data, int offset_all_raw_data) {

        Response responseType = Response.READING_SENSOR_DATA;
        byte[] sensorIdBytes = ByteBuffer.allocate(4).putInt(sensor.getSensorID()).array();//write sensor ID ( just id actually )
        byte[] raw_data_sample = new byte[sensor.getDataSampleByteLength()];//write # bytes as additional data of raw data

        for( int i = 0; i<sensor.getDataSampleByteLength(); ++i )//extract raw data of given sensor from all sensors' raw data
            raw_data_sample[i] = all_sensors_raw_data[i+offset_all_raw_data];

        if(!sensor.dataChanged( raw_data_sample, prevRawData[sensor_i])) return;//if there is no data change ( depends on precision also, check the implementation
        else setPrevRawData(raw_data_sample, sensor_i);


        byte[] rawLengthString = new byte[0];
        byte[] rawString = new byte[0];


        if(sensor.isFormatted()){//get formatted data if sensor is configured for formatting.
            String formattedDataAsString = sensor.formatRawData(raw_data_sample).toJSONString();
            rawLengthString = ByteBuffer.allocate(4).putInt(formattedDataAsString.length()).array();//write length of formatted data
            rawString = formattedDataAsString.getBytes();//write formatted data's string
            responseType = Response.READING_SENSOR_DATA_FORMATTED;//update response!
        }

        //sets up the additional data
        int n_total = raw_data_sample.length + rawLengthString.length + rawString.length;
        byte[] additional_data = new byte[n_total];

        int i = 0;
        for( ; i<raw_data_sample.length; ++i )//copy raw data
            additional_data[i] = raw_data_sample[i];

        for( ; i<raw_data_sample.length + rawLengthString.length; ++i )//copy formatted string's length
            additional_data[i] = rawLengthString[i-raw_data_sample.length];

        for( ; i<rawLengthString.length + rawLengthString.length + rawString.length; ++i )//copy formatted string
            additional_data[i] = rawString[i-(raw_data_sample.length + rawLengthString.length)];

        ResponsePackage responsePackage = new ResponsePackage(responseType, sensorIdBytes, additional_data);//write response
        responsePackage.sendResponse(clientOutputStream);
    }

    private void setPrevRawData(byte[] raw_data_sample, int sensor_i) {
        prevRawData[sensor_i] = new byte[raw_data_sample.length];

        for( int i = 0; i<raw_data_sample.length; ++i )
            prevRawData[sensor_i][i] = raw_data_sample[i];

    }

    public void disconnect(){
        writingData = false;
        readingStopped = false;
    }

    public void stopReading(){
        readingStopped = true;
    }

    private static class SampleRateTracker{
        int sampleRate;
        int slept;

        public SampleRateTracker(int sampleRate) {
            this.sampleRate = sampleRate;
            this.slept = 0;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public void updateSleepTime(int amountSlept) {
            if ( this.slept + amountSlept >= sampleRate )
                this.slept = 0;
            else
                this.slept = this.slept + amountSlept;
        }

        public boolean isSleeping(){
            return !isAwake() && slept < sampleRate;
        }

        public boolean isAwake(){
            return this.slept == 0;
        }

        public void goToSleep(){
            this.slept = 0;
        }

        public int sleepTimeLeft(){
            return sampleRate - slept;
        }

    }

    private int lowestSleepTimeLeft(){

        int lowestSleepTime = availableSensorsSampleRate.entrySet().iterator().next().getValue().sleepTimeLeft();

        for( Map.Entry<Integer, SampleRateTracker> entry : availableSensorsSampleRate.entrySet() ) {
            if( entry.getValue().sleepTimeLeft() < lowestSleepTime )
                lowestSleepTime = entry.getValue().sleepTimeLeft();
        }

        return lowestSleepTime;
    }

    private void updateSleepTime(int timeSlept){
        for( Map.Entry<Integer, SampleRateTracker> entry : availableSensorsSampleRate.entrySet() )
            entry.getValue().updateSleepTime(timeSlept);
    }

    public void sensorConnected(int sensorID) {
        for( SensorEntry sensor: availableSensors ){
            if( sensor.getSensorID() == sensorID ) {
                sensor.setConnected(true);
                break;
            }
        }
    }

    public void sensorDisconnected(int sensorID) {
        for( SensorEntry sensor: availableSensors ){
            if( sensor.getSensorID() == sensorID ) {
                sensor.setConnected(false);
                break;
            }
        }
    }
}