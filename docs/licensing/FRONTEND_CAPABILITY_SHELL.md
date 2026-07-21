# Frontend Capability Shell

## Estado

Implementado en Sprint 3 Hito 6.

## Objetivo

Preparar el frontend para construir navegacion y rutas operativas a partir de las capabilities efectivas del tenant autenticado.

La UI no decide seguridad. Solo refleja lo que el backend ya resolvio y bloquea visualmente rutas no habilitadas para evitar friccion operativa.

## Componentes frontend

- `frontend/src/platform/licensing/capabilities.ts`: contrato TypeScript del response `GET /api/v1/capabilities`, catalogo estable de modulos visibles y helpers de evaluacion.
- `frontend/src/platform/licensing/capabilitiesClient.ts`: cliente HTTP para cargar capabilities desde `/api/v1/capabilities`.
- `frontend/src/platform/licensing/CapabilitiesProvider.tsx`: contexto React con estados `loading`, `ready` y `unavailable`.
- `frontend/src/app/App.tsx`: shell visual con navegacion, dashboard y bloqueo de rutas por modulo.

## Reglas aplicadas

- El frontend consulta `/api/v1/capabilities`.
- El access token se lee desde storage local cuando exista y se envia como Bearer token.
- La request tambien usa `credentials: include` para mantener compatibilidad con sesiones/cookies futuras.
- El menu operativo solo muestra modulos habilitados.
- Una ruta directa a un modulo no habilitado muestra estado bloqueado.
- Si la licencia no se puede cargar, la UI queda en estado controlado y permite reintentar.
- El dashboard lista modulos disponibles y modulos sin licencia activa.

## Modulos reconocidos por el shell

- `CONFIGURATION`
- `CATALOGS`
- `COMMERCIAL_PARTNERS`
- `STOCK`
- `CASH`
- `SALES`
- `PURCHASES`
- `COMMERCIAL_REPORTS`
- `AUDIT`

## Seguridad

El bloqueo frontend es ergonomia, no seguridad.

La seguridad real sigue estando en:

- JWT y Tenant Context.
- RBAC backend.
- `TenantLicenseAccessGuard` backend.
- API de capabilities calculada en backend.

## Validacion

- `npm.cmd run test`: 3 tests frontend, 0 fallos.
- `npm.cmd run lint`: sin errores.
- `npm.cmd run build`: TypeScript y Vite correctos.

Casos cubiertos:

- Render de navegacion solo con modulos habilitados.
- Bloqueo visual de ruta directa hacia modulo deshabilitado.
- Estado controlado cuando no se pueden cargar capabilities.

## Fuera de alcance

- Login UI.
- Pantallas funcionales completas de cada modulo.
- Administracion visual de licencias.
- Cache distribuida o invalidacion realtime de capabilities.
