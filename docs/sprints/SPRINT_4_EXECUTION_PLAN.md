# Sprint 4 - Execution Plan

## Estado

Definicion funcional aprobada por el Product Owner el 2026-07-21. Cierre tecnico completado en Hito 8; aprobacion funcional final pendiente del Product Owner.

## Objetivo

Implementar la primera capa de escalabilidad, observabilidad y endurecimiento SaaS operativo de KODA PLATFORM.

Sprint 4 debe convertir la plataforma en un sistema mas diagnosticable y preparado para operar con muchos tenants. No debe sumar complejidad decorativa ni herramientas externas obligatorias antes de tener senales internas confiables.

## Principio rector

Medir antes de optimizar. Trazar antes de diagnosticar. Endurecer antes de escalar.

La plataforma ya sabe que puede usar cada tenant. Ahora debe saber que esta pasando cuando miles de requests, operaciones y eventos empiecen a circular al mismo tiempo.

## Base funcional aprobada

La base funcional de Sprint 4 fue aprobada por el Product Owner el 2026-07-21 y queda documentada en `docs/sprints/SPRINT_4_FUNCTIONAL_BASELINE.md`.

## Alcance del sprint

- Correlation/request ID.
- Logs estructurados enriquecidos.
- Sanitizacion de datos sensibles en logs/errores.
- Health checks expandidos.
- Metricas base con Actuator/Micrometer.
- Revision de queries e indices criticos.
- Cache tecnico seguro para capabilities o decision documentada de alcance.
- Estrategia inicial de retencion/crecimiento de auditoria.
- Tests, documentacion y cierre.

## Hitos internos propuestos

| Hito | Estado | Resultado esperado |
| --- | --- | --- |
| 1. Base funcional Sprint 4 | Completado | Foco aprobado: Escalabilidad, Observabilidad y Endurecimiento SaaS Operativo. |
| 2. Correlation ID y logs estructurados | Completado | Request tracing basico, header `X-Correlation-ID`, MDC/log context y sanitizacion inicial. |
| 3. Health checks operativos | Completado | Liveness/readiness, DB health, Flyway/schema health y documentacion de diagnostico. |
| 4. Metricas base | Completado | Actuator/Micrometer expone metricas HTTP/runtime y lineamientos de cardinalidad. |
| 5. Performance e indices criticos | Completado | Revision documentada de queries de capabilities, guards, auditoria y reportes; indices aplicados donde corresponde. |
| 6. Cache seguro de capabilities | Completado | Cache local por tenant con TTL conservador, invalidacion administrativa y guards backend sin cache. |
| 7. Auditoria operativa | Completado | Politica operativa de consulta, rango maximo configurable, cursor keyset, indice dedicado y estrategia documentada de retencion/particionamiento futuro. |
| 8. Hardening Sprint 4 | Completado | Tests, validacion completa, documentacion final y reportes de cierre tecnico. Aprobacion funcional final pendiente del Product Owner. |

## Hito 1 completado

El Hito 1 define y aprueba la base funcional del Sprint 4:

- Documento `docs/sprints/SPRINT_4_FUNCTIONAL_BASELINE.md`.
- Foco aprobado: escalabilidad, observabilidad y endurecimiento SaaS operativo.
- Confirmacion de que Sprint 4 no suma nuevos modulos ERP ni UI completa.
- Criterios de aceptacion funcional.
- Reglas iniciales para correlation ID, logs, health checks, metricas, cache y auditoria.

Decision funcional: avanzar con Sprint 4 sin introducir proveedores externos obligatorios ni despliegue cloud productivo. Primero se instrumenta la aplicacion y se deja lista para integraciones futuras.

## Hito 2 completado

El Hito 2 implementa la primera capa operativa de trazabilidad HTTP:

- Filtro transversal `CorrelationIdFilter`.
- Header estandar `X-Correlation-ID` en request y response.
- Reutilizacion de correlation ID valido enviado por cliente.
- Generacion de UUID cuando falta o el valor recibido es inseguro.
- MDC con `correlationId`, metodo HTTP, path normalizado, status y duracion.
- Enriquecimiento del Tenant Context con `tenantId`, `userId` y `platformAdmin` en MDC.
- Limpieza obligatoria del contexto al finalizar cada request.
- Logs JSON con MDC habilitado mediante Logstash Encoder.
- Documento tecnico `docs/observability/CORRELATION_AND_LOGGING.md`.

Decision tecnica: el correlation ID vive en infraestructura compartida y no participa en autorizacion. El tenant sigue saliendo del principal autenticado; no se acepta `tenantId` desde headers ni parametros.

Validacion:

- `mvn -B '-Dtest=CorrelationIdFilterTest,TenantContextAuthenticationFilterTest' test`
- `mvn -B test`

## Hito 3 completado

El Hito 3 implementa health checks operativos separados:

