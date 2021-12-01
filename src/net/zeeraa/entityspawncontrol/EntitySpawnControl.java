package net.zeeraa.entityspawncontrol;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EntitySpawnControl extends JavaPlugin implements Listener {
	private List<EntityType> blockedTypes;
	private boolean debug;

	@Override
	public void onLoad() {
		blockedTypes = new ArrayList<>();
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();

		debug = getConfig().getBoolean("debug");

		if (debug) {
			getLogger().info("Debug mode enabled");
		}

		if (!this.loadBlockedEntitiesConfig(true, false)) {
			getLogger().warning("Error: Invalid configuration. Please backup your config and create a new one");
			Bukkit.getServer().getPluginManager().disablePlugin(this);
		}

		Bukkit.getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		blockedTypes.clear();
	}

	/**
	 * Get a list of blocked {@link EntityType}s. The returned list can be modified to change this plugins behavior without restarting the server
	 * 
	 * @return {@link List} with blocked {@link EntityType}s
	 */
	public List<EntityType> getBlockedTypes() {
		return blockedTypes;
	}

	/**
	 * Check if the plugin is in debug mode
	 * 
	 * @return <code>true</code> if debug mode is enabled
	 */
	public boolean isDebug() {
		return debug;
	}

	/**
	 * Enable or disable debug mode depending on the provided value
	 * 
	 * @param debug <code>true</code> to enable debug mode
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Clear blocked types and load them from config.yml
	 * 
	 * @param saveOnUpdate Set to <code>true</code> to save the config if new
	 *                     entities was added
	 * @param quiet        Set to <code>true</code> to disable warnings on failure
	 * 
	 * @return <code>true</code> on success
	 */
	public boolean loadBlockedEntitiesConfig(boolean saveOnUpdate, boolean quiet) {
		this.reloadConfig();
		blockedTypes.clear();

		boolean changed = false;

		ConfigurationSection blocked = getConfig().getConfigurationSection("blocked-entities");
		if (blocked == null) {
			if (!quiet) {
				getLogger().warning("Missing configuration section: blocked-entities");
			}
			return false;
		}

		for (EntityType et : EntityType.values()) {
			if (!blocked.isBoolean(et.name())) {
				blocked.set(et.name(), false);
				changed = true;
			} else if (blocked.getBoolean(et.name())) {
				blockedTypes.add(et);
			}
		}

		for (String key : blocked.getKeys(false)) {
			if (blocked.isBoolean(key)) {
				boolean isBlocked = blocked.getBoolean(key);
				try {
					EntityType type = EntityType.valueOf(key);

					if (isBlocked) {
						blockedTypes.add(type);
					}
				} catch (IllegalArgumentException e) {
					if (!quiet) {
						getLogger().warning(key + " is not a valid EntityType, it will be ignored");
					}
				}
			} else {
				if (!quiet) {
					getLogger().warning(key + " has a non boolean value, it will be ignored");
				}
			}

		}

		if (changed) {
			if (saveOnUpdate) {
				getConfig().set("blocked-entities", blocked);
				saveConfig();

				if (!quiet) {
					getLogger().info("Configuration changed");
				}
			}
		}

		return true;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntitySpawn(EntitySpawnEvent e) {
		if (blockedTypes.contains(e.getEntityType())) {
			e.setCancelled(true);

			if (debug) {
				getLogger().info("Prevented " + e.getEntityType().name() + " from spawning in world " + e.getLocation().getWorld().getName() + " at " + e.getLocation().getBlockX() + " " + e.getLocation().getBlockY() + " " + e.getLocation().getBlockZ());
			}
		}
	}
}