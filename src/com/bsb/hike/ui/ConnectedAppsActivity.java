package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import com.bsb.hike.R.string;
import com.bsb.hike.models.ConnectedApp;

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

	private ListView listView;

	private BaseAdapter connectedAppsAdapter;

	private TextView text_view_connected_apps_numbers;

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

		connectedAppsAdapter = new BaseAdapter()
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

					image_view_disconn_app.setTag(position);

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

		text_view_connected_apps_numbers = ((TextView) findViewById(R.id.text_view_connected_apps_numbers));
		text_view_connected_apps_numbers.setText(connectedAppsAdapter.getCount() == 1 ? String.format(getString(R.string.connected_apps_to_hike), connectedAppsAdapter.getCount())
				: getString(R.string.connected_app_to_hike));

		listView = ((ListView) findViewById(R.id.list_view_connected_apps));
		listView.setAdapter(connectedAppsAdapter);
	}

	/**
	 * Disconnect application
	 * 
	 * @param index
	 */
	private void disconnectApp(int index)
	{
		ConnectedApp disconnAppObj = connectedAppList.remove(index);

		String connectedPkgCSV = authPrefs.getData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, "");

		if (TextUtils.isEmpty(connectedPkgCSV))
		{
			// Not possible
		}
		else
		{
			StringBuilder sb = null;

			for (ConnectedApp app : connectedAppList)
			{
				if (sb == null)
				{
					sb = new StringBuilder(app.getPackageName());
				}
				else
				{
					sb.append("," + app.getPackageName());
				}
			}

			authPrefs.saveData(HikeAuthActivity.AUTH_SHARED_PREF_PKG_KEY, sb.toString());

			authPrefs.removeData(disconnAppObj.getTitle());

			try
			{
				JSONObject analyticsJSON = new JSONObject();

				analyticsJSON.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.SDK_DISCONNECT_APP);

				analyticsJSON.put(HikeConstants.Extras.SDK_THIRD_PARTY_PKG, disconnAppObj.getTitle());

				Utils.sendLogEvent(analyticsJSON);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			invalidateUI();
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
			final int position = (Integer) v.getTag();
			final CustomAlertDialog alertDialog = new CustomAlertDialog(ConnectedAppsActivity.this);
			alertDialog.setHeader(getString(R.string.are_you_sure));
			alertDialog.setBody(getString(R.string.confirm_disconnect_app));
			alertDialog.setOkButton(getString(R.string.yes), new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					disconnectApp(position);
					alertDialog.dismiss();
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
	 * Refresh UI
	 */
	private void invalidateUI()
	{
		connectedAppsAdapter.notifyDataSetChanged();
		text_view_connected_apps_numbers.setText(connectedAppsAdapter.getCount() == 1 ? String.format(getString(R.string.connected_apps_to_hike), connectedAppsAdapter.getCount())
				: getString(R.string.connected_app_to_hike));
	}
}
