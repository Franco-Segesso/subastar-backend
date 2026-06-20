import os
import smtplib
import sys
import webbrowser
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import date
from decimal import Decimal, InvalidOperation
from email.message import EmailMessage
from pathlib import Path

import pyodbc


BASE_DIR = Path(__file__).resolve().parent
CATEGORIAS = ("comun", "especial", "plata", "oro", "platino")


def configurar_consola():
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")


def cargar_archivo_env(ruta):
    if not ruta.exists():
        return
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if not linea or linea.startswith("#") or "=" not in linea:
            continue
        clave, valor = linea.split("=", 1)
        os.environ.setdefault(
            clave.strip(), valor.strip().strip('"').strip("'")
        )


def variable_requerida(nombre):
    valor = os.getenv(nombre, "").strip()
    if not valor:
        raise RuntimeError(
            f"Falta la variable {nombre}. Revisá {BASE_DIR / '.env'}."
        )
    return valor


@dataclass(frozen=True)
class Configuracion:
    db_driver: str
    db_server: str
    db_database: str
    db_username: str
    db_password: str
    db_trust_certificate: str
    smtp_host: str
    smtp_port: int
    smtp_email: str
    smtp_app_password: str
    verificador_id: int

    @classmethod
    def desde_entorno(cls):
        cargar_archivo_env(BASE_DIR / ".env")
        return cls(
            db_driver=os.getenv(
                "DB_DRIVER", "ODBC Driver 18 for SQL Server"
            ),
            db_server=variable_requerida("DB_SERVER"),
            db_database=variable_requerida("DB_DATABASE"),
            db_username=variable_requerida("DB_USERNAME"),
            db_password=variable_requerida("DB_PASSWORD"),
            db_trust_certificate=os.getenv(
                "DB_TRUST_SERVER_CERTIFICATE", "yes"
            ),
            smtp_host=os.getenv("SMTP_HOST", "smtp.gmail.com"),
            smtp_port=int(os.getenv("SMTP_PORT", "465")),
            smtp_email=variable_requerida("SMTP_EMAIL"),
            smtp_app_password=variable_requerida("SMTP_APP_PASSWORD"),
            verificador_id=int(os.getenv("ADMIN_VERIFICADOR_ID", "1")),
        )

    def cadena_conexion(self):
        return (
            f"DRIVER={{{self.db_driver}}};"
            f"SERVER={self.db_server};"
            f"DATABASE={self.db_database};"
            f"UID={self.db_username};"
            f"PWD={self.db_password};"
            f"TrustServerCertificate={self.db_trust_certificate};"
            "Encrypt=yes;"
        )


@contextmanager
def conexion_db(config):
    conexion = pyodbc.connect(config.cadena_conexion(), timeout=15)
    try:
        yield conexion
    except Exception:
        conexion.rollback()
        raise
    finally:
        conexion.close()


def filas_como_diccionarios(cursor):
    columnas = [columna[0] for columna in cursor.description]
    return [dict(zip(columnas, fila)) for fila in cursor.fetchall()]


def fila_como_diccionario(cursor):
    fila = cursor.fetchone()
    if fila is None:
        return None
    columnas = [columna[0] for columna in cursor.description]
    return dict(zip(columnas, fila))


def pedir_entero(mensaje):
    while True:
        try:
            return int(input(mensaje).strip())
        except ValueError:
            print("Ingresá un número válido.")


def pedir_decimal(mensaje, valor_actual=None):
    while True:
        sufijo = f" [{valor_actual}]" if valor_actual is not None else ""
        valor = input(f"{mensaje}{sufijo}: ").strip().replace(",", ".")
        if not valor and valor_actual is not None:
            return Decimal(str(valor_actual))
        try:
            numero = Decimal(valor)
            if numero < 0:
                raise InvalidOperation
            return numero
        except InvalidOperation:
            print("Ingresá un importe válido mayor o igual que cero.")


def confirmar(mensaje):
    return input(f"{mensaje} [s/N]: ").strip().lower() in ("s", "si", "sí")


def texto(valor, defecto="-"):
    if valor is None or str(valor).strip() == "":
        return defecto
    return str(valor)


