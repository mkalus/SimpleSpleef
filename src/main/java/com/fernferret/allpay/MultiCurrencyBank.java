package com.fernferret.allpay;

import me.ashtheking.currency.CurrencyList;

import org.bukkit.entity.Player;

/**
 * Adapter class for MultiCurrency
 * 
 * @author Eric Stokes
 */
public class MultiCurrencyBank extends GenericBank {

    @Override
    public String getEconUsed() {
        return "MultiCurrency";
    }

    protected boolean hasMoney(Player player, double money, String message) {
        boolean result = CurrencyList.hasEnough(player.getName(), money);
        if (!result) {
            userIsTooPoor(player, -1, message);
        }
        return result;
    }

    @Override
    protected void payMoney(Player player, double amount) {
        CurrencyList.subtract(player.getName(), amount);
        showReceipt(player, amount, -1);
    }

    @Override
    protected String getFormattedMoneyAmount(Player player, double amount) {
        return this.formatCurrency(amount, ((String) CurrencyList.maxCurrency(player.getName())[0]), null);
    }

    @Override
    protected double getMoneyBalance(Player p) {
        return CurrencyList.getValue(((String) CurrencyList.maxCurrency(p.getName())[0]), p.getName());
    }

    @Override
    protected void giveMoney(Player player, double amount) {
        CurrencyList.add(player.getName(), amount);
        showReceipt(player, (amount * -1), -1);
    }

}
