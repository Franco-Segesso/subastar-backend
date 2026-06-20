IF COL_LENGTH('dbo.solicitudesConsignacion', 'costoDevolucion') IS NULL
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD costoDevolucion DECIMAL(18, 2) NULL;
END;
GO

IF COL_LENGTH('dbo.solicitudesConsignacion', 'monedaDevolucion') IS NULL
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD monedaDevolucion VARCHAR(3) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'chk_solicitudesConsignacion_monedaDevolucion'
)
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD CONSTRAINT chk_solicitudesConsignacion_monedaDevolucion
        CHECK (
            monedaDevolucion IS NULL
            OR monedaDevolucion IN ('ARS', 'USD')
        );
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'chk_solicitudesConsignacion_costoDevolucion'
)
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD CONSTRAINT chk_solicitudesConsignacion_costoDevolucion
        CHECK (costoDevolucion IS NULL OR costoDevolucion >= 0);
END;
GO
