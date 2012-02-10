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
package de.beimax.simplespleef.gamehelpers;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 * 
 */
public class OriginalPositionKeeper {
	/**
	 * Keeps data of players teleported
	 */
	private HashMap<String, PlayerOriginalLocation> playerOriginalLocations = new HashMap<String, OriginalPositionKeeper.PlayerOriginalLocation>();

	/**
	 * save original position of player - possibly, because it is possible that
	 * there is an original position saved already
	 * 
	 * @param player
	 */
	public void keepPosition(Player player) {
		long maxTime = SimpleSpleef.getPlugin().getConfig().getInt("keepOriginalLocationsSeconds", 3600);
		if (maxTime < 0) return; // should not happen...

		// prune first
		pruneOriginalLocations();
		String playerName = player.getName();
		// already in list? => delete old etry
		if (playerOriginalLocations.containsKey(playerName))
			updateOriginalLocationTimestamp(player);
		// add position
		else {
			PlayerOriginalLocation loc = new PlayerOriginalLocation();
			loc.timestamp = System.currentTimeMillis() / 1000;
			loc.location = player.getLocation().clone();

			playerOriginalLocations.put(player.getName(), loc);
		}
	}

	/**
	 * get original position of player, or null (and remove entry)
	 * 
	 * @param player
	 * @return
	 */
	public Location getOriginalPosition(Player player) {
		if (player == null) return null; // no NPEs!

		// prune first
		pruneOriginalLocations();

		PlayerOriginalLocation loc = playerOriginalLocations.get(player.getName());
		if (loc == null) return null;
		playerOriginalLocations.remove(player.getName());
		return loc.location;
	}
	
	/**
	* Update timestamp of of an original location
	* @param player
	*/
	public void updateOriginalLocationTimestamp(Player player) {
		PlayerOriginalLocation loc = playerOriginalLocations.get(player.getName());
		if (loc != null)
			loc.timestamp = System.currentTimeMillis() / 1000;
	}

	/**
	 * delete original position of player if it exists
	 * 
	 * @param player
	 */
	public void deleteOriginalPosition(Player player) {
		if (player != null)
			playerOriginalLocations.remove(player.getName());
	}

	/**
	 * called by above methods to clean out original locations list periodically
	 */
	protected void pruneOriginalLocations() {
		long maxTime = SimpleSpleef.getPlugin().getConfig().getInt("keepOriginalLocationsSeconds", 3600);
		if (maxTime < 0) return; // should not happen...
		long checkTime = (System.currentTimeMillis() / 1000) - maxTime;

		// delete entries that are too old
		for (Entry<String, PlayerOriginalLocation> entry : playerOriginalLocations
				.entrySet()) {
			if (entry.getValue().timestamp < checkTime) // remove entries that
														// are too old
				playerOriginalLocations.remove(entry.getKey());
		}
	}

	/**
	 * Simple keeper class for timestamps and locations of players
	 * 
	 * @author mkalus
	 * 
	 */
	private class PlayerOriginalLocation {
		long timestamp;

		Location location;
	}
}
