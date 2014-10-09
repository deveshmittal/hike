package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.modules.contactmgr.ContactManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ComposeChatAdapter;
import com.bsb.hike.adapters.FriendsAdapter;
import com.bsb.hike.adapters.FriendsAdapter.FriendsListFetchedCallback;
import com.bsb.hike.adapters.FriendsAdapter.ViewType;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.tasks.InitiateMultiFileTransferTask;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.LastSeenScheduler;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;
import com.bsb.hike.view.CustomTypeFace;
import com.bsb.hike.view.TagEditText;
import com.bsb.hike.view.TagEditText.TagEditorListener;
import com.google.android.gms.internal.ad;
import com.google.android.gms.internal.ar;

public class ComposeChatActivity extends HikeAppStateBaseFragmentActivity implements TagEditorListener, OnItemClickListener, HikePubSub.Listener, OnScrollListener
{
	private static final String SELECT_ALL_MSISDN="all";
	
	private static int MIN_MEMBERS_GROUP_CHAT = 2;

	private static final int CREATE_GROUP_MODE = 1;

	private static final int START_CHAT_MODE = 2;
	
	private static final int MULTIPLE_FWD = 3;

	private View multiSelectActionBar, groupChatActionBar;

	private TagEditText tagEditText;

	private int composeMode;

	private ComposeChatAdapter adapter;

	int originalAdapterLength = 0;

	private TextView multiSelectTitle;

	private ListView listView;

	private TextView title;

	private boolean createGroup;

	private boolean isForwardingMessage;

	private boolean isSharingFile;

	private String existingGroupId;

	private volatile InitiateMultiFileTransferTask fileTransferTask;

	private ProgressDialog progressDialog;

	private LastSeenScheduler lastSeenScheduler;

	private String[] hikePubSubListeners = { HikePubSub.MULTI_FILE_TASK_FINISHED, HikePubSub.APP_FOREGROUNDED, HikePubSub.LAST_SEEN_TIME_UPDATED,
			HikePubSub.LAST_SEEN_TIME_BULK_UPDATED, HikePubSub.CONTACT_SYNC_STARTED, HikePubSub.CONTACT_SYNCED };

	private int previousFirstVisibleItem;

	private int velocity;

	private long previousEventTime;
	
	private HikePubSub mPubSub;

	private boolean showingMultiSelectActionBar = false;
	
	private List<ContactInfo> recentContacts;
	
	private boolean selectAllMode;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		/* force the user into the reg-flow process if the token isn't set */
		if (Utils.requireAuth(this))
		{
			return;
		}

		// TODO this is being called everytime this activity is created. Way too
		// often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		createGroup = getIntent().getBooleanExtra(HikeConstants.Extras.CREATE_GROUP, false);
		isForwardingMessage = getIntent().getBooleanExtra(HikeConstants.Extras.FORWARD_MESSAGE, false);
		isSharingFile = getIntent().getType() != null;

