-- Datos de demostración para probar la plataforma en local (perfil dev). No se cargan en tests ni en producción.
-- Las imágenes (src/main/resources/demo/imagenes) las copia DatosDemoConfiguration a la carpeta de imágenes.
--
-- Cuentas (además de admin/admin y user/user):
--   fan/fan123       fan con aportes, reclamos y una recompensa entregada
--   murga/user       artista "La Murga Eléctrica"
--   cumbia/user      artista "Cumbia del Litoral"
-- user/user es el artista "Los Pasalagorra".

INSERT INTO jhi_user (id, login, password_hash, first_name, last_name, email, activated, lang_key, created_by, created_date)
VALUES (900001, 'fan', '$2a$10$zC6HrhSmh5BBp5uM7JRMP.B7d/i/3Xj60V/LdcCIkwzMw2goZh3ru', 'Lucía', 'Fernández', 'fan@localhost', TRUE, 'es', 'system', NOW()),
       (900002, 'murga', '$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K', 'Martín', 'Suárez', 'murga@localhost', TRUE, 'es', 'system', NOW()),
       (900003, 'cumbia', '$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K', 'Romina', 'Acosta', 'cumbia@localhost', TRUE, 'es', 'system', NOW());

INSERT INTO jhi_user_authority (user_id, authority_name)
VALUES (900001, 'ROLE_USER'), (900002, 'ROLE_USER'), (900003, 'ROLE_USER'),
       (2, 'ROLE_ARTISTA'), (900002, 'ROLE_ARTISTA'), (900003, 'ROLE_ARTISTA');

INSERT INTO direccion_envio (id, user_id, calle, numero, piso_depto, localidad, provincia, codigo_postal, telefono)
VALUES (900001, 900001, 'Av. Corrientes', '1234', '5° B', 'CABA', 'Ciudad Autónoma de Buenos Aires', 'C1043', '11 5555-1234'),
       (900002, 1, 'Bv. Oroño', '850', NULL, 'Rosario', 'Santa Fe', 'S2000', NULL);

INSERT INTO artista (id, user_id, nombre_artistico, nombre_completo, dni, cuit, ciudad, provincia, biografia, estado, fecha_solicitud, fecha_revision)
VALUES (900001, 2, 'Los Pasalagorra', 'Juan Pérez', '12345678', '20123456786', 'Rosario', 'Santa Fe', 'Rock barrial desde 2015.', 'APROBADO', NOW() - INTERVAL '90 days', NOW() - INTERVAL '89 days'),
       (900002, 900002, 'La Murga Eléctrica', 'Martín Suárez', '30111222', '20301112229', 'Montevideo', 'Ciudad Autónoma de Buenos Aires', 'Murga rioplatense enchufada.', 'APROBADO', NOW() - INTERVAL '60 days', NOW() - INTERVAL '59 days'),
       (900003, 900003, 'Cumbia del Litoral', 'Romina Acosta', '28999888', '27289998887', 'Paraná', 'Entre Ríos', 'Cumbia santafesina y entrerriana.', 'APROBADO', NOW() - INTERVAL '60 days', NOW() - INTERVAL '58 days');