def enviar_email(config, destino, asunto, cuerpo):
    mensaje = EmailMessage()
    mensaje["Subject"] = asunto
    mensaje["From"] = config.smtp_email
    mensaje["To"] = destino
    mensaje.set_content(cuerpo.strip())

    with smtplib.SMTP_SSL(
        config.smtp_host, config.smtp_port, timeout=20
    ) as servidor:
        servidor.login(config.smtp_email, config.smtp_app_password)
        servidor.send_message(mensaje)


def enviar_email_sin_interrumpir(config, destino, asunto, cuerpo):
    try:
        enviar_email(config, destino, asunto, cuerpo)
        print(f"Correo enviado correctamente a {destino}.")
    except Exception as error:
        print(
            "La operación se guardó, pero no se pudo enviar el correo: "
            f"{error}"
        )


def validar_verificador(cursor, verificador_id):
    cursor.execute(
        "SELECT identificador FROM empleados WHERE identificador = ?",
        verificador_id,
    )
    if cursor.fetchone() is None:
        raise RuntimeError(
            f"No existe el empleado verificador con ID {verificador_id}."
        )


CONSULTA_CLIENTE = """
SELECT
    c.identificador,
    c.admitido,
    c.categoria,
    c.fechaAprobacion,
    c.verificador,
    p.nombre,
    p.apellido,
    p.documento,
    p.email,
    p.fechaNacimiento,
    p.direccion,
    p.estado AS estadoPersona,
    p.fotoFrente,
    p.fotoDorso,
    pa.nombre AS pais
FROM clientes c
INNER JOIN personas p ON p.identificador = c.identificador
LEFT JOIN paises pa ON pa.numero = c.numeroPais
"""


def listar_clientes_pendientes(config):
    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute(
            CONSULTA_CLIENTE
            + """
            WHERE LOWER(LTRIM(RTRIM(c.admitido))) IN ('pendiente', 'no')
              AND LOWER(LTRIM(RTRIM(p.estado))) = 'activo'
            ORDER BY c.identificador
            """
        )
        clientes = filas_como_diccionarios(cursor)

    print("\nCLIENTES PENDIENTES DE REVISIÓN")
    if not clientes:
        print("No hay clientes pendientes.")
        return []
    for cliente in clientes:
        print(
            f"ID {cliente['identificador']}: "
            f"{cliente['nombre']} {cliente['apellido']} | "
            f"DNI {cliente['documento']} | {cliente['email']} | "
            f"estado: {cliente['admitido']}"
        )
    return clientes


def obtener_cliente(config, cliente_id):
    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute(
            CONSULTA_CLIENTE + " WHERE c.identificador = ?", cliente_id
        )
        return fila_como_diccionario(cursor)


def mostrar_cliente(cliente):
    print("\nDETALLE DEL CLIENTE")
    print(f"ID: {cliente['identificador']}")
    print(f"Nombre: {cliente['nombre']} {cliente['apellido']}")
    print(f"Documento: {cliente['documento']}")
    print(f"Email: {cliente['email']}")
    print(f"Fecha de nacimiento: {texto(cliente['fechaNacimiento'])}")
    print(f"Domicilio legal: {texto(cliente['direccion'])}")
    print(f"País de origen: {texto(cliente['pais'])}")
    print(f"Estado de aprobación: {texto(cliente['admitido'])}")
    print(f"Categoría actual: {texto(cliente['categoria'])}")
    print(f"DNI frente: {texto(cliente['fotoFrente'])}")
    print(f"DNI dorso: {texto(cliente['fotoDorso'])}")


def abrir_documentacion(cliente):
    urls = [
        ("frente", cliente.get("fotoFrente")),
        ("dorso", cliente.get("fotoDorso")),
    ]
    disponibles = [(nombre, url) for nombre, url in urls if url]
    if not disponibles:
        print("El cliente no tiene imágenes de documentación cargadas.")
        return
    for nombre, url in disponibles:
        print(f"Abriendo DNI {nombre}: {url}")
        webbrowser.open(str(url))


