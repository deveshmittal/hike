package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
import com.bsb.hike.adapters.HikeInviteAdapter;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeListActivity extends HikeAppStateBaseFragmentActivity implements OnItemClickListener
{

	private enum Type
	{
		INVITE, BLOCK
	}

	private HikeInviteAdapter adapter;

	private ListView listView;

	private EditText input;

	private Set<String> selectedContacts;

	private Type type;

	private Map<String, Boolean> toggleBlockMap;

	private View doneBtn;

	private ImageView arrow;

	private TextView postText;

	private TextView title;

	private ImageView backIcon;

	List<Pair<AtomicBoolean, ContactInfo>> firstSectionList;

	private boolean calledFromFTUE = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikelistactivity);

		if (getIntent().getBooleanExtra(HikeConstants.Extras.BLOCKED_LIST, false))
		{
			type = Type.BLOCK;
		}
		else
		{
			type = Type.INVITE;
		}

		if (getIntent().getBooleanExtra(HikeConstants.Extras.CALLED_FROM_FTUE_POPUP, false))
		{
			calledFromFTUE = true;
		}

		selectedContacts = new HashSet<String>();

		listView = (ListView) findViewById(R.id.contact_list);
		input = (EditText) findViewById(R.id.input_number);

		listView.setTextFilterEnabled(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);

		findViewById(android.R.id.empty).setVisibility(View.GONE);

		switch (type)
		{
		case BLOCK:
			toggleBlockMap = new HashMap<String, Boolean>();
			break;
		case INVITE:
			break;
		}
		setupActionBar();
		Utils.executeContactListResultTask(new SetupContactList());
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	private void init()
	{
		if (type != Type.BLOCK)
		{
			postText.setText(getString(R.string.send_invite, selectedContacts.size()));
		}
		backIcon.setImageResource(R.drawable.ic_back);
		setLabel();
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		backIcon = (ImageView) actionBarView.findViewById(R.id.abs__up);
		title = (TextView) actionBarView.findViewById(R.id.title);

		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.post_btn);
		doneBtn = actionBarView.findViewById(R.id.done_container);

		doneBtn.setVisibility(View.VISIBLE);

		Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, false);

		if (type != Type.BLOCK)
		{
			doneBtn.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					final CheckBox selectAllCB = (CheckBox) findViewById(R.id.select_all_cb);
					if(selectAllCB.isChecked())
					{
						showInviteConfirmationPopup(true);
					}
					else if(calledFromFTUE)
					{
						showInviteConfirmationPopup(false);
					}
					else
					{
						showNativeSMSPopup();
					}
				}
			});
		}
		else
		{
			postText.setText(R.string.save);
			doneBtn.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					onTitleIconClick(null);
				}
			});
		}

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent intent = null;
				if (type != Type.BLOCK)
				{
					if (getIntent().getBooleanExtra(HikeConstants.Extras.FROM_CREDITS_SCREEN, false))
					{
						intent = new Intent(HikeListActivity.this, CreditsActivity.class);
					}
					else
					{
						intent = new Intent(HikeListActivity.this, TellAFriend.class);
					}
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
				else
				{
					onBackPressed();
				}
			}
		});

		actionBar.setCustomView(actionBarView);

		init();
	}

	private void showInviteConfirmationPopup(boolean selectAllChecked)
	{
		final CustomAlertDialog confirmDialog = new CustomAlertDialog(this);
		
		View.OnClickListener dialogOkClickListener = new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				confirmDialog.dismiss();
				showNativeSMSPopup();
			}
		};

		if(!selectAllChecked)
		{
			confirmDialog.setHeader(R.string.invite_friends);
			confirmDialog.setBody(getResources().getString(R.string.invite_friends_confirmation_msg, selectedContacts.size()));
		}
		else
		{
			confirmDialog.setHeader(R.string.select_all_confirmation_header);
			confirmDialog.setBody(getResources().getString(R.string.select_all_confirmation_msg, selectedContacts.size()));
		}
		confirmDialog.setOkButton(R.string.yes, dialogOkClickListener);
		confirmDialog.setCancelButton(R.string.no);
		confirmDialog.show();
	}

	private void setLabel()
	{
		if (type != Type.BLOCK)
		{
			SharedPreferences preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
			boolean sendNativeInvite = !HikeMessengerApp.isIndianUser() || preferences.getBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, false);
			title.setText(sendNativeInvite ? R.string.invite_sms : R.string.invite_free_sms);
		}
		else
		{
			title.setText(R.string.blocked_list);
		}
	}

	private void showNativeSMSPopup()
	{
		final SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		boolean sendNativeInvite = !HikeMessengerApp.isIndianUser() || settings.getBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, false);

		if (sendNativeInvite && !settings.getBoolean(HikeConstants.OPERATOR_SMS_ALERT_CHECKED, false))
		{
			final Dialog dialog = new Dialog(this, R.style.Theme_CustomDialog);
			dialog.setContentView(R.layout.operator_alert_popup);
			dialog.setCancelable(true);

			TextView header = (TextView) dialog.findViewById(R.id.header);
			TextView body = (TextView) dialog.findViewById(R.id.body_text);
			Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
			Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

			btnCancel.setVisibility(View.GONE);
			header.setText(R.string.native_header);
			body.setText(R.string.native_info);

			CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.body_checkbox);
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
			{

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					Editor editor = settings.edit();
					editor.putBoolean(HikeConstants.OPERATOR_SMS_ALERT_CHECKED, isChecked);
					editor.commit();
				}
			});
			checkBox.setText(getResources().getString(R.string.not_show_call_alert_msg));

			btnOk.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					onTitleIconClick(null);
				}
			});

			dialog.show();
		}
		else
		{
			onTitleIconClick(null);
		}
	}

	private class SetupContactList extends AsyncTask<Void, Void, List<Pair<AtomicBoolean, ContactInfo>>>
	{

		boolean loadOnUiThread;

		@Override
		protected void onPreExecute()
		{
			loadOnUiThread = Utils.loadOnUiThread();
			findViewById(R.id.progress_container).setVisibility(loadOnUiThread ? View.GONE : View.VISIBLE);
		}

		@Override
		protected List<Pair<AtomicBoolean, ContactInfo>> doInBackground(Void... params)
		{
			if (loadOnUiThread)
			{
				return null;
			}
			else
			{
				return getContactList();
			}
		}

		@Override
		protected void onPostExecute(List<Pair<AtomicBoolean, ContactInfo>> contactList)
		{
			if (contactList == null)
			{
				contactList = getContactList();
			}

			findViewById(R.id.progress_container).setVisibility(View.GONE);

			ViewGroup selectAllContainer = (ViewGroup) findViewById(R.id.select_all_container);

			firstSectionList = new ArrayList<Pair<AtomicBoolean, ContactInfo>>();

			switch (type)
			{
			case BLOCK:
				getBlockedContactsList(contactList, firstSectionList);
				selectAllContainer.setVisibility(View.GONE);
				break;
			case INVITE:
				selectAllContainer.setVisibility(View.VISIBLE);

				final TextView selectAllText = (TextView) findViewById(R.id.select_all_text);
				final CheckBox selectAllCB = (CheckBox) findViewById(R.id.select_all_cb);

				final int size = contactList.size();

				selectAllText.setText(getString(R.string.select_all, size));
				selectAllCB.setOnCheckedChangeListener(new OnCheckedChangeListener()
				{

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
					{
						selectAllToggled(isChecked);
						selectAllText.setText(getString(isChecked ? R.string.deselect_all : R.string.select_all, size));
					}
				});

				selectAllContainer.setOnClickListener(new OnClickListener()
				{

					@Override
					public void onClick(View v)
					{
						selectAllCB.setChecked(!selectAllCB.isChecked());
					}
				});

				getRecommendedInvitesList(contactList, firstSectionList);
				break;
			}

			HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>> completeSectionsData = new HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>>();
			contactList.removeAll(firstSectionList);
			if (!firstSectionList.isEmpty())
			{
				completeSectionsData.put(0, firstSectionList);
			}
			completeSectionsData.put(completeSectionsData.size(), contactList);
			adapter = new HikeInviteAdapter(HikeListActivity.this, -1, completeSectionsData, type == Type.BLOCK);
			input.addTextChangedListener(adapter);

			listView.setAdapter(adapter);
			listView.setEmptyView(findViewById(android.R.id.empty));
			setupActionBarElements();
		}
	}

	public void selectAllToggled(boolean isChecked)
	{
		HashMap<Integer, List<Pair<AtomicBoolean, ContactInfo>>> contactListMap = adapter.getCompleteList();

		for (Entry<Integer, List<Pair<AtomicBoolean, ContactInfo>>> entry : contactListMap.entrySet())
		{
			for (Pair<AtomicBoolean, ContactInfo> pair : entry.getValue())
			{
				pair.first.set(isChecked);
				String msisdn = pair.second.getMsisdn();
				if (isChecked)
				{
					selectedContacts.add(msisdn);
				}
				else
				{
					selectedContacts.remove(msisdn);
				}
			}
		}
		adapter.notifyDataSetChanged();
		setupActionBarElements();
	}

	private List<Pair<AtomicBoolean, ContactInfo>> getContactList()
	{
		switch (type)
		{
		case BLOCK:
			return ContactManager.getInstance().getBlockedUserList();
		case INVITE:
			return HikeMessengerApp.getContactManager().getNonHikeContacts();
		}
		return null;
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
	protected void onDestroy()
	{
		super.onDestroy();
	}

	public void onTitleIconClick(View v)
	{
		if (type != Type.BLOCK)
		{

			if (selectedContacts.isEmpty())
			{
				Toast.makeText(getApplicationContext(), R.string.select_invite_contacts, Toast.LENGTH_SHORT).show();
				return;
			}

			Iterator<String> iterator = selectedContacts.iterator();

			boolean sendNativeInvite = !HikeMessengerApp.isIndianUser()
					|| getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, false);

			long time = System.currentTimeMillis();

			try
			{
				JSONObject mqttPacket = new JSONObject();
				JSONObject data = new JSONObject();

				mqttPacket.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MULTI_INVITE);
				if (sendNativeInvite)
				{
					mqttPacket.put(HikeConstants.SUB_TYPE, HikeConstants.NO_SMS);
				}
				mqttPacket.put(HikeConstants.TIMESTAMP, time / 1000);

				JSONArray inviteArray = new JSONArray();

				while (iterator.hasNext())
				{
					String msisdn = iterator.next();
					Logger.d(getClass().getSimpleName(), "Inviting " + msisdn);
					Utils.sendInvite(msisdn, this, false, true);

					inviteArray.put(msisdn);
				}
				data.put(HikeConstants.MESSAGE_ID, time);
				data.put(HikeConstants.LIST, inviteArray);
				data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()));
				if (calledFromFTUE)
				{
					JSONObject ftueData = new JSONObject();
					ftueData.put(HikeConstants.SCREEN, HikeConstants.FTUE);
					data.put(HikeConstants.METADATA, ftueData);
				}

				mqttPacket.put(HikeConstants.DATA, data);

				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, mqttPacket);

				CheckBox selectAllCB = (CheckBox) findViewById(R.id.select_all_cb);
				if (selectAllCB.isChecked())
				{
					Utils.sendUILogEvent(HikeConstants.LogEvent.SELECT_ALL_INVITE);
				}

				Toast.makeText(getApplicationContext(), selectedContacts.size() > 1 ? R.string.invites_sent : R.string.invite_sent, Toast.LENGTH_SHORT).show();
				finish();
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			for (Entry<String, Boolean> toggleBlockEntry : toggleBlockMap.entrySet())
			{
				String msisdn = toggleBlockEntry.getKey();
				boolean blocked = toggleBlockEntry.getValue();

				HikeMessengerApp.getPubSub().publish(blocked ? HikePubSub.BLOCK_USER : HikePubSub.UNBLOCK_USER, msisdn);
			}
			finish();
		}
	}

	private void setupActionBarElements()
	{
		if (!selectedContacts.isEmpty())
		{
			Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, true);
			if (type != Type.BLOCK)
			{
				postText.setText(getString(R.string.send_invite, selectedContacts.size()));
			}
		}
		else
		{
			Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, false);
			init();
		}
	}

	private void getBlockedContactsList(List<Pair<AtomicBoolean, ContactInfo>> contactList, List<Pair<AtomicBoolean, ContactInfo>> firstSectionList)
	{
		/*
		 * This would be true when we have pre checked items.
		 */
		for (Pair<AtomicBoolean, ContactInfo> contactItem : contactList)
		{
			boolean checked = contactItem.first.get();
			if (checked)
			{
				firstSectionList.add(contactItem);
				selectedContacts.add(contactItem.second.getMsisdn());
			}
			else
			{
				break;
			}
		}
	}

	private void getRecommendedInvitesList(List<Pair<AtomicBoolean, ContactInfo>> contactList, List<Pair<AtomicBoolean, ContactInfo>> firstSectionList)
	{
		int limit = 6;
		List<ContactInfo> recommendedContactList = HikeMessengerApp.getContactManager().getNonHikeMostContactedContacts(20);
		if (recommendedContactList.size() >= limit)
		{
			recommendedContactList = recommendedContactList.subList(0, limit);
		}
		for (Pair<AtomicBoolean, ContactInfo> pair : contactList)
		{
			ContactInfo contactInfo = pair.second;
			if (recommendedContactList.contains(contactInfo))
			{
				if (calledFromFTUE)
				{
					pair.first.set(true);
					selectedContacts.add(contactInfo.getMsisdn());
				}
				firstSectionList.add(pair);
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3)
	{
		Object tag = view.getTag();
		if (tag instanceof Pair<?, ?>)
		{
			Pair<AtomicBoolean, ContactInfo> pair = (Pair<AtomicBoolean, ContactInfo>) tag;
			pair.first.set(!pair.first.get());
			view.setTag(pair);
			adapter.notifyDataSetChanged();
			String msisdn = pair.second.getMsisdn();
			if (type != Type.BLOCK)
			{
				if (selectedContacts.contains(msisdn))
				{
					selectedContacts.remove(msisdn);
				}
				else
				{
					selectedContacts.add(msisdn);
				}

				setupActionBarElements();

			}
			else
			{
				Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, true);
				boolean blocked = pair.first.get();
				toggleBlockMap.put(msisdn, blocked);
			}
		}
		else
		{
			String msisdn = ((ContactInfo) tag).getMsisdn();
			if (type == Type.BLOCK)
			{
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.BLOCK_USER,
						Utils.normalizeNumber(msisdn,
								getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE)));
			}
			else
			{
				msisdn = Utils.normalizeNumber(msisdn,
						getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE));
				Logger.d(getClass().getSimpleName(), "Inviting " + msisdn);
				Utils.sendInvite(msisdn, this);
				Toast.makeText(this, R.string.invite_sent, Toast.LENGTH_SHORT).show();
			}
			finish();
		}
	}

}
