package com.bsb.hike.ui;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.bsb.hike.R;

public class HikeDialog
{
	public static final int FILE_TRANSFER_DIALOG = 1;

	public static final int FAVORITE_ADDED_DIALOG = 2;

	public static final int STEALTH_FTUE_DIALOG = 3;

	public static final int RESET_STEALTH_DIALOG = 4;

	public static final int STEALTH_FTUE_EMPTY_STATE_DIALOG = 5;

	public static Dialog showDialog(Context context, int whichDialog, Object... data)
	{
		return showDialog(context, whichDialog, null, data);
	}

	public static Dialog showDialog(Context context, int whichDialog, HikeDialogListener listener, Object... data)
	{

		switch (whichDialog)
		{
		case FILE_TRANSFER_DIALOG:
			return showFileTransferPOPUp(context, listener);
		case FAVORITE_ADDED_DIALOG:
			return showAddedAsFavoriteDialog(context, listener, data);
		case STEALTH_FTUE_DIALOG:
			return showStealthFtuePopUp(context, listener, true);
		case RESET_STEALTH_DIALOG:
			return showStealthResetDialog(context, listener, data);
		case STEALTH_FTUE_EMPTY_STATE_DIALOG:
			return showStealthFtuePopUp(context, listener, false);
		}

		return null;

	}

	private static Dialog showFileTransferPOPUp(Context context, final HikeDialogListener listener)
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
				if (listener != null)
				{
					listener.neutralClicked(dialog);
				}
				else
				{
					dialog.dismiss();
				}
			}
		});

		dialog.show();
		return dialog;
	}

	private static Dialog showAddedAsFavoriteDialog(Context context, final HikeDialogListener listener, Object... data)
	{
		String name = "";
		try
		{
			name = (String) data[0];
		}
		catch (ClassCastException ex)
		{
			throw new IllegalArgumentException("Make sure You are sending one string , that is name to fill with in dialog");
		}
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.added_as_favorite_pop_up);
		dialog.setCancelable(true);
		TextView heading = (TextView) dialog.findViewById(R.id.addedYouAsFavHeading);
		heading.setText(context.getString(R.string.addedYouAsFavorite, name));
		TextView des = (TextView) dialog.findViewById(R.id.addedYouAsFavDescription);
		des.setText(Html.fromHtml(context.getString(R.string.addedYouFrindDescription, name, name)));
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
		dialog.show();
		return dialog;
	}

	private static Dialog showStealthFtuePopUp(final Context context, final HikeDialogListener listener, boolean isStealthFtueDialog)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.stealth_ftue_popup);
		dialog.setCancelable(true);
		TextView okBtn = (TextView) dialog.findViewById(R.id.awesomeButton);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		
		if(isStealthFtueDialog)
		{
			body.setText(R.string.stealth_mode_popup_msg);
			okBtn.setText(R.string.quick_setup);
		}
		else
		{
			body.setText(R.string.stealth_mode_empty_conv_popup_msg);
			okBtn.setText(android.R.string.ok);
		}
		
		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (listener != null)
				{
					listener.neutralClicked(dialog);
				}
				else
				{
					dialog.dismiss();
				}
				
				dialog.dismiss();
			}
		});

		dialog.show();
		return dialog;
	}

	private static Dialog showStealthResetDialog(Context context, final HikeDialogListener listener, Object... data)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.stealth_ftue_popup);
		dialog.setCancelable(true);

		String header = (String) data[0];
		String body = (String) data[1];
		String okBtnString = (String) data[2];
		String cancelBtnString = (String) data[3];

		TextView headerText = (TextView) dialog.findViewById(R.id.header);
		TextView bodyText = (TextView) dialog.findViewById(R.id.body);
		TextView cancelBtn = (TextView) dialog.findViewById(R.id.noButton);
		TextView okBtn = (TextView) dialog.findViewById(R.id.awesomeButton);

		dialog.findViewById(R.id.btn_separator).setVisibility(View.VISIBLE);

		cancelBtn.setVisibility(View.VISIBLE);

		headerText.setText(header);
		bodyText.setText(body);
		cancelBtn.setText(cancelBtnString);
		okBtn.setText(okBtnString);

		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				listener.positiveClicked(dialog);
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				listener.negativeClicked(dialog);
			}
		});

		dialog.show();
		return dialog;
	}

	public static interface HikeDialogListener
	{
		public void negativeClicked(Dialog dialog);

		public void positiveClicked(Dialog dialog);

		public void neutralClicked(Dialog dialog);
	}
}
