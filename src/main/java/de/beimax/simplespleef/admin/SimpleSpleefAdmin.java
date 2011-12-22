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
			//TODO
		} else if (adminCommand.equals("addarena")) {
			//TODO
		} else if (adminCommand.equals("delarena")) {
			//TODO
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
		}
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
}
