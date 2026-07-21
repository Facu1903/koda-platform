# Metricas Operativas Base

## Estado

Implementado en Sprint 4 Hito 4.

## Objetivo

Exponer senales operativas base de KODA PLATFORM mediante Spring Boot Actuator y Micrometer, sin incorporar aun dependencias externas obligatorias como Prometheus, Grafana o APM comercial.

La prioridad de este hito no es tener dashboards lindos. La prioridad es no operar a ciegas y, al mismo tiempo, evitar metricas con cardinalidad explosiva.

## Endpoints

Actuator expone:

- `/actuator/metrics`
- `/actuator/metrics/{metric.name}`

Los endpoints de metricas estan expuestos, pero no son publicos. Requieren autenticacion.

Decision tecnica:

- Health checks publicos: si.
- Metricas publicas: no.

Motivo: las metricas pueden revelar nombres internos, volumen operativo, patrones de errores y detalles de runtime que no deben estar disponibles anonimamente.

## Metricas base disponibles

Spring Boot y Micrometer publican metricas operativas como:

- `http.server.requests`: requests HTTP, latencia, status, metodo, resultado y URI normalizada.
- `jvm.memory.used`: uso de memoria JVM.
- `jvm.threads.live`: threads vivos.
- `jvm.gc.pause`: pausas de garbage collector.
- `process.uptime`: tiempo vivo del proceso.
- `system.cpu.usage`: uso CPU del sistema si esta disponible.
- `hikaricp.connections.active`: conexiones activas del pool cuando hay datasource Hikari disponible.

La disponibilidad exacta depende del perfil y de si la aplicacion corre con base de datos real.

## Distribucion HTTP

`http.server.requests` queda configurada con:

- histogramas percentiles habilitados,
- percentiles `p50`, `p95` y `p99`,
- buckets SLO: `100ms`, `250ms`, `500ms`, `1s`, `2s`, `5s`.

Esto permite responder preguntas operativas iniciales:

- cuantos requests esta recibiendo la API,
- que endpoints son mas lentos,
- cuanto demora la mediana,
- cuanto demora el percentil 95 y 99,
- que codigos HTTP se estan devolviendo,
- si la degradacion es general o concentrada en una ruta.

## Tags comunes

Todas las metricas reciben el tag comun:

- `application`

Este tag identifica la aplicacion sin generar cardinalidad alta.

## Politica de cardinalidad

Las metricas deben agrupar comportamiento, no identificar eventos individuales.

Tags permitidos recomendados:

- `method`
- `uri` normalizada
- `status`
- `outcome`
- `exception`
- `module`
- `operation`
- `result`

Tags bloqueados por politica tecnica:

- `tenantId`
- `tenant_id`
- `userId`
- `user_id`
- `correlationId`
- `correlation_id`
- `requestId`
- `request_id`
- `sessionId`
- `session_id`
- `email`
- `token`
- `jwt`
- `authorization`

Tambien quedan prohibidos como tags:

- IDs de recursos de negocio,
- UUIDs dinamicos,
- nombres de personas,
- emails,
- codigos fiscales,
- documentos,
- valores libres enviados por usuario.

## Guardrails implementados

La clase `KodaMetricsConfiguration` aplica dos defensas:

- Deniega medidores que intenten registrar tags sensibles o de alta cardinalidad.
- Limita `http.server.requests` a un maximo de 100 valores distintos para el tag `uri`.

Esto no reemplaza criterio de diseno, pero evita que un error de instrumentacion deteriore memoria, almacenamiento y costos de monitoreo cuando la plataforma escale.

## Relacion con logs y auditoria

Cada herramienta tiene un rol distinto:

- Metricas: tendencias agregadas y alertas operativas.
- Logs: diagnostico tecnico por request usando `correlationId`.
- Auditoria: trazabilidad funcional persistente de acciones de negocio.

No se debe usar `tenantId`, `userId` o `correlationId` como tags de metricas. Para investigar una operacion concreta se usa el correlation ID en logs y la auditoria persistente.

## Validacion

Pruebas agregadas:

- `KodaMetricsConfigurationTest`: valida bloqueo de tags de alta cardinalidad, tags operativos estables y limite de cardinalidad para `http.server.requests`.
- `KodaPlatformApplicationTests`: valida que `/actuator/metrics` requiera autenticacion y que `http.server.requests` este disponible con tags estables.

Comando de validacion del hito:

```powershell
mvn -B "-Dtest=KodaMetricsConfigurationTest,KodaPlatformApplicationTests" test
mvn -B verify
```

Validacion completa final: 130 pruebas unitarias y 13 pruebas de integracion, 0 fallos.
