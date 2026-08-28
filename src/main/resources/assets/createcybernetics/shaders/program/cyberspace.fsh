#version 150

uniform sampler2D DiffuseSampler;

uniform float PosterizeSteps;
uniform float Contrast;
uniform float ShadowGamma;
uniform float DarkTintStrength;
uniform float ScanlineStrength;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec3 color = original.rgb;

    color = pow(max(color, vec3(0.0)), vec3(ShadowGamma));

    color = (color - 0.5) * Contrast + 0.5;
    color = clamp(color, 0.0, 1.0);

    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float darkMask = 1.0 - smoothstep(0.10, 0.55, luminance);

    vec3 digitalTint = vec3(0.0, 0.035, 0.075);
    color += digitalTint * darkMask * DarkTintStrength;

    float steps = max(PosterizeSteps, 2.0);
    color = floor(color * steps + 0.5) / steps;

    float scanline = mod(floor(gl_FragCoord.y), 2.0);
    float scanMultiplier = 1.0 - scanline * ScanlineStrength;

    color *= scanMultiplier;

    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}