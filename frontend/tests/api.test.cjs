const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');

function client(fetch, apiUrl) {
  const values = new Map();
  const context = vm.createContext({
    window: { DEVMETRICS_API_URL: apiUrl }, fetch,
    localStorage: {
      getItem: key => values.get(key) || null,
      setItem: (key, value) => values.set(key, value),
      removeItem: key => values.delete(key),
    },
    location: { pathname: '/activities.html' },
  });
  vm.runInContext(fs.readFileSync(require.resolve('../js/api.js'), 'utf8') + '\nthis.client = Api;', context);
  return context.client;
}

test('DELETE 204 completes without a client error', async () => {
  const api = client(async () => ({ status: 204, ok: true }));
  assert.equal(await api.del('/activities/1'), null);
});

test('uses the same origin and encodes query parameters', async () => {
  let url;
  const api = client(async target => {
    url = target;
    return { status: 200, ok: true, text: async () => JSON.stringify({ success: true, data: [] }) };
  });
  await api.get('/projects', { name: 'Java & SQL', empty: '' });
  assert.equal(url, '/api/v1/projects?name=Java%20%26%20SQL');
});

test('allows a custom API origin', async () => {
  let url;
  const api = client(async target => {
    url = target;
    return { status: 204, ok: true };
  }, 'https://api.example.com/api/v1');
  await api.del('/activities/1');
  assert.equal(url, 'https://api.example.com/api/v1/activities/1');
});

test('refreshes an expired access token and retries the original request', async () => {
  const calls = [];
  const api = client(async (url, options) => {
    calls.push({ url, options });
    if (url.endsWith('/auth/refresh')) {
      return { ok: true, json: async () => ({ success: true, data: { accessToken: 'new', refreshToken: 'rotated', user: { id: 1 } } }) };
    }
    if (options.headers.Authorization === 'Bearer old') {
      return { status: 401, ok: false, text: async () => '' };
    }
    return { status: 200, ok: true, text: async () => JSON.stringify({ success: true, data: { id: 1 } }) };
  });
  api.writeAuth({ accessToken: 'old', refreshToken: 'refresh', user: { id: 1 } });
  assert.equal((await api.get('/users/me')).id, 1);
  assert.equal(calls.length, 3);
  assert.equal(api.readAuth().refreshToken, 'rotated');
});
