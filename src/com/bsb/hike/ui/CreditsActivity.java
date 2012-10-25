package com.bsb.hike.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.auth.AccessToken;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.TwitterOAuthView;
import com.bsb.hike.view.TwitterOAuthView.Result;
import com.bsb.hike.view.TwitterOAuthView.TwitterAuthListener;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.fiksu.asotracking.FiksuTrackingManager;

public class CreditsActivity extends DrawerBaseActivity implements Listener, TwitterAuthListener
{
	static final String CALLBACK_URL = "http://get.hike.in/";

	static final String IEXTRA_OAUTH_VERIFIER = "oauth_verifier";

	private TextView mTitleView;
	private ViewGroup creditsContainer;
	private SharedPreferences settings;
	private TextView freeSms50;
	private TextView freeSms100;
	private TextView creditsMax;
	private TextView creditsCurrent;
	private ProgressBar creditsBar;
	private ViewGroup facebookBtn;
	private ViewGroup twitterBtn;
	private TextView facebookTxt;
	private TextView twitterTxt;

	/*Width of the container in which the credit view will slide*/
	private int creditProgressBarWidth;
	/*Width of the view that shows the current credit number*/
	private int creditNumWidth;

	private HikeHTTPTask hikeHTTPTask;

	private ProgressDialog dialog;

	private TwitterOAuthView twitterOAuthView;

	/*Offset for the number of credits/(100 total credits) will come in the curved part of the progress bar*/
	private final static int OFFSET_PORTRAIT = 3;
	private final static int OFFSET_LANDSCAPE = 1;

	private int currentOffset;

	private int creditNumContainerWidth;

	private AlertDialog alertDialog;

	private DeleteSocialCredentialsTask deleteSocialCredentialsTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		currentOffset = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? OFFSET_PORTRAIT : OFFSET_LANDSCAPE;

