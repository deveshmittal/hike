package com.bsb.im.service.aidl;

import  com.bsb.im.service.Contact;
import  com.bsb.im.service.Message;
import  com.bsb.im.service.aidl.IMessageListener;

/**
 * An aidl interface for Chat session.
 */
interface IChat {

    	/**
    	 * Send a message.
    	 * @param message	the message to send
    	 */
	void sendMessage(in Message message);

	/**
	 * Get the participant of the chat
	 * @return the participant
	 */
	Contact getParticipant();

	/**
	 * Add a message listener.
	 * @param listener the listener to add.
	 */
	void addMessageListener(in IMessageListener listener);

	/**
	 * Remove a message listener.
	 * @param listener the listener to remove.
	 */
	void removeMessageListener(in IMessageListener listener);

	String getState();

	void setOpen(in boolean isOpen);

	boolean isOpen();

	void setState(in String state);

	List<Message> getMessages();

}
