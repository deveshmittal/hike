package com.bsb.im.service.aidl;

import  com.bsb.im.service.aidl.IChat;

/**
 * Aidl interface for ChatManager listener.
 * This listener will execute on events like creation of chat session.
 */
interface IChatManagerListener {

    	/**
    	 * Call when a new chat session is created.
    	 * @param chat		the created chat session
    	 * @param locally	true if the session is create by a chat manager.
    	 */
    	void chatCreated(IChat chat, boolean locally);

}
