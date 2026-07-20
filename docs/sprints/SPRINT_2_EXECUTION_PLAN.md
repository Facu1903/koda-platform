# Sprint 2 - Execution Plan

## Estado

Cerrado y aprobado funcionalmente por el Product Owner el 2026-07-20.

## Objetivo

Construir la primera base de operacion comercial de KODA ERP, manteniendo arquitectura SaaS multiempresa, trazabilidad, permisos y compatibilidad futura.

Sprint 2 no debe intentar hacer un sistema contable/fiscal completo. El objetivo correcto es crear el circuito comercial operativo minimo: clientes, proveedores, caja inicial, ventas basicas, compras basicas y reportes simples.

## Principio rector

El codigo de negocio de Sprint 2 debe respetar la base funcional aprobada en `docs/sprints/SPRINT_2_FUNCTIONAL_BASELINE.md`.

Si se codifica ventas o compras sin definir numeracion, estados, impacto en stock, caja, permisos y anulaciones, el sistema empezara a improvisar reglas. Esa improvisacion despues se convierte en deuda con perfume caro.

## Alcance candidato

- Clientes.
- Proveedores.
- Caja inicial.
- Ventas basicas.
- Compras basicas.
- Reportes operativos simples.
- Dashboard operativo inicial.
- Endurecimiento de persistencia y CI/CD minimo.

## Hitos internos propuestos

| Hito | Estado | Resultado esperado |
| --- | --- | --- |
| 1. Base funcional Sprint 2 | Completado | Reglas aprobadas para clientes, proveedores, caja, ventas y compras. |
| 2. CI/CD minimo y tests de persistencia | Completado | GitHub Actions para backend/frontend y prueba Flyway con PostgreSQL 17/Testcontainers. |
| 3. Clientes y proveedores | Completado | CRUD backend tenant-scoped con permisos, auditoria, validaciones y seed Consumidor Final. |
| 4. Caja inicial | Completado | Apertura, cierre, movimientos manuales, permisos, auditoria y migraciones tenant-scoped. |
| 5. Ventas basicas | Completado | Venta draft, confirmacion, anulacion, numeracion interna e impacto explicito en stock/caja. |
| 6. Compras basicas | Completado | Compra draft, confirmacion, anulacion, numeracion interna e impacto explicito en stock/caja. |
| 7. Reportes y dashboard operativo | Completado | API read-only con reportes por rango, stock bajo, top vendidos y dashboard inicial. |
| 8. Hardening Sprint 2 | Completado | Manejo API endurecido, pruebas, documentacion y cierre tecnico del sprint. |

## Base funcional aprobada

La base funcional fue aprobada por el Product Owner el 2026-07-17 y esta documentada en `docs/sprints/SPRINT_2_FUNCTIONAL_BASELINE.md`.

## Decisiones funcionales aprobadas

### Clientes

- Datos obligatorios.
- Identificacion fiscal: obligatoria u opcional.
- Unicidad por documento/email/telefono.
- Manejo de cliente generico o consumidor final.
- Baja logica y restricciones si tiene ventas.

### Proveedores

- Datos obligatorios.
- Identificacion fiscal.
- Unicidad.
- Baja logica y restricciones si tiene compras.

### Caja

- Si existira caja por sucursal, usuario o ambas.
- Si requiere apertura/cierre diario.
- Tipos de movimiento permitidos.
- Medios de pago iniciales.
- Relacion entre venta y caja.

### Ventas

- Estados iniciales: borrador, confirmada, anulada u otros.
- Si una venta confirmada descuenta stock inmediatamente.
- Si se permite venta sin stock.
- Como se corrige o anula una venta.
- Numeracion interna.
- Alcance fiscal: fuera de Sprint 2 salvo aprobacion explicita.

### Compras

- Estados iniciales.
- Si una compra confirmada ingresa stock inmediatamente.
- Como se corrige o anula una compra.
- Numeracion interna.
- Relacion con proveedor y caja.

### Permisos

- Matriz rol-permiso para clientes.
- Matriz rol-permiso para proveedores.
- Matriz rol-permiso para caja.
- Matriz rol-permiso para ventas.
- Matriz rol-permiso para compras.
- Acceso read-only y manager por modulo.

