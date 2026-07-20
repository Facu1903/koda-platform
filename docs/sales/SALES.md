# Ventas basicas

## Estado

Implementado en Sprint 2 Hito 5.

## Objetivo

El modulo de ventas basicas permite crear ventas internas tenant-scoped, confirmar ventas con impacto explicito en stock y caja, y anular ventas confirmadas con movimientos inversos trazables.

La venta no es factura fiscal en Sprint 2. No incluye impuestos avanzados, listas de precios, promociones, cuentas corrientes ni integracion fiscal.

## Reglas funcionales implementadas

- Cada venta pertenece a un tenant y una sucursal.
- Si no se informa cliente, se usa el cliente sistema `Consumidor Final`.
- La numeracion interna es generada por backend y unica por tenant/sucursal.
- Estados: `DRAFT`, `CONFIRMED`, `CANCELLED`.
- Una venta `DRAFT` se puede actualizar o eliminar logicamente segun permisos.
- Una venta `CONFIRMED` no se edita.
- Una venta `CONFIRMED` se corrige mediante anulacion.
- La anulacion genera movimientos inversos de stock cuando corresponde.
- Si la venta tuvo pago registrado, la anulacion requiere una sesion de caja abierta para registrar reversa de pago.
- La moneda se toma de la moneda por defecto del tenant.

## Stock

Cada item referencia un producto del mismo tenant.

- Productos `SERVICE` no impactan stock.
- Productos `GOOD` sin seguimiento de stock no generan movimiento de stock.
- Productos `GOOD` con `stock_tracking_enabled = true` requieren `warehouseId` en el item.
- El deposito debe pertenecer a la misma sucursal de la venta.
- Confirmar una venta genera movimiento `OUT` con referencia `SALE`.
- Anular una venta confirmada genera movimiento `IN` con referencia `SALE`.
- No se permite confirmar si el movimiento dejaria stock negativo o romperia stock reservado.

La exigencia de `warehouseId` en productos stockeables es intencional: el impacto de stock debe ser explicito, no inferido por magia.

## Caja

El pago es opcional en Hito 5.

- Si se confirma sin `cashSessionId` y `paymentMethod`, la venta queda `UNPAID`.
- Si se confirma con pago, deben informarse `cashSessionId` y `paymentMethod` juntos.
- El pago registrado siempre cubre el total simple de la venta.
- El pago genera movimiento de caja `SALE_PAYMENT` con referencia `SALE`.
- Si el medio es `CASH`, `cash_effect` aumenta por el total.
- Si el medio no es efectivo, `cash_effect` queda en `0.0000`.
- Si se anula una venta pagada, se registra una reversa `SALE_PAYMENT` en una sesion abierta autorizada.

## API backend

Base path: `/api/v1/sales`.

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/api/v1/sales` | Lista ventas recientes del tenant. |
| `GET` | `/api/v1/sales/{id}` | Consulta una venta por id. |
| `POST` | `/api/v1/sales` | Crea venta `DRAFT`. |
| `PUT` | `/api/v1/sales/{id}` | Actualiza venta `DRAFT`. |
| `DELETE` | `/api/v1/sales/{id}?version={version}` | Elimina logicamente venta `DRAFT`. |
| `POST` | `/api/v1/sales/{id}/confirm` | Confirma venta. |
| `POST` | `/api/v1/sales/{id}/cancel` | Anula venta confirmada. |

Ningun endpoint acepta `tenant_id`. El tenant se resuelve desde JWT/Tenant Context.

## Permisos

| Permiso | Uso |
| --- | --- |
| `sales:read` | Consultar ventas. |
| `sales:create` | Crear ventas draft. |
| `sales:update` | Actualizar ventas draft. |
| `sales:delete` | Eliminar logicamente ventas draft. |
| `sales:confirm` | Confirmar ventas. |
| `sales:cancel` | Anular ventas confirmadas. |

Matriz aplicada para KODA:

- `TENANT_OWNER`, `TENANT_ADMIN`, `MANAGER`: acceso completo a ventas.
- `SALES_USER`: lectura, creacion, actualizacion draft y confirmacion.
- `STOCK_USER`: lectura.
- `READ_ONLY`: lectura.

Registrar pago durante confirmacion o anulacion requiere tambien `cash_movements:create`.

## Persistencia

Migraciones:

- `V202607201200__create_sales_tables.sql`
- `V202607201210__seed_sales_permissions.sql`

Tablas:

- `sales_number_sequences`
- `sales_orders`
- `sales_order_items`

Modulo SaaS:

- `SALES`

## Auditoria

Se auditan:

- `sales.sale.create`
- `sales.sale.update`
- `sales.sale.delete`
- `sales.sale.confirm`
- `sales.sale.cancel`

Los impactos integrados tambien auditan movimientos de stock/caja correspondientes.

## Errores API

El manejador global devuelve errores estructurados para:

- `SALE_NOT_FOUND`
- `SALE_REFERENCE_NOT_FOUND`
- `SALE_VERSION_CONFLICT`
- `SALE_OPERATION_REJECTED`
- `PERMISSION_DENIED`
- `TENANT_CONTEXT_REQUIRED`
- `INVALID_REQUEST`

## Tests

Validado con:

- `mvn -B test`: 70 pruebas unitarias, 0 fallos.
- `mvn -B verify`: 70 pruebas unitarias y 4 pruebas de integracion, 0 fallos.
- Flyway/Testcontainers PostgreSQL 17.10 hasta `v202607201210`.

## Fuera de alcance

- Facturacion fiscal electronica.
- Impuestos y percepciones.
- Cuentas corrientes avanzadas.
- Pagos parciales.
- Descuentos/promociones/listas de precios.
- Notas de credito/debito fiscales.
- POS offline.
- UI de ventas.