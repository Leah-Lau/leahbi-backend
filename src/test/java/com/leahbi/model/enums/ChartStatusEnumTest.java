package com.leahbi.model.enums;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChartStatusEnumTest {

    @Test
    void getValue() {
        System.out.println(ChartStatusEnum.FAILED.getValue());
        System.out.println(ChartStatusEnum.FAILED.getText());
        System.out.println(ChartStatusEnum.getValues());
        System.out.println(ChartStatusEnum.getEnumByValue("failed"));
    }
}