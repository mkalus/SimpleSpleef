package com.fernferret.allpay;

import java.util.HashMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class GenericBank {
    private String prefix;

    /**
     * Check to ensure the player has enough items for a transaction.
     * 
     * @param player Check this player's item in hand.
     * @param amount How many items should we see if they have?
     * @param type A valid item id. This will check to see if they have the item(s) in their hand.
     * @return true if they have enough items false if not.
     */
    protected final boolean hasItem(Player player, double amount, int type, String message) {
        // TODO: Make this inventory
        boolean hasEnough = player.getInventory().contains(type, (int)amount);
        if (!hasEnough) {
            userIsTooPoor(player, type, message);
        }
        return hasEnough;
    }

    /**
     * Check to ensure the player has enough money
     * 
     * @param player Check this player's bank/pocket for money.
     * @param money How much money should we see if they have?
     * @param message The error message to display after the string. NULL will be passed if one is not required.
     * @return true if they have enough, false if not
     */
    protected abstract boolean hasMoney(Player player, double money, String message);

    /**
     * Check to ensure the player has enough money or items. This method is intended if you want to accept items or money
     * 
     * @param player Check this player's bank/currently held item for money/items.
     * @param amount How much money or how many items should we see if they have?
     * @param type -1 for money, any other valid item id for items. This will check to see if they have the items in their hand.
     * @param message The error message to display after the string. NULL should be passed if one is not required.
     * @return true if they have enough money/items false if not.
     */
    public final boolean hasEnough(Player player, double amount, int type, String message) {
        if (amount == 0) {
            return true;
        }
        if (type == -1) {
            return hasMoney(player, amount, message);
        } else {
            return hasItem(player, amount, type, message);
        }
    }

    /**
     * Convenience method that does not require a message
     * 
     * @param player Check this player's bank/currently held item for money/items.
     * @param amount How much money or how many items should we see if they have?
     * @param type -1 for money, any other valid item id for items. This will check to see if they have the items in their hand.
     * @return true if they have enough money/items false if not.
     */
    public final boolean hasEnough(Player player, double amount, int type) {
        return hasEnough(player, amount, type, null);
    }

    protected final void payItem(Player player, double amount, int type) {
        int removed = 0;
        HashMap<Integer, ItemStack> items = (HashMap<Integer, ItemStack>) player.getInventory().all(type);
        for (int i : items.keySet()) {
            if (removed >= amount) {
                break;
            }
            int diff = (int) (amount - removed);
            int amt = player.getInventory().getItem(i).getAmount();
            if (amt - diff > 0) {
                player.getInventory().getItem(i).setAmount(amt - diff);
                break;
            } else {
                removed += amt;
                player.getInventory().clear(i);
            }
        }
        showReceipt(player, amount, type);
    }

    protected abstract void payMoney(Player player, double amount);

    /**
     * Take the required items/money from the player.
     *
     * @param player The player to take from
     * @param amount How much should we take
     * @param type What should we take? (-1 for money, item id for item)
     */
    public final void pay(Player player, double amount, int type) {
        if (type == -1) {
            payMoney(player, amount);
        } else {
            payItem(player, amount, type);
        }
    }

    /**
     * Give the specified items/money to the player
     *
     * @param player the player to add to
     * @param amount the amount to give
     * @param type the type of currency, -1 for money, itemID otherwise
     */
    public final void give(Player player, double amount, int type) {
        if (type == -1) {
            giveMoney(player, amount);
        } else {
            giveItem(player, amount, type);
        }
    }

    protected abstract void giveMoney(Player player, double amount);

    protected final void giveItem(Player player, double amount, int type) {
        ItemStack item = new ItemStack(type, (int) amount);
        player.getInventory().addItem(item);
        showReceipt(player, (amount * -1), type);
    }

    /**
     * Returns a formatted string of the given amount and type. If type is -1, will return a bank specific string like: "5 Dollars" If type is != -1 will return an item string like: "1 Diamond"
     * 
     * @param amount The number of money/items
     * @param type Money(-1) or item
     * @return A formatted string of the given amount and type
     */
    public final String getFormattedItemAmount(double amount, int type) {
        // If we're here, we have to assume item, this method should only get called from it's children
        Material m = Material.getMaterial(type);
        if (m != null) {
            return amount + " " + m.toString();
        }
        return "NO ITEM FOUND";
    }

    /**
     * This method is provided to enable compatability with plugins that force us to have a player to determine the currency By default it just calles getFormattedMoneyAmount
     * 
     * @param amount The amount to format
     * @param player The player whom we are talking about
     * @return A formatted amount, like $5
     */
    protected abstract String getFormattedMoneyAmount(Player player, double amount);

    public final String getFormattedAmount(Player player, double price, int item) {
        if (item == -1) {
            return getFormattedMoneyAmount(player, price);
        }
        return getFormattedItemAmount(price, item);

    }

    /**
     * This method is called if a user does not have enough money or items. The message parameter allows you to customize what the user does not have enough money for. The format follows: "Sorry but you do not have the required [funds|items] {message}" You can't touch this. My my my...
     * 
     * @param player
     * @param item
     * @param message This message will appear after the sentence that follows: "{prefix}Sorry but you do not have the required [funds|items] {message}"
     */
    protected final void userIsTooPoor(Player player, int item, String message) {
        String type = (item == -1) ? "funds" : "items";
        if (message == null) {
        	// Max Kalus addition/change:
        	if (message == null) return;
        	// End Addition
            //message = "";
        } else {
            message = " " + message;
        }
        player.sendMessage(ChatColor.DARK_RED + this.prefix + ChatColor.WHITE + "Sorry but you do not have the required " + type + message);
    }

    /**
     * Prints a receipt to the user, this should only be called if the econ plugin does not already output when money is taken (Essentials does this) Yo, I told ya, Can't touch this.
     * 
     * @param player The player to send the receipt to
     * @param price The price the user was charged for a wolf
     * @param item The item the user was charged for a wolf (-1 is money)
     */
    protected void showReceipt(Player player, double price, int item) {
        if (price > 0) {
            player.sendMessage(ChatColor.DARK_GREEN + this.prefix + ChatColor.WHITE + "You have been charged " + ChatColor.GREEN + getFormattedAmount(player, price, item));
        } else if (price < 0) {
            player.sendMessage(ChatColor.DARK_GREEN + this.prefix + getFormattedAmount(player, (price * -1), item) + ChatColor.WHITE + " has been added to your account.");
        }
    }

    protected void showError(Player player, String message) {
        player.sendMessage(ChatColor.DARK_RED + this.prefix + ChatColor.WHITE + message);
    }

    /**
     * Simply returns the economy being used.
     * 
     * @return The economy plugin used
     */
    public abstract String getEconUsed();

    public final void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    protected final String formatCurrency(double amount, String currencySingular, String currencyPlural) {
        boolean inFront = false;
        // This determines (by lenght at the moment) if we should put the name before or after the amount.
        if (currencySingular != null && currencySingular.length() == 1 && currencySingular.matches("[a-zA-Z]")) {
            inFront = true;
        }
        // If ther was no plural
        // or the amount is 1 (we'll always display the singular)
        // or if we should display in front (If we're displaying in front, the plural will never matter, for example: $ always goes in front)
        if (currencyPlural == null || amount == 1 || inFront) {
            return inFront ? currencySingular + amount : amount + " " + currencySingular;
        } else {
            return amount + " " + currencyPlural;
        }
    }
    
    public double getBalance(Player p, int itemId) {
        if(itemId == -1) {
            return getMoneyBalance(p);
        }
        return getItemAnount(p, itemId);
    }
    protected abstract double getMoneyBalance(Player p);

    protected final int getItemAnount(Player player, int type) {
        // TODO: Make this inventory
        ItemStack item = player.getItemInHand();
        if(item.getTypeId() != type) {
            return 0;
        }
        return item.getAmount();
    }
}
