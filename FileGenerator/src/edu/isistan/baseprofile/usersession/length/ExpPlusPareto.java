package edu.isistan.baseprofile.usersession.length;

import org.apache.commons.math3.distribution.EnumeratedRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;

/**According to the work "Diversity in smartphone usage", the session length is modeled as a
 * mix of two distributions: an exponential plus a Pareto distribution.
 * 
 * 			r · Exp(λ) + (1 − r) · Pareto(xm, α)
 * 
 * The exponential distribution models the frequency of user short sessions while Pareto
 * distribution models the frequency of user long sessions. A session represents a user
 * interaction interval where, in Android OS, is when the screen is on or a voice call is
 * active and, in Windows Mobile OS, is when an application is reported to be on foreground.
 * The unit of samples returned by this distribution is in seconds. 
 **/
public class ExpPlusPareto implements SessionLengthModel{
	
	/**Indicates the maximum sample value that will
	 * be returned when invoking the getSessionLenghtSample() method.*/
	private double upper_sample_threshold=Double.MAX_VALUE;
	
	/**Indicates the minimum sample value that will
	 * be returned when invoking the getSessionLenghtSample() method.*/
	private double lower_sample_threshold=0;
	
	/**This distribution is used to instantiate the r parameter of the model*/
	public static EnumeratedRealDistribution rDistribution = new EnumeratedRealDistribution(new double[]{0.95d,0.75d}, new double[]{0.7d,0.3d});
	
	/**The r parameter is the relative mix of the two distributions.
	 * The values of r parameter are obtained in the constructor of the model with a
	 * EnumeratedRealDistribution, although the work that proposed the model describes
	 * a linear function**/
	private double r;
	
	/**This distribution is used to instantiate the xm parameter of the model*/
	public static EnumeratedRealDistribution xmDistribution = new EnumeratedRealDistribution(new double[]{15d,30d,60d}, new double[]{0.5d,0.25d,0.25d});
	
	/**The xm parameter is the location for a Pareto distribution and represents the minimum
	 * possible value of the random variable. The location value that offers the best fit is
	 * the screen timeout value of the user.
	 * Since the work that propose the model defines a function to the possible values of xm
	 * which is very similar to a step function, we again use the EnumeratedRealDistribution
	 * to generate values*/
	private double xm;
	
	/**The lamda is the parameter of the exponential distribution of the model. Although the
	 * work that propose the model defines a quasi-linear function with values between 0.05 and
	 * 0.3, we adopt a constant value of 0.2.
	 * */
	private double lamda;
	
	/**The alpha parameter represent the shape of the Pareto distribution. The work that
	 * propose the model defines a function for this parameter, but we adopt a constant value
	 * 0.2
	 * */
	private double alpha;
	
	/**The exponential distribution of the proposed model that represent the frequency of 
	 * short user sessions*/
	private ExponentialDistribution exponentialDist;
	
	/**The pareto distribution of the proposed model that represent the frequency of 
	 * long user sessions*/
	private ParetoDistribution paretoDist;
	
	
	/**the sampleValueThreshold parameter establishes the maximum values of samples that will
	 * be obtain by invoking getSessionLenghtSample() method.
	 * The unit of samples returned by this distribution is in seconds*/
	public ExpPlusPareto(double minSampleValue, double maxSampleValue){
		
		this.upper_sample_threshold = maxSampleValue;
		this.lower_sample_threshold = minSampleValue;
		r = rDistribution.sample();
		xm = xmDistribution.sample();		
		lamda= (new EnumeratedRealDistribution(new double[]{10d,20d,30d,40d,60d}, new double[]{0.2d,0.2d,0.2d,0.2d,0.2d})).sample();
		alpha= 0.2d;
		
		exponentialDist= new ExponentialDistribution(lamda);
		paretoDist = new ParetoDistribution(xm,alpha);
	}
	
	public double getSessionLenghtSample(){
		 
		double ret=r*exponentialDist.sample()+ (1-r)*paretoDist.sample();
		while (ret < lower_sample_threshold || ret > upper_sample_threshold)
			ret = r*exponentialDist.sample()+ (1-r)*paretoDist.sample();
		return ret;
	}	
	
	@Override
	public String toString(){
		return this.getClass().getSimpleName()+"(lampda="+lamda+"_r="+r+"_xm="+xm+"_lowerSampleThreshold="+this.lower_sample_threshold+"_upperSampleThreshold="+this.upper_sample_threshold+")";
	}
	

}
