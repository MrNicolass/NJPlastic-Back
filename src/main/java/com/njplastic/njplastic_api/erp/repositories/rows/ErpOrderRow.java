package com.njplastic.njplastic_api.erp.repositories.rows;

/**
 * Single open production order returned by the customer ERP. Mapped from the
 * configurable {@code app.datasource.erp.query.find-open-orders} SELECT - the
 * SELECT must return the same column order: erp_order_id, machine_code,
 * product_code, target_quantity, status, payload_json. Not an API contract; the
 * sync service writes one row of {@code production_order_cache} per record.
 *
 * @param erpOrderId     external order identifier (PK in the ERP)
 * @param machineCode    machine code the order is bound to (may be null)
 * @param productCode    SKU produced by the order
 * @param targetQuantity planned production quantity
 * @param status         ERP-side status string (typically OPEN)
 * @param payloadJson    free-form JSON document with extra fields
 */
public record ErpOrderRow(
    String erpOrderId,
    String machineCode,
    String productCode,
    Integer targetQuantity,
    String status,
    String payloadJson) {
}