		if(savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.TWITTER_VIEW_VISIBLE))
		{
			onTwitterClick(null);
			return;
		}

		initalizeViews(savedInstanceState);

		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.INVITEE_NUM_CHANGED, this);
	}

	private void initalizeViews(Bundle savedInstanceState)
	{
		twitterOAuthView = null;
		setContentView(R.layout.credits);
		afterSetContentView(savedInstanceState);

		Object o = getLastNonConfigurationInstance();
		if(o instanceof HikeHTTPTask)
		{
			dialog = ProgressDialog.show(this, null, getString(R.string.saving_social));
		}
		else if(o instanceof DeleteSocialCredentialsTask)
		{
			dialog = ProgressDialog.show(this, null, getString(R.string.social_unlinking));
			((DeleteSocialCredentialsTask)o).setDialog(dialog);
		}

		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		mTitleView = (TextView) findViewById(R.id.title_centered);
		creditsContainer = (ViewGroup) findViewById(R.id.credits_container);
		freeSms50 = (TextView) findViewById(R.id.free_sms);
		freeSms100 = (TextView) findViewById(R.id.free_sms_100);
		creditsMax = (TextView) findViewById(R.id.credits_full_txt);
		creditsCurrent = (TextView) findViewById(R.id.credits_num);
		creditsBar = (ProgressBar) findViewById(R.id.credits_progress);
		facebookBtn = (ViewGroup) findViewById(R.id.btn_fb);
		twitterBtn = (ViewGroup) findViewById(R.id.btn_twitter);
		facebookTxt = (TextView) findViewById(R.id.facebook_txt);
		twitterTxt = (TextView) findViewById(R.id.twitter_txt);

		creditNumWidth = getResources().getDimensionPixelSize(R.dimen.credits_num_view_width);

		int totalProgressBarMargin = getResources().getDimensionPixelSize(R.dimen.credits_main_margin) 
							+ getResources().getDimensionPixelSize(R.dimen.credits_progress_margin)
							+ getResources().getDimensionPixelSize(R.dimen.credits_progress_layout_padding)
							+ getResources().getDimensionPixelSize(R.dimen.credits_progress_curve_width);
		creditProgressBarWidth = getResources().getDisplayMetrics().widthPixels - (2*totalProgressBarMargin);

		int totalCreditNumContainerMargin = getResources().getDimensionPixelSize(R.dimen.credits_main_margin) 
							+ getResources().getDimensionPixelSize(R.dimen.credits_num_container_margin)
							+ getResources().getDimensionPixelSize(R.dimen.credits_progress_layout_padding);
		creditNumContainerWidth = getResources().getDisplayMetrics().widthPixels - (2*totalCreditNumContainerMargin);

		String freeSmsString = getString(R.string.invite_friend_free_sms);
		String textToColor = "50 free SMS";
		int startIndex = freeSmsString.indexOf(textToColor);

		SpannableStringBuilder ssb = new SpannableStringBuilder(freeSmsString);
		ssb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.unread_message_blue)), startIndex, startIndex + textToColor.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		freeSms50.setText(ssb);

		freeSmsString = getString(R.string.connect_and_get);
		textToColor = "100 free SMS";
		startIndex = freeSmsString.indexOf(textToColor);

		ssb = new SpannableStringBuilder(freeSmsString);
		ssb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.unread_message_blue)), startIndex, startIndex + textToColor.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		freeSms100.setText(ssb);

		mTitleView.setText(R.string.free_sms_txt);

		updateCredits();
		setupSocialButtons();
	}

	private void setupSocialButtons()
	{
		facebookTxt.setText(settings.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false) ? R.string.connected : R.string.facebook);
		facebookTxt.setCompoundDrawablesWithIntrinsicBounds(
				R.drawable.ic_fb,
				0,
				settings.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false) ? R.drawable.ic_white_tick : 0,
				0);
		facebookBtn.setBackgroundResource(settings.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false) ? R.drawable.bg_fb_btn_pressed : R.drawable.fb_btn);

		twitterTxt.setText(settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false) ? R.string.connected : R.string.twitter);
		twitterTxt.setCompoundDrawablesWithIntrinsicBounds(
				R.drawable.ic_twitter,
				0,
				settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false) ? R.drawable.ic_white_tick : 0,
				0);
		twitterBtn.setBackgroundResource(settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false) ? R.drawable.bg_twitter_btn_pressed : R.drawable.twitter_btn);
	}

	public void onInviteClick(View v)
	{
		Utils.logEvent(CreditsActivity.this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
		startActivity(new Intent(CreditsActivity.this, HikeListActivity.class));
	}

	public void onFacebookClick(View v)
	{
		final Facebook facebook = HikeMessengerApp.getFacebook();

		if(!settings.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false))
		{
			facebook.authorize(CreditsActivity.this, new String[] {"publish_stream"}, new DialogListener() {
	            @Override
	            public void onComplete(Bundle values) 
	            {
	            	final Editor editor = CreditsActivity.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
	                editor.putString(HikeMessengerApp.FACEBOOK_TOKEN, facebook.getAccessToken());
	                editor.putLong(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES, facebook.getAccessExpires());

	                String userId = null;
	                try 
	                {
						JSONObject me = new JSONObject(facebook.request("me"));
						userId = me.optString("id");
						editor.putString(HikeMessengerApp.FACEBOOK_USER_ID, userId);
						editor.commit();
						sendCredentialsToServer(userId, facebook.getAccessToken(), true);
						return;
					} 
	                catch (MalformedURLException e1) 
	                {
						Log.e(getClass().getSimpleName(), "Malformed URL", e1);
					} 
	                catch (JSONException e2) 
	                {
	                	Log.e(getClass().getSimpleName(), "Invalid JSON", e2);
					} 
	                catch (IOException e3) 
	                {
	                	Log.e(getClass().getSimpleName(), "IOException", e3);
					}
	                Toast.makeText(CreditsActivity.this, R.string.social_failed, Toast.LENGTH_SHORT).show();
	            }

	            @Override
	            public void onFacebookError(FacebookError error) 
	            {
	            	Toast.makeText(CreditsActivity.this, R.string.social_failed, Toast.LENGTH_SHORT).show();
	            }

	            @Override
	            public void onError(DialogError e) 
	            {
	            	Toast.makeText(CreditsActivity.this, R.string.social_failed, Toast.LENGTH_SHORT).show();
	            }

	            @Override
	            public void onCancel() {}
	        });
	    }
		else
		{
			showCredentialUnlinkAlert(true);
		}
	}

	public void onTwitterClick(View v)
	{
		if(!settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false))
		{
			twitterOAuthView = new TwitterOAuthView(this);
			twitterOAuthView.start(HikeConstants.APP_TWITTER_ID, HikeConstants.APP_TWITTER_SECRET, CALLBACK_URL, true, this);

			/*
			 * Workaround for an android bug where the keyboard does not popup in the web view.
			 * http://code.google.com/p/android/issues/detail?id=7189 
			 */
			twitterOAuthView.requestFocus(View.FOCUS_DOWN);
			twitterOAuthView.setOnTouchListener(new View.OnTouchListener()
			{
			    @Override
			    public boolean onTouch(View v, MotionEvent event)
			    {
			        switch (event.getAction())
			        {
			            case MotionEvent.ACTION_DOWN:
			            case MotionEvent.ACTION_UP:
			                if (!v.hasFocus())
			                {
			                    v.requestFocus();
			                }
			                break;
			        }
			        return false;
			    }
			});

			setContentView(twitterOAuthView);
		}
		else
		{
			showCredentialUnlinkAlert(false);
		}
	}

	

	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
		outState.putBoolean(HikeConstants.Extras.TWITTER_VIEW_VISIBLE, twitterOAuthView != null);
		super.onSaveInstanceState(outState);
	}

	private void showCredentialUnlinkAlert(final boolean facebook)
	{
		Builder builder = new Builder(CreditsActivity.this);
		builder.setMessage(facebook ? R.string.confirm_unlink_fb : R.string.confirm_unlink_twitter);
		builder.setPositiveButton(R.string.unlink, new OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				deleteSocialCredentialsTask = new DeleteSocialCredentialsTask();
				deleteSocialCredentialsTask.execute(facebook);
			}
		});
		builder.setNegativeButton(R.string.cancel, new OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{}
		});
		alertDialog = builder.create();
		alertDialog.show();
	}

	private void sendCredentialsToServer(String id, String token, final boolean facebook)
	{
		JSONObject request = new JSONObject();
        try 
        {
        	request.put("id", id);
			request.put("token", token);
		} 
        catch (JSONException e) 
        {
        	Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
        HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(facebook ? "/account/connect/fb" : "/account/connect/twitter", new HikeHttpRequest.HikeHttpCallback() {
        	public void onSuccess(JSONObject response)
        	{
        		if(dialog != null)
        		{
        			dialog.dismiss();
        			dialog = null;
        		}
        		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
    			// Fail the whole process if the request to our server fails.
    			if(facebook)
    			{
    				FiksuTrackingManager.uploadPurchaseEvent(CreditsActivity.this, HikeConstants.FACEBOOK, HikeConstants.FACEBOOK_CONNECT, HikeConstants.CURRENCY);
    				editor.putBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, true);
    			}
    			else
    			{
    				FiksuTrackingManager.uploadPurchaseEvent(CreditsActivity.this, HikeConstants.TWITTER, HikeConstants.TWITTER_CONNECT, HikeConstants.CURRENCY);
    				editor.putBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, true);
    			}
    			editor.commit();
    			setupSocialButtons();
    			hikeHTTPTask = null;
        	}

    		public void onFailure() 
    		{
    			if(dialog != null)
        		{
        			dialog.dismiss();
        			dialog = null;
        		}
    			Toast.makeText(CreditsActivity.this, R.string.social_failed, Toast.LENGTH_SHORT).show();
    			Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
    			// Fail the whole process if the request to our server fails.
    			if(facebook)
    			{
    				HikeMessengerApp.getFacebook().setAccessExpires(0);
    				HikeMessengerApp.getFacebook().setAccessToken("");

    				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN);
    				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES);
    				editor.remove(HikeMessengerApp.FACEBOOK_USER_ID);
    				editor.remove(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE);
    			}
    			else
    			{
    				editor.remove(HikeMessengerApp.TWITTER_TOKEN);
    				editor.remove(HikeMessengerApp.TWITTER_TOKEN_SECRET);
    				editor.remove(HikeMessengerApp.TWITTER_AUTH_COMPLETE);
    			}
    			editor.commit();
    			hikeHTTPTask = null;
    		}
        });
        hikeHttpRequest.setJSONData(request);
        hikeHTTPTask = new HikeHTTPTask(null, 0);
        hikeHTTPTask.execute(hikeHttpRequest);

        dialog = ProgressDialog.show(this, null, getString(R.string.saving_social));
	}

	@Override
	public Object onRetainNonConfigurationInstance() 
	{
		if(hikeHTTPTask != null)
		{
			return hikeHTTPTask;
		}
		if(deleteSocialCredentialsTask != null)
		{
			return deleteSocialCredentialsTask;
		}
		return super.onRetainNonConfigurationInstance();
	}

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
        super.onActivityResult(requestCode, resultCode, data);
        HikeMessengerApp.getFacebook().authorizeCallback(requestCode, resultCode, data);
    }

	@Override
	public void onBackPressed()
	{
		if(twitterOAuthView != null)
		{
			initalizeViews(null);
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() 
	{
		if(dialog != null)
		{
			dialog.dismiss();
			dialog = null;
		}
		hikeHTTPTask = null;
		deleteSocialCredentialsTask = null;
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.INVITEE_NUM_CHANGED, this);
		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		// If we are currently showing a webview, we don't need to make any changes in these UI.
		if(twitterOAuthView != null)
		{
			return;
		}
		super.onEventReceived(type, object);
		if(HikePubSub.SMS_CREDIT_CHANGED.equals(type) || HikePubSub.INVITEE_NUM_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					updateCredits();
				}
			});
		}
	}

	private void updateCredits()
	{
		int currentCredits = settings.getInt(HikeMessengerApp.SMS_SETTING, 0);
		int totalCredits = Integer.parseInt(settings.getString(HikeMessengerApp.TOTAL_CREDITS_PER_MONTH, "100"));

		int actualOffset = (int)((currentOffset * totalCredits)/100);

		creditsMax.setText(totalCredits + "+");
		creditsCurrent.setText(currentCredits + "");

		creditsBar.setMax(totalCredits);
		creditsBar.setProgress(currentCredits);

		int paddingLeft;
		if(currentCredits <= actualOffset)
		{
			paddingLeft = 0;
		}
		else if(currentCredits >= totalCredits - actualOffset)
		{
			paddingLeft = creditNumContainerWidth - creditNumWidth;
		}
		else
		{
			int creditsForContainer = currentCredits - actualOffset;
			paddingLeft = (int)((creditsForContainer * creditProgressBarWidth)/totalCredits);
		}
		creditsContainer.setPadding(paddingLeft, 0, 0, 0);
	}

	@Override
	public void onSuccess(TwitterOAuthView view, AccessToken accessToken) 
	{
		initalizeViews(null);
		Log.d(getClass().getSimpleName(), "TOKEN:  " + accessToken.getToken() + " SECRET: " + accessToken.getTokenSecret());
		sendCredentialsToServer(accessToken.getToken(), accessToken.getTokenSecret(), false);
	}

	@Override
	public void onFailure(TwitterOAuthView view, Result result) {
		initalizeViews(null);
	}


	private class DeleteSocialCredentialsTask extends AsyncTask<Boolean, Void, Boolean>
	{
		ProgressDialog taskDialog;

		public void setDialog(ProgressDialog progressDialog)
		{
			taskDialog = progressDialog;
		}

		@Override
		protected void onPreExecute() 
		{
			taskDialog = dialog = ProgressDialog.show(CreditsActivity.this, null, getString(R.string.social_unlinking));
		}

		@Override
		protected Boolean doInBackground(Boolean... params) 
		{
			boolean facebook = params[0];
			Editor editor = settings.edit();
			try 
			{
				AccountUtils.deleteSocialCredentials(facebook);
				if(facebook)
    			{
					AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(HikeMessengerApp.getFacebook());
					mAsyncRunner.logout(getApplicationContext(), new RequestListener() 
					{
						@Override
						public void onMalformedURLException(MalformedURLException arg0, Object arg1) {}
						@Override
						public void onIOException(IOException arg0, Object arg1) {}
						@Override
						public void onFileNotFoundException(FileNotFoundException arg0, Object arg1) {}
						@Override
						public void onFacebookError(FacebookError arg0, Object arg1) {}
						@Override
						public void onComplete(String arg0, Object arg1) {}
					});

    				HikeMessengerApp.getFacebook().setAccessExpires(0);
    				HikeMessengerApp.getFacebook().setAccessToken("");

    				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN);
    				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES);
    				editor.remove(HikeMessengerApp.FACEBOOK_USER_ID);
    				editor.remove(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE);
    			}
    			else
    			{
    				editor.remove(HikeMessengerApp.TWITTER_TOKEN);
    				editor.remove(HikeMessengerApp.TWITTER_TOKEN_SECRET);
    				editor.remove(HikeMessengerApp.TWITTER_AUTH_COMPLETE);
    			}
			} 
			catch (Exception e) 
			{
				Log.e(getClass().getSimpleName(), "Exception while deleting credentials", e);
				return false;
			}
			finally
			{
				editor.commit();
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) 
		{
			if(taskDialog != null)
			{
				taskDialog.dismiss();
				taskDialog = null;
			}
			Toast.makeText(CreditsActivity.this, result ? R.string.social_unlink_success : R.string.unlink_account_failed, Toast.LENGTH_SHORT).show();
			setupSocialButtons();
			deleteSocialCredentialsTask = null;
		}
	}
}