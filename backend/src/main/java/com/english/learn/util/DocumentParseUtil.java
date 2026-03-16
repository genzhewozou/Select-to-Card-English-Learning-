package com.english.learn.util;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档解析工具类。
 * 支持 .doc（HWPF）、.docx（XWPF）、.txt 的文本提取，供上传后入库与前端展示、选词。
 */
public final class DocumentParseUtil {

    private static final String TYPE_DOC = "doc";
    private static final String TYPE_DOCX = "docx";
    private static final String TYPE_TXT = "txt";

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
    /** 统一换行为 \n，便于前端按段落展示且 offset 计算一致 */
    private static String normalizeLineEndings(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    public static String parse(MultipartFile file, String fileType) {
        try {
            String type = fileType == null ? "" : fileType.toLowerCase();
            String raw;
            switch (type) {
                case TYPE_DOCX:
                    raw = parseDocx(file.getInputStream());
                    break;
                case TYPE_DOC:
                    raw = parseDoc(file.getInputStream());
                    break;
                case TYPE_TXT:
                    raw = parseTxt(file.getInputStream());
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
            List<String> paragraphs = doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.toList());
            return String.join("\n\n", paragraphs);
        }
    }

    private static String parseDoc(InputStream is) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(is)) {
            return doc.getDocumentText();
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
