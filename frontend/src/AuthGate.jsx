import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import {
  CheckCircle2,
  Loader2,
  Lock,
  LogOut,
  ShieldCheck,
  User,
  CircleUserRound,
} from "lucide-react";

const AUTH_STORAGE_KEYS = {
  token: "conference_parser_auth_token",
  refreshToken: "conference_parser_auth_refresh_token",
  tokenType: "conference_parser_auth_token_type",
  username: "conference_parser_auth_username",
};

const AUTH_EXPIRED_EVENT = "conference-parser-auth-expired";
const AUTH_UPDATED_EVENT = "conference-parser-auth-updated";

// Базовый путь для служебных запросов авторизации (refresh/logout).
const AUTH_API_BASE = "/api/auth";

const AuthContext = createContext(null);

function safeReadStorage(key) {
  try {
    return window.localStorage.getItem(key) || "";
  } catch {
    return "";
  }
}

function safeWriteStorage(key, value) {
  try {
    if (!value) {
      window.localStorage.removeItem(key);
    } else {
      window.localStorage.setItem(key, value);
    }
  } catch {
    // ignore storage errors
  }
}

function normalizeAuthPayload(payload = {}) {
  const token = String(payload.accessToken || payload.token || "").trim();
  const refreshToken = String(payload.refreshToken || payload.refresh_token || "").trim();
  const tokenType = String(payload.tokenType || payload.token_type || "Bearer").trim() || "Bearer";
  const username = String(payload.username || "").trim();

  return { token, refreshToken, tokenType, username };
}

export function getStoredAuth() {
  return normalizeAuthPayload({
    token: safeReadStorage(AUTH_STORAGE_KEYS.token),
    refreshToken: safeReadStorage(AUTH_STORAGE_KEYS.refreshToken),
    tokenType: safeReadStorage(AUTH_STORAGE_KEYS.tokenType),
    username: safeReadStorage(AUTH_STORAGE_KEYS.username),
  });
}

export function clearStoredAuth() {
  safeWriteStorage(AUTH_STORAGE_KEYS.token, "");
  safeWriteStorage(AUTH_STORAGE_KEYS.refreshToken, "");
  safeWriteStorage(AUTH_STORAGE_KEYS.tokenType, "");
  safeWriteStorage(AUTH_STORAGE_KEYS.username, "");
}

export function storeAuth(auth) {
  const normalized = normalizeAuthPayload(auth);
  safeWriteStorage(AUTH_STORAGE_KEYS.token, normalized.token);
  safeWriteStorage(AUTH_STORAGE_KEYS.refreshToken, normalized.refreshToken);
  safeWriteStorage(AUTH_STORAGE_KEYS.tokenType, normalized.tokenType);
  safeWriteStorage(AUTH_STORAGE_KEYS.username, normalized.username);
  return normalized;
}

export function buildAuthHeaders(extraHeaders = {}) {
  const auth = getStoredAuth();
  if (!auth.token) {
    return { ...extraHeaders };
  }

  return {
    ...extraHeaders,
    Authorization: `${auth.tokenType} ${auth.token}`,
  };
}

function notifyAuthExpired(detail) {
  clearStoredAuth();

  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT, { detail }));
  }
}

// Одновременно выполняется не более одного запроса на обновление токенов:
// на бэкенде включена ротация, поэтому параллельные вызовы отозвали бы друг друга.
let refreshPromise = null;

