package com.hyperdata.proms.common.observability.masking;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

public class MaskingPatternConverter extends CompositeConverter<ILoggingEvent> {

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null) {
            return null;
        }
        return SensitiveDataMasker.mask(in);
    }
}
