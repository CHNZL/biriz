const {
  initializeTestEnvironment,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_teacher_123";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Public access allowed: unauthenticated read and write succeed for smart board", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(
    unauthDb.collection("board_sessions").doc("BRZ94K7P").set({
      sessionCode: "BRZ94K7P",
      status: "CONNECTED",
      isConnected: true,
      updatedAt: Date.now(),
    })
  );
  await assertSucceeds(unauthDb.collection("board_sessions").doc("BRZ94K7P").get());
  await assertSucceeds(unauthDb.collection("classrooms").get());
});

test("Authenticated teacher access: write and read succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("classrooms").doc("c1").set({
      schoolName: "ATATÜRK İLKOKULU",
      gradeLevel: 4,
      section: "A",
      teacherId: ALICE_UID,
    })
  );
  await assertSucceeds(aliceDb.collection("classrooms").doc("c1").get());
});
