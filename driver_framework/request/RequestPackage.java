package driver_framework.request;

import java.io.IOException;
import java.io.InputStream;

public class RequestPackage {
    private byte[] requestBody = new byte[REQUEST_BODY_SIZE];
    private byte requestTypeByte;//used to represent request header
    private Request requestType = null;

    private InputStream inputStream;
    public void getRequestPackage(InputStream inputStream) {
        this.inputStream = inputStream;
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
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InputStream getInputStream() {
        return inputStream;
    }
    public byte[] getRequestBody() {
        return requestBody;
    }
    public byte getRequestTypeByte() {
        return requestTypeByte;
    }
    public Request getRequestType() {
        return requestType;
    }
    public static final int REQUEST_BODY_SIZE = 4;
    public static final int REQUEST_HEADER_SIZE = 1;
}
