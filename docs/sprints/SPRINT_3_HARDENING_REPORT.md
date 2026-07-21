# Sprint 3 - Hardening Report

## Estado

Completado en Hito 7.

## Objetivo

Cerrar Sprint 3 reduciendo riesgos tecnicos reales antes de avanzar a nuevos frentes.

El foco fue validar la fundacion SaaS comercial completa: modelo persistente de licencias, capabilities, guards backend por modulo, administracion interna de licencias, shell frontend condicionado por capabilities, migraciones PostgreSQL 17, arquitectura y documentacion.

No se modificaron reglas funcionales aprobadas de licencias, planes, suscripciones, entitlements, limites, feature flags ni matriz modulo-permiso.

## Cambios realizados

### Validacion backend

Se ejecuto validacion completa con Maven:

- compilacion backend,
- tests unitarios,
- reglas de arquitectura,
- empaquetado jar,
- pruebas de integracion con Testcontainers,
- migraciones Flyway contra PostgreSQL 17 real.

Resultado:

- 115 tests unitarios, 0 fallos.
- 11 tests de integracion, 0 fallos.
- 21 migraciones validadas y aplicadas hasta `v202607201600`.

### Validacion frontend

Se ejecuto validacion completa del frontend:

- tests Vitest,
- lint ESLint,
- build productivo TypeScript/Vite.

Resultado:

- 3 tests frontend, 0 fallos.
- lint sin errores.
- build productivo correcto.

### Estabilizacion de tests frontend

Se ajusto el test del capability shell para evitar una consulta por rol demasiado pesada sobre componentes Material UI.

El comportamiento validado no cambio:

- modulos habilitados aparecen en navegacion,
- modulos sin licencia activa no aparecen en navegacion,
- ruta directa a modulo deshabilitado queda bloqueada visualmente.

### Documentacion

Se agregaron documentos de cierre:

- `docs/sprints/SPRINT_3_HARDENING_REPORT.md`
- `docs/sprints/SPRINT_3_CLOSURE_REPORT.md`

Tambien se actualizaron README, roadmap, changelog, plan de ejecucion del Sprint 3 y documentacion de licenciamiento.

## Validaciones ejecutadas

- `mvn -B verify`: 115 tests unitarios, 11 tests de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 21 migraciones hasta `v202607201600`.
- `npm.cmd run test`: 3 tests frontend, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.

## Riesgos abiertos

- No hay cache distribuida ni invalidacion explicita de capabilities; hoy se recalculan desde base.
- No hay aplicacion runtime de limites cuantitativos como `MAX_USERS`, `MAX_BRANCHES`, `MAX_WAREHOUSES`, `MAX_PRODUCTS` o `AUDIT_RETENTION_DAYS`.
- No hay UI de administracion visual de licencias; existe API interna protegida.
- No hay billing real, precios comerciales finales ni self-service de upgrade/downgrade.
- No hay tests HTTP end-to-end autenticados.
- No hay Row Level Security en PostgreSQL; el aislamiento tenant sigue en capa aplicativa.
- Mockito emite advertencia por carga dinamica de Java agent; debe resolverse antes de endurecer compatibilidad futura de Java.
- HS256 sigue aceptado para etapa actual; antes de produccion multi-nodo se debe definir rotacion de llaves o RS256/JWKS.

## Decision

Hito 7 deja Sprint 3 tecnicamente listo para aprobacion funcional final del Product Owner.

La conclusion honesta: KODA PLATFORM ya no es solo un ERP multiempresa con modulos; ahora tiene una base SaaS comercial gobernable. Todavia no vende, no factura suscripciones y no mide uso para billing, pero ya puede decidir de forma centralizada que puede usar cada tenant.
