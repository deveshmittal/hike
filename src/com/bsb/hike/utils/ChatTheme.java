package com.bsb.hike.utils;

import com.bsb.hike.R;

public enum ChatTheme {

	DEFAULT {
		@Override
		public int bgResId() {
			return R.color.chat_thread_default_bg;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_blue_selector;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_default_preview;
		}

		@Override
		public String bgId() {
			return "0";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header;
		}

		@Override
		public boolean isTiled() {
			return false;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_hike_sent;
		}
	},
	LOVE {

		@Override
		public String bgId() {
			return "1";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_love_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_love_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

	},
	CHATTY {

		@Override
		public String bgId() {
			return "2";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_chatty_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_chatty_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

	},
	GIRLY {

		@Override
		public String bgId() {
			return "3";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_girly_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_girly_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

	},
	STARRY {

		@Override
		public String bgId() {
			return "4";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_starry_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_starry_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_starry_space;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_starry_space;
		}

	},
	CHEERS {

		@Override
		public String bgId() {
			return "5";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_cheers_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_cheery_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_smiley_cheers_pets_sporty_cupcakes;
		}

	},
	SPORTY {

		@Override
		public String bgId() {
			return "6";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_sporty_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_sporty_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_forest_study_sporty;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty;
		}
	},
	SMILEY {

		@Override
		public String bgId() {
			return "7";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_smiley_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_smiley_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_smiley_cheers_pets_sporty_cupcakes;
		}

	},
	CREEPY {

		@Override
		public String bgId() {
			return "8";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_creepy_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_creepy_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_creepy;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_creepy;
		}

	},
	CELEBRATION {

		@Override
		public String bgId() {
			return "9";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_celebration_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_celebration_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_celebration_space;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_celebration_space;
		}

	},
	FLORAL {

		@Override
		public String bgId() {
			return "10";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_floral_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_floral_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

	},
	FOREST {

		@Override
		public String bgId() {
			return "11";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_forest_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_forest_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_forest_study_sporty;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty;
		}

	},
	CUPCAKES {

		@Override
		public String bgId() {
			return "12";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_cupcakes_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_cupcakes_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_smiley_cheers_pets_sporty_cupcakes;
		}

	},
	TECHY {

		@Override
		public String bgId() {
			return "13";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_techy_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_techy_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

	},
	KISSES {

		@Override
		public String bgId() {
			return "14";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_kisses_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_kisses_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

	},
	BEACH {

		@Override
		public String bgId() {
			return "15";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_beach_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_beach_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_chatty_beachy_techy;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_chatty_beachy_techy;
		}

	},
	PETS {

		@Override
		public String bgId() {
			return "16";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_pets_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_pets_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_smiley_cheers_pets_sporty_cupcakes;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_smiley_cheers_pets_sporty_cupcakes;
		}

	},
	STUDY {

		@Override
		public String bgId() {
			return "17";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_study_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_study_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_forest_study_sporty;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_forest_study_sporty;
		}

	},
	VALENTINES {

		@Override
		public String bgId() {
			return "18";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_valentine_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_valentines_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

	},
	BIKER {

		@Override
		public String bgId() {
			return "19";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public int previewResId() {
			return R.drawable.ic_ct_bikers_preview;
		}

		@Override
		public int bgResId() {
			return R.drawable.bg_ct_bikers_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_love_floral_bikers_kisses_valentines_girly;
		}

		@Override
		public boolean isTiled() {
			return true;
		}

		@Override
		public int sentNudgeResId() {
			return R.drawable.ic_nudge_sent_custom_love_floral_bikers_kisses_valentines_girly;
		}

	};

	public abstract String bgId();

	public abstract int headerBgResId();

	public abstract int previewResId();

	public abstract int bgResId();

	public abstract int bubbleResId();

	public abstract boolean isTiled();

	public abstract int sentNudgeResId();

	public static ChatTheme getThemeFromId(String bgId) {
		if (bgId == null) {
			throw new IllegalArgumentException();
		}
		for (ChatTheme chatTheme : values()) {
			if (chatTheme.bgId().equals(bgId)) {
				return chatTheme;
			}
		}
		throw new IllegalArgumentException();
	}
};