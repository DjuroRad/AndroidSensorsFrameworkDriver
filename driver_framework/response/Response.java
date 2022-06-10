package driver_framework.response;

/**
 * Response form has 2 types:
 *  Response field -> | Response |
 *  Body field -> | BODY |
 * | Response | BODY |
 *      1        4
 *
 * | Response | BODY |
 *      1        n          -> n represents variables length of body, this is crucial when writing sensor's data into client's input stream
 *
 * each response contains of 5 bytes
 * 1^st byte represents response type
 * following 4 bytes represent additional response data. Some responses will use this space, some will leave it empty.
 * For example:
 *      Response type: CONFIGURE_Y
 *          [CONFIGURE_Y.byteValue, -, -, -, - ] -> informs that it can get number of bytes requested in CONFIGURE request.
 *      Response type: IS_CONNECTED_Y
 *          [IS_CONNECTED_Y.byteValue, -, -, -, - ]
 *      Response type: START_READING_Y
 *          [ START_READING_Y.byteValue, 0, 0, 0, 0b0000_0100 ] -> informs that reading started and that each time it will send 4 bytes
 *      Response type: READING
 *          [ READING.byteValue, x, x, x, x, ... , n^th 'x' byte ]
 *      etc.
 * */
public enum Response {
    //positive responses
    /**
     *  Request = | CONNECT | - - - - |
     *  Response = | CONNECT_Y | n sensors | (sensorPackage - id + sensor type)
     * */
    CONNECT_Y((byte)200),

    /**
     *  Request = | CONFIGURE | sensor_id | sample rate ( 4 bytes )
     *  Response = | CONNECT_Y | - - - - |
     * */
    CONFIGURE_Y((byte)202),
    /**
     * Request = | CONNECT_SENSOR | SENSOR_TYPE(1 byte) - - - |
     * this response is given if Sensor_type is registered / present on the board. This information is explicitly specified beforehand
     * Response = | CONNECT_SENSOR_Y | - - - - |
     * */
    CONNECT_SENSOR_Y((byte)0b00000_00_1),

    /**
     * Request = | IS_CONNECTED |
     * response given if remote mobile device has previously connected and informs about which sensor connected
     * Response = | IS_CONNECTED_N | SENSOR_TYPE(1 byte) - - - |
     * */
    IS_CONNECTED_Y((byte)0b00000_01_1),

    /**
     * Request = | START_READ | - - - - |
     * This response is given if data is available for reading. Checking IS_CONNECTED beforehand with desired sensor type advised. Otherwise, might be returning data of some other sensor
     * Response = | START_READ_Y | nBytes |
     * */
    START_READ_Y((byte)0b00000_10_1),

    /**
     * Request = | DISCONNECT_SENSOR |
     * if sensor exists and is connected, response is positive, otherwise negative
     * Response = | DISCONNECT_SENSOR_Y | sensor id |
     */
    DISCONNECT_SENSOR_Y((byte)9),


    /**
     * Request = | STOP_READ |
     * response informs that reading paused ( ideally, this will almost always execute )
     * Response = | STOP_READ_Y | - - - - |
     * */
    STOP_READ_Y((byte)0b00000_11_1),

    //negative responses

    /**
     *  Request = | CONNECT | - - - - |
     *  Response = | CONNECT_N | - - - - |
     *  problem with establishing communication
     * */
    CONNECT_N((byte)201),

    /**
     *  Request = | CONFIGURE | sensor_id | sample rate ( 4 bytes )
     *  Response = | CONNECT_N | sensor_id
     * */
    CONFIGURE_N((byte)203),
    /**
     * Request = | CONNECT_SENSOR | SENSOR_TYPE(1 byte) |
     * this response is given if Sensor_type is not registered / present on the board. This information is explicitly specified beforehand by driver developer
     * Response = | CONNECT_SENSOR_n | - - - - |
     * */
    CONNECT_SENSOR_N((byte)0b00000_00_0),
    /**
     * Request = | IS_CONNECTED |
     * response given if remote mobile device hasn't previously connected
     * Response = | IS_CONNECTED_N | - - - - |
     * */
    IS_CONNECTED_N((byte)0b00000_01_0),
    /**
     * Request = | START_READ | - - - - |
     * This response is given if data is available for reading. Checking IS_CONNECTED beforehand with desired sensor type advised. Otherwise, might be returning data of some other sensor
     * Response = | START_READ_Y | nBytes |
     * */
    START_READ_N((byte)0b00000_10_0),

    /**
     * Request = | STOP_READ |
     * response informs that that pausing reading was not possible, this may be due to some system error / unresponsivenes
     * Response = | STOP_READ_N | - - - - |
     * */
    STOP_READ_N((byte)0b00000_11_0),

    /**
     * Request = | DISCONNECT_SENSOR |
     * if sensor exists and is connected, response is positive, otherwise negative
     * Response = | DISCONNECT_SENSOR_N | sensor id |
     */
    DISCONNECT_SENSOR_N((byte)8),

    //other
    /**
     * Request = none -> this response sent only while data is being read from the sensor
     * Informs that reading is in progress, and sends read data along with it. # of bytes read / written depends on sensor's data type, or configuration
     * Response = | READING_SENSOR_DATA | x, x, x, x, ... , n^th 'x' byte |
     * */
    READING_SENSOR_DATA((byte) 0b1111_1111),

    /**
     * Request = 'ANY'
     * Informs that one sending the request is not following framework's conventions and sends the rejected request type alongside with the response.
     * Response = | INVALID_REQUEST | REQUEST(1 byte) - - - |
     * */
    INVALID_REQUEST((byte)0b1000_0000);
    
    private byte mByte;
    Response(byte mByte) {
        this.mByte = mByte;
    }

    public byte getValue() {
        return mByte;
    }
}
