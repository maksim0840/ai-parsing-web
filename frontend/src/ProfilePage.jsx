import React, { useEffect, useMemo, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import {
  ArrowLeft,
  CalendarDays,
  Download,
  Eye,
  FileText,
  Loader2,
  RefreshCw,
  Search,
  Trash2,
  User,
  X,
} from "lucide-react";
import { authFetch, getStoredAuth } from "./AuthGate.jsx";

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

const EXPORT_FORMATS = [
  { value: "JSON", label: "JSON", extension: "json" },
  { value: "XML", label: "XML", extension: "xml" },
  { value: "CSV", label: "CSV", extension: "csv" },
];

function resolveExportExtension(format) {
  const found = EXPORT_FORMATS.find((item) => item.value === format);
  return found ? found.extension : "txt";
}

// Бэкенд присылает имя файла в Content-Disposition; если заголовок недоступен,
// собираем имя сами из выбранного формата.
function resolveExportFilename(contentDisposition, format) {
  const fallback = `result.${resolveExportExtension(format)}`;
  const raw = String(contentDisposition || "");
  if (!raw) return fallback;

  const encodedMatch = raw.match(/filename\*=(?:UTF-8'')?([^;]+)/i);
  if (encodedMatch?.[1]) {
    try {
      return decodeURIComponent(encodedMatch[1].trim().replace(/^"|"$/g, "")) || fallback;
    } catch {
      // падаем в обычный filename
    }
  }

  const plainMatch = raw.match(/filename="?([^";]+)"?/i);
  if (plainMatch?.[1]) {
    return plainMatch[1].trim() || fallback;
  }

  return fallback;
}

function triggerBlobDownload(blob, filename) {
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = objectUrl;
  link.download = filename;
  link.style.display = "none";

  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(objectUrl);
}

function extractErrorMessage(text, status) {
  const raw = String(text || "").trim();
  if (!raw) return `Не удалось экспортировать историю: ${status}`;

  try {
    const parsed = JSON.parse(raw);
    return parsed?.message || parsed?.error || parsed?.details || raw;
  } catch {
    return raw;
  }
}

function formatDateTime(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);

  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatRuDate(date) {
  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();
  return `${day}.${month}.${year}`;
}

function formatInputDateToRu(value) {
  const trimmed = String(value || "").trim();
  if (!trimmed) return "";

  const match = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return trimmed;

  const [, yyyy, mm, dd] = match;
  return `${dd}.${mm}.${yyyy}`;
}

