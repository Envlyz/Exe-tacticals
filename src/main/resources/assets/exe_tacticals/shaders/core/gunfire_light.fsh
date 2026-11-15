#version 150

// Свет вспышек выстрелов поверх кадра: по глубине восстанавливается точка мира (относительно камеры),
// для неё считается вклад до MAX_LIGHTS точечных источников. Мир и освещение блоков не меняются.

#define MAX_LIGHTS 8

uniform sampler2D DepthSampler;
uniform bool IsFabulous;
uniform mat4 InvProjMat;
uniform mat4 InvViewRotMat;
uniform int LightCount;
// xyz — позиция источника относительно камеры, w — радиус
uniform vec4 LightPosRadius[MAX_LIGHTS];
// rgb — цвет * сила
uniform vec4 LightColor[MAX_LIGHTS];

in vec2 texCoord;
out vec4 fragColor;

float unpackDepth(vec3 p) {
    return p.x + p.y * 0.00392156862745 + p.z * 0.0000152590218967;
}

float depthAt(vec2 uv) {
    return IsFabulous ? unpackDepth(texture(DepthSampler, uv).rgb) : texture(DepthSampler, uv).r;
}

void main() {
    float depth = depthAt(texCoord);
    if (depth >= 1.0) {
        discard; // небо
    }
    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InvProjMat * ndc;
    view /= view.w;
    vec3 pos = (InvViewRotMat * vec4(view.xyz, 1.0)).xyz;

    float dist = length(pos);
    if (dist < 0.45) {
        discard; // руки/оружие у самой камеры освещаются своим шейдером
    }

    // нормаль поверхности из производных позиции; разворачиваем к камере
    vec3 normal = normalize(cross(dFdx(pos), dFdy(pos)));
    if (dot(normal, -pos) < 0.0) {
        normal = -normal;
    }

    vec3 light = vec3(0.0);
    for (int i = 0; i < MAX_LIGHTS; i++) {
        if (i >= LightCount) {
            break;
        }
        vec3 toLight = LightPosRadius[i].xyz - pos;
        float d = length(toLight);
        float radius = LightPosRadius[i].w;
        if (d >= radius) {
            continue;
        }
        float att = 1.0 - d / radius;
        att *= att;
        float ndl = max(dot(normal, toLight / max(d, 0.001)), 0.0);
        // немного «заворачиваем» свет, чтобы боковые грани не были совсем чёрными
        light += LightColor[i].rgb * att * (ndl * 0.8 + 0.2);
    }
    float strength = max(light.r, max(light.g, light.b));
    if (strength <= 0.003) {
        discard;
    }
    // смешивание SRC_ALPHA, ONE: прибавляем тёплый свет
    fragColor = vec4(light / strength, min(strength, 1.0));
}
