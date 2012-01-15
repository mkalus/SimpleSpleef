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
package de.beimax.simplespleef.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.Region;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.command.SimpleSpleefCommandExecutor;
import de.beimax.simplespleef.util.ConfigHelper;
import de.beimax.simplespleef.util.LocationHelper;

/**
 * @author mkalus
 * Admin handler that solves admins' actions
 */
public class SimpleSpleefAdmin {
	/**
	 * commands possible from the console
	 */
	private final static String[] consoleCommands = {"help", "addarena", "delarena", "disable", "enable", "reload"};
	/**
	 * saves which command senders have selected which arena as current one
	 */
	private HashMap<CommandSender, String> selectedArenas;
	
	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public SimpleSpleefAdmin() {
		selectedArenas = new HashMap<CommandSender, String>();
	}

	/**
	 * Execute command - this is called by the command executor
	 * @param sender
	 * @param args
	 */
	public void executeCommand(CommandSender sender, String[] args) {
		// only /spl admin entered
		if (args.length < 2) {
			helpCommand(sender);
			return;
		}

		// check arena definition after command
		boolean checkArena = false;
		// get admin command
		String adminCommand = args[1].toLowerCase();
		
		// is it a console command and it cannot be executed from the console?
		if (isConsole(sender) && !isConsoleCommand(adminCommand)) {
			sender.sendMessage("This command cannot be executed from the consolse");
			return;
		}

		// what command is selected?
		if (adminCommand.equals("help")) {
			helpCommand(sender);
		} else if (adminCommand.equals("selected")) {
			selectedCommand(sender);
		} else if (adminCommand.equals("setarena")) {
			// check argument length
			if (checkThreeArgs(sender, args, adminCommand))
				setarenaCommand(sender, args[2]);
		} else if (adminCommand.equals("addarena")) {
			// check argument length
			if (checkThreeArgs(sender, args, adminCommand))
				addarenaCommand(sender, args[2]);
		} else if (adminCommand.equals("delarena")) {
			// check argument length
			if (checkThreeArgs(sender, args, adminCommand))
				delarenaCommand(sender, args[2]);
		} else if (adminCommand.equals("arena") || adminCommand.equals("floor") || adminCommand.equals("loose") || adminCommand.equals("lose")) {
			if (adminCommand.equals("loose")) adminCommand = "lose"; //correct spelling
			// check for WorldEdit selection
			if (args.length == 2 && SimpleSpleef.getWorldEditAPI() != null) {
				defineArenaPointWorldEdit(sender, adminCommand);
				checkArena = true;
			} // check a/b
			else if (checkThirdAB(sender, args, adminCommand)) {
				defineArenaPoint(sender, args[2], adminCommand);
				checkArena = true;
			}
		} else if (adminCommand.equals("spawn")) {
			if (checkThirdSpawnName(sender, args, adminCommand)) {
				defineSpawnPoint(sender, args[2], adminCommand);
				checkArena = true;
			}
		} else if (adminCommand.equals("enable")) {
			if (checkThreeArgs(sender, args, adminCommand))
				if (enableArena(sender, args[2]))
					checkArena = true;
		} else if (adminCommand.equals("disable")) {
			if (checkThreeArgs(sender, args, adminCommand))
				if (disableArena(sender, args[2]))
					checkArena = true;
		} else if (adminCommand.equals("reload")) {
			reloadConfig(sender);
		} else // unknown command feedback
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownCommand", "[COMMAND]", adminCommand));
		
		// should arena definition be checked?
		if (checkArena) checkArena(sender);
	}

