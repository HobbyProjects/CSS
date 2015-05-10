package com.gsm_ccs_client;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity {

	AtomicInteger m_msgId = new AtomicInteger();
	GoogleCloudMessaging m_gcm;
	Context m_context;
	String m_regid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_item);
		
		if(!initialize()) {
			Log.e(Globals.TAG, "Failed to initalize GCM. Fatal! Please inspect the messages above.");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return super.onOptionsItemSelected(item);
	}

	public boolean initialize() {
		Log.i(Globals.TAG, "GCM initialize");

		// GCM startup
		m_gcm = GoogleCloudMessaging.getInstance(this);
		m_context = getApplicationContext();

		if (checkPlayServices()) {
			// Retrieve registration id from local storage
			m_regid = getRegistrationId(m_context);

			if (TextUtils.isEmpty(m_regid)) {
				registerInBackground();
			} else {
				Log.i(Globals.TAG,
						"Registration ID found in shared preferences");
			}
		} else {
			Log.i(Globals.TAG, "No valid Google Play Services APK found.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check the device to make sure it has the Google Play Services APK. If it
	 * doesn't, display a dialog that allows users to download the APK from the
	 * Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						Globals.PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(Globals.TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Gets the current registration ID for application on GCM service, if there
	 * is one.
	 * <p>
	 * If result is empty, the app needs to register.
	 * 
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(Globals.PREFS_PROPERTY_REG_ID,
				"");
		if (registrationId == null || registrationId.equals("")) {
			Log.i(Globals.TAG, "Registration not found in shared perferences");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(Globals.PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(Globals.TAG, "App version changed. Registered: "
					+ registeredVersion + " now: " + currentVersion);
			return "";
		}
		return registrationId;
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGcmPreferences(Context context) {
		// Storing the registration ID in shared preferences.
		return getSharedPreferences(Globals.PREFS_NAME, Context.MODE_PRIVATE);
	}
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and the app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					if (m_gcm == null) {
						m_gcm = GoogleCloudMessaging.getInstance(m_context);
					}
					m_regid = m_gcm.register(Globals.GCM_SENDER_ID);
					msg = "Device registered, registration ID=" + m_regid;

					sendRegistrationIdToBackend();

					// Persist the regID - no need to register again.
					storeRegistrationId(m_context, m_regid);
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				Log.i(Globals.TAG, msg);
			}
		}.execute(null, null, null);
	}
	
	/**
     * Sends the registration ID to the 3rd party server via an upstream 
     * GCM message.
     */
	private void sendRegistrationIdToBackend() {
		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				String msg = "";
				try {
					Bundle data = new Bundle();
					data.putString("action",
							"com.gsm_ccs_client.REGISTER");
					String id = Integer.toString(m_msgId.incrementAndGet());
					m_gcm.send(Globals.GCM_SENDER_ID + "@gcm.googleapis.com", id,
							Globals.GCM_TIME_TO_LIVE, data);
					msg = "Sent registration";
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				Toast.makeText(m_context, msg, Toast.LENGTH_SHORT).show();
				Log.i(Globals.TAG, msg);
			}
		}.execute(null,null,null);
	}
	
	/**
	 * Stores the registration ID and the app versionCode in the application's
	 * {@code SharedPreferences}.
	 * 
	 * @param context
	 *            application's context.
	 * @param regId
	 *            registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getGcmPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i(Globals.TAG, "Storing regId on app version: " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(Globals.PREFS_PROPERTY_REG_ID, regId);
		editor.putInt(Globals.PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}
}
