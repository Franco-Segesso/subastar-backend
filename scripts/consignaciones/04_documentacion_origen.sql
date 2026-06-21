/*
    Agrega soporte persistente para documentacion de origen de consignaciones.
    Es idempotente: puede ejecutarse mas de una vez.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

IF COL_LENGTH('dbo.solicitudesConsignacion', 'motivoDocumentacion') IS NULL
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD motivoDocumentacion VARCHAR(500) NULL;
END;

IF OBJECT_ID('dbo.documentosConsignacion', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.documentosConsignacion (
        identificador INT IDENTITY(1, 1) NOT NULL,
        solicitud INT NOT NULL,
        urlArchivo VARCHAR(500) NOT NULL,
        nombreArchivo VARCHAR(250) NOT NULL,
        descripcion VARCHAR(500) NULL,
        fechaCarga DATETIME NOT NULL
            CONSTRAINT df_documentosConsignacion_fechaCarga DEFAULT GETDATE(),
        estado VARCHAR(15) NOT NULL
            CONSTRAINT df_documentosConsignacion_estado DEFAULT 'pendiente',
        CONSTRAINT pk_documentosConsignacion
            PRIMARY KEY CLUSTERED (identificador),
        CONSTRAINT fk_documentosConsignacion_solicitud
            FOREIGN KEY (solicitud)
            REFERENCES dbo.solicitudesConsignacion(identificador),
        CONSTRAINT chk_documentosConsignacion_estado
            CHECK (estado IN ('pendiente', 'aprobado', 'rechazado'))
    );
END;

IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.solicitudesConsignacion')
      AND name = 'chkEstadoConsignacion'
)
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    DROP CONSTRAINT chkEstadoConsignacion;
END;

ALTER TABLE dbo.solicitudesConsignacion
ALTER COLUMN estado VARCHAR(30) NULL;

ALTER TABLE dbo.solicitudesConsignacion
ADD CONSTRAINT chkEstadoConsignacion CHECK (
    estado IN (
        'pendiente',
        'documentacion_pendiente',
        'documentacion_presentada',
        'aceptado',
        'rechazado'
    )
);

COMMIT TRANSACTION;
