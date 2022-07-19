package com.loreal.clock.Task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncController {
    @Autowired
    private AsyncService asyncService;

    @GetMapping("/open/something")
    public String something() {
        int count = 100;
        for (int i = 0; i < count; i++) {
            asyncService.doSomething("index = " + i);
        }
        return "success";
    }
}


@Slf4j
@Service
class AsyncService {

    // 指定使用beanname为doSomethingExecutor的线程池
    @Async("doSomethingExecutor")
    public String doSomething(String message) {
        log.info("do something, message={}", message);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("do something error: ", e);
        }
        return message;
    }

}