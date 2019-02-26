package com.kgeleta;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Emulator extends JFrame implements KeyListener, ActionListener, ItemListener
{
    private final int offsetX = 10;
    private final int offsetY = 60;

    private final int smallX = 660;
    private final int smallY = 400;
    private final int smallPixelSize = 10;

    private final int mediumX = 990;
    private final int mediumY = 560;
    private final int mediumPixelSize = 15;

    private final int bigX = 1300;
    private final int bigY = 720;
    private final int bigPixelSize = 20;

    private int pixelSize = smallPixelSize;

    private AtomicBoolean pause = new AtomicBoolean(true);
    private AtomicBoolean fileLoaded = new AtomicBoolean(false);
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
        MenuItem speed = new MenuItem("Emulation speed");
        speed.addActionListener(this);

//        sound on/off

//        screen size
        Menu menuScreenSize = new Menu("Screen size");
        MenuItem screenSmall = new MenuItem("Small");
        screenSmall.addActionListener(this);
        MenuItem screenMedium = new MenuItem("Medium");
        screenMedium.addActionListener(this);
        MenuItem screenBig = new MenuItem("Big");
        screenBig.addActionListener(this);

        menuScreenSize.add(screenSmall);
        menuScreenSize.add(screenMedium);
        menuScreenSize.add(screenBig);

//        colors (?)

//        Help

        menuSettings.add(menuScreenSize);
        menuSettings.add(speed);


        menuBar.add(menuFile);
        menuBar.add(menuSettings);

        setMenuBar(menuBar);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(smallX,smallY);
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
                    g.fillRect(pixelSize*x + offsetX,pixelSize*y + offsetY,pixelSize,pixelSize);
            }
    }

    // emulation loop:

    public void emulateLoop()
    {
        while(true)
        {
            if(!pause.get() & fileLoaded.get())
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
        pause.set(true);
        switch(e.getActionCommand())
        {
            case "Open ROM...":
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
                    fileLoaded.set(true);
                }
                break;

            case "Exit":
                System.exit(0);
                break;

            case "Small":
                setSize(smallX,smallY);
                pixelSize = smallPixelSize;
                break;

            case "Medium":
                setSize(mediumX,mediumY);
                pixelSize = mediumPixelSize;
                break;

            case "Big":
                setSize(bigX,bigY);
                pixelSize = bigPixelSize;
                break;

            case "Emulation speed":
                String speedStr = JOptionPane.showInputDialog(this,"Number of milliseconds for single emulation cycle: ", chip8.emulationSpeed);
                try
                {
                    int speedInt = Integer.parseInt(speedStr);
                    if(speedInt < 0 || speedInt > 5000)
                        JOptionPane.showMessageDialog(this, "Value should be in range 0 to 5000", "Wrong value", JOptionPane.WARNING_MESSAGE);
                    else
                        chip8.emulationSpeed = speedInt;
                } catch (NumberFormatException nfe)
                {
                    JOptionPane.showMessageDialog(this, "Wrong format!", "Format error", JOptionPane.ERROR_MESSAGE);
                }
                break;
        }
        pause.set(false);
    }

    public static void main(String[] args) {
        Emulator emulator = new Emulator();
        emulator.emulateLoop();


    }
}
