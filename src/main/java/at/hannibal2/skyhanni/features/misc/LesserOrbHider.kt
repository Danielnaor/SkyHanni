package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LesserOrbHider {
    private val config get() = SkyHanniMod.feature.misc
    private val hiddenEntities = LorenzUtils.weakReferenceList<EntityArmorStand>()

    private val lesserTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjgzMjM2NjM5NjA3MDM2YzFiYTM5MWMyYjQ2YTljN2IwZWZkNzYwYzhiZmEyOTk2YTYwNTU1ODJiNGRhNSJ9fX0="

    @SubscribeEvent
    fun onArmorChange(event: EntityEquipmentChangeEvent) {
        val entity = event.entity
        val itemStack = event.newItemStack ?: return

        if (entity is EntityArmorStand && event.isHand && itemStack.getSkullTexture() == lesserTexture) {
            hiddenEntities.add(entity)
        }
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!isEnabled()) return

        if (event.entity in hiddenEntities) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (event.type != EnumParticleTypes.REDSTONE) return

        for (armorStand in hiddenEntities) {
            val distance = armorStand.distanceTo(event.location)
            if (distance < 4) {
                event.isCanceled = true
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.lesserOrbHider
}
