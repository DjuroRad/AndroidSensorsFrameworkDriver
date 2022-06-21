/**
 *Package provides abstraction for sensors. New sensors can be implemented by either extracting SensorImpl abstract class or by implementing SensorImpl interface<br>
 *Package provides SensorEntry, used by driver developer to define behaviour of each physical sensor its provides. Driver developer has to instantiate each physical sensor through extending abstract class SensorEntry<br>
 *@see arduino_simulator.sensors.SensorImpl
 *@see arduino_simulator.sensors.Sensor
 *@see arduino_simulator.SensorType
 *@see arduino_simulator.sensors.SensorEntry
 *@see arduino_simulator.sensors.SensorEntry.SensorPrecision
 * */
package arduino_simulator.sensors;