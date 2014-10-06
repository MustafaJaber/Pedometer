package com.mjaber.pedometer.services;

interface IDeadReckoning {
	double getOrientation();
	double getSteps();
	double getDistance();
	double getAverageSpeed();
}