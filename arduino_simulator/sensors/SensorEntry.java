package arduino_simulator.sensors;
import arduino_simulator.SensorType;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * class contains basic sensor's requirements for functioning, its limitations and similary, basically it is kind of metadata for sensor
 * part of this data is shared on the initial handshake performed when we connect to the device driver. Device developer is in charge of implementing abstract methods: <br> <br>
 * public abstract JSONObject formatRawData(byte[] rawData);
 * public abstract Object[] getSensorValue(byte[] rawData);
 * */
public abstract class SensorEntry {

    public static final int DEFAULT_SAMPLE_RATE = 500;//default sample rate indicates that each sample reading is to be performed on half a second

    //entries modifiable by android application developer
    private int sampleRate = DEFAULT_SAMPLE_RATE;
    private boolean formatted = false;//sends formatted data also if used wants formatted data
    private SensorPrecision sensorPrecision = new SensorPrecision();

    public void configureSensor(InputStream inputStream){
        try {
            byte[] sampleRateBytes = new byte[4];
            inputStream.read(sampleRateBytes, 0, sampleRateBytes.length);
            this.sampleRate = new BigInteger(sampleRateBytes).intValue();//set sample rate
            sensorPrecision.getSensorPrecision(inputStream);//set precision
            this.formatted = inputStream.read() == 1;//set formatted
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * difference looks for difference between the last measurement and difference specified by android developer, android developer can specify through configure request
     * @see Precision
     * */
    public static class SensorPrecision{
        Precision precision = Precision.PRECISE;
        Double difference = 0.0;

        public Precision getPrecision() {
            return precision;
        }
        public void setPrecision(Precision precision) {
            this.precision = precision;
        }
        public Double getDifference() {
            return difference;
        }
        public void setDifference(Double difference) {
            this.difference = difference;
        }

        public SensorPrecision(){};
        public void getSensorPrecision(InputStream inputStream){
            int n_total = 8 + 1;//8 bytes for precision offset and 1 for precision type
            try {
                int precisionByteInt = inputStream.read();//get the precision first
                if( precisionByteInt == -1 )
                    throw new IOException("Returned -1 while trying to get precision byte");

                this.precision = precisionByteInt == 0 ? Precision.PRECISE : Precision.IMPRECISE_OPTIMIZED;

                byte[] differenceBytes = new byte[8];//get the difference after that
                inputStream.read(differenceBytes, 0, differenceBytes.length);
                this.difference = ByteBuffer.wrap(differenceBytes).getDouble();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * PRECISE indicates that data is always sent even though data maybe has not changed, IMPRECISE_OPTIMIZED indicates that data is sent only if changed by specified desired difference
     * */
    public enum Precision{
        PRECISE(0),
        IMPRECISE_OPTIMIZED(1);

        private final int precisionType;
        Precision(int precisionType) { this.precisionType = precisionType; }
        public int getValue() { return precisionType; }

    }

    private SensorType sensorType = null;
    private int sensorID = -1;
    private int dataSampleByteLength = -1;
    private int formattedDataSampleByteLength = -1;
    private int minValue = -1;

    public int getFormattedDataSampleByteLength() {
        return formattedDataSampleByteLength;
    }

    public void setFormattedDataSampleByteLength(int formattedDataSampleByteLength) {
        this.formattedDataSampleByteLength = formattedDataSampleByteLength;
    }

    private int maxValue = -1;
    private boolean isConnected = false;

    public final static int SENSOR_ENTRY_BYTE_LENGTH = 9 + 4 + 4; // 1 + 4 + 4  -  type + id + sample_byte_length

    private static final String TAG = "SensorEntry";
    private static final Logger logger = Logger.getLogger( TAG );

    /**
     * As driver developer use this class
     *
     * @param sensorType specify type of the sensor, if your sensor type is not in the list of available sensor types, use CUSTOM_SENSOR type
     * @param sensorID specify id of each sensor you register
     * @param dataSampleByteLength specify length from your sensor's data sample in bytes
     * */
    public SensorEntry(SensorType sensorType, int sensorID, int dataSampleByteLength, int minValue, int maxValue) {
        this.sensorType = sensorType;
        this.sensorID = sensorID;
        this.dataSampleByteLength = dataSampleByteLength;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * If Android application's developer wants to get formatted data, this method needs to be overwritten. Formatted data is sent as JSON string later on using JSONObject that needs to be provided through this function
     * @see JSONObject
     * */
    public abstract JSONObject formatRawData(byte[] rawData);

    /**
     * Override this method in order to get some meaningful value from the sensor. This makes sure raw data is manipulated properly
     * */
    public abstract Object[] getSensorValue(byte[] rawData);//return more than 1 possible sensor value

    /**
     * Checks if data has been updated with respect to precision set by Android application developer. Sensor's data needs to be numeric of course
     * @see Precision
     * */
    public boolean dataChanged(byte[] rawData, byte[] prevRawData){
        if(sensorPrecision.precision == Precision.PRECISE || prevRawData.length == 0) return true;

        Object[] values = getSensorValue(rawData);
        Object[] prevValues = getSensorValue(prevRawData);
        Number numValue = (Number)values[0];
        Number prevNumValue = (Number)prevValues[0];

        if( numValue.getClass() == Float.class || numValue.getClass() == Double.class){
            return numValue.doubleValue() >= (prevNumValue.doubleValue() + sensorPrecision.getDifference())
                    || numValue.doubleValue() <= (prevNumValue.doubleValue() - sensorPrecision.getDifference());
        }

        if( numValue.getClass() == Integer.class || numValue.getClass() == Long.class){
            sensorPrecision.setDifference( (double)Math.round(sensorPrecision.getDifference()) );
            return (numValue.longValue() >= (prevNumValue.longValue() + sensorPrecision.getDifference().longValue()))
                    || (numValue.longValue() <= (prevNumValue.longValue() - sensorPrecision.getDifference().longValue()));
        }

        return true;//returns true for other data types, precision not possible
    }

    public void sendSensorEntry(OutputStream outputStream){
        //5 bytes are sent for sensor entry on handshake, 1 byte is for the sensor type, the other 4 bytes are for the id
        byte[] sensorEntryAsBytesArray = new byte[SENSOR_ENTRY_BYTE_LENGTH];

        byte sensorTypeByte = sensorType.getValue();
        byte[] sensorIdBytes = ByteBuffer.allocate(4).putInt(sensorID).array();
        byte[] dataSampleLengthBytes = ByteBuffer.allocate(4).putInt(dataSampleByteLength).array();

        sensorEntryAsBytesArray[0] = sensorTypeByte;
        for( int i = 1; i<5; ++i )
            sensorEntryAsBytesArray[i] = sensorIdBytes[i-1];
        for( int i = 5; i<SENSOR_ENTRY_BYTE_LENGTH; ++i )
            sensorEntryAsBytesArray[i] = dataSampleLengthBytes[i-5];

        try {
            outputStream.write(sensorEntryAsBytesArray);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    //getters
    public int getSampleRate() { return sampleRate; }
    public SensorType getSensorType() {
        return sensorType;
    }
    public int getSensorID() {
        return sensorID;
    }
    public int getDataSampleByteLength() {
        return dataSampleByteLength;
    }
    public int getMinValue() {
        return minValue;
    }
    public int getMaxValue() {
        return maxValue;
    }

    //setters
    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }
    public void setSensorID(int sensorID) {
        this.sensorID = sensorID;
    }
    public void setDataSampleByteLength(int dataSampleByteLength) {
        this.dataSampleByteLength = dataSampleByteLength;
    }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate;}
    public boolean isConnected() {
        return isConnected;
    }
    public void setConnected(boolean connected) {
        isConnected = connected;
    }
    public boolean isFormatted() {
        return formatted;
    }
    public void setFormatted(boolean formatted) {
        this.formatted = formatted;
    }
    public SensorPrecision getSensorPrecision() {
        return sensorPrecision;
    }
    public void setSensorPrecision(SensorPrecision sensorPrecision) {
        this.sensorPrecision = sensorPrecision;
    }
    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
}