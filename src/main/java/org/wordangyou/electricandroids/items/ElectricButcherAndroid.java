package org.wordangyou.electricandroids.items;

import city.norain.slimefun4.api.menu.UniversalMenu;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidFuelSource;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ButcherAndroid;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.wordangyou.electricandroids.ElectricAndroidUtils;
import org.wordangyou.electricandroids.ElectricAndroids;

/**
 * 电力战斗机器人: 以电力驱动的 {@link ButcherAndroid}。
 */
public class ElectricButcherAndroid extends ButcherAndroid implements EnergyNetComponent {

    public ElectricButcherAndroid(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, 1, item, recipeType, recipe);
        ElectricAndroidUtils.setupPureElectricMenu(this);
    }

    /**
     * 电力机器人不需要燃料, 该实现仅用于规避父类的燃料注册逻辑。
     */
    @Override
    public AndroidFuelSource getFuelSource() {
        return AndroidFuelSource.SOLID;
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

        ElectricAndroidUtils.tickPureElectric(this, b, data, this::parentTick);
        ElectricAndroidUtils.updatePureStatus(this, b, data);
    }

    /**
     * 供额度累加器调用的 "父类 tick" 桥接方法, 执行脚本中的下一条指令。
     */
    private void parentTick(Block b, SlimefunUniversalData data) {
        super.tick(b, data);
    }

    @Override
    protected void refuel(UniversalMenu menu, Block facedBlock) {
        ElectricAndroidUtils.chargeFromInterface(this, menu, facedBlock);
    }

    @Override
    public List<ItemStack> getDisplayRecipes() {
        return ElectricAndroidUtils.getChargeItemDisplay();
    }

    @Override
    public String getRecipeSectionLabel(Player p) {
        return "§7\u21E9 §b可用充能物品 §7\u21E9";
    }
}