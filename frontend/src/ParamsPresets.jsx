import React, { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Asterisk, Bookmark, ChevronDown, Loader2, Save, Trash2 } from "lucide-react";
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

// Подбираем свободное имя вида «Параметры №N», не пересекающееся с уже сохранёнными.
function nextDraftName(list) {
  const existing = new Set(Array.isArray(list) ? list : []);
  let index = existing.size + 1;
  while (existing.has(`Параметры №${index}`)) {
    index += 1;
  }
  return `Параметры №${index}`;
}

export default function ParamsPresets({ buildParams, applyParams }) {
  const [names, setNames] = useState([]);
  const [name, setName] = useState("");
  const [draftName, setDraftName] = useState("");
  const [loadedName, setLoadedName] = useState("");
  const [loadedId, setLoadedId] = useState(null);
  const [open, setOpen] = useState(false);

  const [namesLoading, setNamesLoading] = useState(false);
  const [loadingParam, setLoadingParam] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deletingName, setDeletingName] = useState("");

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // Пользователь вручную поменял имя черновика — перестаём подставлять авто-название.
  const draftEditedRef = useRef(false);
  const containerRef = useRef(null);
  const initialLoadDoneRef = useRef(false);

  // Режим черновика (создание нового набора) — когда не выбран сохранённый набор.
  const isDraftMode = loadedName === "";

  useEffect(() => {
    if (initialLoadDoneRef.current) {
      return;
    }
    initialLoadDoneRef.current = true;

    (async () => {
      const list = await loadNames();
      if (!draftEditedRef.current) {
        const initial = nextDraftName(list);
        setDraftName(initial);
        setName(initial);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    function handleOutsideClick(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  async function loadNames() {
    setNamesLoading(true);

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
        throw new Error(parseErrorText(text) || `Не удалось получить список наборов: ${response.status}`);
      }

      const data = await response.json();
      const list = Array.isArray(data) ? data.map((item) => String(item)) : [];

      setNames(list);
      // Если ранее выбранный набор исчез (например, удалён) — сбрасываем выбор.
      setLoadedName((prev) => (prev && !list.includes(prev) ? "" : prev));
      // Пока пользователь не трогал имя черновика — держим его в синхроне со списком.
      if (!draftEditedRef.current) {
        setDraftName(nextDraftName(list));
      }

      return list;
    } catch (e) {
      setError(e.message || "Ошибка при загрузке списка наборов.");
      return [];
    } finally {
      setNamesLoading(false);
    }
  }

  async function toggleDropdown() {
    const next = !open;
    setOpen(next);

    if (next) {
      const list = await loadNames();
      // В режиме черновика без ручной правки подтягиваем актуальное авто-название.
      if (loadedName === "" && !draftEditedRef.current) {
        setName(nextDraftName(list));
      }
    }
  }

  function handleNameChange(value) {
    setName(value);
    setError("");
    setMessage("");

    // Правка имени в режиме черновика меняет сам черновик; для выбранного набора
    // это подготовка к переименованию и черновика не касается.
    if (loadedName === "") {
      draftEditedRef.current = true;
      setDraftName(value);
    }
  }

  function handlePickDraft() {
    setOpen(false);
    setError("");
    setMessage("");
    setLoadedName("");
    setLoadedId(null);
    setName(draftName);
  }

  // Получаем id набора по имени (нужен для edit, т.к. create возвращает void).
  async function fetchParamId(paramName) {
    try {
      const response = await authFetch(`/api/params/fullParam/${encodeURIComponent(paramName)}`, {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        cache: "no-store",
      });

      if (!response.ok) {
        return null;
      }

      const data = await response.json();
      return data?.id ?? null;
    } catch {
      return null;
    }
  }

  async function handlePick(picked) {
    setOpen(false);
    setError("");
    setMessage("");
    setLoadedName(picked);
    setName(picked);

    setLoadingParam(true);

    try {
      const response = await authFetch(`/api/params/fullParam/${encodeURIComponent(picked)}`, {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        cache: "no-store",
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(parseErrorText(text) || `Не удалось загрузить набор: ${response.status}`);
      }

      const data = await response.json();
      setLoadedId(data?.id ?? null);
      applyParams?.(data);
      setMessage(`Набор «${picked}» подставлен в форму.`);
    } catch (e) {
      setError(e.message || "Ошибка при загрузке набора.");
    } finally {
      setLoadingParam(false);
    }
  }

  async function handleSave() {
    const nextName = name.trim();
    setError("");
    setMessage("");

    if (!nextName) {
      setError("Укажите название набора параметров.");
      return;
    }

    setSaving(true);

    try {
      if (!loadedName) {
        // Новый набор (черновик) — создаём.
        const payload = { name: nextName, ...(buildParams?.() || {}) };

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
          throw new Error(parseErrorText(text) || `Не удалось сохранить набор: ${response.status}`);
        }

        setLoadedName(nextName);
        // create возвращает void — дозапрашиваем id, чтобы дальнейшие правки шли через edit.
        setLoadedId(await fetchParamId(nextName));
        // Черновик стал сохранённым — готовим новый свежий черновик.
        draftEditedRef.current = false;
        await loadNames();
        setMessage(`Параметры сохранены как «${nextName}».`);
      } else if (nextName !== loadedName) {
        // Переименование выбранного сохранённого набора.
        const response = await authFetch("/api/params/rename", {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
          body: JSON.stringify({ oldName: loadedName, newName: nextName }),
        });

        if (!response.ok) {
          const text = await response.text();
          throw new Error(parseErrorText(text) || `Не удалось переименовать набор: ${response.status}`);
        }

        setLoadedName(nextName);
        await loadNames();
        setMessage(`Набор переименован в «${nextName}».`);
      } else {
        // Перезапись уже существующего набора — через edit (с указанием id).
        let id = loadedId;
        if (id == null) {
          id = await fetchParamId(loadedName);
        }
        if (id == null) {
          throw new Error("Не удалось определить id набора. Выберите набор из списка заново.");
        }

        const payload = { id, name: nextName, ...(buildParams?.() || {}) };

        const response = await authFetch("/api/params/edit", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
          body: JSON.stringify(payload),
        });

        if (!response.ok) {
          const text = await response.text();
          throw new Error(parseErrorText(text) || `Не удалось сохранить изменения набора: ${response.status}`);
        }

        setLoadedId(id);
        setMessage(`Изменения набора «${nextName}» сохранены.`);
      }
    } catch (e) {
      setError(e.message || "Ошибка при сохранении набора.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(target) {
    const victim = String(target || "").trim();
    if (!victim || !names.includes(victim)) {
      return;
    }

    const confirmed = window.confirm(`Удалить набор параметров «${victim}»?`);
    if (!confirmed) {
      return;
    }

    setError("");
    setMessage("");
    setDeletingName(victim);

    try {
      const response = await authFetch(`/api/params/${encodeURIComponent(victim)}`, {
        method: "DELETE",
        headers: {
          Accept: "application/json, text/plain, */*",
        },
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(parseErrorText(text) || `Не удалось удалить набор: ${response.status}`);
      }

      const list = await loadNames();

      // Если удалили выбранный набор — возвращаемся к черновику.
      if (loadedName === victim) {
        setLoadedName("");
        setLoadedId(null);
        const draft = draftEditedRef.current ? draftName : nextDraftName(list);
        setName(draft);
      }

      setMessage(`Набор «${victim}» удалён.`);
    } catch (e) {
      setError(e.message || "Ошибка при удалении набора.");
    } finally {
      setDeletingName("");
    }
  }

  const isRenameMode = Boolean(loadedName) && name.trim() !== "" && name.trim() !== loadedName;
  const saveTitle = isRenameMode ? "Переименовать набор" : "Сохранить набор";

  return (
    <div className="mb-8 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
        <div className="flex shrink-0 items-center gap-3">
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-2">
            <Bookmark className="h-4 w-4 text-slate-600" />
          </div>
          <div className="leading-tight">
            <div className="text-sm font-medium text-slate-800">Набор параметров</div>
            <div className="text-xs text-slate-500">Сохранение и загрузка настроек формы</div>
          </div>
        </div>

        <div ref={containerRef} className="relative min-w-0 flex-1">
          <div className="flex items-center rounded-2xl border border-slate-200 bg-white transition focus-within:border-slate-400">
            {isDraftMode ? (
              <span
                className="ml-3 inline-flex shrink-0 items-center text-amber-500"
                title="Несохранённый набор"
              >
                <Asterisk className="h-4 w-4" />
              </span>
            ) : null}
            <input
              value={name}
              onChange={(e) => handleNameChange(e.target.value)}
              placeholder="Название набора"
              className={`w-full bg-transparent py-2.5 text-sm text-slate-900 outline-none placeholder:text-slate-400 ${
                isDraftMode ? "pl-2 pr-4" : "px-4"
              }`}
            />
            {loadingParam || namesLoading ? (
              <Loader2 className="mr-1 h-4 w-4 shrink-0 animate-spin text-slate-400" />
            ) : null}
            <button
              type="button"
              onClick={toggleDropdown}
              className="flex h-full items-center px-3 py-2.5 text-slate-500 transition hover:text-slate-700"
              aria-label="Показать сохранённые наборы"
              aria-expanded={open}
            >
              <ChevronDown className={`h-4 w-4 transition-transform ${open ? "rotate-180" : ""}`} />
            </button>
          </div>

          <AnimatePresence>
            {open ? (
              <motion.div
                initial={{ opacity: 0, y: -4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.15 }}
                className="absolute left-0 right-0 z-30 mt-2 max-h-64 overflow-y-auto rounded-2xl border border-slate-200 bg-white py-1 shadow-lg"
              >
                <div className={`flex items-center gap-1 px-1 ${isDraftMode ? "bg-amber-50" : ""}`}>
                  <button
                    type="button"
                    onClick={handlePickDraft}
                    className="flex min-w-0 flex-1 items-center gap-2 rounded-xl px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-100"
                    title={draftName}
                  >
                    <Asterisk className="h-3.5 w-3.5 shrink-0 text-amber-500" />
                    <span className="truncate">{draftName || "Новый набор"}</span>
                    <span className="ml-auto shrink-0 text-xs text-amber-600">не сохранён</span>
                  </button>
                </div>

                {names.length > 0 ? <div className="my-1 border-t border-slate-100" /> : null}

                {namesLoading && names.length === 0 ? (
                  <div className="flex items-center gap-2 px-4 py-3 text-sm text-slate-500">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Загрузка...
                  </div>
                ) : (
                  names.map((item) => (
                    <div
                      key={item}
                      className={`flex items-center gap-1 px-1 ${item === loadedName ? "bg-slate-50" : ""}`}
                    >
                      <button
                        type="button"
                        onClick={() => handlePick(item)}
                        className="min-w-0 flex-1 truncate rounded-xl px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-100"
                        title={item}
                      >
                        {item}
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDelete(item)}
                        disabled={deletingName === item}
                        className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-xl text-slate-400 transition hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-60"
                        aria-label={`Удалить набор ${item}`}
                        title="Удалить набор"
                      >
                        {deletingName === item ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Trash2 className="h-4 w-4" />
                        )}
                      </button>
                    </div>
                  ))
                )}
              </motion.div>
            ) : null}
          </AnimatePresence>
        </div>

        <button
          type="button"
          onClick={handleSave}
          disabled={saving || !name.trim()}
          title={saveTitle}
          className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl bg-slate-950 px-3.5 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
          <span className="hidden sm:inline">{isRenameMode ? "Переименовать" : "Сохранить"}</span>
        </button>
      </div>

      <AnimatePresence>
        {error || message ? (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.15 }}
          >
            {error ? (
              <div className="mt-3 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 whitespace-pre-wrap break-words">
                {error}
              </div>
            ) : (
              <div className="mt-3 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700 whitespace-pre-wrap break-words">
                {message}
              </div>
            )}
          </motion.div>
        ) : null}
      </AnimatePresence>
    </div>
  );
}
