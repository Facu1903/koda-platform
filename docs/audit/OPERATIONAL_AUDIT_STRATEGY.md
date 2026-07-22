# Estrategia Operativa de Auditoria

Implementado en Sprint 4 Hito 7.

## Objetivo

Preparar `audit_events` para operacion SaaS real sin convertir el endpoint de consulta en una herramienta historica ilimitada.

Auditoria crece siempre. Si se la trata como una tabla chica, la tabla se encarga de explicar el error en produccion, con intereses.

## Alcance

Este hito cubre:

- limites operativos de consulta,
- paginacion estable,
- indice base para lectura tenant-scoped,
- politica inicial de retencion candidata,
- preparacion para particionamiento futuro.

No cubre:

- exportacion masiva,
- purga automatica,
- particionamiento fisico,
- almacenamiento frio,
- auditoria global cross-tenant.

## Politica de Consulta

Endpoint operativo:

```http
GET /api/v1/audit/events
```

Reglas:

- El tenant se obtiene desde `TenantContext`.
- No se acepta `tenantId` por parametro.
- La respuesta no expone `tenant_id`.
- El limite maximo por request sigue siendo 500.
- La consulta ordena por `occurred_at DESC, id DESC`.
- La paginacion usa keyset con `beforeOccurredAt` y `beforeId`.
- No se usa `OFFSET`.
- El rango temporal explicito `from`/`to` no puede superar `koda.audit.query.max-range`.

Configuracion inicial:

```yaml
koda:
  audit:
    query:
      max-range: ${KODA_AUDIT_QUERY_MAX_RANGE:P90D}
```

## Paginacion Keyset

Para pedir la pagina siguiente, el cliente toma el ultimo evento recibido y envia:

- `beforeOccurredAt`: valor `occurredAt` del ultimo evento.
- `beforeId`: valor `id` del ultimo evento.

Condicion SQL:

```sql
AND (occurred_at < ? OR (occurred_at = ? AND id < ?))
ORDER BY occurred_at DESC, id DESC
LIMIT ?
```

Esto evita el costo creciente de `OFFSET` cuando el historial empieza a tener millones de filas.

## Indices

Indice agregado:

```sql
CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_occurred_at_id
    ON audit_events (tenant_id, occurred_at DESC, id DESC);
```

Uso esperado:

- ultimos eventos del tenant,
- navegacion historica por cursor,
- soporte estable al ordenamiento principal del endpoint.

Los indices especificos de actor, recurso y accion agregados en Sprint 4 Hito 5 se mantienen para filtros frecuentes.

## Retencion Candidata

Politica candidata para SaaS comercial:

- 12 meses online en `audit_events`.
- 5 a 10 anos en almacenamiento frio/exportable para clientes que lo requieran por contrato o regulacion.
- Retencion configurable por plan/licencia en un hito futuro.

Esta retencion no se aplica automaticamente en Sprint 4. Borrar auditoria sin exportacion, contratos y recuperacion definidos seria rapido, barato y torpe. Tres virtudes falsas.

## Particionamiento Futuro

Estrategia recomendada cuando el volumen lo justifique:

- particionamiento por rango mensual sobre `occurred_at`,
- indices locales equivalentes por particion,
- jobs de mantenimiento para crear particiones futuras,
- estrategia de archivo antes de separar o borrar particiones antiguas,
- pruebas de migracion sobre PostgreSQL real antes de produccion.

No particionar por tenant en la primera etapa. Con miles de tenants, eso puede multiplicar objetos de base sin necesidad. El patron inicial recomendado es tiempo primero, tenant como primer componente de los indices de consulta.

## Guardrails

- No agregar filtros libres sobre `metadata` sin indice y caso de uso aprobado.
- No exponer auditoria cross-tenant desde endpoint tenant-scoped.
- No usar `OFFSET` para historial.
- No subir el limite maximo de 500 sin pruebas de carga.
- No agregar tags metricos con `tenantId`, `userId`, `traceId` o IDs de recurso.

## Validacion

Pruebas agregadas o actualizadas:

- `AuditServiceTest`: rango maximo, cursor completo y propagacion de cursor al repositorio.
- `JdbcAuditRepositoryTest`: SQL keyset y orden estable.
- `FlywayPostgresqlIT`: existencia del indice `idx_audit_events_tenant_occurred_at_id`.

Comando de validacion del hito:

```powershell
mvn -B "-Dtest=AuditServiceTest,JdbcAuditRepositoryTest" test
mvn -B "-Dtest=NoUnitTests" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dit.test=FlywayPostgresqlIT" verify
mvn -B verify
```

Resultado:

- 11 pruebas unitarias enfocadas, 0 fallos.
- 12 pruebas de integracion Flyway/PostgreSQL 17, schema `v202607220900`, 0 fallos.
- Validacion completa backend: 141 pruebas unitarias y 14 pruebas de integracion, 0 fallos.
