package gay.solonovamax.betterrecipes.mixin;


import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {
    @Accessor
    int getLitTimeRemaining();

    @Accessor
    void setLitTimeRemaining(int litTimeRemaining);

    @Accessor
    int getLitTotalTime();

    @Accessor
    void setLitTotalTime(int litTotalTime);

    @Accessor
    int getCookingTimeSpent();

    @Accessor
    void setCookingTimeSpent(int cookingTimeSpent);

    @Accessor
    int getCookingTotalTime();

    @Accessor
    void setCookingTotalTime(int cookingTotalTime);
}
