package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Utils;

/**
 * This class is reponsible for displaying "Connected apps" screen in Settings. Also takes care of underlying functionality.
 * 
 * @author AtulM
 * 
 */
public class ConnectedAppsActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{

	/** The auth prefs. */
	private HikeSharedPreferenceUtil authPrefs;

	/** The connected app list. */
	private ArrayList<ConnectedApp> connectedAppList;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bsb.hike.utils.HikeAppStateBaseFragmentActivity#onCreate(android.os.Bundle)
	 */
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

	/**
	 * Initializes the data.
	 */
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

	/**
	 * Bind content and actions.
	 */
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

	/**
	 * Disconnect application
	 * 
	 * @param appPkgName
	 *            the app pkg name
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
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
	 * Model class.
	 * 
	 * @author Atul M
	 */
	class ConnectedApp
	{

		/** The package name. */
		private String packageName;

		/** The title. */
		private String title;

		/** The app icon. */
		private Drawable appIcon;

		/** The version. */
		private String version;

		/**
		 * Gets the package name.
		 * 
		 * @return the package name
		 */
		public String getPackageName()
		{
			return packageName;
		}

		/**
		 * Sets the package name.
		 * 
		 * @param packageName
		 *            the new package name
		 */
		public void setPackageName(String packageName)
		{
			this.packageName = packageName;
		}

		/**
		 * Gets the title.
		 * 
		 * @return the title
		 */
		public String getTitle()
		{
			return title;
		}

		/**
		 * Sets the title.
		 * 
		 * @param title
		 *            the new title
		 */
		public void setTitle(String title)
		{
			this.title = title;
		}

		/**
		 * Gets the app icon.
		 * 
		 * @return the app icon
		 */
		public Drawable getAppIcon()
		{
			return appIcon;
		}

		/**
		 * Sets the app icon.
		 * 
		 * @param appIcon
		 *            the new app icon
		 */
		public void setAppIcon(Drawable appIcon)
		{
			this.appIcon = appIcon;
		}

		/**
		 * Gets the version.
		 * 
		 * @return the version
		 */
		public String getVersion()
		{
			return version;
		}

		/**
		 * Sets the version.
		 * 
		 * @param version
		 *            the new version
		 */
		public void setVersion(String version)
		{
			this.version = version;
		}
	}
}
