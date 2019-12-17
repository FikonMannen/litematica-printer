package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;

public class BufferBuilderCache {
    private final Map<RenderLayer, BufferBuilder> blockBufferBuilders = new HashMap<RenderLayer, BufferBuilder>();
    private BufferBuilder[] overlayBufferBuilders;

    public BufferBuilderCache() {
        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            this.blockBufferBuilders.put(layer, new BufferBuilder(layer.getExpectedBufferSize()));
        }
        this.overlayBufferBuilders = new BufferBuilder[ChunkRendererSchematicVbo.OverlayRenderType.values().length];
        for (int i = 0; i < this.overlayBufferBuilders.length; ++i) {
            this.overlayBufferBuilders[i] = new BufferBuilder(262144);
        }
    }

    public BufferBuilder getBlockBufferByLayer(RenderLayer layer) {
        return this.blockBufferBuilders.get((Object)layer);
    }

    public BufferBuilder getOverlayBuffer(ChunkRendererSchematicVbo.OverlayRenderType type) {
        return this.overlayBufferBuilders[type.ordinal()];
    }

    public void clear() {
        this.blockBufferBuilders.values().forEach(BufferBuilder::reset);
        for (BufferBuilder buffer : this.overlayBufferBuilders) {
            buffer.reset();
        }
    }
}