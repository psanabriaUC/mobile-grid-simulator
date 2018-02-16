package edu.isistan.mobileGrid.node;

import java.util.*;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.simulator.Entity;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

/**
 * A representation of a device with a limited power supply belonging to a grid. This object emulates the processing
 * of arbitrary tasks and the battery's energy degradation, both when idle and when subjected to extended workloads.<br/>
 * <br/>
 * When sending messages, the following methods are invoked in order:
 * <ul>
 *     <li>Receiver's {@link Device#incrementIncomingJobs()}</li>
 *     <li>Sender's {@link Device#startTransfer(Node, int, Object)}</li>
 *     <li>Receiver's {@link Device#incomingData(Node, int)}</li>
 *     <li>Receiver's {@link Device#onMessageReceived(Message)}</li>
 *     <li>Sender's {@link Device#onMessageSentAck(Message message)}</li>
 *     <li>Receiver's {@link Device#addJob(Job)}</li>
 * </ul>
 * Jobs are received by this device, executed, and then their completion is reported back to the original sender.
 */
public class Device extends Entity implements Node, DeviceListener {

	public static final int EVENT_TYPE_BATTERY_UPDATE = 0;
	public static final int EVENT_TYPE_CPU_UPDATE = 1;
	public static final int EVENT_TYPE_FINISH_JOB = 2;
	public static final int EVENT_TYPE_DEVICE_START = 3;
	public static final int EVENT_TYPE_STATUS_NOTIFICATION = 4;

	/* Size of message buffer for transfers in bytes */
	public static int MESSAGES_BUFFER_SIZE = 1024 * 1024; // 1mb
                                                                    // //128*1024
																	// //128k
	/**
     * This field defines the update frequency of device status messages. The time is given
	 * in milliseconds. The default value is 1298138 which equates approximately to 20 mins.
	 * 
	 * Providing the '0' value, enables the status notification whose frequency is
	 * dynamically adapted based on the time in which the battery level drops 1%.
     */
	public static long STATUS_NOTIFICATION_TIME_FREQ = 1298138;
	private Event nextStatusNotificationEvent = null;

    /**
     * Queue of messages that need to be transferred through the communication channel.
     */
	protected Queue<TransferInfo> transfersPending = new LinkedList<>();

    /**
     * List of jobs that have been finished and have already been reported to the proxy.
     * This is used for logging purposes only by the simulator.
     */
	protected List<TransferInfo> finishedJobTransfersCompleted = new LinkedList<>();

    /**
     * Flag to indicate this device is currently receiving data from the network.
     */
	protected boolean isReceiving = false;

    /**
     * Flag to indicate this device is currently sending data through the network.
     */
	protected boolean isSending = false;

    /**
     * Map containing the incoming jobs currently being transferred to this device. Maps the IDs of the jobs to their
     * respective {@link JobTransfer} information.
     */
	protected Map<Integer, JobTransfer> incomingJobTransfers = new HashMap<>();

	protected BatteryManager batteryManager;
	protected ExecutionManager executionManager;
	protected NetworkEnergyManager networkEnergyManager;

	// TODO: move these two variables to the proxy side.

    /**
     * Counter for scheduled jobs to be transferred by the proxy. This value may differ from {@link Device#incomingJobTransfers}'size
     * as it is updated immediately once the job has been scheduled to be sent by the proxy.
     */
    protected int jobsScheduledByProxy = 0;

    /**
     * Last known battery level of this device by the proxy. This is not the device's real SOC.
     */
	protected int lastBatteryLevelUpdate;

    /**
     * Message handler for processing messages containing {@link Job} payloads.
     */
	private JobMessageHandler jobMessageHandler = new JobMessageHandler();

    /**
     * Message handler for processing messages containing {@link UpdateMsg} payloads.
     */
	private UpdateMessageHandler updateMessageHandler = new UpdateMessageHandler();

    /**
     * Default message handler for unspecified message subtypes. Does nothing.
     */
	private MessageHandler defaultMessageHandler = new MessageHandler();

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

    /**
     * Gets the message handler associated with the given data class type. Subclasses of {@link Device} that define
     * additional message types should overwrite this method to return their own message handlers, and defer back
     * to this implementation for unknown data types.
     *
     * @param data The data to process.
     * @return The respective {@link MessageHandler} for the given data.
     */
    protected MessageHandler getMessageHandler(Object data) {
        if (data instanceof Job) {
            return jobMessageHandler;
        } else if (data instanceof UpdateMsg) {
            return updateMessageHandler;
        }
        return defaultMessageHandler;
    }

