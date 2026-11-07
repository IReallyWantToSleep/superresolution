#version 460

layout(binding = 2) uniform sampler2D backdrop;

layout(std140, binding = 0) uniform frame {
    vec2 viewSize;
};

layout(std140, binding = 1) uniform glass {
    vec4 copyRect;
    vec2 textureSize;
    vec2 shapeSize;
    vec4 radii;
    vec4 colorControls;
    vec4 params;
    vec4 optics;
    vec4 lighting;
    vec4 shapeAxes;
    vec4 surfaceTint;
    vec4 material;
};

layout(location = 0) in vec2 ftcoord;
layout(location = 1) in vec2 fpos;
layout(location = 0) out vec4 outColor;

vec2 materialCorner(vec2 p) {
    vec2 halfSize = shapeSize * 0.5;
    float radius = p.y < halfSize.y
    ? (p.x < halfSize.x ? radii.x : radii.y)
    : (p.x < halfSize.x ? radii.w : radii.z);
    float limit = max(min(halfSize.x, halfSize.y), 0.5);
    radius = clamp(radius, 0.5, limit);
    float extent = radius * (1.0 - inversesqrt(2.0)) / (1.0 - pow(2.0, -1.0 / 2.5));
    if (extent <= limit) {
        return vec2(extent, 2.5);
    }
    float exponent = clamp(1.0 / -log2(1.0 - radius * (1.0 - inversesqrt(2.0)) / limit),
            2.0, 2.0 + 3.3333333);
    return vec2(limit, exponent);
}

vec3 sampleBackdrop(vec2 p) {
    vec2 local = p - ftcoord;
    vec2 screenPosition = fpos + shapeAxes.xy * local.x + shapeAxes.zw * local.y;
    vec2 scale = vec2(params.z);
    vec2 size = ceil(textureSize / scale);
    vec2 uv = screenPosition / scale / size;
    if (params.z > 1.0) {
        vec2 position = clamp(screenPosition / scale, vec2(0.5), size - vec2(0.5));
        uv = position / ceil(textureSize / 2.0);
    }
    if (params.w > 0.5) {
        uv.y = 1.0 - uv.y;
    }
    vec3 color = texture(backdrop, uv).rgb;
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luma), color, colorControls.z);
    return (color - vec3(0.5)) * colorControls.y + vec3(0.5 + colorControls.x);
}

const vec4 shape1 = vec4(0.0);
const vec2 corner1 = vec2(0.0);
const float smoothing = 0.0;
const float morphDispersion = 0.0;
const float progress = 1.0;
const float shadowAlpha = 0.22;
const float blackFade = 0.0;

const float kEpsilon = 0.0001;
const float kUnionDepth = 0.25;
const float kMaxSlope = 11.430052;
const float kEdgeOffset = 0.7;
const float kEdgeSoftness = 0.6;
const float kEdgeReach = 1.0;

float roundRect(vec2 p, vec4 s, vec2 c, out vec2 g) {
    vec2 sgn = vec2(p.x < s.x ? -1.0 : 1.0, p.y < s.y ? -1.0 : 1.0);
    vec2 q = abs(p - s.xy) - s.zw + c.x;
    if (q.x > 0.0 && q.y > 0.0) {
        vec2 m = q / c.x;
        vec2 mp = pow(m, vec2(c.y - 1.0));
        g = normalize(mp) * sgn;
        return (pow(mp.x * m.x + mp.y * m.y, 1.0 / c.y) - 1.0) * c.x;
    }
    g = q.x > q.y ? vec2(sgn.x, 0.0) : vec2(0.0, sgn.y);
    return max(q.x, q.y) - c.x;
}

float scene(vec2 p, out vec2 g) {
    vec2 g0;
    vec4 shape0 = vec4(shapeSize * 0.5, shapeSize * 0.5);
    float d0 = roundRect(p, shape0, materialCorner(p), g0);
    if (shape1.z <= 0.0) {
        g = g0;
        return d0;
    }
    vec2 g1;
    float d1 = roundRect(p, shape1, corner1, g1);
    float h = smoothing > kEpsilon ? clamp(1.0 - abs(d0 - d1) / smoothing, 0.0, 1.0) : 0.0;
    if (h <= kEpsilon) {
        g = d0 <= d1 ? g0 : g1;
        return min(d0, d1);
    }
    float w = clamp(2.0 * kUnionDepth * h, 0.0, 1.0);
    float left = d0 <= d1 ? 1.0 - w : w;
    vec2 blended = g0 * left + g1 * (1.0 - left);
    float len = length(blended);
    g = len > kEpsilon ? blended / len : vec2(0.0, -1.0);
    return min(d0, d1) - smoothing * kUnionDepth * h * h;
}

vec3 background(vec2 c) {
    return vec3(sampleBackdrop(c));
}

float underlay(vec2 p, float d, vec2 g, float coverage) {
    vec3 stroke = vec3(0.65 * params.y, 0.25, 0.2 * params.y);
    vec4 shadow = vec4(0.0, 2.0 * params.y, 7.0 * params.y, -params.y);
    float strokeA = 0.0;
    if (stroke.y > 0.0) {
        float outset = max(stroke.x * g.x * g.x + stroke.z * max(g.y, 0.0), 0.0);
        strokeA = clamp((1.0 - smoothstep(outset - 0.5, outset + 0.5, d)) * smoothstep(-0.5, 0.5, d), 0.0, 1.0)
        * stroke.y * coverage;
    }
    float shadowA = 0.0;
    if (shadowAlpha > 0.0) {
        vec2 unused;
        float ds = scene(p - shadow.xy, unused) - shadow.w;
        float r = max(shadow.z, 0.5);
        shadowA = (1.0 - smoothstep(-r, r, ds)) * shadowAlpha * coverage;
    }
    return strokeA + shadowA * (1.0 - strokeA);
}

