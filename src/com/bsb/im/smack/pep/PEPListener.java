package com.bsb.im.smack.pep;

import org.jivesoftware.smackx.pubsub.Item;
import java.util.List;

/**
 * A listener that is fired anytime a PEP event message is received.
 */
public interface PEPListener {

    /**
     *  Called when PEP events are received.
     *
     * @param from the JID of the user who send the event
     * @param node the node of the items in the event
     * @param items the different items of the event
     */
    void eventReceived(String from, String node, List<Item> items);

}
