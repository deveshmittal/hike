package com.bsb.hike.platform.content;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("NewApi")
public class PlatformWebClient extends WebViewClient
{
static final String tag = "platformwebclient";


	@Override
	public WebResourceResponse shouldInterceptRequest(WebView view, String url)
	{
		Logger.i(tag, "should intercept request "+url);
		if (url.startsWith("http"))
		{
			return  super.shouldInterceptRequest(view, url);
		}

		Uri myuri = Uri.parse(url.replace(PlatformContentConstants.CONTENT_FONTPATH_BASE, PlatformContentConstants.PLATFORM_CONTENT_DIR));

		String fileNameRequested = myuri.getLastPathSegment();
		String[] name = fileNameRequested.split("\\.");
		String prefix = name[0];
		String suffix = name[1];

		InputStream wrtInputStreamm = null;
		WebResourceResponse response = null;

		if (prefix.contains("Roboto") || prefix.contains("roboto"))
		{
			AssetManager assManager = HikeMessengerApp.getInstance().getApplicationContext().getAssets();
			try
			{
				wrtInputStreamm = assManager.open(PlatformContentConstants.ASSETS_FONTS_DIR + fileNameRequested);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			wrtInputStreamm = new FileInputStream(PlatformContentUtils.openFile(myuri, "").getFileDescriptor());
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			int statusCode = 200;
			String reasonPhase = "OK";
			Map<String, String> responseHeaders = new HashMap<String, String>();
			responseHeaders.put("Access-Control-Allow-Origin", "*");
			// TODO Handle case for API level 10
			response = new WebResourceResponse(PlatformContentConstants.ASSETS_FONTS_DIR + suffix, "UTF-8", statusCode, reasonPhase, responseHeaders, wrtInputStreamm);
		}
		else
		{
			// TODO Handle case for API level 10
			response = new WebResourceResponse(PlatformContentConstants.ASSETS_FONTS_DIR + suffix, "UTF-8", wrtInputStreamm);
		}

		return response;
	}
}
