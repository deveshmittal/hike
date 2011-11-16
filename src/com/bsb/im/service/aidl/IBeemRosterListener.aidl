package com.bsb.im.service.aidl;

import com.bsb.im.service.PresenceAdapter;

interface IBeemRosterListener {
    void onEntriesAdded(in List<String> addresses);
    void onEntriesUpdated(in List<String> addresses);
    void onEntriesDeleted(in List<String> addresses);
    void onPresenceChanged(in PresenceAdapter presence);
}
