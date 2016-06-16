package com.Ben12345rocks.VotingPlugin.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Ben12345rocks.VotingPlugin.Main;
import com.Ben12345rocks.VotingPlugin.Utils;
import com.Ben12345rocks.VotingPlugin.BonusReward.BonusVoteReward;
import com.Ben12345rocks.VotingPlugin.Config.Config;
import com.Ben12345rocks.VotingPlugin.Config.ConfigBonusReward;
import com.Ben12345rocks.VotingPlugin.Config.ConfigFormat;
import com.Ben12345rocks.VotingPlugin.Config.ConfigTopVoterAwards;
import com.Ben12345rocks.VotingPlugin.Config.ConfigVoteSites;
import com.Ben12345rocks.VotingPlugin.Data.Data;

public class User {
	static Main plugin = Main.plugin;
	private String playerName;

	private String uuid;

	public User(Main plugin) {
		User.plugin = plugin;
	}

	/**
	 * New user
	 *
	 * @param player
	 *            Player
	 */
	public User(Player player) {
		playerName = player.getName();
		uuid = player.getUniqueId().toString();
	}

	/**
	 * New user
	 *
	 * @param playerName
	 *            The user's name
	 */
	public User(String playerName) {
		this.playerName = playerName;
		uuid = Utils.getInstance().getUUID(playerName);

	}

	/**
	 * New user
	 *
	 * @param uuid
	 *            UUID
	 */
	public User(UUID uuid) {
		this.uuid = uuid.getUUID();
		playerName = Utils.getInstance().getPlayerName(this.uuid);

	}

	/**
	 * New user
	 *
	 * @param uuid
	 *            UUID
	 * @param loadName
	 *            Whether or not to preload name
	 */
	public User(UUID uuid, boolean loadName) {
		this.uuid = uuid.getUUID();
		if (loadName) {
			playerName = Utils.getInstance().getPlayerName(this.uuid);
		}
	}

	/**
	 * Add offline vote to bonus
	 */
	public void addBonusOfflineVote() {
		setBonusOfflineVotes(getBonusOfflineVotes() + 1);
	}

	public void addCumulativeReward(VoteSite voteSite) {
		Data.getInstance().addCumulativeSite(this, voteSite.getSiteName());
	}

	/**
	 *
	 * @param voteSite
	 *            VoteSite to add offline votes to
	 */
	public void addOfflineVote(VoteSite voteSite) {
		setOfflineVotes(voteSite, getOfflineVotes(voteSite) + 1);
	}

	/**
	 * Add total for VoteSite to user
	 *
	 * @param voteSite
	 *            VoteSite to add vote to
	 */
	public void addTotal(VoteSite voteSite) {
		User user = this;
		Data.getInstance().set(user,
				user.getUUID() + ".Total." + voteSite.getSiteName(),
				Data.getInstance().getTotal(user, voteSite.getSiteName()) + 1);
	}

