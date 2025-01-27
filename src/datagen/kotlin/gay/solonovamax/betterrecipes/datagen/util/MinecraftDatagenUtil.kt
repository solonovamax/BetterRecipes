@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED", "HasPlatformType")

package gay.solonovamax.betterrecipes.datagen.util

import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import gay.solonovamax.betterrecipes.datagen.recipe.BetterRecipesRecipeGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator.Pack
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.minecraft.block.Block
import net.minecraft.data.DataProvider
import net.minecraft.data.family.BlockFamily
import net.minecraft.data.recipe.CraftingRecipeJsonBuilder
import net.minecraft.data.recipe.RecipeGenerator.hasItem
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.data.family.BlockFamily.Variant as FamilyVariant

operator fun BlockFamily.get(variant: FamilyVariant): Block = getVariant(variant)

fun <T : DataProvider> Pack.addProvider(providerClass: KClass<T>) {
    val primaryConstructor = requireNotNull(providerClass.primaryConstructor)
    val params = primaryConstructor.parameters

    when {
        !FabricDataOutput::class.createType().isSubtypeOf(params[0].type) -> error("First parameter must always be a FabricDataOutput")

        params.size == 1 -> addProvider { output ->
            primaryConstructor.call(output)
        }

        params.size == 2 && CompletableFuture::class.isSubclassOf(params[1].type.jvmErasure) -> addProvider { output, wrapperLookup ->
            primaryConstructor.call(output, wrapperLookup)
        }

        else -> error("Could not match constructor")
    }
}

fun <T : DataProvider> Pack.addProviders(vararg providerClasses: KClass<out T>) {
    for (providerClass in providerClasses) {
        addProvider(providerClass)
    }
}

context(BetterRecipesRecipeGenerator)
fun CraftingRecipeJsonBuilder.criterion(item: ItemConvertible) = criterion(hasItem(item), conditionsFromItem(item))

context(BetterRecipesRecipeGenerator)
fun CraftingRecipeJsonBuilder.criterion(items: TagKey<Item>) = criterion("has_${items.id.path}", conditionsFromTag(items))

context(BetterRecipesRecipeGenerator)
fun CraftingRecipeJsonBuilder.offerRecipe(suffix: String? = null) {
    if (suffix != null)
        offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE, identifierOf(path = "${outputItem.itemPath}_$suffix")))
    else
        offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE, identifierOf(path = outputItem.itemPath)))
}

fun ItemConvertible.hasItem() = "has_$itemPath"

val ItemConvertible.itemPath: String
    get() = asItem().id.path

val ItemConvertible.recipeName: String
    get() = itemPath

fun ItemConvertible.convert(from: ItemConvertible) = "from_${from.itemPath}"

val ItemConvertible.smeltingItemPath: String
    get() = "from_smelting"

val ItemConvertible.blastingItemPath: String
    get() = "from_blasting"

val ItemConvertible.smokingItemPath: String
    get() = "from_smoking"

val ItemConvertible.campfireCookingItemPath: String
    get() = "from_campfire_cooking"