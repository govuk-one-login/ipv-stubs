package uk.gov.di.ipv.stub.core.config.uatuser;

import java.util.Collection;

public record Questions(
        Collection<Question> questions,
        int numQuestionsAfterBusinessRules,
        int numQuestionsTotal) {}
