package edu.isistan.seas.node;

import java.util.SortedSet;
import java.util.TreeSet;

import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.seas.util.NegativeComparator;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;
/**
 * This is a lineal discharge model. It is based on two profiles.
 * The first one represent the device discharge on standard use, while
 * the other represent the device use when it is in full use.
 * @author cuchillo
 *
 */
public class DefaultBatteryManager implements BatteryManager {
	
	private Device device;
	//hay que inicializarlos con un valor al principio
	private double lastCharge;
	private long lastMeasurement;
	private int reportedCharge;		
	private long batteryCapacityInJoules; //the battery capacity of a device expressed in Joules. Joules = w*h = w * 3600 sec
	private int initialSOC; //this represents the State Of Charge of the device when it join the grid
	
	
	//hay que inicializarlos con un valor al principio
	private double estimatedUpTime;
	private double measures=1;
	private double lastEventTime;
	private double lastReportedCharge;
	private long startTime;
	
	private Event lastAddedEvent; 
	
	private SortedSet<ProfileData>[] profiles;
	private int currentProfile;
	
	private DefaultExecutionManager seasEM;

	@SuppressWarnings("unchecked")
	public DefaultBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules){
		this.setBatteryCapacityInJoules(batteryCapacityInJoules);
		this.initialSOC=charge;
		this.lastCharge=charge;
		this.reportedCharge=charge;
		this.lastReportedCharge=charge;
		this.estimatedUpTime=estUptime;		
		this.profiles= (SortedSet<ProfileData>[])new SortedSet[prof];
		for(int i=0;i<prof;i++)
			this.profiles[i]=new TreeSet<ProfileData>(new NegativeComparator<ProfileData>());
	}
	/**
	 * This method is executed when a job start to execute
	 * or it finished. The idea is to switch between the two
	 * profiles by updating the next battery event.
	 */
	@Override
	public void onCPUProfileChange() {
		int newProf=this.seasEM.getActualCPUProfile();
		if(this.currentProfile==newProf) return;
		double lTime=this.lastMeasurement;
		double now=Simulation.getTime();
		//Calculates new charge
		double slope = this.profiles[this.currentProfile].first().getSlope();
		double newCharge=this.lastCharge+(now-lTime)* slope;
		
		if (newCharge > this.lastCharge)
			Logger.appendDebugInfo("on profile change new charge is bigger than last charge!"+newCharge+"\n");
		if(newCharge<=0.0){
			if (newCharge < 0.0)
				Logger.appendDebugInfo("on profile change charge become negative"+newCharge+"\n");
			this.device.onBatteryDepletion();
			return;
		}
		//Calculates new event time
		double nextChargeEvent	= this.moveToNext(newCharge,newProf);
		
		double nEventTime=now+(nextChargeEvent-newCharge)/ this.profiles[newProf].first().getSlope();
		//Update values and checks validity
		this.currentProfile=newProf;		
		
		this.lastCharge=newCharge;
		this.lastMeasurement=Simulation.getTime();
		
		//discomment for debugging
		//Logger.appendDebugInfo(this.device.getName()+";PRO;"+this.lastMeasurement+";"+(double)this.lastCharge+";"+lastAddedEvent.getEventId()+"\n");
		
		if(this.lastMeasurement>nEventTime) throw new IllegalStateException("Next event time is previous ("+nEventTime+") to current time ("+this.lastMeasurement+")");
		Simulation.removeEvent(this.lastAddedEvent);
		this.lastAddedEvent=Event.createEvent(Event.NO_SOURCE, (long) nEventTime, Simulation.getEntityId(this.device.getName()), Device.EVENT_TYPE_BATTERY_UPDATE,this.profiles[newProf].first().getToCharge());
		Simulation.addEvent(this.lastAddedEvent);
	}

	@Override
	public void onNetworkEnergyConsumption(double decreasingPercentageValue) {
			//Logger.logEnergy( "onNetworkEnergyConsumption","decreasingPercentageValue="+decreasingPercentageValue);
			
			double slope = this.profiles[this.currentProfile].first().getSlope();
			this.lastCharge = this.lastCharge+(Simulation.getTime()-this.lastMeasurement)* slope;	
			
			this.lastCharge -= decreasingPercentageValue;
			this.lastMeasurement = Simulation.getTime(); 
			//discomment for debugging
			//Logger.appendDebugInfo(this.device.getName()+";NET;"+this.lastMeasurement+";"+this.lastCharge);
					
			if (this.lastCharge<=0){
				this.device.onBatteryDepletion();
				return;
			}
			 
			int futureCharge = ((int)lastAddedEvent.getData());			
			int nextCharge=this.moveToNext(lastCharge, this.currentProfile);
			boolean skippedSample= futureCharge != nextCharge ? true : false;
			
			double	newTime = (double)this.lastMeasurement+(((double)nextCharge-this.lastCharge)/this.profiles[this.currentProfile].first().getSlope());
			if(newTime<this.lastMeasurement)  
				throw new IllegalStateException("Next event time is previous ("+newTime+") to current time ("+this.lastMeasurement+")");
						
			//with the energy consumption introduced by network, the lastAddedEvent is out of date.
			//There are two possible update operations. One involves get a new sample from the
			//current profile and calculate the time when this battery level will take place while the other
			//involves only the last operation. Both update operations needs that lastAddedEvent be removed and
			//added to the simulation events queue in order to be inserted in the correct place. 
			Simulation.removeEvent(lastAddedEvent);
			if (!skippedSample)
				lastAddedEvent.modifyTime((long)newTime);
			else
				lastAddedEvent = Event.createEvent(lastAddedEvent.getSrcId(),(long)newTime, lastAddedEvent.getTrgId(), lastAddedEvent.getEventType(), nextCharge);
			Simulation.addEvent(lastAddedEvent);
			
			//TODO: INCLUDE CALL TO this.updateEstimatedUptime();		
	}

	@Override
	public void onBatteryEvent(int level) {
		if(level != 0)
			level = level+1-1;
		Logger.logEnergy( "onBatteryEvent","level="+level);
		
		if(level<=0){
			lastCharge=0;
			this.lastMeasurement=Simulation.getTime();
			this.device.onBatteryDepletion();
			return;
		}
		
		this.lastCharge=level;		
		this.lastMeasurement=Simulation.getTime();
		this.reportedCharge=level;
		
		//discomment for debugging
		//Logger.appendDebugInfo(this.device.getName()+";BAT;"+this.lastMeasurement+";"+this.lastCharge+";"+lastAddedEvent.getEventId()+"\n");
		
		this.moveToNext(this.lastCharge, this.currentProfile);
		double nextEventCharge=this.profiles[this.currentProfile].first().getToCharge();
		
		/**Commented by Matias: The current and the next state of charge are joint with a line whose equation is
		 * (y - b) / a = x, where y is the next state of charge, b is the current charge and a is the slope of
		 * the line that join both state of charge. The equation is used to know the time when the next state 
		 * charge will occur. That time is added to the time when the current state of charge happened
		 * (lastMeasurement).**/
		double nTime=this.lastMeasurement+(nextEventCharge-this.lastCharge)/this.profiles[this.currentProfile].first().getSlope();
		
		if(nTime<this.lastMeasurement)  
			throw new IllegalStateException("Next event time is previous ("+nTime+") to current time ("+this.lastMeasurement+")");
		this.lastAddedEvent=Event.createEvent(Event.NO_SOURCE, (long) nTime, Simulation.getEntityId(this.device.getName()), Device.EVENT_TYPE_BATTERY_UPDATE,this.profiles[this.currentProfile].first().getToCharge());
		Simulation.addEvent(this.lastAddedEvent);
		this.updateEstimatedUptime();
	}
	

	@Override
	public int getCurrentBattery() {		
		return (int)this.lastCharge;
	}
	
	@Override
	public double getCurrentSOC(){
		//return this.lastCharge;
		double slope = this.profiles[this.currentProfile].first().getSlope();
		return this.lastCharge + (Simulation.getTime()-this.lastMeasurement) * slope;
	}
		
	@Override
	public long getEstimatedUptime() {
		return (long) (this.estimatedUpTime+this.startTime-Simulation.getTime());
	}

	public long getBatteryCapacityInJoules() {
		return batteryCapacityInJoules;
	}
	public void setBatteryCapacityInJoules(long batteryCapacityInJoules) {
		this.batteryCapacityInJoules = batteryCapacityInJoules;
	}
	@Override
	public void startWorking() {
		this.lastMeasurement=Simulation.getTime();
		this.lastEventTime=Simulation.getTime();		
		this.startTime=Simulation.getTime();
		double nextEventCharge=this.profiles[this.currentProfile].first().getToCharge();
		double nTime=this.lastMeasurement+(nextEventCharge-this.lastCharge)/this.profiles[this.currentProfile].first().getSlope();
		if(nTime<this.lastMeasurement)  throw new IllegalStateException("Next event time is previous ("+nTime+") to current time ("+this.lastMeasurement+")");
				
		
		this.lastAddedEvent=Event.createEvent(Event.NO_SOURCE, (long) nTime, Simulation.getEntityId(this.device.getName()), Device.EVENT_TYPE_BATTERY_UPDATE,this.profiles[this.currentProfile].first().getToCharge());
		Simulation.addEvent(this.lastAddedEvent);
		
		//debugging line
		//Logger.appendDebugInfo(this.device.getName()+";INI;"+this.lastMeasurement+";"+this.lastCharge+";futEvent:"+lastAddedEvent.getEventId()+"\n");
		
		SchedulerProxy.PROXY.addDevice(this.device);		
		Logger.logEntity(device, "Device started");
	}

	@Override
	public void shutdown() {		
		Simulation.removeEvent(lastAddedEvent);		
	}

	private int moveToNext(double newCharge, int prof) {
		while((this.profiles[prof].first().getToCharge()>=newCharge))
			this.profiles[prof].remove(this.profiles[prof].first());
		return this.profiles[prof].first().getToCharge();
	}
	
	private void updateEstimatedUptime() {
		double rc=this.reportedCharge;
		double now=Simulation.getTime();
		double ttd=(-rc)/((rc-this.lastReportedCharge)/(now-this.lastEventTime))+now;
		this.estimatedUpTime=(this.estimatedUpTime*this.measures+(ttd-this.startTime))/(this.measures+1);
		this.lastReportedCharge=rc;
		this.lastEventTime=now;
		this.measures++;
	}
	
	public void addProfileData(int prof,ProfileData dat){
		this.profiles[prof].add(dat);
	}

	public DefaultExecutionManager getSEASExecutionManager() {
		return seasEM;
	}

	public void setSEASExecutionManager(DefaultExecutionManager seasEM) {
		this.seasEM = seasEM;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}	
	
	@Override
	public long getStartTime() {
		return startTime;
	}
	
	@Override
	public int getInitialSOC() {
		return initialSOC;
	}
	
}
