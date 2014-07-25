package com.bsb.hike.ui.utils;

import com.bsb.hike.R;

import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.widget.EditText;

public class HashSpanWatcher implements TextWatcher
{
	EditText editText;

	boolean spanAdded;

	String hash;

	Object span;

	int color;

	public HashSpanWatcher(EditText editText, String hash, int color)
	{
		this.editText = editText;
		this.hash = hash;
		this.color = color;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{

	}

	@Override
	public void afterTextChanged(Editable s)
	{
		String text = s.toString();
		if (text.matches("(?i)" + hash + ".*"))
		{
			if (!spanAdded)
			{
				spanAdded = true;
				span = SpanUtil.getImageSpanFromTextView(editText.getContext(), R.layout.hash_text, R.id.text, hash);
				s.setSpan(span, 0, hash.length() , Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		else
		{
			if (spanAdded)
			{
				spanAdded = false;
				s.removeSpan(span);
			}
		}

	}

}
