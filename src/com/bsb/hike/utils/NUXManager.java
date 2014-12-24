package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.models.NuxCustomMessage;
import com.bsb.hike.models.NuxInviteFriends;
import com.bsb.hike.models.NuxSelectFriends;

public class NUXManager
{
	private static NUXManager mmManager;

	private HashSet<String> list_nux_contacts;

	private NuxInviteFriends inviteFriends;

	private NuxSelectFriends selectFriends;

	private NuxCustomMessage customMessage;

	private HikeSharedPreferenceUtil mprefs;

	private NUXManager(Context context)
	{
		list_nux_contacts = new HashSet<String>();
		mprefs = HikeSharedPreferenceUtil.getInstance(context);
		String msisdn = mprefs.getData(NUXConstants.CURRENT_NUX_CONTACTS, null);
		if (!TextUtils.isEmpty(msisdn))
		{
			String[] arrmsisdn = msisdn.split(NUXConstants.STRING_SPLIT_SEPERATOR);
			list_nux_contacts.addAll(Arrays.asList(arrmsisdn));
		}
	}

	public static NUXManager getInstance(Context context)
	{
		if (mmManager == null)
		{
			synchronized (NUXManager.class)
			{
				if (mmManager == null)
				{
					mmManager = new NUXManager(context);
				}
			}
		}
		return mmManager;
	}

	public void startNUX(Context context)
	{
		mprefs.saveData(NUXConstants.IS_NUX_ACTIVE, true);
		// Intent in=new Intent(context,);
		// context.startActivity(in);
	}

	public void saveNUXContact(HashSet<String> msisdn, Context context)
	{

		list_nux_contacts.addAll(msisdn);

		mprefs.saveData(NUXConstants.CURRENT_NUX_CONTACTS, list_nux_contacts.toString().replace("[", "").replace("]", ""));
	}

	public void removeNUXContact(HashSet<String> msisdn, Context context)
	{

		list_nux_contacts.removeAll(msisdn);

		mprefs.saveData(NUXConstants.CURRENT_NUX_CONTACTS, list_nux_contacts.toString().replace("[", "").replace("]", ""));

	}

	public int getCountCurrentNUXContacts()
	{
		return list_nux_contacts.size();
	}

	public int getMaxContacts(Context context)
	{

		return mprefs.getData(NUXConstants.MAX_NUX_CONTACTS, -1);
	}

	public void setMaxContacts(Context context, int val)
	{
		mprefs.saveData(NUXConstants.MAX_NUX_CONTACTS, val);
	}

	public int getMinContacts(Context context)
	{

		return mprefs.getData(NUXConstants.MIN_NUX_CONTACTS, -1);
	}

	public void setMinContacts(Context context, int val)
	{
		mprefs.saveData(NUXConstants.MIN_NUX_CONTACTS, val);
	}

	public void shutDownNUX(Context context)
	{
		mprefs.saveData(NUXConstants.IS_NUX_ACTIVE, false);
	}

	public boolean is_NUX_Active()
	{
		return mprefs.saveData(NUXConstants.IS_NUX_ACTIVE, false);
	}

	public HashSet<String> getAllNUXContacts()
	{
		return list_nux_contacts;
	}

