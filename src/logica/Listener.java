package logica;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Properties;

import org.mavlink.IMAVLinkMessage;
import org.mavlink.MAVLinkReader;
import org.mavlink.messages.IMAVLinkMessageID;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.ardupilotmega.msg_global_position_int;
import org.mavlink.messages.ardupilotmega.msg_heartbeat;
import org.mavlink.messages.ardupilotmega.msg_rc_channels_raw;

import api.GUIHelper;
import api.pojo.UTMCoordinates;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import logica.Parametros.Status;

public class Listener extends Thread {

	DatagramSocket socketListener;
	DatagramPacket paquete;
	byte[] buffer;
	long tiempo;

	ByteArrayInputStream bin;
	DataInputStream din;
	DataOutputStream dout;
	MAVLinkReader reader;
	MAVLinkMessage msg;
	boolean continuar;

	// Acceso al puerto serie
	SerialPort serialPort;
	InputStream in;
	OutputStream out;
	
	private BufferedWriter bfwriter;
	private Path file;

	// Sigue el número de paquetes que llegan de cada tipo
	long munServo = 0;
	long munRCChanels = 0;
	long numPosition = 0;
	long numAttitude = 0;
	long numPressure = 0;
	long numVibration = 0;
	long numStatus = 0;
	
	boolean testStarted = false;
	
	UAVCurrentData dron = new UAVCurrentData();

