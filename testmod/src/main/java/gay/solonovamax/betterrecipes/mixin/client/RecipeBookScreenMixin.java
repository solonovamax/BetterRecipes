package gay.solonovamax.betterrecipes.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookScreen.class)
public class RecipeBookScreenMixin {
    @Inject(method = "addRecipeBook", at = @At("HEAD"), cancellable = true)
    void neverAddRecipeBook(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/recipebook/RecipeBookWidget;isOpen()Z"
            )
    )
    boolean recipeBookAlwaysClosed(RecipeBookWidget<?> instance) {
        return false;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/recipebook/RecipeBookWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
            )
    )
    void neverRenderRecipeBook(RecipeBookWidget<?> instance, DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/recipebook/RecipeBookWidget;drawTooltip(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/screen/slot/Slot;)V"
            )
    )
    void neverRenderRecipeBookTooltip(RecipeBookWidget<?> instance, DrawContext context, int x, int y, Slot slot) {
    }
}
