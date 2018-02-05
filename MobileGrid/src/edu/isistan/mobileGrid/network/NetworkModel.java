package edu.isistan.mobileGrid.network;

import java.util.HashMap;
import java.util.Set;

import edu.isistan.simulator.Entity;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Simulation;

public abstract class NetworkModel {
		
	public static final String NETWORK_ENTITY_NAME="network";
	private static NetworkModel MODEL;
	public static double AckMessageSizeInBytes = 0; //2346; //http://stackoverflow.com/questions/5543326/what-is-the-total-length-of-pure-tcp-ack-sent-over-ethernet
	
	public abstract long getTransmissionTime(Node scr, Node dst, int messageSize);
	
	public static void setModel(NetworkModel model){
		MODEL=model;
	}
	
	public static NetworkModel getModel(){
		if(MODEL==null)
			MODEL=new SimpleNetworkModel();
		return MODEL;
	}
	
	protected NetworkDelayEntity networkDelayEntity;
	protected int entityId;
	
	/**
	 * Might Cause IllegalArgumentException if other model was already created
	 * and the simulation was not reseted
	 */
	protected NetworkModel(){
		super();
		this.networkDelayEntity=new NetworkDelayEntity(NETWORK_ENTITY_NAME);
		Simulation.addEntity(this.networkDelayEntity);
		this.entityId=Simulation.getEntityId(NETWORK_ENTITY_NAME);
	}

	/**
	 * Schedule a message to send
	 * @param scr source
	 * @param dst destiny
	 * @param id message id (must be unique for each scr)
	 * @param lenght 
	 * @param data
	 */
	public abstract long send(Node scr, Node dst, int id,int lenght, Object data);
	/**
	 * Adds a new node
	 * @param n
	 */
	public abstract void addNewNode(Node n);
	/**
	 * Adds new link
	 * @param l
	 */
	public abstract void addNewLink(Link l);
	/**
	 * Removes a node and all its associated links
	 * @param n
	 */
	public abstract void removeNode(Node n);
	/**
	 * Removes a link
	 * @param l
	 */
	public abstract void removeLink(Link l);
	/**
	 * Gets the nodes in the network
	 * @return
	 */
	public abstract Set<Node> getNodes();
	
	public double getAckMessageSizeInBytes() {
		return AckMessageSizeInBytes;
	}

	public void setAckMessageSizeInBytes(double ackMessageSizeInBytes) {
		AckMessageSizeInBytes = ackMessageSizeInBytes;
	}

	protected class NetworkDelayEntity extends Entity{

		public NetworkDelayEntity(String name) {
			super(name);
		}

		@Override
		public void processEvent(Event e) {
			Message m=(Message) e.getData();
			if((m.getScr().isOnline())&&(m.getDst().isOnline())){
				m.dst.receive(m.scr, m.getId(), m);
				m.scr.success(m.getId());
			} else {
				m.getScr().fail(m.getId());
				m.dst.failReception(m.getScr(),m.getId());
			}
		}
		
	}
	
	
	public class Message{
		public static final String STEAL_REQUEST_TYPE = "STEAL_REQUEST_TYPE";
		public static final String TYPE = "TYPE";
		public static final String SIZE = "SIZE";
		
		public static final int STEAL_MSG_SIZE = 2346+20; //http://stackoverflow.com/questions/5543326/what-is-the-total-length-of-pure-tcp-ack-sent-over-ethernet
		//20 bytes are integrated by 16 bytes corresponding to a IPv6 address of the stealer node + 4 bytes corresponding to a integer that indicates the quantity of jobs to be stolen
		
		private int id;
		private Node scr;
		private Node dst;
		private Object data;		
		private HashMap<String,Object> attributes;
		
		public Message(int id, Node scr, Node dst, Object data) {
			super();
			this.id = id;
			this.scr = scr;
			this.dst = dst;
			this.data = data;			
		}
		
		public void setAttribute(String name, String value){
			if (attributes == null) attributes = new HashMap<String,Object>();
			attributes.put(name, value);
		}
		
		public Object getAttribute(String name){
			return attributes!=null && attributes.containsKey(name)?attributes.get(name):null;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Node getScr() {
			return scr;
		}

		public void setScr(Node scr) {
			this.scr = scr;
		}

		public Node getDst() {
			return dst;
		}

		public void setDst(Node dst) {
			this.dst = dst;
		}

		public Object getData() {
			return data;
		}

		public void setData(Object data) {
			this.data = data;
		}
						
	}
	
}
