/**
 * 
 */
package de.beimax.simplespleef;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

/**
 * Handle events for all Entity related events
 * 
 * @author mkalus
 */
public class SimpleSpleefEntityListener extends EntityListener {
	//private final SimpleSpleef plugin;
	private final SimpleSpleefGame game;

	/**
	 * Constructor
	 * @param instance
	 * @param game
	 */
	public SimpleSpleefEntityListener(SimpleSpleef instance,
			SimpleSpleefGame game) {
		//plugin = instance;
		this.game = game;
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.entity.EntityListener#onEntityDeath(org.bukkit.event.entity.EntityDeathEvent)
	 */
	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		// check if the entity is a player
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			// possibly remove player on death
			game.removePlayerOnQuitorDeath(player, true);
		}
	}
}
