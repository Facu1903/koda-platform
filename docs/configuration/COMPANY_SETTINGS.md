# Company Settings

## Objetivo

El Hito 5 expone la configuracion visual y regional de una empresa dentro de KODA PLATFORM sin permitir que el cliente decida el tenant efectivo.

El tenant se resuelve desde `TenantContext`, construido a partir del JWT emitido por backend.

## Endpoint

Base path: `/api/v1/company/settings`.

- `GET /api/v1/company/settings`: obtiene la configuracion del tenant autenticado.
- `PUT /api/v1/company/settings`: actualiza configuracion visual/regional del tenant autenticado.

Ambos endpoints son tenant-scoped y requieren autenticacion.

Para renderizado runtime de la UI existe `GET /api/v1/company/profile`. Ese endpoint devuelve solo datos no sensibles y no requiere permiso administrativo. Ver `docs/configuration/COMPANY_PROFILE.md`.

## Permisos

- Lectura: `company_settings:read`.
- Actualizacion: `company_settings:update`.

No se agrego una matriz rol-permiso nueva en este hito. Esa asignacion es una regla funcional y requiere aprobacion del Product Owner. El backend valida permisos reales recibidos en el JWT; no usa nombres de roles como atajo.

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
- `primaryColor` y `secondaryColor` usan formato `#RRGGBB`.
- `themeMode` acepta `light`, `dark` o `system`.
- `defaultCurrency` debe ser ISO 4217.
- `timeZone` debe ser un identificador valido de zona horaria.
- `dateFormat` y `timeFormat` se validan como patrones Java `DateTimeFormatter`.
- Campos opcionales de imagen enviados como string vacio se guardan como `null`.

## Auditoria

Cada actualizacion exitosa registra un evento `company_settings.update` en `audit_events` con:

- tenant,
- usuario actor,
- recurso `company_settings`,
- version anterior y nueva,
- campos modificados.

## Errores

- `401`: falta autenticacion o token valido.
- `403 PERMISSION_DENIED`: el JWT no contiene el permiso requerido.
- `404 COMPANY_SETTINGS_NOT_FOUND`: no existe configuracion para el tenant autenticado.
- `409 COMPANY_SETTINGS_VERSION_CONFLICT`: la version enviada no coincide con la version actual.
- `400 VALIDATION_ERROR`: request invalido por Bean Validation.
- `400 INVALID_REQUEST`: valor regional o visual invalido detectado por la capa de aplicacion.

## Alcance del Hito 5

Incluido:

- API tenant-scoped para consultar y actualizar configuracion visual/regional.
- DTOs de request/response.
- Validacion backend.
- Control optimista por version.
- Auditoria persistente.
- Tests unitarios de servicio.
- Validacion runtime de arranque y proteccion de endpoint.

No incluido:

- Upload de archivos de logo/favicon/imagenes.
- CDN/storage de assets.
- UI de configuracion.
- Cambio de nombre comercial, razon social o pais.
- Matriz rol-permiso aprobada para roles iniciales.
- Preferencias por usuario individual.
