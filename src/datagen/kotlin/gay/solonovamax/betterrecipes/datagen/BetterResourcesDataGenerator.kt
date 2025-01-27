package gay.solonovamax.betterrecipes.datagen

import gay.solonovamax.betterrecipes.datagen.recipe.BetterRecipesRecipeGenerator
import gay.solonovamax.betterrecipes.datagen.util.addProviders
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

@Suppress("unused")
object BetterResourcesDataGenerator : DataGeneratorEntrypoint {
    const val MOD_ID = "better-recipes"

    override fun onInitializeDataGenerator(generator: FabricDataGenerator) {
        val pack = generator.createPack()

        pack.addProviders(
            BetterRecipesRecipeGenerator.Provider::class,
        )
    }

    override fun getEffectiveModId() = MOD_ID
}