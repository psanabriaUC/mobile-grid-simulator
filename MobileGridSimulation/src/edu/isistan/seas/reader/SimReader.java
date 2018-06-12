package edu.isistan.seas.reader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import edu.isistan.gridgain.spi.loadbalacing.energyaware.GridEnergyAwareLoadBalancing;
import edu.isistan.mobileGrid.network.*;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.SchedulerProxy;
import edu.isistan.mobileGrid.persistence.IPersisterFactory;
import edu.isistan.mobileGrid.persistence.ISimulationPersister;
import edu.isistan.mobileGrid.persistence.SQLSession;
import edu.isistan.mobileGrid.persistence.DBEntity.DeviceTuple;
import edu.isistan.mobileGrid.persistence.DBEntity.SimulationTuple;
import edu.isistan.seas.proxy.DeviceComparator;
import edu.isistan.seas.proxy.bufferedproxy.genetic.GAConfiguration;
import edu.isistan.seas.proxy.bufferedproxy.genetic.SimpleGASchedulerProxy;
import edu.isistan.seas.proxy.jobstealing.StealerProxy;
import edu.isistan.seas.proxy.jobstealing.StealingPolicy;
import edu.isistan.seas.proxy.jobstealing.StealingStrategy;
import edu.isistan.seas.proxy.jobstealing.condition.StealingCondition;

public class SimReader {

	private static final String PROXY_NAME = "PROXY";	
	private static IPersisterFactory persisterFactory = null;
	private static int sim_id = -1;
	
	private String line;
	private BufferedReader conf;
	private ReentrantLock simLock = new ReentrantLock();
	private Map<String, DeviceLoader> devices;

    /**
     * Flag to enable or disable energy consumption simulation due to network related tasks (e.g. data transfers).
     */
	private boolean networkEnergyManagementFlag = false;
	private SimulationTuple simulationTuple = new SimulationTuple();	
	//private boolean jobStealer=false;

