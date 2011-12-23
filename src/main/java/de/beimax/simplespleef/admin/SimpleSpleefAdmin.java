/**
 * 
 */
package de.beimax.simplespleef.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.command.SimpleSpleefCommandExecutor;
import de.beimax.simplespleef.util.ConfigHelper;

/**
 * @author mkalus
 * Admin handler that solves admins' actions
 */
public class SimpleSpleefAdmin {
	/**
	 * reference to plugin
	 */
	private final SimpleSpleef plugin;
	
	/**
	 * saves which command senders have selected which arena as current one
	 */
	private HashMap<CommandSender, String> selectedArenas;
	
	/**
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public SimpleSpleefAdmin(SimpleSpleef plugin) {
		this.plugin = plugin;
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
		
		// get admin command
		String adminCommand = args[1].toLowerCase();
		if (adminCommand.equals("help")) {
			helpCommand(sender);
		} else if (adminCommand.equals("selected")) {
			selectedCommand(sender);
		} else if (adminCommand.equals("setarena")) {
			// check argument length
			if (args.length != 3) {
				sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("adminerrors.oneArgument", "[COMMAND]", adminCommand));
				return;
			}
			setarenaCommand(sender, args[2]);
		} else if (adminCommand.equals("addarena")) {
			// check argument length
			if (args.length != 3) {
				sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("adminerrors.oneArgument", "[COMMAND]", adminCommand));
				return;
			}
			addarenaCommand(sender, args[2]);
		} else if (adminCommand.equals("delarena")) {
			// check argument length
			if (args.length != 3) {
				sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("adminerrors.oneArgument", "[COMMAND]", adminCommand));
				return;
			}
			delarenaCommand(sender, args[2]);
		} else if (adminCommand.equals("arena")) {
			//TODO
		} else if (adminCommand.equals("floor")) {
			//TODO
		} else if (adminCommand.equals("loose")) {
			//TODO
		} else if (adminCommand.equals("spawn")) {
			//TODO
		} else if (adminCommand.equals("enable")) {
			//TODO
		} else if (adminCommand.equals("disable")) {
			//TODO
		} else if (adminCommand.equals("set")) {
			//TODO
		} else // unknown command feedback
			sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("errors.unknownCommand", "[COMMAND]", adminCommand));
	}
	
	/**
	 * print admin help
	 * @param sender
	 */
	protected void helpCommand(CommandSender sender) {
		// get help lines
		String[] lines = plugin.ll("admin.help").split("\n");
		for (String line : lines)
			SimpleSpleefCommandExecutor.printCommandString(sender, line);
	}
	
	
	/**
	 * print selected arena and list of all arenas
	 * @param sender
	 */
	protected void selectedCommand(CommandSender sender) {
		// get all possible games
		Map<String, Boolean> arenas = this.plugin.getGameHandler().getPossibleGames();
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
		// get all possible games
		Map<String, Boolean> arenas = this.plugin.getGameHandler().getPossibleGames();
		for (Entry<String, Boolean> checkArena: arenas.entrySet()) {
			if (checkArena.getKey().equals(id)) {
				// set new default arena
				setSelectedArena(sender, id);
				// feedback
				sender.sendMessage(ChatColor.GREEN + this.plugin.ll("adminfeedback.setarena", "[ARENA]", id));
				return;
			}
		}
		// no arena found by that name => error
		sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("errors.unknownArena", "[ARENA]", id));
	}

	/**
	 * Add an arena
	 * @param sender
	 * @param arena
	 */
	protected void addarenaCommand(CommandSender sender, String arena) {
		// get all possible games
		Map<String, Boolean> arenas = this.plugin.getGameHandler().getPossibleGames();
		// arena name to lower case
		String id = arena.toLowerCase();
		// check if arena exists already
		if (arenas.get(id) != null) {
			sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("adminerrors.addarenaArenaExists", "[ARENA]", arena));
			return;
		}
		// create new arena entry in config
		ConfigHelper configHelper = new ConfigHelper(this.plugin);
		if (!configHelper.createNewArena(id, arena)) {
			sender.sendMessage(ChatColor.DARK_RED + "Internal error: Could not create arena - see log file for details.");
			return;
		}
		// set default arena
		setSelectedArena(sender, arena);
		// feedback to user
		sender.sendMessage(ChatColor.GREEN + this.plugin.ll("adminfeedback.addarena", "[ARENA]", arena));
	}

	/**
	 * Add an arena
	 * @param sender
	 * @param arena
	 */
	protected void delarenaCommand(CommandSender sender, String arena) {
		// arena name to lower case
		String id = arena.toLowerCase();
		// get all possible games
		Map<String, Boolean> arenas = this.plugin.getGameHandler().getPossibleGames(); //TODO: write method to do this!
		boolean found = false;
		for (Entry<String, Boolean> checkArena: arenas.entrySet()) {
			if (checkArena.getKey().equals(id)) {
				found = true;
				break;
			}
		}
		// not found?
		if (!found) {
			// no arena found by that name => error
			sender.sendMessage(ChatColor.DARK_RED + this.plugin.ll("errors.unknownArena", "[ARENA]", id));
			return;
		}
		
		// ok, now delete arena config
		this.plugin.getConfig().set("arenas." + id, null);

		// save configuration now
		this.plugin.saveConfig();

		// feedback to user
		sender.sendMessage(ChatColor.GREEN + this.plugin.ll("adminfeedback.delarena", "[ARENA]", arena));
	}

	// TODO add new commands here

	/**
	 * gets or defines currently selected arena
	 * @param sender
	 * @return
	 */
	public String getSelectedArena(CommandSender sender) {
		String arena = selectedArenas.get(sender);
		
		// if not found, define default arena as current one
		if (arena == null)
			return this.plugin.getGameHandler().getDefaultArena();
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
}
