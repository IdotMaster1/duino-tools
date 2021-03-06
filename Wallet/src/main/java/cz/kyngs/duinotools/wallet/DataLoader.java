/*
 * MIT License
 *
 * Copyright (c) 2020 kyngs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cz.kyngs.duinotools.wallet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cz.kyngs.duinotools.wallet.network.Protocol;
import cz.kyngs.duinotools.wallet.utils.concurrent.ConcurrentSet;
import cz.kyngs.duinotools.wallet.utils.thread.MultiThreadUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Main DataLoader.
 *
 * @author kyngs
 * @see cz.kyngs.duinotools.wallet.utils.thread.MultiThreadUtil
 */
public class DataLoader extends MultiThreadUtil {

    /**
     * Gson constant used for reading JSON API data.
     */
    private static final Gson GSON;

    static {
        GSON = new Gson();
    }

    private final Protocol protocol;
    private final Set<StatisticListener> listeners;
    private double balance;
    private double price;

    /**
     * Main and only constructor
     * @param wallet Main class
     * @throws IOException
     * @see Wallet
     */
    public DataLoader(Wallet wallet) throws IOException {
        super("Data", 2);
        protocol = new Protocol(wallet.getNetwork());
        listeners = new ConcurrentSet<>();
        balance = protocol.getBalance();
        parseJSON();
        startTasks();
        Wallet.LOGGER.info(String.format("Balance is: %s", balance));
    }

    /**
     * @return Current duino price according to API.
     */
    public double getPrice() {
        return price;
    }

    /**
     * Starting tasks for retrieving data.
     */
    private void startTasks() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                scheduleTask(() -> {
                    try {
                        DataLoader.this.parseJSON();
                        for (StatisticListener statisticListener : listeners) {
                            statisticListener.onStatisticsUpdate(DataLoader.this);
                        }
                    } catch (Exception e) {
                        Wallet.LOGGER.warn("Cannot fetch data from public API!", e);
                    }
                });
            }
        }, TimeUnit.SECONDS.toMillis(91), TimeUnit.SECONDS.toMillis(91));
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                scheduleTask(() -> {
                    try {
                        balance = protocol.getBalance();
                        for (StatisticListener statisticListener : listeners) {
                            statisticListener.onBalanceUpdate(DataLoader.this);
                        }
                    } catch (IOException e) {
                        Wallet.LOGGER.warn("Cannot fetch balance data!", e);
                    }
                });
            }
        }, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(1));
    }

    /**
     * Task for parsing JSON from public API
     *
     * @throws IOException If I/O error occurs.
     */
    private void parseJSON() throws IOException {
        try (InputStream in = new URL("https://raw.githubusercontent.com/revoxhere/duco-statistics/master/api.json").openStream()) {
            Map<String, String> values = GSON.fromJson(new InputStreamReader(in), new TypeToken<Map<String, String>>() {
            }.getType());
            price = Double.parseDouble(values.get("Duco price"));
        }
    }

    /**
     * Simple  method for adding Listener
     *
     * @param statisticListener listener to be added
     */
    public void registerListener(StatisticListener statisticListener) {
        listeners.add(statisticListener);
    }

    /**
     * Simple method for removing listener.
     *
     * @param listener listener to be removed.
     */
    public void unregisterListener(StatisticListener listener) {
        listeners.remove(listener);
    }


    /**
     * @return balance of user.
     */
    public double getBalance() {
        return balance;
    }
}