	/**
	 * File format 
	 * -scheduler
	 * className
	 * OptionalParameters;
	 * -nodes
	 * name;mips;startTime;batteryBase;batteryFull;cpu*
	 * -jobs
	 * file
	 * @param file
	 */
	public void read(String file, boolean storeInDB){
		ISimulationPersister simPersister = persisterFactory.getSimulationPersister(); 
		SQLSession session = simPersister.openSQLSession();
		if(storeInDB) {
            generateSimulationId(session, simPersister);
        }
		try {
            ExecutorService executorService = Executors.newFixedThreadPool(4);

			this.conf=this.getReader(file);
			simulationTuple.setName(file);
			simulationTuple.setStart_time(new Timestamp(System.currentTimeMillis()));
			this.nextLine();
			while(line!=null){
				if(line.startsWith(";loadBalancing:"))
					this.loadScheduler();
				else if (line.startsWith(";GAConfiguration"))
					this.loadGAConfiguration();
				else if (line.startsWith(";comparator"))
					this.loadComparator();
				else if (line.startsWith(";policy"))
					this.loadPolicy();
				else if (line.startsWith(";strategy"))
					this.loadStragegy();
				else if (line.startsWith(";condition"))
					this.loadCondition();
				else if (line.startsWith(";link"))
					this.loadLink();
				else if (line.startsWith(";networkEnergyManagementEnable"))
					this.loadNetworkEnergyManagementFlag();
				else if (line.startsWith(";devicesStatusNotification"))
					this.loadDeviceStatusNotificationPolicy();
				else if (line.startsWith(";nodeFile"))
					this.loadNodes();
				else if (line.startsWith(";batteryFile"))
					this.loadBatteryFile();
				else if (line.startsWith(";batteryFullCpuUsageFile"))
					this.loadCpuFullScreenOffBatteryFile();
				else if (line.startsWith(";batteruFullCpuScreenOnFile"))
					this.loadCpuFullScreenOnBatteryFile();
				else if (line.startsWith(";cpuFile"))
					this.loadCPUFile();
				else if (line.startsWith(";wifiSignalStrength"))
					this.loadWifiSignalStrength();
				else if (line.startsWith(";userActivity"))
					this.loadUserActivity();
				else if (line.startsWith(";networkActivity")) {
				    this.loadNetworkActivity();
                }
				else if (line.startsWith(";jobsEvent"))
                    executorService.execute(this.loadJobs());
				else throw new IllegalStateException(this.line+" is not a valid parameter");
			}

            for(DeviceLoader loader: this.devices.values()) {
				loader.setSimLock(this.simLock);

				executorService.execute(loader);
			}

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

			this.conf.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		updateSimulationTuple(session,simPersister);
		session.commit();
		session.close();		
	}


	private void loadDeviceStatusNotificationPolicy() throws Exception {
		//parse status notification policy
		this.nextLine();
		String[] statusNotificationPolicyParams = this.line.split(" "); 
		long time = Long.parseLong(statusNotificationPolicyParams[1]);
		Device.STATUS_NOTIFICATION_TIME_FREQ = time;
		
		//parse status messages size
		this.nextLine();
		int statusMessageSize = Integer.parseInt((this.line.split(" "))[1]);
		UpdateMsg.STATUS_MSG_SIZE_IN_BYTES = statusMessageSize;
		
		System.out.println("Status message notification frequency (in millis): "+time);
		System.out.println("Status message size (in bytes): " + UpdateMsg.STATUS_MSG_SIZE_IN_BYTES);
		this.nextLine();
		
	}

	private void loadGAConfiguration() throws Exception {
		String[] galineparts = this.line.split(" "); 
		GAConfiguration gaConf = new GAConfiguration(galineparts[1]);
		((SimpleGASchedulerProxy)SchedulerProxy.PROXY).setGenAlgConf(gaConf);
		this.nextLine();
	}

	private void loadWifiSignalStrength() throws IOException {
		this.nextLine();
		while((this.line != null) && (!this.line.startsWith(";"))) {
			StringTokenizer st = new StringTokenizer(line, ";");
			String wifiSignalStrength = st.nextToken();
			String nodeId = st.nextToken().trim();
			DeviceLoader loader = this.devices.get(nodeId);
			if(loader == null) {
                System.err.println("There is no such device " + nodeId);
            }
			loader.setWifiSignalStrength(new Short(wifiSignalStrength));
			this.nextLine();
		}
		
	}

	private void loadNetworkEnergyManagementFlag() throws Exception{
		String[] lineParts = this.line.split(" ");
		if (lineParts.length > 1){
			networkEnergyManagementFlag = Boolean.valueOf(lineParts[1]);			
		}
		System.out.println("NetworkEnergyManagementFlag: "+ networkEnergyManagementFlag);
		//System.out.println("ACK Message size (in bytes): "+ NetworkModel.getModel().getAckMessageSizeInBytes());
		if(simulationTuple.getPolicy().compareTo("") != 0) {
            System.out.println("StealRequest Message size (in bytes): " + Message.STEAL_MSG_SIZE);
        }
		this.nextLine();
		
	}

	@SuppressWarnings("unchecked")
	private void loadCondition() throws Exception {
		DeviceLoader.MANAGER_FACTORY = new JobStealingFactory();
		String clazzName = this.line.split(" ")[1].trim();
		simulationTuple.setCondition(clazzName);
		Class<StealingCondition> clazz=(Class<StealingCondition>)Class.forName(clazzName);
		StealingCondition policy = clazz.newInstance();
		this.setProperties(policy, clazz, this.line.split(" "), 2);
		this.simLock.lock();
		((StealerProxy)SchedulerProxy.PROXY).setCondition(policy);
		this.simLock.unlock();
		this.nextLine();
	}

	@SuppressWarnings("unchecked")
	private void loadLink() throws Exception {
		String clazzName = this.line.split(" ")[1].trim();
		simulationTuple.setLink(clazzName);
		Class<Link> clazz=(Class<Link>)Class.forName(clazzName);
		Link ss = clazz.newInstance();
		this.setProperties(ss,clazz,this.line.split(" "),2);
		this.simLock.lock();
		((SimpleNetworkModel)NetworkModel.getModel()).setDefaultLink(ss);
		this.simLock.unlock();
		this.nextLine();
	}

	@SuppressWarnings("unchecked")
	private void loadStragegy() throws Exception {
		String clazzName = this.line.split(" ")[1].trim();
		simulationTuple.setStrategy(clazzName);
		Class<StealingStrategy> clazz=(Class<StealingStrategy>)Class.forName(clazzName);
		StealingStrategy ss = clazz.newInstance();
		this.setProperties(ss,clazz,this.line.split(" "),2);
		this.simLock.lock();
		((StealerProxy)SchedulerProxy.PROXY).setStrategy(ss);
		this.simLock.unlock();
		this.nextLine();
	}

	private void setProperties(Object ss,
			Class<?> clazz, String[] split, int i)  throws Exception {
		for(int j=i;j<split.length;j++){
			String prop = split[j].trim();
			String[] kv = prop.split("=");
			String name = "set"+kv[0];
			Method m = clazz.getMethod(name, String.class);
			m.invoke(ss, kv[1]);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadPolicy() throws Exception {
		DeviceLoader.MANAGER_FACTORY = new JobStealingFactory();
		String clazzName = this.line.split(" ")[1].trim();
		simulationTuple.setPolicy(clazzName);
		Class<StealingPolicy> clazz=(Class<StealingPolicy>)Class.forName(clazzName);
		StealingPolicy pol = clazz.newInstance();
		this.setProperties(pol, clazz, this.line.split(" "), 2);
		this.simLock.lock();
		((StealerProxy)SchedulerProxy.PROXY).setPolicy(pol);
		this.simLock.unlock();
		this.nextLine();
	}

	@SuppressWarnings("unchecked")
	private void loadComparator() throws Exception {
		String clazzName = this.line.split(" ")[1].trim();
		simulationTuple.setComparator(clazzName);
		Class<DeviceComparator> clazz=(Class<DeviceComparator>)Class.forName(clazzName);
		DeviceComparator comp = clazz.newInstance();
		this.setProperties(comp, clazz, this.line.split(" "), 2);
		this.simLock.lock();
		((GridEnergyAwareLoadBalancing)SchedulerProxy.PROXY).setDevComp(comp);
		this.simLock.unlock();
		this.nextLine();
	}

	private void loadCPUFile() throws IOException {
		this.nextLine();
		while((this.line!=null)&&(!this.line.startsWith(";"))) {
			StringTokenizer st = new StringTokenizer(line, ";");
			String cpuFile = st.nextToken();
			String nodeId = st.nextToken().trim();
			DeviceLoader loader=this.devices.get(nodeId);
			if(loader==null)
				System.err.println("There is no such device "+nodeId);
			loader.setCPUFile(cpuFile);
			this.nextLine();
		}
	}

	private void loadCpuFullScreenOffBatteryFile() throws IOException {
		this.nextLine();
		while(!this.line.startsWith(";")) {
			StringTokenizer st = new StringTokenizer(line, ";");
			String batFile = st.nextToken();
			String nodeId = st.nextToken().trim();
			DeviceLoader loader=this.devices.get(nodeId);
			if(loader==null)
				System.err.println("There is no such device "+nodeId);
			loader.setBatteryCpuFullScreenOffFile(batFile);
			this.nextLine();
		}
	}

    private void loadCpuFullScreenOnBatteryFile() throws IOException {
        this.nextLine();
        while(!this.line.startsWith(";")) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String batFile = st.nextToken();
            String nodeId = st.nextToken().trim();
            DeviceLoader loader=this.devices.get(nodeId);
            if(loader==null)
                System.err.println("There is no such device "+nodeId);
            loader.setBatteryCpuFullScreenOnFile(batFile);
            this.nextLine();
        }
    }

	private void loadBatteryFile() throws IOException {
		this.nextLine();		
		
		StringTokenizer profileSt = new StringTokenizer(line, ";");
		String batbaseProfile = profileSt.nextToken();
		String[] parts = batbaseProfile.split("/");
		batbaseProfile = parts[parts.length-1];
		simulationTuple.setBase_profile(batbaseProfile);
		
		while(!this.line.startsWith(";")) {
			StringTokenizer st = new StringTokenizer(line, ";");
			String batFile = st.nextToken();
			String nodeId = st.nextToken().trim();
			DeviceLoader loader=this.devices.get(nodeId);
			if(loader==null)
				System.err.println("There is no such device "+nodeId);
			loader.setBatteryFile(batFile);
			this.nextLine();
		}
	}

    private void loadUserActivity() throws IOException {
        this.nextLine();

        while(!this.line.startsWith(";")) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String userActivityFile = st.nextToken();
            String nodeId = st.nextToken().trim();

            DeviceLoader loader=this.devices.get(nodeId);
            if(loader == null)
                System.err.println("There is no such device " + nodeId);
            loader.setUserActivityFilePath(userActivityFile);

            this.nextLine();
        }
    }

    private void loadNetworkActivity() throws IOException {
        this.nextLine();

        while(!this.line.startsWith(";")) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String networkActivityFile = st.nextToken();
            String nodeId = st.nextToken().trim();

            DeviceLoader loader=this.devices.get(nodeId);
            if(loader == null)
                System.err.println("There is no such device " + nodeId);
            loader.setNetworkActivityFilePath(networkActivityFile);

            this.nextLine();
        }
    }

