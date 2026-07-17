# Sprint 2 - Functional Baseline

## Estado

Aprobado por Product Owner el 2026-07-17.

## Objetivo

Definir las reglas funcionales minimas para construir la primera operacion comercial de KODA ERP sin que el codigo invente reglas de negocio.

Sprint 2 debe entregar un circuito comercial simple, auditable y tenant-scoped. No busca resolver facturacion fiscal, contabilidad completa ni precios avanzados.

## Principios de Sprint 2

- El backend sigue siendo la fuente de verdad de tenant, permisos y reglas.
- Ninguna operacion comercial acepta `tenant_id` desde frontend.
- Todo documento comercial importante debe tener estado.
- Todo documento confirmado debe ser trazable e inmutable en sus datos criticos.
- Toda correccion relevante debe hacerse mediante anulacion, movimiento inverso o documento nuevo, no editando historia.
- Toda operacion sensible debe registrar auditoria.
- El impacto en stock y caja debe ser explicito, no efecto secundario escondido.

## Modulos incluidos aprobados

- Clientes.
- Proveedores.
- Caja inicial.
- Ventas basicas.
- Compras basicas.
- Reportes operativos simples.
- Dashboard operativo inicial.

## Clientes

### Reglas aprobadas

- Cada cliente pertenece a un tenant.
- Campos obligatorios: nombre o razon social, estado.
- Campos opcionales: nombre comercial, tipo de documento, numero de documento, condicion fiscal, email, telefono, direccion, ciudad, provincia, pais, notas.
- Se crea un cliente seed `CONSUMIDOR_FINAL` por tenant para ventas sin cliente identificado.
- `CONSUMIDOR_FINAL` no se puede eliminar ni desactivar en Sprint 2.
- Si se informa tipo y numero de documento, la combinacion debe ser unica por tenant.
- Email y telefono no seran unicos en Sprint 2.
- La eliminacion sera soft delete.
- Si el cliente tiene ventas asociadas, solo se permite desactivar, no eliminar fisicamente.

### Estados

- `ACTIVE`
- `INACTIVE`

## Proveedores

### Reglas aprobadas

- Cada proveedor pertenece a un tenant.
- Campos obligatorios: nombre o razon social, estado.
- Campos opcionales: nombre comercial, tipo de documento, numero de documento, condicion fiscal, email, telefono, direccion, ciudad, provincia, pais, notas.
- No se crea proveedor generico por defecto.
- Si se informa tipo y numero de documento, la combinacion debe ser unica por tenant.
- Email y telefono no seran unicos en Sprint 2.
- La eliminacion sera soft delete.
- Si el proveedor tiene compras asociadas, solo se permite desactivar, no eliminar fisicamente.

### Estados

- `ACTIVE`
- `INACTIVE`

## Caja inicial

### Enfoque aprobado

Caja se modela como sesion operativa por tenant, sucursal y usuario.

Esto permite auditar quien abrio, quien registro movimientos y quien cerro. Es mas estricto que una caja global y evita el clasico agujero negro de dinero con interfaz bonita.

### Reglas aprobadas

- Una caja pertenece a tenant y sucursal.
- Una sesion de caja pertenece a una caja y a un usuario.
- Solo puede existir una sesion abierta por caja y usuario.
- Para registrar pagos de ventas o egresos de compras debe existir una sesion de caja abierta.
- La apertura registra saldo inicial informado por el usuario.
- El cierre registra saldo contado y diferencia.
- Una sesion cerrada no se puede reabrir.
- Correcciones posteriores se registran como nuevos movimientos autorizados.

### Estados de sesion

- `OPEN`
- `CLOSED`

### Tipos de movimiento

- `OPENING`
- `SALE_PAYMENT`
- `PURCHASE_PAYMENT`
- `CASH_IN`
- `CASH_OUT`
- `CLOSING_ADJUSTMENT`

### Medios de pago iniciales

- `CASH`
- `CARD`
- `BANK_TRANSFER`
- `OTHER`

## Ventas basicas

### Reglas aprobadas

- Cada venta pertenece a un tenant y sucursal.
- Cliente opcional; si no se informa cliente, se usa `CONSUMIDOR_FINAL`.
- Una venta tiene cabecera e items.
- Cada item referencia producto existente del mismo tenant.
- Se permite vender productos tipo `GOOD` y `SERVICE`.
- Productos `GOOD` con seguimiento de stock descuentan stock al confirmar la venta.
- Productos `SERVICE` no impactan stock.
- No se permite confirmar venta si deja stock negativo.
- Una venta confirmada no se edita.
- Para corregir una venta confirmada se debe anular y generar los movimientos inversos correspondientes.
- La anulacion de venta confirmada revierte stock si hubo descuento.
- Si la venta tuvo pago registrado, la anulacion debe registrar movimiento inverso o ajuste de caja.
- La venta no es factura fiscal en Sprint 2.
- La numeracion es interna, generada por backend, unica por tenant y sucursal.

### Estados

- `DRAFT`
- `CONFIRMED`
- `CANCELLED`

### Totales

- Sprint 2 usara totales simples: cantidad, precio unitario, subtotal y total.
- Impuestos, percepciones, descuentos avanzados y listas de precios quedan fuera de alcance.
- Moneda inicial: moneda por defecto del tenant, salvo aprobacion de multi-moneda posterior.

