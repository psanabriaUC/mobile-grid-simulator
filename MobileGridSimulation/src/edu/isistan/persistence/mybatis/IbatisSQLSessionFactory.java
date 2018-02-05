package edu.isistan.persistence.mybatis;

import java.io.IOException;
import java.io.Reader;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import edu.isistan.mobileGrid.persistence.SQLSession;
import edu.isistan.mobileGrid.persistence.SQLSessionFactory;


public class IbatisSQLSessionFactory implements SQLSessionFactory{

	private Integer mux = 1;
	private SqlSessionFactory sqlMapper;
	
	public IbatisSQLSessionFactory(){
		String resource = "sqlMapConfig.xml";
		Reader reader;
		try {
			reader = Resources.getResourceAsReader (resource);
			sqlMapper = new SqlSessionFactoryBuilder().build(reader);
			
		} catch (IOException e) {
			e.printStackTrace();
		}		

	}
	
	
	public SQLSession openSQLSession(){
		synchronized(mux){
			return new IbatisSQLSession(sqlMapper.openSession());
		}
	}

}
