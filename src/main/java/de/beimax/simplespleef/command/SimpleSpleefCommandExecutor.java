/**
 * 
 */
package de.beimax.simplespleef.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 * Simple Spleef command handler
 */
public class SimpleSpleefCommandExecutor implements CommandExecutor {
	/**
	 * reference to plugin
	 */
	private final SimpleSpleef plugin;
	
	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public SimpleSpleefCommandExecutor(SimpleSpleef plugin) {
		this.plugin = plugin;
	}

	/**
	 * Executed whenever simplespeef is hit by a command
	 */
	public boolean onCommand(CommandSender sender, Command command, String label,
			String[] args) {
		//sender.sendMessage(command.toString());
		
		//TODO handle commands here...
		// do not forget, that commands can be sent from the console, too!
		return true;
	}
}