    /**
     * Called when a message is received by this device.
     *
     * @param message The message that just arrived. Note that this message's payload may represent only a fraction
     *                of the size of a full message in case it is too big to be sent at once.
     */
	@Override
	public void onMessageReceived(Message<?> message) {
		isReceiving = false;
        if (networkEnergyManager.onReceiveData(message.getSource(), message.getDestination(), message.getMessageSize())) {
            getMessageHandler(message.getData()).onMessageReceived(message);
            if (transfersPending.size() > 0 && !isSending) {
                TransferInfo tInfo = transfersPending.peek();
                continueTransferring(tInfo);
            }

            if (isMessageFullyReceived(message)) {
                getMessageHandler(message.getData()).onMessageFullyReceived(message);
            }
        } else {
            getMessageHandler(message.getData()).onCouldNotReceiveMessage(message);
        }
	}

	// TODO: we are currently only checking that the last package of the message has been received, this is insufficient,
    // we should be checking that all messages with lower offsets have been received as well.
	private boolean isMessageFullyReceived(Message message) {
	    return message.isLastMessage();
    }

    /**
     * Called when an ACK for a message sent by this device is received back.
     *
     * @param messageSent The message that was sent.
     */
	@Override
	public void onMessageSentAck(Message messageSent) {
        int id = messageSent.getId();
        if (id != 0) {
            isSending = false;
            TransferInfo transferInfo = transfersPending.peek();

            // Sanity checking of the message offset logic.
            if (transferInfo.getCurrentIndex() != messageSent.getOffset()) {
                throw new IllegalStateException("Message offset mismatch. Expected " + transferInfo.getCurrentIndex() +
                        " but got " + messageSent.getOffset());
            }

            long index = transferInfo.getCurrentIndex() + 1;
            int messageSize = (int) NetworkModel.getModel().getAckMessageSizeInBytes();

            if (networkEnergyManager.onReceiveData(messageSent.getDestination(), this, messageSize)) {// if the ack could be processed then the node update
                // internal data structures
                getMessageHandler(messageSent.getData()).onMessageSentAck(transferInfo);

                // IF FINISH CURRENT TRANSFER
                if (transferInfo.isLastMessage()) {
                    TransferInfo jobTransferDone = transfersPending.remove();
                    getMessageHandler(messageSent.getData()).onMessageFullySent(jobTransferDone);

                    if (transfersPending.size() == 0) {
                        return;
                    }
                    transferInfo = transfersPending.peek();
                } else {
                    transferInfo.increaseIndex();
                }

                if (!isReceiving) {
                    continueTransferring(transferInfo);
                }
            }
        }
	}

	private void continueTransferring(TransferInfo transferInfo) {
		long messageSize;
		if (transferInfo.isLastMessage()) {
		    messageSize = transferInfo.getLastMessageSize();
        } else {
		    messageSize = MESSAGES_BUFFER_SIZE;
        }

		if (this.networkEnergyManager.onSendData(this, SchedulerProxy.PROXY, messageSize)) { // return true if energy is enough to send the message
            getMessageHandler(transferInfo.getData()).onWillSendMessage(transferInfo);

            NetworkModel.getModel().send(this, SchedulerProxy.PROXY, 1, (int) messageSize, transferInfo.getData(),
                    transferInfo.getCurrentIndex(), transferInfo.isLastMessage());
		} else {
		    getMessageHandler(transferInfo.getData()).onCouldNotSendMessage(transferInfo);
        }
	}

	@Override
	public void fail(Message message) {
	    getMessageHandler(message.getData()).onMessageSentFailedToArrive(message);

        // long time=Simulation.getTime()-jobStartTransferTime;
        // JobStatsUtils.fail(j);
        // Logger.logEntity(this, "link failed when send job
        // result.","jobId="+j.getJobId());
        // JobStatsUtils.changeLastTransferTime(j,
        // time,jobStartTransferTime);

	}

	@Override
	public boolean isOnline() {
		return this.isActive();
	}

