package edu.isistan.seas.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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

/**
 * Helper for reading and parsing all associated trace files for a given device.
 */
public class DeviceLoader extends Thread {
	
	public static ManagerFactory MANAGER_FACTORY = new DefaultManagerFactory();

    /**
     * The name of this device.
     */
	private String nodeName;
    /**
     * The flops of the device's CPU.
     */
	private long flops;

	@SuppressWarnings("unused")
	private int maxActiveJobs;

    /**
     * The name of the file containing this device's base battery profile for energy consumption estimations.
     */
	private String batteryBaseFile;

    /**
     * The name of the file containing this device's battery profile for 100% CPU utilization and the screen turned off.
     * Used for energy consumption estimations.
     */
    private String batteryFullScreenOffFile;

	/**
	 * The name of the file containing this device's battery profile for 100% CPU utilization and the screen turned on.
     * Used for energy consumption estimations.
	 */
	private String batteryFullScreenOnFile;

    /**
     * The name of the file containing the device's cpu trace. It defines the <b>real</b> cpu percentage use when in
     * the relevant states for this simulation (i.e. when idle, cpu usage is actually around ~10%).
     */
    private String cpuFile;

	/**
	 * The name of the file containing the device's user activity (when the screen is turned ON and OFF).
	 */
	private String userActivityFilePath;

    /**
     * The name of the file containing the device's network activity stemming from user activity and the app environment.
     */
	private String networkActivityFilePath;

    /**
     * Flag to enable or disable energy consumption simulation due to network related tasks (e.g. data transfers).
     */
	private boolean networkEnergyManagerEnable;
	
	private ReentrantLock simLock;

    /**
     * Current battery state of charge as a value between 0 and 10.000.000, where 10.000.000 corresponds to 100%.
     */
	private int startCharge;

    /**
     * Estimated time the battery will last on a device, in milliseconds. Used by the original SEAS implementation.
     */
	private int startUptime;

    /**
     * Numerical identifier of the device.
     */
	private int deviceId;

    /**
     * Time of the simulation, in milliseconds, at which this device was added to the network.
     */
	private int startTime;

    /**
     * The battery capacity of the device is the value extract from the manufacturer information. It is calculated W x3600 sec
     */
	private long batteryCapacityInJoules;

    /**
     * Represent the signal strength value of a node (in dBm) with respect to the Access Point when its networking hardware is set in infrastructure mode
     */
	private short wifiSignalStrength;
	
	public DeviceLoader(String nodeName, long flops, int maxActiveJobs, boolean networkEnergyManagerEnable, short wifiSignalStrength) {
		this.nodeName = nodeName;
		this.flops = flops;
		this.maxActiveJobs = maxActiveJobs;
		this.networkEnergyManagerEnable = networkEnergyManagerEnable;
		this.setBatteryCapacityInJoules(Long.MAX_VALUE);
		this.setWifiSignalStrength(wifiSignalStrength);
	}

	public DeviceLoader(String nodeName, long flops, int maxActiveJobs, boolean networkEnergyManagerEnable) {
		this.nodeName = nodeName;
		this.flops = flops;
		this.maxActiveJobs = maxActiveJobs;
		this.networkEnergyManagerEnable = networkEnergyManagerEnable;
		this.setBatteryCapacityInJoules(Long.MAX_VALUE);
	}

