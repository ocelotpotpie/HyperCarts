package nu.nerd.hc;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

// ----------------------------------------------------------------------------
/**
 * Per-player state, created on join and removed when the player leaves.
 */
public class PlayerState {
    /**
     * Constructor.
     *
     * @param player the player.
     * @param config the configuration from which player preferences are loaded.
     */
    public PlayerState(Player player, YamlConfiguration config) {
        _player = player;
        load(config);

    }

    // ------------------------------------------------------------------------
    /**
     * Save this player's preferences to the specified configuration.
     *
     * @param config the configuration to update.
     */
    public void save(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(_player.getUniqueId().toString());
        section.set("name", _player.getName());
        section.set("max-speed", getMaxCartSpeed());
    }

    // ------------------------------------------------------------------------
    /**
     * Load the Player's preferences from the specified configuration
     *
     * @param config the configuration from which player preferences are loaded.
     */
    protected void load(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(_player.getUniqueId().toString());
        if (section == null) {
            section = config.createSection(_player.getUniqueId().toString());
        }
        _maxCartSpeed = section.getDouble("max-speed", HyperCarts.PLUGIN.getMaxCartSpeed());
    }

    // ------------------------------------------------------------------------
    /**
     * Set the player's maximum cart speed.
     *
     * The speed is constrained to be no more than the server-wide maximum and
     * no less than the lesser of the vanilla default and the server-wide
     * maximum.
     *
     * @param bpt player's maximum cart speed in blocks per tick.
     */
    public void setMaxCartSpeed(double bpt) {
        double min = Math.min(HyperCarts.VANILLA_MAX_SPEED, HyperCarts.PLUGIN.getMaxCartSpeed());
        _maxCartSpeed = Math.min(Math.max(bpt, min), HyperCarts.PLUGIN.getMaxCartSpeed());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the player's maximum cart speed in blocks per tick.
     *
     * That value is limited to the server-wide maximum cart speed, even if that
     * changes.
     *
     * @return the player's maximum cart speed in blocks per tick.
     */
    public double getMaxCartSpeed() {
        return Math.min(_maxCartSpeed, HyperCarts.PLUGIN.getMaxCartSpeed());
    }

    // ------------------------------------------------------------------------
    /**
     * The Player.
     */
    protected Player _player;

    /**
     * Maximum cart speed.
     *
     * The speed is constrained to be no more than the server-wide maximum and
     * no less than the lesser of the vanilla default and the server-wide
     * maximum.
     */
    protected double _maxCartSpeed;
} // class PlayerState