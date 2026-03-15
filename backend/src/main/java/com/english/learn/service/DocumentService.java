package com.english.learn.service;

import com.english.learn.dto.DocumentDTO;
import com.english.learn.entity.Document;
import com.english.learn.mapper.DocumentMapper;
import com.english.learn.repository.DocumentRepository;
import com.english.learn.util.DocumentParseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档业务服务：上传解析、列表、查看、删除。
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

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
        String content = DocumentParseUtil.parse(file, fileType);
        Document entity = new Document();
        entity.setUserId(userId);
        entity.setFileName(fileName);
        entity.setFileType(fileType);
        entity.setContent(content);
        entity = documentRepository.save(entity);
        return DocumentMapper.toDTO(entity);
    }

    public List<DocumentDTO> listByUserId(Long userId) {
        return documentRepository.findByUserIdOrderByGmtCreateDesc(userId)
                .stream().map(DocumentMapper::toDTO).collect(Collectors.toList());
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
        documentRepository.delete(doc);
    }
}
