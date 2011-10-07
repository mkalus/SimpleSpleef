package com.fernferret.allpay;

import org.bukkit.entity.Player;

/**
 * Special bank class that handles items. If any money (item id = -1) comes in here, it will always return true and never take away This class should be the default bank
 * 
 * @author Eric Stokes
 * 
 */
public class ItemBank extends GenericBank {
	
	@Override
	protected String getFormattedMoneyAmount(Player player, double amount) {
		return "";
	}
	
	@Override
	protected boolean hasMoney(Player player, double money, String message) {
		// The player always has enough money in this bank
		// someone needs to configure a bank differently if they're getting here...
		return true;
	}
	
	@Override
	protected void payMoney(Player player, double amount) {
		// No need to take anything away here, someone needs to configure a bank differently if they're getting here...
	}
	
	/**
	 * For this bank type we only want to show a receipt if there was an item involved
	 */
	@Override
	public void showReceipt(Player player, double price, int item) {
		if (item != -1) {
			super.showReceipt(player, price, item);
		}
	}
	
	@Override
	public String getEconUsed() {
		return "Simple Item Economy";
	}

    @Override
    protected double getMoneyBalance(Player p) {
        // Should never get here...
        return 0;
    }

    @Override
    public void giveMoney(Player player, double amount) {
        // Should never get here...
    }

}
