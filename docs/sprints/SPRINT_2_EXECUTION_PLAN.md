# Sprint 2 - Execution Plan

## Estado

En progreso desde el 2026-07-17.

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
| 3. Clientes y proveedores | Pendiente | CRUD tenant-scoped con permisos, auditoria y validaciones. |
| 4. Caja inicial | Pendiente | Apertura/cierre o movimientos basicos de caja segun decision funcional. |
| 5. Ventas basicas | Pendiente | Registro de venta con estados, cliente, items, impacto en stock/caja segun reglas aprobadas. |
| 6. Compras basicas | Pendiente | Registro de compra con proveedor, items e impacto en stock/caja segun reglas aprobadas. |
| 7. Reportes y dashboard operativo | Pendiente | Indicadores simples para ventas, compras, stock y caja. |
| 8. Hardening Sprint 2 | Pendiente | Tests, documentacion, revision de seguridad y cierre del sprint. |

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

Avanzar al Hito 3: clientes y proveedores. Ahora que existe una puerta automatica basica de calidad, se puede empezar a sumar negocio con menor riesgo de romper migraciones, backend o frontend.