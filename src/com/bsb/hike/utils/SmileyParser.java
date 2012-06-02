package com.bsb.hike.utils;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.widget.EditText;

import com.bsb.hike.R;

/**
 * A class for annotating a CharSequence with spans to convert textual emoticons to graphical ones.
 */
public class SmileyParser
{
	// Singleton stuff
	private static SmileyParser sInstance = null;

	public static SmileyParser getInstance()
	{
		return sInstance;
	}

	public static void init(Context context)
	{
		// GH - added a null check so instances will get reused
		if (sInstance == null)
			sInstance = new SmileyParser(context);
	}

	private final Context mContext;

	private final String[] mSmileyTexts;

	private final Pattern mPattern;

	private final HashMap<String, Integer> mSmileyToRes;

	private SmileyParser(Context context)
	{
		mContext = context;
		mSmileyTexts = mContext.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
		mSmileyToRes = buildSmileyToRes();
		mPattern = buildPattern();
	}

	static class Smileys
	{
		private static final int[] sIconIds = { R.drawable.emo_im_happy, R.drawable.emo_im_sad, R.drawable.emo_im_winking, R.drawable.emo_im_3d,
				R.drawable.emo_im_angry, R.drawable.emo_im_kissing, R.drawable.emo_im_yelling, R.drawable.emo_im_cool, R.drawable.emo_im_blush,
				R.drawable.emo_im_zzz, R.drawable.emo_im_double_thumbs_up, R.drawable.emo_im_angel, R.drawable.emo_im_undecided, R.drawable.emo_im_crying,
				R.drawable.emo_im_lips_are_sealed, R.drawable.emo_im_nerd, R.drawable.emo_im_wtf };

		public static int HAPPY = 0;

		public static int SAD = 1;

		public static int WINKING = 2;

		public static int IM_3D = 3;

		public static int ANGRY = 4;

		public static int KISSING = 5;

		public static int YELLING = 6;

		public static int COOL = 7;

		public static int BLUSH = 8;

		public static int ZZZ = 9;

		public static int DOUBLE_THUMBS_UP = 10;

		public static int ANGEL = 11;

		public static int UNDECIDED = 12;

		public static int CRYING = 13;

		public static int LIPS_ARE_SEALED = 14;

		public static int NERD = 15;

		public static int WTF = 16;

		public static int getSmileyResource(int which)
		{
			return sIconIds[which];
		}
	}

	// NOTE: if you change anything about this array, you must make the corresponding change
	// to the string arrays: default_smiley_texts and default_smiley_names in res/values/arrays.xml
	public static final int[] DEFAULT_SMILEY_RES_IDS = { Smileys.getSmileyResource(Smileys.HAPPY), // 0
			Smileys.getSmileyResource(Smileys.SAD), // 1
			Smileys.getSmileyResource(Smileys.WINKING), // 2
			Smileys.getSmileyResource(Smileys.IM_3D), // 3
			Smileys.getSmileyResource(Smileys.ANGRY), // 4
			Smileys.getSmileyResource(Smileys.KISSING), // 5
			Smileys.getSmileyResource(Smileys.YELLING), // 6
			Smileys.getSmileyResource(Smileys.COOL), // 7
			Smileys.getSmileyResource(Smileys.BLUSH), // 8
			Smileys.getSmileyResource(Smileys.ZZZ), // 9
			Smileys.getSmileyResource(Smileys.DOUBLE_THUMBS_UP), // 10
			Smileys.getSmileyResource(Smileys.ANGEL), // 11
			Smileys.getSmileyResource(Smileys.UNDECIDED), // 12
			Smileys.getSmileyResource(Smileys.CRYING), // 13
			Smileys.getSmileyResource(Smileys.LIPS_ARE_SEALED), // 14
			Smileys.getSmileyResource(Smileys.NERD), // 15
			Smileys.getSmileyResource(Smileys.WTF), // 16
	};

	public static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;

	public static final int DEFAULT_SMILEY_NAMES = R.array.default_smiley_names;

	/**
	 * Builds the hashtable we use for mapping the string version of a smiley (e.g. ":-)") to a resource ID for the icon version.
	 */
	private HashMap<String, Integer> buildSmileyToRes()
	{
		if (DEFAULT_SMILEY_RES_IDS.length != mSmileyTexts.length)
		{
			// Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
			// and failed to update arrays.xml
			throw new IllegalStateException("Smiley resource ID/text mismatch");
		}

		HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mSmileyTexts.length);
		for (int i = 0; i < mSmileyTexts.length; i++)
		{
			smileyToRes.put(mSmileyTexts[i], DEFAULT_SMILEY_RES_IDS[i]);
		}

		return smileyToRes;
	}

	/**
	 * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
	 */
	private Pattern buildPattern()
	{
		// Set the StringBuilder capacity with the assumption that the average
		// smiley is 3 characters long.
		StringBuilder patternString = new StringBuilder(mSmileyTexts.length * 3);

		// Build a regex that looks like (:-)|:-(|...), but escaping the smilies
		// properly so they will be interpreted literally by the regex matcher.
		patternString.append('(');
		for (String s : mSmileyTexts)
		{
			patternString.append(Pattern.quote(s));
			patternString.append('|');
		}
		// Replace the extra '|' with a ')'
		patternString.replace(patternString.length() - 1, patternString.length(), ")");

		return Pattern.compile(patternString.toString());
	}

	/**
	 * Adds ImageSpans to a CharSequence that replace textual emoticons such as :-) with a graphical version.
	 * 
	 * @param text
	 *            A CharSequence possibly containing emoticons
	 * @return A CharSequence annotated with ImageSpans covering any recognized emoticons.
	 */
	public CharSequence addSmileySpans(CharSequence text)
	{
		SpannableStringBuilder builder = new SpannableStringBuilder(text);

		Matcher matcher = mPattern.matcher(text);
		while (matcher.find())
		{
			int resId = mSmileyToRes.get(matcher.group());
			builder.setSpan(new ImageSpan(mContext, resId), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		return builder;
	}
	
	/**
	 * Adds an emoticon image to the compose box 
	 * @param composeBox: A reference to the text box in which the emoticon will be shown.
	 * @param whichEmoticon: Integer value of the emoticon which is to be shown.
	 */
	public void addSmiley(EditText composeBox, int whichEmoticon)
	{
		Editable text = composeBox.getText();
		text.append(mSmileyTexts[whichEmoticon]);

		composeBox.setText(addSmileySpans(text));
		composeBox.setSelection(composeBox.length());
	}
}
