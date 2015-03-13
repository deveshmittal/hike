package com.bsb.hike.productpopup;


public class ProductPopupsConstants
{
	/**
	 * Product Popups Constants
	 */

	public static final String NOTIFICATION_TIME = "notiftime";

	public static final String TRIGGER_POINT = "triggerpoint";

	public static final String START_TIME = "starttime";

	public static final String END_TIME = "endTime";

	public static final String IS_FULL_SCREEN = "isFullScreen";

	public static final String USER = "notiftext";

	public static final String PUSH = "push";

	public static final String NOTIFICATION_TITLE = "notiftitle";

	public static final String PUSH_TIME = "pushTime";

	public static final String STKID = "stkId";
	
	public static final String CATID="catId";

	public static final String PUSH_SOUND = "sound";
	
	public static String SELECTALL="selectAll";
	
	public static String DIALOG_TAG="dialog";
	
	public static String SHOW_CAMERA="show_camera";
	
	public static String URL="url";

	public static enum PopupStateEnum
	{
		NOT_DOWNLOADED(0), DOWNLOADED(1), UNKNOWN(3);
		private int value;

		private PopupStateEnum(int val)
		{
			value = val;
		}

		public static PopupStateEnum getEnumValue(int val)
		{
			for (PopupStateEnum en : PopupStateEnum.values())
			{
				if (en.value == val)
					return en;

			}
			return UNKNOWN;
		}
	}

	public static enum PopupTriggerPoints
	{
		HOME_SCREEN(0), TIMELINE(1), CHAT_SCR(2), ATCH_SCR(3),COMPOSE_CHAT(4), SETTINGS_SCR(5), STATUS(6), NEWGRP(7),
		INVITEFRNDS(8), STKBUT_BUT(9),STICKER_SHOP(10),STICKER_SHOP_SETTINGS(11),PROFILE_PHOTO(12),EDIT_PROFILE(13),
		NOTIFICATION(14),MEDIA(15),ACCOUNT(16),PRIVACY(17),HELP(18),INVITE_SMS(19),FREE_SMS(20),FAVOURITES(21),UNKNOWN(22);
		private int value;

		private PopupTriggerPoints(int val)
		{
			value = val;
		}

		public static PopupTriggerPoints getEnumValue(int val)
		{
			for (PopupTriggerPoints en : PopupTriggerPoints.values())
			{
				if (en.value == val)
					return en;

			}
			return UNKNOWN;
		}
	}


	public static class PopUpAction
	{
		
		public static final String OPENAPPSCREEN="openappscreen";

		public static final String CALLTOSERVER="callserver";
	}

	public static enum HIKESCREEN
	{

		SETTING
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "setting";
			}
		},
		NOTIFICATION
		{
			@Override
			public String toString()
			{
				return "notif";
			}
		},
		MEDIA
		{
			@Override
			public String toString()
			{
				return "media";
			}
		},
		FREE_SMS
		{
			@Override
			public String toString()
			{
				return "freesms";
			}
		},
		ACCOUNT
		{
			@Override
			public String toString()
			{
				return "account";
			}
		},
		PRIVACY
		{
			@Override
			public String toString()
			{
				return "privacy";
			}
		},
		TIMELINE
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "timeline";
			}
		},
		NEWGRP
		{
			@Override
			public String toString()
			{
				return "newgroup";
			}
		},
		INVITEFRNDS
		{
			@Override
			public String toString()
			{
				return "invitefriends";
			}
		},
		REWARDS_EXTRAS
		{
			@Override
			public String toString()
			{
				return "rewards_extras";
			}
		},
		STICKER_SHOP
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "stickershop";
			}
		},
		STICKER_SHOP_SETTINGS
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "stickershopsettings";
			}
		},
		STATUS
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "status";
			}
		},
		COMPOSE_CHAT
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "newchat";
			}
		},
		HIDDEN_MODE
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "hiddenmode";
			}
		},
		INVITE_SMS
		{
			@Override
			public String toString()
			{
				return "invite_sms";
			}
		},
		FAVOURITES
		{
			@Override
			public String toString()
			{
				return "favourites";
			}
		},
		HOME_SCREEN
		{
			@Override
			public String toString()
			{
				return "homescreen";
			}
		},
		PROFILE_PHOTO
		{
			
			@Override
			public String toString()
			{
				return "profilephoto";

			}
		},EDIT_PROFILE{
			
			@Override
			public String toString()
			{
				return "edit_profile";
			}
		},
		HELP
		{
			@Override
			public String toString()
			{
				return "help";
			};
		},
		INVITE_WHATSAPP
		{
			@Override
			public String toString()
			{
				return "invwhatsapp";
			}
		},
		MULTI_FWD_STICKERS
		{
			@Override
			public String toString()
			{
				return "multifwd";
			};
		},
		OPEN_WEB_VIEW
		{
			@Override
			public String toString()
			{
				return "openwebview";
			}
		},
		OPENINBROWSER
		{
			@Override
			public String toString()
			{
				return "openbrowser";
			}
		},
		OPENAPPSTORE
		{
			@Override
			public String toString()
			{
				return "openappstore";
			}
		},
		CANCELDIALOG
		{
			@Override
			public String toString()
			{
				return "cancel";
			}
		},
		NUXINVITE
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "nuxinvite";
			}
		},
		NUXREMIND
		{
			@Override
			public String toString()
			{
				// TODO Auto-generated method stub
				return "nuxremind";
			}
		};
		
		
		
	}

	public static class PushTypeEnum
	{
		public static final String NONE = "none";

		public static final String SILENT = "silent";

		public static final String LOUD = "loud";

		public static final String UNKNOWN = "unknown";

		public static String getEnum(String pushType)
		{
			if (LOUD.equals(pushType))
				return LOUD;

			if (SILENT.equals(pushType))
				return SILENT;

			return UNKNOWN;
		}

	}

}
