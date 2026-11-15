package com.exetacticals.client.recoil;

public record RecoilController(
        float linearZStiffness,
        float linearZDamping,

        float pitchStiffness,
        float pitchDamping,

        float localRotStiffness,
        float localRotDamping,

        float globalRotStiffness,
        float globalRotDamping,

        float rollStiffness,
        float rollDamping,

        float motionAdsModifierStiff,
        float motionAdsModifierDamp,

        float rotAdsModifierStiff,
        float rotAdsModifierDamp,

        float stableDuration
) {
}