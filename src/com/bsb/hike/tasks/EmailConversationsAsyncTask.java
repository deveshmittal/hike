package com.bsb.hike.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

public class EmailConversationsAsyncTask extends AsyncTask<Conversation, Void, Conversation[]>
{

	Activity activity;

	Fragment fragment;

	ProgressDialog dialog;

	List<String> listValues = new ArrayList<String>();

	public EmailConversationsAsyncTask(Activity activity, Fragment fragment)
	{
		this.activity = activity;
		this.fragment = fragment;
	}

	@Override
	protected Conversation[] doInBackground(Conversation... convs)
	{
		ArrayList<Uri> uris = new ArrayList<Uri>();
		String chatLabel = "";
		for (int k = 0; k < convs.length; k++)
		{
			HikeConversationsDatabase db = null;
			String msisdn = convs[k].getMsisdn();
			StringBuilder sBuilder = new StringBuilder();
			Map<String, PairModified<GroupParticipant, String>> participantMap = null;

			db = HikeConversationsDatabase.getInstance();
			Conversation conv = db.getConversation(msisdn, -1);
			boolean isGroup = Utils.isGroupConversation(msisdn);
			chatLabel = conv.getLabel();

			if (isGroup)
			{
				sBuilder.append(R.string.group_name_email);
				GroupConversation gConv = ((GroupConversation) convs[k]);
				participantMap = gConv.getGroupParticipantList();
			}
			// initialize with a label
			sBuilder.append(activity.getResources().getString(R.string.chat_with_prefix) + chatLabel + "\n");

			// iterate through the messages and construct a meaningful
			// payload
			List<ConvMessage> cList = conv.getMessages();
			for (int i = 0; i < cList.size(); i++)
			{
				ConvMessage cMessage = cList.get(i);
				String messageMask = cMessage.getMessage().toString();
				String fromString = null;
				// find if this message was sent or received
				// also find out the sender number, this is needed for the
				// chat
				// file backup
				MessageMetadata cMetadata = cMessage.getMetadata();
				boolean isSent = cMessage.isSent();
				if (cMessage.isGroupChat()) // gc naming logic
				{
					GroupParticipant gPart = null;
					PairModified<GroupParticipant, String> groupParticipantPair = participantMap.get(cMessage.getGroupParticipantMsisdn());
					if(null != groupParticipantPair)
						gPart = groupParticipantPair.getFirst();

					if (gPart != null)
					{
						fromString = (isSent == true) ? activity.getResources().getString(R.string.me_key) : groupParticipantPair.getSecond();
					}
					else
					{
						fromString = (isSent == true) ? activity.getResources().getString(R.string.me_key) : "";
					}
				}
				else
					fromString = (isSent == true) ? activity.getResources().getString(R.string.me_key) : chatLabel; // 1:1
																													// message
																													// logic

				if (cMessage.isFileTransferMessage())
				{
					// TODO: can make this generic and add support for
					// multiple
					// files.
					HikeFile hikeFile = cMetadata.getHikeFiles().get(0);
					HikeFileType fileType = hikeFile.getHikeFileType();
					if ((fileType == (HikeFileType.IMAGE) || fileType == (HikeFileType.AUDIO) || fileType == (HikeFileType.AUDIO_RECORDING) || fileType == (HikeFileType.VIDEO))
							&& !TextUtils.isEmpty(hikeFile.getFilePath()))
					{
						listValues.add(hikeFile.getFilePath());
					}
					// tweak the message here based on the file
					messageMask = activity.getResources().getString(R.string.file_transfer_of_type) + " " + fileType;

				}

				// finally construct the backup string here
				sBuilder.append(Utils.getFormattedDateTimeFromTimestamp(cMessage.getTimestamp(), activity.getResources().getConfiguration().locale) + ":" + fromString + "- "
						+ messageMask + "\n");

				// TODO: add location and contact handling here.
			}
			chatLabel = (Utils.isFilenameValid(chatLabel)) ? chatLabel : "_";
			File chatFile = createChatTextFile(sBuilder.toString(), activity.getResources().getString(R.string.chat_backup_) + "_" + +System.currentTimeMillis() + ".txt");
			if (chatFile != null)
			{
				uris.add(Uri.fromFile(chatFile));
			}
		}
		// append the attachments in hike messages in form of URI's. Dodo
		// android needs uris duh!
		for (String file : listValues)
		{
			File tFile = new File(file);
			Uri u = Uri.fromFile(tFile);
			uris.add(u);
		}

		// TODO: change chatlabel if more than one chats

		// create an email intent to attach the text file and other chat
		// attachments
		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_EMAIL, "");
		intent.putExtra(Intent.EXTRA_SUBJECT, activity.getResources().getString(R.string.backup_of_conversation_with_prefix) + chatLabel);
		intent.putExtra(Intent.EXTRA_TEXT, activity.getResources().getString(R.string.attached_is_the_conversation_backup_string));
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

		// give the hike user a choice of intents
		activity.startActivity(Intent.createChooser(intent, activity.getResources().getString(R.string.email_your_conversation)));

		// TODO: Delete this temp file, although it might be useful for the
		// user to have local chat backups ? Also we need to see

		return null;
	}

	@Override
	protected void onPreExecute()
	{
		dialog = ProgressDialog.show(activity, null, activity.getResources().getString(R.string.exporting_conversations_prefix));

		super.onPreExecute();
	}

	@Override
	protected void onProgressUpdate(Void... values)
	{
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(Conversation[] result)
	{
		if (fragment != null)
		{
			if (fragment.isAdded() && isDialogShowing())
				dialog.dismiss();
		}
		else
		{
			if (!activity.isFinishing() && isDialogShowing())
				dialog.dismiss();
		}
		super.onPostExecute(result);
	}

	private boolean isDialogShowing()
	{
		return dialog != null && dialog.isShowing();
	}

	public File createChatTextFile(String text, String fileName)
	{

		File chatFile = new File(HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT, fileName);

		if (!chatFile.exists())
		{
			try
			{
				chatFile.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}

		try
		{
			BufferedWriter buf = new BufferedWriter(new FileWriter(chatFile, true));
			buf.append(text);
			buf.newLine();
			buf.close();
			return chatFile;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
