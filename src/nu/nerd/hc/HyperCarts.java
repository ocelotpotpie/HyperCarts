package nu.nerd.hc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.plugin.java.JavaPlugin;

// ----------------------------------------------------------------------------
/**
 * Main plugin class.
 */
public class HyperCarts extends JavaPlugin implements Listener {
    /**
     * Maximum speed of carts.
     * 
     * The vanilla default is 0.4.
     */
    private double _maxSpeed;

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        _maxSpeed = getConfig().getDouble("max-speed");

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cart-speed")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "The current maximum cart speed is " + _maxSpeed + " (the vanilla default is 0.4).");
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    return false;
                } else {
                    try {
                        _maxSpeed = Math.max(0.0, Double.parseDouble(args[0]));
                        sender.sendMessage(ChatColor.GOLD + "Maximum cart speed set to " + _maxSpeed + " (the vanilla default is 0.4).");
                        getConfig().set("max-speed", _maxSpeed);
                        saveConfig();
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "You must specify a floating point number for the new speed.");
                    }
                    return true;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * When a cart is created, set its max speed.
     */
    @EventHandler(ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        if (event.getVehicle() instanceof Minecart) {
            Minecart cart = (Minecart) event.getVehicle();
            cart.setMaxSpeed(_maxSpeed);
        }
    }
} // class HyperCarts