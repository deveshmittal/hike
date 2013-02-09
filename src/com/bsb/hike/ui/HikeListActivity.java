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

public class HikeListActivity extends Activity implements OnItemClickListener {

	private enum Type {
		NUX, INVITE, BLOCK
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
			type = Type.NUX;
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

		titleBtn.setText(type == Type.BLOCK ? R.string.done : R.string.send);
		titleBtn.setVisibility(View.VISIBLE);

		findViewById(R.id.button_bar_2).setVisibility(View.VISIBLE);

		if (type == Type.NUX) {
			findViewById(R.id.input_number_container).setVisibility(View.GONE);
			findViewById(R.id.nux_text).setVisibility(View.VISIBLE);
		}

		listView.setTextFilterEnabled(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);

		HikeUserDatabase hUDB = HikeUserDatabase.getInstance();
		List<Pair<AtomicBoolean, ContactInfo>> contactList;
		if (type == Type.NUX) {
			contactList = hUDB.getNonHikeMostContactedContacts(50);
		} else if (type == Type.BLOCK) {
			contactList = hUDB.getBlockedUserList();
			toggleBlockMap = new HashMap<String, Boolean>();
		} else {
			contactList = hUDB.getNonHikeContacts();
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

		if (type == Type.NUX) {
			labelView.setText(R.string.invite_friends);
		} else if (type == Type.BLOCK) {
			labelView.setText(R.string.blocked_list);
		} else {
			labelView.setText(R.string.invite_via_sms);
		}

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
			if (!selectedContacts.isEmpty() || type == Type.NUX) {
				if (!selectedContacts.isEmpty()) {
					Toast.makeText(getApplicationContext(),
							R.string.invites_sent, Toast.LENGTH_SHORT).show();
				}
				if (type == Type.NUX) {
					Editor editor = getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
							.edit();
					editor.putBoolean(HikeMessengerApp.SHOWN_TUTORIAL, true);
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
}
