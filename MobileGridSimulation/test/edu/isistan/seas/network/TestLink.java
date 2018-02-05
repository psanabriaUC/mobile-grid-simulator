package edu.isistan.seas.network;

import edu.isistan.mobileGrid.network.BroadCastLink;

public class TestLink extends BroadCastLink {
	
	
	public TestLink(){
		super(0, 0, null, null);
	}

	@Override
	public long getTransmissionTime(int size) {
		return 500;
	}
}
