/**
 * 
 */
package de.beimax.simplespleef.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import de.beimax.simplespleef.game.GameHandler;

/**
 * @author mkalus
 * Simple Spleef command handler
 */
public class SimpleSpleefCommandExecutor implements CommandExecutor {
	/**
	 * reference to game handler
	 */
	private final GameHandler gameHandler;
	
	/**
	 * Constructor
	 * @param gameHandler reference to game handler
	 */
	public SimpleSpleefCommandExecutor(GameHandler gameHandler) {
		this.gameHandler = gameHandler;
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