def pedir_categoria():
    print("\nCategorías disponibles:")
    for indice, categoria in enumerate(CATEGORIAS, start=1):
        print(f"{indice}. {categoria}")
    while True:
        opcion = pedir_entero("Seleccioná una categoría: ")
        if 1 <= opcion <= len(CATEGORIAS):
            return CATEGORIAS[opcion - 1]
        print("La opción no es válida.")


def aprobar_cliente(config, cliente):
    categoria = pedir_categoria()
    print(
        f"\nSe aprobará a {cliente['nombre']} {cliente['apellido']} "
        f"con categoría {categoria}."
    )
    if not confirmar("¿Confirmás la aprobación?"):
        print("Operación cancelada.")
        return

    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        validar_verificador(cursor, config.verificador_id)
        cursor.execute(
            """
            UPDATE clientes
               SET admitido = 'si',
                   categoria = ?,
                   verificador = ?,
                   fechaAprobacion = CAST(GETDATE() AS date)
             WHERE identificador = ?
            """,
            categoria,
            config.verificador_id,
            cliente["identificador"],
        )
        cursor.execute(
            "UPDATE personas SET estado = 'activo' WHERE identificador = ?",
            cliente["identificador"],
        )
        conexion.commit()

    cuerpo = f"""
Hola {cliente['nombre']},

Tu documentación fue validada y tu cuenta en Subastar fue aprobada.
Se te asignó la categoría {categoria.capitalize()}.

Para completar la activación, ingresá a la aplicación, generá tu clave
personal y registrá al menos un medio de pago.

Saludos,
El equipo de Subastar.
"""
    enviar_email_sin_interrumpir(
        config,
        cliente["email"],
        "¡Tu cuenta en Subastar fue aprobada!",
        cuerpo,
    )
    print("Cliente aprobado correctamente.")


def rechazar_cliente(config, cliente):
    motivo = input("Indicá el motivo del rechazo: ").strip()
    if not motivo:
        print("El motivo es obligatorio. Operación cancelada.")
        return
    if not confirmar("¿Confirmás el rechazo del cliente?"):
        print("Operación cancelada.")
        return

    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        validar_verificador(cursor, config.verificador_id)
        cursor.execute(
            """
            UPDATE clientes
               SET admitido = 'no',
                   verificador = ?,
                   fechaAprobacion = NULL
             WHERE identificador = ?
            """,
            config.verificador_id,
            cliente["identificador"],
        )
        cursor.execute(
            "UPDATE personas SET estado = 'inactivo' WHERE identificador = ?",
            cliente["identificador"],
        )
        conexion.commit()

    cuerpo = f"""
Hola {cliente['nombre']},

No pudimos aprobar tu registro en Subastar.

Motivo:
{motivo}

Para solicitar una nueva revisión, comunicate con la casa de subastas.

Saludos,
El equipo de Subastar.
"""
    enviar_email_sin_interrumpir(
        config,
        cliente["email"],
        "Resultado de la revisión de tu cuenta en Subastar",
        cuerpo,
    )
    print("Cliente rechazado correctamente.")


def gestionar_aprobacion_clientes(config):
    while True:
        listar_clientes_pendientes(config)
        print("\n1. Revisar un cliente por ID")
        print("0. Volver")
        opcion = input("Opción: ").strip()
        if opcion == "0":
            return
        if opcion != "1":
            print("Opción inválida.")
            continue

        cliente_id = pedir_entero("ID del cliente: ")
        cliente = obtener_cliente(config, cliente_id)
        if not cliente:
            print("Cliente no encontrado.")
            continue

        mostrar_cliente(cliente)
        print("\n1. Abrir fotos del DNI")
        print("2. Aprobar y asignar categoría")
        print("3. Rechazar")
        print("0. Volver")
        accion = input("Opción: ").strip()
        if accion == "1":
            abrir_documentacion(cliente)
        elif accion == "2":
            aprobar_cliente(config, cliente)
        elif accion == "3":
            rechazar_cliente(config, cliente)
        elif accion != "0":
            print("Opción inválida.")


