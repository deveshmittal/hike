package com.bsb.im.smack.avatar;

import org.jivesoftware.smack.Connection;
// API level 8
//import android.net.http.AndroidHttpClient;
//import org.apache.http.client.HttpClient;

import com.bsb.im.smack.avatar.AvatarMetadataExtension.Info;

/**
 * A factory for AvatarRetriever.
 */
public final class AvatarRetrieverFactory {

    /**
     * Private constructor.
     */
    private AvatarRetrieverFactory() {
    }

    /**
     * Get a AvatarRetriever to retrieve this avatar.
     *
     * @param con the connection
     * @param from the user which own the avatar
     * @param info the metadata information of the avatar to retrieve
     * @return an AvatarRetriever null if none can retrieve this avatar
     */
    public static AvatarRetriever getRetriever(Connection con, String from, Info info) {
	String url = info.getUrl();
	if (url != null) {
	    // return new HttpAvatarRetriever(url);
	    // HttpClient client = AndroidHttpClient.newInstance("Beem");
	    return new HttpClientAvatarRetriever(url);
	}
	return new XmppAvatarRetriever(con, from, info.getId());
    }
}
