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

package de.beimax.simplespleef.statistics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;

/**
 * Saves statistics to a flat file in the plugin directory
 * @author mkalus
 */
public class FileStatisticsModule implements StatisticsModule {
	/**
	 * statistics reference (YAML)
	 */
	YamlConfiguration statistics;
	
	/**
	 * file reference
	 */
	File statisticsFile;

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.statistics.StatisticsModule#initialize()
	 */
	@Override
	public void initialize() throws Exception {
		statisticsFile = new File(SimpleSpleef.getPlugin().getDataFolder(), "statistics.yml");
		
		// does the file exist - no
		if (!statisticsFile.exists()) {
			SimpleSpleef.log.info("[SimpleSpleef] Using flat file statistics - creating new file.");
			statistics = new YamlConfiguration();
			try {
				statistics.save(statisticsFile);
			} catch (IOException e) {
				SimpleSpleef.log.severe("[SimpleSpleef] Could not create statistics file. Error: " + e.getMessage());
			}
		} else { // load file
			statistics = YamlConfiguration.loadConfiguration(statisticsFile);
			if (statistics == null)
				throw new Exception("Could not load YAML file " + statisticsFile + ".");
		}
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.statistics.StatisticsModule#playerWonGame(org.bukkit.entity.Player, de.beimax.simplespleef.game.Game)
	 */
	@Override
	public void playerWonGame(Player player, Game game) {
		// TODO Auto-generated method stub
		
		// save statistics
		saveStatistics();
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.statistics.StatisticsModule#playerLostGame(org.bukkit.entity.Player, de.beimax.simplespleef.game.Game)
	 */
	@Override
	public void playerLostGame(Player player, Game game) {
		// TODO Auto-generated method stub
		
		// save statistics
		saveStatistics();
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.statistics.StatisticsModule#gameStarted(de.beimax.simplespleef.game.Game)
	 */
	@Override
	public void gameStarted(Game game) {
		// increase game counter
		int gamesCount = statistics.getInt(game.getId() + ".gamesCount", 0) + 1;
		statistics.set(game.getId() + ".gamesCount", gamesCount);
		
		// number of players in last game
		int playersCount = game.getSpleefers().get().size();
		statistics.set(game.getId() + ".lastGamePlayers", playersCount);
		
		// get total players
		int totalPlayers = statistics.getInt(game.getId() + ".gamesTotalPlayers");
		
		// add to total players and set average number of players
		statistics.set(game.getId() + ".gamesTotalPlayers", totalPlayers + playersCount);
		statistics.set(game.getId() + ".gamesAveragePlayers", ((double) (totalPlayers + playersCount)) / (double) gamesCount);

		// add last game started entry
		statistics.set(game.getId() + ".lastGameStartedAt", game.getStartTime());
		
		// save statistics
		saveStatistics();
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.statistics.StatisticsModule#gameFinished(de.beimax.simplespleef.game.Game)
	 */
	@Override
	public void gameFinished(Game game) {
		// add last game started entry
		statistics.set(game.getId() + ".lastGameFinishedAt", game.getFinishTime());
		
		// length of last game
		long length = game.getFinishTime() - game.getStartTime();
		statistics.set(game.getId() + ".lastGameLength", game.getFinishTime());
		
		// get total length
		long totalLength = statistics.getLong(game.getId() + ".gamesTotalLength");
		
		// add to total length and set average length
		statistics.set(game.getId() + ".gamesTotalLength", totalLength + length);
		int gamesCount = statistics.getInt(game.getId() + ".gamesCount");
		statistics.set(game.getId() + ".gamesAverageLength", (totalLength + length) / gamesCount);
		
		// save statistics
		saveStatistics();
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.statistics.StatisticsModule#getStatistics(org.bukkit.entity.Player)
	 */
	@Override
	public HashMap<String, Object> getStatistics(Player player) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.statistics.StatisticsModule#getStatistics(de.beimax.simplespleef.game.Game)
	 */
	@Override
	public HashMap<String, Object> getStatistics(Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * save statistics to disk
	 */
	private void saveStatistics() {
		try {
			statistics.save(statisticsFile);
		} catch (Exception e) {
			SimpleSpleef.log.severe("[SimpleSpleef] Could not save statistics file " +  statisticsFile + ". Reason: " + e.getMessage());
		}
	}
}
