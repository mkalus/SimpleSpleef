/**
 * 
 */
package de.beimax.simplespleef.game;

import org.bukkit.entity.Player;

/**
 * @author mkalus
 *
 */
public abstract class OneTeamGame extends Game {
	/**
	 * Reference to spleefer list
	 */
	SpleeferList spleefers;

	/**
	 * Constructor
	 * @param name
	 */
	public OneTeamGame(String name) {
		super(name);
		this.spleefers = new SpleeferList();
	}

	@Override
	public boolean join(Player player) {
		//TODO: check status
		// inject joining preconditions (like checking funds)
		//TODO: player feedback
		return spleefers.addSpleefer(player);
	}
	
	/**
	 * check preconditions for joining - like checking funds...
	 * @param player
	 * @return
	 */
	public abstract boolean preconditionsJoin(Player player);
	
	/**
	 * called after joining a game - do stuff
	 * @return
	 */
	public abstract void postJoin(Player player);

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
	public boolean isJoinable() {
		// TODO Auto-generated method stub
		return false;
	}

}
