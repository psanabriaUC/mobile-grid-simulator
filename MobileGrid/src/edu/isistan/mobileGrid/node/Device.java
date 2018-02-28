package edu.isistan.mobileGrid.node;

import java.util.*;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.*;
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
 *     <li>Sender's {@link Device#startTransfer(Node, int, Object)}</li>
 *     <li>Receiver's {@link Device#incomingData(Node, int)}</li>
 *     <li>Receiver's {@link Device#onMessageReceived(Message)}</li>
 *     <li>Sender's {@link Device#onMessageSentAck(Message message)}</li>
 * </ul>
 * Jobs are received by this device, executed, and then their completion is reported back to the original sender.
 */
public class Device extends Entity implements Node, DeviceListener {

    public static final int EVENT_TYPE_BATTERY_UPDATE = 0;
    public static final int EVENT_TYPE_CPU_UPDATE = 1;
	public static final int EVENT_TYPE_FINISH_JOB = 2;
	public static final int EVENT_TYPE_DEVICE_START = 3;
	public static final int EVENT_TYPE_STATUS_NOTIFICATION = 4;
	public static final int EVENT_TYPE_SCREEN_ACTIVITY = 5;
	public static final int EVENT_NETWORK_ACTIVITY = 6;

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
	//protected Queue<TransferInfo> transfersPending = new LinkedList<>();
    protected PriorityQueue<TransferInfo> transfersPending = new PriorityQueue<>();

    protected Map<Integer, TransferInfo> transferInfoMap = new HashMap<>();

    /**
     * List of jobs that have been finished and have already been reported to the proxy.
     * This is used for logging purposes only by the simulator.
     */
	protected List<Job> finishedJobsCompleted = new LinkedList<>();

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

    /**
     * Helper for handling all logic related to battery depletion.
     */
	protected BatteryManager batteryManager;

    /**
     * Helper for handling job execution simulation based on available CPU.
     */
	protected ExecutionManager executionManager;

