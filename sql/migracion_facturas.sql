IF COL_LENGTH('dbo.registroDeSubasta', 'modalidadEntrega') IS NULL
BEGIN
    ALTER TABLE dbo.registroDeSubasta
    ADD modalidadEntrega VARCHAR(10) NOT NULL
        CONSTRAINT df_registroDeSubasta_modalidadEntrega DEFAULT ('pendiente');
END;
GO

IF COL_LENGTH('dbo.pujos', 'medioPago') IS NULL
BEGIN
    ALTER TABLE dbo.pujos ADD medioPago INT NULL;
END;
GO

IF COL_LENGTH('dbo.registroDeSubasta', 'medioPago') IS NULL
BEGIN
    ALTER TABLE dbo.registroDeSubasta ADD medioPago INT NULL;
END;
GO

IF COL_LENGTH('dbo.registroDeSubasta', 'estadoPago') IS NULL
BEGIN
    ALTER TABLE dbo.registroDeSubasta
    ADD estadoPago VARCHAR(15) NOT NULL
        CONSTRAINT df_registroDeSubasta_estadoPago DEFAULT ('pendiente');
END;
GO

IF COL_LENGTH('dbo.registroDeSubasta', 'fechaPago') IS NULL
BEGIN
    ALTER TABLE dbo.registroDeSubasta ADD fechaPago DATETIME NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_pujos_medioPago'
)
BEGIN
    ALTER TABLE dbo.pujos
    ADD CONSTRAINT fk_pujos_medioPago
        FOREIGN KEY (medioPago) REFERENCES dbo.mediosDePago(identificador);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_registroDeSubasta_medioPago'
)
BEGIN
    ALTER TABLE dbo.registroDeSubasta
    ADD CONSTRAINT fk_registroDeSubasta_medioPago
        FOREIGN KEY (medioPago) REFERENCES dbo.mediosDePago(identificador);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'chk_registroDeSubasta_estadoPago'
)
BEGIN
    ALTER TABLE dbo.registroDeSubasta
    ADD CONSTRAINT chk_registroDeSubasta_estadoPago
        CHECK (estadoPago IN ('pendiente', 'pagada'));
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
    modalidadEntrega,
    medioPago,
    estadoPago
)
SELECT
    a.subasta,
    pr.duenio,
    ic.producto,
    a.cliente,
    p.importe,
    ic.comision,
    NULL,
    pr.seguro,
    'pendiente',
    p.medioPago,
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
