package edu.isistan.mobileGrid.persistence;

import java.sql.SQLException;

import edu.isistan.mobileGrid.persistence.DBEntity.SimulationTuple;

public interface ISimulationPersister extends SQLSessionFactory{
	
	public void insertSimulation(SQLSession session, SimulationTuple simulationTuple) throws SQLException;

	public void updateSimulation(SQLSession session, SimulationTuple simulationTuple) throws SQLException;
}
