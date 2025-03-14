package gay.solonovamax.betterrecipes

import com.google.common.collect.Lists
import gay.solonovamax.betterrecipes.mixin.AbstractFurnaceBlockEntityAccessor
import gay.solonovamax.betterrecipes.mixin.LockableContainerBlockEntityAccessor
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.AbstractFurnaceBlock
import net.minecraft.block.Blocks
import net.minecraft.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.block.entity.FurnaceBlockEntity
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen
import net.minecraft.client.gui.screen.ingame.CraftingScreen
import net.minecraft.client.util.InputUtil
import net.minecraft.item.Items
import net.minecraft.recipe.AbstractCookingRecipe
import net.minecraft.recipe.BlastingRecipe
import net.minecraft.recipe.CampfireCookingRecipe
import net.minecraft.recipe.RecipeEntry
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.recipe.ShapelessRecipe
import net.minecraft.recipe.SmeltingRecipe
import net.minecraft.recipe.SmokingRecipe
import net.minecraft.recipe.display.FurnaceRecipeDisplay
import net.minecraft.recipe.display.ShapedCraftingRecipeDisplay
import net.minecraft.recipe.display.ShapelessCraftingRecipeDisplay
import net.minecraft.recipe.display.SlotDisplay.StackSlotDisplay
import net.minecraft.recipe.display.SlotDisplayContexts
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.silkmc.silk.core.math.vector.minus
import net.silkmc.silk.core.math.vector.plus
import net.silkmc.silk.core.math.vector.times
import org.slf4j.kotlin.getLogger
import org.slf4j.kotlin.info
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream


@Suppress("UnstableApiUsage")
object BetterRecipesTestmod : ModInitializer, ClientModInitializer, FabricClientGameTest {
    const val MOD_ID = "better-recipes"

    const val COOKING_TIME_STEPS = 10
    const val COOKING_TIME_INCREMENT = 100

    private val logger by getLogger()

    override fun onInitialize() {}

    override fun onInitializeClient() {}

    override fun runTest(context: ClientGameTestContext) {
        context.input.resizeWindow(1920, 1080)

        context.runOnClient<Throwable> { client ->
            client.options.apply {
                guiScale.value = 4
                viewDistance.value = 4
                simulationDistance.value = 5
                fullscreen.value = true
            }
        }

        val worldBuilder = context.worldBuilder()
            .setUseConsistentSettings(true)

        worldBuilder.create().use { singlePlayerContext ->
            val recipeManager = singlePlayerContext.server.computeOnServer<_, Throwable> { server -> server.recipeManager }
            recipeManager.values().asSequence().filter {
                it.id.value.namespace == MOD_ID
            }.map { it.value.group }.filterNot { it.isNullOrBlank() }.distinct().toHashSet()

            var (playerPos, playerFacing) = singlePlayerContext.server.computeOnServer<_, Throwable> { server ->
                val player = server.playerManager.playerList.single()
                player.pos to player.facing
            }

            recipeManager.values().asSequence().filterNot { entry ->
                "waxed" in entry.id.value.path
            }.filter { entry ->
                entry.id.value.namespace == MOD_ID //|| entry.value.group in recipeGroups
            }.groupBy { entry ->
                val id = entry.id.value
                when {
                    "mushroom" in id.path -> "mushroom"
                    "cobweb" in id.path -> "cobweb"
                    "weighted_pressure_plate" in id.path -> "weighted_pressure_plate"
                    "copper_door" in id.path -> "copper_door"
                    "copper_trapdoor" in id.path -> "copper_trapdoor"
                    "mossy_cobblestone" in id.path -> "mossy_cobblestone"
                    "mossy_stone_brick" in id.path -> "mossy_stone_brick"
                    "minecart" in id.path && "chest_minecart" !in id.path -> "minecart"
                    "soul" in id.path -> "soul"
                    "glow" in id.path -> "glow"
                    "horse_armor" in id.path -> "horse_armor"
                    "clock" in id.path || "compass" in id.path -> "clock_compass"
                    else -> entry.value.group.ifEmpty { null }
                }
            }.forEach { (group, recipes) ->
                val comparator = Comparator<RecipeEntry<*>> { a, b ->
                    val namespaceA = a.id.value.namespace
                    val namespaceB = b.id.value.namespace
                    val pathA = a.id.value.path
                    val pathB = b.id.value.path

                    when {
                        namespaceA == namespaceB -> pathA.compareTo(pathB)
                        namespaceA == "minecraft" -> -1
                        namespaceB == "minecraft" -> 1
                        else -> namespaceA.compareTo(namespaceB)
                    }
                }

                for (recipeEntry in recipes.sortedWith(comparator)) {
                    singlePlayerContext.server.computeOnServer<_, Throwable> { server ->
                        val player = server.playerManager.playerList.single()
                        player.teleport(playerPos.x, playerPos.y, playerPos.z, false)
                    }

                    val recipeId = recipeEntry.id.value
                    when (val recipe = recipeEntry.value) {
                        is CampfireCookingRecipe -> continue
                        is AbstractCookingRecipe -> screenshotCookingRecipe(context, singlePlayerContext, group, recipe, recipeId)
                        is ShapelessRecipe -> screenshotShapelessRecipe(context, singlePlayerContext, group, recipe, recipeId)
                        is ShapedRecipe -> screenshotShapedRecipe(context, singlePlayerContext, group, recipe, recipeId)
                    }

                    playerPos -= playerFacing.doubleVector
                }
            }
        }
    }

