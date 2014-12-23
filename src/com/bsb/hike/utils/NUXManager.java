package com.bsb.hike.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

public class NUXManager
{
	private static NUXManager mmManager;

	private static final String IS_NUX_ACTIVE = "is_nux_active";

	private static final String MIN_NUX_CONTACTS = "min_nux_contacts";

	private static final String MAX_NUX_CONTACTS = "max_nux_contacts";

	private static final String CURRENT_NUX_CONTACTS = "current_nux_contacts";

	private static final String STRING_SPLIT_SEPERATOR = ", ";

	private HashSet<String> list_nux_contacts;

	private NUXManager(Context context)
	{
		list_nux_contacts = new HashSet<String>();
		HikeSharedPreferenceUtil mmprefs = HikeSharedPreferenceUtil.getInstance(context);
		String msisdn = mmprefs.getData(CURRENT_NUX_CONTACTS, null);
		if (!TextUtils.isEmpty(msisdn))
		{
			String[] arrmsisdn = msisdn.split(STRING_SPLIT_SEPERATOR);
			list_nux_contacts.addAll(Arrays.asList(arrmsisdn));
		}
	}

	public static NUXManager getInstance(Context context)
	{
		if (mmManager == null)
		{
			synchronized (NUXManager.class)
			{
				mmManager = new NUXManager(context);
			}
		}
		return mmManager;
	}

	public void startNUX(Context context)
	{
		HikeSharedPreferenceUtil mmprefs = HikeSharedPreferenceUtil.getInstance(context);
		mmprefs.saveData(IS_NUX_ACTIVE, true);
		// Intent in=new Intent(context,);
		// context.startActivity(in);
	}

	public void saveNUXContact(HashSet<String> msisdn, Context context)
	{
		HikeSharedPreferenceUtil mmpref = HikeSharedPreferenceUtil.getInstance(context);

		list_nux_contacts.addAll(msisdn);

		mmpref.saveData(CURRENT_NUX_CONTACTS, list_nux_contacts.toString().replace("[", "").replace("]", ""));
	}

	public void removeNUXContact(HashSet<String> msisdn, Context context)
	{
		HikeSharedPreferenceUtil mmpref = HikeSharedPreferenceUtil.getInstance(context);

		list_nux_contacts.removeAll(msisdn);

		mmpref.saveData(CURRENT_NUX_CONTACTS, list_nux_contacts.toString().replace("[", "").replace("]", ""));

	}

	public int getCountCurrentNUXContacts()
	{
		return list_nux_contacts.size();
	}

	public int getMaxContacts(Context context)
	{
		HikeSharedPreferenceUtil mmprefs = HikeSharedPreferenceUtil.getInstance(context);

		return mmprefs.getData(MAX_NUX_CONTACTS, -1);
	}

	public void setMaxContacts(Context context, int val)
	{
		HikeSharedPreferenceUtil.getInstance(context).saveData(MAX_NUX_CONTACTS, val);
	}

	public int getMinContacts(Context context)
	{
		HikeSharedPreferenceUtil mmprefs = HikeSharedPreferenceUtil.getInstance(context);

		return mmprefs.getData(MIN_NUX_CONTACTS, -1);
	}

	public void setMinContacts(Context context, int val)
	{
		HikeSharedPreferenceUtil.getInstance(context).saveData(MIN_NUX_CONTACTS, val);
	}

	public void shutDownNUX(Context context)
	{
		HikeSharedPreferenceUtil mmpref = HikeSharedPreferenceUtil.getInstance(context);
		mmpref.saveData(IS_NUX_ACTIVE, false);
	}

	public boolean is_NUX_Active(Context context)
	{
		return HikeSharedPreferenceUtil.getInstance(context).getData(IS_NUX_ACTIVE, false);
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

	}

	public void removeData(Context context)
	{
		HikeSharedPreferenceUtil.getInstance(context).removeData(CURRENT_NUX_CONTACTS);
	}
}
