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
import android.view.View.OnClickListener;
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
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSDKConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

public class ConnectedAppsActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{
	private HikeSharedPreferenceUtil authPrefs;

	private ArrayList<ConnectedApp> connectedAppList;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		authPrefs = HikeSharedPreferenceUtil.getInstance(getApplicationContext(), HikeAuthActivity.AUTH_SHARED_PREF_NAME);

		setContentView(R.layout.connected_apps_main);

		setupActionBar();

		initData();

		bindContentAndActions();
	}

	private void initData()
	{
		connectedAppList = new ArrayList<ConnectedApp>();

		String connectedPkgCSV = authPrefs.getData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, "");

		String[] connectedPkgs = connectedPkgCSV.split(",");

		// We have host application package name. Get its name and image from package info
		PackageManager pm = getApplicationContext().getPackageManager();

		for (String connPkg : connectedPkgs)
		{
			ConnectedApp connApp = new ConnectedApp();

			String[] pkgInfo = connPkg.split(":");

			try
			{
				PackageInfo packageInfo = pm.getPackageInfo(pkgInfo[0], PackageManager.GET_ACTIVITIES);
				connApp.setTitle(packageInfo.applicationInfo.loadLabel(getApplicationContext().getPackageManager()).toString());
				connApp.setAppIcon(packageInfo.applicationInfo.loadIcon(pm));
				connApp.setPackageName(connPkg);
				connApp.setVersion(packageInfo.versionName);
			}
			catch (PackageManager.NameNotFoundException e)
			{
				e.printStackTrace();
			}

			connectedAppList.add(connApp);
		}

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
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = inflater.inflate(R.layout.connected_apps_list_item, null);
				}

				// We have host application package name. Get its name and image from package info
				try
				{

					((TextView) convertView.findViewById(R.id.text_view_conn_app_title)).setText(connectedAppList.get(position).getTitle());

					((TextView) convertView.findViewById(R.id.text_view_conn_app_since)).setText("ver " + connectedAppList.get(position).getVersion());

					((ImageView) convertView.findViewById(R.id.image_view_conn_app_pkg)).setImageDrawable(connectedAppList.get(position).getAppIcon());

					ImageView image_view_disconn_app = ((ImageView) convertView.findViewById(R.id.image_view_disconn_app));

					image_view_disconn_app.setTag(connectedAppList.get(position).getPackageName());

					image_view_disconn_app.setOnClickListener(ConnectedAppsActivity.this);

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
				return connectedAppList.size();
			}
		};

		((TextView) findViewById(R.id.text_view_connected_apps_numbers)).setText(connectedAppsAdapter.getCount() + " apps connected to hike");

		((ListView) findViewById(R.id.list_view_connected_apps)).setAdapter(connectedAppsAdapter);

	}

	private void disconnectApp(String appPkgName)
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
				if (pkgInfo[0].equals(appPkgName.split(":")[0]))
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

			authPrefs.removeData(appPkgName.split(":")[0]);

			try
			{
				JSONObject analyticsJSON = new JSONObject();

				analyticsJSON.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_DISCONNECT_APP);

				analyticsJSON.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, appPkgName.split(":")[0]);

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
		try
		{
			final String pkg = (String) v.getTag();
			final CustomAlertDialog alertDialog = new CustomAlertDialog(ConnectedAppsActivity.this);
			alertDialog.setTitle(null);
			alertDialog.setBody(getString(R.string.are_you_sure));
			alertDialog.setOkButton(getString(R.string.yes), new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					disconnectApp(pkg);
				}
			});
			alertDialog.setCancelButton(getString(R.string.cancel), new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					alertDialog.dismiss();
				}
			});
			alertDialog.show();
		}
		catch (ClassCastException cce)
		{
			cce.printStackTrace();
		}
	}

	/**
	 * Model class
	 * 
	 * @author Atul M
	 * 
	 */
	class ConnectedApp
	{
		private String packageName;

		private String title;

		private Drawable appIcon;

		private String version;

		public String getPackageName()
		{
			return packageName;
		}

		public void setPackageName(String packageName)
		{
			this.packageName = packageName;
		}

		public String getTitle()
		{
			return title;
		}

		public void setTitle(String title)
		{
			this.title = title;
		}

		public Drawable getAppIcon()
		{
			return appIcon;
		}

		public void setAppIcon(Drawable appIcon)
		{
			this.appIcon = appIcon;
		}

		public String getVersion()
		{
			return version;
		}

		public void setVersion(String version)
		{
			this.version = version;
		}
	}
}
