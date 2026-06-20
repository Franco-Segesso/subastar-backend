#!/usr/bin/env python3
"""
Asistente administrativo para avanzar consignaciones sin una interfaz de admin.

Uso:
    python scripts/consignaciones/gestionar_consignaciones.py
    python scripts/consignaciones/gestionar_consignaciones.py --listar
    python scripts/consignaciones/gestionar_consignaciones.py --id 12

La conexion se toma de src/main/resources/application.properties. Tambien se
puede sobrescribir con SUBASTAR_DB_SERVER, SUBASTAR_DB_NAME,
SUBASTAR_DB_USER y SUBASTAR_DB_PASSWORD.
"""

from __future__ import annotations

import argparse
import getpass
import os
import re
import sys
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any

try:
    import pyodbc
except ImportError:
    print("Falta pyodbc. Instalalo con: python -m pip install pyodbc")
    raise SystemExit(1)


SCRIPT_DIR = Path(__file__).resolve().parent
BACKEND_DIR = SCRIPT_DIR.parent.parent
PROPERTIES_PATH = BACKEND_DIR / "src" / "main" / "resources" / "application.properties"


@dataclass
class Consignacion:
    solicitud_id: int
    estado: str
    condiciones_aceptadas: str
    motivo_documentacion: str | None
    producto_id: int
    descripcion: str
    duenio_id: int
    deposito_id: int | None
    deposito_nombre: str | None
    nro_poliza: str | None
    compania: str | None
    valor_asegurado: Decimal | None
    item_id: int | None
    catalogo_id: int | None
    precio_base: Decimal | None
    comision: Decimal | None
    subasta_id: int | None
    subasta_fecha: Any
    subasta_hora: Any
    subasta_estado: str | None
    cantidad_documentos: int


def propiedades() -> dict[str, str]:
    valores: dict[str, str] = {}
    if PROPERTIES_PATH.exists():
        for linea in PROPERTIES_PATH.read_text(encoding="utf-8").splitlines():
            linea = linea.strip()
            if not linea or linea.startswith("#") or "=" not in linea:
                continue
            clave, valor = linea.split("=", 1)
            valores[clave.strip()] = valor.strip()
    return valores


def configuracion_db() -> tuple[str, str, str, str, str, str]:
    props = propiedades()
    jdbc = props.get("spring.datasource.url", "")
    servidor_match = re.search(r"jdbc:sqlserver://([^:;]+)(?::(\d+))?", jdbc)
    base_match = re.search(r"(?:database|databaseName)=([^;]+)", jdbc, re.IGNORECASE)

    servidor = os.getenv(
        "SUBASTAR_DB_SERVER",
        servidor_match.group(1) if servidor_match else "",
    )
    puerto = servidor_match.group(2) if servidor_match and servidor_match.group(2) else "1433"
    base = os.getenv(
        "SUBASTAR_DB_NAME",
        base_match.group(1) if base_match else "",
    )
    usuario = os.getenv(
        "SUBASTAR_DB_USER",
        props.get("spring.datasource.username", ""),
    )
    clave = os.getenv(
        "SUBASTAR_DB_PASSWORD",
        props.get("spring.datasource.password", ""),
    )

    if not all((servidor, base, usuario)):
        raise RuntimeError(
            "No se pudo leer la configuracion de la base. Revisa application.properties "
            "o las variables SUBASTAR_DB_*."
        )
    if not clave:
        clave = getpass.getpass("Clave de SQL Server: ")

    drivers = pyodbc.drivers()
    preferidos = (
        "ODBC Driver 18 for SQL Server",
        "ODBC Driver 17 for SQL Server",
        "SQL Server",
    )
    driver = next((nombre for nombre in preferidos if nombre in drivers), None)
    if driver is None:
        raise RuntimeError(
            "No hay un driver ODBC de SQL Server instalado. "
            "Instala Microsoft ODBC Driver 18 for SQL Server."
        )
    return driver, servidor, puerto, base, usuario, clave


def conectar():
    driver, servidor, puerto, base, usuario, clave = configuracion_db()
    cadena = (
        f"DRIVER={{{driver}}};"
        f"SERVER={servidor},{puerto};"
        f"DATABASE={base};"
        f"UID={usuario};PWD={clave};"
        "Encrypt=yes;TrustServerCertificate=no;Connection Timeout=30;"
    )
    return pyodbc.connect(cadena, autocommit=False)