CONSULTA_MEDIO_PAGO = """
SELECT
    m.identificador,
    m.cliente,
    m.tipo,
    m.activo,
    m.fechaAlta,
    p.nombre,
    p.apellido,
    p.email,
    tc.ultimosDigitos,
    tc.vencimiento,
    tc.titular,
    tc.esExtranjera,
    tc.paisEmisor,
    cb.CBU_IBAN AS cbuIban,
    cb.alias,
    cb.banco AS bancoCuenta,
    cb.paisBanco,
    cb.fondosReservados,
    cb.moneda AS monedaCuenta,
    cc.nroCheque,
    cc.banco AS bancoCheque,
    cc.moneda AS monedaCheque,
    cc.montoGarantia,
    cc.verificado,
    cc.fechaEntrega
FROM mediosDePago m
INNER JOIN personas p ON p.identificador = m.cliente
LEFT JOIN tarjetasCredito tc ON tc.identificador = m.identificador
LEFT JOIN cuentasBancarias cb ON cb.identificador = m.identificador
LEFT JOIN chequesCertificados cc ON cc.identificador = m.identificador
"""


def obtener_medio_pago(config, medio_id):
    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute(
            CONSULTA_MEDIO_PAGO + " WHERE m.identificador = ?", medio_id
        )
        return fila_como_diccionario(cursor)


def descripcion_medio(medio):
    if medio["tipo"] == "tarjeta":
        return (
            f"Tarjeta ****{texto(medio['ultimosDigitos'])}, "
            f"titular {texto(medio['titular'])}, "
            f"vence {texto(medio['vencimiento'])}"
        )
    if medio["tipo"] == "cuenta":
        return (
            f"Cuenta {texto(medio['bancoCuenta'])}, "
            f"{texto(medio['monedaCuenta'])} "
            f"{texto(medio['fondosReservados'], '0')} reservados"
        )
    return (
        f"Cheque {texto(medio['nroCheque'])}, "
        f"{texto(medio['monedaCheque'])} "
        f"{texto(medio['montoGarantia'])}, "
        f"verificado: {texto(medio['verificado'], 'no')}"
    )


def listar_cheques_pendientes(config):
    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute(
            CONSULTA_MEDIO_PAGO
            + """
            WHERE m.tipo = 'cheque'
              AND m.activo = 'si'
              AND ISNULL(cc.verificado, 'no') = 'no'
            ORDER BY m.fechaAlta, m.identificador
            """
        )
        medios = filas_como_diccionarios(cursor)

    print("\nCHEQUES PENDIENTES DE VERIFICACIÓN")
    if not medios:
        print("No hay cheques pendientes.")
        return []
    for medio in medios:
        print(
            f"ID {medio['identificador']}: cheque {medio['nroCheque']} | "
            f"{medio['bancoCheque']} | {medio['monedaCheque']} "
            f"{medio['montoGarantia']} | "
            f"{medio['nombre']} {medio['apellido']}"
        )
    return medios


def listar_medios_cliente(config, cliente_id):
    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute(
            CONSULTA_MEDIO_PAGO
            + " WHERE m.cliente = ? ORDER BY m.identificador",
            cliente_id,
        )
        medios = filas_como_diccionarios(cursor)

    print(f"\nMEDIOS DE PAGO DEL CLIENTE {cliente_id}")
    if not medios:
        print("El cliente no tiene medios de pago.")
        return []
    for medio in medios:
        estado = "activo" if medio["activo"] == "si" else "inactivo"
        print(
            f"ID {medio['identificador']}: "
            f"{descripcion_medio(medio)} | {estado}"
        )
    return medios


