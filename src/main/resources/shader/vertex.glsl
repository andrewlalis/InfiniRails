#version 330 core

layout (location = 0) in vec3 vertexPositionIn;
layout (location = 1) in vec3 vertexNormalIn;
layout (location = 2) in vec3 vertexColorIn;

uniform mat4 modelTransform;
uniform mat3 normalTransform;
uniform mat4 projectionTransform;
uniform mat4 viewTransform;

out vec3 vertexColor;
out vec3 vertexNormal;
out vec3 fragmentPosition;

void main() {
    gl_Position = projectionTransform * viewTransform * modelTransform * vec4(vertexPositionIn, 1.0);
    vertexColor = vertexColorIn;
    vertexNormal = normalize(normalTransform * vertexNormalIn);
    fragmentPosition = vec3(modelTransform * vec4(vertexPositionIn, 1.0));
}
