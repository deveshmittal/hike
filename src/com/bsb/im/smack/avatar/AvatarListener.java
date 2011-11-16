package com.bsb.im.smack.avatar;

import java.util.List;

import com.bsb.im.smack.avatar.AvatarMetadataExtension.Info;

/**
 * A listener for avatar changes event.
 *
 */
public interface AvatarListener {

    /**
     * Event which is fired when a contact change avatar.
     *
     * @param from the contact who change his avatar
     * @param avatarId the new avatar id, may be null if the contact set no avatar
     * @param avatarInfos the metadata infos of the avatar, may be empty if the contact set no avatar
     */
    void onAvatarChange(String from, String avatarId, List<Info> avatarInfos);

}