CONSULTA_CONSIGNACION = """
SELECT
    sc.identificador,
    LTRIM(RTRIM(COALESCE(sc.estado, 'pendiente'))),
    LTRIM(RTRIM(COALESCE(sc.condicionesAceptadas, 'no'))),
    sc.motivoDocumentacion,
    p.identificador,
    p.descripcionCompleta,
    p.duenio,
    p.depositoActual,
    d.nombre,
    p.seguro,
    sg.compania,
    sg.importe,
    item.identificador,
    item.catalogo,
    item.precioBase,
    item.comision,
    su.identificador,
    su.fecha,
    su.hora,
    su.estado,
    (
        SELECT COUNT(*)
        FROM documentosConsignacion dc
        WHERE dc.solicitud = sc.identificador
    )
FROM solicitudesConsignacion sc
JOIN productos p ON p.identificador = sc.producto
LEFT JOIN depositos d ON d.identificador = p.depositoActual
LEFT JOIN seguros sg ON sg.nroPoliza = p.seguro
OUTER APPLY (
    SELECT TOP 1 ic.*
    FROM itemsCatalogo ic
    WHERE ic.producto = p.identificador
    ORDER BY ic.identificador DESC
) item
LEFT JOIN catalogos ca ON ca.identificador = item.catalogo
LEFT JOIN subastas su ON su.identificador = ca.subasta
WHERE sc.identificador = ?
"""


def cargar_consignacion(cursor, solicitud_id: int) -> Consignacion | None:
    fila = cursor.execute(CONSULTA_CONSIGNACION, solicitud_id).fetchone()
    return Consignacion(*fila) if fila else None


def etapa(c: Consignacion) -> str:
    estado = c.estado.lower()
    condiciones = c.condiciones_aceptadas.lower()
    if estado == "rechazado":
        return "RECHAZADA"
    if c.deposito_id is None:
        return "RECEPCION"
    if estado == "documentacion_pendiente":
        return "DOCUMENTACION_PENDIENTE"
    if estado == "documentacion_presentada":
        return "REVISION_DOCUMENTACION"
    if estado == "pendiente":
        return "INSPECCION"
    if estado == "aceptado" and (
        not c.nro_poliza
        or not c.compania
        or c.valor_asegurado is None
        or c.item_id is None
    ):
        return "CONDICIONES"
    if estado == "aceptado" and condiciones != "si":
        return "ESPERANDO_USUARIO"
    if estado == "aceptado" and condiciones == "si" and c.item_id is not None:
        return "ASIGNADA"
    return "INCONSISTENTE"


def descripcion_etapa(c: Consignacion) -> str:
    nombres = {
        "RECEPCION": "Solicitud enviada - falta registrar la recepcion en deposito",
        "INSPECCION": "Recibido en deposito - falta inspeccionar",
        "DOCUMENTACION_PENDIENTE": "Esperando documentacion del duenio",
        "REVISION_DOCUMENTACION": "Documentacion presentada - falta revisarla",
        "CONDICIONES": "Inspeccionado y aceptado - falta preparar condiciones",
        "ESPERANDO_USUARIO": "Esperando que el duenio acepte o rechace desde Android",
        "ASIGNADA": "Condiciones aceptadas - bien asignado a una subasta",
        "RECHAZADA": "Consignacion rechazada - flujo finalizado",
        "INCONSISTENTE": "Datos incompletos o inconsistentes",
    }
    return nombres[etapa(c)]


def listar_consignaciones(cursor) -> None:
    filas = cursor.execute(
        """
        SELECT TOP 100
            sc.identificador,
            p.descripcionCompleta,
            sc.estado,
            sc.condicionesAceptadas
        FROM solicitudesConsignacion sc
        JOIN productos p ON p.identificador = sc.producto
        ORDER BY sc.identificador DESC
        """
    ).fetchall()
    if not filas:
        print("No hay consignaciones.")
        return
    print("\nCONSIGNACIONES")
    print("-" * 92)
    for solicitud_id, descripcion, estado, condiciones in filas:
        c = cargar_consignacion(cursor, solicitud_id)
        texto = descripcion_etapa(c) if c else "No disponible"
        print(
            f"ID {solicitud_id:<4} | {str(descripcion)[:30]:<30} | "
            f"{str(estado):<10} | {texto}"
        )
    print("-" * 92)


