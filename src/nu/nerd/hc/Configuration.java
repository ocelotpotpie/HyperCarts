package nu.nerd.hc;

import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

// ----------------------------------------------------------------------------
/**
 * Loads and saves the configuration file.
 */
public class Configuration {
    /**
     * If true, log the configuration on load.
     */
    public boolean DEBUG_LOG_CONFIGURATION;

    /**
     * If true, debug messages also go to the server log.
     */
    public boolean DEBUG_TO_LOG;

    /**
     * If non-zero, send debug messages to players with the hypercarts.debug
     * permission. Higher numbers send more information.
     */
    public int DEBUG_LEVEL;

    /**
     * Maximum speed of carts, loaded from the configuration.
     *
     * The vanilla default is 0.4.
     */
    public double MAX_SPEED;

    /**
     * The number of ticks a cart will be slowed down after encountering a ramp
     * or curve that requires that, as loaded from the configuration.
     */
    public int SLOW_DOWN_TICKS;

    // ------------------------------------------------------------------------
    /**
     * Reload the configuration.
     */
    public void reload() {
        HyperCarts.PLUGIN.reloadConfig();
        Logger logger = HyperCarts.PLUGIN.getLogger();
        FileConfiguration config = HyperCarts.PLUGIN.getConfig();

        DEBUG_LOG_CONFIGURATION = config.getBoolean("debug.log-configuration");
        DEBUG_TO_LOG = config.getBoolean("debug.to-log");
        DEBUG_LEVEL = config.getInt("debug.level");
        MAX_SPEED = config.getDouble("max-speed");
        SLOW_DOWN_TICKS = config.getInt("slow-down-ticks");

        if (DEBUG_LOG_CONFIGURATION) {
            logger.info("Configuration:");
            logger.info("debug.to-log: " + DEBUG_TO_LOG);
            logger.info("debug.level: " + DEBUG_LEVEL);
            logger.info("max-speed: " + MAX_SPEED);
            logger.info("slow-down-ticks: " + SLOW_DOWN_TICKS);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save the configuration.
     */
    public void save() {
        // Only one setting is mutable after load.
        HyperCarts.PLUGIN.getConfig().set("max-speed", MAX_SPEED);
        HyperCarts.PLUGIN.saveConfig();
    }
} // class Configuration