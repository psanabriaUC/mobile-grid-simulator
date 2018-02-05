package edu.isistan.mobileGrid.persistence;

import java.sql.SQLException;

import edu.isistan.mobileGrid.persistence.DBEntity.JobTransfer;

public interface IJobTransferedPersister extends SQLSessionFactory{
	
	public void insertJobTransfered(SQLSession session, JobTransfer jobTransfer) throws SQLException;

}