function getTodayInputValue() {
  const date = new Date();
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function getMonthAgoInputValue() {
  const date = new Date();
  date.setMonth(date.getMonth() - 1);
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function parseInputDate(value) {
  const trimmed = String(value || "").trim();
  if (!trimmed) return null;

  const match = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return null;

  const [, yyyy, mm, dd] = match;
  const date = new Date(Number(yyyy), Number(mm) - 1, Number(dd));

  if (
    Number.isNaN(date.getTime()) ||
    date.getFullYear() !== Number(yyyy) ||
    date.getMonth() !== Number(mm) - 1 ||
    date.getDate() !== Number(dd)
  ) {
    return null;
  }

  return date;
}

function toBoundaryIso(date, boundary) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return "";

  const next = new Date(date);
  if (boundary === "start") {
    next.setHours(0, 0, 0, 0);
  } else {
    next.setHours(23, 59, 59, 999);
  }

  return next.toISOString();
}

function normalizeDateRange(dateFromValue, dateToValue) {
  const fromDate = parseInputDate(dateFromValue);
  const toDate = parseInputDate(dateToValue);

  if (!fromDate && !toDate) {
    return { dateFromIso: "", dateToIso: "" };
  }

  if (fromDate && !toDate) {
    return {
      dateFromIso: toBoundaryIso(fromDate, "start"),
      dateToIso: "",
    };
  }

  if (!fromDate && toDate) {
    return {
      dateFromIso: "",
      dateToIso: toBoundaryIso(toDate, "end"),
    };
  }

  const startDate = fromDate <= toDate ? fromDate : toDate;
  const endDate = fromDate <= toDate ? toDate : fromDate;

  return {
    dateFromIso: toBoundaryIso(startDate, "start"),
    dateToIso: toBoundaryIso(endDate, "end"),
  };
}

function stringifyResult(value) {
  if (value == null) return "";
  // Бэкенд теперь отдаёт result уже строкой (JSON/XML/CSV) — показываем как есть.
  if (typeof value === "string") return value;
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

function buildPreviewText(value, maxLines = 4) {
  const text = stringifyResult(value);
  if (!text) return "";

  const lines = text.split(/\r?\n/);
  if (lines.length <= maxLines) {
    return text;
  }

  const previewLines = lines.slice(0, maxLines);
  const lastIndex = previewLines.length - 1;
  previewLines[lastIndex] = `${previewLines[lastIndex]}…`;

  return previewLines.join("\n");
}

function buildPageList(totalPages, currentPage) {
  if (totalPages <= 1) return [1];

  const pages = new Set([1, totalPages, currentPage + 1]);

  if (currentPage + 2 <= totalPages) {
    pages.add(currentPage + 2);
  }
  if (currentPage >= 1) {
    pages.add(currentPage);
  }
  if (currentPage >= 2) {
    pages.add(currentPage - 1);
  }

  const sorted = Array.from(pages)
    .filter((page) => page >= 1 && page <= totalPages)
    .sort((a, b) => a - b);

  const result = [];
  for (let i = 0; i < sorted.length; i += 1) {
    const page = sorted[i];
    const prev = sorted[i - 1];

    if (prev && page - prev > 1) {
      result.push("ellipsis");
    }

    result.push(page);
  }

  return result;
}

function DatePickerField({ label, value, onChange }) {
  const inputRef = useRef(null);

  function openPicker() {
    if (!inputRef.current) return;

    if (typeof inputRef.current.showPicker === "function") {
      inputRef.current.showPicker();
      return;
    }

    inputRef.current.focus();
    inputRef.current.click();
  }

  return (
    <label className="block">
      <span className="mb-2 block text-sm font-medium text-slate-700">{label}</span>
      <div className="relative">
        <button
          type="button"
          onClick={openPicker}
          className="flex w-full items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-left text-sm text-slate-900 outline-none transition hover:bg-slate-50 focus:border-slate-400"
        >
          <CalendarDays className="h-4 w-4 shrink-0 text-slate-400" />
          <span>{formatInputDateToRu(value) || "Выберите дату"}</span>
        </button>
        <input
          ref={inputRef}
          type="date"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="pointer-events-none absolute inset-0 h-0 w-0 opacity-0"
          tabIndex={-1}
          aria-hidden="true"
        />
      </div>
    </label>
  );
}

function ResultModal({ item, onClose }) {
  if (!item) return null;

  const prettyJson = stringifyResult(item.result);

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/45 px-4"
        onClick={onClose}
      >
        <motion.div
          initial={{ scale: 0.96, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.98, opacity: 0 }}
          onClick={(e) => e.stopPropagation()}
          className="flex max-h-[85vh] w-full max-w-4xl flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-2xl"
        >
          <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-4">
            <div className="min-w-0">
              <h3 className="truncate text-lg font-semibold text-slate-950">
                Полный результат
              </h3>
              <p className="mt-1 whitespace-normal break-words text-sm text-slate-500">{item.url || "Без URL"}</p>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-700"
              aria-label="Закрыть окно результата"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto px-6 py-5">
            <pre className="whitespace-pre-wrap break-words rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-800">
              {prettyJson || "Результат пустой."}
            </pre>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}

function EmptyState({ loading }) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-200 bg-slate-50 px-6 py-14 text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl border border-slate-200 bg-white">
        {loading ? (
          <Loader2 className="h-5 w-5 animate-spin text-slate-500" />
        ) : (
          <FileText className="h-5 w-5 text-slate-400" />
        )}
      </div>
      <div className="mt-4 text-sm font-medium text-slate-700">
        {loading ? "Загрузка истории..." : "История ответов пока пуста"}
      </div>
      <p className="mt-2 text-sm text-slate-500">
        После сохранения результатов парсинга они появятся на этой странице.
      </p>
    </div>
  );
}

