package com.bsb.im.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;


import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


import org.jivesoftware.smack.Connection;

import com.bsb.im.smack.avatar.AvatarCache;
import com.bsb.im.smack.avatar.AvatarManager;
import com.bsb.im.smack.avatar.AvatarMetadataExtension;
import com.bsb.im.smack.pep.PepSubManager;

/**
 * An AvatarManager for Beem.
 * It allows to publish avatar on the Android platform.
 */
public class BeemAvatarManager extends AvatarManager {
    private static final String TAG = BeemAvatarManager.class.getSimpleName();
    private static final int JPEG_QUALITY = 100;

    private Context mContext;

    /**
     * Create a BeemAvatarManager.
     *
     * @param ctx the Android context
     * @param con the connection
     * @param pepMgr the PepSubManager of the connection
     * @param cache the cache which will store the avatars
     * @param autoDownload tre to enable auto download of avatars
     */
    public BeemAvatarManager(final Context ctx, final Connection con, final PepSubManager pepMgr,
	    final AvatarCache cache, final boolean autoDownload) {
	super(con, pepMgr, cache, autoDownload);
	mContext = ctx;
    }

    /**
     * Publish an avatar.
     *
     * @param avatarUri the uri of the avatar
     * @return true if the avatar was successfully published
     */
    public boolean publishAvatar(Uri avatarUri) {
	try {
	    Bitmap bmp = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), avatarUri);
	    return publishAvatar(bmp);
	} catch (IOException e) {
	    Log.d(TAG, "Error while publishing avatar " + avatarUri, e);
	}
	return false;
    }

    /**
     * Publish an avatar.
     * This will send the XMPP stanza to enable the publication of an avatar.
     *
     * @param bitmap the avatar to publish
     * @return true on success false otherwise
     */
    private boolean publishAvatar(Bitmap bitmap) {
	//TODO use the metadata available in the mediastore
	AvatarMetadataExtension meta = new AvatarMetadataExtension();
	// Probably a bug on prosody but only the last data sent is kept
	// and in beem we retrieve the first info
	AvatarMetadataExtension.Info jpeg = publishBitmap(bitmap, Bitmap.CompressFormat.JPEG, JPEG_QUALITY);
	// The png format is mandatory for interoperability
	AvatarMetadataExtension.Info png = publishBitmap(bitmap, Bitmap.CompressFormat.PNG, JPEG_QUALITY);
	if (png == null)
	    return false;
	meta.addInfo(png);
	if (jpeg != null)
	    meta.addInfo(jpeg);
	publishAvatarMetaData(png.getId(), meta);
	return true;
    }

    /**
     * Send this bitmap to the avatar data node of the pep server.
     *
     * @param bmp the avatar bitmap
     * @param format the image format to publish this data
     * @param quality the compression quality use for JPEG compression
     * @return the resulting info associate with this bitmap. null if the operation failed
     */
    private AvatarMetadataExtension.Info publishBitmap(Bitmap bmp, Bitmap.CompressFormat format, int quality) {
	try {
	    byte[] data = getBitmapByte(bmp, format, quality);
	    String dataid = getAvatarId(data);
	    if (!publishAvatarData(data))
		return null;
	    String mimetype = "image/png";
	    if (Bitmap.CompressFormat.JPEG == format)
		mimetype = "image/jpeg";
	    AvatarMetadataExtension.Info info = new AvatarMetadataExtension.Info(dataid, mimetype, data.length);
	    info.setHeight(bmp.getHeight());
	    info.setWidth(bmp.getWidth());
	    return info;
	} catch (NoSuchAlgorithmException ex) {
	    return null;
	}
    }

    /**
     * Convert the bitmap to a byte array.
     *
     * @param bitmap the avatar bitmap
     * @param format the resulting image format
     * @param quality the compression quality use for JPEG compression
     * @return the bitmap data or a array of 0 element on error
     */
    private byte[] getBitmapByte(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	if (bitmap.compress(format, quality, bos))
	    return bos.toByteArray();
	else
	    return new byte[0];
    }

}