def mostrar_resumen(c: Consignacion) -> None:
    print("\n" + "=" * 72)
    print(f"Consignacion: {c.solicitud_id}")
    print(f"Producto:     {c.producto_id} - {c.descripcion}")
    print(f"Estado:       {c.estado}")
    print(f"Instancia:    {descripcion_etapa(c)}")
    print(f"Deposito:     {c.deposito_nombre or 'Sin asignar'}")
    print(f"Poliza:       {c.nro_poliza or 'Sin asignar'}")
    if c.motivo_documentacion:
        print(f"Documentacion:{c.motivo_documentacion}")
    if c.cantidad_documentos:
        print(f"Archivos:     {c.cantidad_documentos}")
    if c.item_id:
        print(
            f"Propuesta:    item {c.item_id}, base {c.precio_base}, "
            f"comision {c.comision}%"
        )
    print("=" * 72)


def confirmar(mensaje: str) -> bool:
    return input(f"{mensaje} [s/N]: ").strip().lower() in ("s", "si")


def elegir_fila(cursor, consulta: str, titulo: str) -> Any:
    filas = cursor.execute(consulta).fetchall()
    if not filas:
        raise RuntimeError(f"No hay {titulo.lower()} disponibles.")
    print(f"\n{titulo}")
    for fila in filas:
        print(" | ".join(str(valor) for valor in fila))
    ids = {int(fila[0]) for fila in filas}
    while True:
        try:
            elegido = int(input("Ingresa el ID: ").strip())
            if elegido in ids:
                return elegido
        except ValueError:
            pass
        print("ID invalido.")


def pedir_texto(etiqueta: str) -> str:
    while True:
        valor = input(f"{etiqueta}: ").strip()
        if valor:
            return valor
        print("Este dato es obligatorio.")


def pedir_entero(etiqueta: str, minimo: int = 1) -> int:
    while True:
        try:
            valor = int(input(f"{etiqueta}: ").strip())
            if valor >= minimo:
                return valor
        except ValueError:
            pass
        print(f"Ingresa un numero entero mayor o igual a {minimo}.")


def pedir_opcion(etiqueta: str, opciones: tuple[str, ...]) -> str:
    opciones_normalizadas = {opcion.lower(): opcion for opcion in opciones}
    while True:
        valor = input(f"{etiqueta} ({'/'.join(opciones)}): ").strip().lower()
        if valor in opciones_normalizadas:
            return opciones_normalizadas[valor]
        print("Opcion invalida.")


def pedir_fecha_subasta() -> date:
    fecha_minima = date.today() + timedelta(days=11)
    while True:
        texto = input(
            f"Fecha de la subasta YYYY-MM-DD (desde {fecha_minima.isoformat()}): "
        ).strip()
        try:
            valor = datetime.strptime(texto, "%Y-%m-%d").date()
            if valor >= fecha_minima:
                return valor
        except ValueError:
            pass
        print(
            "La estructura SQL exige una fecha posterior a hoy + 10 dias. "
            "Usa el formato YYYY-MM-DD."
        )


def pedir_hora_subasta() -> time:
    while True:
        texto = input("Hora de la subasta HH:MM: ").strip()
        try:
            return datetime.strptime(texto, "%H:%M").time()
        except ValueError:
            print("Hora invalida. Ejemplo: 18:30")


