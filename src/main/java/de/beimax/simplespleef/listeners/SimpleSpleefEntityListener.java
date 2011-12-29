/**
 * 
 */
package de.beimax.simplespleef.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;

/**
 * Handle events for all Entity related events
 * 
 * @author mkalus
 */
public class SimpleSpleefEntityListener extends EntityListener {
	/* (non-Javadoc)
	 * @see org.bukkit.event.entity.EntityListener#onEntityDeath(org.bukkit.event.entity.EntityDeathEvent)
	 */
	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		// check if games are running and the entity is indeed a player
		if (SimpleSpleef.getGameHandler().hasGames() && event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(player);
			if (game != null) game.onPlayerDeath(player);
		}
	}
}
