package de.beimax.simplespleef.listeners;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * Listener for block breaks
 * 
 * @author mkalus
 * 
 */
public class SimpleSpleefBlockListener extends BlockListener {
	/**
	 * reference to plugin
	 */
	private final SimpleSpleef plugin;

	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public SimpleSpleefBlockListener(SimpleSpleef plugin) {
		this.plugin = plugin;
	}
	
	/* (non-Javadoc)
	 * @see org.bukkit.event.block.BlockListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)
	 */
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		//TODO do something
	}
}
