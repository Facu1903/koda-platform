# Frontend Regional Formatting

## Estado

Implementado en Sprint 5 Hito 4.

## Objetivo

Centralizar el formato regional visible del frontend para que fechas, horas, numeros y moneda se rendericen segun la configuracion runtime del tenant autenticado.

La regla de producto es directa: el formato cambia por empresa, el dato persistido no. Un importe sigue siendo numero mas codigo de moneda; el frontend solo decide como mostrarlo.

## Componentes

- `frontend/src/platform/configuration/regionalFormatting.ts`: fabrica pura de formateadores regionales.
- `frontend/src/platform/configuration/useRegionalFormatters.ts`: hook React conectado a `CompanyProfileProvider`.
- `frontend/src/app/App.tsx`: shell operativo usando formateo regional en dashboard y vigencia de modulos.

## Datos usados

El formateo usa el bloque `regional` de `GET /api/v1/company/profile`:

- `defaultLocale`
- `defaultCurrency`
- `timeZone`
- `dateFormat`
- `timeFormat`
- `numberLocale`
- `currencyFormat`

## Reglas aplicadas

- Fechas y horas se calculan con `timeZone` del tenant.
- Numeros se muestran con `numberLocale`.
- Moneda se muestra con `defaultCurrency`, salvo que una pantalla futura pase una moneda especifica de operacion.
- `currencyFormat` soporta `symbol`, `code`, `name` y `narrowSymbol`.
- `dateFormat` y `timeFormat` soportan un subconjunto seguro de patrones Java `DateTimeFormatter` para display operativo.
- Si un patron no es compatible con el frontend, se usa `Intl` como fallback.
- Si el perfil regional es invalido o incompleto, se usa fallback KODA.
- Valores vacios o invalidos devuelven etiquetas controladas, no excepciones visibles.

## Patrones soportados

Fecha:

- `d`
- `dd`
- `M`
- `MM`
- `MMM`
- `MMMM`
- `yy`
- `yyyy`

Hora:

- `H`
- `HH`
- `h`
- `hh`
- `m`
- `mm`
- `s`
- `ss`
- `a`

Literales seguros:

- espacio
- `.`
- `,`
- `/`
- `:`
- `-`
- texto entre comillas simples

## Decision tecnica

El backend valida `dateFormat` y `timeFormat` como patrones Java. El frontend no intenta replicar todo `DateTimeFormatter`; hacerlo completo en TypeScript seria fragil y costoso. En su lugar, soporta los tokens operativos aprobados para KODA y usa `Intl` como red de seguridad.

Esta decision evita duplicar logica regional compleja, mantiene compatibilidad con navegadores modernos y deja abierta una evolucion futura hacia una libreria especializada si el producto necesita calendarios o formatos avanzados.

## Alcance actual

Incluido:

- utilidades centralizadas de fecha, fecha/hora, hora, numero y moneda,
- hook tenant-aware,
- conexion inicial al shell operativo,
- fallback seguro,
- tests frontend.

No incluido:

- conversion de moneda,
- reglas contables,
- traduccion integral de textos,
- preferencias regionales por usuario,
- inputs de fecha/moneda editables en formularios administrativos.

## Validacion

- `npm.cmd run test`: 9 pruebas, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.