	private Thread loadJobs() throws IOException {
		this.nextLine();
		simulationTuple.setJobs_file(this.line);
		Thread jobReader = new JobReader(this.simLock, this.getReader(this.line), this.networkEnergyManagementFlag);
		this.nextLine();
		return jobReader;
	}

	private void loadNodes() throws IOException {
		this.nextLine();
		simulationTuple.setTopology_file(this.line);
		DeviceReader deviceReader = new DeviceReader(this.line, networkEnergyManagementFlag);
		this.devices = deviceReader.getDevices();
		this.nextLine();
	}

	/**
	 * Scheduler
	 * If it is StealerProxy
	 * policy
	 * method;value*
	 * stategy
	 * method;value*
	 * @throws Exception
	 */
	private void loadScheduler() throws Exception {
		String[] schedulerConstructor = this.line.split(" "); 
		String clazzName = schedulerConstructor[1].trim();
		boolean schedulerHasArguments = false;
		String arguments = "";
		
		if (schedulerConstructor.length > 2){
			schedulerHasArguments = true;
			arguments = schedulerConstructor[2].trim();
		}
		simulationTuple.setScheduler(clazzName);
		// TODO: save arguments into the DB
		@SuppressWarnings("unchecked")
		Class<SchedulerProxy> clazz = (Class<SchedulerProxy>)Class.forName(clazzName);
		this.simLock.lock();
		
		if (schedulerHasArguments) {
			clazz.getConstructor(String.class, String.class).newInstance(PROXY_NAME, arguments);
		} else {
			clazz.getConstructor(String.class).newInstance(PROXY_NAME);
		}
		DeviceTuple proxyTuple = new DeviceTuple(PROXY_NAME,0,0,simulationTuple.getSim_id());
		persisterFactory.getDevicePersister().saveDeviceIntoMemory(PROXY_NAME, proxyTuple);
		this.simLock.unlock();
		this.nextLine();
	}