	public Listener() {
		// Necesario indicar a RXTX que se usa un puerto de nombre raro
		Properties properties = System.getProperties();
		String currentPorts = properties.getProperty("gnu.io.rxtx.SerialPorts", Main.serialPort);
		if (currentPorts.equals(Main.serialPort)) {
			properties.setProperty("gnu.io.rxtx.SerialPorts", Main.serialPort);
		} else {
			properties.setProperty("gnu.io.rxtx.SerialPorts", currentPorts + File.pathSeparator + Main.serialPort);
		}

		CommPortIdentifier portIdentifier;
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(Main.serialPort);

			if (portIdentifier.isCurrentlyOwned()) {
				//System.out.println("Error: Port is currently in use");
			} else {
				int timeout = 2000;
				CommPort commPort;
				try {
					commPort = portIdentifier.open(this.getClass().getName(), timeout);

					if (commPort instanceof SerialPort) {
						serialPort = (SerialPort) commPort;
						try {
							serialPort.setSerialPortParams(Main.baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
									SerialPort.PARITY_NONE);

							in = serialPort.getInputStream();

							out = serialPort.getOutputStream();
							din = new DataInputStream(in);
							dout = new DataOutputStream(out);
							reader = new MAVLinkReader(din, IMAVLinkMessage.MAVPROT_PACKET_START_V10);

						} catch (UnsupportedCommOperationException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

					} else {
						//System.out.println("Error: It only works with serial ports");
					}
				} catch (PortInUseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		} catch (NoSuchPortException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	
	}
	
	@Override
	public void run() {
		
		
		
		Parametros.simStatus = Status.START;
		//System.out.println("Statuts: "+Parametros.simStatus);
		Calendar c = Calendar.getInstance();
		abrirEscritura(c.get(Calendar.YEAR) + "-"
				+ c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH)
				+ "-" + c.get(Calendar.HOUR_OF_DAY) + "-" + c.get(Calendar.MINUTE)
				+ "-" + c.get(Calendar.SECOND) + Text.FILE_LOGS_MSG);
		
		while (true) {
			if (testStarted && Parametros.simStatus == Status.END) {
				//System.out.println("Statuts: "+Parametros.simStatus);
				//System.out.println("Cerrando buffer");
				cerrarBuffer();
				return;
			}
			
			try {
				//System.out.println("Intenta conseguir un mensaje");
				
				msg = reader.getNextMessage();
				
				//System.out.println(msg.toString());
				
				if (msg != null) {
					switch (msg.messageType) {
	/*					case IMAVLinkMessageID.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:
							munServo++;
							registraServoOutPut((msg_servo_output_raw) msg);
							printServoOutPutRaw();
							break;
	*/					case IMAVLinkMessageID.MAVLINK_MSG_ID_RC_CHANNELS_RAW:
//							munRCChanels++;
//							registraRcChanelsRaw((msg_rc_channels_raw) msg);
							if (testStarted) {
								printRcChanelsRaw((msg_rc_channels_raw) msg);
							}
							break; 
						case IMAVLinkMessageID.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
//							numPosition++;
//							registraPosicionGlobal((msg_global_position_int) msg);
							if (testStarted) {
								printPosicionGlobal((msg_global_position_int) msg);
							}
							break;
						case IMAVLinkMessageID.MAVLINK_MSG_ID_HEARTBEAT:
							processMode((msg_heartbeat) msg);
							break;
					}
//					escribirBuffer();
					
				}
			} catch (IOException e) {
				//System.out.println("Fallo al leer el mensaje:\n" + e.getMessage());
			}
//			if(munRCChanels>200)continuar=false;
			
			
			
			
		}
//		dron.cerrarBuffer();
		// Cierre de inputstreams en puertos serie
		// No lo hago porque se captura indefinidamente hasta que se cierra el programa
		
		

	}

	private void printPosicionGlobal(msg_global_position_int mensaje) {
		UTMCoordinates xy = GUIHelper.geoToUTM(mensaje.lat * 0.0000001, mensaje.lon * 0.0000001);
		long time = System.nanoTime();
		double z = mensaje.alt * 0.001;
		double zRel = mensaje.relative_alt * 0.001;
		double heading = (mensaje.hdg * 0.01) * Math.PI / 180;
		double speed = Math.sqrt(Math.pow(mensaje.vx * 0.01, 2) + Math.pow(mensaje.vy * 0.01, 2));

		String res = "0," + time + "," + Listener.round(xy.Easting, 3) + "," + Listener.round(xy.Northing, 3)
			+  "," + Listener.round(z, 3) + "," + Listener.round(zRel, 3) + "," + Listener.round(speed, 3)
			+ "," + heading;
		
		//System.out.println(res);
		if (bfwriter != null) {
			try {
				bfwriter.write(res);
				bfwriter.newLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void printRcChanelsRaw(msg_rc_channels_raw mensaje) {
		long time = System.nanoTime();
		
		String res = "1," + time + "," + mensaje.chan1_raw + "," + mensaje.chan2_raw + "," + mensaje.chan3_raw + "," + mensaje.chan4_raw;
	
		//System.out.println(res);
		if (bfwriter != null) {
			try {
				bfwriter.write(res);
				bfwriter.newLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private void processMode(msg_heartbeat msg) {
		//System.out.println(Parametros.simStatus.name() + "-" + msg.base_mode);
		if (Parametros.simStatus == Status.START && msg.base_mode > Parametros.MIN_MODE_TO_BE_FLYING) {
			Parametros.simStatus = Status.ON;
			//System.out.println("Statuts: "+Parametros.simStatus);
			testStarted = true;
		}
		
		if (Parametros.simStatus == Status.ON && msg.base_mode < Parametros.MIN_MODE_TO_BE_FLYING) {
			Parametros.simStatus = Status.END;
			//System.out.println("Statuts: "+Parametros.simStatus);
		}
	}
	
	public void abrirEscritura(String fileLogsMsg) {
		bfwriter = null;
		try {
			// Borrar el fichero si ya existe
			if (Paths.get("./" + fileLogsMsg).toFile().exists()) {
				Paths.get("./" + fileLogsMsg).toFile().delete();
			}
			file = Files.createFile(Paths.get("./" + fileLogsMsg));
			Charset charset = Charset.forName("UTF-8");
			bfwriter = Files.newBufferedWriter(file, charset);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void cerrarBuffer() {
		try{
            bfwriter.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
	}
	
	/** Round a double number to "places" decimal digits. */
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = new BigDecimal(Double.toString(value));
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
}
