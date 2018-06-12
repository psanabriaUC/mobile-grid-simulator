package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.simulator.Entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Encapsulation of a message that needs to be transferred through a limited-bandwidth channel. This class holds
 * information about the receiver, the payload of the message that needs to be sent, and the progress of the transfer assuming
 * the information is split into several packages.
 */
public class TransferInfo<T> implements Comparable<TransferInfo<T>>
{
    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_HIGH = 1;

    private static final AtomicInteger NEXT = new AtomicInteger(0);

    private Node destination;
	private T data;
	private long messagesCount;
	private int currentIndex;
	private int lastMessageSize;

	private int id;
	private int priority;
	
	TransferInfo(Node destination, T data, long messagesCount, int lastMessageSize) {
		this.destination = destination;
		this.data = data;
		this.messagesCount = messagesCount;
		this.currentIndex = 0;
		this.lastMessageSize = lastMessageSize;

		id = NEXT.incrementAndGet();
		priority = PRIORITY_DEFAULT;
	}

    public int getId() {
        return id;
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

	public boolean allMessagesSent() {
	    return currentIndex >= messagesCount;
    }

	public void increaseIndex() {
	    currentIndex++;
    }

    public void setPriority(int priority) {
        this.priority = priority;
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

    @Override
    public int compareTo(TransferInfo<T> other) {
        // Careful with the order of parameters: for priority, higher priority takes precedence; for id,
        // lower id takes precedence. This implementation is made so that the object with the LOWER value should
        // come first.
        if (this.priority != other.priority) {
            return other.priority - this.priority;
        } else {
            return this.id - other.id;
        }
    }
}
