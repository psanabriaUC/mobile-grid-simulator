package edu.isistan.mobileGrid.network;


public interface Node {

	/**
	 * Starts a transfer
	 * @param scr
	 * @param id
	 */
	public void incomingData(Node scr, int id);
	
	/**
	 * Receive a job or update message from other node
	 * @param scr
	 * @param id
	 * @param object
	 */
	public void receive(Node scr, int id, Object object);

	
	/**
	 * ACK notification to the sender
	 * @param id
	 */
	public void success(int id);
	/**
	 * Fail notification to the sender
	 * @param id
	 */
	public void fail(int id);
	
	public boolean isOnline();
	
	/**
	 * Notifies a transfer starting
	 * @param dst
	 * @param id
	 * @param data
	 */
	public void startTransfer(Node dst, int id, Object data);
	/**
	 * Notifies transfer error
	 * @param scr
	 * @param id
	 */
	public void failReception(Node scr, int id);
	
	/**Indicates the type of node according to its source of power*/
	public boolean runsOnBattery();
	
	public boolean isSending();
	
	public boolean isReceiving();
}
