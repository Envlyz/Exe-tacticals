package com.exetacticals.client.render;

import com.exetacticals.Utils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SightViewRenderer {

    public static void renderScope(FirstPersonRenderContext context) {
        Utils.setUpStencil();

    }

    public static void renderScopePost(FirstPersonRenderContext context) {

    }

    public static void renderRedDot(FirstPersonRenderContext context) {

    }
}
