SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @Correcciones TABLE (
    compraId INT PRIMARY KEY,
    medioPagoId INT NULL,
    tipo VARCHAR(20) NULL,
    diferencia DECIMAL(18, 2) NOT NULL
);

INSERT INTO @Correcciones (compraId, medioPagoId, tipo, diferencia)
SELECT
    r.identificador,
    r.medioPago,
    m.tipo,
    CAST(r.comision - ic.comision AS DECIMAL(18, 2))
FROM dbo.registroDeSubasta r
INNER JOIN dbo.catalogos c
    ON c.subasta = r.subasta
INNER JOIN dbo.itemsCatalogo ic
    ON ic.catalogo = c.identificador
   AND ic.producto = r.producto
LEFT JOIN dbo.mediosDePago m
    ON m.identificador = r.medioPago
WHERE r.comision <> ic.comision;

UPDATE cb
SET cb.fondosReservados = cb.fondosReservados + x.diferencia
FROM dbo.cuentasBancarias cb
INNER JOIN @Correcciones x
    ON x.medioPagoId = cb.identificador
WHERE x.tipo = 'cuenta'
  AND x.diferencia > 0
  AND EXISTS (
      SELECT 1
      FROM dbo.registroDeSubasta r
      WHERE r.identificador = x.compraId
        AND r.estadoPago = 'pagada'
  );

UPDATE cc
SET cc.montoGarantia = cc.montoGarantia + x.diferencia
FROM dbo.chequesCertificados cc
INNER JOIN @Correcciones x
    ON x.medioPagoId = cc.identificador
WHERE x.tipo = 'cheque'
  AND x.diferencia > 0
  AND EXISTS (
      SELECT 1
      FROM dbo.registroDeSubasta r
      WHERE r.identificador = x.compraId
        AND r.estadoPago = 'pagada'
  );

UPDATE r
SET r.comision = ic.comision
FROM dbo.registroDeSubasta r
INNER JOIN dbo.catalogos c
    ON c.subasta = r.subasta
INNER JOIN dbo.itemsCatalogo ic
    ON ic.catalogo = c.identificador
   AND ic.producto = r.producto
WHERE r.comision <> ic.comision;

COMMIT TRANSACTION;
