# Sprint 4 - Hardening Report

## Estado

Completado en Hito 8. Pendiente de aprobacion funcional final por el Product Owner.

## Objetivo

Cerrar Sprint 4 reduciendo riesgos tecnicos reales antes de avanzar a nuevos frentes funcionales.

El foco fue validar y endurecer la primera capa SaaS operativa: trazabilidad HTTP, logs estructurados, health checks, metricas, performance, cache seguro de capabilities, auditoria operativa, migraciones PostgreSQL 17, arquitectura y documentacion.

No se modificaron reglas funcionales aprobadas de licenciamiento, permisos, stock, ventas, compras, caja, reportes, auditoria ni configuracion de empresa.

## Cambios realizados

### Configuracion de tests Java

Se configuro Mockito como Java agent explicito para Surefire y Failsafe.

Esto elimina la advertencia de carga dinamica de agente que Java endurecera en versiones futuras. Tambien se agrego `-Xshare:off` solo para JVM de tests para evitar ruido de class sharing asociado al agente.

Resultado:

- Tests unitarios Maven sin advertencia de self-attach de Mockito.
- Tests de integracion Maven con la misma configuracion.
- Sin cambios en runtime productivo.

### Validacion backend

Se ejecuto validacion completa con Maven:

- compilacion backend,
- tests unitarios,
- reglas de arquitectura,
- empaquetado jar,
- pruebas de integracion con Testcontainers,
- migraciones Flyway contra PostgreSQL 17 real.

Resultado:

- 141 tests unitarios, 0 fallos.
- 14 tests de integracion, 0 fallos.
- 23 migraciones validadas y aplicadas hasta `v202607220900`.

### Validacion frontend

Se mantiene la validacion completa del frontend:

- tests Vitest,
- lint ESLint,
- build productivo TypeScript/Vite.

El Sprint 4 no introdujo una nueva UI funcional. El frontend queda validado para asegurar que el shell existente sigue construyendo correctamente.

### Documentacion

Se agregan documentos de cierre:

- `docs/sprints/SPRINT_4_HARDENING_REPORT.md`
- `docs/sprints/SPRINT_4_CLOSURE_REPORT.md`

Tambien se actualizan README, roadmap, changelog y plan de ejecucion del Sprint 4.

## Validaciones ejecutadas

- `mvn -B "-Dtest=AuthServiceTest,KodaSchemaHealthIndicatorTest" test`: 8 tests, 0 fallos, sin advertencia de self-attach Mockito.
- `mvn -B verify`: 141 tests unitarios, 14 tests de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 23 migraciones hasta `v202607220900`.
- `npm.cmd run test`: 3 tests frontend, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.

## Riesgos abiertos

- No hay APM, Prometheus ni Grafana integrados; la aplicacion queda instrumentada, pero la exportacion externa depende de la estrategia de despliegue.
- No hay pruebas de carga formales; los indices y limites se justifican por queries reales, pero falta validar con volumen sintetico alto.
- No hay cache distribuida de capabilities; el cache actual es local por instancia, con TTL e invalidacion administrativa.
- No hay particionamiento fisico de `audit_events`; queda preparado y documentado, pero no aplicado sin volumen medido.
- No hay exportacion/archivo frio de auditoria.
- No hay Row Level Security en PostgreSQL; el aislamiento tenant sigue en capa aplicativa.
- No hay tests HTTP end-to-end autenticados con usuarios reales.
- HS256 sigue aceptado para etapa actual; antes de produccion multi-nodo se debe definir rotacion de llaves o RS256/JWKS.

## Decision

Hito 8 deja Sprint 4 tecnicamente listo para aprobacion funcional final del Product Owner.

La conclusion honesta: KODA PLATFORM ya no solo ejecuta operaciones SaaS; ahora empieza a poder explicar que pasa cuando algo falla, medir su comportamiento basico y proteger consultas que creceran con el uso real. Todavia no tiene observabilidad externa completa ni pruebas de carga, pero ya no opera a ciegas.
