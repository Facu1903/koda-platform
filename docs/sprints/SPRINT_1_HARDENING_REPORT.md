# Sprint 1 - Hardening Report

## Estado

Completado en Hito 9.

## Objetivo

Cerrar Sprint 1 reduciendo riesgos tecnicos reales antes de avanzar con mas negocio.

El foco fue reforzar arquitectura, seguridad JWT, aislamiento multiempresa y regresiones automatizadas. No se modificaron reglas funcionales aprobadas.

## Cambios realizados

### Arquitectura

Se agrego ArchUnit para validar reglas de Clean Architecture en la suite de tests:

- `application` no puede depender de `api` ni de `infrastructure`.
- `api` no puede depender de `infrastructure`.
- `domain` no puede depender de Spring, Jakarta Persistence ni Hibernate.

Tambien se corrigio una deuda detectada en seguridad: `AuthService` dependia de clases concretas de infraestructura. Ahora depende de puertos de aplicacion:

- `AccessTokenIssuer`
- `RefreshTokenService`
- `AuthTokenPolicy`

Los adaptadores concretos viven en `infrastructure`.

### Seguridad JWT

Se endurecio la configuracion JWT:

- `KODA_JWT_SECRET` sigue siendo obligatorio.
- El secreto HS256 debe tener al menos 32 bytes.
- `KODA_JWT_ISSUER` ahora se valida explicitamente al decodificar tokens entrantes.
- Se agregaron tests para secreto ausente, secreto corto, issuer correcto e issuer incorrecto.

### Aislamiento multiempresa

Se agregaron pruebas especificas para evitar fugas tenant-scoped:

- Catalogos: el listado de productos devuelve solo datos del tenant actual.
- Stock: el listado de saldos devuelve solo datos del tenant actual y respeta el limite maximo normalizado.

## Validaciones ejecutadas

- `mvn test`: 47 tests, 0 fallos.
- `mvn package`: build exitoso.
- Runtime temporal con jar contra PostgreSQL 17: Actuator `UP`.
- Flyway validado en `v202607171550`.
- Matriz rol-permiso validada con 73 asignaciones.
- `/api/v1/audit/events` sin token devuelve `401`.
- Imagen Docker backend reconstruida.
- Backend Docker reiniciado y validado en `http://localhost:8080`.

## Riesgos que siguen abiertos

- No hay Row Level Security en PostgreSQL. El aislamiento actual esta en capa aplicativa; es correcto para Sprint 1, pero no debe confundirse con defensa en profundidad final.
- No hay tests de repositorio con PostgreSQL/Testcontainers ejecutados por defecto.
- No hay CI/CD en GitHub Actions; las verificaciones todavia dependen de ejecucion local.
- El build Docker del backend sigue lento porque Maven descarga dependencias dentro de la imagen builder.
- Mockito emite advertencia por carga dinamica de Java agent; debe resolverse antes de endurecer compatibilidad futura de Java.
- HS256 queda aceptado para Sprint 1, pero antes de produccion multi-nodo deben definirse rotacion de llaves, RS256/JWKS o una politica operacional equivalente.

## Decision

Hito 9 deja Sprint 1 tecnicamente mas defendible, pero no significa que la plataforma este lista para produccion comercial.

La conclusion honesta: la base esta bien encaminada; el siguiente salto no es escribir mas CRUD, sino cerrar el ciclo profesional con GitHub, CI minimo, estrategia de usuarios administrativos y pruebas de persistencia real.