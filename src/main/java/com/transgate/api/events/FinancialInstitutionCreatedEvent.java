package com.transgate.api.events;

import com.transgate.api.models.FinancialInstitutionModel;

/**
 * Published when a live financial institution is created (admin create or approved create).
 */
public class FinancialInstitutionCreatedEvent {

    private final FinancialInstitutionModel institution;
    private final Long outboxId;

    public FinancialInstitutionCreatedEvent(FinancialInstitutionModel institution) {
        this(institution, null);
    }

    public FinancialInstitutionCreatedEvent(FinancialInstitutionModel institution, Long outboxId) {
        this.institution = institution;
        this.outboxId = outboxId;
    }

    public FinancialInstitutionModel getInstitution() {
        return institution;
    }

    public Long getOutboxId() {
        return outboxId;
    }
}
