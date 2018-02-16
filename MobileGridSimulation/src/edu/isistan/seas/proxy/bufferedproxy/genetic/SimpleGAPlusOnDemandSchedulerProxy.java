package edu.isistan.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.DataAssignment;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class SimpleGAPlusOnDemandSchedulerProxy extends SimpleGASchedulerProxy {

	
	
	public SimpleGAPlusOnDemandSchedulerProxy(String name, String bufferValue) {
		super(name, bufferValue);		
	}
	
	@Override
	public void onMessageReceived(Message message) {
		super.onMessageReceived(message);
		
		if (message.getData() instanceof Job)
			sendNextJobToNode((Device) message.getSource());
	}
	
	@Override
	protected void scheduleJobs(ArrayList<DataAssignment> solution) {
		for (DataAssignment da : solution) {
			Device dev = da.getDevice();

			if (!deviceToAssignmentsMap.containsKey(dev))
				deviceToAssignmentsMap.put(dev, da);
			else {
				DataAssignment devAssignment = deviceToAssignmentsMap.get(dev);
				devAssignment.scheduleJobs(da.getAssignedJobs());
			}

			sendNextJobToNode(dev);
		}
	}
	
	private void sendNextJobToNode(Device dev){
		
		DataAssignment deviceAssignment = deviceToAssignmentsMap.get(dev);
		if (deviceAssignment.getAssignedJobs().size() > 0){ //send the next job to the iddle device
			Job job = deviceAssignment.getAssignedJobs().remove(FIRST);
			queueJobTransferring(dev, job);

			/*
			Logger.logEntity(this, "Job assigned to ", job.getJobId() , dev);
			long time=NetworkModel.getModel().send(this, dev, idSend++,  job.getInputSize(), job);
			long currentSimTime = Simulation.getTime();
			JobStatsUtils.transfer(job, dev, time-currentSimTime,currentSimTime);
			*/
		}
	}

}
