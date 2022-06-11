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

        this.availableSensorsSampleRate = new HashMap<>();

        n_bytes_total = 0;
        for( SensorEntry sensor: availableSensors ) {
            n_bytes_total += sensor.getDataSampleByteLength();
            availableSensorsSampleRate.put(sensor.getSensorID(), new SampleRateTracker(generalSampleRate));
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
            for( SensorEntry sensor : availableSensors ){
                if( sensor.isConnected() && availableSensorsSampleRate.get( sensor.getSensorID() ).isAwake() )//write data if sensor is CONNECTED and AWAKE!
                    writeSensorData(sensor, all_sensors_raw_data, offset_all_raw_data);

                offset_all_raw_data += sensor.getDataSampleByteLength();//always move the offset!
            }

            int sleepTime = lowestSleepTimeLeft();
            updateSleepTime(sleepTime);

            try {
//                System.out.println(" SAMPLE RATE SLEEPING " + sleepTime);
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
     * 4. write data sample
     * */
    private void writeSensorData(SensorEntry sensor, byte[] all_sensors_raw_data, int offset_all_raw_data) {
        //write SENSOR TYPE  --- removed as far as I can see
        //write sensor ID ( just id actually )
        //write # bytes as additional data

        byte[] data_sample = new byte[sensor.getDataSampleByteLength()];

        //data sample bytes
        for( int i = 0; i<sensor.getDataSampleByteLength(); ++i )
            data_sample[i] = all_sensors_raw_data[i+offset_all_raw_data];

        //id bytes
        byte[] sensorIdBytes = ByteBuffer.allocate(4).putInt(sensor.getSensorID()).array();

        ResponsePackage responsePackage = new ResponsePackage(Response.READING_SENSOR_DATA, sensorIdBytes, data_sample);//write response

        responsePackage.sendResponse(clientOutputStream);
    }

    public void configureSensor(int sensorID, int sampleRate){
        if( sensorID == -1 ){//update all sensors' sample rate
            for( Map.Entry<Integer, SampleRateTracker> entry : availableSensorsSampleRate.entrySet() )
                availableSensorsSampleRate.replace( entry.getKey(), new SampleRateTracker(sampleRate) );
        }else//update single sensor's sample rate
            availableSensorsSampleRate.replace( sensorID, new SampleRateTracker(sampleRate) );
    }
    public void disconnect(){
        writingData = false;
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


////            byte[] extendedResponse = new byte[ResponsePackage.RESPONSE_BODY_SIZE + ResponsePackage.RESPONSE_HEADER_SIZE + nBytes];
////
////            for( int i = 0; i<buffer.length; ++i )
////                extendedResponse[ResponsePackage.RESPONSE_BODY_SIZE + ResponsePackage.RESPONSE_HEADER_SIZE + i] = buffer[i];
////
////            extendedResponse[0] = responsePackage.getResponseTypeByte();
////            for( int i = 0; i<ResponsePackage.RESPONSE_BODY_SIZE; ++i )
////                extendedResponse[i+1] = responsePackage.getResponseBody()[i];
//
////
////            clientOutputStream.write(extendedResponse);
////            clientOutputStream.flush();//wait for data to be read and only after that get new data from the stream
//        } catch (IOException e) {
//            e.printStackTrace();
//        }