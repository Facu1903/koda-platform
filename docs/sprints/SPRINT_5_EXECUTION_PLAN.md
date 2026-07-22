# Sprint 5 - Execution Plan

## Estado

Definicion funcional aprobada por el Product Owner el 2026-07-22.

## Objetivo

Implementar personalizacion avanzada por tenant sobre la base existente de configuracion de empresa, llevando branding y configuracion regional al frontend operativo sin romper seguridad, aislamiento ni compatibilidad.

## Principio rector

Personalizar sin fragmentar.

KODA PLATFORM debe permitir identidad propia por empresa, pero no debe convertirse en un sistema de forks invisibles. La personalizacion debe ser configurable, validada, testeable y reversible.

## Base funcional aprobada

La base funcional de Sprint 5 fue aprobada por el Product Owner el 2026-07-22 y queda documentada en `docs/sprints/SPRINT_5_FUNCTIONAL_BASELINE.md`.

Con esta aprobacion, Sprint 5 puede iniciar desarrollo por hitos sin modificar las reglas funcionales aprobadas.

## Alcance del sprint

- Perfil runtime de empresa para branding/regional no sensible.
- Tema Material UI dinamico por tenant.
- Fallbacks visuales seguros.
- Favicon/logo/login image por URL validada.
- Utilidades frontend de formato regional.
- Pantalla administrativa de configuracion de empresa.
- Validacion de permisos para lectura administrativa y actualizacion.
- Auditoria de cambios.
- Tests backend/frontend.
- Documentacion y cierre.

## Hitos internos aprobados

| Hito | Estado | Resultado esperado |
| --- | --- | --- |
| 1. Base funcional Sprint 5 | Completado | Product Owner aprobo alcance, reglas y fuera de alcance. |
| 2. Perfil runtime de empresa | Completado | Backend expone datos no sensibles para renderizar UI por tenant autenticado. |
| 3. Theme provider dinamico | Completado | Frontend genera tema MUI desde configuracion del tenant con fallbacks seguros. |
| 4. Formato regional | Completado | Frontend centraliza formato de fecha, hora, numero y moneda por tenant. |
| 5. UI administrativa de configuracion | Pendiente | Pantalla para consultar, previsualizar y editar branding/regional con version optimista. |
| 6. Assets visuales controlados | Pendiente | Logo, favicon e imagen de login por URL validada, con fallback y documentacion de riesgos. |
| 7. Permisos, auditoria y hardening funcional | Pendiente | Matriz aprobada aplicada por migracion, auditoria verificada y errores controlados. |
| 8. Hardening Sprint 5 | Pendiente | Validacion completa backend/frontend, documentacion final y reporte de cierre tecnico. |

## Hito 1 completado

El Hito 1 cierra la definicion funcional:

- Documento `docs/sprints/SPRINT_5_FUNCTIONAL_BASELINE.md`.
- Foco aprobado: personalizacion avanzada por tenant.
- Confirmacion de que Sprint 5 no incluye upload, CDN, custom domains ni login publico tenant-aware completo.
- Matriz de permisos aprobada para configuracion administrativa.
- Criterios de aceptacion funcional.

Decision funcional: avanzar con Sprint 5 manteniendo fuera de alcance upload de archivos, CDN/storage propio, custom domains, subdominios, login publico tenant-aware completo, preferencias por usuario, CSS/HTML arbitrario y white-label total.

## Hito 2 completado - Perfil runtime de empresa

El Hito 2 crea una lectura runtime liviana para la UI:

- Endpoint `GET /api/v1/company/profile`.
- Modelo de aplicacion `CompanyRuntimeProfile`.
- Controlador `CompanyProfileController`.
- Datos de tenant no sensibles: id, nombre comercial y pais.
- Branding efectivo: logo, favicon, login image, colores y modo de tema.
- Configuracion regional efectiva: locale, moneda, zona horaria, formatos de fecha, hora, numero y moneda.
- Requiere autenticacion y Tenant Context.
- Requiere modulo `CONFIGURATION` habilitado por licencia.
- No requiere permiso administrativo `company_settings:read`.
- No permite actualizar datos.
- No expone razon social, version, tax identifier ni datos internos de administracion.
- Documento tecnico `docs/configuration/COMPANY_PROFILE.md`.

Decision tecnica: mantener separado el endpoint runtime `/api/v1/company/profile` del endpoint administrativo `/api/v1/company/settings`. El perfil runtime sirve para renderizar UI; settings sirve para administrar configuracion con permisos y version optimista.

Validacion:

- `mvn -B "-Dtest=CompanySettingsServiceTest,KodaPlatformApplicationTests" test`
- `mvn -B test`

## Hito 3 completado - Theme provider dinamico

El Hito 3 implementa frontend de tema dinamico:

- Contrato TypeScript `CompanyRuntimeProfile`.
- Cliente HTTP `fetchCompanyProfile`.
- `CompanyProfileProvider` con estado `loading`, `ready`, `unavailable` y fallback efectivo.
- `CompanyThemeProvider` para aplicar el tema Material UI.
- Fabrica `createKodaTheme` basada en branding del tenant.
- Soporte `dark`, `light` y `system`.
- Fallback al tema KODA si la configuracion no esta disponible.
- Contraste seguro para texto principal y botones.
- Sin CSS arbitrario.
- Shell actualizado para mostrar tenant, locale y moneda efectivos.
- Documento tecnico `docs/configuration/FRONTEND_TENANT_THEME.md`.

Decision tecnica: el tema runtime no bloquea la aplicacion. Si `/api/v1/company/profile` falla, el frontend renderiza con fallback KODA y mantiene separado el estado de capabilities/licencia.

Validacion:

- `npm.cmd run test`: 9 pruebas, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.

## Hito 4 completado - Formato regional

El Hito 4 centraliza formato regional en frontend:

- fecha,
- fecha/hora,
- hora,
- numeros,
- moneda.

- Fabrica pura `createRegionalFormatters`.
- Hook `useRegionalFormatters` conectado al perfil runtime del tenant.
- Formato de fecha/hora con zona horaria del tenant.
- Formato de numero con `numberLocale`.
- Formato monetario con `defaultCurrency` y `currencyFormat`.
- Soporte controlado de patrones operativos `dateFormat` y `timeFormat`.
- Fallback a KODA o `Intl` cuando la configuracion no es aplicable en frontend.
- Shell actualizado para mostrar calculo de capabilities y vigencia de modulos con formato regional.
- Documento tecnico `docs/configuration/FRONTEND_REGIONAL_FORMATTING.md`.

Decision tecnica: usar `Intl` como base y soportar un subconjunto seguro de patrones Java `DateTimeFormatter` en frontend. No se replica todo el motor de formato Java porque seria fragil y costoso; si una empresa usa un patron fuera del subconjunto operativo, el frontend cae a `Intl` sin romper la UI.

Validacion:

- `npm.cmd run test`
- `npm.cmd run lint`
- `npm.cmd run build`

## Hito 5 - UI administrativa de configuracion

Construir una pantalla operativa dentro del modulo `CONFIGURATION`:

- lectura de configuracion actual,
- formulario de branding,
- formulario regional,
- vista previa de tema,
- guardar con version optimista,
- cancelar cambios locales,
- estados de carga/error,
- conflicto de version con mensaje claro.

La UI no debe ser una landing page. Debe ser una herramienta de configuracion usable, densa y clara.

## Hito 6 - Assets visuales controlados

Aplicar assets configurables:

- logo en shell principal,
- favicon en navegador cuando exista,
- login image preparada para futura pantalla de login,
- preview controlada de imagenes.

Restricciones:

- URL externa validada.
- `https://` obligatorio en produccion.
- sin `data:`,
- sin `javascript:`,
- sin upload en Sprint 5.

## Hito 7 - Permisos, auditoria y hardening funcional

Aplicar reglas aprobadas:

- permisos de lectura administrativa y actualizacion,
- migracion de asignacion rol-permiso segun matriz aprobada,
- auditoria de cambios,
- tests de permisos,
- tests de validacion,
- tests de version optimista,
- tests de aislamiento tenant.

## Hito 8 - Hardening Sprint 5

Cerrar Sprint 5 con:

- `mvn -B verify`,
- `npm.cmd run test`,
- `npm.cmd run lint`,
- `npm.cmd run build`,
- reporte de hardening,
- reporte de cierre,
- actualizacion de README, ROADMAP y CHANGELOG.

## Criterios de calidad

- No exponer datos sensibles en perfil runtime.
- No requerir permisos administrativos para renderizar el shell.
- No permitir estilos arbitrarios por tenant.
- Mantener Clean Architecture.
- Mantener DTOs y validaciones Bean Validation.
- Mantener auditoria para cambios administrativos.
- Mantener aislamiento tenant.
- Mantener frontend rapido y estable con fallback.
- Documentar riesgos y decisiones.

## Riesgos y decisiones a cuidar

- Tenant antes de login: no forzar una estrategia sin aprobar subdominio, dominio custom o selector.
- Assets externos: pueden fallar o tener latencia; siempre deben tener fallback.
- Contraste: permitir cualquier color sin control puede romper accesibilidad.
- Formato regional: mostrar moneda no debe cambiar la moneda real de una operacion ya creada.
- Permisos: todos necesitan leer apariencia runtime, pocos deben editar configuracion.
- Cache: si se cachea perfil runtime, debe invalidarse al actualizar configuracion o usar TTL corto.

## Fuera de alcance

- Upload de archivos.
- Storage/CDN.
- Login publico tenant-aware completo.
- Custom domains/subdominios.
- Preferencias por usuario.
- Traduccion integral de textos.
- White-label total.
- Nuevos modulos ERP.

## Siguiente paso recomendado

Avanzar al Hito 5: UI administrativa de configuracion de empresa.
