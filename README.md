# 🚘운전 습관 분석 기반 실시간 경고 시스템
> 운전자의 얼굴 상태 데이터를 AI로 분석하여 졸음, 흡연, 휴대폰 사용 등 위험 행동을 감지하고, <br/>
> 실시간 알림 및 피드백을 제공하는 안전 운전 유도 시스템입니다.<br/>
> 주행이 끝난 후 졸음, 흡연, 휴대폰 사용과 같은 위험 행동에 대한 피드백을 제공합니다. <br/>

---
<br/>

- 2025-1학기 캡스톤 디자인: AI 데이터셋을 활용한 팀프로젝트
- 수행 기간: 2025.3 ~ 2025.6 (4개월)
- 팀 구성: 백엔드 2명, 프론트엔드 1명, AI 1명
- 담당 역할: 백엔드 총괄

---
<br/>

## 프로젝트 개요 
본 시스템은 단순한 경고 중심 DMS(Driver Monitoring System)를 넘어, <br/>
운전자의 행동을 **실시간 분석 → 즉시 알림 제공 → 습관 패턴 기록 → 피드백 제공**까지 수행합니다.<br/>  
Flutter 기반의 모바일 앱, Spring Boot 백엔드 서버, YOLO 기반 AI 서버 간 통합 구조를 갖추고 있으며, <br/>
졸음 운전 예방과 운전 습관 개선을 목표로 한 **B2B 확장 가능한 운전자 상태 분석 서비스**입니다. <br/>

### 전체 프로젝트 구성
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

## 🛠️ 백엔드 개발 담당
백엔드 기술 스택 상세
- 언어: Java 17
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
- SSE(Server-Sent Events): 실시간 경고 알림 제공


배포 및 인프라
- AWS EC2 (Ubuntu): 백엔드 서버 운영 및 배포
- AWS RDS (MySQL): 데이터베이스 운영 및 관리
- GitHub & Git: 버전 관리 및 협업

---
<br/>

## ✨ 주요 기능

| 기능 | 설명 | 담당 여부 |
|------|------|----------|
| 📷 실시간 프레임 수신 및 분석 요청 | 클라이언트에서 1초 30fps 프레임 수신 → gRPC 통해 AI 서버로 분석 요청 | ✅ 직접 구현 |
| ⚠️ 실시간 졸음/위험 행동 감지 알림 | SSE로 졸음/흡연/폰 사용 감지 시 사용자에게 알림 전송 | ✅ 직접 구현 |
| 🧠 AI 분석 결과 처리 및 DB 저장 | 졸음/흡연/휴대폰 사용 횟수, 시간대, 빈도 저장 | ✅ 직접 구현 |
| 📊 운전 점수 산출 및 API | AI 분석 결과 기반으로 점수화 알고리즘 구현 | ✅ 직접 구현 |
| 📄 피드백 생성 및 통계 API | 주간/월간 단위 피드백 생성, 시간대별 위험 행동 분석 | ✅ 직접 구현 |
| 🏆 챌린지 생성 및 관리 | 사용자 점수 기반 자동 챌린지 생성 및 참여 기능 | ❌ 미담당 (동료 담당) |
| 🥇 랭킹 시스템 | 점수 기반 사용자 간 순위 제공, 순위 API/조회 UI | ❌ 미담당 (동료 담당) 

---
<br/>

## 💻 담당 기능 상세

### ✅ 실시간 영상 프레임 수신 및 gRPC 분석 요청
- WebSocket을 통해 배치 단위로 서버에 전달
- gRPC 비동기 요청 처리 및 재시도 로직 포함

### ✅ 실시간 알림 전송 시스템 (SSE)
- 사용자별 SSE 연결을 `Map<Long, SseEmitter>`로 관리
- 위험 행동 발생 시 `sendRiskBehaviorAlert()`로 알림 전송
- 유휴 연결 및 Keep-Alive 핸들링 구현

### ✅ 분석 결과 처리 및 DB 저장
- AI 서버로부터 결과를 반환 받아 DB에 데이터 저장 및 관리
- `AnalysisResult`, `DrivingSession` 등 JPA 엔티티 설계 및 저장

### ✅ 운전 점수 및 피드백 산출
- 졸음/흡연/폰 사용 각 행동별 가중치를 반영한 점수화
- 시간대/요일별 통계 처리 및 피드백 문구 자동 생성

---
<br/>

## 🧪 시스템 테스트 및 성능
- 평균 실시간 알림 지연 시간: **1초 이내**
- WebSocket → gRPC → SSE 파이프라인 병목 제거
- 프레임 전송/처리 실패 시 재시도 및 오류 로그 수집

<br/>

## 📂 프로젝트 구조 (백엔드)
- gRPC를 사용할 때 별도로 백엔드서버와 AI서버가 동일한 protobuf 메시지 구조를 작성해야한다.

<img width="269" height="451" alt="image" src="https://github.com/user-attachments/assets/a262d273-42a6-4d27-b70d-8455f7a53abe" />

---
<br/>

## 📎 AI 데이터 참고
- YOLOv8 학습 데이터 출처:  
  [AI Hub - 졸음운전 예방을 위한 운전자 상태 정보 영상 데이터셋](https://www.aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&searchKeyword=%EC%A1%B8%EC%9D%8C%EC%9A%B4%EC%A0%84%20%EC%98%88%EB%B0%A9%EC%9D%84%20%EC%9C%84%ED%95%9C%20%EC%9A%B4%EC%A0%84%EC%9E%90%20%EC%83%81%ED%83%9C%20%EC%A0%95%EB%B3%B4%20%EC%98%81%EC%83%81&aihubDataSe=data&dataSetSn=173)
- 졸음 감지 모델 성능 지표: mAP, Recall, Precision 기반

---
