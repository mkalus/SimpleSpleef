/**
 * This file is part of the SimpleSpleef bukkit plugin.
 * Copyright (C) 2011 Maximilian Kalus
 * See http://dev.bukkit.org/server-mods/simple-spleef/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
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
import org.bukkit.entity.Player;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.game.Game;

/**
 * @author mkalus
 * Simple Spleef command handler
 */
public class SimpleSpleefCommandExecutor implements CommandExecutor {
	/**
	 * list of all commands
	 */
	private final static String[] commands = {"help", "announce", "join", "arenas", "info", "list", "ready", "start", "countdown", "leave", "stop", "delete", "reset", "watch", "back", "admin" };
	/**
	 * commands possible from the console
	 */
	private final static String[] consoleCommands = {"help", "announce", "arenas", "info", "list", "countdown", "reset", "admin"};

	/**
	 * Print command string from settings line
	 * @param sender
	 * @param commandString comand|description...
	 */
	public static void printCommandString(CommandSender sender, String commandString) {
		int pos = commandString.indexOf('|');
		if (pos == -1) sender.sendMessage(ChatColor.GOLD + commandString);
		else
			sender.sendMessage(ChatColor.GOLD + commandString.substring(0, pos)
					+ ": " + ChatColor.WHITE + commandString.substring(pos + 1));
	}
	
