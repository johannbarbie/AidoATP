import java.io.IOException;
import java.math.BigDecimal;

import org.aido.atp.exchanges.ATPBTCChinaExchange;
import org.joda.money.BigMoney;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeException;
import com.xeiam.xchange.NotAvailableFromExchangeException;
import com.xeiam.xchange.NotYetImplementedForExchangeException;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.service.polling.PollingAccountService;
import com.xeiam.xchange.service.polling.PollingTradeService;


public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NotYetImplementedForExchangeException 
	 * @throws NotAvailableFromExchangeException 
	 * @throws ExchangeException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException, InterruptedException {
		Exchange ce = ATPBTCChinaExchange.newInstance("c3fc4283-2d9b-4ccb-87dc-2ae45e7c347d","dd4268e1-cfe8-47a2-8879-175b96ea751d");
		PollingAccountService pas = ce.getPollingAccountService();
		AccountInfo ai = pas.getAccountInfo();
		System.out.println(ai);
		
		PollingTradeService pts = ce.getPollingTradeService();
		pts.placeLimitOrder(new LimitOrder(OrderType.ASK,new BigDecimal("0.002829958"),"BTC","CNY","",null,BigMoney.parse("CNY 6762.824")));
		pas.getAccountInfo();
		//pts.placeLimitOrder(new LimitOrder(OrderType.BID,new BigDecimal("0.002829958"),"BTC","CNY","",null,BigMoney.parse("CNY 6750.3975")));
	}

}
