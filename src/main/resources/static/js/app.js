/**
 * Aegis University Management Portal - Frontend Application Logic
 * Communicates directly with Spring Boot 3 REST APIs on http://localhost:8081
 */

(function () {
  'use strict';

  // ─── Application State ───────────────────────────────────────────────────
  const state = {
    accessToken: localStorage.getItem('ums_access_token') || null,
    refreshToken: localStorage.getItem('ums_refresh_token') || null,
    currentUser: JSON.parse(localStorage.getItem('ums_user') || 'null'),
    currentView: 'dashboard',
    students: {
      page: 0,
      size: 15,
      sortBy: 'id',
      sortDir: 'desc',
      search: '',
      data: []
    },
    teachers: {
      page: 0,
      size: 15,
      sortBy: 'id',
      sortDir: 'desc',
      search: '',
      data: []
    },
    admins: {
      data: []
    }
  };

  // ─── DOM Selectors ───────────────────────────────────────────────────────
  const elements = {
    authScreen: document.getElementById('auth-screen'),
    loginForm: document.getElementById('login-form'),
    loginUsername: document.getElementById('login-username'),
    loginPassword: document.getElementById('login-password'),
    btnLoginSubmit: document.getElementById('btn-login-submit'),
    loginSpinner: document.getElementById('login-spinner'),
    loginText: document.getElementById('login-text'),
    btnAutofillAdmin: document.getElementById('btn-autofill-admin'),

    sidebar: document.getElementById('sidebar'),
    btnToggleSidebar: document.getElementById('btn-toggle-sidebar'),
    navLinks: document.querySelectorAll('.nav-link[data-view]'),
    viewSections: document.querySelectorAll('.view-section'),
    currentViewTitle: document.getElementById('current-view-title'),
    currentViewSubtitle: document.getElementById('current-view-subtitle'),

    sidebarUserAvatar: document.getElementById('sidebar-user-avatar'),
    sidebarUserName: document.getElementById('sidebar-user-name'),
    sidebarUserRole: document.getElementById('sidebar-user-role'),
    welcomeUserName: document.getElementById('welcome-user-name'),
    btnLogout: document.getElementById('btn-logout'),

    btnThemeToggle: document.getElementById('btn-theme-toggle'),

    // Dashboard
    statTotalUsers: document.getElementById('stat-total-users'),
    statTotalStudents: document.getElementById('stat-total-students'),
    statTotalTeachers: document.getElementById('stat-total-teachers'),
    statTotalAdmins: document.getElementById('stat-total-admins'),
    statActiveAdmins: document.getElementById('stat-active-admins'),
    badgeTotalStudents: document.getElementById('badge-total-students'),
    badgeTotalTeachers: document.getElementById('badge-total-teachers'),
    btnQuickAddStudent: document.getElementById('btn-quick-add-student'),
    btnQuickAddTeacher: document.getElementById('btn-quick-add-teacher'),
    btnDashNewStudent: document.getElementById('btn-dash-new-student'),
    btnDashNewTeacher: document.getElementById('btn-dash-new-teacher'),

    // Students View
    studentsTableBody: document.getElementById('students-table-body'),
    studentsSearchInput: document.getElementById('students-search-input'),
    studentsSortSelect: document.getElementById('students-sort-select'),
    btnRefreshStudents: document.getElementById('btn-refresh-students'),
    btnOpenAddStudent: document.getElementById('btn-open-add-student'),
    studentsCountBadge: document.getElementById('students-count-badge'),
    studentsPaginationInfo: document.getElementById('students-pagination-info'),
    studentsPaginationControls: document.getElementById('students-pagination-controls'),

    // Teachers View
    teachersTableBody: document.getElementById('teachers-table-body'),
    teachersSearchInput: document.getElementById('teachers-search-input'),
    teachersSortSelect: document.getElementById('teachers-sort-select'),
    btnRefreshTeachers: document.getElementById('btn-refresh-teachers'),
    btnOpenAddTeacher: document.getElementById('btn-open-add-teacher'),
    teachersCountBadge: document.getElementById('teachers-count-badge'),
    teachersPaginationInfo: document.getElementById('teachers-pagination-info'),
    teachersPaginationControls: document.getElementById('teachers-pagination-controls'),

    // Admins View
    navAdminsLink: document.getElementById('nav-admins-link'),
    navAdminTitle: document.getElementById('nav-admin-title'),
    adminsTableBody: document.getElementById('admins-table-body'),
    btnOpenAddAdmin: document.getElementById('btn-open-add-admin'),

    // Profile View
    profileAvatarLarge: document.getElementById('profile-avatar-large'),
    profileUsername: document.getElementById('profile-username'),
    profileEmail: document.getElementById('profile-email'),
    profileRoleTag: document.getElementById('profile-role-tag'),
    profileStatusBadge: document.getElementById('profile-status-badge'),
    profileAccountId: document.getElementById('profile-account-id'),
    profileCreatedAt: document.getElementById('profile-created-at'),
    profileUpdatedAt: document.getElementById('profile-updated-at'),

    // Modals
    modalStudent: document.getElementById('modal-student'),
    modalStudentTitle: document.getElementById('modal-student-title'),
    formStudent: document.getElementById('form-student'),
    studentFormId: document.getElementById('student-form-id'),
    studentAuthFields: document.getElementById('student-auth-fields'),
    studentPasswordHelp: document.getElementById('student-password-help'),
    studentEnabledGroup: document.getElementById('student-enabled-group'),

    modalTeacher: document.getElementById('modal-teacher'),
    modalTeacherTitle: document.getElementById('modal-teacher-title'),
    formTeacher: document.getElementById('form-teacher'),
    teacherFormId: document.getElementById('teacher-form-id'),
    teacherAuthFields: document.getElementById('teacher-auth-fields'),
    teacherPasswordHelp: document.getElementById('teacher-password-help'),
    teacherEnabledGroup: document.getElementById('teacher-enabled-group'),

    modalAdmin: document.getElementById('modal-admin'),
    formAdmin: document.getElementById('form-admin'),

    modalDetail: document.getElementById('modal-detail'),
    modalDetailTitle: document.getElementById('modal-detail-title'),
    modalDetailContent: document.getElementById('modal-detail-content'),

    modalConfirm: document.getElementById('modal-confirm'),
    confirmTitle: document.getElementById('confirm-title'),
    confirmMessage: document.getElementById('confirm-message'),
    btnConfirmAction: document.getElementById('btn-confirm-action'),

    toastContainer: document.getElementById('toast-container')
  };

  // ─── HTTP API Client with Automatic JWT Interceptor ───────────────────────
  async function apiRequest(endpoint, options = {}) {
    const url = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    const headers = options.headers || {};

    if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    if (state.accessToken) {
      headers['Authorization'] = `Bearer ${state.accessToken}`;
    }

    const config = {
      ...options,
      headers
    };

    try {
      let response = await fetch(url, config);

      // Handle 401 Unauthorized -> Attempt token refresh
      if (response.status === 401 && state.refreshToken && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/refresh')) {
        const refreshed = await attemptTokenRefresh();
        if (refreshed) {
          headers['Authorization'] = `Bearer ${state.accessToken}`;
          response = await fetch(url, config);
        } else {
          showAuthScreen();
          throw new Error('Session expired. Please log in again.');
        }
      }

      if (response.status === 204) {
        return null;
      }

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMessage = data?.message || (data?.errors ? Object.values(data.errors).join(', ') : `HTTP Error ${response.status}`);
        throw new Error(errorMessage);
      }

      return data;
    } catch (err) {
      console.error(`API Error on ${url}:`, err);
      throw err;
    }
  }

  // ─── Token Refresh ────────────────────────────────────────────────────────
  async function attemptTokenRefresh() {
    try {
      const res = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: state.refreshToken })
      });

      if (!res.ok) return false;

      const data = await res.json();
      state.accessToken = data.accessToken;
      state.refreshToken = data.refreshToken;
      localStorage.setItem('ums_access_token', data.accessToken);
      localStorage.setItem('ums_refresh_token', data.refreshToken);
      return true;
    } catch {
      return false;
    }
  }

  // ─── Toast Notifications ──────────────────────────────────────────────────
  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let iconSvg = '';
    if (type === 'success') {
      iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>';
    } else if (type === 'error') {
      iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f43f5e" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
    } else {
      iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';
    }

    toast.innerHTML = `
      ${iconSvg}
      <div class="toast-message">${escapeHtml(message)}</div>
      <button type="button" class="btn-icon btn-sm" style="border:none; background:transparent; width:auto; height:auto; opacity:0.6;" onclick="this.parentElement.remove()">✕</button>
    `;

    elements.toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  // ─── Authentication Handlers ──────────────────────────────────────────────
  async function handleLogin(e) {
    e.preventDefault();
    const username = elements.loginUsername.value.trim();
    const password = elements.loginPassword.value;

    if (!username || !password) {
      showToast('Please provide both username and password', 'error');
      return;
    }

    elements.btnLoginSubmit.disabled = true;
    elements.loginSpinner.style.display = 'inline';
    elements.loginText.style.display = 'none';

    try {
      const data = await apiRequest('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      });

      state.accessToken = data.accessToken;
      state.refreshToken = data.refreshToken;
      state.currentUser = data.user;

      localStorage.setItem('ums_access_token', data.accessToken);
      localStorage.setItem('ums_refresh_token', data.refreshToken);
      localStorage.setItem('ums_user', JSON.stringify(data.user));

      hideAuthScreen();
      updateUserInterface();
      showToast(`Welcome back, ${data.user.username}!`, 'success');

      // Load initial view
      navigateTo('dashboard');
    } catch (err) {
      showToast(err.message || 'Login failed. Check credentials.', 'error');
    } finally {
      elements.btnLoginSubmit.disabled = false;
      elements.loginSpinner.style.display = 'none';
      elements.loginText.style.display = 'inline';
    }
  }

  async function handleLogout() {
    try {
      if (state.accessToken) {
        await apiRequest('/api/v1/auth/logout', { method: 'POST' }).catch(() => {});
      }
    } finally {
      state.accessToken = null;
      state.refreshToken = null;
      state.currentUser = null;
      localStorage.removeItem('ums_access_token');
      localStorage.removeItem('ums_refresh_token');
      localStorage.removeItem('ums_user');
      showAuthScreen();
      showToast('You have been logged out securely.', 'info');
    }
  }

  function showAuthScreen() {
    elements.authScreen.classList.remove('hidden');
  }

  function hideAuthScreen() {
    elements.authScreen.classList.add('hidden');
  }

  function updateUserInterface() {
    if (!state.currentUser) return;

    const user = state.currentUser;
    const initials = (user.username || 'AD').substring(0, 2).toUpperCase();

    elements.sidebarUserAvatar.textContent = initials;
    elements.sidebarUserName.textContent = user.username;
    elements.sidebarUserRole.textContent = user.role;
    elements.welcomeUserName.textContent = user.username;

    // Profile View Details
    elements.profileAvatarLarge.textContent = initials;
    elements.profileUsername.textContent = user.username;
    elements.profileEmail.textContent = user.email || 'N/A';
    elements.profileRoleTag.textContent = `ROLE: ${user.role}`;
    elements.profileAccountId.textContent = user.id || '1';

    // Role-based visibility
    const isAdmin = user.role === 'ADMIN';
    if (elements.navAdminsLink) {
      elements.navAdminsLink.style.display = isAdmin ? 'flex' : 'none';
    }
    if (elements.navAdminTitle) {
      elements.navAdminTitle.style.display = isAdmin ? 'block' : 'none';
    }
  }

  // ─── View Routing ─────────────────────────────────────────────────────────
  function navigateTo(viewName) {
    state.currentView = viewName;
    window.location.hash = viewName;

    elements.navLinks.forEach(link => {
      if (link.dataset.view === viewName) {
        link.classList.add('active');
      } else {
        link.classList.remove('active');
      }
    });

    elements.viewSections.forEach(section => {
      section.classList.remove('active');
    });

    const targetSection = document.getElementById(`view-${viewName}`);
    if (targetSection) {
      targetSection.classList.add('active');
    }

    // Update Topbar Titles
    const viewMeta = {
      dashboard: { title: 'Dashboard Overview', sub: 'Aegis University Central Information System' },
      students: { title: 'Students Management', sub: 'Comprehensive Student Records & Directory' },
      teachers: { title: 'Faculty & Teachers', sub: 'Academic Faculty Directory & Designations' },
      admins: { title: 'System Administrators', sub: 'Role-Based Access Control & User Permissions' },
      profile: { title: 'User Account Profile', sub: 'Authenticated Credentials & Identity' }
    };

    if (viewMeta[viewName]) {
      elements.currentViewTitle.textContent = viewMeta[viewName].title;
      elements.currentViewSubtitle.textContent = viewMeta[viewName].sub;
    }

    // Close mobile sidebar
    elements.sidebar.classList.remove('open');

    // Trigger View Data Fetch
    if (viewName === 'dashboard') loadDashboardData();
    if (viewName === 'students') loadStudents();
    if (viewName === 'teachers') loadTeachers();
    if (viewName === 'admins') loadAdmins();
    if (viewName === 'profile') loadUserProfile();
  }

  // ─── Dashboard Data Loader ────────────────────────────────────────────────
  async function loadDashboardData() {
    try {
      if (state.currentUser?.role === 'ADMIN') {
        const dashboard = await apiRequest('/api/v1/admin/dashboard');
        elements.statTotalUsers.textContent = dashboard.totalUsers.toLocaleString();
        elements.statTotalStudents.textContent = dashboard.totalStudents.toLocaleString();
        elements.statTotalTeachers.textContent = dashboard.totalTeachers.toLocaleString();
        elements.statTotalAdmins.textContent = dashboard.totalAdmins.toLocaleString();
        elements.statActiveAdmins.textContent = `● ${dashboard.activeAdmins} Active`;
        elements.badgeTotalStudents.textContent = dashboard.totalStudents;
        elements.badgeTotalTeachers.textContent = dashboard.totalTeachers;
      } else {
        // Non-admin fallback (fetch paginated totals)
        const [studRes, teachRes] = await Promise.all([
          apiRequest('/api/v1/students?page=0&size=1').catch(() => null),
          apiRequest('/api/v1/teachers?page=0&size=1').catch(() => null)
        ]);

        const studentTotal = studRes?.totalElements || 0;
        const teacherTotal = teachRes?.totalElements || 0;

        elements.statTotalStudents.textContent = studentTotal;
        elements.statTotalTeachers.textContent = teacherTotal;
        elements.statTotalUsers.textContent = studentTotal + teacherTotal;
        elements.badgeTotalStudents.textContent = studentTotal;
        elements.badgeTotalTeachers.textContent = teacherTotal;
      }
    } catch (err) {
      console.warn('Dashboard stats error:', err);
    }
  }

  // ─── Students Management ──────────────────────────────────────────────────
  async function loadStudents() {
    elements.studentsTableBody.innerHTML = `
      <tr>
        <td colspan="7" style="text-align: center; padding: 40px; color: var(--text-muted);">
          Loading student directory...
        </td>
      </tr>
    `;

    try {
      const [sortBy, sortDir] = elements.studentsSortSelect.value.split(',');
      const page = state.students.page;
      const size = state.students.size;

      const res = await apiRequest(`/api/v1/students?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`);
      state.students.data = res.content || [];
      renderStudentsTable(state.students.data);
      renderPagination(
        res,
        elements.studentsPaginationInfo,
        elements.studentsPaginationControls,
        newPage => {
          state.students.page = newPage;
          loadStudents();
        }
      );

      elements.studentsCountBadge.textContent = `${res.totalElements} Students`;
      elements.badgeTotalStudents.textContent = res.totalElements;
    } catch (err) {
      elements.studentsTableBody.innerHTML = `
        <tr>
          <td colspan="7" style="text-align: center; padding: 40px; color: var(--accent-rose);">
            Failed to load students: ${escapeHtml(err.message)}
          </td>
        </tr>
      `;
    }
  }

  function renderStudentsTable(students) {
    if (!students || students.length === 0) {
      elements.studentsTableBody.innerHTML = `
        <tr>
          <td colspan="7" style="text-align: center; padding: 40px; color: var(--text-muted);">
            No students found. Click <strong>+ Add Student</strong> to enroll someone.
          </td>
        </tr>
      `;
      return;
    }

    const searchTerm = state.students.search.toLowerCase();
    const filtered = students.filter(s => {
      if (!searchTerm) return true;
      return (
        s.fullName?.toLowerCase().includes(searchTerm) ||
        s.studentNumber?.toLowerCase().includes(searchTerm) ||
        s.department?.toLowerCase().includes(searchTerm) ||
        s.account?.email?.toLowerCase().includes(searchTerm)
      );
    });

    elements.studentsTableBody.innerHTML = filtered.map(student => {
      const initials = (student.fullName || 'ST').substring(0, 2).toUpperCase();
      const isEnabled = student.account?.enabled !== false;
      const statusBadge = isEnabled
        ? '<span class="badge badge-success">Active</span>'
        : '<span class="badge badge-danger">Disabled</span>';

      return `
        <tr>
          <td>
            <div class="user-cell">
              <div class="avatar" style="background: linear-gradient(135deg, #4f46e5, #06b6d4);">${initials}</div>
              <div class="user-cell-meta">
                <h4>${escapeHtml(student.fullName || 'N/A')}</h4>
                <p>${escapeHtml(student.account?.email || 'No email')}</p>
              </div>
            </div>
          </td>
          <td><code>${escapeHtml(student.studentNumber || '-')}</code></td>
          <td>${escapeHtml(student.department || '-')}</td>
          <td><span class="badge badge-indigo">Semester ${student.semester || 1}</span></td>
          <td>${escapeHtml(student.phone || '-')}</td>
          <td>${statusBadge}</td>
          <td>
            <div class="action-buttons" style="justify-content: flex-end;">
              <button type="button" class="action-btn" title="View Student" onclick="window.umsApp.viewStudent(${student.id})">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
              <button type="button" class="action-btn" title="Edit Student" onclick="window.umsApp.editStudent(${student.id})">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
                </svg>
              </button>
              <button type="button" class="action-btn delete" title="Delete Student" onclick="window.umsApp.deleteStudent(${student.id}, '${escapeHtml(student.fullName)}')">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 6h18"/>
                  <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                  <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                </svg>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  // ─── Teachers Management ──────────────────────────────────────────────────
  async function loadTeachers() {
    elements.teachersTableBody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align: center; padding: 40px; color: var(--text-muted);">
          Loading faculty directory...
        </td>
      </tr>
    `;

    try {
      const [sortBy, sortDir] = elements.teachersSortSelect.value.split(',');
      const page = state.teachers.page;
      const size = state.teachers.size;

      const res = await apiRequest(`/api/v1/teachers?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`);
      state.teachers.data = res.content || [];
      renderTeachersTable(state.teachers.data);
      renderPagination(
        res,
        elements.teachersPaginationInfo,
        elements.teachersPaginationControls,
        newPage => {
          state.teachers.page = newPage;
          loadTeachers();
        }
      );

      elements.teachersCountBadge.textContent = `${res.totalElements} Faculty`;
      elements.badgeTotalTeachers.textContent = res.totalElements;
    } catch (err) {
      elements.teachersTableBody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align: center; padding: 40px; color: var(--accent-rose);">
            Failed to load teachers: ${escapeHtml(err.message)}
          </td>
        </tr>
      `;
    }
  }

  function renderTeachersTable(teachers) {
    if (!teachers || teachers.length === 0) {
      elements.teachersTableBody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align: center; padding: 40px; color: var(--text-muted);">
            No faculty members found. Click <strong>+ Add Teacher</strong> to add an instructor.
          </td>
        </tr>
      `;
      return;
    }

    const searchTerm = state.teachers.search.toLowerCase();
    const filtered = teachers.filter(t => {
      if (!searchTerm) return true;
      return (
        t.fullName?.toLowerCase().includes(searchTerm) ||
        t.department?.toLowerCase().includes(searchTerm) ||
        t.designation?.toLowerCase().includes(searchTerm) ||
        t.account?.email?.toLowerCase().includes(searchTerm)
      );
    });

    elements.teachersTableBody.innerHTML = filtered.map(teacher => {
      const initials = (teacher.fullName || 'TC').substring(0, 2).toUpperCase();
      const isEnabled = teacher.account?.enabled !== false;
      const statusBadge = isEnabled
        ? '<span class="badge badge-success">Active</span>'
        : '<span class="badge badge-danger">Disabled</span>';

      return `
        <tr>
          <td>
            <div class="user-cell">
              <div class="avatar" style="background: linear-gradient(135deg, #7c3aed, #ec4899);">${initials}</div>
              <div class="user-cell-meta">
                <h4>${escapeHtml(teacher.fullName || 'N/A')}</h4>
                <p>${escapeHtml(teacher.account?.email || 'No email')}</p>
              </div>
            </div>
          </td>
          <td>${escapeHtml(teacher.department || '-')}</td>
          <td><span class="badge badge-indigo">${escapeHtml(teacher.designation || 'Lecturer')}</span></td>
          <td>${escapeHtml(teacher.phone || '-')}</td>
          <td>${statusBadge}</td>
          <td>
            <div class="action-buttons" style="justify-content: flex-end;">
              <button type="button" class="action-btn" title="View Teacher" onclick="window.umsApp.viewTeacher(${teacher.id})">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
              <button type="button" class="action-btn" title="Edit Teacher" onclick="window.umsApp.editTeacher(${teacher.id})">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
                </svg>
              </button>
              <button type="button" class="action-btn delete" title="Delete Teacher" onclick="window.umsApp.deleteTeacher(${teacher.id}, '${escapeHtml(teacher.fullName)}')">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 6h18"/>
                  <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                  <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                </svg>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');
  }

  // ─── Administrators Management ───────────────────────────────────────────
  async function loadAdmins() {
    if (state.currentUser?.role !== 'ADMIN') return;

    elements.adminsTableBody.innerHTML = `
      <tr>
        <td colspan="5" style="text-align: center; padding: 40px; color: var(--text-muted);">
          Loading administrators...
        </td>
      </tr>
    `;

    try {
      const res = await apiRequest('/api/v1/admins?page=0&size=50');
      state.admins.data = res.content || [];

      if (state.admins.data.length === 0) {
        elements.adminsTableBody.innerHTML = `
          <tr>
            <td colspan="5" style="text-align: center; padding: 40px; color: var(--text-muted);">
              No administrators found.
            </td>
          </tr>
        `;
        return;
      }

      elements.adminsTableBody.innerHTML = state.admins.data.map(admin => {
        const initials = (admin.fullName || admin.account?.username || 'AD').substring(0, 2).toUpperCase();
        return `
          <tr>
            <td>
              <div class="user-cell">
                <div class="avatar" style="background: linear-gradient(135deg, #f59e0b, #ef4444);">${initials}</div>
                <div class="user-cell-meta">
                  <h4>${escapeHtml(admin.fullName || admin.account?.username || 'Administrator')}</h4>
                  <p>${escapeHtml(admin.account?.email || '-')}</p>
                </div>
              </div>
            </td>
            <td>${escapeHtml(admin.department || 'Central IT')}</td>
            <td><span class="badge badge-success">Active</span></td>
            <td><span class="badge badge-warning">ADMIN</span></td>
            <td>${formatDate(admin.account?.createdAt)}</td>
          </tr>
        `;
      }).join('');
    } catch (err) {
      elements.adminsTableBody.innerHTML = `
        <tr>
          <td colspan="5" style="text-align: center; padding: 40px; color: var(--accent-rose);">
            ${escapeHtml(err.message)}
          </td>
        </tr>
      `;
    }
  }

  // ─── User Profile Loader ──────────────────────────────────────────────────
  async function loadUserProfile() {
    try {
      const me = await apiRequest('/api/v1/auth/me');
      elements.profileUsername.textContent = me.username;
      elements.profileEmail.textContent = me.email;
      elements.profileRoleTag.textContent = `ROLE: ${me.role}`;
      elements.profileAccountId.textContent = me.id;
      elements.profileCreatedAt.textContent = formatDate(me.createdAt);
      elements.profileUpdatedAt.textContent = formatDate(me.updatedAt);
      elements.profileStatusBadge.textContent = me.enabled ? 'Active' : 'Disabled';
      elements.profileStatusBadge.className = me.enabled ? 'badge badge-success' : 'badge badge-danger';
    } catch (err) {
      console.warn('Profile fetch error:', err);
    }
  }

  // ─── Modal & CRUD Actions ─────────────────────────────────────────────────
  function openAddStudentModal() {
    elements.modalStudentTitle.textContent = 'Add New Student';
    elements.formStudent.reset();
    elements.studentFormId.value = '';
    elements.studentAuthFields.style.display = 'grid';
    elements.studentPasswordHelp.textContent = 'Required for new student (min 8 chars)';
    elements.studentEnabledGroup.style.display = 'none';
    document.getElementById('student-username').required = true;
    document.getElementById('student-password').required = true;
    document.getElementById('student-number').disabled = false;
    elements.modalStudent.showModal();
  }

  async function editStudent(id) {
    try {
      const student = await apiRequest(`/api/v1/students/${id}`);
      elements.modalStudentTitle.textContent = `Edit Student: ${student.fullName}`;
      elements.studentFormId.value = student.id;

      document.getElementById('student-fullname').value = student.fullName || '';
      document.getElementById('student-email').value = student.account?.email || '';
      document.getElementById('student-number').value = student.studentNumber || '';
      document.getElementById('student-number').disabled = true; // studentNumber immutable
      document.getElementById('student-department').value = student.department || '';
      document.getElementById('student-semester').value = student.semester || 1;
      document.getElementById('student-phone').value = student.phone || '';

      // For updates, username cannot be changed; password is optional
      elements.studentAuthFields.style.display = 'grid';
      document.getElementById('student-username').value = student.account?.username || '';
      document.getElementById('student-username').disabled = true;
      document.getElementById('student-username').required = false;
      document.getElementById('student-password').value = '';
      document.getElementById('student-password').required = false;
      elements.studentPasswordHelp.textContent = 'Leave blank to keep current password';

      elements.studentEnabledGroup.style.display = 'block';
      document.getElementById('student-enabled').checked = student.account?.enabled !== false;

      elements.modalStudent.showModal();
    } catch (err) {
      showToast(`Error fetching student: ${err.message}`, 'error');
    }
  }

  async function handleSaveStudent(e) {
    e.preventDefault();
    const id = elements.studentFormId.value;
    const isEdit = Boolean(id);

    const payload = {
      fullName: document.getElementById('student-fullname').value.trim(),
      email: document.getElementById('student-email').value.trim(),
      department: document.getElementById('student-department').value.trim(),
      semester: parseInt(document.getElementById('student-semester').value, 10) || 1,
      phone: document.getElementById('student-phone').value.trim()
    };

    const passwordVal = document.getElementById('student-password').value;
    if (passwordVal) {
      payload.password = passwordVal;
    }

    try {
      if (isEdit) {
        payload.enabled = document.getElementById('student-enabled').checked;
        await apiRequest(`/api/v1/students/${id}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        });
        showToast('Student record updated successfully!', 'success');
      } else {
        payload.username = document.getElementById('student-username').value.trim();
        payload.studentNumber = document.getElementById('student-number').value.trim();
        if (!passwordVal) {
          showToast('Password is required when creating a student', 'error');
          return;
        }
        await apiRequest('/api/v1/students', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        showToast('New student created and enrolled successfully!', 'success');
      }

      elements.modalStudent.close();
      loadStudents();
      loadDashboardData();
    } catch (err) {
      showToast(err.message || 'Operation failed', 'error');
    }
  }

  function openAddTeacherModal() {
    elements.modalTeacherTitle.textContent = 'Add Faculty Member';
    elements.formTeacher.reset();
    elements.teacherFormId.value = '';
    elements.teacherAuthFields.style.display = 'grid';
    elements.teacherPasswordHelp.textContent = 'Required for new faculty (min 8 chars)';
    elements.teacherEnabledGroup.style.display = 'none';
    document.getElementById('teacher-username').required = true;
    document.getElementById('teacher-password').required = true;
    document.getElementById('teacher-username').disabled = false;
    elements.modalTeacher.showModal();
  }

  async function editTeacher(id) {
    try {
      const teacher = await apiRequest(`/api/v1/teachers/${id}`);
      elements.modalTeacherTitle.textContent = `Edit Faculty: ${teacher.fullName}`;
      elements.teacherFormId.value = teacher.id;

      document.getElementById('teacher-fullname').value = teacher.fullName || '';
      document.getElementById('teacher-email').value = teacher.account?.email || '';
      document.getElementById('teacher-department').value = teacher.department || '';
      document.getElementById('teacher-designation').value = teacher.designation || '';
      document.getElementById('teacher-phone').value = teacher.phone || '';

      elements.teacherAuthFields.style.display = 'grid';
      document.getElementById('teacher-username').value = teacher.account?.username || '';
      document.getElementById('teacher-username').disabled = true;
      document.getElementById('teacher-username').required = false;
      document.getElementById('teacher-password').value = '';
      document.getElementById('teacher-password').required = false;
      elements.teacherPasswordHelp.textContent = 'Leave blank to keep current password';

      elements.teacherEnabledGroup.style.display = 'block';
      document.getElementById('teacher-enabled').checked = teacher.account?.enabled !== false;

      elements.modalTeacher.showModal();
    } catch (err) {
      showToast(`Error fetching teacher: ${err.message}`, 'error');
    }
  }

  async function handleSaveTeacher(e) {
    e.preventDefault();
    const id = elements.teacherFormId.value;
    const isEdit = Boolean(id);

    const payload = {
      fullName: document.getElementById('teacher-fullname').value.trim(),
      email: document.getElementById('teacher-email').value.trim(),
      department: document.getElementById('teacher-department').value.trim(),
      designation: document.getElementById('teacher-designation').value.trim(),
      phone: document.getElementById('teacher-phone').value.trim()
    };

    const passwordVal = document.getElementById('teacher-password').value;
    if (passwordVal) {
      payload.password = passwordVal;
    }

    try {
      if (isEdit) {
        payload.enabled = document.getElementById('teacher-enabled').checked;
        await apiRequest(`/api/v1/teachers/${id}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        });
        showToast('Faculty member updated successfully!', 'success');
      } else {
        payload.username = document.getElementById('teacher-username').value.trim();
        if (!passwordVal) {
          showToast('Password is required when creating a teacher', 'error');
          return;
        }
        await apiRequest('/api/v1/teachers', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        showToast('New faculty member appointed successfully!', 'success');
      }

      elements.modalTeacher.close();
      loadTeachers();
      loadDashboardData();
    } catch (err) {
      showToast(err.message || 'Operation failed', 'error');
    }
  }

  // Detail Inspector View
  async function viewStudent(id) {
    try {
      const student = await apiRequest(`/api/v1/students/${id}`);
      elements.modalDetailTitle.textContent = `Student Profile: ${student.fullName}`;
      elements.modalDetailContent.innerHTML = `
        <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 20px;">
          <div class="avatar" style="width: 54px; height: 54px; font-size: 18px; background: linear-gradient(135deg, #4f46e5, #06b6d4);">
            ${(student.fullName || 'ST').substring(0, 2).toUpperCase()}
          </div>
          <div>
            <h4 style="font-size: 18px; font-weight: 700;">${escapeHtml(student.fullName)}</h4>
            <p style="color: var(--text-muted); font-size: 13px;">${escapeHtml(student.account?.email)}</p>
          </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px; background: var(--bg-app); padding: 18px; border-radius: var(--radius-md);">
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Student ID Number</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);"><code>${escapeHtml(student.studentNumber)}</code></div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Department</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${escapeHtml(student.department)}</div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Current Semester</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">Semester ${student.semester || 1}</div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Contact Phone</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${escapeHtml(student.phone || 'N/A')}</div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Account Username</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${escapeHtml(student.account?.username)}</div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Enrollment Date</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${formatDate(student.account?.createdAt)}</div>
          </div>
        </div>
      `;
      elements.modalDetail.showModal();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  async function viewTeacher(id) {
    try {
      const teacher = await apiRequest(`/api/v1/teachers/${id}`);
      elements.modalDetailTitle.textContent = `Faculty Profile: ${teacher.fullName}`;
      elements.modalDetailContent.innerHTML = `
        <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 20px;">
          <div class="avatar" style="width: 54px; height: 54px; font-size: 18px; background: linear-gradient(135deg, #7c3aed, #ec4899);">
            ${(teacher.fullName || 'TC').substring(0, 2).toUpperCase()}
          </div>
          <div>
            <h4 style="font-size: 18px; font-weight: 700;">${escapeHtml(teacher.fullName)}</h4>
            <p style="color: var(--text-muted); font-size: 13px;">${escapeHtml(teacher.account?.email)}</p>
          </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px; background: var(--bg-app); padding: 18px; border-radius: var(--radius-md);">
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Department</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${escapeHtml(teacher.department)}</div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Designation</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${escapeHtml(teacher.designation || 'Lecturer')}</div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Contact Phone</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${escapeHtml(teacher.phone || 'N/A')}</div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Account Username</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${escapeHtml(teacher.account?.username)}</div>
          </div>
          <div>
            <div style="font-size: 11px; text-transform: uppercase; color: var(--text-muted);">Appointment Date</div>
            <div style="font-size: 14px; font-weight: 600; color: var(--text-primary);">${formatDate(teacher.account?.createdAt)}</div>
          </div>
        </div>
      `;
      elements.modalDetail.showModal();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  // Deletions with Confirmation
  function deleteStudent(id, name) {
    elements.confirmTitle.textContent = 'Delete Student';
    elements.confirmMessage.textContent = `Are you sure you want to delete student "${name}"? This action will permanently remove their records.`;
    elements.btnConfirmAction.onclick = async () => {
      try {
        await apiRequest(`/api/v1/students/${id}`, { method: 'DELETE' });
        showToast(`Student ${name} deleted successfully`, 'success');
        elements.modalConfirm.close();
        loadStudents();
        loadDashboardData();
      } catch (err) {
        showToast(err.message || 'Failed to delete student', 'error');
      }
    };
    elements.modalConfirm.showModal();
  }

  function deleteTeacher(id, name) {
    elements.confirmTitle.textContent = 'Delete Faculty Member';
    elements.confirmMessage.textContent = `Are you sure you want to delete instructor "${name}"?`;
    elements.btnConfirmAction.onclick = async () => {
      try {
        await apiRequest(`/api/v1/teachers/${id}`, { method: 'DELETE' });
        showToast(`Teacher ${name} deleted successfully`, 'success');
        elements.modalConfirm.close();
        loadTeachers();
        loadDashboardData();
      } catch (err) {
        showToast(err.message || 'Failed to delete teacher', 'error');
      }
    };
    elements.modalConfirm.showModal();
  }

  // Admin creation
  async function handleSaveAdmin(e) {
    e.preventDefault();
    const payload = {
      username: document.getElementById('admin-username').value.trim(),
      password: document.getElementById('admin-password').value,
      fullName: document.getElementById('admin-fullname').value.trim(),
      email: document.getElementById('admin-email').value.trim(),
      department: document.getElementById('admin-department').value.trim()
    };

    try {
      await apiRequest('/api/v1/admins', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      showToast('New Administrator account created successfully!', 'success');
      elements.modalAdmin.close();
      elements.formAdmin.reset();
      loadAdmins();
      loadDashboardData();
    } catch (err) {
      showToast(err.message || 'Failed to create admin', 'error');
    }
  }

  // ─── Pagination Renderer Helper ───────────────────────────────────────────
  function renderPagination(pageData, infoEl, controlsEl, onPageClick) {
    const page = pageData.page || 0;
    const size = pageData.size || 15;
    const totalElements = pageData.totalElements || 0;
    const totalPages = pageData.totalPages || 1;

    const start = totalElements === 0 ? 0 : page * size + 1;
    const end = Math.min((page + 1) * size, totalElements);
    infoEl.textContent = `Showing ${start} to ${end} of ${totalElements} records`;

    controlsEl.innerHTML = `
      <button type="button" class="page-btn" ${page === 0 ? 'disabled' : ''} id="btn-prev-page">
        ‹
      </button>
      <span style="font-size: 13px; font-weight: 600; padding: 0 8px; color: var(--text-muted);">
        Page ${page + 1} of ${totalPages}
      </span>
      <button type="button" class="page-btn" ${page + 1 >= totalPages ? 'disabled' : ''} id="btn-next-page">
        ›
      </button>
    `;

    const prevBtn = controlsEl.querySelector('#btn-prev-page');
    const nextBtn = controlsEl.querySelector('#btn-next-page');

    if (prevBtn && page > 0) {
      prevBtn.onclick = () => onPageClick(page - 1);
    }
    if (nextBtn && page + 1 < totalPages) {
      nextBtn.onclick = () => onPageClick(page + 1);
    }
  }

  // ─── Theme Management ─────────────────────────────────────────────────────
  function initTheme() {
    const savedTheme = localStorage.getItem('ums_theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
  }

  function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('ums_theme', newTheme);
  }

  // ─── Utility Helpers ──────────────────────────────────────────────────────
  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function formatDate(isoString) {
    if (!isoString) return '-';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
    } catch {
      return isoString;
    }
  }

  // ─── Event Listeners Setup ────────────────────────────────────────────────
  function setupEventListeners() {
    // Auth Form
    elements.loginForm.addEventListener('submit', handleLogin);
    elements.btnLogout.addEventListener('click', handleLogout);

    // Autofill Admin Demo
    elements.btnAutofillAdmin.addEventListener('click', () => {
      elements.loginUsername.value = 'admin';
      elements.loginPassword.value = 'Admin@123456';
    });

    // Theme Toggle
    elements.btnThemeToggle.addEventListener('click', toggleTheme);

    // Sidebar Mobile Toggle
    elements.btnToggleSidebar.addEventListener('click', () => {
      elements.sidebar.classList.toggle('open');
    });

    // Nav Links View Switching
    elements.navLinks.forEach(link => {
      link.addEventListener('click', () => {
        const view = link.dataset.view;
        navigateTo(view);
      });
    });

    // Hash change handler
    window.addEventListener('hashchange', () => {
      const hash = window.location.hash.replace('#', '');
      if (hash && hash !== state.currentView) {
        navigateTo(hash);
      }
    });

    // Quick add buttons
    elements.btnQuickAddStudent.addEventListener('click', openAddStudentModal);
    elements.btnDashNewStudent.addEventListener('click', openAddStudentModal);
    elements.btnOpenAddStudent.addEventListener('click', openAddStudentModal);

    elements.btnQuickAddTeacher.addEventListener('click', openAddTeacherModal);
    elements.btnDashNewTeacher.addEventListener('click', openAddTeacherModal);
    elements.btnOpenAddTeacher.addEventListener('click', openAddTeacherModal);

    elements.btnOpenAddAdmin.addEventListener('click', () => {
      elements.formAdmin.reset();
      elements.modalAdmin.showModal();
    });

    // Forms Submissions
    elements.formStudent.addEventListener('submit', handleSaveStudent);
    elements.formTeacher.addEventListener('submit', handleSaveTeacher);
    elements.formAdmin.addEventListener('submit', handleSaveAdmin);

    // Close Modal Buttons
    document.querySelectorAll('[data-close-modal]').forEach(btn => {
      btn.addEventListener('click', () => {
        const modalId = btn.getAttribute('data-close-modal');
        const modal = document.getElementById(modalId);
        if (modal) modal.close();
      });
    });

    // Search and Sort Listeners
    elements.studentsSearchInput.addEventListener('input', (e) => {
      state.students.search = e.target.value.trim();
      renderStudentsTable(state.students.data);
    });

    elements.studentsSortSelect.addEventListener('change', () => {
      state.students.page = 0;
      loadStudents();
    });

    elements.btnRefreshStudents.addEventListener('click', loadStudents);

    elements.teachersSearchInput.addEventListener('input', (e) => {
      state.teachers.search = e.target.value.trim();
      renderTeachersTable(state.teachers.data);
    });

    elements.teachersSortSelect.addEventListener('change', () => {
      state.teachers.page = 0;
      loadTeachers();
    });

    elements.btnRefreshTeachers.addEventListener('click', loadTeachers);
  }

  // ─── Export Global API for Table Button Handlers ──────────────────────────
  window.umsApp = {
    viewStudent,
    editStudent,
    deleteStudent,
    viewTeacher,
    editTeacher,
    deleteTeacher
  };

  // ─── App Initialization ───────────────────────────────────────────────────
  function init() {
    initTheme();
    setupEventListeners();

    if (state.accessToken && state.currentUser) {
      hideAuthScreen();
      updateUserInterface();
      const initialView = window.location.hash.replace('#', '') || 'dashboard';
      navigateTo(initialView);
    } else {
      showAuthScreen();
    }
  }

  // Run on DOM Ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
