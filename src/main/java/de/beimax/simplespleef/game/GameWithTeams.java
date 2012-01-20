/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.LinkedList;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import de.beimax.simplespleef.SimpleSpleef;
import de.beimax.simplespleef.util.LocationHelper;

/**
 * @author mkalus
 *
 */
public class GameWithTeams extends GameStandard {
	/**
	 * Constructor
	 * @param name
	 */
	public GameWithTeams(String name) {
		super(name);
	}
	
	@Override
	public String getType() {
		return "randomteams";
	}
	
	@Override
	public boolean start() {
		//create teams at game start
		createTeams();
		return super.start();
	}
	
	@Override
	public String getNumberOfPlayers() {
		// if in progress - tell about teams
		if (isInProgress()) {
			// no spleefers - return empty string
			if (spleefers == null || spleefers.size() == 0) return "";

			int max = spleefers.size();
			return new StringBuilder().append(ChatColor.WHITE).append('(')
				.append(ChatColor.BLUE).append(spleefers.inGame(Spleefer.TEAM_BLUE))
				.append(ChatColor.WHITE).append(": ")
				.append(ChatColor.RED).append(spleefers.inGame(Spleefer.TEAM_RED))
				.append(ChatColor.WHITE).append('/')
				.append((max>0?max:'-')).append(')').toString();
		} else return super.getNumberOfPlayers();
	}

	@Override
	public String getListOfSpleefers() {
		// if in progress - tell about teams
		if (isInProgress()) {
			// no spleefers - return null
			if (spleefers == null || spleefers.size() == 0) return null;
			// create list of spleefers
			String comma = SimpleSpleef.getPlugin().ll("feedback.infoComma");
			StringBuilder builder = new StringBuilder();
			for (int team = Spleefer.TEAM_BLUE; team <= Spleefer.TEAM_RED; team++) {
				// color
				ChatColor teamColor = team==Spleefer.TEAM_BLUE?ChatColor.BLUE:ChatColor.RED;
				// append team
				builder.append(SimpleSpleef.getPlugin().ll("feedback.infoTeam",
						"[TEAM]", teamColor + SimpleSpleef.getPlugin().ll("feedback.team" + Spleefer.getTeamId(team)) + ChatColor.WHITE)).append(' ');
				int i = 0;
				for (Spleefer spleefer : spleefers.get()) {
					if (i > 0) builder.append(comma);  // no ands here...
					// in team?
					if (spleefer.getTeam() == team) {
						// lost or in game?
						if (spleefer.hasLost()) builder.append(ChatColor.RED);
						else builder.append(ChatColor.GREEN);
						builder.append(spleefer.getPlayer().getDisplayName());
						builder.append(ChatColor.GRAY);
						i++;
					}
				}
				if (i == 0) builder.append("---"); // no players in this team
				if (team == Spleefer.TEAM_BLUE) builder.append("; "); // separate both team strings
			}
			return builder.toString();
		} else return super.getListOfSpleefers();		
	}
	
	@Override
	protected boolean checkGameOver() {
		// check if teams are still here
		boolean[] stillHere = {false, false};
		// check spleefer's status
		for (Spleefer spleefer : spleefers.get()) {
			if (!spleefer.hasLost()) { // not lost?
				int team = spleefer.getTeam(); //out-of-bounds check
				if (team == Spleefer.TEAM_BLUE || team == Spleefer.TEAM_RED)
					stillHere[spleefer.getTeam() - 1] = true; // team is still here
			}
		}
		// do we have a team that isn't here?
		for (int i = 0; i < stillHere.length; i++)
			if (!stillHere[i]) return true; // one team has lost
		return false;
	}
	
	@Override
	protected void broadcastWinners(LinkedList<Spleefer> winners) {
		// get the total winners from winners list
		String broadcastKey;
		String replacePlayer = "";
		String team = "---";
		// no winner?
		if (winners.size() == 0) broadcastKey = "None";
		else { // winning team
			broadcastKey = ""; // just winTeam
			// get winning team
			team = SimpleSpleef.getPlugin().ll("feedback.team" + Spleefer.getTeamId(winners.getFirst().getTeam()));
			replacePlayer = SpleeferList.getPrintablePlayerList(winners);
		}
		// broadcast message
		String broadcastMessage = ChatColor.GOLD + SimpleSpleef.getPlugin().ll("broadcasts.winTeam" + broadcastKey, "[PLAYER]", replacePlayer, "[ARENA]", getName(), "[TEAM]", team);
		sendMessage(broadcastMessage, SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceWin", true));		
	}

	@Override
	protected void teleportPlayersAtGameStart() {
		// do red or blue spawns exist?
		Location blue = LocationHelper.configToExactLocation(configuration.getConfigurationSection("blueSpawn"));
		Location red = LocationHelper.configToExactLocation(configuration.getConfigurationSection("redSpawn"));
		for (Spleefer spleefer : spleefers.get()) {
			int team = spleefer.getTeam();
			// if spawn of player is not defined, teleport to normal game spawn
			if ((team == Spleefer.TEAM_BLUE && blue == null)
					|| (team == Spleefer.TEAM_RED && red == null))
				teleportPlayer(spleefer.getPlayer(), "game");
			else teleportPlayer(spleefer.getPlayer(), Spleefer.getTeamId(team) + "Spawn");
		}
	}

	/**
	 * even out the players to two teams and tell everybody about it
	 */
	protected void createTeams() {
		// npe checks
		if (spleefers == null || spleefers.size() == 0) return;
		
		// list of spleefers of the teams
		LinkedList<Spleefer> blueTeam = new LinkedList<Spleefer>();
		LinkedList<Spleefer> redTeam = new LinkedList<Spleefer>();
		
		// fill red team with all spleefers
		for (Spleefer spleefer : spleefers.get()) redTeam.add(spleefer);
		
		// random generator
		if (redTeam.size() > 1) { // don't change 1 player team games
			Random generator = new Random();
			while (blueTeam.size() <= redTeam.size()) { // take one of the red team members from team red to blue until they even out
				if (redTeam.size() > 1) { // checking again because team size might have changed meanwhile //TODO - work on this again...
					int index = generator.nextInt(redTeam.size() -1);
					blueTeam.add(redTeam.get(index));
					redTeam.remove(index);
				}
			}
		}
		
		// we have the teams, now set teams
		for (Spleefer spleefer : blueTeam) spleefer.setTeam(Spleefer.TEAM_BLUE);
		for (Spleefer spleefer : redTeam) spleefer.setTeam(Spleefer.TEAM_RED);
	}
}
