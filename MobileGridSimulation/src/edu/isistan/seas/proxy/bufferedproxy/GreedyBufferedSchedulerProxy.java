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

public class GreedyBufferedSchedulerProxy extends BufferedSchedulerProxy {

	public GreedyBufferedSchedulerProxy(String name, String bufferValue) {
		super(name, bufferValue);		
	}

	@Override
	protected void queueJob(Job job) {
		int dataTransferRequirement = job.getInputSize() + job.getOutputSize();
		boolean inserted = false;
		int queueIndex = 0;
		while (!inserted){
			if (queueIndex < bufferedJobs.size()){ 
				Job currentJob = bufferedJobs.get(queueIndex);
				int currentJobDataTransferRequirement = currentJob.getInputSize()+currentJob.getOutputSize();  
				if (currentJobDataTransferRequirement > dataTransferRequirement){
					bufferedJobs.add(queueIndex,job);
					inserted = true;
				}
				else{
					queueIndex++;
				}
			}
			else{
				bufferedJobs.add(job);
				inserted = true;
			}	
		}
	}

	@Override
	protected void assignBufferedJobs() {
		//TODO: update node availability and deviceAssignments every time this method is called
		DataAssignment.evaluator = new RemainingDataTransferingEvaluator();
		Comparator<DataAssignment> comp = new DescendingDataAssignmentComparator(DataAssignment.evaluator);
		Collections.sort(totalDataPerDevice, comp);
				
		for (Job dataJob : bufferedJobs){
			totalDataPerDevice.get(FIRST).scheduleJob(dataJob);
			Collections.sort(totalDataPerDevice, comp);
		}

        for (DataAssignment deviceAssignment : totalDataPerDevice) {
            Device current = deviceAssignment.getDevice();
            for (Job job : deviceAssignment.getAssignedJobs()) {
                queueJobTransferring(current, job);
            }
        }
	}

}