def crear_subasta_y_catalogo(conexion) -> int:
    cursor = conexion.cursor()
    print("\nNo hay subastas pendientes disponibles.")
    print("Vamos a crear la subasta futura y su catalogo.")

    descripcion = pedir_texto("Nombre del catalogo/subasta")
    fecha = pedir_fecha_subasta()
    hora = pedir_hora_subasta()
    categoria = pedir_opcion(
        "Categoria", ("comun", "especial", "plata", "oro", "platino")
    )
    moneda = pedir_opcion("Moneda", ("ARS", "USD"))
    ubicacion = pedir_texto("Ubicacion")
    capacidad = pedir_entero("Capacidad de asistentes")
    tiene_deposito = pedir_opcion("Tiene deposito", ("si", "no"))
    seguridad_propia = pedir_opcion("Seguridad propia", ("si", "no"))

    subastador_id = elegir_fila(
        cursor,
        """
        SELECT
            s.identificador,
            CONCAT(p.nombre, ' ', p.apellido)
        FROM subastadores s
        JOIN personas p ON p.identificador = s.identificador
        ORDER BY p.apellido, p.nombre
        """,
        "REMATADORES",
    )
    responsable_id = elegir_fila(
        cursor,
        """
        SELECT
            e.identificador,
            CONCAT(p.nombre, ' ', p.apellido)
        FROM empleados e
        JOIN personas p ON p.identificador = e.identificador
        ORDER BY p.apellido, p.nombre
        """,
        "RESPONSABLES DEL CATALOGO",
    )

    print("\nSUBASTA A CREAR")
    print(f"Nombre:       {descripcion}")
    print(f"Fecha/hora:   {fecha} {hora.strftime('%H:%M')}")
    print(f"Categoria:    {categoria}")
    print(f"Moneda:       {moneda}")
    print(f"Ubicacion:    {ubicacion}")
    print(f"Rematador:    {subastador_id}")
    print(f"Responsable:  {responsable_id}")
    if not confirmar("Crear esta subasta pendiente y su catalogo?"):
        raise RuntimeError("Creacion de subasta cancelada.")

    try:
        cursor.execute(
            """
            INSERT INTO subastas (
                fecha, hora, estado, subastador, ubicacion,
                capacidadAsistentes, tieneDeposito, seguridadPropia,
                categoria, moneda
            )
            OUTPUT INSERTED.identificador
            VALUES (?, ?, 'pendiente', ?, ?, ?, ?, ?, ?, ?)
            """,
            fecha,
            hora,
            subastador_id,
            ubicacion,
            capacidad,
            tiene_deposito,
            seguridad_propia,
            categoria,
            moneda,
        )
        subasta_id = int(cursor.fetchone()[0])
        cursor.execute(
            """
            INSERT INTO catalogos (descripcion, subasta, responsable)
            OUTPUT INSERTED.identificador
            VALUES (?, ?, ?)
            """,
            descripcion,
            subasta_id,
            responsable_id,
        )
        catalogo_id = int(cursor.fetchone()[0])
        conexion.commit()
    except Exception:
        conexion.rollback()
        raise

    print(
        f"Subasta {subasta_id} y catalogo {catalogo_id} creados correctamente."
    )
    return catalogo_id


def elegir_o_crear_catalogo(conexion) -> int:
    cursor = conexion.cursor()
    filas = cursor.execute(
        """
        SELECT
            ca.identificador,
            ca.descripcion,
            su.identificador,
            su.fecha,
            su.hora,
            su.moneda,
            su.categoria
        FROM catalogos ca
        JOIN subastas su ON su.identificador = ca.subasta
        WHERE LOWER(LTRIM(RTRIM(su.estado))) = 'pendiente'
        ORDER BY su.fecha, su.hora
        """
    ).fetchall()
    if not filas:
        return crear_subasta_y_catalogo(conexion)

    print("\nSUBASTAS PENDIENTES")
    for fila in filas:
        print(" | ".join(str(valor) for valor in fila))
    ids = {int(fila[0]) for fila in filas}
    print("Ingresa el ID de catalogo de la primera columna.")
    print("Tambien podes escribir C para crear una nueva subasta.")
    while True:
        valor = input("Catalogo o C: ").strip()
        if valor.lower() == "c":
            return crear_subasta_y_catalogo(conexion)
        try:
            catalogo_id = int(valor)
            if catalogo_id in ids:
                return catalogo_id
        except ValueError:
            pass
        print("Opcion invalida.")


def pedir_decimal(etiqueta: str, minimo: Decimal = Decimal("0.01")) -> Decimal:
    while True:
        texto = input(f"{etiqueta}: ").strip().replace(" ", "")
        if "," in texto and "." in texto:
            texto = texto.replace(".", "").replace(",", ".")
        elif "," in texto:
            texto = texto.replace(",", ".")
        try:
            valor = Decimal(texto)
            if valor >= minimo:
                return valor
        except InvalidOperation:
            pass
        print(f"Ingresa un numero mayor o igual a {minimo}. Ejemplo: 3000")


