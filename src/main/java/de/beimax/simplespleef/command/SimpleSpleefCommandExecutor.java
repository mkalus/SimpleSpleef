/**
 * 
 */
package de.beimax.simplespleef.command;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import de.beimax.simplespleef.game.Game;
import de.beimax.simplespleef.game.GameHandler;

/**
 * @author mkalus
 * Simple Spleef command handler
 */
public class SimpleSpleefCommandExecutor implements CommandExecutor {
	/**
	 * list of all commands
	 */
	private final static String[] commands = {"help", "announce", "join", "arenas", "info", "list", "start", "countdown", "leave", "stop", "delete", "reset", "watch"};
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
			e.printStackTrace();
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
			// print command
			printCommandString(sender, commands.get(command));
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
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		String arena = this.getArenaNameFromArgument(sender, args, 1);
		if (arena != null) { // no errors - then try to announce new game
			this.gameHandler.announce(sender, arena);
		}
	}
	
	/**
	 * Join a game (in arena)
	 * @param sender
	 * @param args
	 */
	protected void joinCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		String arena = this.getArenaNameFromArgument(sender, args, 1);
		if (arena != null) { // no errors - then try to announce new game
			this.gameHandler.join(sender, arena);
		}
	}
	
	/**
	 * List available arenas
	 * @param sender
	 * @param args
	 */
	protected void arenasCommand(CommandSender sender, String[] args) {
		//get current games
		List<String> games = this.gameHandler.getGameIds();
		// get all possible games
		Map<String, Boolean> arenas = this.gameHandler.getPossibleGames();
		if (arenas == null) return; // none - unlikely, but possible...
		// cycle through possible games
		for (Entry<String, Boolean> arena: arenas.entrySet()) {
			// color and information on game
			ChatColor color;
			String name = arena.getKey(); // id of arena
			String information;
			if (arena.getValue() == false) {
				color = ChatColor.DARK_GRAY; // arena has been disabled in the the config
				information = this.gameHandler.getPlugin().ll("feedback.arenaDisabled");
			}
			else if (games.contains(name)) { // is it an active game?
				// game active or still joinable?
				Game activeGame = this.gameHandler.getGameByName(name);
				if (activeGame.isJoinable()) {
					color = ChatColor.GREEN; // joinable
					information = this.gameHandler.getPlugin().ll("feedback.arenaJoinable");
				}
				else {
					color = ChatColor.LIGHT_PURPLE; // not joinable - because running
					information = this.gameHandler.getPlugin().ll("feedback.arenaInProgress");
				}
				// ok, gather some more information on the game to display
				information = information + " " + activeGame.getNumberOfPlayers();
			} else {
				color = ChatColor.GRAY; // not an active game
				information = null; // no information
			}
			sender.sendMessage(color + name + (information!=null?(ChatColor.GRAY + " - " + information):""));
		}
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
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		this.gameHandler.start(sender);
	}
	
	/**
	 * Start count down (of arena)
	 * @param sender
	 * @param args
	 */
	protected void countdownCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		String arena = this.getArenaNameFromArgument(sender, args, 1);
		// if player and no arena defined - arena is unset, so player can start current arena
		if (arena != null && args.length < 2 && !isConsole(sender))
			this.gameHandler.start(sender);
		else if (arena != null) { // no errors - then try to announce new game
			this.gameHandler.countdown(sender, arena);
		}
	}
	
	/**
	 * Leave a game
	 * @param sender
	 * @param args
	 */
	protected void leaveCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		this.gameHandler.leave(sender);
	}
	
	/**
	 * Stop a game
	 * @param sender
	 * @param args
	 */
	protected void stopCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		this.gameHandler.stop(sender);
	}
	
	/**
	 * Delete a game
	 * @param sender
	 * @param args
	 */
	protected void deleteCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		this.gameHandler.delete(sender, null);
	}
	
	/**
	 * Reset a game (admin/moderator delete game)
	 * @param sender
	 * @param args
	 */
	protected void resetCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// too few arguments?
		if (tooFewArguments(sender, args, 1)) return;
		String arena = this.getArenaNameFromArgument(sender, args, 1);
		if (arena != null) {
			this.gameHandler.delete(sender, arena);
		}
	}
	
	/**
	 * Watch a game
	 * @param sender
	 * @param args
	 */
	protected void watchCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 1)) return;
		// get game from 2nd argument
		String arena = this.getArenaNameFromArgument(sender, args, 1);
		if (arena != null) { // no errors - then try to announce new game
			this.gameHandler.watch(sender, arena);
		}
	}
	
	//TODO: add further commands here...
	
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
	 * Shows unknown arena error
	 * @param sender
	 * @param command
	 * @return true
	 */
	protected boolean unknownArena(CommandSender sender, String arena) {
		sender.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
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
	
	/**
	 * Print command string from settings line
	 * @param sender
	 * @param commandString comand|description...
	 */
	protected void printCommandString(CommandSender sender, String commandString) {
		int pos = commandString.indexOf('|');
		if (pos == -1) sender.sendMessage(ChatColor.GOLD + commandString);
		else
			sender.sendMessage(ChatColor.GOLD + commandString.substring(0, pos)
					+ ": " + ChatColor.WHITE + commandString.substring(pos + 1));
	}
	
	/**
	 * checks for a minimum number of arguments
	 * @param sender
	 * @param args
	 * @param min index
	 * @return
	 */
	protected boolean tooFewArguments(CommandSender sender, String[] args, int min) {
		if (args.length < min+1) {
			sender.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.tooFewArguments", "[MIN]", String.valueOf(min)));
			String commandString = this.gameHandler.getPlugin().ll("command." + args[0]);
			if (commandString != null)
				printCommandString(sender, commandString);
			return true;
		}
		return false;
	}
	
	/**
	 * checks for a maximum number of arguments
	 * @param args
	 * @param max
	 * @return
	 */
	protected boolean tooManyArguments(CommandSender sender, String[] args, int max) {
		if (args.length > max+1) {
			sender.sendMessage(ChatColor.DARK_RED + this.gameHandler.getPlugin().ll("errors.tooManyArguments", "[MAX]", String.valueOf(max)));
			String commandString = this.gameHandler.getPlugin().ll("command." + args[0]);
			if (commandString != null)
				printCommandString(sender, commandString);
			return true;
		}
		return false;
	}
	
	/**
	 * get game name from string
	 * @param args
	 * @param index
	 * @return null, if arena name is not found
	 */
	protected String getArenaNameFromArgument(CommandSender sender, String[] args, int index) {
		String name;
		// of too short, get the default arena
		if (args.length <= index) name = this.gameHandler.getDefaultArena();
		else name = args[index];
		
		// check for the existence of the arena
		if (!this.gameHandler.gameTypeOrNameExists(name)) {
			// error output, if no arena has been found
			unknownArena(sender, name);
			return null;
		}
		
		return name;
	}
}
