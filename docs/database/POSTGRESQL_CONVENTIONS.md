# PostgreSQL Conventions

## Objetivo

Estas convenciones definen como se modela la base operacional de KODA PLATFORM para mantener consistencia, seguridad multiempresa y evolutividad durante el ciclo de vida SaaS.

## Version objetivo

- PostgreSQL 17.
- Flyway como unica via para cambios estructurales.
- Zona horaria operacional: UTC en persistencia; conversion regional en aplicacion/presentacion.

## Migraciones Flyway

Formato obligatorio:

```text
VYYYYMMDDHHMM__description.sql
```

Reglas:

- Una migracion debe ser pequena, legible y revisable.
- No modificar una migracion ya aplicada en entornos compartidos; crear una nueva.
- No usar scripts manuales fuera de Flyway para estructura o datos base.
- Los seeds permitidos son datos de plataforma aprobados: tenant piloto, productos, modulos, roles y permisos.

## Naming

- Tablas: `snake_case`, plural cuando representen colecciones (`tenants`, `products`, `stock_movements`).
- Columnas: `snake_case`.
- Primary key: `id` UUID.
- Tenant: `tenant_id`.
- Foreign keys: `<entity>_id`.
- Constraints:
  - Primary key: `pk_<table>`.
  - Foreign key: `fk_<table>_<target_or_meaning>`.
  - Unique: `uq_<table>_<columns_or_meaning>`.
  - Check: `ck_<table>_<rule>`.
  - Index: `idx_<table>_<columns_or_meaning>`.

## Multi-tenancy

La estrategia inicial es shared database + shared schema.

Reglas obligatorias:

- Toda tabla de negocio tenant-scoped debe tener `tenant_id NOT NULL`.
- Las relaciones entre tablas tenant-scoped deben usar FK compuestas `(id, tenant_id)` para impedir referencias cruzadas entre empresas.
- Todo listado o busqueda de datos de negocio debe filtrar por tenant desde contexto autenticado.
- El frontend nunca define el tenant efectivo.

## Identificadores

- Usar UUID como primary key.
- Generar UUID en PostgreSQL con `gen_random_uuid()` para filas creadas por migracion o persistencia directa.
- No exponer IDs secuenciales.

## Auditoria tecnica por tabla

Campos estandar recomendados:

```text
created_at timestamptz NOT NULL DEFAULT now()
created_by uuid NULL
updated_at timestamptz NOT NULL DEFAULT now()
updated_by uuid NULL
deleted_at timestamptz NULL
version bigint NOT NULL DEFAULT 0
```

Notas:

- `deleted_at` se usa solo cuando hay razon funcional para borrado logico.
- `version` prepara optimistic locking.
- Los timestamps se guardan como `timestamptz`.

## Seguridad y RBAC

- `user_accounts` representa identidad global.
- `tenant_memberships` representa pertenencia de un usuario a una empresa.
- `roles.scope` separa roles de plataforma y roles de tenant.
- `permissions` contiene permisos atomicos por recurso y accion.
- La matriz rol-permiso no se hardcodea sin aprobacion funcional.

## Stock

- `stock_balances` mantiene saldos actuales por tenant, deposito y producto.
- `stock_movements` funciona como ledger confirmado de movimientos.
- La cantidad en saldos no puede ser negativa por constraint.
- Los tipos iniciales aprobados son `IN`, `OUT` y `ADJUSTMENT`.

## Auditoria persistente

- `audit_events` guarda eventos consultables, no reemplazables por logs.
- Debe registrar tenant, actor, accion, recurso, resultado, origen, `trace_id` y metadata cuando aplique.
- Eventos de plataforma pueden tener `tenant_id` nulo.

## Indices

- Toda tabla tenant-scoped de alto uso debe tener indices que empiecen por `tenant_id`.
- Unicidad de codigos de negocio debe ser compuesta por tenant.
- Los indices parciales con `deleted_at IS NULL` evitan colisiones con registros archivados.

## Decisiones pendientes

- Matriz de permisos por rol.
- Politica de Row Level Security PostgreSQL.
- Estrategia de particionamiento para auditoria y stock si el volumen lo exige.
- Politica de archivado historico por tenant.