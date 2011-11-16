package com.bsb.im.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.ChatStateListener;

import com.bsb.im.service.aidl.IMessageListener;
import com.bsb.im.service.aidl.IChat;

import android.os.Environment;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;



/**
 * An adapter for smack's Chat class.
 */
public class ChatAdapter extends IChat.Stub {
    private static final int HISTORY_MAX_SIZE = 50;
    private static final String TAG = "ChatAdapter";

    private final Chat mAdaptee;
    private final Contact mParticipant;
    private String mState;
    private boolean mIsOpen;
    private final List<Message> mMessages;
    private final RemoteCallbackList<IMessageListener> mRemoteListeners = new RemoteCallbackList<IMessageListener>();
    private final MsgListener mMsgListener = new MsgListener();
    private boolean mIsHistory;
    private File mHistoryPath;
    private String mAccountUser;

    /**
     * Constructor.
     * @param chat The chat to adapt
     */
    public ChatAdapter(final Chat chat) {
	mAdaptee = chat;
	mParticipant = new Contact(chat.getParticipant());
	mMessages = new LinkedList<Message>();
	mAdaptee.addMessageListener(mMsgListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Contact getParticipant() throws RemoteException {
	return mParticipant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(com.bsb.im.service.Message message) throws RemoteException {
	org.jivesoftware.smack.packet.Message send = new org.jivesoftware.smack.packet.Message();
	send.setTo(message.getTo());
	send.setBody(message.getBody());
	send.setThread(message.getThread());
	send.setSubject(message.getSubject());
	send.setType(org.jivesoftware.smack.packet.Message.Type.chat);
	send.setPacketID(message.getTime());
	
	if(message.getReceived().equals("received"))
	{
		PacketExtension extension = new DefaultPacketExtension("received", "jabber:x:event");
		send.addExtension(extension);
		message.setReceived("");
	}
	// TODO gerer les messages contenant des XMPPError
	// send.set
	try {
	    mAdaptee.sendMessage(send);
	    mMessages.add(message);
	} catch (XMPPException e) {
	    e.printStackTrace();
	}
	String state = Environment.getExternalStorageState();
	if (mIsHistory && Environment.MEDIA_MOUNTED.equals(state))
	    saveHistory(message, mAccountUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMessageListener(IMessageListener listen) {
	if (listen != null)
	    mRemoteListeners.register(listen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeMessageListener(IMessageListener listen) {
	if (listen != null) {
	    mRemoteListeners.unregister(listen);
	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() throws RemoteException {
	return mState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) throws RemoteException {
	mState = state;
    }

    /**
     * Get the adaptee for the Chat.
     * @return The real chat object
     */
    public Chat getAdaptee() {
	return mAdaptee;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOpen(boolean isOpen) {
	this.mIsOpen = isOpen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
	return mIsOpen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessages() throws RemoteException {
	return Collections.unmodifiableList(mMessages);
    }

    /**
     * Add a message in the chat history.
     * @param msg the message to add
     */
    void addMessage(Message msg) {
	if (mMessages.size() == HISTORY_MAX_SIZE)
	    mMessages.remove(0);
	mMessages.add(msg);
	if (!"".equals(msg.getBody()) && msg.getBody() != null) {
	    String state = Environment.getExternalStorageState();
	    if (mIsHistory && Environment.MEDIA_MOUNTED.equals(state))
		saveHistory(msg, msg.getFrom());
	}
    }

    /**
     * Save message in SDCard.
     * @param msg the message receive
     * @param contactName the name of the contact
     */
    public void saveHistory(Message msg, String contactName) {
	File path = getHistoryPath();
    	File filepath;
    	if (contactName.equals(msg.getFrom()))
    	    filepath = new File(path, StringUtils.parseBareAddress(contactName));
    	else
    	    filepath = new File(path, StringUtils.parseBareAddress(msg.getTo()));
    	path.mkdirs();
	try {
	    FileWriter file = new FileWriter(filepath, true);
	    String log = msg.getTimestamp() + " " + contactName + " " + msg.getBody()
		+ System.getProperty("line.separator");
	    file.write(log);
	    file.close();
	} catch (IOException e) {
	    Log.e(TAG, "Error writing chat history", e);
	}
    }

    /**
     * set History enable/disable.
     * @param isHisory history state
     */
    public void setHistory(boolean isHisory) {
	this.mIsHistory = isHisory;
    }

    /**
     * get History state.
     * @return mIsHistory
     */
    public boolean getHistory() {
	return mIsHistory;
    }

    /**
     * Set Account user name.
     * @param accountUser user name
     */
    public void setAccountUser(String accountUser) {
	mAccountUser = accountUser;
    }

    /**
     * get Account user name.
     * @return mAccountUser
     */
    public String getAccountUser() {
	return mAccountUser;
    }

    /**
     * set History path.
     * @param historyPath history path
     */
    public void setHistoryPath(File historyPath) {
	this.mHistoryPath = historyPath;
    }

    /**
     * get History path.
     * @return mHistoryPath;
     */
    public File getHistoryPath() {
	return mHistoryPath;
    }

    /**
     * Listener.
     */
    private class MsgListener implements ChatStateListener {
	/**
	 * Constructor.
	 */
	public MsgListener() {
	}

	@Override
	public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
	    Message msg = new Message(message);
	    //TODO add que les message pas de type errors
	    ChatAdapter.this.addMessage(msg);
	    final int n = mRemoteListeners.beginBroadcast();
	    for (int i = 0; i < n; i++) {
		IMessageListener listener = mRemoteListeners.getBroadcastItem(i);
		try {
		    if (listener != null)
			listener.processMessage(ChatAdapter.this, msg);
		} catch (RemoteException e) {
		    Log.w(TAG, "Error while diffusing message to listener", e);
		}
	    }
	    mRemoteListeners.finishBroadcast();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stateChanged(Chat chat, ChatState state) {
	    mState = state.name();
	    final int n = mRemoteListeners.beginBroadcast();

	    for (int i = 0; i < n; i++) {
		IMessageListener listener = mRemoteListeners.getBroadcastItem(i);
		try {
		    listener.stateChanged(ChatAdapter.this);
		} catch (RemoteException e) {
		    Log.w(TAG, e.getMessage());
		}
	    }
	    mRemoteListeners.finishBroadcast();
	}
    }
}
