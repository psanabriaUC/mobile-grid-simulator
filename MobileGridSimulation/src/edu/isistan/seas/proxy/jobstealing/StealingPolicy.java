package edu.isistan.seas.proxy.jobstealing;

import edu.isistan.mobileGrid.node.Device;
/**
 * Stealing policy
 * Todos los set deben tener una version que reciban un string como parametro
 * @author cuchillo
 *
 */
public interface StealingPolicy {

	/**
	 * returns how many jobs should be stolen
	 * @param stealer
	 * @param victim
	 * @return
	 */
	public int jobsToSteal(Device stealer,Device victim);
}
