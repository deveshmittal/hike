package com.bsb.hike.utils;

public class ContactInfo {
	public String name;
	public String number;
	public String id;
	public boolean onhike;

	public ContactInfo(String id, String name, String number) {
		this.id = id;
		this.name = name;
		this.number = number;
	}

	public ContactInfo(String id, String number, boolean onhike) {
		this.id = id;
		this.number = number;
		this.onhike = onhike;
	}
}
