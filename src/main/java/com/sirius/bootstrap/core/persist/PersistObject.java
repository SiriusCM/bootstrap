package com.sirius.bootstrap.core.persist;

import com.sirius.bootstrap.core.frame.Frame;
import com.sirius.bootstrap.core.frame.FrameThread;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Scope("prototype")
@Component
public class PersistObject extends Frame {
    @Autowired
    @Qualifier("persistThread")
    private List<FrameThread> persistThreadList;

    @PostConstruct
    public void postConstruct() {
        int index = (int) (persistThreadList.size() * Math.random());
        FrameThread thread = persistThreadList.get(index);
        bindThread(thread);
    }
}
