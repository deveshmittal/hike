package com.bsb.im.ui;

import com.bsb.im.BeemApplication;
import com.bsb.im.ui.wizard.Account;
import com.bsb.im.utils.BeemConnectivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.im.R;

/**
 * This class is the main Activity for the Beem project.
 * 
 */
public class Login extends Activity {

	private static final int LOGIN_REQUEST_CODE = 1;
	private TextView mTextView;
	private boolean mIsResult;
	private BeemApplication mBeemApplication;

	/**
	 * Constructor.
	 */
	public Login() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Application app = getApplication();
		if (app instanceof BeemApplication) {
			mBeemApplication = (BeemApplication) app;
			if (mBeemApplication.isConnected()) {
				startActivity(new Intent(this, ContactList.class));
				finish();
			} else if (!mBeemApplication.isAccountConfigured()) {
				startActivity(new Intent(this, Account.class));
				finish();
			}
		}
		setContentView(R.layout.login);
		mTextView = (TextView) findViewById(R.id.log_as_msg);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mBeemApplication.isAccountConfigured() && !mIsResult
				&& BeemConnectivity.isConnected(getApplicationContext())) {
			mTextView.setText("");
			Intent i = new Intent(this, LoginAnim.class);
			startActivityForResult(i, LOGIN_REQUEST_CODE);
			mIsResult = false;
		} else {
			mTextView.setText(R.string.login_start_msg);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == LOGIN_REQUEST_CODE) {
			mIsResult = true;
			if (resultCode == Activity.RESULT_OK) {
				startActivity(new Intent(this, ContactList.class));
				finish();
			} else if (resultCode == Activity.RESULT_CANCELED) {
				if (data != null) {
					String tmp = data.getExtras().getString("message");
					Toast.makeText(Login.this, tmp, Toast.LENGTH_SHORT).show();
					mTextView.setText(tmp);
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.login, menu);
		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(MenuItem item) {
		boolean result;
		switch (item.getItemId()) {
		case R.id.login_menu_settings:
			mTextView.setText("");
			startActivity(new Intent(Login.this, Settings.class));
			result = true;
			break;
		case R.id.login_menu_about:
			createAboutDialog();
			result = true;
			break;
		case R.id.login_menu_login:
			if (mBeemApplication.isAccountConfigured()) {
				Intent i = new Intent(this, LoginAnim.class);
				startActivityForResult(i, LOGIN_REQUEST_CODE);
			}
			result = true;
			break;
		default:
			result = false;
			break;
		}
		return result;
	}

	/**
	 * Create an about "BEEM" dialog.
	 */
	private void createAboutDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String versionname;
		try {
			PackageManager pm = getPackageManager();
			PackageInfo pi = pm.getPackageInfo("com.bsb.im", 0);
			versionname = pi.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			versionname = "";
		}
		String title = getString(R.string.login_about_title, versionname);
		builder.setTitle(title).setMessage(R.string.login_about_msg)
				.setCancelable(false);
		builder.setNeutralButton(R.string.login_about_button,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		AlertDialog aboutDialog = builder.create();
		aboutDialog.show();
	}
}