	/**
	 *
	 * @return True if player can vote on all sites
	 */
	public boolean canVoteAll() {
		ArrayList<VoteSite> voteSites = ConfigVoteSites.getInstance()
				.getVoteSites();

		for (VoteSite voteSite : voteSites) {
			boolean canVote = canVoteSite(voteSite);
			if (!canVote) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	/**
	 *
	 * @param voteSite	VoteSite
	 * @return			True if player can vote on specified site
	 */
	public boolean canVoteSite(VoteSite voteSite) {
		String siteName = voteSite.getSiteName();
		long time = getTime(voteSite);
		if (time == 0) {
			return true;
		}
		Date date = new Date(time);
		int month = date.getMonth();
		int day = date.getDate();
		int hour = date.getHours();
		int min = date.getMinutes();

		int votedelay = ConfigVoteSites.getInstance().getVoteDelay(siteName);

		if (votedelay == 0) {
			return false;
		}

		Date voteTime = new Date(new Date().getYear(), month, day, hour, min);
		Date nextvote = DateUtils.addHours(voteTime, votedelay);

		int cday = new Date().getDate();
		int cmonth = new Date().getMonth();
		int chour = new Date().getHours();
		int cmin = new Date().getMinutes();
		Date currentDate = new Date(new Date().getYear(), cmonth, cday, chour,
				cmin);

		if ((nextvote != null) && (day != 0) && (hour != 0)) {
			if (currentDate.after(nextvote)) {
				return true;

			}
		}

		return false;
	}

	/**
	 * Check if user has voted on all sites in one day
	 *
	 * @return True if player has voted on all sites in one day, False if bonus
	 *         reward disabled or player has not voted all sites in one day
	 */
	public boolean checkAllVotes() {
		User user = this;
		if (!ConfigBonusReward.getInstance().getGiveBonusReward()) {
			return false;
		}

		ArrayList<VoteSite> voteSites = ConfigVoteSites.getInstance()
				.getVoteSites();
		ArrayList<Integer> months = new ArrayList<Integer>();
		ArrayList<Integer> days = new ArrayList<Integer>();

		for (int i = 0; i < voteSites.size(); i++) {
			months.add(Utils.getInstance().getMonthFromMili(
					user.getTime(voteSites.get(i))));
			days.add(Utils.getInstance().getDayFromMili(
					user.getTime(voteSites.get(i))));
		}

		// check months
		for (int i = 0; i < months.size(); i++) {
			if (!months.get(0).equals(months.get(i))
					|| days.get(i).equals(
							Utils.getInstance().getMonthFromMili(
									user.getTimeAll()))) {
				return false;
			}
		}

		// check days
		for (int i = 0; i < days.size(); i++) {
			if (!days.get(0).equals(days.get(i))
					|| days.get(i).equals(
							Utils.getInstance().getDayFromMili(
									user.getTimeAll()))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Get Amount of offline votes
	 *
	 * @return Amount of bonus offline votes
	 */
	public int getBonusOfflineVotes() {
		User user = this;
		return Data.getInstance().getData(user)
				.getInt(user.getUUID() + ".BonusOfflineVotes");
	}

	public int getCumulativeReward(VoteSite voteSite) {
		return Data.getInstance().getCumulativeSite(this,
				voteSite.getSiteName());
	}

	public HashMap<VoteSite, Long> getLastVoteTimesSorted() {
		HashMap<VoteSite, Long> times = new HashMap<VoteSite, Long>();

		for (VoteSite voteSite : plugin.voteSites) {
			times.put(voteSite, getTime(voteSite));
		}
		HashMap<VoteSite, Long> sorted = (HashMap<VoteSite, Long>) times
				.entrySet()
				.stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.collect(
						Collectors
						.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return sorted;
	}

	public int getOfflineTopVoter() {
		return Data.getInstance().getTopVoterAwardOffline(this);
	}

	/**
	 * Get amount of offline votes for VoteSite
	 *
	 * @param voteSite
	 *            VoteSite to get offline votes of
	 * @return Amount of offline votes
	 */
	public int getOfflineVotes(VoteSite voteSite) {
		User user = this;
		return Data
				.getInstance()
				.getData(user)
				.getInt(user.getUUID() + ".OfflineVotes."
						+ voteSite.getSiteName());
	}

	/**
	 *
	 * @return User's game name
	 */
	public String getPlayerName() {
		return playerName;

	}

	/**
	 * Get time of last vote from VoteSite for user
	 *
	 * @param voteSite
	 *            VoteSite to check for last vote
	 * @return Time in milliseconds when last vote occurred
	 */
	public long getTime(VoteSite voteSite) {
		User user = this;
		long mills = Data
				.getInstance()
				.getData(user)
				.getLong(
						uuid + ".LastVote." + voteSite.getSiteName()
						+ ".Miliseconds");
		return mills;
	}

	/**
	 * Get time of last bonus vote for user
	 *
	 * @return Time in milliseconds when last bonus reward occurred
	 */
	public long getTimeAll() {
		User user = this;
		long mills = Data.getInstance().getData(user)
				.getLong(uuid + ".LastBonus.Miliseconds");

		return mills;
	}

	/**
	 * Get total from VoteSite for user
	 *
	 * @param voteSite
	 * @return
	 */
	public int getTotal(VoteSite voteSite) {
		User user = this;
		return Data.getInstance().getData(user)
				.getInt(user.getUUID() + ".Total." + voteSite.getSiteName());
	}

	/**
	 *
	 * @return Returns totals of all votes sites
	 */
	public int getTotalVotes() {
		int total = 0;
		for (VoteSite voteSite : ConfigVoteSites.getInstance().getVoteSites()) {
			total += getTotalVotesSite(voteSite);
		}
		return total;
	}

	/**
	 * Get total votes for VoteSite
	 *
	 * @param voteSite
	 *            VoteSite
	 * @return Total votes from VoteSite
	 */
	public int getTotalVotesSite(VoteSite voteSite) {
		return Data.getInstance().getTotal(this, voteSite.getSiteName());
	}

	/**
	 * Get user's uuid
	 *
	 * @return uuid - as string
	 */
	public String getUUID() {
		return uuid;
	}

	public long getVoteTimeLast() {
		ArrayList<Long> times = new ArrayList<Long>();
		for (VoteSite voteSite : plugin.voteSites) {
			times.add(getTime(voteSite));
		}
		Long last = Collections.max(times);
		return last;
	}

	/**
	 * Trigger Bonus Reward
	 */
	public void giveBonus() {

		BonusVoteReward.getInstance().giveBonusReward(this);

	}

	@SuppressWarnings("deprecation")
	/**
	 * Give the user an item
	 * @param id	Item id
	 * @param amount	Item amount
	 * @param data		Item data
	 * @param itemName	Item name
	 * @param lore		Item lore
	 * @param enchants	Item enchants
	 */
	public void giveItem(int id, int amount, int data, String itemName,
			List<String> lore, HashMap<String, Integer> enchants) {

		if (amount == 0) {
			return;
		}

		String playerName = getPlayerName();

		ItemStack item = new ItemStack(id, amount, (short) data);
		item = Utils.getInstance().nameItem(item, itemName);
		item = Utils.getInstance().addLore(item, lore);
		Player player = Bukkit.getPlayer(playerName);
		// player.getInventory().addItem(item);

		item = Utils.getInstance().addEnchants(item, enchants);

		HashMap<Integer, ItemStack> excess = player.getInventory()
				.addItem(item);
		for (Map.Entry<Integer, ItemStack> me : excess.entrySet()) {
			player.getWorld().dropItem(player.getLocation(), me.getValue());
		}

		player.updateInventory();

	}

	/**
	 * Give the user an item, will drop on ground if inv full
	 *
	 * @param item
	 *            ItemStack to give player
	 */
	public void giveItem(ItemStack item) {
		if (item.getAmount() == 0) {
			return;
		}

		String playerName = getPlayerName();

		Player player = Bukkit.getPlayer(playerName);

		HashMap<Integer, ItemStack> excess = player.getInventory()
				.addItem(item);
		for (Map.Entry<Integer, ItemStack> me : excess.entrySet()) {
			player.getWorld().dropItem(player.getLocation(), me.getValue());
		}

		player.updateInventory();

	}

	@SuppressWarnings("deprecation")
	/**
	 * Give user money, needs vault installed
	 * @param money		Amount of money to give
	 */
	public void giveMoney(int money) {
		String playerName = getPlayerName();
		if ((Bukkit.getServer().getPluginManager().getPlugin("Vault") != null)
				&& (money > 0)) {
			Main.econ.depositPlayer(playerName, money);
		}
	}

	public void giveTopVoterAward(int place) {
		giveMoney(ConfigTopVoterAwards.getInstance().getTopVoterAwardMoney(
				place));
		try {
			for (String item : ConfigTopVoterAwards.getInstance().getItems(
					place)) {
				this.giveItem(ConfigTopVoterAwards.getInstance()
						.getTopVoterAwardItemStack(place, item));
			}
		} catch (Exception ex) {
			if (Config.getInstance().getDebugEnabled()) {
				ex.printStackTrace();
			}
		}
		ConfigTopVoterAwards.getInstance().doTopVoterAwardCommands(this, place);
		Player player = Bukkit.getPlayer(java.util.UUID.fromString(uuid));
		if (player != null) {
			player.sendMessage(Utils.getInstance().colorize(
					ConfigFormat.getInstance().getTopVoterRewardMsg()
					.replace("%place%", "" + place)));
		}
	}

	/**
	 *
	 * @return True if the player has joined before
	 */
	public boolean hasJoinedBefore() {
		return Data.getInstance().hasJoinedBefore(this);
	}

	/**
	 * Load the user's name from uuid
	 */
	public void loadName() {
		playerName = Utils.getInstance().getPlayerName(uuid);
	}

	/**
	 * Login message if player can vote
	 */
	public void loginMessage() {
		String playerName = getPlayerName();
		if (Config.getInstance().getRemindVotesEnabled()) {
			if (Utils.getInstance().hasPermission(playerName,
					"VotingPlugin.Login.RemindVotes")
					|| Utils.getInstance().hasPermission(playerName,
							"VotingPlugin.Player")) {
				if (canVoteAll()) {
					Player player = Bukkit.getPlayer(playerName);
					if (player != null) {
						String msg = Utils.getInstance().colorize(
								ConfigFormat.getInstance().getLoginMsg());
						msg = msg.replace("%player%", playerName);
						player.sendMessage(msg);
						setReminded(true);
					}

				}
			}
		}
	}

	/**
	 * Check for offline votes
	 */
	public void offVote() {
		ArrayList<VoteSite> voteSites = ConfigVoteSites.getInstance()
				.getVoteSites();

		ArrayList<String> offlineVotes = new ArrayList<String>();

		String playerName = getPlayerName();

		boolean playSound = false;

		for (VoteSite voteSite : voteSites) {
			int offvotes = getOfflineVotes(voteSite);
			if (offvotes > 0) {
				playSound = true;
				if (Config.getInstance().getDebugEnabled()) {
					plugin.getLogger()
					.info("Offline Vote Reward on Site '"
							+ voteSite.getSiteName()
							+ "' given for player '" + playerName + "'");
				}
				for (int i = 0; i < offvotes; i++) {
					offlineVotes.add(voteSite.getSiteName());
				}
			}

		}

		for (int i = 0; i < offlineVotes.size(); i++) {
			playerVote(plugin.getVoteSite(offlineVotes.get(i)));
		}
		for (int i = 0; i < offlineVotes.size(); i++) {
			setOfflineVotes(plugin.getVoteSite(offlineVotes.get(i)), 0);
		}

		for (int i = 0; i < getBonusOfflineVotes(); i++) {
			BonusVoteReward.getInstance().giveBonusReward(this);
		}

		setBonusOfflineVotes(0);

		if (playSound) {
			playVoteSound();
		}

		int place = getOfflineTopVoter();
		if (place > 0) {
			giveTopVoterAward(place);
			Data.getInstance().setTopVoterAwardOffline(this, 0);
		}

	}

	public void offVoteWorld(String world) {
		ArrayList<VoteSite> voteSites = ConfigVoteSites.getInstance()
				.getVoteSites();

		for (VoteSite voteSite : voteSites) {
			for (String reward : ConfigVoteSites.getInstance()
					.getExtraRewardRewards(voteSite.getSiteName())) {

				ArrayList<String> worlds = ConfigVoteSites.getInstance()
						.getExtraRewardWorld(voteSite.getSiteName(), reward);

				if (worlds != null) {
					if (ConfigVoteSites.getInstance()
							.getExtraRewardGiveInEachWorld(
									voteSite.getSiteName(), reward)) {
						for (String worldName : worlds) {
							if (Config.getInstance().getDebugEnabled()) {
								plugin.getLogger().info(
										"Checking world: " + worldName
										+ ", reard: " + reward
										+ ", votesite: "
										+ voteSite.getSiteName());
							}
							if (worldName != "") {
								if (worldName.equals(world)) {
									if (Config.getInstance().getDebugEnabled()) {
										plugin.getLogger().info(
												"Giving reward...");
									}
									int worldRewards = Data.getInstance()
											.getOfflineVotesWorld(this,
													voteSite.getSiteName(),
													reward, worldName);

									while (worldRewards > 0) {
										voteSite.giveExtraRewardReward(this,
												reward, 100);
										worldRewards--;
									}

									Data.getInstance().setOfflineVotesWorld(
											this, voteSite.getSiteName(),
											reward, worldName, 0);

								}
							}

						}
					} else {
						if (worlds.contains(world)) {
							int worldRewards = Data.getInstance()
									.getOfflineVotesExtraReward(this,
											voteSite.getSiteName(), reward);

							while (worldRewards > 0) {
								voteSite.giveExtraRewardReward(this, reward,
										100);
								worldRewards--;
							}

							Data.getInstance().setOfflineVotesExtraReward(this,
									voteSite.getSiteName(), reward, 0);
						}
					}
				}
			}

		}
	}

	/**
	 * Trigger a vote for the user
	 *
	 * @param voteSite
	 *            Site player voted on
	 */
	public void playerVote(VoteSite voteSite) {
		if (Config.getInstance().getBroadCastVotesEnabled()
				&& ConfigFormat.getInstance().getBroadcastWhenOnline()) {
			voteSite.broadcastVote(this);
		}
		voteSite.giveSiteReward(this);
	}

	public void playSound(String soundName, float volume, float pitch) {
		Player player = Bukkit.getPlayer(java.util.UUID.fromString(uuid));
		if (player != null) {
			Sound sound = Sound.valueOf(soundName);
			if (sound != null) {
				player.playSound(player.getLocation(), sound, volume, pitch);
			} else if (Config.getInstance().getDebugEnabled()) {
				plugin.getLogger().info("Invalid sound: " + soundName);
			}
		}
	}

	public void playVoteSound() {
		if (Config.getInstance().getVoteSoundEnabled()) {
			try {
				playSound(Config.getInstance().getVoteSoundSound(), Config
						.getInstance().getVoteSoundVolume(), Config
						.getInstance().getVoteSoundPitch());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Get whether or not user has been reminded to vote
	 *
	 * @return T
	 */
	public boolean reminded() {
		User user = this;
		return Data.getInstance().getData(user)
				.getBoolean(user.getUUID() + ".Reminded");
	}

	/**
	 * Send the user a message
	 *
	 * @param msg
	 *            Message to send
	 */
	public void sendMessage(String msg) {
		Player player = Bukkit.getPlayer(java.util.UUID.fromString(uuid));
		if ((player != null) && (msg != null)) {
			if (msg != "") {
				player.sendMessage(Utils.getInstance().colorize(
						Utils.getInstance().replacePlaceHolders(player, msg)));
			}
		}
	}

	/**
	 * Send the user a message
	 *
	 * @param msg
	 *            Message to send
	 */
	public void sendMessage(String[] msg) {
		Player player = Bukkit.getPlayer(java.util.UUID.fromString(uuid));
		if ((player != null) && (msg != null)) {

			for (int i = 0; i < msg.length; i++) {
				msg[i] = Utils.getInstance()
						.replacePlaceHolders(player, msg[i]);
			}
			player.sendMessage(Utils.getInstance().colorize(msg));

		}
	}

	/**
	 * Set Bonus offline votes
	 *
	 * @param amount
	 *            Amount of set
	 */
	public void setBonusOfflineVotes(int amount) {
		User user = this;
		Data.getInstance().set(user, user.getUUID() + ".BonusOfflineVotes",
				amount);
	}

	public void setCumulativeReward(VoteSite voteSite, int value) {
		Data.getInstance().setCumulativeSite(this, voteSite.getSiteName(),
				value);
	}

	/**
	 * Set name in player's file
	 */
	public void setName() {
		User user = this;
		Data.getInstance().set(user, user.getUUID() + ".Name",
				user.getPlayerName());
	}

	public void setOfflineTopVoter(int place) {
		Data.getInstance().setTopVoterAwardOffline(this, place);
	}

	/**
	 * Set offline votes for VoteSite for user
	 *
	 * @param voteSite
	 *            VoteSite to set
	 * @param amount
	 *            Offline Votes to set
	 */
	public void setOfflineVotes(VoteSite voteSite, int amount) {
		User user = this;
		Data.getInstance().set(user,
				user.getUUID() + ".OfflineVotes." + voteSite.getSiteName(),
				amount);
	}

	/**
	 * Sets the user's ingame name
	 *
	 * @param playerName
	 *            Player name
	 */
	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	/**
	 * Set whether or not the user has been reminded to vote
	 *
	 * @param reminded
	 *            boolean
	 */
	public void setReminded(boolean reminded) {
		User user = this;
		Data.getInstance().set(user, user.getUUID() + ".Reminded", reminded);
	}

	/**
	 * Set time of last vote for VoteSite
	 *
	 * @param siteName
	 */
	public void setTime(VoteSite voteSite) {
		User user = this;
		Data.getInstance().setTime(voteSite.getSiteName(), user);
	}

	/**
	 * Set time of bonus reward
	 */
	public void setTimeBonus() {
		User user = this;
		Data.getInstance().setTimeAll(user);
	}

	/**
	 * Set total for VoteSite for user
	 *
	 * @param voteSite
	 *            VoteSite to set total
	 * @param amount
	 *            Total to set
	 */
	public void setTotal(VoteSite voteSite, int amount) {
		User user = this;
		Data.getInstance().set(user,
				user.getUUID() + ".Total." + voteSite.getSiteName(), amount);
	}

	/**
	 * Set user's uuid
	 *
	 * @param uuid
	 *            uuid to set to
	 */
	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	public void topVoterAward(int place) {
		if (playerName == null) {
			playerName = Utils.getInstance().getPlayerName(uuid);
		}
		if (Utils.getInstance().isPlayerOnline(playerName)) {
			// online
			giveTopVoterAward(place);
		} else {
			Data.getInstance().setTopVoterAwardOffline(this, place);
		}

	}

}
