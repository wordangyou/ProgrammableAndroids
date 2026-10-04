package org.wordangyou.electricandroids.items;

import city.norain.slimefun4.api.menu.UniversalMenu;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.wordangyou.electricandroids.ElectricAndroidUtils;
import org.wordangyou.electricandroids.ElectricAndroids;

/**
 * 混合动力机器人: 以 "电力 + 燃料" 双能源驱动的 {@link ProgrammableAndroid}。
 * <p>
 * 默认混合挡下电力优先, 电量不足时自动切换为燃料驱动;
 * 电力耗尽时也会通过 INTERFACE_FUEL 指令自动装载燃料。
 */
public class HybridAndroid extends ProgrammableAndroid implements EnergyNetComponent {

    public HybridAndroid(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, 1, item, recipeType, recipe);
        ElectricAndroidUtils.setupHybridMenu(this);
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public long getCapacityLong() {
        return ElectricAndroids.getInstance().getEnergyCapacity();
    }

    @Override
    public int getCapacity() {
        return ElectricAndroids.getInstance().getEnergyCapacity();
    }

    @Override
    protected void tick(Block b, SlimefunUniversalData data) {
        if (b.getType() != Material.PLAYER_HEAD) {
            return;
        }

        ElectricAndroidUtils.tickHybrid(this, b, data, this::parentTick);
        ElectricAndroidUtils.updateHybridStatus(this, b, data);
    }

    /**
     * 供额度累加器调用的 "父类 tick" 桥接方法, 执行脚本中的下一条指令。
     */
    private void parentTick(Block b, SlimefunUniversalData data) {
        super.tick(b, data);
    }

    /**
     * INTERFACE_FUEL 指令: 先吃光能量物品, 剩余物品再交给父类装入燃料罐。
     */
    @Override
    protected void refuel(UniversalMenu menu, Block facedBlock) {
        ElectricAndroidUtils.chargeFromInterface(this, menu, facedBlock);
        super.refuel(menu, facedBlock);
    }

    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> list = new ArrayList<>(ElectricAndroidUtils.getChargeItemDisplay());
        list.addAll(super.getDisplayRecipes());
        return list;
    }

    @Override
    public String getRecipeSectionLabel(Player p) {
        return "§7\u21E9 §b可用能源: 电力与燃料 §7\u21E9";
    }
}