package org.wordangyou.electricandroids;

import city.norain.slimefun4.api.menu.UniversalMenu;
import city.norain.slimefun4.api.menu.UniversalMenuPreset;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 电力机器人公用逻辑: 挡位变速, 电量消耗, 燃料兜底, 接口充能, 菜单显示。
 */
public final class ElectricAndroidUtils {

    /**
     * 机器人控制面板中的能量显示槽位 (即原版机器人的燃料输入槽)。
     */
    private static final int ENERGY_SLOT = 34;

    /**
     * 原版机器人的燃料输入槽; 混合动力机器人继续用于装载燃料,
     * 纯电动版本则转为电池输入槽 (放入能量物品自动充能)。
     */
    private static final int FUEL_INPUT_SLOT = 43;

    /**
     * 混合动力机器人的电池输入槽 (从原版装饰边框的 42 号槽释放出来)。
     */
    private static final int HYBRID_BATTERY_SLOT = 42;

    /**
     * 每个 tick 周期最多执行的指令条数 (防止额度失控)。
     */
    private static final int MAX_OPS_PER_CYCLE = 3;

    /**
     * 无能源时指令额度被冻结的上限值 (防止恢复供电后瞬间连执行)。
     */
    private static final double BUDGET_FREEZE = 0.5;

    // 纯电动系列速度模式
    public static final String MODE_AUTO = "AUTO";
    public static final String MODE_FAST = "FAST";
    public static final String MODE_ECO = "ECO";

    // 混合动力系列挡位
    public static final String GEAR_PURE_ELECTRIC = "PURE_ELECTRIC";
    public static final String GEAR_MIXED = "MIXED";
    public static final String GEAR_FUEL = "FUEL";

    private static final String KEY_MODE = "speed-mode";
    private static final String KEY_GEAR = "power-gear";
    private static final String KEY_BUDGET = "op-budget";
    private static final String KEY_FUEL = "fuel";

    /**
     * 由机器人条目类提供的 "父类 tick" 回调, 用于执行一条脚本指令。
     */
    @FunctionalInterface
    public interface ParentTick {
        void tick(Block block, SlimefunUniversalData data);
    }

    private ElectricAndroidUtils() {}

    // ==================== 菜单 ====================

    /**
     * 将纯电动机器人面板 34 号槽替换为 "速度模式" 显示, 并支持点击切换;
     * 43 号燃料槽转为电池输入槽, 放入能量物品会自动充能。
     */
    public static void setupPureElectricMenu(SlimefunItem item) {
        setupMenu(item, false);
    }

    /**
     * 将混合动力机器人面板 34 号槽替换为 "动力挡位" 显示, 并支持点击切换;
     * 42 号装饰槽释放为电池输入槽, 与 43 号燃料槽组成 [电池][燃料] 布局。
     */
    public static void setupHybridMenu(SlimefunItem item) {
        setupMenu(item, true);
    }

    private static void setupMenu(SlimefunItem item, boolean hybrid) {
        try {
            UniversalMenuPreset preset = UniversalMenuPreset.getPreset(item.getId());

            if (preset == null) {
                return;
            }

            ElectricAndroids plugin = ElectricAndroids.getInstance();
            String defaultMode = hybrid ? plugin.getDefaultHybridGear() : plugin.getDefaultPureElectricMode();

            preset.addItem(ENERGY_SLOT, createStatusItem(hybrid, 0, plugin.getEnergyCapacity(), defaultMode, 0f, false));
            preset.addMenuClickHandler(ENERGY_SLOT, (p, slot, cursor, action) -> {
                toggleMode(p, hybrid);
                return false;
            });

            if (hybrid) {
                // 将 42 号装饰边框释放为电池输入槽, 与 43 号燃料槽组成 [电池][燃料] 布局
                preset.getPresetSlots().remove(HYBRID_BATTERY_SLOT);
            }
        } catch (Exception x) {
            ElectricAndroids.getInstance().getLogger().warning("无法修改机器人菜单显示: " + x);
        }
    }

