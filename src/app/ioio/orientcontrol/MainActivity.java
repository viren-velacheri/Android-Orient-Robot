//-------------------------------------------------------------------------------------------
// Main code for App
// Class looper has event loop controlling Robot
// Author: Viren Velacheri
//-------------------------------------------------------------------------------------------
package app.ioio.orientcontrol;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MainActivity extends IOIOActivity implements SensorEventListener {

	//UI buttons
	Button btnPowerOn, btnPowerZero, btnInvertTurn, btnStop, btnPowerSub , btnUpdate, btnScaleUp, btnScaleDown ;

	//Textviews for display
	public TextView mThrottleView , mOrientView ,mLeftDutyCycleView, mRightDutyCycleView, mSteerView ,
		mEpsilonScaleFactorView,mEpsilonView;

	SensorManager mSensorManager;
	Sensor accel;
	Sensor compass;
	Sensor gyro;

	float[] mMagneticValues;
	float[] mAccelerometerValues;
	float[] gyroscope_values;
	float[] orientation;
	float[] Rot;

	int mThrottle  = 5;
	float mSensorOrient ;

	//Init code, bind buttons and textviews to layout.xml
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		compass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		gyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		btnUpdate = (Button) findViewById(R.id.update);

		btnScaleUp = (Button) findViewById(R.id.scale_up);
		btnScaleDown = (Button) findViewById(R.id.scale_down);

		btnPowerOn = (Button) findViewById(R.id.power_add);
		btnPowerSub = (Button) findViewById(R.id.power_sub);

		btnPowerZero = (Button) findViewById(R.id.power_zero);
		btnInvertTurn = (Button) findViewById(R.id.invert_turn);
		btnStop = (Button) findViewById(R.id.stop);
		mThrottleView = (TextView) findViewById(R.id.throttle);
		mOrientView = (TextView) findViewById(R.id.orient);
		mLeftDutyCycleView = (TextView) findViewById(R.id.leftdutycycle);
		mRightDutyCycleView = (TextView) findViewById(R.id.rightdutycycle);
		mSteerView = (TextView) findViewById(R.id.steer);
		mEpsilonScaleFactorView = (TextView) findViewById(R.id.epsilon_scale_factor);
		mEpsilonView = (TextView) findViewById(R.id.epsilon);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mSensorManager.registerListener(this, accel,SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, compass,SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, gyro,	SensorManager.SENSOR_DELAY_GAME);

		Rot = new float[9];
		mMagneticValues = new float[3];
		mAccelerometerValues = new float[3];
		orientation = new float[3];
		gyroscope_values = new float[3];
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	/***************************************************************  sensors  ***************************************************************/
	//Read sensor data
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		switch (event.sensor.getType())
		{
		case Sensor.TYPE_MAGNETIC_FIELD:
			mMagneticValues[0] = event.values[0];
			mMagneticValues[1] = event.values[1];
			mMagneticValues[2] = event.values[2];
			break;

		case Sensor.TYPE_GYROSCOPE:
			gyroscope_values[0] = event.values[0];
			gyroscope_values[1] = event.values[1];
			gyroscope_values[2] = event.values[2];
			//gyroView.setText("Gyro: " + "\n"+ gyroscope_values[0] + "\n" + gyroscope_values[1] +"\n"+ gyroscope_values[2]);
			break;

		case Sensor.TYPE_ACCELEROMETER:
			mAccelerometerValues[0] = event.values[0];
			mAccelerometerValues[1] = event.values[1];
			mAccelerometerValues[2] = event.values[2];
			//accelView.setText("Accel: " + "\n"+ String.format("%2.0f",mAccelerometerValues[0]) + "\n" +
			//String.format("%2.0f",mAccelerometerValues[1]) +"\n"+ String.format("%2.0f",mAccelerometerValues[2]) );
			//mThrottle = THROTTLE_IDLE - (int)(mAccelerometerValues[0]);
			break;
		}

		SensorManager.getRotationMatrix(Rot, null, mAccelerometerValues, mMagneticValues);
		SensorManager.getOrientation(Rot, orientation);
		orientation[0] = (float) Math.toDegrees(orientation[0]);
		orientation[1] = (float) Math.toDegrees(orientation[1]);
		orientation[2] = (float) Math.toDegrees(orientation[2]);
		//Orientation will be from -180 to 180, change that to 0 to 360 by adding 180
		mSensorOrient = (orientation[0] + 180.0f);
		mOrientView.setText("Sensor Orient = " + mSensorOrient);
		//compassView.setText("Compass: " + "\n"+ orientation[0] + "\n" + orientation[1] +"\n"+ orientation[2]);
	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {}

	//-------------------------------------------------------
	// Main event loop that controls Robot
	//-------------------------------------------------------
	class Looper extends BaseIOIOLooper {

		//Used for forward-reverse control via L298 H-Bridge
		DigitalOutput D1A, D1B, D2A, D2B;

		//Vary power to motors via H-Bridge
		PwmOutput speedA, speedB;

		boolean mInvertTurn = false;

		//Start with a PWM duty cycle of 70%
		private static final float INITIAL_DUTY_CYCLE = 0.7f;
		private static final float STEER_MAX = 0.2f;
		private static final float NULL_ZONE = 1.0f;

		float leftDutyCycle = INITIAL_DUTY_CYCLE;
		float rightDutyCycle = INITIAL_DUTY_CYCLE;
		float steer = 0 ;

		float Epsilon = 0f ;
		float EpsilonPrevious = 0f;
		float DeltaEpsilon = 0f;
		float IntegEpsilon = 0f;
		float EpsilonScaleFactor = 16.0f;

		//This is proportional control scale factor
		private static final float KP=0.1f;

		//Differential control scale factor
		private static final float KD=0.0f;

		//Integral control scale factor
		private static final float KI=0.0f;

		//Timestep of control loop in milliseconds
		private static final int Timestep=50;

		float TargetOrient ;
		boolean bTargetSet=false;

		public void moveForward() {
			try {
				D1A.write(true);
				D1B.write(false);
				D2A.write(true);
				D2B.write(false);
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}

		public void moveReverse() {
			try {
				D1A.write(false);
				D1B.write(true);
				D2A.write(false);
				D2B.write(true);
			} catch (ConnectionLostException e) {
				e.printStackTrace();
			}
		}

		protected void setup() throws ConnectionLostException {

			//Make the actual connections to the IOIO board
			// Pins 1 through 4 are motor digital control
			D1A = ioio_.openDigitalOutput(1, false);
			D1B = ioio_.openDigitalOutput(2, false);
			D2A = ioio_.openDigitalOutput(3, false);
			D2B = ioio_.openDigitalOutput(4, false);

			// Pins 5 & 6 are for speed control
			speedA = ioio_.openPwmOutput(5, 100);
			speedB = ioio_.openPwmOutput(6, 100);

			// Initial duty cycle
			speedA.setDutyCycle(INITIAL_DUTY_CYCLE);
			speedB.setDutyCycle(INITIAL_DUTY_CYCLE);

			btnPowerOn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					mThrottle++;
					if (mThrottle > 10) mThrottle = 10;
					moveForward();
				}
			});

			btnPowerSub.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					mThrottle--;
					if (mThrottle < 0) mThrottle = 0;
				}
			});

			btnScaleUp.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					EpsilonScaleFactor *= 2.0f;
				}
			});

			btnScaleDown.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					EpsilonScaleFactor /= 2.0f;
				}
			});

			btnPowerZero.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
						//moveReverse();
					TargetOrient = 0;
					for(int i=0;i<100;i++) {
						TargetOrient += mSensorOrient ;
				    }
					TargetOrient /= 100 ;
					 leftDutyCycle = INITIAL_DUTY_CYCLE;
					 rightDutyCycle = INITIAL_DUTY_CYCLE;
					bTargetSet = true ;
				}
			}
			);

			btnInvertTurn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					mInvertTurn = !mInvertTurn;
				}
			});

			btnStop.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					try {
						D1A.write(false);
						D1B.write(false);
						D2A.write(false);
						D2B.write(false);
					} catch (ConnectionLostException e) {
						e.printStackTrace();
					}
				}
			});

			btnUpdate.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {

					new Thread(new Runnable() {
						public void run() {

							mLeftDutyCycleView.post(new Runnable() {
								public void run() {
									mLeftDutyCycleView
											.setText("LeftDutycycle = "
													+ leftDutyCycle);
								}
							});

							mRightDutyCycleView.post(new Runnable() {
								public void run() {
									mRightDutyCycleView
											.setText("RightDutycycle = "
													+ rightDutyCycle);
								}
							});

							mSteerView.post(new Runnable() {
								public void run() {
									mSteerView.setText("Steer = " + steer);
								}
							});

							mEpsilonScaleFactorView.post( new Runnable() {
								public void run() {
									mEpsilonScaleFactorView.setText("Scale Factor = " + EpsilonScaleFactor);
								}
							});

							mEpsilonView.post( new Runnable() {
								public void run() {
									mEpsilonView.setText("Epsilon = " + Epsilon);
								}
							});

						}
					}).start();
				}
			});

			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(getApplicationContext(), "Connected!",
							Toast.LENGTH_SHORT).show();
				}
			});
		}

		private float clampDutyCycle(float dutycycle) {
			if ( dutycycle < 0.5f)
				return 0.5f;
			else
				if (dutycycle > 0.7f)
					return  0.7f;
				else
					return dutycycle;
		}

		private float clampSteer(float steer) {
			if ( Math.abs(steer) > STEER_MAX )
				return Math.signum(steer)*STEER_MAX;
			else
				return steer ;
		}

		//Calculate circular distance
		//Trickery here to account for negative values.
		private float calcCircularDistance(float target, float orient) {
			float result = orient - target;
			if ( result > 180)
				result -= 360 ;
			if (result < -180)
				result += 360 ;
			return result;
		}

		//---------------------------------------------------------
		//Timing loop, where we get our current Orientation
		//adjust power and repeat every 50 milliseconds
		//---------------------------------------------------------
		public void loop() throws ConnectionLostException {
			float  deltaT ;

			try {
				Thread.sleep(Timestep);

				//Since Timestep is in milliseconds
				deltaT=(float) (Timestep * 1e-3) ;

				Epsilon = calcCircularDistance(TargetOrient,mSensorOrient) ;

				Epsilon /= EpsilonScaleFactor;

				DeltaEpsilon = (Epsilon - EpsilonPrevious)/deltaT ;
				IntegEpsilon += Epsilon ;

				//Limit max amount of correction
				steer = clampSteer(KP*Epsilon + KD * DeltaEpsilon + KI *IntegEpsilon);

				//Correction only if error exceeds NULL_ZONE
				if (bTargetSet & Math.abs(DeltaEpsilon) > NULL_ZONE ) {
					leftDutyCycle  += (steer) ;
					rightDutyCycle -= (steer);
				}

				EpsilonPrevious = Epsilon ;

				leftDutyCycle  = clampDutyCycle( leftDutyCycle  );
				rightDutyCycle = clampDutyCycle( rightDutyCycle );

				if ( mInvertTurn ) {
					speedA.setDutyCycle(leftDutyCycle);
					speedB.setDutyCycle(rightDutyCycle);
				}
				else {
					speedA.setDutyCycle(rightDutyCycle);
					speedB.setDutyCycle(leftDutyCycle);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}