    /**
     * Helper for handling battery depletion to network-related activity.
     */
	protected NetworkEnergyManager networkEnergyManager;

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
        if (data == null) {
            return defaultMessageHandler;
        } else if (data instanceof Job) {
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
        isSending = false;

        int messageSize = (int) NetworkModel.getModel().getAckMessageSizeInBytes();

        if (networkEnergyManager.onReceiveData(messageSent.getDestination(), this, messageSize)) { // if the ack could be processed then the node update
            transferInfoMap.get(messageSent.getId()).increaseIndex();

            getMessageHandler(messageSent.getData()).onMessageSentAck(messageSent);

            // IF FINISH CURRENT TRANSFER
            if (messageSent.isLastMessage()) {
                getMessageHandler(messageSent.getData()).onMessageFullySent(messageSent);
            }

            TransferInfo nextTransfer = transfersPending.peek();
            while (nextTransfer != null && nextTransfer.allMessagesSent()) {
                transfersPending.remove();
                nextTransfer = transfersPending.peek();
            }

            if (!isReceiving && nextTransfer != null) {
                continueTransferring(nextTransfer);
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

            NetworkModel.getModel().send(this, SchedulerProxy.PROXY, transferInfo.getId(), (int) messageSize, transferInfo.getData(),
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
                        && SchedulerProxy.PROXY.getLastReportedSOC(this) - newBatteryLevel >= BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION
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
                break;
            case Device.EVENT_TYPE_SCREEN_ACTIVITY:
                Boolean flag = (Boolean) event.getData();
                this.batteryManager.onUserActivityEvent(flag);
                break;
            case Device.EVENT_NETWORK_ACTIVITY:
                Event.NetworkActivityEventData eventData = (Event.NetworkActivityEventData) event.getData();
                queueMessageTransfer(CloudNode.getInstance(), null, eventData.getMessageSize(), TransferInfo.PRIORITY_HIGH);
                // queueMessageTransfer();
                break;
		}
	}

    protected <T> void queueMessageTransfer(Node destination, T data, long payloadSize) {
	    queueMessageTransfer(destination, data, payloadSize, TransferInfo.PRIORITY_DEFAULT);
    }

    /**
     * Queues a message for transfer to the given recipient. If the communication channel is currently free,
     * the message is sent immediately.
     *
     * @param destination The receiver of this message.
     * @param data The message's payload.
     * @param payloadSize The size of the payload in bytes.
     */
	protected <T> void queueMessageTransfer(Node destination, T data, long payloadSize, int priority) {
        long subMessagesCount = (long) Math.ceil(payloadSize / (double) MESSAGES_BUFFER_SIZE);
        int lastMessageSize = (int) (payloadSize - (subMessagesCount - 1) * MESSAGES_BUFFER_SIZE);
        TransferInfo transferInfo = new TransferInfo<>(SchedulerProxy.PROXY, data, subMessagesCount, lastMessageSize);
        transferInfoMap.put(transferInfo.getId(), transferInfo);
        transferInfo.setPriority(priority);
        transfersPending.add(transferInfo);

        if (transfersPending.size() == 1 && !isReceiving) {
            int messageSize = transferInfo.getMessageSize(MESSAGES_BUFFER_SIZE);
            if (this.networkEnergyManager.onSendData(this, destination, messageSize)) {
                // if energy is enough to send the message

                getMessageHandler(data).onWillSendMessage(transferInfo);

                NetworkModel.getModel().send(this, destination, transferInfo.getId(), messageSize, data,
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
        SchedulerProxy.PROXY.updateDeviceSOC(this, getBatteryLevel());
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

    public List<Job> getFinishedJobTransfersCompleted() {
        return finishedJobsCompleted;
    }

    public int getCurrentTransfersCount() {
        int enqueuedJobs = 0;
        for (TransferInfo transferInfo : transfersPending) {
            if (transferInfo.getData() instanceof Job) enqueuedJobs++;
        }
        return enqueuedJobs;
    }

    public int getCurrentTotalTransferCount() {
        return getCurrentTransfersCount() + finishedJobsCompleted.size();
    }

    public double getAccEnergyInTransferring() {
        return networkEnergyManager.getAccEnergyInTransfering();
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

    /**
     * Returns the state of charge of the device when it joined the grid as a value between 0 and 10.000.000,
     * where the latter means 100%.
     *
     * @return The initial state of charge of the device.
     */
	public int getInitialSOC() {
		return batteryManager.getInitialSOC();
	}

    /**
     * Returns the Joules of the device when it joined the grid.
     *
     * @return The initial amount of energy in the device's battery, in Joules.
     */
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
     * Handler of messages containing {@link Job}s.
     */
    private class JobMessageHandler extends MessageHandler<Job> {

        @Override
        public void onMessageFullyReceived(Message<Job> message) {
            addJob(message.getData());
        }

        @Override
        public void onWillSendMessage(TransferInfo<Job> transferInfo) {
            if (transferInfo.getCurrentIndex() == 0) {
                JobStatsUtils.transferResults(transferInfo.getData(), transferInfo.getDestination(), Simulation.getTime());
                // JobStatsUtils.transferBackInitiated(job);
            }
        }

        @Override
        public void onMessageSentAck(Message<Job> message) {
            int index = message.getOffset() + 1;
            Logger.logEntity2(Device.this, "Success Transfer (" + index + ")", "jobId=" + message.getData().getJobId());
        }

        @Override
        public void onMessageFullySent(Message<Job> message) {
            Job job = message.getData();
            Logger.logEntity(Device.this, "Result completely transferred; jobId=", job.getJobId());
            JobStatsUtils.successTransferBack(job);
            Device.this.incomingJobTransfers.remove(job.getJobId());

            finishedJobsCompleted.add(message.getData());
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
