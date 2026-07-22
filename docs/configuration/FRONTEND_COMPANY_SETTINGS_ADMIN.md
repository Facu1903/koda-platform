# Frontend Company Settings Admin

## Estado

Implementado en Sprint 5 Hito 5.

## Objetivo

Agregar una pantalla administrativa dentro del modulo `CONFIGURATION` para consultar, editar, previsualizar y guardar configuracion visual/regional del tenant autenticado.

La UI no decide el tenant efectivo. La API resuelve tenant desde JWT/Tenant Context y aplica permisos reales en backend.

## Componentes

- `frontend/src/platform/api/platformHttp.ts`: cliente HTTP comun con token, JSON y errores controlados.
- `frontend/src/platform/configuration/companySettings.ts`: contrato TypeScript de configuracion administrativa.
- `frontend/src/platform/configuration/companySettingsClient.ts`: cliente de `GET` y `PUT /api/v1/company/settings`.
- `frontend/src/platform/configuration/CompanySettingsWorkspace.tsx`: pantalla administrativa.
- `frontend/src/platform/configuration/CompanyProfileProvider.tsx`: aplica perfil runtime actualizado despues de guardar.
- `frontend/src/app/App.tsx`: enruta el modulo `CONFIGURATION` hacia la pantalla administrativa.

## Flujo

1. El shell valida capabilities y muestra el modulo `Configuracion` solo si esta habilitado.
2. La pantalla carga `GET /api/v1/company/settings`.
3. Si la lectura falla por permisos, muestra estado controlado `Acceso restringido`.
4. El usuario edita branding y configuracion regional en un borrador local.
5. La pantalla permite cancelar cambios locales.
6. La pantalla muestra preview de colores, moneda, fecha/hora y assets visuales.
7. Al guardar, envia `PUT /api/v1/company/settings` con `version`.
8. Si la API responde `409`, muestra conflicto de version.
9. Si guarda correctamente, actualiza el estado local y aplica el perfil runtime al tema/formato del shell.

## Reglas aplicadas

- `version` viaja siempre al guardar para control optimista.
- `primaryColor` es obligatorio y se valida como `#RRGGBB` en frontend.
- `secondaryColor` es opcional y, si existe, se valida como `#RRGGBB`.
- `defaultCurrency` se normaliza a mayusculas.
- `defaultLocale` y `numberLocale` aceptan formato operativo `es-AR`, `en-US` o equivalente con `_`.
- URLs visuales se tratan como texto opcional y se validan con la politica de assets visuales.
- Logo, favicon e imagen de login tienen preview controlada.
- Validaciones finales siguen en backend.
- La actualizacion exitosa refresca tema y formato regional sin recargar la pagina.

## Estados visibles

- Cargando.
- Configuracion cargada.
- Cambios sin guardar.
- Guardado correcto.
- Error de lectura.
- Acceso restringido.
- Error de actualizacion.
- Conflicto de version.

## Decision tecnica

Se agrego `platformHttp.ts` para evitar duplicar manejo de token y errores en cada cliente frontend. Esto deja una base mas mantenible para las proximas pantallas SaaS.

La pantalla administrativa se carga con `React.lazy` desde el shell. El build separa `CompanySettingsWorkspace` en un chunk propio para no cargar formularios administrativos durante el arranque operativo general.

La pantalla no implementa upload ni CDN. La politica estricta de assets visuales queda documentada en `docs/configuration/VISUAL_ASSETS.md`.

## Alcance actual

Incluido:

- UI administrativa real dentro de `CONFIGURATION`,
- lectura y actualizacion contra API backend,
- version optimista,
- cancelar cambios locales,
- preview visual/regional,
- preview controlada de assets visuales,
- sincronizacion inmediata del perfil runtime,
- manejo de `403` y `409`,
- tests frontend.

No incluido:

- upload de archivos,
- CDN/storage,
- imagen de login aplicada,
- preferencias por usuario.

## Validacion

- `npm.cmd run test`: 17 pruebas, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos; pantalla administrativa separada en chunk propio.
