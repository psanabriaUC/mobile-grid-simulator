package edu.isistan.seas.proxy.bufferedproxy;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.DataAssignment;
import edu.isistan.seas.proxy.DescendingDataAssignmentComparator;
import edu.isistan.seas.proxy.RemainingDataTransferingEvaluator;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class DataAnalyticBufferedSchedulerProxy extends BufferedSchedulerProxy {

	public DataAnalyticBufferedSchedulerProxy(String name, String bufferValue) {
		super(name, bufferValue);
		DataAssignment.evaluator = new RemainingDataTransferingEvaluator();
	}

	@Override
	protected void queueJob(Job job) {		
		bufferedJobs.add(job);
	}

	@Override
	protected void assignBufferedJobs() {
		//TODO: update node availability and deviceAssignments every time this method is called
		DataAssignment.evaluator = new RemainingDataTransferingEvaluator();
		Comparator<DataAssignment> comp = new DescendingDataAssignmentComparator(DataAssignment.evaluator);
		Collections.sort(totalDataPerDevice, comp);
				
		for (Job dataJob : bufferedJobs){			
			int assignment=FIRST;
			while (assignment < totalDataPerDevice.size() && DataAssignment.evaluator.eval(totalDataPerDevice.get(assignment)) <= 0)
				assignment++;
			if(assignment < totalDataPerDevice.size()){
				totalDataPerDevice.get(assignment).scheduleJob(dataJob);
				Collections.sort(totalDataPerDevice, comp);
			}
			else
				break;				
		}

		for (DataAssignment deviceAssignment : totalDataPerDevice) {
			Device current = deviceAssignment.getDevice();
			for (Job job : deviceAssignment.getAssignedJobs()) {
				queueJobTransferring(current, job);
				/*
				Logger.logEntity(this, "Job assigned to ", job.getJobId() ,current);
				long time=NetworkModel.getModel().send(this, current, idSend++,  job.getInputSize(), job);
				long currentSimTime = Simulation.getTime();
				JobStatsUtils.transfer(job, current, time-currentSimTime,currentSimTime);
				*/
			}
		}		
	}

}
