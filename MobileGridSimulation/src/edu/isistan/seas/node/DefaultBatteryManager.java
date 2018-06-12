package edu.isistan.seas.node;

import java.util.SortedSet;
import java.util.TreeSet;

import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;
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
    private static final int PROFILE_CPU_USER = 0;
    private static final int PROFILE_CPU_FULL_SCREEN_OFF = 1;
    private static final int PROFILE_CPU_FULL_SCREEN_ON = 2;
	
	private Device device;
	//hay que inicializarlos con un valor al principio
	private double lastCharge;
	private long lastMeasurement;
	private int reportedCharge;

    /**
     * The battery capacity of a device expressed in Joules. Joules = w*h = w * 3600 sec.
     */
	private long batteryCapacityInJoules;

    /**
     * The initial state of charge of this device when it joined the grid, as a value between 0 and 10.000.000, where
     * the latter means 100%.
     */
    private int initialSOC;
	
	//hay que inicializarlos con un valor al principio
	private double estimatedUpTime;

    /**
     * Used by the SEAS estimation model. This is the number of battery samples taken into account so far.
     */
	private double measures = 1;

    /**
     * The simulation time at which the last battery update event was transmitted.
     */
	private double lastEventTime;

	// TODO: move towards proxy
	private double lastReportedCharge;

    /**
     * The simulation timestamp at which the device associated with this class started working, therefore, the time
     * since which the battery levels started to decrease.
     */
	private long startTime;

    /**
     * A reference to the next battery update event to post in the simulation.
     */
	private Event lastAddedEvent;

    /**
     * Battery trace files associated with this device, gathered from real-life profiling.
     */
	private SortedSet<ProfileData>[] profiles;

    /**
     * The index of the current profile in active use.
     */
	private int currentProfile;

    /**
     * The {@link edu.isistan.mobileGrid.node.ExecutionManager} associated with this battery manager.
     */
	private DefaultExecutionManager executionManager;

    /**
     * Flag indicating if the device is currently running jobs.
     */
	private boolean runningJobs;

    /**
     * Flag indicating the current status of the device's screen.
     */
	private boolean screenOn;

    /**
     * Builds a standard battery manager with default capabilities to emulate battery discharges when idle, executing
     * CPU intensive jobs, and transferring data over a network.
     *
     * @param prof Number of trace data sets to emulate battery discharge under different workloads.
     * @param charge Initial state of charge of the device's battery (0 - 1000000).
     * @param estUptime Estimated time until discharge in milliseconds.
     * @param batteryCapacityInJoules Battery capacity in Joules.
     */
	@SuppressWarnings("unchecked")
	public DefaultBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules){
        this.batteryCapacityInJoules = batteryCapacityInJoules;
        this.initialSOC = charge;
		this.lastCharge = charge;
		this.reportedCharge = charge;
		this.lastReportedCharge = charge;
		this.estimatedUpTime = estUptime;
		this.profiles = (SortedSet<ProfileData>[]) new SortedSet[prof];
		for(int i = 0; i < prof; i++) {
            this.profiles[i] = new TreeSet<>(new NegativeComparator<ProfileData>());
        }
        this.currentProfile = PROFILE_CPU_USER;
	}
	/**
	 * This method is executed when a job start to execute
	 * or it finished. The idea is to switch between the two
	 * profiles by updating the next battery event.
     *
     * @param profile The index of the profile to switch over to.
	 */
	private void onCPUProfileChange(int profile) {
		if (this.currentProfile == profile) return;
		double lTime = this.lastMeasurement;
		double now = Simulation.getTime();
		// Calculates new charge
		double slope = this.profiles[this.currentProfile].first().getSlope();
		double newCharge = this.lastCharge + (now - lTime) * slope;
		
		if (newCharge > this.lastCharge)
			Logger.appendDebugInfo("on profile change new charge is bigger than last charge!" + newCharge + "\n");
		if (newCharge <= 0.0) {
			if (newCharge < 0.0)
				Logger.appendDebugInfo("on profile change charge become negative" + newCharge + "\n");
			this.device.onBatteryDepletion();
			return;
		}
		// Calculates new event time
		double nextChargeEvent	= this.moveToNext(newCharge, profile);
		
		double nEventTime = now + (nextChargeEvent - newCharge) / this.profiles[profile].first().getSlope();
		// Update values and checks validity
		this.currentProfile = profile;
		
		this.lastCharge = newCharge;
		this.lastMeasurement = Simulation.getTime();
		
		// uncomment for debugging
		// Logger.appendDebugInfo(this.device.getName()+";PRO;"+this.lastMeasurement+";"+(double)this.lastCharge+";"+lastAddedEvent.getEventId()+"\n");
		
		if(this.lastMeasurement > nEventTime) {
		    throw new IllegalStateException("Next event time is previous (" + nEventTime + ") to current time (" + this.lastMeasurement + ")");
        }

		Simulation.removeEvent(this.lastAddedEvent);
		this.lastAddedEvent = Event.createEvent(Event.NO_SOURCE, (long) nEventTime, this.device.getId(),
                Device.EVENT_TYPE_BATTERY_UPDATE, this.profiles[profile].first().getToCharge());
		Simulation.addEvent(this.lastAddedEvent);
	}

	@Override
	public void onNetworkEnergyConsumption(double decreasingPercentageValue) {
			// Logger.logEnergy( "onNetworkEnergyConsumption","decreasingPercentageValue="+decreasingPercentageValue);
			
			double slope = this.profiles[this.currentProfile].first().getSlope();
			this.lastCharge = this.lastCharge + (Simulation.getTime() - this.lastMeasurement) * slope;
			
			this.lastCharge -= decreasingPercentageValue;
			this.lastMeasurement = Simulation.getTime(); 
			// uncomment for debugging
			// Logger.appendDebugInfo(this.device.getName()+";NET;"+this.lastMeasurement+";"+this.lastCharge);
					
			if (this.lastCharge <= 0){
				this.device.onBatteryDepletion();
				return;
			}
			 
			int futureCharge = ((int)lastAddedEvent.getData());			
			int nextCharge=this.moveToNext(lastCharge, this.currentProfile);
			boolean skippedSample = futureCharge != nextCharge;
			
			double	newTime = (double)this.lastMeasurement+(((double)nextCharge-this.lastCharge)/this.profiles[this.currentProfile].first().getSlope());
			if (newTime < this.lastMeasurement) {
                throw new IllegalStateException("Next event time is previous (" + newTime + ") to current time (" + this.lastMeasurement + ")");
            }
						
			// with the energy consumption introduced by network, the lastAddedEvent is out of date.
			// There are two possible update operations. One involves get a new sample from the
			// current profile and calculate the time when this battery level will take place while the other
			// involves only the last operation. Both update operations needs that lastAddedEvent be removed and
			// added to the simulation events queue in order to be inserted in the correct place.
			if (!skippedSample) {
                Simulation.updateEventTime(lastAddedEvent, (long) newTime);
            } else {
				Simulation.removeEvent(lastAddedEvent);
				lastAddedEvent = Event.createEvent(lastAddedEvent.getSourceId(), (long) newTime,
                        lastAddedEvent.getTargetId(), lastAddedEvent.getEventType(), nextCharge);
				Simulation.addEvent(lastAddedEvent);
			}

			
			//TODO: INCLUDE CALL TO this.updateEstimatedUptime();		
	}

	@Override
	public void onBatteryEvent(int level) {
		if(level <= 0) {
			lastCharge = 0;
			this.lastMeasurement = Simulation.getTime();
			this.device.onBatteryDepletion();
			return;
		}
		
		this.lastCharge = level;
		this.lastMeasurement = Simulation.getTime();
		this.reportedCharge = level;
		
		// uncomment for debugging
		// Logger.appendDebugInfo(this.device.getName()+";BAT;"+this.lastMeasurement+";"+this.lastCharge+";"+lastAddedEvent.getEventId()+"\n");
		
		this.moveToNext(this.lastCharge, this.currentProfile);
		double nextEventCharge = this.profiles[this.currentProfile].first().getToCharge();
		
		 // Commented by Matias: The current and the next state of charge are joint with a line whose equation is
		 // (y - b) / a = x, where y is the next state of charge, b is the current charge and a is the slope of
		 // the line that join both state of charge. The equation is used to know the time when the next state
		 // charge will occur. That time is added to the time when the current state of charge happened
		 // (lastMeasurement).
		double nTime = this.lastMeasurement + (nextEventCharge - this.lastCharge) / this.profiles[this.currentProfile].first().getSlope();
		
		if(nTime < this.lastMeasurement)
			throw new IllegalStateException("Next event time is previous (" + nTime + ") to current time (" + this.lastMeasurement + ")");
		this.lastAddedEvent = Event.createEvent(Event.NO_SOURCE, (long) nTime, this.device.getId(),
                Device.EVENT_TYPE_BATTERY_UPDATE, this.profiles[this.currentProfile].first().getToCharge());
		Simulation.addEvent(this.lastAddedEvent);
		this.updateEstimatedUptime();
	}

	@Override
	public void onUserActivityEvent(boolean screenOn) {
	    this.screenOn = screenOn;

	    if (screenOn && runningJobs) {
	        onCPUProfileChange(PROFILE_CPU_FULL_SCREEN_ON);
        } else if (!screenOn && runningJobs) {
	        onCPUProfileChange(PROFILE_CPU_FULL_SCREEN_OFF);
        } else {
	        onCPUProfileChange(PROFILE_CPU_USER);
        }
	}

    @Override
    public void onBeginExecutingJobs() {
	    this.runningJobs = true;

	    if (screenOn) {
            onCPUProfileChange(PROFILE_CPU_FULL_SCREEN_ON);
        } else {
            onCPUProfileChange(PROFILE_CPU_FULL_SCREEN_OFF);
        }
    }

    @Override
    public void onStopExecutingJobs() {
        this.runningJobs = false;

        onCPUProfileChange(PROFILE_CPU_USER);
    }

    @Override
	public void startWorking() {
		this.lastMeasurement = Simulation.getTime();
		this.lastEventTime = Simulation.getTime();
		this.startTime = Simulation.getTime();
		double nextEventCharge = this.profiles[this.currentProfile].first().getToCharge();
		double nTime = this.lastMeasurement + (nextEventCharge - this.lastCharge) / this.profiles[this.currentProfile].first().getSlope();
		if(nTime < this.lastMeasurement) {
		    throw new IllegalStateException("Next event time is previous (" + nTime + ") to current time (" +
                    this.lastMeasurement + ")");
		}

		this.lastAddedEvent = Event.createEvent(Event.NO_SOURCE, (long) nTime, this.device.getId(),
                Device.EVENT_TYPE_BATTERY_UPDATE, this.profiles[this.currentProfile].first().getToCharge());
		Simulation.addEvent(this.lastAddedEvent);
		
		// debugging line
		// Logger.appendDebugInfo(this.device.getName()+";INI;"+this.lastMeasurement+";"+this.lastCharge+";futEvent:"+lastAddedEvent.getEventId()+"\n");

		Logger.logEntity(device, "Device started");
	}

	@Override
	public void shutdown() {		
		Simulation.removeEvent(lastAddedEvent);		
	}

	private int moveToNext(double newCharge, int prof) {
		while((this.profiles[prof].first().getToCharge() >= newCharge))
			this.profiles[prof].remove(this.profiles[prof].first());
		return this.profiles[prof].first().getToCharge();
	}

    /**
     * Updates the estimated time until battery fully discharges according to the SEAS uptime model.
     */
	private void updateEstimatedUptime() {
		double now = Simulation.getTime();
		double timeToDischarge = (-this.reportedCharge) / ((this.reportedCharge - this.lastReportedCharge) / (now - this.lastEventTime)) + now;
		this.estimatedUpTime = (this.estimatedUpTime * this.measures + (timeToDischarge - this.startTime)) / (this.measures + 1);
		this.lastReportedCharge = this.reportedCharge;
		this.lastEventTime = now;
		this.measures++;
	}
	
	public void addProfileData(int prof, ProfileData dat){
		this.profiles[prof].add(dat);
	}

	// Getters and setters

    @Override
    public int getCurrentBattery() {
        return (int)this.lastCharge;
    }

    @Override
    public double getCurrentSOC() {
        //return this.lastCharge;
        double slope = this.profiles[this.currentProfile].first().getSlope();
        return this.lastCharge + (Simulation.getTime()-this.lastMeasurement) * slope;
    }

    @Override
    public long getEstimatedUptime() {
        return (long) (this.estimatedUpTime + this.startTime - Simulation.getTime());
    }

    public long getBatteryCapacityInJoules() {
        return batteryCapacityInJoules;
    }

    public DefaultExecutionManager getSEASExecutionManager() {
		return executionManager;
	}

	public void setSEASExecutionManager(DefaultExecutionManager seasEM) {
		this.executionManager = seasEM;
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