def registrar_recepcion(conexion, c: Consignacion) -> None:
    cursor = conexion.cursor()
    deposito_id = elegir_fila(
        cursor,
        """
        SELECT identificador, nombre, direccion, COALESCE(sector, '')
        FROM depositos
        ORDER BY nombre
        """,
        "DEPOSITOS",
    )
    if not confirmar(f"Registrar el producto {c.producto_id} en el deposito {deposito_id}?"):
        return
    cursor.execute(
        "UPDATE productos SET depositoActual = ? WHERE identificador = ?",
        deposito_id,
        c.producto_id,
    )
    conexion.commit()
    print("Recepcion registrada. La consignacion avanzo a inspeccion.")


def registrar_inspeccion(conexion, c: Consignacion) -> None:
    while True:
        decision = input(
            "Resultado: [A]ceptar / [R]echazar / pedir [D]ocumentacion: "
        ).strip().lower()
        if decision in ("a", "aceptar", "r", "rechazar", "d", "documentacion"):
            break
        print("Opcion invalida.")

    cursor = conexion.cursor()
    if decision.startswith("d"):
        motivo = input(
            "Que documentacion debe presentar el duenio y por que?: "
        ).strip()
        if not motivo:
            print("La explicacion es obligatoria.")
            return
        if not confirmar("Solicitar esta documentacion al duenio?"):
            return
        cursor.execute(
            """
            UPDATE solicitudesConsignacion
            SET estado = 'documentacion_pendiente',
                motivoDocumentacion = ?,
                motivoRechazo = NULL,
                condicionesAceptadas = 'no'
            WHERE identificador = ?
            """,
            motivo,
            c.solicitud_id,
        )
        conexion.commit()
        print(
            "Documentacion solicitada. El duenio podra adjuntarla desde "
            "el detalle de la consignacion en Android."
        )
        return

    if decision.startswith("r"):
        motivo = input("Motivo del rechazo: ").strip()
        if not motivo:
            print("El motivo es obligatorio.")
            return
        if not confirmar("Confirmar rechazo de la consignacion?"):
            return
        cursor.execute(
            """
            UPDATE solicitudesConsignacion
            SET estado = 'rechazado',
                motivoRechazo = ?,
                condicionesAceptadas = 'no'
            WHERE identificador = ?
            """,
            motivo,
            c.solicitud_id,
        )
        conexion.commit()
        print("Consignacion rechazada. El usuario vera el motivo en la app.")
        return

    if not confirmar("Confirmar que el bien fue inspeccionado y aceptado?"):
        return
    cursor.execute(
        """
        UPDATE solicitudesConsignacion
        SET estado = 'aceptado',
            motivoRechazo = NULL,
            motivoDocumentacion = NULL,
            condicionesAceptadas = 'no'
        WHERE identificador = ?
        """,
        c.solicitud_id,
    )
    conexion.commit()
    print("Inspeccion aprobada. Ahora deben prepararse las condiciones.")


def revisar_documentacion(conexion, c: Consignacion) -> None:
    cursor = conexion.cursor()
    documentos = cursor.execute(
        """
        SELECT identificador, nombreArchivo, urlArchivo, descripcion, fechaCarga, estado
        FROM documentosConsignacion
        WHERE solicitud = ?
        ORDER BY fechaCarga
        """,
        c.solicitud_id,
    ).fetchall()
    if not documentos:
        print("No hay documentos cargados para revisar.")
        return

    print("\nDOCUMENTACION PRESENTADA")
    for documento in documentos:
        print("-" * 72)
        print(f"ID:          {documento.identificador}")
        print(f"Archivo:     {documento.nombreArchivo}")
        print(f"Descripcion: {documento.descripcion or '--'}")
        print(f"Fecha:       {documento.fechaCarga}")
        print(f"URL:         {documento.urlArchivo}")
    print("-" * 72)

    while True:
        decision = input(
            "Revision: [A]probar y volver a inspeccion / [R]echazar bien: "
        ).strip().lower()
        if decision in ("a", "aprobar", "r", "rechazar"):
            break
        print("Opcion invalida.")

    if decision.startswith("a"):
        if not confirmar("Aprobar la documentacion presentada?"):
            return
        try:
            cursor.execute(
                """
                UPDATE documentosConsignacion
                SET estado = 'aprobado'
                WHERE solicitud = ?
                """,
                c.solicitud_id,
            )
            cursor.execute(
                """
                UPDATE solicitudesConsignacion
                SET estado = 'pendiente',
                    motivoDocumentacion = NULL
                WHERE identificador = ?
                """,
                c.solicitud_id,
            )
            conexion.commit()
        except Exception:
            conexion.rollback()
            raise
        print(
            "Documentacion aprobada. Ejecuta nuevamente el asistente para "
            "resolver la inspeccion del bien."
        )
        return

    motivo = input("Motivo del rechazo del bien: ").strip()
    if not motivo:
        print("El motivo es obligatorio.")
        return
    if not confirmar("Rechazar definitivamente la consignacion?"):
        return
    try:
        cursor.execute(
            """
            UPDATE documentosConsignacion
            SET estado = 'rechazado'
            WHERE solicitud = ?
            """,
            c.solicitud_id,
        )
        cursor.execute(
            """
            UPDATE solicitudesConsignacion
            SET estado = 'rechazado',
                motivoRechazo = ?,
                motivoDocumentacion = NULL,
                condicionesAceptadas = 'no'
            WHERE identificador = ?
            """,
            motivo,
            c.solicitud_id,
        )
        conexion.commit()
    except Exception:
        conexion.rollback()
        raise
    print("Consignacion rechazada luego de revisar la documentacion.")