	/*
	 * TODO: Cargar estrategias de stealing
	@SuppressWarnings("unchecked")
	private void loadStrategy() throws Exception{
		Class<StealingStrategy> stClas=(Class<StealingStrategy>)Class.forName(this.line);
		StealingStrategy strategy=stClas.newInstance();
		this.nextLine();
		while(line.contains(";")){
			StringTokenizer st=new StringTokenizer(line,";");
			Method m=stClas.getMethod(st.nextToken(), String.class);
			m.invoke(strategy, st.nextToken());
			this.nextLine();
		}
		((StealerProxy)SchedulerProxy.PROXY).setStrategy(strategy);
	}

	@SuppressWarnings("unchecked")
	private void loadPolicy() throws Exception {
		Class<StealingPolicy> polClas=(Class<StealingPolicy>)Class.forName(this.line);
		StealingPolicy policy=polClas.newInstance();
		this.nextLine();
		while(line.contains(";")){
			StringTokenizer st=new StringTokenizer(line,";");
			Method m=polClas.getMethod(st.nextToken(), String.class);
			m.invoke(policy, st.nextToken());
			this.nextLine();
		}
		((StealerProxy)SchedulerProxy.PROXY).setPolicy(policy);
	}*/

	private BufferedReader getReader(String file) throws FileNotFoundException{
		return new BufferedReader(new FileReader(file));
	}
	
	private void nextLine() throws IOException{
		this.line=this.conf.readLine();
		if(line==null) return;
		this.line=this.line.trim();
		while(line.startsWith("#")||
				line.equals("")){
			this.line=this.conf.readLine();
			if(line==null) return;
			this.line=this.line.trim();
		}
	}


	public static void setPersisterFactory(IPersisterFactory persisterFactory) {
		SimReader.persisterFactory = persisterFactory;
	}

	public SimulationTuple getSimulationTuple() {
		return simulationTuple;
	}

	//this method performs an insert to the table Simulation in order to get the id of the simulation tuple corresponding to this run. 
	private void generateSimulationId(SQLSession session, ISimulationPersister simPersister) {
		
		try {
			simPersister.insertSimulation(session, simulationTuple);
			SimReader.sim_id = simulationTuple.getSim_id();			
		} catch (SQLException e) {			
			e.printStackTrace();
		}
	}

	private void updateSimulationTuple(SQLSession session,ISimulationPersister simPersister) {
		try {
			simPersister.updateSimulation(session, simulationTuple);						
		} catch (SQLException e) {			
			e.printStackTrace();
		}
		
	}
	
	public static int getSim_id() {
		return SimReader.sim_id;		
	}
}
