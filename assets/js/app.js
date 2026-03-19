// ─── CONFIG ──────────────────────────────────────────────
const API = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'http://localhost:8080/api'
    : 'https://blog-manager-production-6034.up.railway.app/api';

// ─── STATE ───────────────────────────────────────────────
let currentUser = null;
let authToken = localStorage.getItem('blogToken');
const savedUser = localStorage.getItem('blogUsername');
if (savedUser) currentUser = savedUser;
let editingId = null;

// ─── HELPERS ─────────────────────────────────────────────
function showPage(id) {
    ['welcomePage','loginPage','registerPage','dashboardPage','addArticlePage','editArticlePage']
        .forEach(p => document.getElementById(p).classList.add('hidden'));
    document.getElementById(id).classList.remove('hidden');
}

function toast(msg, type = 'success') {
    const el = document.getElementById('toast');
    el.textContent = type === 'success' ? '✓  ' + msg : '✗  ' + msg;
    el.className = `toast ${type}`;
    clearTimeout(el._t);
    el._t = setTimeout(() => el.classList.add('hidden'), 3000);
}

function setError(elId, msg) {
    const el = document.getElementById(elId);
    el.innerHTML = msg ? `<div class="error-inline">${msg}</div>` : '';
}

function formatDate(d) {
    if (!d) return '';
    const [y, m, day] = d.split('-').map(Number);
    return new Date(y, m - 1, day).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
}

function updateHeader() {
    const actions = document.getElementById('headerActions');
    if (currentUser) {
        actions.innerHTML = `
            <span style="font-family:'IBM Plex Mono',monospace;font-size:0.72rem;color:var(--muted);letter-spacing:0.06em;">${currentUser}</span>
            <button class="btn btn-ghost" id="logoutBtn">Log out</button>
        `;
        document.getElementById('logoutBtn').addEventListener('click', logout);
    } else {
        actions.innerHTML = `
            <button class="btn btn-ghost" id="headerLoginBtn">Log in</button>
            <button class="btn btn-primary" id="headerRegisterBtn">Get started</button>
        `;
        document.getElementById('headerLoginBtn').addEventListener('click', () => showPage('loginPage'));
        document.getElementById('headerRegisterBtn').addEventListener('click', () => showPage('registerPage'));
    }
}

// ─── API CALLS ───────────────────────────────────────────
async function apiFetch(method, path, body) {
    const opts = {
        method,
        headers: {}
    };
    if (authToken) {
        opts.headers['Authorization'] = `Bearer ${authToken}`;
    }
    if (body) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(body);
    }
    const res = await fetch(API + path, opts);
    return res;
}

// ─── AUTH ─────────────────────────────────────────────────
async function login(username, password) {
    const res = await apiFetch('POST', '/auth/login', { username, password });
    if (!res.ok) throw new Error('Invalid username or password.');
    const payload = await res.json();
    if (!payload.token) throw new Error('Missing token in login response.');

    authToken = payload.token;
    currentUser = payload.username || username;
    localStorage.setItem('blogToken', authToken);
    localStorage.setItem('blogUsername', currentUser);
    return currentUser;
}

async function register(username, password) {
    const res = await apiFetch('POST', '/auth/register', { username, password });
    if (!res.ok) {
        const txt = await res.text().catch(() => '');
        throw new Error(txt || 'Registration failed. Username may already be taken.');
    }
}

function logout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('blogToken');
    localStorage.removeItem('blogUsername');
    updateHeader();

    // Clear the input fields so the next user doesn't see them
    document.getElementById('loginUser').value = '';
    document.getElementById('loginPass').value = '';
    document.getElementById('regUser').value = '';
    document.getElementById('regPass').value = '';

    showPage('welcomePage');
    toast('You\'ve been logged out.');
}