def validar_poliza_existente(cursor, c: Consignacion, nro_poliza: str) -> Any:
    poliza = cursor.execute(
        """
        SELECT nroPoliza, compania, polizaCombinada, importe
        FROM seguros
        WHERE nroPoliza = ?
        """,
        nro_poliza,
    ).fetchone()
    if not poliza:
        return None
    conflicto = cursor.execute(
        """
        SELECT TOP 1 identificador
        FROM productos
        WHERE seguro = ? AND duenio <> ?
        """,
        nro_poliza,
        c.duenio_id,
    ).fetchone()
    if conflicto:
        raise RuntimeError("Esa poliza ya cubre un bien de otro duenio.")
    otro_bien = cursor.execute(
        """
        SELECT TOP 1 identificador
        FROM productos
        WHERE seguro = ? AND identificador <> ?
        """,
        nro_poliza,
        c.producto_id,
    ).fetchone()
    if otro_bien and str(poliza.polizaCombinada).strip().lower() != "si":
        raise RuntimeError("La poliza existe pero no permite cobertura combinada.")
    return poliza


def preparar_condiciones(conexion, c: Consignacion) -> None:
    cursor = conexion.cursor()
    catalogo_id = c.catalogo_id
    precio_base = c.precio_base
    comision = c.comision

    if c.item_id is None:
        catalogo_id = elegir_o_crear_catalogo(conexion)
        precio_base = pedir_decimal("Precio base")
        comision = pedir_decimal("Comision de la empresa (%)")

    nro_poliza = (c.nro_poliza or input("Numero de poliza: ").strip()).strip()
    if not nro_poliza:
        print("El numero de poliza es obligatorio.")
        return

    poliza = validar_poliza_existente(cursor, c, nro_poliza)
    compania = c.compania
    valor_asegurado = c.valor_asegurado
    combinada = "no"
    actualizar_valor_poliza = False

    if poliza:
        compania = poliza.compania
        valor_asegurado = poliza.importe
        combinada = poliza.polizaCombinada
        print(
            f"Se usara la poliza existente: {nro_poliza} | {compania} | "
            f"valor asegurado {valor_asegurado}"
        )
        ya_asociada = c.nro_poliza == nro_poliza
        if not ya_asociada:
            valor_actual = Decimal(str(poliza.importe))
            sugerido = valor_actual + Decimal(str(precio_base or 0))
            print(
                "Al agregar otra pieza, el importe de la poliza debe representar "
                "el valor asegurado total."
            )
            valor_asegurado = pedir_decimal(
                f"Nuevo valor asegurado total (sugerido {sugerido})",
                valor_actual + Decimal("0.01"),
            )
            actualizar_valor_poliza = True
    else:
        compania = input("Compania aseguradora: ").strip()
        if not compania:
            print("La compania es obligatoria.")
            return
        valor_asegurado = pedir_decimal(
            "Valor asegurado (normalmente igual al precio base)"
        )
        combinada = (
            "si"
            if input("Puede cubrir varias piezas del mismo duenio? [s/N]: ")
            .strip()
            .lower()
            in ("s", "si")
            else "no"
        )

    print("\nCONDICIONES A REGISTRAR")
    print(f"Catalogo:        {catalogo_id}")
    print(f"Precio base:     {precio_base}")
    print(f"Comision:        {comision}%")
    print(f"Poliza:          {nro_poliza}")
    print(f"Compania:        {compania}")
    print(f"Valor asegurado: {valor_asegurado}")
    if not confirmar("Guardar estas condiciones?"):
        return

    try:
        if not poliza:
            cursor.execute(
                """
                INSERT INTO seguros (nroPoliza, compania, polizaCombinada, importe)
                VALUES (?, ?, ?, ?)
                """,
                nro_poliza,
                compania,
                combinada,
                valor_asegurado,
            )
        elif actualizar_valor_poliza:
            cursor.execute(
                "UPDATE seguros SET importe = ? WHERE nroPoliza = ?",
                valor_asegurado,
                nro_poliza,
            )

        cursor.execute(
            "UPDATE productos SET seguro = ? WHERE identificador = ?",
            nro_poliza,
            c.producto_id,
        )

        if c.item_id is None:
            cursor.execute(
                """
                INSERT INTO itemsCatalogo
                    (catalogo, producto, precioBase, comision, subastado, precioFinal)
                VALUES (?, ?, ?, ?, 'no', NULL)
                """,
                catalogo_id,
                c.producto_id,
                precio_base,
                comision,
            )
        conexion.commit()
    except Exception:
        conexion.rollback()
        raise

    print(
        "Condiciones registradas. Ahora el duenio debe entrar a Android y "
        "aceptarlas o rechazarlas."
    )


