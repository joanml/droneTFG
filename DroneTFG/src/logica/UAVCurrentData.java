package logica;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.javatuples.Quintet;

import com.esotericsoftware.kryo.io.Input;

import uavController.UAVParam;

/**
 * This class generates and object that contains the most recent information
 * received from the UAV.
 */

public class UAVCurrentData {

	private long time; // (ns) Local time when the location was retrieved from the UAV
	private Point2D.Double locationGeo; // (degrees) longitude,latitude coordinates (x=longitude)
	private Point2D.Double locationUTM; // (m) X,Y UTM coordinates
	private double z, zRelative; // (m) Altitude
	private double speed; // (m/s) Currrent speed
	private double acceleration; // (m/s^2) Current acceleration
	private double heading; // (rad) Current heading
	private int[] servo;
	private int[] channel;
	private boolean servo_flag = false;
	private boolean channel_flag = false;
	private boolean z_flag=false;
	private BufferedWriter bfwriter;
	private Path file;
	private String fileName;

	/** Updates the UAV object data. */
	public synchronized void update(long time, Point2D.Double locationGeo, Point2D.Double locationUTM, double z,
			double zRelative, double speed, double heading) {
		this.locationGeo = locationGeo;
		this.locationUTM = locationUTM;
		this.z = z;
		this.zRelative = zRelative;

		z_flag=true;
		
		double acceleration;
		if (this.time != 0) {
			acceleration = (speed - this.speed) / (time - this.time) * 1000000000l;
		} else {
			acceleration = 0.0;
		}
		this.time = time;
		this.speed = speed;
		this.heading = heading;

		// Filtering the acceleration
		double abs = Math.abs(acceleration);
		if (abs <= UAVParam.MAX_ACCELERATION) { // Upper limit
			if (abs < UAVParam.MIN_ACCELERATION) { // White noise
				this.acceleration = 0;
			} else {
				// Filter
				this.acceleration = UAVParam.ACCELERATION_THRESHOLD * acceleration
						+ (1 - UAVParam.ACCELERATION_THRESHOLD) * this.acceleration;
			}
		} else {
			if (acceleration > 0) {
				this.acceleration = UAVParam.MAX_ACCELERATION;
			} else {
				this.acceleration = -UAVParam.MAX_ACCELERATION;
			}
		}
	}

	/**
	 * Returns the current value of the most relevant data:
	 * <p>
	 * Long. time.
	 * <p>
	 * Point2D.Double. UTM coordinates.
	 * <p>
	 * double. Absolute altitude.
	 * <p>
	 * double. Speed.
	 * <p>
	 * double. Acceleration.
	 */
	public synchronized Quintet<Long, java.awt.geom.Point2D.Double, Double, Double, Double> getData() {
		return Quintet.with(this.time, this.locationUTM, this.z, this.speed, this.acceleration);
	}

	/** Returns the current location in UTM coordinates (x,y). */
	public synchronized Point2D.Double getUTMLocation() {
		if (locationUTM == null)
			return locationUTM;
		else
			return new Point2D.Double(this.locationUTM.x, this.locationUTM.y);
	}

	/**
	 * Returns the current location in Geographic coordinates.
	 * <p>
	 * x=longitude, y=latitude.
	 */
	public synchronized Point2D.Double getGeoLocation() {
		if (locationGeo == null)
			return locationGeo;
		else
			return new Point2D.Double(this.locationGeo.x, this.locationGeo.y);
	}

	/** Returns the current relative altitude (m). */
	public synchronized double getZRelative() {
		return this.zRelative;
	}

	/** Returns the current absolute altitude (m). */
	public synchronized double getZ() {
		return this.z;
	}

	/** Returns the current speed (m/s). */
	public synchronized double getSpeed() {
		return this.speed;
	}

	/** Returns the current heading (rad). */
	public synchronized double getHeading() {
		return this.heading;
	}

	public int[] getServo() {
		return servo;
	}

	public void setServo(int[] servo) {
		this.servo = servo;
		servo_flag = true;
	}

	public int[] getChannel() {
		return channel;
	}

	public void setChannel(int[] channel) {
		this.channel = channel;
		channel_flag = true;
	}
	
	public void abrirEscritura(String fileLogsMsg) {
		file = null;
		bfwriter = null;
		fileName = "./" + fileLogsMsg;
		try {
			// De no poner fecha, habría que comprobar si el fichero existe
			// file = Paths.get(fileName);
			// if (Files.exists(file, new LinkOption[]{ LinkOption.NOFOLLOW_LINKS}))
			// Files.delete(file);
			file = Files.createFile(Paths.get(fileName));
			Charset charset = Charset.forName("UTF-8");
			bfwriter = Files.newBufferedWriter(file, charset);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public void escribirBuffer() {
		
		if (bfwriter != null) {
			// Línea de buffer
			String line = "";
				if (channel_flag) {
					line+="Channels: ";
					for (int i = 0; i < channel.length; i++) {
						line+="\tCh " + (i + 1) + ": " + channel[i] + ", ";
					}
					line+="\n";
				}
				if (z_flag) {
					line+="Z: "+z+"\tZRelat: "+zRelative+"\n";
				}
			try {
				bfwriter.write(line);
				bfwriter.newLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public void cerrarBuffer() {
		try{
            bfwriter.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
	}

}
