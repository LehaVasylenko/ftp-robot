package edu.my.service.model;

import edu.my.service.model.nomenclature.NomenclatureIncoming;
import edu.my.service.model.rests.OffersWrapper;

public record Envelope(
        EnvelopeType type,
        String branchId,
        NomenclatureIncoming nom,
        OffersWrapper ow
) {
}
