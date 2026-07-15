# Sprint 1 - Execution Plan

## Estado

En progreso.

## Objetivo del Sprint 1

Construir el primer incremento ejecutable de KODA PLATFORM y KODA ERP con seguridad, multi-tenancy, configuracion inicial, catalogos, stock, auditoria y tests.

## Hitos internos

| Hito | Estado | Resultado esperado |
| --- | --- | --- |
| 1. Scaffolding tecnico | En progreso | Backend, frontend, Docker Compose y estructura modular base. |
| 2. PostgreSQL y Flyway | Pendiente | Base local, migraciones iniciales y convenciones de datos. |
| 3. Tenant context | Pendiente | Resolucion segura de tenant desde contexto autenticado. |
| 4. Seguridad JWT | Pendiente | Login, refresh tokens, usuarios, roles y permisos. |
| 5. Configuracion de empresa | Pendiente | Configuracion visual/regional por tenant. |
| 6. Catalogos ERP | Pendiente | Productos, marcas, categorias, unidades y presentaciones. |
| 7. Stock | Pendiente | Movimientos IN, OUT y ADJUSTMENT sin stock negativo por defecto. |
| 8. Auditoria | Pendiente | Registro persistente de operaciones sensibles. |
| 9. Tests y hardening | Pendiente | Cobertura minima y validacion de aislamiento multiempresa. |

## Hito 1 - Scaffolding tecnico

### Incluye

- Backend Spring Boot 3.5.16 con Java 21.
- Frontend React, TypeScript 6.0.3, Vite y Material UI.
- Docker Compose con PostgreSQL, backend y frontend.
- Estructura inicial por capas y modulos.
- Configuracion base de seguridad, validacion, errores, logs estructurados y Actuator.
- App shell inicial para KODA ERP.

### No incluye

- Reglas de negocio finales.
- Entidades de tenant, usuarios, productos o stock.
- JWT productivo.
- CRUDs funcionales.
- Persistencia de auditoria.

## Herramientas locales detectadas

- Git: disponible.
- Docker CLI: disponible.
- Node/npm: disponible usando `npm.cmd` en PowerShell.
- Java 21: no detectado en PATH.
- Maven: no detectado en PATH.
- Docker Desktop daemon: no disponible al momento de validar `docker compose build backend`.
- Frontend dependencies: `package-lock.json` generado con `npm install --package-lock-only`; instalacion completa de `node_modules` dentro de OneDrive supero el tiempo de espera.

## Riesgo actual

Sin Java 21 y Maven instalados localmente, la validacion backend fuera de Docker queda bloqueada. Docker podria construir el backend usando imagen Maven, pero Docker Desktop debe estar corriendo. Para desarrollo serio conviene instalar Java 21 y Maven en Windows.

## Siguiente decision tecnica

Antes de implementar Hito 2, instalar Java 21 y Maven localmente o aceptar que toda validacion backend se haga via Docker. La recomendacion tecnica es instalar ambos localmente: depender solo de Docker para compilar vuelve mas lenta la iteracion diaria.