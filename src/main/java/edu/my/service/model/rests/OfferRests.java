package edu.my.service.model.rests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.my.service.serializer.OfferRestsDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = OfferRestsDeserializer.class)
public class OfferRests {
    private String code;
    private String name;
    private String producer;
    private Float price;
    private Float quantity;
    private Float priceReserve;
    private String morionId; // Code1

    public OfferRests() {
    }

    public OfferRests(String code, String name, String producer, Float price, Float quantity, Float priceReserve, String morionId) {
        this.code = code;
        this.name = name;
        this.producer = producer;
        this.price = price;
        this.quantity = quantity;
        this.priceReserve = priceReserve;
        this.morionId = morionId;
    }

    @Override
    public String toString() {
        return "{code: '" + code + "', name: '" + name + "', producer: '" + producer + "', price: " + price +
                ", quantity: " + quantity + ", priceReserve: " + priceReserve + ", morionId: '" + morionId + "'}\n";
    }

    //геттеры/сеттеры
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProducer() { return producer; }
    public void setProducer(String producer) { this.producer = producer; }
    public Float getPrice() { return price; }
    public void setPrice(Float price) { this.price = price; }
    public Float getQuantity() { return quantity; }
    public void setQuantity(Float quantity) { this.quantity = quantity; }
    public Float getPriceReserve() { return priceReserve; }
    public void setPriceReserve(Float priceReserve) { this.priceReserve = priceReserve; }
    public String getMorionId() { return morionId; }
    public void setMorionId(String morionId) { this.morionId = morionId; }
}
