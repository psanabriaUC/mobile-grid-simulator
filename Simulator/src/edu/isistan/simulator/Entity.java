package edu.isistan.simulator;

public abstract class Entity {
	
	private String name;
	private int id;
	private boolean active=true;
	
	public Entity(String name) {
		super();
		this.name = name;
	}

	public void receiveEvent(Event e){
		if(isActive())
			this.processEvent(e);
	}
	/**
	 * This method should be extended for defining a new entity type
	 * @param e
	 */
	public abstract void processEvent(Event e);
	
	/**
	 * Only for the Simulator ** DO NOT CALL EVER!!!***
	 * @return
	 */
	int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public String toString() {
		return this.getName();
	}
	
	
}
