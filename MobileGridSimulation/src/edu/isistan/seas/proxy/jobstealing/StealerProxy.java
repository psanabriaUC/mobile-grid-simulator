package edu.isistan.seas.proxy.jobstealing;

import java.util.Collection;

import edu.isistan.gridgain.spi.loadbalacing.energyaware.GridEnergyAwareLoadbalancing;
import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.network.NetworkModel.Message;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.proxy.jobstealing.condition.NoJobsCondition;
import edu.isistan.seas.proxy.jobstealing.condition.StealingCondition;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

public class StealerProxy extends GridEnergyAwareLoadbalancing {

	private StealingPolicy policy;
	private StealingStrategy strategy;
	private StealingCondition condition = new NoJobsCondition();
	
	public StealerProxy(String name) {
		super(name);
	}

	public void steal(Device stealer){
		Device victim=this.strategy.getVictim(this, stealer);
		if(victim==null)return;
		if(!this.condition.canSteal(stealer, victim, this)) return;//TODO: ANALIZAR SI ESTA LINEA INVOLUCRA CONSUMO DE RED O EL CHEQUEO SE REALIZA CON LAS ESTRUCTURAS INTERNAS DE LOS NODOS
		int cant=this.policy.jobsToSteal(stealer, victim);
		Logger.logEntity(stealer, "The device has (Cant) steal jobs from (Device)", cant,victim);
		
		//The proxy send a steal request to the victim node 
		Message msg = NetworkModel.getModel().new Message(1,this,victim,null);
		msg.setAttribute(Message.SIZE, String.valueOf(Message.STEAL_MSG_SIZE));
		msg.setAttribute(Message.TYPE, Message.STEAL_REQUEST_TYPE);		
		NetworkModel.getModel().send(this, victim, idSend++, Message.STEAL_MSG_SIZE, msg);
		
		for(int i=0;i<cant && victim.isOnline() && stealer.isOnline();i++){
			Job job=victim.removeJob(0);
			Logger.logEntity(victim, "Sending stealed job (id) to stealer (st)",job,stealer); 
			long time = NetworkModel.getModel().send(victim, stealer, idSend++, job.getInputSize(), job);
			if (time != 0){//means that there was a conexion problem between the victim and the stealer
				long currentSimTime = Simulation.getTime();			
				JobStatsUtils.transfer(job, stealer, time-currentSimTime,currentSimTime);
			}
			else
				Logger.logEntity(victim, "Fail to send job (id) to stealer (st): broken link.",job,stealer);
		}
	}
	
	public Collection<Device> getDevices(){
		return (Collection<Device>)this.devices.values();
	}

	public StealingPolicy getPolicy() {
		return this.policy;
	}

	public void setPolicy(StealingPolicy policy) {
		this.policy = policy;
		Logger.logEntity(this, "Using StealingPolicy",policy.getClass().getName());
	}

	public StealingStrategy getStrategy() {
		return this.strategy;
	}

	public void setStrategy(StealingStrategy strategy) {
		this.strategy = strategy;
		Logger.logEntity(this, "Using StealingStrategy",strategy.getClass().getName());
	}

	public void setCondition(StealingCondition pol) {
		this.condition = pol;
		Logger.logEntity(this, "Using StealingCondition",pol.getClass().getName());
	}

}
