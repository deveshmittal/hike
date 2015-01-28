package com.bsb.hike.platform.content;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PlatformWebClient extends WebViewClient
{

	@Override
	public WebResourceResponse shouldInterceptRequest(WebView view, String url)
	{
		Log.d("PlatformWebClient", "" + "URL: " + url);
		if (url.startsWith("http") || (!url.startsWith(PlatformContentConstants.CONTENT_FONTPATH_BASE)))
		{
			return super.shouldInterceptRequest(view, url);
		}

		Uri myuri = Uri.parse(url.replace(PlatformContentConstants.CONTENT_FONTPATH_BASE, PlatformContentConstants.PLATFORM_CONTENT_DIR));

		String fileNameRequested = myuri.getLastPathSegment();
		String[] name = fileNameRequested.split("\\.");
		String prefix = name[0];
		String suffix = name[1];

		InputStream wrtInputStreamm = new FileInputStream(PlatformContentUtils.openFile(myuri, "").getFileDescriptor());
		
		WebResourceResponse response = null;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			int statusCode = 200;
			String reasonPhase = "OK";
			Map<String, String> responseHeaders = new HashMap<String, String>();
			responseHeaders.put("Access-Control-Allow-Origin", "*");
			response = new WebResourceResponse(PlatformContentConstants.ASSETS_FONTS_DIR + suffix, "UTF-8", statusCode, reasonPhase, responseHeaders, wrtInputStreamm);
		}
		else
		{
			response = new WebResourceResponse(PlatformContentConstants.ASSETS_FONTS_DIR + suffix, "UTF-8", wrtInputStreamm);
		}

		return response;
	}
}
