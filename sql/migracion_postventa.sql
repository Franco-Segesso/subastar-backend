IF COL_LENGTH('dbo.cuentasDestino', 'solicitud') IS NULL
BEGIN
    ALTER TABLE dbo.cuentasDestino ADD solicitud INT NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_cuentasDestino_solicitud'
)
BEGIN
    ALTER TABLE dbo.cuentasDestino
    ADD CONSTRAINT fk_cuentasDestino_solicitud
        FOREIGN KEY (solicitud)
        REFERENCES dbo.solicitudesConsignacion(identificador);
END;
GO

IF COL_LENGTH('dbo.registroDeSubasta', 'estadoEntrega') IS NULL
BEGIN
    ALTER TABLE dbo.registroDeSubasta
    ADD estadoEntrega VARCHAR(20) NULL
        CONSTRAINT df_registroDeSubasta_estadoEntrega DEFAULT ('pendiente');
END;
GO

IF COL_LENGTH('dbo.registroDeSubasta', 'fechaEntrega') IS NULL
BEGIN
    ALTER TABLE dbo.registroDeSubasta ADD fechaEntrega DATETIME NULL;
END;
GO

IF EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.solicitudesConsignacion')
      AND name = 'chkEstadoConsignacion'
)
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    DROP CONSTRAINT chkEstadoConsignacion;
END;
GO

ALTER TABLE dbo.solicitudesConsignacion
ALTER COLUMN estado VARCHAR(30) NULL;
GO

ALTER TABLE dbo.solicitudesConsignacion
ADD CONSTRAINT chkEstadoConsignacion CHECK (
    estado IN (
        'pendiente',
        'documentacion_pendiente',
        'documentacion_presentada',
        'aceptado',
        'rechazado',
        'vendida'
    )
);
GO
