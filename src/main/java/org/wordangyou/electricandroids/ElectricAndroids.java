package org.wordangyou.electricandroids;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.wordangyou.electricandroids.setup.ItemSetup;

/**
 * 电力机器人附属主类。
 */
public final class ElectricAndroids extends JavaPlugin implements SlimefunAddon {

    private static ElectricAndroids instance;

    private int energyCapacity;
    private int energyPerOperation;
    private double fastSpeed;
    private double normalSpeed;
    private double slowSpeed;
    private String defaultHybridGear;
    private String defaultPureElectricMode;
    private final Map<String, Integer> interfaceChargeItems = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadSettings();

        ItemSetup.setup(this);

        getLogger().info("电力机器人已加载完毕!");
    }

    private void loadSettings() {
        energyCapacity = Math.max(1, getConfig().getInt("energy.capacity", 512));
        energyPerOperation = Math.max(1, getConfig().getInt("energy.consumption-per-operation", 8));

        interfaceChargeItems.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("energy.interface-charging");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                int value = section.getInt(key);

                if (value > 0) {
                    interfaceChargeItems.put(key, value);
                }
            }
        }

        fastSpeed = clampSpeed(getConfig().getDouble("speed.fast", 2.0));
        normalSpeed = clampSpeed(getConfig().getDouble("speed.normal", 1.0));
        slowSpeed = clampSpeed(getConfig().getDouble("speed.slow", 0.5));

        String hybridGear = getConfig().getString("defaults.hybrid-gear", "MIXED");
        defaultHybridGear = switch (hybridGear == null ? "" : hybridGear.toUpperCase(Locale.ROOT)) {
            case "PURE_ELECTRIC" -> "PURE_ELECTRIC";
            case "FUEL" -> "FUEL";
            default -> "MIXED";
        };

        String pureMode = getConfig().getString("defaults.pure-electric-mode", "AUTO");
        defaultPureElectricMode = switch (pureMode == null ? "" : pureMode.toUpperCase(Locale.ROOT)) {
            case "FAST" -> "FAST";
            case "ECO" -> "ECO";
            default -> "AUTO";
        };
    }

    private static double clampSpeed(double value) {
        double clamped = Math.round(value * 100.0) / 100.0;

        if (clamped < 0.1) {
            return 0.1;
        }
        if (clamped > 3.0) {
            return 3.0;
        }
        return clamped;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return null;
    }

    public static ElectricAndroids getInstance() {
        return instance;
    }

    public int getEnergyCapacity() {
        return energyCapacity;
    }

    public int getEnergyPerOperation() {
        return energyPerOperation;
    }

    public double getFastSpeed() {
        return fastSpeed;
    }

    public double getNormalSpeed() {
        return normalSpeed;
    }

    public double getSlowSpeed() {
        return slowSpeed;
    }

    public String getDefaultHybridGear() {
        return defaultHybridGear;
    }

    public String getDefaultPureElectricMode() {
        return defaultPureElectricMode;
    }

    public Map<String, Integer> getInterfaceChargeItems() {
        return interfaceChargeItems;
    }
}