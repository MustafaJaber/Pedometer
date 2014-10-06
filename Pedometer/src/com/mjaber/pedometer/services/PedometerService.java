package com.mjaber.pedometer.services;

import com.mjaber.pedometer.Application;
import com.mjaber.pedometer.logger.DataLogger;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public class PedometerService extends Service implements LocationListener {

	// Debugging Tag
	private static final String TAG = "PedometerService";

	// location obtained when the service starts
	private Location location;

	private double latitude;
	private double longitude;
	private double distance = 0;
	private double averageDistance = 1;
	private double averageSpeed;

	// accuracy of the GPS signal
	private float GPSAccuracy = 0;

	// to indicate if we got the first GPS reading
	private boolean receivedFirstGPSReading = false;

	// logs the time of the last position value
	private long lastOnLocationUpdateTime = 0;

	// A reference to Android's Location Manger
	protected LocationManager locationManager;

	// indicates whether the current sensor is GPS or ACCELEROMETER
	private String currentSensor = Application.GPS;

	// a handler to decide on switching between GPS and ACCELEROMETER
	private Handler handler;

	// to send messages to switch between GPS and Accelerometer
	private Intent sensorSwitchIntent;

	// Called once when service is started
	@Override
	public void onCreate() {

		Log.d(TAG, "Called onCreate");

		super.onCreate();
		initializeLocation();

		sensorSwitchIntent = new Intent(Application.SENSORSWITCHID);
	}

	// Called once when service is stopped
	@Override
	public void onDestroy() {
		locationManager.removeUpdates(this);
	}

	/**
	 * This method does a setup of the location Manager
	 */
	public void initializeLocation() {
		try {
			// Acquire a reference to the system Location Manager
			locationManager = (LocationManager) this
					.getSystemService(LOCATION_SERVICE);

			// Check if GPS is enabled
			boolean isGPSEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			Log.d(TAG,
					"GPS provider enabled: " + Boolean.toString(isGPSEnabled));

			// Check if NetworkPosition is enabled
			boolean isNetworkEnabled = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			Log.d(TAG,
					"Network provider enabled: "
							+ Boolean.toString(isNetworkEnabled));

			if (!isGPSEnabled && !isNetworkEnabled) {
				// There is no way to get location, nothing else to do here
			} else {
				// First try to get location from Network Provider because it's
				// faster than GPS
				if (isNetworkEnabled) {
					getNetworkLocation();
				}

				// if the user enabled GPS usage on the phone, initialize the
				// module and get first values
				if (isGPSEnabled) {
					getGPSLocation();
				}

			}// finished the setup

		} catch (Exception e) {
			Log.e(TAG, "Exception in getLocation" + e.toString());
		}

	}

	private void getGPSLocation() {

		Log.d(TAG, "start getting GPS locations");

		// Register the listener with the Location Manager to
		// receive location updates from GPS module
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				Application.MIN_TIME_MS, Application.MIN_DISTANCE_IN_MTS, this);

		// Try to get the last Known location from the GPS
		if (locationManager != null) {
			
			location = locationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
			if (location != null) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				GPSAccuracy = location.getAccuracy();
				Log.e(TAG, "last known location from GPS");
			}
		}
	}

	private void getNetworkLocation() {
		
		Log.d(TAG, "start getting locations from Network Provider");
		
		// Register the listener with the Location Manager to
		// receive location updates from Network Provider
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, Application.MIN_TIME_MS,
				Application.MIN_DISTANCE_IN_MTS, this);
		
		// Only if the location retrieves position, this is read
		if (locationManager != null) {
			location = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (location != null) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				GPSAccuracy = location.getAccuracy();
			}
		}
		locationManager.removeUpdates(this);
	}

	/**
	 * Called when a new location is sensed by the location provider.
	 */
	@Override
	public void onLocationChanged(Location loc) {
		GPSAccuracy = loc.getAccuracy();
		// Only if the accuracy is better than 5 meters log records on the
		// ListOfLocations
		if (loc.getAccuracy() > 0 && loc.getAccuracy() < 12) {

			switchToGPS();

			distance = DataLogger.getInstance().getCurrDistance();

			Log.d(TAG, "Location Changed and with accuracy");
			
			// If is the first time we get a measurement with enough accuracy
			if (!receivedFirstGPSReading) {
				updateValues(loc);
				DataLogger.getInstance().setFirstTime(loc.getTime());
				receivedFirstGPSReading = true;

				// start the thread to check the update rate of the GPS sensor
				handler = new Handler();
				handler.postDelayed(new CheckGPSUpdateTime(), 5000);
			} else {
				updateValues(loc);
			}
		}

	}

	private void updateValues(Location loc) {
		// check the distance against previous point
		distance += haversine(latitude, longitude, loc.getLatitude(),
				loc.getLongitude());
		latitude = loc.getLatitude();
		longitude = loc.getLongitude();
		location = loc;
		lastOnLocationUpdateTime = System.currentTimeMillis();

		DataLogger.getInstance().setCurrDistance(distance);
		DataLogger.getInstance().addLocation(loc);
	}

	/**
	 * Haversine function to get distance between two coordinate points
	 */
	private double haversine(double lat1, double lon1, double lat2, double lon2) {

		// Constant Earth Radius in meters(Ellipsoidal Quadratic Mean Radius)
		final double R = 6372800;

		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
				* Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
		double c = 2 * Math.asin(Math.sqrt(a));
		return R * c;
	}

	/**
	 * this inner class keeps checking the signal of the GPS, if there's no
	 * signal for 5 seconds it switches to the sensors
	 */
	private class CheckGPSUpdateTime implements Runnable {
		@Override
		public void run() {
			if ((System.currentTimeMillis() - lastOnLocationUpdateTime) > 5000) {
				if (currentSensor == Application.GPS) {
					DataLogger.getInstance().setCurrDistance(distance);
					switchToAccelerometer();
				}
			} else {
				if (currentSensor == Application.ACCELEROMETER) {
					switchToGPS();
					distance = DataLogger.getInstance().getCurrDistance();
				}
			}

			handler.postDelayed(this, 5000);

		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	/**
	 * change the location data source when loosing the signal of the GPS, send
	 * a flag to the Sensor service to start the sensors
	 */
	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

		Log.d(TAG, "Called onStatusChanged");

		if (arg1 == LocationProvider.OUT_OF_SERVICE
				|| arg1 == LocationProvider.TEMPORARILY_UNAVAILABLE) {

			if (currentSensor == Application.GPS) {
				DataLogger.getInstance().setCurrDistance(distance);
				switchToAccelerometer();
			}

			Log.d(TAG, "Called onStatusChanged, OUT_OF_SERVICE");

		} else if (arg1 == LocationProvider.AVAILABLE) {
			if (currentSensor == Application.ACCELEROMETER) {

				distance = DataLogger.getInstance().getCurrDistance();

			}

			Log.d(TAG, "Called onStatusChanged, AVAILABLE");
		}

	}

	private void switchToGPS() {
		switchSensor(Application.GPS, Application.STOP);
	}

	private void switchToAccelerometer() {
		switchSensor(Application.ACCELEROMETER, Application.STOP);
	}

	private void switchSensor(String sensor, String action) {

		sensorSwitchIntent.putExtra(Application.SENSOR, action);
		sendBroadcast(sensorSwitchIntent);
		currentSensor = sensor;
	}

	/**
	 * Returns Average Speed in meters/second
	 * @return
	 */
	private double averageSpeed() {
		double speed = 0.0;
		double deltaTimeIn_MS = (double) lastOnLocationUpdateTime
				- DataLogger.getInstance().getFirstTime();
		if (distance > 0 && deltaTimeIn_MS > 0) {
			speed = distance / (deltaTimeIn_MS / 1000);
		}
		return speed;
	}

	private final IPedometerService.Stub mBinder = new IPedometerService.Stub() {

		@Override
		public double getLatitude() throws RemoteException {
			if (currentSensor == Application.GPS) {
				if (location != null) {
					return latitude;
				} else {
					return 0;
				}
			} else {
				return DataLogger.getInstance().getPrevLatitude();
			}
		}

		@Override
		public double getLongitude() throws RemoteException {
			if (currentSensor == Application.GPS) {
				if (location != null) {
					return longitude;
				} else {
					return 0;
				}
			} else {
				return DataLogger.getInstance().getPrevLongitude();
			}
		}

		@Override
		public double getDistance() throws RemoteException {
			averageDistance = distance;
			return averageDistance;
		}

		@Override
		public double getAverageSpeed() throws RemoteException {
			averageSpeed = averageSpeed();
			return averageSpeed;
		}

		@Override
		public float getAccuracy() throws RemoteException {
			return GPSAccuracy;
		}

		@Override
		public void writeLogFile() throws RemoteException {
			if (DataLogger.getInstance().getCounter() > 1) {
				DataLogger.getInstance().writeGPXfile();
			}
		}

	};

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Called onBind");
		return mBinder;
	}

}
