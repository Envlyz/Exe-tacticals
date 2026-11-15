#version 150

// Полноэкранный прямоугольник: Position уже в координатах экрана (-1..1).
in vec3 Position;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}
