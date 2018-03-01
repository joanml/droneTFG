package logica;


public class Main {

	//public static UAVCurrentData dron = new UAVCurrentData();

	public static String serialPort = "/dev/ttyAMA0";
	public static int baudRate = 57600;


	public static void main(String[] args) {
		Listener t = new Listener();
		t.start();
	}
}
