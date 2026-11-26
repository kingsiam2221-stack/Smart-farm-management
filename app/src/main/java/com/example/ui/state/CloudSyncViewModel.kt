package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.R
import com.example.data.remote.CloudAgriCrop
import com.example.data.remote.CloudAgriExpense
import com.example.data.remote.CloudAgriLand
import com.example.data.remote.CloudFarm
import com.example.data.remote.CloudMarketplaceProduct
import com.example.data.remote.CloudNotification
import com.example.data.remote.CloudOrder
import com.example.data.remote.CloudProductAd
import com.example.data.remote.CloudSyncRepository
import com.example.data.remote.CloudUserProfile
import com.example.data.remote.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CloudSyncViewModel(
    private val repository: CloudSyncRepository,
    val currentUserId: String
) : ViewModel() {

    val userProfileState: StateFlow<UiState<CloudUserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow("user profile")

    val farmsState: StateFlow<UiState<List<CloudFarm>>> =
        repository.observeUserFarms(currentUserId)
            .toUiStateFlow("farms")

    val publicMarketplaceState: StateFlow<UiState<List<CloudMarketplaceProduct>>> =
        repository.observePublicMarketplaceProducts()
            .toUiStateFlow("marketplace products")

    val myMarketplaceState: StateFlow<UiState<List<CloudMarketplaceProduct>>> =
        repository.observeMyMarketplaceProducts(currentUserId)
            .toUiStateFlow("my marketplace products")

    val customerOrdersState: StateFlow<UiState<List<CloudOrder>>> =
        repository.observeCustomerOrders(currentUserId)
            .toUiStateFlow("customer orders")

    val farmerOrdersState: StateFlow<UiState<List<CloudOrder>>> =
        repository.observeFarmerOrders(currentUserId)
            .toUiStateFlow("farmer orders")

    val notificationsState: StateFlow<UiState<List<CloudNotification>>> =
        repository.observeNotifications(currentUserId)
            .toUiStateFlow("notifications")

    val agriLandsState: StateFlow<UiState<List<CloudAgriLand>>> =
        repository.observeAgriLands(currentUserId)
            .toUiStateFlow("agri lands")

    val agriCropsState: StateFlow<UiState<List<CloudAgriCrop>>> =
        repository.observeAgriCrops(currentUserId)
            .toUiStateFlow("agri crops")

    val agriExpensesState: StateFlow<UiState<List<CloudAgriExpense>>> =
        repository.observeAgriExpenses(currentUserId)
            .toUiStateFlow("agri expenses")

    val activeProductAdsState: StateFlow<UiState<List<CloudProductAd>>> =
        repository.observeActiveProductAds()
            .toUiStateFlow("active product ads")

    val myProductAdsState: StateFlow<UiState<List<CloudProductAd>>> =
        repository.observeMyProductAds(currentUserId)
            .toUiStateFlow("my product ads")

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to observe $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun syncUserProfile(
        displayName: String,
        email: String,
        mobile: String,
        role: String,
        preferredLanguage: String,
        preferredCurrency: String
    ) {
        viewModelScope.launch {
            repository.upsertUserProfile(
                displayName = displayName,
                email = email,
                mobile = mobile,
                role = role,
                preferredLanguage = preferredLanguage,
                preferredCurrency = preferredCurrency
            )
        }
    }

    fun syncFarm(
        farmDocId: String,
        farmCode: String,
        name: String,
        location: String,
        address: String,
        category: String,
        managerName: String,
        managerPhone: String,
        initialInvestment: Double
    ) {
        viewModelScope.launch {
            repository.upsertFarm(
                farmDocId = farmDocId,
                farmCode = farmCode,
                name = name,
                location = location,
                address = address,
                category = category,
                managerName = managerName,
                managerPhone = managerPhone,
                initialInvestment = initialInvestment
            )
        }
    }

    fun syncMarketplaceProduct(
        productDocId: String,
        farmId: String,
        farmName: String,
        farmLocation: String,
        name: String,
        category: String,
        sourcingType: String,
        quantity: Double,
        unit: String,
        unitCost: Double,
        sellingPrice: Double,
        listed: Boolean,
        description: String
    ) {
        viewModelScope.launch {
            repository.upsertMarketplaceProduct(
                productDocId = productDocId,
                farmId = farmId,
                farmName = farmName,
                farmLocation = farmLocation,
                name = name,
                category = category,
                sourcingType = sourcingType,
                quantity = quantity,
                unit = unit,
                unitCost = unitCost,
                sellingPrice = sellingPrice,
                listed = listed,
                description = description
            )
        }
    }

    fun deleteMarketplaceProduct(productDocId: String) {
        viewModelScope.launch {
            repository.deleteMarketplaceProduct(productDocId)
        }
    }

    fun placeCloudOrder(
        orderDocId: String,
        orderNumber: String,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        paymentMethod: String,
        product: CloudMarketplaceProduct,
        itemsSummary: String,
        totalAmount: Double
    ) {
        viewModelScope.launch {
            repository.placeOrderWithFarmNotification(
                orderDocId = orderDocId,
                orderNumber = orderNumber,
                customerName = customerName,
                customerPhone = customerPhone,
                deliveryAddress = deliveryAddress,
                paymentMethod = paymentMethod,
                product = product,
                itemsSummary = itemsSummary,
                totalAmount = totalAmount
            )
        }
    }

    fun updateCloudOrderStatus(orderDocId: String, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderDocId, status)
        }
    }

    fun markCloudNotificationRead(notificationDocId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationDocId)
        }
    }

    fun syncAgriLand(
        landDocId: String,
        farmId: String?,
        name: String,
        areaSize: Double,
        areaUnit: String,
        location: String,
        soilOrLandType: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.upsertAgriLand(
                landDocId = landDocId,
                farmId = farmId,
                name = name,
                areaSize = areaSize,
                areaUnit = areaUnit,
                location = location,
                soilOrLandType = soilOrLandType,
                notes = notes
            )
        }
    }

    fun syncAgriCrop(
        cropDocId: String,
        farmId: String?,
        landId: String,
        cropType: String,
        title: String,
        variety: String,
        season: String,
        status: String,
        yieldQuantity: Double,
        yieldUnit: String,
        marketPricePerUnit: Double,
        yieldMarketWorth: Double,
        soldAmount: Double,
        notes: String
    ) {
        viewModelScope.launch {
            repository.upsertAgriCrop(
                cropDocId = cropDocId,
                farmId = farmId,
                landId = landId,
                cropType = cropType,
                title = title,
                variety = variety,
                season = season,
                status = status,
                yieldQuantity = yieldQuantity,
                yieldUnit = yieldUnit,
                marketPricePerUnit = marketPricePerUnit,
                yieldMarketWorth = yieldMarketWorth,
                soldAmount = soldAmount,
                notes = notes
            )
        }
    }

    fun syncAgriExpense(
        expenseDocId: String,
        farmId: String?,
        landId: String,
        cropId: String?,
        sector: String,
        customSectorName: String,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            repository.upsertAgriExpense(
                expenseDocId = expenseDocId,
                farmId = farmId,
                landId = landId,
                cropId = cropId,
                sector = sector,
                customSectorName = customSectorName,
                amount = amount,
                description = description
            )
        }
    }

    fun syncProductAd(
        adDocId: String,
        productDocId: String?,
        productName: String,
        farmName: String,
        priceText: String,
        hookHeadlineBn: String,
        promoBadgeBn: String,
        benefitsPipeSeparatedBn: String,
        callToActionBn: String,
        voiceoverScriptBn: String,
        orderStepsPipeSeparatedBn: String,
        accentColorHex: String,
        active: Boolean
    ) {
        viewModelScope.launch {
            repository.upsertProductAd(
                adDocId = adDocId,
                productDocId = productDocId,
                productName = productName,
                farmName = farmName,
                priceText = priceText,
                hookHeadlineBn = hookHeadlineBn,
                promoBadgeBn = promoBadgeBn,
                benefitsPipeSeparatedBn = benefitsPipeSeparatedBn,
                callToActionBn = callToActionBn,
                voiceoverScriptBn = voiceoverScriptBn,
                orderStepsPipeSeparatedBn = orderStepsPipeSeparatedBn,
                accentColorHex = accentColorHex,
                active = active
            )
        }
    }

    companion object {
        private const val TAG = "CloudSyncVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun factory(currentUserId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    return CloudSyncViewModel(CloudSyncRepository(db), currentUserId) as T
                }
            }
    }
}
