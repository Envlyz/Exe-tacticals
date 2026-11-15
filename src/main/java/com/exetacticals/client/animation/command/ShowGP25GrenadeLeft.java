package com.exetacticals.client.animation.command;

import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.animation.IAnimationSequence;
import com.exetacticals.client.render.ModuleRenderContext;
import com.exetacticals.modularSys.IModular;
import com.exetacticals.modularSys.builder.Unit;
import com.exetacticals.modularSys.modules.guns.IGun;
import com.exetacticals.modularSys.modules.guns.SlottedGunMainPart;
import com.exetacticals.modularSys.modules.impl.GP25;
import com.exetacticals.modularSys.modules.views.IGP25View;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

public class ShowGP25GrenadeLeft extends Command{
    private float coolDown;

    private long lastShow;
    private float startProgress;

    public ShowGP25GrenadeLeft(String command, float timeStamp) {
        super(command, timeStamp);
        if (args.size() != 1) {
            throw new IllegalArgumentException("Invalid command args: " + command);
        }
        this.coolDown = Float.parseFloat(args.get(0));
        this.coolDown = Math.max(coolDown, 0.1f);
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        startProgress = timeStamp / def.lengthInSeconds();
    }

    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        if (sequence.getCurrentAnimatingProgress() >= startProgress) {
            float timeDist = (System.currentTimeMillis() - lastShow) * 0.001f;
            if (timeDist > coolDown) {
                IGun gun = context.gun;
                if (gun instanceof SlottedGunMainPart mainPart) {
                    ListTag modulesTag = mainPart.getModulesTag(context.itemStack);
                    for (int i = 0; i < modulesTag.size(); i++) {
                        CompoundTag moduleTag = modulesTag.getCompound(i);
                        Unit unit = Unit.of(moduleTag);
                        if (unit == null) {
                            continue;
                        }
                        IModular module = unit.getModule();
                        if (module instanceof GP25 gp25) {
                            CompoundTag nodeStatesTag = mainPart.getNodeStatesTag(context.itemStack, moduleTag.getString(Unit.IN_TIME_ID));
                            String chamberStatus = gp25.getChamberStatus(nodeStatesTag);
                            if (IGP25View.CHAMBER_EMPTY.equals(chamberStatus)) {
                                Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("exe_tacticals.ani.gp25_empty"), false);
                            } else if (IGP25View.CHAMBER_LOADED.equals(chamberStatus)) {
                                Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("exe_tacticals.ani.gp25_loaded"), false);
                            }
                            break;
                        }
                    }
                }
                lastShow = System.currentTimeMillis();
            }
        }
    }
}
