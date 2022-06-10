package driver_framework.request;

/**
 * Request form
 * | Request | BODY |
 *      1       4
 * each request contains of 5 bytes
 * 1^st byte represents request type
 * following 4 bytes represent additional request data. Some requests will use this space, some will leave it empty.
 * For example:
 *      Request type: CONNECT_SENSOR
 *          [CONNECT_SENSOR.byteValue, SENSOR_TYPE.byteValue, -, -, - ]
 *      Request type: CONFIGURE
 *          [CONFIGURE.byteValue, 0, 0, 0, 0b0000_1111] - informs that maximum amount of data it can get is 15 bytes
 *      Request type: IS_CONNECTED
 *          [IS_CONNECTED.byteValue, -, -, -, -]
 *      etc.
 *
 * ADDITIONAL DATA IS OPTIONAL AND DEPENDS ON SENSOR TYPE SENT
 * */
public enum Request{

    CONNECT((byte)200),
    CONNECT_SENSOR((byte) 0b0000_0000),
    IS_CONNECTED((byte)0b0000_0001),
    START_READ((byte)0b0000_0010),
    STOP_READ((byte)0b0000_0011),
    DISCONNECT((byte) 0b0000_0100),
    DISCONNECT_SENSOR((byte)6),
    CONFIGURE((byte) 5);

    private final byte mByte;
    Request(byte mByte) {
        this.mByte = mByte;
    }

    public byte getValue() {
        return this.mByte;
    }

    public static Request getRequestFromByte(byte requestAsByte){
        if( requestAsByte == Request.CONNECT.mByte )
            return Request.CONNECT;
        if( requestAsByte == Request.CONNECT_SENSOR.mByte )
            return Request.CONNECT_SENSOR;
        if( requestAsByte == Request.IS_CONNECTED.mByte )
            return Request.IS_CONNECTED;
        if( requestAsByte == Request.START_READ.mByte )
            return Request.START_READ;
        if( requestAsByte == Request.STOP_READ.mByte )
            return Request.STOP_READ;
        if( requestAsByte == Request.DISCONNECT.mByte )
            return Request.DISCONNECT;
        if( requestAsByte == Request.CONFIGURE.mByte )
            return Request.CONFIGURE;
        if( requestAsByte == Request.DISCONNECT_SENSOR.mByte )
            return Request.DISCONNECT_SENSOR;

        return null;
    }

    public static final int REQUEST_SIZE = 5;
}

