package com.bsb.hike.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.View.OnClickListener;

import com.bsb.hike.R;

public class HikeDialog
{
	public static final int FILE_TRANSFER_DIALOG = 1;

	public static void showDialog(Activity context, int whichDialog)
	{

		switch (whichDialog)
		{
		case FILE_TRANSFER_DIALOG:
			showFileTransferPOPUp(context);
			break;
		}
	}

	private static void showFileTransferPOPUp(Activity context)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.file_transfer_tutorial_pop_up);
		dialog.setCancelable(true);

		View okBtn = dialog.findViewById(R.id.awesomeButton);
		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();
			}
		});

		dialog.show();
	}
}
