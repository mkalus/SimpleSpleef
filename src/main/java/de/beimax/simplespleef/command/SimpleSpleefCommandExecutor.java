/**
 * 
 */
package de.beimax.simplespleef.command;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import de.beimax.simplespleef.game.GameHandler;

/**
 * @author mkalus
 * Simple Spleef command handler
 */
public class SimpleSpleefCommandExecutor implements CommandExecutor {
	private final static String[] consoleCommands = {"help", "announce", "arenas", "info", "list", "countdown"};
	
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
		// how many arguments?
		if (args.length == 0) return helpCommand(sender);
		
		//TODO handle commands here...
		// do not forget, that commands can be sent from the console, too!
		
		// any other stuff -> show help
		return helpCommand(sender);
	}

	/**
	 * handle help commands
	 * @param sender
	 * @return
	 */
	protected boolean helpCommand(CommandSender sender) {
		// get all commands in language file
		Map<String, String> commands = this.gameHandler.getPlugin().lls("command");
		// control if there is any output:
		boolean output = false;
		// cycle through commands
		for (String command : commands.keySet()) {
			// check permission on command
			if (!sender.hasPermission("simplespleef." + command)) continue;
			// check if we are on the console and check command in that case
			if (isConsole(sender) && !isConsoleCommand(command)) continue;
			// we are cleared and may output the command nicely
			output = true;
			String value = commands.get(command);
			int pos = value.indexOf('|');
			if (pos == -1) sender.sendMessage(ChatColor.GOLD + value);
			else
				sender.sendMessage(ChatColor.GOLD + value.substring(0, pos)
						+ ": " + ChatColor.WHITE + value.substring(pos + 1));
		}
		// was there output? if not show response
		if (!output)
			sender.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("permissions.noPermissionAtAll"));
		return true;
	}
	
	/**
	 * Checks if sender is console
	 * @param sender
	 * @return
	 */
	protected boolean isConsole(CommandSender sender) {
		if (sender instanceof ConsoleCommandSender) return true;
		return false;
	}
	
	/**
	 * checks whether given command is allowed on the console
	 * @param command
	 * @return
	 */
	protected boolean isConsoleCommand(String command) {
		for (String c : SimpleSpleefCommandExecutor.consoleCommands) {
			if (c.equals(command)) return true;
		}
		return false;
	}
}
