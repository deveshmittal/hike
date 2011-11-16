package com.bsb.im.smack.caps;

import org.xmlpull.v1.XmlPullParser;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.packet.PacketExtension;

/**
 * PacketExtensionProvider for XEP-0115.
 * This provider parse c element of namespace
 * http://jabber.org/protocol/caps which represents a capability of XEP-0115
 *
 */
public class CapsProvider implements PacketExtensionProvider {

    /**
     * Constructor.
     */
    public CapsProvider() { }

    @Override
    public PacketExtension parseExtension(XmlPullParser parser) {
	String ver = parser.getAttributeValue("", "ver");
	String hash = parser.getAttributeValue("", "hash");
	String node = parser.getAttributeValue("", "node");
	String ext = parser.getAttributeValue("", "ext");
	CapsExtension e = new CapsExtension(hash, node, ver);
	e.setExt(ext);
	return e;
    }

}
