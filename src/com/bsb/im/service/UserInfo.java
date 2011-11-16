package com.bsb.im.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class contains information about the user of the connection.
 * These informations are sent by the connection.
 *
 */
public class UserInfo implements Parcelable {

    /** Parcelable.Creator needs by Android. */
    public static final Parcelable.Creator<UserInfo> CREATOR = new Parcelable.Creator<UserInfo>() {

	@Override
	public UserInfo createFromParcel(Parcel source) {
	    return new UserInfo(source);
	}

	@Override
	public UserInfo[] newArray(int size) {
	    return new UserInfo[size];
	}
    };

    private final String mFullJid;
    private String mAvatarId;

    /**
     * Construct a UserInfo from a parcel.
     * @param in parcel to use for construction
     */
    private UserInfo(final Parcel in) {
	mFullJid = in.readString();
	mAvatarId = in.readString();
    }

    /**
     * Constructor.
     * @param jid jid of the user
     */
    public UserInfo(final String jid) {
	// the jid is case insensitive
	mFullJid = jid;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
	dest.writeString(mFullJid);
	dest.writeString(mAvatarId);
    }

    @Override
    public int describeContents() {
	return 0;
    }

    /**
     * Get the avatar id of the user.
     *
     * @return the avatar id
     */
    public String getAvatarId() {
	return mAvatarId;
    }

    /**
     * Set the avater id of the user.
     *
     * @param avatarId the avatar id
     */
    public void setAvatarId(String avatarId) {
	mAvatarId = avatarId;
    }

    /**
     * Get the full jid of the user.
     *
     * @return the jid
     */
    public String getJid() {
	return mFullJid;
    }
}
