package driver_framework;

import driver_framework.response.Response;

import java.io.IOException;
import java.io.OutputStream;

public interface FrameworkUtils {

    public static void writeAndHandleException(OutputStream outputStream, byte[] data){
        try {
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendResponse(OutputStream outputStreamClient, Response response){
        byte[] responseAsByteArray = {response.getValue()};
        writeAndHandleException(outputStreamClient, responseAsByteArray);
    }

    public static void waitServerTermination(Thread thread){
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
