# Sprint 3 - Execution Plan

## Estado

Definicion funcional aprobada por el Product Owner el 2026-07-20. Sprint 3 listo para iniciar implementacion por hitos.

## Objetivo

Implementar la fundacion SaaS comercial de KODA PLATFORM: licencias, modulos y control de acceso por empresa.

Sprint 3 no debe perseguir mas funcionalidad ERP. El objetivo es que la plataforma pueda decir, de forma confiable y auditable: este tenant tiene contratado este producto, estos modulos, estas capacidades y estos limites.

## Principio rector

Permiso RBAC y licencia SaaS son capas distintas. Una empresa puede tener usuarios con permisos, pero si el modulo no esta habilitado para el tenant, la operacion debe ser bloqueada.

Si esto no se resuelve ahora, cada modulo futuro va a traer su propio parche de acceso. Ese camino escala mal y cobra intereses con sonrisa.

## Base funcional aprobada

La base funcional de Sprint 3 fue aprobada por el Product Owner el 2026-07-20 y queda documentada en `docs/sprints/SPRINT_3_FUNCTIONAL_BASELINE.md`.

## Alcance del sprint

- Planes tecnicos/comerciales iniciales.
- Suscripciones por tenant.
- Entitlements efectivos por producto y modulo.
- Limites iniciales modelados.
- Feature flags como herramienta tecnica, no comercial.
- Capability API para tenant autenticado.
- Guard backend reusable por producto/modulo.
- Preparacion frontend para navegacion segun capabilities.
- Auditoria y tests.

## Hitos internos propuestos

| Hito | Estado | Resultado esperado |
| --- | --- | --- |
| 1. Base funcional Sprint 3 | Completado | Foco aprobado: Fundacion SaaS Comercial, licencias, modulos y control de acceso por empresa. |
| 2. Modelo de licencias y migraciones | Completado | Tablas para planes, suscripciones, limites, overrides, feature flags y seed `KODA_PILOT`, preservando entitlements existentes. |
| 3. Capability backend | Completado | Servicio y API `GET /api/v1/capabilities` para calcular productos, modulos, features y limites efectivos del tenant autenticado. |
| 4. Guards backend por modulo | Pendiente | Bloqueo reutilizable `PRODUCT_NOT_ENABLED` y `MODULE_NOT_ENABLED` aplicado a modulos existentes. |
| 5. Administracion interna de licencias | Pendiente | APIs plataforma para consultar y modificar suscripciones/entitlements con auditoria. |
| 6. Frontend capability shell | Pendiente | Contexto frontend de capabilities, menus/rutas condicionados y bloqueo visual de modulos no habilitados. |
| 7. Hardening Sprint 3 | Pendiente | Tests unitarios/integracion, validacion Flyway PostgreSQL 17, documentacion final y cierre. |

## Hito 2 completado

El Hito 2 implemento la base persistente del licenciamiento SaaS comercial:

- Migracion `V202607201500__create_saas_licensing_model.sql`.
- Metadatos `core_module` y `commercially_toggleable` en `platform_modules`.
- Tablas `product_plans`, `product_plan_modules` y `product_plan_limits`.
- Tabla `tenant_product_subscriptions` para suscripciones por tenant/producto/plan.
- Tablas `tenant_limit_overrides` y `tenant_feature_flags`.
- Indices para consultas futuras de capabilities.
- Plan tecnico `KODA_PILOT` para tenant KODA con todos los modulos actuales y limites ilimitados.
- Test de persistencia actualizado para validar 20 migraciones hasta `v202607201500`.
- Documentacion especifica en `docs/licensing/SAAS_LICENSING_MODEL.md`.

Hito 2 no implementa todavia API de capabilities, guards por modulo, administracion interna de licencias ni UI.

## Hito 3 completado

El Hito 3 implemento el calculo backend de capabilities tenant-scoped:

