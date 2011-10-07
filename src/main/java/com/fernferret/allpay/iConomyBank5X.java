package com.fernferret.allpay;

import org.bukkit.entity.Player;

import com.iConomy.iConomy;

/**
 * Adapter class for iConomy 5
 * 
 * @author Eric Stokes
 */
public class iConomyBank5X extends GenericBank {

    @Override
    public String getEconUsed() {
        return "iConomy 5";
    }

    protected boolean hasMoney(Player player, double money, String message) {
        boolean result = iConomy.getAccount(player.getName()).getHoldings().hasEnough(money);
        if (!result) {
            userIsTooPoor(player, -1, message);
        }
        return result;
    }

    @Override
    protected void payMoney(Player player, double amount) {
        iConomy.getAccount(player.getName()).getHoldings().subtract(amount);
        showReceipt(player, amount, -1);
    }

    @Override
    protected String getFormattedMoneyAmount(Player player, double amount) {
        return iConomy.format(amount);
    }

    @Override
    protected double getMoneyBalance(Player p) {
        return iConomy.getAccount(p.getName()).getHoldings().balance();
    }

    @Override
    protected void giveMoney(Player player, double amount) {
        iConomy.getAccount(player.getName()).getHoldings().add(amount);
        showReceipt(player, (amount * -1), -1);
    }
}
