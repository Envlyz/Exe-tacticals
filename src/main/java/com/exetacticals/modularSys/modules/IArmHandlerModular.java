package com.exetacticals.modularSys.modules;

import com.exetacticals.modularSys.modules.gunProperties.IProperties;
import com.exetacticals.modularSys.modules.gunProperties.PropertiesDummies;
import com.exetacticals.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

public interface IArmHandlerModular {
    int DEFAULT_PRIORITY = 0;
    int HANDGUARD_PRIORITY = 1;
    int GRIP_PRIORITY = 2;
    int SUB_WEAPON_PRIORITY = 3;
    int NONE_PRIORITY = -1;

    int getPriority(boolean rightArm);

    @Nullable
    AdditionalPropModifier getModifier();

    class AdditionalPropModifier {
        private float recoilControlInc;
        private float stabilityInc;
        private float agilityInc;
        private float aimingSpeedInc;

        public AdditionalPropModifier(float recoilControlInc, float stabilityInc, float agilityInc, float aimingSpeedInc) {
            this.recoilControlInc = recoilControlInc;
            this.stabilityInc = stabilityInc;
            this.agilityInc = agilityInc;
            this.aimingSpeedInc = aimingSpeedInc;
        }

        public float getRecoilControlInc() {
            return recoilControlInc;
        }

        public float getAimingSpeedInc() {
            return aimingSpeedInc;
        }

        public float getStabilityInc() {
            return stabilityInc;
        }

        public float getAgilityInc() {
            return agilityInc;
        }


        /** Модификаторы «в руках» как полоски изменений. */
        public void appendStatRows(List<com.exetacticals.items.tooltip.StatRow> rows) {
            IProperties dummy = PropertiesDummies.getDummy(BaseProperties.class);
            if (!(dummy instanceof BaseProperties p)) {
                return;
            }
            addHolding(rows, p.recoilControl, recoilControlInc);
            addHolding(rows, p.stability, stabilityInc);
            addHolding(rows, p.agility, agilityInc);
            addHolding(rows, p.aimingSpeed, aimingSpeedInc);
        }

        private static void addHolding(List<com.exetacticals.items.tooltip.StatRow> rows,
                                       com.exetacticals.modularSys.modules.gunProperties.IProp prop, float inc) {
            if (Math.abs(inc) <= 1e-5f) {
                return;
            }
            rows.add(com.exetacticals.items.tooltip.StatRow.delta(prop.getKey(),
                    Component.translatable("tooltip.exe_tacticals.stat_holding", prop.getFullName()), inc * 100f));
        }

        public void appendHoverText(List<Component> tooltipComponents) {
            IProperties dummy = PropertiesDummies.getDummy(BaseProperties.class);
            if (dummy instanceof BaseProperties properties) {
                String recoilControlName = properties.recoilControl.getFullName();
                String stabilityName = properties.stability.getFullName();
                String agilityName = properties.agility.getFullName();
                String aimingSpeedName = properties.aimingSpeed.getFullName();

                String controlFmt = String.format("%.2f", recoilControlInc * 100);
                String stabilityFmt = String.format("%.2f", stabilityInc * 100);
                String agilityFmt = String.format("%.2f", agilityInc * 100);
                String aimingSpeedFmt = String.format("%.2f", aimingSpeedInc * 100);

                controlFmt = recoilControlInc > 0 ? "+" + controlFmt : controlFmt;
                stabilityFmt = stabilityInc > 0 ? "+" + stabilityFmt : stabilityFmt;
                agilityFmt = agilityInc > 0 ? "+" + agilityFmt : agilityFmt;
                aimingSpeedFmt = aimingSpeedInc > 0 ? "+" + aimingSpeedFmt : aimingSpeedFmt;

                String raw = Component.translatable("tooltip.prop.modifier_holding").getString();

                String controlMsg = raw.replace("$name", recoilControlName).replace("$amount", controlFmt);
                String stabilityMsg = raw.replace("$name", stabilityName).replace("$amount", stabilityFmt);
                String agilityMsg = raw.replace("$name", agilityName).replace("$amount", agilityFmt);
                String aimingSpeedMsg = raw.replace("$name", aimingSpeedName).replace("$amount", aimingSpeedFmt);

                tooltipComponents.add(Component.literal(controlMsg));
                tooltipComponents.add(Component.literal(stabilityMsg));
                tooltipComponents.add(Component.literal(agilityMsg));
                tooltipComponents.add(Component.literal(aimingSpeedMsg));
            }
        }
    }
}
