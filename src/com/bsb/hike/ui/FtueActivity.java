package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author sidharth
 * 
 */
public class FtueActivity extends HikeAppStateBaseFragmentActivity
{
	public static final int NUM_OF_STICKERS = 4;

	public static final String TAG = "FtueActivity";

	private SharedPreferences accountPrefs;

	private Resources mResources;

	private List<Sticker> stickers = new ArrayList<Sticker>(NUM_OF_STICKERS);

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (Utils.requireAuth(this))
		{
			return;
		}

		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		mResources = this.getResources();
		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		setContentView(R.layout.ftue6);
		String name = accountPrefs.getString(HikeMessengerApp.NAME_SETTING, null);
		setupActionBar(name);
		String nuxStickerDetails = accountPrefs.getString(HikeConstants.NUX_STICKER_DETAILS, null);
		if (null != nuxStickerDetails)
		{
			try
			{
				JSONArray arr = new JSONArray(nuxStickerDetails);
				for (int i = 0; i < arr.length(); ++i)
				{
					JSONObject j = (JSONObject) arr.get(i);
					String category = j.getString(StickerManager.CATEGORY_ID);
					String stickerId = j.getString(StickerManager.STICKER_ID);
					Sticker s = new Sticker(category, stickerId);
					Logger.d(TAG, " stickers category " + category + "  id : " + stickerId);
					stickers.add(s);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setAdapter(new FtueAdapter(this));
		gridview.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
				Sticker st = (Sticker) v.getTag();
				Intent intent = IntentManager.getForwardStickerIntent(FtueActivity.this, st.getStickerId(), st.getCategory().getCategoryId(), true);
				intent.putExtra(StickerManager.STICKER_ID, st.getStickerId());
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				FtueActivity.this.finish();
				startActivity(intent);
			}
		});
	}

	private void setupActionBar(String name)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View ftueActionBar = LayoutInflater.from(this).inflate(R.layout.ftue6_action_bar, null);

		if (actionBar.getCustomView() == ftueActionBar)
		{
			return;
		}

		TextView tv = (TextView) ftueActionBar.findViewById(R.id.ftue_title);
		tv.setText(getString(R.string.ftue_sticker_screen_title, name));
		actionBar.setCustomView(ftueActionBar);
	}

	public class FtueAdapter extends BaseAdapter
	{
		private Context mContext;

		public FtueAdapter(Context c)
		{
			mContext = c;
		}

		@Override
		public int getCount()
		{
			return stickers.size();
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			ImageView imageView;
			if (convertView == null)
			{
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(170, 170));
				imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			}
			else
			{
				imageView = (ImageView) convertView;
			}

			Sticker sticker = stickers.get(position);
			String stickerId = sticker.getStickerId();
			int idx = stickerId.lastIndexOf(".");
			String resName = "sticker_" + stickerId.substring(0, idx);
			Bitmap stickerBitmap = BitmapUtils.getBitmapFromResourceName(getApplicationContext(), resName);
			imageView.setImageDrawable(HikeBitmapFactory.getBitmapDrawable(stickerBitmap));
			imageView.setTag(sticker);
			return imageView;
		}

		@Override
		public Object getItem(int position)
		{
			return null;
		}

		@Override
		public long getItemId(int position)
		{
			return 0;
		}
	}
}
