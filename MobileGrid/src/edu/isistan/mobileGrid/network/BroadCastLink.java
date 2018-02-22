package edu.isistan.mobileGrid.network;

import java.util.Set;

public class BroadCastLink extends Link {

	public BroadCastLink(int delay, int bandwidth, Set<Node> source,
			Set<Node> destinations) {
		super(delay, bandwidth, source, destinations);
	}
	
	@Override
	public Set<Node> getSources() {
		return NetworkModel.getModel().getNodes();
	}

	@Override
	public Set<Node> getDestinations() {
		return NetworkModel.getModel().getNodes();
	}

	@Override
	public boolean canSend(Node scr, Node dst) {
		return (scr.isOnline())&&(dst.isOnline());
	}
}
