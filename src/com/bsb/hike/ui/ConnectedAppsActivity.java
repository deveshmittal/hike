package com.bsb.hike.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class ConnectedAppsActivity extends HikeAppStateBaseFragmentActivity
{
	private HikeSharedPreferenceUtil authPrefs;

	private String numberOfApps;

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

					((TextView) convertView.findViewById(R.id.text_view_conn_app_since)).setText("Since " + pkgInfo[1]);

					((ImageView) convertView.findViewById(R.id.image_view_conn_app_pkg)).setImageDrawable(appIcon);

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

		((TextView) findViewById(R.id.text_view_connected_apps_numbers)).setText(connectedAppsAdapter.getCount() + " APPS");

		((ListView) findViewById(R.id.list_view_connected_apps)).setAdapter(connectedAppsAdapter);

	}
}
