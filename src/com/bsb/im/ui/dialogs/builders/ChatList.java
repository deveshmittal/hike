package com.bsb.im.ui.dialogs.builders;

import java.util.List;

import com.bsb.im.service.Contact;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.bsb.im.R;


/**
 * Create the change chat dialog.
 */
public class ChatList extends AlertDialog.Builder {

    //private static final String TAG = "Dialogs.Builders > Chat list";

    /**
     * Constructor.
     * @param context context activity.
     * @param openedChats A list containing the JID of participants of the opened chats.
     */
    public ChatList(final Context context, final List<Contact> openedChats) {
	super(context);

	if (openedChats.size() > 0) {
	    CharSequence[] items = new CharSequence[openedChats.size()];

	    int i = 0;
	    for (Contact c : openedChats) {
		items[i++] = c.getName();
	    }
	    setTitle(R.string.chat_dialog_change_chat_title);
	    setItems(items, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int item) {
		    Intent chatIntent = new Intent(context, com.bsb.im.ui.Chat.class);
		    chatIntent.setData((openedChats.get(item)).toUri());
		    context.startActivity(chatIntent);
		}
	    });
	} else {
	    setMessage(R.string.chat_no_more_chats);
	}
    }
}
