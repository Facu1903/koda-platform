# Compras basicas

## Estado

Implementado en Sprint 2 Hito 6.

## Objetivo

El modulo de compras basicas permite registrar compras internas tenant-scoped, confirmar compras con ingreso explicito de stock, registrar pago opcional contra caja y anular compras confirmadas con reversas trazables.

La compra no es comprobante fiscal completo en Sprint 2. No incluye impuestos avanzados, cuentas corrientes, recepcion parcial, costos promedio ni integracion contable.

## Reglas funcionales implementadas

- Cada compra pertenece a un tenant y una sucursal.
- El proveedor es obligatorio y debe existir como proveedor activo del mismo tenant.
- La numeracion interna es generada por backend y unica por tenant/sucursal.
- El numero de documento del proveedor es opcional y se guarda solo como referencia operativa.
- Estados: `DRAFT`, `CONFIRMED`, `CANCELLED`.
- Una compra `DRAFT` se puede actualizar o eliminar logicamente segun permisos.
- Una compra `CONFIRMED` no se edita.
- Una compra `CONFIRMED` se corrige mediante anulacion.
- La anulacion genera movimientos inversos de stock cuando corresponde.
- Si la compra tuvo pago registrado, la anulacion requiere una sesion de caja abierta para registrar reversa de pago.
- La moneda se toma de la moneda por defecto del tenant.

## Stock

Cada item referencia un producto del mismo tenant.

- Solo se permiten productos `GOOD`.
- Productos `SERVICE` se rechazan para compras basicas.
- Productos `GOOD` sin seguimiento de stock no generan movimiento de stock.
- Productos `GOOD` con `stock_tracking_enabled = true` requieren `warehouseId` en el item.
- El deposito debe pertenecer a la misma sucursal de la compra.
- Confirmar una compra genera movimiento `IN` con referencia `PURCHASE`.
- Anular una compra confirmada genera movimiento `OUT` con referencia `PURCHASE`.
- No se permite anular si la reversa dejaria stock negativo o romperia stock reservado.

La exigencia de `warehouseId` en productos stockeables mantiene la trazabilidad por deposito desde el primer dia. En una plataforma SaaS grande, inferir deposito por costumbre termina siendo deuda con traje.

## Caja

El pago es opcional en Hito 6.

- Si se confirma sin `cashSessionId` y `paymentMethod`, la compra queda `UNPAID`.
- Si se confirma con pago, deben informarse `cashSessionId` y `paymentMethod` juntos.
- El pago registrado cubre el total simple de la compra.
- El pago genera movimiento de caja `PURCHASE_PAYMENT` con referencia `PURCHASE`.
- Si el medio es `CASH`, `cash_effect` disminuye por el total.
- Si el medio no es efectivo, `cash_effect` queda en `0.0000`.
- Si se anula una compra pagada, se registra una reversa `PURCHASE_PAYMENT` en una sesion abierta autorizada.

## API backend

Base path: `/api/v1/purchases`.

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/api/v1/purchases` | Lista compras recientes del tenant. |
| `GET` | `/api/v1/purchases/{id}` | Consulta una compra por id. |
| `POST` | `/api/v1/purchases` | Crea compra `DRAFT`. |
| `PUT` | `/api/v1/purchases/{id}` | Actualiza compra `DRAFT`. |
| `DELETE` | `/api/v1/purchases/{id}?version={version}` | Elimina logicamente compra `DRAFT`. |
| `POST` | `/api/v1/purchases/{id}/confirm` | Confirma compra. |
| `POST` | `/api/v1/purchases/{id}/cancel` | Anula compra confirmada. |

Ningun endpoint acepta `tenant_id`. El tenant se resuelve desde JWT/Tenant Context.

## DTOs principales

Creacion/actualizacion:

- `branchId`: sucursal de la compra.
- `supplierId`: proveedor obligatorio.
- `supplierDocumentNumber`: documento del proveedor opcional.
- `items`: de 1 a 200 items.
- `productId`: producto comprado.
- `warehouseId`: deposito requerido si el producto controla stock.
- `quantity`: cantidad mayor a cero.
- `unitCost`: costo unitario mayor o igual a cero.

Confirmacion:

- `version`: version optimista esperada.
- `cashSessionId`: sesion de caja opcional.
- `paymentMethod`: medio de pago opcional; debe viajar junto con `cashSessionId`.

Anulacion:

- `version`: version optimista esperada.
- `reason`: motivo opcional.
- `cashSessionId`: sesion abierta requerida si la compra tenia pago registrado.

## Permisos

| Permiso | Uso |
| --- | --- |
| `purchases:read` | Consultar compras. |
| `purchases:create` | Crear compras draft. |
| `purchases:update` | Actualizar compras draft. |
| `purchases:delete` | Eliminar logicamente compras draft. |
| `purchases:confirm` | Confirmar compras. |
| `purchases:cancel` | Anular compras confirmadas. |

Matriz aplicada para KODA:

- `TENANT_OWNER`, `TENANT_ADMIN`, `MANAGER`: acceso completo a compras.
- `STOCK_USER`: lectura, creacion y actualizacion draft.
- `SALES_USER`: lectura.
- `READ_ONLY`: lectura.

Registrar pago durante confirmacion o anulacion requiere tambien `cash_movements:create`.

## Persistencia

Migraciones:

- `V202607201300__create_purchase_tables.sql`
- `V202607201310__seed_purchase_permissions.sql`

Tablas:

- `purchase_number_sequences`
- `purchase_orders`
- `purchase_order_items`

Modulo SaaS:

- `PURCHASES`

## Auditoria

Se auditan:

- `purchases.purchase.create`
- `purchases.purchase.update`
- `purchases.purchase.delete`
- `purchases.purchase.confirm`
- `purchases.purchase.cancel`

Los impactos integrados tambien auditan movimientos de stock/caja correspondientes.

## Errores API

El manejador global devuelve errores estructurados para:

- `PURCHASE_NOT_FOUND`
- `PURCHASE_REFERENCE_NOT_FOUND`
- `PURCHASE_VERSION_CONFLICT`
- `PURCHASE_OPERATION_REJECTED`
- `PERMISSION_DENIED`
- `TENANT_CONTEXT_REQUIRED`
- `INVALID_REQUEST`

## Tests

Validado con:

- `mvn -B test`: 79 pruebas unitarias, 0 fallos.
- `mvn -B verify`: 79 pruebas unitarias y 5 pruebas de integracion, 0 fallos.
- Flyway/Testcontainers PostgreSQL 17.10 hasta `v202607201310`.

## Fuera de alcance

- Facturacion fiscal de proveedor.
- Impuestos y percepciones.
- Cuentas corrientes de proveedores.
- Pagos parciales.
- Ordenes de compra formales.
- Recepcion parcial.
- Costeo promedio/FIFO/LIFO.
- Transferencias de stock.
- Lotes y vencimientos.
- UI de compras.