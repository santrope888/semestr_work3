package ru.itis.semestr_work3.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@RequiredArgsConstructor
public class GridFsDocumentService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private final GridFsTemplate gridFsTemplate;

    public String store(MultipartFile file) {
        validate(file);
        try {
            ObjectId id = gridFsTemplate.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
            return id.toHexString();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить документ в GridFS", e);
        }
    }

    public GridFsResource load(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("Идентификатор файла пустой");
        }

        GridFSFile file = gridFsTemplate.findOne(
                new Query(where("_id").is(new ObjectId(fileId)))
        );

        if (file == null) {
            throw new IllegalArgumentException("Документ не найден: " + fileId);
        }

        return gridFsTemplate.getResource(file);
    }

    public void delete(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }
        gridFsTemplate.delete(new Query(where("_id").is(new ObjectId(fileId))));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Файл слишком большой. Максимум 5 МБ");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Недопустимый формат файла. Разрешены: " + ALLOWED_TYPES);
        }
    }
}