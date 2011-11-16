package com.bsb.im.smack.avatar;

import java.util.List;
import java.util.Arrays;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.PayloadItem;

import com.bsb.im.smack.pep.PepSubManager;


/**
 * An AvatarRetriever which retrieve the avatar over the XMPP connection.
 */
public class XmppAvatarRetriever implements AvatarRetriever {

    private static String AVATARDATANODE = "urn:xmpp:avatar:data";
    private PepSubManager mPubsub;
    private String mFrom;
    private String mId;

    /**
     * Create an XmppAvatarRetriever.
     *
     * @param con the xmpp connection
     * @param from the contact from which we retrieve the avatar
     * @param id the id of the avatar to retrieve
     */
    public XmppAvatarRetriever(final Connection con, final String from, final String id) {
	mPubsub = new PepSubManager(con, from);
	mFrom = from;
	mId = id;
    }

    @Override
    public byte[] getAvatar() {
	try {
	    LeafNode node = mPubsub.getPEPNode(AVATARDATANODE);
	    List<Item> items = node.getItems(Arrays.asList(mId));
	    PayloadItem<AvatarExtension> item = (PayloadItem<AvatarExtension>) items.get(0);
	    AvatarExtension avatar = item.getPayload();
	    return avatar.getData();
	} catch (XMPPException e) {
	    return null;
	}
    }

}