    private fun screenshotCookingRecipe(
        context: ClientGameTestContext,
        singlePlayerContext: TestSingleplayerContext,
        group: String?,
        recipe: AbstractCookingRecipe,
        recipeId: Identifier,
    ) {
        val slotDisplayContext = singlePlayerContext.server.computeOnServer<_, Throwable> { server ->
            SlotDisplayContexts.createParameters(server.overworld)
        }

        val recipeDisplay = recipe.displays.single() as FurnaceRecipeDisplay
        val result = recipeDisplay.result() as StackSlotDisplay
        val inputs = recipeDisplay.ingredient.getStacks(slotDisplayContext)

        for (input in inputs) {
            singlePlayerContext.server.runOnServer<RuntimeException> { server ->
                val player = server.playerManager.playerList.single()

                val world = player.world
                val targetPos = player.blockPos.up() + (player.facing.vector * 2)

                val block = when (recipe) {
                    is BlastingRecipe -> Blocks.BLAST_FURNACE
                    is CampfireCookingRecipe -> error("Should never happen")
                    is SmokingRecipe -> Blocks.SMOKER
                    is SmeltingRecipe -> Blocks.FURNACE
                    else -> error("Should never happen")
                }
                world.setBlockState(
                    targetPos,
                    block.defaultState.with(AbstractFurnaceBlock.LIT, true)
                )

                val furnace = world.getBlockEntity(targetPos)
                furnace as AbstractFurnaceBlockEntity
                furnace as AbstractFurnaceBlockEntityAccessor
                furnace as LockableContainerBlockEntityAccessor

                furnace.setStack(FurnaceBlockEntity.INPUT_SLOT_INDEX, input)
                furnace.setStack(FurnaceBlockEntity.FUEL_SLOT_INDEX, Items.COAL.defaultStack.copyWithCount(64))
                furnace.setStack(FurnaceBlockEntity.OUTPUT_SLOT_INDEX, result.stack)

                furnace.litTimeRemaining = 10000
                furnace.litTotalTime = 10000
                furnace.cookingTotalTime = COOKING_TIME_STEPS * COOKING_TIME_INCREMENT
            }

            singlePlayerContext.clientWorld.waitForChunksRender()

            context.waitTick()

            logger.info { "Taking screenshots for cooking recipe $recipeId" }

            for (iteration in COOKING_TIME_STEPS downTo 0) {
                singlePlayerContext.server.runOnServer<Throwable> { server ->
                    val player = server.playerManager.playerList.single()
                    val world = player.world
                    val targetPos = player.blockPos.up() + (player.facing.vector * 2)

                    val furnace = world.getBlockEntity(targetPos)

                    furnace as AbstractFurnaceBlockEntity
                    furnace as AbstractFurnaceBlockEntityAccessor
                    furnace.cookingTimeSpent = iteration * COOKING_TIME_INCREMENT
                }
                context.input.pressMouse(1) // 1 is right click
                context.waitFor { client -> client.currentScreen is AbstractFurnaceScreen<*> }

                val recipeInput = Registries.ITEM.getId(input.item).takeIf { inputs.size > 1 }
                val path = takeRecipeScreenshot(context, group, recipeId, recipeInput, "$iteration")

                // re-crop image
                val image = ImageIO.read(path.inputStream().buffered())
                val newImage = image.getSubimage(804, 220, 376, 280)
                ImageIO.write(newImage, "png", path.outputStream().buffered())

                context.input.pressKey(InputUtil.GLFW_KEY_ESCAPE)
            }

            singlePlayerContext.server.runOnServer<Throwable> { server ->
                val player = server.playerManager.playerList.single()
                val newPos = player.facing.rotateYClockwise().doubleVector + player.pos
                player.teleport(newPos.x, newPos.y, newPos.z, false)
            }
        }
    }

