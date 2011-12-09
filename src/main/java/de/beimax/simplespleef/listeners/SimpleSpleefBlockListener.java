package de.beimax.simplespleef.listeners;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.GameHandler;

/**
 * Listener for block breaks
 * 
 * @author mkalus
 * 
 */
public class SimpleSpleefBlockListener extends BlockListener {
	/**
	 * reference to game handler
	 */
	private final GameHandler gameHandler;
	
	/**
	 * Constructor
	 * @param gameHandler reference to game handler
	 */
	public SimpleSpleefBlockListener(GameHandler gameHandler) {
		this.gameHandler = gameHandler;
	}
	
	/* (non-Javadoc)
	 * @see org.bukkit.event.block.BlockListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)
	 */
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		//TODO do something
	}
}
