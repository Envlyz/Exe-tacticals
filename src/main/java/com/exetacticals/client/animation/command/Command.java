package com.exetacticals.client.animation.command;

import com.exetacticals.Utils;
import com.exetacticals.client.animation.AnimationDef;
import com.exetacticals.client.animation.IAnimated;
import com.exetacticals.client.animation.IAnimationSequence;
import com.exetacticals.client.render.ModuleRenderContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class Command {
    public String command;
    public List<String> args;
    public int tick;
    public float timeStamp;
    public AnimationDef def;

    public Command(String command, float timeStamp) {
        this.command = command;
        this.timeStamp = timeStamp;
        this.tick = Utils.secondToTick(timeStamp);
        this.resolve();
    }

    protected void resolve() {
        String raw = command.replace(";", "").trim();

        this.args = new ArrayList<>();

        int left = raw.indexOf('(');
        int right = raw.lastIndexOf(')');

        if (left == -1 || right == -1 || right < left) {
            this.command = raw;
            return;
        }

        this.command = raw.substring(0, left).trim();

        String argPart = raw.substring(left + 1, right).trim();
        if (argPart.isEmpty()) {
            return;
        }

        String[] split = argPart.split(",");
        for (String s : split) {
            args.add(s.trim());
        }
    }

    public void bindDef(AnimationDef def) {
        this.def = def;
    }

    public void onTick() {

    }

    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {

    }

    public void onRemove() {

    }
}
