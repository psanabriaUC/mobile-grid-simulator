package edu.isistan.mobileGrid.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.simulator.Entity;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Simulation;

/**
 * A simple network concrete implementation in which all links between {@link Entity}s are 100% reliable.
 * By default, entities added to this network are isolated from one another. Communication is rendered possible
 * only if a {@link Link} is added between them.
 */
public class SimpleNetworkModel extends NetworkModel {
	
	private Set<Node> nodes = new HashSet<Node>();
	private Map<Node,Map<Node,Link>> links = new HashMap<Node, Map<Node,Link>>();
	private Link defaultLink = new NullLink();

    /**
     * Stores the timestamp at which the different entities first sent a message during the simulation.
     */
	public final HashMap<Node, Long> firstTransferringTimes = new HashMap<Node, Long>();

    /**
     * Stores the timestamp at which the different entities last sent a message during the simulation.
     */
	public final HashMap<Node, Long> lastTransferringTimes = new HashMap<Node, Long>();

	@Override
    public <T> long send(Node scr, Node dst, int id, int length, T data, int offset, boolean lastMessage) {
        Message<T> message = new Message<>(id, scr, dst, data, length, offset, lastMessage);

        Link link = getLink(scr, dst);
        if(link.canSend(scr, dst)) {
            long currentTime = Simulation.getTime();
            long estimatedMessageReceivedTime = currentTime + link.getTransmissionTime(length);

            // Updates first and last message sent timestamps. For post-simulation validation purposes only.
            if(!firstTransferringTimes.containsKey(dst)) {
                firstTransferringTimes.put(dst, currentTime);
            }
            lastTransferringTimes.put(scr, estimatedMessageReceivedTime);

            // Notifies sender that it will start sending data.
            scr.startTransfer(dst, id, data);

            // Notifies receiver that it will start receiving data.
            dst.incomingData(scr, id);

            Simulation.addEvent(Event.createEvent(Event.NO_SOURCE, estimatedMessageReceivedTime,
                    this.getNetworkDelayEntityId(), 0, message));
            return estimatedMessageReceivedTime;
        } else {
            scr.fail(message);
        }
        return 0;
    }

	private Link getLink(Node scr, Node dst) {
		Link result = this.defaultLink;
		Map<Node, Link> map = this.links.get(scr);
		if(map != null && map.containsKey(dst)) {
			result = map.get(dst);
		}
		return result;
	}

	@Override
	public void addNewNode(Node n) {
		this.nodes.add(n);
	}
	
	@Override
	public void addNewLink(Link link) {
		for(Node scr: link.getSources()) {
			Map<Node,Link> map = this.links.get(scr);
			if(map == null) {
				map = new HashMap<Node, Link>();
				this.links.put(scr, map);
			}
			for(Node dst: link.getDestinations())
				map.put(dst, link);
		}
	}

	@Override
	public void removeNode(Node n) {
		this.nodes.remove(n);
		this.links.remove(n);
		for(Node key:this.links.keySet()){
			this.links.get(key).remove(n);
			if(this.links.get(key).isEmpty())this.links.remove(key);
		}
	}

	@Override
	public void removeLink(Link link) {
		for(Node scr: link.getSources()){
			Map<Node,Link> map = this.links.get(scr);
			if (map != null){
				for (Node dst: link.getDestinations()) {
					map.remove(dst);
				}
				if (map.isEmpty()) {
					this.links.remove(scr);
				}
			}
		}
	}

	@Override
	public Set<Node> getNodes() {
		return this.nodes;
	}

	public Link getDefaultLink() {
		return defaultLink;
	}

	public void setDefaultLink(Link defaultLink) {
		this.defaultLink = defaultLink;
	}

	@Override
	public long getTransmissionTime(Node scr, Node dst,int messageSize) {
		Link link = getLink(scr, dst);
		return link.getTransmissionTime(messageSize);
	}

}
