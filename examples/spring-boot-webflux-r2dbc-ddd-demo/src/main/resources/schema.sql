DROP TABLE IF EXISTS order_projections;
DROP TABLE IF EXISTS payment_reservations;
DROP TABLE IF EXISTS demo_orders;

CREATE TABLE demo_orders (
  id VARCHAR(36) PRIMARY KEY,
  customer_id VARCHAR(100) NOT NULL,
  amount NUMERIC(19, 2) NOT NULL,
  status VARCHAR(50) NOT NULL
);

CREATE TABLE payment_reservations (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36) NOT NULL,
  amount NUMERIC(19, 2) NOT NULL
);

CREATE TABLE order_projections (
  order_id VARCHAR(36) PRIMARY KEY,
  customer_id VARCHAR(100) NOT NULL,
  amount NUMERIC(19, 2) NOT NULL,
  status VARCHAR(50) NOT NULL
);