async function requestTokenRefresh() {
  const current = getStoredAuth();
  if (!current.refreshToken) {
    return null;
  }

  try {
    const response = await fetch(`${AUTH_API_BASE}/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ refreshToken: current.refreshToken }),
    });

    if (!response.ok) {
      return null;
    }

    const data = await response.json().catch(() => null);
    if (!data?.accessToken) {
      return null;
    }

    // Бэкенд возвращает новую пару токенов (старый refresh уже отозван).
    const normalized = storeAuth({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      tokenType: data.tokenType || current.tokenType || "Bearer",
      username: current.username,
    });

    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT, { detail: normalized }));
    }

    return normalized;
  } catch {
    return null;
  }
}

export function ensureTokenRefresh() {
  if (!refreshPromise) {
    refreshPromise = requestTokenRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

export async function requestLogout() {
  const { refreshToken } = getStoredAuth();
  if (!refreshToken) {
    return;
  }

  try {
    // Отзываем refresh на сервере; результат не критичен для выхода из интерфейса.
    await fetch(`${AUTH_API_BASE}/logout`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ refreshToken }),
    });
  } catch {
    // ignore network errors on logout
  }
}

export async function authFetch(url, options = {}) {
  const doFetch = () =>
    fetch(url, {
      ...options,
      headers: buildAuthHeaders(options.headers || {}),
    });

  let response = await doFetch();

  if (response.status === 401 || response.status === 403) {
    const refreshed = await ensureTokenRefresh();

    if (refreshed?.token) {
      // Повторяем исходный запрос уже с новым access-токеном.
      response = await doFetch();

      if (response.status !== 401 && response.status !== 403) {
        return response;
      }
    }

    notifyAuthExpired({ status: response.status, url });
  }

  return response;
}

function parseErrorText(text) {
  const raw = String(text || "").trim();
  if (!raw) {
    return "Не удалось выполнить запрос.";
  }

  try {
    const json = JSON.parse(raw);
    return json.message || json.error || json.details || raw;
  } catch {
    return raw;
  }
}

function Field({ icon: Icon, label, type = "text", value, onChange, placeholder, autoComplete }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-medium text-slate-700">{label}</span>
      <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm transition focus-within:border-slate-400">
        <Icon className="h-4 w-4 shrink-0 text-slate-400" />
        <input
          type={type}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          autoComplete={autoComplete}
          className="w-full bg-transparent text-sm text-slate-900 outline-none placeholder:text-slate-400"
        />
      </div>
    </label>
  );
}

function AuthCard({
  mode,
  setMode,
  username,
  setUsername,
  password,
  setPassword,
  confirmPassword,
  setConfirmPassword,
  loading,
  error,
  success,
  onSubmit,
}) {
  const isRegister = mode === "register";

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
      className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"
    >
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-950">
          {isRegister ? "Создание аккаунта" : "Вход в систему"}
        </h1>
      </div>

      <div className="mb-5 grid grid-cols-2 rounded-2xl border border-slate-200 bg-slate-50 p-1">
        <button
          type="button"
          onClick={() => {
            setMode("login");
            setError("");
          }}
          className={`rounded-xl px-3 py-2 text-sm transition ${
            !isRegister ? "bg-white text-slate-950 shadow-sm" : "text-slate-500 hover:text-slate-700"
          }`}
        >
          Вход
        </button>
        <button
          type="button"
          onClick={() => {
            setMode("register");
            setError("");
            setSuccess("");
          }}
          className={`rounded-xl px-3 py-2 text-sm transition ${
            isRegister ? "bg-white text-slate-950 shadow-sm" : "text-slate-500 hover:text-slate-700"
          }`}
        >
          Регистрация
        </button>
      </div>

      <form
        className="space-y-4"
        onSubmit={(e) => {
          e.preventDefault();
          onSubmit();
        }}
      >
        <Field
          icon={User}
          label="Имя пользователя"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Введите username"
          autoComplete="username"
        />

        <Field
          icon={Lock}
          label="Пароль"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Введите пароль"
          autoComplete={isRegister ? "new-password" : "current-password"}
        />

        <AnimatePresence initial={false}>
          {isRegister && (
            <motion.div
              key="confirm-password"
              initial={{ opacity: 0, height: 0, y: -4 }}
              animate={{ opacity: 1, height: "auto", y: 0 }}
              exit={{ opacity: 0, height: 0, y: -4 }}
              transition={{ duration: 0.2 }}
            >
              <Field
                icon={Lock}
                label="Повторите пароль"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Повторите пароль"
                autoComplete="new-password"
              />
            </motion.div>
          )}
        </AnimatePresence>

        {error ? (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 whitespace-pre-wrap break-words">
            {error}
          </div>
        ) : null}

        {success ? (
          <div className="flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
            <div className="whitespace-pre-wrap break-words">{success}</div>
          </div>
        ) : null}

        <button
          type="submit"
          disabled={loading}
          className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              {isRegister ? "Создание аккаунта..." : "Вход..."}
            </>
          ) : (
            <>
              <ShieldCheck className="h-4 w-4" />
              {isRegister ? "Зарегистрироваться" : "Войти"}
            </>
          )}
        </button>
      </form>
    </motion.div>
  );
}

function AuthOverlay({ children, apiBase = "/api/auth", onOpenProfile }) {
  const [mode, setMode] = useState("login");
  const [auth, setAuth] = useState(() => getStoredAuth());
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [bootChecking, setBootChecking] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    setBootChecking(false);
  }, []);

  useEffect(() => {
    const handleAuthExpired = () => {
      clearStoredAuth();
      setAuth(getStoredAuth());
      setMode("login");
      setPassword("");
      setConfirmPassword("");
      setSuccess("");
      setError("Сессия истекла или токен больше недействителен. Войдите в систему снова.");
    };

    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
  }, []);

  useEffect(() => {
    // Токены могли обновиться в фоне (authFetch) — подтягиваем актуальные значения.
    const handleAuthUpdated = () => setAuth(getStoredAuth());

    window.addEventListener(AUTH_UPDATED_EVENT, handleAuthUpdated);
    return () => window.removeEventListener(AUTH_UPDATED_EVENT, handleAuthUpdated);
  }, []);

  const isAuthenticated = Boolean(auth.token);

  const value = useMemo(
    () => ({
      auth,
      isAuthenticated,
      logout: async () => {
        await requestLogout();
        clearStoredAuth();
        setAuth(getStoredAuth());
      },
      setAuth: (nextAuth) => {
        const normalized = storeAuth(nextAuth);
        setAuth(normalized);
      },
    }),
    [auth, isAuthenticated]
  );

  async function handleSubmit() {
    setError("");
    setSuccess("");

    const trimmedUsername = username.trim();
    const trimmedPassword = password;

    if (!trimmedUsername || !trimmedPassword) {
      setError("Укажите имя пользователя и пароль.");
      return;
    }

    if (mode === "register") {
      if (trimmedPassword !== confirmPassword) {
        setError("Пароли не совпадают.");
        return;
      }
      if (trimmedPassword.length < 4) {
        setError("Пароль должен содержать хотя бы 4 символа.");
        return;
      }
    }

    setLoading(true);

    try {
      if (mode === "register") {
        const registerResponse = await fetch(`${apiBase}/register`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
          body: JSON.stringify({
            username: trimmedUsername,
            password: trimmedPassword,
          }),
        });

        if (!registerResponse.ok) {
          const text = await registerResponse.text();
          throw new Error(parseErrorText(text));
        }

        const registerData = await registerResponse.json();
        const registeredUsername = String(registerData?.username || trimmedUsername).trim() || trimmedUsername;
        const registerMessage =
          String(registerData?.message || "").trim() || "Пользователь успешно зарегистрирован.";

        setMode("login");
        setUsername(registeredUsername);
        setPassword("");
        setConfirmPassword("");
        setSuccess(`${registerMessage}\nВойдите в систему под созданной учётной записью.`);
        return;
      }

      const loginResponse = await fetch(`${apiBase}/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({
          username: trimmedUsername,
          password: trimmedPassword,
        }),
      });

      if (!loginResponse.ok) {
        const text = await loginResponse.text();
        throw new Error(parseErrorText(text));
      }

      const data = await loginResponse.json();
      const normalized = storeAuth({
        accessToken: data?.accessToken,
        refreshToken: data?.refreshToken,
        tokenType: data?.tokenType || "Bearer",
        username: trimmedUsername,
      });

      setAuth(normalized);
      setPassword("");
      setConfirmPassword("");
      setSuccess("");
    } catch (err) {
      setError(err?.message || (mode === "register" ? "Не удалось выполнить регистрацию." : "Не удалось выполнить авторизацию."));
    } finally {
      setLoading(false);
    }
  }

  if (bootChecking) {
    return (
      <div className="min-h-screen bg-slate-50 text-slate-900">
        <div className="mx-auto flex min-h-screen max-w-7xl items-center justify-center px-4 py-8 sm:px-6 lg:px-8">
          <div className="rounded-3xl border border-slate-200 bg-white px-6 py-5 shadow-sm">
            <div className="inline-flex items-center gap-3 text-sm text-slate-700">
              <Loader2 className="h-4 w-4 animate-spin" />
              Проверяем авторизацию...
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-slate-50 text-slate-900">
        <div className="mx-auto flex min-h-screen max-w-7xl items-center justify-center px-4 py-8 sm:px-6 lg:px-8">
          <AuthCard
            mode={mode}
            setMode={setMode}
            username={username}
            setUsername={setUsername}
            password={password}
            setPassword={setPassword}
            confirmPassword={confirmPassword}
            setConfirmPassword={setConfirmPassword}
            loading={loading}
            error={error}
            success={success}
            onSubmit={handleSubmit}
          />
        </div>
      </div>
    );
  }

  return (
    <AuthContext.Provider value={value}>
      <div className="relative">
        <div className="pointer-events-none fixed right-4 top-4 z-40">
          <div className="pointer-events-auto flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
            <div className="rounded-xl bg-emerald-50 p-2">
              <CheckCircle2 className="h-4 w-4 text-emerald-600" />
            </div>
            <div className="min-w-0">
              <div className="text-xs text-slate-500">Авторизован как</div>
              <div className="max-w-[180px] truncate text-sm font-medium text-slate-900">
                {auth.username || "Пользователь"}
              </div>
            </div>
            {typeof onOpenProfile === "function" ? (
              <button
                type="button"
                onClick={onOpenProfile}
                className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-700"
                aria-label="Открыть профиль"
                title="Профиль"
              >
                <CircleUserRound className="h-4 w-4" />
              </button>
            ) : null}
            <button
              type="button"
              onClick={async () => {
                await requestLogout();
                clearStoredAuth();
                setAuth(getStoredAuth());
              }}
              className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-700"
              aria-label="Выйти"
              title="Выйти"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>

        {children}
      </div>
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used inside <AuthGate />");
  }
  return value;
}

export default function AuthGate({ children, apiBase = "/api/auth", onOpenProfile }) {
  return <AuthOverlay apiBase={apiBase} onOpenProfile={onOpenProfile}>{children}</AuthOverlay>;
}