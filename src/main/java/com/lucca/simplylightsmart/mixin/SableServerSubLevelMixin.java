package com.lucca.simplylightsmart.mixin;

import com.lucca.simplylightsmart.smart.SableSplitLinkMigration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the brief Sable split metadata before the tracking system clears it. */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.ServerSubLevel", remap = false)
abstract class SableServerSubLevelMixin {
    @Inject(method = "setSplitFrom", at = @At("TAIL"), remap = false)
    private void simplyLightSmart$preserveSplitLinks(@Coerce Object containingSubLevel,
                                                     @Coerce Object originalPose,
                                                     CallbackInfo callback) {
        SableSplitLinkMigration.onSplit(this, containingSubLevel, originalPose);
    }
}
