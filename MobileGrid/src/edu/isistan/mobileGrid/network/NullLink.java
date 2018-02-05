package edu.isistan.mobileGrid.network;

public class NullLink extends Link {

	public NullLink() {
		super(0, 0, null, null);
	}

	@Override
	public boolean canSend(Node scr, Node dst) {
		return false;
	}
	

}