    private static void toggleMode(Player p, boolean hybrid) {
        try {
            if (!(p.getOpenInventory().getTopInventory().getHolder() instanceof UniversalMenu menu)) {
                return;
            }

            var data = StorageCacheUtils.getUniversalBlock(menu.getUuid());

            if (data == null) {
                return;
            }

            if (hybrid) {
                String next = switch (resolveHybridGear(data)) {
                    case GEAR_PURE_ELECTRIC -> GEAR_MIXED;
                    case GEAR_MIXED -> GEAR_FUEL;
                    default -> GEAR_PURE_ELECTRIC;
                };
                data.setData(KEY_GEAR, next);
            } else {
                String next = switch (resolvePureMode(data)) {
                    case MODE_AUTO -> MODE_FAST;
                    case MODE_FAST -> MODE_ECO;
                    default -> MODE_AUTO;
                };
                data.setData(KEY_MODE, next);
            }

            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        } catch (Exception ignored) {
            // 点击切换失败时静默处理
        }
    }

    // ==================== 纯电动 tick ====================

    /**
     * 纯电动机器人的每周期逻辑: 按速度模式消耗电量执行指令。
     * 模式: AUTO (电网内高速 / 离网标准), FAST (始终高速), ECO (始终标准)。
     */
    public static void tickPureElectric(
            EnergyNetComponent component, Block b, SlimefunUniversalData data, ParentTick op) {
        absorbBatterySlot(component, b, data, false);

        if (!"false".equals(data.getData("paused"))) {
            return;
        }

        ElectricAndroids plugin = ElectricAndroids.getInstance();
        int cost = plugin.getEnergyPerOperation();

        // 直接查询当前位置是否接入能源网络, 移动后立即生效, 不再依赖电量差值推测
        boolean grid = isGridConnected(b.getLocation());

        double speed = switch (resolvePureMode(data)) {
            case MODE_FAST -> plugin.getFastSpeed();
            case MODE_ECO -> plugin.getNormalSpeed();
            default -> grid ? plugin.getFastSpeed() : plugin.getNormalSpeed();
        };

        double budget = parseDouble(data.getData(KEY_BUDGET), 0) + speed;

        if (budget > MAX_OPS_PER_CYCLE) {
            budget = MAX_OPS_PER_CYCLE;
        }

        int ops = (int) Math.floor(budget);
        budget -= ops;

        Block current = b;

        for (int i = 0; i < ops; i++) {
            long charge = component.getChargeLong(current.getLocation(), data);

            if (charge < cost) {
                budget = Math.min(budget, BUDGET_FREEZE);
                break;
            }

            component.removeCharge(current.getLocation(), cost);
            protectFuel(data);
            op.tick(current, data);

            Block moved = resolveCurrentBlock(data);

            if (moved == null || moved.getType() != Material.PLAYER_HEAD) {
                break;
            }

            if (!moved.getLocation().equals(current.getLocation())) {
                notifyNetworkUpdate(current.getLocation(), moved.getLocation());
            }

            current = moved;
        }

        data.setData(KEY_BUDGET, formatBudget(budget));
    }

    // ==================== 混合动力 tick ====================

    /**
     * 混合动力机器人的每周期逻辑。
     * 挡位: PURE_ELECTRIC (仅耗电, 没电停机) / MIXED (电优先, 燃料兜底) / FUEL (仅燃料)。
     */
    public static void tickHybrid(EnergyNetComponent component, Block b, SlimefunUniversalData data, ParentTick op) {
        absorbBatterySlot(component, b, data, true);

        if (!"false".equals(data.getData("paused"))) {
            return;
        }

        ElectricAndroids plugin = ElectricAndroids.getInstance();
        int cost = plugin.getEnergyPerOperation();
        String gear = resolveHybridGear(data);

        double speed = switch (gear) {
            case GEAR_PURE_ELECTRIC -> plugin.getFastSpeed();
            case GEAR_FUEL -> plugin.getSlowSpeed();
            default -> plugin.getNormalSpeed();
        };

        double budget = parseDouble(data.getData(KEY_BUDGET), 0) + speed;

        if (budget > MAX_OPS_PER_CYCLE) {
            budget = MAX_OPS_PER_CYCLE;
        }

        int ops = (int) Math.floor(budget);
        budget -= ops;

        Block current = b;

        for (int i = 0; i < ops; i++) {
            long charge = component.getChargeLong(current.getLocation(), data);
            boolean powered;

            if (GEAR_PURE_ELECTRIC.equals(gear)) {
                powered = charge >= cost;

                if (powered) {
                    component.removeCharge(current.getLocation(), cost);
                    protectFuel(data);
                }
            } else if (GEAR_FUEL.equals(gear)) {
                powered = hasFuel(data);
            } else {
                // 混合挡: 电优先, 燃料兜底
                if (charge >= cost) {
                    component.removeCharge(current.getLocation(), cost);
                    protectFuel(data);
                    powered = true;
                } else {
                    powered = hasFuel(data);
                }
            }

            if (!powered) {
                budget = Math.min(budget, BUDGET_FREEZE);
                break;
            }

            op.tick(current, data);

            Block moved = resolveCurrentBlock(data);

            if (moved == null || moved.getType() != Material.PLAYER_HEAD) {
                break;
            }

            if (!moved.getLocation().equals(current.getLocation())) {
                notifyNetworkUpdate(current.getLocation(), moved.getLocation());
            }

            current = moved;
        }

        data.setData(KEY_BUDGET, formatBudget(budget));
    }

