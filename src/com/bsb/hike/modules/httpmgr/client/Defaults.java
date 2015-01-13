package com.bsb.hike.modules.httpmgr.client;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.bsb.hike.utils.HikeSSLUtil;

final class Defaults
{
	static final int CONNECT_TIMEOUT_MILLIS = 30 * 1000; // 30s

	static final int READ_TIMEOUT_MILLIS = 60 * 1000; // 60s
	
	static final int WRITE_TIMEOUT_MILLIS = 60 * 1000; // 60s
	
	static final SocketFactory SOCKET_FACTORY = SocketFactory.getDefault();
	
	static final SSLSocketFactory SSL_SOCKET_FACTORY = HikeSSLUtil.getSSLSocketFactory();

	private Defaults()
	{
		// No instances.
	}
}
