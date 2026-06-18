import React, { useEffect, useRef, useState } from "react";
import {
  Check,
  Loader2,
  Pencil,
  RefreshCw,
  Save,
  Trash2,
  X,
} from "lucide-react";
import { authFetch } from "./AuthGate.jsx";

function parseErrorText(text) {
  const raw = String(text || "").trim();
  if (!raw) {
    return "";
  }

  try {
    const json = JSON.parse(raw);
    return json.message || json.error || json.details || raw;
  } catch {
    return raw;
  }
}

export default function ParamsPresets({ buildParams, applyParams }) {
  const [names, setNames] = useState([]);
  const [selectedName, setSelectedName] = useState("");
  const [saveName, setSaveName] = useState("");

  const [renaming, setRenaming] = useState(false);
  const [renameValue, setRenameValue] = useState("");

  const [namesLoading, setNamesLoading] = useState(false);
  const [loadingParam, setLoadingParam] = useState(false);
  const [saving, setSaving] = useState(false);
  const [renameLoading, setRenameLoading] = useState(false);
  const [deletingName, setDeletingName] = useState("");

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const initialLoadDoneRef = useRef(false);

  useEffect(() => {
    if (initialLoadDoneRef.current) {
      return;
    }
    initialLoadDoneRef.current = true;
    loadNames();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadNames() {
    setNamesLoading(true);
    setError("");

    try {
      const response = await authFetch("/api/params/names", {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        cache: "no-store",
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(parseErrorText(text) || `Не удалось получить список параметров: ${response.status}`);
      }

      const data = await response.json();
      const list = Array.isArray(data) ? data.map((name) => String(name)) : [];

      setNames(list);
      setSelectedName((prev) => (prev && list.includes(prev) ? prev : ""));
    } catch (e) {
      setError(e.message || "Ошибка при загрузке списка параметров.");
    } finally {
      setNamesLoading(false);
    }
  }

  async function handleSelect(name) {
    setMessage("");
    setError("");
    setRenaming(false);
    setRenameValue("");
    setSelectedName(name);

    if (!name) {
      return;
    }

    setLoadingParam(true);

    try {
      const response = await authFetch(`/api/params/fullParam/${encodeURIComponent(name)}`, {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        cache: "no-store",
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(parseErrorText(text) || `Не удалось загрузить параметр: ${response.status}`);
      }

      const data = await response.json();
      applyParams?.(data);
      setMessage(`Набор «${name}» подставлен в форму.`);
    } catch (e) {
      setError(e.message || "Ошибка при загрузке параметра.");
    } finally {
      setLoadingParam(false);
    }
  }

  async function handleSave() {
    const name = saveName.trim();
    setMessage("");
    setError("");

    if (!name) {
      setError("Укажите название для сохраняемого набора параметров.");
      return;
    }

    setSaving(true);

    try {
      const payload = { name, ...(buildParams?.() || {}) };

      const response = await authFetch("/api/params/create", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(parseErrorText(text) || `Не удалось сохранить параметры: ${response.status}`);
      }

      setSaveName("");
      await loadNames();
      setSelectedName(name);
      setMessage(`Параметры сохранены как «${name}».`);
    } catch (e) {
      setError(e.message || "Ошибка при сохранении параметров.");
    } finally {
      setSaving(false);
    }
  }

  function startRename() {
    setError("");
    setMessage("");
    setRenameValue(selectedName);
    setRenaming(true);
  }

  function cancelRename() {
    setRenaming(false);
    setRenameValue("");
  }

  async function handleConfirmRename() {
    const oldName = selectedName;
    const newName = renameValue.trim();

    setError("");
    setMessage("");

    if (!oldName) {
      return;
    }

    if (!newName) {
      setError("Укажите новое название набора параметров.");
      return;
    }

    if (newName === oldName) {
      setRenaming(false);
      return;
    }

    setRenameLoading(true);

    try {
      const response = await authFetch("/api/params/rename", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({ oldName, newName }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(parseErrorText(text) || `Не удалось переименовать набор: ${response.status}`);
      }

      setRenaming(false);
      setRenameValue("");
      await loadNames();
      setSelectedName(newName);
      setMessage(`Набор переименован в «${newName}».`);
    } catch (e) {
      setError(e.message || "Ошибка при переименовании набора.");
    } finally {
      setRenameLoading(false);
    }
  }

  async function handleDelete() {
    const name = selectedName;
    if (!name) {
      return;
    }

    const confirmed = window.confirm(`Удалить набор параметров «${name}»?`);
    if (!confirmed) {
      return;
    }

    setError("");
    setMessage("");
    setDeletingName(name);

    try {
      const response = await authFetch(`/api/params/${encodeURIComponent(name)}`, {
        method: "DELETE",
        headers: {
          Accept: "application/json, text/plain, */*",
        },
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(parseErrorText(text) || `Не удалось удалить набор: ${response.status}`);
      }

      setRenaming(false);
      setRenameValue("");
      setSelectedName("");
      await loadNames();
      setMessage(`Набор «${name}» удалён.`);
    } catch (e) {
      setError(e.message || "Ошибка при удалении набора.");
    } finally {
      setDeletingName("");
    }
  }

  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto]">
        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">Сохранённый набор параметров</span>
          <div className="relative">
            <select
              value={selectedName}
              onChange={(e) => handleSelect(e.target.value)}
              disabled={namesLoading || loadingParam}
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <option value="">{names.length ? "— выберите набор —" : "Нет сохранённых наборов"}</option>
              {names.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
            {loadingParam ? (
              <Loader2 className="pointer-events-none absolute right-10 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-slate-400" />
            ) : null}
          </div>
        </label>

        <div className="flex items-end">
          <button
            type="button"
            onClick={loadNames}
            disabled={namesLoading}
            className="inline-flex h-[50px] w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
          >
            {namesLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
            Обновить
          </button>
        </div>
      </div>

      {selectedName ? (
        renaming ? (
          <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto_auto]">
            <input
              value={renameValue}
              onChange={(e) => setRenameValue(e.target.value)}
              placeholder="Новое название набора"
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
            />
            <button
              type="button"
              onClick={handleConfirmRename}
              disabled={renameLoading}
              className="inline-flex h-[50px] items-center justify-center gap-2 rounded-2xl bg-slate-950 px-4 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {renameLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
              Сохранить
            </button>
            <button
              type="button"
              onClick={cancelRename}
              disabled={renameLoading}
              className="inline-flex h-[50px] items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <X className="h-4 w-4" />
              Отмена
            </button>
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <span className="min-w-0 text-sm text-slate-600">
              Выбран набор:{" "}
              <span className="font-medium text-slate-900">{selectedName}</span>
            </span>
            <div className="ml-auto flex items-center gap-2">
              <button
                type="button"
                onClick={startRename}
                className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              >
                <Pencil className="h-4 w-4" />
                Переименовать
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={deletingName === selectedName}
                className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {deletingName === selectedName ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Trash2 className="h-4 w-4" />
                )}
                Удалить
              </button>
            </div>
          </div>
        )
      ) : null}

      <div className="border-t border-slate-200 pt-4">
        <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto]">
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Сохранить текущие параметры</span>
            <input
              value={saveName}
              onChange={(e) => setSaveName(e.target.value)}
              placeholder="Название нового набора"
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
            />
          </label>
          <div className="flex items-end">
            <button
              type="button"
              onClick={handleSave}
              disabled={saving || !saveName.trim()}
              className="inline-flex h-[50px] w-full items-center justify-center gap-2 rounded-2xl bg-slate-950 px-4 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
            >
              {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              Сохранить
            </button>
          </div>
        </div>
      </div>

      {error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 whitespace-pre-wrap break-words">
          {error}
        </div>
      ) : null}

      {message ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 whitespace-pre-wrap break-words">
          {message}
        </div>
      ) : null}
    </div>
  );
}
