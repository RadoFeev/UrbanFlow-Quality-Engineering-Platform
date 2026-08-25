ALTER TABLE shipments
    RENAME CONSTRAINT shipments_weight_grams_check TO ck_shipments_weight_grams;

ALTER TABLE shipments
    RENAME CONSTRAINT shipments_length_cm_check TO ck_shipments_length_cm;

ALTER TABLE shipments
    RENAME CONSTRAINT shipments_width_cm_check TO ck_shipments_width_cm;

ALTER TABLE shipments
    RENAME CONSTRAINT shipments_height_cm_check TO ck_shipments_height_cm;
