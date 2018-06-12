package edu.isistan.seas.node;

import java.util.LinkedList;
import java.util.List;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.ExecutionManager;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;
/**
 * It represents the execution model. This is a simple model
 * that assigns the free CPU to one job. Basically, it calculates
 * how long it would take to execute the the job using the 
 * following formular:
 * JobOps/(mips*(1-currentCPUUsage)
 * This model updates the job finish time for each new CPU usage event 
 * @author cuchillo
 *
 */
public class DefaultExecutionManager implements ExecutionManager {

	private static final long NO_OPS = 0l;

    /**
     * The {@link Device} attached to this execution manager.
     */
	private Device device;

    /**
     * The processor speed in millions of instructions per second.
     */
	private long mips;

    /**
     * The fraction of the CPU currently in use by the end-user (and therefore unavailable for background operations),
     * as a value between 0 and 1, where 0 means the entire CPU is free for use, and 1 means it is completely hogged
     * by the user. Therefore, the actual speed of the processor can be estimated by:
     * {@link DefaultExecutionManager#mips} * (1 - cpu).
     */
	private double cpu;

    /**
     * List of assigned jobs pending to be executed.
     */
	private List<Job> pendingJobs = new LinkedList<>();

    /**
     * The battery manager attached to this device.
     */
	private DefaultBatteryManager batteryManager;

    /**
     * Job currently being executed. Once assigned to this variable, the job should be removed from the
     * {@link DefaultExecutionManager#pendingJobs} list.
     */
	private Job executing;

    /**
     * The number of instructions executed by this device throughout the simulation. Used for logging purposes.
     */
	private long executedOps;

    /**
     * The simulation timestamp at which the {@link DefaultExecutionManager#lastEvent} was scheduled.
     */
	private long lastEventTime;

    /**
     * A reference to the {@link Device#EVENT_TYPE_FINISH_JOB} event that was scheduled by this class to simulate
     * the completion of the {@link Job} currently assigned to it.
     */
	private Event lastEvent;

    /**
     * The amount of {@link Job} completed by this device. Used for logging purposes.
     */
	private int finishedJob = 0;

	/**
	 * Adds the job to the queue
	 * checks if it can start to execute
	 * notifies a possible change of profile to 
	 * the battery manager
	 */
	@Override
	public void addJob(Job job) {
		this.pendingJobs.add(job);
		this.startExecute();
		if (isExecuting()) {
			this.batteryManager.onBeginExecutingJobs();
		} else {
			this.batteryManager.onStopExecutingJobs();
		}

	}

	@Override
	public int getJobQueueSize() {
		return this.pendingJobs.size();
	}

	@Override
	public Job removeJob(int index) {
		return this.pendingJobs.remove(index);
	}

	/**
	 * Finishes the current Job
	 * Tries to start a new one
	 * Notifies to the battery manager a possible profile change
	 */
	@Override
	public void onFinishJob(Job job) {
		this.finishedJob++;
		JobStatsUtils.success(job);
		Logger.logEntity(device, "The device finished the job",job,
				JobStatsUtils.timeToMinutes(JobStatsUtils.getJobStats(this.executing).getTotalExecutionTime()));
		this.executing = null;
		this.lastEvent = null;
		
		// Execute new job		 
		this.startExecute();
		if (isExecuting()) {
			// TODO: check if maybe we can eliminate this line.
			this.batteryManager.onBeginExecutingJobs();
		} else {
			this.batteryManager.onStopExecutingJobs();
			Logger.logEntity(device, "The device has become lazy");
		}
		
		/*Yisel Log*/
		Logger.logJob(job.getJobId(), device.getName(), device.getBatteryLevel(), job.getInputSize(), job.getOutputSize());
	}

