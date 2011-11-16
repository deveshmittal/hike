package com.bsb.im.service;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.PrivacyList;
import org.jivesoftware.smack.PrivacyListListener;
import org.jivesoftware.smack.PrivacyListManager;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PrivacyItem;
import org.jivesoftware.smack.packet.PrivacyItem.PrivacyRule;

import com.bsb.im.service.aidl.IPrivacyListListener;
import com.bsb.im.service.aidl.IPrivacyListManager;

import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;


/**
 * An adapter for the Smack's PrivacyListManager.
 */
public class PrivacyListManagerAdapter extends IPrivacyListManager.Stub {

    /** Class's Tag. */
    public static final String TAG = "PrivacyListManagerAdapter";

    private final PrivacyListManager mPrivacyListManager;

    private final RemoteCallbackList<IPrivacyListListener> mPrivacyListListeners =
	new RemoteCallbackList<IPrivacyListListener>();
    private final PrivacyListListenerAdapter mPrivacyListListener = new PrivacyListListenerAdapter();

    /**
     * Constructor.
     * @param privacyListManager the privacy list manager
     */
    public PrivacyListManagerAdapter(final PrivacyListManager privacyListManager) {
	mPrivacyListManager = privacyListManager;
	mPrivacyListManager.addListener(mPrivacyListListener);
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#blockUser(java.lang.String, java.lang.String)
     */
    @Override
    public void blockUser(String listName, String jid) throws RemoteException {
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#createPrivacyList(java.lang.String, java.util.List)
     */
    @Override
    public void createPrivacyList(String listName, List<PrivacyListItem> items) throws RemoteException {
	Log.d(TAG, "BEGIN createPrivacyList.");
	try {
	    List<PrivacyItem> privacyItems = new ArrayList<PrivacyItem>();

	    PrivacyItem item = new PrivacyItem(PrivacyItem.Type.subscription.name(), true, 2);
	    item.setValue(PrivacyRule.SUBSCRIPTION_BOTH);
	    privacyItems.add(item);

	    mPrivacyListManager.createPrivacyList(listName, privacyItems);
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
	Log.d(TAG, "END createPrivacyList.");
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#declineActivePrivacyList()
     */
    @Override
    public void declineActivePrivacyList() throws RemoteException {
	try {
	    mPrivacyListManager.declineActiveList();
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#declineDefaultPrivacyList()
     */
    @Override
    public void declineDefaultPrivacyList() throws RemoteException {
	try {
	    mPrivacyListManager.declineDefaultList();
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#editPrivacyList(java.lang.String, java.util.List)
     */
    @Override
    public void editPrivacyList(String listName, List<PrivacyListItem> items) throws RemoteException {
	Log.d(TAG, "BEGIN editPrivacyList.");
	try {
	    mPrivacyListManager.updatePrivacyList(listName, tranformPrivacyListItemsToPrivacyItems(items));
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
	Log.d(TAG, "END editPrivacyList.");
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#getActivePrivacyList()
     */
    @Override
    public String getActivePrivacyList() throws RemoteException {
	try {
	    PrivacyList activePrivacyList = mPrivacyListManager.getActiveList();
	    return activePrivacyList.toString();
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
	return null;
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#getBlockedGroupsByList(java.lang.String)
     */
    @Override
    public List<String> getBlockedGroupsByList(String listName) throws RemoteException {
	List<String> blockedGroups = new ArrayList<String>();
	try {
	    PrivacyList pL = mPrivacyListManager.getPrivacyList(listName);
	    for (PrivacyItem pI : pL.getItems()) {
		if (pI.getType().equals(PrivacyItem.Type.group) && !pI.isAllow())
		    blockedGroups.add(pI.getValue());
	    }
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
	return blockedGroups;
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#getBlockedUsersByList(java.lang.String)
     */
    @Override
    public List<String> getBlockedUsersByList(String listName) throws RemoteException {
	List<String> blockedUsers = new ArrayList<String>();
	try {
	    PrivacyList pL = mPrivacyListManager.getPrivacyList(listName);
	    for (PrivacyItem pI : pL.getItems()) {
		if (pI.getType().equals(PrivacyItem.Type.jid) && !pI.isAllow())
		    blockedUsers.add(pI.getValue());
	    }
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
	return blockedUsers;
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#getDefaultPrivacyList()
     */
    @Override
    public String getDefaultPrivacyList() throws RemoteException {
	try {
	    PrivacyList defaultPrivacyList = mPrivacyListManager.getDefaultList();
	    return defaultPrivacyList.toString();
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
	return null;
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#removePrivacyList(java.lang.String)
     */
    @Override
    public void removePrivacyList(String listName) throws RemoteException {
	try {
	    mPrivacyListManager.deletePrivacyList(listName);
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#setActivePrivacyList(java.lang.String)
     */
    @Override
    public void setActivePrivacyList(String listName) throws RemoteException {
	try {
	    mPrivacyListManager.setActiveListName(listName);
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#setDefaultPrivacyList(java.lang.String)
     */
    @Override
    public void setDefaultPrivacyList(String listName) throws RemoteException {
	try {
	    mPrivacyListManager.setDefaultListName(listName);
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
    }

    /**
     * From a List of PrivacyListItem get a List of PrivacyItem.
     * @param items The List of PrivacyListItem.
     * @return A list of PrivacyItem.
     */
    private List<PrivacyItem> tranformPrivacyListItemsToPrivacyItems(List<PrivacyListItem> items) {
	List<PrivacyItem> rItems = new ArrayList<PrivacyItem>();
	PrivacyItem.Type[] itemTypes = PrivacyItem.Type.values();

	for (int i = 0; i < items.size(); i++) {
	    rItems.add(new PrivacyItem(itemTypes[items.get(i).getType()].name(), false, i));
	}

	return rItems;
    }

    /**
     * From a List of PrivacyItem get a List of PrivacyListItem.
     * @param items The List of PrivacyItem.
     * @return A list of PrivacyListItem.
     */
    private List<PrivacyListItem> tranformPrivacyItemsToPrivacyListItems(List<PrivacyItem> items) {
	List<PrivacyListItem> rItems = new ArrayList<PrivacyListItem>();

	for (int i = 0; i < items.size(); i++) {
	    rItems.add(new PrivacyListItem(items.get(i).getType().ordinal(), items.get(i).getValue()));
	}
	return rItems;
    }

    /**
     * An adapter for the Smack's PrivacyListListener.
     */
    private class PrivacyListListenerAdapter implements PrivacyListListener {
	/**
	 * Constructor.
	 */
	public PrivacyListListenerAdapter() { }

	@Override
	public void setPrivacyList(final String listName, final List<PrivacyItem> listItem) {
	    int i = mPrivacyListListeners.beginBroadcast();
	    while (i > 0) {
		i--;
		try {
		    mPrivacyListListeners.getBroadcastItem(i).setPrivacyList(listName,
			tranformPrivacyItemsToPrivacyListItems(listItem));
		} catch (RemoteException e) {
		    Log.w(TAG, e.getMessage());
		}
	    }
	    mPrivacyListListeners.finishBroadcast();
	}

	@Override
	public void updatedPrivacyList(final String listName) {
	    Log.d(TAG, "BEGIN updatedPrivacyList.");
	    int i = mPrivacyListListeners.beginBroadcast();
	    while (i > 0) {
		i--;
		try {
		    mPrivacyListListeners.getBroadcastItem(i).updatedPrivacyList(listName);
		} catch (RemoteException e) {
		    Log.w(TAG, e.getMessage());
		}
	    }
	    mPrivacyListListeners.finishBroadcast();
	    Log.d(TAG, "END updatedPrivacyList.");
	}
    }

    @Override
    public void addPrivacyListListener(IPrivacyListListener listener) throws RemoteException {
	if (listener != null)
	    mPrivacyListListeners.register(listener);
    }

    @Override
    public void removePrivacyListListener(IPrivacyListListener listener) throws RemoteException {
	if (listener != null)
	    mPrivacyListListeners.unregister(listener);
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IPrivacyListManager#getPrivacyLists()
     */
    @Override
    public List<String> getPrivacyLists() throws RemoteException {
	Log.d(TAG, "BEGIN getPrivacyLists.");
	List<String> res = new ArrayList<String>();
	try {
	    PrivacyList[] registeredPrivacyLists = mPrivacyListManager.getPrivacyLists();
	    Log.d(TAG, "> registeredPrivacyLists size: " + registeredPrivacyLists.length);
	    if (registeredPrivacyLists.length > 0) {
		for (int i = 0; i < registeredPrivacyLists.length; i++) {
		    res.add(registeredPrivacyLists[i].toString());
		    Log.d(TAG, "> " + res.get(i) + " added.");
		}
	    }
	} catch (XMPPException e) {
	    Log.e(TAG, e.getMessage());
	}
	Log.d(TAG, "END getPrivacyLists.");
	return res;
    }
}
