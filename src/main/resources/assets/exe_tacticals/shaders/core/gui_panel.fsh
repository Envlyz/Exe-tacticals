#version 150

in vec2 localUV;
in vec4 vertexColor;

// copy of the frame behind the GUI, only sampled when BlurRadius > 0
uniform sampler2D Sampler0;
// size of the rectangle and corner radius, in GUI units
uniform vec2 Size;
uniform float Radius;
// > 0 draws only an outline of that width (GUI units) instead of a filled shape
uniform float StrokeWidth;
// > 0 blurs the frame behind the rectangle, radius in framebuffer pixels
uniform float BlurRadius;
// > 0 fades the edge out over that many GUI units inwards (soft shadows and glows)
uniform float Softness;

out vec4 fragColor;

const float TAU = 6.28318530718;
const float DIRECTIONS = 12.0;
const float RINGS = 4.0;

float roundedBoxSDF(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

vec3 blurBehind() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 uv = gl_FragCoord.xy * texel;
    vec3 sum = texture(Sampler0, uv).rgb;
    for (float d = 0.0; d < DIRECTIONS; d += 1.0) {
        float angle = d * TAU / DIRECTIONS;
        vec2 dir = vec2(cos(angle), sin(angle)) * BlurRadius * texel;
        for (float r = 1.0; r <= RINGS; r += 1.0) {
            sum += texture(Sampler0, uv + dir * (r / RINGS)).rgb;
        }
    }
    return sum / (DIRECTIONS * RINGS + 1.0);
}

void main() {
    vec2 halfSize = Size * 0.5;
    float radius = clamp(Radius, 0.0, min(halfSize.x, halfSize.y));
    float dist = roundedBoxSDF(localUV * Size - halfSize, halfSize, radius);

    // one physical pixel of antialiasing at any GUI scale
    float aa = max(fwidth(dist), 0.0001);
    float shape;
    if (Softness > 0.0) {
        float t = clamp(-dist / Softness, 0.0, 1.0);
        shape = t * t * (3.0 - 2.0 * t);
    } else {
        shape = 1.0 - smoothstep(-aa, 0.0, dist);
    }
    if (StrokeWidth > 0.0) {
        float band = max(StrokeWidth, aa);
        shape *= smoothstep(-band - aa, -band, dist);
    }
    if (shape <= 0.0) {
        discard;
    }

    if (BlurRadius > 0.0 && StrokeWidth <= 0.0) {
        // vertex alpha is how strongly the blurred frame is tinted
        fragColor = vec4(mix(blurBehind(), vertexColor.rgb, vertexColor.a), shape);
    } else {
        fragColor = vec4(vertexColor.rgb, vertexColor.a * shape);
    }
}
