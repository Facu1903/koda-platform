# Correlation ID y Logs Estructurados

## Estado

Sprint 4 Hito 2 implementado.

## Objetivo

Cada request HTTP debe poder rastrearse de punta a punta sin exponer datos sensibles y sin depender de informacion manual del usuario.

El backend de KODA PLATFORM usa correlation ID como identificador tecnico de diagnostico. No reemplaza auditoria funcional, no modifica permisos y no cambia el aislamiento multiempresa.

## Contrato HTTP

Header estandar:

- `X-Correlation-ID`

Reglas:

- Si el cliente envia un `X-Correlation-ID` valido, el backend lo reutiliza.
- Si falta, esta vacio o tiene formato inseguro, el backend genera un UUID.
- El backend siempre devuelve el correlation ID efectivo en el header de respuesta `X-Correlation-ID`.
- El valor aceptado debe tener entre 8 y 128 caracteres.
- El valor aceptado solo puede contener letras, numeros, punto, guion bajo, dos puntos y guion medio.

Esta regla evita IDs gigantes, caracteres de control y entradas utiles para ensuciar logs.

## Contexto De Logs

El backend escribe logs JSON mediante Logstash Encoder y publica el MDC en la salida.

Campos transversales:

- `correlationId`
- `httpMethod`
- `httpPath`
- `httpStatus`
- `httpDurationMs`
- `tenantId`, cuando el request tenga principal KODA autenticado.
- `userId`, cuando el request tenga principal KODA autenticado.
- `platformAdmin`, cuando el request tenga principal KODA autenticado.

Evento tecnico de cierre por request:

- `http.request.completed`

El path usa el patron normalizado de Spring MVC cuando esta disponible. Ejemplo: `/api/v1/products/{id}` en lugar de loguear cada ID real como ruta distinta.

## Seguridad Y Sanitizacion

No se loguean:

- access tokens,
- refresh tokens,
- passwords,
- secretos,
- headers sensibles,
- cuerpos completos de request o response,
- datos comerciales completos.

El correlation ID es un identificador tecnico de trazabilidad. No debe contener informacion personal, datos comerciales ni secretos.

## Relacion Con Tenant Context

El correlation ID se resuelve antes de seguridad para que tambien existan logs utiles en requests anonimos o rechazados.

El Tenant Context sigue resolviendose exclusivamente desde el principal autenticado de KODA. El frontend no puede fijar `tenantId` por header, query param ni cuerpo.

Cuando existe un principal autenticado, el filtro de Tenant Context agrega `tenantId`, `userId` y `platformAdmin` al MDC durante el procesamiento del request y conserva esos valores como atributos internos para el log final.

## Uso Operativo

Ante un incidente:

1. Tomar el `X-Correlation-ID` devuelto por la API.
2. Buscar ese `correlationId` en los logs.
3. Revisar el evento `http.request.completed` para metodo, path, estado y duracion.
4. Revisar logs internos del mismo `correlationId` para ver reglas, errores o bloqueos.

Este ID no debe usarse como etiqueta de metricas de alta cardinalidad. Pertenece a logs/tracing, no a dashboards agregados.

## Tests

Cobertura agregada:

- reutilizacion de correlation ID valido,
- generacion si falta,
- rechazo de correlation ID inseguro,
- limpieza de MDC al finalizar,
- status `500` cuando el request falla antes de fijar respuesta,
- evento de cierre con campos HTTP y tenant/user cuando existen,
- enriquecimiento y limpieza de MDC en Tenant Context.