vec4 shade(vec2 p, float d, vec2 g, float t, float pulse, float coverage) {
    float bezelWidth = lighting.w;
    float thickness = optics.x;
    float glassIor = params.x;
    float displacementFactor = optics.y;
    float dispersion = optics.z;
    vec4 tint = surfaceTint;
    float legibility = material.x;
    float opacity = colorControls.w;
    vec2 lightDir = normalize(vec2(dot(shapeAxes.xy, lighting.xy), dot(shapeAxes.zw, lighting.xy)));
    vec4 specular = vec4(0.75, 0.38, lighting.z, 0.9);
    vec2 specularShape = vec2(1.5 * params.y, 3.0);
    float fill = 1.0 - smoothstep(kEdgeOffset - kEdgeSoftness, kEdgeOffset + kEdgeSoftness, d);
    float under = fill < 1.0 ? underlay(p, d, g, coverage) : 0.0;
    if (fill <= 0.0) {
        return vec4(0.0, 0.0, 0.0, float(under * opacity));
    }

    float inward = max(-d, 0.0);
    float band = clamp(inward / max(bezelWidth, 1.0), 0.0, 1.0);
    float u = 1.0 - band;
    float u2 = u * u;
    float slope = inward <= bezelWidth
    ? min(2.0 * u2 * u / sqrt(max(1.0 - u2 * u2, kEpsilon)), kMaxSlope)
    : 0.0;
    vec3 normal = normalize(vec3(g * slope, 1.0));

    vec3 ray = refract(vec3(0.0, 0.0, -1.0), normal, 1.0 / max(glassIor, 1.0001));
    vec2 shift = ray.z < 0.0
    ? ray.xy / -ray.z * (thickness + inward * 0.5) * displacementFactor
    : vec2(0.0);
    shift *= t + 7.0 * t * (1.0 - t);

    float spread = dispersion + morphDispersion * pulse;
    vec3 glass;
    if (dot(shift, shift) < 0.0025) {
        glass = background(p);
    } else if (spread < 0.001) {
        glass = background(p + shift);
    } else {
        glass = vec3(
                background(p + shift * (1.0 + spread)).r,
                background(p + shift).g,
                background(p + shift * (1.0 - spread)).b);
    }
    float luma = dot(glass, vec3(0.2126, 0.7152, 0.0722));
    float tintAmount = min(tint.a + legibility * smoothstep(0.35, 0.95, luma), 1.0);
    glass = mix(glass, tint.rgb, tintAmount * coverage);
    if (blackFade > 0.0) {
        vec4 shape0 = vec4(shapeSize * 0.5, shapeSize * 0.5);
        float top = shape0.y - shape0.w;
        float height = clamp((p.y - top) / max(shape0.w * 2.0, 1.0), 0.0, 1.0);
        glass = mix(glass, vec3(0.0), blackFade * (1.0 - smoothstep(0.15, 0.85, height)));
    }

    float rim = 1.0 - smoothstep(0.0, max(specularShape.x, 0.25), abs(d));
    if (rim > 0.0) {
        float falloff = specular.w * band * band;
        float lit = pow(max(dot(g, lightDir), 0.0), specularShape.y) * max(specular.x - falloff, 0.0);
        float opposite = pow(max(dot(g, -lightDir), 0.0), specularShape.y) * max(specular.y - falloff, 0.0);
        glass += clamp((lit + opposite) * rim, 0.0, 1.0) * specular.z * min(coverage + pulse * 0.85, 1.75);
    }

    float alpha = fill + under * (1.0 - fill);
    return vec4(vec3(glass * fill * opacity), float(alpha * opacity));
}

vec4 liquidGlass(vec2 p) {
    vec3 stroke = vec3(0.65 * params.y, 0.25, 0.2 * params.y);
    float bezelWidth = lighting.w;
    float displacementFactor = optics.y;
    float t = clamp(progress, 0.0, 1.0);
    if (t <= 0.0) {
        return vec4(0.0);
    }
    float pulse = 4.0 * t * (1.0 - t);
    float coverage = smoothstep(0.0, 0.55, t);

    vec2 g;
    float d = scene(p, g);
    float outer = max(stroke.x + stroke.z, kEdgeOffset + kEdgeSoftness) + kEdgeReach;
    float rimReach = params.z > 1.0
    ? 1.0
    : clamp(bezelWidth * 0.08 * displacementFactor, 1.0, bezelWidth);
    bool edge = d >= -rimReach && d <= outer;
    float samples = edge ? 4.0 : 1.0;
    vec2 offset = edge ? vec2(0.125, 0.375) : vec2(0.0);
    vec4 sum = vec4(0.0);
    for (int i = 0; i < 4; i++) {
        if (float(i) >= samples) {
            break;
        }
        vec2 q = p + offset;
        float dq = d;
        if (edge) {
            dq = scene(q, g);
        }
        sum += shade(q, dq, g, t, pulse, coverage);
        offset = vec2(-offset.y, offset.x);
    }
    return sum / float(samples);
}

void main() {
    outColor = liquidGlass(ftcoord);
}
