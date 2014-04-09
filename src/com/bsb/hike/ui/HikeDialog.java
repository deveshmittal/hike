package com.bsb.hike.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.View.OnClickListener;

import com.bsb.hike.R;
import com.bsb.hike.R.id;

public class HikeDialog
{
	public static final int FILE_TRANSFER_DIALOG = 1;

	public static final int FAVORITE_ADDED_DIALOG = 2;

	public static Dialog showDialog(Activity context, int whichDialog, Object... data)
	{
		return showDialog(context, whichDialog, null, data);
	}

	public static Dialog showDialog(Activity context, int whichDialog, HikeDialogListener listener, Object... data)
	{

		switch (whichDialog)
		{
		case FILE_TRANSFER_DIALOG:
			return showFileTransferPOPUp(context);
		case FAVORITE_ADDED_DIALOG:
			return showAddedAsFavoriteDialog(context, listener, data);
		}

		return null;

	}

	private static Dialog showFileTransferPOPUp(Activity context)
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
		return dialog;
	}

	private static Dialog showAddedAsFavoriteDialog(Activity context, final HikeDialogListener listener, Object... data)
	{
		try
		{
			String name = (String) data[0];
		}
		catch (ClassCastException ex)
		{
			throw new IllegalArgumentException("Make sure You are sending one string , that is name to fill with in dialog");
		}
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.added_as_favorite_pop_up);
		dialog.setCancelable(true);
		View no = dialog.findViewById(R.id.noButton);
		View yes = dialog.findViewById(R.id.yesButton);
		OnClickListener clickListener = new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				switch (arg0.getId())
				{
				case R.id.noButton:
					if (listener != null)
					{
						listener.negativeClicked(dialog);
					}
					else
					{
						dialog.dismiss();
					}
					break;
				case R.id.yesButton:
					if (listener != null)
					{
						listener.positiveClicked(dialog);
					}
					else
					{
						dialog.dismiss();
					}
					break;
				}

			}
		};
		no.setOnClickListener(clickListener);
		yes.setOnClickListener(clickListener);
		return dialog;
	}

	public static interface HikeDialogListener
	{
		public void negativeClicked(Dialog dialog);

		public void positiveClicked(Dialog dialog);

		public void neutralClicked(Dialog dialog);
	}
}
