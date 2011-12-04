package com.fernferret.allpay;

import org.bukkit.entity.Player;

import com.iCo6.iConomy;
import com.iCo6.system.Accounts;

/**
 * Adapter class for iConomy 6
 * 
 * @author Eric Stokes
 */
public class iConomyBank6X extends GenericBank {
    private Accounts accounts;

    public iConomyBank6X() {
        this.accounts = new Accounts();
    }

    @Override
    public String getEconUsed() {
        return "iConomy 6";
    }

    @Override
    protected boolean hasMoney(Player player, double money, String message) {

        boolean result = this.accounts.get(player.getName()).getHoldings().hasEnough(money);
        if (!result) {
            userIsTooPoor(player, -1, message);
        }
        return result;
    }

    @Override
    protected void payMoney(Player player, double amount) {
        this.accounts.get(player.getName()).getHoldings().subtract(amount);
        showReceipt(player, amount, -1);
    }

    @Override
    protected String getFormattedMoneyAmount(Player player, double amount) {
        return iConomy.format(amount);
    }

    @Override
    protected double getMoneyBalance(Player p) {
        return this.accounts.get(p.getName()).getHoldings().getBalance();
    }

    @Override
    protected void giveMoney(Player player, double amount) {
        this.accounts.get(player.getName()).getHoldings().add(amount);
        showReceipt(player, (amount * -1), -1);
    }
}
