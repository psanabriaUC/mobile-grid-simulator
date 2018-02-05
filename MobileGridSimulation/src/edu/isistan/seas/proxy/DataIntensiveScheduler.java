package edu.isistan.seas.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;

public abstract class DataIntensiveScheduler extends SchedulerProxy {

	protected int idSend = 0;
	protected static final int FIRST = 0;
	protected HashMap<Job,DataAssignment> jobAssignments = null;
	protected HashMap<Device,DataAssignment> deviceToAssignmentsMap = null;
	protected ArrayList<DataAssignment> totalDataPerDevice = null;
	protected boolean device_assignments_initialized = false;
	
	public DataIntensiveScheduler(String name) {
		super(name);
		jobAssignments = new HashMap<Job,DataAssignment>();
		totalDataPerDevice = new ArrayList<DataAssignment>();
		deviceToAssignmentsMap = new HashMap<Device,DataAssignment>();
	}

	@Override
	public boolean runsOnBattery() {		
		return false;
	}

	@Override
	public void processEvent(Event e) {
		if(EVENT_JOB_ARRIVE!=e.getEventType()) throw new IllegalArgumentException("Unexpected event");
		if (!device_assignments_initialized) initializeDeviceAssignments();
		
		Job j=(Job)e.getData();
		JobStatsUtils.addJob(j, this);		
		Logger.logEntity(this, "Job arrived ", j.getJobId());
		
		assignJob(j);
	}
	
	protected abstract void assignJob(Job job);
	
	protected void initializeDeviceAssignments() {
		for (Iterator<Device> iterator = devices.values().iterator();iterator.hasNext();) {
			Device d = (Device)iterator.next();
			DataAssignment tdd = new DataAssignment(d);
			totalDataPerDevice.add(tdd);
			deviceToAssignmentsMap.put(d, tdd);			
		}
		this.device_assignments_initialized = true;		
	}
	
	@Override
	public void remove(Device device) {
		DataAssignment d = deviceToAssignmentsMap.get(device);
		if (totalDataPerDevice.indexOf(d) != -1)
			totalDataPerDevice.get(totalDataPerDevice.indexOf(d));
		deviceToAssignmentsMap.remove(device);
		super.remove(device);
	}
	
	@Override
	public void receive(Node scr, int id, Object data) {
		if (data instanceof Job){//Update sent and received data of the corresponding node
			Job jobResult = (Job) data;
			DataAssignment assignment = jobAssignments.get(jobResult);
			if(assignment!= null){
				assignment.setMbToBeReceived(assignment.getMbToBeReceived()-((double)((double)jobResult.getInputSize() / (double)(1024*1024))));
				assignment.setMbToBeSend(assignment.getMbToBeSend()- ((double)((double)jobResult.getOutputSize() / (double)(1024*1024))));
			}
		}
		super.receive(scr, id, data);	
	}
}