    /**
     * 机器人移动后, 让能源网络重新扫描新旧位置。
     * <p>
     * 原版机器人移动只迁移数据, 不会触发方块事件, 若不同步更新网络,
     * 电网仍会把机器人记在旧位置上, 导致移动后无法从电网充电。
     */
    private static void notifyNetworkUpdate(Location from, Location to) {
        Slimefun.getNetworkManager().updateAllNetworks(from);
        Slimefun.getNetworkManager().updateAllNetworks(to);
    }

    /**
     * 电模式执行时保护燃料罐: 伪造 +1 让父类照常执行指令, 执行后数值自动还原, 燃料不被消耗。
     */
    private static void protectFuel(SlimefunUniversalData data) {
        float fuel = parseFloat(data.getData(KEY_FUEL), 0f);
        data.setData(KEY_FUEL, String.valueOf(fuel + 1f));
    }

    /**
     * 机器人当前是否存在可用燃料 (燃料罐有余量, 或燃料输入槽有待装载的燃料)。
     */
    private static boolean hasFuel(SlimefunUniversalData data) {
        if (parseFloat(data.getData(KEY_FUEL), 0f) >= 0.001f) {
            return true;
        }

        UniversalMenu menu = data.getMenu();

        if (menu == null) {
            return false;
        }

        ItemStack item = menu.getItemInSlot(FUEL_INPUT_SLOT);
        return item != null && item.getType() != Material.AIR;
    }

    // ==================== 状态显示 ====================

    /**
     * 刷新纯电动机器人面板的速度模式显示。
     */
    public static void updatePureStatus(EnergyNetComponent component, Block b, SlimefunUniversalData data) {
        updateStatus(component, b, data, false);
    }

    /**
     * 刷新混合动力机器人面板的动力挡位显示。
     */
    public static void updateHybridStatus(EnergyNetComponent component, Block b, SlimefunUniversalData data) {
        updateStatus(component, b, data, true);
    }

    private static void updateStatus(EnergyNetComponent component, Block b, SlimefunUniversalData data, boolean hybrid) {
        UniversalMenu menu = data.getMenu();

        if (menu == null) {
            return;
        }

        Block current = resolveCurrentBlock(data);

        if (current == null) {
            current = b;
        }

        if (current.getType() != Material.PLAYER_HEAD) {
            return;
        }

        long charge = component.getChargeLong(current.getLocation(), data);
        long capacity = component.getCapacityLong();
        float fuel = parseFloat(data.getData(KEY_FUEL), 0f);
        boolean grid = isGridConnected(current.getLocation());
        String mode = hybrid ? resolveHybridGear(data) : resolvePureMode(data);

        menu.replaceExistingItem(ENERGY_SLOT, createStatusItem(hybrid, charge, capacity, mode, fuel, grid));
    }

