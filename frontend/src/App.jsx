import React, { useEffect, useMemo, useRef, useState } from "react";
import { motion } from "framer-motion";
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
  RefreshCw,
  FileCode2,
  Shield,
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

function createKeyValueRow() {
  return { id: crypto.randomUUID(), key: "", value: "" };
}

function KeyValueEditor({ title, rows, setRows, placeholderKey, placeholderValue }) {
  const addRow = () => {
    setRows((prev) => [...prev, createKeyValueRow()]);
  };

  const updateRow = (id, field, value) => {
    setRows((prev) => prev.map((row) => (row.id === id ? { ...row, [field]: value } : row)));
  };

  const removeRow = (id) => {
    setRows((prev) => prev.filter((row) => row.id !== id));
  };

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
        <button
          type="button"
          onClick={addRow}
          className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
        >
          <Plus className="h-4 w-4" />
          Добавить
        </button>
      </div>

      <div className="space-y-3">
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
    </div>
  );
}

function ProxyEditor({ useProxy, setUseProxy, proxyConfig, setProxyConfig }) {
  const updateProxyField = (field, value) => {
    setProxyConfig((prev) => ({ ...prev, [field]: value }));
  };

  const resetProxy = () => {
    setUseProxy(false);
    setProxyConfig({ ip: "", port: "", username: "", password: "" });
  };

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">Proxy</h3>
          <p className="mt-1 text-xs text-slate-500">
            Можно указать только поля: ip, port, username, password. Каждое поле — не более одного раза.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {useProxy && (
            <button
              type="button"
              onClick={resetProxy}
              className="rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
            >
              Очистить
            </button>
          )}
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
  selectableLabel,
  onToggleSelect,
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

        {typeof onToggleSelect === "function" && (
          <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5">
            <input
              type="checkbox"
              checked={item.selected}
              onChange={() => onToggleSelect(item.id)}
              className="h-4 w-4 rounded border-slate-300"
            />
            <span className="text-sm text-slate-700">{selectableLabel}</span>
          </label>
        )}

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
  loading,
  accept,
  multiple = true,
  onUpload,
  onRefresh,
  emptyText,
  items,
  variant,
  selectableLabel,
  onToggleSelect,
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

          <button
            type="button"
            onClick={onRefresh}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Обновить список
          </button>
        </div>

        {items.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
            {loading ? "Загрузка..." : emptyText}
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {items.map((item) => (
              <StorageFileCard
                key={item.id}
                item={item}
                variant={variant}
                selectableLabel={selectableLabel}
                onToggleSelect={onToggleSelect}
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

function RangeField({ label, value, min, max, step, onChange, leftLabel, rightLabel }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <span className="text-sm font-medium text-slate-700">{label}</span>
        <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-sm font-semibold text-slate-900">
          {value}
        </span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-200"
      />
      <div className="mt-2 flex items-center justify-between text-xs text-slate-500">
        <span>{leftLabel ?? min}</span>
        <span>{rightLabel ?? max}</span>
      </div>
    </div>
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
  const [headers, setHeaders] = useState([createKeyValueRow()]);
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

  const [cleanupTags, setCleanupTags] = useState([
    "script",
    "style",
    "noscript",
    "iframe",
    "embed",
    "object",
  ]);

  const [garageImages, setGarageImages] = useState([]);
  const [garageLoading, setGarageLoading] = useState(false);
  const [garageUploading, setGarageUploading] = useState(false);
  const [imageDeletingId, setImageDeletingId] = useState(null);

  const [garageHtmlFiles, setGarageHtmlFiles] = useState([]);
  const [htmlLoading, setHtmlLoading] = useState(false);
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
  const [lastPayload, setLastPayload] = useState(null);

  const submittingRef = useRef(false);

  useEffect(() => {
    loadGarageImages();
    loadGarageHtmlFiles();
  }, []);

  async function loadGarageImages() {
    setGarageLoading(true);
    setError("");

    try {
      const response = await fetch("/api/images", { method: "GET" });
      if (!response.ok) {
        throw new Error("Не удалось получить список изображений из хранилища.");
      }

      const data = await response.json();
      const normalized = (data.images || []).map((image) => ({
        id: image.id,
        name: image.name,
        size: image.size,
        previewUrl: image.previewUrl || image.downloadUrl || "",
        downloadUrl: image.downloadUrl || "",
        selected: Boolean(image.selectedForOcr),
      }));

      setGarageImages(normalized);
    } catch (e) {
      setError(e.message || "Ошибка при загрузке изображений.");
    } finally {
      setGarageLoading(false);
    }
  }

  async function loadGarageHtmlFiles() {
    setHtmlLoading(true);
    setError("");

    try {
      const response = await fetch("/api/html-files", { method: "GET" });
      if (!response.ok) {
        throw new Error("Не удалось получить список HTML-файлов из хранилища.");
      }

      const data = await response.json();
      const normalized = (data.files || []).map((file) => ({
        id: file.id,
        name: file.name,
        size: file.size,
        downloadUrl: file.downloadUrl || "",
        selected: Boolean(file.selectedForParsing),
      }));

      setGarageHtmlFiles(normalized);
    } catch (e) {
      setError(e.message || "Ошибка при загрузке HTML-файлов.");
    } finally {
      setHtmlLoading(false);
    }
  }

  async function handleUploadImages(event) {
    const files = Array.from(event.target.files || []);
    if (!files.length) return;

    setGarageUploading(true);
    setError("");

    try {
      const formData = new FormData();
      files.forEach((file) => formData.append("files", file));

      const response = await fetch("/api/images", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Не удалось загрузить изображения в Garage/S3.");
      }

      await loadGarageImages();
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
      const formData = new FormData();
      files.forEach((file) => formData.append("files", file));

      const response = await fetch("/api/html-files", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Не удалось загрузить HTML-файлы в Garage/S3.");
      }

      await loadGarageHtmlFiles();
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

  function toggleImageSelection(imageId) {
    setGarageImages((prev) =>
      prev.map((image) =>
        image.id === imageId ? { ...image, selected: !image.selected } : image
      )
    );
  }

  function toggleHtmlSelection(fileId) {
    setGarageHtmlFiles((prev) =>
      prev.map((file) =>
        file.id === fileId ? { ...file, selected: !file.selected } : file
      )
    );
  }

  const selectedImages = useMemo(
    () => garageImages.filter((image) => image.selected),
    [garageImages]
  );

  const selectedHtmlFiles = useMemo(
    () => garageHtmlFiles.filter((file) => file.selected),
    [garageHtmlFiles]
  );

  async function downloadFile(url, fallbackUrl, filename) {
    const response = await fetch(url || fallbackUrl);
    if (!response.ok) {
      throw new Error("Не удалось скачать файл.");
    }

    const blob = await response.blob();
    const blobUrl = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = blobUrl;
    link.download = filename || "file";
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(blobUrl);
  }

  async function handleDownloadImage(image) {
    try {
      await downloadFile(image.downloadUrl, `/api/images/${image.id}/download`, image.name || "image");
    } catch (e) {
      setError(e.message || "Ошибка при скачивании изображения.");
    }
  }

  async function handleDownloadHtmlFile(file) {
    try {
      await downloadFile(file.downloadUrl, `/api/html-files/${file.id}/download`, file.name || "page.html");
    } catch (e) {
      setError(e.message || "Ошибка при скачивании HTML-файла.");
    }
  }

  async function handleDeleteImage(imageId) {
    setImageDeletingId(imageId);
    setError("");

    try {
      const response = await fetch(`/api/images/${imageId}`, { method: "DELETE" });
      if (!response.ok) {
        throw new Error("Не удалось удалить изображение.");
      }
      await loadGarageImages();
    } catch (e) {
      setError(e.message || "Ошибка при удалении изображения.");
    } finally {
      setImageDeletingId(null);
    }
  }

  async function handleDeleteHtmlFile(fileId) {
    setHtmlDeletingId(fileId);
    setError("");

    try {
      const response = await fetch(`/api/html-files/${fileId}`, { method: "DELETE" });
      if (!response.ok) {
        throw new Error("Не удалось удалить HTML-файл.");
      }
      await loadGarageHtmlFiles();
    } catch (e) {
      setError(e.message || "Ошибка при удалении HTML-файла.");
    } finally {
      setHtmlDeletingId(null);
    }
  }

  async function handleStartParsing() {
    if (submittingRef.current) {
      return;
    }

    submittingRef.current = true;
    setIsSubmitting(true);
    setError("");

    const payload = {
      parsing: {
        url: siteUrl.trim(),
        downloadImages: downloadImagesFromSite,
        headers: buildMapFromRows(headers),
        cookies: buildMapFromRows(cookies),
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

    setLastPayload(payload);

    try {
      const response = await fetch("/api/pipeline", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error(`Бекенд вернул ошибку при запуске pipeline: ${response.status}`);
      }

      const responseText = await response.text();

      if (responseText.trim()) {
        try {
          const data = JSON.parse(responseText);
          setResult(data.result || data.answer || responseText);
        } catch {
          setResult(responseText);
        }
      } else {
        setResult("Запрос успешно отправлен на сервер.");
      }
    } catch (e) {
      setError(e.message || "Ошибка при запуске pipeline.");
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  }

  const canStartParsing = Boolean(siteUrl.trim());

  return (
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
              <div className="text-xs text-slate-500">HTML в обработке</div>
              <div className="mt-1 text-lg font-semibold">{selectedHtmlFiles.length}</div>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
              <div className="text-xs text-slate-500">Изображения OCR</div>
              <div className="mt-1 text-lg font-semibold">{selectedImages.length}</div>
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
              description="URL страницы, параметры парсинга, headers, cookies, proxy и HTML-файлы из S3. HTML-файлы пока хранятся отдельно и не входят в DTO pipeline на бекенде."
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

                  <label className="block lg:col-span-2">
                    <span className="mb-2 block text-sm font-medium text-slate-700">
                      Дополнительное ожидание загрузки (сек.)
                    </span>
                    <input
                      type="number"
                      min="0"
                      value={extraWaitSeconds}
                      onChange={(e) => setExtraWaitSeconds(e.target.value)}
                      placeholder="5"
                      className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-slate-400"
                    />
                  </label>
                </div>

                <KeyValueEditor
                  title="Headers"
                  rows={headers}
                  setRows={setHeaders}
                  placeholderKey="Authorization"
                  placeholderValue="Bearer ..."
                />

                <KeyValueEditor
                  title="Cookies"
                  rows={cookies}
                  setRows={setCookies}
                  placeholderKey="sessionid"
                  placeholderValue="abc123"
                />

                <ProxyEditor
                  useProxy={useProxy}
                  setUseProxy={setUseProxy}
                  proxyConfig={proxyConfig}
                  setProxyConfig={setProxyConfig}
                />

                <StorageSection
                  title="HTML-файлы в Garage/S3"
                  description="Загрузите собственные HTML-файлы страниц, отмечайте нужные для обработки, скачивайте и удаляйте их."
                  uploadLabel="Загрузить HTML-файлы"
                  uploading={htmlUploading}
                  loading={htmlLoading}
                  accept=".html,text/html"
                  onUpload={handleUploadHtmlFiles}
                  onRefresh={loadGarageHtmlFiles}
                  emptyText="В хранилище пока нет HTML-файлов."
                  items={garageHtmlFiles}
                  variant="file"
                  selectableLabel="Использовать в обработке"
                  onToggleSelect={toggleHtmlSelection}
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
              description="Загрузка, скачивание, удаление и выбор изображений через Garage/S3. Выбор изображений пока не передаётся в pipeline, потому что RecognitionRequest на бекенде пока пустой."
            >
              <StorageSection
                title="Изображения для OCR"
                description="Отметьте те изображения, которые нужно отправить в OCR, либо добавьте собственные файлы."
                uploadLabel="Загрузить изображения"
                uploading={garageUploading}
                loading={garageLoading}
                accept="image/*"
                onUpload={handleUploadImages}
                onRefresh={loadGarageImages}
                emptyText="В хранилище пока нет изображений."
                items={garageImages}
                variant="image"
                selectableLabel="Использовать для OCR"
                onToggleSelect={toggleImageSelection}
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
              description="Финальная отправка всех параметров на бекенд."
            >
              <div className="space-y-4">
                <button
                  type="button"
                  onClick={handleStartParsing}
                  disabled={isSubmitting || !canStartParsing}
                  className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Выполняется парсинг...
                    </>
                  ) : (
                    <>
                      <Play className="h-4 w-4" />
                      Начать процесс парсинга
                    </>
                  )}
                </button>

                {!canStartParsing && (
                  <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
                    Укажите URL страницы. Сейчас backend DTO pipeline принимает источник только через поле url.
                  </div>
                )}

                {error && (
                  <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                    {error}
                  </div>
                )}

                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="grid grid-cols-2 gap-3 text-sm text-slate-600">
                    <div>
                      <div className="text-xs text-slate-500">URL</div>
                      <div className="mt-1 break-all font-medium text-slate-900">{siteUrl || "—"}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-500">Выбранная модель</div>
                      <div className="mt-1 font-medium text-slate-900">{model}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-500">Сложность</div>
                      <div className="mt-1 font-medium text-slate-900">{parsingComplexity}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-500">Температура</div>
                      <div className="mt-1 font-medium text-slate-900">{temperature}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-500">HTML-файлы</div>
                      <div className="mt-1 font-medium text-slate-900">{selectedHtmlFiles.length}</div>
                    </div>
                    <div>
                      <div className="text-xs text-slate-500">Изображения для OCR</div>
                      <div className="mt-1 font-medium text-slate-900">{selectedImages.length}</div>
                    </div>
                  </div>
                </div>
              </div>
            </Section>

            <Section
              icon={FileText}
              title="Результат"
              description="Ответ от модели с возможностью ручного редактирования."
            >
              <textarea
                value={result}
                onChange={(e) => setResult(e.target.value)}
                rows={24}
                placeholder="Здесь появится ответ от нейросети..."
                className="min-h-[420px] w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-6 outline-none transition focus:border-slate-400"
              />
            </Section>

            <Section
              icon={Shield}
              title="Payload preview"
              description="Предпросмотр последнего JSON, который был отправлен на бекенд. Удобно для отладки."
            >
              <pre className="max-h-[500px] overflow-auto rounded-2xl border border-slate-200 bg-slate-950 p-4 text-xs leading-6 text-slate-100">
                {JSON.stringify(lastPayload, null, 2) || "{}"}
              </pre>
            </Section>
          </div>
        </div>
      </div>
    </div>
  );
}