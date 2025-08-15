package edu.my.service.model.nomenclature;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SupplierCode {
    @JsonProperty("ID")
    private String id;

    @JsonProperty("Code")
    private String code;

    @Override
    public String toString() {
        return "{id: '" + id + "', code: '" + code + "}\n";
    }

    public SupplierCode(String id, String code) {
        this.id = id;
        this.code = code;
    }

    public SupplierCode() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

