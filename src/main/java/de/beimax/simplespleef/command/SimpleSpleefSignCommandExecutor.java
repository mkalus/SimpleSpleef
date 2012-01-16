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

import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.beimax.simplespleef.SimpleSpleef;

/**
 * @author mkalus
 * Execute sign commands
 */
public class SimpleSpleefSignCommandExecutor {
	/**
	 * main method of the class - takes a player and a clicked sign (to be analyzed and possibly executed)
	 * @param player
	 * @param sign
	 */
	public void parseSimpleSpleefSign(Player player, Sign sign) {
		// check if sign is a spleef sign
		if (!isSimpleSpleefSign(sign)) return;
		
		// ok, construct command from the next few lines
		StringBuilder sb = new StringBuilder();
		
		// What command do we handle here?
		CommandSender sender;
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.useSignPermissions")) { // separate sign permissions
			sb.append("spleefsigncmd "); //special sign command
			sb.append(player.getName()); //include player name in command name
			sender = SimpleSpleef.getPlugin().getServer().getConsoleSender();
		} else { // simulate "normal" player command
			sb.append("spleef");
			sender = player;
		}
		
		boolean first = true;
		for (String line : sign.getLines()) {
			if (first) { // ignore first line
				first = false;
			} else {
				sb.append(' ').append(line);
			}
		}
		
		// dispatch the command
		SimpleSpleef.getPlugin().getServer().dispatchCommand(sender, sb.toString());
	}

	/**
	 * checks if a sign is a SimpleSpleef sign
	 * @return
	 */
	private boolean isSimpleSpleefSign(Sign sign) {
		// what should the first line of the sign be?
		String signsFirstLine = SimpleSpleef.getPlugin().getConfig().getString("settings.signsFirstLine", "[Spleef]");
		
		// is the first line ok?
		if (sign.getLine(0) == null || !sign.getLine(0).equals(signsFirstLine)) return false; // nope
		// yes!
		return true;
	}
}
