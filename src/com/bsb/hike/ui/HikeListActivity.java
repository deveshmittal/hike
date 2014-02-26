package com.bsb.hike.ui;

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
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
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

	private ViewGroup doneContainer;

	private TextView doneText;

	private Button doneBtn;

	private TextView title;

	private ImageView backIcon;

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

		selectedContacts = new HashSet<String>();

		listView = (ListView) findViewById(R.id.contact_list);
		input = (EditText) findViewById(R.id.input_number);

		listView.setTextFilterEnabled(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);

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
	}

	private void init()
	{
		if (type != Type.BLOCK)
		{
			selectedContacts.clear();
			doneContainer.setVisibility(View.GONE);
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

		if (type != Type.BLOCK)
		{
			doneContainer = (ViewGroup) actionBarView.findViewById(R.id.done_container);

			int padding = (int) (7 * Utils.densityMultiplier);
			doneContainer.setPadding(padding, 0, padding, 0);

			doneText = (TextView) actionBarView.findViewById(R.id.done_text);
			doneText.setTextSize(14);
			doneText.setTypeface(doneText.getTypeface(), Typeface.BOLD);

			View tickView = actionBarView.findViewById(R.id.ic_tick);
			tickView.setVisibility(View.GONE);

			doneContainer.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					showNativeSMSPopup();
				}
			});
		}
		else
		{
			doneBtn = (Button) actionBarView.findViewById(R.id.post_btn);
			doneBtn.setVisibility(View.VISIBLE);
			doneBtn.setText(R.string.save);
			doneBtn.setEnabled(false);

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
				}
				else
				{
					intent = new Intent(HikeListActivity.this, SettingsActivity.class);
				}
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);

			}
		});

		actionBar.setCustomView(actionBarView);

		init();
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

			switch (type)
			{
			case BLOCK:
				/*
				 * This would be true when we have pre checked items.
				 */
				for (Pair<AtomicBoolean, ContactInfo> contactItem : contactList)
				{
					boolean checked = contactItem.first.get();
					if (checked)
					{
						selectedContacts.add(contactItem.second.getMsisdn());
					}
					else
					{
						break;
					}
				}
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
				break;
			}

			adapter = new HikeInviteAdapter(HikeListActivity.this, -1, contactList, type == Type.BLOCK);
			input.addTextChangedListener(adapter);

			listView.setAdapter(adapter);
		}
	}

	public void selectAllToggled(boolean isChecked)
	{
		List<Pair<AtomicBoolean, ContactInfo>> contactList = adapter.getCompleteList();

		for (Pair<AtomicBoolean, ContactInfo> pair : contactList)
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
		adapter.selectAllToggled();
		setupActionBarElements();
	}

	private List<Pair<AtomicBoolean, ContactInfo>> getContactList()
	{
		HikeUserDatabase hUDB = HikeUserDatabase.getInstance();

		switch (type)
		{
		case BLOCK:
			return hUDB.getBlockedUserList();
		case INVITE:
			return hUDB.getNonHikeContacts();
		}
		return null;
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
					Log.d(getClass().getSimpleName(), "Inviting " + msisdn);
					Utils.sendInvite(msisdn, this, false, true);

					inviteArray.put(msisdn);
				}
				data.put(HikeConstants.MESSAGE_ID, time);
				data.put(HikeConstants.LIST, inviteArray);

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
			doneContainer.setVisibility(View.VISIBLE);
			doneText.setText(getString(R.string.send_invite, selectedContacts.size()));
		}
		else
		{
			init();
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
				doneBtn.setEnabled(true);
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
				Log.d(getClass().getSimpleName(), "Inviting " + msisdn);
				Utils.sendInvite(msisdn, this);
				Toast.makeText(this, R.string.invite_sent, Toast.LENGTH_SHORT).show();
			}
			finish();
		}
	}

}