    private static ItemStack createStatusItem(
            boolean hybrid, long charge, long capacity, String mode, float fuel, boolean grid) {
        ItemStack item = HeadTexture.BATTERY.getAsItemStack();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        ElectricAndroids plugin = ElectricAndroids.getInstance();
        List<String> lore = new ArrayList<>();

        if (hybrid) {
            lore.add(mark(GEAR_PURE_ELECTRIC.equals(mode)) + "⚡ 纯电动挡 §8×" + formatSpeed(plugin.getFastSpeed())
                    + " §8(仅耗电)");
            lore.add(mark(GEAR_MIXED.equals(mode)) + "⚡🔥 混合挡 §8×" + formatSpeed(plugin.getNormalSpeed())
                    + " §8(电优先→燃料)");
            lore.add(mark(GEAR_FUEL.equals(mode)) + "🔥 燃料挡 §8×" + formatSpeed(plugin.getSlowSpeed())
                    + " §8(仅燃料)");
            lore.add("");
            lore.add("§7电量: §b" + charge + " §7/ §b" + capacity + " J");
            lore.add("§7燃料储备: §b" + formatFuel(fuel) + " §7次操作");
            lore.add("");
            lore.add("§8\u21E8 §7点击切换动力挡位");
            meta.setDisplayName("§8\u21E9 §b动力挡位 §8\u21E9");
        } else {
            lore.add(mark(MODE_AUTO.equals(mode)) + "⚡ 自动 §8电网×" + formatSpeed(plugin.getFastSpeed())
                    + " / 电池×" + formatSpeed(plugin.getNormalSpeed()));
            lore.add(mark(MODE_FAST.equals(mode)) + "⚡ 全速 §8×" + formatSpeed(plugin.getFastSpeed()));
            lore.add(mark(MODE_ECO.equals(mode)) + "🔋 节能 §8×" + formatSpeed(plugin.getNormalSpeed()));
            lore.add("");
            lore.add(grid ? "§a⚡ 已接入电网" : "§e🔋 电池供电");
            lore.add("§7电量: §b" + charge + " §7/ §b" + capacity + " J");
            lore.add("");
            lore.add("§8\u21E8 §7点击切换速度模式");
            meta.setDisplayName("§8\u21E9 §b速度模式 §8\u21E9");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String mark(boolean current) {
        return current ? "§a\u25B8 §f" : "§8\u25B8 §7";
    }

    // ==================== 接口充能 ====================

    /**
     * 通过 "安卓接口(燃料)" 为机器人充电。
     * 接口内的能量物品会被消耗, 并按配置转化为机器人的电量 (直到充满为止)。
     */
    public static void chargeFromInterface(EnergyNetComponent component, UniversalMenu menu, Block facedBlock) {
        if (facedBlock.getType() != Material.DISPENSER
                || !StorageCacheUtils.isBlock(facedBlock.getLocation(), "ANDROID_INTERFACE_FUEL")) {
            return;
        }

        BlockState state = facedBlock.getState(false);

        if (!(state instanceof Dispenser dispenser)) {
            return;
        }

        Location robot = resolveRobotLocation(menu);

        if (robot == null) {
            return;
        }

        long capacity = component.getCapacityLong();
        long charge = component.getChargeLong(robot);

        if (charge >= capacity) {
            return;
        }

        Map<String, Integer> chargeItems = ElectricAndroids.getInstance().getInterfaceChargeItems();
        Inventory inventory = dispenser.getInventory();
        long gained = 0;

        for (int slot = 0; slot < inventory.getSize() && charge < capacity; slot++) {
            ItemStack item = inventory.getItem(slot);

            if (item == null || item.getType().isAir()) {
                continue;
            }

            for (Map.Entry<String, Integer> entry : chargeItems.entrySet()) {
                SlimefunItem chargeItem = SlimefunItem.getById(entry.getKey());

                if (chargeItem == null || !SlimefunUtils.isItemSimilar(item, chargeItem.getItem(), true)) {
                    continue;
                }

                int amount = item.getAmount();
                int consumed = 0;

                while (consumed < amount && capacity - charge >= entry.getValue()) {
                    consumed++;
                    charge += entry.getValue();
                }

                if (consumed > 0) {
                    if (consumed == amount) {
                        inventory.setItem(slot, null);
                    } else {
                        item.setAmount(amount - consumed);
                        inventory.setItem(slot, item);
                    }

                    gained += (long) consumed * entry.getValue();
                }

                break; // 该槽位已处理, 继续下一槽
            }
        }

        if (gained > 0) {
            component.addCharge(robot, gained);
        }
    }

    /**
     * 构建指南中展示的 "可用充能物品" 列表。
     */
    public static List<ItemStack> getChargeItemDisplay() {
        List<ItemStack> items = new ArrayList<>();

        for (Map.Entry<String, Integer> entry :
                ElectricAndroids.getInstance().getInterfaceChargeItems().entrySet()) {
            SlimefunItem chargeItem = SlimefunItem.getById(entry.getKey());

            if (chargeItem == null) {
                continue;
            }

            ItemStack item = chargeItem.getItem().clone();
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§8\u21E8 §7放入 §f安卓接口(燃料) §7中");
                lore.add("§8\u21E8 §7可为机器人充入 §b" + entry.getValue() + " J §7电量");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            items.add(item);
        }

        return items;
    }

    // ==================== 电池输入槽 ====================

    /**
     * 电池输入槽吸收: 电量未满时, 消耗槽内的能量物品为机器人充能。
     * <p>
     * 与接口充能的零浪费规则不同, 这里允许溢出: 剩余空间不足一整颗时也会消耗整颗物品,
     * 且溢出的电量会被保留 (电量可暂时超过容量, 随消耗自然回落)。吸收行为与电网一致,
     * 不受暂停状态与动力挡位影响。
     */
    private static void absorbBatterySlot(
            EnergyNetComponent component, Block b, SlimefunUniversalData data, boolean hybrid) {
        UniversalMenu menu = data.getMenu();

        if (menu == null) {
            return;
        }

        int slot = hybrid ? HYBRID_BATTERY_SLOT : FUEL_INPUT_SLOT;
        ItemStack item = menu.getItemInSlot(slot);

        if (item == null || item.getType().isAir()) {
            return;
        }

        Integer value = getChargeValue(item);

        if (value == null) {
            return;
        }

        Location location = b.getLocation();
        long charge = component.getChargeLong(location, data);
        long capacity = component.getCapacityLong();

        if (charge >= capacity) {
            return;
        }

        int amount = item.getAmount();
        int consumed = 0;

        while (consumed < amount && charge < capacity) {
            consumed++;
            charge += value;
        }

        if (consumed == 0) {
            return;
        }

        if (consumed == amount) {
            menu.replaceExistingItem(slot, null);
        } else {
            item.setAmount(amount - consumed);
            menu.replaceExistingItem(slot, item);
        }

        // 直接写入电量数据以保留溢出: 原版 API 的 addCharge 会把电量封顶到容量
        StorageCacheUtils.setData(location, "energy-charge", String.valueOf(charge));
    }

    /**
     * 读取物品可转化成的电量, 非充能物品返回 null。
     */
    private static Integer getChargeValue(ItemStack item) {
        for (Map.Entry<String, Integer> entry :
                ElectricAndroids.getInstance().getInterfaceChargeItems().entrySet()) {
            SlimefunItem chargeItem = SlimefunItem.getById(entry.getKey());

            if (chargeItem == null || !SlimefunUtils.isItemSimilar(item, chargeItem.getItem(), true)) {
                continue;
            }

            return entry.getValue();
        }

        return null;
    }

    // ==================== 工具方法 ====================

    private static String resolvePureMode(SlimefunUniversalData data) {
        String mode = data.getData(KEY_MODE);

        if (MODE_AUTO.equals(mode) || MODE_FAST.equals(mode) || MODE_ECO.equals(mode)) {
            return mode;
        }

        return ElectricAndroids.getInstance().getDefaultPureElectricMode();
    }

    private static String resolveHybridGear(SlimefunUniversalData data) {
        String gear = data.getData(KEY_GEAR);

        if (GEAR_PURE_ELECTRIC.equals(gear) || GEAR_MIXED.equals(gear) || GEAR_FUEL.equals(gear)) {
            return gear;
        }

        return ElectricAndroids.getInstance().getDefaultHybridGear();
    }

    /**
     * 机器人可能已经移动, 通过数据中记录的最新位置重新定位当前方块。
     */
    private static Block resolveCurrentBlock(SlimefunUniversalData data) {
        if (data instanceof SlimefunUniversalBlockData blockData) {
            var lastPresent = blockData.getLastPresent();

            if (lastPresent != null) {
                return lastPresent.toLocation().getBlock();
            }
        }

        return null;
    }

    private static Location resolveRobotLocation(UniversalMenu menu) {
        var uniData = StorageCacheUtils.getUniversalBlock(menu.getUuid());

        if (uniData == null) {
            return null;
        }

        var lastPresent = uniData.getLastPresent();
        return lastPresent == null ? null : lastPresent.toLocation();
    }

    /**
     * 机器人当前位置是否接入能源网络。
     * <p>
     * 直接查询网络结构而非通过电量变化推测, 机器人移动后状态可立即刷新。
     */
    private static boolean isGridConnected(Location loc) {
        try {
            return EnergyNet.getNetworkFromLocation(loc) != null;
        } catch (Exception x) {
            return false;
        }
    }

    private static String formatBudget(double budget) {
        return String.valueOf(Math.round(budget * 10000) / 10000.0);
    }

    private static String formatSpeed(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    private static String formatFuel(float fuel) {
        if (fuel <= 0) {
            return "0";
        }
        if (fuel == Math.floor(fuel)) {
            return String.valueOf((int) fuel);
        }
        return String.format(Locale.ROOT, "%.1f", fuel);
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null) {
            return fallback;
        }

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException x) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException x) {
            return fallback;
        }
    }
}
