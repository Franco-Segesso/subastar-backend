# Asistente administrativo de Subastar

Este asistente permite:

- revisar la documentación de clientes;
- aprobarlos y asignarles una categoría;
- rechazarlos indicando un motivo;
- enviar correos de aprobación o rechazo;
- verificar cheques certificados;
- consultar, desactivar o reactivar medios de pago;
- actualizar los fondos reservados de cuentas bancarias.

## Configuración

El archivo `.env` contiene las credenciales locales y no debe subirse a Git.
El archivo `.env.example` sirve como plantilla para el equipo.

## Ejecución

Desde esta carpeta:

```powershell
python admitir_cliente.py
```

Requiere Python, `pyodbc` y ODBC Driver 18 for SQL Server.

Las tarjetas se validan por sus datos y vencimiento. Las cuentas se validan
por moneda y fondos reservados. Ninguna de las dos tiene estado administrativo
de verificacion.

Solamente los cheques certificados quedan pendientes hasta que la empresa
confirma su recepcion y verificacion. El backend bloquea su uso mientras
`verificado` sea `no`.
