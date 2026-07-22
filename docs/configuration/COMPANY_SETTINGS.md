# Company Settings

## Objetivo

El Hito 5 expone la configuracion visual y regional de una empresa dentro de KODA PLATFORM sin permitir que el cliente decida el tenant efectivo. El Hito 7 aplica la matriz de permisos aprobada, verifica auditoria y endurece errores funcionales.

El tenant se resuelve desde `TenantContext`, construido a partir del JWT emitido por backend.

## Endpoint

Base path: `/api/v1/company/settings`.

- `GET /api/v1/company/settings`: obtiene la configuracion del tenant autenticado.
- `PUT /api/v1/company/settings`: actualiza configuracion visual/regional del tenant autenticado.

Ambos endpoints son tenant-scoped y requieren autenticacion.

Para renderizado runtime de la UI existe `GET /api/v1/company/profile`. Ese endpoint devuelve solo datos no sensibles y no requiere permiso administrativo. Ver `docs/configuration/COMPANY_PROFILE.md`.

La UI administrativa frontend para este endpoint esta documentada en `docs/configuration/FRONTEND_COMPANY_SETTINGS_ADMIN.md`.

## Permisos

- Lectura: `company_settings:read`.
- Actualizacion: `company_settings:update`.

La matriz rol-permiso de Sprint 5 queda aplicada por la migracion `V202607221500__seed_company_settings_permissions.sql`.

| Rol | Leer configuracion administrativa | Actualizar configuracion |
| --- | --- | --- |
| `TENANT_OWNER` | Si | Si |
| `TENANT_ADMIN` | Si | Si |
| `MANAGER` | Si | No |
| `SALES_USER` | No | No |
| `STOCK_USER` | No | No |
| `READ_ONLY` | No | No |

El backend valida permisos reales recibidos en el JWT; no usa nombres de roles como atajo. Los roles se usan solo para sembrar la matriz inicial aprobada.

## Lectura

Response:

```json
{
  "id": "00000000-0000-4000-8000-000000000002",
  "tenant": {
    "id": "00000000-0000-4000-8000-000000000001",
    "commercialName": "KODA",
    "legalName": "KODA",
    "countryCode": "AR"
  },
  "branding": {
    "logoUrl": null,
    "faviconUrl": null,
    "loginImageUrl": null,
    "primaryColor": "#F6862B",
    "secondaryColor": null,
    "themeMode": "dark"
  },
  "regional": {
    "defaultLocale": "es-AR",
    "defaultCurrency": "ARS",
    "timeZone": "America/Argentina/Buenos_Aires",
    "dateFormat": "dd/MM/yyyy",
    "timeFormat": "HH:mm",
    "numberLocale": "es-AR",
    "currencyFormat": "symbol"
  },
  "version": 0,
  "updatedAt": "2026-07-17T00:00:00Z"
}
```

## Actualizacion

Request:

```json
{
  "version": 0,
  "logoUrl": "https://cdn.example.com/logo.png",
  "faviconUrl": "https://cdn.example.com/favicon.ico",
  "loginImageUrl": "https://cdn.example.com/login.png",
  "primaryColor": "#F6862B",
  "secondaryColor": "#FFFFFF",
  "themeMode": "dark",
  "defaultLocale": "es-AR",
  "defaultCurrency": "ARS",
  "timeZone": "America/Argentina/Buenos_Aires",
  "dateFormat": "dd/MM/yyyy",
  "timeFormat": "HH:mm",
  "numberLocale": "es-AR",
  "currencyFormat": "symbol"
}
```

Reglas tecnicas:

- `version` es obligatoria para control optimista de concurrencia.
- `logoUrl`, `faviconUrl` y `loginImageUrl` son opcionales; string vacio se guarda como `null`.
- Las URLs visuales deben usar `https://`, host valido, sin userinfo, sin fragmentos, sin `data:`, sin `javascript:` y sin rutas relativas.
- `primaryColor` y `secondaryColor` usan formato `#RRGGBB`.
- `themeMode` acepta `light`, `dark` o `system`.
- `defaultCurrency` debe ser ISO 4217.
- `timeZone` debe ser un identificador valido de zona horaria.
- `dateFormat` y `timeFormat` se validan como patrones Java `DateTimeFormatter`.

## Auditoria

Cada actualizacion exitosa registra un evento `company_settings.update` en `audit_events` con:

- tenant,
- usuario actor,
- recurso `company_settings`,
- version anterior y nueva,
- campos modificados.
- resultado `SUCCESS`,
- metadata segura de request cuando exista.

No se registran tokens, secretos ni cuerpos completos de request. La auditoria guarda diferencias de configuracion, no un volcado ansioso de todo lo que paso por la puerta.

## Errores

- `401`: falta autenticacion o token valido.
- `403 PERMISSION_DENIED`: el JWT no contiene el permiso requerido; la respuesta incluye `requiredPermission`.
- `404 COMPANY_SETTINGS_NOT_FOUND`: no existe configuracion para el tenant autenticado.
- `409 COMPANY_SETTINGS_VERSION_CONFLICT`: la version enviada no coincide con la version actual.
- `400 VALIDATION_ERROR`: request invalido por Bean Validation.
- `400 INVALID_REQUEST`: valor regional o visual invalido detectado por la capa de aplicacion.

## Alcance implementado

Incluido:

- API tenant-scoped para consultar y actualizar configuracion visual/regional.
- DTOs de request/response.
- Validacion backend.
- Control optimista por version.
- Auditoria persistente.
- Tests unitarios de servicio.
- Validacion runtime de arranque y proteccion de endpoint.
- Migracion de asignacion rol-permiso aprobada.
- Tests de matriz Flyway/PostgreSQL 17.
- Tests de errores `PERMISSION_DENIED`, `COMPANY_SETTINGS_VERSION_CONFLICT` e `INVALID_REQUEST`.

No incluido:

- CDN/storage de assets.
- Upload de archivos de logo/favicon/imagenes.
- Cambio de nombre comercial, razon social o pais.
- Preferencias por usuario individual.