export default function ProfilePage({ onBack }) {
  const auth = useMemo(() => getStoredAuth(), []);
  const [results, setResults] = useState([]);
  const [selectedResult, setSelectedResult] = useState(null);

  const [dateFrom, setDateFrom] = useState(() => getTodayInputValue());
  const [dateTo, setDateTo] = useState(() => getMonthAgoInputValue());
  const [pageNum, setPageNum] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [sortMode, setSortMode] = useState("true");

  const [totalRecords, setTotalRecords] = useState(0);

  const [loading, setLoading] = useState(false);
  const [deletingId, setDeletingId] = useState("");
  const [error, setError] = useState("");

  const [exportFormat, setExportFormat] = useState("JSON");
  const [isExporting, setIsExporting] = useState(false);
  const [exportError, setExportError] = useState("");

  const initialLoadDoneRef = useRef(false);

  function buildQueryString(nextState = {}) {
    const params = new URLSearchParams();

    const effectivePageNum =
      Number.isFinite(Number(nextState.pageNum))
        ? Math.max(0, Number(nextState.pageNum))
        : Math.max(0, Number(pageNum) || 0);

    const effectivePageSize =
      Number.isFinite(Number(nextState.pageSize))
        ? Math.max(1, Number(nextState.pageSize))
        : Math.max(1, Number(pageSize) || 10);

    const effectiveSortMode =
      nextState.sortMode !== undefined ? String(nextState.sortMode) : String(sortMode);

    const effectiveDateFrom =
      nextState.dateFrom !== undefined ? nextState.dateFrom : dateFrom;

    const effectiveDateTo =
      nextState.dateTo !== undefined ? nextState.dateTo : dateTo;

    params.set("pageNum", String(effectivePageNum));
    params.set("pageSize", String(effectivePageSize));

    const { dateFromIso, dateToIso } = normalizeDateRange(effectiveDateFrom, effectiveDateTo);

    if (dateFromIso) {
      params.set("dateFrom", dateFromIso);
    }
    if (dateToIso) {
      params.set("dateTo", dateToIso);
    }

    params.set("isSortDesc", effectiveSortMode === "false" ? "false" : "true");

    return params.toString();
  }

  async function loadResults(nextState = {}) {
    setLoading(true);
    setError("");

    try {
      const query = buildQueryString(nextState);
      const response = await authFetch(`/api/results${query ? `?${query}` : ""}`, {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
        cache: "no-store",
      });

      if (!response.ok) {
        throw new Error(`Не удалось получить историю результатов: ${response.status}`);
      }

      const data = await response.json();
      const pagedData = Array.isArray(data?.pagedData) ? data.pagedData : [];
      const nextTotalRecords = Number.isFinite(Number(data?.totalRecords))
        ? Number(data.totalRecords)
        : 0;

      setResults(pagedData);
      setTotalRecords(nextTotalRecords);
    } catch (e) {
      setError(e.message || "Ошибка при загрузке истории.");
    } finally {
      setLoading(false);
    }
  }

  async function handleExport() {
    if (isExporting) {
      return;
    }

    setIsExporting(true);
    setExportError("");

    try {
      // Берём ровно те же параметры фильтрации, что применены к таблице сейчас.
      const params = new URLSearchParams(buildQueryString());
      params.set("format", exportFormat);

      const response = await authFetch(`/api/results/export?${params.toString()}`, {
        method: "GET",
        headers: {
          Accept: "application/json, application/xml, text/csv, */*",
        },
        cache: "no-store",
      });

      if (!response.ok) {
        const text = await response.text().catch(() => "");
        throw new Error(extractErrorMessage(text, response.status));
      }

      const blob = await response.blob();
      const filename = resolveExportFilename(
        response.headers.get("Content-Disposition"),
        exportFormat
      );

      triggerBlobDownload(blob, filename);
    } catch (e) {
      setExportError(e.message || "Ошибка при экспорте истории.");
    } finally {
      setIsExporting(false);
    }
  }

  async function handleDelete(id) {
    if (!id) return;

    const confirmed = window.confirm("Удалить эту запись из истории?");
    if (!confirmed) return;

    setDeletingId(id);
    setError("");

    try {
      const response = await authFetch(`/api/results/${encodeURIComponent(id)}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        throw new Error(`Не удалось удалить запись: ${response.status}`);
      }

      if (selectedResult?.id === id) {
        setSelectedResult(null);
      }

      const nextTotalRecords = Math.max(0, totalRecords - 1);
      const nextTotalPages = Math.max(1, Math.ceil(nextTotalRecords / pageSize));
      const nextPageNum = Math.min(pageNum, nextTotalPages - 1);

      if (nextPageNum !== pageNum) {
        setPageNum(nextPageNum);
        await loadResults({ pageNum: nextPageNum });
      } else {
        await loadResults();
      }
    } catch (e) {
      setError(e.message || "Ошибка при удалении записи.");
    } finally {
      setDeletingId("");
    }
  }

  function resetFilters() {
    const nextDateFrom = getTodayInputValue();
    const nextDateTo = getMonthAgoInputValue();
    setDateFrom(nextDateFrom);
    setDateTo(nextDateTo);
    setPageNum(0);
    setPageSize(10);
    setSortMode("true");
    loadResults({
      dateFrom: nextDateFrom,
      dateTo: nextDateTo,
      pageNum: 0,
      pageSize: 10,
      sortMode: "true",
    });
  }

  function handleApplyFilters() {
    setPageNum(0);
    loadResults({ pageNum: 0 });
  }

  function handlePageChange(nextPageNum) {
    if (nextPageNum < 0 || nextPageNum >= totalPages || nextPageNum === pageNum) {
      return;
    }

    setPageNum(nextPageNum);
    loadResults({ pageNum: nextPageNum });
  }

  function handlePageSizeChange(nextPageSize) {
    const normalized = Number(nextPageSize) || 10;
    setPageSize(normalized);
    setPageNum(0);
    loadResults({ pageNum: 0, pageSize: normalized });
  }

  useEffect(() => {
    if (initialLoadDoneRef.current) {
      return;
    }

    initialLoadDoneRef.current = true;
    loadResults();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const previewRows = useMemo(
    () =>
      results.map((item) => ({
        ...item,
        preview: buildPreviewText(item.result, 4),
      })),
    [results]
  );

  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));
  const visiblePages = buildPageList(totalPages, pageNum);

  return (
    <>
      <ResultModal item={selectedResult} onClose={() => setSelectedResult(null)} />

      <div className="min-h-screen bg-slate-50 text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          <motion.header
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
            className="mb-8 flex flex-col gap-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm lg:flex-row lg:items-end lg:justify-between"
          >
            <div>
              {typeof onBack === "function" ? (
                <button
                  type="button"
                  onClick={onBack}
                  className="mb-4 inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-700 transition hover:bg-slate-50"
                >
                  <ArrowLeft className="h-4 w-4" />
                  На главную
                </button>
              ) : null}
              <h1 className="text-3xl font-semibold tracking-tight text-slate-950">
                Профиль
              </h1>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
                Информация об аккаунте и сохранённых результатах анализа.
              </p>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
              <div className="flex items-center gap-3">
                <div className="rounded-xl border border-slate-200 bg-white p-2">
                  <User className="h-4 w-4 text-slate-600" />
                </div>
                <div>
                  <div className="text-xs text-slate-500">Имя пользователя</div>
                  <div className="mt-1 text-sm font-semibold text-slate-900">
                    {auth.username || "Пользователь"}
                  </div>
                </div>
              </div>
            </div>
          </motion.header>

          <motion.section
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
            className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"
          >
            <div className="mb-5 flex items-start gap-3">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2.5">
                <FileText className="h-5 w-5 text-slate-700" />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-slate-950">История ответов</h2>
                <p className="mt-1 text-sm text-slate-500">
                  Просматривайте сохранённые результаты анализа веб-страниц. Используйте фильтр по датам и сортировку, чтобы быстро найти нужную запись, открыть результат парсинга или удалить ненужные данные.
                </p>
              </div>
            </div>

            <div className="mb-6 grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_220px_auto]">
              <DatePickerField
                label="Дата от"
                value={dateFrom}
                onChange={setDateFrom}
              />

              <DatePickerField
                label="Дата до"
                value={dateTo}
                onChange={setDateTo}
              />

              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">Сортировка</span>
                <select
                  value={sortMode}
                  onChange={(e) => setSortMode(e.target.value)}
                  className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
                >
                  <option value="true">Сначала новые</option>
                  <option value="false">Сначала старые</option>
                </select>
              </label>

              <div className="flex items-end gap-3">
                <button
                  type="button"
                  onClick={handleApplyFilters}
                  disabled={loading}
                  className="inline-flex h-[50px] items-center justify-center gap-2 rounded-2xl bg-slate-950 px-4 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
                  Найти
                </button>
                <button
                  type="button"
                  onClick={resetFilters}
                  className="inline-flex h-[50px] items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-700 transition hover:bg-slate-50"
                >
                  <RefreshCw className="h-4 w-4" />
                  Сбросить
                </button>
              </div>
            </div>

            {error ? (
              <div className="mb-6 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 whitespace-pre-wrap break-words">
                {error}
              </div>
            ) : null}

            {previewRows.length === 0 ? (
              <EmptyState loading={loading} />
            ) : (
              <>
                <div className="overflow-hidden rounded-3xl border border-slate-200">
                  <div className="overflow-x-auto">
                    <table className="min-w-full bg-white">
                      <thead className="bg-slate-50">
                        <tr className="text-left text-xs uppercase tracking-wide text-slate-500">
                          <th className="px-4 py-3 font-medium">Дата</th>
                          <th className="px-4 py-3 font-medium">URL</th>
                          <th className="px-4 py-3 font-medium">Результат</th>
                          <th className="px-4 py-3 font-medium text-right">Действия</th>
                        </tr>
                      </thead>
                      <tbody>
                        {previewRows.map((item) => (
                          <tr key={item.id} className="border-t border-slate-200 align-top">
                            <td className="px-4 py-4 text-sm text-slate-700">
                              {formatDateTime(item.createdAt)}
                            </td>
                            <td className="max-w-[320px] px-4 py-4 text-sm text-slate-700">
                              <div className="line-clamp-3 break-words">{item.url || "—"}</div>
                            </td>
                            <td className="max-w-[480px] px-4 py-4 text-sm text-slate-700">
                              <div className="whitespace-pre-wrap break-words rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2">
                                {item.preview || "Пустой результат"}
                              </div>
                            </td>
                            <td className="px-4 py-4">
                              <div className="flex items-center justify-end gap-2">
                                <button
                                  type="button"
                                  onClick={() => setSelectedResult(item)}
                                  className="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200 text-slate-600 transition hover:bg-slate-50"
                                  title="Открыть результат"
                                  aria-label="Открыть результат"
                                >
                                  <Eye className="h-4 w-4" />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDelete(item.id)}
                                  disabled={deletingId === item.id}
                                  className="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                                  title="Удалить запись"
                                  aria-label="Удалить запись"
                                >
                                  {deletingId === item.id ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                  ) : (
                                    <Trash2 className="h-4 w-4" />
                                  )}
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                <div className="mt-6 rounded-3xl bg-white p-4">
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
                    <label className="inline-flex shrink-0 items-center gap-3 text-sm text-slate-700">
                      <span>Записей на странице</span>
                      <select
                        value={pageSize}
                        onChange={(e) => handlePageSizeChange(e.target.value)}
                        className="rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-slate-400"
                      >
                        {PAGE_SIZE_OPTIONS.map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    </label>

                    <div className="flex flex-wrap items-center gap-2 lg:ml-8">
                      {visiblePages.map((page, index) =>
                        page === "ellipsis" ? (
                          <span key={`ellipsis-${index}`} className="px-2 text-sm text-slate-400">
                            …
                          </span>
                        ) : (
                          <button
                            key={page}
                            type="button"
                            onClick={() => handlePageChange(page - 1)}
                            className={`inline-flex h-10 min-w-10 items-center justify-center rounded-2xl border px-3 text-sm transition ${
                              pageNum + 1 === page
                                ? "border-slate-900 bg-slate-900 text-white"
                                : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                            }`}
                          >
                            {page}
                          </button>
                        )
                      )}

                      <button
                        type="button"
                        onClick={() => handlePageChange(pageNum + 1)}
                        disabled={pageNum + 1 >= totalPages}
                        className="inline-flex h-10 items-center justify-center rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        Следующая
                      </button>
                    </div>

                    <div className="inline-flex items-stretch overflow-hidden rounded-2xl border border-slate-200 bg-white transition focus-within:border-slate-400 lg:ml-auto">
                      <select
                        value={exportFormat}
                        onChange={(e) => {
                          setExportFormat(e.target.value);
                          setExportError("");
                        }}
                        disabled={isExporting}
                        aria-label="Формат экспорта"
                        className="border-r border-slate-200 bg-transparent px-4 py-2.5 text-sm text-slate-700 outline-none disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        {EXPORT_FORMATS.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>

                      <button
                        type="button"
                        onClick={handleExport}
                        disabled={isExporting || totalRecords === 0}
                        title="Экспортировать по текущим фильтрам"
                        className="inline-flex items-center gap-2 bg-slate-950 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        {isExporting ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Download className="h-4 w-4" />
                        )}
                        Экспортировать
                      </button>
                    </div>
                  </div>

                  <AnimatePresence>
                    {exportError ? (
                      <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: "auto" }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: 0.15 }}
                      >
                        <div className="mt-3 rounded-2xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-700 whitespace-pre-wrap break-words">
                          {exportError}
                        </div>
                      </motion.div>
                    ) : null}
                  </AnimatePresence>
                </div>
              </>
            )}
          </motion.section>
        </div>
      </div>
    </>
  );
}