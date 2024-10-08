/*
 *  (C) Copyright 2017 Gregor Schlechtriem (http://www.pikoder.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *  	Gregor Schlechtriem
 */

package com.udprc4nanoesp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import static com.udprc4nanoesp.R.layout.activity_touch;

public class ActivityTouch extends Activity {

	private UdpServer udpServer = null;
	private UdpReceiver udpReceiver = null;
	private String udpReceiverHotSpotName;	// hotspot name provided by receiver

	private int motorLeft = 0;
	private int motorRight = 0;
	private boolean show_Debug = false;	// show debug information (from settings)
	private boolean mixing = true;  	// for backward compatibility
	private int xMax = 7;		    	// limit on the X axis from settings  (0-10)
	private int yMax = 5;		    	// limit on the Y axis from settings (0-10)
	private int yThreshold = 50;  		// minimum value of PWM from settings
	private int pwmMax = 126;	   		// maximum value of PWM from settings
	private int xR = 5;					// pivot point from settings

	private String host = "192.168.4.1"; // UDP receiver default address
	private String rx_host = "192.168.4.2"; // UDP sender default address for responses (smart phone IP address) -> not relevant!
	private String rx_port = "12000";		// application default port for responses to smart phone
	private String localPort = "12000";
	private String remotePort = "12001";			// application default port
	private String networkPasskey = "PASSWORD";
	private String commandLeft;
	private String commandRight;
	private String cLeftHeader;
	private String cRightHeader;
	private final String cChannelNeutral = "7F";
	private final int iChannelNeutral = 127;

	private boolean sCommand;
	private int iLastLeft = 255;
	private int iLastRight = 255;

	// additional specific defs for processing...
	private final static int BIG_CIRCLE_SIZE = 180;
	private final static int FINGER_CIRCLE_SIZE = 20;
	private int xRperc = 50;	// pivot point from settings

	private static boolean suppressMessage = false;

	private String FM_AUTO_PWM, FM_LEARNING_PWM, FM_MANUAL_PWM;
	private int	intFMMode;
	private String strFMChannel;

	// trim function defs
	static int iChannel1_neutral = 0;
	static int iChannel2_neutral = 0;

	private static String TAG = ActivityTouch.class.getSimpleName();

	// fail safe related definitions
	Timer timer = null;
	TimerTask timerTask = null;
	int iTimeOut = 0;

	// defs for PPM frame position
	private String DirectionRXChannel;
	private String ThrottleRXChannel;

	MyView v1;
	Joystick vStickLeft;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		Resources res = getResources();
		mixing = res.getBoolean(R.bool.pref_Mixing);	// enable mixing
		remotePort = res.getString(R.string.default_PORT); // application default port
		localPort = res.getString(R.string.default_rxPORT);
		host = res.getString(R.string.default_IP);   // UDP receiver default address
		networkPasskey = res.getString(R.string.default_networkPasskey);
		udpReceiverHotSpotName = res.getString(R.string.default_udpReceiverHotSpotName);	// hotspot name provided by receiver
		DirectionRXChannel = res.getString(R.string.default_channelLeftRight);
		ThrottleRXChannel = res.getString(R.string.default_channelForwardBackward);

		setContentView(activity_touch);
        loadPref();

		String uriString = "udp://" + host + ":" + remotePort + "/";

		commandLeft = "FF0" + String.valueOf(Integer.valueOf(DirectionRXChannel) -1) + "7F";    // make sure we init the string to avoid problem w/o mixing
		commandRight = "FF0" + String.valueOf(Integer.valueOf(ThrottleRXChannel) -1) + "7F";

		cRightHeader = "FF0" + String.valueOf(Integer.valueOf(ThrottleRXChannel) -1);
		cLeftHeader = "FF0" + String.valueOf(Integer.valueOf(DirectionRXChannel) -1);

		RelativeLayout canvasLayout = (RelativeLayout) findViewById(R.id.canvasLayout);
		vStickLeft = new Joystick(this, 0, 1, mixing, true, true, false, false, mHandler);
		canvasLayout.addView(vStickLeft);

