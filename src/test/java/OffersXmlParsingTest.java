import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.my.service.model.rests.OfferRests;
import edu.my.service.model.rests.OffersWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OffersXmlParsingTest {

    private static XmlMapper xml;

    @BeforeAll
    static void setup() {
        xml = (XmlMapper) new XmlMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Формат 1: всё в АТРИБУТАХ (может быть <Offer .../> самозакрывающийся)
    private static final String XML_ATTR =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <Offers>
              <Offer Code="ЦБ000017515"
                     Name="Глюкометр Ван Тач Селект Симпл"
                     Producer="Китай"
                     Tax="7"
                     Price="466.00"
                     Quantity="1.000"
                     PriceReserve="390.10"
                     Barcode="4030841001825"
                     Code1="190996"
                     Code7="993.0389"
                     Code9="13372653"
                     UnknownField="ignored-by-jackson" />
            </Offers>
            """;

    // Формат 2: те же данные во ВЛОЖЕННЫХ ТЕГАХ
    private static final String XML_ELEM =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <Offers>
              <Offer>
                <Code>ЦБ000017515</Code>
                <Name>Глюкометр Ван Тач Селект Симпл</Name>
                <Producer>Китай</Producer>
                <Tax>7</Tax>
                <Price>466.00</Price>
                <Quantity>1.000</Quantity>
                <PriceReserve>390.10</PriceReserve>
                <Barcode>4030841001825</Barcode>
                <Code1>190996</Code1>
                <Code7>993.0389</Code7>
                <Code9>13372653</Code9>
                <AlsoUnknown>ignored-as-well</AlsoUnknown>
              </Offer>
            </Offers>
            """;

    @Test
    void parsesAttributeStyle() throws Exception {
        OffersWrapper wrapper = xml.readValue(XML_ATTR.getBytes(StandardCharsets.UTF_8), OffersWrapper.class);
        assertNotNull(wrapper);
        assertNotNull(wrapper.getOffers());
        assertEquals(1, wrapper.getOffers().size());

        OfferRests o = wrapper.getOffers().getFirst();
        assertOffer(o);
    }

    @Test
    void parsesElementStyle() throws Exception {
        OffersWrapper wrapper = xml.readValue(XML_ELEM.getBytes(StandardCharsets.UTF_8), OffersWrapper.class);
        assertNotNull(wrapper);
        assertNotNull(wrapper.getOffers());
        assertEquals(1, wrapper.getOffers().size());

        OfferRests o = wrapper.getOffers().getFirst();
        assertOffer(o);
    }

    @Test
    void bothStylesProduceIdenticalObjects() throws Exception {
        OffersWrapper a = xml.readValue(XML_ATTR.getBytes(StandardCharsets.UTF_8), OffersWrapper.class);
        OffersWrapper b = xml.readValue(XML_ELEM.getBytes(StandardCharsets.UTF_8), OffersWrapper.class);

        OfferRests oa = a.getOffers().getFirst();
        OfferRests ob = b.getOffers().getFirst();

        //ключевые поля
        assertEquals(oa.getCode(), ob.getCode());
        assertEquals(oa.getName(), ob.getName());
        assertEquals(oa.getProducer(), ob.getProducer());
        assertEquals(oa.getPrice(), ob.getPrice());
        assertEquals(oa.getQuantity(), ob.getQuantity());
        assertEquals(oa.getPriceReserve(), ob.getPriceReserve());
        assertEquals(oa.getMorionId(), ob.getMorionId());
    }

    private static void assertOffer(OfferRests o) {
        assertEquals("ЦБ000017515", o.getCode());
        assertEquals("Глюкометр Ван Тач Селект Симпл", o.getName());
        assertEquals("Китай", o.getProducer());
        // ты хотел float/double — ок
        assertEquals(466.00f, o.getPrice(), 0.0001f);
        assertEquals(1.000f, o.getQuantity(), 0.0001f);
        assertEquals(390.10f, o.getPriceReserve(), 0.0001f);
        assertEquals("190996", o.getMorionId());
    }
}