- Servicio de aplicacion `TenantCapabilitiesService`.
- Puerto `TenantCapabilitiesRepository` para mantener Clean Architecture.
- Repositorio JDBC `JdbcTenantCapabilitiesRepository` con consultas optimizadas sobre suscripciones, planes, entitlements, feature flags y overrides de limites.
- API `GET /api/v1/capabilities` para el tenant autenticado.
- DTOs de salida para productos, modulos, feature flags y limites efectivos, sin exponer entidades.
- Manejo global de error `TenantCapabilitiesUnavailableException` para tenant inexistente o inactivo.
- Pruebas unitarias del servicio y prueba de integracion contra PostgreSQL 17 para validar el calculo real sobre el seed `KODA_PILOT`.

Decision tecnica aprobada de facto en este hito: la API de capabilities requiere usuario autenticado y tenant context, pero no exige un permiso RBAC especifico. Es informacion necesaria para construir el shell operativo del frontend. La seguridad de ejecucion real se aplicara en Hito 4 con guards backend por producto/modulo.

Hito 3 no implementa todavia guards backend por modulo, administracion interna de licencias ni UI.

Validacion:

- `mvn -B test`: 92 pruebas unitarias, 0 fallos.
- `mvn -B verify`: 92 pruebas unitarias y 8 pruebas de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 20 migraciones hasta `v202607201500`.

## Orientacion tecnica inicial

### Reutilizar lo existente

Ya existen tablas base para productos, modulos y entitlements. Sprint 3 debe extenderlas o complementarlas, no reemplazarlas.

Estructuras incorporadas en Hito 2:

- `product_plans`
- `product_plan_modules`
- `product_plan_limits`
- `tenant_product_subscriptions`
- `tenant_feature_flags`
- `tenant_limit_overrides`

La migracion priorizo compatibilidad, baja duplicacion y lectura eficiente de capabilities.

### Modulo backend recomendado

Crear un modulo backend dedicado, por ejemplo `licensing`, respetando Clean Architecture:

- `domain`: codigos, estados, capabilities, reglas puras.
- `application`: servicios de consulta, validacion y administracion.
- `infrastructure`: repositorios JDBC/JPA y queries optimizadas.
- `api`: controllers y DTOs, sin exponer entidades.

### Guard backend

El guard debe ser centralizado y testeable.

Criterio inicial recomendado:

- Resolver tenant desde `TenantContext`.
- Validar producto/modulo con un servicio de aplicacion.
- Luego validar permiso RBAC existente.
- Devolver errores estructurados y auditables.

No conviene dispersar consultas de entitlements en cada servicio. Eso seria rapido hoy y caro manana.

### Capability API

Endpoint tenant-scoped implementado en Hito 3:

- `GET /api/v1/capabilities`

Respuesta esperada:

- tenant activo,
- productos habilitados,
- modulos habilitados,
- feature flags efectivas,
- limites efectivos,
- timestamp de calculo.

### Administracion plataforma

Endpoints internos sugeridos para permisos de plataforma:

- Consultar suscripciones de un tenant.
- Activar/suspender/reactivar producto.
- Activar/suspender/reactivar modulo.
- Consultar historial de cambios.

No se habilita self-service comercial en Sprint 3.

## Criterios de calidad

- Toda query tenant-scoped debe filtrar por tenant.
- Todo cambio de licencia o entitlement debe auditarse.
- Los guards deben tener tests de permiso y modulo por separado.
- Las APIs deben usar DTOs y Bean Validation.
- Las migraciones deben ser idempotentes en datos seed cuando aplique.
- Ninguna pantalla frontend debe decidir seguridad.
- README, changelog y docs deben actualizarse en cada hito.

## Riesgos y decisiones a cuidar

- No confundir planes comerciales con permisos RBAC.
- No permitir que `TENANT_OWNER` se auto-habilite modulos pagos.
- No romper el tenant KODA durante la transicion.
- No convertir feature flags en licencias paralelas.
- No aplicar limites caros de calcular sin indices o estrategia de cache.
- No acoplar el frontend a codigos temporales que despues cambien.

## Fuera de alcance

- UI completa de operacion comercial.
- Billing real.
- Pasarela de pago.
- Facturas de suscripcion SaaS.
- Precios finales.
- Marketplace.
- Nuevos modulos ERP.
- Facturacion fiscal o contabilidad.

## Siguiente paso recomendado

Avanzar al Hito 4: implementar guards backend reutilizables por producto/modulo y aplicarlos a los modulos existentes sin mezclar RBAC con licenciamiento.