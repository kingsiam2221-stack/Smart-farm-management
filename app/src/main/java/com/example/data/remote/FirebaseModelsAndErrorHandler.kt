package com.example.data.remote

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val mobile: String = "",
    val role: String = "CUSTOMER",
    val preferredLanguage: String = "en",
    val preferredCurrency: String = "BDT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudFarm(
    @DocumentId val id: String = "",
    val userId: String = "",
    val farmCode: String = "",
    val name: String = "",
    val location: String = "",
    val address: String = "",
    val category: String = "MIXED",
    val managerName: String = "",
    val managerPhone: String = "",
    val initialInvestment: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudMarketplaceProduct(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val farmId: String = "",
    val farmName: String = "",
    val farmLocation: String = "",
    val name: String = "",
    val category: String = "Rice",
    val sourcingType: String = "FARM_PRODUCED",
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val unitCost: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val listed: Boolean = true,
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudOrder(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val paymentMethod: String = "Cash on Delivery",
    val farmId: String = "",
    val farmName: String = "",
    val farmOwnerUid: String = "",
    val productId: String = "",
    val itemsSummary: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudNotification(
    @DocumentId val id: String = "",
    val targetUserId: String = "",
    val senderUid: String = "",
    val orderId: String? = null,
    val title: String = "",
    val message: String = "",
    val category: String = "ORDER",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAgriLand(
    @DocumentId val id: String = "",
    val userId: String = "",
    val farmId: String? = null,
    val name: String = "",
    val areaSize: Double = 0.0,
    val areaUnit: String = "Decimal",
    val location: String = "",
    val soilOrLandType: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAgriCrop(
    @DocumentId val id: String = "",
    val userId: String = "",
    val farmId: String? = null,
    val landId: String = "",
    val cropType: String = "RICE",
    val title: String = "",
    val variety: String = "",
    val season: String = "Boro",
    val status: String = "GROWING",
    val yieldQuantity: Double = 0.0,
    val yieldUnit: String = "mon",
    val marketPricePerUnit: Double = 0.0,
    val yieldMarketWorth: Double = 0.0,
    val soldAmount: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAgriExpense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val farmId: String? = null,
    val landId: String = "",
    val cropId: String? = null,
    val sector: String = "SEEDS",
    val customSectorName: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudProductAd(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val productDocId: String? = null,
    val productName: String = "",
    val farmName: String = "",
    val priceText: String = "",
    val hookHeadlineBn: String = "",
    val promoBadgeBn: String = "",
    val benefitsPipeSeparatedBn: String = "",
    val callToActionBn: String = "",
    val voiceoverScriptBn: String = "",
    val orderStepsPipeSeparatedBn: String = "",
    val accentColorHex: String = "#1B5E20",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

