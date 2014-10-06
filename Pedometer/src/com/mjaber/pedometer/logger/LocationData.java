package com.mjaber.pedometer.logger;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public class LocationData {

	private double longitute;
	private double latitute;
	private double altitude;
	private long time;
	
	public LocationData(double longitute, double latitute, double altitude,
			long time) {
		super();
		this.longitute = longitute;
		this.latitute = latitute;
		this.altitude = altitude;
		this.time = time;
	}

	public double getAltitude() {
		return altitude;
	}

	public double getLongitute() {
		return longitute;
	}

	public double getLatitute() {
		return latitute;
	}

	public long getTime() {
		return time;
	}


	
}
