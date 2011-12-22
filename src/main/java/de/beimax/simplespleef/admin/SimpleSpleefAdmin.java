/**
 * 
 */
package de.beimax.simplespleef.admin;

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
	 * Constructor
	 * @param plugin reference to plugin
	 */
	public SimpleSpleefAdmin(SimpleSpleef plugin) {
		this.plugin = plugin;
	}

	/**
	 * Execute command - this is called by the command executor
	 * @param sender
	 * @param args
	 */
	public void executeCommand(CommandSender sender, String[] args) {
		//TODO get or define currently selected arena
		
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
		//TODO
	}
	// TODO: implement actions
}
