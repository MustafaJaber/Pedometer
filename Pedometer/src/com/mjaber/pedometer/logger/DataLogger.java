package com.mjaber.pedometer.logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.location.Location;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public class DataLogger {

	// DataLogger singleton
	private static DataLogger instance = null;

	// Debugging Tag
	private static final String TAG = "DataLogger";

	private List<LocationData> locations;

	private int counter = 0;

	private double prevLatitude;
	private double prevLongitude;
	private long prevTime;

	private long firstTime;
	private double currDistance;

	protected DataLogger() {

		locations = new LinkedList<LocationData>();

	}

	public static DataLogger getInstance() {
		if (instance == null)
			instance = new DataLogger();

		return instance;
	}

	public synchronized void addLocation(Location location) {

		addLocation(location.getLongitude(), location.getLatitude(),
				location.getAltitude(), location.getTime());
	}

	public synchronized void addLocation(double longitude, double latitude,
			long time) {

		addLocation(longitude, latitude, 0.0, time);

		LocationData data = new LocationData(longitude, latitude, 0.0, time);

		locations.add(data);
		counter++;
	}

	public synchronized void addLocation(double longitude, double latitude,
			double altitude, long time) {

		if (locations.size() == 0) {
			firstTime = time;
		}

		LocationData data = new LocationData(longitude, latitude, altitude,
				time);

		prevLongitude = longitude;
		prevLatitude = latitude;
		prevTime = time;

		locations.add(data);
		counter++;
	}

	public synchronized void writeGPXfile() {

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				GPXWriter.writeFile(Collections.unmodifiableList(locations));
			}
		});

		thread.start();

		resetData();

	}

	private void resetData() {
		counter = 0;
		prevLongitude = 0;
		prevLatitude = 0;
		prevTime = 0;
		currDistance = 0;
		locations.clear();
	}

	public double getPrevLatitude() {
		return prevLatitude;
	}

	public double getPrevLongitude() {
		return prevLongitude;
	}

	public long getPrevTime() {
		return prevTime;
	}

	public double getCurrDistance() {
		return currDistance;
	}

	public void setCurrDistance(double currDistance) {
		this.currDistance = currDistance;
	}

	public long getFirstTime() {
		return firstTime;
	}

	public void setFirstTime(long firstTime) {
		this.firstTime = firstTime;
	}

	public int getCounter() {
		return counter;
	}
}
