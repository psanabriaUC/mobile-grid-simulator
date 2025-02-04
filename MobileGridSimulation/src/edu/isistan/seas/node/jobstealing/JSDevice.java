package edu.isistan.seas.node.jobstealing;

import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.mobileGrid.node.*;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;
import edu.isistan.simulator.Event;

public class JSDevice extends Device {

    public JSDevice(String name, BatteryManager bt, ExecutionManager em,
                    NetworkEnergyManager nem) {
        super(name, bt, em, nem);
    }

    public JSDevice(String name, BatteryManager bt, ExecutionManager em,
                    NetworkEnergyManager nem, boolean useBattery) {
        super(name, bt, em, nem, useBattery);
    }

    @Override
    public void startTransfer(Node dst, int id, Object data) {
        if (data instanceof UpdateMsg) {
            this.networkEnergyManager.onSendData(this, dst, UpdateMsg.STATUS_MSG_SIZE_IN_BYTES);
        } else {
            super.startTransfer(dst, id, data);
        }


	    /*
		if (data instanceof Job){		
			Job job = (Job) data;
			this.incomingJobTransfers.put(id, new JobTransfer(job, Simulation.getTime(), dst));

			int msgSize = dst.runsOnBattery()? job.getInputSize() : job.getOutputSize(); //if dst is another device then the data to be sent is the job not the job result.
			Message msg = new Message(id, this, dst, data, msgSize);
			this.networkEnergyManager.onSendData(msg);			
		} else if (data instanceof UpdateMsg) {
            Message msg = new Message(id, this, dst, data, UpdateMsg.STATUS_MSG_SIZE_IN_BYTES);
            this.networkEnergyManager.onSendData(msg);
		}
		*/
    }

    @Override
    public void processEvent(Event event) {
        super.processEvent(event);
        if (event.getEventType() == EVENT_TYPE_BATTERY_UPDATE) {
            ((StealerProxy)SchedulerProxy.PROXY).steal(this);
        }
    }
}
