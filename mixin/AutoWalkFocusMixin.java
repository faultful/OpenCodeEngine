package dev.nyx.mixin;

import dev.nyx.module.modules.movement.AutoWalkModule;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public abstract class AutoWalkFocusMixin {
   @Inject(
      method = {"isWindowFocused"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void nyx$autoWalkKeepFocused(CallbackInfoReturnable<Boolean> cir) {
      AutoWalkModule var2 = AutoWalkModule.autoWalkModule;
      if (var2 != null && var2.isEnabled3()) {
         cir.setReturnValue(true);
      }
   }
}
