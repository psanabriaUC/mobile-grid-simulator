package edu.isistan.seas.proxy.jobstealing;

import edu.isistan.mobileGrid.node.Device;
/**
 * Stealing Strategy
 * Todos los set deben tener una version que reciban un string como parametro
 * @author cuchillo
 *
 */
public interface StealingStrategy {

	Device getVictim(StealerProxy sp, Device stealer);
}
