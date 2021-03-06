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

package cz.kyngs.duinotools.miner.core;

import cz.kyngs.duinotools.miner.Miner;
import cz.kyngs.duinotools.miner.network.Network;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MinerThreadGroup extends ThreadGroup {

    private final MinerThread[] minerThreads;

    private Timer timer;

    public MinerThreadGroup(int count, Miner miner) {
        super("Miner");
        minerThreads = new MinerThread[count];
        timer = new Timer();
        Network[] networks = new Network[count];
        for (int i = 0; i < networks.length; i++) {
            try {
                networks[i] = new Network(miner);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < minerThreads.length; i++) {
            minerThreads[i] = new MinerThread(miner, i, this, networks[i]);
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int hs = 0;
                for (MinerThread minerThread : minerThreads) {
                    hs += minerThread.getHashCountPerSecond();
                    minerThread.setHashCountPerSecond(0);
                }
                System.out.println(hs);
            }
        }, 1000, 1000);
    }

    public Timer getTimer() {
        return timer;
    }

}
