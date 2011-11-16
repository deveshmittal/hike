package com.bsb.im.service.aidl;

import  com.bsb.im.service.PrivacyListItem;

interface IPrivacyListListener {
	void updatedPrivacyList(in String listName);
	void setPrivacyList(in String listName, in List<PrivacyListItem> listItem);
}
