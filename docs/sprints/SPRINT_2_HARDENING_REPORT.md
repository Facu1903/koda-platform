# Sprint 2 - Hardening Report

## Estado

Completado en Hito 8.

## Objetivo

Cerrar Sprint 2 reduciendo riesgos tecnicos reales antes de pasar a nuevos modulos.

El foco fue reforzar validaciones API, pruebas automatizadas, persistencia PostgreSQL real, documentacion y consistencia de cierre. No se modificaron reglas funcionales aprobadas de clientes, proveedores, caja, ventas, compras, stock ni auditoria.

## Cambios realizados

### Manejo global de errores

Se endurecio `ApiExceptionHandler` para devolver respuestas estructuradas `400` en errores frecuentes de parametros URL:

- parametro obligatorio ausente,
- parametro con tipo invalido,
- validacion de parametros de query/path.

Esto protege endpoints como reportes, stock, ventas y compras frente a requests mal formadas sin convertirlas en falsos `500`.

### Tests

Se agregaron tests unitarios para el manejador global de errores:

- `MISSING_REQUEST_PARAMETER`,
- `INVALID_REQUEST_PARAMETER`,
- `VALIDATION_ERROR` en parametros.

La suite backend queda en 89 tests unitarios y 6 tests de integracion.

### Persistencia

Se mantuvo validacion de Flyway con Testcontainers y PostgreSQL 17 real.

El esquema queda validado con 19 migraciones hasta `v202607201400`, incluyendo clientes/proveedores, caja, ventas, compras, reportes e indices operativos.

### Documentacion

Se agregaron documentos de cierre:

- `docs/sprints/SPRINT_2_HARDENING_REPORT.md`,
- `docs/sprints/SPRINT_2_CLOSURE_REPORT.md`.

Tambien se actualizaron README, roadmap, changelog y plan de ejecucion del Sprint 2.

## Validaciones ejecutadas

- `mvn -B test`: 89 tests unitarios, 0 fallos.
- `mvn -B verify`: 89 tests unitarios, 6 tests de integracion, 0 fallos.
- `npm.cmd run build`: build frontend productivo exitoso con TypeScript y Vite.

## Riesgos abiertos

- Los reportes usan un permiso unico `commercial_reports:read`; la separacion fina por rol (`SALES_USER` comercial basico, `STOCK_USER` stock-related) queda como decision funcional pendiente antes de ampliar reportes.
- No hay Row Level Security en PostgreSQL; el aislamiento tenant sigue en capa aplicativa.
- No hay UI funcional para Sprint 2; el incremento es backend/API.
- No hay tests end-to-end HTTP con autenticacion real todavia.
- No hay cuentas corrientes, impuestos, facturacion fiscal ni contabilidad.
- Mockito emite advertencia por carga dinamica de Java agent; debe resolverse antes de endurecer compatibilidad futura de Java.
- HS256 sigue aceptado para etapa actual; antes de produccion multi-nodo se debe definir rotacion de llaves o RS256/JWKS.

## Decision

Hito 8 deja Sprint 2 tecnicamente mas defendible y listo para cierre funcional del Product Owner.

La conclusion honesta: el circuito comercial minimo ya existe, pero todavia no es un ERP comercial vendible. Es una base backend seria para construir el siguiente salto sin improvisar.