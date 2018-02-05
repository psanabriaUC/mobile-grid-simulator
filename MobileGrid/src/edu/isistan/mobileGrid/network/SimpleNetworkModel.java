package edu.isistan.mobileGrid.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import edu.isistan.simulator.Event;
import edu.isistan.simulator.Simulation;

public class SimpleNetworkModel extends NetworkModel {
	
	private Set<Node> nodes=new HashSet<Node>();
	private Map<Node,Map<Node,Link>> links=new HashMap<Node, Map<Node,Link>>();
	private Link defaultLink=new NullLink();
	
	public HashMap<Node, Long> firstTransferingTimes = new HashMap<Node, Long>();
	public HashMap<Node, Long> lastTransferingTimes = new HashMap<Node, Long>();

	@Override
	public long send(Node scr, Node dst, int id, int lenght, Object data) {
		Link l=getLink(scr, dst);
		if(l.canSend(scr, dst)){
			scr.startTransfer(dst, id, data);
			dst.incomingData(scr, id);
			long simulationTime = Simulation.getTime();
			long time = simulationTime +l.getTransmissionTime(lenght);
			
			if(!firstTransferingTimes.containsKey(dst))
				firstTransferingTimes.put(dst, simulationTime);
			lastTransferingTimes.put(scr, time);
			
			Message m=new Message(id, scr, dst, data);
			m.setAttribute(Message.SIZE, String.valueOf(lenght));
			Simulation.addEvent(Event.createEvent(Event.NO_SOURCE, time,this.entityId, 0, m));
			return time;
		} else 
			scr.fail(id);
		return 0;
	}

	private Link getLink(Node scr, Node dst) {
		Link result=this.defaultLink;
		Map<Node,Link> m=this.links.get(scr);
		if(m!=null)
			if(m.containsKey(dst))
				result=m.get(dst);
		return result;
	}

	@Override
	public void addNewNode(Node n) {
		this.nodes.add(n);
	}
	
	@Override
	public void addNewLink(Link l) {
		for(Node scr:l.getSources()){
			Map<Node,Link> map=this.links.get(scr);
			if(map==null){
				map=new HashMap<Node, Link>();
				this.links.put(scr, map);
			}
			for(Node dst:l.getDestinations())
				map.put(dst, l);
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
	public void removeLink(Link l) {
		for(Node scr:l.getSources()){
			Map<Node,Link> map=this.links.get(scr);
			if(map!=null){
				for(Node dst:l.getDestinations())
					map.remove(dst);
				if(map.isEmpty()) this.links.remove(scr);
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
		Link l=getLink(scr, dst);
		return l.getTransmissionTime(messageSize);
	}

}
