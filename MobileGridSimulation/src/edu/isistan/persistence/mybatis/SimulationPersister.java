package edu.isistan.persistence.mybatis;

import java.sql.SQLException;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;

import edu.isistan.mobileGrid.persistence.ISimulationPersister;
import edu.isistan.mobileGrid.persistence.SQLSession;
import edu.isistan.mobileGrid.persistence.DBEntity.SimulationTuple;

public class SimulationPersister extends IbatisSQLSessionFactory implements ISimulationPersister {

	public SimulationPersister() {		
	}


	@Override
	public void insertSimulation(SQLSession session, SimulationTuple simulationTuple) throws SQLException {
		SqlSession ibatisSession = ((IbatisSQLSession)session).unwrap();
		try{
			ibatisSession.insert("insertSimulation", simulationTuple);
		}catch(PersistenceException e){
			e.printStackTrace();
		}
	}


	@Override
	public void updateSimulation(SQLSession session, SimulationTuple simulationTuple) throws SQLException {
		SqlSession ibatisSession = ((IbatisSQLSession)session).unwrap();
		try{
			ibatisSession.update("updateSimulation", simulationTuple);
		}catch(PersistenceException e){
			e.printStackTrace();
		}
		
	}

}
