package edu.isistan.mobileGrid.persistence;

import java.sql.SQLException;

import edu.isistan.mobileGrid.persistence.DBEntity.JobStatsTuple;

public interface IJobStatsPersister extends SQLSessionFactory{
	
	public void insertJobStats(SQLSession session, JobStatsTuple jobStats) throws SQLException;
	
	public void updateJobStats(SQLSession session, JobStatsTuple jobStats) throws SQLException;

}
