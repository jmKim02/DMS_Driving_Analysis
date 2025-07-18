package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "driving_videos")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class DrivingVideo extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long videoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_path")
    private String filePath;

    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // 연관관계 메서드
    public void setUser(User user) {
        this.user = user;
        user.getVideos().add(this);
    }

    // 생성 메서드
    public static DrivingVideo createVideo(User user, String filePath, Integer duration) {
        DrivingVideo video = new DrivingVideo();
        video.setUser(user); // 연관관계 메서드 사용
        video.filePath = filePath;
        video.duration = duration;
        video.status = VideoStatus.UPLOADED;
        video.uploadedAt = LocalDateTime.now();
        return video;
    }

    public void setStatus(VideoStatus videoStatus) {
        this.status = videoStatus;
    }

    public void setProcessedAt() {
        this.processedAt = LocalDateTime.now();
    }
}
