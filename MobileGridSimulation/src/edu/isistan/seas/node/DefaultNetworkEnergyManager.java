package edu.isistan.seas.node;

import java.util.HashMap;

import edu.isistan.mobileGrid.jobs.JobStatsUtils;
import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.Node;
import edu.isistan.mobileGrid.node.BatteryManager;
import edu.isistan.mobileGrid.node.Device;
import edu.isistan.mobileGrid.node.NetworkEnergyManager;
import edu.isistan.simulator.Logger;

/**This class contains logic to reflect the energy consumption of a device caused by
 * its networking activity, i.e., caused by the sending and receiving of data through the network. The
 * energy consumption is taken as equal for sending and receiving operations but varies according to
 * device strength of signal. The strength of signal information was extracted from "Characterizing and
 * modeling the impact of wireless signal strength on smartphone battery drain" 
 * All these energy consumptions are informed to the batteryManager that handles battery state updates.
 * Author: mhirsch*/
public class DefaultNetworkEnergyManager implements NetworkEnergyManager {
	private final int TEN_KILOBYTES = 10;
	private final boolean TRANSFER_FROM_DEVICE = true;
	private final boolean TRANSFER_TO_DEVICE = false;
	//private double joulesPerKbSent; //= 0.00558233189056585;this is the cost in joules of sending a kb through the network using TCP	  
	//private double batteryPercentageConsumedPerKbyte; // = 0.0000349244988148514; percentage of the battery capacity that represent each Kb sent through the network 
	private boolean networkEnergyManagementEnable = true;	
	
	//remove comment for testing purposes
	private double accGlobalJoules = 0;
	
	private short wifiRSSI;	
	private Device device;	
	private BatteryManager batteryManager;
	
	
	
	//Contains information about the cost (in Joules) of sending 1 kb through a wifi connection depending on the wifi signal
	//strength (RSSI) measured in dBm. These costs apply when the whole data to be send is 10 Kb or less. 
	private static HashMap<Short,Double> wifiRSSI_joulesPerKb_10kb= new HashMap<Short,Double>(){

	{ put((short)-50,0.00999d);		
		put((short)-80,0.010656d);
		put((short)-85,0.01332d);
		put((short)-90,0.034632d);
	}};
	
	//Contains information about the cost (in Joules) of receiving 1 kb through a wifi connection depending on the wifi signal
	//strength (RSSI) measured in dBm. These costs apply when the whole data to be send is 100 kb. 
	private static HashMap<Short,Double> wifiRSSI_joulesPerKb_100kb= new HashMap<Short,Double>(){

	{ put((short)-50,0.0018648d);		
			put((short)-80,0.0022644d);
			put((short)-85,0.00333d);
			put((short)-90,0.012654d);
	}};
		
	
	public DefaultNetworkEnergyManager(boolean enableNetworkEnergyManagement, short wifiSignalStrength) {
		this.networkEnergyManagementEnable = enableNetworkEnergyManagement;
		wifiRSSI = wifiSignalStrength;
	}

	public BatteryManager getBatteryManager() {
		return batteryManager;
	}
	
	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public void setBatteryManager(BatteryManager batteryManager) {
		this.batteryManager = batteryManager;
	}

	@Override
	public boolean onSendData(Node source, Node destination, long bytesSent) {
		return registerEnergyWaste(source, destination, bytesSent, TRANSFER_FROM_DEVICE);
	}

	@Override
	public boolean onReceiveData(Node source, Node destination, long bytesReceived) {
		return registerEnergyWaste(source, destination, bytesReceived, TRANSFER_TO_DEVICE);
	}

	/**This method accounts the energy wasted of sending or receiving the message passed as arguments. If
	 * transfer could be performed with the available energy then the invocation returns true, otherwise
	 * returns false. If networkEnergyManagementEnable flag is false, then the invocation always returns true.
	 * */
	private boolean registerEnergyWaste(Node source, Node destination, long dataSizeInBytes, boolean transferringType) {
		boolean completedTransfer = true;
		
		if (networkEnergyManagementEnable) {
			double dataSizeInKb = dataSizeInBytes / 1024.0;
			double joulesPerKbSent = dataSizeInKb <= TEN_KILOBYTES ? wifiRSSI_joulesPerKb_10kb.get(wifiRSSI) : wifiRSSI_joulesPerKb_100kb.get(wifiRSSI);									
			double joulesNeedForTransferData = dataSizeInKb * joulesPerKbSent;
			
			/*if((device.isReceiving() && transferingType) || (device.isSending() && !transferingType))
				joulesNeedForTransferData =  joulesNeedForTransferData / 2;*/
			
			//infer the available joules of the node from its current battery percentage
			//  100% ------------------------totalJoules
			//  SOC%-------------------------availableJoules
			double availableJoules = ((batteryManager.getCurrentSOC() * (double)batteryManager.getBatteryCapacityInJoules()) /
                    ((double)100 * (double)BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION));
			Logger.logEnergy2( "registerEnergyWaste","availableJoules="+availableJoules, "joulesNeedForTransferData="+joulesNeedForTransferData);
			
			// 1) calculate the percentage of battery represented by the cost of transfer the KBs of data through the network
			// totalJoules ------------------100%
			// joulesNeedForTransferData-----batteryPercentage%
			// NOTE: in the above calculus a transformation of the batteryPercentage% value to the representation used by the battery manager is included
			double batteryPercentageNeeded = ((joulesNeedForTransferData * (double)100) / (double)batteryManager.getBatteryCapacityInJoules()) *
					(double)BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION;
						
			if (Double.isInfinite(joulesNeedForTransferData) ||
				Double.isNaN(joulesNeedForTransferData) || 
				joulesNeedForTransferData > availableJoules) {
				
				joulesNeedForTransferData = availableJoules;				
				batteryPercentageNeeded = batteryManager.getCurrentSOC();
				dataSizeInKb = availableJoules/joulesPerKbSent;
				completedTransfer = false;				
			}
			
			batteryManager.onNetworkEnergyConsumption(batteryPercentageNeeded);
			if (transferringType == TRANSFER_FROM_DEVICE)
				JobStatsUtils.registerSendingDataEnergy(source, joulesNeedForTransferData,dataSizeInKb/(double)1024);
			else
				JobStatsUtils.registerReceivingDataEnergy(destination, joulesNeedForTransferData, dataSizeInKb/(double)1024);
			
			accGlobalJoules+=joulesNeedForTransferData;
		}
		return completedTransfer;
	}

	@Override
	public boolean isNetworkEnergyManagementEnable() {
		return networkEnergyManagementEnable;
	}
	

	@Override
	public short getWifiRSSI() {
		return wifiRSSI;
	}

	@Override
	/**data is expressed in bytes*/
	public double getJoulesWastedWhenTransferData(double data) {
		double joulesPerKbSent = data <= 10240 ? wifiRSSI_joulesPerKb_10kb.get(wifiRSSI) : wifiRSSI_joulesPerKb_100kb.get(wifiRSSI);		
		return (data/(double)1024) * joulesPerKbSent;
	}

	//remove comment for testing purposes
	@Override
	public double getAccEnergyInTransfering() {
		return accGlobalJoules;
	}
	
}
