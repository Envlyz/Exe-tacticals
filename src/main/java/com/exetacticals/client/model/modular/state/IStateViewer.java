package com.exetacticals.client.model.modular.state;

import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.modules.views.IStateView;

import java.util.Optional;

public interface IStateViewer<T extends IStateView> {
    EmptyStateViewer EMPTY = new EmptyStateViewer();
    void onRegisterStateMapping();

    void addStateMapping(StaticState state);

    void addStateMappings(StaticState... state);

    void addStateMapping(String stateName, String animationPath, float scale, float progress);

    boolean removeStateMapping(String stateName);
    Optional<AnimationDef> getAnimationDef(String path);

    StaticState getState(String stateName);

    Optional<StaticState> createState(String animationPath, String stateName, float scale, float progress);

    T getStateView();

    void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states);

    class EmptyStateViewer implements IStateViewer<IStateView> {
        @Override
        public void onRegisterStateMapping() {
        }

        @Override
        public void addStateMapping(StaticState state) {
        }

        @Override
        public void addStateMappings(StaticState... state) {

        }

        @Override
        public void addStateMapping(String stateName, String animationPath, float scale, float progress) {
        }

        @Override
        public boolean removeStateMapping(String stateName) {
            return false;
        }

        @Override
        public Optional<AnimationDef> getAnimationDef(String path) {
            return Optional.empty();
        }

        @Override
        public StaticState getState(String stateName) {
            return StaticState.EMPTY;
        }

        @Override
        public Optional<StaticState> createState(String animationPath, String stateName, float scale, float progress) {
            return Optional.empty();
        }

        @Override
        public IStateView getStateView() {
            return null;
        }

        @Override
        public void applyState(IAnimated animated, ModuleRenderContext context, ReadOnlyTag states) {

        }
    }
}
