package edu.isistan.mobileGrid.persistence;

public interface IPersisterFactory {
	
	IDevicePersister getDevicePersister();
	
	IJobStatsPersister getJobStatsPersister();
	
	IJobTransferedPersister getJobTransferedPersister();

	ISimulationPersister getSimulationPersister();

}
