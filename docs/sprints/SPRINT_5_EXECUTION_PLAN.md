# Sprint 5 - Execution Plan

## Estado

Propuesta inicial pendiente de aprobacion funcional del Product Owner.

## Objetivo

Implementar personalizacion avanzada por tenant sobre la base existente de configuracion de empresa, llevando branding y configuracion regional al frontend operativo sin romper seguridad, aislamiento ni compatibilidad.

## Principio rector

Personalizar sin fragmentar.

KODA PLATFORM debe permitir identidad propia por empresa, pero no debe convertirse en un sistema de forks invisibles. La personalizacion debe ser configurable, validada, testeable y reversible.

## Base funcional propuesta

La propuesta funcional queda documentada en `docs/sprints/SPRINT_5_FUNCTIONAL_BASELINE.md`.

No se debe iniciar codigo de negocio del Sprint 5 hasta que el Product Owner apruebe esta base funcional.

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

## Hitos internos propuestos

| Hito | Estado | Resultado esperado |
| --- | --- | --- |
| 1. Base funcional Sprint 5 | Propuesto | Product Owner aprueba alcance, reglas y fuera de alcance. |
| 2. Perfil runtime de empresa | Pendiente | Backend expone datos no sensibles para renderizar UI por tenant autenticado. |
| 3. Theme provider dinamico | Pendiente | Frontend genera tema MUI desde configuracion del tenant con fallbacks seguros. |
| 4. Formato regional | Pendiente | Frontend centraliza formato de fecha, hora, numero y moneda por tenant. |
| 5. UI administrativa de configuracion | Pendiente | Pantalla para consultar, previsualizar y editar branding/regional con version optimista. |
| 6. Assets visuales controlados | Pendiente | Logo, favicon e imagen de login por URL validada, con fallback y documentacion de riesgos. |
| 7. Permisos, auditoria y hardening funcional | Pendiente | Matriz aprobada aplicada por migracion, auditoria verificada y errores controlados. |
| 8. Hardening Sprint 5 | Pendiente | Validacion completa backend/frontend, documentacion final y reporte de cierre tecnico. |

## Hito 1 propuesto

El Hito 1 debe cerrar la definicion funcional:

- Documento `docs/sprints/SPRINT_5_FUNCTIONAL_BASELINE.md`.
- Foco propuesto: personalizacion avanzada por tenant.
- Confirmacion de que Sprint 5 no incluye upload, CDN, custom domains ni login publico tenant-aware completo.
- Matriz de permisos propuesta para configuracion administrativa.
- Criterios de aceptacion funcional.

Decision requerida: aprobacion del Product Owner para convertir esta propuesta en base funcional aprobada.

## Hito 2 propuesto - Perfil runtime de empresa

Crear una lectura runtime liviana para la UI:

- Datos de tenant no sensibles: nombre comercial, pais, locale, moneda y zona horaria.
- Branding efectivo: logo, favicon, login image, colores y modo de tema.
- Configuracion regional efectiva: formatos de fecha, hora, numero y moneda.
- Requiere autenticacion y Tenant Context.
- No requiere permiso administrativo.
- No permite actualizar datos.

Decision tecnica esperada: mantener separado el endpoint runtime del endpoint administrativo `/api/v1/company/settings`.

## Hito 3 propuesto - Theme provider dinamico

Implementar frontend de tema dinamico:

- `CompanyProfileProvider` o equivalente.
- Generacion de `ThemeProvider` desde configuracion del tenant.
- Soporte `dark`, `light` y `system`.
- Fallback al tema KODA si la configuracion no esta disponible.
- Contraste seguro para texto principal y botones.
- Sin CSS arbitrario.

## Hito 4 propuesto - Formato regional

Centralizar formato regional en frontend:

- fecha,
- fecha/hora,
- hora,
- numeros,
- moneda.

El frontend debe usar `Intl` donde sea suficiente y no debe duplicar reglas de negocio monetarias. Los importes persistidos siguen siendo valores numericos y codigo de moneda.

## Hito 5 propuesto - UI administrativa de configuracion

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

## Hito 6 propuesto - Assets visuales controlados

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

## Hito 7 propuesto - Permisos, auditoria y hardening funcional

Aplicar reglas aprobadas:

- permisos de lectura administrativa y actualizacion,
- migracion de asignacion rol-permiso si el Product Owner aprueba matriz,
- auditoria de cambios,
- tests de permisos,
- tests de validacion,
- tests de version optimista,
- tests de aislamiento tenant.

## Hito 8 propuesto - Hardening Sprint 5

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

Revisar y aprobar o ajustar la propuesta funcional de Sprint 5.
