/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author mkalus
 *
 */
public class FreestyleGame extends OneTeamGame {
	/**
	 * Constructor
	 * @param name
	 */
	public FreestyleGame(String name) {
		super(name);
	}

	@Override
	public void defineSettings(ConfigurationSection conf) {} // no nothing - it's freestyle time!

	@Override
	public boolean spectate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean preconditionsJoin(Player player) {
		return true; // always true in simple games
	}

	@Override
	public void postJoin(Player player) {} //noting to do in simple games
}
