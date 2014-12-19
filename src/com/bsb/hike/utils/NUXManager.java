package com.bsb.hike.utils;

import java.util.ArrayList;

import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

import android.content.Context;

public class NUXManager
{
	private static NUXManager mmManager;

	private static final String IS_NUX_ACTIVE = "is_nux_active";

	private static final String MIN_NUX_CONTACTS = "min_nux_contacts";

	private static final String MAX_NUX_CONTACTS = "max_nux_contacts";

	private static final String CURRENT_NUX_CONTACTS = "current_nux_contacts";

	private static final String STRING_SPLIT_SEPERATOR = ", ";

	private ArrayList<String> list_nux_contacts;

	private NUXManager()
	{
		list_nux_contacts = new ArrayList<String>();
	}

	public static NUXManager getInstance(Context context)
	{
		if (mmManager == null)
		{
			synchronized (NUXManager.class)
			{
				mmManager = new NUXManager();
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

	public void storeNUXContact(ArrayList<String> msisdn, Context context)
	{
		HikeSharedPreferenceUtil mmpref = HikeSharedPreferenceUtil.getInstance(context);

		String presentlist = mmpref.getData(CURRENT_NUX_CONTACTS, null);
		String contacts = msisdn.toString();
		contacts = contacts.replace("[", "").replace("]", "");
		if (presentlist != null)
		{
			presentlist = presentlist.concat(contacts);
		}
		else
		{
			presentlist = contacts;
		}
		mmpref.saveData(CURRENT_NUX_CONTACTS, presentlist);
		list_nux_contacts.addAll(msisdn);
	}

	public void removeNUXCONTACT(ArrayList<String> msisdn, Context context)
	{
		HikeSharedPreferenceUtil mmpref = HikeSharedPreferenceUtil.getInstance(context);
		String presentlist = mmpref.getData(CURRENT_NUX_CONTACTS, null);

		if (presentlist == null)
			return;

		list_nux_contacts.removeAll(msisdn);
		String contacts = list_nux_contacts.toString();
		contacts = contacts.replace("[", "").replace("]", "");

		mmpref.saveData(CURRENT_NUX_CONTACTS, contacts);

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

	public ArrayList<String> getAllNUXContacts()
	{
		return list_nux_contacts;
	}

	public void sendMessage(ArrayList<String> msisdn)
	{
		JSONObject mmObject = null;
		for (String s : msisdn)
		{
			mmObject = new JSONObject();

			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, mmObject);
		}

	}
	
	public void parseJson(String json)
	{
		
	}
}
