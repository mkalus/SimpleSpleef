/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author mkalus
 * Simple Game implementation
 */
public class GameImpl extends Game {
	/**
	 * Reference to spleefer list
	 */
	protected  SpleeferList spleefers;
	
	/**
	 * Reference to configuration
	 */
	protected ConfigurationSection configuration;

	/**
	 * Constructor
	 * @param gameHandler
	 * @param name
	 */
	public GameImpl(GameHandler gameHandler, String name) {
		super(gameHandler, name);
		this.spleefers = new SpleeferList();
	}

	@Override
	public String getType() {
		return "standard";
	}

	@Override
	public void defineSettings(ConfigurationSection conf) {
		this.configuration = conf;
	}

	@Override
	public boolean join(Player player) {
		//check joinable status
		if (!isJoinable()) {
			return false;
		}
		// inject joining preconditions (like checking funds)
		//TODO: player feedback
		return spleefers.addSpleefer(player);
	}

	@Override
	public boolean leave(Player player) {
		//TODO: check status
		//TODO: player feedback
		return spleefers.removeSpleefer(player);
	}

	@Override
	public boolean countdown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean spectate() {
		// TODO Auto-generated method stub
		return false;
	}
}