    private fun screenshotShapelessRecipe(
        context: ClientGameTestContext,
        singlePlayerContext: TestSingleplayerContext,
        group: String?,
        recipe: ShapelessRecipe,
        recipeId: Identifier,
    ) {
        val slotDisplayContext = singlePlayerContext.server.computeOnServer<_, Throwable> { server ->
            SlotDisplayContexts.createParameters(server.overworld)
        }

        val recipeDisplay = recipe.displays.single() as ShapelessCraftingRecipeDisplay
        val result = recipeDisplay.result() as StackSlotDisplay
        val uniqueInputs = recipeDisplay.ingredients.distinct().map { slotDisplay ->
            slotDisplay.getStacks(slotDisplayContext).map { slotDisplay to it }
        }
        recipeDisplay.ingredients.map { it.getStacks(slotDisplayContext) }

        Lists.cartesianProduct(uniqueInputs).forEachIndexed { index, inputList ->
            singlePlayerContext.server.runOnServer<RuntimeException> { server ->
                val player = server.playerManager.playerList.single()

                val world = player.world
                val targetPos = player.blockPos.up() + (player.facing.vector * 2)

                world.setBlockState(targetPos, Blocks.CRAFTING_TABLE.defaultState)
            }

            singlePlayerContext.clientWorld.waitForChunksRender()

            context.waitTick()

            logger.info { "Taking screenshots for shapeless recipe $recipeId" }

            context.input.pressMouse(1) // 1 is right click
            context.waitFor { client -> client.currentScreen is CraftingScreen }

            context.runOnClient<Throwable> { client ->
                val screen = client.currentScreen as CraftingScreen
                val handler = screen.screenHandler
                // handler.updateSlotStacks(handler.nextRevision(), inputList, ItemStack.EMPTY)
                recipeDisplay.ingredients.forEachIndexed { i, slot ->
                    val entry = inputList.find { it.first == slot } ?: error("Should never happen")
                    handler.inputSlots[i].stack = entry.second
                }
                handler.outputSlot.stack = result.stack
            }

            context.waitTick()

            val path = takeRecipeScreenshot(context, group, recipeId, null, "$index")

            // re-crop image
            val image = ImageIO.read(path.inputStream().buffered())
            val newImage = image.getSubimage(712, 220, 488, 280)
            ImageIO.write(newImage, "png", path.outputStream().buffered())

            context.input.pressKey(InputUtil.GLFW_KEY_ESCAPE)
            context.waitTick()

            singlePlayerContext.server.runOnServer<Throwable> { server ->
                val player = server.playerManager.playerList.single()
                val newPos = player.facing.rotateYClockwise().doubleVector + player.pos
                player.teleport(newPos.x, newPos.y, newPos.z, false)
            }
        }
    }

