package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    private fun safeDocId(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.ifBlank { "item" }.take(128)
    }

    // --- Real-Time Authenticated Observation Streams ---

    fun observeUserProfile(userId: String): Flow<CloudUserProfile?> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user profile observation refused.")
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    snap.toObject(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeUserFarms(userId: String): Flow<List<CloudFarm>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user farm observation refused.")
        val path = "users/$uid/farms"
        emitAll(
            db.collection("users").document(uid).collection("farms")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudFarm::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observePublicMarketplaceProducts(): Flow<List<CloudMarketplaceProduct>> = flow {
        val uid = requireUserId()
        val path = "marketplace_products"
        emitAll(
            db.collection(path)
                .whereEqualTo("listed", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudMarketplaceProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeMyMarketplaceProducts(userId: String): Flow<List<CloudMarketplaceProduct>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user product observation refused.")
        val path = "marketplace_products"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudMarketplaceProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeCustomerOrders(userId: String): Flow<List<CloudOrder>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user order observation refused.")
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("customerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeFarmerOrders(userId: String): Flow<List<CloudOrder>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user farm order observation refused.")
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("farmOwnerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<CloudNotification>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user notification observation refused.")
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeAgriLands(userId: String): Flow<List<CloudAgriLand>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user agri land observation refused.")
        val path = "users/$uid/agri_lands"
        emitAll(
            db.collection("users").document(uid).collection("agri_lands")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudAgriLand::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeAgriCrops(userId: String): Flow<List<CloudAgriCrop>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user agri crop observation refused.")
        val path = "users/$uid/agri_crops"
        emitAll(
            db.collection("users").document(uid).collection("agri_crops")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudAgriCrop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeAgriExpenses(userId: String): Flow<List<CloudAgriExpense>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user agri expense observation refused.")
        val path = "users/$uid/agri_expenses"
        emitAll(
            db.collection("users").document(uid).collection("agri_expenses")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudAgriExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeActiveProductAds(): Flow<List<CloudProductAd>> = flow {
        val uid = requireUserId()
        val path = "product_ads"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudProductAd::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeMyProductAds(userId: String): Flow<List<CloudProductAd>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw SecurityException("Cross-user product ad observation refused.")
        val path = "product_ads"
        emitAll(
            db.collection(path)
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudProductAd::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    rethrowOrLog(error, OperationType.LIST, path, uid)
                }
        )
    }

    private fun rethrowOrLog(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- Mutations (Fail-Fast Auth, Pre-Validation, Immutable createdAt, Result<T>) ---

    suspend fun upsertUserProfile(
        displayName: String,
        email: String,
        mobile: String = "",
        role: String = "CUSTOMER",
        preferredLanguage: String = "en",
        preferredCurrency: String = "BDT"
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val normalizedRole = if (role == "FARMER") "FARMER" else "CUSTOMER"
            val normalizedLang = if (preferredLanguage == "bn") "bn" else "en"
            val normalizedCurrency = if (preferredCurrency in setOf("BDT", "USD", "EUR", "INR")) {
                preferredCurrency
            } else "BDT"
            val cleanName = displayName.trim().ifBlank { "Smart Farm User" }.take(120)
            val cleanEmail = email.trim().take(180)
            val cleanMobile = mobile.trim().take(40)

            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateMap = mapOf(
                    "displayName" to cleanName,
                    "email" to cleanEmail,
                    "mobile" to cleanMobile,
                    "role" to normalizedRole,
                    "preferredLanguage" to normalizedLang,
                    "preferredCurrency" to normalizedCurrency,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "email" to cleanEmail,
                    "mobile" to cleanMobile,
                    "role" to normalizedRole,
                    "preferredLanguage" to normalizedLang,
                    "preferredCurrency" to normalizedCurrency,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertFarm(
        farmDocId: String,
        farmCode: String,
        name: String,
        location: String,
        address: String,
        category: String,
        managerName: String,
        managerPhone: String,
        initialInvestment: Double
    ): Result<Unit> {
        val safeId = safeDocId(farmDocId)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/farms/$safeId"
        return try {
            val uid = requireUserId()
            val cleanCode = farmCode.trim().ifBlank { "FM-01" }.take(40)
            val cleanName = name.trim().take(120)
            require(cleanName.isNotEmpty()) { "Farm name is required." }
            require(cleanCode.length >= 2) { "Farm code must be at least 2 characters." }
            val safeInvest = initialInvestment.coerceAtLeast(0.0)

            val docRef = db.collection("users").document(uid).collection("farms").document(safeId)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "farmCode" to cleanCode,
                        "name" to cleanName,
                        "location" to location.trim().take(120),
                        "address" to address.trim().take(240),
                        "category" to category.trim().ifBlank { "MIXED" }.take(60),
                        "managerName" to managerName.trim().take(120),
                        "managerPhone" to managerPhone.trim().take(40),
                        "initialInvestment" to safeInvest,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "farmCode" to cleanCode,
                        "name" to cleanName,
                        "location" to location.trim().take(120),
                        "address" to address.trim().take(240),
                        "category" to category.trim().ifBlank { "MIXED" }.take(60),
                        "managerName" to managerName.trim().take(120),
                        "managerPhone" to managerPhone.trim().take(40),
                        "initialInvestment" to safeInvest,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertMarketplaceProduct(
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
    ): Result<Unit> {
        val safeId = safeDocId(productDocId)
        val docPath = "marketplace_products/$safeId"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(140)
            val cleanCategory = category.trim().take(80)
            require(cleanName.isNotEmpty()) { "Product name is required." }
            require(cleanCategory.isNotEmpty()) { "Category is required." }
            require(sellingPrice > 0.0) { "Selling price must be positive." }
            require(quantity >= 0.0) { "Quantity cannot be negative." }

            val normalizedSourcing = if (sourcingType == "PURCHASED_RESALE") "PURCHASED_RESALE" else "FARM_PRODUCED"
            val docRef = db.collection("marketplace_products").document(safeId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "farmId" to safeDocId(farmId),
                        "farmName" to farmName.trim().ifBlank { "Smart Farm" }.take(120),
                        "farmLocation" to farmLocation.trim().take(120),
                        "name" to cleanName,
                        "category" to cleanCategory,
                        "sourcingType" to normalizedSourcing,
                        "quantity" to quantity,
                        "unit" to unit.trim().ifBlank { "kg" }.take(30),
                        "unitCost" to unitCost.coerceAtLeast(0.0),
                        "sellingPrice" to sellingPrice,
                        "listed" to listed,
                        "description" to description.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "ownerUid" to uid,
                        "farmId" to safeDocId(farmId),
                        "farmName" to farmName.trim().ifBlank { "Smart Farm" }.take(120),
                        "farmLocation" to farmLocation.trim().take(120),
                        "name" to cleanName,
                        "category" to cleanCategory,
                        "sourcingType" to normalizedSourcing,
                        "quantity" to quantity,
                        "unit" to unit.trim().ifBlank { "kg" }.take(30),
                        "unitCost" to unitCost.coerceAtLeast(0.0),
                        "sellingPrice" to sellingPrice,
                        "listed" to listed,
                        "description" to description.trim().take(500),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMarketplaceProduct(productDocId: String): Result<Unit> {
        val safeId = safeDocId(productDocId)
        val docPath = "marketplace_products/$safeId"
        return try {
            requireUserId()
            db.collection("marketplace_products").document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun placeOrderWithFarmNotification(
        orderDocId: String,
        orderNumber: String,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        paymentMethod: String,
        product: CloudMarketplaceProduct,
        itemsSummary: String,
        totalAmount: Double
    ): Result<Unit> {
        val safeOrderId = safeDocId(orderDocId)
        val docPath = "orders/$safeOrderId"
        return try {
            val uid = requireUserId()
            require(deliveryAddress.trim().length >= 2) { "Delivery address is required." }
            require(totalAmount > 0.0) { "Order total must be greater than zero." }
            require(product.id.isNotBlank() && product.ownerUid.isNotBlank()) { "Invalid product metadata." }

            val orderRef = db.collection("orders").document(safeOrderId)
            val notifRef = db.collection("users").document(product.ownerUid)
                .collection("notifications").document("ord_notif_$safeOrderId")

            val batch = db.batch()
            batch.set(
                orderRef,
                mapOf(
                    "orderNumber" to orderNumber.trim().take(40),
                    "customerUid" to uid,
                    "customerName" to customerName.trim().ifBlank { "Customer" }.take(120),
                    "customerPhone" to customerPhone.trim().take(40),
                    "deliveryAddress" to deliveryAddress.trim().take(300),
                    "paymentMethod" to paymentMethod.trim().ifBlank { "Cash on Delivery" }.take(60),
                    "farmId" to product.farmId.take(128),
                    "farmName" to product.farmName.ifBlank { "Smart Farm" }.take(120),
                    "farmOwnerUid" to product.ownerUid.take(128),
                    "productId" to product.id.take(128),
                    "itemsSummary" to itemsSummary.trim().ifBlank { product.name }.take(500),
                    "totalAmount" to totalAmount,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "targetUserId" to product.ownerUid,
                    "senderUid" to uid,
                    "orderId" to safeOrderId,
                    "title" to "New Online Order: $orderNumber".take(160),
                    "message" to "$customerName ordered ${itemsSummary.take(160)} for ${product.farmName}.".take(500),
                    "category" to "ORDER",
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderDocId: String, newStatus: String): Result<Unit> {
        val safeOrderId = safeDocId(orderDocId)
        val docPath = "orders/$safeOrderId"
        return try {
            requireUserId()
            val allowed = setOf("CONFIRMED", "DISPATCHED", "DELIVERED", "CANCELLATION_REQUESTED", "CANCELLED")
            require(newStatus in allowed) { "Unsupported order status: $newStatus" }
            db.collection("orders").document(safeOrderId)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationDocId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val safeId = safeDocId(notificationDocId)
        val docPath = "users/${uid ?: "unauthenticated"}/notifications/$safeId"
        return try {
            val activeUid = requireUserId()
            db.collection("users").document(activeUid)
                .collection("notifications").document(safeId)
                .update(
                    mapOf(
                        "readFlag" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertAgriLand(
        landDocId: String,
        farmId: String?,
        name: String,
        areaSize: Double,
        areaUnit: String,
        location: String,
        soilOrLandType: String,
        notes: String
    ): Result<Unit> {
        val safeId = safeDocId(landDocId)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/agri_lands/$safeId"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(140)
            require(cleanName.isNotEmpty()) { "Land plot name is required." }
            val safeArea = areaSize.coerceAtLeast(0.0)
            val safeUnit = areaUnit.trim().ifBlank { "Decimal" }.take(30)
            val cleanFarmId = farmId?.takeIf { it.isNotBlank() }?.let { safeDocId(it) }

            val docRef = db.collection("users").document(uid).collection("agri_lands").document(safeId)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateMap = mapOf(
                    "farmId" to cleanFarmId,
                    "name" to cleanName,
                    "areaSize" to safeArea,
                    "areaUnit" to safeUnit,
                    "location" to location.trim().take(160),
                    "soilOrLandType" to soilOrLandType.trim().take(100),
                    "notes" to notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "farmId" to cleanFarmId,
                    "name" to cleanName,
                    "areaSize" to safeArea,
                    "areaUnit" to safeUnit,
                    "location" to location.trim().take(160),
                    "soilOrLandType" to soilOrLandType.trim().take(100),
                    "notes" to notes.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertAgriCrop(
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
    ): Result<Unit> {
        val safeId = safeDocId(cropDocId)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/agri_crops/$safeId"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Crop title is required." }
            val safeLandId = safeDocId(landId)
            val cleanFarmId = farmId?.takeIf { it.isNotBlank() }?.let { safeDocId(it) }
            val normalizedStatus = if (status in setOf("GROWING", "HARVESTED", "SOLD")) status else "GROWING"

            val docRef = db.collection("users").document(uid).collection("agri_crops").document(safeId)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateMap = mapOf(
                    "farmId" to cleanFarmId,
                    "landId" to safeLandId,
                    "cropType" to cropType.trim().ifBlank { "RICE" }.take(60),
                    "title" to cleanTitle,
                    "variety" to variety.trim().take(120),
                    "season" to season.trim().ifBlank { "Boro" }.take(40),
                    "status" to normalizedStatus,
                    "yieldQuantity" to yieldQuantity.coerceAtLeast(0.0),
                    "yieldUnit" to yieldUnit.trim().ifBlank { "mon" }.take(30),
                    "marketPricePerUnit" to marketPricePerUnit.coerceAtLeast(0.0),
                    "yieldMarketWorth" to yieldMarketWorth.coerceAtLeast(0.0),
                    "soldAmount" to soldAmount.coerceAtLeast(0.0),
                    "notes" to notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "farmId" to cleanFarmId,
                    "landId" to safeLandId,
                    "cropType" to cropType.trim().ifBlank { "RICE" }.take(60),
                    "title" to cleanTitle,
                    "variety" to variety.trim().take(120),
                    "season" to season.trim().ifBlank { "Boro" }.take(40),
                    "status" to normalizedStatus,
                    "yieldQuantity" to yieldQuantity.coerceAtLeast(0.0),
                    "yieldUnit" to yieldUnit.trim().ifBlank { "mon" }.take(30),
                    "marketPricePerUnit" to marketPricePerUnit.coerceAtLeast(0.0),
                    "yieldMarketWorth" to yieldMarketWorth.coerceAtLeast(0.0),
                    "soldAmount" to soldAmount.coerceAtLeast(0.0),
                    "notes" to notes.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertAgriExpense(
        expenseDocId: String,
        farmId: String?,
        landId: String,
        cropId: String?,
        sector: String,
        customSectorName: String,
        amount: Double,
        description: String
    ): Result<Unit> {
        val safeId = safeDocId(expenseDocId)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/agri_expenses/$safeId"
        return try {
            val uid = requireUserId()
            require(amount > 0.0) { "Expense amount must be positive." }
            val safeLandId = safeDocId(landId)
            val cleanFarmId = farmId?.takeIf { it.isNotBlank() }?.let { safeDocId(it) }
            val cleanCropId = cropId?.takeIf { it.isNotBlank() }?.let { safeDocId(it) }

            val docRef = db.collection("users").document(uid).collection("agri_expenses").document(safeId)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateMap = mapOf(
                    "farmId" to cleanFarmId,
                    "landId" to safeLandId,
                    "cropId" to cleanCropId,
                    "sector" to sector.trim().ifBlank { "SEEDS" }.take(60),
                    "customSectorName" to customSectorName.trim().take(120),
                    "amount" to amount,
                    "description" to description.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "farmId" to cleanFarmId,
                    "landId" to safeLandId,
                    "cropId" to cleanCropId,
                    "sector" to sector.trim().ifBlank { "SEEDS" }.take(60),
                    "customSectorName" to customSectorName.trim().take(120),
                    "amount" to amount,
                    "description" to description.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertProductAd(
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
    ): Result<Unit> {
        val safeId = safeDocId(adDocId)
        val docPath = "product_ads/$safeId"
        return try {
            val uid = requireUserId()
            val cleanName = productName.trim().take(160)
            val cleanHook = hookHeadlineBn.trim().take(220)
            require(cleanName.isNotEmpty()) { "Product name is required." }
            require(cleanHook.isNotEmpty()) { "Ad headline is required." }
            val cleanProdDocId = productDocId?.takeIf { it.isNotBlank() }?.let { safeDocId(it) }

            val docRef = db.collection("product_ads").document(safeId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateMap = mapOf(
                    "productDocId" to cleanProdDocId,
                    "productName" to cleanName,
                    "farmName" to farmName.trim().take(140),
                    "priceText" to priceText.trim().take(80),
                    "hookHeadlineBn" to cleanHook,
                    "promoBadgeBn" to promoBadgeBn.trim().take(80),
                    "benefitsPipeSeparatedBn" to benefitsPipeSeparatedBn.trim().take(600),
                    "callToActionBn" to callToActionBn.trim().take(160),
                    "voiceoverScriptBn" to voiceoverScriptBn.trim().take(1200),
                    "orderStepsPipeSeparatedBn" to orderStepsPipeSeparatedBn.trim().take(800),
                    "accentColorHex" to accentColorHex.trim().ifBlank { "#1B5E20" }.take(20),
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "ownerUid" to uid,
                    "productDocId" to cleanProdDocId,
                    "productName" to cleanName,
                    "farmName" to farmName.trim().take(140),
                    "priceText" to priceText.trim().take(80),
                    "hookHeadlineBn" to cleanHook,
                    "promoBadgeBn" to promoBadgeBn.trim().take(80),
                    "benefitsPipeSeparatedBn" to benefitsPipeSeparatedBn.trim().take(600),
                    "callToActionBn" to callToActionBn.trim().take(160),
                    "voiceoverScriptBn" to voiceoverScriptBn.trim().take(1200),
                    "orderStepsPipeSeparatedBn" to orderStepsPipeSeparatedBn.trim().take(800),
                    "accentColorHex" to accentColorHex.trim().ifBlank { "#1B5E20" }.take(20),
                    "active" to active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
