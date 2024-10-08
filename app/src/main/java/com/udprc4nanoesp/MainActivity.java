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
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	Button btnActAccelerometer, btnActWheel, btnActButtons, btnActTouch;
	WifiManager mainWifi;

	private final Handler handler = new Handler();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.pref, false);

	    setContentView(R.layout.activity_main);

		new ActivityEula(this).show();

		btnActAccelerometer = (Button) findViewById(R.id.button_accel);
	    btnActAccelerometer.setOnClickListener(this);
	    
	    btnActWheel = (Button) findViewById(R.id.button_wheel);
	    btnActWheel.setOnClickListener(this);
	    
	    btnActButtons = (Button) findViewById(R.id.button_buttons);
	    btnActButtons.setOnClickListener(this);

	    btnActTouch = (Button) findViewById(R.id.button_touch);
	    btnActTouch.setOnClickListener(this);
	    
	}

	public void onClick(View v) {
		switch (v.getId()) {
	    case R.id.button_accel:
			Toast.makeText(getApplicationContext(), "Started connecting to ap...", Toast.LENGTH_SHORT).show();
			v.invalidate();
	    	Intent intent_accel = new Intent(this, ActivityAccelerometer.class);
	    	startActivity(intent_accel);
	    	break;
	    case R.id.button_wheel:
			Toast.makeText(getApplicationContext(), "Started connecting to ap...", Toast.LENGTH_SHORT).show();
			v.invalidate();
	    	Intent intent_wheel = new Intent(this, ActivityWheel.class);
	    	startActivity(intent_wheel);
	    	break;
	    case R.id.button_buttons:
			Toast.makeText(getApplicationContext(), "Started connecting to ap...", Toast.LENGTH_SHORT).show();
			v.invalidate();
	    	Intent intent_buttons = new Intent(this, ActivityButtons.class);
	    	startActivity(intent_buttons);
	    	break;
	    case R.id.button_touch:
			Toast.makeText(getApplicationContext(), "Started connecting to ap...", Toast.LENGTH_SHORT).show();
			v.invalidate();
	    	Intent intent_touch = new Intent(this, ActivityTouch.class);
	    	startActivity(intent_touch);
	    	break;
	    default:
	    	break;
	    }
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, SetPreferenceActivity.class);
		startActivityForResult(intent, 0);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Context context;
		// init ap scan...
		WifiManager mainWifi = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		mainWifi.startScan();
	}

}
