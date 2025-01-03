CREATE KEYSPACE IF NOT EXISTS Test
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

USE Test;

CREATE TABLE IF NOT EXISTS orders (
   order_id uuid,
   train_id int,
   trip_date timestamp,
   user_id uuid,
   car int,
   seats_amount int,
   PRIMARY KEY ((train_id, trip_date), car, order_id)
);

CREATE TABLE IF NOT EXISTS users (
   user_id uuid,
   name text,
   PRIMARY KEY (user_id)
);

CREATE TABLE IF NOT EXISTS trains (
   train_id int,
   trip_date timestamp,
   cars int,
   seats_per_car int,
   PRIMARY KEY ((train_id, trip_date))
);

CREATE TABLE IF NOT EXISTS orders_per_user (
   order_id uuid,
   train_id int,
   trip_date timestamp,
   user_id uuid,
   car int,
   seats_amount int,
   PRIMARY KEY ((train_id, trip_date, user_id), order_id, car)
);