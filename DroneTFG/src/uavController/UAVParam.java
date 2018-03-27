package uavController;

public class UAVParam {
	
	// Filter to compensate the acceleration oscilation, applied when a new location is received from the UAV
	public static final double ACCELERATION_THRESHOLD = 0.2;	// [0, 1] 1=new value, 0=previous value
	public static final double MIN_ACCELERATION = 0.2;			// (m/s²) low pass filter
	public static final double MAX_ACCELERATION = 5;			// (m/s²) high pass filter

}
