package com.exetacticals.client.model.modular.state;

import com.exetacticals.client.model.modular.IModularModel;
import com.exetacticals.modularSys.modules.views.IStateView;

public interface IStateViewerModel<T extends IStateView> extends IModularModel {
    IStateViewer<T> getViewer();

    default T getView() {
        return getViewer().getStateView();
    }

}
