#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D gcrHeatMap;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
//0~1
uniform float gcrHeat;
// a > 0: draw a flat colour (silhouette for the outline post effect, see GunWorkbenchRenderer)
uniform vec4 gcrSolidColor;
// 1 — модель от первого лица (пушка, руки): свой свет, независимый от нормалей меша и света мира
uniform float gcrFirstPerson;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 muzzleLightContribution;
in vec3 gcrViewNormal;
in vec3 gcrViewPosition;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.05) {
        discard;
    }
    if (gcrSolidColor.a > 0.0) {
        fragColor = gcrSolidColor;
        return;
    }
    vec3 N = normalize(gcrViewNormal);
    vec4 shade = vertexColor;
    if (gcrFirstPerson > 0.5) {
        // Плоская нормаль грани из производных экранной позиции: всегда верная для кубических моделей
        // и всегда смотрит на камеру. Свет закреплён относительно камеры, формула как у ванильной руки
        // (ambient 0.4 + 0.6 * два источника): верх и лицевые грани полностью освещены.
        // Масштаб ×1000 — чтобы не терять точность на крошечных производных. На пикселях у ребра нормаль бывает
        // вырожденной или развёрнутой от камеры (тёмная «пунктирная» кромка), поэтому всегда разворачиваем
        // её к камере, а вырожденную заменяем на «лицом к камере».
        vec3 dn = cross(dFdx(gcrViewPosition) * 1000.0, dFdy(gcrViewPosition) * 1000.0);
        float dl = length(dn);
        N = (dl > 1e-6 && dl == dl) ? dn / dl : normalize(-gcrViewPosition);
        if (dot(N, -gcrViewPosition) < 0.0) N = -N;
        vec3 l0 = normalize(vec3(0.35, 0.85, 0.55));
        vec3 l1 = normalize(vec3(-0.55, 0.45, 0.70));
        float lit = min(1.0, (max(dot(N, l0), 0.0) + max(dot(N, l1), 0.0)) * 0.6 + 0.4);
        shade = vec4(vec3(lit), 1.0);
    }
    color *= shade * ColorModulator;
    color *= lightMapColor;
    color.rgb += muzzleLightContribution;

    // Camera-space studio-style key/fill for the GCR vanilla shader only.
    // The previous 8.5% specular multiplied by the already-dark albedo was
    // almost invisible. Controlled broad diffuse relief makes real polygon
    // faces readable; the small white highlight gives black metal definition.
    // The Oculus path has its own bounded mesh vertex-lighting fallback.
    vec3 V = normalize(-gcrViewPosition);
    vec3 keyDir = normalize(vec3(0.38, 0.76, 0.51));
    vec3 fillDir = normalize(vec3(-0.78, 0.37, 0.50));
    float keyDiffuse = max(0.0, dot(N, keyDir));
    float fillDiffuse = max(0.0, dot(N, fillDir));
    float faceRelief = 0.80 + 0.28 * keyDiffuse + 0.12 * fillDiffuse;
    color.rgb *= faceRelief;
    vec3 halfVector = normalize(keyDir + V);
    float specular = pow(max(dot(N, halfVector), 0.0), 28.0) * keyDiffuse;
    float fresnel = pow(1.0 - clamp(dot(N, V), 0.0, 1.0), 4.0);
    color.rgb += vec3(0.050) * specular + color.rgb * (0.035 * fresnel);

    float heat = gcrHeat * texture(gcrHeatMap, texCoord0).r;
    float heat2 = heat * heat;
    vec3 heatColor = vec3(
    heat,
    heat2 * 0.5,
    heat2 * 0.3);

    color.rgb += heatColor;

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}