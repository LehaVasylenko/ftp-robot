package edu.my.service.model.nomenclature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.*;
import edu.my.service.serializer.NomenclatureIncomingDeserializer;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Data")
@JsonDeserialize(using = NomenclatureIncomingDeserializer.class)
public class NomenclatureIncoming {
    @JsonProperty("Offers")
    private List<Offer> offers;
    private List<Supplier> suppliers;

    @Override
    public String toString() {
        return "NomenclatureIncoming: { \n\toffers: [\n" + offers + "\n]\n, suppliers: [\n" + suppliers + "\n]\n}";
    }

    public NomenclatureIncoming(List<Offer> offers, List<Supplier> suppliers) {
        this.offers = offers;
        this.suppliers = suppliers;
    }

    public List<Supplier> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<Supplier> suppliers) {
        this.suppliers = suppliers;
    }

    public NomenclatureIncoming() {
    }

    public List<Offer> getOffers() {
        return offers;
    }

    public void setOffers(List<Offer> offers) {
        this.offers = offers;
    }
}

