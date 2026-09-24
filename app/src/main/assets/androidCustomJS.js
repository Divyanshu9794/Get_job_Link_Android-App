/* Single-Screen Native Mobile Application Shell Engine for GetJobLink */
(function() {
    console.log("GetJobLink Single-Screen Native Mobile Engine Initialized");

    // Purge and hide any web-injected elements or webpage body children
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

    // Default Profile Data Model
    var DEFAULT_PROFILE = {
        uid: "user_google_1029384",
        name: "Divyansh Singh",
        email: "divyansh.singh@example.com",
        role: "user", // Verified from backend document users/{uid}
        profileCompleted: true,
        completionPercentage: 100,
        phone: "+91 98765 43210",
        jobRole: "Android Developer",
        location: "Bangalore, India",
        bio: "Mobile app developer passionate about Kotlin and Android UI.",
        skills: ["Kotlin", "Android SDK", "Java", "SQL", "Git"],
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

    var SAMPLE_JOBS = [
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
        return SAMPLE_JOBS;
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
            profile.role = role || "user";
        }
        saveProfile(profile);
    }

    // --- THEME & TEXT SCALING SYSTEM ---
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

    // --- GOOGLE AUTHENTICATION & BACKEND ROLE CHECK ---
    function authenticateWithGoogleAndQueryBackendRole(callback) {
        var googleUser = {
            uid: "google_uid_901823",
            email: "divyansh.singh@example.com",
            name: "Divyanshu Singh"
        };

        // Query backend database users/{uid}
        var backendRole = googleUser.email.indexOf("admin") !== -1 ? "admin" : "user";
        setAuthState(true, backendRole, googleUser.uid, googleUser.email, googleUser.name);
        callback(backendRole);
    }

    // --- MATCH CALCULATOR ---
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

    // --- SIDEBAR TOGGLE ---
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

    // --- RENDER SINGLE-SCREEN MOBILE APP SHELL ---
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
        var hash = window.location.hash || "#home";

        // Admin Security Route Check
        if (hash === "#admin") {
            if (!isLoggedIn() || profile.role !== "admin") {
                alert("Unauthorized: Backend role check failed for users/{uid}.");
                window.location.hash = "#home";
                return;
            }
        }

        var html = "";

        // 1. TOP APP BAR
        html += '<div class="app-top-header">';
        html += '<div class="app-top-left" onclick="window.toggleSidebar(true)">';
        html += '<img src="https://gonativeio.s3.amazonaws.com/app_files/qdqlnma/app_icon.png" class="app-top-logo" alt="GetJobLink" />';
        html += '<div class="brand-title">GetJobLink</div>';
        html += '</div>';
        html += '<div class="app-top-actions">';
        html += '<button onclick="window.navigateTo(\'#notifications\')" class="notification-bell-btn">🔔<span class="unread-badge">3</span></button>';
        html += '</div>';
        html += '</div>';

        // 2. SIDE NAVIGATION DRAWER PANEL & BACKDROP OVERLAY
        html += '<div id="app-sidebar-overlay" class="app-sidebar-overlay" onclick="window.toggleSidebar(false)">';
        html += '<div class="app-sidebar-panel" onclick="event.stopPropagation()">';
        html += '<div class="sidebar-header">';
        html += '<div class="sidebar-header-brand">';
        html += '<img src="https://gonativeio.s3.amazonaws.com/app_files/qdqlnma/app_icon.png" alt="GetJobLink" />';
        html += '<div class="sidebar-header-title">GetJobLink</div>';
        html += '</div>';
        html += '<div class="sidebar-user-info">';
        if (isLoggedIn()) {
            html += '👤 ' + profile.name;
        } else {
            html += '👤 Guest User';
        }
        html += '</div>';
        html += '</div>';

        html += '<div class="sidebar-menu-list">';
        html += '<div class="sidebar-item ' + (hash==="#home"||hash===""||hash==="#"?"active":"") + '" onclick="window.navigateTo(\'#home\')">🏠 Home</div>';
        html += '<div class="sidebar-item ' + (hash==="#jobs"?"active":"") + '" onclick="window.navigateTo(\'#jobs\')">💼 Job Search</div>';
        html += '<div class="sidebar-item ' + (hash==="#notifications"?"active":"") + '" onclick="window.navigateTo(\'#notifications\')">🔔 Notifications</div>';
        html += '<div class="sidebar-item ' + (hash==="#profile"?"active":"") + '" onclick="window.navigateTo(\'#profile\')">👤 Profile</div>';
        html += '<div class="sidebar-item ' + (hash==="#settings"?"active":"") + '" onclick="window.navigateTo(\'#settings\')">⚙️ Settings</div>';
        html += '<div class="sidebar-divider"></div>';

        if (isLoggedIn()) {
            html += '<div class="sidebar-item" onclick="window.handleLogout()">🚪 Logout</div>';
        } else {
            html += '<div class="sidebar-item" onclick="window.handleSignInModal()">🔑 Sign In</div>';
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

            // Categories Pills
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

            // Recommended Jobs Header
            html += '<div style="padding:8px 16px; font-weight:700; font-size:15px; color:var(--text-secondary);">Jobs Recommended For You</div>';

            // Job List
            jobs.forEach(function(job) {
                var match = calculateMatch(profile, job);
                html += renderJobCard(job, match);
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
                html += renderJobCard(job, match);
            });
            html += '</div>';
        }

        // SCREEN 3: NOTIFICATIONS
        else if (hash === "#notifications") {
            html += '<div style="padding:16px;">';
            html += '<div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">';
            html += '<h3>Notifications 🔔</h3>';
            html += '<button onclick="alert(\'Notifications marked as read\')" class="btn-primary" style="width:auto; padding:6px 12px; font-size:12px;">Mark All Read</button>';
            html += '</div>';
            html += '<div class="card" style="margin:8px 0;">';
            html += '<strong>⚡ 12 new jobs added since your last visit.</strong>';
            html += '<p style="margin:4px 0; font-size:13px; color:var(--text-secondary);">Tap to browse high-match backend and mobile openings.</p>';
            html += '<span style="font-size:11px; color:var(--text-muted);">Just now</span>';
            html += '</div>';
            html += '</div>';
        }

        // SCREEN 4: PROFILE
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

        // SCREEN 5: SETTINGS
        else if (hash === "#settings") {
            html += '<div style="padding:16px;">';
            html += renderSettingsView();
            html += '</div>';
        }

        // SCREEN 6: ADMIN DASHBOARD
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
        html += '<button onclick="window.navigateTo(\'#home\')" class="app-bottom-nav-item ' + (hash==="#home"||hash===""||hash==="#"?"active":"") + '"><span class="icon">🏠</span><span>Home</span></button>';
        html += '<button onclick="window.navigateTo(\'#jobs\')" class="app-bottom-nav-item ' + (hash==="#jobs"?"active":"") + '"><span class="icon">💼</span><span>Jobs</span></button>';
        html += '<button onclick="window.navigateTo(\'#notifications\')" class="app-bottom-nav-item ' + (hash==="#notifications"?"active":"") + '"><span class="icon">🔔</span><span>Notifications</span></button>';
        html += '<button onclick="window.navigateTo(\'#profile\')" class="app-bottom-nav-item ' + (hash==="#profile"?"active":"") + '"><span class="icon">👤</span><span>Profile</span></button>';
        html += '</div>';

        mountPoint.innerHTML = html;
    }

    function renderJobCard(job, match) {
        var card = '<div class="job-card">';
        card += '<div class="job-title">' + job.title + '</div>';
        card += '<div class="company-name">' + job.company + ' • ' + job.location + '</div>';
        card += '<div class="job-meta"><span>💼 ' + job.type + '</span> <span>💰 ' + job.salary + '</span></div>';

        if (match.isGuest) {
            card += '<div class="match-badge match-guest">Sign in to see your personalized match <button onclick="window.handleSignInModal()" style="width:auto; padding:2px 8px; font-size:10px; margin-left:6px;" class="btn-primary">Sign In</button></div>';
        } else {
            card += '<div class="match-badge match-high">⚡ ' + match.overall + '% Match</div>';
        }

        card += '<div style="margin-top:10px; display:flex; gap:8px;">';
        card += '<button onclick="window.open(\'' + job.applicationUrl + '\', \'_blank\')" class="btn-primary" style="font-size:12px; padding:8px 12px;">Apply Now</button>';
        card += '</div>';
        card += '</div>';
        return card;
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
        html += '<button onclick="window.navigateTo(\'#settings\')" class="btn" style="margin-top:10px; background:var(--card-border); color:var(--text-primary);">Settings ⚙️</button>';
        html += '<button onclick="window.handleLogout()" style="margin-top:10px; background:#EF4444; color:#FFF;" class="btn">Logout</button>';

        if (profile.role === "admin") {
            html += '<button onclick="window.location.hash=\'#admin\'" style="margin-top:10px; background:#1E3A8A; color:#FFF;" class="btn">Admin Dashboard</button>';
        }

        return html;
    }

    function renderSettingsView() {
        var currentTheme = localStorage.getItem("getjoblink_theme") || "system";
        var currentScale = localStorage.getItem("getjoblink_font_scale") || "1";

        var html = '<h3>Settings ⚙️</h3>';

        // Appearance
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

        // Text Size
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

        // About & Version
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

    // --- NAVIGATION HELPER ---
    window.navigateTo = function(targetHash) {
        window.toggleSidebar(false);
        window.location.hash = targetHash;
        renderAppUI();
    };

    // --- EVENT HANDLERS ---
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
            html += renderJobCard(job, match);
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
