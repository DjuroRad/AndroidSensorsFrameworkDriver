package arduino_simulator;

import java.io.InputStream;

/**
 * Base functionality Arduino simulator provides. It is used to simulate getting data from and connecting to an Arudino device.
 * */
public interface ArduinoSimulatorInterface {

    /**
     * imitates connecting to a bluetooth remote hardware device
     * delay is necessary when this happens alongside with establishing the communication
     * @return input stream needed to establish communication
     * */
    public InputStream connectViaBluetooth();

    /**
     * Sets the baud rate (bits per second transferred) of simulated Arduino
     *
     * @param baud baud rate for the serial communication
     * @return -1 on invalid baud rate
     * */
    public int serialBegin(int baud);

    /**
     * @return The last reading as raw data represented as an array of bytes. If data is digital, then array of only one byte will be returned
     * */
    public byte[] getLastReading();

    /**
     * stop arduino background execution
     * */
    public void disconnect();

}
