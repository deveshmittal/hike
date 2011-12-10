package com.strumsoft.websocket.phonegap;

public interface WebSocketListener {
	public void onMessage(final String msg);
	public void onOpen();
	public void onClose();
}
