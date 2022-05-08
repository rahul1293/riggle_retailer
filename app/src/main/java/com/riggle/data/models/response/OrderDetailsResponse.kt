package com.riggle.data.models.response

data class OrderDetailsResponse(
    val amount: Float,
    val redeemed_riggle_coins_amount: Float,
    val amount_at_delivery_boy: Float,
    val amount_at_service_hub: Float,
    val cancellation_reason: Any,
    val cancelled_at: Any,
    val challan_file: String,
    val code: String,
    val confirmed_at: String,
    val created_at: String,
    val delivered_at: Any,
    val delivery_boy: Any,
    val delivery_date: String,
    val delivery_location: Any,
    val doc_id: String,
    val final_amount: Float,
    val id: Int,
    val paid_amount: Float,
    val paid_at: Any,
    val payment_location: String?,
    val payment_mode: Any,
    val payment_receipt: Any,
    val payment_reschedule_reason: Any,
    val payment_rescheduled_to: String?,
    val pending_amount: Float,
    val placed_at: String,
    val products: List<ProductsData>,
    val redeemed_riggle_coins: Int,
    val retailer: Int,
    val riggle_coins: Int,
    val service_hub: ServiceHub,
    var status: String,
    val update_url: String,
    val updated_at: String
)

/*data class Product(
    val amount: Float,
    val created_at: String,
    val name: String?,
    val code: String?,
    val step: Int = 0,
    val is_active: Boolean = false,
    val free_product: FreeProduct?,
    val free_product_quantity: Int,
    val id: Int,
    val order: Int,
    val ordered_quantity: Int,
    val original_rate: Int,
    val product: ProductX?,
    val products: List<ProductList>?,
    val product_combo: Int?, //ComboProducts?
    var quantity: Int,
    val rate: Double,
    val riggle_coins: Int,
    val update_url: String,
    val updated_at: String,
    val banner_image: BannerImage?
)*/

/*
data class ProductX(
    val base_quantity: Int,
    val base_rate: Int,
    val base_unit: String,
    val brand: Int,
    val category: Int,
    val code: String,
    val company_rate: Double,
    val company_step: Int,
    val created_at: String,
    val delivery_tat_days: Int,
    val description: String,
    val expiry_in_days: Int,
    val id: Int,
    val inactive_pincodes: List<Any>,
    val is_active: Boolean,
    val name: String,
    val normalized_weight: Double,
    val retailer_step: Int,
    val riggle_coins: Int,
    val update_url: String,
    val updated_at: String,
    val banner_image: BannerImage
)

data class FreeProduct(
    val base_quantity: Int,
    val base_rate: Double,
    val base_unit: String,
    val brand: Int,
    val category: Int,
    val code: String,
    val company_rate: Double,
    val company_step: Int,
    val created_at: String,
    val delivery_tat_days: Int,
    val description: String,
    val expiry_in_days: Int,
    val id: Int,
    val inactive_pincodes: List<Any>,
    val is_active: Boolean,
    val name: String,
    val normalized_weight: Double,
    val retailer_step: Int,
    val riggle_coins: Int,
    val update_url: String,
    val updated_at: String
)*/