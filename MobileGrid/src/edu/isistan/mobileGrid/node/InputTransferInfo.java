package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.jobs.Job;

public class InputTransferInfo extends TransferInfo{
	
	public Device device;
	public int nextJobId;
	public InputTransferInfo(Device device, Job job, long messagesCount, long currentIndex, long lastMessageSize) {
		super(job, messagesCount, currentIndex, lastMessageSize);
		this.device = device;
	}
	
}