    /**
     * Builds a parser to load the information relevant to a particular node (device) in the network.
     *
     * @param nodeName The name of the node.
     * @param flops The flops of the device's CPU.
     * @param maxActiveJobs The maximum amount of jobs the device can concurrently handle.
     * @param networkEnergyManagementEnable Flag to specify whether energy spent during network communication should be simulated.
     * @param batteryCapacityInJoules Battery capacity in joules.
     */
	public DeviceLoader(String nodeName, long flops, int maxActiveJobs,
                        boolean networkEnergyManagementEnable, long batteryCapacityInJoules) {
		this.nodeName = nodeName;
		this.flops = flops;
		this.maxActiveJobs = maxActiveJobs;
		this.networkEnergyManagerEnable = networkEnergyManagementEnable;
		this.setBatteryCapacityInJoules(batteryCapacityInJoules);
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
		List<ProfileData> batteryBaseProfileData = this.readBattery(this.batteryBaseFile);
		List<ProfileData> batteryFullScreenOffProfileData = this.readBattery(this.batteryFullScreenOffFile);
		// TODO: uncomment
        // List<ProfileData> batteryFullScreenOnProfileData = this.readBattery(this.batteryFullScreenOffFile);
		
		DefaultNetworkEnergyManager networkEnergyManager = MANAGER_FACTORY.createNetworkEnergyManager(networkEnergyManagerEnable, wifiSignalStrength);
		DefaultBatteryManager batteryManager = MANAGER_FACTORY.createBatteryManager(3, startCharge, startUptime, batteryCapacityInJoules);

		for(ProfileData data: batteryBaseProfileData)
			batteryManager.addProfileData(0, data);

		for(ProfileData data: batteryFullScreenOffProfileData)
			batteryManager.addProfileData(1, data);

		// TODO: placeholder, remove once we have profiles for CPU 100% and screen on.
        for(ProfileData data: batteryFullScreenOffProfileData)
            batteryManager.addProfileData(2, data);
		
		DefaultExecutionManager executionManager = MANAGER_FACTORY.createExecutionManager();
		executionManager.setMips(this.flops);

		Device device = MANAGER_FACTORY.createDevice(this.nodeName, batteryManager, executionManager, networkEnergyManager);
		
		simLock.lock();
		NetworkModel.getModel().addNewNode(device);
		Simulation.addEntity(device);
		this.deviceId = Simulation.getEntity(this.nodeName).getId();
		simLock.unlock();

		// Configure dependencies between the device and its managers.
		batteryManager.setDevice(device);
		executionManager.setDevice(device);
		networkEnergyManager.setDevice(device);
		executionManager.setBatteryManager(batteryManager);
		networkEnergyManager.setBatteryManager(batteryManager);
		batteryManager.setSEASExecutionManager(executionManager);
						
		this.readCPUEvents();

		this.readUserActivityEvents();
		this.readNetworkActivityEvents();

		Event event = Event.createEvent(Event.NO_SOURCE, this.startTime, this.deviceId, Device.EVENT_TYPE_DEVICE_START, null);

		this.simLock.lock();
		Simulation.addEvent(event);
		this.simLock.unlock();
	}

