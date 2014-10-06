package com.mjaber.pedometer;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public enum DataSource {
	GPS("GPS"),ACCELEROMETER("ACCEL");
	
	private String value = "";
	
	
	private DataSource(String val) {
		this.value = val;
	}
	
	public String getValue() {
		return this.value;
	}
	
}