	@Override
	public void processEvent(Event event) {
		switch (event.getEventType()) {
		case Device.EVENT_TYPE_BATTERY_UPDATE:
			int newBatteryLevel = (Integer) event.getData();
			this.batteryManager.onBatteryEvent(newBatteryLevel);

			if (STATUS_NOTIFICATION_TIME_FREQ == 0
					&& lastBatteryLevelUpdate - newBatteryLevel >= BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION
					&& newBatteryLevel > 0) {
				UpdateMsg updateMsg = new UpdateMsg(this.getName(), newBatteryLevel, Simulation.getTime());
				queueMessageTransfer(SchedulerProxy.PROXY, updateMsg, UpdateMsg.STATUS_MSG_SIZE_IN_BYTES);
			}
			break;
		case Device.EVENT_TYPE_CPU_UPDATE:
			this.executionManager.onCPUEvent((Double) event.getData());
			break;
		case Device.EVENT_TYPE_FINISH_JOB:
			Job job = (Job) event.getData();
			this.executionManager.onFinishJob(job);

			queueMessageTransfer(SchedulerProxy.PROXY, job, job.getOutputSize());
			break;
		case Device.EVENT_TYPE_DEVICE_START:
            onStartup();
			break;
		case Device.EVENT_TYPE_STATUS_NOTIFICATION:
			// notify the proxy about my status
			UpdateMsg updateMsg = new UpdateMsg(this.getName(), (int) batteryManager.getCurrentSOC(),
                    Simulation.getTime());
			queueMessageTransfer(SchedulerProxy.PROXY, updateMsg, UpdateMsg.STATUS_MSG_SIZE_IN_BYTES);

			// plan the next status notification event
			if (Device.STATUS_NOTIFICATION_TIME_FREQ > 0){
				long nextNotificationTime = Simulation.getTime() + Device.STATUS_NOTIFICATION_TIME_FREQ;
				this.nextStatusNotificationEvent=Event.createEvent(Event.NO_SOURCE, nextNotificationTime,
                        this.getId(), Device.EVENT_TYPE_STATUS_NOTIFICATION,null);
				Simulation.addEvent(this.nextStatusNotificationEvent);
			}
		}
	}

    /**
     * Queues a message for transfer to the given recipient. If the communication channel is currently free,
     * the message is sent immediately.
     *
     * @param destination The receiver of this message.
     * @param data The message's payload.
     * @param payloadSize The size of the payload in bytes.
     */
	protected <T> void queueMessageTransfer(Node destination, T data, long payloadSize) {
        long subMessagesCount = (long) Math.ceil(payloadSize / (double) MESSAGES_BUFFER_SIZE);
        int lastMessageSize = (int) (payloadSize - (subMessagesCount - 1) * MESSAGES_BUFFER_SIZE);
        TransferInfo transferInfo = new TransferInfo<>(SchedulerProxy.PROXY, data, subMessagesCount, lastMessageSize);
        transfersPending.add(transferInfo);

        if (transfersPending.size() == 1 && !isReceiving) {
            int messageSize = transferInfo.getMessageSize(MESSAGES_BUFFER_SIZE);
            if (this.networkEnergyManager.onSendData(this, destination, messageSize)) {
                // if energy is enough to send the message

                getMessageHandler(data).onWillSendMessage(transferInfo);

                NetworkModel.getModel().send(this, destination, 1, (int) messageSize, data,
                        transferInfo.getCurrentIndex(), transferInfo.isLastMessage());
            } else {
                getMessageHandler(data).onCouldNotSendMessage(transferInfo);
            }
        }
    }

    /**
     * Called when the device boots up.
     */
	private void onStartup() {
        SchedulerProxy.PROXY.addDevice(this);
        this.batteryManager.startWorking();
        JobStatsUtils.deviceJoinTopology(this, this.batteryManager.getStartTime());
        this.lastBatteryLevelUpdate = getBatteryLevel();
        if (Device.STATUS_NOTIFICATION_TIME_FREQ > 0) {
            long nextNotificationTime = Simulation.getTime() + Device.STATUS_NOTIFICATION_TIME_FREQ;
            this.nextStatusNotificationEvent=Event.createEvent(Event.NO_SOURCE, nextNotificationTime, this.getId(),
                    Device.EVENT_TYPE_STATUS_NOTIFICATION,null);
            Simulation.addEvent(this.nextStatusNotificationEvent);
        }
    }

