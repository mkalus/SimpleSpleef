/**
 * 
 */
package de.beimax.simplespleef.util;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 * Class helping with locations
 */
public abstract class LocationHelper {
	/**
	 * get location including yaw and pitch from location
	 * @param location
	 * @param enabled
	 * @return
	 */
	public static Map<String, Object> getExactLocation(Location location, boolean enabled) {
		// create configuration
		ConfigurationSection conf = new MemoryConfiguration();
		// define stuff
		conf.set("enabled", enabled);
		conf.set("world", location.getWorld().getName());
		conf.set("x", location.getX());
		conf.set("y", location.getY());
		conf.set("z", location.getZ());
		conf.set("yaw", location.getYaw());
		conf.set("pitch", location.getPitch());
		
		return conf.getValues(true);
	}

	/**
	 * get location of a block from location
	 * @param location
	 * @return
	 */
	public static Map<String, Object> getXYZLocation(Location location) {
		// create configuration
		ConfigurationSection conf = new MemoryConfiguration();
		// define stuff
		conf.set("world", location.getWorld().getName());
		conf.set("x", location.getBlockX());
		conf.set("y", location.getBlockY());
		conf.set("z", location.getBlockZ());
		
		return conf.getValues(true);
	}

	/**
	 * get exact location from config section
	 * @param config
	 * @return
	 */
	public static Location configToExactLocation(ConfigurationSection config) {
		try {
			World world = SimpleSpleef.getPlugin().getServer().getWorld(config.getString("world"));
			return new Location(world, config.getDouble("x"), config.getDouble("y"), config.getDouble("z"), (float) config.getDouble("yaw"), (float) config.getDouble("pitch"));
		} catch (Exception e) {
			return null;
		}
	}
}
