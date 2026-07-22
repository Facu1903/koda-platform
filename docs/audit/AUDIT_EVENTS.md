# Audit Events

## Objetivo

Definir la consulta inicial de eventos auditables para KODA PLATFORM durante Sprint 1 Hito 8.

Auditoria no es una papelera elegante de logs. Es una fuente controlada de trazabilidad operativa para responder: quien hizo que, cuando, sobre que recurso y con que resultado.

## Alcance aprobado

El Hito 8 expone consulta read-only de eventos auditables del tenant autenticado.

Endpoint:

```http
GET /api/v1/audit/events
```

Reglas:

- El tenant se obtiene exclusivamente desde `TenantContext`.
- No se acepta `tenantId` como parametro de entrada.
- La consulta requiere permiso `audit:read`.
- Los eventos se ordenan por fecha descendente y luego por identificador descendente.
- El limite maximo por request es 500 eventos.
- Los rangos temporales explicitos no pueden superar el maximo operativo configurado.
- La paginacion historica usa cursor keyset con `beforeOccurredAt` y `beforeId`.
- La respuesta no expone `tenant_id`.

## Filtros

| Parametro | Uso |
| --- | --- |
| `actorUserId` | Filtra por usuario actor. |
| `resourceType` | Filtra por tipo de recurso auditado. |
| `resourceId` | Filtra por identificador del recurso auditado. |
| `action` | Filtra por accion registrada. |
| `outcome` | Filtra por resultado `SUCCESS` o `FAILURE`. |
| `from` | Fecha/hora minima de ocurrencia. |
| `to` | Fecha/hora maxima de ocurrencia. |
| `limit` | Cantidad maxima de eventos a devolver. Maximo 500. |
| `beforeOccurredAt` | Cursor temporal para pedir eventos anteriores a la ultima fila recibida. |
| `beforeId` | Cursor de desempate para eventos con el mismo `occurredAt`. |

Validaciones:

- `from` no puede ser posterior a `to`.
- `outcome` solo acepta `SUCCESS` o `FAILURE`.
- `limit` no puede ser menor a 1.
- Si `limit` supera 500, el backend lo reduce a 500.
- Si se informa `beforeOccurredAt`, tambien debe informarse `beforeId`.
- Si se informa `beforeId`, tambien debe informarse `beforeOccurredAt`.
- El rango `from`/`to` explicito no puede superar `koda.audit.query.max-range`, por defecto `P90D`.

## Paginacion

La respuesta sigue siendo una lista plana para mantener compatibilidad.

Para pedir la siguiente pagina:

1. Tomar el ultimo evento recibido.
2. Enviar su `occurredAt` como `beforeOccurredAt`.
3. Enviar su `id` como `beforeId`.

El backend aplica la condicion keyset:

```sql
occurred_at < :beforeOccurredAt
OR (occurred_at = :beforeOccurredAt AND id < :beforeId)
```

No se usa `OFFSET`, porque degrada rapido con millones de registros.

## Matriz de permisos

| Rol | Acceso Hito 8 |
| --- | --- |
| `TENANT_OWNER` | Puede leer auditoria del tenant. |
| `TENANT_ADMIN` | Puede leer auditoria del tenant. |
| `MANAGER` | Puede leer auditoria del tenant. |
| `READ_ONLY` | No puede leer auditoria. |
| `SALES_USER` | No puede leer auditoria. |
| `STOCK_USER` | No puede leer auditoria. |
| `PLATFORM_SUPER_ADMIN` | Auditoria global queda para un hito futuro. |

## Respuesta

Cada evento devuelve:

- `id`
- `occurredAt`
- `actorUserId`
- `action`
- `resourceType`
- `resourceId`
- `outcome`
- `metadata`

## Errores esperados

| Caso | Resultado |
| --- | --- |
| Request anonimo | `401 Unauthorized` |
| Usuario sin `audit:read` | `PERMISSION_DENIED` |
| Tenant ausente en contexto | `TENANT_CONTEXT_REQUIRED` |
| Rango temporal invalido | Error funcional de request invalido |
| `outcome` invalido | Error funcional de request invalido |

## Fuera de alcance

No incluye:

- Crear eventos desde API publica.
- Editar eventos.
- Eliminar eventos.
- Auditoria global cross-tenant.
- Exportacion CSV/PDF/Excel.
- Particionamiento fisico de tabla.
- UI de auditoria.

Estas decisiones quedan separadas porque auditoria crece rapido. Si se mezcla consulta operativa, analitica historica y cumplimiento legal en el mismo hito, el diseno se convierte en sopa con credencial de arquitecto.
