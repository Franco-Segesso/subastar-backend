/*
    Carga la propuesta de precio, comision y subasta para que el duenio pueda
    aceptarla desde Android. El producto queda disponible recien al aceptar.
    Editar solamente las variables de entrada antes de ejecutar.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @SolicitudId INT = 1;
DECLARE @CatalogoId INT = 1;
DECLARE @PrecioBase DECIMAL(18, 2) = 3000.00;
DECLARE @Comision DECIMAL(18, 2) = 10.00;

DECLARE @ProductoId INT;

SELECT @ProductoId = producto
FROM solicitudesConsignacion
WHERE identificador = @SolicitudId
  AND estado = 'aceptado';

IF @ProductoId IS NULL
    THROW 50101, 'La consignacion debe haber sido inspeccionada y aceptada.', 1;

IF NOT EXISTS (SELECT 1 FROM catalogos WHERE identificador = @CatalogoId)
    THROW 50102, 'El catalogo indicado no existe.', 1;

IF NOT EXISTS (
    SELECT 1
    FROM productos
    WHERE identificador = @ProductoId
      AND seguro IS NOT NULL
      AND depositoActual IS NOT NULL
)
    THROW 50103, 'El bien debe tener deposito y seguro antes de asignarse.', 1;

IF EXISTS (SELECT 1 FROM itemsCatalogo WHERE producto = @ProductoId)
    THROW 50104, 'El producto ya fue asignado a un catalogo.', 1;

BEGIN TRANSACTION;

INSERT INTO itemsCatalogo (
    catalogo,
    producto,
    precioBase,
    comision,
    subastado,
    precioFinal
)
VALUES (
    @CatalogoId,
    @ProductoId,
    @PrecioBase,
    @Comision,
    'no',
    NULL
);

COMMIT TRANSACTION;

SELECT
    ic.identificador AS item,
    ic.catalogo,
    ic.producto,
    ic.precioBase,
    ic.comision,
    ic.subastado
FROM itemsCatalogo ic
WHERE ic.producto = @ProductoId;
