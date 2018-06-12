package edu.isistan.simulator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Top class of a grid computing simulation. This class is in charge of holding the references to all {@link Entity}s and
 * {@link Event}s involved in a simulation. Once said objects have been loaded, the simulation can start.
 */
public class Simulation {

	/**
	 * Map of {@link Entity}s (e.g. nodes, devices, proxies) currently present in the simulation. Represented as a
	 * hash map data structure to allow O(1) access to the nodes by their name alone.
	 */
	private static Map<String, Entity> ENTITIES = new HashMap<>();

	/**
	 * Auxiliary data structure that maps entities' IDs with their name. This enables O(1) access to entities when
	 * the caller only knows its ID, but not its name.
	 */
	private static Map<Integer, String> ENTITIES_ID_NAME = new HashMap<>();

	/**
	 * The current time of the simulation in milliseconds. Has a value of -1 if the simulation has not yet started.
	 */
	private static long CURRENT_TIME = -1;

	/**
	 * Set of {@link Event}s to be dispatched by the simulation. Modelled as a TreeSet to ensure they are sorted by
	 * dispatch time when added to the data structure.
	 */
	private static TreeSet<Event> EVENTS = new TreeSet<>();
	
	/**
	 * Reset all variables for a new Simulation configuration
	 */
	public static void fullReset(){
		// Resets Simulation static parameters.
		ENTITIES = new HashMap<>();
		ENTITIES_ID_NAME = new HashMap<>();
		CURRENT_TIME = -1;
		EVENTS = new TreeSet<>();

		// Resets Event static parameters.
		Event.reset();

		// Resets Entity static parameters.
		Entity.reset();
	}

	/**
	 * Gets the current time.
	 *
	 * @return The current time.
	 */
	public static long getTime() {
		return CURRENT_TIME;
	}
	
	/**
	 * Adds an entity with a unique name.
	 *
	 * @param entity The entity to be added.
	 * @throws IllegalArgumentException if the entity's name has already been added to the list.
	 */
	public static void addEntity(Entity entity) throws IllegalArgumentException{
		if(!ENTITIES.containsKey(entity.getName())){
			ENTITIES.put(entity.getName(), entity);
			ENTITIES_ID_NAME.put(entity.getId(), entity.getName());
		} else {
			throw new IllegalArgumentException("There is a repeated entity name: "+entity.getName());
		}
	}
	
	/**
	 * Gets an entity by its name, or null if no such entity exists.
	 *
	 * @param name The name of the entity.
	 * @return An entity with the given name, or null if said entity does not exist.
	 */
	public static Entity getEntity(String name){
		return ENTITIES.get(name);
	}

	/**
	 * Gets an entity by its id.
	 *
	 * @param id The id of the entity.
	 * @return An entity with the given id, or null if no entity with the given id exists.
	 */
	private static Entity getEntity(int id){
		String name = ENTITIES_ID_NAME.get(id);
		if (name != null) {
			return ENTITIES.get(name);
		}

		return null;
	}
	
	/**
	 * Adds a new event.
	 *
	 * @param event The event to add.
	 * @throws IllegalArgumentException if the event's timestamp is less than the current time in the simulation.
	 */
	public static void addEvent(Event event) throws IllegalArgumentException{
		if(event.getTime() >= CURRENT_TIME){
			EVENTS.add(event);
		} else {
			throw new IllegalArgumentException("Event with previous time: "+event.getTime()+" Current "+CURRENT_TIME);
		}
	}
	
	/**
	 * Removes an event.
	 *
	 * @param event The event to remove.
     * @return {@code true} if the given event was found in the event list.
	 */
	public static boolean removeEvent(Event event){
		return EVENTS.remove(event);
	}

    /**
     * Updates the timestamp for an event. This operation may break the sorted tree set if the event is updated
     * when already inside the tree. To prevent this from happening, the event is first removed from the tree set,
     * then updated, and then re-inserted.
     *
     * @param event The event to update.
     * @param time The new timestamp to set.
     */
	public static void updateEventTime(Event event, long time) {
        boolean eventRemoved = Simulation.removeEvent(event);
        event.modifyTime(time);
        if (eventRemoved) {
            Simulation.addEvent(event);
        }
    }

	/**
	 * Gets the number of events available in the event list.
	 *
	 * @return The number of events in the event list.
	 */
	public static int getEventSize(){
		return EVENTS.size();
	}

	/**
	 * Runs the simulation. Dispatches every event in the list sorted by its timestamp. Every event is either dispatched
	 * to a single given target, or broadcast to all nodes in the simulation. The simulation ends once all events have
	 * been taken cared of.
	 */
	public static void runSimulation(){
		long executionCount = 0;
		long start = System.currentTimeMillis();
		while(!EVENTS.isEmpty()){
			executionCount++;
			Event event = EVENTS.pollFirst(); // Gets and removes the event with the lowest timestamp in the list.
			CURRENT_TIME = event.getTime();
			// If the event's target is the BROADCAST flag, the event is sent to all nodes in the list (with the
            // exception of the sender); otherwise, the event is sent only to the node whose ID matches the event's target.
			if(event.getTargetId() == Event.BROADCAST) {
                for (Entity entity : ENTITIES.values()) {
                    if (entity.getId() != event.getSourceId()) {
                        entity.receiveEvent(event);
                    }
                }
            } else {
                getEntity(event.getTargetId()).receiveEvent(event);
            }
		}
		Logger.println("The simulator has executed " + executionCount + " events in " +
                (System.currentTimeMillis() - start) + " milliseconds");
	}
}
