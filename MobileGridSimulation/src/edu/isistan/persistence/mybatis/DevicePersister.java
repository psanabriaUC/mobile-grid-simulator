package edu.isistan.persistence.mybatis;

import java.sql.SQLException;
import java.util.HashMap;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;

import edu.isistan.mobileGrid.persistence.IDevicePersister;
import edu.isistan.mobileGrid.persistence.SQLSession;
import edu.isistan.mobileGrid.persistence.DBEntity.DeviceTuple;

public class DevicePersister extends IbatisSQLSessionFactory implements IDevicePersister {

	private HashMap<String,DeviceTuple> deviceTuples = new HashMap<String,DeviceTuple>();
	
	/**this method saves in memory the Device passed as argument. All Devices saved in memory can be store in DB invoking the method insertInMemoryDevices*/
	@Override
	public void saveDeviceIntoMemory(String name, DeviceTuple deviceTuple){
		deviceTuples.put(name, deviceTuple);		
	}
	
	@Override
	public void insertDevice(SQLSession session, DeviceTuple device) throws SQLException {
		SqlSession ibatisSession = ((IbatisSQLSession)session).unwrap();
		try{
			ibatisSession.insert("insertDevice", device);
		}catch(PersistenceException e){
			e.printStackTrace();
		}

	}
	/**this method stores all not stored tuples of devices that were saved into memory invoking saveDeviceIntoMemory method of the current instance of DevicePersister.*/
	@Override
	public void insertInMemoryDeviceTuples(SQLSession session){
		for (DeviceTuple deviceTuple : deviceTuples.values()){
			if (!deviceTuple.isStored()){
				try{
					SqlSession ibatisSession = ((IbatisSQLSession)session).unwrap();
					ibatisSession.insert("insertDevice", deviceTuple);
				}catch(PersistenceException e){
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public DeviceTuple getDevice(String name) {
		return deviceTuples.get(name);		
	}

}
