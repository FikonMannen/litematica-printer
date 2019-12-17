package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow
    private ClientWorld world;

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == MinecraftClient.getInstance().world)
        {
            LitematicaRenderer.getInstance().loadRenderers();
        }
    }

    @Inject(method={"setupTerrain"}, at={@At(value="TAIL")})
    private void onPostSetupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator, CallbackInfo ci) {
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(frustum);
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", ordinal=0, target="Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDD)V")})
    private void renderLayerSolid(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        LitematicaRenderer.getInstance().piecewiseRenderSolid(matrices, tickDelta);
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", ordinal=1, target="Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDD)V")})
    private void renderLayerCutoutMipped(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(matrices, tickDelta);
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", ordinal=2, target="Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDD)V")})
    private void renderLayerCutout(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        LitematicaRenderer.getInstance().piecewiseRenderCutout(matrices, tickDelta);
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", ordinal=3, target="Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDD)V")})
    private void renderLayerTranslucent(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        LitematicaRenderer.getInstance().piecewiseRenderTranslucent(matrices, tickDelta);
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", ordinal=0, target="Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V")})
    private void onPostRenderEntities(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        LitematicaRenderer.getInstance().piecewiseRenderEntities(matrices, tickDelta);
    }

    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void onRenderWorldLast(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();
        if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert && !Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue()) {
            LitematicaRenderer.getInstance().renderSchematicWorld(matrices, matrix4f, tickDelta);
        }
    }
}
