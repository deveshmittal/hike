package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeSearchContactAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class ComposeActivity extends HikeAppStateBaseFragmentActivity implements
		OnItemClickListener {

	private EditText mInputNumberView;
	private ListView mContactList;
	private Set<ContactInfo> selectedContactSet;
	private ViewGroup doneContainer;
	private TextView doneText;
	private TextView title;
	private ImageView backIcon;

	private boolean isGroupChat;
	private boolean isForwardingMessage;
	private boolean isSharingFile;
	private String existingGroupId;

	private Map<String, GroupParticipant> groupParticipants;
	private boolean createGroup;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.compose);

		selectedContactSet = new HashSet<ContactInfo>();

		createGroup = getIntent().getBooleanExtra(
				HikeConstants.Extras.CREATE_GROUP, false);
		isGroupChat = getIntent().getBooleanExtra(
				HikeConstants.Extras.GROUP_CHAT, false);
		isForwardingMessage = getIntent().getBooleanExtra(
				HikeConstants.Extras.FORWARD_MESSAGE, false);
		isSharingFile = getIntent().getType() != null;
		// Getting the group id. This will be a valid value if the intent
		// was passed to add group participants.
		existingGroupId = getIntent().getStringExtra(
				HikeConstants.Extras.EXISTING_GROUP_CHAT);

		if (Intent.ACTION_SEND.equals(getIntent().getAction())
				|| Intent.ACTION_SENDTO.equals(getIntent().getAction())) {
			isForwardingMessage = true;
		}

		setupActionBar();

		mInputNumberView = (EditText) findViewById(R.id.search_text);
		mContactList = (ListView) findViewById(R.id.compose_list);

		init();
		Utils.executeContactListResultTask(new CreateAutoCompleteViewTask());
	}

	private void init() {
		doneContainer.setVisibility(View.GONE);
		backIcon.setImageResource(R.drawable.ic_back);
		getSupportActionBar().setBackgroundDrawable(
				getResources().getDrawable(R.drawable.bg_header));
		setLabel();
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(
				R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		backIcon = (ImageView) actionBarView.findViewById(R.id.abs__up);

		doneContainer = (ViewGroup) actionBarView
				.findViewById(R.id.done_container);
		doneText = (TextView) actionBarView.findViewById(R.id.done_text);

		title = (TextView) actionBarView.findViewById(R.id.title);

		backContainer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (selectedContactSet.isEmpty()) {
					Intent intent = new Intent(ComposeActivity.this,
							HomeActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				} else {
					selectedContactSet.clear();
					init();
					Utils.executeContactListResultTask(new CreateAutoCompleteViewTask());
				}
			}
		});

		doneContainer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onDoneButtonClick();
			}
		});

		actionBar.setCustomView(actionBarView);

		init();
	}

	private void setLabel() {
		if (createGroup) {
			title.setText(R.string.new_group);
		} else if (isSharingFile) {
			title.setText(R.string.share_file);
		} else if (isForwardingMessage) {
			title.setText(R.string.forward);
		} else if (!TextUtils.isEmpty(existingGroupId)) {
			title.setText(R.string.add_group);
		} else {
			title.setText(R.string.new_chat);
		}
	}

	private void onDoneButtonClick() {
		Iterator<ContactInfo> iterator = selectedContactSet.iterator();
		final ArrayList<ContactInfo> selectedContactList = new ArrayList<ContactInfo>(
				selectedContactSet.size());
		while (iterator.hasNext()) {
			selectedContactList.add(iterator.next());
		}

		if (createGroup && selectedContactList.size() <= 1) {
			Toast.makeText(getApplicationContext(),
					R.string.invalid_group_chat, Toast.LENGTH_SHORT).show();
		}

		ContactInfo conversationContactInfo = null;
		if (selectedContactList.size() == 1
				&& TextUtils.isEmpty(existingGroupId)) {
			conversationContactInfo = selectedContactList.get(0);
			Intent intent = Utils
					.createIntentFromContactInfo(conversationContactInfo, true);
			intent.setClass(this, ChatThread.class);
			startActivity(intent);
			finish();
		} else {
			if (TextUtils.isEmpty(existingGroupId)) {
				final Dialog dialog = new Dialog(this,
						R.style.Theme_CustomDialog_Keyboard);
				dialog.setContentView(R.layout.group_name_change_dialog);

				TextView header = (TextView) dialog.findViewById(R.id.header);
				header.setText(R.string.group_name);

				final EditText editText = (EditText) dialog
						.findViewById(R.id.group_name_edit);

				Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
				Button cancelBtn = (Button) dialog
						.findViewById(R.id.btn_cancel);

				cancelBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});

				okBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						String groupName = editText.getText().toString();
						if (TextUtils.isEmpty(groupName.trim())) {
							Toast toast = Toast.makeText(ComposeActivity.this,
									R.string.enter_valid_group_name,
									Toast.LENGTH_SHORT);
							toast.setGravity(Gravity.CENTER, 0, 0);
							toast.show();
							return;
						}
						dialog.dismiss();
						createGroup(selectedContactList, groupName);
					}
				});

				dialog.show();

			} else {
				createGroup(selectedContactList, null);
			}
		}
	}

	private void createGroup(ArrayList<ContactInfo> selectedContactList,
			String groupName) {
		String groupId = getIntent().getStringExtra(
				HikeConstants.Extras.EXISTING_GROUP_CHAT);
		boolean newGroup = false;

		if (TextUtils.isEmpty(groupId)) {
			// Create new group
			String uid = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(
					HikeMessengerApp.UID_SETTING, "");
			groupId = uid + ":" + System.currentTimeMillis();
			newGroup = true;
		} else {
			// Group alredy exists. Fetch existing participants.
			newGroup = false;
		}
		Map<String, GroupParticipant> participantList = new HashMap<String, GroupParticipant>();

		for (ContactInfo particpant : selectedContactList) {
			GroupParticipant groupParticipant = new GroupParticipant(particpant);
			participantList.put(particpant.getMsisdn(), groupParticipant);
		}
		ContactInfo userContactInfo = Utils
				.getUserContactInfo(getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE));

		GroupConversation groupConversation = new GroupConversation(groupId, 0,
				null, userContactInfo.getMsisdn(), true);
		groupConversation.setGroupParticipantList(participantList);

		Log.d(getClass().getSimpleName(), "Creating group: " + groupId);
		HikeConversationsDatabase mConversationDb = HikeConversationsDatabase
				.getInstance();
		mConversationDb.addGroupParticipants(groupId,
				groupConversation.getGroupParticipantList());
		if (newGroup) {
			mConversationDb.addConversation(groupConversation.getMsisdn(),
					false, groupName, groupConversation.getGroupOwner());
		}

		try {
			// Adding this boolean value to show a different system message
			// if its a new group
			JSONObject gcjPacket = groupConversation
					.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
			gcjPacket.put(HikeConstants.NEW_GROUP, newGroup);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT,
					new ConvMessage(gcjPacket, groupConversation, this, true));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		JSONObject gcjJson = groupConversation
				.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
		/*
		 * Adding the group name to the packet
		 */
		if (newGroup) {
			JSONObject metadata = new JSONObject();
			try {
				metadata.put(HikeConstants.NAME, groupName);
				gcjJson.put(HikeConstants.METADATA, metadata);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, gcjJson);

		ContactInfo conversationContactInfo = new ContactInfo(groupId, groupId,
				groupId, groupId);
		Intent intent = Utils
				.createIntentFromContactInfo(conversationContactInfo, true);
		intent.setClass(this, ChatThread.class);
		startActivity(intent);
		finish();
	}

	private class CreateAutoCompleteViewTask extends
			AsyncTask<Void, Void, List<Pair<AtomicBoolean, ContactInfo>>> {

		private boolean freeSMSOn;
		private boolean nativeSMSOn;
		private String userMsisdn;

		@Override
		protected void onPreExecute() {
			SharedPreferences appPref = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());

			freeSMSOn = appPref.getBoolean(HikeConstants.FREE_SMS_PREF, true);

			nativeSMSOn = appPref
					.getBoolean(HikeConstants.SEND_SMS_PREF, false);

			userMsisdn = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(
					HikeMessengerApp.MSISDN_SETTING, "");
		}

		@Override
		protected List<Pair<AtomicBoolean, ContactInfo>> doInBackground(
				Void... params) {
			List<Pair<AtomicBoolean, ContactInfo>> contactList = getContactsForComposeScreen(
					userMsisdn, freeSMSOn, isGroupChat, isForwardingMessage,
					isSharingFile, nativeSMSOn);

			if (!TextUtils.isEmpty(existingGroupId)) {
				groupParticipants = HikeConversationsDatabase.getInstance()
						.getGroupParticipants(existingGroupId, true, false);
				List<Integer> indicesToRemove = new ArrayList<Integer>();

				for (int i = contactList.size() - 1; i >= 0; i--) {
					Pair<AtomicBoolean, ContactInfo> pair = contactList.get(i);
					ContactInfo contactInfo = pair.second;
					if (groupParticipants.containsKey(contactInfo.getMsisdn())) {
						indicesToRemove.add(i);
					}
				}

				for (Integer i : indicesToRemove) {
					contactList.remove(i.intValue());
				}
			}
			return contactList;
		}

		@Override
		protected void onPostExecute(
				List<Pair<AtomicBoolean, ContactInfo>> contactList) {
			findViewById(R.id.empty).setVisibility(View.GONE);

			mInputNumberView.setText("");

			HikeSearchContactAdapter adapter = new HikeSearchContactAdapter(
					ComposeActivity.this, contactList, mInputNumberView,
					existingGroupId, isForwardingMessage, groupParticipants);

			mContactList.setAdapter(adapter);
			mContactList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			mContactList.setOnItemClickListener(ComposeActivity.this);
			mInputNumberView.addTextChangedListener(adapter);

			getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}

	}

	private List<Pair<AtomicBoolean, ContactInfo>> getContactsForComposeScreen(
			String userMsisdn, boolean freeSMSOn, boolean isGroupChat,
			boolean isForwardingMessage, boolean isSharingFile,
			boolean nativeSMSOn) {
		List<Pair<AtomicBoolean, ContactInfo>> contactList = HikeUserDatabase
				.getInstance().getContactsForComposeScreen(freeSMSOn,
						(isGroupChat || isForwardingMessage || isSharingFile),
						userMsisdn, nativeSMSOn);

		if (isForwardingMessage || isSharingFile) {
			contactList.addAll(0, HikeConversationsDatabase.getInstance()
					.getGroupNameAndParticipantsAsContacts(this));
		}
		return contactList;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,

	int position, long id) {
		Object tag = view.getTag();
		Pair<AtomicBoolean, ContactInfo> pair = (Pair<AtomicBoolean, ContactInfo>) tag;

		if (!isForwardingMessage) {
			boolean isChecked = pair.first.get();
			boolean unknownContact = false;

			int totalSelectedContacts = selectedContactSet.size()
					+ (groupParticipants != null ? groupParticipants.size() : 0);
			if (totalSelectedContacts >= HikeConstants.MAX_CONTACTS_IN_GROUP
					&& !isChecked) {
				Toast.makeText(getApplicationContext(), R.string.max_contact,
						Toast.LENGTH_SHORT).show();
				return;
			}

			ContactInfo contactInfo = pair.second;
			if (contactInfo == null) {
				String msisdn = getNormalisedMsisdn();

				contactInfo = new ContactInfo(msisdn, msisdn, msisdn, msisdn);

				unknownContact = true;
			}

			boolean toggle = true;
			if (!isChecked) {
				if (groupParticipants != null
						&& groupParticipants.containsKey(contactInfo
								.getMsisdn())) {
					toggle = false;
					Toast.makeText(getApplicationContext(),
							R.string.contact_selected_already,
							Toast.LENGTH_SHORT).show();
					return;
				}
			}

			HikeSearchContactAdapter adapter = ((HikeSearchContactAdapter) adapterView
					.getAdapter());

			if (toggle) {
				pair.first.set(!pair.first.get());
			}

			if (unknownContact) {
				Pair<AtomicBoolean, ContactInfo> unknownPair = new Pair<AtomicBoolean, ContactInfo>(
						new AtomicBoolean(true), contactInfo);
				adapter.addItemToCompleteList(unknownPair);
				adapter.add(unknownPair);
			}

			mInputNumberView.setText("");

			view.setTag(pair);
				
			adapter.sort();
			adapter.notifyDataSetChanged();

			mContactList.setSelection(0);

			if (selectedContactSet.contains(contactInfo)) {
				selectedContactSet.remove(contactInfo);
			} else {
				selectedContactSet.add(contactInfo);
			}

			if ((createGroup && selectedContactSet.size() > 1)
					|| (!createGroup && !selectedContactSet.isEmpty())) {
				doneContainer.setVisibility(View.VISIBLE);
				doneText.setText(Integer.toString(selectedContactSet.size()));
				getSupportActionBar().setBackgroundDrawable(
						getResources()
								.getDrawable(R.drawable.bg_header_compose));

				if (!TextUtils.isEmpty(existingGroupId)) {
					title.setText(R.string.add_group);
				} else {
					title.setText((selectedContactSet.size() > 1 || createGroup) ? R.string.new_group
							: R.string.new_chat);
				}

				backIcon.setImageResource(R.drawable.ic_cancel);
			} else {
				init();
			}
		} else {
			Intent presentIntent = getIntent();

			ContactInfo contactInfo;
			if (pair.second != null) {
				contactInfo = pair.second;
			} else {
				String msisdn = getNormalisedMsisdn();
				contactInfo = new ContactInfo(msisdn, msisdn, msisdn, msisdn);
			}
			Intent intent = Utils.createIntentFromContactInfo(contactInfo, true);
			intent.setClass(this, ChatThread.class);
			String type = presentIntent.getType();

			if ("text/plain".equals(type)
					|| presentIntent.hasExtra(HikeConstants.Extras.MSG)) {
				String msg = presentIntent
						.getStringExtra(presentIntent
								.hasExtra(HikeConstants.Extras.MSG) ? HikeConstants.Extras.MSG
								: Intent.EXTRA_TEXT);
				Log.d(getClass().getSimpleName(), "Contained a message: " + msg);
				intent.putExtra(HikeConstants.Extras.MSG, msg);
			} else if (presentIntent.hasExtra(HikeConstants.Extras.FILE_KEY)
					|| presentIntent
							.hasExtra(StickerManager.FWD_CATEGORY_ID)) {
				intent.putExtras(presentIntent);
			} else if (type != null
					&& (type.startsWith("image") || type.startsWith("audio") || type
							.startsWith("video"))) {
				Uri fileUri = presentIntent
						.getParcelableExtra(Intent.EXTRA_STREAM);
				Log.d(getClass().getSimpleName(),
						"File path uri: " + fileUri.toString());
				String fileUriStart = "file:";
				String fileUriString = fileUri.toString();
				String filePath;
				if (fileUriString.startsWith(fileUriStart)) {
					File selectedFile = new File(URI.create(fileUriString));
					/*
					 * Done to fix the issue in a few Sony devices.
					 */
					filePath = selectedFile.getAbsolutePath();
				} else {
					filePath = Utils.getRealPathFromUri(fileUri, this);
				}
				intent.putExtra(HikeConstants.Extras.FILE_PATH, filePath);
				intent.putExtra(HikeConstants.Extras.FILE_TYPE, type);
			}
			startActivity(intent);
			finish();
		}
	}

	private String getNormalisedMsisdn() {
		String textEntered = mInputNumberView.getText().toString();
		return Utils.normalizeNumber(
				textEntered,
				getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0)
						.getString(HikeMessengerApp.COUNTRY_CODE,
								HikeConstants.INDIA_COUNTRY_CODE));
	}
}
