package edu.isistan.baseprofile.usersession.length;

import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;

/**This is a simplified version of the Session length model proposed in the paper "Diversity in smartphone usage".*/
public class Exponential implements SessionLengthModel{
	
	/**The probabilities of different session lengths were discretisized using the EnumeratedRealDistribution. The samples of that distribution
	 * are used to instantiate the exponential function*/
	public static EnumeratedRealDistribution lamdaDistribution = new EnumeratedRealDistribution(new double[]{10d,20d,30d,40d,60d}, new double[]{0.2d,0.2d,0.2d,0.2d,0.2d});
	
	public double lamda;
	
	public ExponentialDistribution sessionLengthDistribution;
	
	public Exponential(){
		lamda = lamdaDistribution.sample();
		sessionLengthDistribution = new ExponentialDistribution(lamda); 
	}
		
	public double getSessionLenghtSample(){
		return sessionLengthDistribution.sample();
	}
	
	@Override
	public String toString(){
		return this.getClass().getSimpleName()+"(lamda"+lamda+")";
	}
}
