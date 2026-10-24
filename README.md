# GetJobLink 📱

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase">
  <img src="https://img.shields.io/badge/AI-Powered-blue?style=for-the-badge" alt="AI Powered">
</p>

<p align="center">
  <b>A mobile-first job discovery platform that helps developers and job seekers discover opportunities and understand how well their profile matches each job.</b>
</p>

---

## 🚀 About GetJobLink

**GetJobLink** is a job discovery Android application designed to make finding relevant opportunities simpler and faster.

Instead of browsing hundreds of job listings manually, users can create their professional profile, upload their resume, and get personalized job-match information based on their skills, education, experience, certifications, and resume.

The application combines:

* 🔎 Job Search
* 🤖 AI-powered Job Description Parsing
* 📄 Resume Management
* 🎯 Personalized Job Matching
* 👤 Professional Profile
* 🔔 Job Notifications
* ⭐ Saved Jobs
* ⚙️ Customizable App Settings
* 🔐 Firebase Google Authentication

---

## ✨ Features

### 🔐 Google Authentication

Users can sign in using their Google account through Firebase Authentication.

There is a single sign-in flow for both users and administrators.

After authentication, the application checks the user's role and routes them accordingly.

```text
Google Sign-In
      ↓
Firebase Authentication
      ↓
Check User Role
      ↓
 ┌─────────────┐
 │             │
User         Admin
 │             │
 ↓             ↓
User App    Admin Dashboard
```

---

## 🏠 Home

The home screen provides a personalized starting point for users.

It includes:

* Job search
* Job categories
* Recommended opportunities
* Recently added jobs
* Profile completion
* Personalized job matches

---

## 💼 Job Search

Search and discover job opportunities using:

* Job title
* Skills
* Keywords
* Location
* Experience
* Job type
* Company
* Other available filters

Each job listing provides important information such as:

* Job title
* Company
* Location
* Experience
* Salary
* Job type
* Required skills
* Job description
* Application link

---

## 🤖 AI-Powered Job Matching

One of the core features of GetJobLink is personalized job matching.

The application compares the user's:

* Resume
* Skills
* Experience
* Education
* Certifications
* Target role

against:

* Job description
* Required skills
* Preferred skills
* Experience requirements
* Education requirements

Example:

```text
Your Match

78%

Skills Match       85%
Experience Match   70%
Education Match   100%
Certification      60%
```

The application also shows matched and missing skills.

### Matched Skills

```text
✓ JavaScript
✓ React.js
✓ Node.js
✓ SQL
```

### Missing Skills

```text
○ AWS
○ Docker
```

The goal is to provide users with more context than simply showing a job listing.

---

## 📄 Resume Management

Users can upload and manage their resume directly from their profile.

Supported functionality includes:

* Upload resume
* Replace resume
* View resume
* Download resume
* Delete/replace resume

The uploaded resume can also be processed for extracting relevant candidate information.

---

## 🧠 AI Resume Parsing

The application can extract structured information from a resume, including:

* Name
* Email
* Phone
* Skills
* Programming languages
* Frameworks
* Databases
* Cloud technologies
* Certifications
* Education
* Experience
* Projects
* Job titles

Extracted information can then be reviewed by the user before being added to their profile.

---

## 📝 AI Job Description Parsing

Administrators can add a job description and use AI to extract structured requirements.

The parser can identify:

* Required skills
* Preferred skills
* Programming languages
* Frameworks
* Databases
* Cloud technologies
* Experience requirements
* Education requirements
* Responsibilities

Example:

```text
Job Description

Looking for a Software Engineer with React.js,
JavaScript, Node.js, SQL and Azure experience.

                ↓

AI Parser

Required Skills:
• React.js
• JavaScript
• Node.js
• SQL
• Azure
```

Administrators can review the extracted information before publishing the job.

---

## 👤 Profile

Users have a dedicated professional profile containing:

### Personal Information

* Name
* Email
* Phone
* Location
* LinkedIn
* GitHub
* Portfolio

### Skills

Users can add and remove technical skills.

### Education

* Degree
* Institution
* Field of study
* Graduation year
* CGPA/Percentage

### Certifications

* Certification name
* Organization
* Issue date
* Credential URL

### Experience

* Company
* Job title
* Duration
* Description
* Technologies

### Resume

Users can upload and manage their resume.

---

## 📊 Profile Completion

The application tracks profile completion.

Example:

```text
Profile Completion

████████████████░░░░ 80%

Complete your profile to improve
your personalized job matches.
```

A more complete profile allows the matching system to use more candidate information.

---

## 🔔 Notifications

Users receive notifications for relevant activity such as:

* New jobs
* Recommended jobs
* Profile reminders
* Job updates
* System notifications

Unread notifications are displayed using a badge.

Example:

```text
🔔 3
```

Users can:

* View notifications
* Mark notifications as read
* Mark all as read

---

## 📱 Mobile Navigation

The application uses a mobile-first navigation system.

### Bottom Navigation

```text
┌────────────────────────────────────┐
│                                    │
│          Current Screen            │
│                                    │
├────────────────────────────────────┤
│ 🏠       💼       🔔        👤     │
│ Home    Jobs   Notifications Profile
└────────────────────────────────────┘
```

### Navigation Drawer

The top-left GetJobLink app icon opens a sidebar containing:

