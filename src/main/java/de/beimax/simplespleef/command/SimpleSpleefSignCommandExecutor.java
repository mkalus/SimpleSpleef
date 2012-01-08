/**
 * 
 */
package de.beimax.simplespleef.command;

import org.bukkit.block.Sign;
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
		StringBuilder sb = new StringBuilder().append("spleef");
		
		boolean first = true;
		for (String line : sign.getLines()) {
			if (first) { // ignore first line
				first = false;
			} else {
				sb.append(' ').append(line);
			}
		}
		
		// dispatch the command
		SimpleSpleef.getPlugin().getServer().dispatchCommand(player, sb.toString());
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