- `/actuator/health/liveness` publico y sin detalles sensibles.
- `/actuator/health/readiness` publico y sin detalles sensibles.
- Readiness con `readinessState`, `db` y `kodaSchema`.
- `kodaSchema` como health indicator propio basado en Flyway.
- Estados `UP`, `OUT_OF_SERVICE` y `DOWN` para schema actual, migraciones pendientes o schema no disponible.
- Seguridad actualizada para permitir `/actuator/health/**` sin autenticar.
- Detalles ocultos con `show-details: never` y componentes visibles con `show-components: always`.
- Prueba de integracion con PostgreSQL 17 real mediante Testcontainers.
- Documento tecnico `docs/observability/HEALTH_CHECKS.md`.

Decision tecnica: no se expone un endpoint Actuator adicional ni se agregan dashboards externos. `kodaSchema` cubre la salud Flyway/schema porque Spring Boot no expone un componente health separado de Flyway en esta configuracion.

Validacion:

- `mvn -B '-Dtest=KodaSchemaHealthIndicatorTest,KodaPlatformApplicationTests' test`
- `mvn -B verify`

## Hito 4 completado

El Hito 4 implementa metricas operativas base con Actuator/Micrometer:

- `/actuator/metrics` expuesto pero protegido por autenticacion.
- `http.server.requests` disponible para requests HTTP con URI normalizada.
- Distribucion HTTP con histogramas, percentiles `p50`, `p95`, `p99` y buckets SLO.
- Tag comun `application` para identificar la aplicacion.
- Metricas JVM, proceso, sistema y datasource/pool disponibles segun runtime.
- Filtro `KodaMetricsConfiguration` para denegar tags sensibles o de alta cardinalidad.
- Limite de 100 valores distintos para el tag `uri` de `http.server.requests`.
- Documento tecnico `docs/observability/METRICS.md`.

Decision tecnica: no se incorporan Prometheus, Grafana ni APM comercial en este hito. Primero se deja la aplicacion correctamente instrumentada y protegida. La exportacion externa se decidira cuando exista una estrategia de despliegue.

Validacion:

- `mvn -B "-Dtest=KodaMetricsConfigurationTest,KodaPlatformApplicationTests" test`
- `mvn -B verify`

## Hito 5 completado

El Hito 5 revisa queries criticas e implementa indices respaldados por consultas existentes:

- Capabilities y guards de licencia.
- Feature flags efectivos.
- Auditoria tenant-scoped por actor, recurso y accion.
- Reportes comerciales de ventas/compras por rango confirmado.
- Dashboard de ultimos movimientos de stock.
- Listados operativos por `updated_at`.
- Sesion de caja abierta por tenant/usuario.

Se agrego la migracion `V202607211900__add_operational_performance_indexes.sql` y el documento tecnico `docs/database/PERFORMANCE_INDEX_REVIEW.md`.

Decision tecnica: no se agregaron indices para cada filtro posible. Se evito indexar `outcome` de auditoria por baja cardinalidad y se pospuso particionamiento hasta el hito especifico de auditoria operativa.

Validacion:

- `mvn -B "-Dtest=NoUnitTests" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dit.test=FlywayPostgresqlIT" verify`
- `mvn -B verify`

## Hito 6 completado

El Hito 6 implementa cache seguro para la resolucion de capabilities:

- Nuevo `TenantCapabilitiesResolver` para centralizar calculo y cache de capabilities.
- Cache local en memoria por tenant mediante `TenantCapabilitiesCache`.
- TTL configurable con default de 30 segundos.
- Limite configurable de 10000 tenants cacheados por instancia.
- Expiracion real acotada por el proximo `valid_until` efectivo conocido.
- Invalidacion explicita post-commit despues de actualizaciones exitosas de suscripcion, entitlement de producto y entitlement de modulo.
- Metrica `koda.capabilities.cache.requests` con tag estable `result`.
- Documento tecnico `docs/licensing/CAPABILITIES_CACHE.md`.

Decision tecnica: los guards backend de licencia no usan este cache en Sprint 4. Permanecen consultando la fuente autoritativa para evitar que una licencia suspendida siga permitiendo operaciones por una entrada cacheada vieja.

Validacion:

- `mvn -B "-Dtest=TenantCapabilitiesServiceTest,TenantLicenseAdministrationServiceTest,InMemoryTenantCapabilitiesCacheTest" test`
- `mvn -B verify`

## Hito 7 completado

El Hito 7 endurece la auditoria como capacidad operativa SaaS:

- Rango maximo configurable para consultas explicitas de auditoria mediante `koda.audit.query.max-range`.
- Default operativo inicial: `P90D`.
- Paginacion keyset con `beforeOccurredAt` y `beforeId`.
- SQL ordenado por `occurred_at DESC, id DESC` sin `OFFSET`.
- Nuevo indice `idx_audit_events_tenant_occurred_at_id` para consultas tenant-scoped recientes y paginacion estable.
- Documento tecnico `docs/audit/OPERATIONAL_AUDIT_STRATEGY.md`.

Decision tecnica: no se implementa particionamiento fisico ni purga automatica en Sprint 4. Se deja la estrategia definida y el endpoint protegido contra consultas demasiado amplias, pero la retencion efectiva requiere politicas comerciales, legales y de exportacion antes de borrar datos.

