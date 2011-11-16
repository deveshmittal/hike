package com.bsb.im.smack.avatar;

import org.jivesoftware.smack.util.Base64;
import org.jivesoftware.smack.packet.PacketExtension;

/**
 * PacketExtension to represent the Avatar data.
 * XML namespace urn:xmpp:avatar:data
 *
 */
public class AvatarExtension implements PacketExtension {

    private String mData;

    /**
     * Create an AvatarExtension.
     * @param base64 the data of the avatar as a base64 string
     */
    public AvatarExtension(final String base64) {
	mData = base64;
    }

    /**
     * Create an AvatarExtension.
     * @param data the data of the avatar
     */
    public AvatarExtension(final byte[] data) {
	mData = Base64.encodeBytes(data);
    }

    /**
     * Get the avatar data as a Base64 string.
     *
     * @return a base64 string.
     */
    public String getBase64() {
	return mData;
    }

    /**
     * Get the avatar data.
     *
     * @return the decoded data
     */
    public byte[] getData() {
	return Base64.decode(mData);
    }

    @Override
    public String getElementName() {
	return "data";
    }

    @Override
    public String getNamespace() {
	return "urn:xmpp:avatar:data";
    }

    @Override
    public String toXML() {
	StringBuilder builder = new StringBuilder("<data xmlns=\"");
	builder.append(getNamespace()).append("\">");
	builder.append(mData);
	builder.append("</data>");
	return builder.toString();
    }

}