    private fun screenshotShapedRecipe(
        context: ClientGameTestContext,
        singlePlayerContext: TestSingleplayerContext,
        group: String?,
        recipe: ShapedRecipe,
        recipeId: Identifier,
    ) {
        val slotDisplayContext = singlePlayerContext.server.computeOnServer<_, Throwable> { server ->
            SlotDisplayContexts.createParameters(server.overworld)
        }

        val recipeDisplay = recipe.displays.single() as ShapedCraftingRecipeDisplay
        val result = recipeDisplay.result() as StackSlotDisplay
        val uniqueInputs = recipeDisplay.ingredients.distinct().map { slotDisplay ->
            slotDisplay.getStacks(slotDisplayContext).map { slotDisplay to it }
        }.filter { it.isNotEmpty() }

        logger.info { "Taking screenshots for shaped recipe $recipeId" }
        Lists.cartesianProduct(uniqueInputs).forEachIndexed { index, inputList ->
            singlePlayerContext.server.runOnServer<RuntimeException> { server ->
                val player = server.playerManager.playerList.single()

                val world = player.world
                val targetPos = player.blockPos.up() + (player.facing.vector * 2)

                world.setBlockState(targetPos, Blocks.CRAFTING_TABLE.defaultState)
            }

            singlePlayerContext.clientWorld.waitForChunksRender()

            context.waitTick()

            context.input.pressMouse(1) // 1 is right click
            context.waitFor { client -> client.currentScreen is CraftingScreen }

            context.runOnClient<Throwable> { client ->
                val screen = client.currentScreen as CraftingScreen
                val handler = screen.screenHandler

                var i = 0
                recipeDisplay.ingredients.forEach { slot ->
                    val entry = inputList.find { it.first == slot }
                    if (entry != null)
                        handler.inputSlots[i].stack = entry.second
                    do {
                        i++
                    } while (recipeDisplay.width <= i % 3)
                }
                handler.outputSlot.stack = result.stack
            }

            context.waitTick()

            val path = takeRecipeScreenshot(context, group, recipeId, null, "$index")

            // re-crop image
            val image = ImageIO.read(path.inputStream().buffered())
            val newImage = image.getSubimage(712, 220, 488, 280)
            ImageIO.write(newImage, "png", path.outputStream().buffered())

            context.input.pressKey(InputUtil.GLFW_KEY_ESCAPE)

            singlePlayerContext.server.runOnServer<Throwable> { server ->
                val player = server.playerManager.playerList.single()
                val newPos = player.facing.rotateYClockwise().doubleVector + player.pos
                player.teleport(newPos.x, newPos.y, newPos.z, false)
            }
        }
    }

    fun takeRecipeScreenshot(
        context: ClientGameTestContext,
        group: String?,
        recipe: Identifier,
        recipeInput: Identifier? = null,
        suffix: String? = null,
    ): Path {
        val recipePath = if (group != null) "recipes/$group" else "recipes/${recipe.path}"

        val name = buildString {
            append(recipe.toUnderscoreSeparatedString())

            if (recipeInput != null) {
                append('_')
                append(recipeInput.toUnderscoreSeparatedString())
            }

            if (suffix != null)
                append("_$suffix")
        }

        val option = TestScreenshotOptions.of(name)
            .disableCounterPrefix()
            .withDestinationDir(FabricLoader.getInstance().gameDir.resolve(recipePath))
            .withSize(1920, 1080)

        return context.takeScreenshot(option)
    }
}
