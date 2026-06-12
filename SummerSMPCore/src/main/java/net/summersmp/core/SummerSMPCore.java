package net.summersmp.core;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class SummerSMPCore extends JavaPlugin {

    private int maceLimit;
    private int heavyCoresFound;

    private File dataFile;
    private FileConfiguration dataConfig;

    /** Tag used to mark a heavy core / mace that has already been counted toward the limit. */
    public NamespacedKey countedKey;

    private CombatManager combatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        countedKey = new NamespacedKey(this, "counted_heavy_core");
        loadData();

        try {
            Bukkit.removeRecipe(NamespacedKey.minecraft("end_crystal"));
        } catch (Throwable t) {
            getLogger().warning("Could not remove the end crystal recipe: " + t.getMessage());
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new EndCrystalListener(), this);
        getServer().getPluginManager().registerEvents(new MaceLimitListener(this), this);

        combatManager = new CombatManager(this);
        getServer().getPluginManager().registerEvents(combatManager, this);
        combatManager.runTaskTimer(this, 20L, 20L);

        // Commands
        BanCommand banCommand = new BanCommand(this);
        if (getCommand("ban") != null) {
            getCommand("ban").setExecutor(banCommand);
            getCommand("ban").setTabCompleter(banCommand);
        }
        if (getCommand("unban") != null) {
            getCommand("unban").setExecutor(banCommand);
            getCommand("unban").setTabCompleter(banCommand);
        }
        MaceCommand maceCommand = new MaceCommand(this);
        if (getCommand("maces") != null) {
            getCommand("maces").setExecutor(maceCommand);
            getCommand("maces").setTabCompleter(maceCommand);
        }
        RtpCommand rtpCommand = new RtpCommand(this);
        if (getCommand("rtp") != null) {
            getCommand("rtp").setExecutor(rtpCommand);
        }

        getLogger().info("Enabled. Mace limit: " + maceLimit + " | heavy cores found so far: " + heavyCoresFound);
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public void reloadSettings() {
        reloadConfig();
        maceLimit = getConfig().getInt("mace-limit", 3);
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("Could not create data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        heavyCoresFound = dataConfig.getInt("heavy-cores-found", 0);
    }

    private void saveData() {
        if (dataConfig == null || dataFile == null) return;
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    public int getMaceLimit() {
        return maceLimit;
    }

    public void setMaceLimit(int value) {
        maceLimit = Math.max(0, value);
        getConfig().set("mace-limit", maceLimit);
        saveConfig();
    }

    public boolean announceClaims() {
        return getConfig().getBoolean("announce-heavy-core-claims", true);
    }

    public String defaultBanReason() {
        return getConfig().getString("default-ban-reason", "Banned by staff");
    }

    public int getHeavyCoresFound() {
        return heavyCoresFound;
    }

    public void setHeavyCoresFound(int value) {
        heavyCoresFound = Math.max(0, value);
        dataConfig.set("heavy-cores-found", heavyCoresFound);
        saveData();
    }

    public void incrementHeavyCores() {
        setHeavyCoresFound(heavyCoresFound + 1);
    }
}
