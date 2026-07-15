# KODA PLATFORM - Contributing

## 1. Proposito

Este documento define como trabajar en KODA PLATFORM sin romper su arquitectura ni convertir el proyecto en una coleccion de ocurrencias bien intencionadas.

## 2. Flujo de trabajo

1. Confirmar alcance funcional aprobado.
2. Revisar documentos fundacionales.
3. Disenar cambio tecnico si afecta arquitectura.
4. Implementar en incrementos pequenos.
5. Agregar o actualizar tests.
6. Ejecutar verificaciones locales.
7. Actualizar documentacion.
8. Actualizar `CHANGELOG.md`.
9. Abrir pull request.
10. Revisar y fusionar.

## 3. Reglas de aprobacion

Requieren aprobacion del Product Owner:

- Cambios de reglas funcionales.
- Cambios de alcance de sprint.
- Cambios de comportamiento visible para usuarios.
- Cambios en modelo comercial, licencias o modulos.
- Eliminacion de funcionalidades.
- Cambios breaking en API.

Requieren propuesta tecnica previa:

- Cambios de arquitectura.
- Cambios en estrategia multi-tenant.
- Cambios de stack.
- Cambios de seguridad.
- Cambios de persistencia relevantes.
- Cambios que afecten performance, escalabilidad o mantenibilidad.

## 4. Git

### Ramas

Ramas recomendadas:

```text
main
develop
feature/<scope>
fix/<scope>
docs/<scope>
chore/<scope>
```

### Politica

- `main` debe representar estado estable.
- `develop` integra trabajo aprobado para el proximo incremento.
- Las ramas de feature salen de `develop`.
- No se trabaja directo sobre `main`.
- Pull request obligatorio para fusionar.

## 5. Commits

Formato:

```text
type(scope): summary
```

Ejemplos:

```text
docs(project): add foundational documents
feat(auth): implement login endpoint
fix(stock): prevent negative movement without permission
test(tenants): verify cross-tenant isolation
```

## 6. Pull requests

Cada PR debe incluir:

- Objetivo.
- Cambios principales.
- Tests ejecutados.
- Riesgos.
- Documentacion actualizada.
- Capturas o evidencia cuando afecte UI.

No se acepta un PR que diga solo "changes". Eso no es una descripcion; es una rendicion.

## 7. Testing requerido

Todo cambio debe tener pruebas proporcionales al riesgo.

Minimo:

- Reglas de negocio: unit tests.
- Persistencia: integration tests cuando haya consultas complejas.
- Seguridad: tests de permisos y tenant isolation.
- API: tests de endpoints criticos.
- Frontend: tests de componentes/flujos relevantes.

## 8. Seguridad

Antes de cerrar una tarea, verificar:

- No se exponen datos de otro tenant.
- No se confia en `tenant_id` del cliente.
- No se loguean secretos.
- No se devuelven entidades internas.
- No se agregan endpoints sin autorizacion.
- No se crean permisos implicitos sin documentar.

## 9. Documentacion

Debe actualizarse:

- `README.md` para uso general.
- `ARCHITECTURE.md` si cambia arquitectura.
- `CODING_STANDARDS.md` si cambia convencion.
- `ROADMAP.md` si cambia plan aprobado.
- `CHANGELOG.md` por cada cambio relevante.
- Diagramas si cambia flujo o estructura.

## 10. Definition of Done

Una tarea se considera terminada si:

- Cumple alcance aprobado.
- Compila.
- Ejecuta.
- Tiene tests.
- Respeta arquitectura.
- Respeta multi-tenancy.
- Actualiza documentacion.
- Actualiza changelog.
- No introduce deuda critica.

## 11. Manejo de deuda tecnica

La deuda tecnica debe registrarse con:

- Descripcion.
- Motivo.
- Riesgo.
- Impacto.
- Plan de resolucion.

La deuda invisible es la peor: no grita, pero cobra intereses.

