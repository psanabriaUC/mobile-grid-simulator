package edu.isistan.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;
import java.util.Iterator;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.DataAssignment;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class SimpleGAPlusOnDemandSchedulerProxy extends SimpleGASchedulerProxy {

	
	
	public SimpleGAPlusOnDemandSchedulerProxy(String name, String bufferValue) {
		super(name, bufferValue);		
	}
	
	@Override
	public void receive(Node scr, int id, Object data) {
		super.receive(scr, id, data);
		
		if (data instanceof Job)
			sendNextJobToNode((Device)scr);
	}
	
	@Override
	protected void scheduleJobs(ArrayList<DataAssignment> solution) {
		for (Iterator<DataAssignment> iterator = solution.iterator(); iterator.hasNext();) {
			DataAssignment da = (DataAssignment) iterator.next();
			Device dev = da.getDevice();
			
			if (!deviceToAssignmentsMap.containsKey(dev))
				deviceToAssignmentsMap.put(dev,da);
			else{
				DataAssignment devAssignment = deviceToAssignmentsMap.get(dev);
				devAssignment.scheduleJobs(da.getAssignedJobs());
			}				
			
			sendNextJobToNode(dev);
		}
	}
	
	private void sendNextJobToNode(Device dev){
		
		DataAssignment deviceAssignment = deviceToAssignmentsMap.get(dev);
		if (deviceAssignment.getAssignedJobs().size() > 0){//send the next job to the iddle device 				
			Job job = deviceAssignment.getAssignedJobs().remove(FIRST);
			Logger.logEntity(this, "Job assigned to ", job.getJobId() , (Node)dev);
			long time=NetworkModel.getModel().send(this, (Node)dev, idSend++,  job.getInputSize(), job);
			long currentSimTime = Simulation.getTime();
			JobStatsUtils.transfer(job, (Node)dev, time-currentSimTime,currentSimTime);
		}
	}

}
