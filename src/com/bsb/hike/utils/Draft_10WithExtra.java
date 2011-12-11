package com.bsb.hike.utils;

import net.tootallnate.websocket.HandshakeBuilder;
import net.tootallnate.websocket.drafts.Draft_10;

import org.apache.http.Header;

public class Draft_10WithExtra extends Draft_10 {
	private Header[] headers;

	public Draft_10WithExtra(Header[] headers) { 
		this.headers = headers;
	}

	@Override
	public HandshakeBuilder postProcessHandshakeRequestAsClient(
			HandshakeBuilder request) {
		for(int i = 0; i<headers.length; ++i) {
			request.put(headers[i].getName(), headers[i].getValue());
		}
		super.postProcessHandshakeRequestAsClient(request);
		return request;
	}
}
