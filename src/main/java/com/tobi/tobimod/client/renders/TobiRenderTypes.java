package com.tobi.tobimod.client.renderers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Render types used by Tobi Mod screens.
 *
 * <p>The radial menu emits an untextured triangle strip, which vanilla's
 * {@code RenderType.gui()} cannot draw correctly because it expects quads.
 */
public final class TobiRenderTypes extends RenderType {
    /** Flat translucent coloured triangle strip, used for radial menu slices. */
    public static final RenderType TRIANGLE_STRIP = create(
            "tobimod_triangle_strip",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLE_STRIP,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .createCompositeState(false)
    );

    /**
     * Never called. {@link RenderType} has no no-arg constructor, so this
     * subclass must declare one purely to satisfy the compiler; the class only
     * exists to expose the protected static {@code create} helper.
     */
    private TobiRenderTypes(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            Runnable setupState,
            Runnable clearState
    ) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}