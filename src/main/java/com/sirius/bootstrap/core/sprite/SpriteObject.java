package com.sirius.bootstrap.core.sprite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirius.bootstrap.core.event.Event;
import com.sirius.bootstrap.core.event.EventListener;
import com.sirius.bootstrap.core.frame.Frame;
import com.sirius.bootstrap.core.ioc.AutoBean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class SpriteObject extends Frame {
    @Autowired
    protected ObjectMapper objectMapper;

    protected Map<Class<?>, Object> poolMap = new HashMap<>();

    protected Map<Class<?>, List<Consumer<Event>>> eventConsumerMap = new HashMap<>();

    public void publishEvent(Event event) {
        eventConsumerMap.get(event.getClass()).forEach(consumer -> consumer.accept(event));
    }

    @Override
    public void init() {
        super.init();
        poolMap.forEach((aClass, roleBean) -> {
            try {
                for (Field field : aClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(AutoBean.class)) {
                        Object bean = poolMap.get(field.getType());
                        field.set(roleBean, bean);
                    }
                }
                for (Method method : aClass.getDeclaredMethods()) {
                    method.setAccessible(true);
                    if (method.isAnnotationPresent(EventListener.class)) {
                        EventListener listener = method.getAnnotation(EventListener.class);
                        eventConsumerMap.compute(listener.type(), (id, list) -> {
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            list.add(eventObject -> {
                                try {
                                    method.invoke(roleBean, eventObject);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            return list;
                        });
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
