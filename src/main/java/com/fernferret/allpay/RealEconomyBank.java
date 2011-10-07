package com.fernferret.allpay;

import org.bukkit.entity.Player;

import fr.crafter.tickleman.RealEconomy.RealEconomy;

public class RealEconomyBank extends GenericBank {
	private RealEconomy plugin;
	
	public RealEconomyBank(RealEconomy plugin) {
		this.plugin = plugin;
	}
	
	@Override
	protected String getFormattedMoneyAmount(Player player, double amount) {
		// NOTE: This plugin does not support currency plurals
	    return this.formatCurrency(amount, this.plugin.getCurrency(), null);
	}
	
	@Override
	protected boolean hasMoney(Player player, double money, String message) {
		boolean result = this.plugin.getBalance(player.getName()) >= money;
		if (!result) {
			userIsTooPoor(player, -1, message);
		}
		return result;
	}
	
	@Override
	protected void payMoney(Player player, double amount) {
		double totalmoney = this.plugin.getBalance(player.getName());
		this.plugin.setBalance(player.getName(), totalmoney - amount);
		showReceipt(player, amount, -1);
	}
	
	@Override
	public String getEconUsed() {
		return "RealEconomy";
	}

    @Override
    protected double getMoneyBalance(Player p) {
        return this.plugin.getBalance(p.getName());
    }

    @Override
    protected void giveMoney(Player player, double amount) {
        double totalmoney = this.plugin.getBalance(player.getName());
        this.plugin.setBalance(player.getName(), totalmoney + amount);
        showReceipt(player, (amount * -1), -1);
    }
}
