package me.owdding.iconographic.config.categories.fishing

import com.teamresourceful.resourcefulconfig.api.types.options.TranslatableValue
import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.iconographic.config.AutoTranslated

object FishingConfig : CategoryKt("fishing"), AutoTranslated {
    override val translationBase: String = "iconographic.config.fishing"
    override val name: TranslatableValue = Translated(translationBase)

    val rodParts by autoBoolean(true)
}
