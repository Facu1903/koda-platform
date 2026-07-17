# KODA PLATFORM

KODA PLATFORM es una plataforma SaaS profesional de gestion empresarial, multiempresa, modular y preparada para evolucionar durante la proxima decada.

El primer producto sera KODA ERP, usado inicialmente por KODA como cliente piloto, pero disenado desde el dia cero como producto comercial para multiples empresas.

## Estado actual

Sprint 1: Hito 7 - Stock completado.

Ya existe una base tecnica ejecutable con backend, frontend, PostgreSQL 17, migraciones Flyway iniciales, seed minimo aprobado, Tenant Context backend, autenticacion JWT con refresh tokens, API tenant-scoped de configuracion de empresa, CRUD backend de catalogos ERP y API tenant-scoped de stock con saldos y movimientos confirmados. La base ya camina; ahora hay que evitar que corra en ojotas.

## Documentos principales

- [Project Blueprint](PROJECT_BLUEPRINT.md)
- [Architecture](ARCHITECTURE.md)
- [Coding Standards](CODING_STANDARDS.md)
- [Roadmap](ROADMAP.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
- [Agents](AGENTS.md)
- [Sprint 1 Functional Baseline](docs/sprints/SPRINT_1_FUNCTIONAL_BASELINE.md)
- [Sprint 1 Execution Plan](docs/sprints/SPRINT_1_EXECUTION_PLAN.md)
- [PostgreSQL Conventions](docs/database/POSTGRESQL_CONVENTIONS.md)
- [Tenant Context](docs/security/TENANT_CONTEXT.md)
- [Authentication](docs/security/AUTHENTICATION.md)
- [Company Settings](docs/configuration/COMPANY_SETTINGS.md)
- [ERP Catalogs](docs/catalogs/ERP_CATALOGS.md)
- [Stock Movements](docs/stock/STOCK_MOVEMENTS.md)

## Stack obligatorio

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- Flyway
- Maven

### Base de datos

- PostgreSQL 17

### Frontend

- React
- TypeScript
- Vite
- Material UI

### Infraestructura

- Docker
- Docker Compose

### Versionado

- Git
- GitHub

## Principios

- Plataforma antes que ERP aislado.
- Multiempresa real desde el inicio.
- Modularidad para productos actuales y futuros.
- Seguridad por defecto.
- Testing desde el primer sprint de codigo.
- DTOs, no entidades expuestas.
- Auditoria y logs estructurados.
- Personalizacion por configuracion, no por forks.

## Sprint 1 previsto

Sprint 1 construira la primera base funcional:

- Autenticacion.
- Usuarios, roles y permisos.
- Empresas/Tenants.
- Sucursales.
- Configuracion de empresa.
- Productos, marcas, categorias, presentaciones y unidades.
- CRUD completo.
- Movimientos de stock.
- Auditoria.
- Tests.

La base funcional minima de Sprint 1 fue aprobada el 2026-07-15 y esta documentada en `docs/sprints/SPRINT_1_FUNCTIONAL_BASELINE.md`.

## Estructura inicial

```text
backend/              API Spring Boot 3 + Clean Architecture
frontend/             Web app React + TypeScript + Vite + Material UI
docker/               Configuracion auxiliar de infraestructura local
docs/                 Documentacion tecnica, sprints y decisiones
docker-compose.yml    PostgreSQL 17, backend y frontend para desarrollo
.env.example          Variables locales de ejemplo
```

## Desarrollo local

### Requisitos

- Java 21.
- Maven.
- Node.js 24 o compatible.
- npm.
- Docker Desktop.

En PowerShell, si `npm` queda bloqueado por politica de scripts, usar `npm.cmd`.

### Backend

```powershell
cd backend
$env:KODA_JWT_SECRET="change-me-with-at-least-32-bytes-local-only"
mvn test
mvn spring-boot:run
```

### Frontend

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

### Docker Compose

```powershell
copy .env.example .env
# Editar .env y definir KODA_JWT_SECRET antes de levantar backend.
docker compose up --build
```

Servicios esperados:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`

## Base de datos inicial

Flyway deja el esquema en `v202607171540` con:

- Tenant piloto KODA.
- Producto `KODA_ERP`.
- Modulos base de Sprint 1.
- Roles y permisos iniciales aprobados.
- Tablas base de tenants, configuracion, sucursales, depositos, usuarios, RBAC, catalogos, stock y auditoria.

La matriz inicial rol-permiso ya existe para la base del Sprint 1 y se ampliara por modulo. La creacion de usuarios reales se controla por bootstrap opt-in o por APIs futuras de administracion.

## Tenant Context

El backend ya tiene una base tecnica para resolver tenant desde el principal autenticado de KODA:

- No se acepta `tenant_id` libre desde el frontend para operaciones tenant-scoped.
- Las rutas tenant-scoped quedan preparadas para exigir un principal con tenant; auth y plataforma quedan neutrales.
- El contexto se limpia al finalizar cada request.
- Los casos de uso podran depender de `CurrentTenantProvider` para obtener el tenant actual.

JWT, login y refresh tokens ya alimentan este Tenant Context desde el principal autenticado de KODA.

## Autenticacion JWT

El backend ya expone `/api/v1/auth/login`, `/api/v1/auth/refresh` y `/api/v1/auth/logout`.

- `KODA_JWT_SECRET` es obligatorio y debe tener al menos 32 bytes.
- El access token es JWT HS256 con TTL corto.
- El refresh token es opaco, se guarda hasheado y rota en cada refresh.
- El usuario inicial no se crea por defecto; puede activarse con variables `KODA_BOOTSTRAP_OWNER_*`.

Ver detalle en `docs/security/AUTHENTICATION.md`.


## Configuracion de empresa

El backend ya expone `/api/v1/company/settings` para consultar y actualizar configuracion visual/regional del tenant autenticado.

- El tenant se resuelve desde JWT/Tenant Context.
- La lectura requiere `company_settings:read`.
- La actualizacion requiere `company_settings:update`.
- Las actualizaciones usan version optimista y registran auditoria.

La matriz rol-permiso para habilitar estos permisos en roles iniciales requiere aprobacion funcional explicita.

Ver detalle en `docs/configuration/COMPANY_SETTINGS.md`.

## Catalogos ERP

El backend ya expone CRUD tenant-scoped bajo `/api/v1/catalog` para:

- marcas,
- categorias planas,
- unidades de medida,
- presentaciones,
- productos.

Reglas aprobadas: marca opcional, SKU unico por tenant, una presentacion principal por producto, desactivacion permitida con stock y eliminacion por soft delete. La matriz aprobada de catalogos fue aplicada por Flyway para KODA: `TENANT_OWNER`/`TENANT_ADMIN` CRUD completo, `MANAGER` lectura/actualizacion y `READ_ONLY` lectura.

Ver detalle en `docs/catalogs/ERP_CATALOGS.md`.

## Stock

El backend ya expone endpoints tenant-scoped bajo `/api/v1/stock` para:

- consulta de saldos por deposito/producto,
- consulta de movimientos confirmados,
- creacion de movimientos `IN`, `OUT` y `ADJUSTMENT`.

Reglas aprobadas: no stock negativo, movimientos confirmados inmutables, correcciones mediante nuevos movimientos, productos stockeables solo si son `GOOD`, activos y con seguimiento de stock. El ledger guarda saldo anterior, saldo posterior y delta para trazabilidad real. La matriz aprobada de stock fue aplicada por Flyway para KODA.

Ver detalle en `docs/stock/STOCK_MOVEMENTS.md`.

## Nota de entorno

Java 21 y Maven 3.9.16 fueron verificados para ejecutar tests backend. Docker Desktop fue validado con PostgreSQL 17, backend y frontend activos.
