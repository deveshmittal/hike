package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSDKConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class ConnectedAppsActivity extends HikeAppStateBaseFragmentActivity
{
	private HikeSharedPreferenceUtil authPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		authPrefs = HikeSharedPreferenceUtil.getInstance(getApplicationContext(), HikeAuthActivity.AUTH_SHARED_PREF_NAME);

		setContentView(R.layout.connected_apps_main);

		setupActionBar();

		bindContentAndActions();
	}

	/**
	 * Setup action bar.
	 */
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(getApplicationContext().getString(R.string.connected_apps));
		backContainer.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void bindContentAndActions()
	{
		final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

		BaseAdapter connectedAppsAdapter = new BaseAdapter()
		{
			private String[] connectedPkgs;

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = inflater.inflate(R.layout.connected_apps_list_item, null);
				}

				String[] pkgInfo = connectedPkgs[position].split(":");

				// We have host application package name. Get its name and image from package info
				PackageManager pm = getApplicationContext().getPackageManager();
				try
				{
					PackageInfo packageInfo = pm.getPackageInfo(pkgInfo[0], PackageManager.GET_ACTIVITIES);

					String appName = packageInfo.applicationInfo.loadLabel(getApplicationContext().getPackageManager()).toString();

					Drawable appIcon = packageInfo.applicationInfo.loadIcon(pm);

					((TextView) convertView.findViewById(R.id.text_view_conn_app_title)).setText(appName);

					((TextView) convertView.findViewById(R.id.text_view_conn_app_since)).setText("ver " + packageInfo.versionName);

					((ImageView) convertView.findViewById(R.id.image_view_conn_app_pkg)).setImageDrawable(appIcon);

					((ImageView) convertView.findViewById(R.id.image_view_disconn_app)).setOnClickListener(new DisconnectAppOnClickListener(connectedPkgs[position]));

				}
				catch (PackageManager.NameNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (ArrayIndexOutOfBoundsException e)
				{
					e.printStackTrace();
				}

				return convertView;
			}

			@Override
			public long getItemId(int position)
			{
				return 0;
			}

			@Override
			public Object getItem(int position)
			{
				return null;
			}

			@Override
			public int getCount()
			{
				String connectedPkgCSV = authPrefs.getData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, "");
				if (TextUtils.isEmpty(connectedPkgCSV))
				{
					return 0;
				}
				else
				{
					connectedPkgs = connectedPkgCSV.split(",");
					return connectedPkgs.length;
				}
			}
		};

		((TextView) findViewById(R.id.text_view_connected_apps_numbers)).setText(connectedAppsAdapter.getCount() + " apps connected to hike");

		((ListView) findViewById(R.id.list_view_connected_apps)).setAdapter(connectedAppsAdapter);

	}

	class DisconnectAppOnClickListener implements View.OnClickListener
	{
		private String mAppPkgName;

		private Dialog mDialog;

		private View clickedView;

		public DisconnectAppOnClickListener(String argAppPkgName)
		{
			mAppPkgName = argAppPkgName;
		}

		private DisconnectAppOnClickListener()
		{

		}

		private void disconnectApp()
		{
			String connectedPkgCSV = authPrefs.getData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, "");
			if (TextUtils.isEmpty(connectedPkgCSV))
			{
				// Not possible
			}
			else
			{
				String[] connectedPkgs = connectedPkgCSV.split(",");
				List<String> connectedPkgsList = new ArrayList<String>(Arrays.asList(connectedPkgs));
				for (String pkg : connectedPkgs)
				{
					String[] pkgInfo = pkg.split(":");
					if (pkgInfo[0].equals(mAppPkgName.split(":")[0]))
					{
						connectedPkgsList.remove(pkg);
					}
				}

				String outputCSV = "";
				for (String pkg : connectedPkgsList)
				{
					outputCSV += TextUtils.isEmpty(outputCSV) ? pkg : "," + pkg;
				}

				authPrefs.saveData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, outputCSV);

				authPrefs.removeData(mAppPkgName.split(":")[0]);

				try
				{
					JSONObject analyticsJSON = new JSONObject();

					analyticsJSON.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_DISCONNECT_APP);

					analyticsJSON.put("third_party_app_pkg", mAppPkgName.split(":")[0]);

					Utils.sendLogEvent(analyticsJSON);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				bindContentAndActions();
			}
		}

		@Override
		public void onClick(View v)
		{
			clickedView = v;
			mDialog = HikeDialog.showDialog(ConnectedAppsActivity.this,// this is fine since HikeDialog does not keep any instance with itself
					HikeDialog.HIKE_GENERIC_CONFIRM_DIALOG, new HikeDialog.HikeDialogListener()
					{
						@Override
						public void positiveClicked(Dialog dialog)
						{
							disconnectApp();
						}

						@Override
						public void onSucess(Dialog dialog)
						{
							// Do nothing
						}

						@Override
						public void neutralClicked(Dialog dialog)
						{
							// Do nothing
						}

						@Override
						public void negativeClicked(Dialog dialog)
						{
							mDialog.dismiss();
						}
					}, (Object) null);
			mDialog.show();
		}
	}
}
