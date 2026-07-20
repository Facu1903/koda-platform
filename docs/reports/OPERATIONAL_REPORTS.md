# Reportes operativos y dashboard

## Estado

Implementado en Sprint 2 Hito 7.

## Objetivo

El modulo de reportes operativos permite consultar indicadores simples de ventas, compras, caja y stock sin introducir contabilidad, fiscalidad ni BI avanzado.

Su funcion en Sprint 2 es dar visibilidad operativa al circuito comercial minimo ya construido: terceros, caja, ventas, compras y stock.

## Reglas funcionales implementadas

- Todos los reportes son tenant-scoped.
- Ningun endpoint acepta `tenant_id` desde frontend.
- Todos los endpoints requieren `commercial_reports:read`.
- Los reportes por rango requieren `from` y `to`.
- El rango se interpreta como `from` inclusivo y `to` exclusivo.
- El rango maximo permitido es de 366 dias.
- Todo listado tiene `limit` con maximo 500.
- El dashboard calcula el dia operativo con `time_zone` del tenant.
- Stock bajo usa parametro `threshold`; por defecto es `0`, hasta que se apruebe stock minimo por producto.
- Los totales de ventas/compras usan documentos `CONFIRMED`; los documentos `CANCELLED` se informan por separado para trazabilidad.

## API backend

Base path: `/api/v1/reports`.

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/api/v1/reports/sales?from={instant}&to={instant}&limit={n}` | Ventas confirmadas/anuladas por rango. |
| `GET` | `/api/v1/reports/purchases?from={instant}&to={instant}&limit={n}` | Compras confirmadas/anuladas por rango. |
| `GET` | `/api/v1/reports/cash-movements?from={instant}&to={instant}&limit={n}` | Movimientos de caja por rango. |
| `GET` | `/api/v1/reports/top-products-sold?from={instant}&to={instant}&limit={n}` | Productos mas vendidos por cantidad. |
| `GET` | `/api/v1/reports/low-stock?threshold={cantidad}&limit={n}` | Productos con stock igual o inferior al umbral. |
| `GET` | `/api/v1/reports/dashboard?lowStockThreshold={cantidad}&lowStockLimit={n}&stockMovementLimit={n}` | Dashboard operativo inicial. |

Los parametros `from` y `to` deben enviarse como instantes ISO-8601, por ejemplo `2026-07-20T00:00:00Z`.

## Dashboard

El dashboard devuelve:

- fecha operativa del tenant,
- periodo UTC equivalente del dia operativo,
- total y cantidad de ventas del dia,
- total y cantidad de compras del dia,
- caja abierta del usuario actual si existe,
- productos con stock bajo,
- ultimos movimientos de stock.

## Permisos

| Permiso | Uso |
| --- | --- |
| `commercial_reports:read` | Consultar reportes operativos y dashboard. |

Matriz aplicada para KODA:

- `TENANT_OWNER`, `TENANT_ADMIN`, `MANAGER`: lectura de reportes operativos.
- `SALES_USER`: lectura de reportes comerciales basicos.
- `STOCK_USER`: lectura de reportes relacionados a stock.
- `READ_ONLY`: lectura.

## Persistencia

Migracion:

- `V202607201400__enable_commercial_reports.sql`

La migracion agrega:

- modulo SaaS `COMMERCIAL_REPORTS`,
- permiso `commercial_reports:read`,
- matriz rol-permiso aprobada,
- indices para consultas por fecha en ventas, compras, caja y stock.

No se crean tablas de reportes en Sprint 2. Los reportes leen los datos operativos confirmados existentes.

## Tests

Validado con:

- `mvn -B test`: 86 pruebas unitarias, 0 fallos.
- `mvn -B verify`: 86 pruebas unitarias y 6 pruebas de integracion, 0 fallos.
- Flyway/Testcontainers PostgreSQL 17.10 hasta `v202607201400`.

## Fuera de alcance

- Reportes fiscales.
- Contabilidad.
- Libro IVA.
- Cuentas corrientes.
- Rentabilidad/margen real.
- Costeo promedio.
- Cubos OLAP.
- BI avanzado.
- Exportaciones Excel/PDF.
- Alertas automaticas.
- UI de dashboard.