package com.bsb.hike.modules.httpmgr.request.facade;

import java.util.List;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;


/**
 * This object is passed in preProcess methods so that users can add some request parameters
 * @author anubhavgupta
 *
 */
public class RequestFacade implements IRequestFacade
{

	private Request<?> request;
	
	public RequestFacade(Request<?> request)
	{
		this.request = request;
	}
	@Override
	public void addHeaders(List<Header> headers)
	{
		request.addHeaders(headers);
	}

	@Override
	public void setBody(IRequestBody body)
	{
		request.setBody(body);
	}

}
