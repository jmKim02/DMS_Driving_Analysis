package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "driving_videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrivingVideo extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long videoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    private Integer duration;

    @Column(name = "recording_date")
    private LocalDateTime recordingDate;

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

    // 생성 메서드 (수정: DrivingSession 파라미터 추가)
    public static DrivingVideo createVideo(User user, String filePath,
                                           Integer duration) {
        DrivingVideo video = new DrivingVideo();
        video.setUser(user);
        video.filePath = filePath;
        video.duration = duration;
        video.recordingDate = LocalDateTime.now();
        video.status = VideoStatus.UPLOADED;
        video.uploadedAt = LocalDateTime.now();

        return video;
    }
    // DrivingVideo 클래스에 추가
    public void updateStatus(VideoStatus status) {
        this.status = status;
        if (status == VideoStatus.ANALYZED) {
            this.processedAt = LocalDateTime.now();
        }
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}