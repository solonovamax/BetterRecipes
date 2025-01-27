package gay.solonovamax.betterrecipes.datagen.util

import gay.solonovamax.betterrecipes.datagen.BetterResourcesDataGenerator
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

fun identifierOf(namespace: String = BetterResourcesDataGenerator.MOD_ID, path: String): Identifier = Identifier.of(namespace, path)

val Item.id: Identifier
    get() = Registries.ITEM.getId(this)

val ItemConvertible.itemId: Identifier
    get() = Registries.ITEM.getId(this.asItem())

val Block.id: Identifier
    get() = Registries.BLOCK.getId(this)
