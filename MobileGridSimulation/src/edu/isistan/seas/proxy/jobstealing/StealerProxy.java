package edu.isistan.seas.proxy.jobstealing;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import edu.isistan.gridgain.spi.loadbalacing.energyaware.GridEnergyAwareLoadBalancing;
import edu.isistan.mobileGrid.jobs.Job;
import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.node.jobstealing.JSMessage;
import edu.isistan.seas.proxy.jobstealing.condition.NoJobsCondition;
import edu.isistan.seas.proxy.jobstealing.condition.StealingCondition;
import edu.isistan.simulator.Logger;
import edu.isistan.simulator.Simulation;

/**
 * Scheduler that implements a job stealing policy.
 *
 * TODO: add network energy consumption emulation support
 */
public class StealerProxy extends GridEnergyAwareLoadBalancing {
	private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

	private StealingPolicy policy;
	private StealingStrategy strategy;
	private StealingCondition condition = new NoJobsCondition();
	
	public StealerProxy(String name) {
		super(name);
	}

	public void steal(Device stealer) {
		Device victim = this.strategy.getVictim(this, stealer);
		if(victim == null || !this.condition.canSteal(stealer, victim, this)) return; //TODO: ANALIZAR SI ESTA LINEA INVOLUCRA CONSUMO DE RED O EL CHEQUEO SE REALIZA CON LAS ESTRUCTURAS INTERNAS DE LOS NODOS

        int numberOfJobsToSteal = this.policy.jobsToSteal(stealer, victim);
		Logger.logEntity(stealer, "The device stole " + numberOfJobsToSteal + " jobs from " + victim);
		
		// The proxy sends a steal request to the victim node
        JSMessage jsMessage = new JSMessage(JSMessage.STEAL_REQUEST_TYPE);
		queueMessageTransfer(victim, jsMessage, Message.STEAL_MSG_SIZE);
		
		for(int i = 0; i < numberOfJobsToSteal && victim.isOnline() && stealer.isOnline(); i++) {
			Job job = victim.removeJob(0);
			Logger.logEntity(victim, "Sending stolen job (id) to stealer (st)", job, stealer);
            queueJobTransferring(stealer, job);

            /*
			// long time = NetworkModel.getModel().send(victim, stealer, NEXT_ID.incrementAndGet(), job.getInputSize(), job);

			if (time != 0) { //means that there was a conexion problem between the victim and the stealer
				long currentSimTime = Simulation.getTime();			
				JobStatsUtils.transfer(job, stealer, time-currentSimTime,currentSimTime);
			} else {
                Logger.logEntity(victim, "Fail to send job (id) to stealer (st): broken link.", job, stealer);
            }
            */
		}
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
