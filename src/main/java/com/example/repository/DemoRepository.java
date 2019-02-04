package com.example.repository;

import com.example.domain.Demo;
import com.example.domain.DemoAgePatch;
import lombok.NonNull;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DemoRepository {

    private static AtomicLong autoIncrementedId = new AtomicLong();

    private Map<Long, Demo> demoEntities = Collections.synchronizedMap(new HashMap<>());

    public List<Demo> findAll() {
        return new ArrayList<>(demoEntities.values());
    }

    public Optional<Demo> findById(Long id) {
        return Optional.ofNullable(demoEntities.get(id));
    }

    public Demo save(Demo demo) {
        Long id =
            Optional.ofNullable(demo.getId())
                .orElseGet(() -> autoIncrementedId.incrementAndGet());
        demo.setId(id);
        demoEntities.put(id, demo);
        return demo;
    }

    public Demo save(Long id, DemoAgePatch demoAgePatch) {
        Objects.requireNonNull(id);
        Demo demo = findById(id).orElseThrow(ResourceNotFoundException::new);
        demo.setAge(demoAgePatch.getAge());
        demoEntities.put(id, demo);
        return demo;
    }
}
