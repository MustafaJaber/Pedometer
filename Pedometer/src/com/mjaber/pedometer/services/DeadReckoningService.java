package com.mjaber.pedometer.services;

import com.mjaber.pedometer.Application;
import com.mjaber.pedometer.Pedometer;
import com.mjaber.pedometer.logger.DataLogger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public class DeadReckoningService extends Service implements
		SensorEventListener {

	// Debugging Tag
	protected static final String TAG = "DeadReckoningService";

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor magneticField;

	private Pedometer meter;

	private BroadcastReceiver sensorSwitch;

	private Intent sensorUpdateIntent;

	private double orientation = -1.0;
	private double distance = 0.0;
	private int steps = 0;

	/**
	 * initialize the Sensor managers and broadcast receiver
	 */
	public void onCreate() {

		Log.d(TAG, "Called onCreate");

		super.onCreate();

		meter = Pedometer.getInstance();

		initializeSensors();

		IntentFilter filter = new IntentFilter(Application.SENSORSWITCHID);
		sensorSwitch = new SensorSwitchReceiver();
		registerReceiver(sensorSwitch, filter);

		sensorUpdateIntent = new Intent(Application.SENSORSUPDATEID);

	}

	private void initializeSensors() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			accelerometer = sensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			magneticField = sensorManager
					.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}
	}

	public void onDestroy() {
		super.onDestroy();

		// unregister sensor manager and broadcast receivers
		sensorManager.unregisterListener(this);
		unregisterReceiver(sensorSwitch);

		Log.d(TAG, "Called onDestroy");
	}

	private final IDeadReckoning.Stub mBinder = new IDeadReckoning.Stub() {

		@Override
		public double getOrientation() throws RemoteException {
			return orientation;
		}

		@Override
		public double getSteps() throws RemoteException {
			return (double) steps;
		}

		@Override
		public double getDistance() throws RemoteException {
			return distance;
		}

		@Override
		public double getAverageSpeed() throws RemoteException {
			return distance
					/ ((System.currentTimeMillis() - DataLogger.getInstance()
							.getFirstTime()) / 1000.0);
		}

	};

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Called onBind");
		return mBinder;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		float[] acc = new float[3];
		float[] mag = new float[3];
		float[] mOrientation = new float[3];
		float[] mRotationM = new float[9];
		float[] mRotationM2 = new float[9];
		
		switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				acc[0] = event.values[0];
				acc[1] = event.values[1];
				acc[2] = event.values[2] - 9.8f;
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				mag[0] = event.values[0];
				mag[1] = event.values[1];
				mag[2] = event.values[2];
				break;
		}

		getOrientationMatrix(acc, mag, mOrientation, mRotationM, mRotationM2);

		meter.setXdat(acc[0]);
		meter.setYdat(acc[1]);
		meter.setZdat(acc[2]);

		orientation = Math.toDegrees(mOrientation[0]);

		meter.update();

		if (meter.isStepChanged()) {
			convertToLocation();
		}

	}

	private void getOrientationMatrix(float[] acc, float[] mag,
			float[] mOrientation, float[] mRotationM, float[] mRotationM2) {
		if (SensorManager.getRotationMatrix(mRotationM, null, acc, mag)) {
			Log.d(TAG, "rotation Matrix exitosa");
			SensorManager.remapCoordinateSystem(mRotationM,
					SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationM2);
			SensorManager.getOrientation(mRotationM, mOrientation);
			Log.d(TAG, "Orientation" + Math.toDegrees(mOrientation[0]));
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/**
	 * convert sensor values to Location values "Longitude/Latitude"
	 */
	public void convertToLocation() {
		double longitude = Double.MIN_VALUE;
		double latitude = Double.MIN_VALUE;

		distance += 0.75;
		steps = meter.getSteps();

		DataLogger.getInstance().setCurrDistance(distance);

		latitude = 0.75 * Math.cos(orientation) * 0.000009
				+ DataLogger.getInstance().getPrevLatitude();
		longitude = 0.75 * Math.sin(orientation) * 0.0000136
				+ DataLogger.getInstance().getPrevLongitude();

		DataLogger.getInstance().addLocation(latitude, longitude,
				java.lang.System.currentTimeMillis());

		Log.d(TAG, "Called convertToLocation");
	}

	/**
	 * Start the accelerometer and magneticField sensors
	 */
	public void startSensors() {
		sensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(this, magneticField,
				SensorManager.SENSOR_DELAY_UI);

		Log.d(TAG, "Called startSensors");
	}

	/**
	 * Stop the accelerometer and magneticField sensors
	 */
	public void stopSensors() {
		sensorManager.unregisterListener(this);

		Log.d(TAG, "Called stopSensors");
	}

	/**
	 * Broadcast receiver to switch on/off the sensors, it gets called from the
	 * GPS service
	 */
	private class SensorSwitchReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			Log.d(TAG, "Called SensorSwitchReceiver/onReceive");

			String sensorValue = intent.getExtras().getString(
					Application.SENSOR);
			if (sensorValue.equals(Application.START)) {
				distance = DataLogger.getInstance().getCurrDistance();

				meter.reset();

				startSensors();

				sensorUpdateIntent.putExtra(Application.SENSOR,
						Application.ACCELEROMETER);
				sendBroadcast(sensorUpdateIntent);

			} else if (sensorValue.equals(Application.STOP)) {

				stopSensors();

				sensorUpdateIntent
						.putExtra(Application.SENSOR, Application.GPS);
				sendBroadcast(sensorUpdateIntent);
			}

		}

	}
}
