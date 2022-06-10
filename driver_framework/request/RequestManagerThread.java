package driver_framework.request;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * This class continuously reads data from an input stream
 * It checks if request abides to framework rules by checking the previous request and some additional parameters
 * For example: START_READ request will not be executed if previous request wasn't CONNECT_SENSOR
 *              or STOP_READ wouldn't be possible if previous request wasn't START_READ
 *
 * This class also manages RequestObserver's behaviour according to request sent
 *
 * Order of appropriate Request execution:
 *      1. CONNECT_SENSOR
 *      2. CONFIGURE (optional) - (required) -> for CUSTOM sensors not present within the framework
 *      3. IS_CONNECTED (optional) - advised before starting to read
 *      4. START_READ
 *      5. STOP_READ
 *      6. DISCONNECT
 *
 *@see Request
 *@see RequestObserver
 * */
public class RequestManagerThread extends Thread{

    private InputStream inputStream;
    private RequestObserver requestObserver = null;//informs the observer about oncoming requests
    private boolean processingRequests = true;

    private RequestPackage currentRequestPackage = new RequestPackage();
    private static final String TAG = "ServerRequestReceiverTh";
    private static final Logger LOGGER = Logger.getLogger( RequestManagerThread.class.getName() );



    public RequestManagerThread(InputStream inputStream, RequestObserver requestObserver){
        this.inputStream = inputStream;
        this.requestObserver = requestObserver;
    }

    @Override
    public void run() {
        super.run();

        while(processingRequests){
            try{
                readNextRequest();
                processRequest();
            }catch (IOException ioe){
                System.out.println(TAG + ": readNextRequest() -> " + ioe.getMessage());
            }
        }

    }

    /**
     * gets next request from client
     * @throws IOException if request invalid and if input stream returned -1 indicating the end of file
     * @see Request for possible requests
     * */
    private void readNextRequest() throws IOException {
        currentRequestPackage.getRequestPackage(inputStream);
    }

    /**
     * Performs actions according to the request received from the client
     * */
    private void processRequest() {
        switch (currentRequestPackage.getRequestType()){
            case DISCONNECT:
                requestObserver.onSubjectFinished();
                processingRequests = false;
            case CONNECT_SENSOR:
            case DISCONNECT_SENSOR:
            case IS_CONNECTED:
            case START_READ:
            case STOP_READ:
            case CONFIGURE:
            case CONNECT:
                requestObserver.onRequestArrived(currentRequestPackage);
        }
    }

}
