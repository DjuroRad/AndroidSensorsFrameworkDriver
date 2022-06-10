package bluetooth_connection_manager;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnectionManager {

    private volatile boolean connectionEstablished = false;
    private StreamConnectionNotifier streamConnectionNotifier = null;
    private StreamConnection streamConnection = null;
    private RemoteDevice remoteDevice = null;
    private String serverName = null;
    private UUID serverUUID = null;

    private OutputStream outputStream = null;
    private InputStream inputStream = null;


    /**
     * @return output stream where response data will be written, null if connection wasn't established
     * */
    public OutputStream getOutputStream(){
        return this.outputStream;
    }

    /**
     * @return input stream where request data will be received, null if connection wasn't established
     * */
    public InputStream getInputStream(){
        return this.inputStream;
    }

    /**
     * if remote device is connected/paired returns true and false otherwise
     * */
    public boolean isConnected() {
        return connectionEstablished;
    }
    /**
     * opens a server and waits for a device to connect to it
     * */
    public void acceptRemoteDevice(){
        initializeServer();
        openConnection();//blocks waiting for client to connect to the open port
        openStreams();
    }

    /**
     * closes the stream connection it was using beforehand
     * */
    public void disconnect(){
        try {
            streamConnectionNotifier.close();
            connectionEstablished = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * opens input and output stream from the established stream connection
     * */
    private void openStreams() {
        try {
            if( streamConnection != null ){
                inputStream = streamConnection.openInputStream();
                outputStream =streamConnection.openOutputStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * initializes necessary variables like UUID and Server Name
     * */
    private void initializeServer(){
        //initialize required elements
        this.serverUUID = new UUID("1101", true);
        this.serverName = "btspp://localhost:" + serverUUID +";name=Sample SPP Server";
    }

    /**
     * opens the streamConnection and waits for client to connect
     * */
    private void openConnection(){
        try {
            streamConnectionNotifier = (StreamConnectionNotifier) Connector.open( serverName );
            streamConnection = streamConnectionNotifier.acceptAndOpen();//waits for client to connect
            remoteDevice = RemoteDevice.getRemoteDevice(streamConnection);//get device that connected
        } catch (IOException e) {
            e.printStackTrace();
        }

        connectionEstablished = true;
    }

}
