/**
 * 
 */
package de.beimax.simplespleef.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.bukkit.configuration.Configuration;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus Helper class that checks for updates.
 */
public class UpdateChecker {
	/**
	 * actually check for an update
	 * @param String version version of current system to check
	 * 
	 * @return new version number, if there is an update or null, if there is no new version available
	 */
	public String checkForUpdate(String version) throws Exception {
		// open HTTP connection
		URL url = new URL("http://mc.auxc.de/SimpleSpleef.version");
		URLConnection connection = url.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		// just read first line
		String inputLine = in.readLine();
		in.close();
		
		if (inputLine != null && inputLine.equals(version)) return null; // no new update

		return inputLine; // new version
	}

	/**
	 * Update configuration files, if needed.
	 * @param simpleSpleef
	 */
	public void updateConfigurationVersion(SimpleSpleef simpleSpleef) {
		Configuration config = SimpleSpleef.getPlugin().getConfig();
		// check version number in config file
		int version = config.getInt("version", 1);
		// changed?
		boolean changed = false;
		
		// update stuff
		if (version <= 1) {
			config.set("allowDiggingOutsideArena", null); // delete obsolete setting allowDiggingOutsideArena
			changed = true;
		}
		// add new update versions here
		
		// increase version number
		if (changed) {
			SimpleSpleef.log.info("[SimpleSpleef] Updating configuration from version " + version + " to version 2.");
			config.set("version", 2);
			SimpleSpleef.getPlugin().saveConfig();
		}
	}
}
