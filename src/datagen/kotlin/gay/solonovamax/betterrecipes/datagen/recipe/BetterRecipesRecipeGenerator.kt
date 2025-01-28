package gay.solonovamax.betterrecipes.datagen.recipe

import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull
import kotlin.streams.asSequence
import gay.solonovamax.betterrecipes.datagen.util.blastingItemPath
import gay.solonovamax.betterrecipes.datagen.util.campfireCookingItemPath
import gay.solonovamax.betterrecipes.datagen.util.convert
import gay.solonovamax.betterrecipes.datagen.util.criterion
import gay.solonovamax.betterrecipes.datagen.util.group
import gay.solonovamax.betterrecipes.datagen.util.hasItem
import gay.solonovamax.betterrecipes.datagen.util.id
import gay.solonovamax.betterrecipes.datagen.util.itemPath
import gay.solonovamax.betterrecipes.datagen.util.offerRecipe
import gay.solonovamax.betterrecipes.datagen.util.optionalOf
import gay.solonovamax.betterrecipes.datagen.util.smeltingItemPath
import gay.solonovamax.betterrecipes.datagen.util.smokingItemPath
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.advancement.criterion.InventoryChangedCriterion.Conditions
import net.minecraft.advancement.criterion.InventoryChangedCriterion.Conditions.Slots
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.data.family.BlockFamilies
import net.minecraft.data.family.BlockFamily.Variant
import net.minecraft.data.recipe.CookingRecipeJsonBuilder
import net.minecraft.data.recipe.CraftingRecipeJsonBuilder
import net.minecraft.data.recipe.RecipeExporter
import net.minecraft.data.recipe.RecipeGenerator
import net.minecraft.data.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.predicate.NumberRange.IntRange as MojangIntRange

