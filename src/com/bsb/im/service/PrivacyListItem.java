package com.bsb.im.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A simplified version of the Smack PrivacyItem class.
 */
public class PrivacyListItem implements Parcelable {

    /**
     * Constructor. Needed to implements the Parcelable.Creator interface. Generates instances of PrivacyListItem from a
     * Parcel.
     */
    public static final Parcelable.Creator<PrivacyListItem> CREATOR = new Parcelable.Creator<PrivacyListItem>() {
	public PrivacyListItem createFromParcel(Parcel in) {
	    return new PrivacyListItem(in);
	}

	public PrivacyListItem[] newArray(int size) {
	    return new PrivacyListItem[size];
	}
    };

    private int mType;
    private String mValue;

    /**
     * Constructor.
     */
    public PrivacyListItem() {
    }

    /**
     * Constructor. Generates instances of PrivacyListItem from a Parcel.
     * @param in The Parcel used to initialize object's attributes.
     */
    public PrivacyListItem(final Parcel in) {
	readFromParcel(in);
    }

    /**
     * Constructor.
     * @param type The type of the item.
     * @param value The value of the item.
     */
    public PrivacyListItem(final int type, final String value) {
	mType = type;
	mValue = value;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int describeContents() {
	return 0;
    }

    /**
     * Initialize object's attributes from a Parcel.
     * @param in The Parcel used to initialize object's attributes.
     */
    public void readFromParcel(Parcel in) {
	mType = in.readInt();
	mValue = in.readString();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
	dest.writeInt(mType);
	dest.writeString(mValue);
    }

    /**
     * PrivacyListItem type accessor.
     * @return The type of the PrivacyListItem.
     */
    public int getType() {
	return mType;
    }

    /**
     * PrivacyListItem value accessor.
     * @return The value of the PrivacyListItem.
     */
    public String getValue() {
	return mValue;
    }

    /**
     * PrivacyListItem type mutator.
     * @param type The type of the PrivacyListItem.
     */
    public void setType(final int type) {
	mType = type;
    }

    /**
     * PrivacyListItem value mutator.
     * @param value The value of the PrivacyListItem.
     */
    public void setValue(final String value) {
	mValue = value;
    }
}
