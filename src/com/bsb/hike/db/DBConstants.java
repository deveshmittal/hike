package com.bsb.hike.db;

import com.bsb.hike.models.ContactInfo.FavoriteType;

public interface DBConstants
{

	public static final int CONVERSATIONS_DATABASE_VERSION = 35;

	public static final int USERS_DATABASE_VERSION = 15;

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
	
	public static final String MESSAGE_HASH = "msgHash";

	public static final String CONV_ID = "convid";
	
	public static final String MESSAGE_TYPE = "type";

	public static final String CONVERSATION_METADATA = "convMetadata";

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
	
	public static final String IS_MUTE = "isMute";

	public static final String FAVORITES_TABLE = "favoritesTable";

	public static final String FAVORITE_TYPE = "favoriteType";

	public static final String FAVORITE_TYPE_SELECTION = "COALESCE((SELECT " + FAVORITE_TYPE + " FROM " + FAVORITES_TABLE + " WHERE " + FAVORITES_TABLE + "." + MSISDN + " = "
			+ USERS_TABLE + "." + MSISDN + "), + " + FavoriteType.NOT_FRIEND.ordinal() + ") AS " + FAVORITE_TYPE;

	public static final String STATUS_TABLE = "statusTable";

	public static final String STATUS_ID = "statusId";

	public static final String STATUS_MAPPED_ID = "statusMappedId";

	public static final String STATUS_TEXT = "statusText";

	public static final String STATUS_TYPE = "statusType";

	public static final String HIKE_JOIN_TIME = "hikeJoinTime";

	public static final String SHOW_IN_TIMELINE = "showInTimeline";

	public static final String MOOD_ID = "moodId";

	public static final String TIME_OF_DAY = "timeOfDay";

	public static final String IS_STATUS_MSG = "isStatusMsg";

	public static final String STATUS_INDEX = "statusIdx";

	public static final String USER_INDEX = "userIdx";

	public static final String THUMBNAIL_INDEX = "thumbnailIdx";

	public static final String FAVORITE_INDEX = "favoriteIdx";

	public static final String IS_HIKE_MESSAGE = "isHikeMessage";

	public static final String STICKER_CATEGORIES_TABLE = "stickerCategoriesTable";
	
	public static final String CATEGORY_ID = "categoryId";

	public static final String TOTAL_NUMBER = "totalNum";

	public static final String UPDATE_AVAILABLE = "updateAvailable";

	public static final String LAST_SEEN = "lastSeen";

	public static final String PROTIP_TABLE = "protipTable";

	public static final String PROTIP_MAPPED_ID = "protipMappedId";

	public static final String HEADER = "header";

	public static final String PROTIP_TEXT = "protipText";

	public static final String IMAGE_URL = "imageUrl";

	public static final String WAIT_TIME = "waitTime";

	public static final String PROTIP_GAMING_DOWNLOAD_URL = "url";

	public static final String IS_OFFLINE = "isOffline";

	public static final String UNREAD_COUNT = "unreadCount";

	public static final String SHARED_MEDIA_TABLE = "sharedMediaTable";

	public static final String FILE_THUMBNAIL_TABLE = "fileThumbnailTable";

	public static final String READ_BY = "readBy";

	public static final String ROUNDED_THUMBNAIL_TABLE = "roundedThumbnailTable";

	public static final String ROUNDED_THUMBNAIL_INDEX = "roundedThumbnailIndex";

	public static final String FILE_THUMBNAIL_INDEX = "fileThumbnailIndex";

	public static final String INVITE_TIMESTAMP = "inviteTimestamp";

	public static final String CHAT_BG_TABLE = "chatBgTable";

	public static final String BG_ID = "bgId";

	public static final String CHAT_BG_INDEX = "chatBgIndex";

	public static final String IS_STEALTH = "isStealth";
	
	public static final String MESSAGE_HASH_INDEX = "messageHashIndex";

	public static final String HIKE_FILE_TYPE = "hikeFileType";

	public static final String _ID = "_id";
	
	public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ";
	public static final String CREATE_INDEX = "CREATE INDEX IF NOT EXISTS ";
	
	public static final String IS_SENT = "isSent";
	public static final String BOT_TABLE = "botTable";

	public static interface HIKE_CONV_DB{
		// CHANNEL TABLE -> _id,channel_id,name,visibility,index 
		public static final String CHANNEL_TABLE = "channel";
		public static final String CHANNEL_ID = "channel_id";
		public static final String CHANNEL_NAME = "name";
		public static final String VISIILITY = "visibility";
		public static final String INDEX_ORDER = "index";
		// CHANNEL TABLE ENDS HERE
		// LOVE TABLE -> _id,love_id,count,user_status,ref_count,timestamp
		public static final String LOVE_TABLE = "love";
		public static final String LOVE_ID = "love_id";
		public static final String COUNT = "count";
		public static final String USER_STATUS = "user_status";
		public static final String REF_COUNT = "ref_count";
		public static final String TIMESTAMP = "timestamp";
		// LOVE TABLE ENDS HERE
		// MESSAGE TABLE
		public static final String LOVE_ID_REL = "love_id";
		// MESSAGE TABLE ENDS HERE
	}
	/**
	 * 
	 * @author gauravKhanna
	 *
	 */
	public static interface HIKE_CONTENT{
		public static final int DB_VERSION = 1;
		public static final String DB_NAME = "hike_content_db";
		// CONTENT TABLE -> _id,content_id,love_id,channel_id,timestamp,metadata
		public static final String CONTENT_TABLE = "content";
		public static final String CONTENT_ID = "content_id";
		public static final String LOVE_ID = "love_id";
		public static final String CHANNEL_ID = "channel_id";
		public static final String TIMESTAMP = "timestamp";
		public static final String METADATA = "metadata";
		
		//ALARM TABLE->id,time,willWakeCpu,time,intent
		
		public static final String ALARM_MGR_TABLE = "HikeAlaMge";

		public static final String TIME = "time";

		public static final String WILL_WAKE_CPU = "willwakecpu";

		public static final String INTENT = "intent";

		// CONTENT TABLE ENDS HERE
		// APP DATA TABLE
		
		// APP DATA TABLE ENDS HERE
		// APP ALARM -> id, data 
		public static final String APP_ALARM_TABLE = "app_alarms";
		public static final String ID = "id";
		public static final String ALARM_DATA = "data";
		// APP ALARM ENDS HERE
		public static final String CONTENT_ID_INDEX = "contentTableContentIdIndex";
	}

	public static final String CATEGORY_NAME = "categoryName";

	public static final String IS_VISIBLE = "isVisible";

	public static final String IS_CUSTOM = "isCustom";

	public static final String IS_ADDED = "isAdded";

	public static final String CATEGORY_INDEX = "catIndex";

	public static final String CATEGORY_SIZE = "categorySize";

	public static final String STICKER_SHOP_TABLE = "stickerShopTable";

}