	/**
	 * Called when the device runs out of battery.
	 */
	public void onBatteryDepletion() {
		JobStatsUtils.deviceLeftTopology(this, Simulation.getTime());
		if (nextStatusNotificationEvent != null) {
            Simulation.removeEvent(nextStatusNotificationEvent);
        }
        SchedulerProxy.PROXY.remove(this);
		this.executionManager.shutdown();
		this.batteryManager.shutdown();
		this.setActive(false);

		for (JobTransfer jobTransfer : incomingJobTransfers.values()) {
		    Node destination = jobTransfer.destination;
		    if (destination instanceof DeviceListener) {
                ((DeviceListener) destination).onDeviceFail(this);
            }
        }
	}

    @Override
    public void incomingData(Node scr, int id) {
        // TODO:provide an energy-aware treatment for an incoming data message.
        // For example, enable the wifi to be able onMessageReceived data.
        isReceiving = true;
    }

    @Override
    public void failReception(Node scr, int id) {
        // TODO:provide an energy-aware treatment for a reception failure
        // message. For example, disable the wifi.
    }

    /**
     * Adds a job to the processing queue. This method should be invoked once a job has finished being
     * transferred to this device.
     *
     * @param job The job to process.
     */
    private void addJob(Job job) {
        jobsScheduledByProxy--;
        this.executionManager.addJob(job);
    }

    public Job removeJob(int index) {
        return this.executionManager.removeJob(index);
    }

    @Override
    public void startTransfer(Node dst, int id, Object data) {
        if (data instanceof Job) {
            Job job = (Job) data;
            if (!this.incomingJobTransfers.containsKey(job.getJobId())) {
                incomingJobTransfers.put(job.getJobId(), new JobTransfer(job, Simulation.getTime(), dst));
            }
        }

        isSending = true;
    }

    @Override
    public void onDeviceFail(Node e) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean runsOnBattery() {
        return true;
    }

    public List<TransferInfo> getFinishedJobTransfersCompleted() {
        return finishedJobTransfersCompleted;
    }

    public int getCurrentTransfersCount() {
        int enqueuedJobs = 0;
        for (TransferInfo transferInfo : transfersPending) {
            if (transferInfo.getData() instanceof Job) enqueuedJobs++;
        }
        return enqueuedJobs;
    }

    public int getCurrentTotalTransferCount() {
        return getCurrentTransfersCount() + finishedJobTransfersCompleted.size();
    }

    public double getAccEnergyInTransferring() {
        return networkEnergyManager.getAccEnergyInTransfering();
    }

    public void incrementIncomingJobs() {
        jobsScheduledByProxy++;
    }

    public int getJobsScheduledByProxy() {
        return jobsScheduledByProxy;
    }

	/**
	 * Returns all the jobs assigned to the device.
	 * 
	 * @return The number of jobs assigned to the device.
	 */
	public int getNumberOfJobs() {
		return this.executionManager.getNumberOfJobs();
	}

	/**
	 * Returns waiting jobs on this device.
	 * 
	 * @return The amount of jobs currently waiting to be executed.
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
	 * waste when sending the amount of data indicated as argument.
     *
     * @param data The amount of data transferred in bytes.
	 */
	public double getEnergyWasteInTransferringData(double data) {
		long subMessagesCount = (long) Math.ceil(data / (double) MESSAGES_BUFFER_SIZE);
		long lastMessageSize = (long) data - (subMessagesCount - 1) * MESSAGES_BUFFER_SIZE;
		double energy = (subMessagesCount - 1)
				* networkEnergyManager.getJoulesWastedWhenTransferData(MESSAGES_BUFFER_SIZE);
		energy += networkEnergyManager.getJoulesWastedWhenTransferData(lastMessageSize);
		return energy;
		// networkEnergyManager.getJoulesWastedWhenTransferData(data);
	}

