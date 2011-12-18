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
public class FreestyleGame extends Game {
	/**
	 * Constructor
	 * @param name
	 */
	public FreestyleGame(String name) {
		super(name);
	}

	@Override
	public void defineSettings(ConfigurationSection conf) {} // no nothing - it's freestyle time!

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.Game#join(org.bukkit.entity.Player)
	 */
	@Override
	public boolean join(Player player) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.Game#leave(org.bukkit.entity.Player)
	 */
	@Override
	public boolean leave(Player player) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.Game#countdown()
	 */
	@Override
	public boolean countdown() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.Game#start()
	 */
	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.Game#stop()
	 */
	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.Game#delete()
	 */
	@Override
	public boolean delete() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see de.beimax.simplespleef.game.Game#spectate()
	 */
	@Override
	public boolean spectate() {
		// TODO Auto-generated method stub
		return false;
	}
}