@Suppress("SameParameterValue")
class BetterRecipesRecipeGenerator(
    registries: RegistryWrapper.WrapperLookup,
    val exporter: RecipeExporter,
) : RecipeGenerator(registries, exporter) {
    override fun generate() {
        generateWoodenRecipes()
        generateIronRecipes()
        generateMinecartRecipes()
        generateRedstoneRecipes()
        generateRawOreRecipes()
        generateFoodRecipes()
        generateMiscRecipes()
        generateStoneRecipes()
    }

    /**
     * - 1 log -> 4 planks
     * - 2 planks -> 4 sticks
     */
    fun generateWoodenRecipes() {
        createShaped(RecipeCategory.DECORATIONS, Blocks.CHEST, 1 * 4)
            .input('#', ItemTags.LOGS)
            .pattern("###")
            .pattern("# #")
            .pattern("###")
            .group(Blocks.CHEST)
            .criterion(
                "has_lots_of_items",
                Criteria.INVENTORY_CHANGED.create(
                    Conditions(
                        optionalOf(),
                        Slots(MojangIntRange.atLeast(10), MojangIntRange.ANY, MojangIntRange.ANY),
                        listOf()
                    )
                )
            )
            .offerRecipe()

        createShaped(RecipeCategory.DECORATIONS, Blocks.TRAPPED_CHEST, 1)
            .input('#', ItemTags.PLANKS)
            .input('t', Blocks.TRIPWIRE_HOOK)
            .pattern("###")
            .pattern("#t#")
            .pattern("###")
            .group(Blocks.TRAPPED_CHEST)
            .criterion(Blocks.TRIPWIRE_HOOK)
            .offerRecipe()

        createShaped(RecipeCategory.MISC, Items.BOWL, 4 / 2)
            .input('#', Items.STICK)
            .pattern("# #")
            .pattern(" # ")
            .group(Items.BOWL)
            .criterion(Blocks.BROWN_MUSHROOM)
            .criterion(Blocks.RED_MUSHROOM)
            .criterion(Items.MUSHROOM_STEW)
            .offerRecipe(suffix = "from_sticks")

        createShaped(RecipeCategory.MISC, Items.BOWL, 4 * 4)
            .input('#', ItemTags.LOGS)
            .pattern("# #")
            .pattern(" # ")
            .group(Items.BOWL)
            .criterion(Blocks.BROWN_MUSHROOM)
            .criterion(Blocks.RED_MUSHROOM)
            .criterion(Items.MUSHROOM_STEW)
            .offerRecipe(suffix = "from_logs")

        createShaped(RecipeCategory.MISC, Items.STICK, 4 * 4)
            .input('#', ItemTags.LOGS)
            .pattern("#")
            .pattern("#")
            .group("sticks")
            .criterion(ItemTags.LOGS)
            .offerRecipe()

        createShaped(RecipeCategory.MISC, Items.STICK, 4)
            .input('#', ItemTags.PLANKS)
            .pattern("##")
            .group("sticks")
            .criterion(ItemTags.LOGS)
            .offerRecipe(suffix = "sideways")

        createShaped(RecipeCategory.MISC, Items.STICK, 4 * 4)
            .input('#', ItemTags.LOGS)
            .pattern("##")
            .group("sticks")
            .criterion(ItemTags.LOGS)
            .offerRecipe(suffix = "sideways_bulk")

        BlockFamilies.getFamilies().asSequence().filter { family ->
            family.shouldGenerateRecipes()
        }.filter { family ->
            family.group.getOrNull() == "wooden"
        }.filter { family ->
            family.baseBlock in Registries.BLOCK
        }.filter { family ->
            family.baseBlock.id.path.contains("planks")
        }.map { family ->
            family to TagKey.of(RegistryKeys.ITEM, family.baseBlock.planksToLogId())
        }.filterNot { (_, tag) ->
            // Registries.ITEM.getOptional(tag).isPresent
            "bamboo" in tag.id.path // the previous statement does not work (bruh)
        }.forEach { (family, logsTag) ->
            for ((variant, block) in family.variants) {
                val recipeBuilder = when (variant) {
                    Variant.BUTTON -> continue
                    Variant.CHISELED -> continue
                    Variant.CRACKED -> continue
                    Variant.CUT -> continue
                    Variant.DOOR -> createBulkDoorRecipe(logsTag, block, 4)
                    Variant.CUSTOM_FENCE -> continue
                    Variant.FENCE -> continue
                    Variant.CUSTOM_FENCE_GATE -> continue
                    Variant.FENCE_GATE -> continue
                    Variant.MOSAIC -> continue
                    Variant.SIGN -> continue
                    Variant.SLAB -> createBulkSlabRecipe(logsTag, block, 4)
                    Variant.STAIRS -> createBulkStairsRecipe(logsTag, block, 4)
                    Variant.PRESSURE_PLATE -> createBulkPressurePlateRecipe(logsTag, block, 4)
                    Variant.POLISHED -> continue
                    Variant.TRAPDOOR -> createBulkTrapdoorRecipe(logsTag, block, 4)
                    Variant.WALL -> continue
                    Variant.WALL_SIGN -> continue
                }

                family.getGroup()
                    .getOrNull()
                    ?.let { group ->
                        @Suppress("UsePropertyAccessSyntax")
                        recipeBuilder.group("${group}_${variant.getName()}")
                    }

                recipeBuilder
                    .criterion(family.unlockCriterionName.orElseGet { block.hasItem() }, conditionsFromTag(logsTag))
                    .offerRecipe()
            }
        }
    }

    /**
     * - 1 iron block -> 9 iron ingots
     * - 1 iron ingot -> 9 iron nuggets
     */
    fun generateIronRecipes() {
        createShaped(RecipeCategory.MISC, Items.BUCKET, 1 * 9)
            .input('#', Items.IRON_BLOCK)
            .pattern("# #")
            .pattern(" # ")
            .group(Items.BUCKET)
            .criterion(Items.IRON_BLOCK)
            .offerRecipe()

        createShaped(RecipeCategory.BREWING, Blocks.CAULDRON, 1 * 9)
            .input('#', Items.IRON_BLOCK)
            .pattern("# #")
            .pattern("# #")
            .pattern("###")
            .group(Blocks.CAULDRON)
            .criterion(Items.WATER_BUCKET)
            .offerRecipe()

        createShaped(RecipeCategory.DECORATIONS, Blocks.CHAIN, 1 * 9)
            .input('B', Items.IRON_BLOCK)
            .input('I', Items.IRON_INGOT)
            .pattern("I")
            .pattern("B")
            .pattern("I")
            .group(Blocks.CHAIN)
            .criterion(Items.IRON_BLOCK)
            .criterion(Items.IRON_INGOT)
            .offerRecipe()

        // non-bulk
        createShaped(RecipeCategory.DECORATIONS, Blocks.CHAIN)
            .input('I', Items.IRON_INGOT)
            .input('N', Items.IRON_NUGGET)
            .pattern("NIN")
            .group(Blocks.CHAIN)
            .criterion(Items.IRON_INGOT)
            .criterion(Items.IRON_NUGGET)
            .offerRecipe(suffix = "sideways")

        createShaped(RecipeCategory.DECORATIONS, Blocks.CHAIN, 1 * 9)
            .input('B', Items.IRON_BLOCK)
            .input('I', Items.IRON_INGOT)
            .pattern("IBI")
            .group(Blocks.CHAIN)
            .criterion(Items.IRON_BLOCK)
            .criterion(Items.IRON_INGOT)
            .offerRecipe(suffix = "sideways_bulk")

        offerBulkDoorRecipe(Items.IRON_BLOCK, Blocks.IRON_DOOR, 9)

        // region Anvils
        createShapeless(RecipeCategory.MISC, Blocks.ANVIL)
            .input(Blocks.CHIPPED_ANVIL)
            .input(Blocks.IRON_BLOCK)
            .group(Blocks.ANVIL)
            .criterion(Blocks.IRON_BLOCK)
            .offerRecipe(suffix = "from_chipped_anvil")

        createShapeless(RecipeCategory.MISC, Blocks.CHIPPED_ANVIL)
            .input(Blocks.DAMAGED_ANVIL)
            .input(Blocks.IRON_BLOCK)
            .group(Blocks.ANVIL)
            .criterion(Blocks.IRON_BLOCK)
            .offerRecipe(suffix = "from_damaged_anvil")

        createShapeless(RecipeCategory.MISC, Blocks.ANVIL)
            .input(Blocks.DAMAGED_ANVIL)
            .input(Blocks.IRON_BLOCK)
            .input(Blocks.IRON_BLOCK)
            .group(Blocks.ANVIL)
            .criterion(Blocks.IRON_BLOCK)
            .offerRecipe(suffix = "from_damaged_anvil")
        // endregion
    }

    fun generateMinecartRecipes() {
        createShaped(RecipeCategory.TRANSPORTATION, Items.CHEST_MINECART)
            .input('i', Items.IRON_INGOT)
            .input('C', Blocks.CHEST)
            .pattern("iCi")
            .pattern("iii")
            .group(Items.CHEST_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_iron_and_chest")

        createShaped(RecipeCategory.TRANSPORTATION, Items.CHEST_MINECART)
            .input('i', Items.IRON_INGOT)
            .input('L', ItemTags.LOGS)
            .pattern(" L ")
            .pattern("iLi")
            .pattern("iii")
            .group(Items.CHEST_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_iron_and_logs")

        createShaped(RecipeCategory.TRANSPORTATION, Items.CHEST_MINECART)
            .input('L', ItemTags.LOGS)
            .input('M', Items.MINECART)
            .pattern("L")
            .pattern("L")
            .pattern("M")
            .group(Items.CHEST_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_minecart_and_logs")

        createShaped(RecipeCategory.TRANSPORTATION, Items.CHEST_MINECART)
            .input('p', ItemTags.PLANKS)
            .input('M', Items.MINECART)
            .pattern("ppp")
            .pattern("pMp")
            .pattern("ppp")
            .group(Items.CHEST_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_minecart_and_planks")

        createShaped(RecipeCategory.TRANSPORTATION, Items.FURNACE_MINECART)
            .input('i', Items.IRON_INGOT)
            .input('F', Blocks.FURNACE)
            .pattern("iFi")
            .pattern("iii")
            .group(Items.FURNACE_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_iron_and_furnace")

        createShaped(RecipeCategory.TRANSPORTATION, Items.FURNACE_MINECART)
            .input('c', ItemTags.STONE_CRAFTING_MATERIALS)
            .input('M', Items.MINECART)
            .pattern("ccc")
            .pattern("cMc")
            .pattern("ccc")
            .group(Items.FURNACE_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_minecart_and_stone")

        createShaped(RecipeCategory.TRANSPORTATION, Items.HOPPER_MINECART)
            .input('i', Items.IRON_INGOT)
            .input('H', Blocks.HOPPER)
            .pattern("iHi")
            .pattern("iii")
            .group(Items.HOPPER_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_iron_and_hopper")

        createShaped(RecipeCategory.TRANSPORTATION, Items.HOPPER_MINECART)
            .input('i', Items.IRON_INGOT)
            .input('C', Items.CHEST)
            .input('M', Items.MINECART)
            .pattern("iMi")
            .pattern("iCi")
            .pattern(" i ")
            .group(Items.HOPPER_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_minecart_and_chest_and_iron")

        createShaped(RecipeCategory.TRANSPORTATION, Items.TNT_MINECART)
            .input('i', Items.IRON_INGOT)
            .input('T', Blocks.TNT)
            .pattern("iTi")
            .pattern("iii")
            .group(Items.TNT_MINECART)
            .criterion(Items.MINECART)
            .offerRecipe(suffix = "from_iron_and_tnt")
    }

    fun generateRedstoneRecipes() {
        createShaped(RecipeCategory.REDSTONE, Blocks.DISPENSER)
            .input('D', Blocks.DROPPER)
            .input('S', Items.STICK)
            .input('s', Items.STRING)
            .pattern("sS ")
            .pattern("sDS")
            .pattern("sS ")
            .group(Blocks.DISPENSER)
            .criterion(Items.BOW)
            .offerRecipe(suffix = "from_dropper_and_sticks_and_string")

        createShapeless(RecipeCategory.REDSTONE, Blocks.DISPENSER)
            .input(Blocks.DROPPER)
            .input(Items.BOW)
            .group(Blocks.DISPENSER)
            .criterion(Items.BOW)
            .offerRecipe(suffix = "from_dropper_and_bow")

        offerBulkPressurePlateRecipe(Blocks.IRON_BLOCK, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, 9)
        offerBulkPressurePlateRecipe(Blocks.GOLD_BLOCK, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, 9)

        offerBulk2x2CompactingRecipe(RecipeCategory.REDSTONE, Items.IRON_BLOCK, Blocks.IRON_TRAPDOOR, 9)

        offerBulkDoorRecipe(Items.COPPER_BLOCK, Blocks.COPPER_DOOR, 9)
        offerBulkTrapdoorRecipe(Items.COPPER_BLOCK, Blocks.COPPER_TRAPDOOR, 9)

        offerBulkDoorRecipe(Items.WAXED_COPPER_BLOCK, Blocks.WAXED_COPPER_DOOR, 9)
        offerBulkTrapdoorRecipe(Items.WAXED_COPPER_BLOCK, Blocks.WAXED_COPPER_TRAPDOOR, 9)

        offerBulkDoorRecipe(Items.EXPOSED_COPPER, Blocks.EXPOSED_COPPER_DOOR, 9)
        offerBulkTrapdoorRecipe(Items.EXPOSED_COPPER, Blocks.EXPOSED_COPPER_TRAPDOOR, 9)

        offerBulkDoorRecipe(Items.WAXED_EXPOSED_COPPER, Blocks.WAXED_EXPOSED_COPPER_DOOR, 9)
        offerBulkTrapdoorRecipe(Items.WAXED_EXPOSED_COPPER, Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, 9)

        offerBulkDoorRecipe(Items.WEATHERED_COPPER, Blocks.WEATHERED_COPPER_DOOR, 9)
        offerBulkTrapdoorRecipe(Items.WEATHERED_COPPER, Blocks.WEATHERED_COPPER_TRAPDOOR, 9)

        offerBulkDoorRecipe(Items.WAXED_WEATHERED_COPPER, Blocks.WAXED_WEATHERED_COPPER_DOOR, 9)
        offerBulkTrapdoorRecipe(Items.WAXED_WEATHERED_COPPER, Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, 9)

        offerBulkDoorRecipe(Items.OXIDIZED_COPPER, Blocks.OXIDIZED_COPPER_DOOR, 9)
        offerBulkTrapdoorRecipe(Items.OXIDIZED_COPPER, Blocks.OXIDIZED_COPPER_TRAPDOOR, 9)

        offerBulkDoorRecipe(Items.WAXED_OXIDIZED_COPPER, Blocks.WAXED_OXIDIZED_COPPER_DOOR, 9)
        offerBulkTrapdoorRecipe(Items.WAXED_OXIDIZED_COPPER, Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR, 9)

        createShaped(RecipeCategory.REDSTONE, Blocks.LIGHTNING_ROD)
            .input('#', Blocks.COPPER_BLOCK)
            .pattern("#")
            .pattern("#")
            .pattern("#")
            .group(Blocks.LIGHTNING_ROD)
            .criterion(Blocks.COPPER_BLOCK)
            .offerRecipe()

        createShaped(RecipeCategory.REDSTONE, Blocks.HOPPER)
            .input('L', ItemTags.LOGS)
            .input('i', Items.IRON_INGOT)
            .pattern("iLi")
            .pattern("iLi")
            .pattern(" i ")
            .group(Blocks.HOPPER)
            .criterion(Items.IRON_INGOT)
            .offerRecipe()
    }

    fun generateRawOreRecipes() {
        // smelting in bulk offers a slight efficiency boost
        offerOreCooking(RecipeCategory.MISC, Blocks.RAW_COPPER_BLOCK, Blocks.COPPER_BLOCK, 0.7F * 9, 200 * 6)
        offerOreCooking(RecipeCategory.MISC, Blocks.RAW_IRON_BLOCK, Blocks.IRON_BLOCK, 0.7F * 9, 200 * 6)
        offerOreCooking(RecipeCategory.MISC, Blocks.RAW_GOLD_BLOCK, Blocks.GOLD_BLOCK, 0.7F * 9, 200 * 6)
    }

    fun generateFoodRecipes() {
        // 1 hay -> 9 wheat
        createShaped(RecipeCategory.FOOD, Items.BREAD, 1 * 9)
            .input('#', Items.HAY_BLOCK)
            .pattern("###")
            .group(Items.BREAD)
            .criterion(Blocks.HAY_BLOCK)
            .offerRecipe()

        offerFoodCooking(
            RecipeCategory.MISC,
            Items.CHORUS_FRUIT,
            Items.POPPED_CHORUS_FRUIT,
            0.1F,
            200,
            includeSmelting = false, // smelting recipe for chorus fruit already exists
        )

        offerFoodCooking(
            RecipeCategory.MISC,
            Items.ROTTEN_FLESH,
            Items.LEATHER,
            0.35F,
            200,
        )

        createShaped(RecipeCategory.FOOD, Items.ENCHANTED_GOLDEN_APPLE)
            .input('#', Blocks.GOLD_BLOCK)
            .input('X', Items.APPLE)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .criterion(Blocks.GOLD_BLOCK)
            .offerRecipe()
    }

    fun generateMiscRecipes() {
        // 1 gold block -> 9 gold ingots
        // 1 redstone block -> 9 redstone dust
        createShaped(RecipeCategory.TOOLS, Items.CLOCK, 1 * 9)
            .input('G', Blocks.GOLD_BLOCK)
            .input('R', Blocks.REDSTONE_BLOCK)
            .pattern(" G ")
            .pattern("GRG")
            .pattern(" G ")
            .group(Items.CLOCK)
            .criterion(Blocks.REDSTONE_BLOCK)
            .offerRecipe()

        // 1 iron block -> 9 iron ingots
        // 1 redstone block -> 9 redstone dust
        createShaped(RecipeCategory.TOOLS, Items.COMPASS, 1 * 9)
            .input('I', Blocks.IRON_BLOCK)
            .input('R', Blocks.REDSTONE_BLOCK)
            .pattern(" I ")
            .pattern("IRI")
            .pattern(" I ")
            .group(Items.COMPASS)
            .criterion(Blocks.REDSTONE_BLOCK)
            .offerRecipe()

        // 1 gold ingot -> 9 gold nuggets
        // 1 melon block -> 9 melon slices
        createShaped(RecipeCategory.BREWING, Items.GLISTERING_MELON_SLICE, 1 * 9)
            .input('g', Items.GOLD_INGOT)
            .input('M', Items.MELON)
            .pattern("ggg")
            .pattern("gMg")
            .pattern("ggg")
            .group(Items.GLISTERING_MELON_SLICE)
            .criterion(Items.MELON_SLICE)
            .offerRecipe()

        // region Shulker Boxes
        createShaped(RecipeCategory.DECORATIONS, Blocks.SHULKER_BOX)
            .input('L', ItemTags.LOGS)
            .input('s', Items.SHULKER_SHELL)
            .pattern(" s")
            .pattern("LL")
            .pattern(" s")
            .group(Blocks.SHULKER_BOX)
            .criterion(Items.SHULKER_SHELL)
            .offerRecipe()

        createShaped(RecipeCategory.DECORATIONS, Blocks.SHULKER_BOX)
            .input('C', Blocks.CHEST)
            .input('s', Items.SHULKER_SHELL)
            .pattern("sCs")
            .group(Blocks.SHULKER_BOX)
            .criterion(Items.SHULKER_SHELL)
            .offerRecipe(suffix = "sideways")

        createShaped(RecipeCategory.DECORATIONS, Blocks.SHULKER_BOX)
            .input('L', ItemTags.LOGS)
            .input('s', Items.SHULKER_SHELL)
            .pattern(" L ")
            .pattern("sLs")
            .group(Blocks.SHULKER_BOX)
            .criterion(Items.SHULKER_SHELL)
            .offerRecipe(suffix = "sideways_logs")
        // endregion

        // 4 snowball -> 1 snow block
        // 3 snow blocks -> 6 snow layers
        // => 4 snowball -> 2 snow blocks
        offerBulk2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SNOW, Blocks.SNOW_BLOCK, 2)

        createShaped(RecipeCategory.DECORATIONS, Blocks.JUKEBOX)
            .input('#', ItemTags.PLANKS)
            .input('X', Items.AMETHYST_SHARD)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .group(Blocks.JUKEBOX)
            .criterion(Items.AMETHYST_SHARD)
            .offerRecipe()

        createShaped(RecipeCategory.DECORATIONS, Blocks.BELL)
            .input('s', Items.STICK)
            .input('G', Items.GOLD_INGOT)
            .input('g', Items.GOLD_NUGGET)
            .pattern("sss")
            .pattern("GGG")
            .pattern("GgG")
            .criterion(Items.GOLD_INGOT)
            .criterion(Items.GOLD_NUGGET)
            .offerRecipe()

        // region Mushroom Blocks
        offerReversible2x2CompactingRecipe(
            RecipeCategory.MISC,
            Blocks.BROWN_MUSHROOM,
            RecipeCategory.DECORATIONS,
            Blocks.BROWN_MUSHROOM_BLOCK,
        )

        offerReversible2x2CompactingRecipe(
            RecipeCategory.MISC,
            Blocks.RED_MUSHROOM,
            RecipeCategory.DECORATIONS,
            Blocks.RED_MUSHROOM_BLOCK,
        )

        createShaped(RecipeCategory.DECORATIONS, Blocks.MUSHROOM_STEM, 1)
            .input('b', Blocks.BROWN_MUSHROOM)
            .input('s', Blocks.RED_MUSHROOM)
            .pattern("bs")
            .pattern("sb")
            .group(Blocks.MUSHROOM_STEM)
            .criterion(Blocks.BROWN_MUSHROOM)
            .criterion(Blocks.RED_MUSHROOM)
            .offerRecipe(suffix = "1")

        createShaped(RecipeCategory.DECORATIONS, Blocks.MUSHROOM_STEM, 1)
            .input('b', Blocks.BROWN_MUSHROOM)
            .input('s', Blocks.RED_MUSHROOM)
            .pattern("sb")
            .pattern("bs")
            .group(Blocks.MUSHROOM_STEM)
            .criterion(Blocks.BROWN_MUSHROOM)
            .criterion(Blocks.RED_MUSHROOM)
            .offerRecipe(suffix = "2")
        // endregion

        // region Cobweb
        createShapeless(RecipeCategory.MISC, Items.STRING, 5)
            .input(Blocks.COBWEB)
            .criterion(Blocks.COBWEB)
            .offerRecipe(Items.STRING.convert(Blocks.COBWEB))

        createShaped(RecipeCategory.DECORATIONS, Blocks.COBWEB, 1)
            .input('#', Items.STRING)
            .pattern("# #")
            .pattern(" # ")
            .pattern("# #")
            .criterion(Items.STRING)
            .offerRecipe()
        // endregion

        createShapeless(RecipeCategory.MISC, Items.FLINT)
            .input(Blocks.GRAVEL)
            .criterion(Blocks.GRAVEL)
            .offerRecipe()

        // region Horses
        createShaped(RecipeCategory.MISC, Items.LEATHER_HORSE_ARMOR)
            .input('l', Items.LEATHER)
            .pattern("  l")
            .pattern("lll")
            .pattern("lll")
            .criterion(Items.LEATHER)
            .offerRecipe()

        createShaped(RecipeCategory.MISC, Items.IRON_HORSE_ARMOR)
            .input('l', Items.LEATHER)
            .input('i', Items.IRON_INGOT)
            .pattern("  i")
            .pattern("ili")
            .pattern("iii")
            .criterion(Items.IRON_INGOT)
            .offerRecipe()

        createShaped(RecipeCategory.MISC, Items.GOLDEN_HORSE_ARMOR)
            .input('l', Items.LEATHER)
            .input('g', Items.GOLD_INGOT)
            .pattern("  g")
            .pattern("glg")
            .pattern("ggg")
            .criterion(Items.GOLD_INGOT)
            .offerRecipe()

        createShaped(RecipeCategory.MISC, Items.DIAMOND_HORSE_ARMOR)
            .input('l', Items.LEATHER)
            .input('d', Items.DIAMOND)
            .pattern("  d")
            .pattern("dld")
            .pattern("ddd")
            .criterion(Items.DIAMOND)
            .offerRecipe()

        createShaped(RecipeCategory.MISC, Items.SADDLE)
            .input('l', Items.LEATHER)
            .input('s', Items.STRING)
            .input('i', Items.IRON_NUGGET)
            .pattern("lll")
            .pattern("s s")
            .pattern("i i")
            .criterion(Items.LEATHER)
            .offerRecipe()
        // endregion

        createShaped(RecipeCategory.MISC, Blocks.JACK_O_LANTERN)
            .input('A', Blocks.CARVED_PUMPKIN)
            .input('B', ItemTags.CANDLES)
            .pattern("A")
            .pattern("B")
            .group(Blocks.JACK_O_LANTERN)
            .criterion(Blocks.CARVED_PUMPKIN)
            .offerRecipe()

        createShapeless(RecipeCategory.MISC, Items.NAME_TAG)
            .input(Items.STRING)
            .input(Items.PAPER)
            .input(Items.INK_SAC)
            .criterion(Items.PAPER)
            .criterion(Items.INK_SAC)
            .criterion(Items.STRING)
            .offerRecipe()

        createShaped(RecipeCategory.MISC, Blocks.POINTED_DRIPSTONE, 8)
            .input('D', Blocks.DRIPSTONE_BLOCK)
            .pattern("D")
            .pattern("D")
            .criterion(Items.DRIPSTONE_BLOCK)
            .offerRecipe()

        offerBulk2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SOUL_SAND, Blocks.SOUL_SOIL, 4)
        offerBulk2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SOUL_SOIL, Blocks.SOUL_SAND, 4)

        createShapeless(RecipeCategory.MISC, Items.STRING, 4)
            .input(ItemTags.WOOL)
            .criterion(ItemTags.WOOL)
            .offerRecipe(suffix = "from_wool")

        createShapeless(RecipeCategory.MISC, Items.QUARTZ, 4)
            .input(Blocks.QUARTZ_BLOCK)
            .criterion(Blocks.QUARTZ_BLOCK)
            .offerRecipe()

        createShapeless(RecipeCategory.MISC, Items.CLAY_BALL, 4)
            .input(Blocks.CLAY)
            .criterion(Blocks.CLAY)
            .offerRecipe()

        createShapeless(RecipeCategory.MISC, Items.GLOW_INK_SAC, 1)
            .input(Items.GLOWSTONE_DUST)
            .input(Items.INK_SAC)
            .criterion(Items.GLOWSTONE_DUST)
            .criterion(Items.INK_SAC)
            .offerRecipe()

        createShapeless(RecipeCategory.MISC, Items.GLOW_LICHEN, 1)
            .input(Items.GLOWSTONE_DUST)
            .input(Blocks.VINE)
            .criterion(Items.GLOWSTONE_DUST)
            .criterion(Blocks.VINE)
            .offerRecipe()

        createShapeless(RecipeCategory.MISC, Items.GLOW_BERRIES, 1)
            .input(Items.GLOWSTONE_DUST)
            .input(Items.SWEET_BERRIES)
            .criterion(Items.GLOWSTONE_DUST)
            .criterion(Items.SWEET_BERRIES)
            .offerRecipe()
    }

    fun generateStoneRecipes() {
        val stonecutterConversionFamilies = mapOf(
            BlockFamilies.DEEPSLATE to BlockFamilies.COBBLED_DEEPSLATE,
            BlockFamilies.STONE to BlockFamilies.COBBLESTONE,
        )

        val mossyConversionFamilies = mapOf(
            BlockFamilies.COBBLESTONE to BlockFamilies.MOSSY_COBBLESTONE,
            BlockFamilies.STONE_BRICK to BlockFamilies.MOSSY_STONE_BRICK
        )

        val mossyConversionBlocks = listOf(
            Blocks.VINE,
            Blocks.MOSS_BLOCK
        )

        for ((baseFamily, mossyFamily) in mossyConversionFamilies) {
            for ((variant, baseBlock) in baseFamily.variants) {
                val mossyBlock = mossyFamily.getVariant(variant) ?: continue

                for (conversionBlock in mossyConversionBlocks) {
                    createShapeless(RecipeCategory.DECORATIONS, mossyBlock)
                        .input(baseBlock)
                        .input(conversionBlock)
                        .group(mossyBlock)
                        .criterion(conversionBlock)
                        .offerRecipe(suffix = mossyBlock.convert(conversionBlock))
                }
            }
        }

        for ((baseFamily, cobbledFamily) in stonecutterConversionFamilies) {
            offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, baseFamily.baseBlock, cobbledFamily.baseBlock)
            offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, cobbledFamily.baseBlock, baseFamily.baseBlock)

            for ((variant, baseBlock) in baseFamily.variants) {
                val cobbledBlock = cobbledFamily.getVariant(variant) ?: continue
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, cobbledBlock, baseBlock)
                offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, baseBlock, cobbledBlock)
            }
        }
    }

    private fun offerBulkDoorRecipe(input: ItemConvertible, output: ItemConvertible, factor: Int, group: Boolean = true) {
        createBulkDoorRecipe(input, output, factor)
            .let { if (group) it.group(output) else it }
            .criterion(input)
            .offerRecipe()
    }

    private fun createBulkDoorRecipe(input: ItemConvertible, output: ItemConvertible, factor: Int): ShapedRecipeJsonBuilder {
        return createBulkDoorRecipe(Ingredient.ofItem(input), output, factor)
    }

    private fun createBulkDoorRecipe(input: TagKey<Item>, output: ItemConvertible, factor: Int): ShapedRecipeJsonBuilder {
        return createBulkDoorRecipe(Ingredient.fromTag(registries.getOrThrow(RegistryKeys.ITEM).getOrThrow(input)), output, factor)
    }

    private fun createBulkDoorRecipe(input: Ingredient, output: ItemConvertible, factor: Int): ShapedRecipeJsonBuilder {
        // 1 log -> 4 planks
        return createShaped(RecipeCategory.REDSTONE, output, 3 * factor)
            .input('#', input)
            .pattern("##")
            .pattern("##")
            .pattern("##")
    }

    private fun offerBulkPressurePlateRecipe(input: ItemConvertible, output: ItemConvertible, factor: Int, group: Boolean = true) {
        createBulkPressurePlateRecipe(Ingredient.ofItem(input), output, factor)
            .let { if (group) it.group(output) else it }
            .criterion(input)
            .offerRecipe()
    }

    private fun createBulkPressurePlateRecipe(input: TagKey<Item>, output: ItemConvertible, factor: Int): CraftingRecipeJsonBuilder {
        return createBulkPressurePlateRecipe(Ingredient.fromTag(registries.getOrThrow(RegistryKeys.ITEM).getOrThrow(input)), output, factor)
    }

    private fun createBulkPressurePlateRecipe(input: Ingredient, output: ItemConvertible, factor: Int): CraftingRecipeJsonBuilder {
        // 1 log -> 4 planks
        return createShaped(RecipeCategory.REDSTONE, output, 1 * factor)
            .input('#', input)
            .pattern("##")
    }

    private fun createBulkSlabRecipe(input: TagKey<Item>, output: ItemConvertible, factor: Int): CraftingRecipeJsonBuilder {
        // 1 log -> 4 planks
        return createShaped(RecipeCategory.BUILDING_BLOCKS, output, 6 * factor)
            .input('#', input)
            .pattern("###")
    }

    private fun createBulkStairsRecipe(input: TagKey<Item>, output: ItemConvertible, factor: Int): CraftingRecipeJsonBuilder {
        // 1 log -> 4 planks
        return createShaped(RecipeCategory.BUILDING_BLOCKS, output, 4 * factor)
            .input('#', input)
            .pattern("#  ")
            .pattern("## ")
            .pattern("###")
    }

    private fun offerBulkTrapdoorRecipe(input: ItemConvertible, output: ItemConvertible, factor: Int, group: Boolean = true) {
        createBulkTrapdoorRecipe(input, output, factor)
            .let { if (group) it.group(output) else it }
            .criterion(input)
            .offerRecipe()
    }

    private fun createBulkTrapdoorRecipe(input: ItemConvertible, output: ItemConvertible, factor: Int): CraftingRecipeJsonBuilder {
        return createBulkTrapdoorRecipe(Ingredient.ofItem(input), output, factor)
    }

    private fun createBulkTrapdoorRecipe(input: TagKey<Item>, output: ItemConvertible, factor: Int): CraftingRecipeJsonBuilder {
        return createBulkTrapdoorRecipe(Ingredient.fromTag(registries.getOrThrow(RegistryKeys.ITEM).getOrThrow(input)), output, factor)
    }

    private fun createBulkTrapdoorRecipe(input: Ingredient, output: ItemConvertible, factor: Int): CraftingRecipeJsonBuilder {
        // 1 log -> 4 planks
        return createShaped(RecipeCategory.REDSTONE, output, 2 * factor)
            .input('#', input)
            .pattern("###")
            .pattern("###")
    }

    private fun offerBulk2x2CompactingRecipe(
        category: RecipeCategory,
        input: ItemConvertible,
        output: ItemConvertible,
        factor: Int,
        group: Boolean = true,
    ) {
        createShaped(category, output, 1 * factor)
            .input('#', input)
            .pattern("##")
            .pattern("##")
            .let { if (group) it.group(output) else it }
            .criterion(input)
            .offerRecipe()
    }

    private fun offerOreCooking(
        category: RecipeCategory,
        input: ItemConvertible,
        output: ItemConvertible,
        experience: Float,
        cookingTime: Int,
        group: Boolean = true,
    ) {
        offerBlasting(category, input, output, experience, cookingTime / 2, group)
        offerSmelting(category, input, output, experience, cookingTime, group)
    }

    private fun offerFoodCooking(
        category: RecipeCategory,
        input: ItemConvertible,
        output: ItemConvertible,
        experience: Float,
        cookingTime: Int,
        group: Boolean = true,
        includeSmelting: Boolean = true,
    ) {
        if (includeSmelting)
            offerSmelting(category, input, output, experience, cookingTime, group)
        offerSmoking(category, input, output, experience, cookingTime / 2, group)
        offerCampfireCooking(category, input, output, experience, cookingTime * 3, group)
    }

    private fun offerSmelting(
        category: RecipeCategory,
        input: ItemConvertible,
        output: ItemConvertible,
        experience: Float,
        cookingTime: Int,
        group: Boolean = true,
    ) {
        CookingRecipeJsonBuilder.createSmelting(Ingredient.ofItem(input), category, output, experience, cookingTime)
            .let { if (group) it.group(output) else it }
            .criterion(input)
            .offerRecipe(suffix = "${output.smeltingItemPath}_${input.itemPath}")
    }

    private fun offerBlasting(
        category: RecipeCategory,
        input: ItemConvertible,
        output: ItemConvertible,
        experience: Float,
        cookingTime: Int,
        group: Boolean = true,
    ) {
        CookingRecipeJsonBuilder.createBlasting(Ingredient.ofItem(input), category, output, experience, cookingTime)
            .let { if (group) it.group(output) else it }
            .criterion(input)
            .offerRecipe(suffix = "${output.blastingItemPath}_${input.itemPath}")
    }

    private fun offerSmoking(
        category: RecipeCategory,
        input: ItemConvertible,
        output: ItemConvertible,
        experience: Float,
        cookingTime: Int,
        group: Boolean = true,
    ) {
        CookingRecipeJsonBuilder.createSmoking(Ingredient.ofItem(input), category, output, experience, cookingTime)
            .let { if (group) it.group(output) else it }
            .criterion(input)
            .offerRecipe(suffix = "${output.smokingItemPath}_${input.itemPath}")
    }

    private fun offerCampfireCooking(
        category: RecipeCategory,
        input: ItemConvertible,
        output: ItemConvertible,
        experience: Float,
        cookingTime: Int,
        group: Boolean = true,
    ) {
        CookingRecipeJsonBuilder.createCampfireCooking(Ingredient.ofItem(input), category, output, experience, cookingTime)
            .let { if (group) it.group(output) else it }
            .criterion(input)
            .offerRecipe(suffix = "${output.campfireCookingItemPath}_${input.itemPath}")
    }

    private fun offerReversible2x2CompactingRecipe(
        reverseCategory: RecipeCategory,
        baseItem: ItemConvertible,
        compactingCategory: RecipeCategory,
        compactItem: ItemConvertible,
        compactingGroup: Boolean = true,
        reverseGroup: Boolean = true,
    ) {
        createShapeless(reverseCategory, baseItem, 4)
            .input(compactItem)
            .let { if (reverseGroup) it.group(baseItem) else it }
            .criterion(compactItem)
            .offerRecipe()

        createShaped(compactingCategory, compactItem, 1)
            .input('#', baseItem)
            .pattern("##")
            .pattern("##")
            .let { if (compactingGroup) it.group(compactItem) else it }
            .criterion(baseItem)
            .offerRecipe()
    }

    private fun Block.planksToLogId(): Identifier {
        val id = Registries.BLOCK.getId(this)
        val replacement = if (STEMS.any { it in id.path }) "stems" else "logs"
        return id.withPath(id.path.replace("planks", replacement))
    }

    class Provider(
        output: FabricDataOutput,
        registryLookup: CompletableFuture<RegistryWrapper.WrapperLookup>,
    ) : FabricRecipeProvider(output, registryLookup) {
        override fun getRecipeGenerator(
            lookup: RegistryWrapper.WrapperLookup,
            exporter: RecipeExporter,
        ): RecipeGenerator = BetterRecipesRecipeGenerator(lookup, exporter)

        override fun getName(): String = "Recipes"
    }

    companion object {
        private val STEMS = listOf(
            "warped",
            "crimson",
        )
    }
}