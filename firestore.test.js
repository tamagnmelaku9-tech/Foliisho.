const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("unauthenticated read is denied for users", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("users").doc("user1").get());
});

test("authenticated user can create own profile", async () => {
  const authedDb = testEnv.authenticatedContext("user1").firestore();
  await assertSucceeds(
    authedDb.collection("users").doc("user1").set({
      userId: "user1",
      fullName: "Abebe Kebede",
      phoneNumber: "0911223344",
      vehiclePlate: "3-A12345 SD",
      vehicleCategory: "Bajaj",
      fuelPreference: "BENZENE",
      language: "am"
    })
  );
});

test("authenticated user cannot write other user's profile", async () => {
  const authedDb = testEnv.authenticatedContext("user1").firestore();
  await assertFails(
    authedDb.collection("users").doc("user2").set({
      userId: "user2",
      fullName: "Chala Tadesse",
      phoneNumber: "0922334455",
      vehiclePlate: "3-B67890 SD",
      vehicleCategory: "Taxi",
      fuelPreference: "BENZENE",
      language: "am"
    })
  );
});

test("authenticated user can create ticket for themselves", async () => {
  const authedDb = testEnv.authenticatedContext("driver1").firestore();
  await assertSucceeds(
    authedDb.collection("tickets").doc("HW-B101").set({
      ticketId: "HW-B101",
      userId: "driver1",
      stationId: "total_piassa",
      driverName: "Dawit Haile",
      vehiclePlate: "2-C54321 AA",
      fuelType: "BENZENE",
      sequenceNumber: 101,
      status: "WAITING"
    })
  );
});

test("user cannot create ticket with another user's ID", async () => {
  const authedDb = testEnv.authenticatedContext("driver1").firestore();
  await assertFails(
    authedDb.collection("tickets").doc("HW-B102").set({
      ticketId: "HW-B102",
      userId: "driver2",
      stationId: "total_piassa",
      driverName: "Fake Driver",
      vehiclePlate: "2-C54321 AA",
      fuelType: "BENZENE",
      sequenceNumber: 102,
      status: "WAITING"
    })
  );
});
