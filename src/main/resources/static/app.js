const state = {
    users: [],
    lots: [],
    auctions: []
};

const elements = {
    notice: document.querySelector('#notice'),
    health: document.querySelector('#health'),
    usersList: document.querySelector('#users-list'),
    lotsTable: document.querySelector('#lots-table'),
    auctionsList: document.querySelector('#auctions-list'),
    lotSeller: document.querySelector('#lot-seller'),
    auctionLot: document.querySelector('#auction-lot'),
    bidAuction: document.querySelector('#bid-auction'),
    bidder: document.querySelector('#bidder')
};

async function api(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        }
    });

    if (response.status === 204) {
        return null;
    }

    const body = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(body?.message || `HTTP ${response.status}`);
    }
    return body;
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function showNotice(message, error = false) {
    elements.notice.textContent = message;
    elements.notice.classList.toggle('error', error);
    elements.notice.hidden = false;
    window.clearTimeout(showNotice.timer);
    showNotice.timer = window.setTimeout(() => {
        elements.notice.hidden = true;
    }, 4_000);
}

function userName(id) {
    return state.users.find(user => user.id === id)?.username || id?.slice(0, 8) || '—';
}

function lotName(id) {
    return state.lots.find(lot => lot.id === id)?.title || id?.slice(0, 8) || 'Лот';
}

function formatDate(value) {
    return new Intl.DateTimeFormat('ru-RU', {
        dateStyle: 'short',
        timeStyle: 'short'
    }).format(new Date(value));
}

function formatMoney(minor, currency) {
    return new Intl.NumberFormat('ru-RU', {
        style: 'currency',
        currency
    }).format(minor / 100);
}

function setOptions(select, items, label, currentValue) {
    const options = items.map(item =>
        `<option value="${escapeHtml(item.id)}">${escapeHtml(label(item))}</option>`
    ).join('');
    select.innerHTML = options || '<option value="">Нет доступных вариантов</option>';
    select.disabled = items.length === 0;
    if (currentValue && items.some(item => item.id === currentValue)) {
        select.value = currentValue;
    }
}

function renderUsers() {
    elements.usersList.innerHTML = state.users.length
        ? state.users.map(user => `<span class="chip">${escapeHtml(user.username)}</span>`).join('')
        : '<span class="empty">Пользователей пока нет</span>';

    setOptions(elements.lotSeller, state.users, user => user.username, elements.lotSeller.value);
    setOptions(elements.bidder, state.users, user => user.username, elements.bidder.value);
}

function renderLots() {
    elements.lotsTable.innerHTML = state.lots.length
        ? state.lots.map(lot => `
            <tr>
                <td>
                    <strong>${escapeHtml(lot.title)}</strong>
                    <div class="muted">${escapeHtml(lot.description || 'Без описания')}</div>
                </td>
                <td>${escapeHtml(userName(lot.sellerId))}</td>
                <td>${escapeHtml(formatDate(lot.createdAt))}</td>
                <td>
                    <div class="row-actions">
                        <button type="button" class="secondary" data-action="edit" data-id="${lot.id}">Изменить</button>
                        <button type="button" class="danger" data-action="delete" data-id="${lot.id}">Удалить</button>
                    </div>
                </td>
            </tr>
        `).join('')
        : '<tr><td colspan="4" class="empty">Лотов пока нет</td></tr>';

    setOptions(elements.auctionLot, state.lots, lot => lot.title, elements.auctionLot.value);
}

function renderAuctions() {
    elements.auctionsList.innerHTML = state.auctions.length
        ? state.auctions.map(auction => `
            <article class="auction-card">
                <header>
                    <h3>${escapeHtml(lotName(auction.lotId))}</h3>
                    <span class="status ${auction.status.toLowerCase()}">${escapeHtml(auction.status)}</span>
                </header>
                <p class="auction-price">${escapeHtml(formatMoney(auction.currentPriceMinor, auction.currency))}</p>
                <div class="muted">Шаг: ${escapeHtml(formatMoney(auction.minIncrementMinor, auction.currency))}</div>
                <div class="muted">Окончание: ${escapeHtml(formatDate(auction.endsAt))}</div>
                <div class="muted">Лидер: ${escapeHtml(userName(auction.currentLeaderId))}</div>
            </article>
        `).join('')
        : '<p class="empty">Аукционов пока нет</p>';

    const active = state.auctions.filter(auction => auction.status === 'ACTIVE');
    setOptions(
        elements.bidAuction,
        active,
        auction => `${lotName(auction.lotId)} — ${formatMoney(auction.currentPriceMinor, auction.currency)}`,
        elements.bidAuction.value
    );
    syncMinimumBid();
}

async function loadAll() {
    const [users, lots, auctions] = await Promise.all([
        api('/api/v1/users'),
        api('/api/v1/lots'),
        api('/api/v1/auctions')
    ]);
    state.users = users;
    state.lots = lots;
    state.auctions = auctions;
    renderUsers();
    renderLots();
    renderAuctions();
}

async function refreshAuctions() {
    state.auctions = await api('/api/v1/auctions');
    renderAuctions();
}

