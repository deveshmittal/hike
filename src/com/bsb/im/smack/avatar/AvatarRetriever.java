package com.bsb.im.smack.avatar;

import java.io.IOException;

/**
 * Interface for an AvatarRetriever.
 */
public interface AvatarRetriever {

    /**
     * Retrieve the avatar.
     *
     * @return the avatar
     * @throws IOException if an IO error occurs while retrieving the avatar
     */
    byte[] getAvatar() throws IOException;
}
