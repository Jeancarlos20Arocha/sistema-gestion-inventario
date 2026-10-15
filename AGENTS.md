# AGENTS.md

## Idioma

- Responde **siempre en español**.
- Explica todo (razonamientos, resúmenes, pasos, mensajes al usuario) **en español**.
- Escribe los **comentarios del código en español**.

## Proyecto

- Sistema de Gestión de Inventario, POS y Usuarios (multi-módulo Maven).
- Módulos: `shared`, `backend`, `client-desktop`, `client-mobile`.
- Java 25 · Spring Boot 3.3.5 · JavaFX 21.

## Comandos

- Compilar e instalar todo (desde la raíz del proyecto):
  - `mvn clean install -U`
  - o con el wrapper: `.\mvnw.cmd clean install -U` (Windows) / `./mvnw clean install -U` (Linux/macOS)
- Validar solo los POMs: `mvn validate`
