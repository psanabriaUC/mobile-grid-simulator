package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.jobs.Job;

public class TransferInfo
{
	public Job job;
	public long messagesCount;
	public long currentIndex;
	public long lastMessageSize;
	
	public TransferInfo(Job job, long messagesCount, long currentIndex, long lastMessageSize)
	{
		this.job = job;
		this.messagesCount = messagesCount;
		this.currentIndex = currentIndex;
		this.lastMessageSize = lastMessageSize;
	}
}
