const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const FARMER_UID = "farmer_alice_123";
const CUSTOMER_UID = "customer_bob_456";
const OTHER_UID = "other_eve_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastTime();

  await assertFails(unauthDb.collection("users").doc(FARMER_UID).get());
  await assertFails(unauthDb.collection("marketplace_products").where("listed", "==", true).get());
  await assertFails(unauthDb.collection("orders").where("customerUid", "==", CUSTOMER_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Anon",
      email: "a@b.com",
      role: "CUSTOMER",
      preferredLanguage: "en",
      preferredCurrency: "BDT",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("User profile: owner create, partial update, cross-user denial, and anti-escalation", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID, { email_verified: true, email: "alice@example.com" }).firestore();
  const bobDb = testEnv.authenticatedContext(CUSTOMER_UID, { email_verified: true, email: "bob@example.com" }).firestore();
  const created = pastTime(10000);

  // Valid create matching CloudSyncRepository.upsertUserProfile
  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).set({
      userId: FARMER_UID,
      displayName: "Alice Farm",
      email: "alice@example.com",
      mobile: "01711111111",
      role: "FARMER",
      preferredLanguage: "bn",
      preferredCurrency: "BDT",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject self-assigned ADMIN role
  await assertFails(
    bobDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Eve",
      email: "bob@example.com",
      mobile: "018",
      role: "SUPER_ADMIN",
      preferredLanguage: "en",
      preferredCurrency: "BDT",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update
  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).update({
      displayName: "Alice Organic Farm",
      updatedAt: pastTime(1000),
    })
  );

  // Reject future timestamp
  await assertFails(
    farmerDb.collection("users").doc(FARMER_UID).update({
      displayName: "Tampered Time",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user PII read denied
  await assertFails(bobDb.collection("users").doc(FARMER_UID).get());
  await assertFails(bobDb.collection("users").doc(FARMER_UID).update({ displayName: "Hijacked", updatedAt: pastTime(500) }));
});

test("Farms subcollection: owner CRUD and cross-user isolation", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const bobDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const created = pastTime(10000);

  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).collection("farms").doc("farm_1").set({
      userId: FARMER_UID,
      farmCode: "FM-1001",
      name: "Padma Agro",
      location: "Bogura",
      address: "Plot 4",
      category: "MIXED",
      managerName: "Alice",
      managerPhone: "01711",
      initialInvestment: 50000,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(farmerDb.collection("users").doc(FARMER_UID).collection("farms").get());
  await assertFails(bobDb.collection("users").doc(FARMER_UID).collection("farms").get());
  await assertFails(
    bobDb.collection("users").doc(FARMER_UID).collection("farms").doc("farm_2").set({
      userId: CUSTOMER_UID,
      farmCode: "FM-999",
      name: "Injected",
      location: "Dhaka",
      address: "X",
      category: "MIXED",
      managerName: "Bob",
      managerPhone: "018",
      initialInvestment: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Marketplace products: listed query vs unfiltered denial, owner manage, shadow update blocked", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const created = pastTime(10000);

  await assertSucceeds(
    farmerDb.collection("marketplace_products").doc("prod_rice_1").set({
      ownerUid: FARMER_UID,
      farmId: "farm_1",
      farmName: "Padma Agro",
      farmLocation: "Dinajpur",
      name: "Miniket Rice 5kg",
      category: "Rice",
      sourcingType: "FARM_PRODUCED",
      quantity: 100,
      unit: "kg",
      unitCost: 60,
      sellingPrice: 80,
      listed: true,
      description: "Fresh seasonal harvest",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject doc containing forbidden `@DocumentId` "id" field
  await assertFails(
    farmerDb.collection("marketplace_products").doc("prod_bad").set({
      id: "prod_bad",
      ownerUid: FARMER_UID,
      farmId: "farm_1",
      farmName: "Padma Agro",
      farmLocation: "Dinajpur",
      name: "Bad",
      category: "Wheat",
      sourcingType: "FARM_PRODUCED",
      quantity: 10,
      unit: "kg",
      unitCost: 30,
      sellingPrice: 50,
      listed: true,
      description: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer filtered query succeeds; unfiltered query fails
  await assertSucceeds(customerDb.collection("marketplace_products").where("listed", "==", true).get());
  await assertFails(customerDb.collection("marketplace_products").get());

  // Farmer own-products query succeeds
  await assertSucceeds(farmerDb.collection("marketplace_products").where("ownerUid", "==", FARMER_UID).get());

  // Shadow update with ghost field rejected
  await assertFails(
    farmerDb.collection("marketplace_products").doc("prod_rice_1").update({
      sellingPrice: 85,
      isFeaturedHack: true,
      updatedAt: pastTime(1000),
    })
  );

  // Non-owner cannot edit or delete
  await assertFails(
    customerDb.collection("marketplace_products").doc("prod_rice_1").update({
      sellingPrice: 1,
      updatedAt: pastTime(1000),
    })
  );
  await assertFails(customerDb.collection("marketplace_products").doc("prod_rice_1").delete());
});

test("Orders and atomic farm notifications: end-to-end isolation and terminal state lock", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const eveDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const created = pastTime(10000);

  await assertSucceeds(
    farmerDb.collection("marketplace_products").doc("prod_mustard_1").set({
      ownerUid: FARMER_UID,
      farmId: "farm_1",
      farmName: "Padma Agro",
      farmLocation: "Rajshahi",
      name: "Pure Mustard Oil",
      category: "Mustard",
      sourcingType: "PURCHASED_RESALE",
      quantity: 30,
      unit: "liter",
      unitCost: 180,
      sellingPrice: 240,
      listed: true,
      description: "Cold pressed",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Atomic batch creating order + exact farm owner notification
  const orderId = "order_101";
  const batch = customerDb.batch();
  batch.set(customerDb.collection("orders").doc(orderId), {
    orderNumber: "ORD-101",
    customerUid: CUSTOMER_UID,
    customerName: "Bob Buyer",
    customerPhone: "01811111111",
    deliveryAddress: "Uttara Sector 7, Dhaka",
    paymentMethod: "bKash",
    farmId: "farm_1",
    farmName: "Padma Agro",
    farmOwnerUid: FARMER_UID,
    productId: "prod_mustard_1",
    itemsSummary: "2x Pure Mustard Oil",
    totalAmount: 480,
    status: "PENDING",
    createdAt: created,
    updatedAt: created,
  });
  batch.set(
    customerDb.collection("users").doc(FARMER_UID).collection("notifications").doc("ord_notif_order_101"),
    {
      targetUserId: FARMER_UID,
      senderUid: CUSTOMER_UID,
      orderId: orderId,
      title: "New Online Order: ORD-101",
      message: "Bob Buyer ordered 2x Pure Mustard Oil for Padma Agro.",
      category: "ORDER",
      readFlag: false,
      createdAt: created,
      updatedAt: created,
    }
  );
  await assertSucceeds(batch.commit());

  // Farmer and customer can each query their own orders; third party Eve cannot
  await assertSucceeds(customerDb.collection("orders").where("customerUid", "==", CUSTOMER_UID).get());
  await assertSucceeds(farmerDb.collection("orders").where("farmOwnerUid", "==", FARMER_UID).get());
  await assertFails(eveDb.collection("orders").doc(orderId).get());
  await assertFails(eveDb.collection("orders").get());

  // Customer CANNOT read farmer's notifications!
  await assertFails(customerDb.collection("users").doc(FARMER_UID).collection("notifications").get());
  await assertSucceeds(farmerDb.collection("users").doc(FARMER_UID).collection("notifications").get());

  // Customer cannot send spoofed notification without matching order in batch
  await assertFails(
    customerDb.collection("users").doc(FARMER_UID).collection("notifications").doc("fake_notif").set({
      targetUserId: FARMER_UID,
      senderUid: CUSTOMER_UID,
      orderId: "nonexistent_order",
      title: "Fake",
      message: "Fake",
      category: "ORDER",
      readFlag: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Farmer marks order DELIVERED -> terminal state locks further non-admin mutations
  await assertSucceeds(
    farmerDb.collection("orders").doc(orderId).update({
      status: "DELIVERED",
      updatedAt: pastTime(1000),
    })
  );
  await assertFails(
    farmerDb.collection("orders").doc(orderId).update({
      status: "CANCELLED",
      updatedAt: pastTime(500),
    })
  );
});

test("Admin isolation: blocks unverified admin email spoofing and allows verified admin", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: "kingsiam2221@gmail.com",
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext("real_admin_uid", {
    email: "kingsiam2221@gmail.com",
    email_verified: true,
  }).firestore();
  const created = pastTime(10000);

  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).set({
      userId: FARMER_UID,
      displayName: "Alice",
      email: "alice@example.com",
      mobile: "017",
      role: "FARMER",
      preferredLanguage: "en",
      preferredCurrency: "BDT",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Unverified spoofed email MUST be denied
  await assertFails(spoofedAdminDb.collection("users").doc(FARMER_UID).get());
  // Verified developer admin email succeeds
  await assertSucceeds(realAdminDb.collection("users").doc(FARMER_UID).get());
});

test("Agriculture ledger subcollections: owner CRUD, schema validation, and cross-user isolation", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const bobDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const created = pastTime(10000);

  // Land Plot CRUD
  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).collection("agri_lands").doc("land_1").set({
      userId: FARMER_UID,
      farmId: "farm_1",
      name: "North Paddy Field",
      areaSize: 120,
      areaUnit: "Decimal",
      location: "Bogura East",
      soilOrLandType: "Loamy",
      notes: "Irrigated plot",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(bobDb.collection("users").doc(FARMER_UID).collection("agri_lands").get());

  // Crop / Cattle Feed Batch CRUD
  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).collection("agri_crops").doc("crop_1").set({
      userId: FARMER_UID,
      farmId: "farm_1",
      landId: "land_1",
      cropType: "RICE",
      title: "BRRI Dhan-28 Rice",
      variety: "BRRI-28",
      season: "Boro",
      status: "HARVESTED",
      yieldQuantity: 45,
      yieldUnit: "mon",
      marketPricePerUnit: 1300,
      yieldMarketWorth: 58500,
      soldAmount: 42000,
      notes: "Good harvest",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject ghost field on crop update
  await assertFails(
    farmerDb.collection("users").doc(FARMER_UID).collection("agri_crops").doc("crop_1").update({
      soldAmount: 50000,
      hackField: 999,
      updatedAt: pastTime(500),
    })
  );

  // Sector Expense CRUD & positive amount enforcement
  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).collection("agri_expenses").doc("agri_exp_1").set({
      userId: FARMER_UID,
      farmId: "farm_1",
      landId: "land_1",
      cropId: "crop_1",
      sector: "FERTILIZER",
      customSectorName: "",
      amount: 3200,
      description: "Urea & DAP bags",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    farmerDb.collection("users").doc(FARMER_UID).collection("agri_expenses").doc("agri_exp_bad").set({
      userId: FARMER_UID,
      landId: "land_1",
      sector: "SEEDS",
      customSectorName: "",
      amount: -50,
      description: "Negative test",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(bobDb.collection("users").doc(FARMER_UID).collection("agri_expenses").get());
});

test("Product ads collection: active public read vs owner management and non-owner denial", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const created = pastTime(10000);

  // Farmer creates active Bengali product ad
  await assertSucceeds(
    farmerDb.collection("product_ads").doc("ad_rice_1").set({
      ownerUid: FARMER_UID,
      productDocId: "prod_rice_1",
      productName: "Kataribhog Rice 5kg",
      farmName: "Padma Agro",
      priceText: "450 BDT",
      hookHeadlineBn: "দিনাজপুরের সেরা সুগন্ধি কাটারিভোগ চাল!",
      promoBadgeBn: "১০০% খাঁটি",
      benefitsPipeSeparatedBn: "নতুন মৌসুমের তাজা চাল|কীটনাশকমুক্ত|সরাসরি খামার থেকে ডেলিভারি",
      callToActionBn: "এখনই অর্ডার করুন • এখানে ক্লিক করুন",
      voiceoverScriptBn: "তাজা ও খাঁটি কাটারিভোগ চাল কিনতে এখনই অর্ডার করুন।",
      orderStepsPipeSeparatedBn: "১. অ্যাপে পণ্য দেখুন|২. এখানে ক্লিক করে কার্টে যোগ করুন|৩. ঠিকানা ও পেমেন্ট দিন|৪. এখনই অর্ডার করুন",
      accentColorHex: "#1B5E20",
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer can query active product ads, but unfiltered list is denied
  await assertSucceeds(customerDb.collection("product_ads").where("active", "==", true).get());
  await assertFails(customerDb.collection("product_ads").get());

  // Farmer can list all own ads and update active flag
  await assertSucceeds(farmerDb.collection("product_ads").where("ownerUid", "==", FARMER_UID).get());
  await assertSucceeds(
    farmerDb.collection("product_ads").doc("ad_rice_1").update({
      active: false,
      updatedAt: pastTime(1000),
    })
  );

  // Customer cannot modify or delete farmer's ad
  await assertFails(
    customerDb.collection("product_ads").doc("ad_rice_1").update({
      active: true,
      updatedAt: pastTime(500),
    })
  );
  await assertFails(customerDb.collection("product_ads").doc("ad_rice_1").delete());
});

