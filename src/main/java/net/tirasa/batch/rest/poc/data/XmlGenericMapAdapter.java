package net.tirasa.batch.rest.poc.data;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlGenericMapAdapter<K, V> extends XmlAdapter<GenericMapType<K, V>, Map<K, V>> {

    @Override
    public Map<K, V> unmarshal(final GenericMapType<K, V> v) throws Exception {
        Map<K, V> map = new HashMap<>();

        v.getEntry().forEach(mapEntryType -> map.put(mapEntryType.getKey(), mapEntryType.getValue()));

        return map;
    }

    @Override
    public GenericMapType<K, V> marshal(final Map<K, V> v) throws Exception {
        GenericMapType<K, V> mapType = new GenericMapType<>();

        v.entrySet().stream().map(entry -> {
            GenericMapEntryType<K, V> mapEntryType = new GenericMapEntryType<>();
            mapEntryType.setKey(entry.getKey());
            mapEntryType.setValue(entry.getValue());
            return mapEntryType;
        }).forEachOrdered(mapEntryType -> mapType.getEntry().add(mapEntryType));

        return mapType;
    }
}
