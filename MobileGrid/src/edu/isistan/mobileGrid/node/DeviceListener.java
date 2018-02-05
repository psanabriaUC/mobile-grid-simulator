package edu.isistan.mobileGrid.node;

import edu.isistan.mobileGrid.network.Node;

public interface DeviceListener {

	/**
	 * Listen for device disconnection
	 * @param e
	 */
	public void onDeviceFail(Node e);
}
