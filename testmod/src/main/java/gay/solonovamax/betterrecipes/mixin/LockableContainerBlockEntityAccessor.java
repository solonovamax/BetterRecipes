package gay.solonovamax.betterrecipes.mixin;


import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LockableContainerBlockEntity.class)
public interface LockableContainerBlockEntityAccessor {
    @Accessor
    Text getCustomName();

    @Accessor
    void setCustomName(Text customName);
}
