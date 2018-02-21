package edu.isistan.mobileGrid.network;

import java.util.Set;

/**
 * Base class for links representing a communication channel between two different sets of {@link Node}s.
 */
public abstract class Link {
	
	protected int delay;
	protected int bandwidth;
	protected Set<Node> source;
	protected Set<Node> destinations;
	
	/**
	 * 
	 * @param delay transmition delay in milliseconds
	 * @param bandwidth bandwidht in bits/second
	 * @param source source nodes
	 * @param destinations dest nodes
	 */
	public Link(int delay, int bandwidth, Set<Node> source,
			Set<Node> destinations) {
		super();
		this.delay = delay;
		this.bandwidth = bandwidth;
		this.source = source;
		this.destinations = destinations;
	}
	/**
	 * Get the src nodes
     *
	 * @return The source nodes.
	 */
	public Set<Node> getSources(){
		return this.source;
	}
	/**
	 * Get the destination nodes
     *
	 * @return The destination nodes.
	 */
	public Set<Node> getDestinations(){
		return this.destinations;
	}
	
	/**
	 * Determines if this link connect both nodes
     *
	 * @param scr The source node.
	 * @param dst The destination node.
     *
	 * @return True if a message can be successfully transmitted over this channel, false otherwise.
	 */
	public boolean canSend(Node scr, Node dst){
		return (scr.isOnline()) && (dst.isOnline()) &&
				this.getSources().contains(scr) &&
				this.getDestinations().contains(dst);
	}
	/**
	 * Get the transmission time in milliseconds
     *
	 * @param size bytes to be transmitted
     *
	 * @return The estimated time it will take to send the specified amount of bytes over this channel.
	 */
	public long getTransmissionTime(int size){
		return (long) ((size * 1000.0) / (this.bandwidth + this.delay));
	}
}
