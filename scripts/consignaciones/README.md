# Asistente de consignaciones

Este script reemplaza la edicion manual de los SQL para avanzar consignaciones.

## Ejecutar

Desde la carpeta `subastar-backend`:

```powershell
python scripts/consignaciones/gestionar_consignaciones.py
```

El programa:

1. Lista las consignaciones.
2. Pide el ID que queres gestionar.
3. Detecta la instancia actual.
4. Pide solamente los datos necesarios.
5. Muestra un resumen y solicita confirmacion antes de modificar Azure.

Para listar sin modificar nada:

```powershell
python scripts/consignaciones/gestionar_consignaciones.py --listar
```

Para abrir directamente una consignacion:

```powershell
python scripts/consignaciones/gestionar_consignaciones.py --id 12
```

## Instancias

- `Solicitud enviada`: pide el deposito.
- `Recibido en deposito`: permite aceptar, rechazar o solicitar documentación.
- `Documentacion pendiente`: espera que el dueño adjunte archivos desde Android.
- `Documentacion presentada`: muestra los archivos para aprobarlos o rechazar el bien.
- `Inspeccionado y aceptado`: pide subasta, precio base y seguro. La comision
  es siempre 15%.

El item se incorpora al catalogo solamente cuando el cliente acepta las
condiciones desde Android.

Para enviar push notifications desde el script:

```powershell
python -m pip install firebase-admin
```
- Si no existe una subasta pendiente, permite crearla con su catalogo desde el
  mismo asistente.
- `Esperando aceptacion`: no modifica nada; el duenio responde desde Android.
- `Asignada a subasta`: flujo administrativo completado.

## Dependencia

Si Python indica que falta `pyodbc`:

```powershell
python -m pip install pyodbc
```

La conexion se lee automáticamente desde
`src/main/resources/application.properties`.
