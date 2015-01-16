package com.bsb.hike.platform.content;

import java.util.HashMap;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;
import com.samskivert.mustache.Mustache.Compiler;

class PlatformTemplateEngine
{

	private static String TAG = "PlatformTemplateEngine";

	/** Initialized compiler. */
	private static Compiler compiler = Mustache.compiler();

	/**
	 * Compile template.
	 * 
	 * @param templateContent
	 *            the template content
	 * @return the template
	 */
	public static Template compileTemplate(String templateContent)
	{
		Log.d(TAG, "compile template");

		try
		{
			return compiler.compile(templateContent);
		}
		catch (MustacheException mex)
		{
			mex.printStackTrace();
			return null;
		}
	}

	/**
	 * Bind content data with pre-compiled template
	 * 
	 * @param template
	 *            the template
	 * @param contentRequest
	 *            the content request
	 * @return true, if successful
	 */
	public static boolean execute(Template template, PlatformContentRequest contentRequest)
	{
		Log.d(TAG, "binding data");
		HashMap<String, Object> contentDataMap = new Gson().fromJson(contentRequest.getContentData().getContentJSON(), new TypeToken<HashMap<String, Object>>()
		{
		}.getType());

		if (contentDataMap == null)
		{
			// TODO Handle
		}

		String formedData = null;

		try
		{
			formedData = template.execute(contentDataMap);
		}
		catch (MustacheException mex)
		{
			mex.printStackTrace();
		}

		if (formedData != null)
		{
			contentRequest.getContentData().setFormedData(formedData);
		}
		else
		{
			return false;
		}

		return true;
	}

}