* 🏠 Home
* 💼 Job Search
* 🔔 Notifications
* 👤 Profile
* ⚙️ Settings
* 🚪 Logout

Both navigation systems point to the same application screens.

---

## ⚙️ Settings

Users can customize their application experience.

### Theme

Supported options:

* Light
* Dark
* System Default

### Text Size

Supported options:

* Small
* Medium
* Large
* Extra Large

Preferences are persisted so they remain after reopening the application.

---

## 👀 Guest Experience

Users can browse jobs without signing in.

Guests can:

* Browse jobs
* Search jobs
* View job descriptions
* View required skills
* Explore opportunities

Personalized features require authentication.

For example:

> **Sign in to see your personalized job match.**

Personalized match percentages are only displayed after the user signs in and provides profile information.

---

## 🛠️ Tech Stack

The project is built using modern Android and backend technologies.

### Android

* Kotlin
* Android SDK
* Android Studio
* Material Design

### Backend & Cloud

* Firebase Authentication
* Firebase
* Cloud services/database as required by the application

### AI

* AI-powered resume parsing
* AI-powered Job Description parsing
* AI-powered candidate-job matching

### Development

* Git
* GitHub
* Gradle

---

## 🏗️ Application Architecture

High-level application flow:

```text
                    GetJobLink
                        │
              ┌─────────┴─────────┐
              │                   │
          Authentication       Guest
              │                   │
       Google Firebase         Browse Jobs
              │
        Check User Role
              │
       ┌──────┴──────┐
       │             │
     USER          ADMIN
       │             │
       ▼             ▼
   User App      Admin Panel
       │
 ┌─────┼─────┬──────────┐
 │     │     │          │
Home  Jobs  Notifications Profile
                         │
                 Resume + Skills
                 Education
                 Experience
                 Certifications
                         │
                         ▼
                  AI Job Matching
```

---

## 🔄 Job Matching Flow

```text
User
 │
 ├── Profile
 ├── Resume
 ├── Skills
 ├── Education
 ├── Experience
 └── Certifications
        │
        ▼
   Candidate Data
        │
        │
        ▼
   AI Matching Engine
        ▲
        │
   Job Requirements
        │
 ├── Job Description
 ├── Required Skills
 ├── Preferred Skills
 ├── Experience
 └── Education
        │
        ▼
  Match Percentage
        │
        ▼
  Personalized Job View
```

---

## 🔒 Security

The application uses Firebase Authentication for user authentication.

Authorization should be enforced on the backend rather than relying only on frontend role checks.

Important protected operations include:

* Admin access
* Job creation
* Job editing
* Job deletion
* Job publishing
* User profile modification
* Resume access

Users should only be able to modify their own profile and personal information.

---

## 📂 Project Structure

The project structure may evolve as development continues, but the application is organized around major areas such as:

```text
GetJobLink/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   │
│   └── build.gradle
│
├── gradle/
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

Install:

* Android Studio
* Android SDK
* JDK compatible with the project
* Git

### Clone the Repository

```bash
git clone https://github.com/Divyanshu9794/Get_job_Link_Android-App.git
```

Move into the project:

```bash
cd Get_job_Link_Android-App
```

Open the project in Android Studio.

Allow Gradle to sync and install the required dependencies.

Then run the application on:

* Android Emulator
* Physical Android device

---

## 🔥 Firebase Configuration

The application uses Firebase services.

To run the project locally, configure Firebase for your own development environment.

Typical setup includes:

1. Create a Firebase project.
2. Register the Android application.
3. Add the required Firebase configuration.
4. Enable Google Authentication.
5. Configure the required Firebase services.
6. Apply the appropriate security rules.

Do not commit private credentials, API keys that should remain secret, signing keys, or other sensitive configuration to GitHub.

---

## 🔑 Authentication Flow

```text
Open App
   ↓
Sign In
   ↓
Continue with Google
   ↓
Firebase Authentication
   ↓
Retrieve User
   ↓
Check Role
   ↓
┌───────────────┐
│               │
USER           ADMIN
│               │
▼               ▼
Home          Dashboard
```

---

## 📌 Roadmap

Planned and ongoing improvements include:

* [ ] Advanced job recommendations
* [ ] Improved AI resume parsing
* [ ] Improved JD parsing
* [ ] Advanced candidate-job matching
* [ ] Application tracking
* [ ] Saved jobs synchronization
* [ ] Push notifications
* [ ] More job-source integrations
* [ ] Resume builder
* [ ] ATS score evaluation
* [ ] More personalization options
* [ ] Improved analytics
* [ ] Offline support where appropriate

---

## 🎯 Vision

GetJobLink aims to make job discovery more focused and useful by combining job listings with the candidate's actual professional profile.

Instead of simply asking:

> "What jobs are available?"

GetJobLink also helps answer:

> "How does this job relate to my current profile?"

---

## 🌐 GetJobLink

**Web:**
https://get-job-link.web.app/

**GitHub:**
https://github.com/Divyanshu9794/Get_job_Link_Android-App

---

## 👨‍💻 Developer

**Divyanshu Singh**

Software Developer | Full Stack Developer

* GitHub: https://github.com/Divyanshu9794
* LinkedIn: https://www.linkedin.com/in/divyanshu-singh-3b433119b/

---

## 📄 License

This project is currently under development.

Add an appropriate open-source license before allowing external contributions or redistribution.
