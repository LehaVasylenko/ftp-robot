package edu.my.service.model.rests;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "Offers")
public class OffersWrapper {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Offer")
    private List<OfferRests> offers;

    public List<OfferRests> getOffers() {
        return offers;
    }

    @Override
    public String toString() {
        return "{offers:\n[\n" + offers + "\n]\n}";
    }

    public void setOffers(List<OfferRests> offers) {
        this.offers = offers;
    }

    public OffersWrapper() {
    }

    public OffersWrapper(List<OfferRests> offers) {
        this.offers = offers;
    }
}