	/**
	 * Executed whenever simplespeef is hit by a command
	 */
	public boolean onCommand(CommandSender sender, Command command, String label,
			String[] args) {
		// spleef admin short command?
		if (label.equalsIgnoreCase("spla")) {
			// expand arguments by prepending admin
			String[] expandargs = new String[args.length+1];
			expandargs[0] = "admin";
			for (int i = 0; i < args.length; i++)
				expandargs[i+1] = args[i];
			args = expandargs;
		}
		
		// how many arguments?
		if (args.length == 0 || args[0].equalsIgnoreCase("help")) return helpCommand(sender);
		
		// first argument is subcommand
		String subcommand = args[0].toLowerCase();

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

		// check permissions
		if (!SimpleSpleef.checkPermission(sender, "simplespleef." + subcommand))
			return permissionMissing(sender, subcommand);

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
		if (!SimpleSpleef.checkPermission(sender, "simplespleef.help")) return false;
		
		// get all commands in language file
		Map<String, String> commands = SimpleSpleef.getPlugin().lls("command");
		// control if there is any output:
		boolean output = false;
		// either commands for player or for console
		String[] commandList = isConsole(sender)?SimpleSpleefCommandExecutor.consoleCommands:SimpleSpleefCommandExecutor.commands;
		
		// cycle through commands
		for (String command : commandList) {
			// check permission on command
			if (!SimpleSpleef.checkPermission(sender, "simplespleef." + command)) continue;
			// check if we are on the console and check command in that case
			if (isConsole(sender) && !isConsoleCommand(command)) continue;
			// we are cleared and may output the command nicely
			output = true;
			// print command
			printCommandString(sender, commands.get(command));
		}
		// was there output? if not show response
		if (!output)
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.noPermissionAtAll"));
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
			SimpleSpleef.getGameHandler().announce(sender, arena);
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
			SimpleSpleef.getGameHandler().join(sender, arena);
		}
	}
	
	/**
	 * List available arenas
	 * @param sender
	 * @param args
	 */
	protected void arenasCommand(CommandSender sender, String[] args) {
		//get current games
		List<String> games = SimpleSpleef.getGameHandler().getGameIds();
		// get all possible games
		Map<String, Boolean> arenas = SimpleSpleef.getGameHandler().getPossibleGames();
		if (arenas == null) return; // none - unlikely, but possible...
		// cycle through possible games
		for (Entry<String, Boolean> arena: arenas.entrySet()) {
			// color and information on game
			ChatColor color;
			String name = arena.getKey(); // id of arena
			// name of arena
			String fullName = SimpleSpleef.getPlugin().getConfig().getString("arenas." + name + ".name");
			String information;
			if (arena.getValue() == false) {
				color = ChatColor.DARK_GRAY; // arena has been disabled in the the config
				information = SimpleSpleef.getPlugin().ll("feedback.arenaDisabled");
			}
			else if (games.contains(name)) { // is it an active game?
				// game active or still joinable?
				Game activeGame = SimpleSpleef.getGameHandler().getGameByName(name);
				if (activeGame.isJoinable() || activeGame.isReady()) {
					color = ChatColor.GREEN; // joinable
					information = SimpleSpleef.getPlugin().ll("feedback.arenaJoinable");
				}
				else {
					color = ChatColor.LIGHT_PURPLE; // not joinable - because running
					information = SimpleSpleef.getPlugin().ll("feedback.arenaInProgress");
				}
				// ok, gather some more information on the game to display
				information = information + " " + activeGame.getNumberOfPlayers();
			} else {
				color = ChatColor.GRAY; // not an active game
				information = null; // no information
			}
			// create feedback
			StringBuilder builder = new StringBuilder();
			builder.append(color).append(name);
			if (information != null) builder.append(ChatColor.GRAY).append(" - ").append(information);
			if (fullName != null) builder.append(ChatColor.GRAY).append(" (").append(fullName).append(')');
			sender.sendMessage(builder.toString());
		}
	}
	
	/**
	 * Show information (on arena)
	 * @param sender
	 * @param args
	 */
	protected void infoCommand(CommandSender sender, String[] args) {
		// remember, we have 20 chat lines maximum...
		// get game from 2nd argument
		String arena = this.getArenaNameFromArgument(sender, args, 1);
		if (arena == null) return; // no arena found
		
		// if player and no arena defined - arena is unset, so player can start current arena
		String possibleArena = getPossiblePlayerSpectatorArena(sender, args, arena);
		if (possibleArena != null) arena = possibleArena;

		SimpleSpleef plugin = SimpleSpleef.getPlugin();
		
		// is game active?
		Game game = SimpleSpleef.getGameHandler().getGameByName(arena);
		
		// ok, define information on arena and print it
		sender.sendMessage(plugin.ll("feedback.infoHeader", "[ARENA]", ChatColor.DARK_AQUA + arena));
		// full name of arena
		sender.sendMessage(plugin.ll("feedback.infoName", "[NAME]", ChatColor.DARK_AQUA + plugin.getConfig().getString("arenas." + arena + ".name", "---")));
		// status of arena
		String information;
		ChatColor color;
		if (game != null) { // game running
			if (game.isJoinable() || game.isReady()) {
				information = SimpleSpleef.getPlugin().ll("feedback.arenaJoinable");
				color = ChatColor.GREEN;
			} else {
				information = SimpleSpleef.getPlugin().ll("feedback.arenaInProgress");
				color = ChatColor.LIGHT_PURPLE;
			}
			// ok, gather some more information on the game to display
			information = information + " " + game.getNumberOfPlayers();
		} else { // no game running
			if (plugin.getConfig().getBoolean("arenas." + arena + ".enabled", true)) {
				information = SimpleSpleef.getPlugin().ll("feedback.arenaOff");
				color = ChatColor.GRAY;
			} else {
				information = SimpleSpleef.getPlugin().ll("feedback.arenaDisabled");
				color = ChatColor.DARK_GRAY;
			}
		}
		sender.sendMessage(plugin.ll("feedback.infoStatus", "[STATUS]", color + information));
		// list of spleefers and spectators
		printGamePlayersAndSpectators(sender, game);
	}
	
	/**
	 * List spleefers in arena
	 * @param sender
	 * @param args
	 */
	protected void listCommand(CommandSender sender, String[] args) {
		// remember, we have 20 chat lines maximum...
		// get game from 2nd argument
		String arena = this.getArenaNameFromArgument(sender, args, 1);
		if (arena == null) return; // no arena found
		
		// if player and no arena defined - arena is unset, so player can start current arena
		String possibleArena = getPossiblePlayerSpectatorArena(sender, args, arena);
		if (possibleArena != null) arena = possibleArena;
		
		// is game active?
		Game game = SimpleSpleef.getGameHandler().getGameByName(arena);
		// list of spleefers and spectators
		printGamePlayersAndSpectators(sender, game);
	}
	
	/**
	 * Ready for the game (spleefers only)
	 * @param sender
	 * @param args
	 */
	protected void readyCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().ready(sender);
	}
	
	/**
	 * Start game (spleefers only)
	 * @param sender
	 * @param args
	 */
	protected void startCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().start(sender);
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
			SimpleSpleef.getGameHandler().start(sender);
		else if (arena != null) { // no errors - then try to announce new game
			SimpleSpleef.getGameHandler().countdown(sender, arena);
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
		SimpleSpleef.getGameHandler().leave(sender);
	}
	
	/**
	 * Stop a game
	 * @param sender
	 * @param args
	 */
	protected void stopCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().stop(sender);
	}
	
	/**
	 * Delete a game
	 * @param sender
	 * @param args
	 */
	protected void deleteCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().delete(sender, null);
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
			SimpleSpleef.getGameHandler().delete(sender, arena);
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
		if (arena != null) { // no errors - then try watch a game
			SimpleSpleef.getGameHandler().watch(sender, arena);
		}
	}
	
	/**
	 * Admin commands
	 * @param sender
	 * @param args
	 */
	protected void adminCommand(CommandSender sender, String[] args) {
		// delegate all further stuff to admin class
		SimpleSpleef.getPlugin().getAdminClass().executeCommand(sender, args);
	}
	
	/**
	 * Back command
	 * @param sender
	 * @param args
	 */
	protected void backCommand(CommandSender sender, String[] args) {
		// too many arguments?
		if (tooManyArguments(sender, args, 0)) return;
		SimpleSpleef.getGameHandler().back(sender);		
	}
	
	//TODO: add further commands here...
	
	/**
	 * Shows unknown command error
	 * @param sender
	 * @param command
	 * @return true
	 */
	protected boolean unknownCommand(CommandSender sender, String command) {
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownCommand", "[COMMAND]", command));
		return true;
	}

	/**
	 * Shows unknown arena error
	 * @param sender
	 * @param command
	 * @return true
	 */
	protected boolean unknownArena(CommandSender sender, String arena) {
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
		return true;
	}
	
	protected boolean permissionMissing(CommandSender sender, String command) {
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.permissionMissing", "[COMMAND]", command));
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
	 * checks for a minimum number of arguments
	 * @param sender
	 * @param args
	 * @param min index
	 * @return
	 */
	protected boolean tooFewArguments(CommandSender sender, String[] args, int min) {
		if (args.length < min+1) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.tooFewArguments", "[MIN]", String.valueOf(min)));
			String commandString = SimpleSpleef.getPlugin().ll("command." + args[0]);
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
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.tooManyArguments", "[MAX]", String.valueOf(max)));
			String commandString = SimpleSpleef.getPlugin().ll("command." + args[0]);
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
		if (args.length <= index) name = SimpleSpleef.getGameHandler().getDefaultArena();
		else name = args[index];
		
		// check for the existence of the arena
		if (!SimpleSpleef.getGameHandler().gameTypeOrNameExists(name)) {
			// error output, if no arena has been found
			unknownArena(sender, name);
			return null;
		}
		
		return name.toLowerCase();
	}

	/**
	 * helper function to find a possible arena if no args are defined (info, list commands)
	 * @param sender
	 * @param args
	 * @param arena
	 * @return
	 */
	private String getPossiblePlayerSpectatorArena(CommandSender sender, String[] args, String arena) {
		// if player and no arena defined - arena is unset, so player can start current arena
		if (arena != null && args.length < 2 && !isConsole(sender)) {
			Game checkGame = SimpleSpleef.getGameHandler().checkPlayerInGame((Player) sender);
			if (checkGame == null) checkGame = SimpleSpleef.getGameHandler().checkSpectatorInGame((Player) sender);
			if (checkGame != null) return checkGame.getId();
		}
		return null;
	}

	/**
	 * sends a list of players and spectators of a specific game to a sender
	 * @param sender
	 * @param game
	 */
	protected void printGamePlayersAndSpectators(CommandSender sender, Game game) {
		// list of spleefers and spectators
		if (game != null && sender != null) {
			SimpleSpleef plugin = SimpleSpleef.getPlugin();
			String spleefers = game.getListOfSpleefers();
			if (spleefers != null)
				sender.sendMessage(plugin.ll("feedback.infoSpleefers", "[SPLEEFERS]", spleefers));
			if (game.supportsReady()) {
				String unready = game.getListOfUnreadySpleefers();
				if (unready != null)
					sender.sendMessage(plugin.ll("feedback.infoUnreadySpleefers", "[SPLEEFERS]", unready));
			}
			String spectators = game.getListOfSpectators();
			if (spectators != null)
				sender.sendMessage(plugin.ll("feedback.infoSpectators", "[SPECTATORS]", spectators));
		}		
	}
}
