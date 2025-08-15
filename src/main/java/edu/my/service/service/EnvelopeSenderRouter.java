package edu.my.service.service;

import edu.my.service.inter.EnvelopeSender;
import edu.my.service.model.Envelope;
import edu.my.service.model.EnvelopeType;

import java.util.EnumMap;
import java.util.Map;

public final class EnvelopeSenderRouter implements EnvelopeSender {
    private final Map<EnvelopeType, EnvelopeSender> routes = new EnumMap<>(EnvelopeType.class);

    public EnvelopeSenderRouter(Map<EnvelopeType, EnvelopeSender> map) {
        routes.putAll(map);
    }

    @Override
    public void send(Envelope e) throws Exception {
        EnvelopeSender s = routes.get(e.type());
        if (s == null) throw new IllegalStateException("No sender for type " + e.type());
        s.send(e);
    }
}
