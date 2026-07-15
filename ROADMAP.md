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

### Objetivo

Extender ERP hacia operaciones comerciales basicas.

### Alcance candidato

- Clientes.
- Proveedores.
- Compras basicas.
- Ventas basicas.
- Caja inicial.
- Reportes operativos simples.
- Mejoras de dashboard.

El alcance definitivo requiere aprobacion del Product Owner.

## 5. Sprint 3 - Licenciamiento y modularidad comercial

### Objetivo

Formalizar la activacion/desactivacion de productos y modulos por tenant.

### Alcance candidato

- Catalogo de productos de plataforma.
- Catalogo de modulos.
- Planes.
- Entitlements.
- Feature flags.
- UI de administracion de licencias.
- Guards backend/frontend por modulo.

## 6. Sprint 4 - Escalabilidad y observabilidad

### Objetivo

Endurecer la plataforma para operacion SaaS real.

### Alcance candidato

- Logs estructurados avanzados.
- Metricas.
- Health checks expandidos.
- Trazabilidad.
- Pruebas de carga iniciales.
- Optimizacion de indices.
- Reglas de retencion de auditoria.

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

## 10. Decisiones pendientes para Product Owner

Antes de Sprint 1 conviene confirmar:

- Nombre legal y datos iniciales del tenant KODA.
- Roles iniciales.
- Permisos iniciales.
- Moneda inicial.
- Pais/provincias iniciales.
- Idioma inicial.
- Definicion exacta de producto, marca, categoria, presentacion y unidad de medida.
- Reglas de stock: tipos de movimiento, impacto y permisos.

