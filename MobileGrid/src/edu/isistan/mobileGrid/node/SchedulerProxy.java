package edu.isistan.mobileGrid.node;

import java.util.HashMap;
import java.util.Iterator;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.simulator.Entity;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public abstract class SchedulerProxy extends Entity  implements Node, DeviceListener{
	
	/* Size of message buffer for transfers in bytes */
	protected static final long MESSAGE_SIZE = 1 * 1024 * 1024;// 1mb

	// JobId->InputTransferInfo
	protected HashMap<Integer, InputTransferInfo> transfersPending = new HashMap<>();
	protected HashMap<Integer, InputTransferInfo> transfersCompleted = new HashMap<>();

	protected HashMap<Device, InputTransferInfo> currentTransfers = new HashMap<>();
	protected HashMap<Device, InputTransferInfo> lastPendingTransfers = new HashMap<>();

	public static final int EVENT_JOB_ARRIVE = 1;

	public abstract void processEvent(Event e);

	public static SchedulerProxy PROXY;
	protected HashMap<String,Device> devices = new HashMap<String,Device>();

	public SchedulerProxy(String name) {
		super(name);
		PROXY=this;
		Simulation.addEntity(this);
		NetworkModel.getModel().addNewNode(this);
		Logger.logEntity(this, "Proxy created", this.getClass().getName());
	}
	
	/**returns the remaining energy of the grid by aggregating the remaining energy of each node
	 * that compose it. Remaining energy of a node is derived from the last state of charge update
	 * message received by the proxy node.
	 * The value is expressed in Joules*/
	public double getCurrentAggregatedNodesEnergy(){
		double currentAggregatedEnergy = 0;
		for (Iterator<Device> iterator = devices.values().iterator(); iterator.hasNext();) {
			Device dev = (Device) iterator.next();
			currentAggregatedEnergy+=dev.getJoulesBasedOnLastReportedSOC();
		}
		return currentAggregatedEnergy;
	}


	@Override
	public void startTransfer(Node dst, int id, Object data) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void incomingData(Node scr, int id) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void failReception(Node scr, int id){
		// TODO Auto-generated method stub
	}
	
	private InputTransferInfo continueTransferringInput(Device device){
		
		InputTransferInfo transferInfo = currentTransfers.get(device);
		long messageSize = transferInfo.currentIndex == transferInfo.messagesCount - 1
				? transferInfo.lastMessageSize : MESSAGE_SIZE;
		/* temporal cast (int)messageSize, message size must be long */
		NetworkModel.getModel().send(this, transferInfo.device, transferInfo.job.getJobId(),
				(int) messageSize, transferInfo.job);
		
		return transferInfo;
	}
	
	private void setJobTotalTransferringTime(InputTransferInfo transferInfo){		
		/**FIXME: this code should be changed/fixed when variable cost of transferring for a node be implemented**/
		long currentSimTime = Simulation.getTime();
		long totalTime = transferInfo.messagesCount
				* NetworkModel.getModel().getTransmissionTime(transferInfo.device, PROXY, (int) MESSAGE_SIZE);
		totalTime += NetworkModel.getModel().getTransmissionTime(PROXY, transferInfo.device,
				(int) transferInfo.lastMessageSize);
		JobStatsUtils.transfer(transferInfo.job, transferInfo.device, totalTime,
				currentSimTime);
	}
	
	@Override
	public void receive(Node scr, int id, Object data) {
		data = ((edu.isistan.mobileGrid.network.NetworkModel.Message)data).getData();
		if (data instanceof Job){						  
			Device device = (Device) scr;
			if (currentTransfers.containsKey(device)) {
				InputTransferInfo transferInfo = continueTransferringInput(device);
				
				if (transferInfo.currentIndex == 0)					
					setJobTotalTransferringTime(transferInfo);				
			}
			
		}else
			if(data instanceof UpdateMsg){
				UpdateMsg msg = (UpdateMsg) data;
				Device device = devices.get(msg.getNodeId());
				Logger.logEntity(this, "Battery update received from device "+msg.getNodeId()+" value="+msg.getPercentageOfRemainingBattery());
				device.setLastBatteryLevelUpdate(msg.getPercentageOfRemainingBattery());				
				JobStatsUtils.registerUpdateMessage(this,(UpdateMsg)data);				
			}
	}	
	
	@Override
	public void success(int id) {
		InputTransferInfo transferInfo = transfersPending.get(id);
		//long index = transferInfo.currentIndex + 1;
		/*Logger.logEntity2(this, "Success Transfer to Device (" + index + "/" + transferInfo.messagesCount + ")",
				"jobId=" + transferInfo.job.getJobId());*/
		Boolean nextJob = false;
		if (transferInfo != null) {
			// IF FINISH CURRENT TRANSFER
			if (transferInfo.currentIndex == transferInfo.messagesCount - 1) {
				Logger.logEntity(this, "Job transfer finished ", transferInfo.job.getJobId(), transferInfo.device);
				transfersPending.remove(id);
				transfersCompleted.put(id, transferInfo);
				Device device = transferInfo.device;
				JobStatsUtils.setJobTransferCompleted(transferInfo.job, device);				
				device.addJob(transferInfo.job);
				currentTransfers.remove(device);
				
				transferInfo = transfersPending.get(transferInfo.nextJobId);
				if (transferInfo == null){//means that the success was of the last transfer
					lastPendingTransfers.remove(device);
					return;	
				}
				nextJob = true;
			} else
				transferInfo.currentIndex++;

			if (!transferInfo.device.isSending()) {
				// CONTINUE TRANSFERING
				long messageSize = transferInfo.currentIndex == transferInfo.messagesCount - 1
						? transferInfo.lastMessageSize : MESSAGE_SIZE;
				/* temporal cast (int)messageSize, message size must be long */
				NetworkModel.getModel().send(this, transferInfo.device, transferInfo.job.getJobId(),
						(int) messageSize, transferInfo.job);
				if (nextJob)
					setJobTotalTransferringTime(transferInfo);
				
			} else {
				currentTransfers.put(transferInfo.device, transferInfo);
			}
		}
	}

	@Override
	public void fail(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isOnline() {
		return true;
	}

	public void remove(Device device) {
		this.devices.remove(device.getName());
	}

	public void addDevice(Device device) {
		this.devices.put(device.getName(),device);
	}

	@Override
	public void onDeviceFail(Node e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isSending() {
		return false;
	}

	@Override
	public boolean isReceiving() {
		return false;
	}
	
	public HashMap<Integer, InputTransferInfo> getTransfersPending() {
		return transfersPending;
	}

	public HashMap<Integer, InputTransferInfo> getTransfersCompleted() {
		return transfersCompleted;
	}
	
	public void queueJobTransferring(Device device, Job job){		
		
			Logger.logEntity(this, "Job assigned to ", job.getJobId(), device);
			device.incrementIncommingJobs();
			JobStatsUtils.setJobAssigned(job);
			
			long subMessagesCount = (long) Math.ceil(job.getInputSize() / (double) MESSAGE_SIZE);
			long lastMessageSize = job.getInputSize() - (subMessagesCount - 1) * MESSAGE_SIZE;
			InputTransferInfo transferInfo = new InputTransferInfo(device, job, subMessagesCount, 0,
					lastMessageSize);
			transfersPending.put(job.getJobId(), transferInfo);
			
			if (!lastPendingTransfers.containsKey(device)){//means that there is no other job currently being transferred to that device
				//idSend++;
				lastPendingTransfers.put(device, transferInfo);
				long messageSize = transferInfo.messagesCount == 1 ? transferInfo.lastMessageSize : MESSAGE_SIZE;
				Logger.logEntity(this, "Initiating Job transferring to ", job.getJobId(), device);
				/*
				 * temporal cast (int)messageSize, message size must be long
				 */
				long time = NetworkModel.getModel().send(this, device, job.getJobId(), (int) messageSize, job);
				long currentSimTime = Simulation.getTime();
				JobStatsUtils.transfer(job, device, time - currentSimTime, currentSimTime);
			}
			else{
				InputTransferInfo lastTInfo = lastPendingTransfers.get(device);
				lastTInfo.nextJobId = job.getJobId();
				lastPendingTransfers.remove(device);
				lastPendingTransfers.put(device, transferInfo);
			}
	}
}