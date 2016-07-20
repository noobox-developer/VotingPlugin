package com.Ben12345rocks.VotingPlugin.Signs;

import java.util.ArrayList;

import org.bukkit.Location;

import com.Ben12345rocks.VotingPlugin.Main;
import com.Ben12345rocks.VotingPlugin.Config.ConfigFormat;
import com.Ben12345rocks.VotingPlugin.Data.ServerData;
import com.Ben12345rocks.VotingPlugin.Objects.SignHandler;

public class Signs {

	static ConfigFormat format = ConfigFormat.getInstance();

	static Signs instance = new Signs();

	static Main plugin = Main.plugin;

	public static Signs getInstance() {
		return instance;
	}

	private Signs() {
	}

	public Signs(Main plugin) {
		Signs.plugin = plugin;
	}

	public String getSignFromLocation(Location loc) {
		for (String sign : ServerData.getInstance().getSigns()) {
			if (ServerData.getInstance().getSignLocation(sign).equals(loc)) {
				return sign;
			}
		}

		return null;
	}

	public void loadSigns() {
		plugin.signs = new ArrayList<SignHandler>();
		for (String sign : ServerData.getInstance().getSigns()) {
			// plugin.getLogger().info("Loading sign " + sign);
			plugin.signs.add(new SignHandler(sign, ServerData.getInstance()
					.getSignLocation(sign), ServerData.getInstance()
					.getSignData(sign), ServerData.getInstance()
					.getSignPosition(sign)));
		}
	}

	public void storeSigns() {
		for (SignHandler sign : plugin.signs) {
			sign.storeSign();
		}
	}

	public void updateSigns() {
		for (int i = plugin.signs.size() - 1; i >= 0; i--) {
			if (!plugin.signs.get(i).isValid()) {
				plugin.signs.get(i).removeSign();
				plugin.signs.remove(i);
				plugin.debug("Sign " + plugin.signs.get(i).getSign()
						+ " invalid, removing from data.");
			} else {
				plugin.signs.get(i).updateLines();
				plugin.signs.get(i).updateSign(i * 3);
			}
		}

		plugin.debug("Signs updated");
	}

}