package com.mjaber.pedometer;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public class Application extends android.app.Application {

	// Debuggin Tag
	public static final String APPTAG = "Pedometer";

	// Sensors Constants
	public static final String SENSOR = "SENSOR";
	public static final String GPS = "GPS";
	public static final String ACCELEROMETER = "ACCELEROMETER";
	
	// ID constanct used by a broadcast receiver to switch between sensors
	public static final String SENSORSWITCHID = "com.mjaber.pedometer.SensorSwitchReceiver";
	
	// ID constanct used by a broadcast receiver to retrieve sensors' data
	public static final String SENSORSUPDATEID = "com.mjaber.pedometer.SensorUpdateReceiver";
	public static final String START = "START";
	public static final String STOP = "STOP";

	// minTime minimum time interval between location updates, in milliseconds
	public static final long MIN_TIME_MS = 1000 * 1;// one second
	
	// minDistance minimum distance between location updates, in meters
	public static final float MIN_DISTANCE_IN_MTS = 0; // one meter
	

	@Override
	public void onCreate() {
		super.onCreate();
	}

}
