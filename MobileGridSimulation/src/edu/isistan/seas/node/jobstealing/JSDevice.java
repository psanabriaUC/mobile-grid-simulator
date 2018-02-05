package edu.isistan.seas.node.jobstealing;

import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.network.NetworkModel.Message;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.network.UpdateMsg;
import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.ExecutionManager;
import edu.isistan.mobileGrid.node.NetworkEnergyManager;
import edu.isistan.simulator.Simulation;

public class JSDevice extends Device {

	public JSDevice(String name, BatteryManager bt, ExecutionManager em,
			NetworkEnergyManager nem) {
		super(name, bt, em, nem);		
	}
	
	@Override
	public void receive(Node scr, int id, Object data) {		
		if (data instanceof Job){
			super.receive(scr, id, data);
		}		
		else
			if(data instanceof Message){
				Message msg = (Message) data;
				if (((String)msg.getAttribute(Message.TYPE)).compareTo(Message.STEAL_REQUEST_TYPE)==0){					
					networkEnergyManager.onReceieveData(msg);
				}
			}
	}
	
	@Override
	public void startTransfer(Node dst, int id, Object data) {		
		if (data instanceof Job){		
			Job job = (Job) data;
			this.jobsBeingTransfered.put(id,job);
			this.transferStarTime.put(id, Simulation.getTime());
			this.sendingTo.put(id, dst);
			int msgSize = dst.runsOnBattery()? job.getInputSize() : job.getOutputSize(); //if dst is another device then the data to be sent is the job not the job result.
			Message msg = NetworkModel.getModel().new Message(id,this,dst,data);
			msg.setAttribute(Message.SIZE, String.valueOf(msgSize));
			this.networkEnergyManager.onSendData(msg);			
		}
		else 
			if (data instanceof UpdateMsg){			
				Message msg = NetworkModel.getModel().new Message(id,this,dst,data);
				msg.setAttribute(Message.SIZE, String.valueOf(UpdateMsg.STATUS_MSG_SIZE_IN_BYTES));
				this.networkEnergyManager.onSendData(msg);
			}
	}	

}
