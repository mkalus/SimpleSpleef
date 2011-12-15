/**
 * 
 */
package de.beimax.simplespleef.command;

import java.lang.reflect.Method;
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
	/**
	 * list of all commands
	 */
	private final static String[] commands = {"help", "announce", "join", "arenas", "info", "list", "countdown", "leave", "stop", "delete", "reset", "watch"};
	/**
	 * commands possible from the console
	 */
	private final static String[] consoleCommands = {"help", "announce", "arenas", "info", "list", "countdown", "reset"};

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
		if (args.length == 0 || args[0].equalsIgnoreCase("help")) return helpCommand(sender);
		
		// first argument is subcommand
		String subcommand = args[0];

		// parse commands - does it exist?
		boolean found = false;
		for (String checkCommand : commands) {
			if (checkCommand.equalsIgnoreCase(subcommand)) {
				found = true;
				break;
			}
		}
		// command not found?
		if (!found) return unknownCommand(sender, subcommand);
		
		// is it a console command and it cannot be executed from the console?
		if (isConsole(sender) && !isConsoleCommand(subcommand)) {
			sender.sendMessage("This command cannot be executed from the consolse");
			return true;
		}

		// Ok, we are clear...
		try {
			// inflect method
			Method myCommand = this.getClass().getDeclaredMethod(subcommand + "Command", CommandSender.class, String[].class);
			Object[] xargs = { sender, args };
			// invoke it
			myCommand.invoke(this, xargs);
			return true;
		} catch (Exception e) {
			//e.printStackTrace();
			sender.sendMessage("Error in command execution - command was not found in executor. Contact SimpleSpleef programmer!");
		}
		// any other stuff -> show help
		return helpCommand(sender);		
	}

	/**
	 * handle help commands
	 * @param sender
	 * @return
	 */
	protected boolean helpCommand(CommandSender sender) {
		// last option...
		if (!sender.hasPermission("simplespleef.help")) return false;
		
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
			sender.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.noPermissionAtAll"));
		return true;
	}
	
	/**
	 * Announce a new game (in arena)
	 * @param sender
	 * @param args
	 */
	protected void announceCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Join a game (in arena)
	 * @param sender
	 * @param args
	 */
	protected void joinCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * List available arenas
	 * @param sender
	 * @param args
	 */
	protected void arenasCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Show information (on arena)
	 * @param sender
	 * @param args
	 */
	protected void infoCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Start game (spleefers only)
	 * @param sender
	 * @param args
	 */
	protected void startCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Start count down (of arena)
	 * @param sender
	 * @param args
	 */
	protected void countdownCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Leave a game
	 * @param sender
	 * @param args
	 */
	protected void leaveCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Stop a game
	 * @param sender
	 * @param args
	 */
	protected void stopCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Delete a game
	 * @param sender
	 * @param args
	 */
	protected void deleteCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Reset a game (admin/moderator delete game)
	 * @param sender
	 * @param args
	 */
	protected void resetCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Watch a game
	 * @param sender
	 * @param args
	 */
	protected void watchCommand(CommandSender sender, String[] args) {
		sender.sendMessage("TODO - " + args[0]);
		//TODO: implement
	}
	
	/**
	 * Shows unknown command error
	 * @param sender
	 * @param command
	 * @return true
	 */
	protected boolean unknownCommand(CommandSender sender, String command) {
		sender.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.unknownCommand", "[COMMAND]", command));
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
