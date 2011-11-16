package com.bsb.im.smack.avatar;

import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * PacketExtension to represent the Avatar metadata.
 * XML namespace urn:xmpp:avatar:metadata
 *
 */
public class AvatarMetadataExtension implements PacketExtension {
    private List<Info> mInfos = new LinkedList<Info>();

    /**
     * Create an AvatarMetadataExtension.
     */
    public AvatarMetadataExtension() {
    }

    /**
     * Get the metadata informations.
     *
     * @return a list of informations
     */
    public List<Info> getInfos() {
	return mInfos;
    }

    /**
     * Add a metadate information.
     *
     * @param info the metadata information to add
     */
    public void addInfo(Info info) {
	mInfos.add(info);
    }

    @Override
    public String getElementName() {
	return "metadata";
    }

    @Override
    public String getNamespace() {
	return "urn:xmpp:avatar:metadata";
    }

    @Override
    public String toXML() {
	StringBuilder builder = new StringBuilder("<metadata xmlns=\"");
	builder.append(getNamespace()).append("\">");
	for (Info info : mInfos) {
	    builder.append(info.toXML());
	}
	builder.append("</metadata>");
	return builder.toString();
    }

    /**
     * A metadata information element.
     */
    public static class Info {
	private int mBytes;
	private int mHeight;
	private int mWidth;
	private String mId;
	private String mType;
	private String mUrl;

	/**
	 * Create an Info.
	 *
	 * @param id the id of the info
	 * @param type the MIME type of the avatar
	 * @param bytes the size of the avatar in bytes
	 */
	public Info(final String id, final String type, final int bytes) {
	    mId = id;
	    mType = type;
	    mBytes = bytes;
	}

	/**
	 * Set the size of the avatar in bytes.
	 *
	 * @param bytes the size
	 */
	public void setBytes(int bytes) {
	    this.mBytes = bytes;
	}

	/**
	 * Set the size of the avatar in bytes.
	 *
	 * @return the size
	 */
	public int getBytes() {
	    return mBytes;
	}

	/**
	 * Set the height.
	 *
	 * @param height the height
	 */
	public void setHeight(int height) {
	    this.mHeight = height;
	}

	/**
	 * Get the height.
	 *
	 * @return the height
	 */
	public int getHeight() {
	    return mHeight;
	}

	/**
	 * Set the width.
	 *
	 * @param width the width
	 */
	public void setWidth(int width) {
	    this.mWidth = width;
	}

	/**
	 * Get the width.
	 *
	 * @return the width
	 */
	public int getWidth() {
	    return mWidth;
	}

	/**
	 * Set the url.
	 *
	 * @param url the url
	 */
	public void setUrl(String url) {
	    this.mUrl = url;
	}

	/**
	 * Get the url.
	 *
	 * @return the url, null if no url is present
	 */
	public String getUrl() {
	    return mUrl;
	}

	/**
	 * Get the id.
	 *
	 * @return the id
	 */
	public String getId() {
	    return mId;
	}

	/**
	 * Set the id.
	 *
	 * @param id the id
	 */
	public void setId(String id) {
	    this.mId = id;
	}

	/**
	 * Set the MIME type of the avatar.
	 *
	 * @param type the type
	 */
	public void setType(String type) {
	    this.mType = type;
	}

	/**
	 * Get the MIME type of the avatar.
	 *
	 * @return the type, null if no type is present
	 */
	public String getType() {
	    return mType;
	}

	/**
	 * Return this information as an xml element.
	 *
	 * @return an xml element representing this information
	 */
	public String toXML() {
	    StringBuilder builder = new StringBuilder("<info ");
	    builder.append("id=\"" + mId + "\"");
	    builder.append(" type=\"" + mType + "\"");
	    builder.append(" bytes=\"" + mBytes + "\"");

	    if (mHeight > 0)
		builder.append(" height=\"" + mHeight + "\"");
	    if (mWidth > 0)
		builder.append(" width=\"" + mWidth + "\"");
	    if (mUrl != null)
		builder.append(" url=\"" + mUrl + "\"");
	    builder.append(" />");
	    return builder.toString();
	}
    }
}
