package driver_framework.response;

import arduino_simulator.SensorType;
import arduino_simulator.sensors.SensorEntry;
import driver_framework.request.Request;
import driver_framework.request.RequestObserver;
import driver_framework.request.RequestPackage;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static arduino_simulator.sensors.SensorEntry.SENSOR_ENTRY_BYTE_LENGTH;


/**
 * observer of requests.
 * When a new request arrives, it processes it through one of the implemented methods
 *
 * Execution cycle:
 *                  1. gets request from request's server
 *                  2. sends a response accordingly
 *
 * It also preserves order of requests making sure invalid behaviour doesn't go unnoticed by sending INVALID_REQUEST response
 *
 * Order of execution(detailed) respect to Request/Response :
 *      1. CONNECT_SENSOR_Y / CONNECT_SENSOR_N <- CONNECT_SENSOR
 *      2. CONFIGURE_Y / CONFIGURE_N <- CONFIGURE          // Note: CONFIGURE request is required for custom sensors, and optional for present sensors
 *      3. IS_CONNECTED_Y / IS_CONNECTED_N <- IS_CONNECTED          //Note: optionally called, won't affect request order
 *      4. START_READ_Y / START_READ_N <- START_READ
 *      6. STOP_READ_Y / STOP_READ_N <- STOP_READ
 *      5. READING_SENSOR_DATA <- sent after START_READ_Y
 *      7. INVALID_REQUEST <- if current request does not follow framework rules
 *      8. DISCONNECT -> no response, disconnects, terminates
 *
 * @see Request
 * @see Response
 * */
public class ResponseManager implements RequestObserver{


    private DataProviderThread dataProviderThread = null; //thread is used to write sensor data to client's output stream
    private OutputStream outputStreamClient = null; // output stream is used to write sensor data to client / Android developer
    private InputStream inputStreamSensor = null; // input stream is used to get sensor data

    private RequestPackage currentRequestPackage = null;
    private Response previousResponse = null;
    private ResponsePackage currentResponsePackage = null;

    private volatile boolean clientConnected = true;
    private boolean isSensorConnected = false;//this field is needed for Request.CONNECT_SENSOR request

    private int generalSampleRateSensors = -1;

    List<SensorEntry> availableSensors = new ArrayList<>();
    public ResponseManager(InputStream inputStreamSensor, OutputStream outputStreamClient, List<SensorEntry> availableSensors ){
        this.outputStreamClient = outputStreamClient;
        this.inputStreamSensor = inputStreamSensor;
        this.availableSensors = availableSensors;
    }
    /**
     * observer's method is triggered here as soon as data arrives
     * */
    @Override
    public void onRequestArrived(RequestPackage currentRequestPackage) {
        this.currentRequestPackage = currentRequestPackage;

        processNewRequest();
    }

    @Override
    public void onSubjectFinished() {
        this.clientConnected = false;
        this.isSensorConnected = false;
        if( dataProviderThread != null )
            dataProviderThread.disconnect();
    }

    /**
     * waits for data provider thread to disconnect ( stop reading ) !
     * */
    private void waitStopReading(){
        if( dataProviderThread != null )
            dataProviderThread.stopReading();//data provider suspends itself
    }

