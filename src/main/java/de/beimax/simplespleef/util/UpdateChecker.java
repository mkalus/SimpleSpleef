/**
 * 
 */
package de.beimax.simplespleef.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author mkalus Helper class that checks for updates.
 */
public class UpdateChecker {
	/**
	 * actually check for an update
	 * @param String version version of current system to check
	 * 
	 * @return true if there is an update out there...
	 */
	public boolean checkForUpdate(String version) throws Exception {
		// open HTTP connection
		URL url = new URL("http://mc.auxc.de/SimpleSpleef.version");
		URLConnection connection = url.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		// just read first line
		String inputLine = in.readLine();
		in.close();
		
		if (inputLine != null && inputLine.equals(version)) return false;

		return true;
	}
}
