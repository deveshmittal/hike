package com.bsb.hike.modules.contactmgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.database.DatabaseUtils;
import android.util.Pair;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.modules.contactmgr.db.HikeUserDatabase;
import com.bsb.hike.utils.Logger;

/**
 * @author Gautam & Sidharth
 * 
 *         This class is used as a cache to store the contacts. This hides the mechanism of storing the contacts object from the clients and the manager. This class should be
 *         enhanced properly when logic of caching is changed. All the threading stuff should be handled by this class only. Most of the operation will be read so synchronizing
 *         with mutex is not good. Taking explicit read write locks will enhance performance.
 */
abstract class ContactsCache
{
	protected boolean allContactsLoaded = false;

	/**
	 * @param key
	 * @return contact object if present and null if not present
	 */
	abstract ContactInfo getContact(String key);

	abstract void insertContact(ContactInfo contact);

	abstract void insertContact(ContactInfo contact, String name);

	abstract void removeContact(String msisdn);

	/**
	 * @param key
	 * @param c
	 *            This function will update the contact object in the corresponding memory
	 */
	abstract void updateContact(ContactInfo contact);

	abstract void updateContact(ContactInfo contact, String name);

	abstract void loadMemory();

	abstract ContactInfo loadMemory(String msisdn, boolean ifNotFoundReturnNull);

	abstract List<ContactInfo> loadMemory(List<String> msisdns);

	abstract void clearMemory();

	abstract void setUnknownContactName(String msisdn, String name);
}
