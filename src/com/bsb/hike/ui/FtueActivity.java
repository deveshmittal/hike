package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.bsb.hike.analytics.Event;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
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

	List<Pair<Sticker, Integer>> stickers;
	
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

		setContentView(R.layout.ftue6);

		setupActionBar();

		stickers = populateStickerList();
		
		initializeImageView((ImageView) findViewById(R.id.one), stickers, 0);
		initializeImageView((ImageView) findViewById(R.id.two), stickers, 1);
		initializeImageView((ImageView) findViewById(R.id.three), stickers, 2);
		initializeImageView((ImageView) findViewById(R.id.four), stickers, 3);
	}

	private void initializeImageView(ImageView img, List<Pair<Sticker, Integer>> stickers, int index)
	{
		img.setImageResource(stickers.get(index).second);
		img.setTag(index);
		img.setOnClickListener(this);
	}
	
	private List<Pair<Sticker, Integer>> prepareDefaultStickerList()
	{
		List<Pair<Sticker, Integer>> defaultStickersList = new ArrayList<Pair<Sticker, Integer>>();
		
		String defaultStickerIds[] = { "042_sahihai.png", "006_dl_balleballe.png", "063_boss.png", "040_waah.png" };

		int defaultStickerResIds[] = { R.drawable.sticker_042_sahihai, R.drawable.sticker_006_dl_balleballe, R.drawable.sticker_063_boss, R.drawable.sticker_040_waah };

		String defaultStickerCategories[] = { "indian" , "delhi", "indian", "indian" };
		
		for (int i = 0; i < NUM_OF_STICKERS; i++)
		{
			Sticker sticker = new Sticker(defaultStickerIds[i], defaultStickerCategories[i]);
			defaultStickersList.add(new Pair<Sticker, Integer>(sticker, defaultStickerResIds[i]));
		}

		return defaultStickersList;
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
		int index = (Integer) v.getTag();
		Pair<Sticker, Integer> pair = stickers.get(index);
		Sticker st = pair.first;
		int stickerResId = pair.second;
		String stId = st.getStickerId();
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.NUX_STICKER_CLICKED + "_" + stId);
		Event e = new Event(metadata);
		e.setEventAttributes(HikeConstants.UI_EVENT, HikeConstants.LogEvent.CLICK);			
		HAManager.getInstance(getApplicationContext()).record(e);

		Intent intent = IntentManager.getForwardStickerIntent(FtueActivity.this, stId, st.getCategoryId(), true);
		intent.putExtra(StickerManager.STICKER_RES_ID, stickerResId);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		FtueActivity.this.finish();
		startActivity(intent);
	}

	/**
	 * This method populates {@link #stickers} list with the stickers sent by server in response of age and gender which were saved in shared preference
	 */
	private List<Pair<Sticker, Integer>> populateStickerList()
	{
		String nuxStickerDetails = HikeSharedPreferenceUtil.getInstance(FtueActivity.this).getData(HikeConstants.NUX_STICKER_DETAILS, null);
		List<Pair<Sticker, Integer>> defaultStickersList = prepareDefaultStickerList();
		/*
		 * if server doesn't send any nux details we should show our default one
		 */
		if (null == nuxStickerDetails)
		{
			return defaultStickersList;
		}
		List<Pair<Sticker, Integer>> stickers = new ArrayList<Pair<Sticker,Integer>>(NUM_OF_STICKERS);
		try
		{
			JSONArray jsonArray = new JSONArray(nuxStickerDetails);
			for (int index = 0; index < jsonArray.length(); ++index)
			{
				JSONObject jObj = (JSONObject) jsonArray.get(index);
				
				String categoryId = jObj.optString(StickerManager.CATEGORY_ID, null);
				if(TextUtils.isEmpty(categoryId))
				{
					continue;
				}
				
				String stickerId = jObj.getString(StickerManager.STICKER_ID);
				if(TextUtils.isEmpty(stickerId))
				{
					continue;
				}
				stickerId = stickerId.toLowerCase();

				int idx = stickerId.lastIndexOf(".");
				if (idx < 0)
				{
					continue;
				}
				String resName = "sticker_" + (new String(stickerId)).substring(0, idx);
				Resources resources = getApplicationContext().getResources();
				int resourceId = resources.getIdentifier(resName, HikeConstants.DRAWABLE, getApplicationContext().getPackageName());
				if (resourceId != 0)
				{
					Sticker sticker = new Sticker(categoryId, stickerId);
					Logger.d(TAG, " stickers categoryId " + categoryId + "  id : " + stickerId);
					stickers.add(new Pair<Sticker, Integer>(sticker, resourceId));
				}
			}
			
			if (stickers.size() == NUM_OF_STICKERS)
			{
				return stickers;
			}
			
			/*
			 * if server sends any invalid sticker, which is not present in apk
			 * We need to send one of our default list sticker
			 */
			for (Pair<Sticker, Integer> pair: stickers)
			{
				if(defaultStickersList.contains(pair))
				{
					defaultStickersList.remove(pair);
				}
			}
			stickers.addAll(defaultStickersList);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return stickers;
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View ftueActionBar = LayoutInflater.from(this).inflate(R.layout.ftue6_action_bar, null);

		TextView tv = (TextView) ftueActionBar.findViewById(R.id.ftue_title);
		String name = HikeSharedPreferenceUtil.getInstance(FtueActivity.this).getData(HikeMessengerApp.NAME_SETTING, null);
		tv.setText(getString(R.string.ftue_sticker_screen_title, name));
		actionBar.setCustomView(ftueActionBar);
	}
}
