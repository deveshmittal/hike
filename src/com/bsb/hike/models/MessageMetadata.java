package com.bsb.hike.models;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.ui.CreditsActivity;
import com.bsb.hike.utils.Utils;

public class MessageMetadata
{
	private String dndMissedCallNumber;
	private boolean newUser;
	private JSONObject json;
	private ParticipantInfoState participantInfoState = ParticipantInfoState.NO_INFO;

	public MessageMetadata(JSONObject metadata)
	{
		this.newUser = metadata.optString(HikeConstants.NEW_USER).equals("true");
		this.dndMissedCallNumber = metadata.optString(HikeConstants.METADATA_DND);
		this.participantInfoState = ParticipantInfoState.fromJSON(metadata);
		this.json = metadata;
	}

	public String getDNDMissedCallNumber()
	{
		return dndMissedCallNumber;
	}

	public ParticipantInfoState getParticipantInfoState()
	{
		return participantInfoState;
	}

	public String serialize()
	{
		return this.json.toString();
	}

	public boolean getNewUser()
	{
		return newUser;
	}

	public Spannable getMessage(final Context context, final ConvMessage convMessage, boolean shouldSetClickListener)
	{
		String content = "tap here";
		String message = context.getString(
				!TextUtils.isEmpty(dndMissedCallNumber) ? 
						R.string.dnd_message : !newUser ? 
								R.string.friend_joined_hike_no_creds : R.string.friend_joined_hike_with_creds, 
								convMessage.getConversation().getLabel(), 
								dndMissedCallNumber);
		Spannable spannable = Spannable.Factory.getInstance().newSpannable(message);
		int index = message.indexOf(content);
		if (index != -1 && shouldSetClickListener) 
		{
			spannable.setSpan(new ClickableSpan() 
			{
				@Override
				public void onClick(View blah) 
				{
					Intent intent = !TextUtils.isEmpty(dndMissedCallNumber) ? new Intent(Intent.ACTION_VIEW) : new Intent(context, CreditsActivity.class);
					if (!TextUtils.isEmpty(dndMissedCallNumber)) 
					{
						Utils.logEvent(context, HikeConstants.LogEvent.OPT_IN_TAP_HERE);
						intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("sms:" + convMessage.getMsisdn()));
						intent.putExtra("sms_body", context.getString(R.string.dnd_invite_message, dndMissedCallNumber));
					}
					context.startActivity(intent);
				}
			}, index, index + content.length(),
			Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return spannable;
	}
}