	/**
	 * recalculates how much time it needs to finish the 
	 * current job
	 */
	@Override
	public void onCPUEvent(double cpuUsage) {
		//If there is no job, there is nothing to do
		if(!this.isExecuting()){
            this.cpu = cpuUsage;
			return;
		}
		//get the old free mips
		double freeMipms = this.getFreeMIPMS();

		//Calculates how many ops it has executed since the last event}
		//and updates the executed ops
		this.executedOps += (long) ((Simulation.getTime() - this.lastEventTime) * freeMipms);
		if(this.executedOps > this.executing.getOps()) throw new IllegalStateException("It executed more ops (" +
                this.executedOps+") than the size of the job (" + this.executing.getOps() + ")");
		double toExecute = this.executing.getOps() - this.executedOps;

		//Update process
        this.cpu = cpuUsage;
		freeMipms = this.getFreeMIPMS();
		//Reestimate the remaining time using the new free mips
		double time = toExecute / freeMipms;
		time += Simulation.getTime();
		//Updates the information in the simulator		
		this.lastEventTime = Simulation.getTime();
		Simulation.removeEvent(this.lastEvent);
		this.lastEvent=Event.createEvent(Event.NO_SOURCE, (long)time, this.device.getId(),
                Device.EVENT_TYPE_FINISH_JOB, this.executing);
		Simulation.addEvent(this.lastEvent);
	}

	/**
	 * It is call when the device stop working
	 * update the state of unfinished jobs to fail
	 */
	@Override
	public void shutdown() {
		for(Job job : this.pendingJobs)
			JobStatsUtils.fail(job, NO_OPS);
		if(this.isExecuting()) {
			JobStatsUtils.fail(this.executing,this.executedOps);
			Simulation.removeEvent(this.lastEvent);
		}

		Logger.logEntity(device, "Device stopped. Failed jobs: " +
                (this.pendingJobs.size() + (this.isExecuting() ? 1 : 0)) + " finished jobs " + this.finishedJob);
		
		/*Yisel Log*/
		int failedJobs = this.pendingJobs.size() + (this.isExecuting() ? 1 : 0);
		Logger.logDevice(device.getName(),
                this.finishedJob + failedJobs,
                this.finishedJob,
                device.getCurrentTransfersCount(),
                device.getCurrentTotalTransferCount(),
                device.getWifiRSSI(),
                device.getEnergyPercentageWastedInNetworkActivity(),
                device.getInitialJoules(),
                device.getAccEnergyInTransferring());
	}

	// public int getActualCPUProfile() {
	// 	return this.isExecuting() ? 1 : 0;
	// }
	
	/**
	 * Start to execute a new job
	 */
	protected void startExecute() {
		//if there is no jobs in the queue or it is already running
		//there is nothing to do
		if (this.isExecuting() || this.pendingJobs.size() == 0) return;
		//get the next job and update current information
		this.executing = this.pendingJobs.remove(0);
		Logger.logEntity(this.device, "The device start executing ", this.executing);
		JobStatsUtils.startExecute(this.executing);

		this.executedOps = 0;
		this.lastEventTime = Simulation.getTime();
		double freeMipms = this.getFreeMIPMS();
		//Calculate time to finish under current settings
		double time = ((double) this.executing.getOps()) / freeMipms;
		time += Simulation.getTime();
		// updates the simulation
		this.lastEvent = Event.createEvent(Event.NO_SOURCE, (long) time, this.device.getId(),
                Device.EVENT_TYPE_FINISH_JOB, this.executing);
		Simulation.addEvent(this.lastEvent);
	}

	// Getters and setters

    @Override
    public int getNumberOfJobs() {
        return this.pendingJobs.size() + (this.isExecuting() ? 1 : 0);
    }

    @Override
    public double getCPUUsage() {
        return this.cpu;
    }

    public DefaultBatteryManager getBatteryManager() {
        return batteryManager;
    }

    public void setBatteryManager(DefaultBatteryManager batteryManager) {
        this.batteryManager = batteryManager;
    }

    protected boolean isExecuting(){
        return this.executing != null;
    }

	/**
	 * Gets the free mips.
	 */
	protected double getFreeMIPMS() {
        // mips are divided by 1000 to get the instructions per millisecond instead of per second
		double res = ((double) this.mips) / 1000d;
		return res * (1d - this.cpu);
	}
	
	@Override
	public long getMIPS() {
		return this.mips;
	}

	public void setMips(long mips) {
		this.mips = mips;
	}
	
	public Device getDevice() {
		return this.device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}
}
