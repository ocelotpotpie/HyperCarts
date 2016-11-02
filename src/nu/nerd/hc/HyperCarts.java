package nu.nerd.hc;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
 *
 * Players can specify their own maximum cart speed in the range between the
 * vanilla value and the server-wide maximum, but this will only be applied to
 * carts ridden by players, not empty carts or chest carts, for instance. It is
 * too onerous to track who placed what cart across restarts.
 */
public class HyperCarts extends JavaPlugin implements Listener {
    /**
     * This plugin as a singleton.
     */
    public static HyperCarts PLUGIN;

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;

        saveDefaultConfig();
        _maxSpeed = getConfig().getDouble("max-speed");

        File playersFile = new File(getDataFolder(), PLAYERS_FILE);
        _playerConfig = YamlConfiguration.loadConfiguration(playersFile);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        for (PlayerState state : _state.values()) {
            state.save(_playerConfig);
        }

        try {
            _playerConfig.save(new File(getDataFolder(), PLAYERS_FILE));
        } catch (IOException ex) {
            getLogger().warning("Unable to save player data: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hypercarts")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "The current server-wide maximum cart speed is " + getMaxCartSpeed() +
                                   " blocks per tick (the vanilla default is " + VANILLA_MAX_SPEED + ").");
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    return false;
                } else {
                    try {
                        _maxSpeed = Math.max(0.0, Double.parseDouble(args[0]));
                        sender.sendMessage(ChatColor.GOLD + "The server-wide maximum cart speed was set to " + getMaxCartSpeed() +
                                           " blocks per tick (the vanilla default is " + VANILLA_MAX_SPEED + ").");
                        getConfig().set("max-speed", getMaxCartSpeed());
                        saveConfig();
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "You must specify a floating point number for the new speed.");
                    }
                    return true;
                }
            } else {
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("cart-speed")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You must be in-game to use this command.");
                return true;
            }

            PlayerState state = getState((Player) sender);
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "Your current maximum cart speed is " + state.getMaxCartSpeed() + " blocks per tick.");
                sender.sendMessage(ChatColor.GOLD + "The vanilla default is " + VANILLA_MAX_SPEED +
                                   " and the server-wide limit is " + getMaxCartSpeed() + ".");
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    return false;
                } else {
                    try {
                        state.setMaxCartSpeed(Double.parseDouble(args[0]));
                        sender.sendMessage(ChatColor.GOLD + "Your maximum cart speed was set to " + state.getMaxCartSpeed() + " blocks per tick.");
                        sender.sendMessage(ChatColor.GOLD + "The vanilla default is " + VANILLA_MAX_SPEED +
                                           " and the server-wide limit is " + getMaxCartSpeed() + ".");
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
            cart.setMaxSpeed(getMaxCartSpeed());
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
            if (isRail(toBlock)) {
                if (shouldTakeSlow(toBlock)) {
                    if (cart.getMaxSpeed() > VANILLA_MAX_SPEED) {
                        Vector oldVelocity = _lastVelocity.get(cart.getEntityId());
                        if (oldVelocity != null) {
                            cart.setVelocity(oldVelocity);
                        }
                        cart.setMaxSpeed(VANILLA_MAX_SPEED);
                    }
                } else {
                    cart.setMaxSpeed((cart.getPassenger() instanceof Player) ? getState((Player) cart.getPassenger()).getMaxCartSpeed()
                                                                             : getMaxCartSpeed());
                }
            }
            _lastVelocity.put(cart.getEntityId(), cart.getVelocity());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * On join, allocate each player a {@link PlayerState} instance.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        _state.put(player.getName(), new PlayerState(player, _playerConfig));
    }

    // ------------------------------------------------------------------------
    /**
     * On quit, forget the {@link PlayerState}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerState state = _state.remove(event.getPlayer().getName());
        state.save(_playerConfig);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the server-wide maximum minecart speed.
     *
     * @return the server-wide maximum minecart speed.
     */
    public double getMaxCartSpeed() {
        return _maxSpeed;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the state corresponding to the specified Player.
     *
     * @param player the Player.
     * @return the PlayerState.
     */
    public PlayerState getState(Player player) {
        return _state.get(player.getName());
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
     * Return true if a cart should follow the rails in the specified block at
     * vanilla speed, to avoid problems.
     *
     * The particular rail types for which a cart should slow down are:
     * <ul>
     * <li>Regular, detector or activator rail ramps, data values 2 through
     * 5,</li>
     * <li>Curved regular rails, data values 6 through 9,</li>
     * <li>Powered rail ramps, data values 10 through 13.</li>
     * </ul>
     *
     * @param b the block to check.
     * @return true if a cart should follow the rails in the specified block at
     *         vanilla speed.
     */
    private boolean shouldTakeSlow(Block b) {
        return (b.getType() == Material.RAILS && (b.getData() >= 2 && b.getData() <= 9))
               ||
               (b.getType() == Material.DETECTOR_RAIL || b.getType() == Material.ACTIVATOR_RAIL) &&
                  (b.getData() >= 2 && b.getData() <= 5)
               ||
               (b.getType() == Material.POWERED_RAIL && (b.getData() >= 10 && b.getData() <= 13));
    }

    // ------------------------------------------------------------------------
    /**
     * Vanilla default max speed (blocks/tick).
     */
    public static final double VANILLA_MAX_SPEED = 0.4;

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

    /**
     * Name of players file.
     */
    private static final String PLAYERS_FILE = "players.yml";

    /**
     * Configuration file for per-player settings.
     */
    protected YamlConfiguration _playerConfig;

    /**
     * Map from Player name to {@link PlayerState} instance.
     *
     * A Player's PlayerState exists only for the duration of a login.
     */
    protected HashMap<String, PlayerState> _state = new HashMap<String, PlayerState>();

} // class HyperCarts