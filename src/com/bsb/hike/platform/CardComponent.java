package com.bsb.hike.platform;

public class CardComponent {
	protected String tag;
	public CardComponent(String tag) {
		this.tag = tag;
	}
	public String getTag() {
		return tag;
	}

	

	public static class TextComponent extends CardComponent {
		private String text;

		public String getText() {
			return text;
		}

		public TextComponent(String tag,String text) {
			super(tag);
			this.text = text;
		}
	}

	public static class MediaComponent extends CardComponent {
		private int key;
		private String base64;
		private String url;
		private String type, size, duration;

		public MediaComponent(String tag, String base64, String url,
				String type, String size, String duration) {
			super(tag);
		}

		public int getKey() {
			return key;
		}

		public String getBase64() {
			return base64;
		}

		public String getUrl() {
			return url;
		}

		public String getDuration() {
			return duration;
		}

		public String getSize() {
			return size;
		}

		public String getType() {
			return type;
		}
	}

	public static class ImageComponent extends MediaComponent {

		public ImageComponent(String tag, String base64, String url,
				String type, String size) {
			super(tag, base64, url, type, size,null);
		}

	}

	public static class VideoComponent extends MediaComponent {

		public VideoComponent(String tag, String base64, String url,
				String videoType, String size, String duration) {
			super(tag, base64, url, videoType, size, duration);
		}

	}

	public static class AudioComponent extends MediaComponent {

		public AudioComponent(String tag, String base64, String url,
				String type, String size, String duration) {
			super(tag, base64, url, type, size, duration);
		}

	}
}
