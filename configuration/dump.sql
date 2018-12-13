CREATE TABLE IF NOT EXISTS coffee (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    price DOUBLE NOT NULL,
    units INTEGER DEFAULT 0,
    CONSTRAINT PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS money (
    id INT NOT NULL AUTO_INCREMENT,
    currency VARCHAR(10) NOT NULL,
    ammount DOUBLE DEFAULT 0.0,
    CONSTRAINT PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS giftcard (
    id INT NOT NULL AUTO_INCREMENT,
    token VARCHAR(8) NOT NULL,
    credit DOUBLE NOT NULL,
    CONSTRAINT PRIMARY KEY (id)
);

INSERT INTO coffee(name, price, units) VALUES ("ristretto", 1.0, 10), ("volutto", 1.2, 15), ("latte", 0.8, 6);
INSERT INTO money(currency, ammount) VALUES ("EUR", 3.0), ("USD", 0.0), ("GBP", 0.0), ("JPY", 0.0);
INSERT INTO giftcard(token, credit) VALUES ("82584629", 10.3), ("fde76cae", 3.2), ("381333b8", 7.1);



UPDATE coffee  SET units = units-1 WHERE  name="latte";

UPDATE coffee SET units = 9 WHERE name="latte";
