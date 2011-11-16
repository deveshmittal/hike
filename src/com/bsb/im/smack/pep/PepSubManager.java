package com.bsb.im.smack.pep;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.EventElementType;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.LeafNode;

/**
 * Little extension of {@link PubSubManager} which allows to add {@link PEPListener}.
 *
 */
public class PepSubManager /*extends PubSubManager*/ {
    private List<PEPListener> mPepListeners = new ArrayList<PEPListener>();
    private PacketFilter mPacketFilter = new PacketExtensionFilter("event", "http://jabber.org/protocol/pubsub#event");

    /**
     * Create a PepSubManager.
     *
     * @param connection the connection
     */
    public PepSubManager(final Connection connection) {
	//super(connection);
	init(connection);
    }

    /**
     * Create a PepSubManager associated to the specified connection where the pubsub
     * requests require a specific to address for packets.
     *
     * @param connection the connection
     * @param toAddress The pubsub specific to address (required for some servers)
     */
    public PepSubManager(final Connection connection, final String toAddress) {
	//super(connection, toAddress);
	init(connection);
    }

    /**
     * Add a listener to PEP event.
     *
     * @param listener the listener
     */
    public void addPEPListener(PEPListener listener) {
	if (!mPepListeners.contains(listener))
	    mPepListeners.add(listener);
    }

    /**
     * Remove a listener to PEP event.
     *
     * @param listener the listener
     */
    public void removePEPListener(PEPListener listener) {
	mPepListeners.remove(listener);
    }

    /**
     * Get a PepNode.
     * This node is obtain without checking its existence as PEP should auto create it.
     *
     * @param nodeName the node name
     * @return the node
     */
    
    public LeafNode getPEPNode(String nodeName) {
	//LeafNode node = new LeafNode(con, nodeName);
	//node.setTo(to);
	return null;//node;
    }
    

    /**
     * Initialize the PepSubManager.
     *
     * @param con the connection
     */
    private void init(Connection con) {
	PacketListener packetListener = new PacketListener() {

	    @Override
	    public void processPacket(Packet packet) {
		EventElement e = (EventElement) packet.getExtension("event", "http://jabber.org/protocol/pubsub#event");
		if (e.getEventType() != EventElementType.items)
		    return;
		ItemsExtension it = (ItemsExtension) e.getEvent();
		if (it.getItemsElementType() != ItemsExtension.ItemsElementType.items)
		    return;
		List<Item> items = (List<Item>) it.getItems();
		firePEPListeners(packet.getFrom(), it.getNode(), items);
	    }
	};
	con.addPacketListener(packetListener, mPacketFilter);
    }

    /**
     * Fire the PEP listeners.
     *
     * @param from the JID of the user who send the event
     * @param node the node of the items in the event
     * @param items the different items of the event
     */
    private void firePEPListeners(String from, String node, List<Item> items) {
	for (PEPListener listener : mPepListeners) {
	    listener.eventReceived(from, node, items);
	}
    }
}
