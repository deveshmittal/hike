package com.bsb.hike.models;

public class ContactInfo {
	@Override
    public String toString() {
        return "ContactInfo [name=" + name + ", number=" + number + ", id="
                + id + ", onhike=" + onhike + "]";
    }

    public String name;
	public String number;
	public String id;
	public boolean onhike;

	public ContactInfo(String id, String number, String name) {
		this(id, number, name, false);
	}

	public ContactInfo(String id, String number, String name, boolean onhike) {
		this.id = id;
		this.number = number;
		this.name = name;
		this.onhike = onhike;
	}
}
