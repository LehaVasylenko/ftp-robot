package edu.my.service.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.my.service.model.rests.OfferRests;

import java.io.IOException;

public class OfferRestsDeserializer extends StdDeserializer<OfferRests> {
    public OfferRestsDeserializer() { super(OfferRests.class); }

    @Override
    public OfferRests deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        ObjectNode node = codec.readTree(p);

        OfferRests o = new OfferRests();
        o.setCode( pickText(node, "@Code", "Code") );
        o.setName( pickText(node, "@Name", "Name") );
        o.setProducer( pickText(node, "@Producer", "Producer") );
        o.setPrice( pickFloat(node, "@Price", "Price") );
        o.setQuantity( pickFloat(node, "@Quantity", "Quantity") );
        o.setPriceReserve( pickFloat(node, "@PriceReserve", "PriceReserve") );
        o.setMorionId( pickText(node, "@Code1", "Code1") );
        //На будущее: Tax, Barcode, Code7/8/9 и т.д. — по тому же шаблону
        return o;
    }

    private static String pickText(ObjectNode node, String... names) {
        for (String n : names) {
            var v = node.get(n);
            if (v != null && !v.isNull()) return v.asText();
        }
        return null;
    }

    private static Float pickFloat(ObjectNode node, String... names) {
        String s = pickText(node, names);
        if (s == null || s.isBlank()) return null;
        // на всякий: если приходят запятые
        s = s.replace(',', '.');
        try { return Float.valueOf(s); } catch (NumberFormatException e) { return null; }
    }
}

