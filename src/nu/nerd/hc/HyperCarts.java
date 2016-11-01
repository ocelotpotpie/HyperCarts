package nu.nerd.hc;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

// ----------------------------------------------------------------------------
/**
 * Main plugin class.
 *
 * Testing revealed a problem with carts configured with an above-vanilla
 * maximum speed: such a cart encountering a powered rail ramp or regular rail
 * ramp would stop and roll back the other way, even though its speed was more
 * than adequate to climb the slope. The cart gets its speed set to 0.0 on the
 * tick when it moves from the flat to the ramp.
 *
 * The work-around implemented in this plugin will be to detect the
 * circumstances where the speed of the cart drops unexpectedly to zero, namely:
 * <ul>
 * <li>The cart is on a ramp. (Excluding un-powered powered rails.)</li>
 * <li>The cart has a non-default maximum speed.</li>
 * </ul>
 * and do two things:
 * <ul>
 * <li>Set the maximum speed back to the default until the ramp is back on the
 * horizontal.</li>
 * <li>Restore the velocity of the cart to what it was in the previous
 * tick.</li>
 * </ul>
 */
public class HyperCarts extends JavaPlugin implements Listener {
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
                sender.sendMessage(ChatColor.GOLD + "The current maximum cart speed is " + _maxSpeed +
                                   " (the vanilla default is " + VANILLA_MAX_SPEED + ").");
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    return false;
                } else {
                    try {
                        _maxSpeed = Math.max(0.0, Double.parseDouble(args[0]));
                        sender.sendMessage(ChatColor.GOLD + "Maximum cart speed set to " + _maxSpeed +
                                           " (the vanilla default is " + VANILLA_MAX_SPEED + ").");
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

    // ------------------------------------------------------------------------
    /**
     * If a minecart is climbing a hill, set its maximum speed back to the
     * default, or it may fail to climb.
     */
    @EventHandler(ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (event.getVehicle() instanceof Minecart) {
            Minecart cart = (Minecart) event.getVehicle();
            Block toBlock = event.getTo().getBlock();
            if (cart.getMaxSpeed() > VANILLA_MAX_SPEED) {
                if (isNonAttenuatingRailRamp(toBlock)) {
                    Vector oldVelocity = _lastVelocity.get(cart.getEntityId());
                    if (oldVelocity != null) {
                        cart.setVelocity(oldVelocity);
                    }
                    cart.setMaxSpeed(VANILLA_MAX_SPEED);
                }
            } else if (isRail(toBlock) && !isNonAttenuatingRailRamp(toBlock)) {
                cart.setMaxSpeed(_maxSpeed);
            }
            _lastVelocity.put(cart.getEntityId(), cart.getVelocity());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the specified block is a type of rail.
     *
     * @param b the block to check.
     * @return true if the specified block is a type of rail.
     */
    private boolean isRail(Block b) {
        return b.getType() == Material.RAILS ||
               b.getType() == Material.POWERED_RAIL ||
               b.getType() == Material.DETECTOR_RAIL ||
               b.getType() == Material.ACTIVATOR_RAIL;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if a block is a rail ramp that does not attenuate a cart's
     * speed, i.e. regular rails or powered rails.
     *
     * @param b the block to check.
     * @return true if the block is inclined active powered rail, or inclined
     *         regular rails.
     */
    private boolean isNonAttenuatingRailRamp(Block b) {
        return ((b.getType() == Material.RAILS ||
                 b.getType() == Material.DETECTOR_RAIL ||
                 b.getType() == Material.ACTIVATOR_RAIL)
                && (b.getData() >= 2 && b.getData() <= 5))
               ||
               (b.getType() == Material.POWERED_RAIL
                && (b.getData() >= 10 && b.getData() <= 13));

    }

    // ------------------------------------------------------------------------
    /**
     * Vanilla default max speed (blocks/tick).
     */
    private static final double VANILLA_MAX_SPEED = 0.4;

    /**
     * Maximum speed of carts.
     *
     * The vanilla default is 0.4.
     */
    private double _maxSpeed;

    /**
     * Map from cart entity ID to its velocity in the previous tick.
     */
    private final HashMap<Integer, Vector> _lastVelocity = new HashMap<Integer, Vector>();
} // class HyperCarts