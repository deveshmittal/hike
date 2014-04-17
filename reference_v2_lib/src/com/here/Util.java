package com.here;

public class Util {
	
	private static final String HERE_MAP_CLASS_NAME = "com.here.android.mapping.Map";

	/**
	 * Are Here Maps supported in this device
	 * 
	 * @return	true if Here Maps are available
	 */
	public static boolean isHereMapsAvailable() {
		boolean available = true;
		try {
			Class.forName(HERE_MAP_CLASS_NAME);
		} catch (ClassNotFoundException e) {
			available = false;
		}
		return available;
	}
}
