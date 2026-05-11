import React, { useEffect, useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Upload,
  Download,
  Plus,
  Trash2,
  Play,
  Loader2,
  Image as ImageIcon,
  Globe,
  Settings,
  FileText,
  Brain,
  FileCode2,
  CheckCircle2,
  AlertCircle,
  Clock3,
} from "lucide-react";

const CLEANUP_TAGS = [
  "noscript",
  "link",
  "style",
  "meta",
  "script",
  "canvas",
  "svg",
  "area",
  "img",
  "video",
  "audio",
  "iframe",
  "portal",
  "embed",
  "object",
  "source",
];

const PARSING_COMPLEXITY = ["LIGHT", "DEFAULT", "DIFFICULT"];
const MODELS = ["YandexGPT 5.1 Pro", "GigaChat 2 Max"];
const PROXY_FIELDS = [
  { key: "ip", label: "IP" },
  { key: "port", label: "Port" },
  { key: "username", label: "Username" },
  { key: "password", label: "Password" },
];

const STATUS_META = {
  NOT_REGISTERED: { label: "Задача ещё не зарегистрирована на сервере", progress: 0 },
  CREATED: { label: "Задача создана", progress: 10 },
  HTML_PARSING: { label: "Парсинг HTML", progress: 30 },
  HTML_PREPROCESSING: { label: "Предобработка HTML", progress: 50 },
  TEXT_RECOGNITION: { label: "Распознавание текста на изображениях", progress: 70 },
  LLM_PROCESSING: { label: "Обработка LLM", progress: 90 },
  DONE: { label: "Готово", progress: 100 },
  FAILED: { label: "Ошибка", progress: 100 },
};

function debugLog(scope, ...args) {
  const ts = new Date().toISOString();
  console.log(`[ConferenceParser][${ts}][${scope}]`, ...args);
}

function createKeyValueRow() {
  return { id: crypto.randomUUID(), key: "", value: "" };
}

