package driver_framework.request;

import java.io.IOException;
import java.io.InputStream;

public class RequestPackage {
    private byte[] requestBody = new byte[REQUEST_BODY_SIZE];
    private byte requestTypeByte;//used to represent request header
    private byte[] requestAdditionalData = new byte[CONFIGURE_ADDITIONAL_DATA];
    private Request requestType = null;

    public void getRequestPackage(InputStream inputStream) {
        try {
            int requestTypeInt = inputStream.read();
            if( requestTypeInt == -1 )
                throw new IOException("RequestType reading: Data read from input stream is -1");
            else {
                this.requestTypeByte = (byte) requestTypeInt;
                this.requestType = Request.getRequestFromByte(requestTypeByte);
                int requestBodyLength = inputStream.read(requestBody,0,REQUEST_BODY_SIZE);
                if( requestBodyLength != 4 )
                    throw new IOException("RequestType reading: Data read from input stream is -1");
                getAdditionalData(inputStream);//gets additional data if it exists for a given command
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static final int CONFIGURE_ADDITIONAL_DATA = 4;//integer pointing out the sample rate in ms

    private void getAdditionalData(InputStream inputStream){
        switch (requestType){
            case CONFIGURE:
                try {

                    int readBytes = inputStream.read(requestAdditionalData,0,CONFIGURE_ADDITIONAL_DATA);
                    if( readBytes == -1 )
                        throw new IOException("Reading additional data for CONFIGURE request ended up returning -1\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    public byte[] getRequestBody() {
        return requestBody;
    }
    public byte[] getAdditionalData(){ return requestAdditionalData; }
    public byte getRequestTypeByte() {
        return requestTypeByte;
    }

    public Request getRequestType() {
        return requestType;
    }

    public static final int REQUEST_BODY_SIZE = 4;
    public static final int REQUEST_HEADER_SIZE = 1;
}
