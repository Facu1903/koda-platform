# Sprint 5 - Closure Report

## Estado

Cierre tecnico preparado el 2026-07-22. Pendiente de aprobacion funcional final por el Product Owner.

## Objetivo

Convertir la configuracion visual y regional existente en una experiencia real de personalizacion por tenant para KODA PLATFORM, manteniendo seguridad SaaS, aislamiento multiempresa y compatibilidad evolutiva.

Sprint 5 no buscaba hacer una pantalla bonita y declararla victoria. Buscaba que cada empresa pueda operar con identidad propia sin romper el producto comun.

## Resultado

Sprint 5 entrega la base completa de personalizacion avanzada por tenant:

- perfil runtime no sensible de empresa,
- tema Material UI dinamico por tenant,
- fallbacks visuales seguros,
- formato regional tenant-aware,
- pantalla administrativa de configuracion,
- actualizacion con version optimista,
- assets visuales por URL validada,
- logo runtime,
- favicon runtime,
- preview controlada de assets,
- matriz rol-permiso aprobada para configuracion administrativa,
- auditoria de cambios,
- documentacion tecnica y funcional.

La plataforma queda mas cerca de un SaaS comercial real: una base comun, muchas empresas, identidad configurable y reglas controladas. Nada de forks silenciosos ni personalizaciones artesanales escondidas debajo de la alfombra.

## Entregables completados

- Base funcional aprobada del Sprint 5.
- Endpoint `GET /api/v1/company/profile`.
- Contrato `CompanyRuntimeProfile`.
- Provider frontend de perfil de empresa.
- Theme provider dinamico con Material UI.
- Fabrica `createKodaTheme`.
- Utilidades regionales frontend para fecha, hora, numero y moneda.
- Hook `useRegionalFormatters`.
- Cliente HTTP frontend compartido `platformHttp`.
- Contrato frontend `CompanySettings`.
- Pantalla `CompanySettingsWorkspace`.
- Validacion backend estricta de URLs visuales.
- Utilidad frontend `visualAssets.ts`.
- Logo runtime en shell principal.
- Favicon runtime por tenant.
- Preview segura de logo, favicon e imagen de login.
- Migracion `V202607221500__seed_company_settings_permissions.sql`.
- Tests backend/frontend de personalizacion, permisos, auditoria y errores.
- Reporte de hardening y cierre tecnico.

## Migraciones

Flyway queda en `v202607221500` con 24 migraciones aplicadas.

Migracion principal de Sprint 5:

- `V202607221500__seed_company_settings_permissions.sql`

Esta migracion aplica la matriz aprobada:

- `TENANT_OWNER`: lectura y actualizacion.
- `TENANT_ADMIN`: lectura y actualizacion.
- `MANAGER`: solo lectura.
- `SALES_USER`, `STOCK_USER`, `READ_ONLY`: sin acceso administrativo.

## APIs y contratos impactados

- `GET /api/v1/company/profile`
- `GET /api/v1/company/settings`
- `PUT /api/v1/company/settings`

Contratos frontend relevantes:

- `CompanyRuntimeProfile`
- `CompanySettings`
- `CompanyProfileProvider`
- `CompanyThemeProvider`
- `createRegionalFormatters`
- `visualAssets`

## Documentacion entregada

- `docs/sprints/SPRINT_5_FUNCTIONAL_BASELINE.md`
- `docs/sprints/SPRINT_5_EXECUTION_PLAN.md`
- `docs/sprints/SPRINT_5_HARDENING_REPORT.md`
- `docs/sprints/SPRINT_5_CLOSURE_REPORT.md`
- `docs/configuration/COMPANY_PROFILE.md`
- `docs/configuration/FRONTEND_TENANT_THEME.md`
- `docs/configuration/FRONTEND_REGIONAL_FORMATTING.md`
- `docs/configuration/FRONTEND_COMPANY_SETTINGS_ADMIN.md`
- `docs/configuration/VISUAL_ASSETS.md`
- `docs/configuration/COMPANY_SETTINGS.md`
- `docs/configuration/COMPANY_SETTINGS_PERMISSIONS_AUDIT.md`

## Validaciones finales

- `mvn -B verify`: 150 tests unitarios, 15 tests de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 24 migraciones hasta `v202607221500`.
- `npm.cmd run test`: 17 tests frontend, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.

## Commits principales

- `433dfd1 docs(sprint-5): propose tenant personalization sprint`
- `b272f9e docs(sprint-5): approve sprint definition`
- `ed94d18 feat(sprint-5): add company runtime profile`
- `f4186f2 feat(sprint-5): add dynamic tenant theme`
- `3ca4f98 feat(sprint-5): add regional tenant formatting`
- `02e8c86 feat(sprint-5): add company settings admin UI`
- `657bec1 feat(sprint-5): add controlled visual assets`
- `791fecd feat(sprint-5): harden company settings permissions`

## Riesgos abiertos

- Upload/CDN/storage propio de assets pendiente.
- Antivirus, optimizacion y proxy de imagenes pendientes.
- Custom domains y subdominios pendientes.
- Login publico tenant-aware completo pendiente.
- Estrategia de tenant discovery antes de login pendiente.
- Preferencias visuales por usuario pendientes.
- Motor i18n avanzado pendiente.
- Validacion visual automatizada con navegador real pendiente.
- Pruebas end-to-end autenticadas pendientes.

## Decision de cierre tecnico

Sprint 5 cumple el objetivo tecnico acordado: KODA PLATFORM queda preparada para personalizacion avanzada por tenant sin fragmentar el producto ni relajar seguridad.

El siguiente paso recomendado es pedir aprobacion funcional final del Product Owner. Si se aprueba, corresponde crear el acta formal `SPRINT_5_APPROVAL.md` y luego subirla a GitHub.