		udpServer = new UdpServer(this, mHandler, host, remotePort);
	}

	private class MyHandler extends Handler {
		private final WeakReference<ActivityTouch> mActivity;

		public MyHandler(ActivityTouch activity) {
			mActivity = new WeakReference<ActivityTouch>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			boolean suppressMessage = false;
			ActivityTouch activity = mActivity.get();
			if (activity != null) {
				switch (msg.what) {
					case UdpServer.RECEIVE_MESSAGE:								// uplink from receiver
						String strIncom = (String) msg.obj;
						Log.d(TAG, "Received: " + strIncom );
						if (strIncom.equals("?")) { // received '?' as errormessage
							Toast.makeText(activity.getBaseContext(), "Last command could not be executed!", Toast.LENGTH_SHORT).show();
						} else {
							if (strIncom.equals("!")) { // did receive acknowledge
							} else {
								if (strIncom.length() >= 4) {
									if (iChannel1_neutral ==1) {
										iChannel1_neutral = Integer.parseInt(strIncom.toString());
										Log.d(TAG, "Neutral set to: " + String.valueOf(iChannel1_neutral));
									} else {
										iChannel2_neutral = Integer.parseInt(strIncom.toString());
									}
								}
							}
						}
						break;
                    case UdpServer.COMMAND_STRING:								// if message is received
                        strIncom = (String) msg.obj;
                        Log.d(TAG, "Received: " + strIncom + " Will tranmit" );
                        udpServer.sendCommand(strIncom);
                        break;
					case UdpServer.WIFI_NOT_AVAILABLE:
						Log.d(UdpServer.TAG, "Wifi not available (Android system setting). Exit");
						Toast.makeText(activity.getBaseContext(), "Wifi not available (Android system setting). Exit", Toast.LENGTH_LONG).show();
						activity.finish();
						break;
					case UdpServer.MISSING_PERMISSION_TO_ACCESS_LOCATION:
						Log.d(UdpServer.TAG, "Missing Permission to access position (Android >= 6). Exit");
						Toast.makeText(activity.getBaseContext(), "Missing Android permission - Access to location required. Exit)", Toast.LENGTH_LONG).show();
						activity.finish();
						break;
					case UdpServer.RECEIVER_NOT_ON_SCAN_LIST:
						Log.d(UdpServer.TAG, "AP is not on current scan list. Exit");
						Toast.makeText(activity.getBaseContext(), "Receiver is not offered by Android system scan", Toast.LENGTH_LONG).show();
						activity.finish();
						break;
					case UdpServer.WIFI_INCORRECT_ADDRESS:
						Log.d(UdpServer.TAG, "Connection attempt failed");
						Toast.makeText(activity.getBaseContext(), "Failed to connect to Hotspot", Toast.LENGTH_SHORT).show();
						break;
					case UdpServer.WIFI_REQUEST_ENABLE:
						Log.d(UdpServer.TAG, "Started connecting to ap...");
						Toast.makeText(activity.getBaseContext(), "Started connecting to ap...", Toast.LENGTH_SHORT).show();
						break;
					case UdpServer.WIFI_SOCKET_FAILED:
						if (!suppressMessage) Toast.makeText(activity.getBaseContext(), "Timeout receiving IP address...", Toast.LENGTH_SHORT).show();
						activity.finish();
						break;
					case UdpServer.USER_STOP_INITIATED:
						suppressMessage = true;
						break;
					case UdpServer.WIFI_HOTSPOT_NOT_FOUND:
						if (!suppressMessage) Toast.makeText(activity.getBaseContext(), "Hotspot not found", Toast.LENGTH_SHORT).show();
						activity.finish();
						break;
					case UdpServer.WIFI_HOTSPOT_CONNECTING:
						if (!suppressMessage) Toast.makeText(activity.getBaseContext(), "Connection ok, loading IP address... ", Toast.LENGTH_SHORT).show();
						break;
					case UdpServer.WIFI_HOTSPOT_CONNECTED:
						String localHostIpAddress = Formatter.formatIpAddress(msg.arg1);
						Log.v(TAG, "Received local IP address: " + localHostIpAddress);
						if (!suppressMessage) Toast.makeText(activity.getBaseContext(), "Connected to receiver.", Toast.LENGTH_SHORT).show();
						break;
				}

			}

		}
	}

	// get the heartbeat going
	public void startTimer() {
		Log.v(TAG, "starting Timer");
		timer = new Timer();
		timerTask = new TimerTask() {
			@Override
			public void run() {
				sentCommand("0x" + commandLeft + commandRight);
			}
		};
		timer.scheduleAtFixedRate(timerTask,0,iTimeOut/2); 	// play it safe...
	}

	public void stopTimer() {
		//stop the timer, if it's not already null
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	private final MyHandler mHandler = new MyHandler(this);
	
	private final static Runnable sRunnable = new Runnable() {
		public void run() { }
	};
	
	
	class MyView extends View {

		Paint fingerPaint, borderPaint, textPaint, alphaPaint;

        int dispWidth;
        int dispHeight;

		Bitmap bitmap, logo;
		int imageW,imageH,imagelW,imagelH;
		Point size;

		float x;
        float y;
        
        float xcirc;
        float ycirc;
        
        // variables for drag
        boolean drag = false;
        float dragX = 0;
        float dragY = 0;

        public MyView(Context context) {
        	super(context);
        	fingerPaint = new Paint();
        	fingerPaint.setAntiAlias(true);
        	fingerPaint.setColor(Color.RED);
                
        	borderPaint = new Paint();
			borderPaint.setColor(Color.parseColor("#555A6E"));
        	borderPaint.setAntiAlias(true);
        	borderPaint.setStyle(Style.STROKE);
        	borderPaint.setStrokeWidth(3);
        	
	        textPaint = new Paint(); 
	        textPaint.setColor(Color.WHITE); 
	        textPaint.setStyle(Style.FILL); 
	        textPaint.setColor(Color.BLACK); 
	        textPaint.setTextSize(14);

			alphaPaint = new Paint();
			alphaPaint.setAlpha(75); // equals 0.3 on a range 0 .. 255

			Display display = getWindowManager().getDefaultDisplay();
			size = new Point();
			display.getSize(size);
			Log.d(TAG, String.valueOf("display width:"+size.x+"  height"+size.y));
			float scale = (float) 0.75 * (float) size.y/ (float) imagelH;
			Log.d(TAG, String.valueOf("scale: "+ scale));
		}


        protected void onDraw(Canvas canvas) {
			super.onDraw(canvas); // the default drawing
			dispWidth = (int) Math.round(size.x/4);
        	dispHeight = (int) Math.round(size.y/3);
        	if(!drag){
        		x = dispWidth;
        		y = dispHeight;
        		fingerPaint.setColor(Color.RED);
        	}

            canvas.drawCircle(x, y, FINGER_CIRCLE_SIZE, fingerPaint);              
			canvas.drawRect(dispWidth - BIG_CIRCLE_SIZE, dispHeight - BIG_CIRCLE_SIZE, dispWidth + BIG_CIRCLE_SIZE, dispHeight + BIG_CIRCLE_SIZE, borderPaint);

            if(show_Debug){
	            canvas.drawText(String.valueOf("X:"+xcirc), 10, 75, textPaint);
	            canvas.drawText(String.valueOf("Y:"+(-ycirc)), 10, 95, textPaint);
	            canvas.drawText(String.valueOf("Motor:"+String.valueOf(motorLeft)+" "+String.valueOf(motorRight)), 10, 115, textPaint);
            }
		}

        @Override
        public boolean onTouchEvent(MotionEvent event) {
        	
        	// coordinate of Touch-event
        	float evX = event.getX();
        	float evY = event.getY();

            // transform to relative coordinates
        	xcirc = event.getX() - dispWidth;
        	ycirc = event.getY() - dispHeight;
        	Log.d(TAG, String.valueOf("X:"+this.getRight()+" Y:"+dispHeight));
            	   
        	// float radius = (float) Math.sqrt(Math.pow(Math.abs(xcirc),2)+Math.pow(Math.abs(ycirc),2));

			boolean inside = (Math.abs(xcirc) <= BIG_CIRCLE_SIZE) && (Math.abs(ycirc) <= BIG_CIRCLE_SIZE);

        	switch (event.getAction()) {

        	case MotionEvent.ACTION_DOWN:        
        		if(inside){
        			x = evX;
        			y = evY;
        			fingerPaint.setColor(Color.GREEN);
        			CalcMotor(xcirc,ycirc);
        			invalidate();
        			drag = true;
        		}
        		break;

        	case MotionEvent.ACTION_MOVE:
        		// if drag mode is enabled 
        		if (drag && inside) {
        			x = evX;
        			y = evY;
        			fingerPaint.setColor(Color.GREEN);
        			//temptxtMotor = CalcMotor(xcirc,ycirc);
        			CalcMotor(xcirc,ycirc);
        			invalidate();
        		}
        		break;

        	// touch completed 
        	case MotionEvent.ACTION_UP:
        		// turn off the drag mode 
        		xcirc = 0;
        		ycirc = 0; 
        		drag = false;
        		//temptxtMotor = CalcMotor(xcirc,ycirc);
        		CalcMotor(xcirc,ycirc);
        		invalidate();
        		break;
        	}
        	return true;
        }
	}

	/**
	 * This method converts dp unit to equivalent pixels, depending on device density.
	 *
	 * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
	 * @param context Context to get resources and device specific display metrics
	 * @return A float value to represent px equivalent to dp depending on device density
	 */
	public static float convertDpToPixel(float dp, Context context){
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
		return px;
	}

	/**
	 * This method converts device specific pixels to density independent pixels.
	 *
	 * @param px A value in px (pixels) unit. Which we need to convert into db
	 * @param context Context to get resources and device specific display metrics
	 * @return A float value to represent dp equivalent to px value
	 */
	public static float convertPixelsToDp(float px, Context context){
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
		return dp;
	}

	public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BIT MAP
		matrix.postScale(scaleWidth, scaleHeight);

		// "RECREATE" THE NEW BITMAP
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
		return resizedBitmap;
	}

	private void CalcMotor(float calc_x, float calc_y){

		int xAxis = Math.round(calc_x*pwmMax/BIG_CIRCLE_SIZE);
		int yAxis = Math.round(calc_y*pwmMax/BIG_CIRCLE_SIZE);

		Log.d(TAG, String.valueOf("xAxis:"+xAxis+"  yAxis"+yAxis));

		int xR = Math.round(BIG_CIRCLE_SIZE*xRperc/100);		// calculate the value of pivot point

		if (mixing) {
			if (xAxis > 0) {
				motorRight = yAxis;
				if (Math.abs(Math.round(calc_x)) > xR) {
					motorLeft = Math.round((calc_x - xR) * pwmMax / (BIG_CIRCLE_SIZE - xR));
					motorLeft = Math.round(-motorLeft * yAxis / pwmMax);
				} else motorLeft = yAxis - yAxis * xAxis / pwmMax;
			} else if (xAxis < 0) {
				motorLeft = yAxis;
				if (Math.abs(Math.round(calc_x)) > xR) {
					motorRight = Math.round((Math.abs(calc_x) - xR) * pwmMax / (BIG_CIRCLE_SIZE - xR));
					motorRight = Math.round(-motorRight * yAxis / pwmMax);
				} else motorRight = yAxis - yAxis * Math.abs(xAxis) / pwmMax;
			} else if (xAxis == 0) {
				motorLeft = yAxis;
				motorRight = yAxis;
			}

			if (motorLeft > 0) {
				if (motorLeft > pwmMax) motorLeft = pwmMax;
				motorLeft = motorLeft + iChannelNeutral;
			} else {
				if (motorLeft < -pwmMax) motorLeft = -pwmMax;
				motorLeft = motorLeft + iChannelNeutral;
			}

			if (motorRight > 0) {
				if (motorRight > pwmMax) motorRight = pwmMax;
				motorRight = -motorRight + iChannelNeutral;
			} else {
				if (motorRight < -pwmMax) motorRight = -pwmMax;
				motorRight = -motorRight + iChannelNeutral;
			}
		} else {
			motorLeft = iChannelNeutral + xAxis;
			motorRight = iChannelNeutral - yAxis;
		}

		// format command strings for miniSSC processing...
		if (motorRight < 0x10) {
			commandRight = cRightHeader + "0" + String.format("%X",(byte) motorRight);
		} else {
			commandRight = cRightHeader + String.format("%X", (byte) motorRight);
		}
		if (motorLeft < 0x10) {
			commandLeft = cLeftHeader + "0" + String.format("%X",(byte) motorLeft);
		} else {
			commandLeft = cLeftHeader + String.format("%X", (byte) motorLeft);
		}

		// evaluate thresholds...
		sCommand = false;
		if (Math.abs(motorLeft-iLastLeft) > xMax) sCommand = true;
		if (Math.abs(motorRight-iLastRight) > yMax) sCommand = true;

		if (sCommand) {
			sentCommand("0x" + commandLeft + commandRight);
			iLastLeft = motorLeft;
			iLastRight = motorRight;
		}

	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	loadPref();
    }
	
	@Override
    protected void onResume() {
		super.onResume();
		suppressMessage = false;
		udpServer.udpConnect(udpReceiverHotSpotName, networkPasskey);
		if (udpReceiver == null) {
			Log.v(TAG, "Restarting receiver...");
			String uriString = "udp://" + rx_host + ":" + rx_port + "/";
			Uri uri = Uri.parse(uriString);
			udpReceiver = new UdpReceiver();
			udpReceiver.runUdpReceiver(uri, this, mHandler);
		}
		if (iTimeOut > 0) {
			startTimer();
		}
	}

    @Override
    protected void onPause() {
    	super.onPause();
		suppressMessage = true;
		stopTimer();
		udpServer.udpServer_onPause();
		if (udpReceiver != null) {
			udpReceiver.stopUdpReceiver();
			udpReceiver = null;
		}
	}

	private void sentCommand(String commandString) {
		udpServer.sendCommand(commandString);
	}

    private void loadPref(){
		SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		host = mySharedPreferences.getString("pref_IP_address", host);
		remotePort = mySharedPreferences.getString("pref_Port_number", remotePort);
		mixing = mySharedPreferences.getBoolean("pref_Mixing_active", true);
		xMax = Integer.parseInt(mySharedPreferences.getString("pref_xMax", String.valueOf(xMax)));
		xR = Integer.parseInt(mySharedPreferences.getString("pref_xR", String.valueOf(xR)));
		yMax = Integer.parseInt(mySharedPreferences.getString("pref_yMax", String.valueOf(yMax)));
		yThreshold = Integer.parseInt(mySharedPreferences.getString("pref_yThreshold", String.valueOf(yThreshold)));
		pwmMax = Integer.parseInt(mySharedPreferences.getString("pref_pwmMax", String.valueOf(pwmMax)));
		show_Debug = mySharedPreferences.getBoolean("pref_Debug", false);
		xRperc = Integer.parseInt(mySharedPreferences.getString("pref_xRperc", String.valueOf(xRperc)));
		udpReceiverHotSpotName = mySharedPreferences.getString("pref_udpReceiverHotSpotName",udpReceiverHotSpotName);
		networkPasskey = mySharedPreferences.getString("pref_networkPasskey",networkPasskey);
		localPort = mySharedPreferences.getString("pref_rx_Port_number",localPort);
		FM_AUTO_PWM = mySharedPreferences.getString("defaultFlightMode_AUTO_PWMValue", getString(R.string.defaultFM_AUTO_PWM));
		FM_LEARNING_PWM = mySharedPreferences.getString("defaultFlightMode_LEARNING_PWMValue", getString(R.string.defaultFM_LEARNING_PWM));
		FM_MANUAL_PWM = mySharedPreferences.getString("defaultFlightMode_MANUAL_PWMValue", getString(R.string.defaultFM_MANUAL_PWM));
		intFMMode = Integer.valueOf(mySharedPreferences.getString("defaultFlightMode", getString(R.string.default_FlightMode)));
		strFMChannel = mySharedPreferences.getString("defaultFlightModeChannel", getString(R.string.default_FlightModeOutput));
		DirectionRXChannel = mySharedPreferences.getString("pref_channelLeftRight", DirectionRXChannel);
		ThrottleRXChannel = mySharedPreferences.getString("pref_channelForwardBackward", ThrottleRXChannel);
	}
}
