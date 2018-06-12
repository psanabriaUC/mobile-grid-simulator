package edu.isistan.seas.proxy;

import java.util.Collections;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.simulator.Logger;

public class RTCScheduler extends DataIntensiveScheduler {

	public RTCScheduler(String name) {
		super(name);		
	}

	@Override
	protected void assignJob(Job job) {
		
		Collections.sort(totalDataPerDevice, new DescendingDataAssignmentComparator(new RemainingDataTransferingEvaluator()));
		DataAssignment d =  totalDataPerDevice.get(FIRST);				
		d.scheduleJob(job);
		
		Device current = totalDataPerDevice.get(FIRST).getDevice();
		Logger.logEntity(this, "Job assigned to ", job.getJobId() ,current);
		JobStatsUtils.setJobAssigned(job);
		incrementIncomingJobs(current);

		queueJobTransferring(current, job);

		/*
		long subMessagesCount = (long) Math.ceil(job.getInputSize() / (double) MESSAGE_SIZE);
		long lastMessageSize = job.getInputSize() - (subMessagesCount - 1) * MESSAGE_SIZE;
		TransferInfo transferInfo = new TransferInfo(current, job, subMessagesCount, 0, lastMessageSize);

		transfersPending.put(job.getJobId(), transferInfo);
		
		if(!lastPendingTransfers.containsKey(current))
		{
			idSend++;
			long messageSize = transferInfo.messagesCount == 1 ? transferInfo.lastMessageSize : MESSAGE_SIZE;

			 // temporal cast (int)messageSize, message size must be long

			long time = NetworkModel.getModel().send(this, current, job.getJobId(), (int) messageSize, job);
			long currentSimTime = Simulation.getTime();
			JobStatsUtils.transfer(job, current, time - currentSimTime, currentSimTime);
		}
		else{
			TransferInfo lastTInfo = lastPendingTransfers.get(current);
			lastTInfo.nextJobId = job.getJobId();
			lastPendingTransfers.remove(current);
		}
		lastPendingTransfers.put(current, transferInfo);

		*/
		
		jobAssignments.put(job, d);
	}

}
