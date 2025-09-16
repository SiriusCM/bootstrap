package com.sirius.bootstrap.core.rpc;

import com.sirius.bootstrap.core.tick.TickObject;
import com.sirius.bootstrap.core.tick.TickThread;
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
public class RpcObject extends TickObject {
    @Autowired
    @Qualifier("rpcThread")
    private List<TickThread> rpcThreadList;

    @PostConstruct
    public void postConstruct() {
        int index = (int) (rpcThreadList.size() * Math.random());
        TickThread thread = rpcThreadList.get(index);
        bindThread(thread);
    }
}
