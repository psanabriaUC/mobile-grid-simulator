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
 * Base class for the implementation of a centralized proxy that assigns jobs to arbitrary amounts for nodes
 * in a grid.
 */
public abstract class SchedulerProxy extends Entity  implements Node, DeviceListener {
	
	/* Size of message buffer for transfers in bytes */
	private static final int MESSAGE_SIZE = 1024 * 1024;// 1mb

    /**
     * Information currently known by the proxy about its different nodes. Maps the Node to the object containing
     * its data.
     */
    private WeakHashMap<Node, DeviceData> deviceDataMap = new WeakHashMap<>();

	public static final int EVENT_JOB_ARRIVE = 1;

    /**
     * Processes an event dispatched by the {@link Simulation}.
     *
     * @param event The event that will be processed.
     */
	public abstract void processEvent(Event event);

    /**
     * A static reference to the proxy so it can be accessed from anywhere.
     */
	public static SchedulerProxy PROXY;

	protected HashMap<String, Device> devices = new HashMap<String,Device>();

	public SchedulerProxy(String name) {
		super(name);
		PROXY = this;
		Simulation.addEntity(this);
		NetworkModel.getModel().addNewNode(this);
		Logger.logEntity(this, "Proxy created", this.getClass().getName());
	}
	
	/**
     * returns the remaining energy of the grid by aggregating the remaining energy of each node
	 * that compose it. Remaining energy of a node is derived from the last state of charge update
	 * message received by the proxy node.
	 * The value is expressed in Joules
     **/
	public double getCurrentAggregatedNodesEnergy(){
		double currentAggregatedEnergy = 0;
        for (Device dev : devices.values()) {
            currentAggregatedEnergy += getJoulesBasedOnLastReportedSOC(dev); //dev.getJoulesBasedOnLastReportedSOC();
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
	
	@Override
	public void onMessageReceived(Message message) {
		Object data = message.getData();

		if (data instanceof UpdateMsg) {
            UpdateMsg msg = (UpdateMsg) data;
            Device device = devices.get(msg.getNodeId());
            Logger.logEntity(this, "Battery update received from device " + msg.getNodeId() +
                    " value=" + msg.getPercentageOfRemainingBattery());
            updateDeviceSOC(device, msg.getPercentageOfRemainingBattery());
            // device.setLastBatteryLevelUpdate(msg.getPercentageOfRemainingBattery());
            JobStatsUtils.registerUpdateMessage(this, (UpdateMsg) data);
		}


        // When we receive a message from a device, we check if we have any data to send to the device we got the
        // message from and start sending it to it.
        Device device = (Device) message.getSource();

        if (!deviceDataMap.get(device).pendingTransfers.isEmpty()) {
            TransferInfo transferInfo = deviceDataMap.get(device).pendingTransfers.peek();
            int messageSize = transferInfo.getMessageSize(MESSAGE_SIZE);

            if (transferInfo.getCurrentIndex() == 0) {
                setJobTotalTransferringTime(transferInfo);
            }

            sendMessage(transferInfo.getDestination(), transferInfo.getData(), messageSize,
                    transferInfo.getCurrentIndex(), transferInfo.isLastMessage());
        }

	}

    public void updateDeviceSOC(Device device, int remainingBatteryPercentage) {
        this.deviceDataMap.get(device).lastReportedStateOfCharge = remainingBatteryPercentage;
    }

    /**
     * returns the available Joules of the device based on the value of the last
     * reported SOC
     */
    public double getJoulesBasedOnLastReportedSOC(Device device) {
        int lastBatterySOC = this.deviceDataMap.get(device).lastReportedStateOfCharge;

        return ((double) ((lastBatterySOC / BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION)
                * device.getTotalBatteryCapacityInJoules())) / (double) (100);
    }

    public int getLastReportedSOC(Device device) {
        return this.deviceDataMap.get(device).lastReportedStateOfCharge;
    }

    private void setJobTotalTransferringTime(TransferInfo transferInfo) {
        // FIXME: this code should be changed/fixed when variable cost of transferring for a node be implemented

        if (transferInfo.getData() instanceof Job) {
            Job job = (Job) transferInfo.getData();

            long currentSimTime = Simulation.getTime();
            long totalTime = transferInfo.getMessagesCount()
                    * NetworkModel.getModel().getTransmissionTime(transferInfo.getDestination(), PROXY, (int) MESSAGE_SIZE);
            totalTime += NetworkModel.getModel().getTransmissionTime(PROXY, transferInfo.getDestination(),
                    transferInfo.getLastMessageSize());
            JobStatsUtils.transfer(job, transferInfo.getDestination(), totalTime, currentSimTime);
        }
    }

    /**
     * Called when a message originating from this entity was successfully received by a node in the network.
     *
     * @param messageSent The message sent.
     */
	@Override
	public void onMessageSentAck(Message messageSent) {
	    // This is the id of the node that received the message.
	    int destinationNodeId = messageSent.getDestination().getId();

	    DeviceData deviceData = this.deviceDataMap.get(messageSent.getDestination());
	    TransferInfo transferInfo = deviceData.pendingTransfers.peek();
	    // Should never be null, but we check just in case.
	    if (transferInfo != null) {
	        // If this is the last fragment of the message we are currently transmitting, we remove the current
            // transfer info from the queue.
	        if (transferInfo.isLastMessage()) {

                deviceData.pendingTransfers.remove();
                deviceData.completedTransfers.add(transferInfo);

                if (transferInfo.getData() instanceof Job) {
                    Job job = (Job) transferInfo.getData();
                    deviceData.incomingJobs--;

                    Logger.logEntity(this, "Job transfer finished ", job.getJobId(), transferInfo.getDestination());
                    JobStatsUtils.setJobTransferCompleted(job, transferInfo.getDestination());
                }
            } else {
	            transferInfo.increaseIndex();
            }

            // If we have more data to send to the device (either additional fragments of the previous message or the
            // first fragment of a new message), and the receiver device is currently not busy, we send the data.
            if (!deviceData.pendingTransfers.isEmpty() && !transferInfo.getDestination().isSending()) {
                TransferInfo nextTransfer = deviceData.pendingTransfers.peek();
                int messageSize = nextTransfer.getMessageSize(MESSAGE_SIZE);

                if (nextTransfer != transferInfo) {
                    setJobTotalTransferringTime(nextTransfer);
                }

                sendMessage(transferInfo.getDestination(), transferInfo.getData(), messageSize,
                        nextTransfer.getCurrentIndex(), nextTransfer.isLastMessage());

            }
        }
	}

	@Override
	public void fail(Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isOnline() {
		return true;
	}

	private long sendMessage(Node destination, Object data, int messageSize, int offset, boolean lastMessage) {
        return NetworkModel.getModel().send(this, destination, destination.getId(),
                messageSize, data, offset, lastMessage);
    }

	public void remove(Device device) {
		this.devices.remove(device.getName());
	}

	public void addDevice(Device device) {
	    this.devices.put(device.getName(), device);
	    this.deviceDataMap.put(device, new DeviceData(device.getInitialSOC()));
	}

	@Override
	public void onDeviceFail(Node e) {
		// TODO Auto-generated method stub
	}

    /**
     * Queues a {@link Job} for transferring as soon as the channel between the proxy and the device becomes available.
     * Jobs are transferred in FIFO order.
     *
     * @param device The device to which assign the job.
     * @param job The job to assign.
     */
	protected void queueJobTransferring(final Device device, final Job job){
        Logger.logEntity(this, "Job assigned to ", job.getJobId(), device);
        // device.incrementIncomingJobs();

        incrementIncomingJobs(device);
        JobStatsUtils.setJobAssigned(job);

        queueMessageTransfer(device, job, job.getInputSize(), new OnMessageSent() {
            @Override
            public void onMessageSent(Node destination, long ETA) {
                long currentSimTime = Simulation.getTime();
                JobStatsUtils.transfer(job, device, ETA - currentSimTime, currentSimTime);
            }
        });
	}

    /**
     * Queues an arbitrary message for transfer to a given recipient.
     *
     * @param destination The receiver of the message.
     * @param data The data to send.
     * @param messageSize The size of the data in bytes.
     */
    protected <T> void queueMessageTransfer(Node destination, T data, long messageSize) {
	    queueMessageTransfer(destination, data, messageSize, null);
    }

    /**
     * Queues an arbitrary message for transfer to a given recipient. Callers may additionally specify a delegate to
     * invoke if we are able to immediately send the message (this only happens if the channel is empty and no previous
     * messages exist in the queue).
     *
     * @param destination The receiver of the message.
     * @param data The data to send.
     * @param messageSize The size of the data in bytes.
     * @param delegate A delegate to invoke in case the message is sent immediately.
     */
	protected <T> void queueMessageTransfer(Node destination, T data, long messageSize, OnMessageSent delegate) {
	    DeviceData deviceData = deviceDataMap.get(destination);

        // If the pending transfers queue for the given device is not empty, we say the transfer channel is busy.
        boolean channelBusy = !deviceData.pendingTransfers.isEmpty();


        long subMessagesCount = (long) Math.ceil(messageSize / (double) MESSAGE_SIZE);
        int lastMessageSize = (int) (messageSize - (subMessagesCount - 1) * MESSAGE_SIZE);

        TransferInfo transferInfo = new TransferInfo<>(destination, data, subMessagesCount, lastMessageSize);
        deviceData.pendingTransfers.add(transferInfo);

        // If the channel is not busy, we send the first message for the given job.
        if (!channelBusy) {

            TransferInfo nextTransferInfo = deviceData.pendingTransfers.peek();
            int packageSize = nextTransferInfo.getMessageSize(MESSAGE_SIZE);

            long time = sendMessage(nextTransferInfo.getDestination(), data, packageSize,
                    nextTransferInfo.getCurrentIndex(), nextTransferInfo.isLastMessage());

            if (delegate != null) {
                delegate.onMessageSent(nextTransferInfo.getDestination(), time);
            }
        }
    }

    protected void incrementIncomingJobs(Node node) {
	    this.deviceDataMap.get(node).incomingJobs++;
    }

	// Getters

    public Collection<Device> getDevices() {
	    return this.devices.values();
    }

    @Override
    public boolean isSending() {
        return false;
    }

    @Override
    public boolean isReceiving() {
        return false;
    }


    public List<TransferInfo> getTransfersPending() {
	    List<TransferInfo> transfers = new ArrayList<>();
	    for (DeviceData deviceData : this.deviceDataMap.values()) {
	        transfers.addAll(deviceData.pendingTransfers);
        }

        return transfers;
    }

    public List<TransferInfo> getTransfersCompleted() {
        List<TransferInfo> transfers = new ArrayList<>();
        for (DeviceData deviceData : this.deviceDataMap.values()) {
            transfers.addAll(deviceData.completedTransfers);
        }

        return transfers;
    }

    /**
     * Gets the number of queued jobs that are in a state of being transferred towards a given device (but haven't
     * been fully sent yet).
     *
     * @param node A node in the grid.
     * @return The amount of jobs queued for transfer against the given node.
     */
    public int getIncomingJobs(Node node) {
        return this.deviceDataMap.get(node).incomingJobs;
    }

    private interface OnMessageSent {
	    void onMessageSent(Node destination, long ETA);
    }

    /**
     * Known information of the proxy for a given {@link Device}.
     */
    private class DeviceData {
        /**
         * This amount of jobs currently enqueued to be transferred to a {@link Device}.
         */
        private int incomingJobs;

        /**
         * The last reported state of charge of a {@link Device} through an {@link UpdateMsg} event. Actual state of
         * charges should actually be slightly lower than what the proxy knows at any given time.
         */
        private int lastReportedStateOfCharge;

        /**
         * Hash map of message transfer queues for each device in the network. Maps the ID of the device
         * to its respective transfer queue.
         */
        private Queue<TransferInfo> pendingTransfers = new LinkedList<>();

        /**
         * Hash map of all messages that have been sent through a given channel with one device. Maps the ID of the
         * device with its respective transfer queue. Used for logging purposes.
         */
        private Queue<TransferInfo> completedTransfers = new LinkedList<>();

        DeviceData(int lastReportedStateOfCharge) {
            incomingJobs = 0;
            this.lastReportedStateOfCharge = lastReportedStateOfCharge;
        }
    }

}