def mostrar_medio(medio):
    print("\nDETALLE DEL MEDIO DE PAGO")
    print(f"ID: {medio['identificador']}")
    print(
        f"Cliente: {medio['nombre']} {medio['apellido']} "
        f"(ID {medio['cliente']})"
    )
    print(f"Email: {medio['email']}")
    print(f"Tipo: {medio['tipo']}")
    print(f"Estado: {'activo' if medio['activo'] == 'si' else 'inactivo'}")
    print(f"Fecha de alta: {texto(medio['fechaAlta'])}")

    if medio["tipo"] == "tarjeta":
        print(f"Titular: {texto(medio['titular'])}")
        print(f"Últimos dígitos: {texto(medio['ultimosDigitos'])}")
        print(f"Vencimiento: {texto(medio['vencimiento'])}")
        print(f"Extranjera: {texto(medio['esExtranjera'])}")
        print(f"País emisor: {texto(medio['paisEmisor'])}")
    elif medio["tipo"] == "cuenta":
        print(f"Banco: {texto(medio['bancoCuenta'])}")
        print(f"CBU/IBAN: {texto(medio['cbuIban'])}")
        print(f"Alias: {texto(medio['alias'])}")
        print(f"País del banco: {texto(medio['paisBanco'])}")
        print(f"Moneda: {texto(medio['monedaCuenta'])}")
        print(f"Fondos reservados: {texto(medio['fondosReservados'], '0')}")
    else:
        print(f"Número de cheque: {texto(medio['nroCheque'])}")
        print(f"Banco: {texto(medio['bancoCheque'])}")
        print(f"Moneda: {texto(medio['monedaCheque'])}")
        print(f"Monto de garantía: {texto(medio['montoGarantia'])}")
        print(f"Fecha de entrega: {texto(medio['fechaEntrega'])}")
        print(f"Verificado: {texto(medio['verificado'], 'no')}")


def verificar_cheque(config, medio):
    if medio["tipo"] != "cheque":
        print("La verificación manual persistida corresponde a cheques.")
        return
    if medio["activo"] != "si":
        print("El cheque está inactivo. Reactivalo antes de verificarlo.")
        return
    if medio["verificado"] == "si":
        print("El cheque ya está verificado.")
        return

    fecha_entrega = medio["fechaEntrega"]
    if fecha_entrega and fecha_entrega > date.today():
        print(
            "Advertencia: la fecha de entrega todavía no llegó "
            f"({fecha_entrega})."
        )
        if not confirmar("¿Querés verificarlo de todas maneras?"):
            return
    elif not confirmar("¿Confirmás que el cheque fue recibido y certificado?"):
        return

    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute(
            """
            UPDATE chequesCertificados
               SET verificado = 'si'
             WHERE identificador = ?
            """,
            medio["identificador"],
        )
        conexion.commit()

    cuerpo = f"""
Hola {medio['nombre']},

El cheque certificado número {medio['nroCheque']} fue verificado por
Subastar y ya puede utilizarse como garantía de pago por hasta
{medio['monedaCheque']} {medio['montoGarantia']}.

Saludos,
El equipo de Subastar.
"""
    enviar_email_sin_interrumpir(
        config,
        medio["email"],
        "Cheque certificado verificado",
        cuerpo,
    )
    print("Cheque verificado correctamente.")


def ajustar_fondos_cuenta(config, medio):
    if medio["tipo"] != "cuenta":
        print("El medio seleccionado no es una cuenta bancaria.")
        return

    nuevo_importe = pedir_decimal(
        "Nuevo total de fondos reservados",
        medio["fondosReservados"] or Decimal("0"),
    )
    if not confirmar(
        f"¿Confirmás fondos reservados por "
        f"{medio['monedaCuenta']} {nuevo_importe}?"
    ):
        return

    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute(
            """
            UPDATE cuentasBancarias
               SET fondosReservados = ?
             WHERE identificador = ?
            """,
            nuevo_importe,
            medio["identificador"],
        )
        conexion.commit()

    cuerpo = f"""
Hola {medio['nombre']},

Los fondos reservados de tu cuenta bancaria en Subastar fueron
actualizados a {medio['monedaCuenta']} {nuevo_importe}.

Saludos,
El equipo de Subastar.
"""
    enviar_email_sin_interrumpir(
        config,
        medio["email"],
        "Actualización de fondos reservados",
        cuerpo,
    )
    print("Fondos reservados actualizados correctamente.")


