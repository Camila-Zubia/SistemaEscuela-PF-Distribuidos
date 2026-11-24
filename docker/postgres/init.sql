-- Se ejecuta solo la primera vez que se crea el contenedor de PostgreSQL.
-- Cada microservicio tiene su propia base de datos.
CREATE DATABASE aval;
CREATE DATABASE notificar;
CREATE DATABASE mensajeria;
CREATE DATABASE integracion;
