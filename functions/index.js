const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Cloud Function triggered when a new job document is created in the `jobs` collection.
 * Sends a Firebase Cloud Messaging (FCM) push notification to registered users.
 */
exports.onJobCreatedSendNotification = onDocumentCreated("jobs/{jobId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    console.log("No data associated with the event");
    return;
  }

  const jobData = snapshot.data();
  const jobId = event.params.jobId;

  const title = jobData.title || jobData.jobTitle || jobData.name || "New Job Posted";
  const company = jobData.company || jobData.companyName || jobData.employer || "";
  const location = jobData.location || jobData.jobLocation || "Remote";
  const salary = jobData.salary || jobData.compensation || jobData.pay || "";

  let body = company ? `${title} at ${company}` : title;
  if (salary) {
    body += ` · ${salary}`;
  } else if (location) {
    body += ` · ${location}`;
  }

  const messagePayload = {
    notification: {
      title: "🚀 New Job Posted",
      body: body,
    },
    data: {
      type: "new_job",
      jobId: jobId,
      title: "🚀 New Job Posted",
      body: body,
      targetUrl: jobData.applicationUrl || jobData.url || ""
    },
    android: {
      notification: {
        channelId: "new_job_alerts",
        sound: "default",
        priority: "high",
        clickAction: "FLUTTER_NOTIFICATION_CLICK"
      }
    }
  };

  try {
    // 1. Send via topic `new_jobs`
    const topicMessage = { ...messagePayload, topic: "new_jobs" };
    await admin.messaging().send(topicMessage);
    console.log(`Successfully sent FCM notification via topic for job ${jobId}`);

    // 2. Send to individual user devices where jobNotificationsEnabled != false
    const usersSnapshot = await admin.firestore().collection("users").get();
    const tokens = [];
    usersSnapshot.forEach((doc) => {
      const userData = doc.data();
      if (userData.jobNotificationsEnabled !== false && Array.isArray(userData.fcmTokens)) {
        tokens.push(...userData.fcmTokens);
      }
    });

    const uniqueTokens = [...new Set(tokens)];
    if (uniqueTokens.length > 0) {
      const multicastMessage = {
        ...messagePayload,
        tokens: uniqueTokens,
      };
      const response = await admin.messaging().sendEachForMulticast(multicastMessage);
      console.log(`Successfully sent multicast FCM notifications: success=${response.successCount}, failure=${response.failureCount}`);

      // Clean up invalid or expired FCM tokens
      if (response.failureCount > 0) {
        const failedTokens = [];
        response.responses.forEach((resp, idx) => {
          if (!resp.success) {
            const errCode = resp.error?.code;
            if (
              errCode === "messaging/invalid-registration-token" ||
              errCode === "messaging/registration-token-not-registered"
            ) {
              failedTokens.push(uniqueTokens[idx]);
            }
          }
        });

        if (failedTokens.length > 0) {
          console.log(`Cleaning up ${failedTokens.length} stale FCM tokens...`);
          for (const token of failedTokens) {
            const matchingUsers = await admin.firestore().collection("users")
              .where("fcmTokens", "array-contains", token).get();
            matchingUsers.forEach((doc) => {
              doc.ref.update({
                fcmTokens: admin.firestore.FieldValue.arrayRemove(token)
              });
            });
          }
        }
      }
    }

    // Store notification record in Firestore for in-app history / alerts
    await admin.firestore().collection("notifications").add({
      type: "new_job",
      jobId: jobId,
      title: "🚀 New Job Posted",
      message: body,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  } catch (error) {
    console.error(`Error sending FCM notification for job ${jobId}:`, error);
  }
});
