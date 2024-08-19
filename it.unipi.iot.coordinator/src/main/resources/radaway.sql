DROP DATABASE IF EXISTS radaway;

CREATE DATABASE radaway;

USE radaway;

CREATE TABLE `temperature`(
	`timestamp`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	`value`	DECIMAL NOT NULL,
	PRIMARY KEY (`timestamp`, `value`)
);

CREATE TABLE `pressure`(
	`timestamp`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	`value`	DECIMAL NOT NULL,
	PRIMARY KEY (`timestamp`, `value`)
);

CREATE TABLE `neutron_flux`(
	`timestamp`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	`value`	DECIMAL NOT NULL,
	PRIMARY KEY (`timestamp`, `value`)
);

CREATE TABLE `actuator` (
    `ip` VARCHAR(100) PRIMARY KEY,
    `type`      VARCHAR(40) NOT NULL
);
