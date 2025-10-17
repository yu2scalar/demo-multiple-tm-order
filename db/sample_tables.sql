CREATE NAMESPACE IF NOT EXISTS "shopping";

CREATE TABLE IF NOT EXISTS shopping.order (
  id TEXT,
  order_datetime TIMESTAMP,
  order_qty INT,
  product_id INT,
  PRIMARY KEY ((id))
);

