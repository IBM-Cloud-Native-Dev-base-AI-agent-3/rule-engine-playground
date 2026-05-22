package com.jongchan.rule_engine_playground.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "announcement_rule")
@Getter
@Setter
public class AnnouncementRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id")
    @JsonIgnore
    private Announcement announcement;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(nullable = false, length = 100)
    private String field;

    @Column(nullable = false, length = 20)
    private String operator;

    @Column(nullable = false)
    private String value;

    @Column(name = "is_mandatory")
    private boolean isMandatory;

    private String description;
}
