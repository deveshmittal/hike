package com.bsb.hike.tasks;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.ui.AccountCreateSuccess;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils;

public class DeleteAccountTask extends AsyncTask<Void, Void, Boolean>
{

	private Context context;
	private ProgressDialog dialog;

	public DeleteAccountTask(Context context)
	{
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(Void... boom)
	{
		HikeUserDatabase db = new HikeUserDatabase(context);
		HikeConversationsDatabase convDb = new HikeConversationsDatabase(context);
		Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();
		try
		{
			AccountUtils.deleteAccount();
			db.deleteAll();
			convDb.deleteAll();
			editor.clear();
			return true;
		}
		catch (Exception e) {
			Log.e("DeleteAccountTask", "error deleting account", e);
			return false;
		}
		finally
		{
			db.close();
			convDb.close();
			editor.commit();
		}
	}

	@Override
	protected void onPreExecute()
	{
		dialog = ProgressDialog.show(context, "Account", "Deleting Account.");
		dialog.show();
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		dialog.dismiss();
		if (result.booleanValue())
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage("Account was deleted.").
				setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id)
					{
						Intent intent = new Intent(context, WelcomeActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(intent);
					}
				});
			builder.show();
		}
		else
		{
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(context, context.getResources().getString(R.string.delete_account_failed), duration);
			toast.show();
		}
	}


}
