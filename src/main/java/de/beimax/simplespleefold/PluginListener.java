package de.beimax.simplespleefold;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

/**
 * Listener for server plugin enable events
 * 
 * @author mkalus
 * 
 */
public class PluginListener extends ServerListener {
	/**
	 * Constructor
	 */
	public PluginListener() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.bukkit.event.server.ServerListener#onPluginEnable(org.bukkit.event
	 * .server.PluginEnableEvent)
	 */
	@Override
	public void onPluginEnable(PluginEnableEvent event) {
		// load allpay economy
		if (event.getPlugin() instanceof SimpleSpleef) {
			((SimpleSpleef) event.getPlugin()).getAllPay().loadEconPlugin();
		}
	}
}
