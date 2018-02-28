package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.simulator.Entity;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Simulation;

public class CloudNode extends Entity implements Node {

    private static volatile CloudNode instance;
    private static final Object lock = new Object();

    public static CloudNode getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new CloudNode();
                }
            }
        }
        return instance;
    }

    private CloudNode() {
        super("Cloud Node");

        Simulation.addEntity(this);
        NetworkModel.getModel().addNewNode(this);
    }

    @Override
    public void incomingData(Node scr, int id) {

    }

    @Override
    public void onMessageReceived(Message<?> message) {

    }

    @Override
    public void onMessageSentAck(Message message) {

    }

    @Override
    public void fail(Message message) {

    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public void startTransfer(Node dst, int id, Object data) {

    }

    @Override
    public void failReception(Node scr, int id) {

    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }

    @Override
    public boolean isSending() {
        return false;
    }

    @Override
    public boolean isReceiving() {
        return false;
    }

    @Override
    public void processEvent(Event event) {

    }
}