def avanzar(conexion, solicitud_id: int) -> None:
    cursor = conexion.cursor()
    c = cargar_consignacion(cursor, solicitud_id)
    if not c:
        print(f"No existe la consignacion {solicitud_id}.")
        return
    mostrar_resumen(c)
    estado_actual = etapa(c)

    if estado_actual == "RECEPCION":
        registrar_recepcion(conexion, c)
    elif estado_actual == "INSPECCION":
        registrar_inspeccion(conexion, c)
    elif estado_actual == "DOCUMENTACION_PENDIENTE":
        print(
            "\nEl duenio todavia debe adjuntar la documentacion desde Android.\n"
            f"Solicitud: {c.motivo_documentacion or '--'}"
        )
    elif estado_actual == "REVISION_DOCUMENTACION":
        revisar_documentacion(conexion, c)
    elif estado_actual == "CONDICIONES":
        preparar_condiciones(conexion, c)
    elif estado_actual == "ESPERANDO_USUARIO":
        print(
            "\nNo hay que ejecutar un cambio administrativo ahora.\n"
            "El duenio debe abrir el detalle en Android y aceptar o rechazar "
            "las condiciones propuestas."
        )
    elif estado_actual == "ASIGNADA":
        print(
            "\nLa consignacion ya completo el flujo administrativo y esta "
            "asignada a una subasta."
        )
    elif estado_actual == "RECHAZADA":
        print("\nLa consignacion fue rechazada y no puede seguir avanzando.")
    else:
        print(
            "\nLos datos no forman una instancia valida. Revisa deposito, estado, "
            "seguro, item de catalogo y condicionesAceptadas."
        )


def argumentos() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Asistente para avanzar consignaciones por instancia."
    )
    parser.add_argument("--listar", action="store_true", help="Lista las consignaciones")
    parser.add_argument("--id", type=int, help="ID de consignacion a gestionar")
    return parser.parse_args()


def main() -> int:
    args = argumentos()
    try:
        with conectar() as conexion:
            if args.listar:
                listar_consignaciones(conexion.cursor())
                return 0

            solicitud_id = args.id
            if solicitud_id is None:
                listar_consignaciones(conexion.cursor())
                try:
                    solicitud_id = int(
                        input("\nID de la consignacion que queres gestionar: ").strip()
                    )
                except ValueError:
                    print("ID invalido.")
                    return 1
            avanzar(conexion, solicitud_id)
            return 0
    except KeyboardInterrupt:
        print("\nOperacion cancelada.")
        return 1
    except Exception as error:
        print(f"\nERROR: {error}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
