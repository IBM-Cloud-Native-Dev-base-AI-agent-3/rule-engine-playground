-- ==========================================
-- 1. 테이블 생성 DDL (MySQL 문법 검증 완료)
-- ==========================================

-- 1. 공고 마스터 테이블
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,          -- 공고명 (예: 2026년 SH 행복주택)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 공고별 자격 규칙 테이블
CREATE TABLE IF NOT EXISTS announcement_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    announcement_id BIGINT,                               -- 어떤 공고의 규칙인지 (외래키)
    rule_name VARCHAR(100) NOT NULL,                      -- 규칙 이름 (예: 나이 제한)
    field VARCHAR(100) NOT NULL,                          -- 검증할 항목 (예: user.age)
    operator VARCHAR(20) NOT NULL,                        -- 연산자 (예: BETWEEN, LTE, EQUAL)
    value VARCHAR(255) NOT NULL,                          -- 기준값 (예: [19,39], 서울)
    is_mandatory TINYINT(1) DEFAULT 1,                    -- 필수 여부 (1: 필수, 0: 가점/선택)
    description VARCHAR(255),
    FOREIGN KEY (announcement_id) REFERENCES announcement(id)
);


-- ==========================================
-- 2. 규칙 데이터 적재 DML (INSERT 문)
-- ==========================================

-- 공고 마스터 정보 적재 (ID: 2026101)
INSERT INTO announcement (id, title)
VALUES (2026101, '2026년도 신혼부부/청년 공공임대 통합공고')
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- 공고별 자격 규칙 데이터 일괄 적재
INSERT INTO announcement_rule (announcement_id, rule_name, field, operator, value, is_mandatory, description)
VALUES 
(2026101, '주택 소유 여부 검증', 'user.isHomeOwner', 'EQUAL', 'false', 1, '신청자 및 세대구성원 전원 무주택이어야 함'),
(2026101, '과거 주택 처분 이력 체크', 'user.hasPastHomeOwnership', 'EQUAL', 'false', 0, '감점 또는 순위 제한 요소로 활용'),
(2026101, '신청자 연령 검증 (청년 기준 예시)', 'user.age', 'BETWEEN', '[19, 39]', 1, '만 19세 이상 만 39세 이하'),
(2026101, '연속 거주기간 확인 (서울 거주 예시)', 'user.residence', 'EQUAL', '서울', 1, NULL),
(2026101, '신혼부부 혼인 기간 검증', 'user.marriageDurationYears', 'LTE', '7.0', 0, '혼인신고일 기준 7년 이내 (6세 이하 자녀 있을 시 예외 처리는 FACT단에서 계산)'),
(2026101, '월평균 소득 기준액 검증', 'user.incomePercent', 'LTE', '120', 1, '전년도 도시근로자 가구원수별 월평균 소득 기준 120% 이하'),
(2026101, '총자산 제한 기준 검증', 'user.totalAsset', 'LTE', '345000000', 1, '부동산+자동차+금융+기타 - 부채 합산액이 3억 4,500만 원 이하'),
(2026101, '자동차 가액 제한 검증', 'user.carValue', 'LTE', '37080000', 1, '보건복지부 장관이 정하는 차량기준가액 제한'),
(2026101, '청약통장 최소 납입 횟수', 'user.subscriptionCount', 'GTE', '6', 0, '청약통장 가입 후 6회 이상 납입 여부'),
(2026101, '우선공급 대상자 필터링', 'user.specialQualifications', 'CONTAINS_ANY', '[''사회취약계층'', ''주거약자'', ''주거급여수급자'', ''중소기업근로자'', ''장애인'']', 0, '우선공급 대상 항목 중 하나라도 해당되는지 체크'),
(2026101, '과거 공공임대 계약 이력 감점 체크', 'user.pastContractHistory', 'NOT_EQUAL', 'WITHIN_3_YEARS', 0, '최근 3년 이내 계약 이력이 있으면 감점 처리하기 위한 룰');
