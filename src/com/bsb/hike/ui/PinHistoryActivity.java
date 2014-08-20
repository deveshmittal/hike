package com.bsb.hike.ui;

import java.util.List;

import org.json.JSONException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.PinHistoryAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.Conversation.MetaData;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class PinHistoryActivity extends HikeAppStateBaseFragmentActivity implements OnScrollListener, HikePubSub.Listener 
{	
	private PinHistoryAdapter pinAdapter;
	
	private List<ConvMessage> textPins;
	
	private String msisdn;
	
	private ChatTheme chatTheme;
	
	private ImageView backgroundImage;
		
	private HikeConversationsDatabase mDb;

	private Conversation mConversation;

	private long convId;
	
	private ListView mPinListView;
	
	private boolean mLoadingMorePins;
	
	private boolean mReachedEnd;
		
	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED};

	protected void onCreate(Bundle savedInstanceState)
	{
		/*
		* Making the action bar transparent for custom theming.
		*/
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		super.onCreate(savedInstanceState);
		
		initialisePinHistory();
	}	
	
	private void initialisePinHistory()
	{
		setContentView(R.layout.sticky_pins);
		
		msisdn = getIntent().getExtras().getString(HikeConstants.TEXT_PINS);
		
		convId = getIntent().getExtras().getLong(HikeConstants.EXTRA_CONV_ID);

		mPinListView = (ListView) findViewById(android.R.id.list);
		
		backgroundImage = (ImageView) findViewById(R.id.pin_history_background);

		mDb = HikeConversationsDatabase.getInstance();
		
		this.mConversation = mDb.getConversation(msisdn, 0, true);
		
		this.textPins = mDb.getAllPinMessage(0, HikeConstants.MAX_PINS_TO_LOAD_INITIALLY, msisdn, mConversation);
		
		chatTheme = mDb.getChatThemeForMsisdn(msisdn);
		
		mPinListView.setEmptyView(findViewById(android.R.id.empty));
		
		pinAdapter = new PinHistoryAdapter(this, textPins, msisdn, convId, mConversation, true,chatTheme);

		mPinListView.setOnScrollListener(this);
		
		mPinListView.setAdapter(pinAdapter);
		
		if (chatTheme != ChatTheme.DEFAULT)
		{
			backgroundImage.setScaleType(chatTheme.isTiled() ? ScaleType.FIT_XY : ScaleType.CENTER_CROP);
			
			backgroundImage.setImageDrawable(Utils.getChatTheme(chatTheme, this));
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
			e.printStackTrace();
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, mConversation);
		
		View pinEmptyState = getPinEmptyState(chatTheme);
		
		if (pinEmptyState != null)
		{
			ViewGroup empty = (ViewGroup)findViewById(android.R.id.empty);
			
			empty.removeAllViews();
			
			empty.addView(pinEmptyState);
			
			mPinListView.setEmptyView(empty);
		}

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		setupActionBar();
	}
	
	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		chatTheme = db.getChatThemeForMsisdn(msisdn);

		actionBar.setBackgroundDrawable(getResources().getDrawable(chatTheme.headerBgResId()));
		actionBar.setDisplayShowTitleEnabled(true);

		actionBar.setIcon(R.drawable.hike_logo_top_bar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		actionBarView.findViewById(R.id.seprator).setVisibility(View.GONE);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.pin_history);

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}
		
	@Override
	protected void onResume()
	{
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
	}	
	
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) 
	{
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) 
	{
		if (!mReachedEnd && !mLoadingMorePins && textPins != null && !textPins.isEmpty() && (firstVisibleItem + visibleItemCount)  <= totalItemCount - 5)
		{
			mLoadingMorePins = true;
			
			AsyncTask<Void, Void, List<ConvMessage>> asyncTask = new AsyncTask<Void, Void, List<ConvMessage>>()
			{
				@Override
				protected List<ConvMessage> doInBackground(Void... params)
				{
					return mDb.getAllPinMessage(pinAdapter.getCurrentPinsCount(), HikeConstants.MAX_OLDER_PINS_TO_LOAD_EACH_TIME, msisdn, mConversation);
				}

				@Override
				protected void onPostExecute(List<ConvMessage> result)
				{
					if (!result.isEmpty())
					{
						pinAdapter.appendPinstoView(result);
					}
					else
					{
						mReachedEnd = true;
					}
					mLoadingMorePins = false;
				}
			};
			if (Utils.isHoneycombOrHigher())
			{
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else
			{
				asyncTask.execute();
			}
		}
	}
	
	@Override
	public void onEventReceived(String type, Object object) 
	{
		if(mConversation == null)
		{
			return;
		}
		
		if(HikePubSub.MESSAGE_RECEIVED.equals(type))
		{
			final ConvMessage convMsg = (ConvMessage)object;
			
			if(convMsg.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			{
				String msisdn = convMsg.getMsisdn();
				
				if(msisdn != null && msisdn.equals(this.msisdn))
				{
					
					if(pinAdapter != null)
					{
						runOnUiThread(new Runnable() 
						{						
							@Override
							public void run() 
							{
								pinAdapter.addPinMessage(convMsg);
								pinAdapter.notifyDataSetChanged();							
							}
						});
					}
				}
				try 
				{
					mConversation.getMetaData().setUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN, 0);
				}
				catch (JSONException e) 
				{
					e.printStackTrace();
				}
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, mConversation);
			}
		}
	}

	private TextView getPinEmptyState(ChatTheme chatTheme)
	{
		try
		{
			TextView tv = (TextView) LayoutInflater.from(this).inflate(chatTheme.systemMessageLayoutId(), null, false);
			tv.setText(R.string.pinHistoryTutorialText);
			if (chatTheme == ChatTheme.DEFAULT)
			{
				tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pin_empty_state_default, 0, 0, 0);
			}
			else
			{
				tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pin_empty_state, 0, 0, 0);
			}
			tv.setCompoundDrawablePadding(10);
			android.widget.ScrollView.LayoutParams lp = new ScrollView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.gravity = Gravity.CENTER;
			tv.setLayoutParams(lp);
			return tv;
		}
		catch (Exception e)
		{
			// if chat theme starts returning layout id which is not text-view, playSafe
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
	}
}
