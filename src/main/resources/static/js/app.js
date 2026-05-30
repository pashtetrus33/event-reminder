// ── Timezone helpers ─────────────────────────────────────────
// Convert datetime-local input (local time) → UTC ISO before submit
document.addEventListener('DOMContentLoaded', () => {
    // Show client timezone and populate hidden field
    const clientTz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const tzSpan = document.getElementById('clientTimezone');
    if (tzSpan) tzSpan.textContent = clientTz;
    const tzField = document.getElementById('timezoneField');
    if (tzField && !tzField.value) tzField.value = clientTz;

    // Pre-fill empty datetime-local inputs with current local time;
    // convert pre-filled UTC values (from server on edit) to local time
    const pad = n => String(n).padStart(2, '0');
    const toLocalInputValue = d =>
        `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    document.querySelectorAll('input[type="datetime-local"]').forEach(input => {
        if (!input.value) {
            input.value = toLocalInputValue(new Date());
        } else {
            input.value = toLocalInputValue(new Date(input.value + 'Z'));
        }
    });

    // On form submit: convert datetime-local values (local → UTC)
    document.querySelectorAll('form[method="post"]').forEach(form => {
        form.addEventListener('submit', () => {
            form.querySelectorAll('input[type="datetime-local"]').forEach(input => {
                if (input.value) {
                    const local = new Date(input.value);
                    input.value = local.toISOString().slice(0, 16);
                }
            });
        });
    });

    // Convert UTC date displays to local time
    document.querySelectorAll('[data-utc]').forEach(el => {
        const utc = el.dataset.utc;
        if (utc) {
            const d = new Date(utc);
            el.textContent = d.toLocaleString('ru-RU', {
                day: '2-digit', month: '2-digit', year: 'numeric',
                hour: '2-digit', minute: '2-digit'
            });
        }
    });
});

// ── Event delete ─────────────────────────────────────────────
function deleteEvent(btn) {
    const id = btn.dataset.eventId;
    if (!confirm('Удалить событие?')) return;
    fetch('/events/' + id, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': getCsrfToken() } })
        .then(r => r.ok ? location.reload() : alert('Ошибка удаления'));
}

// ── Label CRUD ────────────────────────────────────────────────
let editingLabelId = null;

function editLabel(btn) {
    editingLabelId = btn.dataset.labelId;
    document.getElementById('editLabelName').value = btn.dataset.labelName;
    document.getElementById('editLabelColor').value = btn.dataset.labelColor;
    new bootstrap.Modal(document.getElementById('editLabelModal')).show();
}

function saveLabelEdit() {
    const name = document.getElementById('editLabelName').value;
    const color = document.getElementById('editLabelColor').value;
    fetch('/labels/' + editingLabelId, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': getCsrfToken() },
        body: JSON.stringify({ name, color })
    }).then(r => r.ok ? location.reload() : alert('Ошибка сохранения'));
}

function deleteLabel(btn) {
    const id = btn.dataset.labelId;
    if (!confirm('Удалить метку?')) return;
    fetch('/labels/' + id, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': getCsrfToken() } })
        .then(r => r.ok ? location.reload() : alert('Ошибка удаления'));
}

// ── Event form helpers ────────────────────────────────────────
function toggleRecurrence(enabled) {
    document.getElementById('recurrenceFields').classList.toggle('d-none', !enabled);
}

function addRuleRow() {
    document.getElementById('noRulesHint')?.remove();
    const container = document.getElementById('rulesContainer');
    const row = document.createElement('div');
    row.className = 'rule-row d-flex gap-2 align-items-center mb-2';
    row.innerHTML = `
        <select name="ruleChannels" class="form-select form-select-sm flex-grow-0 w-auto">
            <option value="EMAIL">Email</option>
            <option value="TELEGRAM">Telegram</option>
            <option value="BOTH">Email + Telegram</option>
        </select>
        <select name="ruleNotifyTypes" class="form-select form-select-sm flex-grow-0 w-auto" onchange="toggleMinutes(this)">
            <option value="BEFORE">за время до</option>
            <option value="START_OF_DAY">в начале дня</option>
        </select>
        <input type="number" name="ruleMinutes" class="form-control form-control-sm"
               style="width:90px" placeholder="мин" min="0" value="0">
        <span class="text-muted small text-nowrap">повт. каждые</span>
        <input type="number" name="ruleRepeatIntervals" class="form-control form-control-sm"
               style="width:80px" placeholder="мин" min="1">
        <button type="button" class="btn btn-sm btn-outline-danger"
                onclick="this.closest('.rule-row').remove(); checkNoRules()">
            <i class="bi bi-x"></i>
        </button>`;
    container.appendChild(row);
}

function toggleMinutes(select) {
    const row = select.closest('.rule-row');
    const hide = select.value === 'START_OF_DAY';
    row.querySelectorAll('input[name="ruleMinutes"], input[name="ruleRepeatIntervals"], .text-nowrap')
       .forEach(el => el.classList.toggle('d-none', hide));
}

function checkNoRules() {
    const container = document.getElementById('rulesContainer');
    if (!container.querySelector('.rule-row')) {
        const hint = document.createElement('div');
        hint.id = 'noRulesHint';
        hint.className = 'text-muted small';
        hint.textContent = 'Нет напоминаний. Нажми «Добавить», чтобы настроить.';
        container.appendChild(hint);
    }
}

// ── CSRF token helper ─────────────────────────────────────────
function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    if (meta) return meta.content;
    // fallback: read from cookie
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? match[1] : '';
}

// ── Auto-dismiss alerts + active nav ─────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.alert-success').forEach(el => {
        setTimeout(() => bootstrap.Alert.getOrCreateInstance(el).close(), 4000);
    });

    const path = window.location.pathname;
    document.querySelectorAll('[data-nav-path]').forEach(link => {
        const nav = link.dataset.navPath;
        if (nav === '/' ? path === '/' : path.startsWith(nav)) {
            link.classList.add('active');
        }
    });
});
