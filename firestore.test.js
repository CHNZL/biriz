const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_teacher_123";
const BOB_UID = "bob_teacher_456";

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

test("Unauthenticated access: rejected across protected collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("classrooms").get());
  await assertFails(unauthDb.collection("classrooms").doc("c1").collection("stars").get());
});

test("User profile: Alice can create and read her own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      fullName: "Ayşe Öğretmen",
      email: "ayse@meb.k12.tr",
      role: "TEACHER",
      kvkkAccepted: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(profileRef.get());
});

test("User profile: Alice cannot read Bob's profile or list users", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob Öğretmen",
      email: "bob@meb.k12.tr",
      role: "TEACHER",
      kvkkAccepted: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Classrooms: Alice can register a classroom, authenticated users can read, Bob cannot update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const classroomDoc = aliceDb.collection("classrooms").doc("sivas_merkez_ataturk_4a");

  await assertSucceeds(
    classroomDoc.set({
      cityId: "58",
      cityName: "SİVAS",
      districtName: "MERKEZ",
      schoolName: "ATATÜRK İLKOKULU",
      gradeLevel: 4,
      section: "A",
      teacherId: ALICE_UID,
      teacherName: "Ayşe Öğretmen",
      teacherEmail: "ayse@meb.k12.tr",
      inviteCode: "BRZ-4A-5801",
      studentCount: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("classrooms").doc("sivas_merkez_ataturk_4a").get());
  await assertSucceeds(bobDb.collection("classrooms").get());

  await assertFails(
    bobDb.collection("classrooms").doc("sivas_merkez_ataturk_4a").update({
      studentCount: 5,
      updatedAt: new Date(),
    })
  );
});

test("Stars: Alice (teacher) can award stars in her class, Bob cannot, authenticated users can read", async () => {
  // Setup classroom owned by Alice
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("classrooms").doc("c1").set({
      cityId: "58",
      cityName: "SİVAS",
      districtName: "MERKEZ",
      schoolName: "ATATÜRK İLKOKULU",
      gradeLevel: 4,
      section: "A",
      teacherId: ALICE_UID,
      teacherName: "Ayşe Öğretmen",
      teacherEmail: "ayse@meb.k12.tr",
      inviteCode: "BRZ-4A-5801",
      studentCount: 1,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const starDoc = aliceDb.collection("classrooms").doc("c1").collection("stars").doc("star1");

  await assertSucceeds(
    starDoc.set({
      classId: "c1",
      studentId: "stu1",
      studentName: "Ali Yılmaz",
      studentNumber: "101",
      categoryId: "matematik",
      categoryName: "Matematik",
      starCount: 2,
      note: "Problem çözme başarısı",
      isLuckyBonus: false,
      timestamp: Date.now(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Authenticated user (Bob or parent) can read stars
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("classrooms").doc("c1").collection("stars").doc("star1").get());

  // Bob cannot award stars in Alice's class
  const bobStarDoc = bobDb.collection("classrooms").doc("c1").collection("stars").doc("star2");
  await assertFails(
    bobStarDoc.set({
      classId: "c1",
      studentId: "stu1",
      studentName: "Ali Yılmaz",
      studentNumber: "101",
      categoryId: "turkce",
      categoryName: "Türkçe",
      starCount: 1,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
