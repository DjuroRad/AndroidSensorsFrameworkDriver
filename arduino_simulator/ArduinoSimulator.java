package arduino_simulator;

import arduino_simulator.sensors.Sensor;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Usage recommendations:
 * 1. Configure Arduino ( set appropriate Sensor, baud rate and connect )
 * 2. Start Arduino {arduinoInstance.start()} making run simulator on a separate thread
 * */
public class ArduinoSimulator extends Thread implements ArduinoSimulatorInterface{
    private static final String TAG = "ArduinoSimulator";
    Logger logger = Logger.getLogger(TAG);

    /**
     * sensor developer using framework should map values 0-5V of analog output to 0-1023 numeric before sending them if data is analog
     * */
    public static final int ANALOG_UPPER_LIMIT = 1023;
    public static final int ANALOG_LOWER_LIMIT = 0;
    private static final int MAX_ARRAY_LIST_SIZE = 1000;

    private int baud = -1;
    private PipedInputStream pipedInputStream = null;//sensor input stream
    private PipedOutputStream pipedOutputStream = null;//sensor output stream
//    private Sensor sensor = null;
    private List<Sensor> sensors = null;

    volatile private LinkedList<LinkedList<byte[]>> sensorDataList = new LinkedList<LinkedList<byte[]>>();
    private boolean simulatorRunning = true;
    private boolean arduinoConnected = false;

    public ArduinoSimulator(List<Sensor> sensors){
        this( sensors, 9600);
    }

    public ArduinoSimulator(List<Sensor> sensors, int baud){
        for(int i = 0; i<sensors.size(); ++i)
            sensorDataList.add(new LinkedList<byte[]>());

        this.sensors = sensors;
        this.baud = baud;

        pipedInputStream = new PipedInputStream();
        try {
            pipedOutputStream = new PipedOutputStream(pipedInputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    int iter = 0;
    /**
     * will continuously write data to a set output stream
     * */
    @Override
    public void run() {
        super.run();

        if( !arduinoConnected ){
            logger.setLevel(Level.WARNING);
            logger.warning("Arduino was not connected before it is started");
        }

        while( simulatorRunning ) {

            //find how long is all the data, add it to the list of new data
            int all_data_size = 0;
            ArrayList<byte[]> newDataList = new ArrayList<byte[]>();
            for( int i = 0; i<sensors.size(); ++i ) {
                byte[] newData = sensors.get(i).readDataValue();//gives a random number for light sensor for example, this will depend on implementation
                newDataList.add(newData);
                all_data_size += newData.length;
            }

            //add all data to that previous list
            //set raw data of all sensors registered
            byte[] all_raw_data = new byte[all_data_size];
            int offset = 0;
            for( int i =0; i<sensors.size(); ++i ){
                addNewData(newDataList.get(i), i);
                for( int j = 0; j<newDataList.get(i).length; ++j )
                    all_raw_data[offset + j] = newDataList.get(i)[j];

                offset += newDataList.get(i).length;
            }

            writeToOutputStream( all_raw_data );

            ++iter;

        }

        closeStreams();
    }

    /**
     * adds new data with respect to limit, using linked list to keep these times constant
     * */
    private void addNewData(byte[] newData, int i) {
        if( sensorDataList.get(i).size() == MAX_ARRAY_LIST_SIZE )
            sensorDataList.get(i).removeFirst();

        sensorDataList.get(i).addLast(newData);
    }


    private void writeToOutputStream(byte[] newData) {
        try {
            pipedOutputStream.write(newData);
        } catch (IOException e) {
            if( e.getMessage().equals("Read end dead")) {
                //means client disconnected
                disconnect();
            }
            else
                e.printStackTrace();
        }
    }

    /**
     * @see ArduinoSimulatorInterface
     * */
    @Override
    public InputStream connectViaBluetooth() {
        try {
            sleep(2000);
            arduinoConnected = true;
            return pipedInputStream;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @see ArduinoSimulatorInterface
     * */
    @Override
    public int serialBegin(int baud) {
        this.baud = baud;
        return this.baud;
    }

    /**
     * @see ArduinoSimulatorInterface
     * */
    @Override
    public byte[] getLastReading() {//get all sensor's last data one by one && return it
        ArrayList<Byte> allData = new ArrayList<Byte>();

        int n_bytes = 0;//calculate how many bytes array will have
        ArrayList<byte[]> dataReadings = new ArrayList<>();
        for( Sensor sensor: sensors ) {
            byte[] newDataValue = sensor.readDataValue();
            dataReadings.add(newDataValue);
            n_bytes += newDataValue.length;
        }

        byte[] dataToSend = new byte[n_bytes];
        int offset = 0;
        for( byte[] dataReading: dataReadings){
            System.arraycopy(dataReading, 0, dataToSend, offset, dataReading.length);
            offset += dataReading.length;
        }

        return dataToSend;
    }

    /**
     * when arduino is disconnected it closes the streams that were used for input/output
     * */
    private void closeStreams() {

        try {
            pipedInputStream.close();
            pipedOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see ArduinoSimulatorInterface
     * */
    @Override
    public void disconnect() {
        simulatorRunning = false;
    }
}
