package com.kgeleta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

public class Chip8
{
    private int opcode;     // 16 bit
    private int[] memory;   // 4096 x 8 bit
    private int[] V;        // registers 16 x 8 bit
    private int I;          // additional register using in memory operations (16 bits)
    private int pc;         // index of currently executing opcode in memory

    private int delayTimer; //timers
    private int soundTimer;

    private int[] stack;    //stack:
    private int sp;         // stack pointer

    private final int[] fontSet = {       //font set
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };
    private Random random;   // random for 0xCXNN opcode

    public boolean[][] gfx;  // 64 x 32
    public boolean[] key;    // 16 keys (0x0 - 0xF)
    public boolean drawFlag = false;


    public void initialize()
    {
        opcode = 0x0000;                        // reset opcode
        memory = new int[4096];                 //clear memory

        for(int i = 0; i < 80; i++)             //loadFile font to memory
            memory[i + 0x50] = fontSet[i] & 0x000000FF; // font is stored in memory from 0x50 to 0xA0

        V = new int[16];                        //clear registers
        pc = 0x200 & 0x00000FFF;                // program counter start value = 0x200
        gfx = new boolean[64][32];              // reset gfx

        delayTimer = 0x0;                       // reset timers
        soundTimer = 0x0;

        stack = new int[16];                    // clear stack
        sp = -1;                                // stack pointer initial value = -1
        random = new Random();                  // initialize random
        key = new boolean[16];                  // clear key
    }

    public void loadFile(String filePath) throws IOException
    {
        // read binary file:
        Path path = Paths.get(filePath);
        byte[] rom = Files.readAllBytes(path);

        // load ROM to memory:
        for(int i = 0; i < rom.length; i++)
            memory[i + 0x200] = rom[i] & 0x000000FF;    // ROM starts at 0x200
    }

