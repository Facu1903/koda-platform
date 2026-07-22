# Frontend Tenant Theme

## Estado

Implementado en Sprint 5 Hito 3.

## Objetivo

Generar el tema Material UI del frontend a partir del perfil runtime del tenant autenticado, manteniendo fallback seguro al tema KODA cuando el perfil no esta disponible.

## Componentes

- `frontend/src/platform/configuration/companyProfile.ts`: contrato TypeScript del perfil runtime y fallback KODA.
- `frontend/src/platform/configuration/companyProfileClient.ts`: cliente HTTP para `GET /api/v1/company/profile`.
- `frontend/src/platform/configuration/CompanyProfileProvider.tsx`: provider React del perfil runtime.
- `frontend/src/theme/kodaTheme.ts`: fabrica `createKodaTheme` para tema dinamico.
- `frontend/src/theme/CompanyThemeProvider.tsx`: aplica el tema efectivo al shell.

## Flujo

1. `CompanyProfileProvider` intenta cargar `/api/v1/company/profile`.
2. Si la carga funciona, expone el perfil del tenant.
3. Si la carga falla, expone un perfil fallback KODA.
4. `CompanyThemeProvider` genera el tema MUI desde `branding`.
5. El shell de la aplicacion usa el tema efectivo sin bloquear la carga por fallas del perfil.

## Reglas aplicadas

- `primaryColor` y `secondaryColor` deben usar `#RRGGBB`; si no, se aplica fallback.
- `themeMode` soporta `dark`, `light` y `system`.
- `system` respeta `prefers-color-scheme` cuando el navegador lo soporta.
- El contraste del texto principal de botones se calcula desde el color efectivo.
- No se permite CSS arbitrario por tenant.
- No se permite HTML custom por tenant.
- El shell mantiene identidad KODA PLATFORM aunque muestre datos regionales y tenant del perfil.

## Fallback

El fallback visual usa:

- tenant `KODA`,
- `primaryColor` `#F6862B`,
- modo `dark`,
- locale `es-AR`,
- moneda `ARS`,
- zona horaria `America/Argentina/Buenos_Aires`.

Esto evita que una falla temporal de `/api/v1/company/profile` deje la aplicacion sin tema.

## Alcance actual

Incluido:

- consumo frontend del perfil runtime,
- tema MUI dinamico,
- modo claro/oscuro/sistema,
- fallback seguro,
- subtitulo del shell con tenant, locale y moneda efectivos,
- tests frontend.

No incluido:

- aplicacion de logo en shell,
- favicon runtime,
- imagen de login,
- UI administrativa de configuracion,
- cache persistente del perfil.

El formato regional centralizado se implementa en `docs/configuration/FRONTEND_REGIONAL_FORMATTING.md`.

## Validacion

- `npm.cmd run test`: 5 tests, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.
