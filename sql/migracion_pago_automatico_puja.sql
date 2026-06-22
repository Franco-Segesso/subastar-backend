IF COL_LENGTH('dbo.pujos', 'modalidadEntrega') IS NULL
BEGIN
    ALTER TABLE dbo.pujos
    ADD modalidadEntrega VARCHAR(10) NULL;
END;
GO

UPDATE dbo.pujos
SET modalidadEntrega = 'retiro'
WHERE modalidadEntrega IS NULL;
GO

ALTER TABLE dbo.pujos
ALTER COLUMN modalidadEntrega VARCHAR(10) NOT NULL;
GO
