package driver_framework;

import arduino_simulator.sensors.SensorEntry;
import driver_framework.request.RequestManagerThread;
import driver_framework.request.RequestObserver;
import driver_framework.response.ResponseManager;

import java.io.*;
import java.util.List;

/**
 * instance of this class is to be used by driver developer in order to start providing communicatino with Android device
 * */
public class SensorServerManager extends Thread{

   RequestManagerThread requestManagerThread = null;
   ResponseManager responseManager = null;

    public SensorServerManager(InputStream sensorInputStream, OutputStream clientOutputStream, InputStream serverInputStream, List<SensorEntry> availableSensors){
        responseManager = new ResponseManager(sensorInputStream, clientOutputStream, availableSensors);
        requestManagerThread = new RequestManagerThread(serverInputStream, (RequestObserver) responseManager);
    }

    /**
     * after everything is properly set up, call start() on SensorServerManager instance
     * executing it waits for requests and sends responses accordingly
     * */
    @Override
    public void run() {
        super.run();
        requestManagerThread.start();//will continuously process incoming requests until incoming request is Request.DISCONNECT

        join(requestManagerThread);//waits for disconnect request that will terminate this thread

    }

    private void join(Thread thread){
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
