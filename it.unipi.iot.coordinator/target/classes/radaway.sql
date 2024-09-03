DROP DATABASE IF EXISTS radaway;

CREATE DATABASE radaway;

USE radaway;

CREATE TABLE `temperature`(
	`timestamp`	TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
	`value`	DECIMAL NOT NULL,
	PRIMARY KEY (`timestamp`, `value`)
);

CREATE TABLE `pressure`(
	`timestamp`	TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
	`value`	DECIMAL NOT NULL,
	PRIMARY KEY (`timestamp`, `value`)
);

CREATE TABLE `neutron_flux`(
	`timestamp`	TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
	`value`	DECIMAL NOT NULL,
	PRIMARY KEY (`timestamp`, `value`)
);

CREATE TABLE `actuator` (
    `ipv6` VARCHAR(100) PRIMARY KEY,
    `type` VARCHAR(40) NOT NULL,
	`sensor_types` VARCHAR(255) NOT NULL
);

CREATE TABLE `actuator_control_rods` (
    `ipv6` VARCHAR(100) NOT NULL,
	`timestamp`	TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
	`value` DECIMAL NOT NULL,
	PRIMARY KEY (`ipv6`, `timestamp`, `value`)
);


CREATE TABLE `actuator_coolant_flow` (
    `ipv6` VARCHAR(100) NOT NULL,
	`timestamp`	TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
	`value` DECIMAL NOT NULL,
	PRIMARY KEY (`ipv6`, `timestamp`, `value`)
);
