/**
 * This file is part of the SimpleSpleef bukkit plugin.
 * Copyright (C) 2011 Maximilian Kalus
 * See http://dev.bukkit.org/server-mods/simple-spleef/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package de.beimax.simplespleef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import org.bukkit.plugin.java.JavaPlugin;

import de.beimax.simplespleef.util.ConfigHelper;

/**
 * SimpleSpleef for Bukkit
 * 
 * @author mkalus
 */
public class SimpleSpleef extends JavaPlugin {
	public static Logger log = Logger.getLogger("Minecraft");

	/**
	 * Called when enabling plugin
	 */
	public void onEnable() {
		// initialize plugin
		log.info(this.toString() + " is loading.");

		// configure plugin
		configurePlugin();
	}

	/**
	 * Called when disabling plugin
	 */
	public void onDisable() {
		log.info(this.toString() + " is shutting down.");
		//TODO: clean memory

		//save config to disk
		this.saveConfig();
	}
	
	/**
	 * Run some configuration stuff at the initialization of the plugin
	 */
	protected void configurePlugin() {
		// define that default config should be copied
		this.getConfig().options().copyDefaults(true);
		
		// create config helper
		ConfigHelper configHelper = new ConfigHelper(this);
		// update sample config, if needed
		configHelper.updateSampleConfig();
	}
}
