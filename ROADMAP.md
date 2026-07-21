# KODA PLATFORM - Roadmap

## 1. Enfoque

El desarrollo se organiza por sprints. No se avanza al siguiente sprint hasta que el anterior entregue un sistema funcional, verificable y documentado.

La hoja de ruta protege dos cosas:

- Valor usable en cada etapa.
- Arquitectura capaz de sostener la plataforma durante anos.

## 2. Sprint 0 - Constitucion del proyecto

### Objetivo

Definir las reglas tecnicas, arquitectonicas y operativas antes de escribir codigo de negocio.

### Entregables

- `PROJECT_BLUEPRINT.md`
- `ARCHITECTURE.md`
- `CODING_STANDARDS.md`
- `ROADMAP.md`
- `CONTRIBUTING.md`
- `CHANGELOG.md`
- `README.md`
- `AGENTS.md`

### Criterios de aceptacion

- Arquitectura documentada.
- Estructura de carpetas propuesta.
- Estrategia Git definida.
- Versionado definido.
- Testing definido.
- Seguridad definida.
- Flujo de desarrollo definido.
- Criterios de calidad definidos.

## 3. Sprint 1 - Base funcional de KODA ERP

### Estado

Cerrado el 2026-07-17. Ver `docs/sprints/SPRINT_1_CLOSURE_REPORT.md`.

### Objetivo

Construir el primer incremento ejecutable de plataforma y ERP, con seguridad, multi-tenancy, configuracion basica y catalogo/stock inicial.

### Alcance funcional

- Autenticacion.
- Usuarios.
- Roles.
- Permisos.
- Empresas/Tenants.
- Sucursales.
- Configuracion de empresa.
- Productos.
- Marcas.
- Categorias.
- Presentaciones.
- Unidades de medida.
- CRUD completo.
- Movimientos de stock.
- Auditoria.
- Tests.

### Base funcional aprobada

La base funcional minima de Sprint 1 fue aprobada por el Product Owner el 2026-07-15 y queda documentada en `docs/sprints/SPRINT_1_FUNCTIONAL_BASELINE.md`.

Resumen:

- Tenant inicial KODA: Argentina, `es-AR`, ARS, `America/Argentina/Buenos_Aires`, tema oscuro y color primario `#F6862B`.
- Roles iniciales: `PLATFORM_SUPER_ADMIN`, `TENANT_OWNER`, `TENANT_ADMIN`, `MANAGER`, `SALES_USER`, `STOCK_USER`, `READ_ONLY`.
- Permisos iniciales para usuarios, roles, tenants, sucursales, configuracion de empresa, catalogos, stock y auditoria.
- Stock: movimientos `IN`, `OUT`, `ADJUSTMENT`, sin stock negativo por defecto, auditoria obligatoria y correcciones por movimiento inverso.

### Hitos tecnicos internos sugeridos

Estos hitos no cambian el alcance aprobado; lo ordenan para no mezclar cimientos con pintura.

1. Scaffolding backend, frontend e infraestructura Docker.
2. Base PostgreSQL y Flyway.
3. Modelo tenant y contexto multiempresa.
4. Seguridad JWT, usuarios, roles y permisos.
5. Configuracion de empresa y theming inicial.
6. Catalogo de productos, marcas, categorias, unidades y presentaciones.
7. Stock y movimientos.
8. Auditoria transversal.
9. Tests, hardening y documentacion.

### Criterios de aceptacion

- Backend compila.
- Frontend compila.
- Docker Compose levanta dependencias.
- Migraciones ejecutan correctamente.
- Login funcional.
- CRUDs protegidos por permisos.
- Datos aislados por tenant.
- Movimientos de stock auditados.
- Tests minimos ejecutan correctamente.
- README, documentacion tecnica y changelog actualizados.

### Riesgo

El alcance es amplio para un Sprint 1. La forma madura de manejarlo es mantener la frontera funcional, pero validar por hitos internos pequenos. El camino inmaduro seria prometer todo junto y descubrir tarde que la seguridad quedo de adorno. No haremos eso.

## 4. Sprint 2 - Operacion comercial inicial

### Estado

Cerrado y aprobado funcionalmente por el Product Owner el 2026-07-20. Ver `docs/sprints/SPRINT_2_CLOSURE_REPORT.md` y `docs/sprints/SPRINT_2_APPROVAL.md`.

### Objetivo

Extender ERP hacia operaciones comerciales basicas. Sprint 2 entrega backend/API para clientes, proveedores, caja, ventas, compras, reportes y dashboard operativo inicial.

