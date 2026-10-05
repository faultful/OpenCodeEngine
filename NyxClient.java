package dev.nyx;

import cn.enaium.fabric.imgui.FabricImGui;
import dev.nyx.auth.AuthState;
import dev.nyx.imgui.ImGuiClickGui;
import dev.nyx.imgui.ImGuiHud;
import dev.nyx.imgui.LoginScreen;
import dev.nyx.module.Module;
import dev.nyx.module.ModuleManager;
import dev.nyx.config.Manager;
import dev.nyx.render.GlintTextureReplacer;
import dev.nyx.render.ListUtils;
import dev.nyx.storage.Chest$KindUtils;
import dev.nyx.util.PathUtils_2_3;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NyxClient implements ClientModInitializer {
   public static final String MOD_ID = "codeengine";
   public static final Logger LOGGER = LoggerFactory.getLogger("Code Engine");
   public static ModuleManager MODULES;
   private final Set<Integer> downKeys = new HashSet<>();

   public void onInitializeClient() {
      LOGGER.info("[c] initialising on Minecraft 1.21.11 (Fabric)");

      try {
         AuthState.run2();
      } catch (Throwable var5) {
         LOGGER.error("[Auth] boot failed", var5);
      }

      MODULES = new ModuleManager();
      MODULES.run();

      try {
         GlintTextureReplacer.init();
      } catch (Throwable var4) {
         LOGGER.error("[Glint] init failed", var4);
      }

      boolean[] var1 = new boolean[]{false};
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)var2x -> {
         if (!AuthState.isEnabled()) {
            if (!(var2x.currentScreen instanceof LoginScreen)) {
               try {
                  new LoginScreen(var2x.currentScreen);
               } catch (Throwable var4x) {
                  LOGGER.error("[Auth] setScreen(LoginScreen) failed", var4x);
               }
            }
         } else {
            if (!var1[0]) {
               var1[0] = true;

               try {
                  Manager.INSTANCE.load(MODULES);
               } catch (Throwable var5x) {
                  LOGGER.error("[c] ClientConfig.load failed on first tick — modules stay at ctor defaults", var5x);
               }
            }

            this.handleKeybinds(var2x);
            ImGuiClickGui.tick(var2x);
            if (var2x.player != null && var2x.world != null) {
               MODULES.run19();
            }
         }
      });
      HudRenderCallback.EVENT.register((HudRenderCallback)(var0, var1x) -> {
         if (AuthState.isEnabled()) {
            MinecraftClient var2x = MinecraftClient.getInstance();
            if (var2x == null || !(var2x.currentScreen instanceof LoginScreen)) {
               if (var2x != null && var2x.player != null) {
                  try {
                     FabricImGui.IMGUI.draw(io -> {
                        ImGuiHud.render();
                        ImGuiClickGui.render();
                     });
                  } catch (Throwable var4x) {
                     LOGGER.error("[ImGui] FabricImGui.draw threw", var4x);
                  }
               }
            }
         }
      });
      HudRenderCallback.EVENT.register((HudRenderCallback)(var0, var1x) -> {
         if (AuthState.isEnabled()) {
            if (MinecraftClient.getInstance().player != null) {
               float var2x = var1x == null ? 0.0F : var1x.getTickProgress(true);
               MODULES.run4(var0, var2x);
            }
         }
      });
      WorldRenderEvents.AFTER_ENTITIES.register(ListUtils::run15);
      AtomicReference var2 = new AtomicReference<>("");
      int[] var3 = new int[]{0};
      ClientPlayConnectionEvents.JOIN.register((Join)(var1x, var2x, var3x) -> var3x.execute(() -> {
         Chest$KindUtils.importEntries((java.util.Map)PathUtils_2_3.load(var3x));
         var2.set(PathUtils_2_3.serverKey(var3x));
      }));
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(var2x, var3x) -> {
         PathUtils_2_3.save(var3x, (java.util.Map)Chest$KindUtils.exportEntries());
         Chest$KindUtils.clear();
         var2.set("");
         var3[0] = 0;
         Manager.INSTANCE.save(MODULES);
      });
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)var2x -> {
         if (var2x.world != null) {
            String var3x = PathUtils_2_3.serverKey(var2x);
            String var4x = (String)var2.get();
            if (!var3x.equals(var4x)) {
               Chest$KindUtils.clear();
               Chest$KindUtils.importEntries((java.util.Map)PathUtils_2_3.load(var2x));
               var2.set(var3x);
               var3[0] = 0;
            } else {
               if (++var3[0] >= 1200) {
                  var3[0] = 0;
                  PathUtils_2_3.save(var2x, (java.util.Map)Chest$KindUtils.exportEntries());
                  if (Manager.INSTANCE.isDirty()) {
                     Manager.INSTANCE.save(MODULES);
                  }
               }
            }
         }
      });
      LOGGER.info("[c] initialised {} modules", MODULES.getList().size());
   }

   private void handleKeybinds(MinecraftClient var1) {
      if (var1.currentScreen != null) {
         this.downKeys.clear();
      } else {
         for (Module var3 : MODULES.getList()) {
            int var4 = var3.getInt();
            if (var4 != 0) {
               boolean var5 = InputUtil.isKeyPressed(var1.getWindow(), var4);
               if (var5 && !this.downKeys.contains(var4)) {
                  this.downKeys.add(var4);
                  MODULES.run6(var4);
               } else if (!var5) {
                  this.downKeys.remove(var4);
               }
            }
         }
      }
   }
}
