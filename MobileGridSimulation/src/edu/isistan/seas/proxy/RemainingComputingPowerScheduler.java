package edu.isistan.seas.proxy;

import java.util.Arrays;
import java.util.HashMap;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class RemainingComputingPowerScheduler extends SchedulerProxy {

	/**stores the max flop that a device type (identified by flops) is able to execute*/
	private final HashMap<Long,Long> maxExecFlop = new HashMap<Long,Long>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{ put(61665000L,1615086267840L);//A100		
		  put(17070000L,850153716690L);//viewpad
		  put(7602000L,267025031658L);//i5500			
		}};

	
	private int currentDeviceIDIndex = 0;
	private long maxDeviceExecPower=Long.MAX_VALUE;
	private int maxDevices;	
	private String[] devicesIDs=null;

	public RemainingComputingPowerScheduler(String name) {
		super(name);		
	}

	@Override
	public boolean runsOnBattery() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private Device getDevice(long jobOps){
		if (maxDeviceExecPower < 0) { //means that the device computing capacity is exceeded
			if (currentDeviceIDIndex + 1 == maxDevices) //means that there are no more candidates nodes with remaining computing capacity
				return null;
			else { //select the next device in the list of candidates
				currentDeviceIDIndex++;
				maxDeviceExecPower = maxExecFlop.get(devices.get(devicesIDs[currentDeviceIDIndex]).getMIPS());				
			}
		}
		maxDeviceExecPower -= jobOps;
		return devices.get(devicesIDs[currentDeviceIDIndex]);		
	}

	@Override
	public void processEvent(Event event) {
		if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job job = (Job) event.getData();
		JobStatsUtils.addJob(job, this);
		Logger.logEntity(this, "Job arrived ", job.getJobId());
		
		if (devicesIDs == null) {
			maxDevices = devices.size();
			devicesIDs = new String[maxDevices];
			devices.keySet().toArray(devicesIDs);
			Arrays.sort(devicesIDs);
			maxDeviceExecPower = maxExecFlop.get(devices.get(devicesIDs[currentDeviceIDIndex]).getMIPS());
		}
		
		Device dev = getDevice(job.getOps());
		if (dev != null) {
			queueJobTransferring(dev, job);
		} else {
			 JobStatsUtils.rejectJob(job,Simulation.getTime());
			 Logger.logEntity(this, "Job rejected = " + job.getJobId() + " at " + Simulation.getTime() +
					 " simulation time");
		}
			
	}

}
