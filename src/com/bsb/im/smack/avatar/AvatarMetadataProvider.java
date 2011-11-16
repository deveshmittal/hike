package com.bsb.im.smack.avatar;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * A PacketExtensionProvider to parse the Avatar metadata.
 * XML namespace urn:xmpp:avatar:metadata
 */
public class AvatarMetadataProvider implements PacketExtensionProvider {

    /**
     * Creates a new AvatarMetadataProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument constructor
     */
    public AvatarMetadataProvider() {
    }

    @Override
    public PacketExtension parseExtension(XmlPullParser parser)
	throws Exception {
	AvatarMetadataExtension metadata = new AvatarMetadataExtension();
	boolean done = false;
	StringBuilder buffer = new StringBuilder();
	while (!done) {
	    int eventType = parser.next();
	    if (eventType == XmlPullParser.START_TAG) {
		if ("info".equals(parser.getName())) {
		    String id = parser.getAttributeValue(null, "id");
		    String type = parser.getAttributeValue(null, "type");
		    String sbytes = parser.getAttributeValue(null, "bytes");
		    String sheight = parser.getAttributeValue(null, "height");
		    String swidth = parser.getAttributeValue(null, "width");
		    int bytes = 0;
		    AvatarMetadataExtension.Info info = null;
		    try {
			if (sbytes != null)
			    bytes = Integer.parseInt(sbytes);
		    } catch (NumberFormatException e) { }
		    if (bytes != 0 && id != null && type != null)
			info = new AvatarMetadataExtension.Info(id, type, bytes);
		    else // invalid info
			continue;

		    String url = parser.getAttributeValue(null, "url");
		    info.setUrl(url);
		    try {
			int height = 0;
			int width = 0;
			if (sheight != null)
			    height = Integer.parseInt(parser.getAttributeValue(null, "height"));
			if (swidth != null)
			    width = Integer.parseInt(parser.getAttributeValue(null, "width"));
			info.setHeight(height);
			info.setWidth(width);
		    } catch (NumberFormatException e) { }
		    metadata.addInfo(info);
		}
	    } else if (eventType == XmlPullParser.END_TAG) {
		if (parser.getName().equals(metadata.getElementName())) {
		    done = true;
		}
	    }
	}
	return metadata;
    }
}
