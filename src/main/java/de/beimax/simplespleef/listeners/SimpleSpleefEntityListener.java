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
package de.beimax.simplespleef.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

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

	/* (non-Javadoc)
	 * @see org.bukkit.event.entity.EntityListener#onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent)
	 */
	@Override
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		// check if games are running and the entity is indeed a player
		if (SimpleSpleef.getGameHandler().hasGames() && event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(player);
			// if setting noHunger has been set for this arena, do not feel any hunger
			if (game != null && SimpleSpleef.getPlugin().getConfig().getBoolean("arenas." + game.getId() + ".noHunger", true))
				event.setCancelled(true);
		}
	}
}
