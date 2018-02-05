package edu.isistan.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class Simulation {
	
	private static List<Entity> ENTITIES=new ArrayList<Entity>();
	private static long CURRENT_TIME=-1;
	private static SortedSet<Event> EVENTS=new TreeSet<Event>();
	private static Map<String, Integer> ENTITY_ID=new HashMap<String, Integer>();
	
	/**
	 * Reset all variables for a new Simulation configuration
	 */
	public static void fullReset(){
		Simulation.reset();
		Event.reset();
	}
	/**
	 * Cleans the singleton
	 */
	public static void reset(){
		ENTITIES=new ArrayList<Entity>();
		CURRENT_TIME=-1;
		EVENTS=new TreeSet<Event>();
		ENTITY_ID=new HashMap<String, Integer>();
	}
	/**
	 * Gets the current time
	 * @return
	 */
	public static long getTime() {
		return CURRENT_TIME;
	}
	
	/**
	 * Adds an entity with an unique name
	 * @param e
	 * @throws IllegalArgumentException if the name is not unique
	 */
	public static void addEntity(Entity e) throws IllegalArgumentException{
		if(!ENTITY_ID.containsKey(e.getName())){
			int id=ENTITIES.size();
			ENTITIES.add(e);
			e.setId(id);
			ENTITY_ID.put(e.getName(), id);
		} else {
			throw new IllegalArgumentException("There is an repeated entity name: "+e.getName());
		}
	}
	
	/**
	 * Gets the id of an entity by the name
	 * @param name
	 * @return
	 */
	public static int getEntityId(String name){
		return ENTITY_ID.get(name);
	}
	
	/**
	 * Gets the entity by its name
	 * @param name
	 * @return
	 */
	public static Entity getEntity(String name){
		return ENTITIES.get(ENTITY_ID.get(name));
	}
	
	/**
	 * Gets the entity by its id
	 * @param name
	 * @return
	 */
	public static Entity getEntity(int id){
		return ENTITIES.get(id);
	}
	
	/**
	 * Adds a new event
	 * @param e
	 * @throws IllegalArgumentException
	 */
	public static void addEvent(Event e) throws IllegalArgumentException{
		if(e.getTime()>=CURRENT_TIME){
			EVENTS.add(e);
		} else {
			throw new IllegalArgumentException("Event with previous time: "+e.getTime()+" Current "+CURRENT_TIME);
		}
	}
	
	/**
	 * Removes an event
	 * @param e
	 */
	public static void removeEvent(Event e){
		EVENTS.remove(e);
	}
	
	public static int getEventSize(){
		return EVENTS.size();
	}
	
	public static void runSimulation(){
		long executionCount=0;
		long start=System.currentTimeMillis();
		while(!EVENTS.isEmpty()){
			executionCount++;
			Event e=EVENTS.first();
			EVENTS.remove(e);
			CURRENT_TIME=e.getTime();
			if(e.getTrgId()==Event.BROADCAST)
				for(Entity ent:ENTITIES)
					ent.receiveEvent(e);
			else
				getEntity(e.getTrgId()).receiveEvent(e);
		}
		Logger.println("The simulator has executed "+executionCount+" events in "+(System.currentTimeMillis()-start)+" miliseconds");
	}
}
