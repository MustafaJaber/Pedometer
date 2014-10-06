package com.mjaber.pedometer;

import android.util.Log;

/**
 * 
 * @author Mustafa Jaber 'mstfajbr@gmail.com'
 *
 */
public class Pedometer {

	// Debugging tag
	private static final String TAG = "Pedometer";
	
	// Pedometer singleton
	private static Pedometer instance;

	// values of accelerometer
	private double xdat, ydat, zdat;
	
	// counter for 50 acceleration values
	int cycles;

	// total number of cycles before detecting a step
	int totalCycles;

	// the length of the moving average filter
	int AvgFilterLength;

	// magnitude of XYZ accelerometer values
	double magnitude;

	// old average values
	double oldavg;

	// new average values
	double newavg;

	// threshold to update average values
	double avgThreshold;

	// a flag to initialize average values
	int stepflag;

	// array to store 50 values of acceleration for smoothing
	double[] acceleration = new double[50];

	// Max and Min avg values of acceleration to detect steps
	double maxavg, minavg;

	// Steps Counter
	int steps;

	// A flag to detect a step
	boolean isStepChanged = false;

	private Pedometer() {

		// initialize the variables required to detect steps
		stepflag = 2;
		maxavg = -10000.0;
		minavg = 10000.0;
		oldavg = 0.0;
		newavg = 0.0;
		cycles = 0;
		totalCycles = 0;
		steps = 0;
		AvgFilterLength = 8;
		avgThreshold = 1.5;

		Log.d(TAG, "Called Pedometer Constructor");
	}
	
	public static Pedometer getInstance(){
		if(instance == null)
			instance = new Pedometer();
		
		return instance;
	}
	
	/**
	 * initialize the variables required to detect steps
	 */
	public void reset(){
		
		stepflag = 2;
		maxavg = -10000.0;
		minavg = 10000.0;
		oldavg = 0.0;
		newavg = 0.0;
		cycles = 0;
		totalCycles = 0;
		AvgFilterLength = 8;
		avgThreshold = 1.0;
	
		Log.d(TAG, "Called Pedometer Constructor");
	}

	public void update() {

		// update average acceleration values after 8 cycles
		if (totalCycles > 7) {
			oldavg = newavg;
			newavg -= acceleration[cycles - AvgFilterLength];
		}

		// update the magnitude of the accelerometer values
		magnitude = (double) Math
				.sqrt((xdat * xdat + ydat * ydat + zdat * zdat));

		// update acceleration array
		acceleration[cycles] = magnitude;

		// check whether the average values along with the new magnitude value
		// is bigger/less than the threshold value
		newavg += magnitude;
		if (Math.abs(newavg - oldavg) < avgThreshold)
			newavg = oldavg;

		totalCycles++;
		cycles++;
		cycles = cycles > 49 ? cycles = 8 : cycles;

		if (totalCycles > 8) {
			if (isStep(newavg, oldavg)) { // check if it's a step

				this.steps++;

				// update acceleration values, the idea here is to use moving
				// average filter with a length of 8 steps
				for (int i = 0; i < AvgFilterLength; i++)
					acceleration[i] = acceleration[cycles + i - AvgFilterLength];

				cycles = AvgFilterLength;
				totalCycles = 0;

				// a flag to be used for detecting a step
				isStepChanged = true;
			} else {
				isStepChanged = false;
			}
		}

		Log.d(TAG, "Called update");
	}

	private boolean isStep(double newavg, double oldavg) {

		Log.d(TAG, "Called isStep");

		// Initial phase, detect whether we are in the positive/negative part of
		// the cycle/step
		if (stepflag == 2) {
			if (newavg > (oldavg + avgThreshold))
				stepflag = 1;
			if (newavg < (oldavg - avgThreshold))
				stepflag = 0;
			return false;
		}

		// check the positive part of the step, and detect a step if the old and
		// new average values are in different halves of the cycle
		if (stepflag == 1) {

			// check if the old value is in an either half of the cycle, and the
			// new average value is in the opposite direction
			if ((maxavg > minavg) && (newavg > ((maxavg + minavg) / 2))
					&& (oldavg < ((maxavg + minavg / 2))))
				return true;

			if (newavg < (oldavg - avgThreshold)) {
				stepflag = 0;
				if (oldavg > maxavg)
					maxavg = oldavg; // update max average value
			}
			return false;
		}

		// detect the lower part of the cycle/step
		if (stepflag == 0) {
			if (newavg > (oldavg + avgThreshold)) {
				stepflag = 1;
				if (oldavg < minavg)
					minavg = oldavg; // update min average value
			}
			return false;
		}

		return false;
	}

	public int getSteps() {
		return steps;
	}

	public boolean isStepChanged() {
		return isStepChanged;
	}

	public void setXdat(double xdat) {
		this.xdat = xdat;
	}

	public void setYdat(double ydat) {
		this.ydat = ydat;
	}

	public void setZdat(double zdat) {
		this.zdat = zdat;
	}
}
