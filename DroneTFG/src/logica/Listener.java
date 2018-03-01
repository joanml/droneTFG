package logica;

import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Properties;

import org.mavlink.IMAVLinkMessage;
import org.mavlink.MAVLinkReader;
import org.mavlink.messages.IMAVLinkMessageID;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.ardupilotmega.msg_attitude;
import org.mavlink.messages.ardupilotmega.msg_global_position_int;
import org.mavlink.messages.ardupilotmega.msg_scaled_pressure;

import api.GUIHelper;
import api.pojo.UTMCoordinates;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import sim.logic.SimParam;

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

	// Sigue el número de paquetes que llegan de cada tipo
	long numPosition = 0;
	long numAttitude = 0;
	long numPressure = 0;
	long numVibration = 0;
	long numStatus = 0;
	
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
						System.out.println("Error: Port is currently in use");
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
								System.out.println("Error: It only works with serial ports");
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
		continuar = true;

		while (true) {
/*			try {
				// Versión por puerto serie
				//   No requiere convertir el inputStream en DataInputStream
				//   Al no estar orientado a paquetes no hay que crear un reader cada vez
				
				// Versión por UDP
				if (!Main.interfaceIsSerial) {
					// Lectura de información por UDP
					buffer = new byte[MainWindow.DGRAM_MAX_LENGTH];
					paquete = new DatagramPacket(buffer, buffer.length);
					socketListener.receive(paquete);
					if (din != null)
						din.close(); // Cierre en cada ciclo
					if (bin != null)
						bin.close();
					bin = new ByteArrayInputStream(paquete.getData());
					din = new DataInputStream(bin);
					reader = new MAVLinkReader(din, IMAVLinkMessage.MAVPROT_PACKET_START_V10);
				}
				
				// Si interesa el tipo de mensaje se guarda la información que interesa
				selectMessage();
				
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			selectMessage();
		}

		// Cierre de inputstreams en puertos serie
		// No lo hago porque se captura indefinidamente hasta que se cierra el programa

	}

	
	private void selectMessage() {
		try {
			//System.out.println("Intenta conseguir un mensaje");
			
			msg = reader.getNextMessage();
			
			//System.out.println(msg.toString());
			
			if (msg != null) {
				switch (msg.messageType) {
					case IMAVLinkMessageID.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
						numPosition++;
						registraPosicionGlobal((msg_global_position_int) msg);
						printPosicionGlobal();
						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_ATTITUDE:
//						numAttitude++;
//						registraPostura((msg_attitude) msg);
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_SCALED_PRESSURE:
//						numPressure++;
//						registraTemperatura((msg_scaled_pressure) msg);
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_VIBRATION:
//						numVibration++;
//						registraVibracion((msg_vibration) msg);
//						break;
//					case IMAVLinkMessageID.MAVLINK_MSG_ID_SYS_STATUS:
//						numStatus++;
//						registraBateria((msg_sys_status) msg);
//						break;
				}
			}
		} catch (IOException e) {
			System.out.println("Fallo al leer el mensaje:\n" + e.getMessage());
		}
	}

	private void printPosicionGlobal() {
		System.out.println(dron.getGeoLocation());
	}

	private void registraPosicionGlobal(msg_global_position_int mensaje) {
		Point2D.Double locationUTM = new Point2D.Double();
		Point2D.Double locationGeo = new Point2D.Double(mensaje.lon * 0.0000001, mensaje.lat * 0.0000001);
		UTMCoordinates locationUTMauxiliary = GUIHelper.geoToUTM(locationGeo.y, locationGeo.x);
		if (SimParam.zone < 0) {
			SimParam.zone = locationUTMauxiliary.Zone;
			SimParam.letter = locationUTMauxiliary.Letter;
		}
		locationUTM.setLocation(locationUTMauxiliary.Easting, locationUTMauxiliary.Northing);
		long time = System.nanoTime();
		double z = mensaje.alt * 0.001;
		double heading = (mensaje.hdg * 0.01) * Math.PI / 180;
		double speed = Math.sqrt(Math.pow(mensaje.vx * 0.01, 2) + Math.pow(mensaje.vy * 0.01, 2));

		
		dron.update(time, locationGeo, locationUTM, z, mensaje.relative_alt * 0.001, speed, heading);
		System.out.println("Actualizado: "+time);

	}
}
