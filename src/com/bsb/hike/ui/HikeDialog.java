package com.bsb.hike.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.test.suitebuilder.annotation.MediumTest;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontButton;
import com.bsb.hike.view.CustomFontTextView;

public class HikeDialog
{
	public static final int FAVORITE_ADDED_DIALOG = 2;

	public static final int STEALTH_FTUE_DIALOG = 3;

	public static final int RESET_STEALTH_DIALOG = 4;

	public static final int STEALTH_FTUE_EMPTY_STATE_DIALOG = 5;
	
	public static final int SHARE_IMAGE_QUALITY_DIALOG = 6;

	public static final int SMS_CLIENT_DIALOG = 7;

	public static Dialog showDialog(Context context, int whichDialog, Object... data)
	{
		return showDialog(context, whichDialog, null, data);
	}

	public static Dialog showDialog(Context context, int whichDialog, HikeDialogListener listener, Object... data)
	{

		switch (whichDialog)
		{
		case FAVORITE_ADDED_DIALOG:
			return showAddedAsFavoriteDialog(context, listener, data);
		case STEALTH_FTUE_DIALOG:
			return showStealthFtuePopUp(context, listener, true);
		case RESET_STEALTH_DIALOG:
			return showStealthResetDialog(context, listener, data);
		case STEALTH_FTUE_EMPTY_STATE_DIALOG:
			return showStealthFtuePopUp(context, listener, false);
		case SHARE_IMAGE_QUALITY_DIALOG:
			return showImageQualityDialog(context, listener, data);
		case SMS_CLIENT_DIALOG:
			return showSMSClientDialog(context, listener, data);
		}

		return null;

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
	
	private static Dialog showImageQualityDialog(final Context context, final HikeDialogListener listener, Object... data)
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.image_quality_popup);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		final Editor editor = appPrefs.edit();
		//int quality = appPrefs.getInt(HikeConstants.IMAGE_QUALITY, 2);
		final LinearLayout small_ll = (LinearLayout) dialog.findViewById(R.id.hike_small_container);
		final LinearLayout medium_ll = (LinearLayout) dialog.findViewById(R.id.hike_medium_container);
		final LinearLayout original_ll = (LinearLayout) dialog.findViewById(R.id.hike_original_container);
		final CheckBox small = (CheckBox) dialog.findViewById(R.id.hike_small_checkbox);
		final CheckBox medium = (CheckBox) dialog.findViewById(R.id.hike_medium_checkbox);
		final CheckBox original = (CheckBox) dialog.findViewById(R.id.hike_original_checkbox);
		//CustomFontButton always = (CustomFontButton) dialog.findViewById(R.id.btn_always);
		//CustomFontButton justOnce = (CustomFontButton) dialog.findViewById(R.id.btn_just_once);
		CustomFontTextView header = (CustomFontTextView) dialog.findViewById(R.id.image_quality_popup_header);
		CustomFontTextView smallSize = (CustomFontTextView) dialog.findViewById(R.id.image_quality_small_cftv);
		CustomFontTextView mediumSize = (CustomFontTextView) dialog.findViewById(R.id.image_quality_medium_cftv);
		CustomFontTextView originalSize = (CustomFontTextView) dialog.findViewById(R.id.image_quality_original_cftv);
		small.setChecked(true);
		
		if(data!=null)
			{
			Long[] dataBundle = (Long[])data;
			int smallsz,mediumsz,originalsz;
			if(dataBundle.length>0)
				{
				if(dataBundle[0]!=1)
					header.setText(context.getString(R.string.image_quality_send) + " " + dataBundle[0] + " " + context.getString(R.string.image_quality_files_as));
				else
					header.setText(context.getString(R.string.image_quality_send) + " " + dataBundle[0] + " " + context.getString(R.string.image_quality_file_as));				
				
				originalsz = dataBundle[1].intValue();
				smallsz = (int) (dataBundle[0] * HikeConstants.IMAGE_SIZE_SMALL);
				mediumsz = (int) (dataBundle[0] * HikeConstants.IMAGE_SIZE_MEDIUM);
				if (smallsz >= originalsz)
				{
					smallsz = originalsz;
					smallSize.setVisibility(View.GONE);
				}
				if(mediumsz >= originalsz)
				{
					mediumsz = originalsz;
					mediumSize.setVisibility(View.GONE);
					// if medium option text size is gone, so is small's
					smallSize.setVisibility(View.GONE);
				}
				smallSize.setText(" (" + Utils.getSizeForDisplay(smallsz)+ ")");
				mediumSize.setText(" (" + Utils.getSizeForDisplay(mediumsz) + ")");
				originalSize.setText(" (" + Utils.getSizeForDisplay(originalsz) + ")");
			}
		}
		