	public double getEnergyPercentageWastedInNetworkActivity() {
		double initialJoules = (((double) getInitialSOC()
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
		// return finishedJobTransfersPending.size()!= 0;
		return isSending;
	}

	@Override
	public boolean isReceiving() {
		return isReceiving;
	}

    /**
     * Utility helper class to encapsulate the information of a job transfer.
     */
	protected static class JobTransfer {
	    private Job job;
	    private long transferStartTime;
	    private Node destination;

        JobTransfer(Job job, long transferStartTime, Node destination) {
            this.job = job;
            this.transferStartTime = transferStartTime;
            this.destination = destination;
        }
    }

    /**
     * Base utility class for handling messages at different stages of their life cycle.
     *
     * @param <T> The type of the message's payload to process.
     */
    protected class MessageHandler<T> {
        /**
         * Called when a message is received. No guarantee is made that the payload is complete.
         *
         * @param message The message received.
         */
	    public void onMessageReceived(Message<T> message) {}

        /**
         * Called after {@link MessageHandler#onMessageReceived(Message)} only if the entire payload of the message
         * has been confirmed to have been received.
         *
         * @param message The message received.
         */
	    public void onMessageFullyReceived(Message<T> message) {}

        /**
         * Called right before sending a message.
         *
         * @param transferInfo The information of the message's transfer state.
         */
	    public void onWillSendMessage(TransferInfo<T> transferInfo) {}

        /**
         * Called when receiving confirmation that a delivered message has been successfully received by the intended
         * receiver.
         *
         * @param transferInfo The information of the message's transfer state.
         */
	    public void onMessageSentAck(TransferInfo<T> transferInfo) {}

        /**
         * Called after {@link MessageHandler#onMessageSentAck(TransferInfo)} only if the entire payload of the message
         * has been confirmed to have been received.
         *
         * @param transferInfo The information of the message's transfer state.
         */
	    public void onMessageFullySent(TransferInfo<T> transferInfo) {};

        /**
         * Called when a message sent by someone else failed to arrive to this {@link Device}.
         *
         * @param message The message sent.
         */
	    public void onCouldNotReceiveMessage(Message<T> message) {}

        /**
         * Called when this {@link Device} does not have enough power to send a message.
         *
         * @param transferInfo The information of the message's transfer state.
         */
	    public void onCouldNotSendMessage(TransferInfo<T> transferInfo) {};

        /**
         * Called when a message sent by this {@link Device} could not be received by its intended recipient.
         *
         * @param message The message sent.
         */
	    public void onMessageSentFailedToArrive(Message<T> message) {};
    }

    /**
     * Handler of messages containing {@link Job}s.
     */
    private class JobMessageHandler extends MessageHandler<Job> {

        @Override
        public void onMessageFullyReceived(Message<Job> message) {
            addJob(message.getData());
        }

        @Override
        public void onMessageSentAck(TransferInfo<Job> transferInfo) {
            int index = transferInfo.getCurrentIndex() + 1;
            Logger.logEntity2(Device.this, "Success Transfer (" + index + "/" +
                    transferInfo.getMessagesCount() + ")", "jobId=" + transferInfo.getData().getJobId());
        }

        @Override
        public void onMessageFullySent(TransferInfo<Job> transferInfo) {
            Job job = transferInfo.getData();
            Logger.logEntity(Device.this, "Result completely transferred; jobId=", job.getJobId());
            JobStatsUtils.successTransferBack(job);
            Device.this.incomingJobTransfers.remove(job.getJobId());

            finishedJobTransfersCompleted.add(transferInfo);
        }


        @Override
        public void onWillSendMessage(TransferInfo<Job> transferInfo) {
            if (transferInfo.getCurrentIndex() == 0) {
                JobStatsUtils.transferResults(transferInfo.getData(), transferInfo.getDestination(), Simulation.getTime());
                // JobStatsUtils.transferBackInitiated(job);
            }
        }

        @Override
        public void onCouldNotReceiveMessage(Message<Job> message) {
            Logger.logEntity(Device.this, "Failed to onMessageReceived job " + message.getData().getJobId());
        }

        @Override
        public void onCouldNotSendMessage(TransferInfo<Job> transferInfo) {
            Logger.logEntity(Device.this, "failed to send job result.", "jobId=" + transferInfo.getData().getJobId(),
                    "pendingJobs=" + transfersPending.size());
        }

        @Override
        public void onMessageSentFailedToArrive(Message<Job> message) {
            incomingJobTransfers.remove(message.getData().getJobId());
        }
    }

    /**
     * Handler of messages containing {@link UpdateMsg}s.
     */
    private class UpdateMessageHandler extends MessageHandler<UpdateMsg> {

        @Override
        public void onWillSendMessage(TransferInfo<UpdateMsg> transferInfo) {
            if (transferInfo.getCurrentIndex() == 0) {
                JobStatsUtils.registerUpdateMessage(Device.this, transferInfo.getData());
            }
        }
    }
}
