package com.bsb.im.service.aidl;

import com.bsb.im.service.Message;
import com.bsb.im.service.aidl.IChat;

interface IMessageListener {

	/**
	 * This method is executed when a chat receive a message.
	 * @param chat the chat receiving the message.
	 * @param msg the message received in the chat.
	 */
	void processMessage(in IChat chat, in Message msg);

	/**
	 * This method is executed when a new ChatState is received by the chat.
	 * You can use IChat.getState() in order to get the new state.
	 * @param chat the chat changed.
	 */
	void stateChanged(in IChat chat);
}
