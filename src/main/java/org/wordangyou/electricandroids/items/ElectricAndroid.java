package org.wordangyou.electricandroids.items;

import city.norain.slimefun4.api.menu.UniversalMenu;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidFuelSource;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.Instruction;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.wordangyou.electricandroids.ElectricAndroidUtils;
import org.wordangyou.electricandroids.ElectricAndroids;

/**
 * 基础型电力机器人: 以电力代替燃料运行的可编程机器人。
 * <p>
 * 它不消耗任何燃料, 而是作为电网终端 (CONSUMER) 从电力网络充能,
 * 每执行一条脚本指令消耗定量电量。电量耗尽时会原地待机。
 */
public class ElectricAndroid extends ProgrammableAndroid implements EnergyNetComponent {

    public ElectricAndroid(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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

    /**
     * 脚本编辑器 (查看菜单): 打开后将 INTERFACE_FUEL 指令的文案改为充能语义。
     */
    @Override
    public void openScript(Player p, SlimefunUniversalBlockData uniData, String sourceCode) {
        super.openScript(p, uniData, sourceCode);
        renameInterfaceFuelInstruction(p);
    }

    /**
     * 脚本编辑器 (指令选择菜单): 打开后将 INTERFACE_FUEL 指令的文案改为充能语义。
     */
    @Override
    protected void editInstruction(Player p, SlimefunUniversalBlockData uniData, String[] script, int index) {
        super.editInstruction(p, uniData, script, index);
        renameInterfaceFuelInstruction(p);
    }

    /**
     * 纯电动机器人的 INTERFACE_FUEL 指令实际执行的是 {@code chargeFromInterface()}
     * (消耗面向的安卓接口中的能量物品充能), 而非装载燃料, 因此将脚本编辑器中的
     * "从容器中取出燃料" 文案改为充能语义, 避免误导。
     * <p>
     * 文案通过本地化键动态获取 (兼容自定义语言文件), 仅修改菜单显示, 不影响脚本数据。
     */
    private static void renameInterfaceFuelInstruction(Player p) {
        LegacyComponentSerializer legacySection = LegacyComponentSerializer.legacySection();
        PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
        String rawLabel = Slimefun.getLocalization()
                .getMessage(p, "android.scripts.instructions." + Instruction.INTERFACE_FUEL.name());
        String expected = plainText.serialize(legacySection.deserialize(rawLabel.replace('&', '§')));
        Inventory inventory = p.getOpenInventory().getTopInventory();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);

            if (item == null || !item.hasItemMeta()) {
                continue;
            }

            ItemMeta meta = item.getItemMeta();

            if (meta == null) {
                continue;
            }

            Component name = meta.displayName();

            if (name == null || !expected.equals(plainText.serialize(name))) {
                continue;
            }

            meta.displayName(Component.text("从容器中取出能量", NamedTextColor.RED));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }
    }

    /**
     * INTERFACE_FUEL 指令: 从 "安卓接口(燃料)" 中抽取能量物品为机器人充能。
     */
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