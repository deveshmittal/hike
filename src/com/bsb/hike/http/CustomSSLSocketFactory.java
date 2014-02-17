package com.bsb.hike.http;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.apache.http.conn.ssl.SSLSocketFactory;

import com.bsb.hike.utils.HikeSSLUtil;

public class CustomSSLSocketFactory extends SSLSocketFactory
{

	private static javax.net.ssl.SSLSocketFactory newFactory = HikeSSLUtil.getSSLSocketFactory();

	public CustomSSLSocketFactory(KeyStore trustStore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException
	{
		super(trustStore);
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException
	{
		return newFactory.createSocket(socket, host, port, autoClose);
	}

	@Override
	public Socket createSocket() throws IOException
	{
		return newFactory.createSocket();
	}
}