package com.english.learn.util;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文档解析工具类。
 * 支持 .doc（HWPF）、.docx（XWPF）、.txt 的文本提取，供上传后入库与前端展示、选词。
 * <p>
 * docx 使用 {@link XWPFWordExtractor}：除普通段落外，会提取表格、页眉页脚、内容控件（SDT）、脚注等，
 * 解决仅遍历 {@code getParagraphs()} 时「富文本/表格内文字」丢失的问题。
 * 说明：入库仍为纯文本，不保留 Word 版式与嵌入图片像素；若某段仅有图无字，提取结果可能仍为空行。
 */
public final class DocumentParseUtil {

    private static final String TYPE_DOC = "doc";
    private static final String TYPE_DOCX = "docx";
    private static final String TYPE_TXT = "txt";
    private static final String IMG_TOKEN_PREFIX = "[[IMG_";
    private static final String IMG_TOKEN_SUFFIX = "]]";

    private DocumentParseUtil() {
    }

    public static boolean isSupported(String fileType) {
        return TYPE_DOC.equalsIgnoreCase(fileType)
                || TYPE_DOCX.equalsIgnoreCase(fileType)
                || TYPE_TXT.equalsIgnoreCase(fileType);
    }

    /**
     * 根据文件类型解析为纯文本。
     *
     * @param file     上传文件
     * @param fileType 扩展名，如 doc、docx、txt
     * @return 解析后的文本内容
     */
    /**
     * 统一换行为 \n，便于前端按段落展示且 offset 计算一致。
     * 同时清理 Word 提取中的对象占位符与零宽字符，避免页面出现黑方块/不可见脏字符。
     */
    private static String normalizeLineEndings(String text) {
        if (text == null || text.isEmpty()) return text;
        String cleaned = text
                .replace("\uFFFC", "") // 兜底：未被 token 化时，直接移除对象占位符
                .replace("\u25A0", "") // BLACK SQUARE（部分文档中的图片/符号占位）
                .replace("\u25AA", "") // BLACK SMALL SQUARE
                .replace("\u25AB", "") // WHITE SMALL SQUARE
                .replaceAll("[\\uE000-\\uF8FF]", "") // 私有区字符（常见于字体图标占位）
                .replace("\u200B", "") // ZERO WIDTH SPACE
                .replace("\u200C", "") // ZERO WIDTH NON-JOINER
                .replace("\u200D", "") // ZERO WIDTH JOINER
                .replace("\uFEFF", "") // ZERO WIDTH NO-BREAK SPACE (BOM)
                .replace("\r\n", "\n")
                .replace("\r", "\n");
        // 一些 Word 模板会在每行前带块状符号（几何/块元素区），作为图片或项目符号占位。
        cleaned = cleaned.replaceAll("(?m)^[\\t ]*[\\u2580-\\u259F\\u25A0-\\u25FF]+[\\t ]*", "");

        // 处理「去空行」：删除仅包含空白字符的行，避免页面出现大段空白影响阅读
        cleaned = cleaned
                .replaceAll("\\[\\[IMG_\\d+]]", "") // 如果还残留 token，移除占位
                .replace("[图片]", "")
                .trim();
        String[] lines = cleaned.split("\\n", -1);
        StringBuilder out = new StringBuilder(cleaned.length());
        boolean first = true;
        for (String line : lines) {
            if (line == null) continue;
            if (line.trim().isEmpty()) continue;
            if (!first) out.append('\n');
            out.append(line);
            first = false;
        }
        return out.toString();
    }

    /**
     * 将 docx 中的对象占位符替换为可解析 token，供前端按位置渲染图片。
     */
    private static String replaceObjectPlaceholderWithImgToken(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 32);
        int imgIndex = 1;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\uFFFC' || isBlockImageMarker(text, i, ch)) {
                sb.append(IMG_TOKEN_PREFIX).append(imgIndex++).append(IMG_TOKEN_SUFFIX);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * 某些文档提取时，图片位置会退化为黑块字符（不是 \uFFFC）。
     * 仅在「行首附近 + 后续紧跟英文/数字」时当作图片占位，避免误伤普通项目符号。
     */
    private static boolean isBlockImageMarker(String text, int i, char ch) {
        boolean isBlock = (ch >= '\u2580' && ch <= '\u259F') || (ch >= '\u25A0' && ch <= '\u25FF');
        if (!isBlock) return false;
        char prev = i > 0 ? text.charAt(i - 1) : '\n';
        boolean nearLineStart = prev == '\n' || prev == '\r' || prev == '\t' || prev == ' ';
        if (!nearLineStart) return false;
        char next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';
        return Character.isLetterOrDigit(next);
    }

    public static String parse(MultipartFile file, String fileType) {
        try {
            return parse(file.getInputStream(), fileType);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从已落盘的流解析（例如临时文件），与 {@link #parse(MultipartFile, String)} 逻辑一致。
     */
    public static String parse(InputStream inputStream, String fileType) {
        try {
            String type = fileType == null ? "" : fileType.toLowerCase();
            String raw;
            switch (type) {
                case TYPE_DOCX:
                    raw = parseDocx(inputStream);
                    break;
                case TYPE_DOC:
                    raw = parseDoc(inputStream);
                    break;
                case TYPE_TXT:
                    raw = parseTxt(inputStream);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的格式: " + fileType);
            }
            return normalizeLineEndings(raw);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    private static String parseDocx(InputStream is) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            try (XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                extractor.setCloseFilesystem(false);
                String text = extractor.getText();
                return replaceObjectPlaceholderWithImgToken(text);
            }
        }
    }

    private static String parseDoc(InputStream is) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(is)) {
            try (WordExtractor extractor = new WordExtractor(doc)) {
                String text = extractor.getText();
                return text != null ? text : "";
            }
        }
    }

    private static String parseTxt(InputStream is) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = is.read(b)) != -1) {
            buf.write(b, 0, n);
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }
}
