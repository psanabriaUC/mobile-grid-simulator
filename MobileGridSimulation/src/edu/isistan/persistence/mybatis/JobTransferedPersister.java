package edu.isistan.persistence.mybatis;

import java.sql.SQLException;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;

import edu.isistan.mobileGrid.persistence.IJobTransferedPersister;
import edu.isistan.mobileGrid.persistence.SQLSession;
import edu.isistan.mobileGrid.persistence.DBEntity.JobTransfer;

public class JobTransferedPersister extends IbatisSQLSessionFactory implements IJobTransferedPersister {
	
	@Override
	public void insertJobTransfered(SQLSession session, JobTransfer jobTransfer)
			throws SQLException {
		SqlSession ibatisSession = ((IbatisSQLSession)session).unwrap();
		try{
			ibatisSession.insert("insertJobTransfered", jobTransfer);
		}catch(PersistenceException e){
			e.printStackTrace();
		}
		
	}

}
