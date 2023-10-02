/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JFrame;

import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;

public class CS16Viewer {
    S16Image[] fr;
    BufferedImage bi;
    int frI = 0;

    public CS16Viewer(S16Image[] fr) {
        this.fr = fr;
    }

    public static void main(String[] args) throws IOException {
        FileDialog fd = new FileDialog((JFrame) null);
        fd.setVisible(true);
        S16Image[] fr = CS16IO.decodeCS16(fd.getFiles()[0]);
        new CS16Viewer(fr).doTheThing();
    }

    @SuppressWarnings("serial")
    public void doTheThing() {
        JFrame testFrame = new JFrame();
        testFrame.setSize(400, 400);
        Canvas testCanvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                int w = getWidth();
                int h = getHeight();
                g.setColor(Color.black);
                g.fillRect(0, 0, w, h);
                g.setColor(Color.white);
                g.drawString(Integer.toString(frI), 8, 16);
                if (bi != null)
                    g.drawImage(bi, 0, 24, null);
            }
        };
        testFrame.add(testCanvas);
        testFrame.setVisible(true);
        testCanvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    frI--;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    frI++;
                }
                if (frI < 0 || frI >= fr.length) {
                    bi = null;
                } else {
                    bi = fr[frI].toBI(true);
                }
                testCanvas.repaint();
            }
        });
    }
}
