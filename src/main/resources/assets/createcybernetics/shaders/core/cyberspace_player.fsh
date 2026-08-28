#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 cyberspaceColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);

    if (color.a < 0.1) {
        discard;
    }

    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));

    float detail = smoothstep(0.05, 0.90, luminance);
    float intensity = 0.60 + luminance * 0.85;

    vec3 tintedColor = cyberspaceColor * intensity;

    float highlight = smoothstep(0.45, 0.95, luminance);
    vec3 brightColor = mix(tintedColor, vec3(1.0), highlight * 0.12);

    color.rgb = clamp(mix(tintedColor, brightColor, detail), 0.0, 1.0);

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}