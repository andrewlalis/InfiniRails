#version 330 core

in vec3 vertexPosition;
in vec3 vertexColor;
in vec3 vertexNormal;
in vec3 fragmentPosition;

uniform vec3 lightPosition;
uniform vec3 lightColor;
uniform vec3 cameraPosition;

out vec4 fragmentColor;

void main() {
    vec3 lightDirection = normalize(lightPosition - fragmentPosition);
    vec3 reflection = reflect(-lightDirection, vertexNormal);
    vec3 viewVector = normalize(cameraPosition - fragmentPosition);

    float diffuseSpecularRatio = 0.8;
    vec3 c = vertexColor;
    // Color blue below the water line.
    if (vertexPosition.y < 0) {
        c = vec3(0.0, 0.0, 0.8);
    }

    vec3 ambientComponent = vec3(0.1, 0.1, 0.1);
    vec3 diffuseComponent = max(dot(vertexNormal, lightDirection), 0.0) * lightColor * diffuseSpecularRatio;
    vec3 specularComponent = pow(max(0, dot(reflection, viewVector)), 10) * lightColor * (1 - diffuseSpecularRatio);

    fragmentColor = vec4((ambientComponent + diffuseComponent + specularComponent) * c, 1.0);
}
