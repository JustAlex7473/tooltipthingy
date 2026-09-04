package me.owdding.iconographic.features

import me.owdding.iconographic.TooltipLine
import me.owdding.iconographic.TooltipLine.Companion.asComponentOrNull
import me.owdding.iconographic.config.categories.misc.MiscConfig
import me.owdding.iconographic.system.RegisterFeature
import me.owdding.iconographic.system.Result
import me.owdding.iconographic.system.TooltipFeature
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockEnchantmentsRepo
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped


@RegisterFeature
data object EnchantedBookFeature : TooltipFeature() {
    override val enabled: Boolean = MiscConfig.enchantedBookNames
    override val priority: Int = 0

    override fun ItemStack.applies(): Boolean = DataTypes.SKYBLOCK_ID()?.isEnchantment == true

    private fun ItemStack.enchantTitle(): Component? {
        val entry = DataTypes.ENCHANTMENTS()?.entries?.firstOrNull() ?: return null
        val enchant = SkyBlockEnchantmentsRepo.get(entry.key) ?: return null

        // makes a string that matches the line for the enchantment name + level
        val fullEnchantName = "${enchant.name} ${enchant.levels[entry.value]?.literalLevel}"

        // Finds constructed enchantment name + level from lore of itemstack
        return getLore().firstOrNull { it.stripped == fullEnchantName }
    }

    // Replaces "Enchanted Book" item name with the enchant's title
    override fun ItemStack.nameReplacement(original: Component): Component = enchantTitle() ?: original

    override fun ItemStack.modifyEntries(list: MutableList<TooltipLine>, previousResult: Result?): Result {
        val titleText = enchantTitle()?.stripped ?: return Result.unmodified

        // Removes the Enchant title from lore
        val titleIndex = list.indexOfFirst { it.asComponentOrNull()?.stripped?.trim()?.equals(titleText, ignoreCase = true) == true }
        if (titleIndex == -1) return Result.unmodified

        // Removes "Combinable in anvil" and the blank line from lore
        var start = titleIndex
        if (start >= 2 &&
            list[start - 2].asComponentOrNull()?.stripped?.trim()?.equals("Combinable in Anvil", ignoreCase = true) == true &&
            list[start - 1].asComponentOrNull()?.stripped?.isBlank() == true
        ) {
            start -= 2
        }

        var end = titleIndex
        if (end + 1 < list.size && list[end + 1].asComponentOrNull()?.stripped?.isBlank() == true) {
            end += 1
        }

        repeat(end - start + 1) { list.removeAt(start) }

        return Result.modified
    }


}
