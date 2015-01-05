package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.NUXConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.MultipleConvMessage;
import com.bsb.hike.models.NUXChatReward;
import com.bsb.hike.models.NUXTaskDetails;
import com.bsb.hike.models.NuxCustomMessage;
import com.bsb.hike.models.NuxInviteFriends;
import com.bsb.hike.models.NuxSelectFriends;
import com.bsb.hike.modules.contactmgr.ContactManager;

public class NUXManager
{
	private static NUXManager mmManager;

	private HashSet<String> list_nux_contacts;

	private NuxInviteFriends inviteFriends;

	private NuxSelectFriends selectFriends;

	private NuxCustomMessage customMessage;

	private NUXChatReward chatReward;

	private HikeSharedPreferenceUtil mprefs;

	private NUXTaskDetails taskDetails;

	private NUXManager(Context context)
	{
		list_nux_contacts = new HashSet<String>();
		mprefs = HikeSharedPreferenceUtil.getInstance(context, NUXConstants.NUX_SHARED_PREF);
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
		// mprefs.saveData(NUXConstants.IS_NUX_ACTIVE, true);
		context.startActivity(IntentManager.openInviteFriends(context));

		// shutDownNUX(context);
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

	// public int getMaxContacts(Context context)
	// {
	//
	// return mprefs.getData(NUXConstants.TD_MAX_CONTACTS, -1);
	// }
	//
	// public void setMaxContacts(Context context, int val)
	// {
	// mprefs.saveData(NUXConstants.TD_MAX_CONTACTS, val);
	// }
	//
	// public int getMinContacts(Context context)
	// {
	//
	// return mprefs.getData(NUXConstants.TD_MIN_CONTACTS, -1);
	// }
	//
	// public void setMinContacts(Context context, int val)
	// {
	// mprefs.saveData(NUXConstants.TD_MIN_CONTACTS, val);
	// }

	public void shutDownNUX(Context context)
	{
		mprefs.deleteAllData();
		clearALLPojo();
	}

	private void clearALLPojo()
	{
		selectFriends = null;
		taskDetails = null;
		inviteFriends = null;
		customMessage = null;

	}

	public boolean is_NUX_Active()
	{
		return mprefs.getData(NUXConstants.IS_NUX_ACTIVE, false);
	}

	public HashSet<String> getAllNUXContacts()
	{
		return list_nux_contacts;
	}

	public void sendMessage(HashSet<String> msisdn, String message, Context context)
	{
		ConvMessage convMessage = null;

		ArrayList<ContactInfo> contactList = new ArrayList<ContactInfo>(msisdn.size());

		ArrayList<ConvMessage> messageList = new ArrayList<ConvMessage>();

		for (String number : msisdn)
		{
			contactList.add(ContactManager.getInstance().getContact(number, true, true, false));

		}

		convMessage = Utils.makeConvMessage(null, message, true);
		messageList.add(convMessage);
		MultipleConvMessage multiConvMessages = new MultipleConvMessage(messageList, contactList, System.currentTimeMillis() / 1000, true, null);
		Utils.sendMultiConvMessage(multiConvMessages);
	}

	public void parseJson(String json, Context context)
	{

		try
		{

			JSONObject root = new JSONObject(
					"{  \"t\": \"nux\",  \"st\": \"new\", \"d\":{ \"task\": {    \"incentive_id\": \"8976rtfghfre456789iuhju\",    \"activity_id\": \"b7657yghh7tyhgtyghftt\",    \"incr_max\": -3,    \"incr_min\": 4,    \"max\": 6,    \"min\": 3  },  \"s\": {    \"s1\": {      \"ttl\": \"main_title\",      \"txt\": \"abcd\",      \"nxTgl\": false,      \"im\": \"images\",      \"btnTxt\": \"abcd\"    },    \"s2\": {      \"ttl1\": \"section_title\",      \"ttl2\": \"section_title\",      \"ttl3\": \"section_title\",      \"srchTgl\": true,      \"recTitle\": \"My Recoomendations\",      \"recList\": [        \"+91-987656\",        \"+91-273623\",        \"+92827722\"      ],      \"cntctTyp\": \"5values\",      \"hdeLst\": [        \"+91-987656\",        \"+91-273623\",        \"+92827722\"      ],      \"nxTxt\": \"next\"    },    \"s3\": {      \"scrnTgl\": true,      \"defMsg\": \"message\",      \"hnt\": \"tap to do something\",      \"stkrLst\": [        \"+91-987656\",        \"+91-273623\",        \"+92827722\"      ],      \"nxTxt\": \"next\"    },    \"s4\": {      \"mdlTgl\": true,      \"rwdCrdTxt\": \"image\",      \"rwdCrdTxtSccss\": \"image\",      \"sttsTxt\": \"image\",      \"chtWtngTxt\": \"image\",      \"pndngChtIcn\": \"image\",      \"dtlsTxt\": \"image\",      \"dtlsLnk\": \"image\",      \"bttn1Txt\": \"image\",      \"bttn2Txt\": \"image\",      \"tpToClmLnk\": \"image\"    },    \"s5\": {      \"hkImg\": \"image\",      \"hkTxt\": \"image\",      \"nonHkImg\": \"image\",      \"nonHkTxt\": \"image\",      \"hkBttnTxt\": \"image\",      \"hkStkID\": \"image\"    }  }}}");

			if (root.optString(HikeConstants.SUB_TYPE).equals(NUXConstants.SUBTYPE_NEW))
			{

				shutDownNUX(context);
				if (!TextUtils.isEmpty(root.optString(HikeConstants.DATA)))
				{
					JSONObject data = root.optJSONObject(HikeConstants.DATA);
					mprefs.saveData(NUXConstants.TASK_DETAILS, data.optString(NUXConstants.TASK_DETAILS));

					JSONObject mmJsonObject = data.optJSONObject(NUXConstants.SCREENS);
					if (mmJsonObject != null)
					{
						mprefs.saveData(NUXConstants.INVITE_FRIENDS, mmJsonObject.optString(NUXConstants.INVITE_FRIENDS));
						mprefs.saveData(NUXConstants.SELECT_FRIENDS, mmJsonObject.optString(NUXConstants.SELECT_FRIENDS));
						mprefs.saveData(NUXConstants.CUSTOM_MESSAGE, mmJsonObject.optString(NUXConstants.CUSTOM_MESSAGE));
						mprefs.saveData(NUXConstants.CHAT_REWARDS_BAR, mmJsonObject.optString(NUXConstants.CHAT_REWARDS_BAR));

						setCurrentState(NUXConstants.NUX_NEW);

					}
				}
			}
			else if (root.optString(HikeConstants.SUB_TYPE).equals(NUXConstants.KILLNUX))
			{
				shutDownNUX(context);
			}
			else if (root.optString(HikeConstants.SUB_TYPE).equals(NUXConstants.REFRESH))
			{

			}
			else if (root.optString(HikeConstants.SUB_TYPE).equals(NUXConstants.UNLOCK))
			{

			}
			else if (root.optString(HikeConstants.SUB_TYPE).equals(NUXConstants.REMINDER))
			{

			}

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
					ArrayList<String> stickerList = new ArrayList<String>();
					String screentitle = custommessage.optString(NUXConstants.CM_DEF_MESSAGE);
					String hint = custommessage.optString(NUXConstants.CM_HINT);
					JSONArray mmArray = custommessage.optJSONArray(NUXConstants.CM_STICKER_LIST);

					for (int i = 0; i < mmArray.length(); i++)
					{
						stickerList.add(mmArray.optString(i));
					}
					String buttext = custommessage.optString(NUXConstants.CM_BUTTON_TEXT);
					boolean togglecustommsg = custommessage.optBoolean(NUXConstants.CM_SCREEN_TOGGLE);
					customMessage = new NuxCustomMessage(screentitle, hint, buttext, togglecustommsg, stickerList);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return customMessage;
	}

	public NUXTaskDetails getNuxTaskDetailsPojo()
	{
		if (taskDetails == null)
		{
			try
			{
				JSONObject task_details = new JSONObject(mprefs.getData(NUXConstants.TASK_DETAILS, ""));
				if (!TextUtils.isEmpty(task_details.toString()))
				{

					String incentiveId = task_details.optString(NUXConstants.TD_INCENTIVE_ID);
					int incrMax = task_details.optInt(NUXConstants.TD_INCR_MAX);
					String activityId = task_details.optString(NUXConstants.TD_ACTIVITY_ID);
					int min = task_details.optInt(NUXConstants.TD_MIN_CONTACTS);
					int max = task_details.optInt(NUXConstants.TD_MAX_CONTACTS);
					int incrMin = task_details.optInt(NUXConstants.TD_INCR_MIN);
					taskDetails = new NUXTaskDetails(incentiveId, activityId, incrMax, incrMin, min, max);
				}
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return taskDetails;
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

					ArrayList<String> recoList = new ArrayList<String>();
					ArrayList<String> hideList = new ArrayList<String>();
					String sectionTitle = select_friends.optString(NUXConstants.SF_SECTION_TITLE);
					String recoSectionTitle = select_friends.optString(NUXConstants.SF_RECO_SECTION_TITLE);
					JSONArray mmArray = select_friends.optJSONArray(NUXConstants.SF_RECO_LIST);

					for (int i = 0; i < mmArray.length(); i++)
					{
						recoList.add(mmArray.optString(i));
					}

					mmArray = select_friends.optJSONArray(NUXConstants.SF_HIDE_LIST);

					for (int i = 0; i < mmArray.length(); i++)
					{
						hideList.add(mmArray.optString(i));
					}
					boolean toggleContactSection = select_friends.optBoolean(NUXConstants.SF_CONTACT_SECTION_TOGGLE);
					String butText = select_friends.optString(NUXConstants.SF_BUTTON_TEXT);
					String title2 = select_friends.optString(NUXConstants.SF_SECTION_TITLE2);
					String title3 = select_friends.optString(NUXConstants.SF_SECTION_TITLE3);
					boolean searchToggle = select_friends.optBoolean(NUXConstants.SF_SEARCH_TOGGLE);
					int contactSectionType = select_friends.optInt(NUXConstants.SF_CONTACT_SECTION_TYPE);
					selectFriends = new NuxSelectFriends(sectionTitle, title2, title3, recoSectionTitle, recoList, hideList, toggleContactSection, butText, searchToggle,
							contactSectionType);
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

	public NUXChatReward getNuxChatRewardPojo()
	{
		if (chatReward == null)
		{

			try
			{
				JSONObject chatrewardobj = new JSONObject(mprefs.getData(NUXConstants.CHAT_REWARDS_BAR, ""));
				if (!TextUtils.isEmpty(chatrewardobj.toString()))
				{

					boolean toggleModule = chatrewardobj.optBoolean(NUXConstants.CR_MODULE_TOGGLE);
					String rewardCardText = chatrewardobj.optString(NUXConstants.CR_REWARD_CARD_TEXT);
					String rewardCardSuccessText = chatrewardobj.optString(NUXConstants.CR_REWARD_CARD_SUCCESS_TEXT);
					String statusText = chatrewardobj.optString(NUXConstants.CR_STATUS_TEXT);
					String chatWaitingText = chatrewardobj.optString(NUXConstants.CR_CHAT_WAITING_TEXT);
					String pendingChatIcon = chatrewardobj.optString(NUXConstants.CR_PENDINGCHAT_ICON);
					String detailsText = chatrewardobj.optString(NUXConstants.CR_DETAILS_TEXT);
					String detailsLink = chatrewardobj.optString(NUXConstants.CR_DETAILS_LINK);
					String button1Text = chatrewardobj.optString(NUXConstants.CR_BUTTON1_TEXT);
					String button2Text = chatrewardobj.optString(NUXConstants.CR_BUTTON2_TEXT);
					String tapToClaimLink = chatrewardobj.optString(NUXConstants.CR_TAPTOCLAIM);
					chatReward = new NUXChatReward(toggleModule, rewardCardText, rewardCardSuccessText, statusText, chatWaitingText, pendingChatIcon, detailsText, detailsLink,
							button1Text, button2Text, tapToClaimLink);

				}
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return chatReward;
	}

	public void setCurrentState(int mmCurrentState)
	{
		mprefs.saveData(NUXConstants.CURRENT_NUX_ACTIVITY, mmCurrentState);
	}

	public int getCurrentState()
	{
		return mprefs.getData(NUXConstants.CURRENT_NUX_ACTIVITY, NUXConstants.NUX_KILLED);
	}

	public void startCurrentActivity(Context context)
	{

		if (mprefs.getData(NUXConstants.IS_NUX_ACTIVE, false))
		{
			switch (mprefs.getData(NUXConstants.CURRENT_NUX_ACTIVITY, -1))
			{
			case NUXConstants.NUX_NEW:
				IntentManager.openInviteFriends(context);
				break;
			// case NUXConstants.SELFRD:
			// IntentManager.openSelectFriends(context);
			// break;
			case NUXConstants.NUX_SKIPPED:
				IntentManager.openCustomMessage(context);
				break;
			}
		}
	}

	public void removeData(Context context)
	{
		HikeSharedPreferenceUtil.getInstance(context).removeData(NUXConstants.CURRENT_NUX_CONTACTS);
	}

	public void sendMsisdnListToServer(HashSet<String> msisdn)
	{
		JSONObject root = new JSONObject();
		try
		{
			root.put(HikeConstants.TYPE, HikeConstants.NUX);
			root.put(HikeConstants.SUB_TYPE, NUXConstants.INVITE);
			JSONArray mmArray = new JSONArray(msisdn);
			JSONObject object = new JSONObject();
			object.put(NUXConstants.INVITE_ARRAY, mmArray);
			root.put(HikeConstants.DATA, object);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, root.toString());

		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void putJsonData()
	{
		JSONObject root = new JSONObject();
		JSONObject incentive_reward = new JSONObject();

		JSONObject select_friends = new JSONObject();
		try
		{
			incentive_reward.put(NUXConstants.INVITEFRDS_TEXT, "abcd");
			incentive_reward.put(NUXConstants.TD_INCENTIVE_ID, 1);
			incentive_reward.put(NUXConstants.TD_ACTIVITY_ID, 2);
			incentive_reward.put(NUXConstants.TD_INCR_MAX, -3);
			incentive_reward.put(NUXConstants.TD_INCR_MIN, 4);
			incentive_reward.put(NUXConstants.INVITEFRDS_BUT_TEXT, "abcd");
			incentive_reward.put(NUXConstants.INVITEFRDS_IMAGE, "");
			incentive_reward.put(NUXConstants.INVITEFRDS_SKIP_TOGGLE_BUTTON, false);
			incentive_reward.put(NUXConstants.INVITEFRDS_MAIN_TITLE, "main_title");
			root.put(NUXConstants.INVITE_FRIENDS, incentive_reward);

			select_friends.put(NUXConstants.SF_BUTTON_TEXT, "button_text");
			select_friends.put(NUXConstants.SF_CONTACT_SECTION_TOGGLE, true);
			select_friends.put(NUXConstants.SF_SECTION_TITLE2, "progress_text");
			select_friends.put(NUXConstants.SF_RECO_SECTION_TITLE, "My Recoomendations");
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
			custommessage.put(NUXConstants.CM_DEF_MESSAGE, "screen tilit");
			custommessage.put(NUXConstants.CM_HINT, true);

			root.put(NUXConstants.CUSTOM_MESSAGE, custommessage);
			Logger.d(NUXConstants.TAG, root.toString());
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
