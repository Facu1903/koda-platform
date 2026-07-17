# Tenant Context

## Objetivo

El Tenant Context define como el backend conoce el tenant efectivo de una operacion. Es una pieza critica de seguridad multiempresa: si se resuelve mal, una empresa podria leer o modificar datos de otra.

## Regla principal

El frontend no decide el tenant efectivo.

KODA PLATFORM no acepta `tenant_id` libre desde headers, query params o bodies para operar sobre datos tenant-scoped. El tenant debe venir del principal autenticado construido por la capa de seguridad del backend.

## Componentes

- `TenantId`: value object para identificadores de tenant.
- `TenantContext`: contiene tenant, usuario, roles, permisos y marca de platform admin para la request actual.
- `CurrentTenantProvider`: puerto de aplicacion que los casos de uso usaran para obtener el tenant actual.
- `TenantContextHolder`: almacenamiento por thread/request usado por infraestructura web.
- `TenantAwarePrincipal`: contrato que debera cumplir el principal autenticado de KODA.
- `TenantContextAuthenticationFilter`: traduce el principal autenticado a `TenantContext` durante la request y limpia el contexto al finalizar.

## Comportamiento actual

- Rutas tenant-scoped bajo `/api/**` requieren un principal compatible con `TenantAwarePrincipal`.
- Rutas neutrales como Actuator, OpenAPI, Swagger, `/api/v1/auth/**` y `/api/v1/platform/**` no requieren tenant.
- Si existe autenticacion generica pero no hay tenant para una ruta tenant-scoped, el backend responde `403`.
- Si no existe autenticacion, Spring Security mantiene el flujo normal de `401`.

## Alcance del Hito 3

Incluido:

- Contrato tecnico para resolver tenant desde autenticacion.
- Guardrail para no aceptar autenticaciones sin tenant en rutas tenant-scoped.
- Limpieza obligatoria del contexto por request.
- Tests unitarios de resolucion y bloqueo.

No incluido en el Hito 3:

- JWT productivo.
- Login.
- Refresh tokens funcionales.
- Matriz rol-permiso.
- Repositorios tenant-scoped.
- Row Level Security PostgreSQL.

## Integracion con autenticacion

Desde el Hito 4, los JWT emitidos por backend se convierten a `KodaAuthenticatedPrincipal`. Ese principal alimenta `TenantContextAuthenticationFilter` con tenant, usuario, roles y permisos. El punto importante sigue siendo el mismo: queda prohibido el camino facil y peligroso de resolver tenant desde datos enviados libremente por el cliente.
