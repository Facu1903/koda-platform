# KODA PLATFORM

KODA PLATFORM es una plataforma SaaS profesional de gestion empresarial, multiempresa, modular y preparada para evolucionar durante la proxima decada.

El primer producto sera KODA ERP, usado inicialmente por KODA como cliente piloto, pero disenado desde el dia cero como producto comercial para multiples empresas.

## Estado actual

Sprint 1: Hito 2 - PostgreSQL y Flyway completado.

Ya existe una base tecnica ejecutable con backend, frontend, PostgreSQL 17, migraciones Flyway iniciales y seed minimo aprobado. Todavia no hay CRUDs ni reglas de negocio expuestas por API; esa contencion es intencional. Primero cimientos, despues pisos. Hacerlo al reves queda lindo hasta que empieza a crujir.

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
docker compose up --build
```

Servicios esperados:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`

## Base de datos inicial

Flyway deja el esquema en `v202607171520` con:

- Tenant piloto KODA.
- Producto `KODA_ERP`.
- Modulos base de Sprint 1.
- Roles y permisos iniciales aprobados.
- Tablas base de tenants, configuracion, sucursales, depositos, usuarios, RBAC, catalogos, stock y auditoria.

La matriz rol-permiso y la creacion de usuarios reales se definen en hitos posteriores.

## Nota de entorno

Java 21 y Maven 3.9.16 fueron verificados para ejecutar tests backend. Docker Desktop fue validado con PostgreSQL 17, backend y frontend activos.