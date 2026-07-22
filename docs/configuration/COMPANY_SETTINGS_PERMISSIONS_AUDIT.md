# Company Settings Permissions and Audit

## Estado

Implementado en Sprint 5 Hito 7.

## Objetivo

Aplicar la matriz aprobada de acceso administrativo a configuracion de empresa, verificar auditoria de cambios y asegurar errores API controlados.

Este hito no cambia reglas funcionales. Ejecuta la decision aprobada en `docs/sprints/SPRINT_5_FUNCTIONAL_BASELINE.md`.

## Separacion de accesos

La plataforma mantiene dos contratos distintos:

- `GET /api/v1/company/profile`: lectura runtime no sensible para renderizar la UI. Requiere autenticacion y Tenant Context, pero no permiso administrativo.
- `/api/v1/company/settings`: contrato administrativo para consultar y actualizar configuracion. Requiere permisos RBAC.

Esta separacion evita que un usuario operativo necesite permisos administrativos solo para ver la interfaz con la marca de su empresa.

## Matriz aplicada

| Capacidad | TENANT_OWNER | TENANT_ADMIN | MANAGER | SALES_USER | STOCK_USER | READ_ONLY |
| --- | --- | --- | --- | --- | --- | --- |
| Ver apariencia runtime | Si | Si | Si | Si | Si | Si |
| Leer configuracion administrativa | Si | Si | Si | No | No | No |
| Actualizar configuracion | Si | Si | No | No | No | No |

Permisos atomicos:

- `company_settings:read`
- `company_settings:update`

## Migracion

La asignacion inicial se aplica con:

- `backend/src/main/resources/db/migration/V202607221500__seed_company_settings_permissions.sql`

Asignaciones sembradas:

- `TENANT_OWNER`: `company_settings:read`, `company_settings:update`
- `TENANT_ADMIN`: `company_settings:read`, `company_settings:update`
- `MANAGER`: `company_settings:read`

Roles sin permisos administrativos de configuracion:

- `SALES_USER`
- `STOCK_USER`
- `READ_ONLY`

La migracion usa `ON CONFLICT DO NOTHING` para ser idempotente frente a ambientes donde la asignacion ya exista.

## Auditoria

Cada actualizacion exitosa registra `company_settings.update` con:

- tenant,
- usuario actor,
- recurso `company_settings`,
- campos modificados,
- version anterior,
- version nueva,
- resultado `SUCCESS`,
- metadata segura de request.

No se auditan tokens, secretos ni cuerpos completos de request.

## Errores controlados

- `403 PERMISSION_DENIED`: incluye `requiredPermission`.
- `409 COMPANY_SETTINGS_VERSION_CONFLICT`: respuesta estructurada para conflicto de version optimista.
- `400 INVALID_REQUEST`: no filtra valores internos o mensajes crudos de validacion de aplicacion.

## Validacion automatizada

- `CompanySettingsServiceTest`: valida lectura de `MANAGER`, bloqueo de update para `MANAGER`, bloqueo administrativo para `SALES_USER`, `STOCK_USER` y `READ_ONLY`, y auditoria de actualizacion exitosa.
- `ApiExceptionHandlerTest`: valida errores estructurados para permiso insuficiente, conflicto de version e invalid request.
- `FlywayPostgresqlIT`: valida que la migracion deje PostgreSQL 17 en `v202607221500` y que la matriz aplicada coincida con la aprobada.

## Fuera de alcance

- Upload, CDN o storage propio de assets.
- Permisos por campo individual dentro de configuracion.
- Preferencias visuales por usuario.
- Login publico tenant-aware antes de definir estrategia de tenant discovery.
