package edu.my.service.inter;

import edu.my.service.model.Envelope;

public interface EnvelopeSender {
    void send(Envelope envelope) throws Exception;
}
