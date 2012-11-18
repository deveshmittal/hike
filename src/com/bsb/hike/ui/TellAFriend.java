package com.bsb.hike.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;

public class TellAFriend extends DrawerBaseActivity implements OnClickListener {

	private enum ItemTypes {
		FACEBOOK, TWITTER, SMS, EMAIL, OTHER
	}

	private ViewGroup itemContainer;
	private int itemHeight = (int) (48 * Utils.densityMultiplier);

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tell_a_friend);

		itemContainer = (ViewGroup) findViewById(R.id.items_container);

		afterSetContentView(savedInstanceState);

		TextView mTitleView = (TextView) findViewById(R.id.title_centered);
		mTitleView.setText(R.string.invite);

		int[] textResIds = { R.string.facebook, R.string.twitter, R.string.sms,
				R.string.email, R.string.share_via_other };
		int[] textImgResIds = { R.drawable.ic_invite_fb,
				R.drawable.ic_invite_twitter, R.drawable.ic_invite_sms,
				R.drawable.ic_invite_email, R.drawable.ic_invite_other };

		int[] subtextResIds = { R.string.fb_subtext, R.string.twitter_subtext,
				R.string.sms_subtext, R.string.email_subtext,
				R.string.other_subtext };

		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		for (int i = 0; i < textResIds.length; i++) {
			View itemView = layoutInflater.inflate(R.layout.tell_a_friend_item,
					null);
			itemView.setFocusable(true);
			itemView.setClickable(true);

			TextView itemTxt = (TextView) itemView.findViewById(R.id.item_txt);
			TextView itemSubTxt = (TextView) itemView
					.findViewById(R.id.item_subtxt);

			itemTxt.setText(textResIds[i]);
			itemTxt.setCompoundDrawablesWithIntrinsicBounds(textImgResIds[i],
					0, 0, 0);

			itemSubTxt.setText(subtextResIds[i]);

			if (i == 0) {
				itemView.findViewById(R.id.divider).setVisibility(View.VISIBLE);
				itemView.setBackgroundResource(R.drawable.profile_top_item_selector);
			} else if (i == textResIds.length - 1) {
				itemView.findViewById(R.id.divider).setVisibility(View.GONE);
				itemView.setBackgroundResource(R.drawable.profile_bottom_item_selector);
			} else {
				itemView.findViewById(R.id.divider).setVisibility(View.VISIBLE);
				itemView.setBackgroundResource(R.drawable.profile_center_item_selector);
			}
			int id = ItemTypes.values()[i].ordinal();

			itemView.setId(id);

			LayoutParams layoutParams = new LayoutParams(
					LayoutParams.MATCH_PARENT, itemHeight);
			itemView.setLayoutParams(layoutParams);

			itemView.setOnClickListener(this);
			itemContainer.addView(itemView);
		}
	}

	@Override
	public void onClick(View v) {
		switch (ItemTypes.values()[v.getId()]) {
		case FACEBOOK:

			break;

		case TWITTER:

			break;

		case SMS:
			Utils.logEvent(this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
			startActivity(new Intent(this, HikeListActivity.class));
			break;

		case EMAIL:
			Intent mailIntent = new Intent(Intent.ACTION_SENDTO);

			mailIntent.setData(Uri.parse("mailto:"));
			mailIntent.putExtra(Intent.EXTRA_SUBJECT,
					getString(R.string.email_subject));
			mailIntent.putExtra(Intent.EXTRA_TEXT,
					Utils.getInviteMessage(this, R.string.email_body));

			startActivity(mailIntent);
			break;

		case OTHER:
			Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_INVITE);
			Utils.startShareIntent(this,
					Utils.getInviteMessage(this, R.string.invite_share_message));
			break;
		}
	}
}
