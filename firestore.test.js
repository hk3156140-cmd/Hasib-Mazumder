const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const OWNER_UID = "owner_uid";
const STAFF_UID = "staff_uid";
const CUST_UID = "cust_uid";
const UNPERM_UID = "unperm_uid";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
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

test("Unauthenticated user: cannot read sales or audit logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("sales").get());
  await assertFails(unauthDb.collection("audit_logs").get());
});

test("Public browse: anyone can view categories and active products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("products").get());
  await assertSucceeds(unauthDb.collection("categories").get());
});

test("Owner: full access to products, staff, and sales", async () => {
  // Seed owner in DB
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(OWNER_UID).set({
      userId: OWNER_UID,
      name: "Owner",
      role: "OWNER",
      status: "ACTIVE"
    });
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertSucceeds(
    ownerDb.collection("products").doc("prod_1").set({
      name: "Premium Linen Shirt",
      sku: "ILAM-LS-001",
      sellingPrice: 2450,
      currentStock: 25,
      category: "MEN"
    })
  );
  await assertSucceeds(
    ownerDb.collection("sales").doc("sale_1").set({
      invoiceNumber: "INV-1001",
      total: 2450,
      paidAmount: 2450,
      paymentMethod: "CASH",
      status: "COMPLETED"
    })
  );
});

test("Staff without permission: cannot create sale", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(UNPERM_UID).set({
      userId: UNPERM_UID,
      name: "Rahim",
      role: "STAFF",
      status: "ACTIVE",
      permissions: { posSales: false }
    });
  });

  const staffDb = testEnv.authenticatedContext(UNPERM_UID).firestore();
  await assertFails(
    staffDb.collection("sales").doc("sale_2").set({
      invoiceNumber: "INV-1002",
      total: 1000,
      paidAmount: 1000,
      paymentMethod: "CASH",
      status: "COMPLETED"
    })
  );
});

test("Staff with posSales permission: can create sale", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(STAFF_UID).set({
      userId: STAFF_UID,
      name: "Karim",
      role: "STAFF",
      status: "ACTIVE",
      permissions: { posSales: true }
    });
  });

  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  await assertSucceeds(
    staffDb.collection("sales").doc("sale_3").set({
      invoiceNumber: "INV-1003",
      total: 1500,
      paidAmount: 1500,
      paymentMethod: "CASH",
      status: "COMPLETED"
    })
  );
});

test("Staff cannot elevate their own role or permissions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(STAFF_UID).set({
      userId: STAFF_UID,
      name: "Karim",
      role: "STAFF",
      status: "ACTIVE",
      permissions: { posSales: true }
    });
  });

  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  // Attempt to elevate self to OWNER
  await assertFails(
    staffDb.collection("users").doc(STAFF_UID).update({
      role: "OWNER"
    })
  );
  // Attempt to add permissions
  await assertFails(
    staffDb.collection("users").doc(STAFF_UID).update({
      permissions: { posSales: true, viewProfitReports: true }
    })
  );
});