INSERT INTO campania (id, artista_id, titulo, slug, descripcion, video_url, meta, fecha_cierre, estado, fecha_creacion, fecha_envio_revision, fecha_publicacion, comision_porcentaje)
VALUES
 (900001, 900002, 'Primer EP de La Murga Eléctrica', 'primer-ep-murga-electrica',
  E'Somos una murga que se enchufó. Queremos grabar nuestro primer EP en un estudio de verdad, con cinco temas que venimos tocando en los carnavales de Montevideo y Buenos Aires.\n\nCon tu aporte pagamos las horas de estudio, la mezcla y el master. Si llegamos a la meta, en marzo sale en todas las plataformas.',
  'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 800000, CURRENT_DATE + 12, 'PUBLICADA', NOW() - INTERVAL '20 days', NOW() - INTERVAL '19 days', NOW() - INTERVAL '18 days', 8),
 (900002, 900003, 'Vinilo de Cumbia del Litoral', 'vinilo-cumbia-del-litoral',
  E'Prensamos 300 vinilos de nuestro primer disco, con arte de tapa de un ilustrador de Paraná.\n\nYa pasamos la meta: lo que sume de acá al cierre va a una segunda tirada.',
  NULL, 1200000, CURRENT_DATE + 3, 'PUBLICADA', NOW() - INTERVAL '40 days', NOW() - INTERVAL '39 days', NOW() - INTERVAL '38 days', 8),
 (900003, 900001, 'Gira por el norte', 'gira-por-el-norte',
  E'Diez fechas entre Tucumán, Salta y Jujuy. Con tu aporte cubrimos el alquiler de la combi y el sonido.',
  NULL, 650000, CURRENT_DATE + 25, 'PUBLICADA', NOW() - INTERVAL '10 days', NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days', 8),
 (900004, 900002, 'Videoclip "Noche de Paraná"', 'videoclip-noche-de-parana',
  E'Filmamos el videoclip en la costanera, con drone y una banda de 20 personas en escena.',
  NULL, 400000, CURRENT_DATE + 40, 'PUBLICADA', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days', 8),
 (900005, 900003, 'Gira patagónica 2027', 'gira-patagonica-2027',
  E'De Neuquén a Ushuaia en quince días. Cada aporte es un litro de nafta y una noche de hostel.',
  NULL, 2500000, CURRENT_DATE + 28, 'PUBLICADA', NOW() - INTERVAL '12 days', NOW() - INTERVAL '11 days', NOW() - INTERVAL '10 days', 8),
 (900006, 900001, 'Disco en vivo en el Teatro El Círculo', 'disco-en-vivo-el-circulo',
  E'Grabamos el show del Teatro El Círculo y lo vamos a editar en CD y digital. ¡Llegamos a la meta gracias a ustedes!',
  NULL, 300000, CURRENT_DATE - 5, 'EXITOSA', NOW() - INTERVAL '70 days', NOW() - INTERVAL '69 days', NOW() - INTERVAL '68 days', 8),
 (900007, 900001, 'Documental de la banda', 'documental-de-la-banda',
  E'Diez años de rock barrial en 50 minutos.', NULL, 900000, CURRENT_DATE + 50, 'EN_REVISION', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 days', NULL, NULL),
 (900008, 900001, 'Nuevo disco (borrador)', 'nuevo-disco-borrador',
  E'Todavía lo estamos armando.', NULL, 500000, CURRENT_DATE + 45, 'BORRADOR', NOW(), NULL, NULL, NULL);

INSERT INTO campania_imagen (id, campania_id, archivo, orden)
VALUES (900001, 900001, 'd0000000-0000-4000-8000-000000000000.jpg', 0),
       (900002, 900001, 'd0000000-0000-4000-8000-000000000003.jpg', 1),
       (900003, 900002, 'd0000000-0000-4000-8000-000000000001.jpg', 0),
       (900004, 900003, 'd0000000-0000-4000-8000-000000000002.jpg', 0),
       (900005, 900004, 'd0000000-0000-4000-8000-000000000004.jpg', 0),
       (900006, 900005, 'd0000000-0000-4000-8000-000000000005.jpg', 0),
       (900007, 900006, 'd0000000-0000-4000-8000-000000000006.jpg', 0),
       (900008, 900007, 'd0000000-0000-4000-8000-000000000007.jpg', 0);

INSERT INTO recompensa (id, campania_id, titulo, descripcion, monto_minimo, stock, cantidad_vendida, fecha_entrega_estimada, requiere_envio, orden)
VALUES
 (900001, 900001, 'Agradecimiento en redes', 'Te nombramos en el posteo de lanzamiento.', 3000, NULL, 4, CURRENT_DATE + 60, FALSE, 0),
 (900002, 900001, 'EP en digital + sticker', 'Descarga anticipada y un sticker de la murga.', 8000, NULL, 6, CURRENT_DATE + 60, TRUE, 1),
 (900003, 900001, 'Ensayo abierto', 'Venís a un ensayo con un acompañante.', 25000, 20, 18, CURRENT_DATE + 90, FALSE, 2),
 (900004, 900002, 'Vinilo firmado', 'El vinilo firmado por la banda, con envío a todo el país.', 45000, 300, 26, CURRENT_DATE + 120, TRUE, 0),
 (900005, 900002, 'Vinilo + remera', 'El vinilo y la remera oficial.', 70000, 50, 4, CURRENT_DATE + 120, TRUE, 1),
 (900006, 900003, 'Entrada anticipada', 'Una entrada para la fecha que elijas.', 12000, NULL, 2, CURRENT_DATE + 40, FALSE, 0),
 (900007, 900004, 'Aparecés en el video', 'Sos extra en una escena.', 30000, 10, 2, CURRENT_DATE + 70, FALSE, 0),
 (900008, 900005, 'Postal de cada ciudad', 'Te mandamos una postal escrita desde cada fecha.', 20000, NULL, 8, CURRENT_DATE + 60, TRUE, 0),
 (900009, 900006, 'CD firmado', 'El disco en vivo firmado.', 15000, NULL, 3, CURRENT_DATE + 20, TRUE, 0),
 (900010, 900007, 'Avant premiere', 'Entrada al estreno.', 10000, 100, 0, CURRENT_DATE + 120, FALSE, 0),
 (900011, 900008, 'Disco en digital', 'Descarga anticipada.', 5000, NULL, 0, CURRENT_DATE + 90, FALSE, 0);

-- Aportes: los grandes son "de otros fans" (de admin), para que las campañas tengan movimiento.
INSERT INTO aporte (id, campania_id, user_id, recompensa_id, monto, estado, referencia, pago_id, fecha_creacion, fecha_pago, entregado, fecha_entrega)
VALUES
 (900001, 900001, 1, NULL, 470000, 'APROBADO', 'demo-0001', 'SIM-demo-0001', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', FALSE, NULL),
 (900002, 900001, 900001, 900002, 10000, 'APROBADO', 'demo-0002', 'SIM-demo-0002', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', FALSE, NULL),
 (900003, 900002, 1, NULL, 1250000, 'APROBADO', 'demo-0003', 'SIM-demo-0003', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', FALSE, NULL),
 (900004, 900002, 900001, 900004, 45000, 'APROBADO', 'demo-0004', 'SIM-demo-0004', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days', FALSE, NULL),
 (900005, 900003, 1, NULL, 98000, 'APROBADO', 'demo-0005', 'SIM-demo-0005', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', FALSE, NULL),
 (900006, 900004, 1, NULL, 76000, 'APROBADO', 'demo-0006', 'SIM-demo-0006', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', FALSE, NULL),
 (900007, 900005, 1, NULL, 910000, 'APROBADO', 'demo-0007', 'SIM-demo-0007', NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days', FALSE, NULL),
 (900008, 900006, 1, NULL, 290000, 'APROBADO', 'demo-0008', 'SIM-demo-0008', NOW() - INTERVAL '40 days', NOW() - INTERVAL '40 days', FALSE, NULL),
 (900009, 900006, 900001, 900009, 15000, 'APROBADO', 'demo-0009', 'SIM-demo-0009', NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days', TRUE, NOW() - INTERVAL '2 days'),
 (900010, 900006, 1, 900009, 15000, 'APROBADO', 'demo-0010', 'SIM-demo-0010', NOW() - INTERVAL '34 days', NOW() - INTERVAL '34 days', FALSE, NULL),
 (900011, 900003, 900001, NULL, 5000, 'RECHAZADO', 'demo-0011', 'SIM-demo-0011', NOW() - INTERVAL '1 days', NULL, FALSE, NULL);

INSERT INTO novedad (id, campania_id, titulo, texto, fecha)
VALUES (900001, 900006, '¡Llegamos a la meta!', E'Gracias a todos los que bancaron. Ya estamos mezclando el show.', NOW() - INTERVAL '4 days'),
       (900002, 900006, 'Salieron los primeros CDs', E'Los primeros CDs firmados ya están en el correo. El resto sale esta semana.', NOW() - INTERVAL '2 days'),
       (900003, 900001, 'Entramos al estudio', E'Arrancamos a grabar las baterías. ¡Gracias por el aguante!', NOW() - INTERVAL '5 days');

INSERT INTO reclamo (id, aporte_id, tipo, texto, estado, fecha)
VALUES (900001, 900010, 'INCUMPLIMIENTO', 'Pasaron las fechas estimadas y todavía no recibí el CD.', 'ABIERTO', NOW() - INTERVAL '1 days');
