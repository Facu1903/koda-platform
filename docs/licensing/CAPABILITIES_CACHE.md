# Cache Seguro de Capabilities

## Estado

Implementado en Sprint 4 Hito 6.

## Objetivo

Reducir lecturas repetidas de `GET /api/v1/capabilities` sin relajar seguridad ni convertir el cache en fuente de verdad.

PostgreSQL sigue siendo la fuente autoritativa. El cache solo guarda snapshots efectivos de capabilities por tenant durante una ventana corta y controlada.

## Alcance implementado

El cache aplica a:

- resolucion de capabilities efectivas del tenant autenticado,
- productos habilitados,
- modulos habilitados,
- feature flags efectivos,
- limites efectivos.

No aplica a:

- autorizacion backend de operaciones,
- guards de licencia por modulo,
- permisos RBAC,
- auditoria,
- login o JWT.

Decision de seguridad: los guards backend siguen consultando el repositorio de licencia directamente. Si una entrada cacheada queda vieja durante pocos segundos, la UI podria mostrar una opcion de mas, pero el backend no debe permitir una operacion que ya no corresponde.

## Configuracion

Propiedades:

```yaml
koda:
  licensing:
    capabilities-cache:
      enabled: true
      ttl: 30s
      max-size: 10000
```

Variables de entorno:

- `KODA_CAPABILITIES_CACHE_ENABLED`
- `KODA_CAPABILITIES_CACHE_TTL`
- `KODA_CAPABILITIES_CACHE_MAX_SIZE`

Valores por defecto:

- habilitado,
- TTL de 30 segundos,
- maximo 10000 tenants cacheados por instancia.

## Vencimiento seguro

La expiracion real de cada entrada es:

```text
min(now + ttl, proximo valid_until efectivo conocido)
```

Se consideran vencimientos de:

- suscripcion de producto,
- entitlement de producto,
- entitlement de modulo,
- feature flag,
- override de limite.

Esto evita que un snapshot quede cacheado mas alla de una fecha de vencimiento conocida.

## Invalidacion explicita

Las operaciones administrativas de licencia invalidan el tenant afectado despues del commit cuando la escritura fue exitosa:

- actualizacion de suscripcion,
- actualizacion de entitlement de producto,
- actualizacion de entitlement de modulo.

Si una escritura falla por version optimista, validacion o permiso, no se invalida porque no hubo cambio efectivo.

Regla para futuros hitos: toda nueva escritura que cambie feature flags, limites, plan, suscripcion o estado comercial de licencia debe llamar a `TenantCapabilitiesCache.evict(tenantId)`.

## Metricas

El cache publica la metrica:

- `koda.capabilities.cache.requests`

Tag estable:

- `result`

Valores posibles:

- `hit`
- `miss`
- `expired`
- `evicted`
- `skipped`

No se usa `tenantId`, `userId`, `correlationId` ni ningun identificador dinamico como tag.

## Riesgos y tradeoffs

- Cache local por instancia: suficiente para desarrollo y una primera operacion simple; en despliegues con varias instancias, cada instancia mantiene su propio cache.
- TTL corto: reduce riesgo de datos viejos, pero no elimina completamente cambios externos fuera del flujo administrativo.
- Guards sin cache: mas lecturas de base, pero autorizacion mas fresca. Para Sprint 4 es la decision correcta.
- Cache distribuido: queda fuera de alcance hasta definir infraestructura productiva.

## Validacion

Pruebas agregadas o actualizadas:

- `TenantCapabilitiesServiceTest`: cache dentro del TTL y vencimiento por `valid_until`.
- `TenantLicenseAdministrationServiceTest`: invalidacion tras escrituras exitosas y no invalidacion ante conflicto.
- `InMemoryTenantCapabilitiesCacheTest`: hit, expiracion, eviccion y modo deshabilitado.

Comando de validacion del hito:

```powershell
mvn -B "-Dtest=TenantCapabilitiesServiceTest,TenantLicenseAdministrationServiceTest,InMemoryTenantCapabilitiesCacheTest" test
```

Resultado: 15 pruebas, 0 fallos.

Validacion completa de backend:

```powershell
mvn -B verify
```

Resultado: 137 pruebas unitarias y 14 pruebas de integracion, 0 fallos.
