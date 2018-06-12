package edu.isistan.mobileGrid.network;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a message that needs to be transferred between devices.
 */
public class Message<T> {
    public static final int STEAL_MSG_SIZE = 2346 + 20; //http://stackoverflow.com/questions/5543326/what-is-the-total-length-of-pure-tcp-ack-sent-over-ethernet
    // 20 bytes are integrated by 16 bytes corresponding to a IPv6 address of the stealer node + 4 bytes corresponding to a integer that indicates the quantity of jobs to be stolen

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private int id;

    /**
     * The sender of this message.
     */
    private Node source;

    /**
     * The receiver of this message.
     */
    private Node destination;

    /**
     * The payload of this message.
     */
    private T data;

    /**
     * The total size of this message. Used for estimating transfer times.
     */
    private long messageSize;

    // Offset and HasNextMessage are used to simulate message fragmentation.

    /**
     * Indicates the offset of this data package with respect to the full message.
     */
    private int offset;

    /**
     * Flag to indicate if this is the last package of a message or not. If false, receivers should expect to get
     * in the future another message with a higher offset than this one.
     */
    private boolean lastMessage;


    /*
    public Message(int id, Node source, Node dst, T data, long messageSize) {
        this(id, source, dst, data, messageSize, 0, true);
    }
    */

    /**
     * Creates a new message to be transferred accross the network.
     *
     * @param id The ID of the message.
     * @param source The sender of the message.
     * @param dst The message's recipient.
     * @param data The message's payload.
     * @param messageSize The size of the message in bytes.
     * @param offset The offset of this message's payload. Only relevant when a large payload has to be fragmented accross
     *               multiple messages.
     * @param lastMessage A flag indicating whether this is the last fragment for a given message or not.
     */
    public Message(int id, Node source, Node dst, T data, long messageSize, int offset, boolean lastMessage) {
        super();
        this.id = id; //NEXT_ID.incrementAndGet();
        this.source = source;
        this.destination = dst;
        this.data = data;

        this.messageSize = messageSize;
        this.offset = offset;
        this.lastMessage = lastMessage;
    }

    // Getters and setters

    public int getId() {
        return id;
    }

    public Node getSource() {
        return source;
    }

    public void setSource(Node source) {
        this.source = source;
    }

    public Node getDestination() {
        return destination;
    }

    public void setDestination(Node dst) {
        this.destination = dst;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getMessageSize() {
        return messageSize;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isLastMessage() {
        return lastMessage;
    }
}
