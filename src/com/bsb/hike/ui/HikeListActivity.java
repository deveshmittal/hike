package com.bsb.hike.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
import com.bsb.hike.adapters.HikeInviteAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class HikeListActivity extends HikeAppStateBaseFragmentActivity
		implements OnItemClickListener {

	private enum Type {
		INVITE, BLOCK
	}

	private HikeInviteAdapter adapter;
	private ListView listView;
	private EditText input;
	private Set<String> selectedContacts;
	private Type type;
	private Map<String, Boolean> toggleBlockMap;

	private Button doneBtn;
	private TextView title;
	private ImageView backIcon;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikelistactivity);

		if (getIntent().getBooleanExtra(HikeConstants.Extras.BLOCKED_LIST,
				false)) {
			type = Type.BLOCK;
		} else {
			type = Type.INVITE;
		}

		selectedContacts = new HashSet<String>();

		listView = (ListView) findViewById(R.id.contact_list);
		input = (EditText) findViewById(R.id.input_number);

		listView.setTextFilterEnabled(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);

		switch (type) {
		case BLOCK:
			toggleBlockMap = new HashMap<String, Boolean>();
			break;
		case INVITE:
			break;
		}
		setupActionBar();
		Utils.executeContactListResultTask(new SetupContactList());
	}

	private void init() {
		if (type != Type.BLOCK) {
			selectedContacts.clear();
			doneBtn.setVisibility(View.GONE);
		}
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
		title = (TextView) actionBarView.findViewById(R.id.title);

		if (type != Type.BLOCK) {
			doneBtn = (Button) actionBarView.findViewById(R.id.done_btn);
		} else {
			doneBtn = (Button) actionBarView.findViewById(R.id.post_btn);
			doneBtn.setVisibility(View.VISIBLE);
			doneBtn.setText(R.string.save);
			doneBtn.setEnabled(false);
		}

		backContainer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = null;
				if (type != Type.BLOCK) {
					if (getIntent().getBooleanExtra(
							HikeConstants.Extras.FROM_CREDITS_SCREEN, false)) {
						intent = new Intent(HikeListActivity.this,
								CreditsActivity.class);
					} else {
						intent = new Intent(HikeListActivity.this,
								TellAFriend.class);
					}
				} else {
					intent = new Intent(HikeListActivity.this,
							SettingsActivity.class);
				}
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);

			}
		});

		doneBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (type == Type.INVITE) {
					showNativeSMSPopup();
				} else {
					onTitleIconClick(null);
				}
			}
		});

		actionBar.setCustomView(actionBarView);

		init();
	}

	private void setLabel() {
		if (type != Type.BLOCK) {
			title.setText(R.string.invite_sms);
		} else {
			title.setText(R.string.blocked_list);
		}
	}

	private void showNativeSMSPopup() {
		final Dialog dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.enable_sms_client_popup);
		dialog.setCancelable(true);

		TextView header = (TextView) dialog.findViewById(R.id.header);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
		Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

		btnCancel.setVisibility(View.GONE);
		header.setText(R.string.native_header);
		body.setText(R.string.native_info);

		btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onTitleIconClick(null);
			}
		});

		dialog.show();
	}

	private class SetupContactList extends
			AsyncTask<Void, Void, List<Pair<AtomicBoolean, ContactInfo>>> {

		boolean loadOnUiThread;

		@Override
		protected void onPreExecute() {
			loadOnUiThread = Utils.loadOnUiThread();
			findViewById(R.id.progress_container).setVisibility(
					loadOnUiThread ? View.GONE : View.VISIBLE);
		}

		@Override
		protected List<Pair<AtomicBoolean, ContactInfo>> doInBackground(
				Void... params) {
			if (loadOnUiThread) {
				return null;
			} else {
				return getContactList();
			}
		}

		@Override
		protected void onPostExecute(
				List<Pair<AtomicBoolean, ContactInfo>> contactList) {
			if (contactList == null) {
				contactList = getContactList();
			}

			findViewById(R.id.progress_container).setVisibility(View.GONE);

			/*
			 * This would be true when we have pre checked items.
			 */
			if (type != Type.INVITE) {
				for (Pair<AtomicBoolean, ContactInfo> contactItem : contactList) {
					boolean checked = contactItem.first.get();
					if (checked) {
						selectedContacts.add(contactItem.second.getMsisdn());
					} else {
						break;
					}
				}
			}

			adapter = new HikeInviteAdapter(HikeListActivity.this, -1,
					contactList, type == Type.BLOCK);
			input.addTextChangedListener(adapter);

			listView.setAdapter(adapter);
		}

	}

	private List<Pair<AtomicBoolean, ContactInfo>> getContactList() {
		HikeUserDatabase hUDB = HikeUserDatabase.getInstance();

		switch (type) {
		case BLOCK:
			return hUDB.getBlockedUserList();
		case INVITE:
			return hUDB.getNonHikeContacts();
		}
		return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public void onTitleIconClick(View v) {
		if (type != Type.BLOCK) {

			Iterator<String> iterator = selectedContacts.iterator();

			while (iterator.hasNext()) {
				String msisdn = iterator.next();
				Log.d(getClass().getSimpleName(), "Inviting " + msisdn);
				Utils.sendInvite(msisdn, this);
			}

			if (!selectedContacts.isEmpty()) {
				Toast.makeText(
						getApplicationContext(),
						selectedContacts.size() > 1 ? R.string.invites_sent
								: R.string.invite_sent, Toast.LENGTH_SHORT)
						.show();
				finish();
			} else {
				Toast.makeText(getApplicationContext(),
						R.string.select_invite_contacts, Toast.LENGTH_SHORT)
						.show();
			}
		} else {
			for (Entry<String, Boolean> toggleBlockEntry : toggleBlockMap
					.entrySet()) {
				String msisdn = toggleBlockEntry.getKey();
				boolean blocked = toggleBlockEntry.getValue();

				HikeMessengerApp.getPubSub().publish(
						blocked ? HikePubSub.BLOCK_USER
								: HikePubSub.UNBLOCK_USER, msisdn);
			}
			finish();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
		Object tag = view.getTag();
		if (tag instanceof Pair<?, ?>) {
			Pair<AtomicBoolean, ContactInfo> pair = (Pair<AtomicBoolean, ContactInfo>) tag;
			pair.first.set(!pair.first.get());
			view.setTag(pair);
			adapter.notifyDataSetChanged();
			String msisdn = pair.second.getMsisdn();
			if (type != Type.BLOCK) {
				if (selectedContacts.contains(msisdn)) {
					selectedContacts.remove(msisdn);
				} else {
					selectedContacts.add(msisdn);
				}

				if (!selectedContacts.isEmpty()) {
					doneBtn.setVisibility(View.VISIBLE);
					doneBtn.setText(Integer.toString(selectedContacts.size()));
					getSupportActionBar().setBackgroundDrawable(
							getResources().getDrawable(
									R.drawable.bg_header_compose));
				} else {
					init();
				}

			} else {
				doneBtn.setEnabled(true);
				boolean blocked = pair.first.get();
				toggleBlockMap.put(msisdn, blocked);
			}
		} else {
			String msisdn = ((ContactInfo) tag).getMsisdn();
			if (type == Type.BLOCK) {
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.BLOCK_USER,
						Utils.normalizeNumber(
								msisdn,
								getSharedPreferences(
										HikeMessengerApp.ACCOUNT_SETTINGS,
										MODE_PRIVATE).getString(
										HikeMessengerApp.COUNTRY_CODE,
										HikeConstants.INDIA_COUNTRY_CODE)));
			} else {
				msisdn = Utils.normalizeNumber(
						msisdn,
						getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
								MODE_PRIVATE).getString(
								HikeMessengerApp.COUNTRY_CODE,
								HikeConstants.INDIA_COUNTRY_CODE));
				Log.d(getClass().getSimpleName(), "Inviting " + msisdn);
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.MQTT_PUBLISH,
						Utils.makeHike2SMSInviteMessage(msisdn, this)
								.serialize());
				Toast.makeText(this, R.string.invite_sent, Toast.LENGTH_SHORT)
						.show();
			}
			finish();
		}
	}

}
