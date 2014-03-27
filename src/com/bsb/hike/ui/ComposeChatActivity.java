package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ComposeChatAdapter;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.adapters.FriendsAdapter.ViewType;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TagEditText;
import com.bsb.hike.view.TagEditText.TagEditorListener;

public class ComposeChatActivity extends HikeAppStateBaseFragmentActivity implements TagEditorListener, OnClickListener, OnItemClickListener
{
	private static final int MIN_MEMBERS_GROUP_CHAT = 2;

	private static final int CREATE_GROUP_MODE = 1;

	private static final int START_CHAT_MODE = 2;

	private View multiSelectActionBar, groupChatActionBar;

	private TagEditText tagEditText;

	private int composeMode;

	private ComposeChatAdapter adapter;

	int originaltAdapterLength = 0;

	private TextView multiSelectTitle;

	private ListView listView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.compose_chat);
		init();
		setActionBar(getString(R.string.compose_chat_action_bar_title));
	}

	private void init()
	{

		listView = (ListView) findViewById(R.id.list);

		listView.setAdapter(adapter = new ComposeChatAdapter(this));
		adapter.setShowExtraAtFirst(true);
		listView.setOnItemClickListener(this);
		originaltAdapterLength = adapter.getCount();
		initTagEditText();
	}

	private void initTagEditText()
	{
		tagEditText = (TagEditText) findViewById(R.id.composeChatNewGroupTagET);
		tagEditText.setListener(this);
		tagEditText.setMinCharChangeThreshold(2);
		// need to confirm with rishabh --gauravKhanna
		tagEditText.setMinCharChangeThresholdForTag(8);
		tagEditText.setSeparator(TagEditText.SEPARATOR_SPACE);
	}

	@Override
	public void onClick(View arg0)
	{
		switch (arg0.getId())
		{
		case R.id.composeChatNewGroupHeading:
			setMode(CREATE_GROUP_MODE);
			break;

		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		ContactInfo contactInfo = adapter.getItem(arg2);
		// jugaad , coz of pinned listview , discussed with team
		if (ComposeChatAdapter.EXTRA_ID.equals(contactInfo.getId()))
		{
			setMode(CREATE_GROUP_MODE);
			return;
		}
		if (composeMode == CREATE_GROUP_MODE)
		{

			// for SMS users, append SMS text with name
			String name = adapter.getViewTypebasedOnFavType(contactInfo) == ViewType.NOT_FRIEND_SMS.ordinal() ? contactInfo.getName() + " (SMS) " : contactInfo.getName();
			tagEditText.toggleTag(name, contactInfo.getMsisdn(), contactInfo);
		}
		else
		{
			if (FriendsAdapter.SECTION_ID.equals(contactInfo.getId()) || FriendsAdapter.EMPTY_ID.equals(contactInfo.getId()))
			{
				return;
			}

			Utils.startChatThread(this, contactInfo);
		}
	}

	@Override
	public void onBackPressed()
	{
		if (composeMode == CREATE_GROUP_MODE)
		{
			setMode(START_CHAT_MODE);
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void tagRemoved(Object data, String uniqueNess)
	{
		adapter.removeContact((ContactInfo) data);
		if (adapter.getSelectedContactCount() == 0)
		{
			setActionBar(getString(R.string.compose_chat_action_bar_group_mode_title));
		}
	}

	@Override
	public void tagAdded(Object data, String uniqueNess)
	{
		adapter.addContact((ContactInfo) data);
		int selectedCount = adapter.getSelectedContactCount();
		// so we add it only first time
		if (selectedCount == 1)
		{
			setupMultiSelectActionBar();
		}
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, selectedCount));
	}

	@Override
	public void characterAddedAfterSeparator(String characters)
	{
		Toast.makeText(this, characters + " added after sep", Toast.LENGTH_SHORT).show();
		adapter.onQueryChanged(characters);
	}

	@Override
	public void charResetAfterSeperator()
	{
		if (adapter.getCount() != originaltAdapterLength)
		{
			adapter.removeFilter();
		}
	}

	private void setMode(int mode)
	{
		this.composeMode = mode;
		switch (composeMode)
		{
		case CREATE_GROUP_MODE:
			// createGroupHeader.setVisibility(View.GONE);
			adapter.setShowExtraAtFirst(false);
			tagEditText.setVisibility(View.VISIBLE);
			adapter.showCheckBoxAgainstItems(true);
			tagEditText.clear(false);
			adapter.removeFilter();
			setActionBar(getString(R.string.compose_chat_action_bar_group_mode_title));
			break;
		case START_CHAT_MODE:
			// createGroupHeader.setVisibility(View.VISIBLE);
			adapter.setShowExtraAtFirst(true);
			tagEditText.setVisibility(View.GONE);
			tagEditText.clear(false);
			adapter.clearAllSelection(false);
			adapter.removeFilter();
			setActionBar(getString(R.string.compose_chat_action_bar_title));
			break;
		}
	}

	private void createGroup(ArrayList<ContactInfo> selectedContactList, String groupName)
	{

		String groupId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);
		boolean newGroup = false;

		if (TextUtils.isEmpty(groupId))
		{
			// Create new group
			String uid = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.UID_SETTING, "");
			groupId = uid + ":" + System.currentTimeMillis();
			newGroup = true;
		}
		else
		{
			// Group alredy exists. Fetch existing participants.
			newGroup = false;
		}
		Map<String, GroupParticipant> participantList = new HashMap<String, GroupParticipant>();

		for (ContactInfo particpant : selectedContactList)
		{
			GroupParticipant groupParticipant = new GroupParticipant(particpant);
			participantList.put(particpant.getMsisdn(), groupParticipant);
		}
		ContactInfo userContactInfo = Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE));

		GroupConversation groupConversation = new GroupConversation(groupId, 0, null, userContactInfo.getMsisdn(), true);
		groupConversation.setGroupParticipantList(participantList);

		Log.d(getClass().getSimpleName(), "Creating group: " + groupId);
		HikeConversationsDatabase mConversationDb = HikeConversationsDatabase.getInstance();
		mConversationDb.addGroupParticipants(groupId, groupConversation.getGroupParticipantList());
		if (newGroup)
		{
			mConversationDb.addConversation(groupConversation.getMsisdn(), false, groupName, groupConversation.getGroupOwner());
		}

		try
		{
			// Adding this boolean value to show a different system message
			// if its a new group
			JSONObject gcjPacket = groupConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
			gcjPacket.put(HikeConstants.NEW_GROUP, newGroup);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, new ConvMessage(gcjPacket, groupConversation, this, true));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		JSONObject gcjJson = groupConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
		/*
		 * Adding the group name to the packet
		 */
		if (newGroup)
		{
			JSONObject metadata = new JSONObject();
			try
			{
				metadata.put(HikeConstants.NAME, groupName);
				gcjJson.put(HikeConstants.METADATA, metadata);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, gcjJson);

		ContactInfo conversationContactInfo = new ContactInfo(groupId, groupId, groupId, groupId);
		Intent intent = Utils.createIntentFromContactInfo(conversationContactInfo, true);
		intent.setClass(this, ChatThread.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();

	}

	private void setActionBar(String titleString)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (groupChatActionBar == null)
		{
			groupChatActionBar = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
		}
		View backContainer = groupChatActionBar.findViewById(R.id.back);

		TextView title = (TextView) groupChatActionBar.findViewById(R.id.title);
		title.setText(titleString);
		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		actionBar.setCustomView(groupChatActionBar);
	}

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (multiSelectActionBar == null)
		{
			multiSelectActionBar = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);
		}
		Button sendBtn = (Button) multiSelectActionBar.findViewById(R.id.save);
		View closeBtn = multiSelectActionBar.findViewById(R.id.close_action_mode);
		ViewGroup closeContainer = (ViewGroup) multiSelectActionBar.findViewById(R.id.close_container);

		multiSelectTitle = (TextView) multiSelectActionBar.findViewById(R.id.title);
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, 1));

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (adapter.getSelectedContactCount() < MIN_MEMBERS_GROUP_CHAT)
				{
					Toast.makeText(getApplicationContext(), "Select Min 2 members to start group chat", Toast.LENGTH_SHORT).show();
					return;
				}
				createGroup(adapter.getAllSelectedContacts(), "Placeholder");
			}
		});

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});
		actionBar.setCustomView(multiSelectActionBar);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		sendBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
	}
}