# Sprint 5 - Approval

## Estado

Aprobado funcionalmente por el Product Owner el 2026-07-22.

## Decision aprobada

El Product Owner aprueba formalmente el cierre funcional del Sprint 5: **Personalizacion Avanzada por Tenant**.

Con esta aprobacion, se acepta que el alcance entregado cumple la base funcional aprobada en `docs/sprints/SPRINT_5_FUNCTIONAL_BASELINE.md` y el cierre tecnico documentado en `docs/sprints/SPRINT_5_CLOSURE_REPORT.md`.

## Alcance aceptado

- Perfil runtime no sensible por tenant.
- Tema Material UI dinamico por empresa.
- Fallback visual KODA.
- Formato regional tenant-aware para fecha, hora, numeros y moneda.
- Pantalla administrativa de configuracion de empresa.
- Actualizacion de configuracion con version optimista.
- Assets visuales por URL validada.
- Logo runtime.
- Favicon runtime.
- Preview controlada de assets.
- Matriz rol-permiso aprobada para configuracion administrativa.
- Auditoria de cambios administrativos.
- Validaciones backend/frontend completas.
- Documentacion tecnica y funcional actualizada.

## Validaciones aceptadas

- `mvn -B verify`: 150 tests unitarios, 15 tests de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 24 migraciones hasta `v202607221500`.
- `npm.cmd run test`: 17 tests frontend, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.

## Riesgos aceptados para backlog

- Upload/CDN/storage propio de assets pendiente.
- Antivirus, optimizacion y proxy de imagenes pendientes.
- Custom domains y subdominios pendientes.
- Login publico tenant-aware completo pendiente.
- Estrategia de tenant discovery antes de login pendiente.
- Preferencias visuales por usuario pendientes.
- Motor i18n avanzado pendiente.
- Validacion visual automatizada con navegador real pendiente.
- Pruebas end-to-end autenticadas pendientes.
- Row Level Security en PostgreSQL pendiente de decision futura.
- Estrategia final de llaves JWT para produccion multi-nodo pendiente.

## Condiciones de continuidad

No se deben modificar reglas funcionales de personalizacion, permisos, auditoria, tenant runtime profile ni configuracion administrativa sin aprobacion explicita del Product Owner.

Cualquier avance futuro que incorpore upload, CDN/storage, custom domains, subdominios, login publico tenant-aware completo, preferencias visuales por usuario, CSS/HTML arbitrario o white-label total requiere definicion funcional previa.

## Cierre

Sprint 5 queda cerrado y aprobado funcionalmente.

La plataforma ya tiene una base seria de personalizacion SaaS por empresa. No esta todo resuelto, ni deberia estarlo: las piezas que faltan son decisiones de producto e infraestructura que conviene pensar bien antes de abrir otra puerta.
