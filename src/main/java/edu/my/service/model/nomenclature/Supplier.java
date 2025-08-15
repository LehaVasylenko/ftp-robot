package edu.my.service.model.nomenclature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Supplier {
    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private String id;

    @JacksonXmlProperty(isAttribute = true, localName = "Name")
    private String name;

    @JacksonXmlProperty(isAttribute = true, localName = "Edrpo")
    private String edrpo;

    public Supplier(String id, String name, String edrpo) {
        this.id = id;
        this.name = name;
        this.edrpo = edrpo;
    }

    @Override
    public String toString() {
        return "Supplier{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", edrpo='" + edrpo + '\'' +
                '}';
    }

    public Supplier() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEdrpo() {
        return edrpo;
    }

    public void setEdrpo(String edrpo) {
        this.edrpo = edrpo;
    }
}
