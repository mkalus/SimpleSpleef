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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;

/**
 * Handle events for all Entity related events
 * 
 * @author mkalus
 */
public class SimpleSpleefEntityListener implements Listener {
	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		// check if games are running and the entity is indeed a player
		if (SimpleSpleef.getGameHandler().hasGames() && event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(player);
			if (game != null) game.onPlayerDeath(player);
		}
	}

	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.isCancelled()) return;

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

	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;

		// check if games are running and the entity is indeed a player
		if (SimpleSpleef.getGameHandler().hasGames() && event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			// no PVP is allowed anyway
			if (!player.getWorld().getPVP()) return;
			
			// player part of a game?
			Game game = SimpleSpleef.getGameHandler().checkPlayerInGame(player);
			// if setting noPvP has been set for this arena, check further
			if (game != null && SimpleSpleef.getPlugin().getConfig().getBoolean("arenas." + game.getId() + ".noPvP", true)) {
				// get cause of damage - only consider damage by other entities
				if (event instanceof EntityDamageByEntityEvent) {
					EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
					// only consider player damage
					if (damageEvent.getDamager() instanceof Player) {
						event.setCancelled(true); // cancel damage event by caused by other players
					}
				}
			}
		}
	}
}
