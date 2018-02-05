package edu.isistan.seas.node;

import java.util.LinkedList;
import java.util.List;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.ExecutionManager;
import edu.isistan.mobileGrid.node.SchedulerProxy;
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

	private final long NO_OPS=0l;
	private int id;
	private Device device;
	private long mips;
	
	private double cpu;
	
	private List<Job> jobs=new LinkedList<Job>();
	
	private DefaultBatteryManager batteryManager;
	
	private Job executing;
	private long executedOps;
	private long lastEventTime;
	private Event lastEvent;

	private int finishedJob=0;
	/**
	 * Adds the job to the queue
	 * checks if it can start to execute
	 * notifies a possible change of profile to 
	 * the battery manager
	 */
	@Override
	public void addJob(Job job) {
		this.jobs.add(job);
		this.startExecute();
		this.batteryManager.onCPUProfileChange();
	}

	@Override
	public int getJobQueueSize() {
		return this.jobs.size();
	}

	@Override
	public Job getJob(int index) {
		return this.jobs.get(index);
	}

	@Override
	public void removeJob(int index) {
		this.jobs.remove(index);
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
		this.executing=null;
		this.lastEvent=null;
		
		// Execute new job		 
		this.startExecute();
		this.batteryManager.onCPUProfileChange();
		if(!this.isExecuting()) Logger.logEntity(device, "The device has become lazy");
		
		/*Yisel Log*/
		Logger.logJob(job.getJobId(),device.getName(), device.getBatteryLevel(), job.getInputSize(), job.getOutputSize());
	}

	/**
	 * recalculates how much time it needs to finish the 
	 * current job
	 */
	@Override
	public void onCPUEvent(double cpuUsage) {
		//If there is no job, there is nothing to do
		if(!this.isExecuting()){
			this.cpu=cpuUsage;
			return;
		}
		//get the old free mips
		double freeMipms=this.getFreeMIPMS();
		//Calculates how many ops it has executed since the last event}
		//and updates the executed ops
		this.executedOps+=(long) ((Simulation.getTime()-this.lastEventTime)*freeMipms);
		if(this.executedOps>this.executing.getOps()) throw new IllegalStateException("It executed more ops ("+this.executedOps+") than the size of the job ("+this.executing.getOps()+")");
		double toExecute=this.executing.getOps()-this.executedOps;
		//Update process
		this.cpu=cpuUsage;
		freeMipms=this.getFreeMIPMS();
		//Reestimate the remaining time using the new free mips
		double time=toExecute/freeMipms;
		time+=Simulation.getTime();
		//Updates the information in the simulator		
		this.lastEventTime=Simulation.getTime();
		Simulation.removeEvent(this.lastEvent);
		this.lastEvent=Event.createEvent(Event.NO_SOURCE, (long)time, this.id, Device.EVENT_TYPE_FINISH_JOB, this.executing);
		Simulation.addEvent(this.lastEvent);
	}

	@Override
	public int getNumberOfJobs() {
		return this.jobs.size()+(this.isExecuting() ? 1:0);
	}

	@Override
	public double getCPUUsage() {
		return this.cpu;
	}

	/**
	 * It is call when the device stop working
	 * update the state of unfinished jobs to fail
	 */
	@Override
	public void shutdown() {
		for(Job j:this.jobs)
			JobStatsUtils.fail(j,NO_OPS);
		if(this.isExecuting()){
			
			JobStatsUtils.fail(this.executing,this.executedOps);
			Simulation.removeEvent(this.lastEvent);
		}
		SchedulerProxy.PROXY.remove(this.device);
		Logger.logEntity(device, "Device stopped. Failed jobs: "+(this.jobs.size()+(this.isExecuting() ? 1:0))+" finished jobs "+this.finishedJob);
		
		/*Yisel Log*/
		int failedJobs = this.jobs.size()+(this.isExecuting() ? 1:0);
		Logger.logDevice(device.getName(), this.finishedJob+failedJobs, this.finishedJob, device.getCurrentTransfersCount(),device.getCurrentTotalTransferCount(),device.getWifiRSSI(), device.getEnergyPercentageWastedInNetworkActivity(), device.getInitialJoules(), device.getAccEnergyInTransfering());
	}

	public int getActualCPUProfile() {
		return this.isExecuting() ? 1 : 0;
	}

	public DefaultBatteryManager getBatteryManager() {
		return batteryManager;
	}

	public void setBatteryManager(DefaultBatteryManager batteryManager) {
		this.batteryManager = batteryManager;
	}

	protected boolean isExecuting(){
		return this.executing!=null;
	}
	
	/**
	 * Start to execute a new job
	 */
	protected void startExecute(){
		//if there is no jobs in the queue or it is already running
		//there is nothing to do
		if(this.isExecuting()||this.jobs.size()==0) return;
		//get the next job and update current information
		this.executing=this.jobs.remove(0);
		Logger.logEntity(this.device, "The device start executing ", this.executing);
		JobStatsUtils.startExecute(this.executing);
		this.executedOps=0;
		this.lastEventTime=Simulation.getTime();
		double freeMipms=this.getFreeMIPMS();
		//Calculate time to finish under current settings
		double time=((double)this.executing.getOps())/freeMipms;
		time+=Simulation.getTime();
		//updates the simulation
		this.lastEvent=Event.createEvent(Event.NO_SOURCE, (long)time, this.id, Device.EVENT_TYPE_FINISH_JOB,this.executing);
		Simulation.addEvent(this.lastEvent);
	}

	/**
	 * get the free mips
	 */
	protected double getFreeMIPMS() {
		double res=((double)this.mips)/1000d;//mips are divided by 1000 to get the instructions per millisecond instead of per second
		return res*(1d-this.cpu);
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
		this.id = Simulation.getEntityId(device.getName());
	}
}
