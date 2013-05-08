package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;

public class Rewards extends DrawerBaseActivity {
	private TextView currentAmount;
	private TextView claimedAmount;

	private ImageButton claimBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rewards);
		afterSetContentView(savedInstanceState);

		TextView mTitleView = (TextView) findViewById(R.id.title_centered);
		mTitleView.setText(R.string.rewards);

		currentAmount = (TextView) findViewById(R.id.current_amount);
		claimedAmount = (TextView) findViewById(R.id.claimed_amount);
		claimBtn = (ImageButton) findViewById(R.id.btn_claim);

		claimBtn.setEnabled(false);

		String textToBeReplaced = "Rs.";
		String textToBeReplaced2 = "FAQs";

		TextView moneyAmt = (TextView) findViewById(R.id.money_amt);
		String moneyAmtTxt = moneyAmt.getText().toString();
		SpannableStringBuilder moneyAmtSsb = new SpannableStringBuilder(
				moneyAmtTxt);
		if (moneyAmtTxt.indexOf(textToBeReplaced) != -1) {
			moneyAmtSsb.setSpan(
					new ImageSpan(this, R.drawable.ic_rupee_black_small),
					moneyAmtTxt.indexOf(textToBeReplaced),
					moneyAmtTxt.indexOf(textToBeReplaced)
							+ textToBeReplaced.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		moneyAmt.setText(moneyAmtSsb);

		TextView disclaimer = (TextView) findViewById(R.id.disclaimer);
		String disclaimerTxt = disclaimer.getText().toString();
		SpannableStringBuilder disclaimerSsb = new SpannableStringBuilder(
				disclaimerTxt);
		if (disclaimerTxt.indexOf(textToBeReplaced) != -1) {
			disclaimerSsb.setSpan(
					new ImageSpan(this, R.drawable.ic_rupee_disclaimer),
					disclaimerTxt.indexOf(textToBeReplaced),
					disclaimerTxt.indexOf(textToBeReplaced)
							+ textToBeReplaced.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		disclaimerSsb.setSpan(
				new ForegroundColorSpan(getResources().getColor(
						R.color.signup_blue)),
				disclaimerTxt.indexOf(textToBeReplaced2),
				disclaimerTxt.indexOf(textToBeReplaced2)
						+ textToBeReplaced2.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		disclaimer.setText(disclaimerSsb);
	}

	public void onInviteClicked(View v) {
		Utils.logEvent(this, HikeConstants.LogEvent.REWARDS_INVITE);
		Utils.startShareIntent(this,
				Utils.getInviteMessage(this, R.string.invite_share_message));
	}

	public void onClaimClicked(View v) {
		Utils.logEvent(this, HikeConstants.LogEvent.REWARDS_CLAIM);
	}

	public void onFaqClicked(View v) {
		Utils.logEvent(this, HikeConstants.LogEvent.REWARDS_FAQ);
		Intent intent = new Intent(this, WebViewActivity.class);
		intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
				HikeConstants.HELP_URL);
		intent.putExtra(HikeConstants.Extras.TITLE, "Help");
		startActivity(intent);
	}
}
