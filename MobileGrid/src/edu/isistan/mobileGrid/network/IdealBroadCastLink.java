package edu.isistan.mobileGrid.network;

public class IdealBroadCastLink extends BroadCastLink {

	public IdealBroadCastLink(){
		super(0, 0, null, null);
	}

	@Override
	public long getTransmissionTime(int size) {
		return 0;
	}

}
