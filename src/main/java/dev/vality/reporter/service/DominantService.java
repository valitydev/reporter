package dev.vality.reporter.service;

import dev.vality.damsel.domain.Currency;
import dev.vality.damsel.domain.CurrencyRef;

public interface DominantService {

    Currency getCurrency(String currencyCode);
}
