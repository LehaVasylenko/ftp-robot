import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.my.service.model.nomenclature.NomenclatureIncoming;
import edu.my.service.model.nomenclature.Offer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

public class NomenclatureIncomingXmlTest {

    private static XmlMapper xml;

    @BeforeAll
    static void setup() {
        xml = (XmlMapper) new XmlMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final String XML_ATTR = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Data>
              <Suppliers>
                <Supplier ID="CFC69F90-EF04-4AC4-8380-8687A21B9B78" Name="Оптима" Edrpo="21642228"/>
                <Supplier ID="96208AED-C7F6-49F6-89D2-2801022063C6" Name="БАДМ" Edrpo="31816235"/>
              </Suppliers>
              <Offers>
                <Offer Code="238151" Name="Юмекс,тб,№50" Producer="Sanofi" VAT="20">
                  <SupplierCodes>
                    <SupplierCode ID="CFC69F90-EF04-4AC4-8380-8687A21B9B78" Code="YUMEX-1"/>
                    <SupplierCode ID="96208AED-C7F6-49F6-89D2-2801022063C6" Code="YUMEX-2"/>
                  </SupplierCodes>
                </Offer>
              </Offers>
            </Data>
            """;

    private static final String XML_ELEM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Data>
              <Suppliers>
                <Supplier>
                  <ID>CFC69F90-EF04-4AC4-8380-8687A21B9B78</ID>
                  <Name>Оптима</Name>
                  <Edrpo>21642228</Edrpo>
                </Supplier>
                <Supplier>
                  <ID>96208AED-C7F6-49F6-89D2-2801022063C6</ID>
                  <Name>БАДМ</Name>
                  <Edrpo>31816235</Edrpo>
                </Supplier>
              </Suppliers>
              <Offers>
                <Offer>
                  <Code>238151</Code>
                  <Name>Юмекс,тб,№50</Name>
                  <Producer>Sanofi</Producer>
                  <VAT>20</VAT>
                  <SupplierCodes>
                    <SupplierCode>
                      <ID>CFC69F90-EF04-4AC4-8380-8687A21B9B78</ID>
                      <Code>YUMEX-1</Code>
                    </SupplierCode>
                    <SupplierCode>
                      <ID>96208AED-C7F6-49F6-89D2-2801022063C6</ID>
                      <Code>YUMEX-2</Code>
                    </SupplierCode>
                  </SupplierCodes>
                </Offer>
              </Offers>
            </Data>
            """;

    @Test
    void parsesAttributeStyle() throws Exception {
        NomenclatureIncoming dto = xml.readValue(XML_ATTR.getBytes(StandardCharsets.UTF_8), NomenclatureIncoming.class);
        assertOk(dto);
    }

    @Test
    void parsesElementStyle() throws Exception {
        NomenclatureIncoming dto = xml.readValue(XML_ELEM.getBytes(StandardCharsets.UTF_8), NomenclatureIncoming.class);
        assertOk(dto);
    }

    @Test
    void bothStylesEquivalent() throws Exception {
        NomenclatureIncoming a = xml.readValue(XML_ATTR.getBytes(StandardCharsets.UTF_8), NomenclatureIncoming.class);
        NomenclatureIncoming b = xml.readValue(XML_ELEM.getBytes(StandardCharsets.UTF_8), NomenclatureIncoming.class);

        assertEquals(a.getSuppliers().size(), b.getSuppliers().size());
        assertEquals(a.getOffers().size(), b.getOffers().size());

        Offer ao = a.getOffers().getFirst();
        Offer bo = b.getOffers().getFirst();

        assertEquals(ao.getCode(), bo.getCode());
        assertEquals(ao.getName(), bo.getName());
        assertEquals(ao.getProducer(), bo.getProducer());
        assertEquals(ao.getVat(), bo.getVat());

        assertEquals(ao.getSupplierCodes().size(), bo.getSupplierCodes().size());
        assertEquals(ao.getSupplierCodes().getFirst().getId(), bo.getSupplierCodes().getFirst().getId());
        assertEquals(ao.getSupplierCodes().getFirst().getCode(), bo.getSupplierCodes().getFirst().getCode());
    }

    private static void assertOk(NomenclatureIncoming dto) {
        assertNotNull(dto);
        assertNotNull(dto.getSuppliers());
        assertEquals(2, dto.getSuppliers().size());
        assertEquals("Оптима", dto.getSuppliers().getFirst().getName());

        assertNotNull(dto.getOffers());
        assertEquals(1, dto.getOffers().size());
        Offer offer = dto.getOffers().getFirst();
        assertEquals("238151", offer.getCode());
        assertEquals("Юмекс,тб,№50", offer.getName());
        assertEquals("Sanofi", offer.getProducer());
        assertEquals(20f, offer.getVat());

        assertNotNull(offer.getSupplierCodes());
        assertEquals(2, offer.getSupplierCodes().size());
        assertEquals("YUMEX-1", offer.getSupplierCodes().getFirst().getCode());
    }
}

