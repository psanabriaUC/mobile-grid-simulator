package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.network.Message;
import edu.isistan.mobileGrid.network.Node;

/**This interface describes methods that cause a decrement in the battery charge of a device that are particularly related to send or onMessageReceived data through the network of the mobile grid
 * Author: mhirsch*/
public interface NetworkEnergyManager {

	/**Called when a device sends data through the network. The true value is returned if
	 * the device could received the message passed as argument*/
	public boolean onSendData(Node source, Node destination, long bytesSent);
	
	/**Called when a device receives data through the network. The true value is returned if
	 * the device could received the message passed as argument*/
	public boolean onReceiveData(Node source, Node destination, long bytesReceived);
	
	/**Returns the wifi Received Signal Strength of the device*/
	public short getWifiRSSI();

	/**this method returns the energy (in Joules) that the device is supposed to waste when sending the
	 * amount of data indicated as argument. Data is expressed in bytes. The value returned is
	 * independent from the available energy of the device
	 * */
	public double getJoulesWastedWhenTransferData(double data);
	
	/**NOTE: remove comment for testing purposes
	 * This method has testing purposes and returns the percentage of energy of the device that was
	 * consumed in networking activity*/
	public double getAccEnergyInTransfering();
	

	/**return true if the network energy management is enabled, false otherwise*/
	public boolean isNetworkEnergyManagementEnable();
	
}