    private void disconnectReading(){
        if( dataProviderThread != null ){
            dataProviderThread.disconnect();//stops writing data, disconnects from tied input stream
            try {
                dataProviderThread.join();//wait for disconnect confirmation from the thread
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * Case 1: previous response was READING_SENSOR_DATA
     * Case 2: wasn't reading data previously -> possible requests are:
     *              CONNECT_SENSOR -> independent
     *              IS_CONNECTED -> independent
     *              START_READ -> dependent on CONNECT_SENSOR
     *              STOP_READ -> dependent on previousResponse = READING_SENSOR_DATA
     *              DISCONNECT -> independent
     *              CONFIGURE -> dependent on CONNECT_SENSOR
     *              -> if framework conventions are not satisfied -> INVALID_REQUEST is the response
     * */
    private void processNewRequest() {
        if( previousResponse != Response.START_READ_Y ){
            switch (currentRequestPackage.getRequestType()){
                case CONNECT -> connectResponse();
                case CONNECT_SENSOR -> connectSensorResponse();
                case DISCONNECT_SENSOR -> disconnectSensorResponse();
                case IS_CONNECTED -> isConnectedResponse();
                case START_READ -> startReadResponse();
                case STOP_READ -> stopReadResponse();
                case DISCONNECT -> {
                    disconnectResponse();
                    return;
                }
                case CONFIGURE -> configureResponse();
                default -> sendInvalidRequestResponse();
            }
        }else if( currentRequestPackage.getRequestType() == Request.STOP_READ ){
            stopReadResponse();
        }else if( currentRequestPackage.getRequestType() == Request.DISCONNECT ){
            disconnectResponse();
            return;
        }


        this.previousResponse = currentResponsePackage.getResponseType();
        currentResponsePackage.sendResponse(outputStreamClient);

        if( this.currentResponsePackage.responseType == Response.START_READ_Y )
            startReadingThread();

        this.currentResponsePackage = null;
    }

    /**
     * if there are no sensors available, sends CONNECT_N
     * otherwise:
     *      header -> CONNECT_Y
     *      body -> # of sensors ( integer )
     *      additional data -> each sensor's data.
     * */
    private void connectResponse() {
        if( availableSensors.size() == 0)
            currentResponsePackage = new ResponsePackage(Response.CONNECT_N);
        else{

            //set up body
            byte[] sizeBytes = ByteBuffer.allocate(4).putInt(availableSensors.size()).array();

            //set up additional data
            byte[] additionalData = new byte[availableSensors.size() * SENSOR_ENTRY_BYTE_LENGTH];//+ 4 + 4 - 2 additional integer values for MIN and MAX
            for( int i = 0; i<availableSensors.size(); ++i ){//set sensor type, sensor id, n length
                SensorType sensorType = availableSensors.get(i).getSensorType();
                int sensorID = availableSensors.get(i).getSensorID();
                int dataSampleLength = availableSensors.get(i).getDataSampleByteLength();

                byte sensorTypeByte = sensorType.getValue();
                byte[] sensorIdBytes = ByteBuffer.allocate(4).putInt(sensorID).array();
                byte[] dataSampleLengthBytes = ByteBuffer.allocate(4).putInt(dataSampleLength).array();

                additionalData[i*SENSOR_ENTRY_BYTE_LENGTH] = sensorTypeByte;
                for( int j = 1; j<5; ++j )
                    additionalData[i*SENSOR_ENTRY_BYTE_LENGTH+j] = sensorIdBytes[j-1];//send sensor's id

                for( int j = 5; j<9; ++j )
                    additionalData[i*SENSOR_ENTRY_BYTE_LENGTH+j] = dataSampleLengthBytes[j-5];//send length of its byte

                byte[] minValueBytes = ByteBuffer.allocate(4).putInt(availableSensors.get(i).getMinValue()).array();
                for( int j = 9; j<13; ++j )//send minimum value
                    additionalData[i*SENSOR_ENTRY_BYTE_LENGTH+j] = minValueBytes[j-9];//send sensor's id

                byte[] maxValueBytes = ByteBuffer.allocate(4).putInt(availableSensors.get(i).getMaxValue()).array();
                for( int j = 13; j<SENSOR_ENTRY_BYTE_LENGTH; ++j )//send maximum value
                    additionalData[i*SENSOR_ENTRY_BYTE_LENGTH+j] = maxValueBytes[j-13];//send length of its byte
            }
            currentResponsePackage = new ResponsePackage(Response.CONNECT_Y, sizeBytes, additionalData);

            isSensorConnected = true;

            //instantiate data transfer thread with provided sample rate
            byte[] generalSampleRateBytes = currentRequestPackage.getRequestBody();
            int generalSampleRate = new BigInteger(generalSampleRateBytes).intValue();
            this.generalSampleRateSensors = generalSampleRate;
        }
    }

    /**
     * starts reading sensor's data and sending it to the client
     * */
    private void startReadingThread() {
        if( dataProviderThread.getState() == Thread.State.WAITING ) {//if it was suspended, continue
                synchronized (dataProviderThread) {
                    dataProviderThread.notify();
                }
        }
        else//start for the first time
            dataProviderThread.start();
    }


    /**
     * Request = | CONNECT_SENSOR | SENSOR_TYPE (1 byte) - - - |
     * this response is given if Sensor_type is registered / present on the board. This information is explicitly specified beforehand through availableSensors given by driver developer
     * Response = | CONNECT_SENSOR_Y | sensorID |
     * Response = | CONNECT_SENSOR_N | sensorID |
     * */
    private void connectSensorResponse() {//check if given sensor type is present
        //independent response
        int sensorID = new BigInteger(currentRequestPackage.getRequestBody()).intValue();
        Response responseType = null;

        responseType = (Response.CONNECT_SENSOR_N);//assume not present

        isSensorConnected = false;
        for( SensorEntry sensor: availableSensors ){//change assumption if you find a contradiction
            if (sensorID == sensor.getSensorID()) {
                responseType = Response.CONNECT_SENSOR_Y;//form a package and send it
                sensor.setConnected(true);
//                dataProviderThread.sensorConnected(sensorID);
                isSensorConnected = true;
                break;
            }
        }

        currentResponsePackage = new ResponsePackage(responseType, currentRequestPackage.getRequestBody());
    }

    private static SensorType getSensorTypeFromRequestPackage( RequestPackage requestPackage ){
        byte sensorTypeByte = requestPackage.getRequestBody()[0];
        return SensorType.getSensorTypeFromByte(sensorTypeByte);
    }

    private void sendInvalidRequestResponse() {
        currentResponsePackage = new ResponsePackage(Response.INVALID_REQUEST);
    }

    private void configureResponse() {//sets sample rate for the given sensor id
       if( !isSensorConnected ) { currentResponsePackage = new ResponsePackage(Response.CONFIGURE_N); return; }//performs configuration only if initial handshake has been established

       currentResponsePackage = new ResponsePackage(Response.CONFIGURE_Y, currentRequestPackage.getRequestBody());
       byte[] sensorIdBytes = currentRequestPackage.getRequestBody();
       int sensorID = new BigInteger(sensorIdBytes).intValue();//get sensor id

//       byte[] sampleRateConfiguration = currentRequestPackage.getAdditionalData();
//       int newSampleRate = new BigInteger(sampleRateConfiguration).intValue();//get sample rate

       boolean id_exists = false;//check if given id requested for configuration exists
       for(SensorEntry sensor : availableSensors){
           if( sensor.getSensorID() == sensorID ) {
               id_exists = true;
               sensor.configureSensor( currentRequestPackage.getInputStream() );
//               dataProviderThread.configureSensor(sensorID, sensor.getSampleRate());//update sample rate in data provider
               break;
           }
       }

       if( id_exists || sensorID == -1 )//return id and response is Y or N
           currentResponsePackage = new ResponsePackage(Response.CONFIGURE_Y, currentRequestPackage.getRequestBody());
       else
           currentResponsePackage = new ResponsePackage(Response.CONFIGURE_N, currentRequestPackage.getRequestBody());

    }




    private void disconnectResponse(){
        synchronized (dataProviderThread){
            if( dataProviderThread.getState() == Thread.State.WAITING )
                dataProviderThread.notify();//ensures thread will quit
        }

        dataProviderThread.disconnect();
    }

    private void disconnectSensorResponse() {

        //independent response
        int sensorID = new BigInteger(currentRequestPackage.getRequestBody()).intValue();
        Response responseType = null;

        responseType = (Response.DISCONNECT_SENSOR_N);//assume not present

        for( SensorEntry sensor: availableSensors ){//change assumption if you find a contradiction
            if (sensorID == sensor.getSensorID()) {
                responseType = Response.DISCONNECT_SENSOR_Y;//form a package and send it
                sensor.setConnected(false);
                dataProviderThread.sensorDisconnected(sensorID);
                break;
            }
        }

        currentResponsePackage = new ResponsePackage(responseType, currentRequestPackage.getRequestBody());
    }

    //1. check if sensor with specified id exists
    //2. if does exist, check if the sensor is connected
    private void isConnectedResponse() {//checks if certain sensor id is present
        //independent response
        Response responseType = null;

        //check if requested sensor ID is connected
        boolean isSensorConnected = false;
        int sensorID = new BigInteger(currentRequestPackage.getRequestBody()).intValue();

        for( SensorEntry sensor: availableSensors ) {
            if (sensorID == sensor.getSensorID() && sensor.isConnected()) {//if sensor's id is found and sensor pointed to by the given id is connected => return that a sensor is connected
                isSensorConnected = true;
                break;
            }
        }
        //form a package and send it
        //send that sensor is connected
        responseType  = isSensorConnected ? Response.IS_CONNECTED_Y : Response.IS_CONNECTED_N;//return that sensor is not connected alongside with the provided ID, body is ID

        currentResponsePackage = new ResponsePackage(responseType, currentRequestPackage.getRequestBody());//returns the requested ID
    }

    /**
     * starting to read sensor's data
     * Case 1: wasn't reading before -> inform client that reading is starting and inform about the number of bytes you want to send
     * Case 2: was reading before -> continue reading
     *
     * | START_READING_Y | NUM_OF_BYTES |
     * */
    private void startReadResponse() {//if handshake done you can start reading now
        if( isSensorConnected ){
            currentResponsePackage = new ResponsePackage( Response.START_READ_Y);
            dataProviderThread = new DataProviderThread(outputStreamClient, inputStreamSensor, availableSensors, generalSampleRateSensors);
        }else //if sensor is not / has not previously been connected send an invalid request response
            currentResponsePackage = new ResponsePackage(Response.START_READ_N);
    }

    /**
     * stops reading of sensor data / terminates the thread that was responsible for reading data and sending it to client's output stream
     * Case 1: was reading and now stops reading
     * Case 2: wasn't reading -> response is invalid -> STOP_READ_N since it was not reading beforehand
     * */
    private void stopReadResponse() {
        if( previousResponse == Response.START_READ_Y ) {
            waitStopReading();
            currentResponsePackage = new ResponsePackage(Response.STOP_READ_Y);
        }
        else
            currentResponsePackage = new ResponsePackage(Response.STOP_READ_N);
    }
}
