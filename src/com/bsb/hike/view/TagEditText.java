package com.bsb.hike.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

import com.bsb.hike.R;
import com.bsb.hike.ui.utils.SpanUtil;

public class TagEditText extends EditText
{
	private static final String TOKEN = "!`!";

	public static final String SEPARATOR_COMMA = ",";

	public static final String SEPARATOR_SPACE = " ";

	private Map<String, Object> addedTags;

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
	}

	/**
	 * Append new tag
	 * 
	 * @param text
	 */
	public void appendTag(String text, String uniqueness, Object data)
	{
		Editable editable = getText();
		String textS = editable.toString();
		String afterSep = getCharAfterSeparator(textS);
		if (afterSep != null)
		{
			int lastIndex = textS.lastIndexOf(afterSep);
			editable.replace(lastIndex, textS.length(), "");
		}
		int length = editable.length();
		String customuniqueness = generateUniqueness(uniqueness);
		addedTags.put(customuniqueness, data);
		editable.append(customuniqueness + this.separator);
		// if(afterSep!=null){
		// editable.append(afterSep);
		// }
		editable.setSpan(SpanUtil.getImageSpanFromTextView(getContext(), R.layout.tag, R.id.tagTV, text), length, length + customuniqueness.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		if (listener != null)
		{
			listener.tagAdded(data, uniqueness);
		}
	}

	public void removeTag(String text, String uniqueness, Object data)
	{
		Editable editable = getText();
		String curentText = editable.toString();
		uniqueness = generateUniqueness(uniqueness);
		int index = curentText.lastIndexOf(uniqueness);
		Log.i("tagedit", uniqueness + " index of removable tag " + index);
		if (index != -1)
		{
			editable.replace(index, index + uniqueness.length(), "");
			if (length() > index)
			{
				if (separator.equals(Character.toString(editable.charAt(index))))
				{
					editable.replace(index, index + separator.length(), "");
				}
			}
		}
	}

	public void toggleTag(String text, String uniqueness, Object data)
	{
		Log.i("tagedit", "before toggle #" + getText().toString() + "#");
		String newUniqueness = generateUniqueness(uniqueness);
		if (addedTags.containsKey(newUniqueness))
		{
			removeTag(text, uniqueness, data);
		}
		else
		{
			appendTag(text, uniqueness, data);
		}
		Log.i("tagedit", "after toggle #" + getText().toString() + "#");
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
		// caller has to set needcallback to false and change edit text value if no call back required
		// this has to be performed every time callback not required , for example clear edit text
		if (needCallback)
		{
			String textS = text.toString();
			Log.i("tagedit", "length before : " + lengthBefore + " and lengthAfter " + lengthAfter);
			int charChanged = Math.abs((lengthAfter - lengthBefore));
			if (charChanged > minCharacterChangeThresholdForTag)
			{
				performTagsProcessing(textS);
			}
			if (listener != null)
			{
				String afterSep = getCharAfterSeparator(textS);
				if (afterSep != null && afterSep.length() >= minCharacterChangeThreshold)
				{
					if (!afterSep.equals(lastAfterSepCallback))
						listener.characterAddedAfterSeparator(lastAfterSepCallback = afterSep);
				}
				else
				{
					listener.charResetAfterSeperator();
				}
			}
		}
		else
		{
			needCallback = true;
		}
	}

	private void performTagsProcessing(String text)
	{
		String[] possibleUniqueness = text.split(TOKEN);
		HashSet<String> temp = new HashSet<String>();

		for (String uniqueNess : possibleUniqueness)
		{
			if (!separator.equals(uniqueNess.trim()))
			{
				temp.add(generateUniqueness(uniqueNess));
			}
		}
		Iterator<String> iterator = addedTags.keySet().iterator();
		while (iterator.hasNext())
		{
			String uniqueNess = iterator.next();

			if (!temp.contains((uniqueNess)))
			{

				// give callback
				Log.e("tagedit", "key " + uniqueNess + " removed");

				if (listener != null)
				{
					listener.tagRemoved(addedTags.get(uniqueNess), generateOrigUniqueNess(uniqueNess));
				}
				iterator.remove();
			}
		}
	}

	/*
	 * This tries all possibilities to give you text after separator, Note: This text is not exact logical but from users perspective
	 */
	private String getCharAfterSeparator(String textS)
	{
		textS = textS.trim();

		int lastTokenIndex = textS.lastIndexOf(TOKEN);
		if (lastTokenIndex != -1)
		{
			return textS.substring(lastTokenIndex + TOKEN.length()).trim();
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
