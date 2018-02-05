package edu.isistan.mobileGrid.network;

import java.util.Set;

public class Link {
	
	protected int delay;
	protected int bandwidth;
	protected Set<Node> source;
	protected Set<Node> destinations;
	
	/**
	 * 
	 * @param delay transmition delay in milliseconds
	 * @param bandwight bandwidht in bits/second
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
	 * @return
	 */
	public Set<Node> getSources(){
		return this.source;
	}
	/**
	 * Get the destination nodes
	 * @return
	 */
	public Set<Node> getDestinations(){
		return this.destinations;
	}
	
	/**
	 * Determine if this link connect both nodes
	 * @param scr
	 * @param dst
	 * @return
	 */
	public boolean canSend(Node scr,Node dst){
		return (scr.isOnline())&&(dst.isOnline())&&
				this.getSources().contains(scr)&&
				this.getDestinations().contains(dst);
	}
	/**
	 * Get the transmission time in milliseconds
	 * @param size bits to be trasmitted
	 * @return
	 */
	public long getTransmissionTime(int size){
		double s=size;
		double bw=this.bandwidth;
		double d=this.delay;
		return ((long)(s*1000.0/bw+d));
	}
}
