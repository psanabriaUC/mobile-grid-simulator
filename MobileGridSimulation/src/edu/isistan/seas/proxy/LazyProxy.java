package edu.isistan.seas.proxy;

import java.util.ArrayList;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class LazyProxy extends SchedulerProxy {
	
	private static final int FIRST = 0;	
	//protected HashMap<String,Device> devices = new HashMap<String,Device>();
	protected ArrayList<Job> inQueueJobs = null;
	protected ArrayList<Device> iddleDevices = null;
	protected ArrayList<Long> startIddleTimes = null;
	protected int idSend = 0;
	
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
	public void receive(Node scr, int id, Object data) {
		if (data instanceof Job){
			Job jobResult = (Job) data;
			JobStatsUtils.successTrasferBack(jobResult);
			if (!inQueueJobs.isEmpty()){
				Job j = inQueueJobs.remove(FIRST);
				Logger.logEntity(this, "Job assigned to ", j.getJobId() ,scr);
				long time=NetworkModel.getModel().send(this, scr, idSend++,  j.getInputSize(), j);
				long currentSimTime = Simulation.getTime();
				JobStatsUtils.transfer(j, scr, time-currentSimTime,currentSimTime);
			}
			else{
				iddleDevices.add((Device)scr);
				startIddleTimes.add(Simulation.getTime());
			}
		}else
			if(data instanceof UpdateMsg){
				UpdateMsg msg = (UpdateMsg) data;
				Device device = devices.get(msg.getNodeId());
				device.setLastBatteryLevelUpdate(msg.getPercentageOfRemainingBattery());				
				JobStatsUtils.registerUpdateMessage(scr,(UpdateMsg)data);
			}

	}

	@Override
	public void success(int id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void fail(int id) {
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
	public void processEvent(Event e) {
		if(EVENT_JOB_ARRIVE!=e.getEventType()) throw new IllegalArgumentException("Unexpected event");
		Job j=(Job)e.getData();
		JobStatsUtils.addJob(j, this);		
		Logger.logEntity(this, "Job arrived ", j.getJobId());
		inQueueJobs.add(j);		
		assignJob();		
	}
	
	private void assignJob() {
		if (!iddleDevices.isEmpty() && !inQueueJobs.isEmpty()){
			Device device = iddleDevices.remove(FIRST);
			Job j = inQueueJobs.remove(FIRST);
			Logger.logEntity(this, "Job assigned to ", j.getJobId() ,device);
			long time=NetworkModel.getModel().send(this, device, idSend++,  j.getInputSize(), j);
			long currentSimTime = Simulation.getTime();
			JobStatsUtils.incIddleTime(currentSimTime-startIddleTimes.remove(FIRST));
			JobStatsUtils.transfer(j, device, time-currentSimTime,currentSimTime);
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
