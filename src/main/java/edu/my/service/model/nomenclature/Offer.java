package edu.my.service.model.nomenclature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Offer {
    @JsonProperty("Code")
    private String code;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Producer")
    private String producer;
    private float vat;

    @JsonProperty("SupplierCodes")
    private List<SupplierCode> supplierCodes;

    @Override
    public String toString() {
        return "{code: '" + code + "', name: '" + name + "', producer: '" + producer + "', supplierCodes: [\n" + supplierCodes + "\n]\n}\n";
    }

    public Offer(String code, String name, String producer, float vat, List<SupplierCode> supplierCodes) {
        this.code = code;
        this.name = name;
        this.producer = producer;
        this.vat = vat;
        this.supplierCodes = supplierCodes;
    }

    public float getVat() {
        return vat;
    }

    public void setVat(float vat) {
        this.vat = vat;
    }

    public Offer() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public List<SupplierCode> getSupplierCodes() {
        return supplierCodes;
    }

    public void setSupplierCodes(List<SupplierCode> supplierCodes) {
        this.supplierCodes = supplierCodes;
    }
}

