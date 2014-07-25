package com.bsb.hike.ui.fragments;

import java.util.List;

import org.json.JSONException;

import android.graphics.Bitmap;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ImageView.ScaleType;

import com.actionbarsherlock.app.SherlockListFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.adapters.PinHistoryAdapter;
import com.bsb.hike.adapters.PinHistoryAdapter.PinHistoryItemsListener;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.Conversation.MetaData;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class PinHistoryFragment extends SherlockListFragment implements PinHistoryItemsListener
{
	private PinHistoryAdapter PHadapter;
			
	private List<ConvMessage> textPins;
	
	private String msisdn;
	
	private ChatTheme chatTheme;
	
	private ImageView backgroundImage;
		
	private HikeConversationsDatabase mDb;

	private Conversation mConversation;

	private long convId;

	private HikePubSub mPubSub;
		
	public PinHistoryFragment()
	{
	}

	@Override
	public void setArguments(Bundle args) 
	{
		super.setArguments(args);
		
		msisdn = args.getString(HikeConstants.TEXT_PINS);
		
		convId = args.getLong(HikeConstants.EXTRA_CONV_ID);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.sticky_pins, null);
		
		ListView pinsList = (ListView) parent.findViewById(android.R.id.list);
		
		backgroundImage = (ImageView) parent.findViewById(R.id.pin_history_background);

		mDb = HikeConversationsDatabase.getInstance();
		
		this.mConversation = mDb.getConversation(msisdn, HikeConstants.MAX_PINS_TO_LOAD_INITIALLY, true);
		
		this.textPins = mDb.getAllPinMessage(0, HikeConstants.MAX_PINS_TO_LOAD_INITIALLY, msisdn, mConversation);
		
		chatTheme = mDb.getChatThemeForMsisdn(msisdn);
		
		pinsList.setEmptyView(parent.findViewById(android.R.id.empty));
		
		return parent;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) 
	{
		super.onViewCreated(view, savedInstanceState);
						
		if (chatTheme != ChatTheme.DEFAULT)
		{
			backgroundImage.setScaleType(chatTheme.isTiled() ? ScaleType.FIT_XY : ScaleType.CENTER_CROP);
			backgroundImage.setImageDrawable(getChatTheme(chatTheme));
		}
		else
		{
			backgroundImage.setImageResource(chatTheme.bgResId());
		}
		MetaData metadata = mConversation.getMetaData();
		try
		{
			metadata.setUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN, 0);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mPubSub.publish(HikePubSub.UPDATE_PIN_METADATA,mConversation);
		
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		if (PHadapter != null)
		{
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		PHadapter = new PinHistoryAdapter(getActivity(), textPins, msisdn, convId, mConversation, this);
		
		setListAdapter(PHadapter);		
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mPubSub = HikeMessengerApp.getPubSub();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
	}

	public Drawable getChatTheme(ChatTheme chatTheme)
	{
		/*
		 * for xhdpi and above we should not scale down the chat theme nodpi asset for hdpi and below to save memory we should scale it down
		 */
		int inSampleSize = 1;
		if(!chatTheme.isTiled() && Utils.densityMultiplier < 2)
		{
			inSampleSize = 2;
		}
		
		Bitmap b = HikeBitmapFactory.decodeSampledBitmapFromResource(getResources(), chatTheme.bgResId(), inSampleSize);

		BitmapDrawable bd = HikeBitmapFactory.getBitmapDrawable(getResources(), b);

		Logger.d(getClass().getSimpleName(), "chat themes bitmap size= " + BitmapUtils.getBitmapSize(b));

		if (bd != null && chatTheme.isTiled())
		{
			bd.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		}

		return bd;
	}

	@Override
	public void onLastItemRequested() 
	{		
		this.textPins = mDb.getAllPinMessage(PHadapter.getCount(), HikeConstants.MAX_OLDER_PINS_TO_LOAD_EACH_TIME, msisdn, mConversation);
		
		if(textPins != null && textPins.size() <= 0)
		{
			PHadapter.removeItemViewListener();
		}
		else
		{
			this.PHadapter.appendPinstoView(textPins);
		}
	}
}
