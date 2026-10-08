shader_type spatial;
render_mode diffuse_toon, specular_disabled;

uniform vec4 base_color : source_color = vec4(0.35,0.65,0.90,1.0);
uniform vec4 rim_color : source_color = vec4(0.30,0.85,1.0,1.0);
uniform float rim_strength : hint_range(0.0,1.0) = 0.24;
uniform vec4 emission_color : source_color = vec4(0.0,0.0,0.0,0.0);
uniform float emission_strength : hint_range(0.0,4.0) = 0.0;

void fragment(){
    ALBEDO = base_color.rgb;
    EMISSION = emission_color.rgb;
    EMISSION_ENERGY = emission_strength;
}

void light(){
    float ndl = max(dot(NORMAL,LIGHT),0.0);
    float band = step(0.45,ndl);
    DIFFUSE_LIGHT += ALBEDO * LIGHT_COLOR * band;
    float rim = pow(1.0-max(dot(NORMAL,VIEW),0.0),2.4) * rim_strength;
    DIFFUSE_LIGHT += rim_color.rgb * rim;
}
