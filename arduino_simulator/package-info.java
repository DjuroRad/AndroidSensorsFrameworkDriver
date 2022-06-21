/**
 *Classes and methods in this package provide everything necessary to run a simulation for external sensors.<br>
 *This package also provides class named SensorEntry used in order to provide sensors to Android side.<br>
 *Device developer has to provide sensor instance of each physical sensor it has on the device. Also, these sensors are passed to the framework in order in which data arrives! More precisely, framework service takes these parameters as a list and that list needs to have SensorEntity instances added in order in which data arrives.<br>
 *@see arduino_simulator.sensors.SensorEntry
 *@see arduino_simulator.ArduinoSimulator
 * */
package arduino_simulator;