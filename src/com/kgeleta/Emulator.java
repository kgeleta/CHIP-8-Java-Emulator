package com.kgeleta;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Emulator extends JFrame implements KeyListener, ActionListener, ItemListener
{
    private AtomicBoolean pause = new AtomicBoolean(true);// = false;
    private Chip8 chip8 = new Chip8();
    private final char[] keyMap = {'x','1','2','3','q','w','e','a','s','d','z','c','4','r','f','v'};
                                //  0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F

    public Emulator()
    {
        super();
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }

        // Menu:
        MenuBar menuBar = new MenuBar();

        Menu menuFile = new Menu("File");
        MenuItem open = new MenuItem("Open ROM...");
        open.addActionListener(this);
        open.setShortcut(new MenuShortcut(KeyEvent.getExtendedKeyCodeForChar('o'), false));
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener(this);
        CheckboxMenuItem pauseItem = new CheckboxMenuItem("Pause");
        pauseItem.setShortcut(new MenuShortcut(KeyEvent.getExtendedKeyCodeForChar('p'), false));
        pauseItem.addItemListener(this);

        menuFile.add(open);
        menuFile.add(pauseItem);
        menuFile.add(exit);

        Menu menuSettings = new Menu("Settings");
//        emulation speed
//        sound on/off
//        screen size
//        colors (?)

//        Help



        menuBar.add(menuFile);
        menuBar.add(menuSettings);

        setMenuBar(menuBar);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(660,400);
        setResizable(false);
        setVisible(true);
        addKeyListener(this);

        chip8.initialize();

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
                    g.fillRect(10*x + 10,10*y + 60,10,10);
            }
    }

    // emulation loop:

    public void emulateLoop()
    {
        while(true)
        {
            if(!pause.get())
            {
                // emulate single cycle:
                chip8.cycle();
                // draw output
                if (chip8.drawFlag)
                    repaint();
            }
        }
    }

    // KeyListener

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

    // itemListener

    @Override
    public void itemStateChanged(ItemEvent e) {
        if(e.getItem().equals("Pause"))
            pause.set(!pause.get());
    }


    // ActionListener

    @Override
    public void actionPerformed(ActionEvent e)
    {
        switch(e.getActionCommand())
        {
            case "Open ROM...":
                pause.set(true);
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "CHIP-8 ROMs", "c8", "ch8");
                chooser.setFileFilter(filter);
                chooser.setAcceptAllFileFilterUsed(false);
                int returnVal = chooser.showOpenDialog(this);
                if(returnVal == JFileChooser.APPROVE_OPTION)
                {
                    chip8.initialize();
                    try {
                        chip8.loadFile(chooser.getSelectedFile().getAbsolutePath());
                    }catch(IOException ioe) {System.err.println("Wrong file path!");}
                }
                pause.set(false);
                break;

            case "Exit":

                pause.set(true);
                System.exit(0);
                break;
        }

    }

    public static void main(String[] args) {
        Emulator emulator = new Emulator();
        emulator.emulateLoop();


    }
}
