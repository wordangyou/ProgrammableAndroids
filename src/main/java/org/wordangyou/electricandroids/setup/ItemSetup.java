package org.wordangyou.electricandroids.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.wordangyou.electricandroids.ElectricAndroids;
import org.wordangyou.electricandroids.items.ElectricAndroid;
import org.wordangyou.electricandroids.items.ElectricButcherAndroid;
import org.wordangyou.electricandroids.items.ElectricFarmerAndroid;
import org.wordangyou.electricandroids.items.ElectricFishermanAndroid;
import org.wordangyou.electricandroids.items.ElectricMinerAndroid;
import org.wordangyou.electricandroids.items.ElectricWoodcutterAndroid;
import org.wordangyou.electricandroids.items.HybridAndroid;
import org.wordangyou.electricandroids.items.HybridButcherAndroid;
import org.wordangyou.electricandroids.items.HybridFarmerAndroid;
import org.wordangyou.electricandroids.items.HybridFishermanAndroid;
import org.wordangyou.electricandroids.items.HybridMinerAndroid;
import org.wordangyou.electricandroids.items.HybridWoodcutterAndroid;

/**
 * 注册纯电动 / 混合动力两个系列的机器人物品。
 */
public final class ItemSetup {

    private ItemSetup() {}

    public static void setup(ElectricAndroids plugin) {
        int energyPerOperation = plugin.getEnergyPerOperation();

        // ==================== 纯电动系列 ====================

        ItemGroup pureGroup = new ItemGroup(
                new NamespacedKey(plugin, "electric_androids"),
                new SlimefunItemStack("ELECTRIC_ANDROIDS", HeadTexture.ENERGY_REGULATOR, "&b纯电动机器人"));
        pureGroup.register(plugin);

        SlimefunItemStack electric = new SlimefunItemStack(
                "ELECTRIC_ANDROID",
                HeadTexture.PROGRAMMABLE_ANDROID,
                "&b纯电动机器人",
                lorePure(energyPerOperation, "&f以 &b电力 &f为唯一能源的机器人", "&f无需任何燃料"));

        SlimefunItemStack electricMiner = new SlimefunItemStack(
                "ELECTRIC_ANDROID_MINER",
                HeadTexture.PROGRAMMABLE_ANDROID_MINER,
                "&b纯电动矿工机器人",
                lorePure(energyPerOperation, "&f可以挖掘 &e脚本 &f中指定的方块"));

        SlimefunItemStack electricFarmer = new SlimefunItemStack(
                "ELECTRIC_ANDROID_FARMER",
                HeadTexture.PROGRAMMABLE_ANDROID_FARMER,
                "&b纯电动农业机器人",
                lorePure(energyPerOperation, "&f可以收获 &e脚本 &f中指定的作物"));

        SlimefunItemStack electricWoodcutter = new SlimefunItemStack(
                "ELECTRIC_ANDROID_WOODCUTTER",
                HeadTexture.PROGRAMMABLE_ANDROID_WOODCUTTER,
                "&b纯电动伐木机器人",
                lorePure(energyPerOperation, "&f可以砍伐 &e脚本 &f中指定的树木"));

        SlimefunItemStack electricButcher = new SlimefunItemStack(
                "ELECTRIC_ANDROID_BUTCHER",
                HeadTexture.PROGRAMMABLE_ANDROID_BUTCHER,
                "&b纯电动战斗机器人",
                lorePure(energyPerOperation, "&f可以攻击 &e脚本 &f中指定的生物"));

        SlimefunItemStack electricFisherman = new SlimefunItemStack(
                "ELECTRIC_ANDROID_FISHERMAN",
                HeadTexture.PROGRAMMABLE_ANDROID_FISHERMAN,
                "&b纯电动钓鱼机器人",
                lorePure(energyPerOperation, "&f可以在水面自动钓鱼"));

        new ElectricAndroid(pureGroup, electric, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    SlimefunItems.PLASTIC_SHEET,
                    SlimefunItems.ANDROID_MEMORY_CORE,
                    SlimefunItems.PLASTIC_SHEET,
                    SlimefunItems.ADVANCED_CIRCUIT_BOARD,
                    SlimefunItems.BATTERY,
                    SlimefunItems.BATTERY,
                    SlimefunItems.PLASTIC_SHEET,
                    SlimefunItems.ELECTRIC_MOTOR,
                    SlimefunItems.PLASTIC_SHEET
                })
                .register(plugin);