	public void sendMessage(ArrayList<String> msisdn)
	{
		JSONObject mmObject = null;
		for (String r : msisdn)
		{
			mmObject = new JSONObject();

			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, mmObject);
		}

	}

	public void parseJson(String json)
	{
		try
		{

			JSONObject root = new JSONObject(json);

			mprefs.saveData(NUXConstants.INVITE_FRIENDS, root.optString(NUXConstants.INVITE_FRIENDS));
			mprefs.saveData(NUXConstants.SELECT_FRIENDS, root.optString(NUXConstants.SELECT_FRIENDS));
			mprefs.saveData(NUXConstants.CUSTOM_MESSAGE, root.optString(NUXConstants.CUSTOM_MESSAGE));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public NuxCustomMessage getNuxCustomMessagePojo()
	{
		if (customMessage == null)
		{
			try
			{
				JSONObject custommessage = new JSONObject(mprefs.getData(NUXConstants.CUSTOM_MESSAGE, ""));

				if (!TextUtils.isEmpty(custommessage.toString()))
				{
					String screentitle = custommessage.optString(NUXConstants.CM_SCREEN_TITLE);
					String smsmessage = custommessage.optString(NUXConstants.CM_DEFAULT_MESSAGE, "message");

					String buttext = custommessage.optString(NUXConstants.CM_BUTTON_TEXT);
					boolean togglecustommsg = custommessage.optBoolean(NUXConstants.CM_TOGGLE_CUSTOM_MESSAGE);
					customMessage = new NuxCustomMessage(screentitle, smsmessage, buttext, togglecustommsg);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return customMessage;
	}

	public NuxSelectFriends getNuxSelectFriendsPojo()
	{
		if (selectFriends == null)
		{

			try
			{
				JSONObject select_friends = new JSONObject(mprefs.getData(NUXConstants.SELECT_FRIENDS, ""));

				if (!TextUtils.isEmpty(select_friends.toString()))
				{

					HashSet<String> recolist = new HashSet<String>();

					String sectiontitle = select_friends.optString(NUXConstants.SF_SECTION_TITLE);
					String recosectiontitle = select_friends.optString(NUXConstants.SF_RECO_SECTION_TITLE);
					boolean togglerecosection = select_friends.optBoolean(NUXConstants.SF_RECO_SECTION_TOGGLE);
					JSONArray mmArray = select_friends.optJSONArray(NUXConstants.SF_RECO_LIST);

					for (int i = 0; i < mmArray.length(); i++)
					{
						recolist.add(mmArray.optString(i));
					}
					boolean togglecontactsection = select_friends.optBoolean(NUXConstants.SF_CONTACT_SECTION_TOGGLE);
					String buttext = select_friends.optString(NUXConstants.SF_BUTTON_TEXT);
					String progresstext = select_friends.optString(NUXConstants.SF_PROGRESS_TEXT);
					boolean searchtoggle = select_friends.optBoolean(NUXConstants.SF_SEARCH_TOGGLE);
					selectFriends = new NuxSelectFriends(sectiontitle, recosectiontitle, togglerecosection, recolist, togglecontactsection, buttext, progresstext, searchtoggle);
				}
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return selectFriends;
	}

	public NuxInviteFriends getNuxInviteFriendsPojo()
	{

		if (inviteFriends == null)
		{
			try
			{
				JSONObject incentive_reward = new JSONObject(mprefs.getData(NUXConstants.INVITE_FRIENDS, ""));
				if (!TextUtils.isEmpty(incentive_reward.toString()))
				{
					mprefs.saveData(NUXConstants.INCENTIVE_ID, incentive_reward.optInt(NUXConstants.INCENTIVE_ID));
					mprefs.saveData(NUXConstants.ACTIVITY_ID, incentive_reward.optInt(NUXConstants.ACTIVITY_ID));
					mprefs.saveData(NUXConstants.INCR_MIN, incentive_reward.optInt(NUXConstants.INCR_MIN));
					mprefs.saveData(NUXConstants.INCR_MAX, incentive_reward.optInt(NUXConstants.INCR_MAX));

					String text = incentive_reward.optString(NUXConstants.INVITEFRDS_TEXT);
					String image = incentive_reward.optString(NUXConstants.INVITEFRDS_IMAGE);
					boolean skip_toggle_button = incentive_reward.optBoolean(NUXConstants.INVITEFRDS_SKIP_TOGGLE_BUTTON);
					String title = incentive_reward.optString(NUXConstants.INVITEFRDS_MAIN_TITLE);
					String buttext = incentive_reward.optString(NUXConstants.INVITEFRDS_BUT_TEXT);
					inviteFriends = new NuxInviteFriends(title, text, buttext, image, skip_toggle_button);
				}
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return inviteFriends;
	}

	public void setCurrentState(int mmCurrentState)
	{
		mprefs.saveData(NUXConstants.CURRENT_NUX_ACTIVITY, mmCurrentState);
	}

	public int getCurrentState()
	{
		return mprefs.getData(NUXConstants.CURRENT_NUX_ACTIVITY, NUXConstants.OTHER_ACTIVITY);
	}

	public void startCurrentActivity(Context context)
	{

		if (mprefs.getData(NUXConstants.IS_NUX_ACTIVE, false))
		{
			switch (mprefs.getData(NUXConstants.CURRENT_NUX_ACTIVITY, -1))
			{
			case NUXConstants.INVFRD:
				IntentManager.openInviteFriends(context);
				break;
			case NUXConstants.SELFRD:
				IntentManager.openSelectFriends(context);

				break;
			case NUXConstants.CUSMES:
				IntentManager.openCustomMessage(context);
				break;
			}
		}
	}

	public void removeData(Context context)
	{
		HikeSharedPreferenceUtil.getInstance(context).removeData(NUXConstants.CURRENT_NUX_CONTACTS);
	}

	public void putJsonData()
	{
		JSONObject root = new JSONObject();
		JSONObject incentive_reward = new JSONObject();

		JSONObject select_friends = new JSONObject();
		try
		{
			incentive_reward.put(NUXConstants.INVITEFRDS_TEXT, "abcd");
			incentive_reward.put(NUXConstants.INCENTIVE_ID, 1);
			incentive_reward.put(NUXConstants.ACTIVITY_ID, 2);
			incentive_reward.put(NUXConstants.INCR_MAX, -3);
			incentive_reward.put(NUXConstants.INCR_MIN, 4);
			incentive_reward.put(NUXConstants.INVITEFRDS_BUT_TEXT, "abcd");
			incentive_reward.put(NUXConstants.INVITEFRDS_IMAGE, "");
			incentive_reward.put(NUXConstants.INVITEFRDS_SKIP_TOGGLE_BUTTON, false);
			incentive_reward.put(NUXConstants.INVITEFRDS_MAIN_TITLE, "main_title");
			root.put(NUXConstants.INVITE_FRIENDS, incentive_reward);

			select_friends.put(NUXConstants.SF_BUTTON_TEXT, "button_text");
			select_friends.put(NUXConstants.SF_CONTACT_SECTION_TOGGLE, true);
			select_friends.put(NUXConstants.SF_PROGRESS_TEXT, "progress_text");
			select_friends.put(NUXConstants.SF_RECO_SECTION_TITLE, "My Recoomendations");
			select_friends.put(NUXConstants.SF_RECO_SECTION_TOGGLE, true);
			select_friends.put(NUXConstants.SF_SEARCH_TOGGLE, true);
			select_friends.put(NUXConstants.SF_SECTION_TITLE, "section_title");

			JSONArray mmarray = new JSONArray();
			mmarray.put("+91-987656");
			mmarray.put("+91-273623");
			mmarray.put("+92827722");

			select_friends.put(NUXConstants.SF_RECO_LIST, mmarray);

			root.put(NUXConstants.SELECT_FRIENDS, select_friends);

			JSONObject custommessage = new JSONObject();
			custommessage.put(NUXConstants.CM_BUTTON_TEXT, "bu_text");
			custommessage.put(NUXConstants.CM_DEFAULT_MESSAGE, "message");
			custommessage.put(NUXConstants.CM_SCREEN_TITLE, "screen tilit");
			custommessage.put(NUXConstants.CM_TOGGLE_CUSTOM_MESSAGE, true);

			root.put(NUXConstants.CUSTOM_MESSAGE, custommessage);

			parseJson(root.toString());
			Logger.d(NUXConstants.TAG, root.toString());

		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
