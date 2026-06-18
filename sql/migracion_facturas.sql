IF COL_LENGTH('dbo.registroDeSubasta', 'modalidadEntrega') IS NULL
BEGIN
    ALTER TABLE dbo.registroDeSubasta
    ADD modalidadEntrega VARCHAR(10) NOT NULL
        CONSTRAINT df_registroDeSubasta_modalidadEntrega DEFAULT ('pendiente');
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'chk_registroDeSubasta_modalidadEntrega'
)
BEGIN
    ALTER TABLE dbo.registroDeSubasta
    ADD CONSTRAINT chk_registroDeSubasta_modalidadEntrega
        CHECK (modalidadEntrega IN ('pendiente', 'envio', 'retiro'));
END;
GO

INSERT INTO dbo.registroDeSubasta (
    subasta,
    duenio,
    producto,
    cliente,
    importe,
    comision,
    costoEnvio,
    nroPolizaSeguro,
    modalidadEntrega
)
SELECT
    a.subasta,
    pr.duenio,
    ic.producto,
    a.cliente,
    p.importe,
    ROUND(p.importe * ic.comision / 100.0, 2),
    NULL,
    pr.seguro,
    'pendiente'
FROM dbo.pujos p
INNER JOIN dbo.asistentes a
    ON a.identificador = p.asistente
INNER JOIN dbo.itemsCatalogo ic
    ON ic.identificador = p.item
INNER JOIN dbo.productos pr
    ON pr.identificador = ic.producto
WHERE LOWER(LTRIM(RTRIM(p.ganador))) = 'si'
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.registroDeSubasta r
      WHERE r.subasta = a.subasta
        AND r.producto = ic.producto
        AND r.cliente = a.cliente
  );
GO
