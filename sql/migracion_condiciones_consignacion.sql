IF COL_LENGTH('dbo.solicitudesConsignacion', 'catalogoPropuesto') IS NULL
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD catalogoPropuesto INT NULL;
END;
GO

IF COL_LENGTH('dbo.solicitudesConsignacion', 'precioBasePropuesto') IS NULL
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD precioBasePropuesto DECIMAL(18, 2) NULL;
END;
GO

UPDATE solicitud
SET catalogoPropuesto = item.catalogo,
    precioBasePropuesto = item.precioBase
FROM dbo.solicitudesConsignacion solicitud
INNER JOIN dbo.productos producto
    ON producto.identificador = solicitud.producto
OUTER APPLY (
    SELECT TOP 1 item.catalogo, item.precioBase
    FROM dbo.itemsCatalogo item
    WHERE item.producto = producto.identificador
    ORDER BY item.identificador DESC
) item
WHERE solicitud.catalogoPropuesto IS NULL
  AND item.catalogo IS NOT NULL;
GO

DELETE item
FROM dbo.itemsCatalogo item
INNER JOIN dbo.solicitudesConsignacion solicitud
    ON solicitud.producto = item.producto
WHERE LOWER(LTRIM(RTRIM(COALESCE(solicitud.condicionesAceptadas, 'no')))) <> 'si'
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.pujos puja
      WHERE puja.item = item.identificador
  )
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.registroDeSubasta registro
      WHERE registro.producto = item.producto
  );
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_solicitudesConsignacion_catalogoPropuesto'
)
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD CONSTRAINT fk_solicitudesConsignacion_catalogoPropuesto
        FOREIGN KEY (catalogoPropuesto)
        REFERENCES dbo.catalogos(identificador);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'chk_solicitudesConsignacion_precioBasePropuesto'
)
BEGIN
    ALTER TABLE dbo.solicitudesConsignacion
    ADD CONSTRAINT chk_solicitudesConsignacion_precioBasePropuesto
        CHECK (
            precioBasePropuesto IS NULL
            OR precioBasePropuesto > 0
        );
END;
GO
