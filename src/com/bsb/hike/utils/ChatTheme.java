package com.bsb.hike.utils;

import com.bsb.hike.R;

public enum ChatTheme
{

	DEFAULT
	{
		@Override
		public int bgResId()
		{
			return R.color.chat_thread_default_bg;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_blue_selector;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_default_preview;
		}

		@Override
		public String bgId()
		{
			return "0";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_hike_sent;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_hike_receive;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_blue;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.list_item_subtext;
		}
		
	},

	SPRING
	{

		@Override
		public String bgId()
		{
			return "24";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_spring_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_spring;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_forest_study_sporty;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_forest_study_sporty;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	MOUNTAINS
	{

		@Override
		public String bgId()
		{
			return "25";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_mountain_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_mountain;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_hikin_couple_mountain;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_hikin_couple_mountain;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_3x;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_3x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_hikin_couple_mountain;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.action_bar_item_pressed;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	BEACH_2
	{

		@Override
		public String bgId()
		{
			return "26";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_beach2_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_beach_2;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_rains_beach_2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_rain_beach_2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_2x;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_3x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_rains_beach_2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	CRICKET
	{

		@Override
		public String bgId()
		{
			return "27";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_cricket_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_cricket;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_forest_study_sporty;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_forest_study_sporty;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	FRIENDS
	{

		@Override
		public String bgId()
		{
			return "28";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_friends_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_friends;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_2x;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_2x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	RAIN
	{

		@Override
		public String bgId()
		{
			return "29";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_3x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_rains_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_rains;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_rains_beach_2;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_rain_beach_2;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_3x;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_3x;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_rains_beach_2;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme_3x;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.action_bar_item_pressed;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	MUSIC
	{

		@Override
		public String bgId()
		{
			return "30";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_music_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_music;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_music;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_white;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_music;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme_2x;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	MR_RIGHT
	{

		@Override
		public String bgId()
		{
			return "31";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_mr_right_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_mr_right;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_hikin_couple_mountain;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_hikin_couple_mountain;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_hikin_couple_mountain;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	HIKIN_COUPLE
	{

		@Override
		public String bgId()
		{
			return "32";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent_2x;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_hikin_couple_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_hikin_couple;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_mr_right_exam;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_mr_right_exam;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_mr_right_exam;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme_2x;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	EXAM
	{

		@Override
		public String bgId()
		{
			return "33";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_exam_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_exam;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_mr_right_exam;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_mr_right_exam;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_mr_right_exam;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	ANXIETY
	{

		@Override
		public String bgId()
		{
			return "34";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_anxiety_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_anxiety;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_starry_space;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_starry_space;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_stary_Space;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	VALENTINES_2
	{

		@Override
		public String bgId()
		{
			return "20";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_valentine_2_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_valentines_2_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_valentines_2;
		}

		@Override
		public boolean isAnimated()
		{
			return true;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom_valentines_2;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	NIGHT_PATTERN
	{

		@Override
		public String bgId()
		{
			return "21";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_night_pattern_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_night_pattern;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_night;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_night;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_dark;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_dark;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_night;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	NIGHT
	{

		@Override
		public String bgId()
		{
			return "22";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.bg_ct_night_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_night;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_night;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_night;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme_dark;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom_dark;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_night;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	OWL
	{

		@Override
		public String bgId()
		{
			return "23";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.bg_ct_owl_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_owl;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_owl;
		}

		@Override
		public boolean isTiled()
		{
			return false;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_owl;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_owl;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},

	LOVE
	{

		@Override
		public String bgId()
		{
			return "1";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_love_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_love_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	CHATTY
	{

		@Override
		public String bgId()
		{
			return "2";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_chatty_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_chatty_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	GIRLY
	{

		@Override
		public String bgId()
		{
			return "3";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_girly_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_girly_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	STARRY
	{

		@Override
		public String bgId()
		{
			return "4";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_starry_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_starry_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_starry_space;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_starry_space;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_stary_Space;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	CHEERS
	{

		@Override
		public String bgId()
		{
			return "5";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_cheers_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_cheery_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	SPORTY
	{

		@Override
		public String bgId()
		{
			return "6";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_sporty_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_sporty_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_forest_study_sporty;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_forest_study_sporty;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}
	},
	SMILEY
	{

		@Override
		public String bgId()
		{
			return "7";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_smiley_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_smiley_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	CREEPY
	{

		@Override
		public String bgId()
		{
			return "8";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_creepy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_creepy_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_creepy;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_creepy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_creepy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_blue_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	CELEBRATION
	{

		@Override
		public String bgId()
		{
			return "9";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_celebration_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_celebration_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_celebration_space;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_celebration_space;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_celebration_space;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	FLORAL
	{

		@Override
		public String bgId()
		{
			return "10";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_floral_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_floral_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	FOREST
	{

		@Override
		public String bgId()
		{
			return "11";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_forest_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_forest_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_forest_study_sporty;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_forest_study_sporty;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	CUPCAKES
	{

		@Override
		public String bgId()
		{
			return "12";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_cupcakes_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_cupcakes_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	TECHY
	{

		@Override
		public String bgId()
		{
			return "13";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_techy_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_techy_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	KISSES
	{

		@Override
		public String bgId()
		{
			return "14";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_kisses_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_kisses_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	BEACH
	{

		@Override
		public String bgId()
		{
			return "15";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_beach_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_beach_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_chatty_bechy_techy;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	PETS
	{

		@Override
		public String bgId()
		{
			return "16";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_pets_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_pets_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	STUDY
	{

		@Override
		public String bgId()
		{
			return "17";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_study_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_study_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_forest_study_sporty;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_forest_study_sporty;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	},
	VALENTINES
	{

		@Override
		public String bgId()
		{
			return "18";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_valentine_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_valentines_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}

		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}
	},
	BIKER
	{

		@Override
		public String bgId()
		{
			return "19";
		}

		@Override
		public int headerBgResId()
		{
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId()
		{
			return R.drawable.ic_ct_bikers_preview;
		}

		@Override
		public int bgResId()
		{
			return R.drawable.bg_ct_bikers_tile;
		}

		@Override
		public int bubbleResId()
		{
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled()
		{
			return true;
		}

		@Override
		public int sentNudgeResId()
		{
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isAnimated()
		{
			return false;
		}

		@Override
		public int receivedNudgeResId()
		{
			return R.drawable.ic_nudge_receive_custom;
		}

		@Override
		public int inLineUpdateBGResId()
		{
			return R.drawable.bg_status_chat_thread_custom_theme;
		}

		@Override
		public int systemMessageLayoutId()
		{
			return R.layout.participant_info_custom;
		}
		
		@Override
		public int bubbleColor()
		{
			return R.color.bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public int smsToggleBgRes()
		{
			return R.drawable.bg_sms_toggle_custom_theme;
		}

		@Override
		public int multiSelectBubbleColor()
		{
			return R.color.light_black_transparent;
		}

		@Override
		public int offlineMsgTextColor()
		{
			return R.color.white;
		}

	};

	public abstract String bgId();

	public abstract int headerBgResId();

	public abstract int previewResId();

	public abstract int bgResId();

	public abstract int bubbleResId();

	public abstract boolean isTiled();

	public abstract int sentNudgeResId();

	public abstract boolean isAnimated();

	public abstract int receivedNudgeResId();

	public abstract int inLineUpdateBGResId();

	public abstract int systemMessageLayoutId();
	
	public abstract int bubbleColor();
	
	public abstract int smsToggleBgRes();
	
	public abstract int multiSelectBubbleColor();
	
	public abstract int offlineMsgTextColor();

	public static ChatTheme getThemeFromId(String bgId)
	{
		if (bgId == null)
		{
			throw new IllegalArgumentException();
		}
		for (ChatTheme chatTheme : values())
		{
			if (chatTheme.bgId().equals(bgId))
			{
				return chatTheme;
			}
		}
		throw new IllegalArgumentException();
	}

	public static ChatTheme[] FTUE_THEMES = { STARRY, BEACH, FOREST };
};