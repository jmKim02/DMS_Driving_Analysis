package com.sejong.drivinganalysis.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;

    @Column(nullable = false)
    private String role = "USER"; // ADMIN 데모에서는 필요 없다고 판단

    // 양방향 관계로 설정
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<DrivingVideo> videos = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserChallenge> challenges = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Feedback> feedbacks = new ArrayList<>();

    // 생성 메서드
    public static User createUser(String username, String password, String email) {
        User user = new User();
        user.username = username;
        user.password = password;
        user.email = email;
        return user;
    }
}
