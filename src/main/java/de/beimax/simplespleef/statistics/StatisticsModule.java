/**
 * This file is part of the SimpleSpleef bukkit plugin.
 * Copyright (C) 2012 Maximilian Kalus
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

package de.beimax.simplespleef.statistics;

import java.util.HashMap;

import org.bukkit.entity.Player;

import de.beimax.simplespleef.game.Game;

/**
 * Interface for implementations of statistics modules
 * @author mkalus
 */
public interface StatisticsModule {
	/**
	 * Notify statistics that player won a game
	 * @param player
	 * @param game
	 */
	public void playerWonGame(Player player, Game game);

	/**
	 * Notify statistics that player lost a game
	 * @param player
	 * @param game
	 */
	public void playerLostGame(Player player, Game game);
	
	/**
	 * Notify statistics that a game has started
	 * @param game
	 */
	public void gameStarted(Game game);
	
	/**
	 * Notify statistics that a game has started
	 * @param game
	 */
	public void gameFinished(Game game);
	
	/**
	 * Get statistics for a certain player
	 * @param player
	 * @return
	 */
	public HashMap<String, Object> getStatistics(Player player);
	
	/**
	 * Get statistics for a certain game
	 * @param player
	 * @return
	 */
	public HashMap<String, Object> getStatistics(Game game);
}