### Alcance candidato

- Clientes.
- Proveedores.
- Compras basicas.
- Ventas basicas.
- Caja inicial.
- Reportes operativos simples.
- Mejoras de dashboard.
- CI/CD minimo y tests de persistencia como refuerzo tecnico recomendado.

La base funcional minima fue aprobada por el Product Owner el 2026-07-17.

## 5. Sprint 3 - Fundacion SaaS Comercial

### Estado

Cerrado y aprobado funcionalmente por el Product Owner el 2026-07-21. Hitos 2, 3, 4, 5, 6 y 7 completados: modelo persistente de licencias SaaS, API backend de capabilities tenant-scoped, guards backend por modulo, administracion interna de licencias con auditoria, shell frontend condicionado por capabilities y hardening final. Ver `docs/sprints/SPRINT_3_FUNCTIONAL_BASELINE.md`, `docs/sprints/SPRINT_3_EXECUTION_PLAN.md`, `docs/sprints/SPRINT_3_HARDENING_REPORT.md`, `docs/sprints/SPRINT_3_CLOSURE_REPORT.md`, `docs/sprints/SPRINT_3_APPROVAL.md`, `docs/licensing/SAAS_LICENSING_MODEL.md`, `docs/licensing/TENANT_LICENSE_GUARDS.md`, `docs/licensing/TENANT_LICENSE_ADMINISTRATION.md` y `docs/licensing/FRONTEND_CAPABILITY_SHELL.md`.

### Objetivo

Implementar licencias, modulos y control de acceso por empresa para convertir la base multi-tenant en una plataforma SaaS comercialmente gobernable.

### Alcance aprobado

- Plan tecnico inicial `KODA_PILOT`.
- Suscripciones por tenant.
- Entitlements efectivos por producto y modulo.
- Limites iniciales modelados.
- Feature flags tecnicos.
- Capability API tenant-scoped.
- Guards backend por producto/modulo.
- Preparacion frontend para rutas y menus segun capabilities.
- Administracion interna de licencias protegida por permisos de plataforma.
- Auditoria y tests.

## 6. Sprint 4 - Escalabilidad y observabilidad

### Estado

Definido y aprobado funcionalmente por el Product Owner el 2026-07-21. Ver `docs/sprints/SPRINT_4_FUNCTIONAL_BASELINE.md` y `docs/sprints/SPRINT_4_EXECUTION_PLAN.md`.

### Objetivo

Endurecer la plataforma para operacion SaaS real: observabilidad, trazabilidad, health checks, metricas, performance, cache seguro y auditoria operativa.

### Alcance aprobado

- Correlation/request ID.
- Logs estructurados avanzados.
- Metricas.
- Health checks expandidos.
- Trazabilidad.
- Optimizacion de indices.
- Cache tecnico seguro para capabilities.
- Reglas de retencion de auditoria.
- Hardening y documentacion.

## 7. Sprint 5 - Personalizacion avanzada por tenant

### Objetivo

Permitir configuracion visual y regional completa por empresa.

### Alcance candidato

- Logo.
- Favicon.
- Paleta.
- Tema claro/oscuro.
- Login image.
- Moneda.
- Fecha/hora.
- Idioma.
- Zona horaria.

## 8. Versionado

Mientras el producto no tenga clientes externos, se usara versionado `0.x.y`.

Convencion:

- `0.<sprint>.<patch>`

Ejemplos:

- `0.0.0`: Sprint 0 documental.
- `0.1.0`: Sprint 1 completo.
- `0.1.1`: correccion sobre Sprint 1.

Cuando exista primera version comercial estable:

- Adoptar SemVer: `MAJOR.MINOR.PATCH`.

## 9. Estrategia de compatibilidad

Desde la primera API:

- Versionar endpoints.
- Evitar cambios breaking sin plan.
- Mantener migraciones reproducibles.
- Documentar cambios en `CHANGELOG.md`.
- Separar cambios tecnicos de cambios funcionales.

## 10. Decisiones funcionales aprobadas

El 2026-07-15 el Product Owner aprobo la base funcional minima para Sprint 1:

- Datos iniciales del tenant KODA.
- Roles iniciales.
- Permisos iniciales.
- Moneda inicial.
- Pais, idioma y zona horaria.
- Reglas iniciales de stock.

Pendientes de detalle durante Sprint 1:

- Provincias iniciales.
- Definicion operativa exacta de producto, marca, categoria, presentacion y unidad de medida.
- Reglas avanzadas de stock por sucursal, deposito, permiso o excepcion comercial.
