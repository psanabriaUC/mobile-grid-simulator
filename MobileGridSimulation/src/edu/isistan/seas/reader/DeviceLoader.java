package edu.isistan.seas.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

import edu.isistan.mobileGrid.network.NetworkModel;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.seas.node.DefaultBatteryManager;
import edu.isistan.seas.node.DefaultExecutionManager;
import edu.isistan.seas.node.DefaultNetworkEnergyManager;
import edu.isistan.seas.node.ProfileData;
import edu.isistan.simulator.Event;
import edu.isistan.simulator.Simulation;

public class DeviceLoader extends Thread {
	
	public static ManagerFactory MANAGER_FACTORY=new DefaultManagerFactory();
	
	private String nodeId;
	private long flops;
	@SuppressWarnings("unused")
	private int maxActiveJobs;
	private String batteryFile;
	private String fullBatteryFile;
	private String CPUFile;
	private boolean networkEnergyManagerEnable;
	
	private ReentrantLock simLock;
	private ReentrantLock eventLock;
	private BufferedReader batteryBasedFile;
	private BufferedReader batteryCPUFile;
	private BufferedReader cpuFile;
	private int startCharge;
	private int startUptime;
	private int dId;
	private int startTime;
	
	//The battery capacity of the device is the value extract from the manufacturer information. It is calculated W x3600 sec
	private long batteryCapacityInJoules;
	
	//Represent the signal strength value of a node (in dBm) with respect to the Access Point when its networking hardware is set in infrastructure mode 
	private short wifiSignalStrength;
	
	public DeviceLoader(String nodeId, long flops, int maxActiveJobs, boolean networkEnergyManagerEnable, short wifiSignalStrength) {
		this.nodeId = nodeId;
		this.flops = flops;
		this.maxActiveJobs = maxActiveJobs;
		this.networkEnergyManagerEnable = networkEnergyManagerEnable;
		this.setBatteryCapacityInJoules(Long.MAX_VALUE);
		this.setWifiSignalStrength(wifiSignalStrength);
	}
	
	public DeviceLoader(String nodeId, long flops, int maxActiveJobs, boolean networkEnergyManagerEnable) {
		this.nodeId = nodeId;
		this.flops = flops;
		this.maxActiveJobs = maxActiveJobs;
		this.networkEnergyManagerEnable = networkEnergyManagerEnable;
		this.setBatteryCapacityInJoules(Long.MAX_VALUE);
	}

	public DeviceLoader(String nodeId, long flops, int maxActiveJobs,
			boolean networkEnergyManagementEnable, long batteryCapacityInJoules) {		
		this.nodeId = nodeId;
		this.flops = flops;
		this.maxActiveJobs = maxActiveJobs;
		this.networkEnergyManagerEnable = networkEnergyManagementEnable;
		this.setBatteryCapacityInJoules(batteryCapacityInJoules);
	}	

	public long getBatteryCapacityInJoules() {
		return batteryCapacityInJoules;
	}

	public void setBatteryCapacityInJoules(long batteryCapacityInJoules) {
		this.batteryCapacityInJoules = batteryCapacityInJoules;
	}

	/**
	 * File format battery
	 * time;charge*
	 * File format cpu
	 * time;cpu*
	 * no blank lines
	 */
	@Override
	public void run() {
		List<ProfileData> base=this.readBattery(this.batteryBasedFile);
		List<ProfileData> cpuFull=this.readBattery(this.batteryCPUFile);
		
		DefaultNetworkEnergyManager nem = MANAGER_FACTORY.createNetworkEnergyManager(networkEnergyManagerEnable, wifiSignalStrength);
		DefaultBatteryManager sb=MANAGER_FACTORY.createBatteryManager(2, startCharge, startUptime, batteryCapacityInJoules);
		for(ProfileData b:base)
			sb.addProfileData(0, b);
		for(ProfileData b:cpuFull)
			sb.addProfileData(1, b);
		
		DefaultExecutionManager se=MANAGER_FACTORY.createExecutionManager();		
		se.setMips(this.flops);
		Device d= MANAGER_FACTORY.createDevice(this.nodeId, sb, se, nem);
		
		simLock.lock();
		NetworkModel.getModel().addNewNode(d);
		Simulation.addEntity(d);
		this.dId=Simulation.getEntityId(this.nodeId);
		simLock.unlock();
		
		sb.setDevice(d);
		se.setDevice(d);
		nem.setDevice(d);
		se.setBatteryManager(sb);
		nem.setBatteryManager(sb);
		sb.setSEASExecutionManager(se);
						
		this.readCPUEvents();
		
		this.eventLock.lock();
		Event e=Event.createEvent(Event.NO_SOURCE, this.startTime, this.dId, Device.EVENT_TYPE_DEVICE_START, null);
		this.eventLock.unlock();
		this.simLock.lock();
		Simulation.addEvent(e);
		this.simLock.unlock();
	}


	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if(this.batteryBasedFile!=null)
			this.batteryBasedFile.close();

