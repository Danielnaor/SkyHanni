package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


import kotlin.time.Duration.Companion.seconds
/**
 * This class represents a feature in the garden inventory called SkyMartCopperPrice.
 * It provides functionality to calculate and display the price of copper in the SkyMart inventory.
 * The class subscribes to various events to handle inventory opening, closing, and rendering.
 */

class SkyMartCopperPrice {
    private val copperPattern = "§c(?<amount>.*) Copper".toPattern()
    private var display = emptyList<List<Any>>()
    private val config get() = GardenAPI.config.skyMart

    companion object {
        var inInventory = false
    }

    private fun ItemStack.loreCosts(): MutableList<NEUInternalName> {
        var found = false
        val list = mutableListOf<NEUInternalName>()
        for (lines in getLore()) {
            if (lines == "§7Cost") {
                found = true
                continue
            }

            if (!found) continue
            if (lines.isEmpty()) return list

            NEUItems.getInternalNameOrNull(lines)?.let {
                list.add(it)
            }
        }
        return list
    }
    
    /**
     * Handles the event when the inventory is fully opened.
     * If the feature is enabled and the inventory name is "SkyMart", it populates a table with the coins per copper ratio for each item in the inventory.
     * The table is then displayed in the UI.
     *
     * @param event The InventoryFullyOpenedEvent.
     */

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "SkyMart") return

        
        LorenzUtils.sendTitle("§SkyMart just opened!", 4.seconds)
        LorenzUtils.chat("§cSkyMart just opened!")
    
        inInventory = true
        val table = mutableMapOf<Pair<String, String>, Pair<Double, NEUInternalName>>()
        for (stack in event.inventoryItems.values) {
            val lore = stack.getLore()
            val otherItemsPrice = stack.loreCosts().sumOf { it.getPrice() }

            for (line in lore) {
                val internalName = stack.getInternalName()
                val lowestBin = internalName.getPriceOrNull() ?: continue
                val profit = lowestBin - otherItemsPrice

                val amount = copperPattern.matchMatcher(line) {
                    group("amount").replace(",", "").toInt()
                } ?: continue
                val factor = profit / amount
                val perFormat = NumberUtil.format(factor)
                val priceFormat = NumberUtil.format(profit)
                val amountFormat = NumberUtil.format(amount)

                val name = stack.nameWithEnchantment!!
                val advancedStats = if (config.copperPriceAdvancedStats) {
                    " §7(§6$priceFormat §7/ §c$amountFormat Copper§7)"
                } else ""
                val pair = Pair("$name§f:", "§6§l$perFormat$advancedStats")
                table[pair] = Pair(factor, internalName)
            }
        }

        val newList = mutableListOf<List<Any>>()
        newList.addAsSingletonList("§eCoins per Copper§f:")
        LorenzUtils.fillTable(newList, table)
        display = newList
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {  

        LorenzUtils.sendTitle("§SkyMartCopperPrice.kt --> onBackgroundDraw", 4.seconds)
        LorenzUtils.chat("§cSkyMartCopperPrice.kt --> onBackgroundDraw")

        
        if (inInventory) {
            config.copperPricePos.renderStringsAndItems(
                display,
                extraSpace = 5,
                itemScale = config.itemScale,
                posLabel = "SkyMart Copper Price"
            )
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.copperPrice

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.skyMartCopperPrice", "garden.skyMart.copperPrice")
        event.move(3, "garden.skyMartCopperPriceAdvancedStats", "garden.skyMart.copperPriceAdvancedStats")
        event.move(3, "garden.skyMartCopperPricePos", "garden.skyMart.copperPricePos")
    }
}
