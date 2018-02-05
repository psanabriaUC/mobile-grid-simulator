package edu.isistan.seas.reader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import edu.isistan.mobileGrid.persistence.IDevicePersister;
import edu.isistan.mobileGrid.persistence.IPersisterFactory;
import edu.isistan.mobileGrid.persistence.DBEntity.DeviceTuple;

public class DeviceReader {


	private BufferedReader conf;
	private List<DeviceLoader> devices;
	private Map<String,DeviceLoader> idDevices;
	public static IPersisterFactory persisterFactory = null;
		 
	
	private String line;
	
	public DeviceReader(String file, boolean networkEnergyManagementEnable) throws IOException {
		this.conf=this.getReader(file);
		this.devices = new ArrayList<DeviceLoader>();
		this.idDevices = new HashMap<String, DeviceLoader>();
		this.nextLine();
		boolean batteryCapacityUndefined = false;
		IDevicePersister devicePersister = persisterFactory.getDevicePersister(); 
		int sim_id = SimReader.getSim_id();
		while (this.line!=null){
			StringTokenizer st=new StringTokenizer(line, ";");
			String nodeId=st.nextToken();
			long flops=Long.parseLong(st.nextToken());
			int maxActiveJobs=Integer.parseInt(st.nextToken());
			long batteryCapacityInJoules = Long.MAX_VALUE;
			if(st.hasMoreTokens()){
				batteryCapacityInJoules=Long.parseLong(st.nextToken());
			}
			else{
				batteryCapacityUndefined = true;
			}
			DeviceLoader loader=new DeviceLoader(nodeId,flops,maxActiveJobs, networkEnergyManagementEnable, batteryCapacityInJoules);
			DeviceTuple dt =  new DeviceTuple(nodeId,flops,batteryCapacityInJoules,sim_id);
			devicePersister.saveDeviceIntoMemory(nodeId, dt);
			
			this.devices.add(loader);
			this.idDevices.put(nodeId, loader);
			this.nextLine();
		}
		
		if (batteryCapacityUndefined){
			System.out.println("[WARN] At least one node has no battery capacity defined and the value of Long.MAX_VALUE is being used instead");
		}
		this.conf.close();
	}

	public List<DeviceLoader> getDevices() {
		return this.devices;
	}

	public Map<String, DeviceLoader> getIdDevices() {
		return this.idDevices;
	}
	
	
	private BufferedReader getReader(String file) throws FileNotFoundException{
		return new BufferedReader(new FileReader(file));
	}
	
	private void nextLine() throws IOException{
		this.line=this.conf.readLine();
		if(line==null) return;
		this.line=this.line.trim();
		while(line.startsWith("#")||
				line.equals(""))
			this.line=this.conf.readLine().trim();
	}

	public static IPersisterFactory getPersisterFactory() {
		return persisterFactory;
	}

	public static void setPersisterFactory(IPersisterFactory persisterFactory) {
		DeviceReader.persisterFactory = persisterFactory;
	}

}
