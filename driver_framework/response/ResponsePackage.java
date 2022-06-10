package driver_framework.response;

import java.io.IOException;
import java.io.OutputStream;

public class ResponsePackage {
    private byte[] responseBody = new byte[RESPONSE_BODY_SIZE];
    private byte responseTypeByte;//used to represent response header
    private byte[] additionalData;

    Response responseType = null;
    public void sendResponse(OutputStream outputStream) {
        try {
            byte[] responseInBytes = new byte[RESPONSE_BODY_SIZE + RESPONSE_HEADER_SIZE];
            responseInBytes[0] = responseTypeByte;
            for( int i = 0; i<RESPONSE_BODY_SIZE; ++i )
                responseInBytes[i+1] = responseBody[i];

            outputStream.write(responseInBytes);//write the response
            if( additionalData != null && additionalData.length > 0 )//write additional data if it was previously set
                outputStream.write(additionalData);

            outputStream.flush();//wait for response to reach its destination
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAdditionalData(byte[] additionalData){
        this.additionalData = additionalData;
    }



    public ResponsePackage(Response responseType, byte[] responseBody){
        this.responseBody = responseBody;
        setResponseType(responseType);
    }


    public ResponsePackage(Response responseType){
        this( responseType, new byte[RESPONSE_BODY_SIZE] );
    }

    public ResponsePackage(Response responseType, byte[] responseBody, byte[] additionalData){
        this(responseType, responseBody);
        setAdditionalData(additionalData);
    }

    public void setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
    }

    private void setResponseTypeByte(byte responseTypeByte) {
        this.responseTypeByte = responseTypeByte;
    }

    public void setResponseType(Response responseType) {
        this.responseType = responseType;
        this.setResponseTypeByte(this.responseType.getValue());
    }

    public Response getResponseType() {
        return responseType;
    }

    public static final int RESPONSE_BODY_SIZE = 4;
    public static final int RESPONSE_HEADER_SIZE = 1;

    public byte[] getResponseBody() {
        return responseBody;
    }

    public byte getResponseTypeByte() {
        return responseTypeByte;
    }

    public byte[] getAdditionalData() {
        return additionalData;
    }
}
