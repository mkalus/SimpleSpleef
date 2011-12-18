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
public class StandardGame extends OneTeamGame {
	/**
	 * Constructor
	 * @param name
	 */
	public StandardGame(String name) {
		super(name);
	}

	@Override
	public void defineSettings(ConfigurationSection conf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean preconditionsJoin(Player player) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void postJoin(Player player) {
		// TODO Auto-generated method stub
		// remember position, etc.
	}

	@Override
	public boolean spectate() {
		// TODO Auto-generated method stub
		return false;
	}
}
