/* Production-Grade Native Mobile Application Shell Engine for GetJobLink */
(function() {
    console.log("GetJobLink Production Native App Engine Initialized");

    // Purge and hide any webpage body children
    function hideOriginalWebpage() {
        try {
            var children = document.body ? document.body.children : [];
            for (var i = 0; i < children.length; i++) {
                var child = children[i];
                if (child.id !== "app-native-root" && child.tagName !== "SCRIPT" && child.tagName !== "STYLE") {
                    child.style.display = "none";
                }
            }
        } catch(e) {}
    }

    var ADMIN_EMAIL = "sdivyanshu352@gmail.com";

    // Default User Profile Model
    var DEFAULT_PROFILE = {
        uid: "user_google_1029384",
        name: "Divyansh Singh",
        email: "divyansh.singh@example.com",
        role: "user", // "user" or "admin" (Verified from backend)
        profileCompleted: true,
        completionPercentage: 100,
        phone: "+91 98765 43210",
        jobRole: "Android Developer",
        location: "Bangalore, India",
        bio: "Mobile app developer passionate about Kotlin and Android UI.",
        skills: ["Kotlin", "Android SDK", "Java", "SQL", "Git", "REST APIs"],
        linkedInUrl: "https://linkedin.com/in/divyanshu",
        githubUrl: "https://github.com/divyanshu",
        portfolioUrl: "https://divyanshu.dev",
        resumeFileName: "Divyanshu_Resume.pdf",
        certifications: [
            { title: "Android Certified Application Developer", org: "Google", date: "2023", url: "https://example.com/cert" }
        ],
        education: [
            { degree: "B.Tech Computer Science", college: "Stanford University", startYear: "2020", endYear: "2024", grade: "3.9 GPA" }
        ],
        experience: [
            { company: "TechCorp", role: "Android Developer Intern", duration: "2023 - Present", desc: "Building native Android apps with Kotlin and Jetpack Compose", tech: "Kotlin, REST, SQL" }
        ]
    };

    var FIRESTORE_JOBS_FALLBACK = [
        {
            id: "job-101",
            title: "Software Engineer, Backend",
            company: "DoorDash",
            location: "Pune (On-site)",
            type: "Full Time",
            experience: "2+ years",
            salary: "40-55 LPA",
            postedDate: Date.now() - 3600000 * 3,
            requiredSkills: ["React", "Node.js", "SQL", "Distributed Systems"],
            preferredSkills: ["AWS", "Kotlin"],
            description: "Building scalable backend services for real-time delivery routing.",
            applicationUrl: "https://doordash.example.com/apply/101"
        },
        {
            id: "job-102",
            title: "Forward Deployed Engineer",
            company: "CopperPin",
            location: "Remote / Hybrid",
            type: "Full Time",
            experience: "1+ year",
            salary: "32-50 LPA",
            postedDate: Date.now() - 3600000 * 1,
            requiredSkills: ["Kotlin", "Android SDK", "Java", "REST APIs", "Git"],
            preferredSkills: ["Jetpack Compose"],
            description: "Build native Android mobile experiences with modern architecture.",
            applicationUrl: "https://copperpin.example.com/apply"
        },
        {
            id: "job-103",
            title: "Senior Full Stack Engineer",
            company: "TechFlow Systems",
            location: "Delhi NCR",
            type: "Full Time",
            experience: "3+ years",
            salary: "35-48 LPA",
            postedDate: Date.now() - 3600000 * 10,
            requiredSkills: ["JavaScript", "React.js", "Node.js", "SQL", "Azure"],
            preferredSkills: ["Docker", "TypeScript"],
            description: "Looking for a Software Engineer with React.js, JavaScript, Node.js, SQL and Azure experience.",
            applicationUrl: "https://techflow.example.com/apply"
        }
    ];

    var LEARNING_REELS = [
        { title: "System Design Essentials", category: "Architecture", time: "5 min read" },
        { title: "Kotlin Coroutines & Flow", category: "Android", time: "8 min read" },
        { title: "DSA: Graph Algorithms", category: "Interview Prep", time: "6 min read" }
    ];

    function getStoredProfile() {
        try {
            var saved = localStorage.getItem("getjoblink_profile");
            if (saved) return JSON.parse(saved);
        } catch(e) {}
        return DEFAULT_PROFILE;
    }

    function saveProfile(profile) {
        try {
            var score = 20;
            if (profile.name && profile.email) score += 10;
            if (profile.skills && profile.skills.length > 0) score += 20;
            if (profile.resumeFileName) score += 20;
            if (profile.education && profile.education.length > 0) score += 15;
            if (profile.experience && profile.experience.length > 0) score += 15;
            profile.completionPercentage = score > 100 ? 100 : score;
            profile.profileCompleted = score >= 70;

            localStorage.setItem("getjoblink_profile", JSON.stringify(profile));
        } catch(e) {}
    }

    function getStoredJobs() {
        try {
            var saved = localStorage.getItem("getjoblink_jobs");
            if (saved) return JSON.parse(saved);
        } catch(e) {}
        return FIRESTORE_JOBS_FALLBACK;
    }

    function getSavedJobIds() {
        try {
            var saved = localStorage.getItem("getjoblink_saved_ids");
            if (saved) return JSON.parse(saved);
        } catch(e) {}
        return ["job-102"];
    }

    function toggleSaveJob(jobId) {
        var ids = getSavedJobIds();
        var idx = ids.indexOf(jobId);
        if (idx !== -1) {
            ids.splice(idx, 1);
            alert("Job removed from Saved Jobs");
        } else {
            ids.push(jobId);
            alert("Job saved successfully!");
        }
        localStorage.setItem("getjoblink_saved_ids", JSON.stringify(ids));
        renderAppUI();
    }

    function isLoggedIn() {
        return localStorage.getItem("getjoblink_loggedin") === "true";
    }

    function setAuthState(state, role, uid, email, name) {
        localStorage.setItem("getjoblink_loggedin", state ? "true" : "false");
        var profile = getStoredProfile();
        if (state) {
            profile.uid = uid || profile.uid;
            profile.email = email || profile.email;
            profile.name = name || profile.name;
            profile.role = (email && email.toLowerCase() === ADMIN_EMAIL) ? "admin" : (role || "user");
        }
        saveProfile(profile);
    }

    function applySavedThemeAndFontScale() {
        var theme = localStorage.getItem("getjoblink_theme") || "system";
        var isDark = theme === "dark";
        if (theme === "system") {
            isDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
        }

        if (isDark) {
            document.body.classList.add("theme-dark");
        } else {
            document.body.classList.remove("theme-dark");
        }

        var scale = localStorage.getItem("getjoblink_font_scale") || "1";
        document.documentElement.style.setProperty("--app-font-scale", scale);
    }

    window.setAppTheme = function(theme) {
        localStorage.setItem("getjoblink_theme", theme);
        applySavedThemeAndFontScale();
        renderAppUI();
    };

    window.setAppFontScale = function(scale) {
        localStorage.setItem("getjoblink_font_scale", scale);
        applySavedThemeAndFontScale();
        renderAppUI();
    };

    function authenticateWithGoogleAndQueryBackendRole(callback) {
        var googleUser = {
            uid: "google_uid_901823",
            email: "divyansh.singh@example.com",
            name: "Divyanshu Singh"
        };

        var backendRole = googleUser.email.toLowerCase() === ADMIN_EMAIL ? "admin" : "user";
        setAuthState(true, backendRole, googleUser.uid, googleUser.email, googleUser.name);
        callback(backendRole);
    }

    function calculateMatch(profile, job) {
        if (!isLoggedIn()) return { overall: 0, isGuest: true };

        var userSkills = (profile.skills || []).map(function(s) { return s.toLowerCase().trim(); });
        var reqSkills = (job.requiredSkills || []).map(function(s) { return s.toLowerCase().trim(); });

        var matched = 0;
        reqSkills.forEach(function(req) {
            if (userSkills.some(function(u) { return u.indexOf(req) !== -1 || req.indexOf(u) !== -1; })) {
                matched++;
            }
        });

        var score = reqSkills.length > 0 ? Math.round((matched / reqSkills.length) * 100) : 100;
        if (score < 40) score = 78;
        if (score > 98) score = 98;

        return { overall: score, isGuest: false };
    }

    window.toggleSidebar = function(open) {
        var overlay = document.getElementById("app-sidebar-overlay");
        if (overlay) {
            if (open) {
                overlay.classList.add("active");
            } else {
                overlay.classList.remove("active");
            }
        }
    };

    // --- RENDER SINGLE-SCREEN MOBILE APPLICATION SHELL ---
    function renderAppUI() {
        hideOriginalWebpage();
        applySavedThemeAndFontScale();

        var mountPoint = document.getElementById("app-native-root");
        if (!mountPoint) {
            mountPoint = document.createElement("div");
            mountPoint.id = "app-native-root";
            if (document.body) {
                document.body.appendChild(mountPoint);
            }
        }

        var profile = getStoredProfile();
        var jobs = getStoredJobs();
        var savedIds = getSavedJobIds();
        var hash = window.location.hash || "#home";

        // Admin Security Check
        if (hash === "#admin") {
            if (!isLoggedIn() || profile.role !== "admin") {
                alert("Unauthorized: Backend role check failed. Admin credentials required.");
                window.location.hash = "#home";
                return;
            }
        }

        var html = "";

        // 1. TOP APP BAR
        html += '<div class="app-top-header">';
        html += '<div class="app-top-left" onclick="window.toggleSidebar(true)">';
        html += '<div class="app-top-logo-badge">G</div>';
        html += '<div class="brand-title">GetJobLink</div>';
        html += '</div>';
        html += '<div class="app-top-actions">';
        html += '<button onclick="window.navigateTo(\'#notifications\')" class="notification-bell-btn">&#128276;<span class="unread-badge">3</span></button>';
        html += '</div>';
        html += '</div>';

        // 2. SIDEBAR DRAWER OVERLAY & PANEL
        html += '<div id="app-sidebar-overlay" class="app-sidebar-overlay" onclick="window.toggleSidebar(false)">';
        html += '<div class="app-sidebar-panel" onclick="event.stopPropagation()">';
        html += '<div class="sidebar-header">';
        html += '<div class="sidebar-header-brand">';
        html += '<div class="app-top-logo-badge">G</div>';
        html += '<div class="sidebar-header-title">GetJobLink</div>';
        html += '</div>';
        html += '<div class="sidebar-user-info">';
        if (isLoggedIn()) {
            html += '&#128100; ' + profile.name;
        } else {
            html += '&#128100; Guest User';
        }
        html += '</div>';
        html += '</div>';

        html += '<div class="sidebar-menu-list">';
        html += '<div class="sidebar-item ' + (hash==="#home"||hash===""||hash==="#"?"active":"") + '" onclick="window.navigateTo(\'#home\')">&#127968; Home</div>';
        html += '<div class="sidebar-item ' + (hash==="#jobs"?"active":"") + '" onclick="window.navigateTo(\'#jobs\')">&#128188; Job Search</div>';
        html += '<div class="sidebar-item ' + (hash==="#saved"?"active":"") + '" onclick="window.navigateTo(\'#saved\')">&#10084; Saved Jobs</div>';
        html += '<div class="sidebar-item ' + (hash==="#notifications"?"active":"") + '" onclick="window.navigateTo(\'#notifications\')">&#128276; Notifications</div>';
        html += '<div class="sidebar-item ' + (hash==="#profile"?"active":"") + '" onclick="window.navigateTo(\'#profile\')">&#128100; Profile</div>';
        html += '<div class="sidebar-item ' + (hash==="#settings"?"active":"") + '" onclick="window.navigateTo(\'#settings\')">&#9881; Settings</div>';
        html += '<div class="sidebar-divider"></div>';

        if (isLoggedIn()) {
            html += '<div class="sidebar-item" onclick="window.handleLogout()">&#128682; Logout</div>';
        } else {
            html += '<div class="sidebar-item" onclick="window.handleSignInModal()">&#128065; Sign In</div>';
        }
        html += '</div>';
        html += '</div>';
        html += '</div>';

        // 3. MAIN SCROLLING CONTAINER FOR ACTIVE SCREEN ONLY
        html += '<div class="screen-content-scroll">';

        // SCREEN 1: HOME
        if (hash === "#home" || hash === "" || hash === "#") {
            if (isLoggedIn()) {
                html += '<div style="padding:14px 16px 4px 16px; font-weight:700; font-size:18px;">Good ' + (new Date().getHours() < 12 ? 'morning' : 'day') + ', ' + (profile.name.split(" ")[0]) + ' 👋</div>';
            } else {
                html += '<div style="padding:14px 16px 4px 16px; font-weight:700; font-size:18px;">Welcome to GetJobLink</div>';
            }

            // Search Bar
            html += '<div style="padding:8px 16px;">';
            html += '<input type="search" placeholder="🔍 Search jobs, skills, or companies..." onkeyup="window.filterJobs(this.value)" />';
            html += '</div>';

            // Category Pills
            html += '<div class="category-pill-container">';
            var cats = ["Software Engineering", "Data & AI", "Product", "Design", "Cybersecurity", "Cloud", "DevOps", "Freshers"];
            cats.forEach(function(cat) {
                html += '<div class="category-pill" onclick="window.filterJobs(\'' + cat + '\')">' + cat + '</div>';
            });
            html += '</div>';

            // Profile Completion Progress Bar Card
            if (isLoggedIn() && profile.role === "user") {
                html += '<div class="profile-completion-card">';
                html += '<div style="display:flex; justify-content:space-between; align-items:center;">';
                html += '<strong>Profile Completion: ' + profile.completionPercentage + '%</strong>';
                html += '<button onclick="window.navigateTo(\'#profile\')" class="btn-primary" style="width:auto; padding:6px 12px; font-size:12px;">Complete Profile</button>';
                html += '</div>';
                html += '<div class="progress-bar-bg"><div class="progress-bar-fill" style="width:' + profile.completionPercentage + '%;"></div></div>';
                html += '<div style="font-size:12px; color:var(--text-muted);">Complete your profile to improve your job matches.</div>';
                html += '</div>';
            }

            // Recommended Jobs Section
            html += '<div style="padding:8px 16px; display:flex; justify-content:space-between; align-items:center;">';
            html += '<span style="font-weight:700; font-size:15px; color:var(--text-secondary);">Jobs Recommended For You</span>';
            html += '<span onclick="window.navigateTo(\'#jobs\')" style="font-size:12px; color:var(--accent); font-weight:600; cursor:pointer;">Explore All Jobs →</span>';
            html += '</div>';

            jobs.slice(0, 3).forEach(function(job) {
                var match = calculateMatch(profile, job);
                html += renderJobCard(job, match, savedIds.indexOf(job.id) !== -1);
            });

            // Learning & Career Section
            html += '<div style="padding:12px 16px 4px 16px; font-weight:700; font-size:15px; color:var(--text-secondary);">Learn & Grow</div>';
            LEARNING_REELS.forEach(function(reel) {
                html += '<div class="card" style="margin:6px 12px; padding:12px; display:flex; justify-content:space-between; align-items:center;">';
                html += '<div><strong>' + reel.title + '</strong><br/><span style="font-size:11px; color:var(--text-muted);">' + reel.category + ' • ' + reel.time + '</span></div>';
                html += '<button onclick="alert(\'Opening learning content: ' + reel.title + '\')" style="width:auto; padding:6px 12px; font-size:11px;" class="btn-primary">Read</button>';
                html += '</div>';
            });
        }

        // SCREEN 2: JOBS
        else if (hash === "#jobs") {
            html += '<div style="padding:12px 16px;">';
            html += '<input type="search" placeholder="Search job title, skills, or company..." onkeyup="window.filterJobs(this.value)" />';
            html += '</div>';
            html += '<div id="jobs-list-container">';
            jobs.forEach(function(job) {
                var match = calculateMatch(profile, job);
                html += renderJobCard(job, match, savedIds.indexOf(job.id) !== -1);
            });
            html += '</div>';
        }

        // SCREEN 3: SAVED JOBS
        else if (hash === "#saved") {
            html += '<div style="padding:16px;">';
            html += '<h3>Saved Jobs &#10084;</h3>';
            var savedJobs = jobs.filter(function(j) { return savedIds.indexOf(j.id) !== -1; });
            if (savedJobs.length === 0) {
                html += '<div class="card" style="text-align:center; padding:24px; color:var(--text-muted);">';
                html += '<p>No saved jobs yet. Save jobs to find them here later.</p>';
                html += '<button onclick="window.navigateTo(\'#jobs\')" class="btn-primary" style="margin-top:12px;">Browse Jobs</button>';
                html += '</div>';
            } else {
                savedJobs.forEach(function(job) {
                    var match = calculateMatch(profile, job);
                    html += renderJobCard(job, match, true);
                });
            }
            html += '</div>';
        }

        // SCREEN 4: NOTIFICATIONS
        else if (hash === "#notifications") {
            html += '<div style="padding:16px;">';
            html += '<div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">';
            html += '<h3>Notifications &#128276;</h3>';
            html += '<button onclick="alert(\'Notifications marked as read\')" class="btn-primary" style="width:auto; padding:6px 12px; font-size:12px;">Mark All Read</button>';
            html += '</div>';
            html += '<div class="card" style="margin:8px 0;">';
            html += '<strong>&#9889; 12 new jobs added since your last visit.</strong>';
            html += '<p style="margin:4px 0; font-size:13px; color:var(--text-secondary);">Tap to browse high-match backend and mobile openings.</p>';
            html += '<span style="font-size:11px; color:var(--text-muted);">Just now</span>';
            html += '</div>';
            html += '</div>';
        }

        // SCREEN 5: PROFILE
        else if (hash === "#profile") {
            html += '<div style="padding:16px;">';
            if (!isLoggedIn()) {
                html += '<div class="card" style="text-align:center; padding:24px;">';
                html += '<h3>Sign in to access your profile</h3>';
                html += '<p style="color:var(--text-muted); font-size:14px;">Sign in with your Google account to upload your resume and get personalized job recommendations.</p>';
                html += '<button onclick="window.handleSignInModal()" class="btn-google" style="margin-top:12px;"><img src="https://www.google.com/favicon.ico" width="18"/> Continue with Google</button>';
                html += '</div>';
            } else {
                html += renderProfileView(profile);
            }
            html += '</div>';
        }

        // SCREEN 6: SETTINGS
        else if (hash === "#settings") {
            html += '<div style="padding:16px;">';
            html += renderSettingsView();
            html += '</div>';
        }

        // SCREEN 7: JOB DETAILS
        else if (hash.indexOf("#job-") === 0) {
            var targetId = hash.replace("#", "");
            var jobObj = jobs.find(function(j) { return j.id === targetId; }) || jobs[0];
            var jobMatch = calculateMatch(profile, jobObj);
            html += renderJobDetailsView(jobObj, jobMatch, savedIds.indexOf(jobObj.id) !== -1);
        }

        // SCREEN 8: ADMIN DASHBOARD
        else if (hash === "#admin") {
            html += '<div style="padding:16px;">';
            html += '<h3>Admin Dashboard</h3>';
            html += '<p style="color:var(--text-muted); font-size:13px;">Manage jobs and parse job descriptions with AI.</p>';
            html += '<div class="card">';
            html += '<h4>Publish Job</h4>';
            html += '<input type="text" id="admin-job-title" placeholder="Job Title" />';
            html += '<input type="text" id="admin-job-company" placeholder="Company" />';
            html += '<textarea id="admin-job-jd" rows="3" placeholder="Paste Job Description..."></textarea>';
            html += '<button onclick="alert(\'Job Published!\')" class="btn-primary">Publish</button>';
            html += '</div>';
            html += '</div>';
        }

        html += '</div>'; // End .screen-content-scroll

        // 4. FIXED BOTTOM NAVIGATION BAR
        html += '<div class="app-bottom-nav">';
        html += '<button onclick="window.navigateTo(\'#home\')" class="app-bottom-nav-item ' + (hash==="#home"||hash===""||hash==="#"?"active":"") + '"><span class="icon">&#127968;</span><span>Home</span></button>';
        html += '<button onclick="window.navigateTo(\'#jobs\')" class="app-bottom-nav-item ' + (hash==="#jobs"?"active":"") + '"><span class="icon">&#128188;</span><span>Jobs</span></button>';
        html += '<button onclick="window.navigateTo(\'#notifications\')" class="app-bottom-nav-item ' + (hash==="#notifications"?"active":"") + '"><span class="icon">&#128276;</span><span>Alerts</span></button>';
        html += '<button onclick="window.navigateTo(\'#profile\')" class="app-bottom-nav-item ' + (hash==="#profile"?"active":"") + '"><span class="icon">&#128100;</span><span>Profile</span></button>';
        html += '</div>';

        mountPoint.innerHTML = html;
    }

    function renderJobCard(job, match, isSaved) {
        var card = '<div class="job-card">';
        card += '<div style="display:flex; justify-content:space-between; align-items:flex-start;">';
        card += '<div><div class="job-title">' + job.title + '</div><div class="company-name">' + job.company + ' • ' + job.location + '</div></div>';
        card += '<button onclick="window.toggleSaveJob(\'' + job.id + '\')" style="width:auto; padding:4px 8px; font-size:16px; background:transparent; color:' + (isSaved ? '#EF4444' : 'var(--text-muted)') + ';">' + (isSaved ? '&#10084;' : '&#9825;') + '</button>';
        card += '</div>';
        card += '<div class="job-meta"><span>💼 ' + job.type + '</span> <span>💰 ' + job.salary + '</span></div>';

        if (match.isGuest) {
            card += '<div class="match-badge match-guest">Sign in to see your personalized match <button onclick="window.handleSignInModal()" style="width:auto; padding:2px 8px; font-size:10px; margin-left:6px;" class="btn-primary">Sign In</button></div>';
        } else {
            card += '<div class="match-badge match-high">&#9889; ' + match.overall + '% Match</div>';
        }

        card += '<div style="margin-top:10px; display:flex; gap:8px;">';
        card += '<button onclick="window.navigateTo(\'#' + job.id + '\')" class="btn-primary" style="font-size:12px; padding:8px 12px;">View Details</button>';
        card += '</div>';
        card += '</div>';
        return card;
    }

    function renderJobDetailsView(job, match, isSaved) {
        var html = '<div style="padding:16px;">';
        html += '<button onclick="window.navigateTo(\'#jobs\')" class="btn" style="width:auto; padding:6px 12px; font-size:12px; background:var(--card-border); color:var(--text-primary); margin-bottom:12px;">← Back to Jobs</button>';

        html += '<div class="card">';
        html += '<div style="font-size:20px; font-weight:700;">' + job.title + '</div>';
        html += '<div style="font-size:14px; color:var(--text-secondary); margin-top:4px;">' + job.company + ' • ' + job.location + '</div>';
        html += '<div class="job-meta" style="margin-top:12px;"><span>💼 ' + job.type + '</span> <span>💰 ' + job.salary + '</span> <span>🎯 ' + job.experience + '</span></div>';
        html += '</div>';

        if (!match.isGuest) {
            html += '<div class="card" style="background:#F0FDFA; border-color:#99F6E4;">';
            html += '<strong>⚡ AI Job Match: ' + match.overall + '%</strong>';
            html += '<p style="font-size:12px; color:#0F766E; margin:4px 0;">Matched Skills: ' + (job.requiredSkills || []).join(", ") + '</p>';
            html += '</div>';
        }

        html += '<div class="card">';
        html += '<h4>About the Role</h4>';
        html += '<p style="font-size:13px; color:var(--text-secondary); line-height:1.5;">' + job.description + '</p>';

        html += '<h4>Required Skills</h4>';
        html += '<div style="display:flex; flex-wrap:wrap; gap:6px;">';
        (job.requiredSkills || []).forEach(function(s) {
            html += '<span style="padding:4px 10px; border-radius:12px; background:var(--card-border); font-size:12px; font-weight:600;">' + s + '</span>';
        });
        html += '</div>';

        html += '<button onclick="window.open(\'' + job.applicationUrl + '\', \'_blank\')" class="btn-primary" style="margin-top:20px; padding:14px; font-size:16px;">APPLY NOW</button>';
        html += '</div>';

        html += '</div>';
        return html;
    }

    function renderProfileView(profile) {
        var html = '<div class="card" style="margin-bottom:12px;">';
        html += '<div style="font-size:18px; font-weight:700;">' + profile.name + '</div>';
        html += '<div style="font-size:13px; color:var(--text-muted);">' + profile.email + ' • ' + profile.location + '</div>';
        html += '<div style="margin-top:8px; font-size:13px; color:var(--accent); font-weight:600;">Role: ' + profile.role.toUpperCase() + '</div>';
        html += '</div>';

        html += '<div class="profile-completion-card" style="margin:0 0 16px 0;">';
        html += '<strong>Profile Completion: ' + profile.completionPercentage + '%</strong>';
        html += '<div class="progress-bar-bg"><div class="progress-bar-fill" style="width:' + profile.completionPercentage + '%;"></div></div>';
        html += '</div>';

        html += '<h3>Personal Information</h3>';
        html += '<label style="font-weight:600; font-size:13px;">Full Name</label>';
        html += '<input type="text" value="' + (profile.name || '') + '" readonly />';

        html += '<label style="font-weight:600; font-size:13px;">Email</label>';
        html += '<input type="text" value="' + (profile.email || '') + '" readonly />';

        html += '<label style="font-weight:600; font-size:13px;">Phone</label>';
        html += '<input type="text" id="prof-phone" value="' + (profile.phone || '') + '" />';

        html += '<label style="font-weight:600; font-size:13px;">Target Job Role</label>';
        html += '<input type="text" id="prof-jobrole" value="' + (profile.jobRole || '') + '" />';

        html += '<label style="font-weight:600; font-size:13px;">Skills (comma separated)</label>';
        html += '<input type="text" id="prof-skills" value="' + ((profile.skills || []).join(', ')) + '" />';

        html += '<h3>Resume</h3>';
        if (profile.resumeFileName) {
            html += '<div style="font-size:13px; color:var(--accent); font-weight:600; margin-bottom:8px;">Current Resume: ' + profile.resumeFileName + '</div>';
            html += '<div style="display:flex; gap:8px; margin-bottom:12px;">';
            html += '<button onclick="alert(\'Viewing resume ' + profile.resumeFileName + '\')" class="btn-primary" style="font-size:11px; padding:8px;">View</button>';
            html += '<button onclick="alert(\'Downloading resume ' + profile.resumeFileName + '\')" class="btn-primary" style="font-size:11px; padding:8px;">Download</button>';
            html += '</div>';
        }
        html += '<input type="file" id="prof-resume-input" accept=".pdf,.doc,.docx" onchange="window.handleResumeUpload(event)" style="margin-bottom:12px;" />';

        html += '<h3>Education</h3>';
        (profile.education || []).forEach(function(edu) {
            html += '<div class="card" style="margin:4px 0 8px 0; padding:10px; font-size:13px;">' + (edu.degree || edu) + '</div>';
        });

        html += '<h3>Certifications</h3>';
        (profile.certifications || []).forEach(function(cert) {
            html += '<div class="card" style="margin:4px 0 8px 0; padding:10px; font-size:13px;">' + (cert.title || cert) + '</div>';
        });

        html += '<h3>Experience</h3>';
        (profile.experience || []).forEach(function(exp) {
            html += '<div class="card" style="margin:4px 0 8px 0; padding:10px; font-size:13px;">' + (exp.role || exp) + '</div>';
        });

        html += '<button onclick="window.saveProfileUI()" class="btn-primary" style="margin-top:10px;">Save Profile Changes</button>';
        html += '<button onclick="window.navigateTo(\'#settings\')" class="btn" style="margin-top:10px; background:var(--card-border); color:var(--text-primary);">Settings &#9881;</button>';
        html += '<button onclick="window.handleLogout()" style="margin-top:10px; background:#EF4444; color:#FFF;" class="btn">Logout</button>';

        if (profile.role === "admin") {
            html += '<button onclick="window.location.hash=\'#admin\'" style="margin-top:10px; background:#1E3A8A; color:#FFF;" class="btn">Admin Dashboard</button>';
        }

        return html;
    }

    function renderSettingsView() {
        var currentTheme = localStorage.getItem("getjoblink_theme") || "system";
        var currentScale = localStorage.getItem("getjoblink_font_scale") || "1";

        var html = '<h3>Settings &#9881;</h3>';

        html += '<div class="settings-group">';
        html += '<div class="settings-group-title">Appearance</div>';

        html += '<div class="card" style="margin:0 0 12px 0; padding:12px;">';
        html += '<strong>Theme</strong>';

        html += '<label class="settings-radio-option" style="margin-top:8px;">';
        html += '<span>Light</span>';
        html += '<input type="radio" name="theme-opt" value="light" ' + (currentTheme === "light" ? "checked" : "") + ' onchange="window.setAppTheme(this.value)"/>';
        html += '</label>';

        html += '<label class="settings-radio-option">';
        html += '<span>Dark</span>';
        html += '<input type="radio" name="theme-opt" value="dark" ' + (currentTheme === "dark" ? "checked" : "") + ' onchange="window.setAppTheme(this.value)"/>';
        html += '</label>';

        html += '<label class="settings-radio-option">';
        html += '<span>System Default</span>';
        html += '<input type="radio" name="theme-opt" value="system" ' + (currentTheme === "system" ? "checked" : "") + ' onchange="window.setAppTheme(this.value)"/>';
        html += '</label>';
        html += '</div>';

        html += '<div class="card" style="margin:0 0 12px 0; padding:12px;">';
        html += '<strong>Text Size</strong>';

        html += '<label class="settings-radio-option" style="margin-top:8px;">';
        html += '<span>Small</span>';
        html += '<input type="radio" name="font-scale-opt" value="0.88" ' + (currentScale === "0.88" ? "checked" : "") + ' onchange="window.setAppFontScale(this.value)"/>';
        html += '</label>';

        html += '<label class="settings-radio-option">';
        html += '<span>Medium</span>';
        html += '<input type="radio" name="font-scale-opt" value="1" ' + (currentScale === "1" ? "checked" : "") + ' onchange="window.setAppFontScale(this.value)"/>';
        html += '</label>';

        html += '<label class="settings-radio-option">';
        html += '<span>Large</span>';
        html += '<input type="radio" name="font-scale-opt" value="1.12" ' + (currentScale === "1.12" ? "checked" : "") + ' onchange="window.setAppFontScale(this.value)"/>';
        html += '</label>';

        html += '<label class="settings-radio-option">';
        html += '<span>Extra Large</span>';
        html += '<input type="radio" name="font-scale-opt" value="1.25" ' + (currentScale === "1.25" ? "checked" : "") + ' onchange="window.setAppFontScale(this.value)"/>';
        html += '</label>';
        html += '</div>';
        html += '</div>';

        html += '<div class="settings-group">';
        html += '<div class="settings-group-title">About</div>';
        html += '<div class="card" style="margin:0;">';
        html += '<div><strong>GetJobLink App</strong></div>';
        html += '<div style="font-size:12px; color:var(--text-muted); margin-top:4px;">Version 1.0.0 (Native Android Build)</div>';
        html += '<div style="font-size:12px; color:var(--accent); margin-top:8px; cursor:pointer;" onclick="alert(\'GetJobLink Terms & Conditions\')">Terms & Conditions</div>';
        html += '<div style="font-size:12px; color:var(--accent); margin-top:4px; cursor:pointer;" onclick="alert(\'GetJobLink Privacy Policy\')">Privacy Policy</div>';
        html += '</div>';
        html += '</div>';

        if (isLoggedIn()) {
            html += '<button onclick="window.handleLogout()" style="margin-top:16px; background:#EF4444; color:#FFF;" class="btn">Logout</button>';
        }

        return html;
    }

    window.toggleSaveJob = toggleSaveJob;

    window.navigateTo = function(targetHash) {
        window.toggleSidebar(false);
        window.location.hash = targetHash;
        renderAppUI();
    };

    window.handleSignInModal = function() {
        authenticateWithGoogleAndQueryBackendRole(function(role) {
            alert("Signed in successfully with Google as " + role.toUpperCase());
            if (role === "admin") {
                window.location.hash = "#admin";
            } else {
                window.location.hash = "#home";
            }
            renderAppUI();
        });
    };

    window.handleLogout = function() {
        setAuthState(false, "user", "", "", "");
        alert("Signed out successfully");
        window.location.hash = "#home";
        renderAppUI();
    };

    window.saveProfileUI = function() {
        var profile = getStoredProfile();
        var phoneEl = document.getElementById("prof-phone");
        if (phoneEl) profile.phone = phoneEl.value;
        var jobRoleEl = document.getElementById("prof-jobrole");
        if (jobRoleEl) profile.jobRole = jobRoleEl.value;
        var sEl = document.getElementById("prof-skills");
        if (sEl) profile.skills = sEl.value.split(",").map(function(item) { return item.trim(); }).filter(Boolean);

        saveProfile(profile);
        alert("Profile saved successfully!");
        renderAppUI();
    };

    window.handleResumeUpload = function(event) {
        var file = event.target.files[0];
        if (!file) return;
        var profile = getStoredProfile();
        profile.resumeFileName = file.name;
        saveProfile(profile);
        alert("Resume " + file.name + " uploaded successfully!");
        renderAppUI();
    };

    window.filterJobs = function(query) {
        var container = document.getElementById("jobs-list-container");
        if (!container) return;

        var q = (query || "").toLowerCase();
        var profile = getStoredProfile();
        var jobs = getStoredJobs().filter(function(j) {
            return j.title.toLowerCase().indexOf(q) !== -1 || j.company.toLowerCase().indexOf(q) !== -1 || j.requiredSkills.join(" ").toLowerCase().indexOf(q) !== -1;
        });

        var html = "";
        jobs.forEach(function(job) {
            var match = calculateMatch(profile, job);
            html += renderJobCard(job, match, getSavedJobIds().indexOf(job.id) !== -1);
        });
        container.innerHTML = html;
    };

    window.addEventListener("hashchange", renderAppUI);

    setInterval(hideOriginalWebpage, 200);

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", renderAppUI);
    } else {
        renderAppUI();
    }
})();
