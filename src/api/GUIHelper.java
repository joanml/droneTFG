package api;

import api.pojo.UTMCoordinates;
import sim.logic.SimParam;


/** This class consists exclusively of static methods that help the developer to validate and show information on screen. */

public class GUIHelper {

	/** Transforms Geographic coordinates to UTM coordinates. */
	public static UTMCoordinates geoToUTM(double lat, double lon) {
		double Easting;
		double Northing;
		int Zone = (int) Math.floor(lon / 6 + 31);
		char Letter;
		if (lat < -72)		Letter = 'C';
		else if (lat < -64)	Letter = 'D';
		else if (lat < -56)	Letter = 'E';
		else if (lat < -48)	Letter = 'F';
		else if (lat < -40)	Letter = 'G';
		else if (lat < -32)	Letter = 'H';
		else if (lat < -24)	Letter = 'J';
		else if (lat < -16)	Letter = 'K';
		else if (lat < -8)	Letter = 'L';
		else if (lat < 0)	Letter = 'M';
		else if (lat < 8)	Letter = 'N';
		else if (lat < 16)	Letter = 'P';
		else if (lat < 24)	Letter = 'Q';
		else if (lat < 32)	Letter = 'R';
		else if (lat < 40)	Letter = 'S';
		else if (lat < 48)	Letter = 'T';
		else if (lat < 56)	Letter = 'U';
		else if (lat < 64)	Letter = 'V';
		else if (lat < 72)	Letter = 'W';
		else				Letter = 'X';
		Easting = 0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) * 0.9996 * 6399593.62 / Math.pow((1 + Math.pow(0.0820944379, 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)), 0.5) * (1 + Math.pow(0.0820944379, 2) / 2 * Math.pow((0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin(lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2) / 3) + 500000;
		Easting = Math.round(Easting * 100) * 0.01;
		Northing = (Math.atan(Math.tan(lat * Math.PI / 180) / Math.cos((lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) - lat * Math.PI / 180) * 0.9996 * 6399593.625 / Math.sqrt(1 + 0.006739496742 * Math.pow(Math.cos(lat * Math.PI / 180), 2)) * (1 + 0.006739496742 / 2 * Math.pow(0.5 * Math.log((1 + Math.cos(lat * Math.PI / 180) * Math.sin((lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180))) / (1 - Math.cos(lat * Math.PI / 180) * Math.sin((lon * Math.PI / 180 - (6 * Zone - 183) * Math.PI / 180)))), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) + 0.9996 * 6399593.625 * (lat * Math.PI / 180 - 0.005054622556 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + 4.258201531e-05 * (3 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 4 - 1.674057895e-07 * (5 * (3 * (lat * Math.PI / 180 + Math.sin(2 * lat * Math.PI / 180) / 2) + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 4 + Math.sin(2 * lat * Math.PI / 180) * Math.pow(Math.cos(lat * Math.PI / 180), 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2)) / 3);
		if (Letter < 'M')
			Northing = Northing + 10000000;
		Northing = Math.round(Northing * 100) * 0.01;
	
		if (SimParam.zone < 0) {
			SimParam.zone = Zone;
			SimParam.letter = Letter;
		}
		
		return new UTMCoordinates(Easting, Northing, Zone, Letter);
	}
	
	

}