        new ElectricMinerAndroid(pureGroup, electricMiner, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    null,
                    null,
                    new ItemStack(Material.DIAMOND_PICKAXE),
                    electric,
                    new ItemStack(Material.DIAMOND_PICKAXE),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        new ElectricFarmerAndroid(pureGroup, electricFarmer, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    null,
                    null,
                    new ItemStack(Material.DIAMOND_HOE),
                    electric,
                    new ItemStack(Material.DIAMOND_HOE),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        new ElectricWoodcutterAndroid(pureGroup, electricWoodcutter, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    null,
                    null,
                    new ItemStack(Material.DIAMOND_AXE),
                    electric,
                    new ItemStack(Material.DIAMOND_AXE),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        new ElectricButcherAndroid(pureGroup, electricButcher, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    SlimefunItems.GPS_TRANSMITTER,
                    null,
                    new ItemStack(Material.DIAMOND_SWORD),
                    electric,
                    new ItemStack(Material.DIAMOND_SWORD),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        new ElectricFishermanAndroid(pureGroup, electricFisherman, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    null,
                    null,
                    new ItemStack(Material.FISHING_ROD),
                    electric,
                    new ItemStack(Material.FISHING_ROD),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        // ==================== 混合动力系列 ====================

        ItemGroup hybridGroup = new ItemGroup(
                new NamespacedKey(plugin, "hybrid_androids"),
                new SlimefunItemStack("HYBRID_ANDROIDS", HeadTexture.BATTERY, "&b混合动力机器人"));
        hybridGroup.register(plugin);

        SlimefunItemStack hybrid = new SlimefunItemStack(
                "HYBRID_ANDROID",
                HeadTexture.PROGRAMMABLE_ANDROID,
                "&b混合动力机器人",
                loreHybrid(energyPerOperation, "&f以 &b电力 + 燃料 &f双能源驱动的机器人", "&f电力耗尽时自动切换燃料"));

        SlimefunItemStack hybridMiner = new SlimefunItemStack(
                "HYBRID_ANDROID_MINER",
                HeadTexture.PROGRAMMABLE_ANDROID_MINER,
                "&b混合动力矿工机器人",
                loreHybrid(energyPerOperation, "&f可以挖掘 &e脚本 &f中指定的方块"));

        SlimefunItemStack hybridFarmer = new SlimefunItemStack(
                "HYBRID_ANDROID_FARMER",
                HeadTexture.PROGRAMMABLE_ANDROID_FARMER,
                "&b混合动力农业机器人",
                loreHybrid(energyPerOperation, "&f可以收获 &e脚本 &f中指定的作物"));

        SlimefunItemStack hybridWoodcutter = new SlimefunItemStack(
                "HYBRID_ANDROID_WOODCUTTER",
                HeadTexture.PROGRAMMABLE_ANDROID_WOODCUTTER,
                "&b混合动力伐木机器人",
                loreHybrid(energyPerOperation, "&f可以砍伐 &e脚本 &f中指定的树木"));

        SlimefunItemStack hybridButcher = new SlimefunItemStack(
                "HYBRID_ANDROID_BUTCHER",
                HeadTexture.PROGRAMMABLE_ANDROID_BUTCHER,
                "&b混合动力战斗机器人",
                loreHybrid(energyPerOperation, "&f可以攻击 &e脚本 &f中指定的生物"));

        SlimefunItemStack hybridFisherman = new SlimefunItemStack(
                "HYBRID_ANDROID_FISHERMAN",
                HeadTexture.PROGRAMMABLE_ANDROID_FISHERMAN,
                "&b混合动力钓鱼机器人",
                loreHybrid(energyPerOperation, "&f可以在水面自动钓鱼"));

        new HybridAndroid(hybridGroup, hybrid, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    new ItemStack(Material.BLAZE_ROD),
                    new ItemStack(Material.COAL_BLOCK),
                    new ItemStack(Material.BLAZE_ROD),
                    SlimefunItems.ADVANCED_CIRCUIT_BOARD,
                    electric,
                    SlimefunItems.BATTERY,
                    SlimefunItems.PLASTIC_SHEET,
                    SlimefunItems.ELECTRIC_MOTOR,
                    SlimefunItems.PLASTIC_SHEET
                })
                .register(plugin);

        new HybridMinerAndroid(hybridGroup, hybridMiner, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    new ItemStack(Material.COAL_BLOCK),
                    null,
                    new ItemStack(Material.DIAMOND_PICKAXE),
                    hybrid,
                    new ItemStack(Material.DIAMOND_PICKAXE),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        new HybridFarmerAndroid(hybridGroup, hybridFarmer, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    new ItemStack(Material.COAL_BLOCK),
                    null,
                    new ItemStack(Material.DIAMOND_HOE),
                    hybrid,
                    new ItemStack(Material.DIAMOND_HOE),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        new HybridWoodcutterAndroid(hybridGroup, hybridWoodcutter, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    new ItemStack(Material.COAL_BLOCK),
                    null,
                    new ItemStack(Material.DIAMOND_AXE),
                    hybrid,
                    new ItemStack(Material.DIAMOND_AXE),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        new HybridButcherAndroid(hybridGroup, hybridButcher, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    SlimefunItems.GPS_TRANSMITTER,
                    null,
                    new ItemStack(Material.DIAMOND_SWORD),
                    hybrid,
                    new ItemStack(Material.DIAMOND_SWORD),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);

        new HybridFishermanAndroid(hybridGroup, hybridFisherman, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    null,
                    new ItemStack(Material.COAL_BLOCK),
                    null,
                    new ItemStack(Material.FISHING_ROD),
                    hybrid,
                    new ItemStack(Material.FISHING_ROD),
                    null,
                    SlimefunItems.ELECTRIC_MOTOR,
                    null
                })
                .register(plugin);
    }

    private static String[] lorePure(int energyPerOperation, String... description) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        Collections.addAll(lore, description);
        lore.add("");
        lore.add("&8\u21E8 &7每次操作消耗 &b" + energyPerOperation + " J &7电量");
        lore.add("&8\u21E8 &7接入 &b电力网络 &7自动充能");
        lore.add("&8\u21E8 &7使用 &b安卓接口(燃料) &7+ 能量物品原地充能");
        lore.add("&8\u21E8 &7点击面板中的 &b电池 &7切换速度模式");
        return lore.toArray(new String[0]);
    }

    private static String[] loreHybrid(int energyPerOperation, String... description) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        Collections.addAll(lore, description);
        lore.add("");
        lore.add("&8\u21E8 &7电力模式每次操作消耗 &b" + energyPerOperation + " J &7电量");
        lore.add("&8\u21E8 &7接入 &b电力网络 &7自动充能");
        lore.add("&8\u21E8 &7使用 &b安卓接口(燃料) &7+ 能量物品充能或装载燃料");
        lore.add("&8\u21E8 &7点击面板中的 &b电池 &7切换动力挡位");
        return lore.toArray(new String[0]);
    }
}