// ─── DASHBOARD ───────────────────────────────────────────
async function renderDashboard() {
    document.getElementById('dashUsername').textContent = currentUser;
    const container = document.getElementById('dashboardArticles');
    container.innerHTML = '<p style="color:var(--muted);padding:1rem 0;font-family:\'IBM Plex Mono\',monospace;font-size:0.8rem;">Loading…</p>';

    try {
        const res = await apiFetch('GET', '/articles');
        if (!res.ok) throw new Error('Failed to load articles.');
        const articles = await res.json();

        if (!articles.length) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📝</div>
                    <p>No articles yet. Start writing!</p>
                    <button class="btn btn-primary" onclick="document.getElementById('newArticleBtn').click()">Write your first article</button>
                </div>`;
            return;
        }

        container.innerHTML = articles.map(a => `
            <div class="article-row">
                <div class="article-row-info">
                    <h3>${escHtml(a.title)}</h3>
                    <div class="meta">${formatDate(a.date)}</div>
                </div>
                <div class="article-row-actions">
                    <button class="btn btn-edit" data-id="${a.id}">Edit</button>
                    <button class="btn btn-danger" data-id="${a.id}">Delete</button>
                </div>
            </div>
        `).join('');

        container.querySelectorAll('.btn-edit').forEach(b =>
            b.addEventListener('click', () => openEdit(+b.dataset.id)));
        container.querySelectorAll('.btn-danger').forEach(b =>
            b.addEventListener('click', () => deleteArticle(+b.dataset.id)));

    } catch (err) {
        container.innerHTML = `<div class="error-inline">⚠ ${err.message}</div>`;
    }
}

function escHtml(str) {
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// ─── EDIT ─────────────────────────────────────────────────
async function openEdit(id) {
    try {
        const res = await apiFetch('GET', `/articles/${id}`);
        if (!res.ok) throw new Error('Could not load article.');
        const a = await res.json();
        editingId = id;
        document.getElementById('editTitle').value = a.title;
        document.getElementById('editDate').value = a.date;
        document.getElementById('editContent').value = a.content;
        showPage('editArticlePage');
    } catch (err) {
        toast(err.message, 'error');
    }
}

// ─── DELETE ───────────────────────────────────────────────
async function deleteArticle(id) {
    if (!confirm('Delete this article? This cannot be undone.')) return;
    try {
        const res = await apiFetch('DELETE', `/articles/${id}`);
        if (!res.ok) throw new Error('Delete failed.');
        toast('Article deleted.');
        renderDashboard();
    } catch (err) {
        toast(err.message, 'error');
    }
}

// ─── EVENT LISTENERS ─────────────────────────────────────

document.getElementById('logoBtn').addEventListener('click', () => {
    if (currentUser) { showPage('dashboardPage'); renderDashboard(); }
    else showPage('welcomePage');
});

document.getElementById('heroRegisterBtn').addEventListener('click', () => showPage('registerPage'));
document.getElementById('heroLoginBtn').addEventListener('click', () => showPage('loginPage'));

document.getElementById('loginSubmitBtn').addEventListener('click', async () => {
    const btn = document.getElementById('loginSubmitBtn');
    const user = document.getElementById('loginUser').value.replace(/\s+/g, '');
    const pass = document.getElementById('loginPass').value.replace(/\s+/g, '');
    setError('loginError', '');
    if (!user || !pass) { setError('loginError', 'Please fill in all fields.'); return; }

    btn.innerHTML = '<span class="loading-spinner"></span> Signing in…';
    btn.disabled = true;
    try {
        await login(user, pass);
        updateHeader();
        document.getElementById('loginUser').value = '';
        document.getElementById('loginPass').value = '';
        showPage('dashboardPage');
        renderDashboard();
        toast('Welcome back, ' + currentUser + '!');
    } catch (err) {
        setError('loginError', err.message);
    } finally {
        btn.innerHTML = 'Sign in';
        btn.disabled = false;
    }
});

['loginUser','loginPass'].forEach(id =>
    document.getElementById(id).addEventListener('keydown', e => {
        if (e.key === 'Enter') document.getElementById('loginSubmitBtn').click();
    })
);

document.getElementById('cancelLoginBtn').addEventListener('click', e => { e.preventDefault(); showPage('welcomePage'); });
document.getElementById('switchToRegister').addEventListener('click', e => { e.preventDefault(); showPage('registerPage'); });

document.getElementById('registerSubmitBtn').addEventListener('click', async () => {
    const btn = document.getElementById('registerSubmitBtn');
    const user = document.getElementById('regUser').value.replace(/\s+/g, '');
    const pass = document.getElementById('regPass').value.replace(/\s+/g, '');
    setError('registerError', '');
    if (!user || !pass) { setError('registerError', 'Please fill in all fields.'); return; }
    if (pass.length < 4) { setError('registerError', 'Password must be at least 4 characters.'); return; }

    btn.innerHTML = '<span class="loading-spinner"></span> Creating account…';
    btn.disabled = true;
    try {
        await register(user, pass);
        await login(user, pass);
        updateHeader();
        document.getElementById('regUser').value = '';
        document.getElementById('regPass').value = '';
        showPage('dashboardPage');
        renderDashboard();
        toast('Account created! Welcome, ' + currentUser + ' 🎉');
    } catch (err) {
        setError('registerError', err.message);
    } finally {
        btn.innerHTML = 'Create account';
        btn.disabled = false;
    }
});

['regUser','regPass'].forEach(id =>
    document.getElementById(id).addEventListener('keydown', e => {
        if (e.key === 'Enter') document.getElementById('registerSubmitBtn').click();
    })
);

document.getElementById('cancelRegisterBtn').addEventListener('click', e => { e.preventDefault(); showPage('welcomePage'); });
document.getElementById('switchToLogin').addEventListener('click', e => { e.preventDefault(); showPage('loginPage'); });

document.getElementById('newArticleBtn').addEventListener('click', () => {
    document.getElementById('addTitle').value = '';
    document.getElementById('addDate').value = new Date().toISOString().split('T')[0];
    document.getElementById('addContent').value = '';
    showPage('addArticlePage');
});

document.getElementById('addSubmitBtn').addEventListener('click', async () => {
    const btn = document.getElementById('addSubmitBtn');
    const title = document.getElementById('addTitle').value.trim();
    const date = document.getElementById('addDate').value;
    const content = document.getElementById('addContent').value.trim();
    if (!title || !date || !content) { toast('Please fill in all fields.', 'error'); return; }

    btn.innerHTML = '<span class="loading-spinner"></span> Publishing…';
    btn.disabled = true;
    try {
        const res = await apiFetch('POST', '/articles', { title, date, content, published: true });
        if (!res.ok) throw new Error('Failed to publish article.');
        showPage('dashboardPage');
        renderDashboard();
        toast('Article published!');
    } catch (err) {
        toast(err.message, 'error');
    } finally {
        btn.innerHTML = 'Publish article';
        btn.disabled = false;
    }
});

document.getElementById('backFromAdd').addEventListener('click', e => { e.preventDefault(); showPage('dashboardPage'); });
document.getElementById('cancelAddBtn').addEventListener('click', () => showPage('dashboardPage'));

document.getElementById('editSubmitBtn').addEventListener('click', async () => {
    const btn = document.getElementById('editSubmitBtn');
    const title = document.getElementById('editTitle').value.trim();
    const date = document.getElementById('editDate').value;
    const content = document.getElementById('editContent').value.trim();
    if (!title || !date || !content) { toast('Please fill in all fields.', 'error'); return; }

    btn.innerHTML = '<span class="loading-spinner"></span> Saving…';
    btn.disabled = true;
    try {
        const res = await apiFetch('PUT', `/articles/${editingId}`, { title, date, content, published: true });
        if (!res.ok) throw new Error('Failed to save changes.');
        editingId = null;
        showPage('dashboardPage');
        renderDashboard();
        toast('Article updated!');
    } catch (err) {
        toast(err.message, 'error');
    } finally {
        btn.innerHTML = 'Save changes';
        btn.disabled = false;
    }
});

document.getElementById('backFromEdit').addEventListener('click', e => { e.preventDefault(); showPage('dashboardPage'); });
document.getElementById('cancelEditBtn').addEventListener('click', () => { showPage('dashboardPage'); });

updateHeader();
if (authToken && currentUser) {
    showPage('dashboardPage');
    renderDashboard();
}
