package com.bsb.hike.platform;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.platform.CardComponent.ImageComponent;
import com.bsb.hike.platform.CardComponent.MediaComponent;
import com.bsb.hike.platform.CardComponent.TextComponent;
import com.bsb.hike.platform.CardComponent.VideoComponent;

public class PlatformMessageMetadata implements HikePlatformConstants {
	public int layoutId;
	public int loveId;
	public String recepient; 
	public List<TextComponent> textComponents = new ArrayList<CardComponent.TextComponent>();;
	public List<MediaComponent> mediaComponents = new ArrayList<CardComponent.MediaComponent>();;

	public PlatformMessageMetadata(JSONObject json) {
		layoutId = getInt(json, LAYOUT_ID);
		loveId = getInt(json, LOVE_ID);
		recepient = getString(json, RECEPIENT);
		
		if (json.has(DATA)) {
			try {
				JSONObject data = json.getJSONObject(DATA);
				if (data.has(ASSETS)) {
					JSONObject assets = data.getJSONObject(ASSETS);
					if (assets.has(TEXTS)) {
						parseTextComponents(assets.getJSONArray(TEXTS));
					}
					if (assets.has(IMAGES)) {
						parseImageComponents(json.getJSONArray(IMAGES));
					}
					if(assets.has(VIDEOS)){
						parseVideoComponents(assets.getJSONArray(VIDEOS));
					}
					if(assets.has(AUDIO)){
						parseAudioComponents(assets.getJSONArray(AUDIO));
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private void parseTextComponents(JSONArray json) {
		int total = json.length();

		for (int i = 0; i < total; i++) {
			try {
				JSONObject obj = json.getJSONObject(i);
				TextComponent textCom = new TextComponent(obj.optString(TAG),
						obj.optString(TEXT));
				textComponents.add(textCom);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void parseImageComponents(JSONArray json) {
		int total = json.length();
		for (int i = 0; i < total; i++) {
			JSONObject obj;
			try {
				obj = json.getJSONObject(i);
				ImageComponent imageCom = new ImageComponent(
						obj.optString(TAG), obj.optString(BASE64_STRING),
						obj.optString(URL), obj.optString(CONTENT_TYPE),
						obj.optString(MEDIA_SIZE));
				mediaComponents.add(imageCom);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	private void parseVideoComponents(JSONArray json) {
		int total = json.length();
		for (int i = 0; i < total; i++) {
			JSONObject obj;
			try {
				obj = json.getJSONObject(i);
				VideoComponent videoCom = new VideoComponent(
						obj.optString(TAG), obj.optString(BASE64_STRING),
						obj.optString(URL), obj.optString(CONTENT_TYPE),
						obj.optString(MEDIA_SIZE),obj.optString(DURATION));
				mediaComponents.add(videoCom);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	private void parseAudioComponents(JSONArray json) {
		int total = json.length();
		for (int i = 0; i < total; i++) {
			JSONObject obj;
			try {
				obj = json.getJSONObject(i);
				VideoComponent audioCom = new VideoComponent(
						obj.optString(TAG), obj.optString(BASE64_STRING),
						obj.optString(URL), obj.optString(CONTENT_TYPE),
						obj.optString(MEDIA_SIZE),obj.optString(DURATION));
				mediaComponents.add(audioCom);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	private int getInt(JSONObject json,String key){
		if(json.has(key)){
			try {
				return json.getInt(key);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return 0;
	}
	private String getString(JSONObject json,String key){
		if(json.has(key)){
			try {
				return json.getString(key);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}
