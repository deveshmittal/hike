package com.bsb.hike.db;

import com.bsb.hike.models.ContactInfo.FavoriteType;

public class DBConstants {

	public static final int CONVERSATIONS_DATABASE_VERSION = 9;

	public static final int USERS_DATABASE_VERSION = 7;

	public static final String HAS_CUSTOM_PHOTO = "hascustomphoto";

	public static final String CONVERSATIONS_DATABASE_NAME = "chats";

	public static final String CONVERSATIONS_TABLE = "conversations";

	public static final String MESSAGES_TABLE = "messages";

	public static final String USERS_DATABASE_NAME = "hikeusers";

	public static final String USERS_TABLE = "users";

	public static final String GROUP_MEMBERS_TABLE = "groupMembers";

	public static final String GROUP_INFO_TABLE = "groupInfo";

	/* Table Constants */

	public static final String MESSAGE = "message";

	public static final String MSG_STATUS = "msgStatus";

	public static final String TIMESTAMP = "timestamp";

	public static final String MESSAGE_ID = "msgid";

	public static final String MAPPED_MSG_ID = "mappedMsgId";

	public static final String CONV_ID = "convid";

	public static final String ONHIKE = "onhike";

	public static final String CONTACT_ID = "contactid";

	public static final String MSISDN = "msisdn";

	public static final String MESSAGE_METADATA = "metadata";

	public static final String CONVERSATION_INDEX = "conversation_idx";

	public static final String ID = "id";

	public static final String NAME = "name";

	public static final String PHONE = "phoneNumber";

	public static final String BLOCK_TABLE = "blocked";

	public static final String THUMBNAILS_TABLE = "thumbnails";

	public static final String IMAGE = "image";

	public static final String OVERLAY_DISMISSED = "overlayDismissed";

	public static final String GROUP_ID = "groupId";

	public static final String GROUP_NAME = "groupName";

	public static final String GROUP_INDEX = "group_idx";

	public static final String GROUP_PARTICIPANT = "groupParticipant";

	public static final String GROUP_OWNER = "groupOwner";

	public static final String GROUP_ALIVE = "groupAlive";

	public static final String HAS_LEFT = "hasLeft";

	public static final String LAST_MESSAGED = "lastMessaged";

	public static final String MSISDN_TYPE = "msisdnType";

	public static final String FILE_TABLE = "fileTable";

	public static final String FILE_KEY = "fileKey";

	public static final String FILE_NAME = "fileName";

	public static final String ON_DND = "onDnd";

	public static final String SHOWN_STATUS = "shownStatus";

	public static final String EMOTICON_TABLE = "emoticonTable";

	public static final String EMOTICON_NUM = "emoticonNum";

	public static final String LAST_USED = "lastUsed";

	public static final String EMOTICON_INDEX = "emoticonIdx";

	public static final String MUTE_GROUP = "muteGroup";

	public static final String FAVORITES_TABLE = "favoritesTable";

	public static final String FAVORITE_TYPE = "favoriteType";

	public static final String FAVORITE_TYPE_SELECTION = "COALESCE((SELECT "
			+ FAVORITE_TYPE + " FROM " + FAVORITES_TABLE + " WHERE "
			+ FAVORITES_TABLE + "." + MSISDN + " = " + USERS_TABLE + "."
			+ MSISDN + "), + " + FavoriteType.NOT_FRIEND.ordinal() + ") AS "
			+ FAVORITE_TYPE;

	public static final String STATUS_TABLE = "statusTable";

	public static final String STATUS_ID = "statusId";

	public static final String STATUS_MAPPED_ID = "statusMappedId";

	public static final String STATUS_TEXT = "statusText";

	public static final String STATUS_TYPE = "statusType";

	public static final String STATUS_SEEN = "statusSeen";
}
