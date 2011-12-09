/**
 * 
 */
package de.beimax.simplespleef.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.GameHandler;

/**
 * Handle events for all Entity related events
 * 
 * @author mkalus
 */
public class SimpleSpleefEntityListener extends EntityListener {
	/**
	 * reference to game handler
	 */
	private final GameHandler gameHandler;
	
	/**
	 * Constructor
	 * @param gameHandler reference to game handler
	 */
	public SimpleSpleefEntityListener(GameHandler gameHandler) {
		this.gameHandler = gameHandler;
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.entity.EntityListener#onEntityDeath(org.bukkit.event.entity.EntityDeathEvent)
	 */
	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		//TODO implement
	}
}
