package com.exetacticals.client.model.modular;

import com.exetacticals.client.model.modular.state.IStateViewerModel;
import com.exetacticals.modularSys.modules.views.IStateView;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IBulletShellHandlerModel<T extends IStateView> extends IStateViewerModel<T> {
    IBulletShellHandler<?> getBulletShellHandler();
}
