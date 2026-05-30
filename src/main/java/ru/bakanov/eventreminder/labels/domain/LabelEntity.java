package ru.bakanov.eventreminder.labels.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.bakanov.eventreminder.shared.persistence.AssertUtil;
import ru.bakanov.eventreminder.shared.persistence.IdGenerator;

@Entity
@Table(name = "labels")
@EntityListeners(AuditingEntityListener.class)
class LabelEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LabelEntity() {}

    LabelEntity(String userId, String name, String color) {
        this.id = IdGenerator.generateString();
        this.userId = AssertUtil.requireNotBlank(userId, "userId is required");
        this.name = AssertUtil.requireNotBlank(name, "name is required");
        this.color = color != null ? color : "#6c757d";
    }

    String getId() {
        return id;
    }

    String getUserId() {
        return userId;
    }

    String getName() {
        return name;
    }

    String getColor() {
        return color;
    }

    void update(String name, String color) {
        this.name = AssertUtil.requireNotBlank(name, "name is required");
        this.color = color != null ? color : "#6c757d";
    }
}
