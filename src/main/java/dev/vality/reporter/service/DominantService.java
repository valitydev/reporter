package dev.vality.reporter.service;

import dev.vality.damsel.domain.Currency;
import dev.vality.damsel.domain.ShopConfig;

import java.util.Map;

public interface DominantService {

    Currency getCurrency(String currencyCode);

    Map<String, ShopConfig> getShopConfigs(String partyId);
}