		// Getting the group id. This will be a valid value if the intent
		// was passed to add group participants.
		existingGroupId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);

		if (!shouldInitiateFileTransfer())
		{
			Toast.makeText(this, getString(R.string.max_num_files_reached, FileTransferManager.getInstance(this).getTaskLimit()), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		setContentView(R.layout.compose_chat);

		Object object = getLastCustomNonConfigurationInstance();

		if (object instanceof InitiateMultiFileTransferTask)
		{
			fileTransferTask = (InitiateMultiFileTransferTask) object;
			progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));
		}

		if (Intent.ACTION_SEND.equals(getIntent().getAction()) || Intent.ACTION_SENDTO.equals(getIntent().getAction())
				|| Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction()))
		{
			isForwardingMessage = true;
		}

		setActionBar();

		init();
		mPubSub = HikeMessengerApp.getPubSub();
		mPubSub.addListeners(this, hikePubSubListeners);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if(!showingMultiSelectActionBar)
			getSupportMenuInflater().inflate(R.menu.compose_chat_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == R.id.refresh_contacts)
		{
			if(HikeMessengerApp.syncingContacts)
				return super.onOptionsItemSelected(item);
			if(!Utils.isUserOnline(this))
			{
				Utils.showNetworkUnavailableDialog(this);
				return super.onOptionsItemSelected(item);
			}
			Intent contactSyncIntent = new Intent(HikeService.MQTT_CONTACT_SYNC_ACTION);
			contactSyncIntent.putExtra(HikeConstants.Extras.MANUAL_SYNC, true);
			sendBroadcast(contactSyncIntent);
			Utils.sendUILogEvent(HikeConstants.LogEvent.COMPOSE_REFRESH_CONTACTS);
		}
		return super.onOptionsItemSelected(item);
	}


	private boolean shouldInitiateFileTransfer()
	{
		if (isSharingFile)
		{
			if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction()))
			{
				ArrayList<Uri> imageUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				if (imageUris.size() > FileTransferManager.getInstance(this).remainingTransfers())
				{
					return false;
				}
			}
			else if (getIntent().hasExtra(Intent.EXTRA_STREAM))
			{
				if (FileTransferManager.getInstance(this).remainingTransfers() == 0)
				{
					return false;
				}
			}
		}
		else if (isForwardingMessage)
		{
			if (getIntent().hasExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT))
			{
				String jsonString = getIntent().getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
				try
				{
					JSONArray multipleMsgFwdArray = new JSONArray(jsonString);

					int fileCount = 0;

					for (int i = 0; i < multipleMsgFwdArray.length(); i++)
					{
						JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);

						if (msgExtrasJson.has(HikeConstants.Extras.FILE_PATH))
						{
							fileCount++;
						}
					}

					if (fileCount > FileTransferManager.getInstance(this).remainingTransfers())
					{
						return false;
					}
				}
				catch (JSONException e)
				{
				}
			}
		}
		return true;
	}

	private void init()
	{
		listView = (ListView) findViewById(R.id.list);
		String sendingMsisdn = getIntent().getStringExtra(HikeConstants.Extras.PREV_MSISDN);

		adapter = new ComposeChatAdapter(this, listView, isForwardingMessage, (isForwardingMessage && !isSharingFile), existingGroupId, sendingMsisdn, friendsListFetchedCallback);
		adapter.setEmptyView(findViewById(android.R.id.empty));
		adapter.setLoadingView(findViewById(R.id.spinner));
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		listView.setOnScrollListener(this);

		originalAdapterLength = adapter.getCount();

		initTagEditText();

		if (existingGroupId != null)
		{
			MIN_MEMBERS_GROUP_CHAT = 1;
		}
		if (isForwardingMessage && !isSharingFile)
		{
			setMode(MULTIPLE_FWD);
		}
		else
		{
			setMode(getIntent().hasExtra(HikeConstants.Extras.GROUP_ID) || existingGroupId != null ? CREATE_GROUP_MODE : START_CHAT_MODE);
		}
		
		adapter.setIsCreatingOrEditingGroup(this.composeMode == CREATE_GROUP_MODE);

		adapter.executeFetchTask();
	}

	private void initTagEditText()
	{
		tagEditText = (TagEditText) findViewById(R.id.composeChatNewGroupTagET);
		tagEditText.setListener(this);
		tagEditText.setMinCharChangeThreshold(1);
		// need to confirm with rishabh --gauravKhanna
		tagEditText.setMinCharChangeThresholdForTag(8);
		tagEditText.setSeparator(TagEditText.SEPARATOR_SPACE);
	}
	
	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		if(adapter != null)
		{
			adapter.getIconLoader().setExitTasksEarly(true);
		}
	}
	
	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		if(adapter != null)
		{
			adapter.getIconLoader().setExitTasksEarly(false);
			adapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onDestroy()
	{
		if (progressDialog != null)
		{
			progressDialog.dismiss();
			progressDialog = null;
		}

		if (lastSeenScheduler != null)
		{
			lastSeenScheduler.stop(true);
			lastSeenScheduler = null;
		}

		HikeMessengerApp.getPubSub().removeListeners(this, hikePubSubListeners);
		super.onDestroy();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (fileTransferTask != null)
		{
			return fileTransferTask;
		}
		else
		{
			return null;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		final ContactInfo contactInfo = adapter.getItem(arg2);

		// jugaad , coz of pinned listview , discussed with team
		if (ComposeChatAdapter.EXTRA_ID.equals(contactInfo.getId()))
		{
			Intent intent = new Intent(this, CreateNewGroupActivity.class);
			startActivity(intent);
			return;
		}
		if (composeMode == CREATE_GROUP_MODE)
		{

			if (adapter.isContactPresentInExistingParticipants(contactInfo))
			{
				// basicly it will work when you add participants to existing group via typing numbers
				showToast("You have already added this contact in group");

				return;
			}
			else if (adapter.getSelectedContactCount() >= HikeConstants.MAX_CONTACTS_IN_GROUP && !adapter.isContactAdded(contactInfo))
			{
				showToast(getString(R.string.maxContactInGroupErr, HikeConstants.MAX_CONTACTS_IN_GROUP));
				return;
			}
			// for SMS users, append SMS text with name
			int viewtype = adapter.getItemViewType(arg2);
			if (contactInfo.getName() == null)
			{
				contactInfo.setName(contactInfo.getMsisdn());
			}
			String name = viewtype == ViewType.NOT_FRIEND_SMS.ordinal() ? contactInfo.getName() + " (SMS) " : contactInfo.getName();
			tagEditText.toggleTag(name, contactInfo.getMsisdn(), contactInfo);
		}
		else
		{
			Logger.i("composeactivity", contactInfo.getId() + " - id of clicked");
			if (FriendsAdapter.SECTION_ID.equals(contactInfo.getId()) || FriendsAdapter.EMPTY_ID.equals(contactInfo.getId()))
			{
				return;
			}

			if (isForwardingMessage)
			{
				// for SMS users, append SMS text with name
				int viewtype = adapter.getItemViewType(arg2);
				if (contactInfo.getName() == null)
				{
					contactInfo.setName(contactInfo.getMsisdn());
				}
				
				if(selectAllMode){
					tagEditText.clear(false);
					if(adapter.isContactAdded(contactInfo)){
						adapter.removeContact(contactInfo);
					}else{
						adapter.addContact(contactInfo);
					}
					tagEditText.toggleTag(getString(R.string.selected_count,adapter.getSelectedContactCount()), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);
				}else{
				String name = viewtype == ViewType.NOT_FRIEND_SMS.ordinal() ? contactInfo.getName() + " (SMS) " : contactInfo.getName();
				tagEditText.toggleTag(name, contactInfo.getMsisdn(), contactInfo);
				}
			}
			else
			{
				/*
				 * This would be true if the user entered a stealth msisdn and tried starting a chat with him/her in non stealth mode.
				 */
				if (HikeMessengerApp.isStealthMsisdn(contactInfo.getMsisdn()))
				{
					int stealthMode = HikeSharedPreferenceUtil.getInstance(this).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
					if (stealthMode != HikeConstants.STEALTH_ON)
					{
						return;
					}
				}

				Utils.startChatThread(this, contactInfo);
				finish();
			}
		}
	}

	@Override
	public void tagRemoved(Object data, String uniqueNess)
	{
		if(selectAllMode){
		((CheckBox) findViewById(R.id.select_all_cb)).setChecked(false);;
		}else{
			if(data instanceof ContactInfo){
				adapter.removeContact((ContactInfo) data);
			}
		}
		if (adapter.getCurrentSelection() == 0)
		{
			setActionBar();
			invalidateOptionsMenu();
		}
		else
		{
			multiSelectTitle.setText(getString(R.string.gallery_num_selected, adapter.getCurrentSelection()));
		}
	}

	@Override
	public void tagAdded(Object data, String uniqueNess)
	{
		String dataString = null;
		if(data instanceof ContactInfo){
		adapter.addContact((ContactInfo) data);
		}else if(data instanceof String)
		{
			dataString = (String) data;
		}

		setupMultiSelectActionBar();
		invalidateOptionsMenu();
		
		int selectedCount = adapter.getCurrentSelection();
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, selectedCount));
		
	}

	@Override
	public void characterAddedAfterSeparator(String characters)
	{
		adapter.onQueryChanged(characters);
	}

	@Override
	public void charResetAfterSeperator()
	{
		adapter.removeFilter();
	}

	private void setMode(int mode)
	{
		this.composeMode = mode;
		switch (composeMode)
		{
		case CREATE_GROUP_MODE:
			// createGroupHeader.setVisibility(View.GONE);
			adapter.showCheckBoxAgainstItems(true);
			tagEditText.clear(false);
			adapter.removeFilter();
			adapter.clearAllSelection(true);
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_group_mode);
			break;
		case START_CHAT_MODE:
			// createGroupHeader.setVisibility(View.VISIBLE);
			tagEditText.clear(false);
			adapter.clearAllSelection(false);
			adapter.removeFilter();
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_chat_mode);
			return;
		case MULTIPLE_FWD:
			// createGroupHeader.setVisibility(View.GONE);
			adapter.showCheckBoxAgainstItems(true);
			tagEditText.clear(false);
			adapter.removeFilter();
			adapter.clearAllSelection(true);
			adapter.setStatusForEmptyContactInfo(R.string.compose_chat_empty_contact_status_group_mode);
			// select all bottom text
			setupForSelectAll();
			break;
		}
		setTitle();
	}
	private void setupForSelectAll(){
		View selectAllCont = findViewById(R.id.select_all_container);
		selectAllCont.setVisibility(View.VISIBLE);
		final TextView tv = (TextView) selectAllCont.findViewById(R.id.select_all_text);
		tv.setText(getString(R.string.select_all_hike));
		CheckBox cb = (CheckBox) selectAllCont.findViewById(R.id.select_all_cb);
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					// call adapter select all
					selectAllMode = true;
					tv.setText(getString(R.string.unselect_all_hike));
					adapter.clearAllSelection(true);
					adapter.selectAllContacts(true);
					tagEditText.clear(false);
					tagEditText.toggleTag(getString(R.string.selected_count,adapter.getSelectedContactCount()), SELECT_ALL_MSISDN, SELECT_ALL_MSISDN);
					
				}else{
					// call adapter unselect all
					selectAllMode = false;
					tv.setText(getString(R.string.select_all_hike));
					adapter.selectAllContacts(false);
					tagEditText.clear(true);
					setActionBar();
					invalidateOptionsMenu();
					
				}
				
			}
		});
		
	}

	private void createGroup(ArrayList<ContactInfo> selectedContactList)
	{

		String groupName = getIntent().getStringExtra(HikeConstants.Extras.GROUP_NAME);
		String groupId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);
		boolean newGroup = false;

		if (TextUtils.isEmpty(groupId))
		{
			// Create new group
			groupId = getIntent().getStringExtra(HikeConstants.Extras.GROUP_ID);
			newGroup = true;
		}
		else
		{
			// Group alredy exists. Fetch existing participants.
			newGroup = false;
		}
		Map<String, PairModified<GroupParticipant, String>> participantList = new HashMap<String, PairModified<GroupParticipant, String>>();

		for (ContactInfo particpant : selectedContactList)
		{
			GroupParticipant groupParticipant = new GroupParticipant(particpant);
			participantList.put(particpant.getMsisdn(), new PairModified<GroupParticipant, String>(groupParticipant, groupParticipant.getContactInfo().getNameOrMsisdn()));
		}
		ContactInfo userContactInfo = Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE));

		GroupConversation groupConversation = new GroupConversation(groupId, null, userContactInfo.getMsisdn(), true);
		groupConversation.setGroupParticipantList(participantList);

		Logger.d(getClass().getSimpleName(), "Creating group: " + groupId);
		HikeConversationsDatabase mConversationDb = HikeConversationsDatabase.getInstance();
		mConversationDb.addRemoveGroupParticipants(groupId, groupConversation.getGroupParticipantList(), false);
		if (newGroup)
		{
			mConversationDb.addConversation(groupConversation.getMsisdn(), false, groupName, groupConversation.getGroupOwner());
			ContactManager.getInstance().insertGroup(groupConversation.getMsisdn(),groupName);
		}

		try
		{
			// Adding this boolean value to show a different system message
			// if its a new group
			JSONObject gcjPacket = groupConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
			gcjPacket.put(HikeConstants.NEW_GROUP, newGroup);
			ConvMessage msg = new ConvMessage(gcjPacket, groupConversation, this, true);
			ContactManager.getInstance().updateGroupRecency(groupId, msg.getTimestamp());
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, msg);
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

				String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
				String fileName = Utils.getTempProfileImageFileName(groupId);
				File groupImageFile = new File(directory, fileName);

				if (groupImageFile.exists())
				{
					metadata.put(HikeConstants.REQUEST_DP, true);
				}

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

	private void setActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (groupChatActionBar == null)
		{
			groupChatActionBar = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);
		}
		if (actionBar.getCustomView() == groupChatActionBar)
		{
			return;
		}
		View backContainer = groupChatActionBar.findViewById(R.id.back);

		title = (TextView) groupChatActionBar.findViewById(R.id.title);
		groupChatActionBar.findViewById(R.id.seprator).setVisibility(View.GONE);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{

				onBackPressed();
			}
		});
		setTitle();
		if(HikeMessengerApp.syncingContacts)
		{
			// For showing progress bar when activity is closed and opened again
			showProgressBarContactsSync(View.VISIBLE);
		}

		actionBar.setCustomView(groupChatActionBar);

		showingMultiSelectActionBar = false;
	}

	private void setTitle()
	{
		if (createGroup)
		{
			title.setText(R.string.new_group);
		}
		else if (isSharingFile)
		{
			title.setText(R.string.share_file);
		}
		else if (isForwardingMessage)
		{
			title.setText(R.string.forward);
		}
		else if (!TextUtils.isEmpty(existingGroupId))
		{
			title.setText(R.string.add_group);
		}
		else
		{
			title.setText(R.string.new_chat);
		}
	}

	private void setupMultiSelectActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		if (multiSelectActionBar == null)
		{
			multiSelectActionBar = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);
		}
		View sendBtn = multiSelectActionBar.findViewById(R.id.done_container);
		View closeBtn = multiSelectActionBar.findViewById(R.id.close_action_mode);
		ViewGroup closeContainer = (ViewGroup) multiSelectActionBar.findViewById(R.id.close_container);

		multiSelectTitle = (TextView) multiSelectActionBar.findViewById(R.id.title);
		multiSelectTitle.setText(getString(R.string.gallery_num_selected, 1));

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (isForwardingMessage)
				{
					forwardConfirmation(adapter.getAllSelectedContacts());
				}
				else
				{
 				int selected = adapter.getCurrentSelection();
					if (selected < MIN_MEMBERS_GROUP_CHAT)
					{
						Toast.makeText(getApplicationContext(), "Select Min " + MIN_MEMBERS_GROUP_CHAT + " member(s) to start group chat", Toast.LENGTH_SHORT).show();
						return;
					}
					createGroup(adapter.getAllSelectedContacts());
				}
			}
		});

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				setMode(composeMode);
				if(selectAllMode)
				{
					View selectAllCont = findViewById(R.id.select_all_container);
					CheckBox cb = (CheckBox) selectAllCont.findViewById(R.id.select_all_cb);
					cb.setChecked(false);
				}
				setActionBar();
				invalidateOptionsMenu();
			}
		});

		if(HikeMessengerApp.syncingContacts)
		{
			showProgressBarContactsSync(View.VISIBLE);
		}
		actionBar.setCustomView(multiSelectActionBar);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		sendBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));

		showingMultiSelectActionBar = true;
	}

	private void forwardConfirmation(final ArrayList<ContactInfo> arrayList)
	{
		final CustomAlertDialog forwardConfirmDialog = new CustomAlertDialog(this);
		if (isSharingFile)
		{
			forwardConfirmDialog.setHeader(R.string.share);
			forwardConfirmDialog.setBody(getForwardConfirmationText(arrayList, false));
		}
		else
		{
			forwardConfirmDialog.setHeader(R.string.forward);
			forwardConfirmDialog.setBody(getForwardConfirmationText(arrayList, true));
		}
		View.OnClickListener dialogOkClickListener = new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				forwardConfirmDialog.dismiss();
				forwardMultipleMessages(arrayList);
			}
		};

		forwardConfirmDialog.setOkButton(R.string.ok, dialogOkClickListener);
		forwardConfirmDialog.setCancelButton(R.string.cancel);
		forwardConfirmDialog.show();
	}
	
	private String getForwardConfirmationText(ArrayList<ContactInfo> arrayList, boolean forwarding)
	{
		if(forwarding)
		{
			return getResources().getString(R.string.forward_to, arrayList.size());
		}
		StringBuilder sb = new StringBuilder();

		int lastIndex = arrayList.size()-1;

		boolean moreNamesThanMaxCount = false;
		if (lastIndex < 0)
		{
			lastIndex = 0;
		}
		else if (lastIndex == 1)
		{
			/*
			 * We increment the last index if its one since we can accommodate another name in this case.
			 */
			//lastIndex++;
			moreNamesThanMaxCount = true;
		}
		else if (lastIndex > 0)
		{
			moreNamesThanMaxCount = true;
		}

		for (int i = arrayList.size() - 1; i >= lastIndex; i--)
		{
			sb.append(arrayList.get(i).getFirstName());
			if (i > lastIndex + 1)
			{
				sb.append(", ");
			}
			else if (i == lastIndex + 1)
			{
				if (moreNamesThanMaxCount)
				{
					sb.append(", ");
				}
				else
				{
					sb.append(" and ");
				}
			}
		}
		String readByString = sb.toString();
		if (moreNamesThanMaxCount)
		{
			
				return getResources().getString(R.string.share_with_names_numbers, readByString, lastIndex);
			
		}
		else
		{
			
				return getResources().getString(R.string.share_with, readByString);
			
			
		}
	}

	private void forwardMultipleMessages(ArrayList<ContactInfo> arrayList)
	{
		if (getIntent().hasExtra(HikeConstants.Extras.PREV_MSISDN))
		{
			Intent presentIntent = getIntent();
			String id = getIntent().getStringExtra(HikeConstants.Extras.PREV_MSISDN);
			Intent intent = Utils.createIntentFromMsisdn(id, true);
			intent.setClass(this, ChatThread.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			forwardMessageAsPerType(presentIntent, intent,arrayList);
			startActivity(intent);
			finish();
		}
	}

	private void forwardMessageAsPerType(Intent presentIntent, Intent intent, ArrayList<ContactInfo> arrayList)
	{
		// update contact info sequence as per conversation ordering
		arrayList = updateContactInfoOrdering(arrayList);
		String type = presentIntent.getType();

		if (Intent.ACTION_SEND_MULTIPLE.equals(presentIntent.getAction()))
		{
			if (type != null)
			{
				ArrayList<Uri> imageUris = presentIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				if (imageUris != null)
				{
					boolean showMaxFileToast = false;

					ArrayList<Pair<String, String>> fileDetails = new ArrayList<Pair<String, String>>(imageUris.size());
					for (Uri fileUri : imageUris)
					{
						Logger.d(getClass().getSimpleName(), "File path uri: " + fileUri.toString());
						String fileUriStart = "file:";
						String fileUriString = fileUri.toString();

						String filePath;
						if (fileUriString.startsWith(fileUriStart))
						{
							File selectedFile = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
							/*
							 * Done to fix the issue in a few Sony devices.
							 */
							filePath = selectedFile.getAbsolutePath();
						}
						else
						{
							filePath = Utils.getRealPathFromUri(fileUri, this);
						}

						File file = new File(filePath);
						if (file.length() > HikeConstants.MAX_FILE_SIZE)
						{
							showMaxFileToast = true;
							continue;
						}

						String fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(filePath));

						fileDetails.add(new Pair<String, String>(filePath, fileType));
					}

					if (showMaxFileToast)
					{
						Toast.makeText(ComposeChatActivity.this, R.string.max_file_size, Toast.LENGTH_SHORT).show();
					}

//					String msisdn = Utils.isGroupConversation(arrayList.getMsisdn()) ? arrayList.getId() : arrayList.getMsisdn();
//					boolean onHike = arrayList.isOnhike();
//
//					if (fileDetails.isEmpty())
//					{
//						return;
//					}
//
//					fileTransferTask = new InitiateMultiFileTransferTask(getApplicationContext(), fileDetails, msisdn, onHike);
//					Utils.executeAsyncTask(fileTransferTask);

					progressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.multi_file_creation));

					return;
				}
			}
		}
		else if (presentIntent.hasExtra(HikeConstants.Extras.FILE_KEY) )
		{
			intent.putExtras(presentIntent);
		}else if (presentIntent.hasExtra(StickerManager.FWD_CATEGORY_ID))
		{
			intent.putExtras(presentIntent);
		}else if ( presentIntent.hasExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT))
		{
			ArrayList<ConvMessage> multipleMessageList = new ArrayList<ConvMessage>();
			String jsonString = presentIntent.getStringExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
			try
			{
				JSONArray multipleMsgFwdArray = new JSONArray(jsonString);
				int msgCount = multipleMsgFwdArray.length();
				for (int i = 0; i < msgCount; i++)
				{
					JSONObject msgExtrasJson = (JSONObject) multipleMsgFwdArray.get(i);
					if (msgExtrasJson.has(HikeConstants.Extras.MSG))
					{
						String msg = msgExtrasJson.getString(HikeConstants.Extras.MSG);
						// as we will be changing msisdn and hike status while inserting in DB
						ConvMessage convMessage = Utils.makeConvMessage(null,null, msg, true);
						//sendMessage(convMessage);
						multipleMessageList.add(convMessage);
					}else if(msgExtrasJson.has(HikeConstants.Extras.POKE)){
						// as we will be changing msisdn and hike status while inserting in DB
						ConvMessage convMessage = Utils.makeConvMessage(null, null, getString(R.string.poke_msg), true);
						JSONObject metadata = new JSONObject();
						try
						{
							metadata.put(HikeConstants.POKE, true);
							convMessage.setMetadata(metadata);
						}
						catch (JSONException e)
						{
							Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
						}
						multipleMessageList.add(convMessage);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.FILE_PATH))
					{
						String fileKey = null;
						if (msgExtrasJson.has(HikeConstants.Extras.FILE_KEY))
						{
							fileKey = msgExtrasJson.getString(HikeConstants.Extras.FILE_KEY);
						}
						else
						{
						}
						String filePath = msgExtrasJson.getString(HikeConstants.Extras.FILE_PATH);
						String fileType = msgExtrasJson.getString(HikeConstants.Extras.FILE_TYPE);

						boolean isRecording = false;
						long recordingDuration = -1;
						if (msgExtrasJson.has(HikeConstants.Extras.RECORDING_TIME))
						{
							recordingDuration = msgExtrasJson.getLong(HikeConstants.Extras.RECORDING_TIME);
							isRecording = true;
							fileType = HikeConstants.VOICE_MESSAGE_CONTENT_TYPE;
						}

						HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

						if (Utils.isPicasaUri(filePath))
						{
							FileTransferManager.getInstance(getApplicationContext()).uploadFile(Uri.parse(filePath), hikeFileType, ((ContactInfo)arrayList.get(0)).getMsisdn(), ((ContactInfo)arrayList.get(0)).isOnhike());
						}
						else
						{
							initialiseFileTransfer(filePath, fileKey, hikeFileType, fileType, isRecording, recordingDuration, true, arrayList);
						}
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.LATITUDE) && msgExtrasJson.has(HikeConstants.Extras.LONGITUDE)
							&& msgExtrasJson.has(HikeConstants.Extras.ZOOM_LEVEL))
					{
						String fileKey = null;
						double latitude = msgExtrasJson.getDouble(HikeConstants.Extras.LATITUDE);
						double longitude = msgExtrasJson.getDouble(HikeConstants.Extras.LONGITUDE);
						int zoomLevel = msgExtrasJson.getInt(HikeConstants.Extras.ZOOM_LEVEL);
						initialiseLocationTransfer(latitude, longitude, zoomLevel,arrayList);
					}
					else if (msgExtrasJson.has(HikeConstants.Extras.CONTACT_METADATA))
					{
						try
						{
							JSONObject contactJson = new JSONObject(msgExtrasJson.getString(HikeConstants.Extras.CONTACT_METADATA));
							initialiseContactTransfer(contactJson,arrayList);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
					else if (msgExtrasJson.has(StickerManager.FWD_CATEGORY_ID))
					{
						String categoryId = msgExtrasJson.getString(StickerManager.FWD_CATEGORY_ID);
						String stickerId = msgExtrasJson.getString(StickerManager.FWD_STICKER_ID);
						int stickerIdx = msgExtrasJson.getInt(StickerManager.FWD_STICKER_INDEX);
						Sticker sticker = new Sticker(categoryId, stickerId, stickerIdx);
						multipleMessageList.add(sendSticker(sticker, categoryId, arrayList));
						boolean isDis = sticker.isDisabled(sticker, this.getApplicationContext());
						// add this sticker to recents if this sticker is not disabled
						if (!isDis)
							StickerManager.getInstance().addRecentSticker(sticker);
						/*
						 * Making sure the sticker is not forwarded again on orientation change
						 */
						presentIntent.removeExtra(StickerManager.FWD_CATEGORY_ID);
					}
					/*
					 * Since the message was not forwarded, we check if we have any drafts saved for this conversation, if we do we enter it in the compose box.
					 */
				}
				if(multipleMessageList.size()==0){
					return;
				}else{
					sendMultiMessages(multipleMessageList,arrayList);
				}
				
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), "Invalid JSON Array", e);
			}
			presentIntent.removeExtra(HikeConstants.Extras.MULTIPLE_MSG_OBJECT);
		}
		else if (type != null && presentIntent.hasExtra(Intent.EXTRA_STREAM))
		{
			Uri fileUri = presentIntent.getParcelableExtra(Intent.EXTRA_STREAM);
			if (type.startsWith(HikeConstants.SHARE_CONTACT_CONTENT_TYPE))
			{
				String lookupKey = fileUri.getLastPathSegment();

        		String[] projection = new String[] { Data.CONTACT_ID };
        		String selection = Data.LOOKUP_KEY + " =?";
        		String[] selectionArgs = new String[] { lookupKey };

        		Cursor c = getContentResolver().query(Data.CONTENT_URI, projection, selection, selectionArgs, null);

        		int contactIdIdx = c.getColumnIndex(Data.CONTACT_ID);
        		String contactId = null;
        		while(c.moveToNext())
        		{
        			contactId = c.getString(contactIdIdx);
        			if(!TextUtils.isEmpty(contactId))
        				break;
        		}
        		intent.putExtra(HikeConstants.Extras.CONTACT_ID, contactId);
        		intent.putExtra(HikeConstants.Extras.FILE_TYPE, type);
			}
			else
			{
				Logger.d(getClass().getSimpleName(), "File path uri: " + fileUri.toString());
				fileUri = Utils.makePicasaUri(fileUri);
				String fileUriStart = "file:";
				String fileUriString = fileUri.toString();
				String filePath;
				if (Utils.isPicasaUri(fileUriString))
				{
					filePath = fileUriString;
				}
				else if (fileUriString.startsWith(fileUriStart))
				{
					File selectedFile = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
					/*
					 * Done to fix the issue in a few Sony devices.
					 */
					filePath = selectedFile.getAbsolutePath();
				}
				else
				{
					filePath = Utils.getRealPathFromUri(fileUri, this);
				}
	
				if (TextUtils.isEmpty(filePath))
				{
					Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
					return;
				}
	
				File file = new File(filePath);
				if (file.length() > HikeConstants.MAX_FILE_SIZE)
				{
					Toast.makeText(ComposeChatActivity.this, R.string.max_file_size, Toast.LENGTH_SHORT).show();
					return;
				}
	
				type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.getFileExtension(filePath));
				if (type == null)
					type = presentIntent.getType();
	
				intent.putExtra(HikeConstants.Extras.FILE_PATH, filePath);
				intent.putExtra(HikeConstants.Extras.FILE_TYPE, type);
			}
		}
		else if (presentIntent.hasExtra(Intent.EXTRA_TEXT) || presentIntent.hasExtra(HikeConstants.Extras.MSG))
		{
			String msg = presentIntent.getStringExtra(presentIntent.hasExtra(HikeConstants.Extras.MSG) ? HikeConstants.Extras.MSG : Intent.EXTRA_TEXT);
			Logger.d(getClass().getSimpleName(), "Contained a message: " + msg);
			//intent.putExtra(HikeConstants.Extras.MSG, msg);
			ConvMessage convMessage;
			try
			{
				convMessage = new ConvMessage(new JSONObject(msg));
				sendMessage(convMessage);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	private ArrayList<ContactInfo> updateContactInfoOrdering(ArrayList<ContactInfo> arrayList){
		Set<ContactInfo> set = new HashSet<ContactInfo>(arrayList);
		ArrayList<ContactInfo> toReturn = new ArrayList<ContactInfo>();
		List<ContactInfo> conversations = getRecentContacts();
		int total = conversations.size();
		// we want to maintain ordering, conversations on home screen must appear in same order they were before multi forward
		// we are adding from last to first , so that when db entry is made timestamp for last is less than first
		for(int i=0;i<total;i++){
			ContactInfo contactInfo = conversations.get(i);
			if(set.contains(contactInfo)){
				toReturn.add(contactInfo);
				set.remove(contactInfo);
			}
		}
		toReturn.addAll(set);
		return toReturn;
	}
	
	
	private void sendMultiMessages(ArrayList<ConvMessage> multipleMessageList, ArrayList<ContactInfo> arrayList)
	{
		MultipleConvMessage multiMessages = new MultipleConvMessage(multipleMessageList, arrayList, System.currentTimeMillis() / 1000);
		mPubSub.publish(HikePubSub.MULTI_MESSAGE_SENT, multiMessages);
		Toast.makeText(getApplicationContext(), getString(R.string.messages_sent_succees), Toast.LENGTH_SHORT).show();
	}

	private void sendMessage(ConvMessage convMessage)
	{
		
		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
		
	}
	

	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);

		if (HikePubSub.MULTI_FILE_TASK_FINISHED.equals(type))
		{
			final String msisdn = fileTransferTask.getMsisdn();

			fileTransferTask = null;

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					Intent intent = new Intent(ComposeChatActivity.this, ChatThread.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra(HikeConstants.Extras.MSISDN, msisdn);
					startActivity(intent);
					finish();

					if (progressDialog != null)
					{
						progressDialog.dismiss();
						progressDialog = null;
					}
				}
			});
		}
		else if (HikePubSub.APP_FOREGROUNDED.equals(type))
		{

			if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.LAST_SEEN_PREF, true))
			{
				return;
			}

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (lastSeenScheduler == null)
					{
						lastSeenScheduler = LastSeenScheduler.getInstance(ComposeChatActivity.this);
					}
					else
					{
						lastSeenScheduler.stop(true);
					}
					lastSeenScheduler.start(true);
				}
			});
		}
		else if (HikePubSub.LAST_SEEN_TIME_BULK_UPDATED.equals(type))
		{
			List<ContactInfo> friendsList = adapter.getFriendsList();

			Utils.updateLastSeenTimeInBulk(friendsList);

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					adapter.makeCompleteList(false);
				}
			});

		}
		else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type))
		{
			final ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo.getFavoriteType() != FavoriteType.FRIEND)
			{
				return;
			}

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					adapter.addToGroup(contactInfo, FriendsAdapter.FRIEND_INDEX);
				}

			});
		}
		else if(HikePubSub.CONTACT_SYNC_STARTED.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// For showing auto/manual sync progress bar when already on the activity
					showProgressBarContactsSync(View.VISIBLE);
				}

			});
		}
		else if (HikePubSub.CONTACT_SYNCED.equals(type))
		{
			Boolean[] ret = (Boolean[]) object;
			final boolean contactsChanged = ret[1];
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					// Dont repopulate list if no sync changes
					if(contactsChanged)
						adapter.executeFetchTask();
					showProgressBarContactsSync(View.GONE);
				}

			});
		}
	}

	private void showProgressBarContactsSync(int value)
	{
		ProgressBar progress_bar = null;
		if(groupChatActionBar!=null)
		{
			progress_bar = (ProgressBar)groupChatActionBar.findViewById(R.id.loading_progress);
			progress_bar.setVisibility(value);
		}
		if(multiSelectActionBar!=null)
		{
			progress_bar = (ProgressBar)multiSelectActionBar.findViewById(R.id.loading_progress);
			progress_bar.setVisibility(value);
		}
	}

	@Override
	public void onBackPressed()
	{
		if (composeMode == CREATE_GROUP_MODE)
		{
			if (existingGroupId != null || createGroup)
			{
				ComposeChatActivity.this.finish();
				return;
			}
			setMode(START_CHAT_MODE);
			return;
		}
		else if(composeMode == MULTIPLE_FWD)
		{
			ComposeChatActivity.this.finish();
			return;
		}
		super.onBackPressed();
	}

	private void showToast(String message)
	{
		Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
	}

	FriendsListFetchedCallback friendsListFetchedCallback = new FriendsListFetchedCallback()
	{

		@Override
		public void listFetched()
		{
			if (PreferenceManager.getDefaultSharedPreferences(ComposeChatActivity.this).getBoolean(HikeConstants.LAST_SEEN_PREF, true))
			{
				lastSeenScheduler = LastSeenScheduler.getInstance(ComposeChatActivity.this);
				lastSeenScheduler.start(true);
			}
		}
	};

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (previousFirstVisibleItem != firstVisibleItem)
		{
			long currTime = System.currentTimeMillis();
			long timeToScrollOneElement = currTime - previousEventTime;
			velocity = (int) (((double) 1 / timeToScrollOneElement) * 1000);

			previousFirstVisibleItem = firstVisibleItem;
			previousEventTime = currTime;
		}

		if (adapter == null)
		{
			return;
		}

		adapter.setIsListFlinging(velocity > HikeConstants.MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
	}
	public ConvMessage sendSticker(Sticker sticker, String categoryIdIfUnknown, ArrayList<ContactInfo> arrayList)
	{
		ConvMessage convMessage = Utils.makeConvMessage(new Conversation(((ContactInfo) arrayList.get(0)).getMsisdn()),((ContactInfo) arrayList.get(0)).getMsisdn(), "Sticker", ((ContactInfo) arrayList.get(0)).isOnhike());
	
		JSONObject metadata = new JSONObject();
		try
		{
			String categoryName;
			if (sticker.getCategory().categoryId == StickerCategoryId.unknown)
			{
				categoryName = categoryIdIfUnknown;
			}
			else
			{
				categoryName = sticker.getCategory().categoryId.name();
			}
			metadata.put(StickerManager.CATEGORY_ID, categoryName);

			metadata.put(StickerManager.STICKER_ID, sticker.getStickerId());

			convMessage.setMetadata(metadata);
			Logger.d(getClass().getSimpleName(), "metadata: " + metadata.toString());
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		return convMessage;
		//sendMessage(convMessage);
	}
	private void initialiseFileTransfer(String filePath, String fileKey, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration,
			boolean isForwardingFile, ArrayList<ContactInfo> arrayList)
	{
		clearTempData();
		if (filePath == null)
		{
			Toast.makeText(getApplicationContext(), R.string.unknown_msg, Toast.LENGTH_SHORT).show();
			return;
		}
		File file = new File(filePath);
		Logger.d(getClass().getSimpleName(), "File size: " + file.length() + " File name: " + file.getName());

		if (HikeConstants.MAX_FILE_SIZE != -1 && HikeConstants.MAX_FILE_SIZE < file.length())
		{
			Toast.makeText(getApplicationContext(), R.string.max_file_size, Toast.LENGTH_SHORT).show();
			return;
		}
		FileTransferManager.getInstance(getApplicationContext()).uploadFile(arrayList, file, fileKey, fileType, hikeFileType, isRecording, isForwardingFile,
				((ContactInfo)arrayList.get(0)).isOnhike(), recordingDuration);
	}
	private void clearTempData()
	{
		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
		editor.remove(HikeMessengerApp.TEMP_NAME);
		editor.remove(HikeMessengerApp.TEMP_NUM);
		editor.commit();
	}
	private void initialiseLocationTransfer(double latitude, double longitude, int zoomLevel, ArrayList<ContactInfo> arrayList)
	{
		clearTempData();
		FileTransferManager.getInstance(getApplicationContext()).uploadLocation(((ContactInfo)arrayList.get(0)).getMsisdn(), latitude, longitude, zoomLevel, ((ContactInfo)arrayList.get(0)).isOnhike());
	}
	private void initialiseContactTransfer(JSONObject contactJson, ArrayList<ContactInfo> arrayList)
	{
		FileTransferManager.getInstance(getApplicationContext()).uploadContact(((ContactInfo)arrayList.get(0)).getMsisdn(), contactJson, (((ContactInfo)arrayList.get(0)).isOnhike()));
	}

	
	List<ContactInfo> getRecentContacts()
	{
		if(recentContacts == null)
		{
			recentContacts = HikeMessengerApp.getContactManager().getAllConversationContactsSorted(false);
		}
		return recentContacts;
	}
}