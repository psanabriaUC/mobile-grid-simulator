package edu.isistan.simulator;

import java.util.concurrent.atomic.AtomicLong;

public class Event implements Comparable<Event>{
	
	//private static long NEXT_ID=0;
	private static AtomicLong NEXT_ID=new AtomicLong(0);
	public static final int BROADCAST=-1;
	public static final int NO_SOURCE=-1;
	
	/**
	 * Creates an event setting all it parameters
	 * @param srcId source, use Event.NO_SOURCE to indicate that the event has no source
	 * @param trgId target, use Event.BROADCAST to indicate that is a broadcast event
	 * @param eventType indicates the event type
	 * @param data extra information, might be null
	 * @return a new event
	 */
	public static Event createEvent(int srcId, long time, int trgId, int eventType, Object data){
		//long eventId=NEXT_ID;
		//NEXT_ID++;
		long eventId=NEXT_ID.getAndIncrement();
		return new Event(eventId, time, srcId, trgId, eventType, data);	
	}
	
	public static void reset(){
		NEXT_ID.set(0);
	}
	
	private long eventId;
	private long time;
	private int srcId;
	private int trgId;
	private int eventType;
	private Object data;
	
	private Event(long eventId, long time, int srcId, int trgId, int eventType, Object data) {
		super();
		this.eventId = eventId;
		this.time=time;
		this.srcId = srcId;
		this.trgId = trgId;
		this.eventType = eventType;
		this.data = data;
	}

	@Override
	public int compareTo(Event o) {
		long result=this.time-o.time;
		if(result<0)
			return -1;
		if (result>0)
			return 1;
		long id=this.eventId-o.eventId;
		if(id<0)
			return -1;
		if (id>0)
			return 1;
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		return ((obj instanceof Event)&&(((Event)obj).eventId==this.eventId));
	}

	public long getEventId() {
		return eventId;
	}

	public long getTime() {
		return time;
	}

	public int getSrcId() {
		return srcId;
	}

	public int getTrgId() {
		return trgId;
	}

	public int getEventType() {
		return eventType;
	}

	public Object getData() {
		return data;
	}
	
	public void modifyTime(long newTime){
		time=newTime;
	}
	
}
