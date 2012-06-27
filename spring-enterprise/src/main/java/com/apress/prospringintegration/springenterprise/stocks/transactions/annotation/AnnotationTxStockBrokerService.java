/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apress.prospringintegration.springenterprise.stocks.transactions.annotation;

import com.apress.prospringintegration.springenterprise.stocks.dao.StockBrokerService;
import com.apress.prospringintegration.springenterprise.stocks.dao.StockDao;
import com.apress.prospringintegration.springenterprise.stocks.model.BestAsk;
import com.apress.prospringintegration.springenterprise.stocks.model.Order;
import com.apress.prospringintegration.springenterprise.stocks.model.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

@Component
public class AnnotationTxStockBrokerService implements StockBrokerService {

    @Autowired
    private HibernateTemplate hibernateTemplate;

    @Autowired
    @Qualifier("hibernateStockDao")
    private StockDao stockDao;

    Random random = new Random();

    @Transactional
    public void preFillStocks(final String exchangeId, final String... symbols) {
        int i = 0;
        for (String sym : symbols) {
            float pp = (float) Math.random() * 100.0f;
            int qq = (int) Math.random() * 250;
            Stock s = new Stock(sym, "INV00" + i, exchangeId, pp, qq, Calendar
                    .getInstance().getTime());
            stockDao.insert(s);
            System.out.println("ORIG INVENTORY: " + s.getInventoryCode() + " ");
            int randomized = (random.nextInt(100) % 4) == 0 ? 0 : i;
            s.setInventoryCode("INV00" + randomized);
            System.out.println("NEW RANDOMIZED INVENTORY:"
                    + s.getInventoryCode() + " " + randomized);
            stockDao.update(s);
            i++;
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ,
            timeout = 30,
            readOnly = true)
    @SuppressWarnings("unchecked")
    public BestAsk findBestPrice(Order o) throws Exception {
        BestAsk bestAsk = new BestAsk();
        List<Stock> rets = (List<Stock>) hibernateTemplate.find("from Stock s where " +
                "s.pricePerShare <= ? and " +
                "s.quantityAvailable >= ? and s.symbol = ?",
                new Object[]{o.getBid(), o.getQuantity(), o.getSymbol()}, String.class);
        if (!rets.isEmpty()) {
            Stock stock = stockDao.findByInventoryCode(rets.get(0).getInventoryCode());
            bestAsk.setStock(stock);
        }

        return bestAsk;
    }

}
