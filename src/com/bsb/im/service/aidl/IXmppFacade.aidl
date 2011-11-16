package com.bsb.im.service.aidl;

import com.bsb.im.service.aidl.IXmppConnection;
import com.bsb.im.service.aidl.IRoster;
import com.bsb.im.service.aidl.IChatManager;
import com.bsb.im.service.aidl.IPrivacyListManager;
import com.bsb.im.service.PresenceAdapter;
import com.bsb.im.service.UserInfo;

import android.net.Uri;

interface IXmppFacade {

    /**
     * Get the XmppConnection of the facade.
     */
    IXmppConnection createConnection();

    /**
     * Get the roster of the user
     */
    IRoster getRoster();

    /**
     * Connect and login synchronously on the server.
     */
    void connectSync();

    /**
     * Connect and login asynchronously on the server.
     */
    void connectAsync();

    /**
     * Disconnect from the server
     */
    void disconnect();

    /**
     * Get the chat manager.
     */
    IChatManager getChatManager();

    /**
     * Change the status of the user.
     * @param status the status to set
     * @param msg the message state to set
     */
    void changeStatus(in int status, in String msg);

    void sendPresencePacket(in PresenceAdapter presence);

    /**
     * make a jingle audio call
     * @param jid the receiver id
     */
     void call(in String jid);

    boolean publishAvatar(in Uri avatarUri);

    void disableAvatarPublishing();

    UserInfo getUserInfo();

     IPrivacyListManager getPrivacyListManager();
}
