package edu.isistan.seas.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Message;
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
	public void processEvent(Event event) {
		if(EVENT_JOB_ARRIVE!= event.getEventType()) throw new IllegalArgumentException("Unexpected event");
		if (!device_assignments_initialized) initializeDeviceAssignments();
		
		Job j=(Job) event.getData();
		JobStatsUtils.addJob(j, this);		
		Logger.logEntity(this, "Job arrived ", j.getJobId());
		
		assignJob(j);
	}
	
	protected abstract void assignJob(Job job);
	
	protected void initializeDeviceAssignments() {
		for (Device device : devices.values()) {
			DataAssignment dataAssignment = new DataAssignment(device);
			totalDataPerDevice.add(dataAssignment);
			deviceToAssignmentsMap.put(device, dataAssignment);
		}
		this.device_assignments_initialized = true;		
	}
	
	@Override
	public void remove(Device device) {
		DataAssignment dataAssignment = deviceToAssignmentsMap.get(device);
		if (totalDataPerDevice.indexOf(dataAssignment) != -1)
			totalDataPerDevice.get(totalDataPerDevice.indexOf(dataAssignment));
		deviceToAssignmentsMap.remove(device);
		super.remove(device);
	}
	
	@Override
	public void onMessageReceived(Message message) {
		if (message.getData() instanceof Job){ //Update sent and received data of the corresponding node
			Job jobResult = (Job) message.getData();
			DataAssignment assignment = jobAssignments.get(jobResult);
			if(assignment != null){
				assignment.setMbToBeReceived(assignment.getMbToBeReceived() - (double)jobResult.getInputSize() /
						(double)(1024 * 1024));
				assignment.setMbToBeSend(assignment.getMbToBeSend() - (double)jobResult.getOutputSize() /
                        (double)(1024 * 1024));
			}
		}
		super.onMessageReceived(message);
	}
}