## Criterios de calidad

- Toda API tenant-scoped debe resolver tenant desde contexto autenticado.
- No se aceptan `tenant_id` libres desde frontend.
- Toda operacion sensible debe auditarse.
- Todo listado debe tener limite maximo.
- Toda escritura importante debe tener tests de permisos, tenant y caso de error.
- Las reglas de negocio deben vivir en application/domain, no en controladores.
- Cada hito debe actualizar README, changelog y documentacion especifica.

## Hito 2 completado

El Hito 2 agrego una primera barrera automatica de calidad antes de sumar mas negocio:

- Workflow GitHub Actions en `.github/workflows/ci.yml`.
- Backend validado con `mvn -B verify`.
- Maven Failsafe configurado para pruebas de integracion.
- Prueba `FlywayPostgresqlIT` con PostgreSQL 17 real mediante Testcontainers.
- Frontend validado con build TypeScript/Vite.
- Documentacion especifica en `docs/ci/GITHUB_ACTIONS.md`.

## Hito 3 completado

El Hito 3 implemento la base backend de clientes y proveedores sin duplicar modelo innecesariamente:

- Tablas `business_partners` y `business_partner_roles`.
- APIs `/api/v1/customers` y `/api/v1/suppliers`.
- Permisos atomicos `customers:*` y `suppliers:*`.
- Matriz rol-permiso aprobada aplicada por Flyway para KODA.
- Cliente sistema `Consumidor Final` protegido contra baja y desactivacion.
- Auditoria de creacion, actualizacion y eliminacion logica de roles comerciales.
- Tests unitarios de permisos, tenant, versionado, sistema protegido, reutilizacion por documento y auditoria.
- Testcontainers PostgreSQL 17 validando 12 migraciones hasta `v202607201010`.
- Documentacion especifica en `docs/commercial/COMMERCIAL_PARTNERS.md`.

## Hito 4 completado

El Hito 4 implemento caja inicial como ledger operativo tenant-scoped:

- Tablas `cash_registers`, `cash_sessions` y `cash_movements`.
- API `/api/v1/cash` para cajas, sesiones y movimientos.
- Apertura con saldo inicial y cierre con saldo esperado, contado y diferencia.
- Movimientos manuales `CASH_IN` y `CASH_OUT`.
- Movimiento `OPENING` automatico al abrir y `CLOSING_ADJUSTMENT` automatico cuando el cierre tiene diferencia.
- Permisos atomicos `cash_registers:*`, `cash_sessions:*` y `cash_movements:*`.
- Matriz rol-permiso aprobada aplicada por Flyway para KODA.
- Caja seed `CAJA_PRINCIPAL`.
- Auditoria de apertura, cierre y movimientos manuales.
- Tests unitarios de permisos, restricciones de usuario, medios no efectivos, cierre y ajustes.
- Testcontainers PostgreSQL 17 validando 14 migraciones hasta `v202607201110`.
- Documentacion especifica en `docs/cash/CASH_SESSIONS.md`.


## Hito 5 completado

El Hito 5 implemento ventas basicas como documento comercial tenant-scoped:

- Tablas `sales_number_sequences`, `sales_orders` y `sales_order_items`.
- API `/api/v1/sales` para crear, listar, consultar, actualizar, eliminar draft, confirmar y anular ventas.
- Numeracion interna unica por tenant y sucursal.
- Estados `DRAFT`, `CONFIRMED` y `CANCELLED`.
- Cliente por defecto `Consumidor Final` cuando no se informa cliente.
- Confirmacion con descuento de stock para productos `GOOD` con seguimiento de stock.
- Items stockeables con `warehouseId` obligatorio para mantener impacto explicito.
- Pago opcional contra sesion de caja abierta mediante movimiento `SALE_PAYMENT`.
- Anulacion con movimiento inverso de stock y reversa de pago cuando corresponde.
- Permisos atomicos `sales:read/create/update/delete/confirm/cancel`.
- Matriz rol-permiso aprobada aplicada por Flyway para KODA.
- Auditoria de creacion, actualizacion, eliminacion draft, confirmacion y anulacion.
- Tests unitarios de permisos, totales, cliente default, deposito requerido, confirmacion, caja, stock y anulacion.
- Testcontainers PostgreSQL 17 validando 16 migraciones hasta `v202607201210`.
- Documentacion especifica en `docs/sales/SALES.md`.