## Compras basicas

### Reglas aprobadas

- Cada compra pertenece a un tenant y sucursal.
- Proveedor obligatorio.
- Una compra tiene cabecera e items.
- Cada item referencia producto existente del mismo tenant.
- Se permite comprar productos tipo `GOOD`.
- Productos `GOOD` con seguimiento de stock ingresan stock al confirmar la compra.
- Una compra confirmada no se edita.
- Para corregir una compra confirmada se debe anular y generar movimientos inversos si corresponde.
- La anulacion de compra confirmada intenta revertir stock.
- Si no existe stock suficiente para revertir una compra anulada, la anulacion debe ser rechazada y requerir correccion operativa posterior.
- Si la compra tuvo pago registrado, la anulacion debe registrar movimiento inverso o ajuste de caja.
- La compra no es registracion contable completa en Sprint 2.
- La numeracion interna es generada por backend, unica por tenant y sucursal.
- Numero de comprobante de proveedor sera opcional en Sprint 2.

### Estados

- `DRAFT`
- `CONFIRMED`
- `CANCELLED`

## Reportes operativos simples

### Reportes aprobados

- Ventas por rango de fechas.
- Compras por rango de fechas.
- Movimientos de caja por rango de fechas.
- Top productos vendidos simple.
- Stock bajo simple usando saldos actuales.

### Reglas

- Todos los reportes son tenant-scoped.
- Todo reporte debe tener limite o rango de fechas obligatorio.
- No se incluyen reportes fiscales ni contables.

## Dashboard operativo inicial

### Indicadores aprobados

- Ventas del dia.
- Compras del dia.
- Caja abierta del usuario actual.
- Productos con stock bajo.
- Ultimos movimientos de stock.

## Matriz de permisos aprobada

| Recurso | TENANT_OWNER | TENANT_ADMIN | MANAGER | SALES_USER | STOCK_USER | READ_ONLY |
| --- | --- | --- | --- | --- | --- | --- |
| `customers` | CRUD | CRUD | CRUD | Read/Create/Update | Read | Read |
| `suppliers` | CRUD | CRUD | CRUD | Read | Read/Create/Update | Read |
| `cash_sessions` | CRUD | CRUD | Open/Close/Read | Open/Close own/Read own | No access | Read |
| `cash_movements` | CRUD | CRUD | Create/Read | Create own/Read own | No access | Read |
| `sales` | CRUD/Confirm/Cancel | CRUD/Confirm/Cancel | CRUD/Confirm/Cancel | Create/Update draft/Confirm/Read | Read | Read |
| `purchases` | CRUD/Confirm/Cancel | CRUD/Confirm/Cancel | CRUD/Confirm/Cancel | Read | Create/Update draft/Read | Read |
| `commercial_reports` | Read | Read | Read | Read own/basic | Read stock-related | Read |

Notas:

- `Cancel` queda reservado a `TENANT_OWNER`, `TENANT_ADMIN` y `MANAGER`.
- `READ_ONLY` nunca crea, confirma, cancela ni ajusta.
- Los permisos finales deben convertirse en permisos atomicos por recurso y accion antes de implementar.

## Auditoria aprobada

Deben auditarse como minimo:

- Creacion, actualizacion, desactivacion y eliminacion logica de clientes.
- Creacion, actualizacion, desactivacion y eliminacion logica de proveedores.
- Apertura y cierre de caja.
- Movimientos manuales de caja.
- Creacion, confirmacion y anulacion de ventas.
- Creacion, confirmacion y anulacion de compras.
- Rechazos por permisos insuficientes o reglas criticas cuando aplique.

## Fuera de alcance de Sprint 2

- Facturacion fiscal electronica.
- Integracion AFIP u otros organismos fiscales.
- Contabilidad general.
- Libro IVA, reportes fiscales o declaraciones.
- Cuentas corrientes avanzadas.
- Notas de credito/debito fiscales.
- Listas de precios multiples.
- Promociones.
- Impuestos avanzados.
- Multi-moneda operativa completa.
- Transferencias entre depositos.
- Reservas de stock.
- Lotes y vencimientos.
- Costeo promedio o valorizacion contable.
- POS offline.
- Mobile.

## Criterios de aceptacion funcional

Sprint 2 se considerara funcionalmente aceptable si:

- Se pueden crear clientes y proveedores tenant-scoped.
- Se puede abrir y cerrar caja inicial.
- Se puede registrar una venta confirmada que descuenta stock cuando corresponde.
- Se puede registrar una compra confirmada que ingresa stock cuando corresponde.
- Se pueden anular ventas y compras confirmadas con trazabilidad.
- Se respetan permisos por rol.
- Se auditan operaciones sensibles.
- No existe forma de acceder a datos de otro tenant desde API.
- Existen tests backend para permisos, tenant, estados, stock, caja y anulaciones.

## Decision

Se aprueba esta base funcional para iniciar la implementacion de Sprint 2.

Cualquier cambio sobre clientes, proveedores, caja, ventas, compras, permisos, auditoria, estados o impactos en stock/caja requiere aprobacion explicita del Product Owner antes de implementarse.