		if(this.batteryCPUFile!=null)
			this.batteryCPUFile.close();

		if(this.cpuFile!=null)
			this.cpuFile.close();
	}

	private void readCPUEvents() {
		try {
			String line=this.cpuFile.readLine();
			while(line!=null){
				if(line.trim().equals("")){
					line=this.cpuFile.readLine();
					break;
				}
				StringTokenizer st=new StringTokenizer(line, ";");
				st.nextToken();
				long time=Long.parseLong(st.nextToken())+this.startTime;
				st.nextToken();
				double cpu=Double.parseDouble(st.nextToken());
				Event e;
				
				this.eventLock.lock();
				e=Event.createEvent(Event.NO_SOURCE, time, this.dId, Device.EVENT_TYPE_CPU_UPDATE, new Double(cpu));
				this.eventLock.unlock();
				
				this.simLock.lock();
				Simulation.addEvent(e);
				this.simLock.unlock();
				
				line=this.cpuFile.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	private List<ProfileData> readBattery(BufferedReader rd) {
		List<ProfileData> pd=new ArrayList<ProfileData>();
		try {
			String line=rd.readLine();
			int battery=0;
			double time=0;
			while(line!=null){
				if(line.trim().equals("")){
					line=rd.readLine();			
				} else
				{
					String[] data=line.split(";");
					if(line.startsWith("ADD_NODE;")){
						this.startTime=Integer.parseInt(data[1]);
						line=rd.readLine();
					}
					else
					{
						int nbattery=0;
						double ntime=0;
						if(line.startsWith("LEFT_NODE;")){
							line=rd.readLine();
							nbattery=0;
							ntime=Double.parseDouble(data[1]);
						}
						else if(line.startsWith("NEW_BATTERY_STATE_NODE;")){
							nbattery=Integer.parseInt(data[3]);
							ntime=Double.parseDouble(data[1]);
						}
						double slope=nbattery-battery;
						slope/=(ntime-time);
						if(startCharge==0){
							startCharge=nbattery;
							startUptime=(int) ((ntime-time)*nbattery);
						} else {
							//Ignora eventos iguales
							if(battery!=nbattery)
								pd.add(new ProfileData(nbattery, slope));
						}
						//Ignora eventos iguales
						if(battery!=nbattery){
							battery=nbattery;
							time=ntime;
						}
						line=rd.readLine();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		if(pd.get(pd.size()-1).getToCharge()!=0)
			pd.add(new ProfileData(0, pd.get(pd.size()-1).getSlope()));
		return pd;
	}
	
	public String getBatteryFile() {
		return batteryFile;
	}

	public void setBatteryFile(String batteryFile) throws FileNotFoundException {
		this.batteryFile = batteryFile;
		this.batteryBasedFile = new BufferedReader(new FileReader(new File(this.batteryFile)));
	}

	public String getCPUFile() {
		return CPUFile;
	}

	public void setCPUFile(String cPUFile) throws FileNotFoundException {
		this.CPUFile = cPUFile;
		this.cpuFile = new BufferedReader(new FileReader(new File(this.CPUFile)));
	}

	public String getFullBatteryFile() {
		return fullBatteryFile;
	}

	public void setFullBatteryFile(String fullBatteryFile) throws FileNotFoundException {
		this.fullBatteryFile = fullBatteryFile;
		this.batteryCPUFile = new BufferedReader(new FileReader(new File(this.fullBatteryFile)));
	}

	public void setSimLock(ReentrantLock simLock) {
		this.simLock = simLock;
	}

	public void setEventLock(ReentrantLock eventLock) {
		this.eventLock=eventLock;
	}

	public void setWifiSignalStrength(short wifiSignalStrength) {
		this.wifiSignalStrength = wifiSignalStrength;		
	}

}