def cambiar_estado_medio(config, medio, activar):
    nuevo_estado = "si" if activar else "no"
    accion = "reactivar" if activar else "desactivar"
    motivo = ""
    if not activar:
        motivo = input("Motivo de la desactivación: ").strip()
        if not motivo:
            print("El motivo es obligatorio.")
            return

    if not confirmar(f"¿Confirmás {accion} este medio de pago?"):
        return

    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute(
            "UPDATE mediosDePago SET activo = ? WHERE identificador = ?",
            nuevo_estado,
            medio["identificador"],
        )
        if medio["tipo"] == "cheque" and not activar:
            cursor.execute(
                """
                UPDATE chequesCertificados
                   SET verificado = 'no'
                 WHERE identificador = ?
                """,
                medio["identificador"],
            )
        conexion.commit()

    if activar:
        cuerpo = f"""
Hola {medio['nombre']},

Tu medio de pago ({descripcion_medio(medio)}) fue reactivado en Subastar.

Saludos,
El equipo de Subastar.
"""
        asunto = "Medio de pago reactivado"
    else:
        cuerpo = f"""
Hola {medio['nombre']},

Tu medio de pago ({descripcion_medio(medio)}) fue desactivado.

Motivo:
{motivo}

Saludos,
El equipo de Subastar.
"""
        asunto = "Medio de pago desactivado"

    enviar_email_sin_interrumpir(
        config, medio["email"], asunto, cuerpo
    )
    estado_final = "reactivado" if activar else "desactivado"
    print(f"Medio de pago {estado_final} correctamente.")


def gestionar_medio_seleccionado(config, medio):
    while True:
        medio = obtener_medio_pago(config, medio["identificador"])
        mostrar_medio(medio)
        print("\n1. Verificar cheque")
        print("2. Ajustar fondos reservados de una cuenta")
        print("3. Desactivar medio")
        print("4. Reactivar medio")
        print("0. Volver")
        opcion = input("Opción: ").strip()
        if opcion == "0":
            return
        if opcion == "1":
            verificar_cheque(config, medio)
        elif opcion == "2":
            ajustar_fondos_cuenta(config, medio)
        elif opcion == "3":
            cambiar_estado_medio(config, medio, activar=False)
        elif opcion == "4":
            cambiar_estado_medio(config, medio, activar=True)
        else:
            print("Opción inválida.")


def gestionar_medios_pago(config):
    while True:
        print("\nVERIFICACIÓN Y GESTIÓN DE MEDIOS DE PAGO")
        print("1. Ver cheques pendientes de verificación")
        print("2. Ver medios de pago de un cliente")
        print("3. Revisar un medio por ID")
        print("0. Volver")
        opcion = input("Opción: ").strip()
        if opcion == "0":
            return
        if opcion == "1":
            listar_cheques_pendientes(config)
        elif opcion == "2":
            listar_medios_cliente(
                config, pedir_entero("ID del cliente: ")
            )
        elif opcion == "3":
            medio = obtener_medio_pago(
                config, pedir_entero("ID del medio de pago: ")
            )
            if not medio:
                print("Medio de pago no encontrado.")
                continue
            gestionar_medio_seleccionado(config, medio)
        else:
            print("Opción inválida.")


def probar_conexiones(config):
    with conexion_db(config) as conexion:
        cursor = conexion.cursor()
        cursor.execute("SELECT DB_NAME()")
        base = cursor.fetchone()[0]
    print(f"Conexión a SQL Server correcta. Base: {base}.")
    print(
        "La configuración SMTP fue cargada. El correo se probará al "
        "realizar una operación para no enviar mensajes innecesarios."
    )


def main():
    configurar_consola()
    try:
        config = Configuracion.desde_entorno()
    except Exception as error:
        print(f"Error de configuración: {error}")
        return 1

    while True:
        print("\n==============================================")
        print(" ADMINISTRACIÓN DE CLIENTES - SUBASTAR")
        print("==============================================")
        print("1. Aprobar o rechazar usuarios")
        print("2. Verificar y gestionar medios de pago")
        print("3. Probar configuración")
        print("0. Salir")
        opcion = input("Opción: ").strip()
        try:
            if opcion == "0":
                print("Hasta luego.")
                return 0
            if opcion == "1":
                gestionar_aprobacion_clientes(config)
            elif opcion == "2":
                gestionar_medios_pago(config)
            elif opcion == "3":
                probar_conexiones(config)
            else:
                print("Opción inválida.")
        except pyodbc.Error as error:
            print(f"Error de base de datos: {error}")
        except Exception as error:
            print(f"Error: {error}")


if __name__ == "__main__":
    raise SystemExit(main())
