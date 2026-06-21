/*
    Completa la recepcion e inspeccion de una consignacion y asocia su seguro.
    seguros.importe representa el valor asegurado, no el premio de la poliza.

    Editar solamente las variables de entrada antes de ejecutar.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @SolicitudId INT = 1;
DECLARE @DepositoId INT = 1;
DECLARE @NroPoliza VARCHAR(30) = 'POL-2026-0001';
DECLARE @Compania VARCHAR(150) = 'La Segunda Seguros';
DECLARE @PolizaCombinada VARCHAR(2) = 'no';
DECLARE @ValorAsegurado DECIMAL(18, 2) = 3000.00;

IF @ValorAsegurado <= 0
    THROW 50001, 'El valor asegurado debe ser mayor a cero.', 1;

IF @PolizaCombinada NOT IN ('si', 'no')
    THROW 50002, 'polizaCombinada debe ser si o no.', 1;

DECLARE @ProductoId INT;
DECLARE @DuenioId INT;

SELECT
    @ProductoId = sc.producto,
    @DuenioId = p.duenio
FROM solicitudesConsignacion sc
JOIN productos p ON p.identificador = sc.producto
WHERE sc.identificador = @SolicitudId;

IF @ProductoId IS NULL
    THROW 50003, 'La solicitud de consignacion no existe.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM depositos
    WHERE identificador = @DepositoId
)
    THROW 50004, 'El deposito indicado no existe.', 1;

IF EXISTS (
    SELECT 1
    FROM productos
    WHERE seguro = @NroPoliza
      AND duenio <> @DuenioId
)
    THROW 50005, 'Una poliza solo puede cubrir bienes del mismo duenio.', 1;

IF EXISTS (
    SELECT 1
    FROM productos
    WHERE seguro = @NroPoliza
      AND identificador <> @ProductoId
)
AND (
    @PolizaCombinada = 'no'
    OR EXISTS (
        SELECT 1
        FROM seguros
        WHERE nroPoliza = @NroPoliza
          AND polizaCombinada = 'no'
    )
)
    THROW 50006, 'La poliza debe ser combinada para cubrir mas de una pieza.', 1;

BEGIN TRANSACTION;

IF EXISTS (SELECT 1 FROM seguros WHERE nroPoliza = @NroPoliza)
BEGIN
    UPDATE seguros
    SET compania = @Compania,
        polizaCombinada = @PolizaCombinada,
        importe = @ValorAsegurado
    WHERE nroPoliza = @NroPoliza;
END
ELSE
BEGIN
    INSERT INTO seguros (nroPoliza, compania, polizaCombinada, importe)
    VALUES (@NroPoliza, @Compania, @PolizaCombinada, @ValorAsegurado);
END;

UPDATE productos
SET depositoActual = @DepositoId,
    seguro = @NroPoliza
WHERE identificador = @ProductoId;

UPDATE solicitudesConsignacion
SET estado = 'aceptado',
    motivoRechazo = NULL
WHERE identificador = @SolicitudId;

COMMIT TRANSACTION;

SELECT
    sc.identificador AS solicitud,
    sc.estado,
    p.identificador AS producto,
    p.duenio,
    d.nombre AS deposito,
    s.nroPoliza,
    s.compania,
    s.polizaCombinada,
    s.importe AS valorAsegurado
FROM solicitudesConsignacion sc
JOIN productos p ON p.identificador = sc.producto
JOIN depositos d ON d.identificador = p.depositoActual
JOIN seguros s ON s.nroPoliza = p.seguro
WHERE sc.identificador = @SolicitudId;
