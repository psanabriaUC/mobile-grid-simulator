package edu.isistan.mobileGrid.persistence;

import java.sql.SQLException;

import edu.isistan.mobileGrid.persistence.DBEntity.DeviceTuple;

public interface IDevicePersister extends SQLSessionFactory{
	
	public void saveDeviceIntoMemory(String name, DeviceTuple deviceTuple);
	
	public void insertDevice(SQLSession session, DeviceTuple device) throws SQLException;

	public void insertInMemoryDeviceTuples(SQLSession session);
	
	public DeviceTuple getDevice(String name);

}
