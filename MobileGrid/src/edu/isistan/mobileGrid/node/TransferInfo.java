package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.simulator.Entity;

/**
 * Encapsulation of a message that needs to be transferred through a limited-bandwidth channel. This class holds
 * information about the receiver, the payload of the message that needs to be sent, and the progress of the transfer assuming
 * the information is split into several packages.
 */
public class TransferInfo<T>
{
    private Node destination;
	private T data;
	private long messagesCount;
	private int currentIndex;
	private int lastMessageSize;
	
	TransferInfo(Node destination, T data, long messagesCount, int lastMessageSize) {
		this.destination = destination;
		this.data = data;
		this.messagesCount = messagesCount;
		this.currentIndex = 0;
		this.lastMessageSize = lastMessageSize;
	}

    public Node getDestination() {
        return destination;
    }

    public T getData() {
        return data;
    }

    public long getMessagesCount() {
        return messagesCount;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getLastMessageSize() {
        return lastMessageSize;
    }

    public boolean isLastMessage() {
		return currentIndex == messagesCount - 1;
	}

	public void increaseIndex() {
	    currentIndex++;
    }

    /**
     * Utility function to calculate the message size for a data package given a fixed buffer size.
     *
     * @param bufferSize The sender's max buffer size. This value should always be the same for one specific device.
     * @return The buffer size in case this is not the last message, or the value specified by
     * {@link TransferInfo#lastMessageSize} if this is the last message.
     */
    public int getMessageSize(int bufferSize) {
	    if (isLastMessage()) {
	        return lastMessageSize;
        } else {
	        return bufferSize;
        }
    }
}
