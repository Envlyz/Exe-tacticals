#version 150

// Круговое меню целиком: стеклянные лепестки (кольцевые сектора со скруглёнными углами и зазорами),
// подсветка выбранного (градиент к акценту + свечение + яркая кромка) и круглая кнопка в центре.
// Все размеры — в единицах GUI.

in vec2 localUV;

// копия кадра под меню (для стекла), читается при BlurRadius > 0
uniform sampler2D Sampler0;
// сторона квадрата, в котором рисуется колесо
uniform float Size;
// [радиус центральной кнопки, внутренний радиус лепестков, внешний радиус, скругление углов]
uniform vec4 Radii;
// зазор между лепестками
uniform float Gap;
uniform float Count;
// 0..1 — насколько подсвечен лепесток (анимируется на стороне Java)
uniform float Highlight[8];
// 1 — предпочтительный тип магазина (тонкая метка на кромке)
uniform float Favorite[8];
uniform float CenterHighlight;
uniform vec3 Accent;
// радиус блюра в пикселях кадра
uniform float BlurRadius;
// общая прозрачность (появление)
uniform float Opacity;

out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = 6.28318530718;

vec3 blurBehind() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 uv = gl_FragCoord.xy * texel;
    vec3 sum = texture(Sampler0, uv).rgb;
    float n = 1.0;
    for (float d = 0.0; d < 12.0; d += 1.0) {
        float a = d * TAU / 12.0;
        vec2 dir = vec2(cos(a), sin(a)) * BlurRadius * texel;
        for (float r = 1.0; r <= 4.0; r += 1.0) {
            sum += texture(Sampler0, uv + dir * (r / 4.0)).rgb;
            n += 1.0;
        }
    }
    return sum / n;
}

// скруглённое пересечение двух «внутренних расстояний» (положительны внутри): SDF, отрицательный внутри
float roundedInside(vec2 e, float c) {
    vec2 q = vec2(c) - e;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - c;
}

void main() {
    vec2 p = (localUV - 0.5) * Size;          // y вниз, как в GUI
    float r = length(p);
    float rc = Radii.x, rin = Radii.y, rout = Radii.z, corner = Radii.w;
    float aa = max(fwidth(r), 0.0001) * 1.0;

    // угол от верха по часовой стрелке (как в Java: x = sin, y = -cos)
    float ang = atan(p.x, -p.y);
    if (ang < 0.0) ang += TAU;

    int n = int(Count + 0.5);
    vec3 bg = BlurRadius > 0.0 ? blurBehind() : vec3(0.0);
    vec4 outColor = vec4(0.0);

    if (n > 0) {
        float seg = TAU / float(n);
        int idx = int(floor(mod(ang + seg * 0.5, TAU) / seg));
        idx = min(idx, n - 1);
        float theta = ang - float(idx) * seg;
        if (theta > PI) theta -= TAU;
        float h = seg * 0.5;

        // внутреннее расстояние до кромок: радиальные и боковые (зазор постоянной ширины)
        float radial = min(r - rin, rout - r);
        float side = (n == 1) ? 1e4 : r * sin(h - abs(theta)) - Gap * 0.5;
        if (n > 1 && abs(theta) > h) side = -1e4;
        float d = roundedInside(vec2(radial, side), corner);

        float hl = Highlight[idx];
        float fav = Favorite[idx];
        float t = clamp((r - rin) / (rout - rin), 0.0, 1.0);

        // стекло: затемнённый блюр + лёгкий радиальный градиент
        vec3 glass = mix(bg, vec3(0.02, 0.025, 0.03), 0.72);
        glass += vec3(0.03) * (1.0 - t);
        // подсветка: от тёмного у центра к акценту у края (≈ t²)
        vec3 lit = mix(vec3(0.0, 0.01, 0.01), Accent, pow(t, 1.8));
        vec3 fill = mix(glass, lit, hl * 0.92);

        float inside = 1.0 - smoothstep(-aa, aa, d);
        // тонкая светлая рамка у всех лепестков, у выбранного — цвета акцента
        float rim = (1.0 - smoothstep(0.0, 1.2, abs(d + 0.6))) * inside;
        vec3 rimColor = mix(vec3(1.0), Accent * 1.25, hl);
        float rimAlpha = mix(0.10, 0.85, hl);
        // яркая полоса по внешней кромке выбранного
        float outerBand = (1.0 - smoothstep(0.0, 2.5, rout - r)) * inside * hl;
        // метка предпочтительного типа — короткая полоса на внутренней кромке
        float favBand = (1.0 - smoothstep(0.0, 2.0, r - rin)) * inside * fav * (1.0 - smoothstep(h * 0.35, h * 0.45, abs(theta)));

        vec3 col = fill;
        col = mix(col, rimColor, rim * rimAlpha);
        col = mix(col, Accent * 1.35 + 0.15, outerBand * 0.9);
        col = mix(col, vec3(1.0, 0.82, 0.35), favBand * 0.9);
        float alpha = inside * mix(0.88, 0.97, hl);

        // свечение снаружи выбранного лепестка
        float glow = hl * exp(-max(d, 0.0) / 7.0) * step(0.0, d) * 0.45;
        outColor = vec4(col, alpha);
        if (glow > 0.0 && alpha < 1.0) {
            float ga = glow * (1.0 - alpha);
            outColor.rgb = (outColor.rgb * outColor.a + Accent * ga) / max(outColor.a + ga, 1e-4);
            outColor.a = outColor.a + ga;
        }
    }

    // центральная кнопка
    float dc = r - rc;
    float cin = 1.0 - smoothstep(-aa, aa, dc);
    if (cin > 0.0) {
        float tc = clamp(r / rc, 0.0, 1.0);
        vec3 glassC = mix(bg, vec3(0.02, 0.025, 0.03), 0.78);
        vec3 litC = mix(vec3(0.0, 0.01, 0.01), Accent, pow(tc, 1.8) * 0.85);
        vec3 colC = mix(glassC, litC, CenterHighlight * 0.9);
        float rimC = 1.0 - smoothstep(0.0, 1.2, abs(dc + 0.6));
        colC = mix(colC, mix(vec3(1.0), Accent * 1.25, CenterHighlight), rimC * mix(0.12, 0.85, CenterHighlight));
        outColor = vec4(colC, cin * 0.92);
    } else if (CenterHighlight > 0.0 && dc > 0.0 && r < Radii.y) {
        float g = CenterHighlight * exp(-dc / 6.0) * 0.4;
        outColor = vec4(Accent, max(outColor.a, g));
    }

    outColor.a *= Opacity;
    if (outColor.a <= 0.002) discard;
    fragColor = outColor;
}
