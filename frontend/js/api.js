/**
 * DevMetrics — cliente HTTP central.
 *
 * Unica camada que conhece fetch, o token e o formato do envelope de resposta
 * ({ success, data, error, meta }). Toda outra parte do frontend chama Api.get/post/...
 * e recebe direto o "data" (ou lanca ApiError).
 */

const Api = (() => {
  const BASE_URL = window.DEVMETRICS_API_URL || "http://localhost:8080/api/v1";
  const STORAGE_KEY = "devmetrics.auth";

  let refreshPromise = null;

  class ApiError extends Error {
    constructor(code, message, status, details) {
      super(message || "Erro inesperado");
      this.code = code || "UNKNOWN";
      this.status = status || 0;
      this.details = details || [];
    }
  }

  function readAuth() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  function writeAuth(auth) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  }

  function clearAuth() {
    localStorage.removeItem(STORAGE_KEY);
  }

  function isAuthenticated() {
    return !!readAuth()?.accessToken;
  }

  function currentUser() {
    return readAuth()?.user || null;
  }

  async function doFetch(path, options, skipRefresh) {
    const auth = readAuth();
    const headers = Object.assign(
      { "Content-Type": "application/json" },
      options.headers || {}
    );
    if (auth?.accessToken) {
      headers.Authorization = `Bearer ${auth.accessToken}`;
    }

    let response;
    try {
      response = await fetch(BASE_URL + path, Object.assign({}, options, { headers }));
    } catch {
      throw new ApiError("NETWORK_ERROR", "Nao foi possivel conectar ao servidor.", 0);
    }

    if (response.status === 204) {
      return null;
    }

    let body = null;
    const text = await response.text();
    if (text) {
      try {
        body = JSON.parse(text);
      } catch {
        body = null;
      }
    }

    if (response.status === 401 && !skipRefresh && auth?.refreshToken) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        return doFetch(path, options, true);
      }
      clearAuth();
      if (!location.pathname.endsWith("login.html") && !location.pathname.endsWith("index.html")) {
        location.href = "login.html?expired=1";
      }
    }

    if (!response.ok || (body && body.success === false)) {
      const error = body?.error || {};
      throw new ApiError(error.code, error.message, response.status, error.details);
    }

    return { data: body?.data, meta: body?.meta };
  }

  async function refreshAccessToken() {
    if (refreshPromise) {
      return refreshPromise;
    }
    const auth = readAuth();
    if (!auth?.refreshToken) {
      return false;
    }
    refreshPromise = fetch(BASE_URL + "/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: auth.refreshToken }),
    })
      .then(async (response) => {
        if (!response.ok) {
          return false;
        }
        const body = await response.json();
        if (!body.success) {
          return false;
        }
        writeAuth(Object.assign({}, auth, {
          accessToken: body.data.accessToken,
          refreshToken: body.data.refreshToken,
          user: body.data.user,
        }));
        return true;
      })
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
    return refreshPromise;
  }

  function buildQuery(params) {
    if (!params) return "";
    const usable = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== "");
    if (usable.length === 0) return "";
    return "?" + usable.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join("&");
  }

  return {
    ApiError,
    isAuthenticated,
    currentUser,
    clearAuth,
    writeAuth,
    readAuth,

    async get(path, params) {
      return (await doFetch(path + buildQuery(params), { method: "GET" })).data;
    },
    async getPaged(path, params) {
      return doFetch(path + buildQuery(params), { method: "GET" });
    },
    async post(path, body) {
      return (await doFetch(path, { method: "POST", body: body ? JSON.stringify(body) : undefined })).data;
    },
    async put(path, body) {
      return (await doFetch(path, { method: "PUT", body: body ? JSON.stringify(body) : undefined })).data;
    },
    async patch(path, body) {
      return (await doFetch(path, { method: "PATCH", body: body ? JSON.stringify(body) : undefined })).data;
    },
    async del(path) {
      return (await doFetch(path, { method: "DELETE" })).data;
    },

    async login(email, password) {
      const data = await this.post("/auth/login", { email, password });
      writeAuth(data);
      return data;
    },
    async register(payload) {
      const data = await this.post("/auth/register", payload);
      writeAuth(data);
      return data;
    },
    async logout() {
      const auth = readAuth();
      try {
        if (auth?.refreshToken) {
          await this.post("/auth/logout", { refreshToken: auth.refreshToken });
        }
      } catch {
        // segue o logout mesmo se a chamada falhar
      }
      clearAuth();
    },
  };
})();

function requireAuth() {
  if (!Api.isAuthenticated()) {
    location.href = "login.html";
  }
}
