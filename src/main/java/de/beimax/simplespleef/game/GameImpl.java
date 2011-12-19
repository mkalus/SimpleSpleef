/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.ChatColor;
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
	protected SpleeferList spleefers;
	
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
			player.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.join", "[ARENA]", getName()));
			return false;
		}
		// max number of players?
		int maximumPlayers = configuration.getInt("maximumPlayers", 0);
		if (maximumPlayers > 0 && spleefers.size() >= maximumPlayers) {
			player.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.joinMax", "[ARENA]", getName(), "[NUMBER]", String.valueOf(maximumPlayers)));
			return false;
		}
		// already joined this game? => is caught by GameHandler, so we do not check this here...
		// TODO: check funds of player...
		if (!spleefers.addSpleefer(player)) { // some weird error
			player.sendMessage(ChatColor.DARK_RED + "Internal error while joining occured! Please tell the SimpleSpleef creator!");
		}
		// TODO clear inventory
		// TODO: add to inventory
		// TODO: alternatively give shovels
		// TODO: remember player's last position
		// TODO: teleport player to lobby
		return true;
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

	@Override
	public boolean hasPlayer(Player player) {
		return spleefers.hasSpleefer(player);
	}
}