    /**
     * Parses the CPU trace file specified by {@link DeviceLoader#cpuFile}. This parameter must not be null.
     */
	private void readCPUEvents() {
	    BufferedReader reader = null;
		try {
		    reader = new BufferedReader(new FileReader(new File(this.cpuFile)));
			String line=reader.readLine();
			while(line != null){
				if(line.trim().equals("")){
					line = reader.readLine();
					break;
				}
				StringTokenizer st = new StringTokenizer(line, ";");
				st.nextToken();
				long time = Long.parseLong(st.nextToken()) + this.startTime;
				st.nextToken();
				double cpu = Double.parseDouble(st.nextToken());
				Event event;

				event = Event.createEvent(Event.NO_SOURCE, time, this.deviceId, Device.EVENT_TYPE_CPU_UPDATE, cpu);
				
				this.simLock.lock();
				Simulation.addEvent(event);
				this.simLock.unlock();
				
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
		    if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Parses the battery trace file specified by <b>batteryTraceFile</b>. Must not be null.
     *
     * @param batteryTraceFile The name of the file containing the battery trace.
     * @return The list of battery profiling samples contained in the file.
     */
	private List<ProfileData> readBattery(String batteryTraceFile) {
		List<ProfileData> profileData = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(batteryTraceFile)));
			String line = reader.readLine();
			int battery = 0;
			double time = 0;
			while(line != null){
				if (line.trim().equals("")) {
					line = reader.readLine();
				} else {
					String[] data = line.split(";");
					if(line.startsWith("ADD_NODE;")){
						this.startTime=Integer.parseInt(data[1]);
						line=reader.readLine();
					} else {
						int nbattery = 0;
						double ntime = 0;
						if(line.startsWith("LEFT_NODE;")) {
							line = reader.readLine();
							nbattery = 0;
							ntime = Double.parseDouble(data[1]);
						}
						else if (line.startsWith("NEW_BATTERY_STATE_NODE;")){
							nbattery = Integer.parseInt(data[3]);
							ntime = Double.parseDouble(data[1]);
						}
						double slope = nbattery - battery;
						slope = slope / (ntime - time);
						if (startCharge == 0) {
							startCharge = nbattery;
							startUptime = (int) ((ntime - time) * nbattery);
						} else {
							// Ignores similar events
							if(battery != nbattery)
								profileData.add(new ProfileData(nbattery, slope));
						}
						// Ignores similar events
						if (battery != nbattery) {
							battery = nbattery;
							time = ntime;
						}
						line=reader.readLine();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

		if (profileData.get(profileData.size() - 1).getToCharge() != 0) {
            profileData.add(new ProfileData(0, profileData.get(profileData.size() - 1).getSlope()));
        }

		return profileData;
	}

	private void readUserActivityEvents() {
	    if (userActivityFilePath != null) {
	        File file = new File(userActivityFilePath);

            Scanner scanner = null;
            try {
                scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine().trim();
                    if (line.indexOf("#") == 0 || line.length() == 0) continue;

                    String[] values = line.split(";");
                    long time = Long.parseLong(values[0]);
                    String flag = values[1];
                    Boolean activity = flag.equals("ON");

                    Event event = Event.createEvent(Event.NO_SOURCE, time, this.deviceId, Device.EVENT_TYPE_SCREEN_ACTIVITY, activity);

                    this.simLock.lock();
                    Simulation.addEvent(event);
                    this.simLock.unlock();

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
        }
    }

    private void readNetworkActivityEvents() {
        if (networkActivityFilePath != null) {
            File file = new File(networkActivityFilePath);

            Scanner scanner = null;
            try {
                scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine().trim();
                    if (line.indexOf("#") == 0 || line.length() == 0) continue;

                    String[] values = line.split(";");
                    long time = Long.parseLong(values[0]);
                    int messageSize = Integer.parseInt(values[1]);
                    String flag = values[2];

                    Event.NetworkActivityEventData eventData = new Event.NetworkActivityEventData(messageSize, flag.equals("IN"));

                    Event event = Event.createEvent(Event.NO_SOURCE, time, this.deviceId, Device.EVENT_NETWORK_ACTIVITY, eventData);

                    this.simLock.lock();
                    Simulation.addEvent(event);
                    this.simLock.unlock();

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
        }
    }

    // Getters and setters.

    public String getBatteryFile() {
        return batteryBaseFile;
    }

    public void setBatteryFile(String batteryFile) throws FileNotFoundException {
        this.batteryBaseFile = batteryFile;
        if (!new File(this.batteryBaseFile).exists()) {
            throw new FileNotFoundException();
        }
    }

    public String getCPUFile() {
        return cpuFile;
    }

    public void setCPUFile(String cpuFile) throws FileNotFoundException {
        this.cpuFile = cpuFile;
        if (!new File(this.cpuFile).exists()) {
            throw new FileNotFoundException();
        }
    }

    public String getFullBatteryFile() {
        return batteryFullScreenOffFile;
    }

    public void setBatteryCpuFullScreenOffFile(String fullBatteryFile) throws FileNotFoundException {
        this.batteryFullScreenOffFile = fullBatteryFile;
        if (!new File(this.batteryFullScreenOffFile).exists()) {
            throw new FileNotFoundException();
        }
    }

    public void setBatteryCpuFullScreenOnFile(String batteryFullScreenOnFile) {
        this.batteryFullScreenOnFile = batteryFullScreenOnFile;
    }

    public void setUserActivityFilePath(String userActivityFile) {
        this.userActivityFilePath = userActivityFile;
    }

    public void setNetworkActivityFilePath(String networkActivityFilePath) {
        this.networkActivityFilePath = networkActivityFilePath;
    }

    public void setSimLock(ReentrantLock simLock) {
        this.simLock = simLock;
    }

    public void setWifiSignalStrength(short wifiSignalStrength) {
        this.wifiSignalStrength = wifiSignalStrength;
    }

    public long getBatteryCapacityInJoules() {
        return batteryCapacityInJoules;
    }

    public void setBatteryCapacityInJoules(long batteryCapacityInJoules) {
        this.batteryCapacityInJoules = batteryCapacityInJoules;
    }
}
