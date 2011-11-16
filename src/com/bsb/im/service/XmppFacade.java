package com.bsb.im.service;

import org.jivesoftware.smack.packet.Presence;

import com.bsb.im.utils.PresenceType;

import com.bsb.im.service.aidl.IChatManager;
import com.bsb.im.service.aidl.IPrivacyListManager;
import com.bsb.im.service.aidl.IRoster;
import com.bsb.im.service.aidl.IXmppConnection;
import com.bsb.im.service.aidl.IXmppFacade;

import android.net.Uri;
import android.os.RemoteException;


public class XmppFacade extends IXmppFacade.Stub {

    private final XmppConnectionAdapter mConnexion;

    /**
     * Constructor for XMPPFacade.
     * @param connection the connection use by the facade
     */
    public XmppFacade(final XmppConnectionAdapter connection) {
	this.mConnexion = connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeStatus(int status, String msg) {
	mConnexion.changeStatus(status, msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectAsync() throws RemoteException {
	mConnexion.connectAsync();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectSync() throws RemoteException {
	mConnexion.connectSync();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IXmppConnection createConnection() throws RemoteException {
	return mConnexion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect() throws RemoteException {
	mConnexion.disconnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IChatManager getChatManager() throws RemoteException {
	return mConnexion.getChatManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRoster getRoster() throws RemoteException {
	return mConnexion.getRoster();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPrivacyListManager getPrivacyListManager() {
	return mConnexion.getPrivacyListManager();
    }

    @Override
    public void sendPresencePacket(PresenceAdapter presence) throws RemoteException {
	Presence presence2 = new Presence(PresenceType.getPresenceTypeFrom(presence.getType()));
	presence2.setTo(presence.getTo());
	mConnexion.getAdaptee().sendPacket(presence2);
    }

    /* (non-Javadoc)
     * @see com.bsb.im.service.aidl.IXmppFacade#call(java.lang.String)
     */
    @Override
    public void call(String jid) throws RemoteException {
    }

    @Override
    public boolean publishAvatar(Uri avatarUri) throws RemoteException {
	BeemAvatarManager mgr = mConnexion.getAvatarManager();
	if (mgr == null)
	    return false;

	return mgr.publishAvatar(avatarUri);
    }

    @Override
    public void disableAvatarPublishing() throws RemoteException {
	BeemAvatarManager mgr = mConnexion.getAvatarManager();
	if (mgr != null)
	    mgr.disableAvatarPublishing();
    }

    @Override
    public UserInfo getUserInfo() throws RemoteException {
	return mConnexion.getUserInfo();
    }
}