	/**
	 * checks, if there are exactly three arguments in the list 
	 * @param sender
	 * @param args
	 * @param adminCommand
	 * @return
	 */
	protected boolean checkThreeArgs(CommandSender sender, String[] args, String adminCommand) {
		// check argument length
		if (args.length != 3) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("adminerrors.oneArgument", "[COMMAND]", adminCommand));
			return false;
		}
		return true;
	}
	
	/**
	 * checks, if there are exactly four arguments in the list 
	 * @param sender
	 * @param args
	 * @param adminCommand
	 * @return
	 */
	protected boolean checkFourArgs(CommandSender sender, String[] args, String adminCommand) {
		// check argument length
		if (args.length < 4) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("adminerrors.twoArguments", "[COMMAND]", adminCommand));
			return false;
		}
		// more than four arguments -> reduce number three
		if (args.length > 4) {
			StringBuilder builder = new StringBuilder();
			for (int i = 3; i < args.length; i++) {
				if (i > 3) builder.append(' ');
				builder.append(args[i]);
			}
			// replace arg 4
			args[3] = builder.toString();
		}
		return true;
	}
	
	/**
	 * checks, if the spawn name is set and defined
	 * @param sender
	 * @param args
	 * @param adminCommand
	 * @return
	 */
	protected boolean checkThirdSpawnName(CommandSender sender, String[] args, String adminCommand) {
		// check argument length
		if (!checkThreeArgs(sender, args, adminCommand)) return false;
		// check third argument
		String spawn = args[2].toLowerCase();
		if (spawn.equals("lounge") || spawn.equals("game") || spawn.equals("spectator") || spawn.equals("loose")
				 || spawn.equals("lose") || spawn.equals("red") || spawn.equals("blue"))
			return true;
		// error feedback
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("adminerrors.oneArgumentSpawn"));
		return false;		
	}
	
	/**
	 * check wheter the thrid argumen is a or b
	 * @param sender
	 * @param args
	 * @param adminCommand
	 * @return
	 */
	protected boolean checkThirdAB(CommandSender sender, String[] args,
			String adminCommand) {
		if (args.length != 3) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("adminerrors.oneArgument", "[COMMAND]", adminCommand));
			return false;
		}
		
		if (args[2].equalsIgnoreCase("a") || args[2].equalsIgnoreCase("b")) return true;
		sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("adminerrors.aOrB", "[COMMAND]", adminCommand));
		return false;
	}

	/**
	 * print admin help
	 * @param sender
	 */
	protected void helpCommand(CommandSender sender) {
		// get help lines
		String[] lines = SimpleSpleef.getPlugin().ll("admin.help").split("\n");
		for (String line : lines)
			SimpleSpleefCommandExecutor.printCommandString(sender, line);
	}
	
	
	/**
	 * print selected arena and list of all arenas
	 * @param sender
	 */
	protected void selectedCommand(CommandSender sender) {
		// get all possible games
		Map<String, Boolean> arenas = SimpleSpleef.getGameHandler().getPossibleGames();
		// get selected arena
		String selected = getSelectedArena(sender);
		for (Entry<String, Boolean> arena: arenas.entrySet()) {
			sender.sendMessage((arena.getValue()?ChatColor.DARK_BLUE:ChatColor.GRAY) + arena.getKey() + (selected.equals(arena.getKey())?ChatColor.WHITE + " *":""));
		}
	}
	
	/**
	 * Add an arena
	 * @param sender
	 * @param arena
	 */
	protected void setarenaCommand(CommandSender sender, String arena) {
		// arena name to lower case
		String id = arena.toLowerCase();
		// check arena existence
		if (!SimpleSpleef.getGameHandler().gameTypeOrNameExists(id)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
			return;
		}

		// set new default arena
		setSelectedArena(sender, id);
		// feedback
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.setarena", "[ARENA]", id));
	}

	/**
	 * Add an arena
	 * @param sender
	 * @param arena
	 */
	protected void addarenaCommand(CommandSender sender, String arena) {
		// get all possible games
		Map<String, Boolean> arenas = SimpleSpleef.getGameHandler().getPossibleGames();
		// arena name to lower case
		String id = arena.toLowerCase();
		// check if arena exists already
		if (arenas.get(id) != null) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("adminerrors.addarenaArenaExists", "[ARENA]", arena));
			return;
		}
		// create new arena entry in config
		ConfigHelper configHelper = new ConfigHelper();
		if (!configHelper.createNewArena(id, arena)) {
			sender.sendMessage(ChatColor.DARK_RED + "Internal error: Could not create arena - see log file for details.");
			return;
		}
		// set default arena
		setSelectedArena(sender, arena);
		// feedback to user
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.addarena", "[ARENA]", arena));
	}

	/**
	 * Add an arena
	 * @param sender
	 * @param arena
	 */
	protected void delarenaCommand(CommandSender sender, String arena) {
		// arena name to lower case
		String id = arena.toLowerCase();
		// does arena exist?
		if (!SimpleSpleef.getGameHandler().gameTypeOrNameExists(id)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", id));
			return;
		}
		
		// ok, now delete arena config
		SimpleSpleef.getPlugin().getConfig().set("arenas." + id, null);

		// save configuration now
		SimpleSpleef.getPlugin().saveConfig();

		// feedback to user
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.delarena", "[ARENA]", arena));
	}

	/**
	 * define an arena point depending on point and position of player
	 * @param sender
	 * @param aOrB
	 * @param adminCommand
	 */
	protected void defineArenaPoint(CommandSender sender, String aOrB,
			String adminCommand) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.notAPlayer", "[PLAYER]", sender.getName()));
			return;
		}
		
		// correct case
		aOrB = aOrB.toLowerCase();
		adminCommand = adminCommand.toLowerCase();
		
		// get player location and arena
		String arena = getSelectedArena(sender);
		//check arena existence
		if (!SimpleSpleef.getGameHandler().gameTypeOrNameExists(arena)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
			return;
		}

		// get arena section
		ConfigurationSection arenaSection = SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas." + arena);
		
		// create section, if needed
		if (!arenaSection.isConfigurationSection(adminCommand))
			arenaSection.createSection(adminCommand);
		// get this section
		ConfigurationSection mySection = arenaSection.getConfigurationSection(adminCommand);
		// enable section
		mySection.set("enabled", true);
		// create section with location stuff - block which the player is standing on
		mySection.createSection(aOrB, LocationHelper.getXYZLocation(((Player) sender).getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation()));
		
		// save config to file
		SimpleSpleef.getPlugin().saveConfig();
		
		// feedback to player
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.defineArenaPoint", "[ARENA]", arena, "[POINT]", aOrB, "[SECTION]", adminCommand));
	}

	/**
	 * define an arena arena depending on WorldEdit selection - API is not checked in this method!
	 * @param sender
	 * @param adminCommand
	 */
	protected void defineArenaPointWorldEdit(CommandSender sender,
			String adminCommand) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.notAPlayer", "[PLAYER]", sender.getName()));
			return;
		}
		// correct case
		adminCommand = adminCommand.toLowerCase();

		// get WorldEdit Session
		LocalSession session = SimpleSpleef.getWorldEditAPI().getSession((Player) sender);

		// get player location and arena
		String arena = getSelectedArena(sender);
		//check arena existence
		if (!SimpleSpleef.getGameHandler().gameTypeOrNameExists(arena)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
			return;
		}

		// is a region defined?
		Region region;
		try {
			region = session.getSelection(session.getSelectionWorld());
		} catch (Exception e) { // error in selection
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("adminerrors.worldEditRegion"));
			return;
		}
		// get minimum and maximum vectors
		Vector minP = region.getMinimumPoint();
		Vector maxP = region.getMaximumPoint();
		// get world
		World world = SimpleSpleef.getPlugin().getServer().getWorld(region.getWorld().getName());
		// sanity check
		if (world == null || minP == null || maxP == null) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("adminerrors.worldEditRegion"));
			return;			
		}

		// get arena section
		ConfigurationSection arenaSection = SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas." + arena);
		
		// create section, if needed
		if (!arenaSection.isConfigurationSection(adminCommand))
			arenaSection.createSection(adminCommand);
		// get this section
		ConfigurationSection mySection = arenaSection.getConfigurationSection(adminCommand);
		// enable section
		mySection.set("enabled", true);
		// create section with location stuff - selected blocks
		mySection.createSection("a", LocationHelper.getXYZLocation(new Location(world, minP.getX(), minP.getY(), minP.getZ())));
		mySection.createSection("b", LocationHelper.getXYZLocation(new Location(world, maxP.getX(), maxP.getY(), maxP.getZ())));

		// save config to file
		SimpleSpleef.getPlugin().saveConfig();

		// feedback to player
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.defineArenaPointWorldEdit", "[ARENA]", arena, "[SECTION]", adminCommand));
	}

	/**
	 * Define a spawn point for the current arena depending on point and position of player
	 * @param sender
	 * @param spawn
	 * @param adminCommand
	 */
	protected void defineSpawnPoint(CommandSender sender, String spawn,
			String adminCommand) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.notAPlayer", "[PLAYER]", sender.getName()));
			return;
		}
		
		// correct case
		spawn = spawn.toLowerCase();
		adminCommand = adminCommand.toLowerCase();
		
		// get player location and arena
		String arena = getSelectedArena(sender);
		// check arena existence
		if (!SimpleSpleef.getGameHandler().gameTypeOrNameExists(arena)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
			return;
		}

		// get arena section
		ConfigurationSection arenaSection = SimpleSpleef.getPlugin().getConfig().getConfigurationSection("arenas." + arena);

		// create section
		arenaSection.createSection(spawn + "Spawn", LocationHelper.getExactLocation(((Player) sender).getLocation(), true));

		// save config to file
		SimpleSpleef.getPlugin().saveConfig();
		
		// feedback to player
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.defineSpawnPoint", "[ARENA]", arena, "[SPAWN]", spawn));
	}

	/**
	 * check arena changed by sender -> do updates
	 */
	protected void checkArena(CommandSender sender) {
		// reload the configuration of everything
		SimpleSpleef.getGameHandler().reloadConfig();
	}
	
	/**
	 * reload the config
	 * @param sender
	 */
	protected void reloadConfig(CommandSender sender) {
		SimpleSpleef.getPlugin().reloadSimpleSpleefConfiguration();
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.reload"));
	}

	/**
	 * enable arena
	 * @param sender
	 * @param string
	 * @return
	 */
	protected boolean enableArena(CommandSender sender, String arena) {
		// does arena exist?
		if (!SimpleSpleef.getGameHandler().gameTypeOrNameExists(arena)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
			return false;
		}
		// enable arena
		SimpleSpleef.getPlugin().getConfig().set("arenas." + arena.toLowerCase() + ".enabled", true);
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.enable", "[ARENA]", arena));
		return true;
	}

	/**
	 * disable arena
	 * @param sender
	 * @param string
	 * @return
	 */
	protected boolean disableArena(CommandSender sender, String arena) {
		// does arena exist?
		if (!SimpleSpleef.getGameHandler().gameTypeOrNameExists(arena)) {
			sender.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.unknownArena", "[ARENA]", arena));
			return false;
		}
		// disable arena
		SimpleSpleef.getPlugin().getConfig().set("arenas." + arena.toLowerCase() + ".enabled", false);
		sender.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("adminfeedback.disable", "[ARENA]", arena));
		return true;
	}

	/**
	 * gets or defines currently selected arena
	 * @param sender
	 * @return
	 */
	public String getSelectedArena(CommandSender sender) {
		String arena = selectedArenas.get(sender);
		
		// if not found, define default arena as current one
		if (arena == null)
			return SimpleSpleef.getGameHandler().getDefaultArena();
		return arena;
	}
	
	/**
	 * set a selected arena for a sender
	 * @param sender
	 * @param arena
	 */
	public void setSelectedArena(CommandSender sender, String arena) {
		// arena name to lower case
		String id = arena.toLowerCase();
		// delete old key, if needed
		if (selectedArenas.containsKey(sender))
			selectedArenas.remove(sender);
		selectedArenas.put(sender, id);
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
		for (String c : SimpleSpleefAdmin.consoleCommands) {
			if (c.equals(command)) return true;
		}
		return false;
	}
}
