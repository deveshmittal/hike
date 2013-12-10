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
			return R.color.chat_thread_default_bg;
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
	},
	AQUA {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_aqua_tile;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_aqua_tile;
		}

		@Override
		public String bgId() {
			return "1";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	},
	BLUE {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_blue;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_blue;
		}

		@Override
		public String bgId() {
			return "2";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	},
	BLUE_GREEN {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_blue_green;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_blue_green;
		}

		@Override
		public String bgId() {
			return "3";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	},
	CYAN {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_cyan_snowman;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_cyan_snowman;
		}

		@Override
		public String bgId() {
			return "4";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	},
	GREEN {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_green_christmastree;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_green_christmastree;
		}

		@Override
		public String bgId() {
			return "5";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	},
	ORANGE {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_orange;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_orange;
		}

		@Override
		public String bgId() {
			return "6";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	},
	PURPLE {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_purple_gift;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_purple_gift;
		}

		@Override
		public String bgId() {
			return "7";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	},
	RED_CHRISTMAS {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_red_christmas;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_red_christmas;
		}

		@Override
		public String bgId() {
			return "8";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	},
	RED_HEARTS {
		@Override
		public int bgResId() {
			return R.drawable.bg_ct_red_hearts;
		}

		@Override
		public int bubbleResId() {
			return R.drawable.ic_bubble_translucent;
		}

		@Override
		public int previewResId() {
			return R.drawable.bg_ct_red_hearts;
		}

		@Override
		public String bgId() {
			return "9";
		}

		@Override
		public int headerBgResId() {
			return R.drawable.bg_header_transparent;
		}

		@Override
		public boolean isTiled() {
			return true;
		}
	};

	public abstract String bgId();

	public abstract int headerBgResId();

	public abstract int previewResId();

	public abstract int bgResId();

	public abstract int bubbleResId();

	public abstract boolean isTiled();

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