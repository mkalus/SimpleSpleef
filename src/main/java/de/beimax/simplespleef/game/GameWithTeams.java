/**
 * 
 */
package de.beimax.simplespleef.game;

import java.util.LinkedList;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
		// broadcast message of player teams, so everyone knows this...
		String broadcastMessage = ChatColor.BLUE + SimpleSpleef.getPlugin().ll("broadcasts.teams", "[TEAMS]", getListOfSpleefers());
		sendMessage(broadcastMessage, SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceTeam", false));
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
	public boolean team(Player player, String team) {
		if (player == null || team == null) return false; // prevent NPEs
		// is spleefer?
		Spleefer spleefer = spleefers.getSpleefer(player);
		if (spleefer == null) return false; // should not happen, because the game handler should have checked that..

		// no teams during gaming
		if (!isJoinable()) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.teamAlreadyStarted", "[ARENA]", getName()));
			return false;
		}
		// is team command disallowed in the arena?
		if (!configuration.getBoolean("teamCommand", true)) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.teamNoTeamCommand", "[ARENA]", getName()));
			return false;
		}
		// parse team string
		int teamId;
		if (team.equalsIgnoreCase("red")) teamId = Spleefer.TEAM_RED;
		else if (team.equalsIgnoreCase("blue")) teamId = Spleefer.TEAM_BLUE;
		else { // no valid team name
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.teamNoValidTeam", "[NAME]", team));
			return false;			
		}
		// is the player already part of this team?
		if (spleefer.getTeam() == teamId) {
			player.sendMessage(ChatColor.DARK_RED + SimpleSpleef.getPlugin().ll("errors.teamAlreadyInTeam", "[TEAM]", Spleefer.getTeamId(teamId)));
			return false;
		}

		// clear: join the team
		spleefer.setTeam(teamId);
		player.sendMessage(ChatColor.GREEN + SimpleSpleef.getPlugin().ll("feedback.team", "[TEAM]", Spleefer.getTeamId(teamId)));

		// broadcast message of somebody joining a team
		String broadcastMessage = ChatColor.DARK_PURPLE + SimpleSpleef.getPlugin().ll("broadcasts.team", "[PLAYER]", player.getDisplayName(), "[ARENA]", getName(), "[TEAM]", Spleefer.getTeamId(teamId));
		if (SimpleSpleef.getPlugin().getConfig().getBoolean("settings.announceTeam", false)) {
			SimpleSpleef.getPlugin().getServer().broadcastMessage(broadcastMessage); // broadcast message
		} else {
			// send message to all receivers
			sendMessage(broadcastMessage, player);
		}

		// if teamJoiningAlsoReadies is true, also ready player
		if (configuration.getBoolean("teamJoiningAlsoReadies", true)) {
			spleefer.setReady(true);
			checkReadyAndStartGame(); // check status and possibly start the game
		}
		return true;
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
		
		// fill teams with spleefers
		for (Spleefer spleefer : spleefers.get()) {
			if (spleefer.getTeam() == Spleefer.TEAM_BLUE) redTeam.add(spleefer);
			else redTeam.add(spleefer); // move all others to the red team
		}
		
		// are the teams unevenly mixed? If yes, even them out on a random basis.
		if (redTeam.size() > blueTeam.size()) evenOutTeamLists(blueTeam, redTeam);
		else if (blueTeam.size() > redTeam.size()) evenOutTeamLists(redTeam, blueTeam);
		
		// we have the teams, now set teams
		for (Spleefer spleefer : blueTeam) spleefer.setTeam(Spleefer.TEAM_BLUE);
		for (Spleefer spleefer : redTeam) spleefer.setTeam(Spleefer.TEAM_RED);
	}
	
	protected void evenOutTeamLists(LinkedList<Spleefer> smaller, LinkedList<Spleefer> larger) {
		// if the size difference is 1, ignore evening out - also ignore 1 member teams
		if (larger.size()-1 == smaller.size() || larger.size() <= 1) return;
		
		Random generator = new Random();
		while (smaller.size() < larger.size()) { // take one of the red team members from team red to blue until they even out
			int index = generator.nextInt(larger.size() -1);
			smaller.add(larger.get(index));
			larger.remove(index);
		}
	}
}
