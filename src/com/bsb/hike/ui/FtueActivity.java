package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
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

public class FtueActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{
	public static final int NUM_OF_STICKERS = 4;

	public static final String TAG = "FtueActivity";

	private SharedPreferences accountPrefs;

	private List<Pair<Sticker, Integer>> stickers = new ArrayList<Pair<Sticker, Integer>>();

	private String defaultStickerIds[] = { "002_lol.png", "113_whereareyou.png", "069_hi.png", "003_teasing.png" };

	private int defaultStickerResIds[] = { R.drawable.sticker_002_lol, R.drawable.sticker_113_whereareyou, R.drawable.sticker_069_hi, R.drawable.sticker_003_teasing };

	private String defaultStickerCategories[] = { StickerManager.EXPRESSIONS, StickerManager.EXPRESSIONS, StickerManager.HUMANOID, StickerManager.HUMANOID };

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		overridePendingTransition(0, 0);
		super.onCreate(savedInstanceState);
		if (Utils.requireAuth(this))
		{
			return;
		}

		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		setContentView(R.layout.ftue6);

		setupActionBar();

		populateStickerList();

		ImageView img1 = (ImageView) findViewById(R.id.one);
		img1.setImageResource(stickers.get(0).second);
		img1.setTag(stickers.get(0).first);
		img1.setOnClickListener(this);

		ImageView img2 = (ImageView) findViewById(R.id.two);
		img2.setImageResource(stickers.get(1).second);
		img2.setTag(stickers.get(1).first);
		img2.setOnClickListener(this);

		ImageView img3 = (ImageView) findViewById(R.id.three);
		img3.setImageResource(stickers.get(2).second);
		img3.setTag(stickers.get(2).first);
		img3.setOnClickListener(this);

		ImageView img4 = (ImageView) findViewById(R.id.four);
		img4.setImageResource(stickers.get(3).second);
		img4.setTag(stickers.get(3).first);
		img4.setOnClickListener(this);

	}

	@Override
	protected void onPause()
	{
		overridePendingTransition(0, 0);
		super.onPause();
	}
	
	@Override
	public void onClick(View v)
	{
		Sticker st = (Sticker) v.getTag();
		Intent intent = IntentManager.getForwardStickerIntent(FtueActivity.this, st.getStickerId(), st.getCategory().getCategoryId(), true);
		intent.putExtra(StickerManager.STICKER_ID, st.getStickerId());
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		FtueActivity.this.finish();
		startActivity(intent);
	}

	/**
	 * This method populates {@link #stickers} list with the stickers sent by server in response of age and gender which were saved in shared preference
	 */
	private void populateStickerList()
	{
		String nuxStickerDetails = accountPrefs.getString(HikeConstants.NUX_STICKER_DETAILS, null);
		if (null != nuxStickerDetails)
		{
			try
			{
				JSONArray jsonArray = new JSONArray(nuxStickerDetails);
				for (int index = 0; index < jsonArray.length(); ++index)
				{
					JSONObject jObj = (JSONObject) jsonArray.get(index);
					String category = jObj.getString(StickerManager.CATEGORY_ID);
					String stickerId = jObj.getString(StickerManager.STICKER_ID);

					int idx = stickerId.lastIndexOf(".");
					if (idx >= 0)
					{
						String resName = "sticker_" + stickerId.substring(0, idx);
						Resources resources = getApplicationContext().getResources();
						int resourceId = resources.getIdentifier(resName, HikeConstants.DRAWABLE, getApplicationContext().getPackageName());
						if (resourceId != 0)
						{
							Sticker sticker = new Sticker(category, stickerId);
							Logger.d(TAG, " stickers category " + category + "  id : " + stickerId);
							stickers.add(new Pair<Sticker, Integer>(sticker, resourceId));
						}
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		Logger.d(TAG, " size : +" + stickers.size());
		List<Pair<Sticker, Integer>> temp = new ArrayList<Pair<Sticker, Integer>>();

		/*
		 * Adding default stickers if sticker ids sent by server are not correct by checking with already added in stickers to avoid repetition
		 */
		if (stickers.size() < NUM_OF_STICKERS)
		{
			int remaining = NUM_OF_STICKERS - stickers.size();
			for (int i = 0; i < NUM_OF_STICKERS; ++i)
			{
				int defResId = defaultStickerResIds[i];
				boolean flag = false;

				// num of stickers to be shown is small otherwise would have taken a map
				for (int j = 0; j < stickers.size(); ++j)
				{
					int resId = stickers.get(j).second;
					if (resId == defResId)
					{
						flag = true;
						break;
					}
				}

				if (flag == false)
				{
					// flag tells this sticker was already present in stickers list or not
					Sticker sticker = new Sticker(defaultStickerCategories[i], defaultStickerIds[i]);
					temp.add(new Pair<Sticker, Integer>(sticker, defResId));
					remaining--;
				}

				if (remaining == 0)
					break;
			}

			stickers.addAll(temp);
		}

	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View ftueActionBar = LayoutInflater.from(this).inflate(R.layout.ftue6_action_bar, null);

		if (actionBar.getCustomView() == ftueActionBar)
		{
			return;
		}

		TextView tv = (TextView) ftueActionBar.findViewById(R.id.ftue_title);
		String name = accountPrefs.getString(HikeMessengerApp.NAME_SETTING, null);
		tv.setText(getString(R.string.ftue_sticker_screen_title, name));
		actionBar.setCustomView(ftueActionBar);
	}
}
