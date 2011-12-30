/**
 * 
 */
package de.beimax.simplespleef.listeners;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

import com.sk89q.worldedit.bukkit.WorldEditAPI;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus Listens to plugin events
 */
public class PluginListener extends ServerListener {
	@Override
	public void onPluginEnable(PluginEnableEvent event) {
		// worldEdit - inspired by MultiversePortals
		if (event.getPlugin().getDescription().getName().equals("WorldEdit")) {
			SimpleSpleef.setWorldEditAPI(new WorldEditAPI((WorldEditPlugin) SimpleSpleef.getPlugin().getServer().getPluginManager().getPlugin("WorldEdit")));
			SimpleSpleef.log.info("[SimpleSpleef] Found WorldEdit. Using it for selections.");
		}
	}
}
