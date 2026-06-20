/*
    Completa polizas faltantes de productos historicos que ya fueron asignados
    a un catalogo. El valor asegurado se toma del precio base del bien.

    No modifica tablas ni reemplaza polizas validas existentes.
    Revisar @Compania y @PrefijoPoliza antes de ejecutar.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @Compania VARCHAR(150) = 'La Segunda Seguros';
DECLARE @PrefijoPoliza VARCHAR(20) = 'POL-HIST-';

DECLARE @Pendientes TABLE (
    productoId INT PRIMARY KEY,
    duenioId INT NOT NULL,
    nroPoliza VARCHAR(30) NOT NULL,
    valorAsegurado DECIMAL(18, 2) NOT NULL
);

INSERT INTO @Pendientes (productoId, duenioId, nroPoliza, valorAsegurado)
SELECT
    p.identificador,
    p.duenio,
    COALESCE(
        NULLIF(LTRIM(RTRIM(p.seguro)), ''),
        CONCAT(@PrefijoPoliza, p.identificador)
    ),
    MAX(ic.precioBase)
FROM productos p
JOIN itemsCatalogo ic ON ic.producto = p.identificador
LEFT JOIN seguros s ON s.nroPoliza = p.seguro
WHERE p.seguro IS NULL
   OR LTRIM(RTRIM(p.seguro)) = ''
   OR s.nroPoliza IS NULL
GROUP BY p.identificador, p.duenio, p.seguro;

IF EXISTS (
    SELECT nroPoliza
    FROM @Pendientes
    GROUP BY nroPoliza
    HAVING COUNT(DISTINCT duenioId) > 1
)
    THROW 50201, 'Hay una poliza historica asociada a bienes de distintos duenios.', 1;

IF EXISTS (
    SELECT 1
    FROM @Pendientes pendientes
    JOIN productos existente ON existente.seguro = pendientes.nroPoliza
    WHERE existente.duenio <> pendientes.duenioId
)
    THROW 50202, 'El numero de poliza generado ya pertenece a otro duenio.', 1;

BEGIN TRANSACTION;

INSERT INTO seguros (nroPoliza, compania, polizaCombinada, importe)
SELECT
    pendientes.nroPoliza,
    @Compania,
    CASE WHEN COUNT(*) > 1 THEN 'si' ELSE 'no' END,
    SUM(pendientes.valorAsegurado)
FROM @Pendientes pendientes
LEFT JOIN seguros existente ON existente.nroPoliza = pendientes.nroPoliza
WHERE existente.nroPoliza IS NULL
GROUP BY pendientes.nroPoliza;

UPDATE producto
SET producto.seguro = pendientes.nroPoliza
FROM productos producto
JOIN @Pendientes pendientes ON pendientes.productoId = producto.identificador
WHERE producto.seguro IS NULL
   OR LTRIM(RTRIM(producto.seguro)) = '';

UPDATE registro
SET registro.nroPolizaSeguro = producto.seguro
FROM registroDeSubasta registro
JOIN productos producto ON producto.identificador = registro.producto
WHERE registro.nroPolizaSeguro IS NULL
  AND producto.seguro IS NOT NULL;

COMMIT TRANSACTION;

SELECT
    pendientes.productoId,
    pendientes.duenioId,
    seguro.nroPoliza,
    seguro.compania,
    seguro.polizaCombinada,
    seguro.importe AS valorAsegurado
FROM @Pendientes pendientes
JOIN seguros seguro ON seguro.nroPoliza = pendientes.nroPoliza
ORDER BY pendientes.productoId;

SELECT
    p.identificador AS productoSinSeguro,
    p.duenio
FROM productos p
LEFT JOIN itemsCatalogo ic ON ic.producto = p.identificador
WHERE (p.seguro IS NULL OR LTRIM(RTRIM(p.seguro)) = '')
  AND ic.identificador IS NULL;
