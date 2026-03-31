package com.english.learn.service;

import com.english.learn.dto.DocumentDTO;
import com.english.learn.dto.DocumentDownloadResult;
import com.english.learn.dto.DocumentImageResult;
import com.english.learn.entity.Document;
import com.english.learn.mapper.DocumentMapper;
import com.english.learn.repository.CardSourceRepository;
import com.english.learn.repository.DocumentRepository;
import com.english.learn.util.DocumentParseUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档业务服务：上传解析、原件落盘、列表、查看、删除。
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CardSourceRepository cardSourceRepository;
    private final DocumentFileStorage documentFileStorage;

    @Transactional(rollbackFor = Exception.class)
    public DocumentDTO upload(Long userId, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String fileType = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!DocumentParseUtil.isSupported(fileType)) {
            throw new IllegalArgumentException("仅支持 doc、docx、txt 格式");
        }
        String suffix = fileType.isEmpty() ? "bin" : fileType;
        Path temp;
        try {
            temp = Files.createTempFile("english-learn-doc-", "." + suffix);
        } catch (IOException e) {
            throw new RuntimeException("无法创建临时文件: " + e.getMessage(), e);
        }
        try {
            file.transferTo(temp.toFile());
            String content = DocumentParseUtil.parse(Files.newInputStream(temp), fileType);
            Document entity = new Document();
            entity.setUserId(userId);
            entity.setFileName(fileName);
            entity.setFileType(fileType);
            entity.setContent(content);
            entity = documentRepository.save(entity);
            if ("docx".equalsIgnoreCase(fileType)) {
                extractAndStoreDocxImages(temp, userId, entity.getId());
            }
            String relative = documentFileStorage.storeUploadedFile(userId, entity.getId(), fileName, temp);
            entity.setStoredFilePath(relative);
            entity = documentRepository.save(entity);
            return DocumentMapper.toDTO(entity);
        } catch (IOException e) {
            throw new RuntimeException("文档解析或原件保存失败: " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public List<DocumentDTO> listByUserId(Long userId) {
        return documentRepository.findSummaryByUserId(userId).stream()
                .map(DocumentMapper::fromSummary)
                .collect(Collectors.toList());
    }

    public Page<DocumentDTO> pageByUserId(Long userId, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size, 100));
        PageRequest pr = PageRequest.of(p - 1, s);
        return documentRepository.findSummaryByUserId(userId, pr).map(DocumentMapper::fromSummary);
    }

    public DocumentDTO getById(Long id, Long userId) {
        Document doc = documentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限查看该文档");
        }
        return DocumentMapper.toDTO(doc);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id, Long userId) {
        Document doc = documentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除该文档");
        }
        String stored = doc.getStoredFilePath();
        cardSourceRepository.deleteByUserIdAndDocumentId(userId, id);
        documentRepository.delete(doc);
        documentFileStorage.deleteIfExists(stored);
    }

    /**
     * 下载服务器上保存的上传原件（非历史仅解析入库的记录）。
     */
    public DocumentDownloadResult loadOriginalDownload(Long id, Long userId) {
        Document doc = documentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限下载该文档");
        }
        if (doc.getStoredFilePath() == null || doc.getStoredFilePath().isEmpty()) {
            throw new IllegalArgumentException("该文档无服务器保存的原件（历史数据或未落盘）");
        }
        Path path = documentFileStorage.toAbsolute(doc.getStoredFilePath());
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("原件文件不存在或已删除");
        }
        return new DocumentDownloadResult(doc.getFileName(), new FileSystemResource(path));
    }

    /**
     * 加载文档中提取出的第 N 张图片（1-based）。
     */
    public DocumentImageResult loadDocImage(Long id, Long userId, int index) {
        if (index <= 0) {
            throw new IllegalArgumentException("图片序号不合法");
        }
        Document doc = documentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("文档不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限查看该文档图片");
        }
        try {
            Path img = documentFileStorage.findDocImage(userId, id, index);
            if (img == null) {
                throw new IllegalArgumentException("文档图片不存在");
            }
            String ct = Files.probeContentType(img);
            if (ct == null || ct.trim().isEmpty()) {
                ct = "application/octet-stream";
            }
            return new DocumentImageResult(new FileSystemResource(img), ct);
        } catch (IOException e) {
            throw new RuntimeException("读取文档图片失败: " + e.getMessage(), e);
        }
    }

    private void extractAndStoreDocxImages(Path temp, Long userId, Long docId) {
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(temp))) {
            List<XWPFPictureData> pics = doc.getAllPictures();
            for (int i = 0; i < pics.size(); i++) {
                XWPFPictureData p = pics.get(i);
                String ext = p.suggestFileExtension();
                documentFileStorage.storeDocImage(userId, docId, i + 1, ext, p.getData());
            }
        } catch (IOException e) {
            throw new RuntimeException("提取 docx 图片失败: " + e.getMessage(), e);
        }
    }
}