    public void cycle()
    {
        drawFlag = false;
        //fetch opcode:
        opcode = ((memory[pc] << 8) | (memory[pc + 1] & 0x000000FF)) & 0x0000FFFF;
        //decode and execute:
        switch(opcode & 0xF000)
        {
            case 0x0000:
                switch(opcode & 0x000F)
                {
                    case 0x0000:                             // 0x0000 - clear display
                        for(boolean[] row : gfx)
                            Arrays.fill(row, false);    // set all elements to false
                        pc += 2;
                        drawFlag = true;
                        break;
                    case 0x000E:                            // 0x000E - return from subroutine
                        pc = stack[sp];                     // get pc from stack
                        sp--;
                        pc += 2;                            // next opcode!!!!!!
                        break;
                }
                break;

            case 0x1000:                                    // 0x1NNN - jump to NNN
                pc = opcode & 0x0FFF;
                break;

            case 0x2000:                                    // 0x2NNN - call subroutine at NNN
                sp++;
                stack[sp] = pc;                             // save current pc value on stack
                pc = opcode & 0x0FFF;                       // jump to address NNN
                break;

            case 0x3000:                                    // 0x3XNN skip next instruction if V[X] == NN
                if(V[(opcode & 0x0F00) >> 8] == (opcode & 0x00FF))
                    pc += 4;                                // skip next instruction
                else
                    pc += 2;                                // don't skip
                break;

            case 0x4000:                                    // 0x4XNN skip next instruction if V[X] != NN
                if(V[(opcode & 0x0F00) >> 8] != (opcode & 0x00FF))
                    pc += 4;                                // skip next
                else
                    pc += 2;                                // don't skip
                break;

            case 0x5000:                                    // 0x5XY0 - skip next if V[X] == V[Y]
                if(V[(opcode & 0x0F00) >> 8] == V[(opcode & 0x00F0) >> 4])
                    pc += 4;                                // skip
                else
                    pc += 2;                                // don't
                break;

            case 0x6000:                                    // 0x6XNN - set register V[X] to value NN
                V[(opcode & 0x0F00) >> 8] = (opcode & 0x000000FF);
                pc += 2;
                break;

            case 0x7000:                                    // 0x7XNN - add value NN to register V[X]
                // this one is a little tricky because Java has no unsigned type. You need to check and
                // simulate overflow by yourself - if value is bigger than 255 it starts counting from 0
                // for example 250 + 17 => 11 (250 + 17 - 256)
                if(V[(opcode & 0x0F00) >> 8] > (0xFF - (opcode & 0x000000FF)))
                    V[(opcode & 0x0F00) >> 8] = (V[(opcode & 0x0F00) >> 8] + (opcode & 0x00FF) - 256) & 0x000000FF;
                else
                    V[(opcode & 0x0F00) >> 8] += (opcode & 0x000000FF);
                pc += 2;
                break;

            case 0x8000:
                switch(opcode & 0x000F)
                {
                    case 0x0000:                            // 0x8XY0 - set register V[X] to value V[Y]
                        V[(opcode & 0x0F00) >> 8] = V[(opcode & 0x00F0) >> 4];
                        pc += 2;
                        break;

                    case 0x0001:                            // 0x8XY1 - set register V[X] to V[X] OR V[Y]
                        V[(opcode & 0x0F00) >> 8] |= (V[(opcode & 0x00F0) >> 4]);
                        pc += 2;
                        break;

                    case 0x0002:                            // 0x8XY2 - set register V[X] to V[X] AND V[Y]
                        V[(opcode & 0x0F00) >> 8] &= (V[(opcode & 0x00F0) >> 4]);
                        pc += 2;
                        break;

                    case 0x0003:                            // 0x8XY2 - set register V[X] to V[X] XOR V[Y]
                        V[(opcode & 0x0F00) >> 8] ^= (V[(opcode & 0x00F0) >> 4]);
                        pc += 2;
                        break;

                    case 0x0004:                            // 0x8XY4 - V[X] += V[Y]
                        if(V[(opcode & 0x00F0) >> 4] > (0xFF - V[(opcode & 0x0F00) >> 8]))
                        {
                            V[0xF] = 1;                     // if there is a carry - set flag to 1
                            V[(opcode & 0x0F00) >> 8] = (V[(opcode & 0x00F0) >> 4] + V[(opcode & 0x0F00) >> 8] - 256);
                        }
                        else {
                            V[0xF] = 0;                     // no carry
                            V[(opcode & 0x0F00) >> 8] = (V[(opcode & 0x00F0) >> 4] + V[(opcode & 0x0F00) >> 8]);
                        }
                        pc += 2;
                        break;

                    case 0x0005:                            // 0x8XY5 - V[X] -= V[Y]
                        if(V[(opcode & 0x0F00) >> 8] < V[(opcode & 0x00F0) >> 4])
                        {
                            V[0xF] = 0;                     // if there is a borrow - set flag to 0
                            V[(opcode & 0x0F00) >> 8] = (256 - (V[(opcode & 0x00F0) >> 4] - V[(opcode & 0x0F00) >> 8]));
                        }
                        else {
                            V[0xF] = 1;                     // no borrow
                            V[(opcode & 0x0F00) >> 8] = (V[(opcode & 0x0F00) >> 8] - V[(opcode & 0x00F0) >> 4]);
                        }
                        pc += 2;
                        break;

                    case 0x0006:                            // 0x8XY6 - stores least significant bit of V[X] in V[F]
                        V[0xF] = (V[(opcode & 0x0F00) >> 8] & 0x1);
                        V[(opcode & 0x0F00) >> 8] >>= 1;
                        pc += 2;
                        break;

                    case 0x0007:                            // 0x8XY7 - V[X] = V[Y] - V[X]
                        if(V[(opcode & 0x0F00) >> 8] > V[(opcode & 0x00F0) >> 4])
                        {
                            V[0xF] = 0;                     // borrow - set flag to 0
                            V[(opcode & 0x0F00) >> 8] = (256 - (V[(opcode & 0x0F00) >> 8] - V[(opcode & 0x00F0) >> 4]));
                        }
                        else
                        {
                            V[0xF] = 1;                     // no borrow
                            V[(opcode & 0x0F00) >> 8] = ((V[(opcode & 0x00F0) >> 4] - V[(opcode & 0x0F00) >> 8]));
                        }
                        pc += 2;
                        break;

                    case 0x000E:                            // 0x8XYE - stores most significant bit of V[X] in V[F]
                        V[0xF] = (V[(opcode & 0x0F00) >> 8] >> 7);
                        V[(opcode & 0x0F00) >> 8] <<= 1;
                        pc += 2;
                        break;
                }
                break;

            case 0x9000:                                    // 0x9XY0 - skip next if V[X] not equals V[Y]
                if(V[(opcode & 0x00F0) >> 4] != V[(opcode & 0x0F00) >> 8])
                    pc += 4;
                else
                    pc += 2;
                break;

            case 0xA000:                                    // 0xANNN - register I = NNN
                I = (opcode & 0x00000FFF);
                pc += 2;
                break;

            case 0xB000:                                    // 0xBNNN - jump to address V[0] + NNN
                pc = V[0x0] + (opcode & 0x0FFF);
                break;

            case 0xC000:                                    // 0xCXNN - V[X] = random() & NN
                V[(opcode & 0x0F00) >> 8] = (random.nextInt(256) & (opcode & 0x00FF));
                pc += 2;
                break;

            case 0xD000:                                    // 0xDXYN - display
                int x0 = V[(opcode & 0x0F00) >> 8] & 0x000000FF;    // x0 = X
                int y0 = V[(opcode & 0x00F0) >> 4] & 0x000000FF;    // y0 = Y
                int height = (opcode & 0x0000000F);                 // height = N
                int spriteRow;

                V[0xF] = 0x0;                               // clear flag
                for(int y = 0; y < height; y++)
                {
                    spriteRow = (memory[I + y] & 0x000000FF);
                    for(int x = 0; x < 8; x++)
                    {
                        if((spriteRow & (0x00000080 >> x)) != 0)
                        {
                            if(gfx[(x+x0) % 64][(y+y0) % 32])
                                V[0xF] = 1;
                            gfx[(x+x0) % 64][(y+y0) % 32] = !gfx[(x+x0) % 64][(y+y0) % 32];
                        }
                    }
                }
                drawFlag = true;
                pc += 2;
                break;

            case 0xE000:
                switch(opcode & 0x00FF)
                {
                    case 0x009E:                            // 0xEX9E - skip next if key 'V[X]' is pressed
                        if(key[V[(opcode & 0x0F00) >> 8]])
                            pc += 4;
                        else
                            pc += 2;
                        break;

                    case 0x00A1:                            // 0xEXA1 - skip next if key 'V[X]' is not pressed
                        if(!key[V[(opcode & 0x0F00) >> 8]])
                            pc += 4;
                        else
                            pc += 2;
                        break;
                }
                break;

            case 0xF000:
                switch(opcode & 0x00FF)
                {
                    case 0x0007:                            // 0xFX07 - V[X] = delayTimer
                        V[(opcode & 0x0F00) >> 8] = delayTimer & 0x000000FF;
                        pc += 2;
                        break;

                    case 0x000A:                            // 0xFX0A - wait for any key to be pressed
                        for(int i = 0; i < key.length; i++)                 // check all the keys
                            if(key[i])                                      // if key 'i' is pressed
                            {
                                V[(opcode & 0x0F00) >> 8] = i & 0x000000FF; // V[X] = i
                                pc += 2;
                                break;
                            }
                        break;

                    case 0x0015:                            // 0xFX15 - delayTimer = V[X]
                        delayTimer = V[(opcode & 0x0F00) >> 8] & 0x000000FF;
                        pc += 2;
                        break;

                    case 0x0018:                            // 0xFX18 - soundTimer = V[X]
                        soundTimer = V[(opcode & 0x0F00) >> 8] & 0x000000FF;
                        pc += 2;
                        break;

                    case 0x001E:                            // 0xFX1E - I += V[X]
                        if(I + V[(opcode & 0x0F00) >> 8] > 0xFFF)
                            V[0xF] = 1;
                        else
                            V[0xF] = 0;
                        I += (V[(opcode & 0x0F00) >> 8]);
                        pc += 2;
                        break;

                    case 0x0029:                            // 0xFX29 - set I register to address of font sprite of 'V[X]' character
                        I = (V[(opcode & 0x0F00) >> 8] * 5 + 0x50);
                        pc += 2;
                        break;

                    case 0x0033:                            // 0xFX33 - stores binary-coded decimal representation of V[X]
                        memory[I] = (V[(opcode & 0x0F00) >> 8] / 100) & 0x000000FF;
                        memory[I + 1] = ((V[(opcode & 0x0F00) >> 8] / 10) % 10) & 0x000000FF;
                        memory[I + 2] = ((V[(opcode & 0x0F00) >> 8] % 100) % 10) & 0x000000FF;
                        pc += 2;
                        break;

                    case 0x0055:                            // 0xFX55 - stores registers V[0] to V[X] in memory
                        for(int i = 0; i <= ((opcode & 0x0F00) >> 8); i++)
                            memory[I + i] = V[i];
                        pc += 2;
                        I += (((opcode & 0x0F00) >> 8) + 1);
                        break;

                    case 0x0065:                            // 0xFX65 - stores memory in registers V[0] to V[X]
                        for(int i = 0; i <= ((opcode & 0x0F00) >> 8); i++)
                            V[i] = (memory[I + i] & 0x00FF);
                        pc += 2;
                        I += (((opcode & 0x0F00) >> 8) + 1);
                        break;
                }
                break;
        }

        // timers
        if(delayTimer > 0)
            delayTimer--;
        if(soundTimer > 0)
        {
            soundTimer--;
            // TODO: make sound!
        }

        // slow down
        try {
            Thread.sleep(1);
        }catch(InterruptedException ie){}
    }


}
