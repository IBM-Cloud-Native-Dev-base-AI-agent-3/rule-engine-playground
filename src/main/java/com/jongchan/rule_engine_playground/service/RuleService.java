package com.jongchan.rule_engine_playground.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jongchan.rule_engine_playground.entity.Announcement;
import com.jongchan.rule_engine_playground.entity.AnnouncementRule;
import com.jongchan.rule_engine_playground.repository.AnnouncementRepository;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.RuleListener;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.mvel.MVELRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RuleService {

    private final AnnouncementRepository announcementRepository;

    @Autowired
    public RuleService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    /**
     * DB에서 공고 ID로 규칙들을 조회하고, 입력받은 Facts(데이터)에 대해 청약 자격 룰엔진을 실행합니다.
     */
    public Map<String, Object> evaluate(Long announcementId, Map<String, Object> inputFacts) {
        try {
            // 1. DB에서 공고 마스터 및 규칙 조회
            Announcement announcement = announcementRepository.findById(announcementId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고 ID입니다: " + announcementId));

            List<AnnouncementRule> dbRules = announcement.getRules();

            // 결과 보관용 컬렉션
            List<String> passedRules = new ArrayList<>();
            List<String> failedMandatoryRules = new ArrayList<>();
            List<String> failedOptionalRules = new ArrayList<>();
            final boolean[] isEligible = {true}; // 람다식 내부 사용 목적

            Rules rules = new Rules();
            Map<String, AnnouncementRule> ruleMetadataMap = new HashMap<>();

            for (AnnouncementRule dbRule : dbRules) {
                String ruleName = dbRule.getRuleName();
                String field = dbRule.getField();
                String operator = dbRule.getOperator();
                String value = dbRule.getValue();
                boolean isMandatory = dbRule.isMandatory();
                String description = dbRule.getDescription();

                // MVEL 조건문 빌드
                String mvelCondition = buildMvelCondition(field, operator, value);

                // MVELRule 객체 동적 생성
                MVELRule mvelRule = new MVELRule()
                        .name(ruleName)
                        .description(description != null ? description : "")
                        .when(mvelCondition)
                        // 규칙 통과 결과를 RuleListener가 취합하므로 안전한 콘솔 출력으로 액션 대체
                        .then("System.out.println(\"Rule passed: \" + \"" + ruleName + "\");");

                rules.register(mvelRule);

                // 메타데이터 보관
                ruleMetadataMap.put(ruleName, dbRule);
            }

            // Facts 설정
            Facts facts = new Facts();
            for (Map.Entry<String, Object> entry : inputFacts.entrySet()) {
                facts.put(entry.getKey(), entry.getValue());
            }

            // Easy Rules 룰엔진 생성 및 리스너 등록
            DefaultRulesEngine rulesEngine = new DefaultRulesEngine();
            rulesEngine.registerRuleListener(new RuleListener() {
                @Override
                public boolean beforeEvaluate(Rule rule, Facts facts) {
                    return true;
                }

                @Override
                public void afterEvaluate(Rule rule, Facts facts, boolean evaluationResult) {
                    String ruleName = rule.getName();
                    AnnouncementRule meta = ruleMetadataMap.get(ruleName);
                    boolean isMandatory = meta != null && meta.isMandatory();

                    if (evaluationResult) {
                        passedRules.add(ruleName);
                    } else {
                        if (isMandatory) {
                            isEligible[0] = false; // 필수 규칙 중 하나라도 미달되면 부적격
                            failedMandatoryRules.add(ruleName);
                        } else {
                            failedOptionalRules.add(ruleName);
                        }
                    }
                }

                @Override
                public void onEvaluationError(Rule rule, Facts facts, Exception exception) {
                    String ruleName = rule.getName();
                    failedMandatoryRules.add(ruleName + " (오류: " + exception.getMessage() + ")");
                    isEligible[0] = false;
                }

                @Override
                public void beforeExecute(Rule rule, Facts facts) {}

                @Override
                public void onSuccess(Rule rule, Facts facts) {}

                @Override
                public void onFailure(Rule rule, Facts facts, Exception exception) {}
            });

            // 엔진 가동
            rulesEngine.fire(rules, facts);

            // 결과 취합 반환
            Map<String, Object> result = new HashMap<>();
            result.put("announcementId", announcementId);
            result.put("announcementName", announcement.getTitle());
            result.put("isEligible", isEligible[0]);
            result.put("passedRules", passedRules);
            result.put("failedMandatoryRules", failedMandatoryRules);
            result.put("failedOptionalRules", failedOptionalRules);
            result.put("inputFacts", inputFacts);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("DB 기반 룰 평가 수행 중 에러 발생: " + e.getMessage(), e);
        }
    }

    /**
     * DB에서 공고 정보를 읽어와서 기존 JSON 포맷으로 조립해 반환합니다 (프론트엔드 연동용).
     */
    public String getRulesJsonFromDb() {
        try {
            Announcement announcement = announcementRepository.findById(2026101L)
                    .orElseGet(() -> {
                        Announcement a = new Announcement();
                        a.setId(2026101L);
                        a.setTitle("2026년도 신혼부부/청년 공공임대 통합공고");
                        return announcementRepository.save(a);
                    });

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("announcementId", announcement.getId());
            response.put("announcementName", announcement.getTitle());

            List<Map<String, Object>> rulesList = new ArrayList<>();
            for (AnnouncementRule rule : announcement.getRules()) {
                Map<String, Object> ruleMap = new LinkedHashMap<>();
                ruleMap.put("ruleName", rule.getRuleName());
                ruleMap.put("field", rule.getField());
                ruleMap.put("operator", rule.getOperator());
                ruleMap.put("value", rule.getValue());
                ruleMap.put("isMandatory", rule.isMandatory());
                ruleMap.put("description", rule.getDescription());
                rulesList.add(ruleMap);
            }
            response.put("rules", rulesList);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("DB 규칙 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 문자열을 파싱하여 DB의 규칙 레코드를 업데이트합니다 (프론트엔드 UI 저장용).
     */
    @Transactional
    public void saveRulesJsonToDb(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS, true);
            Map<String, Object> jsonMap = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            Long announcementId = Long.parseLong(jsonMap.get("announcementId").toString());
            String announcementName = (String) jsonMap.get("announcementName");

            // 1. 공고 마스터 저장/조회
            Announcement announcement = announcementRepository.findById(announcementId)
                    .orElseGet(() -> {
                        Announcement a = new Announcement();
                        a.setId(announcementId);
                        return a;
                    });
            announcement.setTitle(announcementName);
            announcement = announcementRepository.save(announcement);

            // 2. 기존 규칙 비우기 (OrphanRemoval 작동)
            announcement.getRules().clear();
            announcementRepository.saveAndFlush(announcement);

            // 3. 새 규칙 생성 및 저장
            List<Map<String, Object>> rulesList = (List<Map<String, Object>>) jsonMap.get("rules");
            if (rulesList != null) {
                for (Map<String, Object> ruleData : rulesList) {
                    AnnouncementRule rule = new AnnouncementRule();
                    rule.setAnnouncement(announcement);
                    rule.setRuleName((String) ruleData.get("ruleName"));
                    rule.setField((String) ruleData.get("field"));
                    rule.setOperator((String) ruleData.get("operator"));
                    rule.setValue(String.valueOf(ruleData.get("value")));
                    rule.setMandatory(ruleData.containsKey("isMandatory") && (boolean) ruleData.get("isMandatory"));
                    rule.setDescription((String) ruleData.get("description"));

                    announcement.getRules().add(rule);
                }
            }
            announcementRepository.save(announcement);
        } catch (Exception e) {
            throw new RuntimeException("DB 규칙 업데이트 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 필드명, 연산자, 임계치를 바탕으로 안전한 MVEL 조건문 작성
     */
    private String buildMvelCondition(String field, String operator, String value) {
        String[] parts = field.split("\\.");
        StringBuilder guard = new StringBuilder();
        StringBuilder currentPath = new StringBuilder();

        guard.append(parts[0]).append(" != null");
        currentPath.append(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            guard.append(" && ").append(currentPath.toString()).append(".").append(parts[i]).append(" != null");
            currentPath.append(".").append(parts[i]);
        }

        String fieldAccess = currentPath.toString();
        String opExpr = "";

        switch (operator.toUpperCase()) {
            case "EQUAL":
                opExpr = fieldAccess + " == " + formatValue(value);
                break;
            case "NOT_EQUAL":
                opExpr = fieldAccess + " != " + formatValue(value);
                break;
            case "LTE":
                opExpr = fieldAccess + " <= " + formatValue(value);
                break;
            case "GTE":
                opExpr = fieldAccess + " >= " + formatValue(value);
                break;
            case "BETWEEN":
                String clean = value.replace("[", "").replace("]", "").trim();
                String[] valParts = clean.split(",");
                String min = valParts[0].trim();
                String max = valParts[1].trim();
                opExpr = fieldAccess + " >= " + min + " && " + fieldAccess + " <= " + max;
                break;
            case "CONTAINS_ANY":
                opExpr = "(" + fieldAccess + " instanceof java.util.Collection ? !java.util.Collections.disjoint(" + formatValue(value) + ", " + fieldAccess + ") : " + formatValue(value) + ".contains(" + fieldAccess + "))";
                break;
            default:
                opExpr = "true";
        }

        return guard.toString() + " && (" + opExpr + ")";
    }

    /**
     * 리터럴 문자열 형태를 MVEL 표현식용 타입 표현으로 변경
     */
    private String formatValue(String val) {
        if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
            return val.toLowerCase();
        }
        try {
            Double.parseDouble(val);
            return val;
        } catch (NumberFormatException e) {
            if (val.startsWith("[") && val.endsWith("]")) {
                return val;
            }
            return "\"" + val.replace("\"", "\\\"") + "\"";
        }
    }
}