		/*switch (quality)
		{
		case 1:
			small.setChecked(false);
			medium.setChecked(false);
			original.setChecked(true);
			break;
		case 2:
			small.setChecked(false);
			medium.setChecked(true);
			original.setChecked(false);
			break;
		case 3:
			small.setChecked(true);
			medium.setChecked(false);
			original.setChecked(false);
			break;
		}
		*/
		OnClickListener onClickListener = new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				// TODO Auto-generated method stub
				switch (v.getId())
				{
				case R.id.hike_small_container:
					small.setChecked(true);
					medium.setChecked(false);
					original.setChecked(false);
					saveImageQualitySettings(editor,3);
					callOnSucess(listener, dialog);
					
					break;
				case R.id.hike_medium_container:
					small.setChecked(false);
					medium.setChecked(true);
					original.setChecked(false);
					saveImageQualitySettings(editor,2);
					callOnSucess(listener, dialog);
					
					break;
				case R.id.hike_original_container:
					small.setChecked(false);
					medium.setChecked(false);
					original.setChecked(true);
					saveImageQualitySettings(editor,1);
					callOnSucess(listener, dialog);
					
					break;
					
				/*case R.id.btn_always:
					if (medium.isChecked())
					{
						editor.putInt(HikeConstants.IMAGE_QUALITY, 2);
					}
					else if (original.isChecked())
					{
						editor.putInt(HikeConstants.IMAGE_QUALITY, 1);
					}
					else
					{
						editor.putInt(HikeConstants.IMAGE_QUALITY, 3);
					}
					editor.commit();
					
					HikeSharedPreferenceUtil.getInstance(context).saveData(HikeConstants.REMEMBER_IMAGE_CHOICE, true);

					if (listener != null)
					{
						listener.onSucess(dialog);
					}
					else
					{
						dialog.dismiss();
					}
					
					break;
					
				case R.id.btn_just_once:
					if (medium.isChecked())
					{
						editor.putInt(HikeConstants.IMAGE_QUALITY, 2);
					}
					else if (original.isChecked())
					{
						editor.putInt(HikeConstants.IMAGE_QUALITY, 1);
					}
					else
					{
						editor.putInt(HikeConstants.IMAGE_QUALITY, 3);
					}
					editor.commit();
					
					HikeSharedPreferenceUtil.getInstance(context).saveData(HikeConstants.REMEMBER_IMAGE_CHOICE, false);

					if (listener != null)
					{
						listener.onSucess(dialog);
					}
					else
					{
						dialog.dismiss();
					}
					
					break;*/
				}
			}
		};

		small_ll.setOnClickListener(onClickListener);
		medium_ll.setOnClickListener(onClickListener);
		original_ll.setOnClickListener(onClickListener);
		//always.setOnClickListener(onClickListener);
		//justOnce.setOnClickListener(onClickListener);

		dialog.show();
		return dialog;
	}
		
	private static void saveImageQualitySettings(Editor editor, int i)
	{
		// TODO Auto-generated method stub
		editor.putInt(HikeConstants.IMAGE_QUALITY, i);
		editor.commit();
	}

	private static void callOnSucess(HikeDialogListener listener, Dialog dialog)
	{
		// TODO Auto-generated method stub
		if (listener != null)
		{
			listener.onSucess(dialog);
		}
		else
		{
			dialog.dismiss();
		}
	}

	private static Dialog showSMSClientDialog(Context context, final HikeDialogListener listener, Object... data)
	{
		return showSMSClientDialog(context, listener, (Boolean) data[0], (CompoundButton) data[1], (Boolean) data[2]);
	}
	
	private static Dialog showSMSClientDialog(final Context context, final HikeDialogListener listener, final boolean triggeredFromToggle, 
			final CompoundButton checkBox, final boolean showingNativeInfoDialog )
	{
		final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.enable_sms_client_popup);
		dialog.setCancelable(showingNativeInfoDialog);

		TextView header = (TextView) dialog.findViewById(R.id.header);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		Button btnOk = (Button) dialog.findViewById(R.id.btn_ok);
		Button btnCancel = (Button) dialog.findViewById(R.id.btn_cancel);

		header.setText(showingNativeInfoDialog ? R.string.native_header : R.string.use_hike_for_sms);
		body.setText(showingNativeInfoDialog ? R.string.native_info : R.string.use_hike_for_sms_info);

		if (showingNativeInfoDialog)
		{
			btnCancel.setVisibility(View.GONE);
			btnOk.setText(R.string.continue_txt);
		}
		else
		{
			btnCancel.setText(R.string.cancel);
			btnOk.setText(R.string.allow);
		}

		btnOk.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				listener.positiveClicked(dialog);
			}
		});

		dialog.setOnCancelListener(new OnCancelListener()
		{

			@Override
			public void onCancel(DialogInterface dialog)
			{
				if (showingNativeInfoDialog && checkBox != null)
				{
					checkBox.setChecked(false);
				}
			}
		});

		btnCancel.setOnClickListener(new OnClickListener()
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
		
		public void onSucess(Dialog dialog);
	}
}
