package me.owdding.iconographic.features

import me.owdding.iconographic.ComponentLike
import me.owdding.iconographic.ExtractableTooltipLine
import me.owdding.iconographic.Iconographic.id
import me.owdding.iconographic.TooltipLine
import me.owdding.iconographic.config.categories.fishing.FishingConfig
import me.owdding.iconographic.font
import me.owdding.iconographic.lines.SpacerLine
import me.owdding.iconographic.system.RegisterFeature
import me.owdding.iconographic.system.Result
import me.owdding.iconographic.system.TooltipFeature
import me.owdding.iconographic.utils.chat.DisplayColor
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.ARGB
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockCategory
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import kotlin.math.max

@RegisterFeature
data object RodParts : TooltipFeature() {
    override val enabled: Boolean get() = FishingConfig.rodParts
    override val priority: Int = 11

    private val PART_PREFIXES = listOf("ථ", "ꨃ", "࿉", "Talk to Roddy")

    override fun ItemStack.modifyEntries(list: MutableList<TooltipLine>, previousResult: Result?): Result {
        if (getData(DataTypes.CATEGORY) != SkyBlockCategory.FISHING_ROD) return Result.unmodified

        val hookRange = findPartRange(list, "ථ") ?: return Result.unmodified
        val lineRange = findPartRange(list, "ꨃ") ?: return Result.unmodified
        val sinkerRange = findPartRange(list, "࿉") ?: return Result.unmodified
        val mechanicRange = findPartRange(list, "Talk to Roddy")

        val hookPart = parsePart(list, hookRange, ComponentType.HOOK, getData(DataTypes.HOOK)?.second) ?: return Result.unmodified
        val linePart = parsePart(list, lineRange, ComponentType.LINE, getData(DataTypes.LINE)?.second) ?: return Result.unmodified
        val sinkerPart = parsePart(list, sinkerRange, ComponentType.SINKER, getData(DataTypes.SINKER)?.second) ?: return Result.unmodified

        val ranges = listOfNotNull(hookRange, lineRange, sinkerRange, mechanicRange)
        val removeStart = ranges.minOf { it.first }
        val removeEnd = ranges.maxOf { it.last }

        for (i in removeEnd downTo removeStart) {
            list.removeAt(i)
        }

        var insertIdx = removeStart
        list.add(insertIdx++, hookPart)
        list.add(insertIdx++, SpacerLine(height = 4))
        list.add(insertIdx++, linePart)
        list.add(insertIdx++, SpacerLine(height = 4))
        list.add(insertIdx++, sinkerPart)

        return Result.modified
    }

    private fun findPartRange(list: List<TooltipLine>, prefix: String): IntRange? {
        val start = list.indexOfFirst { (it as? ComponentLike)?.stripped?.trim()?.startsWith(prefix) == true }
        if (start == -1) return null

        var end = start
        while (end + 1 < list.size) {
            val nextLine = list[end + 1]
            val isBlank = when (nextLine) {
                is ComponentLike -> nextLine.stripped.isBlank()
                is SpacerLine -> true
                else -> false
            }
            if (isBlank) break

            val nextText = (nextLine as? ComponentLike)?.stripped?.trim() ?: ""
            if (PART_PREFIXES.any { nextText.startsWith(it) }) break

            end++
        }
        return start..end
    }

    private fun parsePart(
        list: MutableList<TooltipLine>,
        range: IntRange,
        type: ComponentType,
        skyblockId: String?,
    ): RodPartLine? {
        val lines = range.mapNotNull { list[it] as? ComponentLike }
        if (lines.isEmpty()) return null

        val isInstalled = !lines[0].stripped.trim().endsWith("NONE")
        return RodPartLine(type, lines[0], lines.drop(1), isInstalled, skyblockId?.let { SkyBlockId.item(it) })
    }

    data class RodPartLine(
        val type: ComponentType,
        val nameLine: ComponentLike,
        val statLines: List<ComponentLike>,
        val isInstalled: Boolean,
        val skyblockId: SkyBlockId?,
    ) : ExtractableTooltipLine {

        private val textXOffset = 32

        private val itemStack: ItemStack? by lazy {
            if (!isInstalled || skyblockId == null) return@lazy null
            skyblockId.toItem().takeUnless { it.isEmpty }
        }

        override fun extract(graphics: GuiGraphicsExtractor, totalWidth: Int, x: Int, y: Int) {
            val totalHeight = getHeight(font)

            val bgSize = 26
            val iconSize = 16
            val bgY = y + (totalHeight - bgSize) / 2
            val iconY = y + (totalHeight - iconSize) / 2

            val iconX = x + 5
            val textX = x + textXOffset

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, id("drill/background"), x, bgY, bgSize, bgSize, -1)

            val stack = itemStack
            if (stack != null) {
                graphics.item(stack, iconX, iconY)
            } else {
                val drawColor = if (isInstalled) type.color else ARGB.opaque(DisplayColor.GRAY)
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, type.id, iconX, iconY, iconSize, iconSize, drawColor)
            }

            graphics.text(font, nameLine.charSequence, textX, y + 4, -1)

            var currentY = y + 16
            for (stat in statLines) {
                graphics.text(font, stat.charSequence, textX, currentY, -1)
                currentY += font.lineHeight + 1
            }
        }

        override fun getWidth(font: Font): Int {
            val nameWidth = nameLine.width + textXOffset
            val statsWidth = statLines.maxOfOrNull { it.width + textXOffset } ?: 0
            return max(nameWidth, max(statsWidth, 100))
        }

        override fun getHeight(font: Font): Int = (18 + (statLines.size * (font.lineHeight + 1))).coerceAtLeast(26)
    }

    enum class ComponentType(location: String, val color: Int) {
        HOOK("hook", ARGB.opaque(DisplayColor.RED)),
        LINE("line", ARGB.opaque(DisplayColor.AQUA)),
        SINKER("sinker", ARGB.opaque(DisplayColor.YELLOW));

        val id = id("rod/$location")
    }
}
