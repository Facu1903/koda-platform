# Sprint 5 - Hardening Report

## Estado

Completado en Hito 8. Aprobado funcionalmente por el Product Owner el 2026-07-22.

## Objetivo

Cerrar tecnicamente Sprint 5 validando que la personalizacion avanzada por tenant funcione sin debilitar seguridad, aislamiento multiempresa, mantenibilidad ni compatibilidad futura.

El foco fue comprobar que branding, perfil runtime, tema dinamico, formato regional, UI administrativa, assets visuales, permisos y auditoria quedaron integrados como capacidades SaaS reales, no como maquillaje de interfaz con casco de obra.

No se modificaron reglas funcionales aprobadas del Sprint 5.

## Cambios realizados

### Validacion backend

Se ejecuto validacion completa con Maven:

- compilacion backend,
- tests unitarios,
- reglas de arquitectura,
- empaquetado jar,
- pruebas de integracion con Testcontainers,
- migraciones Flyway contra PostgreSQL 17 real.

Resultado:

- 150 tests unitarios, 0 fallos.
- 15 tests de integracion, 0 fallos.
- 24 migraciones validadas y aplicadas hasta `v202607221500`.

### Validacion frontend

Se ejecuto validacion completa del frontend:

- tests Vitest,
- lint ESLint,
- build productivo TypeScript/Vite.

Resultado:

- 17 tests frontend, 0 fallos.
- lint sin errores.
- build productivo correcto.

La pantalla `CompanySettingsWorkspace` queda separada en chunk propio para no inflar innecesariamente el bundle inicial.

### Revision funcional de Sprint 5

Se revisaron los criterios de aceptacion de `docs/sprints/SPRINT_5_FUNCTIONAL_BASELINE.md` contra lo implementado:

- Perfil runtime tenant-scoped disponible para usuarios autenticados.
- Separacion entre perfil runtime y configuracion administrativa.
- Tema Material UI dinamico con fallback KODA.
- Formateo regional centralizado en frontend.
- Pantalla administrativa de configuracion.
- Version optimista para actualizacion.
- Manejo de permisos insuficientes y conflicto de version.
- Assets visuales externos validados.
- Matriz rol-permiso aprobada aplicada por Flyway.
- Auditoria de cambios administrativos.
- Documentacion tecnica actualizada.

### Documentacion

Se agregan documentos de cierre:

- `docs/sprints/SPRINT_5_HARDENING_REPORT.md`
- `docs/sprints/SPRINT_5_CLOSURE_REPORT.md`

Tambien se actualizan README, roadmap, changelog y plan de ejecucion del Sprint 5.

## Validaciones ejecutadas

- `mvn -B verify`: 150 tests unitarios, 15 tests de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 24 migraciones hasta `v202607221500`.
- `npm.cmd run test`: 17 tests frontend, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.

## Observaciones del entorno local

En la primera ejecucion local, `npm.cmd run test` y `npm.cmd run build` fueron bloqueados por `EPERM` al escribir archivos temporales de Vite/TypeScript dentro de OneDrive. Al reintentarlos con permisos externos, ambas validaciones pasaron sin cambios de codigo.

Conclusion: no es una falla funcional del frontend. Es friccion de entorno local Windows/OneDrive escribiendo cache temporal. Conviene mantenerlo observado porque puede molestar el flujo diario, aunque GitHub Actions en Linux no deberia reproducir ese bloqueo.

## Riesgos abiertos

- No hay upload, storage propio, CDN, optimizacion de imagenes ni antivirus para assets visuales.
- No hay custom domains, subdominios ni login publico tenant-aware completo.
- `loginImageUrl` queda modelado y administrable, pero su aplicacion antes del login requiere definir tenant discovery.
- No hay preferencias visuales por usuario.
- No hay motor i18n avanzado ni traduccion integral del producto.
- No hay validacion visual automatizada con navegador real para contraste/accesibilidad.
- El aislamiento tenant sigue siendo responsabilidad de backend, guards, Tenant Context y tests; no hay Row Level Security en PostgreSQL.
- HS256 sigue aceptado para etapa actual; antes de produccion multi-nodo debe definirse rotacion de llaves o RS256/JWKS.

## Decision

Hito 8 deja Sprint 5 tecnicamente cerrado y aprobado funcionalmente por el Product Owner.

La conclusion honesta: KODA PLATFORM ya puede comportarse como SaaS personalizable por empresa sin abrir la puerta al caos de CSS libre, assets inseguros o permisos improvisados. Todavia falta resolver identidad antes del login, storage/CDN y validacion visual avanzada, pero esas decisiones necesitan estrategia de producto e infraestructura, no impulso de teclado.
