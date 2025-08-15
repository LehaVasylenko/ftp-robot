package edu.my.service.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.*;
import edu.my.service.model.nomenclature.NomenclatureIncoming;
import edu.my.service.model.nomenclature.Offer;
import edu.my.service.model.nomenclature.Supplier;
import edu.my.service.model.nomenclature.SupplierCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NomenclatureIncomingDeserializer extends StdDeserializer<NomenclatureIncoming> {
    public NomenclatureIncomingDeserializer() { super(NomenclatureIncoming.class); }

    @Override
    public NomenclatureIncoming deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        ObjectNode root = codec.readTree(p);

        NomenclatureIncoming out = new NomenclatureIncoming();
        out.setOffers(parseOffers(root));
        out.setSuppliers(parseSuppliers(root));
        return out;
    }

    // ---------- Offers ----------
    private List<Offer> parseOffers(ObjectNode root) {
        List<Offer> list = new ArrayList<>();
        ObjectNode offersNode = asObject(root.get("Offers"));
        if (offersNode == null) return list;

        for (JsonNode offerNode : asArrayOrSingleton(offersNode.get("Offer"))) {
            ObjectNode o = asObject(offerNode);
            if (o == null) continue;

            Offer offer = new Offer();
            offer.setCode(pickText(o, "@Code", "Code"));
            offer.setName(pickText(o, "@Name", "Name"));
            offer.setProducer(pickText(o, "@Producer", "Producer"));
            offer.setVat(pickFloat(o, "@VAT", "VAT"));

            // SupplierCodes может прийти как контейнер <SupplierCodes><SupplierCode .../></SupplierCodes>
            // или вдруг сразу как массив элементов (на всякий случай поддержим оба)
            ObjectNode supplierCodesContainer = asObject(o.get("SupplierCodes"));
            JsonNode supplierCodesNode = supplierCodesContainer != null ? supplierCodesContainer.get("SupplierCode") : o.get("SupplierCode");
            offer.setSupplierCodes(parseSupplierCodes(supplierCodesNode));

            list.add(offer);
        }
        return list;
    }

    private List<SupplierCode> parseSupplierCodes(JsonNode node) {
        List<SupplierCode> list = new ArrayList<>();
        for (JsonNode scNode : asArrayOrSingleton(node)) {
            ObjectNode sc = asObject(scNode);
            if (sc == null) continue;

            SupplierCode s = new SupplierCode();
            s.setId(pickText(sc, "@ID", "ID"));
            s.setCode(pickText(sc, "@Code", "Code"));
            list.add(s);
        }
        return list;
    }

    // ---------- Suppliers ----------
    private List<Supplier> parseSuppliers(ObjectNode root) {
        List<Supplier> list = new ArrayList<>();
        ObjectNode suppliersNode = asObject(root.get("Suppliers"));
        if (suppliersNode == null) return list;

        for (JsonNode supNode : asArrayOrSingleton(suppliersNode.get("Supplier"))) {
            ObjectNode sNode = asObject(supNode);
            if (sNode == null) continue;

            Supplier s = new Supplier();
            s.setId(pickText(sNode, "@ID", "ID"));
            s.setName(pickText(sNode, "@Name", "Name"));
            s.setEdrpo(pickText(sNode, "@Edrpo", "Edrpo"));
            list.add(s);
        }
        return list;
    }

    // ---------- helpers ----------
    private static ObjectNode asObject(JsonNode node) {
        return (node instanceof ObjectNode on) ? on : null;
    }

    private static List<JsonNode> asArrayOrSingleton(JsonNode node) {
        List<JsonNode> out = new ArrayList<>();
        if (node == null || node.isNull()) return out;
        if (node.isArray()) {
            node.forEach(out::add);
        } else {
            out.add(node);
        }
        return out;
    }

    private static String pickText(ObjectNode node, String... names) {
        for (String n : names) {
            JsonNode v = node.get(n);
            if (v != null && !v.isNull()) return v.asText();
        }
        return null;
    }

    private static Float pickFloat(ObjectNode node, String... names) {
        String s = pickText(node, names);
        if (s == null || s.isBlank()) return null;
        s = s.replace(',', '.');
        try { return Float.valueOf(s); } catch (NumberFormatException e) { return null; }
    }
}

