package de.beimax.simplespleefold;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

/**
 * Listener for block breaks
 * 
 * @author mkalus
 * 
 */
public class SimpleSpleefBlockListener extends BlockListener {
	//private final SimpleSpleef plugin;
	private final SimpleSpleefGame game;

	/**
	 * Constructor
	 * @param instance
	 * @param game
	 */
	public SimpleSpleefBlockListener(SimpleSpleef instance,
			SimpleSpleefGame game) {
		//plugin = instance;
		this.game = game;
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		game.checkBlockBreak(event);
	}
}