Validacion:

- `mvn -B "-Dtest=AuditServiceTest,JdbcAuditRepositoryTest" test`
- `mvn -B "-Dtest=NoUnitTests" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dit.test=FlywayPostgresqlIT" verify`
- `mvn -B verify`

## Hito 8 completado

El Hito 8 cierra tecnicamente Sprint 4:

- Configuracion de Mockito como Java agent explicito en Surefire y Failsafe.
- Eliminacion de la advertencia de self-attach dinamico de Mockito en tests.
- Validacion completa backend.
- Validacion completa frontend.
- Reporte tecnico `docs/sprints/SPRINT_4_HARDENING_REPORT.md`.
- Reporte de cierre `docs/sprints/SPRINT_4_CLOSURE_REPORT.md`.
- README, roadmap y changelog actualizados.

Decision tecnica: el cierre no incorpora proveedores externos de observabilidad ni pruebas de carga formales. Sprint 4 deja la aplicacion preparada e instrumentada; la exportacion externa y validacion con volumen alto quedan como decisiones de infraestructura/backlog.

Validacion:

- `mvn -B "-Dtest=AuthServiceTest,KodaSchemaHealthIndicatorTest" test`
- `mvn -B verify`
- `npm.cmd run test`
- `npm.cmd run lint`
- `npm.cmd run build`

## Orientacion tecnica inicial

### Correlation ID

Implementar un filtro reusable en backend:

- leer `X-Correlation-ID`,
- validar longitud/formato razonable,
- generar UUID si falta o es invalido,
- agregarlo a MDC/log context,
- devolverlo como header de respuesta,
- limpiar contexto al finalizar el request.

El correlation ID debe poder convivir con Tenant Context sin mezclarse con autorizacion.

### Logs estructurados

Extender el contexto de logs con:

- correlation ID,
- tenant ID cuando exista,
- user ID cuando exista,
- metodo HTTP,
- path normalizado,
- status,
- duracion.

No loguear tokens, passwords, secretos, cuerpos completos ni headers sensibles.

### Health checks

Revisar configuracion de Actuator:

- `/actuator/health/liveness`
- `/actuator/health/readiness`
- health de PostgreSQL,
- indicador de migraciones/schema,
- documentacion local de interpretacion.

Los detalles sensibles deben quedar restringidos.

### Metricas

Usar Micrometer/Actuator ya presente en Spring Boot.

Metricas candidatas:

- HTTP server requests,
- latencias por endpoint normalizado,
- conteo de errores por codigo,
- JVM/memoria/threads,
- datasource/pool si esta disponible,
- metricas de cache si se implementa cache.

Evitar etiquetas de alta cardinalidad como user ID o IDs dinamicos.

### Performance e indices

Priorizar consultas que escalan con tenants/registros:

- capabilities,
- guards de licencia,
- auditoria,
- reportes comerciales,
- busquedas por tenant y fecha.

Cada indice debe justificarse por consulta real, no por intuicion. Un indice gratis no existe; solo parece gratis hasta que escritura y mantenimiento pasan factura.

### Cache de capabilities

Opciones aceptables:

- cache por request,
- cache por tenant con TTL conservador,
- cache por tenant con invalidacion explicita tras administracion de licencias.

La decision final debe preservar seguridad: una licencia suspendida no debe seguir habilitando operaciones por culpa de cache viejo.

### Auditoria

Definir estrategia inicial:

- retencion candidata,
- indices por consulta,
- crecimiento estimado,
- preparacion para particionamiento futuro,
- limites operativos de consulta.

No implementar particionamiento sin validacion tecnica y necesidad clara.

## Criterios de calidad

- No exponer datos sensibles en logs, metricas ni health checks.
- No agregar dependencias externas obligatorias sin aprobacion.
- Mantener Clean Architecture.
- Mantener tests unitarios/integracion donde corresponda.
- Documentar decisiones tecnicas con riesgos y tradeoffs.
- Mantener compatibilidad con Docker Compose local.
- Actualizar README, changelog y docs en cada hito.

## Riesgos y decisiones a cuidar

- No crear metricas con cardinalidad explosiva.
- No convertir logs en un volcado de datos sensibles.
- No filtrar detalles internos por health checks publicos.
- No usar cache como fuente de verdad.
- No optimizar queries sin medir o sin justificar.
- No introducir proveedores externos antes de instrumentar bien la app.
- No confundir auditoria funcional con logs tecnicos.

## Fuera de alcance

- UI operativa completa.
- UI de observabilidad.
- Infraestructura cloud productiva.
- Kubernetes.
- Prometheus/Grafana productivos obligatorios.
- APM comercial obligatorio.
- Billing real.
- Nuevos modulos ERP.
- Row Level Security.

## Siguiente paso recomendado

Avanzar al Hito 7: auditoria operativa, estrategia de retencion, crecimiento e impacto futuro de particionamiento.
