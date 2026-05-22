package com.jongchan.rule_engine_playground.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalHomeResponseDto {

    private Long announcementId;                 // 공고 ID
    private String announcementName;             // 공고명
    private Boolean isEligible;                  // 최종 자격 적격성 판정 결과
    private List<String> passedRules;            // 통과한 구체적 규칙명 리스트
    private List<String> failedMandatoryRules;   // 탈락한 필수 요건 규칙명 리스트
    private List<String> failedOptionalRules;    // 탈락한 선택/가점 요건 규칙명 리스트
    private Map<String, Object> inputFacts;      // 입력받았던 사용자의 청약 데이터 리프린트

    // 추후 AI 연동 대비를 위한 성공 확률 예측 프로퍼티 배치
    private Double aiPredictionRate;             // AI가 예측한 신청 당첨 확률 (예: 84.5)
}
