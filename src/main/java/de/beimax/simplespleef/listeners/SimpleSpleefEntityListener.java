/**
 * 
 */
package de.beimax.simplespleef.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * Handle events for all Entity related events
 * 
 * @author mkalus
 */
public class SimpleSpleefEntityListener extends EntityListener {
	/**
	 * reference to plugin
	 */
	private final SimpleSpleef plugin;

	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public SimpleSpleefEntityListener(SimpleSpleef plugin) {
		this.plugin = plugin;
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.entity.EntityListener#onEntityDeath(org.bukkit.event.entity.EntityDeathEvent)
	 */
	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		//TODO implement
	}
}
