package com.zs.forex.task;

import com.zs.forex.service.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Slf4j
@AllArgsConstructor
@Component
public class StartUp implements CommandLineRunner {


    private final SystemModeService systemModeService;

    @Override
    public void run(String... args) throws InterruptedException {

         systemModeService.init();

    }
}
