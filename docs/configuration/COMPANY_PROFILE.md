# Company Runtime Profile

## Estado

Implementado en Sprint 5 Hito 2.

## Objetivo

Exponer una lectura runtime tenant-scoped con branding y configuracion regional no sensible para que la UI pueda renderizarse con identidad de empresa sin requerir permisos administrativos.

Este endpoint no reemplaza `/api/v1/company/settings`. El perfil runtime sirve para pintar la aplicacion; el endpoint de settings sirve para administrarla.

## Endpoint

Base path: `/api/v1/company/profile`.

- `GET /api/v1/company/profile`: obtiene el perfil visual/regional del tenant autenticado.

El endpoint requiere autenticacion y Tenant Context valido.

## Permisos

No requiere `company_settings:read`.

Justificacion: todos los usuarios autenticados del tenant necesitan ver la aplicacion con la identidad visual y regional de su empresa, aunque no puedan administrar esa configuracion.

La lectura sigue protegida por:

- autenticacion JWT,
- Tenant Context backend,
- guard de licencia del modulo `CONFIGURATION`,
- aislamiento tenant en repositorio.

## Response

```json
{
  "tenant": {
    "id": "00000000-0000-4000-8000-000000000001",
    "commercialName": "KODA",
    "countryCode": "AR"
  },
  "branding": {
    "logoUrl": "https://cdn.example.com/logo.png",
    "faviconUrl": "https://cdn.example.com/favicon.ico",
    "loginImageUrl": "https://cdn.example.com/login.png",
    "primaryColor": "#F6862B",
    "secondaryColor": "#FFFFFF",
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
  "updatedAt": "2026-07-22T12:00:00Z"
}
```

## Datos excluidos

El perfil runtime no devuelve:

- razon social,
- CUIT/tax identifier,
- version de configuracion,
- datos internos de administracion,
- permisos,
- roles,
- informacion de licencias.

La version queda en `/api/v1/company/settings` porque solo hace falta para actualizaciones administrativas con control optimista.

## Errores

- `401`: falta autenticacion o token valido.
- `403 TENANT_CONTEXT_REQUIRED`: el request autenticado no resolvio Tenant Context.
- `403 TENANT_LICENSE_ACCESS_DENIED`: el tenant no tiene habilitado el producto/modulo requerido.
- `404 COMPANY_SETTINGS_NOT_FOUND`: no existe configuracion para el tenant autenticado.

## Auditoria

La lectura runtime no registra auditoria funcional.

Los cambios administrativos siguen auditados por `/api/v1/company/settings` con accion `company_settings.update`.

## Decision tecnica

El Hito 2 reutiliza `CompanySettingsRepository` y agrega un modelo de aplicacion `CompanyRuntimeProfile` para separar el contrato runtime del contrato administrativo.

No se agrega cache en este hito. Si el perfil se vuelve una lectura caliente en frontend, se evaluara cache con TTL corto o invalidacion al actualizar `company_settings`.

## Validacion

- `mvn -B "-Dtest=CompanySettingsServiceTest,KodaPlatformApplicationTests" test`: 13 tests, 0 fallos.
- `mvn -B test`: 144 tests unitarios backend, 0 fallos.
