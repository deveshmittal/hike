package com.bsb.hike.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeInviteAdapter;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.utils.Utils;
import com.fiksu.asotracking.FiksuTrackingManager;

public class HikeListActivity extends Activity implements OnItemClickListener,
		OnClickListener {

	private enum Type {
		NUX1, NUX2, INVITE, BLOCK
	}

	private HikeInviteAdapter adapter;
	private ListView listView;
	private TextView labelView;
	private Button titleBtn;
	private EditText input;
	private Set<String> selectedContacts;
	private Type type;
	private Map<String, Boolean> toggleBlockMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikelistactivity);

		if (getIntent().getBooleanExtra(
				HikeConstants.Extras.SHOW_MOST_CONTACTED, false)) {
			type = Type.NUX1;
		} else if (getIntent().getBooleanExtra(
				HikeConstants.Extras.SHOW_FAMILY, false)) {
			type = Type.NUX2;
		} else if (getIntent().getBooleanExtra(
				HikeConstants.Extras.BLOCKED_LIST, false)) {
			type = Type.BLOCK;
		} else {
			type = Type.INVITE;
		}

		selectedContacts = new HashSet<String>();

		labelView = (TextView) findViewById(R.id.title);
		listView = (ListView) findViewById(R.id.contact_list);
		titleBtn = (Button) findViewById(R.id.title_icon);
		input = (EditText) findViewById(R.id.input_number);

		titleBtn.setVisibility(View.VISIBLE);
		findViewById(R.id.button_bar_2).setVisibility(View.VISIBLE);

		listView.setTextFilterEnabled(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);

		HikeUserDatabase hUDB = HikeUserDatabase.getInstance();
		List<Pair<AtomicBoolean, ContactInfo>> contactList = null;

		TextView nuxText = (TextView) findViewById(R.id.nux_text);
		switch (type) {
		case NUX1:
			findViewById(R.id.input_number_container).setVisibility(View.GONE);
			nuxText.setVisibility(View.VISIBLE);
			nuxText.setText(R.string.which_friend_invite);
			titleBtn.setText(R.string.next_signup);
			contactList = hUDB
					.getNonHikeMostContactedContacts(HikeConstants.MAX_NUX_CONTACTS);
			labelView.setText(R.string.invite_friends);
			labelView.setOnClickListener(this);
			break;
		case NUX2:
			findViewById(R.id.input_number_container).setVisibility(View.GONE);
			nuxText.setVisibility(View.VISIBLE);
			nuxText.setText(R.string.which_family);
			titleBtn.setText(R.string.done);
			contactList = hUDB.getFamilyList(this,
					HikeConstants.MAX_NUX_CONTACTS);
			labelView.setText(R.string.invite_family);
			labelView.setOnClickListener(this);
			break;
		case BLOCK:
			titleBtn.setText(R.string.done);
			contactList = hUDB.getBlockedUserList();
			toggleBlockMap = new HashMap<String, Boolean>();
			labelView.setText(R.string.blocked_list);
			break;
		case INVITE:
			titleBtn.setText(R.string.send);
			contactList = hUDB.getNonHikeContacts();
			labelView.setText(R.string.invite_via_sms);
			break;
		}

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

		adapter = new HikeInviteAdapter(this, -1, contactList,
				type == Type.BLOCK);
		input.addTextChangedListener(adapter);

		listView.setAdapter(adapter);
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
				FiksuTrackingManager.uploadPurchaseEvent(this,
						HikeConstants.INVITE, HikeConstants.INVITE_SENT,
						HikeConstants.CURRENCY);
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.MQTT_PUBLISH,
						Utils.makeHike2SMSInviteMessage(msisdn, this)
								.serialize());
			}
			if (!selectedContacts.isEmpty() || type == Type.NUX1
					|| type == Type.NUX2) {
				if (!selectedContacts.isEmpty()) {
					Toast.makeText(
							getApplicationContext(),
							selectedContacts.size() > 1 ? R.string.invites_sent
									: R.string.invite_sent, Toast.LENGTH_SHORT)
							.show();
				}
				if (type == Type.NUX1) {
					Editor editor = getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
							.edit();
					editor.putBoolean(HikeMessengerApp.NUX1_DONE, true);
					editor.commit();

					Intent i = new Intent(this, HikeListActivity.class);
					i.putExtra(HikeConstants.Extras.SHOW_FAMILY, true);
					startActivity(i);
				} else if (type == Type.NUX2) {
					Editor editor = getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
							.edit();
					editor.putBoolean(HikeMessengerApp.NUX2_DONE, true);
					editor.commit();

					Intent i = new Intent(this, MessagesList.class);
					i.putExtra(HikeConstants.Extras.FIRST_TIME_USER, true);
					startActivity(i);
				}
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
				finish();
			}
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
			} else {
				boolean blocked = pair.first.get();
				toggleBlockMap.put(msisdn, blocked);
			}
		} else {
			String msisdn = ((ContactInfo) tag).getMsisdn();
			msisdn = Utils.normalizeNumber(
					msisdn,
					getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
							MODE_PRIVATE).getString(
							HikeMessengerApp.COUNTRY_CODE,
							HikeConstants.INDIA_COUNTRY_CODE));
			Log.d(getClass().getSimpleName(), "Inviting " + msisdn);
			FiksuTrackingManager.uploadPurchaseEvent(this,
					HikeConstants.INVITE, HikeConstants.INVITE_SENT,
					HikeConstants.CURRENCY);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
					Utils.makeHike2SMSInviteMessage(msisdn, this).serialize());
			Toast.makeText(this, R.string.invite_sent, Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public void onClick(View v) {
		skipNux();
	}

	private void skipNux() {
		selectedContacts.clear();
		onTitleIconClick(null);
	}
}
