# 운전 습관 분석 기반 실시간 경고 시스템

---
<br/>

- 2025-1학기 캡스톤 디자인: AI 데이터셋을 활용한 팀프로젝트
- 수행 기간: 2025.3 ~ 2025.6 (4개월)
- 팀 구성: 백엔드 2명, 프론트엔드 1명, AI 1명
- 담당 역할: 백엔드 총괄

---
<br/>

## 프로젝트 개요 
운전자의 얼굴 및 상태 데이터를 기반으로 졸음운전 여부를 실시간으로 분석하고, 위험 감지 시 즉시 알림을 제공하는 시스템을 개발했습니다. <br/>
프론트엔드에서 전송된 영상 프레임을 WebSocket을 통해 백엔드 서버가 수신하고, AI 분석 서버와 gRPC 통신을 통해 졸음 여부를 판단한 뒤, <br/>
졸음이 감지된 경우 SSE를 통해 사용자에게 실시간 알림을 전달합니다.<br/>

주행이 끝난 후 졸음, 흡연, 휴대폰 사용과 같은 위험 행동에 대한 피드백을 제공합니다. <br/>

### 전체 프로젝트 스택
- 프론트엔드
  - Flutter, Dart
- 백엔드
  - Spring, Java
- AI
  - YOLOv8, Python

### 시스템 구조

<img width="600" height="400" alt="시스템구조도" src="https://github.com/user-attachments/assets/690eb108-fc76-4f10-af8c-478c3df8f994" />


---
<br/>

## 💻 백엔드 개발 담당
주요 기술 스택
- 언어: Java
- 프레임워크: Spring (SpringBoot)
- 빌드 도구: Gradle
- IDE: IntelliJ IDEA
- 데이터베이스: MySQL 8.0 (AWS RDS)


사용 라이브러리 및 기술
- Spring Web: RESTful API 설계 및 구현
- Spring Security: 사용자 인증 및 권한 관리
- Spring Validation: 요청 데이터 유효성 검사
- Spring Data JPA: ORM 기반의 데이터 액세스 계층 구성


통신 및 실시간 처리
- WebSocket: 클라이언트로부터 영상 프레임을 전송받음
- gRPC: AI 분석 서버와의 고속 양방향 통신에 활용
- SSE: 실시간 경고 알림 제공


배포 및 인프라
- AWS EC2 (Ubuntu): 백엔드 서버 운영 및 배포
- AWS RDS (MySQL): 데이터베이스 운영 및 관리
- GitHub & Git: 버전 관리 및 협업
