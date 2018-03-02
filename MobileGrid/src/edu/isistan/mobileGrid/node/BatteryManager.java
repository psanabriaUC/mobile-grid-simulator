package edu.isistan.mobileGrid.node;

public interface BatteryManager {

	/**Since percentage in device profiles are represent by a 7 digits number, the value of
	 * PROFILE_ONE_PERCENT_REPRESENTATION indicates the scale to represent 1%
	 * */
	public static int PROFILE_ONE_PERCENT_REPRESENTATION = 100000;
	
	/**This represents the minimum difference between two consecutive SOC battery samples of a profile*/
	public static int PROFILE_STEP_REPRESENTATION = 1000;

    /**
     * Called when the device starts running a series of jobs.
     */
	void onBeginExecutingJobs();

    /**
     * Called when a job is finished and no more jobs are available in the queue.
     */
	void onStopExecutingJobs();

	/**
	 * It is called when the network usage make the battery level decreased in a predefined percentage  
	 */
	public void onNetworkEnergyConsumption(double decreasedBatteryPercentage);
	/**
	 * It is call when a battery event occurs
	 * @param level
	 */
	public void onBatteryEvent(int level);

    /**
     * Called whenever the device's screen comes on or off.
     *
     * @param flag true if the device's screen is being turned on, false if it is being turned off.
     */
	void onUserActivityEvent(boolean flag);

	/**
	 * Get current battery level
	 * @return
	 */
	public int getCurrentBattery();
	
	/**return the total battery capacity of a device measured in Joules*/
	public long getBatteryCapacityInJoules();
	/**
	 * Get current estimated uptime
	 * @return
	 */
	public long getEstimatedUptime();
	/**
	 * Call when the device start to work
	 */
	public void startWorking();
	/**
	 * Call when the device shutdown
	 */
	public void shutdown();
	
	/**return the time the device join the topology*/
	public long getStartTime();
	
	/**return the State Of Charge of the device when it joint the topology*/
	public int getInitialSOC();
	
	/**similar to getCurrentBattery but returns a double*/
	public double getCurrentSOC();
	
	
}
