/**
 * Copyright (c) 2013 Aido
 * 
 * This file is part of Aido ATP.
 * 
 * Aido ATP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Aido ATP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Aido ATP.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aido.atp;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.xchange.ExchangeException;
import com.xeiam.xchange.NotAvailableFromExchangeException;
import com.xeiam.xchange.NotYetImplementedForExchangeException;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.trade.Wallet;
import com.xeiam.xchange.service.polling.PollingAccountService;

/**
* Account manager class.
*
* @author Aido
*/

public class AccountManager implements Runnable {

	private static HashMap<String, AccountManager> instances = new HashMap<String, AccountManager>();
	private String exchangeName;
	private AccountInfo accountInfo;
	private HashMap<CurrencyUnit, ArrayList<BigMoney>> books;//We only look at first and last right now, but it would be handy to have changes over time as well.
	private static Logger log;
	private PollingAccountService accountService;
	private List<Wallet> wallets;

	public static AccountManager getInstance(String exchangeName) {
		if(!instances.containsKey(exchangeName))
			instances.put(exchangeName, new AccountManager(exchangeName));
		return instances.get(exchangeName);
	}

	private AccountManager(String exchangeName){
		this.exchangeName = exchangeName;
		try {
			log = LoggerFactory.getLogger(AccountManager.class);
			books = new HashMap<CurrencyUnit, ArrayList<BigMoney>>();

			// Interested in the private account functionality (authentication)
			accountService = ExchangeManager.getInstance(exchangeName).getExchange().getPollingAccountService();

			// Get the account information
			accountInfo = accountService.getAccountInfo();
			log.info("{} AccountInfo as String: {}",exchangeName,accountInfo.toString());
			refreshAccounts();
			startTickers();
		} catch (com.xeiam.xchange.ExchangeException e) {
			Socket testSock = null;
			while (true) {
				try {
					log.warn("WARNING: Testing connection to exchange {}",exchangeName);
					testSock = new Socket(ExchangeManager.getInstance(exchangeName).getHost(),ExchangeManager.getInstance(exchangeName).getPort());
					if (testSock != null) { break; }
				}
				catch (java.io.IOException e1) {
					try {
						log.error("ERROR: Cannot connect to exchange {}. Sleeping for one minute",exchangeName);
						TimeUnit.MINUTES.sleep(1);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			log.error("ERROR: Caught unexpected {} exception, shutting down now!.Details are listed below.",exchangeName);
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public synchronized void run() {
		refreshAccounts();
	}

	public BigMoney getBalance(CurrencyUnit currency) throws WalletNotFoundException{
		refreshAccounts();
		wallets = accountInfo.getWallets();

		for(Wallet wallet : wallets){
			BigMoney balance = wallet.getBalance();
			CurrencyUnit unit = balance.getCurrencyUnit();

			if(unit.equals(currency)){
				return balance;
			}
		}
		log.error("ERROR: Could not find a {} wallet for the currency {}. Exiting now!",exchangeName,currency);
		throw new WalletNotFoundException();
	}

	public synchronized void refreshAccounts() {
		try {
			accountInfo = accountService.getAccountInfo();
			updateBooks();
			ProfitLossAgent.getInstance().updateBalances(accountInfo.getWallets());
		} catch (ExchangeException | NotAvailableFromExchangeException
				| NotYetImplementedForExchangeException | IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	private void updateBooks() {
		wallets = accountInfo.getWallets();
		for(Wallet wallet : wallets){
			CurrencyUnit currency = wallet.getBalance().getCurrencyUnit();

			//Do we have a new currency in our wallet?
			if(!books.containsKey(currency)){
				//Make some space for it.
				books.put(currency,new ArrayList<BigMoney>());
			}

			ArrayList<BigMoney> ledger = books.get(currency);
			ledger.add(wallet.getBalance());
		}
	}

	public AccountInfo getAccountInfo() {
		return accountInfo;
	}

	public void startTickers() {
		ThreadGroup tickerThreadGroup = new ThreadGroup("Tickers");
		for(Wallet wallet : wallets) {
			CurrencyUnit currency = wallet.getBalance().getCurrencyUnit();
			
			if(!currency.getCode().equals("BTC") && !currency.getCode().equals("NMC") && !currency.getCode().equals("LTC") && !currency.getCode().equals("PPC") && !currency.getCode().equals("TRC") && !currency.getCode().equals("NVC") && !currency.getCode().equals("FTC") && !currency.getCode().equals("CNC") ) {				
				if (Application.getInstance().getConfig("UseLocalOnly").equals("1")) {
					if (currency.getCode().equals(Application.getInstance().getConfig("LocalCurrency"))) {
						Thread tickermanagerManagerThread = new Thread(tickerThreadGroup,TickerManager.getInstance(exchangeName,currency));
						tickermanagerManagerThread.start();
					}
				} else {
					Thread tickermanagerManagerThread = new Thread(tickerThreadGroup,TickerManager.getInstance(exchangeName,currency));
					tickermanagerManagerThread.start();
				}
			}			
		}
	}
}
