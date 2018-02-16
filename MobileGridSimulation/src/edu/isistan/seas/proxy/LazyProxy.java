package edu.isistan.seas.proxy;

import java.util.ArrayList;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Entity;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

/**
 * Scheduler that assigns a single {@link Job} to every {@link Entity} in the grid, then waits until a device finishes
 * executing his job before re-assigning it a new one.
 */
public class LazyProxy extends SchedulerProxy {
	
	private static final int FIRST = 0;	
	//protected HashMap<String,Device> devices = new HashMap<String,Device>();
	protected ArrayList<Job> inQueueJobs = null;
	protected ArrayList<Device> iddleDevices = null;
	protected ArrayList<Long> startIddleTimes = null;
	
	public LazyProxy(String name) {		
		super(name);		
		inQueueJobs = new ArrayList<Job>();
		iddleDevices = new ArrayList<Device>();
		startIddleTimes = new ArrayList<Long>();		
	}

	@Override
	public void onDeviceFail(Node e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void incomingData(Node scr, int id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageReceived(Message message) {
		if (message.getData() instanceof Job){
			Job jobResult = (Job) message.getData();
			JobStatsUtils.successTransferBack(jobResult);
			if (!inQueueJobs.isEmpty()) {
				Job job = inQueueJobs.remove(FIRST);
				queueJobTransferring((Device) message.getSource(), job);
				/*
				Logger.logEntity(this, "Job assigned to ", job.getJobId() ,scr);
				long time=NetworkModel.getModel().send(this, scr, idSend++,  job.getInputSize(), job);
				long currentSimTime = Simulation.getTime();
				JobStatsUtils.transfer(job, scr, time-currentSimTime,currentSimTime);
				*/
			} else {
				iddleDevices.add((Device)message.getSource());
				startIddleTimes.add(Simulation.getTime());
			}
		} else if(message.getData() instanceof UpdateMsg) {
			UpdateMsg updateMessage = (UpdateMsg) message.getData();
			Device device = devices.get(updateMessage.getNodeId());
			device.setLastBatteryLevelUpdate(updateMessage.getPercentageOfRemainingBattery());
			JobStatsUtils.registerUpdateMessage(message.getSource(), updateMessage);
		}

	}

	@Override
	public void onMessageSentAck(Message message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void fail(Message message) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isOnline() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void startTransfer(Node dst, int id, Object data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void failReception(Node scr, int id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean runsOnBattery() {		
		return false;
	}

	@Override
	public void processEvent(Event event) {
		if(EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job job = (Job) event.getData();
		JobStatsUtils.addJob(job, this);
		Logger.logEntity(this, "Job arrived ", job.getJobId());
		inQueueJobs.add(job);
		assignJob();		
	}
	
	private void assignJob() {
		if (!iddleDevices.isEmpty() && !inQueueJobs.isEmpty()) {
			Device device = iddleDevices.remove(FIRST);
			Job job = inQueueJobs.remove(FIRST);
			queueJobTransferring(device, job);
		}		
	}

	@Override
	public void remove(Device device) {
		this.devices.remove(device.getName());
		int deviceIndex = iddleDevices.indexOf(device);
		if (deviceIndex > -1){
			iddleDevices.remove(deviceIndex);
			startIddleTimes.remove(deviceIndex);
		}
	}

	@Override
	public void addDevice(Device device) {
		this.devices.put(device.getName(),device);
		iddleDevices.add(device);
		startIddleTimes.add(Simulation.getTime());
	}

}
