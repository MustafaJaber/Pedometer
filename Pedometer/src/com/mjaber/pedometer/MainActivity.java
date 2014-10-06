package com.mjaber.pedometer;

import com.mjaber.pedometer.services.DeadReckoningService;
import com.mjaber.pedometer.services.IDeadReckoning;
import com.mjaber.pedometer.services.IPedometerService;
import com.mjaber.pedometer.services.PedometerService;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public class MainActivity extends Activity {

	// Debugging Tag
	protected static final String TAG = "MainActivity";

	// Start all the services Button, this is used to enable RPC
	private Button startServiceButton;

	// Stop all the services
	private Button stopServiceButton;

	// Update all the values on the screen
	private Button updateButton;

	// Exit the application
	private Button exitButton;

	// Display the accuracy of the GPS on the screen
	private Button accuracyButton;

	// To display position latitude on the screen
	private TextView latitudeTextView;

	// To display position longitude on the screen
	private TextView longitudeTextView;

	// To display the traveled distance on the screen
	private TextView distanceTextView;

	// To display the speed on the screen
	private TextView speedTextView;

	// To display the accuracy value on the screen
	private TextView accuracyTextView;

	// To display the traveled steps when dead reckoning starts
	private TextView stepsTextView;

	// Displays the orientation when dead reckoning starts
	private TextView orientationTextView;

	// A reference to PedometerService to perform RPC
	IPedometerService mPedometerService = null;

	// A reference to DeadReckonningService to perform RPC
	IDeadReckoning mDeadReckoningService = null;

	// A tag to check if PedometerService is bound
	boolean mPedometerIsBound = false;

	// A tag to check if DeadReckonningService is bound
	boolean mDeadReckIsBound = false;

	// A receiver of the sensors values
	private BroadcastReceiver sensorsUpdate;

	// To indicate whether the source of data is GPS or Accelerometer
	private String dataSource = Application.GPS;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initializeViews();

		IntentFilter filter = new IntentFilter(Application.SENSORSUPDATEID);
		sensorsUpdate = new SensorUpdate();
		registerReceiver(sensorsUpdate, filter);
	}

	/**
	 * Initialize all the views with their listeners
	 */
	private void initializeViews() {
		startServiceButton = (Button) findViewById(R.id.StartServiceButton);
		startServiceButton.setOnClickListener(startServiceButtonListener);
		stopServiceButton = (Button) findViewById(R.id.StopServiceButton);
		stopServiceButton.setOnClickListener(stopServiceButtonListener);
		updateButton = (Button) findViewById(R.id.UpdateValuesButton);
		updateButton.setOnClickListener(updateButtonListener);
		accuracyButton = (Button) findViewById(R.id.AccuracyButton);
		accuracyButton.setOnClickListener(accuracyButtonListener);
		exitButton = (Button) findViewById(R.id.ExitButton);
		exitButton.setOnClickListener(exitButtonListener);

		stepsTextView = (TextView) findViewById(R.id.Steps_textResult);
		orientationTextView = (TextView) findViewById(R.id.Orientation_textResult);
		latitudeTextView = (TextView) findViewById(R.id.latitude_textResult);
		longitudeTextView = (TextView) findViewById(R.id.longitude_textResult);
		distanceTextView = (TextView) findViewById(R.id.distance_textResult);
		speedTextView = (TextView) findViewById(R.id.avg_speed_textResult);
		accuracyTextView = (TextView) findViewById(R.id.Accuracy_textResult);
	}

	@Override
	protected void onStop() {
		// stop broadcastreceiver by unregistering it
		unregisterReceiver(sensorsUpdate);
		// unbind all the services
		unbind();
		super.onStop();
	}

	/**
	 * bind services
	 */
	protected void bind() {
		// bind only if it's not bound
		if (!mPedometerIsBound) {
			bindService(pedometerServiceConnection, PedometerService.class);
		}

		if (!mDeadReckIsBound) {
			bindService(deadRecknoningServiceConnection,
					DeadReckoningService.class);
		}
	}

	/**
	 * helper method to do binding
	 * 
	 * @param connection
	 * @param clazz
	 */
	private void bindService(ServiceConnection connection,
			Class<? extends Service> clazz) {
		Intent ib = new Intent(MainActivity.this, clazz);
		getApplicationContext().bindService(ib, connection,
				Context.BIND_AUTO_CREATE);
		Log.d(TAG, clazz.getSimpleName() + " is bound");
	}

	/**
	 * unbid services
	 */
	protected void unbind() {
		// check if services are bound first
		if (mPedometerIsBound) {
			getApplicationContext().unbindService(pedometerServiceConnection);
			mPedometerIsBound = false;
			Log.d(TAG, "GPS Service UNbounded");
		}

		if (mDeadReckIsBound) {
			getApplicationContext().unbindService(
					deadRecknoningServiceConnection);
			mDeadReckIsBound = false;
			Log.d(TAG, "Sensor Service UNbounded");
			unregisterReceiver(sensorsUpdate);
		}
	}
	
	/**
	 * check if PedometerService is running
	 */
	private boolean isPedometerServiceRunning(){
		return isMyServiceRunning(PedometerService.class);
	}
	
	/**
	 * check if DeadReckoningService is running
	 */
	private boolean isDeadReckoningServiceRunning(){
		return isMyServiceRunning(DeadReckoningService.class);
	}
	
	/**
	 * helper method to check if a service are running
	 */
	private boolean isMyServiceRunning(Class<? extends Service> clazz) {
		boolean isRunning = false;
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (clazz.getName().equals(
					service.service.getClassName())) {
				isRunning = true;
			}
		}

		return isRunning;
	}

	/**
	 * broadcast receiver to detect where to get the values from
	 */
	private class SensorUpdate extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			String source = intent.getExtras().getString(Application.SENSOR);

			if (source.equals(Application.GPS)) {
				dataSource = Application.GPS;

			} else if (source.equals(Application.ACCELEROMETER)) {
				dataSource = Application.ACCELEROMETER;

			}

		}

	}

	/*
	 * A button listener to start all the services to start communicating using RPC
	 */
	private OnClickListener startServiceButtonListener = new View.OnClickListener() {
		public void onClick(View v) {

			Intent i = new Intent(MainActivity.this, PedometerService.class);
			getApplicationContext().startService(i);

			Intent mSensorIntent = new Intent(MainActivity.this,
					DeadReckoningService.class);
			getApplicationContext().startService(mSensorIntent);

			bind();
			Toast.makeText(MainActivity.this, "Service Started",
					Toast.LENGTH_SHORT).show();
		}
	};

	/*
	 * A listener to stop all the services
	 */
	private OnClickListener stopServiceButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			// write GPX file before stoping the service
			if (mPedometerIsBound == true) {
				try {
					mPedometerService.writeLogFile();
				} catch (RemoteException e) {
					Log.e(TAG, "Error when trying to call WriteLogFile " + e.toString());
				}
				unbind();
			}
			
			// stop services if they're running
			if(isPedometerServiceRunning())
				stopService(PedometerService.class);
			
			if(isDeadReckoningServiceRunning())
				stopService(DeadReckoningService.class);
			
			Toast.makeText(MainActivity.this, "Services Stopped",
					Toast.LENGTH_SHORT).show();
		}
	};
	
	/**
	 * helper method to stop a service
	 * @param clazz
	 */
	private void stopService(Class<? extends Service> clazz){
		Intent i = new Intent(MainActivity.this, clazz);
		getApplicationContext().stopService(i);
		
		Log.d(TAG, clazz.getSimpleName() + " stopped");
	}

	/*
	 * a lister to update the values on the screen
	 */
	private OnClickListener updateButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			boolean isServiceRunning = isDeadReckoningServiceRunning() & isPedometerServiceRunning();
			
			if (!isServiceRunning) {
				Toast.makeText(MainActivity.this,
						"You have to start the service first",
						Toast.LENGTH_SHORT).show();
			} else {
				bind();
				if (mPedometerService != null) {
					try {
						updateLongLatValues();
						
						// check if the values come from the GPS or Accelerometer
						if (dataSource.equals(Application.GPS)) {
							updateGPSValues();
						} else if (dataSource.equals(Application.ACCELEROMETER)) {
							updateAccelerometerValues();
							updateStepsOrientationValues();
						}

						Log.d(TAG, "Service performed read lat/long");
					} catch (RemoteException e) {
						Log.e(TAG, "An issue when trying to fetch values " + e.toString());
					}
				}

			}
		}

		private void updateGPSValues() throws RemoteException {
			double distance;
			double speed;
			distance = mPedometerService.getDistance();
			speed = mPedometerService.getAverageSpeed();

			updateDistanceSpeedView(distance, speed);
		}

		private void updateAccelerometerValues() throws RemoteException {
			double distance;
			double speed;
			speed = mDeadReckoningService.getAverageSpeed();
			distance = mDeadReckoningService.getDistance();

			updateDistanceSpeedView(distance, speed);
		}

		private void updateStepsOrientationValues() throws RemoteException {
			double steps;
			double orientation;
			steps = mDeadReckoningService.getSteps();
			orientation = mDeadReckoningService.getOrientation();

			stepsTextView.setText(Double.toString(steps));
			orientationTextView.setText(Double.toString(orientation));
		}

		private void updateLongLatValues() throws RemoteException {

			double latitude = mPedometerService.getLatitude();
			double longitude = mPedometerService.getLongitude();

			latitudeTextView.setText(Double.toString(latitude));
			longitudeTextView.setText(Double.toString(longitude));
		}

		private void updateDistanceSpeedView(double distance, double speed) {
			distanceTextView.setText(Double.toString(distance));
			speedTextView.setText(Double.toString(speed));
		}
	};

	/*
	 *  A listener to update the values on the screen
	 */
	private OnClickListener accuracyButtonListener = new OnClickListener() {
		public void onClick(View v) {
			float accuracy = 0;
			if (!isPedometerServiceRunning()) {
				Toast.makeText(MainActivity.this,
						"You have to start the service first",
						Toast.LENGTH_SHORT).show();
			} else {
				bind();
				if (mPedometerService != null) {
					try {
						accuracy = mPedometerService.getAccuracy();
						Log.d(TAG, "Service performed Read Accuracy");
					} catch (RemoteException e) {
						// There is nothing special we need to do if the service
						// has crashed.
						Log.e(TAG, "Service not sucess");
					}
				}

				accuracyTextView.setText(Float.toString(accuracy));

			}
		}

	};

	/*
	 * A listener to exit the application
	 */
	private OnClickListener exitButtonListener = new OnClickListener() {
		public void onClick(View v) {
			finish();
		}
	};

	/**
	 * a connection to the PedometerService
	 */
	private ServiceConnection pedometerServiceConnection = new ServiceConnection() {
		// Called when the connection with the service is established
		public void onServiceConnected(ComponentName className, IBinder service) {
			mPedometerService = IPedometerService.Stub.asInterface(service);
			mPedometerIsBound = true;
			Log.e(TAG, "PedometerService Connection created");
		}

		public void onServiceDisconnected(ComponentName className) {
			mPedometerService = null;
			mPedometerIsBound = false;
			Log.e(TAG, "PedometerService Connection lost");
		}
	};

	/**
	 * a connection to the DeadReckoningService
	 */
	private ServiceConnection deadRecknoningServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {

			mDeadReckoningService = IDeadReckoning.Stub.asInterface(service);
			mDeadReckIsBound = true;
			Log.e(TAG, "DeadReckoningServices Connection created");
		}

		public void onServiceDisconnected(ComponentName name) {
			mDeadReckoningService = null;
			mDeadReckIsBound = false;
			Log.e(TAG, "DeadReckoningServices Connection Lost");
		}
	};

}