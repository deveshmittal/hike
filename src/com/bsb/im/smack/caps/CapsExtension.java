package com.bsb.im.smack.caps;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * This extension represents a capability of XEP-0115.
 *
 */
public class CapsExtension implements PacketExtension {

    private String mVer;
    private String mHash;
    private String mNode;
    private String mExt;


    /**
     * Create a CapsExtension.
     *
     * @param hash The value of the hash attribute.
     * @param node the value of the node attribute
     * @param ver the value of the ver attribute.
     */
    public CapsExtension(final String hash, final String node, final String ver) {
	mHash = hash;
	mNode = node;
	mVer = ver;
    }

    /**
     * Get the ver attribute value.
     *
     * @return the value of the ver attribute.
     */
    public String getVer() {
	return mVer;
    }

    /**
     * Get the hash attribute value.
     *
     * @return the value of the hash attribute.
     */
    public String getHash() {
	return mHash;
    }

    /**
     * Get the node attribute value.
     *
     * @return the value of the node attribute.
     */
    public String getNode() {
	return mNode;
    }

    /**
     * Get the ext attribute value.
     *
     * @return the value of the ext attribute.
     */
    public String getExt() {
	return mExt;
    }

    /**
     * Set the hash attribute.
     *
     * @param hash the value of hash
     */
    public void setHash(String hash) {
	mHash = hash;
    }

    /**
     * Set the ver attribute.
     *
     * @param ver the value of ver
     */
    public void setVer(String ver) {
	mVer = ver;
    }

    /**
     * Set the node attribute.
     *
     * @param node the value of node
     */
    public void setNode(String node) {
	mNode = node;
    }

    /**
     * Set the ext attribute.
     *
     * @param ext the value of ext
     */
    public void setExt(String ext) {
	mExt = ext;
    }

    @Override
    public String getElementName() {
	return "c";
    }

    @Override
    public String getNamespace() {
	return "http://jabber.org/protocol/caps";
    }

    @Override
    public String toXML() {
	StringBuilder b = new StringBuilder("<");
	b.append(getElementName());
	b.append(" xmlns=\"").append(getNamespace()).append("\" ");
	if (mHash != null) {
	    b.append("hash=\"").append(mHash).append("\" ");
	}
	if (mNode != null)
	    b.append("node=\"").append(mNode).append("\" ");
	if (mVer != null)
	    b.append("ver=\"").append(mVer).append("\" ");
	if (mExt != null)
	    b.append("ext=\"").append(mExt).append("\" ");
	b.append("/>");
	return b.toString();
    }

}
