package de.beimax.simplespleef;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

import com.iConomy.iConomy;
import com.nijikokun.bukkit.Permissions.Permissions;

import org.bukkit.plugin.Plugin;

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
		// load Permissions
		if (SimpleSpleef.getPermissions() == null) {
			Plugin permissions = SimpleSpleef.getBukkitServer()
					.getPluginManager().getPlugin("Permissions");
			if (permissions != null) {
				if (permissions.isEnabled()) {
					SimpleSpleef.setPermissions(((Permissions) permissions)
							.getHandler());

					SimpleSpleef.log.info("[SimpleSpleef] Using Permissions.");
				}
			} else {
				try {
					Class.forName("org.bukkit.permissions.Permission");
					SimpleSpleef.log
						.info("[SimpleSpleef] Permission system not detected, defaulting to Bukkit builtin.");
				} catch (ClassNotFoundException e) {
					SimpleSpleef.log
						.info("[SimpleSpleef] Permission system not detected, defaulting to all.");
				}
			}
		}//

		// load iConomy
		if (SimpleSpleef.getiConomy() == null) {
			Plugin iConomy = SimpleSpleef.getBukkitServer().getPluginManager()
					.getPlugin("iConomy");

			if (iConomy != null) {
				if (iConomy.isEnabled() && iConomy.getClass().getName().equals("com.iConomy.iConomy")) {
					SimpleSpleef.setiConomy((iConomy) iConomy);

					SimpleSpleef.log
							.info("[SimpleSpleef] Successfully linked with iConomy.");
				}
			}
		}
	}

}
