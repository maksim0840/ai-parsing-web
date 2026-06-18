import React, { useEffect, useRef, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { authFetch } from "./AuthGate.jsx";
import ParamsPresets from "./ParamsPresets.jsx";
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
  Pencil,
  X,
  Bookmark,
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

let cachedSessionId = "";
let sessionIdRequestPromise = null;

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
  description,
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
            {description}
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
            Прокси-сервер для отправки запросов через другой IP-адрес.
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
  onEditImageText,
  deleting,
}) {
  const isImage = variant === "image";
  const isInvalid = item.valid === false;
  const [isErrorModalOpen, setIsErrorModalOpen] = useState(false);
  const fileDescription = isImage && !isInvalid ? String(item.description || "").trim() : "";
  const firstDescriptionLine = fileDescription
    ? fileDescription.split(/\r?\n/).find((line) => line.trim()) || fileDescription
    : "";
  const errorMessage = String(item.errorMessage || "").trim() || "Файл не прошёл валидацию.";

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
          <div className="flex items-start gap-2">
            <p className="min-w-0 flex-1 truncate text-sm font-semibold text-slate-900">{item.name}</p>
            {isInvalid && (
              <button
                type="button"
                onClick={() => setIsErrorModalOpen(true)}
                className="inline-flex shrink-0 items-center justify-center rounded-lg text-red-600 transition hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-200"
                aria-label="Показать описание ошибки файла"
              >
                <AlertCircle className="h-4 w-4" />
              </button>
            )}
          </div>
          <p className="mt-1 text-xs text-slate-500">{formatBytes(item.size)}</p>
          {isImage && !isInvalid ? (
            <div className="mt-2 border-t border-slate-200 pt-2">
              <div className="flex items-center gap-2">
                <span className="min-w-0 flex-1 truncate text-xs leading-5 text-slate-600">
                  {firstDescriptionLine || "Текст не распознан"}
                </span>
                <button
                  type="button"
                  onClick={() => onEditImageText?.(item)}
                  className="inline-flex h-7 w-7 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-700"
                  title="Открыть полный текст"
                  aria-label="Открыть полный распознанный текст"
                >
                  <Pencil className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          ) : null}
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

      <AnimatePresence>
        {isErrorModalOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/40 px-4"
            onClick={() => setIsErrorModalOpen(false)}
          >
            <motion.div
              initial={{ scale: 0.96, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.98, opacity: 0 }}
              className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-5 shadow-2xl"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-start gap-3">
                <div className="rounded-2xl bg-red-50 p-2.5">
                  <AlertCircle className="h-5 w-5 text-red-600" />
                </div>
                <div className="min-w-0 flex-1">
                  <h4 className="text-sm font-semibold text-slate-950">Ошибка файла</h4>
                  <p className="mt-1 whitespace-pre-wrap break-words text-sm leading-6 text-slate-700">
                    {errorMessage}
                  </p>
                </div>
              </div>

              <div className="mt-4 flex justify-end">
                <button
                  type="button"
                  onClick={() => setIsErrorModalOpen(false)}
                  className="inline-flex items-center justify-center rounded-2xl border border-slate-200 px-4 py-2.5 text-sm text-slate-700 transition hover:bg-slate-50"
                >
                  Закрыть
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
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
  onEditImageText,
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
                onEditImageText={onEditImageText}
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


function ImageTextEditorModal({
  item,
  value,
  onChange,
  onClose,
  onSave,
}) {
  if (!item) {
    return null;
  }

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/45 px-4"
      >
        <motion.div
          initial={{ scale: 0.96, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.98, opacity: 0 }}
          className="flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-2xl"
        >
          <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-4">
            <div className="min-w-0">
              <h3 className="truncate text-lg font-semibold text-slate-950">{item.name}</h3>
              <p className="mt-1 text-sm text-slate-500">
                Полный распознанный текст изображения. Здесь его можно просмотреть и отредактировать.
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:bg-slate-50 hover:text-slate-700"
              aria-label="Закрыть окно редактирования текста"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto px-6 py-5">
            <textarea
              value={value}
              onChange={(e) => onChange(e.target.value)}
              rows={18}
              className="min-h-[360px] w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-6 text-slate-800 outline-none transition focus:border-slate-400"
              placeholder="Здесь появится полный распознанный текст..."
            />
          </div>

          <div className="flex items-center justify-end gap-3 border-t border-slate-200 px-6 py-4">
            <button
              type="button"
              onClick={onClose}
              className="inline-flex items-center justify-center rounded-2xl border border-slate-200 px-4 py-2.5 text-sm text-slate-700 transition hover:bg-slate-50"
            >
              Закрыть
            </button>
            <button
              type="button"
              onClick={onSave}
              className="inline-flex items-center justify-center rounded-2xl bg-slate-950 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800"
            >
              Сохранить
            </button>
          </div>
        </motion.div>
      </motion.div>
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

function buildRowsFromMap(map) {
  if (!map || typeof map !== "object") {
    return [];
  }

  return Object.entries(map)
    .filter(([key]) => String(key).trim())
    .map(([key, value]) => ({
      id: crypto.randomUUID(),
      key: String(key),
      value: String(value ?? ""),
    }));
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

function getFileItemKey(item) {
  return String(item?.filePath || item?.downloadUrl || item?.previewUrl || item?.name || "").trim();
}

function mergeFileItems(existingItem, incomingItem) {
  const merged = {
    ...existingItem,
    ...incomingItem,
    id: incomingItem?.id || existingItem?.id,
    size: incomingItem?.size ?? existingItem?.size,
    name: incomingItem?.name || existingItem?.name,
    fileName: incomingItem?.fileName || existingItem?.fileName,
    filePath: incomingItem?.filePath || existingItem?.filePath,
    fileType: incomingItem?.fileType ?? existingItem?.fileType,
    downloadUrl: incomingItem?.downloadUrl || existingItem?.downloadUrl,
    selected:
      typeof incomingItem?.selected === "boolean"
        ? incomingItem.selected
        : existingItem?.selected,
  };

  if (existingItem?.previewUrl?.startsWith("blob:") && !incomingItem?.previewUrl?.startsWith("blob:")) {
    merged.previewUrl = existingItem.previewUrl;
  } else {
    merged.previewUrl = incomingItem?.previewUrl || existingItem?.previewUrl || "";
  }

  if (
    existingItem?.rawFileInfo &&
    typeof existingItem.rawFileInfo === "object" &&
    incomingItem?.rawFileInfo &&
    typeof incomingItem.rawFileInfo === "object"
  ) {
    merged.rawFileInfo = {
      ...existingItem.rawFileInfo,
      ...incomingItem.rawFileInfo,
      sizeBytes:
        typeof incomingItem.rawFileInfo.sizeBytes === "number"
          ? incomingItem.rawFileInfo.sizeBytes
          : typeof merged.size === "number"
            ? merged.size
            : existingItem.rawFileInfo.sizeBytes,
    };
  } else if (!incomingItem?.rawFileInfo && existingItem?.rawFileInfo) {
    merged.rawFileInfo = {
      ...existingItem.rawFileInfo,
      sizeBytes:
        typeof existingItem.rawFileInfo.sizeBytes === "number"
          ? existingItem.rawFileInfo.sizeBytes
          : typeof merged.size === "number"
            ? merged.size
            : existingItem.rawFileInfo.sizeBytes,
    };
  }

  return merged;
}

function appendUniqueFiles(prevItems, nextItems) {
  const updatedItems = [...prevItems];
  const keyToIndex = new Map(
    prevItems.map((item, index) => [getFileItemKey(item), index]).filter(([key]) => key)
  );

  nextItems.forEach((item) => {
    const key = getFileItemKey(item);

    if (!key) {
      updatedItems.push(item);
      return;
    }

    if (keyToIndex.has(key)) {
      const existingIndex = keyToIndex.get(key);
      updatedItems[existingIndex] = mergeFileItems(updatedItems[existingIndex], item);
      return;
    }

    keyToIndex.set(key, updatedItems.length);
    updatedItems.push(item);
  });

  return updatedItems;
}


function normalizeFileInfoDto(fileInfo, options = {}) {
  if (!fileInfo || typeof fileInfo !== "object") {
    return null;
  }

  const filePath = String(fileInfo.filePath || "").trim();
  const fileName = String(fileInfo.fileName || "").trim();
  const resolvedName = fileName || getFileNameFromPath(filePath, options.fallbackName || "file");
  const isImage = options.variant === "image";
  const normalizedSizeBytes =
    typeof fileInfo.sizeBytes === "number"
      ? fileInfo.sizeBytes
      : typeof fileInfo.size_bytes === "number"
        ? fileInfo.size_bytes
        : typeof options.sizeBytes === "number"
          ? options.sizeBytes
          : undefined;

  return {
    id: filePath || crypto.randomUUID(),
    name: resolvedName,
    fileName: resolvedName,
    size: normalizedSizeBytes,
    filePath,
    fileType: fileInfo.fileType || options.fileType || null,
    description: String(fileInfo.description || ""),
    valid: typeof fileInfo.valid === "boolean" ? fileInfo.valid : true,
    errorMessage: String(fileInfo.errorMessage || ""),
    previewUrl: options.previewUrl ?? (isImage && filePath ? buildStoredFileUrl(filePath) : ""),
    rawFileInfo: {
      ...fileInfo,
      fileName: fileName || resolvedName,
      filePath,
      sizeBytes: normalizedSizeBytes,
    },
  };
}

function normalizeFileInfoDtoList(fileInfos, options = {}) {
  if (!Array.isArray(fileInfos)) {
    return [];
  }

  return fileInfos
    .map((fileInfo) => normalizeFileInfoDto(fileInfo, options))
    .filter(Boolean);
}

function serializeFileInfoDto(fileItem) {
  if (!fileItem || typeof fileItem !== "object") {
    return null;
  }

  if (fileItem.rawFileInfo && typeof fileItem.rawFileInfo === "object") {
    return {
      ...fileItem.rawFileInfo,
      sizeBytes:
        typeof fileItem.rawFileInfo.sizeBytes === "number"
          ? fileItem.rawFileInfo.sizeBytes
          : typeof fileItem.size === "number"
            ? fileItem.size
            : fileItem.rawFileInfo.sizeBytes,
    };
  }

  const filePath = String(fileItem.filePath || "").trim();
  if (!filePath) {
    return null;
  }

  return {
    filePath,
    fileName: String(fileItem.fileName || fileItem.name || getFileNameFromPath(filePath, "file")),
    fileType: fileItem.fileType ?? null,
    sizeBytes: typeof fileItem.size === "number" ? fileItem.size : undefined,
    description: String(fileItem.description || ""),
    valid: typeof fileItem.valid === "boolean" ? fileItem.valid : true,
    errorMessage: String(fileItem.errorMessage || ""),
  };
}

function serializeFileInfoDtoList(fileItems) {
  if (!Array.isArray(fileItems)) {
    return [];
  }

  return fileItems
    .map((fileItem) => serializeFileInfoDto(fileItem))
    .filter(Boolean);
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

  const [imageTextEditorItem, setImageTextEditorItem] = useState(null);
  const [imageTextEditorValue, setImageTextEditorValue] = useState("");

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
  const [isSavingResult, setIsSavingResult] = useState(false);
  const [saveResultMessage, setSaveResultMessage] = useState("");
  const [saveResultError, setSaveResultError] = useState("");
  const [savedResultId, setSavedResultId] = useState("");
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

    try {
      if (cachedSessionId) {
        debugLog("session", "using cached sessionId", cachedSessionId);

        if (isMountedRef.current) {
          setSessionId(cachedSessionId);
          setGarageImages([]);
          setGarageHtmlFiles([]);
        }
        return;
      }

      if (!sessionIdRequestPromise) {
        debugLog("session", "requesting sessionId");

        sessionIdRequestPromise = authFetch("/api/sessionId", {
          method: "GET",
          headers: {
            Accept: "text/plain, application/json",
          },
          cache: "no-store",
        })
          .then(async (response) => {
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

            cachedSessionId = nextSessionId;
            return nextSessionId;
          })
          .catch((error) => {
            sessionIdRequestPromise = null;
            throw error;
          });
      } else {
        debugLog("session", "awaiting existing sessionId request");
      }

      const nextSessionId = await sessionIdRequestPromise;

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

    const response = await authFetch(`/api/pipeline/${nextTaskId}/result`, {
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

    const htmlDocs =
      Array.isArray(data?.htmlPreprocessingResponse?.htmlDocs) && data.htmlPreprocessingResponse.htmlDocs.length > 0
        ? data.htmlPreprocessingResponse.htmlDocs
        : Array.isArray(data?.htmlParserResponse?.htmlDocs)
          ? data.htmlParserResponse.htmlDocs
          : [];

    const imageDocs =
      Array.isArray(data?.textRecognitionResponse?.images) && data.textRecognitionResponse.images.length > 0
        ? data.textRecognitionResponse.images
        : Array.isArray(data?.htmlParserResponse?.images)
          ? data.htmlParserResponse.images
          : [];

    if (!isMountedRef.current) {
      return;
    }

    const normalizedHtmlDocs = normalizeFileInfoDtoList(htmlDocs, {
      variant: "file",
      fallbackName: "page.html",
      fileType: "HTML",
    });

    const normalizedImageDocs = normalizeFileInfoDtoList(imageDocs, {
      variant: "image",
      fallbackName: "image",
      fileType: "IMG",
    });

    if (normalizedHtmlDocs.length > 0) {
      setGarageHtmlFiles((prev) => appendUniqueFiles(prev, normalizedHtmlDocs));
      debugLog("result", "html docs added to storage section", normalizedHtmlDocs);
    }

    if (normalizedImageDocs.length > 0) {
      setGarageImages((prev) => appendUniqueFiles(prev, normalizedImageDocs));
      debugLog("result", "image docs added to storage section", normalizedImageDocs);
    }

    setLastSuccessfulTaskId(nextTaskId);
    setResult(llmOutput || "Сервер завершил задачу, но поле llmResponse.llmOutput оказалось пустым.");
  }

  async function requestTaskStatus(nextTaskId) {
    debugLog("status", "sending status request", nextTaskId);

    const response = await authFetch(`/api/pipeline/${nextTaskId}/status`, {
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

      const response = await authFetch("/api/files/upload", {
        method: "POST",
        body: formData,
      });

      debugLog("files", "upload response HTTP", response.status);

      if (!response.ok) {
        throw new Error(`Не удалось загрузить файл ${file.name}: ${response.status}`);
      }

      const fileInfo = await response.json();
      debugLog("files", "upload response JSON", fileInfo);

      const normalizedItem = normalizeFileInfoDto(fileInfo, {
        variant: fileType === "IMG" ? "image" : "file",
        fallbackName: file.name,
        fileType,
        previewUrl: fileType === "IMG" ? URL.createObjectURL(file) : "",
      });

      if (!normalizedItem?.filePath) {
        throw new Error(`Сервер вернул пустой путь для файла ${file.name}.`);
      }

      if (typeof normalizedItem.size !== "number") {
        normalizedItem.size = file.size;
        if (normalizedItem.rawFileInfo && typeof normalizedItem.rawFileInfo === "object") {
          normalizedItem.rawFileInfo.sizeBytes = file.size;
        }
      }

      uploadedItems.push(normalizedItem);
    }

    return uploadedItems;
  }

  async function deleteStoredFile(filePath) {
    const response = await authFetch(`/api/files/delete?filePath=${encodeURIComponent(filePath)}`, {
      method: "DELETE",
      headers: {
        Accept: "application/json, text/plain, */*",
      },
    });

    debugLog("files", "delete response HTTP", response.status, { filePath });

    if (!response.ok) {
      throw new Error(`Не удалось удалить файл: ${response.status}`);
    }
  }

  async function downloadStoredFile(filePath, fallbackName) {
    const response = await authFetch(`/api/files/download?filePath=${encodeURIComponent(filePath)}`, {
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
      setGarageImages((prev) => appendUniqueFiles(prev, uploadedItems));
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
      setGarageHtmlFiles((prev) => appendUniqueFiles(prev, uploadedItems));
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

  function openImageTextEditor(item) {
    setImageTextEditorItem(item);
    setImageTextEditorValue(String(item?.description || ""));
  }

  function closeImageTextEditor() {
    setImageTextEditorItem(null);
    setImageTextEditorValue("");
  }

  function saveImageTextEditor() {
    if (!imageTextEditorItem) {
      return;
    }

    const nextDescription = imageTextEditorValue;
    const currentId = imageTextEditorItem.id;

    setGarageImages((prev) =>
      prev.map((image) =>
        image.id === currentId
          ? {
              ...image,
              description: nextDescription,
              rawFileInfo:
                image.rawFileInfo && typeof image.rawFileInfo === "object"
                  ? {
                      ...image.rawFileInfo,
                      description: nextDescription,
                      sizeBytes:
                        typeof image.rawFileInfo.sizeBytes === "number"
                          ? image.rawFileInfo.sizeBytes
                          : typeof image.size === "number"
                            ? image.size
                            : image.rawFileInfo.sizeBytes,
                    }
                  : image.rawFileInfo,
            }
          : image
      )
    );

    closeImageTextEditor();
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

  async function handleDeleteImage(imageId) {
    setImageDeletingId(imageId);
    setError("");

    const target = garageImages.find((image) => image.id === imageId);
    if (!target) {
      setImageDeletingId(null);
      return;
    }

    try {
      if (target.filePath) {
        await deleteStoredFile(target.filePath);
      }

      setGarageImages((prev) => {
        const currentTarget = prev.find((image) => image.id === imageId);
        if (currentTarget?.previewUrl?.startsWith("blob:")) {
          URL.revokeObjectURL(currentTarget.previewUrl);
        }
        return prev.filter((image) => image.id !== imageId);
      });
    } catch (e) {
      setError(e.message || "Ошибка при удалении изображения.");
    } finally {
      setImageDeletingId(null);
    }
  }

  async function handleDeleteHtmlFile(fileId) {
    setHtmlDeletingId(fileId);
    setError("");

    const target = garageHtmlFiles.find((file) => file.id === fileId);
    if (!target) {
      setHtmlDeletingId(null);
      return;
    }

    try {
      if (target.filePath) {
        await deleteStoredFile(target.filePath);
      }

      setGarageHtmlFiles((prev) => prev.filter((file) => file.id !== fileId));
    } catch (e) {
      setError(e.message || "Ошибка при удалении HTML-файла.");
    } finally {
      setHtmlDeletingId(null);
    }
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
    setSaveResultMessage("");
    setSaveResultError("");
    setSavedResultId("");
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

    const htmlDocsPayload = serializeFileInfoDtoList(garageHtmlFiles);
    const imageDocsPayload = serializeFileInfoDtoList(garageImages);

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
      preprocessing: {
        htmlDocs: htmlDocsPayload,
        ...buildPreprocessingPayload(cleanupTags),
      },
      recognition: {
        images: imageDocsPayload,
      },
      llm: {
        modelName: model,
        systemMessage: systemPrompt,
        userMessage: userPrompt,
        temperature,
        maxOutputTokens,
        htmlDocs: htmlDocsPayload,
        images: imageDocsPayload,
      },
    };

    debugLog("pipeline", "sending pipeline payload", { sessionId, payload });

    try {
      const response = await authFetch(`/api/pipeline/${encodeURIComponent(sessionId)}`, {
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

  async function handleSaveResult() {
    if (isSavingResult) {
      return;
    }

    const normalizedUrl = String(siteUrl || "").trim();
    const normalizedResult = String(result || "").trim();

    setSaveResultMessage("");
    setSaveResultError("");
    setSavedResultId("");

    if (!normalizedUrl) {
      setSaveResultError("Невозможно сохранить результат без URL страницы.");
      return;
    }

    if (!normalizedResult) {
      setSaveResultError("Невозможно сохранить пустой результат.");
      return;
    }

    setIsSavingResult(true);

    try {
      const response = await authFetch("/api/results/save", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({
          url: normalizedUrl,
          result: normalizedResult,
        }),
      });

      if (!response.ok) {
        const rawText = await response.text();
        let message = String(rawText || "").trim();

        try {
          const parsed = JSON.parse(message);
          message = parsed?.message || parsed?.error || parsed?.details || message;
        } catch {
          // keep plain text
        }

        throw new Error(message || `Не удалось сохранить результат: ${response.status}`);
      }

      const data = await response.json();
      debugLog("results", "save response JSON", data);

      const nextSavedId = String(data?.id || "").trim();
      setSavedResultId(nextSavedId);
      setSaveResultMessage("Результат успешно сохранён в базу данных.");
    } catch (e) {
      setSaveResultError(e.message || "Ошибка при сохранении результата.");
    } finally {
      setIsSavingResult(false);
    }
  }

  function buildParamsPayload() {
    return {
      htmlParserParams: {
        downloadImages: downloadImagesFromSite,
        headers: useHeaders ? buildMapFromRows(headers) : {},
        cookies: useCookies ? buildMapFromRows(cookies) : {},
        proxy: buildProxyPayload(useProxy, proxyConfig) || {},
        pageComplexity: parsingComplexity,
        additionalPageLoadTimeoutS: Number(extraWaitSeconds) || 0,
      },
      htmlPreprocessingParams: {
        noscriptProcessing: cleanupTags.includes("noscript"),
        linkProcessing: cleanupTags.includes("link"),
        styleProcessing: cleanupTags.includes("style"),
        metaProcessing: cleanupTags.includes("meta"),
        scriptProcessing: cleanupTags.includes("script"),
        canvasProcessing: cleanupTags.includes("canvas"),
        svgProcessing: cleanupTags.includes("svg"),
        areaProcessing: cleanupTags.includes("area"),
        imgProcessing: cleanupTags.includes("img"),
        videoProcessing: cleanupTags.includes("video"),
        audioProcessing: cleanupTags.includes("audio"),
        iframeProcessing: cleanupTags.includes("iframe"),
        portalProcessing: cleanupTags.includes("portal"),
        embedProcessing: cleanupTags.includes("embed"),
        objectProcessing: cleanupTags.includes("object"),
        sourceProcessing: cleanupTags.includes("source"),
      },
      llmParams: {
        modelName: model,
        systemMessage: systemPrompt,
        userMessage: userPrompt,
        temperature,
        maxOutputTokens,
      },
    };
  }

  function applyLoadedParams(param) {
    if (!param || typeof param !== "object") {
      return;
    }

    const parser = param.htmlParserParams && typeof param.htmlParserParams === "object" ? param.htmlParserParams : {};
    const pre =
      param.htmlPreprocessingParams && typeof param.htmlPreprocessingParams === "object"
        ? param.htmlPreprocessingParams
        : {};
    const llm = param.llmParams && typeof param.llmParams === "object" ? param.llmParams : {};

    if (typeof parser.downloadImages === "boolean") {
      setDownloadImagesFromSite(parser.downloadImages);
    }

    const headerRows = buildRowsFromMap(parser.headers);
    if (headerRows.length > 0) {
      setUseHeaders(true);
      setHeaders(headerRows);
    } else {
      setUseHeaders(false);
      setHeaders([createKeyValueRow()]);
    }

    const cookieRows = buildRowsFromMap(parser.cookies);
    if (cookieRows.length > 0) {
      setUseCookies(true);
      setCookies(cookieRows);
    } else {
      setUseCookies(false);
      setCookies([createKeyValueRow()]);
    }

    const proxyObj = parser.proxy && typeof parser.proxy === "object" ? parser.proxy : null;
    const hasProxy = proxyObj && Object.values(proxyObj).some((value) => String(value ?? "").trim());
    if (hasProxy) {
      setUseProxy(true);
      setProxyConfig({
        ip: String(proxyObj.ip ?? ""),
        port: String(proxyObj.port ?? ""),
        username: String(proxyObj.username ?? ""),
        password: String(proxyObj.password ?? ""),
      });
    } else {
      setUseProxy(false);
      setProxyConfig({ ip: "", port: "", username: "", password: "" });
    }

    if (parser.pageComplexity && PARSING_COMPLEXITY.includes(parser.pageComplexity)) {
      setParsingComplexity(parser.pageComplexity);
    }

    if (parser.additionalPageLoadTimeoutS != null && !Number.isNaN(Number(parser.additionalPageLoadTimeoutS))) {
      setExtraWaitSeconds(clampNumber(Number(parser.additionalPageLoadTimeoutS), 0, 100));
    }

    const tagFieldMap = {
      noscript: "noscriptProcessing",
      link: "linkProcessing",
      style: "styleProcessing",
      meta: "metaProcessing",
      script: "scriptProcessing",
      canvas: "canvasProcessing",
      svg: "svgProcessing",
      area: "areaProcessing",
      img: "imgProcessing",
      video: "videoProcessing",
      audio: "audioProcessing",
      iframe: "iframeProcessing",
      portal: "portalProcessing",
      embed: "embedProcessing",
      object: "objectProcessing",
      source: "sourceProcessing",
    };

    const hasAnyPreField = Object.values(tagFieldMap).some((field) => pre[field] != null);
    if (hasAnyPreField) {
      const nextTags = CLEANUP_TAGS.filter((tag) => pre[tagFieldMap[tag]] === true);
      setCleanupTags(nextTags);
    }

    if (typeof llm.modelName === "string" && llm.modelName.trim()) {
      setModel(llm.modelName);
    }
    if (typeof llm.systemMessage === "string") {
      setSystemPrompt(llm.systemMessage);
    }
    if (typeof llm.userMessage === "string") {
      setUserPrompt(llm.userMessage);
    }
    if (llm.temperature != null && !Number.isNaN(Number(llm.temperature))) {
      setTemperature(clampNumber(Number(llm.temperature), 0, 2));
    }
    if (llm.maxOutputTokens != null && !Number.isNaN(Number(llm.maxOutputTokens))) {
      setMaxOutputTokens(clampNumber(Number(llm.maxOutputTokens), 128, 8192));
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
      <ImageTextEditorModal
        item={imageTextEditorItem}
        value={imageTextEditorValue}
        onChange={setImageTextEditorValue}
        onClose={closeImageTextEditor}
        onSave={saveImageTextEditor}
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
                AI Parse
              </div>
              <h1 className="text-3xl font-semibold tracking-tight text-slate-950">
                Автоматический сбор и структурирование данных с веб-страниц
              </h1>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
                Загрузите ссылку на сайт, укажите нужные поля и настройте запрос к нейросети. Сервис
                проанализирует HTML-код и изображения страницы, извлечёт данные и представит
                результат в структурированном виде.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-5">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs text-slate-500">Теги очистки</div>
                <div className="mt-1 text-lg font-semibold">{cleanupTags.length}</div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs text-slate-500">HTML файлы</div>
                <div className="mt-1 text-lg font-semibold">{garageHtmlFiles.length}</div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs text-slate-500">Изображения</div>
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
                icon={Bookmark}
                title="Сохранённые параметры"
                description="Выберите ранее сохранённый набор параметров, чтобы подставить его в форму, либо сохраните текущие настройки под новым названием. URL, загруженные файлы и изображения в набор не входят."
              >
                <ParamsPresets
                  buildParams={buildParamsPayload}
                  applyParams={applyLoadedParams}
                />
              </Section>

              <Section
                icon={Globe}
                title="Блок 1. Извлечение данных со страницы"
                description="Укажите URL-адрес страницы и настройте параметры парсинга для загрузки HTML-кода выбранной веб-страницы."
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
                    description="Дополнительные HTTP-заголовки браузера для имитации пользовательских запросов."
                    rows={headers}
                    setRows={setHeaders}
                    placeholderKey="User-Agent"
                    placeholderValue="Mozilla/5.0 (Windows NT 10.0; Win64; x64) ..."
                    useSection={useHeaders}
                    setUseSection={setUseHeaders}
                    checkboxLabel="Использовать headers"
                  />

                  <KeyValueEditor
                    title="Cookies"
                    description="Параметры cookies для доступа к страницам с пользовательской авторизацией и сохранёнными настройками."
                    rows={cookies}
                    setRows={setCookies}
                    placeholderKey="session-id"
                    placeholderValue="YK7D6c4GahPcyumIDobsybMNyIvVC9zj7 ..."
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
                    title="HTML-файлы"
                    description="Добавьте HTML-файлы страниц. Все файлы автоматически учитываются в общем контексте."
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
                description="Выберите HTML-теги для очистки и упрощения. Это сократит объём анализируемого контекста."
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
                description="Добавьте изображения для OCR-анализа, чтобы извлечь с них текст и использовать его при обработке."
              >
                <StorageSection
                  title="Изображения для OCR"
                  description="Добавьте изображения для OCR. Все изображения автоматически учитываются в общем контексте."
                  uploadLabel="Загрузить изображения"
                  uploading={garageUploading}
                  accept="image/*"
                  onUpload={handleUploadImages}
                  emptyText="В хранилище пока нет изображений."
                  items={garageImages}
                  variant="image"
                  onDownload={handleDownloadImage}
                  onDelete={handleDeleteImage}
                  onEditImageText={openImageTextEditor}
                  deletingId={imageDeletingId}
                />
              </Section>

              <Section
                icon={Brain}
                title="Блок 4. Запрос к LLM модели"
                description="Выберите LLM-модель и задайте параметры обработки. В системном и пользовательском сообщении опишите задачу извлечения данных и требуемый формат ответа."
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
                description="Отправка запроса на сервер для обработки контекста и формирования итогового результата."
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
                description="Здесь появится структурированный результат обработки данных, сформированный выбранной нейросетевой моделью."
              >
                <textarea
                  value={result}
                  onChange={(e) => {
                    setResult(e.target.value);
                    setSaveResultMessage("");
                    setSaveResultError("");
                    setSavedResultId("");
                  }}
                  rows={24}
                  placeholder="Здесь появится ответ от нейросети..."
                  className="min-h-[420px] w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-6 outline-none transition focus:border-slate-400"
                />

                <div className="mt-4 space-y-3">
                  <button
                    type="button"
                    onClick={handleSaveResult}
                    disabled={isSavingResult || !result.trim() || !siteUrl.trim()}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {isSavingResult ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Сохраняем результат...
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="h-4 w-4" />
                        Сохранить json в базу данных
                      </>
                    )}
                  </button>

                  {saveResultMessage ? (
                    <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 whitespace-pre-wrap break-words">
                      {saveResultMessage}
                    </div>
                  ) : null}

                  {saveResultError ? (
                    <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 whitespace-pre-wrap break-words">
                      {saveResultError}
                    </div>
                  ) : null}
                </div>
              </Section>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}