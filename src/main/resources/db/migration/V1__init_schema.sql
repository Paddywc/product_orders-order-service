CREATE TABLE `order_customer_details` (
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                          `customer_id` binary(16) NOT NULL,
                                          `customer_address` varchar(2000) NOT NULL,
                                          `customer_email` varchar(255) NOT NULL,
                                          PRIMARY KEY (`id`)
);



CREATE TABLE `order_table` (
                               `currency` varchar(3) NOT NULL,
                               `amount_cents` bigint(20) DEFAULT NULL,
                               `created_at` datetime(6) NOT NULL,
                               `customer_details_id` bigint(20) NOT NULL,
                               `updated_at` datetime(6) NOT NULL,
                               `version` bigint(20) DEFAULT NULL,
                               `order_id` binary(16) NOT NULL,
                               `inventory_status` enum('FAILED','PENDING','RELEASED','RESERVED') NOT NULL,
                               `order_status` enum('CANCELLED','CONFIRMED','CREATED') NOT NULL,
                               `payment_status` enum('COMPLETED','FAILED','PENDING','REFUNDED') NOT NULL,
                               PRIMARY KEY (`order_id`),
                               KEY `fk_order_table_cdi_ocd_idx` (`customer_details_id`),
                               CONSTRAINT `fk_order_table_cdi_ocd` FOREIGN KEY (`customer_details_id`) REFERENCES `order_customer_details` (`id`)
) ;



CREATE TABLE `order_item` (
                              `quantity` int(11) NOT NULL,
                              `amount_cents` bigint(20) DEFAULT NULL,
                              `id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `order_id` binary(16) NOT NULL,
                              `product_id` binary(16) NOT NULL,
                              `name_snapshot` varchar(255) NOT NULL,
                              `unit_price_currency` varchar(255) DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_order_item_order` (`order_id`,`product_id`),
                              CONSTRAINT `fk_order_item_order_id_order_table` FOREIGN KEY (`order_id`) REFERENCES `order_table` (`order_id`)
);

CREATE TABLE `processed_inventory_event` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                             `processed_at` datetime(6) NOT NULL,
                                             `event_id` binary(16) NOT NULL,
                                             PRIMARY KEY (`id`),
                                             UNIQUE KEY `uk_pie_event_id` (`event_id`)
) ;

CREATE TABLE `processed_payment_event` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                           `processed_at` datetime(6) NOT NULL,
                                           `event_id` binary(16) NOT NULL,
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_ppe_event_id` (`event_id`)
) ;
