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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * Listener for block breaks
 * 
 * @author mkalus
 * 
 */
public class SimpleSpleefBlockListener implements Listener {
	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;

		// check block breaks globally, since blocks could be broken from a protected arena
		SimpleSpleef.getGameHandler().checkBlockBreak(event);
	}

	/**
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) return;

		// check block breaks globally, since blocks could be places in a protected arena
		SimpleSpleef.getGameHandler().checkBlockPlace(event);
	}
}
