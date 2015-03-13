package com.bsb.hike.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

import com.bsb.hike.R;
import com.bsb.hike.ui.utils.SpanUtil;
import com.bsb.hike.utils.Logger;

public class TagEditText extends EditText
{
	private static final String TOKEN = "!`!";

	private static final String SPAN_REPLACEMENT = "|";

	public static final String SEPARATOR_COMMA = ",";

	public static final String SEPARATOR_SPACE = " ";

	private Map<String, Object> addedTags;

	private Map<String, ImageSpan> addedSpans;

	private Map<ImageSpan, String> spanToUniqueness;

	private int minCharacterChangeThreshold;

	// as we add token on both sides
	private int minCharacterChangeThresholdForTag = 2;

	private String separator = SEPARATOR_SPACE;

	private String lastAfterSepCallback;

	private TagEditorListener listener;

	private boolean needCallback = true;

	public TagEditText(Context context)
	{
		super(context);
		init();
	}

	public TagEditText(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public TagEditText(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init();
	}

	private void init()
	{
		addedTags = new HashMap<String, Object>();
		addedSpans = new HashMap<String, ImageSpan>();
		spanToUniqueness = new LinkedHashMap<ImageSpan, String>();
	}

	/**
	 * Append new tag
	 * 
	 * @param text
	 */
	private void appendTag(String text, String uniqueness, Object data)
	{

		String customuniqueness = generateUniqueness(uniqueness);
		addedTags.put(customuniqueness, data); 
		
		   ImageSpan span = SpanUtil.getImageSpanFromTextView(getContext(), R.layout.tag, R.id.tagTV, text);
		if (span != null)
		{
			addedSpans.put(customuniqueness, span);
			spanToUniqueness.put(span, uniqueness);
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			Set<ImageSpan> allSpans = spanToUniqueness.keySet();
			for (ImageSpan ispan : allSpans)
			{
				ssb.append(SPAN_REPLACEMENT + " ");
				int length = ssb.length();
				// -1 for space
				ssb.setSpan(ispan, length - SPAN_REPLACEMENT.length() - 1, length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			needCallback = false;
			setText(ssb);
			setSelection(ssb.length());
			if (listener != null)
			{

				listener.tagAdded(data, uniqueness);
				listener.charResetAfterSeperator();
			}
		}
	}

	public void removeTag(String text, String uniqueness, Object data)
	{
		Editable editable = getText();
		SpannableStringBuilder ssb = new SpannableStringBuilder(editable);

		String genUniqueNess = generateUniqueness(uniqueness);
		ImageSpan span = addedSpans.get(genUniqueNess);

		if (span != null && ssb.getSpanStart(span) != -1)
		{
			// client only invoked , so no callback on text changed
			needCallback = false;
			editable.delete(ssb.getSpanStart(span), ssb.getSpanEnd(span));
			setSelection(editable.length());
			tagRemoved(uniqueness, span);

		}

	}

	private void tagRemoved(String uniqueness, ImageSpan span)
	{
		String genUniqueNess = generateUniqueness(uniqueness);
		if (listener != null)
		{
			listener.tagRemoved(addedTags.get(genUniqueNess), uniqueness);
		}
		addedTags.remove(genUniqueNess);
		addedSpans.remove(genUniqueNess);
		spanToUniqueness.remove(span);
	}

	public void toggleTag(String text, String uniqueness, Object data)
	{
		Logger.i("tagedit", "before toggle #" + getText().toString() + "#");
		String newUniqueness = generateUniqueness(uniqueness);
		if (addedTags.containsKey(newUniqueness))
		{
			removeTag(text, uniqueness, data);
		}
		else
		{
			appendTag(text, uniqueness, data);
		}
		Logger.i("tagedit", "after toggle #" + getText().toString() + "#");
	}
	
	public void addTag(String text, String uniqueness, Object data)
	{
		Logger.i("tagedit", "before toggle #" + getText().toString() + "#");
		String newUniqueness = generateUniqueness(uniqueness);
		if (!addedTags.containsKey(newUniqueness))
		{
			appendTag(text, uniqueness, data);
		}
		Logger.i("tagedit", "after adding #" + getText().toString() + "#");
	}

	/**
	 * clears all text of edit text and add new tags
	 * 
	 * @param tags
	 */
	public void replaceTags(String... tags)
	{

	}

	/**
	 * append new tags , one tag per String
	 * 
	 * @param tags
	 */
	public void appendTags(String... tags)
	{
	}

	public int getTagsCount()
	{
		return 0;
	}

	public ArrayList<String> getTags()
	{
		return null;
	}

	private String generateUniqueness(String input)
	{
		return TOKEN + input + TOKEN;

	}

	private String generateOrigUniqueNess(String input)
	{
		return input.replaceAll(TOKEN, "");
	}

	public void setSeparator(String separator)
	{
		if (separator != null)
			this.separator = separator;
	}

	/**
	 * Set minimum number of characters to change , so that it gives notification to listener with text after separator
	 * 
	 * @param threshold
	 */
	public void setMinCharChangeThreshold(int threshold)
	{
		this.minCharacterChangeThreshold = threshold;
	}

	/**
	 * Set minimum number of characters to change , so that it will perform tag search processing and will give notification to listener for addition or removal of tags
	 * 
	 * @param threshold
	 */
	public void setMinCharChangeThresholdForTag(int threshold)
	{
		this.minCharacterChangeThresholdForTag = threshold;
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter)
	{
		super.onTextChanged(text, start, lengthBefore, lengthAfter);
		Log.i("tagedit", "text changed , before " + lengthBefore + " and after " + lengthAfter);
		// caller has to set needcallback to false and change edit text value if no call back required
		// this has to be performed every time callback not required , for example clear edit text
		if (needCallback)
		{
			performTagsProcessing();

			String afterSep = getCharAfterSeparator();
			if (afterSep != null)
			{
				Log.i("tagedit", "afterSep is " + afterSep);
				if (afterSep.length() >= minCharacterChangeThreshold)
				{

					if (!afterSep.equals(lastAfterSepCallback))
					{
						listener.characterAddedAfterSeparator(lastAfterSepCallback = afterSep);
					}

				}
				else
				{
					giveResetCallback();
				}
			}
			else
			{
				giveResetCallback();
			}
		}
		else
		{
			needCallback = true;

		}
	}

	private void giveResetCallback()
	{
		if (null != lastAfterSepCallback)
		{
			listener.charResetAfterSeperator();
			lastAfterSepCallback = null;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		Log.e("tagedit", "onkey up , " + event.getKeyCode());
		return super.onKeyUp(keyCode, event);
	}

	private void performTagsProcessing()
	{
		Editable editText = getText();
		SpannableStringBuilder ssb = new SpannableStringBuilder(editText);
		Set<ImageSpan> spans = new HashSet<ImageSpan>(spanToUniqueness.keySet());
		for (ImageSpan span : spans)
		{
			if (ssb.getSpanStart(span) == -1)
			{
				// tag removed
				tagRemoved(spanToUniqueness.get(span), span);
			}
		}
	}

	/*
	 * This tries all possibilities to give you text after separator, Note: This text is not exact logical but from users perspective
	 */
	private String getCharAfterSeparator()
	{
		String textS = getText().toString().trim();

		int lastTokenIndex = textS.lastIndexOf(SPAN_REPLACEMENT);
		if (lastTokenIndex != -1)
		{
			return textS.substring(lastTokenIndex + SPAN_REPLACEMENT.length()).trim();
		}

		else
		{
			// no TOKEN present , try for separator
			// int separatorIndex = textS.lastIndexOf(separator);
			// if (separatorIndex != -1)
			// {
			// return textS.substring(separatorIndex + separator.length()).trim();
			// }
			// else
			// {
			// separator not present , try for all text
			if (textS.length() > 0)
			{
				return textS;
			}
			// }
		}
		return null;
	}

	public void setListener(TagEditorListener listener)
	{
		this.listener = listener;

	}

	public void clear(boolean callbackRequired)
	{
		addedTags.clear();
		spanToUniqueness.clear();
		addedSpans.clear();
		setText("", callbackRequired);
	}

	public void setText(String text, boolean callbackRequired)
	{
		this.needCallback = callbackRequired;
		setText(text);

	}

	public static interface TagEditorListener
	{
		public void tagRemoved(Object data, String uniqueNess);

		public void tagAdded(Object textData, String uniqueNess);

		public void characterAddedAfterSeparator(String characters);

		public void charResetAfterSeperator();
	}
}
