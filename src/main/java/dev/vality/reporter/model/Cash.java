package dev.vality.reporter.model;

import dev.vality.damsel.domain.Currency;
import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Data
public class Cash {
    private AtomicLong amount;
    private Currency currency;

    public Cash() {
        amount = new AtomicLong();
        currency = new Currency();
    }
}
