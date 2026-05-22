package com.jongchan.rule_engine_playground.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalHomeAlertRequestDto {

    // 알림 대상 수신용 식별 정보
    private String email;

    // 룰 검증용 신청자 자격 정보 (Flat 구조로 프론트엔드 통신 편의성 극대화)
    private Boolean isHomeOwner;
    private Boolean hasPastHomeOwnership;
    private Integer age;
    private String residence;
    private Double marriageDurationYears;
    private Integer incomePercent;
    private Long totalAsset;
    private Long carValue;
    private Integer subscriptionCount;
    private List<String> specialQualifications;
    private String pastContractHistory;

    /**
     * DTO의 평탄한(Flat) 신청자 데이터 구조를
     * 규칙 엔진이 요구하는 중첩 구조(user.xxx)의 Facts Map 포맷으로 변환합니다.
     */
    public Map<String, Object> toFactsMap() {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("isHomeOwner", this.isHomeOwner);
        userMap.put("hasPastHomeOwnership", this.hasPastHomeOwnership);
        userMap.put("age", this.age);
        userMap.put("residence", this.residence);
        userMap.put("marriageDurationYears", this.marriageDurationYears);
        userMap.put("incomePercent", this.incomePercent);
        userMap.put("totalAsset", this.totalAsset);
        userMap.put("carValue", this.carValue);
        userMap.put("subscriptionCount", this.subscriptionCount);
        userMap.put("specialQualifications", this.specialQualifications);
        userMap.put("pastContractHistory", this.pastContractHistory);

        Map<String, Object> factsMap = new HashMap<>();
        factsMap.put("user", userMap);
        return factsMap;
    }
}
