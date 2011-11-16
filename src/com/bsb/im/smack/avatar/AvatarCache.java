package com.bsb.im.smack.avatar;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for an AvatarCache.
 * This can be improved to a generic cache.
 *
 */
public interface AvatarCache {

    /**
     * Put some datas in cache.
     *
     * @param id the key id of the data
     * @param data the datato cache
     * @throws IOException if an IO error occurs while caching the data
     */
    void put(String id, byte[] data) throws IOException;

    /**
     * Put some datas in cache.
     *
     * @param id the key id of the data
     * @param data an InputStream to the data to cache
     * @throws IOException if an IO error occurs while caching the data
     */
    void put(String id, InputStream data) throws IOException;

    /**
     * Get some data from the cache.
     *
     * @param id the id of the data to get
     * @return the cached data
     * @throws IOException  if an IO error occurs while geting the data
     */
    byte[] get(String id) throws IOException;

    /**
     * Test if a data is in cache.
     *
     * @param id the id of the data
     * @return true if data is in cache false otherwise
     */
    boolean contains(String id);
}
