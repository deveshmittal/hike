package com.bsb.im.service.aidl;

import  com.bsb.im.service.PrivacyListItem;
import  com.bsb.im.service.aidl.IPrivacyListListener;

interface IPrivacyListManager {
	void createPrivacyList(in String listName, in List<PrivacyListItem> items);
	void removePrivacyList(in String listName);
	void editPrivacyList(in String listName, in List<PrivacyListItem> items);
	String getActivePrivacyList();
	String getDefaultPrivacyList();
	void setActivePrivacyList(in String listName);
	void setDefaultPrivacyList(in String listName);
	void declineActivePrivacyList();
	void declineDefaultPrivacyList();
	List<String> getPrivacyLists();
	void blockUser(in String listName, in String jid);
	List<String> getBlockedUsersByList(in String listName);
	List<String> getBlockedGroupsByList(in String listName);
	void addPrivacyListListener(in IPrivacyListListener listener);
	void removePrivacyListListener(in IPrivacyListListener listener);
}

