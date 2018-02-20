package ro.stancalau.datamining.store;

import lombok.ToString;
import ro.stancalau.datamining.model.Entity;

import java.util.HashMap;
import java.util.Map;

@ToString
public class EntityStore<T extends Entity> {

    private Map<String, T> map = new HashMap<>();

    public void put(T entity) {
        map.put(entity.getId(), entity);
    }

    public T getById(String id){
        return map.get(id);
    }

    public Map<String, T> getMap() {
        return map;
    }
}
