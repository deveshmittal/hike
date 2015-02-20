package com.bsb.hike.test;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

public class HttpManagerTest
{

	private static final String TAG = "HttpManagerTest";
	
	static long time = 0;
	
	private static HttpManagerTest _instance;

	private HttpManagerTest()
	{

	}

	public static HttpManagerTest getInstance()
	{
		if (_instance == null)
		{
			_instance = new HttpManagerTest();
		}
		return _instance;
	}

	/*public void okHttpTest()
	{
		new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				try
				{
					OkHttpClient client = new OkHttpClient();
					client.setSslSocketFactory(HikeSSLUtil.getSSLSocketFactory());
					client.setConnectTimeout(HikeConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS);
					client.setReadTimeout(HikeConstants.SOCKET_TIMEOUT, TimeUnit.SECONDS);
					client.setWriteTimeout(HikeConstants.SOCKET_TIMEOUT, TimeUnit.SECONDS);

					Request request = new Request.Builder().url(AccountUtils.base + "/user/lastseen/" + "+919868185209")
							.addHeader("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid).build();
					Call c = client.newCall(request);
					Response r = c.execute();
					ResponseBody body = r.body();
					Logger.d("Okhttp", "string :" + new String(body.bytes()));

					String base = AccountUtils.base.replace("http", "https");
					Request request1 = new Request.Builder().url(base + "/user/lastseen/" + "+919868185209")
							.addHeader("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid).build();
					Call c1 = client.newCall(request1);
					Response r1 = c1.execute();
					ResponseBody body1 = r1.body();
					Logger.d("Okhttp", "string :" + new String(body1.bytes()));

					Request request2 = new Request.Builder().url(base + "/user/lastseen/" + "+919999238132")
							.addHeader("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid).build();
					Call c2 = client.newCall(request2);
					Response r2 = c2.execute();
					ResponseBody body2 = r2.body();
					Logger.d("Okhttp", "string :" + new String(body2.bytes()));
				}
				catch (Exception e)
				{
					Logger.e("Okhttp", "exception : ", e);
				}

			}
		}).start();
	}*/

}
