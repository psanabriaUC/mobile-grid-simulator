package edu.isistan.mobileGrid.network;


public interface Node {

    /**
     * Gets the id of the node.
     *
     * @return The id of the node.
     */
    public int getId();

	/**
	 * Starts a transfer
	 * @param scr
	 * @param id
	 */
	public void incomingData(Node scr, int id);
	
	/**
	 * Receive a job or update message from other node.
	 *
	 * @param message
	 */
	public void onMessageReceived(Message<?> message);
	
	/**
	 * ACK notification to the sender
	 *
	 * @param message
	 */
	public void onMessageSentAck(Message message);

	/**
	 * Fail notification to the sender
     *
	 * @param message The message that failed to be received.
	 */
	public void fail(Message message);
	
	public boolean isOnline();
	
	/**
	 * Notifies a transfer starting
     *
	 * @param dst
	 * @param id
	 * @param data
	 */
	public void startTransfer(Node dst, int id, Object data);

	/**
	 * Notifies transfer error
     *
	 * @param scr
	 * @param id
	 */
	public void failReception(Node scr, int id);
	
	/**Indicates the type of node according to its source of power*/
	public boolean runsOnBattery();
	
	public boolean isSending();
	
	public boolean isReceiving();
}
