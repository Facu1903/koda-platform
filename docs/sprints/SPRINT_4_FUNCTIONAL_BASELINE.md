# Sprint 4 - Functional Baseline

## Estado

Aprobado por Product Owner el 2026-07-21.

## Decision aprobada

Sprint 4 se define como: **Escalabilidad, Observabilidad y Endurecimiento SaaS Operativo**.

## Objetivo

Preparar KODA PLATFORM para operar como SaaS real: medible, trazable, diagnosticable y mas resistente antes de seguir agregando grandes superficies funcionales.

Sprint 4 no busca sumar mas modulos ERP ni construir UI operativa completa. Busca que la plataforma deje de depender de intuicion para saber que paso, donde fallo, cuanto tarda, que tenant fue afectado y que componente necesita atencion.

La regla brutalmente simple: si no se puede observar, no se puede operar a escala. Y si no se puede operar a escala, todavia no es SaaS serio.

## Problema a resolver

Sprint 3 dejo una base SaaS comercial gobernable: productos, modulos, licencias, capabilities y guards.

Pero una plataforma con miles de tenants necesita ademas:

- trazabilidad por request,
- logs consistentes,
- metricas confiables,
- health checks utiles,
- consultas criticas revisadas,
- cache controlado para lecturas calientes,
- estrategia inicial para auditoria creciente,
- criterios de diagnostico sin exponer datos sensibles.

Sin esto, cada incidente futuro obliga a mirar la base, leer logs incompletos y adivinar. Esa forma de operar funciona hasta que deja de funcionar justo el dia que mas importa.

## Principios de Sprint 4

- Observabilidad no reemplaza seguridad.
- Logs y metricas no deben exponer secretos, tokens, passwords, datos fiscales ni informacion sensible innecesaria.
- Todo request debe poder correlacionarse de punta a punta dentro del backend.
- Los errores deben conservar codigo estructurado y trazabilidad.
- Las metricas deben tener cardinalidad controlada; no se deben crear etiquetas explosivas por usuario, path dinamico o tenant sin estrategia.
- Health checks deben distinguir liveness, readiness y dependencias criticas sin filtrar datos internos.
- Performance se mejora midiendo, no adivinando.
- Cache nunca es fuente de verdad; la base de datos sigue siendo la fuente autoritativa.
- Toda invalidacion de cache ligada a licencias debe ser explicita o conservadora.
- La auditoria debe seguir siendo confiable sin convertirse en una tabla infinita imposible de operar.

## Alcance aprobado

Sprint 4 incluye:

- Correlation/request ID por request.
- Propagacion de correlation ID en respuesta HTTP.
- Enriquecimiento de logs estructurados con correlation ID, tenant, usuario y ruta cuando aplique.
- Politica de sanitizacion para logs y errores.
- Health checks expandidos para aplicacion, base de datos, migraciones y readiness.
- Separacion conceptual de liveness y readiness.
- Metricas base con Actuator/Micrometer.
- Metricas HTTP y de runtime.
- Lineamientos de metricas por tenant con cardinalidad controlada.
- Revision de queries e indices criticos de licencias, capabilities, auditoria y reportes.
- Cache tecnico inicial para capabilities, con invalidacion ante cambios administrativos de licencias.
- Estrategia inicial de retencion/crecimiento de auditoria.
- Documentacion de operacion local y criterios de diagnostico.
- Tests para los componentes incorporados.

## Reglas funcionales aprobadas

### Correlation ID

- Si el request trae un header de correlacion valido, se reutiliza.
- Si el request no lo trae, el backend genera uno.
- La respuesta debe devolver el correlation ID usado.
- El correlation ID debe aparecer en logs estructurados del request.
- No debe usarse como mecanismo de autenticacion ni autorizacion.

Header aprobado inicialmente:

- `X-Correlation-ID`

### Logs estructurados

- Los logs deben seguir formato estructurado compatible con ingestion futura.
- Los logs de requests autenticados deben incluir tenant y usuario cuando esten disponibles.
- Los logs no deben incluir access tokens, refresh tokens, passwords, secretos, headers sensibles ni cuerpos completos de requests.
- Los errores funcionales deben mantener codigo estructurado.
- Los logs de auditoria funcional no se reemplazan por logs tecnicos.

### Health checks

Sprint 4 debe diferenciar:

- liveness: el proceso esta vivo,
- readiness: la aplicacion esta lista para recibir trafico,
- dependency health: dependencias criticas como PostgreSQL y Flyway/schema.

Los health checks publicos no deben revelar detalles sensibles de infraestructura.

### Metricas

Metricas iniciales candidatas:

- requests HTTP por estado,
- latencia HTTP,
- errores por codigo,
- uso de JVM,
- estado de conexiones,
- health de dependencias,
- cache hits/misses de capabilities si se implementa cache,
- operaciones administrativas de licencia por resultado.

No se aprueban metricas de alta cardinalidad sin decision tecnica explicita.

### Cache de capabilities

- Puede cachearse el calculo de capabilities para reducir lecturas repetidas.
- La fuente de verdad sigue siendo PostgreSQL.
- Cambios administrativos de licencias deben invalidar el cache afectado.
- Si no se puede invalidar con precision, debe usarse TTL conservador o cache por request.
- Un cache vencido no debe permitir acceso a modulos que el backend ya bloqueo.

### Performance e indices

Sprint 4 debe revisar como minimo:

- consultas de `GET /api/v1/capabilities`,
- guards de licencia por modulo,
- busquedas de auditoria,
- reportes operativos,
- indices de tablas que creceran por tenant.

No se deben agregar indices por estetica. Cada indice debe justificar lectura, cardinalidad y costo de escritura.

### Auditoria operativa

- Sprint 4 no elimina auditoria existente.
- Debe proponer una estrategia inicial de retencion y crecimiento.
- Puede documentar particionamiento futuro, pero no debe implementarlo sin necesidad tecnica clara y aprobacion.

## Fuera de alcance de Sprint 4

- Nuevos modulos ERP funcionales.
- UI completa de operacion.
- UI visual de observabilidad.
- APM comercial externo obligatorio.
- Prometheus/Grafana productivo obligatorio.
- Kubernetes o cloud deployment.
- Billing real.
- Facturacion SaaS.
- Row Level Security en PostgreSQL.
- Reescritura general de arquitectura.
- Optimizacion prematura de todo el sistema.

## Criterios de aceptacion funcional

Sprint 4 se considerara funcionalmente aceptable si:

- Cada request puede trazarse con correlation ID.
- Los logs tecnicos incluyen datos utiles sin exponer informacion sensible.
- Existen health checks utiles para diagnostico local y futuro despliegue.
- Existen metricas base consumibles por Actuator/Micrometer.
- Capabilities tiene una estrategia de cache segura o una decision documentada de no cachear todavia.
- Las queries criticas de licenciamiento, auditoria y reportes quedan revisadas.
- Auditoria tiene estrategia inicial documentada de crecimiento/retencion.
- Tests relevantes pasan.
- README, roadmap, changelog y documentacion especifica quedan actualizados.

## Decision

Se aprueba esta base funcional para iniciar Sprint 4.

Cualquier cambio que incorpore infraestructura externa obligatoria, altere seguridad, exponga datos sensibles en logs/metricas o modifique reglas funcionales de negocio requiere aprobacion explicita del Product Owner antes de implementarse.
