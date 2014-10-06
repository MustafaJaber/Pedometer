package com.mjaber.pedometer.services;

interface IPedometerService {
	double getLatitude();
	double getLongitude();
	double getDistance();
	double getAverageSpeed();
	float  getAccuracy();
	void writeLogFile();

}