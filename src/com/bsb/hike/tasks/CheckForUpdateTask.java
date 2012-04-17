package com.bsb.hike.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Utils;

/**
 * Task which fetches the minimum version of the App that the user must be having and the latest version of the App that is present on the server.
 * @author rs
 *
 */
public class CheckForUpdateTask extends AsyncTask<Void, Void, Void> {
	
	private Context context;
	private Editor editor;
	private int minVersion;
	private int currentVersion;
	private int appVersion;
	// URL for checking the current version of the App
	private static final String CHECK_FOR_UPDATE_URL = "http://192.168.11.7/a.php";
		
	public CheckForUpdateTask(Context context) {
		this.context = context;
	}
	
	@Override
	protected void onPreExecute() {
		editor = context.getSharedPreferences(HikeMessengerApp.UPDATE_SETTING, 0).edit();
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		
		try {
			URL url = new URL(CHECK_FOR_UPDATE_URL);
			InputStream is = new BufferedInputStream(url.openConnection().getInputStream());
			
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line;
			while((line = br.readLine())!=null)
			{
				sb.append(line);
			}
			br.close();
			is.close();
			
			Log.d("HikeService", "Response: "+sb.toString());
			
			JSONObject jsonObject = new JSONObject(sb.toString());
			
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			
			minVersion = Utils.convertVersionToInt(jsonObject.getString("minVersion"));
			currentVersion = Utils.convertVersionToInt(jsonObject.getString("currentVersion"));
			appVersion = Utils.convertVersionToInt(pInfo.versionName);
			
			Log.d("HikeService", "MinVersion: "+minVersion);
			Log.d("HikeService", "CurrentVersion: "+currentVersion);
			Log.d("HikeService", "AppVersion: "+appVersion);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if(appVersion < minVersion)
		{
			editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.CRITICAL_UPDATE);
		}
		else if(appVersion < currentVersion)
		{
			editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.UPDATE_AVAILABLE);
		}
		else
		{
			editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
		}
		editor.commit();
	}
	
}
