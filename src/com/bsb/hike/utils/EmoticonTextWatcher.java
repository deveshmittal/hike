package com.bsb.hike.utils;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

public class EmoticonTextWatcher implements TextWatcher
{

	private String mod;

	private int startIndex;

	@Override
	public void afterTextChanged(Editable editable)
	{
		if (!TextUtils.isEmpty(mod) && SmileyParser.getInstance().containsEmoticon(mod))
		{
			// For adding smileys as the user is typing.
			SmileyParser.getInstance().addSmileyToEditable(editable, false, startIndex, mod.length());
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		String initial = s.subSequence(0, start).toString();
		int startOffset = Math.min(initial.length(), SmileyParser.MAX_EMOTICON_TEXT_LENGTH);
		mod = s.subSequence(start - startOffset, start + count).toString();
		startIndex = start - startOffset;
	}

}
