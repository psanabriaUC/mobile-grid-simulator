package edu.isistan.mobileGrid.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.NetworkModel.Message;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.simulator.Entity;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class Device extends Entity implements Node, DeviceListener {

	public static final int EVENT_TYPE_BATTERY_UPDATE = 0;
	public static final int EVENT_TYPE_CPU_UPDATE = 1;
	public static final int EVENT_TYPE_FINISH_JOB = 2;
	public static final int EVENT_TYPE_DEVICE_START = 3;
	public static final int EVENT_TYPE_STATUS_NOTIFICATION = 4;

	/* Size of message buffer for transfers in bytes */
	public static long MESSAGES_BUFFER_SIZE = 1 * 1024 * 1024;// 1mb
																	// //128*1024
																	// //128k
	/**This field defines the update frequency of device status messages. The time is given
	 * in milliseconds. The default value is 1298138 which equates approximately to 20 mins.
	 * 
	 * Providing the '0' value, enables the status notification whose frequency is
	 * dynamically adapted based on the time in which the battery level drops 1%.*/
	public static long STATUS_NOTIFICATION_TIME_FREQ = 1298138;
	private Event nextStatusNotificationEvent = null;

	protected List<TransferInfo> jobsTransfersPendings = new ArrayList<TransferInfo>();
	protected List<TransferInfo> jobsTransfersCompleted = new ArrayList<TransferInfo>();

	protected boolean isReceiving = false;
	protected boolean isSending = false;
	
	protected int incomingJobs = 0;

	protected Map<Integer, Job> jobsBeingTransfered = new HashMap<Integer, Job>();
	protected Map<Integer, Long> transferStarTime = new HashMap<Integer, Long>();
	protected Map<Integer, Node> sendingTo = new HashMap<Integer, Node>();
	protected BatteryManager batteryManager;
	protected ExecutionManager executionManager;
	protected NetworkEnergyManager networkEnergyManager;

	protected int lastBatteryLevelUpdate;

	/**
	 * when this flag is true the device informs its State Of Charge every time
	 * it decreases in at least one percentage w.r.t the last SOC informed
	 */
	//private boolean informSOC = true;

	public Device(String name, BatteryManager bt, ExecutionManager em, NetworkEnergyManager nem) {
		super(name);
		this.batteryManager = bt;
		this.executionManager = em;
		this.networkEnergyManager = nem;
	}

	public List<TransferInfo> getJobsTransfersPendings() {
		return jobsTransfersPendings;
	}

	public List<TransferInfo> getJobsTransfersCompleted() {
		return jobsTransfersCompleted;
	}

	public int getCurrentTransfersCount() {
		return jobsTransfersPendings.size();
	}

	public int getCurrentTotalTransferCount() {
		return jobsTransfersPendings.size() + jobsTransfersCompleted.size();
	}

	public double getAccEnergyInTransfering() {
		return networkEnergyManager.getAccEnergyInTransfering();
	}
	
	public void incrementIncommingJobs(){
		incomingJobs++;
	}
	public int getIncommingJobs(){
		return incomingJobs;
	}

	@Override
	public void receive(Node scr, int id, Object data) {
		isReceiving = false;
		if (data instanceof Message) {
			Message msj = (Message) data;
			if (networkEnergyManager.onReceieveData(msj)) {
				if (jobsTransfersPendings.size() > 0) {
					TransferInfo tInfo = jobsTransfersPendings.get(0);
					continueTransfering(tInfo);
				}
			} else
				Logger.logEntity(this, "Failed to receive job " + ((Job) msj.getData()).getJobId());
		}
	}

	public void addJob(Job job) {
		incomingJobs -- ;
		this.executionManager.addJob(job);
	}

	@Override
	public void success(int id) {
		if (id != 0) {
			isSending = false;			
			TransferInfo sm = jobsTransfersPendings.get(0);
			long index = sm.currentIndex + 1;
			Message msj = NetworkModel.getModel().new Message(id, null, this, null);
			msj.setAttribute(Message.SIZE, String.valueOf(NetworkModel.getModel().getAckMessageSizeInBytes()));
			if (networkEnergyManager.onReceieveData(msj)) {// if the ack could be processed then the node update
																//internal data structures
				Logger.logEntity2(this, "Success Transfer (" + index + "/" + sm.messagesCount + ")",
							"jobId=" + sm.job.getJobId());
				// IF FINISH CURRENT TRANSFER
				if (sm.currentIndex == sm.messagesCount - 1) {
					Logger.logEntity(this, "Result completely transferred; jobId=", sm.job.getJobId());
					JobStatsUtils.successTrasferBack(jobsBeingTransfered.get(id));

					this.jobsBeingTransfered.remove(id);
					this.transferStarTime.remove(id);
					this.sendingTo.remove(id);

					TransferInfo jobTranfDone = jobsTransfersPendings.remove(0);
					jobsTransfersCompleted.add(jobTranfDone);
					if (jobsTransfersPendings.size() == 0)
						return;
					sm = jobsTransfersPendings.get(0);
				} else
					sm.currentIndex++;
				
				if (!isReceiving) continueTransfering(sm);
			}
		}

	}

	protected void continueTransfering(TransferInfo tInfo) {
		// CONTINUE TRANSFERING
		Job job = tInfo.job;
		Message msg = NetworkModel.getModel().new Message(1, this, SchedulerProxy.PROXY, job);

		long messageSize = tInfo.currentIndex == tInfo.messagesCount - 1 ? tInfo.lastMessageSize : MESSAGES_BUFFER_SIZE;
		msg.setAttribute(Message.SIZE, String.valueOf(messageSize));

		if (this.networkEnergyManager.onSendData(msg)) {// return true if energy
														// is enough to send the
														// message
			/* temporal cast (int)messageSize, message size must be long */
			NetworkModel.getModel().send(this, SchedulerProxy.PROXY, job.getJobId(), (int) messageSize, job);
		} else
			Logger.logEntity(this, "failed to send job result.", "jobId=" + job.getJobId(),
					"pending jobs=" + jobsTransfersPendings.size());
	}

	@Override
	public void fail(int id) {
		Job j = this.jobsBeingTransfered.remove(id);
		if (j != null) {
			this.sendingTo.remove(id);
			long jobStartTransferTime = this.transferStarTime.remove(id);
			// long time=Simulation.getTime()-jobStartTransferTime;
			// JobStatsUtils.fail(j);
			// Logger.logEntity(this, "link failed when send job
			// result.","jobId="+j.getJobId());
			// JobStatsUtils.changeLastTransferTime(j,
			// time,jobStartTransferTime);
		}

	}

	@Override
	public boolean isOnline() {
		return this.isActive();
	}

	@Override
	public void processEvent(Event e) {
		switch (e.getEventType()) {
		case Device.EVENT_TYPE_BATTERY_UPDATE:
			int newBatteryLevel = (Integer) e.getData();
			this.batteryManager.onBatteryEvent(newBatteryLevel);

			if (STATUS_NOTIFICATION_TIME_FREQ == 0
					&& lastBatteryLevelUpdate - newBatteryLevel >= BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION
					&& newBatteryLevel > 0) {
				UpdateMsg updateMsg = new UpdateMsg(this.getName(), newBatteryLevel, Simulation.getTime());
				Message msg = NetworkModel.getModel().new Message(1, this, SchedulerProxy.PROXY, updateMsg);
				msg.setAttribute(Message.SIZE, String.valueOf(UpdateMsg.STATUS_MSG_SIZE_IN_BYTES));
				if (this.networkEnergyManager.onSendData(msg)) {
					JobStatsUtils.registerUpdateMessage(this, updateMsg);
					NetworkModel.getModel().send(this, SchedulerProxy.PROXY, 0,
							UpdateMsg.STATUS_MSG_SIZE_IN_BYTES, updateMsg);
				}
			}
			break;
		case Device.EVENT_TYPE_CPU_UPDATE:
			this.executionManager.onCPUEvent((Double) e.getData());
			break;
		case Device.EVENT_TYPE_FINISH_JOB:
			Job job = (Job) e.getData();
			this.executionManager.onFinishJob(job);

			long subMessagesCount = (long) Math.ceil(job.getOutputSize() / (double) MESSAGES_BUFFER_SIZE);
			long lastMessageSize = job.getOutputSize() - (subMessagesCount - 1) * MESSAGES_BUFFER_SIZE;
			TransferInfo sm = new TransferInfo(job, subMessagesCount, 0, lastMessageSize);
			jobsTransfersPendings.add(sm);

			Message msg = NetworkModel.getModel().new Message(1, this, SchedulerProxy.PROXY, job);
			msg.setAttribute(Message.SIZE,
					String.valueOf(subMessagesCount == 1 ? lastMessageSize : MESSAGES_BUFFER_SIZE));

			if (jobsTransfersPendings.size() == 1 && !isReceiving) {
				if (this.networkEnergyManager.onSendData(msg)) {// return true
																// if energy is
																// enough to
																// send the
																// message
					JobStatsUtils.transferResults(job, SchedulerProxy.PROXY, Simulation.getTime());
					// JobStatsUtils.transferBackInitiated(job);

					/*
					 * temporal cast (int)MESSAGES_BUFFER_SIZE, message size
					 * must be long
					 */
					int size = (int) (subMessagesCount == 1 ? lastMessageSize : MESSAGES_BUFFER_SIZE);
					NetworkModel.getModel().send(this, SchedulerProxy.PROXY, job.getJobId(), size, job);
				} else
					Logger.logEntity(this, "failed to send job result.", "jobId=" + job.getJobId(),
							"pendingJobs=" + jobsTransfersPendings.size());
			}
			break;
		case Device.EVENT_TYPE_DEVICE_START:
			this.batteryManager.startWorking();
			JobStatsUtils.deviceJoinTopology(this, this.batteryManager.getStartTime());
			this.lastBatteryLevelUpdate = getBatteryLevel();
			if (Device.STATUS_NOTIFICATION_TIME_FREQ > 0){
				long nextNotificationTime = Simulation.getTime() + Device.STATUS_NOTIFICATION_TIME_FREQ;
				this.nextStatusNotificationEvent=Event.createEvent(Event.NO_SOURCE, nextNotificationTime, Simulation.getEntityId(this.getName()), Device.EVENT_TYPE_STATUS_NOTIFICATION,null);
				Simulation.addEvent(this.nextStatusNotificationEvent);
			}
			break;
		case Device.EVENT_TYPE_STATUS_NOTIFICATION:
			/**notify the proxy about my status*/
			UpdateMsg updateMsg = new UpdateMsg(this.getName(), (int)batteryManager.getCurrentSOC(), Simulation.getTime());			
			Message statusMsg = NetworkModel.getModel().new Message(1, this, SchedulerProxy.PROXY, updateMsg);
			statusMsg.setAttribute(Message.SIZE, String.valueOf(UpdateMsg.STATUS_MSG_SIZE_IN_BYTES));
			if (this.networkEnergyManager.onSendData(statusMsg)) {
				JobStatsUtils.registerUpdateMessage(this, updateMsg);
				NetworkModel.getModel().send(this, SchedulerProxy.PROXY, 0,
						UpdateMsg.STATUS_MSG_SIZE_IN_BYTES, updateMsg);
			}
			
			/**plan the next status notification event*/
			if (Device.STATUS_NOTIFICATION_TIME_FREQ > 0){
				long nextNotificationTime = Simulation.getTime() + Device.STATUS_NOTIFICATION_TIME_FREQ;
				this.nextStatusNotificationEvent=Event.createEvent(Event.NO_SOURCE, nextNotificationTime, Simulation.getEntityId(this.getName()), Device.EVENT_TYPE_STATUS_NOTIFICATION,null);
				Simulation.addEvent(this.nextStatusNotificationEvent);
			}
		}
	}

	/**
	 * Call when the device runs out of battery
	 */
	public void onBatteryDepletion() {
		JobStatsUtils.deviceLeftTopology(this, Simulation.getTime());
		if (nextStatusNotificationEvent!=null)
			Simulation.removeEvent(nextStatusNotificationEvent);
		this.executionManager.shutdown();
		this.batteryManager.shutdown();
		this.setActive(false);
		for (Integer id : this.sendingTo.keySet()) {
			Node n = this.sendingTo.get(id);
			if (n instanceof DeviceListener)
				((DeviceListener) n).onDeviceFail(this);
		}

	}

	/**
	 * Returns all the jobs assigned to the device
	 * 
	 * @return
	 */
	public int getNumberOfJobs() {
		return this.executionManager.getNumberOfJobs();
	}

	/**
	 * Returns waiting jobs on this device
	 * 
	 * @return
	 */
	public int getWaitingJobs() {
		return this.executionManager.getJobQueueSize();
	}

	public long getMIPS() {
		return this.executionManager.getMIPS();
	}

	public double getCPUUsage() {
		return this.executionManager.getCPUUsage();
	}

	public int getBatteryLevel() {
		return this.batteryManager.getCurrentBattery();
	}

	public long getEstimatedUptime() {
		return this.batteryManager.getEstimatedUptime();
	}

	public long getTotalBatteryCapacityInJoules() {
		return this.batteryManager.getBatteryCapacityInJoules();
	}

	@Override
	public void incomingData(Node scr, int id) {
		// TODO:provide an energy-aware treatment for an incomming data message.
		// For example, enable the wifi to be able receive data.
		isReceiving = true;
	}

	@Override
	public void failReception(Node scr, int id) {
		// TODO:provide an energy-aware treatment for a reception failure
		// message. For example, disable the wifi.
	}

	public Job removeJob(int index) {
		Job j = this.executionManager.getJob(index);
		this.executionManager.removeJob(index);
		return j;
	}

	@Override
	public void startTransfer(Node dst, int id, Object data) {
		if (data instanceof Job) {
			if (!this.jobsBeingTransfered.containsKey(id)) {
				this.jobsBeingTransfered.put(id, (Job) data);
				this.transferStarTime.put(id, Simulation.getTime());
				this.sendingTo.put(id, dst);
			}
			isSending = true;
		}
	}

	@Override
	public void onDeviceFail(Node e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean runsOnBattery() {
		return true;
	}

	public int getLastBatteryLevelUpdate() {
		return lastBatteryLevelUpdate;
	}

	public void setLastBatteryLevelUpdate(int lastBatteryLevelUpdate) {
		this.lastBatteryLevelUpdate = lastBatteryLevelUpdate;
	}

	/**
	 * returns the available Joules of the device based on the value of the last
	 * reported SOC
	 */
	public double getJoulesBasedOnLastReportedSOC() {
		return ((double) ((this.getLastBatteryLevelUpdate() / BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION)
				* this.getTotalBatteryCapacityInJoules())) / (double) (100);
	}

	/**
	 * This method returns the last Wifi Received Signal Strength reported by
	 * the device
	 */
	public short getWifiRSSI() {
		return networkEnergyManager.getWifiRSSI();
	}

	/**
	 * this method returns the energy (in Joules) that the device is supposed to
	 * waste when sending the amount of data indicated as argument. Data is
	 * expressed in bytes.
	 */
	public double getEnergyWasteInTransferingData(double data) {
		long subMessagesCount = (long) Math.ceil(data / (double) MESSAGES_BUFFER_SIZE);
		long lastMessageSize = (long) data - (subMessagesCount - 1) * MESSAGES_BUFFER_SIZE;
		double energy = (subMessagesCount - 1)
				* networkEnergyManager.getJoulesWastedWhenTransferData(MESSAGES_BUFFER_SIZE);
		energy += networkEnergyManager.getJoulesWastedWhenTransferData(lastMessageSize);
		return energy;
		// networkEnergyManager.getJoulesWastedWhenTransferData(data);
	}

	public double getEnergyPercentageWastedInNetworkActivity() {
		double initialJoules = ((double) ((double) getInitialSOC()
				/ (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION)
				* (double) batteryManager.getBatteryCapacityInJoules()) / 100;

		return (networkEnergyManager.getAccEnergyInTransfering() * 100) / initialJoules;
	}

	/** Returns the state of charge of the device when it joint the grid */
	public int getInitialSOC() {
		return batteryManager.getInitialSOC();
	}

	/** Returns the Joules of the device when it joint the grid */
	public double getInitialJoules() {
		return getInitialSOC() * getTotalBatteryCapacityInJoules()
				/ ((double) 100 * (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION);
	}

	@Override
	public boolean isSending() {
		// return jobsTransfersPendings.size()!= 0;
		return isSending;
	}

	@Override
	public boolean isReceiving() {
		return isReceiving;
	}
}
