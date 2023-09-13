/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFrame;

import cdsp.common.app.GameInfo;
import cdsp.common.app.JButtonWR;
import cdsp.common.app.JGameInfo;

@SuppressWarnings("serial")
public class Main extends JFrame {
    public final GameInfo gameInfo = new GameInfo();
    public Main() {
        // Load config
        gameInfo.loadFromDefaultLocation();
        // Continue
        setTitle("cdsp-tools");
        setLayout(new GridLayout(0, 1));
        setAlwaysOnTop(true);
        add(new JButtonWR("Configuration", () -> {
            JDialog configPage = new JDialog(Main.this, "Configuration");
            configPage.add(new JGameInfo(gameInfo));
            configPage.setSize(800, 600);
            configPage.setVisible(true);
        }));
        add(new JButtonWR("View C16/S16", () -> {
            
        }));
        pack();
        setLocationByPlatform(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        new Main();
    }
}
