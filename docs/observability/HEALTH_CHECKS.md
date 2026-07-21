# Health Checks Operativos

## Estado

Sprint 4 Hito 3 implementado.

## Objetivo

Separar senales de vida del proceso, preparacion para recibir trafico y salud de dependencias criticas.

Un health check no es un dashboard ni una auditoria. Es un semaforo operativo para decidir si la instancia debe seguir recibiendo trafico.

## Endpoints

Endpoints publicos permitidos sin autenticacion:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/info`

Endpoints Actuator no incluidos en `management.endpoints.web.exposure.include` no quedan expuestos por este hito.

## Liveness

Endpoint:

- `GET /actuator/health/liveness`

Componentes:

- `livenessState`

Uso:

- Indica si el proceso de la aplicacion esta vivo.
- No consulta PostgreSQL.
- No consulta Flyway/schema.
- No debe usarse para decidir si la instancia esta lista para recibir trafico.

Si liveness cae, el orquestador futuro puede reiniciar la instancia.

## Readiness

Endpoint:

- `GET /actuator/health/readiness`

Componentes:

- `readinessState`
- `db`
- `kodaSchema`

Uso:

- Indica si la instancia puede recibir trafico.
- Verifica conectividad con PostgreSQL mediante `db`.
- Verifica estado de migraciones/schema mediante `kodaSchema`.

Si readiness cae, la instancia debe salir de rotacion de trafico sin necesariamente reiniciar el proceso.

## KODA Schema Health

Componente:

- `kodaSchema`

Reglas:

- `UP`: Flyway tiene una version actual y no hay migraciones pendientes.
- `OUT_OF_SERVICE`: existen migraciones pendientes.
- `DOWN`: el schema no esta inicializado o Flyway no puede resolver el estado.

El componente esta activo cuando `spring.flyway.enabled=true`, que es el comportamiento por defecto del backend.

La configuracion real valida que los miembros declarados en readiness existan. Si `db` o `kodaSchema` faltan, la aplicacion debe fallar temprano en lugar de publicar una readiness incompleta.

No se exponen mensajes internos de excepciones en el health indicator. Se reporta solo el tipo de error para evitar filtrar URLs, credenciales, hosts o detalles de infraestructura.

## Seguridad

Configuracion aplicada:

- `show-details: never`
- `show-components: always`
- `validate-group-membership: true`

Esto permite ver que componente esta caido sin revelar detalles sensibles.

No se exponen:

- credenciales,
- URLs internas completas,
- mensajes de excepcion,
- variables de entorno,
- configuracion de datasource,
- datos de tenants.

## Interpretacion Operativa

| Senal | Interpretacion | Accion recomendada |
| --- | --- | --- |
| `liveness=UP`, `readiness=UP` | Instancia sana y lista. | Mantener trafico. |
| `liveness=UP`, `readiness=DOWN/OUT_OF_SERVICE` | Proceso vivo, pero no listo. | Sacar de trafico y revisar dependencias. |
| `db=DOWN` | PostgreSQL no responde o no es accesible. | Revisar base, red, credenciales y pool. |
| `kodaSchema=OUT_OF_SERVICE` | Hay migraciones pendientes. | Revisar pipeline/migraciones antes de recibir trafico. |
| `kodaSchema=DOWN` | Schema no inicializado o Flyway no pudo leer estado. | Bloquear trafico y diagnosticar migraciones/schema. |
| `liveness=DOWN` | Proceso no saludable. | Reiniciar instancia o investigar bloqueo severo. |

## Verificacion Local

Con backend levantado:

```powershell
curl.exe http://localhost:8080/actuator/health/liveness
curl.exe http://localhost:8080/actuator/health/readiness
```

Respuesta esperada en entorno sano:

```json
{
  "status": "UP",
  "components": {
    "readinessState": {
      "status": "UP"
    },
    "db": {
      "status": "UP"
    },
    "kodaSchema": {
      "status": "UP"
    }
  }
}
```

## Tests

Cobertura agregada:

- endpoints `/actuator/health/liveness` y `/actuator/health/readiness` publicos sin autenticacion,
- ocultamiento de detalles en health responses,
- `kodaSchema` `UP` cuando Flyway esta al dia,
- `kodaSchema` `OUT_OF_SERVICE` con migraciones pendientes,
- `kodaSchema` `DOWN` si schema no esta inicializado,
- sanitizacion de errores Flyway,
- integracion con PostgreSQL 17 real mediante Testcontainers.

## Decision Tecnica

No se agregan proveedores externos ni dashboards en este hito.

Primero se define una senal interna confiable. En Hito 4 se avanzara con metricas base; ahi se decidira que datos conviene exponer como metricas sin generar cardinalidad explosiva.
