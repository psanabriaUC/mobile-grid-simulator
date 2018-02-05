package edu.isistan.persistence.mybatis;

import org.apache.ibatis.session.SqlSession;
import edu.isistan.mobileGrid.persistence.SQLSession;


public class IbatisSQLSession implements SQLSession{
	
	SqlSession sessionInstance;

	public IbatisSQLSession(SqlSession sessionInstance) {
		this.sessionInstance = sessionInstance;
	}

	public void close() {
		sessionInstance.close();
		
	}

	public void commit() {
		sessionInstance.commit(true);		
	}
	
	public SqlSession unwrap(){
		return sessionInstance;
	}

}