function resetLotForm() {
    document.querySelector('#lot-form').reset();
    document.querySelector('#lot-id').value = '';
    document.querySelector('#lot-submit').textContent = 'Создать лот';
    document.querySelector('#lot-cancel').hidden = true;
    elements.lotSeller.disabled = state.users.length === 0;
    renderUsers();
}

function startLotEdit(lot) {
    document.querySelector('#lot-id').value = lot.id;
    elements.lotSeller.value = lot.sellerId;
    elements.lotSeller.disabled = true;
    document.querySelector('#lot-title').value = lot.title;
    document.querySelector('#lot-description').value = lot.description || '';
    document.querySelector('#lot-image').value = lot.imageUrl || '';
    document.querySelector('#lot-submit').textContent = 'Сохранить изменения';
    document.querySelector('#lot-cancel').hidden = false;
    document.querySelector('#lot-form').scrollIntoView({behavior: 'smooth', block: 'center'});
}

function syncMinimumBid() {
    const auction = state.auctions.find(item => item.id === elements.bidAuction.value);
    const input = document.querySelector('#bid-amount');
    if (!auction) {
        input.value = '';
        input.disabled = true;
        return;
    }
    const minimum = auction.currentPriceMinor + auction.minIncrementMinor;
    input.min = String(minimum);
    input.value = String(minimum);
    input.disabled = false;
}

document.querySelector('#user-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        await api('/api/v1/users', {
            method: 'POST',
            body: JSON.stringify({username: document.querySelector('#username').value})
        });
        form.reset();
        await loadAll();
        showNotice('Пользователь создан');
    } catch (error) {
        showNotice(error.message, true);
    }
});

document.querySelector('#lot-form').addEventListener('submit', async event => {
    event.preventDefault();
    const id = document.querySelector('#lot-id').value;
    const payload = {
        sellerId: elements.lotSeller.value,
        title: document.querySelector('#lot-title').value,
        description: document.querySelector('#lot-description').value || null,
        imageUrl: document.querySelector('#lot-image').value || null
    };

    try {
        await api(id ? `/api/v1/lots/${id}` : '/api/v1/lots', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(payload)
        });
        resetLotForm();
        await loadAll();
        showNotice(id ? 'Лот обновлён' : 'Лот создан');
    } catch (error) {
        showNotice(error.message, true);
    }
});

elements.lotsTable.addEventListener('click', async event => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const lot = state.lots.find(item => item.id === button.dataset.id);
    if (!lot) return;

    if (button.dataset.action === 'edit') {
        startLotEdit(lot);
        return;
    }

    if (!window.confirm(`Удалить лот «${lot.title}»?`)) return;
    try {
        await api(`/api/v1/lots/${lot.id}?sellerId=${encodeURIComponent(lot.sellerId)}`, {
            method: 'DELETE'
        });
        await loadAll();
        showNotice('Лот удалён');
    } catch (error) {
        showNotice(error.message, true);
    }
});

document.querySelector('#auction-form').addEventListener('submit', async event => {
    event.preventDefault();
    const lot = state.lots.find(item => item.id === elements.auctionLot.value);
    if (!lot) return;
    const delayMinutes = Number(document.querySelector('#start-delay').value);
    const durationMinutes = Number(document.querySelector('#duration').value);
    const startsAt = new Date(Date.now() + delayMinutes * 60_000);
    const endsAt = new Date(startsAt.getTime() + durationMinutes * 60_000);

    try {
        await api('/api/v1/auctions', {
            method: 'POST',
            body: JSON.stringify({
                lotId: lot.id,
                sellerId: lot.sellerId,
                currency: 'RUB',
                startPriceMinor: Number(document.querySelector('#start-price').value),
                minIncrementMinor: Number(document.querySelector('#min-increment').value),
                startsAt: startsAt.toISOString(),
                endsAt: endsAt.toISOString()
            })
        });
        await loadAll();
        showNotice('Аукцион создан');
    } catch (error) {
        showNotice(error.message, true);
    }
});

document.querySelector('#bid-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
        await api(`/api/v1/auctions/${elements.bidAuction.value}/bids`, {
            method: 'POST',
            body: JSON.stringify({
                bidderId: elements.bidder.value,
                amountMinor: Number(document.querySelector('#bid-amount').value),
                idempotencyKey: crypto.randomUUID()
            })
        });
        await refreshAuctions();
        showNotice('Ставка принята');
    } catch (error) {
        showNotice(error.message, true);
    }
});

document.querySelector('#lot-cancel').addEventListener('click', resetLotForm);
document.querySelector('#refresh').addEventListener('click', async () => {
    try {
        await loadAll();
        showNotice('Данные обновлены');
    } catch (error) {
        showNotice(error.message, true);
    }
});
elements.bidAuction.addEventListener('change', syncMinimumBid);

async function initialize() {
    try {
        const health = await api('/actuator/health');
        elements.health.classList.add('is-up');
        elements.health.querySelector('span:last-child').textContent = `API ${health.status}`;
        await loadAll();
    } catch (error) {
        elements.health.classList.add('is-down');
        elements.health.querySelector('span:last-child').textContent = 'API недоступно';
        showNotice(error.message, true);
    }
}

initialize();
window.setInterval(() => refreshAuctions().catch(() => {}), 5_000);