## Hito 6 completado

El Hito 6 implemento compras basicas como documento comercial tenant-scoped:

- Tablas `purchase_number_sequences`, `purchase_orders` y `purchase_order_items`.
- API `/api/v1/purchases` para crear, listar, consultar, actualizar, eliminar draft, confirmar y anular compras.
- Numeracion interna unica por tenant y sucursal.
- Estados `DRAFT`, `CONFIRMED` y `CANCELLED`.
- Proveedor obligatorio y documento de proveedor opcional.
- Confirmacion con ingreso de stock para productos `GOOD` con seguimiento de stock.
- Items stockeables con `warehouseId` obligatorio para mantener impacto explicito.
- Pago opcional contra sesion de caja abierta mediante movimiento `PURCHASE_PAYMENT`.
- Anulacion con movimiento inverso de stock y reversa de pago cuando corresponde.
- Permisos atomicos `purchases:read/create/update/delete/confirm/cancel`.
- Matriz rol-permiso aprobada aplicada por Flyway para KODA.
- Auditoria de creacion, actualizacion, eliminacion draft, confirmacion y anulacion.
- Tests unitarios de permisos, totales, proveedor obligatorio, producto comprable, deposito requerido, confirmacion, caja, stock y anulacion.
- Testcontainers PostgreSQL 17 validando 18 migraciones hasta `v202607201310`.
- Documentacion especifica en `docs/purchases/PURCHASES.md`.

## Hito 7 completado

El Hito 7 implemento reportes operativos y dashboard inicial como modulo read-only tenant-scoped:

- Modulo SaaS `COMMERCIAL_REPORTS`.
- Permiso atomico `commercial_reports:read`.
- API `/api/v1/reports` para ventas por rango, compras por rango, caja por rango, top productos vendidos, stock bajo y dashboard.
- Reportes por rango con `from` y `to` obligatorios, rango maximo de 366 dias y limite maximo de 500 filas.
- Dashboard calculado con `time_zone` del tenant.
- Stock bajo simple por parametro `threshold`, con default `0` hasta aprobar stock minimo por producto.
- Totales operativos sobre documentos `CONFIRMED`, con documentos `CANCELLED` informados por separado.
- Indices para consultas por fecha en ventas, compras, caja y stock.
- Tests unitarios de permisos, rangos, limites, threshold y zona horaria del dashboard.
- Testcontainers PostgreSQL 17 validando 19 migraciones hasta `v202607201400`.
- Documentacion especifica en `docs/reports/OPERATIONAL_REPORTS.md`.

## Hito 8 completado

El Hito 8 cerro el Sprint 2 con hardening tecnico y documentacion final:

- Manejo estructurado `400` para parametros faltantes, invalidos o fuera de validacion.
- Tests unitarios especificos para `ApiExceptionHandler`.
- Suite backend elevada a 89 tests unitarios.
- Reporte de hardening en `docs/sprints/SPRINT_2_HARDENING_REPORT.md`.
- Reporte de cierre en `docs/sprints/SPRINT_2_CLOSURE_REPORT.md`.
- Riesgo de permisos finos de reportes documentado para decision futura.

## Fuera de alcance propuesto

- Facturacion fiscal electronica.
- Contabilidad completa.
- Cuentas corrientes avanzadas.
- Precios complejos/listas de precios multiples.
- Impuestos avanzados.
- Transferencias de stock entre depositos.
- Reservas.
- Lotes y vencimientos.
- App mobile.
- BI avanzado.

## Siguiente paso recomendado

Definir Sprint 3. Recomendacion tecnica inicial: priorizar licenciamiento/modularidad comercial o UI operativa, pero no mezclar ambos en el mismo sprint.
