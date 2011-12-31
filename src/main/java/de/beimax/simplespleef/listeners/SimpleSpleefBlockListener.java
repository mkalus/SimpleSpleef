package de.beimax.simplespleef.listeners;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * Listener for block breaks
 * 
 * @author mkalus
 * 
 */
public class SimpleSpleefBlockListener extends BlockListener {
	/* (non-Javadoc)
	 * @see org.bukkit.event.block.BlockListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)
	 */
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		// check block breaks globally, since blocks could be broken from a protected arena
		SimpleSpleef.getGameHandler().checkBlockBreak(event);
	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.block.BlockListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)
	 */
	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		// check block breaks globally, since blocks could be places in a protected arena
		SimpleSpleef.getGameHandler().checkBlockPlace(event);
	}
}
