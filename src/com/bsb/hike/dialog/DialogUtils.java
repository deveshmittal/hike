package com.bsb.hike.dialog;

import java.util.ArrayList;

import android.content.Context;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;

public class DialogUtils
{

	public static String getForwardConfirmationText(Context context, ArrayList<ContactInfo> arrayList, boolean forwarding)
	{
		// multi forward case
		if (forwarding)
		{
			return arrayList.size() == 1 ? context.getResources().getString(R.string.forward_to_singular) : context.getResources().getString(R.string.forward_to_plural,
					arrayList.size());
		}
		StringBuilder sb = new StringBuilder();

		int lastIndex = arrayList.size() - 1;

		boolean moreNamesThanMaxCount = false;
		if (lastIndex < 0)
		{
			lastIndex = 0;
		}
		else if (lastIndex == 1)
		{
			/*
			 * We increment the last index if its one since we can accommodate another name in this case.
			 */
			// lastIndex++;
			moreNamesThanMaxCount = true;
		}
		else if (lastIndex > 0)
		{
			moreNamesThanMaxCount = true;
		}

		for (int i = arrayList.size() - 1; i >= lastIndex; i--)
		{
			sb.append(arrayList.get(i).getFirstName());
			if (i > lastIndex + 1)
			{
				sb.append(", ");
			}
			else if (i == lastIndex + 1)
			{
				if (moreNamesThanMaxCount)
				{
					sb.append(", ");
				}
				else
				{
					sb.append(" and ");
				}
			}
		}
		String readByString = sb.toString();
		if (moreNamesThanMaxCount)
		{
			return context.getResources().getString(R.string.share_with_names_numbers, readByString, lastIndex);
		}
		else
		{
			return context.getResources().getString(R.string.share_with, readByString);
		}
	}

}
