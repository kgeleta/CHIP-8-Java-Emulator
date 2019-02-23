package com.kgeleta;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

public class Graphic extends JFrame implements KeyListener
{
    private Chip8 chip8 = new Chip8();
    private final char[] keyMap = {'x','1','2','3','q','w','e','a','s','d','z','c','4','r','f','v'};
                                //  0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F

    public Graphic()
    {
        super();
        addKeyListener(this);
    }

    @Override
    public void paint(Graphics g)
    {
        // clear screen with black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.WHITE);
        for(int x = 0; x < 64; x++)
            for(int y = 0; y < 32; y++)
            {
                if(chip8.gfx[x][y])
                    g.fillRect(10*x,10*(y+5),10,10);
            }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        for(int i = 0; i < keyMap.length; i++)
            if(e.getKeyChar() == keyMap[i])
                chip8.key[i] = true;

    }

    @Override
    public void keyTyped(KeyEvent e){}

    @Override
    public void keyReleased(KeyEvent e)
    {
        for(int i = 0; i < keyMap.length; i++)
            if(e.getKeyChar() == keyMap[i])
                chip8.key[i] = false;
    }

    public static void main(String[] args) {
        Graphic graphic = new Graphic();
        graphic.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        graphic.setSize(660,380);
        graphic.setResizable(false);
        graphic.setVisible(true);

        // initialize:
        graphic.chip8.initialize();

        // loadFile ROM
//        String filePath = "C:\\Users\\Leslie\\Documents\\chip 8\\Blinky2.ch8";
//        String filePath = "C:\\Users\\Leslie\\Documents\\chip 8\\Pong.ch8";
//        String filePath = "C:\\Users\\Leslie\\Documents\\chip 8\\Space Invaders.ch8";
        String filePath = "C:\\Users\\Leslie\\Documents\\chip 8\\Space Intercept.ch8";

        try {
            graphic.chip8.loadFile(filePath);
        }catch(IOException ioe)
        {
            System.err.println("Wrong file path!");
        }

        // emulation loop:
        for(;;)
        {
            // emulate single cycle:
            graphic.chip8.cycle();
            // draw output
            if(graphic.chip8.drawFlag)
                graphic.repaint();

        }


    }
}
