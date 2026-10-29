-- La comisión se fija en cada campaña al publicarla: si el porcentaje de la plataforma cambia, las campañas ya
-- publicadas mantienen el suyo. Las publicadas hasta ahora usaron el 8% (el valor de application.comision-porcentaje).
ALTER TABLE campania ADD COLUMN comision_porcentaje NUMERIC(5, 2);

UPDATE campania SET comision_porcentaje = 8 WHERE estado NOT IN ('BORRADOR', 'EN_REVISION');
