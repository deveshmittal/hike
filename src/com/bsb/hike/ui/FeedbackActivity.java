package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;

public class FeedbackActivity extends Activity implements FinishableEvent
{

	private EditText mFeedbackText;
	private Button mFeedbackButton;
	private HikeHTTPTask mTask;
	private ProgressDialog mDialog;
	private TextView mTitleView;
	private View selectedEmoticon;
	private TextView mNameText;
	private View mButtonBar;

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return mTask;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback);

		mFeedbackText = (EditText) findViewById(R.id.feedback);
		mFeedbackButton = (Button) findViewById(R.id.title_icon);
		mButtonBar = (View) findViewById(R.id.button_bar_2);
		mTitleView = (TextView) findViewById(R.id.title);
		mNameText = (TextView) findViewById(R.id.user_name);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String name = settings.getString(HikeMessengerApp.NAME, "Set a name!");

		mNameText.setText(name);
		mFeedbackButton.setVisibility(View.VISIBLE);
		mFeedbackButton.setText(R.string.send);
		mTitleView.setText(getResources().getString(R.string.feedback));
		mButtonBar.setVisibility(View.VISIBLE);

		mFeedbackButton.setEnabled(false);

		//This hack is to prevent the cursor from being shown initially on the text box in touch screen devices. On touching the text box the cursor becomes visible 
		if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS 
				|| getResources().getConfiguration().hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
			mFeedbackText.setCursorVisible(false);
			mFeedbackText.setOnTouchListener(new OnTouchListener() 
			{
				@Override
				public boolean onTouch(View v, MotionEvent event) 
				{
					if(event.getAction() == MotionEvent.ACTION_DOWN)
					{
						mFeedbackText.setCursorVisible(true);
					}
					return false;
				}
			});
		}

		Object o = getLastNonConfigurationInstance();
		if (o instanceof HikeHTTPTask)
		{
			mTask = (HikeHTTPTask) o;
			/* we're currently executing a task, so show the progress dialog */
			mTask.setActivity(this);
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.sending_feedback));	
		}
		
		mFeedbackText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				if(s.length() != 0 || selectedEmoticon != null)
				{
					mFeedbackButton.setEnabled(true);
				}
				else
				{
					mFeedbackButton.setEnabled(false);
				}
			}
		});
	}

	public void onTitleIconClick(View view)
	{
		if (view == mFeedbackButton)
		{
			String feedback = mFeedbackText.getEditableText().toString();
			mTask = new HikeHTTPTask(this, R.string.sending_feedback_failed);
			HikeHttpRequest request = new HikeHttpRequest("/feedback", new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{

				}

				public void onSuccess()
				{
					/* show a toast */
					Toast toast = Toast.makeText(FeedbackActivity.this,
									R.string.feedback_success,
									Toast.LENGTH_SHORT);
					toast.show();
					finish();
				}
			});
			JSONObject obj = new JSONObject();
			try
			{
				obj.put("data", feedback);
				request.setJSONData(obj);
			}
			catch (JSONException e)
			{
				Log.wtf("FeedbackActivity", "Jesus, I hate this library");
			}

			mTask.execute(request);
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.sending_feedback));
		}
	}

	public void onEmoticonClicked(View view)
	{	
		mFeedbackButton.setEnabled(true);
		if(selectedEmoticon != null)
		{
			selectedEmoticon.setSelected(false);
		}
		view.setSelected(true);
		selectedEmoticon = view;
	}

	public void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
		}
	}

	@Override
	public void onFinish(boolean success)
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}

		mTask = null;
	}

}