function clampNumber(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function normalizeTaskStatus(status) {
  return String(status || "").trim().toUpperCase();
}

function getStatusMeta(status) {
  if (!status) {
    return { label: "Ожидание запуска", progress: 0 };
  }
  return STATUS_META[status] || { label: status, progress: 0 };
}

function KeyValueEditor({
  title,
  rows,
  setRows,
  placeholderKey,
  placeholderValue,
  useSection,
  setUseSection,
  checkboxLabel,
}) {
  const addRow = () => {
    setRows((prev) => [...prev, createKeyValueRow()]);
  };

  const updateRow = (id, field, value) => {
    setRows((prev) => prev.map((row) => (row.id === id ? { ...row, [field]: value } : row)));
  };

  const removeRow = (id) => {
    setRows((prev) => prev.filter((row) => row.id !== id));
  };

  const isEnabled = Boolean(useSection);

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
          <p className="mt-1 text-xs text-slate-500">
            Можно добавить несколько пар ключ-значение или не использовать этот блок вовсе.
          </p>
        </div>
        <label className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={isEnabled}
            onChange={(e) => setUseSection(e.target.checked)}
            className="h-4 w-4 rounded border-slate-300"
          />
          {checkboxLabel}
        </label>
      </div>

      {!isEnabled ? (
        <div className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-sm text-slate-500">
          {title} не используются.
        </div>
      ) : (
        <div className="space-y-3">
          <div className="flex justify-end">
            <button
              type="button"
              onClick={addRow}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
            >
              <Plus className="h-4 w-4" />
              Добавить
            </button>
          </div>

          {rows.length === 0 && (
            <div className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-sm text-slate-500">
              Параметры не указаны.
            </div>
          )}

          {rows.map((row) => (
            <div key={row.id} className="grid grid-cols-1 gap-3 md:grid-cols-[220px_minmax(0,1fr)_auto]">
              <input
                value={row.key}
                onChange={(e) => updateRow(row.id, "key", e.target.value)}
                placeholder={placeholderKey}
                className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none transition focus:border-slate-400"
              />
              <input
                value={row.value}
                onChange={(e) => updateRow(row.id, "value", e.target.value)}
                placeholder={placeholderValue}
                className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none transition focus:border-slate-400"
              />
              <button
                type="button"
                onClick={() => removeRow(row.id)}
                className="inline-flex items-center justify-center rounded-xl border border-slate-200 px-3 py-2 text-slate-600 transition hover:bg-slate-50"
                aria-label={`Удалить ${title}`}
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ProxyEditor({ useProxy, setUseProxy, proxyConfig, setProxyConfig }) {
  const updateProxyField = (field, value) => {
    setProxyConfig((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">Proxy</h3>
          <p className="mt-1 text-xs text-slate-500">
            Доступны только поля: ip, port, username, password. При снятии галочки значения сохраняются, но не отправляются.
          </p>
        </div>
        <label className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={useProxy}
            onChange={(e) => setUseProxy(e.target.checked)}
            className="h-4 w-4 rounded border-slate-300"
          />
          Использовать proxy
        </label>
      </div>

      {!useProxy ? (
        <div className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-sm text-slate-500">
          Proxy не используется.
        </div>
      ) : (
        <div className="space-y-3">
          {PROXY_FIELDS.map((field) => (
            <div key={field.key} className="grid grid-cols-1 gap-3 md:grid-cols-[160px_minmax(0,1fr)]">
              <div className="flex items-center rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm font-medium text-slate-700">
                {field.label}
              </div>
              <input
                value={proxyConfig[field.key]}
                onChange={(e) => updateProxyField(field.key, e.target.value)}
                placeholder={`Введите ${field.label.toLowerCase()}`}
                className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none transition focus:border-slate-400"
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function StorageFileCard({
  item,
  variant = "file",
  onDownload,
  onDelete,
  deleting,
}) {
  const isImage = variant === "image";

  return (
    <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
      <div className="aspect-[4/3] bg-slate-100">
        {isImage && item.previewUrl ? (
          <img src={item.previewUrl} alt={item.name} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full flex-col items-center justify-center gap-2 text-slate-400">
            {isImage ? <ImageIcon className="h-9 w-9" /> : <FileCode2 className="h-9 w-9" />}
            {!isImage && <span className="text-xs uppercase tracking-wide">HTML</span>}
          </div>
        )}
      </div>

      <div className="space-y-3 p-4">
        <div>
          <p className="truncate text-sm font-semibold text-slate-900">{item.name}</p>
          <p className="mt-1 text-xs text-slate-500">{formatBytes(item.size)}</p>
        </div>

        <div className="grid grid-cols-2 gap-2">
          <button
            type="button"
            onClick={() => onDownload(item)}
            className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 px-3 py-2.5 text-sm text-slate-700 transition hover:bg-slate-50"
          >
            <Download className="h-4 w-4" />
            Скачать
          </button>
          <button
            type="button"
            onClick={() => onDelete(item.id)}
            disabled={deleting}
            className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 px-3 py-2.5 text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {deleting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
            Удалить
          </button>
        </div>
      </div>
    </div>
  );
}

function StorageSection({
  title,
  description,
  uploadLabel,
  uploading,
  accept,
  multiple = true,
  onUpload,
  emptyText,
  items,
  variant,
  onDownload,
  onDelete,
  deletingId,
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-4">
        <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
        <p className="mt-1 text-xs text-slate-500">{description}</p>
      </div>

      <div className="space-y-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <label className="inline-flex cursor-pointer items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 transition hover:bg-slate-50">
            <Upload className="h-4 w-4" />
            <span>{uploading ? "Загрузка..." : uploadLabel}</span>
            <input
              type="file"
              accept={accept}
              multiple={multiple}
              className="hidden"
              onChange={onUpload}
              disabled={uploading}
            />
          </label>
        </div>

        {items.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
            {emptyText}
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {items.map((item) => (
              <StorageFileCard
                key={item.id}
                item={item}
                variant={variant}
                onDownload={onDownload}
                onDelete={onDelete}
                deleting={deletingId === item.id}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function RangeField({
  label,
  value,
  min,
  max,
  step,
  inputStep,
  onChange,
  leftLabel,
  rightLabel,
}) {
  const handleInputChange = (event) => {
    const rawValue = event.target.value;

    if (rawValue === "") {
      onChange(min);
      return;
    }

    const nextValue = Number(rawValue);
    if (Number.isNaN(nextValue)) {
      return;
    }

    onChange(clampNumber(nextValue, min, max));
  };

  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <span className="text-sm font-medium text-slate-700">{label}</span>
        <input
          type="number"
          min={min}
          max={max}
          step={inputStep ?? step}
          value={value}
          onChange={handleInputChange}
          className="w-28 rounded-xl border border-slate-200 bg-white px-3 py-2 text-right text-sm font-semibold text-slate-900 outline-none transition focus:border-slate-400 [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-inner-spin-button]:m-0 [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-outer-spin-button]:m-0"
        />
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(clampNumber(Number(e.target.value), min, max))}
        className="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-200"
      />
      <div className="mt-2 flex items-center justify-between text-xs text-slate-500">
        <span>{leftLabel ?? min}</span>
        <span>{rightLabel ?? max}</span>
      </div>
    </div>
  );
}

function StatusOverlay({ visible, taskId, status, message, sessionId }) {
  const statusMeta = getStatusMeta(status);
  const isFailed = status === "FAILED";

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 px-4"
        >
          <motion.div
            initial={{ scale: 0.96, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.98, opacity: 0 }}
            className="w-full max-w-lg rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl"
          >
            <div className="flex items-start gap-4">
              <div className={`rounded-2xl p-3 ${isFailed ? "bg-red-50" : "bg-slate-100"}`}>
                {isFailed ? (
                  <AlertCircle className="h-6 w-6 text-red-600" />
                ) : (
                  <Loader2 className="h-6 w-6 animate-spin text-slate-700" />
                )}
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-lg font-semibold text-slate-950">Выполняется обработка</h3>
                <p className="mt-1 text-sm text-slate-500">
                  Сервер обрабатывает задачу. Статус обновляется автоматически каждые 5 секунд.
                </p>
              </div>
            </div>

            <div className="mt-6 space-y-4">
              <div>
                <div className="mb-2 flex items-center justify-between gap-3 text-sm">
                  <span className="font-medium text-slate-700">Текущий статус</span>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-semibold ${
                      isFailed ? "bg-red-100 text-red-700" : "bg-slate-100 text-slate-700"
                    }`}
                  >
                    {statusMeta.label}
                  </span>
                </div>
                <div className="h-3 overflow-hidden rounded-full bg-slate-100">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${isFailed ? "bg-red-500" : "bg-slate-900"}`}
                    style={{ width: `${statusMeta.progress}%` }}
                  />
                </div>
              </div>

              <div className="grid gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
                <div>
                  <div className="text-xs text-slate-500">Session ID</div>
                  <div className="mt-1 break-all text-sm font-medium text-slate-900">{sessionId || "—"}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-500">Task ID</div>
                  <div className="mt-1 break-all text-sm font-medium text-slate-900">{taskId || "—"}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-500">Статус из сервера</div>
                  <div className="mt-1 text-sm font-medium text-slate-900">{status || "—"}</div>
                </div>
              </div>

              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="mb-1 text-xs text-slate-500">Сообщение</div>
                <div className="whitespace-pre-wrap break-words text-sm text-slate-700">
                  {message || "Сервер пока не прислал дополнительное сообщение."}
                </div>
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function Section({ icon: Icon, title, description, children }) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
      className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"
    >
      <div className="mb-5 flex items-start gap-3">
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-2.5">
          <Icon className="h-5 w-5 text-slate-700" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-slate-950">{title}</h2>
          <p className="mt-1 text-sm text-slate-500">{description}</p>
        </div>
      </div>
      {children}
    </motion.section>
  );
}

function buildMapFromRows(rows) {
  return rows
    .filter((row) => row.key.trim() && row.value.trim())
    .reduce((acc, row) => {
      acc[row.key.trim()] = row.value.trim();
      return acc;
    }, {});
}

function buildProxyPayload(useProxy, proxyConfig) {
  if (!useProxy) return null;

  const normalized = Object.entries(proxyConfig).reduce((acc, [key, value]) => {
    if (String(value).trim()) {
      acc[key] = String(value).trim();
    }
    return acc;
  }, {});

  return Object.keys(normalized).length ? normalized : {};
}

function buildPreprocessingPayload(selectedTags) {
  return {
    noscript: selectedTags.includes("noscript"),
    link: selectedTags.includes("link"),
    style: selectedTags.includes("style"),
    meta: selectedTags.includes("meta"),
    script: selectedTags.includes("script"),
    canvas: selectedTags.includes("canvas"),
    svg: selectedTags.includes("svg"),
    area: selectedTags.includes("area"),
    img: selectedTags.includes("img"),
    video: selectedTags.includes("video"),
    audio: selectedTags.includes("audio"),
    iframe: selectedTags.includes("iframe"),
    portal: selectedTags.includes("portal"),
    embed: selectedTags.includes("embed"),
    object: selectedTags.includes("object"),
    source: selectedTags.includes("source"),
  };
}

function getFileNameFromPath(filePath, fallback = "file") {
  const normalized = String(filePath || "").split(/[?#]/)[0];
  const parts = normalized.split("/").filter(Boolean);
  return parts[parts.length - 1] || fallback;
}

function buildStoredFileUrl(filePath) {
  return `/api/files/download?filePath=${encodeURIComponent(filePath)}`;
}

function appendUniqueFiles(prevItems, nextItems) {
  const seen = new Set(
    prevItems.map((item) => item.filePath || item.downloadUrl || item.previewUrl || item.name)
  );

  const filtered = nextItems.filter((item) => {
    const key = item.filePath || item.downloadUrl || item.previewUrl || item.name;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });

  return [...prevItems, ...filtered];
}

function formatBytes(size) {
  if (!size && size !== 0) return "—";
  const units = ["B", "KB", "MB", "GB"];
  let value = size;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value.toFixed(value >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

export default function ConferenceParserPage() {
  const [siteUrl, setSiteUrl] = useState("");
  const [downloadImagesFromSite, setDownloadImagesFromSite] = useState(true);

  const [useHeaders, setUseHeaders] = useState(false);
  const [headers, setHeaders] = useState([createKeyValueRow()]);

  const [useCookies, setUseCookies] = useState(false);
  const [cookies, setCookies] = useState([createKeyValueRow()]);

  const [useProxy, setUseProxy] = useState(false);
  const [proxyConfig, setProxyConfig] = useState({
    ip: "",
    port: "",
    username: "",
    password: "",
  });

  const [parsingComplexity, setParsingComplexity] = useState("DEFAULT");
  const [extraWaitSeconds, setExtraWaitSeconds] = useState(5);

  const [cleanupTags, setCleanupTags] = useState([...CLEANUP_TAGS]);

  const [garageImages, setGarageImages] = useState([]);
  const [garageUploading, setGarageUploading] = useState(false);
  const [imageDeletingId, setImageDeletingId] = useState(null);

  const [garageHtmlFiles, setGarageHtmlFiles] = useState([]);
  const [htmlUploading, setHtmlUploading] = useState(false);
  const [htmlDeletingId, setHtmlDeletingId] = useState(null);

  const [model, setModel] = useState("YandexGPT 5.1 Pro");
  const [temperature, setTemperature] = useState(0.2);
  const [maxOutputTokens, setMaxOutputTokens] = useState(2048);
  const [systemPrompt, setSystemPrompt] = useState(
    "Ты — парсер HTML-документов. Анализируй HTML, текст с изображений и отвечай строго по заданному формату. Не придумывай данные, которых нет в источниках."
  );
  const [userPrompt, setUserPrompt] = useState(
    "Извлеки название конференции, даты, место проведения, дедлайны, секции, стоимость участия и контактные данные. Если какого-то поля нет — явно укажи это."
  );

  const [result, setResult] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [sessionId, setSessionId] = useState("");
  const [isSessionLoading, setIsSessionLoading] = useState(false);

  const [taskId, setTaskId] = useState("");
  const [taskStatus, setTaskStatus] = useState("");
  const [taskMessage, setTaskMessage] = useState("");
  const [isPollingStatus, setIsPollingStatus] = useState(false);
  const [lastSuccessfulTaskId, setLastSuccessfulTaskId] = useState("");

  const submittingRef = useRef(false);
  const isMountedRef = useRef(true);
  const pollingIntervalRef = useRef(null);
  const resultRequestedRef = useRef(false);
  const taskIdRef = useRef("");

  useEffect(() => {
    isMountedRef.current = true;
    debugLog("mount", "component mounted", { isMounted: isMountedRef.current });
    loadSessionId();

    return () => {
      debugLog("unmount", "component unmounted, clearing interval");
      isMountedRef.current = false;
      stopPolling("component cleanup");
    };
  }, []);

  useEffect(() => {
    debugLog("state", {
      sessionId,
      taskId,
      taskStatus,
      taskMessage,
      isPollingStatus,
      result,
      error,
    });
  }, [sessionId, taskId, taskStatus, taskMessage, isPollingStatus, result, error]);

  async function loadSessionId() {
    setIsSessionLoading(true);
    setError("");
    debugLog("session", "requesting sessionId");

    try {
      const response = await fetch("/api/sessionId", {
        method: "GET",
        headers: {
          Accept: "text/plain, application/json",
        },
        cache: "no-store",
      });

      debugLog("session", "sessionId response HTTP", response.status);

      if (!response.ok) {
        throw new Error(`Не удалось получить sessionId: ${response.status}`);
      }

      const rawText = await response.text();
      const nextSessionId = String(rawText || "").trim().replace(/^"|"$/g, "");

      debugLog("session", "received sessionId", nextSessionId);

      if (!nextSessionId) {
        throw new Error("Сервер вернул пустой sessionId.");
      }

      if (isMountedRef.current) {
        setSessionId(nextSessionId);
        setGarageImages([]);
        setGarageHtmlFiles([]);
      }
    } catch (e) {
      debugLog("session", "sessionId request failed", e);
      if (isMountedRef.current) {
        setError(e.message || "Ошибка при получении sessionId.");
      }
    } finally {
      if (isMountedRef.current) {
        setIsSessionLoading(false);
      }
    }
  }

  function stopPolling(reason = "no reason") {
    debugLog("polling", "stopPolling called", reason, { currentTaskId: taskIdRef.current });

    if (pollingIntervalRef.current) {
      window.clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }

    if (isMountedRef.current) {
      setIsPollingStatus(false);
    }
  }

  async function fetchPipelineResult(nextTaskId) {
    if (resultRequestedRef.current) {
      debugLog("result", "result already requested, skip", nextTaskId);
      return;
    }

    resultRequestedRef.current = true;
    debugLog("result", "requesting pipeline result", nextTaskId);

    const response = await fetch(`/api/pipeline/${nextTaskId}/result`, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      cache: "no-store",
    });

    debugLog("result", "result response status", response.status);

    if (!response.ok) {
      throw new Error(`Не удалось получить результат задачи: ${response.status}`);
    }

    const data = await response.json();
    debugLog("result", "result JSON", data);

    const llmOutput = data?.llmResponse?.llmOutput;
    const htmlPath = data?.htmlParserResponse?.htmlPath;
    const imagePaths = Array.isArray(data?.htmlParserResponse?.imagePaths)
      ? data.htmlParserResponse.imagePaths
      : [];

    if (!isMountedRef.current) {
      return;
    }

    if (htmlPath) {
      const htmlItem = {
        id: crypto.randomUUID(),
        name: getFileNameFromPath(htmlPath, "page.html"),
        size: undefined,
        filePath: htmlPath,
      };

      setGarageHtmlFiles((prev) => appendUniqueFiles(prev, [htmlItem]));
      debugLog("result", "html path added to storage section", htmlPath);
    }

    if (imagePaths.length > 0) {
      const imageItems = imagePaths.map((imagePath) => ({
        id: crypto.randomUUID(),
        name: getFileNameFromPath(imagePath, "image"),
        size: undefined,
        filePath: imagePath,
        previewUrl: buildStoredFileUrl(imagePath),
      }));

      setGarageImages((prev) => appendUniqueFiles(prev, imageItems));
      debugLog("result", "image paths added to storage section", imagePaths);
    }

    setLastSuccessfulTaskId(nextTaskId);
    setResult(llmOutput || "Сервер завершил задачу, но поле llmResponse.llmOutput оказалось пустым.");
  }

  async function requestTaskStatus(nextTaskId) {
    debugLog("status", "sending status request", nextTaskId);

    const response = await fetch(`/api/pipeline/${nextTaskId}/status`, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
      cache: "no-store",
    });

    debugLog("status", "status response HTTP", response.status);

    if (!response.ok) {
      throw new Error(`Не удалось получить статус задачи: ${response.status}`);
    }

    const data = await response.json();
    debugLog("status", "status JSON", data);

    const normalizedStatus = normalizeTaskStatus(data?.status);
    const message = String(data?.message || "");
    const responseTaskId = String(data?.taskId || nextTaskId).trim();

    debugLog("status", "parsed status", {
      responseTaskId,
      rawStatus: data?.status,
      normalizedStatus,
      message,
      currentRenderedStatus: taskStatus,
    });

    if (!isMountedRef.current) {
      debugLog("status", "component unmounted, skip state apply");
      return;
    }

    debugLog("status", "applying status to state", normalizedStatus);
    setTaskId(responseTaskId);
    setTaskStatus(normalizedStatus);
    setTaskMessage(message);

    if (normalizedStatus === "DONE") {
      debugLog("status", "DONE received, stop polling and fetch result");
      stopPolling("DONE received");
      await fetchPipelineResult(nextTaskId);
      return;
    }

    if (normalizedStatus === "FAILED") {
      debugLog("status", "FAILED received, stop polling and set error");
      stopPolling("FAILED received");
      setError(message || "Сервер сообщил об ошибке при выполнении задачи.");
    }
  }

  function startPolling(nextTaskId) {
    debugLog("polling", "startPolling called", nextTaskId);
    stopPolling("restart before starting new polling");
    taskIdRef.current = nextTaskId;

    if (isMountedRef.current) {
      setIsPollingStatus(true);
    }

    requestTaskStatus(nextTaskId).catch((e) => {
      debugLog("polling", "initial polling request failed", e);
      stopPolling("initial status request failed");
      if (isMountedRef.current) {
        setError(e.message || "Ошибка при получении статуса задачи.");
      }
    });

    pollingIntervalRef.current = window.setInterval(() => {
      debugLog("polling", "interval tick", { taskId: nextTaskId });
      requestTaskStatus(nextTaskId).catch((e) => {
        debugLog("polling", "interval polling failed", e);
        stopPolling("interval status request failed");
        if (isMountedRef.current) {
          setError(e.message || "Ошибка при получении статуса задачи.");
        }
      });
    }, 5000);

    debugLog("polling", "interval started", pollingIntervalRef.current);
  }

  function getContentDispositionFileName(headerValue) {
    if (!headerValue) return "";

    const utf8Match = headerValue.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) {
      try {
        return decodeURIComponent(utf8Match[1]);
      } catch {
        return utf8Match[1];
      }
    }

    const asciiMatch = headerValue.match(/filename="?([^";]+)"?/i);
    return asciiMatch?.[1] || "";
  }

  async function uploadStoredFiles(files, fileType) {
    if (!sessionId.trim()) {
      throw new Error("Session ID ещё не получен от сервера. Дождитесь инициализации страницы.");
    }

    const uploadedItems = [];

    for (const file of files) {
      const formData = new FormData();
      formData.append("sessionId", sessionId);
      formData.append("fileType", fileType);
      formData.append("file", file);

      debugLog("files", "uploading file", { sessionId, fileType, name: file.name, size: file.size });

      const response = await fetch("/api/files/upload", {
        method: "POST",
        body: formData,
      });

      debugLog("files", "upload response HTTP", response.status);

      if (!response.ok) {
        throw new Error(`Не удалось загрузить файл ${file.name}: ${response.status}`);
      }

      const rawText = await response.text();
      const filePath = String(rawText || "").trim().replace(/^"|"$/g, "");

      if (!filePath) {
        throw new Error(`Сервер вернул пустой путь для файла ${file.name}.`);
      }

      uploadedItems.push({
        id: crypto.randomUUID(),
        name: file.name,
        size: file.size,
        filePath,
        previewUrl: fileType === "IMG" ? URL.createObjectURL(file) : "",
      });
    }

    return uploadedItems;
  }

  async function downloadStoredFile(filePath, fallbackName) {
    const response = await fetch(`/api/files/download?filePath=${encodeURIComponent(filePath)}`, {
      method: "GET",
      headers: {
        Accept: "application/octet-stream",
      },
      cache: "no-store",
    });

    debugLog("files", "download response HTTP", response.status, { filePath });

    if (!response.ok) {
      throw new Error(`Не удалось скачать файл: ${response.status}`);
    }

    const blob = await response.blob();
    const fileName =
      getContentDispositionFileName(response.headers.get("Content-Disposition")) ||
      fallbackName ||
      "file";

    const blobUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = blobUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(blobUrl);
  }

  async function handleUploadImages(event) {
    const files = Array.from(event.target.files || []);
    if (!files.length) return;

    setGarageUploading(true);
    setError("");

    try {
      const uploadedItems = await uploadStoredFiles(files, "IMG");
      setGarageImages((prev) => [...prev, ...uploadedItems]);
      event.target.value = "";
    } catch (e) {
      setError(e.message || "Ошибка при загрузке изображений.");
    } finally {
      setGarageUploading(false);
    }
  }

  async function handleUploadHtmlFiles(event) {
    const files = Array.from(event.target.files || []);
    if (!files.length) return;

    setHtmlUploading(true);
    setError("");

    try {
      const uploadedItems = await uploadStoredFiles(files, "HTML");
      setGarageHtmlFiles((prev) => [...prev, ...uploadedItems]);
      event.target.value = "";
    } catch (e) {
      setError(e.message || "Ошибка при загрузке HTML-файлов.");
    } finally {
      setHtmlUploading(false);
    }
  }

  function toggleCleanupTag(tag) {
    setCleanupTags((prev) =>
      prev.includes(tag) ? prev.filter((item) => item !== tag) : [...prev, tag]
    );
  }

  async function handleDownloadImage(image) {
    try {
      await downloadStoredFile(image.filePath, image.name || "image");
    } catch (e) {
      setError(e.message || "Ошибка при скачивании изображения.");
    }
  }

  async function handleDownloadHtmlFile(file) {
    try {
      await downloadStoredFile(file.filePath, file.name || "page.html");
    } catch (e) {
      setError(e.message || "Ошибка при скачивании HTML-файла.");
    }
  }

  function handleDeleteImage(imageId) {
    setGarageImages((prev) => {
      const target = prev.find((image) => image.id === imageId);
      if (target?.previewUrl?.startsWith("blob:")) {
        URL.revokeObjectURL(target.previewUrl);
      }
      return prev.filter((image) => image.id !== imageId);
    });
  }

  function handleDeleteHtmlFile(fileId) {
    setGarageHtmlFiles((prev) => prev.filter((file) => file.id !== fileId));
  }

  async function handleStartParsing() {
    if (submittingRef.current || isPollingStatus) {
      debugLog("pipeline", "start blocked", {
        submitting: submittingRef.current,
        isPollingStatus,
      });
      return;
    }

    submittingRef.current = true;
    setIsSubmitting(true);
    setError("");
    setResult("");
    setTaskStatus("CREATED");
    setTaskMessage("Задача отправлена на сервер и ожидает запуска.");
    setTaskId("");
    setLastSuccessfulTaskId("");
    resultRequestedRef.current = false;
    stopPolling("before starting new pipeline");

    if (!sessionId.trim()) {
      const message = "Session ID ещё не получен от сервера. Дождитесь инициализации страницы.";
      debugLog("pipeline", message);
      setError(message);
      submittingRef.current = false;
      setIsSubmitting(false);
      return;
    }

    const payload = {
      parsing: {
        url: siteUrl.trim(),
        downloadImages: downloadImagesFromSite,
        headers: useHeaders ? buildMapFromRows(headers) : {},
        cookies: useCookies ? buildMapFromRows(cookies) : {},
        proxy: buildProxyPayload(useProxy, proxyConfig),
        pageComplexity: parsingComplexity,
        additionalPageLoadTimeoutS: Number(extraWaitSeconds) || 0,
      },
      preprocessing: buildPreprocessingPayload(cleanupTags),
      recognition: {},
      llm: {
        modelName: model,
        systemMessage: systemPrompt,
        userMessage: userPrompt,
        temperature,
        maxOutputTokens,
      },
    };

    debugLog("pipeline", "sending pipeline payload", { sessionId, payload });

    try {
      const response = await fetch(`/api/pipeline/${encodeURIComponent(sessionId)}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify(payload),
      });

      debugLog("pipeline", "pipeline response HTTP", response.status);

      if (!response.ok) {
        throw new Error(`Бекенд вернул ошибку при запуске pipeline: ${response.status}`);
      }

      const data = await response.json();
      debugLog("pipeline", "pipeline start JSON", data);

      const nextTaskId = String(data?.taskId || "").trim();

      if (!nextTaskId) {
        throw new Error("Сервер не вернул taskId для дальнейшего отслеживания.");
      }

      debugLog("pipeline", "received taskId", nextTaskId);
      taskIdRef.current = nextTaskId;
      setTaskId(nextTaskId);
      setTaskStatus("CREATED");
      setTaskMessage(
        "Задача поставлена в очередь. Статус будет обновляться автоматически каждые 5 секунд."
      );
      startPolling(nextTaskId);
    } catch (e) {
      debugLog("pipeline", "pipeline start failed", e);
      stopPolling("pipeline start failed");
      setError(e.message || "Ошибка при запуске pipeline.");
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  }

  const canStartParsing =
    Boolean(siteUrl.trim()) && Boolean(sessionId.trim()) && !isSessionLoading;
  const statusMeta = getStatusMeta(taskStatus);

  return (
    <>
      <StatusOverlay
        visible={isPollingStatus}
        taskId={taskId}
        status={taskStatus}
        message={taskMessage}
        sessionId={sessionId}
      />

      <div className="min-h-screen bg-slate-50 text-slate-900">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          <motion.header
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
            className="mb-8 flex flex-col gap-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm lg:flex-row lg:items-end lg:justify-between"
          >
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium uppercase tracking-wide text-slate-600">
                Conference Parser
              </div>
              <h1 className="text-3xl font-semibold tracking-tight text-slate-950">
                Минималистичный фронт для парсинга сайтов конференций
              </h1>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
                Настройте параметры загрузки страницы, предобработку HTML, дополнительные HTML-файлы,
                изображения для OCR и запрос к LLM. Затем запустите процесс и при необходимости
                отредактируйте итоговый ответ.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs text-slate-500">Теги очистки</div>
                <div className="mt-1 text-lg font-semibold">{cleanupTags.length}</div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs text-slate-500">HTML файлов</div>
                <div className="mt-1 text-lg font-semibold">{garageHtmlFiles.length}</div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs text-slate-500">Изображений</div>
                <div className="mt-1 text-lg font-semibold">{garageImages.length}</div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs text-slate-500">Модель</div>
                <div className="mt-1 text-sm font-semibold">{model}</div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs text-slate-500">Режим</div>
                <div className="mt-1 text-lg font-semibold">{parsingComplexity}</div>
              </div>
            </div>
          </motion.header>

          <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
            <div className="space-y-6">
              <Section
                icon={Globe}
                title="Блок 1. Извлечение данных со страницы"
                description="URL страницы, параметры парсинга, headers, cookies, proxy и HTML-файлы из S3. HTML-файлы автоматически входят в общий контекст."
              >
                <div className="space-y-5">
                  <div className="grid gap-5 lg:grid-cols-2">
                    <label className="block lg:col-span-2">
                      <span className="mb-2 block text-sm font-medium text-slate-700">Ссылка на сайт</span>
                      <input
                        type="url"
                        value={siteUrl}
                        onChange={(e) => setSiteUrl(e.target.value)}
                        placeholder="https://conference-site.org"
                        className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
                      />
                    </label>

                    <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                      <input
                        type="checkbox"
                        checked={downloadImagesFromSite}
                        onChange={(e) => setDownloadImagesFromSite(e.target.checked)}
                        className="h-4 w-4 rounded border-slate-300"
                      />
                      <span className="text-sm text-slate-700">Скачивать изображения с сайта</span>
                    </label>

                    <label className="block">
                      <span className="mb-2 block text-sm font-medium text-slate-700">Сложность парсинга</span>
                      <select
                        value={parsingComplexity}
                        onChange={(e) => setParsingComplexity(e.target.value)}
                        className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
                      >
                        {PARSING_COMPLEXITY.map((level) => (
                          <option key={level} value={level}>
                            {level}
                          </option>
                        ))}
                      </select>
                    </label>

                    <div className="lg:col-span-2">
                      <RangeField
                        label="Дополнительное ожидание загрузки (сек.)"
                        value={extraWaitSeconds}
                        min={0}
                        max={100}
                        step={1}
                        inputStep={1}
                        onChange={setExtraWaitSeconds}
                        leftLabel="0"
                        rightLabel="100"
                      />
                    </div>
                  </div>

                  <KeyValueEditor
                    title="Headers"
                    rows={headers}
                    setRows={setHeaders}
                    placeholderKey="Authorization"
                    placeholderValue="Bearer ..."
                    useSection={useHeaders}
                    setUseSection={setUseHeaders}
                    checkboxLabel="Использовать headers"
                  />

                  <KeyValueEditor
                    title="Cookies"
                    rows={cookies}
                    setRows={setCookies}
                    placeholderKey="sessionid"
                    placeholderValue="abc123"
                    useSection={useCookies}
                    setUseSection={setUseCookies}
                    checkboxLabel="Использовать cookies"
                  />

                  <ProxyEditor
                    useProxy={useProxy}
                    setUseProxy={setUseProxy}
                    proxyConfig={proxyConfig}
                    setProxyConfig={setProxyConfig}
                  />

                  <StorageSection
                    title="HTML-файлы в Garage/S3"
                    description="Загрузите собственные HTML-файлы страниц. Все добавленные HTML-файлы автоматически учитываются в общем контексте."
                    uploadLabel="Загрузить HTML-файлы"
                    uploading={htmlUploading}
                    accept=".html,text/html"
                    onUpload={handleUploadHtmlFiles}
                    emptyText="В хранилище пока нет HTML-файлов."
                    items={garageHtmlFiles}
                    variant="file"
                    onDownload={handleDownloadHtmlFile}
                    onDelete={handleDeleteHtmlFile}
                    deletingId={htmlDeletingId}
                  />
                </div>
              </Section>

              <Section
                icon={Settings}
                title="Блок 2. Предобработка данных"
                description="Выберите HTML-теги, которые нужно почистить или удалить перед отправкой в LLM."
              >
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
                  {CLEANUP_TAGS.map((tag) => {
                    const checked = cleanupTags.includes(tag);
                    return (
                      <label
                        key={tag}
                        className={`flex cursor-pointer items-center gap-3 rounded-2xl border px-4 py-3 text-sm transition ${
                          checked
                            ? "border-slate-900 bg-slate-900 text-white"
                            : "border-slate-200 bg-slate-50 text-slate-700 hover:bg-white"
                        }`}
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleCleanupTag(tag)}
                          className="hidden"
                        />
                        <span className="font-medium">{tag}</span>
                      </label>
                    );
                  })}
                </div>
              </Section>

              <Section
                icon={ImageIcon}
                title="Блок 3. Извлечение текста с изображений"
                description="Загрузка, скачивание и удаление изображений через Garage/S3. Все добавленные изображения автоматически входят в общий контекст."
              >
                <StorageSection
                  title="Изображения для OCR"
                  description="Добавьте изображения для OCR. Все изображения автоматически учитываются без дополнительного выбора."
                  uploadLabel="Загрузить изображения"
                  uploading={garageUploading}
                  accept="image/*"
                  onUpload={handleUploadImages}
                  emptyText="В хранилище пока нет изображений."
                  items={garageImages}
                  variant="image"
                  onDownload={handleDownloadImage}
                  onDelete={handleDeleteImage}
                  deletingId={imageDeletingId}
                />
              </Section>

              <Section
                icon={Brain}
                title="Блок 4. Запрос к LLM модели"
                description="Выбор модели, настройки генерации, системное сообщение и основной пользовательский запрос."
              >
                <div className="space-y-5">
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">Модель</span>
                    <select
                      value={model}
                      onChange={(e) => setModel(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
                    >
                      {MODELS.map((item) => (
                        <option key={item} value={item}>
                          {item}
                        </option>
                      ))}
                    </select>
                  </label>

                  <div className="grid gap-4 lg:grid-cols-2">
                    <RangeField
                      label="Температура"
                      value={temperature}
                      min={0}
                      max={2}
                      step={0.1}
                      inputStep={0.1}
                      onChange={setTemperature}
                      leftLabel="0.0"
                      rightLabel="2.0"
                    />
                    <RangeField
                      label="Макс. выходных токенов"
                      value={maxOutputTokens}
                      min={128}
                      max={8192}
                      step={128}
                      inputStep={1}
                      onChange={setMaxOutputTokens}
                      leftLabel="128"
                      rightLabel="8192"
                    />
                  </div>

                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">Системное сообщение</span>
                    <textarea
                      value={systemPrompt}
                      onChange={(e) => setSystemPrompt(e.target.value)}
                      rows={6}
                      placeholder="Опишите поведение модели, формат ответа, ограничения..."
                      className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
                    />
                  </label>

                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">Основной запрос</span>
                    <textarea
                      value={userPrompt}
                      onChange={(e) => setUserPrompt(e.target.value)}
                      rows={6}
                      placeholder="Что именно нужно извлечь со страницы?"
                      className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
                    />
                  </label>
                </div>
              </Section>
            </div>

            <div className="space-y-6">
              <Section
                icon={Play}
                title="Запуск процесса"
                description="Отправка запроса на бекенд и автоматическое отслеживание статуса задачи."
              >
                <div className="space-y-4">
                  <button
                    type="button"
                    onClick={handleStartParsing}
                    disabled={isSubmitting || isPollingStatus || isSessionLoading || !canStartParsing}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {isSubmitting || isPollingStatus || isSessionLoading ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        {isSessionLoading ? "Получаем sessionId..." : "Выполняется обработка..."}
                      </>
                    ) : (
                      <>
                        <Play className="h-4 w-4" />
                        Начать процесс парсинга
                      </>
                    )}
                  </button>

                  {(!siteUrl.trim() || !sessionId.trim() || isSessionLoading) && (
                    <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
                      {!siteUrl.trim()
                        ? "Укажите URL страницы."
                        : isSessionLoading
                          ? "Получаем sessionId от сервера..."
                          : "Session ID ещё не получен от сервера."}
                    </div>
                  )}

                  {error && (
                    <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 whitespace-pre-wrap break-words">
                      {error}
                    </div>
                  )}

                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <div className="mb-4 flex items-center gap-3">
                      {taskStatus === "DONE" ? (
                        <CheckCircle2 className="h-5 w-5 text-emerald-600" />
                      ) : taskStatus === "FAILED" ? (
                        <AlertCircle className="h-5 w-5 text-red-600" />
                      ) : (
                        <Clock3 className="h-5 w-5 text-slate-500" />
                      )}
                      <div>
                        <div className="text-sm font-semibold text-slate-900">Статус задачи</div>
                        <div className="text-xs text-slate-500">{statusMeta.label}</div>
                      </div>
                    </div>

                    <div className="mb-4 h-2 overflow-hidden rounded-full bg-white">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${taskStatus === "FAILED" ? "bg-red-500" : "bg-slate-900"}`}
                        style={{ width: `${statusMeta.progress}%` }}
                      />
                    </div>

                    <div className="grid grid-cols-1 gap-3 text-sm text-slate-600 sm:grid-cols-2">
                      <div>
                        <div className="text-xs text-slate-500">Session ID</div>
                        <div className="mt-1 break-all font-medium text-slate-900">{sessionId || "—"}</div>
                      </div>
                      <div>
                        <div className="text-xs text-slate-500">Task ID</div>
                        <div className="mt-1 break-all font-medium text-slate-900">
                          {taskId || lastSuccessfulTaskId || "—"}
                        </div>
                      </div>
                      <div>
                        <div className="text-xs text-slate-500">Статус из сервера</div>
                        <div className="mt-1 font-medium text-slate-900">{taskStatus || "—"}</div>
                      </div>
                      <div>
                        <div className="text-xs text-slate-500">Выбранная модель</div>
                        <div className="mt-1 font-medium text-slate-900">{model}</div>
                      </div>
                      <div>
                        <div className="text-xs text-slate-500">Сложность</div>
                        <div className="mt-1 font-medium text-slate-900">{parsingComplexity}</div>
                      </div>
                    </div>

                    <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-3">
                      <div className="text-xs text-slate-500">Сообщение от сервера</div>
                      <div className="mt-1 whitespace-pre-wrap break-words text-sm text-slate-700">
                        {taskMessage || "Пока нет дополнительного сообщения."}
                      </div>
                    </div>
                  </div>
                </div>
              </Section>

              <Section
                icon={FileText}
                title="Результат"
                description="После статуса DONE сюда автоматически подставляется llmResponse.llmOutput. Поле можно редактировать вручную."
              >
                <textarea
                  value={result}
                  onChange={(e) => setResult(e.target.value)}
                  rows={24}
                  placeholder="Здесь появится ответ от нейросети..."
                  className="min-h-[420px] w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-6 outline-none transition focus:border-slate-400"
                />
              </Section>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}