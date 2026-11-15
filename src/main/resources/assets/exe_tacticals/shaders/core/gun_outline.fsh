#version 150

// Белая обводка оружия на столе верстака (client/render/fx/GunOutline).
// Sampler0 — маска силуэта того же размера, что кадр (альфа 1 — оружие).
// Пиксель вне силуэта, у которого в радиусе OutlineWidth есть пиксель силуэта, красится в OutlineColor.
uniform sampler2D Sampler0;
uniform vec4 OutlineColor;
uniform float OutlineWidth;

out vec4 fragColor;

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    ivec2 size = textureSize(Sampler0, 0);
    if (texelFetch(Sampler0, p, 0).a > 0.5) {
        discard;
    }
    int r = int(ceil(OutlineWidth));
    float best = 1e9;
    for (int y = -r; y <= r; y++) {
        for (int x = -r; x <= r; x++) {
            ivec2 q = clamp(p + ivec2(x, y), ivec2(0), size - 1);
            if (texelFetch(Sampler0, q, 0).a > 0.5) {
                best = min(best, length(vec2(x, y)));
            }
        }
    }
    // сглаженный край: полностью внутри OutlineWidth, затухание на последнем пикселе
    float a = clamp(OutlineWidth + 0.5 - best, 0.0, 1.0);
    if (a <= 0.0) {
        discard;
    }
    fragColor = vec4(OutlineColor.rgb, OutlineColor.a * a);
}
