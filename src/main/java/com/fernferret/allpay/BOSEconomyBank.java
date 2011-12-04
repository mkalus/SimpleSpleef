package com.fernferret.allpay;

import org.bukkit.entity.Player;

import cosine.boseconomy.BOSEconomy;

public class BOSEconomyBank extends GenericBank {
	private BOSEconomy plugin;
	
	public BOSEconomyBank(BOSEconomy plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public String getFormattedMoneyAmount(Player player, double amount) {
		return this.formatCurrency(amount, this.plugin.getMoneyName(), this.plugin.getMoneyNamePlural());
	}
	
	@Override
	public boolean hasMoney(Player player, double money, String message) {
		boolean result = this.plugin.getPlayerMoney(player.getName()) >= money;
		if (!result) {
			userIsTooPoor(player, -1, message);
		}
		return result;
	}
	
	@Override
	public void payMoney(Player player, double amount) {
		int negativePrice = (int) (-1 * Math.abs(amount));
		this.plugin.addPlayerMoney(player.getName(), negativePrice, true);
		showReceipt(player, amount, -1);
	}
	
	@Override
	public String getEconUsed() {
		return "BOSEconomy";
	}

    @Override
    protected double getMoneyBalance(Player p) {
        return this.plugin.getPlayerMoney(p.getName());
    }

    @Override
    protected void giveMoney(Player player, double amount) {
        this.plugin.addPlayerMoney(player.getName(), (int)amount, true);
        showReceipt(player, (amount * -1), -1);
    }

}
